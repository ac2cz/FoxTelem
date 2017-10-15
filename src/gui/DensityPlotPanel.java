package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;

import common.Config;
import common.FoxSpacecraft;
import telemetry.PayloadStore;

/**
 * We use many of the core graph classes to draw a density plot. The first example will be a plot of signal strength vs
 * azimuth and elevation.
 * 
 * @author chris
 *
 */
@SuppressWarnings("serial")
public class DensityPlotPanel extends GraphCanvas {
	int[][] timePeriod = null; // The time period for the graph reset count and uptime

	DensityPlotPanel(String t, int conversionType, int plType, GraphFrame gf, FoxSpacecraft sat) {
		super(t, conversionType, plType, gf, sat);
		updateGraphData("DensityPlotPanel.new");
	}
	private void drawLegend(int graphHeight, int graphWidth, double minValue, double maxValue, String units) {
			
		int verticalOffset = 60;
		int leftOffset = 15;
		int legendHeight = graphHeight-verticalOffset*2;

		int font = (int)(9 * Config.graphAxisFontSize / 11 );
		int fonth = (int)(12*font/9);

		int legendWidth = font*4;

		int numberOfLabels = legendHeight/labelHeight;
		double[] labels = calcAxisInterval(minValue, maxValue, numberOfLabels, false);
		if (labels.length > 0) {
			int rows = labels.length;
			int boxHeight = (int)legendHeight/rows;
			legendHeight = boxHeight*rows;

			g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
			g2.drawString("Key", sideBorder + graphWidth + leftOffset + 5, verticalOffset - fonth*2  );
			g2.drawString("("+units+")", sideBorder + graphWidth + leftOffset + 5, verticalOffset - fonth  );

			g.setFont(new Font("SansSerif", Font.PLAIN, font));
			for (int i=0; i < rows; i++) {
				int shade = getRatioPosition(minValue, maxValue, labels[i], 255);
				if (shade > 255) shade = 255;
				if (shade <0) shade = 0;
				shade = 255-shade; // we want min signal white and max black
				g2.setColor(new Color(shade,shade,shade));
				g2.fillRect(sideBorder + graphWidth + leftOffset, verticalOffset + i * boxHeight, legendWidth, boxHeight);
				if (shade > 127) g2.setColor(Color.BLACK);
				else g2.setColor(Color.WHITE);
				g2.drawString(""+labels[i], sideBorder + graphWidth + leftOffset+5, verticalOffset + i * boxHeight + (int)(boxHeight*0.8));

			}
		}
		
	}
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		
		if (!checkDataExists()) return;
		boolean noAzElReadings = true;	
		int graphHeight = getHeight() - topBorder - bottomBorder;
		int graphWidth = getWidth() - sideBorder*2; // width of entire graph
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		

		// Create the Data Grid and the Horizontal Axis and a new version of graphData that will hold the data for the axis themselves, so the
		// standard routines can draw them
		double maxVert = 90.0;
		double maxHor = 360.0;
		int maxVertBoxes = 90/2;//18*4; // 90 = 1 degree sky segments, 45 = 2 degree
		int maxHorBoxes = 180/2;//36*4; // 180 = 2 degree sky segments
		
		int boxHeight = graphHeight / maxVertBoxes;
		graphHeight = boxHeight * maxVertBoxes; // fix rounding issues
		int boxWidth = graphWidth / maxHorBoxes;
		graphWidth = boxWidth * maxHorBoxes;
		
		double[][] dataGrid = new double[maxVertBoxes][maxHorBoxes]; 
		int[][] dataGridCount = new int[maxVertBoxes][maxHorBoxes]; 
		
		double vertStep = maxVert/(double)maxVertBoxes; // the step size for the vertical axis
		double horStep = maxHor/(double)maxHorBoxes; // the step size for the horixental axis
		
		// we have three sets of data:
		// In graphData2 EL is in variable 0, AZ in variable 1 and the value is in variable 0 of graphData
		// We do not care about resets and uptime, we just running average the data into the grid
		for (int i=1; i < graphData[0][PayloadStore.DATA_COL].length; i++) {
			double vert = graphData2[0][PayloadStore.DATA_COL][i];
			if (vert > 0) noAzElReadings = false;
			double hor = graphData2[1][PayloadStore.DATA_COL][i];
			if (hor > 0) noAzElReadings = false;
			double value = graphData[0][PayloadStore.DATA_COL][i];
			if (Double.isNaN(value)) value = 0;
			
			// integrity check
			if (graphData[0][PayloadStore.UPTIME_COL][i] != graphData2[1][PayloadStore.UPTIME_COL][i])
				System.err.println("ERROR!!!!!!!!!");
			
			if (hor > maxHor) hor = hor % maxHor;  // if greater than 360 we start again at 0
			if (vert > maxVert) vert = maxVert - (vert - maxVert); // if greater than 90 we start to count back down to zero.
			int vertBox = (int)Math.round((vert/vertStep));
			int horBox = (int)Math.round((hor/horStep));
			if (vertBox < 0) vertBox = 0;
			if (horBox < 0) horBox = 0;
			if (vertBox >= maxVertBoxes) vertBox = maxVertBoxes-1;
			if (horBox >= maxHorBoxes) horBox = maxHorBoxes-1;
			dataGrid[vertBox][horBox] += value;
			
			dataGridCount[vertBox][horBox]++;
			
		}
		// now calculate the averages
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
				
			}
		}

		drawLegend(graphHeight, graphWidth, minValue, maxValue, graphFrame.fieldUnits);
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		// Draw vertical axis - always in the same place
		g2.setColor(graphAxisColor);
		g2.drawLine(sideBorder, getHeight()-bottomBorder, sideBorder, topBorder);
		int numberOfLabels = (graphHeight)/labelHeight;
		double[] labels = calcAxisInterval(0, maxVert, numberOfLabels, false);
		numberOfLabels = labels.length;
		g2.setColor(graphTextColor);
		g2.drawString("Elevation", sideLabelOffset, topBorder -(int)(Config.graphAxisFontSize/2)); 
		
		DecimalFormat f2 = new DecimalFormat("0");
		for (int v=1; v < numberOfLabels; v++) {
			
			int pos = getRatioPosition(0, maxVert, labels[v], graphHeight);
			pos = graphHeight-pos;
			String s = f2.format(labels[v]);

			g2.drawString(s, sideLabelOffset, pos+topBorder+(int)(Config.graphAxisFontSize/2)); 
		}
		g2.setColor(graphAxisColor);
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
		String title = graphFrame.displayTitle + " vs Az El";
		g2.drawString(title, sideBorder/2 + graphWidth/2 - graphFrame.displayTitle.length()/2 * Config.graphAxisFontSize/2, titleHeight-Config.graphAxisFontSize/2);

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
		
		for (int h=0; h < numberOfTimeLabels; h++) {
			int timepos = getRatioPosition(0, maxHor, timelabels[h], graphWidth);
			g2.setColor(graphTextColor);
			g2.drawString(""+(long)timelabels[h], timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset);
		}
		
		if (noAzElReadings) {
			g2.setColor(Color.BLACK);
			g2.drawString("No Azimuth and Elevation Data Available for plot", graphWidth/2-50, graphHeight/2);
			return;
		}

		for (int v=0; v < maxVertBoxes; v++)
			for (int h=0; h < maxHorBoxes; h++) {

				int x = getRatioPosition(0, maxHorBoxes, h, graphWidth) +sideBorder+1;
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
