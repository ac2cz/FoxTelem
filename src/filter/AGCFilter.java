package filter;

import javax.sound.sampled.AudioFormat;

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
public class AGCFilter extends Filter {

	
	public AGCFilter(AudioFormat af, int size) {
		super(af, size);
	}
	@Override
	public double filterDouble(double abBuffer) {
		// We don't change the data, we just want it to be amplified
		
		return abBuffer;
	}

	@Override
	protected int getFilterLength() {
		return 0;
	}
	@Override
	public double[] getKernal() {
		return null;
	}

}
