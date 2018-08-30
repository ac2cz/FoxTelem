package decoder.FoxBPSK;

import java.io.IOException;
import common.Config;
import common.Log;
import common.Performance;
import common.Spacecraft;
import decoder.CodePRN;
import decoder.Decoder;
import decoder.SourceAudio;
import filter.AGCFilter;
import filter.DcRemoval;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.FoxBPSK.FoxBPSKFrame;
import telemetry.FoxBPSK.FoxBPSKHeader;
import filter.Complex;
import filter.ComplexOscillator;
import filter.CosOscillator;
import filter.Delay;
import filter.HilbertTransform;
import filter.IirFilter;
import filter.RaisedCosineFilter;
import filter.WindowedSincFilter;

public class FoxBPSKDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	public static final int WORD_LENGTH = 10;
	private double sumClockError = 0;
	private int samplePoint = 20;
	
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

	//CosOscillator testOscillator = new CosOscillator(48000,1200);

	/**
	 * This holds the stream of bits that we have not decoded. Once we have several
	 * SYNC words, this is flushed of processed bits.
	 */
	protected FoxBPSKBitStream bitStream = null;  // Hold bits until we turn them into decoded frames

	public FoxBPSKDecoder(SourceAudio as, int chan) {
		super("1200bps BPSK", as, chan);
		init();
	}

	@Override
	protected void init() {
		Log.println("Initializing 1200bps BPSK decoder: ");
		bitStream = new FoxBPSKBitStream(this, WORD_LENGTH, CodePRN.getSyncWordLength());
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

		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
		audioDcFilter = new DcRemoval(0.9999d);
		filter.init(currentSampleRate, 0, 0);

		dataFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		dataFilter.init(currentSampleRate, 3000, 512); // just remove noise, perhaps at twice data rate? Better wider and steeper to give selectivity

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

	/**
	 * 
	 * 
	 */
	double gain = 1.0;

	double alpha = 0.1; //the feedback coeff  0 - 4.  But typical range is 0.01 and smaller.  
	double beta = alpha*alpha / 4.0d;  // alpha * alpha / 4 is critically damped. 
	double error;  // accumulation of the error over a buffer
	double loopError;
	boolean lastPhase = false;
	double freq = 1200.0d;

	// Kep track of where we are in the bit for sampling
//	int bitNumber = 0; // for eye diagram only
	int bitPosition = 0;
	int offset = 0;
	double YnMinus2Sample = 0;
	double YnSample = 0;
	double YnMinus1Sample;
	boolean delayClock = false;
	
	protected void sampleBuckets() {
		long maxValue = 0;
		long minValue = 0;
		long DESIRED_RANGE =60000;
		sumClockError = 0;
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			for (int s=0; s < bucketSize; s++) {
				double value = dataValues[i][s]/ 32768.0;
				value = value*gain;
				double psk = costasLoop(value, value, i);
				//psk = audioDcFilter.filter(psk);		
				int eyeValue = (int)(-1*psk*32768.0); 
				if (eyeValue > maxValue) maxValue = eyeValue;
				if (eyeValue < minValue) minValue = eyeValue;

				loopError = beta*error + alpha*error; //loopFilter.filterDouble(error); // /(double)(bucketSize);

				//				freq = freq + beta*loopError + alpha*loopError;
				if (freq > 2300) freq = 2300;
				if (freq < -2300) freq = -2300;
				nco.changePhase(alpha*error);
				freq = freq + beta*error;
				nco.setFrequency(freq);

				if (bitPosition == 0 ) {
					YnMinus1Sample = psk;
				}

				if (bitPosition == samplePoint  ) {
					if (delayClock) // we already sampled this
						delayClock = false;
					else {
						// ALL END OF BIT LOGIC TO DO WITH BIT CHOICE MUST GO HERE TO BE IN SYNC

						//					System.err.print("Bit: " + bitPosition + " Sample: " +((Config.windowsProcessed-1)*SAMPLE_WINDOW_LENGTH+i) + " " + psk + " >>");
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

						if (Config.debugValues)
							psk = psk*1.5;
						//eyeValue = (int) (psk*-32768);
						//HERE WE DO THE GARDNER ALGORITHM AND UPDATE THE SAMPLE POINTS WITH WRAP.
						// Gardner Error calculation
						// error = (Yn -Yn-2)*Yn-1
						double clockError = (YnSample - YnMinus2Sample) * YnMinus1Sample;
						sumClockError += clockError;
						// uncomment if clock calc per bit
						double errThreshold = 0.1;
						if (clockError < -1*errThreshold) {
							delayClock = true;
							bitPosition = bitPosition - 1;
						} else if (clockError > errThreshold) // sample is late because error is positive, so sample earlier by increasing bitPosition
							bitPosition = bitPosition + 1;

						if (thisPhase == false)
							eyeData.setLow((int) (YnSample*32768));
//							eyeData.setOffsetLow(i, SAMPLE_WIDTH, offset );
						else
							eyeData.setHigh((int) (YnSample*32768));
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


	//		offset = -bitPosition; // uncomment if clock calc per bit
			

		}
		// uncomment for clock offset per window
//		System.err.println(sumClockError);
//		if(sumClockError > 0) 
//			bitPosition = bitPosition + 1;
//		else
//			bitPosition = bitPosition - 1;
//		if (bitPosition >= bucketSize)
//			bitPosition = 0;
//		if (bitPosition < 0)
//			bitPosition = bucketSize-1;
//		
		offset = -bitPosition; 

		gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		//		System.out.println(DESIRED_RANGE + " " + maxValue + " " + minValue + " " +gain);

		eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

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

	/**
	 * Determine if the bit sampling buckets are aligned with the data. This is calculated when the
	 * buckets are sampled
	 * 
	 */
	@Override
	public int recoverClockOffset() {

		return 0;//clockOffset;
	}

	protected double[] recoverClock(int factor) {

		return null;
	}

	@Override
	protected void processBitsWindow() {
		Performance.startTimer("findSync");
		boolean found = bitStream.findSyncMarkers(SAMPLE_WINDOW_LENGTH);
		Performance.endTimer("findSync");
		if (found) {
			processPossibleFrame();
		}
	}

	private Frame decodedFrame = null;
	/**
	 *  Decode the frame
	 */
	protected void processPossibleFrame() {

		Spacecraft sat = null;
		//Performance.startTimer("findFrames");
		decodedFrame = bitStream.findFrames();
		//Performance.endTimer("findFrames");
		if (decodedFrame != null && !decodedFrame.corrupt) {
			Performance.startTimer("Store");
			// Successful frame
			eyeData.lastErasureCount = bitStream.lastErasureNumber;
			eyeData.lastErrorsCount = bitStream.lastErrorsNumber;
			//eyeData.setBER(((bitStream.lastErrorsNumber + bitStream.lastErasureNumber) * 10.0d) / (double)bitStream.SYNC_WORD_DISTANCE);
			if (Config.storePayloads) {

				FoxBPSKFrame hsf = (FoxBPSKFrame)decodedFrame;
				FoxBPSKHeader header = hsf.getHeader();
				sat = Config.satManager.getSpacecraft(header.id);
				hsf.savePayloads(Config.payloadStore);;

				// Capture measurements once per payload or every 5 seconds ish
				addMeasurements(header, decodedFrame, bitStream.lastErrorsNumber, bitStream.lastErasureNumber);
				if (Config.autoDecodeSpeed)
					MainWindow.inputTab.setViewDecoder2();


			}
			Config.totalFrames++;
			if (Config.uploadToServer)
				try {
					Config.rawFrameQueue.add(decodedFrame);
				} catch (IOException e) {
					// Don't pop up a dialog here or the user will get one for every frame decoded.
					// Write to the log only
					e.printStackTrace(Log.getWriter());
				}
			if (sat != null && sat.sendToLocalServer())
				try {
					Config.rawPayloadQueue.add(decodedFrame);
				} catch (IOException e) {
					// Don't pop up a dialog here or the user will get one for every frame decoded.
					// Write to the log only
					e.printStackTrace(Log.getWriter());
				}
			framesDecoded++;
			Performance.endTimer("Store");
		} else {
			if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
			//clockLocked = false;
		}
	}


	//	HilbertTransform ht = new HilbertTransform(9600, 255);
	//	Delay delay = new Delay((255-1)/2);

	double iMix, qMix;

	double fi = 0.0, fq = 0.0;

	public double getError() { return loopError; }
	public double getFrequency() { return nco.getFrequency(); }
	Complex c;
	private double costasLoop(double i, double q, int bucketNumber) {
		c = nco.nextSample();
		c.normalize();
		// Mix 
		double iFil = dataFilter.filterDouble(i);
		iMix = iFil * c.geti(); // + q*c.getq();
		qMix = iFil * -1*c.getq(); // - i*c.getq();
		// Filter
		fi = iFilter.filterDouble(iMix);
		fq = qFilter.filterDouble(qMix);

		// Phase error
		error = (fi*fq);
		error = loopFilter.filterDouble(error);
		return fi;
	}

}


