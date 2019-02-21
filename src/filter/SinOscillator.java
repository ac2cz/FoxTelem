package filter;

public class SinOscillator extends Oscillator {

	public SinOscillator(int samples, int freq) {
		super(samples, freq);
		initTable();
	}
	
	public SinOscillator(int samples, double phaseIncrement) {
		super(samples, phaseIncrement);
		initTable();
	}
	
	private void initTable() {
		for (int n=0; n<TABLE_SIZE; n++) {
			sinTable[n] = Math.sin(n*2.0*Math.PI/(double)TABLE_SIZE);
		}
	}
}
