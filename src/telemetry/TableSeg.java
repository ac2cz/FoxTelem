package telemetry;

import common.Config;

/**
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
 * This class stores the list of files that support a table
 * 
 */
public class TableSeg implements Comparable<TableSeg> {
	int fromReset = 0;
	long fromUptime = 0;
	int records = 0;
	private boolean loaded = false;
	String fileName;
	long lastAccess;
	//private static final int STALE_PERIOD = 60*1000; // Keep the stale period short. In milliseconds
	
	/**
	 * Create a new segment and give it a filename
	 * @param r
	 * @param u
	 * @param f
	 */
	public TableSeg(int r, long u, String f) {
		fromReset = r;
		fromUptime = u;
		records = 0;
		fileName = f+ "_" + r + "_"+ u +".log";
		accessed();
	}
	
	/**
	 * Create a segment when loaded from the index file
	 * @param r
	 * @param u
	 * @param f
	 * @param rec
	 */
	TableSeg(int r, long u, String f, int rec) {
		fromReset = r;
		fromUptime = u;
		records = rec;
		fileName = f;
		accessed();
	}	
	
	public void accessed() {
		lastAccess = System.nanoTime()/1000000;
	}
	
	public boolean isLoaded() {
		accessed();
		return loaded; 
	}
	public boolean isStale() {
		if (loaded) {
			long now = System.nanoTime()/1000000;
			long elapsed = now - lastAccess;
			if (elapsed > Config.timeUntilTableSegOffloaded)
				return true;
		}
		return false;
	}
	public void setLoaded(boolean t) { 
		accessed();
		loaded = t; }
	
	public String toFile() {
		String s = "";
		s = s + fromReset + ",";
		s = s + fromUptime + ",";
		s = s + records + ",";
		s = s + fileName + ",";
		return s;
	}
	
	public int compareTo(TableSeg p) {
		if (fromReset == p.fromReset && fromUptime == p.fromUptime) 
			return 0;
		else if (fromReset < p.fromReset)
			return -1;
		else if (fromReset > p.fromReset)
			return +1;
		else if (fromReset == p.fromReset)	
			if (fromUptime < p.fromUptime)
				return -1;
		return +1;
	}
	
	public String toString() {
		return fileName + ": "+fromReset+","+fromUptime+","+records;
	}
}
