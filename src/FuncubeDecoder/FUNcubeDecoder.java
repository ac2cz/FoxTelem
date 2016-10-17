package FuncubeDecoder;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import common.Config;
import common.Log;
import decoder.Decoder;
import decoder.SourceAudio;
import filter.AGCFilter;

public class FUNcubeDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	private int lastDataValue[];
	private int clockOffset = 0;
	private double[] cosTab;
	private double[] sinTab;
	
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
		
		bitStream = new FUNcubeBitStream(500000, this);
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
		
		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
		filter.init(currentSampleRate, 0, 0);
		
		cosTab = new double[SINCOS_SIZE];
		sinTab = new double[SINCOS_SIZE];
		
		for (int n=0; n<SINCOS_SIZE; n++) {
			cosTab[n] = Math.cos(n*2.0*Math.PI/SINCOS_SIZE);
			sinTab[n] = Math.sin(n*2.0*Math.PI/SINCOS_SIZE);
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
	//private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)48000;
	private static final int SINCOS_SIZE = 256;

	
	protected void sampleBucketsVCO() {
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
				double product = (dataValues[i][s] * sinTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE]);
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
			System.out.println("Bit: " + i +" " + middleSample[i]);
			bitStream.checkSyncVector();

		}
		clockOffset = 0;
		
	}
	
	protected void sampleBuckets() {
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {

			for (int s=0; s < bucketSize; s++) {
				RxDownSample(dataValues[i][s]/ 32768.0,dataValues[i][s]/ 32768.0);
			}
		}
	}
	protected void sampleBuckets1BitDelay() {
		long avgClockOffset = 0;
		double scale = MAX_VOLUME/32767*32767;
		double maxBitEnergy = 0;
		int maxBitEnergyIdx = 0;
		int sumMaxBitEnergyIdx = 0;
		
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			sampleNumber++;
			long sampleSum = 0;
			int samples = 0;
			maxBitEnergyIdx = 0;
			
			int clockSample1 = 0, clockSample2 = 0, clockSample3 = 0;

			for (int s=0; s < bucketSize; s++) {
				// detect the peak energy for clock sync
				double energy = dataValues[i][s] * dataValues[i][s];
				//Log.println("E:" + energy);
				if (energy > maxBitEnergy) {
					maxBitEnergy = energy;
					maxBitEnergyIdx = s;
				}
				// Multiple and sum the samples with the previous bit
				if (i ==0) { // first bit {
					sampleSum = sampleSum + dataValues[i][s] * lastDataValue[s];
					eyeData.setData(i,s,(int) (dataValues[i][s] * lastDataValue[s]));  // overwrite the raw waveform with the recovered bits
				} else {
					sampleSum = sampleSum + dataValues[i][s] * dataValues[i-1][s];
					eyeData.setData(i,s,(int) (dataValues[i][s] * dataValues[i-1][s]));  // overwrite the raw waveform with the recovered bits
					if (i == SAMPLE_WINDOW_LENGTH-1) {
						// last bit, store the value for next time
						lastDataValue[s] = dataValues[i][s];
					}
				}
				samples++;
			}
			sumMaxBitEnergyIdx = sumMaxBitEnergyIdx + maxBitEnergyIdx;
			
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
			} else { // we had a phase change, so a zero
				middleSample[i] = false;
				eyeData.setLow((int)sampleSum/samples);
			}
		//	System.out.println("Bit: " + i +" " + middleSample[i]);
			bitStream.addBit(middleSample[i]);
			bitStream.checkSyncVector();
		}
		int avgMaxBitEnergyIdx = sumMaxBitEnergyIdx / SAMPLE_WINDOW_LENGTH;
//		System.out.println(bucketSize/4 + ": Max Energy: " + avgMaxBitEnergyIdx);
		
//		for (int i=0; i < bucketSize; i++) {
//			lastDataValue[i] = dataValues[SAMPLE_WINDOW_LENGTH-1][i];
//		 }	
		avgClockOffset = avgClockOffset / (SAMPLE_WINDOW_LENGTH-1);
		
		if (avgMaxBitEnergyIdx > bucketSize/4)
			clockOffset = 1;
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
	
	protected double[] recoverClock(int factor) {

    	if (clockOffset > 0) {
    	// There are 40 samples in a 1200bps bucket. The clock offset 
    		double[] clockData = new double[clockOffset];
    		if (Config.debugClock) Log.println("Advancing clock " + clockOffset + " samples");
    		int nBytesRead = read(clockData);
    		if (nBytesRead != (clockOffset*bytesPerSample)) {
    			if (Config.debugClock) Log.println("ERROR: Could not advance clock");
    		} else {
    			// This is the new clock offsest
    			// Reprocess the data in the current window
    			return clockData;
    		}
    	} else {
    		if (Config.debugClock) Log.println("PSK CLOCK STABLE");
    		return null;
    	}
    	return null;
	}

	@Override
	protected void processBitsWindow() {
		// TODO Auto-generated method stub
		
	}
	
	private static final int DOWN_SAMPLE_FILTER_SIZE = 27;
	private static final double[] dsFilter = {
		-6.103515625000e-004F,  /* filter tap #    0 */
		-1.220703125000e-004F,  /* filter tap #    1 */
		+2.380371093750e-003F,  /* filter tap #    2 */
		+6.164550781250e-003F,  /* filter tap #    3 */
		+7.324218750000e-003F,  /* filter tap #    4 */
		+7.629394531250e-004F,  /* filter tap #    5 */
		-1.464843750000e-002F,  /* filter tap #    6 */
		-3.112792968750e-002F,  /* filter tap #    7 */
		-3.225708007813e-002F,  /* filter tap #    8 */
		-1.617431640625e-003F,  /* filter tap #    9 */
		+6.463623046875e-002F,  /* filter tap #   10 */
		+1.502380371094e-001F,  /* filter tap #   11 */
		+2.231445312500e-001F,  /* filter tap #   12 */
		+2.518310546875e-001F,  /* filter tap #   13 */
		+2.231445312500e-001F,  /* filter tap #   14 */
		+1.502380371094e-001F,  /* filter tap #   15 */
		+6.463623046875e-002F,  /* filter tap #   16 */
		-1.617431640625e-003F,  /* filter tap #   17 */
		-3.225708007813e-002F,  /* filter tap #   18 */
		-3.112792968750e-002F,  /* filter tap #   19 */
		-1.464843750000e-002F,  /* filter tap #   20 */
		+7.629394531250e-004F,  /* filter tap #   21 */
		+7.324218750000e-003F,  /* filter tap #   22 */
		+6.164550781250e-003F,  /* filter tap #   23 */
		+2.380371093750e-003F,  /* filter tap #   24 */
		-1.220703125000e-004F,  /* filter tap #   25 */
		-6.103515625000e-004F   /* filter tap #   26 */
	};
//	private static final double DOWN_SAMPLE_MULT = 0.9*32767.0;	// XXX: Voodoo from Howard?
	private static final int MATCHED_FILTER_SIZE = 65;
	private static final double[] dmFilter = {
		-0.0101130691F,-0.0086975143F,-0.0038246093F,+0.0033563764F,+0.0107237026F,+0.0157790936F,+0.0164594107F,+0.0119213911F,
		+0.0030315224F,-0.0076488191F,-0.0164594107F,-0.0197184277F,-0.0150109226F,-0.0023082460F,+0.0154712381F,+0.0327423589F,
		+0.0424493086F,+0.0379940454F,+0.0154712381F,-0.0243701991F,-0.0750320094F,-0.1244834076F,-0.1568500423F,-0.1553748911F,
		-0.1061032953F,-0.0015013786F,+0.1568500423F,+0.3572048240F,+0.5786381191F,+0.7940228249F,+0.9744923010F,+1.0945250059F,
		+1.1366117829F,+1.0945250059F,+0.9744923010F,+0.7940228249F,+0.5786381191F,+0.3572048240F,+0.1568500423F,-0.0015013786F,
		-0.1061032953F,-0.1553748911F,-0.1568500423F,-0.1244834076F,-0.0750320094F,-0.0243701991F,+0.0154712381F,+0.0379940454F,
		+0.0424493086F,+0.0327423589F,+0.0154712381F,-0.0023082460F,-0.0150109226F,-0.0197184277F,-0.0164594107F,-0.0076488191F,
		+0.0030315224F,+0.0119213911F,+0.0164594107F,+0.0157790936F,+0.0107237026F,+0.0033563764F,-0.0038246093F,-0.0086975143F,
		-0.0101130691F,
		-0.0101130691F,-0.0086975143F,-0.0038246093F,+0.0033563764F,+0.0107237026F,+0.0157790936F,+0.0164594107F,+0.0119213911F,
		+0.0030315224F,-0.0076488191F,-0.0164594107F,-0.0197184277F,-0.0150109226F,-0.0023082460F,+0.0154712381F,+0.0327423589F,
		+0.0424493086F,+0.0379940454F,+0.0154712381F,-0.0243701991F,-0.0750320094F,-0.1244834076F,-0.1568500423F,-0.1553748911F,
		-0.1061032953F,-0.0015013786F,+0.1568500423F,+0.3572048240F,+0.5786381191F,+0.7940228249F,+0.9744923010F,+1.0945250059F,
		+1.1366117829F,+1.0945250059F,+0.9744923010F,+0.7940228249F,+0.5786381191F,+0.3572048240F,+0.1568500423F,-0.0015013786F,
		-0.1061032953F,-0.1553748911F,-0.1568500423F,-0.1244834076F,-0.0750320094F,-0.0243701991F,+0.0154712381F,+0.0379940454F,
		+0.0424493086F,+0.0327423589F,+0.0154712381F,-0.0023082460F,-0.0150109226F,-0.0197184277F,-0.0164594107F,-0.0076488191F,
		+0.0030315224F,+0.0119213911F,+0.0164594107F,+0.0157790936F,+0.0107237026F,+0.0033563764F,-0.0038246093F,-0.0086975143F,
		-0.0101130691F
	};
	private static final int SYNC_VECTOR_SIZE = 65;
	private static final byte[] SYNC_VECTOR = {	// 65 symbol known pattern
		1,1,1,1,1,1,1,-1,-1,-1,-1,1,1,1,-1,1,1,1,1,-1,-1,1,-1,1,1,-1,-1,1,-1,-1,1,-1,-1,-1,-1,-1,-1,1,-1,-1,-1,1,-1,-1,1,1,-1,-1,-1,1,-1,1,1,1,-1,1,-1,1,1,-1,1,1,-1,-1,-1
	};
	private static final int FEC_BITS_SIZE = 5200;
	private static final int FEC_BLOCK_SIZE = 256;
	private static final int DOWN_SAMPLE_RATE = 9600;
	private static final int BIT_RATE = 1200;
	private static final int SAMPLES_PER_BIT = DOWN_SAMPLE_RATE/BIT_RATE;
	private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)DOWN_SAMPLE_RATE;
	private static final double BIT_SMOOTH1 = 1.0/200.0;
	private static final double BIT_SMOOTH2 = 1.0/800.0;
	private static final double BIT_PHASE_INC = 1.0/(double)DOWN_SAMPLE_RATE;
	private static final double BIT_TIME = 1.0/(double)BIT_RATE;



	private AudioFormat format;
	private boolean decodeOK = false;
	private byte[] decoded = new byte[FEC_BLOCK_SIZE];
	private FECDecoder decoder = new FECDecoder();
	private int cntRaw, cntDS, cntBit, cntFEC, cntDec, dmErrBits;
	private double energy1, energy2;

	// debugging stuff
	//private double[] tuned;
	//private int tunedIdx;
	//private double[] downSmpl;
	//private int downSmplIdx;
	//private double[] demodIQ;
	//private int[] demodBits;
	//private double[] demodRe;
	//private int demodIdx, demodLastBit;
	// Down sample from input rate to DOWN_SAMPLE_RATE and low pass filter
		private double[][] dsBuf = new double[DOWN_SAMPLE_FILTER_SIZE][2];
		private int dsPos = DOWN_SAMPLE_FILTER_SIZE-1, dsCnt = 0;
		private double HOWARD_FUDGE_FACTOR = 0.9 * 32768.0;
		private void RxDownSample(double i, double q) {
		//	tuned[tunedIdx]=i;
		//	tuned[tunedIdx+1]=q;
		//	tunedIdx = (tunedIdx+2) % tuned.length;
			dsBuf[dsPos][0]=i;
			dsBuf[dsPos][1]=q;
			if (++dsCnt>=(int)currentSampleRate/DOWN_SAMPLE_RATE) {	// typically 96000/9600
				double fi = 0.0, fq = 0.0;
				// apply low pass FIR
				for (int n=0; n<DOWN_SAMPLE_FILTER_SIZE; n++) {
					int dsi = (n+dsPos)%DOWN_SAMPLE_FILTER_SIZE; 
					fi+=dsBuf[dsi][0]*dsFilter[n];
					fq+=dsBuf[dsi][1]*dsFilter[n];
				}
				dsCnt=0;
				// feed down sampled values to demodulator
				RxDemodulate(fi * HOWARD_FUDGE_FACTOR, fq * HOWARD_FUDGE_FACTOR);
			}
			dsPos--;
			if (dsPos<0)
				dsPos=DOWN_SAMPLE_FILTER_SIZE-1;
			cntRaw++;
		}
		
		private double[][] dmBuf = new double[MATCHED_FILTER_SIZE][2];
		private int dmPos = MATCHED_FILTER_SIZE-1;
		private double[] dmEnergy = new double[SAMPLES_PER_BIT+2];
		private int dmBitPos = 0, dmPeakPos = 0, dmNewPeak = 0, dmCorr = 0, dmMaxCorr = 0;
		private double dmEnergyOut = 1.0;
		private int[] dmHalfTable = {4,5,6,7,0,1,2,3};
		private double dmBitPhase = 0.0;
		private double[] dmLastIQ = new double[2];
		private byte[] dmFECCorr = new byte[FEC_BITS_SIZE];
		private byte[] dmFECBits = new byte[FEC_BITS_SIZE];
		private void RxDemodulate(double i, double q) {
			// debugging
		//	downSmpl[downSmplIdx]=i;
		//	downSmpl[downSmplIdx+1]=q;
		//	downSmplIdx=(downSmplIdx+2)%downSmpl.length;
			// advance phase of VCO, wrap at 2*Pi
			vcoPhase += VCO_PHASE_INC;
			if (vcoPhase > 2.0*Math.PI)
				vcoPhase -= 2.0*Math.PI;
			// quadrature demodulate carrier to base band with VCO, store in FIR buffer
			dmBuf[dmPos][0]=i*cosTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
			dmBuf[dmPos][1]=q*sinTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
			// apply FIR (base band smoothing, root raised cosine)
			double fi = 0.0, fq = 0.0;
			for (int n=0; n<MATCHED_FILTER_SIZE; n++) {
				int dmi = (MATCHED_FILTER_SIZE-dmPos+n);
				fi+=dmBuf[n][0]*dmFilter[dmi];
				fq+=dmBuf[n][1]*dmFilter[dmi];
			}
			dmPos--;
			if (dmPos<0)
				dmPos=MATCHED_FILTER_SIZE-1;

			// debug IQ data
//			demodIdx = (demodIdx+2)%demodIQ.length;
//			demodIQ[demodIdx]=fi;
//			demodIQ[demodIdx+1]=fq;

			// store smoothed bit energy
			energy1 = fi*fi+fq*fq;
			dmEnergy[dmBitPos] = (dmEnergy[dmBitPos]*(1.0-BIT_SMOOTH1))+(energy1*BIT_SMOOTH1);
			// at peak bit energy? decode 
			if (dmBitPos==dmPeakPos) {
				dmEnergyOut = (dmEnergyOut*(1.0-BIT_SMOOTH2))+(energy1*BIT_SMOOTH2);
				double di = -(dmLastIQ[0]*fi + dmLastIQ[1]*fq);
				double dq = dmLastIQ[0]*fq - dmLastIQ[1]*fi;
				dmLastIQ[0]=fi;
				dmLastIQ[1]=fq;
				energy2 = Math.sqrt(di*di+dq*dq);
				if (energy2>100.0) {	// TODO: work out where these magic numbers come from!
					boolean bit = di<0.0;	// is that a 1 or 0?
					// debug bit
					//for (int bi=demodLastBit; bi!=demodIdx; bi=(bi+1)%demodBits.length)
					//	demodBits[bi] = demodBits[demodLastBit];
//					demodRe[demodIdx/2] = di;
//					demodBits[demodIdx/2]=bit ? 1 : -1;
//					demodLastBit=demodIdx/2;
					// copy bit into rolling buffer of FEC bits
					System.arraycopy(dmFECCorr,1,dmFECCorr,0,dmFECCorr.length-1);
					dmFECCorr[dmFECCorr.length-1] = (byte)(bit ? 1: -1);
					// detect sync vector by correlation
					dmCorr = 0;
					for (int n=0; n<SYNC_VECTOR_SIZE; n++) {
						dmCorr+=dmFECCorr[n*80]*SYNC_VECTOR[n];
					}
					if (dmCorr>=45) {
						// good correlation, attempt full FEC decode
						Log.println("FOUND SYNC VECTOR!!!!!!!!!!!!!!!!");
						for (int n=0; n<FEC_BITS_SIZE; n++) {
							dmFECBits[n] = (byte)(dmFECCorr[n]==1 ? 0xc0 : 0x40);
						}
						dmErrBits=decoder.FECDecode(dmFECBits, decoded);
						cntFEC++;
						dmMaxCorr=0;
						decodeOK = dmErrBits<0 ? false : true;
						Log.println("FEC DECODE: " + decodeOK);
						if (decodeOK) {
							FUNcubeFrame fcf = new FUNcubeFrame();
							fcf.addRawFrame(decoded);
							Log.println(fcf.toString());
							if (Config.storePayloads) {
								Config.payloadStore.add(fcf.header.id, 0, 0, fcf.rtPayload);
							}
						}
						//cntDec += (decodeOK ? 1 : 0);
					}
					if (dmCorr > dmMaxCorr)
						dmMaxCorr=dmCorr;
					cntBit++;
				}
			}
			// half-way into next bit? reset peak energy point
			if (dmBitPos==dmHalfTable[dmPeakPos])
				dmPeakPos = dmNewPeak;
			dmBitPos = (dmBitPos+1) % SAMPLES_PER_BIT;
			// advance phase of bit position
			dmBitPhase += BIT_PHASE_INC;
			if (dmBitPhase>=BIT_TIME) {
				dmBitPhase-=BIT_TIME;
				dmBitPos=0;	// TODO: Is this a kludge?
				// rolled round another bit, measure new peak energy position
				double eMax = -1.0e10F;
				for (int n=0; n<SAMPLES_PER_BIT; n++) {
					if (dmEnergy[n]>eMax) {
						dmNewPeak=n;
						eMax=dmEnergy[n];
					}
				}
			}
			cntDS++;
		}

}
