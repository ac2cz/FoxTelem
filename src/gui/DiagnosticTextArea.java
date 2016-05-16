package gui;

import javax.swing.JTextArea;

import common.Config;
import telemetry.FramePart;
import telemetry.PayloadStore;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
@SuppressWarnings("serial")
public class DiagnosticTextArea extends JTextArea {
	double[][] graphData = null;
	String title = "Test Graph";
	String fieldName = null;
	GraphFrame graphFrame;
	
	DiagnosticTextArea(String t, String fieldName, GraphFrame gf) {
		title = t;
		this.fieldName = fieldName;
		graphFrame = gf;
		updateData();
		this.setEditable(false);
	}
	
	public void updateData() {
	//	String display = null;

		if (fieldName == "IHUDiagnosticData") 
			updateDiagnosticData();
		else if (fieldName == "IHUHardErrorData")
			updateHardErrorData();
		else if (fieldName == "IHUSoftErrorData")
			updateSoftErrorData();

	}

	public void updateDiagnosticData() {
		graphData = Config.payloadStore.getRtGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		this.setText(null);
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String display = null;
				
				display = FramePart.ihuDiagnosticString(value, false, graphFrame.fox);
				if (display != null) { 	
					this.append((int)graphData[PayloadStore.RESETS_COL][i] + " " + (int)graphData[PayloadStore.UPTIME_COL][i] + " " +
							display + "\n");
				}
			}
		}
			
	}

	public void updateHardErrorData() {
		graphData = Config.payloadStore.getMaxGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		this.setText(null);
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
			//for (int i=0; i < graphData[0].length; i++) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String display = null;
				display = FramePart.hardErrorString(value, false);
				
				if (display != null) { 	
					this.append((int)graphData[PayloadStore.RESETS_COL][i] + " " + (int)graphData[PayloadStore.UPTIME_COL][i] + " " +
							display + "\n");
				}
			}
		}
			
	}

	public void updateSoftErrorData() {
		graphData = Config.payloadStore.getMinGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME);
		this.setText(null);
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
			//for (int i=0; i < graphData[0].length; i++) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String display = null;
				display = FramePart.softErrorString(value, false);
				
				if (display != null) { 	
					this.append((int)graphData[PayloadStore.RESETS_COL][i] + " " + (int)graphData[PayloadStore.UPTIME_COL][i] + " " +
							display + "\n");
				}
			}
		}
			
	}

}
