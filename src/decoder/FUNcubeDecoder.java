package decoder;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import common.Log;
import filter.AGCFilter;

public class FUNcubeDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	/**
     * This holds the stream of bits that we have not decoded. Once we have several
     * SYNC words, this is flushed of processed bits.
     */
    protected BitStream bitStream = null;  // Hold bits until we turn them into decoded frames
    
	public FUNcubeDecoder(SourceAudio as, int chan) {
		super("1200bps BPSK", as, chan);
		init();
	}

	@Override
	protected void init() {
		Log.println("Initializing 1200bps BPSK decoder: ");
		
		bitStream = new BitStream(5000);
		BITS_PER_SECOND = BITS_PER_SECOND_1200;
		SAMPLE_WINDOW_LENGTH = 10;  
		bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit
		
		BUFFER_SIZE = bytesPerSample * SAMPLE_WINDOW_LENGTH * bucketSize;
		SAMPLE_WIDTH = bucketSize*SAMPLE_WIDTH_PERCENT/100;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = 10;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		initWindowData();
		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE /bytesPerSample));
		filter.init(currentSampleRate, 0, 0);
	}

	@Override
	protected void sampleBuckets() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int recoverClockOffset() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void processBitsWindow() {
		// TODO Auto-generated method stub
		
	}
	
	
}
