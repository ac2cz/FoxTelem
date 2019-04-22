package test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import telemetry.uw.CanPacket;
import decoder.FoxBitStream;

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
public class CanPacketTest {

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
	public void test() {
		
//		int[] raw = {0x00,0x02,0x20,0x80,0x01,0x02,0x03,0x04,0x05};
		int[] raw = {0x00,0x02,0x20,0x80,0x01,0x02,0x03,0x04,0x05};
		CanPacket packet = new CanPacket(null);
		
		for (int i : raw)
			packet.addNext8Bits((byte)i);
		System.out.println(packet.toString());
		
		printByte(raw[0]);
		printByte(raw[1]);
		printByte(raw[2]);
		printByte(raw[3]);
	}
	
	private void printByte(int i) {
		boolean[] w = FoxBitStream.intToBin8(i);
		for(boolean b : w)
			System.out.print(b ? 1 : 0);
		System.out.println();
	}

}
