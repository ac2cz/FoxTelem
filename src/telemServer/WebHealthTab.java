package telemServer;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.tabs.DisplayModule;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.PayloadDbStore;
import telemetry.PayloadStore;
import telemetry.SortedFramePartArrayList;
import telemetry.legacyPayloads.PayloadRadExpData;
import telemetry.payloads.PayloadMaxValues;
import telemetry.payloads.PayloadMinValues;
import telemetry.payloads.PayloadRtValues;

@SuppressWarnings("deprecation")
public class WebHealthTab {
	Spacecraft fox;
	PayloadDbStore payloadDbStore;
	FramePart payloadRt;
	PayloadMaxValues payloadMax;
	PayloadMinValues payloadMin;

	BitArrayLayout rtlayout;
	BitArrayLayout maxlayout;
	BitArrayLayout minlayout;

	String[] topModuleNames = new String[20];
	int[] topModuleLines = new int[20];
	String[] bottomModuleNames = new String[10];
	int[] bottomModuleLines = new int[10];
	int numOfTopModules = 1;
	int numOfBottomModules = 1;
	int port = 8080; // port to pass onto further calls

	public WebHealthTab(PayloadDbStore pdb, Spacecraft f, int p, String layout) throws LayoutLoadException {
		fox = f;
		if (fox == null) throw new LayoutLoadException("Spacecraft is not valid");
		port = p;
		payloadDbStore = pdb;
		rtlayout = fox.getLayoutByName(layout);
		if (fox.hasFOXDB_V3) {
			maxlayout = fox.getLayoutByName(fox.getLayoutNameByType(BitArrayLayout.MAX));
			minlayout = fox.getLayoutByName(fox.getLayoutNameByType(BitArrayLayout.MIN));			
		} else {
			maxlayout = fox.getLayoutByName(Spacecraft.MAX_LAYOUT);
			minlayout = fox.getLayoutByName(Spacecraft.MIN_LAYOUT);
		}
		analyzeModules(rtlayout, maxlayout,minlayout, 0);
	}
	
	public void setRtPayload(FramePart rt) {payloadRt = rt;}
	public void setMaxPayload(PayloadMaxValues max) {payloadMax = max;}
	public void setMinPayload(PayloadMinValues min) {payloadMin = min;}
	
	public String toCsvString(String fieldName, boolean convert, int num, int fromReset, int fromUptime) {
		String s = "";
		//s = s + "<style> td { border: 5px } th { background-color: lightgray; border: 3px solid lightgray; } td { padding: 5px; vertical-align: top; background-color: darkgray } </style>";	
		//s = s + "<h3>Fox "+ fox.getIdString()+" - " + fieldName +"</h3>"
		//		+ "<table><tr><th>Reset</th> <th>Uptime </th> <th>" + fieldName + "</th> </tr>";
		double[][] graphData = payloadDbStore.getRtGraphData(fieldName, num, fox, fromReset, fromUptime, false, true);
		if (graphData != null) {
			for (int i=0; i< graphData[0].length; i++) {
			//	s = s + "<tr>";
				s = s + (int)graphData[PayloadStore.RESETS_COL][i] + "," +
						(int)graphData[PayloadStore.UPTIME_COL][i] + "," +
						graphData[PayloadStore.DATA_COL][i] + "/n";
				
			}
		}
		s = s + "</table>";
		return s;
	}
	
	public String toGraphString(String fieldName, boolean convert, int num, int fromReset, int fromUptime) {
		String s = "";
		s = s + "<style> td { border: 5px } th { background-color: lightgray; border: 3px solid lightgray; } td { padding: 5px; vertical-align: top; background-color: darkgray } </style>";	
		s = s + "<h1 class='entry-title'>Fox "+ fox.getIdString()+" - " + fieldName +"</h1>"
				+ "<table><tr><th>Reset</th> <th>Uptime </th> <th>" + fieldName + "</th> </tr>";
		
		double[][] graphData = null;

		if (rtlayout.hasFieldName(fieldName))
			graphData = payloadDbStore.getRtGraphData(fieldName, num, fox, fromReset, fromUptime, false, true);
		else if (minlayout.hasFieldName(fieldName))
			graphData = payloadDbStore.getMinGraphData(fieldName, num, fox, fromReset, fromUptime, false, true);
		else if (maxlayout.hasFieldName(fieldName))
			graphData = payloadDbStore.getMaxGraphData(fieldName, num, fox, fromReset, fromUptime, false, true);
		
		if (graphData != null) {
			for (int i=0; i< graphData[0].length; i++) {
				s = s + "<tr>";
				s = s + "<td>"+(int)graphData[PayloadStore.RESETS_COL][i] + "</td>" +
						"<td>"+(int)graphData[PayloadStore.UPTIME_COL][i] + "</td>" +
						"<td>"+graphData[PayloadStore.DATA_COL][i] + "</td>";
				s = s + "</tr>";
			}
		}
		s = s + "</table>";
		return s;
	}
	
	public String toGraphString(String fieldName, boolean convert, int num, int fromReset, int fromUptime, String layout) {
		String s = "";
		s = s + "<style> td { border: 5px } th { background-color: lightgray; border: 3px solid lightgray; } td { padding: 5px; vertical-align: top; background-color: darkgray } </style>";	
		s = s + "<h1 class='entry-title'>Fox "+ fox.getIdString()+" - " + fieldName +"</h1>"
				+ "<table><tr><th>Reset</th> <th>Uptime </th> <th>" + fieldName + "</th> </tr>";
		
		double[][] graphData = payloadDbStore.getGraphData(fieldName, num, fox, fromReset, fromUptime, false, true, layout);
		
		
		if (graphData != null) {
			for (int i=0; i< graphData[0].length; i++) {
				s = s + "<tr>";
				s = s + "<td>"+(int)graphData[PayloadStore.RESETS_COL][i] + "</td>" +
						"<td>"+(int)graphData[PayloadStore.UPTIME_COL][i] + "</td>" +
						"<td>"+graphData[PayloadStore.DATA_COL][i] + "</td>";
				s = s + "</tr>";
			}
		}
		s = s + "</table>";
		return s;
	}
	
	public String toString() {
		String s = "";
		String mode = "UNKNOWN";
		if (fox.hasModeInHeader) { // Post Fox-1E BPSK has mode in header
			mode = determineModeFromHeader();
		} else {
		
		PayloadRadExpData radPayload = payloadDbStore.getLatestRad(fox.foxId);
			mode = Spacecraft.determineModeString(fox, (PayloadRtValues)payloadRt, (PayloadMaxValues)payloadMax, (PayloadMinValues)payloadMin, radPayload);
		}
		if (payloadRt != null) {
			
			s = s + "<h1 class='entry-title'>Fox "+ fox.getIdString()+"</h1>";
			// We set the style of table 1 for the telemetry.  Table 2 is the inner table for the max/min/rt rows
		s = s + "<style> table.table1 td { border: 5px solid lightgray; } "
				+ "table.table1 th { background-color: lightgray; border: 3px solid lightgray; } "
				+ "table.table1 td { padding: 5px; vertical-align: top; background-color: darkgray } </style>";	
		s = s + "<style> table.table2 td { border: 0px solid darkgray; } "
				+ "table.table2 th { background-color: darkgray; border: 1px solid darkgray; } "
				+ "table.table2 td { padding: 0px; vertical-align: top; background-color: darkgray } </style>";	

		boolean realtime = false;
		boolean wod = false;
		if (fox.hasFOXDB_V3) {
			if (payloadRt.getLayout().isRealTime())
				realtime = true;
			if (payloadRt.getLayout().isWOD())
				wod = true;
		} else {
			String layoutName = payloadRt.getLayout().name;
			if (layoutName.equalsIgnoreCase(Spacecraft.REAL_TIME_LAYOUT))
				realtime = true;
			if (layoutName.equalsIgnoreCase(Spacecraft.WOD_LAYOUT))
				wod=true;
		}
		if (realtime)
			s = s + "<h3>REAL TIME Telemetry   Reset: " + payloadRt.getResets() + " Uptime: " + payloadRt.getUptime();				
		else if (wod)
			s = s + "<h3>Whole Orbit Telemetry   Reset: " + payloadRt.getResets() + " Uptime: " + payloadRt.getUptime();
		else
			s = s + "<h3>Telemetry   Reset: " + payloadRt.getResets() + " Uptime: " + payloadRt.getUptime();
		s = s + " Mode: " + mode + "<br>";
		s = s + "</h3>";
		if (payloadRt.getCaptureDate() != null)
			s = s + " Received: " + formatCaptureDate(payloadRt.getCaptureDate()) + "<br>";
		
		if (payloadMax != null)
			s = s + "Last MAX Reset: " + payloadMax.getResets() + " Uptime: " + payloadMax.getUptime() + ".";
		if (payloadMin != null)
			s = s + " Last MIN Reset: " + payloadMin.getResets() + " Uptime: " + payloadMin.getUptime() + ".</br><p>";
		
		
		s = s + "<table class='table1'>";
		
		s = s + "<tr bgcolor=silver>";
		for (int i=1; i < 4; i++) {
			s = s + "<th><strong>" + topModuleNames[i] + "<strong>"
			+ "</th>";
		}
		s = s + "</tr>";
		s = s + "<tr>";
		for (int i=1; i < 4; i++) {
			s = s + buildModule(i);		
		}
		s = s + "</tr>";
		s = s + "<tr bgcolor=silver>";
		for (int i=4; i < 6; i++) {
			s = s + "<th><strong>" + topModuleNames[i] + "</strong>"
			+ "</th>";
		}
		s = s + "</tr><tr>";
		for (int i=4; i < 6; i++) {	
			s = s + buildModule(i);	
		}
		s = s + "</tr>";
		s = s + "<tr bgcolor=silver>";
		for (int i=6; i < 9; i++) {
			s = s + "<th><strong>" + topModuleNames[i] + "</strong>"
			+ "</th>";
		}
		s = s + "</tr><tr>";
		for (int i=6; i < 9; i++) {
			s = s + buildModule(i);	
		}
		s = s + "</tr>";
		s = s + "<tr bgcolor=silver>";
		for (int i=9; i < 12; i++) {
			s = s + "<th><strong>" + topModuleNames[i] + "</strong>"
			+ "</th>";
		}
		s = s + "</tr><tr>";
		for (int i=9; i < 12; i++) {
			s = s + buildModule(i);	
		}
		s = s + "</tr>";
		s= s+ "</table>";
		}
		return s;
	}
	
	/**
	 * Local copy of this routine that does not use Config.payloadstore
	 * @return
	 */
	public String determineModeFromHeader() {
		// Mode is stored in the header
		// Find the most recent frame and return the mode that it has
		SortedFramePartArrayList payloads = new SortedFramePartArrayList(fox.numberOfLayouts);
		int maxLayouts = 4; // First four layouts are rt, max, min, exp, but we may have mode in any layout.  Cap at 10.
		for (int i=0; i <= maxLayouts && i < fox.layout.length; i++) { 
			FramePart part = payloadDbStore.getLatest(fox.foxId, fox.layout[i].name);
			if (part != null)
				payloads.add(part);
			payloads.add(part);
		}

		int mode = Spacecraft.NO_MODE;
		if (payloads.size() > 0)
			mode = payloads.get(payloads.size()-1).newMode;
		return Spacecraft.getModeString(mode);
	}
	
	private String buildModule(int i) {
		String s = "";
		s = s + "<td><table class='table2'>";
		s = s + "<tr><th></th><th>RT</th><th>MIN</th><th>MAX</th></tr>";
		try {
			s = s + addModuleLines(topModuleNames[i], topModuleLines[i], rtlayout, payloadRt);
		    if (maxlayout != null && payloadMax!=null ) 
		    	s = s + addModuleLines(topModuleNames[i], topModuleLines[i], maxlayout, payloadMax);
		    if (minlayout != null && payloadMin != null) 
		    	s = s + addModuleLines(topModuleNames[i], topModuleLines[i], minlayout, payloadMin);
		} catch (LayoutLoadException e) {
			e.printStackTrace(Log.getWriter());
		}

		s = s + "</table></td>";
		return s;
	}
	
	private String addModuleLines(String topModuleName, int topModuleLine, BitArrayLayout rt, FramePart payloadRt) throws LayoutLoadException {
		String layoutName = payloadRt.getLayout().name;
		
		String s = "";
		for (int j=0; j<rt.NUMBER_OF_FIELDS; j++) {
			if (rt.module[j].equals(topModuleName)) {
				//Log.println("Adding:" + rt.shortName[j]);
				if (rt.moduleLinePosition[j] > topModuleLine) throw new LayoutLoadException("Found error in Layout File: "+ rt.fileName +
						".\nModule: " + topModuleName +
						" has " + topModuleLine + " lines, so we can not add " + rt.shortName[j] + " on line " + rt.moduleLinePosition[j]);
				
				if (!rt.fieldName[j].startsWith("IHUDiag")) {
				s = s + "<tr>";
				
				s= s + "<td>";
				
				boolean wod=false;
				if (fox.hasFOXDB_V3) {
					if (payloadRt.getLayout().isWOD())
						wod=true;
				} else {
					if (layoutName.equalsIgnoreCase(Spacecraft.WOD_LAYOUT))
						wod=true;
				}
				if (wod) {
					s = s + "<a href=/tlm/wodGraph.php?"
							+ "sat=" 
							+ fox.foxId;
					s = s	+"&wod-field="; 
				}
				else {
				s = s + "<a href=/tlm/graph.php?"
						+ "sat=" 
						+ fox.foxId;
					s = s	+"&field=";				
				}  
				s = s + rt.fieldName[j]
								+ "&raw=conv"  
								+ "&reset=0"
								+ "&uptime=0"
								+ "&rows=100"
								+ "&port=" + port
								+ ">" + rt.shortName[j] + "</a>";
				s = s + formatUnits(rt.fieldUnits[j]) + "</td>";

				if (rt.moduleDisplayType[j] == DisplayModule.DISPLAY_RT_ONLY)
					s= s + "<td align=left colspan=3>";
				else
					s= s + "<td align=center > ";
				
				s = s + payloadRt.getStringValue(rt.fieldName[j], fox)  + "</td>"; 

				
				// Min
				if (rt.moduleDisplayType[j] != DisplayModule.DISPLAY_RT_ONLY) {
					s = s + "<td align=center >";
					if (rt.moduleDisplayType[j] == DisplayModule.DISPLAY_ALL_SWAP_MINMAX) {
						if (payloadMax != null)
							s = s + payloadMax.getStringValue(rt.fieldName[j], fox);
					} else if (payloadMin != null)
						s = s + payloadMin.getStringValue(rt.fieldName[j], fox);
				}
				
				// Max
				if (rt.moduleDisplayType[j] != DisplayModule.DISPLAY_RT_ONLY) {
					s = s + "</td><td align=center >";
					if (rt.moduleDisplayType[j] == DisplayModule.DISPLAY_ALL_SWAP_MINMAX) {
						if (payloadMin != null)
							s = s + payloadMin.getStringValue(rt.fieldName[j], fox);
					} else if (payloadMax != null)
						s = s + payloadMax.getStringValue(rt.fieldName[j], fox);
				}
				s = s + "</td></tr>";
				}
			}
		}
		
		return s;

	}
	
	private String formatUnits(String unit) {
		if (unit.equals("-") || unit.equalsIgnoreCase(BitArrayLayout.NONE)) return "";
		unit = " ("+unit+")";
		return unit;
				
	}
	
	private String formatCaptureDate(String u) {
		Date result = null;
		String reportDate = null;
		try {
			FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			result = FramePart.fileDateFormat.parse(u);	
			FramePart.reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			reportDate = FramePart.reportDateFormat.format(result);

		} catch (ParseException e) {
			reportDate = "unknown";				
		} catch (NumberFormatException e) {
			reportDate = "unknown";				
		} catch (Exception e) {
			reportDate = "unknown";				
		}

		return reportDate;
	}

	//FIXME - This is copied from ModuleTab and should be in a shared place
	protected void analyzeModules(BitArrayLayout rt, BitArrayLayout max, BitArrayLayout min, int moduleType) throws LayoutLoadException {
		// First get a quick list of all the modules names and sort them into top/bottom
		for (int i=0; i<rt.NUMBER_OF_FIELDS; i++) {
			if (!rt.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (!containedIn(topModuleNames, rt.module[i])) {
					topModuleNames[rt.moduleNum[i]] = rt.module[i];
					numOfTopModules++;
				}
				topModuleLines[rt.moduleNum[i]]++;
			}
		}
		if (max != null)
			for (int i=0; i<max.NUMBER_OF_FIELDS; i++) {
				if (!max.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
					if (!containedIn(topModuleNames, max.module[i])) {
						topModuleNames[max.moduleNum[i]] = max.module[i];
						numOfTopModules++;
					}
					topModuleLines[max.moduleNum[i]]++;
				}
			}
		if (min != null)
			for (int i=0; i<min.NUMBER_OF_FIELDS; i++) {
				if (!min.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
					if (!containedIn(topModuleNames, min.module[i])) {
						topModuleNames[min.moduleNum[i]] = min.module[i];
						numOfTopModules++;
					}
					topModuleLines[min.moduleNum[i]]++;
				}

			}

	}
	
	private boolean containedIn(String[] array, String item) {
		for(String s : array) {
			if (s!=null)
				if (s.equals(item)) return true;
		}
		return false;
	}
}
