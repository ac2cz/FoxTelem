package telemetry.conversion;

import common.Spacecraft;
import telemetry.FramePart;

/**
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2022 amsat.org
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
 * This class implements String formatting of conversions.  It should
 * be the last conversion in a list if there is a pipeline of conversions because
 * it only implements the calculateString() method.
 */
public class ConversionFormat extends Conversion {

	public String type;
	int modifier = 0;
	
	public ConversionFormat(String name, Spacecraft fox) {
		super(name, fox);
	
		name.trim();
		type = name;
		
		String stem3 = "";
		if (name.length() >=3) {
			stem3 = name.substring(0, 3); 
			if (stem3.equalsIgnoreCase(Conversion.FMT_HEX)) {
				type = Conversion.FMT_HEX;
				modifier = getIndex(name,3);
			}
			else if (stem3.equalsIgnoreCase(Conversion.FMT_BIN)) {
				type = Conversion.FMT_BIN;
				modifier = getIndex(name,3);
			} else if (name.length() >=5) {
				if (name.substring(0, 5).equalsIgnoreCase(Conversion.FMT_F) ) {
					type = name;
				}
			} 
		}	
	}


	private int getIndex(String name, int offset) {
		int modifier = 0;
		String index3 = name.substring(offset); // all characters after the stem
		try {
			modifier = Integer.parseInt(index3);
		} catch (NumberFormatException e) { modifier=1; };
		return modifier;
	}
	

	@Override
	public double calculate(double x) {
		return x;
	}

	@Override
	public String calculateString(double x) {
		String s = "";
		
		switch (type) {
			case Conversion.FMT_INT:
				s = Long.toString((long) x);
				return s;
				
			case Conversion.FMT_F:
			case Conversion.FMT_1F:
				s = String.format("%2.1f", x);
				return s;
			case Conversion.FMT_2F:
				s = String.format("%1.2f", x);
				return s;
			case Conversion.FMT_3F:
				s = String.format("%1.3f", x);
				return s;
			case Conversion.FMT_4F:
				s = String.format("%1.4f", x);
				return s;
			case Conversion.FMT_5F:
				s = String.format("%1.5f", x);
				return s;
			case Conversion.FMT_6F:
				s = String.format("%1.6f", x);
				return s;
			case Conversion.FMT_HEX:
				s = FramePart.toHexString((long)x,modifier);
				return s;
			case Conversion.FMT_BIN:
				s = FramePart.intToBin((int)x,modifier);
				return s;
		} 
		return null;
	}

}
