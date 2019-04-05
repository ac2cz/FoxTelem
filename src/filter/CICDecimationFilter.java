package filter;

import javax.sound.sampled.AudioFormat;

/**
 * A two state CIC Decimation filter with a Compensation filter
 * @author chris
 *
 */
public class CICDecimationFilter extends Filter {

	protected double[] xv;
	double GAIN = 1;
	double previousOutput[];
	
	// Compensation filer order and taps from python CIC Compensation filter calculator
	int M = 127;
	double[] xcoeffs = {8.593779e-06,
			5.697627e-06,
			-1.750550e-05,
			-1.935780e-05,
			2.490024e-05,
			4.450960e-05,
			-2.421413e-05,
			-8.184734e-05,
			6.330681e-06,
			1.277673e-04,
			3.897493e-05,
			-1.726743e-04,
			-1.204248e-04,
			2.001757e-04,
			2.416643e-04,
			-1.877985e-04,
			-3.972685e-04,
			1.097069e-04,
			5.691903e-04,
			5.842869e-05,
			-7.247108e-04,
			-3.327294e-04,
			8.169643e-04,
			7.134259e-04,
			-7.888770e-04,
			-1.177538e-03,
			5.808677e-04,
			1.673587e-03,
			-1.419772e-04,
			-2.120056e-03,
			-5.566946e-04,
			2.409036e-03,
			1.508089e-03,
			-2.415840e-03,
			-2.657183e-03,
			2.014441e-03,
			3.893361e-03,
			-1.097525e-03,
			-5.049328e-03,
			-4.010691e-04,
			5.907848e-03,
			2.484479e-03,
			-6.216324e-03,
			-5.074727e-03,
			5.707522e-03,
			8.001118e-03,
			-4.122839e-03,
			-1.099652e-02,
			1.232179e-03,
			1.370046e-02,
			3.158929e-03,
			-1.566334e-02,
			-9.231583e-03,
			1.633741e-02,
			1.724286e-02,
			-1.501631e-02,
			-2.779653e-02,
			1.060636e-02,
			4.276591e-02,
			-7.540386e-04,
			-6.984689e-02,
			-2.265479e-02,
			1.813897e-01,
			3.822780e-01,
			3.822780e-01,
			1.813897e-01,
			-2.265479e-02,
			-6.984689e-02,
			-7.540386e-04,
			4.276591e-02,
			1.060636e-02,
			-2.779653e-02,
			-1.501631e-02,
			1.724286e-02,
			1.633741e-02,
			-9.231583e-03,
			-1.566334e-02,
			3.158929e-03,
			1.370046e-02,
			1.232179e-03,
			-1.099652e-02,
			-4.122839e-03,
			8.001118e-03,
			5.707522e-03,
			-5.074727e-03,
			-6.216324e-03,
			2.484479e-03,
			5.907848e-03,
			-4.010691e-04,
			-5.049328e-03,
			-1.097525e-03,
			3.893361e-03,
			2.014441e-03,
			-2.657183e-03,
			-2.415840e-03,
			1.508089e-03,
			2.409036e-03,
			-5.566946e-04,
			-2.120056e-03,
			-1.419772e-04,
			1.673587e-03,
			5.808677e-04,
			-1.177538e-03,
			-7.888770e-04,
			7.134259e-04,
			8.169643e-04,
			-3.327294e-04,
			-7.247108e-04,
			5.842869e-05,
			5.691903e-04,
			1.097069e-04,
			-3.972685e-04,
			-1.877985e-04,
			2.416643e-04,
			2.001757e-04,
			-1.204248e-04,
			-1.726743e-04,
			3.897493e-05,
			1.277673e-04,
			6.330681e-06,
			-8.184734e-05,
			-2.421413e-05,
			4.450960e-05,
			2.490024e-05,
			-1.935780e-05,
			-1.750550e-05,
			5.697627e-06,
			8.593779e-06
};
	
	int R = 5; // RTL decimation is 240/48 = 5
	int D = 10; // Delay needs to be multiple of the Decimation Rate
	int Q = 1; // Order is 1 for now
	
	double[][] delayLine;
	int sampleNumber = 0;
	
	public CICDecimationFilter(AudioFormat af, int size) {
		super(af, size);
	}
	
    
	public void init(double sampleRate, double freq, int len) {
		Q=10;
		previousOutput = new double[Q];

		R = 4;
		D = R*10;
		delayLine = new double[Q][];
		for (int i=0; i<Q; i++)
			delayLine = new double[i][D/R];
		xv = new double[M+1];
		setDecimationFactor(R);
	}
	
	private double integrate(double currentInput, int stage) {
		double currentOutput = ( currentInput + previousOutput[stage] );
		previousOutput[stage] = currentOutput;
		return currentOutput;
	}
	
	private double comb(double currentOutput, int stage) {
		delayLine[stage][0] = currentOutput;
		currentOutput = currentOutput + delayLine[stage][delayLine[0].length-1];
		for (int i=0; i < delayLine[0].length-1; i++)
			delayLine[stage][i+1] = delayLine[stage][i];

		return currentOutput;
	}
	
	public double filter( double currentInput ) {
		double currentOutput = 0;
		int q=0;

		currentOutput = currentInput;
		// Integrator
		for (q=0; q<Q; q++)
			currentOutput = integrate(currentOutput, q );
		
		// Decimate
		if (calculateNow()) {

			// Comb
			for (q=0; q<Q; q++)
				currentOutput = comb(currentOutput, q);
			// Compensate Filter
			currentOutput = filterDouble(currentOutput);
			
			return currentOutput;
		} else {
			return 0;
		}
	}
	
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] getKernal() {
		// TODO Auto-generated method stub
		return null;
	}
}
