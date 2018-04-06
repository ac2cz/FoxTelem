package filter;

public class Delay {

	double[] xv;  // This array holds the delayed values
	int M; // The number of taps, the length of the filter
	
	public Delay(int len) {
		xv = new double[len+1];
		M = len;
	}
	
	public double filter(double in) {
		for (int i = 0; i < M; i++) 
			xv[i] = xv[i+1];
		xv[M] = in;
		return xv[0];
	}
}
