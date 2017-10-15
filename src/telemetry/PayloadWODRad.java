package telemetry;

import java.util.StringTokenizer;

import org.joda.time.DateTime;

import common.Config;
import common.FoxSpacecraft;
import common.Spacecraft;
import decoder.FoxDecoder;
import predict.PositionCalcException;
import uk.me.g4dpz.satellite.SatPos;

public class PayloadWODRad extends PayloadRadExpData {
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";

	public PayloadWODRad(BitArrayLayout lay) {
		super(lay);
		// TODO Auto-generated constructor stub
	}

	public PayloadWODRad(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	
		if (satLatitude == NO_POSITION_DATA || satLatitude == NO_T0) {
			if (Config.foxTelemCalcsPosition)
				captureSatPosition();
		}
	}
	
	@Override
	protected void init() {
		type = TYPE_WOD_RAD;
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}

	@Override
	public void captureHeaderInfo(int id, long uptime, int resets) {
		copyBitsToFields();
		this.id = id;
		this.captureDate = fileDateStamp();
		if (satLatitude == NO_POSITION_DATA || satLatitude == NO_T0) {
			if (Config.foxTelemCalcsPosition)
				captureSatPosition();
		}
	}
	
	@Override
	public void copyBitsToFields() {
		super.copyBitsToFields();
		resets = getRawValue(WOD_RESETS);
		uptime = getRawValue(WOD_UPTIME);
	}
	
	public void captureSatPosition() {
		FoxSpacecraft sat = (FoxSpacecraft) Config.satManager.getSpacecraft(id);
		SatPos pos = null;
		// We need to construct a date for the historical time of this WOD record
		DateTime wodTime = sat.getUtcDateTimeForReset(getRawValue(WOD_RESETS), getRawValue(WOD_UPTIME));
		if (wodTime != null) {
			//DateTime timeNow = new DateTime(wodTime); 

			//capture the satellite position so we can visualize the WOD
			try {
				pos = sat.getSatellitePosition(wodTime);
			} catch (PositionCalcException e) {
				if (e.errorCode == FramePart.NO_TLE) {
					satLatitude = NO_TLE;
					satLongitude = NO_TLE;
					satAltitude = NO_TLE;
				}
			}	
			
		}
		if (satLatitude != NO_TLE)
			setSatPosition(pos);
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public WodRadiationTelemetry calculateTelemetryPalyoad() {
		WodRadiationTelemetry radTelem = new WodRadiationTelemetry(resets, uptime, Config.satManager.getLayoutByName(id, Spacecraft.WOD_RAD2_LAYOUT));
		for (int k=0; k<RadiationTelemetry.MAX_RAD_TELEM_BYTES; k++) { 
			radTelem.addNext8Bits(fieldValue[k]);
		}
		return radTelem;
		
	}

	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "WOD RADIATION EXPERIMENT DATA:\n";
		for (int i =0; i< PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; i++) {
			s = s + FoxDecoder.hex(fieldValue[i]) + " ";
			// Print 8 bytes in a row
			if ((i+1)%8 == 0) s = s + "\n";
		}
		for (int i=PayloadRadExpData.MAX_PAYLOAD_RAD_SIZE; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
		return s;
	}

	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		s = s + captureDate + "," + id + "," + resets + "," + uptime + "," +  type + "," + satLatitude + "," + satLongitude + "," + satAltitude + "," ;
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + FoxDecoder.dec(getRawValue(layout.fieldName[i])) + ",";
		}
		s = s + FoxDecoder.dec(getRawValue(layout.fieldName[layout.fieldName.length-1]));
		return s;
	}
	
	public String getInsertStmt() {
		copyBitsToFields();
		/*
		 * The server does not rely on the position data sent by ground stations.  It calculates the position based on known good keps
		 * and overwrites any position data sent by the station
		 */
		captureSatPosition();
		String s = new String();
		s = s + " (captureDate,  id, resets, uptime, type, satLatitude, satLongitude, satAltitude, \n";
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + layout.fieldName[i] + ",\n";
		}
		s = s + layout.fieldName[layout.fieldName.length-1] + ")\n";
		s = s + "values ('" + this.captureDate + "', " + this.id + ", " + this.resets + ", " + this.uptime + ", " + this.type + 
				", " + satLatitude + ", " + satLongitude + ", " + satAltitude + ",\n";
		for (int i=0; i < fieldValue.length-1; i++) {
			s = s + fieldValue[i] + ",\n";
		}
		s = s + fieldValue[fieldValue.length-1] + ")\n";
		return s;
	}
	
	/**
	 * Load this framePart from a file, which has been opened by a calling method.  The string tokenizer contains a 
	 * set of tokens that represent the raw values to be loaded into the fields.
	 * The framePart header has already been loaded by the calling routine, which had to work out the type first
	 * @param st
	 */
	protected void load(StringTokenizer st) {
		satLatitude = Double.valueOf(st.nextToken()).doubleValue();
		satLongitude = Double.valueOf(st.nextToken()).doubleValue();
		satAltitude = Double.valueOf(st.nextToken()).doubleValue();
		super.load(st);
	}
}
