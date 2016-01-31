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
	//private boolean initRad2 = false;
	
	// Primary Payloads
	public static String RT_LOG = "rttelemetry";
	public static String MAX_LOG = "maxtelemetry";
	public static String MIN_LOG = "mintelemetry";
	public static String RAD_LOG = "radtelemetry";

	// Secondary payloads - decoded from the primary payloads
	public static String RAD_TELEM_LOG = "radtelemetry2";

	public static String HERCI_LOG = "herciHSdata";
	public static String HERCI_HEADER_LOG = "herciHSheader";
	public static String HERCI_PACKET_LOG = "herciHSpackets";
		
	SatPayloadTable rtRecords;
	SatPayloadTable maxRecords;
	SatPayloadTable minRecords;
	SatPayloadTable radRecords;
	SatPayloadTable radTelemRecords;
	SatPayloadTable herciRecords;
	SatPayloadTable herciHeaderRecords;
	SatPayloadTable herciPacketRecords;
	
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
		
		try {
			initPayloadFiles();
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					 "You may need to reset FoxTelem.properties or re-install FoxTelem\n"
								+ "Was the data directory moved?\n" + e.toString(),
					"FATAL! Cannot find the Stored Payload data",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					 "You may need to reset FoxTelem.properties or re-install FoxTelem\n"
								+ "Was the data directory moved?\n" + e.toString(),
					"FATAL! Cannot Load the Stored Payload data",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		}
	}
	
	private void initPayloadFiles() throws IOException {
		rtRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+RT_LOG);
		maxRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+MAX_LOG);
		minRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+MIN_LOG);
		radRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+RAD_LOG);
		radTelemRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+RAD_TELEM_LOG);
		if (Config.satManager.hasHerci(foxId)) {
			herciRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+HERCI_LOG);
			herciHeaderRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+HERCI_HEADER_LOG);
			herciPacketRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+HERCI_PACKET_LOG);
		}
	}
	
	public void setUpdatedAll() {
		rtRecords.setUpdated(true);
		maxRecords.setUpdated(true);
		minRecords.setUpdated(true);
		radRecords.setUpdated(true);
		radTelemRecords.setUpdated(true);
		if (Config.satManager.hasHerci(foxId)) {
			herciRecords.setUpdated(true);
			herciHeaderRecords.setUpdated(true);
			herciPacketRecords.setUpdated(true);
		}
	}
	
	public boolean getUpdatedRt() { return rtRecords.getUpdated(); }
	public void setUpdatedRt(boolean u) { rtRecords.setUpdated(u); }
	public boolean getUpdatedMin() { return minRecords.getUpdated(); }
	public void setUpdatedMin(boolean u) { minRecords.setUpdated(u); }
	public boolean getUpdatedMax() { return maxRecords.getUpdated(); }
	public void setUpdatedMax(boolean u) { maxRecords.setUpdated(u); }
	public boolean getUpdatedRad() { return radRecords.getUpdated(); }
	public void setUpdatedRad(boolean u) { radRecords.setUpdated(u); }
	public boolean getUpdatedRadTelem() { return radTelemRecords.getUpdated(); }
	public void setUpdatedRadTelem(boolean u) { radTelemRecords.setUpdated(u); }
	public boolean getUpdatedHerci() { return herciRecords.getUpdated(); }
	public void setUpdatedHerci(boolean u) { herciRecords.setUpdated(u); }
	public boolean getUpdatedHerciHeader() { return herciHeaderRecords.getUpdated(); }
	public void setUpdatedHerciHeader(boolean u) { herciHeaderRecords.setUpdated(u); }
	public boolean getUpdatedHerciPacket() { return herciPacketRecords.getUpdated(); }
	public void setUpdatedHerciPacket(boolean u) { herciPacketRecords.setUpdated(u); }
		
	public int getNumberOfFrames() {
		int herci = 0;
		if (Config.satManager.hasHerci(foxId))
			herci = herciRecords.getSize();
		return herci + rtRecords.getSize() + maxRecords.getSize() + minRecords.getSize() + radRecords.getSize(); 
	}
	public int getNumberOfTelemFrames() { return rtRecords.getSize() + maxRecords.getSize() + minRecords.getSize(); }
	public int getNumberOfRadFrames() { return radRecords.getSize(); }
	public int getNumberOfHerciFrames() { return herciRecords.getSize(); }
		
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
					radRecords.setUpdated(true);
					try {
						radRecords.save(f[i]);
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
		
		// Capture and store any secondary payloads
		if (fox.hasHerci() || f.isTelemetry()) {
			RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
			radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
			if (f.type >= 400) // this is a high speed record
				radiationTelemetry.type = f.type + 300; // we give the telem record 700+ type
			add(radiationTelemetry);
			radTelemRecords.setUpdated(true);
		}
		return true;
	}

	/**
	 * Add a HERCI High Speed payload record
	 * @param f
	 * @return
	 * @throws IOException
	 */
	protected boolean addHerciRecord(PayloadHERCIhighSpeed f) throws IOException {
		//herciRecords.add(f);
		
		// Capture and store any secondary payloads
		
		HerciHighspeedHeader radiationTelemetry = f.calculateTelemetryPalyoad();
		radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
		if (f.type >= 600) // this is a high speed record
			radiationTelemetry.type = f.type + 200; // we give the telem record 800+ type
		add(radiationTelemetry);
		herciHeaderRecords.setUpdated(true);
		//updatedHerciHeader = true;

		ArrayList<HerciHighSpeedPacket> pkts = f.calculateTelemetryPackets();
		for(int i=0; i< pkts.size(); i++) {
			HerciHighSpeedPacket pk = pkts.get(i);
			pk.captureHeaderInfo(f.id, f.uptime, f.resets);
			if (f.type >= 600) // this is a high speed record
				pk.type = f.type*1000 + 900 + i;; // we give the telem record 900+ type.  Assumes 10 minipackets or less
			add(pk);
			herciPacketRecords.setUpdated(true);

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
			return rtRecords.save(f);
		} else if (f instanceof PayloadMaxValues  ) {
				return maxRecords.save(f);
		} else if (f instanceof PayloadMinValues ) {
				return minRecords.save(f);
		} else if (f instanceof PayloadRadExpData ) {
			if (radRecords.save(f))
				return addRadRecord((PayloadRadExpData)f);
		} else if (f instanceof RadiationTelemetry ) {
				return radTelemRecords.save(f);
		} else if (f instanceof PayloadHERCIhighSpeed ) {
			if (herciRecords.save(f))
				return addHerciRecord((PayloadHERCIhighSpeed)f);
		} else if (f instanceof HerciHighspeedHeader ) {
				return herciHeaderRecords.save(f);
		} else if (f instanceof HerciHighSpeedPacket ) {
				return herciPacketRecords.save(f);
		}
		return false;
	}
		
	public PayloadRtValues getLatestRt() throws IOException {
		return (PayloadRtValues) rtRecords.getLatest();
	}

	public PayloadMaxValues getLatestMax() throws IOException {
		return (PayloadMaxValues) maxRecords.getLatest();
	}

	public PayloadMinValues getLatestMin() throws IOException {
		return (PayloadMinValues) minRecords.getLatest();
	}

	public PayloadRadExpData getLatestRad() throws IOException {
		return (PayloadRadExpData) radRecords.getLatest();
	}

	public RadiationTelemetry getLatestRadTelem() throws IOException {
		return (RadiationTelemetry) radTelemRecords.getLatest();
	}

	public PayloadHERCIhighSpeed getLatestHerci() throws IOException {
		return (PayloadHERCIhighSpeed) herciRecords.getLatest();
	}

	public HerciHighspeedHeader getLatestHerciHeader() throws IOException {
		return (HerciHighspeedHeader) herciHeaderRecords.getLatest();
	}


	/**
	 * Try to return an array with "period" entries for this attribute, starting with the most 
	 * recent
	 * 
	 * @param name
	 * @param period
	 * @return
	 * @throws IOException 
	 */
	public double[][] getRtGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return rtRecords.getGraphData(name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return maxRecords.getGraphData(name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return minRecords.getGraphData(name, period, id, fromReset, fromUptime);
		
	}

	/**
	 * Get data for a single named field from the Radiation Telemetry file for the period given
	 * @param name
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public double[][] getRadTelemGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return radTelemRecords.getGraphData(name, period, id, fromReset, fromUptime);
		
	}

	public String[][] getRadData(int period, int id, int fromReset, long fromUptime) throws IOException {
		return radRecords.getPayloadData(period, id, fromReset, fromUptime, MAX_RAD_DATA_LENGTH);

	}
	
	/**
	 * Get a set of Radiation Telemetry records for the range given
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime) throws IOException {
		return radTelemRecords.getPayloadData(period, id, fromReset, fromUptime, MAX_HERCI_HK_DATA_LENGTH); 

	}

	/**
	 * Get data for a single named field from the HERCI Science Header for the period given
	 * @param name
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public double[][] getHerciScienceHeaderGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return herciHeaderRecords.getGraphData(name, period, id, fromReset, fromUptime);
		
	}

	/**
	 * Get a set of HERCI Mini Packets for the range given
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime) throws IOException {
		return herciPacketRecords.getPayloadData(period, id, fromReset, fromUptime, MAX_HERCI_PACKET_DATA_LENGTH); // FIXME - LENGTH NOT CORECT

	}

	
/*
	protected String getRtUTCFromUptime(int reset, long uptime) {
		int idx = rtRecords.getFrameIndex(foxId, uptime, reset);
		if (idx != -1) {
			return rtRecords.get(idx).getCaptureDate();
		}
		return null;
		
	}
	*/
	
	

	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {

		try {
			rtRecords.remove();
			maxRecords.remove();
			minRecords.remove();
			radRecords.remove();
			radTelemRecords.remove();
			if (fox.hasHerci()) {
				herciRecords.remove();
				herciHeaderRecords.remove();
				herciPacketRecords.remove();
			}
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
	
	public void convert() throws IOException {
		rtRecords.convert();
		maxRecords.convert();
		minRecords.convert();
		radRecords.convert();
		radTelemRecords.convert();
		if (fox.hasHerci()) {
			herciRecords.convert();
			herciHeaderRecords.convert();
			herciPacketRecords.convert();
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

