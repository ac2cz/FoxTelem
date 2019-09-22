package telemetry.uw;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import common.Config;
import common.Log;
import common.Spacecraft;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
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
public class CanPacket extends FoxFramePart implements Comparable<FramePart> {
	public static final int MAX_PACKET_BYTES = 12;
	
	public static final int ID_FIELD0 = 0; 
	public static final int ID_FIELD1 = 1; 
	public static final int ID_FIELD2 = 2; 
	public static final int ID_FIELD3 = 3; 
	public static final int ID_BYTES = 4;
	public int pkt_id = 0; // this is a unique id we use for streaming.  This is not the canId
	
	public int ihuPacketId = 0;
	public int canId;
	int length = 0;
	
	byte[] bytes; // keep a copy so we can sent to the COSMOS server
		
	public CanPacket(BitArrayLayout lay) {
		super(TYPE_UW_CAN_PACKET, lay);
	}
	
	public CanPacket(int id, int resets, long uptime, String date, byte[] data, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_UW_CAN_PACKET, date, data, lay);
	}
	
	public CanPacket(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_UW_CAN_PACKET, date, st, lay);
	}

	public CanPacket(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_UW_CAN_PACKET, lay);
	}

	public void init() {
		bytes = new byte[MAX_PACKET_BYTES];
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
		int len = getLength();
		if (isValid()) {
			byte[] data = new byte[getLength() + ID_BYTES];
			if (bytes.length < getLength() + ID_BYTES) return null;
			for (int i=0; i < getLength() + ID_BYTES; i++)
				data[i] = bytes[i];
			return data;
		}
		return null;
	}
	

	@Override
	/** True if we have a valid length, id and have received all the bytes */
	public boolean isValid() {
		if (rawBits == null) return true;
		if (numberBytesAdded < ID_BYTES) return false;  // If rawBits not null this is being created, otherwise we are loading from file or DB.
		copyBitsToFields();
		if (ihuPacketId != 0)
			if (getLength() > 0 && getLength() < 9)
				if (numberBytesAdded == getLength() + ID_BYTES)
					return true;
		return false;
	}

	/**
	 * True if we have added at least 4 bytes and the ID is zero
	 * @return
	 */
	public boolean hasEndOfCanPacketsId() {
		if (numberBytesAdded >= ID_BYTES)
			if (ihuPacketId == 0)
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
			numberBytesAdded++;	
		bytes[numberBytesAdded-1] = b;
		if (numberBytesAdded == ID_BYTES) {
			copyBitsToFields();
		}
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.errorDialog("FATAL ERROR", "Error adding data to layout:\n" + layout.name + " layout length: " + (layout.getMaxNumberOfBytes()-4)
					+ " data len: " + getLength() + "\n at byte: "+numberBytesAdded );
			System.exit(1);
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
	
	
	public static int getIdFromRawBytes(int a, int b, int c, int d) {
		int id = a + 256*b + 65536*c + 16777216*d;  // little endian
		return getIdfromRawID(id);
	}

	public static int getLengthFromRawBytes(int a, int b, int c, int d) {
		int id = a + 256*b + 65536*c + 16777216*d;  // little endian
		return getLengthfromRawID(id);
	}

	
	protected static int getIdfromRawID(int canPacketId) {
		int id = canPacketId & 0x1FFFFFFF; // 3 MSB are the Length, so ignore that and take the rest as the id
		return id;
	}
	
	protected static int getLengthfromRawID(int canPacketId) {
		int length = (canPacketId >> 29) & 0x7; // We want just the 3 MSB as the length
		return length + 1;
	}
	
	/**
	 * The first 4 bytes are from FOX and are little endian with MSB first.
	 * The rest of the bytes are from the experiment and are network byte order ie Big endian.  Yes really...
	 * This suggest these should be in different classes, one wrapping the other.  Perhaps with the experiment data as a secondary payload
	 * with the ID, Length and data in big endian.
	 */
	public void copyBitsToFields() {
		resetBitPosition();
		super.copyBitsToFields();
		ihuPacketId = fieldValue[ID_FIELD0] + 256*fieldValue[ID_FIELD1] + 65536*fieldValue[ID_FIELD2] + 16777216*fieldValue[ID_FIELD3];  // little endian
		length = getLengthfromRawID(ihuPacketId);
		canId = getIdfromRawID(ihuPacketId);
	}
	
		@Override
	public String toString() {
		copyBitsToFields();
		String s = "";
		s = s + resets + ":"+uptime;
		s = s + "CAN ID:";
		s = s + FoxDecoder.hex(canId);
		s = s + " " + canId;
		Spacecraft fox = Config.satManager.getSpacecraft(getFoxId());
		s = s + ": " + fox.canFrames.getNameByCanId(canId);
		s = s + ": " + fox.canFrames.getGroundByCanId(canId);
		s = s + " - " + fox.canFrames.getSenderByCanId(canId);
		s = s + " Len:" + getLength() + " Type:" + type;
		for (int i=1; i<fieldValue.length; i++)
			s = s + " " + FoxDecoder.hex(fieldValue[i]);
		return s;
	}
	
	public PcanPacket getPCanPacket() {
		copyBitsToFields();
		if (!isValid()) return null;
		byte[] data = new byte[getLength()];
		for (int i=0; i<getLength(); i++)
			data[i] = (byte) fieldValue[i+this.ID_BYTES]; // skips the id fields
		
		PcanPacket pcan = new PcanPacket(reportDate, id, resets, uptime, type, canId, (byte)getLength(), data);
		return pcan;
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
	
}
