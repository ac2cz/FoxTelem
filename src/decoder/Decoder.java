package decoder;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import measure.RtMeasurement;
import measure.SatMeasurementStore;
import measure.SatPc32DDE;
import common.Config;
import common.Log;
import common.Performance;
import filter.Filter;
import filter.RaisedCosineFilter;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.FramePart;
import telemetry.Header;
import telemetry.HighSpeedHeader;
import telemetry.PayloadCameraData;
import telemetry.PayloadHERCIhighSpeed;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRadExpData;
import telemetry.PayloadRtValues;
import telemetry.HighSpeedFrame;
import telemetry.SlowSpeedFrame;
import telemetry.SlowSpeedHeader;

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
	public static final int BIT_DISTANCE_THRESHOLD_PERCENT = 15; // use 20 for 736R audio *****15; // Distance that bits need to be apart to change the bit decision as % of average BIT HEIGHT
														
	protected int BUFFER_SIZE = 0; // * 4 for sample size of 2 bytes and both channels
	private byte[] abData;
	private byte[] filteredData;
	private double[] abBufferDouble;
	private double[] abBufferDoubleFiltered; 
	protected int currentFilterLength = 0;
	protected double currentFilterFreq = 0d;
	private boolean dataFresh = false; // true if we have just written new data for the GUI to read
	
	
    private int[][] dataValues = null;
    public EyeData eyeData;
    private int[] maxValue = null;
    private int[] minValue = null;
    private int[] firstZero = null;
    private int[] secondZero = null;
    private boolean[] middleSample = null; // The sampled bit for the entire bucket
    
    /**
     * This holds the stream of bits that we have not decoded. Once we have several
     * SYNC words, this is flushed of processed bits.
     */
    protected BitStream bitStream = null;  // Hold bits until we turn them into decoded frames
    
    private int averageMax;
    private int averageMin;
    private int zeroValue;
	
    private int sampleNumber = 0;;
   // private int windowNumber = 0;
    
    private int framesDecoded = 0;

    private boolean squelch = true;
    private boolean tooLoud = false;
    private boolean clockLocked = false; // set to true when we have decoded a frame. Not reset until decode fails
    private boolean processing = true; // called to end the thread from the GUI
    private boolean done = true; // true when we have dropped out of the loop or when not running the loop
	
    private int lastBitValue = 0; // store the value of the last bit for use in the bit detection algorithm
    private boolean lastBit = false;
	
  //  private boolean frameMarkerFound = false;
  //  private int missedSyncWords = 0;
  //  private int frameByteCount = 0;
    
    private long lastLoopTime = 0; // loop timer to slow down execution if we want to simulate decoding from a file
    private long OPTIMAL_TIME = 0; // scaling factor for loop timer
    private Frame decodedFrame = null;
    
    protected Filter filter = null;
    public Filter monitorFilter = null;
//    private AGCFilter agcFilter = null;
    
    private SinkAudio sink = null; // The audio output device that we copy bytes to if we are monitoring the audio
    private boolean monitorAudio = false; // true if we are monitoring the audio
	//protected boolean monitorFiltered; // try if we are monitoring audio after it is filtered
    
    /**
     * Given an audio source, decode the data in it,
     */
	public Decoder(SourceAudio as, int chan) {
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
	public Filter getFilter() { return filter; }
	public boolean getBigEndian() { return bigEndian; }
	public int getBucketSize() { return bucketSize; }
	/**
	 * This is called each time the decoder is started from the GUI
	 */
	public void init() {
		Performance.setEnabled(Config.debugPerformance);  // enable performance logging (or not)
		
		processing = true;
		done = false;
		
		BUFFER_SIZE = bytesPerSample * SAMPLE_WINDOW_LENGTH * bucketSize;

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
		monitorFilter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE /bytesPerSample);
		monitorFilter.init(currentSampleRate, 3000, 256);
	}

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
			audioReadThread.start();
		}
		
	}

	
	//public static int getSampleRate() { return Config.currentSampleRate; }
	
	public boolean toggleAudioMonitor(SinkAudio s, boolean monitorFiltered, int position) throws LineUnavailableException {
		
		sink = s;
		Config.monitorFilteredAudio = monitorFiltered;
		monitorAudio = !monitorAudio;
		if (!monitorAudio)
			sink.closeOutput();
		else {
			sink.setDevice(position);
		}
		return monitorAudio;
	}
	

	public void stopAudioMonitor(SinkAudio s) throws LineUnavailableException {
		sink = s;
		if (monitorAudio == true) {
			monitorAudio = false;
			sink.closeOutput();
		}
		
	}

	public void setMonitorAudio(SinkAudio s, boolean m, int position) throws IllegalArgumentException, LineUnavailableException { 
		sink = s;
		if (sink != null) {
		monitorAudio = m; 
		if (!monitorAudio)
			sink.closeOutput();
		else {
			sink.setDevice(position);
		}
		}
}
	
//	public void setMonitorFiltered(boolean m) { monitorFiltered = m; }

	public int getAudioBufferSize() { return audioSource.getAudioBufferSize(); }
	public int getAudioBufferCapacity() { return audioSource.getAudioBufferCapacity(); }
	
	public byte[] getAudioData() {
		if (dataFresh==false) {
			return null;
		} else {
			dataFresh=false;
			return abData;
		}
	}
	public byte[] getFilteredData() {
		if (dataFresh==false) 
			return null;
		else {
			dataFresh=false;
			return filteredData;
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
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"FATAL ERROR IN DECODER",
				    JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace();
			e.printStackTrace(Log.getWriter());
			
		}
		Log.println("DECODER Exit");
	}
	
	protected int readBytes(byte[] abData) {
		int nBytesRead = 0;
		//Log.println("Reading bytes from channel: " + audioChannel);
		nBytesRead = audioSource.readBytes(abData, audioChannel);	
		return nBytesRead;
	}

	
	public void process() throws UnsupportedAudioFileException, IOException {
		boolean stereo = true;
		if (audioSource.audioFormat.getChannels() == Decoder.MONO) stereo = false;
        int nBytesRead = 0;
        abData = new byte[BUFFER_SIZE];  
        filteredData = new byte[BUFFER_SIZE];  
		abBufferDouble = new double[BUFFER_SIZE /bytesPerSample];
		abBufferDoubleFiltered = new double[BUFFER_SIZE /bytesPerSample];
		
 /*       if (Config.writeDebugWavFile) {
        	WavDecoder.initDebugWav();
        	allabData = new byte[BUFFER_SIZE*600];  // Debug Wav file storage.  This is a crazy approach only used for debug
        }
 */
        if (Config.filterData)
        	if (this instanceof Fox9600bpsDecoder) 
        		Log.println("FILTER: none");
        	else
        		Log.println("FILTER: " + filter.toString()); //filters[Config.useFilterNumber].toString());
        Log.println("BUFFER: " + BUFFER_SIZE);
        Log.println("DECODING FRAMES LENGTH " + bitStream.SYNC_WORD_DISTANCE + " bits ... ");
        
		startAudioThread();
        
        int count=0;
        while (nBytesRead != -1 && processing) {
        	Performance.startTimer("Setup");
    		resetWindowData();
    		Performance.endTimer("Setup");
        	if (nBytesRead >= 0) {
        		Performance.startTimer("Read");
                nBytesRead = readBytes(abData);
                if (monitorAudio && !squelch && !Config.monitorFilteredAudio) {
                	if (sink != null)
                		sink.write(abData, abData.length);
                }
                if (Config.debugBytes) 
                	if (nBytesRead != abData.length) Log.println("ERROR: COULD NOT READ FULL BUFFER");
        		Performance.endTimer("Read");
        		Performance.startTimer("Filter");

                if (Config.filterData) { // && !Config.highSpeed) {
                	SourceAudio.getDoublesFromBytes(abData, stereo, abBufferDouble);
                	/**
                	 * Note that the filter converts the byte values to doubles and then back, so that
                	 * we can play the filtered audio back to the user if requested.
                	 */
                	//filteredData = filter.filter(abData); //filters[Config.useFilterNumber].filter(abData);
                	filter.filter(abBufferDouble, abBufferDoubleFiltered);
                	SourceAudio.getBytesFromDoubles(abBufferDoubleFiltered, abBufferDoubleFiltered.length, stereo, filteredData);
                } else
                	filteredData = abData;
                if (Config.filterOutputAudio) {
//                	monitorFilter.filter(abBufferDouble, abBufferDouble);
 //               	SourceAudio.getBytesFromDoubles(abBufferDouble, abBufferDouble.length, stereo, abData);
                }
                Performance.endTimer("Filter");
                Performance.startTimer("Monitor");

                dataFresh = true;
                if (monitorAudio && !squelch && Config.monitorFilteredAudio) {
        			sink.write(filteredData, abData.length);
                }
                Performance.endTimer("Monitor");
                Performance.startTimer("Bucket");

                bucketData(filteredData);
                Performance.endTimer("Bucket");

        	}


        	Performance.startTimer("Sample");
        	sampleBuckets();
        	Performance.endTimer("Sample");

        	Performance.startTimer("ClockSync");

        	if (!clockLocked) {
        		byte[] clockAdvance;
        		clockAdvance = recoverClock(1);
        		if (clockAdvance != null && Config.recoverClock ) {
        			rebucketData(filteredData, clockAdvance);    				
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

        	eyeData.setFreshData(true);
        	Performance.endTimer("ClockSync");

        	Performance.startTimer("BitsWindow");

        	//       		debugBitsWindow();
        	processBitsWindow();

        	Performance.endTimer("BitsWindow");

        	Performance.startTimer("debugValues");

        	if (Config.debugValues /*&& framesDecoded == writeAfterFrame*/) {
        		if (Config.debugBytes) printBytes(abData);
        		//    			if (debugBytes) printBuckets();
        		//if (frameMarkerFound)  
        		printBucketsValues();
        		//printByteValues();
        	}
        	if (Config.debugValues) count ++;
        	if (count == 3) System.exit(0); /// DROP OUT FOR TEST PURPOSES
        	if (Config.DEBUG_COUNT != -1 && count > Config.DEBUG_COUNT)
        		nBytesRead = -1; /// DROP OUT FOR TEST PURPOSES

        	Performance.endTimer("debugValues");

        	Performance.startTimer("findSync");
        	boolean found = bitStream.findSyncMarkers(SAMPLE_WINDOW_LENGTH);
        	Performance.endTimer("findSync");
        	if (found) {
        		processPossibleFrame();
        	}
        	//windowNumber++;

        	
            Performance.startTimer("Yield");
        	if (Config.realTimePlaybackOfFile) {
        		long now = System.nanoTime()/1000000;
        		long sleepTime = (lastLoopTime-now + OPTIMAL_TIME);
        		lastLoopTime = now;
        		if (sleepTime > 0)
        			try {
        				Thread.sleep( sleepTime );
        			} catch (InterruptedException e) {
        				Log.println("Mainloop Sleep Interrupted!");
        			} 
        		else {
        			Thread.yield();
        		}
        		
        	}
        	
    		Performance.endTimer("Yield");
        }
        if (sink != null)
        	sink.closeOutput();
        
  //      if (Config.writeDebugWavFile) WavDecoder.writeBytes(allabData);
        Log.println("Frames Decoded: " + framesDecoded);
        cleanup();
        done = true;
		Performance.printResults();
		

    }
	
	protected void processPossibleFrame() {
		
		/*
		 * Cause the audio to glitch for testing
		 *
		if (Config.debugAudioGlitches)
			try {
				Thread.sleep(400);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		*/
		
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
				if (decodedFrame instanceof SlowSpeedFrame) {
					SlowSpeedFrame ssf = (SlowSpeedFrame)decodedFrame;
					FramePart payload = ssf.getPayload();
					SlowSpeedHeader header = ssf.getHeader();
					if (Config.storePayloads) Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
					
					// Capture measurements once per payload or every 5 seconds ish
					addMeasurements(header, decodedFrame);
				} else {
					HighSpeedFrame hsf = (HighSpeedFrame)decodedFrame;
					HighSpeedHeader header = hsf.getHeader();
					PayloadRtValues payload = hsf.getRtPayload();
					Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
					PayloadMaxValues maxPayload = hsf.getMaxPayload();
					Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), maxPayload);
					PayloadMinValues minPayload = hsf.getMinPayload();
					Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), minPayload);
					PayloadRadExpData[] radPayloads = hsf.getRadPayloads();
					Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), radPayloads);
					if (Config.satManager.hasCamera(header.getFoxId())) {
						PayloadCameraData cameraData = hsf.getCameraPayload();
						if (cameraData != null)
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), cameraData);
					}
					if (Config.satManager.hasHerci(header.getFoxId())) {
						PayloadHERCIhighSpeed[] herciDataSet = hsf.getHerciPayloads();
						if (herciDataSet != null)
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), herciDataSet);
					}
					// Capture measurements once per payload or every 5 seconds ish
					addMeasurements(header, decodedFrame);
				}

			}
			if (Config.uploadToServer)
				try {
					Config.rawFrameQueue.add(decodedFrame);
				} catch (IOException e) {
					// Don't pop up a dialog here or the user will get one for every frame decoded.
					// Write to the log only
					e.printStackTrace(Log.getWriter());
				}
			framesDecoded++;
			Performance.endTimer("Store");
		} else {
			if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
			clockLocked = false;
		}
	}
	

	private void addMeasurements(Header header, Frame frame) {
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
		rtMeasurement.setErrors(bitStream.lastErrorsNumber);
		rtMeasurement.setErasures(bitStream.lastErasureNumber);
		if (Config.useDDEforAzEl) {
			SatPc32DDE satPC = new SatPc32DDE();
			boolean connected = satPC.connect();
			if (connected) {
				if (Config.useDDEforAzEl) {
					rtMeasurement.setAzimuth(satPC.azimuth);
					rtMeasurement.setElevation(satPC.elevation);
				}

			}
		}
		if (this.audioSource instanceof SourceIQ) {
			long freq = ((SourceIQ)audioSource).getFrequencyFromBin(Config.selectedBin);
			double sig = ((SourceIQ)audioSource).rfData.getAvg(RfData.PEAK);
			double rfSnr = ((SourceIQ)audioSource).rfData.rfSNR;
			rtMeasurement.setCarrierFrequency(freq);
			rtMeasurement.setRfPower(sig);
			rtMeasurement.setRfSNR(rfSnr);
		}
		Config.payloadStore.add(header.getFoxId(), rtMeasurement);		
		frame.setMeasurement(rtMeasurement);
	}
	
	/**
	 * calculate the clock offset from the data.  Use the offset to pull the clock forward for the next period.  Reprocess the
	 * current window if the clock offset was large
	 */
	protected byte[] recoverClock(int factor) {
    	int transitionPoint = 0; // The average offset of the start/end of a pulse in each bucket
    	transitionPoint = recoverClockOffset();
    	
    	// There are 240 samples in a slow speed bucket, so if we are within 24 samples, then the start/end is in the first 10% and we do nothing
    	// There are 5 samples in high speed bucket, so we need to transition between sample 0 and 1.
    	if (transitionPoint > bucketSize/CLOCK_TOLERANCE && transitionPoint < bucketSize-bucketSize/CLOCK_TOLERANCE) {
    		int clockAdvance = (transitionPoint-bucketSize/CLOCK_TOLERANCE)*bytesPerSample;  // We must consume a multiple of BYTES_PER_SAMPLE (4), otherwise we offset from the byte order of the samples
    		byte[] clockData = new byte[clockAdvance * factor];;
    		if (Config.debugClock) Log.println("Advancing clock " + clockAdvance/bytesPerSample + " samples");
   // 		if (Config.debugValues)
   // 			System.out.println(-60000); // clock change marker
    		int nBytesRead = readBytes(clockData);
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
	protected void rebucketData(byte[] abData, byte[] clippedData) {
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
	protected void appendClippedData(byte[] data, byte[] clippedData) {
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
	protected void bucketData(byte[] abData) {
		int k = 0; // position in the data stream where we read data
		
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			minValue[i] = 256*256;
			
			if (channels == STEREO) {
				for (int j=0; j < bucketSize; j++ ) { // sample size is 4, 2 bytes per channel 				
					byte[] by = new byte[2];
					if (Config.useLeftStereoChannel) {     // THIS PROBABLLY SHOULD BE PASSED IN AS PART OF INIT, BUT WE WILL ALLOW IT TO BE CHANGED LIVE BY THE USER IN THE GUI
						by[0] = abData[k];
						by[1] = abData[k+1];
					} else {
						by[0] = abData[k+2];  
						by[1] = abData[k+3];
					}
					int value;
					if (bigEndian)
						value = bigEndian2(by, bitsPerSample);
					else
						value = littleEndian2(by, bitsPerSample);
					dataValues[i][j] = value; 
					eyeData.setData(i,j,value);  // this data is not reset to zero and is easier to graph

					if (value > maxValue[i]) maxValue[i] = value;
					if (value < minValue[i]) minValue[i] = value;
					k=k+4; // move forward 4 to skip the next two bytes on the other stereo channel
				}
			} else {
				// MONO
				for (int j=0; j < bucketSize; j++ ) { // sample size is 2, 2 bytes per channel 				
					byte[] by = new byte[2];
					by[0] = abData[k];
					by[1] = abData[k+1];
					int value;
					if (bigEndian)
						value = bigEndian2(by, bitsPerSample);
					else
						value = littleEndian2(by, bitsPerSample);
					dataValues[i][j] = value; 
					eyeData.setData(i,j,value);  // this data is not reset to zero and is easier to graph

					if (value > maxValue[i]) maxValue[i] = value;
					if (value < minValue[i]) minValue[i] = value;
					k=k+2; // move forward 2
				
				}
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
	
	protected void sampleBuckets() {
		if (this instanceof Fox9600bpsDecoder) 
			sampleBucketsAgainstZeroCrossover();
		else
			sampleBucketsVsDistanceToLastBit();
		//sampleBucketsAgainstThresholdChanges();
	}
	
	/**
	 * This algorithm samples the bit at the center and compares it to the zero crossing.  It also calculates the level
	 * change from the previous bit.  If the bit has  not changed a sufficient amount, then we determine this is still
	 * the same value as the previous bit, even if we have passed the zero level.  This compensates for deteriation of 
	 * long bit streams.
	 * 
	 * THIS SHOULD BE UPDATED TO ONLY COMPENSATE IF IT IS THE 4TH BIT IN A ROW WITH THE SAME VALUE AND IF THE SEQUENCE LOOKS
	 * LIKE A SYNC WORD.....
	 * 
	 * THIS CAN ALSO BE IN CONJUNCTION WITH THE 10b BEST GUESS ALGORITHM THAT I NEED TO IMPLEMENT.
	 * 
	 */
	protected void sampleBucketsVsDistanceToLastBit() {
				
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			sampleNumber++;
			int sampleSum = 0;
			int samples = 0;
			// Making upper limit <= means we decode 1 less frame in test set.... Need to investigate
			// because this is needed for HS frames and should not make a difference for LS.
//			for (int s=BUCKET_SIZE/2-SAMPLE_WIDTH; s < BUCKET_SIZE/2+SAMPLE_WIDTH; s++) {
			for (int s=bucketSize/2-SAMPLE_WIDTH; s <= bucketSize/2+SAMPLE_WIDTH; s++) {
				sampleSum = sampleSum + dataValues[i][s];
				samples++;
			}
			sampleSum = sampleSum/samples; // get the average value for this bit
	
			
			// NOTE THAT THE BIT DISTANCE DOES NOT WORK IF WE ARE RE-PROCESSING DUE TO CLOCK MOVE
			int bitDistance = Math.abs(lastBitValue-sampleSum);
			int bitHeight = averageMax-averageMin;
			if (bitHeight == 0) bitHeight = 1; // avoid divide by zero error
			int movePercent = bitDistance*100/bitHeight;
			
			if (sampleSum >= zeroValue) {
				// We are above the zero threshold.  Decide on "1" unless the last value was "0" and we moved less
				// than threshold
				
				if (lastBit == false) {
					if ( movePercent < BIT_DISTANCE_THRESHOLD_PERCENT) {
						middleSample[i] = false; // we did not move far enough, stay at "0"
						eyeData.setLow(sampleSum);
					} else {
						middleSample[i] = true;
						eyeData.setHigh(sampleSum);
					}
				} else {
					middleSample[i] = true;
					eyeData.setHigh(sampleSum);
				}
				
			} else { 
				// We are below the zero threshold.  Decide on "0" unless the last value was "1" and we moved less
				// than threshold
				if (lastBit == true) {
					if (movePercent < BIT_DISTANCE_THRESHOLD_PERCENT) {
						middleSample[i] = true; // we did not move far enough, stay at "1"
						eyeData.setHigh(sampleSum);
					} else {
						middleSample[i] = false;
						eyeData.setLow(sampleSum);
					}
				} else {
					middleSample[i] = false;
					eyeData.setLow(sampleSum);
				}
			}
			
			lastBitValue = sampleSum;
			lastBit = middleSample[i];
		}
	}

	/**
	 * Use the zerovalue that we have calculated for this window of data to determine if each bucket is a 1 or a zero
	 * 
	 */
	protected void sampleBucketsAgainstZeroCrossover() {
				
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			sampleNumber++;
			int sampleSum = 0;
			int samples = 0;
			// Making upper limit <= means we decode 1 less frame in test set.... Need to investigate
			// because this is needed for HS frames and should not make a difference for LS.
//			for (int s=BUCKET_SIZE/2-SAMPLE_WIDTH; s < BUCKET_SIZE/2+SAMPLE_WIDTH; s++) {
			for (int s=bucketSize/2-SAMPLE_WIDTH; s <= bucketSize/2+SAMPLE_WIDTH; s++) {
				sampleSum = sampleSum + dataValues[i][s];
				samples++;
				
			}
			sampleSum = sampleSum/samples; // get the average value
			
			if (sampleSum >= zeroValue) {
				middleSample[i] = true;
				eyeData.setHigh(sampleSum);
			} else {
				middleSample[i] = false;
				eyeData.setLow(sampleSum);
			}
		}
	}


	
	protected void processBitsWindow() {
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			bitStream.addBit(middleSample[i]);
		}
	}

	int rd = 0;
	int nextRd = 0;
	protected void debugBitsWindow() {
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i+=10) {
			boolean[] b = new boolean[10];
			boolean[] b8 = new boolean[8];
			for (int k=0; k<10; k++)
				b[k] = middleSample[i+k];
			
			int word = BitStream.binToInt(b);
			rd = Code8b10b.getRdSense10b(word, flipReceivedBits);
			BitStream.printBitArray(b);
			System.out.println("Expected: " + nextRd + " rd: " + rd);
			nextRd = Code8b10b.getNextRd(word, flipReceivedBits);
			byte word8b;
			try {
				word8b = Code8b10b.decode(word, flipReceivedBits);
				Log.print(i+ ": 10b:" + Decoder.hex(word));	
				
				Log.print(" 8b:" + Decoder.hex(word8b));
				Log.println("");
				b8 = BitStream.intToBin8(word8b);
				BitStream.printBitArray(b8);
			} catch (LookupException e) {
				Log.print(i+ ": 10b:" + Decoder.hex(word));	
				Log.print(" 8b: -1");
				Log.println("");
			}
		}
	}
	
	
	
	

	/**
	 * Check the correlation between the clock and the data.  Determine if we should move the clock forward or backwards
	 * The clock will then be adjusted for the next window
	 * 
	 * If the clock is perfectly aligned, then the rise of fall would happen in the first and last samples of the bucket.  We will define sync when
	 * the transition happens in the first 10% of the bucket.  This will guarantee that the middle of the bucket is stable
	 * 
	 * If we calculate that we are offset from that position then we will move the clock halfway to the right position.  It should then sync over a few samples.
	 * If it is inside the 10%, on average, then we do nothing.
	 * 
	 * @return
	 */
	public int recoverClockOffset() {
		int transitionPoint[] = new int[SAMPLE_WINDOW_LENGTH];
		int averageTransition = 0;
		int numberOfTransitions = 0;
		boolean foundTransition = false;
		int threshold = (averageMax - averageMin) / CLOCK_REOVERY_ZERO_THRESHOLD;  // 10 better for High Speed?
	//	int threshold = (averageMax - averageMin) / 20 ; // Use 5% of the distance between max and min as a threhold value to avoid noise
		int initialValue = 0; // This holds the logic value while we are scanning, to see if it changes
		
		// run through the whole sample window, looking in each bucket for a clock change
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			if (i>0) {
				if (dataValues[i-1][bucketSize-1] >= zeroValue + threshold) initialValue = 1; else initialValue = 0;  // default the initial value based on last sample in the previous bucket
		//		if (dataValues[i][0] >= zeroValue) initialValue = 1; else initialValue = 0;  // default the initial value based on last sample in the previous bucket
			}
			foundTransition = false; // reset the flag at the start of each bucket
			for (int j=0; j<bucketSize; j++) {
				
				if (dataValues[i][j] > (zeroValue + threshold) && initialValue == 0) {
					if (!foundTransition) {
						transitionPoint[i] = j;
						initialValue = 1;
						foundTransition = true;
						numberOfTransitions ++;
					}
					else
						;//System.out.println("Multiple LOW-HIGHTransitions in window/bucket: " + i + " " + j);
					
				} 
				if (dataValues[i][j] < (zeroValue - threshold) && initialValue == 1) {
					if (!foundTransition) {			
						transitionPoint[i] = j;
						initialValue = 0;
						foundTransition = true;
						numberOfTransitions ++;
					}
					else
						;//System.out.println("Multiple HIGH-LOW Transitions in window/bucket: " + i + " " + j);
					
				}
			}
			averageTransition = averageTransition + transitionPoint[i];
			
		}
		if (numberOfTransitions > 0)
			averageTransition = averageTransition/numberOfTransitions;
		if (Config.debugClock) Log.println("CLOCK: Average First Transition at: " + averageTransition);
		
		return averageTransition;
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

	/**
	 * Print the data for debug purposes so that we can graph it in excel
	 * Include markers for the start and end of buckets and for the value of the mid point sample
	 */
	protected void printByteValues() {
		byte[] by = new byte[2];
		int k = 0;
	//		System.out.println(-40000); // start of window
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			//System.out.print("BUCKET" + i + " MIN: " + minValue[i] + " MAX: " + maxValue[i] + " - ");
//			for (int m=0; m<4; m++)
	//			System.out.println(40000); // start of bucket marker
			int step = 10;
			if (this instanceof Fox9600bpsDecoder) {
				step = 1;
			}
			
			for (int j=0; j<bucketSize; j+=step) { // 20) {
				//if (j== BUCKET_SIZE/4) System.out.print("** ");
				//if (j==BUCKET_SIZE/2 && Config.debugBits) {
				
//				by[0] = filteredData[k];
//				by[1] = filteredData[k+1];
				by[0] = abData[k];
				by[1] = abData[k+1];
				System.out.println(littleEndian2(by, bitsPerSample));
				//if (j== BUCKET_SIZE/4) System.out.print(" ** ");
				k +=4;
			}
		}
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
