package decoder;

import java.util.ArrayList;

import common.Config;
import common.Log;
import common.Performance;

@SuppressWarnings("serial")
public class BitStream extends CircularBuffer {
	protected int PURGE_THRESHOLD = 100000; // Remove bits if we have accumulated this many and not found a frame
	protected int SYNC_WORD_DISTANCE = 0; // 10*(SlowSpeedFrame.getMaxBytes()+1);
	protected ArrayList<Integer> syncWords = new ArrayList<Integer>(); // The positions of all the SYNC words we have found
	protected ArrayList<SyncPair> framesTried = new ArrayList<SyncPair>(); // keep track of the SYNC word combinations that we have already tried
	
	Decoder decoder;
	
	public BitStream(int initialSize, Decoder decoder, int syncWordDistance) {
		super(initialSize);
		SYNC_WORD_DISTANCE = syncWordDistance;
		this.decoder = decoder;

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
	 * If we have too many bits, then remove one frame's worth from the beginning.  This prevents too much data from being accumulated
	 */
	protected void purgeBits() {
		
		if (this.size() > PURGE_THRESHOLD) {
			removeBits(0, SYNC_WORD_DISTANCE);
			framesTried = new ArrayList<SyncPair>();
		}
	}
	
	protected long totalBits = 0;  // add this to the current bits to get the total bits in the frame
	
	public long getStartOfWindowBit() {
		return totalBits + this.size()-1;
	}
	
	/**
	 * Remove bits from the bit list and update the position of any
	 * frameMarker Candidates
	 * @return
	 */
	public void removeBits(int start, int end) {
		if (Config.debugFrames) Log.println("Purging " + (end - start) + " bits");
		totalBits = totalBits + (end-start);
/*		if (Config.debugFrames) {
			if (Config.iq)
			Log.println("Pass State: " + Config.passManager.getState() + ""
				+ " at Bin: " + Config.selectedBin
				+ " Strongest (dB): " + MainWindow.inputTab.iqSource1.rfData.getAvg(RfData.STRONGEST_SIG)
				+ " Bin: " + MainWindow.inputTab.iqSource1.rfData.getBinOfStrongestSignal()
				+ " Peak (dB): " + MainWindow.inputTab.iqSource1.rfData.getAvg(RfData.PEAK)
				+ " Bin: " + MainWindow.inputTab.iqSource1.rfData.getBinOfPeakSignal()	);
		}*/
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
		for (int j=0; j< framesTried.size(); j++) {
			int marker = framesTried.get(j).word1 - distance;
			if (marker < 0) { 
				framesTried.remove(j);
				j=j-1;
			} else
				framesTried.set(j, new SyncPair(marker, marker+SYNC_WORD_DISTANCE));
		}
	}
	
	protected class SyncPair {
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
