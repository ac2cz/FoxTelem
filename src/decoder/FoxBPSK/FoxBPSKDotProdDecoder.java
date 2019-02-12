package decoder.FoxBPSK;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

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
import filter.ComplexOscillator;
import filter.CosOscillator;
import filter.DotProduct;
import filter.RootRaisedCosineFilter;
import filter.SinOscillator;

/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Based on KA9Q PSK dot product decoder and a discussion with Phil Karn
 * at AMSAT Symposium 2018.
 * 
 * @author chris
 *
 */
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
		
//		filter = new RootRaisedCosineFilter(audioSource.audioFormat, (BUFFER_SIZE));
//		filter.init(currentSampleRate, 3000, 64);

		pskAudioData = new double[BUFFER_SIZE];
		
		phase_inc_start = (Carrier - Carrier_range) * 2 * Math.PI / (double) currentSampleRate;
		phase_inc_stop = (Carrier + Carrier_range) * 2 * Math.PI / (double) currentSampleRate;
		phase_inc_step = 2 * Math.PI * 100 / (double) currentSampleRate; // 100Hz - how much to increase phase for each searcher
		Fperslot = (Ftotal+NSEARCHERS-1)/NSEARCHERS;
		
		matchedFilter = new DotProduct();
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
	public static final int NSEARCHERS = 4;
	double Carrier = 1200;         // Center of carrier frequency search range
	double Carrier_range = 600;    // Limits of search range above and below Carrier frequency
	int Ftotal = (int) (2 * Carrier_range/100.0d + 1);
	int Fperslot = (Ftotal+NSEARCHERS-1)/NSEARCHERS;
	PskSearcher[] searchers = new PskSearcher[NSEARCHERS];
	Thread[] searcherThreads = new Thread[NSEARCHERS];
	
	double phase_inc_start, phase_inc_stop, phase_inc_step;
	DotProduct matchedFilter;
	
	CosOscillator cos = new CosOscillator(currentSampleRate, (int)Carrier);
	SinOscillator sin = new SinOscillator(currentSampleRate, (int)Carrier);

	CosOscillator ftcos = new CosOscillator(currentSampleRate, (int)Carrier);
	SinOscillator ftsin = new SinOscillator(currentSampleRate, (int)Carrier);
	
	double freq = 1200.0;  //////////////// legacy value, REMOVE
	
	double carrier = 0;
    double cphase_inc;
    double cphase = 0; // phase that we start the window when we downconvert with best carrier
    int symphase = 0;
	double[] baseband_i = new double[BUFFER_SIZE];
	double[] baseband_q = new double[BUFFER_SIZE];
	static final int NUM_OF_DEMODS = 5;
	PskDemodState[] demodState = new PskDemodState[NUM_OF_DEMODS];
	int symbol_count = 0;
	double[] data; // the demodulated symbols
	int Symbols_demodulated; // total symbols demodulated
	double Gain = 128; // Heuristically, this seems about optimum
	public int samples_processed = 0;

	protected void sampleBuckets() {
		double maxValue = 0;
		double minValue = 0;
		double DESIRED_RANGE = 2;
		sumClockError = 0;
		
		// dataValues is already bucketed, but we don't want that.
		// we use abBufferDoubleFiltered which has DC balance and some AGC but nothing else
		// abBufferDoubleFiltered is BUFFER_SIZE long

		// Perform a brute force search periodically
		if (chunk % SEARCH_INTERVAL == 0) {
			int slot;
	        double maxenergy_value;
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
	        if(symphase < bucketSize/2)
	        	symphase += bucketSize; // Allow room for early symbol timing test
	        //Log.println("   --full search: carrier "+carrier+" Hz; best offset "+symphase+"; energy "+maxenergy_value);
	    }
	    
		// track frequency with two probes, one below and one above nominal carrier frequency
	    // Use linear interpolation to determine more accurate carrier frequency
	    double lower = carrier - 75;
	    double upper = carrier + 75;
	    double lower_phase = lower * 2 * Math.PI / (double) currentSampleRate;
	    double upper_phase = upper * 2 * Math.PI / (double) currentSampleRate;

        //Log.println("   CARRIER "+carrier+" Hz Upper:"+upper + " Lower:"+lower);

	    double cpt0 = frequencyTracker(abBufferDoubleFiltered,symphase, lower_phase);
	    double cpt1 = frequencyTracker(abBufferDoubleFiltered,symphase, upper_phase);

	    // Notes from KA9A:
	    // Solve for zero crossing of cpt / carrier curve
	    // Note: this isn't exact. The cpt values aren't linear as they're actually functions of the sine of the frequency offset
	    // I should work up a more exact function, or at least bound the error
	    if(cpt1 != cpt0) // Avoid unlikely divide by zero
	      carrier = lower - cpt0 * (upper - lower) / (cpt1 - cpt0);

        //Log.println("   CARRIER "+carrier+" Hz cpt0:"+cpt0 + " cpt1:"+cpt1);

	    // Spin down to baseband with best estimate of carrier frequency
	    cphase_inc = carrier * 2 * Math.PI / (double) currentSampleRate;
		cos.setPhaseIncrement(cphase_inc);
		sin.setPhaseIncrement(cphase_inc);
		cos.setPhase(cphase);
		sin.setPhase(cphase);
		int eyeValue = 0;
//		double[] nextPhase = new double[BUFFER_SIZE]; // store the phase so we can wind the buffer back and continue from that point
	    for(int i=0; i < BUFFER_SIZE; i++){
//			nextPhase[i] = cos.getPhase();  // we store for N meaning it is the phase ready to process that number
			baseband_i[i] = abBufferDoubleFiltered[i] * cos.nextSample();
			baseband_q[i] = abBufferDoubleFiltered[i] * sin.nextSample();
			pskAudioData[i] = baseband_i[i] + baseband_q[i];
			eyeValue = (int)(-1*pskAudioData[i]*32767.0); 
			eyeData.setData(i/bucketSize,i%bucketSize,eyeValue);
	    }

	    // Demodulate N times: early, on time, and late
	    int DEFAULT_INDEX = (int)(NUM_OF_DEMODS-1)/-2;
	    int best_index = -1;
	    double energy = -9E99;
	    for(int i=0;i<NUM_OF_DEMODS;i++){
	    	demodState[i] = demodulate(baseband_i, baseband_q, symphase + i + DEFAULT_INDEX); // default_index is -ve so we add it
	    	if(energy < demodState[i].energy){
	    		best_index = i;
	    		energy = demodState[i].energy;
	    	}
	    }
	    
	    if(best_index == -1) {
	    	best_index = (int)(NUM_OF_DEMODS-1)/2; // we did not pick any state, so pick middle
	    	System.err.println("Best Index -1");
	    }
	    symphase = symphase + best_index + DEFAULT_INDEX; // default_index is -ve so we add it
	    symbol_count = demodState[best_index].symbol_count;
	    data = demodState[best_index].data;
	    Symbols_demodulated += symbol_count;
	    
	    if (chunk % 10 == 0) {
	    System.err.print(/*"Def: " + DEFAULT_INDEX + */" Best Idx: " + best_index + " delta: " + (best_index + DEFAULT_INDEX));
	    System.err.println(" symphase: " + symphase);
	    }
	    //Log.println("Symbols decoded:" + symbol_count);
	    // Scale demodulated data and pass to FEC
//	    double rms = Math.sqrt(energy / symbol_count);
//	    gain = Gain / rms; // Tune this
	    // Skip data[0], which is actually the last symbol from the previous chunk
	    // It was needed by the demodulator as the reference for differentially decoding the second symbol, i.e., data[1]
	    for(int i=1;i<symbol_count;i++){
//	    	double y;
//	    	y = 127.5 + gain * data[i];
//	    	y = (y > 255) ? 255 : ((y < 0) ? 0 : y);
	    	//symbolq((int)y+" "); // Pass to FEC decoder thread
	    	
	    	boolean thisSample = false;
	    	if (data[i] > 0) thisSample = true;
			bitStream.addBit(thisSample);
			if (thisSample == false)
				eyeData.setLow((int) (data[i]*32767));
//				eyeData.setOffsetLow(i, SAMPLE_WIDTH, offset );
			else
				eyeData.setHigh((int) (data[i]*32767));
			
	    }
	    
	    // Move carefully to next chunk, allowing for overlap of last/first symbol used for differential decoding and for timing skew
	    samples_processed = bucketSize * (symbol_count-1);
	    int move = bucketSize/2; //bucketSize/2;
	    if(symphase <= bucketSize/2){
	    	// Timing is moving early; move back 1/2 symbol
	    	System.err.println("EARLY!!!!");
	    	samples_processed -= move;
	    	symphase += move;
	    } else if(symphase >= (3*bucketSize)/2){
	    	// Timing is moving late; move forward 1/2 sample
	    	System.err.println("LATE!!!!");
	    	samples_processed += move;
	    	symphase -= move;
	    }

	    // adjust the read pointer based on the number of symbols we actually processed
	    int amount = BUFFER_SIZE - samples_processed;
	    rewind(amount);
//	    cphase = nextPhase[samples_processed-1];
	    cphase = samples_processed * cphase_inc;
	    
	    //Sampcounter += samples_processed; // not currently used

	    // At the end of chunk processing, increment counter
	    chunk++;

//		
		if (maxValue - minValue != 0)
			gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		//System.err.println(DESIRED_RANGE + " " + maxValue + " " + minValue + " " +gain);
		if (gain < 1) gain = 1;

		eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

	}
	
	/**
	 * Demodulate chunk, determining frequency error
	 * @param samples - samples to be processed
	 * @param symphase - Offset to first sample of first symbol; determined by symbol timing (input)
	 * @param cphase_inc - Carrier phase increment per sample (2^32 = 2 pi radians) (input)
	 * @return
	 */
	private double frequencyTracker(double[] samples, int symphase, double cphase_inc) {
		double[] baseband_i = new double[BUFFER_SIZE];
		double[] baseband_q = new double[BUFFER_SIZE];
		int bb_p,i;
		double tlast_i,tlast_q;
		double cpt;
		int Ntaps = matchedFilter.getNumOfTaps();

		ftcos.setPhaseIncrement(cphase_inc);
		ftsin.setPhaseIncrement(cphase_inc);
		// reset each time to do the search
		ftcos.setPhase(0);
		ftsin.setPhase(0);

		// Downconvert chunk of samples to baseband 
		for(i=0; i < BUFFER_SIZE; i++){
			baseband_i[i] = samples[i] * ftcos.nextSample();
			baseband_q[i] = samples[i] * ftsin.nextSample();
		}
		// Perform demodulation with specified symbol timing, computing sum of cross products
		cpt = 0;
		tlast_i = tlast_q = 0;
		for(bb_p = symphase; bb_p + Ntaps < BUFFER_SIZE; bb_p += bucketSize){ // For every symbol in baseband buffer
			double fi,fq;
			double symbol;

			// Demodulate through matched filter, compute baseband energy
			fi = matchedFilter.dotprod(baseband_i, bb_p);
			fq = matchedFilter.dotprod(baseband_q, bb_p);

			// Dot product of previous and current center complex samples gives differentially demodulated symbol
			symbol = fi * tlast_i + fq * tlast_q;
			// When there hasn't been a transition, a positive cross product means the signal frequency is positive, i.e.,
			// our estimate of carrier frequency is too low. A transition reverses the sense of the cross product.
			if(symbol < 0)
				cpt -= fi * tlast_q - fq * tlast_i;
			else
				cpt += fi * tlast_q - fq * tlast_i;

			tlast_i = fi;
			tlast_q = fq;
		}
		return cpt;
	}
	
	/**
	 * 
	 * @param baseband_i // Baseband I-channel samples (input)
	 * @param baseband_q 
	 * @param symphase // Offset to first sample of first symbol; determined by symbol timing (input)
	 * @return PskDemodState
	 */
	private PskDemodState demodulate(double[] baseband_i, double[] baseband_q, int symphase) {

		int bb_p, symbol_count = 0;
		double tlast_i = 0, tlast_q = 0;
		double en_tmp;
		int Ntaps = matchedFilter.getNumOfTaps();
		double[] data = new double[SAMPLE_WINDOW_LENGTH];
		
		en_tmp = 0;
		for(bb_p = symphase; bb_p + Ntaps < BUFFER_SIZE; bb_p += bucketSize){ // For every symbol in baseband buffer
			double fi, fq;
			double symbol;

			// Sample matched filter
			fi = matchedFilter.dotprod(baseband_i, bb_p);
			fq = matchedFilter.dotprod(baseband_q, bb_p);

			// Dot product of previous and current complex samples gives differentially demodulated symbol
			// data[0] will always be 0
			symbol = fi * tlast_i + fq * tlast_q;
			en_tmp += (double)symbol * symbol;

			assert(symbol_count < SAMPLE_WINDOW_LENGTH);
			data[symbol_count++] = symbol;
			tlast_i = fi;
			tlast_q = fq;
		}
		PskDemodState demodState = new PskDemodState(symphase, en_tmp, data, symbol_count);
		return demodState;
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
		ArrayList<Frame> frames = bitStream.findFrames(SAMPLE_WINDOW_LENGTH);
		Performance.endTimer("findSync");
		if (frames != null) {
			processPossibleFrame(frames);
		}
	}
	private Frame decodedFrame = null;
	/**
	 *  Decode the frame
	 */
	protected void processPossibleFrame(ArrayList<Frame> frames) {

		Spacecraft sat = null;
		for (Frame decodedFrame : frames) {
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
	}


	public double getFrequency() { return cos.getFrequency(); }
	public int getOffset() { return symphase; }

	class PskDemodState {
		int symphase;                // Offset to first sample of first symbol; determined by symbol timing (input)
		double energy;               // Total demodulator output energy (output)
		double[] data;  	// Demodulated symbols (output)
		int symbol_count;
		
		PskDemodState(int symphase, double energy, double[] data, int symbol_count) {
			this.symphase = symphase;
			this.energy = energy;
			this.data = data;
			this.symbol_count = symbol_count;
		}
	}
	
}


