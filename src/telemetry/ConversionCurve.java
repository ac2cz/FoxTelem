/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2019 amsat.org
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
 * This class holds a conversion.  It could be a curve, a lookup table or a static conversion
 * 
 *
 */
package telemetry;

public class ConversionCurve extends Conversion {

	public static final int CSF_FILE_ROW_LENGTH = 8;

	public double a, b, c, d, e, f; // if this is a curve we store the coefficients
	private String description;
	
	public ConversionCurve(String[] values) {
		if (values == null) throw new IllegalArgumentException("Conversion File row null");
		if (values.length != CSF_FILE_ROW_LENGTH) throw new IllegalArgumentException("Conversion File row has wrong number of values: " + values.length);
		name = values[0];
		try {
			a = Double.parseDouble(values[1]);
			b = Double.parseDouble(values[2]);
			c = Double.parseDouble(values[3]);
			d = Double.parseDouble(values[4]);
			e = Double.parseDouble(values[5]);
			f = Double.parseDouble(values[6]);
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException(name + ": Coeeficients are not all parsable numbers");
		}
		description = values[7];
	}
	
	// Given an input value calculate the result
	public double calculate(double x) {
		double y = f*Math.pow(x, 5) 
				+ e*Math.pow(x,4)
				+ d*Math.pow(x,3)
				+ c*Math.pow(x,2)
				+ b*x 
				+ a;
		return y;
	}
	
	public String toString() {
		String s = "";
		s = s + name + ": " + a + " + " + b + "x + " + c + "x^2 + " + d + "x^3 + " + e + "x^4 + " + f + "x^5 + ";
		return s;
	}
}
