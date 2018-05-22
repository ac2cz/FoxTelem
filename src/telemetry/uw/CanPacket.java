package telemetry.uw;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import decoder.FoxBitStream;
import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;

public class CanPacket extends FoxFramePart {
	public static int MAX_PACKET_BYTES = 12;
	public int NUMBER_OF_FIELDS = 10; // This is set to the maximum, but reset once the type is initialized
	
	public static final int ID_FIELD = 0; 
	public static final int ID_BYTES = 4;
	
	int canPacketId = 0;
	int length = 0;
	
	
	public CanPacket(BitArrayLayout lay) {
		super(lay);
	}
	
	public CanPacket(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);
	}

	public CanPacket(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, lay);
	}

	public void initBytes() {
		NUMBER_OF_FIELDS = getLength();
		for (int i=0; i < getLength(); i++) {
			layout.fieldName[i+1] = 	"BYTE" + i;		
			layout.fieldBitLength[i+1] = 8;
		}
	}

	public void setType(int t) {
		type = t;
	}
	
	@Override
	protected void init() {
		// This is called from the parent constructor so the below values are not yet initialized.  So set them up here.  Leave at top of class just as documentation
		NUMBER_OF_FIELDS = 10;
		MAX_PACKET_BYTES = 12;
		rawBits = new boolean[MAX_PACKET_BYTES*8];
		fieldValue = new int[NUMBER_OF_FIELDS];
	}

	public int getLength() {
		return length;
	}
	public int getID() {
		if (fieldValue == null) return 0;
		return fieldValue[ID_FIELD];
	}
	@Override
	/** True if we have a valid length, id and have received all the bytes */
	public boolean isValid() {
		if (numberBytesAdded < ID_BYTES) return false;
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
		if (numberBytesAdded == ID_BYTES) {
			copyBitsToFields();
			initBytes();
		}
	}
	
	public static int getIdfromRawID(int canPacketId) {
		int id = canPacketId << 3;
		return id;
	}
	
	public static int getLengthfromRawID(int canPacketId) {
		int length = (canPacketId >> 29) & 0x7;
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
	
	
}
