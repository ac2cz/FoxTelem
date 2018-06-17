package telemStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import common.Log;
import telemetry.FramePart;
import telemetry.HighSpeedFrame;
import telemetry.PayloadDbStore;
import telemetry.SortedFramePartArrayList;
import telemetry.uw.CanPacket;
import telemetry.uw.PcanPacket;

public class StreamProcess implements Runnable {
	public static final int REFRESH_PERIOD = 5000; // Check every 5 seconds
	public static final String GUEST = "guest";
	public static final String GUEST_PASSWORD = "amsat";
	
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

			/*  REQUIRE username and password. */
			String username = input.readLine();
			Log.println("Username: " + username);
			String password = input.readLine();
			Log.println("Pass: " + password); // this should NOT be logged once we are live
			String spacecraft_id = input.readLine();
			Log.println("Id: " + spacecraft_id); // this should NOT be logged once we are live

			int id = 0;
			try {
				id = Integer.parseInt(spacecraft_id);
			} catch (NumberFormatException e) {
				Log.println("Rejected invalid FoxId: " + spacecraft_id);
				spacecraft_id = null;
			}
			out = socket.getOutputStream();

			if (username != null && password != null && spacecraft_id != null)
				try {
					streamTelemetry(username, password, id, out);
				} catch (SQLException e) {
					Log.println("ERROR: with SQL TRANS" + e.getMessage());
					e.printStackTrace(Log.getWriter());
				}

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
	
	private void streamTelemetry(String user, String pass, int sat, OutputStream out) throws SQLException  {
		boolean streaming=true;

		PayloadDbStore payloadDbStore = null;

		try {
			payloadDbStore = new PayloadDbStore(u,p,db);
			if (!validLogin(payloadDbStore, user, pass)) {
				return;
			}
			CanPacket lastCan = (CanPacket) payloadDbStore.getLatestUwCanPacket(sat);
			int lastPktId;

			if (lastCan.getFoxId() == 0) // there are no CAN Packets yet
				lastPktId = 0;
			else
				if (user.equalsIgnoreCase(GUEST)) {
					// we want to use the date of the most recent packet.  We send new packets live after that
					lastPktId = lastCan.pkt_id;
				} else {
					// we send everything or everything since last connection
					lastPktId = payloadDbStore.getLastCanId(sat, user);
				}

			while (streaming) {
				
				String where = "where pkt_id > '" + lastPktId + "'";
				
				payloadDbStore.derby.setAutoCommit(false); // use a transaction to make sure packets are not written with the same timestamp, but after we query
				SortedFramePartArrayList canPacketsList = payloadDbStore.selectCanPackets(sat, where);
				
				int count=0;
				for (FramePart can : canPacketsList) {
					PcanPacket pc = ((CanPacket)can).getPCanPacket();
					byte[] bytes = pc.getBytes();
					try {
						out.write(bytes);
						out.flush();
						count++;
						lastCan = (CanPacket)can; // make a note each time we send one
					} catch (IOException e) {
						// Client likely disconnected
						streaming = false;
						break;
					} 
				}
				if (count > 0) {
					Log.println("Sent: " + count + " CAN packets to: " + socket.getInetAddress() );
					lastPktId = lastCan.pkt_id;
					if (!user.equalsIgnoreCase(GUEST))
						payloadDbStore.storeLastCanId(sat, user, lastPktId);
				}
				payloadDbStore.derby.commit();
				
				if (streaming) {
					try {
						Thread.sleep(REFRESH_PERIOD);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		} finally {
			try { if (payloadDbStore != null) payloadDbStore.closeConnection(); } catch (Exception e) {	}
		}
	}
	
	private boolean validLogin(PayloadDbStore db, String user, String pass) {
		Statement stmt = null;
		String update = "  SELECT password, salt FROM users where username= '" + user + "'"; // Derby Syntax FETCH FIRST ROW ONLY";
		ResultSet r = null;

		try {
			Connection derby = db.getConnection();
			stmt = derby.createStatement();
			r = stmt.executeQuery(update);
			if (r.next()) {
				String password = r.getString("password");
				String salt = r.getString("salt");
				return true;
			} else {
				Log.println("Invalid username");
				return false;  // invalid username
			}
		} catch (SQLException e) {
			PayloadDbStore.errorPrint("ERROR Check Password SQL:", e);
			try { r.close(); stmt.close();	} catch (SQLException e1) { e1.printStackTrace(); }
		} finally {
			try { if (r != null) r.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return false;
	}
	

}
