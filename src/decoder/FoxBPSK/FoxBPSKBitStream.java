package decoder.FoxBPSK;

import common.Config;
import common.Log;
import decoder.Decoder;
import decoder.FoxBitStream;
import decoder.HighSpeedBitStream;
import decoder.LookupException;
import telemetry.Frame;
import telemetry.HighSpeedFrame;
import telemetry.SlowSpeedFrame;
import telemetry.FoxBPSK.FoxBPSKFrame;
import fec.RsCodeWord;

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
public class FoxBPSKBitStream extends HighSpeedBitStream {
	public static int SLOW_SPEED_SYNC_WORD_DISTANCE = 5735; 
	public static int NUMBER_OF_RS_CODEWORDS = 3;
	
	public FoxBPSKBitStream(Decoder dec, int wordLength, int syncWordLnegth) {
		super(dec, wordLength, syncWordLnegth);
		SYNC_WORD_LENGTH = syncWordLnegth;
		SYNC_WORD_DISTANCE = SLOW_SPEED_SYNC_WORD_DISTANCE;
		PURGE_THRESHOLD = SYNC_WORD_DISTANCE * 5;
		maxBytes = FoxBPSKFrame.getMaxBytes();
		frameSize = FoxBPSKFrame.MAX_FRAME_SIZE;
		numberOfRsCodeWords = FoxBPSKBitStream.NUMBER_OF_RS_CODEWORDS;
		rsPadding = new int[FoxBPSKBitStream.NUMBER_OF_RS_CODEWORDS];
		rsPadding[0] = 64;
		rsPadding[1] = 64;
		rsPadding[2] = 65;
		findFramesWithPRN = true;
	}
	
	/**
	 * Attempt to decode the PSK 1200bps Speed Frame
	 * 
	 */
	public Frame decodeFrame(int start, int end) {
		byte[] rawFrame = decodeBytes(start, end);
		if (rawFrame == null) return null;
		// ADD in the next SYNC WORD to help the decoder
		// This is a nice idea and even works sometimes, but we need to make sure it does not cause a crash if it is off the end of the data.
		///////////////////////////////////////syncWords.add(SYNC_WORD_LENGTH+SYNC_WORD_DISTANCE);
				
		FoxBPSKFrame bpskFrame = new FoxBPSKFrame();
		bpskFrame.addRawFrame(rawFrame);
		return bpskFrame;
	}
}
