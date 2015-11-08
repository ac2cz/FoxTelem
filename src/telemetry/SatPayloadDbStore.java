package telemetry;


import gui.MainWindow;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
public class SatPayloadDbStore {

	public static final String ERR_TABLE_DOES_NOT_EXIST = "42S02";
	public static final String ERR_DUPLICATE = "23000";
	public static final String ERR_OPEN_RESULT_SET = "X0X95";
	
	public int foxId;
	public Spacecraft fox;
	
	public static String RT_LOG = "RTTELEMETRY";
	public static String MAX_LOG = "MAXTELEMETRY";
	public static String MIN_LOG = "MINTELEMETRY";
	public static String RAD_LOG = "RADTELEMETRY";
	public static String RAD_TELEM_LOG = "RAD2TELEMETRY";
	
	public String rtTableName;
	public String maxTableName;
	public String minTableName;
	public String radTableName;
	public String radTelemTableName;
	
	private boolean multiUpdate = false;
	
//	SortedFramePartArrayList rtRecords;
//	SortedFramePartArrayList maxRecords;
//	SortedFramePartArrayList minRecords;
//	SortedFramePartArrayList radRecords;
	
	boolean updatedRt = true;
	boolean updatedMax = true;
	boolean updatedMin = true;
	boolean updatedRad = true;
	boolean updatedRadTelem = true;
	
	/**
	 * Create the payload store this this fox id
	 * @param id
	 */
	public SatPayloadDbStore(Spacecraft fox) {
		this.fox = fox;
		foxId = fox.foxId;
		rtTableName = "Fox"+foxId+RT_LOG;
		maxTableName = "Fox"+foxId+MAX_LOG;
		minTableName = "Fox"+foxId+MIN_LOG;
		radTableName = "Fox"+foxId+RAD_LOG;
		radTelemTableName = "Fox"+foxId+RAD_TELEM_LOG;
		
		initPayloadFiles();
	}
	
	private void initPayloadFiles() {
		initPayloadTable(rtTableName, fox.rtLayout);
		initPayloadTable(maxTableName, fox.maxLayout);
		initPayloadTable(minTableName, fox.minLayout);
		initPayloadTable(radTableName, fox.radLayout);
		initPayloadTable(radTelemTableName, fox.rad2Layout);
	}

	/** 
	 *  create the tables if they do not exist
	 */
	private void initPayloadTable(String table, BitArrayLayout layout) {
		Statement stmt = null;
		ResultSet select = null;
		try {
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement();
			select = stmt.executeQuery("select * from " + table);
			select.close();
		} catch (SQLException e) {
			
			if ( e.getSQLState().equals(ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				String createString = "CREATE TABLE " + table + " ";
				createString = createString + layout.getTableCreateStmt();
				
				System.out.println ("Creating new DB table " + table);
				try {
					stmt.execute(createString);
				} catch (SQLException ex) {
					PayloadDbStore.errorPrint(ex);
				}
			} else {
				PayloadDbStore.errorPrint(e);
			}
		} 
	}


	
	public void setUpdatedAll() {
		updatedRt = true;
		updatedMax = true;
		updatedMin = true;
		updatedRad = true;
		updatedRadTelem = true;
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

	private int count(String table) {
		Statement stmt = null;
		ResultSet rs;
		String update = "select count(*) from " + table;
		//Log.println("SQL:" + update);
		try {
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement();
			rs = stmt.executeQuery(update);
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				// ignore. We are probablly starting up or deleting the tables
			} else
				PayloadDbStore.errorPrint(e);
			return 0;
		}
		int count = 0;
		try {
			if (!rs.isClosed() && rs.next());
				count = rs.getInt(1);
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace(Log.getWriter());
			count = 0;
		}
		return count;

	}

	public int getNumberOfFrames() { return count(rtTableName) + count(maxTableName) + count(minTableName) + count(radTableName); }
	public int getNumberOfTelemFrames() { return count(rtTableName) + count(maxTableName) + count(minTableName); }
	public int getNumberOfRadFrames() { return count(radTableName);}
		
	public boolean add(int id, long uptime, int resets, FramePart f) throws IOException {
		f.captureHeaderInfo(id, uptime, resets);
		return add(f);
	}

	private boolean addRadRecord(PayloadRadExpData f)  {
		insert(radTableName,f);
		
		// Capture and store any secondary payloads
		if (f.isTelemetry()) {
			RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
			radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
			add(radiationTelemetry);
		}
		return true;
	}
	/**
	 * Add an array of payloads, usually when we have a set of radiation data from the high speed
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	public boolean add(int id, long uptime, int resets, PayloadRadExpData[] f) {
			for (int i=0; i< f.length; i++) {
				if (f[i].hasData()) {
					f[i].captureHeaderInfo(id, uptime, resets);
					f[i].type = i+100;  // use type as the serial number for high speed type 4s, starting at 100 to distinguish from a Low Speed type 4
					addRadRecord(f[i]);
				}
			}

		return true;
	}

	/**
	 * Insert the data into the database
	 * FIXME - we should use prepared statements that are pre-generated for the class.  One for each payload type
	 * @param table
	 * @param f
	 * @return
	 */
	private boolean insert(String table, FramePart f) {
		Statement stmt = null;
		String update = "insert into " + table;
		update = update + f.getInsertStmt();
		//Log.println("SQL:" + update);
		try {
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement();
			@SuppressWarnings("unused")
			int r = stmt.executeUpdate(update);
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
				Log.println("DUPLICATE RECORD, not stored");
				return true; // We have the data
			} else {
				PayloadDbStore.errorPrint(e);
			}
			return false;
		}
		return true;

	}
	
	/**
	 * Add the frame to the correct array and file
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	private boolean add(FramePart f) {
		if (f instanceof PayloadRtValues ) {
			setUpdatedRt(true);
			return insert(rtTableName,f);
		} else if (f instanceof PayloadMaxValues  ) {
			setUpdatedMax(true);
			return insert(maxTableName,f);
		} else if (f instanceof PayloadMinValues ) {
			setUpdatedMin(true);
			return insert(minTableName,f);
		} else if (f instanceof PayloadRadExpData ) {
			setUpdatedRad(true);
			return addRadRecord((PayloadRadExpData)f);
		} else if (f instanceof RadiationTelemetry ) {
			setUpdatedRadTelem(true);
			return insert(radTelemTableName, f);
		}
		return false;
	}


	private ResultSet selectLatest(String table) {
		Statement stmt = null;
		String update = "  SELECT * FROM " + table + " ORDER BY resets DESC, uptime DESC LIMIT 1"; // Derby Syntax FETCH FIRST ROW ONLY";

		try {
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement();
			//Log.println(update);
			ResultSet r = stmt.executeQuery(update);
			if (r.next()) {
				return r;
			} else {
				return null;
			}
		} catch (SQLException e) {
			PayloadDbStore.errorPrint(e);
		}
		return null;
		
	}
	
	public PayloadRtValues getLatestRt() throws SQLException {
		ResultSet r = selectLatest(rtTableName);
		if (r != null) {
			PayloadRtValues rt = new PayloadRtValues(r, fox.rtLayout);
			r.close();
			return rt;
		} else return null;
	}

	public PayloadMaxValues getLatestMax() throws SQLException {
		ResultSet r = selectLatest(maxTableName);
		if (r != null) {
			PayloadMaxValues max = new PayloadMaxValues(r, fox.maxLayout);
			r.close();
			return max;
		} else return null;
	}

	public PayloadMinValues getLatestMin() throws SQLException {
		ResultSet r = selectLatest(minTableName);
		if (r != null) {
			PayloadMinValues min = new PayloadMinValues(r, fox.minLayout);
			r.close();
			return min;
		} else return null;
	}

	public PayloadRadExpData getLatestRad() throws SQLException {
		ResultSet r = selectLatest(radTableName);
		if (r != null) {
			PayloadRadExpData rad = new PayloadRadExpData(r, fox.radLayout);
			r.close();
			return rad;
		} else return null;
	}


	/**
	 * Try to return an array with "period" entries for this attribute, starting with the most 
	 * recent
	 * 
	 * @param name
	 * @param period
	 * @return
	 * @throws SQLException 
	 */
	public double[][] getRtGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws SQLException {
		return getGraphData(rtTableName, name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws SQLException {
		return getGraphData(maxTableName, name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws SQLException {
		return getGraphData(minTableName, name, period, id, fromReset, fromUptime);
		
	}

	public void initRad2() {
		ResultSet rs;
		String where = "select * from " + this.radTableName;
		Statement stmt = null;
		try {
			//Log.println("SQL:" + update);
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(where);
			
			while (rs.next()) {
				PayloadRadExpData f = new PayloadRadExpData(rs, fox.radLayout);
				// Capture and store any secondary payloads
				if (f.isTelemetry()) {
					RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
					radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
					add(radiationTelemetry);
				}
				
			}
			rs.close();

		} catch (SQLException e) {
			PayloadDbStore.errorPrint(e);
		}
	}
	
	/**
	 * Return an array of radiation data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws SQLException 
	 */
	public String[][] getRadData(int period, int id, int fromReset, long fromUptime) throws SQLException {
		ResultSet rs;
		String where = "";
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			where = " ORDER BY resets DESC, uptime DESC ";
		} else {
			where = " where uptime >= "+ fromUptime + " and resets >= " + fromReset +
					" ORDER BY resets DESC, uptime DESC ";
		}
		rs = selectRows(radTableName,null, where,period);
		
		int size =0;
		if (rs != null) {
		  rs.beforeFirst();
		  rs.last();
		  size = rs.getRow();
		}
		String[][] resultSet = new String[size][PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE+3];

		int i=0;

		if (size > 0) {
			resultSet[i][0] = ""+rs.getInt("resets");
			resultSet[i][1] = ""+rs.getLong("uptime");
			for (int j=0; j<PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; j++)
				resultSet[i][j+2] = ""+rs.getInt(j+6);
			i++;
			while (rs.previous()) {
				resultSet[i][0] = ""+rs.getInt("resets");
				resultSet[i][1] = ""+rs.getLong("uptime");
				for (int j=0; j<PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; j++)
					resultSet[i][j+2] = ""+rs.getInt(j+6);
				i++;
			}
		} else {
			
		}
		rs.close();
		return resultSet;

	
	}

	private ResultSet selectRows(String table, String name, String where, int numberOfRows) {
		Statement stmt = null;
		String update = "";
		if (name == null) {
			update = " SELECT * FROM ";
		} else {
			update = " SELECT resets, uptime, " + name + " FROM ";
		}
		//DERBY SYNTAX - update = update + table + where + " FETCH NEXT " + numberOfRows + " ROWS ONLY";
		update = update + table + where + " LIMIT " + numberOfRows;
		try {
			//Log.println("SQL:" + update);
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet r = stmt.executeQuery(update);
			return r;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint(e);
		}
		return null;
		
	}

    
	private double[][] getGraphData(String table, String name, int period, Spacecraft fox, int fromReset, long fromUptime) throws SQLException {
		ResultSet rs;
		String where = "";
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			where = " ORDER BY resets DESC, uptime DESC ";
		} else {
			where = " where uptime >= "+ fromUptime + " and resets >= " + fromReset +
					" ORDER BY resets DESC, uptime DESC ";
		}
		//FIXME - we get all of the columns so that we can populate at Payload record - see below
		rs = selectRows(table,name, where,period);
		
		int size =0;
		if (rs != null) {
		  rs.beforeFirst();
		  rs.last();
		  size = rs.getRow();
		}
		double[] results = new double[size];
		double[] upTime = new double[size];
		double[] resets = new double[size];

		int i=0;

		if (Config.displayRawValues)
			;//FIXME conversion = 0;
		if (size > 0) {
			resets[i] = rs.getInt("resets");
			upTime[i] = rs.getLong("uptime");
			//FIXME - we need a payload record so that we can access the right conversion.  But this means we need all the columns....bad
			//PayloadRtValues rt = new PayloadRtValues(rs, fox.rtLayout);
			results[i++] = rs.getDouble(name);
			while (rs.previous()) {
				resets[i] = rs.getInt("resets");
				upTime[i] = rs.getLong("uptime");
				//rt = new PayloadRtValues(rs, fox.rtLayout);
				results[i++] = rs.getDouble(name);
			}
		} else {
			results = new double[1];
			upTime = new double[1];
			resets = new double[1];
		}
		double[][] resultSet = new double[3][size];
		resultSet[PayloadStore.DATA_COL] = results;
		resultSet[PayloadStore.UPTIME_COL] = upTime;
		resultSet[PayloadStore.RESETS_COL] = resets;
		rs.close();
		return resultSet;

	}


	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
        drop(rtTableName);
        drop(maxTableName);
        drop(minTableName);
        drop(radTableName);
        initPayloadFiles();
        setUpdatedAll();

	}
	
	private void drop(String table) {
		Statement stmt = null;
		try {
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement();
			@SuppressWarnings("unused")
			boolean res = stmt.execute("drop table " + table);
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_OPEN_RESULT_SET) ) {  // Open Result set
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
				}
				try {
					Log.println("RETRYING.....");
					boolean res = stmt.execute("drop table " + table);
				} catch (SQLException e1) {
					Log.println("RETRY FAILED");
					PayloadDbStore.errorPrint(e);
				}
			}
			if ( e.getSQLState().equals(ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				// then we don't care cause its already gone
			} else {
				PayloadDbStore.errorPrint(e);
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

