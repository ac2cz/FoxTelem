package telemetry.legacyPayloads;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import common.Config;
import common.Spacecraft;
import decoder.FoxBitStream;
import decoder.FoxDecoder;
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
@Deprecated
public class PayloadRadExpData extends FramePart {
	
	public static final int MAX_PAYLOAD_RAD_SIZE = 58;
	
	public static final String EXP1_BOARD_NUM = "exp1BoardNum";
	public static final String WOD_RESETS = "WODTimestampReset";
	public static final String WOD_UPTIME = "WODTimestampUptime";
	public static final String WOD_CRC_ERROR = "WodCRCError";
	
	// Fox-1E Radiation constants
	public static final int ACTIVE = 3;
	public static final String STATE2 = "State2";
	public static final String STATE3 = "State3";
	public static final String STATE4 = "State4";
	
	
	public PayloadRadExpData(BitArrayLayout lay) {
		super(TYPE_RAD_EXP_DATA, lay);
//		MAX_BYTES = MAX_PAYLOAD_RAD_SIZE;
//		rawBits = new boolean[MAX_BYTES*8];
		
	}
	
	public PayloadRadExpData(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_RAD_EXP_DATA, date, st, lay);
//		MAX_BYTES = MAX_PAYLOAD_RAD_SIZE;
	}

	public PayloadRadExpData(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_RAD_EXP_DATA, lay);
	}
	
	protected void init() {

	}
	
	/*
	public void copyBitsTo58Fields() {
		resetBitPosition();
		for (int i =0; i< MAX_PAYLOAD_RAD_SIZE; i++)
			fieldValue[i] = nextbits(8);	
	}
*/
	
	
	/**
	 * Packet Telemetry never worked on the Vanderbilt boards, so this will always return true.
	 * 
	 * LEGACY COMMENTS BELOW:
	 * For Fox 1-A and Fox-1C If byte 21 onwards is zero then this is telemetry.  Zeros are not allowed in the packet format because of the
	 * COBS routine.  So if we find zeros, this is telemetry
	 * Fox-1D has Housekeeping telemetry.  We can never confuse this with packets.  So return true.
	 * Fox-1E does not use packets.  All payloads are telemetry
	 * Fox-1B fields 11 - 20 are zero (because there is no VUC Exp 1.  We also have not tested or used packets, so assume true
	 * @return
	 */
	public boolean isTelemetry() {
		if (id == Spacecraft.FOX1A || id == Spacecraft.FOX1C) {
			for (int i=21; i < 25; i++)
				if (fieldValue[i] != 0) return false;
		}

		return true;
	}

	/**
	 * We have two peculiarities for the Fox-1E radiation telemetry.  The VUC telemetry is the first 10 bytes, as it is for the other
	 * spacecraft.  The next ten to sixteen bytes are then the telemetry for the active experiment.  It is not in chunks like Fox-1B.
	 * However, the layout file has the telemetry layout for each experiment in chunks anyway, so we can pick which layout we want without 
	 * having lots of additional layout files.  We use the State of the experiments to determine which telemetry
	 * layout to use.  The layouts start at fixed positions, so we offset by an appropriate amount and then artificially put the telemetry bytes
	 * into that part of the RadiationTelemetry layout.  It is then converted and formatted according to the layout for that section.
	 *
	 * @param radTelem
	 */
	protected void calcFox1ETelemetry(RadiationTelemetry radTelem) {
		for (int k=0; k<10; k++) { // add the first 10 bytes 
			radTelem.addNext8Bits(fieldValue[k]);
		}
		radTelem.copyBitsToFields();
		int offset=0;  
		int length=0; // Default is that we display nothing
		if (radTelem.getRawValue(STATE2) == ACTIVE) { // LEPF is 2
			offset=0;
			length = 16;
		} else if (radTelem.getRawValue(STATE3) == ACTIVE) {// LEP is 3
			offset=16;
			length = 10;
		} else if (radTelem.getRawValue(STATE4) == ACTIVE) { // REM is 4
			offset=26;
			length = 16;
		} 
		// Pretend there is a gap, so that the layout works like Fox-1B
		for (int k=0; k<offset; k++) { 
			radTelem.addNext8Bits(0);
		}
		// Now flow the rest of the data in, 
		for (int k=10+offset; k<10+offset+length; k++) { 
			radTelem.addNext8Bits(fieldValue[k-offset]);
		}
		radTelem.copyBitsToFields();
		
		// Now we copy the extra Fox Fields at the end, but we put them directly in the fields.  Fox computer is little endian, but the data so far
		// was big endian.  We could remember that and convert each part correctly, or we can leverage the fact that the extra Fox Fields we already
		// converted correctly in the core radiation record.
		// Note that subsequently calling copyBitsToFields will eradicate this copy, so we add a BLOCK COPY BITS boolean
		radTelem.blockCopyBits = true;
		copyFieldValue(EXP1_BOARD_NUM, radTelem);
		copyFieldValue(WOD_RESETS, radTelem);
		copyFieldValue(WOD_UPTIME, radTelem);
		copyFieldValue(WOD_CRC_ERROR, radTelem);
	}
	
	private void copyFieldValue(String field, RadiationTelemetry radTelem) {
		int pos = radTelem.layout.getPositionByName(field);
		int val = getRawValue(field);
		if (pos != BitArrayLayout.ERROR_POSITION && val != BitArrayLayout.ERROR_POSITION) radTelem.fieldValue[pos] = val;
	}
	
	/**
	 * Calculate the telemetry and return it
	 * @return
	 */
	public RadiationTelemetry calculateTelemetryPalyoad() {
		//if (isTelemetry()) {
		Spacecraft fox = Config.satManager.getSpacecraft(id);
		if (fox.hasExperiment(Spacecraft.EXP_VANDERBILT_REM) && fox.hasExperiment(Spacecraft.EXP_VANDERBILT_LEPF)) {
			// We cheat because the layout has each experiment 10 bytes appart but the 10 comes in the same 10 bytes.  This is in contrast to 1A and 1B
			RadiationTelemetry radTelem = new RadiationTelemetry(resets, uptime, Config.satManager.getLayoutByName(id, Spacecraft.RAD2_LAYOUT));
			calcFox1ETelemetry(radTelem);
			return radTelem;
		} else {
			RadiationTelemetry radTelem = new RadiationTelemetry(resets, uptime, Config.satManager.getLayoutByName(id, Spacecraft.RAD2_LAYOUT));
			for (int k=0; k<radTelem.getMaxBytes(); k++) { 
				radTelem.addNext8Bits(fieldValue[k]);
			}
			return radTelem;
		}
	}

	/**
	 * Calculate the telemetry and return it
	 * @return
	 
	public PayloadHERCIHousekeeping calculateHerciTelemetryPalyoad() {
		//if (isTelemetry()) {
		PayloadHERCIHousekeeping radTelem = new PayloadHERCIHousekeeping(resets, uptime, Config.satManager.getRadTelemLayout(id));
			for (int k=0; k<RadiationTelemetry.MAX_RAD_TELEM_BYTES; k++) { 
				radTelem.addNext8Bits(fieldValue[k]);
			}
			return radTelem;
		//}
		//return null;
	}
    */
	
	/**
	 * We have bytes in big endian order, so we need to add the bits in a way
	 * that makes sense when we retrieve them sequentially
	 * So we add the msb first.  Then when 12 bits pulled in a row it will make sense.
	 * Note that if we pull a subset of 8 bits, then we have to be careful of the order.
	 * @param b
	 */
	public void DATAaddNext8Bits(byte b) {
		for (int i=0; i<8; i++) {
			if ((b >> i & 1) == 1) 
				rawBits[7-i+numberBytesAdded*8] = true;
			else 
				rawBits[7-i+numberBytesAdded*8] = false;
		}
		numberBytesAdded++;	
	}

	/**
	 * Return the next n bits of the raw bit array, converted into an integer
	 * We get them sequentially, with the msb first, so they just go into the 
	 * array in order
	 * @param n
	 * @return
	*/
	protected int DATAnextbits(int n ) {
		int field = 0;
		
		boolean[] b = new boolean[n];
		for (int i=0; i < n; i++) {
			b[i] = rawBits[bitPosition+i];
			
		}
		bitPosition = bitPosition + n;
		field = FoxBitStream.binToInt(b);
		return field;
		
	}

	
	public boolean hasData() {
		copyBitsToFields();
		for (int i =0; i< MAX_PAYLOAD_RAD_SIZE; i++) {
			if (fieldValue[i] != 0) return true;
		}
		return false;
	}
	
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "RADIATION EXPERIMENT DATA:\n";
		for (int i =0; i< MAX_PAYLOAD_RAD_SIZE; i++) {
			s = s + FoxDecoder.hex(fieldValue[i]) + " ";
			// Print 8 bytes in a row
			if ((i+1)%8 == 0) s = s + "\n";
		}
		return s;
	}

	public String toStringField() {
		copyBitsToFields();
		String s = new String();
		s = s + "RADIATION EXPERIMENT DATA:\n";
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i]+"\n";
		
		}
		return s;
	}

	
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		s = s + reportDate + "," + id + "," + resets + "," + uptime + "," + type + ",";
		for (int i=0; i < fieldValue.length-1; i++) {
			s = s + FoxDecoder.dec(fieldValue[i]) + ",";
			//s = s + FoxDecoder.hex(fieldValue[i]) + ",";
		}
		// add the final field with no comma delimiter
		s = s + FoxDecoder.dec(fieldValue[fieldValue.length-1]);
		//s = s + FoxDecoder.hex(fieldValue[fieldValue.length-1]);
		return s;
	}
	
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
