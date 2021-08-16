package telemetry.payloads;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

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
public class PayloadMinValues extends FramePart {

	//public static final int NUMBER_OF_FIELDS = 38;

	public PayloadMinValues(BitArrayLayout lay) {
		super(TYPE_MIN_VALUES, lay);
	}
	
	public PayloadMinValues(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, TYPE_MIN_VALUES, date, st, lay);
	}

	public PayloadMinValues(ResultSet r, BitArrayLayout lay) throws SQLException {
		super(r, TYPE_MIN_VALUES, lay);
	}
	
	protected void init() {

	}

	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = new String();
		s = s + "MINIMUM VALUES:\n";
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
