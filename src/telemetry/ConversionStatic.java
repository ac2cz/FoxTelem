package telemetry;

public class ConversionStatic extends Conversion {
	private int staticConversion; 
	
	ConversionStatic(int staticConversion) {
		super(""+staticConversion);
		this.staticConversion = staticConversion;
	}
	
	public int getConversionInt() { return staticConversion; }

	@Override
	public double calculate(double x) {
		// TODO Auto-generated method stub
		return 0;
	}
}
