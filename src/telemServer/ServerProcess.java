package telemServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
	
	String nextSTPFile() throws FileNotFoundException {
		Date today = Calendar.getInstance().getTime();
		String year = dirName(today, yearDirName);
		String month = dirName(today, monthDirName);
		String day = dirName(today, dayDirName);
		String fileName = dirName(today, fileDateName);
		
		makeDir(year);
		makeDir(year + File.separator + month);
		makeDir(year + File.separator + month + File.separator + day);
		String f = year + File.separator + month + File.separator + day 
				+ File.separator + fileName + "." + sequence + ".stp";
		return f;
		
	}
	
	public static String dirName(Date today, DateFormat df) {
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = df.format(today);
		return reportDate;
	}

	public void makeDir(String dir) {
		File aFile = new File(dir);
		if(aFile.isDirectory()){
			// already exists
			return;
		} else {
			aFile.mkdir();
		}
		if(!aFile.isDirectory()){
			Log.println("Can not make the dir: " + dir);
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
		try {
			//out = new PrintWriter(socket.getOutputStream(), true);
			//f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sequence + ".stp"), "utf-8"));
			
			String fileName = nextSTPFile();
			f = new FileOutputStream(fileName);
			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			in = socket.getInputStream();
			int c;
			while ((c = in.read()) != -1) {
				f.write(c);
				Character ch = (char) c;
				System.out.print(ch);
			}
			System.out.println();
			in.close();
			socket.close();

			// At this point the file is on disk, so we can process it
			File stp = new File(fileName);
			// Import it into the database
			Frame frm = Frame.importStpFile(stp, false);
			if (frm != null)
				Log.println("Processed: " + frm.receiver + " " + frm.getHeader().getResets() + " " + frm.getHeader().getUptime() + " " + frm.getHeader().getType());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { f.close(); } catch (Exception ex) { /*ignore*/} 
		}
	}

}
