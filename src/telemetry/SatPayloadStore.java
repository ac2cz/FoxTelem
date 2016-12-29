package telemetry;


import gui.MainWindow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import javax.swing.JOptionPane;

import common.Config;
import common.Log;
import common.Spacecraft;
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
//	public static String RT_LOG = "rttelemetry";
//	public static String MAX_LOG = "maxtelemetry";
//	public static String MIN_LOG = "mintelemetry";
//	public static String RAD_LOG = "radtelemetry";

	// Secondary payloads - decoded from the primary payloads
//	public static String RAD_TELEM_LOG = "radtelemetry2";

//	public static String HERCI_LOG = "herciHSdata";
//	public static String HERCI_HEADER_LOG = "herciHSheader";
//	public static String HERCI_PACKET_LOG = "herciHSpackets";
		
	SatPayloadTable[] records;
	//SatPayloadTable maxRecords;
	//SatPayloadTable minRecords;
	//SatPayloadTable radRecords;
	//SatPayloadTable radTelemRecords;
	//SatPayloadTable herciRecords;
	//SatPayloadTable herciHeaderRecords;
	//SatPayloadTable herciPacketRecords;
	
	public static final int MAX_RAD_DATA_LENGTH = 61;
	public static final int MAX_HERCI_PACKET_DATA_LENGTH = 128;
	//public static final int MAX_HERCI_HK_DATA_LENGTH = 27; // 25 fields plus the 2 fields needed for Reset and Uptime
	
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
		records = new SatPayloadTable[fox.numberOfLayouts];
		for (int i=0; i<fox.numberOfLayouts; i++)
			records[i] = new SatPayloadTable(INIT_SIZE, fox.series+foxId+fox.layout[i].name+".csv");
		/*
		maxRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+MAX_LOG);
		minRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+MIN_LOG);
		radRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+RAD_LOG);
		radTelemRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+RAD_TELEM_LOG);
		if (Config.satManager.hasHerci(foxId)) {
			herciRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+HERCI_LOG);
			herciHeaderRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+HERCI_HEADER_LOG);
			herciPacketRecords = new SatPayloadTable(INIT_SIZE, "Fox"+foxId+HERCI_PACKET_LOG);
		}
		*/
	}
	
	public void setUpdatedAll() {
		for (int i=0; i<fox.numberOfLayouts; i++)
			records[i].setUpdated(true);
		/* maxRecords.setUpdated(true);
		minRecords.setUpdated(true);
		radRecords.setUpdated(true);
		radTelemRecords.setUpdated(true);
		if (Config.satManager.hasHerci(foxId)) {
			herciRecords.setUpdated(true);
			herciHeaderRecords.setUpdated(true);
			herciPacketRecords.setUpdated(true);
		}
		*/
	}

	public boolean getUpdated(String layout) { 
		int i = fox.getLayoutIdxByName(layout);
		if (i != fox.ERROR_IDX)
			return records[i].getUpdated(); 
		return false;
	}
	public void setUpdated(String layout, boolean u) {
		int i = fox.getLayoutIdxByName(layout);
		if (i != fox.ERROR_IDX)
		records[i].setUpdated(u); 
	}

	/*
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
	*/
	
	public int getNumberOfFrames() {
		int total = 0;
		for (int i=0; i<fox.numberOfLayouts; i++) {
			if (!fox.layout[i].isSecondaryPayload())
				total += records[i].getSize();
		}
		return total;
		/*
		int herci = 0;
		if (Config.satManager.hasHerci(foxId))
			herci = herciRecords.getSize();
		return herci + rtRecords.getSize() + maxRecords.getSize() + minRecords.getSize() + radRecords.getSize(); 
		*/
	}
	public int getNumberOfFrames(String layout) { 
		int i = fox.getLayoutIdxByName(layout);
		if (i != fox.ERROR_IDX) {
			if (!fox.layout[i].isSecondaryPayload())
				return records[i].getSize();
			return 0;
		}
		return 0;
	}
	
	public int getNumberOfTelemFrames() { return getNumberOfFrames(Spacecraft.REAL_TIME_LAYOUT) 
			+ getNumberOfFrames(Spacecraft.MAX_LAYOUT) +getNumberOfFrames(Spacecraft.MIN_LAYOUT); }
	/*
	public int getNumberOfRadFrames() { return radRecords.getSize(); }
	public int getNumberOfHerciFrames() { return herciRecords.getSize(); }
	*/
	
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
		int l = fox.getLayoutIdxByName(Spacecraft.RAD_LAYOUT);
		if (l != fox.ERROR_IDX)
		if (!records[l].hasFrame(id, uptime, resets)) {
			for (int i=0; i< f.length; i++) {
				if (f[i].hasData()) {
					f[i].captureHeaderInfo(id, uptime, resets);
					//radRecords.setUpdated(true);
					try {
						records[l].save(f[i]);
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
		if (f.layout.name.equalsIgnoreCase(Spacecraft.HERCI_HS_LAYOUT) || f.isTelemetry()) {
			RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
			radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
			if (f.type >= 400) // this is a high speed record
				radiationTelemetry.type = f.type + 300; // we give the telem record 700+ type
			add(radiationTelemetry);
			//radTelemRecords.setUpdated(true);
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
		//herciHeaderRecords.setUpdated(true);
		//updatedHerciHeader = true;

		ArrayList<HerciHighSpeedPacket> pkts = f.calculateTelemetryPackets();
		for(int i=0; i< pkts.size(); i++) {
			HerciHighSpeedPacket pk = pkts.get(i);
			pk.captureHeaderInfo(f.id, f.uptime, f.resets);
			if (f.type >= 600) // this is a high speed record
				pk.type = f.type*1000 + 900 + i;; // we give the telem record 900+ type.  Assumes 10 minipackets or less
			add(pk);
			//herciPacketRecords.setUpdated(true);

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
		
		int i = fox.getLayoutIdxByName(f.layout.name);
		if (i != fox.ERROR_IDX) {
			boolean ret = records[i].save(f); 
			if (f instanceof PayloadRadExpData) {
				return addRadRecord((PayloadRadExpData)f);				
			} else if (f instanceof PayloadHERCIhighSpeed ) {
				return addHerciRecord((PayloadHERCIhighSpeed)f);				
			}
		}
		return false;
		
		/*
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
		*/
	}
		

	public FramePart getLatest(String layout) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		if (i != fox.ERROR_IDX)
			return records[i].getLatest(); 
		return null;
	}

	public FramePart getLatestRt() throws IOException {
		return getLatest(Spacecraft.REAL_TIME_LAYOUT);
	}

	public FramePart getLatestMax() throws IOException {
		return getLatest(Spacecraft.MAX_LAYOUT);
	}

	public FramePart getLatestMin() throws IOException {
		return getLatest(Spacecraft.MIN_LAYOUT);
	}

	public PayloadRadExpData getLatestRad() throws IOException {
		return (PayloadRadExpData) getLatest(Spacecraft.RAD_LAYOUT);
	}

	public RadiationTelemetry getLatestRadTelem() throws IOException {
		return (RadiationTelemetry) getLatest(Spacecraft.RAD2_LAYOUT);
	}
	
	
	public RadiationTelemetry getRadTelem(int id, int resets, long uptime) throws IOException {
		if (uptime == 0 && resets == 0)
			return getLatestRadTelem();
		int i = fox.getLayoutIdxByName(Spacecraft.RAD2_LAYOUT);
		if (i != fox.ERROR_IDX)
			return (RadiationTelemetry) records[i].getFrame(id, uptime, resets);
		return null;
	}

	public PayloadHERCIhighSpeed getLatestHerci() throws IOException {
		return (PayloadHERCIhighSpeed) getLatest(Spacecraft.HERCI_HS_LAYOUT);
	}

	public HerciHighspeedHeader getLatestHerciHeader() throws IOException {
		return (HerciHighspeedHeader) getLatest(Spacecraft.HERCI_HS_HEADER_LAYOUT);
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
	public double[][] getGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime, String layout) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		if (i != fox.ERROR_IDX)
			return records[i].getGraphData(name, period, id, fromReset, fromUptime);
		return null;
	}

	public double[][] getRtGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.REAL_TIME_LAYOUT);
	}
	
	public double[][] getMaxGraphData(String name, int period, Spacecraft fox2, int fromReset, long fromUptime) throws IOException {
		return getGraphData(name, period, fox2, fromReset, fromUptime, Spacecraft.MAX_LAYOUT);
		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.MIN_LAYOUT);
		
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
	public double[][] getRadTelemGraphData(String name, int period, FoxSpacecraft id, int fromReset, long fromUptime) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.RAD2_LAYOUT);
		
	}

	public String[][] getRadData(int period, int id, int fromReset, long fromUptime) throws IOException {
		int i = fox.getLayoutIdxByName(Spacecraft.RAD_LAYOUT);
		if (i != fox.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, MAX_RAD_DATA_LENGTH);
		return null;
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
		int i = fox.getLayoutIdxByName(Spacecraft.RAD2_LAYOUT);
		if (i != fox.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, RadiationTelemetry.MAX_HERCI_HK_DATA_LENGTH+2); 
		return null;
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
	public double[][] getHerciScienceHeaderGraphData(String name, int period, FoxSpacecraft id, int fromReset, long fromUptime) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.HERCI_HS_HEADER_LAYOUT);
		
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
		int i = fox.getLayoutIdxByName(Spacecraft.HERCI_HS_PKT_LAYOUT);
		if (i != fox.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, MAX_HERCI_PACKET_DATA_LENGTH); // FIXME - LENGTH NOT CORECT
		return null;
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
	
	public void offloadSegments() {
		rtRecords.offloadSegments();
		maxRecords.offloadSegments();
		minRecords.offloadSegments();
		radRecords.offloadSegments();
		radTelemRecords.offloadSegments();
		if (fox.hasHerci()) {
			herciRecords.offloadSegments();
			herciHeaderRecords.offloadSegments();
			herciPacketRecords.offloadSegments();
		}
	}

	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {

		try {
			for (int i=0; i<fox.numberOfLayouts; i++)
				records[i].remove();
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
	@SuppressWarnings("resource") // because we have a finally statement and the checker does not seem to realize that
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
		for (int i=0; i<fox.numberOfLayouts; i++)
			records[i].convert();
		/*
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
		*/
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

