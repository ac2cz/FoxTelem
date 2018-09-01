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
	
	public static final int FLAG_FIELD = 0;
	public static final String FLAG = "Flag";
	
	public ArrayList<CanPacket> canPackets; 
	protected CanPacket canPacket; // the current CAN Packet we are adding bytes to
	private int startPacketSerial = 0;
	
	public PayloadUwExperiment(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_UW_EXPERIMENT,lay);
		//MAX_BYTES = 1;
		canPackets = new ArrayList<CanPacket>();
		captureHeaderInfo(id, uptime, resets);
	}
	
	public PayloadUwExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_UW_EXPERIMENT, date, st, lay);
		//MAX_BYTES = 1;
		canPackets = new ArrayList<CanPacket>();
	}
	
	protected void init() { 
		//rawBits = new boolean[8];	
	}
	
	public void setStartSerial(int serial) {
		startPacketSerial = serial;
	}
	
	/**
	 * Add a byte to the next CAN Packet.  If the packet is full and we have more bytes, create another packet.
	 * We are finished once we have hit the ID 0x0000, which means end of CAN Packets or we run out of bytes.  
	 * That final packet is thrown away, unless it fit exactly and passes the isValid() check.
	 * away
	 * @param b
	 */
	int debugCount = 0;
	protected void addToCanPackets(byte b) {
//		if (Config.debugBytes) {
//			String debug = (Decoder.plainhex(b));
//			debugCount++;
//			Log.print(debug);
//			if (debugCount % 40 == 0) {
//				Log.println("");
//				debugCount = 0;
//			}
//		}
		if (canPacket == null) {
			canPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT));
			canPacket.captureHeaderInfo(id, uptime, resets);
			//canPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100+startPacketSerial);
		}
		if (canPacket.hasEndOfCanPacketsId()) return;
		canPacket.addNext8Bits(b);
		if (canPacket.isValid()) {
			canPackets.add(canPacket);
			canPacket = new CanPacket(Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT));
			canPacket.captureHeaderInfo(id, uptime, resets);
			//.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100+startPacketSerial+canPackets.size());
		}
	}
	
	@Override
	public void addNext8Bits(byte b) {
		if (numberBytesAdded <1)
			super.addNext8Bits(b);  // the flag byte
		else  {
			addToCanPackets(b);
			super.addNext8Bits(b);
		}	
	}

	@Override
	public String toString() {
		copyBitsToFields();
		String s = "UW EXPERIMENT PAYLOAD - " + canPackets.size() + " CAN PACKETS\n";
		s = s + "RESET: " + resets;
		s = s + "  UPTIME: " + uptime;
		s = s + "  TYPE: " + type;
		s = s + "  OVERFLOW FLAG: " + rawBits[0] + "\n";
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

	public boolean savePayloads(FoxPayloadStore payloadStore, int serial) {
		type = type * 100 + serial;
		if (!payloadStore.add(getFoxId(), getUptime(), getResets(), this))
			return false;
		int j = 0;
		for (CanPacket p : canPackets) {
			int p_type = p.getType();
			p_type = p_type * 100 + serial + j++;
			p.setType(p_type);
			if (!payloadStore.add(getFoxId(), getUptime(), getResets(), p))
				return false;
		}
		return true;

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
