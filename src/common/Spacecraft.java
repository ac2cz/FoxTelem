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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import decoder.SourceIQ;
import gui.SourceTab;
import predict.FoxTLE;
import predict.PositionCalcException;
import predict.SortedTleList;
import telemetry.BitArrayLayout;
import telemetry.Conversion;
import telemetry.ConversionCurve;
import telemetry.FrameLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.ConversionLookUpTable;
import telemetry.TelemFormat;
import telemetry.uw.CanFrames;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public abstract class Spacecraft implements Comparable<Spacecraft> {
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	SatelliteManager satManager;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static final int ERROR_IDX = -1;
	
	// THESE HARD CODED LOOKUPS SHOULD NOT BE USED FOR NEW SPACECRAFT
	// Code a paramater into the spacecraft file that might work with future hardware, e.g. hsaCanBus
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
	
	public int foxId = 1;
	public int catalogNumber = 0;
	public String series = "FOX";
	public String description = "";
	public int model;
	public String canFileDir = "HuskySat";
	public int user_format = SourceTab.FORMAT_FSK_DUV;
	
	public boolean telemetryMSBfirst = true;
	public boolean ihuLittleEndian = true;
		
	public int numberOfLayouts = 4;
	public int numberOfDbLayouts = 4; // if we load additional layouts for CAN BUS then this stores the core layouts
	public String[] layoutFilename;
	public BitArrayLayout[] layout;
	private boolean[] sendLayoutLocally;  // CURRENTLY UNUSED SO MADE PRIVATE
	public CanFrames canFrames;
	
	public int numberOfLookupTables = 3;
	public String[] lookupTableFilename;
	public ConversionLookUpTable[] lookupTable;
	
	public int numberOfSources = 2;
	public String[] sourceName;
	public String[] sourceFormatName;
	public TelemFormat[] sourceFormat;
	
	public String measurementsFileName;
	public String passMeasurementsFileName;
	public BitArrayLayout measurementLayout;
	public BitArrayLayout passMeasurementLayout;
	
	public static final String MEASUREMENTS = "measurements";
	public static final String PASS_MEASUREMENTS = "passmeasurements";
	
	public int numberOfFrameLayouts = 1;
	public String[] frameLayoutFilename;
	public FrameLayout[] frameLayout;
	
	// User Config
	public static final String USER_ = "user_";
	public Properties user_properties; // Java properties file for user defined values
	public File userPropertiesFile;
	public String user_display_name = "Fox-1A";
	public String user_keps_name = "Fox-1A";
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
	
	private SortedTleList tleList; // this is a list of TLEs loaded from the history file.  We search this for historical TLEs
	
	public boolean useConversionCoeffs = false;
	private HashMap<String, Conversion> conversions;
	
	/*
	final String[] testTLE = {
            "AO-85",
            "1 40967U 15058D   16111.35540844  .00000590  00000-0  79740-4 0 01029",
            "2 40967 064.7791 061.1881 0209866 223.3946 135.0462 14.74939952014747"};
	*/
	
	/**
	 * Initialize the spacecraft settings
	 * 
	 * @param masterFileName
	 * @param userFileName
	 * @throws LayoutLoadException
	 * @throws IOException
	 */
	public Spacecraft(SatelliteManager satManager, File masterFileName, File userFileName ) throws LayoutLoadException, IOException {
		this.satManager = satManager;
		properties = new Properties();
		propertiesFile = masterFileName;	
		
		userPropertiesFile = userFileName;	
		tleList = new SortedTleList(10);
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
		String file = FoxSpacecraft.SPACECRAFT_DIR + File.separator + series + this.foxId + ".tle";
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
		String file = FoxSpacecraft.SPACECRAFT_DIR + File.separator + series + this.foxId + ".tle";
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
					frameLayout[i] = new FrameLayout(FoxSpacecraft.SPACECRAFT_DIR + File.separator + frameLayoutFilename[i]);
					frameLayout[i].name = getProperty("frameLayout"+i+".name");
				}
			}
			
			// Conversions
			if (useConversionCoeffs) {
				String conversionCurvesFileName = getOptionalProperty("conversionCurvesFileName");
				if (conversionCurvesFileName != null) {
					loadConversionCurves(FoxSpacecraft.SPACECRAFT_DIR + File.separator + conversionCurvesFileName);
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
				layout[i].name = getProperty("layout"+i+".name");
				layout[i].parentLayout = getOptionalProperty("layout"+i+".parentLayout");
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
	
	
	abstract protected void save();

	
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
	}
	
	private void loadConversionCurves(String conversionCurvesFileName) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(conversionCurvesFileName))) {
		    String line = br.readLine(); // read the header, which we ignore
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(",");
		        if (values.length == ConversionCurve.CSF_FILE_ROW_LENGTH)
		        try {
		        	ConversionCurve conversion = new ConversionCurve(values);
		        	if (conversions.containsKey(conversion.getName())) {
		        		// we have a namespace clash, warn the user
		        		Log.errorDialog("DUPLICATE CURVE NAME", this.user_keps_name + "- Conversion Curve already defined and will not be stored: " + conversion.getName());
		        	} else {
		        		conversions.put(conversion.getName(), conversion);
		        		Log.println("Stored: " + conversion);
		        	}
		        } catch (IllegalArgumentException e) {
		        	Log.println("Could not load conversion: " + e);
		        	// ignore this corrupt row
		        }
		    }
		}
	}
	
	
	public String getIdString() {
		String id = "??";
		id = Integer.toString(foxId);

		return id;
	}
	
	public String toString() {
		return user_display_name;
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
