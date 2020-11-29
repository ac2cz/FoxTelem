package telemetry;

public abstract class Conversion {
	public static final String FMT_INT = "INT";
	public static final String FMT_F = "FLOAT";
	public static final String FMT_1F = "FLOAT1";
	public static final String FMT_2F = "FLOAT2";

	protected String name; // must be unique in the namespace of the spacecraft
	
	Conversion(String name) {
		if (name == null) throw new IllegalArgumentException("Conversion name null");
		this.name = name;
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
