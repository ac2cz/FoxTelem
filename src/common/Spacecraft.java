package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.LookUpTable;

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
public class Spacecraft {
	
	public Properties properties; // Java properties file for user defined values
	public String propertiesFileName;
	
	public static String SPACECRAFT_DIR = "spacecraft";
	
	// Model Versions
	public static final int EM = 0;
	public static final int FM = 1;
	public static final int FS = 2;
	
	// Flattened ENUM for spacecraft name
	public static final String[] modelNames = {
		"Engineering Model",
		"Flight Model",
		"Flight Spare"
	};
	
	public static final String[] models = {
		"EM",
		"FM",
		"FS"
	};
	
	public static final int EXP_EMPTY = 0;
	public static final int EXP_VULCAN = 1;
	public static final int EXP_VT_CAMERA = 2;
	public static final int EXP_IOWA_HERCI = 3;
	public static final int EXP_RAD_FX_SAT = 4;
	
	public static final String[] expNames = {
		"Empty",
		"Vanderbilt LEP",
		"Virgina Tech Camera",
		"IOWA HERCI",
		"Rad FX Sat"
	};
	
	public int foxId = 1;
	public int catalogNumber = 0;
	public String name = "Fox-1A";
	public String description = "";
	public int model = FM;
	public int IHU_SN = 0;
	public int[] experiments = {EXP_EMPTY, EXP_EMPTY, EXP_EMPTY, EXP_EMPTY};
	public int telemetryDownlinkFreqkHz = 145980;
	public int minFreqBoundkHz = 145970;
	public int maxFreqBoundkHz = 145990;
	
	// Calibration
	public double BATTERY_CURRENT_ZERO = 0;
	public String rssiLookUpTableFileName = "";
	public String ihuTempLookUpTableFileName = "";
	public String ihuVBattLookUpTableFileName = "";

	// layout flags
	public boolean useIHUVBatt = false;

	// layout filenames
	public String rtLayoutFileName;
	public String maxLayoutFileName;
	public String minLayoutFileName;
	public String radLayoutFileName;
	public String rad2LayoutFileName;
	public String measurementsFileName;
	public String passMeasurementsFileName;

	// Stored tables
	public LookUpTable rssiTable;
	public LookUpTable ihuTable;
	public LookUpTable ihuVBattTable;

	//Telemetry layouts
	public BitArrayLayout rtLayout;
	public BitArrayLayout maxLayout;
	public BitArrayLayout minLayout;
	public BitArrayLayout radLayout;
	public BitArrayLayout rad2Layout;
	public BitArrayLayout measurementLayout;
	public BitArrayLayout passMeasurementLayout;
		
	// User Config
	public boolean track = true; // default is we track a satellite
	ArrayList<Long> timeZero = null;
	
	public Spacecraft(String fileName ) throws FileNotFoundException, LayoutLoadException {
		properties = new Properties();
		propertiesFileName = fileName;
		load();
		try {
			loadTimeZeroSeries();
		} catch (FileNotFoundException e) {
			timeZero = null;
		} catch (IndexOutOfBoundsException e) {
			timeZero = null;
		}
		rtLayout = new BitArrayLayout(rtLayoutFileName);
		maxLayout = new BitArrayLayout(maxLayoutFileName);
		minLayout = new BitArrayLayout(minLayoutFileName);
		radLayout = new BitArrayLayout(radLayoutFileName);
		rad2Layout = new BitArrayLayout(rad2LayoutFileName);
		measurementLayout = new BitArrayLayout(measurementsFileName);
		if (passMeasurementsFileName != null)
			passMeasurementLayout = new BitArrayLayout(passMeasurementsFileName);
		
		if (!rssiLookUpTableFileName.equalsIgnoreCase(""))
			rssiTable = new LookUpTable(rssiLookUpTableFileName);
		if (!ihuTempLookUpTableFileName.equalsIgnoreCase(""))
			ihuTable = new LookUpTable(ihuTempLookUpTableFileName);
		if (!ihuVBattLookUpTableFileName.equalsIgnoreCase(""))
			ihuVBattTable = new LookUpTable(ihuVBattLookUpTableFileName);
		if (ihuVBattLookUpTableFileName.equalsIgnoreCase("") && useIHUVBatt == true)
			throw new LayoutLoadException("File: "+fileName + "\nCan't load a satellite that uses IHU VBatt if the ihuVBatt look-up table is missing");
		
	}

	public static final DateFormat timeDateFormat = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat dateDateFormat = new SimpleDateFormat("dd MMM yyyy");
	
	public boolean hasTimeZero() { 
		if (timeZero == null) return false;
		return true;
	}
	
	public boolean hasTimeZero(int reset) { 
		if (timeZero == null) return false;
		if (reset >= timeZero.size()) return false;
		return true;
	}
	
	public String[][] getT0TableData() {
		if (timeZero == null) return null;
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
		timeDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String time = dateDateFormat.format(dt);
		return time;
	}

	
	public void save() {
		properties.setProperty("foxId", Integer.toString(foxId));
		properties.setProperty("catalogNumber", Integer.toString(catalogNumber));
		properties.setProperty("name", name);
		properties.setProperty("description", description);
		properties.setProperty("model", Integer.toString(model));
		properties.setProperty("IHU_SN", Integer.toString(IHU_SN));
		for (int i : experiments)
			properties.setProperty("EXP"+(i+1), Integer.toString(experiments[i]));
		properties.setProperty("telemetryDownlinkFreqkHz", Integer.toString(telemetryDownlinkFreqkHz));
		properties.setProperty("minFreqBoundkHz", Integer.toString(minFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("BATTERY_CURRENT_ZERO", Double.toString(BATTERY_CURRENT_ZERO));
		properties.setProperty("rssiLookUpTableFileName", rssiLookUpTableFileName);
		properties.setProperty("ihuTempLookUpTableFileName", ihuTempLookUpTableFileName);
		properties.setProperty("ihuVBattLookUpTableFileName", ihuVBattLookUpTableFileName);
		properties.setProperty("useIHUVBatt", Boolean.toString(useIHUVBatt));
		properties.setProperty("rtLayoutFileName", rtLayoutFileName);
		properties.setProperty("maxLayoutFileName", maxLayoutFileName);
		properties.setProperty("minLayoutFileName", minLayoutFileName);
		properties.setProperty("radLayoutFileName", radLayoutFileName);
		properties.setProperty("rad2LayoutFileName", rad2LayoutFileName);
		properties.setProperty("measurementsFileName", measurementsFileName);
		properties.setProperty("passMeasurementsFileName", passMeasurementsFileName);
		properties.setProperty("track", Boolean.toString(track));
		store();
	
	}
	
	private void store() {
		try {
			properties.store(new FileOutputStream(Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator + propertiesFileName), "Fox 1 Telemetry Decoder Properties");
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write spacecraft file. Check permissions on run directory or on the file");
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error writing spacecraft file");
			e1.printStackTrace(Log.getWriter());
		}
	}
	
	public void loadTimeZeroSeries() throws FileNotFoundException {
		timeZero = new ArrayList<Long>(100);
        String line;
        String log = "FOX"+ foxId + Config.t0UrlFile;
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
			Log.println("Loading: " + log);
		}
        //File aFile = new File(log );

        
        @SuppressWarnings("resource")
		BufferedReader dis = new BufferedReader(new FileReader(log));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			StringTokenizer st = new StringTokenizer(line, ",");
        			int reset = Integer.valueOf(st.nextToken()).intValue();
        			long uptime = Long.valueOf(st.nextToken()).longValue();
        			Log.println("Loaded T0: " + reset + ": " + uptime);
        			if (reset == timeZero.size())
        				timeZero.add(uptime);
        			else throw new IndexOutOfBoundsException("Reset in T0 file is missing or out of sequence: " + reset);
        		}
        	}
			dis.close();
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());
        	
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        }
		
	}
	
	private void load() throws LayoutLoadException {
		// try to load the properties from a file
		try {
			FileInputStream f=new FileInputStream(Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName);
			properties.load(f);
		} catch (IOException e) {
			throw new LayoutLoadException("Could not load spacecraft files: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName);
		}
		try {
			foxId = Integer.parseInt(getProperty("foxId"));
			catalogNumber = Integer.parseInt(getProperty("catalogNumber"));			
			name = getProperty("name");
			description = getProperty("description");
			model = Integer.parseInt(getProperty("model"));
			IHU_SN = Integer.parseInt(getProperty("IHU_SN"));
			for (int i=0; i< experiments.length; i++)
				experiments[i] = Integer.parseInt(getProperty("EXP"+(i+1)));
			telemetryDownlinkFreqkHz = Integer.parseInt(getProperty("telemetryDownlinkFreqkHz"));			
			minFreqBoundkHz = Integer.parseInt(getProperty("minFreqBoundkHz"));
			maxFreqBoundkHz = Integer.parseInt(getProperty("maxFreqBoundkHz"));
			BATTERY_CURRENT_ZERO = Double.parseDouble(getProperty("BATTERY_CURRENT_ZERO"));
			rssiLookUpTableFileName = getProperty("rssiLookUpTableFileName");
			ihuTempLookUpTableFileName = getProperty("ihuTempLookUpTableFileName");
			ihuVBattLookUpTableFileName = getProperty("ihuVBattLookUpTableFileName");			
			useIHUVBatt = Boolean.parseBoolean(getProperty("useIHUVBatt"));
			rtLayoutFileName = getProperty("rtLayoutFileName");
			maxLayoutFileName = getProperty("maxLayoutFileName");
			minLayoutFileName = getProperty("minLayoutFileName");
			radLayoutFileName = getProperty("radLayoutFileName");
			rad2LayoutFileName = getProperty("rad2LayoutFileName");
			measurementsFileName = getProperty("measurementsFileName");
			passMeasurementsFileName = getProperty("passMeasurementsFileName");
			String t = getOptionalProperty("track");
			if (t == null) 
				track = true;
			else 
				track = Boolean.parseBoolean(t);
			
		} catch (NumberFormatException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Corrupt data found when loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );
		} catch (NullPointerException nf) {
			nf.printStackTrace(Log.getWriter());
			throw new LayoutLoadException("Missing data value when loading Spacecraft file: " + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );		
		}

	}
	
	
	private String getOptionalProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return value;
	}

	private String getProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new LayoutLoadException("Missing data value: " + key + " when loading Spacecraft file: \n" + Config.currentDir + File.separator + SPACECRAFT_DIR + File.separator +propertiesFileName );
//			throw new NullPointerException();
		}
		return value;
	}

	/**
	 * Return true if one of the experiment slots contains the Virginia Tech Camera
	 * @return
	 */
	public boolean hasCamera() {
		for (int i=0; i< experiments.length; i++)
			if (experiments[i] == EXP_VT_CAMERA) return true;
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
	
	public String toString() {

		return "Fox-" + getIdString();

	}
}
