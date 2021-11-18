package gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.payloads.CanPacket;
import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import gui.graph.GraphFrame;

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
 * This is a tab that displays experiement data.  This has two layouts.  Layout1 is the raw experiment data,
 * often just raw bytes.  Layout2 is the data parsed into fields and is shown at the top of the tab.
 *
 */
@SuppressWarnings("serial")
public class CanExperimentTab extends ExperimentTab implements ItemListener, Runnable, MouseListener {

	private static final String DECODED = "Payloads Decoded: ";
	public final int DEFAULT_DIVIDER_LOCATION = 350;

	JLabel lblName;
	private String NAME;
	JLabel lblFramesDecoded;

	//JCheckBox showRawBytes;
	ExperimentLayoutTableModel expTableModel;
	ExperimentLayoutTableModel expTableModel2; // translated values put in layout2

	CanPacketRawTableModel canPacketRawTableModel;
	
	JTable canPacketRawTable;
	
	JPanel healthPanel;

	boolean displayTelem = true;

	BitArrayLayout layout; // raw values
	BitArrayLayout layout2; // translated format
	
	BitArrayLayout canPktLayout; // translated format

	public CanExperimentTab(Spacecraft sat, String displayName, BitArrayLayout displayLayout, BitArrayLayout canPktLayout, int displayType)  {
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " " + displayName;
		this.canPktLayout = canPktLayout;
		
		//int j = 0;
		this.layout = displayLayout;
		this.layout2 = layout;

		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, layout.name, "splitPaneHeight");

		lblName = new JLabel(NAME);
		lblName.setMaximumSize(new Dimension(1600, 20));
		lblName.setMinimumSize(new Dimension(1600, 20));
		lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
		topPanel.add(lblName);

		lblFramesDecoded = new JLabel(DECODED);
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblFramesDecoded.setBorder(new EmptyBorder(5, 2, 5, 5) );
		topPanel.add(lblFramesDecoded);

		healthPanel = new JPanel();

		healthPanel.setLayout(new BoxLayout(healthPanel, BoxLayout.X_AXIS));
		healthPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		healthPanel.setBackground(Color.DARK_GRAY);

		initDisplayHalves(healthPanel);

		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));

		BitArrayLayout none = null;

		try {
			analyzeModules(layout2, none, none, displayType);
		} catch (LayoutLoadException e) {
			Log.errorDialog("FATAL - Load Aborted", e.getMessage());
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		}

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				healthPanel, centerPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		if (splitPaneHeight != 0) 
			splitPane.setDividerLocation(splitPaneHeight);
		else
			splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);

		SplitPaneUI spui = splitPane.getUI();
		if (spui instanceof BasicSplitPaneUI) {
			// Setting a mouse listener directly on split pane does not work, because no events are being received.
			((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					splitPaneHeight = splitPane.getDividerLocation();
					//Log.println("SplitPane: " + splitPaneHeight);
					Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, layout.name, "splitPaneHeight", splitPaneHeight);
				}
			});
		}
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);

		showRawBytes = new JCheckBox("Show Raw Bytes", Config.displayRawRadData);
		bottomPanel.add(showRawBytes );
		showRawBytes.addItemListener(this);

		addCanPacketTables();
		
		addBottomFilter();

		expTableModel = new ExperimentLayoutTableModel(layout);
		expTableModel2 = new ExperimentLayoutTableModel(layout2);
		addTables(expTableModel,expTableModel2);

		// initial populate
		parseExperimentFrames();
	}

	int total;
	protected void displayFramesDecoded(int u) {
		total = u;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				lblFramesDecoded.setText(DECODED + total);
				lblFramesDecoded.invalidate();
				topPanel.validate();
			}
		});
	
	}
	
	void addCanPacketTables() {
		canPacketRawTableModel = new CanPacketRawTableModel();
		canPacketRawTable = new JTable(canPacketRawTableModel);
		canPacketRawTable.setAutoCreateRowSorter(true);
		canPacketRawTable.addMouseListener(this);
		
		JScrollPane scrollPane = new JScrollPane (canPacketRawTable, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		canPacketRawTable.setFillsViewportHeight(true);
		canPacketRawTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		String PREV = "prev";
		String NEXT = "next";
		InputMap inMap = canPacketRawTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMap = canPacketRawTable.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//System.out.println("PREV CAN MSG");
				int row = canPacketRawTable.getSelectedRow();
				if (row > 0)
					displayRow(canPacketRawTable,NO_ROW_SELECTED, row-1);
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//System.out.println("NEXT CAN MSG");
				int row = canPacketRawTable.getSelectedRow();
				if (row < canPacketRawTable.getRowCount()-1)
					displayRow(canPacketRawTable,NO_ROW_SELECTED, row+1);        
			}
		});
		//table.setMinimumSize(new Dimension(6200, 6000));
		healthPanel.add(scrollPane);
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );

		TableColumn column = null;
		column = canPacketRawTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(45); // reset
		
		column = canPacketRawTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(55); // uptime
		column = canPacketRawTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(55); // seq
		column = canPacketRawTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(55); // ID
		column = canPacketRawTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(55); // ID HEX
		
		for (int i=0; i<canPacketRawTable.getColumnCount()-5; i++) {
			column = canPacketRawTable.getColumnModel().getColumn(i+5);
			column.setPreferredWidth(30);
		}
	}


	protected void addTables(AbstractTableModel expTableModel, AbstractTableModel expTableModel2) {
		super.addTables(expTableModel, expTableModel2);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );

		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		
		for (int i=0; i<table.getColumnCount()-2; i++) {
			column = table.getColumnModel().getColumn(i+2);
			int w = layout.fieldName[i].length();
			column.setPreferredWidth(10+7*w);
		}
		
		column = table2.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table2.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		
		for (int i=0; i<table2.getColumnCount()-2; i++) {
			column = table2.getColumnModel().getColumn(i+2);
			int w = layout2.fieldName[i].length();
			column.setPreferredWidth(10+7*w);
		}
	}
	
	long[][] noKeys = {{0L,0L,0L}};
	String[][] noData = {{"","","","","","","","","","",""}};
	
	/**
	 * Show the can packets at the top of the window
	 * 
	 * @param reset
	 * @param uptime
	 */
	protected void parseCanPackets(String data[][], CanPacketRawTableModel tableModel, int resets, long uptime) {
		// Purge rows with the wrong reset uptime
		ArrayList<String[]> rows = new ArrayList<String[]>();
		for (int i=0; i<data.length; i++) {
			int r = Integer.parseInt(data[i][0]); // reset
			long u = Long.parseLong(data[i][1]); // uptime
			if (r == resets && u == uptime)
				rows.add(data[i]);
		}
		
		if (rows.size() == 0) {
			tableModel.setData(noKeys, noData);
			return;
		}
		
		long[][] keyRawData = new long[rows.size()][2];
		String[][] rawData = new String[rows.size()][data[0].length-1];  // returned data length is 3 + length of data record
		
		for (int i=0; i<rows.size(); i++) {
			try {
				keyRawData[i][0] = Long.parseLong(rows.get(i)[0]); // reset
				keyRawData[i][1] = Long.parseLong(rows.get(i)[1]); // uptime
				rawData[i][0] = rows.get(i)[2]; // seq
				int rawid = CanPacket.getRawIdFromRawBytes(Integer.valueOf(rows.get(i)[3]), Integer.valueOf(rows.get(i)[4]));
				int id = CanPacket.getIdfromRawID(rawid);
				rawData[i][1] = ""+id;
				rawData[i][2] = Long.toHexString(id);
				int len = CanPacket.getLengthfromRawID(rawid);
				rawData[i][3] = ""+len;

				for (int k=5; k<rows.get(i).length; k++)
					rawData[i][k-1] = Integer.toHexString(Integer.valueOf(rows.get(i)[k]));

			} catch (NumberFormatException e) {

			}
		}
		tableModel.setData(keyRawData, rawData);
	}

	protected void parseExperimentFrames() {
		if (!Config.payloadStore.initialized()) return;
		String[][] data = null;
					
		if (Config.displayRawRadData) {
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, false, reverse, layout.name);	
			if (data != null && data.length > 0)
				parseRawBytes(data,expTableModel);
		} else {
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, false, reverse, layout2.name);	
			if (data != null && data.length > 0) {
				//parseTelemetry(data);
				parseRawBytes(data,expTableModel2);
			}
		}

		if (showRawBytes.isSelected()) {
			scrollPane2.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			scrollPane2.setVisible(true);
			scrollPane.setVisible(false);
		}

		displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, layout.name));
		MainWindow.frame.repaint();
	}

	public void updateTab(FramePart rad, boolean refreshTable) {
		if (!Config.payloadStore.initialized()) return;
		if (rad == null) return;
		String[][] data = null;
		boolean RETURN_TYPE = true;
		boolean REVERSE = false;
//		if (Config.displayRawValues) {
		if (Config.payloadStore != null && canPktLayout != null) {
			// This returns SAMPLES packets and then we filter out the ones with the correct reset/uptime
			// This is a bit of a hack.  We should likely have a seperate number to return.  This is arbitrarily the same as the number of
			// payloads to display.
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, rad.resets, rad.uptime, RETURN_TYPE, REVERSE, canPktLayout.name);	
			if (data != null && data.length > 0) {
				parseCanPackets(data,canPacketRawTableModel, rad.resets, rad.uptime);
			}
		}
//		} else {
//			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, false, reverse, layout2.name);	
//			if (data != null && data.length > 0) {
//				//parseTelemetry(data);
//				parseRawBytes(data,expTableModel2);
//			}
//		}

		if (rad != null) {
			for (DisplayModule mod : topModules) {
				if (mod != null)
					mod.updateRtValues(rad);
			}
			if (bottomModules != null)
			for (DisplayModule mod : bottomModules) {
				if (mod != null)
					mod.updateRtValues(rad);
			}
		}
	}

	@Override
	public void run() {
		Thread.currentThread().setName("ExpTab: " + layout.name);
		running = true;
		done = false;
		int currentFrames = 0;
		boolean justStarted = true;
		while(running) {

			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HealthTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			if (foxId != 0 && Config.payloadStore.initialized()) {
				int frames = Config.payloadStore.getNumberOfFrames(foxId, layout.name);
				if (frames != currentFrames) {
					currentFrames = frames;
					updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
					displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, layout.name));
					Config.payloadStore.setUpdated(foxId, layout.name, false);
					MainWindow.setTotalDecodes();
					parseExperimentFrames(); //which also repaints the window
					if (justStarted) {
						openGraphs();
						justStarted = false;
					}
					MainWindow.frame.repaint();
				}
				// If either of these are toggled then redisplay the results
				// But this fails if we have a row selected!!
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					parseExperimentFrames();
					updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
				}
			}
		}
		done = true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		super.itemStateChanged(e);
		Object source = e.getItemSelectable();

		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawValues = false;
			} else {
				Config.displayRawValues = true;
			}
			Config.save();
			updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
		}
		if (source == cbUTC) {
			updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
		}
	}

	@Override
	public void parseFrames() {
		parseExperimentFrames();

	}

	protected void displayRow(JTable table, int fromRow, int row) {
		long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
		long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
		//Log.println("RESET: " + reset_l);
		//Log.println("UPTIME: " + uptime);
		int reset = (int)reset_l;
		if (table.getModel() instanceof CanPacketRawTableModel) {
			; // need to decide what to do if we click the actual packet.  For now do nothing
		} else {
//		if (Config.displayRawRadData) {
//			updateTab(Config.payloadStore.getFramePart(foxId, reset, uptime, layout.name, false), false);
//		} else {
			updateTab(Config.payloadStore.getFramePart(foxId, reset, uptime, layout2.name, false), false);
			//			updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
//		}
		}
		if (fromRow == NO_ROW_SELECTED)
			fromRow = row;
		if (fromRow <= row)
			table.setRowSelectionInterval(fromRow, row);
		else
			table.setRowSelectionInterval(row, fromRow);
	}

}