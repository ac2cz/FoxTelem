package decoder;

import common.Log;
import filter.AGCFilter;

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
	
	public Fox9600bpsDecoder(SourceAudio as) {
		super(as);
	}

	public void init() {
		Log.println("Initializing HIGH SPEED: ");
		setHighSpeedParameters();
		super.init();
		filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE /bytesPerSample));
		filter.init(currentSampleRate, 0, 0);

	}
	
	private void setHighSpeedParameters() {
		//decodedFrame = new HighSpeedFrame();
		bitStream = new HighSpeedBitStream();
		BITS_PER_SECOND = HIGH_SPEED_BITS_PER_SECOND;
		bucketSize = currentSampleRate / BITS_PER_SECOND;
		SAMPLE_WIDTH = 1;
		SAMPLE_WINDOW_LENGTH = 100; 
		CLOCK_TOLERANCE = 10;
		CLOCK_REOVERY_ZERO_THRESHOLD = 10;

	}
	
}