package telemetry.conversion;

import java.util.Date;
import java.util.TimeZone;

import common.Config;
import common.Spacecraft;
import telemetry.FramePart;

public class ConversionTimestamp extends Conversion {

	String resetFieldName;
	String uptimeFieldName;
	
	public ConversionTimestamp(String name, Spacecraft fox) {
		super(name, fox);
		/*
		 * Try to parse the TIMESTAMP formatting keyword.  If so, we expect two fields names after it
		 * Those are used to form a reset / uptime pair which is displayed as is or converted to UTC
		 * 
		 */
		String stem9 = "";

		if (name.length() >=9) {
			stem9 = name.substring(0, 9); // first 9 characters to check for TIMESTAMP
			if (stem9.equalsIgnoreCase(Conversion.TIMESTAMP)) {
				String index1 = name.substring(9); // all characters after the stem
				String[] values = index1.split("\\s+"); // split on whitespace
				if (values.length < 3 || values[1] == null  || values[2] == null) {
					resetFieldName = null;
					uptimeFieldName = null;
				} else if (values.length == 3) { // space after the TIMESTAMP in position 0 then the two values
					resetFieldName = values[1];
					uptimeFieldName = values[2];	
				}	
			}
		}    
	}

	@Override
	public double calculate(double x) {
		return x;
	}

	@Override
	public String calculateString(double x) {
		throw new RuntimeException("Call conversion "+name+" with calculateTimeString method only!");
	}
	
	public String calculateTimeString(double x, FramePart framePart) {
		Integer reset = null;
		Long uptime = null;
		String s = "";
		
		if (framePart.hasFieldName(resetFieldName))
			reset = (int) framePart.getDoubleValue(resetFieldName, fox);
		else {
			s = "!Invalid Reset Field";
			return s;
		}
		if (framePart.hasFieldName(uptimeFieldName))
			uptime = (long) framePart.getDoubleValue(uptimeFieldName, fox);
		else {
			s = "!Invalid Uptime Field";
			return s;
		}
		
		if (reset != null && uptime != null) {
			if (Config.displayUTCtime) {
				Date date = fox.getUtcForReset(reset, uptime);
				if (date == null) {
					s = "T0 not set";
				} else {
					FramePart.reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
					s = FramePart.reportDateFormat.format(date);
				}
			} else
		    	s = "" + reset + " / " + uptime;	
		return s;
	}
	
		return null;
	}

}
