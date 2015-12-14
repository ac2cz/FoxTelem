package telemetry;

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
 *
 *
 */
public class RadiationPacket extends BitArray{

		public int MAX_PACKET_BYTES = 256;
		public int NUMBER_OF_FIELDS = MAX_PACKET_BYTES; // This is set to the maximum, but reset once the type is initialized
		public int reset;
		public long uptime;
		
		int[] rawBytes = new int[MAX_PACKET_BYTES];
		int numberOfRawBytes = 0;
		
		public static final String FILLER = "FILLER";
		public static final String[] radPacketState = {
			"0-???",
			"STANDBY",
			"2-???",
			"ACTIVE",
			"DISABLED",
			"5-???"
		};

		public static final String[] radPacketStateShort = {
			"0",
			"STDBY",
			"2",
			"ACT",
			"DIS",
			"5"
		};
		
		public static final String[] packetType = {
			"0-ASCII",
			"1-TIME",
			"2-REBOOT",
			"3-TELEM",
			"4-STATE",
			"5-EXPERIMENT"
		};
				
		public static final int ASCII = 0;
		public static final int TIME = 1;
		public static final int REBOOT = 2;
		public static final int TELEM = 3;
		public static final int STATE = 4;
		public static final int EXPERIMENT = 5;
		
		public static final int TYPE_FIELD = 0;
		public static final int SEQUENCE_FIELD = 1;
		public static final String[] LepPacketType = {
			"0-UNKNOWN",
			"1-UNKNOWN",
			"2-UNKNOWN",
			"3-START OF EXPOSURE",
			"4-SINGLE EVENT UPSET",
			"5-END OF EXPOSURE"
		};

		public static final int LEP_START = 3;
		public static final int LEP_SINGLE_EVENT = 4;
		public static final int LEP_END = 5;
		
		public static final int LEP_TYPE_FIELD = 2;
		public static final int LEP_SEQUENCED_FIELD = 3;
		
		
		/**
		 * Initialize the packet with the reset and uptime from the payload where the start 0x7E was found
		 * @param r
		 * @param u
		 */
		public RadiationPacket(int r, long u) {
			super(new BitArrayLayout());
			reset = r;
			uptime = u;
			
			rawBits = new boolean[MAX_PACKET_BYTES*8];
			
			layout = new BitArrayLayout(); // initialize a layout
			
			layout.fieldName = new String[NUMBER_OF_FIELDS];
			//fieldValue = new int[NUMBER_OF_FIELDS];
			
			layout.fieldName[TYPE_FIELD] = 	"TYPE";
			layout.fieldName[SEQUENCE_FIELD] = 	"SEQUENCE";
			
			
			fieldValue = new int[layout.fieldName.length];
			
			layout.fieldBitLength = new int[NUMBER_OF_FIELDS];

			layout.fieldBitLength[TYPE_FIELD] = 4;	
			layout.fieldBitLength[SEQUENCE_FIELD] = 12; 
		}
		
		public int getType() { return fieldValue[TYPE_FIELD]; }
		public int getSequence() { return fieldValue[SEQUENCE_FIELD]; }
		
		public String getStringValue(String name, Spacecraft fox) {
			// FIXME - implement
			return null;
		}
		
		public String getTypeString() { 
			if (fieldValue[TYPE_FIELD] >= packetType.length) return "UNKNOWN";
			return packetType[fieldValue[TYPE_FIELD]]; 
			}
		public int getLepType() { return fieldValue[LEP_TYPE_FIELD]; }
		
		/**
		 * Add a byte to the raw byte array
		 * @param b
		 */
		public void addRawByte(int b) {
			rawBytes[numberOfRawBytes++] = b; 
		}
		
		/**
		 * Parse the raw bytes.  If the COBS decode fails then it returns null
		 * or .  In both cases we return false to indicate a
		 * corrupt packet
		 * @return
		 * 
		 */
		public void parseRawBytes() throws CobsDecodeException {
			numberBytesAdded=0;
			
			int[] in = new int[numberOfRawBytes];
			for (int i=0; i< numberOfRawBytes; i++)
				in[i] = rawBytes[i];
			int[] parsed = null;
			try {
				parsed = Cobs.unStuffData(in, MAX_PACKET_BYTES);
			} catch ( ArrayIndexOutOfBoundsException e) {
				throw new CobsDecodeException("ERROR: Packet in reset: " + reset + " uptime: " + uptime + " overflowed. Array out of bounds");
			}
			if (parsed == null) throw new CobsDecodeException("ERROR: Packet in reset: " + reset + " uptime: " + uptime + " - COBS unStuffData returned null");
			
			for (int i=0; i< parsed.length; i++)
				addNext8Bits(parsed[i]);
			
			initType();
			copyBitsToFields();
			
		}
		
		/**
		 * This is called once the first 24 bits are decoded and we know the sequence and type.  This
		 * then sets up the rest of the fields ready for the remainder of the data.
		 */
		public void initType() {
			copyBitsToFields();
		
			if (getType() == TELEM)
				setTypeTELEM();
			else if (getType() == REBOOT)
				setTypeRESTART();
			else if (getType() == TIME)
				setTypeTIME();
			else if (getType() == STATE)
				setTypeSTATE();
			else if (getType() == EXPERIMENT)
				setTypeEXPERIMENT();
			else setTypeASCII();
		}

		public void setTypeASCII() {
			NUMBER_OF_FIELDS = numberOfRawBytes-2; // two used by the header

			for (int i=2; i< NUMBER_OF_FIELDS; i++) {
				layout.fieldName[i] = 	"";
				layout.fieldBitLength[i] = 8;
			}

		}
		public void setTypeRESTART() {
			NUMBER_OF_FIELDS = 4;
			
			layout.fieldName[2] = 	"LOCAL RESTART COUNT";
			layout.fieldName[3] = 	"RCON";  // The RCON registers indicates the type of reboot
			
			layout.fieldBitLength[2] = 16;
			layout.fieldBitLength[3] = 8;
		}

		public void setTypeTIME() {
			NUMBER_OF_FIELDS = 6;
			
			layout.fieldName[2] = 	"REF RESTART COUNT";
			layout.fieldName[3] = 	"REF CLOCK";
			layout.fieldName[4] = 	"LOCAL RESTART COUNT";
			layout.fieldName[5] = 	"LOCAL CLOCK";
			
			layout.fieldBitLength[2] = 16;
			layout.fieldBitLength[3] = 32;
			layout.fieldBitLength[4] = 16;
			layout.fieldBitLength[5] = 32;
		}

		public void setTypeTELEM() {
			// Type 3
			// 40 bytes for VUC
			// 10 bytes for LEP
			// 30 bytes reserved
			
			NUMBER_OF_FIELDS = 26;

			layout.fieldName[2] = 	"VUC UPTIME";
			layout.fieldName[3] = 	"VUC LIVETIME";
			layout.fieldName[4] = 	FILLER;
			layout.fieldName[5] = 	FILLER;
			layout.fieldName[6] = 	"HARD RESETS";
			layout.fieldName[7] = 	"SOFT RESETS";
			layout.fieldName[8] = 	"VUC RUN STATE";
			layout.fieldName[9] = 	FILLER;
			layout.fieldName[10] =	"EXP1 DRIFT";
			layout.fieldName[11] =	"EXP2 DRIFT";
			layout.fieldName[12] =	"EXP3 DRIFT";
			layout.fieldName[13] =	"EXP4 DRIFT";
			layout.fieldName[14] =	"EXP1 POWER";
			layout.fieldName[15] =	"EXP2 POWER";
			layout.fieldName[16] =	"EXP3 POWER";
			layout.fieldName[17] =	"EXP4 POWER";
			layout.fieldName[18] =	"EXP1 STATE";
			layout.fieldName[19] =	"EXP2 STATE";
			layout.fieldName[20] =	"EXP3 STATE";
			layout.fieldName[21] =	"EXP4 STATE";
			layout.fieldName[22] =	"LEP RESTARTS";
			layout.fieldName[23] =	"LEP UPTIME";
			layout.fieldName[24] =	"LEP LIVETIME";
			layout.fieldName[25] =	"LEP TOTAL MEMORY UPSETS";
			
			layout.fieldBitLength[2] = 32;
			layout.fieldBitLength[3] = 32;
			layout.fieldBitLength[4] = 32;
			layout.fieldBitLength[5] = 32;
			layout.fieldBitLength[6] = 16;
			layout.fieldBitLength[7] = 16;
			layout.fieldBitLength[8] = 16;
			layout.fieldBitLength[9] = 16;
			layout.fieldBitLength[10] = 16;
			layout.fieldBitLength[11] = 16;
			layout.fieldBitLength[12] = 16;
			layout.fieldBitLength[13] = 16;
			layout.fieldBitLength[14] = 16;
			layout.fieldBitLength[15] = 16;
			layout.fieldBitLength[16] = 16;
			layout.fieldBitLength[17] = 16;
			layout.fieldBitLength[18] = 16;
			layout.fieldBitLength[19] = 16;
			layout.fieldBitLength[20] = 16;
			layout.fieldBitLength[21] = 16;
			layout.fieldBitLength[22] = 8;
			layout.fieldBitLength[23] = 24;
			layout.fieldBitLength[24] = 32;
			layout.fieldBitLength[25] = 32;
			
		}

		public void setTypeSTATE() {
			NUMBER_OF_FIELDS = 5;
			
			layout.fieldName[2] = 	"LOCAL CLOCK";
			layout.fieldName[3] = 	"EXPERIMENT";  // 1 = LEP
			layout.fieldName[4] = 	"STATE";
			
			layout.fieldBitLength[2] = 32;
			layout.fieldBitLength[3] = 8;
			layout.fieldBitLength[4] = 8;
		}

		public void setTypeEXPERIMENT() {
			NUMBER_OF_FIELDS = 4;
			
			layout.fieldName[LEP_TYPE_FIELD] = 	"LEP TYPE";
			layout.fieldName[LEP_SEQUENCED_FIELD] = 	"SEQUENCE";
			
			layout.fieldBitLength[LEP_TYPE_FIELD] = 4;
			layout.fieldBitLength[LEP_SEQUENCED_FIELD] = 12;

			initLepType();
			copyBitsToFields();
		}

		public void initLepType() {
			copyBitsToFields();
			
			if (getLepType() == LEP_START)
				setTypeLEP_START();
			else if (getLepType() == LEP_SINGLE_EVENT)
				setTypeLEP_SINGLE_EVENT();
			else if (getLepType() == LEP_END)
				setTypeLEP_END();
			else setTypeUNKNOWNLep();
		}

		public void setTypeUNKNOWNLep() {
			NUMBER_OF_FIELDS = numberOfRawBytes; // two used by the header

			for (int i=4; i< NUMBER_OF_FIELDS; i++) {
				layout.fieldName[i] = 	"";
				layout.fieldBitLength[i] = 8;
			}

		}

		public void setTypeLEP_START() {
			NUMBER_OF_FIELDS = 5;
			
			layout.fieldName[4] = 	"LOCAL CLOCK START"; 
			layout.fieldBitLength[4] = 32;
			
		}

		public void setTypeLEP_SINGLE_EVENT() {
			NUMBER_OF_FIELDS = 6;
			
			layout.fieldName[4] = 	"ADDRESS"; 
			layout.fieldName[5] = 	"DATA"; 

			layout.fieldBitLength[4] = 32;
			layout.fieldBitLength[5] = 16;
			
		}

		public void setTypeLEP_END() {
			NUMBER_OF_FIELDS = 8;
			
			layout.fieldName[4] = 	"LOCAL CLOCK END"; 
			layout.fieldName[5] = 	"CURRENT"; 
			layout.fieldName[6] = 	"S0";
			//layout.fieldName[7] = 	"MEMORIES";
			layout.fieldName[7] = "UPSETS";
			
			layout.fieldBitLength[4] = 32;
			layout.fieldBitLength[5] = 8;
			layout.fieldBitLength[6] = 8;
			//layout.fieldBitLength[7] = 1;
			layout.fieldBitLength[7] = 16;
		}

		/**
		 * We have bytes in big endian order, so we need to add the bits in a way
		 * that makes sense when we retrieve them sequentially
		 * So we add the msb first.  Then when 12 bits pulled in a row it will make sense.
		 * Note that if we pull a subset of 8 bits, then we have to be careful of the order.
		 * @param b
		 */
		public void addNext8Bits(int b) {
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

		/**
		 * Get the string represetnation of a field in this framePart.  Run any conversion
		 * routine assigned to this field
		 * @param name
		 * @return
		 */
		public String getStringValue(String name) {
			int pos = -1;
			for (int i=0; i < layout.fieldName.length; i++) {
				if (name.equalsIgnoreCase(layout.fieldName[i]))
					pos = i;
			}
			String s = "-----";
			// Special Formatting
			if (pos == -1) 
				;//System.err.println("ERROR: No Index for Field:" + name);
			int value = getRawValue(name);
				if (value == 0)
					;//s = "--";
				else
					s = ""+value; 
			if (name == "EXPERIMENT") {
				if (value == 0)
					s = "VUC";
				else
					s = "LEP";
			}
			
			if (name == "STATE") {
				if (value < radPacketState.length)
					s = radPacketState[value];
				
				else
					s = "???";
				
			}
			
			if (s.length() < 5)
				for (int k=0; k < (5 - s.length()); k++)
					s = " " + s;
			return s;
		}
		
		
		public String toString() {
			String bytes = new String();
			bytes = bytes + layout.fieldName[0] + ": "+ getTypeString() + " ";
			bytes = bytes + layout.fieldName[1] + ": "+ (fieldValue[1]) + " ";
			bytes = bytes + " " + getDataString();
			return bytes;
		}
		public String getDataString() {
			String bytes = new String();
			int max = NUMBER_OF_FIELDS;
			for (int b=2; b < max; b++) {
				if (layout.fieldName[b] != FILLER)
					bytes = bytes + layout.fieldName[b] + ": "+ (fieldValue[b]) + " ";
				if (b % 8 == 0) bytes = bytes + "\n";
			}
			return bytes;
		}

		public double convertRawValue(String name, int rawValue,
				int conversion, Spacecraft fox) {
			// TODO Auto-generated method stub
			return 0;
		}
}
