package test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Location;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
public class LocationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGridSquareToLatLon() {
		Location loc = new Location("JN58td");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, 48.14666f, 2f/60);
		assertEquals(loc.longitude,11.60833f, 4f/60);
	
//		loc = new Location("IO91tp");
//		System.out.println(loc.latitude + " " + loc.longitude );
//		assertEquals(loc.latitude, 51.665, 2f/60);
//		assertEquals(loc.longitude,-0.3957f, 4f/60);

		loc = new Location("GF15vc");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, -34.91, 2f/60);
		assertEquals(loc.longitude,-56.21166f, 4f/60);

		loc = new Location("FM18lw");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, 38.92, 2f/60);
		assertEquals(loc.longitude,-77.065f, 4f/60);
		
		loc = new Location("RE78ir");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, -41.28333, 2f/60);
		assertEquals(loc.longitude,174.745f, 4f/60);
		
		loc = new Location("FN31pr");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, 41.714775, 2f/60);
		assertEquals(loc.longitude, -72.72726f, 4f/60);
		
		loc = new Location("CM87wj");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, 37.413708, 2f/60);
		assertEquals(loc.longitude,-122.1073236f, 4f/60);
		
		loc = new Location("EM75kb");
		System.out.println(loc.latitude + " " + loc.longitude );
		assertEquals(loc.latitude, 35.0542, 2f/60);
		assertEquals(loc.longitude,-85.114f, 4f/60);
	}
	
	@Test
	public void testLatLonToGridSquare() {
		Location loc = new Location(48.14666f,11.60833f);
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "JN58td");
		
		loc = new Location("-34.91","-56.21166");
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "GF15vc");
		
		loc = new Location(38.92f,-77.065f);
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "FM18lw");
		
		loc = new Location(-41.28333f,174.745f);
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "RE78ir");

		loc = new Location(41.714775f,-72.727260f);
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "FN31pr");

		loc = new Location(37.413708f,-122.1073236f);
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "CM87wj");

		loc = new Location(35.0542f,-85.1142f );
		System.out.println(loc.maidenhead);
		assertEquals(loc.maidenhead, "EM75kb");

	}

}
