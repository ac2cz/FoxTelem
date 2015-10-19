package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.JLabel;

import common.Config;
import common.Log;
import decoder.Decoder;

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
 * A panel that displays a graph of the data that it reads from the decoder
 * 
 */
@SuppressWarnings("serial")
public class AudioGraphPanel extends JPanel implements Runnable {

//	JLabel lblIqbalance;
	String fileName = null;
	boolean running = true;
	boolean done = false;
	int centerFreqX = 220;
	Decoder decoder;
	byte[] audioData = null;
	int AUDIO_DATA_SIZE = 1024;
	int currentDataPosition = 0;
	JLabel sample;
	int s = 0;
	JLabel title;
	
	boolean showFilteredAudio = false;
	
	long bufferCapacityAvg;
	int bufferCapacitySample;
	final int BUFFER_CAP_SAMPLE_NO = 100;
	int bufferCapacity;
	
	public AudioGraphPanel() {
		//decoder = d;
		title = new JLabel("Sample rate: ");// + Integer.toString(decoder.getCurrentSampleRate()));
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		add(title);
		//sample  = new JLabel("sample: " + 0);
		//add(sample);
	}
	
	public void updateFont() {
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
	}
	
	public void showFilteredAudio() {
		showFilteredAudio = true;
	}
	
	public void showUnFilteredAudio() {
		showFilteredAudio = false;
	}
	
	@Override
	public void run() {
		done = false;
		running = true;
//		int bytesRead =0;
//		int decimate=0;
		currentDataPosition = 0;
		//int border = 5;
		//int AUDIO_DATA_SIZE = getWidth() - border*2;
		
		while(running) {
			
			try {
				Thread.sleep(1000/60); // approx 1/60 sec refresh
			} catch (InterruptedException e) {
				Log.println("ERROR: Audiograph thread interrupted");
				//e.printStackTrace();
			} 
			
			byte[] buffer;
			if (decoder != null) {
				if (showFilteredAudio)
					buffer = decoder.getFilteredData();
				else
					buffer = decoder.getAudioData();
			
				if (buffer != null) {
					audioData = buffer;				
				}
				
				this.repaint();
			}
		}			
	}
	
	public void startProcessing(Decoder d) {
		decoder = d;
		title.setText("Sample rate: " + Integer.toString(decoder.getCurrentSampleRate()) + " | Samples: " + decoder.getSampleWindowLength());
		
		running = true;
	}
	
	public void stopProcessing() { 
		running = false;
		//source.drain();
	}
	
	/*
	 * Draw on a panel, where x is horizontal from left to right and y is vertical from top to bottom
	 * Draw a line segment for each sample, from the previous sample
	 * drawline x1,y1,y2,y2
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent( g ); // call superclass's paintComponent  
		Graphics2D g2 = ( Graphics2D ) g; // cast g to Graphics2D  
		
		// Have 5 pix border
		int border = 5;
		int graphHeight = getHeight() - border;
		int graphWidth = getWidth() - border*2;
		
		// Draw baseline with enough space for text under it
		g2.drawLine(0, graphHeight-border, graphWidth, graphHeight-border);
		// Draw vertical axis
		g2.drawLine(border*2, getHeight()-border, border*2, border*4);
	
		int lastx = border*2+1; 
		int lasty = graphHeight/2;
		int x = border*2+1;
		
		g2.setColor(Color.BLUE);
		
		int stepSize = 1;
		//int spaceSize = 1;
			
		if (audioData != null && audioData.length > 0) {
			if (audioData.length > graphWidth) {
				stepSize = (int) Math.round((audioData.length)/graphWidth);
			} else {
				// we leave step size at 1 and plot all of the points, but space them out.
				//spaceSize = graphWidth/(audioData.length);
			}
			
			if (stepSize <= 0) stepSize = 1;
			for (int i=0; i < audioData.length-stepSize; i+=stepSize*2) {
				// data is stereo, but we want to decimate before display
				byte[] by = new byte[2];
				by[0] = audioData[i];
				by[1] = audioData[i+1];
				int value;
				if (decoder.getBigEndian())
					value = Decoder.bigEndian2(by, decoder.getBitsPerSample());
				else
					value = Decoder.littleEndian2(by, decoder.getBitsPerSample());

					//x = (i*j/(Decoder.SAMPLE_WINDOW_LENGTH*Decoder.BUCKET_SIZE))*graphWidth;
					x = border*2 + i*(graphWidth-border*2)/audioData.length;
					
					// Calculate a value between -1 and + 1 and scale it to the graph height.  Center in middle of graph
					double y = graphHeight/2+graphHeight/2.5*value/32768 + border;
					//int y = 100;
					g2.drawLine(lastx, lasty, x, (int)y);
					lastx = x;
					lasty = (int)y;

				}
		}
		g2.setColor(Color.GRAY);
		// Center (decode) line
		g2.drawLine(0, graphHeight/2+border, graphWidth, graphHeight/2+border);
		

		if (Config.debugAudioGlitches) {
			Runtime rt = Runtime.getRuntime();
			long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
			g.drawString("Mem: "+usedMB, 10, 20 );
			if (decoder !=null)
				if (decoder.getFilter() != null) {
					g.drawString("Gain: "+GraphPanel.roundToSignificantFigures(decoder.getFilter().getGain(),4), 70, 20 );
					bufferCapacityAvg += decoder.getAudioBufferCapacity();
					bufferCapacitySample++;
					if (bufferCapacitySample == BUFFER_CAP_SAMPLE_NO) {
						bufferCapacity = (int) (bufferCapacityAvg / bufferCapacitySample);
						bufferCapacitySample=0;
						bufferCapacityAvg = 0;
					}
					g.drawString("Size: "+decoder.getAudioBufferSize() + 
							" Capacity: "+decoder.getAudioBufferCapacity(), getWidth()-200, 20 );

				}
		}
		//sample.setText("sample: " + s++);
	}

	
}

