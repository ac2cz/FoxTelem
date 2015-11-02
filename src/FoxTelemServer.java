import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import telemServer.ServerProcess;
import telemServer.StpFileProcessException;
import telemetry.Frame;
import common.Config;
import common.Log;




public class FoxTelemServer {

	public static String version = "Version 0.1";
	public static int port = 41042;
	static int sequence = 0;
	private static final int MAX_SEQUENCE = 1000;// This needs to be larger than the maximum number of connections in a second so we dont get duplicate file names
	
	public static void main(String[] args) {
		
		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = true;
		Log.init("FoxServer");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kinds

		try {
			makeExceptionDir();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace(Log.getWriter());
			System.exit(1);
		}

		Log.println("Starting FoxServer: " + version);
		Log.println("Listening on port: " + 41042);

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
			
				Log.println("AMSAT Fox Server. \nSTP FILE LOAD FROM DIR: " + dir);
				importStp(dir, false);
				System.exit(0);
			}

		}
		
		ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            serverSocket = new ServerSocket(Config.tcpPort);
        } catch (IOException e) {
            Log.println("Could not listen on port: " + Config.tcpPort);
            System.exit(-1);
        }

        ServerProcess process = null;
        Thread processThread;
        while (listening) {
        	try {
				process = new ServerProcess(serverSocket.accept(), sequence++);
			}  catch (SocketTimeoutException s) {
		        Log.println("Socket timed out! - trying to continue	");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
        	if (process != null) {
        		Log.println("Started Thread to handle connection from: " + serverSocket.getInetAddress());
        		processThread = new Thread(process);
        		processThread.start();
        	}
            if (sequence == MAX_SEQUENCE)
            	sequence=0;
        }

        try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(Log.getWriter());
		}
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
					try {
						Frame f = Frame.importStpFile(listOfFiles[i], true);
						if (f == null) {
							// null data - try to delete it but do nothing if we can not (so don't check the return code
							listOfFiles[i].delete();
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
