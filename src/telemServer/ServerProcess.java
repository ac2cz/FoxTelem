package telemServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import common.Log;
import common.Sequence;
import telemetry.frames.Frame;
import telemetry.frames.HighSpeedFrame;

public class ServerProcess implements Runnable {
	public static final String NONE = "NONE";
	public static final int DUV_FRAME_LEN = 768; 
	public static final int HIGH_SPEED_FRAME_LEN = 42176;
	public static final int PSK_FRAME_LEN = 4576;
	public static final byte[] OK = {0x4F,0x4D,0x0D,0x0A};
	public static final byte[] FAIL = {0x46,0x41,0x0D,0x0A};

	String u;
	String p;
	String db;
	private Socket socket = null;
	private int sequence = 0;
	
	public ServerProcess(String u, String p, String db, Socket socket, int seq) {
		sequence = seq;
		this.socket = socket;
		this.u = u;
		this.p = p;
		this.db = db;
	}


	// safety limit to stop massive files being sent to us
	// Max frame size is a high speed frame plus the maximum STP Header Size, which is circa 350 bytes.  1000 used to be conservative
	public static final int MAX_FRAME_SIZE = HighSpeedFrame.MAX_FRAME_SIZE + 1000;
	public static final DateFormat fileDateName = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final DateFormat yearDirName = new SimpleDateFormat("yyyy");
	public static final DateFormat monthDirName = new SimpleDateFormat("MM");
	public static final DateFormat dayDirName = new SimpleDateFormat("dd");
	
	String nextSTPFile() throws IOException {
		Date today = Calendar.getInstance().getTime();
		String year = datePartName(today, yearDirName);
		String month = datePartName(today, monthDirName);
		String day = datePartName(today, dayDirName);
		String fileName = datePartName(today, fileDateName);
		
		makeDir(year);
		makeDir(year + File.separator + month);
		makeDir(year + File.separator + month + File.separator + day);
		String f = year + File.separator + month + File.separator + day 
				+ File.separator + fileName + "." + sequence + ".stp";
		return f;
		
	}
	
	String exceptionDir() throws IOException {
		Date today = Calendar.getInstance().getTime();
		String year = datePartName(today, yearDirName);
		String month = datePartName(today, monthDirName);
		String day = datePartName(today, dayDirName);
		//String fileName = datePartName(today, fileDateName);
		
		makeDir(year);
		makeDir(year + File.separator + month);
		makeDir(year + File.separator + month + File.separator + day);
		String exception = year + File.separator + month + File.separator + day 
				+ File.separator + "exception";
		makeDir(exception);
		return exception;
		
	}
	
	public static String datePartName(Date today, DateFormat df) {
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = df.format(today);
		return reportDate;
	}

	public void makeDir(String dir) throws IOException {
		File aFile = new File(dir);
		if(aFile.isDirectory()){
			// already exists
			return;
		} else {
			aFile.mkdir();
		}
		if(!aFile.isDirectory()){
			throw new IOException("Can not make the dir: " + dir);
		}
	}
	
	public boolean fileExists(String fileName) {
		File toFile = new File(fileName);
		if(!toFile.exists())
			return false;
		return true;
			
	}
	
	
	/**
	 * This is started when we have a TCP connection.  We read the data until the connection is closed
	 * This could be one or more STP files.
	 */
	public void run() {
		Log.println("Started Thread to handle connection from: " + socket.getInetAddress());

		FileOutputStream f = null;
		InputStream in = null;
		OutputStream out = null;
		String fileName;
		File stp = null;
		try {
			boolean done = false;
			boolean readingKey = true;
			String key = "";
			String value = "";
			byte[] rawFrame = null;
			int length = 0;
			@SuppressWarnings("unused")
			String receiver = null;
			@SuppressWarnings("unused")
			Date stpDate = null;
			@SuppressWarnings("unused")
			String frequency = NONE; // frequency when this frame received
			@SuppressWarnings("unused")
			String source; // The frame source subsystem
			@SuppressWarnings("unused")
			String rx_location = NONE; // the lat, long and altitude
			@SuppressWarnings("unused")
			String receiver_rf = NONE; // human description of the receiver
			@SuppressWarnings("unused")
			String demodulator = null; // will contain Config.VERSION
			@SuppressWarnings("unused")
			long sequenceNumber = Sequence.ERROR_NUMBER;
			@SuppressWarnings("unused")
			String measuredTCA = NONE; // time of TCA
			@SuppressWarnings("unused")
			String measuredTCAfrequency = NONE;
			int lineLen = 0;
			boolean firstColon = true;
			char ch;
			int b=0;
			fileName = nextSTPFile();
			f = new FileOutputStream(fileName);
			socket.setSoTimeout(ServerConfig.socketReadTimeout);
			in = socket.getInputStream();
			out = socket.getOutputStream();
			int c;
			
			while (!done && (c = in.read()) != -1) {
				f.write(c);
				b++;
				if (b > MAX_FRAME_SIZE) 
					throw new StpFileProcessException(fileName,"Frame too long, probablly spam: Aborted");
				ch = (char) c;
				if (c == 58 && firstColon) { // ':'
					firstColon = false;
					c = in.read(); // consume the space
					f.write(c);
					c = in.read();
					f.write(c);
					ch = (char) c; // set ch to the first character
					readingKey = false;
				}
				if ( (c == 13 || c == 10)) { // CR or LF
					c = in.read(); // consume the lf
					f.write(c);
					if ((length == DUV_FRAME_LEN || length == HIGH_SPEED_FRAME_LEN || length == PSK_FRAME_LEN) && lineLen == 1) {
						// then we are ready to process
						rawFrame = new byte[length/8];
						for (int i=0; i<length/8; i++) {
							c = in.read();
							rawFrame[i] = (byte) c;
							f.write(c);
						}
						done = true;
					} else {
						// It was a header line
						readingKey = true;
						firstColon = true;
						if (key.startsWith("Length")) {
							length = Integer.parseInt(value);
						} else
						if (key.equalsIgnoreCase("Receiver")) {
							receiver = value;
						} else
						if (key.equalsIgnoreCase("Source")) {
							source = value;
						} else
						if (key.equalsIgnoreCase("Frequency")) {
							frequency = value;
						} else
						if (key.equalsIgnoreCase("Rx-location")) {
							rx_location = value;
						} else
						if (key.equalsIgnoreCase("Receiver-RF")) {
							receiver_rf = value;
						} else
						if (key.equalsIgnoreCase("Demodulator")) {
							demodulator = value;
						} else
						if (key.endsWith("Sequence")) {
							sequenceNumber = Long.parseLong(value);
						} else
						if (key.equalsIgnoreCase("MeasuredTCA")) {
							measuredTCA = value;
						} else
						if (key.equalsIgnoreCase("MeasuredTCAfrequency")) {
							measuredTCAfrequency = value;
						} else
						if (key.startsWith("Date")) {
							String dt = value.replace(" UTC", "");
							Frame.stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
							try {
								stpDate = Frame.stpDateFormat.parse(dt);
							} catch (ParseException e) {
								Log.println("-"+ socket.getInetAddress()+" ERROR - Date was not parsable. Setting to null"  + "\n" + e.getMessage());
								stpDate = null;
							} catch (NumberFormatException e) {
								Log.println("-"+ socket.getInetAddress()+" ERROR - Date has number format exception. Setting to null"  + "\n" + e.getMessage());
								stpDate = null;
							} catch (Exception e) { // we can get other unusual exceptions such as ArrayIndexOutOfBounds...
								Log.println("-"+ socket.getInetAddress()+" ERROR - Date was not parsable. Setting to null: " + e.getMessage());
								stpDate = null;
								e.printStackTrace(Log.getWriter());								
							}
						} else {
							// Eror, not a valid header, FAIL
							throw new StpFileProcessException(fileName,"Invalid Header: Aborted");
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
			// Frame read successfully, send OK if client still connected
			try {out.write(OK); Log.println("-"+ socket.getInetAddress()+" OK SENT");} catch (IOException e1) { Log.println("-"+ socket.getInetAddress()+" OK Ignored");/*ignore*/}
			
			in.close();
			out.close();
			socket.close();
			f.close();
			
			// At this point the file is on disk, so we can process it
			stp = new File(fileName);
			
			// Import it into the database
			// null return means the file can not be recognized as an STP file or was test data
			Frame frm = Frame.importStpFile(u, p, db, stp, false);
			if (frm != null) {
				Log.println("-"+ socket.getInetAddress()+" Processed: " + b + " bytes from " + frm.receiver + " for " 
						+ frm.getHeader().getFoxId() + " " + frm.getHeader().getResets() + " " + frm.getHeader().getUptime() 
						+ " " + frm.getHeader().getType() + "---" + fileName);
				File toFile = new File(stp.getPath()+".processed");
				if (stp.renameTo(toFile))
					;
				else
					Log.println("-"+ socket.getInetAddress()+" ERROR: Could not mark file as processed: " + stp.getAbsolutePath()); // while this seems serious, we have the data and it is marked as unprocessed, so we can go back and recover
			}
			else {
				// This was the test data and we do not care if it is processed
				File toFile = new File(stp.getPath()+".null");
				if (stp.renameTo(toFile))
					;
				else
					Log.println("-"+ socket.getInetAddress()+" ERROR: Could not mark file as null data: " + stp.getAbsolutePath());
			}
			
		} catch (SocketException e) {
			Log.println("-"+ socket.getInetAddress()+" SOCKET EXCEPTION, file will not be processed");
			// try to send error message to client
			try {out.write(FAIL);} catch (IOException e1) { /*ignore*/}
		} catch (SocketTimeoutException e) {
			Log.println("-"+ socket.getInetAddress()+" SOCKET TIMEOUT EXCEPTION, file will not be processed: " + e);
			// try to send error message to client
			try {out.write(FAIL);} catch (IOException e1) { /*ignore*/}
		} catch (IOException e) {
			// try to send error message to client
			try {out.write(FAIL);} catch (IOException e1) { /*ignore*/}
			Log.println("-"+ socket.getInetAddress()+" ERROR ALERT:" + e);
			e.printStackTrace(Log.getWriter());
			// We could not read the data from the socket or write the file.  So we log an alert!  Something wrong with server
			////ALERT
			Log.alert("-"+ socket.getInetAddress()+" FATAL: " + e);
			e.printStackTrace(Log.getWriter());		
		} catch (StpFileRsDecodeException rs) {
			// try to send error message to client
			try {out.write(FAIL);} catch (IOException e1) { /*ignore*/}
			Log.println("-"+ socket.getInetAddress()+" STP FILE Could not be decoded: " + rs.getMessage());
			File toFile = new File(stp.getPath()+".null");
			if (stp.renameTo(toFile))
				;
			else
				Log.println("-"+ socket.getInetAddress()+" ERROR: Could not mark failed RS Decode file as null data: " + stp.getAbsolutePath());
		} catch (StpFileProcessException e) {
			// try to send error message to client
			try {out.write(FAIL);} catch (IOException e1) { /*ignore*/}
			Log.println("-"+ socket.getInetAddress()+" STP EXCPETION: " + e.getMessage());
			//e.printStackTrace(Log.getWriter());
			// We could not process the file so try to store it as an exception, something wrong with the data or we could not write to the DB
			try {
				storeException(stp);
			} catch (IOException e1) {
				Log.println("-"+ socket.getInetAddress()+" ERROR: Could not rename the file into the exeption dir: " + e1);
			}
		} catch (Exception e) {
			// try to send error message to client
			try {out.write(FAIL);} catch (IOException e1) { /*ignore*/}
			Log.println("-"+ socket.getInetAddress()+" FATAL THREAD EXCPETION: " + e);
			e.printStackTrace(Log.getWriter());
		} finally {
			try { in.close();  } catch (Exception ex) { /*ignore*/}
			try { out.close();  } catch (Exception ex) { /*ignore*/}
			try { socket.close();  } catch (Exception ex) { /*ignore*/} 
			try { f.close();  } catch (Exception ex) { /*ignore*/}
		}
	}

	private void storeException(File f) throws IOException {
		
		if (f != null) {
			String exceptions = exceptionDir();
			File toFile = new File(exceptions + File.separator + f.getName()+".ex");
			if (f.renameTo(toFile)) {
				;
			} else {
				Log.println("-"+ socket.getInetAddress()+" ERROR: Could not rename the file into the exeption dir: " + f.getPath());
			}
		} else
			Log.println("-"+ socket.getInetAddress()+" Don't know which file to store as an exception");
	}

}
