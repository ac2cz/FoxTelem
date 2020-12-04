package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import common.Config;
import common.FoxSpacecraft;
import predict.PositionCalcException;
import telemetry.BitArrayLayout;
import telemetry.Conversion;
import telemetry.ConversionStringLookUpTable;
import telemetry.PayloadStore;
import uk.me.g4dpz.satellite.SatPos;

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
public class GraphPanel extends GraphCanvas {
	
	public static final int NO_TIME_VALUE = -999;
	double[] firstDifference = null;
	double[] dspData = null;

	int[] plottedXreset;
	long[] plottedXuptime;
	int zeroPoint;
	
	public static final int MAX_VARIABLES = 13;
	Color[] graphColor = {Color.BLUE, Config.GRAPH1, Config.GRAPH2, Config.GRAPH3, Config.GRAPH4, Config.GRAPH5, Config.GRAPH6, 
			Config.GRAPH7, Config.GRAPH8, Config.GRAPH9, Config.GRAPH10 , Config.GRAPH11 , Config.GRAPH12};
	
	//String[] fieldName = null;
	
	HashMap<Integer, Long> maxPlottedUptimeForReset;
	
	int sideLabel = 0;
	int bottomLabelOffset = 5;
	
	GraphPanel(String t, GraphFrame gf, FoxSpacecraft fox2) {
		super(t, gf, fox2);
		freqOffset = (int) (fox2.user_telemetryDownlinkFreqkHz * 1000);
		updateGraphData(gf.layout, "GrapPanel.new");
	}

	
	private void drawLegend(int graphHeight, int graphWidth) {
		if (graphFrame.fieldName.length == 1 && graphFrame.fieldName2 == null) return;
		
		int titleHeight = topBorder;
		int verticalOffset = 15;
		if (zeroPoint < Config.graphAxisFontSize*3) {
			// we need the title at bottom of graph, not top
			titleHeight = graphHeight - 15;	
		}
		
		
		int lineLength = 15;
		
		int rows = graphFrame.fieldName.length;
		int font = (int)(Config.graphAxisFontSize );
		
		int leftLineOffset = 50;
		int fonth = (int)(12*font/9);
		int fontw = (int)(9*font/10);
		if (graphFrame.fieldName2 != null)
			rows = rows + graphFrame.fieldName2.length;

		if (graphFrame.plotDerivative) rows +=1;
		if (graphFrame.dspAvg) rows +=1;
		
		
		int longestWord = 20;
		for (int i=0; i < graphFrame.fieldName.length; i++)
			if (graphFrame.fieldName[i].length() > longestWord)
				longestWord = graphFrame.fieldName[i].length();
		
		if (graphFrame.fieldName2 != null)
		for (int i=0; i < graphFrame.fieldName2.length; i++)
			if (graphFrame.fieldName2[i].length() > longestWord)
				longestWord = graphFrame.fieldName2[i].length();
		int leftOffset = (int) (longestWord * 1.25* fontw + 20); // the point where we start drawing the box from the right edge of the graph
		
		g.setFont(new Font("SansSerif", Font.PLAIN, font));
		g2.setColor(Color.BLACK);
		g2.drawRect(sideBorder + graphWidth - leftOffset - 1, titleHeight + 4,leftOffset-10 , 9 + fonth * rows +1  );
		g2.setColor(Color.WHITE);
		g2.fillRect(sideBorder + graphWidth - leftOffset, titleHeight + 5, leftOffset-11 , 9 + fonth * rows  );
		
		
		for (int i=0; i < graphFrame.fieldName.length; i++) {
			g2.setColor(Color.BLACK);
			//String name = graphFrame.fieldName[i].toLowerCase();
			String name = graphFrame.layout.getModuleByName(graphFrame.fieldName[i]) + "-" + graphFrame.layout.getShortNameByName(graphFrame.fieldName[i]);
			g2.drawString(name+" ("+graphFrame.fieldUnits+")", sideBorder+ graphWidth - leftOffset + 2, titleHeight + verticalOffset +5 + i * fonth );
			g2.setColor(graphColor[i]);
			g2.fillRect(sideBorder + graphWidth - leftLineOffset, titleHeight + verticalOffset + i * fonth, lineLength + 5,2);
		}
		
		verticalOffset =+ verticalOffset + graphFrame.fieldName.length * fonth;
		
		if (graphFrame.fieldName2 != null) {
		for (int i=0; i < graphFrame.fieldName2.length; i++) {
			g2.setColor(Color.BLACK);
			//String name = graphFrame.fieldName2[i].toLowerCase();
			String name = graphFrame.layout.getModuleByName(graphFrame.fieldName2[i]) + "-" + graphFrame.layout.getShortNameByName(graphFrame.fieldName2[i]);
			g2.drawString(name+" ("+graphFrame.fieldUnits2+")", sideBorder + graphWidth - leftOffset + 2, titleHeight + verticalOffset +5 + i * fonth );
			g2.setColor(graphColor[graphFrame.fieldName.length + i]);
			g2.fillRect(sideBorder + graphWidth - leftLineOffset, titleHeight + verticalOffset + i * fonth, lineLength + 5,2);
		}
		verticalOffset =+ verticalOffset + graphFrame.fieldName2.length * fonth;
		}
		
		int x = 0;
		if (graphFrame.plotDerivative) {
			g2.setColor(Color.BLACK);
			g2.drawString("Der:" + graphFrame.fieldName[0]+" ("+graphFrame.fieldUnits+")", sideBorder+ graphWidth - leftOffset + 2, titleHeight + verticalOffset +5  );
			g2.setColor(Config.AMSAT_RED);
			g2.fillRect(sideBorder + graphWidth - leftLineOffset, titleHeight + verticalOffset, lineLength + 5,2);
			x = 1;
		}
		if (graphFrame.dspAvg) {
			g2.setColor(Color.BLACK);
			g2.drawString("Avg:" + graphFrame.fieldName[0]+" ("+graphFrame.fieldUnits+")", sideBorder+ graphWidth - leftOffset + 2, titleHeight + verticalOffset +5 + x * fonth );
			g2.setColor(Config.AMSAT_GREEN);
			g2.fillRect(sideBorder + graphWidth - leftLineOffset, titleHeight + verticalOffset + x * fonth, lineLength + 5,2);
		}
	}
	
	/*
	 * Draw on a panel, where x is horizontal from left to right and y is vertical from top to bottom
	 * Draw a line segment for each sample, from the previous sample
	 * drawline x1,y1,x2,y2
	 */
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		
		if (!checkDataExists()) return;
		
		maxPlottedUptimeForReset = new HashMap<Integer, Long>();
		
		
		// Have border above and below
		
		int graphHeight = getHeight() - topBorder - bottomBorder;
		int graphWidth = getWidth() - sideBorder*2; // width of entire graph
		if (graphWidth < 1) return;
		if (graphHeight < 1) return;
		
		plottedXreset = new int[graphWidth+1];
		plottedXuptime = new long[graphWidth+1];
		
		for (int j=0; j<graphWidth+1; j++) {
			plottedXreset[j] = NO_TIME_VALUE;
			plottedXuptime[j] = NO_TIME_VALUE; 
		}
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		if (graphFrame.plotDerivative) {
			firstDifference = new double[graphData[0][0].length];	
		}

		// Calculate the axis points and the labels, but dont draw the lines yet, even if user requested
		double[] axisPoints2 = {0d, 0d, 0d};
		if (drawGraph2) {
			axisPoints2 = plotVerticalAxis(graphWidth, graphHeight, graphWidth, graphData2, false, graphFrame.fieldUnits2, graphFrame.conversionType2, graphFrame.conversion2); // default graph type to 0 for now
		}
		
		double[] axisPoints = plotVerticalAxis(0, graphHeight, graphWidth, graphData, graphFrame.showHorizontalLines, graphFrame.fieldUnits, graphFrame.conversionType, graphFrame.conversion);
		
		
		zeroPoint = (int) axisPoints[0];
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

			if (!graphFrame.roundLabels)
				drawLabels = false;

			drawGraphForSingleReset(start, end, width, graphHeight, startScreenPos, zeroPoint, axisPoints[1], axisPoints[2], 
					axisPoints2[1], axisPoints2[2], drawLabels);
		}

		if (!graphFrame.roundLabels)
			plotAlternateLabels(zeroPoint, graphHeight);
				
//		// Now analyze the axis again and plot the lines this time if requested by user
//		if (graphFrame.showHorizontalLines)
//			plotVerticalAxis(0, graphHeight, graphWidth, graphData, graphFrame.showHorizontalLines,graphFrame.fieldUnits, conversionType);
		
		// draw the key
		drawLegend(graphHeight, graphWidth); // FIXME - need to work out where to plot the key when the axis is on top
				

	}

	private void plotAlternateLabels(int zeroPoint, int graphHeight) {

		int prevReset = -1;
		boolean firstLabel = true;
		DecimalFormat d = new DecimalFormat("0");
		int w = 0;
		for (int v=0; v < plottedXuptime.length; v++) {
			int resets = plottedXreset[v];
			long uptime = plottedXuptime[v];
			

			if (firstLabel || w++ >= labelWidth && plottedXuptime[v] != NO_TIME_VALUE) {
				firstLabel = false;
				w=0;
				int timepos = v;

				String s = d.format(uptime);
				int offset = 0;
				if (!graphFrame.hideUptime) {
					offset = Config.graphAxisFontSize;	
				}
				if (resets != prevReset)
				{
					g2.setColor(graphTextColor);
					if (!graphFrame.showUTCtime)
						if (resets != NO_TIME_VALUE)
							g2.drawString(""+resets, timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset );
				}

				g2.setColor(graphTextColor);

				if (!graphFrame.hideUptime) {
					if (uptime != NO_TIME_VALUE)
						g2.drawString(s, timepos+sideBorder+2, zeroPoint+Config.graphAxisFontSize );

				}
				if (graphFrame.showUTCtime) {
					//if (fox.isFox1()) {
						FoxSpacecraft fox2 = (FoxSpacecraft)fox;
						if (fox2.hasTimeZero(resets) && resets != NO_TIME_VALUE) {
							g2.drawString(fox2.getUtcTimeForReset(resets, uptime), timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset);
							g2.drawString(""+fox2.getUtcDateForReset(resets, uptime), timepos+sideBorder+2, zeroPoint+2 * Config.graphAxisFontSize +offset);
						}
					//}
				}
				g2.setColor(graphAxisColor);
				if (graphFrame.showVerticalLines) {
					g2.setColor(Color.GRAY);
					g.drawLine(timepos+sideBorder, graphHeight + topBorder+5, timepos+sideBorder, topBorder);
				} else
					g.drawLine(timepos+sideBorder, zeroPoint-5, timepos+sideBorder, zeroPoint+5);
				prevReset = resets;
			}
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
	 * @param minValue2
	 * @param maxValue2
	 * @param drawLabels - if false then no labels are drawn for this reset because it is too narrow
	 */
	private void drawGraphForSingleReset(int start, int end, int graphWidth, int graphHeight, 
			int sideBorder, int zeroPoint, double minValue, double maxValue, double minValue2, double maxValue2, boolean drawLabels) {
		
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

		double maxTimeValue = 0;
		double minTimeValue = 99999999;
		for (int i=start; i < end; i++) {
			if (graphData[0][PayloadStore.UPTIME_COL][i] >= maxTimeValue) maxTimeValue = graphData[0][PayloadStore.UPTIME_COL][i];
			if (graphData[0][PayloadStore.UPTIME_COL][i] <= minTimeValue) minTimeValue = graphData[0][PayloadStore.UPTIME_COL][i];
			if (graphFrame.plotDerivative && i > 0) {
				double value = graphData[0][PayloadStore.DATA_COL][i];
				double value2 = graphData[0][PayloadStore.DATA_COL][i-1];
				if (graphFrame.conversionType == BitArrayLayout.CONVERT_FREQ) {
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
		plotSun(graphData, graphHeight, graphWidth, start, end, stepSize, sideBorder, minTimeValue, 
				maxTimeValue, minValue, maxValue, 0, graphFrame.conversionType, true);
		plotGraph(graphData, graphHeight, graphWidth, start, end, stepSize, sideBorder, minTimeValue, 
				maxTimeValue, minValue, maxValue, 0, graphFrame.conversionType, graphFrame.conversion, true);
		if (drawGraph2)
			plotGraph(graphData2, graphHeight, graphWidth, start, end, stepSize, sideBorder, minTimeValue, 
					maxTimeValue, minValue2, maxValue2, graphFrame.fieldName.length, graphFrame.conversionType2, graphFrame.conversion2, false);
		
	}

	private void plotGraph(double[][][] graphData, int graphHeight, int graphWidth, int start, int end, int stepSize, int sideBorder, double minTimeValue, 
			double maxTimeValue, double minValue, double maxValue, int colorIdx, int graphType, Conversion conversion, boolean plotDsp) {
		if (graphData != null)
			for (int j=0; j<graphData.length; j++) {
				int lastx = sideBorder+1; 
				int nextx = 0; 
				//int lastMidPoint = 0;
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
					double p = graphData[j][PayloadStore.UPTIME_COL][i];
					x = getRatioPosition(minTimeValue, maxTimeValue, p, graphWidth);
					if (i < end-stepSize) {
						double q = graphData[j][PayloadStore.UPTIME_COL][i+stepSize];
					
						nextx = getRatioPosition(minTimeValue, maxTimeValue, q, graphWidth);
						nextx = nextx + sideBorder;
					} else
						nextx=0;
					
					x = x + sideBorder; // sideborder is the position of this reset, unless its the first one, which is equal to this.sideBorder

					plottedXreset[x-this.sideBorder] = (int) graphData[j][PayloadStore.RESETS_COL][i];
					plottedXuptime[x-this.sideBorder] = (long) graphData[j][PayloadStore.UPTIME_COL][i];

					x2 = (x + lastx)/2; // position for the first deriv
					//				System.out.println(x + " graphData " + graphData[i]);

					// Calculate the ratio from min to max
					if (!Config.displayRawValues && conversion != null && conversion instanceof ConversionStringLookUpTable) 
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i]+1, graphHeight);
					else if (!Config.displayRawValues && (graphType == BitArrayLayout.CONVERT_ANTENNA|| graphType == BitArrayLayout.CONVERT_STATUS_ENABLED
							|| graphType == BitArrayLayout.CONVERT_STATUS_BIT 
							|| graphType == BitArrayLayout.CONVERT_BOOLEAN  || graphType == BitArrayLayout.CONVERT_VULCAN_STATUS) ) 
						//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i]+1, graphHeight);
					else if (graphType == BitArrayLayout.CONVERT_FREQ) {
						//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i]-freqOffset, graphHeight);
						if (graphFrame.plotDerivative && plotDsp && j==0)
							y2 = getRatioPosition(minValue, maxValue, firstDifference[i], graphHeight);
						if (graphFrame.dspAvg && plotDsp && j==0)
							y3 = getRatioPosition(minValue, maxValue, dspData[i]-freqOffset, graphHeight);
					} else {
						//if (graphFrame.displayMain)
						y = getRatioPosition(minValue, maxValue, graphData[j][PayloadStore.DATA_COL][i], graphHeight);
						if (graphFrame.plotDerivative && plotDsp && j==0)
							y2 = getRatioPosition(minValue, maxValue, firstDifference[i], graphHeight);
						if (graphFrame.dspAvg && plotDsp && j==0)
							y3 = getRatioPosition(minValue, maxValue, dspData[i], graphHeight);
					}
					y=graphHeight-y+topBorder;
					y2=graphHeight-y2+topBorder;
					y3=graphHeight-y3+topBorder;

					//				System.out.println(x + " value " + value);
					if (i == start) {
						lastx=x;
						//lastMidPoint = x;
						lastx2=x2;
						lasty=y;
						lasty2=y2;
						lasty3=y3;
					}
					
					// Hide the trace if hideMain is set, unless this is not the first trace or we are not plotting deriv/dsp for this set of data
					if (!graphFrame.hideMain || ( graphFrame.hideMain && plotDsp && j > 0 ) || ( graphFrame.hideMain && !plotDsp)) {
						g2.setColor(graphColor[j+colorIdx]);
						if (!graphFrame.hideLines) g2.drawLine(lastx, lasty, x, y);
						if (!graphFrame.hidePoints) g2.draw(new Ellipse2D.Double(x-1, y-1, 2,2));
					}

					if (graphFrame.plotDerivative && plotDsp && j==0) {
						g2.setColor(Config.AMSAT_RED);
						if (!graphFrame.hideLines) g2.drawLine(lastx2, lasty2, x2, y2);
						if (!graphFrame.hidePoints) g2.draw(new Ellipse2D.Double(x2, y2,2,2));
					}
					if (graphFrame.dspAvg && plotDsp && j==0) {
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
	
	private void plotSun(double[][][] graphData, int graphHeight, int graphWidth, int start, int end, int stepSize, int sideBorder, double minTimeValue, 
			double maxTimeValue, double minValue, double maxValue, int colorIdx, int graphType, boolean plotDsp) {
		if (graphData != null)
			for (int j=0; j<graphData.length; j++) {
				int lastx = sideBorder+1; 
				int nextx = 0; 
				int lastMidPoint = 0;
				int x = 0;
			
				for (int i=start; i < end; i+=stepSize) {

					// calculate the horizontal position of this point based on the number of points and the width
					double p = graphData[j][PayloadStore.UPTIME_COL][i];
					x = getRatioPosition(minTimeValue, maxTimeValue, p, graphWidth);
					if (i < end-stepSize) {
						double q = graphData[j][PayloadStore.UPTIME_COL][i+stepSize];
					
						nextx = getRatioPosition(minTimeValue, maxTimeValue, q, graphWidth);
						nextx = nextx + sideBorder;
					} else
						nextx=0;
					
					x = x + sideBorder; // sideborder is the position of this reset, unless its the first one, which is equal to this.sideBorder

					// draw the sun if requested by user
					long up = (long) graphData[j][PayloadStore.UPTIME_COL][i];
					int res = (int) graphData[j][PayloadStore.RESETS_COL][i];
					SatPos pos = null;
					try {
						 pos = this.fox.getSatellitePosition(res, up);
					} catch (PositionCalcException e) {
						// Ignore, we just don't plot it
						pos = null;
					}
					
					if (i == start) {
						lastx=x;
						lastMidPoint = x;
					}
					
					if (graphFrame.showSun) {
						if (pos != null && pos.isEclipsed())
							g2.setColor(new Color(204,204,204)); // gray
						else
							g2.setColor(new Color(255,204,0)); // yellow

						int midPoint = 0;
						int w = 0; // width of sun rectangle
						if (nextx == 0)
							nextx = x;
						midPoint = x + (nextx-x)/2;
						w = midPoint - lastMidPoint;
						if (lastx == x) {
							// project forward half width.  Likely first point
							g2.fillRect(lastx, topBorder, w, graphHeight);	
						} else if (w == 0) {
							midPoint = x;
							g2.drawLine(x, topBorder, x, graphHeight+topBorder);
						} else {
							g2.fillRect(lastMidPoint, topBorder, w, graphHeight);
						//g2.fillRect(lastx, topBorder+5, x-lastx, graphHeight-5);
						}
						lastMidPoint = midPoint;
					}

					
					lastx = x;
				}
			}
	}

	
	
}
