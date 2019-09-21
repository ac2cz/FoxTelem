import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import common.TlmServer;

/**
 * Test program that sends bad data to the FoxServer for Testing.
 * @author chris
 *
 */
public class BadFox {

	public static void main(String[] args) {
//		emptyFrame();
//		crashInFrame();
//		sendBytes();
		massiveFrame();
	}
	
	@SuppressWarnings("unused")
	private static void emptyFrame() {
		try {
			Socket socket = new Socket("127.0.0.1", 41042);
			OutputStream out = socket.getOutputStream();
			
			out.flush();
			out.close();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void massiveFrame() {
		byte[] bytes = {1,0,45,67,8,9,1,33,4,5};
		try {
			Socket socket = new Socket("127.0.0.1", 41042);
			OutputStream out = socket.getOutputStream();
			
			// Write 10 bytes times 1M, ie about 10MB.  Way over frame size
			for (int i=0; i< 1000000; i++)
				out.write(bytes);
			out.flush();
			out.close();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private static void crashInFrame() {
		byte[] bytes = new byte[10];
		Socket socket = null;
		try {
			socket = new Socket("127.0.0.1", 41042);
			OutputStream out = socket.getOutputStream();

			out.write(bytes);
			System.exit(1);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try{ socket.close(); } catch (Exception e) {};
		}
	}
	
	@SuppressWarnings("unused")
	private static void sendBytes() {
		TlmServer tlmServer = new TlmServer("127.0.0.1", 41042, TlmServer.AUTO_CLOSE, TlmServer.NO_ACK);
		byte[] bytes = new byte[100];
		try {
			tlmServer.sendToServer(bytes, TlmServer.TCP);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
