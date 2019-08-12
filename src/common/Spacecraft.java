package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import predict.FoxTLE;
import predict.PositionCalcException;
import predict.SortedTleList;
import telemetry.BitArrayLayout;
import telemetry.FrameLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.LookUpTable;
import telemetry.uw.CanFrames;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public abstract class Spacecraft implements Comparable<Spacecraft> {
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static final int ERROR_IDX = -1;
	
	// THESE HARD CODED LOOKUPS SHOULD NOT BE USED FOR NEW SPACECRAFT
	// Code a paramater into the spacecraft file that might work with future hardware, e.g. hsaCanBus
	// Or switch logic based on standard layouts defined below, or custom layouts if required e.g. camera formats
	public static final int FOX1A = 1;
	public static final int FOX1B = 2;
	public static final int FOX1C = 3;
	public static final int FOX1D = 4;
	public static final int FOX1E = 5;
	//public static final int HUSKY_SAT = 6;
	//public static final int GOLF_TEE = 7;
	//public static final int FUN_CUBE1 = 100;
	//public static final int FUN_CUBE2 = 101;
	
	public static final String[][] SOURCES = {
			{ "amsat.fox-test.ihu.duv", "amsat.fox-test.ihu.highspeed" },
			{ "amsat.fox-1a.ihu.duv", "amsat.fox-1a.ihu.highspeed" },
			{ "amsat.fox-1b.ihu.duv", "amsat.fox-1b.ihu.highspeed" },
			{ "amsat.fox-1c.ihu.duv", "amsat.fox-1c.ihu.highspeed" },
			{ "amsat.fox-1d.ihu.duv", "amsat.fox-1d.ihu.highspeed" },
			{ "amsat.fox-1e.ihu.bpsk", "amsat.fox-1e.ihu.bpsk" },
			{ "amsat.husky_sat.ihu.bpsk", "amsat.husky_sat.ihu.bpsk" },
			{ "amsat.golf-t.ihu.bpsk", "amsat.golf-t.ihu.bpsk" } };

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
	public String name = "Fox-1A";
	public int priority = 9; // set to low priority so new spacecraft are not suddenly ahead of old ones
	public String description = "";
	public int model;
	public double telemetryDownlinkFreqkHz = 145980;
	public double minFreqBoundkHz = 145970;
	public double maxFreqBoundkHz = 145990;
	
	public boolean telemetryMSBfirst = true;
	public boolean ihuLittleEndian = true;
	
	public String localServer = ""; // default to blank, otherwise we try to send to the local server
	public int localServerPort = 8587;
	
	public int numberOfLayouts = 4;
	public int numberOfDbLayouts = 4; // if we load additional layouts for CAN BUS then this stores the core layouts
	public String[] layoutFilename;
	//public String[] layoutName;
	public BitArrayLayout[] layout;
	private boolean[] sendLayoutLocally;  // CURRENTLY UNUSED SO MADE PRIVATE
	public CanFrames canFrames;
	
	public int numberOfLookupTables = 3;
	public String[] lookupTableFilename;
	//public String[] lookupTableName;
	public LookUpTable[] lookupTable;
	
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
	public boolean track = true; // default is we track a satellite
	public SatPos satPos; // cache the position when it gets calculated so others can read it
	public double satPosErrorCode; // Store the error code when we return null for the position
	public boolean hasCanBus;
	
	private SortedTleList tleList; // this is a list of TLEs loaded from the history file.  We search this for historical TLEs
	
	/*
	final String[] testTLE = {
            "AO-85",
            "1 40967U 15058D   16111.35540844  .00000590  00000-0  79740-4 0 01029",
            "2 40967 064.7791 061.1881 0209866 223.3946 135.0462 14.74939952014747"};
	*/
	
	public Spacecraft(File fileName ) throws LayoutLoadException, IOException {
		properties = new Properties();
		propertiesFile = fileName;	
		tleList = new SortedTleList(10);
	}
	
	public boolean isFox1() {
		if (foxId < 10) return true;
		return false;
	}
	
	public int getLayoutIdxByName(String name) {
		for (int i=0; i<numberOfLayouts; i++)
			if (layout[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	public int getLookupIdxByName(String name) {
		for (int i=0; i<numberOfLookupTables; i++)
			if (lookupTable[i].name.equalsIgnoreCase(name))
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

	public LookUpTable getLookupTableByName(String name) {
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
		if (satPos == null)
			return false;
		return (FramePart.radToDeg(satPos.getElevation()) >= 0);
	}
	
	public boolean sendToLocalServer() {
		if (localServer == null) return false;
		if (localServer.equalsIgnoreCase(""))
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
	
	protected void load() throws LayoutLoadException {
		// try to load the properties from a file
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.load(f);
			f.close();
		} catch (IOException e) {
			if (f!=null) try { f.close(); } catch (Exception e1) {};
			throw new LayoutLoadException("Could not load spacecraft files: " + propertiesFile.getAbsolutePath());
			
		}
		try {
			foxId = Integer.parseInt(getProperty("foxId"));
			catalogNumber = Integer.parseInt(getProperty("catalogNumber"));			
			name = getProperty("name");
			description = getProperty("description");
			model = Integer.parseInt(getProperty("model"));
			telemetryDownlinkFreqkHz = Double.parseDouble(getProperty("telemetryDownlinkFreqkHz"));			
			minFreqBoundkHz = Double.parseDouble(getProperty("minFreqBoundkHz"));
			maxFreqBoundkHz = Double.parseDouble(getProperty("maxFreqBoundkHz"));

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

			// Lookup Tables
			numberOfLookupTables = Integer.parseInt(getProperty("numberOfLookupTables"));
			lookupTableFilename = new String[numberOfLookupTables];
			lookupTable = new LookUpTable[numberOfLookupTables];
			for (int i=0; i < numberOfLookupTables; i++) {
				lookupTableFilename[i] = getProperty("lookupTable"+i+".filename");
				lookupTable[i] = new LookUpTable(lookupTableFilename[i]);
				lookupTable[i].name = getProperty("lookupTable"+i);
			}
			
			String t = getOptionalProperty("track");
			if (t == null) 
				track = true;
			else 
				track = Boolean.parseBoolean(t);
			String s = getOptionalProperty("series");
			if (s == null) 
				series = "FOX";
			else 
				series = s;
			String serv = getOptionalProperty("localServer");
			if (serv == null) 
				localServer = null;
			else 
				localServer = serv;
			String p = getOptionalProperty("localServerPort");
			if (p == null) 
				localServerPort = 0;
			else 
				localServerPort = Integer.parseInt(p);
//			for (int i=0; i < numberOfLayouts; i++) {
//				String l = getOptionalProperty("sendLayoutLocally"+i);
//				if (l != null)
//					sendLayoutLocally[i] = Boolean.parseBoolean(l);
//				else
//					sendLayoutLocally[i] = false;
//			}
			String pri = getOptionalProperty("priority");
			if (pri == null) 
				priority = 1;
			else 
				priority = Integer.parseInt(pri);
			String c = getOptionalProperty("hasCanBus");
			if (c == null) 
				hasCanBus = false;
			else 
				hasCanBus =  Boolean.parseBoolean(c);
			if (hasCanBus) {
				loadCanLayouts();
			}

		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing data value: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );		
		} catch (FileNotFoundException e) {
			e.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("File not found: "+ e.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath());
		}
	}
	
	/**
	 * We have a CAN Bus and need to load the additional layouts for CAN Packts
	 * These are defined in frames.csv in a subdirectory with the name of the spacecraft
	 */
	private void loadCanLayouts() {
		try {
			canFrames = new CanFrames(this.name+ File.separator +"frames.csv");
			int canLayoutNum = canFrames.NUMBER_OF_FIELDS;
			Log.println("Loading " + canLayoutNum + " CAN Layouts");
			BitArrayLayout[] existingLayouts = layout;
			layout = new BitArrayLayout[layout.length+canLayoutNum];
			int i = 0;
			for (BitArrayLayout l : existingLayouts)
				layout[i++] = l;
			for (String frameName : canFrames.frame) {
				layout[i] = new BitArrayLayout(this.name+ File.separator + frameName + ".csv");
				layout[i].name = frameName;
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

	protected String getProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new LayoutLoadException("Missing data value: " + key + " when loading Spacecraft file: \n" + propertiesFile.getAbsolutePath() );
//			throw new NullPointerException();
		}
		return value;
	}
	protected void store() {
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.store(new FileOutputStream(propertiesFile), "Fox 1 Telemetry Decoder Properties");
			f.close();
		} catch (FileNotFoundException e1) {
			if (f!=null) try { f.close(); } catch (Exception e2) {};
			Log.errorDialog("ERROR", "Could not write spacecraft file. Check permissions on run directory or on the file");
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			if (f!=null) try { f.close(); } catch (Exception e3) {};
			Log.errorDialog("ERROR", "Error writing spacecraft file");
			e1.printStackTrace(Log.getWriter());
		}
	}
	protected void save() {
		
		properties.setProperty("foxId", Integer.toString(foxId));
		properties.setProperty("catalogNumber", Integer.toString(catalogNumber));
		properties.setProperty("name", name);
		properties.setProperty("description", description);
		properties.setProperty("model", Integer.toString(model));
		properties.setProperty("telemetryDownlinkFreqkHz", Double.toString(telemetryDownlinkFreqkHz));
		properties.setProperty("minFreqBoundkHz", Double.toString(minFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Double.toString(maxFreqBoundkHz));
		properties.setProperty("track", Boolean.toString(track));
		
		if (localServer != null) {
			properties.setProperty("localServer",localServer);
			properties.setProperty("localServerPort", Integer.toString(localServerPort));
		}
		properties.setProperty("priority", Integer.toString(priority));
	}
	
	public String getIdString() {
		String id = "??";
		id = Integer.toString(foxId);

		return id;
	}
	
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(Spacecraft s2) {
		if (priority == s2.priority) 
			return name.compareTo(s2.name);
		else if (priority < s2.priority) return -1;
		else if (priority > s2.priority) return 1;
		return -1;
	}
	
}
