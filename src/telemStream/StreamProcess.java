package telemStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.Frame;
import telemetry.FramePart;
import telemetry.HighSpeedFrame;
import telemetry.PayloadDbStore;
import telemetry.PayloadMaxValues;
import telemetry.SortedFramePartArrayList;
import telemetry.uw.CanPacket;
import telemetry.uw.PcanPacket;

public class StreamProcess implements Runnable {
	public static final int REFRESH_PERIOD = 5000; // Check every 5 seconds
	
	//PayloadDbStore payloadStoreX;
	String u;
	String p;
	String db;
	private Socket socket = null;
	
	public StreamProcess(String u, String p, String db, Socket socket) {
		this.socket = socket;
		this.u = u;
		this.p = p;
		this.db = db;
	}


	// safety limit to stop massive files being sent to us
	// Max frame size is a high speed frame plus the maximum STP Header Size, which is circa 350 bytes.  1000 used to be conservative
	public static final int MAX_FRAME_SIZE = HighSpeedFrame.MAX_FRAME_SIZE + 1000;
	
	/**
	 * This is started when we have a TCP connection.  We read the data until the connection is closed
	 * This could be one or more STP files.
	 */
	public void run() {
		Log.println("Started Thread to handle connection from: " + socket.getInetAddress());

		InputStream in = null;
		OutputStream out = null;
		BufferedReader input;
		try {
			in = socket.getInputStream();
			/**
			 * We could handle username here and then send unique telemetry that has not been received so far.  
			 * For now the connection just gets live telemetry
			 *
			int b=0;
			String s = "";
			int c;
			while ((c = in.read()) != -1) {
				//f.write(c);
				b++; // bytes received
				if (b > MAX_FRAME_SIZE)
					; // abort the connection
				char ch = (char)c;
				System.out.print(ch);				
			}
   			System.out.println("Received: " + b + " bytes");

			 */
			input = new BufferedReader(new InputStreamReader(in));

			/*  Uncomment to REQUIRE username and password.  This blocks
			String username = input.readLine();
			Log.println("Username: " + username);
			String password = input.readLine();
			Log.println("Pass: " + password); // this should NOT be logged once we are live
			*/
			out = socket.getOutputStream();

//			if (username != null && password != null)
//				streamTelemetry(username, password, 6, out);
			streamTelemetry(null, null, 6, out);

			in.close();
			out.close();
			socket.close();
			Log.println("Connection closed to: " + socket.getInetAddress());

		} catch (SocketException e) {
			Log.println("SOCKET EXCEPTION: " + e.getMessage());
		} catch (IOException e) {
			Log.println("ERROR ALERT:" + e.getMessage());
			e.printStackTrace(Log.getWriter());
			// We could not read the data from the socket or write the file.  So we log an alert!  Something wrong with server
			////ALERT
			Log.alert("FATAL: " + e.getMessage());
		
		} finally {
			try { in.close();  } catch (Exception ex) { /*ignore*/}
			try { out.close();  } catch (Exception ex) { /*ignore*/}
			try { socket.close();  } catch (Exception ex) { /*ignore*/} 
		}
	}
	
	private void streamTelemetry(String user, String pass, int sat, OutputStream out) {
		boolean streaming=true;

		PayloadDbStore payloadDbStore = null;

		try {
			payloadDbStore = new PayloadDbStore(u,p,db);
			CanPacket lastCan = (CanPacket) payloadDbStore.getLatestUwCanPacket(sat);

			while (streaming) {
				// 36 bytes
				String lastDate;
				if (lastCan.serverTimeStamp != null)
					lastDate = lastCan.serverTimeStamp.toString();
				else
					lastDate = "0000-00-00 00:00:00";
				String where = "where date_time > '" + lastDate + "'";
				SortedFramePartArrayList canPacketsList = payloadDbStore.selectCanPackets(sat, where);
				for (FramePart can : canPacketsList) {
					PcanPacket pc = ((CanPacket)can).getPCanPacket();
					byte[] bytes = pc.getBytes();
					try {
						out.write(bytes);
						out.flush();
					} catch (IOException e) {
						// Client likely disconnected
						streaming = false;
						break;
					} 
					Log.println("Sent: " + ((CanPacket)can));
					lastCan = (CanPacket)can;
				}
				
				try {
					Thread.sleep(REFRESH_PERIOD);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		} finally {
			try { if (payloadDbStore != null) payloadDbStore.closeConnection(); } catch (Exception e) {	}
		}
	}
}
