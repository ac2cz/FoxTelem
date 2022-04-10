package telemetry.conversion;

import common.Spacecraft;
import decoder.FoxBitStream;
import gui.graph.LinePlotPanel;
import telemetry.BitArrayLayout;
import telemetry.FramePart;

public class ConversionLegacy extends Conversion {
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
	public static final int UNKNOWN = 15;
	public static final int IHU_SW_VERSION = 14;
	//public static final int BUS_VOLTAGE_OVER_2 = 15;
	public static final int ISISStatus = 16;
	public static final int IHU_TEMP_CALIBRATION_VOLTAGE = 17;
	public static final int AUTO_SAFE_VOLTAGES = 18;
	
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
//	private static final double MPPT_RTD_EXTRA_RESISTANCE = 6.58; // This is an error resistance in the RTD line, as calculated for MinusX plane from Jerry's measurements of Panel Temperatures
//	private static final int MPPT_TEMP_OFF_VALUE = 1600;
	public static final int MPPT_DEFAULT_TEMP = 0; // plot this value when the raw is below the OFF VALUE
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

	int convInt; // the legacy conversion number
	
	public ConversionLegacy(String name, Spacecraft fox) {
		super(name, fox);
		convInt = 0;
		try {
			convInt = Integer.parseInt(name);
		} catch (NumberFormatException e) { convInt = 0;}
		
	}

	@Override
	public double calculate(double x) {
		throw new RuntimeException("Call legacy conversion "+name+" with calculateLegacy method only!");
	}
	
	public double calculateLegacy(double x, FramePart framePart, String fieldName) {
		double result = convertRawValue(framePart, fieldName, x, convInt, fox);
		return result;
	}

	@Override
	public String calculateString(double x) {
		String s = legacyStringConversion(convInt, x, fox);
		return s;
	}
	
	@Deprecated
	private String legacyStringConversion(int conv, double value, Spacecraft fox) {
		String s = "-----";
		if (value == FramePart.ERROR_VALUE) {
			s = "-----";
		} else
		if (conv == BitArrayLayout.CONVERT_ANTENNA) {
			if (value == 0)
				s = "Stowed";
			else
				s = "Deployed";
		} else if (conv == BitArrayLayout.CONVERT_COM1_ISIS_ANT_STATUS) {  
			s = isisAntennaStatus((int)value, true);
		} else if (conv == BitArrayLayout.CONVERT_STATUS_BIT) {
			if (value == 0)
				s = "OK";
			else
				s = "FAIL";
		} else if (conv == BitArrayLayout.CONVERT_STATUS_ENABLED) {
			if (value == 1)
				s = "Enabled";
			else
				s = "Disabled";
		} else if (conv == BitArrayLayout.CONVERT_BOOLEAN) {
			if (value == 1)
				s = "TRUE";
			else
				s = "FALSE";
		} else if ((conv == BitArrayLayout.CONVERT_INTEGER) || (conv == BitArrayLayout.CONVERT_WOD_STORED)) {
			s = Long.toString((long) value);
		} else if (conv == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC) {
			s = ihuDiagnosticString((int) value, true, fox);
		} else if (conv == BitArrayLayout.CONVERT_HARD_ERROR) {
			s = hardErrorString((int) value, true);
		} else if (conv == BitArrayLayout.CONVERT_SOFT_ERROR) {
			s = softErrorStringFox1A((int) value, true);
		} else if (conv == BitArrayLayout.CONVERT_SOFT_ERROR_84488) {
			s = softErrorString84488((int) value, true);
		} else if (conv == BitArrayLayout.CONVERT_ICR_SW_COMMAND_COUNT) {
			s = icrSwCommandCount((int) value, true);	
		} else if (conv == BitArrayLayout.CONVERT_ICR_DIAGNOSTIC) {
			s = icrDiagnosticString((int) value, true);	
		} else if (conv == BitArrayLayout.CONVERT_HUSKY_UW_DIST_BOARD_STATUS) {
			if (value >= 1500) return "OK";
			else return "FAIL";
		} else if (conv == BitArrayLayout.CONVERT_BATTERY 
					|| conv == BitArrayLayout.CONVERT_ICR_VOLT_SENSOR
					|| conv == BitArrayLayout.CONVERT_MPPT_SOLAR_PANEL
					|| conv == BitArrayLayout.CONVERT_COM1_ACCELEROMETER
					|| conv == BitArrayLayout.CONVERT_MEMS_X_ROTATION
					|| conv == BitArrayLayout.CONVERT_MEMS_Y_ROTATION
					|| conv == BitArrayLayout.CONVERT_MEMS_Z_ROTATION
					|| conv == BitArrayLayout.CONVERT_MEMS_SCALAR_ROTATION
					|| conv == BitArrayLayout.CONVERT_LT_VGA) {
				s = String.format("%1.2f", value);
		} else {
				s = String.format("%2.1f", value);
		}

		
		return s;
		
	}
	

	/**
	 * LEGACY CONVERSIONS for backwards compatibility
	 * Given a raw value, BitArrayLayout.CONVERT it into the actual value that we can display based on the
	 * conversion type passed.  Field name is also used in some conversions, e.g. the batteries
	 * @param name
	 * @param rawValue
	 * @param conversion
	 * @return
	 */
	@Deprecated
	protected double convertRawValue(FramePart framePart, String name, double rawValue, int conversion, Spacecraft fox ) {
		
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
			if (name.equalsIgnoreCase("BATT_C_V"))  // then this is fox
				if (fox.useIHUVBatt)
					return fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
				else
					return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/BATTERY_C_SCALING_FACTOR;
			return FramePart.ERROR_VALUE;

		case BitArrayLayout.CONVERT_BATTERY_TEMP:
			return batteryTempTable.calculate(rawValue);
			
		case BitArrayLayout.CONVERT_BATTERY_CURRENT:
			double d = (double)rawValue;
			d = (( d * VOLTAGE_STEP_FOR_2V5_SENSORS - BATTERY_CURRENT_MIN) * fox.user_BATTERY_CURRENT_ZERO + 2)*1000;
			return d;
		case BitArrayLayout.CONVERT_SOLAR_PANEL:
			return rawValue * VOLTAGE_STEP_FOR_3V_SENSORS/SOLAR_PANEL_SCALING_FACTOR;
		case BitArrayLayout.CONVERT_MPPT_SOLAR_PANEL:
			return rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS * MPPT_SOLAR_PANEL_SCALING_FACTOR;
		case BitArrayLayout.CONVERT_SOLAR_PANEL_TEMP:
			return solarPanelTempTable.calculate(rawValue) ;
		case BitArrayLayout.CONVERT_MPPT_SOLAR_PANEL_TEMP:
			if (rawValue < fox.user_mpptSensorOffThreshold) return FramePart.ERROR_VALUE;
			double raw = (double)rawValue;
			double vadc = raw * VOLTAGE_STEP_FOR_2V5_SENSORS;
			double v =  (vadc - MPPT_RTD_AMP_FACTOR) / (MPPT_RTD_AMP_GAIN);
			double r = v / MPPT_RTD_CONSTANT_CURERNT - fox.user_mpptResistanceError;
			
			// Cubic fit using equation from http://www.mosaic-industries.com/embedded-systems/microcontroller-projects/temperature-measurement/platinum-rtd-sensors/resistance-calibration-table
			double t = -247.29+2.3992*r+0.00063962*Math.pow(r,2)+(0.0000010241)*Math.pow(r,3);
			
			return t;
		case BitArrayLayout.CONVERT_TEMP:
			return temperatureTable.calculate(rawValue);
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
			return calcMemsValue((int)rawValue, name, fox);
			//return (rawValue * VOLTAGE_STEP_FOR_3V_SENSORS - MEMS_ZERO_VALUE_VOLTS)/MEMS_VOLT_PER_DPS;

		case BitArrayLayout.CONVERT_MEMS_SCALAR_ROTATION:
			double xdps = framePart.getDoubleValue("SatelliteXAxisAngularVelocity", fox);
			double ydps = framePart.getDoubleValue("SatelliteYAxisAngularVelocity", fox);
			double zdps = framePart.getDoubleValue("SatelliteZAxisAngularVelocity", fox);
			double scalar = Math.sqrt(xdps * xdps + ydps * ydps + zdps * zdps);
			return scalar;
		case BitArrayLayout.CONVERT_MEMS_X_ROTATION:
			xdps = framePart.getDoubleValue("SatelliteXAxisAngularVelocity", fox);
			scalar = framePart.getDoubleValue("MemsScalarRotation", fox);
			double xcosine = xdps/scalar;
			double xangle = Math.acos(Math.abs(xcosine));
			return 360*xangle/(2*Math.PI); // return in degrees
		case BitArrayLayout.CONVERT_MEMS_Y_ROTATION:
			double yvel = framePart.getDoubleValue("SatelliteYAxisAngularVelocity", fox);
			scalar = framePart.getDoubleValue("MemsScalarRotation", fox);
			double ycosine = yvel/scalar;
			double yangle = Math.acos(Math.abs(ycosine));
			return 360*yangle/(2*Math.PI); // return in degrees
		case BitArrayLayout.CONVERT_MEMS_Z_ROTATION:
			double zvel = framePart.getDoubleValue("SatelliteZAxisAngularVelocity", fox);
			scalar = framePart.getDoubleValue("MemsScalarRotation", fox);
			double zcosine = zvel/scalar;
			double zangle = Math.acos(Math.abs(zcosine));
			return 360*zangle/(2*Math.PI); // return in degrees
			
		case BitArrayLayout.CONVERT_RSSI:
			return fox.getLookupTableByName(Spacecraft.RSSI_LOOKUP).calculate(rawValue);
		case BitArrayLayout.CONVERT_IHU_TEMP:
			return fox.getLookupTableByName(Spacecraft.IHU_TEMP_LOOKUP).calculate(rawValue);
		case BitArrayLayout.CONVERT_STATUS_BIT:
			return rawValue;
		case BitArrayLayout.CONVERT_IHU_DIAGNOSTIC:
			return rawValue;
		case BitArrayLayout.CONVERT_HARD_ERROR:
			return rawValue;
		case BitArrayLayout.CONVERT_SOFT_ERROR:
			return rawValue;
		case BitArrayLayout.CONVERT_SOFT_ERROR_84488:
			return rawValue;
		case BitArrayLayout.CONVERT_ICR_SW_COMMAND_COUNT:
			return rawValue;
		case BitArrayLayout.CONVERT_ICR_DIAGNOSTIC:
			return rawValue;
		case BitArrayLayout.CONVERT_WOD_STORED:
			return rawValue * 4;
		case BitArrayLayout.CONVERT_LT_TXRX_TEMP:
			double volts = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			volts = volts / 2;
			return 100 * volts - 50; // TMP36 sensor conversion graph is a straight line where 0.5V is 0C and 0.01V rise is 1C increase.  So 0.75V is 25C
		case BitArrayLayout.CONVERT_LT_PA_CURRENT:
			double voltspa = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			voltspa = voltspa / 2;
			double pacurrent = voltspa/PA_CURRENT_INA194_FACTOR/0.1; 
			return 1000* pacurrent;
		case BitArrayLayout.CONVERT_LT_TX_FWD_PWR:
			double x = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			x = x / 2;
			double y = 1.6707*Math.pow(x, 3) - 12.954*Math.pow(x,2)+ 37.706*x - 14.388;
			return Math.pow(10, y/10);
		case BitArrayLayout.CONVERT_LT_TX_REF_PWR:
			x = rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/0.758; // where 0.758 is the voltage divider
			y = 0.1921*Math.pow(x, 3) + 14.663*Math.pow(x,2)+ 11.56*x - 1.8544;
			if (y < 0) y = 0;  // Power can not be negative
			return y;
		case BitArrayLayout.CONVERT_LT_VGA:
			volts = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			volts = volts / 2;
			return volts;
		case BitArrayLayout.CONVERT_ICR_VOLT_SENSOR:
			volts = rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS;
			volts = volts * 99/75; // based in voltage divider on the ICR of 24k/75k
			return volts;
		case BitArrayLayout.CONVERT_COM1_SPIN:
			double spin = rawValue - 32768; // signed 16 bit with 32768 added by IHU
			spin = spin / 131.0 ; // 131 per dps
			return spin;
		case BitArrayLayout.CONVERT_COM1_ACCELEROMETER:
			double acc = rawValue - 32768; // signed 16 bit with 32768 added by IHU
			acc = acc / 16384.0 ; // 16384 per g.  If we want this in m/s * 9.80665 
			return acc;
		case BitArrayLayout.CONVERT_COM1_MAGNETOMETER:
			double mag = rawValue - 32768; // signed 16 bit with 32768 added by IHU
			mag = mag * 0.6 ; // 0.6 micro Tesla per count value 
			return mag;
		case BitArrayLayout.CONVERT_COM1_GYRO_TEMP:
			double temp = rawValue * 0.1 - 40; // 0 is -40 then increments in 1/10 degree per raw value
			return temp;
		case BitArrayLayout.CONVERT_COM1_ISIS_ANT_TEMP:
			double V = (3.3 / 1023) * 1000 *  rawValue;
			double antTemp = fox.getLookupTableByName(Spacecraft.HUSKY_SAT_ISIS_ANT_TEMP).calculate((int)V);
			return antTemp;
		case BitArrayLayout.CONVERT_COM1_ISIS_ANT_TIME:
			double time = rawValue / 20.0d; // deploy time in 50ms steps
			return time;
		case BitArrayLayout.CONVERT_COM1_ISIS_ANT_STATUS:
			return rawValue;
		case BitArrayLayout.CONVERT_ROOT_10:
			return Math.pow(10, rawValue/10);
		case BitArrayLayout.CONVERT_COM1_TX_FWD_PWR:
			//System.err.println("RAW: " + rawValue); 
			x = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			x = x / 2;
			//System.err.println("X: " + x); 
			y = 1.7685*Math.pow(x, 3) - 13.107*Math.pow(x,2)+ 36.436*x - 13.019;
			//System.err.println("Y: " + y); 
			double z = Math.pow(10, y/10);
			//System.err.println("Z: " + z); 
			return z;
		case BitArrayLayout.CONVERT_COM1_TX_REF_PWR:
			x = rawValue * 1000* VOLTAGE_STEP_FOR_2V5_SENSORS/0.758; // where 0.758 is the voltage divider
			y = 0.1727*x - 34.583;
			return Math.pow(10, y/10);
		case BitArrayLayout.CONVERT_HUSKY_UW_DIST_BOARD_STATUS:
			// TODO - this does not match what Eric sent in conversion document..
			x = rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/0.758; // where 0.758 is the voltage divider
			return x;
		case BitArrayLayout.CONVERT_COM1_RSSI:
			x = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			x = x / 2;
			y = 46.566*x - 135.54;
			return y;
		case BitArrayLayout.CONVERT_COM1_ICR_2V5_SENSOR:
			x = rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/0.758; // where 0.758 is the voltage divider
			return x;
		case BitArrayLayout.CONVERT_COM1_SOLAR_PANEL:
			x = rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/0.324; 
			return x;
		case BitArrayLayout.CONVERT_COM1_BUS_VOLTAGE:
			x = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(rawValue);
			x = x / 2 ;
			x = x / 0.2424;
			return x;
		case BitArrayLayout.CONVERT_VULCAN_STATUS:
			return rawValue;
		case BitArrayLayout.CONVERT_16_SEC_UPTIME:
			return rawValue*16;
		case BitArrayLayout.CONVERT_HERCI_SOURCE:
			return rawValue;
		case BitArrayLayout.CONVERT_HERCI_HEX:
			return rawValue;
		
		}
		
		return rawValue; // no conversion, return as is
	}
	
	/**
	 * Calculate the MEMS rotation value based in the rest values saved in the spacecraft config file, if
	 * they exist.  Otherwise use the default values from the data sheet.
	 * @param value
	 * @param name
	 * @param fox
	 * @return
	 */
	private static double calcMemsValue(int value, String name, Spacecraft fox) {
		double volts = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(value);
		volts = volts / 2;
		double memsZeroValue = MEMS_ZERO_VALUE_VOLTS;
		double result = 0;
		
		if (fox.hasMemsRestValues) {
			int restValue = 0;
			if (name.equalsIgnoreCase(Spacecraft.MEMS_REST_VALUE_X)) restValue = fox.user_memsRestValueX;	
			if (name.equalsIgnoreCase(Spacecraft.MEMS_REST_VALUE_Y)) restValue = fox.user_memsRestValueY;
			if (name.equalsIgnoreCase(Spacecraft.MEMS_REST_VALUE_Z)) restValue = fox.user_memsRestValueZ;
			memsZeroValue = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(restValue);
			memsZeroValue = memsZeroValue/2;
		}
		
	    result = (volts - memsZeroValue)/MEMS_VOLT_PER_DPS;
		return result;
	}
	
	/**
	 * Decode the IHU diagnostic and return a string representation.  shortString is
	 * set to true for display in a small space on the GUI.  When false a longer verbose
	 * explanation is returned
	 * @param rawValue
	 * @param shortString
	 * @return
	 */
	public static String ihuDiagnosticString(int rawValue, boolean shortString, Spacecraft fox) {
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
			if (fox.hasImprovedCommandReceiver) {
				return icrCommandCountFox1E(value, shortString);
			} else if (fox.hasImprovedCommandReceiverII) {
				return icrCommandCount(value, shortString);
			} else {
				if (shortString)
					return "Count: " + value;
				else
					return "Number of commands received since boot: " + value;
			}
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
		case GYRO1Z: // Gyro1Z - this is the "extra" Z axis reading.  We have 2 chips, each with 2 axis.  So we have Z twice
			value = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			if (shortString)
				//return "Gyro1Z: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
				return "Gyro1Z (dps): " + LinePlotPanel.roundToSignificantFigures(calcMemsValue(value, Spacecraft.MEMS_REST_VALUE_Z, fox),3);
			else
				return "Gyro1Z (dps): " + LinePlotPanel.roundToSignificantFigures(calcMemsValue(value, Spacecraft.MEMS_REST_VALUE_Z, fox),3);
			//return "Gyro1 Z Value: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
		case GYRO1V: // Gyro1V
			value = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			double vRef = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(value);
			vRef = vRef/2;
			int cameraChecksumErrors = (rawValue >> 24) & 0xff; // last 8 bits
			cameraChecksumErrors = cameraChecksumErrors - 1; // This is initialized to 1, so we subtract that initial value
			if (shortString)
				return "Gyro1V (V): " + LinePlotPanel.roundToSignificantFigures(vRef,3);
				//return "Gyro1V: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
			else
				return "Gyro1V (V): " + LinePlotPanel.roundToSignificantFigures(vRef,3) + " Camera Checksum Errors: " + cameraChecksumErrors;
				//return "Gyro1 Vref: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS + " Camera Checksum Errors: " + cameraChecksumErrors;
		case GYRO2V: // Gyro2V
			value = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			vRef = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(value);
			vRef = vRef/2;
			int hsAudioBufferUnderflows = (rawValue >> 24) & 0xff; // last 8 bits
			if (shortString)
				return "Gyro2V (V): " + LinePlotPanel.roundToSignificantFigures(vRef,3);
				//return "Gyro2V: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS;
			else
				return "Gyro2V (V): " + LinePlotPanel.roundToSignificantFigures(vRef,3) + " HS Audio Buffer Underflows: " + hsAudioBufferUnderflows;
				//return "Gyro2 Vref: " + value * FramePart.VOLTAGE_STEP_FOR_3V_SENSORS + " HS Audio Buffer Underflows: " + hsAudioBufferUnderflows;
		case IHU_SW_VERSION: // Version of the software on the IHU
			int swType = (rawValue >> 8) & 0xff;
			int swMajor = (rawValue >> 16) & 0xff;
			int swMinor = (rawValue >> 24) & 0xff;
			
			return "IHU SW: " + Character.toString((char) swType) + fox.foxId + "." + Character.toString((char) swMajor) + Character.toString((char) swMinor);
		case ISISStatus: // Version of the software on the IHU
			int antStatus = (rawValue >> 8) & 0xffff; // shift away the type byte and get 16 bit status
			
			// Bus status - 0 - did not try to read, 1 tried and failed, tried and succeeded
			int bus0 = (rawValue >> 24) & 0xf;
			int bus1 = (rawValue >> 28) & 0xf;
			return "ISIS Status: " + Integer.toHexString(antStatus) + " " + Integer.toHexString(bus0) + " "+Integer.toHexString(bus1);
		case UNKNOWN: // IHU measurement of bus voltage
			value = (rawValue >> 8) & 0xfff;
			return "Bus Voltage: " + value + " - " + LinePlotPanel.roundToSignificantFigures(fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(value),3) + "V";
		case IHU_TEMP_CALIBRATION_VOLTAGE: // IHU measurement of bus voltage
			value = (rawValue >> 8) & 0xffffff;  // 24 bits of temp calibration
			return "IHU Temp Cal: " + value;
		case AUTO_SAFE_VOLTAGES:
			int value1 = (rawValue >> 8) & 0xfff; // 12 bit value after the type
			double voltageIn = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(value1)/2;
			int value2 = (rawValue >> 20) & 0xfff; // last 12 bits
			double voltageOut = fox.getLookupTableByName(Spacecraft.IHU_VBATT_LOOKUP).calculate(value2)/2;
			if (shortString)
				return "AS Vin: " + LinePlotPanel.roundToSignificantFigures(voltageIn*99/24,3) +
						" Vout: " + LinePlotPanel.roundToSignificantFigures(voltageOut*99/24,3);
			else
				return "Auto Safe Vin: " + LinePlotPanel.roundToSignificantFigures(voltageIn*99/24,3) +
						" Vout: " + LinePlotPanel.roundToSignificantFigures(voltageOut*99/24,3);
		}
		return "-----" + type;
	}

	// Flattened C ENUM for ICRDiagnostic aka swCmds - COMMAND NAME SPACE
//		public static final int SWCmdNSSpaceCraftOps = 1;
//		public static final int SWCmdNSTelemetry = 2;
//		public static final int SWCmdNSExperiment1 = 3;
//		public static final int SWCmdNSExperiment2 = 4;
//		public static final int SWCmdNSExperiment3 = 5;
//		public static final int SWCmdNSExperiment4 = 6;
//		public static final int SWCmdNSCan = 7;
//		public static final int SWCmdNSGyro = 8;
//
//		// Flattened C ENUM for ICRDiagnostic Command names in Ops Namespace
//		public static final String[] SWOpsCommands = {
//		"Unknown", //0
//		"SafeMode", //1
//		"TransponderMode", //2
//		"ScienceMode", //3
//		"DisableAutosafe", //4
//		"EnableAutosafe", //5
//		"ClearMinMax", //6
//		"OpsNoop", //7
//		"ForceOffExp1", //8
//		"ForceDeployRx", //9
//		"ForceDeployTx", //0xA
//		"ResetIHU" //0xB
//		};
//
//		// Flattened C ENUM for ICRDiagnostic Command names in Tlm Namespace
//		public static final String[] SWTlmCommands = {
//		"Unknown",
//		"Gain",
//		"WODSaveSize",
//		"Encoding",
//		"Frequency"
//		};
//
//		// Flattened C ENUM for ICRDiagnostic Command names in Exp1 Namespace
//		public static final String[] SWExp1Commands = {
//		"Unknown",
//		"CycleTiming",
//		"SetBoard"
//		};
	
		
	/**
	 * The Diagnostic String for the Improved Command Receiver shows a ring of the last commands.  There are 32 bits holding up
	 * to four commands.  Each byte has the top 3 bits that hold the namespace and 5 bits for the command name	
	 * @param rawValue
	 * @param shortString
	 * @return
	 */
	public static String[] icrDiagnosticStringArray(int rawValue, boolean shortString) {
		String[] s = new String[4];
		for (int i=0; i<4; i++) {
			s[i] = "";
			int value = rawValue & 0x1f; // bottom 5 bits hold the command
			rawValue = rawValue >> 5;
			int nameSpace = rawValue & 0x7; // Top 3 bits of the byte hold the command namespace
			
			if ( nameSpace == 0)
				s[i] = s[i] + "-:--";
			else {
				s[i] = nameSpace + ":" + value;
			}
//			switch (nameSpace) {
//
//			case SWCmdNSSpaceCraftOps: // Spacecraft Operations
//				if (shortString)
//					s[i] = s[i] + SWOpsCommands[value];
//				else
//					s[i] = s[i] + "Ops: " + SWOpsCommands[value];
//				break;
//			case SWCmdNSTelemetry: // Telemetry Control
//				if (shortString)
//					s[i] = s[i] + SWTlmCommands[value];
//				else
//					s[i] = s[i] + "Tlm: " + SWTlmCommands[value];
//				break;
//			case SWCmdNSExperiment1: // Experiment 1
//				if (shortString)
//					s[i] = s[i] + SWExp1Commands[value];
//				else
//					s[i] = s[i] + "Exp1: " + SWExp1Commands[value];
//			break;
//			default:
//				s[i] = s[i] + "ERR:" + nameSpace;
//			}
			rawValue = rawValue >> 3; // move to the next byte
		}
		return s;
	}

	public static String icrDiagnosticString(int rawValue, boolean shortString) {
		String[] s = icrDiagnosticStringArray(rawValue, shortString);
		String result = "---";
		if (s[0] != null)
			result = s[0];
		for (int i=1; i< s.length; i++)
			if (s[i] != null)
				result = result + ", " + s[i];
		return result;
	}
	
	public static String icrCommandCountFox1E(int rawValue, boolean shortString) {
		String s = new String();
		if (rawValue != FramePart.ERROR_VALUE) {
			// First 8 bits are the hardware commands 
			int hardwareCmds = rawValue & 0xff ;

			// 8 MSBs are software command number
			int softwareCmds = (rawValue >> 8) & 0xff;
			
			if (shortString)
				s = s + "hw " + hardwareCmds + " "
						+ "sw " + softwareCmds;
			else
				s = s + "Number of Hardware Cmds: " + hardwareCmds  + " "
				+ " Software Cmds: " + softwareCmds;
		}
		return s;
	}
	
	public static String icrSwCommandCount(int rawValue, boolean shortString) {
		String s = new String();
		
		// 1 bit for Reply Time Checking bit
		int timeMarker = (rawValue) & 0x1;
					
		// 11 MSBs are software command number
		int softwareCmds = (rawValue >> 1) & 0xfff;

		if (shortString)
			s = s + " rtc:" + timeMarker 
					+ " sw:" + softwareCmds;
		else
			s = s + " Reply Time Checking: " + timeMarker
			+ " SW Cmds: " + softwareCmds;

		return s;
	}
	
	public static String icrCommandCount(int rawValue, boolean shortString) {
		String s = new String();
		if (rawValue != FramePart.ERROR_VALUE) {
			// First 12 bits are the hardware commands 
			int hardwareCmds = rawValue & 0xfff ;

			String swCmds = icrSwCommandCount((rawValue >> 12) & 0xfff, shortString);
			
			if (shortString)
				s = s + "hw:" + hardwareCmds + swCmds;
			else
				s = s + "HW Cmds: " + hardwareCmds  + swCmds;
		}
		return s;
		}

	/**
	 * Decode the IHU Hard Error bits
	 * @param rawValue
	 * @return
	 */
	public static String hardErrorString(int rawValue, boolean shortString) {
		String s = new String();
		if (rawValue != FramePart.ERROR_VALUE) {
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
			else {
				s = s + "Watchdog Reports: " + FoxBitStream.stringBitArray(FoxBitStream.intToBin9(watchDogReports))  
				+ " ";//  Integer.toHexString(watchDogReports) + " "
				if (errorCode < ihuErrorType.length)
					s = s	+ " Error Type: " + ihuErrorType[errorCode] + " ";
				else
					s = s	+ " Error Type: " + errorCode;
				
				s = s	+ " MRAM Error Count: " + Integer.toHexString(mramErrorCount) + " "
						+ " Non Fatal Error Count: " + Integer.toHexString(nonFatalErrorCount) + " "
						+ " Task Number: " + taskNumber + "- ";
				if (taskNumber < ihuTask.length)
					s = s + ihuTask[taskNumber];
				else
					s = s + "Unknown";
				s = s + " Alignment: " + alignment;
			}
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
		if (rawValue != FramePart.ERROR_VALUE) {
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

			s[0] = ""+errorCode;
			if (errorCode < ihuErrorType.length)
				s[0] = s[0] + " - " + ihuErrorType[errorCode];
			s[1] = Integer.toHexString(alignment);

			s[2] =  FoxBitStream.stringBitArray(FoxBitStream.intToBin9(watchDogReports));
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

	public static String[] isisAntennaStatusArray(int rawValue, boolean shortString) {
		String[] s = new String[16];
		if (rawValue == 9999) {
			for (int i=0; i< s.length; i++)
				s[i] = "";
			return s;
		}
		// First bit is the ARM bit 
		int ARM = rawValue & 0x1;  // ARM
		int A4B = (rawValue >> 1) & 0x1;  // A4B
		int A4T = (rawValue >> 2) & 0x1;  // A4T
		int A4S = (rawValue >> 3) & 0x1;  // A4S
		int INDB = (rawValue >> 4) & 0x1; // INDB
		int A3B = (rawValue >> 5) & 0x1;  // A3B
		int A3T = (rawValue >> 6) & 0x1;  // A3T
		int A3S = (rawValue >> 7) & 0x1;  // A3S
		int IG = (rawValue >> 8) & 0x1;   // IG
		int A2B = (rawValue >> 9) & 0x1;  // A2B
		int A2T = (rawValue >> 10) & 0x1;  // A2T
		int A2S = (rawValue >> 11) & 0x1;  // A2S
		int zero = (rawValue >> 12) & 0x1; // zero
		int A1B = (rawValue >> 13) & 0x1;  // A1B
		int A1T = (rawValue >> 14) & 0x1;  // A1T
		int A1S = (rawValue >> 15) & 0x1;  // A1S

		if (shortString) {
			if (ARM == 1) s[0] = "ARM "; else s[0] = "";
			if (INDB == 1) s[1] = "BURN "; else s[1] = "";
			if (IG == 1) s[2] = "IG "; else s[2] = "";
			if (A1S == 1) s[3] = "N "; else s[3] = "Y ";
			if (A2S == 1) s[4] = "N "; else s[4] = "Y ";
			if (A3S == 1) s[5] = "N "; else s[5] = "Y ";
			if (A4S == 1) s[6] = "N "; else s[6] = "Y ";
		} else {
			if (ARM == 1) s[0] = "ARMED"; else s[0] = "NO";
			if (INDB == 1) s[1] = "ACT"; else s[1] = "NONE";
			if (IG == 1) s[2] = "YES"; else s[2] = "NO";
			if (A1B == 1) s[3] = "ACT"; else s[3] = "NOT ACT";
			if (A1T == 1) s[4] = "TIMEOUT"; else s[4] = "---";
			if (A1S == 1) s[5] = "NO"; else s[5] = "YES";
			if (A2B == 1) s[6] = "ACT"; else s[6] = "NOT ACT";
			if (A2T == 1) s[7] = "TIMEOUT"; else s[7] = "--";
			if (A2S == 1) s[8] = "NO"; else s[8] = "YES";
			if (A3B == 1) s[9] = "ACT"; else s[9] = "NOT ACT";
			if (A3T == 1) s[10] = "TIMEOUT"; else s[10] = "--";
			if (A3S == 1) s[11] = "NO"; else s[11] = "YES";
			if (A4B == 1) s[12] = "ACT"; else s[12] = "NOT ACT";
			if (A4T == 1) s[13] = "TIMEOUT"; else s[13] = "--";
			if (A4S == 1) s[14] = "NO"; else s[14] = "YES";
			s[15] = ""+zero;
		}
		return s;
	}

	public static String isisAntennaStatus(int rawValue, boolean shortString) {
		String[] s = isisAntennaStatusArray(rawValue, shortString);
		String result = "---";
		if (s[0] != null)
			result = s[0];
		for (int i=1; i< s.length; i++)
			if (s[i] != null)
				result = result + s[i];
		return result;
	}

	/**
	 * Decode the IHU Soft error bits
	 * @param rawValue
	 * @return
	 */
	public static String softErrorStringFox1A(int rawValue, boolean shortString) {
		// Soft error is 4 8 bit numbers 
		String s = new String();
		if (rawValue != FramePart.ERROR_VALUE) {
			int DACoverflows = rawValue & 0xff;
			int I2CRetries = (rawValue >> 8) & 0xff;
			int SPIRetries = (rawValue >> 16) & 0xff;
			int MramCRCs = (rawValue >> 24) & 0xff;

			if (shortString)
				s = s + "dac " + DACoverflows + " i2c " + I2CRetries + " spi " + SPIRetries + " mr " + MramCRCs;
			else
				s = s + "DAC Overflows: " + DACoverflows + "  I2C Retries: " + I2CRetries + "  SPI Retries: " + SPIRetries + "  MRAM CRCs: " + MramCRCs;
		}
		return s;
	}
	
	public static String softErrorString84488(int rawValue, boolean shortString) {
		// Soft error is 4 8 bit numbers 
		String s = new String();
		if (rawValue != FramePart.ERROR_VALUE) {
			int DACoverflows = rawValue & 0xff;
			int I2C1Retries = (rawValue >> 8) & 0x0f;
			int I2C2Retries = (rawValue >> 12) & 0x0f;
			int SPIRetries = (rawValue >> 16) & 0xff;
			int MramCRCs = (rawValue >> 24) & 0xff;

			if (shortString)
				s = s + "dac " + DACoverflows + " i2c1 " + I2C1Retries + " i2c2 " + I2C2Retries + " spi " + SPIRetries + " mr " + MramCRCs;
			else
				s = s + "DAC Overflows: " + DACoverflows + "  I2C1 Retries: " + I2C1Retries + "  I2C2 Retries: " + I2C2Retries+ "  SPI Retries: " + SPIRetries + "  MRAM CRCs: " + MramCRCs;
		}
		return s;
	}

	public static String[] softErrorStringArrayFox1A(int rawValue, boolean shortString) {
		// Soft error is 4 8 bit numbers 
		String[] s = new String[4];
		if (rawValue != FramePart.ERROR_VALUE) {
			int DACoverflows = rawValue & 0xff;
			int I2CRetries = (rawValue >> 8) & 0xff;
			int SPIRetries = (rawValue >> 16) & 0xff;
			int MramCRCs = (rawValue >> 24) & 0xff;

			s[0] = Integer.toString(DACoverflows); 
			s[1] = Integer.toString(I2CRetries);
			s[2] = Integer.toString(SPIRetries);
			s[3] = Integer.toString(MramCRCs);
		}
		return s;
	}
	
	public static String[] softErrorStringArray84488(int rawValue, boolean shortString) {
		// Soft error is 5 numbers 
		String[] s = new String[5];
		if (rawValue != FramePart.ERROR_VALUE) {
			int DACoverflows = rawValue & 0xff;
			int I2C1Retries = (rawValue >> 4) & 0x0f;
			int I2C2Retries = (rawValue >> 4) & 0x0f;
			int SPIRetries = (rawValue >> 8) & 0xff;
			int MramCRCs = (rawValue >> 8) & 0xff;

			s[0] = Integer.toString(DACoverflows); 
			s[1] = Integer.toString(I2C1Retries);
			s[2] = Integer.toString(I2C2Retries);
			s[3] = Integer.toString(SPIRetries);
			s[4] = Integer.toString(MramCRCs);
		}
		return s;
	}
	

	
}
