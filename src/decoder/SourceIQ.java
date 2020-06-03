package decoder;

import javax.sound.sampled.AudioFormat;

import org.jtransforms.fft.DoubleFFT_1D;

import common.Config;
import common.Log;
import filter.Complex;
import filter.ComplexOscillator;
import filter.DcRemoval;
import filter.IirFilter;
import filter.PolyPhaseFilter;
import sun.awt.SunToolkit.IllegalThreadException;
import filter.Delay;
import filter.HilbertTransform;

/**
 * The IQ Source takes an audio source that it reads from.  It then processes the IQ audio and produces and
 * output audio circular buffer.  It then looks like an audio source itself to a decoder.
 * 
 * @author chris.e.thompson
 *
 */
public class SourceIQ extends SourceAudio {
	// This is the audio source that holds incoming audio.  It contains a circularByte buffer and the decoder reads from it.  The decoder does
	// not care where the audio comes from or how it gets into the buffer
	SourceAudio upstreamAudioSource;
	Thread upstreamAudioReadThread;
	
	public boolean runPSKthroughNCO = true;
	
//	public static final int MODE_WFM = 0;
	public static final int MODE_FSK_HS = 1;
	public static final int MODE_FSK_DUV = 0;
	public static final int MODE_FSK_AUTO = 2;
	public static final int MODE_PSK_NC = 3;
	public static final int MODE_PSK_COSTAS = 4;
	
	private int mode = MODE_FSK_DUV;
	
	private int upstreamChannel = 0; // This is the audio channel that we read from the upstream audioSource
	private int channel = 0; // This is the audio channel where we store results - ALWAYS 0 for IQSource
	private boolean highSpeed = false;
	public static int AF_SAMPLE_RATE = 0;
	public AudioFormat upstreamAudioFormat;
	public int IQ_SAMPLE_RATE = 0;

	public static int FFT_SAMPLES = 4096;
	
//	private int selectedBin = 192/4;

	/* The number of samples to read from the IQ source each time we request data */
	public static int samplesToRead = 3840;
		
	int decimationFactor = 4; // This is the IQ SAMPLE_RATE / decoder SAMPLE_RATE.  e.g. 192/48
	
	double[] fftData = null; //new double[FFT_SAMPLES*2];
	double[] newData = null; //new double[fftData.length]; // make a new array to copy so we can store the section we want
//	private double[] psd = null; //new double[FFT_SAMPLES*2+1];;

	private double[] psdAvg = null; //new double[FFT_SAMPLES*2+1];;
	int psdAvgCount = 0;
	int PSD_AVG_LEN = 3;
	double[] overlap = null; //new double[2*FFT_SAMPLES - (samplesToRead/2)];
	
	double[] outputData = null;
	double[] fcdData = null; //new double[samplesToRead];
	double[] audioData = null;
	double[] demodAudio = null; //new double[samplesToRead/4];

	double centerFreq; // The frequency that the dongle is set to in kHz for historical reasons.  Multiply by 1000 for Hz
	
	double binBandwidth = 0;
	int filterWidth = 0 ; //We FFT filter +- this number of bins 64 bins is 3000 Hz for 4096 FFT samples, Normal FM channel is 16kHz = +-8kHz = 170
	int filterWidthHz = 4000; //If we use NCO, this is the width of the IF in Hz
	
	double[] blackmanWindow = null; //new double[FFT_SAMPLES+1];
	double[] blackmanFilterShape;
	double[] tukeyFilterShape;
	
	DoubleFFT_1D fft;
	FmDemodulator fm;

	// decimation filter params
	private static final int NZEROS = 5;
	private static final int NPOLES = 5;
	private double[] xvi = new double[NZEROS+1];
	private double[] yvi = new double[NPOLES+1];
	private double[] xvq = new double[NZEROS+1];
	private double[] yvq = new double[NPOLES+1];

	
	// DC balance filters
	DcRemoval audioDcFilter;
	DcRemoval iDcFilter;
	DcRemoval qDcFilter;
	
	PolyPhaseFilter polyFilter;
	PolyPhaseFilter polyFilter2;
	
	HilbertTransform ht;
	Delay delay;
	
	boolean fftDataFresh = false;
	public boolean offsetFFT = true;
	int dist = 0; // the offset distance
	
	// Only needed for Legacy NCO
//	private static final int SINCOS_SIZE = 256;
//	private double[] sinTab = new double[SINCOS_SIZE];
//	private double[] cosTab = new double[SINCOS_SIZE];
	
	RfData rfData;
	Thread rfDataThread;
	double[] phasorData;
	
	private int ssbOffset = -1400; // tune this far below or above the signal for optimal decode

	public SourceIQ(int circularDoubleBufferSize, int chan, boolean hs) {
		super("IQ Source" + hs, circularDoubleBufferSize, chan, false);
		highSpeed = hs;
		channel = chan;
		AF_SAMPLE_RATE = Config.afSampleRate;
		if (highSpeed && AF_SAMPLE_RATE < 48000) AF_SAMPLE_RATE = 48000;
		audioFormat = makeAudioFormat();

		// Legacy Oscillator
		// NCO
//		for (int n=0; n<SINCOS_SIZE; n++) {
//			sinTab[n] = Math.sin(n*2.0*Math.PI/SINCOS_SIZE);
//			cosTab[n] = Math.cos(n*2.0*Math.PI/SINCOS_SIZE);
//		}
	}

	public void setAudioSource(SourceAudio as, int chan) {
		upstreamChannel = chan;
		upstreamAudioSource = as;
		upstreamAudioFormat = as.getAudioFormat();	
	}
	
	public void setFilterWidth(int freq) {
		filterWidthHz = freq;
		if (freq == 0 || binBandwidth == 0) return;
		filterWidth = (int) (freq/binBandwidth);
		blackmanFilterShape = initBlackmanWindow(filterWidth*2); 
		tukeyFilterShape = initTukeyWindow(filterWidth*2); 
	}

	public int getMode() { return mode; }
	
	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public RfData getRfData() {
		if (rfData != null) {
			return rfData; 
		}
//		Log.println("RF DATA NULL");
		return null;
	}
	
	public int getAudioBufferCapacity() { return upstreamAudioSource.circularDoubleBuffer[upstreamChannel].getCapacity(); }
	
	public int getFilterWidth() { return filterWidth; }
	public void setSSBOffset(int s) { ssbOffset=s;}	
	public int getSSBOffset() { return ssbOffset;}
	public int getSSBOffsetInBins() { return (int)(ssbOffset/binBandwidth);}
	public double getCenterFreqkHz() { return centerFreq; }
	public void setCenterFreqkHz(double freq) { 
		centerFreq = freq; 
	}
	
	public void setSelectedBin(int bin) {  
		double f = getOffsetFrequencyFromBin(bin); // costas freq
		setSelectedFrequency(f);
	}
	
	public int getSelectedBin() {
		return getBinFromOffsetFreqHz(freq);
	}

	public void incSelectedFrequency() {
		setSelectedFrequency(freq+10);
	}
	
	public void decSelectedFrequency() {
		setSelectedFrequency(freq-10);
	}

	/**
	 * Tune the offset to the right place when passed a frequency in Hz
	 * Compensate for centerFreq which is in kHz
	 * @param f
	 */
	public void setTunedFrequency(double f) {
		double offset = f - centerFreq*1000;
		setSelectedFrequency(offset);
	}
	
	public double getTunedFrequency() {
		return centerFreq*1000 + freq;
	}
	
	/**
	 * Set the offset frequency to this frequency in Hz and tune the NCO if needed
	 * Store in config
	 * @param f
	 */
	public void setSelectedFrequency(double f) {
//		if (f > sampleRate / 2) f = sampleRate/2;
//		if (f < -1* sampleRate/ 2) f = -1* sampleRate/2;
		freq = f;
		Config.selectedFrequency = freq;
		if (runPSKthroughNCO && mode == MODE_PSK_COSTAS)  // not sure if this makes a difference, more testing needed
			return;
		if (nco != null) // && mode != MODE_PSK_COSTAS)
			nco.setFrequency(freq);
	}
	
	public double getSelectedFrequency() {
		return freq;
//		return getFrequencyFromBin(Config.selectedBin);
	}
	
	/**
	 * Get the offset from the center freq given a bin
	 * @param bin
	 * @return
	 */
	public long getOffsetFrequencyFromBin(int bin) {
		long freq = 0;
		if (bin < FFT_SAMPLES/2) {
			freq = (long)(bin*binBandwidth);
		} else {
			freq = (long)( -1* (FFT_SAMPLES-bin)*binBandwidth);
		}
		return freq;
	}
	
	public long getFrequencyFromBin(int bin) {
		long freq = 0;
		if (bin < FFT_SAMPLES/2) {
			freq = (long)(getCenterFreqkHz()*1000 + bin*binBandwidth);
		} else {
			freq = (long)(getCenterFreqkHz()*1000 - (FFT_SAMPLES-bin)*binBandwidth);
		}
		return freq;
	}

	public int getBinFromFreqHz(long freq) {
		long delta = (long) (freq-centerFreq*1000);
		return getBinFromOffsetFreqHz(delta);
	}
	
	public int getBinFromOffsetFreqHz(double delta) {
		int bin = 0;
		// Positive freq are 0 - FFT_SAMPLES/2
		// Negative freq are FFT_SAMPLES/2 - FFT_SAMPLES
		
		if (delta >= 0) {
			bin = (int) (delta/binBandwidth);
			if (bin >= FFT_SAMPLES/2) bin = FFT_SAMPLES/2-1;
		} else {
			bin = FFT_SAMPLES + (int) (delta/binBandwidth);
			if (bin < FFT_SAMPLES/2) bin = FFT_SAMPLES/2;
		}
		return bin;
	}
	
	public void setBinFromFrequencyHz(long freq) {
		
	}
	
	public double[] getPowerSpectralDensity() { 
//		if (fftDataFresh==false) {
//			return null;
//		} else {
//			fftDataFresh=false;
//			return psd;
			return psdAvg;
//		}
	}	

	/**
	 * Called when the IQ Source is started.  This tells the audio source to begin reading data
	 */
	protected void startAudioThread() {
		if (upstreamChannel == 0) {
			if (upstreamAudioReadThread != null) { 
				upstreamAudioSource.stop(); 
			}	
			
			if (!(upstreamAudioSource instanceof SourceUSB)) {
				upstreamAudioReadThread = new Thread(upstreamAudioSource);
				upstreamAudioReadThread.start();
			}
		}
	}

	/**
	 * 47Hz resolution has worked well in FoxTelem for the FCD.  ie 4096 length FFT.  To preserve that fidelity we calculate the nearest 
	 * power of 2 that gives that resolution, given a sampleRate, up to a maximum of 2^16
	 * Note that this fidelity is only needed if we are performing an FFT Filter. Not needed for NCO.
	 */
	private void setFFTsize() {
		
		if (Config.isRasperryPi()) {
			FFT_SAMPLES=2048;
			samplesToRead = 3840 /2;
			return;
		}
		if (mode == MODE_PSK_NC || mode == MODE_PSK_COSTAS || Config.useNCO) {
			FFT_SAMPLES=4096;
			samplesToRead = 3840 /2;
			return;			
		}
		for (int f=0; f<17; f++) {
			int len = (int)Math.pow(2, f);
			if (IQ_SAMPLE_RATE / len < 47) {
				int factor = len / 4096;
				if (factor == 0 ) {
					// Set to default
					FFT_SAMPLES=4096;
					samplesToRead = 3840;
					return;
				}
				FFT_SAMPLES = len; 
				samplesToRead = 3840 * factor/2;
				return;
			}
		}
		// Default to max
		FFT_SAMPLES = 4096 * 16;
		samplesToRead = 3840 * 16/2;
	}
	
	private void init() {	
		// The IQ Sample Rate is the same as the format for the upstream audio
		IQ_SAMPLE_RATE = (int)upstreamAudioFormat.getSampleRate();
	
		setFFTsize();
		fft = new DoubleFFT_1D(FFT_SAMPLES);
		fm = new FmDemodulator();
		blackmanWindow = initBlackmanWindow(FFT_SAMPLES); 

		fftData = new double[FFT_SAMPLES*2];
		psdAvg = new double[FFT_SAMPLES*2+1];;
		newData = new double[fftData.length]; // make a new array to copy so we can store the section we want

		decimationFactor = IQ_SAMPLE_RATE / AF_SAMPLE_RATE;
		if (decimationFactor == 0) decimationFactor = 1;  // User has chosen the wrong rate most likely
		binBandwidth = IQ_SAMPLE_RATE/(double)FFT_SAMPLES;
		
			
		if (mode == MODE_PSK_NC || mode == MODE_PSK_COSTAS) {		
			setFilterWidth(5000); // the width of the USB is only half this. We scan all in FFTFilter for Doppler follow 
			ht = new HilbertTransform(AF_SAMPLE_RATE, 255); // audio bandwidth and length
			delay = new Delay((255-1)/2);
		} else if (mode == MODE_FSK_HS) {
			if (Config.useNCO) 
				setFilterWidth(7000); //9600); // 7000 seems best for PolyPhase filter
			else
				setFilterWidth(2*9600); //9600); // 7000 seems best for PolyPhase filter
		//	setFilterWidth(2*75000);
		} else {
			if (Config.useNCO) 
				setFilterWidth(4000); // 5kHz deviation, 3kHz audio on Fox
			else
				setFilterWidth(5000); // 5kHz deviation, 3kHz audio on Fox
			//mode = MODE_NFM;
		}
		if (offsetFFT) // only needed for FFT Filter
			dist = 128*FFT_SAMPLES/4096; // offset puts the data outside the taper of the window function and gives better audio, but at the expense of dynamic range

		overlap = new double[2*FFT_SAMPLES - (samplesToRead)]; // we only use part of this, but this is the maximum it could be
		
		fcdData = new double[samplesToRead]; // this is the data block we read from the IQ source and pass to the FFT
		demodAudio = new double[samplesToRead/2];
		audioData = new double[samplesToRead/2/decimationFactor];  // we need the 2 because there are 4 bytes for each double and demod audio is samplesToRead/2
		phasorData = new double[samplesToRead]; // same length as the IQ data

		Log.println("IQDecoder Samples to read: " + samplesToRead);
		Log.println("IQDecoder using FFT sized to: " + FFT_SAMPLES);
		Log.println("Decimation Factor: " + decimationFactor);
		Log.println("IQ Sample Rate: " + IQ_SAMPLE_RATE);
		
		
		polyFilter = new PolyPhaseFilter(IQ_SAMPLE_RATE, filterWidthHz, decimationFactor, 13*decimationFactor);
		polyFilter2 = new PolyPhaseFilter(IQ_SAMPLE_RATE, filterWidthHz, decimationFactor, 13*decimationFactor);
		in = new double[decimationFactor];
		in2 = new double[decimationFactor];

		audioDcFilter = new DcRemoval(0.9999d);

		iDcFilter = new DcRemoval(0.9999d);
		qDcFilter = new DcRemoval(0.9999d);
		
///		setSelectedBin(Config.selectedBin);
		freq = Config.selectedFrequency;
		// Costas Loop or NCO downconvert
		nco = new ComplexOscillator(IQ_SAMPLE_RATE, (int) freq);
		// Costas
		
		// 4 pole cheb at fc = 0.025 = 1200Kz at 48k.  Ch 20 Eng and Sci guide to DSP
		double[] a = {1.504626E-05, 6.018503E-05, 9.027754E-05, 6.018503E-05, 1.504626E-05};
		double[] b = {1, 3.725385E+00, -5.226004E+00,  3.270902E+00,  -7.705239E-01};

		iFilter = new IirFilter(a,b);
		qFilter = new IirFilter(a,b);

		// Single pole IIR from Eng and Scientists Guide to DSP ch 19.  Higher X is amount of decay.  Higher X is slower
		// decay. x = e^-1/d where d is number of samples for decay. x = e^-2*pi*fc
		double x = 0.1;//0.86 is 6 samples, 0.606 is 2 samples 0.3678 is 1 sample, 0.1353 is 0.5 samples
		double[] a2 = {1-x};
		double[] b2 = {1, x};

		loopFilter = new IirFilter(a2,b2);

		rfData = new RfData(this);
		rfDataThread = new Thread(rfData);
		rfDataThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		rfDataThread.start();
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("SourceIQ");
		done = false;
		running = true;
		// Start reading data from the audio source.  This will store it in the circular buffer in the audio source
		startAudioThread();
		
		Log.println("IQ Source START. Running="+running);
		try {
		init();
		} catch (IllegalThreadStateException e) {
			Log.errorDialog("ERROR", "Trying to start a second Decoder when not supported by the hardware\n"
					+ "Perhaps DUV and HS were selected for an SDR that can not be opened twice. \n"
					+ "Set the decoder to one mode and restart FoxTelem.");
			running = false;
		}
		while (running) {
			int nBytesRead = 0;
			if (circularDoubleBuffer[channel].getCapacity() > fcdData.length) {
				nBytesRead = upstreamAudioSource.read(fcdData, upstreamChannel);
				if (nBytesRead != fcdData.length)
					if (Config.debugAudioGlitches) Log.println("ERROR: IQ Source could not read sufficient data from audio source");
				if (mode == MODE_PSK_COSTAS)
					if (runPSKthroughNCO)
						outputData = processNCOBytes(fcdData);
					else
						outputData = processPSKBytes(fcdData);
				else if (mode == MODE_PSK_NC)
					if (runPSKthroughNCO)
						outputData = processNCOBytes(fcdData);
					else
						outputData = processBytes(fcdData);
				else if (Config.useNCO)
					outputData = processNCOBytes(fcdData);
				else
					outputData = processBytes(fcdData);
		////		Log.println("IQ Source writing data to audio thread");
				/** 
				 * Simulate a slower computer for testing
				 
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				*/
				for(int i=0; i < outputData.length; i+=2) {	
			// FUDGE		
					if (i+2 > outputData.length) {
						//Log.println("Only added: "+i +" of " + outputData.length);
						break;
					}
					circularDoubleBuffer[channel].add(outputData[i],outputData[i+1]);
				}
			} else {
				try {
					Thread.sleep(0,1);
				} catch (InterruptedException e) {
					e.printStackTrace(Log.getWriter());
				}
			}
		}
		rfData.stopProcessing();
		Log.println("IQ Source EXIT.  Running="+running);

	}

	@Override
	public void stop() {
		running = false;
		upstreamAudioSource.stop();
		while (!upstreamAudioSource.isDone())
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		done = true;
		
	}

	// local variables that I want to allocate only once
//	byte[] ib = new byte[2];
//	byte[] qb = new byte[2];
	double gain = 1;
	static final double DESIRED_RANGE = 0.7; // from -0.5 to +0.5
	
	int decimateCount = 0;
	double[] in ;
	double[] in2 ;
	double pfValue, pfValue2, audioI, audioQ;

	double iMixNco, qMixNco;
	/**
	 * Process IQ bytes and return a set of 48K audio bytes that can be processed by the decoder as normal
	 * @param fcdData
	 * @return
	 */
	protected double[] processNCOBytes(double[] fcdData) {
		zeroFFT();
		int i = 0;
		
		// Loop through the 192k data, sample size 2 because we read doubles from the audio source buffer
		for (int j=0; j < fcdData.length; j+=2 ) { // sample size is 2, 1 double per channel
			//double id, qd;

			id = fcdData[j];
			qd = fcdData[j+1];
			// filter out any DC from I/Q signals
			id = iDcFilter.filter(id);
			qd = qDcFilter.filter(qd);
			
			if (mode == MODE_PSK_NC || mode == MODE_PSK_COSTAS) // effectively USB
				nco.setFrequency(freq+ssbOffset); //-1400); // ssboffset -ve for USB demod

			c = nco.nextSample();
			c.normalize();
			// Mix 
			iMixNco = gain*id * c.geti() + gain*qd*c.getq();
			qMixNco = gain*qd * c.geti() - gain*id*c.getq();

			try {
			in[decimateCount] = iMixNco;
			in2[decimateCount] = qMixNco;
			} catch (ArrayIndexOutOfBoundsException e) {
				// we likely changed the rate, but usually we can recover from this. Try to ignore
				Log.println("ERROR WITH DECIMATION RATE. count:" + decimateCount + " rate:" + decimationFactor);
			}
			decimateCount++;
			if (decimateCount >= decimationFactor) {
				decimateCount = 0;
				pfValue = polyFilter.filterDouble(in);
				pfValue2 = polyFilter2.filterDouble(in2);
				if (mode == MODE_PSK_NC || mode == MODE_PSK_COSTAS) {
					//Demodulate ssb
					audioQ = ht.filter(pfValue);
					audioI = delay.filter(pfValue2);
					//double audio = audioI - audioQ; // LSB
					audioData[j/(2*decimationFactor)] = audioI + audioQ; // USB
				} else
					audioData[j/(2*decimationFactor)] = fm.demodulate(pfValue, pfValue2);
			}
			
			// i and q go into consecutive spaces in the complex FFT data input
			if (Config.swapIQ) {
				fftData[i+dist] = qd;
				fftData[i+1+dist] = id;
			} else {
				fftData[i+dist] = id;
				fftData[i+1+dist] = qd;
			}
			i+=2;
		}
		runFFT(fftData); // results back in fftData
		fftDataFresh = false;	
		if (!Config.showIF) calcPsd();
		fftDataFresh = true;
		filterFFTWindow(fftData); // do this regardless because it also calculates the SNR
		if (Config.showIF) calcPsd();

		return audioData; 
	}
	
protected double[] processPSKBytes(double[] fcdData) {
		
		double maxValue = 0;
		double minValue = 0;
		zeroFFT();
		int i = 0;
		int d=0;
		int k = 0;
		sumLockLevel = 0;
		// Loop through the 192k data, sample size 2 because we read doubles from the audio source buffer
		for (int j=0; j < fcdData.length; j+=2 ) { // sample size is 2, 1 double per channel
			//double id, qd;

			id = fcdData[j];
			qd = fcdData[j+1];
			// filter out any DC from I/Q signals
			id = iDcFilter.filter(id);
			qd = qDcFilter.filter(qd);
			
			if (id > maxValue) maxValue = id;
			if (id < minValue) minValue = id;
			demodAudio[d++] = pskDemod(gain*id, gain*qd, j/2);
			sumLockLevel += lockLevel;
			// i and q go into consecutive spaces in the complex FFT data input
			if (Config.swapIQ) {
				fftData[i+dist] = qd;
				fftData[i+1+dist] = id;
			} else {
				fftData[i+dist] = id;
				fftData[i+1+dist] = qd;
			}
			i+=2;
		}
		avgLockLevel = sumLockLevel / (double)(fcdData.length/2.0);
		runFFT(fftData); // results back in fftData

		if (!Config.showIF) calcPsd();

		filterFFTWindow(fftData); // do this regardless because it also calculates the SNR
			
		fftDataFresh = false;	
		if (Config.showIF) calcPsd();
		fftDataFresh = true;	
		
		gain = DESIRED_RANGE / (1.0f * (maxValue-minValue));
		
		// Filter any frequencies above 24kHz before we decimate to 48k. These are gentle
		// This is a balance.  Too much filtering impacts the 9600 bps decode, so we use a wider filter
		// These are gentle phase neutral IIR filters, so that we don't mess up the FM demodulation
		// No needed with NCO as we have already filtered to 3kHz
		for (int t=0; t < 1; t++) // FUDGE  - 5 better for Airspy 1 for not
			if (highSpeed)
				antiAlias20kHzIIRFilter(demodAudio);
			else {
				antiAlias16kHzIIRFilter(demodAudio);				
			}
		
		// Every 4th entry goes to the audio output to get us from 192k -> 48k
		for (int j=0; j < demodAudio.length; j+=decimationFactor ) { // data size is 1 decimate by factor of 4 to get to audio format size
			// scaling and conversion to integer
			double value = audioDcFilter.filter(demodAudio[j]); // remove DC.  Only need to do this to the values we want to keep
	// FUDGE - safety factor because the decimation is not exact
			if (k >= audioData.length ) {
				//Log.println("k:" + k);
				break;
			}
			audioData[k] = value;
			k+=1;			
		 }
		return audioData; 
	}

double id, qd;
/**
 * Process IQ bytes and return a set of 48K audio bytes that can be processed by the decoder as normal
 * We cache the read bytes in case we need to adjust for the clock
 * If this is being called because the clock moved, we do not re-cache the data
 * 
 * We use this for the FFT Filter version of DUV/HS and for the Non Coherent PSK decoder.
 * @param fcdData
 * @return
 */
protected double[] processBytes(double[] fcdData) {
	if (!(mode == MODE_PSK_NC) && Config.useNCO) {
		Log.errorDialog("FATAL", "Trying to run non NCO decoder with Config.useNCO set");
		return new double[0];
	}

	zeroFFT();
	int i = 0;
	
	// DC Filter the incoming data
	for (int j=0; j < fcdData.length; j+=2 ) { // sample size is 2, 1 double per channel
		
		id = fcdData[j];
		qd = fcdData[j+1];
		// filter out any DC from I/Q signals
		fcdData[j] = iDcFilter.filter(id);
		fcdData[j+1] = qDcFilter.filter(qd);
		
		
	}
	
//	if (Config.useNCO) {
//			 ncoDecimate(fcdData, fcdData2);
//	}

	// Loop through the 192k data, sample size 2 because we read doubles from the audio source buffer
	for (int j=0; j < fcdData.length; j+=2 ) { // sample size is 2, 1 double per channel
		double id, qd;
		id = fcdData[j];
		qd = fcdData[j+1];
		
		// filter out any DC from I/Q signals
//		id = iDcFilter.filter(id);
//		qd = qDcFilter.filter(qd);

		// i and q go into consecutive spaces in the complex FFT data input
		if (Config.swapIQ) {
			fftData[i+dist] = qd;
			fftData[i+1+dist] = id;
		} else {
			fftData[i+dist] = id;
			fftData[i+1+dist] = qd;
		}
		i+=2;
	}

	runFFT(fftData); // results back in fftData
	fftDataFresh = false;	
	if (!Config.showIF) calcPsd();
	fftDataFresh = true;

	filterFFTWindow(fftData); // do this regardless because it also calculates the SNR

	if (Config.showIF) calcPsd();

	if (mode == MODE_PSK_NC) 
		;
	else
		inverseFFT(fftData);
	int d=0;
	// loop through the raw Audio array, which has 2 doubles for each entry - i and q
	for (int j=0; j < fcdData.length; j +=2 ) { // data size is 2 
		if (mode == MODE_PSK_NC)
			demodAudio[d++] = ncoDownconvert(fcdData[j], fcdData[j+1]);
		else
			demodAudio[d++] = fm.demodulate(fftData[j+dist], fftData[j+1+dist]);	
	}
	
	int k = 0;
	// Filter any frequencies above 24kHz before we decimate to 48k. These are gentle
	// This is a balance.  Too much filtering impacts the 9600 bps decode, so we use a wider filter
	// These are gentle phase neutral IIR filters, so that we don't mess up the FM demodulation

	for (int t=0; t < 1; t++) // FUDGE  - 5 better for Airspy 1 for not
		if (highSpeed)
			demodAudio = mono20kHzIIRFilter(demodAudio);
			//antiAlias20kHzIIRFilter(demodAudio);
		else {
			demodAudio = mono16kHzIIRFilter(demodAudio);
//			antiAlias16kHzIIRFilter(demodAudio);				
		}
	
	// Every 4th entry goes to the audio output to get us from 192k -> 48k
	for (int j=0; j < demodAudio.length; j+=decimationFactor ) { // data size is 1 decimate by factor of 4 to get to audio format size
		// scaling and conversion to integer
		double value = audioDcFilter.filter(demodAudio[j]); // remove DC.  Only need to do this to the values we want to keep
// FUDGE - safety factor because the decimation is not exact
		if (k >= audioData.length ) {
			//Log.println("k:" + k);
			break;
		}
		audioData[k] = value;
		k+=1;			
	 }
	return audioData; 
}

	private void zeroFFT() {
		for (int i=0; i< FFT_SAMPLES*2; i++) {
			fftData[i] = 0.0;
		}
	}
	
	private void runFFT(double[] fftData) {
		
		// calculating the fft of the data, so we will have spectral power of each frequency component
		// fft resolution (number of bins) is samplesNum, because we initialized with that value
		//if (Config.applyBlackmanWindow)
		for (int s=0; s<fftData.length-1; s+=2) {
			fftData[s] = blackmanWindow[s/2] * fftData[s];
			fftData[s+1] = blackmanWindow[s/2] * fftData[s+1];
		}
		fft.complexForward(fftData);
	}

	boolean firstRun = true;
	double psd;
	private void calcPsd() {
		// Calculate power spectral density (PSD) so that we can display it
		// This is the magnitude of the complex signal, so it is sqrt(i^2 + q^2)
		// divided by the bin bandwidth  
		for (int s=0; s<fftData.length-1; s+=2) {
			psd =  psd(fftData[s], fftData[s+1]);
			if (firstRun)
				psdAvg[s/2] = psd; // we do not have an average yet
			else if (psd != 0)
				psdAvg[s/2] = average(psdAvg[s/2], psd, PSD_AVG_LEN);
		}
		firstRun = false;

	}
	
	private double psd(double i, double q) {
		// 10*log (x^2 + y^2) == 20 * log(sqrt(x^2 + y^2)) so we can avoid the sqrt
		return (20*Math.log10(Math.sqrt((i*i) + (q*q))/binBandwidth));	// Compute PSD
		//psd[s/2] = 10*Math.log10( ((fftData[s]*fftData[s]) + (fftData[s+1]*fftData[s+1]) )/binBandwidth);	// Compute PSD
		
	}
	
	public static double average (double avg, double new_sample, int N) {
		avg -= avg / N;
		avg += new_sample / N;
		if (Double.isNaN(avg)) avg = 0;
		if (Double.isInfinite(avg)) avg = 0;
		return avg;
	}
	
	int overlapLength;
	private void inverseFFT(double[] fftData) {
		fft.complexInverse(fftData, true); // true means apply scaling - but it does not work?
	
		// Add the last overlap.  
		// We have iq data, so the FFTData buffer is actually FFT_SAMPLES * 2 in length
		// We request samplesToRead bytes from the source
		// That data becomes 2 iq samples

		overlapLength = filterWidth*2; // te amount of overlap we need is equal to the length of the filter
		overlapLength = 0;
			
		for (int o=0; o < overlapLength; o++) {
			fftData[o+dist] += overlap[o]; ///// ADDING THE OVERLAP causes distortion unless we get it just right.  The data is offset, so add from the offset distance
		}

		// capture this overlap
		// We start right after the data, which runs from dist to samplesToRead+dist
		for (int o=samplesToRead+dist; o < samplesToRead+dist+overlapLength; o++) {
			overlap[o-samplesToRead-dist] = fftData[o];
		}

	}

	/**
	 * Apply a Tukey or blackman window to the segment of the spectrum that we want, then move the selected frequency to zero
	 * We have selected a bin.  This is the FFT bin from 0 to FFT_SAMPLES.  It could be positive or negative.
	 * 
	 * We use the window so that we do not have the high frequency effects from a square window.  These will alias
	 * due to the gibbs effect and cause issues.  However, we are also limited because we can only make a course move otherwise phase
	 * discontinuities give us issues:
	 * 
	 * We also calculate the RF Measurements:
	 * As of v1.06g we have clarified the names and meanings of the values as follows:
	 * 
	 * PEAK_SIGNAL_IN_FILTER_WIDTH: the bin of the strongest signal in the pass band is set as the peak Signal, 
	 * the average signal
	 * in the passband and the noise in an equivalent bandwidth just outside the pass band is also measured so we can calculate the RF SNR
	 *  
	 * @param fftData
	 */
	private void filterFFTWindow(double[] fftData) {
		for (int z=0; z<fftData.length; z++) newData[z] = 0.0;

		int binOfPeakSignalInFilterWidth = 0;  // the peak signal bin
		double peakSignalInFilterWidth = -999999;
		double strongestSigInSatBand = -999999;
		int binOfStrongestSigInSatBand = 0;

		// We break the spectrum into chunks, so that we pass through it only once, but can analyze the different sections in different ways
		// 0 - noiseStart
		// noiseStart to start of pass band
		// the pass band
		// pass band to noiseEnd
		// noiseEnd to end of spectrum

		double noiseOutsideFilterWidth = 0;
		double avgSigInFilterWidth = 0;
		int noiseReading = 0;
		int sigReading = 0;
		// snapshot these so they dont change as we measure
		int fromBin = Config.fromBin;
		int toBin = Config.toBin;
		boolean spansDcSpike = false;
		
		int selectedBin = getBinFromOffsetFreqHz(freq);
		
		// If we are outside the FFT range then pick a default point.  This happens if the FFT is resized between runs, e.g. a different SDR device
		if (selectedBin*2 > fftData.length) {
			selectedBin = 3 * fftData.length / 8;
			this.setSelectedBin(selectedBin);
		}
		int binIndex = selectedBin*2; // multiply by two because each bin has a real and imaginary part
		int filterBins = filterWidth*2;

		int start = binIndex - filterBins;
//		if (mode == SourceIQ.MODE_PSK_COSTAS || mode == SourceIQ.MODE_PSK_NC)
//			start = binIndex; // USB demod, so we do not look at lower sideband

		if (start < 0) start = 0;
		int end = binIndex + filterBins;
		if (end > fftData.length-2) end = fftData.length-2;
		
		if (fromBin == toBin) {
			// We have a mismatch between the sat range and the frequency.  Set from/to equal to filter width only
			fromBin = start/2;
			toBin = end/2;
		}
		if (toBin < fromBin) {
			// Then we span the central spike.  Not ideal.  We will not get a Strong Sig reading at all unless we search the two parts of the FFT
			// seperately
			spansDcSpike = true;
		}
		
		// Selected frequencies part
		int dcOffset=0; // must be even
		int k=dcOffset;
		double iAvg = 0;
		double qAvg = 0;

		int noiseStart = start - filterBins;
		if (noiseStart < 0 ) noiseStart = 0;
		int noiseEnd = end + filterBins;
		if (noiseEnd > fftData.length-2) noiseEnd = fftData.length - 2;

		/* Here we start the analysis of the spectrum.  We use our knowledge of the sat band, which is fromBin toBin.
		 * We split it into the following chunks:
		 * 0 - noiseStart: This is outside the filter width and beyond the point we measure noise
		 * noiseStart - start: This is just outside the filter width and is the place we measure the noise level
		 * start - end: This is the fitler width where we decode the signal.  This is where we want the signal to be
		 * end - noiseEnd: This is just outside the filter width and is the place we measure the noise level
		 * noiseEnd - length of FFT data: This is outside the filter width and beyond the point we measure noise
		 */
		double sig = 0;
		for (int n=0; n < noiseStart; n+=2) {
			if ((fromBin*2 < n && n < toBin*2) 
					|| (spansDcSpike && fromBin*2 < n && n < fftData.length-2) || (spansDcSpike && 0 <= n && n < toBin*2)) {
				sig = psd(fftData[n], fftData[n+1]);
				if (sig > strongestSigInSatBand) {
					strongestSigInSatBand = sig;
					binOfStrongestSigInSatBand = n/2;
				}
			}
		}

		for (int n=noiseStart; n< start; n+=2) {
			sig = psd(fftData[n], fftData[n+1]);
			if ((fromBin*2 < n && n < toBin*2)
					|| (spansDcSpike && fromBin*2 < n && n < fftData.length-2) || (spansDcSpike && 0 <= n && n < toBin*2)) {
				if (sig > strongestSigInSatBand) {
					strongestSigInSatBand = sig;
					binOfStrongestSigInSatBand = n/2;
				}
			}			
			noiseOutsideFilterWidth += sig;
			noiseReading++;
		}
//		double sigList[] = new double[(end-start+2)/2];
		for (int i = start; i <= end; i+=2) {
			if (!Config.useNCO)
				if (Config.applyBlackmanWindow) {
					newData[k] = fftData[i] * Math.abs(blackmanFilterShape[k/2-dcOffset/2]) ;
					newData[k+1] = fftData[i + 1] * Math.abs(blackmanFilterShape[k/2-dcOffset/2]);	
				} else {
					newData[k] = fftData[i] * Math.abs(tukeyFilterShape[k/2-dcOffset/2]) ;
					newData[k+1] = fftData[i + 1] * Math.abs(tukeyFilterShape[k/2-dcOffset/2]);
				}
			sig = psd(fftData[i], fftData[i+1]);
			if ((fromBin*2 < i && i < toBin*2)
					|| (spansDcSpike && fromBin*2 < i && i < fftData.length-2) || (spansDcSpike && 0 <= i && i < toBin*2)) {
				if (sig > strongestSigInSatBand) {
					strongestSigInSatBand = sig;
					binOfStrongestSigInSatBand = i/2;
				}


				if (sig > peakSignalInFilterWidth) { 
					peakSignalInFilterWidth = sig;
					binOfPeakSignalInFilterWidth = i/2;
				}
			}
			avgSigInFilterWidth += sig;
			//			sigList[sigReading] = sig;
			sigReading++;

			iAvg += newData[k];
			qAvg += newData[k+1];
			k+=2;
		}
		for (int n=end; n < noiseEnd; n+=2) {
			sig = psd(fftData[n], fftData[n+1]);
			if ((fromBin*2 < n && n < toBin*2)
					|| (spansDcSpike && fromBin*2 < n && n < fftData.length-2) || (spansDcSpike && 0 <= n && n < toBin*2)) {
				if (sig > strongestSigInSatBand) {
					strongestSigInSatBand = sig;
					binOfStrongestSigInSatBand = n/2;
				}
			}			
			noiseOutsideFilterWidth += sig;
			noiseReading++;
		}

		for (int n=noiseEnd; n < fftData.length-2; n+=2) {
			if ((fromBin*2 < n && n < toBin*2)
					|| (spansDcSpike && fromBin*2 < n && n < fftData.length-2) || (spansDcSpike && 0 <= n && n < toBin*2)) {
				sig = psd(fftData[n], fftData[n+1]);
				if (sig > strongestSigInSatBand) {
					strongestSigInSatBand = sig;
					binOfStrongestSigInSatBand = n/2;
				}
			}
		}
		
		// redo strongest sig to debug
//		if (Config.findSignal) {
//			strongestSigInSatBand = -99999;
//			binOfStrongestSigInSatBand = 0;
//			for (int n=0; n < fftData.length-2; n+=2) {
//				if ((fromBin*2 < n && n < toBin*2) 
//						|| (spansDcSpike && fromBin*2 < n && n < fftData.length-2) || (spansDcSpike && 0 <= n && n < toBin*2)) {
//					sig = psd(fftData[n], fftData[n+1]);
//					if (sig > strongestSigInSatBand) {
//						strongestSigInSatBand = sig;
//						binOfStrongestSigInSatBand = n/2;
//					}
//				}
//			}
//		}

		//		fftData[0] = 0;
		//		fftData[1] = 0;
		// Write the DC bins - seem to need this for the blackman filter
		if (!Config.useNCO)
			if (Config.applyBlackmanWindow) {
				newData[0] = iAvg / (filterBins * 2);
				newData[1] = qAvg / (filterBins * 2);
			}
		avgSigInFilterWidth = avgSigInFilterWidth / (double)sigReading;
		noiseOutsideFilterWidth = noiseOutsideFilterWidth / (double)noiseReading;
		//		if (Config.debugSignalFinder) {
		
//		Log.println("Sig: " + avgSigInFilterWidth + " from " + sigReading + " Noise: " + noiseOutsideFilterWidth + " from readings: " + noiseReading);			
//		Log.println("Peak: " + peakSignalInFilterWidth+ " bin: " + binOfPeakSignalInFilterWidth);	
//		Log.println("Strong: "+ strongestSigInSatBand+ " bin: " + binOfStrongestSigInSatBand + " From: " + fromBin + " To: " + toBin);
//		Log.println("DC Spike:"+spansDcSpike + " From: " + Config.fromBin + " To: " + Config.toBin);
//		for (double d : sigList) Log.println(""+ d);
		//		}

		// store the peak signal - PEAK_SIGNAL_IN_FILTER_WIDTH
		rfData.setPeakSignalInFilterWidth(peakSignalInFilterWidth, binOfPeakSignalInFilterWidth, avgSigInFilterWidth, noiseOutsideFilterWidth);

		// store the strongest sigs - STRONGEST_SIGNAL_IN_SAT_BAND
		rfData.setStrongestSignal(strongestSigInSatBand, binOfStrongestSigInSatBand);

		if (!Config.useNCO)
			for (int i = 0; i < fftData.length; i+=1) {
				fftData[i] = newData[i];			
			}

		//return newData;
	}



	
	@SuppressWarnings("unused")
	private void antiAlias12kHzIIRFilter(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// Given we only care about frequencies much lower, we can be more aggressive with the cutoff
		// and use a shorter filter, so we choose 12kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		double GAIN = 9.197583870e+02;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
			xvi[5] = rawAudioData[j] / GAIN;
			yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
			yvi[5] =   (xvi[0] + xvi[5]) + 5 * (xvi[1] + xvi[4]) + 10 * (xvi[2] + xvi[3])
					+ (  0.0884067190 * yvi[0]) + ( -0.6658412244 * yvi[1])
					+ (  2.0688801399 * yvi[2]) + ( -3.3337619982 * yvi[3])
					+ (  2.8075246179 * yvi[4]);
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
			xvq[5] = rawAudioData[j+1] / GAIN;
			yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
			yvq[5] =   (xvq[0] + xvq[5]) + 5 * (xvq[1] + xvq[4]) + 10 * (xvq[2] + xvq[3])
					+ (  0.0884067190 * yvq[0]) + ( -0.6658412244 * yvq[1])
					+ (  2.0688801399 * yvq[2]) + ( -3.3337619982 * yvq[3])
					+ (  2.8075246179 * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
	}
	
	private void antiAlias16kHzIIRFilter(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// To avoid aliases, we filter at less than 24kHz, but we need to filter higher than 9600 * 2.  So we pick 20kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		double GAIN = 3.006238283e+02;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
	        xvi[5] = rawAudioData[j] / GAIN;
	        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
	        yvi[5] =   (xvi[0] + xvi[5]) + 5 * (xvi[1] + xvi[4]) + 10 * (xvi[2] + xvi[3])
	                     + (  0.0394215023 * yvi[0]) + ( -0.3293736653 * yvi[1])
	                     + (  1.1566456875 * yvi[2]) + ( -2.1603685744 * yvi[3])
	                     + (  2.1872297286 * yvi[4]);
	        
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
	        xvq[5] = rawAudioData[j+1] / GAIN;
	        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
	        yvq[5] =   (xvq[0] + xvq[5]) + 5 * (xvq[1] + xvq[4]) + 10 * (xvq[2] + xvq[3])
	                     + (  0.0394215023 * yvq[0]) + ( -0.3293736653 * yvq[1])
	                     + (  1.1566456875 * yvq[2]) + ( -2.1603685744 * yvq[3])
	                     + (  2.1872297286 * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}

	private double[] mono16kHzIIRFilter(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// To avoid aliases, we filter at less than 24kHz, but we need to filter higher than 9600 * 2.  So we pick 20kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		double[] out = new double[rawAudioData.length];
		
		for (int j=0; j < rawAudioData.length; j+=4 ) {
			out[j] = mono16kHzValue(rawAudioData[j]);
		}
		return out;
	}
	
	private double mono16kHzValue(double in) {
		double GAIN = 3.006238283e+02;
		xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
        xvi[5] = in / GAIN;
        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
        yvi[5] =   (xvi[0] + xvi[5]) + 5 * (xvi[1] + xvi[4]) + 10 * (xvi[2] + xvi[3])
                     + (  0.0394215023 * yvi[0]) + ( -0.3293736653 * yvi[1])
                     + (  1.1566456875 * yvi[2]) + ( -2.1603685744 * yvi[3])
                     + (  2.1872297286 * yvi[4]);
        
		return yvi[5];
	}

	/*
	private double mono16kHzQValue(double in) {
		double GAIN = 3.006238283e+02;
		xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
        xvq[5] = in / GAIN;
        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
        yvq[5] =   (xvq[0] + xvq[5]) + 5 * (xvq[1] + xvq[4]) + 10 * (xvq[2] + xvq[3])
                     + (  0.0394215023 * yvq[0]) + ( -0.3293736653 * yvq[1])
                     + (  1.1566456875 * yvq[2]) + ( -2.1603685744 * yvq[3])
                     + (  2.1872297286 * yvq[4]);
		return yvq[5];
        
	}
	*/
	
	private double[] mono20kHzIIRFilter(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// To avoid aliases, we filter at less than 24kHz, but we need to filter higher than 9600 * 2.  So we pick 20kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		double[] out = new double[rawAudioData.length];
		
		for (int j=0; j < rawAudioData.length; j+=4 ) {
			out[j] = mono20kHzValue(rawAudioData[j]);
		}
		return out;
	}
	
	private double mono20kHzValue(double in) {
		double GAIN = 3.006238283e+02;
		xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
        xvi[5] = in / GAIN;
        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
        yvi[5] =   (xvi[0] + xvi[5]) + 5 * (xvi[1] + xvi[4]) + 10 * (xvi[2] + xvi[3])
                + (  0.0175819825 * yvi[0]) + ( -0.1605530625 * yvi[1])
                + (  0.6276667742 * yvi[2]) + ( -1.3438919231 * yvi[3])
                + (  1.6182326801 * yvi[4]);
        
		return yvi[5];
	}

	private void antiAlias20kHzIIRFilter(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// To avoid aliases, we filter at less than 24kHz, but we need to filter higher than 9600 * 2.  So we pick 20kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		double GAIN = 1.328001690e+02;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
	        xvi[5] = rawAudioData[j] / GAIN;
	        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
	        yvi[5] =   (xvi[0] + xvi[5]) + 5 * (xvi[1] + xvi[4]) + 10 * (xvi[2] + xvi[3])
	                     + (  0.0175819825 * yvi[0]) + ( -0.1605530625 * yvi[1])
	                     + (  0.6276667742 * yvi[2]) + ( -1.3438919231 * yvi[3])
	                     + (  1.6182326801 * yvi[4]);
	        
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
	        xvq[5] = rawAudioData[j+1] / GAIN;
	        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
	        yvq[5] =   (xvq[0] + xvq[5]) + 5 * (xvq[1] + xvq[4]) + 10 * (xvq[2] + xvq[3])
	                     + (  0.0175819825 * yvq[0]) + ( -0.1605530625 * yvq[1])
	                     + (  0.6276667742 * yvq[2]) + ( -1.3438919231 * yvq[3])
	                     + (  1.6182326801 * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}

	@SuppressWarnings("unused")
	private void antiAlias24kHzIIRFilter(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		double GAIN = 7.052142314e+01;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
	        xvi[5] = rawAudioData[j] / GAIN;
	        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
	        yvi[5] =   (xvi[0] + xvi[5]) + 5 * (xvi[1] + xvi[4]) + 10 * (xvi[2] + xvi[3])
	                     + (  0.0078245894 * yvi[0]) + ( -0.0773061573 * yvi[1])
	                     + (  0.3292913096 * yvi[2]) + ( -0.8090163636 * yvi[3])
	                     + (  1.0954437995 * yvi[4]);
	        
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
	        xvq[5] = rawAudioData[j+1] / GAIN;
	        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
	        yvq[5] =   (xvq[0] + xvq[5]) + 5 * (xvq[1] + xvq[4]) + 10 * (xvq[2] + xvq[3])
	                     + (  0.0078245894 * yvq[0]) + ( -0.0773061573 * yvq[1])
	                     + (  0.3292913096 * yvq[2]) + ( -0.8090163636 * yvq[3])
	                     + (  1.0954437995 * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}

	private double[] initBlackmanWindow(int len) {
		double[] blackmanWindow = new double[len+1];
		for (int i=0; i <= len; i ++) {
			blackmanWindow[i] =  (0.42 - 0.5 * Math.cos(2 * Math.PI * i / len) + 0.08 * Math.cos((4 * Math.PI * i) / len));
			if (blackmanWindow[i] < 0)
				blackmanWindow[i] = 0;
			//blackmanWindow[i] = 1;
		}
		return blackmanWindow;
	}

	/**
	 * A tapered cosine window that has a flat top and Hann window sides
	 * At alpha = 0 this is a rectangular window
	 * At alpha = 1 it is a Hann window
	 * @param len
	 * @return
	 */
	private double[] initTukeyWindow(int len) {
		double[] window = new double[len+1];
		double alpha = 0.5f;
		int lowerLimit = (int) ((alpha*(len-1))/2);
		int upperLimit = (int) ((len -1) * ( 1 - alpha/2));
		
		for (int i=0; i < lowerLimit; i ++) {
			window[i] = (double) (0.5 * (1 + Math.cos(Math.PI*( 2*i/(alpha*(len-1)) -1))));
		}
		for (int i=lowerLimit; i < upperLimit; i ++) {
			window[i] = 1;
		}
		for (int i=upperLimit; i < len; i ++) {
			window[i] = (double) (0.5 * (1 + Math.cos(Math.PI*( 2*i/(alpha*(len-1)) - (2/alpha)+1))));
		}
		return window;
	}

	
	public static AudioFormat makeAudioFormat(){		
		float sampleRate = AF_SAMPLE_RATE;
		int sampleSizeInBits = 16;
		//Decoder.bitsPerSample = 16;
		int channels = 2;
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat af = new AudioFormat(sampleRate,sampleSizeInBits,channels,signed,bigEndian); 
		//System.out.println("Using standard format");
		Log.println("IQ Standard output format " + af);
		return af;
	}//end getAudioFormat


	private double ncoDownconvert(double i, double q) {
		nco.setFrequency(freq+ssbOffset); // ssboffset
		c = nco.nextSample();
		c.normalize();
		// Mix 
		iMix = i * c.geti() + q*c.getq();
		qMix = q * c.geti() - i*c.getq();
		return iMix + qMix;
	}
	
	double psk = 0.0;
	private double pskDemod(double i, double q, int sample) {
		ssbOffset = 0;
		psk = costasLoop(i, q, sample);
		if (this.rfData.rfStrongestSigSNRInSatBand > Config.SCAN_SIGNAL_THRESHOLD) {
			
			// only scan if we have a signal, but any signal in the sat band triggers this
			nco.changePhase(alpha*error);
			costasLoopFreq = costasLoopFreq + beta*error;		
			if (avgLockLevel < LOCK_LEVEL_THRESHOLD && lockLevel > 0) {
				// susceptible to false lock at half the bitrate. Scan range only needs to be enough to defeeat false lock.  Not to find or follow signal.
				long strongestFreq = getOffsetFrequencyFromBin(rfData.getBinOfStrongestSignalInSatBand());
				if (strongestFreq - costasLoopFreq > SCAN_RANGE || strongestFreq - costasLoopFreq < -SCAN_RANGE) {
					// we are more than a kc off
					costasLoopFreq = strongestFreq;
				} else {
					costasLoopFreq = costasLoopFreq + gamma; 
//					if (freq > this.getOffsetFrequencyFromBin(selectedBin)+SCAN_RANGE) // defeat false lock at 600Hz or other freq.
//						freq = this.getOffsetFrequencyFromBin(selectedBin)+SCAN_RANGE;
//					if (freq < this.getOffsetFrequencyFromBin(selectedBin)-SCAN_RANGE)
//						freq = this.getOffsetFrequencyFromBin(selectedBin)-SCAN_RANGE;
				} 
			}
			// These are hard limits for safety, but if we are locked, then the loop can follow the signal as far as it likes
			// Note if we momentarily lose lock, then the frequency could snap back to the tune point.  So we  update the tune point without moving it
			// We just change the bin but not the freq
			if (costasLoopFreq > freq + SCAN_RANGE) costasLoopFreq = freq + SCAN_RANGE;
			if (costasLoopFreq < (freq-SCAN_RANGE)) costasLoopFreq = freq-SCAN_RANGE;
			nco.setFrequency(costasLoopFreq);
			//selectedBin = getBinFromOffsetFreqHz((long) getCostasFrequency());
		} else { 
			lockLevel = 0;
			error = 10;
		}
		return psk;
	}
	
	ComplexOscillator nco;
	//RaisedCosineFilter idataFilter;
	//RaisedCosineFilter qdataFilter;
	IirFilter iFilter;
	IirFilter qFilter;
	//IirFilter iFilter2;
	//IirFilter qFilter2;
	IirFilter loopFilter;
	double iMix, qMix;
	double fi, fq;
	double freq; // this is the frequency of the carrier we tune to the side for ssb
	double costasLoopFreq; // this is the actual frequency NCO is tuned to for costas
	double error;
	double alpha = 0.1; //the feedback coeff  0 - 4.  But typical range is 0.01 and smaller.  
	double beta = 2048*alpha*alpha / 4.0d;  // alpha * alpha / 4 is critically damped. // 4096* seems to aggressive and can have long lock time
	double gamma = 0.04; //scan frequency rate when not locked 0.02 may not be strong enough to defeat false lock
	double ri, rq, lockLevel, avgLockLevel, sumLockLevel;
	public static final double LOCK_LEVEL_THRESHOLD = 10; // Depending on the signal the actual lock seems to vary from 18 to 50 or so.
	public static final int SCAN_RANGE = 2000	;
	
	public double getLockLevel() { return avgLockLevel; }
	public double getError() { return error; }
	public double getCostasFrequency() { return costasLoopFreq; }
	
	Complex c = new Complex(0,0);
	double lockGain = 1;
	private double costasLoop(double i, double q, int sample) {
		nco.nextSample(c);
		c.normalize();
		// Mix 
		iMix = i * c.geti() + q*c.getq();
		qMix = q * c.geti() - i*c.getq();
		// Filter
		fi = iFilter.filterDouble(iMix);
		fq = qFilter.filterDouble(qMix);;
		
		phasorData[2*sample] = fi*5;
		phasorData[2*sample+1] = fq*5;


		lockGain = gain;
		if (lockGain < 1) lockGain = 1;
		ri = SourceIQ.fullwaveRectify(fi*lockGain);
		rq = SourceIQ.fullwaveRectify(fq*lockGain);
		
		lockLevel = 1E0*(ri - rq);  // in lock state rq is close to zero.  In a false lock it is not.
		
		// Phase error
		error = fi*fq; 
		error = loopFilter.filterDouble(error);
		
		return fi;
	}
	
	public static double fullwaveRectify(double in) {
		if (in < 0) return -1*in;
		return in;
	}
	
	public double[] getPhasorData() {
			return phasorData;
	}
	
}
