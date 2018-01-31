package filter;

import javax.sound.sampled.AudioFormat;

import common.Config;
import decoder.FoxDecoder;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
public abstract class Filter {
	protected String name = "UNDEFINED FILTER";
	protected int SAMPLERATE = 48000;
	protected int length = 0;
	//private boolean firstRun = true; 
	double[] overlapDouble;
	double cutoffFreq = 200; // Half amplitude rolloff point
	
	boolean overlapAdd = true;
	boolean stereo = true;
	boolean useAGC = false;
	boolean filterDC = true;
	DcRemoval dcFilter;
	AudioFormat audioFormat;
	int doubleBufferSize;
	private double gain = 1;
	double[] abBufferDouble;  // temp buffer used in the filter to hold the overlap and the data
	boolean decimationFilter = false; // true if we are decimating, in which case decimation factor is set
	int decimationFactor = 0;
	int decimationCount = 0;
	
	
	public Filter(AudioFormat af, int bufferSize) {	
		audioFormat = af;
		doubleBufferSize = bufferSize;
	}
	
	public double getGain() {return gain;}
	
	public void setDecimationFactor(int f) {
		decimationFactor = f;
		decimationCount = f; // so we start from the first value
		decimationFilter = true;
	}
	
	protected boolean calculateNow() {
		if (decimationFilter) {
			if (decimationCount == decimationFactor) {
				decimationCount--;
				return true;
			}
			decimationCount--;
			if (decimationCount == 0) decimationCount = decimationFactor;	
			return false;
		} else
			return true;
	}
	
	protected void coreInit() {
		if (audioFormat.getChannels() == FoxDecoder.MONO) stereo = false;
		useAGC = Config.useAGC;
	}
	
//	public void setStereo(boolean s) { stereo = s;}
	public void setAGC(boolean s) { useAGC = s;}
	public void setFilterDC(boolean s) { filterDC = s;}
	
	/*
	public void firstRunComplete() {
		firstRun = false;
	}
	
	public void resetFirstRun() {
		firstRun = true;
	}
	*/
	public void init(double sampleRate, double freq, int len) {
		coreInit();
		SAMPLERATE = (int) sampleRate;
		cutoffFreq = freq;
		length = len;
		dcFilter = new DcRemoval(0.9999f);
		// A buffer to hold the overlap at the end of filtering a window of data
		// needs to be added to the start of the next window
		overlapDouble = new double[getFilterLength()];
		// create a buffer with the extra space for the overlap
		abBufferDouble = new double[doubleBufferSize + getFilterLength()];

	}
	
	/**
	 * Take an array of bytes and filter them.  Return a filtered array of bytes
	 * These are raw bytes from the audio stream and are returned in the same format
	 * @param abBuffer
	 * @return
	 
	public byte[] filterAndCopy(byte[] abBuffer) {
				
		int bytesPerSample = 2;
		if (stereo) bytesPerSample = 4;
		
		double[] abBufferDouble = new double[abBuffer.length /bytesPerSample];
		SourceAudio.getDoublesFromBytes(abBuffer, stereo, abBufferDouble);
		
		filter(abBufferDouble, abBufferDouble);
		
		byte[] audioDataBytes = new byte[abBufferDouble.length * bytesPerSample];
		SourceAudio.getBytesFromDoubles(abBufferDouble, abBufferDouble.length, stereo, audioDataBytes);
		return audioDataBytes;
	}
	*/
	
	public void filter(double[] inputDouble, double[] outputDouble) {
		
		int samplesRead = inputDouble.length;
		double maxLevel=0d;
		double minLevel=0d;
		//double sum=0d;
		//double average=0d;

		/** We want DC balance and gain to prevent clipping at high or low point.  We are working with
		 *  doubles, so the range is from -1 to +1.
		 *  We want to fill 50% of the range, so from -0.5 to +0.5.  If we exceed that, then do nothing. DC balance is 
		 *  obtained in the bit detection, which calculates a floating zero level
		 *  
		 */

		
		// copy into a buffer with the extra space for the overlap and DC filter while we are at it
		for (int i = 0; i < samplesRead; i++) {
			if (filterDC)
				abBufferDouble[i] = dcFilter.filter( inputDouble[i]);
			else
				abBufferDouble[i] = inputDouble[i];
			//abBufferDouble[i] = inputDouble[i];
		}
		
		// zero the overlap
		for (int i = samplesRead; i < samplesRead + getFilterLength(); i++) {
			abBufferDouble[i] = 0;			
		}
		
		// We run the filter all the way to the extra length
		for (int i = 0; i < samplesRead + getFilterLength(); i++) {
			abBufferDouble[i] = filterDouble(abBufferDouble[i]);
			if (abBufferDouble[i]>maxLevel) maxLevel = abBufferDouble[i];
			if (abBufferDouble[i]<minLevel) minLevel = abBufferDouble[i];
		}
		
		// Add the last overlap to the start of the results of this filter iteration
		if (overlapAdd)
			abBufferDouble = overlapAdd(abBufferDouble);
		
		//Store the overlap from this filter iteration in the overlap buffer
		storeOverlap(abBufferDouble);
		

		//gain = 4; // 6db 
	//	double range = maxLevel - minLevel;
	//	if (range < 1.0d) {    
			double DESIRED_MAX_LEVEL = 0.5f;
			gain = DESIRED_MAX_LEVEL/maxLevel;
//			double desiredRange = 1.5d;
//			gain = desiredRange/range;
	//	}
		if (gain < 1) gain = 1;
		
//		System.out.println("Max Level: "+maxLevel);
//		System.out.println("Min Level: "+minLevel);
//		System.out.println("Range: "+range);
//		System.out.println("Gain: "+gain + " average " + average);
		
		// If AGC is off, fix the gain
		if (!useAGC) gain = 1;
		
		//if (firstRun && gain != 1 && Config.useAGC) // This is the first run so apply gain
		if (gain != 1 && Config.useAGC) // Apply gain
		for (int i = 0; i < samplesRead + getFilterLength(); i++) {
			//System.out.println("before: "+abBufferDouble[i]);
			abBufferDouble[i] = amplify(abBufferDouble[i], gain); // apply gain
			//System.out.println("after: "+abBufferDouble[i]);
		}
		
		//double[] outputDouble = new double[samplesRead];
		for (int i = 0; i < samplesRead; i++) {
			outputDouble[i] = abBufferDouble[i];
		}
		//return outputDouble;
	}

	public abstract double filterDouble(double abBuffer);
	
	protected abstract int getFilterLength();
	
	public abstract double[] getKernal();
	/**
	 * Add the overlap buffer to the start of this buffer
	 * @param buffer
	 * @return
	 */
	private double[] overlapAdd(double[] buffer) {
		for (int i=0; i< overlapDouble.length; i++) {
			buffer[i] = buffer[i] + overlapDouble[i];
		}
		return buffer;
	}
	
	/**
	 * Copy the last doubles into the overlap buffer
	 * @param buffer
	 */
	private void storeOverlap(double[] buffer) {
		if (buffer.length > overlapDouble.length + 1)
			for (int i=0; i < overlapDouble.length; i++) {
				overlapDouble[i] = buffer[buffer.length - overlapDouble.length - 1 + i];
			}
	}
	
	/**
	 * Amplify the signal
	 * @param in
	 * @param gain
	 * @return
	 */
	private double amplify(double in, double gain) {
		
		in = in * gain;
		if (in > 1.0) in = 1.0;
		if (in < -1.0) in = -1.0;
		return in;
	}
		
			
	public String toString() {
		return name ;
	}
}
