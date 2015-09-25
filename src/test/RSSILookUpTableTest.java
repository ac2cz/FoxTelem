package test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import telemetry.LookUpTableRSSI;

public class RSSILookUpTableTest {

	LookUpTableRSSI table;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		table = new LookUpTableRSSI();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLookupValue() {
		double val = 0.0;
		
		val = table.lookupValue(1900);
		assertEquals(-100.0d, val, 0.0d);
	}
	
	@Test
	public void testInterpolateValue() {
		double val = 0.0;
		
		// Interpolation
		val = table.lookupValue(1600);
		assertEquals(-112.3d, val, 0.1d);
	}

	@Test
	public void testExtrapolateFirstValue() {
		double val = 0.0;
		
		// Extrapolation
		val = table.lookupValue(1300);
		assertEquals(-145.7d, val, 0.1d);

		val = table.lookupValue(1000);
		assertEquals(-186.7d, val, 0.1d);

	}

	@Test
	public void testExtrapolateEndValue() {
		double val = 0.0;
		
		// Extrapolation
		val = table.lookupValue(2420);
		assertEquals(-73.3d, val, 0.1d);

		val = table.lookupValue(2600);
		assertEquals(-13.3d, val, 0.1d);

		
	}

}
