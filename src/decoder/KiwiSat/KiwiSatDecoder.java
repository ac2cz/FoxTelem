package decoder.KiwiSat;

import java.io.IOException;

import common.Config;
import common.Log;
import common.Performance;
import decoder.Fox9600bpsDecoder;
import decoder.FoxDecoder;
import decoder.SourceAudio;
import decoder.FoxBPSK.FoxBPSKBitStream;
import filter.RaisedCosineFilter;
import gui.MainWindow;
import telemetry.Frame;
import telemetry.FoxBPSK.FoxBPSKFrame;
import telemetry.FoxBPSK.FoxBPSKHeader;

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
 * This is the KiwiSat Decoder, based on the High Speed Decoder
 * 
 */
public class KiwiSatDecoder extends Fox9600bpsDecoder {
	public static final int HIGH_SPEED_BITS_PER_SECOND = 9600;
	public static final int WORD_LENGTH = 8; // 8 data bits 1 stop bit No parity.  Start is 0, then 8 data bits, then Stop is 1 - https://en.wikipedia.org/wiki/Asynchronous_serial_communication
	public static final int SYNC_WORD_LENGTH = 8;  // This will be the KISS FRAME Start/End
	
	public KiwiSatDecoder(SourceAudio as, int chan) {
		super(as, chan);
	}

	@Override
	public void init() {
		Log.println("Starting KiwiSat Decoder - 9600bps RAW");
		super.init();
	}

	@Override
	protected void setHighSpeedParameters() {
		foxBitStream = new KiwiSatBitStream(this, WORD_LENGTH, SYNC_WORD_LENGTH);
		BITS_PER_SECOND = HIGH_SPEED_BITS_PER_SECOND;
		bucketSize = currentSampleRate / BITS_PER_SECOND;
		SAMPLE_WIDTH = 1;
		SAMPLE_WINDOW_LENGTH = 100; 
		CLOCK_TOLERANCE = 10;
		CLOCK_REOVERY_ZERO_THRESHOLD = 10;
	}
	
	
	/**
	 * Decode KiwiSat Frames and save to the payload store
	 */
	@Override
	protected void processPossibleFrame() {

		//Performance.startTimer("findFrames");
		decodedFrame = foxBitStream.findFrames();
		//Performance.endTimer("findFrames");
		if (decodedFrame != null && !decodedFrame.corrupt) {
			Performance.startTimer("Store");
			// Successful frame
			if (Config.storePayloads) {
				KiwiSatFrame hsf = (KiwiSatFrame)decodedFrame;
				KiwiSatHeader header = hsf.getHeader();
				KiwiSatTelemetryPayload payload = hsf.getPayload();
				Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
				
				// Capture measurements once per payload or every 5 seconds ish
				addMeasurements(header, decodedFrame, foxBitStream.lastErrorsNumber, foxBitStream.lastErasureNumber);
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
			framesDecoded++;
			Performance.endTimer("Store");
		} else {
			if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
		}
	}
	
	int dCount = 0;
	boolean lastState = true;
	Scrambler scrambler = new Scrambler();
	/**
	 * KiwiSAT uses HDLC framing.  This is NRX-I encoded such that a logical zero is marked by a change of state. No change of state means
	 * a 1.
	 * The bits are also run through a scrambler per the G3RUH spec.  
	 */
	protected void sampleBuckets() {
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			sampleNumber++;
			int sampleSum = 0;
			int samples = 0;
			for (int s=bucketSize/2-SAMPLE_WIDTH; s <= bucketSize/2+SAMPLE_WIDTH; s++) {
				sampleSum = sampleSum + dataValues[i][s];
				samples++;				
			}
			sampleSum = sampleSum/samples; // get the average value
			boolean currentState;
			
			if (sampleSum >= zeroValue) {
				currentState = true;
				eyeData.setHigh(sampleSum);
			} else {
				currentState = false;
				eyeData.setLow(sampleSum);
			}
			
			if (currentState == lastState) {
				middleSample[i] = scrambler.decode(true);
			} else {
				middleSample[i] = scrambler.decode(false);
			}
			lastState = currentState;
			
			//DEBUG
			//if (middleSample[i]) System.out.print(1); else System.out.print(0);
			//dCount++;
			//if (dCount % 40 == 0) System.out.println();
			//if (dCount > 2048) System.exit(0);
		}
	}
	
}
