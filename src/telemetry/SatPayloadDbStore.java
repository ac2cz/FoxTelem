package telemetry;


import gui.MainWindow;
import telemServer.ServerConfig;
import telemetry.uw.CanPacket;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

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
public class SatPayloadDbStore {

	// MySQL error codes
	public static final String ERR_TABLE_DOES_NOT_EXIST = "42S02";
	public static final String ERR_DUPLICATE = "23000";
	public static final String ERR_OPEN_RESULT_SET = "X0X95";
	
	public int foxId;
	public FoxSpacecraft fox;
	
	public static String RT_LOG = "RTTELEMETRY";
	public static String MAX_LOG = "MAXTELEMETRY";
	public static String MIN_LOG = "MINTELEMETRY";
	public static String RAD_LOG = "RADTELEMETRY";
	public static String RAD_TELEM_LOG = "RAD2TELEMETRY";
	public static String HERCI_HS_LOG = "HERCI_HS";
	public static String HERCI_HS_HEADER_LOG = "HERCI_HS_HEADER";
	public static String HERCI_HS_PACKET_LOG = "HERCI_HS_PACKET";
	public static String JPG_IDX = "JPG_IDX";
	public static String PICTURE_LINES = "PICTURE_LINES_IDX";
	public static String WOD_LOG = "WODTELEMETRY";
	public static String WOD_RAD_LOG = "WODRADTELEMETRY";
	public static String WOD_RAD_TELEM_LOG = "WODRAD2TELEMETRY";
	public static String UW_CAN_PACKET_LOG = "UW_CAN_PACKET";
	public static String UW_CAN_PACKET_TIMESTAMP = "UW_CAN_PACKET_TIMESTAMP";
	public static String CAN_LOG = "CANTELEMETRY";
	public static String WOD_CAN_LOG = "WODCANTELEMETRY";
	
	public String rtTableName;
	public String maxTableName;
	public String minTableName;
	public String radTableName;
	public String radTelemTableName;
	public String herciHSTableName;
	public String herciHSHeaderTableName;
	public String herciHSPacketTableName;
	public String jpgIdxTableName;
	public String pictureLinesTableName;
	public String wodTableName;
	public String wodRadTableName;
	public String wodRadTelemTableName;
	public String uwCanPacketTableName;
	public String uwCanPacketTimestampTableName;
	
	boolean updatedRt = true;
	boolean updatedMax = true;
	boolean updatedMin = true;
	boolean updatedRad = true;
	boolean updatedRadTelem = true;
	boolean updatedHerciHS = true;
	boolean updatedHerciHeader = true;
	boolean updatedHerciPacket = true;
	boolean updatedCamera = true;
	boolean updatedWod = true;
	boolean updatedWodRad = true;
	boolean updatedWodRadTelem = true;
	boolean updatedUwCanPacket = true;
	
	PayloadDbStore payloadDbStore;
	
	/**
	 * Create the payload store this this fox id
	 * @param id
	 */
	public SatPayloadDbStore(PayloadDbStore store, FoxSpacecraft fox) {
		this.fox = fox;
		payloadDbStore = store;
		foxId = fox.foxId;
		rtTableName = "Fox"+foxId+RT_LOG;
		maxTableName = "Fox"+foxId+MAX_LOG;
		minTableName = "Fox"+foxId+MIN_LOG;
		radTableName = "Fox"+foxId+RAD_LOG;
		radTelemTableName = "Fox"+foxId+RAD_TELEM_LOG;
		herciHSTableName = "Fox"+foxId+HERCI_HS_LOG;
		herciHSHeaderTableName = "Fox"+foxId+HERCI_HS_HEADER_LOG;
		herciHSPacketTableName = "Fox"+foxId+HERCI_HS_PACKET_LOG;
		jpgIdxTableName = "Fox"+foxId+JPG_IDX;
		pictureLinesTableName = "Fox"+foxId+PICTURE_LINES;
		wodTableName = "Fox"+foxId+WOD_LOG;
		wodRadTableName = "Fox"+foxId+WOD_RAD_LOG;
		wodRadTelemTableName = "Fox"+foxId+WOD_RAD_TELEM_LOG;
		uwCanPacketTableName = "Fox"+foxId+UW_CAN_PACKET_LOG;
		uwCanPacketTimestampTableName = "Fox"+foxId+UW_CAN_PACKET_TIMESTAMP;
		initPayloadFiles();
	}
	
	private String makeTableName(String layoutName) {
		String s = "Fox"+foxId;
		String LAY = layoutName.toUpperCase();
		s = s + LAY;
		return s;
	}
	
	private void initPayloadFiles() {
		boolean storeMode = false;
		if (fox.hasModeInHeader)
			storeMode = true;

		// This would create all of the tables, but not backwards compatible
//		for (int i=0; i<fox.numberOfDbLayouts; i++)
//			initPayloadTable(fox.layout[i].name, fox.hasModeInHeader);
		initPayloadTable(Spacecraft.REAL_TIME_LAYOUT, storeMode);
		initPayloadTable(rtTableName, fox.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT), storeMode);
		initPayloadTable(maxTableName, fox.getLayoutByName(Spacecraft.MAX_LAYOUT), storeMode);
		initPayloadTable(minTableName, fox.getLayoutByName(Spacecraft.MIN_LAYOUT), storeMode);
		if (fox.getLayoutIdxByName(Spacecraft.CAN_LAYOUT) != Spacecraft.ERROR_IDX) {
			radTableName = "Fox"+foxId+CAN_LOG;
			initPayloadTable(radTableName, fox.getLayoutByName(Spacecraft.CAN_LAYOUT), storeMode);
			initCanPacketTable(storeMode);
			initCanTimestampTable();
		} else {
			initPayloadTable(radTableName, fox.getLayoutByName(Spacecraft.RAD_LAYOUT), storeMode);
			initPayloadTable(radTelemTableName, fox.getLayoutByName(Spacecraft.RAD2_LAYOUT), storeMode);
		}
		if (fox.hasHerci()) {
			initHerciTables(storeMode);
		}
		if (fox.hasCamera()) {
			initCameraTables();
		}
		if (fox.getLayoutIdxByName(Spacecraft.WOD_LAYOUT) != Spacecraft.ERROR_IDX) {
			initPayloadTable(wodTableName, fox.getLayoutByName(Spacecraft.WOD_LAYOUT), storeMode);
		}
		if (fox.getLayoutIdxByName(Spacecraft.WOD_RAD_LAYOUT) != Spacecraft.ERROR_IDX) {
			initPayloadTable(wodRadTableName, fox.getLayoutByName(Spacecraft.WOD_RAD_LAYOUT), storeMode);
			initPayloadTable(wodRadTelemTableName, fox.getLayoutByName(Spacecraft.WOD_RAD2_LAYOUT), storeMode);
		}
		if (fox.getLayoutIdxByName(Spacecraft.WOD_CAN_LAYOUT) != Spacecraft.ERROR_IDX) {
			wodRadTableName = "Fox"+foxId+WOD_CAN_LOG;
			initPayloadTable(wodRadTableName, fox.getLayoutByName(Spacecraft.WOD_CAN_LAYOUT), storeMode);
		}
	}

	/** 
	 *  create the tables if they do not exist
	 */
	private void initPayloadTable(String layoutName, boolean storeMode) {
		initPayloadTable(makeTableName(layoutName), fox.getLayoutByName(layoutName), storeMode);
	}
	private void initPayloadTable(String table, BitArrayLayout layout, boolean storeMode) {
		if (layout == null) return; // we don't need this table if there is no layout
		String createStmt = layout.getTableCreateStmt(storeMode);
		createTable(table, createStmt);
	}

	private void initHerciTables(boolean storeMode) {
		initPayloadTable(herciHSTableName, fox.getLayoutByName(Spacecraft.HERCI_HS_LAYOUT), storeMode);
		initPayloadTable(herciHSHeaderTableName, fox.getLayoutByName(Spacecraft.HERCI_HS_HEADER_LAYOUT), storeMode);
		String table = herciHSPacketTableName;
		String createStmt = HerciHighSpeedPacket.getTableCreateStmt();
		createTable(table, createStmt);
	}
	
	private void initCameraTables() {
		String table = jpgIdxTableName;
		String createStmt = CameraJpeg.getTableCreateStmt();
		createTable(table, createStmt);
		table = pictureLinesTableName;
		createStmt = PictureScanLine.getTableCreateStmt();
		createTable(table, createStmt);
	}
	
	private void initCanPacketTable(boolean storeMode) {
		String table = uwCanPacketTableName;
		BitArrayLayout lay = fox.getLayoutByName(Spacecraft.CAN_PKT_LAYOUT);
		String s = new String();
		s = s + "(captureDate varchar(14), id int, resets int, uptime bigint, type int, ";
		if (storeMode)
			s = s + "newMode int,";
		for (int i=0; i < lay.fieldName.length; i++) {
			s = s + lay.fieldName[i] + " int,\n";
		}
		// We use serial for the type, except for type 4 where we use it for the payload number.  This allows us to have
		// multiple high speed records with the same reset and uptime
		s = s + "pkt_id int(11) NOT NULL AUTO_INCREMENT,";
		s = s + "PRIMARY KEY (pkt_id),";
		s = s + "UNIQUE KEY (id, resets, uptime, type)" + ")";
		createTable(table, s);
	}
	
	private void initCanTimestampTable() {
		String table = uwCanPacketTimestampTableName;
		String s = new String();
		s = s + "(username varchar(255) not null,"
				+ "last_pkt_id int(11) NOT NULL,";
				s = s + "PRIMARY KEY (username))";
		createTable(table, s);
	}
	
	private void createTable(String table, String createStmt) {
		Statement stmt = null;
		ResultSet select = null;
		try {
			payloadDbStore.getConnection();
			stmt = payloadDbStore.derby.createStatement();
			select = stmt.executeQuery("select 1 from " + table + " LIMIT 1");
		} catch (SQLException e) {
			
			if ( e.getSQLState().equals(SatPayloadDbStore.ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				String createString = "CREATE TABLE " + table + " ";
				createString = createString + createStmt;
				
				Log.println ("Creating new DB table " + table);
				Log.println("***************************************\n"+createString+"***************************************\n");

				try {
					stmt.execute(createString);
				} catch (SQLException ex) {
					PayloadDbStore.errorPrint("createTable:"+table, ex);
				}
			} else {
				PayloadDbStore.errorPrint("createTable:"+table,e);
			}
		} finally {
			try { if (select != null) select.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
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
	public boolean getUpdatedCamera() { return updatedCamera; }
	public void setUpdatedCamera(boolean u) {
		updatedCamera = u;
	}

	@SuppressWarnings("unused")
	private int count(String table) {
		int count = 0;
		Statement stmt = null;
		ResultSet rs = null;
		String update = "select count(*) from " + table;
		//Log.println("SQL:" + update);
		try {
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement();
			rs = stmt.executeQuery(update);
			if (!rs.isClosed() && rs.next());
			count = rs.getInt(1);
		
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				// ignore. We are probablly starting up or deleting the tables
			} else
				PayloadDbStore.errorPrint("count:"+table, e);
			return 0;
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		} 
		return count;

	}

	public int getNumberOfFrames() { return count(rtTableName) + count(maxTableName) + count(minTableName) + count(radTableName); }
	public int getNumberOfTelemFrames() { return count(rtTableName) + count(maxTableName) + count(minTableName); }
	public int getNumberOfRadFrames() { return count(radTableName);}
	public int getNumberOfPictureCounters() { return count(jpgIdxTableName);}
		
	public boolean add(int id, long uptime, int resets, FramePart f) throws IOException {
		f.captureHeaderInfo(id, uptime, resets);
		return add(f);
	}

	private boolean addRadRecord(PayloadRadExpData f)  {
		if (insert(radTableName,f)) {

			// Capture and store any secondary payloads
			if (fox.hasHerci() || f.isTelemetry()) {
				RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
				radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
				if (f.type >= 400) // this is a high speed record
					radiationTelemetry.type = f.type + 300; // we give the telem record 700+ type
				return add(radiationTelemetry);
			}
		} else {
			return false;
		}
		return true;
	}

	private boolean addWodRadRecord(PayloadWODRad f)  {
		if (insert(wodRadTableName,f)) {
			WodRadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
			radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
			return add(radiationTelemetry);
		} else {
			return false;
		}
	}

	/**
	 * Add a HERCI High Speed payload record
	 * @param f
	 * @return
	 * @throws IOException
	 */
	private boolean addHerciRecord(PayloadHERCIhighSpeed f) {
		if (insert(herciHSTableName, f)) {

			// Capture and store any secondary payloads
			HerciHighspeedHeader radiationTelemetry = f.calculateTelemetryPalyoad();
			radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
			if (f.type >= 600) // this is a high speed record
				radiationTelemetry.type = f.type + 200; // we give the telem record 800+ type
			if (add(radiationTelemetry)) {
				updatedHerciHeader = true;

				ArrayList<HerciHighSpeedPacket> pkts = f.calculateTelemetryPackets();
				for(int i=0; i< pkts.size(); i++) {
					HerciHighSpeedPacket pk = pkts.get(i);
					pk.captureHeaderInfo(f.id, f.uptime, f.resets);
					if (f.type >= 600) // this is a high speed record
						pk.type = f.type*1000 + 900 + i;  // This will give a type formatted like 601903
					add(pk);
					updatedHerciPacket = true;
				}
			} else {
				return false;
			}
		} else {
			return false;
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
					f[i].type = i+400;  // use type as the serial number for high speed type 4s, starting at 400 to distinguish from a Low Speed type 4
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
		String insertStmt = f.getInsertStmt();
		return insertData(table, insertStmt);
	}
	
	private boolean insert(String table, CameraJpeg f) {
		String insertStmt = f.getInsertStmt();
		return insertData(table, insertStmt);
	}
	
	@SuppressWarnings("unused")
	private boolean insert(String table, PictureScanLine f) {
		String insertStmt = f.getInsertStmt();
		return insertData(table, insertStmt);
	}
	
	/**
	 * This inserts data into the specified table using the specified insert statement
	 * If the SQL insert fails then it returns false, unless if fails as a duplicate.  Then
	 * true is returned.
	 * 
	 * @param table
	 * @param insertStmt
	 * @return
	 */
	private boolean insertData(String table, String insertStmt) {
		Statement stmt = null;
		String update = "insert into " + table;
		update = update + insertStmt;
		//Log.println("SQL:" + update);
		try {
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement();
			@SuppressWarnings("unused")
			int r = stmt.executeUpdate(update);
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
				//Log.println("DUPLICATE RECORD, not stored");
				return true; // We have the data
			} else {
				PayloadDbStore.errorPrint("insertData:"+table, e);
			}
			return false;
		} finally {
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return true;

	}
	
	private boolean insertImageLine(String table, PictureScanLine f) throws SQLException {
		String insertStmt = f.getInsertStmt();
		boolean inserted = insertData(table, insertStmt);
		// FIXME - this returns true unless there is an error.  So even with a duplicate we copy all the bytes in, which is wastefull!
		// We need to fix the logic.  true/false should be based in actual insert of the data.  Throw error for failure and let that
		// be caught and stopped at the point where we deal with it and ALERT
		java.sql.PreparedStatement ps = null;
		if (!inserted)
			return false;
		else // we inserted the image line, so now we add the actual image data to the data blob
		try {
			Connection derby = payloadDbStore.getConnection();
			ps = derby.prepareStatement("UPDATE "+ table + " set imageBytes = ?"
					+ " where id = " + f.id
					+ " and resets = " + f.resets
					+ " and uptime = " + f.uptime
					+ " and pictureCounter = " + f.pictureCounter
					+ " and scanLineNumber = " + f.scanLineNumber);
			ps.setBytes(1, f.getBytes());
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
				Log.println("ERROR, image bytes not stored");
				return false; // We have the data
			} else {
				throw e;
			}
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e2) {};
		}
		return true;
	}
	
	/*
	private boolean insertHerciPacket(String table, HerciHighSpeedPacket f) {
		String insertStmt = f.getInsertStmt();
		boolean inserted = insertData(table, insertStmt);
		// FIXME - this returns true unless there is an error.  So even with a duplicate we copy all the bytes in, which is wastefull!
		if (!inserted)
			return false;
		else
		try {
			Connection derby = PayloadDbStore.getConnection();
			java.sql.PreparedStatement ps = derby.prepareStatement("UPDATE "+ table + " set minipacket = ?"
					+ " where id = " + f.id
					+ " and resets = " + f.resets
					+ " and uptime = " + f.uptime
					+ " and type = " + f.type);
			ps.setBytes(1, f.getMiniPacketBytes());
			int count = ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
				Log.println("ERROR, image bytes not stored");
				return false; // We have the data
			} else {
				PayloadDbStore.errorPrint(e);
			}
		}
		return true;
	}
	*/
	/**
	 * Add the frame to the correct array and file
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	private boolean add(FramePart f) {
		if (f instanceof PayloadWOD ) {
			return insert(wodTableName, (PayloadWOD)f);
		} else if (f instanceof PayloadRtValues ) {
			setUpdatedRt(true);
			return insert(rtTableName,f);
		} else if (f instanceof PayloadMaxValues  ) {
			setUpdatedMax(true);
			return insert(maxTableName,f);
		} else if (f instanceof PayloadMinValues ) {
			setUpdatedMin(true);
			return insert(minTableName,f);
		} else if (f instanceof PayloadWODRad ) {
			return addWodRadRecord((PayloadWODRad)f);			
		} else if (f instanceof PayloadRadExpData ) {
			setUpdatedRad(true);
			return addRadRecord((PayloadRadExpData)f);
		} else if (f instanceof WodRadiationTelemetry ) {
			return insert(wodRadTelemTableName, f);
		} else if (f instanceof RadiationTelemetry ) {
			setUpdatedRadTelem(true);
			return insert(radTelemTableName, f);
		} else if (f instanceof PayloadHERCIhighSpeed ) {
			return addHerciRecord((PayloadHERCIhighSpeed)f); 
		} else if (f instanceof HerciHighspeedHeader ) {
			return insert(herciHSHeaderTableName, f);
		} else if (f instanceof HerciHighSpeedPacket ) {
			return insert(herciHSPacketTableName, (HerciHighSpeedPacket)f);
		} else if (f instanceof PayloadWODUwExperiment ) {
			updatedWodRad=true;
			return insert(wodRadTableName, (PayloadWODUwExperiment)f);
		} else if (f instanceof PayloadUwExperiment ) {
			updatedRad=true;
			return insert(radTableName, (PayloadUwExperiment)f);
		} else if (f instanceof CanPacket ) {
			return insert(uwCanPacketTableName, (CanPacket)f);
		}
		return false;
	}

	/**
	 * This is called from the server image thread and will process any new Image lines that have been received since it
	 * last ran.  New jpeg images are written to disk and existing jpegs are updated.
	 * 
	 * Note that image lines are in sets of three, but we process each line individually.  This is inefficient because
	 * we effectively write the file three times over.  But this happens every 5 seconds at most, so the overhead is low
	 * If time permits this can be optimized to write the file once for each set of three lines.
	 * 
	 * @return true if we added image lines to a jpeg
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public boolean processNewImageLines() throws SQLException, IOException {
		if (!fox.hasCamera()) {
			//Log.println("Nothing to process for " + fox.getIdString());
			return false; // nothing to process
		}
		
		//Log.println("Checking for lines on: " + fox.getIdString());
		// Get a list of new unprocessed lines
		String lineswhere = " where id = " + this.foxId
				+ " and processed = 0";
					
		SortedArrayList<PictureScanLine> psl = selectImageLines(pictureLinesTableName, lineswhere);
		if (psl == null) return false;
		for (PictureScanLine p : psl) {
			Log.println("Processing new image line " + p.scanLineNumber + " for FoxId: " 
					+ this.foxId + " r:" + p.resets +" u:" + p.uptime + " pc:" + p.pictureCounter);
			String where = " where id = " + this.foxId
					+ " and resets = " + p.resets
					+ " and uptime = " + p.uptime
					+ " and pictureCounter = " + p.pictureCounter;
			CameraJpeg jpg = selectExistingJpeg(jpgIdxTableName, this.foxId, p.resets, p.uptime, p.pictureCounter);
			
			if (jpg == null) {
				// We don't have an existing JPEG.
				// Select all three lines for this uptime and add to a new Jpeg
				SortedArrayList<PictureScanLine> jpgPsl = selectImageLines(pictureLinesTableName, where);
				if (jpgPsl == null) return false;
				jpg = new CameraJpeg(this.foxId, p.resets, p.uptime, p.uptime, p.pictureCounter, jpgPsl);
				insert(jpgIdxTableName, jpg); // we add this.  If its a duplicate, we ignore and keep going.  The line still needs to be added
			}
			jpg.writeAllLines();  // this triggers us to write it to the file on disk
			updatedCamera = true;
			// Now mark this line as processed
			java.sql.PreparedStatement ps = null;
			try {
			Connection derby = payloadDbStore.getConnection();
			ps = derby.prepareStatement("UPDATE "+ pictureLinesTableName 
					+ " set processed = ? "
					+ " where id = " + this.foxId
					+ " and resets = " + p.resets
					+ " and uptime = " + p.uptime
					+ " and pictureCounter = " + p.pictureCounter
					+ " and scanLineNumber = " + p.scanLineNumber);
			ps.setLong(1, 1);
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
			
			} finally {
				if (ps != null) ps.close();	
			}
			
		}
		/*
		if (added) {
			// This was a new line, so we want to see if this is a new JPEG.  Either way we read all the latest lines 
			// when we instantiate the CameraJpeg
			
			return true;
		}
		*/
		return true;
	}
	
	/**
	 * Add a camera payload to the server database.  We must write an index entry to the camera lines table,
	 * write the camera lines to a file if this is a unique entry (not a dupe). We then check if this is a new
	 * picture and if so, add an entry to the jpeg index. The jpeg is then generated or updated and the latest
	 * jpeg and thumbnail is written to disk.
	 * 
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param line
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public boolean add(PictureScanLine line) throws IOException, SQLException {
		return insertImageLine(pictureLinesTableName, line);
	}
	
	/**
	 * Query the database to see if we have a Jpeg with the same id, reset and pictureCounter, where
	 * the uptime is close to the uptime of this line.  Then we can conclude that the line belongs in this
	 * picture.  Otherwise we return an empty set
	 * @param table
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param pictureCounter
	 * @return
	 */
	private CameraJpeg selectExistingJpeg(String table, int id, int resets, long uptime, int pictureCounter) {
		String where = " where id = " + id
				+ " and resets = " + resets
				+ " and ABS(" + uptime + " - fromUptime ) < " + CameraJpeg.UPTIME_THRESHOLD
				+ " and pictureCounter = " + pictureCounter;
		Statement stmt = null;
		String update = "";
		
		ResultSet r = null;
		
		update = " SELECT id, resets, fromUptime, toUptime, pictureCounter, fileName FROM ";
		//DERBY SYNTAX - update = update + table + where + " FETCH NEXT " + numberOfRows + " ROWS ONLY";
		update = update + table + where ;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			r = stmt.executeQuery(update);
			
			if (r.next()) {
				String lineswhere = " where id = " + id
						+ " and resets = " + resets
						+ " and ABS(" + uptime + " - uptime ) < " + CameraJpeg.UPTIME_THRESHOLD
						+ " and pictureCounter = " + pictureCounter;				
				SortedArrayList<PictureScanLine> psl = selectImageLines(pictureLinesTableName, lineswhere);
				if (psl == null) return null;
				boolean runUpdate = false;
				// we have an existing record, so load it
				int rsid = r.getInt("id");
				int rsresets = r.getInt("resets");
				long fromUptime = r.getInt("fromUptime");
				long toUptime = r.getInt("toUptime");
				@SuppressWarnings("unused")
				String fileName = r.getString("fileName");
				int rspictureCounter = r.getInt("pictureCounter");
				long newFromUptime = fromUptime;
				long newToUptime = toUptime;
				if (toUptime < uptime) {
					// then we have a line that is later than the index records toUptime, so we need to update the database and return that record
					runUpdate = true;
					newToUptime = uptime;
				}
				// Note that we do not update the "fromUptime".  We used the first uptime that we got
				if (runUpdate) {
					java.sql.PreparedStatement ps = null;
					try {
						ps = derby.prepareStatement("UPDATE "+ table 
								+ " set fromUptime = ?, toUptime = ? "
								+ " where id = " + rsid
								+ " and resets = " + rsresets
								+ " and fromUptime = " + fromUptime
								+ " and toUptime = " + toUptime
								+ " and pictureCounter = " + rspictureCounter);
						ps.setLong(1, newFromUptime);
						ps.setLong(2, newToUptime);
						@SuppressWarnings("unused")
						int count = ps.executeUpdate();
					} catch (SQLException e) {
						if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
							Log.println("ERROR, image bytes not stored");
							return null; // We have the data
						} else {
							PayloadDbStore.errorPrint("selectExistingJpeg", e);
						}
						return null;
					} finally {
						try { if (ps != null) ps.close(); } catch (SQLException e2) {};
					}
				}
				CameraJpeg j = new CameraJpeg(rsid, rsresets, newFromUptime, newToUptime, rspictureCounter, psl);
				return j;
			} else {
				return null;
			}
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectExistingJpeg", e);
		} finally {
			try { if (r != null) r.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};	
		}
		return null;
	}
	
	ArrayList<FramePart> selectCanPackets(String where) {
		Statement stmt = null;
		String update = "  SELECT * FROM "; // Derby Syntax FETCH FIRST ROW ONLY";
		update = update + uwCanPacketTableName + " " + where + " ORDER BY pkt_id limit 1000";
		ResultSet r = null;
		ArrayList<FramePart> frameParts = new ArrayList<FramePart>(60);

		try {
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement();
			//Log.println(update);
			r = stmt.executeQuery(update);
			if (r != null) {
				while (r.next()) {
					CanPacket payload = new CanPacket(Config.satManager.getLayoutByName(foxId, Spacecraft.CAN_PKT_LAYOUT));
					payload.id = r.getInt("id");
					payload.resets = r.getInt("resets");
					payload.uptime = r.getLong("uptime");
					payload.type = r.getInt("type");
					payload.reportDate = r.getString("captureDate");
					payload.pkt_id = r.getInt("pkt_id");
					payload.init();
					payload.rawBits = null; // no binary array when loaded from database
					for (int i=0; i < payload.fieldValue.length; i++) {
						payload.fieldValue[i] = r.getInt(payload.layout.fieldName[i]);
					}
					frameParts.add(payload);
				}
			} else {
				frameParts = null;
			}
			return frameParts;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectLatest:"+uwCanPacketTableName, e);
			try { r.close(); stmt.close();	} catch (SQLException e1) { e1.printStackTrace(); }
		} finally {
			try { if (r != null) r.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return null;
	}
	
	
	private SortedArrayList<PictureScanLine> selectImageLines(String table, String where) {
		Statement stmt = null;
		String update = "";
		SortedArrayList<PictureScanLine> pictureLines = new SortedArrayList<PictureScanLine>(60);
		
		update = " SELECT id, resets, uptime, date_time, pictureCounter, scanLineNumber, scanLineLength, imageBytes FROM ";
		//DERBY SYNTAX - update = update + table + where + " FETCH NEXT " + numberOfRows + " ROWS ONLY";
		update = update + table + where + " order by scanLineNumber";
		ResultSet r = null;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			r = stmt.executeQuery(update);
			
			if (r != null) {
				while (r.next()) {
					int lineId = r.getInt("id");
					int lineResets = r.getInt("resets");
					int lineUptime = r.getInt("uptime");
					int linePc = r.getInt("pictureCounter");
					int lineNum = r.getInt("scanLineNumber");
					int lineLineLen = r.getInt("scanLineLength");
					java.sql.Blob blob = r.getBlob("imageBytes");
					if (blob == null) {
						Log.println("ERROR: Tried to create JPEG but no data bytes available");
						return null;
					}
					int len = (int)blob.length();
					byte[] blobAsBytes = blob.getBytes(1, len);
					// we give all the lines the fromUptime so that they are sorted in order
					PictureScanLine psl = new PictureScanLine(lineId, lineResets, lineUptime, "", linePc,lineNum,lineLineLen, blobAsBytes );
					pictureLines.add(psl);
					blob.free();
				}
			}
			return pictureLines;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectImageLines:"+table, e);
			
		} finally {
			try { if (r != null) r.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return null;
	}
	
	private void selectLatest(String table, FoxFramePart payload) {
		Statement stmt = null;
		String update = "  SELECT * FROM " + table + " ORDER BY"; // Derby Syntax FETCH FIRST ROW ONLY";
		if (payload instanceof CanPacket)
			update = update + " pkt_id DESC LIMIT 1"; 
		else
			update = update + " resets DESC, uptime DESC, type DESC LIMIT 1"; 
		ResultSet r = null;

		try {
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement();
			//Log.println(update);
			r = stmt.executeQuery(update);
			if (r.next()) {
				payload.id = r.getInt("id");
				payload.resets = r.getInt("resets");
				payload.uptime = r.getLong("uptime");
				payload.type = r.getInt("type");
				if (fox.hasModeInHeader)
					payload.newMode = r.getInt("newMode");
				payload.reportDate = r.getString("captureDate");
				if (payload instanceof CanPacket)
					((CanPacket)payload).pkt_id = r.getInt("pkt_id");
				payload.init();
				payload.rawBits = null; // no binary array when loaded from database
				for (int i=0; i < payload.fieldValue.length; i++) {
					payload.fieldValue[i] = r.getInt(payload.layout.fieldName[i]);
				}
				return;
			} else {
				payload = null;
			}
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectLatest:"+table, e);
			try { r.close(); stmt.close();	} catch (SQLException e1) { e1.printStackTrace(); }
		} finally {
			try { if (r != null) r.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		payload = null;
	}
	
//	public FramePart getLatest(String layout) throws SQLException {
//		BitArrayLayout lay = fox.getLayoutByName(layout);
//		String tableName = lay.getTableName();
//		FoxFramePart payload = FramePart.makePayload(lay);
//		selectLatest(tableName, payload);
//		return payload;
//	}
	
	public CanPacket getLatestUwCanPacket() throws SQLException {
		CanPacket payload = new CanPacket(fox.getLayoutByName(Spacecraft.CAN_PKT_LAYOUT));
		selectLatest(uwCanPacketTableName, payload);
		return payload;
	}
	
	public PayloadRtValues getLatestRt() throws SQLException {
		PayloadRtValues payload = new PayloadRtValues(fox.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT));
		selectLatest(rtTableName, payload);
		return payload;
	}

	public PayloadMaxValues getLatestMax() throws SQLException {
		PayloadMaxValues max = new PayloadMaxValues(fox.getLayoutByName(Spacecraft.MAX_LAYOUT));
		selectLatest(maxTableName, max);
		return max;
	}

	public PayloadMinValues getLatestMin() throws SQLException {
		PayloadMinValues min = new PayloadMinValues(fox.getLayoutByName(Spacecraft.MIN_LAYOUT));
		selectLatest(minTableName, min);
		return min;
	}

	public PayloadRadExpData getLatestRad() throws SQLException {
		PayloadRadExpData rad = new PayloadRadExpData(fox.getLayoutByName(Spacecraft.RAD_LAYOUT));
		selectLatest(radTableName, rad);
		return rad;
	}

	public PayloadUwExperiment getLatestUwExp() throws SQLException {
		PayloadUwExperiment rad = new PayloadUwExperiment(fox.getLayoutByName(Spacecraft.CAN_LAYOUT), 0, 0, 0);
		selectLatest(radTableName, rad);
		return rad;
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
	public double[][] getRtGraphData(String name, int period, Spacecraft fox2, int fromReset, long fromUptime) throws SQLException {
		return getGraphData(rtTableName, name, period, fox2, fromReset, fromUptime);
		
	}

	public double[][] getMaxGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws SQLException {
		return getGraphData(maxTableName, name, period, id, fromReset, fromUptime);
		
	}

	public double[][] getMinGraphData(String name, int period, Spacecraft id, int fromReset, long fromUptime) throws SQLException {
		return getGraphData(minTableName, name, period, id, fromReset, fromUptime);
		
	}

	public void initRad2() {
		ResultSet rs = null;
		String where = "select * from " + this.radTableName;
		Statement stmt = null;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(where);
			
			while (rs.next()) {
				PayloadRadExpData f = new PayloadRadExpData(rs, fox.getLayoutByName(Spacecraft.RAD_LAYOUT));
				// Capture and store any secondary payloads
				if (fox.hasHerci() || f.isTelemetry()) {
					RadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
					radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
					add(radiationTelemetry);
				}
				
			}

		} catch (SQLException e) {
			PayloadDbStore.errorPrint("initRad2", e);
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
	}

	public void initHerciPackets() {
		int k = 0;
		int p = 0;
		ResultSet rs = null;
		String where = "select * from " + this.herciHSTableName;
		Statement stmt = null;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(where);
			
			while (rs.next()) {
				PayloadHERCIhighSpeed f = new PayloadHERCIhighSpeed(rs, fox.getLayoutByName(Spacecraft.HERCI_HS_LAYOUT));
				int type = rs.getInt(5); // Column 5 is the Type.  We need to get this as we increment them 601, 602, 603 for the identical reset/uptime
				k++;
				ArrayList<HerciHighSpeedPacket> pkts = f.calculateTelemetryPackets();
				for(int i=0; i< pkts.size(); i++) {
					HerciHighSpeedPacket pk = pkts.get(i);
					pk.captureHeaderInfo(f.id, f.uptime, f.resets);
					pk.type = type*1000 + 900 + i;  // This will give a type formatted like 601903
					add(pk);
					p++;
					updatedHerciPacket = true;
				}
				
			}
			Log.println("Processed " + k + " HERCI HS FRAMES generating " + p + " mini packets");

		} catch (SQLException e) {
			PayloadDbStore.errorPrint("initRad2", e);
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
	}
	
	public boolean storeLastCanId(String user, int pkt_id) {
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		try {
			Connection derby = payloadDbStore.getConnection();
			ps = derby.prepareStatement(
				      "UPDATE "+ uwCanPacketTimestampTableName +" SET last_pkt_id = ? where username = ?");

			// set the preparedstatement parameters
		    ps.setInt(1,pkt_id);
		    ps.setString(2,user);
		    
		    int rows = ps.executeUpdate();
			if (rows > 0) {
				return true;
			} else {
				Log.println("Could not update the CAN pkt id for " + user + " trying INSERT");
				ps2 = derby.prepareStatement(
					      "INSERT INTO "+ uwCanPacketTimestampTableName +" (username, last_pkt_id) VALUES (?, ?)");

				// set the preparedstatement parameters
			    ps2.setString(1,user);
			    ps2.setInt(2,pkt_id);
			    
			    rows = ps2.executeUpdate();
			    if (rows > 0) {
					return true;
				} else {
					Log.println("ERROR: Could not insert the CAN pkt id "+pkt_id+" for " + user);
					return false; 
				}
			}
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("ERROR Check Password SQL:", e);
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e2) {};
			try { if (ps2 != null) ps2.close(); } catch (SQLException e2) {};
		}
		return false;
	}
	
	public int getLastCanId(String user) {
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pktId = 0;
		try {
			Connection derby = payloadDbStore.getConnection();
			ps = derby.prepareStatement("SELECT last_pkt_id from "+ uwCanPacketTimestampTableName +" where username = ?");
		    ps.setString(1,user);
		    
		    rs = ps.executeQuery();
			
		    while (rs.next()) {
		    	pktId = rs.getInt("last_pkt_id");	
			}
			return pktId;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("ERROR Check Password SQL:", e);
			try { rs.close();	} catch (SQLException e1) { e1.printStackTrace(); }
			try { ps.close();	} catch (SQLException e1) { e1.printStackTrace(); }
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (ps != null) ps.close(); } catch (SQLException e2) {};
		}
		return 0;
	}
	
	/**
	 * Given the current reset (likely always 1) and the uptime, is this a new reset?  If so, update the reset 
	 * table with estimated T0 and return the new reset number. Block with a transaction during this whole process.
	 * 
	 * @param uptime
	 * @param stepDate
	 * @return
	 */
	public int checkForNewReset(int id, long uptime, Date stpDate, int resetOnFrame) {
		PreparedStatement ps = null, ps1 = null, ps2 = null;
		ResultSet rs = null, rs1 = null;
		Timestamp T0 = null;
		int currentReset = 0;
		
		long stpSeconds = stpDate.getTime()/1000;
		
		Connection conn = null;
		try {
			conn = payloadDbStore.getConnection();
			conn.setAutoCommit(false);
			
			// first get the current reset
			ps = conn.prepareStatement("SELECT max(resets) as reset from T0_LOG where id = ?");
		    ps.setInt(1,id);
		    
		    rs = ps.executeQuery();
			
		    while (rs.next()) {
		    	currentReset = rs.getInt("reset");	
			}
			
		    if (resetOnFrame - currentReset == 1) { // this may never happen, or we might jump a few resets
		    	// Then we had a normal reset, write it in the log
	    		Log.println("*** HUSKY NORMAL RESET DETECTED ....");
	    		Log.println("*** stpDate: " + stpDate + " " + stpSeconds);
	    		Log.println("*** uptime: " + uptime);
	    		currentReset = resetOnFrame;
	    		// We have a reset, so insert a new T0 row
	    		ps2 = conn.prepareStatement(
	    				"INSERT INTO T0_LOG (id, resets, T0_estimate_date_time) VALUES (?, ?, ?)");

	    		// set the preparedstatement parameters
	    		ps2.setInt(1,id);
	    		ps2.setInt(2,currentReset);
	    		Timestamp newT0estimate = new Timestamp((stpSeconds - uptime)*1000);
	    		ps2.setTimestamp(3,newT0estimate);

	    		int rows = ps2.executeUpdate();
	    		if (rows > 0) {
	    			conn.commit();
	    			return currentReset;
	    		} else {
	    			Log.println("ERROR: Could not insert the new reset number" + currentReset);
	    			conn.rollback();
	    			return currentReset; // this was a genuine reset, so we do not want to ignore the data 
	    		}
		    }
		    
			ps1 = conn.prepareStatement("SELECT T0_estimate_date_time from T0_LOG where id = ? and resets = ?");
		    ps1.setInt(1,id);
		    ps1.setInt(2,currentReset);
		    
		    rs1 = ps1.executeQuery();
			
		    while (rs1.next()) {
		    	T0 = rs1.getTimestamp("T0_estimate_date_time");	
			}
		    
		    // We have T0 for the current reset.  Given the uptime that we have, is it valid for this reset and this stpDate
		    long T0Seconds = 0;
		    
		    if (T0 != null) // if there was a row in the table then set T0Seconds
		    	T0Seconds = T0.getTime()/1000;

		    if (T0Seconds == 0 || (uptime < ServerConfig.newResetCheckUptimeMax)) {// after this time assume we already have the reset
		    	long diff = Math.abs(stpSeconds - (T0Seconds + uptime));
		    	if (diff > ServerConfig.newResetCheckThreshold) {
		    		// we have had a reset. 
		    		Log.println("*** HUSKY NON MRAM LOGGED RESET DETECTED ....");
		    		Log.println("*** stpDate: " + stpDate + " " + stpSeconds);
		    		Log.println("*** TO: " + T0 + " " + T0Seconds);
		    		Log.println("*** uptime: " + uptime);
		    		Log.println("*** DIFF: " + diff);
		    		if (T0Seconds != 0) // increment the reset unless this is the first time we insert a row for this genuine reset
		    			currentReset = currentReset + 1;
		    		// We have a reset, so insert a new T0 row
		    		ps2 = conn.prepareStatement(
		    				"INSERT INTO T0_LOG (id, resets, T0_estimate_date_time) VALUES (?, ?, ?)");

		    		// set the preparedstatement parameters
		    		ps2.setInt(1,id);
		    		ps2.setInt(2,currentReset);
		    		Timestamp newT0estimate = new Timestamp((stpSeconds - uptime)*1000);
		    		ps2.setTimestamp(3,newT0estimate);

		    		int rows = ps2.executeUpdate();
		    		if (rows > 0) {
		    			conn.commit();
		    			return currentReset;
		    		} else {
		    			Log.println("ERROR: Could not insert the new reset number" + currentReset);
		    			conn.rollback();
		    			return -1; 
		    		}
		    	}
		    }
		    // we use the current reset and return that
		    conn.commit();
		    return currentReset;

		} catch (SQLException e) {
			PayloadDbStore.errorPrint("ERROR Check SQL:", e);
			
			try { conn.rollback(); rs.close();	} catch (SQLException e1) { e1.printStackTrace(); }
			try { if (rs1 != null) rs1.close(); } catch (SQLException e2) {};
			try { if (ps != null) ps.close();	} catch (SQLException e1) { e1.printStackTrace(); }
			try { if (ps1 != null) ps1.close();	} catch (SQLException e1) { e1.printStackTrace(); }
			try { if (ps2 != null) ps2.close();	} catch (SQLException e1) { e1.printStackTrace(); }
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (rs1 != null) rs.close(); } catch (SQLException e2) {};
			try { if (ps != null) ps.close(); } catch (SQLException e2) {};
			try { if (ps1 != null) ps1.close(); } catch (SQLException e2) {};
			try { if (ps2 != null) ps2.close(); } catch (SQLException e2) {};
			
			// Make sure we are back out of the transaction
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e2) {};	
		}
		// we should not end up here, something went wrong
		return -1;
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
		String where = "";

		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			where = " ORDER BY resets DESC, uptime DESC ";
		} else {
			where = " where uptime >= "+ fromUptime + " and resets >= " + fromReset +
					" ORDER BY resets DESC, uptime DESC ";
		}
		//rs = selectRows(radTableName,null, where,period);
		Statement stmt = null;
		String update = " SELECT * FROM ";

		update = update + radTableName + where + " LIMIT " + period;
		ResultSet rs = null;
		String[][] resultSet = null;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(update);

			int size =0;
			if (rs != null) {
				rs.beforeFirst();
				rs.last();
				size = rs.getRow();
			}
			resultSet = new String[size][PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE+3];

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
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectRows:"+radTableName, e);
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return resultSet;
	}

	/*
	private ResultSet selectRows(String table, String name, String where, int numberOfRows) {
		Statement stmt = null;
		String update = "";
		if (name == null) {
			update = " SELECT * FROM ";
		} else {
			update = " SELECT resets, uptime, " + name + " FROM ";
		}
		
		update = update + table + where + " LIMIT " + numberOfRows;
		ResultSet r = null;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			r = stmt.executeQuery(update);
			stmt.close();
			return r;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectRows:"+table, e);
			try { r.close(); stmt.close();	} catch (SQLException e1) { e1.printStackTrace(); }
		}
		return null;
		
	}
*/
    
	private double[][] getGraphData(String table, String name, int period, Spacecraft id, int fromReset, long fromUptime) throws SQLException {
		ResultSet rs = null;
		String where = "";
		
		if (fromReset == 0.0 && fromUptime == 0.0) { // then we take records nearest the end
			where = " ORDER BY resets DESC, uptime DESC ";
		} else {
			where = " where uptime >= "+ fromUptime + " and resets >= " + fromReset +
					" ORDER BY resets, uptime ";
		}
		//FIXME - we get all of the columns so that we can populate at Payload record - see below
		//rs = selectRows(table,name, where,period);
		Statement stmt = null;
		String update = " SELECT resets, uptime, " + name + " FROM ";

		update = update + table + where + " LIMIT " + period;
		double[] results = null;
		double[] upTime = null;
		double[] resets = null;
		double[][] resultSet = null;
		int size =0;
		try {
			//Log.println("SQL:" + update);
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(update);
			
			if (rs != null) {
				rs.beforeFirst();
				rs.last();
				size = rs.getRow();
			}
			results = new double[size];
			upTime = new double[size];
			resets = new double[size];

			int i=0;

			if (Config.displayRawValues)
				;//FIXME conversion = 0;
			if (size > 0) {
				resets[i] = rs.getInt("resets");
				upTime[i] = rs.getLong("uptime");
				//FIXME - we need a payload record so that we can access the right conversion.  But this means we need all the columns....bad
				PayloadRtValues rt = new PayloadRtValues(id.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT));
				results[i++] = rt.convertRawValue(name, (int)rs.getDouble(name), rt.getConversionByName(name), id);
				while (rs.previous()) {
					resets[i] = rs.getInt("resets");
					upTime[i] = rs.getLong("uptime");
					//rt = new PayloadRtValues(rs, fox.rtLayout);
					//raw value
					//results[i++] = rs.getDouble(name);
					// converted

					results[i++] = rt.convertRawValue(name, (int)rs.getDouble(name), rt.getConversionByName(name), id);
				}
			} else {
				results = new double[1];
				upTime = new double[1];
				resets = new double[1];
			}
			resultSet = new double[3][size];
			resultSet[PayloadStore.DATA_COL] = results;
			resultSet[PayloadStore.UPTIME_COL] = upTime;
			resultSet[PayloadStore.RESETS_COL] = resets;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("selectRows:"+table, e);


		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		
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
			Connection derby = payloadDbStore.getConnection();
			stmt = derby.createStatement();
			@SuppressWarnings("unused")
			boolean res = stmt.execute("drop table " + table);
			stmt.close();
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_OPEN_RESULT_SET) ) {  // Open Result set
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					;; // do nothing
				}
				try {
					Log.println("RETRYING.....");
					@SuppressWarnings("unused")
					boolean res = stmt.execute("drop table " + table);
				} catch (SQLException e1) {
					Log.println("RETRY FAILED");
					PayloadDbStore.errorPrint("drop:"+table, e);
				}
			}
			if ( e.getSQLState().equals(ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				// then we don't care cause its already gone
			} else {
				PayloadDbStore.errorPrint("drop:"+table,e);
			}
		} finally {
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
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

