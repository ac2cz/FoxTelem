package telemetry;

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
public class SortedFramePartArrayList extends SortedArrayList<FramePart> {

    public SortedFramePartArrayList(int i) {
		super(i);
	}

	public boolean hasFrame(int id, long uptime, int resets) {
    	if (getFrameIndex(id, uptime, resets) != -1)
            	return true;
        return false;
    }

	
	public boolean hasFrame(int id, long uptime, int resets, int type) {
    	if (getFrameIndex(id, uptime, resets, type) != -1)
            	return true;
        return false;
    }
    
    public int getFrameIndex(int id, long uptime, int resets) {
    	for (int i=0; i<this.size(); i++) { 
    		FramePart f = this.get(i);
            if (f.id == id && f.uptime == uptime && f.resets == resets)
            	return i;
    	}
        return -1;
    }

    public int getFrameIndex(int id, long uptime, int resets, int type) {
    	for (int i=0; i<this.size(); i++) { 
    		FramePart f = this.get(i);
            if (f.id == id && f.uptime == uptime && f.resets == resets && f.type == type)
            	return i;
    	}
        return -1;
    }

    public FramePart getFrame(int id, long uptime, int resets) {
    	int i = getNearestFrameIndex(id, uptime, resets);
    	if (i != -1)
            return get(i);
        return null;
    }
    
    public FramePart getFrame(int id, long uptime, int resets, int type) {
    	int i = getNearestFrameIndex(id, uptime, resets, type);
    	if (i != -1)
            return get(i);
        return null;
    }
    
    public int getNearestFrameIndex(int id, long uptime, int resets) {
    	// start searching from the beginning where reset and uptime should be the lowest
    	for (int i=0; i<this.size(); i++) { 
    		FramePart f = this.get(i);
            if (compare(f, id, uptime, resets, 0) <= 0)
            	return i;
    	}
        return -1;
    }
    
    public int getNearestFrameIndex(int id, long uptime, int resets, int type) {
    	// start searching from the beginning where reset and uptime should be the lowest
    	for (int i=0; i<this.size(); i++) { 
    		FramePart f = this.get(i);
    		if (compare(f, id, uptime, resets, type) <= 0)
            	return i;
    	}
        return -1;
    }

    private int compare(FramePart p, int id, long uptime, int resets, int type) {
    	if (resets == p.resets && uptime == p.uptime && type == p.type) 
    		return 0;
    	else if (resets < p.resets)
    		return -1;
    	else if (resets > p.resets)
    		return +1;
    	else if (resets == p.resets && uptime == p.uptime) {
    		if (type < p.type)
    			return -1;
    		if (type > p.type)
    			return +1;
    	} else if (resets == p.resets) {	
    		if (uptime < p.uptime)
    			return -1;
    		if (uptime > p.uptime)
    			return +1;
    	} 
    	return +1;
    }
}
