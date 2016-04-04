package decoder;

import FuncubeDecoder.FECDecoder;
import common.Config;
import common.Log;

public class FUNcubeBitStream extends BitStream {

	private static final int FEC_BITS_SIZE = 5200;
	private static final int FEC_BLOCK_SIZE = 256;
	private static final int SYNC_VECTOR_SIZE = 65;
	private static final byte[] SYNC_VECTOR = {	// 65 symbol known pattern
		1,1,1,1,1,1,1,-1,-1,-1,-1,1,1,1,-1,1,1,1,1,-1,-1,1,-1,1,1,-1,-1,1,-1,-1,1,-1,-1,-1,-1,-1,-1,1,-1,-1,-1,1,-1,-1,1,1,-1,-1,-1,1,-1,1,1,1,-1,1,-1,1,1,-1,1,1,-1,-1,-1
	};
	
	public FUNcubeBitStream(int initialSize, Decoder decoder) {
		super(initialSize, decoder);
		SYNC_WORD_DISTANCE = FEC_BLOCK_SIZE; // for the purge, set the distance (which is not used) to the frame size
	}
	
	/**
	 * We just added windowLength bits.  Perhaps this completed the SYNC_VECTOR.  So search back through the last bits and test to see if the SYNC Vector 
	 * is present.  To be sure we need to look back through windowLength + SYNC_VECTOR_SIZE -1 bits, assuming that many are present
	 * @param windowLength
	 * @return true if we found the SYNC word
	 */
	public boolean findSyncVector() {
		FECDecoder fecDecoder = new FECDecoder();
		byte[] rawFrame = new byte[FEC_BLOCK_SIZE];
		byte[] decoded = new byte[FEC_BLOCK_SIZE];
		
		boolean found = false;
		int position = 0;
		byte[] rawBits = new byte[FEC_BITS_SIZE];
		if (this.size() < (FEC_BITS_SIZE+1)) return false;
		
		int start = this.size()-FEC_BITS_SIZE;
		for (int i=start; i < this.size(); i++) {
			rawBits[position++] = (byte)(this.get(i) ? 1: -1);

			// detect sync vector by correlation
			
			// to make this efficient we don't want to pull out all 5200 bits.  We want to directly correlate against the bits in the buffer
			// We want to do that for the latest, then slide the window each time we add a bit, always just looking at the 65 bits that would
			// fit under the SYNC VECTOR
			
			int corr = 0;

			corr+=rawBits[(i-start)*80]*SYNC_VECTOR[i-start];
			System.out.println("CORR: " + corr );
			if (corr>=45) {
				System.err.println("FOUND VECTOR!");
				// good correlation, attempt full FEC decode
				for (int n=0; n<FEC_BITS_SIZE; n++) {
					rawFrame[n] = (byte)(rawBits[n]==1 ? 0xc0 : 0x40);
				}
				int err=fecDecoder.FECDecode(rawFrame, decoded);

				found = err <0 ? false : true;
				if (found) {
					found = true;
					syncWords.add(i+1);
					if (Config.debugFrames) {
						Log.println("SYNC WORD "+ syncWords.size() + " ADDED AT: "+ (i+1));
					}
				} 
			}
		}
		return found;
	}

}
