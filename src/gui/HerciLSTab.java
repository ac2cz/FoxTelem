package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.TableColumn;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
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
public class HerciLSTab extends RadiationTab implements ItemListener, ListSelectionListener, Runnable {

	public static final String HERCITAB = "HERCITAB";
	private static final String DECODED = "Housekeeping Payloads Decoded: ";
	public final int DEFAULT_DIVIDER_LOCATION = 350;
	
	JLabel lblName;
	private String NAME;
	JLabel lblFramesDecoded;
		
	JCheckBox showRawValues;
	JCheckBox showRawBytes;
	RadiationTableModel radTableModel;
	RadiationPacketTableModel radPacketTableModel;
	JTable table;
	JTable packetTable;
	JScrollPane packetScrollPane;
	JScrollPane scrollPane;
	
	JPanel healthPanel;
	JPanel topHalfPackets;
	JPanel bottomHalfPackets;
	DisplayModule vucModule;
	DisplayModule lepModule;
	DisplayModule lastExposure;
	DisplayModule lastState;
	
	DisplayModule vulcanExp1;
	DisplayModule vulcanExp2;
	DisplayModule vulcanExp3;
	DisplayModule vulcanExp4;
	
	JRadioButton decodePacket;
	JRadioButton decodeTelem;
	
	boolean displayTelem = true;
	
	public HerciLSTab(FoxSpacecraft sat)  {
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " IOWA HERCI Housekeeping";
		
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), HERCITAB, "splitPaneHeight");
		
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
	      		Config.saveGraphIntParam(fox.getIdString(), HERCITAB, "splitPaneHeight", splitPaneHeight);
	          }
	      });
	    }
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);
				
		showRawValues = new JCheckBox("Dislay Raw Values", Config.displayRawValues);
		bottomPanel.add(showRawValues );
		showRawValues.addItemListener(this);
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
		
		addTables();

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
	
	
	private void addTables() {
		radTableModel = new RadiationTableModel();
		
		table = new JTable(radTableModel);
		table.setAutoCreateRowSorter(true);
		
		radPacketTableModel = new RadiationPacketTableModel();
		packetTable = new JTable(radPacketTableModel);
		packetTable.setAutoCreateRowSorter(true);
		
		//JScrollPane scrollPane = new JScrollPane(table);
		scrollPane = new JScrollPane (table, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//table.setMinimumSize(new Dimension(6200, 6000));
		centerPanel.add(scrollPane);

		packetScrollPane = new JScrollPane (packetTable, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		packetTable.setFillsViewportHeight(true);
		packetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//table.setMinimumSize(new Dimension(6200, 6000));
		centerPanel.add(packetScrollPane);

		
		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		
		for (int i=0; i<58; i++) {
			column = table.getColumnModel().getColumn(i+2);
			column.setPreferredWidth(25);
		}

		column = packetTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = packetTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);

		column = packetTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(80);

		column = packetTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(70);

		column = packetTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(600);

		//packetTable.getSelectionModel().addListSelectionListener(this);
		//table.getSelectionModel().addListSelectionListener(this);
		//packetTable.getRowSelectionAllowed();
				
	}
	
	protected void parseRadiationFrames() {
		
			if (Config.displayRawRadData) {
				String[][] data = Config.payloadStore.getRadData(SAMPLES, fox.foxId, START_RESET, START_UPTIME);
				if (data.length > 0)
					radTableModel.setData(parseRawBytes(data));
			} else {
				String[][] data = Config.payloadStore.getRadTelemData(SAMPLES, fox.foxId, START_RESET, START_UPTIME);
				if (data.length > 0) {
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
	
	
	private void parseTelemetry(String data[][]) {
		
		// Now put the telemetry data into the table data structure
		int len = data.length;
		String[][] packetData = new String[len][5];
		for (int i=0; i < len; i++) { 
			packetData[len-i-1][0] = ""+data[i][0];
			packetData[len-i-1][1] = ""+data[i][1];
			packetData[len-i-1][2] = "TELEMETRY";
			packetData[len-i-1][3] = ""+data[i][2];
			String telem = "";
			for (int j=2; j< fox.getLayoutByName(Spacecraft.RAD2_LAYOUT).fieldName.length+2; j++) {  // 24 is the number of fieleds in the HERCI LS Telem Data
				telem = telem + FoxDecoder.plainhex(Integer.parseInt(data[i][j])) + " ";
				
			}
			packetData[len-i-1][4] = telem;
		}

		if (packetData.length > 0) {
			radPacketTableModel.setData(packetData);
		}
		updateTab(Config.payloadStore.getRadTelem(foxId, START_RESET, START_UPTIME));
		//updateTab(data.get(packets.size()-1));
	}
	
	
	public void updateTab(FramePart rad) {
		
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
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					parseRadiationFrames();
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatestRadTelem(foxId));
				}

				if (foxId != 0)
					if (Config.payloadStore.getUpdated(foxId, Spacecraft.RAD_LAYOUT)) {
						//radPayload = Config.payloadStore.getLatestRad(foxId);
						Config.payloadStore.setUpdated(foxId, Spacecraft.RAD_LAYOUT, false);

						parseRadiationFrames();
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.RAD_LAYOUT));
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
		Object source = e.getItemSelectable();
		
		if (source == showRawBytes) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawRadData = false;
			} else {
				Config.displayRawRadData = true;
			}
			if (showRawBytes.isSelected()) {
				packetScrollPane.setVisible(false); 
				scrollPane.setVisible(true);
			} else { 
				packetScrollPane.setVisible(true);
				scrollPane.setVisible(false);
			}

			parseRadiationFrames();
			
		}
		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawValues = false;
			} else {
				Config.displayRawValues = true;
			}

			updateTab(Config.payloadStore.getLatestRadTelem(foxId));
			
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (e.getSource() == decodePacket) { 
			displayTelem=false;
			parseRadiationFrames();
		}
		if (e.getSource() == decodeTelem) {
			displayTelem=true;
			parseRadiationFrames();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		Object source = e.getSource();
		if (source == packetTable.getSelectionModel() ) {
			int r = packetTable.getSelectedRow();
			if (r >=0 && r <= packetTable.getRowCount()) {
				int reset = Integer.parseInt( (String) packetTable.getValueAt(r, 0));
				long uptime = Long.parseLong( (String) packetTable.getValueAt(r, 1));
				textFromReset.setText(""+reset);
				textFromUptime.setText(""+uptime);
				System.out.println(reset + " " + uptime);
				parseRadiationFrames();
			}
		}
		if (source == table.getSelectionModel() ) {
			int r = table.getSelectedRow();
			if (r >=0 && r <= table.getRowCount()) {
				int reset = Integer.parseInt( (String) table.getValueAt(r, 0));
				long uptime = Long.parseLong( (String) table.getValueAt(r, 1));
				System.out.println(reset + " " + uptime);
				parseRadiationFrames();
			}
		}
	}

	@Override
	public void parseFrames() {
		// TODO Auto-generated method stub
		
	}

}
