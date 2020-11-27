package telemetry;

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
 * To look up a value we find the nearest key value by iterating over the map, then return the interpolated value
 * 
 *
 */
public class ConversionLookUpTable extends Conversion{
	
	protected Map<Integer, Double> table = new LinkedHashMap<Integer, Double>();

	/**
	 * Called for a static table where the values are not loaded from a file
	 */
	public ConversionLookUpTable(String name) {
		super(name);
	}
	
	/**
	 * Load the lookup table from a file
	 * @param fileName
	 * @throws LayoutLoadException 
	 * @throws FileNotFoundException 
	 */
	public ConversionLookUpTable(String name, String fileName) throws FileNotFoundException, LayoutLoadException {
		super(name);
		load(fileName);
	}
	
	/**
	 * Standard algorithm for straight line interpolation
	 * @param x - the key we want to find the value for
	 * @param x0 - lower key
	 * @param x1 - higher key
	 * @param y0
	 * @param y1
	 * @return
	 */
	private double linearInterpolation(double x, double x0, double x1, double y0, double y1) {
		double y = y0 + (y1 - y0) * ((x - x0)/(x1 - x0));
		return y;
	}
		
	/**
	 * Look up a value from the table.  We start from the top of the table.  The keys are in ascending order
	 * with the lowest value first.  So we search for the first key that is greater than the key we are 
	 * looking up.  Then we run a linear interpolation between the previous key and the key that is greater than
	 * our lookup key.
	 * 
	 * If the lookup key is less than the first key in the table then we must extrapolate
	 * If the lookup key is greater than the last key in the table we must extrapolate past the end
	 * @param lookUpkKey
	 * @return
	 */
	public double calculate(double x) {
		int lookUpKey = (int)x;
		Iterator<Entry<Integer, Double>> it = table.entrySet().iterator();
		double lastValue = 0;
		int lastKey = 0;
		double prevValue = 0;
		int prevKey = 0;
		Entry<Integer, Double> pairs = null;
		int key;
		boolean firstKey = true;
		double value = FoxFramePart.ERROR_VALUE;
	    while (it.hasNext()) {
	        pairs = it.next();
	        key = (Integer)pairs.getKey();
	        value = (Double)pairs.getValue(); 
	        if (firstKey) {
	        	firstKey = false;
	        	if (lookUpKey < key) {
	        		
	        		// store the values
	        		lastKey = key;
	    	        lastValue = value;
	        		// get the next set
	    	        pairs = it.next();
	    	        key = (Integer)pairs.getKey();
	    	        value = (Double)pairs.getValue();
	    	        // and extrapolate from there
	        		value = linearInterpolation((double)lookUpKey, lastKey, (double)pairs.getKey(), lastValue, (Double)pairs.getValue());
	        		return value;
	        	}
	        }
	        if (lookUpKey == key) 
	        	return value;
	        else if (lookUpKey < key) {
	        	// Solve for the value using linear interpolation
	  //      	System.out.println("Interpolating: " + (double)lookUpkKey +" "+ lastKey + " " + (double)pairs.getKey() + " " + lastValue + " " + (Double)pairs.getValue());
	        	value = linearInterpolation((double)lookUpKey, lastKey, (double)pairs.getKey(), lastValue, (Double)pairs.getValue());
	        	return value;
	        }
	        prevKey = lastKey;
	        prevValue = lastValue;
	        lastKey = key;
	        lastValue = value;
	    }
	    // try to extrapolate off the end
	    if (pairs != null)
	    	value = linearInterpolation((double)lookUpKey, prevKey, lastKey, prevValue, lastValue);
	    return value;
	}
	
	public int reverseLookup(double lookUpKey) {
		Iterator<Entry<Integer, Double>> it = table.entrySet().iterator();
		int lastValue = 0;
		double lastKey = 0;
		int prevValue = 0;
		double prevKey = 0;
		Entry<Integer, Double> pairs = null;
		double key;
		boolean firstKey = true;
		int value = (int)FoxFramePart.ERROR_VALUE;
	    while (it.hasNext()) {
	        pairs = it.next();
	        value = (Integer)pairs.getKey();
	        key = (Double)pairs.getValue(); 
	        if (firstKey) {
	        	firstKey = false;
	        	if (lookUpKey < key) {
	        		
	        		// store the values
	        		lastKey = key;
	    	        lastValue = value;
	        		// get the next set
	    	        pairs = it.next();
	    	        value = (Integer)pairs.getKey();
	    	        key = (Double)pairs.getValue();
	    	        // and extrapolate from there
	        		value = (int) linearInterpolation((double)lookUpKey, lastKey, (double)pairs.getKey(), lastValue, (Double)pairs.getValue());
	        		return value;
	        	}
	        }
	        if (lookUpKey == key) 
	        	return value;
	        else if (lookUpKey < key) {
	        	// Solve for the value using linear interpolation
	  //      	System.out.println("Interpolating: " + (double)lookUpkKey +" "+ lastKey + " " + (double)pairs.getKey() + " " + lastValue + " " + (Double)pairs.getValue());
	        	value = (int) linearInterpolation((double)lookUpKey, lastKey, (double)pairs.getKey(), lastValue, (Double)pairs.getValue());
	        	return value;
	        }
	        prevKey = lastKey;
	        prevValue = lastValue;
	        lastKey = key;
	        lastValue = value;
	    }
	    // try to extrapolate off the end
	    if (pairs != null)
	    	value = (int) linearInterpolation((double)lookUpKey, prevKey, lastKey, prevValue, lastValue);
	    return value;
	}
	
	/**
	 * Return the passed value as a String
	 * @param lookUpkKey
	 * @return
	 */
	public String calculateString(double x) {
		String s = String.format("%2.1f", x);
		return s;
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
					double value = Double.valueOf(st.nextToken());
					table.put(key, value);
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
