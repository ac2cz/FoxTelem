package telemetry;

import java.util.StringTokenizer;

import decoder.Decoder;

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
public class PayloadHERCIhighSpeed extends FramePart {

	public static final int MAX_PAYLOAD_SIZE = 868;
	
	public PayloadHERCIhighSpeed() {
		super(new BitArrayLayout());
	}

	/**
	 * Load this payload from disk
	 * @param id
	 * @param resets
	 * @param uptime
	 * @param date
	 * @param st
	 */
	public PayloadHERCIhighSpeed(int id, int resets, long uptime, String date, StringTokenizer st) {
		super(id, resets, uptime, date, st, new BitArrayLayout());
		MAX_BYTES = MAX_PAYLOAD_SIZE;
	}

	@Override
	protected void init() {
		MAX_BYTES = MAX_PAYLOAD_SIZE;
		fieldValue = new int[MAX_PAYLOAD_SIZE];
		type = FramePart.TYPE_HERCI_HIGH_SPEED_DATA;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	public void copyBitsToFields() {
		resetBitPosition();
		for (int i =0; i< MAX_PAYLOAD_SIZE; i++)
			fieldValue[i] = nextbits(8);	
	}

	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "HERCI EXPERIMENT HIGH SPEED DATA: " + MAX_BYTES + " bytes\n";
		for (int i =0; i< MAX_BYTES; i++) {
			s = s + Decoder.hex(fieldValue[i]) + " ";
			// Print 32 bytes in a row
			if ((i+1)%32 == 0) s = s + "\n";
		}
		return s;
	}
	
	public String toFile() {
		copyBitsToFields();
		String s = new String();
		s = s + captureDate + "," + id + "," + resets + "," + uptime + "," + type + ",";
		for (int i=0; i < fieldValue.length-1; i++) {
			//s = s + Decoder.dec(fieldValue[i]) + ",";
			s = s + fieldValue[i] + ",";
		}
		// add the final field with no comma delimiter
		//s = s + Decoder.dec(fieldValue[fieldValue.length-1]);
		s = s + fieldValue[fieldValue.length-1];
		return s;
	}

}
