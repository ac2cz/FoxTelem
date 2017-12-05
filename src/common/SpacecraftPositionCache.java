package common;

import java.util.HashMap;

import uk.me.g4dpz.satellite.SatPos;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2017 amsat.org
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
 * This class caches the position of the spacecraft so that we only have to
 * calculate it once.
 * 
 *
 */
public class SpacecraftPositionCache {
	HashMap<Long, SatPos> satPositions;
	int id; // The FoxId of the spacecraft
	
	SpacecraftPositionCache(int foxid) {
		satPositions = new HashMap<Long, SatPos>();
	}
	
	public SatPos getPosition(long time) {
		SatPos pos = satPositions.get(time);
		return pos;
	}
	
	public boolean storePosition(long time, SatPos pos) {
		satPositions.put(time, pos);
		return true;
	}
}
