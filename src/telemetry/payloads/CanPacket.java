package telemetry.payloads;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FramePart;

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
 * This is a CAN Packet payload.  The data in CAN packets is big endian except
 * the first 4 bytes which come fro the IHU.  They are little endian.  To keep
 * our sanity the first 4 bytes are specified individually in the layout. Once
 * parsed the result is cached in ihuPacketId.  The decoded can ID is stored in 
 * canId and the length in length
 * 
 */
public class CanPacket extends FramePart implements Comparable<FramePart> {
	public static final int MAX_PACKET_BYTES = 12;
	
	public static final int ID_FIELD0 = 0; 
	public static final int ID_FIELD1 = 1; 
	public static final int HEADER_ID_BYTES = 2; // Length and Id
	public int pkt_id = 0; // this is a unique id we use for streaming.  This is not the canId
	
	public int canId;
	int length = 0;
	
	byte[] bytes; // keep a copy so we can sent to the COSMOS server
		
	public CanPacket(BitArrayLayout lay) {
		super(TYPE_CAN_PACKET, lay);
	}
	
	public CanPacket(BitArrayLayout lay, int id, long uptime, int resets) {
		super(TYPE_CAN_PACKET,lay);
		captureHeaderInfo(id, uptime, resets);
	}
	
	public CanPacket(int id, int resets, long uptime, String date, byte[] data, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_CAN_PACKET, date, data, lay);
	}
	
	public CanPacket(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_CAN_PACKET, date, st, lay);
	}

	public CanPacket(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_CAN_PACKET, lay);
	}

	public void init() {
		bytes = new byte[getMaxBytes()];
	}
	
	public void initBytes() {
		// set the layout again based on the CAN ID
//		layout = Config.satManager.getLayoutByCanId(id, canPacketId);
//		for (int i=0; i < getLength(); i++) {
//			layout.fieldName[i+1] = 	"BYTE" + i;		
//			layout.fieldBitLength[i+1] = 8;
//		}
	}

	public void setType(int t) {
		type = t;
	}

	public int getLength() {
		return length;
	}
	public int getID() {
		return canId;
	}
	
	public byte[] getBytes() {
		if (isValid()) {
			byte[] data = new byte[getLength() + HEADER_ID_BYTES];
			if (bytes.length < getLength() + HEADER_ID_BYTES) return null;
			for (int i=0; i < getLength() + HEADER_ID_BYTES; i++)
				data[i] = bytes[i];
			return data;
		}
		return null;
	}
	

	@Override
	/** True if we have a valid length, id and have received all the bytes */
	public boolean isValid() {
		if (rawBits == null) return true;
		if (numberBytesAdded < HEADER_ID_BYTES) return false;  // If rawBits not null this is being created, otherwise we are loading from file or DB.
		copyBitsToFields();
		if (canId != 0)
			if (numberBytesAdded == getLength() + HEADER_ID_BYTES)
				return true;
		return false;
	}

	/**
	 * True if we have added at least 4 bytes and the ID is zero
	 * @return
	 */
	public boolean hasEndOfCanPacketsId() {
		// First do an integrity check
		if (numberBytesAdded >= getMaxBytes())
			if (!isValid())
				return true; // this is corrupt, trigger end of can packets
		if (numberBytesAdded >= HEADER_ID_BYTES)
			if (canId == 0)
				return true;
		return false;
	}
	
	public void addNext8Bits(byte b) {
		try {
			//super.addNext8Bits(b);
			for (int i=0; i<8; i++) {
				if ((b >> i & 1) == 1) 
					rawBits[7-i+numberBytesAdded*8] = true;
				else 
					rawBits[7-i+numberBytesAdded*8] = false;
			}
			bytes[numberBytesAdded] = b;
			numberBytesAdded++;	
			if (numberBytesAdded == HEADER_ID_BYTES) { // id and length added
				copyBitsToFields();
			}
			
		} catch (ArrayIndexOutOfBoundsException e) {
			if (Config.debugFrames)
				Log.errorDialog("ERROR", "Error adding data to Can Packet:\n" + layout.name + " layout length is: " + (layout.getMaxNumberOfBytes()-4)
					+ " data len from packet is: " + getLength() + "\n Error at byte: "+numberBytesAdded 
					+ "\nTurn off Debug Frames to prevent this message in future.");
			Log.println("ERROR: adding data to Can Packet:\n" + layout.name + " layout length is: " + (layout.getMaxNumberOfBytes()-4)
					+ " data len from packet is: " + getLength() + "\n Error at byte: "+numberBytesAdded);
			// This packet is corrupt and perhaps the rest of the payload
			// trigger end of can packets
			length = 0;
			fieldValue[ID_FIELD0] = 0;
			fieldValue[ID_FIELD1] = 0;
			canId = 0; 
			return;
		}
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
	
	public static int getRawIdFromRawBytes(int a, int b) {
		int id = a + 256*b;  // little endian
		return id;
	}

	public static int getIdfromRawID(int canPacketId) {
		int id = (canPacketId >> 4 )& 0x0FFF; // 4 LSB are the Length, we want the 12 MSBs
		return id;
	}
	
	public static int getLengthfromRawID(int canPacketId) {
		int length = (canPacketId& 0xF); // We want just the 4 LSB as the length
		return length;
	}
	
	/**
	 * The first 4 bytes are from FOX and are little endian with MSB first.
	 * The rest of the bytes are from the experiment and may be in network byte order ie Big endian.  Yes really...
	 * This suggest these should be in different classes, one wrapping the other.  Perhaps with the experiment data as a secondary payload
	 * with the ID, Length and data in big endian.
	 */
	public void copyBitsToFields() {
		resetBitPosition();
		super.copyBitsToFields();
		int rawId = getRawIdFromRawBytes(fieldValue[ID_FIELD0], fieldValue[ID_FIELD1]);
		canId = getIdfromRawID(rawId); // 12 bit  little endian field
		length = getLengthfromRawID(rawId);
	}
	
		@Override
	public String toString() {
		copyBitsToFields();
		String s = "";
		s = s + resets + ":"+uptime;
		s = s + "CAN ID:";
		s = s + FoxDecoder.hex(canId);
		s = s + " " + canId;
//		Spacecraft fox = Config.satManager.getSpacecraft(getFoxId());
		s = s + " Len:" + getLength();
		s = s + " Type:" + type;
		for (int i=1; i<fieldValue.length; i++)
			s = s + " " + FoxDecoder.hex(fieldValue[i]);
		return s;
	}
	
	public String getTableCreateStmt(boolean storeMode) {
		String s = new String();
		s = s + "(captureDate varchar(14), id int, resets int, uptime bigint, type int, ";
		if (storeMode)
			s = s + "newMode int,";
		
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + " int,\n";
		}
		// We use serial for the type, except for type 4 where we use it for the payload number.  This allows us to have
		// multiple high speed records with the same reset and uptime
		s = s + "date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,";
		s = s + "PRIMARY KEY (id, resets, uptime, type))";
		return s;
	}
	
//	public static void main(String args[]) {
//		System.out.println("Can Id test");
//		int[] bytes = {0x12,0x80};
//		
//		int len = getLengthfromRawID(rawid);
//		int id = getIdfromRawID(rawid);
//		
//		System.out.println(Integer.toHexString(id) + " " + Integer.toHexString(len));
//		
//	}
	
}
