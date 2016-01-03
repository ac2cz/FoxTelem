package decoder;

import java.util.ArrayList;

import common.Config;
import common.Log;
import common.Performance;
import telemetry.Frame;
import telemetry.FramePart;
import telemetry.HighSpeedHeader;
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
public abstract class BitStream extends CircularBuffer {

	/* used only for testing erasures and RS corrections
	public static int TEST_ERASURES = 0; // A non zero number tests the erasure mechanism by corrupting this many 10b words
	public int[] eraseWord = {0,1,2,3, 6, 9, 12, 13, 14, 18, 20, 21, 30, 31, 32, 35, 37, 45, 46, 48, 50, 51, 52, 53, 54, 55, 56, 57, 58 }; // specifically mark these positions as erasures
	public int eraseWordCounter = 0; // counter to see if we are at the right position to corrupt a word
	public int eraseWordPosition = 0; // current position in the eraseWord array
	public boolean testedErasure = false;
	public static int TEST_CORRUPTIONS = 00; //A non zero number tests the RS decode mechanism by corrupting this many 8b words
	*/
	
	protected static final int MAX_ERASURES = 16; // If we have more erasures than this then abandon decoding the RSCodeWord, can not let it get to 32
	protected static final int FRAME_PROCESSED = -999;
	protected int SYNC_WORD_DISTANCE = 0; // 10*(SlowSpeedFrame.getMaxBytes()+1);
	protected static final int SYNC_WORD_BIT_TOLERANCE = 0; // if we are within this many bits, then try to decode the frame - hey, you never know..
	protected int PURGE_THRESHOLD = 0; // Remove bits if we have accumulated this many and not found a frame
	protected ArrayList<Integer> syncWords = new ArrayList<Integer>(); // The positions of all the SYNC words we have found
	protected int word10bitPosition = 0; // The position in the 10 bit word when we are searching for SYNC words bit by bit
	protected boolean[] word10 = new boolean[10]; // The 10 bit word used to find SYNC words, selected from the end of the bitStream
	protected boolean alreadyTriedToFlipBits = false; // only try to flip the bits once, otherwise we willl try to double process every failed RS word
	
	public int lastErasureNumber;
	public int lastErrorsNumber;
	Decoder decoder;
	
	protected ArrayList<SyncPair> framesTried = new ArrayList<SyncPair>(); // keep track of the SYNC word combinations that we have already tried
	
	/**
	 * Initialize the array with enough room to hold 6 frames worth of bits
	 * We should never reach this because we purge bits once we exceed 4 frames in length
	 */
	public BitStream(int size, Decoder dec) {
		super(size);
		decoder = dec;
		
	}
	
	/**
	 * Adds a bit at the end
	 * @param n
	 */
	public void addBit(boolean n) {
		//System.out.println("BIT STREAM SIZE: " + this.size() + " " + this);
		this.add(n);
		Performance.startTimer("add:purge");
		purgeBits();
		Performance.endTimer("add:purge");

	}
	
	/**
	 * Search through windowLength bits and test to see if the last
	 * 10 bits are a frame marker.  If it is, then add the position of the first bit of data that
	 * FOLLOWS the SYNC marker to the syncWords array
	 * @param windowLength
	 * @return true if we found the SYNC word
	 */
	public boolean findSyncMarkers(int windowLength) {
		boolean found = false;
		if (this.size() < 10) return false;
		for (int i=this.size()-windowLength; i < this.size(); i++) {
			word10[word10bitPosition++] = this.get(i);
			if (word10bitPosition > 9) {
				word10bitPosition = 9;
				// Check the last 10 bits in the bit stream for the end of frame market
				int word = binToInt(word10);
				if (word == Code8b10b.FRAME || word == Code8b10b.NOT_FRAME) {
					found = true;
					syncWords.add(i+1);
					if (Config.debugFrames) {
						Log.println("SYNC WORD "+ syncWords.size() + " ADDED AT: "+ (i+1));
						printBitArray(word10);
					}
				} 
				// now shift the bits and continue looking for frame marker
				for (int k=1; k<10; k++)
					word10[k-1] = word10[k];
			} 
		}
		return found;
	}
	
	public Frame findFrames() {
		
		Performance.startTimer("findFrames:checks");

		if (Config.highSpeed)
			checkMissingStartSYNC(0, new HighSpeedHeader());
		else
			checkMissingStartSYNC(0, new SlowSpeedHeader()); 
		checkMissingMiddleSYNC();
		//checkLongFramesForMissingEndSYNC();
		//checkLongFramesForMissingStartSYNC();

		Performance.endTimer("findFrames:checks");

		int start = 0;
		int end = 0;

		Performance.startTimer("findFrames:decode");

		for (int i=0; i<syncWords.size()-1; i++ ) {
			start = syncWords.get(i);
			for (int e=i+1; e<syncWords.size(); e++) {
				end = syncWords.get(e);
				if (start != FRAME_PROCESSED) {
					if (end-start >= SYNC_WORD_DISTANCE - SYNC_WORD_BIT_TOLERANCE && end-start <= SYNC_WORD_DISTANCE+SYNC_WORD_BIT_TOLERANCE) {

						if (newFrame(start, end)) {
							if (Config.debugFrames) Log.println("FRAME from bits " + start + " to " + end + " length " + (end-start) + " bits " + (end-start)/10 + " bytes");
							alreadyTriedToFlipBits = false; // reset the flag, in case we need to flip the bit stream
							Frame frame = decodeFrame(start,end);

							if (frame == null) {
								if (!alreadyTriedToFlipBits) {
									alreadyTriedToFlipBits = true;
									decoder.flipReceivedBits = !decoder.flipReceivedBits;
									//Log.println("..trying Flipped bits");
									Frame flipFrame = decodeFrame(start, end); 
									if (flipFrame != null) {
										// it worked, so flip the whole bitstream and we carry on
										Log.println("DECODER: Flipped bits");
										// flip any bits left
										flipBitStream();
										return flipFrame;
									} else {
										// was not a flip bit issue
										decoder.flipReceivedBits = !decoder.flipReceivedBits;
										framesTried.add(new SyncPair(start,end));
										return null;
									}
								} else {
									framesTried.add(new SyncPair(start,end));
									return null;
								}
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
	 * Return true if we have not tried to decode this frame before
	 * @param start
	 * @param end
	 * @return
	 */
	private boolean newFrame(int start, int end) {
		for (int i=0; i<framesTried.size(); i++) {
			if (framesTried.get(i).equals(start, end))
				return false;
		}
		return true;
	}
	
	
	/**
	 * Attempt to decode a frame by checking the lengths between the sync words that have been found
	 * SYNC words are added to the syncWords array, with the value pointing to the first bit
	 * of data that follows them.
	 * 
	 */
	public abstract Frame decodeFrame(int start, int end);

	/**
	 * Search to see if there is a header before this SYNC word indicating that we missed a SYNC
	 * Otherwise we throw this data away
	 */
	protected void checkMissingStartSYNC(int n, FramePart header) {
		int start = syncWords.get(n);
		
		if (start > SYNC_WORD_DISTANCE) {
			// We might have a frame before this word, but we missed the start SYNC word
			// Try to find a valid header
			boolean loopUpError = false;
			for (int j=start-SYNC_WORD_DISTANCE; j< start-SYNC_WORD_DISTANCE+header.getMaxBytes()*10; j+=10) {
				//int[] b8 = processWord(j);
				byte b8;
				try {
					b8 = processWord(j);
				} catch (LookupException e) {
					b8 = -1;
					loopUpError = true;
				}
				header.addNext8Bits(b8);
			}
			
			if (header.isValid() && !loopUpError) {
				syncWords.add(n, start-SYNC_WORD_DISTANCE);
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
			if (Config.debugBits) Log.print(j + ": 10b:" + Decoder.hex(word));	
			if (Config.debugBits) Log.print(" 8b:" + Decoder.hex(word8b) + "\n");
			if (Config.debugBits) printBitArray(intToBin8(word8b));
			return word8b;
		} catch (LookupException e) {
			if (Config.debugBits) Log.print(j + ": 10b:" + Decoder.hex(word));	
			if (Config.debugBits) Log.print(" 8b: -1" + "\n\n");
			
			throw e;
		}
	}
	
	/**
	 * If we have too many bits, then remove one frame's worth from the beginning.  This prevents too much data from being accumulated
	 */
	protected void purgeBits() {
		
		if (this.size() > PURGE_THRESHOLD) {
			//printSyncWordPositions();
			
			removeBits(0, SYNC_WORD_DISTANCE);
			framesTried = new ArrayList<SyncPair>();
//			this.incStartPointer(SYNC_WORD_DISTANCE-1);
			//printSyncWordPositions();
		}
	}
	

	/**
	 * Remove bits from the bit list and update the position of any
	 * frameMarker Candidates
	 * @return
	 */
	public void removeBits(int start, int end) {
		if (Config.debugFrames) Log.println("Purging " + (end - start) + " bits");
		int distance = end - start;
//		for (int c=start; c < end; c++) {
//			this.remove(0); // Remove the bits in this range
//		}
		this.incStartPointer(distance);
		// Update the frame markers
		for (int j=0; j< syncWords.size(); j++) {
			int marker = syncWords.get(j) - distance;
			if (marker < 0) { 
				syncWords.remove(j);
				j=j-1;
			} else
				syncWords.set(j,  marker);
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
	
	class SyncPair {
		int word1;
		int word2;
		
		SyncPair(int a, int b) {
			word1 = a;
			word2 = b;
		}
		
		public boolean equals(int x, int y) {
			if (word1 == x && word2 == y) return true;
			if (word2 == x && word1 == y) return true;
			return false;
		}
	}
}
