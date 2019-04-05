package measure;

import java.io.IOException;
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
    	// could probablly optimize this with binary search aglo but needs to be implement from scatch as not an exact match
    	// First check special case where we have value off the end
    	if (this.size() == 0) return -1;
    	if (resets > this.get(size()-1).reset) return size()-1;
    	if (resets == this.get(size()-1).reset && uptime > this.get(size()-1).uptime) return size()-1;
    	
    	for (int i=0; i<this.size(); i++) { 
    		Measurement f = this.get(i);
    		if (compare(f, id, uptime, resets) <= 0)
            	return i;
    	}
        return -1;
	    }

	protected int getNumberOfPayloadsBetweenTimestamps(int reset, long uptime, int toReset, long toUptime) throws IOException {
		int number = 0;
		int id = get(0).id; // id is the same for all records in this table
		
		int start = getNearestFrameIndex(id, uptime, reset);
		int end = getNearestFrameIndex(id, toUptime, toReset);
		if (end == -1) end = this.size();
		if (start < end)
			number = end - start;

		return number;
	}
	
	private int compare(Measurement p, int id, long uptime, int resets) {
    	if (resets == p.reset && uptime == p.uptime) 
    		return 0;
    	else if (resets < p.reset)
    		return -1;
    	else if (resets > p.reset)
    		return +1;
    	
    	else if (resets == p.reset) {	
    		if (uptime < p.uptime)
    			return -1;
    		if (uptime > p.uptime)
    			return +1;
    	} 
    	return +1;
    }
}
