package decoder.KiwiSat;

import decoder.FoxBitStream;
import decoder.FoxDecoder;
import decoder.LookupException;

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
 * 
 * Static variables and methods to encode and decode 8N1 line encoding
 * 
 *
 */
public class CodeHDLC {
	
	// Both frame markers are the same because HDLC is NRZ-I, so the sense of the bits does not matter, only the transitions matter
	public static final int NOT_FRAME = /* 07e */ 0x7e & 0xff;
	public static final int FRAME = /* 07e */ 0x7e & 0xff;
	/**
	 * Given a 10 bit word, return the 8 bit value
	 * @param word - the 10 bit word
	 * @return - the value returned 
	 * @throws LookupException 
	 * 
	 */
	public static byte decode(int word, boolean flip) throws LookupException {
		if (flip) word = ~word & 0x3ff;
		// Is the start bit 0
		int start = 0x1 & word;
		//if (start != 0) throw new LookupException();

		// Is the stop bit 1
		int stop = 0x200 & word;
		// if (stop != 1) throw new LookupException();

		int i = word >> 1;
		i = i & 0xFF; // Take the first 8 bit remaining
		return (byte)i; // return the middle 8 bits
	}

	
}
