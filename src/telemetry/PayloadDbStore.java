package telemetry;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import measure.Measurement;
import measure.PassMeasurement;
import measure.RtMeasurement;
import telemServer.StpFileProcessException;
import telemetry.uw.CanPacket;
import common.Config;
import common.Log;
import common.Spacecraft;
import common.FoxSpacecraft;

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
 * 
 * This stores the payloads for all of the satellites.  The class callings the methods does not need to know
 * how the data is stored.  The data could be moved into an SQL database in the future and it should make no 
 * difference to code outside of this class
 * 
 *
 */
public class PayloadDbStore extends FoxPayloadStore implements Runnable {
	public static final int DATA_COL = 0;
	public static final int UPTIME_COL = 1;
	public static final int RESETS_COL = 2;
	@SuppressWarnings("unused")
	private boolean running = true;
	@SuppressWarnings("unused")
	private boolean done = false;
	
	//private List<FramePart> payloadQueue;
	
	public Connection derby;

	static String url = "jdbc:mysql://localhost:3306/"; //FOXDB?autoReconnect=true";
    static String db = "FOXDB";
	static String user = "g0kla";
    static String password = "amsatfox";

	SatPayloadDbStore[] payloadStore;
	
	public PayloadDbStore(String u, String pw, String database) {
		db = database;
		user = u;
		password = pw;
		//payloadQueue = Collections.synchronizedList(new SortedFramePartArrayList(INITIAL_QUEUE_SIZE));
		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
		
	    Statement st = null;
	    ResultSet rs = null;
	        
        
        try {
            derby = getConnection();
            st = derby.createStatement();
            rs = st.executeQuery("SELECT VERSION()");

            if (rs.next()) {
                Log.println("Connectted to MYSQL FOXDB Version: " + rs.getString(1));
            }

            initStpHeaderTable();
            initT0LogTable();
        } catch (SQLException ex) {
           Log.println(ex.getMessage());
           System.err.print("FATAL: Could not connect to DB");
           Log.alert("FATAL: Could not connect to DB");
           //System.exit(1);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }

            } catch (SQLException ex) {
                Log.println(ex.getMessage());
            }
        }
        // Check for and create the images directory if it does not exist
        String dir = CameraJpeg.IMAGES_DIR;
        if (!Config.logFileDirectory.equalsIgnoreCase("")) {
        	dir = Config.logFileDirectory + File.separator + dir;

        }
        File aFile = new File(dir);
        if(!aFile.isDirectory()){
        	aFile.mkdir();
        	Log.println("Making directory: " + dir);
        }
        if(!aFile.isDirectory()){
        	Log.println("FATAL: can't create the images directory: " + aFile.getAbsolutePath());
        	Log.alert("FATAL: can't create the images directory: " + aFile.getAbsolutePath());
        }
        payloadStore = new SatPayloadDbStore[sats.size()];
        //pictureStore = new SatPictureStore[sats.size()];
        for (int s=0; s<sats.size(); s++) {
        	payloadStore[s] = new SatPayloadDbStore(this, (FoxSpacecraft) sats.get(s));
			//if (sats.get(s).hasCamera()) pictureStore[s] = new SatPictureStore(sats.get(s).foxId);;
			
		}
	}
	
	public void initRad2() {
		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
		for (int s=0; s<sats.size(); s++) {
			payloadStore[s] = new SatPayloadDbStore(this, (FoxSpacecraft) sats.get(s));
			payloadStore[s].initRad2();
		}
	}
	
	public void initHerciPackets() {
		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
		for (int s=0; s<sats.size(); s++) {
			//if (sats.get(s).isFox1()) {
				FoxSpacecraft fox = (FoxSpacecraft)sats.get(s);
				if (fox.hasHerci()) {
					payloadStore[s] = new SatPayloadDbStore(this, (FoxSpacecraft) sats.get(s));
					payloadStore[s].initHerciPackets();
				}
			//}
		}
	}
	public Connection getConnection() throws SQLException {
		if (derby == null || !derby.isValid(2))  // check that the connection is still valid, otherwise reconnect
            derby = DriverManager.getConnection(url + db + "?autoReconnect=true", user, password);
		return derby;

	}

	public void closeConnection() throws SQLException {
		if (derby != null) 
            derby.close();
	}

	
	public boolean hasQueuedFrames() {
		//if (payloadQueue.size() > 0) return true;
		return false;
	}
	
	private SatPayloadDbStore getPayloadStoreById(int id) {
		for (SatPayloadDbStore store : payloadStore)
			if (store != null)
				if (store.foxId == id) return store;
		return null;
	}
	/*
	private SatPictureStore getPictureStoreById(int id) {
		for (SatPictureStore store : pictureStore)
			if (store != null)
				if (store.foxId == id) return store;
		return null;
	}
*/
	public void setUpdatedAll() {
		for (SatPayloadDbStore store : payloadStore)
		if (store != null)
			store.setUpdatedAll();
	}

	public void setUpdatedAll(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedAll();
	}
	
	public boolean getUpdatedRt(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedRt();
		return false;
	}
	
	public void setUpdatedRt(int id, boolean u) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedRt(u);
	}
	
	public boolean getUpdatedMax(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedMax();
		return false;
	}
	public void setUpdatedMax(int id, boolean u) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedMax(u);
	}

	public boolean getUpdatedMin(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedMin();
		return false;
	}
	public void setUpdatedMin(int id, boolean u) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedMin(u);
	}
	public boolean getUpdatedRad(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedRad();
		return false;
	}
	public void setUpdatedRad(int id, boolean u) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedRad(u);
	}
	public boolean getUpdatedCamera(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getUpdatedCamera();
		return false;
	}
	public void setUpdatedCamera(int id, boolean u) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			store.setUpdatedCamera(u);
	}
	public int getTotalNumberOfFrames() {
		int total = 0;
		for (SatPayloadDbStore store : payloadStore)
			total += store.getNumberOfFrames();
		return total;
	}
	public int getTotalNumberOfTelemFrames() { 
		int total = 0;
		for (SatPayloadDbStore store : payloadStore)
			total += store.getNumberOfTelemFrames();
		return total;
	}
	public int getTotalNumberOfRadFrames() { 
		int total = 0;
		for (SatPayloadDbStore store : payloadStore)
			total += store.getNumberOfRadFrames();
		return total;
	}

	public int getNumberOfFrames(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfFrames();
		return 0;
	}
	public int getNumberOfTelemFrames(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfTelemFrames();
		return 0;
	}
	public int getNumberOfRadFrames(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfRadFrames();
		return 0;
	}
	
	
	public int getNumberOfPictureCounters(int id) { 
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getNumberOfPictureCounters();
		return 0;
	}
	/*
	public int getNumberOfPictureLines(int id) { 
		SatPictureStore store = getPictureStoreById(id);
		if (store != null)
			return store.getNumberOfPictureLines();
		return 0;
	}
	*/

	public SortedJpegList getJpegIndex(int id, int period, int fromReset, long fromUptime) {
	//	SatPayloadDbStore store = getPayloadStoreById(id);
	//	if (store != null)
	//		return store.jpegIndex;
		return null;
	}

	/**
	 * This add will block while waiting for the SQL database.  This works for the server where each
	 * request is in a seperate thread.  For a GUI application this add would need to be called from a background thread.
	 */
	public boolean add(int id, long uptime, int resets, FramePart f) {
		if (Config.debugFieldValues)
			Log.println(f.toString());
		if (f instanceof PayloadCameraData)
			if (addToDb(id, uptime, resets, (PayloadCameraData)f))
				return true;
			else
				Log.alert("ERROR: Could not add camera record to the database: " + id + " " + uptime+ " " + resets+ " " + f);
		else if (addToDb(id, uptime, resets, f))
			return true;
		else {
			// Serious error where we could not add data to the database.  Something is wrong and we have no way at this point to prevent the STP
			// file from being marked as processed
			//// ALERT
			Log.alert("ERROR: Could not add record to the database: " + id + " " + uptime+ " " + resets+ " " + f);
		}
		return false;
		/*
		boolean rc = false;
		int retries = 0;
		int MAX_RETRIES = 5;
		f.captureHeaderInfo(id, uptime, resets);
		while (retries++ < MAX_RETRIES)
		try {
			rc = payloadQueue.add(f);
			retries = MAX_RETRIES;
		} catch (NullPointerException e) {
			Log.println("NULL POINTER adding to DB Queue, pause then retry");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				
			}
		} catch (IndexOutOfBoundsException e) {
			Log.println("INDEX OUT OF BOUNDS adding to DB Queue, pause then retry");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				
			}
		}

		return rc;
		*/
	}

	public boolean addToDb(int id, long uptime, int resets, FramePart f) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.add(id, uptime, resets, f);
			} catch (IOException e) {
				// FIXME We dont want to stop the decoder but we want to warn the user...
				e.printStackTrace(Log.getWriter());
			}
		return false;
	}
	
	

	/**
	 * Add an array of payloads, usually when we have a set of radiation data from the high speed
	 * @param f
	 * @return
	 */
	public boolean add(int id, long uptime, int resets, PayloadRadExpData[] f) {
		for (int i=0; i< f.length; i++) {
			if (f[i].hasData()) {
				f[i].captureHeaderInfo(id, uptime, resets);
				f[i].type = 400 + i; // store the index in the type field so it is unique
				if (!addToDb(id, uptime, resets, f[i]))
					return false;
				/*
				try {
					payloadQueue.add(f[i]);
				} catch (NullPointerException e) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					payloadQueue.add(f[i]);
				}
				*/
			}
		}
		return true;
	}

	/*
	public boolean addToDb(int id, long uptime, int resets, PayloadRadExpData[] f) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.add(id, uptime, resets, f);
		return false;
	}
	 */
	
	@Override
	public boolean add(int id, long uptime, int resets, PayloadHERCIhighSpeed[] herci) {
		for (int i=0; i< herci.length; i++) {
			herci[i].captureHeaderInfo(id, uptime, resets);
			herci[i].type = 600 + i; // store the index in the type field so it is unique
			if (!addToDb(id, uptime, resets, herci[i]))
				return false;
			/*
			try {
				payloadQueue.add(herci[i]);
			} catch (NullPointerException e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				payloadQueue.add(herci[i]);
			}
			*/
		}
		return true;
	}
	
	private void initStpHeaderTable() {
		String table = "STP_HEADER";
		Statement stmt = null;
		ResultSet select = null;
		try {
			derby = getConnection();
			stmt = derby.createStatement();
			select = stmt.executeQuery("select 1 from " + table + " LIMIT 1");
			
		} catch (SQLException e) {
			
			if ( e.getSQLState().equals(SatPayloadDbStore.ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				String createString = "CREATE TABLE " + table + " ";
				createString = createString + Frame.getTableCreateStmt();
				
				Log.println ("Creating new DB table " + table);
				try {
					stmt.execute(createString);
				} catch (SQLException ex) {
					PayloadDbStore.errorPrint("initStpHeaderTable", ex);
					Log.alert("FATAL: Could not create STP HEADER table");
				}
			} else {
				PayloadDbStore.errorPrint("initStpHeaderTable", e);
				Log.alert("FATAL: Could not access the STP HEADER table");
				
			}
		} finally {
			try { if (select != null) select.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
	}

	private void initT0LogTable() {
		String table = "T0_LOG";
		Statement stmt = null;
		ResultSet select = null;
		try {
			derby = getConnection();
			stmt = derby.createStatement();
			select = stmt.executeQuery("select 1 from " + table + " LIMIT 1");
			
		} catch (SQLException e) {
			
			if ( e.getSQLState().equals(SatPayloadDbStore.ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				String createString = "CREATE TABLE " + table + " ";
				createString = createString +  
				"(id int, resets int, "
						+ "T0_estimate_date_time timestamp NOT NULL,";
				createString = createString + "PRIMARY KEY (id, resets))";;
				
				Log.println ("Creating new DB table " + table);
				try {
					stmt.execute(createString);
				} catch (SQLException ex) {
					PayloadDbStore.errorPrint("initT0LogTable", ex);
					Log.alert("FATAL: Could not create T0_LOG table");
				}
			} else {
				PayloadDbStore.errorPrint("initT0LogTable", e);
				Log.alert("FATAL: Could not access the T0_LOG table");
				
			}
		} finally {
			try { if (select != null) select.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
	}

	@Override
	public boolean addStpHeader(Frame f) {
		PreparedStatement ps = null;
		try {
			derby = getConnection();
			ps = f.getPreparedInsertStmt(derby);
			
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			if ( e.getSQLState().equals(SatPayloadDbStore.ERR_DUPLICATE) ) {  // duplicate
				Log.println("DUPLICATE RECORD, not stored");
				return true; // we consider this data added
			} else {
				PayloadDbStore.errorPrint("addStpHeader", e);
			}
			return false;
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e2) {};
		}
		return true;
	}
	
	public boolean updateStpHeader(Frame f) throws StpFileProcessException {

		Statement stmt = null;
		String update = "update STP_HEADER ";
		update = update + "set rx_location='"+f.rx_location +"', ";
		update = update + "receiver_rf='"+f.receiver_rf +"' ";
		update = update + " where receiver='"+f.receiver+"' ";
		update = update + " and sequenceNumber="+f.sequenceNumber;
		update = update + " and resets="+f.header.resets;
		update = update + " and uptime="+f.header.uptime;
		update = update + " and id="+f.header.id;
		//Log.println("SQL:" + update);
		try {
			derby = getConnection();
			stmt = derby.createStatement();
			int r = stmt.executeUpdate(update);
			if (r > 1) throw new StpFileProcessException("FOXDB","MULTIPLE ROWS UPDATED!");
		} catch (SQLException e) {
			if ( e.getSQLState().equals(SatPayloadDbStore.ERR_DUPLICATE) ) {  // duplicate
				Log.println("DUPLICATE RECORD, not stored");
				return true; // we consider this data added
			} else {
				PayloadDbStore.errorPrint("updateStpHeader", e);
			}
			return false;
		} finally {
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return true;
	}

	/**
	 * Add a camera payload.  This is added to the picture line store one line at a time.  We do not store the actual
	 * camera payloads as there is no additional information that we need beyond the lines.  The raw frame are sent to the server
	 * @param id
	 * @param uptime
	 * @param resets
	 * @param f
	 * @return
	 */
	public boolean addToDb(int id, long uptime, int resets, PayloadCameraData f) {
		SatPayloadDbStore store = getPayloadStoreById(id);  
		if (store != null) {
			ArrayList<PictureScanLine> lines = f.pictureLines;
			for (PictureScanLine line : lines) {
				// Capture the header into the line
				line.id = id;
				line.resets = resets;
				line.uptime = uptime;
				try {
					if (!store.add(line))
						return false;
				} catch (IOException e) {
					// this probably means we did not store the camera payload or could not create the Jpeg.  Perhaps the header was missing etc
					e.printStackTrace(Log.getWriter());
					return false; // we did not add the data
				} catch (ArrayIndexOutOfBoundsException e) {
					// FIXME We dont want to stop the SERVER but we want to warn the operator...
					Log.println("ERROR: CORRUPT CAMERA DATA, line not written: " + id + " " + resets + " " + uptime);
					e.printStackTrace(Log.getWriter());
					return false; // we did not add the data
				} catch (SQLException e) {
					Log.println("SQL ERROR with CAMERA DATA, line not written: " + id + " " + resets + " " + uptime);
					e.printStackTrace(Log.getWriter());
					return false; // we did not add the data
				} 
			}
		}
		return true;
	}

	
	@Override
	public FramePart getLatest(int id, String layout) {
//		SatPayloadDbStore store = getPayloadStoreById(id);
//		if (store != null)
//			try {
//				return store.getLatest(layout);
//			} catch (SQLException e) {
//				e.printStackTrace(Log.getWriter());
//				return null;
//			}
		return null;
	}

	public ArrayList<FramePart> selectCanPackets(int id, String where) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.selectCanPackets(where);
		return null;
	}
	
	public int getLastCanId(int id, String user) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.getLastCanId(user);
		return 0;
	}
	
	public CanPacket getLatestUwCanPacket(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestUwCanPacket();
			} catch (SQLException e) {
				e.printStackTrace(Log.getWriter());
				return null;
			}
		return null;
	}
	
	public boolean storeLastCanId(int id, String date, int pkt_id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.storeLastCanId(date, pkt_id);
		return false;
	}
	
	public PayloadRtValues getLatestRt(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestRt();
			} catch (SQLException e) {
				e.printStackTrace(Log.getWriter());
				return null;
			}
		return null;
	}

	public PayloadMaxValues getLatestMax(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestMax();
			} catch (SQLException e) {
				e.printStackTrace(Log.getWriter());
				return null;
			}
		return null;
	}

	public PayloadMinValues getLatestMin(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestMin();
			} catch (SQLException e) {
				e.printStackTrace(Log.getWriter());
				return null;
			}
		return null;

	}

	public PayloadRadExpData getLatestRad(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestRad();
			} catch (SQLException e) {
				errorPrint("getLatestRad", e);
				e.printStackTrace(Log.getWriter());
				return null;
			}
		return null;

	}
	
	public PayloadUwExperiment getLatestUwExp(int id) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getLatestUwExp();
			} catch (SQLException e) {
				errorPrint("getLatestUwExp", e);
				e.printStackTrace(Log.getWriter());
				return null;
			}
		return null;

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
	public double[][] getRtGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean plot, boolean reverse) {
		SatPayloadDbStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getRtGraphData(name, period, fox, fromReset, fromUptime);
			} catch (SQLException e) {
				Log.println("SQL ERROR!" + e.getMessage());
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean plot, boolean reverse) {
		SatPayloadDbStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getMaxGraphData(name, period, fox, fromReset, fromUptime);
			} catch (SQLException e) {
				errorPrint("getMaxGraphData", e);
				e.printStackTrace(Log.getWriter());
			}
		return null;		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime, boolean plot, boolean reverse) {
		SatPayloadDbStore store = getPayloadStoreById(fox.foxId);
		if (store != null)
			try {
				return store.getMinGraphData(name, period, fox, fromReset, fromUptime);
			} catch (SQLException e) {
				errorPrint("getMinGraphData", e);
				e.printStackTrace(Log.getWriter());
			}
		return null;		
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
	public String[][] getRadData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			try {
				return store.getRadData(period, id, fromReset, fromUptime);
			} catch (SQLException e) {
				Log.println("SQL ERROR!" + e.getMessage());
				e.printStackTrace(Log.getWriter());
			}
		return null;
	}


	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void deleteAll() {
		for (SatPayloadDbStore store : payloadStore)
			if (store != null)
				store.deleteAll();
		
	}

	/**
	 * Delete all of the log files.  This is called from the main window by the user
	 */
	public void delete(Spacecraft sat ) {
		for (SatPayloadDbStore store : payloadStore) {
			if(store != null && sat.foxId == store.foxId) {
				if (store != null)
					store.deleteAll();
			}
		}
	}

	public static void errorPrint(String cause, Throwable e) {
		if (e instanceof SQLException)
			SQLExceptionPrint(cause, (SQLException)e);
		else {
			Log.println("ERROR: "+cause+" A NON SQLException error occured while accessing the DB");
			e.printStackTrace(Log.getWriter());
		}
	} // END errorPrint

	// Iterates through a stack of SQLExceptions
	static void SQLExceptionPrint(String cause, SQLException sqle) {
		while (sqle != null) {
			Log.println("\n---SQLException Caught--- Caused by: "+cause+"\n");
			Log.println("SQLState: " + (sqle).getSQLState());
			Log.println("Severity: " + (sqle).getErrorCode());
			Log.println("Message: " + (sqle).getMessage());
			sqle.printStackTrace(Log.getWriter());
			Log.alert("SERIOUS SQL exception caused by "+cause+".  Need to clear the ALERT and restart the server:\n");
			//sqle.printStackTrace(Log.getWriter());
			sqle = sqle.getNextException();
		}
	} // END SQLExceptionPrint

	/**
	 * The run thread is for inserts, so that we minimize the load on the decoder.  We check the queue of payloads and add any that are in it
	 */
	@Override
	public void run() {

		running = true;
		done = false;
		/*
		while(running) {
			try {
				Thread.sleep(100); // check for new inserts multiple times per second
			} catch (InterruptedException e) {
				Log.println("ERROR: PayloadStore thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 	
			while (payloadQueue.size() > 0) {
				//for (int i=0; i< payloadQueue.size(); i++) {
					FramePart f = (FramePart) payloadQueue.get(0);
					if (Config.debugFieldValues)
						Log.println(f.toString());
					if (f instanceof PayloadCameraData)
						if (addToDb(f.id, f.uptime, f.resets, (PayloadCameraData)f))
							;
						else
							Log.alert("ERROR: Could not add camera record to the database: " + f.id + " " + f.uptime+ " " + f.resets+ " " + f);
					else if (addToDb(f.id, f.uptime, f.resets, f))
						;
					else {
						// Serious error where we could not add data to the database.  Something is wrong and we have no way at this point to prevent the STP
						// file from being marked as processed
						//// ALERT
						Log.alert("ERROR: Could not add record to the database: " + f.id + " " + f.uptime+ " " + f.resets+ " " + f);
					}
					payloadQueue.remove(0);
				//}
			}
		}
		 */
		Log.println("Database Payload store started. No background thread..");
		done = true;
	}

	@Override
	public boolean hasQueuedMeasurements() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getUpdatedMeasurement(int id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setUpdatedMeasurement(int id, boolean u) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getUpdatedPassMeasurement(int id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setUpdatedPassMeasurement(int id, boolean u) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean addToFile(int id, long uptime, int resets, FramePart f) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addToFile(int id, long uptime, int resets,
			PayloadRadExpData[] f) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Not used.  See addToDb method
	 */
	@Override
	public boolean addToPictureFile(int id, long uptime, int resets, PayloadCameraData f) {
		return false;
	}

	@Override
	public boolean add(int id, RtMeasurement m) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addToFile(int id, Measurement m) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RtMeasurement getLatestMeasurement(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(int id, PassMeasurement m) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PassMeasurement getLatestPassMeasurement(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RadiationTelemetry getLatestRadTelem(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getRadTelemData(int period, int id, int fromReset,
			long fromUptime, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] getRadTelemGraphData(String name, int period,
			FoxSpacecraft fox, int fromReset, long fromUptime, boolean plot, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] getMeasurementGraphData(String name, int period,
			FoxSpacecraft fox, int fromReset, long fromUptime, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRtUTCFromUptime(int id, int reset, long uptime) {
		// TODO Auto-generated method stub
		return null;
	}

	

	@Override
	public double[][] getHerciScienceHeaderGraphData(String name, int period, FoxSpacecraft fox, int fromReset,
			long fromUptime, boolean plot, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public PayloadHERCIhighSpeed getLatestHerci(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HerciHighspeedHeader getLatestHerciHeader(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getHerciPacketData(int period, int id, int fromReset, long fromUptime, boolean type, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RadiationTelemetry getRadTelem(int id, int resets, long uptime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean processNewImageLines() throws SQLException, IOException {
		for (SatPayloadDbStore store : payloadStore)
			if (store != null)
				store.processNewImageLines();
		return true; // we don't care if lines were added or not
	}

	@Override
	public double[][] getPassMeasurementGraphData(String name, int period, FoxSpacecraft fox, int fromReset,
			long fromUptime, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	} 

	@Override
	public boolean getUpdated(int id, String lay) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setUpdated(int id, String lay, boolean u) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getTotalNumberOfFrames(String lay) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfFrames(int id, String lay) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[][] getGraphData(String name, int period, Spacecraft fox, int fromReset, long fromUptime,
			String layout, boolean plot, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getWodRadTelemData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getRtData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getWODData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfPayloadsBetweenTimestamps(int id, int reset, long uptime, int toReset, long toUptime, String payloadType) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public FramePart getFramePart(int id, int reset, long uptime, String layout, boolean prev) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getWODRadData(int sAMPLES, int foxId, int sTART_RESET, long sTART_UPTIME, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getHerciHsData(int period, int id, int fromReset, long fromUptime, boolean reverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FramePart getFramePart(int id, int reset, long uptime, int type, String layout, boolean prev) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean reverse, String layout) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean returnType,
			boolean reverse, String layout) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int checkForNewReset(int id, long uptime, Date stpDate, int resetOnFrame, String groundStation) {
		SatPayloadDbStore store = getPayloadStoreById(id);
		if (store != null)
			return store.checkForNewReset(id, uptime, stpDate, resetOnFrame, groundStation);
		return -1;
	}

	@Override
	public int getQueuedFramesSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SortedFramePartArrayList getFrameParts(int id, int fromReset, long fromUptime, int period, boolean reverse,
			String layout) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNumberOfMeasurements(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumberOfPassMeasurements(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	



}
