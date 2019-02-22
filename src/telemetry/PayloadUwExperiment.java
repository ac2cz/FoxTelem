package telemetry;

import java.util.ArrayList;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.uw.CanPacket;
import telemetry.uw.PcanPacket;

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
public class PayloadUwExperiment extends FoxFramePart {	
	public ArrayList<CanPacket> canPackets; 
	public ArrayList<CanPacket> splitPackets; 
	protected CanPacket canPacket; // a temporary packet to hold the bytes and calculate the ID 
	protected CanPacket rawCanPacket; // a temporary packet to hold the bytes and calculate the ID 
	
	// the current CAN Packet we are adding bytes to
	//private int startPacketSerial = 0;
	
	public PayloadUwExperiment(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_UW_EXPERIMENT,lay);
		canPackets = new ArrayList<CanPacket>();
		//if (Config.splitCanPackets)
		splitPackets = new ArrayList<CanPacket>();
		captureHeaderInfo(id, uptime, resets);
	}
	
	public PayloadUwExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_UW_EXPERIMENT, date, st, lay);
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
	 */
	int debugCount = 0;
	protected void addToCanPackets(byte b) {

//		if (!Config.splitCanPackets) {
//			if (canPacket == null) {
//				canPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT));
//				canPacket.captureHeaderInfo(id, uptime, resets);
//				//canPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100+startPacketSerial);
//			}
//			if (canPacket.hasEndOfCanPacketsId()) return;
//			canPacket.addNext8Bits(b);
//			if (canPacket.isValid()) {
//				canPackets.add(canPacket);
//				canPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT));
//				canPacket.captureHeaderInfo(id, uptime, resets);
//				//.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100+startPacketSerial+canPackets.size());
//			}
//		} else {
			if (rawCanPacket == null) {
				rawCanPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT)); 
				rawCanPacket.captureHeaderInfo(id, uptime, resets);
			}
			if (rawCanPacket.hasEndOfCanPacketsId()) return;
			rawCanPacket.addNext8Bits(b);
			if (rawCanPacket.isValid()) {
				canPackets.add(rawCanPacket);
				byte[] data = rawCanPacket.getBytes();
				BitArrayLayout canLayout = Config.satManager.getLayoutByCanId(id, rawCanPacket.getID());
				if (canLayout == null) Log.errorDialog("ERROR", "Missing CAN Layout for CAN ID: "+rawCanPacket.getID());
				CanPacket newPacket = new CanPacket(id, resets, uptime, captureDate, data, canLayout);
				newPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET_TELEM);
				splitPackets.add(newPacket);
				rawCanPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT)); 
				rawCanPacket.captureHeaderInfo(id, uptime, resets);
			}
//		}
	}
	
	@Override
	public void addNext8Bits(byte b) {
		if (numberBytesAdded <1)
			super.addNext8Bits(b);  // the flag byte
		else  {
			addToCanPackets(b);
		}	
	}

	@Override
	public String toString() {
		copyBitsToFields();
		String s = "UW EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "RESET: " + resets;
		s = s + "  UPTIME: " + uptime;
		s = s + "  TYPE: " + type;
		s = s + "  OVERFLOW FLAG: (not printed)"  + "\n";
		//for (int p=0; p < canPackets.size(); p++) {
		//	s = s + canPackets.get(p).toString() + "    " ;
		//	if ((p+1)%3 == 0) s = s + "\n";
		//}
		//s=s+"\n";

		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean savePayloads(FoxPayloadStore payloadStore, int serial, boolean storeMode) {
		type = type * 100 + serial;
		if (!payloadStore.add(getFoxId(), getUptime(), getResets(), this))
			return false;
		int j = 0;
		for (CanPacket p : canPackets) {
			// Set the type here as it needs to span across payloads.  The uptime is NOT unique for multiple payloads in same Frame.
			int p_type = p.getType();
			p_type = p_type * 100 + serial + j++;
			p.setType(p_type);
			if (storeMode)
				p.newMode = newMode;
			if (!payloadStore.add(getFoxId(), getUptime(), getResets(), p))
				return false;
		}
		if (splitPackets != null && splitPackets.size() > 0) {
			j = 0;
			for (CanPacket p : splitPackets) {
				// Set the type here as it needs to span across payloads.  The uptime is NOT unique for multiple payloads in same Frame.
				int p_type = p.getType();
				p_type = p_type * 100 + serial + j++;
				p.setType(p_type);
				if (storeMode)
					p.newMode = newMode;
				if (!payloadStore.add(getFoxId(), getUptime(), getResets(), p))
					return false;
			}
		}

		return true;

	}
	
	public byte[][] getCANPacketBytes() {
		return getCANPacketBytes(canPackets);
	}
	
	/**
	 * Get all the Can Packets Bytes in this Payload as an array of payload byte arrays
	 * @return
	 */
	static public byte[][] getCANPacketBytes(ArrayList<CanPacket> canPackets) {
		byte[][] buffers = new byte[canPackets.size()][];
		int i=0;
		for (CanPacket p : canPackets) {
			PcanPacket pc = p.getPCanPacket();
		//	if (Config.debugFrames)
		//		Log.println("PCAN: " + pc);
			buffers[i++] = pc.getBytes(); 
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
