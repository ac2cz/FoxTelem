package filter;

/**
 * An NCO
 * sinTable holds a set of amplitudes at given phase offsets in radians.  The actual values need to be calculated and stored
 * by the parent class (sin, cos or other wave shape).
 * phase is the current position in the table
 * phaseIncrement is the amount we increment each sample period.  For a given frequency we increment the phase
 * by 2 * PI * frequency / samplesPerSecond.  So that in a full second the phase gets incremented by 2 * Pi * f
 * 
 * To find an amplitude given a phase, we convert the phase to the index position in the table.  This is the same
 * as TABLE_SIZE * phase / 2*Pi
 * 
 * Note that frequency is the speed that we march through the table
 * Phase is the current position in the table
 * phaseIncrement is the amount that the frequency causes the phase to increase.
 * 
 * @author chris
 *
 */
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
		incPhase(phaseIncrement);
	}
	
	public void changePhaseOLD(double phaseIncrement) { 
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

	private void incPhase(double phaseIncrement) {
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
