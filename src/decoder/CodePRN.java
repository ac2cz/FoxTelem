package decoder;

public class CodePRN {
	public static final int FRAME = 0x5647 & 0x7fff;  //101011001000111
	public static final int LONG_FRAME_SYNC = 0x47cd215d;//
	//static boolean[] FRAME_PRN = {true,false,true,false,true,true,false,false,true,false,false,false,true,true,true};
	static boolean[] NOT_FRAME_PRN = {false,true,false,true,false,false,true,true,false,true,true,true,false,false,false};
	static boolean[] FRAME_PRN = {true,false,false,false,true,true,true,true,true,false,false,true,true,false,true,false,
			false,true,false,false,false,false,true,false,true,false,true,true,true,false,true};
//	public static final int CORRELATION_THRESHOLD = 13;  // Accept the SYNC VECTOR if this many bits match
	public static final int CORRELATION_THRESHOLD = 27;  // Accept the SYNC VECTOR if this correlation
	public static final int FAIL = -99999;
	
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
		if (simpleBinaryCorrelation(word, FRAME_PRN) > CORRELATION_THRESHOLD) 
			return true;
	//	if (simpleBinaryCorrelation(word, NOT_FRAME_PRN) > CORRELATION_THRESHOLD) 
	//		return true;
		return false;
	}
	
	
	
	public static void main(String[] args) {
		boolean[] w1 = {false,false,true,false,true,true,false,false,true,false,false,false,true,true,true};
		boolean[] w2 = {true,false,true,false,true,true,false,false,true,false,false,false,true,true,true};
		boolean[] w4 = {false,true,false,false,true,true,true,true,true,false,false,true,true,false,true,false,false,true,false,false,false,false,true,false,true,false,true,true,true,false,true};
		boolean[] w3 = new boolean[31];
		for (int i=0; i<w3.length; i++) {
			int f = (LONG_FRAME_SYNC >>i) & 0x1;
//			System.out.println(f);
			if ( (f) == 0)
				w3[w3.length-1-i] = false;
			else
				w3[w3.length-1-i] = true;
		}
		System.out.println(simpleBinaryCorrelation(FRAME_PRN,w4));
//		for (int i=0; i<w3.length; i++) {
//			System.out.print(w3[i] + ",");
//		}
	}
	
	
}
