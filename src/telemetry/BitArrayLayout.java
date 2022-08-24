package telemetry;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import common.Config;
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
 * This class holds the layout for the telemetry for a given satellite.  It is loaded from a CSV file at program start
 * 
 * The layout will not change during the life of the program for a given satellite, so no provision is added for version control
 * or loading old formats.
 * 
 *
 */
public class BitArrayLayout {
	public static final String RT = "RT";
	public static final String MAX = "MAX";
	public static final String MIN = "MIN";
	public static final String WOD = "WOD";
	public static final String EXP = "EXP";
	public static final String WOD_EXP = "WOD_EXP";
	public static final String CAN_EXP = "CAN_EXP"; // a Payload filled with CAN packets
	public static final String CAN_WOD_EXP = "CAN_WOD_EXP"; // a WOD payload filled with CAN packets
	public static final String CAN_PKT = "CAN_PKT"; // individual can packet
	public static final String WOD_CAN_PKT = "WOD_CAN_PKT"; // individual can packet from WOD
	public static final String DBG = "DBG";
	
	public static final String[] types = {
			"RT","MAX","MIN","WOD","EXP","WOD_EXP","CAN_EXP", "CAN_WOD_EXP", "CAN_PKT","WOD_CAN_PKT","DGB"
	};

	public int NUMBER_OF_FIELDS = 0;
	public static int ERROR_POSITION = -1;
	
	public String fileName;
	public String tableName; // the name of the database table this is stored in if SQL backend
	public String name; // the name, which is stored in the spacecraft file and used to index the layouts
	public String parentLayout = null; // this is set to the value of the primary payload that spawns this
	public String secondaryLayout = null; // this is set to the value of the secondary payload we generate when this is stored
	public int number; // this replaces what used to be the payload type and now matches the number on the MASTER file
	public String typeStr = ""; // set when this is loaded by the spacecraft.
	public String title; // the title to put on the tab
	public String shortTitle; // the title to name the tab
	public Color color = Color.BLUE;
	
	public static final String NONE = "NONE";
	
	public String[] fieldName = null;  // name of the field that the bits correspond to
	public String[] conversion = null; // the conversion routine to change raw bits into a real value
	public String[] fieldUnits = null; // the units as they would be displayed on a graph e.g. C for Celcius
	public int[] fieldBitLength = null; // the number of bits in this field
	public String[] module = null; // the module on the display screen that this would be shown in e.g. Radio
	public int[] moduleNum = null; // the order that the module is displayed on the screen with 1-9 in the top and 10-19 in the bottom
	public int[] moduleLinePosition = null; // the line position in the module, starting from 1
	public int[] moduleDisplayType = null; // a code determining if this is displayed across all columns or in the RT, MIN, MAX columns.  Defined in DisplayModule

	public String[] shortName = null;
	public String[] description = null;
	
	public boolean hasGPSTime = false;
	
	private int numberOfBits = 0;
	private int numberOfBytes = 0;
	
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
	public static final int CONVERT_HERCI_HEX = 28;
	public static final int CONVERT_HERCI_SOURCE = 29;
	public static final int CONVERT_HERCI_MICRO_PKT_TYP = 30;
	public static final int CONVERT_HERCI_MICRO_PKT_SOURCE = 31;
	public static final int CONVERT_HERCI_MICRO_PKT_HEX = 32;
	public static final int CONVERT_JAVA_DATE = 33;
	public static final int CONVERT_ICR_SW_COMMAND_COUNT = 34;
	public static final int CONVERT_ICR_DIAGNOSTIC = 35;
	public static final int CONVERT_WOD_STORED = 36;
	public static final int CONVERT_LT_TXRX_TEMP = 37;
	public static final int CONVERT_LT_PA_CURRENT = 38;
	public static final int CONVERT_SOFT_ERROR_84488 = 39;
	public static final int CONVERT_LT_TX_FWD_PWR = 40;
	public static final int CONVERT_LT_TX_REF_PWR = 41;
	public static final int CONVERT_LT_VGA = 42;
	public static final int CONVERT_ICR_VOLT_SENSOR = 43;
	public static final int CONVERT_STATUS_ENABLED = 44;
	public static final int CONVERT_COM1_ACCELEROMETER = 45;
	public static final int CONVERT_COM1_MAGNETOMETER = 46;
	public static final int CONVERT_COM1_SPIN = 47;
	public static final int CONVERT_COM1_GYRO_TEMP = 48;
	public static final int CONVERT_COM1_ISIS_ANT_TEMP = 49; // COM1
	public static final int CONVERT_COM1_ISIS_ANT_TIME = 50; // COM1
	public static final int CONVERT_COM1_ISIS_ANT_STATUS = 51; // COM1
	public static final int CONVERT_COM1_SOLAR_PANEL = 52; // COM1
	public static final int CONVERT_COM1_TX_FWD_PWR = 53; // COM1
	public static final int CONVERT_COM1_TX_REF_PWR = 54; // COM1
	public static final int CONVERT_HUSKY_UW_DIST_BOARD_STATUS = 55; // COM1
	public static final int CONVERT_COM1_RSSI = 56; // COM1
	public static final int CONVERT_COM1_ICR_2V5_SENSOR = 57; // COM1
	public static final int CONVERT_COM1_BUS_VOLTAGE = 58; // COM1
	public static final int CONVERT_ROOT_10 = 59; // COM1 PWR calc component
	public static final int CONVERT_MEMS_SCALAR_ROTATION = 60; // FOX-1A Scalar Rotation Calculation
	public static final int CONVERT_MEMS_X_ROTATION = 61; // FOX-1A Scalar Rotation Calculation
	public static final int CONVERT_MEMS_Y_ROTATION = 62; // FOX-1A Scalar Rotation Calculation
	public static final int CONVERT_MEMS_Z_ROTATION = 63; // FOX-1A Scalar Rotation Calculation
	
	public static final int MAX_CONVERSION_NUMBER = 63; // For integrity check
	
	/**
	 * Create an empty layout for manual init
	 * Note that if this is called, the BitArray is not initialized.  So it must also be setup manually
	 */
	public BitArrayLayout() {
		
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
	
	/**
	 * Calculate and return the total number of bits across all fields
	 * @return
	 */
	public int getMaxNumberOfBits() {
		return numberOfBits;
	}
	
	public int getMaxNumberOfBytes() {
		return numberOfBytes;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public boolean isRealTime() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.RT)) return true;
		return false;
	}

	public boolean isWOD() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.WOD)) return true;
		return false;
	}

	public boolean isMAX() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.MAX)) return true;
		return false;
	}

	public boolean isMIN() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.MIN)) return true;
		return false;
	}
	
	public boolean isExperiment() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.EXP)) return true;
		return false;
	}

	public boolean isWODExperiment() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.WOD_EXP)) return true;
		return false;
	}
	
	public boolean isCanExperiment() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.CAN_EXP)) return true;
		return false;
	}
	
	public boolean isCanWodExperiment() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.CAN_WOD_EXP)) return true;
		return false;
	}
	
	public boolean isCanPkt() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.CAN_PKT)) return true;
		return false;
	}
	
	public boolean isCanWodPkt() {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.WOD_CAN_PKT)) return true;
		return false;
	}

	public static boolean isValidType(String typeStr) {
		if (typeStr.equalsIgnoreCase(BitArrayLayout.RT)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.WOD)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.MAX)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.MIN)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.EXP)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.WOD_EXP)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.CAN_EXP)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.CAN_WOD_EXP)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.CAN_PKT)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.WOD_CAN_PKT)) return true;
		if (typeStr.equalsIgnoreCase(BitArrayLayout.DBG)) return true;
		return false;
	}
	
	
	public String getSecondaryPayloadName() {
		return secondaryLayout;
	}
	
	public boolean isSecondaryPayload() {
		if (parentLayout != null) return true;
		return false;
	}
	
	public boolean hasFieldName(String name) {
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				return true;
		}
		return false;
	}

	public String getConversionNameByPos(int pos) {
		return conversion[pos];
	}

	public String getConversionNameByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i])) {
				pos = i;
				break;
			}
		}
		if (pos == ERROR_POSITION) {
			return NONE;
		} else {
			return getConversionNameByPos(pos);
		}
	}

	public int getIntConversionByPos(int pos) {
		int c = 0;
		try {
			c = Integer.parseInt(conversion[pos]);
		} catch (NumberFormatException e) {
			return CONVERT_NONE; // no conversion
		}
		return (c);
	}

	public int getIntConversionByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return CONVERT_NONE;
		} else {
			return getIntConversionByPos(pos);
		}
	}

	public String getUnitsByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return "";
		} else {
			return (fieldUnits[pos]);
		}
	}
	
	public int getPositionByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return ERROR_POSITION;
		} else {
			return pos;
		}
	}
	
	public String getShortNameByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return "";
		} else {
			return (shortName[pos]);
		}
	}

	public String getModuleByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return "";
		} else {
			return (module[pos]);
		}
	}
	
	public String[][] getJTableData() {
		
		String[][] data = new String[fieldName.length][12];
		for (int i=0; i < fieldName.length; i++) {
			data[i][0] = "" + i;
			data[i][1] =  typeStr;
			data[i][2] = fieldName[i];
			data[i][3] = ""+fieldBitLength[i];
			data[i][4] = fieldUnits[i];
			data[i][5] = conversion[i];
			data[i][6] = module[i];
			data[i][7] = ""+moduleNum[i];
			data[i][8] = ""+moduleLinePosition[i];
			data[i][9] = ""+moduleDisplayType[i];
			data[i][10] = shortName[i];
			data[i][11] = description[i];
			
		}
		return data;
	}
	
//	/**
//	 * This should never be called by the decoder. The files are read only.  This 
//	 * is available for the spacecraft editor
//	 * 
//	 * @param f
//	 * @throws IOException 
//	 */
//	public void save(String f) throws IOException {
//		String line;
//		fileName = "spacecraft" +File.separator + f;
//		BufferedWriter dis = new BufferedWriter(new FileWriter(fileName));
//		line = fieldName.length + ",TYPE,FIELD,BITS,UNIT,CONVERSION,MODULE,MODULE_NUM,MODULE_LINE,LINE_TYPE,SHORT_NAME,DESCRIPTION";
//		
//		dis.write(line);
//		
//		dis.close();
//	}

	protected void load(String fileName) throws FileNotFoundException, LayoutLoadException {

		String line;
	//	File aFile = new File(fileName);
		
		Log.println("Loading layout: "+ fileName);
		@SuppressWarnings("resource")
		BufferedReader dis = new BufferedReader(new FileReader(fileName));
		int field=0;
		int column=0; // this is just for debugging error messages
		try {
			line = dis.readLine(); // read the header, and only look at first item, the number of fields.
			StringTokenizer header = new StringTokenizer(line, ",");
			NUMBER_OF_FIELDS = Integer.valueOf(header.nextToken()).intValue();			
			fieldName = new String[NUMBER_OF_FIELDS];		
			conversion = new String[NUMBER_OF_FIELDS];
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
					int fieldId = Integer.valueOf(st.nextToken()).intValue(); column++;
					@SuppressWarnings("unused")
					String type = st.nextToken(); column++;
					String tmpName = st.nextToken(); column++;
					if (hasFieldName(tmpName)) {
						throw new LayoutLoadException("Error loading layout " + fileName +
								"\n Duplicate field: " + tmpName);
					} else {
						fieldName[field] = tmpName;
					}
					fieldBitLength[field] = Integer.valueOf(st.nextToken()).intValue(); column++;
					fieldUnits[field] = st.nextToken(); column++;
					conversion[field] = st.nextToken(); column++; //Integer.valueOf(st.nextToken()).intValue();
					module[field] = st.nextToken(); column++;					
					moduleNum[field] = Integer.valueOf(st.nextToken()).intValue(); column++;
					moduleLinePosition[field] = Integer.valueOf(st.nextToken()).intValue(); column++;
					moduleDisplayType[field] = Integer.valueOf(st.nextToken()).intValue(); column++;
					shortName[field] = st.nextToken(); column++;
					description[field] = st.nextToken(); column++;
					field++;
					column = 0;
				}
			}
			dis.close();
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());

		} catch (NumberFormatException n) {
			Log.errorDialog("NUMBER FORMAT EXCEPTION", "In layout: " + fileName+"\n" + " in row for field: " + fieldName[field] + " on row: " + field +" at col: " + column +"\n" + n.getMessage());
			n.printStackTrace(Log.getWriter());
		} catch (IndexOutOfBoundsException n) {
			Log.errorDialog("INDEX EXCEPTION", "Error loading Layout "+fileName+"\n" + " on row: " + field +" at col: " + column +"\n" + " Index is out of bounds: " + n.getMessage());
			n.printStackTrace(Log.getWriter());
		} catch (NoSuchElementException n) {
			Log.errorDialog("Missing Field in Layout File", "Halted loading of: " + fileName + "\n on row: " + field +" at col: " + column +"\n");
			n.printStackTrace(Log.getWriter());
		}
		if (NUMBER_OF_FIELDS != field) throw new LayoutLoadException("Error loading fields from " + fileName +
				". Expected " + NUMBER_OF_FIELDS + " fields , but loaded " + field);
		if (fieldBitLength != null) {
			numberOfBits = 0;
			for (int i=0; i < fieldBitLength.length; i++) {
				numberOfBits += fieldBitLength[i];
			}
			numberOfBytes = (int)(Math.ceil(numberOfBits / 8.0));
		}
	}
	
	public String getTableCreateStmt(boolean storeMode) {
		String s = new String();
		s = s + "(captureDate varchar(14), id int, resets int, uptime bigint, type int, ";
		if (storeMode)
			s = s + "newMode int,";
		for (int i=0; i < fieldName.length; i++) {
			s = s + fieldName[i] + " int,\n";
		}
		// We use serial for the type, except for type 4 where we use it for the payload number.  This allows us to have
		// multiple high speed records with the same reset and uptime
		s = s + "PRIMARY KEY (id, resets, uptime, type))";
		return s;
	}

	public String toString() {
		String s = name;
		return s;
	}
}
