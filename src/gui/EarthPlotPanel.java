package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import telemetry.CameraJpeg;
import telemetry.FramePart;
import telemetry.PayloadStore;

@SuppressWarnings("serial")
public class EarthPlotPanel extends GraphCanvas {

	public static final double MAX_LATITUDE = 81;
	public static final double MIN_LATITUDE = 85;
	public static final int RECTANGULAR_PROJECTION = 0;
	public static final int MERCATOR_PROJECTION = 1;
	public int mapProjection = RECTANGULAR_PROJECTION;
	
	EarthPlotPanel(String t, int conversionType, int plType, GraphFrame gf, FoxSpacecraft sat) {
		super(t, conversionType, plType, gf, sat);
		sideBorder = sideBorder + 50;
		sideLabelOffset = sideLabelOffset + 50;
		updateGraphData("EarthPlotPanel.new");
	}
	
	private void setImage() {
//		setImage("C:\\Users\\chris\\Desktop\\workspace\\FoxTelem\\src\\images\\Equirectangular_projection_SW.jpg");
		setImage("C:\\Users\\chris\\Desktop\\workspace\\FoxTelem\\src\\images\\WorldCoastLine_EquiRectangular.jpg");
		
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
				g2.setColor(getColorGradient(minValue, maxValue, labels[i], 255));
/*
				int shade = getRatioPosition(minValue, maxValue, labels[i], 255);
				if (shade > 255) shade = 255;
				if (shade <0) shade = 0;
				shade = 255-shade; // we want min signal white and max black
				
				g2.setColor(new Color(shade,shade,shade));
	*/
				g2.fillRect(sideBorder + graphWidth + leftOffset, verticalOffset + i * boxHeight, legendWidth, boxHeight);
				if (shade > 127) g2.setColor(Color.BLACK);
				else g2.setColor(Color.WHITE);
				
				g2.drawString(""+labels[i], sideBorder + graphWidth + leftOffset+5, verticalOffset + i * boxHeight + (int)(boxHeight*0.8));

			}
		}
		
	}
	
	int maxVertBoxes = 90; // = 2 degree sky segments
	int maxHorBoxes = 180;// = 2 degree sky segments
	
    /**
     * We plot lat lon on a map projection.
     * lat is stored in degrees from -90 to +90
     * lon is stored in degrees from -180 to +180
     * We store averaged values in a grid where lat is mapped to 0-180 and lon is map to 0-360
     * The values are then plotted on a projection of the earth
     * The map is defined by graphHeight and graphWidth.  
     * x is plotted left to right from 0 to graphWidth.  0 is the sideBorder
     * y is plotted vertically with 0 at the top and graphHeight at the bottom.  0 is the topBorder
     * 
     * 
     */
	public void paintComponent(Graphics gr) {
		super.paintComponent( gr ); // call superclass's paintComponent  
		
		topBorder = 0;
		bottomBorder = 0;
		
		if (!checkDataExists()) return;
		boolean noLatLonReadings = true;	
		int graphHeight = getHeight() - topBorder - bottomBorder;
		int graphWidth = getWidth() - sideBorder*2; // width of entire graph
		
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		// Create the Data Grid and the Horizontal Axis and a new version of graphData that will hold the data for the axis themselves, so the
		// standard routines can draw them
		double maxVert = 90.0; // latitude
		double minVert = -90.0; // latitude
		double maxHor = 180.0;
		double minHor = -180.0;
		
		int boxHeight = graphHeight / maxVertBoxes;
		graphHeight = boxHeight * maxVertBoxes; // fix rounding issues
//		int boxWidth = graphWidth / maxHorBoxes;
//		graphWidth = boxWidth * maxHorBoxes;
		int boxWidth = 2 * boxHeight;
		graphWidth = 2 * graphHeight;;
//		Log.println("DISPLAY RATIO:" + (double)graphHeight/(double)graphWidth);
		setImage();
		paintMap(gr, sideBorder, 0, graphHeight, graphWidth);

		double[][] dataGrid = new double[maxVertBoxes][maxHorBoxes]; 
		int[][] dataGridCount = new int[maxVertBoxes][maxHorBoxes]; 

		double vertStep = (maxVert-minVert)/(double)maxVertBoxes; // the step size for the vertical axis
		double horStep = (maxHor-minHor)/(double)maxHorBoxes; // the step size for the horixental axis

		// we have data as follows:
		// In graphData[0] DATA_COL has the actual data values
		// In LAT_COL we should have the latitude in radians.  In LON_COL the longitude
		// We do not care about resets and uptime, we just running average the data into the grid
		for (int i=1; i < graphData[0][PayloadStore.DATA_COL].length; i++) {
			double lat = graphData[0][PayloadStore.LAT_COL][i];
			if (lat == FramePart.NO_POSITION_DATA || lat == FramePart.NO_T0 || lat == FramePart.NO_TLE) {
				// we don't calc this point
			} else {
				noLatLonReadings = false;
				//lat = FramePart.radToDeg(lat);
				//lat = lat/ (Math.PI * 2.0) * 360;
				if (lat > maxVert) lat = maxVert;				
				if (lat < minVert) lat = minVert;

				double lon = graphData[0][PayloadStore.LON_COL][i];
				if (lon > maxHor) lon = maxHor;;			
				if (lon < minHor) lon = minHor;

				double value = graphData[0][PayloadStore.DATA_COL][i];
				if (Double.isNaN(value)) value = 0;

				// We plot longitude horizontally
				// We plot latitude vertically
				// Map to a positive scale so we can store in array and calc the averages
				lat += 90;
				lon += 180;
				
				int vertBox = (int)Math.round((lat/vertStep));
				int horBox = (int)Math.round((lon/horStep));


				//System.out.println("Lat: " + lat + " vert: " + vertBox);
				//System.out.println("Lon: " + lon + " hor: " + horBox);

				if (vertBox >= maxVertBoxes) vertBox = maxVertBoxes-1;
				if (horBox >= maxHorBoxes) horBox = maxHorBoxes-1;
				dataGrid[vertBox][horBox] += value;

				dataGridCount[vertBox][horBox]++;
			}
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
		double[] labels = calcAxisInterval(minVert, maxVert, numberOfLabels, false);
		numberOfLabels = labels.length;
		g2.setColor(graphTextColor);
		//g2.drawString("Latitude", sideLabelOffset, topBorder -(int)(Config.graphAxisFontSize/2)); 
		int zeroPoint = getHeight() - topBorder - bottomBorder; //graphHeight + topBorder;
		
		DecimalFormat f2 = new DecimalFormat("0");
		for (int v=1; v < numberOfLabels; v++) {
			
			//int pos = getRatioPosition(minVert, maxVert, labels[v], graphHeight);
			int pos = latToY(labels[v], graphWidth, graphHeight);
			pos = graphHeight-pos+topBorder;
			if (labels[v] == 0) zeroPoint = pos+topBorder;
		//	pos = graphHeight-pos;
			String s = f2.format(labels[v]);

			g2.drawString(s, sideLabelOffset, pos+(int)(Config.graphAxisFontSize/2)); 
		}
		g2.setColor(graphAxisColor);
		
		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));

		int titleHeight = Config.graphAxisFontSize+10;
		if (zeroPoint < Config.graphAxisFontSize*3) {
			// we need the title at bottom of graph, not top
			titleHeight = graphHeight + topBorder;
		}

		// Draw the title
		g2.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif", Font.BOLD, Config.graphAxisFontSize+3));
//		String title = graphFrame.displayTitle + " (Mercator Projection)";
		String title = graphFrame.displayTitle + " (Equirectangular Projection)";
		
		g2.drawString(title, sideBorder/2 + graphWidth/2 - graphFrame.displayTitle.length()/2 * Config.graphAxisFontSize/2, titleHeight-Config.graphAxisFontSize/2);

		g.setFont(new Font("SansSerif", Font.PLAIN, Config.graphAxisFontSize));
		
		// Draw baseline at the zero point
		g2.setColor(graphAxisColor);
		g2.drawLine(sideLabelOffset, zeroPoint, graphWidth+sideBorder, zeroPoint);
		g2.setColor(graphTextColor);
		int offset = 0;
		//g2.drawString("Longitude", sideLabelOffset, zeroPoint+1*Config.graphAxisFontSize + offset );

		// Plot the labels for the horizontal axis
		int numberOfTimeLabels = graphWidth/(labelWidth/2);
		double[] timelabels = calcAxisInterval(minHor, maxHor, numberOfTimeLabels, true);
		numberOfTimeLabels = timelabels.length;
		
		for (int h=0; h < numberOfTimeLabels; h++) {
			int timepos = getRatioPosition(minHor, maxHor, timelabels[h], graphWidth);
			g2.setColor(graphTextColor);
			g2.drawString(""+(long)timelabels[h], timepos+sideBorder+2, zeroPoint+1*Config.graphAxisFontSize + offset);
		}
		
		if (noLatLonReadings) {
			g2.setColor(Color.BLACK);
			g2.drawString("No Latitude and Longitude Data Available for plot", graphWidth/2-50, graphHeight/2);
			return;
		}

		// Now we plot the data
		// We have to remember that the latitude boxes run 0 - 180 but mean -90 to +90
		//
		for (int v=0; v < maxVertBoxes; v++)
			for (int h=0; h < maxHorBoxes; h++) {

		//		int x = getRatioPosition(0, maxHorBoxes, h, graphWidth) +sideBorder+1;
		//		int y = getRatioPosition(0, maxVertBoxes, v-90, graphHeight);
				double lat = (180.0*(v)/maxVertBoxes) -90;
				double lon = 360.0*h/maxHorBoxes;
				int x = lonToX(lon, graphWidth) +sideBorder+1;
    			int y = latToY(lat, graphWidth, graphHeight);
    			double val = dataGrid[v][h];
				if (val != 0) {
					
					g2.setColor(getColorGradient(minValue, maxValue, val, 255));
					g2.fillRect(x, graphHeight-y+topBorder-boxHeight, boxWidth, boxHeight);
				}
				//
			}


	}

	private Color getColorGradient(double minValue, double maxValue, double val, int range) {
		int shade = getRatioPosition(minValue, maxValue, val, 255);
		if (shade > 255) shade = 255;
		if (shade <0) shade = 0;
		shade = 255-shade; // we want min signal white and max black

		int r1 = 200;
		int r2 = 100;
		int grn1=0;
		int grn2 = 0;
		int b1=0;
		int b2 = 100;
		int p = shade/255;
		// TODO - why does this linear interpolation not work?
		int r = (int) ((1.0-p) * r1 + p * r2 + 0.5);
		int g = (int) ((1.0-p) * grn1 + p * grn2 + 0.5);
		int b = (int) ((1.0-p) * b1 + p * b2 + 0.5);
		
		//g2.setColor(new Color(shade,shade,shade));
		return new Color(255-shade,0,shade);
		
	}
	
	int lonToX(double lon, int mapWidth) {
		return mercatorLonToX(lon,mapWidth);
	}

	int latToY(double lat, int mapWidth, int mapHeight) {
		if (mapProjection == RECTANGULAR_PROJECTION)
			return rectangularLatToY(lat, mapWidth, mapHeight);
		else
			return mercatorLatToY(lat,mapWidth, mapHeight);
	}

	int rectangularLatToY(double lat, int mapWidth, int mapHeight) {
		int y = (int)(lat*mapHeight/180);
		return mapHeight/2+y;
	}

	
	   /**
     * Convert the longitude to the x coordinate of the Mercator projection
     * 0 is in the center 180 is the mapWidth. -180 is at the left edge of the map
     * 
     * @param lon
     * @param mapWidth
     * @return
     */
    int mercatorLonToX(double lon, int mapWidth) {
		int x = 0;
	
		x = (int) (lon*mapWidth/360);
		
		return x;
    }
    
    /**
     * Convert the latitude to the y coordinate of the Mercator projection
     * 0 is the center of the map vertically
     * @param lat
     * @param mapWidth
     * @param mapHeight
     * @return
     */
    int mercatorLatToY(double lat, int mapWidth, int mapHeight) {
    	// squash vertically to meet the map projection
/*
    	if (lat > 0)
    		lat = MAX_LATITUDE*lat / 90;
    	else
    		lat = MIN_LATITUDE*lat / 90;
 */
    	// convert from degrees to radians because Math functions are in radians
		double latRad = lat * (Math.PI/180);

		// get y value
		double mercN = Math.log(Math.tan((Math.PI/4)+(latRad/2)));
		int y = (int) ((mapWidth/(2*Math.PI))*mercN);
		//return mapHeight/2-y;
		return mapHeight/2+y;
    }
}
