package telemetry;

import gui.MainWindow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import measure.PassMeasurement;
import common.Config;
import common.Log;
import common.TlmServer;
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
 * Class to hold a queue of frames after they are received by the ground station and before they have been sent to the 
 * amsat server.  This class runs as a back ground thread.  When frames are added to it they are stored.  They are then 
 * sent in the background to the server.
 * 
 * If the passManager is active (findSignal is true) then we are attempting to monitor the satellites and measure TCA.
 * In that case, we check if a pass is active and hold on to the last record until the pass is done.  We then append
 * TCA is measured.
 *
 */
public class RawFrameQueue implements Runnable {
	private static final int INIT_SIZE = 1000;
	public static String RAW_SLOW_SPEED_FRAMES_FILE = "rawDUVframes.log";
	public static String RAW_HIGH_SPEED_FRAMES_FILE = "rawHSframes.log";
	SortedFrameArrayList rawSlowSpeedFrames;
	SortedFrameArrayList rawHighSpeedFrames;
	private boolean updatedSlowQueue = false;
	private boolean updatedHSQueue = false;
	
	TlmServer primaryServer;
	TlmServer secondaryServer;
	
	boolean running = false;
	
	public RawFrameQueue() {
		init();
	}
	
	private void init() {
		primaryServer = new TlmServer(Config.primaryServer, Config.serverPort);
		secondaryServer = new TlmServer(Config.secondaryServer, Config.serverPort);
		rawSlowSpeedFrames = new SortedFrameArrayList(INIT_SIZE);
		rawHighSpeedFrames = new SortedFrameArrayList(INIT_SIZE);
		try {
			load(RAW_SLOW_SPEED_FRAMES_FILE, Frame.DUV_FRAME);
			load(RAW_HIGH_SPEED_FRAMES_FILE, Frame.HIGH_SPEED_FRAME);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"ERROR Raw Frames Queue File not found",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					e.toString(),
					"I/O ERROR Loading Stored Raw Frames Queue",
					JOptionPane.ERROR_MESSAGE) ;
			e.printStackTrace(Log.getWriter());
		}
	}

	public boolean add(Frame f) throws IOException {
		if (f instanceof SlowSpeedFrame ) {
			if (!rawSlowSpeedFrames.hasFrame(f)) {
				updatedSlowQueue = true;
				save(f, RAW_SLOW_SPEED_FRAMES_FILE);
				MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size());
				return rawSlowSpeedFrames.add(f);
			} else {
				Log.println("DUPLICATE FRAME, not loaded");
			}
		} else {
			if (!rawHighSpeedFrames.hasFrame(f)) {
				updatedHSQueue = true;
				save(f, RAW_HIGH_SPEED_FRAMES_FILE);
				MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size());
				return rawHighSpeedFrames.add(f);
			} else {
				Log.println("DUPLICATE FRAME, not loaded");
			}
		}
		
		return false;
	}

	
	public void load(String log, int type) throws IOException {
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
			

		}

		if (type == Frame.HIGH_SPEED_FRAME) {
			//size = HighSpeedFrame.MAX_HEADER_SIZE + HighSpeedFrame.MAX_PAYLOAD_SIZE 
			//		+ HighSpeedFrame.MAX_TRAILER_SIZE;
			while (reader.ready()) {
				frame = new HighSpeedFrame(reader);
				rawHighSpeedFrames.add(frame);
			}
			updatedHSQueue = true;
		}

		dis.close();
		MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size());

	}

	/**
	 * Save a payload to the log file
	 * @param frame
	 * @param log
	 * @throws IOException
	 */
	public void save(Frame frame, String log) throws IOException {
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			log = Config.logFileDirectory + File.separator + log;
		} 
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

	/**
	 * Remove the first record in the queue
	 * @throws IOException 
	 */
	private void deleteAndSave(SortedFrameArrayList frames, String log) throws IOException {
		
		frames.remove(0);
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
			for (int i=0; i< frames.size(); i++) {
				frames.get(i).save(writer);
			}
		} finally {
			writer.flush();
			writer.close();
		}

		}
	
	public void delete() {
		try {
			SatPayloadStore.remove(RAW_SLOW_SPEED_FRAMES_FILE);
			SatPayloadStore.remove(RAW_HIGH_SPEED_FRAMES_FILE);
			init();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					ex.toString(),
					"Error Deleting Server Upload Queues, check permissions on files:\n" +
					RAW_SLOW_SPEED_FRAMES_FILE + "\n" +
					RAW_HIGH_SPEED_FRAMES_FILE,
					JOptionPane.ERROR_MESSAGE) ;
		}

	}

	
	public void stopProcessing() {
		running = false;
	}
	
	@Override
	public void run() {
		running = true;
		boolean success = true;
		int retryStep = 0;
		while(running) {
			
			try {
				Thread.sleep(1000 * Config.serverTxPeriod); // refresh data periodically
			} catch (InterruptedException e) {
				Log.println("ERROR: server frame queue thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size());
			if (Config.uploadToServer) {
				if (!success) {
					// We failed the last time we tried to connect, so wait until we retry
					//System.out.print(".");
					if (retryStep++ > Config.serverRetryWaitPeriod) {
						success = true;
						retryStep = 0;
					}
				}
				// try to send these frames to the server
				// We attempt to send the first one, if unsuccessful, we try the backup server.  If still unsuccessful we drop out
				// and try next time, unless sendToBoth is set, in which case we just send to both servers
				while (rawSlowSpeedFrames.size() > 0 && success) {
					if (!Config.passManager.inPass() || (Config.passManager.inPass() && rawSlowSpeedFrames.size() > 1))
						success = sendFrame(rawSlowSpeedFrames, RAW_SLOW_SPEED_FRAMES_FILE);
					try {
						Thread.sleep(100); // pause so that the server can keep up
					} catch (InterruptedException e) {
						Log.println("ERROR: server frame queue thread interrupted");
						e.printStackTrace(Log.getWriter());
					} 	
				}
				while (rawHighSpeedFrames.size() > 0 && success) {
					if (!Config.passManager.inPass() || (Config.passManager.inPass() && rawSlowSpeedFrames.size() > 1))
						success = sendFrame(rawHighSpeedFrames, RAW_HIGH_SPEED_FRAMES_FILE);
					try {
						Thread.sleep(100); // pause so that the server can keep up
					} catch (InterruptedException e) {
						Log.println("ERROR: server frame queue thread interrupted");
						e.printStackTrace(Log.getWriter());
					}
				}
			}
		}
		Log.println("Server Queue thread ended");
	}

	private boolean sendFrame(SortedFrameArrayList frames, String file) {
		boolean success = false;
		if (Config.passManager.hasTCA()) {
			PassMeasurement passMeasurement = Config.passManager.getPassMeasurement(); 
			frames.get(0).setPassMeasurement(passMeasurement);
			Config.passManager.sentTCA();
		}
		
		// Make sure these are up to date
		primaryServer.setHostName(Config.primaryServer);
		secondaryServer.setHostName(Config.secondaryServer);
		
		String protocol = "udp";
		if (Config.serverProtocol == TlmServer.TCP)
			protocol = "tcp";
		Log.println("Trying Primary Server: " + protocol + "://" + Config.primaryServer + ":" + Config.serverPort);
		try {
			frames.get(0).sendToServer(primaryServer, Config.serverProtocol);
			success = true;
		} catch (UnknownHostException e) {
			Log.println("Could not connect to primary server");
			//e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("IO Exception with primary server");
			//e.printStackTrace(Log.getWriter());
		}
		if (running)
			if (Config.sendToBothServers || !success) // We send to the secondary if we failed or if we are sending to both servers
			try {
				Log.println("Trying Secondary Server: " + protocol + "://" + Config.secondaryServer + ":" + Config.serverPort);
				frames.get(0).sendToServer(secondaryServer, Config.serverProtocol);
				//String primary = Config.primaryServer;
				//Config.primaryServer = Config.secondaryServer;
				//Config.secondaryServer = primary;
				//primaryServer.setHostName(Config.primaryServer);
				//secondaryServer.setHostName(Config.secondaryServer);
				success = true;
			} catch (UnknownHostException e) {
				Log.println("Could not connect to secondary server");
				//e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				Log.println("IO Exception with secondary server");
				//e.printStackTrace(Log.getWriter());
			}
		if (success) // then at least one of the transmissions was successful
			try {
				//Log.println("Sent frame " + frames.get(0).header.toString());
				deleteAndSave(frames, file);
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not remove raw frames from the queue file:\n" + file + "\n, you may"
						+ " need to delete it manually or they will be sent again");
				e.printStackTrace(Log.getWriter());
			}
		MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size());
		return success; // return true if one succeeded
	}
}