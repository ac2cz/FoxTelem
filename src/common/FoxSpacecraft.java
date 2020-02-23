package common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.joda.time.DateTime;

import predict.PositionCalcException;
import telemServer.ServerConfig;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRtValues;
import telemetry.SortedFramePartArrayList;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

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
 * This class holds any details that are specific to a given spacecraft
 * 
 *
 */
public class FoxSpacecraft extends Spacecraft{
	
	public static final int EXP_EMPTY = 0;
	public static final int EXP_VANDERBILT_LEP = 1; // This is the 1A LEP experiment
	public static final int EXP_VT_CAMERA = 2;
	public static final int EXP_IOWA_HERCI = 3;
	public static final int EXP_RAD_FX_SAT = 4;
	public static final int EXP_VT_CAMERA_LOW_RES = 5;
	public static final int EXP_VANDERBILT_VUC = 6; // This is the controller and does not have its own telem file
	public static final int EXP_VANDERBILT_REM = 7; // This is the controller and does not have its own telem file
	public static final int EXP_VANDERBILT_LEPF = 8; // This is the controller and does not have its own telem file
	public static final int EXP_UW = 9; // University of Washington
	public static final int ADAC = 10; // Ragnaroc
	
	public static final String SAFE_MODE_IND = "SafeModeIndication";
	public static final String SCIENCE_MODE_IND = "ScienceModeActive";
	public static final String MEMS_REST_VALUE_X = "SatelliteXAxisAngularVelocity";
	public static final String MEMS_REST_VALUE_Y = "SatelliteYAxisAngularVelocity";
	public static final String MEMS_REST_VALUE_Z = "SatelliteZAxisAngularVelocity";
	
	public static final int NO_MODE = 0;
	public static final int SAFE_MODE = 1;
	public static final int TRANSPONDER_MODE = 2;
	public static final int DATA_MODE = 3;
	public static final int SCIENCE_MODE = 4;
	public static final int HEALTH_MODE = 5;
	public static final int CAMERA_MODE = 6;
	
	public static final String[] modeNames = {
		"UNKNOWN",
		"SAFE",
		"TRANSPONDER",
		"DATA",
		"SCIENCE",
		"HEALTH",
		"CAMERA"
	};
	
	public static final String[] expNames = {
		"Empty",
		"Vanderbilt LEP",
		"Virgina Tech Camera",
		"University of Iowa HERCI",
		"Rad FX Sat",
		"Virginia Tech Low-res Camera",
		"Vanderbilt VUC",
		"Vanderbilt LEP",
		"Vanderbilt REM",
		"Vanderbilt LEPF",
		"University of Washington Experiment",
		"ADAC"
	};
	
	
	public int IHU_SN = 0;
	public int[] experiments = {EXP_EMPTY, EXP_EMPTY, EXP_EMPTY, EXP_EMPTY};
	
	public boolean hasImprovedCommandReceiver = false;
	public boolean hasModeInHeader = false;
	public boolean hasMpptSettings = false;
	public boolean hasMemsRestValues = false;
	public boolean hasFixedReset = false;
	
	// User settings - Calibration
	public double user_BATTERY_CURRENT_ZERO = 0;
	public double user_mpptResistanceError = 6.58d;
	public int user_mpptSensorOffThreshold = 1600;
	public int user_memsRestValueX = 0;
	public int user_memsRestValueY = 0;
	public int user_memsRestValueZ = 0;
	
	// layout flags
	public boolean useIHUVBatt = false;

	ArrayList<Long> timeZero = null;
	SpacecraftPositionCache positionCache;
	
	public FoxSpacecraft(File masterFileName, File userFileName) throws LayoutLoadException, IOException {
		super(masterFileName, userFileName);
		load(); // don't call load until this constructor has started and the variables have been initialized
		try {
			loadTimeZeroSeries(null);
		} catch (FileNotFoundException e) {
			timeZero = null;
		} catch (IndexOutOfBoundsException e) {
			timeZero = null;
		}
		measurementLayout = new BitArrayLayout(measurementsFileName);
		measurementLayout.name=MEASUREMENTS;
		if (passMeasurementsFileName != null) {
			passMeasurementLayout = new BitArrayLayout(passMeasurementsFileName);
			passMeasurementLayout.name = PASS_MEASUREMENTS;
		}
		loadTleHistory(); // DOnt call this until the Name and FoxId are set
		positionCache = new SpacecraftPositionCache(foxId);
	}

	public static final DateFormat timeDateFormat = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat dateDateFormat = new SimpleDateFormat("dd MMM yy");
	
	public int getCurrentReset(int resetOnFrame, long uptime) {
		if (!hasFixedReset) return resetOnFrame;
		if (hasTimeZero()) {
			int reset = timeZero.size()-1; // we have one entry per reset
			
			long T0Seconds = timeZero.get(reset)/1000;;
			Date now = new Date();
			long nowSeconds = now.getTime()/1000;
			long newT0estimate = (nowSeconds - uptime)*1000;
			
			if (resetOnFrame == reset + 1) {
				// somehow we had a normal reset
				// Add a new reset for this
				timeZero.add(newT0estimate);
				try {
					saveTimeZeroSeries();
				} catch (IOException e) {
					e.printStackTrace(Log.getWriter());
				}
				return resetOnFrame;
			}
			
			long diff = Math.abs(nowSeconds - (T0Seconds + uptime));
	    	if (uptime < Config.newResetCheckUptimeMax)	
	    		if (diff > Config.newResetCheckThreshold) {
	    			Log.println("*** HUSKY RESET DETECTED ...." + diff);
	    			timeZero.add(newT0estimate);
	    			try {
						saveTimeZeroSeries();
					} catch (IOException e) {
						e.printStackTrace(Log.getWriter());
					}
	    			return reset + 1;
	    		}
			return reset;
		} else 
		return resetOnFrame;
	}
	
	public boolean hasTimeZero() { 
		if (timeZero == null) return false;
		if (timeZero.size() == 0) return false;
		return true;
	}
	
	public boolean hasTimeZero(int reset) { 
		if (timeZero == null) return false;
		if (reset >= timeZero.size()) return false;
		return true;
	}
	
	public String[][] getT0TableData() {
		if (timeZero == null) return null;
		if (timeZero.size() == 0) return null;
		String[][] data = new String[timeZero.size()][];
		for (int i=0; i< timeZero.size(); i++) {
			data[i] = new String[2];
			data[i][0] = ""+i;
			data[i][1] = getUtcDateForReset(i,0) + " " + getUtcTimeForReset(i,0);
		}
		return data;
	}
	
	public String getUtcTimeForReset(int reset, long uptime) {
		if (timeZero == null) return null;
		if (reset >= timeZero.size()) return null;
		Date dt = new Date(timeZero.get(reset) + uptime*1000);
		timeDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String time = timeDateFormat.format(dt); 
		return time;
	}

	public String getUtcDateForReset(int reset, long uptime) {
		if (timeZero == null) return null;
		if (reset >= timeZero.size()) return null;
		Date dt = new Date(timeZero.get(reset) + uptime *1000);
		dateDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String time = dateDateFormat.format(dt);
		return time;
	}

	public Date getUtcForReset(int reset, long uptime) {
		if (timeZero == null) return null;
		if (reset >= timeZero.size()) return null;
		Date dt = new Date(timeZero.get(reset) + uptime*1000);
		return dt;
	}
	
	public DateTime getUtcDateTimeForReset(int reset, long uptime) {
		if (timeZero == null) return null;
		if (reset >= timeZero.size()) return null;
		Date dt = new Date(timeZero.get(reset) + uptime*1000);
		DateTime dateTime = new DateTime(dt); // FIXME - this date conversion is not working.  Need to understand how it works.
		return dateTime;
	}

	/**
	 * Given a date, what is the reset and uptime
	 * @param fromDate
	 * @return a FoxTime object with the reset and uptime
	 */
	public FoxTime getUptimeForUtcDate(Date fromDate) {
		if (timeZero == null) return null;
		if (fromDate == null) return null;
		if (timeZero.size() == 0) return null;
		long dateTime = fromDate.getTime();
		long T0 = -1;
		int reset = 0;
		long uptime = 0;
		// Search T0.  It's short so we can scan the whole list
		for (int i=0; i<timeZero.size(); i++) {
			if (timeZero.get(i) > dateTime) {
				// then we want the previous value
				if (i==0) break; 
				reset = i-1;
				T0 = timeZero.get(reset);
				break;
			}
			
		}
		if (T0 == -1) {
			// return the last reset, we scanned the whole list
			reset = timeZero.size()-1;
			T0 = timeZero.get(reset);
		}
		
		// Otherwise we have a valid reset, so calc the uptime, which is seconds from the T0 to the passed dateTime
		uptime = dateTime - T0; // milliseconds
		uptime = uptime / 1000; // seconds;
		
		FoxTime ft = new FoxTime(reset, uptime);
		return ft;
	}

	
	public SatPos getSatellitePosition(int reset, long uptime) throws PositionCalcException {
		// We need to construct a date for the historical time of this WOD record
		DateTime timeNow = getUtcDateTimeForReset(reset, uptime);
		if (timeNow == null) return null;
		SatPos satellitePosition = positionCache.getPosition(timeNow.getMillis());
		if (satellitePosition != null) {
			return satellitePosition;
		}
		final TLE tle = getTLEbyDate(timeNow);
//		if (Config.debugFrames) Log.println("TLE Selected fOR date: " + timeNow + " used TLE epoch " + tle.getEpoch());
		if (tle == null) throw new PositionCalcException(FramePart.NO_TLE); // We have no keps
		final Satellite satellite = SatelliteFactory.createSatellite(tle);
        satellitePosition = satellite.getPosition(Config.GROUND_STATION, timeNow.toDate());
//        Log.println("Cache value");
        positionCache.storePosition(timeNow.getMillis(), satellitePosition);
		return satellitePosition;
	}
	
	public void save() {
//		super.save();
//		properties.setProperty("IHU_SN", Integer.toString(IHU_SN));
//		for (int i=0; i< experiments.length; i++)
//			properties.setProperty("EXP"+(i+1), Integer.toString(experiments[i]));
//		
//		properties.setProperty("useIHUVBatt", Boolean.toString(useIHUVBatt));
//		properties.setProperty("measurementsFileName", measurementsFileName);
//		properties.setProperty("passMeasurementsFileName", passMeasurementsFileName);
//				
//		properties.setProperty("hasModeInHeader", Boolean.toString(hasModeInHeader));
//		store();
		
		// Only the user params can be changed and saved
		save_user_params();
	}
	
	public void save_user_params() {
		super.save_user_params();
		user_properties.setProperty("BATTERY_CURRENT_ZERO", Double.toString(user_BATTERY_CURRENT_ZERO));
		if (this.hasMpptSettings) {
			user_properties.setProperty("mpptResistanceError", Double.toString(user_mpptResistanceError));
			user_properties.setProperty("mpptSensorOffThreshold", Integer.toString(user_mpptSensorOffThreshold));
		}
		if (hasMemsRestValues) {
			user_properties.setProperty("memsRestValueX", Integer.toString(user_memsRestValueX));
			user_properties.setProperty("memsRestValueY", Integer.toString(user_memsRestValueY));
			user_properties.setProperty("memsRestValueZ", Integer.toString(user_memsRestValueZ));			
		}
		
		user_properties.setProperty("hasModeInHeader", Boolean.toString(hasModeInHeader));
		store_user_params();
	
	}

	public void saveTimeZeroSeries() throws IOException {

		String log = "FOX"+ foxId + Config.t0UrlFile;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
			Log.println("Loading: " + log);
		}

		//use buffering and replace the existing file
		File aFile = new File(log );
		Writer output = new BufferedWriter(new FileWriter(aFile, false));
		int r = 0;

		try {
			for (long l : timeZero) {
				output.write(r +"," + l + "\n" );
				output.flush();
				r++;
			}
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}

	}

	public boolean loadTimeZeroSeries(String log) throws FileNotFoundException {
		timeZero = new ArrayList<Long>(100);
		String line = null;
		if (log == null) { // then use the default
			log = "FOX"+ foxId + Config.t0UrlFile;
        	if (!Config.logFileDirectory.equalsIgnoreCase("")) {
        		log = Config.logFileDirectory + File.separator + log;
        		Log.println("Loading: " + log);
        	}
        }
        //File aFile = new File(log );
        boolean hasContent = false;
        
        BufferedReader dis = new BufferedReader(new FileReader(log));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			StringTokenizer st = new StringTokenizer(line, ",");
        			int reset = Integer.valueOf(st.nextToken()).intValue();
        			long uptime = Long.valueOf(st.nextToken()).longValue();
        			//Log.println("Loaded T0: " + reset + ": " + uptime);
        			if (reset == timeZero.size()) {
        				timeZero.add(uptime);
        				hasContent = true;
        			} else throw new IndexOutOfBoundsException("Reset in T0 file is missing or out of sequence: " + reset);
        		}
        	}
			dis.close();
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());
        	return false;
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        	return false;
        } catch (NoSuchElementException m) {
        	// This was likely a blank file because we have no internet connection
        	return false;
        } finally {
        	try {
				dis.close();
			} catch (IOException e) {
				// ignore error
			}
        }
		return hasContent;
	}
	
	/**
	 * This loads all of the settings including those that are overridden by the user_ settings.  It they do not
	 * exist yet then they are initialized here
	 * @throws LayoutLoadException
	 */
	protected void load() throws LayoutLoadException {
		super.load();
		try {
			IHU_SN = Integer.parseInt(getProperty("IHU_SN"));
			for (int i=0; i< experiments.length; i++)
				experiments[i] = Integer.parseInt(getProperty("EXP"+(i+1)));
			
			user_BATTERY_CURRENT_ZERO = Double.parseDouble(getProperty("BATTERY_CURRENT_ZERO"));
		
			useIHUVBatt = Boolean.parseBoolean(getProperty("useIHUVBatt"));

			measurementsFileName = getProperty("measurementsFileName");
			passMeasurementsFileName = getProperty("passMeasurementsFileName");
			String error = getOptionalProperty("mpptResistanceError");
			if (error != null) {
				user_mpptResistanceError = Double.parseDouble(error);
				hasMpptSettings = true;
			}
			String threshold = getOptionalProperty("mpptSensorOffThreshold");
			if (threshold != null) {
				user_mpptSensorOffThreshold = Integer.parseInt(threshold);
				hasMpptSettings = true;
			}
			String icr = getOptionalProperty("hasImprovedCommandReceiver");
			if (icr != null) {
				hasImprovedCommandReceiver = Boolean.parseBoolean(icr);
			}
			String mode = getOptionalProperty("hasModeInHeader");
			if (mode != null) {
				hasModeInHeader = Boolean.parseBoolean(getProperty("hasModeInHeader"));
			}
			String fixedReset = getOptionalProperty("hasFixedReset");
			if (fixedReset != null) {
				hasFixedReset = Boolean.parseBoolean(getProperty("hasFixedReset"));
			}
			String mems_x = getOptionalProperty("memsRestValueX");
			if (mems_x != null) {
				user_memsRestValueX = Integer.parseInt(mems_x);
			} 
			String mems_y = getOptionalProperty("memsRestValueY");
			if (mems_y != null) {
				user_memsRestValueY = Integer.parseInt(mems_y);
			} 
			String mems_z = getOptionalProperty("memsRestValueZ");
			if (mems_z != null) {
				user_memsRestValueZ = Integer.parseInt(mems_z);
			} 
			if (user_memsRestValueX != 0 && user_memsRestValueY != 0 & user_memsRestValueZ != 0)
				hasMemsRestValues = true;

		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt FOX data found when loading Spacecraft file: " + propertiesFile.getAbsolutePath() );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing FOX data value when loading Spacecraft file: " + propertiesFile.getAbsolutePath());		
		}
		load_user_params();
	}
	
	protected void load_user_params() throws LayoutLoadException {
		super.load_user_params();
		
		try {
			user_BATTERY_CURRENT_ZERO = Double.parseDouble(getUserProperty("BATTERY_CURRENT_ZERO"));
		
			String error = getOptionalUserProperty("mpptResistanceError");
			if (error != null) {
				user_mpptResistanceError = Double.parseDouble(error);
				hasMpptSettings = true;
			}
			String threshold = getOptionalUserProperty("mpptSensorOffThreshold");
			if (threshold != null) {
				user_mpptSensorOffThreshold = Integer.parseInt(threshold);
				hasMpptSettings = true;
			}
			String mems_x = getOptionalUserProperty("memsRestValueX");
			if (mems_x != null) {
				user_memsRestValueX = Integer.parseInt(mems_x);
			}
			String mems_y = getOptionalUserProperty("memsRestValueY");
			if (mems_y != null) {
				user_memsRestValueY = Integer.parseInt(mems_y);
			}
			String mems_z = getOptionalUserProperty("memsRestValueZ");
			if (mems_z != null) {
				user_memsRestValueZ = Integer.parseInt(mems_z);
			}
			if (user_memsRestValueX != 0 && user_memsRestValueY != 0 & user_memsRestValueZ != 0)
				hasMemsRestValues = true;
		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt FOX data found when loading Spacecraft file: " + propertiesFile.getAbsolutePath() );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing FOX data value when loading Spacecraft file: " + propertiesFile.getAbsolutePath());		
		}
	}
	
	/**
	 * Return true if one of the experiment slots contains the Virginia Tech Camera
	 * @return
	 */
	public boolean hasCamera() {
		for (int i=0; i< experiments.length; i++) {
			if (experiments[i] == EXP_VT_CAMERA) return true;
			if (experiments[i] == EXP_VT_CAMERA_LOW_RES) return true;
		}
		return false;
	}

	public boolean hasLowResCamera() {
		for (int i=0; i< experiments.length; i++) {
			if (experiments[i] == EXP_VT_CAMERA_LOW_RES) return true;
		}
		return false;
	}

	/**
	 * Return true if one of the experiment slots contains the HERCI experiment
	 * @return
	 */
	public boolean hasHerci() {
		for (int i=0; i< experiments.length; i++)
			if (experiments[i] == EXP_IOWA_HERCI) return true;
		return false;
	}
	
	public String getIdString() {
		String id = "??";
		if (foxId == 1) id = "1A";
		else if (foxId == 2) id = "1B";
		else if (foxId == 3) id = "1Cliff";
		else if (foxId == 4) id = "1D";
		else if (foxId == 5) id = "1E";
		else if (foxId == 6) id = "1F";
		else if (foxId == 7) id = "1G";
		else id = Integer.toString(foxId);

		return id;
	}
	
	public static String getModeString(int mode) {
		return FoxSpacecraft.modeNames[mode];
	}
	
	/**
	 * Return the mode of the spacecraft based in the most recent RT, MAX, MIN and EXP payloads
	 * @param realTime
	 * @param maxPaylaod
	 * @param minPayload
	 * @param radPayload
	 * @return
	 */
	public static int determine1A1DMode(PayloadRtValues realTime, PayloadMaxValues maxPayload, PayloadMinValues minPayload, FramePart radPayload) {
		if (realTime != null && minPayload != null && maxPayload != null) {
			if (realTime.uptime == minPayload.uptime && minPayload.uptime == maxPayload.uptime)				
				return DATA_MODE;
		}
		// Otherwise, if RAD received more recently than max/min and RT
		// In the compare, a -ve result means older, because the reset or uptime is less
		// So a result >0 means the object calling the compare is newer
		if (radPayload != null)
			if (realTime != null && radPayload.compareTo(realTime) > 0)
				if (maxPayload == null && minPayload == null)
					return TRANSPONDER_MODE;
				else if (maxPayload != null && radPayload.compareTo(maxPayload) > 0)
					if (minPayload == null)
						return TRANSPONDER_MODE;
					else if (radPayload.compareTo(minPayload) > 0)
						return TRANSPONDER_MODE;
		
		// Otherwise find the most recent max/min
		// if we have just a max payload, or we have both and max is more recent or the same
		if ((minPayload == null && maxPayload != null) || (maxPayload != null && minPayload != null && maxPayload.compareTo(minPayload) >=0)) {
			if (maxPayload.getRawValue(SCIENCE_MODE_IND) == 1)
				return SCIENCE_MODE;
			else if (maxPayload.getRawValue(SAFE_MODE_IND) == 1)
				return SAFE_MODE;
			else
				return TRANSPONDER_MODE;
		} else if (minPayload != null) {  // this is the case when we have both and min is more recent
			if (minPayload.getRawValue(SCIENCE_MODE_IND) == 1)
				return SCIENCE_MODE;
			else if (minPayload.getRawValue(SAFE_MODE_IND) == 1)
				return SAFE_MODE;
			else
				return TRANSPONDER_MODE;
		}
		// return default
		return TRANSPONDER_MODE;
	}
	public String determineModeFromHeader() {
		// Mode is stored in the header
		// Find the most recent frame and return the mode that it has
		SortedFramePartArrayList payloads = new SortedFramePartArrayList(numberOfLayouts);
		int maxLayouts = 4; // First four layouts are rt, max, min, exp
		for (int i=0; i <= maxLayouts && i < layout.length; i++) {
			//System.err.println("Checking mode in: "+layout[i].name );
			payloads.add(Config.payloadStore.getLatest(foxId, layout[i].name));
		}

		int mode = NO_MODE;
		if (payloads.size() > 0)
			mode = payloads.get(payloads.size()-1).newMode;
		return getModeString(mode);
	}

	public static String OLDdetermineModeFromHeader(PayloadRtValues realTime, PayloadMaxValues maxPayload, PayloadMinValues minPayload, 
			FramePart radPayload) {
		// Mode is stored in the header
		// Find the most recent frame and return the mode that it has
		SortedFramePartArrayList payloads = new SortedFramePartArrayList(4);
		if (realTime != null) payloads.add(realTime);
		if (maxPayload != null) payloads.add(maxPayload);
		if (minPayload != null) payloads.add(minPayload);
		if (radPayload != null) payloads.add(radPayload);
		int mode = NO_MODE;
		if (payloads.size() > 0)
			mode = payloads.get(payloads.size()-1).newMode;
		return getModeString(mode);
	}
	
	public static String determineModeString(FoxSpacecraft fox, PayloadRtValues realTime, PayloadMaxValues maxPayload, 
			PayloadMinValues minPayload, FramePart radPayload) {
		int mode;

		mode = determine1A1DMode(realTime, maxPayload, minPayload, radPayload);
		return getModeString(mode);
	}

	public String toString() {

		return this.user_display_name; //"Fox-" + getIdString();

	}
}
