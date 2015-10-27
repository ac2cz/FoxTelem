import java.io.IOException;
import java.net.ServerSocket;

import telemServer.ServerConfig;
import telemServer.ServerProcess;
import common.Config;
import common.Log;




public class FoxTelemServer {

	public static String version = "Version 0.1";
	public static int port = 41042;
	static int sequence = 0;
	
	public static void main(String[] args) {
		
		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Log.init("FoxServer.log");
		Log.showGuiDialogs = false;
		System.out.println("Starting FoxServer: " + version);
		System.out.println("Listening on port: " + 41042);

		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  This runs in a seperate thread to the GUI and the decoder

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

}
