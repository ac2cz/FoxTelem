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
import telemetry.FramePart;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.RadiationTelemetry;
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
public class HerciLSTab extends ExperimentTab implements ItemListener, Runnable, MouseListener {

	public static final String HERCITAB = "HERCITAB";
	private static final String DECODED = "Housekeeping Payloads Decoded: ";
	public final int DEFAULT_DIVIDER_LOCATION = 350;
	
	JLabel lblName;
	private String NAME;
	JLabel lblFramesDecoded;
		
	//JCheckBox showRawBytes;
	ExperimentLayoutTableModel radTableModel;
	RadiationPacketTableModel radPacketTableModel;

	JPanel healthPanel;
	JPanel topHalfPackets;
	JPanel bottomHalfPackets;

	boolean displayTelem = true;
	
	public HerciLSTab(FoxSpacecraft sat)  {
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " IOWA HERCI Housekeeping";
		
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HERCITAB, "splitPaneHeight");
		
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

		BitArrayLayout rad = fox.getLayoutByName(Spacecraft.RAD2_LAYOUT);
		BitArrayLayout none = null;
		try {
			analyzeModules(rad, none, none, DisplayModule.DISPLAY_HERCI_HK);
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
	      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HERCITAB, "splitPaneHeight", splitPaneHeight);
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
		
		radTableModel = new ExperimentLayoutTableModel(rad);
		radPacketTableModel = new RadiationPacketTableModel();
		addTables(radTableModel,radPacketTableModel);

		addPacketModules();
		topHalfPackets.setVisible(false);
		bottomHalfPackets.setVisible(false);
		
		// initial populate
		parseRadiationFrames();
	}
	
	private void displayFramesDecoded(int u) {
		lblFramesDecoded.setText(DECODED + u);
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
		
		for (int i=0; i<table.getColumnCount()-2; i++) {
			column = table.getColumnModel().getColumn(i+2);
			column.setPreferredWidth(25);
		}

		column = table2.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table2.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);

		column = table2.getColumnModel().getColumn(2);
		column.setPreferredWidth(80);

		column = table2.getColumnModel().getColumn(3);
		column.setPreferredWidth(70);

		column = table2.getColumnModel().getColumn(4);
		column.setPreferredWidth(600);

		//packetTable.getSelectionModel().addListSelectionListener(this);
		//table.getSelectionModel().addListSelectionListener(this);
		//packetTable.getRowSelectionAllowed();
				
	}
	
	protected void parseRadiationFrames() {
		
			if (Config.displayRawRadData) {
				String[][] data = Config.payloadStore.getRadData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
				if (data != null && data.length > 0)
					parseRawBytes(data,radTableModel);
			} else {
				String[][] data = Config.payloadStore.getRadTelemData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
				if (data != null && data.length > 0) {
					parseTelemetry(data);
				}
			//		topHalfPackets.setVisible(false);
			//		bottomHalfPackets.setVisible(false);
			//		topHalf.setVisible(true);
			//		bottomHalf.setVisible(true);

			}
	
		if (showRawBytes.isSelected()) {
			scrollPane2.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			scrollPane2.setVisible(true);
			scrollPane.setVisible(false);
		}
		
		MainWindow.frame.repaint();
	}
	
	
	private void parseTelemetry(String data[][]) {
		
		// Now put the telemetry data into the table data structure
		int len = data.length;
		long[][] keyPacketData = new long[len][2];
		String[][] packetData = new String[len][3];
		for (int i=0; i < len; i++) { 
			keyPacketData[len-i-1][0] = Long.parseLong(data[i][0]);
			keyPacketData[len-i-1][1] = Long.parseLong(data[i][1]);
			packetData[len-i-1][0] = "TELEMETRY";
			packetData[len-i-1][1] = ""+data[i][2];
			String telem = "";
			for (int j=2; j< fox.getLayoutByName(Spacecraft.RAD2_LAYOUT).fieldName.length+2; j++) {  // 24 is the number of fieleds in the HERCI LS Telem Data
				telem = telem + FoxDecoder.plainhex(Integer.parseInt(data[i][j])) + " ";
				
			}
			packetData[len-i-1][2] = telem;
		}

		if (packetData.length > 0) {
			radPacketTableModel.setData(keyPacketData, packetData);
		}
		
		//updateTab(data.get(packets.size()-1));
	}
	
	
	public void updateTab(FramePart rad, boolean refreshTable) {
		if (!Config.payloadStore.initialized()) return;
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
		Thread.currentThread().setName("HerciLSTab");
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
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					parseRadiationFrames();
					updateTab(Config.payloadStore.getRadTelem(foxId, START_RESET, START_UPTIME), true);
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatestRadTelem(foxId), true);
					
				}

				if ((foxId != 0) && Config.payloadStore.initialized()) {
					int frames = Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.RAD_LAYOUT);
					if (frames != currentFrames) {
						currentFrames = frames;
						//radPayload = Config.payloadStore.getLatestRad(foxId);
						Config.payloadStore.setUpdated(foxId, Spacecraft.RAD_LAYOUT, false);

						parseRadiationFrames();
						updateTab(Config.payloadStore.getRadTelem(foxId, START_RESET, START_UPTIME), true);
						displayFramesDecoded(frames);
						MainWindow.setTotalDecodes();
						if (justStarted) {
							openGraphs();
							justStarted = false;
						}
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
			updateTab(Config.payloadStore.getLatestRadTelem(foxId), true);
			
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
    	updateTab((RadiationTelemetry) Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.RAD2_LAYOUT, false), false);
    	
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
