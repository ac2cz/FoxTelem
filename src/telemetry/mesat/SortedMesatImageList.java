

package telemetry.mesat;

import telemetry.SortedArrayList;
import telemetry.legacyPayloads.CameraJpeg;

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
 * Sorted Array List to store Jpeg telemetry records.   
 * 
 */
@SuppressWarnings("serial")
public class SortedMesatImageList extends SortedArrayList<MesatImage> {


	public SortedMesatImageList(int i) {
		super(i);
	}

	public boolean hasFrame(int id, long uptime, int resets, int image_index, int image_channel) {
		if (getPictureIndex(id, uptime, resets, image_index, image_channel) != -1)
			return true;
		return false;
	}

	public int getPictureIndex(int id, long uptime, int resets, int image_index, int image_channel) {
		for (int i=0; i<this.size(); i++) { 
			MesatImage f = this.get(i);
			if (f.id == id && f.epoch == resets && f.image_index == image_index && f.image_channel == image_channel) {
				//if (f.fromUptime - uptime < 200)
					return i;
			}
		}
		return -1;
	}
	
	public int getNearestFrameIndex(int id, long uptime, int epoch) {
    	// start searching from the beginning where reset and uptime should be the lowest
		// We want the first record where the reset is equal to or 
    	for (int i=0; i<this.size(); i++) { 
    		MesatImage f = this.get(i);
            if (f.id == id && f.epoch >= epoch && f.fromUptime >= uptime)
            	return i;
    	}
        return -1;
    }

}


