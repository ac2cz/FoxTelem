package telemetry.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import telemetry.conversion.ConversionLookUpTable;
import telemetry.conversion.ConversionStringLookUpTable;

public class ConversionStringLookUpTableTest {

	ConversionStringLookUpTable table;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		table = new ConversionStringLookUpTable("STATUS_ENABLED", "status_enabled.tab");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLookupValue() {
		double val = 0.0;
		
		val = table.calculate(1900);
		assertEquals(1900.0d, val, 0.0d);
		
		String s = table.calculateString(0);
		System.err.println(s);
		assertEquals("Disabled", s);
		s = table.calculateString(1);
		System.err.println(s);
		assertEquals("Enabled", s);
		s = table.calculateString(27);
		System.err.println(s);
		assertEquals(ConversionStringLookUpTable.ERROR, s);
	}
	
	

}
