package telemetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import telemetry.payloads.PayloadCanExperiment;
import telemetry.uw.UwCanPacket;

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
	private boolean storeMode = false;
	private BitArrayLayout layout;
	private boolean isFOXDB_V3;

	public SatPayloadTable(int size, String name, BitArrayLayout layout, boolean storeMode, boolean isFOXDB_V3) throws IOException {
		tableIdx = new SortedArrayList<TableSeg>(INITIAL_SIZE);
		baseFileName = name;
		this.layout = layout;
		this.storeMode = storeMode;
		this.isFOXDB_V3 = isFOXDB_V3;
		String dir = getDir();
        fileName = dir + PayloadStore.DB_NAME+File.separator + name;
      
		rtRecords = new SortedFramePartArrayList(size);
		loadIdx();
		updated = true;
	}
	
	public SortedFramePartArrayList getFrameParts(int fromReset, long fromUptime, int period, boolean reverse) throws IOException {
		if (rtRecords == null) return null;
		loadSegments(fromReset, fromUptime, period, reverse);
		//if (rtRecords == null) return null;
		return rtRecords;
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
		TableSeg seg = loadSeg(resets, uptime, false);
		return rtRecords.hasFrame(id, uptime, resets); }
	
	public FramePart getLatest() throws IOException {
		if (tableIdx.size() > 0) {
			TableSeg lastSeg = tableIdx.get(tableIdx.size()-1);
			if (!lastSeg.isLoaded()) {
				load(lastSeg);
				
			} else {
				lastSeg.accessed();
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
		TableSeg seg = loadSeg(resets, uptime, prev);
		if (seg == null) return null;
		if (seg.records == 0) return null;
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
	
	public FramePart getFrame(int id, long uptime, int resets, int type, boolean prev) throws IOException { 
		// Make sure the segment is loaded, so we can check
		@SuppressWarnings("unused")
		TableSeg seg = loadSeg(resets, uptime, prev);
		if (prev) {
			if (seg == null) return null;
			int i = rtRecords.getNearestPrevFrameIndex(id, uptime, resets, type); 
			if (i == -1) return null;
			return rtRecords.get(i);
		} else {
			int i = rtRecords.getNearestFrameIndex(id, uptime, resets, type); 
			if (i == -1) return null;
			return rtRecords.get(i);
		}
	}
	
	public String[][] getPayloadData(int period, int id, int fromReset, long fromUptime, int length, boolean reverse) throws IOException {
		return getPayloadData(period, id, fromReset, fromUptime, length, false, reverse);
	}
	
	
	/**
	 * Return an array of payloads data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * 
	 * If period is 0 then we want records that exactly match the reset and uptime, up to a maximum of 100.  As payloads must belong to the same
	 * frame, this is just a practical limit and should not be exceeded
	 * 
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 */
	public synchronized String[][] getPayloadData(int period, int id, int fromReset, long fromUptime, int length, boolean returnType, boolean reverse) throws IOException {
		boolean exactMatch = false;
		if (period == 0) {
			exactMatch = true;
			period = 100;
		}
		if (rtRecords == null) return null;
		deleteLock = true;
		try {
			loadSegments(fromReset, fromUptime, period, reverse);
			if (rtRecords.size() == 0) {
				// nothing to query
				return null;
			}
			int start = 0;
			int end = 0;

			if (reverse) { // then we take records nearest the end
				if (exactMatch) {
					end = rtRecords.getNearestFrameIndex(id, fromUptime, fromReset);
					start = rtRecords.getNearestPrevFrameIndex(id, fromUptime, fromReset);
				} else {
					start = rtRecords.size()-period;
					end = rtRecords.size();
				}
			} else {
				// we need to find the start point
				start = rtRecords.getNearestFrameIndex(id, fromUptime, fromReset);
				if (start == -1 ) start = rtRecords.size()-period;
				if (exactMatch) {
					end = rtRecords.getNearestPrevFrameIndex(id, fromUptime, fromReset);
				} else
					end = start + period;
			}
			if (end > rtRecords.size()) end = rtRecords.size();
			if (end < start) end = start;
			if (start < 0) start = 0;
			if (start > rtRecords.size()) start = rtRecords.size();
			if (start == end) end = start +1;

			int[][] results = new int[end-start][];
			String[] upTime = new String[end-start];
			String[] resets = new String[end-start];
			String[] type = null;

			if (returnType)
				type = new String[end-start];

			int j = results.length-1;
			for (int i=end-1; i>= start; i--) {
				//System.out.println(rtRecords.size());
				results[j] = rtRecords.get(i).getFieldValues();
				if (returnType)
					type[j] = ""+rtRecords.get(i).type; // get type returns a different type for some payloads, e.g. HerciPackets.  Reference directly
				upTime[j] = ""+rtRecords.get(i).getUptime();
				resets[j--] = ""+rtRecords.get(i).getResets();
			}

			// Create a results set, with reset, uptime and the data on the same line
			int offset = 2;
			if (returnType) offset = 3;
			String[][] resultSet = new String[end-start][length+offset];   // removed +1 to debug CAN Pkt display issue 8/14/2018
			for (int r=0; r< end-start; r++) {
				resultSet[r][0] = resets[r];
				resultSet[r][1] = upTime[r];
				if (returnType)
					resultSet[r][2] = type[r];
				for (int k=0; k<results[r].length; k++)
					resultSet[r][k+offset] = ""+results[r][k];
			}

			return resultSet;
		} finally {
			deleteLock = false;
		}
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
	synchronized double[][] getGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime, boolean positionData, boolean reverse) throws IOException {
		deleteLock = true;
		try {
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
			else {
//				if (rtRecords.get(i).getStringValue(name, id).equalsIgnoreCase("invalid")) {
//					;
//				} else
				results[j] = rtRecords.get(i).getDoubleValue(name, id);
			}
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
		} finally {
			deleteLock = false;
		}
	}
		
	/**
	 * If Prev is true then we are searching for the previous record.  We do not create a new seg if it is missing
	 * and we do not need reset to be the same in the seg we found.  We just want the previous record.
	 * @param reset
	 * @param uptime
	 * @param prev
	 * @return
	 * @throws IOException
	 */
	private TableSeg getSeg(int reset, long uptime, boolean prev) throws IOException {
		if (Config.debugSegs) Log.println("SEG-GET: " + this.fileName + ":" + reset + ":" + uptime);
		for (int i=tableIdx.size()-1; i>=0; i--) {
			if (tableIdx.get(i).fromReset <= reset && tableIdx.get(i).fromUptime <= uptime) {
				return tableIdx.get(i);
			} else if (prev && tableIdx.get(i).fromReset < reset) {
				return tableIdx.get(i);
			}
		}
		if (prev) return null;
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
	private TableSeg loadSeg(int reset, long uptime, boolean prev) throws IOException {
		if (Config.debugSegs) Log.println("SEG-LOAD: " + this.fileName + ":" + reset + ":" + uptime);
		TableSeg seg = getSeg(reset, uptime, prev);
		if (seg == null) return null;
		seg.accessed();
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
	protected synchronized int getNumberOfPayloadsBetweenTimestamps(int reset, long uptime, int toReset, long toUptime) throws IOException {
		deleteLock = true;
		try {
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
		} finally {
			deleteLock = false;
		}
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
					if (tableIdx.get(i).fromReset == reset && tableIdx.get(i).fromUptime > uptime) { // load this.  It might have the data we need and its the earliest segment
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
	
	/*
	private int findLastSeg(int reset, long uptime) {
		return 0;
	}
	*/
	
	/**
	 * Load all of the segments needed so that "number" of records is available.  Used for plotting graphs.  If segments are missing then
	 * we do not create them
	 * @param reset
	 * @param uptime
	 * @param number
	 * @throws IOException
	 */
	private synchronized void loadSegments(int reset, long uptime, int number, boolean reverse) throws IOException {
		boolean existingLock = deleteLock;
		deleteLock = true;
		int total = 0;
		try {
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
					} else {
						tableIdx.get(i).accessed();
					}
					total += tableIdx.get(i).records;

				}
//				System.err.println("Success we got: "+total+" records and needed "+number);
			} else {
				int i = findFirstSeg(reset, uptime);
				// Then we need to load segment at i and start counting from here
				//System.err.println("Loading from seg: "+i);
				if (i >= 0)
					while(i < tableIdx.size()) {
						if (!tableIdx.get(i).isLoaded()) {
							load(tableIdx.get(i));
						} else {
							tableIdx.get(i).accessed();
						}
						total += tableIdx.get(i++).records;
						if (total >= number+MAX_SEGMENT_SIZE) break; // add an extra segment because often we start from the segment before
					}
			}
		} finally {
			deleteLock = existingLock; // if this was already set then leave it set. e.g. called from data load
		}
	}
	
	boolean deleteLock = false;
	
	public synchronized void offloadSegments() {
		while (deleteLock)
			try {
				Thread.sleep(10); // wait
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		boolean resized = false;
		for (TableSeg seg : tableIdx) {
			if (seg.isStale()) {
				offloadSeg(seg);
				resized = true;
			}
		}
		if (resized)
			rtRecords.trimToSize(); // now try to reclaim the space
	}
	
	/**
	 * Offload this segment.  Assume that segment on disk and a continuous run of records in memory are
	 * the same.  This may not be exact in some edge cases, so we may leave a record or two in memory.
	 * @param seg
	 */
	private void offloadSeg(TableSeg seg) {
		if (rtRecords == null || rtRecords.size() == 0) return;
		if (Config.debugSegs) Log.println("Offloaded SEG: " + seg.toString());
		boolean foundStart = false;
		int removed = 0;
		seg.setLoaded(false);
		for (int i=0; i<rtRecords.size();i++) {
			if (!foundStart) {
				FramePart f = rtRecords.get(i);
				if (f != null)
					if (f.resets == seg.fromReset && f.uptime == seg.fromUptime) {
						// we have the first record, so we offload them
						foundStart = true;
					}
			}
			// Now if we found start we can remove the record and subsequent ones
			if (foundStart) {
				rtRecords.remove(i);
				removed++;
				if (removed == seg.records) {
					break; // we are done
				}
			}
		}		
	}
	
	/**
	 * Save a new record to disk		
	 * @param f
	 */
	public synchronized boolean save(FramePart f) throws IOException {
		deleteLock = true;
		try {
		// Make sure this segment is loaded, or create an empty segment if it does not exist
		TableSeg seg = loadSeg(f.resets, f.uptime, false);
		if (rtRecords.add(f)) {
		//if (!rtRecords.hasFrame(f.id, f.uptime, f.resets)) {
			updated = true;
			//System.out.println(this.fileName + " TABLE UPDATED");
			if (seg.records == MAX_SEGMENT_SIZE) {
				// We need to add a new segment with this as the first record
				TableSeg newseg = new TableSeg(f.resets, f.uptime, baseFileName);
				boolean added = tableIdx.add(newseg);
				if (added) // fails to add if reset/uptime has not changed, e.g. if all records from the same highspeed record.  Only issue in testing if segments very short
					seg = newseg; 
			}
			save(f, getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName);
			seg.records++;
			saveIdx();
			//return rtRecords.add(f);
			
			return true;
		} else {
			if (Config.debugFieldValues) Log.println("DUPLICATE (or corrupt) RECORD, not saved: " + f.resets +":"+ f.uptime + " Ty:" + f.type);
		}
			return false;
		} finally {
			deleteLock = false;
		}
	}
	
	/**
	 * Load a payload file from disk
	 * Payload files are stored in separate logs, but this routine is written so that it can load mixed records
	 * from a single file
	 * @param log
	 * @throws IOException 
	 */
	public synchronized void load(TableSeg seg) throws IOException {
		String log = getDir() + PayloadStore.DB_NAME+File.separator + seg.fileName;
        String line;
        createNewFile(log);
 
        BufferedReader dis = new BufferedReader(new FileReader(log));
        deleteLock = true;
        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			addLine(line);
        		}
        	}
        	seg.setLoaded(true);
        	if (Config.debugSegs) Log.println("Loaded SEG: " + seg.toString());
        	dis.close();
        } catch (IOException e) {
        	e.printStackTrace(Log.getWriter());
        	Log.println(e.getMessage());
        } catch (NumberFormatException n) {
        	n.printStackTrace(Log.getWriter());
        	Log.println(n.getMessage());
        } finally {
        	dis.close();
        	deleteLock = false;
        }

	}

	@SuppressWarnings("deprecation")
	private FramePart addLine(String line) {
		if (line.length() == 0) return null;
		String date = null;
		int id = 0;
		int resets = 0;
		long uptime = 0;
		int type = 0;
		int mode = 0;
		StringTokenizer st = null;
		try {
			st = new StringTokenizer(line, ",");
			date = st.nextToken();
			id = Integer.valueOf(st.nextToken()).intValue();
			resets = Integer.valueOf(st.nextToken()).intValue();
			uptime = Long.valueOf(st.nextToken()).longValue();
			type = Integer.valueOf(st.nextToken()).intValue();
			if (storeMode)
				mode = Integer.valueOf(st.nextToken()).intValue();

			// We should never get this situation, but good to check..
			if (Config.satManager.getSpacecraft(id) == null) {
				Log.errorDialog("FATAL", "Attempting to Load payloads from the Payload store for satellite with Fox Id: " + id 
						+ "\n when no sattellite with that FoxId is configured.  Add this spacecraft to the satellite directory and restart FoxTelem."
						+ "\nProgram will now exit");
				System.exit(1);
			}
			
			FramePart rt = null;
			
			if (type == FramePart.TYPE_UW_CAN_PACKET_TELEM || type >= 1700 && type < 1800) {
				String[] st2 = line.split(",");
				int canIdField = 5;
				if (storeMode)
					canIdField = 6;
				int pktid = Integer.valueOf(st2[canIdField]).intValue();
				int pktid1 = Integer.valueOf(st2[canIdField+1]).intValue();
				int pktid2 = Integer.valueOf(st2[canIdField+2]).intValue();
				int pktid3 = Integer.valueOf(st2[canIdField+3]).intValue();
				int canId = UwCanPacket.getIdFromRawBytes(pktid,pktid1,pktid2,pktid3);
				BitArrayLayout canLayout = Config.satManager.getLayoutByCanId(id, canId);
				rt = new UwCanPacket(id, resets, uptime, date, st, canLayout);

				if (rt != null)
					rt.type = type; // make sure we get the right type
			} else {
				if (isFOXDB_V3) {
					rt = FramePart.makePayload(id, resets, uptime, date, st, layout);
					rt.type = type; // this is used as a sequence number in V3
				} else
					rt = FramePart.makeLegacyPayload(id, resets, uptime, date, st, type);
			}
			

			// Check the the record set is actually loaded.  Sometimes at start up the GUI is querying for records before they are loaded
			if (rtRecords != null && rt != null) {
				if (storeMode)
					rt.newMode = mode;
				try {
				rtRecords.add(rt);
				} catch (NullPointerException e) {
					rtRecords.add(rt); // try again, sometimes we get an issue from the SortedArrayList
				}
			}
			return rt;
		} catch (NoSuchElementException e) {
			Log.errorDialog("ERROR: Corrupted record",  
					" Could not load record. If this is test data \nuse File>Delete Payloads once FoxTelem has started." +
					"\nThis record will be ignored.");
			// we are done and can finish
			return null;
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.errorDialog("ERROR: Too many fields: Index out of bounds", e.getMessage() + 
					" Could not load line for SAT: " + id + " Reset:" + resets + " Up:" + uptime + " Type:" + type);
			return null;
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  " + n.getMessage() + " Could not load frame " + id + " " + resets + " " + uptime + " " + type);
			Log.errorDialog("LOAD ERROR - DEBUG MESSAGE", "ERROR: Invalid number:  " + n.getMessage() + " Could not load frame " + id + " " + resets + " " + uptime + " " + type);
			return null;
		} 
//		catch (NullPointerException n) {
//			Log.println("ERROR: Null Pointer:  " + n.getMessage() + " Could not load frame " + id + " " + resets + " " + uptime + " " + type);
//			Log.errorDialog("LOAD ERROR - DEBUG MESSAGE", "ERROR: Null Pointer:  " + n.getMessage() + " Could not load frame " + id + " " + resets + " " + uptime + " " + type);
//			return null;
//		}
		
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
	 * @param f
	 * @param log
	 * @throws IOException
	 */
	private void save(FramePart f, String log) throws IOException {
		boolean appendNewLine = false;
		if (!createNewFile(log)) {
			// the file was not new, so check to see if the last written line finsihed correctly, otherwise clean it up.
			if (!newLineExists(log) ) {
				appendNewLine = true;
			}
		}
		//Log.println("Saving: " + log);

		//use buffering and append to the existing file
		File aFile = new File(log );
		Writer output = new BufferedWriter(new FileWriter(aFile, true));
		try {
			if (appendNewLine)
				output.write( "\n" );
			output.write( f.toFile(storeMode) + "\n" );
			output.flush();
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}
	}
	
	public static boolean newLineExists(String log) throws IOException {
		File file = new File(log );
	    RandomAccessFile fileHandler = new RandomAccessFile(file, "r");
	    long fileLength = fileHandler.length() - 1;
	    if (fileLength < 0) {
	        fileHandler.close();
	        return true;
	    }
	    fileHandler.seek(fileLength);
	    byte readByte = fileHandler.readByte();
	    fileHandler.close();

	    if (readByte == 0xA || readByte == 0xD) {
	        return true;
	    }
	    return false;
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
