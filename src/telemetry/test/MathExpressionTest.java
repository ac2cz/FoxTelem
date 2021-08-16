package telemetry.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.conversion.ConversionLookUpTable;
import telemetry.conversion.MathExpression;
import telemetry.payloads.PayloadRtValues;

public class MathExpressionTest {
	BitArrayLayout lay;
	FramePart rt;
	Spacecraft fox1;
	
	@Before
	public void setUp() throws Exception {
		String logFileDir = "C:\\Users\\chris\\Desktop\\Test\\JUNIT_TEST";
		Config.homeDirectory = logFileDir;
		Log.init("JUnitTest");
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 	
		Config.minInit(logFileDir); 
		Config.initSatelliteManager();
		Log.println("LogFileDir is:" + Config.logFileDirectory);
		
		fox1 = Config.satManager.getSpacecraft(1);
		lay = Config.satManager.getLayoutByName(fox1.foxId, Spacecraft.REAL_TIME_LAYOUT);
		rt = new PayloadRtValues(lay);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testMathExpression() {
		MathExpression math = new MathExpression("1 + 2", 5);
		double y = math.parse(rt, fox1);
		assertEquals(3.0d, y, 0.0d);
	}
	
	@Test
	public void testComplexMathExpression() {
		MathExpression math = new MathExpression(" ((4  	- 2^3+1) * -sqrt (3*3+4 *4)) / 2", 0);
		double y = math.parse(rt, fox1);
		assertEquals(7.5d, y, 0.0d);
	}
	
	@Test
	public void testRawValue() {
		MathExpression math = new MathExpression("1 + x", 5);
		double y = math.parse(rt, fox1);
		assertEquals(6.0d, y, 0.0d);
		
		math = new MathExpression("10^(10/x)", 5);
		y = math.parse(rt, fox1);
		assertEquals(100.0d, y, 0.0d);
		
		math = new MathExpression("10^x", 2);
		y = math.parse(rt, fox1);
		assertEquals(100.0d, y, 0.0d);
	}
	
	@Test
	public void testComplexRawValue() {
		MathExpression math = new MathExpression("((4  	- 2^x+1) * -sqrt (x*x+4 *4)) / 2", 3);
		double y = math.parse(rt, fox1);
		assertEquals(7.5d, y, 0.0d);
		
		math = new MathExpression("((X  	- 2^3+1) * -sqrt (3*3+x *x)) / 2", 4);
		y = math.parse(rt, fox1);
		assertEquals(7.5d, y, 0.0d);
		
		math = new MathExpression("x^2", 2);
		y = math.parse(rt, fox1);
		assertEquals(4d, y, 0.0d);
		
		math = new MathExpression("2^X", 2);
		y = math.parse(rt, fox1);
		assertEquals(4d, y, 0.0d);
		
		math = new MathExpression("((4 - x^3+1) * -sqrt (3*3+4 *4)) / x", 2);
		y = math.parse(rt, fox1);
		assertEquals(7.5d, y, 0.0d);
	}
	
	@Test
	public void testFunctions() {
		MathExpression math = new MathExpression("cos(3.1415926)", 0);
		double y = math.parse(rt, fox1);
		assertEquals(-1, y, 0.0001d);
		
		math = new MathExpression("sin(3.1415926)", 0);
		y = math.parse(rt, fox1);
		assertEquals(0, y, 0.0001d);
		
		math = new MathExpression("sin(x)", 3.1415926);
		y = math.parse(rt, fox1);
		assertEquals(0, y, 0.0001d);
		
		math = new MathExpression("tan(3.1415926)", 0);
		y = math.parse(rt, fox1);
		assertEquals(-5.358e-8, y, 0.0001d);
		
		math = new MathExpression("acos(1)", 0);
		y = math.parse(rt, fox1);
		assertEquals(0, y, 0.0001d);
		
		math = new MathExpression("asin(1)", 0);
		y = math.parse(rt, fox1);
		assertEquals(1.5707, y, 0.0001d);
		
		math = new MathExpression("atan(1)", 0);
		y = math.parse(rt, fox1);
		assertEquals(0.78539, y, 0.0001d);
		
		math = new MathExpression("abs(1)", 0);
		y = math.parse(rt, fox1);
		assertEquals(1, y, 0.0001d);
		
		math = new MathExpression("abs(-1)", 0);
		y = math.parse(rt, fox1);
		assertEquals(1, y, 0.0001d);
		
		math = new MathExpression("10^4", 0);
		y = math.parse(rt, fox1);
		assertEquals(10000, y, 0.0001d);
	}

	@Test
	public void testFields() {
		rt.fieldValue[0] = 1000; // BATT_A_V runs conversion 4 = rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS
		rt.fieldValue[1] = 1000; // BATT_B_V rawValue * VOLTAGE_STEP_FOR_2V5_SENSORS/BATTERY_B_SCALING_FACTOR
		
		double expected_result = 1000 * 2.5/4096 + 1000 * 2.5/4096 / 0.76;
		
		MathExpression math = new MathExpression("BATT_A_V + BATT_B_V", 0);
		double y = math.parse(rt, fox1);
		assertEquals(expected_result, y, 0.0001d);
		
	}
}
