package FuncubeDecoder;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.FramePart;


/**
* FOX 1 Telemetry Decoder
* 
* @author chris.e.thompson g0kla/ac2cz
*
*         Copyright (C) 2015 amsat.org
*
*         This program is free software: you can redistribute it and/or modify
*         it under the terms of the GNU General Public License as published by
*         the Free Software Foundation, either version 3 of the License, or (at
*         your option) any later version.
*
*         This program is distributed in the hope that it will be useful, but
*         WITHOUT ANY WARRANTY; without even the implied warranty of
*         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
*         General Public License for more details.
*
*         You should have received a copy of the GNU General Public License
*         along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*
*         The base class for FUNCUBE telemetry frames. 
* 
*
*/
public class FUNcubeFrame {
	
	
	public static final int MAX_HEADER_SIZE = 1;
	public static final int MAX_FRAME_SIZE = 256;

	public static final int MAX_RT_TELEMETRY_SIZE = 55;
	public static final int MAX_WHOLE_ORBIT_DATA_SIZE = 23;
	public static final int MAX_HIGH_RES_TELEMETRY_SIZE = 10;
	public static final int MAX_FITTER_MESSAGE_SIZE = 200;
	
	FUNcubeHeader header = null;
	PayloadRealTime rtPayload = null;
	FramePart payload = null;
	FUNcubeSpacecraft funCube;
	
	byte[] bytes;
	int numberBytesAdded = 0;
	boolean corrupt;
	
	public FUNcubeFrame() {
		header = new FUNcubeHeader();
		bytes = new byte[MAX_FRAME_SIZE];
	}
	
	public void addRawFrame(byte[] b) {
		int byteLen = b.length;

		for (int i = 0; i < byteLen; i++)
			addNext8Bits(b[i]);

	}
	@SuppressWarnings("deprecation")
	public void addNext8Bits(byte b) {
		if (corrupt) return;
		if (numberBytesAdded < MAX_HEADER_SIZE)
			header.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + PayloadRealTime.MAX_RT_PAYLOAD_SIZE)
			rtPayload.addNext8Bits(b);
		else if (numberBytesAdded < MAX_FRAME_SIZE)
			; // add to the payload
		else
			Log.println("ERROR: attempt to add byte past end of frame");

		bytes[numberBytesAdded] = b;
		numberBytesAdded++;

		if (numberBytesAdded == MAX_HEADER_SIZE) {
			// Then we decode the type
			header.copyBitsToFields();
			if (Config.debugFrames) Log.println("DECODING FUNCUBE PAYLOAD TYPE: " + header.type);
			int type = header.type;
			header.id = FUNcubeSpacecraft.FUNCUBE_ID; //// TO DO HARD CODED SO THAT IT GOES TO THE RIGHT PAYLOAD STORE
			funCube = (FUNcubeSpacecraft)Config.satManager.getSpacecraft(FUNcubeSpacecraft.FUNCUBE_ID);
			if (funCube != null) {
				rtPayload = new PayloadRealTime(funCube.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT));
				payload = funCube.getPayloadByType(type);
				if (payload == null) {
					//Log.errorDialog("Missing or Invalid Fox Id", 
					Log.println("FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");
					corrupt = true;
				}
			}
		}
		
		if (numberBytesAdded == MAX_FRAME_SIZE) {
			rtPayload.copyBitsToFields();
		}
	}
	
	
	
	public static int getMaxBytes() {
		return MAX_FRAME_SIZE;
	}
	
	public String toString() {
		return "\n" + header.toString() 
				+ "\n\n"+ payload.toString() + "\n"; 
	}

}
