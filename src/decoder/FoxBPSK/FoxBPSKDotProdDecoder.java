package decoder.FoxBPSK;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import common.Config;
import common.Log;
import common.Performance;
import common.Spacecraft;
import decoder.CodePRN;
import decoder.Decoder;
import decoder.PskSearcher;
import decoder.SourceAudio;
import decoder.SourceIQ;
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
import filter.RootRaisedCosineFilter;
import filter.WindowedSincFilter;

public class FoxBPSKDotProdDecoder extends Decoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	public static final int WORD_LENGTH = 10;
	private double sumClockError = 0;
	private int samplePoint = 20;
	public static final int AUDIO_MODE = 0;
	public static final int PSK_MODE = 1;
	public int mode = AUDIO_MODE;

	DcRemoval audioDcFilter;

	ComplexOscillator nco = new ComplexOscillator(currentSampleRate, 1200);

	double[] pskAudioData;

	//CosOscillator testOscillator = new CosOscillator(48000,1200);

	/**
	 * This holds the stream of bits that we have not decoded. Once we have several
	 * SYNC words, this is flushed of processed bits.
	 */
	protected FoxBPSKBitStream bitStream = null;  // Hold bits until we turn them into decoded frames

	public FoxBPSKDotProdDecoder(SourceAudio as, int chan, int mode) {
		super("1200bps BPSK", as, chan);
		this.mode = mode;
		init();
	}

	@Override
	protected void init() {
		Log.println("Initializing 1200bps BPSK Non Coherent Dot Product decoder: ");
		bitStream = new FoxBPSKBitStream(this, WORD_LENGTH, CodePRN.getSyncWordLength());
		BITS_PER_SECOND = BITS_PER_SECOND_1200;
		SAMPLE_WINDOW_LENGTH = 128; // 512 for KA9Q decoder on 2m, but we have 3-4x Doppler on 70cm  
		bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit
		samplePoint = bucketSize/2;
		BUFFER_SIZE =  SAMPLE_WINDOW_LENGTH * bucketSize;
		SAMPLE_WIDTH = 1;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = bucketSize/2;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
		initWindowData();

		audioDcFilter = new DcRemoval(0.9999d);
		
		// We don't filter the data as it comes in
		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
		filter.init(currentSampleRate, 0, 0);		

		pskAudioData = new double[BUFFER_SIZE];
		
		phase_inc_start = (Carrier - Carrier_range) * 2 * Math.PI / (double) currentSampleRate;
		phase_inc_stop = (Carrier + Carrier_range) * 2 * Math.PI / (double) currentSampleRate;
		phase_inc_step = 2 * Math.PI * 100 / (double) currentSampleRate; // 100Hz - how much to increase phase for each searcher
		Fperslot = (Ftotal+NSEARCHERS-1)/NSEARCHERS;
	}

	protected void resetWindowData() {
		super.resetWindowData();

	}

	public double[] getBasebandData() {
		return pskAudioData;
	}

	double gain = 1.0;
	
//	double iMix, qMix;
//	double fi = 0.0, fq = 0.0;

	// Keep track of where we are in the bit for sampling - Gardner algorithm
	int bitPosition = 0;
	int offset = 0;
	double YnMinus2Sample = 0;
	double YnSample = 0;
	double YnMinus1Sample;
	boolean delayClock = false;

	int chunk = 0;
	public static final int SEARCH_INTERVAL = 16;
	public static final int NSEARCHERS = 1;
	double Carrier = 1200;         // Center of carrier frequency search range
	double Carrier_range = 600;    // Limits of search range above and below Carrier frequency
	int Ftotal = (int) (2 * Carrier_range/100.0d + 1);
	int Fperslot = (Ftotal+NSEARCHERS-1)/NSEARCHERS;
	PskSearcher[] searchers = new PskSearcher[NSEARCHERS];
	Thread[] searcherThreads = new Thread[NSEARCHERS];
	
	double phase_inc_start, phase_inc_stop, phase_inc_step;
	
	double freq = 1200.0;  //////////////// legacy value, REMOVE
	
	protected void sampleBuckets() {
		double maxValue = 0;
		double minValue = 0;
		double DESIRED_RANGE = 2;
		sumClockError = 0;
		
		// dataValues is already bucketed, but we don't want that.
		// we use abBufferDoubleFiltered which has DC balance but nothing else
		// abBufferDoubleFiltered is BUFFER_SIZE long
		
		// Perform a brute force search periodically
		if (chunk % SEARCH_INTERVAL == 0) {
			int slot;
	        double cphase_inc, maxenergy_value, symphase = 0;
	        int slots;
	        int fleft;
	        
	        cphase_inc = phase_inc_start;
	        fleft = Ftotal;
	        for (slot=0; fleft > 0 && slot < NSEARCHERS; slot++) {
	        	searchers[slot] = new PskSearcher(abBufferDoubleFiltered, cphase_inc, phase_inc_step, 
	        			min(Fperslot,fleft), BUFFER_SIZE, bucketSize, currentSampleRate);
	        	
	        	cphase_inc += phase_inc_step * searchers[slot].getNfreq();
	        	fleft -= searchers[slot].getNfreq();
	        	searcherThreads[slot] = new Thread(searchers[slot]); 
	        	searcherThreads[slot].setName("Searcher Thread:" + slot);
	        	searcherThreads[slot].setUncaughtExceptionHandler(Log.uncaughtExHandler);
	        	searcherThreads[slot].start();
	        	searchers[slot].run();
	        }
	        
	        // Find the winner
	        double carrier = 0;
	        slots = slot;
	        maxenergy_value = 0;
	        for(slot=0;slot < slots;slot++){
	        	try {
					searcherThreads[slot].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        	if(searchers[slot].getEnergy() >= maxenergy_value){
	        		maxenergy_value = searchers[slot].getEnergy();
	        		cphase_inc = searchers[slot].getCphaseInc();
	        		symphase = searchers[slot].getSymphase();
	        		carrier = searchers[slot].getFrequency();
	        	}
	        	searchers[slot].stop();
	        }
	        ////////////// Need to think about this.  He has phase in terms of the actual samples in the 
	        ////////////// symbol I think.  Which is clever ...
//	        if(symphase < SAMPPSYM/2)
//	        	symphase += SAMPPSYM; // Allow room for early symbol timing test
	        Log.println("   --full search: carrier "+carrier+" Hz; best offset "+symphase+"; energy "+maxenergy_value+"\n");
	    }
	    
		// At the end of chunk processing, increment counter
		chunk++;
		
//		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
//			for (int s=0; s < bucketSize; s++) {
//				double value = dataValues[i][s]/ 32768.0;
//				
//				if (value > maxValue) maxValue = value;
//				if (value < minValue) minValue = value;
//
//				value = value*gain;
//				
//				psk = demodulate(value, i);
//				int eyeValue = (int)(-1*psk*32768.0); 
//
//				// Here we would adjust the freq to track
//				//		nco.setFrequency(freq);
//				
//				
//				if (bitPosition == 0 ) {
//					YnMinus1Sample = psk;
//				}
//
//				if (bitPosition == samplePoint  ) {
//					if (delayClock) // we already sampled this
//						delayClock = false;
//					else {
//						// ALL END OF BIT LOGIC TO DO WITH BIT CHOICE MUST GO HERE TO BE IN SYNC
//
//	//					System.err.print("Bit: " + bitPosition + " Sample: " +((Config.windowsProcessed-1)*SAMPLE_WINDOW_LENGTH+i) + " " + psk + " >>");
//						YnMinus2Sample = YnSample;
//						YnSample = psk;
//
//						boolean thisBit;
//						////  Determine the dot product here and the bit value
//						////bitStream.addBit(thisSample);
//
//						if (Config.debugValues)
//							psk = psk*1.5;
//
//						//HERE WE DO THE GARDNER ALGORITHM AND UPDATE THE SAMPLE POINTS WITH WRAP.
//						// Gardner Error calculation
//						// error = (Yn -Yn-2)*Yn-1
//						double clockError = (YnSample - YnMinus2Sample) * YnMinus1Sample;
//						sumClockError += clockError;
//						double errThreshold = 0.1;
//						if (clockError < -1*errThreshold) {
//							delayClock = true;
//							bitPosition = bitPosition - 1;
//						} else if (clockError > errThreshold) // sample is late because error is positive, so sample earlier by increasing bitPosition
//							bitPosition = bitPosition + 1;
//
//						if (/*thisBit == */false)
//							eyeData.setLow((int) (YnSample*32768));
//						else
//							eyeData.setHigh((int) (YnSample*32768));
//
//						//					System.err.print("End bp: " + bitPosition + " ");
//						//					System.err.println(" (" + YnSample + " - " + YnMinus2Sample + ")* " + YnMinus1Sample + " = " + error);
//					}
//				}
//				
//				pskAudioData[i*bucketSize+s] = psk; 
////				pskQAudioData[i*bucketSize+s] = c.geti();	// this shows the waveform we mixed with
//				eyeData.setData(i,s,eyeValue);
//				
//				bitPosition++;
//				if (bitPosition == bucketSize)
//					bitPosition = 0;
//				
//			}
//		}
//
//		offset = -bitPosition; 

		if (maxValue - minValue != 0)
			gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		//System.err.println(DESIRED_RANGE + " " + maxValue + " " + minValue + " " +gain);
		if (gain < 1) gain = 1;

		eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

	}

	private int min(int a, int b) {
		if (a < b)
			return a;
		else
			return b;
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
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
				    public void run() { MainWindow.setTotalDecodes();}
				});
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Performance.endTimer("Store");
		} else {
			if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
			//clockLocked = false;
		}
	}


	public double getFrequency() { return nco.getFrequency(); }
	
	
	
}


