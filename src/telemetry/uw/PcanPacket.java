package telemetry.uw;

import java.util.Date;
import decoder.Decoder;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
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
 * This class holds the data format needed to send CAN Packets to COSMOS. It emulates the PCAN message 
 * format of the PCAN Gateway
 *
 */
public class PcanPacket {

	private final short length = 0x0024; //36
	private final short messageType = 0x0080;
//	private byte[] tag = new byte[8]; // not used. Will populate with fox header as it is 8 bytes too
//	private int timestampLow;
//	private int timestampHigh;
	private static final byte CHANNEL = 0x00;
	private byte dataLengthCount;
	private static final short FLAGS = 0x0000;
	private int canId;
	private byte[] canData = new byte[8];
	
	private int resets;
	private long uptime;
	private int type;
	@SuppressWarnings("unused") // not currently used, but might be
	private int foxId;
	private Date createDate;
	
	public PcanPacket(Date createDate, int foxid, int resets, long uptime, int type, int id, byte len, byte[] data) {
		canId = id;
		this.foxId = foxid;
		this.resets = resets;
		this.uptime = uptime;
		this.type = type;
		this.createDate = createDate;
		dataLengthCount = len;
		for (int i=0; i<data.length; i++) 
			canData[i] = data[i];
	}
	
//	private Date parseDate(String strDate) {
//		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
//		Date date = null;
//		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//		try {
//			date = dateFormat.parse(strDate);
//		} catch (ParseException e) {
//					// We don't do anything in this case, the date will be null
//					date = null;
//		}
//
//		return date;
//	}
	
	/**
	 * Get a set of bytes in Big Endian order that conform to the PCAN layout
	 * @return
	 */
	public byte[] getBytes() {
		byte[] buffer = new byte[length];
		
		buffer[0] = (byte)((length >> 8) & 0xff);
		buffer[1] = (byte)(length & 0xff);

		buffer[2] = (byte)((messageType >> 8) & 0xff);
		buffer[3] = (byte)(messageType & 0xff);

		// Pack the FOX Timestamp info into the TAG field
		buffer[4] = (byte)((resets >> 8) & 0xff);
		buffer[5] = (byte)(resets & 0xff);
		byte[] uptimeBytes = Decoder.bigEndian4(uptime);
		for (int j=0; j<4; j++) 
			buffer[6+j] = uptimeBytes[j];
		buffer[10] = (byte)((type >> 8) & 0xff);
		buffer[11] = (byte)(type & 0xff);
		
		//for (int i=0; i<tag.length; i++) 
		//	buffer[4+i] = tag[i];
				
		//Date dt = parseDate(createDate);
		long date_long = createDate.getTime();
		byte[] timeBytes = Decoder.bigEndian8(date_long); 
		
		// For some reason the spec wants the 4 low bytes of the timestamp first, even though it specifies big endian order overall
		// We ignore that and send 8 bytes in big endian order.
		for (int j=0; j<8; j++) 
			buffer[12+j] = timeBytes[j];
	
		buffer[20] = CHANNEL;
		
		buffer[21] = dataLengthCount;

		buffer[22] = (byte)((FLAGS >> 8) & 0xff);
		buffer[23] = (byte)(FLAGS & 0xff);

		byte[] id = Decoder.bigEndian4(canId);
		for (int k=0; k<4; k++) 
			buffer[24+k] = id[k];
		
		for (int k=0; k<canData.length; k++) 
			buffer[28+k] = canData[k];
		
		return buffer;
	}
	
	public String toString() {
		String s = "";
		byte[] buffer = getBytes();
		for (byte b : buffer)
			s = s + String.format("%02x", b) + " ";
		return s;
	}
}
