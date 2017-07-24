package decoder.KiwiSat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import decoder.FoxDecoder;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;

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
public class KiwiSatTelemetryPayload extends FoxFramePart {

	//public static final int NUMBER_OF_FIELDS = 38;

	public KiwiSatTelemetryPayload(BitArrayLayout lay) {
		super(lay);
	}
	
	public KiwiSatTelemetryPayload(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);
		MAX_BYTES = KiwiSatFrame.MAX_PAYLOAD_SIZE;
	}

	public KiwiSatTelemetryPayload(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, lay);
	}
	
	protected void init() {
		type = 0;  // PROBABLLY NEED TO SET THIS TO A SUIABLE TYPE.  TYPE 0 IS DEBUG	
		MAX_BYTES = KiwiSatFrame.MAX_PAYLOAD_SIZE;
		fieldValue = new int[layout.fieldName.length];
	}

	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "KIWI SAT TELEMETRY:\n";
		for (int i=0; i < layout.fieldName.length; i++) {
			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
			if ((i+1)%6 == 0) s = s + "\n";
		}
		return s;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
}
