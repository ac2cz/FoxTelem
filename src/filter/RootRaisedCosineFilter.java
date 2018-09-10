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
public class RootRaisedCosineFilter extends Filter {
	
	//private static int NZEROS = 960;

	protected double[] xv;

	int M = 0; // length sets the roll off.  Should be approx 4/Bandwidth as fraction of Sampling freq
	double GAIN = 1;
	double alpha = 0.5;
	double Fc = 0d; // Will be set to cutoffFreq/SAMPLE_RATE; 
	
	double xcoeffs[];
		
	public RootRaisedCosineFilter(AudioFormat af, int size) {
		super(af, size);
	}
	
	
	public void init(double sampleRate, double freq, int len) {

		M = len;
		xcoeffs = new double[M+1];
		Fc = freq/sampleRate;
		xv = new double[M+1];
					
		double Ts = 1/(freq); // reciprocal of the symbol rate in Hz
		double sum = 0;

		xcoeffs = new double[M+1];

		for (int i=0; i <= M; i++) {
			double t = (i - M/2) / sampleRate;

			double sin = Math.sin((1 - alpha) * Math.PI * t / Ts);
			double cos = (4 * alpha * t / Ts) * Math.cos((1 + alpha) * Math.PI * t / Ts);
			double det = Math.PI * t / Ts * (1 - Math.pow(4 * alpha * t / Ts,2));
			//double blackman = 0.42 - 0.5 * Math.cos((2 * Math.PI * i) / M) + 0.08 * Math.cos((4 * Math.PI * i) / M);
			if (t == 0) {
				//System.out.println("ZERO");
				xcoeffs[i] = 1/Math.sqrt(Ts) * (1 - alpha + 4*alpha/Math.PI);
			} else 	if (t == Ts/(4*alpha) || t == -Ts/(4*alpha)) {
				//System.out.println("4alpha");
				xcoeffs[i] = alpha/Math.sqrt(2 * Ts) * ((1+2/Math.PI)* Math.sin(Math.PI/(4*alpha)) + (1-2/Math.PI)*Math.cos(Math.PI/(4*alpha))); 
			} else {
				xcoeffs[i] = 1/Math.sqrt(Ts) * ((sin + cos)/det);
			}
			//xcoeffs[i] = xcoeffs[i] * blackman;
			sum += xcoeffs[i];//*xcoeffs[i];
		}

		for (int i=0; i<=M; i++) {
			xcoeffs[i] = xcoeffs[i]/Math.sqrt(sum);
			System.out.println(xcoeffs[i]);
		}
		GAIN = Math.sqrt(sum);
		Log.println("Root Raised Cosine Filter GAIN: " + GAIN);
		super.init((double)sampleRate, (double)freq, len);
		name = "RootRaisedCosine ("+cutoffFreq + " "+ xcoeffs.length+ " stereo:"+stereo+ " Rate: " + SAMPLERATE+")";
	}
	
	@Override
	public double filterDouble(double in) {
		double sum; 
		int i;
		for (i = 0; i < M; i++) 
			xv[i] = xv[i+1];
		xv[M] = in;
		if (calculateNow()) {
		sum = 0.0;
		for (i = 0; i <= M; i++) 
			sum += (xcoeffs[i] * xv[i]);
		return sum;
		}
		return 0;  // return zero for all the values we don't need when decimating.  Ignored after decimation
	}

	@Override
	protected int getFilterLength() {
		return xcoeffs.length;
	}
	
	public double[] getKernal() {
		return xcoeffs;
	}

}
