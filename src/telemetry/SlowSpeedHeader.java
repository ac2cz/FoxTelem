package telemetry;

import common.Config;

import decoder.FoxDecoder;
/**
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
 *
 * The Header for the slow speed telemetry
 * 48 bits of data that store the header information for the slow speed telemetry frames
 * 
 * The satellite sends the data most significant bit first, but it is a little endian system
 * So we get the least significant byte first, but with its most significant bit received first.
 * 
 * To compensate we reverse the order of the received bytes when we store them in the bit array.
 * That means that the least significant bit of the least significant byte is stored first and 
 * the msb of the whole number is stored last.
 * When we read the value out of the array, we again read it backwards, starting with the highest
 * number in the array (which is the msb) and then down to the lowest number.  This is then converted 
 * to an int and stored in the field.
 * 
 * More detailed explanation follows, starting with the 10 bit SYNC word and then the 8 bit decoded words
 * that typically follow in the header
 * 
 
1 0 | 1  0  1  0  1  0  1  0 sync
    | 1  0  1  1  0} [0  0  1]<- [FoxID] (3 bits) = 001
    | 0  0  0  0  0   0  0  1 <- {Reset count} (16 bits = 000 00000001 10110 = 54)
    | 0  1  0  1  1] {0  0  0
    | 0  0  0  0  0   0  0  1 <- [Uptime] (25 bits = 0000 00000000 00000001 01011 = 43
    | 0  0  0  0  0   0  0  0
    |{0  0  0  1}[0   0  0  0 <- {Frame type} (4 bits = 0001)


Now in your 1-bit-per-array entry put it as follow

Array index   Value
0                    1     <- Least significant bit of first byte and of the FoxID
1                    0
2                    0     <- Most sig bit of Fox ID
3                    0 <- Least significant bit of reset count
4                    1
5                    1
6                    0
7                    1  <- Most significant bit of first byte

8                    1  <- Least significant bit of 2nd byte
9                    0
10                  0
11                  0
12                  0
13                  0
14                  0
15                  0  <- Most significant bit of 2nd byte                 

16                  0
17                  0
18                  0 <- Most significant bit of reset count
19                  1 <- Least significant bit of uptime.
20                  1
21                  0
22                  1
23                  0

24                       Least significant bit of byte 3 etc.
 * 
 */
public class SlowSpeedHeader extends Header {
	
	public SlowSpeedHeader() {
		super(TYPE_SLOW_SPEED_HEADER);
		MAX_BYTES = SlowSpeedFrame.MAX_HEADER_SIZE;
		rawBits = new boolean[MAX_BYTES*8];
	}
	
	public int getType() { return type; }
	
	/**
	 * Take the bits from the faw bit array and copy them into the fields
	 */
	public void copyBitsToFields() {
		super.copyBitsToFields();
		type = nextbits(4);

	}
	
	public boolean isValid() {
		copyBitsToFields();
		if (Config.satManager.validFoxId(id) && isValidType(type))
			return true;
		return false;
	}
		
	public String toString() {
		copyBitsToFields();
		String s = new String();

		
		s = s + "AMSAT FOX-1 Telemetry Captured at: " + reportDate() + "\n" 
				+ "ID: " + FoxDecoder.dec(id) 
				+ " RESET COUNT: " + FoxDecoder.dec(resets)
				+ " UPTIME: " + FoxDecoder.dec(uptime)
				+ " TYPE: " + FoxDecoder.dec(type);
		return s;
		
		//String result = String.format("%s: %02d - %02d percent", name, seconds,dfd);
	}
	
}
