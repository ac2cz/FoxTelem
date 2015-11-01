package telemServer;

import gui.DisplayModule;
import common.Config;
import common.Log;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRtValues;
import telemetry.PayloadStore;

public class WebHealthTab {
	Spacecraft fox;
	PayloadRtValues payloadRt;
	PayloadMaxValues payloadMax;
	PayloadMinValues payloadMin;

	BitArrayLayout rtlayout;
	BitArrayLayout maxlayout;
	BitArrayLayout minlayout;

	String[] topModuleNames = new String[10];
	int[] topModuleLines = new int[10];
	String[] bottomModuleNames = new String[10];
	int[] bottomModuleLines = new int[10];
	int numOfTopModules = 1;
	int numOfBottomModules = 1;

	public WebHealthTab(Spacecraft f) throws LayoutLoadException {
		fox = f;
		rtlayout = fox.rtLayout;
		maxlayout = fox.maxLayout;
		minlayout = fox.minLayout;
		analyzeModules(fox.rtLayout, fox.maxLayout, fox.minLayout, 0);
	}
	
	public void setRtPayload(PayloadRtValues rt) {payloadRt = rt;}
	public void setMaxPayload(PayloadMaxValues max) {payloadMax = max;}
	public void setMinPayload(PayloadMinValues min) {payloadMin = min;}
	
	public String toGraphString(String fieldName) {
		String s = "";
		s = s + "<h3>Fox "+ fox.getIdString()+" - " + fieldName +"</h3>"
				+ "<table><tr><td>Reset</td> <td>Uptime </td> <td>" + fieldName + "</td> </tr>";
		double[][] graphData = Config.payloadStore.getRtGraphData(fieldName, 100, fox, 0, 0);
		if (graphData != null) {
			for (int i=0; i< graphData[0].length; i++) {
				s = s + "<tr>";
				s = s + "<td>"+graphData[PayloadStore.RESETS_COL][i] + "</td>" +
						"<td>"+graphData[PayloadStore.UPTIME_COL][i] + "</td>" +
						"<td>"+graphData[PayloadStore.DATA_COL][i] + "</td>";
				s = s + "</tr>";
			}
		}
		s = s + "</table>";
		return s;
	}
	
	public String toString() {
		String s = "";
		if (payloadRt != null) {
		s = s + "<h3>Fox "+ fox.getIdString()+"  REAL TIME Telemetry   Reset: " + payloadRt.getResets() + " Uptime: " + payloadRt.getUptime() + "</h3>"
				+ "<table>";
		
		s = s + "<tr bgcolor=silver>";

		// FIXME - These headers span the name, rt, max and min
		for (int i=1; i < numOfTopModules; i++) {
			s = s + "<td><h3>" + topModuleNames[i] + "</h3>"
			+ "</td>";
		}
		
		s = s + "</tr><tr>";
		for (int i=1; i < numOfTopModules; i++) {
			s = s + "<td>";
			// FIXME - FORMAT TO TOP.
			try {
				s = s + addModuleLines(topModuleNames[i], topModuleLines[i], rtlayout);
			} catch (LayoutLoadException e) {
				e.printStackTrace(Log.getWriter());
			}
			s = s + "</td>";
		}
		s = s + "</tr>";

		s = s + "<tr bgcolor=silver>";

		// FIXME - These headers span the name, rt, max and min
		for (int i=1; i < numOfBottomModules; i++) {
			s = s + "<td><h3>" + bottomModuleNames[i] + "</h3>"
			+ "</td>";
		}
		
		s = s + "</tr><tr>";
		for (int i=1; i < numOfBottomModules; i++) {
			s = s + "<td>";
			try {
				s = s + addModuleLines(bottomModuleNames[i], bottomModuleLines[i], rtlayout);
			} catch (LayoutLoadException e) {
				e.printStackTrace(Log.getWriter());
			}
			s = s + "</td>";
		}
		s = s + "</tr>";

		
		s= s+ "</table>";
		}
		return s;
	}
	
	
	private String addModuleLines(String topModuleName, int topModuleLine, BitArrayLayout rt) throws LayoutLoadException {
		String s = "";
		for (int j=0; j<rt.NUMBER_OF_FIELDS; j++) {
			if (rt.module[j].equals(topModuleName)) {
				//Log.println("Adding:" + rt.shortName[j]);
				if (rt.moduleLinePosition[j] > topModuleLine) throw new LayoutLoadException("Found error in Layout File: "+ rt.fileName +
				".\nModule: " + topModuleName +
						" has " + topModuleLine + " lines, so we can not add " + rt.shortName[j] + " on line " + rt.moduleLinePosition[j]);
				//FIXME - PUT NAME, RT, MIN, MAX in seperate columns
				//FIXME use rt.moduleDisplayType[j] to determine if it is one values that spans across them - like antenna
				//FIXME - make each value clickable - underline the name is best.  That will open the table for diagnostics
				s = s + "<a href=http://localhost:8080/1A/" + rt.fieldName[j] + ">" + rt.shortName[j] + "</a>" + formatUnits(rt.fieldUnits[j]) + ": " + payloadRt.getStringValue(rt.fieldName[j], fox)  + "<br>"; 
				//displayModule.addName(rt.moduleLinePosition[j], rt.shortName[j] + formatUnits(rt.fieldUnits[j]), rt.fieldName[j], rt.description[j], );					
			}
		}
		return s;

	}
	
	private String formatUnits(String unit) {
		if (unit.equals("-") || unit.equalsIgnoreCase(BitArrayLayout.NONE)) return "";
		unit = " ("+unit+")";
		return unit;
				
	}
	//FIXME - This is copied from ModuleTab and should be in a shared place
	protected void analyzeModules(BitArrayLayout rt, BitArrayLayout max, BitArrayLayout min, int moduleType) throws LayoutLoadException {
		// First get a quick list of all the modules names and sort them into top/bottom
		for (int i=0; i<rt.NUMBER_OF_FIELDS; i++) {
			if (!rt.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (rt.moduleNum[i] > 0 && rt.moduleNum[i] < 10) {
					if (!containedIn(topModuleNames, rt.module[i])) {
						topModuleNames[rt.moduleNum[i]] = rt.module[i];
						numOfTopModules++;
					}
					topModuleLines[rt.moduleNum[i]]++;
				} else if (rt.moduleNum[i] >= 10 && rt.moduleNum[i] < 20) {
					if (!containedIn(bottomModuleNames,rt.module[i])) {
						bottomModuleNames[rt.moduleNum[i]-9] = rt.module[i];
						numOfBottomModules++;
					}
					bottomModuleLines[rt.moduleNum[i]-9]++;		
				}
			}
		}
		if (max != null)
		for (int i=0; i<max.NUMBER_OF_FIELDS; i++) {
			if (!max.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (max.moduleNum[i] > 0 && max.moduleNum[i] < 10) {
					if (!containedIn(topModuleNames, max.module[i])) {
						topModuleNames[max.moduleNum[i]] = max.module[i];
						numOfTopModules++;
					}
					topModuleLines[max.moduleNum[i]]++;
				} else if (max.moduleNum[i] >= 10 && max.moduleNum[i] < 20) {
					if (!containedIn(bottomModuleNames,max.module[i])) {
						bottomModuleNames[max.moduleNum[i]-9] = max.module[i];
						numOfBottomModules++;
					}
					bottomModuleLines[max.moduleNum[i]-9]++;		
				}
			}
		}
		if (min != null)
		for (int i=0; i<min.NUMBER_OF_FIELDS; i++) {
			if (!min.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (min.moduleNum[i] > 0 && min.moduleNum[i] < 10) {
					if (!containedIn(topModuleNames, min.module[i])) {
						topModuleNames[min.moduleNum[i]] = min.module[i];
						numOfTopModules++;
					}
					topModuleLines[min.moduleNum[i]]++;
				} else if (min.moduleNum[i] >= 10 && min.moduleNum[i] < 20) {
					if (!containedIn(bottomModuleNames,min.module[i])) {
						bottomModuleNames[min.moduleNum[i]-9] = min.module[i];
						numOfBottomModules++;
					}
					bottomModuleLines[min.moduleNum[i]-9]++;		
				}
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
