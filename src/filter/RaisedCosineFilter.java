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
 * Raised cosine filter to preserve the bit shape
 * 
 *
 */
public class RaisedCosineFilter extends Filter {
	
	//private static int NZEROS = 960;

	protected double[] xv;

	int M = 0; // length sets the roll off.  Should be approx 4/Bandwidth as fraction of Sampling freq
	double GAIN = 1;
	double alpha = 0.5;
	double Fc = 0d; // Will be set to cutoffFreq/SAMPLE_RATE; 
	
	double xcoeffs[];
		
	public RaisedCosineFilter(AudioFormat af, int size) {
		super(af, size);
	}
	
	
	public void init(double sampleRate, double freq, int len) {

		M = len;
		xcoeffs = new double[M+1];
		Fc = freq/sampleRate;
		xv = new double[M+1];
		
		double sumofsquares = 0;
		int limit = (int)(0.5 / (alpha * Fc));
		for (int i=0; i <= M; i++) {
			double sinc = (Math.sin(2 * Math.PI * Fc * (i - M/2)))/ (i - M/2);
			double cos = Math.cos(alpha * Math.PI * Fc * (i - M/2)) / ( 1 - (Math.pow((2 * alpha * Fc * (i - M/2)),2)));
			
			if (i == M/2) {
				xcoeffs[i] = 2 * Math.PI * Fc * cos;
			} else {
				xcoeffs[i] = sinc * cos;
			}
			
			// Care because ( 1 - ( 2 * Math.pow((alpha * Fc * (i - M/2)),2))) is zero for 
			if ((i-M/2) == limit || (i-M/2) == -limit) {
				xcoeffs[i] = 0.25 * Math.PI * sinc;
			} 
			
			sumofsquares += xcoeffs[i]*xcoeffs[i];
//			System.out.println(xcoeffs[i]);
		}
		GAIN = Math.sqrt(sumofsquares)/xcoeffs.length;
		Log.println("Raised Cosine Filter GAIN: " + GAIN);
		super.init((double)sampleRate, (double)freq, len);
		name = "RaisedCosine ("+cutoffFreq + " "+ xcoeffs.length+ " stereo:"+stereo+ " Rate: " + SAMPLERATE+")";
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
