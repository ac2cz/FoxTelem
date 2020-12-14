package decoder.FoxBPSK;

import common.Config;
import common.Log;
import decoder.SourceAudio;
import decoder.SourceIQ;
import filter.AGCFilter;
import filter.DcRemoval;
import telemetry.TelemFormat;
import filter.Complex;
import filter.ComplexOscillator;
import filter.IirFilter;
import filter.RaisedCosineFilter;
import filter.RootRaisedCosineFilter;

public class FoxBPSKCostasDecoder extends FoxBPSKDecoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	public static final int WORD_LENGTH = 10;
	//private double sumClockError = 0;
	private int samplePoint = 20;
	public static final int AUDIO_MODE = 0; // means just take final audio of the bits and decode, which is used in IQ mode!
	public static final int PSK_MODE = 1; // this actually decodes the PSK and confusingly is used in AF mode
	public int mode = AUDIO_MODE;

	DcRemoval audioDcFilter;

	ComplexOscillator nco = new ComplexOscillator(currentSampleRate, 1200);

	RaisedCosineFilter dataFilter;
	//RaisedCosineFilter iFilter;
	//RaisedCosineFilter qFilter;
	//RaisedCosineFilter loopFilter;
	IirFilter iFilter;
	IirFilter qFilter;
	IirFilter loopFilter;

	double[] pskAudioData;
	double[] pskQAudioData;
	double[] phasorData;

	double gain = 1.0;

	double alpha = 0.1; //the feedback coeff  0 - 4.  But typical range is 0.01 and smaller.  
	double beta = 64*alpha*alpha / 4.0d;  // alpha * alpha / 4 is critically damped. 
	double error;  // accumulation of the error over a buffer
	//double loopError;
	boolean lastPhase = false;
	double freq = 700.0d;
	public double LOW_SWEEP_LIMIT = 700.0;
	public double HIGH_SWEEP_LIMIT = 5000.0;
	
	double iMix, qMix;
	double fi = 0.0, fq = 0.0;
	double ri, rq;
	double lockLevel, avgLockLevel;
	public static final double LOCK_LEVEL_THRESHOLD = 3000;
	public static final double FREQ_SWEEP_INCREMENT = 0.04;


	// Kep track of where we are in the bit for sampling
//	int bitNumber = 0; // for eye diagram only
	int bitPosition = 0;
	int offset = 0;
	double YnMinus2Sample = 0;
	double YnSample = 0;
	double YnMinus1Sample;
	boolean delayClock = false;
	double psk;
	//CosOscillator testOscillator = new CosOscillator(48000,1200);

	public FoxBPSKCostasDecoder(SourceAudio as, int chan, int mode, TelemFormat telemFormat) {
		super("1200bps BPSK", as, chan, telemFormat);
		this.mode = mode;
		init();
	}

	@Override
	protected void init() {
		if (!Config.iq && Config.use12kHzIfForBPSK) {
			LOW_SWEEP_LIMIT = 10000;
			HIGH_SWEEP_LIMIT = 14000;
		} else {
			LOW_SWEEP_LIMIT = 700;
			HIGH_SWEEP_LIMIT = 5000;
		}
			
		Log.println("Initializing 1200bps Costas Loop BPSK decoder: ");
		BITS_PER_SECOND = BITS_PER_SECOND_1200;
		SAMPLE_WINDOW_LENGTH = 40; //40;  
		bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit
		samplePoint = bucketSize/2;
		BUFFER_SIZE =  SAMPLE_WINDOW_LENGTH * bucketSize;
		SAMPLE_WIDTH = 1;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = bucketSize/2;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		initWindowData();

		audioDcFilter = new DcRemoval(0.9999d);
		if (mode == PSK_MODE) { // costas psk demod mode
			filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
			filter.init(currentSampleRate, 0, 0);
		} else {
			filter = new RootRaisedCosineFilter(audioSource.audioFormat, (BUFFER_SIZE));
			filter.init(currentSampleRate, 1200, 512);
		}

		dataFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		dataFilter.init(currentSampleRate, HIGH_SWEEP_LIMIT+1000, 256); // just remove noise, perhaps at quarter sample rate? Better wider and steeper to give selectivity

		//		iFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		//		iFilter.init(48000, 1200, 128);

		//		qFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		//		qFilter.init(48000, 1200, 128);

		// 4 pole cheb at fc = 0.025 = 1200Kz at 48k.  Ch 20 Eng and Sci guide to DSP
		double[] a = {1.504626E-05, 6.018503E-05, 9.027754E-05, 6.018503E-05, 1.504626E-05};
		double[] b = {1, 3.725385E+00, -5.226004E+00,  3.270902E+00,  -7.705239E-01};

		iFilter = new IirFilter(a,b);
		qFilter = new IirFilter(a,b);

		// Single pole IIR from Eng and Scientists Guide to DSP ch 19.  Higher X is amount of decay.  Higher X is slower
		// decay. x = e^-1/d where d is number of samples for decay. x = e^-2*pi*fc
		double x = 0.3678;//0.86 is 6 samples, 0.606 is 2 samples 0.3678 is 1 sample, 0.1353 is 0.5 samples
		double[] a2 = {1-x};
		double[] b2 = {1, x};

		loopFilter = new IirFilter(a2,b2);

		//	loopFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		//	loopFilter.init(48000, 50, 32); // this filter should not contribute to lock.  Should be far outside the closed loop response.

		pskAudioData = new double[BUFFER_SIZE];
		pskQAudioData = new double[BUFFER_SIZE];
		phasorData = new double[BUFFER_SIZE*2]; // actually it stores baseband i and q
		
	}

	protected void resetWindowData() {
		super.resetWindowData();

	}

	public double[] getBasebandData() {
		return pskAudioData;
	}

	public double[] getBasebandQData() {
		return pskQAudioData;
	}
	
	public double[] getPhasorData() {
		if (mode == PSK_MODE)
			return phasorData;
		else
			return ((SourceIQ)audioSource).getPhasorData();
	}

	protected void sampleBuckets() {
		double maxValue = 0;
		double minValue = 0;
		double DESIRED_RANGE = 2;
		int sumLockLevel = 0;

		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			for (int s=0; s < bucketSize; s++) {
				double value = dataValues[i][s]/ 32767.0;
				
				if (value > maxValue) maxValue = value;
				if (value < minValue) minValue = value;

				value = value*gain;
				if (mode == PSK_MODE) {
					psk = costasLoop(value, value, i, s);
					sumLockLevel += lockLevel;
				} else {
					psk = value;
				}	
				int eyeValue = (int)(-1*psk*32767.0); 

				if (mode == PSK_MODE) {
					//if ) {
						nco.changePhase(alpha*error);
						freq = freq + beta*error;
						if (avgLockLevel < LOCK_LEVEL_THRESHOLD) {
							freq = freq + FREQ_SWEEP_INCREMENT; // susceptible to false lock at half the bitrate
							if (freq > HIGH_SWEEP_LIMIT) freq = LOW_SWEEP_LIMIT;
						} 
						if (freq > HIGH_SWEEP_LIMIT) freq = HIGH_SWEEP_LIMIT;
						if (freq < LOW_SWEEP_LIMIT) freq = LOW_SWEEP_LIMIT;
						nco.setFrequency(freq);
					//}
				}
				if (bitPosition == 0 ) {
					YnMinus1Sample = psk;
				}

				if (bitPosition == samplePoint  ) {
					if (delayClock) // we already sampled this
						delayClock = false;
					else {
						// ALL END OF BIT LOGIC TO DO WITH BIT CHOICE MUST GO HERE TO BE IN SYNC

						//System.err.println("Bit: " + bitPosition + " Sample: " +((Config.windowsProcessed-1)*SAMPLE_WINDOW_LENGTH+i) + " " + psk + " >>");
						YnMinus2Sample = YnSample;
						YnSample = psk;

						boolean thisPhase = false;
						boolean thisSample = false;
						if (YnSample > 0) {
							thisPhase = true;
						} else {
						}

						// If this bit looks like the last bit then we did not change phase and we have a 1
						if (thisPhase == lastPhase)
							thisSample = true;
						else // phase change so a zero
							thisSample = false;
						lastPhase = thisPhase;
						bitStream.addBit(thisSample);
						middleSample[i] = thisSample;

						if (Config.debugValues) {
							psk = psk*1.5;
						}
						//eyeValue = (int) (psk*-32768);
						//HERE WE DO THE GARDNER ALGORITHM AND UPDATE THE SAMPLE POINTS WITH WRAP.
						// Gardner Error calculation
						// error = (Yn -Yn-2)*Yn-1
						double clockError = (YnSample - YnMinus2Sample) * YnMinus1Sample;
						// uncomment if clock calc per bit
						double errThreshold = 0.1;
						if (clockError < -1*errThreshold) {
							delayClock = true;
							bitPosition = bitPosition - 1;
						} else if (clockError > errThreshold) // sample is late because error is positive, so sample earlier by increasing bitPosition
							bitPosition = bitPosition + 1;

						if (thisPhase == false)
							eyeData.setLow((int) (YnSample*32767));
//							eyeData.setOffsetLow(i, SAMPLE_WIDTH, offset );
						else
							eyeData.setHigh((int) (YnSample*32767));
//							eyeData.setOffsetHigh(i, SAMPLE_WIDTH, offset);

						//					System.err.print("End bp: " + bitPosition + " ");
						//					System.err.println(" (" + YnSample + " - " + YnMinus2Sample + ")* " + YnMinus1Sample + " = " + error);
					}
				}
				
				pskAudioData[i*bucketSize+s] = psk; 
				pskQAudioData[i*bucketSize+s] = fq; //fq*gain;	
//				pskQAudioData[i*bucketSize+s] = c.geti();	// this shows the waveform we mixed with
				eyeData.setData(i,s,eyeValue);
				
				bitPosition++;
				if (bitPosition == bucketSize)
					bitPosition = 0;
				
			}
		}

		offset = -bitPosition; 

		if (mode == PSK_MODE) {
			avgLockLevel = sumLockLevel / SAMPLE_WINDOW_LENGTH*bucketSize;
		}
		if (maxValue - minValue != 0)
			gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		//System.err.println(DESIRED_RANGE + " " + maxValue + " " + minValue + " " +gain);
		if (gain < 1) gain = 1;

		eyeData.clockOffset = offset; // rotate the data so that it matches the clock offset
	}

	public void incFreq () {
		freq = freq + 1d;
		//		nco.changePhase(10*alpha);
		//nco.setPhase(0, freq); 
	}
	public void incMiliFreq () {
		freq = freq + 0.01d;
		//			nco.changePhase(alpha);
		//nco.setPhase(alpha, freq); 
	}

	public void decFreq () {
		freq = freq - 1; 
		//		nco.changePhase(-10*alpha);
		//nco.setPhase(0, freq); 
	}
	public void decMiliFreq () {
		freq = freq - 0.01d;
		//		nco.changePhase(-1*alpha);
		//	nco.setPhase(-alpha, freq); 
	}

	public static double average (double avg, double new_sample, int N) {
		avg -= avg / N;
		avg += new_sample / N;
		return avg;
	}

	
	public double getError() { return error; }
	public double getFrequency() { return nco.getFrequency(); }
	public double getLockLevel() { return avgLockLevel; }

	Complex c = new Complex(0d, 0d); // to avoid allocation in audio loop
	double iFil;
	
	private double costasLoop(double i, double q, int bucketNumber, int sample) {
		nco.nextSample(c);
		c.normalize();
		// Mix 
		iFil = dataFilter.filterDouble(i);
		iMix = iFil * c.geti(); // + q*c.getq();
		qMix = iFil * -1*c.getq(); // - i*c.getq();
		// Filter
		fi = iFilter.filterDouble(iMix);
		fq = qFilter.filterDouble(qMix);
		
		int p = bucketSize*bucketNumber+sample;
		phasorData[2*p] = fi;
		phasorData[2*p+1] = fq;

		ri = SourceIQ.fullwaveRectify(fi);
		rq = SourceIQ.fullwaveRectify(fq);
		
		lockLevel = 1E1*(ri - rq);  // in lock state rq is close to zero;
		
		// Phase error
		error = (fi*fq);
		error = loopFilter.filterDouble(error);
		return fi;
	}
	
	
}


