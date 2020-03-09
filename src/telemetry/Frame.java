package telemetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import telemServer.ServerConfig;
import telemServer.StpFileProcessException;
import telemServer.StpFileRsDecodeException;
import telemetry.FoxBPSK.FoxBPSKFrame;
import measure.PassMeasurement;
import measure.RtMeasurement;
import common.Config;
import common.Log;
import common.Sequence;
import common.Spacecraft;
import common.FoxSpacecraft;
import decoder.HighSpeedBitStream;
import decoder.FoxBPSK.FoxBPSKBitStream;
import fec.RsCodeWord;

/**
 * FOX 1 Telemetry Decoder
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 *         Copyright (C) 2015 amsat.org
 *
 *         This program is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or (at
 *         your option) any later version.
 *
 *         This program is distributed in the hope that it will be useful, but
 *         WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *         General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *         The base class for telemetry frames. Slow speed and high speed frames
 *         are derived from this. A frame contains FramePart objects. The
 *         Headers, Payloads and Trailers are derived from the FrameParts
 * 
 *         The raw bits are stored in the frameparts. This class holds the raw
 *         bytes and can save them to disk if needed. This is used to send to
 *         the amsat server.
 * 
 *         Frames are created when they are decoded from the satellite. The
 *         payloads are stored in the payloadStore for analysis by graphs or
 *         other means. The raw frames are stored in the rawFrameQueue until
 *         they have been sent to the amsat server. The queue is persisted on
 *         disk in case the program is shut down. Frames are then sent when the
 *         program is restarted.
 * 
 *
 */
public abstract class Frame implements Comparable<Frame> {

	public static final DateFormat stpDateFormat = new SimpleDateFormat(
			"E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);

	protected Header header = null;
	protected FoxSpacecraft fox; // the satellite that we are decoding a frame for, populated
					// once the header is filled

	// TODO - These should really be looked up from the formats loaded in Config.satManager
	public static final int DUV_FRAME = 0; 
	public static final int HIGH_SPEED_FRAME = 1;
	public static final int PSK_FRAME = 2;
	public static final int GOLF_BPSK_FRAME = 3;
	
	// TODO - These should be looked up from the formats loaded in Config.satManager.  They are frame_length * 8
	public static final int DUV_FRAME_LEN = 768; 
	public static final int HIGH_SPEED_FRAME_LEN = 42176;
	public static final int PSK_FRAME_LEN = 4576;


	public static final String SEQUENCE_FILE_NAME = "seqno.dat";
	public static final String NONE = "NONE";
	public boolean corrupt = false;;
	public int foxId = 0;
	public String receiver = NONE; // unique name (usually callsign) chosen by
									// the user. May vary over life of program
									// usage, so stored
	private String frequency = NONE; // frequency when this frame received
	private String source; // The frame source subsystem
	private String length; // The frame length in bytes
	public String rx_location = NONE; // the lat, long and altitude
	public String receiver_rf = NONE; // human description of the receiver
	public String demodulator; // will contain Config.VERSION
	protected Date stpDate;
	public long sequenceNumber = Sequence.ERROR_NUMBER;

	private String measuredTCA = NONE; // time of TCA
	private String measuredTCAfrequency = NONE; // frequency if this frame was
												// just after TCA

	int numberBytesAdded = 0;
	protected byte[] bytes;

	// Store a reference to any measurements that were made at the same time as
	// the Frame was downloaded, so we can pass them on to the server
	@SuppressWarnings("unused") // this is used
	private RtMeasurement rtMeasurement;
	@SuppressWarnings("unused") // this is used
	private PassMeasurement passMeasurement;
	
	public int rsErrors; // set by the decoder when this frame is decoded
	public int rsErasures;

	/**
	 * This creates a new frame, freshly decoded from the satellite, and
	 * snapshots all of the STP header paramaters
	 */
	public Frame() {
		// Snapshot the paramaters that are saved along with this frame
		// Note that we do NOT have the foxId until the frame is decoded
		// So we can not set the source. While we could set the length, we
		// choose to set that when we processes the raw bytes.

		try {
			sequenceNumber = Config.sequence.getNextSequence();
		} catch (IOException e) {
			Log.errorDialog("ERROR", e.getMessage());
			e.printStackTrace();
		}

		receiver = Config.callsign;
		// frequency = "";
		rx_location = formatLatitude(Config.latitude) + " "
				+ formatLongitude(Config.longitude);
		if (notNone(Config.altitude))
			rx_location = rx_location + " " + Config.altitude;
		receiver_rf = Config.stationDetails;
		String os = System.getProperty("os.name").toLowerCase();
		demodulator = "FoxTelem " + Config.VERSION + " (" + os + ")";
		stpDate = Calendar.getInstance().getTime();

	}

	public abstract Header getHeader();

	private String formatLatitude(String lat) {
		String s = "";
		float f = Float.parseFloat(lat);
		if (f >= 0)
			s = "N " + lat;
		else
			s = "S " + Math.abs(f);
		return s;
	}

	public void setStpDate(Date stp) {
		stpDate = stp;
	}
	
	public String getStpDate() {
		if (stpDate == null) return null;
		String dt = "";
		try {
			stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dt = Frame.stpDateFormat.format(stpDate);
		} catch (Exception e) {
			// catch any exceptions based on random date formats the user may send to us
			// For example we get IndexOutOfBounds, format Exceptions and others
			Log.println("ERROR: Could not parse date preparing DB insert, ignoring - " + stpDate  + "\n" + e.getMessage());
		}
		return dt;
	}

	/**
	 * Calculate the time in milliseconds since the epoch in java date format
	 * 
	 * @return
	 */
	public long getExtimateOfT0() {
		if (stpDate == null)
			return 0;
		long stp = stpDate.getTime() / 1000; // calculate the number of seconds
												// in out stp data since epoch
		long T0 = stp - header.uptime;
		return T0 * 1000;
	}

	private String formatLongitude(String lon) {
		String s = "";
		float f = Float.parseFloat(lon);
		if (f >= 0)
			s = "E " + lon;
		else
			s = "W " + Math.abs(f);
		return s;
	}

	public void setMeasurement(RtMeasurement m) {
		rtMeasurement = m;
		if (m.getRawValue(RtMeasurement.CARRIER_FREQ) != 0)
			frequency = Long.toString(Math.round(m
					.getRawValue(RtMeasurement.CARRIER_FREQ))) + " Hz";
	}

	public void setPassMeasurement(PassMeasurement m) {
		passMeasurement = m;
		String strDate = null;
		if (m.getStringValue(PassMeasurement.TCA) != null)
			strDate = m.getStringValue(PassMeasurement.TCA);

		if (strDate.equals(PassMeasurement.DEFAULT_VALUE)) {
			Log.println("TCA Could not be measured");
			measuredTCA = NONE;
		} else
		if (strDate != null) {
			Log.println("Got TCA: " + strDate);
			Date date = null;
			try {
				date = FoxFramePart.fileDateFormat.parse(strDate);
			} catch (ParseException e) {
				// We don't do anything in this case, the date will be null
				Log.println("Error parsing TCA date:");
				e.printStackTrace(Log.getWriter());
				date = null;
			}

			if (date != null) {
				stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				try {
					measuredTCA = stpDateFormat.format(date);
				} catch (Exception e) {
					// catch any exceptions based on random date formats the user may send to us
					// For example we get IndexOutOfBounds, format Exceptions and others
					Log.println("ERROR: Could not parse date setting pass measurement, ignoring - " + stpDate  + "\n" + e.getMessage());
				}
				Log.println("STP TCA set as: " + measuredTCA);
			} else {
				measuredTCA = NONE;
			}
		}
		if (m.getRawValue(PassMeasurement.TCA_FREQ) != PassMeasurement.ERR)
			measuredTCAfrequency = m.getRawValue(PassMeasurement.TCA_FREQ)
					+ " Hz";
	}

	/**
	 * Load a new frame from disk
	 * 
	 * @param input
	 * @throws IOException
	 */
	public Frame(BufferedReader input) throws IOException {
		
	}

	abstract public void addNext8Bits(byte b);

	/**
	 * Add a whole raw frame of data.
	 * 
	 * @param b
	 * @throws StpFileProcessException 
	 */
	public void addRawFrame(byte[] b) throws FrameProcessException {
		int byteLen = b.length;

		// We process the data bytes. addNext8Bits is implemented by the
		// SlowSpeedFrame and
		// HighSpeedFrame classes. It allocates the bits to the correct header,
		// payload or fec sections
		// which in turn allocate the bits to the correct fields.
		for (int i = 0; i < byteLen; i++)
			addNext8Bits(b[i]);
		// At this point we grab the decoded foxId and length in bits, ready for
		// transmission to the server
		foxId = header.getFoxId();
		if (fox == null) throw new FrameProcessException("Invalid Fox Id: " + foxId + ", can not process frame");
		length = Integer.toString(byteLen * 8);

		// TODO - this should be set by the DECODER which knows what type of format it is decoding
		source = "amsat.fox-" + fox.getIdString() + ".";
		try {
			if (this instanceof SlowSpeedFrame) {
				source = source + fox.sourceName[DUV_FRAME];
			} else if (this instanceof HighSpeedFrame){
				source = source + fox.sourceName[HIGH_SPEED_FRAME];
			} else {
				source = source + fox.sourceName[0]; // first value
			}
		} catch (IndexOutOfBoundsException e) {
			// We have a corrupt FoxId
			throw new FrameProcessException("Source missing in Spacecraft File, frame could not be processed.  ID: " + foxId + "\n" +e);
		}
		source = source.toLowerCase();

	}

	/**
	 * Compare Frames based on their sequence number. We should not be comparing
	 * frames across satellites, and we should not have two records with the
	 * same sequence number, but just in case we are, we look at the foxId when
	 * the sequence numbers are equal.
	 * 
	 * @param frame
	 *            to compare against
	 */
	public int compareTo(Frame f) {
		if (sequenceNumber == f.sequenceNumber && foxId == f.foxId)
			return 0;
		else if (sequenceNumber < f.sequenceNumber)
			return -1;
		else if (sequenceNumber > f.sequenceNumber)
			return +1;
		else if (sequenceNumber == f.sequenceNumber)
			if (foxId < f.foxId)
				return -1;
		return +1;
	}

	private String getSTPCoreHeader() {
		String header;

		stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		String date = "";
		try {
			date = stpDateFormat.format(stpDate);
		} catch (Exception e) {
			// catch any exceptions based on random date formats the user may send to us
			// For example we get IndexOutOfBounds, format Exceptions and others
			Log.println("ERROR: Could not parse date getting STP Core Header, ignoring - " + stpDate  + "\n" + e.getMessage());
		}

		// These have to be sent, so cannot be NONE
		header = "Sequence: " + sequenceNumber + "\r\n";
		header = header + "Source: " + source + "\r\n";
		header = header + "Length: " + length + "\r\n";
		header = header + "Date: " + date + " UTC\r\n";
		header = header + "Receiver: " + Config.callsign + "\r\n";
		header = header + "Rx-Location: " + rx_location + "\r\n";

		// These are optional
		if (notNone(frequency))
			header = header + "Frequency: " + frequency + "\r\n";

		if (notNone(receiver_rf))
			header = header + "Receiver-RF: " + receiver_rf + "\r\n";

		if (notNone(demodulator))
			header = header + "Demodulator: " + demodulator + "\r\n";

		return header;
	}

	private boolean notNone(String s) {
		if (s.equalsIgnoreCase(NONE))
			return false;
		if (s.equalsIgnoreCase(""))
			return false;
		return true;

	}

	private String getSTPExtendedHeader() {
		String header = "";

		if (notNone(measuredTCA)) {
			header = "MeasuredTCA: " + measuredTCA + " UTC\r\n";
			;
			header = header + "MeasuredTCAFrequency: " + measuredTCAfrequency
					+ "\r\n";
		}
		return header;

	}
	
	static char pattern = ',';
	static char escapeChar = '~';
	private String escapeComma(String s) {	
		return s.replace(pattern, escapeChar);
	}

	private String insertComma(String s) {
		return s.replace(escapeChar, pattern);
	}

	public void save(BufferedWriter output) throws IOException {
		output.write(Long.toString(sequenceNumber) + "," + foxId + ","
				+ escapeComma(source) + "," + escapeComma(receiver) + ","
				+ escapeComma(frequency) + "," + escapeComma(rx_location) + ","
				+ escapeComma(receiver_rf) + "," + escapeComma(demodulator)
				+ "," + stpDate.getTime() + "," + length + ",");
		for (int i = 0; i < bytes.length - 1; i++)
			output.write(Integer.toString(bytes[i]) + ",");
		output.write(Integer.toString(bytes[bytes.length - 1]) + "\n");
	}
	
	/**
	 * Static factory method that creates a frame from a file
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws StpFileProcessException 
	 * @throws LayoutLoadException
	 */
	public static Frame loadStp(String fileName, boolean rerunRsDecode) throws IOException, StpFileProcessException {
		//String stpDir = fileName;
		//if (!dir.equalsIgnoreCase("")) {
		//	stpDir = dir + File.separator + fileName;
		//	
		//}
		FileInputStream in = new FileInputStream(fileName);
		int c;
		int lineLen = 0;

		boolean done = false;
		boolean readingKey = true;
		String key = "";
		String value = "";
		byte[] rawFrame = null;
		int length = 0;
		String receiver = null;
		Date stpDate = null;
		String frequency = NONE; // frequency when this frame received
		//String source; // The frame source subsystem
		String rx_location = NONE; // the lat, long and altitude
		String receiver_rf = NONE; // human description of the receiver
		String demodulator = null; // will contain Config.VERSION
		long sequenceNumber = Sequence.ERROR_NUMBER;
		
		String measuredTCA = NONE; // time of TCA
		String measuredTCAfrequency = NONE;
		
		boolean firstColon = true;
		char ch;
		// Read the file
		try {
			while (!done && (c = in.read()) != -1) {
				ch = (char) c;
				//System.out.print(ch);

				if (c == 58 && firstColon) { // ':'
					firstColon = false;
					c = in.read(); // consume the space
					c = in.read();
					ch = (char) c; // set ch to the first character
					readingKey = false;
				}
				if ( (c == 13 || c == 10)) { // CR or LF
					c = in.read(); // consume the lf
					if ((length == DUV_FRAME_LEN || length == HIGH_SPEED_FRAME_LEN || length == PSK_FRAME_LEN) && lineLen == 1) {
						// then we are ready to process
						rawFrame = new byte[length/8];
						for (int i=0; i<length/8; i++) {
							rawFrame[i] = (byte) in.read();
						}
						done = true;
					} else {
						// It was a header line
						readingKey = true;
						firstColon = true;
						if (key.startsWith("Length")) {
							length = Integer.parseInt(value);
						}
						if (key.equalsIgnoreCase("Receiver")) {
							receiver = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Frequency")) {
							frequency = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Rx-location")) {
							rx_location = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Receiver-RF")) {
							receiver_rf = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Demodulator")) {
							demodulator = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.endsWith("Sequence")) {
							sequenceNumber = Long.parseLong(value);
							//System.out.println(key + " *** " + value);
						}
						if (key.equalsIgnoreCase("MeasuredTCA")) {
							measuredTCA = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("MeasuredTCAfrequency")) {
							measuredTCAfrequency = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.startsWith("Date")) {
							//                		System.out.println(key + " " + value);
							String dt = value.replace(" UTC", "");
							stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
							try {
								stpDate = stpDateFormat.parse(dt);
							} catch (ParseException | NumberFormatException e) {
								Log.println("ERROR - Date was not parsable. Setting to null"  + "\n" + e.getClass() + "\n" + e.getMessage());
								stpDate = null;
							} catch (Exception e) { // we can get other unusual exceptions such as ArrayIndexOutOfBounds...
								Log.println("ERROR - Date was not parsable. Setting to null: " + e.getMessage());
								stpDate = null;
								e.printStackTrace(Log.getWriter());								
							}
						}
						key = "";
						value = "";
						lineLen = 0;
					}
				} else {
					if (readingKey) 
						key = key + ch;
					else
						value = value + ch;
				}
				lineLen++;

			}

		} finally {
			in.close();
		}
		in.close();

		if (rawFrame == null) {
			// We failed to process the file
			Log.println("Failed to Process STP file. RAW FRAME is null.  No content.  Likely SPAM or broken connection.");
			return null;
			
		}

		// Let's really check it is OK by running the RS decode!

		byte[] frame = null;
		Frame frm;
		if (length == DUV_FRAME_LEN) {
			frm = new SlowSpeedFrame();
		} else if (length == PSK_FRAME_LEN){
			frm = new FoxBPSKFrame(Config.satManager.getFormatByName("FOX_BPSK")); // TO DO - hard coded the format. We should lookup from the length
		} else {
			frm = new HighSpeedFrame();
		}

		if (rerunRsDecode) {

			if (length == DUV_FRAME_LEN) {
				if (ServerConfig.slowSpeedRsDecode) {
					RsCodeWord rs = new RsCodeWord(rawFrame, RsCodeWord.DATA_BYTES-SlowSpeedFrame.MAX_HEADER_SIZE-SlowSpeedFrame.MAX_PAYLOAD_SIZE);
					if (!rs.validDecode()) {
						Log.println("RS Decode Failed");
						throw new StpFileRsDecodeException(fileName, "ERROR: FAILED RS DECODE " + fileName);
					}
				}
			} else if(length == PSK_FRAME_LEN) {
				// High Speed Frame
				// Log.println("RS Decode for: " + length/8 + " byte frame..");
				int[] rsPadding = new int[FoxBPSKBitStream.NUMBER_OF_RS_CODEWORDS];
				rsPadding[0] = 64;
				rsPadding[1] = 64;
				rsPadding[2] = 65;
				if (ServerConfig.highSpeedRsDecode)
					// TODO - This should be looked up from the FoxId and the frameLayout for the sat
					if (!highSpeedRsDecode(476, FoxBPSKBitStream.NUMBER_OF_RS_CODEWORDS, rsPadding, rawFrame, demodulator)) {
						Log.println("BPSK RS Decode Failed");
						throw new StpFileRsDecodeException(fileName, "ERROR: FAILED BPSK RS DECODE " + fileName);
					}
			} else if(length == HIGH_SPEED_FRAME_LEN) {
				// High Speed Frame
				// Log.println("RS Decode for: " + length/8 + " byte frame..");
				if (ServerConfig.highSpeedRsDecode)
					if (!highSpeedRsDecode(HighSpeedFrame.MAX_FRAME_SIZE, HighSpeedBitStream.NUMBER_OF_RS_CODEWORDS, HighSpeedBitStream.RS_PADDING, rawFrame, demodulator)) {
						Log.println("HIGH SPEED RS Decode Failed");
						throw new StpFileRsDecodeException(fileName, "ERROR: FAILED HIGH SPEED RS DECODE " + fileName);
					}
			}
		}
		frame = rawFrame; //rs.decode();


		try {
			frm.addRawFrame(frame);
		} catch (FrameProcessException e) {
			Log.println("Failed to Process STP file. FoxId is corrupt.");
			return null;
		}
		frm.receiver = receiver;
		frm.demodulator = demodulator;
		frm.stpDate = stpDate;
		frm.frequency = frequency;
		frm.rx_location = rx_location;
		frm.receiver_rf = receiver_rf;
		frm.demodulator = demodulator;
		frm.sequenceNumber = sequenceNumber;
		frm.measuredTCA = measuredTCA;
		frm.measuredTCAfrequency = measuredTCAfrequency;

		if ((frm.getHeader().resets == 44 && frm.getHeader().uptime == 260)
				|| (frm.getHeader().resets == 44 && frm.getHeader().uptime == 263)
				|| (frm.getHeader().resets == 44 && frm.getHeader().uptime == 390)
				|| (frm.getHeader().resets == 44 && frm.getHeader().uptime == 393)
				||

				(frm != null && frm.getHeader().resets > 10000))
			return null;

		// if (frm != null) Log.println(frm.getHeader().toString());
		return frm;

	}
	
	/**
	 * Run an RS Decode on a high speed frame.  These bytes should have already been corrected at the ground station, so there should be
	 * no errors.  We do not have a list of erasures at this point, but that should not matter.
	 * @param rawFrame byte[]
	 * @return boolean
	 */
	private static boolean highSpeedRsDecode(int maxFrameSize, int numOfCodewords, int[] padding, byte[] rawFrame, String demodulator) {
		
		String versionString[] = demodulator.split(" ");
		int major = Config.parseVersionMajor(versionString[1]);
		int minor = Config.parseVersionMinor(versionString[1]);
		//String point = Config.parseVersionPoint(versionString[1]);
		//Log.println("RS Decode for: " + demodulator + " "+ major +" "+ minor +" "+ point);
		
		RsCodeWord[] codeWords = new RsCodeWord[numOfCodewords];
		 
		for (int q=0; q < numOfCodewords; q++) {
			codeWords[q] = new RsCodeWord(padding[q]);
		}
	
		int bytesInFrame = 0;
		int f=0; // position in the Rs code words as we allocate bits to them
		int rsNum = 0; // counter that remembers the RS Word we are adding bytes to
		
		for (byte b8 : rawFrame) {
			bytesInFrame++;
			if (bytesInFrame == maxFrameSize+1) {  
				// first parity byte.  All the checkbytes are at the end
				//Log.println("parity");
				if (major > 1 || 
					(major == 1 && minor >= 5)) {
					// FoxTelem 1.05d and later (but a-d were not released externally) - use correct RS Decode
					// Reset to the first code word and Next byte position in the codewords
					rsNum = 0;
					f++;
				} else {
					// FoxTelem 1.05b and earlier - Compensate for the offset FEC Bytes and Decode
					rsNum = 1;
				}
			}
			try {
				codeWords[rsNum++].addByte(b8);
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace(Log.getWriter());
			}
			if (rsNum == numOfCodewords) {
				rsNum=0;
				f++;
				if (f > RsCodeWord.NN)
					Log.println("ERROR: Allocated more high speed data that fits in an RSCodeWord");
			}
		}

	//// DEBUG ///
	//		System.out.println(codeWords[0]);
			
		// Now Decode all of the RS words and fail if any do not pass
		for (int i=0; i < numOfCodewords; i++) {
			codeWords[i].decode();  
			if (!codeWords[i].validDecode()) {
				// We had a failure to decode, so the frame is corrupt
				Log.println("FAILED RS DECODE FOR HS WORD " + i);
				return false;
			} 
		}
		return true;
	}

	public static Frame importStpFile(String u, String p, String db, File f, boolean delete) throws StpFileProcessException {
		PayloadDbStore payloadStore = null;
		try {
			Frame decodedFrame = Frame.loadStp(f.getPath(), true);
			if (decodedFrame != null && !decodedFrame.corrupt) {

				/*
				if (decodedFrame.receiver.equalsIgnoreCase("DK3WN")) {
					long t0 = decodedFrame.getExtimateOfT0();
					if (t0 != 0) {
						Date d0 = new Date(t0);
						Log.println(decodedFrame.receiver + ", Reset, " + decodedFrame.getHeader().getResets() + ", Uptime, " +
								decodedFrame.getHeader().getUptime() + ", STP Date, " + decodedFrame.getStpDate() + ", T0, " + Frame.stpDateFormat.format(d0) + " "
								+ d0.getTime());
					}
				}
				*/
				payloadStore = new PayloadDbStore(u,p,db);
				int newReset = 0; // this will store the reset for HuskySat if the MRAM is broken
				
				if (decodedFrame instanceof SlowSpeedFrame) {
					if (!payloadStore.addStpHeader(decodedFrame))
						throw new StpFileProcessException(f.getName(), "Could not add the STP HEADER to the database ");
					SlowSpeedFrame ssf = (SlowSpeedFrame)decodedFrame;
					FoxFramePart payload = ssf.getPayload();
					SlowSpeedHeader header = ssf.getHeader();
					if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add DUV record to database");
					//duvFrames++;
				} else if (decodedFrame instanceof FoxBPSKFrame) {
					FoxBPSKFrame hsf = (FoxBPSKFrame)decodedFrame;
					FoxSpacecraft fox = (FoxSpacecraft) Config.satManager.getSpacecraft(hsf.header.id);
					if (hsf.header.id == 6) { // better if this was not hardcoded and was in the spacecraft file
						// We are husky sat and the MRAM is broken.  Need to see if this was a reset
						newReset = payloadStore.checkForNewReset(hsf.header.id, hsf.header.uptime, decodedFrame.stpDate, hsf.header.resets, decodedFrame.receiver);
						if (newReset == -1) throw new StpFileProcessException(f.getName(), "Failed to process file: Could not get the reset");
						hsf.header.copyBitsToFields(); // make sure it is all initialized
						hsf.header.resets = newReset; // put in the calculated reset
						hsf.header.rawBits = null; // make sure the updated reset is not overwritten
					}
					if (!payloadStore.addStpHeader(decodedFrame))
						throw new StpFileProcessException(f.getName(), "Could not add the STP HEADER to the database ");
					// For BPSK the header is stored on the frame and the timestamp info is saved
					if (!hsf.savePayloads(payloadStore, fox.hasModeInHeader, newReset))
							throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add PSK record to database");;
				} else {
					if (!payloadStore.addStpHeader(decodedFrame))
						throw new StpFileProcessException(f.getName(), "Could not add the STP HEADER to the database ");
					HighSpeedFrame hsf = (HighSpeedFrame)decodedFrame;
					HighSpeedHeader header = hsf.getHeader();
					PayloadRtValues payload = hsf.getRtPayload();
					if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS RT to database");
					PayloadMaxValues maxPayload = hsf.getMaxPayload();
					if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), maxPayload))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS MAX to database");
					PayloadMinValues minPayload = hsf.getMinPayload();
					if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), minPayload)) 
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not HS MIN add to database");
					PayloadRadExpData[] radPayloads = hsf.getRadPayloads();
					if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), radPayloads))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS RAD to database");
					if (Config.satManager.hasCamera(header.getFoxId())) {
						PayloadCameraData cameraData = hsf.getCameraPayload();
						if (cameraData != null)
							if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), cameraData))
								throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS CAMERA data to database");

					}
					if (Config.satManager.hasHerci(header.getFoxId())) {
						PayloadHERCIhighSpeed[] herciDataSet = hsf.getHerciPayloads();
						if (herciDataSet != null)
							if (!payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), herciDataSet))
								throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HERCI HS data to database");
					}
			
					//hsFrames++;
				}
			}
			if (delete) {
				f.delete();
			}
			return decodedFrame;
		} catch (IOException e) {
			Log.println(e.getMessage());
			e.printStackTrace(Log.getWriter());
			throw new StpFileProcessException(f.getName(), "IO Exception processing file");
		} finally {
			try { payloadStore.closeConnection(); } catch (Exception e) {	}
		}
	}
	


	public static String getTableCreateStmt() {
		String s = new String();
		s = s + "(stpDate varchar(35), id int, resets int, uptime bigint, type int, "
		 + "sequenceNumber bigint, "
		 + "length int, "
		 + "source varchar(35)," 
		 + "receiver varchar(35),"		
		 + "frequency varchar(35),"
		 + "rx_location varchar(35),"	
		 + "receiver_rf varchar(52),"		
		 + "demodulator varchar(100),"
		+ "measuredTCA varchar(35),"
		+ "measuredTCAfrequency varchar(35),"
		+ "date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,";
		s = s + "PRIMARY KEY (id, resets, uptime, type, receiver))";
		return s;
	}
	
	public PreparedStatement getPreparedInsertStmt(Connection con) throws SQLException {
				
		//java.sql.Date sqlDate = new java.sql.Date(stpDate.getTime());
		//FIXME - need to make this a proper date in the DB
		String dt = "";
		if (stpDate != null)
			try {
			dt = stpDateFormat.format(stpDate);
			} catch (Exception e) {
				// catch any exceptions based on random date formats the user may send to us
				// For example we get IndexOutOfBounds, format Exceptions and others
				Log.println("ERROR: Could not parse date preparing DB insert, ignoring - " + stpDate + "\n" + e.getMessage());
			}
		String s = new String();
		if (demodulator.length() > 99) demodulator = demodulator.substring(0, 99);
		if (source.length() > 32) source = source.substring(0, 32);
		if (receiver.length() > 32) receiver = receiver.substring(0, 32);
		if (frequency.length() > 32) frequency = frequency.substring(0, 32);
		if (rx_location.length() > 32) rx_location = rx_location.substring(0, 32);
		if (receiver_rf.length() > 50) receiver_rf = receiver_rf.substring(0, 50);
		if (measuredTCA.length() > 32) measuredTCA = measuredTCA.substring(0, 32);
		if (measuredTCAfrequency.length() > 32) measuredTCAfrequency = measuredTCAfrequency.substring(0, 32);
		s = s + "insert into STP_HEADER (stpDate,  id, resets, uptime, type, \n";
		s = s + "sequenceNumber,\n";
		s = s + "length,\n";
		s = s + "source,\n";
		s = s + "receiver,\n";
		s = s + "frequency,\n";
		s = s + "rx_location,\n";
		s = s + "receiver_rf,\n";
		s = s + "demodulator,\n";
		s = s + "measuredTCA,\n";
		s = s + "measuredTCAfrequency)\n";

		s = s + "values (?, ?, ?, ?, ?,"
				+ "?,?,?,?,?,?,?,?,?,?)";

		java.sql.PreparedStatement ps = con.prepareStatement(s);
		
		ps.setString(1, dt);
		ps.setInt(2, foxId);
		ps.setInt(3, header.resets);
		ps.setLong(4, header.uptime);
		ps.setInt(5, header.type);
		ps.setLong(6, sequenceNumber);
		ps.setString(7, length);
		ps.setString(8, source);
		ps.setString(9, receiver);
		ps.setString(10, frequency);
		ps.setString(11, rx_location);
		ps.setString(12, receiver_rf);
		ps.setString(13, demodulator);
		ps.setString(14, measuredTCA);
		ps.setString(15, measuredTCAfrequency);

		return ps;
		
	}
	
	public void load(BufferedReader input) throws IOException {
		
		String line = input.readLine();
		if (line != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			sequenceNumber = Long.parseLong(st.nextToken());
			foxId = Integer.parseInt(st.nextToken());
			source = insertComma(st.nextToken());
			receiver = insertComma(st.nextToken());
			frequency = insertComma(st.nextToken());
			rx_location = insertComma(st.nextToken());
			receiver_rf = insertComma(st.nextToken());
			demodulator = insertComma(st.nextToken());
			stpDate = new Date(Long.parseLong(st.nextToken()));
			length = st.nextToken();
			//System.out.println("loaded: " + sequenceNumber);
			while (st.hasMoreTokens()) {

				//bytes[i] = (byte) Integer.parseInt(st.nextToken());
				this.addNext8Bits((byte) Integer.parseInt(st.nextToken()));
				// System.out.println("loaded: " + i + " - " + bytes[i]);
			}
		}
		// We do not need to load the bytes into the payloads. The only reason
		// we load a frame from disk is if we want to
		// send it to the amsat server. This only needs the raw bytes
	}

	/**
	 * Send this frame to the amsat server "hostname" on "port" for storage. It
	 * is sent by the queue so there is no need for this routine to remove the
	 * record from the queue
	 * 
	 * @param hostName
	 * @param port
	 */
//	public void sendToServer_DEPRECIATED(TlmServer tlmServer, int protocol)
//			throws UnknownHostException, IOException {
//		String header = getSTPCoreHeader();
//		header = header + getSTPExtendedHeader();
//		header = header + "\r\n";
//		byte[] headerBytes = header.getBytes();
//
//		int j = 0;
//		byte[] buffer = new byte[headerBytes.length + bytes.length];
//		for (byte b : headerBytes)
//			buffer[j++] = b;
//		for (byte b : bytes)
//			buffer[j++] = b;
//
//		tlmServer.sendToServer(buffer, protocol);
//		if (Config.debugFrames)
//			Log.println(header);
//	}

	/**
	 * Get the bytes from this frame so they can be sent to a server
	 * @return
	 */
	public byte[] getServerBytes() {
		String header = getSTPCoreHeader();
		header = header + getSTPExtendedHeader();
		header = header + "\r\n";
		byte[] headerBytes = header.getBytes();

		int j = 0;
		byte[] buffer = new byte[headerBytes.length + bytes.length];
		for (byte b : headerBytes)
			buffer[j++] = b;
		for (byte b : bytes)
			buffer[j++] = b;

		if (Config.debugFrames)
			Log.println(header);
		return buffer;
	}
	
	/**
	 * Send Payload bytes to local server if it is configured
	 * This returns an array of byte arrays.  One byte array per payload in the frame.
	 * Override in child class
	 * 
	 */
	public byte[][] getPayloadBytes() {
		return null;
	}

	public String toWodTimestampString(int reset, long uptime) {
		return null;
	}
}
