package filter;

import javax.sound.sampled.AudioFormat;

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
 * Windowed Sinc Filter
 * 
 */
public class WindowedSincFilter extends Filter {
	
	//private static int NZEROS = 960;

	protected double[] xv;

	int M = 0; // length sets the roll off.  Should be approx 4/Bandwidth as fraction of Sampling freq
	double GAIN = 1;
	double Fc = 0d; 
	double xcoeffs[] = null;
		
	public WindowedSincFilter(AudioFormat af, int size) {
		super(af, size);	
	}
	
	
	public void init(double sampleRate, double freq, int len) {
		
		M = len;
		xcoeffs = new double[M+1];
		Fc = freq/sampleRate;
		double sumofsquares = 0;
		for (int i=0; i <= M; i++) {
			double sinc = (GAIN * Math.sin(2 * Math.PI * Fc * (i - M/2)))/ (i - M/2);
			double blackman = 0.42 - 0.5 * Math.cos((2 * Math.PI * i) / M) + 0.08 * Math.cos((4 * Math.PI * i) / M);
			if (i == M/2) {
				xcoeffs[i] = 2 * Math.PI * Fc * GAIN * blackman;
			} else {
				xcoeffs[i] = sinc * blackman;
			}
			sumofsquares += xcoeffs[i]*xcoeffs[i];
		}
		xv = new double[M+1];
		GAIN = Math.sqrt(sumofsquares)/xcoeffs.length;
		Log.println("Windowed Sinc Filter GAIN: " + GAIN);
		super.init((double)sampleRate, (double)freq, len);
		name = "WindowedSinc Cutoff:"+cutoffFreq + " Taps:"+ xcoeffs.length + " stereo:"+stereo + " Rate: " + sampleRate;
	}
	
	@Override
	public double filterDouble(double in) {
		double sum; 
		int i;
		for (i = 0; i < M; i++) 
			xv[i] = xv[i+1];
		xv[M] = in * GAIN;
		sum = 0.0;
		for (i = 0; i <= M; i++) 
			sum += (xcoeffs[i] * xv[i]);
		return sum;
	}

	@Override
	protected int getFilterLength() {
		return xcoeffs.length;
	}
	
	public double[] getKernal() {
		return xcoeffs;
	}


}
