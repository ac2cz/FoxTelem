import java.io.IOException;

import common.Config;
import common.Log;
import telemServer.StpFileProcessException;
import telemetry.Frame;
import telemetry.FoxBPSK.FoxBPSKFrame;

public class FoxStp {

	public static final String version = "Version 0.1 - 7 December 2015";
	public static final String usage = "FoxStp [-vrW] <stp>\n"
			+ "-v - Verbose\n"
			+ "-r - Output raw values\n"
			+ "-W - Output timestamps for WOD only\n"
			+ "stp - The STP file to process\n";
	
	static boolean wodTimestampsOnly = false;
	
	public static void main(String[] args) {
		Config.logging = false; // Don't log or this foes in the server log file
		//Log.init("FoxServer");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes to stdout or stderr if this is true
		
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.foxTelemCalcsPosition = false;  // make sure we dont try to get TLEs
		Config.basicInit(); // initialize sequence and spacecraft.  No storage.

		if (args.length > 0) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println(usage);
				System.exit(0);
			}
			if ((args[0].equalsIgnoreCase("-v")) ||args[0].equalsIgnoreCase("-version")) {
				Log.setStdoutEcho(true); // everything goes to stdout or stderr if this is true
			}

			if ((args[0].equalsIgnoreCase("-W")) ) {
				wodTimestampsOnly = true;
				processStpFile(args[1]);
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
			Frame decodedFrame = Frame.loadStp(f, false);
			if (decodedFrame != null && !decodedFrame.corrupt) {
				if (wodTimestampsOnly) {
					FoxBPSKFrame bpsk = (FoxBPSKFrame)decodedFrame;
					System.out.print(decodedFrame.toWodTimestampString(bpsk.getHeader().resets, bpsk.getHeader().getUptime()));
				} else {
					System.out.println(decodedFrame.toString());
				}
			}
		} catch (StpFileProcessException e) {
			Log.println("ERROR PROCESSING: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
