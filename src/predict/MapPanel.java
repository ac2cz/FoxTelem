package predict;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import common.Config;
import common.Log;
import gui.ImagePanel;
import uk.me.g4dpz.satellite.SatPos;

public class MapPanel extends ImagePanel implements Runnable {
	boolean running = true;
	public static final int MAP_UPDATE_PERIOD = 1000;

	SatPos[] satPositions;
	
	public static final double MAX_LATITUDE = 81;
	public static final double MIN_LATITUDE = 85;
	
	public MapPanel(String filePath, SatPos[] positions) {
		super(filePath);
		satPositions = positions;
	}

	
    @Override
    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);
    	Graphics2D g2 = ( Graphics2D ) g; // cast g to Graphics2D  
 
    	double ratio = (double)this.getHeight()/(double)image.getHeight();
		if (image.getWidth() * ratio > this.getWidth())
			ratio = (double)this.getWidth()/(double)image.getWidth();
    	int mapHeight = (int) (image.getHeight()*ratio);
		int mapWidth = (int) (image.getWidth()*ratio);
		//int titleHeight = 20;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);	
    	
    	
    	if (satPositions != null && satPositions.length > 0)
    	for (SatPos pos : satPositions) {
    		if (pos != null) {
    		g.setFont(new Font("SansSerif", Font.BOLD, 12));
    		
    		g2.setColor(Color.LIGHT_GRAY);
    		g2.drawLine(mapWidth/2, 0, mapWidth/2, mapHeight); //prime meridian
    		g2.drawLine(0, mapHeight/2, mapWidth, mapHeight/2); //equator
    		
    		//g2.drawString("", mapWidth/2 , titleHeight);
    		double lat = pos.getLatitude()/ (Math.PI * 2.0) * 360;
    		double lon = pos.getLongitude()/ (Math.PI * 2.0) * 360;
    		//lat=51.4826; lon=0.0077; // test greenwich
    		
    		//lat=0; lon=0;
    //		lon = 0; lat = 89; // north pole
    //		lon = 0; lat = -89; // south pole
   
    		Log.println("Sat lat/long: " + lat + "," + lon);
    		double[][] rangeCircle = pos.getRangeCircle();

    		g2.setColor(Color.YELLOW);
    		
    		// Cape town
    		double la = -33.9249;
    		double lo = 18.4241;
    		drawCross(la, lo, mapWidth, mapHeight, g2);
    		
    		//London
    		la = 51.5074;
    		lo = -0.1278;
    		drawCross(la, lo, mapWidth, mapHeight, g2);
    		
    		// My QTH
    		lo=-73.980599; la=40.703328; // test my qth
    		drawCross(la, lo, mapWidth, mapHeight, g2);
    		
    		// Plot the lat long of the sat as a cross
    		g2.setColor(Color.RED);
    		drawCross(lat, lon, mapWidth, mapHeight, g2);
    		int x=0; int y =0;
    		g2.setColor(Color.YELLOW);
    		int lastX = 99999, lastY = 99999;
    		for (double[] latLon : rangeCircle) {
    			lat = latLon[0];
    			lon = latLon[1];
    			
    			x = lonToX(lon, mapWidth);
    			y = latToY(lat, mapWidth, mapHeight);
    			if (lastX == 99999 ) {
    				lastX = x;
    				lastY = y;
    			}
    			g2.drawLine(x, y, x, y);
    		}
    		}
    	}

    //	
    }
    
    private void drawCross(double lat, double lon, int mapWidth, int mapHeight, Graphics2D g2) {
		// Must check if lat = +-90 as this causes answer to be infinity
		int x = lonToX(lon, mapWidth);
		int y = latToY(lat, mapWidth, mapHeight);
		
		g2.drawLine(x-10, y, x+10, y);
		g2.drawLine(x, y-10, x, y+10);
		
		
		Log.println("Drawn at: " + x + "," + y);

    }
    
    /**
     * Convert the longitude to the x coordinate of the Mercator projection
     * 0 is in the center 180 is the mapWidth. 181 is at the left edge of the map
     * @param lon
     * @param mapWidth
     * @return
     */
    int lonToX(double lon, int mapWidth) {
		int x = 0;
		
		if (lon > 180.0) {
			lon = lon - 180.0; // so that we will be on the left half of the map
		} else {
			lon = lon + 180; // so we will be in the right half of the map.
		}
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
    int latToY(double lat, int mapWidth, int mapHeight) {
    	if (lat > 0)
    		lat = MAX_LATITUDE*lat / 90;
    	else
    		lat = MIN_LATITUDE*lat / 90;
    	// convert from degrees to radians
		double latRad = lat * (Math.PI/180);

		// get y value
		double mercN = Math.log(Math.tan((Math.PI/4)+(latRad/2)));
		int y = (int) ((mapWidth/(2*Math.PI))*mercN);
		return mapHeight/2-y;
    }
    
	@Override
	public void run() {
		while (running) {
			try {
				Thread.sleep(MAP_UPDATE_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
			
			repaint();
			
		}
	}

}
