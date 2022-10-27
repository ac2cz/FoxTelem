package telemetry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.conversion.Conversion;
import telemetry.conversion.ConversionMathExpression;
import telemetry.conversion.ConversionTimestamp;
import telemetry.frames.Header;
import telemetry.herci.HerciHighSpeedPacket;
import telemetry.herci.HerciHighspeedHeader;
import telemetry.herci.PayloadHERCIhighSpeed;
import telemetry.legacyPayloads.PayloadRadExpData;
import telemetry.legacyPayloads.PayloadWODRad;
import telemetry.legacyPayloads.RadiationTelemetry;
import telemetry.legacyPayloads.WodRadiationTelemetry;
import telemetry.payloads.PayloadCanExperiment;
import telemetry.payloads.PayloadCanWODExperiment;
import telemetry.payloads.PayloadExperiment;
import telemetry.payloads.PayloadMaxValues;
import telemetry.payloads.PayloadMinValues;
import telemetry.payloads.PayloadRtValues;
import telemetry.payloads.PayloadWOD;
import telemetry.payloads.PayloadWODExperiment;
import telemetry.payloads.CanPacket;
import telemetry.uw.PayloadUwExperiment;
import telemetry.uw.PayloadWODUwExperiment;
import telemetry.uw.UwCanPacket;
import uk.me.g4dpz.satellite.SatPos;

/**
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2022 amsat.org
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
 * This class is the core storage unit for frames.  Payloads are frame parts and extend this class.
 * It handles the creation and storage of payloads as well as converting raw values to human
 * readable values by calling the appropriate conversions.
 * 
 * A frame part is an array of bits and it extends BitArray.  This allows us to parse the bits
 * from the spacecraft.  The organization of the bits into telemetry fields is stored in the
 * layout, which is handled by the BitArrayLayout class.  This also stores details of which
 * conversions to use and how to display the fields on the screen in FoxTelem.
 * 
 * 
 */
public abstract class FramePart extends BitArray implements Comparable<FramePart> {
	protected int MAX_BYTES = 78;  // This provides enough storage to cover the zero filled bytes at the end of the Slow, High Speed frames or 1E frames

	/**
	 * These are legacy types and should not be used for processing in the future.  All processing should be based on
	 * the layout and attributes of the layout that are set in the MASTER file.
	 */
	public static final int TYPE_DEBUG = 0;
	public static final int TYPE_REAL_TIME = 1;
	public static final int TYPE_MAX_VALUES = 2;
	public static final int TYPE_MIN_VALUES = 3;
	public static final int TYPE_CAMERA_DATA = 5;
	public static final int TYPE_RAD_EXP_DATA = 4; // This is both Vulcan and HERCI
	public static final int TYPE_HERCI_HIGH_SPEED_DATA = 6;
	public static final int TYPE_RAD_TELEM_DATA = 7;  // Translated both Vulcan and HERCI HK
	public static final int TYPE_HERCI_SCIENCE_HEADER = 8; // This is the header from the high speed data once decoded
	public static final int TYPE_HERCI_HS_PACKET = 9; // This is the header from the high speed data once decoded
	public static final int TYPE_WOD = 10; // Whole orbit data ib Fox-1E
	public static final int TYPE_WOD_EXP = 11; // Whole orbit data ib Fox-1E
	public static final int TYPE_WOD_EXP_TELEM_DATA = 12; // Translated Vulcan WOD

	public static final int TYPE_CAN_EXP = 13; // Whole orbit data ib Fox-1E
	public static final int TYPE_CAN_WOD_EXP = 14; // Translated Vulcan WOD
	public static final int TYPE_CAN_PACKET = 15;
	public static final int TYPE_CAN_WOD_PACKET = 16;

	// UW 
	public static final int TYPE_UW_EXPERIMENT = 13; // UW Experiment Payload
	public static final int TYPE_UW_CAN_PACKET = 14; // UW Can packets for HuskySat
	public static final int TYPE_UW_WOD_EXPERIMENT = 15; // WOD for UW Experiment Payload
	public static final int TYPE_UW_WOD_CAN_PACKET = 16; // UW Can packets from WOD for HuskySat
	public static final int TYPE_UW_CAN_PACKET_TELEM = 17; // UW Can packets split into their ids

	// Golf
	public static final int TYPE_RAG_TELEM = 18; // UW Can packets from WOD for HuskySat
	public static final int TYPE_WOD_RAG = 19; // UW Can packets split into their ids

	// These are infrastructure and not saved to Disk
	public static final int TYPE_SLOW_SPEED_HEADER = 98;
	public static final int TYPE_SLOW_SPEED_TRAILER = 99;
	public static final int TYPE_HIGH_SPEED_HEADER = 100;
	public static final int TYPE_HIGH_SPEED_TRAILER = 101;
	public static final int TYPE_CAMERA_SCAN_LINE_COUNT = 102;
	public static final int TYPE_HERCI_LINE_COUNT = 103;
	public static final int TYPE_EXTENDED_HEADER = 104;
	public static final int TYPE_GOLF_HEADER = 105;

	// NOTE THAT TYPE 400+ are reserverd for the High Speed Radiation Payloads, where type is part of the uniqueness check
	// Correspondingly TYPE 600+ are reserved for Herci HS payloads
	// Correspondingly TYPE 800+ are reserved for Herci Telemetry payloads
	// Correspondingly TYPE 900+ are reserved for Herci Packets payloads
	// Correspondingly TYPE 700+ are reserved for Rad Telemetry payloads

	public static final DateFormat reportDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final DateFormat dateFormat2 = new SimpleDateFormat(
			"yyyyMMdd HHmmss", Locale.ENGLISH);

	// These fields are updated when the Frame Part is stored in the PayloadStore
	public int id; // The id copied from the header of the highspeed or slow speed frame that this was captured in
	public long uptime;  // The Uptime captured from the header.  Time in seconds from Reset.  For non Fox Spacecraft this is the UTC milliseconds since the date epoch
	public int resets;  // The resets captured from the header.  Zero for Non FOX Spacecraft
	protected String reportDate; // the date/time that this was written to the file.  NOT the same as the STP date, which is just on the frame.
	public int type; // the type of this payload. Zero if the spacecraft does not use types
	public int newMode = Spacecraft.NO_MODE; // this is only valid for HuskySat and later.  Otherwise set to NO_MODE
	public static final double NO_POSITION_DATA = -999.0;
	public static final double NO_T0 = -998.0;
	public static final double NO_TLE = -997.0;

	// lat lon are stored in degrees
	double satLatitude = NO_POSITION_DATA;  // from -90 to 90
	double satLongitude = NO_POSITION_DATA; // from -180 to 180
	double satAltitude = NO_POSITION_DATA;

	public FramePart(int id, int resets, long uptime, int type, String date, StringTokenizer st, BitArrayLayout lay) {
		super(lay);
		this.type = type;
		this.id = id;
		this.resets = resets;
		this.uptime = uptime;
		this.reportDate = date;
		init();
		rawBits = null; // no binary array when loaded from file, even if the local init creates one
		load(st);
	}

	public FramePart(int id, int resets, long uptime, int type, String date, byte[] data, BitArrayLayout lay) {
		super(lay);
		this.type = type;
		this.id = id;
		this.resets = resets;
		this.uptime = uptime;
		this.reportDate = date;
		init();
		for (byte b : data) {
			addNext8Bits(b);
		}
		copyBitsToFields();
	}

	public FramePart(int type, BitArrayLayout lay) {
		super(lay);
		this.type = type;
		init();
	}

	/**
	 * Create a new payload based on the result set from the db
	 * @param results
	 * @throws SQLException 
	 */
	public FramePart(ResultSet results, int type, BitArrayLayout lay) throws SQLException {
		super(lay);
		this.type = type;
		this.id = results.getInt("id");
		this.resets = results.getInt("resets");
		this.uptime = results.getLong("uptime");
		this.reportDate = results.getString("captureDate");
		init();
		rawBits = null; // no binary array when loaded from database
		for (int i=0; i < fieldValue.length; i++) {
			fieldValue[i] = results.getInt(layout.fieldName[i]);
		}
		//		results.close();
	}

	public void captureHeaderInfo(int id, long uptime, int resets) {
		this.id = id;
		this.uptime = uptime;
		this.resets = resets;
		if (reportDate == null)
			this.reportDate = fileDateStamp(); // snap the current time
	}

	abstract protected void init();
	public abstract boolean isValid();

	public double getSatLatitude() { return satLatitude; }
	public double getSatLongitude() { return satLongitude; }
	public double getSatAltitude() { return satAltitude; }

	/**
	 * Store the satellite position. If T0 is not available and the satellite position can not be calculated then pass null for the position
	 * @param pos
	 */
	public void setSatPosition(SatPos pos) {
		if (pos != null) {
			satLatitude = latRadToDeg (pos.getLatitude());
			satLongitude = lonRadToDeg(pos.getLongitude());
			satAltitude = pos.getAltitude();

			//if (Config.debugFrames)
			//	Log.println("POSITION captured : " + resets + ":" + uptime + " Type: " + type + " at " + satLatitude + " " + satLongitude);
		} else {
			satLatitude = NO_POSITION_DATA;
			satLongitude = NO_POSITION_DATA;
			satAltitude = NO_POSITION_DATA;
		}
	}

	public String getSatLatitudeStr() { 
		DecimalFormat d = new DecimalFormat("00.00");
		if (satLatitude == NO_POSITION_DATA)
			return "UNK";
		else if (satLatitude == NO_T0)
			return "T0 NOT SET";
		else if (satLatitude == NO_TLE)
			return "NO TLE";
		else
			return d.format(satLatitude); 
	}

	public String getSatLongitudeStr() { 
		DecimalFormat d = new DecimalFormat("00.00");
		if (satLongitude == NO_POSITION_DATA)
			return "UNK";
		else if (satLongitude == NO_T0)
			return "T0 NOT SET";
		else if (satLongitude == NO_TLE)
			return "NO TLE";
		else
			return d.format (satLongitude); 
	}

	public static double radToDeg(Double rad) {
		return 180 * (rad / Math.PI);
	}
	public static double latRadToDeg(Double rad) {
		return radToDeg(rad);
	}

	public static double lonRadToDeg(Double rad) {
		double lon = radToDeg(rad);
		if (lon > 180)
			return lon -360;
		else
			return lon;
	}

	public int getFoxId() { return id; }
	public long getUptime() { return uptime; }
	public int getResets() { return resets; }
	public int getType() { return type; }
	public String getCaptureDate() { return reportDate; }

	public static String fileDateStamp() {

		Date today = Calendar.getInstance().getTime();  
		fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String reportDate = fileDateFormat.format(today);
		return reportDate;
	}

	@Override
	public int compareTo(FramePart p) {
		if (resets == p.resets && uptime == p.uptime && type == p.type) 
			return 0;
		else if (resets < p.resets)
			return -1;
		else if (resets > p.resets)
			return +1;
		else if (resets == p.resets && uptime == p.uptime) {
			if (type < p.type)
				return -1;
			if (type > p.type)
				return +1;
		} else if (resets == p.resets) {	
			if (uptime < p.uptime)
				return -1;
			if (uptime > p.uptime)
				return +1;
		} 
		return +1;
	}

	//	@Override
	//	public String getStringValue(String name, Spacecraft fox) {
	//		return ""+getDoubleValue(name, fox);
	//	}

	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile(boolean storeMode) {
		copyBitsToFields();
		String s = new String();
		s = s + reportDate + "," + id + "," + resets + "," + uptime + "," + type + ",";

		// If we have the mode in the header we save it here
		if (storeMode)
			s = s + newMode + ",";

		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + getRawValue(layout.fieldName[i]) + ",";
		}
		s = s + getRawValue(layout.fieldName[layout.fieldName.length-1]);
		return s;
	}

	public String getInsertStmt() {
		copyBitsToFields();
		String s = new String();
		s = s + " (captureDate,  id, resets, uptime, type, \n";
		if (newMode != Spacecraft.NO_MODE)
			s = s + "newMode,";
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + layout.fieldName[i] + ",\n";
		}
		s = s + layout.fieldName[layout.fieldName.length-1] + ")\n";
		s = s + "values ('" + this.reportDate + "', " + this.id + ", " + this.resets + ", " + this.uptime + ", " + this.type + ",\n";
		if (newMode != Spacecraft.NO_MODE)
			s = s + newMode+",\n";
		for (int i=0; i < fieldValue.length-1; i++) {
			s = s + fieldValue[i] + ",\n";
		}
		s = s + fieldValue[fieldValue.length-1] + ")\n";
		return s;
	}

	public abstract String toString();

	/**
	 * Load this framePart from a file, which has been opened by a calling method.  The string tokenizer contains a 
	 * set of tokens that represent the raw values to be loaded into the fields.
	 * The header has already been loaded by the calling routine.
	 * @param st
	 */
	protected void load(StringTokenizer st) {
		int i = 0;
		String s = null;
		try {
			while((s = st.nextToken()) != null) {
				if (s.startsWith("0x")) {
					s = s.replace("0x", "");
					fieldValue[i++] = Integer.valueOf(s,16);
				} else
					fieldValue[i++] = Integer.valueOf(s).intValue();
			}
		} catch (NoSuchElementException e) {
			// we are done and can finish. The line was terminated and is corrupt.
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.errorDialog("ERROR: Too many fields: Index out of bounds", "Layout: " + layout.name +"\n" + e + 
					" Could not load field "+i+ " SAT: " + this.id + " Reset:" + this.resets + " Up:" + this.uptime + " Type:" + this.type);
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  " + n.getMessage() + " Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
			Log.errorDialog("LOAD ERROR - DEBUG MESSAGE", "ERROR: Invalid number:  " + n.getMessage() + " Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
		}
	}

	/**
	 * 
	 * Factory Method to make a V3 SEG DB Frame Part from a layout
	 * @return
	 */
	public static FramePart makePayload(Header header, BitArrayLayout layout) {
		return makePayload(header, layout.name);
	}

	public static FramePart makePayload(Header header, String layoutName) {
		return makePayload(header.id, header.resets, header.uptime, layoutName);
	}

	/**
	 * Make a V3 SEG DB payload from its layout type name
	 * @param header
	 * @param layoutName
	 * @return
	 */
	public static FramePart makePayload(int id, int resets, long uptime, String layoutName) {
		BitArrayLayout layout = Config.satManager.getLayoutByName(id, layoutName);
		// TODO - setting the reset/uptime should be forced in the constructor for FramePart
		switch (layout.typeStr) {
		case BitArrayLayout.RT:
			return new PayloadRtValues(layout);
		case BitArrayLayout.MAX:
			return new PayloadMaxValues(layout);
		case BitArrayLayout.MIN:
			return new PayloadMinValues(layout);
		case BitArrayLayout.WOD:
			return new PayloadWOD(layout);
		case BitArrayLayout.EXP:
			return new PayloadExperiment(layout, id, uptime, resets);
		case BitArrayLayout.WOD_EXP:
			return new PayloadWODExperiment(layout, id, uptime, resets);
		case BitArrayLayout.CAN_EXP:
			return new PayloadCanExperiment(layout, id, uptime, resets);
		case BitArrayLayout.CAN_WOD_EXP:
			return new PayloadCanWODExperiment(layout, id, uptime, resets);
		case BitArrayLayout.CAN_PKT:
			return new CanPacket(layout, id, uptime, resets);
		case BitArrayLayout.WOD_CAN_PKT:
			return new CanPacket(layout, id, uptime, resets);
		default:
			return null;
		}
	}

	/**
	 * 
	 * Factory Method to make a pre V3 SEGDB Frame Part from a layout
	 * @return
	 */
	@Deprecated
	public static FramePart makeLegacyPayload(Header header, BitArrayLayout layout) {
		return makeLegacyPayload(header, layout.name);
	}

	@Deprecated
	public static FramePart makeLegacyPayload(Header header, String layoutName) {
		return makeLegacyPayload(header.id, header.resets, header.uptime, layoutName);
	}

	/**
	 * Make a pre V3 SEG DB payload from its layout
	 * @param header
	 * @param layoutName
	 * @return
	 */
	@Deprecated
	public static FramePart makeLegacyPayload(int id, int resets, long uptime, String layoutName) {
		BitArrayLayout layout = Config.satManager.getLayoutByName(id, layoutName);
		// TODO - setting the reset/uptime should be forced in the constructor for FramePart
		switch (layoutName) {
		case Spacecraft.REAL_TIME_LAYOUT:
			return new PayloadRtValues(layout);
		case Spacecraft.MAX_LAYOUT:
			return new PayloadMaxValues(layout);
		case Spacecraft.MIN_LAYOUT:
			return new PayloadMinValues(layout);
		case Spacecraft.RAD_LAYOUT:
			return new PayloadRadExpData(layout);
		case Spacecraft.WOD_LAYOUT:
			return new PayloadWOD(layout);
		case Spacecraft.WOD_RAD_LAYOUT:
			return new PayloadWODRad(layout);
		case Spacecraft.WOD_CAN_LAYOUT:
			return new PayloadWODUwExperiment(layout, id, uptime, resets);
		case Spacecraft.CAN_LAYOUT:
			return new PayloadUwExperiment(layout, id, uptime, resets);
		default:
			// Other experiment data, though this should use the V3 method
			if (layout.isExperiment())
				return new PayloadExperiment(layout, id, uptime, resets);
			if (layout.isWODExperiment())
				return new PayloadWODExperiment(layout, id, uptime, resets);

			return null;
		}
	}

	/**
	 * Make a payload from the V3 SegDB using its Layout
	 * Before V3 SegDB there is no guarantee that the layout type is set correctly for this to work
	 * 
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param date
	 * @param st
	 * @param lay
	 * @return
	 */
	public static FramePart makePayload(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		FramePart rt = null;
		if (lay.isRealTime())
			rt = new PayloadRtValues(id, resets, uptime, date, st, lay);
		else if (lay.isMAX())
			rt = new PayloadMaxValues(id, resets, uptime, date, st, lay);
		else if (lay.isMIN())
			rt = new PayloadMinValues(id, resets, uptime, date, st, lay);
		else if (lay.isWOD())
			rt = new PayloadWOD(id, resets, uptime, date, st, lay);
		else if (lay.isWODExperiment())
			rt = new PayloadWODExperiment(id, resets, uptime, date, st, lay);
		else if (lay.isExperiment())
			rt = new PayloadExperiment(id, resets, uptime, date, st, lay);
		else if (lay.isCanExperiment())
			rt = new PayloadCanExperiment(id, resets, uptime, date, st, lay);		
		else if (lay.isCanWodExperiment())
			rt = new PayloadCanWODExperiment(id, resets, uptime, date, st, lay);	
		else if (lay.isCanPkt())
			rt = new CanPacket(id, resets, uptime, date, st, lay);
		else if (lay.isCanWodPkt())
			rt = new CanPacket(id, resets, uptime, date, st, lay);	
		return rt;
	}

	/**
	 * Make a payload from the LEGACY V3 DB based on the type that was stored in the DB flat file.  
	 * Avoid following this process in the future
	 * 
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param date
	 * @param st
	 * @param type
	 * @return
	 */
	@Deprecated
	public static FramePart makeLegacyPayload(int id, int resets, long uptime, String date, StringTokenizer st, int type) {
		FramePart rt = null;

		if (type == FramePart.TYPE_REAL_TIME) {
			rt = new PayloadRtValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.REAL_TIME_LAYOUT));
		} else if (type == FramePart.TYPE_WOD) {
			rt = new PayloadWOD(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_LAYOUT));
		} else if (type == FramePart.TYPE_MAX_VALUES) {
			rt = new PayloadMaxValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.MAX_LAYOUT));

		} else if (type == FramePart.TYPE_MIN_VALUES) {
			rt = new PayloadMinValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.MIN_LAYOUT));

		} else if (type == FramePart.TYPE_RAD_TELEM_DATA || type >= 700 && type < 800) {
			rt = new RadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.RAD2_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FramePart.TYPE_WOD_EXP_TELEM_DATA ) {
			rt = new WodRadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD2_LAYOUT));
			rt.type = type; // make sure we get the right type

		} else if (type == FramePart.TYPE_RAD_EXP_DATA || type >= 400 && type < 500) {
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
		} else if (type == FramePart.TYPE_WOD_EXP) {
			rt = new PayloadWODRad(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD_LAYOUT));
			rt.type = type;

			// hack to convert data - only used in testing
			if (Config.generateSecondaryPayloads) {
				PayloadWODRad f = (PayloadWODRad)rt; 
				WodRadiationTelemetry radiationTelemetry = f.calculateTelemetryPalyoad();
				radiationTelemetry.captureHeaderInfo(f.id, f.uptime, f.resets);
				Config.payloadStore.add(f.id, f.uptime, f.resets, radiationTelemetry);
				Config.payloadStore.setUpdated(id, Spacecraft.WOD_RAD_LAYOUT, true);			
			}
		} else if (type == FramePart.TYPE_HERCI_HIGH_SPEED_DATA || type >= 600 && type < 700) {
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
		} else if (type == FramePart.TYPE_HERCI_SCIENCE_HEADER || type >= 800 && type < 900) {
			rt = new HerciHighspeedHeader(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_HEADER_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FramePart.TYPE_HERCI_HS_PACKET || type >= 600900 && type < 700000) {
			rt = new HerciHighSpeedPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FramePart.TYPE_UW_CAN_PACKET || type >= 1400 && type < 1500) {
			rt = new UwCanPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FramePart.TYPE_UW_WOD_CAN_PACKET || type >= 1600 && type < 1700) {
			rt = new CanPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_CAN_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FramePart.TYPE_UW_EXPERIMENT || type >= 1300 && type < 1400 ) {
			rt = new PayloadUwExperiment(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.CAN_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FramePart.TYPE_UW_WOD_EXPERIMENT || type >= 1500 && type < 1600 ) {
			rt = new PayloadWODUwExperiment(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_CAN_LAYOUT));
			rt.type = type; // make sure we get the right type
		} 
		//		else if (type == FoxFramePart.TYPE_RAG_TELEM ) {
		//				rt = new PayloadRagAdac(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.RAG_LAYOUT));
		//				rt.type = type; // make sure we get the right type
		//		} else if (type == FoxFramePart.TYPE_WOD_RAG ) {
		//			rt = new PayloadWODRagAdac(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAG_LAYOUT));
		//			rt.type = type; // make sure we get the right type
		//		}
		return rt;
	}
	
	/**
	 * Calculate the secondary payload and return it
	 * @return
	 */
	public FramePart getSecondaryPayload() {
		if (this.layout.getSecondaryPayloadName() != null) {
			FramePart telem = FramePart.makePayload(id, resets, uptime, this.layout.getSecondaryPayloadName());
			for (int k=0; k<telem.getMaxBytes(); k++) { 
				telem.addNext8Bits((byte) fieldValue[k]);
			}
			return telem;
		}
		return null;
	}

	//*************************************************************************
	// Conversion Section



	public int getMaxBytes() { return layout.getMaxNumberOfBytes(); }
	public int getMaxBits() { return layout.getMaxNumberOfBits(); }

	@Deprecated
	public boolean isValidType(int t) {
		if (t == TYPE_DEBUG) return true;
		if (t == TYPE_REAL_TIME) return true;
		if (t == TYPE_MAX_VALUES) return true;
		if (t == TYPE_MIN_VALUES) return true;
		if (t == TYPE_RAD_EXP_DATA) return true;
		return false;

	}

	public static String reportDate() {

		// Get the date using Calendar object.
		// java.util.Date has no timezone, but the forat will give it the 
		// local timezone unless we tell it otherwise
		Date today = Calendar.getInstance().getTime();  
		reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.
		String reportDate = reportDateFormat.format(today);
		return reportDate;
	}

	/**
	 * Get the string representation of a field in this framePart.  Run any conversion
	 * routine assigned to this field
	 * @param name
	 * @return
	 */
	public String getStringValue(String name, Spacecraft fox) {
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i])) {
				pos = i;
				break;
			}
		}

		String convName = layout.getConversionNameByPos(pos);
		//// TESTING ONLY  
		//	if (convName.equalsIgnoreCase("8_bit_temp|FLOAT2"))  // trap for testing
		//		System.out.println("STOP");

		// First calculate the value as normal, converting the raw value
		double dvalue = getDoubleValue(name, fox);
		String s = "-----";
		if (pos != -1) {
			String lastConv = Conversion.getLastConversionInPipeline(convName);
			Conversion conv = fox.getConversionByName(lastConv);
			if (conv == null) return s;
			if (conv instanceof ConversionTimestamp) {
				s = ((ConversionTimestamp) conv).calculateTimeString(dvalue, this);
				
			} else
				s = conv.calculateString(dvalue);
		}
		if (s.length() < 5)
			for (int k=0; k < (5 - s.length()); k++)
				s = " " + s;
		return s;
	}

	public static String intToBin(int word, int len) {
		boolean b[] = new boolean[len];
		for (int i=0; i<len; i++) {
			if (((word >>i) & 0x01) == 1) b[len-1-i] = true; else b[len-1-i] = false; 
		}
		String s = "";
		for (boolean bit : b)
			if (bit) s=s+"1"; else s=s+"0";
		return s;
	}

	public static String toHexString(long value, int len) {
		String s = "";
		for (int i=0; i<len; i++) {
			String digit = String.format("%1s", Long.toHexString(value & 0xf)).replace(' ', '0');
			s = digit + s; // we get the least sig byte each time, so new bytes go on the front
			value = value >> 4 ;
		}
		return s;
	}

	public static String txByteString(long value, int len) {
		String s = "";
		for (int i=0; i<len; i++) {
			s = plainhex(value & 0xff) + s; // we get the least sig byte each time, so new bytes go on the front
			value = value >> 8 ;
		}
		return s;
	}

	public static String plainhex(long l) {
		return String.format("%2s", Long.toHexString(l)).replace(' ', '0');
	}
	
	/**
	 * Return the value of this field, specified by its name.  Run any conversion routine
	 * to BitArrayLayout.CONVERT_this into the appropriate units.
	 * Used to plot graphs
	 * @param name
	 * @return
	 */
	public double getDoubleValue(String name, Spacecraft fox) {
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i])) {
				pos = i;
				break;
			}
		}

		if (pos == -1) return ERROR_VALUE;
		int value = fieldValue[pos];
		double result = value; // initialize the result to the value we start with, in case its a pipeline

		String convName = layout.getConversionNameByPos(pos);
		//				if (convName.equalsIgnoreCase("36|HEX2"))  // trap for testing
		//					System.out.println("STOP");
		String[] conversions = convName.split("\\|"); // split the conversion based on | in case its a pipeline
		for (String singleConv : conversions) {
			singleConv = singleConv.trim();
			Conversion conv = fox.getConversionByName(singleConv);					
			if (conv == null) { // use legacy conversion, remain backwards compatible if name is numeric. String conversions ignored here
				return ERROR_VALUE;
			} else
				result = convertCoeffRawValue(name, result, conv, fox);	
		}

		return result;
	}
	
	public double convertRawValue(String name, double value, Spacecraft fox) {
		double result = value; // initialize the result to the value we start with, in case its a pipeline
		String convName = layout.getConversionNameByName(name);
		String[] conversions = convName.split("\\|"); // split the conversion based on | in case its a pipeline
		for (String singleConv : conversions) {
			singleConv = singleConv.trim();
			Conversion conv = fox.getConversionByName(singleConv);					
			if (conv == null) { // use legacy conversion, remain backwards compatible if name is numeric. String conversions ignored here
				return ERROR_VALUE;
			} else
				result = convertCoeffRawValue(name, result, conv, fox);	
		}

		return result;
	}
	
	public static final String GPS_VALID = "UTCValid";
	public static final String GPS_SECONDS = "UTCSeconds";
	public static final String GPS_MINUTES = "UTCMinutes";
	public static final String GPS_HOURS = "UTCHours";
	public static final String GPS_DAY = "UTCDay";
	public static final String GPS_MONTH = "UTCMonth";
	public static final String GPS_YEAR = "UTCYear";
	public static final String GPS_SECS_IN_EPOCH = "secsInEpoch";
	
	public ZonedDateTime getGPSTime(Spacecraft fox) {
		if (!fox.hasGPSTime) return null;
		if (!layout.hasGPSTime) return null;
		//long longGpsTime = 0;
		
		int valid = (int) getDoubleValue(GPS_VALID, fox);
		if (valid == 0) return null;
		
		int sec = (int) getDoubleValue(GPS_SECONDS, fox);
		if (sec < 0 || sec > 59) return null;
		
		int min = (int) getDoubleValue(GPS_MINUTES, fox);
		if (min < 0 || min > 59) return null;
		
		int hrs = (int) getDoubleValue(GPS_HOURS, fox);
		if (hrs < 0 || hrs > 23) return null;
		
		int days = (int) getDoubleValue(GPS_DAY, fox);
		if (days < 1 || days > 31) return null;
		
		int mths = (int) getDoubleValue(GPS_MONTH, fox);
		if (mths < 1 || mths > 12) return null;
		
		int yrs = (int) getDoubleValue(GPS_YEAR, fox);
		yrs += 2000;
		
		ZonedDateTime dateTime = ZonedDateTime.of(yrs,  mths, days, hrs, min, sec, 0, ZoneId.of("UTC"));
		
		return dateTime;
	}
	
	public long getSecsInEpochAtGPSTimestamp() {
		long secs = 0;
		int pos = -1;
		for (int i=0; i < layout.fieldName.length; i++) {
			if (GPS_SECS_IN_EPOCH.equalsIgnoreCase(layout.fieldName[i])) {
				pos = i;
				break;
			}
		}

		if (pos == -1) return 0;
		secs = (long)(fieldValue[pos]);
		return secs;
	}

	/**
	 * Given a raw value, convert it with a curve, lookup table it into the actual value that we can display based on the
	 * conversion type passed.  
	 * @param name
	 * @param rawValue
	 * @param conversion
	 * @param fox
	 * @return
	 */
	@SuppressWarnings("deprecation")
	protected double convertCoeffRawValue(String name, double rawValue, Conversion conversion, Spacecraft fox) {
		double x = 0;
		try {
			if (conversion instanceof ConversionMathExpression)
				x = ((ConversionMathExpression)conversion).calculateExpression(rawValue, this);
			else if (conversion instanceof telemetry.conversion.ConversionLegacy)
				x = ((telemetry.conversion.ConversionLegacy)conversion).calculateLegacy(rawValue, this, name);
			else
				x = conversion.calculate(rawValue);
		} catch (RuntimeException e) {
			Log.errorDialog("Error with Conversion", "Error processing conversion for field "+name+ "\n Using conversion - "
					+conversion.toString() +"\n Error is: " + e.getMessage());
		}
		return x; 
	}
}
