package filter;

public class SinOscillator extends Oscillator {

	public SinOscillator(int samples, int freq) {
		super(samples, freq);
		for (int n=0; n<TABLE_SIZE; n++) {
			sinTable[n] = Math.sin(n*2.0*Math.PI/(double)TABLE_SIZE);
		}
	}
}
