package telemetry.legacyPayloads;

import java.util.ArrayList;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.frames.HighSpeedFrame;

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
 * This is a camera payload.  It is part of an image and is downloaded in the High Speed frame if the satellite has a camera.
 * Each payload contains a number of lines, defined to be 0-59 in the ICD, but the number is passed and is typically 3, but we handle any value.
 * The line is a 640 x 8 pixel line typically, but we handle any length of line because that is also passed.
 * 
 * The first byte specifies the number of lines.  The lines are variable length we read the line headers and then the lines
 * in a loop until all of the lines have been read.
 * 
 * The lines represent the compressed JPEG image data without the header. 
 * 
 *
 */
@Deprecated
public class PayloadCameraData extends FramePart {
	//public static final int TYPE = TYPE_CAMERA_DATA;
	public static final byte END_OF_JPEG_DATA = -86; //0xaa;
	public static final int LINE_HEADER_BYTES = 9; // include pre-amble 3; // bytes 0, 1, 2
	private int headerByte = 0;
	private int lineByte = 0;
	boolean firstByte = false;
	PictureScanLine currentScanLine;
	private int currentScanLineCount = 0;
	
	int[] cameraData = new int[MAX_BYTES];
	int scanLineCount = 0;
	
	public ArrayList<PictureScanLine> pictureLines; // we dont know how many of these so use an array list
	int linesAdded = 0;
	private boolean foundEndOfJpegData = false;
	
	public PayloadCameraData(int slc) {
		super(TYPE_CAMERA_DATA, new BitArrayLayout());
		
		MAX_BYTES = HighSpeedFrame.MAX_CAMERA_PAYLOAD_SIZE;
		rawBits = new boolean[MAX_BYTES*8];
		
		setupScanLines(slc);
	}
	
	public PayloadCameraData(int id, int resets, long uptime, String date, StringTokenizer st) {
		super(id, resets, uptime, TYPE_CAMERA_DATA, date, st, new BitArrayLayout());
		MAX_BYTES = HighSpeedFrame.MAX_CAMERA_PAYLOAD_SIZE;
	}
	
	protected void init() { }
	
	public boolean foundEndOfJpegData() { return foundEndOfJpegData; }

	private void setupScanLines(int slc) {
		firstByte = false;
		scanLineCount = slc;
		resetBitPosition();
		pictureLines = new ArrayList<PictureScanLine>();
		if (scanLineCount == 0) 
			foundEndOfJpegData = true;
		headerByte = 0;
		currentScanLineCount = 0;
		if (Config.debugCameraFrames)
			Log.print("SLC: " + scanLineCount + " ");			
		
	}
	
	public void addNext8Bits(byte b) {
		
		super.addNext8Bits(b);
		if (firstByte) { // very first byte of the type 5 payload is the scan line count
			firstByte = false;
			resetBitPosition();
			pictureLines = new ArrayList<PictureScanLine>();
			scanLineCount = nextbits(8);
			if (scanLineCount == 0) 
				foundEndOfJpegData = true;
			headerByte = 0;
			currentScanLineCount = 0;
			if (Config.debugCameraFrames)
				Log.print("SLC: " + scanLineCount + " ");			
		} else {
			headerByte++;
			if (headerByte == LINE_HEADER_BYTES) {// End of the line header
				@SuppressWarnings("unused")
				int preamble = 0;
				for (int p=0; p<6; p++)
					preamble = nextbits(8);
				int pictureCounter = nextbits(8); // 8 bits unsigned - picture count indicator
				int descriptor = nextbits(8); 
				int descriptor2 = nextbits(8); 
				
				int scanLineLength = ((descriptor & 0x3) << 8) + (descriptor2); // 2 lsb of first byte and the second byte together
				int scanLineNumber = descriptor >> 2; // we want the 6 msbs
				
				int[] scanLineData = new int[scanLineLength];				
				currentScanLine = new PictureScanLine(FramePart.fileDateStamp(), pictureCounter, scanLineNumber, scanLineLength, scanLineData);
				pictureLines.add(currentScanLine);
				lineByte = 0;

			} else if (headerByte > (LINE_HEADER_BYTES) && lineByte < (currentScanLine.scanLineLength-1)) {
				if (Config.debugCameraFrames)
				Log.print("PC:" + currentScanLine.pictureCounter + " SL:" + currentScanLine.scanLineNumber + " SLL:" + currentScanLine.scanLineLength + " Byte Num:" + lineByte + " ");
				currentScanLine.scanLineData[lineByte++] = nextbits(8);
			} else if (headerByte > (LINE_HEADER_BYTES) && lineByte == (currentScanLine.scanLineLength-1)) {
				if (Config.debugCameraFrames)
					Log.print("PC:" + currentScanLine.pictureCounter + " SL:" + currentScanLine.scanLineNumber + " SLL:" + currentScanLine.scanLineLength + " Byte Num:" + lineByte + " ");
				currentScanLine.scanLineData[lineByte] = nextbits(8); // last byte on this line
				currentScanLineCount++;
				headerByte = 0;
				lineByte=0;
				if (currentScanLineCount == scanLineCount) { // this was the last line to be added
					//Log.println("ADDED: " + currentScanLineCount + " LINES");
					foundEndOfJpegData = true;
				}
			}
		}
		
		if (Config.debugCameraFrames) {
			Log.print(FoxDecoder.hex(b) + ": ");
			FoxBitStream.printBitArray(FoxBitStream.intToBin8(b));
		}
		if (numberBytesAdded == HighSpeedFrame.MAX_CAMERA_PAYLOAD_SIZE) {
			foundEndOfJpegData = true;
		}
	}

	
	@Override
	public void copyBitsToFields() {
		// Nothing to do here as bits are put into the structure as they are received
	}

	public ArrayList<PictureScanLine> getPictureLines() { return pictureLines; }
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = "JPEG CAMERA IMGAGE - " + pictureLines.size() + " picture lines\n";
		for (PictureScanLine line : pictureLines) {
			s = s + line.toShortString() + "\n";
		}
		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Write the data out to a file.  We use the following format:
	 * Header then a line from the camera.  This is repeated for all lines.  We use the header information to remember which
	 * lines went together when we downloaded.
	 * Each picture scan line has the pictureCounter, scanLineNumber, scanLineLength, then a set of bytes
	 * 
	 */
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		for (PictureScanLine line : pictureLines) {
			s = s + reportDate + "," + id + "," + resets + "," + uptime + "," + type + ",";
			s = s + line.toString() + "\n";
		}
		return s;
	}
}
