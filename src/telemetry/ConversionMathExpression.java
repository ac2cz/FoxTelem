package telemetry;

import common.Spacecraft;

public class ConversionMathExpression extends Conversion {

	int pos = -1, ch;
	String str;
	
	public ConversionMathExpression(String name, String str) {
		super(name);
		this.str = str;
	}
	
	public double calculate(double x) {
		throw new RuntimeException("Call conversion "+name+" with calculateExpression method only!");
	}
	
	public double calculateExpression(double x, FramePart framePart, Spacecraft spacecraft) {
		
		
		/// DUH, use x here to support pipelines!!
		
		MathExpression exp = new MathExpression(str, x);
		double y = exp.parse(framePart, spacecraft);
		return y;
	}
	
	/**
	 * Return the passed value as a String, apply default formatting if this is the only conversion or if it is the 
	 * last in a pipeline
	 * 
	 * @param lookUpkKey
	 * @return
	 */
	public String calculateString(double x) {
		String s = String.format("%2.1f", x);
		return s;
	}
    
    public String toString() {
    	String s = "";
    	s = name + ": " + str;
    	return s;
    }
}
