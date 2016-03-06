package telemServer;

import java.io.IOException;
import java.sql.SQLException;

import common.Config;
import common.Log;

public class ImageProcess implements Runnable {

	boolean running = true;
	private static final int SLEEP_PERIOD = 5000; // 5 seconds
	
	@Override
	public void run() {
		Log.println("Started Thread to handle image assembly");
		
		while (running) {
			//Get a list of images which have updated lines, then load the image data from the database and save each image to disk as a Jpeg
			try {
				Config.payloadStore.processNewImageLines();
			} catch (SQLException e) {
				Log.println("ERROR ALERT:" + e.getMessage());
				e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				Log.println("ERROR ALERT:" + e.getMessage());
				e.printStackTrace(Log.getWriter());
			}
			try {
				Thread.sleep(SLEEP_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
