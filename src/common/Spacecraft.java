package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.joda.time.DateTime;

import FuncubeDecoder.FUNcubeSpacecraft;
import decoder.Decoder;
import decoder.SourceAudio;
import decoder.KiwiSat.KiwiSatSpacecraft;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.LookUpTable;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public abstract class Spacecraft {
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	public static final int ERROR_IDX = -1;
	
	public static final int FOX1A = 1;
	public static final int FOX1B = 2;
	public static final int FOX1C = 3;
	public static final int FOX1D = 4;
	public static final int FOX1E = 5;
	public static final int FUN_CUBE1 = 100;
	public static final int FUN_CUBE2 = 101;
	public static final int KIWI_SAT = 102;
	
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
	
	public static final String RSSI_LOOKUP = "RSSI";
	public static final String IHU_VBATT_LOOKUP = "IHU_VBATT";
	public static final String IHU_TEMP_LOOKUP = "IHU_TEMP";

	
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
	public String series = "Fox";
	public String name = "Fox-1A";
	public String description = "";
	public int model;
	public int telemetryDownlinkFreqkHz = 145980;
	public int minFreqBoundkHz = 145970;
	public int maxFreqBoundkHz = 145990;
	
	public boolean telemetryMSBfirst = true;
	public boolean ihuLittleEndian = true;
	
	public int numberOfLayouts = 4;
	public String[] layoutFilename;
	//public String[] layoutName;
	public BitArrayLayout[] layout;
	 	
	public int numberOfLookupTables = 3;
	public String[] lookupTableFilename;
	//public String[] lookupTableName;
	public LookUpTable[] lookupTable;
	
	public String measurementsFileName;
	public String passMeasurementsFileName;
	public BitArrayLayout measurementLayout;
	public BitArrayLayout passMeasurementLayout;
	
	public int numberOfFrameLayouts = 1;
	public String[] frameLayoutFilename;
	//public FrameLayout[] frameLayout;
	
	// User Config
	public boolean track = true; // default is we track a satellite
	
	final String[] TLE = {
            "AO-85",
            "1 40967U 15058D   16111.35540844  .00000590  00000-0  79740-4 0 01029",
            "2 40967 064.7791 061.1881 0209866 223.3946 135.0462 14.74939952014747"};
	
	/**
	 * Factory method to create the right Spacecraft given the spacecraft ID
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 * @throws LayoutLoadException
	 */
	public static Spacecraft makeSpacecraft(File fileName) throws FileNotFoundException, LayoutLoadException {
		Properties properties = new Properties();
		try {
			FileInputStream f=new FileInputStream(fileName);
			properties.load(f);
		} catch (IOException e) {
			throw new LayoutLoadException("Could not load spacecraft files: " + fileName.getAbsolutePath());
		}
		try {
			int foxId = Integer.parseInt(properties.getProperty("foxId"));
			switch (foxId) {
			case FUN_CUBE1:
				return new FUNcubeSpacecraft(fileName);
			case FUN_CUBE2:
				return new FUNcubeSpacecraft(fileName);
			case KIWI_SAT:
				return new KiwiSatSpacecraft(fileName);
			default:
				return new FoxSpacecraft(fileName);
			}
		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + fileName.getAbsolutePath() );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing data value: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + fileName.getAbsolutePath() );		
		}
	}
	
	public Spacecraft(File fileName ) throws FileNotFoundException, LayoutLoadException {
		properties = new Properties();
		propertiesFile = fileName;		
	}
	
	public abstract Decoder getDecoder(String n, SourceAudio as, int chan, int mode);
	
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

	private TLE getTLEbyDate(DateTime dateTime) {
		final TLE tle = new TLE(TLE);
		return tle;
	}
	
	public SatPos getSatellitePosition(DateTime timeNow) {
		final TLE tle = getTLEbyDate(timeNow);
		final Satellite satellite = SatelliteFactory.createSatellite(tle);
        final SatPos satellitePosition = satellite.getPosition(Config.GROUND_STATION, timeNow.toDate());
		
		return satellitePosition;
	}
	
	protected void load() throws LayoutLoadException {
		// try to load the properties from a file
		try {
			FileInputStream f=new FileInputStream(propertiesFile);
			properties.load(f);
		} catch (IOException e) {
			throw new LayoutLoadException("Could not load spacecraft files: " + propertiesFile.getAbsolutePath());
		}
		try {
			foxId = Integer.parseInt(getProperty("foxId"));
			catalogNumber = Integer.parseInt(getProperty("catalogNumber"));			
			name = getProperty("name");
			description = getProperty("description");
			model = Integer.parseInt(getProperty("model"));
			telemetryDownlinkFreqkHz = Integer.parseInt(getProperty("telemetryDownlinkFreqkHz"));			
			minFreqBoundkHz = Integer.parseInt(getProperty("minFreqBoundkHz"));
			maxFreqBoundkHz = Integer.parseInt(getProperty("maxFreqBoundkHz"));
			measurementsFileName = getProperty("measurementsFileName");
			passMeasurementsFileName = getProperty("passMeasurementsFileName");
			
			measurementLayout = new BitArrayLayout(measurementsFileName);
			if (passMeasurementsFileName != null)
				passMeasurementLayout = new BitArrayLayout(passMeasurementsFileName);

			
			// Frame Layouts
			/**
			numberOfFrameLayouts = Integer.parseInt(getProperty("numberOfFrameLayouts"));
			frameLayoutFilename = new String[numberOfFrameLayouts];
			frameLayout = new FrameLayout[numberOfFrameLayouts];
			for (int i=0; i < numberOfFrameLayouts; i++) {
				frameLayoutFilename[i] = getProperty("frameLayout"+i+".filename");
				frameLayout[i] = new FrameLayout(frameLayoutFilename[i]);
				frameLayout[i].name = getProperty("frameLayout"+i+".name");
			}
			*/
			
			
			// Telemetry Layouts
			numberOfLayouts = Integer.parseInt(getProperty("numberOfLayouts"));
			layoutFilename = new String[numberOfLayouts];
			layout = new BitArrayLayout[numberOfLayouts];
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
		try {
			properties.store(new FileOutputStream(propertiesFile), "Fox 1 Telemetry Decoder Properties");
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write spacecraft file. Check permissions on run directory or on the file");
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
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
		properties.setProperty("telemetryDownlinkFreqkHz", Integer.toString(telemetryDownlinkFreqkHz));
		properties.setProperty("minFreqBoundkHz", Integer.toString(minFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("track", Boolean.toString(track));
		properties.setProperty("measurementsFileName", measurementsFileName);
		properties.setProperty("passMeasurementsFileName", passMeasurementsFileName);

	}
	
	public String getIdString() {
		String id = "??";
		id = Integer.toString(foxId);

		return id;
	}
	
	public String toString() {
		return name;
	}
	
}
