package telemetry.uw;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FramePart;

public class CanPacket extends FoxFramePart implements Comparable<FramePart> {
	public static final int MAX_PACKET_BYTES = 12;
	
	public static final int ID_FIELD = 0; 
	public static final int ID_BYTES = 4;
	public int pkt_id = 0;
	
	int canPacketId = 0;
	int length = 0;
	
	byte[] bytes = new byte[MAX_PACKET_BYTES]; // keep a copy so we can sent to the COSMOS server
		
	public CanPacket(BitArrayLayout lay) {
		super(TYPE_UW_CAN_PACKET, lay);
	}
	
	public CanPacket(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_UW_CAN_PACKET, date, st, lay);
	}

	public CanPacket(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_UW_CAN_PACKET, lay);
	}

	public void initBytes() {
		for (int i=0; i < getLength(); i++) {
			layout.fieldName[i+1] = 	"BYTE" + i;		
			layout.fieldBitLength[i+1] = 8;
		}
	}

	public void setType(int t) {
		type = t;
	}
	
	@Override
	public void init() {
	}

	public int getLength() {
		return length;
	}
	public int getID() {
		if (fieldValue == null) return 0;
		return fieldValue[ID_FIELD];
	}
	
	@Override
	public int compareTo(FramePart p) {
		if (pkt_id == ((CanPacket)p).pkt_id)
			return 0;
		if (pkt_id < ((CanPacket)p).pkt_id)
			return -1;
		if (pkt_id > ((CanPacket)p).pkt_id)
			return +1;
		return +1;
	}
	@Override
	/** True if we have a valid length, id and have received all the bytes */
	public boolean isValid() {
		if (rawBits == null) return true;
		if (numberBytesAdded < ID_BYTES) return false;  // If rawBits not null this is being created, otherwise we are loading from file or DB.
		copyBitsToFields();
		if (fieldValue[ID_FIELD] != 0)
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
			if (fieldValue[ID_FIELD] == 0)
				return true;
		return false;
	}
	
	public void addNext8Bits(byte b) {
		super.addNext8Bits(b);
		bytes[numberBytesAdded-1] = b;
		if (numberBytesAdded == ID_BYTES) {
			copyBitsToFields();
			initBytes();
		}
	}
	
	public static int getIdfromRawID(int canPacketId) {
		int id = canPacketId & 0x1FFFFFFF; // 3 MSB are the Length, so ignore that and take the rest as the id
		return id;
	}
	
	public static int getLengthfromRawID(int canPacketId) {
		int length = (canPacketId >> 29) & 0x7; // We want just the 3 MSB as the length
		return length + 1;
	}
	
	public void copyBitsToFields() {
		resetBitPosition();
		super.copyBitsToFields();
		canPacketId = fieldValue[ID_FIELD];
		length = getLengthfromRawID(canPacketId);
		canPacketId = getIdfromRawID(canPacketId);
	}
	
		@Override
	public String toString() {
		copyBitsToFields();
		String s = "CAN ID:";
		s = s + FoxDecoder.hex(canPacketId);
		s = s + " Len:" + getLength() + " Type:" + type;
		for (int i=0; i<getLength(); i++)
			s = s + " " + FoxDecoder.hex(fieldValue[i+1]);
		return s;
	}
	
	public PcanPacket getPCanPacket() {
		copyBitsToFields();
		if (!isValid()) return null;
		byte[] data = new byte[getLength()];
		for (int i=0; i<getLength(); i++)
			data[i] = (byte) fieldValue[i+1]; // skips the id field
		
		PcanPacket pcan = new PcanPacket(captureDate, id, resets, uptime, type, canPacketId, (byte)getLength(), data);
		return pcan;
	}
	
	public String getTableCreateStmt() {
		String s = new String();
		s = s + "(captureDate varchar(14), id int, resets int, uptime bigint, type int, ";
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
