package decoder.FoxBPSK;

import java.util.Date;

import common.Config;
import common.Log;
import decoder.Decoder;
import decoder.HighSpeedBitStream;
import telemetry.Frame;
import telemetry.FrameProcessException;
import telemetry.TelemFormat;
import telemetry.FoxBPSK.FoxBPSKFrame;

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
	public static final int FOX_BPSK_SPEED_SYNC_WORD_DISTANCE = 5720 + 31; 
	TelemFormat telemFormat;
	public static final boolean GOLF_FORMAT = true;
	public static final boolean FOX_FORMAT = false;
	
	public FoxBPSKBitStream(Decoder dec, TelemFormat telemFormat) {
		super(dec, telemFormat.getSyncWordDistance(), telemFormat.getInt(TelemFormat.WORD_LENGTH), 
				telemFormat.getInt(TelemFormat.SYNC_WORD_LENGTH), telemFormat.getInt(TelemFormat.BPS));
		this.telemFormat = telemFormat;
		SYNC_WORD_BIT_TOLERANCE = 10;
		maxBytes = telemFormat.getFrameLength(); //FoxBPSKFrame.getMaxBytes(); // 572 = 476 + 96
		frameSize = telemFormat.getInt(TelemFormat.DATA_LENGTH); // FoxBPSKFrame.MAX_FRAME_SIZE; // 476
		numberOfRsCodeWords = telemFormat.getInt(TelemFormat.RS_WORDS);
		this.rsPadding = telemFormat.getPaddingArray();
		findFramesWithPRN = true;
	}
	
	/**
	 * Attempt to decode the PSK 1200bps Speed Frame
	 * 
	 */
	public Frame decodeFrame(int start, int end, int missedBits, int repairPosition, Date timeOfStartSync) {
		totalRsErrors = 0;
		totalRsErasures = 0;
		byte[] rawFrame = decodeBytes(start, end, missedBits, repairPosition);
		if (rawFrame == null) return null;
		// ADD in the next SYNC WORD to help the decoder
		// This is a nice idea and even works sometimes, but we need to make sure it does not cause a crash if it is off the end of the data.
		///////////////////////////////////////syncWords.add(SYNC_WORD_LENGTH+SYNC_WORD_DISTANCE);
				
		Frame bpskFrame;
//		if (telemFormat.name.equalsIgnoreCase("GOLF_BPSK"))  // TODO - dont hard code here.  Factory method in satManager??  Or do we now have enough in Telem format for one frame class
//			bpskFrame = new GolfBPSKFrame(telemFormat);
//		else
		if (Config.debugFrames)
			Log.println("Decoding frame with Format: " + telemFormat);
		bpskFrame = new FoxBPSKFrame(telemFormat);
		
		try {
			bpskFrame.addRawFrame(rawFrame);
			bpskFrame.rsErrors = totalRsErrors;
			bpskFrame.rsErasures = totalRsErasures;
			bpskFrame.setStpDate(timeOfStartSync);
		} catch (FrameProcessException e) {
			// The FoxId is corrupt, frame should not be decoded.  RS has actually failed
			return null;
		}
//		String os = System.getProperty("os.name").toLowerCase();
//		boolean b = Frame.highSpeedRsDecode(frameSize, FoxBPSKBitStream.NUMBER_OF_RS_CODEWORDS, rsPadding, rawFrame, "FoxTelem " + Config.VERSION + " (" + os + ")");
//		Log.println("SELF RS CHECK:" + b);
		return bpskFrame;
	}
		
}
