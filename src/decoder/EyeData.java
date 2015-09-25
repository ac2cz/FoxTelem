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
 * Bit Measurements that can be dsiplayed on an eye diagram
 * 
 *
 */
public class EyeData extends DataMeasure {
	
	protected boolean eyeDataFresh = false;
    int[][] eyeData = null;
	
    int SAMPLE_WINDOW_LENGTH = 0;
    int bucketSize = 0;
    public double bitSNR;
    
    public int lastErasureCount; // number of erasures in the last frame
    public int lastErrorsCount; // number of errors in the last frame
    
    public static final int HIGH = 0;
    public static final int LOW = 1;
    
    protected long AVERAGE_PERIOD = 100; // 1000 = 1 sec average time
   
    public EyeData(int l, int b) {
    	MEASURES = 2;
    	init();
    	
    	SAMPLE_WINDOW_LENGTH = l;
    	bucketSize = b;
		eyeData = new int[SAMPLE_WINDOW_LENGTH][];
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			eyeData[i] = new int[bucketSize];
		}

    }
    
    public void calcAverages() {
    	if (readyToAverage()) {
			double noise = sd[LOW] + sd[HIGH];
			double signal = avg[HIGH] - avg[LOW];
			if (signal != 0 && noise != 0) {
				bitSNR = (signal/noise);
			}
			reset();
			
		}
    }
    
    public boolean isFresh() { return eyeDataFresh; }
    public void setFreshData(boolean b) { eyeDataFresh = b; }
    public void setData(int window, int j, int value) { 
    	eyeData[window][j] = value;
    }
    public int[][] getData() { return eyeData; }
    
    public void setHigh(int value) {
    	setValue(HIGH, value);
    }

    public void setLow(int value) {
    	setValue(LOW,value);
    }

}
