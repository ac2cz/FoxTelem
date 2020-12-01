package decoder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import common.Config;
import common.Log;
import telemetry.Frame;
import telemetry.FoxFramePart;
import telemetry.SlowSpeedHeader;

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
 * This is a stream of bits received from the decoder.  We search the stream for SYNC words and determine if we have
 * found a valid frame.
 * 
 * 

 *
 */
@SuppressWarnings("serial")
public abstract class FoxBitStream extends BitStream {

	/* used only for testing erasures and RS corrections
	public static int TEST_ERASURES = 0; // A non zero number tests the erasure mechanism by corrupting this many 10b words
	public int[] eraseWord = {0,1,2,3, 6, 9, 12, 13, 14, 18, 20, 21, 30, 31, 32, 35, 37, 45, 46, 48, 50, 51, 52, 53, 54, 55, 56, 57, 58 }; // specifically mark these positions as erasures
	public int eraseWordCounter = 0; // counter to see if we are at the right position to corrupt a word
	public int eraseWordPosition = 0; // current position in the eraseWord array
	public boolean testedErasure = false;
	public static int TEST_CORRUPTIONS = 00; //A non zero number tests the RS decode mechanism by corrupting this many 8b words
	*/
	protected int SYNC_WORD_LENGTH = 10; // These can be overridden in a child class if the length is different.  Update in the constructor
	protected int DATA_WORD_LENGTH = 10; 
	
	protected static final int MAX_ERASURES = 15; // If we have more erasures than this then abandon decoding the RSCodeWord, can not let it get to 32
	protected static final int FRAME_PROCESSED = -999;
	
	protected static int SYNC_WORD_BIT_TOLERANCE = 0; // if we are within this many bits, then try to decode the frame.  Set by Constructor
	
	protected int syncWordbitPosition = 0; // The position in the 10 bit word when we are searching for SYNC words bit by bit
	protected boolean[] syncWord = new boolean[SYNC_WORD_LENGTH]; // The SYNC_WORD_LENGTH bit word used to find SYNC words, selected from the end of the bitStream
	protected boolean alreadyTriedToFlipBits = false; // only try to flip the bits once, otherwise we willl try to double process every failed RS word
	
	private long millisecondsBetweenSyncWords;
	private double millisecondsPerBit;
	
	protected boolean findFramesWithPRN = false;
	
	/**
	 * Initialize the array with enough room to hold 6 frames worth of bits
	 * We should never reach this because we purge bits once we exceed 4 frames in length
	 */
	public FoxBitStream(int size, Decoder dec, int syncWordDistance, int wordLength, int syncWordLength,  double millisecondsPerBit) {
		super(size, dec, syncWordDistance);
		SYNC_WORD_LENGTH = syncWordLength;
		DATA_WORD_LENGTH = wordLength;
		syncWord = new boolean[syncWordLength];
		this.millisecondsPerBit = millisecondsPerBit;
		millisecondsBetweenSyncWords = (long) (millisecondsPerBit * SYNC_WORD_DISTANCE);
	}
	
	/**
	 * Add the bits from the current processed window.  For each bit added check to see if the previous N bits are
	 * a sync word.  If they are try to process it as a frame.  If that is successful and enough bits remain, try to 
	 * process a previous frame and continue until no more bits
	 * If there was no frame, continue adding bits and checking for the sync word
	 * @param windowLength
	 * @return
	 */
	public ArrayList<Frame> findFrames(int windowLength) {
		if (this.size() < SYNC_WORD_LENGTH) return null; // not enough bits yet
		
		ArrayList<Frame> frames = null;
		syncWordbitPosition = 0;
		syncWord = new boolean[SYNC_WORD_LENGTH]; // re-init each search
		
		// We look for a new sync word in the current window of bits.  We add each bit one by one
		// and see if the previous N bits are the sync word.  We might have just the last bit in this window
		for (int i=this.size()-windowLength-SYNC_WORD_LENGTH; i < this.size(); i++) {
			if (i < 0) return null; // not enough bits.  We may have purged mid attempt?
			syncWord[syncWordbitPosition++] = this.get(i);
			if (syncWordbitPosition > SYNC_WORD_LENGTH-1) {
				syncWordbitPosition = SYNC_WORD_LENGTH-1;
				// Check the last SYNC_WORD_LENGTH bits in the bit stream for the end of frame market
				int word = binToInt(syncWord);
				if ((findFramesWithPRN && CodePRN.probabllyFrameMarker(syncWord ) ) ||
				//if ((findFramesWithPRN && (word == CodePRN.FRAME )) ||
				//if ((findFramesWithPRN && CodePRN.equals(syncWord ) ) ||
						!findFramesWithPRN && (word == Code8b10b.FRAME || word == Code8b10b.NOT_FRAME)) {
					// We found a sync word
					Date timeOfSync = Calendar.getInstance().getTime();
					if (Config.debugFrames) {
						Log.println("SYNC WORD "+ syncWords.size() + " FROM " + (i-SYNC_WORD_LENGTH) + " total:" 
					+ (totalBits + i -SYNC_WORD_LENGTH) + " DATA STARTS: "+ (i) + " total:" + (totalBits + i) 
					+ " at: " + timeOfSync);
						int last = 0;
						for (int s : syncWords) {
							Log.print(s + "-" + (s-last)+", ");
							last = s;
						}
						Log.println("");
						printBitArray(syncWord);
					}
					alreadyTriedToFlipBits = false; // reset the flag, in case we need to flip the bit stream
					frames = tryToProcessFrames(timeOfSync, i+1);
					if (!(Config.mode == SourceIQ.MODE_PSK_NC || Config.mode == SourceIQ.MODE_PSK_COSTAS))
						if (frames == null || frames.isEmpty()) {
							if (!alreadyTriedToFlipBits) {
								alreadyTriedToFlipBits = true;
								decoder.flipReceivedBits = !decoder.flipReceivedBits;
								//Log.println("..trying Flipped bits");
								frames = tryToProcessFrames(timeOfSync, i+1);
								if (frames != null && !frames.isEmpty()) {
									// it worked, so flip the whole bitstream and we carry on
									Log.println("DECODER: Flipped bits");
									// flip any bits left
									flipBitStream();
								} else {
									// was not a flip bit issue
									decoder.flipReceivedBits = !decoder.flipReceivedBits;
								}
							} 
					}
					// we do not exit as we have to add the remaining bits in the window, which are the start of the next frame
				} 
				// now shift the bits and continue looking for frame marker
				for (int k=1; k<SYNC_WORD_LENGTH; k++)
					syncWord[k-1] = syncWord[k];
			} 
		}
//		if (frames !=null && frames.size() > 1) { // then reverse the order or the timestamps are back to front
//			ArrayList<Frame> returnFrames = new ArrayList<Frame>(frames.size());
//			for (int i = 0; i< frames.size(); i++)
//				returnFrames.add(frames.get(frames.size()-1-i));
//			return returnFrames;
//		} else
			return frames;

	}

	/**
	 * We have a sync word at this bit position, try to process a frame or frames
	 * @param bitPosition
	 */
	private ArrayList<Frame> tryToProcessFrames(Date timeOfFinalSync, int end) {
		// Find the time at the start of the first bit of the SyncWord that begins this frame.  
		// This was the time on the spacecraft when transmission of the frame began
		Date timeOfStartSync = new Date(timeOfFinalSync.getTime() - millisecondsBetweenSyncWords - (long)(SYNC_WORD_LENGTH * millisecondsPerBit));
		int END = end;
		ArrayList<Frame> frames = new ArrayList<Frame>();
		int start = end - SYNC_WORD_DISTANCE;
		int missedBits = 0;
		int repairPosition = 0;
		while (start > 0) {
			Frame frame = decodeFrame(start,end, missedBits, repairPosition, timeOfFinalSync);
			if (frame != null) {
				// We found a frame
				frames.add(frame);
				end = end - SYNC_WORD_DISTANCE;
				start = start - SYNC_WORD_DISTANCE;
				timeOfStartSync = new Date(timeOfStartSync.getTime() - millisecondsBetweenSyncWords);
			} else {
				start = -1;
			}
		}
		if (frames.isEmpty()) return null;
		// Otherwise we found some frames
		// purge the bits, we found a frame or frames
		removeBits(0, END-SYNC_WORD_LENGTH);
		return frames;
	}
	
	/**
	 * Return true if we have not tried to decode this frame before
	 * @param start
	 * @param end
	 * @return
	 */
//	private boolean newFrame(int start, int end) {
//		for (int i=0; i<framesTried.size(); i++) {
//			if (framesTried.get(i).equals(start, end))
//				return false;
//		}
//		return true;
//	}
	
	
	/**
	 * Attempt to decode a frame by checking the lengths between the sync words that have been found
	 * SYNC words are added to the syncWords array, with the value pointing to the first bit
	 * of data that follows them.
	 * 
	 */
	public abstract Frame decodeFrame(int start, int end, int missedBits, int repairPosition, Date timeOfFinalSync);


	protected int checkShortFrame(int start, int end) {

		@SuppressWarnings("unused")
		int SYNC_WORD_BIT_TOLERANCE = 6; // look for frames that might be short by up to this amount

		int firstMinErasures = 0;
		int shortBits = 0;
		shortBits = SYNC_WORD_DISTANCE - (end-start);
		int minErasures = 999;

		if (Config.debugFrames) Log.println("**** SHORT FRAME from bits " + start + " to " + end + " length " + (end-start) + " bits, " + shortBits + " short");
		// We have a short frame, which means some bits were dropped.  The question is, where do we insert them	
		// We insert the missing bits in each 10b code word and check which gives the least erasures.  This is a brute force approach
		// The insertion is achieved by using that last few bits of the previous 10b word.  We move the pointer backwards
		// This leaves the data unchanged while we analyze it
		int totalBytes = SYNC_WORD_DISTANCE/10;
		int[] erasureCount = new int[totalBytes]; // count how many erasures if we insert the bits at this point
		for (int a=0; a < totalBytes; a++) {
			int currentErasureCount=0;
			int currentWord=0;
			for (int j=start; j< end-SYNC_WORD_LENGTH; j+=10) {
				if (a == currentWord++) // This is where we insert
					j=j-shortBits;
				@SuppressWarnings("unused")
				byte b8 = -1;
				try {
					b8 = processWord(j);
				} catch (LookupException er) {
					// erasure
					currentErasureCount++;
				}
			}
			erasureCount[a] = currentErasureCount;
			//Log.println("Byte: "+ a + " erasurse: " + currentErasureCount);
			if (currentErasureCount < minErasures ) {
				minErasures = currentErasureCount;
				firstMinErasures = a;
			}
		}

		if (shortBits > 0) {
			int position = start + firstMinErasures * 10;
			return position;
		}
		return 0;
	}

	/**
	 * Search to see if there is a header before this SYNC word indicating that we missed a SYNC
	 * Otherwise we throw this data away
	 */
	@Deprecated
	protected void checkMissingStartSYNC(int n, FoxFramePart header) {
		int start = syncWords.get(n);
		
		if (start > SYNC_WORD_DISTANCE) {
			// We might have a frame before this word, but we missed the start SYNC word
			// Try to find a valid header
			int loopUpError = 0;
			for (int j=start-SYNC_WORD_DISTANCE; j< start-SYNC_WORD_DISTANCE+header.getMaxBytes()*10; j+=10) {
				//int[] b8 = processWord(j);
				byte b8;
				try {
					b8 = processWord(j);
				} catch (LookupException e) {
					b8 = -1;
					loopUpError++;
				}
				header.addNext8Bits(b8);
			}
			
			if (loopUpError < header.getMaxBytes()) { // If we find any valid 10b words then perhaps this was a header, try to process
				syncWords.add(n, start-SYNC_WORD_DISTANCE);
			//if (true || header.isValid() && !loopUpError) {
//				if (!haveSyncWordAtBit(start-SYNC_WORD_DISTANCE)) syncWords.add(n, start-SYNC_WORD_DISTANCE);
				if (Config.debugFrames) Log.println("SYNC WORD MISSING, but found Header");
				//System.out.println(slowSpeedheader);
			} else {
				// This must be noise in front of this SYNC word, remove it
				if (Config.debugFrames) Log.println("Looked for Missing START sync but did not find it");
	//			removeBits(0, start-10);  // Hmm, we might have a missing SYNC in this data, with a later END SYNC.  Unlikely, but why purge this here?
				
			}
		}
	}
	
	/**
	 * Check long frames to see if we are missing the SYNC word at the start
	 * 
	 * THIS FINDs LONG FRAME AND DECODES, but does not actually find any different frames.
	 * Soo something wrong with it.
	 * 
	 * We should also look for long frames that are missing and end sync, which could be the case at the end of a transmission
	 * 
	 * CURRENTLY UNUSED
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void checkLongFramesForMissingStartSYNC() {
		int start = 0;
		int end = 0;
		int syncs = syncWords.size();
		for (int i=0; i<syncs-1; i++ ) {
			start = syncWords.get(i);
			for (int e=i+1; e<syncs; e++) {
				end = syncWords.get(e);
				if (start != FRAME_PROCESSED) {
					if (end-start > SYNC_WORD_DISTANCE) {
						if (Config.debugFrames) Log.println("CHECKING LONG FRAME from bits " + start + " to " + end);
						checkMissingStartSYNC(e, new SlowSpeedHeader());
					}
				}
			}
		}
	}
	
	/**
	 * Look for a double length frame and conclude that the middle SYNC is missing,
	 * so insert a SYNC word at that point
	 */
	@Deprecated
	protected void checkMissingMiddleSYNC() {
		int start = 0;
		int end = 0;
		int syncs = syncWords.size();
		for (int i=0; i<syncs-1; i++ ) {
			start = syncWords.get(i);
			for (int e=i+1; e<syncs; e++) {
				end = syncWords.get(e);
				if (start != FRAME_PROCESSED) {
					if (end-start == SYNC_WORD_DISTANCE*2 ) 
						if (!haveSyncWordAtBit(end-SYNC_WORD_DISTANCE)) { // make sure we have not already added this
							if (Config.debugFrames) 
								Log.println("DOUBLE LENGTH FRAME from bits " + start + " to " + end);
							syncWords.add(e, end-SYNC_WORD_DISTANCE);
							
						}
				}
			}
		}

	}

	/**
	 * Decode the 10b word that starts at position j and extends to j+9
	 * @param j
	 * @return - an array containing the 8 bits
	 * @throws LookupException 
	 */
	protected byte processWord(int j) throws LookupException {
		if (Config.debugBits) printBitArray(get10Bits(j));

		int word = binToInt(get10Bits(j));
		byte word8b;
		try {
			word8b = Code8b10b.decode(word, decoder.flipReceivedBits);


			/*
			 * ONLY FOR TESTING

		if (!testedErasure && TEST_ERASURES > 0) {
			if (eraseWordCounter++ == eraseWord[eraseWordPosition]) {
				eraseWordPosition++;
				word8b = -1;
				testedErasure = true;
				TEST_ERASURES--;
			}
		} else {
			testedErasure = false;
			if (this.TEST_CORRUPTIONS > 0) {
				word8b = 127;
				TEST_CORRUPTIONS--;
			}
		}
			 */
			// Print position and add 1 to total so it matches the windowPosition in the audioGraph
			if (Config.debugBits) Log.print((totalBits + j - 1) + ": " + j + ": 10b:" + FoxDecoder.hex(word));	
			if (Config.debugBits) Log.print(" 8b:" + FoxDecoder.hex(word8b) + "\n");
			if (Config.debugBits) printBitArray(intToBin8(word8b));
			return word8b;
		} catch (LookupException e) {
			if (Config.debugBits) Log.print((totalBits + j - 1) +": " + j + ": 10b:" + FoxDecoder.hex(word));	
			if (Config.debugBits) Log.print(" 8b: -1" + "\n\n");
			
			throw e;
		}
	}
	
	public boolean haveSyncWordAtBit(int b) {
		for (int i=0; i< syncWords.size(); i++)
			if (syncWords.get(i) == b) return true;
		return false;
	}
	
	/**
	 * true if the bit stream has at least 10 bits in it
	 * @return
	 */
	public boolean has10() {
		if (this.size() > 9)
			return true;
		return false;
	}

	/**
	 * Return 10 bits from n to n + 9
	 * @param n
	 * @return
	 */
	public boolean[] get10Bits(int n) {
		if (this.size() > n + 9) {
			boolean[] b = new boolean[10];
			for (int j=0; j < 10; j++)
				b[j] = this.get(n+j);
			return b;
		}
		return null;
	}
	
	/**
	 * Returns the last 10 bits of the bitStream
	 * @return
	 */
	public boolean[] last10() {
		if (!has10()) return null;
		boolean[] b = new boolean[10];
		for (int j=0; j < 10; j++)
			b[j] = this.get(this.size()-10+j);
		return b;
	}
	
	/**
	 * Given a set of bits, convert it into an integer
	 * The most significant bit is in the lowest index of the array. e.g. 1 0 0 will have the value 4, with the 1 in array position 0
	 * We start from the highest array index, 
	 * @param word10
	 * @return
	 */
	public static int binToInt(boolean[] word10) {
		int d = 0;
	      
		for (int i=0; i<word10.length; i++) {
			int value = 0;
			if (word10[word10.length-1-i]) value = 1;
			d = d + (value << i);
		}
		return d;
	}
	
	/**
	 * Given an integer that represents an 8 bit word, convert it back to 8 bits
	 * This stores the bits in an array with the msb in position 0, so it prints
	 * in the right order
	 * @param word
	 * @return
	 */
	public static boolean[] intToBin8(int word) {
		boolean b[] = new boolean[8];
		for (int i=0; i<8; i++) {
			if (((word >>i) & 0x01) == 1) b[7-i] = true; else b[7-i] = false; 
		}
		return b;
	}

	public static boolean[] intToBin9(int word) {
		boolean b[] = new boolean[9];
		for (int i=0; i<9; i++) {
			if (((word >>i) & 0x01) == 1) b[8-i] = true; else b[8-i] = false; 
		}
		return b;
	}
	
	public static boolean[] intToBin10(int word) {
		boolean b[] = new boolean[10];
		for (int i=0; i<10; i++) {
			if (((word >>i) & 0x01) == 1) b[9-i] = true; else b[9-i] = false; 
		}
		return b;
	}
	
	/**
	 * Debug routine to print out the positions of the SYNC words
	 */
	public void printSyncWordPositions() {
		for (int i=0; i<syncWords.size(); i++)
			Log.println("SYNC WORD AT: "+ syncWords.get(i));
	}

	/**
	 * Debug routine to print a bit array
	 * @param bs
	 */
	public static void printBitArray(boolean[] bs) {
		for (int j=0; j < bs.length; j++)
			if (bs[j]) Log.print(1+" "); else Log.print(0+" ");
		Log.print("\n");
	}
	
	public static String stringBitArray(boolean[] bs) {
		String s = new String();
		for (int j=0; j < bs.length; j++)
			if (bs[j]) s = s + "1"; else s = s + "0";
		return s;
	}
	
	public String toString() {
		if (!has10()) return "";
		String s = new String();
		for (int i=this.size()-10; i< this.size(); i++)
			s = s + this.get(i) + " ";
		return s;
	}
	
	protected void flipBitStream() {
		for (int c=0; c < this.size(); c++) {
			this.set(c,  !this.get(c)); 
		}
	}
	
	
}
