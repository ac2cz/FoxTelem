package decoder.KiwiSat;

import common.Config;
import common.Log;
import common.Performance;
import decoder.Code8b10b;
import decoder.Decoder;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
import decoder.HighSpeedBitStream;
import decoder.LookupException;
import decoder.SourceIQ;
import decoder.FoxBitStream.SyncPair;
import telemetry.Frame;
import telemetry.HighSpeedFrame;
import telemetry.HighSpeedHeader;
import telemetry.SlowSpeedFrame;
import telemetry.SlowSpeedHeader;
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
public class KiwiSatBitStream extends FoxBitStream {
	public static int HIGH_SPEED_SYNC_WORD_DISTANCE = 1760; //8*(KiwiSatFrame.MAX_FRAME_SIZE+2);   // NEED TO SET THIS TO THE TOTAL LENGTH OF KIWI-SAT HDLC FRAME
	public static int NUMBER_OF_RS_CODEWORDS = 3; // WONT NEED THIS

	// KISS Frame control characters
	public static int FEND = 0xC0; // Frame  End                         C0  
	public static int FESC = 0xDB; // Frame  Escape                      DB 
	public static int TFEND = 0xDC; // Transposed Frame End               DC 
	public static int TFESC = 0xDD; // Transposed Frame Escape            DD

	public KiwiSatBitStream(Decoder dec, int wordLength, int syncWordLength) {
		super(HIGH_SPEED_SYNC_WORD_DISTANCE*8, wordLength,syncWordLength, dec);
		SYNC_WORD_LENGTH = syncWordLength;
		SYNC_WORD_DISTANCE = HIGH_SPEED_SYNC_WORD_DISTANCE;
		PURGE_THRESHOLD = SYNC_WORD_DISTANCE * 5;
		//maxBytes = FoxBPSKFrame.getMaxBytes();
		//frameSize = FoxBPSKFrame.MAX_FRAME_SIZE;
		//numberOfRsCodeWords = 0; // WE WONT USE RS DECODER
		//rsPadding = new int[0]; // NO PADDING OF RS WORDS
		findFramesWithPRN = false; // ASSUME WE CAN SPOT FRAMES WITH A STRING OF BITS
	}

	int bitStuffCount = 0;
	/**
	 * Adds a bit at the end remove bit stuffing.
	 * If the Tx sees 5 1s in a row then it adds a 0.  If we see 5 1s in a row and the next value is a 0, then 
	 * it should be removed.  If it is a 1, then it is a SYNC word and it should be left.
	 * @param n
	 */
	public void addBit(boolean n) {
		if (bitStuffCount == 5 && n == false) {
			bitStuffCount = 0;
			return; // we don't add the bit
		}
		if (bitStuffCount == 7) {
			//System.err.println("FRAME ERROR: 7 1 bits");
			bitStuffCount = 0; // we should abort the frame, but this might have been noise and perhaps we can recover?
			purgeBits();
		}
		if (n == true) bitStuffCount++;
		super.addBit(n);
	}

	public Frame findFrames() {

		Performance.startTimer("findFrames:decode");

		// For HDLC frames we do not rely on the distance between frames, instead we check all sync words that are at least a certain threshold apart.
		int start;
		int end;
		// We have no expensive RS Decode so we can check as many as we like
		for (int i=0; i<syncWords.size()-1; i++ ) {
			start = syncWords.get(i);
			for (int e=i+1; e<syncWords.size(); e++) {
				end = syncWords.get(e);
				if (start != FRAME_PROCESSED) {
					if (end-start >= 8*200 && end-start <= 8*(KiwiSatFrame.MAX_FRAME_SIZE+10)) { // at least this long surely!

						if (newFrame(start, end)) {
							if (Config.debugFrames) Log.println("FRAME from bits " + start + " to " + end + " length " + (end-start) + " bits " + (end-start)/DATA_WORD_LENGTH + " bytes");
							Frame frame = decodeFrame(start,end);

							if (frame == null) {
								framesTried.add(new SyncPair(start,end));
								return null;

							}
							return frame;
						}
					}
				}
			}
		}
		Performance.endTimer("findFrames:decode");

		return null;
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
	 * Decode the HDLC 8 bit word that starts at position j and extends to j+9
	 * @param j
	 * @return - an byte containing the 8 bits
	 * @throws LookupException 
	 */
	protected byte processWord(int j) throws LookupException {
		int word = binToInt(getNBitsFromPos(DATA_WORD_LENGTH, j));
		char ch = (char)word;
		if (Config.debugBits) Log.print(word + ":" + ch + " ");
		if (Config.debugBits) printBitArray(getNBitsFromPos(DATA_WORD_LENGTH, j));

		byte word8b = (byte) (word & 0xFF);
		return word8b;
	}


	protected byte[] decodeBytes(int start, int end) {
		Log.println("TRIED KIWI FRAME from: "+start + " to " + end);

		int bytesInFrame = 0; 

		byte[] rawFrame = new byte[KiwiSatFrame.MAX_FRAME_SIZE];

		if (rawFrame.length != SYNC_WORD_DISTANCE/10-1)
			Log.println("WARNING: Frame length " + rawFrame.length + " bytes is different to default SYNC word distance "+ (SYNC_WORD_DISTANCE/10-1));

		// We have found a frame, so process it. start is the first bit of data
		// end is the first bit after the second SYNC word.  We do not 
		// want to pass the SYNC word to the FRAME, so we process all the 
		// bits up to but not including end-SYNC_WORD_LENGTH.
		int f=0; // position in the Rs code words as we allocate bits to them
		//int rsNum = 0; // counter that remembers the RS Word we are adding bytes to

		// Traverse the bits between the frame markers and allocate the decoded bytes round robin back to the RS Code words
		for (int j=start; j< end-SYNC_WORD_LENGTH; j+=10) {

			byte b8 = -1;
			try {
				b8 = processWord(j);
			} catch (LookupException e) {

			}
			rawFrame[bytesInFrame++] = b8;

			if (bytesInFrame == KiwiSatFrame.MAX_FRAME_SIZE-2) {  
				// first parity byte
				//Log.println("parity");
				// Reset to the first code word

				//Next byte position in the codewords
				f++;
			}


		}

		if (Config.debugFrames || Config.debugRS)
			Log.println("CAPTURED " + bytesInFrame + " high speed bytes");

		lastErasureNumber = 0;
		lastErrorsNumber = 0;


		// Consume all of the bits up to this point, but not the end SYNC word
		removeBits(0, end-SYNC_WORD_LENGTH);

		f=0;

		boolean readingParity = false;
		// We have corrected the bytes, now allocate back to the rawFrame and add to the frame

		return rawFrame;

	}

	/**
	 * Given a set of bits, convert it into an integer
	 * The LEAST significant bit is in the lowest index of the array. e.g. 1 0 0 will have the value 1, with the 1 in array position 0
	 * We therefore start from the lowest array index, 
	 * @param word10
	 * @return
	 */
	public static int binToInt(boolean[] word10) {
		int d = 0;

		for (int i=0; i<word10.length; i++) {
			int value = 0;
			if (word10[i]) value = 1;
			d = d + (value << i);
		}
		return d;
	}
}
