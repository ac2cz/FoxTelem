package decoder;

import javax.sound.sampled.AudioFormat;

import common.Config;
import common.Log;

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
 * An generic audio source that stores a circular buffer of audio bytes.  The run method is implemented by a child
 * class and pulls audio from a file or other audio source.  The bytes are written into the circular buffer
 * The bytes are extracted from the circular buffer with the read method.  This method is common across
 * audio sources, so that all sources look the same to the decoder.
 * 
 * The read method blocks until enough bytes are available.  It is effectively waiting for the run method to fill the
 * buffer
 * 
 * The run method for a real time source should be implemented so that it writes data to the buffer as fast as it is
 * available and it should throw an exception if the end pointer reaches the start pointer.  This indicates that the
 * decoder can not keep up with the arrival of real time data and data was lost.  For a file source, the run method can block
 * until space is available in the circular buffer, as no data will be lost.
 * 
 * @author chris.e.thompson
 *
 */
public abstract class SourceAudio implements Runnable {
	public static final int FILE_SOURCE = 1;
//	public static final int BIT_FILE_SOURCE = 3;
//	public static final int IQ_FILE_SOURCE = 2;
	public static final int OFFSET = 2;
	public static final String FILE_SOURCE_NAME = "Load Wav File";
//	public static final String IQ_FILE_SOURCE_NAME = "Load IQ File";
//	public static final String BIT_FILE_SOURCE_NAME = "Load Bit File";

	protected String name="unknown";
	protected boolean done = false;
	protected boolean running = true;
	protected int sampleRate = 48000; // samples per second
	protected AudioFormat audioFormat = null; // The format of the audio

	protected CircularByteBuffer circularBuffer;
	
	public AudioFormat getAudioFormat() { return audioFormat; }
	
	public SourceAudio(String n, int circularBufferSize) {
		name = n;
		if (circularBufferSize % 2 == 0) circularBufferSize+=1; // must be odd to prevent corruption if the buffer overflows
		circularBuffer = new CircularByteBuffer(circularBufferSize);
	}
	
	public int readBytes(byte[] abData) {
		int bytesRead = 0;

		// We need at least abData length +1 bytes before we can read them out of the circularBuffer
		// We make sure it has at least an extra sample
		while (running && circularBuffer.size() < abData.length+4) {
			try {
				Thread.sleep(0, 1);
			} catch (InterruptedException e) {
				e.printStackTrace(Log.getWriter());
			}
		//	Log.println(name + ": readbytes Wait.. because running="+running+ " circ buf size="+circularBuffer.size());
		}
		if (running) // in case we stopped mid read
			try {
				for(int i=0; i < abData.length; i+=audioFormat.getFrameSize()) {
					if (audioFormat.getFrameSize() == 4) {
						abData[i+3] = circularBuffer.get(3);  // try the second byte first, because we only want to succeed if both are available
						abData[i+2] = circularBuffer.get(2);
					}
					abData[i+1] = circularBuffer.get(1);  // try the second byte first, because we only want to succeed if both are available
					abData[i] = circularBuffer.get(0);
					circularBuffer.incStartPointer(audioFormat.getFrameSize());
					bytesRead+=audioFormat.getFrameSize();
				}
			} catch (IndexOutOfBoundsException e) {
				// If this happens, we are in an unusual situation.  We waited until the circularBuffer contains abData.length of data
				// then we started to read it one byte at a time.  However, we have moved the read (start) pointer as far as the end
				// pointer, so we have run out of data.
				Log.errorDialog("AUDIO BUFFER READ ERROR", e.getMessage() + "\nTry starting the decoder again.");
				e.printStackTrace(Log.getWriter());
			}
		return bytesRead;
	}

	public int getAudioBufferCapacity() { return circularBuffer.getCapacity(); }
	
	public boolean isDone() { return done; }
	public abstract void run();
	public abstract void stop();
	
	/**
	 * Convert audio bytes from a soundcard into an array of doubles.  Create the doubles array
	 * @param abBuffer
	 * @return
	 */
	static public void getDoublesFromBytes(byte[] abBuffer, boolean stereo, double[] abBufferDouble) {
		//There are 4 bytes_per_sample in a stereo file, 2 in mono
		int bytesPerSample = 2;
		if (stereo) bytesPerSample = 4;
		int samplesRead = abBuffer.length /bytesPerSample;  
		//double[] abBufferDouble = new double[samplesRead];
		// We copy the bytes into a doubles array
				// Channel 1 only is updated for a stereo file
				if (!stereo) {
					// MONO
					for (int i = 0; i < samplesRead; i++)
							abBufferDouble[i] = ((abBuffer[i*2] & 0xFF) | (abBuffer[i*2 + 1] << 8)) / 32768.0;
					
				} else { 
					// STEREO
					for (int i = 0; i < samplesRead; i++)
						if (Config.useLeftStereoChannel)
							abBufferDouble[i] = ((abBuffer[i * 4] & 0xFF) | (abBuffer[i * 4 + 1] << 8)) / 32768.0;
						else
							abBufferDouble[i] = ((abBuffer[i * 4 + 2] & 0xFF) | (abBuffer[i * 4 + 3] << 8)) / 32768.0;
				}
				
			//	return abBufferDouble;
	}

	// converts double array to byte array
	static public void getBytesFromDoubles(final double[] audioData, final int storedSamples, boolean stereo, byte[] audioDataBytes) {
		//int bytesPerSample = 2;
		//if (stereo) bytesPerSample = 4;
		
			//byte[] audioDataBytes = new byte[storedSamples * bytesPerSample];

			int k = 0;
			if (!stereo) {
				for (int i = 0; i < storedSamples; i++) {
					// saturation
					audioData[i] = Math.min(1.0, Math.max(-1.0, audioData[i]));

					// scaling and conversion to integer
					int sample = (int) Math.round((audioData[i] + 1.0) * 32767.5) - 32768;

					byte high = (byte) ((sample >> 8) & 0xFF);
					byte low = (byte) (sample & 0xFF);
					audioDataBytes[k] = low;
					audioDataBytes[k + 1] = high;
					k = k + 2;
				}
			} else {
				// STEREO
				for (int i = 0; i < storedSamples; i++) {
					// saturation
					audioData[i] = Math.min(1.0, Math.max(-1.0, audioData[i]));

					// scaling and conversion to integer
					int sample = (int) Math.round((audioData[i] + 1.0) * 32767.5) - 32768;

					byte high = (byte) ((sample >> 8) & 0xFF);
					byte low = (byte) (sample & 0xFF);
					audioDataBytes[k] = low;
					audioDataBytes[k + 1] = high;
					// Put the same in the other channel
					audioDataBytes[k + 2] = low;
					audioDataBytes[k + 3] = high;
					k = k + 4;
				}
				
			}

			//return audioDataBytes;
		}
}
