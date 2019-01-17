package telemetry;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import uk.me.g4dpz.satellite.SatPos;

public abstract class FramePart extends BitArray implements Comparable<FramePart> {
	public static final DateFormat reportDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static final DateFormat fileDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
	// These fields are updated when the Frame Part is stored in the PayloadStore
	public int id; // The id copied from the header of the highspeed or slow speed frame that this was captured in
	public long uptime;  // The Uptime captured from the header.  Time in seconds from Reset.  For non Fox Spacecraft this is the UTC milliseconds since the date epoch
	public int resets;  // The resets captured from the header.  Zero for Non FOX Spacecraft
	protected String captureDate; // the date/time that this was captured
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

//	public void captureHeaderInfo(int id, long uptime, int resets, String captureDate, int mode) {
	public void captureHeaderInfo(int id, long uptime, int resets) {
		this.id = id;
		this.uptime = uptime;
		this.resets = resets;
		if (captureDate == null)
			this.captureDate = fileDateStamp(); // snap the current time
		else
			this.captureDate = captureDate;
//		this.newMode = mode; // this is zero unless the mode was on the header.
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
	public String getCaptureDate() { return captureDate; }
	
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

	@Override
	public double convertRawValue(String name, int rawValue, int conversion, Spacecraft fox) {
		// TODO Auto-generated method stub
		return 0;
	}
	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile(boolean storeMode) {
		copyBitsToFields();
		String s = new String();
		s = s + captureDate + "," + id + "," + resets + "," + uptime + "," + type + ",";
		
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
		s = s + "values ('" + this.captureDate + "', " + this.id + ", " + this.resets + ", " + this.uptime + ", " + this.type + ",\n";
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
}
