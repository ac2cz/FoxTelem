package telemetry;

import common.Spacecraft;
import decoder.FoxBitStream;

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
 * This is a bit array that is constructed by added bytes. The resulting bits are then unpacked and mapped to a set of fields.
 * The fields are named in fieldName and have lengths defined in fieldBitLength
 * 
 * The actual bit lengths and field names are described in the BitArrayLayout.  This is initialized separately and
 * can be loaded from a file
 * 
 *
 */
public abstract class BitArray {

	protected static final double ERROR_VALUE = 9999;
	protected static final String PAD = "pad";

	public boolean[] rawBits = null;
	protected int bitPosition = 0; // position in the raw bits as we allocate them to the fields
	//public String[] fieldName = null;
	public int[] fieldValue = null;
	//protected String[] fieldUnits = null;
	//protected int[] fieldBitLength = null;
	//public String[] description = null;
	protected BitArrayLayout layout;
	
	public int numberBytesAdded = 0;

	public void resetBitPosition() {
		bitPosition = 0;
	}
	
//	public String[] getFieldNames() { return fieldName; }
	public int[] getFieldValues() { return fieldValue; }

	protected BitArray(BitArrayLayout l) {
		layout = l;
	}
	
	public boolean hasFieldName(String name) {
		return layout.hasFieldName(name);
	}
	
	public int getConversionByName(String name) {
		return layout.getConversionByName(name);
		
	}
	
	public String getUnitsByName(String name) {
		return layout.getUnitsByName(name);
		
	}

	public abstract String getStringValue(String name, Spacecraft fox);
	/**
	 * Given a downloaded byte, add it to the raw bits array
	 * Store the least significant bit first, even though the satellite sends the msb first.
	 * This compensates for the little endian nature of the satellite.  It means that the lsb or
	 * the least significant byte is the first value we come across.
	 * @param b
	 */
	public void addNext8Bits(byte b) {
		for (int i=0; i<8; i++) {
			if ((b >> i & 1) == 1) 
				rawBits[i+numberBytesAdded*8] = true;
			else 
				rawBits[i+numberBytesAdded*8] = false;
		}
		numberBytesAdded++;	
	}

	/**
	 *  Copy all of the bits from the raw byte frame to the fields according to the bit pattern in the fieldBitLength array
	 *  
	 */
	public void copyBitsToFields() {
		if (rawBits != null) { // only convert if we actually have a raw binary array.  Otherwise this was loaded from a file and we do not want to convert
			resetBitPosition();
			for (int i=0; i < layout.fieldName.length; i++) {
				if (layout.fieldName[i].startsWith(PAD)) {  // ignore pad values and set the results to zero
					nextbits(layout.fieldBitLength[i]);
					fieldValue[i] = 0;
				} else
					fieldValue[i] = nextbits(layout.fieldBitLength[i]);
			}
		}
	}

	/**
	 * Return the next n bits of the raw bit array, converted into an integer
	 * @param n
	 * @return
	 */
	protected int nextbits(int n ) {
		int field = 0;
		
		boolean[] b = new boolean[n];
		for (int i=0; i < n; i++) {
			b[i] = rawBits[bitPosition+n-i-1];
			
		}
		bitPosition = bitPosition + n;
		field = FoxBitStream.binToInt(b);
		return field;
		
	}

	/**
	 * Return the raw integer value of this field, specified by its name.  Used to store in the
	 * file and to display to the user when "raw" values are chosen.  This would be the actual DAC
	 * value sent down from the spacecraft, for example.
	 * @param name
	 * @return
	 */
	public int getRawValue(String name) {
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				return fieldValue[i];
		}
		return -1;
	}
	
	/**
	 * Return the value of this field, specified by its name.  Run any conversion routine
	 * to BitArrayLayout.CONVERT_this into the appropriate units.
	 * Used to plot graphs
	 * @param name
	 * @return
	 */
	public double getDoubleValue(String name, Spacecraft fox) {
		
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				pos = i;
		}

		if (pos != -1) {
			int value = fieldValue[pos];
			double result = convertRawValue(name, value, layout.conversion[pos], fox);
			return result;
		}
		return ERROR_VALUE;
	}

	public abstract double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox );	
	
	
}
