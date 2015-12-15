package telemetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import telemServer.StpFileProcessException;
import measure.PassMeasurement;
import measure.RtMeasurement;
import common.Config;
import common.Log;
import common.Sequence;
import common.Spacecraft;
import common.TlmServer;
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
	Spacecraft fox; // the satellite that we are decoding a frame for, populated
					// once the header is filled

	public static final int DUV_FRAME = 0;
	public static final int HIGH_SPEED_FRAME = 1;
	public static final String[][] SOURCES = {
			{ "amsat.fox-test.ihu.duv", "amsat.fox-test.ihu.highspeed" },
			{ "amsat.fox-1a.ihu.duv", "amsat.fox-1a.ihu.highspeed" },
			{ "amsat.fox-1b.ihu.duv", "amsat.fox-1b.ihu.highspeed" },
			{ "amsat.fox-1c.ihu.duv", "amsat.fox-1c.ihu.highspeed" },
			{ "amsat.fox-1d.ihu.duv", "amsat.fox-1d.ihu.highspeed" },
			{ "amsat.fox-1e.ihu.duv", "amsat.fox-1e.ihu.highspeed" } };

	public static final String SEQUENCE_FILE_NAME = "seqno.dat";
	public static final String NONE = "NONE";
	public boolean corrupt = false;;
	private int foxId = 0;
	public String receiver = NONE; // unique name (usually callsign) chosen by
									// the user. May vary over life of program
									// usage, so stored
	private String frequency = NONE; // frequency when this frame received
	private String source; // The frame source subsystem
	private String length; // The frame length in bytes
	private String rx_location = NONE; // the lat, long and altitude
	private String receiver_rf = NONE; // human description of the receiver
	public String demodulator; // will contain Config.VERSION
	private Date stpDate;
	private long sequenceNumber = Sequence.ERROR_NUMBER;

	private String measuredTCA = NONE; // time of TCA
	private String measuredTCAfrequency = NONE; // frequency if this frame was
												// just after TCA

	int numberBytesAdded = 0;
	byte[] bytes;

	// Store a reference to any measurements that were made at the same time as
	// the Frame was downloaded, so we can pass them on to the server
	private RtMeasurement rtMeasurement;
	private PassMeasurement passMeasurement;

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

	public String getStpDate() {
		if (stpDate == null) return null;
		return Frame.stpDateFormat.format(stpDate);
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
		if (m.getRawValue(PassMeasurement.TCA) != null)
			strDate = m.getRawValue(PassMeasurement.TCA);

		if (strDate != null) {
			Log.println("Got TCA: " + strDate);
			Date date = null;
			try {
				date = FramePart.fileDateFormat.parse(strDate);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			measuredTCA = stpDateFormat.format(date);
			Log.println("STP TCA set as: " + measuredTCA);
		}
		if (m.getRawValue(PassMeasurement.TCA_FREQ) != null)
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
		load(input);
	}

	abstract public void addNext8Bits(byte b);

	/**
	 * Add a whole raw frame of data.
	 * 
	 * @param b
	 */
	public void addRawFrame(byte[] b) {
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
		length = Integer.toString(byteLen * 8);

		if (this instanceof SlowSpeedFrame) {
			source = SOURCES[foxId][DUV_FRAME];
		} else {
			source = SOURCES[foxId][HIGH_SPEED_FRAME];
		}

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
			return +1;
		else if (sequenceNumber > f.sequenceNumber)
			return -1;
		else if (sequenceNumber == f.sequenceNumber)
			if (foxId < f.foxId)
				return +1;
		return -1;
	}

	private String getSTPCoreHeader() {
		String header;

		stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date = stpDateFormat.format(stpDate);

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

	private String escapeComma(String s) {
		char pattern = ',';
		char escapeChar = '~';
		return s.replace(pattern, escapeChar);
	}

	private String insertComma(String s) {
		char pattern = ',';
		char escapeChar = '~';
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
	public static Frame loadStp(String fileName) throws IOException, StpFileProcessException {
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
		String source; // The frame source subsystem
		String rx_location = NONE; // the lat, long and altitude
		String receiver_rf = NONE; // human description of the receiver
		String demodulator = null; // will contain Config.VERSION
		long sequenceNumber = Sequence.ERROR_NUMBER;
		
		String measuredTCA = NONE; // time of TCA
		String measuredTCAfrequency = NONE;
		
		boolean firstColon = true;

		// Read the file
		try {
			while (!done && (c = in.read()) != -1) {
				Character ch = (char) c;
				//System.out.print(ch);

				if (ch == ':' && firstColon) {
					firstColon = false;
					c = in.read(); // consume the space
					c = in.read();
					ch = (char) c; // set ch to the first character
					readingKey = false;
				}
				if ( (c == 13 || c == 10)) { // CR or LF
					c = in.read(); // consume the lf
					if ((length == 768 || length == 42176) && lineLen == 1) {
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
						if (key.equalsIgnoreCase("Rx_location")) {
							rx_location = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Receiver_rf")) {
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
							} catch (ParseException e) {
								Log.println("ERROR - Date was not parsable. Setting to null");
								stpDate = null;
								//e.printStackTrace(Log.getWriter());
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
			Log.println("Failed to Process STP file");
			throw new StpFileProcessException(fileName, "Failed to process file: rawFrame is null " + fileName);
			
		}

		// Let's really check it is OK by running the RS decode!

		byte[] frame = null;
		if (length == 768) {
			RsCodeWord rs = new RsCodeWord(rawFrame, RsCodeWord.DATA_BYTES-SlowSpeedFrame.MAX_HEADER_SIZE-SlowSpeedFrame.MAX_PAYLOAD_SIZE);
			if (!rs.validDecode()) {
				Log.println("RS Decode Failed");
				throw new StpFileProcessException(fileName, "ERROR: FAILED RS DECODE " + fileName);
			}
		}
		frame = rawFrame; //rs.decode();

		Frame frm;
		if (length == 768) {
			frm = new SlowSpeedFrame();
		} else {
			frm = new HighSpeedFrame();
		}
		frm.addRawFrame(frame);
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

	public static Frame importStpFile(File f, boolean delete) throws StpFileProcessException {
		try {
			Frame decodedFrame = Frame.loadStp(f.getPath());
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
				if (!Config.payloadStore.addStpHeader(decodedFrame))
					throw new StpFileProcessException(f.getName(), "Could not add the STP HEADER to the database ");
				if (decodedFrame instanceof SlowSpeedFrame) {
					SlowSpeedFrame ssf = (SlowSpeedFrame)decodedFrame;
					FramePart payload = ssf.getPayload();
					SlowSpeedHeader header = ssf.getHeader();
					if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add DUV record to database");
					//duvFrames++;
				} else {
					HighSpeedFrame hsf = (HighSpeedFrame)decodedFrame;
					HighSpeedHeader header = hsf.getHeader();
					PayloadRtValues payload = hsf.getRtPayload();
					if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS RT to database");
					PayloadMaxValues maxPayload = hsf.getMaxPayload();
					if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), maxPayload))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS MAX to database");
					PayloadMinValues minPayload = hsf.getMinPayload();
					if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), minPayload)) 
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not HS MIN add to database");
					PayloadRadExpData[] radPayloads = hsf.getRadPayloads();
					if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), radPayloads))
						throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS RAD to database");
					if (Config.satManager.hasCamera(header.getFoxId())) {
						PayloadCameraData cameraData = hsf.getCameraPayload();
						if (!Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), cameraData))
							throw new StpFileProcessException(f.getName(), "Failed to process file: Could not add HS CAMERA data to database");

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
	
	public String getInsertStmt() {
	//	java.sql.Date sqlDate = new java.sql.Date(stpDate.getTime());
		//FIXME - need to omake this a proper date in the DB
		String dt = "";
		if (stpDate != null)
			dt = stpDateFormat.format(stpDate);
		String s = new String();
		if (demodulator.length() > 99) demodulator = demodulator.substring(0, 99);
		if (source.length() > 32) source = source.substring(0, 32);
		if (receiver.length() > 32) receiver = receiver.substring(0, 32);
		if (frequency.length() > 32) frequency = frequency.substring(0, 32);
		if (rx_location.length() > 32) rx_location = rx_location.substring(0, 32);
		if (receiver_rf.length() > 50) receiver_rf = receiver_rf.substring(0, 50);
		if (measuredTCA.length() > 32) measuredTCA = measuredTCA.substring(0, 32);
		if (measuredTCAfrequency.length() > 32) measuredTCAfrequency = measuredTCAfrequency.substring(0, 32);
		s = s + " (stpDate,  id, resets, uptime, type, \n";
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
		
		s = s + "values ('" + dt + "', " + this.foxId + ", " + header.resets + ", " + header.uptime + ", " + header.type + ",\n";
		s = s + sequenceNumber+",\n";
		s = s + length+",\n";
		s = s + "'" + source+"',\n";
		s = s + "'" + receiver+"',\n";
		s = s + "'" + frequency+"',\n";
		s = s + "'" + rx_location+"',\n";
		s = s + "'" + receiver_rf+"',\n";
		s = s + "'" + demodulator+"',\n";
		s = s + "'" + measuredTCA+"',\n";
		s = s + "'" + measuredTCAfrequency+"')\n";
		return s;
	}

	
	public void load(BufferedReader input) throws IOException {
		if (this instanceof SlowSpeedFrame)
			bytes = new byte[SlowSpeedFrame.MAX_HEADER_SIZE
					+ SlowSpeedFrame.MAX_PAYLOAD_SIZE
					+ SlowSpeedFrame.MAX_TRAILER_SIZE];
		else
			bytes = new byte[HighSpeedFrame.MAX_HEADER_SIZE
					+ HighSpeedFrame.MAX_PAYLOAD_SIZE
					+ HighSpeedFrame.MAX_TRAILER_SIZE];
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
			// System.out.println("loaded: " + this.getSTPHeader());
			for (int i = 0; i < bytes.length; i++) {

				bytes[i] = (byte) Integer.parseInt(st.nextToken());
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
	public void sendToServer(TlmServer tlmServer, int protocol)
			throws UnknownHostException, IOException {
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

		tlmServer.sendToServer(buffer, protocol);
		if (Config.debugFrames)
			Log.println(header);
	}

}
