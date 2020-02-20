package telemetry;

public class ConversionStatic extends Conversion {
	private int staticConversion; 
	
	ConversionStatic(int staticConversion) {
		this.staticConversion = staticConversion;
		name = ""+staticConversion;
	}
	
	public int getConversionInt() { return staticConversion; }
}
