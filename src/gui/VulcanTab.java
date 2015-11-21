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
public class VulcanTab extends RadiationTab implements ItemListener, Runnable {

	public static final String VULCANTAB = "VULCANTAB";
	private static final String DECODED = "Radiation Payloads Decoded: ";
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
	
	public VulcanTab(Spacecraft sat)  {
		
		super();
		fox = sat;
		foxId = fox.foxId;
		NAME = fox.toString() + " Vanderbilt University Radiation Experiments";
		
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), VULCANTAB, "splitPaneHeight");
		
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
	      		Config.saveGraphIntParam(fox.getIdString(), VULCANTAB, "splitPaneHeight", splitPaneHeight);
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

		decodePacket = addRadioButton("Packets (Buffered Mode)", bottomPanel );
		decodeTelem = addRadioButton("Telemetry", bottomPanel );
		ButtonGroup group = new ButtonGroup();
		group.add(decodePacket);
		group.add(decodeTelem);
		if (displayTelem) 
			decodeTelem.setSelected(true);
		else
			decodePacket.setSelected(true);
		
		decodeTelem.setMinimumSize(new Dimension(1600, 14));
		decodeTelem.setMaximumSize(new Dimension(1600, 14));

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
		
		vucModule = new DisplayModule(fox, "Vanderbilt University Controller (VUC)", 6, DisplayModule.DISPLAY_VULCAN);
		topHalfPackets.add(vucModule);
		vucModule.addName(1, "Uptime (s)", "VUC UPTIME", DisplayModule.DISPLAY_VULCAN);
		vucModule.addName(2, "Livetime (s)", "VUC LIVETIME", DisplayModule.DISPLAY_VULCAN);
		vucModule.addName(3, "Hard Resets", "HARD RESETS", DisplayModule.DISPLAY_VULCAN);
		vucModule.addName(4, "Soft Resets", "SOFT RESETS", DisplayModule.DISPLAY_VULCAN);
		vucModule.addName(5, "Run State", "VUC RUN STATE", DisplayModule.DISPLAY_VULCAN);

		lepModule = new DisplayModule(fox, "Low Energy Proton (LEP) Experiment", 5, DisplayModule.DISPLAY_LEP);
		topHalfPackets.add(lepModule);
		lepModule.addName(1, "Restarts", "LEP RESTARTS", DisplayModule.DISPLAY_LEP);
		lepModule.addName(2, "Uptime (s)", "LEP UPTIME", DisplayModule.DISPLAY_LEP);
		lepModule.addName(3, "Livetime (s)", "LEP LIVETIME", DisplayModule.DISPLAY_LEP);
		lepModule.addName(4, "Total Memory Upsets", "LEP TOTAL MEMORY UPSETS", DisplayModule.DISPLAY_LEP);

		lastExposure = new DisplayModule(fox, "Last LEP Exposure", 6, DisplayModule.DISPLAY_LEP_EXPOSURE);
		topHalfPackets.add(lastExposure);
		lastExposure.addName(1, "Start (s)", "LOCAL CLOCK START", DisplayModule.DISPLAY_LEP_EXPOSURE);
		lastExposure.addName(2, "End (s)", "LOCAL CLOCK END", DisplayModule.DISPLAY_LEP_EXPOSURE);
		lastExposure.addName(3, "Memories S0-S7", "MEMORIES", DisplayModule.DISPLAY_LEP_EXPOSURE);
		lastExposure.addName(4, "Current (mA)", "CURRENT", DisplayModule.DISPLAY_LEP_EXPOSURE);
		lastExposure.addName(5, "Upsets", "UPSETS", DisplayModule.DISPLAY_LEP_EXPOSURE);

		lastState = new DisplayModule(fox, "Last State", 4, DisplayModule.DISPLAY_LEP_EXPOSURE);
		topHalfPackets.add(lastState);
		lastState.addName(1, "Clock (s)", "LOCAL CLOCK", DisplayModule.DISPLAY_LEP_EXPOSURE);
		lastState.addName(2, "Experiment", "EXPERIMENT", DisplayModule.DISPLAY_LEP_EXPOSURE);
		lastState.addName(3, "State", "STATE", DisplayModule.DISPLAY_LEP_EXPOSURE);
		
		vulcanExp1 = addVulcanExpModule("Vulcan Experiment 1", "1");
		vulcanExp2 = addVulcanExpModule("Vulcan Experiment 2", "2");
		vulcanExp3 = addVulcanExpModule("Vulcan Experiment 3", "3");
		vulcanExp4 = addVulcanExpModule("Vulcan Experiment 4", "4");
	}
	
	private DisplayModule addVulcanExpModule(String title, String number) {
		DisplayModule mod;
		mod = new DisplayModule(fox, title, 4, DisplayModule.DISPLAY_VULCAN_EXP);
		
		bottomHalfPackets.add(mod);
		mod.addName(1, "Drift", "EXP" + number + " DRIFT" , DisplayModule.DISPLAY_VULCAN_EXP);
		mod.addName(2, "Power (mW) ", "EXP" + number + " POWER", DisplayModule.DISPLAY_VULCAN_EXP);
		mod.addName(3, "State", "EXP" + number + " STATE", DisplayModule.DISPLAY_VULCAN_EXP);
		return mod;
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
				if (displayTelem) {
					parseTelemetry(data);
					topHalfPackets.setVisible(false);
					bottomHalfPackets.setVisible(false);
					topHalf.setVisible(true);
					bottomHalf.setVisible(true);
				}
				else {
					parsePackets(data);
					topHalfPackets.setVisible(true);
					bottomHalfPackets.setVisible(true);
					topHalf.setVisible(false);
					bottomHalf.setVisible(false);

				}
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
			for (int k=2; k<RadiationTelemetry.MAX_RAD_TELEM_BYTES+2; k++) {  // Add 2 to skip past reset uptime
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
		updateTab(Config.payloadStore.getLatestRadTelem(foxId));
		//updateTab(packets.get(packets.size()-1));
	}
	
	private void parsePackets(String data[][]) {
		// We decode the radiation packets
		// The data returned is a two dimensional array, one row per payload with the first two columns being reset and uptime and the next 58 being data
		// These are in the order received, so we hope that data carries over from one packet to another, but it may not.
		// When we finish a packet, it is stored unless it spanned a row.
		// First we divide this into packets based on the 0x7E seperator
		
		int i=2; // position in the payload, start after uptime
		int p=0; // payload number
		int bytesAdded=0;
		boolean hasMoreBytes = true;
		boolean endOfRowReached=false; // used to keep track of packets that span over the end of a row
		boolean foundPacket = false;
		ArrayList<RadiationPacket> packets = new ArrayList<RadiationPacket>(20);
		RadiationPacket previousPacket = null;  // stores a packet that spanned a row
		RadiationPacket packet = null;
//		int debug=0;
		
		while (hasMoreBytes) {
			try {
				if (Integer.valueOf(data[p][i]) == 0x7E) {
					//Log.println("FOUND Radiation packet! Reset: " + Integer.valueOf(data[p][0]) + " uptime " + Integer.valueOf(data[p][1]));
					// We found a packet marker, this is either the start of a packet or, if foundPacket is set
					// this is the end of a packet and the start of the next
					if (foundPacket) {
						// This is the end of a packet and the start of the next
						// If endOfRowsReached is set then this spanned over the end of a row
						// If previousPacket != null then this is the first packet after a packet that spanned a row
						
						if (previousPacket != null) {
							// This is the first packet after one that spanned a row
							// if it was corrupt it might be too long, so we catch an exception
							try {
								packet.parseRawBytes();
								previousPacket.parseRawBytes();
							} catch (CobsDecodeException e) {
								// We dont print this error because it is frequent
								//
								//Log.println("ERROR: Found corrupt rad packet in reset: " + previousPacket.reset + " uptime: " + previousPacket.uptime + " at seq: " + previousPacket.getSequence() + " followed by " + packet.getSequence());
								//e.printStackTrace(Log.getWriter());
							}
							int previousSeq = previousPacket.getSequence();
							int sequence = packet.getSequence();
							if (sequence == previousSeq+1) {
								// This follows and the previous packet is good
								packets.add(previousPacket);
								//System.out.println("Added rad packet across frames: " + previousPacket.getSequence() + " followed by " + packet.getSequence());
							} else {
								// No need to do anything
								//System.out.println("Did not add corrupt rad packet seq: " + previousPacket.getSequence() + " followed by " + packet.getSequence());
							}
							previousPacket = null;  // set this to null again regardless

						}

						if (endOfRowReached) {
							// This is a packet that spanned the end of a row
							endOfRowReached = false;
							previousPacket = packet;  // hold it until we decode the next packet, so we can see if it followed sequentially
						} else {
							packets.add(packet); // add the packet
						}
						packet = new RadiationPacket(Integer.valueOf(data[p][0]),Long.valueOf(data[p][1]));
						bytesAdded = 0;
					} else {
						// start of a packet for the first time
						foundPacket = true;
						packet = new RadiationPacket(Integer.valueOf(data[p][0]),Long.valueOf(data[p][1]));
						bytesAdded = 0;
					}
					i++;
				}

				if (foundPacket) {
					if (bytesAdded < packet.MAX_PACKET_BYTES) {
						packet.addRawByte(Integer.valueOf(data[p][i]));
					//	Log.println("Added: "+Integer.toHexString(Integer.valueOf(data[p][i])));
						bytesAdded++;
					}
				}
			} catch (NumberFormatException e) {
				//Log.println("Number issue with: " + data[p][i]);
			}
			i++;
			if (i == data[p].length) {
				// Here we have spanned a row and are starting a new one.
				endOfRowReached=true;
				i=2;
				p++;
				if (p == data.length) {
					hasMoreBytes = false;
					// Don't add the final partial packet, it will always be corrupt
					//if (foundPacket)
					//	packets.add(packet);
				}
			}
		}

		int lastTelem = -1;
		int lastExpStart = -1;
		int lastExpEnd = -1;
		int lastStateChange = -1;
		String[][] packetData = new String[packets.size()][5];
		for (i=0; i < packets.size(); i++) { 
			try {
				packets.get(i).parseRawBytes();
			} catch (CobsDecodeException e) {
///////						Log.println("ERROR: parsing radition bytes in reset: " + packets.get(i).reset + " uptime: " + packets.get(i).uptime + " seq: " + packets.get(i).getSequence());
			}
			packetData[packets.size()-i-1][0] = ""+packets.get(i).reset;
			packetData[packets.size()-i-1][1] = ""+packets.get(i).uptime;
			String type = "";
			type = packets.get(i).getTypeString();

			packetData[packets.size()-i-1][2] = type;
			packetData[packets.size()-i-1][3] = ""+packets.get(i).fieldValue[1]; // sequence
			String bytes = packets.get(i).getDataString();
			packetData[packets.size()-i-1][4] = bytes; 
			if (packets.get(i).getType() == RadiationPacket.TELEM)
				lastTelem = i;
			if (packets.get(i).getType() == RadiationPacket.LEP_START )
				lastExpStart = i;
			if (packets.get(i).getType() == RadiationPacket.LEP_END )
				lastExpEnd = i;
			if (packets.get(i).getType() == RadiationPacket.STATE )
				lastStateChange = i;
		}
		if (packetData.length > 0) {
			radPacketTableModel.setData(packetData);

			// Now update the modules with the latest packets of each type
			if (lastTelem != -1) {
				vucModule.updateVulcanValues(packets.get(lastTelem));
				lepModule.updateVulcanValues(packets.get(lastTelem));
				lepModule.updateVulcanValues(packets.get(lastTelem));
				vulcanExp1.updateVulcanValues(packets.get(lastTelem));
				vulcanExp2.updateVulcanValues(packets.get(lastTelem));
				vulcanExp3.updateVulcanValues(packets.get(lastTelem));
				vulcanExp4.updateVulcanValues(packets.get(lastTelem));
			}
			if (lastStateChange != -1)
				lastState.updateVulcanValues(packets.get(lastStateChange));
			if (lastExpStart != -1)
				lastExposure.updateVulcanValues(packets.get(lastExpStart));
			if (lastExpEnd != -1)
				lastExposure.updateVulcanValues(packets.get(lastExpEnd));

			MainWindow.frame.repaint();

		} else {
			String[][] empty =  {{"","","","",""} };
			radPacketTableModel.setData(empty);
		}

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
			if (showRawValues.isSelected()) {
				packetScrollPane.setVisible(false); 
				scrollPane.setVisible(true);
			} else { 
				packetScrollPane.setVisible(true);
				scrollPane.setVisible(false);
			}

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
