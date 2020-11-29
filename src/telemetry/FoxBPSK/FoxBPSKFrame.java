package telemetry.FoxBPSK;

import common.Config;
import common.FoxSpacecraft;

import java.io.BufferedReader;
import java.io.IOException;
import common.Log;
import common.Spacecraft;
import decoder.Crc32;
import decoder.Decoder;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FoxPayloadStore;
import telemetry.Frame;
import telemetry.FrameLayout;
import telemetry.FramePart;
import telemetry.HighSpeedTrailer;
import telemetry.PayloadUwExperiment;
import telemetry.PayloadWOD;
import telemetry.PayloadWODUwExperiment;
import telemetry.TelemFormat;

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
		
		FrameLayout frameLayout;
		TelemFormat telemFormat;
		public FramePart[] payload;
		HighSpeedTrailer trailer = null;
		byte[] headerBytes;
		byte[] dataBytes;
		BitArrayLayout headerLayout;
		int numberBytesAdded = 0;
		int c = 0;
		int crc = 0;
		int crcLength = 4; // bytes
		byte[] crcBytes = new byte[crcLength];
		
		/**
		 * Initialize the frame.  At this point we do not know which spacecraft it is for.  We reserve enough bytes for the header.
		 * Once the header is decoded we can allocate the rest of the bytes for the frame
		 */
		public FoxBPSKFrame(TelemFormat telemFormat) {
			super();
			this.telemFormat = telemFormat;
			headerLayout = telemFormat.getHeaderLayout();
			//header = new FoxBPSKHeader(headerLayout, telemFormat);
			headerBytes = new byte[telemFormat.getInt(TelemFormat.HEADER_LENGTH)];
		}

		public FoxBPSKFrame(TelemFormat telemFormat, BufferedReader input) throws IOException {
			super(input);
			this.telemFormat = telemFormat;
			headerLayout = telemFormat.getHeaderLayout();
			header = new FoxBPSKHeader(headerLayout, telemFormat);
			headerBytes = new byte[telemFormat.getInt(TelemFormat.HEADER_LENGTH)];
			load(input);
		}
		
		public FoxBPSKHeader getHeader() { return (FoxBPSKHeader)header; }
		
		int debugCount = 0;
		public void addNext8Bits(byte b) {
			if (Config.debugBytes) {
				String debug = (Decoder.plainhex(b));
				debugCount++;
				Log.print("0x" + debug + ",");
				if (debugCount % 20 == 0) Log.println("");
			}

			if (corrupt) return;
			if (numberBytesAdded < telemFormat.getInt(TelemFormat.HEADER_LENGTH)) {
				if (header == null)
					header = new FoxBPSKHeader(headerLayout, telemFormat);
				header.addNext8Bits(b);
			} else if (numberBytesAdded == telemFormat.getInt(TelemFormat.HEADER_LENGTH)) {
				// first non header byte
				try {
					header.copyBitsToFields(); // make sure the id is populated
					fox = (FoxSpacecraft) Config.satManager.getSpacecraft(header.id);
				} catch (ArrayIndexOutOfBoundsException e) {
					if (Config.debugFrames)
						Log.errorDialog("ERROR","The header length in the format file may not agree with the header layout.  Decode not possible.\n"
								+ "Turn off Debug Frames to prevent this message in future.");
					else
						Log.println("ERROR: The header length in the format file may not agree with the header layout.  Decode not possible.");							
					corrupt = true;
					return;
				}
				if (fox != null) {
					if (Config.debugFrames)
						Log.println(header.toString());
					frameLayout = Config.satManager.getFrameLayout(header.id, header.getType());
					if (frameLayout == null) {
						if (Config.debugFrames)
							Log.errorDialog("ERROR","FOX ID: " + header.id + " Type: " + header.getType() + " has no frame layout. Decode not possible.\n"
									+ "Turn off Debug Frames to prevent this message in future.");
						else
							Log.println("FOX ID: " + header.id + " Type: " + header.getType() + " has no frame layout. Decode not possible.");
						
						corrupt = true;
						return;
					}
					bytes = new byte[telemFormat.getInt(TelemFormat.FRAME_LENGTH)]; 
					if (fox.hasFrameCrc && Config.calculateBPSKCrc)
						dataBytes = new byte[telemFormat.getInt(TelemFormat.DATA_LENGTH)-crcLength];
					else
						dataBytes = new byte[telemFormat.getInt(TelemFormat.DATA_LENGTH)];
					for (int k=0; k < telemFormat.getInt(TelemFormat.HEADER_LENGTH); k++) {
						bytes[k] = headerBytes[k];
						if (fox.hasFrameCrc && Config.calculateBPSKCrc)
							dataBytes[k] = headerBytes[k];
					}
					initPayloads((FoxBPSKHeader)header, frameLayout);
//					initPayloads(header.id, header.getType());
					if (payload[0] == null) {
						if (Config.debugFrames)
							Log.errorDialog("ERROR","FOX ID: " + header.id + " Type: " + header.getType() + " not valid. "
									+ "Check that the Payloads defined in the MASTER file correctly match the payload names in the .frame definition file.\nDecode not possible.\n"
									+ "Turn off Debug Frames to prevent this message in future.");
						else
							Log.println("FOX ID: " + header.id + " Type: " + header.getType() + " not valid. Check that the Payloads defined in the MASTER file correctly match the payload names in the .frame definition file. Decode not possible.");
						corrupt = true;
						return;
					}
					
				} else {
					if (Config.debugFrames)
						Log.errorDialog("ERROR","FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.\n"
								+ "Turn off Debug Frames to prevent this message in future.");
					else
						Log.println("FOX ID: " + header.id + " is not configured in the spacecraft directory.  Decode not possible.");							
					corrupt = true;
					return;
				}
				payload[0].addNext8Bits(b); // add the first byte to the first payload
				
			/*
			 * This is the start of the section that deals with FRAMES defined in the Frames LAYOUT
			 * 
			 */	
			} else {
				// try to add the byte to a payload, step through each of them
				int maxByte = telemFormat.getInt(TelemFormat.HEADER_LENGTH);
				int minByte = telemFormat.getInt(TelemFormat.HEADER_LENGTH);
				for (int p=0; p < frameLayout.getInt(FrameLayout.NUMBER_OF_PAYLOADS); p++) {
					maxByte += frameLayout.getInt(FrameLayout.PAYLOAD+p+FrameLayout.DOT_LENGTH);
					if (numberBytesAdded >= minByte && numberBytesAdded < maxByte) {
						try {
							payload[p].addNext8Bits(b);
						} catch (Exception e) {
							if (payload[p] != null && payload[p].getLayout() != null)
								Log.errorDialog("ERROR", "Could not add byte number " + numberBytesAdded + " to frame: " + frameLayout
									+ " for payload number " + p + " : " + payload[p].getLayout().name + " at payload byte " + payload[p].numberBytesAdded);
							else if (payload[p] != null && payload[p].getLayout() == null)
								Log.errorDialog("ERROR", "Could not add byte number " + numberBytesAdded + " to frame: " + frameLayout
										+ " for payload number "+ p + " of type " + payload[p].getType() + " at payload byte " + payload[p].numberBytesAdded
										+"\nThis payload's Layout is null and not defined or loaded correctly.");
							else if (payload[p] == null)
								Log.errorDialog("ERROR", "Could not add byte number " + numberBytesAdded + " to frame: " + frameLayout
										+ " for payload number " + p + " because the payload is null." 
										+"\nThe payload Layout is probablly also not defined or loaded correctly.");
							corrupt = true;
							return;
						}
					} 
					minByte += frameLayout.getInt(FrameLayout.PAYLOAD+p+FrameLayout.DOT_LENGTH);
				}
			}
			if (fox != null && fox.hasFrameCrc && Config.calculateBPSKCrc) {
				// Check the CRC on the frame.  This frame has already been error corrected and passed RS Decode
				// This is an extra check to reject frames that erroniously pass the RS Decode.
				if (numberBytesAdded > telemFormat.getInt(TelemFormat.DATA_LENGTH)-crcLength-1  
						&& numberBytesAdded <= telemFormat.getInt(TelemFormat.DATA_LENGTH)-1)
					crcBytes[c++] = b;
				if (numberBytesAdded == telemFormat.getInt(TelemFormat.DATA_LENGTH)-1) {
					crc = Decoder.littleEndian4(crcBytes);
					if (Config.debugBytes || Config.debugFrames) {
						Log.print("=> Frame CRC: " + Decoder.plainhex(crc));
					}
					// Now calculate the CRC for all data bytes received so far
					//int calculatedCrc = crc32.byte2crc32(dataBytes);
					int myCalculatedCrc = Crc32.crc32(dataBytes);
					
					if (Config.debugBytes || Config.debugFrames) {
						//Log.println("=> Sun Calculated CRC: " + Decoder.plainhex(calculatedCrc));
						Log.print(" => Calculated CRC: " + Decoder.plainhex(myCalculatedCrc));
						if (crc == myCalculatedCrc) 
							Log.println(" .. pass");
						else {
							Log.println(" ***** FAIL ***** Frame Rejected");
							Config.passManager.incCrcFailure();
							corrupt = true;
							return;
						}
					}
				}
			}
				
			if (numberBytesAdded >= telemFormat.getInt(TelemFormat.HEADER_LENGTH)) {
				bytes[numberBytesAdded] = b;
				if (fox != null && fox.hasFrameCrc && Config.calculateBPSKCrc && numberBytesAdded < telemFormat.getInt(TelemFormat.DATA_LENGTH)-crcLength)
					dataBytes[numberBytesAdded] = b; 
			} else {
				headerBytes[numberBytesAdded] = b;
			}
			
			numberBytesAdded++;
		}
		
		private void initPayloads(FoxBPSKHeader header, FrameLayout frameLayout) {
			payload = new FoxFramePart[frameLayout.getInt(FrameLayout.NUMBER_OF_PAYLOADS)];
			for (int i=0; i<frameLayout.getInt(FrameLayout.NUMBER_OF_PAYLOADS); i+=1 ) {
				BitArrayLayout layout = Config.satManager.getLayoutByName(header.id, frameLayout.getPayloadName(i));
				if (layout == null) {
					payload[0] = null; // cause us to drop out
					return;
				}
				payload[i] = (FoxFramePart) FramePart.makePayload(header, layout);
			}
		}
		
		
		public boolean savePayloads(FoxPayloadStore payloadStore, boolean storeMode, int newReset) {
			int serial = 0;
			header.copyBitsToFields(); // make sure we have defaulted the extended FoxId correctly
			for (int i=0; i<payload.length; i++ ) {
				if (payload[i] != null) {
					payload[i].copyBitsToFields();
					payload[i].resets = newReset;
					if (storeMode)
						payload[i].newMode = header.newMode;
					if (payload[i] instanceof PayloadUwExperiment) { 
						((PayloadUwExperiment)payload[i]).savePayloads(payloadStore, serial, storeMode);
						serial = serial + ((PayloadUwExperiment)payload[i]).canPackets.size();
					} else if (payload[i] instanceof PayloadWODUwExperiment) { 
						((PayloadWODUwExperiment)payload[i]).savePayloads(payloadStore, serial, storeMode);
						serial = serial + ((PayloadWODUwExperiment)payload[i]).canPackets.size();
					} else
						if (!payloadStore.add(header.getFoxId(), header.getUptime(), newReset, payload[i]))
							return false;
					payload[i].rawBits = null; // free memory associated with the bits
					headerBytes = null; // free memory 
					dataBytes = null; // free memory
				}
			}
			return true;			
		}

		/**
		 * Get a buffer containing all of the CAN Packets in this frame.  There may be multiple payloads that have CAN Packets,
		 * so we need to check all of them.  First we gather the bytes from each payload in the PCAN format.  We return an 
		 * array of those byte arrays.  The calling routine will send each PCAN packet individually
		 */
		public byte[][] getPayloadBytes() {

			byte[][] allBuffers = null;

			Spacecraft sat = Config.satManager.getSpacecraft(foxId);
			if (sat.sendToLocalServer()) {
				int totalBuffers = 0;
				for (int i=0; i< payload.length; i++) {
					// if this payload should be output then add to the byte buffer
					if (payload[i] instanceof PayloadUwExperiment) {
						byte[][] buffer = ((PayloadUwExperiment)payload[i]).getCANPacketBytes(stpDate); 
						totalBuffers += buffer.length; 
					}
					if (payload[i] instanceof PayloadWODUwExperiment) {
						byte[][] buffer = ((PayloadWODUwExperiment)payload[i]).getCANPacketBytes(stpDate); 
						totalBuffers += buffer.length; 
					}
				}
					
				allBuffers = new byte[totalBuffers][];
				int startPosition = 0;
				for (int p=0; p< payload.length; p++) {
					// if this payload should be output then add its byte buffers to the output
					if (payload[p] instanceof PayloadUwExperiment) {
						byte[][] buffer = ((PayloadUwExperiment)payload[p]).getCANPacketBytes(stpDate); 
						for (int j=0; j < buffer.length; j++) {
							allBuffers[j + startPosition] = buffer[j];
						}
						startPosition += buffer.length;
					}
					if (payload[p] instanceof PayloadWODUwExperiment) {
						byte[][] buffer = ((PayloadWODUwExperiment)payload[p]).getCANPacketBytes(stpDate); 
						for (int j=0; j < buffer.length; j++) {
							allBuffers[j + startPosition] = buffer[j];
						}
						startPosition += buffer.length;
					}
				}

			}
			return allBuffers;				
		}
		
		public String toWodTimestampString(int r, long u) {
			String s = new String();			
			if (payload != null) {
				for (int i=0; i < payload.length; i++) {
					if (payload[i] instanceof PayloadWOD) {
						s = s + r + ", " + u + ", ";
						payload[i].copyBitsToFields();
						s = s + payload[i].resets + ", " + payload[i].uptime + "\n";
					}
				}
			} 
			
			return s;
		}

		public String toString() {
			String s = new String();
			s = s + "AMSAT FOX-1 BPSK Telemetry Captured at DATE: " + getStpDate() + "\n"; 
			s = header.toString();
			
			if (payload != null) {
				for (int i=0; i < payload.length; i++) {
					s = s + payload[i].toString() +
					"\n"; 
				}
			} 
			
			return s;
		}
}
