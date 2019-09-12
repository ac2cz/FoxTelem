package telemServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import common.Log;
import telemetry.Frame;
import telemetry.HighSpeedFrame;

public class ServerProcess implements Runnable {
	//PayloadDbStore payloadStoreX;
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
		String fileName;
		File stp = null;
		try {
			int b=0;
			fileName = nextSTPFile();
			f = new FileOutputStream(fileName);
			in = socket.getInputStream();
			int c;
			while ((c = in.read()) != -1) {
				f.write(c);
				b++;
				if (b > MAX_FRAME_SIZE) 
					throw new StpFileProcessException(fileName,"Frame too long, probablly spam: Aborted");
			}
			
			in.close();
			socket.close();
			f.close();
			
			// At this point the file is on disk, so we can process it
			stp = new File(fileName);
			
			// Import it into the database
			// null return means the file can not be recognized as an STP file or was test data
			Frame frm = Frame.importStpFile(u, p, db, stp, false);
			if (frm != null) {
				Log.println("Processed: " + b + " bytes from " + frm.receiver + " for " 
						+ frm.getHeader().getFoxId() + " " + frm.getHeader().getResets() + " " + frm.getHeader().getUptime() 
						+ " " + frm.getHeader().getType() + "---" + fileName);
				File toFile = new File(stp.getPath()+".processed");
				if (stp.renameTo(toFile))
					;
				else
					Log.println("ERROR: Could not mark file as processed: " + stp.getAbsolutePath()); // while this seems serious, we have the data and it is marked as unprocessed, so we can go back and recover
			}
			else {
				// This was the test data and we do not care if it is processed
				File toFile = new File(stp.getPath()+".null");
				if (stp.renameTo(toFile))
					;
				else
					Log.println("ERROR: Could not mark file as null data: " + stp.getAbsolutePath());
			}
			
		} catch (SocketException e) {
			Log.println("SOCKET EXCEPTION, file will not be processed");
		} catch (IOException e) {
			Log.println("ERROR ALERT:" + e);
			e.printStackTrace(Log.getWriter());
			// We could not read the data from the socket or write the file.  So we log an alert!  Something wrong with server
			////ALERT
			Log.alert("FATAL: " + e);
			e.printStackTrace(Log.getWriter());
		
		} catch (StpFileRsDecodeException rs) {
			Log.println("STP FILE Could not be decoded: " + rs.getMessage());
			File toFile = new File(stp.getPath()+".null");
			if (stp.renameTo(toFile))
				;
			else
				Log.println("ERROR: Could not mark failed RS Decode file as null data: " + stp.getAbsolutePath());
		} catch (StpFileProcessException e) {
			Log.println("STP EXCPETION: " + e);
			e.printStackTrace(Log.getWriter());
			// We could not process the file so try to store it as an exception, something wrong with the data or we could not write to the DB
			storeException(stp);
		} catch (Exception e) {
			Log.println("FATAL THREAD EXCPETION: " + e);
			e.printStackTrace(Log.getWriter());
		} finally {
			try { in.close();  } catch (Exception ex) { /*ignore*/}
			try { socket.close();  } catch (Exception ex) { /*ignore*/} 
			try { f.close();  } catch (Exception ex) { /*ignore*/}
		}
	}

	private void storeException(File f) {
		
		if (f != null) {
			File toFile = new File("exceptions" + File.separator + f.getName()+".ex");
			if (f.renameTo(toFile)) {
				;
			} else {
				Log.println("ERROR: Could not rename the file into the exeption dir: " + f.getPath());
			}
		} else
			Log.println("Don't know which file to store as an exception");
	}

}
