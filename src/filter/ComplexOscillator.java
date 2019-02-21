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
	
	public void changePhase(double phaseIncrement) {
		cosOsc.changePhase(phaseIncrement);
		sinOsc.changePhase(phaseIncrement);
	}
	
	public void setPhaseIncrement(double phaseInc) {
		cosOsc.setPhaseIncrement(phaseInc);
		sinOsc.setPhaseIncrement(phaseInc);
	}
	
	public void setPhase(double phase) {
		cosOsc.setPhase(phase);
		sinOsc.setPhase(phase);
	}
	
	public double getFrequency() {
		return cosOsc.getFrequency();
	}
	
	public double getPhase() {
		return cosOsc.getPhase();
	}
}
