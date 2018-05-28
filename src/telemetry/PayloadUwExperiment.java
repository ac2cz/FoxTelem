package telemetry;

import java.util.ArrayList;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import decoder.Decoder;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
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
	private int flagByte = 0;
	
	public ArrayList<CanPacket> canPackets; 
	private CanPacket canPacket; // the current CAN Packet we are adding bytes to
	
	public PayloadUwExperiment() {
		super(TYPE_UW_EXPERIMENT, new BitArrayLayout());
		MAX_BYTES = 1;
		rawBits = new boolean[8];
		canPackets = new ArrayList<CanPacket>();
	}
	
	public PayloadUwExperiment(int id, int resets, long uptime, String date, StringTokenizer st) {
		super(id, resets, uptime, TYPE_UW_EXPERIMENT, date, st, new BitArrayLayout());
		MAX_BYTES = 1;
		canPackets = new ArrayList<CanPacket>();
	}
	
	protected void init() { 
		
	}
	
	/**
	 * Add a byte to the next CAN Packet.  If the packet is full and we have more bytes, create another packet.
	 * We are finished once we have hit the ID 0x0000, which means end of CAN Packets or we run out of bytes.  
	 * That final packet is thrown away, unless it fit exactly and passes the isValid() check.
	 * away
	 * @param b
	 */
	private void addToCanPackets(byte b) {
		String debug = (Decoder.hex(b));
		if (canPacket == null) {
			canPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.RAD_LAYOUT));
			canPacket.captureHeaderInfo(id, uptime, resets);
			canPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100);
		}
		if (canPacket.hasEndOfCanPacketsId()) return;
		canPacket.addNext8Bits(b);
		if (canPacket.isValid()) {
			canPackets.add(canPacket);
			canPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.RAD_LAYOUT));
			canPacket.captureHeaderInfo(id, uptime, resets);
			canPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100+canPackets.size());
		}
	}
	
	public void addNext8Bits(byte b) {
		if (numberBytesAdded <1)
			super.addNext8Bits(b);  // the flag byte
		else
			addToCanPackets(b);
	}

	
	@Override
	public void copyBitsToFields() {
		// Nothing to do here as bits are put into the structure as they are received
	}

	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = "UW EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "OVERFLOW FLAG: " + rawBits[0];
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

	public boolean savePayloads() {
		for (CanPacket p : canPackets)
			if (!Config.payloadStore.add(getFoxId(), getUptime(), getResets(), p))
				return false;
		return true;

	}

	/**
	 * Write the data out to file
	 * This may need to write the flag byte and perhaps the total number of packets.
	 * The packets are saved as seperate payloads
	 * 
	 */
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		return s;
	}
	
	/**
	 * Get all the Can Packets Bytes in this Payload as an array of payload byte arrays
	 * @return
	 */
	public byte[][] getCANPacketBytes() {
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
	
	byte[] concatenateByteArrays(byte[] a, byte[] b) {
	    byte[] result = new byte[a.length + b.length]; 
	    System.arraycopy(a, 0, result, 0, a.length); 
	    System.arraycopy(b, 0, result, a.length, b.length); 
	    return result;
	} 
}
