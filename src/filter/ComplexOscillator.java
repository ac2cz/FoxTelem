package filter;

public class ComplexOscillator {

	private Oscillator cosOsc;
	private Oscillator sinOsc;
	
	public ComplexOscillator(int samples, int freq) {
		cosOsc = new CosOscillator(samples, freq);
		sinOsc = new SinOscillator(samples, freq);
	}
	
	public void setFrequency(double freq) {
		cosOsc.setFrequency(freq);
		sinOsc.setFrequency(freq);
	}
	
	public Complex nextSample() {
		double i = cosOsc.nextSample();
		double q = sinOsc.nextSample();
		Complex c = new Complex(i, q);
		return c;
	}
	
	public void setPhase(double phaseIncrement, double freq) {
		cosOsc.setPhase(phaseIncrement, freq);
		sinOsc.setPhase(phaseIncrement, freq);
	}
	
	public double getFrequency() {
		return cosOsc.getFrequency();
	}
}
