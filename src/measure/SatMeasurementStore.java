package measure;

import gui.MainWindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import telemetry.PayloadStore;
import common.Config;
import common.Log;
import common.FoxSpacecraft;

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
 */
public class SatMeasurementStore {

	private static final int INIT_SIZE = 1000;
	
	// These types need to be unique compared to the Type in FramePart so that we can graph things in different ways
	public static final int RT_MEASUREMENT_TYPE = -1;
	public static final int PASS_MEASUREMENT_TYPE = -2;
	public static final int UTC_COL = 3;
	
	public int foxId;
	
	public static String PASS_LOG = "passmeasurements2.log";
	public static String OLD_PASS_LOG = "passmeasurements.log";
	public static String RT_LOG = "rtmeasurements.log";
	
	public String rtFileName;
	public String passFileName;
	
	SortedMeasurementArrayList rtRecords;
	SortedMeasurementArrayList passRecords;
	
	boolean updatedRt = false;
	boolean updatedPass = false;

	/**
	 * Create the payload store this this fox id
	 * @param id
	 */
	public SatMeasurementStore(int id) {
		foxId = id;
		initArrays();

		try {
			rtFileName = "Fox"+id+RT_LOG;
			passFileName = "Fox"+id+PASS_LOG;
			
			
			String testFile = passFileName;
			if (!Config.logFileDirectory.equalsIgnoreCase("")) {
				testFile = Config.logFileDirectory + File.separator + testFile;
			}
			File aFile = new File(testFile);
			if(!aFile.exists()){
				// then we need to convert to the new format in 1.04 and beyond
				try {
					convertPassMeasures();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(MainWindow.frame,
							e.toString(),
							"ERROR converting old pass measurements",
							JOptionPane.ERROR_MESSAGE) ;
					e.printStackTrace(Log.getWriter());
				}
			} else {
				load(passFileName, false);
			}
			
			load(rtFileName, false);
			
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"ERROR Loading Stored Payload data",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
		}
	}

	private void initArrays() {
		rtRecords = new SortedMeasurementArrayList(INIT_SIZE);
		passRecords = new SortedMeasurementArrayList(INIT_SIZE);
		
	}

	public boolean getUpdatedMeasurement() { return updatedRt; }
	public void setUpdatedMeasurement(boolean u) {
		updatedRt = u;
	}
	public boolean getUpdatedPassMeasurement() { return updatedPass; }
	public void setUpdatedPassMeasurement(boolean u) {
		updatedPass = u;
	}
	public void setUpdatedAll() {
		updatedRt = true;
		updatedPass = true;
	}

	/**
	 * Add a measurement set to this Sats store
	 * @param id
	 * @param m
	 * @return
	 * @throws IOException
	 */
	public boolean add(int id, Measurement m) throws IOException {
		if (m instanceof RtMeasurement) {
			try {
				save(m, rtFileName);
			} catch (IOException e) {
				// NEED TO SET A FLAG HERE THAT IS THEN SEEN BY THE GUI WHEN IT POLLS FOR RESULTS
				e.printStackTrace(Log.getWriter());
			}
			rtRecords.add(m);
			updatedRt = true;
			return true;
		} else if (m instanceof PassMeasurement) {
			try {
				save(m, passFileName);
			} catch (IOException e) {
				// NEED TO SET A FLAG HERE THAT IS THEN SEEN BY THE GUI WHEN IT POLLS FOR RESULTS
				e.printStackTrace(Log.getWriter());
			}
			passRecords.add(m);
			updatedPass = true;
			return true;	
		}
		return false;
	}


	public RtMeasurement getLatestMeasurement() {
		if (rtRecords.size() == 0) return null;
		return (RtMeasurement) rtRecords.get(rtRecords.size()-1);
	}

	public PassMeasurement getLatestPassMeasurement() {
		if (passRecords.size() == 0) return null;
		return (PassMeasurement) passRecords.get(passRecords.size()-1);
	}

	public double[][] getMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime) {
		return getGraphData(rtRecords, name, period, fox, fromReset, fromUptime);
	}
	
	public double[][] getPassMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime) {
		return getGraphData(passRecords, name, period, fox, fromReset, fromUptime);
	}
	
	public double[][] getGraphData(SortedMeasurementArrayList rtRecords, String name, int period, FoxSpacecraft fox, int fromReset, long fromUptime) {

		int start = 0;
		int end = 0;
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take rtRecords nearest the end
			start = rtRecords.size()-period;
			end = rtRecords.size();
		} else {
			// we need to find the start point
			start = rtRecords.getNearestFrameIndex(fox.foxId, fromUptime, fromReset);
			if (start == -1 ) start = rtRecords.size()-period;
			end = start + period;
		}
		if (end > rtRecords.size()) end = rtRecords.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > rtRecords.size()) start = rtRecords.size();
		double[] results = new double[end-start];
		double[] upTime = new double[end-start];
		double[] resets = new double[end-start];
		double[] dates = new double[end-start];
		
		//RtMeasurement m = new RtMeasurement(fox.rtLayout);
		
		int validRecords = 0;
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			
			Measurement m = rtRecords.get(i);
			double result;
			if (m instanceof RtMeasurement )
				result = ((RtMeasurement)rtRecords.get(i)).getRawValue(name);
			else
				result = ((PassMeasurement)rtRecords.get(i)).getRawValue(name);
//			if ( (Double.compare(result, 0.0d) != 0) || !((RtMeasurement)rtRecords.get(i)).zeroIsNull(name) ) { 
			  // screening out zero values because they corrupt the freq graph but breaks FEC plots
//				System.out.println("IN:" + result);
			results[j] = result;
			upTime[j] = rtRecords.get(i).uptime;
			resets[j] = rtRecords.get(i).reset;
			dates[j--] = rtRecords.get(i).date.getTime();
			validRecords++;
//			}
		}
//		System.out.println("ValidRecords:" + validRecords);
		// We have results in the result set from results.length-1-validRecords to results.length-1
//		double[][] resultSet = new double[4][end-start];
		double[][] resultSet = new double[4][validRecords];
		for (int i=j+1; i< results.length; i++) {
//			System.out.println("OUT:" + results[i]);
		resultSet[PayloadStore.DATA_COL][i-j-1] = results[i];
		resultSet[PayloadStore.UPTIME_COL][i-j-1] = upTime[i];
		resultSet[PayloadStore.RESETS_COL][i-j-1] = resets[i];
		resultSet[SatMeasurementStore.UTC_COL][i-j-1] = dates[i];
		}
//		System.out.println("ResultSet:" + resultSet[PayloadStore.DATA_COL].length);
		return resultSet;
	}

	/**
	 * Load a file from disk
	 * @param log
	 * @throws FileNotFoundException
	 */
	public void load(String log, boolean load103) throws FileNotFoundException {
        String line;
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
		}
        Log.println("Loading: " + log);
        File aFile = new File(log );
		if(!aFile.exists()){
			try {
				aFile.createNewFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						e.toString(),
						"ERROR creating file " + log,
						JOptionPane.ERROR_MESSAGE) ;
				e.printStackTrace(Log.getWriter());
			}
		}
 
        BufferedReader dis = new BufferedReader(new FileReader(log));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			StringTokenizer st = new StringTokenizer(line, ",");
        			String date = st.nextToken();
        			int id = Integer.valueOf(st.nextToken()).intValue();
          			int reset = Integer.valueOf(st.nextToken()).intValue();
          			long uptime = Long.valueOf(st.nextToken()).longValue();
          		    int type = Integer.valueOf(st.nextToken()).intValue();
        			
        			// We should never get this situation, but good to check..
        			if (Config.satManager.getSpacecraft(id) == null) {
        				Log.errorDialog("FATAL", "Attempting to Load payloads from the Payload store for satellite with Fox Id: " + id 
        						+ "\n when no sattellite with that FoxId is configured.  Add this spacecraft to the satellite directory and restart FoxTelem."
        						+ "\nProgram will now exit");
        				System.exit(1);
        			}
        			if (type == RT_MEASUREMENT_TYPE || type == 1) {  // 1 is the legacy type
        				RtMeasurement rt = new RtMeasurement(id, date, reset, uptime, RT_MEASUREMENT_TYPE, st);
        				rtRecords.add(rt);
        				updatedRt = true;
        			}
        			if (type == PASS_MEASUREMENT_TYPE || type == 2) {  // 2 is the legacy type
        				PassMeasurement rt = null;
        				if (load103) {
        					rt = new PassMeasurement(id, date, reset, uptime, PASS_MEASUREMENT_TYPE, null);
        					rt.load103(st);
        				} else {
        					rt = new PassMeasurement(id, date, reset, uptime, PASS_MEASUREMENT_TYPE, st);
        				}
        				passRecords.add(rt);
        				updatedPass = true;
        			}
        		}
        	}
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        } catch (NoSuchElementException n) {
        	// File is corrupt, so better tell the user
        	Log.errorDialog("FATAL: CORRUPT FILE", "Can not load logfile, it appears to be corrupt.  You need to fix or remove the file for FoxTelem to load\n"
        			+ "Log: " + log);
        	n.printStackTrace(Log.getWriter());
        	System.exit(1);
        } finally {
        	try {
				dis.close();
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());
			}
        }
        
	}

	/**
	 * Save a payload to the log file
	 * @param frame
	 * @param log
	 * @throws IOException
	 */
	public void save(Measurement measurement, String log) throws IOException {
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
		} 
		File aFile = new File(log );
		if(!aFile.exists()){
			aFile.createNewFile();
		}
		//Log.println("Saving: " + log);
		//use buffering and append to the existing file
		Writer output = new BufferedWriter(new FileWriter(aFile, true));
		try {
			if (measurement instanceof RtMeasurement)
				output.write( ((RtMeasurement)measurement).toFile() + "\n" );		
			else
				output.write( ((PassMeasurement)measurement).toFile() + "\n" );		
			output.flush();
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}
		
	}

	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
		String dir = "";
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator ;
			//System.err.println("Loading: "+log);
		}
			try {
				remove(dir+rtFileName);
				remove(dir+passFileName);
				initArrays();
				setUpdatedAll();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						ex.toString(),
						"Error Deleting Measurement Files for FoxId:"+foxId+", check permissions",
						JOptionPane.ERROR_MESSAGE) ;
			}

	}
	
	/**
	 * Remove a log file from disk and report any errors.
	 * @param f
	 * @throws IOException
	 */
	public static void remove(String f) throws IOException {
		try {
//	        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
//				f = Config.logFileDirectory + File.separator + f;
//				//System.err.println("Loading: "+log);
//			}
	        File file = new File(f);
	        if (file.exists())
	        	if(file.delete()){
	        		Log.println(file.getName() + " is deleted!");
	        	}else{
	        		Log.println("Delete operation failed for: "+ file.getName());
	        		throw new IOException("Could not delete file " + file.getName() + " Check the file system and remove it manually.");
	        	}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					ex.toString(),
					"Error Deleting File",
					JOptionPane.ERROR_MESSAGE) ;
		}
	}

	/**
	 * Convert from passMeasurement in 1.03 to 1.04
	 * Only the dates pose an issue. These have been saved as strings and need to be converted into long
	 * @throws IOException
	 */ 
	public void convertPassMeasures() throws IOException {
		String dir = "";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator;
		} 
        String oldlog = "Fox"+foxId+OLD_PASS_LOG;
        String log = "Fox"+foxId+PASS_LOG;
		Log.println("CONVERTING " + oldlog + " to " + log);
		File aFile = new File(dir + oldlog );
		if(aFile.exists()){
			// then convert it
			load(oldlog, true);
			Log.println("Loaded: " + this.passRecords.size() + " records");
			for(Measurement m: this.passRecords) {
				PassMeasurement p = (PassMeasurement)m;
				save(p,log);	
			}
		}
	}
	
}
