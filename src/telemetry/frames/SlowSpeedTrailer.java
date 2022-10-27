package telemetry.frames;

import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FramePart;

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
public class SlowSpeedTrailer extends FramePart {
	public static final int MAX_BYTES = 32;

	int[] fecBytes = new int[MAX_BYTES];
	
	SlowSpeedTrailer() {
		super (TYPE_SLOW_SPEED_HEADER, new BitArrayLayout());
	}
	
	@Override
	public void copyBitsToFields() {	
		resetBitPosition();
		for (int i =0; i< MAX_BYTES; i++)
			fecBytes[i] = nextbits(8);	
	}
	
	protected void init() { }
	
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "FEC CHECk BYTES:\n";
		for (int i =0; i< MAX_BYTES; i++) {
			s = s + FoxDecoder.hex(fecBytes[i]) + " ";
			// Too tired to write the algorithm for row separators.... later....
			if (i==7 || i==15|| i==23 || i==31 || i==39 || i==47 || i==55) s = s + "\n";
		}
		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
}
