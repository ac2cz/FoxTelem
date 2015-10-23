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
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.TableColumn;

import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.CobsDecodeException;
import telemetry.LayoutLoadException;
import telemetry.PayloadHERCIHousekeeping;
import telemetry.RadiationPacket;
import telemetry.RadiationTelemetry;
import common.Config;
import common.Log;
import common.Spacecraft;

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
public class HerciLSTab extends RadiationTab implements ItemListener, Runnable {

	public static final String HERCITAB = "HERCITAB";
	private static final String DECODED = "Housekeeping Payloads Decoded: ";
	public final int DEFAULT_DIVIDER_LOCATION = 350;
	
	JLabel lblName;
	private String NAME;
	JLabel lblFramesDecoded;
		
	JCheckBox showRawValues;
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
	
	public HerciLSTab(Spacecraft sat)  {
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

		BitArrayLayout rad = fox.rad2Layout;
		BitArrayLayout none = null;
		try {
			analyzeModules(rad, none, none, DisplayModule.DISPLAY_VULCAN);
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
				
		showRawValues = new JCheckBox("Display Raw Values", Config.displayRawRadData);
				bottomPanel.add(showRawValues );
		showRawValues.addItemListener(this);

//		decodePacket = addRadioButton("Packets (Buffered Mode)", bottomPanel );
//		decodeTelem = addRadioButton("Telemetry", bottomPanel );
//		ButtonGroup group = new ButtonGroup();
//		group.add(decodePacket);
//		group.add(decodeTelem);
//		if (displayTelem) 
//			decodeTelem.setSelected(true);
//		else
//			decodePacket.setSelected(true);
		
//		decodeTelem.setMinimumSize(new Dimension(1600, 14));
//		decodeTelem.setMaximumSize(new Dimension(1600, 14));

		addBottomFilter();
		
		addTables();

		addPacketModules();
		topHalfPackets.setVisible(false);
		bottomHalfPackets.setVisible(false);
		
		// initial populate
		parseRadiationFrames();
	}
	
	private JRadioButton addRadioButton(String name, JPanel panel) {
		JRadioButton radioButton = new JRadioButton(name);
		radioButton.setEnabled(true);
		radioButton.addActionListener(this);
		panel.add(radioButton);
		return radioButton;
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

	}
	
	protected void parseRadiationFrames() {
		String[][] data = Config.payloadStore.getRadData(SAMPLES, fox.foxId, START_RESET, START_UPTIME);
		
		if (data.length > 0)
			if (Config.displayRawRadData) {
				radTableModel.setData(parseRawBytes(data));
			} else {
					parseTelemetry(data);
					topHalfPackets.setVisible(false);
					bottomHalfPackets.setVisible(false);
					topHalf.setVisible(true);
					bottomHalf.setVisible(true);

			}
	
		if (showRawValues.isSelected()) {
			packetScrollPane.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			packetScrollPane.setVisible(true);
			scrollPane.setVisible(false);
		}
		
		MainWindow.frame.repaint();
	}
	
	
	private void parseTelemetry(String data[][]) {
		
		ArrayList<RadiationTelemetry> packets = new ArrayList<RadiationTelemetry>(20);
		
		// try to decode any telemetry packets
		for (int i=0; i<data.length; i++) {
			RadiationTelemetry radTelem = null;
			radTelem = new RadiationTelemetry(Integer.valueOf(data[i][0]), Long.valueOf(data[i][1]), this.fox.rad2Layout);
			for (int k=2; k<PayloadHERCIHousekeeping.MAX_RAD_TELEM_BYTES+2; k++) {  // Add 2 to skip past reset uptime
				try {
					radTelem.addNext8Bits(Integer.valueOf(data[i][k]));
				} catch (NumberFormatException e) {

				}
			}
			if (radTelem != null) {
				packets.add(radTelem);
			}
			
		}
		
		// Now put the telemetry packets into the table data structure
		String[][] packetData = new String[packets.size()][5];
		for (int i=0; i < packets.size(); i++) { 
			packetData[packets.size()-i-1][0] = ""+packets.get(i).reset;
			packetData[packets.size()-i-1][1] = ""+packets.get(i).uptime;
			packetData[packets.size()-i-1][2] = "TELEMETRY";
			packetData[packets.size()-i-1][3] = ""+packets.get(i).fieldValue[3]; // UPTIME
			String telem = packets.get(i).toDataString(fox);
			packetData[packets.size()-i-1][4] = telem; 
		}

		if (packetData.length > 0) {
			radPacketTableModel.setData(packetData);
		}
		//updateTab(Config.payloadStore.getLatestRadTelem(foxId));
		updateTab(packets.get(packets.size()-1));
	}
	
	
	public void updateTab(BitArray rad) {
		
	//	System.out.println("GOT PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
		if (rad != null) {
			for (DisplayModule mod : topModules) {
				if (mod != null)
					mod.updateRtValues(rad);
			}
			for (DisplayModule mod : bottomModules) {
				if (mod != null)
					mod.updateRtValues(rad);
			}
		}
//		displayId(rad.getFoxId());
//		displayUptime(rad.getUptime());
//		displayResets(rad.getResets());
//		displayCaptureDate(rad.getCaptureDate());
	}

	
	@Override
	public void run() {
		running = true;
		done = false;
		while(running) {
			
			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HealthTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 			
			if (Config.displayRawRadData != showRawValues.isSelected()) {
				showRawValues.setSelected(Config.displayRawRadData);
				parseRadiationFrames();
			}

			if (foxId != 0)
				if (Config.payloadStore.getUpdatedRad(foxId)) {
					//radPayload = Config.payloadStore.getLatestRad(foxId);
					Config.payloadStore.setUpdatedRad(foxId, false);

					parseRadiationFrames();
					displayFramesDecoded(Config.payloadStore.getNumberOfRadFrames(foxId));
					MainWindow.setTotalDecodes();
				}
			
		}
		done = true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawRadData = false;
			} else {
				Config.displayRawRadData = true;
			}
	//		Config.save();
			parseRadiationFrames();
			
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

}
