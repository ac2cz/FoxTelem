import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import telemServer.ImageProcess;
import telemServer.ServerConfig;
import telemServer.ServerProcess;
import telemServer.StpFileProcessException;
import telemetry.PayloadDbStore;
import telemetry.frames.Frame;
import common.Config;
import common.Log;

/**
 * FOX 1 Telemetry Server
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
 * Version 0.6
 * Added -r option to init the secondary radiation payloads
 * 
 */

public class FoxTelemServer {

	public static String version = "Version 0.37 - 25 July 2021";
	public static int port = Config.tcpPort;
	static int sequence = 0;
	private static final int MAX_SEQUENCE = 1000;// This needs to be larger than the maximum number of connections in a second so we dont get duplicate file names
	static int poolSize = 8; // max number of threads
	static final String usage = "FoxServer user database [-vr] [-s dir] [-f dir]\n-v - Version Information\n"
			+ "-s <dir> - Process all of the stp files in the specified directory and load them into the db\n"
			+ "-f <dir> - Read the stp files in the specified directory and fix the STP_HEADER table db\n"
			+ "-r - Reprocess the radiation data and generate the secondary payloads\n"
			+ "-hpk - Reprocess the Herci High Speed data and generate the packet payloads\n";
	
	static Thread imageThread;
	static ImageProcess imageProcess;
	
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println(usage);
				System.exit(0);
			} else
			if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
				System.out.println("AMSAT Fox Server. Version " + version);
				System.exit(0);
			} else {
				System.out.println(usage);
				System.exit(1);
			}
				
		}
		String u,db;
		if (args.length < 2) {
			System.out.println(usage);
			System.exit(1);
		}
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
		Log.init("FoxServer");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kinds

		// Avoid creating all of the tables on the server
		Config.splitCanPackets = false;

		try {
			makeExceptionDir();
		} catch (IOException e1) {
			e1.printStackTrace(Log.getWriter());
			Log.alert("FATAL: Cannot create exception dir");
		}

		Log.println("Starting FoxServer: " + version);
		Log.println("Listening on port: " + port);

		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  

		ServerConfig.init();
				
		if (args.length == 3) {
			if ((args[2].equalsIgnoreCase("-r")) ) {
				Log.println("AMSAT Fox Server. \nPROCESS RAD DATA: ");
				processRadData(u,p,db);
				System.exit(0);
			} else if ((args[2].equalsIgnoreCase("-hpk")) ) {
				Log.println("AMSAT Fox Server. \nPROCESS HERCI PACKET DATA: ");
				processHerciPktData(u,p,db);
				System.exit(0);
			} else {
				System.out.println(usage);
				System.exit(1);
			}
		} else
		if (args.length == 4) {

			if ((args[2].equalsIgnoreCase("-s")) ) {
				String dir = args[3]; 

				Log.println("AMSAT Fox Server. \nSTP FILE LOAD FROM DIR: " + dir);
				importStp(dir, false, u,p,db);
				System.exit(0);
			} else

			if ((args[2].equalsIgnoreCase("-f")) ) {
				String dir = args[3]; 

				Log.println("AMSAT Fox Server. \nSTP FILE fix: " + dir);
				fixStp(dir);
				System.exit(0);
			} else {
				System.out.println(usage);
				System.exit(1);
			}
		} else if (args.length > 4) {
			System.out.println(usage);
			System.exit(1);
		}

		// Make sure CAN Packets are not split
		Config.splitCanPackets=false;

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

        // Start the background image processing thread
        imageProcess = new ImageProcess(initPayloadDB(u,p,db));
        imageThread = new Thread(imageProcess);
        imageThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
        imageThread.start();
        
        int retries = 0;
        int RETRY_LIMIT = 10;
        while (listening) {
        	try {
        		Log.println("Waiting for connection ...");
        		pool.execute(new ServerProcess(u,p,db, serverSocket.accept(), sequence++));
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
            if (sequence == MAX_SEQUENCE)
            	sequence=0;
            if (retries == RETRY_LIMIT) {
            	Log.println("Max Socket Retries hit: Terminating Server");
            	listening = false; // Note if this error is causes because process threads are stuck then we will not Exit by setting to false, but hang
            }
        }

        try {
			serverSocket.close();
			pool.shutdown();
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}
    }
	
	public static PayloadDbStore initPayloadDB(String u, String p, String db) {	
		return new PayloadDbStore(u,p,db);
		
	}
	public static void makeExceptionDir() throws IOException {
		File aFile = new File("exception");
		if(aFile.isDirectory()){
			// already exists
			return;
		} else {
			aFile.mkdir();
		}
		if(!aFile.isDirectory()){
			throw new IOException("Can not make the exception dir");
		}
	}

	private static void processRadData(String u, String p, String db) {
		Config.payloadStore = initPayloadDB(u,p,db);
		Config.payloadStore.initRad2();
	}
	
	private static void processHerciPktData(String u, String p, String db) {
		Config.payloadStore = initPayloadDB(u,p,db);
		Config.payloadStore.initHerciPackets();
	}
	
		
	/**
	 * Get a list of all the files in the STP dir and import them
	 */
	private static void fixStp(String stpDir) {
		String dir = stpDir;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator + dir;

		}
		Log.println("FIX STP files in " + dir);
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() ) {
					//Log.print("Loading STP data from: " + listOfFiles[i].getName());
					try {
						Frame f = Frame.loadStp(listOfFiles[i].getPath(), true);
						if (f == null || f.corrupt) {
							// null data - try to delete it but do nothing if we can not (so don't check the return code
						} else{
							Config.payloadStore.updateStpHeader(f);
						}
					} catch (StpFileProcessException e) {
						Log.println("STP IMPORT ERROR: " + e.getMessage());
						e.printStackTrace(Log.getWriter());
					} catch (IOException e) {
						Log.println("STP IO ERROR: " + e.getMessage());
						e.printStackTrace();
					}
					//if (f != null)
					//	Log.println(" ... " + f.getHeader().getResets() + " " + f.getHeader().getUptime());
					if (i%100 == 0)
						Log.println("Loaded: " + 100.0*i/listOfFiles.length +"%");
				}
			}
			Log.println("Files Processed: " + listOfFiles.length);
		}
	}

/**
 * Get a list of all the files in the STP dir and import them
 */
private static void importStp(String stpDir, boolean delete, String u, String p, String db) {
	//PayloadDbStore payload = initPayloadDB(u,p,db);
	String dir = stpDir;
	if (!Config.logFileDirectory.equalsIgnoreCase("")) {
		dir = Config.logFileDirectory + File.separator + dir;

	}
	Log.println("IMPORT STP from " + dir);
	File folder = new File(dir);
	File[] listOfFiles = folder.listFiles();
	if (listOfFiles != null) {
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() ) {
				//Log.print("Loading STP data from: " + listOfFiles[i].getName());
				try {
					Frame f = Frame.importStpFile(u,p,db, listOfFiles[i], false);
					if (f == null) {
						// null data -  do nothing if we can not (so don't check the return code
						//listOfFiles[i].delete();
					}
				} catch (StpFileProcessException e) {
					Log.println("STP IMPORT ERROR: " + e.getMessage());
					e.printStackTrace(Log.getWriter());
				}
				//if (f != null)
				//	Log.println(" ... " + f.getHeader().getResets() + " " + f.getHeader().getUptime());
				if (i%100 == 0)
					Log.println("Loaded: " + 100.0*i/listOfFiles.length +"%");
			}
		}
		Log.println("Files Processed: " + listOfFiles.length);
	}
}

}