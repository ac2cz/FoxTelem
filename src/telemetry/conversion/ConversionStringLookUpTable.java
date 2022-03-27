package telemetry.conversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import common.Log;
import common.Spacecraft;
import telemetry.LayoutLoadException;

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
 *
 * A table of values that can be used to interpolate a final reading
 * We store the reference values in a file and read on startup.  The values are read into a Map sorted
 * by the key.
 *
 * To look up a String we find the nearest key value by iterating over the map, then return the closest String to the value
 * 
 *
 */
public class ConversionStringLookUpTable extends Conversion{
	public static final String ERROR = "UNK";
	
	protected Map<Integer, String> table = new LinkedHashMap<Integer, String>();

	/**
	 * Called for a static table where the values are not loaded from a file
	 */
	public ConversionStringLookUpTable(String name, Spacecraft fox) {
		super(name, fox);
	}
	
	/**
	 * Load the lookup table from a file
	 * @param fileName
	 * @throws LayoutLoadException 
	 * @throws FileNotFoundException 
	 */
	public ConversionStringLookUpTable(String name, String fileName, Spacecraft fox) throws FileNotFoundException, LayoutLoadException {
		super(name, fox);
		load(fileName);
	}
		
	/**
	 * Lookup the string in the table and return it.  Return an error value if not found
	 * @param lookUpkKey
	 * @return
	 */
	public String calculateString(double x) {
		int key = (int) Math.round(x);
		String value = table.get(key);
		if (value == null) {
			String s = String.format("%2.1f", x);
			return s;
		} else
			return value;
		
	}
	
	/**
	 * If this is called in a raw calculation then just return the value.  A string lookup has no effect.
	 */
	public double calculate(double x) {
		return x;
	}
	
	/**
	 * The table should be in order, but in case it is not, and given it is short, we iterate over it and get the max key value
	 * @return
	 */
	public int getMaxKey() {
		int max = -9999;
		Entry<Integer, String> pairs = null;
		Iterator<Entry<Integer, String>> it = table.entrySet().iterator();
		while (it.hasNext()) {
			pairs = it.next();
			int key = (Integer)pairs.getKey();
			if (key > max)
				max = key;
		}
		return max;
	}

	public int getMinKey() {
		int min = 9999;
		Entry<Integer, String> pairs = null;
		Iterator<Entry<Integer, String>> it = table.entrySet().iterator();
		while (it.hasNext()) {
			pairs = it.next();
			int key = (Integer)pairs.getKey();
			if (key < min)
				min = key;
		}
		return min;
	}
	
	protected void load(String fileName) throws FileNotFoundException, LayoutLoadException {

		String line;
		fileName = "spacecraft" +File.separator + fileName;
		//File aFile = new File(fileName);
		
		Log.println("Loading lookup table: "+ fileName);
		BufferedReader dis = new BufferedReader(new FileReader(fileName));
		try {
			
			while ((line = dis.readLine()) != null) {
				if (line != null) {
					StringTokenizer st = new StringTokenizer(line, ",");

					int key = Integer.valueOf(st.nextToken()).intValue();
					table.put(key, st.nextToken().trim());
				}
			}
			dis.close();
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());

		} catch (NumberFormatException n) {
			Log.errorDialog("NUMBER FORMAT EXCEPTION", "Loading Lookup table " + fileName + " " + n.getMessage());
			n.printStackTrace(Log.getWriter());

		} catch (NoSuchElementException n) {
			Log.errorDialog("Missing Field in Lookup table File", "Halted loading " + fileName);
			n.printStackTrace(Log.getWriter());
		}

	}
	
	public String toString() {
		String s = "";
		s = s + "Lookup Table: " + name;
		return s;
	}
}
