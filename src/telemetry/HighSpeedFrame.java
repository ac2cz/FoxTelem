package telemetry;

import common.Config;
import common.FoxSpacecraft;

import java.io.BufferedReader;
import java.io.IOException;

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
public class HighSpeedFrame extends Frame {
	
	public static final int MAX_HEADER_SIZE = 6;
	public static final int PAYLOAD_SIZE = 60;
	public static final int MAX_PAYLOAD_SIZE = 4600 - MAX_HEADER_SIZE; 
	public static final int MAX_CAMERA_PAYLOAD_SIZE = 4300;
	public static final int MAX_TRAILER_SIZE = HighSpeedTrailer.MAX_BYTES; // Multiple RS Codewords with 32 FEC bytes each
	
	// FIXME - we never have 76 rad payloads.  I think there is a maximum in the IHU that is not exceeded?  Do we really
	// need to hold this much space?
	public static final int DEFAULT_RAD_EXP_PAYLOADS = 76; // Default number to fill up frame if there is no camera data
	public static final int MAX_HERCI_PAYLOADS = 5;
	
	//HighSpeedHeader header = null;
	FoxFramePart rtPayload = null;
	FoxFramePart maxPayload = null;
	FoxFramePart minPayload = null;
	PayloadRadExpData[] radExpPayload = new PayloadRadExpData[DEFAULT_RAD_EXP_PAYLOADS];
	PayloadCameraScanLineCount cameraScanLineCount = null;
	PayloadCameraData cameraPayload = null;
	PayloadHerciLineCount herciLineCount = null;
	PayloadHERCIhighSpeed[] herciPayload = null;
	
	HighSpeedTrailer trailer = null;

	int[] radiationLookaheadBuffer = new int[20]; // a 20 byte buffer to check if this frame contains rad telemetry data
	int radiationPayloadSize = 20;  // default size expects buffered mode to be off
	boolean bufferedRadMode = true; // assume we are in rad mode unless we find zero bytes

	int numberBytesAdded = 0;
	int radiationBytesAdded = 0;  // count of the number of vulcan or HERCI bytes that we have added
	private boolean first20RadBytes = true; // true while we are reading the first 20 bytes of rad data
	int radFrame = 0;  // the number of Vulcan or HERCI experiment payloads added to this frame so far
	private boolean firstNonHeaderByte = true;
	private boolean firstCameraByte = true;
	private boolean firstHERCIByte = true;
	
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
	public PayloadHERCIhighSpeed[] getHerciPayloads() { return herciPayload; }
	public PayloadRadExpData[] getRadPayloads() { return radExpPayload; }
	
	/**
	 *  Add the next decoded byte to the high speed frame. These are valid bytes after the RsDecode has been run
	 *  Somehow we need to know if we have hit the end of the jpeg data, so we need to know how the
	 *  8 bit words are aligned in the camera data.... Or we can allocate it and ask the camerFrame
	 *  to tell us when the END word is found, then we start allocating to the rad data frames
	 */
	public void addNext8Bits(byte b) {
		if (corrupt) return;
		if (numberBytesAdded < MAX_HEADER_SIZE)
			header.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE) {
			if (firstNonHeaderByte) {
				header.copyBitsToFields(); // make sure the id is populated
				fox = (FoxSpacecraft) Config.satManager.getSpacecraft(header.id);
				if (fox != null) {
					rtPayload = new PayloadRtValues(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
					maxPayload = new PayloadMaxValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MAX_LAYOUT));
					minPayload = new PayloadMinValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MIN_LAYOUT));
					for (int i=0; i < DEFAULT_RAD_EXP_PAYLOADS; i++)
						radExpPayload[i] = new PayloadRadExpData(Config.satManager.getLayoutByName(header.id, Spacecraft.RAD_LAYOUT));				
					if (Config.debugFrames)
						Log.println(header.toString());
					if (fox.hasCamera())
						cameraScanLineCount = new PayloadCameraScanLineCount();
					if (fox.hasHerci())
						herciLineCount = new PayloadHerciLineCount(); 
				} else {
					Log.println("FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");
					corrupt = true;
					return;
				}
				firstNonHeaderByte = false;
			}
			rtPayload.addNext8Bits(b);
		} else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*2)
			maxPayload.addNext8Bits(b);
		else if (numberBytesAdded < MAX_HEADER_SIZE + PAYLOAD_SIZE*3)
			minPayload.addNext8Bits(b);
		else if (fox.hasCamera() && !cameraScanLineCount.foundEndOfJpegData() && numberBytesAdded < MAX_HEADER_SIZE + MAX_CAMERA_PAYLOAD_SIZE) {
			if (firstCameraByte) {
				cameraScanLineCount.addNext8Bits(b);
				if (!cameraScanLineCount.foundEndOfJpegData()) {
					cameraPayload = new PayloadCameraData(cameraScanLineCount.getScanLineCount());
				} 
				if(Config.debugHerciFrames) Log.println("CAMERA Lines to Decode: " + cameraScanLineCount.getScanLineCount());
				firstCameraByte = false;
			} else {
				cameraPayload.addNext8Bits(b);
				if (cameraPayload.foundEndOfJpegData()) {
					cameraScanLineCount.setFoundEndOfJpegData();
					if(Config.debugFrames && fox.hasCamera())
						Log.println(cameraPayload.toString());
				}
			}
		} else if (fox.hasHerci() && herciLineCount.hasData() && cameraScanLineCount.getScanLineCount() == 0 
				&& numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE && radFrame < MAX_HERCI_PAYLOADS) {
			if (firstHERCIByte) {
				herciLineCount.addNext8Bits(b);
				if(Config.debugHerciFrames) Log.println("HERCI HS Frames to Decode: " + herciLineCount.getLineCount());
				if (herciLineCount.getLineCount() <= MAX_HERCI_PAYLOADS) {
					if (herciLineCount.hasData()) {
						herciPayload = new PayloadHERCIhighSpeed[herciLineCount.getLineCount()];
						for (int i=0; i < herciLineCount.getLineCount(); i++)
							herciPayload[i] = new PayloadHERCIhighSpeed(Config.satManager.getLayoutByName(header.id, Spacecraft.HERCI_HS_LAYOUT));	
					}
				} else {
					// This looks like a corrupt frame, set the linecount to zero so that we do not process it
					herciLineCount.zeroLineCount();
				}
				firstHERCIByte = false;
			} else {
				//Log.print(b + " ");
				herciPayload[radFrame].addNext8Bits(b);	
				radiationBytesAdded++;
				if (radiationBytesAdded == PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE) {
					radiationBytesAdded = 0;
					if(Config.debugHerciFrames) 
						Log.println(herciPayload[radFrame].toString());
					radFrame++;
				}
				if (radFrame == herciLineCount.getLineCount()) {
					radFrame = MAX_HERCI_PAYLOADS; // we have added all of the data, so terminate the loop
					if(Config.debugFrames && fox.hasHerci())
						Log.println("HERCI HIGH SPEED PAYLOADS DECODED [" +herciLineCount.getLineCount()+ "]");
				}
			}
		} else if (!fox.hasHerci() && numberBytesAdded < MAX_HEADER_SIZE + MAX_PAYLOAD_SIZE && radFrame < DEFAULT_RAD_EXP_PAYLOADS) {
			radExpPayload[radFrame].addNext8Bits(b);
			if (first20RadBytes)
				radiationLookaheadBuffer[radiationBytesAdded] = b;
			radiationBytesAdded++;
			if (first20RadBytes && radiationBytesAdded == RadiationTelemetry.TELEM_BYTES ) {
				// We have the first 20 bytes of the experiment data.  We analyze it and try to "guess" if it is Telemetry or Packets
				// If the data contains zeros it is definitely telemetry
				// If the data contains no zeros AND 0x7E markers then it is most likely packets
				// If it has no zeros and no 0x7E markets then we don't know, so we default to telemetry
				first20RadBytes = false;
				boolean foundZero = false;
				boolean foundMarker = false;
				for (int j=0; j < radiationLookaheadBuffer.length; j++) {
					if (radiationLookaheadBuffer[j] == 0) foundZero = true;
					if (radiationLookaheadBuffer[j] == 0x7E) foundMarker = true;
				}
				if (foundZero)
					bufferedRadMode = false;
				else
					if (!foundZero && foundMarker)
						bufferedRadMode = true;
					else
						bufferedRadMode = false;
			}
			if ((!bufferedRadMode && radiationBytesAdded == RadiationTelemetry.TELEM_BYTES) || 
			     (bufferedRadMode && radiationBytesAdded == PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE)) {
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
				"\n\n"+ minPayload.toString() +
				"\n\n"+ cameraPayload.toString();
		
		s = s + "\n\n" + radExpPayload.length + " RADIATION FRAMES:\n\n";
		if (radExpPayload != null)
			for (int i=0; i < radExpPayload.length; i++) {
				if (radExpPayload[i].hasData())
				s = s + "\nRadation Payload [" + i + "]\n"+ radExpPayload[i].toString() +
				"\n"; 
			}
		if(fox.hasHerci())
			s = s + "\n\n" + herciPayload.toString();
		
		return s;
	}

}