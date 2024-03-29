package telemetry.conversion;

import common.Spacecraft;

public abstract class Conversion {
	public static final String FMT_INT = "INT";
	public static final String FMT_BIN = "BIN";
	public static final String FMT_HEX = "HEX";
	public static final String FMT_F = "FLOAT";
	public static final String FMT_1F = "FLOAT1";
	public static final String FMT_2F = "FLOAT2";
	public static final String FMT_3F = "FLOAT3";
	public static final String FMT_4F = "FLOAT4";
	public static final String FMT_5F = "FLOAT5";
	public static final String FMT_6F = "FLOAT6";
	public static final String TIMESTAMP = "TIMESTAMP";
	
	protected String name; // must be unique in the namespace of the spacecraft
	protected Spacecraft fox;
	
	Conversion(String name, Spacecraft sat) {
		if (name == null) throw new IllegalArgumentException("Conversion name null");
		this.name = name;
		this.fox = sat;
	}
	
	public String getName() { return name; }
	abstract public double calculate(double x);
	abstract public String calculateString(double x);
	
	public static String getLastConversionInPipeline(String convName) {
		String[] conversions = convName.split("\\|"); // split the conversion based on | in case its a pipeline
		// If this is a pipeline then all of the conversions have been run to calculate the dvalue.  
		// We only care about the final conversion to get a string value
		String lastConv = conversions[conversions.length-1].trim();
		return lastConv;
	}
	
	public static int getLegacyConversionFromString(String lastConv) {
		int convInt = 0;
		try {
			convInt = Integer.parseInt(lastConv);
		} catch (NumberFormatException e) { convInt = 0;}
		return convInt;
	}

}
