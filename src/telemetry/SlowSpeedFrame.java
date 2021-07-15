package telemetry;

import java.io.BufferedReader;
import java.io.IOException;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;

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
public class SlowSpeedFrame extends Frame {
	
	public static final int MAX_HEADER_SIZE = 6;
	public static final int MAX_PAYLOAD_SIZE = 58;
	public static final int MAX_TRAILER_SIZE = 32;
	
	//SlowSpeedHeader header = null;
	FramePart payload = null;
	FramePart fecTrailer = null;
	
	
	public SlowSpeedFrame() {
		super();
		header = new SlowSpeedHeader();
		fecTrailer = new SlowSpeedTrailer();
		bytes = new byte[MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE];
	}
	
	public SlowSpeedFrame(BufferedReader input) throws IOException {
		super(input);
		header = new SlowSpeedHeader();
		bytes = new byte[SlowSpeedFrame.MAX_HEADER_SIZE
							+ SlowSpeedFrame.MAX_PAYLOAD_SIZE
							+ SlowSpeedFrame.MAX_TRAILER_SIZE];
		load(input);
	}
	
	public int getType() {
		return header.getType();
	}

	public SlowSpeedHeader getHeader() {
		return (SlowSpeedHeader)header;
	}

	public FramePart getPayload() {
		return payload;
	}

	
	public void addNext8Bits(byte b) {
		if (corrupt) return;
		if (numberBytesAdded < MAX_HEADER_SIZE)
			header.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE)
			payload.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE)
			; //fecTrailer.addNext8Bits(b); //FEC ;
		else
			Log.println("ERROR: attempt to add byte past end of frame");

		bytes[numberBytesAdded] = b;
		numberBytesAdded++;

		if (numberBytesAdded == MAX_HEADER_SIZE) {
			// Then we 
			header.copyBitsToFields();
			if (Config.debugFrames) Log.println(header.toString());
			int type = header.type;
			fox = (FoxSpacecraft) Config.satManager.getSpacecraft(header.id);
			if (fox != null) {
				if (type == FramePart.TYPE_REAL_TIME) payload = new PayloadRtValues(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
				if (type == FramePart.TYPE_MAX_VALUES) payload = new PayloadMaxValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MAX_LAYOUT));
				if (type == FramePart.TYPE_MIN_VALUES) payload = new PayloadMinValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MIN_LAYOUT));
				if (type == FramePart.TYPE_RAD_EXP_DATA) payload = new PayloadRadExpData(Config.satManager.getLayoutByName(header.id, Spacecraft.RAD_LAYOUT));
				if (type == FramePart.TYPE_DEBUG || type > FramePart.TYPE_RAD_EXP_DATA) {
					Log.println("INVALID payload type:"+type+", rejecting as corrupt");
					corrupt = true;
				}
			} else {
				//Log.errorDialog("Missing or Invalid Fox Id", 
				Log.println("FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");
				corrupt = true;
			}
		}
		
		if (numberBytesAdded == MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE) {
			payload.copyBitsToFields();
		}
	}
	
	public static int getMaxBytes() {
		return MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE;
	}
	
	public String toString() {
		String s = "AMSAT FOX-1 Telemetry Captured at: " + getStpDate() + "\n"; 
		return s + header.toString() 
				+ "\n\n"+ payload.toString() + "\n"; //\n"+ fecTrailer.toString() + "\n";
	}

}
