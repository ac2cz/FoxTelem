package filter;

public class CosOscillator extends Oscillator {

	public CosOscillator(int samples, int freq) {
		super(samples, freq);
		initTable();
	}
	
	public CosOscillator(int samples, double phaseIncrement) {
		super(samples, phaseIncrement);
		initTable();
	}
	
	private void initTable() {
		for (int n=0; n<TABLE_SIZE; n++) {
			sinTable[n] = Math.cos(n*2.0*Math.PI/(double)TABLE_SIZE);
		}
	}
}
