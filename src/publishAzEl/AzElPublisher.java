package publishAzEl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Log;

public class AzElPublisher implements Runnable {
	
	int port = 0;
	int poolSize = 8;
	boolean running = true;
	
	public AzElPublisher (int port) {
		this.port = port;
	}
	
	public void stopProcessing() {
		running = false;
	}
	
	@Override
	public void run() {
		Log.println("Listening on port: " + port + " to publish Az El information");
		
		ServerSocket serverSocket = null;
        running = true;
        ExecutorService pool = null;

        try {
        	serverSocket = new ServerSocket(port);
        	pool = Executors.newFixedThreadPool(poolSize);
        } catch (IOException e) {
        	Log.println("Could not listen on port: " + port);
        	Log.alert("FATAL: Could not listen on port: " + port);
        }
        
        int retries = 0;
        int RETRY_LIMIT = 10;
        while (running) {
        	try {
        		//process = new ServerProcess(serverSocket.accept(), sequence++);
        		Log.println("Waiting for connection from Antenna Controller or Radio ...");
        		pool.execute(new AzElPublisherProcess(serverSocket.accept()));
        		retries = 0;
        	}  catch (SocketTimeoutException s) {
        		Log.println("Socket timed out! - trying to continue	");
        		retries++;
        		try { Thread.sleep(1000); } catch (InterruptedException e1) {	}
        	} catch (IOException e) {
        		e.printStackTrace(Log.getWriter());
        		Log.println("Socket Error: waiting to see if we recover: " + e.getMessage());
        		retries++;
        		try { Thread.sleep(1000); } catch (InterruptedException e1) {	}
        	}
            if (retries == RETRY_LIMIT) {
            	Log.println("Max Socket Retries hit: Terminating Server");
            	running = false;
            }
        }

        try {
			serverSocket.close();
			pool.shutdown();
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}

	}

}
