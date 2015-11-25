package common;

import gui.MainWindow;
import gui.SourceTab;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JOptionPane;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
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
 * Save messages from the GUI to a log file for later analysis.  This is a static class and new should not be called
 *
 *
 */
public class Log {

	static private boolean echoToStdout = true; // true for debugging through eclipse because its handy.  This will never be true in production version.
	static PrintWriter output = null;
	public static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	public static SourceTab logPanel;
	public static String logFile = "FoxTelemDecoder.log";
	public static Thread.UncaughtExceptionHandler uncaughtExHandler;
	/**
	 * Initialise the logger and create a logfile with the passed name
	 * @param file
	 * @throws IOException 
	 */
	public static void init() {
		if (Config.logging) {
			try {
				if (!Config.logFileDirectory.equalsIgnoreCase("")) {
					logFile = Config.logFileDirectory + File.separator + logFile;
				} 
				File aFile = new File(logFile);
				if(!aFile.exists()){
					aFile.createNewFile();
				}

				//use buffering and append to the existing file if it is there
				output = new PrintWriter(new FileWriter(aFile, true));
			} catch (IOException e) {
				System.err.println("FATAL ERROR: Cannot write log file: FoxTelemDecoder.log\n"
						+ "Perhaps the disk is full or the directory is not writable:\n" + Config.logFileDirectory);

				e.printStackTrace();
		        Log.errorDialog("FATAL ERROR", "Cannot write log file: FoxTelemDecoder.log\n"
		        		+ "Perhaps the disk is full or the directory is not writable:\n" + Config.logFileDirectory + "\n\n"
		        				+ "You can reset FoxTelem by deleting the settings file (might want to back it up first):\n"
		        				+ Config.homeDirectory+ File.separator+"FoxTelem.properties");
		        System.exit(1);
			}
		}
		uncaughtExHandler = new Thread.UncaughtExceptionHandler() {
		    public void uncaughtException(Thread th, Throwable ex) {
		    	ex.printStackTrace(Log.getWriter());
		    	StringWriter sw = new StringWriter();
		    	PrintWriter pw = new PrintWriter(sw);
		    	ex.printStackTrace(pw);
		        Log.errorDialog("SERIOUS ERROR", "Uncaught exception.  You probablly need to restart FoxTelem:\n" + sw.toString());
		    }
		};
	}
	
	public static boolean getLogging() { return Config.logging; }
	public static void setLogging(boolean on) { Config.logging = on; }
	public static void setStdoutEcho(boolean on) { echoToStdout = on; }
	public static PrintWriter getWriter() { 
		if (output != null)
			return output;
		else
			return new PrintWriter(System.err);
	}
	
	public static void setGUILog(SourceTab panel) {
		logPanel = panel;
	}
	
	public static void print(String s) {
		if (Config.logging) {
			if (output == null) init();
			output.write(s);
			if (logPanel != null) logPanel.log(s);
			flush();
		
			if (echoToStdout) {
				System.out.print(s);
			}
		}
	}
	
	public static void println(String s) {
		if (Config.logging) {
			if (output == null) init();
			output.write(fileDateStamp() + s + System.getProperty("line.separator") );
			if (logPanel != null) logPanel.log(s);
			flush();
		
			if (echoToStdout) {
				System.out.println(s);
			}
		}
	}
	
	public static void errorDialog(String title, String message) {
		JOptionPane.showMessageDialog(MainWindow.frame,
				message.toString(),
				title,
			    JOptionPane.ERROR_MESSAGE) ;
	}
	
	public static String fileDateStamp() {	
		Date today = Calendar.getInstance().getTime();  
		fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = fileDateFormat.format(today) + ": ";
		return reportDate;
	}

	public static void flush() {
		if (Config.logging && output != null)
			output.flush();
	}
	
	public static void close() {
		if (Config.logging && output != null)
			output.close();
	}
}
