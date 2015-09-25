package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
 * A class to track and report on the performance of the program.  Used for debugging only.
 *
 */
public class Performance {
	public static int bindCalls = 0;
	public static float bindAverage = 0; 
	public static int bindFrames = 0;
	
	private static boolean timing = false;
	
	static private HashMap<String, Integer> bindFreq = new HashMap<String, Integer>();
	static private ArrayList<PerfTimer> timers = new ArrayList<PerfTimer>(64);
	
	//static private int timerNumber = 0;
	
	public Performance() {
	}
	
	public static void setEnabled(boolean t) { timing = t; }
	
	public static void bind(String s) {
		if (!timing) return;
		bindCalls++;
		if (bindFreq.containsKey(s)) {
			int current = bindFreq.get(s);
			bindFreq.put(s, current+1);
		} else {
			bindFreq.put(s, 1);
		}
	}
	
	public static void bindEndFrame() {
		if (!timing) return;
		bindFrames++;
		bindAverage = bindCalls/bindFrames;
	}
	
	/**
	 * Start a performance timer with this name
	 * @param s
	 * @return
	 */
	public static void startTimer(String s) {
		if (!timing) return;
		PerfTimer pt = null;
		for (int i=0; i<timers.size(); i++) {
			if (timers.get(i).timer == s) {
				pt = timers.get(i);
				break;
			}
		}
		if (pt == null) {
			
			timers.add(new PerfTimer(s));
			
		} else {
			pt.resetStartTime();
		}
	}
	
	public static void endTimer(String s) {
		
		if (!timing) return;
		PerfTimer pt = null;
		for (int i=0; i<timers.size(); i++) {
			if (timers.get(i).timer == s) {
				pt = timers.get(i);
				break;
			}
		}
		if (pt == null) {
			Log.println("ERROR: Not a valid timer: " + s);
			return;
		}
		pt.updateTimer();	
	}
	
	public static void printResults() {
		if (!timing) return;
		long totalTime = 0;
		for (int i=0; i<timers.size(); i++) {
			totalTime = totalTime + timers.get(i).result;  
		}
		Log.println("Total time: " + totalTime);
		for (int i=0; i<timers.size(); i++) {
			String name = timers.get(i).timer;
			Long time = timers.get(i).result;
			double seconds = (double)time / 1000000000.0;
			double percent = (double)time/(double)totalTime;
			//String result = String.format("%s: %02d - %02d percent", name, seconds, (double)time/totalTime);
			
			Log.println(name + ": " + seconds + " - " + percent * 100);
		}
		if (bindFrames > 0) {
			Log.println("Binds: " + bindCalls);
			Log.println("Frames: " + bindFrames);
			Log.println("Binds Per Frame: " + bindAverage);
			for (Map.Entry<String, Integer> entry : bindFreq.entrySet()) {
	    	    String key = entry.getKey();
	    	    Integer v = entry.getValue();
	    	    Log.println(key + ": " + v/bindFrames + " per frame");
			}
		}
	}
}
