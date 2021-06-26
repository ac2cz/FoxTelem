package common;

import gui.MainWindow;
import gui.SourceTab;

import telemetry.PayloadStore;
import uk.me.g4dpz.satellite.SatPos;
import measure.PassMeasurement;
import measure.RtMeasurement;
import measure.SatMeasurementStore;
import measure.SatPc32DDE;
import predict.PositionCalcException;
import decoder.Decoder;
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
	//SatelliteManager satelliteManager;
	//ArrayList<Spacecraft> foxSpacecraft;
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
	final int ANALYZE_PERIOD = 450; //ms - we pause for this if strongest signal > scan signal threshold 
	final int SNR_PERIOD = 1800; //ms - we pause for this if rfAvg signal > analyze threshold.  We then measure the Bit SNR.  Needs to be long enough to wait for swoop
	final int DECODE_PERIOD = 2000; //ms
	final int FADE_PERIOD = 125 * 1000; //ms - need to wait for the length of a beacon to see if this is still a pass
	final int PSK_FADE_PERIOD = 2 * 1000; //ms - need to wait only for 1 frame then search again..
	
	private int state = INIT;
	private boolean newPass = false; // true if we are starting a new pass and need to be give the reset/uptime
	private boolean faded = false;
	private boolean pendingTCA = false; // True if we have a TCA measurement that needs to be sent to the server
	
	static final int MIN_FREQ_READINGS_FOR_TCA = 10;
	
	public PassManager( ) {
		//satelliteManager = satMan;
		//foxSpacecraft = satMan.spacecraftList;
		pp1 = new PassParams();
		pp2 = new PassParams();
	}

	public void setDecoder1(Decoder d, SourceIQ iq, SourceTab in) {
		pp1.foxDecoder = d;		
		pp1.iqSource = iq;	
		inputTab = in;
	}
	
	public void setDecoder2(Decoder d, SourceIQ iq, SourceTab in) {
		pp2.foxDecoder = d;		
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
	public void setState(int state) { this.state = state; }
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
	
	public void incCrcFailure() {
		if (passMeasurement != null) {
			long crc = passMeasurement.getRawValue(PassMeasurement.CRC_FAILURES);
			passMeasurement.setRawValue(PassMeasurement.CRC_FAILURES, crc+1);
		}
	}

	public void setLastResetUptime(int id, int reset, long uptime) {
		lastReset = reset;
		lastUptime = uptime;
	}
	
	private void stateMachine(Spacecraft spacecraft) {
		state = INIT;
		while (state != EXIT)
			nextState(spacecraft);
	}
	
	private void nextState(Spacecraft spacecraft) {
		int nextState = state;
		int currentState = state;
		switch (state) {
		case INIT:
			nextState = init(spacecraft);
			break;
		case SCAN:
			nextState = scan(spacecraft);
			break;
		case ANALYZE:
			nextState = analyzeSNR(spacecraft);
			break;
		case START_PASS:
			nextState = startPass(spacecraft);
			break;
		case DECODE:
			nextState = decode(spacecraft);
			break;
		case FADED:
			nextState = faded(spacecraft);
			break;
		case END_PASS:
			nextState = endPass(spacecraft);
			break;
		case EXIT:
			nextState = exit(spacecraft);
			break;
		default:
			break;
		}
		
		// Update the state if it was not changed by an override such as the user clicking on the FFT display
		if (state == currentState) {
			state = nextState;
		} 
	}

	private void setFreqRangeBins(Spacecraft spacecraft, PassParams pp) {
		if (pp.foxDecoder != null && pp.iqSource != null) {
			Config.toBin = pp.iqSource.getBinFromFreqHz((long) (spacecraft.user_maxFreqBoundkHz*1000));
			Config.fromBin = pp.iqSource.getBinFromFreqHz((long) (spacecraft.user_minFreqBoundkHz*1000));
//			if (Config.fromBin > SourceIQ.FFT_SAMPLES/2 && Config.toBin < SourceIQ.FFT_SAMPLES/2) {
//				Config.toBin = 0;
//				Config.fromBin = pp.iqSource.getBinFromFreqHz(spacecraft.minFreqBoundkHz*1000);
//			}
		}
	}
	
	private void initParams(PassParams pp) {
		if (pp.foxDecoder != null && pp.iqSource != null) {
			if (Config.debugSignalFinder) Log.println("Initialized Pass Params for: " + pp.foxDecoder.name);
			pp.rfData = pp.iqSource.getRfData();
			if (pp.rfData != null)
				pp.rfData.reset(); // new satellite to scan so reset the data
			pp.resetEyeData();
		}

		
	}
	
	private int init(Spacecraft spacecraft) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " Entering INIT state");
		faded = false;
		if (pp1.foxDecoder != null) {  // if the start button is pressed then Decoder1 must be none null
			setFreqRangeBins(spacecraft, pp1);
			initParams(pp1);
			if (pp2.foxDecoder != null) {
				//setFreqRangeBins(spacecraft, pp2);
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
				boolean spansDcSpike = false;
				if (Config.toBin < Config.fromBin) {
					// Then we span the central spike.  Not ideal.  We need to check forStrong Sig reading in the two parts of the FFT
					spansDcSpike = true;
				}
				int n = pp1.rfData.getBinOfStrongestSignalInSatBand();
				if ((Config.fromBin < n && n < Config.toBin) 
						|| (spansDcSpike && Config.fromBin < n && n < SourceIQ.FFT_SAMPLES-2) || (spansDcSpike && 0 <= n && n < Config.toBin)) {
				//if (Config.fromBin < pp1.rfData.getBinOfStrongestSignalInSatBand() && Config.toBin > pp1.rfData.getBinOfStrongestSignalInSatBand()) {
					//double strongestSignal = pp1.rfData.getAvg(RfData.STRONGEST_SIG);
					if (Config.debugSignalFinder) Log.println(sat.getIdString() + " STRONG SIG:" + pp1.rfData.rfStrongestSigSNRInSatBand);
					if (pp1.rfData != null && pp1.rfData.rfStrongestSigSNRInSatBand > Config.SCAN_SIGNAL_THRESHOLD) {
					//if (pp1.rfData != null && pp1.rfData.getAvg(RfData.STRONGEST_SIGNAL_IN_SAT_BAND) > Config.SCAN_SIGNAL_THRESHOLD) {
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
	private int analyzeSNR(Spacecraft spacecraft) {
		if (!Config.findSignal) return EXIT;
		if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " Entering ANALYZE state");
		MainWindow.inputTab.fftPanel.setFox(spacecraft);
		if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " Setting Bin to: " + pp1.rfData.getBinOfStrongestSignalInSatBand());
//		if (pp1.iqSource.getMode() != SourceIQ.MODE_PSK_COSTAS) {
			//pp1.iqSource.setSelectedBin(pp1.rfData.getBinOfStrongestSignalInSatBand());
//			Config.selectedBin = pp1.rfData.getBinOfStrongestSignalInSatBand();
//			pp1.iqSource.setSelectedBin(Config.selectedBin);
//			if (pp2 != null && pp2.iqSource != null)
//				pp2.iqSource.setSelectedBin(Config.selectedBin);
		
		// Check for nulls incase we are in the middle of stopping
		if (pp1.rfData != null) {
			int bin = pp1.rfData.getBinOfStrongestSignalInSatBand();
			if (pp1.iqSource != null)
				pp1.iqSource.setSelectedBin(bin);
			if (pp2 != null && pp2.iqSource != null)
				pp2.iqSource.setSelectedBin(bin);

			pp1.rfData.reset(); // because we changed frequency
		}
//		}

		if (pp1.rfData != null) {
			try {
				Thread.sleep(ANALYZE_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (foundRfSignal(spacecraft, pp1)) {
				// We have a signal
				return START_PASS;			
			} else {
				//Log.println("Only SNR of " + eyeData.bitSNR);
			}
		}
		return EXIT;
	}

	private boolean foundRfSignal(Spacecraft spacecraft, PassParams pp) {
		//System.out.println(sat.getIdString() + " RF SIG:" + rfData.rfSNR);
		if (Config.debugSignalFinder) Log.println(spacecraft.getIdString() + " CHECK SNR:" + pp1.rfData.rfSNRInFilterWidth);
		if (pp.rfData != null && pp.rfData.rfSNRInFilterWidth > Config.ANALYZE_SNR_THRESHOLD) {
			// We have a signal
			if (Config.debugSignalFinder) Log.println("Found Candiate Signal from " + spacecraft.getIdString());
//			Config.selectedBin = pp.rfData.getBinOfStrongestSignalInSatBand(); // make sure we are on frequency for it quickly
//			pp1.rfData.reset(); // because we changed frequency
			return true;
		}
		return false;
	}

	
	/**
	 * We check if we should start the pass by measuring the BIT SNR.  If there are two deocders we need to do this in
	 * both.  If either has a FOX Signal then we Save the AOS and begin decode
	 * @param spacecraft
	 * @return
	 */
	private int startPass(Spacecraft spacecraft) {
		if (!Config.findSignal) return EXIT;
		if (pp1.foxDecoder != null) { // start button is still pressed
			
			try {
				Thread.sleep(SNR_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (foundFoxSignal(spacecraft, pp1)) {
				// We have a signal
				lockSignal(spacecraft, pp1);
				return DECODE;
			}
			if (pp2 != null)
			if (foundFoxSignal(spacecraft, pp2)) {
				// We have a signal
				lockSignal(spacecraft, pp2);
				return DECODE;
			}
		}
		return EXIT;
	}

	private void lockSignal(Spacecraft spacecraft, PassParams pp) {
//		Config.selectedBin = pp.rfData.getBinOfStrongestSignal(); // make sure we are on frequency for it quickly

		passMeasurement = new PassMeasurement(spacecraft.foxId, SatMeasurementStore.PASS_MEASUREMENT_TYPE);
		if (Config.useDDEforAzEl) {
			if (Config.satPC != null && Config.satPC.getSatellite() != null)
				if (passMeasurement != null) {
					passMeasurement.setRawValue(PassMeasurement.START_AZIMUTH, (long)Config.satPC.getAzimuth());
				}
		} else if (Config.foxTelemCalcsPosition) {
			if (passMeasurement != null) {
				try {
					passMeasurement.setRawValue(PassMeasurement.START_AZIMUTH, (long) spacecraft.getCurrentPosition().getAzimuth());
				} catch (PositionCalcException e) {
					// Ignore this, not a fatal error
					passMeasurement.setRawValue(PassMeasurement.START_AZIMUTH, (long) 0);
					e.printStackTrace(Log.getWriter()); // put message in the log
				}
			}			
		}

		if (passMeasurement != null && pp != null && pp.foxDecoder != null)
			if (Config.debugSignalFinder) Log.println("AOS for Fox-" + spacecraft.foxId + " at " + passMeasurement.getRawValue(PassMeasurement.AOS) 
				+ " with " + pp.foxDecoder.name + " decoder freq:" + Config.selectedFrequency); //// pp1.iqSource.getSelectedBin());
		newPass = true;
	}
	
	/**
	 * In Decode mode we are decoding the signal.  We check to make sure that the signal has not stopped
	 * @return
	 */
	private int decode(Spacecraft spacecraft) {
		if (!Config.findSignal) return EXIT;
		if (!trackSpacecraft(spacecraft)) return EXIT; // sat is no longer up
		if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " Entering DECODE state");
		try {
			Thread.sleep(DECODE_PERIOD);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setFreqRangeBins(spacecraft, pp1);
		//if (pp2 != null)
		//setFreqRangeBins(spacecraft, pp2);

		if (foundFoxSignal(spacecraft, pp1)) {
//			lockSignal(sat, pp1);
			inputTab.setViewDecoder1();
			return DECODE;
		}
		if (pp2 != null)
		if (foundFoxSignal(spacecraft, pp2)) {
//			lockSignal(sat, pp2);
			inputTab.setViewDecoder2();
			return DECODE;
		}
		return FADED;
	}

	private boolean foundFoxSignal(Spacecraft spacecraft, PassParams pp) {
		
		if (Config.findSignal && pp.rfData != null && pp.foxDecoder != null && pp.eyeData != null) {
			
			// Lock to Bit SNR is faster
			pp.eyeData = pp.foxDecoder.eyeData;
			if (Config.debugSignalFinder)
				Log.println(spacecraft.getIdString() + " BIT SNR:" + pp.eyeData.bitSNR);
			if (pp.eyeData != null && pp.eyeData.bitSNR > Config.BIT_SNR_THRESHOLD) {
				// We have a signal
				return true;
			}

			// But check if we got a decode as this is more definite.  Especially if the bit SNR is set too high
			// This is often a problem for PSK where we can decode signal that has very low SNR
			if (pp.foxDecoder.decodedFrame != null) {
				if (pp.lastFrame != null)
					if (pp.lastFrame.getHeader().uptime - pp.foxDecoder.decodedFrame.getHeader().uptime > 11) {
						return false;
					}
				pp.lastFrame = pp.foxDecoder.decodedFrame;
				return true;
			}
		}
		return false;
	}
	
	private void logEndOfPass(Spacecraft spacecraft) {
		if (passMeasurement != null) {
			passMeasurement.setLOS(); // store the LOS in case we do not get any more data.
			if (Config.useDDEforAzEl) { // store end Azimuth too
				if (Config.satPC != null && Config.satPC.getSatellite() != null) {
					passMeasurement.setRawValue(PassMeasurement.END_AZIMUTH, (long)Config.satPC.getAzimuth());
				}
			} else if (Config.foxTelemCalcsPosition) {
				if (passMeasurement != null) {
					try {
						passMeasurement.setRawValue(PassMeasurement.END_AZIMUTH, (long) spacecraft.getCurrentPosition().getAzimuth());
					} catch (PositionCalcException e) {
						// Ignore this, not a fatal error
						passMeasurement.setRawValue(PassMeasurement.END_AZIMUTH, (long) 0);
						e.printStackTrace(Log.getWriter()); // put message in the log
					}
				}			
			}
			if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " Cached LOS as " + passMeasurement.getRawValue(PassMeasurement.LOS));
		}
	}

	/**
	 * Wait to see if we have lost the signal because the satellite faded, or because the pass ended
	 * @param spacecraft
	 * @return
	 */
	private int faded(Spacecraft spacecraft) {
		if (!Config.findSignal) return EXIT;
		logEndOfPass(spacecraft);
		faded = true;

		long startTime = System.nanoTime()/1000000; // get time in ms
		long fadeTime = 0;
		int fade_period = FADE_PERIOD;
		if (pp1 != null && pp1.iqSource != null) // make sure we did not end the pass
			if (pp1.iqSource.getMode() == SourceIQ.MODE_PSK_COSTAS || pp1.iqSource.getMode() == SourceIQ.MODE_FSK_HS)
				fade_period = PSK_FADE_PERIOD;

		while (fadeTime < fade_period && state == FADED) {
			
			try {
				Thread.sleep(SNR_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setFreqRangeBins(spacecraft,pp1);

			if (Config.findSignal && pp1.foxDecoder != null ) { // start button still pressed and still tracking
				//Log.println("Getting eye data");
				//if (foundRfSignal(sat))
				if (foundFoxSignal(spacecraft, pp1)) {
					// We have a signal
				//	Config.selectedBin = pp1.rfData.getBinOfStrongestSignalInSatBand(); // make sure we are on frequency for it quickly, in case we were slightly off
				//	pp1.rfData.reset();
					faded = false;
					inputTab.setViewDecoder1();
					return DECODE;
				}
				if (pp2 != null)
				if (foundFoxSignal(spacecraft, pp2)) {
					// We have a signal
				//	Config.selectedBin = pp2.rfData.getBinOfStrongestSignalInSatBand(); // make sure we are on frequency for it quickly, in case we were slightly off
				//	pp2.rfData.reset();
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
	 * @param spacecraft
	 * @return
	 */
	private int endPass(Spacecraft spacecraft) {
		if (!Config.findSignal) return EXIT;
		logEndPassMeasurement(spacecraft);
		return EXIT;
	}
	
	private void logEndPassMeasurement(Spacecraft spacecraft) {
		if (passMeasurement != null) {
			calculateTCA(spacecraft);
			calculateMaxEl(spacecraft);
			if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " LOS at " + passMeasurement.getRawValue(PassMeasurement.LOS));
			Config.payloadStore.add(spacecraft.foxId, passMeasurement);
		}
	}

	private void calculateMaxEl(Spacecraft spacecraft) {
		double[][] graphData = null;
		int MAX_QUANTITY = 999; // get all of them.  We will never have this many for a pass
		if (passMeasurement.getReset() == 0 && passMeasurement.getUptime() == 0) {
			// We did not get any readings
			passMeasurement.setRawValue(PassMeasurement.MAX_ELEVATION, 0);
		} else {
			long maxEl = -180; // just in case we have a pass that is theoretically below the horizon but we still manage to track it, allow negatives
			graphData = Config.payloadStore.getMeasurementGraphData(RtMeasurement.EL, MAX_QUANTITY, (FoxSpacecraft) spacecraft, passMeasurement.getReset(), passMeasurement.getUptime(), false);
			for (int i=1; i < graphData[0].length; i++) {
				long value = (long)graphData[PayloadStore.DATA_COL][i];
				if (value > maxEl) maxEl = value;
			}
			passMeasurement.setRawValue(PassMeasurement.MAX_ELEVATION, maxEl);
		}
	}

	private void calculateTCA(Spacecraft spacecraft) {
		// Get the frequency data for this pass
		double[][] graphData = null;
		int MAX_QUANTITY = 999; // get all of them.  We will never have this many for a pass
		if (passMeasurement.getReset() == 0 && passMeasurement.getUptime() == 0) {
			// We did not get any readings
			passMeasurement.setRawValue(PassMeasurement.TOTAL_PAYLOADS, 0);
			passMeasurement.setEndResetUptime(0, 0);
		} else {
			graphData = Config.payloadStore.getMeasurementGraphData(RtMeasurement.CARRIER_FREQ, MAX_QUANTITY, (FoxSpacecraft) spacecraft, passMeasurement.getReset(), passMeasurement.getUptime(), false);

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
					long date = (long) (graphData[SatMeasurementStore.UTC_COL][max] + graphData[SatMeasurementStore.UTC_COL][max-1])/2;

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

	private int exit(Spacecraft spacecraft) {
		if (Config.debugSignalFinder) Log.println(spacecraft.foxId + " Entering EXIT state");
		passMeasurement = null;
		return EXIT;
	}
	
	public void stopProcessing() {
		running = false;
	}
	
	//SatPc32DDE satPC = null;
	boolean satPC32Connected = false;
	
	@Override
	public void run() {

		running = true;
		done = false;
		newPass = false;
		int currentSatId = 0;


		/**
		 * We run in a loop checking each spacecraft that the user has configured for tracking. 
		 * This is configured by the following:
		 *  First the subroutine trackSpacecraft() determines if we should be tracking this spacecraft.  It returns true if:
		 *    sat.track is on and either:
		 *      Config.whenAboveHorizon is on and satPC32DDE is used and the sat is up
		 *      Config.foxTelemCalcsPosition is true and the sat is up
		 *      
		 *  If we should be tracking this spacecraft right now, then it is further defined by:
		 *    Config.findSignal - this determines if we searching for the signal.  It is enabled if any are tracked and we are
		 *      not calculating Doppler
		 *      It controls if the findSignal panel is shown in the inputTab.  It determines if we enter the state machine below
		 *    Config.iq - true if FoxTelem is the SDR.  Otherwise we don't bother with all this, we are in AF mode
		 *    Config.whenAboveHorizon - the decoder is frozen until a spacecraft that is tracked is above the horizon
		 *  
		 *  
		 */
		while (running) {
			//	System.err.println("PASS MGR RUNNING");
			try {
				if (Config.foxTelemCalcsDoppler)
					Thread.sleep(1000);
				if (Config.iq)
					Thread.sleep(100);
				else
					Thread.sleep(5000); // AF mode, just to see if mode needs change, check once per frame
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (Config.useDDEforAzEl || Config.useDDEforFreq) {
				if (Config.satPC == null) {
					Config.satPC = new SatPc32DDE();
				}
				if (Config.satPC != null ) {
					satPC32Connected = Config.satPC.request();
					if (MainWindow.inputTab == null || !MainWindow.inputTab.isStarted()) {
						// No input tab or the decoder is not started.  Throttle the requests to SatPC32
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			//if (Config.findSignal) {
			boolean atLeastOneTracked = false; // false if nothing tracked, might be a user error
			boolean oneSatUp = false; // true if we have a sat above the horizon, so we don't toggle the decoder off

			for (int s=0; s < Config.satManager.spacecraftList.size(); s++) {
				Spacecraft sat = Config.satManager.spacecraftList.get(s);
				if (sat.user_track) atLeastOneTracked = true;
				if (MainWindow.inputTab != null) {

					if (trackSpacecraft(sat)) {
						oneSatUp = true;
						MainWindow.inputTab.startDecoding();
						if (Config.iq) {
							if (((Config.foxTelemCalcsPosition || Config.useDDEforAzEl) && !Config.findSignal) || Config.whenAboveHorizon) {
								// We try to retune if FoxTelem Calc is on and FindSignal is not.
								if (Config.retuneCenterFrequency) {
									if (Config.iq)
										retunedCenterFreqIfNeeded(sat);
									switchedFormatIfNeeded(sat);
								}
							}
							if (Config.findSignal) {
								stateMachine(sat);
								if (Config.whenAboveHorizon)
									break; // only find the signal for the highest priority sat if in autoStart mode
							} else if (Config.foxTelemCalcsPosition && Config.foxTelemCalcsDoppler) {
								if (sat.foxId != currentSatId) { // we have a new pass for a new sat
									lockSignal(sat, pp1);   
								}
								// we set the spacecraft ranges but no find signal panel
								// Doppler is displayed and we tune the signal for the active spacecraft only if up
								// We have to pass a delta from the center frequency to the nco
								double dopplerShiftedFreq = 0;
								if (sat != null && sat.satPos != null)
									dopplerShiftedFreq = sat.user_telemetryDownlinkFreqkHz*1000 + sat.satPos.getDopplerFrequency(sat.user_telemetryDownlinkFreqkHz*1000);
								//DecimalFormat d3 = new DecimalFormat("0.000");
								//System.err.println("Sat: " + sat + d3.format(dopplerShiftedFreq/1000));
								if (dopplerShiftedFreq != 0) {
									setFreqRangeBins(sat, pp1);
									if (pp1 != null && pp1.iqSource != null)
										pp1.iqSource.setTunedFrequency(dopplerShiftedFreq);
									if (pp2 != null && pp2.iqSource != null)
										pp2.iqSource.setTunedFrequency(dopplerShiftedFreq);
								}

								break; // we only tune Doppler for the first spacecraft in the priority ordered list that passes trackSpacecraft(sat)
							} else {
								// we don't have find signal on. set full range or signals calculated incorrectly
								Config.fromBin = 0; 
								Config.toBin = SourceIQ.FFT_SAMPLES;
								break; // this is a mode for the lab, we don't cycle through the spacecraft
							}
						} else { // not in IQ mode, but still may want to switch modes
							if (satIsUp(sat)) {
								if (((Config.foxTelemCalcsPosition || Config.useDDEforAzEl) && !Config.findSignal) || Config.whenAboveHorizon) {
									if (Config.retuneCenterFrequency) {
										switchedFormatIfNeeded(sat);
									}

								}
								break; // only for the highest priority sat that is up
							}

						}

					} else {
						if (currentSatId == sat.foxId) { // close out the pass for a previous sat
							logEndOfPass(sat);
							currentSatId = 0;
						} 
					}
				}
			}
			if (MainWindow.inputTab != null && !oneSatUp) {
				MainWindow.inputTab.stopDecoding();

			}
			if (Config.whenAboveHorizon && !atLeastOneTracked) {
				if (MainWindow.inputTab != null) {
					MainWindow.inputTab.rdbtnFindSignal.setSelected(false);
					Config.whenAboveHorizon = false;
					Log.errorDialog("NO SPACECRAFT TRACKED", "You have paused the decoder waiting for a spacecraft above the horizon,\n"
							+ "but no spacecraft are being tracked.  Toggle 'Not Tracked' to 'Tracked'\n"
							+ "by clicking on it, top right of the input tab.\n"
							+ "'Auto Start Decoder when Above Horizon' will be disabled.");
				}
			}
			//}
		}
		Log.println("Pass Manager DONE");
		done = true;
	}

	private boolean retunedCenterFreqIfNeeded(Spacecraft sat) {
		if (pp1 != null && pp1.iqSource != null) {
			int range = pp1.iqSource.IQ_SAMPLE_RATE/2;
			// For IQ Source we can span +- the rate/2
			// Assume we want the spacecraft to be in the middle of either side band.  ie not across center spike and not at ends
			double maxFreq1 = pp1.iqSource.getCenterFreqkHz() + 0.80 * range/1000.0;
			double aboveCenter = pp1.iqSource.getCenterFreqkHz() + 10;  // within 10kHz of Center 
			double belowCenter = pp1.iqSource.getCenterFreqkHz() - 10;
			double minFreq2 = pp1.iqSource.getCenterFreqkHz() - 0.80 * range/1000.0;
			if (sat.user_telemetryDownlinkFreqkHz < minFreq2 || sat.user_telemetryDownlinkFreqkHz > maxFreq1 ||
					(sat.user_telemetryDownlinkFreqkHz > belowCenter && sat.user_telemetryDownlinkFreqkHz < aboveCenter)) {
				// we need to retune as the sat is outside the current band or too near center spike
				double newCenterFreq = sat.user_telemetryDownlinkFreqkHz - 0.25 * range / 1000;
				if (Config.debugSignalFinder)
					Log.println("Retuning for "+ sat.user_display_name + " downlink: " + sat.user_telemetryDownlinkFreqkHz + " center: " + newCenterFreq);
				MainWindow.inputTab.setCenterFreqKhz(newCenterFreq); // this retunes pp1 and pp2.
				return true;
			}
		}
		return false;
	}

	private boolean switchedFormatIfNeeded(Spacecraft sat) {
		// If the mode is wrong we should switch modes
		if (Config.format != sat.user_format) {
			// Except if in Auto and Sat mode is DUV or HS
			if (MainWindow.inputTab.sdrSelected() && sat.user_format == SourceTab.FORMAT_FSK_AUTO) {
				sat.user_format = SourceTab.FORMAT_FSK_DUV; // we can't switch to auto mode for SDR, pick DUV
				if (Config.format == sat.user_format)
					return false; // we are already in DUV
				// otherwise process change as through the user requested DUV
			}
			
			if (SourceTab.STARTED)
				MainWindow.inputTab.processStartButtonClick(); // stop the decoder
			Config.format = sat.user_format;
			MainWindow.inputTab.setupFormat();
			if (Config.format != sat.user_format) // then user has an override, such as Use Costas, so remember that for this sat
				sat.user_format = Config.format;
			MainWindow.inputTab.processStartButtonClick();
			//}
			return true;
		}
		return false;
	}

	/**
	 * Returns true if aboveHorizon is not set or if the sat is actually above the horizon with our chosen method
	 * Return false if we are not tracking this sat
	 * We run the position calculations regardless so the sat position can be displayed if the user has selected that option.
	 * @return
	 */
	private boolean trackSpacecraft(Spacecraft sat) {
		if (!sat.user_track) return false;
		if (Config.whenAboveHorizon || Config.foxTelemCalcsDoppler)
			return satIsUp(sat);
		return true;
	}

	private boolean satIsUp(Spacecraft sat) {
		if (Config.useDDEforAzEl) {
			String satString = null;
			if (satPC32Connected && Config.satPC != null) {
				satString = Config.satPC.getSatellite();
				//Log.println("SATISUP: " + satString);
			}
			if (satString != null && satString.equalsIgnoreCase(sat.user_keps_name)) {
				if (Config.satPC.getElevation() > 0)
					return true;
			}

			return false;
		}
		if (Config.foxTelemCalcsPosition) {
			// We use FoxTelem Predict calculation, but only if we have the lat/lon set
					SatPos pos = null;
					try {
						pos = sat.getCurrentPosition();
					} catch (PositionCalcException e) {
						return false; // a sat with no T0 or TLE will never be considered above horizon
					}
					if (pos != null) {
						if (sat.aboveHorizon()) {
							return true;
						}
					}
					return false;
		}
		return false;
	}
	
}
