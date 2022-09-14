package decoder.FoxBPSK;

import java.util.Date;

import common.Config;
import common.Log;
import decoder.Decoder;
import decoder.FoxFskDecoder;
import decoder.HighSpeedBitStream;
import telemetry.Format.FormatFrame;
import telemetry.Format.TelemFormat;
import telemetry.frames.Frame;
import telemetry.frames.FrameProcessException;
import telemetry.frames.HighSpeedFrame;
import telemetry.frames.SlowSpeedFrame;

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
@SuppressWarnings({ "serial", "deprecation" })
public class FormatBitStream extends HighSpeedBitStream {
	//public static final int FOX_BPSK_SPEED_SYNC_WORD_DISTANCE = 5720 + 31; 
	TelemFormat telemFormat;
	//public static final boolean GOLF_FORMAT = true;
	//public static final boolean FOX_FORMAT = false;
	
	public FormatBitStream(Decoder dec, TelemFormat telemFormat, boolean use31bitSyncWord) {
		super(dec, telemFormat.getSyncWordDistance(), telemFormat.getInt(TelemFormat.WORD_LENGTH), 
				telemFormat.getInt(TelemFormat.SYNC_WORD_LENGTH), telemFormat.getInt(TelemFormat.BPS));
		this.telemFormat = telemFormat;
		SYNC_WORD_BIT_TOLERANCE = 10;
		maxBytes = telemFormat.getFrameLength(); //FoxBPSKFrame.getMaxBytes(); // 572 = 476 + 96
		dataLength = telemFormat.getInt(TelemFormat.DATA_LENGTH); // FoxBPSKFrame.MAX_FRAME_SIZE; // 476
		numberOfRsCodeWords = telemFormat.getInt(TelemFormat.RS_WORDS);
		this.rsPadding = telemFormat.getPaddingArray();
		findFramesWithPRN = use31bitSyncWord;
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
				
		Frame formatFrame;
		if (Config.debugFrames)
			Log.println("Decoding frame with Format: " + telemFormat);
		// For legacy support this needs to be hard coded
		if (telemFormat.name.equalsIgnoreCase(FoxFskDecoder.DUV_FSK))
			formatFrame = new SlowSpeedFrame();
		else if (telemFormat.name.equalsIgnoreCase(FoxFskDecoder.HIGHSPEED_FSK))
			formatFrame = new HighSpeedFrame();
		else // otherwise we just use the format and the layouts/payloads defined for it
			formatFrame = new FormatFrame(telemFormat);
		
		try {
			formatFrame.addRawFrame(rawFrame);
			formatFrame.rsErrors = totalRsErrors;
			formatFrame.rsErasures = totalRsErasures;
			formatFrame.setStpDate(timeOfStartSync);
		} catch (FrameProcessException e) {
			// The FoxId is corrupt, frame should not be decoded.  RS has actually failed
			return null;
		}
//		String os = System.getProperty("os.name").toLowerCase();
//		boolean b = Frame.highSpeedRsDecode(frameSize, FoxBPSKBitStream.NUMBER_OF_RS_CODEWORDS, rsPadding, rawFrame, "FoxTelem " + Config.VERSION + " (" + os + ")");
//		Log.println("SELF RS CHECK:" + b);
		return formatFrame;
	}
		
}
