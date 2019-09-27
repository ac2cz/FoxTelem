package filter;

public class HilbertTransform {

	double coeffs[];
	
	double[] xv;  // This array holds the delayed values
	double gain = 1;
	int M; // The number of taps, the length of the filter
	
	public HilbertTransform(double sampleRate, int len) {
		init(sampleRate, len);
		M = len-1;
		xv = new double[len];
	}
	
	private void init(double sampleRate, int len) {
		double []tempCoeffs = new double[len];

		double sumofsquares = 0;
		
		for (int n=0; n < len; n++) {		
			if (n == len/2) {
				tempCoeffs[n] = 0;
			} else {
				tempCoeffs[n] = sampleRate / (Math.PI * (n-len/2) ) * ( 1 - Math.cos(Math.PI * (n-len/2) ));
			}
			sumofsquares += tempCoeffs[n]*tempCoeffs[n];
		}
		gain = Math.sqrt(sumofsquares);///tempCoeffs.length;
		System.out.println("Hilbert Transform GAIN: " + gain);
		// flip
		coeffs = new double[len];
		for (int i=0; i < tempCoeffs.length; i++) {
			coeffs[i] = tempCoeffs[tempCoeffs.length-i-1]/gain;
			//System.out.println(coeffs[i]);
		}

	}
	
	public double filter(double in) {
		double sum; 
		int i;
		for (i = 0; i < M; i++) 
			xv[i] = xv[i+1];
		xv[M] = in;
		sum = 0.0;
		for (i = 0; i <= M; i++) 
			sum += (coeffs[i] * xv[i]);
		return sum;
	}
}
