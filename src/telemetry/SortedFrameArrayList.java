package telemetry;

import telemetry.frames.Frame;

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
 *
 * Sorted Array List to store telemetry records
 *
 */
@SuppressWarnings("serial")
public class SortedFrameArrayList extends SortedArrayList<Frame> {

    public SortedFrameArrayList(int i) {
		super(i);
	}

	public boolean hasFrame(Frame f) {
    	for (int i=0; i<this.size(); i++) { 
    		if (this.get(i).compareTo(f) == 0)
    			return true;
    	}
        return false;
    }
    
}
