package decoder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import common.Config;
import common.Log;
import common.Performance;
import filter.Filter;
import gui.MainWindow;

/**
 * An Abstract class that provides the framework for all decoders.  It is a runnable thread that must implement a process method.
 * It provides the audio framework to read (and write to a sink).  The process method processes the audio and write to the
 * attached circularBitBuffer
 * 
 * A Decoder class implements this abstract class.  It must define the following variables, which are calculated in the init function:
 * - BUFFER_SIZE - this is the size of the byte buffer that is read each time we pull aduio from the audio source
 * - BITS_PER_SECOND - this is the speed of the decoder
 * 
 * It must also specify a bitStream in the init class.  This is a circular buffer that knows how to decode bits into frames for this decoder
 * 
 * @author chris
 *
 */
public abstract class Decoder implements Runnable {
	protected int BITS_PER_SECOND = 0;
	public boolean flipReceivedBits;	
	
	public static final int MONO = 1;
	public static final int STEREO = 2;
	
	protected Thread audioReadThread;
	protected boolean processing = true; // called to end the thread from the GUI
	protected boolean done = true; // true when we have dropped out of the loop or when not running the loop

	protected int bucketSize = 0; // Must be initialized based on the sample rate and bits per second

	protected int BUFFER_SIZE = 0; // * 4 for sample size of 2 bytes and both channels
	protected byte[] abData;
	protected byte[] filteredData;
	protected double[] abBufferDouble;
	protected double[] abBufferDoubleFiltered; 
	
	public String name = "";
	// This is the audio source that holds incoming audio.  It contains a circularByte buffer and the decoder reads from it.  The decoder does
	// not care where the audio comes from or how it gets into the buffer
	protected SourceAudio audioSource;
	protected int audioChannel = 0; // this is the audio channel that we read from the upstream audioSource
	protected int currentSampleRate = 48000;
	protected int bitsPerSample = 16;
	protected boolean bigEndian = false;
	protected int bytesPerSample = 4; // 4 bytes for stereo.  Updated by the program if file is mono
	protected int channels = 2; // one for mono two for stereo
	boolean stereo; // true if both audio channels are used.  Otherwise the source is mono
	
	protected int framesDecoded = 0;

	protected int SAMPLE_WINDOW_LENGTH = 70; //This is the number of buckets to grab each time we pull data from the stream.  Usually set by a class that implements this abstract class
	protected int CLOCK_TOLERANCE = 10; // use 4 for 736R audio *****10; // The BUCKET_SIZE is divided by this to give a tolerance at the start of the window
	protected int SAMPLE_WIDTH_PERCENT = 10; //use 1 for 736R audio ***** 10; // 10% is reasonable.  Increase to this needs decrease to BIT_DISTANCE_THRESHOLD_PERCENT 
	protected int SAMPLE_WIDTH = 0; // sample +- this number and average them.  Set when initialized
	protected int CLOCK_REOVERY_ZERO_THRESHOLD = 20; // but updated to 10 for highspeed in the code
	
	protected SinkAudio sink = null; // The audio output device that we copy bytes to if we are monitoring the audio
	protected boolean monitorAudio = false; // true if we are monitoring the audio
	protected boolean dataFresh = false; // true if we have just written new data for the GUI to read
	public EyeData eyeData; // Data we can use to draw an eye diagram
	protected int zeroValue;
	protected boolean squelch = true;
	
    protected Filter filter = null;
    
    
    
    public static final int MIN_BIT_SNR = 2;// Above this threshold we unsquelch the audio
    protected int averageMax;
    protected int averageMin;
    protected int sampleNumber = 0;
    protected int[][] dataValues = null;
    protected boolean[] middleSample = null; // The sampled bit for the entire bucket
    //protected int[] maxValue = null;
    //protected int[] minValue = null;
    
    public static final int MAX_VOLUME = 32000;
	public static final int MIN_VOLUME = 600;// Use low value for 736R audio e.g. 100 ***** 600;
	protected boolean tooLoud = false;
	   
	/**
	 * Given an audio source, decode the data in it,
	 */
	public Decoder(String n, SourceAudio as, int chan) {
		processing = true;
		done = false;
		name = n;
		audioSource = as;
		audioChannel = chan;
		AudioFormat audioFormat = as.getAudioFormat();
		currentSampleRate = (int) audioFormat.getSampleRate();
		bitsPerSample = audioFormat.getSampleSizeInBits();
		bigEndian = audioFormat.isBigEndian();
		channels = audioFormat.getChannels();
		bytesPerSample = audioFormat.getFrameSize();

	}

	abstract protected void init();
	abstract protected void sampleBuckets(); // the routine that samples the buckets and caches the binary data for analysis by clock recovery
	abstract protected int recoverClockOffset(); // the routine that calculates how many samples to pull the audio so that the clock is aligned
	abstract protected void processBitsWindow(); // add the bits to the bitStream and process them into frames
	public Filter getFilter() { return filter; }
	public boolean getBigEndian() { return bigEndian; }
	public int getBucketSize() { return bucketSize; }
	
	/**
	 * This is once when the decoder is started
	 */
	protected void initWindowData() {

		dataValues = new int[SAMPLE_WINDOW_LENGTH][];

	//	maxValue = new int[SAMPLE_WINDOW_LENGTH];
	//	minValue = new int[SAMPLE_WINDOW_LENGTH];
	//	firstZero = new int[SAMPLE_WINDOW_LENGTH];
	//	secondZero = new int[SAMPLE_WINDOW_LENGTH];
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
			middleSample[j] = false; // = new boolean[SAMPLE_WINDOW_LENGTH];
		}
		
	}

	protected void process() throws UnsupportedAudioFileException, IOException {
		Log.println("Decoder using sample rate: " + currentSampleRate);
		Log.println("Decoder using bucketSize: " + bucketSize);
		Log.println("Decoder using SAMPLE_WIDTH: " + SAMPLE_WIDTH);
		Log.println("Decoder using BUFFER_SIZE: " + BUFFER_SIZE);
		Log.println("Decoder using BITS_PER_SECOND: " + BITS_PER_SECOND);
		Log.println("Decoder CHANNELS: " + channels);
		stereo = true;
		if (audioSource.audioFormat.getChannels() == FoxDecoder.MONO) stereo = false;

        abData = new byte[BUFFER_SIZE];  
        filteredData = new byte[BUFFER_SIZE];  
		abBufferDouble = new double[BUFFER_SIZE /bytesPerSample];
		abBufferDoubleFiltered = new double[BUFFER_SIZE /bytesPerSample];
		
		int nBytesRead = 0;
        
        if (Config.filterData)
        	if (filter == null) 
        		Log.println("FILTER: none");
        	else
        		Log.println("FILTER: " + filter.toString()); //filters[Config.useFilterNumber].toString());
        Log.println("BUFFER: " + BUFFER_SIZE);
        //Log.println("DECODING FRAMES LENGTH " + foxBitStream.SYNC_WORD_DISTANCE + " bits ... ");
        
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

                if (filter != null && Config.filterData) { // && !Config.highSpeed) {
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

        	//if (!clockLocked) {
        		byte[] clockAdvance;
        		clockAdvance = recoverClock(1);
        		if (clockAdvance != null && Config.recoverClock ) {
        			rebucketData(filteredData, clockAdvance);    				
        		}
        	//}
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
	
	
	/**
	 * Divide the sampled data into buckets, based on the perfect clock
	 * @param abData
	 */
	protected void bucketData(byte[] abData) {
		int k = 0; // position in the data stream where we read data
		int minValue = 256*256;
		int maxValue = 0;
		
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			minValue = 256*256;
			maxValue = 0;
			
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

					if (value > maxValue) maxValue = value;
					if (value < minValue) minValue = value;
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

					if (value > maxValue) maxValue = value;
					if (value < minValue) minValue = value;
					k=k+2; // move forward 2
				
				}
			}
		
			averageMax = averageMax + maxValue;
			averageMin = averageMin + minValue;
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
	public AudioFormat getAudioFormat() { return audioSource.getAudioFormat(); }
	public int getCurrentSampleRate() { return currentSampleRate; }
	public int getFramesDecoded() { return framesDecoded; }
	public int getBitsPerSample() { return bitsPerSample; }
	public int getSampleWindowLength() { return SAMPLE_WINDOW_LENGTH; }

	public void stopProcessing() {
		cleanup();
		processing = false;
		Log.println("DECODER STOPPING");
	}

	public boolean isDone() { return done; }

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

	protected int readBytes(byte[] abData) {
		int nBytesRead = 0;
		//Log.println("Reading bytes from channel: " + audioChannel);
		nBytesRead = audioSource.readBytes(abData, audioChannel);	
		return nBytesRead;
	}


	@Override
	public void run() {
		Log.println("DECODER Start");
		try {
			process();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace(Log.getWriter());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			JOptionPane.showMessageDialog(MainWindow.frame,
					sw.toString(),
					"ERROR",
					JOptionPane.ERROR_MESSAGE) ;
		}catch (IOException e) {
			e.printStackTrace(Log.getWriter());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			JOptionPane.showMessageDialog(MainWindow.frame,
					sw.toString(),
					"ERROR",
					JOptionPane.ERROR_MESSAGE) ;	
		} 

		catch (NullPointerException e) {
			// ONLY CATCH THIS IN PRODUCTION VERSION	
			e.printStackTrace(Log.getWriter());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			JOptionPane.showMessageDialog(MainWindow.frame,
					sw.toString(),
					"FATAL ERROR IN DECODER",
					JOptionPane.ERROR_MESSAGE) ;

		}
		Log.println("DECODER Exit");
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
