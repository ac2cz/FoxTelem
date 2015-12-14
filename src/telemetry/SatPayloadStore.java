package telemetry;


import gui.MainWindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

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
 *
 * A store of payloads for a single satellite, sorted by reset count and uptime.  The payloads are all loaded into memory when the program is
 * started.  Newly decoded payloads are stored in memory and written to disk imediately.  There is no save when the program
 * exits because all data has already been written.
 * 
 *
 */
public class SatPayloadStore {

	public int foxId;
	private Spacecraft fox;
	
	private static final int INIT_SIZE = 1000;
	private boolean initRad2 = false;
	
	// Primary Payloads
	public static String RT_LOG = "rttelemetry.log";
	public static String MAX_LOG = "maxtelemetry.log";
	public static String MIN_LOG = "mintelemetry.log";
	public static String RAD_LOG = "radtelemetry.log";

	// Secondary payloads - decoded from the primary payloads
	public static String RAD_TELEM_LOG = "radtelemetry2.log";

	public static String HERCI_LOG = "herciHSdata.log";
	public static String HERCI_HEADER_LOG = "herciHSheader.log";
	public static String HERCI_PACKET_LOG = "herciHSpackets.log";
	
	public String rtFileName;
	public String maxFileName;
	public String minFileName;
	public String radFileName;
	public String radTelemFileName;
	public String herciFileName;  // The High Speed HERCI records
	public String herciHeaderFileName; // Secondary Payload for HERCI Science Headers
	public String herciPacketFileName; // Secondary Payload for HERCI Mini Packets
	
	SortedFramePartArrayList rtRecords;
	SortedFramePartArrayList maxRecords;
	SortedFramePartArrayList minRecords;
	SortedFramePartArrayList radRecords;
	SortedFramePartArrayList radTelemRecords;
	SortedFramePartArrayList herciRecords;
	SortedFramePartArrayList herciHeaderRecords;
	SortedFramePartArrayList herciPacketRecords;
	
	boolean updatedRt = false;
	boolean updatedMax = false;
	boolean updatedMin = false;
	boolean updatedRad = false;
	boolean updatedRadTelem = false;
	boolean updatedHerci = false;
	boolean updatedHerciHeader = false;
	boolean updatedHerciPacket = false;
	
	public static final int MAX_RAD_DATA_LENGTH = 61;
	public static final int MAX_HERCI_PACKET_DATA_LENGTH = 128;
	public static final int MAX_HERCI_HK_DATA_LENGTH = 27; // 25 fields plus the 2 fields needed for Reset and Uptime
	
	/**
	 * Create the payload store this this fox id
	 * @param id
	 */
	public SatPayloadStore(int id) {
		foxId = id;
		fox = Config.satManager.getSpacecraft(foxId);
		initPayloadFiles();
		try {
			rtFileName = "Fox"+id+RT_LOG;
			maxFileName = "Fox"+id+MAX_LOG;
			minFileName = "Fox"+id+MIN_LOG;
			radFileName = "Fox"+id+RAD_LOG;
			radTelemFileName = "Fox"+id+RAD_TELEM_LOG;
			load(rtFileName);
			load(maxFileName);
			load(minFileName);
			load(radTelemFileName);
			load(radFileName);
			if (Config.satManager.hasHerci(foxId)) {
				herciHeaderFileName = "Fox"+id+HERCI_HEADER_LOG;
				load(herciHeaderFileName);
				herciPacketFileName = "Fox"+id+HERCI_PACKET_LOG;
				load(herciPacketFileName);
				herciFileName = "Fox"+id+HERCI_LOG;
				load(herciFileName);
			}

		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					 "You may need to reset FoxTelem.properties or re-install FoxTelem\n"
								+ "Was the data directory moved?\n" + e.toString(),
					"FATAL! Cannot Load the Stored Payload data",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		}
	}
	
	private void initPayloadFiles() {
		rtRecords = new SortedFramePartArrayList(INIT_SIZE);
		maxRecords = new SortedFramePartArrayList(INIT_SIZE);
		minRecords = new SortedFramePartArrayList(INIT_SIZE);
		radRecords = new SortedFramePartArrayList(INIT_SIZE);
		radTelemRecords = new SortedFramePartArrayList(INIT_SIZE);
		if (Config.satManager.hasHerci(foxId)) {
			herciRecords = new SortedFramePartArrayList(INIT_SIZE);
			herciHeaderRecords = new SortedFramePartArrayList(INIT_SIZE);
			herciPacketRecords = new SortedFramePartArrayList(INIT_SIZE);
		}
	}
	
	public void setUpdatedAll() {
		updatedRt = true;
		updatedMax = true;
		updatedMin = true;
		updatedRad = true;
		updatedRadTelem = true;
		updatedHerci = true;
		updatedHerciHeader = true;
		updatedHerciPacket = true;
	}
	
	public boolean getUpdatedRt() { return updatedRt; }
	public void setUpdatedRt(boolean u) {
		updatedRt = u;
	}
	public boolean getUpdatedMax() { return updatedMax; }
	public void setUpdatedMax(boolean u) {
		updatedMax = u;
	}

	public boolean getUpdatedMin() { return updatedMin; }
	public void setUpdatedMin(boolean u) {
		updatedMin = u;
	}
	public boolean getUpdatedRad() { return updatedRad; }
	public void setUpdatedRad(boolean u) {
		updatedRad = u;
	}

	public boolean getUpdatedRadTelem() { return updatedRadTelem; }
	public void setUpdatedRadTelem(boolean u) {
		updatedRadTelem = u;
	}

	public boolean getUpdatedHerci() { return updatedHerci; }
	public void setUpdatedHerci(boolean u) {
		updatedHerci = u;
	}

	public boolean getUpdatedHerciHeader() { return updatedHerciHeader; }
	public void setUpdatedHerciHeader(boolean u) {
		updatedHerciHeader = u;
	}

	public boolean getUpdatedHerciPacket() { return updatedHerciPacket; }
	public void setUpdatedHerciPacket(boolean u) {
		updatedHerciPacket = u;
	}
	
	public int getNumberOfFrames() {
		int herci = 0;
		if (Config.satManager.hasHerci(foxId))
			herci = herciRecords.size();
		return herci + rtRecords.size() + maxRecords.size() + minRecords.size() + radRecords.size(); 
	}
	public int getNumberOfTelemFrames() { return rtRecords.size() + maxRecords.size() + minRecords.size(); }
	public int getNumberOfRadFrames() { return radRecords.size(); }
	public int getNumberOfHerciFrames() { return herciRecords.size(); }
		
	public boolean add(int id, long uptime, int resets, FramePart f) throws IOException {
		f.captureHeaderInfo(id, uptime, resets);
		return add(f);
	}

	/**
	 * Add an array of payloads, usually when we have a set of radiation data from the high speed
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	public boolean add(int id, long uptime, int resets, PayloadRadExpData[] f) throws IOException {
		if (!radRecords.hasFrame(id, uptime, resets)) {
			for (int i=0; i< f.length; i++) {
				if (f[i].hasData()) {
					f[i].captureHeaderInfo(id, uptime, resets);
					updatedRad = true;
					try {
						save(f[i], radFileName);
					} catch (IOException e) {
						// NEED TO SET A FLAG HERE THAT IS THEN SEEN BY THE GUI WHEN IT POLLS FOR RESULTS
						e.printStackTrace(Log.getWriter());
					}
					addRadRecord(f[i]);
				}
			}
		} else {
			if (Config.debugFrames) Log.println("DUPLICATE RAD RECORD SET, not loaded");
			return false;
		}
		return true;
	}

	private boolean addRadRecord(PayloadRadExpData f) throws IOException {
		radRecords.add(f);
		
		// Capture and store any secondary payloads
		if (fox.hasHerci() || f.isTelemetry()) {
			RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
			radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
			add(radiationTelemetry);
			updatedRadTelem = true;
		}
		return true;
	}

	/**
	 * Add a HERCI High Speed payload record
	 * @param f
	 * @return
	 * @throws IOException
	 */
	private boolean addHerciRecord(PayloadHERCIhighSpeed f) throws IOException {
		herciRecords.add(f);
		
		// Capture and store any secondary payloads
		
		HerciHighspeedHeader radiationTelemetry = f.calculateTelemetryPalyoad();
		radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
		add(radiationTelemetry);
		updatedHerciHeader = true;

		ArrayList<HerciHighSpeedPacket> pkts = f.calculateTelemetryPackets();
		for(int i=0; i< pkts.size(); i++) {
			HerciHighSpeedPacket pk = pkts.get(i);
			pk.captureHeaderInfo(f.id, f.uptime, f.resets);
			add(pk);
			updatedHerciPacket = true;

		}
		return true;
	}

	/**
	 * Add the frame to the correct array and file
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	private boolean add(FramePart f) throws IOException {
		if (f instanceof PayloadRtValues ) {
			if (!rtRecords.hasFrame(f.id, f.uptime, f.resets)) {
				updatedRt = true;
				
					save(f, rtFileName);
				
				return rtRecords.add(f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE RECORD, not loaded");
			}
		} else if (f instanceof PayloadMaxValues  ) {
			if (!maxRecords.hasFrame(f.id, f.uptime, f.resets)) {
				updatedMax = true;
				
					save(f, maxFileName);
				
				return maxRecords.add(f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE MAX RECORD, not loaded");
			}
		} else if (f instanceof PayloadMinValues ) {
			if (!minRecords.hasFrame(f.id, f.uptime, f.resets)) {
				updatedMin = true;
				
					save(f, minFileName);
				
				return minRecords.add(f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE MIN RECORD, not loaded");
			}
		} else if (f instanceof PayloadRadExpData ) {
			if (!radRecords.hasFrame(f.id, f.uptime, f.resets, f.type)) {
				updatedRad = true;
					save(f, radFileName);				
				return addRadRecord((PayloadRadExpData)f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE RAD RECORD, not loaded");
			}
		} else if (f instanceof RadiationTelemetry ) {
			if (!radTelemRecords.hasFrame(f.id, f.uptime, f.resets, f.type)) {
				updatedRadTelem = true;
					save(f, radTelemFileName);				
				return radTelemRecords.add(f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE RAD TELEM RECORD, not loaded");
			}
		} else if (f instanceof PayloadHERCIhighSpeed ) {
			if (!herciRecords.hasFrame(f.id, f.uptime, f.resets, f.type)) {
				updatedHerci = true;
				save(f, herciFileName);				
				return addHerciRecord((PayloadHERCIhighSpeed)f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE HERCI RECORD, not loaded");
			}
		} else if (f instanceof HerciHighspeedHeader ) {
			if (!herciHeaderRecords.hasFrame(f.id, f.uptime, f.resets, f.type)) {
				updatedHerciHeader = true;
					save(f, herciHeaderFileName);				
				return herciHeaderRecords.add(f);
			} else {
				if (Config.debugFrames) Log.println("DUPLICATE HERCI HS HEADER RECORD, not loaded");
			}
		} else if (f instanceof HerciHighSpeedPacket ) {
			//if (!herciPacketRecords.hasFrame(f.id, f.uptime, f.resets, f.type)) {
			// FIXME - Duplicate check needs to be based on packet sequence and not Fox Timebase
				updatedHerciPacket = true;
					save(f, herciPacketFileName);				
				return herciPacketRecords.add(f);
			//} else {
			//	if (Config.debugFrames) Log.println("DUPLICATE HERCI HS PACKET RECORD, not loaded");
			//}
		}
		return false;
	}
		
	public PayloadRtValues getLatestRt() {
		if (rtRecords.size() == 0) return null;
		return (PayloadRtValues) rtRecords.get(rtRecords.size()-1);
	}

	public PayloadMaxValues getLatestMax() {
		if (maxRecords.size() == 0) return null;
		return (PayloadMaxValues) maxRecords.get(maxRecords.size()-1);
	}

	public PayloadMinValues getLatestMin() {
		if (minRecords.size() == 0) return null;
		return (PayloadMinValues) minRecords.get(minRecords.size()-1);
	}

	public PayloadRadExpData getLatestRad() {
		if (radRecords.size() == 0) return null;
		return (PayloadRadExpData) radRecords.get(radRecords.size()-1);
	}

	public RadiationTelemetry getLatestRadTelem() {
		if (radTelemRecords.size() == 0) return null;
		return (RadiationTelemetry) radTelemRecords.get(radTelemRecords.size()-1);
	}

	public PayloadHERCIhighSpeed getLatestHerci() {
		if (herciRecords.size() == 0) return null;
		return (PayloadHERCIhighSpeed) herciRecords.get(herciRecords.size()-1);
	}

	public HerciHighspeedHeader getLatestHerciHeader() {
		if (herciHeaderRecords.size() == 0) return null;
		return (HerciHighspeedHeader) herciHeaderRecords.get(herciHeaderRecords.size()-1);
	}


	/**
	 * Try to return an array with "period" entries for this attribute, starting with the most 
	 * recent
	 * 
	 * @param name
	 * @param period
	 * @return
	 */
	public double[][] getRtGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) {
		return getGraphData(rtRecords, name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) {
		return getGraphData(maxRecords, name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) {
		return getGraphData(minRecords, name, period, id, fromReset, fromUptime);
		
	}

	/**
	 * Get data for a single named field from the Radiation Telemetry file for the period given
	 * @param name
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public double[][] getRadTelemGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) {
		return getGraphData(radTelemRecords, name, period, id, fromReset, fromUptime);
		
	}

	public String[][] getRadData(int period, int id, int fromReset, long fromUptime) {
		return getRadData(radRecords, period, id, fromReset, fromUptime, MAX_RAD_DATA_LENGTH);

	}
	
	/**
	 * Get a set of Radiation Telemetry records for the range given
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime) {
		return getRadData(radTelemRecords, period, id, fromReset, fromUptime, MAX_HERCI_HK_DATA_LENGTH); 

	}

	/**
	 * Get data for a single named field from the HERCI Science Header for the period given
	 * @param name
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public double[][] getHerciScienceHeaderGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) {
		return getGraphData(herciHeaderRecords, name, period, id, fromReset, fromUptime);
		
	}

	/**
	 * Get a set of HERCI Mini Packets for the range given
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime) {
		return getRadData(herciPacketRecords, period, id, fromReset, fromUptime, MAX_HERCI_PACKET_DATA_LENGTH); // FIXME - LENGTH NOT CORECT

	}

	/**
	 * Return an array of radiation data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 */
	public String[][] getRadData(SortedFramePartArrayList records, int period, int id, int fromReset, long fromUptime, int length) {
		int start = 0;
		int end = 0;
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			start = records.size()-period;
			end = records.size();
		} else {
			// we need to find the start point
			start = records.getNearestFrameIndex(id, fromUptime, fromReset);
			if (start == -1 ) start = records.size()-period;
			end = start + period;
		}
		if (end > records.size()) end = records.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > records.size()) start = records.size();
		
		int[][] results = new int[end-start][];
		String[] upTime = new String[end-start];
		String[] resets = new String[end-start];
		
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			results[j] = records.get(i).getFieldValues();
			upTime[j] = ""+records.get(i).getUptime();
			resets[j--] = ""+records.get(i).getResets();
		}
		
		String[][] resultSet = new String[end-start][length];
		for (int r=0; r< end-start; r++) {
			resultSet[r][0] = resets[r];
			resultSet[r][1] = upTime[r];
			for (int k=0; k<results[r].length; k++)
				resultSet[r][k+2] = ""+results[r][k];
		}
		
		return resultSet;
	}

	protected String getRtUTCFromUptime(int reset, long uptime) {
		int idx = rtRecords.getFrameIndex(foxId, uptime, reset);
		if (idx != -1) {
			return rtRecords.get(idx).getCaptureDate();
		}
		return null;
		
	}
	private double[][] getGraphData(SortedFramePartArrayList records, String name, int period, Spacecraft fox, int fromReset, long fromUptime) {

		int start = 0;
		int end = 0;
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			start = records.size()-period;
			end = records.size();
		} else {
			// we need to find the start point
			start = records.getNearestFrameIndex(fox.foxId, fromUptime, fromReset);
			if (start == -1 ) start = records.size()-period;
			end = start + period;
		}
		if (end > records.size()) end = records.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > records.size()) start = records.size();
		double[] results = new double[end-start];
		double[] upTime = new double[end-start];
		double[] resets = new double[end-start];
		
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			if (Config.displayRawValues)
				results[j] = records.get(i).getRawValue(name);
			else
				results[j] = records.get(i).getDoubleValue(name, fox);
			upTime[j] = records.get(i).getUptime();
			resets[j--] = records.get(i).getResets();
		}
		
		double[][] resultSet = new double[3][end-start];
		resultSet[PayloadStore.DATA_COL] = results;
		resultSet[PayloadStore.UPTIME_COL] = upTime;
		resultSet[PayloadStore.RESETS_COL] = resets;
		
		return resultSet;
	}
	
	/**
	 * Load a payload file from disk
	 * Payload files are stored in seperate logs, but this routine is written so that it can load mixed records
	 * from a single file
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
        			int resets = Integer.valueOf(st.nextToken()).intValue();
        			long uptime = Long.valueOf(st.nextToken()).longValue();
        			int type = Integer.valueOf(st.nextToken()).intValue();
        			
        			// We should never get this situation, but good to check..
        			if (Config.satManager.getSpacecraft(id) == null) {
        				Log.errorDialog("FATAL", "Attempting to Load payloads from the Payload store for satellite with Fox Id: " + id 
        						+ "\n when no sattellite with that FoxId is configured.  Add this spacecraft to the satellite directory and restart FoxTelem."
        						+ "\nProgram will now exit");
        				System.exit(1);
        			}
        			if (type == FramePart.TYPE_REAL_TIME) {
        				PayloadRtValues rt = new PayloadRtValues(id, resets, uptime, date, st, Config.satManager.getRtLayout(id));
        				rtRecords.add(rt);
        				updatedRt = true;
        			} else
        			if (type == FramePart.TYPE_MAX_VALUES) {
        				PayloadMaxValues rt = new PayloadMaxValues(id, resets, uptime, date, st, Config.satManager.getMaxLayout(id));
        				maxRecords.add(rt);
        				updatedMax = true;
        			} else
        			if (type == FramePart.TYPE_MIN_VALUES) {
        				PayloadMinValues rt = new PayloadMinValues(id, resets, uptime, date, st, Config.satManager.getMinLayout(id));
        				minRecords.add(rt);
        				updatedMin = true;
        			}
        			if (type == FramePart.TYPE_RAD_TELEM_DATA || type >= 700 && type < 800) {
        				RadiationTelemetry rt = new RadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getRadTelemLayout(id));
        				radTelemRecords.add(rt);
        				updatedRadTelem = true;
        			}
        			if (type == FramePart.TYPE_RAD_EXP_DATA || type >= 400 && type < 500) {
        				PayloadRadExpData rt = new PayloadRadExpData(id, resets, uptime, date, st);
        				radRecords.add(rt);
        				updatedRad = true;
        				// Capture and store any secondary payloads, this is duplicative but thorough
        				if (initRad2) // this step really slows things down, so only enable if needed
            			if (fox.hasHerci() || rt.isTelemetry()) {
        					RadiationTelemetry radiationTelemetry = rt.calculateTelemetryPalyoad();
        					radiationTelemetry.captureHeaderInfo(rt.id, rt.uptime, rt.resets);
        					add(radiationTelemetry);
        				}
        			}
        			
        			if (type == FramePart.TYPE_HERCI_HIGH_SPEED_DATA) {
        				PayloadHERCIhighSpeed rt = new PayloadHERCIhighSpeed(id, resets, uptime, date, st, Config.satManager.getHerciHSLayout(id));
        				herciRecords.add(rt);
        				updatedHerci = true;
    					HerciHighspeedHeader headerRec = rt.calculateTelemetryPalyoad();
    					headerRec.captureHeaderInfo(rt.id, rt.uptime, rt.resets);
    					add(headerRec);
    					updatedHerciHeader = true;
    		    		ArrayList<HerciHighSpeedPacket> pkts = rt.calculateTelemetryPackets();
      					for(int i=0; i< pkts.size(); i++) {
      						HerciHighSpeedPacket pk = pkts.get(i);
      						pk.captureHeaderInfo(rt.id, rt.uptime, rt.resets);
      						add(pk);
      					}
    					updatedHerciPacket = true;
        			}
        		}
        	}
        	dis.close();
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());
        	
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        }
        
	}

	/**
	 * Save a payload to the log file
	 * @param frame
	 * @param log
	 * @throws IOException
	 */
	public void save(FramePart frame, String log) throws IOException {
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
			output.write( frame.toFile() + "\n" );
		
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
				remove(dir+maxFileName);
				remove(dir+minFileName);
				remove(dir+radFileName);
				remove(dir+radTelemFileName);
				remove(dir+herciFileName);
				remove(dir+herciHeaderFileName);
				remove(dir+herciPacketFileName);
				initPayloadFiles();
				setUpdatedAll();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						ex.toString(),
						"Error Deleting Payload Files for FoxId:"+foxId+", check permissions",
						JOptionPane.ERROR_MESSAGE) ;
			}

	}
	
	/**
	 * Utility function to copy a file
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
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

