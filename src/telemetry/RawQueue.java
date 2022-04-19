package telemetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

import common.Config;
import common.Log;
import gui.MainWindow;
import telemetry.Format.FormatFrame;
import telemetry.frames.Frame;
import telemetry.frames.HighSpeedFrame;
import telemetry.frames.SlowSpeedFrame;

public abstract class RawQueue implements Runnable {
	ConcurrentLinkedQueue<Frame> rawSlowSpeedFrames;
	ConcurrentLinkedQueue<Frame> rawHighSpeedFrames;
	ConcurrentLinkedQueue<Frame> rawPSKFrames;
	@SuppressWarnings("unused")
	protected boolean updatedSlowQueue = false;
	@SuppressWarnings("unused")
	protected boolean updatedHSQueue = false;
	@SuppressWarnings("unused")
	protected boolean updatedPSKQueue = false;
	
	boolean running = false;
	
	RawQueue() {
		
	}
	
	public abstract boolean add(Frame f) throws IOException;
	public abstract void delete();
	
	protected void load(String log, int type) throws IOException {
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
			Log.println("Loading: " + log);
		}
		File aFile = new File(log );
		if(!aFile.exists()){
			aFile.createNewFile();
		}

		FileInputStream dis = new FileInputStream(log);
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));

		Frame frame = null;
		//int size = 0;
		if (type == Frame.DUV_FRAME) {
			//size = SlowSpeedFrame.MAX_HEADER_SIZE + SlowSpeedFrame.MAX_PAYLOAD_SIZE
			//		+ SlowSpeedFrame.MAX_TRAILER_SIZE;
			while (reader.ready()) {
				frame = new SlowSpeedFrame(reader);
				rawSlowSpeedFrames.add(frame);
			}
			updatedSlowQueue = true;
		} else if (type == Frame.PSK_FRAME) {
			while (reader.ready()) {
				frame = new FormatFrame(Config.satManager.getFormatByName("FOX_BPSK"), reader); // TO DO format should come from satManager
				rawPSKFrames.add(frame);
			}
			updatedPSKQueue = true;
		} else if (type == Frame.HIGH_SPEED_FRAME) {
			//size = HighSpeedFrame.MAX_HEADER_SIZE + HighSpeedFrame.MAX_PAYLOAD_SIZE 
			//		+ HighSpeedFrame.MAX_TRAILER_SIZE;
			while (reader.ready()) {
				frame = new HighSpeedFrame(reader);
				rawHighSpeedFrames.add(frame);
			}
			updatedHSQueue = true;
		}

		dis.close();
		MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size() + this.rawPSKFrames.size());

	}

	/**
	 * Save a payload to the log file
	 * @param frame
	 * @param log
	 * @throws IOException
	 */
	protected void save(Frame frame, String log) throws IOException {
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
		} 
		synchronized(this) { // make sure we have exlusive access to the file on disk, otherwise a removed frame can clash with this
			File aFile = new File(log );
			if(!aFile.exists()){
				aFile.createNewFile();
			}
			//Log.println("Saving: " + log);
			//use buffering and append to the existing file
			FileOutputStream dis = new FileOutputStream(log, true);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dis));

			try {
				frame.save(writer);
			} finally {
				writer.flush();
				writer.close();
			}

			writer.close();
			dis.close();
		}
	}

	/**
	 * Remove the first record in the queue.  Save all of the records to the file as a backup
	 * @throws IOException 
	 */
	protected void deleteAndSave(ConcurrentLinkedQueue<Frame> frames, String log) throws IOException {
		synchronized(this) {  // make sure we have exclusive access to the file on disk, otherwise a frame being added can clash with this
			frames.poll(); // remove the head of the queue
			if (!Config.logFileDirectory.equalsIgnoreCase("")) {
				log = Config.logFileDirectory + File.separator + log;
			} 
			File aFile = new File(log );
			if(!aFile.exists()){
				aFile.createNewFile();
			}
			//use buffering and OVERWRITE the existing file
			FileOutputStream dis = new FileOutputStream(log, false);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dis));
			try {
				for (Frame f : frames) {
					f.save(writer);
				}
			} finally {
				writer.flush();
				writer.close();
			}

		}
	}
	


	
	public void stopProcessing() {
		running = false;
	}
}
