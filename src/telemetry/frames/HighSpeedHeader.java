package telemetry.frames;

import common.Config;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;

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

public class HighSpeedHeader extends Header {
	
	@Deprecated
	public HighSpeedHeader() {
		super(TYPE_HIGH_SPEED_HEADER, new BitArrayLayout());
		MAX_BYTES = HighSpeedFrame.MAX_HEADER_SIZE;
		rawBits = new boolean[MAX_BYTES*8];
	}

	
		

	// FIXME this is poor, must be a better way to determine a high speed header is valid ....
	// Perhaps this works if we have determined if any of the words are corrupted too
	public boolean isValid() {
		copyBitsToFields();
		if ((Config.satManager.validFoxId(id)) )
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();

		s = s + "ID: " + FoxDecoder.dec(id) 
				+ " RESET COUNT: " + FoxDecoder.dec(resets)
				+ " UPTIME: " + FoxDecoder.dec(uptime);
		
		return s;
	}




}
