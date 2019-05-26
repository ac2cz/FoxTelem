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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import telemetry.Frame;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.PayloadDbStore;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRtValues;
import uk.me.g4dpz.satellite.SatPos;
import common.Config;
import common.FoxSpacecraft;
import common.FoxTime;
import common.Log;

public class WebServiceProcess implements Runnable {
	PayloadDbStore payloadDbStore;
	public static String version = "Version 1.01 - 26 May 2019";
	private Socket socket = null;
	int port = 8080;
	
	public static final String VERSION = "version";
	public static final String TIME = "getSatUtcAtResetUptime";
	public static final String POSITION = "getSatLatLonAtResetUptime";
	
	public WebServiceProcess(PayloadDbStore db, Socket socket, int p) {
		this.socket = socket;
		port = p;
		payloadDbStore = db;
	}


	@Override
	public void run() {
		BufferedReader in = null;
		PrintWriter out = null;
		String GET = null;
		try {
			Log.println("Started Thread to handle connection from: " + socket.getInetAddress());

			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());

			// read the data sent. 
			// stop reading once a blank line is hit. This
			// blank line signals the end of the client HTTP headers.
			String str = ".";
			GET = in.readLine();
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

				//String path = request.substring(1, request.length());

				WebHealthTab fox1Atab = null;


				/*
				 * The FoxService Command API is in the format:
				 * COMMAND/SAT/OPTIONS
				 * 
				 * The following commands are valid:
				 * /VERSION - return the version
				 * /getSatLatLonAtResetUptime=sat&reset&uptime
				 * /getSatUtcForResetUptime=sat&reset&uptime
				 * 
				 * LEGACY APIs:
				 * /T0/SAT/RESET/CALL/N - Returns N T0 records for RESET logged by CALL for sat SAT
				 * /FRAME/SAT/TYPE - Return the latest frame of type TYPE for SAT
				 * /FIELD/SAT/NAME/R|C/N/RESET/UPTIME - Return N R-RAW or C-CONVERTED values for field NAME from sat SAT from RESET and UPTIME
				 * 
				 */
				String[] path = request.split("/");
				
				if (path.length == 0) {
					out.println("<H2>AMSAT FOX WEB SERVICE</H2>");
				} else { 
					String[] params = path[1].split("\\?");

					switch (params[0]) {
					case VERSION:
						out.println("Fox Web Service..." + version);
						return;
					case POSITION:
						if (params.length == 2) {
							getSatLatLonAtResetUptime(params[1], out);
							return;	
						}
					case TIME:
						if (params.length == 2) {
							getSatUtcForResetUptime(params[1], out);
							return;	
						}
					default:
						//out.println("Amsat API Malformed Request");  // put this message back in if NO LEGACY APIs
						break;
					}
					
					
					// LEGACY APIs in old format
					if (path[1].equalsIgnoreCase("T0")) { // T0 COMMAND
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
							PayloadRtValues rt = null;
							int sat = 1;
							@SuppressWarnings("unused")
							int type = 1;
							try {
								sat = Integer.parseInt(path[2]);
								type = Integer.parseInt(path[3]);
								rt = (PayloadRtValues) payloadDbStore.getLatestRt(sat);
							} catch (NumberFormatException e) {
								out.println("Invalid sat or type");
							}
							PayloadMaxValues max = (PayloadMaxValues) payloadDbStore.getLatestMax(sat);
							PayloadMinValues min = (PayloadMinValues) payloadDbStore.getLatestMin(sat);
							if (rt != null) {								
								try {
									fox1Atab = new WebHealthTab(payloadDbStore, (FoxSpacecraft) Config.satManager.getSpacecraft(sat),port);
								} catch (LayoutLoadException e1) {
									e1.printStackTrace(Log.getWriter());
								}
								//out.println("<H2>Fox-1 Telemetry</H2>");
								fox1Atab.setRtPayload(rt);
								fox1Atab.setMaxPayload(max);
								fox1Atab.setMinPayload(min);
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
							String name = path[3];
							String raw = path[4];
							boolean convert = true;
							int sat = 0;
							int num = 0;
							int fromReset = 0;
							int fromUptime = 0;
							try {
								sat = Integer.parseInt(path[2]);
								num = Integer.parseInt(path[5]);
								fromReset = Integer.parseInt(path[6]);
								fromUptime = Integer.parseInt(path[7]);
							} catch (NumberFormatException e) {
								out.println("Invalid sat or type");
							}
							if (sat > 0) {
								try {
									fox1Atab = new WebHealthTab(payloadDbStore,(FoxSpacecraft) Config.satManager.getSpacecraft(sat),port);
								} catch (LayoutLoadException e1) {
									e1.printStackTrace(Log.getWriter());
								}
								if (raw.startsWith("C"))
									convert = false;

								if (fox1Atab != null)
									out.println(fox1Atab.toGraphString(name, convert, num, fromReset, fromUptime));

							} else {
								out.println("FOX SAT Requested invalid\n");
							}
						} else {
							out.println("FOX FIELD Request invalid\n");
						}
					}
				}
			}

			out.flush();
		} catch (IOException e) {
			Log.println("ERROR: IO Exception in Webservice" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} finally {
			try { out.close(); } catch (Exception e) {e.printStackTrace(Log.getWriter());}
			try { in.close();  } catch (Exception e) {e.printStackTrace(Log.getWriter());}
			try { socket.close();  } catch (Exception e) {e.printStackTrace(Log.getWriter());}
			try { payloadDbStore.closeConnection(); } catch (Exception e) {e.printStackTrace(Log.getWriter());}
			Log.println("Finished Request: " + GET);
		}
	}

	public static final String SAT = "sat";
	public static final String RESET = "reset";
	public static final String UPTIME = "uptime";

	HashMap consumeArgs(String in) {
		HashMap args = new HashMap();
		String[] params = in.split("&");
		for (String p : params) {
			String[] keyVal = p.split("=");
			if (keyVal.length > 1)
				args.put(keyVal[0], keyVal[1]);
		}
		return args;
	}
	
	/**
	 * Calculate and return the position
	 * amsat.org/getPositionAtResetUptime&sat&reset&uptime
	 * @param path
	 */
	private void getSatLatLonAtResetUptime(String args, PrintWriter out) {
		DecimalFormat d = new DecimalFormat("00.000000");
		HashMap params = consumeArgs(args);
		String satId = (String) params.get(SAT);
		String satReset = (String) params.get(RESET);
		String satUptime = (String) params.get(UPTIME);
		if (satId == null) return;
		if (satReset == null) return;
		if (satUptime == null) return;

		try {
			FoxSpacecraft fox = (FoxSpacecraft) Config.satManager.getSpacecraft(Integer.parseInt(satId));
			int reset = Integer.parseInt(satReset);
			long uptime = Long.parseLong(satUptime);
			if (fox != null) {
				SatPos pos = fox.getSatellitePosition(reset, uptime);
				double satLatitude = FramePart.latRadToDeg (pos.getLatitude());
				double satLongitude = FramePart.lonRadToDeg(pos.getLongitude());
				out.println(d.format (satLatitude) + "," + d.format (satLongitude));
			} else out.println("0, 0");
		} catch (Exception e) {
			out.println("AMSAT API Request is invalid id + " + satId + " reset " + satReset + " uptime " +satUptime+"\n");								
		}

	}
	
	private void getSatUtcForResetUptime(String args, PrintWriter out) {
		Frame.stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		HashMap params = consumeArgs(args);
		String satId = (String) params.get(SAT);
		String satReset = (String) params.get(RESET);
		String satUptime = (String) params.get(UPTIME);
		if (satId == null) return;
		if (satReset == null) return;
		if (satUptime == null) return;
		
		try {
			FoxSpacecraft fox = (FoxSpacecraft) Config.satManager.getSpacecraft(Integer.parseInt(satId));
			int reset = Integer.parseInt(satReset);
			long uptime = Long.parseLong(satUptime);
			if (fox != null) {
				Date t = fox.getUtcForReset(reset, uptime);
				out.println(Frame.stpDateFormat.format(t));
			} else out.println("0, 0" + "\n");
		} catch (Exception e) {
			out.println("AMSAT API Request is invalid id + " + satId + " reset " + satReset + " uptime " +satUptime+"\n");										
		}

	}
	
	String calculateSpacecraftTime(FoxSpacecraft fox) {
		Date currentDate = new Date();
		FoxTime foxTime = fox.getUptimeForUtcDate(currentDate);
		
		return foxTime.getReset() + ", " + foxTime.getUptime();
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
		
		Connection derby = null;
		ResultSet r = null;
		try {
			derby = payloadDbStore.getConnection();
			stmt = derby.createStatement();
			//Log.println(update);
			r = stmt.executeQuery(update);
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
			PayloadDbStore.errorPrint("calculateT0", e);
			return e.toString();
		} finally {
			try { r.close(); } catch (SQLException e) {e.printStackTrace(Log.getWriter());}
			try { stmt.close(); } catch (SQLException e) {e.printStackTrace(Log.getWriter());}
		}
	}
}
