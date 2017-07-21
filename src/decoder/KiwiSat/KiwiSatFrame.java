package decoder.KiwiSat;

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
	public class KiwiSatFrame extends Frame {
		
		public static final int MAX_FRAME_SIZE = 221;
		public static final int MAX_HEADER_SIZE = 17;
		public static final int PAYLOAD_SIZE = 202;
		public static final int MAX_PAYLOAD_SIZE = MAX_FRAME_SIZE - MAX_HEADER_SIZE; 
		public static final int MAX_TRAILER_SIZE = 2;
		
		// MAKING THIS A REAL TIME LAYOUT MEANS THAT THE LAYOUT DETAILS GO IN THE LAYOUT FILE ATTACHED TO rttelemetry
		public KiwiSatTelemetryPayload payload = new KiwiSatTelemetryPayload(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
		
		HighSpeedTrailer trailer = null;

		int numberBytesAdded = 0;
		private boolean firstNonHeaderByte = true;
		
		public KiwiSatFrame() {
			super();
			header = new KiwiSatHeader();
			bytes = new byte[MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE];
		}

		public KiwiSatFrame(BufferedReader input) throws IOException {
			super(input);
		}
		
		public KiwiSatHeader getHeader() { return (KiwiSatHeader)header; }
		public KiwiSatTelemetryPayload getPayload() { return (KiwiSatTelemetryPayload)payload; }
		
		public void addNext8Bits(byte b) {
			if (corrupt) return;
			if (numberBytesAdded < MAX_HEADER_SIZE)
				header.addNext8Bits(b);
			else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE) {
				if (firstNonHeaderByte) {
					header.copyBitsToFields();
					// IF THERE IS LOGIC TO RUN ONCE HEADER RECEIVED, THEN IT GOES HERE

					if (Config.debugFrames)
							Log.println(header.toString());
					firstNonHeaderByte = false;
				}
				// WE ADD THE BYTES TO THE PAYLOAD(S) HERE. ADD ANY LOGIC TO POPULATE THE PAYLOADS
				payload.addNext8Bits(b);
			} else
				Log.println("ERROR: attempt to add byte past end of frame");

			bytes[numberBytesAdded] = b;
			numberBytesAdded++;
		}
		
	
		public static int getMaxDataBytes() {
			return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE;
		}
		
		public static int getMaxBytes() {
			return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE;
		}
		
}
