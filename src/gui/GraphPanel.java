package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;

import common.Config;
import common.Spacecraft;
import measure.SatMeasurementStore;
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
	double[][][] graphData = null;
	double[][][] graphData2 = null;
	double[] firstDifference = null;
	double[] dspData = null;
	int[][] timePeriod = null; // The time period for the graph reset count and uptime
	public static final int MAX_VARIABLES = 13;
	Color[] graphColor = {Color.BLUE, Config.GRAPH1, Config.GRAPH2, Config.GRAPH3, Config.GRAPH4, Config.GRAPH5, Config.GRAPH6, 
			Config.GRAPH7, Config.GRAPH8, Config.GRAPH9, Config.GRAPH10 , Config.GRAPH11 , Config.GRAPH12};
	Color graphAxisColor = Color.BLACK;
	Color graphTextColor = Color.DARK_GRAY;
	
	String title = "Test Graph";
	//String[] fieldName = null;
	
	HashMap<Integer, Long> maxPlottedUptimeForReset;
	
	int sideLabel = 0;
	int bottomLabelOffset = 5;
	
	int freqOffset = 0;
	
	int topBorder = (int)(Config.graphAxisFontSize*2); //Config.graphAxisFontSize; // The distance from the top of the drawing surface to the graph.  The title goes in this space
	int bottomBorder = (int)(Config.graphAxisFontSize*2.5); // The distance from the bottom of the drawing surface to the graph
	int sideBorder = (int)(Config.graphAxisFontSize *5); // left side border.  The position that the axis is drawn and the graph starts
	int sideLabelOffset = (int)(Config.graphAxisFontSize *0.5); // offset before we draw a label on the vertical axis
	static int labelWidth = (int)(6.7 * Config.graphAxisFontSize);  // 40 was a bit too tight for 6 digit uptime
	static int labelHeight = (int)(Config.graphAxisFontSize * 1.4);

	private static int MAX_TICKS = 4096/labelWidth;

	Graphics2D g2;
	Graphics g;
	
	GraphFrame graphFrame;
	private int conversionType;  
	private int payloadType; // This is RT, MAX, MIN, RAD, Measurement etc
	
	GraphPanel(String t, int conversionType, int plType, GraphFrame gf, Spacecraft sat) {
		title = t;
		payloadType = plType;
		this.conversionType = conversionType;
		//this.fieldName = fieldName;
		graphFrame = gf;
		freqOffset = sat.telemetryDownlinkFreqkHz * 1000;
		fox = sat;
		updateGraphData("GrapPanel.new");
	}

	public void updateGraphData(String by) {
		graphData = new double[graphFrame.fieldName.length][][];
		for (int i=0; i<graphFrame.fieldName.length; i++) {
			if (payloadType == FramePart.TYPE_REAL_TIME)
				graphData[i] = Config.payloadStore.getRtGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			else if (payloadType == FramePart.TYPE_MAX_VALUES)
				graphData[i] = Config.payloadStore.getMaxGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			else if (payloadType == FramePart.TYPE_MIN_VALUES)
				graphData[i] = Config.payloadStore.getMinGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			else if (payloadType == FramePart.TYPE_RAD_TELEM_DATA)
				graphData[i] = Config.payloadStore.getRadTelemGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			else if (payloadType == FramePart.TYPE_HERCI_SCIENCE_HEADER)
				graphData[i] = Config.payloadStore.getHerciScienceHeaderGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
				graphData[i] = Config.payloadStore.getMeasurementGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
				graphData[i] = Config.payloadStore.getPassMeasurementGraphData(graphFrame.fieldName[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
			
		}
		graphData2 = null;
		if (graphFrame.fieldName2 != null && graphFrame.fieldName2.length > 0) {
			graphData2 = new double[graphFrame.fieldName2.length][][];
			for (int i=0; i<graphFrame.fieldName2.length; i++) {
				if (payloadType == FramePart.TYPE_REAL_TIME)
					graphData2[i] = Config.payloadStore.getRtGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				else if (payloadType == FramePart.TYPE_MAX_VALUES)
					graphData2[i] = Config.payloadStore.getMaxGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				else if (payloadType == FramePart.TYPE_MIN_VALUES)
					graphData2[i] = Config.payloadStore.getMinGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				else if (payloadType == FramePart.TYPE_RAD_TELEM_DATA)
					graphData2[i] = Config.payloadStore.getRadTelemGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				else if (payloadType == FramePart.TYPE_HERCI_SCIENCE_HEADER)
					graphData2[i] = Config.payloadStore.getHerciScienceHeaderGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
					graphData2[i] = Config.payloadStore.getMeasurementGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
					graphData2[i] = Config.payloadStore.getPassMeasurementGraphData(graphFrame.fieldName2[i], graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
				
			}
		}
		//System.err.println("-repaint by: " + by);
		if (graphData != null && graphData[0] != null && graphData[0][0].length > 0)
			this.repaint();
	}
	
	private void drawLegend(int graphWidth, int titleHeight) {
		if (graphFrame.fieldName.length == 1 && graphFrame.fieldName2 == null) return;
		
		int verticalOffset = 15;
		int lineLength = 15;
		
		int rows = graphFrame.fieldName.length;
		int font = (int)(9 * Config.graphAxisFontSize / 11 );
		
		int leftLineOffset = 50;
		int fonth = (int)(12*font/9);
		int fontw = (int)(8*font/10);
		if (graphFrame.fieldName2 != null)
			rows = rows + graphFrame.fieldName2.length;
		
		int longestWord = 20;
		for (int i=0; i < graphFrame.fieldName.length; i++)
			if (graphFrame.fieldName[i].length() > longestWord)
				longestWord = graphFrame.fieldName[i].length();
		
		if (graphFrame.fieldName2 != null)
		for (int i=0; i < graphFrame.fieldName2.length; i++)
			if (graphFrame.fieldName2[i].length() > longestWord)
				longestWord = graphFrame.fieldName2[i].length();
		int leftOffset = longestWord * fontw + 20; // the point where we start drawing the box from the right edge of the graph
		
		g.setFont(new Font("SansSerif", Font.PLAIN, font));
		g2.drawRect(sideBorder + graphWidth - leftOffset - 1, titleHeight + 4,longestWord * fontw +1 , 9 + fonth * rows +1  );
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(sideBorder + graphWidth - leftOffset, titleHeight + 5, longestWord * fontw , 9 + fonth * rows  );
		
		for (int i=0; i < graphFrame.fieldName.length; i++) {
			g2.setColor(Color.BLACK);
			g2.drawString(graphFrame.fieldName[i]+" ("+graphFrame.fieldUnits+")", sideBorder+ graphWidth - leftOffset + 2, titleHeight + verticalOffset +5 + i * fonth );
			g2.setColor(graphColor[i]);
			g2.fillRect(sideBorder + graphWidth - leftLineOffset, titleHeight + verticalOffset + i * fonth, lineLength + 5,2);
		}
		
		verticalOffset =+ verticalOffset + graphFrame.fieldName.length * fonth;
		
		if (graphFrame.fieldName2 != null)
		for (int i=0; i < graphFrame.fieldName2.length; i++) {
			g2.setColor(Color.BLACK);
			g2.drawString(graphFrame.fieldName2[i]+" ("+graphFrame.fieldUnits2+")", sideBorder + graphWidth - leftOffset + 2, titleHeight + verticalOffset +5 + i * fonth );
			g2.setColor(graphColor[graphFrame.fieldName.length + i]);
			g2.fillRect(sideBorder + graphWidth - leftLineOffset, titleHeight + verticalOffset + i * fonth, lineLength + 5,2);
		}
	}
	
	/*
	 * Draw on a panel, where x is horizontal from left to right and y is vertical from top to bottom
	 * Draw a line segment for each sample, from the previous sample
	 * drawline x1,y1,x2,y2
	 */
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		
		if (graphData == null) return;
		if (graphData[0] == null) return;
		if (graphData[0][0] == null) return;
		if (graphData[0][0].length == 0) return;
		
		maxPlottedUptimeForReset = new HashMap();
		
		if (graphFrame.showUTCtime && !graphFrame.hideUptime) {
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
		
		if (graphFrame.plotDerivative) {
			firstDifference = new double[graphData[0][0].length];	
		}

		double[] axisPoints2 = {0d, 0d, 0d};
		if (graphData2 != null) {
			axisPoints2 = plotVerticalAxis(graphWidth, graphHeight, graphWidth, graphData2, false, graphFrame.fieldUnits2, graphFrame.conversionType2); // default graph type to 0 for now
		}
		
		double[] axisPoints = plotVerticalAxis(0, graphHeight, graphWidth, graphData, graphFrame.showHorizontalLines,graphFrame.fieldUnits, conversionType);
		
		
		int zeroPoint = (int) axisPoints[0];
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		// Analyze the data for the horizontal axis next
		// We only need to do this for the first data set (if there are multiple)
		// First check to see if we have more than one reset
		// Remember that the data STARTS with the most recent records and goes back in time
		// We will also look for large jumps in the Uptime meaning that data was taken from a later day or pass
		// If so, we make an artifical Reset boundry
		ArrayList<Integer> resetPosition = new ArrayList<Integer>();
		resetPosition.add(0); // We have a reset at position 0 of course
		
		double currentReset = graphData[0][PayloadStore.RESETS_COL][0];
		double lastUptime = graphData[0][PayloadStore.UPTIME_COL][0];
		
		for (int i=1; i < graphData[0][PayloadStore.RESETS_COL].length; i++) {
			if (graphData[0][PayloadStore.RESETS_COL][i] != currentReset) {
				// We have another reset in this data
				// Add this position to the array.  It is the FIRST piece of data that has this reset
				resetPosition.add(i);
				currentReset = graphData[0][PayloadStore.RESETS_COL][i];
			} else {
				if (graphFrame.UPTIME_THRESHOLD != GraphFrame.CONTINUOUS_UPTIME_THRESHOLD)
					// We dont have a reset, but maybe a big gap in the data
					if ((graphData[0][PayloadStore.UPTIME_COL][i] - lastUptime) > graphFrame.UPTIME_THRESHOLD) {
						resetPosition.add(i);
						currentReset = graphData[0][PayloadStore.RESETS_COL][i];
					}
			}
			lastUptime = graphData[0][PayloadStore.UPTIME_COL][i];
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

		// draw the key
		drawLegend(graphWidth, topBorder); // FIXME - need to work out where to plot the key when the axis is on top
		
		// Draw baseline at the zero point, but not the labels, which are drawn for each reset
		g2.setColor(graphAxisColor);
		g2.drawLine(sideLabelOffset, zeroPoint, graphWidth+sideBorder, zeroPoint);
		g2.setColor(graphTextColor);
		int offset = 0;
		if (!graphFrame.hideUptime) {
			g2.drawString("Uptime", sideLabelOffset, zeroPoint+Config.graphAxisFontSize );
			offset = Config.graphAxisFontSize;
		}
		//g2.setColor(graphColor);
		if (!graphFrame.showUTCtime)
			g2.drawString("Resets", sideLabelOffset, zeroPoint+1*Config.graphAxisFontSize + offset );
		else {
			g2.drawString("UTC", sideLabelOffset, zeroPoint+(int)(1.5*Config.graphAxisFontSize)+offset );
			g.setFont(new Font("SansSerif", Font.PLAIN, (int)(Config.graphAxisFontSize*0.9)));
			g2.drawString("(Spacecraft UTC is approximate)", graphWidth-Config.graphAxisFontSize*10, titleHeight );
			g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		}

//		System.out.println("Found " + resetPosition.size() + " resets at: ");
//		for (int q=0; q < resetPosition.size(); q++ )
//		System.out.println(" -" + graphData[PayloadStore.UPTIME_COL][resetPosition.get(q)] + " and ");
		
		
		/*
		 * Now we cycle through the resets and draw them one after another.  We will draw the labels on the horizontal axis with each
		 * reset.  There are a few things that we need to keep in mind
		 * 1. If "continuous" is checked then we are spacing the resets out according to the uptime.
		 * 2. If a reset is very narrow then we may not have room to draw a label, so we calculate the maximum number of labels for the window
		 *    size and work out if we need to skip drawing some labels so they will fit
		 */
		int maxLabels = (int)(graphWidth/(labelWidth*2));
		int resetLabelFreq = (int) Math.ceil(resetPosition.size() / (double)maxLabels); // reduce the number of labels if we have too many resets
		int resetLabelCount = 0;
		boolean drawLabels = false; // set to false if we want to skip labels on the reset we are going to plot
		for (int r=0; r < resetPosition.size(); r++) {
//			int resets = (int) graphData[PayloadStore.RESETS_COL][resetPosition.get(r)];
//			System.out.println("Processing reset " + resets);
			
			// Grab the paramaters we need to pass to the routine that draws the part of the graph for this reset
			int startScreenPos = 0;
			int width = 0;
			int start = resetPosition.get(r); // The position in the data array that this reset starts
			int end = 0; // The position in the data array that this reset starts. It is one before the next reset or the end of the data
			int len = graphData[0][PayloadStore.DATA_COL].length;
			startScreenPos = sideBorder + (graphWidth * start)/len;
			if (r == resetPosition.size()-1) // then we are at the last reset
				end = len; // this reset runs to the end of the data
			else
				end = resetPosition.get(r+1); // Data will be One data position before the next reset, but we calculate width up to same point
			width = (graphWidth * (end - start )/len);  // -labelWidth/2;
			if (width <1) width =1;
			resetLabelCount++;
			if (graphFrame.showContinuous || resetLabelCount == resetLabelFreq) {
				drawLabels = true;
				resetLabelCount = 0;
			} else {
				drawLabels = false;
			}
			drawGraphForSingleReset(start, end, width, graphHeight, startScreenPos, zeroPoint, axisPoints[1], axisPoints[2], 
					axisPoints2[1], axisPoints2[2], drawLabels);
		}
				
	}
	
	private double[] plotVerticalAxis(int axisPosition, int graphHeight, int graphWidth, double[][][] graphData, boolean showHorizontalLines, String units, int graphType) {
		
		// Draw vertical axis - always in the same place
		g2.drawLine(sideBorder + axisPosition, getHeight()-bottomBorder, sideBorder+axisPosition, topBorder);
		
		//Analyze the vertical data first because it also determines where we draw the baseline		 
				double maxValue = 0;
				double minValue = 99E99;
			//	double maxValueAxisTwo = 0;
			//	double minValueAxisTwo = 99E99;

				for (int j=0; j < graphData.length; j++)
					for (int i=0; i < graphData[j][0].length; i++) {
						if (graphData[j][PayloadStore.DATA_COL][i] >= maxValue) maxValue = graphData[j][PayloadStore.DATA_COL][i];
						if (graphData[j][PayloadStore.DATA_COL][i] <= minValue) minValue = graphData[j][PayloadStore.DATA_COL][i];
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

//				} else {
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
				
				boolean intStep = false;
				if (graphType == BitArrayLayout.CONVERT_INTEGER)
					intStep = true;
				// calculate the label step size
				double[] labels = calcAxisInterval(minValue, maxValue, numberOfLabels, intStep);
				// check the actual number
				numberOfLabels = labels.length;
				
				boolean foundZeroPoint = false;
				int zeroPoint = graphHeight + topBorder; // 10 is the default font size

				DecimalFormat f1 = new DecimalFormat("0.0");
				DecimalFormat f2 = new DecimalFormat("0");
				
				int fudge = 0; // fudge factor so that labels on the right axis are offset by the side border, otherwise they would be drawn to left of axis
				if (axisPosition > 0) fudge = sideBorder + (int)(Config.graphAxisFontSize);
				
				g2.setColor(graphTextColor);
				g2.drawString("("+units+")", fudge+axisPosition+sideLabelOffset, topBorder -(int)(Config.graphAxisFontSize/2)); 
				
				for (int v=0; v < numberOfLabels; v++) {
					
					int pos = getRatioPosition(minValue, maxValue, labels[v], graphHeight);
					pos = graphHeight-pos;
					String s = null;
					if (labels[v] == Math.round(labels[v]))
						s = f2.format(labels[v]);
					else
						s = f1.format(labels[v]);
					
					boolean drawLabel = true;
					// dont draw a label at the zero point or just below it because we have axis labels there, unless
					// this is the second axis
					if ( v < numberOfLabels-1 
							&& !((axisPosition == 0) && (labels[v] == 0.0 || labels[v+1] == 0.0))
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
								s = RadiationPacket.radPacketStateShort[0];
								drawLabel = true;
							}
							if (labels[v] == 2) {
								s = RadiationPacket.radPacketStateShort[1];
								drawLabel = true;
							}
							if (labels[v] == 3) {
								s = RadiationPacket.radPacketStateShort[2];
								drawLabel = true;
							}
							if (labels[v] == 4) {
								s = RadiationPacket.radPacketStateShort[3];
								drawLabel = true;
							}
							if (labels[v] == 5) {
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
							
							g2.drawString(s, fudge+axisPosition+sideLabelOffset, pos+topBorder+(int)(Config.graphAxisFontSize/2)); // add 4 to line up with tick line
							g2.setColor(graphAxisColor);
							if (showHorizontalLines) {
								g2.setColor(Color.GRAY);
								g2.drawLine(sideBorder-5, pos+topBorder, graphWidth+sideBorder, pos+topBorder);
								g2.setColor(graphTextColor);
							} else
								g.drawLine(sideBorder+axisPosition-5, pos+topBorder, sideBorder+axisPosition+5, pos+topBorder);
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
				double[] axisPoint = new double[3];
				axisPoint[0] = zeroPoint;
				axisPoint[1] = minValue;
				axisPoint[2] = maxValue;
				return axisPoint;
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
	private void drawGraphForSingleReset(int start, int end, int graphWidth, int graphHeight, 
			int sideBorder, int zeroPoint, double minValue, double maxValue, double minValue2, double maxValue2, boolean drawLabels) {
		
		

		double maxTimeValue = 0;
		double minTimeValue = 99999999;
		for (int i=start; i < end; i++) {
			if (graphData[0][PayloadStore.UPTIME_COL][i] >= maxTimeValue) maxTimeValue = graphData[0][PayloadStore.UPTIME_COL][i];
			if (graphData[0][PayloadStore.UPTIME_COL][i] <= minTimeValue) minTimeValue = graphData[0][PayloadStore.UPTIME_COL][i];
			if (graphFrame.plotDerivative && i > 0) {
				double value = graphData[0][PayloadStore.DATA_COL][i];
				double value2 = graphData[0][PayloadStore.DATA_COL][i-1];
				if (conversionType == BitArrayLayout.CONVERT_FREQ) {
					value = value - freqOffset;
					value2 = value2 - freqOffset;
				}
				firstDifference[i] = 5 * ((value - value2) / (graphData[0][PayloadStore.UPTIME_COL][i]-graphData[0][PayloadStore.UPTIME_COL][i-1]));
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

		// draw the labels if that was passed in.  Only draw the labels for continuous graphs if we have room for one
		// For non continuous graphs the calling function makes sure there is room, so we will always draw one,
		// except if this is the last reset, because we don't want to draw a label off the right end of the graph
		if (drawLabels && (numberOfTimeLabels > 0 || !graphFrame.showContinuous) 
				&& (numberOfTimeLabels > 0 || start < graphData[0][PayloadStore.RESETS_COL].length-1)) {  
			// calculate the label step size
			double[] timelabels = calcAxisInterval(minTimeValue, maxTimeValue, numberOfTimeLabels, true);
			numberOfTimeLabels = timelabels.length;
			int resets = (int) graphData[0][PayloadStore.RESETS_COL][start];

			boolean firstLabel = true;
			DecimalFormat d = new DecimalFormat("0");
			for (int v=0; v < numberOfTimeLabels; v++) {
				if ( ! (maxPlottedUptimeForReset.containsKey(resets) && maxPlottedUptimeForReset.get(resets) > timelabels[v]) ) {
					maxPlottedUptimeForReset.put(resets, (long) timelabels[v]); // otherwise store this uptime as the maximum value
					int timepos = getRatioPosition(minTimeValue, maxTimeValue, timelabels[v], graphWidth) + 2;

					if (!graphFrame.showContinuous && numberOfTimeLabels == 1) timepos = 1; // We are just plotting the first label
					// dont draw the label if we are too near the start or end
					if ((graphFrame.showContinuous && timepos > 0 && graphWidth > timepos && (graphWidth - timepos) > labelWidth/2)
							// this is when continuous not ticked.  We only draw labels in the range, but we may not quite fit in.  
							// This is a compromise so we draw enough labels in short resets
							|| (!graphFrame.showContinuous && timepos > 0 && graphWidth > timepos)
							// this is for rests with 1 value. It might cause an overlap but we live with that
							|| (!graphFrame.showContinuous && numberOfTimeLabels == 1)) {  

						String s = d.format(timelabels[v]);

						int offset = 0;
						if (!graphFrame.hideUptime) {
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

						if (!graphFrame.hideUptime) {
							g2.drawString(s, timepos+sideBorder+2, zeroPoint+Config.graphAxisFontSize );

						}
						if (graphFrame.showUTCtime) {
							if (fox.hasTimeZero(resets)) {
								g2.drawString(fox.getUtcTimeForReset(resets, (long)timelabels[v]), timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset);
								g2.drawString(""+fox.getUtcDateForReset(resets, (long)timelabels[v]), timepos+sideBorder+2, zeroPoint+2 * Config.graphAxisFontSize +offset);
							}
						}
						g2.setColor(graphAxisColor);
						if (graphFrame.showVerticalLines) {
							g2.setColor(Color.GRAY);
							g.drawLine(timepos+sideBorder, graphHeight + topBorder+5, timepos+sideBorder, topBorder);
						} else
							g.drawLine(timepos+sideBorder, zeroPoint-5, timepos+sideBorder, zeroPoint+5);
						firstLabel = false;
					}
				}
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

	//	int skip = 0;
		plotGraph(graphData, graphHeight, graphWidth, start, end, stepSize, sideBorder, minTimeValue, maxTimeValue, minValue, maxValue, 0, conversionType);
		if (graphData2 != null)
			plotGraph(graphData2, graphHeight, graphWidth, start, end, stepSize, sideBorder, minTimeValue, maxTimeValue, minValue2, maxValue2, graphFrame.fieldName.length, graphFrame.conversionType2);
		
	}

	private void plotGraph(double[][][] graphData, int graphHeight, int graphWidth, int start, int end, int stepSize, int sideBorder, double minTimeValue, 
			double maxTimeValue, double minValue, double maxValue, int colorIdx, int graphType) {
		if (graphData != null)
			
			/**
			 * Calculate a running average if the user selected it.  We can only do this if we have enough data
			 * 
			 */
			if (graphFrame.dspAvg) {
				if (graphFrame.AVG_PERIOD > graphData[0][0].length/2)
					graphFrame.AVG_PERIOD = graphData[0][0].length /2 ;
				double sum = 0;
				boolean first = true;
				dspData = new double[graphData[0][0].length];
				for (int i=graphFrame.AVG_PERIOD/2; i < graphData[0][0].length-graphFrame.AVG_PERIOD/2; i++) {
					sum = 0;
					for (int j=0; j< graphFrame.AVG_PERIOD; j++)
						sum += graphData[0][PayloadStore.DATA_COL][i+j-graphFrame.AVG_PERIOD/2];
					sum = sum / (double)(graphFrame.AVG_PERIOD);
					dspData[i] = sum;
					if (first) {
						for (int j=0; j<graphFrame.AVG_PERIOD/2; j++)
							dspData[j] = sum;
						first = false;
					}
				}
				for (int j=graphData[0][0].length-graphFrame.AVG_PERIOD; j<graphData[0][0].length; j++)
					dspData[j] = sum;
			}
			
			
			
			for (int j=0; j<graphData.length; j++) {
				int lastx = sideBorder+1; 
				int lastx2 = sideBorder+1;
				int lasty = graphHeight/2;
				int lasty2 = graphHeight/2;
				int lasty3 = graphHeight/2;
				int x = 0;
				int x2 = 0;
				int y = 0;
				int y2=0;
				int y3=0;
				for (int i=start; i < end; i+=stepSize) {

					// calculate the horizontal position of this point based on the number of points and the width
					x = getRatioPosition(minTimeValue, maxTimeValue, graphData[j][PayloadStore.UPTIME_COL][i], graphWidth);
					x = x + sideBorder;
					x2 = (x + lastx)/2; // position for the first deriv
					//				System.out.println(x + " graphData " + graphData[i]);

					// Calculate the ratio from min to max
					if (graphType == BitArrayLayout.CONVERT_ANTENNA || graphType == BitArrayLayout.CONVERT_STATUS_BIT 
							|| graphType == BitArrayLayout.CONVERT_BOOLEAN  || graphType == BitArrayLayout.CONVERT_VULCAN_STATUS ) 
						//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i]+1, graphHeight);
					else if (graphType == BitArrayLayout.CONVERT_FREQ) {
						//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i]-freqOffset, graphHeight);
						if (graphFrame.plotDerivative)
							y2 = getRatioPosition(minValue, maxValue, firstDifference[i], graphHeight);
						if (graphFrame.dspAvg)
							y3 = getRatioPosition(minValue, maxValue, dspData[i]-freqOffset, graphHeight);
					} else {
						//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i], graphHeight);
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
						lastx2=x2;
						lasty=y;
						lasty2=y2;
						lasty3=y3;
					}
					if (!graphFrame.hideMain) {
						g2.setColor(graphColor[j+colorIdx]);
						if (!graphFrame.hideLines) g2.drawLine(lastx, lasty, x, y);
						if (!graphFrame.hidePoints) g2.draw(new Ellipse2D.Double(x-1, y-1, 2,2));
					}

					if (graphFrame.plotDerivative) {
						g2.setColor(Config.AMSAT_RED);
						if (!graphFrame.hideLines) g2.drawLine(lastx2, lasty2, x2, y2);
						if (!graphFrame.hidePoints) g2.draw(new Ellipse2D.Double(x2, y2,2,2));
					}
					if (graphFrame.dspAvg) {
						g2.setColor(Config.AMSAT_GREEN);
						if (!graphFrame.hideLines) g2.drawLine(lastx, lasty3, x, y3);
						if (!graphFrame.hidePoints) g2.draw(new Ellipse2D.Double(x, y3,2,2));
					}

					
					lastx = x;
					lastx2 = x2;
					lasty = y;
					lasty2 = y2;
					lasty3 = y3;
				}
			}
	}

	/**
	 * Given a number of ticks across a window and the range, calculate the tick size
	 * and return an array of tick values to use on an axis.  It will have one of the step sizes
	 * calculated by the stepFunction
	 * 
	 * @param range
	 * @param ticks
	 * @return
	 */
	static double[] calcAxisInterval(double min, double max, int ticks, boolean intStep) {
		double range = max - min;
		if (ticks ==0) ticks = 1;
		double step = 0.0;

		// From the range and the number of ticks, work out a suitable tick size
		step = getStep(range, ticks, intStep);
		
		// We don't want labels that plot off the end of the graph, so reduce the ticks if needed
		ticks = (int) Math.ceil(range / step);
		// Now find the first value before the minimum.
		double startValue = roundToSignificantFigures(Math.round(min/step) * step, 6);

		// We want ticks that go all the way to the end, so resize the tick list if needed

//		int newticks = (int)((max - startValue) / step + 1);
//		if (newticks < ticks * 3 && newticks > ticks) {
//			ticks = newticks;
//			step = getStep(range, ticks);
//		}
		if (ticks > MAX_TICKS) {
			ticks = MAX_TICKS;  // safety check
			step = getStep(range, ticks, intStep);
		}
		if (ticks < 0) {
			ticks = 1;
			step = getStep(range, ticks, intStep);
		}
		
		double[] tickList = new double[ticks];

		if (ticks == 1) { // special case where we do not have room for a label so only some labels are plotted
			//double midValue = roundToSignificantFigures(Math.round(range/2/step) * step, 6);
			tickList[0] = startValue;
		} else 	if (ticks == 2) {
			double midValue = roundToSignificantFigures(startValue + step, 6);
			tickList[0] = startValue;
			tickList[1] = midValue;
		} else
		if (ticks > 0)
			tickList[0] = startValue;
		for (int i=1; i< ticks; i++) {
			startValue = roundToSignificantFigures(startValue + step, 6);
			//val = Math.round(val/step) * step;
			tickList[i] = startValue;
		}
		
		return tickList;
	}

	private static double getStep(double range, int ticks, boolean intStep) {
		double step = 0;
		
		if (!intStep && range/ticks <= 0.01) step = 0.01d;
		else if (!intStep && range/ticks <= 0.1) step = 0.10d;
		else if (!intStep && range/ticks <= 0.2) step = 0.20d;
		else if (!intStep && range/ticks <= 0.25) step = 0.25d;
		else if (!intStep && range/ticks <= 0.33) step = 0.33d;
		else if (!intStep && range/ticks <= 0.5) step = 0.50d;
		else if (range/ticks <= 1) step = 1.00d;
		else if (range/ticks <= 2) step = 2.00d;
		else if (!intStep && range/ticks <= 2.5) step = 2.50d;
		else if (!intStep && range/ticks <= 3.3) step = 3.33d;
		else if (range/ticks <= 5) step = 5.00d;
		else if (range/ticks <= 10) step = 10.00d;
		else if (range/ticks <= 25) step = 25.00d;
		else if (!intStep && range/ticks <= 33) step = 33.33d;
		else if (range/ticks <= 50) step = 50.00d;
		else if (range/ticks <= 100) step = 100.00d;
		else if (range/ticks <= 200) step = 200.00d;
		else if (range/ticks <= 250) step = 250.00d;
		else if (!intStep && range/ticks <= 333) step = 333.33d;
		else if (range/ticks <= 500) step = 500.00d;
		else if (range/ticks <= 1000) step = 1000.00d;
		else if (range/ticks <= 2000) step = 2000.00d;
		else if (range/ticks <= 2500) step = 2500.00d;
		else if (!intStep && range/ticks <= 3333) step = 3333.33d;
		else if (range/ticks <= 5000) step = 5000.00d;
		else if (range/ticks <= 10000) step = 10000.00d;
		else if (range/ticks <= 20000) step = 20000.00d;
		else if (range/ticks <= 25000) step = 25000.00d;
		else if (!intStep && range/ticks <= 33333) step = 33333.33d;
		else if (range/ticks <= 50000) step = 50000.00d;
		else if (range/ticks <= 100000) step = 100000.00d;
		else if (range/ticks <= 250000) step = 250000.00d;
		else if (!intStep && range/ticks <= 333333) step = 333333.33d;
		else if (range/ticks <= 500000) step = 500000.00d;
		else if (range/ticks <= 1000000) step = 1000000.00d;
		else if (range/ticks <= 2000000) step = 2000000.00d;
		else if (range/ticks <= 2500000) step = 2500000.00d;
		else if (!intStep && range/ticks <= 3333333) step = 3333333.33d;
		else if (range/ticks <= 5000000) step = 5000000.00d;
		else if (range/ticks <= 10000000) step = 10000000.00d;
		return step;
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
