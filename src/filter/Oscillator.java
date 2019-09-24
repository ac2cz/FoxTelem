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
	
	public Oscillator(int samples, double phaseInc) {
		this.samplesPerSecond = samples;
		setPhaseIncrement(phaseInc);
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
		if (frequency != freq) { // avoid the calculation if they are the same
			frequency = freq;
			phaseIncrement = 2 * Math.PI * frequency / (double)samplesPerSecond;
		}
	}
	
	public void setPhase(double phase) {
		this.phase = phase % 2*Math.PI;
	}
	
	public void setPhaseIncrement(double phaseInc) {
		if (phaseIncrement != phaseInc) { // avoid the calculation if they are the same
			frequency = phaseInc * samplesPerSecond / (2 * Math.PI);
			phaseIncrement = phaseInc;
		}
	}
	
	public double getFrequency() { 
		return frequency;
	}
	
	public double getPhase() {
		return phase;
	}
	
	public double getPhaseIncrement() {
		return phaseIncrement;
	}

	// avoid memory alloc in audio loop
	double value = 0; 
	int idx = 0;
	public double nextSample() {
		incPhase(phaseIncrement);
		idx = ((int)((phase * (double)TABLE_SIZE/(2 * Math.PI))))%TABLE_SIZE;
		if (idx < 0 || idx > sinTable.length)
			;//System.err.println("NEG IDX ERROR: " + idx + " phase:" + phase + " inc:"+phaseIncrement);
		else
			value = sinTable[idx];
		return value;
	}
	
}
