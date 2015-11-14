import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import telemServer.ServerProcess;
import telemServer.WebHealthTab;
import telemServer.WebServiceProcess;
import telemetry.LayoutLoadException;
import telemetry.PayloadRtValues;
import common.Config;
import common.Log;

public class FoxService {

	public static String version = "Version 0.7";
	public static int port = 8080;
	int poolSize = 100;
	
	public static void main(String args[]) {
		FoxService ws = new FoxService();
		ws.start();
	}

	protected void start() {
		

		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = true;
		Log.init("FoxWebService");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kinds
		
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  This runs in a seperate thread to the GUI and the decoder

		Log.println("Fox Webservice starting up on port " + port + ": " + version);
		Log.println("(press ctrl-c to exit)");

		ServerSocket serverSocket = null;
        boolean listening = true;
        ExecutorService pool = null;

        try {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(poolSize);
            } catch (IOException e) {
            Log.println("Could not listen on port: " + port);
            System.exit(-1);
        }

        WebServiceProcess process = null;
        Thread processThread;
        
        while (listening) {
        	try {
        		//process = new ServerProcess(serverSocket.accept(), sequence++);
        		Log.println("Waiting for WebService connection ...");
        		pool.execute(new WebServiceProcess(serverSocket.accept()));
        	}  catch (SocketTimeoutException s) {
        		Log.println("Socket timed out! - trying to continue	");
        	} catch (IOException e) {
        		// TODO Auto-generated catch block
        		e.printStackTrace(Log.getWriter());
        	}
        }

        try {
			serverSocket.close();
			pool.shutdown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(Log.getWriter());
		}

	}
}