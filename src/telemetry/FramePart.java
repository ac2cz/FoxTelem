package telemetry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import common.Log;
import common.Spacecraft;
import decoder.BitStream;
import decoder.Decoder;
import gui.GraphPanel;

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
 * A telemetry frame is made up of several parts, e.g. Header, Payload, Trailer
 * This is the parent class and holds common methods and data shared by all
 * 
 * Many of the values are shared across the Max, Min and Real Time FrameParts, so this class
 * contains all of the conversion routines to translate from raw values to final values
 * 
 * This also contains the methods to read and write the payloads to disk
 * 
 *
 */
public abstract class FramePart extends BitArray implements Comparable<FramePart> {
	public static final int TYPE_DEBUG = 0;
	public static final int TYPE_REAL_TIME = 1;
	public static final int TYPE_MAX_VALUES = 2;
	public static final int TYPE_MIN_VALUES = 3;
	public static final int TYPE_CAMERA_DATA = 5;
	public static final int TYPE_RAD_EXP_DATA = 4; // This is both Vulcan and HERCI
	public static final int TYPE_HERCI_HIGH_SPEED_DATA = 6;
	public static final int TYPE_RAD_TELEM_DATA = 7; 
	public static final int TYPE_HERCI_SCIENCE_HEADER = 8; // This is the header from the high speed data once decoded
	public static final int TYPE_HERCI_HS_PACKET = 9; // This is the header from the high speed data once decoded
	public static final int TYPE_SLOW_SPEED_HEADER = 98;
	public static final int TYPE_SLOW_SPEED_TRAILER = 99;
	public static final int TYPE_HIGH_SPEED_HEADER = 100;
	public static final int TYPE_HIGH_SPEED_TRAILER = 101;
	// NOTE THAT TYPE 400+ are reserverd for the High Speed Radiation Payloads, where type is part of the uniqueness check
	// Correspondingly TYPE 600+ are reserved for Herci HS payloads
	// Correspondingly TYPE 800+ are reserved for Herci Telemetry payloads
	// Correspondingly TYPE 900+ are reserved for Herci Packets payloads
	// Correspondingly TYPE 700+ are reserved for Rad Telemetry payloads
	
	public static final DateFormat reportDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
	//protected BitArrayLayout.CONVERT_] conversion = null;

	// These fields are updated when the Frame Part is stored in the PayloadStore
	protected int id; // The id copied from the header of the highspeed or slow speed frame that this was captured in
	protected long uptime;  // The Uptime captured from the header
	protected int resets;  // The resets captured from the header
	protected String captureDate; // the date/time that this was captured
	protected int type; // the type of this payload
	
	/**public static enum BitArrayLayout.CONVERT_{ 
		NONE,
		INTEGER,
		V25_SENSOR,
		V3_SENSOR,
		BATTERY,
		SOLAR_PANEL,
		SOLAR_PANEL_TEMP,
		TEMP,
		BATTERY_TEMP,
		BATTERY_CURRENT,
		PA_CURRENT,
		PSU_CURRENT,
		SPIN,
		MEMS_ROTATION,
		RSSI,
		IHU_TEMP,
		ANTENNA,
		STATUS_BIT,
		IHU_DIAGNOSTIC,
		HARD_ERROR,
		SOFT_ERROR
	}
	*/
	
	// Flattened C ENUM for IHUDiagnostic Error Type
	public static final int SPININFO_1 = 0;
	public static final int SPININFO_2 = 1;
	public static final int SPININFO_3 = 2;
	public static final int SPININFO_4 = 3;
	public static final int LAST3_DL_STATES = 4;
	public static final int COMMAND_RING = 5;
	public static final int COMMAND_COUNT = 6;
	public static final int I2C1_ERRORS = 7;
	public static final int I2C2_ERRORS = 8;
	public static final int GYRO1Z = 11;
	public static final int GYRO1V = 12;
	public static final int GYRO2V = 13;
	
	// Flattened C ENUM for IHU Errors
	public static final String[] ihuErrorType = {
		"Unknown",
		"PowerCycle",
		"StackOverflow",
		"NMIExc",
		"HardFault",
		"MemManage",
		"BusFault",
		"UseFault",
		"USBHighPrio",
		"SPIInUse",
		"SPIOperationTimeout", // This is 10
		"SPIMramTimeout",
		"UnexpectedBehavior",
		"SemaphoreFail",
		"USARTError",
		"DMAInUseTimeout",
		"IllegalGPIOOutput",
		"IllegalGPIOInput",
		"IllegalGPIOWait",
		"MRAMcrc ",
		"MRAMread",// This is 20
		"MRAMwrite",
		"RTOSfailure",
		"ADCTimeout",
		"ADCDACSync",
		"I2C1failure",
		"I2C2failure",
		"ControlQueueOverflow", /* This is #27 */
		"ControlTimerNotStarted",
		"FlashCRCfaulty",
		"ExperimentFailure"
	};

	public static final String[] ihuTask = {
		"Unknown",
		 "Audio", // = 1
		 "Telemetry", // 2,
		 "Control", // 3
		 "Command", // 4,
		 "Idle", // 5,  
		 "Experiment" // 6,
	};
	
	public static final String[] ihuDownlinkStateMachineTask = {
	"NoChange",		// No state change
	/* These are actual states */
	"Relay",		// In relay or transponder mode
	"TlmFnsh",  	// Waiting for telemetry to finish after relay mode
	"Idle",
	"IdleBcon", 	// Carrier on but silence at the start then 1 frame tlm
	"IdleMsg",   	// Beacon voice message and one more frame of telemetry being sent
	"IdleWaitTlm", 	// Beacon voice is done, but waiting for telem to finish.
	"IdleWaitID", // Beacon telemetry is done, but waiting for voice ID to finish
	"IdleCar2",		// Silent carrier on at the end of the beacon
	"Safe",		// Safe mode, no carrier
	"SafeBcon",		// Send safe mode carrier, and telemetry
	"SafeMsg",   	// Send safe mode Voice ID
	"SafeWaitTlm", 	// Beacon voice is done, but waiting for telem to finish.
	"SafeWaitID",// Beacon telemetry is done, but waiting for voice to finish
	"SafeCar2",		// Silent carrier on at the end of the beacon
	"DataMode",
	"Unexpctd"
	};
	
	
	// MODEL SPECIFIC CALIBRATIONS are stored in common/Spacecraft.java
	//private static final double BATTERY_CURRENT_ZERO = -1.839;
	//public static LookUpTableRSSI rssiTable = new LookUpTableRSSI();
	//public static LookUpTableIHUTemp ihuTable = new LookUpTableIHUTemp();
	
	// Constants for conversions
	private static final double BATTERY_CURRENT_MIN = 0.05;  // The minimum voltage that the current sensor can measure across the sense resistor
	private static final double VOLTAGE_STEP_FOR_2V5_SENSORS = 2.5/4096; //0.0006103515625; // Multiply DAC by this to get value in Volts
	private static final double VOLTAGE_STEP_FOR_3V_SENSORS = 3.0/4096; //0.000732421875; // Multiply DAC by this to get value in Volts
	private static final double BATTERY_B_SCALING_FACTOR = 0.76; // Multiply the battery B reading by the 2.5V Sensor step and then divide by this factor
	private static final double BATTERY_C_SCALING_FACTOR = 0.5; // Multiply the battery C reading by the 2.5V Sensor step and then divide by this factor
	private static final double SOLAR_PANEL_SCALING_FACTOR = 0.428; // Multiply the solar panel reading by the 3V Sensor step and then divide by this factor
	private static final double MPPT_SOLAR_PANEL_SCALING_FACTOR = 6.54/2.42; // per Burns, then multiply.  Note that Bryce gave: 0.37069; // 30.1/(30.1+51.1).  Multiply the solar panel reading by the 2V5 Sensor step and then divide by this factor
	private static final double PSU_CURRENT_SCALING_FACTOR = 0.003; // Multiply the PSU current reading by the 3V Sensor step and then divide by this factor
	private static final double MPPT_CURRENT_SCALING_FACTOR = 2.5; //100.0/0.025; // Multiply the MPPT current reading by the 2V5 Sensor step and then divide by this factor
	private static final double MPPT_RTD_CONSTANT_CURERNT = 0.001; // Constant current driver for the MPPT RTD is 1mA
	private static final double MPPT_RTD_AMP_GAIN = -8.14228; // RTD conditioning amplifier Vout = -8.14228 * Vin +2.0523 
	private static final double MPPT_RTD_AMP_FACTOR = 2.0523; // 
	private static final double PA_CURRENT_INA194_FACTOR = 50; // Multiply the PSU current reading by the 3V Sensor step and then divide by this factor and the shunt value
	private static final double PA_CURRENT_SHUNT_RESISTOR_FACTOR = 0.2; // Multiply the PSU current reading by the 3V Sensor step and then divide by the IN914 factor and this factor
	private static final double MEMS_ZERO_VALUE_VOLTS = 1.51; // Updated from datasheet value of 1.51 following observation of Vref in the diagnostics
	private static final double MEMS_VOLT_PER_DPS = 0.0333; // This value is from the data sheet.  Jerry to provide a value for FM
	
	/*
calibration of the MEMS gyros - at rest right now, X is 2072, Y
is 2085, Z is 2036.  The flight stack is laying on its side though, so
maybe we need to get those numbers again when it's in the cube and
sitting "upright"?  At least, something to capture before it can no
longer send telemetry.
	 */
	
	// Look up tables common to all Models
	public static LookUpTableTemperature temperatureTable = new LookUpTableTemperature();
	public static LookUpTableBatteryTemp batteryTempTable = new LookUpTableBatteryTemp();
	public static LookUpTableSolarPanelTemp solarPanelTempTable = new LookUpTableSolarPanelTemp();
	
	protected int MAX_BYTES = 60;  // This provides enough storage to cover the zero filled bytes at the end of the Slow or High Speed frames
	
	public FramePart(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(lay);
		this.id = id;
		this.resets = resets;
		this.uptime = uptime;
		this.captureDate = date;
		init();
		rawBits = null; // no binary array when loaded from file, even if the local init creates one
		load(st);
	}
	
	public FramePart(BitArrayLayout lay) {
		super(lay);
		init();
		rawBits = new boolean[MAX_BYTES*8];
	}
	
	/**
	 * Create a new payload based on the result set from the db
	 * @param results
	 * @throws SQLException 
	 */
	public FramePart(ResultSet results, BitArrayLayout lay) throws SQLException {
		super(lay);
		this.id = results.getInt("id");
		this.resets = results.getInt("resets");
		this.uptime = results.getLong("uptime");
		this.captureDate = results.getString("captureDate");
		init();
		rawBits = null; // no binary array when loaded from database
		for (int i=0; i < fieldValue.length; i++) {
			fieldValue[i] = results.getInt(layout.fieldName[i]);
		}
//		results.close();
	}
	abstract protected void init();
		
	public void captureHeaderInfo(int id, long uptime, int resets) {
		this.id = id;
		this.uptime = uptime;
		this.resets = resets;
		this.captureDate = fileDateStamp();
	}
	
	public int getFoxId() { return id; }
	public long getUptime() { return uptime; }
	public int getResets() { return resets; }
	public int getType() { return type; }
	public String getCaptureDate() { return captureDate; }
	
	
	public int compareTo(FramePart p) {
		if (resets == p.resets && uptime == p.uptime && type == p.type) 
			return 0;
		else if (resets < p.resets)
			return -1;
		else if (resets > p.resets)
			return +1;
		else if (resets == p.resets && uptime == p.uptime) {
			if (type < p.type)
				return -1;
			if (type > p.type)
				return +1;
		} else if (resets == p.resets) {	
			if (uptime < p.uptime)
				return -1;
			if (uptime > p.uptime)
				return +1;
		} 
		return +1;
	}
	
	

	public abstract boolean isValid();
	
	public int getMaxBytes() { return MAX_BYTES; }
	public int getMaxBits() { return rawBits.length; }
	
	public boolean isValidType(int t) {
		if (t == TYPE_DEBUG) return true;
		if (t == TYPE_REAL_TIME) return true;
		if (t == TYPE_MAX_VALUES) return true;
		if (t == TYPE_MIN_VALUES) return true;
		if (t == TYPE_RAD_EXP_DATA) return true;
		return false;
			
	}
	
	public String reportDate() {
		
		// Get the date using Calendar object.
		// java.util.Date has no timezone, but the forat will give it the 
		// local timezone unless we tell it otherwise
		Date today = Calendar.getInstance().getTime();  
		reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.
		String reportDate = reportDateFormat.format(today);
		return reportDate;
	}
	
	public static String fileDateStamp() {
		
		Date today = Calendar.getInstance().getTime();  
		fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = fileDateFormat.format(today);
		return reportDate;
	}


	/**
	 * Get the string represetnation of a field in this framePart.  Run any conversion
	 * routine assigned to this field
	 * @param name
	 * @return
	 */
	public String getStringValue(String name, Spacecraft fox) {
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				pos = i;
		}
		String s = "-----";
		// Special Formatting
		if (pos == -1) 
			;//System.err.println("ERROR: No Index for Field:" + name);
		else
		if (layout.conversion[pos] == BitArrayLayout.CONVERT_ANTENNA) {
			int value = getRawValue(name);
			if (value == 0)
				s = "Stowed";
			else
				s = "Deployed";
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_STATUS_BIT) {
			int value = getRawValue(name);
			if (value == 0)
				s = "OK";
			else
				s = "FAIL";
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_BOOLEAN) {
			int value = getRawValue(name);
			if (value == 1)
				s = "TRUE";
			else
				s = "FALSE";
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_INTEGER) {
			s = Long.toString(Math.round(getRawValue(name)));
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC) {
			s = ihuDiagnosticString(getRawValue(name), true);
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_HARD_ERROR) {
			s = hardErrorString(getRawValue(name), true);
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_SOFT_ERROR) {
			s = softErrorString(getRawValue(name), true);
		} else {
			double dvalue = getDoubleValue(name, fox);
			if (dvalue == ERROR_VALUE) {
				s = "-----";
			} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_BATTERY) {
				s = String.format("%1.2f", dvalue);
			} else {
				s = String.format("%2.1f", dvalue);
			}
			
		}
		if (s.length() < 5)
			for (int k=0; k < (5 - s.length()); k++)
				s = " " + s;
		return s;
	}
		
	
	
	
	/**
	 * Given a raw value, BitArrayLayout.CONVERT_it into the actual value that we can display based on the
	 * conversion type passed.  Field name is also used in some conversions, e.g. the batteries
	 * @param name
	 * @param rawValue
	 * @param conversion
	 * @return
	 */
	public double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox ) {
		
	//	System.out.println("BitArrayLayout.CONVERT_ng: " + name + " raw: " + rawValue + " CONV: " + conversion);
		switch (conversion) {
		case BitArrayLayout.CONVERT_ANTENNA:
			return rawValue;
		case BitArrayLayout.CONVERT_BOOLEAN:
			return rawValue;
		case BitArrayLayout.CONVERT_NONE:
			return rawValue;
		case BitArrayLayout.CONVERT_INTEGER:
			return rawValue;
		case BitArrayLayout.CONVERT_V25_SENSOR:
			return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS;
			
		case BitArrayLayout.CONVERT_V3_SENSOR:
			return rawValue * VOLTAGE_STEP_FOR_3V_SENSORS;
			
		case BitArrayLayout.CONVERT_BATTERY:
			if (name.equalsIgnoreCase("BATT_A_V"))
					return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS;
			if (name.equalsIgnoreCase("BATT_B_V"))
				return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/BATTERY_B_SCALING_FACTOR;
			if (name.equalsIgnoreCase("BATT_C_V"))
				if (fox.useIHUVBatt)
					return fox.ihuVBattTable.lookupValue(rawValue);
				else
					return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/BATTERY_C_SCALING_FACTOR;
			return ERROR_VALUE;

		case BitArrayLayout.CONVERT_BATTERY_TEMP:
			return batteryTempTable.lookupValue(rawValue);
			
		case BitArrayLayout.CONVERT_BATTERY_CURRENT:
			double d = (double)rawValue;
			d = (( d * VOLTAGE_STEP_FOR_2V5_SENSORS - BATTERY_CURRENT_MIN) * fox.BATTERY_CURRENT_ZERO + 2)*1000;
			return d;
		case BitArrayLayout.CONVERT_SOLAR_PANEL:
			return rawValue * VOLTAGE_STEP_FOR_3V_SENSORS/SOLAR_PANEL_SCALING_FACTOR;
		case BitArrayLayout.CONVERT_MPPT_SOLAR_PANEL:
			return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS * MPPT_SOLAR_PANEL_SCALING_FACTOR;
		case BitArrayLayout.CONVERT_SOLAR_PANEL_TEMP:
			return solarPanelTempTable.lookupValue(rawValue) ;
		case BitArrayLayout.CONVERT_MPPT_SOLAR_PANEL_TEMP:
			double v = (double)rawValue;
			v = v * VOLTAGE_STEP_FOR_2V5_SENSORS;
			v =  (v - MPPT_RTD_AMP_FACTOR) / (MPPT_RTD_AMP_GAIN);
			double r = v / this.MPPT_RTD_CONSTANT_CURERNT;
			
			// Cubic fir using equation from http://www.mosaic-industries.com/embedded-systems/microcontroller-projects/temperature-measurement/platinum-rtd-sensors/resistance-calibration-table
			double t = -247.29+2.3992*r+0.00063962*Math.pow(r,2)+(0.0000010241)*Math.pow(r,3);
			
			return t;
		case BitArrayLayout.CONVERT_TEMP:
			return temperatureTable.lookupValue(rawValue);
		case BitArrayLayout.CONVERT_PA_CURRENT:
			double paVolts = rawValue * VOLTAGE_STEP_FOR_3V_SENSORS;
			double paCurrent = paVolts / PA_CURRENT_INA194_FACTOR/PA_CURRENT_SHUNT_RESISTOR_FACTOR;
			return paCurrent * 1000;
		case BitArrayLayout.CONVERT_PSU_CURRENT:
			return rawValue * VOLTAGE_STEP_FOR_3V_SENSORS/PSU_CURRENT_SCALING_FACTOR;
		case BitArrayLayout.CONVERT_MPPT_CURRENT:
			return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/MPPT_CURRENT_SCALING_FACTOR *1000;
		case BitArrayLayout.CONVERT_SPIN:
			// SPIN is 3.8 fraction fixed point. 
			// If raw value > 2^11 (2048) - 1 then value = value - 2^12 (4096)
			// This gives a signed value which we then divide by 256 to get a signed double
			double value = rawValue;
			if (value > (2048-1)) value = -4096 + value;
			value = value / 256.0d;
			return value ;
		case BitArrayLayout.CONVERT_MEMS_ROTATION:
			return (rawValue * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS;
		case BitArrayLayout.CONVERT_RSSI:
			return fox.rssiTable.lookupValue(rawValue);
		case BitArrayLayout.CONVERT_IHU_TEMP:
			return fox.ihuTable.lookupValue(rawValue);
		case BitArrayLayout.CONVERT_STATUS_BIT:
			return rawValue;
		case BitArrayLayout.CONVERT_IHU_DIAGNOSTIC:
			return rawValue;
		case BitArrayLayout.CONVERT_HARD_ERROR:
			return rawValue;
		case BitArrayLayout.CONVERT_SOFT_ERROR:
			return rawValue;
		}
		
		return ERROR_VALUE;
	}
	
	/**
	 * Decode the IHU diagnostic and return a string representation.  shortString is
	 * set to true for display in a small space on the GUI.  When false a longer verbose
	 * explanation is returned
	 * @param rawValue
	 * @param shortString
	 * @return
	 */
	public static String ihuDiagnosticString(int rawValue, boolean shortString) {
		// First 8 bits hold the type
		int type = rawValue & 0xff ;
		int value = 0;
		String s = new String();
		
		switch (type) {
		
		case SPININFO_1: // spinInfo1
			value = rawValue >> 8;
			s = "spin1: " + value;
			return s;
		case SPININFO_2: // spinInfo2
			value = rawValue >> 8;
			s = "spin2: " + value;
			return s;
		case SPININFO_3: // spinInfo3
			value = rawValue >> 8;
			s = "spin3: " + value;
			return s;
		case SPININFO_4: // spinInfo4
			value = rawValue >> 8;
			s = "spin4: " + value;
			return s;
		case LAST3_DL_STATES: // Last3 states of DownlinkControl State Machine
			int a = (rawValue >> 8) & 0xff;
			int b = (rawValue >> 16) & 0xff;
			int c = (rawValue >> 24) & 0xff;
			if (shortString)
				return "Last3DL: "+ Integer.toHexString(a) + " "+ Integer.toHexString(b) + " "+ Integer.toHexString(c);
			else
				return "Last 3 states of the DownlinkControl state machine: "+ ihuDownlinkStateMachineTask[a] + " "+ ihuDownlinkStateMachineTask[b] + " "+ ihuDownlinkStateMachineTask[c];
		case COMMAND_RING: // CommandRing of last 5 uplink commands
			int n1 = (rawValue >> 8) & 0x0f;  // first nibble after the 8 type bits
			int n2 = (rawValue >> 12) & 0x0f; 
			int n3 = (rawValue >> 16) & 0x0f;
			int n4 = (rawValue >> 20) & 0x0f;
			int n5 = (rawValue >> 24) & 0x0f;
			int n6 = (rawValue >> 28) & 0x0f;
			if (shortString)
				return "Commands: " + Integer.toHexString(n1) + " "+ Integer.toHexString(n2) + " "+ Integer.toHexString(n3) + " "
				+ Integer.toHexString(n4) + " "+ Integer.toHexString(n5) + " "+ Integer.toHexString(n6);
			else
				return "Command Ring of last 5 uplinked commands: " + Integer.toHexString(n1) + " "+ Integer.toHexString(n2) + " "+ Integer.toHexString(n3) + " "
				+ Integer.toHexString(n4) + " "+ Integer.toHexString(n5) + " "+ Integer.toHexString(n6);
		case COMMAND_COUNT: // CommandCount - number of commands received since boot
			value = (rawValue >> 8) & 0xffffff; // 24 bit value after the type
			if (shortString)
				return "Count: " + value;
			else
				return "Number of commands received since boot: " + value;
		case I2C1_ERRORS: // I2C1Errors
			int writeTimeout = (rawValue >> 8) & 0xff;
			int busBusyTimeout = (rawValue >> 16) & 0xff;
			int readTimeout = (rawValue >> 24) & 0xff;
			if (shortString)
				return "I2C1: W " + writeTimeout + " B "+ busBusyTimeout + " R " + readTimeout;
			else
				return "I2C1 Errors: Write Timeouts " + writeTimeout + " Busy Timeouts "+ busBusyTimeout + " Read Timeouts " + readTimeout;
		case I2C2_ERRORS: // I2C2Errors
			int writeTimeout2 = (rawValue >> 8) & 0xff;
			int busBusyTimeout2 = (rawValue >> 16) & 0xff;
			int readTimeout2 = (rawValue >> 24) & 0xff;
			if (shortString)
				return "I2C2: W " + writeTimeout2 + " B "+ busBusyTimeout2 + " R " + readTimeout2;
			else
				return "I2C2 Errors: Write Timeouts " + writeTimeout2 + " Busy Timeouts "+ busBusyTimeout2 + " Read Timeouts " + readTimeout2;
		case 9: // unused
			return "XXXX";
		case 10: // unused
			return "XXXX";
		case GYRO1Z: // Gyro1Z
			value = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			if (shortString)
				//return "Gyro1Z: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
				return "Gyro1Z (dps): " + GraphPanel.roundToSignificantFigures((value * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS,3);
			else
				return "Gyro1Z (dps): " + GraphPanel.roundToSignificantFigures((value * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS,3);
			//return "Gyro1 Z Value: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
		case GYRO1V: // Gyro1V
			value = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			int cameraChecksumErrors = (rawValue >> 20) & 0xff; // last 8 bits
			if (shortString)
				return "Gyro1V (dps): " + GraphPanel.roundToSignificantFigures((value * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS,3);
				//return "Gyro1V: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
			else
				return "Gyro1V (dps): " + GraphPanel.roundToSignificantFigures((value * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS,3) + " Camera Checksum Errors: " + cameraChecksumErrors;
				//return "Gyro1 Vref: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS + " Camera Checksum Errors: " + cameraChecksumErrors;
		case GYRO2V: // Gyro2V
			value = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			int hsAudioBufferUnderflows = (rawValue >> 20) & 0xff; // last 8 bits
			if (shortString)
				return "Gyro2V (dps): " + GraphPanel.roundToSignificantFigures((value * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS,3);
				//return "Gyro2V: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
			else
				return "Gyro2V (dps): " + GraphPanel.roundToSignificantFigures((value * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS,3) + " HS Audio Buffer Underflows: " + hsAudioBufferUnderflows;
				//return "Gyro2 Vref: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS + " HS Audio Buffer Underflows: " + hsAudioBufferUnderflows;
		}
		return "-----";
	}

	/**
	 * Decode the IHU Hard Error bits
	 * @param rawValue
	 * @return
	 */
	public static String hardErrorString(int rawValue, boolean shortString) {
		String s = new String();
		if (rawValue != ERROR_VALUE) {
		// First 9 bits are the watch dog 
		int watchDogReports = rawValue & 0x1ff ;
		
		// Error code is the next 5 bits
		int errorCode = (rawValue >> 9) & 0x1f;

		// mramErrorCount is the next 3 bits
		int mramErrorCount = (rawValue >> 14) & 0x07;

		// nonFatalErrorCount is the next 3 bits
		int nonFatalErrorCount = (rawValue >> 17) & 0x07;

		// taskNumber is the next 4 bits
		int taskNumber = (rawValue >> 20) & 0x0f;

		// alignment is the next 8 bits
		int alignment = (rawValue >> 24) & 0xff;

		if (shortString)
			s = s + "wd " + Integer.toHexString(watchDogReports) + " "
				+ "ec " + Integer.toHexString(errorCode) + " "
				+ "mr " + Integer.toHexString(mramErrorCount) + " "
				+ "nf " + Integer.toHexString(nonFatalErrorCount) + " "
				+ "tn " + Integer.toHexString(taskNumber) + " "
				+ "al " + Integer.toHexString(alignment);
		else
			s = s + "Watchdog Reports: " + BitStream.stringBitArray(BitStream.intToBin9(watchDogReports))  + " "//  Integer.toHexString(watchDogReports) + " "
					+ " Error Type: " + ihuErrorType[errorCode] + " "
					+ " MRAM Error Count: " + Integer.toHexString(mramErrorCount) + " "
					+ " Non Fatal Error Count: " + Integer.toHexString(nonFatalErrorCount) + " "
					+ " Task Number: " + taskNumber + "- ";
		if (taskNumber < ihuTask.length)
			s = s + ihuTask[taskNumber];
		else
			s = s + "Unknown";
		s = s + " Alignment: " + alignment;
		}
		return s;
	}

	public static final int wdReportAudioTask = 0x01;
	public static final int wdReportTelemetryCollectionTask = 0x02;
	public static final int wdReportDownlinkControlTask = 0x04;
	public static final int wdReportUplinkCommandRxTask = 0x08;
	public static final int wdReportIdleTask = 0x10;
	public static final int wdReportExpTask = 0x20;
	
	/**
	 * Decode the IHU Hard Error bits
	 * @param rawValue
	 * @return
	 */
	public static String[] hardErrorStringArray(int rawValue, boolean shortString) {
		String[] s = new String[6];
		if (rawValue != ERROR_VALUE) {
		// First 9 bits are the watch dog 
		int watchDogReports = rawValue & 0x1ff ;
		
		// Error code is the next 5 bits
		int errorCode = (rawValue >> 9) & 0x1f;

		// mramErrorCount is the next 3 bits
		int mramErrorCount = (rawValue >> 14) & 0x07;

		// nonFatalErrorCount is the next 3 bits
		int nonFatalErrorCount = (rawValue >> 17) & 0x07;

		// taskNumber is the next 4 bits
		int taskNumber = (rawValue >> 20) & 0x0f;

		// alignment is the next 8 bits
		int alignment = (rawValue >> 24) & 0xff;

		s[0] = errorCode + " - " + ihuErrorType[errorCode];
		s[1] = Integer.toHexString(alignment);
		
		s[2] =  BitStream.stringBitArray(BitStream.intToBin9(watchDogReports));
		s[3] = Integer.toHexString(taskNumber) + " - ";
		if (taskNumber < ihuTask.length)
			s[3] = s[3] + ihuTask[taskNumber];
		else
			s[3] = s[3] + "Unknown";
		s[4] = Integer.toHexString(mramErrorCount);
		s[5] = Integer.toHexString(nonFatalErrorCount);
		
		
		}
		return s;
	}

	
	/**
	 * Decode the IHU Soft error bits
	 * @param rawValue
	 * @return
	 */
	public static String softErrorString(int rawValue, boolean shortString) {
		// Soft error is 4 8 bit numbers 
		String s = new String();
		if (rawValue != ERROR_VALUE) {
			int DACoverflows = rawValue & 0xff;
			int I2CRetries = (rawValue >> 8) & 0xff;
			int SPIRetries = (rawValue >> 16) & 0xff;
			int MramCRCs = (rawValue >> 20) & 0xff;

			if (shortString)
				s = s + "dac " + DACoverflows + " i2c " + I2CRetries + " spi " + SPIRetries + " mr " + MramCRCs;
			else
				s = s + "DAC Overflows: " + DACoverflows + "  I2C Retries: " + I2CRetries + "  SPI Retries: " + SPIRetries + "  MRAM CRCs: " + MramCRCs;
		}
		return s;
	}

	public static String[] softErrorStringArray(int rawValue, boolean shortString) {
		// Soft error is 4 8 bit numbers 
		String[] s = new String[4];
		if (rawValue != ERROR_VALUE) {
			int DACoverflows = rawValue & 0xff;
			int I2CRetries = (rawValue >> 8) & 0xff;
			int SPIRetries = (rawValue >> 16) & 0xff;
			int MramCRCs = (rawValue >> 20) & 0xff;

			s[0] = Integer.toString(DACoverflows); 
			s[1] = Integer.toString(I2CRetries);
			s[2] = Integer.toString(SPIRetries);
			s[3] = Integer.toString(MramCRCs);
		}
		return s;
	}
	
	public abstract String toString();

	/**
	 * Load this framePart from a file, which has been opened by a calling method.  The string tokenizer contains a 
	 * set of tokens that represent the raw values to be loaded into the fields.
	 * The header has already been loaded by the calling routine.
	 * @param st
	 */
	protected void load(StringTokenizer st) {
		int i = 0;
		String s = null;
		try {
			while((s = st.nextToken()) != null) {
				if (s.startsWith("0x")) {
					s = s.replace("0x", "");
					fieldValue[i++] = Integer.valueOf(s,16);
				} else
					fieldValue[i++] = Integer.valueOf(s).intValue();
			}
		} catch (NoSuchElementException e) {
			// we are done and can finish
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.println("ERROR: Too many fields:  Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
		}
	}
	
	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		s = s + captureDate + "," + id + "," + resets + "," + uptime + "," + type + ",";
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + Decoder.dec(getRawValue(layout.fieldName[i])) + ",";
		}
		s = s + Decoder.dec(getRawValue(layout.fieldName[layout.fieldName.length-1]));
		return s;
	}
	
	public String getInsertStmt() {
		copyBitsToFields();
		String s = new String();
		s = s + " (captureDate,  id, resets, uptime, type, \n";
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + layout.fieldName[i] + ",\n";
		}
		s = s + layout.fieldName[layout.fieldName.length-1] + ")\n";
		s = s + "values ('" + this.captureDate + "', " + this.id + ", " + this.resets + ", " + this.uptime + ", " + this.type + ",\n";
		for (int i=0; i < fieldValue.length-1; i++) {
			s = s + fieldValue[i] + ",\n";
		}
		s = s + fieldValue[fieldValue.length-1] + ")\n";
		return s;
	}
}
