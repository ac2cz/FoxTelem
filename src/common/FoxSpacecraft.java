package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.joda.time.DateTime;

import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;

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
	public static final int EXP_VULCAN = 1; // This is the LEP experiment
	public static final int EXP_VT_CAMERA = 2;
	public static final int EXP_IOWA_HERCI = 3;
	public static final int EXP_RAD_FX_SAT = 4;
	public static final int EXP_VT_CAMERA_LOW_RES = 5;
	public static final int EXP_VANDERBILT_VUC = 6; // This is the controller and does not have its own telem file
	
	public static final String[] expNames = {
		"Empty",
		"Vanderbilt LEP",
		"Virgina Tech Camera",
		"University of Iowa HERCI",
		"Rad FX Sat",
		"Virginia Tech Low-res Camera",
		"Vanderbilt VUC"
	};
	
	
	public int IHU_SN = 0;
	public int[] experiments = {EXP_EMPTY, EXP_EMPTY, EXP_EMPTY, EXP_EMPTY};
	
	
	// Calibration
	public double BATTERY_CURRENT_ZERO = 0;

	// layout flags
	public boolean useIHUVBatt = false;

	ArrayList<Long> timeZero = null;
	
	public FoxSpacecraft(File fileName ) throws LayoutLoadException, IOException {
		super(fileName);
		load(); // don't call load until this constructor has started and the variables have been initialized
		try {
			loadTimeZeroSeries(null);
		} catch (FileNotFoundException e) {
			timeZero = null;
		} catch (IndexOutOfBoundsException e) {
			timeZero = null;
		}
		measurementLayout = new BitArrayLayout(measurementsFileName);
		if (passMeasurementsFileName != null)
			passMeasurementLayout = new BitArrayLayout(passMeasurementsFileName);
		loadTleHistory(); // DOnt call this until the Name and FoxId are set
	}

	public static final DateFormat timeDateFormat = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat dateDateFormat = new SimpleDateFormat("dd MMM yy");
	
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

	public DateTime getUtcDateTimeForReset(int reset, long uptime) {
		if (timeZero == null) return null;
		if (reset >= timeZero.size()) return null;
		Date dt = new Date(timeZero.get(reset) + uptime*1000);
		DateTime dateTime = new DateTime(dt); // FIXME - this date conversion is not working.  Need to understand how it works.
		return dateTime;
	}
	
	public void save() {
		super.save();
		properties.setProperty("IHU_SN", Integer.toString(IHU_SN));
		for (int i=0; i< experiments.length; i++)
			properties.setProperty("EXP"+(i+1), Integer.toString(experiments[i]));
		
		properties.setProperty("BATTERY_CURRENT_ZERO", Double.toString(BATTERY_CURRENT_ZERO));
		properties.setProperty("useIHUVBatt", Boolean.toString(useIHUVBatt));
		properties.setProperty("measurementsFileName", measurementsFileName);
		properties.setProperty("passMeasurementsFileName", passMeasurementsFileName);
		
		store();
	
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
        } finally {
        	try {
				dis.close();
			} catch (IOException e) {
				// ignore error
			}
        }
		return hasContent;
	}
	
	protected void load() throws LayoutLoadException {
		super.load();
		try {
			IHU_SN = Integer.parseInt(getProperty("IHU_SN"));
			for (int i=0; i< experiments.length; i++)
				experiments[i] = Integer.parseInt(getProperty("EXP"+(i+1)));
			
			BATTERY_CURRENT_ZERO = Double.parseDouble(getProperty("BATTERY_CURRENT_ZERO"));
		
			useIHUVBatt = Boolean.parseBoolean(getProperty("useIHUVBatt"));

			measurementsFileName = getProperty("measurementsFileName");
			passMeasurementsFileName = getProperty("passMeasurementsFileName");
			
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
	
	public String toString() {

		return this.name; //"Fox-" + getIdString();

	}
}
