package telemetry;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import common.Log;

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
 * This is a single line in a downloaded image
 *
 */
public class PictureScanLine implements Comparable<PictureScanLine> {
	int pictureCounter = 0; // 8 bits unsigned - picture count indicator
	int scanLineNumber = 0; // 6 bits unsigned 0x00 to 0x3B, where 0x00 is the top line
	int scanLineLength = 0; // 10 bits unsigned 0x001 to 0x3FF - count of bytes in the scan line
	int[] scanLineData = null; // variable length line of data
	String fileName = ""; // fileName used when this is saved to disk (only needed by the server)
	
	// Store the information from the frame this came from so that we can tell unique picture lines apart
	int id;
	Long uptime;
	int resets;
	String captureDate;

	/**
	 * Create a scanLine from the data we have just decoded from a High Speed Camera Payload
	 * Typically the scanLineData is an empty array ready to be populated
	 * @param pc
	 * @param sln
	 * @param sll
	 * @param sld
	 */
	public PictureScanLine(String date, int pc, int sln, int sll, int[] sld) {
		pictureCounter = pc;
		scanLineNumber = sln;
		scanLineLength = sll;
		scanLineData = sld;
		captureDate = date;
	}
	

	public PictureScanLine(int id, int resets, long uptime, String captureDate, int pc, int sln, int sll, StringTokenizer st) {
		this.id = id;
		this.resets = resets;
		this.uptime = uptime;
		this.captureDate = captureDate;
		pictureCounter = pc;
		scanLineNumber = sln;
		scanLineLength = sll;
		load(st);
	}

	public void captureHeaderInfo(int id, long uptime, int resets) {
		this.id = id;
		this.uptime = uptime;
		this.resets = resets;
	}

	public String getFileName() {
		fileName = "Fox" + id + "psl" + resets + "_" + uptime + "_" + pictureCounter +"_" +scanLineNumber + ".log";
		return fileName;
	}
	
	public int compareTo(PictureScanLine p) {
		if (resets == p.resets && uptime == p.uptime && pictureCounter == p.pictureCounter 
				&& scanLineNumber == p.scanLineNumber) 
			return 0;
		else if (resets < p.resets)
			return +1;
		else if (resets > p.resets)
			return -1;
		else if (resets == p.resets)	
			if (uptime < p.uptime)
				return +1;
			else if (uptime > p.uptime)
				return -1;
			else if (uptime == p.uptime)
				if (pictureCounter < p.pictureCounter)
					return +1;
				else if (pictureCounter > p.pictureCounter)
					return -1;
				else if (pictureCounter == p.pictureCounter)
					if (scanLineNumber < p.scanLineNumber)
						return +1;
					else if (scanLineNumber > p.scanLineNumber)
						return -1;
							
		return -1;
	}

	public String toShortString() {
		String s = "";		
		s = s + pictureCounter + "," + scanLineNumber + "," + scanLineLength + ",";
		return s;
	}

	
	public String toString() {
		String s = "";
		
		s = s + captureDate + "," + id + "," + resets + "," + uptime + ",";
		s = s + pictureCounter + "," + scanLineNumber + "," + scanLineLength + ",";
		if (scanLineLength > 0) {
			for (int i=0; i < scanLineLength-1; i++)
				s = s + scanLineData[i] + ",";
			s = s + scanLineData[scanLineLength-1];
		}
		return s;
	}
	
	public void load(StringTokenizer st) {
		int i = 0;
		String s = null;
		try {
			while((s = st.nextToken()) != null) {
				scanLineData[i++] = Integer.valueOf(s).intValue();
			}
		} catch (NoSuchElementException e) {
			// we are done and can finish
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			Log.println("ERROR: Too many bytes:  Could not load picture line " + this.id + " " + this.resets + " " + this.uptime + " " 
					+ this.pictureCounter + " " + this.scanLineNumber);
		} catch (NumberFormatException n) {
			Log.println("ERROR: Invalid number:  Could not load picture line " + this.id + " " + this.resets + " " + this.uptime + " " 
					+ this.pictureCounter + " " + this.scanLineNumber);
		}

	}
	
	public static String getTableCreateStmt() {
		String s = new String();
		s = s + "(id int, resets int, uptime bigint, "
		 + "pictureCounter int, "
		 + "scanLineNumber int," 
		 + "scanLineLength int,"
		 + "fileName varchar(255)," 
		+ "date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,";
		s = s + "PRIMARY KEY (id, resets, uptime, pictureCounter))";
		return s;
	}
	
	public String getInsertStmt() {
		String s = new String();
		s = s + " (id, resets, uptime,\n";
		s = s + "pictureCounter,\n";
		s = s + "scanLineNumber,\n";
		s = s + "scanLineLength,\n";
		s = s + "fileName)\n";
		
		s = s + "values (" + this.id + ", " + resets + ", " + uptime +  ",\n";
		s = s + pictureCounter+",\n";
		s = s + scanLineNumber+",\n";
		s = s + scanLineLength+",\n";
		s = s + "'" + fileName+"')\n";
		return s;
	}
}
