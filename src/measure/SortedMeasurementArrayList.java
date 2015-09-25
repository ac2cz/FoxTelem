package measure;

import telemetry.SortedArrayList;

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
@SuppressWarnings("serial")
public class SortedMeasurementArrayList extends SortedArrayList<Measurement> {

	 public SortedMeasurementArrayList(int i) {
		super(i);
	}

	public int getNearestFrameIndex(int id, long uptime, int resets) {
	    	// start searching from the beginning where reset and uptime should be the lowest
	    	for (int i=0; i<this.size(); i++) { 
	    		Measurement f = this.get(i);
	            if (f.id == id && f.reset >= resets && f.uptime >= uptime)
	            	return i;
	    	}
	        return -1;
	    }

}
