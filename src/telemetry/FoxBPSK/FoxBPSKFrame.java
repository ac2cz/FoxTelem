package telemetry.FoxBPSK;

import common.Config;
import common.FoxSpacecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import common.Log;
import common.Spacecraft;
import decoder.Decoder;
import decoder.FoxDecoder;
import telemetry.FoxFramePart;
import telemetry.Frame;
import telemetry.HighSpeedTrailer;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRadExpData;
import telemetry.PayloadRtValues;
import telemetry.PayloadWOD;
import telemetry.PayloadWODRad;
import telemetry.uw.CanPacket;

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
	 */
	public class FoxBPSKFrame extends Frame {
		
		public static final int MAX_FRAME_SIZE = 476;
		public static final int MAX_HEADER_SIZE = 8;
		public static final int PAYLOAD_SIZE = 78;
		public static final int MAX_PAYLOAD_SIZE = MAX_FRAME_SIZE - MAX_HEADER_SIZE; 
		public static final int MAX_TRAILER_SIZE = 96;
		
		public static final int NUMBER_DEFAULT_PAYLOADS = 6; 
		
		public static final int ALL_WOD_FRAME = 0;
		public static final int REALTIME_FRAME = 1;
		public static final int MINMAX_FRAME = 2;
		public static final int REALTIME_BEACON = 3;
		public static final int WOD_BEACON = 4;
		public static final int CAN_PACKET_SCIENCE_FRAME = 5;
		public static final int CAN_PACKET_CAMERA_FRAME = 6;
		public static final int TYPES_OF_FRAME = 7;
		
		private boolean canPacketFrame = false;

		public FoxFramePart[] payload = new FoxFramePart[NUMBER_DEFAULT_PAYLOADS];
		public ArrayList<CanPacket> canPackets; 
		private CanPacket canPacket; // the current CAN Packet we are adding bytes to
		
		HighSpeedTrailer trailer = null;

		int numberBytesAdded = 0;
		private boolean firstNonHeaderByte = true;
		private boolean firstCANPayloadByte = true;
		
		public FoxBPSKFrame() {
			super();
			header = new FoxBPSKHeader();
			bytes = new byte[MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE];
		}

		public FoxBPSKFrame(BufferedReader input) throws IOException {
			super(input);
		}
		
		public FoxBPSKHeader getHeader() { return (FoxBPSKHeader)header; }
		
		public void addNext8Bits(byte b) {
			if (corrupt) return;
			if (numberBytesAdded < MAX_HEADER_SIZE)
				header.addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE) {
				if (firstNonHeaderByte) {
					header.copyBitsToFields(); // make sure the id is populated
					fox = (FoxSpacecraft) Config.satManager.getSpacecraft(header.id);
					if (fox != null) {
						initPayloads(header.getType());
						if (payload[0] == null && !canPacketFrame) {
							if (Config.debugFrames)
								Log.errorDialog("ERROR","FOX ID: " + header.id + " Type: " + header.getType() + " not valid. Decode not possible.\n"
										+ "Turn off Debug Frames to prevent this message in future.");
							else
								Log.println("FOX ID: " + header.id + " Type: " + header.getType() + " not valid. Decode not possible.");

							corrupt = true;
							return;
						}
						if (Config.debugFrames)
							Log.println(header.toString());
					} else {
						if (Config.debugFrames)
							Log.errorDialog("ERROR","FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.\n"
									+ "Turn off Debug Frames to prevent this message in future.");
						else
							Log.println("FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");							
						corrupt = true;
						return;
					}
					firstNonHeaderByte = false;
				}
				if (!canPacketFrame)
					payload[0].addNext8Bits(b);
				else if (firstCANPayloadByte){
					// The first non header byte of a CAN packet payload is a flag byte
					// BIT 0 means overflow
					; // it still needs debugging by BURNS
					firstCANPayloadByte = false;
				} else {
					addToCanPackets(b);
				}
			} else if (canPacketFrame)
				addToCanPackets(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*2)
				payload[1].addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*3)
				payload[2].addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*4)
				payload[3].addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*5)
				payload[4].addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*6)
				payload[5].addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE)
				;//trailer.addNext8Bits(b); //FEC ;
			else
				Log.println("ERROR: attempt to add byte past end of frame");

			bytes[numberBytesAdded] = b;
			numberBytesAdded++;
		}
		
		/**
		 * Add a byte to the next CAN Packet.  If the packet is full and we have more bytes, create another packet.
		 * We are finished once we have hit the ID 0x0000, which means end of CAN Packets.  That final packet is thrown
		 * away
		 * @param b
		 */
		private void addToCanPackets(byte b) {
			String debug = (Decoder.hex(b));
			if (canPacket == null) {
				canPacket = new CanPacket(Config.satManager.getLayoutByName(header.id, Spacecraft.RAD_LAYOUT));
				canPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100);
			}
			if (canPacket.hasEndOfCanPacketsId()) return;
			canPacket.addNext8Bits(b);
			if (canPacket.isValid()) {
				canPackets.add(canPacket);
				canPacket = new CanPacket(Config.satManager.getLayoutByName(header.id, Spacecraft.RAD_LAYOUT));
				canPacket.setType(FoxFramePart.TYPE_UW_CAN_PACKET*100+canPackets.size());
			}
		}

		/**
		 *  Here is how the frames are defined in the IHU:
		 *  
            // 0 ALL_WOD_FRAME
            {WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5,  WOD_SCI_PAYLOAD6,WOD_HK_PAYLOAD5,  WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5},
            // 1 REALTIME_FRAME (Realtime plus WOD, actually)
            {WOD_SCI_PAYLOAD6,WOD_HK_PAYLOAD5,WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5, REALTIME_PAYLOAD1,RAD_EXP_PAYLOAD4},
            // 2 MINMAX_FRAME (Min/Max plus WOD)
            {WOD_SCI_PAYLOAD6,WOD_HK_PAYLOAD5,WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5,MAX_VALS_PAYLOAD2,MIN_VALS_PAYLOAD3},
            // 3 REALTIME_BEACON
            {REALTIME_PAYLOAD1, WOD_HK_PAYLOAD5,  WOD_HK_PAYLOAD5, WOD_HK_PAYLOAD5,  WOD_HK_PAYLOAD5, REALTIME_PAYLOAD1},
            // 4 WOD_BEACON
            {WOD_HK_PAYLOAD5,WOD_HK_PAYLOAD5,WOD_HK_PAYLOAD5,WOD_HK_PAYLOAD5, WOD_HK_PAYLOAD5,WOD_HK_PAYLOAD5}
		 *
		 * @param type
		 */
		private void initPayloads(int type) {
			switch (type) {
			case ALL_WOD_FRAME:
				for (int i=0; i<NUMBER_DEFAULT_PAYLOADS; i+=2 ) {
					payload[i] = new PayloadWODRad(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAD_LAYOUT));
					payload[i+1] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				}
				break;
			case REALTIME_FRAME:
				payload[0] = new PayloadWODRad(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAD_LAYOUT));
				payload[1] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[2] = new PayloadWODRad(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAD_LAYOUT));
				payload[3] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[4] = new PayloadRtValues(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
				payload[5] = new PayloadRadExpData(Config.satManager.getLayoutByName(header.id, Spacecraft.RAD_LAYOUT));
				break;
			case MINMAX_FRAME:
				payload[0] = new PayloadWODRad(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAD_LAYOUT));
				payload[1] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[2] = new PayloadWODRad(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAD_LAYOUT));
				payload[3] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[4] = new PayloadMaxValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MAX_LAYOUT));
				payload[5] = new PayloadMinValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MIN_LAYOUT));
				break;
			case REALTIME_BEACON:
				payload[0] = new PayloadRtValues(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
				payload[1] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[2] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[3] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[4] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[5] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				break;
			case WOD_BEACON:
				for (int i=0; i<NUMBER_DEFAULT_PAYLOADS; i++ ) {
					payload[i] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				}
				break;
			case CAN_PACKET_SCIENCE_FRAME:
				canPackets = new ArrayList<CanPacket>();
				canPacketFrame = true;
				break;
			case CAN_PACKET_CAMERA_FRAME:
				canPackets = new ArrayList<CanPacket>();
				canPacketFrame = true;
				break;
			default: 
				break;
			}
		}

		public boolean savePayloads() {
			if (canPacketFrame) {
				for (CanPacket p : canPackets)
					if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), p))
						return false;
				return true;
			}
			header.copyBitsToFields(); // make sure we have defaulted the extended FoxId correctly
			for (int i=0; i<NUMBER_DEFAULT_PAYLOADS; i++ ) {
				payload[i].copyBitsToFields();
				if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload[i]))
					return false;
			}
			return true;			
		}
		
		public static int getMaxDataBytes() {
			return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE;
		}
		
		public static int getMaxBytes() {
			return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE;
		}

		public byte[] getPayloadBytes() {
			if (canPacketFrame) return null;
			byte buffer[] = null;
			Spacecraft sat = Config.satManager.getSpacecraft(foxId);
			if (sat.sendToLocalServer()) {
				int numBytes = 0;
				for (int i=0; i< payload.length; i++) {
					// if this payload should be output then add to the byte buffer
					if (payload[i] instanceof PayloadRadExpData) 
						numBytes += ((PayloadRadExpData)payload[i]).getMaxBytes();
					
				}
				buffer = new byte[numBytes];
				int j = 0;
				for (int i=0; i< payload.length; i++) {
					// if this payload should be output then add to the byte buffer
					if (payload[i] instanceof PayloadRadExpData) {
						for (int b : ((PayloadRadExpData)payload[i]).fieldValue)
							buffer[j++] = (byte)b;
					}
				}
			}
			return buffer;
		}

		public String toString() {
			String s = new String();
			s = "\n" + header.toString();
			if (canPacketFrame) {
				s = s + "\nUW CAN PACKET FRAME:\n";
				for (int p=0; p < canPackets.size(); p++) {
					s = s + canPackets.get(p).toString() + "    " ;
					if ((p+1)%3 == 0) s = s + "\n";
				}
				s=s+"\n";
				for (int i =0; i< bytes.length; i++) {
					s = s + FoxDecoder.plainhex(bytes[i]) + " ";
					// Print 8 bytes in a row
					if ((i+1)%32 == 0) s = s + "\n";
				}
			} else
			if (payload != null) {
				for (int i=0; i < payload.length; i++) {
					s = s + "\n"+ payload[i].toString() +
					"\n"; 
				}
			} 
			
			return s;
		}
}
