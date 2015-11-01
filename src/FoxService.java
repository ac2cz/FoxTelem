import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import telemServer.WebHealthTab;
import telemetry.LayoutLoadException;
import telemetry.PayloadRtValues;
import common.Config;
import common.Log;

public class FoxService {

	public static void main(String args[]) {
		FoxService ws = new FoxService();
		ws.start();
	}

	protected void start() {
		ServerSocket s;

		// Need server Logging and Server Config.  Do not want to mix the config with FoxTelem
		Config.logging = true;
		Log.init("FoxWebService");
		Log.showGuiDialogs = false;
		Log.setStdoutEcho(false); // everything goes in the server log.  Any messages to stdout or stderr are a serious bug of some kinds
		
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  This runs in a seperate thread to the GUI and the decoder

		Log.println("Fox Webservice starting up on port 8080");
		Log.println("(press ctrl-c to exit)");
		try {
			// create the main server socket
			s = new ServerSocket(8080);
		} catch (Exception e) {
			Log.println("Error: " + e);
			return;
		}

		WebHealthTab fox1Atab = null;
		try {
			fox1Atab = new WebHealthTab(Config.satManager.getSpacecraft(1));
		} catch (LayoutLoadException e1) {
			e1.printStackTrace(Log.getWriter());
			System.exit(1);
		}
		for (;;) {
			try {
				// wait for a connection
				Socket remote = s.accept();
				// remote is now the connected socket
				Log.println("Connection, sending data to: " + remote.getInetAddress());
				BufferedReader in = new BufferedReader(new InputStreamReader(
						remote.getInputStream()));
				PrintWriter out = new PrintWriter(remote.getOutputStream());

				// read the data sent. 
				// stop reading once a blank line is hit. This
				// blank line signals the end of the client HTTP
				// headers.
				String str = ".";
				String GET = in.readLine();
				Log.println(GET);
				String[] requestLine = GET.split(" "); // GET <path> HTTP/1.1
				String request = new String(requestLine[1]);
				while (!str.equals("")) {
					str = in.readLine(); // ignore the rest of the header
				}

				// Send the response
				// Send the headers
				out.println("HTTP/1.0 200 OK");
				out.println("Content-Type: text/html");
				out.println("Server: Bot");
				// this blank line signals the end of the headers
				out.println("");
				
				String path = request.substring(1, request.length());
				if (path.equalsIgnoreCase("1A")) {
					// Send the HTML page
					PayloadRtValues rt = Config.payloadStore.getLatestRt(1);
					if (rt != null)
						fox1Atab.setRtPayload(rt);
						out.println(fox1Atab.toString());
				} else if (path.equalsIgnoreCase("1C")) {
					// Send the HTML page
					PayloadRtValues rt = Config.payloadStore.getLatestRt(3);
					if (rt != null)
						out.println("<H2>Fox-1C Telemetry</H2>" + rt.toWebString());
				} else if (path.equalsIgnoreCase("1D")) {
					// Send the HTML page
					PayloadRtValues rt = Config.payloadStore.getLatestRt(4);
					if (rt != null)
						out.println("<H2>Fox-1D Telemetry</H2>" + rt.toWebString());
				} else {
				// Send the DEFAULT HTML page
					out.println("<H2>AMSAT FOX WEB SERVICE - TEST POINT</H2>");
				}
				
				out.flush();
				remote.close();
			} catch (Exception e) {
				Log.println("Error: " + e);
				e.printStackTrace(Log.getWriter());
			}
		}
	}
}