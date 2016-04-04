package decoder;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import common.Config;
import common.Log;
import filter.AGCFilter;

public class FUNcubeDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	private int lastDataValue[];
	private int clockOffset = 0;
	
	/**
     * This holds the stream of bits that we have not decoded. Once we have several
     * SYNC words, this is flushed of processed bits.
     */
    protected FUNcubeBitStream bitStream = null;  // Hold bits until we turn them into decoded frames
    
	public FUNcubeDecoder(SourceAudio as, int chan) {
		super("1200bps BPSK", as, chan);
		init();
	}

	@Override
	protected void init() {
		Log.println("Initializing 1200bps BPSK decoder: ");
		
		bitStream = new FUNcubeBitStream(50000, this);
		BITS_PER_SECOND = BITS_PER_SECOND_1200;
		SAMPLE_WINDOW_LENGTH = 10;  
		bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit
		
		BUFFER_SIZE = bytesPerSample * SAMPLE_WINDOW_LENGTH * bucketSize;
		SAMPLE_WIDTH = bucketSize*SAMPLE_WIDTH_PERCENT/100;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = bucketSize/2;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		initWindowData();
		lastDataValue = new int[bucketSize];
		
		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE /bytesPerSample));
		filter.init(currentSampleRate, 0, 0);
		
		for (int n=0; n<SINCOS_SIZE; n++) {
			cosTab[n] = Math.cos(n*2.0*Math.PI/SINCOS_SIZE);
		}
	}

	protected void resetWindowData() {
		super.resetWindowData();
		
	}
	
	
	/**
	 * Sample the buckets (one bucket per bit) to determine the change in phase and hence
	 * the bit that each bucket contains.  We use the following approach:
	 * Each sample in each bit is multiplied by the corresponding sample in the previous bit.  These multiplications
	 * These multiplications are Integrated (summed) over the bit
	 * If the total sum is positive then the phase did not change and we have a 1
	 * If the total sum is negative then the phase did change and we have a 0
	 * We store the data for the last bit so that we can use it as the "previous bit" for the first calculation next time
	 * 
	 * While we are sampling, we keep track of the clock offset in case we need to adjust it.
	 * 
	 */
	private double vcoPhase = 0.0;
	private static final double RX_CARRIER_FREQ = 1200.0;
	private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)48000;
	private static final int SINCOS_SIZE = 256;
	private double[] cosTab = new double[SINCOS_SIZE];
	
	protected void sampleBuckets2() {
		int avgClockOffset = 0;

		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			sampleNumber++;
			double sampleSum = 0;
			int samples = 0;
			
			int clockSample1 = 0, clockSample2 = 0, clockSample3 = 0;

			// Multiple and sum the samples with the previous bit
			for (int s=0; s < bucketSize; s++) {

				vcoPhase += VCO_PHASE_INC;
				if (vcoPhase > 2.0*Math.PI)
					vcoPhase -= 2.0*Math.PI;
				double product = (dataValues[i][s] * cosTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE]);
				sampleSum =+ product;
				eyeData.setData(i,s,(int) product);  // overwrite the raw waveform with the recovered bits

				samples++;


			}
			
			// bit decision based in the integral and threshold
			//System.out.println("Sum: " + sampleSum);
			if (sampleSum > 0) {
				middleSample[i] = true;
				eyeData.setHigh((int)sampleSum/samples);
			} else {
				middleSample[i] = false;
				eyeData.setLow((int)sampleSum/samples);
			}
			//System.out.println("Bit: " + i +" " + middleSample[i]);
			bitStream.addBit(middleSample[i]);

		}
		bitStream.findSyncVector();
		clockOffset = 0;
	//	System.exit(1);
	}
	
	protected void sampleBuckets() {
		long avgClockOffset = 0;
		double scale = MAX_VOLUME/32767*32767;
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			sampleNumber++;
			long sampleSum = 0;
			int samples = 0;
			
			int clockSample1 = 0, clockSample2 = 0, clockSample3 = 0;

			// Multiple and sum the samples with the previous bit
			for (int s=0; s < bucketSize; s++) {
				if (i ==0) { // first bit {
					sampleSum = sampleSum + dataValues[i][s] * lastDataValue[s];
					eyeData.setData(i,s,(int) (dataValues[i][s] * lastDataValue[s]));  // overwrite the raw waveform with the recovered bits
				} else {
					sampleSum = sampleSum + dataValues[i][s] * dataValues[i-1][s];
					eyeData.setData(i,s,(int) (dataValues[i][s] * dataValues[i-1][s]));  // overwrite the raw waveform with the recovered bits
				}
				samples++;
				

			}
			//Clock offset calculation
			if (i ==0) { // first bit {
				clockSample1 = dataValues[i][bucketSize/2] * lastDataValue[bucketSize/2]; // middle of bit
				clockSample3 = dataValues[i+1][bucketSize/2] * dataValues[i][bucketSize/2]; // middle of next
				clockSample2 = dataValues[i][bucketSize-1] * lastDataValue[bucketSize-1]; // between the samples, at end of current bit
			} else if (i< SAMPLE_WINDOW_LENGTH-1) { // all bits except the last
				clockSample1 = dataValues[i][bucketSize/2] * dataValues[i-1][bucketSize/2]; // middle of bit
				clockSample3 = dataValues[i+1][bucketSize/2] * dataValues[i][bucketSize/2]; // middle of next
				clockSample2 = dataValues[i][bucketSize-1] * dataValues[i-1][bucketSize-1]; // between the samples, at end of current bit
			}
			int clockError = (clockSample3 - clockSample1) * clockSample2;
			//System.out.println("Clock error: " + clockError);
			
			avgClockOffset =  avgClockOffset + clockError;
			
			// bit decision based in the sign of the integral
			if (sampleSum >= 0) { // This means the phase did not change
				middleSample[i] = true;
				eyeData.setHigh((int)sampleSum/samples);
			} else {
				middleSample[i] = false;
				eyeData.setLow((int)sampleSum/samples);
			}
			//System.out.println("Bit: " + i +" " + middleSample[i]);
			bitStream.addBit(middleSample[i]);
		}
		bitStream.findSyncVector();
		for (int i=0; i < bucketSize; i++) {
			lastDataValue[i] = dataValues[SAMPLE_WINDOW_LENGTH-1][i];
		 }	
		avgClockOffset = avgClockOffset / (SAMPLE_WINDOW_LENGTH-1);
		System.out.println("Clock Offset: " + avgClockOffset);
		if (avgClockOffset < 1000000)
			clockOffset = 2;
		else
			clockOffset = 0;
	//	System.exit(1);
		
	}

	/**
	 * Determine if the bit sampling buckets are aligned with the data. This is calculated when the
	 * buckets are sampled
	 * 
	 */
	@Override
	protected int recoverClockOffset() {
		
		return clockOffset;
	}

	@Override
	protected void processBitsWindow() {
		// TODO Auto-generated method stub
		
	}
	
	
}
