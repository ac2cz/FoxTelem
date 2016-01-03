package telemetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;

/**
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
 * This class is a flat file database for a single payload type.  It is referred to as a table, but
 * the actual data may be spread across several files on disk
 * 
 */
public class SatPayloadTable {

	public static final int MAX_DATA_LENGTH = 61;
	public static final int MAX_SEGMENT_SIZE = 1000;
	private SortedArrayList<TableSeg> tableIdx; // The map of data on disk and the parts of it that are loaded
	private static final int INITIAL_SIZE = 2; // inital number of table parts
	private String fileName; // this is the path and filename for this table
	private String baseFileName; // this is the base filename for this table
	private SortedFramePartArrayList rtRecords; // this is the rtRecords that are loaded into memory
	private boolean updated = false;

	public SatPayloadTable(int size, String name) throws IOException {
		tableIdx = new SortedArrayList<TableSeg>(INITIAL_SIZE);
		baseFileName = name;
		String dir = getDir();
        fileName = dir + PayloadStore.DB_NAME+File.separator + name;
      
		rtRecords = new SortedFramePartArrayList(size);
		loadIdx();
		updated = true;
	}
	
	public void setUpdated(boolean t) { updated = t; }
	public boolean getUpdated() { return updated; }
	
	private String getDir() {
		String dir = "";
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator ;
			//System.err.println("Loading: "+log);
		}
        return dir;
	}
	
	public int getSize() { 
		int s=0;
		for (TableSeg t: tableIdx) {
			s = s + t.records;
		}
		return s; 
	}
	
	public boolean hasFrame(int id, long uptime, int resets) throws IOException { 
		// Make sure the segment is loaded, so we can check
		TableSeg seg = loadSeg(resets, uptime);
		return rtRecords.hasFrame(id, uptime, resets); }
	
	public FramePart getLatest() throws IOException {
		if (tableIdx.size() > 0) {
			TableSeg lastSeg = tableIdx.get(tableIdx.size()-1);
			if (!lastSeg.isLoaded()) {
				load(lastSeg);
				
			}
			if (rtRecords.size() == 0) return null;
			return rtRecords.get(rtRecords.size()-1);
		}
		return null;
	}
	
	/**
	 * Return an array of payloads data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public String[][] getPayloadData(int period, int id, int fromReset, long fromUptime, int length) throws IOException {
		loadSegments(fromReset, fromUptime, period);
		int start = 0;
		int end = 0;
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			start = rtRecords.size()-period;
			end = rtRecords.size();
		} else {
			// we need to find the start point
			start = rtRecords.getNearestFrameIndex(id, fromUptime, fromReset);
			if (start == -1 ) start = rtRecords.size()-period;
			end = start + period;
		}
		if (end > rtRecords.size()) end = rtRecords.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > rtRecords.size()) start = rtRecords.size();
		
		int[][] results = new int[end-start][];
		String[] upTime = new String[end-start];
		String[] resets = new String[end-start];
		
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			results[j] = rtRecords.get(i).getFieldValues();
			upTime[j] = ""+rtRecords.get(i).getUptime();
			resets[j--] = ""+rtRecords.get(i).getResets();
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
	
	/**
	 * Return a single field so that it can be graphed or analyzed
	 * @param name
	 * @param period
	 * @param fox
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	double[][] getGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime) throws IOException {
		loadSegments(fromReset, fromUptime, period);
		int start = 0;
		int end = 0;
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
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
		
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			if (Config.displayRawValues)
				results[j] = rtRecords.get(i).getRawValue(name);
			else
				results[j] = rtRecords.get(i).getDoubleValue(name, fox);
			upTime[j] = rtRecords.get(i).getUptime();
			resets[j--] = rtRecords.get(i).getResets();
		}
		
		double[][] resultSet = new double[3][end-start];
		resultSet[PayloadStore.DATA_COL] = results;
		resultSet[PayloadStore.UPTIME_COL] = upTime;
		resultSet[PayloadStore.RESETS_COL] = resets;
		
		return resultSet;
	}
	
	private TableSeg getSeg(int reset, long uptime) throws IOException {
		for (int i=tableIdx.size()-1; i>=0; i--) {
			if (tableIdx.get(i).fromReset <= reset && tableIdx.get(i).fromUptime <= uptime) {
				return tableIdx.get(i);
			}
		}
		// We could not find a valid Segment, so create a new segment at the head of the list
		TableSeg seg = new TableSeg(reset, uptime, baseFileName);
		tableIdx.add(seg);
		saveIdx();
		return seg;
	}
	
	/**
	 * Make sure the segment for this reset/uptime is loaded and is ready to receive data
	 * @param f
	 * @throws IOException 
	 */
	private TableSeg loadSeg(int reset, long uptime) throws IOException {
		TableSeg seg = getSeg(reset, uptime);
		if (seg.isLoaded()) return seg;
		load(seg);
		return seg;
	}
	
	/**
	 * Load all of the segments needed so that "number" of records is available.  Used for plotting graphs.  If segments are missing then
	 * we do not create them
	 * @param reset
	 * @param uptime
	 * @param number
	 * @throws IOException
	 */
	private void loadSegments(int reset, long uptime, int number) throws IOException {
		int total = 0;
		if (reset == 0 && uptime == 0) {
			// load backwards, but load in the right order so that the inserts into the records list are fast (append at end)
			// So we first calculate where to start
			int startIdx = 0;
			for (int i=tableIdx.size()-1; i>=0; i--) {
				total += tableIdx.get(i).records;
				if (total >= number) {
					startIdx = i;
					break;
				}
			}
			total = 0;
			// Now start index is the first segment we need to load, so now load them if needed
			for (int i=startIdx; i<tableIdx.size(); i++) {
				if (!tableIdx.get(i).isLoaded()) {
					load(tableIdx.get(i));
				}
				total += tableIdx.get(i).records;
				
			}
			//if (total >= number) System.err.println("Success we got: "+total+" records and needed "+number);
		} else {
			// load forwards from the relevant reset/uptime
			for (int i=0; i< tableIdx.size(); i++) {
				if ( ((i == tableIdx.size()-1) && tableIdx.get(i).fromReset <= reset && tableIdx.get(i).fromUptime <= uptime) || 
						(i < tableIdx.size()-1) &&
						tableIdx.get(i).fromReset <= reset && tableIdx.get(i).fromUptime <= uptime
						&& tableIdx.get(i+1).fromReset > reset && tableIdx.get(i+1).fromUptime > uptime) {
					// Then we need to load segment at i and start counting from here
					
					while(i < tableIdx.size()) {
						if (!tableIdx.get(i).isLoaded())
							load(tableIdx.get(i));
						total += tableIdx.get(i).records;
						if (total >= number) break;
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Save a new record to disk		
	 * @param f
	 */
	public boolean save(FramePart f) throws IOException {
		// Make sure this segment is loaded, or create an empty segment if it does not exist
		TableSeg seg = loadSeg(f.resets, f.uptime);
		if (rtRecords.add(f)) {
		//if (!rtRecords.hasFrame(f.id, f.uptime, f.resets)) {
			updated = true;
			if (seg.records == MAX_SEGMENT_SIZE) {
				// We need to add a new segment with this as the first record
				seg = new TableSeg(f.resets, f.uptime, baseFileName);
				tableIdx.add(seg);
			}
			save(f, getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName);
			seg.records++;
			saveIdx();
			//return rtRecords.add(f);
			return true;
		} else {
			if (Config.debugFrames) Log.println("DUPLICATE RECORD, not loaded");
		}
		return false;
	}
	
	/**
	 * Load a payload file from disk
	 * Payload files are stored in separate logs, but this routine is written so that it can load mixed records
	 * from a single file
	 * @param log
	 * @throws IOException 
	 */
	public void load(TableSeg seg) throws IOException {
		String log = getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName;
        String line;
        createNewFile(log);
 
        BufferedReader dis = new BufferedReader(new FileReader(log));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			addLine(line);
        		}
        	}
        	seg.setLoaded(true);
        	dis.close();
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());

        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        }

	}

	private FramePart addLine(String line) {
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
		FramePart rt = null;
		if (type == FramePart.TYPE_REAL_TIME) {
			rt = new PayloadRtValues(id, resets, uptime, date, st, Config.satManager.getRtLayout(id));
		} else
			if (type == FramePart.TYPE_MAX_VALUES) {
				rt = new PayloadMaxValues(id, resets, uptime, date, st, Config.satManager.getMaxLayout(id));

			} else
				if (type == FramePart.TYPE_MIN_VALUES) {
					rt = new PayloadMinValues(id, resets, uptime, date, st, Config.satManager.getMinLayout(id));

				}
		if (type == FramePart.TYPE_RAD_TELEM_DATA || type >= 700 && type < 800) {
			rt = new RadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getRadTelemLayout(id));
			rt.type = type; // make sure we get the right type
		}
		if (type == FramePart.TYPE_RAD_EXP_DATA || type >= 400 && type < 500) {
			rt = new PayloadRadExpData(id, resets, uptime, date, st);
			rt.type = type; // make sure we get the right type
		}        			
		if (type == FramePart.TYPE_HERCI_HIGH_SPEED_DATA) {
			rt = new PayloadHERCIhighSpeed(id, resets, uptime, date, st, Config.satManager.getHerciHSLayout(id));
		}
		if (type == FramePart.TYPE_HERCI_SCIENCE_HEADER ) {
			rt = new HerciHighspeedHeader(id, resets, uptime, date, st, Config.satManager.getHerciHSHeaderLayout(id));
		}
		if (type == FramePart.TYPE_HERCI_HS_PACKET ) {
			rt = new HerciHighSpeedPacket(id, resets, uptime, date, st, Config.satManager.getHerciHSHeaderLayout(id));
		}

		if (rt != null) {
			rtRecords.add(rt);
		}
		return rt;
	}
	
	public void convert() throws IOException {
		
		
        String log = getDir()+baseFileName+".log";
		String line;
		int linesAdded = 0;
		TableSeg seg = null;
		File aFile = new File(log );
		if(aFile.exists()){
			// then convert it
			BufferedReader dis = new BufferedReader(new FileReader(log));
			try {
				while ((line = dis.readLine()) != null) {
					if (line != null) {
						FramePart rt = addLine(line);
						if (rt != null) {
							if (linesAdded == SatPayloadTable.MAX_SEGMENT_SIZE) {
								linesAdded = 0;
							}
							if (linesAdded == 0) {
								// First line in a segment
								seg = new TableSeg(rt.resets, rt.uptime, baseFileName);
								tableIdx.add(seg);
							}
							save(rt, getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName);
							linesAdded++;
							seg.records = linesAdded;
						}
					}
				}
				updated = true;
				
				dis.close();
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());

			} catch (NumberFormatException n) {
				n.printStackTrace(Log.getWriter());
			}
		}
		saveIdx();
	}
	
	private boolean createNewFile(String log) throws IOException {
		File aFile = new File(log );
		if(!aFile.exists()){
			try {
				aFile.createNewFile();
				return true;

			} catch (IOException e) {
				JOptionPane.showMessageDialog(MainWindow.frame,
						e.toString(),
						"ERROR creating file " + log,
						JOptionPane.ERROR_MESSAGE) ;
				e.printStackTrace(Log.getWriter());
				return false;
			} 
		}
		return false;
	}
	
	private void writeVersion(Writer output) throws IOException {
		
		/*try {
			output.write( "FOXDB VESION:" + DB_VERSION + "\n" );
			output.flush();
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}*/
	}

	/**
	 * Save a payload to the a file
	 * @param frame
	 * @param log
	 * @throws IOException
	 */
	private void save(FramePart frame, String log) throws IOException {

		createNewFile(log);
		//Log.println("Saving: " + log);
		//use buffering and append to the existing file
		File aFile = new File(log );
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
	 * Save Index to the a file
	 * @throws IOException
	 */
	private void saveIdx() throws IOException {
		File aFile = new File(fileName + ".idx" );
		createNewFile(fileName + ".idx");
		//Log.println("Saving: " + log);
		//use buffering and REPLACE the existing file
		Writer output = new BufferedWriter(new FileWriter(aFile, false));
		writeVersion(output);
		try {
			for (TableSeg seg: tableIdx) {

				output.write( seg.toFile() + "\n" );
				output.flush();
			}
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}
	}
	
	private void parseVersion(BufferedReader dis) throws IOException {
		
		/*String version;
    	version = dis.readLine(); // read the first line
    	String[] versionLine = version.split(":");
    	try {
    		String availableVersion = versionLine[1];
    		//Log.println("FILE VERSION: "+availableVersion);
    	
    		int maj = Config.parseVersionMajor(availableVersion);
    		int min = Config.parseVersionMinor(availableVersion);
    		String point = Config.parseVersionPoint(availableVersion);
    		//System.out.println("MAJ: "+maj);
    		//System.out.println("MIN: "+min);
    		//System.out.println("POINT: "+point);

    		if (Config.getVersionMajor() < maj) { // fatal
    			JOptionPane.showMessageDialog(MainWindow.frame,
   					 "You may need to reset FoxTelem.properties or re-install FoxTelem\n"
    					+"Payload log "+fileName + " Version: "+availableVersion+ " is incompatible with this version of FoxTelem\n"
   								+ "Was the data directory moved or new files copied in?\n",
   					"FATAL! Data file version incompatible ",
   					JOptionPane.ERROR_MESSAGE) ;
   			
   			System.exit(1);
    		}
    		if (Config.getVersionMajor() == maj && Config.getVersionMinor() < min) // ignore
    			;
    	} catch (NumberFormatException e) {
    		e.printStackTrace(Log.getWriter());
    		Log.println("Error parsing the Database version information.  Abandoning the check");
    	} catch (ArrayIndexOutOfBoundsException e) {
    		e.printStackTrace(Log.getWriter());
    		Log.println("Error parsing the Database version format.  Abandoning the check");
    	}*/
	}
	
	/**
	 * Load the Index from disk
	 * @throws IOException 
	 */
	public void loadIdx() throws IOException {
        String line;
        File aFile = new File(fileName + ".idx" );
		if (createNewFile(fileName + ".idx")) {
			Writer output = new BufferedWriter(new FileWriter(aFile, true));
			writeVersion(output);
		}
 
        BufferedReader dis = new BufferedReader(new FileReader(aFile.getPath()));

        try {
        	parseVersion(dis);
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			StringTokenizer st = new StringTokenizer(line, ",");
        			
        			int resets = Integer.valueOf(st.nextToken()).intValue();
        			long uptime = Long.valueOf(st.nextToken()).longValue();
        			int records = Integer.valueOf(st.nextToken()).intValue();
        			String name = st.nextToken();
        			TableSeg seg = new TableSeg(resets, uptime, name, records);
    				tableIdx.add(seg);
        		}
        	}

        	dis.close();
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());

        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        }
	}	
	
	public void remove() throws IOException {
		for (TableSeg seg: tableIdx)
			SatPayloadStore.remove(getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName);
		SatPayloadStore.remove(fileName + ".idx");
	}
}
