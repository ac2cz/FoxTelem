import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Config;
import common.Log;
import telemServer.ServerConfig;
import telemStream.StreamProcess;


/**
 * FOX 1 Telemetry Server
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
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
 * This is the main class and entry point for the Fox Telemetry Streamer.  It is a server
 * that accepts TCP connections and streams telemetry to the listener.
 * 
 */
public class FoxStream {

	public static String version = "Version 0.06 - 29 Nov 2019";
	public static int port = 41043;
	static int sequence = 0;
	static int poolSize = 16; // max number of threads
	static final String usage = "FoxStream user database [-v]\n"
			+ "-v - Version Information\n";
	
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println(usage);
				System.exit(0);
			} else
			if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
				System.out.println("AMSAT Fox Stream. Version " + version);
				System.exit(0);
			} else {
				System.out.println(usage);
				System.exit(1);
			}
		} else if (args.length != 2) {
			System.out.println(usage);
			System.exit(1);
		}

		String u,db;
		u = args[0];
		db = args[1];

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String p;	
	    p = in.readLine();
		if (p == null || p.isEmpty()) {
			System.out.println("Missing password");
			System.exit(2);
		}
		
		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = true;
		Log.init("FoxStream");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kind

		Log.println("Starting FoxStream: " + version);
		Log.println("Listening on port: " + port);

		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  

		ServerConfig.init();
		
		ServerSocket serverSocket = null;
        boolean listening = true;
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
        while (listening) {
        	try {
        		//process = new ServerProcess(serverSocket.accept(), sequence++);
        		Log.println("Waiting for connection ...");
        		pool.execute(new StreamProcess(u,p,db, serverSocket.accept()));
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
            	listening = false;
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
