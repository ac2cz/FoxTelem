package common;
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
 */
public class Location {

	public String maidenhead;
	public float latitude;
	public float longitude;
	
	String U = "ABCDEFGHIJKLMNOPQRSTUVWX";
	String L = U.toLowerCase();
	  
	public Location (String p1, String p2) {
		  float lat = -100.0f;
		  float lon = 0.0f;
		try {
			  lat = Float.parseFloat(p1);
			  lon = Float.parseFloat(p2);

			maidenhead = latLonToGridSquare(lat, lon);
		} catch (Exception e) {
			Log.errorDialog("ERROR", e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
	}

	public Location (float lat, float lon) {
		try {
			maidenhead = latLonToGridSquare(lat, lon);
		} catch (Exception e) {
			Log.errorDialog("ERROR", e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
	}

	public Location (String mh) {
		try {
			gridSquareToLatLon(mh);
		} catch (Exception e) {
			Log.errorDialog("ERROR", e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
	}

	/**
	 * 18 x 18 fields, First letters * 10 for latitude and * 20 for longitude
	 * 10 x 10 squares, so digits are 1 deg of lat and 2 deg of long
	 * Final 2 letters are 2.5 mins of lat and 5 mins of long
	 * 
	 * We add a half to the last two letters location so that we get the middle of the square and not the corner
	 * @param grid
	 * @throws Exception 
	 */
	private void gridSquareToLatLon(String grid) throws Exception {
		if (grid.length() != 6) throw new Exception("Maidenhead locator needs to be 6 characters");
		grid = grid.toUpperCase();
		char gLon, gLat, GLat, GLon;
		char nLat, nLon;
		
		GLon = grid.charAt(0);
		GLat = grid.charAt(1);
		nLon = grid.charAt(2);
		nLat = grid.charAt(3);
		gLon = grid.charAt(4);
		gLat = grid.charAt(5);
		
		// Long
		float lon = U.indexOf(GLon);
		lon = lon * 20;  
		lon = lon + Character.getNumericValue(nLon) * 2;
		lon = lon + (((float)U.indexOf(gLon)+0.5f) * 5f )/ 60f;

		
		// move from adjusted to standard longitude where West negative
		lon = lon - 180;
		if (Float.isNaN(lon)) throw new Exception ("lon is NaN");
		if (Math.abs(lon) > 180) throw new Exception ("invalid longitude: "+lon);
		longitude = lon;

		// Lat
		float lat = U.indexOf(GLat);
		lat = lat * 10;
		lat = lat + Character.getNumericValue(nLat);
		lat = lat + (((float)U.indexOf(gLat)+0.5f) * 2.5f )/ 60f;
		
		lat = lat - 90;
		if (Float.isNaN(lat)) throw new Exception("lat is NaN");
		if (Math.abs(lat) == 90.0) throw new Exception ("grid squares invalid at N/S poles");
		if (Math.abs(lat) > 90) throw new Exception ("invalid latitude: "+lat);
		latitude = lat;
		
	}
	
	public String latLonToGridSquare(float lat, float lon) throws Exception{
		  float adjLat,adjLon;
		  char GLat,GLon;
		  String nLat,nLon;
		  char gLat,gLon;
		  float rLat,rLon;
		  String U = "ABCDEFGHIJKLMNOPQRSTUVWX";
		  String L = U.toLowerCase();
		  
		  // support Chris Veness 2002-2012 LatLon library and
		  // other objects with lat/lon properties
		  // properties could be getter functions, numbers, or strings
		  
		  
		  if (Float.isNaN(lat)) throw new Exception("lat is NaN");
		  if (Float.isNaN(lon)) throw new Exception ("lon is NaN");
		  if (Math.abs(lat) == 90.0) throw new Exception ("grid squares invalid at N/S poles");
		  if (Math.abs(lat) > 90) throw new Exception ("invalid latitude: "+lat);
		  if (Math.abs(lon) > 180) throw new Exception ("invalid longitude: "+lon);
		  
		  
		  adjLat = lat + 90;
		  adjLon = lon + 180;
		  GLat = U.charAt((int) (adjLat/10));
		  GLon = U.charAt((int) (adjLon/20));
		  nLat = ""+(int)(adjLat % 10);
		  nLon = ""+(int)((adjLon/2) % 10);
		  rLat = (adjLat - (int)(adjLat)) * 60;
		  rLon = (adjLon - 2*(int)(adjLon/2)) *60;
		  gLat = L.charAt((int)(rLat/2.5));
		  gLon = L.charAt((int)(rLon/5));
		  String locator = ""+GLon+GLat+nLon+nLat+gLon+gLat;
		  return locator;
		}


}
