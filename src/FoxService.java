import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import telemServer.WebServiceProcess;
import telemetry.PayloadDbStore;
import common.Config;
import common.Log;

/**
 * 
 * @author chris.e.thompson
 *
 *
 * Verion 0.9
 * T0 Analysis added as a service
 * 
 */
public class FoxService {

	public static String version = "Version 0.24a - 24 Oct 2017";
	public static int port = 8080;
	int poolSize = 100;
	
	public static void main(String args[]) throws IOException {
		FoxService ws = new FoxService();
		String u,p, db;
		if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
			System.out.println("AMSAT Fox Web Service. Version " + version);
			System.exit(0);
		}
		if (args.length == 3) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		    p = in.readLine();
			if (p == null || p.isEmpty()) {
				System.out.println("Missing password");
				System.exit(2);
			}
			u = args[0];
			db = args[1];
			try {
			port = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.println("FATAL: Invalid Port - " + port);
				Log.println("FATAL: Invalid Port - " + port);
				System.exit(1);
			}
			ws.start(u,p,db);

		} else {
			System.out.println("Usage: FoxService user database port");
			System.exit(1);
		}
	}

	protected void start(String u, String p, String db) {
		
		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = true;
		Log.init("FoxWebService");
		Log.alertsAreFatal = false;
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kinds
		
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  This runs in a seperate thread to the GUI and the decoder

		Log.println("Fox Webservice starting up on port " + port + ": " + WebServiceProcess.version);
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

        while (listening) {
        	try {
        		//process = new ServerProcess(serverSocket.accept(), sequence++);
        		Log.println("Waiting for WebService connection ...");
        		pool.execute(new WebServiceProcess(initPayloadDB(u,p,db),serverSocket.accept(),port));
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
	
	public static PayloadDbStore initPayloadDB(String u, String p, String db) {	
		return new PayloadDbStore(u,p,db);
		
	}
}