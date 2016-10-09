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
import telemetry.FoxFramePart;
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
 * Fox200bpsDecoder - the DUV decoder
 * Fox9600bpsDecoder - the High Speed decoder
 * 
 * This class contains the methods needed to sample the audio stream and determine if each bucket contains a 1 or a 0.  
 * 
 * The sampled bits are passed to the BitStream which holds all of the logic needed to find the start and end of
 * frames and to decode the 10 bit words into 8 bit bytes. It then calls the routines to decode the frame and add it to the
 * database
 * 
 * This can be configured for either high speed or slow speed telemetry.
 *  
 * 
 * @author chris.e.thompson
 *
 */
public abstract class FoxDecoder extends Decoder {
	/**
     * This holds the stream of bits that we have not decoded. Once we have several
     * SYNC words, this is flushed of processed bits.
     */
    protected FoxBitStream foxBitStream = null;  // Hold bits until we turn them into decoded frames
	
	public static final int BIT_DISTANCE_THRESHOLD_PERCENT = 15; // use 20 for 736R audio *****15; // Distance that bits need to be apart to change the bit decision as % of average BIT HEIGHT
														
	protected int currentFilterLength = 0;
	protected double currentFilterFreq = 0d;
    
    private int lastBitValue = 0; // store the value of the last bit for use in the bit detection algorithm
    private boolean lastBit = false;
	    
    private long lastLoopTime = 0; // loop timer to slow down execution if we want to simulate decoding from a file
    private long OPTIMAL_TIME = 0; // scaling factor for loop timer
    private Frame decodedFrame = null;
    
    public Filter monitorFilter = null;
    
    /**
     * Given an audio source, decode the data in it,
     */
	public FoxDecoder(String n, SourceAudio as, int chan) {
		super(n,as,chan);
		init();
	}
	

	/**
	 * This is called each time the decoder is started from the GUI
	 */
	public void init() {
		Performance.setEnabled(Config.debugPerformance);  // enable performance logging (or not)
		
		BUFFER_SIZE = bytesPerSample * SAMPLE_WINDOW_LENGTH * bucketSize;

		// Timing for each loop in milli seconds
		OPTIMAL_TIME = 500*SAMPLE_WINDOW_LENGTH/BITS_PER_SECOND;
		
		
		
		initWindowData();
//		agcFilter = new AGCFilter();
		monitorFilter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE /bytesPerSample);
		monitorFilter.init(currentSampleRate, 3000, 256);
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
		decodedFrame = foxBitStream.findFrames();
		//Performance.endTimer("findFrames");
		if (decodedFrame != null && !decodedFrame.corrupt) {
			Performance.startTimer("Store");
			// Successful frame
			eyeData.lastErasureCount = foxBitStream.lastErasureNumber;
			eyeData.lastErrorsCount = foxBitStream.lastErrorsNumber;
			//eyeData.setBER(((bitStream.lastErrorsNumber + bitStream.lastErasureNumber) * 10.0d) / (double)bitStream.SYNC_WORD_DISTANCE);
			if (Config.storePayloads) {
				if (decodedFrame instanceof SlowSpeedFrame) {
					SlowSpeedFrame ssf = (SlowSpeedFrame)decodedFrame;
					FoxFramePart payload = ssf.getPayload();
					SlowSpeedHeader header = ssf.getHeader();
					if (Config.storePayloads) Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
					
					// Capture measurements once per payload or every 5 seconds ish
					addMeasurements(header, decodedFrame);
					if (Config.autoDecodeSpeed)
						MainWindow.inputTab.setViewDecoder1();  // FIXME - not sure I should call the GUI from the DECODER, but works for now.
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
					if (Config.autoDecodeSpeed)
						MainWindow.inputTab.setViewDecoder2();
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
			//clockLocked = false;
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
		rtMeasurement.setErrors(foxBitStream.lastErrorsNumber);
		rtMeasurement.setErasures(foxBitStream.lastErasureNumber);
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
			foxBitStream.addBit(middleSample[i]);
		}
		
		Performance.startTimer("debugValues");

    	if (Config.debugValues /*&& framesDecoded == writeAfterFrame*/) {
   /////// 		//if (Config.debugBytes) printBytes(abData);
    		//    			if (debugBytes) printBuckets();
    		//if (frameMarkerFound)  
    		printBucketsValues();
    		//printByteValues();
    	}
    	//if (Config.debugValues) count ++;
    	//if (count == 3) System.exit(0); /// DROP OUT FOR TEST PURPOSES
    	//if (Config.DEBUG_COUNT != -1 && count > Config.DEBUG_COUNT)
    	//	nBytesRead = -1; /// DROP OUT FOR TEST PURPOSES

    	Performance.endTimer("debugValues");

    	Performance.startTimer("findSync");
    	boolean found = foxBitStream.findSyncMarkers(SAMPLE_WINDOW_LENGTH);
    	Performance.endTimer("findSync");
    	if (found) {
    		processPossibleFrame();
    	}
    	//windowNumber++;

    	
        //Performance.startTimer("Yield");
    	/*if (Config.realTimePlaybackOfFile) {
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
    	*/
        
		//Performance.endTimer("Yield");
	}

	int rd = 0;
	int nextRd = 0;
	protected void debugBitsWindow() {
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i+=10) {
			boolean[] b = new boolean[10];
			boolean[] b8 = new boolean[8];
			for (int k=0; k<10; k++)
				b[k] = middleSample[i+k];
			
			int word = FoxBitStream.binToInt(b);
			rd = Code8b10b.getRdSense10b(word, Config.flipReceivedBits);
			FoxBitStream.printBitArray(b);
			System.out.println("Expected: " + nextRd + " rd: " + rd);
			nextRd = Code8b10b.getNextRd(word, Config.flipReceivedBits);
			byte word8b;
			try {
				word8b = Code8b10b.decode(word, Config.flipReceivedBits);
				Log.print(i+ ": 10b:" + FoxDecoder.hex(word));	
				
				Log.print(" 8b:" + FoxDecoder.hex(word8b));
				Log.println("");
				b8 = FoxBitStream.intToBin8(word8b);
				FoxBitStream.printBitArray(b8);
			} catch (LookupException e) {
				Log.print(i+ ": 10b:" + FoxDecoder.hex(word));	
				Log.print(" 8b: -1");
				Log.println("");
			}
		}
	}
	
	
	
	

	/**
	 * Check the correlation between the clock and the data.  Determine if we should pull the data forward and by how much
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
	

	
	

}
