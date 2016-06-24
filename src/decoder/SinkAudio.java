package decoder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

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
public class SinkAudio {
	SourceDataLine sourceDataLine;
	static Mixer[] mixerList;
	AudioFormat audioFormat;
	
	public SinkAudio(AudioFormat af) {
		audioFormat = af;
		initializeOutput();
	}

	private static String getMixerIdString(Mixer appMixer) {
		Mixer.Info info = appMixer.getMixerInfo();
		return info.getName() + info.getDescription() + info.getVendor() + info.getVersion();
	}
	
	public static String getDeviceName(int position) {
		Mixer appMixer = mixerList[position];
		return getMixerIdString(appMixer);
	}
	
	public static int getDeviceIdByName(String name) {
		for (int i=1; i< mixerList.length; i++) {
			if (mixerList[i] != null)
				if (name.equalsIgnoreCase(getMixerIdString(mixerList[i]))) {
					return i;
				}
		}
		return 0;
	}
	
	public void setDevice(int position) throws LineUnavailableException, IllegalArgumentException {
		if (position == 0 || position == -1) {
			initializeOutput();
		} else {
			Mixer appMixer = mixerList[position];

			Info sdlLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
			
			sourceDataLine = (SourceDataLine) appMixer.getLine(sdlLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();
		}
			

	}
	
	/**
	 * FIXME:
	 * specify the buffer size in the open(AudioFormat,int) method. A delay of 10ms-100ms will be acceptable for realtime audio. Very low latencies like will 
	 * not work on all computer systems, and 100ms or more will probably be annoying for your users. A good tradeoff is, e.g. 50ms. For your audio format, 
	 * 8-bit, mono at 44100Hz, a good buffer size is 2200 bytes, which is almost 50ms
	 */
	void initializeOutput() {
		
		DataLine.Info dataLineInfo = new DataLine.Info(  SourceDataLine.class, audioFormat);
		//line = (TargetDataLine) AudioSystem.getLine(info);
		//Mixer m = AudioSystem.getMixer(null);
		try {
			//sourceDataLine = (SourceDataLine)m.getLine(dataLineInfo);
			sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(Log.getWriter());
		}

	}
	
	
	public static String[] getAudioSinks() {
		int device = 1;
			//AudioFormat audioFmt = getAudioFormat();
		    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		    mixerList = new Mixer[mixers.length+1];
			String[] devices = new String[mixers.length+1];

		    for (Mixer.Info info : mixers) 
		    {
		        Mixer mixer = AudioSystem.getMixer(info);
		        try
		        {
//		          System.out.println(info);
		            Info sdlLineInfo = new DataLine.Info(SourceDataLine.class, getAudioFormat());

		            // test if line is assignable
		            SourceDataLine sdl = (SourceDataLine) mixer.getLine(sdlLineInfo);
		            sdl.close();
		            // if successful, add to list
		            mixerList[device] = mixer;
		            String name = info.getName();
		            if (name.length() > 50) name = name.substring(0,50);
		            devices[device++] = name; //info.getName()+ " " + info.getDescription(); // + " <> " + info.getDescription();
		        }
		        catch (LineUnavailableException e) 
		        {
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
		    for (int i=0; i< device; i++)
		    	result[i] = devices[i];
		return result;
	}

	public void flush() {
		sourceDataLine.flush();;
	}
	
	public void closeOutput() {
		sourceDataLine.drain();
		sourceDataLine.close();
	}

	
	public void write(float[] f) {
		byte[] audioData = new byte[f.length*2];
		boolean stereo = true;
		SourceAudio.getBytesFromFloats(f, f.length, stereo, audioData);

		write(audioData, audioData.length);
	}
	/**
	 * Write bytes to the output.  
	 * @param myData
	 * @param numBytesRead
	 */
	public void write(byte[] myData, int numBytesRead) {
		// FIXME - This potentially needs to happen in a background thread because the write blocks until the buffer is filled.  This slows the overall
		// audio loop down to the speed of the audio playback.  If we are receiving real time, that means we risk missing some of the audio on the input
	//	if (Config.playbackSampleRate < Config.currentSampleRate) {
			
			sourceDataLine.write(myData, 0, numBytesRead);
	//	} else
	//		sourceDataLine.write(myData, 0, numBytesRead);
	}

	public void resetChannels() {
		if (sourceDataLine.getFormat().getChannels() != 2) {
			//sourceDataLine.close();
			//sourceDataLine.open(getAudioFormat());
		}
	}

	/**
	 * Get the audio format for output to the speaker. This format is only used to find the sound cards initially and to test that they work with a 
	 * standard value.  The actual rate is determined when new is called.
	 * @return
	 */
	private static AudioFormat getAudioFormat(){
	    float sampleRate = 48000; 
	    int sampleSizeInBits = 16;
	    //8,16
	    int channels = 2;
	    //1,2
	    boolean signed = true;
	    //true,false
	    boolean bigEndian = false;
	    //true,false
	    return new AudioFormat( sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}
	
}
