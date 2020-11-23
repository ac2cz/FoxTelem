package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.JPanel;

import common.Config;
import common.Log;
import decoder.FoxDecoder;
import decoder.FoxBPSK.FoxBPSKCostasDecoder;
import decoder.FoxBPSK.FoxBPSKDecoder;
import decoder.FoxBPSK.FoxBPSKDotProdDecoder;
import decoder.Decoder;
import decoder.EyeData;
import decoder.Fox9600bpsDecoder;
import gui.GraphCanvas;

/** 
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
 * Draw an eye diagram based on a set of bit buckets that are passed
 * We get a set of data from the decoder because it is read in a chunk from the source (sound card etc).  We want to draw the set of bits
 * one on top of another and then refresh continually.
 * 
 * We call repaint every 1/50 sec and draw the set of bits that we have stored.
 * 
 *
 */
@SuppressWarnings("serial")
public class EyePanel extends JPanel implements Runnable {
	boolean done = false;
	boolean running = true;
	Decoder decoder;
	int zeroValue;
	//double[] snr;
	int[][] buffer;


	public double avgHigh;
	public double avgLow;
	public double sdHigh;
	public double sdLow;

	private double bitSNR;
	private int errors;
	private int erasures;
	private int clockOffset;
	//private double bitErrorRate;

	EyeData eyeData;
	JLabel title;
	Color GRAPH_COLOR;
	int border = 5;
	//int snrSample=0;
	//int snrAvgLen = 10;

	public static int NUMBER_OF_BITS = 50;  // We draw this many bits on the screen each time we refresh the eye diagram
	public static int SAMPLES = 120; // We draw this many pixels for each bit
	public EyePanel() {
		title = new JLabel("Eye Diagram");
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		add(title);
		GRAPH_COLOR = Color.BLUE; //new Color(153,0,0);
		//snr = new double[snrAvgLen];
		//snrSample = 0;
	}

	private void init() {
		if (decoder instanceof Fox9600bpsDecoder) SAMPLES = 5; 
		else if (decoder instanceof FoxBPSKDecoder) SAMPLES = 8;
		else SAMPLES = decoder.getBucketSize()/2;
		buffer = new int[NUMBER_OF_BITS][];
		for (int i=0; i < NUMBER_OF_BITS; i++) {
			buffer[i] = new int[SAMPLES];
		}
		//snr = new double[snrAvgLen];
		//snrSample = 0;
	}
	@Override
	public void run() {
		Thread.currentThread().setName("EyePanel");
		done = false;
		running = true;
		Log.println("STARTING EYE PANEL THREAD");
		int[][] data = null;
		while(running) {
			//			Log.println("eye running");
			try {
				Thread.sleep(1000/10); // approx 1/10 sec refresh
			} catch (InterruptedException e) {
				Log.println("ERROR: Eye Diagram thread interrupted");
				//e.printStackTrace();
			} 
			//Log.println("RUNNING EYE THREAD FOR: " + decoder.name);
			//if (decoder.name.equalsIgnoreCase("High Speed"))
			//	Log.println("STOP");

			if (decoder != null) {
				// We get the eye data, which is a copy of the bucket data

				eyeData = decoder.getEyeData();
				zeroValue = decoder.getZeroValue();
				if (eyeData != null) {
					// Cache the values while we graph them
					data = eyeData.getData();
					avgHigh = eyeData.getAvg(EyeData.HIGH);
					avgLow = eyeData.getAvg(EyeData.LOW);
					sdHigh = eyeData.getStandardDeviation(EyeData.HIGH);
					sdLow = eyeData.getStandardDeviation(EyeData.LOW);
					bitSNR = eyeData.bitSNR;
					errors = eyeData.lastErrorsCount;
					erasures = eyeData.lastErasureCount;
					clockOffset = eyeData.clockOffset;
				}
			}
			if (decoder != null && data != null ) { 
				init();
				int a=0; 
				int b=0;
				try {
					if (NUMBER_OF_BITS > data.length) NUMBER_OF_BITS = data.length;
					for (int i=0; i < NUMBER_OF_BITS; i++) {
						for (int j=0; j < decoder.getBucketSize(); j+=decoder.getBucketSize()/SAMPLES) {
							if (data !=null && a < NUMBER_OF_BITS && b < SAMPLES) {
								buffer[a][b++] = data[i][j];
							}
						}
						b=0;
						a++;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					// nothing to do at run time.  We switched decoders and the array length changed underneath us
					//Log.println("Ran off end of eye diagram data: a:" + a + " b:" + b);	
				}

			} else {
				//Log.println("NULL EYE DATA");
			}
			this.repaint();
		}
		Log.println("ENDING EYE PANEL THREAD");
	}

	public void updateFont() {
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
	}

	public void startProcessing(Decoder decoder1) {
		if (decoder != null) {
			// we were already live and we are swapping to a new decoder
		}
		decoder = decoder1;
		running = true;
	}

	//	public void stopProcessing() { 
	//		running = false;
	//	}

	/*
	 * Draw on a panel, where x is horizontal from left to right and y is vertical from top to bottom
	 * Draw a line segment for each sample, from the previous sample
	 * drawline x1,y1,y2,y2
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent( g ); // call superclass's paintComponent  
		Graphics2D g2 = ( Graphics2D ) g; // cast g to Graphics2D  

		// Have 5 pix border
		int graphHeight = getHeight() - border;
		int graphWidth = getWidth() - border*2;

		// Draw baseline with enough space for text under it
		g2.drawLine(0, graphHeight-border, graphWidth+border*2, graphHeight-border);
		// Draw vertical axis
		g2.drawLine(border*2, getHeight()-border, border*2, border*4);
		// Draw vertical end axis
		g2.drawLine(graphWidth, getHeight()-border, graphWidth, border*4);

		int lastx = border*2+1; 
		int lasty = graphHeight/2;
		int x = border*2+1;

		g2.setColor(GRAPH_COLOR);

		//int step = 20;
		//if (Config.highSpeed)
		//	step = 1;
		//int spaceSize = 1;
		int maxValue = (int)(Decoder.MAX_VOLUME/1.5);
		int minValue = (int)(-1*Decoder.MAX_VOLUME/1.5);
		if (decoder instanceof FoxBPSKDecoder || decoder instanceof FoxBPSKCostasDecoder
				|| decoder instanceof FoxBPSKDotProdDecoder) {
			maxValue = (int)(Decoder.MAX_VOLUME*1.5);
			minValue = (int)(-1*Decoder.MAX_VOLUME*1.5);
		}
		// Check that buffer has been populated all the way to the end
		if (buffer != null && buffer[NUMBER_OF_BITS-1] != null) {
			try {
				for (int i=0; i < NUMBER_OF_BITS; i++) {
					for (int j=0; j < SAMPLES; j++) {
						x = border*2 + j*(graphWidth-border*2)/(SAMPLES-1);
						//double y = graphHeight/2+graphHeight/2.5*buffer[i][j]/FoxDecoder.MAX_VOLUME + border;
						double y = GraphCanvas.getRatioPosition(minValue, maxValue, buffer[i][j]*0.5, graphHeight);
						if (j==0) {
							lastx = x;
							lasty = (int)y;
						}
						g2.drawLine(lastx, lasty, x, (int)y);
						lastx = x;
						lasty = (int)y;
					}
				}
			} catch (NullPointerException e) {
				// this means the buffer was changed while we were drawing it
				//Log.println("Eye Data buffer changed while drawing it");
			} catch (ArrayIndexOutOfBoundsException e) {
				// this means the buffer was changed while we were drawing it
				//Log.println("Eye Data buffer changed while drawing it");
			}

		} else {
			//Log.println("Eye Data truncated");
		}
		g2.setColor(Color.GRAY);
		// Center (decode) line
		double h = graphHeight/2+graphHeight/3*zeroValue/FoxDecoder.MAX_VOLUME+border;
		g2.drawLine(0, (int)h, graphWidth, (int)h);

		//sample.setText("sample: " + s++);

		// Draw the eye facts
		g2.setColor(Config.AMSAT_RED);

		int width = 30;

		double low = GraphCanvas.getRatioPosition(minValue, maxValue, avgLow*0.5, graphHeight);
		g2.drawLine(graphWidth/2-width + border*2, (int)low, graphWidth/2+width + border*2, (int)low);


		double high = GraphCanvas.getRatioPosition(minValue, maxValue, avgHigh*0.5, graphHeight);
		g2.drawLine(graphWidth/2-width + border*2, (int)high, graphWidth/2+width + border*2, (int)high);

		g2.drawLine(graphWidth/2 + border*2 , (int)high, graphWidth/2 + border*2, (int)low);

		double r = GraphPanel.roundToSignificantFigures(bitSNR,2);
		String s = Double.toString(r) + "";
		g.drawString(s, graphWidth/2 -25  + border*2, (int)(low + high)/2 + 5  );  // Height is the middle of the SNR bars
		g.drawString("SNR", graphWidth/2 + 10  + border*2, (int)(low + high)/2 + 5  );  // Height is the middle of the SNR bars

		//debug
		//g.drawString("HIGH:"+avgHigh, graphWidth/2 + 10  + border, 10  );
		//g.drawString("LOW:"+avgLow, graphWidth/2 + 10  + border, graphHeight - 30  );

		g2.setColor(Color.GRAY);
		if (Config.debugValues) {
			g.drawString("Clk Offset  "+clockOffset, 10  + border, 40    );
			//g.drawString("Offset Typ  "+offsetType, 10  + border, 60    );
		}
		g.drawString("Errors  "+errors, graphWidth/2 - 70  + border, graphHeight - 10  );
		g.drawString("Erasures  "+erasures, graphWidth/2 - 0  + border, graphHeight - 10  );
		//g.drawString("BER:"+bitErrorRate, graphWidth/2 + 35  + border, graphHeight - 10  );
		//erasures++;  // test to see if the window is updating
	}

}
