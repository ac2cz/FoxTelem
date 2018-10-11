package telemetry;

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
public abstract class Header extends FoxFramePart {
	//int id = 0; // unsigned 3 bit int
	//int resetCount = 0; // unsigned 16 bit integer
	//long uptime = 0;  // unsigned 25 bit integer, max value 33554431
	
	public abstract String toString();

	protected Header(int type) {
		super(type, new BitArrayLayout());
	}
	
	protected void init() { }
	
	/**
	 * Override the getMaxBytes in FoxFramePart because it uses the layout and the Header has no layout.
	 */
	public int getMaxBytes() {
		return MAX_BYTES;
	}

	@Override
	public void copyBitsToFields() {
		resetBitPosition();
		id = nextbits(3);
		resets = nextbits(16);
		uptime = nextbits(25);
		
	}

	

}
