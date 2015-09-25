package measure;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import common.Config;
import common.Log;
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

	public String[] fieldValue = null;

	public int endReset = 0;
	public long endUptime = 0;
	
	public static final String AOS = "AOS";
	public static final String LOS = "LOS";
	public static final String TCA = "TCA";
	public static final String TCA_FREQ = "TCA_FREQ";
	public static final String START_AZIMUTH = "START_AZIMUTH";
	public static final String END_AZIMUTH = "END_AZIMUTH";
	public static final String MAX_ELEVATION = "MAX_ELEVATION";
	public static final String TOTAL_PAYLOADS = "TOTAL_PAYLOADS";

	/**
	 * Load a past measurement from disk
	 * @param id
	 * @param date
	 * @param st
	 */
	public PassMeasurement(int foxid, String dt, int reset, long uptime, int type, StringTokenizer st) {
		id = foxid;
		this.type = type;

		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.date = new Date();
		try {
			this.date = FramePart.fileDateFormat.parse(dt);
		} catch (ParseException e) {
			e.printStackTrace(Log.getWriter());
		}
		layout = Config.satManager.getPassMeasurementLayout(foxid);
		fieldValue = new String[layout.NUMBER_OF_FIELDS];
		setRawValue(AOS, dt);
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
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String captureDate = FramePart.fileDateFormat.format(date);
		layout = Config.satManager.getPassMeasurementLayout(foxid);
		fieldValue = new String[layout.NUMBER_OF_FIELDS];

		setRawValue(AOS, captureDate);

	}

	public void setStartResetUptime(int reset, long uptime) {
		this.reset = reset;
		this.uptime = uptime;
	}
	public void setEndResetUptime(int reset, long uptime) {
		this.endReset = reset;
		this.endUptime = uptime;
	}
	public String getRawValue(String name) {
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				return fieldValue[i];
		}
		return null;
	}

	public void setRawValue(String name, String value) {
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				fieldValue[i] = value;
		}
	}

	public void setLOS() {
		Date losDate = Calendar.getInstance().getTime();  
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dt = FramePart.fileDateFormat.format(losDate);
		setRawValue(LOS, dt);
	}

	public void setTCA(Long dateMills) {
		Date dt = new Date(dateMills);
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String dateTime = FramePart.fileDateFormat.format(dt);
		setRawValue(TCA, dateTime);
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
				fieldValue[i] = st.nextToken();
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
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile() {
		String s = new String();
		s = s + getRawValue(AOS) + "," + id + "," + reset + "," + uptime + "," + type + ",";
		for (int i=0; i < layout.NUMBER_OF_FIELDS-1; i++)
			s = s + fieldValue[i] + ",";
		s = s + fieldValue[layout.NUMBER_OF_FIELDS-1];
		return s;
	}

}
