package measure;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import telemetry.FramePart;
import common.Config;
import common.Log;

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
public class RtMeasurement extends Measurement {

	String captureDate;
		
	
	
	public static final String BIT_SNR = "BIT_SNR";
	public static final String RF_SNR = "RF_SNR";
	public static final String RF_POWER = "RF_POWER";
	public static final String CARRIER_FREQ = "CARRIER_FREQ";
	public static final String AZ = "AZ";
	public static final String EL = "EL";
	public static final String STATION_CONFIG = "STATION_CONFIG";
	public static final String ERRORS = "ERRORS";
	public static final String ERASURES = "ERASURES";
		
	/**
	 * Load a past measurement from disk
	 * @param id
	 * @param date
	 * @param st
	 */
	public RtMeasurement(int foxid, String date, int reset, long uptime, int type, StringTokenizer st) {
		id = foxid;
		captureDate = date;
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.date = new Date();
		try {
			this.date = FramePart.fileDateFormat.parse(captureDate);
		} catch (ParseException e) {
			e.printStackTrace(Log.getWriter());
		}
		this.reset = reset;
		this.uptime = uptime;
		this.type = type;
		layout = Config.satManager.getMeasurementLayout(foxid);
		fieldValue = new double[layout.NUMBER_OF_FIELDS];
		load(st);
	}
	
	/**
	 * Create a new measurement
	 * @param id
	 * @param type
	 */
	public RtMeasurement(int foxid, int reset, long uptime, int type) {
		id = foxid;
		date = Calendar.getInstance().getTime();  
		FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		captureDate = FramePart.fileDateFormat.format(date);
		this.reset = reset;
		this.uptime = uptime;
		this.type = type;
		layout = Config.satManager.getMeasurementLayout(foxid);
		fieldValue = new double[layout.NUMBER_OF_FIELDS];
	}
	
	public double getRawValue(String name) {
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				return fieldValue[i];
		}
		return -1;
	}
	
	public void setRawValue(String name, double value) {
		for (int i=0; i < layout.fieldName.length; i++) {
			if (name.equalsIgnoreCase(layout.fieldName[i]))
				fieldValue[i] = value;
		}
	}
	
	public void setBitSNR(double d) {
		setRawValue(BIT_SNR,d);
	}

	public void setRfSNR(double d) {
		setRawValue(RF_SNR,d);
	}
	
	public void setRfPower(double d) {
		setRawValue(RF_POWER,d);
	}

	public void setCarrierFrequency(long d) {
		setRawValue(CARRIER_FREQ,d);
	}
	
	public void setAzimuth(double d) {
		setRawValue(AZ,d);
	}

	public void setElevation(double d) {
		setRawValue(EL,d);
	}

	public void setErrors(int d) {
		setRawValue(ERRORS,d);
	}
	
	public void setErasures(int d) {
		setRawValue(ERASURES,d);
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
				fieldValue[i] = Double.valueOf(st.nextToken()).doubleValue();
		} catch (NoSuchElementException e) {
			// we are done and can finish
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.println("ERROR: Too many fields:  Could not load measurement " + this.id + " " + this.captureDate + this.type);
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  Could not load measurement " + this.id + " " + this.captureDate+ this.type);
		}
	}
	
	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile() {
		String s = new String();
		s = s + captureDate + "," + id + "," + reset + "," + uptime + "," + type + ",";
		for (int i=0; i < layout.NUMBER_OF_FIELDS-1; i++)
			s = s + fieldValue[i] + ",";
		s = s + fieldValue[layout.NUMBER_OF_FIELDS-1];
		return s;
	}


}
