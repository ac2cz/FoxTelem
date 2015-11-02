package telemServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import common.Log;
import telemetry.Frame;

public class ServerProcess implements Runnable {

	private Socket socket = null;
	private int sequence = 0;
	public ServerProcess(Socket socket, int seq) {
		sequence = seq;
		this.socket = socket;
	}

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

		//Writer f = null;
		FileOutputStream f = null;
		//PrintWriter out = null;
		//BufferedReader in = null;
		InputStream in = null;
		String fileName;
		File stp = null;
		try {
			//out = new PrintWriter(socket.getOutputStream(), true);
			//f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sequence + ".stp"), "utf-8"));
			int b=0;
			fileName = nextSTPFile();
			f = new FileOutputStream(fileName);
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			in = socket.getInputStream();
			int c;
			while ((c = in.read()) != -1) {
				f.write(c);
			//	Character ch = (char) c;
			//	System.out.print(ch);
				b++;
			}
			//System.out.println();
			in.close();
			socket.close();
			f.close();
			// At this point the file is on disk, so we can process it
			stp = new File(fileName);
			// Import it into the database
			// null return means the files is te
			Frame frm = Frame.importStpFile(stp, false);
			if (frm != null) {
				Log.println("Processed: " + b + " bytes from " + frm.receiver + " " + frm.getHeader().getResets() + " " + frm.getHeader().getUptime() 
						+ " " + frm.getHeader().getType());
				File toFile = new File(stp.getPath()+".processed");
				if (stp.renameTo(toFile))
					;
				else
					Log.println("ERROR: Could not mark file as processed: " + stp.getAbsolutePath());
			}
			else {
				// This was the test data and we do not care if it is processed
				File toFile = new File(stp.getPath()+".null");
				if (stp.renameTo(toFile))
					;
				else
					Log.println("ERROR: Could not mark file as null data: " + stp.getAbsolutePath());
			}
			
		} catch (IOException e) {
			Log.println("ERROR ALERT:" + e.getMessage());
			e.printStackTrace(Log.getWriter());
			// We could not read the data from the socket or write the file.  So we log an alert!  Something wrong with server
			////ALERT
		} catch (StpFileProcessException e) {
			Log.println("STP EXCPETION: " + e.getMessage());
			e.printStackTrace(Log.getWriter());
			// We could not process the file so try to store it as an exception, something wrong with the data
			storeException(stp);
		} finally {
			try { 
				in.close();
				socket.close();
				f.close();
			} catch (Exception ex) { /*ignore*/} 
		}
	}

	private void storeException(File f) {
		File toFile = new File("exceptions" + File.separator + f.getName()+".ex");
		if (f != null) {
			if (f.renameTo(toFile)) {
				;
			} else {
				Log.println("ERROR: Could rename the file into the exeption dir: " + f.getPath());
			}
			//try {
				/*
				copyFile(f, toFile);
				if (f.delete()) {
					;
				} else {
					Log.println("ERROR: Can't remove the (original) file we stored as an exception");
				}
			} catch (IOException e) {
				Log.println("ERROR: Could not copy the file into the exception dir: " + f.getPath());
				e.printStackTrace(Log.getWriter());
			}
			*/
		} else
			Log.println("Don't know which file to store as an exception");
	}

}
