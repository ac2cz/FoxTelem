package telemetry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import common.Log;

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
 * This class holds the layout for the telemetry for a given satelite.  It is loaded from a CSV file at program start
 * 
 * The layout will not change during the life of the program for a given satellite, so no provision is added for version control
 * or loading old formats.
 * 
 *
 */
public class BitArrayLayout {
	public int NUMBER_OF_FIELDS = 0;
	
	public String fileName;
	
	public static final String NONE = "NONE";
	
	public String[] fieldName = null;  // name of the field that the bits correspond to
	public int[] conversion = null; // the conversion routine to change raw bits into a real value
	public String[] fieldUnits = null; // the units as they would be displayed on a graph e.g. C for Celcius
	public int[] fieldBitLength = null; // the number of bits in this field
	public String[] module = null; // the module on the display screen that this would be shown in e.g. Radio
	public int[] moduleNum = null; // the order that the module is displayed on the screen with 1-9 in the top and 10-19 in the bottom
	public int[] moduleLinePosition = null; // the line position in the module, starting from 1
	public int[] moduleDisplayType = null; // a code determining if this is displayed across all columns or in the RT, MIN, MAX columns.  Defined in DisplayModule

	public String[] shortName = null;
	public String[] description = null;

	public static final int CONVERT_NONE = 0;
	public static final int CONVERT_INTEGER = 1;
	public static final int CONVERT_V25_SENSOR = 2;
	public static final int CONVERT_V3_SENSOR = 3;
	public static final int CONVERT_BATTERY = 4;
	public static final int CONVERT_SOLAR_PANEL = 5;
	public static final int CONVERT_SOLAR_PANEL_TEMP = 6;
	public static final int CONVERT_TEMP = 7;
	public static final int CONVERT_BATTERY_TEMP = 8;
	public static final int CONVERT_BATTERY_CURRENT = 9;
	public static final int CONVERT_PA_CURRENT = 10;
	public static final int CONVERT_PSU_CURRENT = 11;
	public static final int CONVERT_SPIN = 12;
	public static final int CONVERT_MEMS_ROTATION = 13;
	public static final int CONVERT_RSSI = 14;
	public static final int CONVERT_IHU_TEMP = 15;
	public static final int CONVERT_ANTENNA = 16;
	public static final int CONVERT_STATUS_BIT = 17;
	public static final int CONVERT_IHU_DIAGNOSTIC = 18;
	public static final int CONVERT_HARD_ERROR = 19;
	public static final int CONVERT_SOFT_ERROR = 20;
	public static final int CONVERT_BOOLEAN = 21;
	public static final int CONVERT_MPPT_CURRENT = 22;
	public static final int CONVERT_MPPT_SOLAR_PANEL = 23;
	public static final int CONVERT_MPPT_SOLAR_PANEL_TEMP = 24;
	public static final int CONVERT_16_SEC_UPTIME = 25;
	public static final int CONVERT_FREQ = 26;
	public static final int CONVERT_VULCAN_STATUS = 27;

	/**
	 * Create an empty layout for manual init
	 */
	BitArrayLayout() {
		
	}
	
	/**
	 * Create a layout and load it from the file with the given path
	 * @param f
	 * @throws FileNotFoundException
	 * @throws LayoutLoadException 
	 */
	public BitArrayLayout(String f) throws FileNotFoundException, LayoutLoadException {
		load(f);
	}
	
	public boolean hasFieldName(String name) {
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				return true;
		}
		return false;
	}
	
	public int getConversionByName(String name) {
		int pos = -1;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == -1) {
			return BitArrayLayout.CONVERT_NONE;
		} else {
			return (conversion[pos]);
		}
	}

	
	protected void load(String f) throws FileNotFoundException, LayoutLoadException {

		String line;
		fileName = "spacecraft" +File.separator + f;
	//	File aFile = new File(fileName);
		
		Log.println("Loading layout: "+ fileName);
		BufferedReader dis = new BufferedReader(new FileReader(fileName));
		int field=0;
		try {
			line = dis.readLine(); // read the header, and only look at first item, the number of fields.
			StringTokenizer header = new StringTokenizer(line, ",");
			NUMBER_OF_FIELDS = Integer.valueOf(header.nextToken()).intValue();			
			fieldName = new String[NUMBER_OF_FIELDS];		
			conversion = new int[NUMBER_OF_FIELDS];
			fieldBitLength = new int[NUMBER_OF_FIELDS];
			fieldUnits = new String[NUMBER_OF_FIELDS];
			module = new String[NUMBER_OF_FIELDS];
			moduleLinePosition = new int[NUMBER_OF_FIELDS];
			moduleNum = new int[NUMBER_OF_FIELDS];
			moduleDisplayType = new int[NUMBER_OF_FIELDS];
			shortName = new String[NUMBER_OF_FIELDS];
			description = new String[NUMBER_OF_FIELDS];
			
			while ((line = dis.readLine()) != null) {
				if (line != null) {
					StringTokenizer st = new StringTokenizer(line, ",");

					@SuppressWarnings("unused")
					int fieldId = Integer.valueOf(st.nextToken()).intValue();
					@SuppressWarnings("unused")
					String type = st.nextToken();
					fieldName[field] = st.nextToken();
					fieldBitLength[field] = Integer.valueOf(st.nextToken()).intValue();
					fieldUnits[field] = st.nextToken();
					conversion[field] = Integer.valueOf(st.nextToken()).intValue();
					module[field] = st.nextToken();					
					moduleNum[field] = Integer.valueOf(st.nextToken()).intValue();
					moduleLinePosition[field] = Integer.valueOf(st.nextToken()).intValue();
					moduleDisplayType[field] = Integer.valueOf(st.nextToken()).intValue();
					shortName[field] = st.nextToken();
					description[field] = st.nextToken();
					field++;
				}
			}
			dis.close();
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());

		} catch (NumberFormatException n) {
			Log.errorDialog("NUMBER FORMAT EXCEPTION", n.getMessage());
			n.printStackTrace(Log.getWriter());
		} catch (IndexOutOfBoundsException n) {
			Log.errorDialog("INDEX EXCEPTION", "Error loading Layout at Index: " + n.getMessage());
			n.printStackTrace(Log.getWriter());
		} catch (NoSuchElementException n) {
			Log.errorDialog("Missing Field in Layout File", "Halted loading " + fileName);
			n.printStackTrace(Log.getWriter());
		}
		if (NUMBER_OF_FIELDS != field) throw new LayoutLoadException("Error loading fields from " + fileName +
				". Expected " + NUMBER_OF_FIELDS + " fields , but loaded " + field);

	}
	
	public String getTableCreateStmt() {
		String s = new String();
		s = s + "(captureDate varchar(14), id int, resets int, uptime bigint, type int, ";
		for (int i=0; i < fieldName.length; i++) {
			s = s + fieldName[i] + " int,\n";
		}
		// We use serial for the type, except for type 4 where we use it for the payload number.  This allows us to have
		// multiple high speed records with the same reset and uptime
		s = s + "PRIMARY KEY (id, resets, uptime, type))";
		return s;
	}

}
