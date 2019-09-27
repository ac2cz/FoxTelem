package filter;

import common.Log;

public class PolyPhaseFilter {

	double xcoeffs[];
	
	//double[] xv;  // This array holds the delayed values
	double gain = 1;
	int M; // The number of taps, the length of the filter
	double alpha = 0.5;
	double Fc = 0d; // Will be set to cutoffFreq/SAMPLE_RATE; 
	int R = 0;
	SubFilter[] subFilters;
	
	/**
	 * Taps LEN must be an integer number of decimationRate
	 * @param sampleRate
	 * @param freq
	 * @param decimationRate
	 * @param len
	 */
	public PolyPhaseFilter(double sampleRate, double freq, int decimationRate, int len) {
		R = decimationRate;
		init(sampleRate, freq, len);
		M = len-1;
	}
	
	private void init(double sampleRate, double freq, int len) {

		M = len;
		xcoeffs = new double[M+1];
		Fc = freq/sampleRate;
//		xv = new double[M+1];
		
		double sumofsquares = 0;
		double[] tempCoeffs = new double[M+1];
		int limit = (int)(0.5 / (alpha * Fc));
		for (int i=0; i <= M; i++) {
			double sinc = (Math.sin(2 * Math.PI * Fc * (i - M/2)))/ (i - M/2);
			double cos = Math.cos(alpha * Math.PI * Fc * (i - M/2)) / ( 1 - (Math.pow((2 * alpha * Fc * (i - M/2)),2)));
			
			if (i == M/2) {
				tempCoeffs[i] = 2 * Math.PI * Fc * cos;
			} else {
				tempCoeffs[i] = sinc * cos;
			}
			
			// Care because ( 1 - ( 2 * Math.pow((alpha * Fc * (i - M/2)),2))) is zero for 
			if ((i-M/2) == limit || (i-M/2) == -limit) {
				tempCoeffs[i] = 0.25 * Math.PI * sinc;
			} 
			
			sumofsquares += tempCoeffs[i]*tempCoeffs[i];
//			System.out.println(xcoeffs[i]);
		}
		gain = Math.sqrt(sumofsquares);
		Log.println("Raised Cosine PolyPhase Filter GAIN: " + gain);
		for (int i=0; i < tempCoeffs.length; i++) {
			xcoeffs[i] = tempCoeffs[tempCoeffs.length-i-1]/gain;
			//System.out.println(coeffs[i]);
		}
		subFilters = new SubFilter[R];
		int P = R - 1; // position of the polyphase switch

		for (int j=0; j < R; j ++) {
			double[] taps = new double[M/R];			
			for (int i = 0; i < M/R; i++) {
				taps[i] = (xcoeffs[P + i*R]);
				//System.out.print("Tap: " + (P + i*R) + " ");
			}
			subFilters[j] = new SubFilter(taps);
			//System.out.println("");
			P = P - 1;
			if (P < 0)
				P = R - 1;
		}
	}

	/**
	 * Each time we get an input we rotate though the coefficients based on the decimate rate R and the position P.
	 * @param in
	 * @return
	 */
	public double filterDouble(double[] in) {
		if (subFilters == null) return 0;
		double sum; 
		int j = in.length-1;
		int i;
		sum = 0.0;
		for (i = 0; i < in.length; i++) 
			sum += subFilters[i].filter(in[j--]);
//			sum += subFilters[i].filter(in[i]);
		return sum;
	}
	
	public static void main(String[] args) {
		PolyPhaseFilter f = new PolyPhaseFilter(48000, 12000, 4, 12);
		double[] in = {1,2,3,4};
		for (int j=0; j<12; j++) {
			f.filterDouble(in);
		}
	}

	private class SubFilter {
		int M; // length of the sub filter
		double[] coeff;
		double[] xv;  // This array holds the delayed values

		SubFilter (double[] taps) {
			coeff = taps;
			M = taps.length-1;
			xv = new double[M+1];
		}

		public double filter(double in) {
			double sum; 
			int i;
			for (i = 0; i < M; i++) 
				xv[i] = xv[i+1];
			xv[M] = in;
			sum = 0.0;
			for (i = 0; i <= M; i++) 
				sum += (coeff[i] * xv[i]);
			return sum;
		}
		
	}
}
