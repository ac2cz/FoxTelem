	package decoder;

import gui.MainWindow;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Line.Info;

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
 */
public class SourceSoundCardAudio extends SourceAudio implements Runnable {
	
	static Mixer[] mixerList;
	public static final int DEFAULT_READ_BUFFER_SIZE = 512 * 4; // about 5 ms at 48k sample rate;
	TargetDataLine targetDataLine = null;
	int errorCount = 0;
	byte[] readBuffer;
	double a,b;
	
	boolean skippedOneByte = false;
	int channels = 0;
	
	/*
	public SourceSoundCardAudio(int circularBufferSize, int rate, int device) throws IllegalArgumentException, LineUnavailableException {
		super("SoundCard", circularBufferSize);
		sampleRate = rate;
		setDevice(device);
		readBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
		
	}
*/
	public SourceSoundCardAudio(int circularBufferSize, int rate, int device, int chan, boolean storeStereo) throws IllegalArgumentException, LineUnavailableException {
		super("SoundCard", circularBufferSize, chan, storeStereo);
		channels = chan;
		sampleRate = rate;
		setDevice(device);
		readBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
	}

	
	private void init() throws LineUnavailableException {
		if (targetDataLine != null) {
			targetDataLine.stop();
			targetDataLine.drain();
			targetDataLine.close();
		}
		audioFormat = makeAudioFormat(sampleRate);
		targetDataLine.open(audioFormat);
		Log.println("Soundcard Line opened with buffer: " + targetDataLine.getBufferSize());

		targetDataLine.start();
	}
	
	public void stop() {
		running = false;
		if (targetDataLine != null) {
			targetDataLine.stop();
			targetDataLine.drain();
			targetDataLine.close();
		}
		done = true;
	}
	
	/**
	 * Build a list of available audio sources so that we can let the user select the one they like
	 * @return
	 */
	public static String[] getAudioSources() {
		
		int device = OFFSET; // Start at 3 because first two spaces are blank and File wav, file bit
			AudioFormat audioFmt = makeAudioFormat(48000);  // Use a standard format as this is just a check to see if the device is available
		    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		    String[] devices = new String[mixers.length+OFFSET]; // we need to add 2 just in case there are no devices, otherwise we crash
		    mixerList = new Mixer[mixers.length+OFFSET]; 
		    
		    for (Mixer.Info info : mixers) 
		    {
		        Mixer mixer = AudioSystem.getMixer(info);
		        try
		        {
		        	Log.println("Found audio Device: " + info.getName() + " Desc:" + info.getDescription());
		            Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFmt);
		            String name = info.getName();
		            String desc = info.getDescription();
		    
		            // test if line is assignable
		            // If this is a funcube dongle and we are on MacOs add it regardless because the line check fails on some platforms
		            if (Config.isMacOs() && (desc.indexOf("FUNcube Dongle")>=0 ||
		            		name.indexOf("FUNcube Dongle")>=0)) {
		            	Log.println("Detected FCD");
		            	//Info fcddataline = new DataLine.Info(TargetDataLine.class, IQDecoder.getAudioFormat());
		            	//TargetDataLine targetDataLine = (TargetDataLine)mixer.getLine(fcddataline);
		            	//targetDataLine.close(); // close it again so it is available when we need it
		            } else {

		            	TargetDataLine targetDataLine = (TargetDataLine)mixer.getLine(dataLineInfo);
		            	targetDataLine.close(); // close it again so it is available when we need it
		            
		            }
		            mixerList[device] = mixer;
		            // Linux puts the name in the description
		            if (Config.isLinuxOs()) {
		            	if (desc.length() > 50) name = desc.substring(0,50);
		            	else
		            		name = desc;
		            } else {
		            if (name.length() > 50) name = name.substring(0,50);
		            }
		            devices[device++] = name; // + " " + info.getDescription();
		        }
		        catch (LineUnavailableException e) 
		        {
		            // We dont throw this exception because we are just using it to screen out the devices that don't have this line
		        	//e.printStackTrace();
		            //System.err.println("Mixer rejected, Line Unavailable: " + info);
		        }
		        catch (IllegalArgumentException e)
		        {
		            //e.printStackTrace();
		            //System.err.println("Mixer rejected, Illegal Argument: " + info);
		        }           
		    }
		    String[] result = new String[device];
		    result[0] = "Select audio source here then press start";
		    result[FILE_SOURCE] = FILE_SOURCE_NAME;
		    //result[AIRSPY_SOURCE] = AIRSPY_SOURCE_NAME;
		 //   result[BIT_FILE_SOURCE] = BIT_FILE_SOURCE_NAME;
		    for (int i=OFFSET; i< device; i++)
		    	result[i] = devices[i];
		return result;
	}
	
	private static String getMixerIdString(Mixer appMixer) {
		Mixer.Info info = appMixer.getMixerInfo();
		return info.getName() + info.getDescription() + info.getVendor() + info.getVersion();
	}
	
	public static String getDeviceName(int position) {
//		if (position == SourceAudio.AIRSPY_SOURCE) return SourceAudio.AIRSPY_SOURCE_NAME;
		Mixer appMixer = mixerList[position];
		return getMixerIdString(appMixer);
	}
	
	public static int getDeviceIdByName(String name) {
//		if (name.equalsIgnoreCase(SourceAudio.AIRSPY_SOURCE_NAME)) return SourceAudio.AIRSPY_SOURCE;
		for (int i=1; i< mixerList.length; i++) {
			if (mixerList[i] != null)
				if (name.equalsIgnoreCase(getMixerIdString(mixerList[i]))) {
					return i;
				}
		}
		return 0;
	}
	
	/**
	 * Pass the combo box position and select this device
	 * @param position
	 * @throws LineUnavailableException
	 * @throws IllegalArgumentException
	 */
	private void setDevice(int position) throws LineUnavailableException, IllegalArgumentException {
		//Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		//AudioFormat audioFmt = getAudioFormat();

		Mixer appMixer = mixerList[position];

		Info sdlLineInfo = new DataLine.Info(TargetDataLine.class, makeAudioFormat(sampleRate));
		
		stop();

		targetDataLine = (TargetDataLine) appMixer.getLine(sdlLineInfo);

		init();
	
	}
	
	/**
	 * This is a legacy command line interface and no longer used
	 */
	

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
		AudioFormat af = new AudioFormat(sampleRate,sampleSizeInBits,channels,signed,bigEndian); 
		//System.out.println("Using standard format");
		Log.println("SC Format " + af);
		return af;
	}//end getAudioFormat

	

	/**
	 * the run method reads from the actual physical source and stores the results in the circular buffer
	 */
	@Override
	public void run() {
		done = false;
		running = true;
		try {
			init();
		} catch (LineUnavailableException e1) {
			Log.errorDialog("ERROR", "Can't start the decoder" + e1.getMessage());
			e1.printStackTrace(Log.getWriter());
			running = false;
		}
		int lastErrorCount = 0;
		int audioBufferPeriodCounter = 0;
		int audioBufferPeriod = 1000; // After this many loops, average the audio buffer errors
		Log.println("Audio Source START");
		
		while (running) {
			audioBufferPeriodCounter++;
			if (audioBufferPeriodCounter == audioBufferPeriod) {
				audioBufferPeriodCounter = 0;
				MainWindow.setAudioMissed((errorCount + lastErrorCount) / 2);  // divide by 2 to average and 10 to get to %
				lastErrorCount = errorCount;
				errorCount = 0;
			}
			if (targetDataLine != null) {
				while (running && targetDataLine.available() < targetDataLine.getBufferSize()*0.5)
					try {
						Thread.sleep(0, 1);  // without this, the audio will be choppy
					} catch (InterruptedException e) {
						e.printStackTrace(Log.getWriter());
					} 
				//boolean readBoth = false;
				int nBytesRead = targetDataLine.read(readBuffer, 0, readBuffer.length);
				
				try {
					for(int i=0; i< nBytesRead; i+=audioFormat.getFrameSize()) {
						byte[] ia = {readBuffer[i],readBuffer[i+1]};
						if (audioFormat.isBigEndian()) {
							a = Decoder.bigEndian2(ia, audioFormat.getSampleSizeInBits())/ 32768.0;
						} else {
							a = Decoder.littleEndian2(ia, audioFormat.getSampleSizeInBits())/ 32768.0;
						}
						//a = SourceAudio.getDoubleFromBytes(readBuffer[i],readBuffer[i+1],audioFormat);
						
						if (audioFormat.getFrameSize() == 4) {  // STEREO DATA because 4 bytes and 2 bytes are used for each channel
							byte[] ib = {readBuffer[i+2],readBuffer[i+3]};
							if (audioFormat.isBigEndian()) {
								b = Decoder.bigEndian2(ib, audioFormat.getSampleSizeInBits())/ 32768.0;
							} else {
								b = Decoder.littleEndian2(ib, audioFormat.getSampleSizeInBits())/ 32768.0;
							}
							//b = SourceAudio.getDoubleFromBytes(readBuffer[i+2],readBuffer[i+3], audioFormat);
						}
						if (audioFormat.getFrameSize() == 4 && storeStereo) {
							if (channels == 0)
								circularDoubleBuffer[0].add(a,b);
							else
								for (int chan=0; chan < channels; chan++)
									circularDoubleBuffer[chan].add(a,b);
						} else { // we have only mono and we need to know which channel to take the data from
							if (!Config.useLeftStereoChannel)
								a = b; // use the audio from the right channel
							if (channels == 0)
								circularDoubleBuffer[0].add(a);
							else
								for (int chan=0; chan < channels; chan++)
									circularDoubleBuffer[chan].add(a);
						}
					}
				} catch (IndexOutOfBoundsException e) {
//						Log.errorDialog("Sound Card Error", "Missed Audio");
					// We get this error if the circularBuffer is not being emptied fast enough.  We are filling it by reading data from the sound card
					// as fast as it is available (real time).  The decoder is reading it and processing it.  The circularBuffer throws the IndexOutOfBounds
					// error from the add method only when end pointer had reached the start pointer.  This means the circularBuffer is full and the next 
					// write would destroy data.  We choose to throw away this data rather than overwrite the older data.  Is does not matter
					// We do not pop up a message to the user unless we accumulate a number of these issues
					errorCount++;
					if (Config.debugAudioGlitches) {
						if (errorCount % 10 == 0) {
							Log.println("Missed audio from the sound card, Buffers missed: " + errorCount + " with capacity: " + getAudioBufferCapacity());
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
		}
		Log.println("Audio Source EXIT, channels:" + channels);
	}

	
	
}
