package gui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import common.Config;
import common.FoxSpacecraft;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
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
 *
 */
@SuppressWarnings("serial")
public class DiagnosticTable extends JPanel {
	FoxSpacecraft fox;
	double[][] graphData = null;
	String title = "Test Graph";
	String fieldName = null;
	int conversionType;
	GraphFrame graphFrame;
	JTable table;
	AbstractTableModel tableModel;
	JScrollPane scrollPane;
	
	
	DiagnosticTable(String t, String fieldName, int conversionType, GraphFrame gf, FoxSpacecraft sat) {
		this.conversionType = conversionType;
		title = t;
		this.fieldName = fieldName;
		graphFrame = gf;
		fox = sat;
		this.setLayout(new BorderLayout());
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(new Font("SansSerif", Font.BOLD, Config.graphAxisFontSize + 3));
		add(titleLabel, BorderLayout.NORTH);
		table = addErrorTable();
		updateData();
	}
	
	private JTable addErrorTable() {
		if (conversionType == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC) 
			tableModel = new DiagnosticTableModel();
		else if (conversionType == BitArrayLayout.CONVERT_HARD_ERROR)
			tableModel = new HardErrorTableModel();
		else if (conversionType == BitArrayLayout.CONVERT_SOFT_ERROR)
			tableModel = new SoftErrorTableModel();
		else if (conversionType == BitArrayLayout.CONVERT_SOFT_ERROR_84488)
			tableModel = new SoftError84488TableModel();
		else if (conversionType == BitArrayLayout.CONVERT_ICR_DIAGNOSTIC)
			tableModel = new IcrDiagnosticTableModel();
		else if (conversionType == BitArrayLayout.CONVERT_COM1_ISIS_ANT_STATUS)
			tableModel = new IsisAntennaStatusTableModel();
		
		JTable table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		
		scrollPane = new JScrollPane (table, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		add(scrollPane, BorderLayout.CENTER);
		//add(table, BorderLayout.CENTER);

		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( SwingConstants.CENTER );
		
		if (conversionType == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC) {
			column = table.getColumnModel().getColumn(2);
			column.setPreferredWidth(250);
		} else if (conversionType == BitArrayLayout.CONVERT_HARD_ERROR)
			for (int i=0; i<6; i++) {
				column = table.getColumnModel().getColumn(i+2);
				column.setPreferredWidth(60);
				column.setCellRenderer(centerRenderer);
				column.setHeaderRenderer(centerRenderer);
			}
		else if (conversionType == BitArrayLayout.CONVERT_SOFT_ERROR) {
			for (int i=0; i<4; i++) {
				column = table.getColumnModel().getColumn(i+2);
				column.setPreferredWidth(60);
				column.setCellRenderer(centerRenderer);
				column.setHeaderRenderer(centerRenderer);
			}
			if (conversionType == BitArrayLayout.CONVERT_SOFT_ERROR_84488) {
				column = table.getColumnModel().getColumn(6);
				column.setPreferredWidth(60);
				column.setCellRenderer(centerRenderer);
				column.setHeaderRenderer(centerRenderer);
			}
		}
		else if (conversionType == BitArrayLayout.CONVERT_ICR_DIAGNOSTIC) {
			for (int i=0; i<4; i++) {
				column = table.getColumnModel().getColumn(i+2);
				column.setPreferredWidth(60);
				column.setCellRenderer(centerRenderer);
				column.setHeaderRenderer(centerRenderer);
			}
		}
		else if (conversionType == BitArrayLayout.CONVERT_COM1_ISIS_ANT_STATUS) {
			for (int i=0; i<15; i++) {
				column = table.getColumnModel().getColumn(i+2);
				column.setPreferredWidth(60);
				column.setCellRenderer(centerRenderer);
				column.setHeaderRenderer(centerRenderer);
			}
		}

		
		return table;
	}
	
	public void updateData() {
	//	String display = null;

		if (conversionType == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC) 
			updateDiagnosticData();
		else if (conversionType == BitArrayLayout.CONVERT_HARD_ERROR)
			updateHardErrorData();
		else if (conversionType == BitArrayLayout.CONVERT_SOFT_ERROR)
			updateSoftErrorData();
		else if (conversionType == BitArrayLayout.CONVERT_SOFT_ERROR_84488)
			updateSoftErrorData84488();
		else if (conversionType == BitArrayLayout.CONVERT_ICR_DIAGNOSTIC)
			updateIcrDiagnosticData();
		else if (conversionType == BitArrayLayout.CONVERT_COM1_ISIS_ANT_STATUS)
			updateIsisAntennaData();

	}

	private void setColumnName(int col, String name) {
		int viewColumn = table.convertColumnIndexToView(col);
		TableColumn column = table.getColumnModel().getColumn(viewColumn);
		column.setHeaderValue(name);
		table.getTableHeader().repaint();

	}
	
	public void updateDiagnosticData() {
		boolean reverse = false;
		if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
			reverse=true;
		graphData = Config.payloadStore.getGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);
//		graphData = Config.payloadStore.getRtGraphData(fieldName, graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
		String[][] tableData = new String[graphData[0].length][7];
		
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String display = null;
				
				display = FoxFramePart.ihuDiagnosticString(value, false, fox);
				if (display != null) { 	
					initTableColumns(tableData, i);
					tableData[i][2] = display;
				}
			}
		
			((DiagnosticTableModel) tableModel).setData(tableData);
		}
		MainWindow.frame.repaint();	
			
	}

	public void updateHardErrorData() {
		boolean reverse = false;
		if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
			reverse=true;
		graphData = Config.payloadStore.getGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);
//		graphData = Config.payloadStore.getMaxGraphData(fieldName, graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
		String[][] tableData = new String[graphData[0].length][8];
		
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String[] display = null;
				display = FoxFramePart.hardErrorStringArray(value, false);
				
				if (display != null) { 	
					initTableColumns(tableData, i);
					for (int j=2; j<8; j++)
						tableData[i][j] = display[j-2];
				}
			}
		
			((HardErrorTableModel) tableModel).setData(tableData);
		}
		MainWindow.frame.repaint();	
	}

	public void updateSoftErrorData() {
		boolean reverse = false;
		if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
			reverse=true;
		graphData = Config.payloadStore.getGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);
		//graphData = Config.payloadStore.getMinGraphData(fieldName, graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
		String[][] tableData = new String[graphData[0].length][6];
		
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String[] display = null;
				display = FoxFramePart.softErrorStringArrayFox1A(value, false);
				
				if (display != null) { 	
					initTableColumns(tableData, i);
					for (int j=2; j<6; j++)
						tableData[i][j] = display[j-2];
				}
			}
		
			((SoftErrorTableModel) tableModel).setData(tableData);
		}
		MainWindow.frame.repaint();	
			
	}
	
	public void updateSoftErrorData84488() {
		boolean reverse = false;
		if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
			reverse=true;
		graphData = Config.payloadStore.getGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);
//		graphData = Config.payloadStore.getMinGraphData(fieldName, graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
		String[][] tableData = new String[graphData[0].length][7];
		
		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String[] display = null;
				display = FoxFramePart.softErrorStringArray84488(value, false);
				
				if (display != null) { 	
					initTableColumns(tableData, i);
					for (int j=2; j<7; j++)
						tableData[i][j] = display[j-2];
				}
			}
		
			((SoftError84488TableModel) tableModel).setData(tableData);
		}
		MainWindow.frame.repaint();	
			
	}

	public void updateIcrDiagnosticData() {
		boolean reverse = false;
		if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
			reverse=true;
		graphData = Config.payloadStore.getGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);
//		graphData = Config.payloadStore.getRtGraphData(fieldName, graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
		String[][] tableData = new String[graphData[0].length][7];

		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String[] display = null;

				display = FoxFramePart.icrDiagnosticStringArray(value, false);
				if (display != null) { 	
					initTableColumns(tableData, i);
					for (int j=2; j<6; j++)
						tableData[i][j] = display[j-2];
				}
			}

			((IcrDiagnosticTableModel) tableModel).setData(tableData);
		}
		MainWindow.frame.repaint();	

	}

	public void updateIsisAntennaData() {
		boolean reverse = false;
		if (graphFrame.showLatest == GraphFrame.SHOW_LIVE)
			reverse=true;
		graphData = Config.payloadStore.getGraphData(fieldName, graphFrame.SAMPLES, graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, graphFrame.layout.name, false, reverse);
		//		graphData = Config.payloadStore.getRtGraphData(fieldName, graphFrame.SAMPLES, (FoxSpacecraft)graphFrame.fox, graphFrame.START_RESET, graphFrame.START_UPTIME, false, reverse);
		String[][] tableData = new String[graphData[0].length][17];

		if (graphData[0].length > 0) {
			for (int i=graphData[0].length-1; i >=0 ; i--) {
				int value = (int) graphData[PayloadStore.DATA_COL][i];
				String[] display = null;

				display = FoxFramePart.isisAntennaStatusArray(value, false);
				if (display != null) { 	
					initTableColumns(tableData, i);
					for (int j=2; j<17; j++)
						tableData[i][j] = display[j-2];
				}
			}

			((IsisAntennaStatusTableModel) tableModel).setData(tableData);
		}
		MainWindow.frame.repaint();	

	}
	
	private void initTableColumns(String[][] tableData, int i) {
		if (graphFrame.showUTCtime) {
			setColumnName(0, "Date");
			setColumnName(1, "Time (UTC)");

			int resets = (int)graphData[PayloadStore.RESETS_COL][i];
			long uptime = (int)graphData[PayloadStore.UPTIME_COL][i];
			if (fox.hasTimeZero(resets)) {
				tableData[i][1] = fox.getUtcTimeForReset(resets, uptime);
				tableData[i][0] = fox.getUtcDateForReset(resets, uptime);
			} else {
				tableData[i][1] = "";
				tableData[i][0] = "";
			}
		} else {
			setColumnName(0, "Reset");
			setColumnName(1, "Uptime");
			tableData[i][0] = Integer.toString((int)graphData[PayloadStore.RESETS_COL][i]);
			tableData[i][1] = Long.toString((long)graphData[PayloadStore.UPTIME_COL][i]);
		}
	}

}
