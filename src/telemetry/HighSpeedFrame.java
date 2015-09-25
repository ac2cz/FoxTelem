package telemetry;

import common.Config;
import java.io.BufferedReader;
import java.io.IOException;

import common.Log;

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
public class HighSpeedFrame extends Frame {
	
	public static final int MAX_HEADER_SIZE = 6;
	public static final int PAYLOAD_SIZE = 60;
	public static final int MAX_PAYLOAD_SIZE = 4600 - MAX_HEADER_SIZE; 
	public static final int MAX_CAMERA_PAYLOAD_SIZE = 4300;
	public static final int MAX_TRAILER_SIZE = HighSpeedTrailer.MAX_BYTES; // Multiple RS Codewords with 32 FEC bytes each
	
	// FIXME - we never have 76 rad payloads.  I think there is a maximum in the IHU that is not exceeded?  Do we really
	// need to hold this much space?
	public static final int DEFAULT_RAD_EXP_PAYLOADS = 76; // Default number to fill up frame if there is no camera data
	
	//HighSpeedHeader header = null;
	FramePart rtPayload = null;
	FramePart maxPayload = null;
	FramePart minPayload = null;
	PayloadRadExpData[] radExpPayload = new PayloadRadExpData[DEFAULT_RAD_EXP_PAYLOADS];
	PayloadCameraData cameraPayload = null;
	HighSpeedTrailer trailer = null;

	int numberBytesAdded = 0;
	int radiationBytesAdded = 0;
	int radFrame = 0;
	private boolean firstNonHeaderByte = true;
	
	public HighSpeedFrame() {
		super();
		header = new HighSpeedHeader();
		trailer = new HighSpeedTrailer();
		bytes = new byte[MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE];
	}

	public HighSpeedFrame(BufferedReader input) throws IOException {
		super(input);
	}
	
	public HighSpeedHeader getHeader() { return (HighSpeedHeader)header; }
	public PayloadRtValues getRtPayload() { return (PayloadRtValues) rtPayload; }
	public PayloadMaxValues getMaxPayload() { return (PayloadMaxValues) maxPayload; }
	public PayloadMinValues getMinPayload() { return (PayloadMinValues) minPayload; }
	public PayloadCameraData getCameraPayload() { return cameraPayload; }
	public PayloadRadExpData[] getRadPayloads() { return radExpPayload; }
	
	/**
	 *  Add the next decoded byte to the high speed frame. These are valid bytes after the RsDecode has been run
	 *  Somehow we need to know if we have hit the end of the jpeg data, so we need to know how the
	 *  8 bit words are aligned in the camera data.... Or we can allocate it and ask the camerFrame
	 *  to tell us when the END word is found, then we start allocating to the rad data frames
	 */
	public void addNext8Bits(byte b) {
		if (numberBytesAdded < MAX_HEADER_SIZE)
			header.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE) {
			if (firstNonHeaderByte) {
				header.copyBitsToFields(); // make sure the id is populated
				fox = Config.satManager.getSpacecraft(header.id);
				if (fox != null) {
					rtPayload = new PayloadRtValues(Config.satManager.getRtLayout(header.id));
					maxPayload = new PayloadMaxValues(Config.satManager.getMaxLayout(header.id));
					minPayload = new PayloadMinValues(Config.satManager.getMinLayout(header.id));
					for (int i=0; i < DEFAULT_RAD_EXP_PAYLOADS; i++)
						radExpPayload[i] = new PayloadRadExpData(Config.satManager.getRadLayout(header.id));				
					if (Config.debugFrames)
						Log.println(header.toString());
					if (fox.hasCamera())
						cameraPayload = new PayloadCameraData();
				} else {
					Log.errorDialog("Missing or Invalid Fox Id", "FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");
				}
				firstNonHeaderByte = false;
			}
			rtPayload.addNext8Bits(b);
		} else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*2)
			maxPayload.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*3)
			minPayload.addNext8Bits(b);
		else if (fox.hasCamera() && !cameraPayload.foundEndOfJpegData() && numberBytesAdded < MAX_HEADER_SIZE + MAX_CAMERA_PAYLOAD_SIZE) {
			cameraPayload.addNext8Bits(b);
			if (cameraPayload.foundEndOfJpegData() && Config.debugFrames && fox.hasCamera())
				Log.println(cameraPayload.toString());
		} else if (numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE && radFrame < DEFAULT_RAD_EXP_PAYLOADS) {
			
			/*
			if (radExpPayload == null) {
				// This is the first byte of the radiation data, so we know the size left
				radiationFrameMax = getMaxDataBytes() - numberBytesAdded;
				radiationFrames = radiationFrameMax / PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE;
				Log.println("Expecting " + radiationFrames + " radiation frames in high speed data");
				//////////////// Init one frame for now
				radExpPayload = new PayloadRadExpData(PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE);
			}
			*/
			radExpPayload[radFrame].addNext8Bits(b);	
			radiationBytesAdded++;
			if (radiationBytesAdded == PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE) {
				radiationBytesAdded = 0;
				radFrame++;
			}
			
		} else if (numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE + MAX_TRAILER_SIZE)
			;//trailer.addNext8Bits(b); //FEC ;
		else
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
	
	public String toString() {
		String s = new String();
		s = "\n" + header.toString() + 
				"\n\n"+ rtPayload.toString() + 
				"\n\n"+ maxPayload.toString() +
				"\n\n"+ minPayload.toString();
				//"\n\n"+ cameraPayload.toString();
		
		s = s + "\n\n" + radExpPayload.length + " RADIATION FRAMES:\n\n";
		if (radExpPayload != null)
			for (int i=0; i < radExpPayload.length; i++) {
				if (radExpPayload[i].hasData())
				s = s + "\nRadation Payload [" + i + "]\n"+ radExpPayload[i].toString() +
				"\n"; 
			}
		
		return s;
	}

}