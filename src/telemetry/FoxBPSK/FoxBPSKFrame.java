package telemetry.FoxBPSK;

import common.Config;
import common.FoxSpacecraft;

import java.io.BufferedReader;
import java.io.IOException;

import common.Log;
import common.Spacecraft;
import telemetry.FoxFramePart;
import telemetry.Frame;
import telemetry.HighSpeedTrailer;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRadExpData;
import telemetry.PayloadRtValues;
import telemetry.PayloadWOD;
import telemetry.PayloadWODRad;
import telemetry.RadiationTelemetry;

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
		public static final int TYPES_OF_FRAME = 5;

		FoxFramePart[] payload = new FoxFramePart[NUMBER_DEFAULT_PAYLOADS];
		
		HighSpeedTrailer trailer = null;

		int numberBytesAdded = 0;
		private boolean firstNonHeaderByte = true;
		
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
						if (Config.debugFrames)
							Log.println(header.toString());
					} else {
						Log.println("FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");
						corrupt = true;
						return;
					}
					firstNonHeaderByte = false;
				}
				payload[0].addNext8Bits(b);
			} else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*2)
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
		 *  Here is how the frames are defined in the IHU:
		 *  
            // ALL_WOD_FRAME
            {WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5,  WOD_SCI_PAYLOAD6,WOD_HK_PAYLOAD5,  WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5},
            // REALTIME_FRAME (Realtime plus WOD, actually)
            {WOD_SCI_PAYLOAD6,WOD_HK_PAYLOAD5,WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5, REALTIME_PAYLOAD1,RAD_EXP_PAYLOAD4},
            // MINMAX_FRAME (Min/Max plus WOD)
            {WOD_SCI_PAYLOAD6,WOD_HK_PAYLOAD5,WOD_SCI_PAYLOAD6, WOD_HK_PAYLOAD5,MAX_VALS_PAYLOAD2,MIN_VALS_PAYLOAD3},
            // REALTIME_BEACON
            {REALTIME_PAYLOAD1, WOD_HK_PAYLOAD5,  WOD_HK_PAYLOAD5, WOD_HK_PAYLOAD5,  WOD_HK_PAYLOAD5, REALTIME_PAYLOAD1},
            // WOD_BEACON
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
				payload[0] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[1] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				payload[2] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
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
				payload[5] = new PayloadRtValues(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
				break;
			case WOD_BEACON:
				for (int i=0; i<NUMBER_DEFAULT_PAYLOADS; i++ ) {
					payload[i] = new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
				}
				break;
			default: break;
			}
		}

		public void savePayloads() {
			for (int i=0; i<NUMBER_DEFAULT_PAYLOADS; i++ ) {
				payload[i].copyBitsToFields(); // this makes sure that the timestamps are correct
				Config.payloadStore.add(header.getFoxId(), payload[1].getUptime(), payload[1].getResets(), payload[i]);
			}			
		}
		
		public static int getMaxDataBytes() {
			return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE;
		}
		
		public static int getMaxBytes() {
			return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE;
		}
		
		public String toString() {
			String s = new String();
			s = "\n" + header.toString();

			if (payload != null)
				for (int i=0; i < payload.length; i++) {
					s = s + "\n"+ payload[i].toString() +
					"\n"; 
				}
			
			return s;
		}
}
