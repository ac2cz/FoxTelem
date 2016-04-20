package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;

import common.Config;
import common.Spacecraft;
import telemetry.PayloadStore;

/**
 * We use many of the core graph classes to draw a density plot. The first example will be a plot of signal strength vs
 * azimuth and elevation.
 * 
 * @author chris
 *
 */
public class DensityPlotPanel extends GraphCanvas {
	int[][] timePeriod = null; // The time period for the graph reset count and uptime

	DensityPlotPanel(String t, int conversionType, int plType, GraphFrame gf, Spacecraft sat) {
		super(t, conversionType, plType, gf, sat);
		updateGraphData("DensityPlotPanel.new");
	}
	
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		
		if (!checkDataExists()) return;
			
		int graphHeight = getHeight() - topBorder - bottomBorder;
		int graphWidth = getWidth() - sideBorder*2; // width of entire graph
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		

		// Create the Data Grid and the Horizontal Axis and a new version of graphData that will hold the data for the axis themselves, so the
		// standard routines can draw them
		double maxVert = 90.0;
		double maxHor = 360.0;
		int maxVertBoxes = 18*2;
		int maxHorBoxes = 36*2;
		
		double[][] dataGrid = new double[maxVertBoxes][maxHorBoxes]; // 10 degree sky segments
		int[][] dataGridCount = new int[maxVertBoxes][maxHorBoxes]; // 10 degree sky segments
		double[][][] axisGraphData = new double[1][3][maxHorBoxes];
		
		double vertStep = maxVert/(double)maxVertBoxes; // the step size for the vertical axis
		double horStep = maxHor/(double)maxHorBoxes; // the step size for the horixental axis
		
		// we have three sets of data:
		// EL is in variable 0, AZ in variable 1 and the value is in variable 2
		// We do not care about resets and uptime, we just running average the data into the grid
		for (int i=1; i < graphData[0][PayloadStore.DATA_COL].length; i++) {
			double vert = graphData[0][PayloadStore.DATA_COL][i];
			double hor = graphData[1][PayloadStore.DATA_COL][i];
			double value = graphData[2][PayloadStore.DATA_COL][i];
			if (Double.isNaN(value)) value = 0;
			
			// integrity check
			if (graphData[0][PayloadStore.UPTIME_COL][i] != graphData[2][PayloadStore.UPTIME_COL][i])
				System.err.println("ERROR!!!!!!!!!");
			
			int vertBox = (int)Math.round((vert/vertStep));
			int horBox = (int)Math.round((hor/horStep));
			if (vertBox < 0) vertBox = 0;
			if (horBox < 0) horBox = 0;
			if (vertBox >= maxVertBoxes) vertBox = maxVertBoxes-1;
			if (horBox >= maxHorBoxes) horBox = maxHorBoxes-1;
			dataGrid[vertBox][horBox] += value;
			
			dataGridCount[vertBox][horBox]++;
			
		}
		// now calculate the averages and store the fake values for the axis
		double maxValue = -999999999;
		double minValue = 999999999;
		for (int h=0; h < maxHorBoxes; h++) {
			for (int v=0; v < maxVertBoxes; v++) {
				if (dataGridCount[v][h] == 0)
					dataGrid[v][h] = 0;
				else
					dataGrid[v][h] = dataGrid[v][h] / (double)dataGridCount[v][h];
				if (dataGrid[v][h] != 0) {
					if (dataGrid[v][h] > maxValue) maxValue = dataGrid[v][h];
					if (dataGrid[v][h] < minValue) minValue = dataGrid[v][h];
				}
				if (h==0) {
					axisGraphData[0][PayloadStore.DATA_COL][v] = v * vertStep;
				}
			}
			axisGraphData[0][PayloadStore.RESETS_COL][h] = 0;
			axisGraphData[0][PayloadStore.UPTIME_COL][h] = h;
		}

		//double[] axisPoints = plotVerticalAxis(0, graphHeight, graphWidth, axisGraphData, graphFrame.showHorizontalLines,graphFrame.fieldUnits, conversionType);
		// Draw vertical axis - always in the same place
		g2.drawLine(sideBorder, getHeight()-bottomBorder, sideBorder, topBorder);
		int numberOfLabels = (graphHeight)/labelHeight;
		double[] labels = calcAxisInterval(0, maxVert, numberOfLabels, false);
		numberOfLabels = labels.length;
		g2.setColor(graphTextColor);
		g2.drawString("("+graphFrame.fieldUnits+")", sideLabelOffset, topBorder -(int)(Config.graphAxisFontSize/2)); 
		
		DecimalFormat f2 = new DecimalFormat("0");
		for (int v=1; v < numberOfLabels; v++) {
			
			int pos = getRatioPosition(0, maxVert, labels[v], graphHeight);
			pos = graphHeight-pos;
			String s = f2.format(labels[v]);

			g2.drawString(s, sideLabelOffset, pos+topBorder+(int)(Config.graphAxisFontSize/2)); 
			g2.setColor(graphAxisColor);
			//g.drawLine(sideBorder+axisPosition-5, pos+topBorder, sideBorder+axisPosition+5, pos+topBorder);
		}
		
		int zeroPoint = graphHeight + topBorder;
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));

		int titleHeight = Config.graphAxisFontSize+10;
		if (zeroPoint < Config.graphAxisFontSize*3) {
			// we need the title at bottom of graph, not top
			titleHeight = graphHeight + topBorder;
		}

		// Draw the title
		g2.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif", Font.BOLD, Config.graphAxisFontSize+3));
		graphFrame.displayTitle = "Fox-1A - Bit Signal to Noise vs Az El";
		g2.drawString(graphFrame.displayTitle, sideBorder/2 + graphWidth/2 - graphFrame.displayTitle.length()/2 * Config.graphAxisFontSize/2, titleHeight);

		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		// Draw baseline at the zero point
		g2.setColor(graphAxisColor);
		g2.drawLine(sideLabelOffset, zeroPoint, graphWidth+sideBorder, zeroPoint);
		g2.setColor(graphTextColor);
		int offset = 0;
		g2.drawString("Azimuth", sideLabelOffset, zeroPoint+1*Config.graphAxisFontSize + offset );

		// Plot the labels for the horizontal axis
		int numberOfTimeLabels = graphWidth/(labelWidth/2);
		double[] timelabels = calcAxisInterval(0, maxHor, numberOfTimeLabels, true);
		numberOfTimeLabels = timelabels.length;
		
		DecimalFormat d = new DecimalFormat("0");
		for (int h=0; h < numberOfTimeLabels; h++) {
			int timepos = getRatioPosition(0, maxHor, timelabels[h], graphWidth);
			g2.setColor(graphTextColor);
			g2.drawString(""+(long)timelabels[h], timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset);
		}
		
		int boxHeight = graphHeight / maxVertBoxes;
		int boxWidth = graphWidth / maxHorBoxes;

		for (int v=0; v < maxVertBoxes; v++)
			for (int h=0; h < maxHorBoxes; h++) {

				int x = getRatioPosition(0, maxHorBoxes, h, graphWidth) +sideBorder;
				int y = getRatioPosition(0, maxVertBoxes, v, graphHeight);

				double val = dataGrid[v][h];
				if (val == 0) val = -100;
				//minValue = -100;
				//maxValue = -10;
				int shade = getRatioPosition(minValue, maxValue, val, 255);
				if (shade > 255) shade = 255;
				if (shade <0) shade = 0;
				shade = 255-shade; // we want min signal white and max black
				g2.setColor(new Color(shade,shade,shade));
				g2.fillRect(x, graphHeight-y+topBorder-boxHeight, boxWidth, boxHeight);

				//
			}


	}
}
