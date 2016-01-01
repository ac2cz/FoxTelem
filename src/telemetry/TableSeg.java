package telemetry;
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
	}	
	
	public boolean isLoaded() { return loaded; }
	public void setLoaded(boolean t) { loaded = t; }
	
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
		return fromReset+","+fromUptime+","+records;
	}
}
