package FuncubeDecoder;

import common.Log;
import decoder.BitStream;
import decoder.Decoder;

public class FUNcubeBitStream extends BitStream {
	private static final long serialVersionUID = 1L;
	private static final int FEC_BITS_SIZE = 5200;
	private static final int FEC_BLOCK_SIZE = 256;
	private static final int SYNC_VECTOR_SIZE = 65;
	private static final byte[] SYNC_VECTOR = {	// 65 symbol known pattern
		1,1,1,1,1,1,1,-1,-1,-1,-1,1,1,1,-1,1,1,1,1,-1,-1,1,-1,1,1,-1,-1,1,-1,-1,1,-1,-1,-1,-1,-1,-1,1,-1,-1,-1,1,-1,-1,1,1,-1,-1,-1,1,-1,1,1,1,-1,1,-1,1,1,-1,1,1,-1,-1,-1
	};
	
	public FUNcubeBitStream(int initialSize, Decoder decoder) {
		super(initialSize, decoder, FEC_BITS_SIZE);
		SYNC_WORD_DISTANCE = FEC_BITS_SIZE; // for the purge, set the distance (which is not used) to the frame size
		PURGE_THRESHOLD = FEC_BITS_SIZE * 5;
	}
	
	/**
	 * We just added a bit, so check if the SYNC_VECTOR is present
	 * @param windowLength
	 * @return true if we found the SYNC word
	 */
	public boolean checkSyncVector() {
		boolean found = false;
		if (this.size() < (FEC_BITS_SIZE)) return false; // we don't have enough bits to try a correlation
		int start = this.size()-FEC_BITS_SIZE;
		int corr = 0;
		for (int i=start; i < SYNC_VECTOR_SIZE; i++) {
			byte b = (byte)(this.get(i*80) ? 1: -1);
			corr+=b*SYNC_VECTOR[i];
		}
	//	if (corr > 30)
			System.out.println("CORR: " + corr );
		if (corr > 45) {

			Log.println("FOUND THE SYNC VECTOR!!!!!!!!!!!!!!!!!!!!!!!!!!!");

			// good correlation, attempt full FEC decode
			byte[] rawFrame = new byte[FEC_BITS_SIZE];
			byte[] rawBits = new byte[FEC_BITS_SIZE];
			byte[] decoded = new byte[FEC_BLOCK_SIZE];
			for (int n=0; n<FEC_BITS_SIZE; n++) {
				rawFrame[n] = (byte)(rawBits[n]==1 ? 0xc0 : 0x40);
			}
			FECDecoder fecDecoder = new FECDecoder();
			int err=fecDecoder.FECDecode(rawFrame, decoded);

			Log.println("FEC SAID: " + err);		
		}
		return found;
	}

}
