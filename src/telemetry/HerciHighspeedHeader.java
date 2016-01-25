package telemetry;

import java.util.StringTokenizer;

import common.Spacecraft;
import decoder.BitStream;
import decoder.Decoder;

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
 * This is a SECONDARY payload that is decoded from the primary PayloadHERCIhighSpeed
 *
 *Transfer Frame:
         +0      +1      +2      +3
        +-------+-------+-------+-------+
     +0 |    Synchronization Pattern    |
        |   FA  |   F3  |   34  |   03  |
        +-------+-------+-------+-------+
     +4 |  CRC CCITT-16 |    Sequence   |
        |    Seed = 0   |   monotonic   |
        +-------+-------+-------+-------+
     +8 |         System Time           |
        | D31..D1 seconds, D0 qual Flag |
        +-------+-------+-------+-------+
    +12 |  Epoch Number | Record Length |
        |    1..32767   | from begining |
        +-------+-------+-------+-------+
    +16 |   Science minipackets
        .    see minipacket document
        .    for individual record
        .    formats
        |  
        +-------+-------+-------+-------+

  Synchronization Pattern:
      Standard 32bit synchronizatioin pattern.
      Always has FAF33403.
      This allows the HERCI/FLEXI telemetry to
      be treated as a simply byte-stream.  No
      additional byte-level framing is required
      to process the data.

  CRC CCITT-16:
                                      16    12    5
      The CCITT CRC 16 polynomial is X   + X   + X  + 1.
      Seed value is ZERO for the WAVES/FLEXI/HERCI implementation.
      (Sequence field is always non-zero so the CRC is
      effective for our purposes).

  Sequence: (D15..D8 are in column +2, D7..D0 are in column +3)
      Two fields in this 16 bit area.
      D15..D14 is a source field
          00        PANIC/FAILED (FSW didn't load)
          01        HRS	(HERCI soes not make use of HRS telemetry)
          10        LRS   (Science Telemetry)
          11        HSK   (Housekeeping)
      D13..D0 is a monotonically increasing frame count.

  System Time: (D31..D24 is at offset 0 and D7..D0 are at offset 3)
      Seconds from some arbritrary Epoch as a 32 bit integer.
      Bit 0 of time is replaced with a "time quality flag" that
      is set to a value of '1' to indicate that time may be
      suspect (in other words, there hasn't been a recent
      time update message from the host spacecraft).

  Epoch Number: (D15..D8 is at offset 0 and D7..D0 are at offset 1)
      This field identifies the epoch for the associated
      time field.  In the FOX1 environment, this is the
      number of times the host system has been reset.

  Record Length: (D15..D8 is at offset 2 and D7..D0 are at offset 3)
      This is the number of octets in the transfer frame.
      Length includes from the synch pattern through to 
      the last data in the buffer including any fill bytes.

  Science minipackets:
      Individual science packets are concatenated in this
      area.  Header fields are all identical to allow 
      consistent packet extraction but the format of each
      type is unique.  Lengths also vary with each type
      of data.
 */
public class HerciHighspeedHeader extends FramePart {

	public static final int MAX_RAD_TELEM_BYTES = 16;
	public int NUMBER_OF_FIELDS = 8;
	public int reset;
	public long uptime;

	public static final String[] herciSource = {
		"PANIC",
		"HRS",
		"LRS",
		"HSK"
	};

	public HerciHighspeedHeader(int r, long u, BitArrayLayout l) {
		super(l);
		reset = r;
		uptime = u;
	}

	public HerciHighspeedHeader(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
	}

	@Override
	protected void init() {
		type = TYPE_HERCI_SCIENCE_HEADER;
		
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
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
		else if (layout.conversion[pos] == BitArrayLayout.CONVERT_HERCI_SOURCE) {
			int value = getRawValue(name);
			try {
				s = herciSource[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				s = "???";
			}
		} else if (layout.conversion[pos] == BitArrayLayout.CONVERT_HERCI_HEX) {
			s="";
			int value = getRawValue(name);
			for (int i=0; i<4; i++) {
				s = " " + Decoder.plainhex(value & 0xff) + s; // we get the least sig byte each time, so new bytes go on the front
				value = value >> 8 ;
			}
		} else s =  super.getStringValue(name, fox);

		return s;
	}

	public double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox ) {
		
		//	System.out.println("BitArrayLayout.CONVERT_ng: " + name + " raw: " + rawValue + " CONV: " + conversion);
			switch (conversion) {
			case BitArrayLayout.CONVERT_HERCI_HEX:
				return rawValue;
			}
			return super.convertRawValue(name, rawValue, conversion, fox);

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
		s = s + "HERCI HS SCIENCE HEADER:\n";
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
