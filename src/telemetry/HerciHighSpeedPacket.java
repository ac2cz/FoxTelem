package telemetry;

import common.Spacecraft;
import decoder.BitStream;
import decoder.Decoder;
/*
 * 
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
 *
 *
 * Minipacket:
  In spite of living in the Z80 world of LSB first,
the HEADERS in the science minipacket are stored
MSB first when size exceeds 8 bits.  The comment
about Big-Endian above all applies here as well.

         +0              +1
        +-------+-------+-------+-------+
     +0 | Type  |         Length        |
        +-------+-------+-------+-------+
     +2 |    Truncated Time (ticks)     |
        +-------+-------+-------+-------+
     +4 | Segmentation  |     Status    |
        +-------+-------+-------+-------+
     +6 |             Status            |
        +-------+-------+-------+-------+
     +8 |   Data
        .    
        .    
        .    
        +-------+-------+-------+-------+

  Type:
      Type field indicating where the data was generated.
      HERCI will produce a limited set of types:

      0000    Fill Data, length will be zero.  No more
              minipacket data appears in the transfer frame
              after the fill data. 

  Length:
      Number of bytes that FOLLOW the STATUS bytes minuz one.
      A minipacket with 4096 bytes if data will have a length 
      of 4095 (0x0FFF),

  Truncated Time:
      The timetag is placed at the begining of a data collection
      cycle or buffer of data.  The value is calcualted by masking
      the system time seconds field, shifting it to the left and 
      adding in the sub-seconds.  Assuming a 25mS tick rate, the
      following calculation would be used:
          TRUNC_TIME = (SYSTEM_TIME_32 & 0x000003FF)*40 + RTI
              TRUNC_TIME is the field in the minipacket
              SYSTEM_TIME_32 is the 32 bit seconds field
              RTI is the current sub-seconds, which increments 
                  every 25mSec, counting from 0 to 39.

  Segmentation:
      D7      Data Quality bit.
              Set to indicate data is of questionable quality.
      D6..D5  MSF; More Status Follows.
              These bits indicate the length of the status
              field.  These bits are combined with the length
              field to determine total minipacket length.
              00    Header is  8 octets.  Overall length is 'Length' + 7
              01    Header is 10 octets.  Overall length is 'Length' + 9
              10    Header is 12 octets.  Overall length is 'Length' + 11
              11    Header is 16 octets.  Overall length is 'Length' + 15
      D4      EOF indicator.  This bit is set in the last
                  segment of data packet.  Unsegmented data
                  packets will have this bit set. 
      D3..D0  Segment Number.  Allows for up to 16 4K segments
                  in a unique data packet.  Note that the
                  Truncated Time field will be identical in
                  all segments of a data packet.

  Status:
              3, 5, 7 or 11 'Type'-specific status octets.
              Each subsystem within the FLEXI system will produce
              a unique 'Type' and status bit assignment.

 */
public class HerciHighSpeedPacket extends FramePart {
	public static int MAX_PACKET_BYTES = 128; // FIXME-not sure what the max value is
	public static int MAX_PACKET_HEADER_BYTES = 8;
	public int NUMBER_OF_FIELDS = 5; // This is the initial value as it is the header size
	public int reset;
	public long uptime;
	public int id;
	
	int[] rawBytes = new int[MAX_PACKET_BYTES];
	int numberOfRawBytes = 0;

	public static final int TYPE_FIELD = 0;
	public static final int LENGTH_FIELD = 1;
	public static final int TIME_FIELD = 2;
	public static final int SEG_FIELD = 3;
	public static final int STATUS_FIELD = 4;
	
	HerciHighSpeedPacket(int sat, int r, long u) {
		super(new BitArrayLayout());
		reset = r;
		uptime = u;
		id = sat;
		rawBits = new boolean[MAX_PACKET_HEADER_BYTES*8 + MAX_PACKET_BYTES*8]; 
		
		initFields();
	}

	@Override
	protected void init() {
		// TODO Auto-generated method stub
		
	}

	public void initFields() {
		layout = new BitArrayLayout(); // initialize a layout
		layout.fieldName = new String[NUMBER_OF_FIELDS];
		//fieldValue = new int[NUMBER_OF_FIELDS];
		fieldValue = new int[layout.fieldName.length];
		layout.fieldBitLength = new int[NUMBER_OF_FIELDS];
		
		layout.fieldName[TYPE_FIELD] = 	"TYPE";
		layout.fieldName[LENGTH_FIELD] = "LENGTH";
		layout.fieldName[TIME_FIELD] = 	"TIME";
		layout.fieldName[SEG_FIELD] = "SEG";
		layout.fieldName[STATUS_FIELD] = "STATUS";
		layout.fieldBitLength[TYPE_FIELD] = 4;	
		layout.fieldBitLength[LENGTH_FIELD] = 12; 
		layout.fieldBitLength[TIME_FIELD] = 16;
		layout.fieldBitLength[SEG_FIELD] = 8;
		layout.fieldBitLength[STATUS_FIELD] = 24;

	}
	public int getType() { return fieldValue[TYPE_FIELD]; }
	public int getLength() { return fieldValue[LENGTH_FIELD]; }
	public int getTime() { return fieldValue[TIME_FIELD]; }
	public int getSeg() { return fieldValue[SEG_FIELD]; }
	public int getStatus() { return fieldValue[STATUS_FIELD]; }

	/**
	 * Call this once the header is populated and before getting the length
	 */
	public void initPacket() {
		copyBitsToFields();
		// Need to get the length correct
		int len = getLength();
		int type = getType();
		int time = getTime();
		int seg = getSeg();
		int status = getStatus();
		
		NUMBER_OF_FIELDS = 5 + getLength();
		initFields();
		
		fieldValue[TYPE_FIELD] = type;
		fieldValue[LENGTH_FIELD] = len;
		fieldValue[TIME_FIELD] = time;
		fieldValue[SEG_FIELD] = seg;
		fieldValue[STATUS_FIELD] = status;
		
		for (int i=5; i< layout.fieldName.length; i++) {
			layout.fieldName[i] = "Byte"+i+"";
			layout.fieldBitLength[i] = 8;
		}
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

	@Override
	public String getStringValue(String name, Spacecraft fox) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double convertRawValue(String name, int rawValue, int conversion,
			Spacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "HERCI Science Mini Packet: " + MAX_PACKET_HEADER_BYTES+getLength() + " bytes\n";
		for (int i =0; i< MAX_PACKET_HEADER_BYTES; i++) {
			s = s + Decoder.hex(fieldValue[i]) + " ";
			// Print 32 bytes in a row
			if ((i+1)%32 == 0) s = s + "\n";
		}
		return s;
	}


	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}


}
