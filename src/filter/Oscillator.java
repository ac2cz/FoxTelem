package filter;

public abstract class Oscillator {

	protected final int TABLE_SIZE = 9600;
	protected double[] sinTable = new double[TABLE_SIZE];
	private int samplesPerSecond = 0;
	private double frequency = 0;
	private double phase = 0;
	private double phaseIncrement = 0;
	
	public Oscillator(int samples, int freq) {
		this.samplesPerSecond = samples;
		setFrequency(freq);
	}
	
	public void changePhase(double phaseIncrement) { 
		if ((phaseIncrement < 2*Math.PI) && (phaseIncrement> -2*Math.PI)) {
			phase = phase + phaseIncrement;
			if (phase >= 2 * Math.PI) {
				phase = phase - 2*Math.PI;
				frequency = frequency + 1;
				setFrequency(frequency);
			}
			if (phase <= 0) {
				phase = phase + 2*Math.PI;
				frequency = frequency - 1;
				setFrequency(frequency);
			}
		}
	}

	public void incPhase(double phaseIncrement) {
		if ((phaseIncrement < 2*Math.PI) && (phaseIncrement> -2*Math.PI)) {
			phase = phase + phaseIncrement;
			if (phase >= 2 * Math.PI)
				phase = phase - 2*Math.PI;
			if (phase <= 0)
				phase = phase + 2*Math.PI;
		}

	}
	
	public void setFrequency(double freq) {
		frequency = freq;
		phaseIncrement = 2 * Math.PI * frequency / (double)samplesPerSecond;
	}
	
	public double getFrequency() { 
		return frequency;
	}
	
	public double nextSample() {
		incPhase(phaseIncrement);
		int idx = (int)((phase * (double)TABLE_SIZE/(2 * Math.PI))%TABLE_SIZE);
		double value = sinTable[idx];
		return value;
	}
	
}
