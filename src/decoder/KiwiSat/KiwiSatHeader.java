package decoder.KiwiSat;

import common.Spacecraft;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.Header;
import telemetry.SlowSpeedFrame;

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
 */
public class KiwiSatHeader extends Header {
	
	String address;
	int control;
	int pid;
	
	protected KiwiSatHeader() {
		super();
		MAX_BYTES = KiwiSatFrame.MAX_HEADER_SIZE;
		rawBits = new boolean[MAX_BYTES*8];
		id = Spacecraft.KIWI_SAT;
		resets = 0; // THIS IS A FOX CONCEPT AND LIKELY FIXED AT ZERO FOR NON FOX SPACECRAFT
	}
	
	protected void init() { }

	@Override
	public void copyBitsToFields() {
		// THE HEADER DOES NOT HAVE A LAYOUT FILE, SO WE SPECIFY THE BIT POSITIONS HERE
		resetBitPosition();
		char ch = 0;
		for (int i=0; i<14; i++) {
			ch = (char) nextbits(8);
			address = address + ch;
		}
		control = nextbits(8);
		pid = nextbits(8);
		
		
	}

	// THIS CONTAINS DEBUG INFORMATION THAT GOES TO THE LOG OR CONSOLE IF WE WANT
	public String toString() {
		copyBitsToFields();
		String s = new String();
		
		s = s + "AMSAT-NL KiwiSat Telemetry Captured at: " + reportDate() + "\n" 
				+ "ID: " + FoxDecoder.dec(id) 
				+ " ADDRESS: " + address
				+ " CONTROL: " + control
				+ " PID: " + pid;
		return s;
	}

	@Override
	public boolean isValid() {
		// ADD ANY LOGIC THAT CAN BE USED TO DETERMINE IF THIS IS A VALID HEADER, BEYOND THE FRAME DELIMITER.  WE USE THIS WHEN
		// WE FIND A DOUBLE LENGTH FRAME OR A TRAILING FRAME MARKER TO SEE IF WE MISSED A FRAME MARKER AT THE START
		
		
		return false;
	}

	

}
