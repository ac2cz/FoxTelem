package device;

import java.io.IOException;

import decoder.SourceUSB;
import device.rtl.RTL2832TunerController.SampleRate;

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
 * This class wraps the HID Device and holds the commands that we pass to the Funcube Dongle.
 * 
 */
public abstract class TunerController {
	
	public long MIN_FREQ = 1500;
	public long MAX_FREQ = 2050000000;
	public int SAMPLE_RATE = 192000;
	
	public String name;

	public TunerController(String name, long min, long max) {
		this.name = name;
		MIN_FREQ = min;
		MAX_FREQ = max;
	}

	public abstract int setFrequency(long freq) throws DeviceException;
	
    public abstract void cleanup() throws IOException, DeviceException;
    
    public abstract boolean isConnected();

	public int getCurrentSampleRate() throws DeviceException {
		// TODO Auto-generated method stub
		return SAMPLE_RATE;
	}
	
	public abstract void setSampleRate(SampleRate sampleRate) throws DeviceException; // only for RTL

	public long getMinFreq() { return MIN_FREQ; }
	public long getMaxFreq() { return MAX_FREQ; }

	public abstract DevicePanel getDevicePanel() throws IOException, DeviceException;
	
	public void setUsbSource(SourceUSB audioSource) {
		// TODO Auto-generated method stub
		
	}
	
}
