package decoder.KiwiSat;

import common.Config;
import common.Log;
import decoder.Code8b10b;
import decoder.Decoder;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
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
 * The bit stream is a circular buffer of bits.  We search through the bits to find frames.  The bitstream understand how frames are delimited.  It can
 * then pass a set of bits to the Frame class, which then decodes it. Any Forward Error Checking is also handled by the bitstrea.
 * 
 * For KISS RAW frames:
 * From: http://www.ax25.net/kiss.aspx
 * The "asynchronous packet protocol" spoken between the host and TNC is very simple, since its only function is to delimit frames. Each frame is both 
 * preceded and followed by a special FEND (Frame End) character, analogous to an HDLC flag. No CRC or checksum is provided. In addition, no RS-232C 
 * handshaking signals are employed.
 * 
The special characters are:

Abbreviation            Description                    Hex value
   FEND                 Frame  End                         C0  
   FESC                 Frame  Escape                      DB 
   TFEND                Transposed Frame End               DC 
   TFESC                Transposed Frame Escape            DD

The reason for both preceding and ending frames with FENDs is to improve performance when there is noise on the asynch line. The FEND at the beginning of a 
frame serves to "flush out" any accumulated garbage into a separate frame (which will be discarded by the upper layer protocol) instead of sticking it on 
the front of an otherwise good frame. As with back-to-back flags in HDLC, two FEND characters in a row should not be interpreted as delimiting an 
empty frame.

Frames are sent in 8-bit binary; the asynchronous link is set to 8 data bits, 1 stop bit, and no parity. If a FEND ever appears in the data, it is 
translated into the two byte sequence FESC TFEND (Frame Escape, Transposed Frame End). Likewise, if the FESC character ever appears in the user data, 
it is replaced with the two character sequence FESC TFESC (Frame Escape, Transposed Frame Escape).

As characters arrive at the receiver, they are appended to a buffer containing the current frame. Receiving a FEND marks the end of the current frame. 
Receipt of a FESC puts the receiver into "escaped mode", causing the receiver to translate a following TFESC or TFEND back to FESC or FEND, respectively, 
before adding it to the receive buffer and leaving escaped mode. Receipt of any character other than TFESC or TFEND while in escaped mode is an error; 
no action is taken and frame assembly continues. A TFEND or TESC received while not in escaped mode is treated as an ordinary data character.

This procedure may seem somewhat complicated, but it is easy to implement and recovers quickly from errors. In particular, the FEND character is 
never sent over the channel except as an actual end-of-frame indication. This ensures that any intact frame (properly delimited by FEND characters) 
will always be received properly regardless of the starting state of the receiver or corruption of the preceding frame.

This asynchronous framing protocol is identical to "SLIP" (Serial Line IP), a popular method for sending ARPA IP datagrams across asynchronous links. 
It could also form the basis of an asynchronous amateur packet radio link protocol that avoids the complexity of HDLC on slow speed channels.

 */
@SuppressWarnings("serial")
public class KiwiSatBitStream extends HighSpeedBitStream {
	public static int SLOW_SPEED_SYNC_WORD_DISTANCE = 5735;   // NEED TO SET THIS TO THE TOTAL LENGTH OF KIWI-SAT FRAME
	public static int NUMBER_OF_RS_CODEWORDS = 3; // WONT NEED THIS
	public static int FEND = 0xC0; // Frame  End                         C0  
	public static int FESC = 0xDB; // Frame  Escape                      DB 
	public static int TFEND = 0xDC; // Transposed Frame End               DC 
	public static int TFESC = 0xDD; // Transposed Frame Escape            DD
	
	public KiwiSatBitStream(Decoder dec, int wordLength, int syncWordLnegth) {
		super(dec, wordLength, syncWordLnegth);
		SYNC_WORD_LENGTH = syncWordLnegth;
		SYNC_WORD_DISTANCE = SLOW_SPEED_SYNC_WORD_DISTANCE;
		PURGE_THRESHOLD = SYNC_WORD_DISTANCE * 5;
		maxBytes = FoxBPSKFrame.getMaxBytes();
		frameSize = FoxBPSKFrame.MAX_FRAME_SIZE;
		numberOfRsCodeWords = 0; // WE WONT USE RS DECODER
		rsPadding = new int[0]; // NO PADDING OF RS WORDS
		findFramesWithPRN = true; // ASSUME WE CAN SPOT FRAMES WITH A STRING OF BITS
	}
	
	/**
	 * Attempt to decode the PSK 1200bps Speed Frame
	 * 
	 */
	public Frame decodeFrame(int start, int end) {
		byte[] rawFrame = decodeBytes(start, end);
		if (rawFrame == null) return null;
		KiwiSatFrame kiwiFrame = new KiwiSatFrame();
		kiwiFrame.addRawFrame(rawFrame);
		return kiwiFrame;
	}
	
	/**
	 * Decode the 8N1 10 bit word that starts at position j and extends to j+9
	 * @param j
	 * @return - an array containing the 8 bits
	 * @throws LookupException 
	 */
	protected byte processWord(int j) throws LookupException {
		if (Config.debugBits) printBitArray(get10Bits(j));

		int word = binToInt(get10Bits(j));
		byte word8b;
		try {
			word8b = Code8N1.decode(word, decoder.flipReceivedBits);

			if (Config.debugBits) Log.print(j + ": 10b:" + FoxDecoder.hex(word));	
			if (Config.debugBits) Log.print(" 8b:" + FoxDecoder.hex(word8b) + "\n");
			if (Config.debugBits) printBitArray(intToBin8(word8b));
			return word8b;
		} catch (LookupException e) {
			if (Config.debugBits) Log.print(j + ": 10b:" + FoxDecoder.hex(word));	
			if (Config.debugBits) Log.print(" 8b: -1" + "\n\n");
			
			throw e;
		}
	}
}
