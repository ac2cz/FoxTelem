package telemetry;

import gui.MainWindow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JOptionPane;

import measure.PassMeasurement;
import telemetry.Format.FormatFrame;
import telemetry.frames.Frame;
import telemetry.frames.SlowSpeedFrame;
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
public class RawFrameQueue extends RawQueue {
	public static String RAW_SLOW_SPEED_FRAMES_FILE = "rawDUVframes.log";
	public static String RAW_HIGH_SPEED_FRAMES_FILE = "rawHSframes.log";
	public static String RAW_PSK_FRAMES_FILE = "rawPSKframes.log";
	
	TlmServer primaryServer;
	TlmServer secondaryServer;
	
	
	
	public RawFrameQueue() {
		super();
		init();
	}
	
	public void init() {
		primaryServer = new TlmServer(Config.primaryServer, Config.serverPort, TlmServer.AUTO_CLOSE, TlmServer.WAIT_FOR_ACK);
		secondaryServer = new TlmServer(Config.secondaryServer, Config.serverPort, TlmServer.AUTO_CLOSE, TlmServer.WAIT_FOR_ACK);
		rawSlowSpeedFrames = new ConcurrentLinkedQueue<Frame>();
		rawHighSpeedFrames = new ConcurrentLinkedQueue<Frame>();
		formatFrames = new ConcurrentLinkedQueue<Frame>();
		try {
			synchronized(this) { // lock will be load the files
				load(RAW_SLOW_SPEED_FRAMES_FILE, Frame.DUV_FRAME);
				load(RAW_HIGH_SPEED_FRAMES_FILE, Frame.HIGH_SPEED_FRAME);
				load(RAW_PSK_FRAMES_FILE, Frame.PSK_FRAME);
			}
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
				updatedSlowQueue = true;
				save(f, RAW_SLOW_SPEED_FRAMES_FILE);
				MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size()+ this.formatFrames.size());
				return rawSlowSpeedFrames.add(f);
			
		} else if (f instanceof FormatFrame ) {
				updatedPSKQueue = true;
				save(f, RAW_PSK_FRAMES_FILE);
				MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size() + this.formatFrames.size());
				return formatFrames.add(f);
		} else {
				updatedHSQueue = true;
				save(f, RAW_HIGH_SPEED_FRAMES_FILE);
				MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size() + this.formatFrames.size());
				return rawHighSpeedFrames.add(f);
		}		
	}

	
	public void delete() {
		try {
			SatPayloadStore.remove(SatPayloadTable.getDir() + RAW_SLOW_SPEED_FRAMES_FILE);
			SatPayloadStore.remove(SatPayloadTable.getDir() + RAW_HIGH_SPEED_FRAMES_FILE);
			SatPayloadStore.remove(SatPayloadTable.getDir() + RAW_PSK_FRAMES_FILE);
			init();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(MainWindow.frame,
					ex.toString(),
					"Error Deleting Server Upload Queues, check permissions on files:\n" +
					RAW_SLOW_SPEED_FRAMES_FILE + "\n" +
					RAW_PSK_FRAMES_FILE + "\n" +
					RAW_HIGH_SPEED_FRAMES_FILE,
					JOptionPane.ERROR_MESSAGE) ;
		}

	}
	
	@Override
	public void run() {
		running = true;
		boolean success = true;
		int retryStep = 0;
		while(running) {
			
			try {
				Thread.sleep(100 * Config.serverTxPeriod); // refresh data periodically. This also throttles sending large queue.  Delay in 100ms increments
			} catch (InterruptedException e) {
				Log.println("ERROR: server frame queue thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size() + this.formatFrames.size());
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
				if (rawSlowSpeedFrames.size() > 0 && success) {
					// If we are in a pass, then don't send the last frame
					if (!Config.passManager.inPass() || (Config.passManager.inPass() && rawSlowSpeedFrames.size() > 1))
						success = sendFrame(rawSlowSpeedFrames, RAW_SLOW_SPEED_FRAMES_FILE);
					try {
						Thread.sleep(100); // pause so that the server can keep up
					} catch (InterruptedException e) {
						Log.println("ERROR: server DUV frame queue thread interrupted");
						e.printStackTrace(Log.getWriter());
					} 	
				}
				if (rawHighSpeedFrames.size() > 0 && success) {
					if (!Config.passManager.inPass() || (Config.passManager.inPass() && rawHighSpeedFrames.size() > 1))
						success = sendFrame(rawHighSpeedFrames, RAW_HIGH_SPEED_FRAMES_FILE);
					try {
						Thread.sleep(100); // pause so that the server can keep up
					} catch (InterruptedException e) {
						Log.println("ERROR: server HS frame queue thread interrupted");
						e.printStackTrace(Log.getWriter());
					}
				}
				if (formatFrames.size() > 0 && success) {
					if (!Config.passManager.inPass() || (Config.passManager.inPass() && formatFrames.size() > 1))
						success = sendFrame(formatFrames, RAW_PSK_FRAMES_FILE);
					try {
						Thread.sleep(100); // pause so that the server can keep up
					} catch (InterruptedException e) {
						Log.println("ERROR: server PSK frame queue thread interrupted");
						e.printStackTrace(Log.getWriter());
					}
				}
			}
		}
		Log.println("Server Queue thread ended");
	}

	private boolean sendFrame(ConcurrentLinkedQueue<Frame> frames, String file) {
		boolean success = false;
		if (Config.passManager.hasTCA()) {
			PassMeasurement passMeasurement = Config.passManager.getPassMeasurement(); 
			if (frames.peek() != null) {
				frames.peek().setPassMeasurement(passMeasurement);
				Config.passManager.sentTCA();
			}
		}
		
		// Make sure these are up to date
		primaryServer.setHostName(Config.primaryServer);
		secondaryServer.setHostName(Config.secondaryServer);
		
		String protocol = "udp";
		if (Config.serverProtocol == TlmServer.TCP)
			protocol = "tcp";
		Log.println("Trying Primary Server: " + protocol + "://" + Config.primaryServer + ":" + Config.serverPort);
		try {
			if (frames.peek() != null) {
				byte[] buffer = frames.peek().getServerBytes();
				success = primaryServer.sendToServer(buffer, Config.serverProtocol);
			}
		} catch (UnknownHostException e) {
			Log.println("Could not connect to primary server:" + e.getMessage());
			//e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("IO Exception with primary server: " + e.getMessage());
			//e.printStackTrace(Log.getWriter());
		}
		if (running)
			if (Config.sendToBothServers || !success) // We send to the secondary if we failed or if we are sending to both servers
			try {
				Log.println("Trying Secondary Server: " + protocol + "://" + Config.secondaryServer + ":" + Config.serverPort);
				if (frames.peek() != null) {
					byte[] buffer = frames.peek().getServerBytes();
					success = secondaryServer.sendToServer(buffer, Config.serverProtocol);
				}
			} catch (UnknownHostException e) {
				Log.println("Could not connect to secondary server: " + e.getMessage());
				//e.printStackTrace(Log.getWriter());
			} catch (IOException e) {
				Log.println("IO Exception with secondary server: " + e.getMessage());
				//e.printStackTrace(Log.getWriter());
			}
		if (success) // then at least one of the transmissions was successful
			try {
				//Log.println("Sent frame " + frames.get(0).header.toString());
				deleteAndSave(frames, file);
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not remove raw frames from the queue file:\n" + file + "\n"
						+ " The frame will be sent again.  If this error repeats you may need to remove the queue file manually");
				e.printStackTrace(Log.getWriter());
			}
		MainWindow.setTotalQueued(this.rawSlowSpeedFrames.size() + this.rawHighSpeedFrames.size()+ this.formatFrames.size());
		return success; // return true if one succeeded
	}
}