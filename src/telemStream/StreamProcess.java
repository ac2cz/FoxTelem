package telemStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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

import common.Log;
import telemetry.FramePart;
import telemetry.PayloadDbStore;
import telemetry.uw.CanPacket;
import telemetry.uw.PcanPacket;

public class StreamProcess implements Runnable {
	public static final int REFRESH_PERIOD = 1000; // Check every second
	public static final String GUEST = "guest";
	public static final String GUEST_PASSWORD = "amsat";
	public static final int TIMEOUT_CONNECTION = 5*1000; // 5s timeout while connected
	
	static HashMap<String, Boolean> connectedUsers = new HashMap<String, Boolean>(); // the list of users logged in right now
	
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
			socket.setSoTimeout(TIMEOUT_CONNECTION);
			in = socket.getInputStream();
			input = new BufferedReader(new InputStreamReader(in));

			/*  REQUIRE username and password. */
			String username = input.readLine();
			//Log.println("Username: " + username);
			String password = input.readLine();
			//Log.println("Pass: <***>"); 
			String spacecraft_id = input.readLine();
			//Log.println("Id: " + spacecraft_id); // Ask for ID, but only 6 valid currently

			int id = 0;
			try {
				id = Integer.parseInt(spacecraft_id);
			} catch (NumberFormatException e) {
				Log.println("Rejected invalid FoxId: " + spacecraft_id);
				spacecraft_id = null;
			}
			if (id != 6) {
				Log.println("Invalid FoxId for streaming: Connection closed");
				return;
			}
			out = socket.getOutputStream();
			
			if (username != null && password != null && spacecraft_id != null) {
				if (connectedUsers.containsKey(username)) {
					Log.println(username + " already logged in: Connection being closed");
					connectedUsers.put(username, false); // set to false, which will log out the existing users
				} else
					connectedUsers.put(username, true);
				try {
					while (connectedUsers.get(username) != null && connectedUsers.get(username) == false)
						; // wait until the other account is logged out
					if (connectedUsers.get(username) == null)
						connectedUsers.put(username, true);
					streamTelemetry(username, password, id, out, input);
				} catch (SQLException e) {
					Log.println("ERROR: with SQL TRANS" + e.getMessage());
					e.printStackTrace(Log.getWriter());
				}
			}

			in.close();
			out.close();
			socket.close();
			if (username != null) {
				if (connectedUsers.get(username) == false) // we were logged out, so do not remove
					connectedUsers.put(username, true);
				else
					connectedUsers.remove(username);
			}
			Log.println("Connection closed to: " + username +" " + socket.getInetAddress());

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
	
	public static final DateFormat dateFormat = new SimpleDateFormat(
			"yyyyMMddHHmmss", Locale.ENGLISH);
	
	private void streamTelemetry(String user, String pass, int sat, OutputStream out, BufferedReader in) throws SQLException  {
		boolean streaming=true;

		PayloadDbStore payloadDbStore = null;

		try {
			payloadDbStore = new PayloadDbStore(u,p,db);
			if (!validLogin(payloadDbStore, user, pass)) {
				Log.println("Invalid Login for streaming: Connection closed");
				return;
			} else {
				Log.println("Logged in for streaming: " + u);
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
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			while (streaming) {
				
				String where = "where pkt_id > '" + lastPktId + "'";
				
				payloadDbStore.derby.setAutoCommit(false); // use a transaction to make sure packets are not written with the same timestamp, but after we query
				ArrayList<FramePart> canPacketsList = payloadDbStore.selectCanPackets(sat, where);
				int prevPktId = lastPktId;
				int count=0;
				for (FramePart can : canPacketsList) {
					Date captureDate;
					try {
						captureDate = dateFormat.parse(can.getCaptureDate());
					} catch (ParseException e) {
						Log.println("ERROR: Could not parse captureDate.  Setting to current time: " + can.id + " "+ can.resets + ":" + can.uptime +" " + can.getType());
						captureDate = Calendar.getInstance().getTime(); // this is really worst case.  NULL might have been better
					}
					PcanPacket pc = ((CanPacket)can).getPCanPacket(captureDate);
					byte[] bytes = pc.getBytes();
					if (connectedUsers.get(user) != null && connectedUsers.get(user) == true) 
					try {
						out.write(bytes);  // This does not fail if the socket closed.  It fails on the write after that!
						out.flush();
					} catch (IOException e) {
						// Client likely disconnected or was kicked out
						streaming = false;
						if (count > 0) count = count -1;
						lastPktId = prevPktId;
						if (!user.equalsIgnoreCase(GUEST))
							payloadDbStore.storeLastCanId(sat, user, lastPktId);
						break;
					} 
					try {
						String resp = in.readLine(); // listen for ACK.   But this times out as soon as socket disconnected
						Log.print(user + ": " + resp + " ");
						if (resp != null && resp.equalsIgnoreCase("ACK")) {
							count++;
							lastCan = (CanPacket)can; // make a note each time we send one
							prevPktId = lastPktId;
							lastPktId = lastCan.pkt_id;
							if (!user.equalsIgnoreCase(GUEST))
								payloadDbStore.storeLastCanId(sat, user, lastPktId);
							Log.println("=> PktId: "+ lastPktId + " : " + lastCan.resets + ":" + lastCan.uptime +" " + lastCan.getType() );							
						} else {
							// something went wrong and we got a different response
							// We likely need to send this data again.  Drop out of the loop but don't mark this as sent
							break;
						}
					} catch (IOException e) {
						// Client likely disconnected or was kicked out
						streaming = false;
						break;
					} 
				}
				if (count > 0) {
					Log.println("Sent: " + user + " " + count + " CAN packets to: " + socket.getInetAddress() );
				} 
				payloadDbStore.derby.commit();
				
				if (connectedUsers.get(user) == null || connectedUsers.get(user) == false) {
					streaming = false; // we have been logged out
					Log.println(user + " kicked out..");
				}
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
				String checkPassword = get_SHA_256_SecurePassword(pass, salt);
				if (checkPassword.equals(password))
					return true;
				else
					return false;
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
	
	private static String get_SHA_256_SecurePassword(String password, String salt)
    {
        String generatedPassword = null;
        byte[] bytes = null;
		try {
		    MessageDigest digest = MessageDigest.getInstance("SHA-256");
		    bytes = digest.digest((password + salt).getBytes("UTF-8"));
		    for (int i=0; i< 65536; i++)
		    	bytes = digest.digest((toByteString(bytes) + salt).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
		    return null;
		}
		
		generatedPassword = toByteString(bytes);
        return generatedPassword;
    }
     
    
    static String toByteString(byte[] bytes) {
    	StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
		    sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
    }

}
