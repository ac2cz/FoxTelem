package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

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
 * Class the holds the connection to the telemetry server and send data to it
 * 
 *
 */
public class TlmServer {
	public static final int TCP = 0;
	public static final int UDP = 1;
	public static final int FTP_PORT = 22;
	public static final boolean KEEP_OPEN = false;
	public static final boolean AUTO_CLOSE = true;
	public static final boolean WAIT_FOR_ACK = true;
	public static final boolean NO_ACK = false;

	public static final int TIMEOUT_CONNECT = 5000; // 5s timeout on trying to connect
	public static final int TIMEOUT_CONNECTION = 2000; // 2s timeout for blocking read while connected

	String hostName;
	int portNumber;
	boolean autoClose = true; // If this is false we keep the socket open, but reopen if it is closed
	boolean waitForAck = true; // If this is true we listen for OK or FAIL from the server
	Socket socket = null;
	OutputStream out = null;
	InputStream in = null;
	
	public TlmServer(String hostName, int portNumber, boolean autoClose, boolean waitForAck) {
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.autoClose = autoClose;
		this.waitForAck = waitForAck;
	}

	public void setHostName(String hostName) {
		if (this.hostName != hostName)
			close();
		this.hostName = hostName;
	}
	
	public void setPort(int port) {
		if (this.portNumber != port)
			close();
		this.portNumber = port;
	}
		
	/**
	 * Use TCP, even if we are in UDP mode, to check that the server is there
	 * @return
	 
	public boolean ping() {
		Log.print("Ping: " + hostName + "..");
		byte[] buffer = new byte[2];
		
		Socket socket = null;
		try {
			socket = new Socket(hostName, FTP_PORT);
			socket.close();
		} catch (UnknownHostException e) {
			Log.println(" no connection");
			return false;
		} catch (IOException e) {
			Log.println(" no connection");
			return false;
		}
		Log.println(" connected");
		return true;
	}
	*/
	
	public void close() {
		if (out != null)
			try {
				out.close();
				in.close();
			} catch (Exception e) {
				// Nothing to do
			}
		out = null;
		in = null;
		if (socket != null)
			try {
				socket.close();
			} catch (Exception e) {
				// Nothing to do
			}
		socket = null;
	}
	
	/**
	 * Send this frame to the amsat server "hostname" on "port" for storage.  It is sent by the queue
	 * so there is no need for this routine to remove the record from the queue
	 * @param hostName
	 * @param port
	 */
	public boolean sendToServer(byte[] buffer, int protocol) throws UnknownHostException, IOException {
		boolean success = false;
		if (protocol == TCP) {
			if (autoClose || socket == null) {
				socket = new Socket();
				socket.connect(new InetSocketAddress(hostName, portNumber), TIMEOUT_CONNECT);
				if (waitForAck) // then we need a timeout on the read
					socket.setSoTimeout(TIMEOUT_CONNECTION);
				
				out = socket.getOutputStream();
				in = socket.getInputStream();
			}

			out.write(buffer);
			if (waitForAck)
				success = waitForAck();
			else
				success = true;
			if (autoClose) {
				close();
			}
			return success;
			
		} else {
			DatagramSocket socket = new DatagramSocket();

			InetAddress address = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
					address, portNumber);
			socket.send(packet);
			socket.close();
			return success;
		}
	}
	
	public static final int[] OK = {0x4F,0x4D,0x0D,0x0A};
	public static final int[] FAIL = {0x46,0x41,0x0D,0x0A};
	
	private boolean waitForAck() throws IOException {
		boolean success = false;
		int c;
		int i=0;
		final int LEN=4;
		int[] buffer = new int[LEN];
		boolean done = false;
		
		while (!done && (c = in.read()) != -1) {
			buffer[i++] = c;
			if (i == LEN)
				done = true;
			if ( (c == 13 || c == 10)) { // CR or LF
				c = in.read(); // consume the lf
				buffer[i++] = c;
				if (Arrays.equals(OK, buffer)) {
					Log.println("SERVER ACK: OK");
					return true;
				} else {
					Log.println("SERVER ACK: FAIL");
					return false;
				}
			}
		}
		return success;
	}
	
}
