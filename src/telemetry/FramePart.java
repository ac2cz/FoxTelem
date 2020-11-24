package telemetry;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import telemetry.uw.CanPacket;
import uk.me.g4dpz.satellite.SatPos;

public abstract class FramePart extends BitArray implements Comparable<FramePart> {
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
	public static final int TYPE_WOD_RAD = 11; // Whole orbit data ib Fox-1E
	public static final int TYPE_WOD_RAD_TELEM_DATA = 12; // Translated Vulcan WOD
	
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
	
	// These fields are updated when the Frame Part is stored in the PayloadStore
	public int id; // The id copied from the header of the highspeed or slow speed frame that this was captured in
	public long uptime;  // The Uptime captured from the header.  Time in seconds from Reset.  For non Fox Spacecraft this is the UTC milliseconds since the date epoch
	public int resets;  // The resets captured from the header.  Zero for Non FOX Spacecraft
	protected String reportDate; // the date/time that this was written to the file.  NOT the same as the STP date, which is just on the frame.
	protected int type; // the type of this payload. Zero if the spacecraft does not use types
	public int newMode = FoxSpacecraft.NO_MODE; // this is only valid for HuskySat and later.  Otherwise set to NO_MODE
	public static final double NO_POSITION_DATA = -999.0;
	public static final double NO_T0 = -998.0;
	public static final double NO_TLE = -997.0;
	
	// lat lon are stored in degrees
	double satLatitude = NO_POSITION_DATA;  // from -90 to 90
	double satLongitude = NO_POSITION_DATA; // from -180 to 180
	double satAltitude = NO_POSITION_DATA;
	
	protected FramePart(BitArrayLayout l, int type) {
		super(l);
		this.type = type;
		// TODO Auto-generated constructor stub
	}

	public void captureHeaderInfo(int id, long uptime, int resets) {
		this.id = id;
		this.uptime = uptime;
		this.resets = resets;
		if (reportDate == null)
			this.reportDate = fileDateStamp(); // snap the current time
	}
	
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
	
	@Override
	public String getStringValue(String name, Spacecraft fox) {
		return ""+getDoubleValue(name, fox);
	}

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
		if (newMode != FoxSpacecraft.NO_MODE)
			s = s + "newMode,";
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + layout.fieldName[i] + ",\n";
		}
		s = s + layout.fieldName[layout.fieldName.length-1] + ")\n";
		s = s + "values ('" + this.reportDate + "', " + this.id + ", " + this.resets + ", " + this.uptime + ", " + this.type + ",\n";
		if (newMode != FoxSpacecraft.NO_MODE)
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
	 * Factory Method to make a new Frame Part from a layout
	 * @return
	 */
	public static FramePart makePayload(Header header, BitArrayLayout layout) {
		return makePayload(header, layout.name);
	}
	
	public static FramePart makePayload(Header header, String layoutName) {
		switch (layoutName) {
			case Spacecraft.REAL_TIME_LAYOUT:
				return new PayloadRtValues(Config.satManager.getLayoutByName(header.id, Spacecraft.REAL_TIME_LAYOUT));
			case Spacecraft.MAX_LAYOUT:
				return new PayloadMaxValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MAX_LAYOUT));
			case Spacecraft.MIN_LAYOUT:
				return new PayloadMinValues(Config.satManager.getLayoutByName(header.id, Spacecraft.MIN_LAYOUT));
			case Spacecraft.RAD_LAYOUT:
				return new PayloadRadExpData(Config.satManager.getLayoutByName(header.id, Spacecraft.RAD_LAYOUT));
			case Spacecraft.WOD_LAYOUT:
				return new PayloadWOD(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_LAYOUT));
			case Spacecraft.WOD_RAD_LAYOUT:
				return new PayloadWODRad(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAD_LAYOUT));
			case Spacecraft.WOD_CAN_LAYOUT:
				return new PayloadWODUwExperiment(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_CAN_LAYOUT), header.id, header.uptime, header.resets);
			case Spacecraft.CAN_LAYOUT:
				return new PayloadUwExperiment(Config.satManager.getLayoutByName(header.id, Spacecraft.CAN_LAYOUT), header.id, header.uptime, header.resets);
			case Spacecraft.RAG_LAYOUT:
				return new PayloadRagAdac(Config.satManager.getLayoutByName(header.id, Spacecraft.RAG_LAYOUT), header.id, header.uptime, header.resets);
			case Spacecraft.WOD_RAG_LAYOUT:
				return new PayloadWODRagAdac(Config.satManager.getLayoutByName(header.id, Spacecraft.WOD_RAG_LAYOUT), header.id, header.uptime, header.resets);
			default:
				return null;	
		}
	}
	
	public static FramePart makePayload(int id, int resets, long uptime, String date, StringTokenizer st, int type) {
		FoxFramePart rt = null;
		
		if (type == FoxFramePart.TYPE_REAL_TIME) {
			rt = new PayloadRtValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.REAL_TIME_LAYOUT));
		} else if (type == FoxFramePart.TYPE_WOD) {
			rt = new PayloadWOD(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_LAYOUT));
		} else if (type == FoxFramePart.TYPE_MAX_VALUES) {
			rt = new PayloadMaxValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.MAX_LAYOUT));

		} else if (type == FoxFramePart.TYPE_MIN_VALUES) {
			rt = new PayloadMinValues(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.MIN_LAYOUT));

		} else if (type == FoxFramePart.TYPE_RAD_TELEM_DATA || type >= 700 && type < 800) {
			rt = new RadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.RAD2_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_WOD_RAD_TELEM_DATA ) {
			rt = new WodRadiationTelemetry(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD2_LAYOUT));
			rt.type = type; // make sure we get the right type

		} else if (type == FoxFramePart.TYPE_RAD_EXP_DATA || type >= 400 && type < 500) {
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
		} else if (type == FoxFramePart.TYPE_WOD_RAD) {
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
		} else if (type == FoxFramePart.TYPE_HERCI_HIGH_SPEED_DATA || type >= 600 && type < 700) {
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
		} else if (type == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER || type >= 800 && type < 900) {
			rt = new HerciHighspeedHeader(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_HEADER_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_HERCI_HS_PACKET || type >= 600900 && type < 700000) {
			rt = new HerciHighSpeedPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.HERCI_HS_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_UW_CAN_PACKET || type >= 1400 && type < 1500) {
			rt = new CanPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.CAN_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_UW_WOD_CAN_PACKET || type >= 1600 && type < 1700) {
			rt = new CanPacket(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_CAN_PKT_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_UW_EXPERIMENT || type >= 1300 && type < 1400 ) {
			rt = new PayloadUwExperiment(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.CAN_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_UW_WOD_EXPERIMENT || type >= 1500 && type < 1600 ) {
			rt = new PayloadWODUwExperiment(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_CAN_LAYOUT));
			rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_RAG_TELEM ) {
				rt = new PayloadRagAdac(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.RAG_LAYOUT));
				rt.type = type; // make sure we get the right type
		} else if (type == FoxFramePart.TYPE_WOD_RAG ) {
			rt = new PayloadWODRagAdac(id, resets, uptime, date, st, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAG_LAYOUT));
			rt.type = type; // make sure we get the right type
	}
		return rt;
	}
}
