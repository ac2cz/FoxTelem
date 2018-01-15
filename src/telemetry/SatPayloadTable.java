package telemetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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

	public static final int MAX_DATA_LENGTH = 62;
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
	
	public static String getDir() {
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
		@SuppressWarnings("unused")
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
	 * Return the frame part with the passed reset and uptime or the first frame after it. Unless the "prev" boolean is passed
	 * then we return the previous frame if the required reset/uptime is not found 
	 * @param id
	 * @param uptime
	 * @param resets
	 * @return
	 * @throws IOException
	 */
	public FramePart getFrame(int id, long uptime, int resets, boolean prev) throws IOException { 
		// Make sure the segment is loaded, so we can check
		@SuppressWarnings("unused")
		TableSeg seg = loadSeg(resets, uptime);
		if (prev) {
			int i = rtRecords.getNearestPrevFrameIndex(id, uptime, resets); 
			if (i == -1) return null;
			return rtRecords.get(i);
		} else {
			int i = rtRecords.getNearestFrameIndex(id, uptime, resets); 
			if (i == -1) return null;
			return rtRecords.get(i);
		}
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
	public String[][] getPayloadData(int period, int id, int fromReset, long fromUptime, int length, boolean reverse) throws IOException {
		if (rtRecords == null) return null;
		loadSegments(fromReset, fromUptime, period, reverse);
		int start = 0;
		int end = 0;
		
		if (reverse) { // then we take records nearest the end
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
		
		// Create a results set, with reset, uptime and the data on the same line
		String[][] resultSet = new String[end-start][length+3];
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
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @param positionData
	 * @param reverse - return the data from the end of the table in reverse order,such as when monitoring a live graph
	 * @return
	 * @throws IOException 
	 */
	double[][] getGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		loadSegments(fromReset, fromUptime, period, reverse);
		int start = 0;
		int end = 0;
		
		int COLUMNS = 3;
		double[] lat = null;
		double[] lon = null;
		if (positionData)
			COLUMNS = 5;
		
		if (reverse) { // then we take records nearest the end
			start = rtRecords.size()-period;
			end = rtRecords.size();
		} else {
			// we need to find the start point
			start = rtRecords.getNearestFrameIndex(id.foxId, fromUptime, fromReset);
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
		if (positionData) {
			lat = new double[end-start];
			lon = new double[end-start];
		}
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			if (Config.displayRawValues)
				results[j] = rtRecords.get(i).getRawValue(name);
			else
				results[j] = rtRecords.get(i).getDoubleValue(name, id);
			if (positionData) {
				lat[j] = rtRecords.get(i).satLatitude;
				lon[j] = rtRecords.get(i).satLongitude;
			}
			upTime[j] = rtRecords.get(i).getUptime();
			resets[j--] = rtRecords.get(i).getResets();
		}
		
		double[][] resultSet = new double[COLUMNS][end-start];
		resultSet[PayloadStore.DATA_COL] = results;
		resultSet[PayloadStore.UPTIME_COL] = upTime;
		resultSet[PayloadStore.RESETS_COL] = resets;
		if (positionData) {
			resultSet[PayloadStore.LAT_COL] = lat;
			resultSet[PayloadStore.LON_COL] = lon;
			
		}
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
	 * Load all of the segments needed between two timestamps and return the number of records in that range
	 * Our first challenge is that either or both timestamps may be outside of a file.
	 * 
	 * @param reset
	 * @param uptime
	 * @param toReset
	 * @param toUptime
	 * @return the number of records in the range
	 * @throws IOException
	 */
	protected int getNumberOfPayloadsBetweenTimestamps(int reset, long uptime, int toReset, long toUptime) throws IOException {
		int fromSeg = findFirstSeg(reset, uptime);
		int toSeg = findFirstSeg(toReset, toUptime);
		int number = 0;
		// Then we need to load segment at i and start counting from here, until we find the toReset and toUptime
		//System.err.println("Loading from seg: "+i);

		int i = fromSeg;
		while(i <= toSeg && i < tableIdx.size()) {
			if (!tableIdx.get(i).isLoaded())
				load(tableIdx.get(i));
			i++;
		}
		int id = rtRecords.get(0).id; // id is the same for all records in this table
		// Now all the segments are loaded that contain the data we want, so find the nearest records and count the distance between
		int start = rtRecords.getNearestFrameIndex(id, uptime, reset);
		int end = rtRecords.getNearestFrameIndex(id, toUptime, toReset);
		if (start < end)
			number = end - start;

		return number;
	}
	
	/**
	 * Search forwards through the segments to find the Segment with this reset/uptime
	 * We search from the earliest index records looking for the first instance where the reset or uptime is greater than the search point.
	 * We would then start loading from the previous record.
	 * @param reset
	 * @param uptime
	 * @return
	 */
	private int findFirstSeg(int reset, long uptime) {
		// load forwards from the relevant reset/uptime
		/* Logic is like this:
			reset x
			uptime y
			
			idx
			fromR	fromU
			0		100
			1		50
			2		900
			3		55
			
			case 1: 
			reset=1
			uptime=100
			We want to load 2/900 onwards
			So:
			reset < fromR, uptime then irrelevant
			
			case 2: - special case at the start
			reset = 0
			uptime = 1
			We want to load 0/100 onwards because we DO NOT HAVE data before it, otherwise it is case 4
			So: reset = fromR, y < fromU
							
			case 3: - special case at the end
			x=3
			y=100
			We want to load 3/55
			x = fromR, y > fromU - load current
			
			case 4: 
			reset=1
			uptime=0
			We want to load 0/100 because the data could be at the end
			x > fromR, y is irrelevent
			AND (x < next fromR OR (x = next from R AND y < uptime) )
		 * 
		 */
		
		boolean loadnow = false;
		for (int i=0; i< tableIdx.size(); i++) {
			if (!loadnow) { // we test the cases
				//case 1:
				if (tableIdx.get(i).fromReset > reset ) { // situation where the data is for a higher reset, so load from here always by default
					loadnow = true;
					//System.out.println("Case 1: " + i);
				} 
				// case 2:
				if (i == 0) 
					if (tableIdx.get(i).fromReset == reset) { // load this.  It might have the data we need and its the last segment
						loadnow = true;
					}
				//case 4:
				if (i < tableIdx.size()-1 &&  tableIdx.get(i).fromReset < reset && // this record has a lower reset
						(tableIdx.get(i+1).fromReset > reset || (tableIdx.get(i+1).fromReset == reset && tableIdx.get(i+1).fromUptime > uptime))) { // but the next record has higher reset or same reset and high uptime
					loadnow = true;
					//System.out.println("Case 4: " + i);
				}
				//case 4b:
				if (i < tableIdx.size()-1 &&  tableIdx.get(i).fromReset == reset && tableIdx.get(i).fromUptime < uptime && // this record has the same reset and uptime is less than the target
						(tableIdx.get(i+1).fromReset > reset || (tableIdx.get(i+1).fromReset == reset && tableIdx.get(i+1).fromUptime > uptime))) { // but the next record has higher reset or same reset and uptime higher
					loadnow = true;
					//System.out.println("Case 4b: " + i);

				}
				//case 3:
				if (i == tableIdx.size()-1 && tableIdx.get(i).fromReset <= reset) { // load this.  It might have the data we need and its the last segment
					loadnow = true;
					//System.out.println("Case 3: " + i);

				}
			}
			if (loadnow) return i;
		}
		return -99;
		
	}
	
	private int findLastSeg(int reset, long uptime) {
		return 0;
	}
	
	/**
	 * Load all of the segments needed so that "number" of records is available.  Used for plotting graphs.  If segments are missing then
	 * we do not create them
	 * @param reset
	 * @param uptime
	 * @param number
	 * @throws IOException
	 */
	private void loadSegments(int reset, long uptime, int number, boolean reverse) throws IOException {
		int total = 0;
		if (reverse) {
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
			int i = findFirstSeg(reset, uptime);
			// Then we need to load segment at i and start counting from here
			//System.err.println("Loading from seg: "+i);
			if (i >= 0)
				while(i < tableIdx.size()) {
					if (!tableIdx.get(i).isLoaded())
						load(tableIdx.get(i));
					total += tableIdx.get(i++).records;
					if (total >= number+MAX_SEGMENT_SIZE) break; // add an extra segment because often we start from the segment before
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
        	Log.println(e.getMessage());
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        	Log.println(n.getMessage());
        } finally {
        	dis.close();
        }

	}

	private FoxFramePart addLine(String line) {
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
		FoxFramePart rt = null;
		if (type == FoxFramePart.TYPE_REAL_TIME) {
			rt = new PayloadRtValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.REAL_TIME_LAYOUT));
		}
		if (type == FoxFramePart.TYPE_WOD) {
			rt = new PayloadWOD(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_LAYOUT));
		}
		if (type == FoxFramePart.TYPE_MAX_VALUES) {
			rt = new PayloadMaxValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.MAX_LAYOUT));

		}
		if (type == FoxFramePart.TYPE_MIN_VALUES) {
			rt = new PayloadMinValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.MIN_LAYOUT));

		}
		if (type == FoxFramePart.TYPE_RAD_TELEM_DATA || type >= 700 && type < 800) {
			rt = new RadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.RAD2_LAYOUT));
			rt.type = type; // make sure we get the right type
		}
		if (type == FoxFramePart.TYPE_WOD_RAD_TELEM_DATA ) {
			rt = new WodRadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD2_LAYOUT));
			rt.type = type; // make sure we get the right type
		}
		if (type == FoxFramePart.TYPE_RAD_EXP_DATA || type >= 400 && type < 500) {
			rt = new PayloadRadExpData(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.RAD_LAYOUT));
			rt.type = type; // make sure we get the right type
			
			// hack to convert data - only used in testing
			if (Config.generateSecondaryPayloads) {
				PayloadRadExpData f = (PayloadRadExpData)rt; 
				RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
				radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
				if (f.type >= 400) // this is a high speed record
					radiationTelemetry.type = f.type + 300; // we give the telem record 700+ type
				Config.payloadStore.add(f.id, f.uptime, f.resets, radiationTelemetry);
				Config.payloadStore.setUpdated(id, Spacecraft.RAD_LAYOUT, true);			
			}
		}
		if (type == FoxFramePart.TYPE_WOD_RAD) {
			rt = new PayloadWODRad(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD_LAYOUT));
		}
		if (type == FoxFramePart.TYPE_HERCI_HIGH_SPEED_DATA || type >= 600 && type < 700) {
			rt = new PayloadHERCIhighSpeed(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_LAYOUT));
			rt.type = type; // make sure we get the right type
			if (Config.generateSecondaryPayloads) {
				// Test routine that generates the secondary payloads
				PayloadHERCIhighSpeed f = (PayloadHERCIhighSpeed)rt;
				HerciHighspeedHeader radiationTelemetry = f.calculateTelemetryPalyoad();
				radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
				if (f.type >= 600) // this is a high speed record
					radiationTelemetry.type = f.type + 200; // we give the telem record 800+ type
				Config.payloadStore.add(f.id, f.uptime, f.resets, radiationTelemetry);

				//updatedHerciHeader = true;

				ArrayList<HerciHighSpeedPacket> pkts = f.calculateTelemetryPackets();
				for(int i=0; i< pkts.size(); i++) {
					HerciHighSpeedPacket pk = pkts.get(i);
					pk.captureHeaderInfo(f.id, f.uptime, f.resets);
					if (f.type >= 600) // this is a high speed record
						pk.type = f.type*1000 + 900 + i;; // we give the telem record 900+ type.  Assumes 10 minipackets or less
						Config.payloadStore.add(f.id, f.uptime, f.resets,pk);


				}
			}
		}
		if (type == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER || type >= 800 && type < 900) {
			rt = new HerciHighspeedHeader(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_HEADER_LAYOUT));
			rt.type = type; // make sure we get the right type
		}
		if (type == FoxFramePart.TYPE_HERCI_HS_PACKET || type >= 600900 && type < 700000) {
			rt = new HerciHighSpeedPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		}

		// Check the the record set is actuall loaded.  Sometimes at start up the GUI is querying for records before they are loaded
		if (rtRecords != null && rt != null) {
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
						FoxFramePart rt = addLine(line);
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
	 * @param f
	 * @param log
	 * @throws IOException
	 */
	private void save(FramePart f, String log) throws IOException {

		createNewFile(log);
		//Log.println("Saving: " + log);
		//use buffering and append to the existing file
		File aFile = new File(log );
		Writer output = new BufferedWriter(new FileWriter(aFile, true));
		try {
			output.write( f.toFile() + "\n" );
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
			output.close();
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
        } finally {
        	
        	dis.close();
        }
	}	
	
	public void remove() throws IOException {
		for (TableSeg seg: tableIdx)
			SatPayloadStore.remove(getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName);
		SatPayloadStore.remove(fileName + ".idx");
	}
}
