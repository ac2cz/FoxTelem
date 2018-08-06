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
	public static final DateFormat logDateName = new SimpleDateFormat("yyyyMMdd");
	public static SourceTab logPanel;
	public static String logFile = "FoxTelemDecoder";
	public static Thread.UncaughtExceptionHandler uncaughtExHandler;
	public static boolean showGuiDialogs = true;  // if true popup windows are shown for serious errors.  If false ALERTs are written.
	public static boolean alertsAreFatal = true;
	
	/**
	 * Initialise the logger and create a logfile with the passed name
	 * @param file
	 * @throws IOException 
	 */
	public static void init(String logFile) {
		
		Log.logFile = rollLog(logFile);
		if (Config.logging) {
			try {
				if (!Config.logFileDirectory.equalsIgnoreCase("")) {
					Log.logFile = Config.logFileDirectory + File.separator + Log.logFile;
				} 
				File aFile = new File(Log.logFile);
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
		    	
		    	String stacktrace = "";  
	            StackTraceElement[] elements = ex.getStackTrace();
	            int limit = 5;
	            for (int i=0; i< limit && i< elements.length; i++) {
	            	stacktrace =  stacktrace + elements[i] + "\n";
	            }
	            if (elements.length > limit)
	            	stacktrace = stacktrace + " ... " + (elements.length - limit) + " items not shown .... ";
		    	
		        Log.errorDialog("SERIOUS ERROR", "Uncaught exception.  You probablly need to restart FoxTelem:\n" + stacktrace);
		        if (!showGuiDialogs) // this is the server
		        	alert("Uncaught exception.  Need to clear the ALERT and restart the server:\n" + stacktrace);
		    }
		};
	}
	
	public static String rollLog(String logFile) {
		Date today = Calendar.getInstance().getTime();
		logDateName.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = logDateName.format(today);
		return logFile + reportDate + ".log";
	}
	
	public static void alert(String message) {
		try {
			println("ALERT: " + message); // put as last item in the log too
			String alertFile = Log.logFile + ".ALERT";
			File aFile = new File(alertFile);
			if(!aFile.exists()){
				aFile.createNewFile();
			}

			//use buffering and append to the existing file if it is there, though this should rarely be the case for an alert
			PrintWriter out = new PrintWriter(new FileWriter(aFile, true));
			out.write(fileDateStamp() + message + System.getProperty("line.separator") );
			out.flush();
			out.close();
			if (alertsAreFatal)
				System.exit(9);
		} catch (Exception e) { // catch all exceptions at this point, otherwise we can go into a loop
			System.err.println("FATAL ERROR: Cannot write log file: FoxTelemDecoder.log\n"
					+ "Perhaps the disk is full or the directory is not writable:\n" + Config.logFileDirectory);

			e.printStackTrace();
	        Log.errorDialog("FATAL ERROR", "Cannot write log file: FoxTelemDecoder.log\n"
	        		+ "Perhaps the disk is full or the directory is not writable:\n" + Config.logFileDirectory + "\n\n"
	        				+ "You can reset FoxTelem by deleting the settings file (might want to back it up first):\n"
	        				+ Config.homeDirectory+ File.separator+"FoxTelem.properties");
	        if (alertsAreFatal)
	        	System.exit(1);
		}
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
			if (output == null) init(logFile);
			output.write(s);
			if (logPanel != null) logPanel.log(s);
			flush();
		}
		if (echoToStdout) {
			System.out.print(s);
		}

	}

	public static void println(String s) {
		if (Config.logging) {
			if (output == null) init("FoxTelemDecoder");
			output.write(fileDateStamp() + s + System.getProperty("line.separator") );
			if (logPanel != null) logPanel.log(s);
			flush();
		}
		if (echoToStdout) {
			System.out.println(s);
		}

	}
	
	public static int optionYNdialog(String title, String message) {
		if (!showGuiDialogs)
			return 1; // Default is no if a dialog is called from server
		Object[] options = {"Yes",
        "No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message.toString(),
				title,
				JOptionPane.YES_NO_OPTION, 
			    JOptionPane.QUESTION_MESSAGE,
			    null,
			    options,
			    options[1]);
		return n;
	}
	
	public static void errorDialog(String title, String message) {
		dialog(title, message, JOptionPane.ERROR_MESSAGE );
	}
	
	public static void infoDialog(String title, String message) {
		dialog(title, message, JOptionPane.INFORMATION_MESSAGE );
	}
	
	private static void dialog(String title, String message, int type) {
		try {
		if (showGuiDialogs)
		JOptionPane.showMessageDialog(MainWindow.frame,
				message.toString(),
				title,
			    type) ;
		else Log.println(title + " " + message.toString());
		} catch (Exception e) {
			// catch all exceptions at this point, to avoid popping up messages in a loop
			System.err.println("FATAL ERROR: Cannot show dialog: " + title + "\n" + message + "\n");
			System.exit(1);
		}
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
