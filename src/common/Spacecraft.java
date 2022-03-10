package common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import gui.SourceTab;
import predict.FoxTLE;
import predict.PositionCalcException;
import predict.SortedTleList;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.SortedFramePartArrayList;
import telemetry.TelemFormat;
import telemetry.conversion.Conversion;
import telemetry.conversion.ConversionCurve;
import telemetry.conversion.ConversionLookUpTable;
import telemetry.conversion.ConversionMathExpression;
import telemetry.conversion.ConversionStringLookUpTable;
import telemetry.frames.FrameLayout;
import telemetry.payloads.PayloadMaxValues;
import telemetry.payloads.PayloadMinValues;
import telemetry.payloads.PayloadRtValues;
import telemetry.uw.CanFrames;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class Spacecraft implements Comparable<Spacecraft> {
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	SatelliteManager satManager;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static final int ERROR_IDX = -1;
	
	// THESE HARD CODED LOOKUPS SHOULD NOT BE USED FOR NEW SPACECRAFT
	// Code a paramater into the spacecraft file that might work with future hardware, e.g. hasCanBus
	// Or switch logic based on standard layouts defined below, or custom layouts if required e.g. camera formats
	public static final int FOX1A = 1;
	public static final int FOX1B = 2;
	public static final int FOX1C = 3;
	public static final int FOX1D = 4;

	public static final int FIRST_FOXID_WITH_MODE_IN_HEADER = 6;

	public static final int MAX_FOXID = 256; // experimentally increase this to allow other ids. Note the header is limited to 8 bits

	// Layout Types
	public static final String DEBUG_LAYOUT = "DEBUG";
	public static final String REAL_TIME_LAYOUT = "rttelemetry";
	public static final String MAX_LAYOUT = "maxtelemetry";
	public static final String MIN_LAYOUT = "mintelemetry";
	public static final String RAD_LAYOUT = "radtelemetry";
	public static final String RAD2_LAYOUT = "radtelemetry2";
	public static final String HERCI_HS_LAYOUT = "herciHSdata";
	public static final String HERCI_HS_HEADER_LAYOUT = "herciHSheader";
	public static final String HERCI_HS_PKT_LAYOUT = "herciHSpackets";
	public static final String WOD_LAYOUT = "wodtelemetry";
	public static final String WOD_RAD_LAYOUT = "wodradtelemetry";
	public static final String WOD_RAD2_LAYOUT = "wodradtelemetry2";
	public static final String RAD_LEPF_LAYOUT = "radtelemLEPF";
	public static final String RAD_LEP_LAYOUT = "radtelemLEP";
	public static final String RAD_REM_LAYOUT = "radtelemREM";

	// These are the layouts
	public static final String CAN_LAYOUT = "cantelemetry";
	public static final String WOD_CAN_LAYOUT = "wodcantelemetry";

	// These are the individual CAN packets inside the layouts
	public static final String CAN_PKT_LAYOUT = "canpacket";
	public static final String WOD_CAN_PKT_LAYOUT = "wodcanpacket";

	public static final String RSSI_LOOKUP = "RSSI";
	public static final String IHU_VBATT_LOOKUP = "IHU_VBATT";
	public static final String IHU_TEMP_LOOKUP = "IHU_TEMP";
	public static final String HUSKY_SAT_ISIS_ANT_TEMP = "HUSKY_ISIS_ANT_TEMP";
	
	// Model Versions
	public static final int EM = 0;
	public static final int FM = 1;
	public static final int FS = 2;
	
	// Flattened ENUM for spacecraft name
	public static String[] modelNames = {
			"Engineering Model",
			"Flight Model",
			"Flight Spare"
	};
		
	public static String[] models = {
			"EM",
			"FM",
			"FS"
	};
	
	public int foxId = 0;
	public int catalogNumber = 0;
	public String series = "FOX";
	public String description = "";
	public int model;
	public String canFileDir = "HuskySat";
	public int user_format = SourceTab.FORMAT_FSK_DUV;
	
	public boolean telemetryMSBfirst = true;
	public boolean ihuLittleEndian = true;
		
	public int numberOfLayouts = 0;
	public int numberOfDbLayouts = 0; // if we load additional layouts for CAN BUS then this stores the core layouts
	public String[] layoutFilename;
	public BitArrayLayout[] layout;
	private boolean[] sendLayoutLocally;  // CURRENTLY UNUSED SO MADE PRIVATE
	public CanFrames canFrames;
	
	public int numberOfLookupTables = 0;
	public String[] lookupTableFilename;
	public ConversionLookUpTable[] lookupTable;
	
	public int numberOfStringLookupTables = 0;
	public String[] stringLookupTableFilename;
	public ConversionStringLookUpTable[] stringLookupTable;
	
	public int numberOfSources = 1;
	public String[] sourceName = {"amsat.fox-1a.ihu.duv"}; // default to 1 source DUV;
	public String[] sourceFormatName;
	public TelemFormat[] sourceFormat;
	
	public String measurementsFileName = "measurements.csv"; // theoretically it is possible to change this in the MASTER file, but it is the same for all spacecraft
	public String passMeasurementsFileName = "passmeasurements.csv";
	public BitArrayLayout measurementLayout;
	public BitArrayLayout passMeasurementLayout;
	
	public static final String MEASUREMENTS = "measurements";
	public static final String PASS_MEASUREMENTS = "passmeasurements";
	
	public int numberOfFrameLayouts = 0;
	public String[] frameLayoutFilename;
	public FrameLayout[] frameLayout;
	
	// User Config
	public static final String USER_ = "user_";
	public Properties user_properties; // Java properties file for user defined values
	public File userPropertiesFile;
	public String user_display_name = "Amsat-1";
	public String user_keps_name = "Amsat-1";
	public int user_priority = 9; // set to low priority so new spacecraft are not suddenly ahead of old ones
	public boolean user_track = true; // default is we track a satellite
	public double user_telemetryDownlinkFreqkHz = 145980;
	public double user_minFreqBoundkHz = 145970;
	public double user_maxFreqBoundkHz = 145990;
	public String user_localServer = ""; // default to blank, otherwise we try to send to the local server
	public int user_localServerPort = 8587;

	public SatPos satPos; // cache the position when it gets calculated so others can read it
	public double satPosErrorCode; // Store the error code when we return null for the position
	public boolean hasCanBus;
	public boolean hasFrameCrc = false;
	
	private SortedTleList tleList; // this is a list of TLEs loaded from the history file.  We search this for historical TLEs
	
	public boolean useConversionCoeffs = false;
	private HashMap<String, Conversion> conversions;
	
	public boolean hasFOXDB_V3 = false;
	
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
	public static final int RAG_ADAC = 10; // Ragnaroc
	
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
		"Virginia Tech Camera",
		"University of Iowa HERCI",
		"Rad FX Sat",
		"Virginia Tech Low-res Camera",
		"Vanderbilt VUC",
		"Vanderbilt REM",
		"Vanderbilt LEPF",
		"CAN Packet Interface",
		"Ragnaroc ADAC",
		"L-Band Downshifter"
	};
	
	
	public int IHU_SN = 0;
	public int[] experiments = {EXP_EMPTY, EXP_EMPTY, EXP_EMPTY, EXP_EMPTY};
	
	public boolean hasImprovedCommandReceiver = false;
	public boolean hasImprovedCommandReceiverII = false;
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
	
	public static final DateFormat timeDateFormat = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat dateDateFormat = new SimpleDateFormat("dd MMM yy");
	
	/*
	final String[] testTLE = {
            "AO-85",
            "1 40967U 15058D   16111.35540844  .00000590  00000-0  79740-4 0 01029",
            "2 40967 064.7791 061.1881 0209866 223.3946 135.0462 14.74939952014747"};
	*/
	
	/**
	 * Initialize the spacecraft settings. Load Spacecraft from disk.  Load from the master file and the user file
	 * 
	 * @param masterFileName
	 * @param userFileName
	 * @throws LayoutLoadException
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	public Spacecraft(SatelliteManager satManager, File masterFileName, File userFileName ) throws LayoutLoadException, FileNotFoundException {
		this.satManager = satManager;
		properties = new Properties();
		propertiesFile = masterFileName;	
		
		userPropertiesFile = userFileName;	
		tleList = new SortedTleList(10);
		
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
		loadTleHistory(); // Dont call this until the Name and FoxId are set
		positionCache = new SpacecraftPositionCache(foxId);
	}
	
	/**
	 * Create a new blank spacecraft MASTER file
	 * @param satManager
	 * @param masterFileName
	 */
	public Spacecraft(SatelliteManager satManager, File masterFileName, File userFileName, int foxId) {

		this.satManager = satManager;
		properties = new Properties();
		
		propertiesFile = masterFileName;	
		userPropertiesFile = userFileName;	
		
		tleList = new SortedTleList(10);
		
		this.foxId = foxId;

		hasFOXDB_V3 = true; // should be true for all new spacecraft
		
		store_master_params();
	}
	
	/**
	 * This is a routine that determines if we can cast to FoxSpeacecrft, but currently everything can, so we return true
	 * If this is used in the future then we need to be careful about the functionality that is turned on/off by it
	 * It is not removed because the points in the code that call this may well be important switch points in the future and
	 * it is easier to return true here than remove the calls and later try to work out where they were.
	 * @return
	 */
	@Deprecated
	public boolean isFox1() {
		return true;
//		if (foxId < 10) return true;
//		return false;
	}
	
	public int getLayoutIdxByName(String name) {
		for (int i=0; i<numberOfLayouts; i++)
			if (layout[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	public int getLookupIdxByName(String name) {
		for (int i=0; i<numberOfLookupTables; i++)
			if (lookupTable[i].getName().equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	
	public BitArrayLayout getLayoutByName(String name) {
		int i = getLayoutIdxByName(name);
		if (i != ERROR_IDX)
				return layout[i];
		return null;
	}
	
	/**
	 * Find the secondary layout for this layout
	 * The secondary layout points to its parent
	 * @param name
	 * @return
	 */
	public BitArrayLayout getSecondaryLayoutFromPrimaryName(String name) {
		for (int i=0; i<numberOfLayouts; i++)
			if (layout[i].parentLayout != null)
				if (layout[i].parentLayout.equalsIgnoreCase(name))
					return layout[i];
		return null;
	}
	
	public BitArrayLayout getLayoutByCanId(int canId) {
		if (!hasCanBus) return null;
		if (canFrames == null) return null;
		String name = canFrames.getNameByCanId(canId);
		if (name != null) {
			int i = getLayoutIdxByName(name);
			if (i != ERROR_IDX)
				return layout[i];
		}
		return getLayoutByName(Spacecraft.CAN_PKT_LAYOUT); // try to return the default instead. We dont have this CAN ID
	}

	public Conversion getConversionByName(String name) {
		if (conversions == null) return null;
		Conversion conv = conversions.get(name);
		return conv;
	}

	public ConversionLookUpTable getLookupTableByName(String name) {
		int i = getLookupIdxByName(name);
		if (i != ERROR_IDX)
				return lookupTable[i];
		return null;
	}

	public String getLayoutFileNameByName(String name) {
		int i = getLayoutIdxByName(name);
		if (i != ERROR_IDX)
				return layoutFilename[i];
		return null;
	}

	public String getLookupTableFileNameByName(String name) {
		int i = getLookupIdxByName(name);
		if (i != ERROR_IDX)
				return lookupTableFilename[i];
		return null;
	}

	/**
	 * TLEs are stored in the spacecraft directory in the logFileDirectory.
	 * @throws IOException 
	 */
	protected void loadTleHistory() {
		String file = Spacecraft.SPACECRAFT_DIR + File.separator + series + this.foxId + ".tle";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + file;		
		}
		
		File f = new File(file);
		InputStream is = null;
		try {
			is = new FileInputStream(f);
			tleList = FoxTLE.importFoxSat(is);
		} catch (IOException e) {
			Log.println("TLE file not loaded: " + file);
			//e.printStackTrace(Log.getWriter()); // No TLE, but this is not viewed as fatal.  It should be fixed by Kep check
		} finally {
			try {
				if (is != null) is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void saveTleHistory() throws IOException {
		String file = Spacecraft.SPACECRAFT_DIR + File.separator + series + this.foxId + ".tle";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + file;		
		}
		File f = new File(file);
		Writer output = new BufferedWriter(new FileWriter(f, false));
		for (FoxTLE tle : tleList) {
			//Log.println("Saving TLE to file: " + tle.toString() + ": " + tle.getEpoch());
			output.write(tle.toFileString());
		}
		output.flush();
		output.close();
	}
	
	/**
	 * We are passed a new TLE for this spacecarft.  We want to store it in the file if it is a TLE that we do not already have.
	 * @param tle
	 * @return
	 * @throws IOException 
	 */
	public boolean addTLE(FoxTLE tle) throws IOException {
		tleList.add(tle);
		saveTleHistory();
		return true;
	}
	
	protected TLE getTLEbyDate(DateTime dateTime) throws PositionCalcException {
		if (tleList == null) return null;
		TLE t = tleList.getTleByDate(dateTime);
		if (t==null) {
			satPosErrorCode = FramePart.NO_TLE;
			throw new PositionCalcException(FramePart.NO_TLE);
		}
		return t;
	}
	
	
	
	/**
	 * Calculate the position at a historical data/time
	 * Typically we don't call this directly.  Instead we call with the reset/uptime and hope that the value is already cached
	 * @param timeNow
	 * @return
	 * @throws PositionCalcException
	 */
	public SatPos calcSatellitePosition(DateTime timeNow) throws PositionCalcException {
		final TLE tle = getTLEbyDate(timeNow);
//		if (Config.debugFrames) Log.println("TLE Selected fOR date: " + timeNow + " used TLE epoch " + tle.getEpoch());
		if (tle == null) {
			satPosErrorCode = FramePart.NO_TLE;
			throw new PositionCalcException(FramePart.NO_TLE); // We have no keps
		}
		final Satellite satellite = SatelliteFactory.createSatellite(tle);
        final SatPos satellitePosition = satellite.getPosition(Config.GROUND_STATION, timeNow.toDate());
		return satellitePosition;
	}

	/**
	 * Calculate the current position and cache it
	 * @return
	 * @throws PositionCalcException
	 */
	protected SatPos calcualteCurrentPosition() throws PositionCalcException {
		DateTime timeNow = new DateTime(DateTimeZone.UTC);
		SatPos pos = null;
		pos = calcSatellitePosition(timeNow);
		satPos = pos;
		if (Config.debugSignalFinder)
			Log.println("Fox at: " + FramePart.latRadToDeg(pos.getAzimuth()) + " : " + FramePart.lonRadToDeg(pos.getElevation()));
		return pos;
	}

	public SatPos getCurrentPosition() throws PositionCalcException {
		if (satPos == null) {
			throw new PositionCalcException(FramePart.NO_POSITION_DATA);
		}
		return satPos;
	}
	
	public boolean aboveHorizon() {
//		if (Config.useDDEforAzEl) {
//			String satString = null;
//			SatPc32DDE satPC = new SatPc32DDE();
//			boolean connected = satPC.connect();
//			if (connected) {
//				satString = satPC.satellite;
//				//Log.println("SATPC32: " + satString);
//				if (satString != null && satString.equalsIgnoreCase(user_keps_name)) {
//					return true;
//				}
//			}
//			return false;
//		}
		if (satPos == null)
			return false;
		return (FramePart.radToDeg(satPos.getElevation()) >= 0);
	}
	
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
				if (Config.debugFrames)
					Log.println("SAVING RESET: " + resetOnFrame);
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
					if (Config.debugFrames)
						Log.println("SAVING RESET: " + (reset+1));
	    			return reset + 1;
	    		}
			if (Config.debugFrames)
				Log.println("SAVING RESET: " + reset);
			return reset;
		} else {
			if (Config.debugFrames)
				Log.println("SAVING RESET: " + resetOnFrame);
			return resetOnFrame;
		}
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
		if (reset < 0 || reset >= timeZero.size()) return null;
		if (uptime < 0) return null;
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
		if (timeNow == null) throw new PositionCalcException(FramePart.NO_T0);
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
	
	public boolean sendToLocalServer() {
		if (user_localServer == null) return false;
		if (user_localServer.equalsIgnoreCase(""))
			return false;
		else
			return true;
	}
	
	/**
	 * Returns true if this layout should be sent to the local server
	 * @param name
	 * @return
	 */
	public boolean shouldSendLayout(String name) {
		for (int i=0; i<numberOfLayouts; i++)
			if (layout[i].name.equals(name))
				if (sendLayoutLocally[i])
					return true;
		return false;
	}
	
	/**
	 * This loads all of the settings including those that are overridden by the user_ settings.  It they do not
	 * exist yet then they are initialized here
	 * @throws LayoutLoadException
	 */
	protected void load() throws LayoutLoadException {
		// try to load the properties from a file
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.load(f);
			f.close();
		} catch (IOException e) {
			if (f!=null) try { f.close(); } catch (Exception e1) {};
			throw new LayoutLoadException("Could not load spacecraft.  File is missing: " + propertiesFile.getAbsolutePath());
			
		}
		try {
			foxId = Integer.parseInt(getProperty("foxId"));
			catalogNumber = Integer.parseInt(getProperty("catalogNumber"));			
			user_keps_name = getProperty("name");
			description = getProperty("description");
			model = Integer.parseInt(getProperty("model"));
			user_telemetryDownlinkFreqkHz = Double.parseDouble(getProperty("telemetryDownlinkFreqkHz"));			
			user_minFreqBoundkHz = Double.parseDouble(getProperty("minFreqBoundkHz"));
			user_maxFreqBoundkHz = Double.parseDouble(getProperty("maxFreqBoundkHz"));

			useConversionCoeffs = getOptionalBooleanProperty("useConversionCoeffs");
			if (useConversionCoeffs)
				conversions = new HashMap<String, Conversion>();

			
			// Frame Layouts
			String frames = getOptionalProperty("numberOfFrameLayouts");
			if (frames == null) 
				numberOfFrameLayouts = 0;
			else {
				numberOfFrameLayouts = Integer.parseInt(frames);
				frameLayoutFilename = new String[numberOfFrameLayouts];
				frameLayout = new FrameLayout[numberOfFrameLayouts];
				for (int i=0; i < numberOfFrameLayouts; i++) {
					frameLayoutFilename[i] = getProperty("frameLayout"+i+".filename");
					frameLayout[i] = new FrameLayout(foxId, Spacecraft.SPACECRAFT_DIR + File.separator + frameLayoutFilename[i]);
					frameLayout[i].name = getProperty("frameLayout"+i+".name");
				}
			}
			
			// Conversions
			if (useConversionCoeffs) {
				String conversionCurvesFileName = getOptionalProperty("conversionCurvesFileName");
				if (conversionCurvesFileName != null) {
					loadConversionCurves(Spacecraft.SPACECRAFT_DIR + File.separator + conversionCurvesFileName);
				}
			
				String conversionExpressionsFileName = getOptionalProperty("conversionExpressionsFileName");
				if (conversionExpressionsFileName != null) {
					loadConversionExpresions(Spacecraft.SPACECRAFT_DIR + File.separator + conversionExpressionsFileName);
				}
				
				// String Lookup Tables
				String sNumberOfStringLookupTables = getOptionalProperty("numberOfStringLookupTables");
				if (sNumberOfStringLookupTables != null) {
				numberOfStringLookupTables = Integer.parseInt(sNumberOfStringLookupTables);
				stringLookupTableFilename = new String[numberOfStringLookupTables];
				stringLookupTable = new ConversionStringLookUpTable[numberOfStringLookupTables];
				for (int i=0; i < numberOfStringLookupTables; i++) {
					stringLookupTableFilename[i] = getProperty("stringLookupTable"+i+".filename");
					String tableName = getProperty("stringLookupTable"+i);
					stringLookupTable[i] = new ConversionStringLookUpTable(tableName, stringLookupTableFilename[i]);

					if (conversions.containsKey(stringLookupTable[i].getName())) {
						// we have a namespace clash, warn the user
						Log.errorDialog("DUPLICATE STRING TABLE NAME", this.user_keps_name + ": Lookup table or Curve already defined and will not be stored: " + tableName);
					} else {
						conversions.put(tableName, stringLookupTable[i]);
						Log.println("Stored: " + stringLookupTable[i]);
					}
				}
				}
			}

			String V3DB = getOptionalProperty("hasFOXDB_V3");
			if (V3DB == null) 
				hasFOXDB_V3 = false;
			else 
				hasFOXDB_V3 = Boolean.parseBoolean(V3DB);
			
			// Lookup Tables
			numberOfLookupTables = Integer.parseInt(getProperty("numberOfLookupTables"));
			lookupTableFilename = new String[numberOfLookupTables];
			lookupTable = new ConversionLookUpTable[numberOfLookupTables];
			for (int i=0; i < numberOfLookupTables; i++) {
				lookupTableFilename[i] = getProperty("lookupTable"+i+".filename");
				String tableName = getProperty("lookupTable"+i);
				lookupTable[i] = new ConversionLookUpTable(tableName, lookupTableFilename[i]);
				if (useConversionCoeffs) {
					if (conversions.containsKey(lookupTable[i].getName())) {
						// we have a namespace clash, warn the user
						Log.errorDialog("DUPLICATE TABLE NAME", this.user_keps_name + ": Lookup table already defined and will not be stored: " + tableName);
					} else {
						conversions.put(tableName, lookupTable[i]);
						Log.println("Stored: " + lookupTable[i]);
					}
				}
			}

			// Telemetry Layouts
			numberOfLayouts = Integer.parseInt(getProperty("numberOfLayouts"));
			numberOfDbLayouts = numberOfLayouts;
			layoutFilename = new String[numberOfLayouts];
			layout = new BitArrayLayout[numberOfLayouts];
			sendLayoutLocally = new boolean[numberOfLayouts];
			for (int i=0; i < numberOfLayouts; i++) {
				layoutFilename[i] = getProperty("layout"+i+".filename");
				layout[i] = new BitArrayLayout(layoutFilename[i]);
				
				// Check that the conversions look valid -- any this should be re-factored into the Conversion class when I have time //TODO
				if (useConversionCoeffs)
				for (int c=0; c<layout[i].NUMBER_OF_FIELDS; c++) {
					
					String convName = layout[i].getConversionNameByPos(c);
					
						// Not a legacy int conversion
						
						String[] conversions = convName.split("\\|"); // split the conversion based on | in case its a pipeline
						for (String singleConv : conversions) {
							singleConv = singleConv.trim();
							try {
								int convInt = Integer.parseInt(singleConv);
								if (convInt > BitArrayLayout.MAX_CONVERSION_NUMBER) {
									throw new LayoutLoadException("Conversion number "+ convInt +" is not defined. "+ "Error in row for field: " + layout[i].fieldName[c] + " on row: " + c + "\nwhen processing layout: " + layoutFilename[i] );
								}
							} catch (NumberFormatException e) {
								Conversion conv = this.getConversionByName(singleConv);
								if (conv == null) {

									String stem3 = "";
									if (singleConv.length() >=3)
										stem3 = singleConv.substring(0, 3); // first 3 characters to check for BIN, HEX
									String stem5 = "";
									if (singleConv.length() >=5)
										stem5 = singleConv.substring(0, 5); // first 5 characters to check for FLOAT
									String stem9 = "";
									if (singleConv.length() >=9)
										stem9 = singleConv.substring(0, 9); // first 9 characters to check for TIMESTAMP

									if (stem3.equalsIgnoreCase(Conversion.FMT_INT) 
											|| stem3.equalsIgnoreCase(Conversion.FMT_BIN)
											|| stem3.equalsIgnoreCase(Conversion.FMT_HEX)
											|| stem5.equalsIgnoreCase(Conversion.FMT_F)
											|| stem9.equalsIgnoreCase(Conversion.TIMESTAMP)) {
										// we skip, this is applied in string formatting later
									} else
										throw new LayoutLoadException("Conversion '"+ convName +"' is not defined. "+ "Error in row for field: " + layout[i].fieldName[c] + " on row: " + c + "\nwhen processing layout: " + layoutFilename[i] );
								}
							}
						
					}
					
				}
					
				
				layout[i].name = getProperty("layout"+i+".name");
				layout[i].parentLayout = getOptionalProperty("layout"+i+".parentLayout");
				if (hasFOXDB_V3) {
					layout[i].number = i;
					layout[i].typeStr = getProperty("layout"+i+".type");
					if (!BitArrayLayout.isValidType(layout[i].typeStr)) {
						throw new LayoutLoadException("Invalid payload type found: "+ layout[i].typeStr 
								+ "\nfor payload: " + layout[i].name 
								+ "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );
					}
					layout[i].title = getOptionalProperty("layout"+i+".title");
					layout[i].shortTitle = getOptionalProperty("layout"+i+".shortTitle");
				}
			}
			
			// sources
			numberOfSources = Integer.parseInt(getProperty("numberOfSources"));
			sourceName = new String[numberOfSources];
			for (int i=0; i < numberOfSources; i++) {
				sourceName[i] = getProperty("source"+i+".name");
			}

			// Source details
			String format = getOptionalProperty("source0.formatName");
			if (format == null) {
			} else {
				sourceFormat = new TelemFormat[numberOfSources];
				sourceFormatName = new String[numberOfSources];
				for (int i=0; i < numberOfSources; i++) {
					sourceFormatName[i] = getProperty("source"+i+".formatName");
					sourceFormat[i] = satManager.getFormatByName(sourceFormatName[i]);
				}				

			}
			
			String t = getOptionalProperty("track");
			if (t == null) 
				user_track = true;
			else 
				user_track = Boolean.parseBoolean(t);
			String s = getOptionalProperty("series");
			if (s == null) 
				series = "FOX";
			else 
				series = s;
			String serv = getOptionalProperty("localServer");
			if (serv == null) 
				user_localServer = null;
			else 
				user_localServer = serv;
			String p = getOptionalProperty("localServerPort");
			if (p == null) 
				user_localServerPort = 0;
			else 
				user_localServerPort = Integer.parseInt(p);
//			for (int i=0; i < numberOfLayouts; i++) {
//				String l = getOptionalProperty("sendLayoutLocally"+i);
//				if (l != null)
//					sendLayoutLocally[i] = Boolean.parseBoolean(l);
//				else
//					sendLayoutLocally[i] = false;
//			}
			String pri = getOptionalProperty("priority");
			if (pri == null) 
				user_priority = 1;
			else 
				user_priority = Integer.parseInt(pri);
			String c = getOptionalProperty("hasCanBus");
			if (c == null) 
				hasCanBus = false;
			else 
				hasCanBus =  Boolean.parseBoolean(c);
			if (hasCanBus) {
				this.canFileDir = getProperty("canFileDir");
				loadCanLayouts();
			}
			user_format = Integer.parseInt(getProperty("user_format"));
			user_display_name = getProperty("displayName");
			
			String crc = getOptionalProperty("hasFrameCrc");
			if (crc == null) 
				hasFrameCrc = false;
			else 
				hasFrameCrc = Boolean.parseBoolean(crc);

		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );
//		} catch (NullPointerException nf) {
//			nf.printStackTrace(Log.getWriter());
//			throw new LayoutLoadException("NULL data value: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );		
		} catch (FileNotFoundException e) {
			e.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("File not found: "+ e.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("File load error: "+ e.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath());
		}
		
		try {
			IHU_SN = Integer.parseInt(getProperty("IHU_SN"));
			
			
			// If fox hasFOXDB_V3 then the experiments are optional because all informtion for
			// tab layout is stored in the layouts and the rest of the master file
			for (int i=0; i< experiments.length; i++) {
				try {
					int num = Integer.parseInt(getProperty("EXP"+(i+1)));
					experiments[i] = num;
				} catch (LayoutLoadException nf) {
					if (!hasFOXDB_V3) throw nf;
				}

			}
			
			try {
				user_BATTERY_CURRENT_ZERO = Double.parseDouble(getProperty("BATTERY_CURRENT_ZERO"));
			} catch (LayoutLoadException nf) {
				if (!hasFOXDB_V3) throw nf;
			}
			try {
				useIHUVBatt = Boolean.parseBoolean(getProperty("useIHUVBatt"));
			} catch (LayoutLoadException nf) {
				if (!hasFOXDB_V3) throw nf;
			}
			
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
			String icr2 = getOptionalProperty("hasImprovedCommandReceiverII");
			if (icr2 != null) {
				hasImprovedCommandReceiverII = Boolean.parseBoolean(icr2);
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
		// try to load the properties from a file
		FileInputStream f = null;
		try {
			f=new FileInputStream(userPropertiesFile);
			user_properties = new Properties();
			user_properties.load(f);
			f.close();
		} catch (IOException e) {
			if (f!=null) try { f.close(); } catch (Exception e1) {};
			//throw new LayoutLoadException("Could not load spacecraft user settings from: " + userPropertiesFile.getAbsolutePath());
			// File does not exist, init
			save_user_params();

		}
		try {
			user_keps_name = getUserProperty("name");

			user_telemetryDownlinkFreqkHz = Double.parseDouble(getUserProperty("telemetryDownlinkFreqkHz"));			
			user_minFreqBoundkHz = Double.parseDouble(getUserProperty("minFreqBoundkHz"));
			user_maxFreqBoundkHz = Double.parseDouble(getUserProperty("maxFreqBoundkHz"));
			
			String t = getOptionalUserProperty("track");
			if (t == null) 
				user_track = true;
			else 
				user_track = Boolean.parseBoolean(t);
			String serv = getOptionalUserProperty("localServer");
			if (serv == null) 
				user_localServer = null;
			else 
				user_localServer = serv;
			String p = getOptionalUserProperty("localServerPort");
			if (p == null) 
				user_localServerPort = 0;
			else 
				user_localServerPort = Integer.parseInt(p);
			String pri = getOptionalUserProperty("priority");
			if (pri == null) 
				user_priority = 1;
			else 
				user_priority = Integer.parseInt(pri);
			user_format = Integer.parseInt(getUserProperty("user_format"));
			user_display_name = getUserProperty("displayName");

		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found: "+ nf.getMessage() + "\nwhen processing Spacecraft user settings: " + userPropertiesFile.getAbsolutePath() );
		} catch (LayoutLoadException L) {
			Log.infoDialog("Paramater Missing when loading User Spacecraft Properties", "For: "+user_keps_name+". If this is a new Spacecraft file or an upgrade then new values will be initialized.");
			save_user_params();
		} catch (NullPointerException nf) {
			//nf.printStackTrace(Log.getWriter());
			//throw new LayoutLoadException("Missing data value: "+ nf.getMessage() + "\nwhen processing Spacecraft user settings: " + userPropertiesFile.getAbsolutePath() );	
			// missing a value is OK, we must have it from the MASTER file.  It will get saved at next save.
			Log.errorDialog("Initialization Corruption for User Properties", "For: "+user_keps_name+". If this is a new Spacecraft file then new values will be initialized.");
			save_user_params();
		} 
		
		try {
			if (!hasFOXDB_V3)
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
	 * We have a CAN Bus and need to load the additional layouts for CAN Packts
	 * These are defined in frames.csv in a subdirectory with the name of the spacecraft
	 */
	private void loadCanLayouts() {
		try {
			canFrames = new CanFrames(this.canFileDir+ File.separator +"frames.csv");
			int canLayoutNum = canFrames.NUMBER_OF_FIELDS;
			Log.println("Loading " + canLayoutNum + " CAN Layouts");
			BitArrayLayout[] existingLayouts = layout;
			layout = new BitArrayLayout[layout.length+canLayoutNum];
			int i = 0;
			for (BitArrayLayout l : existingLayouts)
				layout[i++] = l;
			for (String frameName : canFrames.frame) {
				layout[i] = new BitArrayLayout(this.canFileDir+ File.separator + frameName + ".csv");
				layout[i].name = frameName;
				layout[i].parentLayout = "cantelemetry"; // give it any name so that it has a parent and is not a top level "payload"
				i++;
			}
			numberOfLayouts = layout.length;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LayoutLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//layout[i] = new BitArrayLayout(layoutFilename[i]);
	}
	
	protected String getOptionalProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return value;
	}

	protected boolean getOptionalBooleanProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			return false;
		}
		boolean b = Boolean.parseBoolean(value);
		return b;
	}

	protected String getOptionalUserProperty(String key) throws LayoutLoadException {
		String value = user_properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return value;
	}

	protected String getProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new LayoutLoadException("Missing data value: " + key + " when loading Spacecraft file: \n" + propertiesFile.getAbsolutePath() );
//			throw new NullPointerException();
		}
		return value;
	}
	
	protected String getUserProperty(String key) throws LayoutLoadException {
		String value = user_properties.getProperty(key);
		if (value == null) {
			throw new LayoutLoadException("Missing user data value: " + key + " when loading Spacecraft file: \n" + userPropertiesFile.getAbsolutePath() );
//			throw new NullPointerException();
		}
		return value;
	}
	
	
	/**
	 * The default save just saves the user editable params.  FoxTelem will never edit the master file
	 */
	public void save() {

		save_user_params();
	}

	
	protected void store_user_params() {
		FileOutputStream f = null;
		try {
			f=new FileOutputStream(userPropertiesFile);
			user_properties.store(f, "Fox 1 Telemetry Decoder User Spacecraft Properties");
			f.close();
		} catch (FileNotFoundException e1) {
			if (f!=null) try { f.close(); } catch (Exception e2) {};
			Log.errorDialog("ERROR", "Could not write spacecraft user properties file. Check permissions on run directory or on the file");
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			if (f!=null) try { f.close(); } catch (Exception e3) {};
			Log.errorDialog("ERROR", "Error writing spacecraft user properties file");
			e1.printStackTrace(Log.getWriter());
		}
	}
	
	protected void save_user_params() {
		user_properties = new Properties(); // clean record ready to save
		user_properties.setProperty("name", user_keps_name);
		user_properties.setProperty("displayName", user_display_name);
		user_properties.setProperty("telemetryDownlinkFreqkHz", Double.toString(user_telemetryDownlinkFreqkHz));
		user_properties.setProperty("minFreqBoundkHz", Double.toString(user_minFreqBoundkHz));
		user_properties.setProperty("maxFreqBoundkHz", Double.toString(user_maxFreqBoundkHz));
		user_properties.setProperty("track", Boolean.toString(user_track));
		user_properties.setProperty("user_format", Integer.toString(user_format));
		
		if (user_localServer != null) {
			user_properties.setProperty("localServer",user_localServer);
			user_properties.setProperty("localServerPort", Integer.toString(user_localServerPort));
		}
		user_properties.setProperty("priority", Integer.toString(user_priority));
		
		if (!this.hasFOXDB_V3)
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
	
	protected void store_master_params() {
		FileOutputStream f = null;
		try {
			f=new FileOutputStream(propertiesFile);
			properties.store(f, "AMSAT Spacecraft Properties");
			f.close();
		} catch (FileNotFoundException e1) {
			if (f!=null) try { f.close(); } catch (Exception e2) {};
			Log.errorDialog("ERROR", "Could not write spacecraft MASTER file. Check permissions on run directory or on the file");
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			if (f!=null) try { f.close(); } catch (Exception e3) {};
			Log.errorDialog("ERROR", "Error writing spacecraft MASTER file");
			e1.printStackTrace(Log.getWriter());
		}
	}
	
	/**
	 * This should never be called by the decoder.  This is only called by the spacecraft editor
	 */
	public void save_master_params() {
		// Store the MASTER params
		// Store the values
		properties.setProperty("foxId", String.valueOf(foxId));
		properties.setProperty("IHU_SN", String.valueOf(IHU_SN));
		properties.setProperty("catalogNumber", String.valueOf(catalogNumber));
		properties.setProperty("name", user_keps_name);
		properties.setProperty("description", description);
		properties.setProperty("model", String.valueOf(model));
		properties.setProperty("numberOfLookupTables", String.valueOf(numberOfLookupTables));
		for (int i=0; i < numberOfLookupTables; i++) {
			if (lookupTable[i] != null) 
				properties.setProperty("lookupTable"+i,lookupTable[i].getName());
			if (this.lookupTableFilename[i] != null)
				properties.setProperty("lookupTable"+i+".filename",lookupTableFilename[i]);
		}
		
		properties.setProperty("numberOfLayouts", String.valueOf(numberOfLayouts));
		for (int i=0; i < numberOfLayouts; i++) {
			if (this.layoutFilename[i] != null)
				properties.setProperty("layout"+i+".filename",layoutFilename[i]);
			if (this.layout[i] != null) {
				properties.setProperty("layout"+i+".name",layout[i].name);
				if (!layout[i].typeStr.equalsIgnoreCase(""))
					properties.setProperty("layout"+i+".type",layout[i].typeStr);
				if (layout[i].title != null)
					properties.setProperty("layout"+i+".title",layout[i].title);
				if (layout[i].shortTitle != null)
					properties.setProperty("layout"+i+".shortTitle",layout[i].shortTitle);
				if (layout[i].parentLayout != null)
					properties.setProperty("layout"+i+".parentLayout",layout[i].parentLayout);
				
			}
		}

		properties.setProperty("numberOfSources", String.valueOf(numberOfSources));
		
		for (int i=0; i < numberOfSources; i++) {
			if (sourceName[i] != null)
				properties.setProperty("source"+i+".name",sourceName[i]);
			if (sourceFormatName != null && sourceFormatName[i] != null)
				properties.setProperty("sourceFormat"+i+".name",sourceFormatName[i]);
		}
		
		properties.setProperty("measurementsFileName", measurementsFileName);
		properties.setProperty("passMeasurementsFileName", passMeasurementsFileName);
		
		// optional properties
		properties.setProperty("useConversionCoeffs", String.valueOf(useConversionCoeffs));
		/////// IF TRUE SAVE THE FILE NAMES HERE
		
		
		properties.setProperty("numberOfStringLookupTables", String.valueOf(numberOfStringLookupTables));
		for (int i=0; i < numberOfStringLookupTables; i++) {
			if (this.stringLookupTable[i] != null) {
				properties.setProperty("stringLookupTable"+i,stringLookupTable[i].getName());
				properties.setProperty("stringLookupTable"+i+".name",stringLookupTableFilename[i]);
			}
		}
		
		properties.setProperty("hasFOXDB_V3", String.valueOf(hasFOXDB_V3));
		
		// user params that need default value in master file
		properties.setProperty("telemetryDownlinkFreqkHz", String.valueOf(user_telemetryDownlinkFreqkHz));
		properties.setProperty("minFreqBoundkHz", String.valueOf(user_minFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", String.valueOf(user_maxFreqBoundkHz));
		properties.setProperty("track", String.valueOf(user_track));
		properties.setProperty("series", String.valueOf(series));
		properties.setProperty("priority", String.valueOf(user_priority));
		properties.setProperty("user_format", String.valueOf(user_format));
		properties.setProperty("displayName", String.valueOf(user_display_name));

		// Frame Layouts
		properties.setProperty("numberOfFrameLayouts", String.valueOf(numberOfFrameLayouts));				
		for (int i=0; i < numberOfFrameLayouts; i++) {
			if (this.frameLayout[i] != null) {
				properties.setProperty("frameLayout"+i+".name",frameLayout[0].name);
				properties.setProperty("frameLayout"+i+".filename",this.frameLayoutFilename[0]);
			}
		}

		if (user_localServer != null) {
			properties.setProperty("localServer",user_localServer);
			properties.setProperty("localServerPort", Integer.toString(user_localServerPort));
		}
		
		if (!this.hasFOXDB_V3)
			properties.setProperty("BATTERY_CURRENT_ZERO", Double.toString(user_BATTERY_CURRENT_ZERO));
		if (this.hasMpptSettings) {
			properties.setProperty("mpptResistanceError", Double.toString(user_mpptResistanceError));
			properties.setProperty("mpptSensorOffThreshold", Integer.toString(user_mpptSensorOffThreshold));
		}
		if (hasMemsRestValues) {
			properties.setProperty("memsRestValueX", Integer.toString(user_memsRestValueX));
			properties.setProperty("memsRestValueY", Integer.toString(user_memsRestValueY));
			properties.setProperty("memsRestValueZ", Integer.toString(user_memsRestValueZ));			
		}
		
		properties.setProperty("hasModeInHeader", Boolean.toString(hasModeInHeader));
		
		store_master_params();
	}
	
	private void loadConversionCurves(String conversionCurvesFileName) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(conversionCurvesFileName))) { // try with resource closes it
		    String line = br.readLine(); // read the header, which we ignore
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
		       // if (values.length == ConversionCurve.CSF_FILE_ROW_LENGTH)
		        try {
		        	ConversionCurve conversion = new ConversionCurve(values);
		        	if (conversions.containsKey(conversion.getName())) {
		        		// we have a namespace clash, warn the user
		        		Log.errorDialog("DUPLICATE CURVE NAME", this.user_keps_name + "- Conversion Curve already defined. This duplicate name will not be stored: " + conversion.getName());
		        	} else {
		        		conversions.put(conversion.getName(), conversion);
		        		Log.println("Stored: " + conversion);
		        	}
		        } catch (IllegalArgumentException e) {
		        	Log.println("Could not load conversion: " + e);
		        	Log.errorDialog("CORRUPT CONVERSION: ", e.toString());
		        	// ignore this corrupt row
		        }
		    }
		}
		
		
	}
	
	private void loadConversionExpresions(String conversionExpressionsFileName) throws FileNotFoundException, IOException, LayoutLoadException {
		try (BufferedReader br = new BufferedReader(new FileReader(conversionExpressionsFileName))) { // try with resource closes it
		    String line = br.readLine(); // read the header, which we ignore
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
		       // Don't check the length because we are allowed to have commas in the description
		        try {
		        	ConversionMathExpression conversion = new ConversionMathExpression(values[0], values[1]); // name, equation
		        	if (conversions.containsKey(conversion.getName())) {
		        		// we have a namespace clash, warn the user
		        		Log.errorDialog("DUPLICATE CONVERSION EXPRESSION NAME", this.user_keps_name + "- A Curve, expression or table is already defined called " + conversion.getName()
		        		+ "\nThis duplicate name will not be stored.");
		        	} else {
		        		conversions.put(conversion.getName(), conversion);
		        		Log.println("Expression loaded: " + conversion);
		        	}
		        } catch (IllegalArgumentException e) {
		        	Log.println("Could not load conversion: " + e);
		        	Log.errorDialog("CORRUPT CONVERSION: ", e.toString());
		        	// ignore this corrupt row
		        }
		    }
		}
		
		
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
	
//	public String getIdString() {
//		String id = "??";
//		id = Integer.toString(foxId);
//
//		return id;
//	}
	
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
	
	/**
	 * Return true if one of the experiment slots contains the HERCI experiment
	 * @return
	 */
	public boolean hasExperiment(int e) {
		for (int i=0; i< experiments.length; i++)
			if (experiments[i] == e) return true;
		return false;
	}
	
	public String getIdString() {
		String id = "??";
		if (foxId == 1) id = "1A";
		else if (foxId == 2) id = "1B";
		else if (foxId == 3) id = "1Cliff";
		else if (foxId == 4) id = "1D";
		else if (foxId == 5) id = "1E";
		else id = Integer.toString(foxId); // after the "fox-1" spacecraft just use the fox id

		return id;
	}
	
	public static String getModeString(int mode) {
		return Spacecraft.modeNames[mode];
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
	
	public static String determineModeString(Spacecraft fox, PayloadRtValues realTime, PayloadMaxValues maxPayload, 
			PayloadMinValues minPayload, FramePart radPayload) {
		int mode;

		mode = determine1A1DMode(realTime, maxPayload, minPayload, radPayload);
		return getModeString(mode);
	}

	public String toString() {
		return this.user_display_name; //"Fox-" + getIdString();
	}
	
	@Override
	public int compareTo(Spacecraft s2) {
		if (user_priority == s2.user_priority) 
			return user_display_name.compareTo(s2.user_display_name);
		else if (user_priority < s2.user_priority) return -1;
		else if (user_priority > s2.user_priority) return 1;
		return -1;
	}
	
}
