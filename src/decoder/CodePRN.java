package decoder;

import common.Config;

public class CodePRN {
	public static final int FRAME = 0x5647 & 0x7fff;  //101011001000111
	public static final int LONG_FRAME_SYNC = 0x47cd215d;//
	public static boolean[] SHORT_FRAME_PRN = {true,false,true,false,true,true,false,false,true,false,false,false,true,true,true};
	static boolean[] NOT_FRAME_PRN = {false,true,false,true,false,false,true,true,false,true,true,true,false,false,false};
	public static boolean[] LONG_FRAME_PRN = {true,false,false,false,true,true,true,true,true,false,false,true,true,false,true,false,
			false,true,false,false,false,false,true,false,true,false,true,true,true,false,true};
//	public static final int CORRELATION_THRESHOLD = 13;  // Accept the SYNC VECTOR if this many bits match 13 = 1 bit missed 11 = 2
	public static final int LONG_CORRELATION_THRESHOLD = 27;  // Accept the SYNC VECTOR if this correlation - 27 = 2 bits missed, 23 = 4
	public static final int SHORT_CORRELATION_THRESHOLD = 11;  // Accept the SYNC VECTOR if this correlation - 13 = 1 bit missed 11 = 2, 9 = 3 missed, 7 = 4
	public static final int FAIL = -99999;
	
	public static final int getSyncWordLength() {
		//int len = SHORT_FRAME_PRN.length;
		//if (Config.useLongPRN)
		int len = LONG_FRAME_PRN.length;
		return len;
	}
	/**
	 * We treat each binary zero as -1 and each one as 1 and then multiply the two numbers bit wise. We sum
	 * the result to get a correlation.
	 * @param word1
	 * @param word2
	 * @return
	 */
	private static int simpleBinaryCorrelation(boolean[] word1, boolean[] word2) {
		int b1 = -1;
		int b2 = -1;
		int sum = 0;
		if (word1.length != word2.length) return FAIL;
		for (int i=0; i<word1.length; i++) {
			if (word1[i] == false) b1 = -1; else b1 = 1;
			if (word2[i] == false) b2 = -1; else b2 = 1;
			sum+=b1*b2;
			
		}
		return sum;
	}
	
	/**
	 * Test function to treat the PRN SYNC as though it is a classic frame marker. Requires an exact match
	 * @param word1
	 * @return
	 */
	public static boolean equals(boolean[] word1) {
		//boolean[] FRAME_PRN = SHORT_FRAME_PRN;
		//if (Config.useLongPRN)
		boolean[] FRAME_PRN = LONG_FRAME_PRN;
		if (word1.length != FRAME_PRN.length) {
			System.err.println("PRN FRAME MARKER WRONG LENGTH");
			return false;
		}
		for (int i=0; i<word1.length; i++) {
			if (word1[i] != FRAME_PRN[i])
				return false;
		}
		return true;
	}
	
	/**
	 * Check if the passed bits correlate with the SYNC Vector
	 * @param word
	 * @return
	 */
	public static final boolean probabllyFrameMarker(boolean[] word) {
		//boolean[] FRAME_PRN = SHORT_FRAME_PRN;
		//int threshold = SHORT_CORRELATION_THRESHOLD;
		//if (Config.useLongPRN) {
		boolean[] FRAME_PRN = LONG_FRAME_PRN;
		int threshold = LONG_CORRELATION_THRESHOLD;

		if (simpleBinaryCorrelation(word, FRAME_PRN) > threshold) 
			return true;
	//	if (simpleBinaryCorrelation(word, NOT_FRAME_PRN) > CORRELATION_THRESHOLD) 
	//		return true;
		return false;
	}
	
	
	
	public static void main(String[] args) {
		boolean[] w1 = {false,true,false,true,true,true,false,false,true,false,false,false,true,true,false};
	//	boolean[] w2 = {true,false,true,false,true,true,false,false,true,false,false,false,true,true,true};
	//	boolean[] w4 = {true,true,true,true,true,true,true,true,true,false,false,true,true,false,true,false,false,true,false,false,false,false,true,false,true,false,true,true,true,false,true};
		boolean[] w3 = new boolean[31];
		for (int i=0; i<w3.length; i++) {
			int f = (LONG_FRAME_SYNC >>i) & 0x1;
//			System.out.println(f);
			if ( (f) == 0)
				w3[w3.length-1-i] = false;
			else
				w3[w3.length-1-i] = true;
		}
		boolean[] FRAME_PRN = SHORT_FRAME_PRN;
		
		System.out.println(simpleBinaryCorrelation(FRAME_PRN,w1));
//		for (int i=0; i<w3.length; i++) {
//			System.out.print(w3[i] + ",");
//		}
	}
	
	
}
