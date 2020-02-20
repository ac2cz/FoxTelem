package telemetry;

import java.util.StringTokenizer;

import common.FoxSpacecraft;
import common.Spacecraft;
import decoder.FoxBitStream;
import decoder.FoxDecoder;

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
 *
 * The Layout is defined in the spacecraft directory.  This could be either VULCAN TELEMETRY or HERCI Housekeeping
 * 
 * HERCI FORMAT FOLLOWS:
 * 103-60106

Rev 0	March 2015
		Initial Release
Rev 1	01 MAY 2015
		Removed the fixed IEB bytes replacing them
		with Peak Count Rate.  Fields shuffled
		down to keep the count fields adjacent
REv 2   21 Oct 2015
                Minor update indicating V201 capabilities.
---------------------------------------------------

Updates to the housekeeping formats
used with the FLEXI processor system

Assumtions:
    Use the JUNO/WAVES software with
minimal changes.  Remove IP/UDP/CIP
headers from the data.

Changes (from JUNO world):
    Change in header (but this should be
handled in the telemetry formatting routine)

Data Formats
------------

Transfer Frame:
         +0      +1      
        +-------+-------+
     +0 |  CRC CCITT-16 |
        |    Seed = 0   |
        +-------+-------+
     +2 |    Sequence   |
        |   monotonic   |
        +-------+-------+
     +4 |  System Time  |
        | D31..D16 sec  |
        +-------+-------+
     +6 |  System Time  |
        | D15..D1 D0=TQF|
        +-------+-------+
     +8 |  Epoch Number | <- FOX1 refers to this field
        |    1..32767   |    as the reset number
        +-------+-------+
    +10 |     Length    |
        |   Always 58   |
        +-------+-------+
 +0 +12 | Static  HSK   |
        | Counts/Second | <- detector sanity check
        +-------+-------+    (fun for students)
 +2 +14 | Static  HSK   |
        |   Peak Rate   | <- detector sanity check
        +-------+-------+    (fun for students)
 +4 +16 | Static  HSK   |
        | Science PKT C | <- unsigned
        +-------+-------+
 +6 +18 | Static  HSK   |
        | Command CNT 1 | <- counts commands from IHU
        +-------+-------+
 +9 +20 | Static  HSK   |
        | Command CNT 2 | <- counts all commands
        +-------+-------+
+10 +22 | Static  HSK   |
	| Command CNT 3 | <- validation errors
        +-------+-------+
+12 +24 | Static  HSK   |
        | ANA 0 | ANA 1 |
        +-------+-------+
    +26 | Dynamic  HSK  | <- micro packet
        | Type  |  Seq  |
        +-------+-------+
    +28 | Dynamic  HSK  |
	| Trunc Seconds |
        +-------+-------+
    +30 |   Data        |
        .   (12 bytes)  |
        .               |
        +-------+-------+
    +42 | Dynamic  HSK  | <- micro packet
        | Type  |  Seq  |
        +-------+-------+
    +44 | Dynamic  HSK  |
	| Trunc Seconds |
        +-------+-------+
    +46 |   Data        |
        .   (12 bytes)  |
        .               |
        +-------+-------+
    +58
  Synchronization Pattern: 
      (does not appear in housekeeping)
      Standard 32bit synchronization pattern.
      Always has FAF33403

  CRC CCITT-16:
                                      16    12    5
      The CCITT CRC 16 polynomial is X   + X   + X  + 1.
      Seed value is ZERO for the WAVES/FLEXI/HERCI implementation.
      (Sequence field is always non-zero so the CRC is
      effective for our purposes).

  Sequence:
      There are two fields in this 16 bit word, a source indicateor in
        the upper two bits and a monotonically increasing sequence
        number in the lower 14 bits.
      D15..D14 is the source field
          00        PANIC/FAIL FSW didn't load
          01        HRS	(HERCI soes not make use of HRS telemetry)
          10        LRS   (Science Telemetry)
          11        HSK   (Housekeeping)
      D13..D0 is the sequence (monotonically increasing)

  System Time:
      Seconds from some arbritrary Epoch as a 32 bit integer.
      Bit 0 of time is relaced with a "time quality flag" that
      is set to a value of '1' to indicate that time may be
      suspect (in other words, there hasn't been a recent
      time update message from the host spacecraft).

  Epoch Number:
      This field identifies the epoch for the associated
      time field.  In the FOX1 environment, this is the
      number of times the host system has been reset.

  Length:
      This field is the overall record length. 
      It is fixed at 58 for housekeeping.

  Static Housekeeping:
      16b  average count rate
      16b  peak count rate
      16b  science packet count
      16b  command count, channel
      16b  command count, universal
      16b  command count, errors
       8b  temp 1 (DPU)
       8b  temp 2 (Detector)


Dynamic Housekeeping
--------------------

  Dynamic Housekeeping
      Also referred to as micropackets, these are small
      housekeeping packets from sub-systems within the
      instrument.  Format is logically similar to the
      science telemetry minipackets.

        +-------+-------+
     +0 |  SRC  |  TYP  |
        |       |       |
        +-------+-------+
     +1 |    Sequence   |
        |   monotonic   |
        +-------+-------+
     +2 |   RTI Time    |
        |    D15..D8    |
        +-------+-------+
     +3 |   RTI Time    |
        |     D7..D0    |
        +-------+-------+
     +4 |   Status      |
      . |      Data     |
      . +---- . . . ----+
      . |               |
    +15 |               |
        +-------+-------+

  SRC/TYP:
      Source & Type field (4 bits/4 bits).
      SRC indicating whick task generated the data
        micropacket.
      TYP each of 15 housekeeping sources can have 15 
        packet types the source task to generate 
        multiple data types.

          0xF? POST Status
              This micro packet occurs in the first few
              housekeeping records to show the status of
              POST.
          0xE? Command Status
              This micro packet occurs when processing 
              degenerate commands (to load flash memory)
          0x?? IEB reporting.
              These micro packets occur as a result of an
              IEB trigger command.

  Seq:
      8 bit sequence number, monotonically increasing.  This
      is used to discard/ignore duplicate micropackets as micro-
      packet traffic is not multiplexed at the housekeeping
      frame rate (it is somewhat slower).

  Truncated Time:
      The timetag is placed when the associated data is collected.
      It is decoded in a manner similar to the science data minipackets,
      but it holds the lower 16 bits of the system time field (thus
      precision limited to the second level).  Rollover is about
      18 hours which limits the useful lifetime of the data in the
      micropacket.


Dynamic Housekeeping Packet Types
---------------------------------
As of V201 software release (10/2015), only the F1 and F2 type 
micropackets are emitted by the instrument.

Source	
Field	
------	------------------------------
0x1x	CLOCK MANAGEMENT
  12	Clock Status, DPLL status
  14	Clock Status, Command Arrival Time and Anomaly Time

0xCx	IEB Handler (Command MACRO expansion)
  C1	IEB Handler, Trigger
  C2	IEB Handler, Label
	    The IRB Subsystem micropackets are generated as a result of
        commanding the IEB subsystem and by commands within an IEB
	sequence.  When generated, micropackets are sent to the 
	housekeeping process for transport through the housekeeping 
	channel to the ground.  

0xEx	Command Subsystem
  E1	Command Hardware Status Frame
  E2	Command Counters
	    The Command Subsystem micropackets are generated 
        occasionally and are sent to the housekeeping process for 
        transport through the housekeeping channel to the ground.  

0xFx	HOUSEKEEPING
  F1	Kernel Version Strings
          +0  +1  +2  +3  +4  +5  +6  +7  +8  +9 +10 +11 +12 +13 +14 +15
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        |xF1|Seq|TimeTag| KERNL Version | Build Time: YYDDD | THSK Vers |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        "KERNL Version" is the version string in the O/S
	"Build Time" is the year and day the O/S was built
	"THSK Vers" is the version string from the housekeeping Task
	    The version string micropacket is placed in the housekeeping
        buffer when the system is initially loaded into memory.  Subsequent
        micropacket traffic will discard this record and it is never
        presented again.

  F2	POST Status
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        |xF2|Seq|TimeTag|       |       |       |       |       |       |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
	    The POST Status micropacket is placed in the housekeeping
        buffer when the system is initially loaded into memory.  Subsequent
        micropacket traffic will discard this record and it is never
        presented again.

  F9	Additional Housekeeping, Command Counts
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        |xF9|Seq|TimeTag|       |       |       |C_CMD_w|T1x Cnt|T11|T12|
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
	"C_CMD_w"
	"T1x Cnt"
	"T11"
	"T12"
	    The Command Counts micropacket is generated every 30 minutes 
	and sent to the housekeeping process for transport through the 
	housekeeping channel to the ground.  

  FA	Additional Housekeeping, SPI Counts
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        |xF9|Seq|TimeTag| WRCnt |       | CmdEr | TimOT |       |       |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
	"WRCnt" write data (to DATA device)
	"CmdEr"	Invalid function code
	"TimOT" write polling timout
	    The SPI Counts micropacket is generated every 30 minutes and
        sent to the housekeeping process for transport through the
        housekeeping channel to the ground.


  FB	Additional Housekeeping, Science Counts
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
        |xF9|Seq|TimeTag|       |       |       |       |       |       |
        +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
	""
	    The Science Counts micropacket is generated every 30 minutes 
        and sent to the housekeeping process for transport through the
        housekeeping channel to the ground.

 */
public class RadiationTelemetry extends FoxFramePart {

	public static final int TELEM_BYTES = 20;
	public static final int MAX_HERCI_HK_DATA_LENGTH = 46;
	public static final int MAX_RAD_TELEM_BYTES = 58;
	//public int NUMBER_OF_FIELDS = MAX_RAD_TELEM_BYTES;
	boolean blockCopyBits = false;
	
	public static final String[] herciSource = {
			"PANIC",
			"HRS",
			"LRS",
			"HSK"
		};
	public static final String[] herciMicroPktTyp = {
			"??",
			"VERSION",
			"POST"
		};
	public static final String[] herciMicroPktSource = {
			"??", //0
			"CLOCK?", //1
			"??", //0
			"??", //0
			"??", //0
			"??", //0
			"??", //0
			"??", //0
			"??", //0
			"??", //0
			"??", //A
			"??", //B
			"IEB", //C
			"??", //D
			"COMMAND", //E - 14
			"HOUSEKEEPING" //F - 15
		};
	public RadiationTelemetry(int r, long u, BitArrayLayout l) {
		super(TYPE_RAD_TELEM_DATA, l);
		resets = r;
		uptime = u;
		
		
	}

	public RadiationTelemetry(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_RAD_TELEM_DATA, date, st, lay);	
	}

	@Override
	protected void init() {
		type = TYPE_RAD_TELEM_DATA;
		//rawBits = new boolean[MAX_RAD_TELEM_BYTES*8];
		//fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}

	public String getStringValue(String name, Spacecraft fox) {
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				pos = i;
		}
		String s = "-----";
		int conv = layout.getIntConversionByPos(pos);
		// Special Formatting
		if (pos == -1) 
			;//System.err.println("ERROR: No Index for Field:" + name);
		else if (conv == BitArrayLayout.CONVERT_VULCAN_STATUS) {
			int value = getRawValue(name);
			try {
				s = RadiationPacket.radPacketState[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				s = "???";
			}
		} else if (conv == BitArrayLayout.CONVERT_16_SEC_UPTIME) {
				int value = getRawValue(name);
				s = Integer.toString(value * 16);
		} else if (conv == BitArrayLayout.CONVERT_HERCI_SOURCE) {
			int value = getRawValue(name);
			try {
				s = herciSource[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				s = "???";
			}
		} else if (conv == BitArrayLayout.CONVERT_HERCI_HEX) {
			int value = getRawValue(name);
			s="";
			for (int i=0; i<2; i++) {
				s = FoxDecoder.plainhex(value & 0xff) + " " + s; // we get the least sig byte each time, so new bytes go on the front
				value = value >> 8 ;
			}
		} else if (conv == BitArrayLayout.CONVERT_HERCI_MICRO_PKT_HEX) {
			int value = getRawValue(name);

			s = FoxDecoder.plainhex(value & 0xff);
			for (int i=2; i<=12; i++) {
				value = getRawValue(name+i);
				s = s+ " " + FoxDecoder.plainhex(value & 0xff); // we get the least sig byte each time, so new bytes go on the front
				value = value >> 8 ;
			}
			s= s + " \"";
			value = getRawValue(name);
			s = s+ (char) (value & 0xff);
			for (int i=2; i<=12; i++) {
				value = getRawValue(name+i);
				s = s+ (char) (value & 0xff);
				value = value >> 8 ;
			}
			s=s+"\"";
		} else if (conv == BitArrayLayout.CONVERT_HERCI_MICRO_PKT_TYP) {
			int value = getRawValue(name);
			try {
				s = herciMicroPktTyp[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				s = "???";
			}
		} else if (conv == BitArrayLayout.CONVERT_HERCI_MICRO_PKT_SOURCE) {
			int value = getRawValue(name);
			try {
				s = herciMicroPktSource[value];
			} catch (ArrayIndexOutOfBoundsException e) {
				s = "???";
			}
		} else s =  super.getStringValue(name, fox); //Integer.toString(getRawValue(name));

		return s;
	}

	public double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox ) {
		
		//	System.out.println("BitArrayLayout.CONVERT_ng: " + name + " raw: " + rawValue + " CONV: " + conversion);
			switch (conversion) {
			case BitArrayLayout.CONVERT_VULCAN_STATUS:
				return rawValue;
			case BitArrayLayout.CONVERT_16_SEC_UPTIME:
				return rawValue*16;
			case BitArrayLayout.CONVERT_HERCI_SOURCE:
				return rawValue;
			}
			return super.convertRawValue(name, rawValue, conversion, fox);

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
		field = FoxBitStream.binToInt(b);
		return field;
		
	}

	public String toDataString(FoxSpacecraft fox) {
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
	
	public void copyBitsToFields() {
		if (blockCopyBits) return;
		super.copyBitsToFields();
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
