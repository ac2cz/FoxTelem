package telemetry;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConversionMathExpressionTest {

	@Test
	public void testParse() {
		ConversionMathExpression exp = new ConversionMathExpression("","((4  	- 2^3+1) * -sqrt(3*3+4*4)) / 2");
		double r = exp.parse();
		System.out.println(r);  // 7.5
		
		int x = 2123;
		int y = 2126;
		int z = 2104;
		
		String fs;
		fs = String.format("sqrt((((%d-2087)*3/4095)*33.3)^2 + (((%d-2101)*3/4095)*33.3)^2 + (((%d-2045)*3/4095)*33.3)^2)",
		                   x,y,z);
		
		ConversionMathExpression exp2 = new ConversionMathExpression("",fs);
		double r2 = exp2.parse();
		
		System.out.println(r2); // 1.793

		String xa = String.format("acos(abs((((%d-2087)*3/4095)*33.3)/%f))*360/(2*3.14159)", x,r2);
		ConversionMathExpression exp3 = new ConversionMathExpression("",xa);
		double r3 = exp3.parse();
		
		System.out.println(r3); // 60.672

	}

}
