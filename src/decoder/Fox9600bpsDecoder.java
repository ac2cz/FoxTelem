package decoder;

import common.Log;
import filter.AGCFilter;
import filter.MatchedFilter;
import filter.RaisedCosineFilter;

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
 * This is the High Speed Decoder
 * 
 */
public class Fox9600bpsDecoder extends Decoder {
	public static final int HIGH_SPEED_BITS_PER_SECOND = 9600;
	
	public Fox9600bpsDecoder(SourceAudio as, int chan) {
		super("High Speed", as, chan);
	}

	public void init() {
		Log.println("Initializing HIGH SPEED: ");
		setHighSpeedParameters();
		super.init();
		//filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE /bytesPerSample));
		//filter.init(currentSampleRate, 0, 0);
		
		//filter = new MatchedFilter(audioSource.audioFormat, (BUFFER_SIZE /bytesPerSample));
		//filter.init(currentSampleRate, 9600, 10);
		
		// Use Raised cosine as a matched filter.  It should be the same length as the pulse, so we make
		// it length 10, for a 1 and a 0.  The 1 will be centered.
		filter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE /bytesPerSample);
		filter.init(currentSampleRate, 9600, 10);

	}
	
	private void setHighSpeedParameters() {
		//decodedFrame = new HighSpeedFrame();
		bitStream = new HighSpeedBitStream(this);
		BITS_PER_SECOND = HIGH_SPEED_BITS_PER_SECOND;
		bucketSize = currentSampleRate / BITS_PER_SECOND;
		SAMPLE_WIDTH = 1;
		SAMPLE_WINDOW_LENGTH = 100; 
		CLOCK_TOLERANCE = 10;
		CLOCK_REOVERY_ZERO_THRESHOLD = 10;

	}
	
}
