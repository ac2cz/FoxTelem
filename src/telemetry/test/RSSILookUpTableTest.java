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
		table = new ConversionLookUpTable("RSSI", "FOX1A_rssiFM.tab",null);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLookupValue() {
		double val = 0.0;
		
		val = table.calculate(1920);
		assertEquals(-80.0d, val, 0.0d);
	}
	
	@Test
	public void testInterpolateValue() {
		double val = 0.0;
		
		// Interpolation
		val = table.calculate(1600);
		assertEquals(-94.66d, val, 0.1d);
	}

	@Test
	public void testExtrapolateFirstValue() {
		double val = 0.0;
		
		// Extrapolation
		val = table.calculate(900);
		assertEquals(-127.28d, val, 0.1d);

	}

	@Test
	public void testExtrapolateEndValue() {
		double val = 0.0;
		
		// Extrapolation
		val = table.calculate(2600);
		assertEquals(-65d, val, 0.1d);

		
	}

}
