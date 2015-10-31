import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import telemServer.ServerConfig;
import telemServer.ServerProcess;
import telemetry.Frame;
import common.Config;
import common.Log;




public class FoxTelemServer {

	public static String version = "Version 0.1";
	public static int port = 41042;
	static int sequence = 0;
	
	public static void main(String[] args) {
		
		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = true;
		Log.init("FoxServer.log");
		Log.showGuiDialogs = false;
		
		System.out.println("Starting FoxServer: " + version);
		System.out.println("Listening on port: " + 41042);

		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  This runs in a seperate thread to the GUI and the decoder

		if (args.length > 0) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println("FoxServer [-v] [-s dir]\n-v - Version Information\n-s <dir> - Process all of the stp files in the specified directory and load them into the db\n");
				System.exit(0);
			}
			if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
				System.out.println("AMSAT Fox Server. Version " + Config.VERSION);
				System.exit(0);
			}

			if ((args[0].equalsIgnoreCase("-s")) ) {
				String dir = args[1]; 
			
				System.out.println("AMSAT Fox Server. \nSTP FILE LOAD FROM DIR: " + dir);
				importStp(dir, false);
				System.exit(0);
			}

		}
		
		ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            serverSocket = new ServerSocket(Config.tcpPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + Config.tcpPort);
            System.exit(-1);
        }

        ServerProcess process = null;
        Thread processThread;
        while (listening) {
        	try {
				process = new ServerProcess(serverSocket.accept(), sequence++);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	if (process != null) {
        		System.out.println("Started Thread to handle connection");
        		processThread = new Thread(process);
        		processThread.start();
        	}
        }

        try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	/**
	 * Get a list of all the files in the STP dir and import them
	 */
	private static void importStp(String stpDir, boolean delete) {
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
					Frame f = Frame.importStpFile(listOfFiles[i], true);
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
