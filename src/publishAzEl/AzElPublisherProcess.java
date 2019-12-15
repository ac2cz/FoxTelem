package publishAzEl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import common.Config;
import common.Log;
import telemetry.FramePart;
import telemetry.PayloadDbStore;
import telemetry.uw.CanPacket;
import telemetry.uw.PcanPacket;

public class AzElPublisherProcess implements Runnable {
	public static final int REFRESH_PERIOD = 1000; // Publish every second
	public static final int TIMEOUT_CONNECTION = 5*1000; // 5s timeout while connected
	
	private Socket socket = null;
	boolean streaming = false;
	
	static final String noSat = "** No Satellite **\n\r";
	
	public AzElPublisherProcess(Socket socket) throws IOException {	
		this.socket = socket;
	}

	/**
	 * This is started when we have a TCP connection.  We send the data until the connection is closed
	 */
	public void run() {
		Log.println("Started Thread to publish positions to: " + socket.getInetAddress());

		InputStream in = null;
		OutputStream out = null;
		BufferedReader input;
		streaming = true;
		
		try {
			socket.setSoTimeout(TIMEOUT_CONNECTION);
			out = socket.getOutputStream();

			while (streaming) {
	        	try {
					Thread.sleep(1000); // send data periodically. Delay in ms
				} catch (InterruptedException e) {
					Log.println("ERROR: server frame queue thread interrupted");
					e.printStackTrace(Log.getWriter());
				} 	

	        	
				byte[] bytes = null; // make the bytes to send here
				if (Config.passManager.lastSatUp == null) {
					bytes = noSat.getBytes();
				} else {
					// String format follows SatPC32 DDE
					// SNAO-95 AZ121.2 EL5.8 UP435185239 UMFM DN145918243 DMFM MA39.5
					double az = FramePart.radToDeg(Config.passManager.lastAz);
					double el = FramePart.radToDeg(Config.passManager.lastEl);
					String position = "SN" + Config.passManager.lastSatUp 
							+ " AZ" + String.format("%3.1f", az) 
							+ " EL" + String.format("%3.1f", el)
							+ " UP" + "0.0" 
							+ " UM" + "FM" 
							+ " DN" + String.format("%9.0f", Config.passManager.lastDoppler) 
							+ " DM" + "FM" 
							+ " MA" + "--" 
							+ "\n\r";
					bytes = position.getBytes();
				}
				try {
					out.write(bytes);  // Note this does not fail if the socket closed.  It fails on the write after that!
					out.flush();
				} catch (IOException e) {
					// Client likely disconnected or was kicked out
					break;
				} 
			}
			
			Log.println("Connection closed to: " + socket.getInetAddress());

		} catch (SocketException e) {
			Log.println("SOCKET EXCEPTION: " + e.getMessage());
		} catch (IOException e) {
			Log.println("IO ERROR:" + e.getMessage());
		} finally {
			try { in.close();  } catch (Exception ex) { /*ignore*/}
			try { out.close();  } catch (Exception ex) { /*ignore*/}
			try { socket.close();  } catch (Exception ex) { /*ignore*/} 
		}
	}

}
