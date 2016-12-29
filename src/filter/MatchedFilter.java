
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
 * Matched filter to extract the bit shape of the high speed waveform
 * 
 * Normally filters use convolution, where the kernal is flipped left for right.  For a matched filter we
 * use correlation and the kernal is not flipped.  We use a square 1 bit as the template.
 * We pass in the frequency of the bits and then the filter is calcualted.
 * 
 *
 */
public class MatchedFilter extends Filter {

	//private static int NZEROS = 960;

	protected double[] xv;
	int M = 0; 
	double GAIN = 1;

	double xcoeffs[];

	public MatchedFilter(AudioFormat af, int size) {
		super(af, size);
	}


	public void init(double sampleRate, double freq, int len) {

		int pulseLength = (int) (sampleRate/freq);  // e.g. 48000/9600 = 5
		M = len; // needs to be twice the pulse length
		xcoeffs = new double[M+1]; 
		xv = new double[M+1];
		double sumofsquares = 0;
		
		// We have a filter length M+1 with a pulse in the middle length pulseLength.
		// So we have (M+1-len/2) at either end
		
		for (int i=0; i <= M; i++) {
			if (i >= (M+1-pulseLength)/2 && i < M+1-(M+1-pulseLength)/2) {
				xcoeffs[i] = 0.5;
			} else {
				xcoeffs[i] = 0;
			}
			sumofsquares += xcoeffs[i]*xcoeffs[i];
			
		}
		GAIN = Math.sqrt(sumofsquares)/xcoeffs.length;
		Log.println("Matched Filter GAIN: " + GAIN);
		super.init((double)sampleRate, (double)freq, len);
		name = "MatchedFilter ("+cutoffFreq + " "+ xcoeffs.length+ " stereo:"+stereo+ " Rate: " + SAMPLERATE+")";
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

