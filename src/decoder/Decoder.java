package decoder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import measure.RtMeasurement;
import measure.SatMeasurementStore;
import measure.SatPc32DDE;
import common.Config;
import common.Log;
import common.Performance;
import common.Spacecraft;
import filter.Filter;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.FramePart;
import telemetry.Header;
import uk.me.g4dpz.satellite.SatPos;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
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
 * 
 * A decoder that can be started from the GUI or from the command line.  This is an abstract class, so one of its children
 * is instantiated:
 * WavDecoder - for test wav files
 * SoundCardDecoder - to read bytes from the sound card
 * IQDecoder - to read bytes from an SDR or IQ receiver, with I and Q on the two stereo channels
 * BitFileDecoder - test decoder to read text files that contain 10b words.  The file must contain a complete frame
 * 
 * This class contains the methods needed to convert the received bytes into integers, filter the received audio,
 * and sample the audio at the bit threshold.  This can be configured for either high speed or slow speed telemetry.
 * 
 * The sampled bits are passed to the BitStream which holds all of the logic needed to find the start and end of
 * frames and to decode the 10 bit words into 8 bit bytes. 
 *  
 * 
 * @author chris.e.thompson
 *
 */
public abstract class Decoder implements Runnable {
	public String name = "";
	// This is the audio source that holds incoming audio.  It contains a circularByte buffer and the decoder reads from it.  The decoder does
	// not care where the audio comes from or how it gets into the buffer
	protected SourceAudio audioSource;
	protected Thread audioReadThread;
	int audioChannel = 0; // this is the audio channel that we read from the upstream audioSource
	
	protected int BITS_PER_SECOND = 0;
	public boolean flipReceivedBits;	
	private boolean bigEndian = false;
	private int bitsPerSample = 16;
	protected int bytesPerSample = 4; // 4 bytes for stereo.  Updated by the program if file is mono
	private int channels = 2; // one for mono two for stereo
	boolean stereo; // true if both audio channels are used.  Otherwise the source is mono
	public static final int MONO = 1;
	public static final int STEREO = 2;
	protected int currentSampleRate = 48000;
	
	protected int bucketSize = 0; // Must be initialized based on the sample rate and bits per second

	/**
	 * The parameters below can be used to tune the decoder
	 *
	 * sample window length determines how many bit buckets we average over to judge the levels and to move the clock.
	 * 10 is too short, but helpful sometimes for debugging. 128 is too long and is over half a second of input
	 */
	protected int SAMPLE_WINDOW_LENGTH = 70; //64; //64; //70; // This is the number of buckets to grab each time we pull data from the stream
	protected int SAMPLE_WIDTH_PERCENT = 10; //use 1 for 736R audio ***** 10; // 10% is reasonable.  Increase to this needs decrease to BIT_DISTANCE_THRESHOLD_PERCENT 
	protected int SAMPLE_WIDTH = 0; // sample +- this number and average them.  Set when initialized
	protected int CLOCK_TOLERANCE = 10; // use 4 for 736R audio *****10; // The BUCKET_SIZE is divided by this to give a tolerance at the start of the window
	protected int CLOCK_REOVERY_ZERO_THRESHOLD = 20; // but updated to 10 for highspeed in the code
	public static final int MAX_VOLUME = 32000;
	public static final int MIN_VOLUME = 600;// Use low value for 736R audio e.g. 100 ***** 600;
	public static final int MIN_BIT_SNR = 2;// Above this threshold we unsquelch the audio
														
	protected int BUFFER_SIZE = 0; // * 4 for sample size of 2 bytes and both channels
	private double[] abBufferDouble;
	private double[] abBufferDoubleFiltered; 
	
	protected boolean dataFresh = false; // true if we have just written new data for the GUI to read
	
	
    protected int[][] dataValues = null;
    public EyeData eyeData;
    private int[] maxValue = null;
    private int[] minValue = null;
    private int[] firstZero = null;
    private int[] secondZero = null;
    public boolean[] middleSample = null; // The sampled bit for the entire bucket
    
    /**
     * This holds the stream of bits that we have not decoded. Once we have several
     * SYNC words, this is flushed of processed bits.
     */
    protected BitStream bitStream = null;  // Hold bits until we turn them into decoded frames
    
    protected int averageMax;
    protected int averageMin;
    protected int zeroValue;
	
    protected int sampleNumber = 0;;
   // private int windowNumber = 0;
    
    protected int framesDecoded = 0;

    private boolean squelch = true;
    private boolean tooLoud = false;
    private boolean clockLocked = false; // set to true when we have decoded a frame. Not reset until decode fails
    private boolean processing = true; // called to end the thread from the GUI
    private boolean done = true; // true when we have dropped out of the loop or when not running the loop
    
    protected Filter filter = null;
    public Filter monitorFilter = null;
    
    private SinkAudio sink = null; // The audio output device that we copy bytes to if we are monitoring the audio
    private boolean monitorAudio = false; // true if we are monitoring the audio
    
    /**
     * Given an audio source, decode the data in it,
     */
	public Decoder(String n, SourceAudio as, int chan) {
		name = n;
		audioSource = as;
		audioChannel = chan;
		AudioFormat audioFormat = as.getAudioFormat();
		currentSampleRate = (int) audioFormat.getSampleRate();
		bitsPerSample = audioFormat.getSampleSizeInBits();
        bigEndian = audioFormat.isBigEndian();
        channels = audioFormat.getChannels();
        bytesPerSample = audioFormat.getFrameSize();
        init();
	}
	
	public AudioFormat getAudioFormat() { return audioSource.getAudioFormat(); }
	public int getCurrentSampleRate() { return currentSampleRate; }
	public int getFramesDecoded() { return framesDecoded; }
	public int getBitsPerSample() { return bitsPerSample; }
	public int getSampleWindowLength() { return SAMPLE_WINDOW_LENGTH; }
	
	
	abstract protected void init();
	abstract protected void sampleBuckets(); // the routine that samples the buckets and caches the binary data for analysis by clock recovery
	abstract protected int recoverClockOffset(); // the routine that calculates how many samples to pull the audio so that the clock is aligned
	abstract protected void processBitsWindow(); // add the bits to the bitStream and process them into frames
	public Filter getFilter() { return filter; }
	public boolean getBigEndian() { return bigEndian; }
	public int getBucketSize() { return bucketSize; }
	/**
	 * This is called each time the decoder is started from the GUI
	 */
	
	/* now in FoxDecoder
	public void init() {
		Performance.setEnabled(Config.debugPerformance);  // enable performance logging (or not)
		
		processing = true;
		done = false;
		
		BUFFER_SIZE = SAMPLE_WINDOW_LENGTH * bucketSize;

		// Timing for each loop in milli seconds
		OPTIMAL_TIME = 500*SAMPLE_WINDOW_LENGTH/BITS_PER_SECOND;
		
		Log.println("Decoder using sample rate: " + currentSampleRate);
		Log.println("Decoder using bucketSize: " + bucketSize);
		Log.println("Decoder using SAMPLE_WIDTH: " + SAMPLE_WIDTH);
		Log.println("Decoder using BUFFER_SIZE: " + BUFFER_SIZE);
		Log.println("Decoder using BITS_PER_SECOND: " + BITS_PER_SECOND);
		Log.println("Decoder CHANNELS: " + channels);
		
		initWindowData();
//		agcFilter = new AGCFilter();
		monitorFilter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE);
		monitorFilter.init(currentSampleRate, 3000, 256);
	}
    */
	/**
	 * This is once when the decoder is started
	 */
	protected void initWindowData() {

		dataValues = new int[SAMPLE_WINDOW_LENGTH][];

		maxValue = new int[SAMPLE_WINDOW_LENGTH];
		minValue = new int[SAMPLE_WINDOW_LENGTH];
		firstZero = new int[SAMPLE_WINDOW_LENGTH];
		secondZero = new int[SAMPLE_WINDOW_LENGTH];
		middleSample = new boolean[SAMPLE_WINDOW_LENGTH];

		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			dataValues[i] = new int[bucketSize];
		}
		eyeData = new EyeData(SAMPLE_WINDOW_LENGTH,bucketSize);
	}

	/**
	 * We want to avoid calling new in the audio loop, in case it stalls.  So we zero out
	 * the storage manually each time we go around the audio loop
	 */
	protected void resetWindowData() {

		//Log.println("Reset data");
		for (int j=0; j< SAMPLE_WINDOW_LENGTH; j++) {

			for (int i=0; i < bucketSize; i++) {
				dataValues[j][i] = 0;
			 }	
			maxValue[j] = 0;// new int[SAMPLE_WINDOW_LENGTH];
			minValue[j] = 0; //= new int[SAMPLE_WINDOW_LENGTH];
			firstZero[j] = 0; //= new int[SAMPLE_WINDOW_LENGTH];
			secondZero[j] = 0; //= new int[SAMPLE_WINDOW_LENGTH];
			middleSample[j] = false; // = new boolean[SAMPLE_WINDOW_LENGTH];
		}
		
	}

	public void stopProcessing() {
		cleanup();
		processing = false;
		Log.println("DECODER STOPPING");
	}
		
	public boolean isDone() { return done; }

	/**
	 * Called when the decoder is started.  This tells the audio source to begin reading data.
	 * We only do this if it is decoder 0, because the other deoder reads from the same source, unless
	 * this is an IQDecoder, in which case it needs to be started in all cases
	 */
	protected void startAudioThread() {
		if (audioChannel == 0 || audioSource instanceof SourceIQ) {
			if (audioReadThread != null) { 
				audioSource.stop(); 
			}	

			audioReadThread = new Thread(audioSource);
			audioReadThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
			audioReadThread.start();
		}
		
	}

	
	//public static int getSampleRate() { return Config.currentSampleRate; }
	
	public boolean toggleAudioMonitor(SinkAudio s, boolean monitorFiltered, int position) throws LineUnavailableException {
		
		sink = s;
		Config.monitorFilteredAudio = monitorFiltered;
		monitorAudio = !monitorAudio;
		if (!monitorAudio) {
			sink.closeOutput();
			sink = null;
		} else {
			sink.setDevice(position);
		}
		return monitorAudio;
	}
	

	public void stopAudioMonitor() throws LineUnavailableException {
		if (sink != null) { // then we have something to stop
			if (monitorAudio == true) {
				monitorAudio = false;

			}
			sink.closeOutput();
		}
		sink = null;
		
	}

	public void setMonitorAudio(SinkAudio s, boolean m, int position) throws IllegalArgumentException, LineUnavailableException { 
		if (sink == null) { // then we are setting this for the first time
			sink = s;
			monitorAudio = m; 
			if (monitorAudio) {
				sink.setDevice(position);
			}
		} else { // we might be changing it or closing it
			sink = s;
			monitorAudio = m; 
			if (!monitorAudio) {
				sink.closeOutput();
				sink = null;
			}
		}
}
	
//	public void setMonitorFiltered(boolean m) { monitorFiltered = m; }

	public int getAudioBufferSize() { return audioSource.getAudioBufferSize(); }
	public int getAudioBufferCapacity() { return audioSource.getAudioBufferCapacity(); }
	
	public double[] getAudioData() {
		if (dataFresh==false) {
			return null;
		} else {
			dataFresh=false;
			return abBufferDouble;
		}
	}
	public double[] getFilteredData() {
		if (dataFresh==false) 
			return null;
		else {
			dataFresh=false;
			return abBufferDoubleFiltered;
		}
	}
	
	public int getZeroValue() {
		return zeroValue;
	}
	
	public EyeData getEyeData() {
		if (eyeData != null)
			if (eyeData.isFresh()) {
				eyeData.setFreshData(false);
				return eyeData;
			} 
		return null;
	}
	
	public boolean isSoundCardDecoder() { return false; }
	public boolean isSoundWavDecoder() { return false; }
	
	
	@Override
	public void run() {
		Log.println("DECODER Start");
		try {
			process();
		} catch (UnsupportedAudioFileException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"ERROR",
					JOptionPane.ERROR_MESSAGE) ;
		}catch (IOException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"ERROR",
					JOptionPane.ERROR_MESSAGE) ;	
		} 
		
		catch (NullPointerException e) {
			// ONLY CATCH THIS IN PRODUCTION VERSION	
			/*
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"FATAL ERROR IN DECODER",
				    JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace();
			e.printStackTrace(Log.getWriter());
			*/
			e.printStackTrace(Log.getWriter());
	    	StringWriter sw = new StringWriter();
	    	PrintWriter pw = new PrintWriter(sw);
	    	e.printStackTrace(pw);
	        Log.errorDialog("FATAL ERROR IN DECODER", "Uncaught exception.  You probablly need to restart FoxTelem:\n" + sw.toString());
		}
		Log.println("DECODER Exit");
	}
	
	protected int read(double[] abData) {
		int nBytesRead = 0;
		//Log.println("Reading bytes from channel: " + audioChannel);
		nBytesRead = audioSource.read(abData, audioChannel);	
		return nBytesRead;
	}

	
	public void process() throws UnsupportedAudioFileException, IOException {
		Log.println("Decoder using sample rate: " + currentSampleRate);
		Log.println("Decoder using bucketSize: " + bucketSize);
		Log.println("Decoder using SAMPLE_WIDTH: " + SAMPLE_WIDTH);
		Log.println("Decoder using BUFFER_SIZE: " + BUFFER_SIZE);
		Log.println("Decoder using BITS_PER_SECOND: " + BITS_PER_SECOND);
		Log.println("Decoder CHANNELS: " + channels);
		stereo = true;
		if (audioSource.audioFormat.getChannels() == Decoder.MONO) stereo = false;
        int nBytesRead = 0;
 		abBufferDouble = new double[BUFFER_SIZE];
		abBufferDoubleFiltered = new double[BUFFER_SIZE];
		
 /*       if (Config.writeDebugWavFile) {
        	WavDecoder.initDebugWav();
        	allabData = new byte[BUFFER_SIZE*600];  // Debug Wav file storage.  This is a crazy approach only used for debug
        }
 */
        if (Config.filterData)
        	//if (this instanceof Fox9600bpsDecoder) 
        	//	Log.println("FILTER: none");
        	//else
        	Log.println("FILTER: " + filter.toString()); //filters[Config.useFilterNumber].toString());
        Log.println("BUFFER: " + BUFFER_SIZE);
        //Log.println("DECODING FRAMES LENGTH " + bitStream.SYNC_WORD_DISTANCE + " bits ... ");
        
		startAudioThread();

        while (nBytesRead != -1 && processing) {
        	Performance.startTimer("Setup");
    		resetWindowData();
    		Performance.endTimer("Setup");
        	if (nBytesRead >= 0) {
        		Performance.startTimer("Read");
                nBytesRead = read(abBufferDouble);
                if (monitorAudio && !squelch && !Config.monitorFilteredAudio) {
                	if (sink != null)
                		sink.write(abBufferDouble);
                }
                if (Config.debugBytes) 
                	if (nBytesRead != abBufferDouble.length) Log.println("ERROR: COULD NOT READ FULL BUFFER");
        		Performance.endTimer("Read");
        		Performance.startTimer("Filter");

                if (Config.filterData) { // && !Config.highSpeed) {
                	//SourceAudio.getDoublesFromBytes(abData, stereo, abBufferDouble);
                	/**
                	 * Note that the filter converts the byte values to doubles and then back, so that
                	 * we can play the filtered audio back to the user if requested.
                	 */
                	//filteredData = filter.filter(abData); //filters[Config.useFilterNumber].filter(abData);
                	filter.filter(abBufferDouble, abBufferDoubleFiltered);
                	//SourceAudio.getBytesFromDoubles(abBufferDoubleFiltered, abBufferDoubleFiltered.length, stereo, filteredData);
                } else
                	abBufferDoubleFiltered = abBufferDouble;
                if (Config.filterOutputAudio) {
//                	monitorFilter.filter(abBufferDouble, abBufferDouble);
 //               	SourceAudio.getBytesFromDoubles(abBufferDouble, abBufferDouble.length, stereo, abData);
                }
                Performance.endTimer("Filter");
                Performance.startTimer("Monitor");

                if (monitorAudio && !squelch && Config.monitorFilteredAudio) {
        			sink.write(abBufferDoubleFiltered);
                }
                Performance.endTimer("Monitor");
                Performance.startTimer("Bucket");

                bucketData(abBufferDoubleFiltered);
                Performance.endTimer("Bucket");

        	}


        	Performance.startTimer("Sample");
        	sampleBuckets();
        	Performance.endTimer("Sample");

        	Performance.startTimer("ClockSync");

        	if (!clockLocked) {
        		double[] clockAdvance;
        		clockAdvance = recoverClock(1);
        		if (clockAdvance != null && Config.recoverClock ) {
        			rebucketData(abBufferDoubleFiltered, clockAdvance);    				
        		}
        	}
        	eyeData.calcAverages();
    		if (monitorAudio && Config.squelchAudio) {
    			if (eyeData.bitSNR < MIN_BIT_SNR) {
    				if (!squelch) {
    					Log.println("No telemetry, squelched ...");
    					// Make sure the audioSink is empty
    					if (sink != null)
    						sink.flush();
    				}
    				squelch=true;
    			} else {
    				if (squelch) Log.println("Attempting to decode ...");
    				squelch=false;
    			}
    		} else {
    			squelch = false;
    		}
            dataFresh = true;

        	eyeData.setFreshData(true);
        	Performance.endTimer("ClockSync");

        	Performance.startTimer("BitsWindow");

        	//       		debugBitsWindow();
        	processBitsWindow();

        	Performance.endTimer("BitsWindow");

        }
        if (sink != null)
        	sink.closeOutput();
        
  //      if (Config.writeDebugWavFile) WavDecoder.writeBytes(allabData);
        Log.println("Frames Decoded: " + framesDecoded);
        cleanup();
        done = true;
		Performance.printResults();		

    }
	
	
	
	/**
	 * calculate the clock offset from the data.  Use the offset to pull the clock forward for the next period.  Reprocess the
	 * current window if the clock offset was large
	 */
	protected double[] recoverClock(int factor) {
    	int transitionPoint = 0; // The average offset of the start/end of a pulse in each bucket
    	transitionPoint = recoverClockOffset();
    	
    	// There are 240 samples in a slow speed bucket, so if we are within 24 samples, then the start/end is in the first 10% and we do nothing
    	// There are 5 samples in high speed bucket, so we need to transition between sample 0 and 1.
    	if (transitionPoint > bucketSize/CLOCK_TOLERANCE && transitionPoint < bucketSize-bucketSize/CLOCK_TOLERANCE) {
    		int clockAdvance = (transitionPoint-bucketSize/CLOCK_TOLERANCE);  // We must consume a multiple of BYTES_PER_SAMPLE (4), otherwise we offset from the byte order of the samples
    		double[] clockData = new double[clockAdvance * factor];;
    		if (Config.debugClock) Log.println("Advancing clock " + clockAdvance/bytesPerSample + " samples");
   // 		if (Config.debugValues)
   // 			System.out.println(-60000); // clock change marker
    		int nBytesRead = read(clockData);
    		if (nBytesRead != (clockAdvance * factor)) {
    			if (Config.debugClock) Log.println("ERROR: Could not advance clock");
    		} else {
    			// This is the new clock offsest
    			// Reprocess the data in the current window
    			return clockData;
    		}
    	} else {
    		if (Config.debugClock) Log.println("CLOCK STABLE");
    		return null;
    	}
    	return null;
		
	}
	
	/**
	 * Given an advance in the clock, we go back to the beginning of the window and shift the data backward in the
	 * raw array.  Then we rebucket and sample the data
	 * 
	 * @param clockAdvance
	 */
	protected void rebucketData(double[] abData, double[] clippedData) {
		appendClippedData(abData, clippedData);
    	resetWindowData();
		bucketData(abData);
		sampleNumber = sampleNumber - SAMPLE_WINDOW_LENGTH;
    	sampleBuckets();
    	
	}
	
	/**
	 * Add clipped data onto the end of the abData array by first moving values to the left
	 * to make room
	 * @param abData
	 * @param clippedData
	 */
	protected void appendClippedData(double[] data, double[] clippedData) {
		// Pull the data back in the abByte array
		for (int i=clippedData.length; i<data.length; i++) {
			data[i-clippedData.length] = data[i];
		}

		// Add the clipped data onto the end of the abData array
		for (int i=0; i<clippedData.length; i++) {
			data[data.length-clippedData.length+i] = clippedData[i];
		}
	}
	
	//int nBytesWritten = sourceLine.write(abData, 0, nBytesRead);  // this writes to the output dataline for playback
	
	public void cleanup() { // release any system assets we have held
		if (audioSource != null) { 
			audioSource.stop();

			while (!audioSource.isDone()) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Divide the sampled data into buckets, based on the perfect clock
	 * @param abData
	 */
	protected void bucketData(double[] abData) {
		int k = 0; // position in the data stream where we read data
		
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			minValue[i] = 256*256;
			
				// AT THIS POINT ASSUME MONO STREAM OF DOUBLES that we need to convert to
				// integer samples in the buckets
				// Doubles are from +/-1.  The samples are from +/- 32k
				for (int j=0; j < bucketSize; j++ ) { // sample size is 2, 2 bytes per channel 				
					int value = (int)(abData[k] * 32768.0);
					dataValues[i][j] = value; 
					eyeData.setData(i,j,value);  // this data is not reset to zero and is easier to graph

					if (value > maxValue[i]) maxValue[i] = value;
					if (value < minValue[i]) minValue[i] = value;
					k=k+1; // move forward 1
				}
			averageMax = averageMax + maxValue[i];
			averageMin = averageMin + minValue[i];
		}
		averageMax = averageMax / SAMPLE_WINDOW_LENGTH;
		averageMin = averageMin / SAMPLE_WINDOW_LENGTH;
		zeroValue = (averageMax + averageMin) / 2;

		if (averageMax > MAX_VOLUME) {
			if (!tooLoud) Log.println("Input too loud ..." + averageMax);
			tooLoud = true;
		} else {
			tooLoud = false;
		}
		
	}
	
	protected void addMeasurements(Header header, Frame frame, int lastErrorsNumber, int lastErasureNumber) {
		// Pass Measurements
		if (Config.passManager.isNewPass()) {
			Log.println("Setting reset/uptime for new pass");
			Config.passManager.setStartResetUptime(header.getFoxId(), header.getResets(), header.getUptime());
		} else {
			Config.passManager.setLastResetUptime(header.getFoxId(), header.getResets(), header.getUptime());
		}

		// Real time measurements
		RtMeasurement rtMeasurement = new RtMeasurement(header.getFoxId(), header.getResets(), header.getUptime(), SatMeasurementStore.RT_MEASUREMENT_TYPE);
		rtMeasurement.setBitSNR(eyeData.bitSNR);
		rtMeasurement.setErrors(lastErrorsNumber);
		rtMeasurement.setErasures(lastErasureNumber);
		SatPc32DDE satPC = null;
		DateTime timeNow = new DateTime(DateTimeZone.UTC);
		Spacecraft sat = Config.satManager.getSpacecraft(header.id);
		SatPos pos = null;
		if (Config.useDDEforAzEl) {
			satPC = new SatPc32DDE();
			boolean connected = satPC.connect();
			if (connected) {
				if (Config.useDDEforAzEl) {
					rtMeasurement.setAzimuth(satPC.azimuth);
					rtMeasurement.setElevation(satPC.elevation);
				}

			}
		} else {
			// We use FoxTelem Predict calculation
			timeNow = new DateTime(DateTimeZone.UTC);
			sat = Config.satManager.getSpacecraft(header.id);
			pos = sat.getSatellitePosition(timeNow);
			if (pos != null) {
				rtMeasurement.setAzimuth(FramePart.radToDeg(pos.getAzimuth()));
				rtMeasurement.setElevation(FramePart.radToDeg(pos.getElevation()));
			}
		}
		if (this.audioSource instanceof SourceIQ) {
			long freq = ((SourceIQ)audioSource).getFrequencyFromBin(Config.selectedBin);
			double sig = ((SourceIQ)audioSource).rfData.getAvg(RfData.PEAK);
			double rfSnr = ((SourceIQ)audioSource).rfData.rfSNR;
			rtMeasurement.setCarrierFrequency(freq);
			rtMeasurement.setRfPower(sig);
			rtMeasurement.setRfSNR(rfSnr);
		} else {
			if (Config.useDDEforFreq) {
				if (satPC == null)
					satPC = new SatPc32DDE();
				boolean connected = satPC.connect();
				if (connected)
					rtMeasurement.setCarrierFrequency(satPC.downlinkFrequency);
			} else {
				// Do nothing for now.  Need to work out how to get doppler from predict
				/* Use Fox Predict calcualtion for frequency in AF mode
				if (pos == null) {
					timeNow = new DateTime(DateTimeZone.UTC);
					sat = Config.satManager.getSpacecraft(header.id);
					pos = sat.getSatellitePosition(timeNow);
				}
				if (pos != null) {
					rtMeasurement.setCarrierFrequency(pos.FREQ);
					
				}
				*/

			}
		}
		
		Config.payloadStore.add(header.getFoxId(), rtMeasurement);		
		frame.setMeasurement(rtMeasurement);
	}	
	
	/**
	 * Print the data for debug purposes so that we can graph it in excel
	 * Include markers for the start and end of buckets and for the value of the mid point sample
	 */
	protected void printBucketsValues() {
		
//		for (int m=0; m<2; m++)
	//		System.out.println(-40000); // start of window
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			//System.out.print("BUCKET" + i + " MIN: " + minValue[i] + " MAX: " + maxValue[i] + " - ");
//			for (int m=0; m<4; m++)
	//			System.out.println(40000); // start of bucket marker
			int step = 10;
			int middle = 120;
			if (this instanceof Fox9600bpsDecoder) {
				step = 1;
				middle = 3;
			}
			
			for (int j=0; j<bucketSize; j+=step) { // 20) {
				//if (j== BUCKET_SIZE/4) System.out.print("** ");
				//if (j==BUCKET_SIZE/2 && Config.debugBits) {
				
				if (j==middle) {  
					if (this.middleSample[i] == true)
						System.out.println(35000); // middle of bucket value
					else
						System.out.println(-35000); // middle of bucket value
						
				}
				
				System.out.println(dataValues[i][j] + " ");
				//if (j== BUCKET_SIZE/4) System.out.print(" ** ");
			}
		}
//		System.out.println("Average Max: " + averageMax);
//		System.out.println("Average Min: " + averageMin);
//		System.out.println("Zero: " + zeroValue);
	}

	
	public static void printBytes(byte b[]) {
		for (int i=0; i < b.length; i++) {
			System.out.print(b[i] + " ");
		}
	}
	
	public static void printBytesAndChars(byte b[]) {
		for (int i=0; i < b.length; i++) {
			char c = (char) b[i];
			System.out.print(b[i] + " " + c + " ");
		}
	}
	
	public static int littleEndian4(byte b[]) {
		byte b1 = b[0];
		byte b2 = b[1];
		byte b3 = b[2];
		byte b4 = b[3];
		int value =  ((b4 & 0xff) << 24)
		     | ((b3 & 0xff) << 16)
		     | ((b2 & 0xff) << 8)
		     | ((b1 & 0xff) << 0);
		return value;
	}

	public static int oldlittleEndian2(byte b[], int bitsPerSample) {
		byte b1 = b[0];
		byte b2 = b[1];
		int value =  ((b2 & 0xff) << 8)
		     | ((b1 & 0xff) << 0);
		if (value > (32768-1)) value = -65536 + value;
		return value;
	}

	public static int littleEndian2(byte b[], int bitsPerSample) {
		byte b1 = b[0];
		byte b2 = b[1];
		int value =  ((b2 & 0xff) << 8)
		     | ((b1 & 0xff) << 0);
		if (value > (Math.pow(2,bitsPerSample-1)-1)) value = (int) (-1*Math.pow(2,bitsPerSample) + value);
		return value;
	}

	public static int bigEndian2(byte b[], int bitsPerSample) {
		byte b1 = b[1];
		byte b2 = b[0];
		int value =  ((b2 & 0xff) << 8)
		     | ((b1 & 0xff) << 0);
		if (value > (2^(bitsPerSample-1)-1)) value = -(2^bitsPerSample) + value;
		return value;
	}

	
	public static int littleEndian2unsigned(byte b[]) {
		byte b1 = b[0];
		byte b2 = b[1];
		int value =  ((b2 & 0xff) << 8)
		     | ((b1 & 0xff) << 0);
		return value;
	}

	public static String dec(int n) {
	    // call toUpperCase() if that's required
	    return String.format("%4s", Integer.toString(n)).replace(' ', '0');
	}
	
	public static String dec(long n) {
	    // call toUpperCase() if that's required
	    return String.format("%6s", Long.toString(n)).replace(' ', '0');
	}
	
	public static String hex(long n) {
	    // call toUpperCase() if that's required
	    return String.format("0x%5s", Long.toHexString(n)).replace(' ', '0');
	}
	
	public static String hex(int n) {
		return String.format("0x%2s", Integer.toHexString(n)).replace(' ', '0');
		//return String.valueOf(n);
	}

	public static String hex(byte n) {
		int i = 0xff & n;
		return String.format("0x%2s", Integer.toHexString(i)).replace(' ', '0');
		//return String.valueOf(n);
	}
	
	public static String plainhex(int n) {
		return String.format("%2s", Integer.toHexString(n)).replace(' ', '0');
		//return String.valueOf(n);
	}

	public static long getTimeNano() {
		long l = System.nanoTime();
		return l;
	}

}
