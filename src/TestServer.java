import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Log;
import telemServer.ServerProcess;
import telemServer.StpFileProcessException;

public class TestServer {

	private static int port = 8587;
	
	public static void main(String[] args) {
		
		ServerSocket serverSocket = null;
		boolean listening = true;
		ExecutorService pool = null;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			Log.println("Could not listen on port: " + port);
			Log.alert("FATAL: Could not listen on port: " + port);
		}
		while (listening) {
			try {
        		//process = new ServerProcess(serverSocket.accept(), sequence++);
        		Log.println("Waiting for connection ...");
        		Socket socket = serverSocket.accept(); // blocks till we get a connection
        		InputStream in = null;
        		int b=0;
        		in = socket.getInputStream();
        		int c;
        		while ((c = in.read()) != -1) {
        			System.out.write(c);
        			b++;
        		}

        		in.close();
        		socket.close();
       			System.out.println();
       			System.out.println("Received: " + b + " bytes");
       	     
			}  catch (SocketTimeoutException s) {
        		Log.println("Socket timed out! - trying to continue	");
        		try { Thread.sleep(1000); } catch (InterruptedException e1) {	}
        	} catch (IOException e) {
        		e.printStackTrace(Log.getWriter());
        		Log.println("Socket Error: waiting to see if we recover: " + e.getMessage());
        		try { Thread.sleep(1000); } catch (InterruptedException e1) {	}
        	}
		}
	}
}
