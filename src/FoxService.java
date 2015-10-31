import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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
		Log.init("FoxServer.log");
		Log.showGuiDialogs = false;

		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		Config.serverInit(); // initialize and create the payload store.  This runs in a seperate thread to the GUI and the decoder

		System.out.println("Fox Webservice starting up on port 8080");
		System.out.println("(press ctrl-c to exit)");
		try {
			// create the main server socket
			s = new ServerSocket(8080);
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}

		for (;;) {
			try {
				// wait for a connection
				Socket remote = s.accept();
				// remote is now the connected socket
				System.out.println("Connection, sending data.");
				BufferedReader in = new BufferedReader(new InputStreamReader(
						remote.getInputStream()));
				PrintWriter out = new PrintWriter(remote.getOutputStream());

				// read the data sent. 
				// stop reading once a blank line is hit. This
				// blank line signals the end of the client HTTP
				// headers.
				String str = ".";
				String GET = in.readLine();
				System.out.println(GET);
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
					out.println("<H2>Fox-1A Telemetry</H2>" + rt.toWebString());
				} else {
				// Send the HTML page
				out.println("<H2>AMSAT!</H2>");
				}
				
				out.flush();
				remote.close();
			} catch (Exception e) {
				System.out.println("Error: " + e);
			}
		}
	}
}