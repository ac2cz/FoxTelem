package telemServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import telemetry.Frame;
import telemetry.LayoutLoadException;
import telemetry.PayloadDbStore;
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


				/*
				 * The FoxService Command API is in the format:
				 * COMMAND/SAT/OPTIONS
				 * 
				 * The following commands are valid:
				 * /VERSION - return the version
				 * /T0/SAT/RESET/CALL/N - Returns N T0 records for RESET logged by CALL for sat SAT
				 * /FRAME/SAT/TYPE - Return the latest frame of type TYPE for SAT
				 * /FIELD/SAT/NAME/R|C/N/RESET/UPTIME - Return N R-RAW or C-CONVERTED values for field NAME from sat SAT from RESET and UPTIME
				 * 
				 */
				String[] path = request.split("/");
				if (path.length > 0) { // VERSION COMMAND
					if (path[1].equalsIgnoreCase("version")) {
						out.println("Fox Web Service Starting...");
					} else if (path[1].equalsIgnoreCase("T0")) { // T0 COMMAND
						if (path.length == 6) {
							try {
								String t0 = calculateT0(out, Integer.parseInt(path[2]), Integer.parseInt(path[3]), path[4],  Integer.parseInt(path[5]));
								out.println(t0 + "\n");
							} catch (NumberFormatException e) {
								out.println("FOX T0 Request is invalid id + " + path[2] + " reset " + path[3] + " number " + path[5]+"\n");								
							}
						} else {
							out.println("FOX T0 Request invalid\n");
						}
					} else if (path[1].equalsIgnoreCase("FRAME")) { // Frame Command
						// Send the HTML page
						if (path.length == 4) {
							int sat = Integer.parseInt(path[2]);
							int type = Integer.parseInt(path[3]);
							PayloadRtValues rt = Config.payloadStore.getLatestRt(sat);
							if (rt != null) {								
								try {
									fox1Atab = new WebHealthTab(Config.satManager.getSpacecraft(sat));
								} catch (LayoutLoadException e1) {
									e1.printStackTrace(Log.getWriter());
								}
								//out.println("<H2>Fox-1 Telemetry</H2>");
								fox1Atab.setRtPayload(rt);
								out.println(fox1Atab.toString());
							} else {
								out.println("FOX SERVER Currently not returning data....\n");
							}
						} else {
							out.println("FOX FRAME Request invalid\n");
						}
					} else if (path[1].equalsIgnoreCase("FIELD")) { // Field Command
						// /FIELD/SAT/NAME/R|C/N/RESET/UPTME - Return N R-RAW or C-CONVERTED values for field NAME from sat SAT
						if (path.length == 8) {
							int sat = Integer.parseInt(path[2]);
							String name = path[3];
							String raw = path[4];
							boolean convert = true;
							int num = Integer.parseInt(path[5]);
							int fromReset = Integer.parseInt(path[6]);
							int fromUptime = Integer.parseInt(path[7]);
							try {
								fox1Atab = new WebHealthTab(Config.satManager.getSpacecraft(sat));
							} catch (LayoutLoadException e1) {
								e1.printStackTrace(Log.getWriter());
							}
							if (raw.startsWith("C"))
								convert = false;
							out.println(fox1Atab.toGraphString(name, convert, num, fromReset, fromUptime));
						} else {
							out.println("FOX FIELD Request invalid\n");
						}
					}
				}
			} else {
				out.println("<H2>AMSAT FOX WEB SERVICE</H2>");
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

	String calculateT0(PrintWriter out, int sat, int reset, String receiver, int num) {
		Statement stmt = null;
		Frame.stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		String update = "  SELECT stpDate, uptime FROM STP_HEADER "
				+ "where id = "+sat+" and resets = "+reset+" and receiver = '"+receiver+"' "
				+ "ORDER BY resets DESC, uptime DESC LIMIT " + num; // Derby Syntax FETCH FIRST ROW ONLY";
		String stpDate[];
		long uptime[];
		long t0Sum = 0L;
		String s = "";
		s = s + "<style> td { border: 5px } th { background-color: lightgray; border: 3px solid lightgray; } td { padding: 5px; vertical-align: top; background-color: darkgray } </style>";	
		s = s + "<table><tr><th>STP Date</th><th>Uptime</th><th>Estimated T0</th></tr>";
		
		try {
			Connection derby = PayloadDbStore.getConnection();
			stmt = derby.createStatement();
			//Log.println(update);
			ResultSet r = stmt.executeQuery(update);
			int size = 0;
			int i=0;
			if (r.last()) {
				size = r.getRow();
				r.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first element
			}
			stpDate = new String[size];
			uptime = new long[size];
			if (size == 0) return "No Frames for sat "+sat+" from reset " + reset +" for receiver " + receiver;
			while (r.next()) {
				uptime[i] = r.getLong("uptime");
				stpDate[i] = r.getString("stpDate");
				
				Date stpDT;
				try {
					stpDT = Frame.stpDateFormat.parse(stpDate[i]);
					
				long stp = stpDT.getTime()/1000; // calculate the number of seconds in out stp data since epoch
				long T0 = stp - uptime[i];
				t0Sum += T0;
				Date T0DT = new Date(T0*1000);
				s = s + "<tr><td>" + stpDate[i] + "</td><td>" + uptime[i] + "</td><td>" + T0DT +"</td></tr>";
				i++;
				} catch (ParseException e) {
					// ignore this row.  Don't increment i so that we overwrite it
				}
			}
			s = s + "</table>";
			long avgT0 = t0Sum/i;
			Date T0DT = new Date(avgT0*1000);
			s = s + "<br>";
			s = s + "T0 Est: " + T0DT + "<br>";
			s = s + "T0: " + avgT0 * 1000 + "<br>";
			
			return s;
		} catch (SQLException e) {
			PayloadDbStore.errorPrint(e);
			return e.toString();
		}
		
	}
}
