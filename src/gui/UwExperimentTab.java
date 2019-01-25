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
import javax.swing.table.TableColumn;

import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.PayloadUwExperiment;
import telemetry.uw.CanPacket;
import common.Config;
import common.Log;
import common.Spacecraft;
import common.FoxSpacecraft;
import decoder.FoxDecoder;

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
	
	public UwExperimentTab(FoxSpacecraft sat, int displayType)  {
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " CAN PACKETS";
		
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

		BitArrayLayout rad = null;

		rad = fox.getLayoutByName(Spacecraft.RAD_LAYOUT);
		BitArrayLayout none = null;
		try {
			analyzeModules(rad, none, none, DisplayModule.DISPLAY_UW);
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
	        	  Log.println("SplitPane: " + splitPaneHeight);
	      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, UWTAB, "splitPaneHeight", splitPaneHeight);
	          }
	      });
	    }
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);
				
//		showRawValues = new JCheckBox("Dislay Raw Values", Config.displayRawValues);
//		bottomPanel.add(showRawValues );
//		showRawValues.addItemListener(this);
		showRawBytes = new JCheckBox("Show Raw Bytes", Config.displayRawRadData);
		bottomPanel.add(showRawBytes );
		showRawBytes.addItemListener(this);
		

//		decodePacket = addRadioButton("Packets (Buffered Mode)", bottomPanel );
//		decodeTelem = addRadioButton("Telemetry", bottomPanel );
//		ButtonGroup group = new ButtonGroup();
//		group.add(decodePacket);
//		group.add(decodeTelem);
//		if (displayTelem) 
//			decodeTelem.setSelected(true);
//		else
//			decodePacket.setSelected(true);
		
//		showRawBytes.setMinimumSize(new Dimension(1600, 14));
//		showRawBytes.setMaximumSize(new Dimension(1600, 14));

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
	
	protected void displayFramesDecoded(int u, int c) {
		lblFramesDecoded.setText(DECODED + u + CAN_DECODED + c);
	}
	
	private void addPacketModules() {
		
	}
	
	
	protected void addTables(AbstractTableModel radTableModel, AbstractTableModel radPacketTableModel) {
		super.addTables(radTableModel, radPacketTableModel);
		
		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);

		column = table.getColumnModel().getColumn(2);
		column.setPreferredWidth(55);

		column = table.getColumnModel().getColumn(3);
		column.setPreferredWidth(65);
		
		column = table.getColumnModel().getColumn(4);
		column.setPreferredWidth(55);

		for (int i=0; i<8; i++) {
			column = table.getColumnModel().getColumn(i+5);
			column.setPreferredWidth(25);
		}

		column = packetTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = packetTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);

		column = packetTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(55);

		column = packetTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(65);

		column = packetTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(55);

		column = packetTable.getColumnModel().getColumn(5);
		column.setPreferredWidth(600);

		//packetTable.getSelectionModel().addListSelectionListener(this);
		//table.getSelectionModel().addListSelectionListener(this);
		//packetTable.getRowSelectionAllowed();
				
	}
	
	protected void parseRawBytes(String data[][], CanPacketRawTableModel radTableModel) {
		long[][] keyRawData = new long[data.length][3];
		String[][] rawData = new String[data.length][CanPacket.MAX_PACKET_BYTES-CanPacket.ID_BYTES+2]; // +2 fields for ID, LEN
		for (int i=0; i<data.length; i++)
			for (int k=0; k< data[0].length; k++)
				try {
					if (k<3) // key
						keyRawData[i][k] = Long.parseLong(data[data.length-i-1][k]);
					else {
						if (k==3) {
							// ID String
							int id = CanPacket.getIdfromRawID(Integer.valueOf(data[data.length-i-1][k]));
							int len = CanPacket.getLengthfromRawID(Integer.valueOf(data[data.length-i-1][k]));
							rawData[i][k-3] = String.format("%08x", id);
							rawData[i][k-2] = Integer.toString(len);
						} else {
							rawData[i][k-2] = Integer.toHexString(Integer.valueOf(data[data.length-i-1][k]));
							if (rawData[i][k-2] == null)
								rawData[i][k-2] = "";
						}
					}
				} catch (NumberFormatException e) {

				}
		radTableModel.setData(keyRawData, rawData);
	}
	
	protected void parseRadiationFrames() {
		
			if (Config.displayRawRadData) {
				String[][] data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.CAN_PKT_LAYOUT);
				if (data != null && data.length > 0)
					parseRawBytes(data,radTableModel);
			} else {
				String[][] data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.CAN_PKT_LAYOUT);
				//String[][] data = Config.payloadStore.getRadTelemData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
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
		
		MainWindow.frame.repaint();
	}
	
	
	protected void parseTelemetry(String data[][]) {
		int len = data.length;
		long[][] keyPacketData = new long[len][3];
		String[][] packetData = new String[len][3];
		for (int i=0; i < len; i++) { 
			keyPacketData[len-i-1][0] = Long.parseLong(data[i][0]);
			keyPacketData[len-i-1][1] = Long.parseLong(data[i][1]);
			keyPacketData[len-i-1][2] = Long.parseLong(data[i][2]);
			int id = CanPacket.getIdfromRawID(Integer.valueOf(data[i][3]));
			int length = CanPacket.getLengthfromRawID(Integer.valueOf(data[i][3]));
			packetData[len-i-1][0] = String.format("%08x", id);
			packetData[len-i-1][1] = Integer.toString(length);
	
			String telem = "";
			for (int j=4; j< data[0].length; j++) {  
				telem = telem + FoxDecoder.plainhex(Integer.parseInt(data[i][j])) + " ";
				
			}
			packetData[len-i-1][2] = telem;
		}

		if (packetData.length > 0) {
			radPacketTableModel.setData(keyPacketData, packetData);
		}
		 
	}
	
	
	public void updateTab(FramePart rad, boolean refreshTable) {
		
	//	System.out.println("GOT PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
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
					updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.RAD_LAYOUT), true);
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.RAD_LAYOUT), true);
					
				}

				if (foxId != 0)
					if (Config.payloadStore.getUpdated(foxId, Spacecraft.RAD_LAYOUT)) {
						//radPayload = Config.payloadStore.getLatestRad(foxId);
						Config.payloadStore.setUpdated(foxId, Spacecraft.RAD_LAYOUT, false);

						parseRadiationFrames();
						updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.RAD_LAYOUT), true);
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.RAD_LAYOUT),
								Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.CAN_PKT_LAYOUT));
						MainWindow.setTotalDecodes();
						if (justStarted) {
							openGraphs(FoxFramePart.TYPE_RAD_EXP_DATA);
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

			updateTab(Config.payloadStore.getLatestRad(foxId), true);  // we don't have RAD2 conversion for the Experiment Payloads so just get RAD.
			
		}
	}

	@Override
	public void parseFrames() {
		parseRadiationFrames();
		
	}
	
	protected void displayRow(JTable table, int fromRow, int row) {
		long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
    	long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
    	//Log.println("RESET: " + reset);
    	//Log.println("UPTIME: " + uptime);
    	int reset = (int)reset_l;
    	updateTab((PayloadUwExperiment) Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.RAD_LAYOUT, false), false);
    	
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