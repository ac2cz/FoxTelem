package telemetry;

import java.util.StringTokenizer;

import common.Spacecraft;
import decoder.BitStream;

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
public class RadiationTelemetry extends FramePart {

	public static final int MAX_RAD_TELEM_BYTES = 20;
	public int NUMBER_OF_FIELDS = MAX_RAD_TELEM_BYTES;
	public int reset;
	public long uptime;

	public RadiationTelemetry(int r, long u, BitArrayLayout l) {
		super(l);
		reset = r;
		uptime = u;
		
		
	}

	public RadiationTelemetry(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}

	@Override
	protected void init() {
		rawBits = new boolean[MAX_RAD_TELEM_BYTES*8];
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}

	public String getStringValue(String name, Spacecraft fox) {
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				pos = i;
		}
		String s = "-----";
		// Special Formatting
		if (pos == -1) 
			;//System.err.println("ERROR: No Index for Field:" + name);
		else if (layout.conversion[pos] == BitArrayLayout.CONVERT_STATUS_BIT) {
			int value = getRawValue(name);
			try {
				s = RadiationPacket.radPacketState[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				s = "???";
			}
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_16_SEC_UPTIME) {
				int value = getRawValue(name);
				s = Integer.toString(value * 16);
		} else s =  Integer.toString(getRawValue(name));

		return s;
	}

	public double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox ) {
		
		//	System.out.println("BitArrayLayout.CONVERT_ng: " + name + " raw: " + rawValue + " CONV: " + conversion);
			switch (conversion) {
			case BitArrayLayout.CONVERT_NONE:
				return rawValue;
			case BitArrayLayout.CONVERT_INTEGER:
				return rawValue;
			case BitArrayLayout.CONVERT_STATUS_BIT:
				return rawValue;
			case BitArrayLayout.CONVERT_16_SEC_UPTIME:
				return rawValue*16;
			}
			return FramePart.ERROR_VALUE;

	}
	/**
	 * We have bytes in big endian order, so we need to add the bits in a way
	 * that makes sense when we retrieve them sequentially
	 * The spacecraft sends the lsb first, so we flip that and add the msb first.  
	 * Then when 12 bits pulled in a row it will make sense.
	 * Note that if we pull a subset of 8 bits, then we have to be careful of the order.
	 * @param b
	 */
	public void addNext8Bits(int b) {
//		super.addNext8Bits((byte)b);

		for (int i=0; i<8; i++) {
			if ((b >> i & 1) == 1) 
				rawBits[7-i+numberBytesAdded*8] = true;
			else 
				rawBits[7-i+numberBytesAdded*8] = false;
		}
		numberBytesAdded++;	
	}

	
	/**
	 * Return the next n bits of the raw bit array, converted into an integer
	 * We get them sequentially, with the msb first, so they just go into the 
	 * array in order
	 * @param n
	 * @return
	*/
	protected int nextbits(int n ) {
		int field = 0;
		
		boolean[] b = new boolean[n];
		for (int i=0; i < n; i++) {
			b[i] = rawBits[bitPosition+i];
			
		}
		bitPosition = bitPosition + n;
		field = BitStream.binToInt(b);
		return field;
		
	}

	public String toDataString(Spacecraft fox) {
		copyBitsToFields();
		String s = new String();
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + getStringValue(layout.fieldName[i], fox)+" ";
		
		}
		return s;
	}
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "RADIATION TELEMETRY:\n";
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i]+"\n";
		
		}
		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
