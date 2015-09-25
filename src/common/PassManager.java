package common;

import gui.MainWindow;

import java.util.ArrayList;
import telemetry.PayloadStore;
import measure.PassMeasurement;
import measure.RtMeasurement;
import measure.SatMeasurementStore;
import decoder.Decoder;
import decoder.EyeData;
import decoder.RfData;
import decoder.SourceIQ;

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
 * A class that monitors for the satellites that we have loaded and sets the frequency (where it can) so that we can monitor the whole fleet for telemetry.
 * 
 * Even if we are only monitoring one satellite and setting the frequency with another program, this class will flag the start and end of a pass (AOS and LOS).
 * 
 * We implement the pass analysis as a state machine.  The run method loops through each satellite and starts a state machine to analyze
 * any signals.  If it finds a signals from FOX then the frequency is maintained while the decoder runs. It also logs the results as a Pass in a PassMeasurement,
 * including the start frequency - the Fox Tail - and the TCA, calculated at the end.
 * STATES:
 * 
 * INIT - We have just started looking at a new satellite.  Set the SCAN range and get the strongest signal in that range
 * SCAN - Check the peak is greater than THRESHOLD and ANALYZE else EXIT
 * ANALYZE - Wait a short period and measure the RF SNR.  If > ANALYZE_SNR_THRESHOLD, START_PASS else EXIT 
 * START_PASS - Save AOS and begin DECODE
 * DECODE - while BIT_SNR > BIT_SNR_THRESHOLD continue DECODE else FADED
 * FADED - Cache AOS, in case we are done.  While in FADE_PERIOD measure SNR.  If RF_SNR > ANALYZE_SNR_THRESHOLD continue DECODE.  Otherwise END_PASS
 * END_PASS - Save AOS.  Measure the TCA if possible and SAVE, then EXIT
 * EXIT - Done with this satellite, proceed to next
 *  
 * 
 * @author chris.e.thompson
 *
 */
public class PassManager implements Runnable {
	
	SourceIQ iqSource;
	Decoder decoder;
	SatelliteManager satelliteManager;
	ArrayList<Spacecraft> spacecraft;
	boolean running = true;
	boolean done = false;
	RfData rfData;
	EyeData eyeData;
	PassMeasurement passMeasurement; // the paramaters we have measured about the current pass
	
	
	public static final int INIT = 0;
	public static final int SCAN = 1;
	public static final int ANALYZE = 2;
	public static final int START_PASS = 3;
	public static final int DECODE = 4;
	public static final int FADED = 5;
	public static final int END_PASS = 6;
	public static final int EXIT = 7;

	static final String[] stateName = {"Scanning", "Scanning", "Analyze", "Scanning", "Decode", "Faded", "Scanning", "Scanning"}; 

	
	final int SCAN_PERIOD = 250; //ms - we always do this
	final int ANALYZE_PERIOD = 500; //ms - we pause for this if strongest signal > scan signal threshold 
	final int SNR_PERIOD = 1500; //ms - we pause for this if rfAvg signal > analyze threshold.  We then measure the Bit SNR
	final int DECODE_PERIOD = 5000; //ms
	final int FADE_PERIOD = 125 * 1000; //ms - need to wait for the length of a beacon to see if this is still a pass
	
	private int state = INIT;
	private boolean newPass = false; // true if we are starting a new pass and need to be give the reset/uptime
	private boolean faded = false;
	
	static final double SCAN_SIGNAL_THRESHOLD = 20d; // This is peak signal to average noise.  Strongest signal needs to be above this
	static final double ANALYZE_SNR_THRESHOLD = 6d; // This is average signal in the pass band to average noise outside the passband
	static final double BIT_SNR_THRESHOLD = 2d; 
	
	static final int MIN_FREQ_READINGS_FOR_TCA = 10;
	
	public PassManager(SatelliteManager satMan) {
		satelliteManager = satMan;
		spacecraft = satMan.spacecraftList;
	}

	public void setDecoder(Decoder d, SourceIQ iq) {
		decoder = d;		
		iqSource = iq;
		
	}

	public boolean isDone() { return done; }
	public boolean isNewPass() { return newPass; }
	public boolean isFaded() { return faded; }
	public String getStateName() { return stateName[state];}
	public int getState() { return state; }
	
	/**
	 * This initializes the pass measurement
	 * @param reset
	 * @param uptime
	 */
	public void setStartResetUptime(int id, int reset,long uptime) {
		passMeasurement.setStartResetUptime(reset, uptime);
		newPass = false;
	}
	
	private void stateMachine(Spacecraft sat) {
		state = INIT;
		while (state != EXIT)
			nextState(sat);
	}
	
	private void nextState(Spacecraft sat) {

		switch (state) {
		case INIT:
			state = init(sat);
			break;
		case SCAN:
			state = scan(sat);
			break;
		case ANALYZE:
			state = analyzeSNR(sat);
			break;
		case START_PASS:
			state = startPass(sat);
			break;
		case DECODE:
			state = decode(sat);
			break;
		case FADED:
			state = faded(sat);
			break;
		case END_PASS:
			state = endPass(sat);
			break;
		case EXIT:
			state = exit(sat);
			break;
		default:
			break;
		}

	}
	
	private void setFreqRangeBins(Spacecraft sat) {
		if (decoder != null && iqSource != null) {
			Config.toBin = iqSource.getBinFromFreqHz(sat.maxFreqBoundkHz*1000);
			Config.fromBin = iqSource.getBinFromFreqHz(sat.minFreqBoundkHz*1000);
		}
	}
	
	private int init(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering INIT state");
		faded = false;
		if (decoder != null && iqSource != null) {
			setFreqRangeBins(sat);

			rfData = iqSource.getRfData();
			if (rfData != null)
				rfData.reset(); // new satellite to scan so reset the data
//			Log.println(sat.foxId + " From Bin:" + Config.fromBin);
//			Log.println(sat.foxId + " To Bin:" + Config.toBin);
			resetEyeData();
			return SCAN;
		}
		return EXIT;
	}
	
	private void resetEyeData() {
		eyeData = decoder.eyeData;
		if (eyeData != null)
			eyeData.reset();
//	
	}
	
	/**
	 * In scan mode we check the strongest signal in the scan range for this satellite
	 * @return
	 */
	private int scan(Spacecraft sat) {
		//Log.println("Scanning for "+sat.getIdString()+" from: " + fromFreq + " - " + toFreq);
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering SCAN state");
		
		if (iqSource != null) {
			// Wait for a while to gather data
			try {
				Thread.sleep(SCAN_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Make sure we are looking at the right frequency range, then measure the signal (we should not have to check this, but sometimes
			// the average has not completed.  If we line all of the timings up, this should not happen.
			if (rfData!= null && Config.fromBin < rfData.getBinOfStrongestSignal() && Config.toBin > rfData.getBinOfStrongestSignal()) {
				//double strongestSignal = rfData.getAvg(RfData.STRONGEST_SIG);
				//System.out.println(sat.getIdString() + " STRONG SIG:" + rfData.strongestSigRfSNR);
				if (rfData != null && rfData.strongestSigRfSNR > SCAN_SIGNAL_THRESHOLD) {
					MainWindow.inputTab.fftPanel.setFox(sat);
					if (Config.debugSignalFinder) Log.println(sat.foxId + " Setting Bin to: " + rfData.getBinOfStrongestSignal());
					Config.selectedBin = rfData.getBinOfStrongestSignal();
					rfData.reset(); // because we changed frequency
					return ANALYZE;
				}
			}
		}
		return EXIT;

	}

	/**
	 * In analyze mode we see if the signal looks like a signal from Fox by measuring the RF and BIT SNR
	 * @return
	 */
	private int analyzeSNR(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering ANALYZE state");

		if (rfData != null) {
			try {
				Thread.sleep(ANALYZE_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (foundRfSignal(sat)) {
				// We have a signal
				return START_PASS;			
			} else {
				//Log.println("Only SNR of " + eyeData.bitSNR);
			}
		}
		return EXIT;
	}

	private boolean foundRfSignal(Spacecraft sat) {
		//System.out.println(sat.getIdString() + " RF SIG:" + rfData.rfSNR);
		if (rfData != null && rfData.rfSNR > ANALYZE_SNR_THRESHOLD) {
			// We have a signal
			if (Config.debugSignalFinder) Log.println("Found Candiate Signal from " + sat.getIdString());
			Config.selectedBin = rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly
			
			return true;
		}
		return false;
	}

	
	/**
	 * Save the AOS and begin decode
	 * @param sat
	 * @return
	 */
	private int startPass(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (decoder != null) {
			
			try {
				Thread.sleep(SNR_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (foundFoxSignal(sat)) {
				// We have a signal
				Config.selectedBin = rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly
				passMeasurement = new PassMeasurement(sat.foxId, SatMeasurementStore.PASS_MEASUREMENT_TYPE);
				if (Config.debugSignalFinder) Log.println("AOS for Fox-" + sat.foxId + " at " + passMeasurement.getRawValue(PassMeasurement.AOS));
				newPass = true;
				return DECODE;
			}
		}
		return EXIT;
	}
	
	/**
	 * In Decode mode we are decoding the signal.  We check to make sure that the signal has not stopped
	 * @return
	 */
	private int decode(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering DECODE state");
		try {
			Thread.sleep(DECODE_PERIOD);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setFreqRangeBins(sat);

		if (foundFoxSignal(sat)) {
			return DECODE;
		}
		return FADED;
	}

	private boolean foundFoxSignal(Spacecraft sat) {
		
		if (decoder != null) {
			//Log.println("Getting eye data");
			eyeData = decoder.eyeData;
			//System.out.println(sat.getIdString() + " BIT SNR:" + eyeData.bitSNR);
			if (eyeData != null && eyeData.bitSNR > BIT_SNR_THRESHOLD) {
				// We have a signal
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Wait to see if we have lost the signal because the satellite faded, or because the pass ended
	 * @param sat
	 * @return
	 */
	private int faded(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		passMeasurement.setLOS(); // store the LOS in case we do not get any more data.
		faded = true;
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Cached LOS as " + passMeasurement.getRawValue(PassMeasurement.LOS));

		long startTime = System.nanoTime()/1000000; // get time in ms
		long fadeTime = 0;
		while (fadeTime < FADE_PERIOD) {
			try {
				Thread.sleep(SNR_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setFreqRangeBins(sat);
			
			if (Config.trackSignal && rfData != null && decoder != null && eyeData != null) {
				//Log.println("Getting eye data");
				if (foundRfSignal(sat))
					if (foundFoxSignal(sat)) {
						// We have a signal
						Config.selectedBin = rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly
						rfData.reset();
						faded = false;
						return DECODE;
					}
			} else {
				return END_PASS;
			}
			long now = System.nanoTime()/1000000; // get time in ms
			fadeTime = now - startTime;

		}
		faded = false;
		return END_PASS;
	}
	
	/**
	 * Record the LOS and calculate the TCA if possible, then exit
	 * @param sat
	 * @return
	 */
	private int endPass(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		calculateTCA(sat);
		if (Config.debugSignalFinder) Log.println(sat.foxId + " LOS at " + passMeasurement.getRawValue(PassMeasurement.LOS));
		Config.payloadStore.add(sat.foxId, passMeasurement);
		return EXIT;
	}


	private void calculateTCA(Spacecraft sat) {
		// Get the frequency data for this pass
		double[][] graphData = null;
		graphData = Config.payloadStore.getMeasurementGraphData(RtMeasurement.CARRIER_FREQ, 999, sat, passMeasurement.getReset(), passMeasurement.getUptime());

		// if we have enough readings, calculate the first derivative
		if (graphData[0].length > MIN_FREQ_READINGS_FOR_TCA) {
			double[] firstDifference = new double[graphData[0].length];

			for (int i=1; i < graphData[0].length; i++) {
				double value = graphData[PayloadStore.DATA_COL][i];
				double value2 = graphData[PayloadStore.DATA_COL][i-1];
				firstDifference[i] = 5 * ((value - value2) / (graphData[PayloadStore.UPTIME_COL][i]-graphData[PayloadStore.UPTIME_COL][i-1]));
			}
			
			// Now we find the point where the doppler is changing fastest. This is a negative slope because the frequency is
			// falling, so we look for the largest negative number, which is the minimum of the first derivative
			int max = 0;
			double maxDeriv = 0;
			for (int i=1; i < graphData[0].length; i++) {
				if (firstDifference[i] < maxDeriv) {
					maxDeriv = firstDifference[i];
					max = i;
				}
			}
			
			if (max != 0 && max != graphData[0].length) {
				// we found a maximum at an inflection point, rather than it being at one end or the other
				long tca = (long) graphData[PayloadStore.DATA_COL][max];
				long up = (long) graphData[PayloadStore.UPTIME_COL][max];
				long date = (long) graphData[PayloadStore.UTC_COL][max];
				
				
				// FIXME
				// We could interpolate here to get a more accurate time than we have from the uptime.
				// We store the actual dateTime in the file, we should be pulling that.
				
				passMeasurement.setTCA(date);
				passMeasurement.setRawValue(PassMeasurement.TCA_FREQ, Long.toString(tca));
				if (Config.debugSignalFinder) Log.println("TCA calculated as " + passMeasurement.getRawValue(PassMeasurement.TCA) + " with Uptime " + up + " and frequency: " + Long.toString(tca));
				passMeasurement.setRawValue(PassMeasurement.TOTAL_PAYLOADS, Integer.toString(graphData[0].length));
				
				//FIXME
				// Store the start and end azimuith
				// store the max elevation
				// store end reset and uptime - this requires ability to get the "latest reset uptime"  which works if we grab the latest RT record from the store for this sat
				
			}
			
		} else {
			if (Config.debugSignalFinder) Log.println("Can't calculate TCA, not enough readings");
		}
	}

	private int exit(Spacecraft sat) {
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering EXIT state");
		passMeasurement = null;
		return EXIT;
	}
	
	public void stopProcessing() {
		running = false;
	}
	
	@Override
	public void run() {
		
		running = true;
		done = false;

		while (running) {
		//	System.err.println("PASS MGR RUNNING");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (decoder != null && Config.findSignal)
				for (int s=0; s < spacecraft.size(); s++) {
					//Log.println("Looking for: " + spacecraft.get(s).name);
					stateMachine(spacecraft.get(s));
				}
			else {
				//Log.println("Waiting for decoder");
				//Config.toBin = Config.DEFAULT_TO_BIN;
				//Config.fromBin = Config.DEFAULT_FROM_BIN;

			}
		}
		Log.println("Pass Manager DONE");
		done = true;
	}
	
	
}
