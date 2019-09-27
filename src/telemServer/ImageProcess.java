package telemServer;

import java.io.IOException;
import java.sql.SQLException;

import common.Log;
import telemetry.PayloadDbStore;

public class ImageProcess implements Runnable {
	PayloadDbStore payloadStore;
	boolean running = true;
	private static final int SLEEP_PERIOD = 5000; // 5 seconds
	
	public ImageProcess(PayloadDbStore db) {
		payloadStore = db;
	}
	@Override
	public void run() {
		Log.println("Started Thread to handle image assembly");
		
		while (running) {
			//Get a list of images which have updated lines, then load the image data from the database and save each image to disk as a Jpeg
			try {
				payloadStore.processNewImageLines();
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
		Log.println("STOPPING: Image Procesing Thread Exit");
	}

}
