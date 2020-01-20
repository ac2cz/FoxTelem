package decoder.FoxBPSK;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Performance;
import decoder.CodePRN;
import decoder.Decoder;
import decoder.SourceAudio;
import filter.AGCFilter;
import filter.Complex;
import filter.DcRemoval;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.TelemFormat;
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
public class FoxBPSKDotProdDecoder extends FoxBPSKDecoder {
	public static final int BITS_PER_SECOND_1200 = 1200;
	public static final int WORD_LENGTH = 10;
	public static final int AUDIO_MODE = 0;
	public static final int PSK_MODE = 1;
	public int mode = AUDIO_MODE;

	DcRemoval audioIDcFilter;
	DcRemoval audioQDcFilter;
	
	RootRaisedCosineFilter dataFilterI;
	RootRaisedCosineFilter dataFilterQ;

	double[] pskAudioData;
	double[] pskQAudioData;

	double gain = 1;	
	int chunk = 0;
	public int SEARCH_INTERVAL = 0; //set by window length. 16;  // 512 symbols gives 16, 128 gives 64, 40 gives 204, ie 256
	public static final int NSEARCHERS = 4;
	public static final double CENTER_CARRIER = 1500;         // Center of carrier frequency search range
	static final double CARRIER_SEARCH_RANGE = 800;    // Limits of search range above and below Carrier frequency, stay below 2400 which is twice the carrier to avoid false lock
	int Ftotal = (int) (2 * CARRIER_SEARCH_RANGE/100.0d + 1);
	int Fperslot = (Ftotal+NSEARCHERS-1)/NSEARCHERS;
	PskSearcher[] searchers = new PskSearcher[NSEARCHERS];
	Thread[] searcherThreads = new Thread[NSEARCHERS];
	
	double phase_inc_start, phase_inc_stop, phase_inc_step;
	DotProduct matchedFilter;
	
	//CosOscillator cos = new CosOscillator(currentSampleRate, (int)Carrier);
	//SinOscillator sin = new SinOscillator(currentSampleRate, (int)Carrier);
	ComplexOscillator nco = new ComplexOscillator(currentSampleRate, (int)CENTER_CARRIER);


	CosOscillator ftcos = new CosOscillator(currentSampleRate, (int)CENTER_CARRIER);
	SinOscillator ftsin = new SinOscillator(currentSampleRate, (int)CENTER_CARRIER);
	
//	double freq = 100.0;  //////////////// legacy value, REMOVE
	
	double carrier = CENTER_CARRIER;
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
//	double Gain = 128; // Heuristically, this seems about optimum
	public int samples_processed = 0;
	Complex c;
	double[] phasorData;

	public FoxBPSKDotProdDecoder(SourceAudio as, int chan, int mode, TelemFormat telemFormat) {
		super("1200bps BPSK", as, chan,  telemFormat);
		this.mode = mode;
		init();
	}

	@Override
	protected void init() {
		Log.println("Initializing 1200bps BPSK Non Coherent Dot Product decoder: ");
		BITS_PER_SECOND = BITS_PER_SECOND_1200;
		SAMPLE_WINDOW_LENGTH = 100; // 512 for KA9Q decoder on 2m, but we have 3-4x Doppler on 70cm  
		SEARCH_INTERVAL = (int) 1*8192/SAMPLE_WINDOW_LENGTH;
		bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit
		symphase = bucketSize/2;
		BUFFER_SIZE =  SAMPLE_WINDOW_LENGTH * bucketSize;
		initWindowData();

		audioIDcFilter = new DcRemoval(0.9999d);
		audioQDcFilter = new DcRemoval(0.9999d);
		
		// We don't filter the data as it comes in
		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
		filter.init(currentSampleRate, 0, 0);
		filter.setFilterDC(false);
		
//		filter = new RaisedCosineFilter(audioSource.audioFormat, (BUFFER_SIZE));
//		filter.init(currentSampleRate, 1500, 64);

		dataFilterI = new RootRaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		dataFilterI.init(currentSampleRate, 1200, 400); 
		dataFilterI.setAGC(false);
		dataFilterI.setFilterDC(false);
		dataFilterQ = new RootRaisedCosineFilter(audioSource.audioFormat, 1); // filter a single double
		dataFilterQ.init(currentSampleRate, 1200, 400); 
		dataFilterQ.setAGC(false);
		dataFilterQ.setFilterDC(false);
		
		pskAudioData = new double[BUFFER_SIZE];
		pskQAudioData = new double[BUFFER_SIZE];
		phasorData = new double[BUFFER_SIZE*2]; // actually it stores baseband i and q
		
		phase_inc_start = (CENTER_CARRIER - CARRIER_SEARCH_RANGE) * 2 * Math.PI / (double) currentSampleRate;
		phase_inc_stop = (CENTER_CARRIER + CARRIER_SEARCH_RANGE) * 2 * Math.PI / (double) currentSampleRate;
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
	
	public double[] getBasebandQData() {
		return pskQAudioData;
	}

	public double[] getPhasorData() {
		return phasorData;
	}

	protected void sampleBuckets() {
//		double maxValue = 0;
//		double minValue = 0;
//		double DESIRED_RANGE = 1;
		
		// dataValues is already bucketed, but we don't want that.
		// we use abBufferDoubleFiltered which has DC balance and some AGC but nothing else
		// abBufferDoubleFiltered is BUFFER_SIZE long
//		for (int j=0; j<BUFFER_SIZE; j+=2) {
//			if (abBufferDoubleFiltered[j] > maxValue) maxValue = abBufferDoubleFiltered[j];
//			if (abBufferDoubleFiltered[j] < minValue) minValue = abBufferDoubleFiltered[j];
//			abBufferDoubleFiltered[j] = audioIDcFilter.filter(abBufferDoubleFiltered[j]);
//			abBufferDoubleFiltered[j+1] = audioQDcFilter.filter(abBufferDoubleFiltered[j+1]);
//		}

		// Perform a brute force search periodically
		if (chunk % SEARCH_INTERVAL == 0) {
			int slot;
	        double maxenergy_value;
	        int slots;
	        int fleft;
	        int newSymphase = symphase;
	        double newCarrier = carrier;
	        
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
	        maxenergy_value = -99E99;
	        for(slot=0;slot < slots;slot++){
	        	try {
					searcherThreads[slot].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        	if(searchers[slot].getEnergy() >= maxenergy_value){
	        		maxenergy_value = searchers[slot].getEnergy();
// NOT USED	        		cphase_inc = searchers[slot].getCphaseInc();
	        		newSymphase = searchers[slot].getSymphase();
	        		newCarrier = searchers[slot].getFrequency();
	        	}
	        	searchers[slot].stop();
	        }
	        
	        // Dont adjust symphase or carrier unless carrier has changed
	        if (Math.abs(newCarrier - carrier) > 75) { // more than 75 hz diff, differnt slot, outside our tracker range
	        	carrier = newCarrier;
	        	symphase = newSymphase;
	        	if(symphase < bucketSize/2)
	        		symphase += bucketSize; // Allow room for early symbol timing test
	        	if (Config.debugClock)
	        		Log.println("   --full search: carrier "+carrier+" Hz; best offset "+symphase+"; energy "+maxenergy_value);
	        } else
	        	if (Config.debugClock)
	        		Log.println("   NO CHANGE: carrier "+carrier+" Hz; best offset "+symphase+"; energy "+maxenergy_value);
		}
		
		if (Double.isNaN(carrier))
			carrier = CENTER_CARRIER;
		if (carrier > CENTER_CARRIER+CARRIER_SEARCH_RANGE)
	    	carrier = CENTER_CARRIER+CARRIER_SEARCH_RANGE;
	    if (carrier < CENTER_CARRIER-CARRIER_SEARCH_RANGE)
	    	carrier = CENTER_CARRIER-CARRIER_SEARCH_RANGE;
	    
		// track frequency with two probes, one below and one above nominal carrier frequency
	    // Use linear interpolation to determine more accurate carrier frequency
	    double lower = carrier - 75;
	    double upper = carrier + 75;
	    double lower_phase = lower * 2 * Math.PI / (double) currentSampleRate;
	    double upper_phase = upper * 2 * Math.PI / (double) currentSampleRate;

      //  Log.println("   CARRIER "+carrier+" Hz Upper:"+upper + " Lower:"+lower);

	    double cpt0 = frequencyTracker(abBufferDoubleFiltered,symphase, lower_phase);
	    double cpt1 = frequencyTracker(abBufferDoubleFiltered,symphase, upper_phase);

	    // Notes from KA9A:
	    // Solve for zero crossing of cpt / carrier curve
	    // Note: this isn't exact. The cpt values aren't linear as they're actually functions of the sine of the frequency offset
	    // I should work up a more exact function, or at least bound the error
	    if(cpt1 != cpt0) // Avoid unlikely divide by zero
	      carrier = lower - cpt0 * (upper - lower) / (cpt1 - cpt0);
	    if (Double.isNaN(carrier))
			carrier = CENTER_CARRIER;
	    if (carrier > CENTER_CARRIER+CARRIER_SEARCH_RANGE)
	    	carrier = CENTER_CARRIER+CARRIER_SEARCH_RANGE;
	    if (carrier < CENTER_CARRIER-CARRIER_SEARCH_RANGE)
	    	carrier = CENTER_CARRIER-CARRIER_SEARCH_RANGE;
	    
//	    carrier = 1264; // ignore freqTracker

       // Log.println("   CARRIER "+carrier+" Hz cpt0:"+cpt0 + " cpt1:"+cpt1);

	    // Spin down to baseband with best estimate of carrier frequency
	    cphase_inc = carrier * 2 * Math.PI / (double) currentSampleRate;
		nco.setPhaseIncrement(cphase_inc);
		nco.setPhase(cphase);
		
		int eyeValue = 0;
	    for(int i=0; i < BUFFER_SIZE; i++){
	    	c = nco.nextSample();
			c.normalize();
			
			baseband_i[i] = abBufferDoubleFiltered[i] * c.geti();
			baseband_q[i] = abBufferDoubleFiltered[i] * -1*c.getq();
			
			baseband_i[i] = dataFilterI.filterDouble(baseband_i[i]);
			baseband_q[i] = dataFilterQ.filterDouble(baseband_q[i]);

			// This shows the input buffer.  Need to restrict to samples_processed at end of chunk to see actual
			double mag = Math.sqrt(baseband_i[i]*baseband_i[i] + baseband_q[i]*baseband_q[i]);
			//double phase = Math.atan2(baseband_i[i], baseband_q[i])/3;
			pskAudioData[i] = 1*(mag-0.7);
			eyeValue = (int)(pskAudioData[i]*32767.0); 
			eyeData.setData(i/bucketSize,i%bucketSize,eyeValue);
			phasorData[2*i] = baseband_i[i];
			phasorData[2*i+1] = baseband_q[i];
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
	    	//System.err.println("Best Index -1");
	    }
	    symphase = symphase + best_index + DEFAULT_INDEX; // default_index is -ve so we add it
	    symbol_count = demodState[best_index].symbol_count;
	    data = demodState[best_index].data;
	    Symbols_demodulated += symbol_count;
	    
	    if (chunk % 1 == 0) {
	   // 	System.err.print(/*"Def: " + DEFAULT_INDEX + */" Best Idx: " + best_index + " delta: " + (best_index + DEFAULT_INDEX));
	   // 	System.err.println(" symphase: " + symphase);
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
	    	int middleSamplePosition = (int) (symphase+bucketSize/2.0+(i)*bucketSize);
	    	boolean thisSample = false;
	    	if (data[i] > 0) thisSample = true;
			bitStream.addBit(thisSample);
			if (thisSample == false)
				eyeData.setLow((int) (pskAudioData[middleSamplePosition]*-32767.0)); // take the middle of the bit as the eye sample
			else
				eyeData.setHigh((int) (pskAudioData[middleSamplePosition]*32767.0));
	    }

		int offset = 0;//-1*(symphase)%bucketSize;
		eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

	    // Move carefully to next chunk, allowing for overlap of last/first symbol used for differential decoding and for timing skew
	    samples_processed = bucketSize * (symbol_count-1); // this ignores the first symbol which as not processed and puts the last symbol as our new first symbol
	    int move = bucketSize/2; //bucketSize/2;
	    if(symphase <= bucketSize/2){
	    	// Timing is moving early; move back 1/2 symbol
	    	//System.err.println("EARLY!!!! " + symphase + " " + samples_processed);
	    	samples_processed -= move;
	    	symphase += move;
	    } else if(symphase >= (3*bucketSize)/2){
	    	// Timing is moving late; move forward 1/2 sample
	    	//System.err.println("LATE!!!!" + symphase + " " + samples_processed);
	    	samples_processed += move;
	    	symphase -= move;
	    } else {
	    	;//System.err.println("Ontime:" + symphase + " " + samples_processed + " idx delta: " + (best_index + DEFAULT_INDEX));
	    }

	    // adjust the read pointer based on the number of symbols we actually processed
	    // In theory we read BUFFER_SIZE samples
	    int amount = BUFFER_SIZE - samples_processed;
	    rewind(amount);
	    cphase = (cphase + samples_processed * cphase_inc) % (2 * Math.PI); // for some reason this is worse..

//	    for (int i=0; i< samples_processed; i++) {
//	    	pskAudioData[i] = baseband_i[i];
//	    	eyeValue = (int)(-1*pskAudioData[i]*32767.0); 
//	    	eyeData.setData(i/bucketSize,i%bucketSize,eyeValue);
//	    }
	    
	    //Sampcounter += samples_processed; // not currently used

	    // At the end of chunk processing, increment counter
	    chunk++;

//		if (maxValue - minValue != 0)
//			gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		//System.err.println(DESIRED_RANGE + " " + maxValue + " " + minValue + " " +gain);
		//if (gain < 1) gain = 1;

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


	public static double average (double avg, double new_sample, int N) {
		avg -= avg / N;
		avg += new_sample / N;
		return avg;
	}




	public double getFrequency() { return nco.getFrequency(); }
//	public double getFrequency() { return cos.getFrequency(); }
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


