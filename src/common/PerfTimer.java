package common;

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
 */
public class PerfTimer {
	public String timer = "";
	public Long startTime = 0L;
	public Long result = 0L;

	PerfTimer(String s) {
		timer = s;
		resetStartTime();
	}
	
	public void resetStartTime() {
		startTime = System.nanoTime();
	}
	
	public void updateTimer() {
		long l = System.nanoTime();
		result = result + l - startTime;
	}
	
}
