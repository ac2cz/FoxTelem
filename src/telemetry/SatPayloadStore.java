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
	private FoxSpacecraft fox;
	
	private static final int INIT_SIZE = 1000;
	//private boolean initRad2 = false;
			
	SatPayloadTable[] records;
	
	public static final int MAX_RAD_DATA_LENGTH = 61;
	public static final int MAX_HERCI_PACKET_DATA_LENGTH = 128;
	//public static final int MAX_HERCI_HK_DATA_LENGTH = 27; // 25 fields plus the 2 fields needed for Reset and Uptime
	
	/**
	 * Create the payload store this this fox id
	 * @param id
	 */
	public SatPayloadStore(int id) {
		foxId = id;
		fox = (FoxSpacecraft) Config.satManager.getSpacecraft(foxId);
		
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
			records[i] = new SatPayloadTable(INIT_SIZE, fox.series+foxId+fox.layout[i].name, fox.hasModeInHeader);
	}
	
	public void setUpdatedAll() {
		for (int i=0; i<fox.numberOfLayouts; i++)
			records[i].setUpdated(true);
	}

	public boolean getUpdated(String layout) { 
		int i = fox.getLayoutIdxByName(layout);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getUpdated(); 
		return false;
	}
	public void setUpdated(String layout, boolean u) {
		int i = fox.getLayoutIdxByName(layout);
		if (i != Spacecraft.ERROR_IDX)
		records[i].setUpdated(u); 
	}

	
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
		if (i != Spacecraft.ERROR_IDX) {
			if (!fox.layout[i].isSecondaryPayload())
				return records[i].getSize();
			return 0;
		}
		return 0;
	}
	
	public int getNumberOfTelemFrames() { return getNumberOfFrames(Spacecraft.REAL_TIME_LAYOUT) 
			+ getNumberOfFrames(Spacecraft.MAX_LAYOUT) +getNumberOfFrames(Spacecraft.MIN_LAYOUT); }
	
	public boolean add(int id, long uptime, int resets, FramePart f) throws ArrayIndexOutOfBoundsException, IOException {
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
		if (l != Spacecraft.ERROR_IDX)
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
					addRadSecondaryRecord(f[i]);
				}
			}
		} else {
			if (Config.debugFrames) Log.println("DUPLICATE RAD RECORD SET, not loaded");
			return false;
		}
		return true;
	}

	private boolean addRadSecondaryRecord(PayloadRadExpData f) throws IOException {
		
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

	private boolean addWODRadSecondaryRecord(PayloadWODRad f) throws IOException {
		// Capture and store any secondary payloads
		if (f.layout.name.equalsIgnoreCase(Spacecraft.HERCI_HS_LAYOUT) || f.isTelemetry()) {
			WodRadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
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
	protected boolean addHerciSecondaryRecord(PayloadHERCIhighSpeed f) throws IOException {
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
		boolean ret = false;
		int i = fox.getLayoutIdxByName(f.layout.name);
		if (i != Spacecraft.ERROR_IDX) {
			ret = records[i].save(f); 
			if (ret) {
				if (f instanceof PayloadWODRad) {
					return addWODRadSecondaryRecord((PayloadWODRad)f);
				} else if (f instanceof PayloadRadExpData) {
					return addRadSecondaryRecord((PayloadRadExpData)f);				
				} else if (f instanceof PayloadHERCIhighSpeed ) {
					return addHerciSecondaryRecord((PayloadHERCIhighSpeed)f);				
				}
			}
		}
		return ret;
		
	}
		
	public FramePart getLatest(int id, int reset, long uptime, String layout, boolean prev) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getFrame(id, uptime, reset, prev); 
		return null;
	}
	
	public FramePart getLatest(int id, int reset, long uptime, int type, String layout, boolean prev) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getFrame(id, uptime, reset, type, prev); 
		return null;
	}

	public FramePart getLatest(String layout) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		if (i != Spacecraft.ERROR_IDX)
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

	public FoxFramePart getLatestRad() throws IOException {
		return (FoxFramePart) getLatest(Spacecraft.RAD_LAYOUT);
	}

	public RadiationTelemetry getLatestRadTelem() throws IOException {
		return (RadiationTelemetry) getLatest(Spacecraft.RAD2_LAYOUT);
	}
	
	
	public RadiationTelemetry getRadTelem(int id, int resets, long uptime) throws IOException {
		if (uptime == 0 && resets == 0)
			return getLatestRadTelem();
		int i = fox.getLayoutIdxByName(Spacecraft.RAD2_LAYOUT);
		if (i != Spacecraft.ERROR_IDX)
			return (RadiationTelemetry) records[i].getFrame(id, uptime, resets, false);
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
	 * @param positionData - returns lat/lon with data if available
	 * @return
	 * @throws IOException 
	 */
	public double[][] getGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime, String layout, boolean positionData, boolean reverse) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getGraphData(name, period, id, fromReset, fromUptime, positionData, reverse);
		return null;
	}

	public double[][] getRtGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.REAL_TIME_LAYOUT, positionData, reverse);
	}
	
	public double[][] getMaxGraphData(String name, int period, Spacecraft fox2, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		return getGraphData(name, period, fox2, fromReset, fromUptime, Spacecraft.MAX_LAYOUT, positionData, reverse);
		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.MIN_LAYOUT, positionData, reverse);
		
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
	public double[][] getRadTelemGraphData(String name, int period, FoxSpacecraft id, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.RAD2_LAYOUT, positionData, reverse);
		
	}

	/**
	 * Get a set of Realtime Telemetry records for the range given
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public String[][] getRtData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
		return getTableData(period, id, fromReset, fromUptime, reverse, Spacecraft.REAL_TIME_LAYOUT);
	}

	/**
	 * Get a set of WOD Telemetry records for the range given
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public String[][] getWODData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
		return getTableData(period, id, fromReset, fromUptime, reverse, Spacecraft.WOD_LAYOUT);
	}

	public String[][] getRadData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
			return getTableData(period, id, fromReset, fromUptime, reverse, Spacecraft.RAD_LAYOUT);
	}
	
	public String[][] getWODRadData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
		return getTableData(period, id, fromReset, fromUptime, reverse, Spacecraft.WOD_RAD_LAYOUT);
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
	public String[][] getRadTelemData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
		return getTableData(period, id, fromReset, fromUptime, reverse, Spacecraft.RAD2_LAYOUT);
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
	public String[][] getWodRadTelemData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
		return getTableData(period, id, fromReset, fromUptime, reverse, Spacecraft.WOD_RAD2_LAYOUT);
	}
	
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean returnType, boolean reverse, String layout) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		BitArrayLayout l = fox.getLayoutByName(layout);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, l.fieldName.length, returnType, reverse);
		return null;	
	}
	
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean reverse, String layout) throws IOException {
		int i = fox.getLayoutIdxByName(layout);
		BitArrayLayout l = fox.getLayoutByName(layout);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, l.fieldName.length, reverse);
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
	public double[][] getHerciScienceHeaderGraphData(String name, int period, FoxSpacecraft id, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		return getGraphData(name, period, id, fromReset, fromUptime, Spacecraft.HERCI_HS_HEADER_LAYOUT, positionData, reverse);
		
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
	public String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime, boolean type, boolean reverse) throws IOException {
		int i = fox.getLayoutIdxByName(Spacecraft.HERCI_HS_PKT_LAYOUT);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, MAX_HERCI_PACKET_DATA_LENGTH, type, reverse); // FIXME - LENGTH NOT CORECT
		return null;
	}
	
	public String[][] getHerciHsData(int period, int id, int fromReset, long fromUptime, boolean reverse) throws IOException {
		int i = fox.getLayoutIdxByName(Spacecraft.HERCI_HS_LAYOUT);
		if (i != Spacecraft.ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE, true, reverse); 
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
		while (deleteLock)
			try {
				Thread.sleep(10); // wait
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		for (int i=0; i<fox.numberOfLayouts; i++)
			records[i].offloadSegments();
	}

	boolean deleteLock = false;
	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
		deleteLock = true;
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
		deleteLock = false;
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

	public int getNumberOfPayloadsBetweenTimestamps(int id, int reset, long uptime, int toReset, long toUptime, String payloadType) {
		int i = fox.getLayoutIdxByName(payloadType);
		if (i != Spacecraft.ERROR_IDX)
			try {
				return records[i].getNumberOfPayloadsBetweenTimestamps(reset, uptime, toReset, toUptime);
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());
				return 0;
			} 
		return 0;
	}
}

