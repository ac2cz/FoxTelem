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
import filter.RaisedCosineFilter;
import filter.WindowedSincFilter;

public class FoxBPSKDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	public static final int WORD_LENGTH = 10;
	private int clockOffset = 0;
	DcRemoval audioDcFilter;
	
	RaisedCosineFilter dataFilter;
	RaisedCosineFilter iFilter;
	RaisedCosineFilter qFilter;
	RaisedCosineFilter loopFilter;

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

		BUFFER_SIZE =  SAMPLE_WINDOW_LENGTH * bucketSize;
		SAMPLE_WIDTH = 4;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = bucketSize/2;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		initWindowData();

		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
		audioDcFilter = new DcRemoval(0.9999d);
		filter.init(currentSampleRate, 0, 0);
		
		dataFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		dataFilter.init(48000, 1200, 512);
		
		iFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		iFilter.init(48000, 1200, 32);

		qFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		qFilter.init(48000, 1200, 32);
		
		loopFilter = new RaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		loopFilter.init(48000, 10, 32); // this filter should not contribute to lock.  Should be far outside the closed loop response.
		
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
	
	double alpha = 0.001; //the feedback coeff  0 - 4.  But typical range is 0.01 and smaller.  In fact seems unstable above 0.01
	double beta = 10*alpha*alpha / 4.0d;  // alpha * alpha / 4 is critically damped. 
	double error, errTotal;  // accumulation of the error over a buffer
	double loopError;
	boolean lastBit = false;
	double freq = 1242.0d;
	
	protected void sampleBuckets() {
		long maxValue = 0;
		long minValue = 0;
		long DESIRED_RANGE =60000;
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			double averageValue = 0;
			for (int s=0; s < bucketSize; s++) {
				//sampleWithVCO(dataValues[i][s], i, s);
				double value = dataValues[i][s]/ 32768.0;
//				value = testOscillator.nextSample();
//				value = audioDcFilter.filter(value);		
//				RxDownSample(value, value, i);
			//	double psk = squareLoop(value, value, i);
				double psk = costasLoop(value, value, i);
				int eyeValue = (int)(psk*32768.0); //(Math.sqrt(energy1)-32768.0/2);

//				int eyeValue = (int) (Math.sqrt(energy1)-32768.0/2);
				if (eyeValue > maxValue) maxValue = eyeValue;
				if (eyeValue < minValue) minValue = eyeValue;
				eyeValue = (int) (eyeValue * gain); // gain from the last SAMPLE_WINDOW
				pskAudioData[i*bucketSize+s] = psk*gain;	
				pskQAudioData[i*bucketSize+s] = fq*gain;	
				eyeData.setData(i,s,eyeValue);
				averageValue += eyeValue/32768.0;
				
				loopError = errTotal; // /(double)(bucketSize);
				
				freq = freq + beta * loopError;
				if (freq > 2300) freq = 2300;
				if (freq < -2300) freq = -2300;
//				nco.setFrequency(freq);
				nco.setPhase(alpha*loopError, freq);
				errTotal = 0;
			}
			int offset = recoverClockOffset();
			offset = 0;
			
			averageValue = averageValue / (double)bucketSize;
			boolean thisBit = false;
			if (averageValue > 0) {
				thisBit = true;
			}
			if (thisBit == lastBit)
				middleSample[i] = true;
			else // phase change so a zero
				middleSample[i] = false;
			lastBit = thisBit;
			bitStream.addBit(middleSample[i]);
			
			if (middleSample[i] == false)
				eyeData.setOffsetLow(i, SAMPLE_WIDTH, offset );
			else
				eyeData.setOffsetHigh(i, SAMPLE_WIDTH, offset);
				
			
		}
		
		
		
		gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
	//	System.out.println(DESIRED_RANGE + " " + maxValue + " " + minValue + " " +gain);
		
		int offset = recoverClockOffset();
//		eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

		//	Scanner scanner = new Scanner(System.in);
		//		System.out.println("Press enter");
		//	String username = scanner.next();
		
	}

	public void incFreq () {
		freq = freq + 0.1;
		nco.setPhase(0, freq); }
	public void incMiliFreq () {
		//	freq = freq + beta;
		nco.setPhase(alpha, freq); 
	}

	public void decFreq () {
		freq = freq - 0.1; 
		nco.setPhase(0, freq); }
	public void decMiliFreq () {
		//	freq = freq - beta;
		nco.setPhase(-alpha, freq); 
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

		return clockOffset;
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
	ComplexOscillator nco = new ComplexOscillator(48000, 1200);
	double iMix, qMix;
	
	double fi = 0.0, fq = 0.0;
	
	public double getError() { return loopError; }
	public double getFrequency() { return nco.getFrequency(); }
	
	private double costasLoop(double i, double q, int bucketNumber) {
		Complex c = nco.nextSample();
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
		errTotal += loopFilter.filterDouble(error);
		return fi;
	}
	
	private double squareLoop(double i, double q, int bucketNumber) {
		Complex c = nco.nextSample();
		c.normalize();
		// Mix 
		fi = i*i;
		// then need to divide freq by 2 to get the carrier

		return fi;
	}

}


