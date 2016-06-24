package decoder;

import javax.sound.sampled.AudioFormat;

import org.jtransforms.fft.FloatFFT_1D;

import common.Config;
import common.Log;
import filter.DcRemoval;

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
	
	private int upstreamChannel = 0; // This is the audio channel that we read from the upstream audioSource
	private int channel = 0; // This is the audio channel where we store results - ALWAYS 0 for IQSource
	private boolean highSpeed = false;
	public static final int AF_SAMPLE_RATE = 48000;
	public AudioFormat upstreamAudioFormat;
//	public static final int READ_BUFFER_SIZE = 512 * 4; // about 5 ms at 48k sample rate;
	public int IQ_SAMPLE_RATE = 0;

	// FUDGE
	public static final int FFT_SAMPLES = 32*4096;
	//public static final int FFT_SAMPLES = 4096; //2048; //4096; //8192;
	/* The number of samples to read from the IQ source each time we request data */

	// FUDGE
	public static final int samplesToRead = 32*3840;
	//public static final int samplesToRead = 3840; //3840; // 1 bit at 192k is bytes_per_sample * bucket_size * 4, or 2 bits at 96000
		
	int decimationFactor = 4; // This is the IQ SAMPLE_RATE / decoder SAMPLE_RATE.  e.g. 192/48
	
	float[] fftData = new float[FFT_SAMPLES*2];
//	double[] fftFilter = new double[FFT_SAMPLES*2]; // this will hold the fft of the low pass filter and is the same length as FFT
//	double[] rawAudioI = new double[samplesToRead/2]; // NCO
//	double[] rawAudioQ = new double[samplesToRead/2]; // NCO
//	double[] rawAudio = new double[FFT_SAMPLES*2]; // NCO
	private float[] psd = new float[FFT_SAMPLES*2+1];;
	private float[] psdSum = new float[FFT_SAMPLES*2+1];;
	private float[] psdAvg = new float[FFT_SAMPLES*2+1];;
	int psdAvgCount = 0;
	int PSD_AVG_LEN = 5;
	float[] overlap = new float[2*FFT_SAMPLES - (samplesToRead/2)];
	
	byte[] outputData = new byte[samplesToRead/4];
	float[] fcdData = new float[samplesToRead];
	byte[] audioData = null;
	float[] demodAudio = new float[samplesToRead/4];

	int centerFreq; // The frequency that the dongle is set to
	
	float binBandwidth = 0;
	int filterWidth = 0 ; //We filter +- this number of bins 64 bins is 3000 Hz for 4096 FFT samples, Normal FM channel is 16kHz = +-8kHz = 170
		
	float[] blackmanWindow = new float[FFT_SAMPLES+1];
	float[] blackmanFilterShape;
	float[] tukeyFilterShape;
	
	FloatFFT_1D fft;
	FmDemodulator fm;

	// decimation filter params
	private static final int NZEROS = 5;
	private static final int NPOLES = 5;
	private double GAIN = 9.197583870e+02;
	private float[] xvi = new float[NZEROS+1];
	private float[] yvi = new float[NPOLES+1];
	private float[] xvq = new float[NZEROS+1];
	private float[] yvq = new float[NPOLES+1];

	// NCO filter params
//	private static double NCOGAIN = 9.197583870e+02;
//	private double[] nxvi = new double[NZEROS+1];
//	private double[] nyvi = new double[NPOLES+1];
//	private double[] nxvq = new double[NZEROS+1];
//	private double[] nyvq = new double[NPOLES+1];

	
	// DC balance filters
	DcRemoval audioDcFilter;
	DcRemoval iDcFilter;
	DcRemoval qDcFilter;
	
	//Filter userAudioFilter;
//	Filter audioFilterI;
//	Filter audioFilterQ;
	
	boolean fftDataFresh = false;
	public static boolean useNCO = false;
	public boolean offsetFFT = true;
	
	// Only needed for NCO
//	private static final int SINCOS_SIZE = 256;
//	private double[] sinTab = new double[SINCOS_SIZE];
//	private double[] cosTab = new double[SINCOS_SIZE];

	RfData rfData;
	
	public SourceIQ(int circularBufferSize, int chan, boolean hs) {
		super("IQ Source" + hs, circularBufferSize, chan);
		highSpeed = hs;
		channel = chan;
		audioFormat = makeAudioFormat();
		fft = new FloatFFT_1D(FFT_SAMPLES);
		fm = new FmDemodulator();
		blackmanWindow = initBlackmanWindow(FFT_SAMPLES);
		//initFftFilter();
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
	
	public RfData getRfData() {
		if (rfData != null) {
			rfData.calcAverages();
			return rfData; 
		}
//		Log.println("RF DATA NULL");
		return null;
	}
	
	public int getAudioBufferCapacity() { return upstreamAudioSource.circularBuffer[upstreamChannel].getCapacity(); }
	
	public int getFilterWidth() { return filterWidth; }
	public int getCenterFreqkHz() { return centerFreq; }
	public void setCenterFreqkHz(int freq) { 
		centerFreq = freq; 
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
		long delta = freq-centerFreq*1000 ;
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

	
	public float[] getPowerSpectralDensity() { 
		if (fftDataFresh==false) {
			return null;
		} else {
			
			fftDataFresh=false;
			return psdAvg;
		}
	}	

	protected int readUpstreamBytes(byte[] abData) {
		int nBytesRead = 0;
		Log.println("Reading IQ...");
		nBytesRead = upstreamAudioSource.readBytes(abData, upstreamChannel);	
		return nBytesRead;
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

	private void init() {	
		
		// The IQ Sample Rate is the same as the format for the upstream audio
		IQ_SAMPLE_RATE = (int)upstreamAudioFormat.getSampleRate();
		decimationFactor = IQ_SAMPLE_RATE / AF_SAMPLE_RATE;
		if (decimationFactor == 0) decimationFactor = 1;  // User has chosen the wrong rate most likely
		binBandwidth = IQ_SAMPLE_RATE/FFT_SAMPLES;
		
		if (highSpeed)
			filterWidth = (int) (9600*2/binBandwidth) ;
		else
			filterWidth = (int) (5000/binBandwidth) ;
		
/////////////// FUDGE - NEED TO WORK OUT WHY THE BANDWIDTH IS COMING OUT WRONG....		
		filterWidth = filterWidth*4;
	// filterWidth = (int) (200000/binBandwidth);
//		filterWidth = filterWidth*2;
		//decimationFactor = decimationFactor/2;
		blackmanFilterShape = initBlackmanWindow(filterWidth*2); 
		tukeyFilterShape = initTukeyWindow(filterWidth*2); 
		overlap = new float[2*FFT_SAMPLES - (samplesToRead/2)];
		
		outputData = new byte[samplesToRead/4];
		fcdData = new float[samplesToRead]; // this is the data block we read from the IQ source and pass to the FFT
		demodAudio = new float[samplesToRead/2];
/////////////// BIG FUDGE - not sure why doubling the length of the audio file helps here....
		audioData = new byte[2*samplesToRead/decimationFactor];  // we need the 2 because there are 4 bytes for each float and demod audio is samplesToRead/2
		
		Log.println("IQDecoder Samples to read: " + samplesToRead);
		Log.println("IQDecoder using FFT sized to: " + FFT_SAMPLES);
		Log.println("Decimation Factor: " + decimationFactor);

		audioDcFilter = new DcRemoval(0.9999f);

		iDcFilter = new DcRemoval(0.9999f);
		qDcFilter = new DcRemoval(0.9999f);
		rfData = new RfData(this);
		zeroPsdAvg();
	}
	
	
	boolean skippedOneByte = false;
	@Override
	public void run() {
		done = false;
		running = true;
		// Start reading data from the audio source.  This will store it in the circular buffer in the audio source
		startAudioThread();
		
		Log.println("IQ Source START. Running="+running);
		init();
		while (running) {
			int nBytesRead = 0;
			if (circularBuffer[channel].getCapacity() > fcdData.length) {
				//nBytesRead = upstreamAudioSource.readBytes(fcdData, upstreamChannel);
				// FUDGE
				nBytesRead = ((SourceUSB)upstreamAudioSource).readBytes(fcdData, upstreamChannel);
				if (nBytesRead != fcdData.length)
					Log.println("ERROR: IQ Source could not read sufficient bytes from audio source");
				outputData = processBytes(fcdData, false);
		////		Log.println("IQ Source writing data to audio thread");
				/** 
				 * Simulate a slower computer for testing
				 
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				fftDataFresh = true;
				for(int i=0; i < outputData.length; i+=4) {	
					if (i+4 > outputData.length) {
			//			Log.println("Only added: "+i +" of " + outputData.length);
						break;
					}
					circularBuffer[channel].add(outputData[i],outputData[i+1],outputData[i+2],outputData[i+3]);
				}
			} else {
				try {
					Thread.sleep(0,1);
				} catch (InterruptedException e) {
					e.printStackTrace(Log.getWriter());
				}
			}
		}

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
	byte[] ib = new byte[2];
	byte[] qb = new byte[2];

	/**
	 * Process IQ bytes and return a set of 48K audio bytes that can be processed by the decoder as normal
	 * We cache the read bytes in case we need to adjust for the clock
	 * If this is being called because the clock moved, we do not re-cache the data
	 * @param fcdData
	 * @return
	 */
	protected byte[] processBytes(float[] fcdData, boolean clockMove) {
		int dist = 0;
		if (offsetFFT) dist = (FFT_SAMPLES - SourceIQ.samplesToRead) /2;
		
		//int nBytesRead = fcdData.length;
		zeroFFT();
		int i = 0;
		
		// Loop through the 192k data, sample size 4
		for (int j=0; j < fcdData.length; j+=2 ) { // sample size is 4, 2 bytes per channel
			float id, qd;
			/*
			ib[0] = fcdData[j];
			ib[1] = fcdData[j+1];
			qb[0] = fcdData[j+2];  
			qb[1] = fcdData[j+3];
			if (upstreamAudioFormat.isBigEndian()) {
				id = (float) (Decoder.bigEndian2(ib, upstreamAudioFormat.getSampleSizeInBits())/ 32768.0);
				qd = (float) (Decoder.bigEndian2(qb, upstreamAudioFormat.getSampleSizeInBits())/ 32768.0);
			} else {
				id = (float) (Decoder.littleEndian2(ib, upstreamAudioFormat.getSampleSizeInBits())/ 32768.0);
				qd = (float) (Decoder.littleEndian2(qb, upstreamAudioFormat.getSampleSizeInBits())/ 32768.0);
			}
			 */
			id = fcdData[j];
			qd = fcdData[j+1];
			// filter out any DC from I/Q signals
			id = iDcFilter.filter(id);
			qd = qDcFilter.filter(qd);

			// i and q go into consecutive spaces in the complex FFT data input
			if (Config.swapIQ) {
				fftData[i+dist*2] = qd;
				fftData[i+1+dist*2] = id;
			} else {
				fftData[i+dist*2] = id;
				fftData[i+1+dist*2] = qd;
			}
			i+=2;
		}
		
		runFFT(fftData); // results back in fftData

		if (!Config.showIF) calcPsd();
		
		filterFFTWindow(fftData);
		if (Config.showIF) calcPsd();
		
		inverseFFT(fftData);

		int d=0;		
		// loop through the raw Audio array, which has 2 doubles for each entry - i and q
		for (int j=0; j < fcdData.length; j +=2 ) { // data size is 2 
				demodAudio[d++] = fm.demodulate(fftData[j+dist*2], fftData[j+1+dist*2]);			
		}
		
		int k = 0;
		// Filter any frequencies above 24kHz before we decimate to 48k. These are gentle
		// This is a balance.  Too much filtering impacts the 9600 bps decode, so we use a wider filter
		// These are gentle phase neutral IIR filters, so that we don't mess up the FM demodulation
		for (int t=0; t < 5; t++)
			if (highSpeed)
				antiAlias20kHzIIRFilter(demodAudio);
			else
				antiAlias16kHzIIRFilter(demodAudio);
		
		// Every 4th entry goes to the audio output to get us from 192k -> 48k
		for (int j=0; j < demodAudio.length; j+=decimationFactor ) { // data size is 1 decimate by factor of 4 to get to audio format size
			// scaling and conversion to integer
			float value = audioDcFilter.filter(demodAudio[j]); // remove DC.  Only need to do this to the values we want to keep
			int sample = (int) Math.round((value + 1.0) * 32767.5) - 32768;
			byte high = (byte) ((sample >> 8) & 0xFF);
			byte low = (byte) (sample & 0xFF);
			if (k + 3 >= audioData.length ) {
				//Log.println("k:" + k);
				break;
			}
			audioData[k] = low;
			audioData[k + 1] = high;
			// Put same in other stereo channel
			audioData[k +2] = low;
			audioData[k + 3] = high;
			k+=4;			
		 }
		return audioData; 
	}

	private void zeroPsdAvg() {
		//Log.println("ZERO PSD!!!!!!");
		for (int s=0; s<fftData.length-1; s+=2) {
			psdAvg[s/2] = 0;
			psdSum[s/2] = 0;
			psd[s/2] = 0;
		}
		psdAvgCount = 0;
	}
	private void zeroFFT() {
		for (int i=0; i< FFT_SAMPLES*2; i++) {
			fftData[i] = 0.0f;
		}
	}
	
	private void runFFT(float[] fftData) {
		
		// calculating the fft of the data, so we will have spectral power of each frequency component
		// fft resolution (number of bins) is samplesNum, because we initialized with that value
		//if (Config.applyBlackmanWindow)
		for (int s=0; s<fftData.length-1; s+=2) {
			fftData[s] = blackmanWindow[s/2] * fftData[s];
			fftData[s+1] = blackmanWindow[s/2] * fftData[s+1];
		}
		fft.complexForward(fftData);
	}

	private void calcPsd() {
		// Calculate power spectral density (PSD) so that we can display it
		// This is the magnitude of the complex signal, so it is sqrt(i^2 + q^2)
		// divided by the bin bandwidth  THIS IS NOT DONE CURRENTLY  - SO WHAT DOES IT MEAN?
		for (int s=0; s<fftData.length-1; s+=2) {
			psd[s/2] = psd(fftData[s], fftData[s+1]);
			
			if (psdAvgCount >= PSD_AVG_LEN) {
				psdSum[s/2] = psdSum[s/2]/(float)PSD_AVG_LEN;
				psdAvg[s/2] = psdSum[s/2];
				psdAvgCount = 0;
			} else {
				if (Float.isInfinite(psdSum[s/2]))
					psdSum[s/2] = psd[s/2]*psdAvgCount;
				psdSum[s/2] += psd[s/2];
			}
			psdAvgCount++;
		}

	}
	
	private float psd(float i, float q) {
		// 10*log (x^2 + y^2) == 20 * log(sqrt(x^2 + y^2)) so we can avoid the sqrt
		return (float) (20*Math.log10(Math.sqrt((i*i) + (q*q))/binBandwidth));	// Compute PSD
		//psd[s/2] = 10*Math.log10( ((fftData[s]*fftData[s]) + (fftData[s+1]*fftData[s+1]) )/binBandwidth);	// Compute PSD
		
	}
	
	private void inverseFFT(float[] fftData) {
//		int dist = 0;
//		if (offsetFFT) dist = (FFT_SAMPLES - SourceIQ.samplesToRead) /2;
		fft.complexInverse(fftData, true); // true means apply scaling - but it does not work?
	
		// Add the last overlap.  
		// We have iq data, so the FFTData buffer is actually FFT_SAMPLES * 2 in length
		// We request samplesToRead bytes from readBytes, it grabs 4 * samplesToRead from the source because we
		// That data becomes 2 iq samples
		int overlapLength = 2*FFT_SAMPLES - samplesToRead;
		//int overlapLength = dist;
		for (int o=0; o < overlapLength; o++) {
			fftData[o] += overlap[o];
		}

		// capture this overlap
		for (int o=2*FFT_SAMPLES-overlapLength; o < 2*FFT_SAMPLES; o++) {
			overlap[o-(2*FFT_SAMPLES-overlapLength)] = fftData[o];
		}

	}

	float[] newData = new float[fftData.length]; // make a new array to copy so we can store the section we want
	/**
	 * Apply a Tukey or blackman window to the segment of the spectrum that we want, then move the selected frequency to zero
	 * We have selected a bin.  This is the FFT bin from 0 to FFT_SAMPLES.  It could be positive or negative.
	 * 
	 * We use the window so that we do not have the high frequency effects from a square window.  These will alias
	 * due to the gibbs effect and cause issues.  However, we are also limited because we can only make a course move otherwise phase
	 * discontinuities give us issues:
	 * 
	 * We also calculate the RF Measurements, the bin of the strongest signal in the pass band is set as the peak Signal, the average signal
	 * in the passband and the noise in an equivalent bandwidth just outside the pass band is also measured so we can calculate the RF SNR
	 *  
	 * @param fftData
	 */
	private void filterFFTWindow(float[] fftData) {
		for (int z=0; z<fftData.length; z++) newData[z] = 0.0f;
		
		int peakSignalBin = 0;  // the peak signal bin
		float peak = -999999f;
		float strongest = -999999f;
		int strongestBin = 0;
		
		// We break the spectrum into chunks, so that we pass through it only once, but can analyze the different sections in different ways
		// 0 - noiseStart
		// noiseStart to start of pass band
		// the pass band
		// pass band to noiseEnd
		// noiseEnd to end of spectrum
		
		float noise = 0f;
		float avgSig = 0f;
		int noiseReading = 0;
		int sigReading = 0;
		int binIndex = Config.selectedBin*2; // multiply by two because each bin has a real and imaginary part
		int filterBins = filterWidth*2;

		int start = binIndex - filterBins;
		if (start < 0) start = 0;
		int end = binIndex + filterBins;
		if (end > fftData.length-2) end = fftData.length-2;
		// Selected frequencies part
		int dcOffset=0; // must be even
		int k=dcOffset;
		float iAvg = 0;
		float qAvg = 0;
		
		int noiseStart = start - filterBins;
		if (noiseStart < 0 ) noiseStart = 0;
		int noiseEnd = end + filterBins;
		if (noiseEnd > fftData.length-2) noiseEnd = fftData.length - 2;
		
		// Here we start the analysis of the spectrum

		float sig = 0;
		for (int n=0; n < noiseStart; n+=2) {
			if (Config.fromBin*2 < n && n < Config.toBin*2) {
				sig = psd(fftData[n], fftData[n+1]);
				if (sig > strongest) {
					strongest = sig;
					strongestBin = n/2;
				}
			}
		}

		for (int n=noiseStart; n< start; n+=2) {
			sig = psd(fftData[n], fftData[n+1]);
			if (Config.fromBin*2 < n && n < Config.toBin*2) {
				if (sig > strongest) {
					strongest = sig;
					strongestBin = n/2;
				}
			}			
			noise += sig;
			noiseReading++;
		}
		for (int n=end; n < noiseEnd; n+=2) {
			sig = psd(fftData[n], fftData[n+1]);
			if (Config.fromBin*2 < n && n < Config.toBin*2) {

				if (sig > strongest) {
					strongest = sig;
					strongestBin = n/2;
				}
			}			
			noise += sig;
			noiseReading++;
		}

		for (int n=noiseEnd; n < fftData.length-2; n+=2) {
			if (Config.fromBin*2 < n && n < Config.toBin*2) {

				sig = psd(fftData[n], fftData[n+1]);
				if (sig > strongest) {
					strongest = sig;
					strongestBin = n/2;
				}
			}
		}

		for (int i = start; i <= end; i+=2) {
			if (Config.applyBlackmanWindow) {
				newData[k] = fftData[i] * Math.abs(blackmanFilterShape[k/2-dcOffset/2]) ;
				newData[k+1] = fftData[i + 1] * Math.abs(blackmanFilterShape[k/2-dcOffset/2]);	
			} else {
				newData[k] = fftData[i] * Math.abs(tukeyFilterShape[k/2-dcOffset/2]) ;
				newData[k+1] = fftData[i + 1] * Math.abs(tukeyFilterShape[k/2-dcOffset/2]);
			}
			sig = psd(fftData[i], fftData[i+1]);
			if (Config.fromBin*2 < i && i < Config.toBin*2) {
				if (sig > strongest) {
					strongest = sig;
					strongestBin = i/2;
				}
			}
			
			if (sig > peak) { 
				peak = sig;
				peakSignalBin = i/2;
			}
			avgSig += sig;
			sigReading++;

			iAvg = newData[k];
			qAvg = newData[k+1];
			k+=2;
		}
		
//		fftData[0] = 0;
//		fftData[1] = 0;
		// Write the DC bins - seem to need this for the blackman filter
		if (Config.applyBlackmanWindow) {
			newData[0] = iAvg / (filterBins * 2);
			newData[1] = qAvg / (filterBins * 2);
		}
		avgSig = avgSig / (float)sigReading;
		noise = noise / (float)noiseReading;
		//Log.println("Sig: " + avgSig + " from " + sigReading + " Noise: " + noise + " from readings: " + noiseReading);
		//Log.println("peak bin: " + peakSignalBin);
		// store the peak signal
		rfData.setPeakSignal(peak, peakSignalBin, avgSig, noise);

		// store the strongest sigs
		rfData.setStrongestSignal(strongest, strongestBin);
		
		for (int i = 0; i < fftData.length; i+=1) {
			fftData[i] = newData[i];			
		}

		//return newData;
	}

	/*
	private int getRequiredBin(int bin) {
		double binBW = 192000f / 2 / SourceIQ.FFT_SAMPLES; // nyquist freq / fft length
		double freq = (double)Config.selectedBin * binBW;
		
		//int actBin = (int) (Math.round(SourceIQ.FFT_SAMPLES * freq / (30 * 192000)) * 30);
		int actBin = (int) (Math.round(bin / 30) * 30 - 1) ;
		return actBin;
	}
	 */
	
	/*
	private double[] rotateFFT(double[] fftData) {
		double[] newData = new double[fftData.length]; // make a new array to copy so we can store the section we want
		
		//int binIndex = (int) (Math.round(Config.selectedBin / 30) * 30) *2; // multiply by two because each bin has a real and imaginary part
		int binIndex = Config.selectedBin *2;
		
		// We move start to zero and rotate everything else
		for (int i = 0; i < fftData.length; i+=1) {
			int newIdx = (i - binIndex);
			newIdx = newIdx % fftData.length;
			if (newIdx < 0) newIdx += fftData.length; // Java Modulus has the sign of the dividend so need to correct for that
			newData[newIdx] = fftData[i];
			
		}	
		// Write the DC bins
//		newData[0] = iAvg / (filterBins * 2);
//		newData[1] = qAvg / (filterBins * 2);
			
		return newData;
	}
	 */
	
	/**
	 * A simple window of the data introduces phase noise.  Instead I do the following:
	 * - calculate a Windowed Sinc Low pass filter for the desired bandwidth
	 * - take the kernal and put into a buffer with zero padding so its the same length as my FFT
	 * - fft the filter kernal to produce a complex spectrum with mag and phase
	 * - run fft on my audio data
	 * - rotate the fft so that the center frequency is at zero - calculate V from the length of the FFT and the length of the filter kernal so that we can rotate
	 *   by an amount that does not cause a phase issue.
	 * - multiply (complex) by the fft'd filter kernal
	 * This result should be the low pass filtered spectrum.  For testing I can toggle the spectrum on and off at each stage - the rotation, apply the filter,
	 * just the filter kernal etc
	 * After the multiplication we run the iFFT and get the audio back that we want.  We use the overlap add of course.
	 * @param fftData
	 * @return
	 */
	/*
	@SuppressWarnings("unused")
	private double[] fftFilter(double[] fftData) {
		// We need to multiply so that Real = Re*ReFilter - Img * ImgFilter
		// Img = Re*ImgFilter + Img*RealFilter
		
		// The results of the Complex FFT are alternating values of Real/Img
		for (int i=0; i<fftData.length; i+=2) {
			double temp = fftData[i] * fftFilter[i] - fftData[i+1] * fftFilter[i+1];
			fftData[i+1] = fftData[i] * fftFilter[i+1] + fftData[i+1] * fftFilter[i];
			fftData[i] = temp;
		}
		return fftData;
	}
	*/
	/*
	private void initFftFilter() {
		Filter lpf = new WindowedSincFilter();
		lpf.init(IQ_SAMPLE_RATE, 3000, 128);
		//lpf.init(192000, 8000, 128);
		double[] kernal = lpf.getKernal();
		for (int i=0; i<kernal.length; i++)
			fftFilter[i] = kernal[i];
		fft.complexForward(fftFilter);
	}
	*/
	
	@SuppressWarnings("unused")
	private void antiAlias12kHzIIRFilter(float[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// Given we only care about frequencies much lower, we can be more aggressive with the cutoff
		// and use a shorter filter, so we choose 12kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		float GAIN = 9.197583870e+02f;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
			xvi[5] = rawAudioData[j] / GAIN;
			yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
			yvi[5] =   (xvi[0] + xvi[5]) + 5f * (xvi[1] + xvi[4]) + 10f * (xvi[2] + xvi[3])
					+ (  0.0884067190f * yvi[0]) + ( -0.6658412244f * yvi[1])
					+ (  2.0688801399f * yvi[2]) + ( -3.3337619982f * yvi[3])
					+ (  2.8075246179f * yvi[4]);
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
			xvq[5] = rawAudioData[j+1] / GAIN;
			yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
			yvq[5] =   (xvq[0] + xvq[5]) + 5f * (xvq[1] + xvq[4]) + 10f * (xvq[2] + xvq[3])
					+ (  0.0884067190f * yvq[0]) + ( -0.6658412244f * yvq[1])
					+ (  2.0688801399f * yvq[2]) + ( -3.3337619982f * yvq[3])
					+ (  2.8075246179f * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}
	private void antiAlias16kHzIIRFilter(float[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// To avoid aliases, we filter at less than 24kHz, but we need to filter higher than 9600 * 2.  So we pick 20kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		float GAIN = 3.006238283e+02f;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
	        xvi[5] = rawAudioData[j] / GAIN;
	        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
	        yvi[5] =   (xvi[0] + xvi[5]) + 5f * (xvi[1] + xvi[4]) + 10f * (xvi[2] + xvi[3])
	                     + (  0.0394215023f * yvi[0]) + ( -0.3293736653f * yvi[1])
	                     + (  1.1566456875f * yvi[2]) + ( -2.1603685744f * yvi[3])
	                     + (  2.1872297286f * yvi[4]);
	        
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
	        xvq[5] = rawAudioData[j+1] / GAIN;
	        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
	        yvq[5] =   (xvq[0] + xvq[5]) + 5f * (xvq[1] + xvq[4]) + 10f * (xvq[2] + xvq[3])
	                     + (  0.0394215023f * yvq[0]) + ( -0.3293736653f * yvq[1])
	                     + (  1.1566456875f * yvq[2]) + ( -2.1603685744f * yvq[3])
	                     + (  2.1872297286f * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}

	private void antiAlias20kHzIIRFilter(float[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// To avoid aliases, we filter at less than 24kHz, but we need to filter higher than 9600 * 2.  So we pick 20kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		float GAIN = 1.328001690e+02f;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
	        xvi[5] = rawAudioData[j] / GAIN;
	        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
	        yvi[5] =   (xvi[0] + xvi[5]) + 5f * (xvi[1] + xvi[4]) + 10f * (xvi[2] + xvi[3])
	                     + (  0.0175819825f * yvi[0]) + ( -0.1605530625f * yvi[1])
	                     + (  0.6276667742f * yvi[2]) + ( -1.3438919231f * yvi[3])
	                     + (  1.6182326801f * yvi[4]);
	        
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
	        xvq[5] = rawAudioData[j+1] / GAIN;
	        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
	        yvq[5] =   (xvq[0] + xvq[5]) + 5f * (xvq[1] + xvq[4]) + 10f * (xvq[2] + xvq[3])
	                     + (  0.0175819825f * yvq[0]) + ( -0.1605530625f * yvq[1])
	                     + (  0.6276667742f * yvq[2]) + ( -1.3438919231f * yvq[3])
	                     + (  1.6182326801f * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}

	@SuppressWarnings("unused")
	private void antiAlias24kHzIIRFilter(float[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		float GAIN = 7.052142314e+01f;
		for (int j=0; j < rawAudioData.length; j+=2 ) {
			xvi[0] = xvi[1]; xvi[1] = xvi[2]; xvi[2] = xvi[3]; xvi[3] = xvi[4]; xvi[4] = xvi[5]; 
	        xvi[5] = rawAudioData[j] / GAIN;
	        yvi[0] = yvi[1]; yvi[1] = yvi[2]; yvi[2] = yvi[3]; yvi[3] = yvi[4]; yvi[4] = yvi[5]; 
	        yvi[5] =   (xvi[0] + xvi[5]) + 5f * (xvi[1] + xvi[4]) + 10f * (xvi[2] + xvi[3])
	                     + (  0.0078245894f * yvi[0]) + ( -0.0773061573f * yvi[1])
	                     + (  0.3292913096f * yvi[2]) + ( -0.8090163636f * yvi[3])
	                     + (  1.0954437995f * yvi[4]);
	        
			rawAudioData[j] = yvi[5];

			xvq[0] = xvq[1]; xvq[1] = xvq[2]; xvq[2] = xvq[3]; xvq[3] = xvq[4]; xvq[4] = xvq[5]; 
	        xvq[5] = rawAudioData[j+1] / GAIN;
	        yvq[0] = yvq[1]; yvq[1] = yvq[2]; yvq[2] = yvq[3]; yvq[3] = yvq[4]; yvq[4] = yvq[5]; 
	        yvq[5] =   (xvq[0] + xvq[5]) + 5f * (xvq[1] + xvq[4]) + 10f * (xvq[2] + xvq[3])
	                     + (  0.0078245894f * yvq[0]) + ( -0.0773061573f * yvq[1])
	                     + (  0.3292913096f * yvq[2]) + ( -0.8090163636f * yvq[3])
	                     + (  1.0954437995f * yvq[4]);
			rawAudioData[j+1] = yvq[5];

		}
		
	}
/*
	private void antiAlias24kHzIIRFilterMono(double[] rawAudioData) {
		// Designed at www-users.cs.york.ac.uk/~fisher
		// Need to filter the data before we decimate
		// We decimate by factor of 4, so need to filter at 192/8 = 24kHz
		// We use a Bessel IIR because it has linear phase response and we don't need to do overlap/add
		//double[] out = new double[rawAudioData.length];
		NCOGAIN = 7.052142314e+01;
		for (int j=0; j < rawAudioData.length; j+=1 ) {
			nxvi[0] = nxvi[1]; nxvi[1] = nxvi[2]; nxvi[2] = nxvi[3]; nxvi[3] = nxvi[4]; nxvi[4] = nxvi[5]; 
	        nxvi[5] = rawAudioData[j] / NCOGAIN;
	        nyvi[0] = nyvi[1]; nyvi[1] = nyvi[2]; nyvi[2] = nyvi[3]; nyvi[3] = nyvi[4]; nyvi[4] = nyvi[5]; 
	        nyvi[5] =   (nxvi[0] + nxvi[5]) + 5 * (nxvi[1] + nxvi[4]) + 10 * (nxvi[2] + nxvi[3])
	                     + (  0.0078245894 * nyvi[0]) + ( -0.0773061573 * nyvi[1])
	                     + (  0.3292913096 * nyvi[2]) + ( -0.8090163636 * nyvi[3])
	                     + (  1.0954437995 * nyvi[4]);
	        
			rawAudioData[j] = nyvi[5];

		}
		
	}
*/
	private float[] initBlackmanWindow(int len) {
		float[] blackmanWindow = new float[len+1];
		for (int i=0; i <= len; i ++) {
			blackmanWindow[i] = (float) (0.42 - 0.5 * Math.cos(2 * Math.PI * i / len) + 0.08 * Math.cos((4 * Math.PI * i) / len));
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
	private float[] initTukeyWindow(int len) {
		float[] window = new float[len+1];
		float alpha = 0.5f;
		int lowerLimit = (int) ((alpha*(len-1))/2);
		int upperLimit = (int) ((len -1) * ( 1 - alpha/2));
		
		for (int i=0; i < lowerLimit; i ++) {
			window[i] = (float) (0.5 * (1 + Math.cos(Math.PI*( 2*i/(alpha*(len-1)) -1))));
		}
		for (int i=lowerLimit; i < upperLimit; i ++) {
			window[i] = 1;
		}
		for (int i=upperLimit; i < len; i ++) {
			window[i] = (float) (0.5 * (1 + Math.cos(Math.PI*( 2*i/(alpha*(len-1)) - (2/alpha)+1))));
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

	/*
	private double iPhase = 0.0;
	private double ncoMixerI(double i, double q) {
		
		double inc = 2.0*Math.PI*Config.selectedBin/FFT_SAMPLES; //getFrequencyFromBin()/192000;
		iPhase+=inc;
		if (iPhase>2.0*Math.PI)
			iPhase-=2.0*Math.PI;
		// mix with input signal (unless negative)
		if (iPhase>0.0) {
			double mi=i*cosTab[(int)(iPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE] +
					q*sinTab[(int)(iPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
			return mi; //cosTab[(int)(iPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
		} else {
			return i;
		}
	}

	private double qPhase = 0.0;
	private double ncoMixerQ(double i, double q) {
		
		double inc = 2.0*Math.PI*Config.selectedBin/FFT_SAMPLES; //getFrequencyFromBin()/192000;
		qPhase+=inc;
		if (qPhase>2.0*Math.PI)
			qPhase-=2.0*Math.PI;
		// mix with input signal (unless negative)
		if (qPhase>0.0) {
			double mq=q*cosTab[(int)(iPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE] -
					i*sinTab[(int)(qPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
			return mq; //sinTab[(int)(qPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
		} else {
			return q;
		}
	}
*/
}
