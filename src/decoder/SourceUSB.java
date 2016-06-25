package decoder;

import javax.sound.sampled.AudioFormat;

import common.Config;
import common.Log;
import device.airspy.AirspyDevice;

public class SourceUSB extends SourceAudio implements Listener<float[]>, Runnable {

	
	int errorCount = 0;
	
	public SourceUSB(String n, int sampleRate, int circularBufferSize, int channels) {
		super(n, circularBufferSize, channels);
		this.sampleRate = sampleRate;
		audioFormat = makeAudioFormat(sampleRate);
	}

	//byte[] readBuffer;
	float a,b,c,d;
	public void receive(float[] realSamples) {
		try {
			int bytesPerSample = 2;
			for(int i=0; i< realSamples.length; i+=bytesPerSample) {

				a = realSamples[i];
				b = realSamples[i+1];

				if (channels == 0)
					circularDoubleBuffer[0].add(a,b);
				else
					for (int chan=0; chan < channels; chan++)
						circularDoubleBuffer[chan].add(a,b);

			}
		} catch (IndexOutOfBoundsException e) {
			//	Log.println("Missed Audio due to:" + e.getMessage());
			// We get this error if the circularBuffer is not being emptied fast enough.  We are filling it by reading data from the sound card
			// as fast as it is available (real time).  The decoder is reading it and processing it.  The circularBuffer throws the IndexOutOfBounds
			// error from the add method only when end pointer had reached the start pointer.  This means the circularBuffer is full and the next 
			// write would destroy data.  We choose to throw away this data rather than overwrite the older data.  Is does not matter
			// We do not pop up a message to the user unless we accumulate a number of these issues
			errorCount++;
			if (Config.debugAudioGlitches) {
				if (errorCount % 10 == 0) {
			//		Log.println("Missed audio from the sound card, Buffers missed: " + errorCount + " with capacity: " + getAudioBufferCapacity());
					//	
					//}
					if (errorCount % 100 == 0) {
						Log.println("Cant keep up with audio from soundcard: " + e.getMessage());
						//if (Config.debugAudioGlitches)
						//Log.errorDialog("Sound Card Error", "Cant keep up with audio from soundcard.  Perhaps the sample rate\n"
						//		+ "is wrong or mismatched with the source it is reading from?");
						//errorCount = 0;
					}
				}
			}
		}
	}
	
	/**
	 * Populate an AudioFormat object with the parameters we need and return the object
	 * @return
	 */
	public static AudioFormat makeAudioFormat(int sampleRate){
		
		int sampleSizeInBits = 16;
		//Decoder.bitsPerSample = 16;
		int channels = 2;
		boolean signed = true;
		boolean bigEndian = false;
		//Decoder.bigEndian = false;
		sampleRate = AirspyDevice.DEFAULT_SAMPLE_RATE.getRate();
		AudioFormat af = new AudioFormat(sampleRate,sampleSizeInBits,channels,signed,bigEndian); 
		//System.out.println("Using standard format");
		Log.println("SC Format " + af);
		return af;
	}//end getAudioFormat

	
	@Override
	public void run() {
		// Nothing to run.  The thread runs in the USB Device and writes data here
		
	}

	@Override
	public void stop() {
		// Nothing to stop
		done = true;
		
	}

}
