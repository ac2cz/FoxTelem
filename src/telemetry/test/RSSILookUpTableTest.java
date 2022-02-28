package telemetry.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import telemetry.conversion.ConversionLookUpTable;

public class RSSILookUpTableTest {

	ConversionLookUpTable table;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		table = new ConversionLookUpTable("test");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLookupValue() {
		double val = 0.0;
		
		val = table.calculate(1900);
		assertEquals(-100.0d, val, 0.0d);
	}
	
	@Test
	public void testInterpolateValue() {
		double val = 0.0;
		
		// Interpolation
		val = table.calculate(1600);
		assertEquals(-112.3d, val, 0.1d);
	}

	@Test
	public void testExtrapolateFirstValue() {
		double val = 0.0;
		
		// Extrapolation
		val = table.calculate(1300);
		assertEquals(-145.7d, val, 0.1d);

		val = table.calculate(1000);
		assertEquals(-186.7d, val, 0.1d);

	}

	@Test
	public void testExtrapolateEndValue() {
		double val = 0.0;
		
		// Extrapolation
		val = table.calculate(2420);
		assertEquals(-73.3d, val, 0.1d);

		val = table.calculate(2600);
		assertEquals(-13.3d, val, 0.1d);

		
	}

}
