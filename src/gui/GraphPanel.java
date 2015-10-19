package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

import common.Config;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.PayloadStore;
import telemetry.RadiationPacket;

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
 * Draw a graph on a JPanel
 * 
 * 
 */
@SuppressWarnings("serial")
public class GraphPanel extends JPanel {
	Spacecraft fox;
	double[][] graphData = null;
	double[] firstDifference = null;
	double[] dspData = null;
	int[][] timePeriod = null; // The time period for the graph reset count and uptime
	Color graphColor = Color.BLUE;
	Color graphAxisColor = Color.BLACK;
	Color graphTextColor = Color.DARK_GRAY;
	
	String title = "Test Graph";
	String fieldName = null;
	
	int sideLabel = 0;
	int bottomLabelOffset = 5;
	
	int freqOffset = 0;
	
	int topBorder = 0; //Config.graphAxisFontSize; // The distance from the top of the drawing surface to the graph.  The title goes in this space
	int bottomBorder = (int)(Config.graphAxisFontSize*2.5); // The distance from the bottom of the drawing surface to the graph
	int sideBorder = (int)(Config.graphAxisFontSize *4); // left side border.  The position that the axis is drawn and the graph starts
	int sideLabelOffset = (int)(Config.graphAxisFontSize *0.5); // offset before we draw a label on the vertical axis
	static int labelWidth = 6 * Config.graphAxisFontSize;;  // 40 was a bit too tight for 6 digit uptime
	static int labelHeight = (int)(Config.graphAxisFontSize * 1.4);

	private static final int MAX_TICKS = 4096/labelWidth;

	Graphics2D g2;
	Graphics g;
	
	GraphFrame graphFrame;
	private int graphType;
	private int payloadType;
	
	GraphPanel(String t, String fieldName, int conversionType, int plType, GraphFrame gf, Spacecraft sat) {
		title = t;
		payloadType = plType;
		graphType = conversionType;
		this.fieldName = fieldName;
		graphFrame = gf;
		freqOffset = sat.telemetryDownlinkFreqkHz * 1000;
		fox = sat;
		updateGraphData();
	}

	public void updateGraphData() {
		if (payloadType == FramePart.TYPE_REAL_TIME)
			graphData = Config.payloadStore.getRtGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		else if (payloadType == FramePart.TYPE_MAX_VALUES)
			graphData = Config.payloadStore.getMaxGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		else if (payloadType == FramePart.TYPE_MIN_VALUES)
			graphData = Config.payloadStore.getMinGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		else if (payloadType == FramePart.TYPE_RAD_TELEM_DATA)
			graphData = Config.payloadStore.getRadTelemGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		else if  (payloadType == 0) // measurement
			graphData = Config.payloadStore.getMeasurementGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		
		if (graphData != null && graphData[0].length > 0)
			this.repaint();
	}
	
	
	/*
	 * Draw on a panel, where x is horizontal from left to right and y is vertical from top to bottom
	 * Draw a line segment for each sample, from the previous sample
	 * drawline x1,y1,x2,y2
	 */
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		if (graphData[0].length == 0) return;
		if (graphFrame.showUTCtime && graphFrame.showUptime) {
			bottomBorder = (int)(Config.graphAxisFontSize*3.5);
		} else {
			bottomBorder = (int)(Config.graphAxisFontSize*2.5);
		}
			
		g2 = ( Graphics2D ) gr; // cast g to Graphics2D  
		g = gr;
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		// Have border above and below
		
		int graphHeight = getHeight() - topBorder - bottomBorder;
		int graphWidth = getWidth() - sideBorder*2; // width of entire graph
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		// Draw vertical axis - always in the same place
		g2.drawLine(sideBorder, getHeight()-bottomBorder, sideBorder, topBorder);

		
		if (graphFrame.plotDerivative) {
			firstDifference = new double[graphData[0].length];	
		}

		//Analyze the vertical data first because it also determines where we draw the baseline		 
		double maxValue = 0;
		double minValue = 99E99;
	//	double maxValueAxisTwo = 0;
	//	double minValueAxisTwo = 99E99;
				
		for (int i=0; i < graphData[0].length; i++) {
			if (graphData[PayloadStore.DATA_COL][i] >= maxValue) maxValue = graphData[PayloadStore.DATA_COL][i];
			if (graphData[PayloadStore.DATA_COL][i] <= minValue) minValue = graphData[PayloadStore.DATA_COL][i];
		}

		if (maxValue == minValue) {
			if (graphType == BitArrayLayout.CONVERT_INTEGER) 
				maxValue = maxValue + 10;
			else
				maxValue = maxValue + 1;
		}
		
		if (graphType == BitArrayLayout.CONVERT_SPIN) {
			maxValue = 8;
			minValue = -8;
					
		}

		if (graphType == BitArrayLayout.CONVERT_VULCAN_STATUS) {
			maxValue = 5;
			minValue = 0;
					
		}

		
		if (graphType == BitArrayLayout.CONVERT_ANTENNA || graphType == BitArrayLayout.CONVERT_STATUS_BIT || graphType == BitArrayLayout.CONVERT_BOOLEAN) {
			maxValue = 2;
			minValue = 0;		
		}
	
		if (graphType == BitArrayLayout.CONVERT_FREQ) {
			maxValue = maxValue - freqOffset;
			minValue = minValue - freqOffset;
		}
		
		if (graphFrame.plotDerivative) {
			// We want to include zero in the graph
			if (minValue > 0) minValue = maxValue * -0.1;
			if (maxValue < 0) maxValue = minValue * -0.1;
		}
		// Scale the max and min values so we do not crash into the top and bottom of the window
	//	if (graphType == BitArrayLayout.CONVERT_FREQ) {

//		} else {
		if (maxValue < 0)
			maxValue = maxValue - maxValue * 0.20;
		else
			maxValue = maxValue + maxValue * 0.20;
		if (minValue < 0)
			minValue = minValue + minValue * 0.20;
		else
			minValue = minValue - minValue * 0.20;
	//	}
		// calculate number of labels we need on vertical axis
		int numberOfLabels = (graphHeight)/labelHeight;
		
		// calculate the label step size
		double[] labels = calcAxisInterval(minValue, maxValue, numberOfLabels);
		// check the actual number
		numberOfLabels = labels.length;
		
		boolean foundZeroPoint = false;
		int zeroPoint = graphHeight + topBorder; // 10 is the default font size

		DecimalFormat f1 = new DecimalFormat("0.0");
		DecimalFormat f2 = new DecimalFormat("0");
		
		for (int v=0; v < numberOfLabels; v++) {
			
			int pos = getRatioPosition(minValue, maxValue, labels[v], graphHeight);
			pos = graphHeight-pos;
			String s = null;
			if (labels[v] == Math.round(labels[v]))
				s = f2.format(labels[v]);
			else
				s = f1.format(labels[v]);
			// dont draw a label at the zero point or just below it because we have axis labels there
			boolean drawLabel = true;
			if (v < numberOfLabels-1 
					&& !(labels[v] == 0.0 || labels[v+1] == 0.0)
					&& !(v == 0 && pos > graphHeight)
					) {
				if (graphType == BitArrayLayout.CONVERT_ANTENNA) {
					drawLabel = false;
					if (labels[v] == 1) {
						s = "STWD";
						drawLabel = true;
					}
					if (labels[v] == 2) {
						s = "DEP";
						drawLabel = true;
					}
					
				} 
				if (graphType == BitArrayLayout.CONVERT_STATUS_BIT) {
					drawLabel = false;
					if (labels[v] == 1) {
						s = "OK";
						drawLabel = true;
					}
					if (labels[v] == 2) {
						s = "FAIL";
						drawLabel = true;
					}
					
				} 

				if (graphType == BitArrayLayout.CONVERT_VULCAN_STATUS) {
					drawLabel = false;
					if (labels[v] == 1) {
						s = RadiationPacket.radPacketStateShort[1];
						drawLabel = true;
					}
					if (labels[v] == 2) {
						s = RadiationPacket.radPacketStateShort[2];
						drawLabel = true;
					}
					if (labels[v] == 3) {
						s = RadiationPacket.radPacketStateShort[3];
						drawLabel = true;
					}
					if (labels[v] == 4) {
						s = RadiationPacket.radPacketStateShort[4];
						drawLabel = true;
					}
					
				} 

				
				if (graphType == BitArrayLayout.CONVERT_BOOLEAN) {
					drawLabel = false;
					if (labels[v] == 2) {
						s = "TRUE";
						drawLabel = true;
					}
					if (labels[v] == 1) {
						s = "FALSE";
						drawLabel = true;
					}
					
				} 
				
				if (drawLabel && pos > 0 && (pos < graphHeight-Config.graphAxisFontSize/2))	{ 
					g2.setColor(graphTextColor);
					g2.drawString(s, sideLabelOffset, pos+topBorder+(int)(Config.graphAxisFontSize/2)); // add 4 to line up with tick line
					g2.setColor(graphAxisColor);
					g.drawLine(sideBorder-5, pos+topBorder, sideBorder+5, pos+topBorder);
				}
			}
			if (!foundZeroPoint) {
				if (labels[v] >= 0) {
					foundZeroPoint = true;
					if ( v ==0 )
						zeroPoint = graphHeight+topBorder; // 10 is the default font size;
					else
						zeroPoint = pos+topBorder;
				}
			}
		}

		// Analyze the data for the horizontal axis next
		// First check to see if we have more than one reset
		// Remember that the data STARTS with the most recent records and goes back in time
		// We will also look for large jumps in the Uptime meaning that data was taken from a later day or pass
		// If so, we make an artifical Reset boundry
		ArrayList<Integer> resetPosition = new ArrayList<Integer>();
		resetPosition.add(0); // We have a reset at position 0 of course
		
		double currentReset = graphData[PayloadStore.RESETS_COL][0];
		double lastUptime = graphData[PayloadStore.UPTIME_COL][0];
		
		for (int i=1; i < graphData[PayloadStore.RESETS_COL].length; i++) {
			if (graphData[PayloadStore.RESETS_COL][i] != currentReset) {
				// We have another reset in this data
				// Add this position to the array.  It is the FIRST piece of data that has this reset
				resetPosition.add(i);
				currentReset = graphData[PayloadStore.RESETS_COL][i];
			} else {
				if (graphFrame.UPTIME_THRESHOLD != GraphFrame.CONTINUOUS_UPTIME_THRESHOLD)
					// We dont have a reset, but maybe a big gap in the data
					if ((graphData[PayloadStore.UPTIME_COL][i] - lastUptime) > graphFrame.UPTIME_THRESHOLD) {
						resetPosition.add(i);
						currentReset = graphData[PayloadStore.RESETS_COL][i];
					}
			}
			lastUptime = graphData[PayloadStore.UPTIME_COL][i];
		}

		int titleHeight = Config.graphAxisFontSize+10;
		if (zeroPoint < Config.graphAxisFontSize*3) {
			// we need the title at bottom of graph, not top
			titleHeight = graphHeight + topBorder;
		}
		
		// Draw the title
				g2.setColor(Color.BLACK);
				g.setFont(new Font("SansSerif", Font.BOLD, Config.graphAxisFontSize+3));
				g2.drawString(graphFrame.displayTitle, sideBorder/2 + graphWidth/2 - graphFrame.displayTitle.length()/2 * Config.graphAxisFontSize/2, titleHeight);
				
				g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
				
		
		// Draw baseline at the zero point, but not the labels, which are drawn for each reset
		g2.drawLine(sideLabelOffset, zeroPoint, graphWidth+sideBorder, zeroPoint);
		g2.setColor(graphTextColor);
		int offset = 0;
		if (graphFrame.showUptime) {
			g2.drawString("Uptime", sideLabelOffset, zeroPoint+Config.graphAxisFontSize );
			offset = Config.graphAxisFontSize;
		}
		//g2.setColor(graphColor);
		if (!graphFrame.showUTCtime)
			g2.drawString("Resets", sideLabelOffset, zeroPoint+1*Config.graphAxisFontSize + offset );
		else
			g2.drawString("UTC", sideLabelOffset, zeroPoint+(int)(1.5*Config.graphAxisFontSize)+offset );
		

//		System.out.println("Found " + resetPosition.size() + " resets at: ");
		for (int q=0; q < resetPosition.size(); q++ )
//		System.out.println(" -" + graphData[PayloadStore.UPTIME_COL][resetPosition.get(q)] + " and ");
		
		for (int r=0; r < resetPosition.size(); r++) {
//			int resets = (int) graphData[PayloadStore.RESETS_COL][resetPosition.get(r)];
//			System.out.println("Processing reset " + resets);
			int startScreenPos = 0;
			int width = 0;
			int start = resetPosition.get(r); // The position in the data array that this reset starts
			int end = 0; // The position in the data array that this reset starts. It is one before the next reset or the end of the data
			int len = graphData[PayloadStore.DATA_COL].length;
			startScreenPos = sideBorder + (graphWidth * start)/len;
			if (r == resetPosition.size()-1) // then we are at the last reset
				end = len; // this reset runs to the end of the data
			else
				end = resetPosition.get(r+1); // Data will be One data position before the next reset, but we calculate width up to same point
			width = (graphWidth * (end - start )/len);
			drawGraphForSingleReset(start, end, width, graphHeight, startScreenPos, zeroPoint, minValue, maxValue);
		}
				
	}
	
	
	/**
	 * Draw a graph from sideBorder to graphWidth.  This is for a single reset.  If the graph has more than one reset
	 * in the data, then it is partitioned according to the number of samples in each reset.  The graph is then drawn with the
	 * samples aligned to the upTime
	 * 
	 * @param resets
	 * @param start - the position in the data where this reset starts
	 * @param end - the position in the data where this reset ends
	 * @param graphWidth
	 * @param graphHeight
	 * @param sideBorder
	 * @param zeroPoint
	 * @param minValue
	 * @param maxValue
	 */
	private void drawGraphForSingleReset(int start, int end, int graphWidth, int graphHeight, int sideBorder, int zeroPoint, double minValue, double maxValue) {
		
		/**
		 * Calculate a running average if the user selected it.  We can only do this if we have enough data
		 * 
		 */
		if (graphFrame.dspAvg) {
			if (graphFrame.AVG_PERIOD > graphData[0].length/2)
				graphFrame.AVG_PERIOD = graphData[0].length /2 ;
			double sum = 0;
			boolean first = true;
			dspData = new double[graphData[0].length];
			for (int i=graphFrame.AVG_PERIOD/2; i < graphData[0].length-graphFrame.AVG_PERIOD/2; i++) {
				sum = 0;
				for (int j=0; j< graphFrame.AVG_PERIOD; j++)
					sum += graphData[PayloadStore.DATA_COL][i+j-graphFrame.AVG_PERIOD/2];
				sum = sum / (double)(graphFrame.AVG_PERIOD);
				dspData[i] = sum;
				if (first) {
					for (int j=0; j<graphFrame.AVG_PERIOD/2; j++)
						dspData[j] = sum;
					first = false;
				}
			}
			for (int j=graphData[0].length-graphFrame.AVG_PERIOD; j<graphData[0].length; j++)
				dspData[j] = sum;
		}

		double maxTimeValue = 0;
		double minTimeValue = 99999999;
		for (int i=start; i < end; i++) {
			if (graphData[PayloadStore.UPTIME_COL][i] >= maxTimeValue) maxTimeValue = graphData[PayloadStore.UPTIME_COL][i];
			if (graphData[PayloadStore.UPTIME_COL][i] <= minTimeValue) minTimeValue = graphData[PayloadStore.UPTIME_COL][i];
			if (graphFrame.plotDerivative && i > 0) {
				double value = graphData[PayloadStore.DATA_COL][i];
				double value2 = graphData[PayloadStore.DATA_COL][i-1];
				if (graphType == BitArrayLayout.CONVERT_FREQ) {
					value = value - freqOffset;
					value2 = value2 - freqOffset;
				}
				firstDifference[i] = 5 * ((value - value2) / (graphData[PayloadStore.UPTIME_COL][i]-graphData[PayloadStore.UPTIME_COL][i-1]));
			//	if (firstDifference[i] < minValue) minValue = firstDifference[i];
			//	if (firstDifference[i] > maxValue) maxValue = firstDifference[i];
			}
		}
		/* Second Deriv
		if (graphFrame.plotDerivative)
		for (int i=start; i < end; i++) {
			if (i>0)
			firstDifference[i] = 5 * ((firstDifference[i] - firstDifference[i-1]) / (graphData[PayloadStore.UPTIME_COL][i]-graphData[PayloadStore.UPTIME_COL][i-1]));
		}
		 */
		int numberOfTimeLabels = graphWidth/labelWidth;
		
		// calculate the label step size
		double[] timelabels = calcAxisInterval(minTimeValue, maxTimeValue, numberOfTimeLabels);
		numberOfTimeLabels = timelabels.length;
		int resets = (int) graphData[PayloadStore.RESETS_COL][start];
	
		boolean firstLabel = true;
		DecimalFormat d = new DecimalFormat("0");
		for (int v=0; v < numberOfTimeLabels; v++) {
			int timepos = getRatioPosition(minTimeValue, maxTimeValue, timelabels[v], graphWidth);
			if (timepos == graphWidth) {
				// We only have 1 piece of data to plot
			}
			
			// dont draw the label if we are too near the start or end
			if (timepos > 0 && (graphWidth - timepos) > labelWidth/2) {
				String s = d.format(timelabels[v]);

				int offset = 0;
				if (graphFrame.showUptime) {
					offset = Config.graphAxisFontSize;	
				}
				if ( firstLabel) {
					g2.setColor(graphTextColor);
					if (!graphFrame.showUTCtime)
						g2.drawString(""+resets, timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset );
					//else
					//	g2.drawString(""+fox.getUtcDateforReset(resets, timepos), timepos+sideBorder+2, zeroPoint+2 * Config.graphAxisFontSize );	
					
				}
				
				g2.setColor(graphTextColor);
				
				if (graphFrame.showUptime) {
					g2.drawString(s, timepos+sideBorder+2, zeroPoint+Config.graphAxisFontSize );
				
				}
				if (graphFrame.showUTCtime) {
					if (fox.hasTimeZero(resets)) {
						g2.drawString(fox.getUtcTimeforReset(resets, (long)timelabels[v]), timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset);
						g2.drawString(""+fox.getUtcDateforReset(resets, (long)timelabels[v]), timepos+sideBorder+2, zeroPoint+2 * Config.graphAxisFontSize +offset);
					}
				}
				g2.setColor(graphAxisColor);
				g.drawLine(timepos+sideBorder, zeroPoint-5, timepos+sideBorder, zeroPoint+5);
				firstLabel = false;
			}
		}

		
		//
		// Either the data is too long for the window, then we skip some of it
		// Or the window is too long for the data, so we space it out
		int stepSize = 1;
		//int spaceSize = 1;

		if (end - start > graphWidth && graphWidth != 0) {
			stepSize = Math.round((end - start)/graphWidth);
		} else {
			// we leave step size at 1 and plot all of the points, but space them out.
			//spaceSize = graphWidth/(end - start);
		}

		int lastx = sideBorder+1; 
		int lasty = graphHeight/2;
		int lasty2 = graphHeight/2;
		int lasty3 = graphHeight/2;
		int x = 0;
		int y = 0;
		int y2=0;
		int y3=0;
	//	int skip = 0;
		g2.setColor(graphColor);
		
		if (graphData != null)
			for (int i=start; i < end; i+=stepSize) {

				// calculate the horizontal position of this point based on the number of points and the width
				x = getRatioPosition(minTimeValue, maxTimeValue, graphData[PayloadStore.UPTIME_COL][i], graphWidth);
				x = x + sideBorder;
//				System.out.println(x + " graphData " + graphData[i]);
								
				// Calculate the ratio from min to max
				if (graphType == BitArrayLayout.CONVERT_ANTENNA || graphType == BitArrayLayout.CONVERT_STATUS_BIT || graphType == BitArrayLayout.CONVERT_BOOLEAN ) 
					//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[PayloadStore.DATA_COL][i]+1, graphHeight);
				else if (graphType == BitArrayLayout.CONVERT_FREQ) {
					//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[PayloadStore.DATA_COL][i]-freqOffset, graphHeight);
					if (graphFrame.plotDerivative)
						y2 = getRatioPosition(minValue, maxValue, firstDifference[i], graphHeight);
					if (graphFrame.dspAvg)
						y3 = getRatioPosition(minValue, maxValue, dspData[i]-freqOffset, graphHeight);
				} else {
					//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[PayloadStore.DATA_COL][i], graphHeight);
					if (graphFrame.plotDerivative)
						y2 = getRatioPosition(minValue, maxValue, firstDifference[i], graphHeight);
					if (graphFrame.dspAvg)
						y3 = getRatioPosition(minValue, maxValue, dspData[i], graphHeight);
				}
			y=graphHeight-y+topBorder;
			y2=graphHeight-y2+topBorder;
			y3=graphHeight-y3+topBorder;
				
//				System.out.println(x + " value " + value);
				if (i == start) {
					lastx=x;
					lasty=y;
					lasty2=y2;
					lasty3=y3;
				}
				if (graphFrame.displayMain)
					g2.drawLine(lastx, lasty, x, y);
				
				if (graphFrame.plotDerivative) {
					g2.setColor(Config.AMSAT_RED);
					g2.drawLine(lastx, lasty2, x, y2);
				}
				if (graphFrame.dspAvg) {
					g2.setColor(Config.AMSAT_GREEN);
					g2.drawLine(lastx, lasty3, x, y3);
				}
				
				g2.setColor(graphColor);
				lastx = x;
				lasty = y;
				lasty2 = y2;
				lasty3 = y3;
			}
	}
	
	/**
	 * Given a number of ticks across a window and the range, calculate the tick size
	 * and return an array of tick values to use on an axis.  It will have one of the following step sizes:
	 * 0.01
	 * 0.1
	 * 1
	 * 10
	 * 100
	 * 1000
	 * @param range
	 * @param ticks
	 * @return
	 */
	static double[] calcAxisInterval(double min, double max, int ticks) {
		double range = max - min;
		double t = ticks;
		double step = 0.0;
		
		// From the range and the number of ticks, work out a suitable tick size
		if (range/t <= 0.01) step = 0.01d;
		else if (range/t <= 0.1) step = 0.10d;
		else if (range/t <= 0.5) step = 0.50d;
		else if (range/t <= 1) step = 1.00d;
		else if (range/t <= 5) step = 5.00d;
		else if (range/t <= 10) step = 10.00d;
		else if (range/t <= 25) step = 25.00d;
		else if (range/t <= 50) step = 50.00d;
		else if (range/t <= 100) step = 100.00d;
		else if (range/t <= 200) step = 200.00d;
		else if (range/t <= 500) step = 500.00d;
		else if (range/t <= 1000) step = 1000.00d;
		else if (range/t <= 2000) step = 2000.00d;
		else if (range/t <= 5000) step = 5000.00d;
		else if (range/t <= 10000) step = 10000.00d;
		else if (range/t <= 50000) step = 50000.00d;
		else if (range/t <= 100000) step = 100000.00d;
		else if (range/t <= 500000) step = 500000.00d;
		else if (range/t <= 1000000) step = 1000000.00d;
		else if (range/t <= 5000000) step = 5000000.00d;

		// Now find the first value before the minimum.
		double startValue = roundToSignificantFigures(Math.round(min/step) * step, 6);

		// We want ticks that go all the way to the end, so resize the tick list if needed

		int newticks = (int)((max - startValue) / step + 1);
		if (newticks < ticks * 3 && newticks > ticks)
			ticks = newticks;
		if (ticks > MAX_TICKS) ticks = MAX_TICKS;  // safety check
		if (ticks < 0) ticks = 2;
		
		double[] tickList = new double[ticks];

		if (ticks > 0)
			tickList[0] = startValue;
		for (int i=1; i< ticks; i++) {
			startValue = roundToSignificantFigures(startValue + step, 6);
			//val = Math.round(val/step) * step;
			tickList[i] = startValue;
		}
		
		return tickList;
	}
	
	public static double roundToSignificantFigures(double num, int n) {
	    if(num == 0) {
	        return 0;
	    }

	    final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
	    final int power = n - (int) d;

	    final double magnitude = Math.pow(10, power);
	    final long shifted = Math.round(num*magnitude);
	    return shifted/magnitude;
	}
	
	private int getRatioPosition(double min, double max, double value, int dimension) {
		double ratio = (max - value) / (max - min);
		int position = (int)Math.round(dimension * ratio);
		return dimension-position;
	}
	
}