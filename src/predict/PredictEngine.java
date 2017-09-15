package predict;

import java.io.IOException;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import common.Log;
import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class PredictEngine implements Runnable {
	public static final int PREDICT_UPDATE_PERIOD = 1000; // update every 500ms by default
	public SatPos[] satPositions;
	
	boolean running = true;
	/*
	 * Predict Test
	 */
	private static final String TLE_AO51_2 = "2 28375 098.0551 118.9086 0084159 315.8041 043.6444 14.40638450251959";
	private static final String TLE_AO51_1 = "1 28375U 04025K   09105.66391970  .00000003  00000-0  13761-4 0  3643";
	protected static final String[] LEO_TLE = {
            "AO-51 [+]",
            TLE_AO51_1,
            TLE_AO51_2};
	
	protected static final String[] AO85_TLE = {
            "AO-85",
            "1 40967U 15058D   16111.35540844  .00000590  00000-0  79740-4 0 01029",
            "2 40967 064.7791 061.1881 0209866 223.3946 135.0462 14.74939952014747"};
	
	static final GroundStationPosition GROUND_STATION = new GroundStationPosition(40.703328, -73.980599, 20);
	static final TimeZone TZ = TimeZone.getTimeZone("UTC:UTC");
	private DateTime timeNow;
	
	public PredictEngine() {
		satPositions = new SatPos[1];
	}
	
	
	private void calculateSatPositions() {
		
		timeNow = new DateTime(DateTimeZone.UTC); //("2009-04-17T06:57:32Z");
		final TLE tle = new TLE(AO85_TLE);
        final Satellite satellite = SatelliteFactory.createSatellite(tle);
        final SatPos satellitePosition = satellite.getPosition(GROUND_STATION, timeNow.toDate());
        satPositions[0] = satellitePosition;
    //    Log.println("At time: " + timeNow);
     //   Log.println("Sat at: AZ " + String.format("%9.7f", satellitePosition.getAzimuth()/Math.PI*180));
    //    Log.println("Sat at: EL " + String.format("%9.7f", satellitePosition.getElevation()/Math.PI*180));
    //    Log.println("Sat at: alt " + String.format("%9.7f",  satellitePosition.getAltitude()));
    //    Log.println("Sat at: range " + String.format("%9.7f",  satellitePosition.getRange()));
		
	}
	
	@Override
	public void run() {
		
		while (running) {
			try {
				Thread.sleep(PREDICT_UPDATE_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
			
			calculateSatPositions();
			
		}
		
	}

}
