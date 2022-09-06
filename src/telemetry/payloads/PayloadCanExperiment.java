package telemetry.payloads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FoxPayloadStore;
import telemetry.FramePart;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * This is a payload containing University of Washington experiment results.  It consists of the following:
 * A flag byte - bit 1 indicates overflow
 * A set of CAN Packets
 * 
 *
 */
public class PayloadCanExperiment extends FramePart {	
	public ArrayList<CanPacket> canPackets; 
//	public ArrayList<CanPacket> splitPackets; 
//	protected CanPacket canPacket; // a temporary packet to hold the bytes and calculate the ID 
	protected CanPacket rawCanPacket; // a temporary packet to hold the bytes and calculate the ID 
	boolean addingCanPackets = true;
	
	// the current CAN Packet we are adding bytes to
	//private int startPacketSerial = 0;
	
	public PayloadCanExperiment(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_CAN_EXP,lay);
		canPackets = new ArrayList<CanPacket>();
		//if (Config.splitCanPackets)
//		splitPackets = new ArrayList<CanPacket>();
		captureHeaderInfo(id, uptime, resets);
	}
	
	public PayloadCanExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_CAN_EXP, date, st, lay);
		canPackets = new ArrayList<CanPacket>();
	}
	
	protected void init() { 
		// nothing extra to init here
	}
	
//	public void setStartSerial(int serial) {
//		startPacketSerial = serial;
//	}
	
	/**
	 * Add a byte to the next CAN Packet.  If the packet is full and we have more bytes, create another packet.
	 * We are finished once we have hit the ID 0x0000, which means end of CAN Packets or we run out of bytes.  
	 * That final packet is thrown away, unless it fit exactly and passes the isValid() check.
	 * away
	 * @param b
	 * 
	 * returns false if this was the end of the can packets
	 */
	int debugCount = 0;
	protected boolean addToCanPackets(byte b) {
		String canPktLayout = Spacecraft.CAN_PKT_LAYOUT;
		if (this instanceof PayloadCanWODExperiment)
			canPktLayout = Spacecraft.WOD_CAN_PKT_LAYOUT;
		if (rawCanPacket == null) {
			BitArrayLayout canLayout = Config.satManager.getLayoutByName(id, canPktLayout);
			if (canLayout == null) {
				Log.println("ERROR: Payload Layout is missing for: " + canPktLayout + "\nCheck the MASTER file to confirm this is defined correctly.");
			}
			rawCanPacket = new CanPacket(canLayout); 
			rawCanPacket.captureHeaderInfo(id, uptime, resets);
		}
		if (rawCanPacket.hasEndOfCanPacketsId()) return false;
		rawCanPacket.addNext8Bits(b);
		if (rawCanPacket.isValid()) {
			canPackets.add(rawCanPacket);
			
			BitArrayLayout canLayout = Config.satManager.getLayoutByName(id, canPktLayout);
			rawCanPacket = new CanPacket(canLayout); 
			rawCanPacket.captureHeaderInfo(id, uptime, resets);
		}
		return true;
		//		}
	}
	
	@Override
	public void addNext8Bits(byte b) {
		super.addNext8Bits(b);
		if (addingCanPackets)
			addingCanPackets = addToCanPackets(b); // returns false when at end of can packets
	}

	@Override
	public String toString() {
		copyBitsToFields();
		String s = "CAN EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "RESET: " + resets;
		s = s + "  UPTIME: " + uptime;
		s = s + "  TYPE: " + type;
		s = s + "  OVERFLOW FLAG: (not printed)"  + "\n";
		for (int p=0; p < canPackets.size(); p++) {
			s = s + canPackets.get(p).toString() + "    " ;
			if ((p+1)%3 == 0) s = s + "\n";
		}
		s=s+"\n";

		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean savePayloads(FoxPayloadStore payloadStore, int serial, boolean storeMode) {
		// Don't update the type with a serial number if this is WOD, as WOD records are unique based on the reset/uptime only
		// But Experiment payloads may be packed into a frame and each has their own timestamp
		if (!(this instanceof PayloadCanWODExperiment))
			type = type * 100 + serial;
		copyBitsToFields(); // make sure timestamps correct if this is WOD
		if (!payloadStore.add(getFoxId(), getUptime(), getResets(), this))
			return false;
		int j = 0;
		for (CanPacket p : canPackets) {
			// Set the type here as it may need to span across payloads
			// If this is a WOD record then the packets are sequential within this payload
			// For non WOD the packet serial number increases across payloads
			int p_type = p.getType();
			if (this instanceof PayloadCanWODExperiment)
				p_type = p_type * 100 + j++;
			else
				p_type = p_type * 100 + serial + j++;

			p.setType(p_type);
			if (storeMode)
				p.newMode = newMode;
			if (payloadStore.add(getFoxId(), getUptime(), getResets(), p)) {
				// We added this to the database and it is new
				Spacecraft sat = Config.satManager.getSpacecraft(id);
				if (sat.hasMesatCamera()) {
					// Try to add this to a camera image
					if (payloadStore.mesatImageStore != null) {
						try {
							payloadStore.mesatImageStore.add(getFoxId(), getResets(), getUptime(), p, getCaptureDate());
						} catch (IOException e) {
							Log.println("ERROR: Could not add the MESAT packet to the image");
							e.printStackTrace(Log.getWriter());
						}
					}
				}
			} else {
				// this packet could not be added, but we still try to add the others
				// though if this was a duplicate then likely all the others are too
			}
			p.rawBits = null; // free up the bit array
		}

		return true;

	}
	
	public byte[][] getCANPacketBytes(Date createDate) {
		return getCANPacketBytes(canPackets, createDate);
	}
	
	/**
	 * Get all the Can Packets Bytes in this Payload as an array of payload byte arrays
	 * @return
	 */
	static public byte[][] getCANPacketBytes(ArrayList<CanPacket> canPackets, Date createDate) {
		byte[][] buffers = new byte[canPackets.size()][];
		int i=0;
		for (CanPacket p : canPackets) {
		//	if (Config.debugFrames)
		//		Log.println("CAN: " + pc);
			buffers[i++] = p.getBytes(); 
		}
		return buffers;
	}
	
	static byte[] concatenateByteArrays(byte[] a, byte[] b) {
	    byte[] result = new byte[a.length + b.length]; 
	    System.arraycopy(a, 0, result, 0, a.length); 
	    System.arraycopy(b, 0, result, a.length, b.length); 
	    return result;
	} 
}
