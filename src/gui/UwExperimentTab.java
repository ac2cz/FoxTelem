package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.uw.CanPacket;
import common.Config;
import common.Log;
import common.Spacecraft;
import common.FoxSpacecraft;

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
public class UwExperimentTab extends ExperimentTab implements ItemListener, Runnable, MouseListener {

	public static final String UWTAB = "UWEXPTAB";
	private static final String DECODED = "Payloads Decoded: ";
	private static final String CAN_DECODED = " CAN Pkts: ";
	public final int DEFAULT_DIVIDER_LOCATION = 350;
	
	JLabel lblName;
	private String NAME;
	JLabel lblFramesDecoded;
		
	//JCheckBox showRawBytes;
	CanPacketRawTableModel radTableModel;
	CanPacketTableModel radPacketTableModel;

	JPanel healthPanel;
	JPanel topHalfPackets;
	JPanel bottomHalfPackets;

	boolean displayTelem = true;
	
	BitArrayLayout[] layout;
	public static int[] ids = {308871750, // RC_EPS_DIST_4 
				 308871681, // RC_EPS_BATT_2
				 0x12690204, // RC_EPS_BATT_5
				 307823128,  // RC_EPS_GEN_9
				 0x12590215,  // RC_EPS_GEN_6
				 0x12590216, // RC_EPS_GEN_7
				 0x12590217, // RC_EPS_GEN_8
				 0x12590250 // RX_EPS_DIST_14
				 };
	 
	public UwExperimentTab(FoxSpacecraft sat, int displayType)  {
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " CAN PACKETS";
		
		int j = 0;
		layout = new BitArrayLayout[ids.length];
		for (int canid : ids)
			 layout[j++] = Config.satManager.getLayoutByCanId(6, canid);

		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, UWTAB, "splitPaneHeight");
		
		lblName = new JLabel(NAME);
		lblName.setMaximumSize(new Dimension(1600, 20));
		lblName.setMinimumSize(new Dimension(1600, 20));
		lblName.setFont(new Font("SansSerif", Font.BOLD, 14));
		topPanel.add(lblName);
		
		lblFramesDecoded = new JLabel(DECODED + CAN_DECODED);
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblFramesDecoded.setBorder(new EmptyBorder(5, 2, 5, 5) );
		topPanel.add(lblFramesDecoded);
	
		healthPanel = new JPanel();
		
		healthPanel.setLayout(new BoxLayout(healthPanel, BoxLayout.Y_AXIS));
		healthPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		healthPanel.setBackground(Color.DARK_GRAY);
		
		topHalfPackets = new JPanel(); 
		topHalfPackets.setBackground(Color.DARK_GRAY);
		bottomHalfPackets = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars5.png");
		bottomHalfPackets.setBackground(Color.DARK_GRAY);
		healthPanel.add(topHalfPackets);
		healthPanel.add(bottomHalfPackets);
	
		initDisplayHalves(healthPanel);
		
		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));

		addModules();
		
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
	      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, UWTAB, "splitPaneHeight", splitPaneHeight);
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
		

		addBottomFilter();
		
		radTableModel = new CanPacketRawTableModel();
		radPacketTableModel = new CanPacketTableModel();
		addTables(radTableModel,radPacketTableModel);

		addPacketModules();
		topHalfPackets.setVisible(false);
		bottomHalfPackets.setVisible(false);
		
		// initial populate
		parseRadiationFrames();
	}
	
	public static boolean inCanIds(int id) {
		for (int canid : ids)
			if (id == canid) return true;
		return false;
	}
	
	void addModules() {
		BitArrayLayout rad = null;

		rad = fox.getLayoutByName(Spacecraft.CAN_LAYOUT);
		BitArrayLayout none = null;
		try {
//			analyzeModules(rad, none, none, DisplayModule.DISPLAY_UW); // add the experiment header to the top
			makeDisplayModules(layout, DisplayModule.DISPLAY_UW);	// add the CAN to the bottom
		} catch (LayoutLoadException e) {
			Log.errorDialog("FATAL - Load Aborted", e.getMessage());
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		}
	}
	
	protected void displayFramesDecoded(int u, int c) {
		lblFramesDecoded.setText(DECODED + u + CAN_DECODED + c);
	}
	
	private void addPacketModules() {

	}
	

	protected void addTables(AbstractTableModel radTableModel, AbstractTableModel radPacketTableModel) {
		super.addTables(radTableModel, radPacketTableModel);
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		
		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		column.setCellRenderer( centerRenderer );
		
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		column.setCellRenderer( centerRenderer );

		column = table.getColumnModel().getColumn(2);
		column.setPreferredWidth(55);
		column.setCellRenderer( centerRenderer );

		column = table.getColumnModel().getColumn(3);
		column.setPreferredWidth(75);
		column.setCellRenderer( centerRenderer );

		column = table.getColumnModel().getColumn(4);
		column.setPreferredWidth(75);
		column.setCellRenderer( centerRenderer );

		column = table.getColumnModel().getColumn(5);
		column.setPreferredWidth(55);
		column.setCellRenderer( centerRenderer );

		for (int i=0; i<8; i++) {
			column = table.getColumnModel().getColumn(i+6);
			column.setPreferredWidth(45);
		}

		// This is the table that shows CAN Packets and counts
		column = packetTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(100);
		column.setCellRenderer( centerRenderer );

		column = packetTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(150);
		column.setCellRenderer( centerRenderer );

		column = packetTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(150);

		column = packetTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(150);

		column = packetTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(155);
		column = packetTable.getColumnModel().getColumn(5);
		column.setPreferredWidth(55);
		column.setCellRenderer( centerRenderer );
	}
	
	protected void parseRawBytes(String data[][], CanPacketRawTableModel radTableModel) {
		long[][] keyRawData = new long[data.length][3];
		String[][] rawData = new String[data.length][CanPacket.MAX_PACKET_BYTES-CanPacket.ID_BYTES+3]; // +3 fields for ID, HEX ID LEN
		for (int i=0; i<data.length; i++) {
			// ID String
			int id = CanPacket.getIdFromRawBytes(Integer.valueOf(data[data.length-i-1][3]),Integer.valueOf(data[data.length-i-1][4]),
					Integer.valueOf(data[data.length-i-1][5]),Integer.valueOf(data[data.length-i-1][6]));
			int len = CanPacket.getLengthFromRawBytes(Integer.valueOf(data[data.length-i-1][3]),Integer.valueOf(data[data.length-i-1][4]),
					Integer.valueOf(data[data.length-i-1][5]),Integer.valueOf(data[data.length-i-1][6]));
			for (int k=0; k< 3; k++)
				try {
					if (k<3) // key
						keyRawData[i][k] = Long.parseLong(data[data.length-i-1][k]);
				} catch (NumberFormatException e) {

				}
			rawData[i][0] = ""+id;
			rawData[i][1] = String.format("%08x", id);
			rawData[i][2] = Integer.toString(len);
			for (int k=7; k< data[0].length; k++) {
				if (data[data.length-i-1].length > k && data[data.length-i-1][k] != null)
					rawData[i][k-4] = Integer.toHexString(Integer.valueOf(data[data.length-i-1][k]));
				if (rawData[i][k-4] == null)
					rawData[i][k-4] = "";
			}
		}
	radTableModel.setData(keyRawData, rawData);
	}
	
	private boolean in(int[] array, int a) {
		for (int b : array)
			if (a == b) return true;
		return false;
	}
	
	protected void parseRadiationFrames() {
		if (!Config.payloadStore.initialized()) return;
		String[][] data = null;

		if (Config.displayRawRadData) {
//			if (Config.splitCanPackets) {
			boolean showParsedPackets = false;
			if (showParsedPackets) {
				// for each CAN Packet get the layout and all the packets
				int maxCanIDs = 250;
				String[][][] all = new String[maxCanIDs][][];
				int number = 0;
				int total = 0;
				for (int id : fox.canFrames.canId) {
					if (!in(WodUwExperimentTab.wod_ids, id) ) {
						BitArrayLayout lay = Config.satManager.getLayoutByCanId(fox.foxId, id);
						String[][] records = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, lay.name);
						if (records.length > 0) {
							all[number] = records;
							number++;
							total = total + records.length;
						}
					}
				}
				int k = 0;
				data = new String[total][];
				for (int j=0; j<number; j++)
					for (int r=0; r<all[j].length; r++) {
						data[k++] = all[j][r];
					}
			} else {
				data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.CAN_PKT_LAYOUT);				
			}
			if (data != null && data.length > 0)
				parseRawBytes(data,radTableModel);
		} else {
//			if (Config.splitCanPackets) {
				// for each CAN Packet name, get the layout and see what total we have
				String[][] totals = new String[250][];
				int number = 0;
				for (int id : fox.canFrames.canId) {
					if (!in(WodUwExperimentTab.wod_ids, id) ) {
						BitArrayLayout lay = Config.satManager.getLayoutByCanId(fox.foxId, id);
						int total = Config.payloadStore.getNumberOfFrames(fox.foxId, lay.name);
						if (total > 0) {
							String[] row = new String[5];
							row[0] = ""+id;
							row[1] = fox.canFrames.getGroundByCanId(id);
							row[2] = fox.canFrames.getNameByCanId(id);
							row[3] = fox.canFrames.getSenderByCanId(id);
							row[4] = ""+total;
							totals[number] = row;
							number++;
						}
					}
				}
				data = new String[number][];
				for (int j=0; j<number; j++)
					data[j] = totals[j];
//			} else {
//				data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.CAN_PKT_LAYOUT);				
//			}
			if (data != null && data.length > 0) {
				parseTelemetry(data);
			}
			//		topHalfPackets.setVisible(false);
			//		bottomHalfPackets.setVisible(false);
			//		topHalf.setVisible(true);
			//		bottomHalf.setVisible(true);

		}

		if (showRawBytes.isSelected()) {
			packetScrollPane.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			packetScrollPane.setVisible(true);
			scrollPane.setVisible(false);
		}

		displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.CAN_LAYOUT),
				getTotalPackets());
		MainWindow.frame.repaint();
	}


	protected void parseTelemetry(String data[][]) {
		int len = data.length;
		long[][] keyPacketData = null;
		String[][] packetData = null;
//		if (Config.splitCanPackets) {
			keyPacketData = new long[len][1];
			packetData = new String[len][5];
			for (int i=0; i < len; i++) { 
				keyPacketData[len-i-1][0] = Long.parseLong(data[i][0]);
				packetData[len-i-1][0] = String.format("%08x", Long.parseLong(data[i][0]));
				packetData[len-i-1][1] = data[i][1];
				packetData[len-i-1][2] = data[i][2];
				packetData[len-i-1][3] = data[i][3];
				packetData[len-i-1][4] = data[i][4];
			}
//		} else {
//			keyPacketData = new long[len][3];
//			packetData = new String[len][3];
//			for (int i=0; i < len; i++) { 
//				keyPacketData[len-i-1][0] = Long.parseLong(data[i][0]);
//				keyPacketData[len-i-1][1] = Long.parseLong(data[i][1]);
//				keyPacketData[len-i-1][2] = Long.parseLong(data[i][2]);
//				int id = CanPacket.getIdFromRawBytes(Integer.valueOf(data[i][3]),Integer.valueOf(data[i][4]),
//						Integer.valueOf(data[i][5]),Integer.valueOf(data[i][6]));
//				int length = CanPacket.getLengthFromRawBytes(Integer.valueOf(data[i][3]),Integer.valueOf(data[i][4]),
//						Integer.valueOf(data[i][5]),Integer.valueOf(data[i][6]));
//
//				packetData[len-i-1][0] = String.format("%08x", id);
//				packetData[len-i-1][1] = Integer.toString(length);
//
//				String telem = "";
//				for (int j=7; j< data[0].length; j++) {  
//					telem = telem + FoxDecoder.plainhex(Integer.parseInt(data[i][j])) + " ";
//
//				}
//				packetData[len-i-1][2] = telem;
//			}
//		}

		if (packetData.length > 0) {
			
			radPacketTableModel.setData(keyPacketData, packetData);
		}
		 
	}
	
	
	public void updateTab(FramePart rad, boolean refreshTable) {
		if (!Config.payloadStore.initialized()) return;
	//	System.out.println("GOT PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
		if (rad != null) {
			for (DisplayModule mod : topModules) {
				if (mod != null) {
					if (mod.getTelemLayout() != null) {
						FramePart data = Config.payloadStore.getLatest(foxId, mod.getTelemLayout().name);
						if (data != null)
							mod.updateRtValues(data);
					}
				}
			}
			
		}
	}
	
	private int getTotalPackets() {
		int total = 0;
		if (showRawBytes.isSelected())
			total = Config.payloadStore.getNumberOfFrames(fox.foxId, Spacecraft.CAN_PKT_LAYOUT);
		else
		for (int id : fox.canFrames.canId) {
			if (!in(WodUwExperimentTab.wod_ids, id) ) {
				BitArrayLayout lay = Config.satManager.getLayoutByCanId(fox.foxId, id);
				total += Config.payloadStore.getNumberOfFrames(fox.foxId, lay.name);
			}
		}
		return total;
	}

	
	@Override
	public void run() {
		Thread.currentThread().setName("UwTab");
		running = true;
		done = false;
		boolean justStarted = true;
		while(running) {
			
			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HealthTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			if (foxId != 0 && Config.payloadStore.initialized()) {
				// If either of these are toggled then redisplay the results
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					parseRadiationFrames();
					//					for (BitArrayLayout lay : layout)
					//						updateTab(Config.payloadStore.getLatest(foxId, lay.name), true);
					updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.CAN_LAYOUT), true);
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					//					for (BitArrayLayout lay : layout)
					//						updateTab(Config.payloadStore.getLatest(foxId, lay.name), true);
					updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.CAN_LAYOUT), true);
				}

				boolean refresh = false;

				if (Config.payloadStore.getUpdated(foxId, Spacecraft.CAN_LAYOUT)) {
					//radPayload = Config.payloadStore.getLatestRad(foxId);
					Config.payloadStore.setUpdated(foxId, Spacecraft.CAN_LAYOUT, false);
					refresh = true;
				}
				if (refresh) {
					parseRadiationFrames();
					//						for (BitArrayLayout lay : layout)
					//							updateTab(Config.payloadStore.getLatest(foxId, lay.name), true);
					updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.CAN_LAYOUT), true);
					displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.CAN_LAYOUT),
							getTotalPackets());
					MainWindow.setTotalDecodes();
					if (justStarted) {
						openGraphs();
						justStarted = false;
					}
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
		//			for (BitArrayLayout lay : layout)
		//				updateTab(Config.payloadStore.getLatest(foxId, lay.name), true);

		updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.CAN_LAYOUT), true);

		//updateTab(Config.payloadStore.getLatestRad(foxId), true);  // we don't have RAD2 conversion for the Experiment Payloads so just get RAD.

	}
}

@Override
public void parseFrames() {
	parseRadiationFrames();

}

protected void displayRow(JTable table, int fromRow, int row) {
	if (Config.displayRawRadData) {
			long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
	    	long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
	    	//Log.println("RESET: " + reset_l);
	    	//Log.println("UPTIME: " + uptime);
	    	int reset = (int)reset_l;
	    	updateTab(Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.CAN_LAYOUT, false), false);
	} else {
		updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.CAN_LAYOUT), true);
	}
	if (fromRow == NO_ROW_SELECTED)
		fromRow = row;
	if (fromRow <= row)
		table.setRowSelectionInterval(fromRow, row);
	else
		table.setRowSelectionInterval(row, fromRow);
}

//	public void mouseClicked(MouseEvent e) {
//
//		if (showRawBytes.isSelected()) {
//			int row = table.rowAtPoint(e.getPoint());
//			int col = table.columnAtPoint(e.getPoint());
//			if (row >= 0 && col >= 0) {
//				//Log.println("CLICKED ROW: "+row+ " and COL: " + col);
//				displayRow(table, row);
//			}
//		} else {
//			int row = packetTable.rowAtPoint(e.getPoint());
//			int col = packetTable.columnAtPoint(e.getPoint());
//			if (row >= 0 && col >= 0) {
//				//Log.println("CLICKED ROW: "+row+ " and COL: " + col);
//				displayRow(packetTable, row);
//			}
//		}
//	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}