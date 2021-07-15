package measure;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import common.Config;
import common.Log;
import telemetry.BitArrayLayout;
import telemetry.FramePart;

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
 */
public class PassMeasurement extends Measurement {

	public long[] fieldValue = null;

	int endReset = 0;
	long endUptime = 0;
	
	public static final String AOS = "AOS";
	public static final String LOS = "LOS";
	public static final String TCA = "TCA";
	public static final String TCA_FREQ = "TCA_FREQ";
	public static final String START_AZIMUTH = "START_AZIMUTH";
	public static final String END_AZIMUTH = "END_AZIMUTH";
	public static final String MAX_ELEVATION = "MAX_ELEVATION";
	public static final String TOTAL_PAYLOADS = "TOTAL_PAYLOADS";
	public static final String CRC_FAILURES = "CRC_FAILURES";
	
	public static final long ERR = -99999;
	public static final String DEFAULT_VALUE = "-----";

	/**
	 * Load a past measurement from disk
	 * @param id
	 * @param date
	 * @param st
	 */
	public PassMeasurement(int foxid, String dt, int reset, long uptime, int type, StringTokenizer st) {
		id = foxid;
		this.reset = reset;
		this.uptime = uptime;
		this.type = type;
		try {
			this.date = FramePart.fileDateFormat.parse(dt);
		} catch (ParseException e) {
			e.printStackTrace(Log.getWriter());
		}
		layout = Config.satManager.getPassMeasurementLayout(foxid);
		fieldValue = new long[layout.NUMBER_OF_FIELDS];
		if (st!= null) // if null was passed then we are probablly going to call a legacy load routine for a conversion from old data
			load(st);
	}

	/**
	 * Create a new measurement.  We have not received a frame, we have just found the signal with a reasonable Signal To Noise ratio.
	 * @param id
	 * @param type
	 */
	public PassMeasurement(int foxid, int type) {
		id = foxid;
		this.type = SatMeasurementStore.PASS_MEASUREMENT_TYPE;
		date = Calendar.getInstance().getTime();  
		//FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		//String captureDate = FramePart.fileDateFormat.format(date);
		layout = Config.satManager.getPassMeasurementLayout(foxid);
		if (layout == null) { 
			// This sat is not setup in the measurement store
			Log.println("FoxId: " + id + " is not setup in the measurement store.  Can not create pass measurement");
			return;
		}
		fieldValue = new long[layout.NUMBER_OF_FIELDS];

		setRawValue(AOS, date.getTime());

	}

	public int getEndReset() { return endReset; }
	public long getEndUptime() { return endUptime; }

	public void setStartResetUptime(int reset, long uptime) {
		this.reset = reset;
		this.uptime = uptime;
	}
	public void setEndResetUptime(int reset, long uptime) {
		this.endReset = reset;
		this.endUptime = uptime;
	}
	
	public String getDateFromLong(long dt) {
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date = FramePart.fileDateFormat.format(dt);
		return date;
	}
	
	public long getRawValue(String name) {
		if (layout == null) { 
			// This sat is not setup in the measurement store
			Log.println("FoxId: " + id + " is not setup in the measurement store.  Can not create pass measurement");
			return 0;
		}
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				return fieldValue[i];
		}
		return ERR;
	}
	
	
	public String getStringValue(String name) {
		if (layout == null) { 
			// This sat is not setup in the measurement store
			Log.println("FoxId: " + id + " is not setup in the measurement store.  Can not create pass measurement");
			return null;
		}
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i])) {
				if (layout.getIntConversionByPos(i) == BitArrayLayout.CONVERT_JAVA_DATE) {
					if (fieldValue[i] == 0)
						return "-----";
					else {
						String dt = getDateFromLong(fieldValue[i]);
						return dt;
					}
				} else if (layout.getIntConversionByPos(i) == BitArrayLayout.CONVERT_FREQ) {
					if (fieldValue[i] == 0)
						return DEFAULT_VALUE;
					else {
						return ""+fieldValue[i];
					}
				} else {
					return ""+fieldValue[i];
				}
			}
		}
		return null;
	}
	
	public void setRawValue(String name, long value) {
		if (layout == null) { 
			// This sat is not setup in the measurement store
			Log.println("FoxId: " + id + " is not setup in the measurement store.  Can not create pass measurement");
			return;
		}
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				fieldValue[i] = value;
		}
	}

	public void setLOS() {
		Date losDate = Calendar.getInstance().getTime();  
		//FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		//String dt = FramePart.fileDateFormat.format(losDate);
		setRawValue(LOS, losDate.getTime());
	}

	public void setTCA(Long dateMills) {
		Date dt = new Date(dateMills);
		//FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		//String dateTime = FramePart.fileDateFormat.format(dt);
		setRawValue(TCA, dt.getTime());
	}

	

	/**
	 * Load this framePart from a file, which has been opened by a calling method.  The string tokenizer contains a 
	 * set of tokens that represent the raw values to be loaded into the fields.
	 * The header has already been loaded by the calling routine.
	 * @param st
	 */
	protected void load(StringTokenizer st) {

		try {
			for (int i=0; i < layout.NUMBER_OF_FIELDS; i++)
				fieldValue[i] = Long.valueOf(st.nextToken());
		} catch (NoSuchElementException e) {
			// we are done and can finish
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.println("ERROR: Too many fields:  Could not load measurement " + this.id + " " + getRawValue(AOS));
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  Could not load measurement " + this.id + " " + getRawValue(AOS));
		}
	}
	
	/**
	 * Load the pre 1.04 format and convert the data in place
	 * @param st
	 */
	protected void load103(StringTokenizer st) {

		try {
			// the first 3 fields are dates that need to be converted
			
			try {
				loadDate(st,0);
				loadDate(st,1);
				loadDate(st,2);
			} catch (ParseException e) {
				e.printStackTrace(Log.getWriter());
			}
			for (int i=3; i < layout.NUMBER_OF_FIELDS-1; i++) {
				String tok = st.nextToken();
				if (tok != null && !tok.equalsIgnoreCase("null"))
					fieldValue[i] = Long.valueOf(tok);
				else
					fieldValue[i] = 0;
			}
			// lastly get the number of decodes and screen out the values greater than 900 which were caused by a bug
			String tok = st.nextToken();
			long decodes = 0;
			if (tok != null && !tok.equalsIgnoreCase("null"))
				decodes = Long.valueOf(tok);
			if (decodes > 900) decodes = 0;
			fieldValue[layout.NUMBER_OF_FIELDS-1] = decodes;
			
		} catch (NoSuchElementException e) {
			// we are done and can finish
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.println("ERROR: Too many fields:  Could not load measurement " + this.id + " " + getRawValue(AOS));
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  Could not load measurement " + this.id + " " + getRawValue(AOS));
		}
	}

	private void loadDate(StringTokenizer st, int i) throws ParseException {
		String dt1 = st.nextToken();
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date1 = FramePart.fileDateFormat.parse(dt1);
		fieldValue[i] = date1.getTime();

	}
	
	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * We need to override the default method in Measurement, otherwise we output the double array vs the longs
	 * 
	 * @return
	 */
	public String toFile() {
		String s = new String();
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String captureDate = FramePart.fileDateFormat.format(date);
		s = s + captureDate + "," + id + "," + reset + "," + uptime + "," + type + ",";
		for (int i=0; i < layout.NUMBER_OF_FIELDS-1; i++)
			s = s + fieldValue[i] + ",";
		s = s + fieldValue[layout.NUMBER_OF_FIELDS-1];
		return s;
	}
}
