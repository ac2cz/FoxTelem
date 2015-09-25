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
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import telemetry.PayloadStore;
import common.Config;
import common.Log;
import common.Spacecraft;

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
	public static final int RT_MEASUREMENT_TYPE = 1;
	public static final int PASS_MEASUREMENT_TYPE = 2;
	
	public int foxId;
	
	public static String PASS_LOG = "passmeasurements.log";
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
			load(rtFileName);
			load(passFileName);
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

	public double[][] getMeasurementGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) {

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
		
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtrtRecords.size());
			results[j] = ((RtMeasurement)rtRecords.get(i)).getRawValue(name);
			upTime[j] = rtRecords.get(i).uptime;
			resets[j] = rtRecords.get(i).reset;
			dates[j--] = rtRecords.get(i).date.getTime();
		}
		
		double[][] resultSet = new double[4][end-start];
		resultSet[PayloadStore.DATA_COL] = results;
		resultSet[PayloadStore.UPTIME_COL] = upTime;
		resultSet[PayloadStore.RESETS_COL] = resets;
		resultSet[PayloadStore.UTC_COL] = dates;
		return resultSet;
	}

	/**
	 * Load a file from disk
	 * @param log
	 * @throws FileNotFoundException
	 */
	public void load(String log) throws FileNotFoundException {
        String line;
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
			Log.println("Loading: " + log);
		}
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
        			if (type == RT_MEASUREMENT_TYPE) {
        				RtMeasurement rt = new RtMeasurement(id, date, reset, uptime, type, st);
        				rtRecords.add(rt);
        				updatedRt = true;
        			}
        			if (type == PASS_MEASUREMENT_TYPE) {
        				PassMeasurement rt = new PassMeasurement(id, date, reset, uptime, type, st);
        				passRecords.add(rt);
        				updatedPass = true;
        			}
        		}
        	}
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());
        	
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
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

}
