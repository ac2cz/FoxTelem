package decoder;

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
 * A data measurement container.  Stores measurements and performs running averaging and standard deviation calculations
 *
 *
 */
public abstract class DataMeasure implements Runnable {
	private long lastAverageTime = 0;
	protected long AVERAGE_PERIOD = 500; // 1000 = 1 sec average time
	
	double[] sum;
	double[] sumOfSquares;
	double[] avg;
	double[] sd;
	int[] numOf;
	boolean running = true;

	protected int MEASURES = 0;
	
	protected void init() {
    	sum = new double[MEASURES];
    	sumOfSquares = new double[MEASURES];
    	avg = new double[MEASURES];
    	sd = new double[MEASURES];
    	numOf = new int[MEASURES];
	}
	
	
	public void stopProcessing() {
		running = false;
	}
	
	protected boolean readyToAverage() {
		long now = System.nanoTime()/1000000; // get time in ms
		long averageTime = now - lastAverageTime;
		
		if (averageTime > AVERAGE_PERIOD) {
			lastAverageTime = now;
			return true;
		}
		return false;
	}

	protected void runAverage() {
		for (int i=0; i < MEASURES; i++) {
			double a = sum[i] / numOf[i];
			if (!Double.isNaN(a) && Double.isFinite(a)) {
				avg[i] = a;
				sd[i] = Math.sqrt( ( (sumOfSquares[i] - ( (sum[i]*sum[i]) / numOf[i]) )/ ((double)numOf[i]-1)) );		
			}
			
			// Now store the value as the average and reset the number
			if (!Double.isNaN(avg[i]) && Double.isFinite(avg[i])) {
				sum[i] = avg[i];
				numOf[i] = 1;
				sumOfSquares[i] = avg[i]*avg[i]; 
				//Log.println("SUM:" + sum[i] + "Sq:" + sumOfSquares[i]);
			}
		}
	}

	public double getAvg(int key) { 
		return avg[key]; 
	}

	public double getStandardDeviation(int key) { 
		return sd[key]; 
	}

	
	protected void setValue(int key, double value) {
    	numOf[key]++;
		sum[key] += value;
		sumOfSquares[key] += value*value;

	}
	
    public void reset() {
    	readyToAverage(); // resets the timer
    	for (int i=0; i < MEASURES; i++) {
    		sum[i] = 0; //sumOfHighs / numOfHighs;  
    		sumOfSquares[i] = 0; //sumOfHighSquares / numOfHighs;  
    		numOf[i] = 0;
    	}
    	
    }

}
