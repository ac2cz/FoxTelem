package decoder;

import common.Config;
import common.Log;
import fec.RsCodeWord;
import telemetry.HighSpeedFrame;

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
 * High Speed Frame BitStream.  The frame needs to be decoded from the bitstream as follows:
 * Initialize a set of RS Code words
 * One bit is added to each RS Code word in a round robin fashion until all code words are full
 * Then one bit is added to the error sections each each rs code word until they too are full.
 * The RS code words are decoded and corrections are made
 * The data is fed into the High Speed Frame, which will then allocate the bits to the appropriate 
 * payloads
 * 
 *
 */
@SuppressWarnings("serial")
public class HighSpeedBitStream extends BitStream {
	public static int HIGH_SPEED_SYNC_WORD_DISTANCE = 52730; // 52790 - 6 bytes of header, 4600 data bytes, 672 parity bytes for 21 code words + 10 bit SYNC word
	public static final int NUMBER_OF_RS_CODEWORDS = 21;
	public static final int[] RS_PADDING = {3,4,4,4,4, 4,4,4,4,4, 4,4,4,4,4, 4,4,4,4,4, 4};
	
	public HighSpeedBitStream() {
		super(HIGH_SPEED_SYNC_WORD_DISTANCE*5);
		SYNC_WORD_DISTANCE = HIGH_SPEED_SYNC_WORD_DISTANCE;
		PURGE_THRESHOLD = SYNC_WORD_DISTANCE * 3;	
	}

	/**
	 * Attempt to decode the High Speed Frame
	 * We need to keep track of a set of rs codewords, each has 223 byts of data 
	 * We allocate the received bytes into the code words round robin
	 * Then we allocate a set of FEC bits, which also get allocated round robin
	 * Then we decode
	 * The corrected data is re-allocated, again round robin, into a rawFrame.  This frame should
	 * then contain the data BACK in the original order, but with corrections made
	 */
	public HighSpeedFrame decodeFrame(int start, int end) {
		RsCodeWord[] codeWords = new RsCodeWord[NUMBER_OF_RS_CODEWORDS];
	
		int bytesInFrame = 0; 
		
		byte[] rawFrame = new byte[HighSpeedFrame.getMaxBytes()];
		
		int[][] erasurePositions = new int[NUMBER_OF_RS_CODEWORDS][];
		for (int q=0; q < NUMBER_OF_RS_CODEWORDS; q++) {
			codeWords[q] = new RsCodeWord(RS_PADDING[q]);
			erasurePositions[q] = new int[RsCodeWord.DATA_BYTES];
		}
		int[] numberOfErasures = new int[NUMBER_OF_RS_CODEWORDS];
		
		if (rawFrame.length != SYNC_WORD_DISTANCE/10-1)
			Log.println("WARNING: Frame length " + rawFrame.length + " bytes is different to default SYNC word distance "+ (SYNC_WORD_DISTANCE/10-1));

		// We have found a frame, so process it. start is the first bit of data
		// end is the first bit after the second SYNC word.  We do not 
		// want to pass the SYNC word to the FRAME, so we process all the 
		// bits up to but not including end-10.
		int f=0; // position in the Rs code words as we allocate bits to them
		int rsNum = 0; // counter that remembers the RS Word we are adding bytes to
		
		// Traverse the bits between the frame markers and allocate the decoded bytes round robin back to the RS Code words
		for (int j=start; j< end-10; j+=10) {

			byte b8 = -1;
			try {
				b8 = processWord(j);
			} catch (LookupException e) {
				
				if (Config.useRSerasures) {
					// This is an invalid word, so process an erasure
					// Put the position in the erasurePositions array
					if (numberOfErasures[rsNum] < MAX_ERASURES) {
						erasurePositions[rsNum][numberOfErasures[rsNum]] = f;
						numberOfErasures[rsNum]++;
					} else {
						return null;
					}
				}
			}
			bytesInFrame++;

			if (bytesInFrame == 4601) {  //FIXME - hard coded value
				// first parity byte
				//Log.println("parity");
				// Reset to the first code word
				rsNum = 0;
				//Next byte position in the codewords
				f++;
			}
		
			try {
			codeWords[rsNum++].addByte(b8);
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace(Log.getWriter());
			}
			if (rsNum == NUMBER_OF_RS_CODEWORDS) {
				rsNum=0;
				f++;
				if (f > RsCodeWord.NN)
					Log.println("ERROR: Allocated more high speed data that fits in an RSCodeWord");
			}
		}

		if (Config.debugFrames)
			Log.println("CAPTURED " + bytesInFrame + " high speed bytes");
		
		

		// Now Decode all of the RS words and put the bytes back into the 
		// order we started with, but now with corrected data
		//byte[] correctedBytes = new byte[RsCodeWord.DATA_BYTES];
		for (int i=0; i < NUMBER_OF_RS_CODEWORDS; i++) {
			if (numberOfErasures[i] < MAX_ERASURES) {
//				lastErasureNumber += numberOfErasures[rsNum];
				if (Config.useRSfec) {								
					if (Config.useRSerasures) codeWords[rsNum].setErasurePositions(erasurePositions[i], numberOfErasures[i]);
					codeWords[i].decode();  
//					lastErrorsNumber += codeWords[i].getNumberOfCorrections();
					if (!codeWords[i].validDecode()) {
						// We had a failure to decode, so the frame is corrupt
						Log.println("FAILED RS DECODE FOR HS WORD " + i);
//						lastErrorsNumber = lastErrorsNumber / i;
//						lastErasureNumber = lastErasureNumber /i;
						return null;
					} else {
						//Log.println("RS Decoder Successful for HS Data");
						// Consume all of the bits up to this point, but not the end SYNC word
						//removeBits(0, end-10);
					}
				}
			} else {
				if (Config.debugFrames) Log.println("Too many erasures, failure to decode");
//				lastErrorsNumber = lastErrorsNumber / i;
//				lastErasureNumber = lastErasureNumber /i;
				return null;
			}
		}
//		lastErrorsNumber = lastErrorsNumber / NUMBER_OF_RS_CODEWORDS;
//		lastErasureNumber = lastErasureNumber /NUMBER_OF_RS_CODEWORDS;
		// Consume all of the bits up to this point, but not the end SYNC word
		removeBits(0, end-10);

		f=0;
		rsNum=0;
		// We have corrected the bytes, now allocate back to the rawFrame and add to the frame
		for (int i=0; i < bytesInFrame; i++) {
			try {
				rawFrame[i] = codeWords[rsNum++].getByte(f);
			} catch (IndexOutOfBoundsException e) {
				Log.println(e.getMessage());
				if (Config.useRSfec)
					return null;
				else {
					// return what we have, this is a run to grab raw data for debugging
					HighSpeedFrame highSpeedFrame = new HighSpeedFrame();
					highSpeedFrame.addRawFrame(rawFrame);
					return highSpeedFrame;					
				}
			}
			if (rsNum == NUMBER_OF_RS_CODEWORDS) {
				rsNum=0;
				f++;
				if (f > RsCodeWord.NN)
					Log.println("ERROR: Allocated more high speed data than fits in an RSCodeWord");
			}
		}
		HighSpeedFrame highSpeedFrame = new HighSpeedFrame();
		highSpeedFrame.addRawFrame(rawFrame);
		return highSpeedFrame;
		
	}

}