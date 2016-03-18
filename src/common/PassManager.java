package common;

import gui.MainWindow;
import gui.SourceTab;

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
	
	SourceTab inputTab; // the source tab that called this.  For callbacks when decoder live
	SatelliteManager satelliteManager;
	ArrayList<Spacecraft> spacecraft;
	boolean running = true;
	boolean done = false;
	PassMeasurement passMeasurement; // the paramaters we have measured about the current pass
	PassParams pp1;
	PassParams pp2;
	
	public static final int INIT = 0;
	public static final int SCAN = 1;
	public static final int ANALYZE = 2;
	public static final int START_PASS = 3;
	public static final int DECODE = 4;
	public static final int FADED = 5;
	public static final int END_PASS = 6;
	public static final int EXIT = 7;

	static final String[] stateName = {"Scanning", "Scanning", "Analyze", "Scanning", "Decode", "Faded", "Scanning", "Scanning"}; 

	// The reset and uptime fromm the last frame that was received
	int lastReset;
	long lastUptime;
	
	final int SCAN_PERIOD = 250; //ms - we always do this
	final int ANALYZE_PERIOD = 500; //ms - we pause for this if strongest signal > scan signal threshold 
	final int SNR_PERIOD = 1500; //ms - we pause for this if rfAvg signal > analyze threshold.  We then measure the Bit SNR
	final int DECODE_PERIOD = 5000; //ms
	final int FADE_PERIOD = 125 * 1000; //ms - need to wait for the length of a beacon to see if this is still a pass
	
	private int state = INIT;
	private boolean newPass = false; // true if we are starting a new pass and need to be give the reset/uptime
	private boolean faded = false;
	private boolean pendingTCA = false; // True if we have a TCA measurement that needs to be sent to the server
	
	
	
	static final int MIN_FREQ_READINGS_FOR_TCA = 10;
	
	public PassManager(SatelliteManager satMan ) {
		satelliteManager = satMan;
		spacecraft = satMan.spacecraftList;
		pp1 = new PassParams();
		pp2 = new PassParams();
	}

	public void setDecoder1(Decoder d, SourceIQ iq, SourceTab in) {
		pp1.decoder = d;		
		pp1.iqSource = iq;	
		inputTab = in;
	}
	
	public void setDecoder2(Decoder d, SourceIQ iq, SourceTab in) {
		pp2.decoder = d;		
		pp2.iqSource = iq;		
		inputTab = in;
	}

	public boolean isDone() { return done; }
	public boolean isNewPass() { return newPass; }
	public boolean isFaded() { return faded; }
	public boolean hasTCA() { return pendingTCA; }
	public void sentTCA() { pendingTCA = false; }
	public PassMeasurement getPassMeasurement() { return passMeasurement; }
	public String getStateName() { return stateName[state];}
	public int getState() { return state; }
	public boolean inPass() {
		if (state == DECODE) return true;
		if (state == FADED) return true;		
		return false;
	}
	
	/**
	 * This initializes the pass measurement
	 * @param reset
	 * @param uptime
	 */
	public void setStartResetUptime(int id, int reset,long uptime) {
		passMeasurement.setStartResetUptime(reset, uptime);
		newPass = false;
	}

	public void setLastResetUptime(int id, int reset, long uptime) {
		lastReset = reset;
		lastUptime = uptime;
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
	
	private void setFreqRangeBins(Spacecraft sat, PassParams pp) {
		if (pp.decoder != null && pp.iqSource != null) {
			Config.toBin = pp.iqSource.getBinFromFreqHz(sat.maxFreqBoundkHz*1000);
			Config.fromBin = pp.iqSource.getBinFromFreqHz(sat.minFreqBoundkHz*1000);
			
		}
	}
	
	private void initParams(PassParams pp) {
		if (pp.decoder != null && pp.iqSource != null) {
			if (Config.debugSignalFinder) Log.println("Initialized Pass Params for: " + pp.decoder.name);
			pp.rfData = pp.iqSource.getRfData();
			if (pp.rfData != null)
				pp.rfData.reset(); // new satellite to scan so reset the data
			pp.resetEyeData();
		}

		
	}
	
	private int init(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering INIT state");
		faded = false;
		if (pp1.decoder != null) {  // if the start button is pressed then Decoder1 must be none null
			setFreqRangeBins(sat, pp1);
			initParams(pp1);
			if (pp2.decoder != null) {
				setFreqRangeBins(sat, pp2);
				initParams(pp2);
			}
			return SCAN;
		}
		return EXIT; // The start button is not pressed
	}
	
	
	/**
	 * In scan mode we check the strongest signal in the scan range for this satellite.
	 * If two decoders are running then we only need to check the signal strength in Decoder 1
	 * @return
	 */
	private int scan(Spacecraft sat) {
		//Log.println("Scanning for "+sat.getIdString()+" from: " + fromFreq + " - " + toFreq);
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering SCAN state");
		
		if (pp1.iqSource != null) {
			// Wait for a while to gather data
			if (Config.debugSignalFinder) Log.println("Scanning..");
			try {
				Thread.sleep(SCAN_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Make sure we are looking at the right frequency range, then measure the signal (we should not have to check this, but sometimes
			// the average has not completed.  If we line all of the timings up, this should not happen.
			if (pp1.rfData != null) {
				//System.out.println("..Checking RF Data");
				if (Config.fromBin < pp1.rfData.getBinOfStrongestSignal() && Config.toBin > pp1.rfData.getBinOfStrongestSignal()) {
					//double strongestSignal = pp1.rfData.getAvg(RfData.STRONGEST_SIG);
					if (Config.debugSignalFinder) Log.println(sat.getIdString() + " STRONG SIG:" + pp1.rfData.strongestSigRfSNR);
					if (pp1.rfData != null && pp1.rfData.strongestSigRfSNR > Config.SCAN_SIGNAL_THRESHOLD) {
						return ANALYZE;
					}
				}
			}
		}
		return EXIT;

	}

	/**
	 * In analyze mode we see if the signal looks like a signal from a satellite by measuring the RF SNR
	 * The RF SNR is measured in decoder 1, even if we have two decoders.
	 * @return
	 */
	private int analyzeSNR(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Entering ANALYZE state");
		MainWindow.inputTab.fftPanel.setFox(sat);
		if (Config.debugSignalFinder) Log.println(sat.foxId + " Setting Bin to: " + pp1.rfData.getBinOfStrongestSignal());
		Config.selectedBin = pp1.rfData.getBinOfStrongestSignal();
		pp1.rfData.reset(); // because we changed frequency

		if (pp1.rfData != null) {
			try {
				Thread.sleep(ANALYZE_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (foundRfSignal(sat, pp1)) {
				// We have a signal
				return START_PASS;			
			} else {
				//Log.println("Only SNR of " + eyeData.bitSNR);
			}
		}
		return EXIT;
	}

	private boolean foundRfSignal(Spacecraft sat, PassParams pp) {
		//System.out.println(sat.getIdString() + " RF SIG:" + rfData.rfSNR);
		if (pp.rfData != null && pp.rfData.rfSNR > Config.ANALYZE_SNR_THRESHOLD) {
			// We have a signal
			if (Config.debugSignalFinder) Log.println("Found Candiate Signal from " + sat.getIdString());
			Config.selectedBin = pp.rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly
			
			return true;
		}
		return false;
	}

	
	/**
	 * We check if we should start the pass by measuring the BIT SNR.  If there are two deocders we need to do this in
	 * both.  If either has a FOX Signal then we Save the AOS and begin decode
	 * @param sat
	 * @return
	 */
	private int startPass(Spacecraft sat) {
		if (!Config.findSignal) return EXIT;
		if (pp1.decoder != null) { // start button is still pressed
			
			try {
				Thread.sleep(SNR_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (foundFoxSignal(sat, pp1)) {
				// We have a signal
				lockSignal(sat, pp1);
				return DECODE;
			}
			if (pp2 != null)
			if (foundFoxSignal(sat, pp2)) {
				// We have a signal
				lockSignal(sat, pp2);
				return DECODE;
			}
		}
		return EXIT;
	}

	private void lockSignal(Spacecraft sat, PassParams pp) {
//		Config.selectedBin = pp.rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly
		passMeasurement = new PassMeasurement(sat.foxId, SatMeasurementStore.PASS_MEASUREMENT_TYPE);
		if (Config.debugSignalFinder) Log.println("AOS for Fox-" + sat.foxId + " at " + passMeasurement.getRawValue(PassMeasurement.AOS) 
				+ " with " + pp.decoder.name + " decoder bin:" + Config.selectedBin);
		newPass = true;
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
		setFreqRangeBins(sat, pp1);
		if (pp2 != null)
		setFreqRangeBins(sat, pp2);

		if (foundFoxSignal(sat, pp1)) {
//			lockSignal(sat, pp1);
			inputTab.setViewDecoder1();
			return DECODE;
		}
		if (pp2 != null)
		if (foundFoxSignal(sat, pp2)) {
//			lockSignal(sat, pp2);
			inputTab.setViewDecoder2();
			return DECODE;
		}
		return FADED;
	}

	private boolean foundFoxSignal(Spacecraft sat, PassParams pp) {
		
		if (Config.findSignal && pp.rfData != null && pp.decoder != null && pp.eyeData != null) {
			
			//Log.println("Getting eye data");
			pp.eyeData = pp.decoder.eyeData;
			//System.out.println(sat.getIdString() + " BIT SNR:" + eyeData.bitSNR);
			if (pp.eyeData != null && pp.eyeData.bitSNR > Config.BIT_SNR_THRESHOLD) {
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
			setFreqRangeBins(sat,pp1);

			if (Config.findSignal && pp1.decoder != null ) { // start button still pressed and still tracking
				//Log.println("Getting eye data");
				//if (foundRfSignal(sat))
				if (foundFoxSignal(sat, pp1)) {
					// We have a signal
					Config.selectedBin = pp1.rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly, in case we were slightly off
					pp1.rfData.reset();
					faded = false;
					inputTab.setViewDecoder1();
					return DECODE;
				}
				if (pp2 != null)
				if (foundFoxSignal(sat, pp2)) {
					// We have a signal
					Config.selectedBin = pp2.rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly, in case we were slightly off
					pp2.rfData.reset();
					faded = false;
					inputTab.setViewDecoder2();
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
		int MAX_QUANTITY = 999; // get all of them.  We will never have this many for a pass
		if (passMeasurement.getReset() == 0 && passMeasurement.getUptime() == 0) {
			// We did not get any readings
			passMeasurement.setRawValue(PassMeasurement.TOTAL_PAYLOADS, 0);
			passMeasurement.setEndResetUptime(0, 0);
		} else {
			graphData = Config.payloadStore.getMeasurementGraphData(RtMeasurement.CARRIER_FREQ, MAX_QUANTITY, sat, passMeasurement.getReset(), passMeasurement.getUptime());

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

				// We need to make sure that the minimum was not the first or last point, which would indicate that we did not find an inflection
				if (max != 0 && max != graphData[0].length) {
					// we found a maximum at an inflection point, rather than it being at one end or the other
					// Interpolate between the two frequencies to find the actual frequency at the max slope
					long up = (long) (graphData[PayloadStore.UPTIME_COL][max] + graphData[PayloadStore.UPTIME_COL][max-1])/2;
					long date = (long) (graphData[PayloadStore.UTC_COL][max] + graphData[PayloadStore.UTC_COL][max-1])/2;

					long tca = (long) linearInterpolation(up, graphData[PayloadStore.UPTIME_COL][max], graphData[PayloadStore.UPTIME_COL][max-1],
							graphData[PayloadStore.DATA_COL][max], graphData[PayloadStore.DATA_COL][max-1]);

					// FIXME
					// We should check that the maxDeriv is large enough to indicate it was actually the inflection point and not just
					// noise or random perturbations

					pendingTCA = true;
					passMeasurement.setTCA(date);
					passMeasurement.setRawValue(PassMeasurement.TCA_FREQ, tca);
					if (Config.debugSignalFinder) Log.println("TCA calculated as " + passMeasurement.getRawValue(PassMeasurement.TCA) + " with Uptime " + up + " and frequency: " + Long.toString(tca));


				}

			} else {
				if (Config.debugSignalFinder) Log.println("Can't calculate TCA, not enough readings");
			}
			passMeasurement.setRawValue(PassMeasurement.TOTAL_PAYLOADS, graphData[0].length);
			passMeasurement.setEndResetUptime(lastReset, lastUptime);
			//FIXME
			// Store the start and end azimuith - these are a RtMeasurement.  Need to grab the first and last one from the Decoder
			// store the max elevation - this requires a search like the TCA.  But this is only from SatPC32, so why bother?  Perhaps Elevation at TCA is more interesting.
			// the END RESET and UPTIME is captured but not written to disk or used in any way
		}
	}

	private double linearInterpolation(double x, double x0, double x1, double y0, double y1) {
		double y = y0 + (y1 - y0) * ((x - x0)/(x1 - x0));
		return y;
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
			if (pp1.decoder != null && Config.findSignal)
				for (int s=0; s < spacecraft.size(); s++) {
					//Log.println("Looking for: " + spacecraft.get(s).name);
					if (spacecraft.get(s).track)
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
