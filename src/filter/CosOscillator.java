package filter;

public class CosOscillator extends Oscillator {

	public CosOscillator(int samples, int freq) {
		super(samples, freq);
		for (int n=0; n<TABLE_SIZE; n++) {
			sinTable[n] = Math.cos(n*2.0*Math.PI/(double)TABLE_SIZE);
		}
	}
}
