package decoder;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

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
public class SourceWav extends SourceAudio implements Runnable {
	String fileName;
	long totalFrames = 0;
	int frameSize = 0;
	long bytesRead = 0;
	long framesProcessed = 0;

	//boolean fileDone = false;
	byte[] readBuffer;
	public static final int DEFAULT_READ_BUFFER_SIZE = 512 * 4; // about 5 ms at 48k sample rate;
	
	AudioInputStream audioStream = null; // The object used to read the stream of data from the wave file
	
	public SourceWav(String f) throws UnsupportedAudioFileException, IOException {
		super("WavFile", 67200*3);
		fileName = f;;
		initWav();
//        
 //       
 //       if (sink != null)
  //      	sink.initializeOutput(); // make sure we have the same channels in the output

	}

	private void initWav() throws UnsupportedAudioFileException, IOException {
		readBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
//		circularBuffer = new CircularByteBuffer(67200*3);
	    Log.println("Wavefile: " + fileName);
		File soundFile = null;
        soundFile = new File(fileName);
        audioStream = AudioSystem.getAudioInputStream(soundFile);
        audioFormat = audioStream.getFormat();

        Config.wavSampleRate = (int) audioFormat.getSampleRate();
        Log.println("Format: " + audioFormat);
        totalFrames = audioStream.getFrameLength();
        frameSize = audioFormat.getFrameSize();
        Log.println("Length (frames): " + totalFrames);
        bytesRead = 0;
        framesProcessed = 0;
	}
	
	public void stop() {
		running = false;
		cleanup();
	}
	
	public void cleanup() {
		try {
			if (audioStream != null)
			audioStream.close();
			audioStream = null; // This will prevent us from trying one last read from the file and generating an exception
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}
		// Give the decoder time to finish - not sure this makes any difference though??
		if (circularBuffer.size() > 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace(Log.getWriter());
			}
		}
		done = true;
	}
	
	@Override
	public void run() {
		running = true;
		done = false;
		Log.println("WAV Source START");
	    if (audioStream == null)
			try {
				initWav();
			} catch (UnsupportedAudioFileException e1) {
				Log.errorDialog("ERROR", "Unsupported File Format\n" + e1.getMessage());
				e1.printStackTrace(Log.getWriter());
				running = false;
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "There was a problem opening the wav file\n" + e1.getMessage());
				e1.printStackTrace(Log.getWriter());
				running = false;
			}
 
		while (running) {
//			Log.println("wav running");
			if (audioStream != null) {
					int nBytesRead = 0;
					if (circularBuffer.getCapacity() > readBuffer.length) {
						try {
							nBytesRead = audioStream.read(readBuffer, 0, readBuffer.length);
							bytesRead = bytesRead + nBytesRead;
							framesProcessed = framesProcessed + nBytesRead/frameSize;
							// Check we have not stopped mid read
							if (audioStream == null) running = false; else if (!(audioStream.available() > 0)) running = false;
						} catch (IOException e) {
							Log.errorDialog("ERROR", "Failed to read from file " + fileName) ;
							e.printStackTrace(Log.getWriter());
						}
					} else {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//Log.println("No room in Buffer");
					}
					for(int i=0; i< nBytesRead; i+=2) {
						//circularBuffer.add(readBuffer[i]);
						circularBuffer.add(readBuffer[i],readBuffer[i+1]);
					}

			}
		}
		framesProcessed = totalFrames;
		cleanup(); // This might cause the decoder to miss the end of a file (testing inconclusive) but it also ensure the file is close and stops an error if run again very quickly
		running = false;
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.println("WAV Source EXIT");
	}

	public int getPercentProgress() {
		//System.out.println(framesProcessed + " " +  totalFrames);
		int percent = 0;
		try {
		percent = (int) (100 * framesProcessed / totalFrames);
		} catch (ArithmeticException a) {
			// Don't print a message here or it fills the log up!
			//a.printStackTrace(Log.getWriter());
			
		} 
		return percent;
	}

}
