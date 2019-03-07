package gui;

import java.awt.Color;
import java.awt.Dimension;
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
 * Draw a phasor diagram based on a set of phase data passed in
 * We get a set of data from the decoder because it is read in a chunk from the source (sound card etc).  We want to draw the phase
 * one on top of another and then refresh continually.
 * 
 * We call repaint every 1/50 sec and draw the set of bits that we have stored.
 * 
 *
 */
@SuppressWarnings("serial")
public class PhasorPanel extends JPanel implements Runnable {
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

	double[] phasorData;
	JLabel title;
	Color GRAPH_COLOR;
	int border = 5;
	//int snrSample=0;
	//int snrAvgLen = 10;

	public static int NUMBER_OF_BITS = 50;  // We draw this many bits on the screen each time we refresh the eye diagram
	public static int SAMPLES = 120; // We draw this many pixels for each bit
	public PhasorPanel() {
		title = new JLabel("Phasor");
		title.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		add(title);
		GRAPH_COLOR = Color.BLUE; //new Color(153,0,0);
		setMaximumSize(new Dimension(Short.MAX_VALUE,
                Short.MAX_VALUE));
	}
	
	

	private void init() {
	}
	@Override
	public void run() {
		Thread.currentThread().setName("PhasorPanel");
		done = false;
		running = true;
		Log.println("STARTING PHASOR PANEL THREAD");
		while(running) {
			try {
				Thread.sleep(1000/50); // approx 1/50 sec refresh
			} catch (InterruptedException e) {
				Log.println("ERROR: Phasor Diagram thread interrupted");
			} 

			if (decoder != null) {
				// We get the eye data, which is a copy of the bucket data

				if (decoder instanceof FoxBPSKCostasDecoder)
					phasorData = ((FoxBPSKCostasDecoder)decoder).getPhasorData();
				else
					phasorData = ((FoxBPSKDotProdDecoder)decoder).getPhasorData();
				EyeData eyeData = decoder.getEyeData();
				//				zeroValue = decoder.getZeroValue();
				if (eyeData != null) {
					errors = eyeData.lastErrorsCount;
					erasures = eyeData.lastErasureCount;
				}
			}
			this.repaint();
		}
		Log.println("ENDING PHASOR PANEL THREAD");
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
		int y = 0,x = border*2+1;

		g2.setColor(GRAPH_COLOR);
		if (phasorData != null) {
			for (int j=0; j < phasorData.length/2; j+=2) {	
				//			x = (int) (border*2 + phasorData[2*j]*(graphWidth-border*2));
				x = GraphCanvas.getRatioPosition(-1, 1, phasorData[2*j]*0.5, graphWidth);
				//			y = (int) (graphHeight/2+graphHeight/2.5*phasorData[2*j+1] + border);
				y = GraphCanvas.getRatioPosition(-1, 1, phasorData[2*j+1]*0.5, graphHeight);
				if (j==0) {
					lastx = x;
					lasty = (int)y;
				}
				g2.drawLine(x, y, x, (int)y);
				lastx = x;
				lasty = (int)y;
			}
		}
		g2.setColor(Color.GRAY);
		// Center (decode) line
		double h = graphHeight/2+graphHeight/3*zeroValue/FoxDecoder.MAX_VOLUME+border;
		g2.drawLine(0, (int)h, graphWidth, (int)h);

		// Draw the eye facts
		g2.setColor(Config.AMSAT_RED);

		int width = 30;

		//		double r = GraphPanel.roundToSignificantFigures(bitSNR,2);
		//		String s = Double.toString(r) + "";
		//		g.drawString(s, graphWidth/2 -25  + border*2, (int)(low + high)/2 + 5  );  // Height is the middle of the SNR bars
		//		g.drawString("SNR", graphWidth/2 + 10  + border*2, (int)(low + high)/2 + 5  );  // Height is the middle of the SNR bars

		g2.setColor(Color.GRAY);
		if (Config.debugValues) 	
			g.drawString("Clk Offset  "+clockOffset, 10  + border, 40    );
		g.drawString("Errors  "+errors, graphWidth/2 - 70  + border, graphHeight - 10  );
		g.drawString("Erasures  "+erasures, graphWidth/2 - 0  + border, graphHeight - 10  );
	}

}
