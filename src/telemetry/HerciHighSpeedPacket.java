package telemetry;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import common.Log;
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
	public static final int MAX_PACKET_BYTES = 128; // Since the maximum packet size is 8+30*4, segmentation is never
													// required to ship down a complete minipacket, i.e. the minipackets
													// are always single segments.
	public static final int MAX_PACKET_HEADER_BYTES = 8;  // There are 7 fields in the header across 8 bytes
	public static final int NUMBER_OF_HEADER_FIELDS = 7;
	public int NUMBER_OF_FIELDS = 7; // This is the initial value as it is the header size.  We add the number if minipacket bytes to this later
	
	int[] rawBytes = new int[MAX_PACKET_BYTES];
	int numberOfRawBytes = 0;

	public static final int TYPE_FIELD = 0;
	public static final int LENGTH_FIELD = 1;
	public static final int TIME_FIELD = 2;
	public static final int SEG_FIELD = 3;
	public static final int STATUS_FIELD1 = 4;
	public static final int STATUS_FIELD2 = 5;
	public static final int STATUS_FIELD3 = 6;
	
	HerciHighSpeedPacket(int sat, int r, long u) {
		super(new BitArrayLayout());
		resets = r;
		uptime = u;
		id = sat;
		rawBits = new boolean[MAX_PACKET_HEADER_BYTES*8 + MAX_PACKET_BYTES*8]; 
		
		initFields();
	}

	public HerciHighSpeedPacket(int id, int resets, long uptime, String date, StringTokenizer st) {
		super(new BitArrayLayout());
		this.id = id;
		this.resets = resets;
		this.uptime = uptime;
		this.captureDate = date;
		init();
		rawBits = null; // no binary array when loaded from file, even if the local init creates one
		initFields();
		loadFrom(st,0);
		initPacket();
		loadFrom(st,NUMBER_OF_HEADER_FIELDS);
		
		
	}
	
	protected void loadFrom(StringTokenizer st, int i) {
		String s = null;
		try {
			while(i < NUMBER_OF_FIELDS) {
				if ((s = st.nextToken()) != null) {
				if (s.startsWith("0x")) {
					s = s.replace("0x", "");
					fieldValue[i++] = Integer.valueOf(s,16);
				} else
					fieldValue[i++] = Integer.valueOf(s).intValue();
				}
			}
		} catch (NoSuchElementException e) {
			// we are done and can finish
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.println("ERROR: Too many fields:  Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
		}
	}
	
	@Override
	protected void init() {
		type = TYPE_HERCI_HS_PACKET;
		
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
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
		layout.fieldName[STATUS_FIELD1] = "STATUS1";
		layout.fieldName[STATUS_FIELD2] = "STATUS2";
		layout.fieldName[STATUS_FIELD3] = "STATUS3";
		layout.fieldBitLength[TYPE_FIELD] = 4;	
		layout.fieldBitLength[LENGTH_FIELD] = 12; 
		layout.fieldBitLength[TIME_FIELD] = 16;
		layout.fieldBitLength[SEG_FIELD] = 8;
		layout.fieldBitLength[STATUS_FIELD1] = 8;
		layout.fieldBitLength[STATUS_FIELD2] = 8;
		layout.fieldBitLength[STATUS_FIELD3] = 8;

	}
	public int getType() { return fieldValue[TYPE_FIELD]; }
	public int getLength() { return fieldValue[LENGTH_FIELD]; }
	public int getTime() { return fieldValue[TIME_FIELD]; }
	public int getSeg() { return fieldValue[SEG_FIELD]; }
	public int getStatus1() { return fieldValue[STATUS_FIELD1]; }
	public int getStatus2() { return fieldValue[STATUS_FIELD2]; }
	public int getStatus3() { return fieldValue[STATUS_FIELD3]; }

	/**
	 * Call this once the header is populated and before getting the length
	 */
	public void initPacket() {
		copyBitsToFields();
		// Need to get the length correct, so we cache the values, read the length, then re-init with the correct sixe
		int len = getLength();
		int type = getType();
		int time = getTime();
		int seg = getSeg();
		int status1 = getStatus1();
		int status2 = getStatus2();
		int status3 = getStatus3();
		
		NUMBER_OF_FIELDS = NUMBER_OF_HEADER_FIELDS + getLength()+1;
		initFields();
	
		fieldValue[TYPE_FIELD] = type;
		fieldValue[LENGTH_FIELD] = len;
		fieldValue[TIME_FIELD] = time;
		fieldValue[SEG_FIELD] = seg;
		fieldValue[STATUS_FIELD1] = status1;
		fieldValue[STATUS_FIELD2] = status2;
		fieldValue[STATUS_FIELD3] = status3;
		
		for (int i=NUMBER_OF_HEADER_FIELDS; i< layout.fieldName.length; i++) {
			layout.fieldName[i] = "Byte"+i+"";
			layout.fieldBitLength[i] = 8;
		}
		copyBitsToFields();
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

	public byte[] getMiniPacketBytes() {
		copyBitsToFields();
		byte[] b = new byte[NUMBER_OF_FIELDS-NUMBER_OF_HEADER_FIELDS];
		for (int i=NUMBER_OF_HEADER_FIELDS; i<NUMBER_OF_FIELDS; i++)
			b[i-7] = (byte)fieldValue[i];
		return b;
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

	public static String getTableCreateStmt() {
		String s = new String();
		s = s + "(date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, id int, resets int, uptime bigint, type int, "
		 + "pktType int, "
		 + "length int, "
		 + "truncTime int,"
		 + "segmentation int,"
		 + "st1 int,"
		 + "st2 int,"
		 + "st3 int,";
		 
		for (int i=NUMBER_OF_HEADER_FIELDS; i< MAX_PACKET_BYTES; i++ )
			s = s + "byte" + i + " int NOT NULL DEFAULT 0,";
		s = s + "PRIMARY KEY (id, resets, uptime, type))";
		return s;
	}
	
	public String getInsertStmt() {
		copyBitsToFields();
		String s = new String();
		s = s + " (id, resets, uptime, type, \n";
		s = s + "pktType,\n";
		s = s + "length,\n";
		s = s + "truncTime,\n";
		s = s + "segmentation,\n";
		s = s + "st1,\n";
		s = s + "st2,\n";
		s = s + "st3,\n";
		for (int i=NUMBER_OF_HEADER_FIELDS; i < NUMBER_OF_FIELDS-1; i++ )
			s = s + "byte" + i + " ,\n";
		s = s + "byte" + (NUMBER_OF_FIELDS-1) + " )\n";
		
		s = s + "values (" + this.id + ", " + resets + ", " + uptime + ", " + type + ",\n";
		s = s + fieldValue[TYPE_FIELD]+",\n";
		s = s + fieldValue[LENGTH_FIELD]+",\n";
		s = s + fieldValue[TIME_FIELD]+",\n";
		s = s + fieldValue[SEG_FIELD]+",\n";
		s = s + fieldValue[STATUS_FIELD1]+",\n";
		s = s + fieldValue[STATUS_FIELD2]+",\n";
		s = s + fieldValue[STATUS_FIELD3]+",\n";
		for (int i=NUMBER_OF_HEADER_FIELDS; i< NUMBER_OF_FIELDS-1; i++ )
			s = s + fieldValue[i] + " ,\n";
		s = s + fieldValue[NUMBER_OF_FIELDS-1] + " )\n";
		return s;
	}

}
 