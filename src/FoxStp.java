import java.io.IOException;

import common.Config;
import common.Log;
import telemServer.StpFileProcessException;
import telemetry.Frame;

public class FoxStp {

	public static final String version = "Version 0.1 - 7 December 2015";
	public static final String usage = "FoxStp [-vr] <stp>\n-v - Version Information\n-r - Output raw values\n"
			+ "stp - The STP file to process\n";
	public static void main(String[] args) {
		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = false;
		//Log.init("FoxServer");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kinds


		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.basicInit(); // initialize sequence and spacecraft.  No storage.

		if (args.length > 0) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println(usage);
				System.exit(0);
			}
			if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
				System.out.println("FoxStp. Version " + version);
				System.exit(0);
			}

			if ((args[0].equalsIgnoreCase("-r")) ) {
			
			}

			if ((args[0] != null) ) {
				System.out.println("FoxStp: " + version);
				processStpFile(args[0]);
				System.exit(0);
			}

		} else {
			System.out.println(usage);
		}
		

	}

	private static void processStpFile(String f) {
		try {
			Frame decodedFrame = Frame.loadStp(f);
			if (decodedFrame != null && !decodedFrame.corrupt) {
				System.out.println(decodedFrame.toString());
			}
		} catch (StpFileProcessException e) {
			Log.println("ERROR PROCESSING: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
