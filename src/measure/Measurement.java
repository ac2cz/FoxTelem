package measure;

import java.util.Date;

import telemetry.BitArrayLayout;

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
public abstract class Measurement implements Comparable<Measurement> {

	public static final int INVALID_ERRORS = 999;

	Date date;
	public int id;
	int type;
	int reset; // the reset on the received frame when this value was measured
	long uptime; // the uptime on the received frame when this was measured

	public double[] fieldValue = null;
	protected BitArrayLayout layout;

	public int getReset() { return reset; }
	public long getUptime() { return uptime; }
	public Date getCaptureDate() { return date; }
	
	@Override
	public int compareTo(Measurement p) {
		if (date !=null && p.date != null) {
			return date.compareTo(p.date);
		}
		return -1;

	}

}
