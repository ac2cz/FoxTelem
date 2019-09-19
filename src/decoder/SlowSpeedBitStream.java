package decoder;

import common.Config;
import common.Log;
import telemetry.FrameProcessException;
import telemetry.SlowSpeedFrame;
import fec.RsCodeWord;
import telemServer.StpFileProcessException;

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
 */
@SuppressWarnings("serial")
public class SlowSpeedBitStream extends FoxBitStream {
	public static int SLOW_SPEED_SYNC_WORD_DISTANCE = 970; // 10*(SlowSpeedFrame.getMaxBytes())+SYNC_WORD_LENGTH; // Also note this is the default value, but the actual is loaded from the config file
	
	public SlowSpeedBitStream(Decoder dec, int wordLength, int syncWordLength) {
		super(SLOW_SPEED_SYNC_WORD_DISTANCE*5, wordLength, syncWordLength, dec);
		SYNC_WORD_DISTANCE = SLOW_SPEED_SYNC_WORD_DISTANCE;
		PURGE_THRESHOLD = SYNC_WORD_DISTANCE * 3;
		SYNC_WORD_BIT_TOLERANCE = 10;
	}
	
	/**
	 * Given a section of the bit stream between two sync words, attempt to decode it
	 */
	public SlowSpeedFrame decodeFrame(int start, int end, int missedBits, int repairPosition ) {
		boolean insertedMissedBits = false;
		byte[] rawFrame = new byte[SlowSpeedFrame.getMaxBytes()]; // The decoded 8b bytes ready to be passed to the fec decoder
		int[] erasurePositions = new int[SlowSpeedFrame.getMaxBytes()];
		int numberOfErasures = 0;
		
		if (rawFrame.length != SYNC_WORD_DISTANCE/10-1)
			Log.println("ERROR: Frame length " + rawFrame.length + " bytes is different to SYNC word distance "+ (SYNC_WORD_DISTANCE/10-1));
		

		// We have found a frame, so process it. start is the first bit of data
		// end is the first bit after the second SYNC word.  We do not 
		// want to pass the SYNC word to the FRAME, so we process all the 
		// bits up to but not including end-SYNC_WORD_LENGTH.
		int f=0; // position in the frame as we decode it

		for (int j=start; j< end-SYNC_WORD_LENGTH; j+=10) {
			if (Config.insertMissingBits && !insertedMissedBits && missedBits > 0 && j >= repairPosition) {
				if (Config.debugFrames) {
					Log.println("INSERTED "+ missedBits + " missed bits at " + repairPosition);
					Log.println("RS Codeword byte: " + f);
				}
				j = j-missedBits;
				insertedMissedBits = true;
			}
			byte b8 = -1;
			lastErasureNumber = numberOfErasures;
			if (numberOfErasures < MAX_ERASURES) // otherwise we can fast forward to end of this frame, it is bad
				try {
					b8 = processWord(j);
				} catch (LookupException e) {
					if (Config.useRSerasures) {
						// This is an invalid word, so process an erasure
						// Put the position in the erasurePositions array
						erasurePositions[numberOfErasures] = f;
						numberOfErasures++;
					}
				} 
			else {
				if (Config.debugFrames) Log.println(".. abandonded, too many erasures");
				return null;		
			}
			rawFrame[f++] = b8;
		}


		if (numberOfErasures < MAX_ERASURES) { 
			// Initialize a code word with the raw data and the amount of pre-padding required for our partial code words
			RsCodeWord rs = new RsCodeWord(rawFrame, RsCodeWord.DATA_BYTES-SlowSpeedFrame.MAX_HEADER_SIZE-SlowSpeedFrame.MAX_PAYLOAD_SIZE /*159*/);
			if (Config.useRSfec) {								
				if (Config.useRSerasures) 
					rs.setErasurePositions(erasurePositions, numberOfErasures);
				rawFrame = rs.decode();
				lastErrorsNumber = rs.getNumberOfCorrections();
			}
			if (rs.validDecode()) {
				SlowSpeedFrame slowSpeedFrame = new SlowSpeedFrame();

				try {
					slowSpeedFrame.addRawFrame(rawFrame);
				} catch (FrameProcessException e) {
					// The FoxId is corrupt, frame should not be decoded.  RS has actually failed
					return null;
				}

				return slowSpeedFrame;
			} else {
				if (Config.debugFrames) Log.println(".. abandonded, failed RS decode");
				return null;
			}
		} else {
			Log.println("FAIL: " + numberOfErasures + " exceeds max erasures: " + MAX_ERASURES);
			return null; // this was a bad frame
		}
	}
	
}
