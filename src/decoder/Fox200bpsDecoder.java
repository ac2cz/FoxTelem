package decoder;

import filter.MatchedFilter;
import filter.RaisedCosineFilter;
import filter.WindowedSincFilter;
import gui.FilterPanel;
import common.Config;
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
 * This is the DUV Decoder
 */
public class Fox200bpsDecoder extends FoxDecoder {

	public static final int SLOW_SPEED_BITS_PER_SECOND = 200;
	public static final int WORD_LENGTH = 10;
	public static final int SYNC_WORD_LENGTH = 10;
	private int useFilterNumber;
	
	public Fox200bpsDecoder(SourceAudio as, int chan) {
		super("DUV", as, chan, null);
		//Log.println("STARTED filter len: " + Config.filterLength);
	}
	
	public void init() {
		Log.println("Initializing SLOW SPEED: ");
		setSlowSpeedParameters();
		super.init();
		useFilterNumber = Config.useFilterNumber;
		//Log.println("INIT filter len: " + Config.filterLength);
		updateFilter();
	}
	
	/**
	 * Called if any of the filter params have changed
	 */
	private void updateFilter() {
		// Get the params that were set by the GUI
		FilterPanel.checkFilterParams();
		//Log.println("UPDATE filter len: " + Config.filterLength);
		currentFilterLength = Config.filterLength;
		currentFilterFreq = Config.filterFrequency;
		useFilterNumber = Config.useFilterNumber;
		if (useFilterNumber == FilterPanel.RAISED_COSINE) {
			filter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE);
			filter.init(currentSampleRate, Config.filterFrequency, Config.filterLength);
		} else if (useFilterNumber == FilterPanel.WINDOWED_SINC) {
			filter = new WindowedSincFilter(audioSource.audioFormat, BUFFER_SIZE);
			filter.init(currentSampleRate, Config.filterFrequency, Config.filterLength);
		} else if (useFilterNumber == FilterPanel.MATCHED) {
			filter = new MatchedFilter(audioSource.audioFormat, BUFFER_SIZE);
			// Experiments have determined that the optimal filter is the WS length 480
			//filter = new WindowedSincFilter(audioSource.audioFormat, BUFFER_SIZE);
			filter.init(currentSampleRate, Config.filterFrequency, bucketSize*2);
			Config.filterLength = bucketSize*2;
			currentFilterLength = Config.filterLength;
		}
		
		//double[] coef = filter.getKernal();
		//int i=0;
		//for (double d: coef)
		//	System.out.println(i++ + "," + d);
	}
	
	private void setSlowSpeedParameters() {
		//decodedFrame = new SlowSpeedFrame();
		bitStream = new SlowSpeedBitStream(this, WORD_LENGTH, SYNC_WORD_LENGTH, SLOW_SPEED_BITS_PER_SECOND);
		BITS_PER_SECOND = SLOW_SPEED_BITS_PER_SECOND;
		SAMPLE_WINDOW_LENGTH = 70; 
		bucketSize = currentSampleRate / BITS_PER_SECOND;
		SAMPLE_WIDTH = bucketSize*SAMPLE_WIDTH_PERCENT/100;
		if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
		CLOCK_TOLERANCE = 10;
		CLOCK_REOVERY_ZERO_THRESHOLD = 20;
	}
	
	protected void resetWindowData() {
		super.resetWindowData();
		if ((currentFilterLength != Config.filterLength) ||
				(currentFilterFreq != Config.filterFrequency) ||
				(useFilterNumber != Config.useFilterNumber)) {

			updateFilter();
			Log.println("REBUILDING FILTER: " + filter.toString());
		}

	}
}
