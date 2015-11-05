package telemServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import telemetry.LayoutLoadException;
import telemetry.PayloadRtValues;
import common.Config;
import common.Log;

public class WebServiceProcess implements Runnable {

	private Socket socket = null;
	
	public WebServiceProcess(Socket socket) {
		this.socket = socket;
	}

	
	@Override
	public void run() {
		try {
			Log.println("Started Thread to handle connection from: " + socket.getInetAddress());

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream());

			// read the data sent. 
			// stop reading once a blank line is hit. This
			// blank line signals the end of the client HTTP headers.
			String str = ".";
			String GET = in.readLine();
			if (GET != null) {
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

				//			String path = request.substring(1, request.length());

				WebHealthTab fox1Atab = null;

				try {
					fox1Atab = new WebHealthTab(Config.satManager.getSpacecraft(1));
				} catch (LayoutLoadException e1) {
					e1.printStackTrace(Log.getWriter());
				}

				String[] path = request.split("/");
				if (path.length > 0) {
					if (path[1].equalsIgnoreCase("version")) {
						out.println("Fox Web Service - 0.02");
					} else if (path[1].equalsIgnoreCase("1A")) {
						// Send the HTML page
						PayloadRtValues rt = Config.payloadStore.getLatestRt(1);
						if (rt != null) {
							if (path.length == 2) {
								fox1Atab.setRtPayload(rt);
								out.println(fox1Atab.toString());
							} else if (path.length == 3) {
								fox1Atab.setRtPayload(rt);
								out.println(fox1Atab.toGraphString(path[2]));
							}
						} else {
							out.println("FOX SERVER Currently not returning data....\n");
						}
					} else if (path[1].equalsIgnoreCase("1C")) {
						// Send the HTML page
						PayloadRtValues rt = Config.payloadStore.getLatestRt(3);
						if (rt != null)
							out.println("<H2>Fox-1C Telemetry</H2>" + rt.toWebString());
					} else if (path[1].equalsIgnoreCase("1D")) {
						// Send the HTML page
						PayloadRtValues rt = Config.payloadStore.getLatestRt(4);
						if (rt != null)
							out.println("<H2>Fox-1D Telemetry</H2>" + rt.toWebString());
					} else {
						// Send the DEFAULT HTML page
						out.println("<H2>AMSAT FOX WEB SERVICE - TEST POINT</H2>");
					}
				} else {
					out.println("<H2>AMSAT FOX WEB SERVICE</H2>");
				}
			}
			out.flush();
			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			Log.println("ERROR: IO Exception in Webservice" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				// ignore
			}
		}

		
	}

}
