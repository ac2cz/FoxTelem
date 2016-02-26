package gui;

import java.awt.BorderLayout;
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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.TableColumn;

import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.PayloadHERCIhighSpeed;
import telemetry.PayloadRadExpData;
import telemetry.SatPayloadStore;
import common.Config;
import common.Log;
import common.Spacecraft;
import decoder.Decoder;

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
public class HerciHSTab extends RadiationTab implements Runnable, ItemListener, ListSelectionListener {

	public static final String HERCITAB = "HERCITAB";
	public final int DEFAULT_DIVIDER_LOCATION = 226;
	PayloadHERCIhighSpeed hsPayload;

	JLabel lblFramesDecoded;
	JLabel lblHSpayload;
	int displayRows = PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE/32+1;
	JLabel[] lblBytes = new JLabel[displayRows];
	
	HerciHSTableModel radTableModel;
	JTable table;
	HerciHsPacketTableModel radPacketTableModel;
	JTable packetTable;
	JScrollPane scrollPane;
	JScrollPane packetScrollPane;
	JCheckBox showRawValues;
	JCheckBox showRawBytes;
	private static final String DECODED = "HS Payloads Decoded: ";

	
	public HerciHSTab(Spacecraft sat) {
		super();
		fox = sat;
		foxId = fox.foxId;

		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), HERCITAB, "splitPaneHeight");
		
		JLabel lblId = new JLabel("University of Iowa High Energy Radiation CubeSat Instrument (HERCI)");
		lblId.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblId.setForeground(textLblColor);
		topPanel.add(lblId);
		lblId.setMaximumSize(new Dimension(1600, 20));
		lblId.setMinimumSize(new Dimension(1600, 20));
	
		lblFramesDecoded = new JLabel(DECODED);
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblFramesDecoded.setBorder(new EmptyBorder(5, 2, 5, 5) );
		topPanel.add(lblFramesDecoded);

		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
		
		JPanel healthPanel = new JPanel();
		healthPanel.setLayout(new BoxLayout(healthPanel, BoxLayout.Y_AXIS));
		
		lblHSpayload = new JLabel();
		//lblHSpayload.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblHSpayload.setBorder(new EmptyBorder(5, 2, 5, 5) );
		
		healthPanel.add(lblHSpayload);		
		for (int r=0; r<displayRows; r++) {
			lblBytes[r] = new JLabel();
		//	healthPanel.add(lblBytes[r]);
		}
		

		initDisplayHalves(healthPanel);

		BitArrayLayout rad = fox.herciHS2Layout;
		BitArrayLayout none = null;
		try {
			analyzeModules(rad, none, none, DisplayModule.DISPLAY_HERCI);
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
		
		showRawValues = new JCheckBox("Display Raw Values", Config.displayRawValues);
		bottomPanel.add(showRawValues );
		showRawValues.addItemListener(this);
		showRawBytes = new JCheckBox("Show Raw Bytes", Config.displayRawRadData);
		bottomPanel.add(showRawBytes );
		showRawBytes.addItemListener(this);
	//	showRawBytes.setMinimumSize(new Dimension(1600, 14));
	//	showRawBytes.setMaximumSize(new Dimension(1600, 14));

		addBottomFilter();

		addTables();

	}

	private void addTables() {
		radTableModel = new HerciHSTableModel();
		
		table = new JTable(radTableModel);
		table.setAutoCreateRowSorter(true);
		
		radPacketTableModel = new HerciHsPacketTableModel();
		packetTable = new JTable(radPacketTableModel);
		packetTable.setAutoCreateRowSorter(true);
		
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
		
		for (int i=0; i<PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE; i++) {
			column = table.getColumnModel().getColumn(i+2);
			if (i <100)
				column.setPreferredWidth(25);
			else
				column.setPreferredWidth(30);
		}

		column = packetTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = packetTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		column = packetTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(90);

		column = packetTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(35);
		column = packetTable.getColumnModel().getColumn(4);
		column.setPreferredWidth(35);

		column = packetTable.getColumnModel().getColumn(5);
		column.setPreferredWidth(55);

		column = packetTable.getColumnModel().getColumn(6);
		column.setPreferredWidth(35);
		
		column = packetTable.getColumnModel().getColumn(7);
		column.setPreferredWidth(45);
		column = packetTable.getColumnModel().getColumn(8);
		column.setPreferredWidth(45);
		column = packetTable.getColumnModel().getColumn(9);
		column.setPreferredWidth(45);

		column = packetTable.getColumnModel().getColumn(10);
		column.setPreferredWidth(600);

		if (showRawBytes.isSelected()) {
			packetScrollPane.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			packetScrollPane.setVisible(true);
			scrollPane.setVisible(false);
		}
		packetTable.getSelectionModel().addListSelectionListener(this);
		table.getSelectionModel().addListSelectionListener(this);
				
	}

	private String[][] parseMiniPackets() {
		String[][] rawData = Config.payloadStore.getHerciPacketData(this.SAMPLES, this.foxId, 0, 0);
		String[][] data = new String[rawData.length][11];
		
		//for (int k =0; k < rawData.length; k++) {
		for (int k =rawData.length-1; k >= 0; k--) {
			data[rawData.length-k-1][0] = rawData[k][0];
			data[rawData.length-k-1][1] = rawData[k][1];
			data[rawData.length-k-1][2] = rawData[k][2] + ":" + rawData[k][4];  //acquire time
			data[rawData.length-k-1][3] = rawData[k][5];
			data[rawData.length-k-1][4] = rawData[k][6];
			data[rawData.length-k-1][5] = ""+Integer.parseInt(rawData[k][7])/40+":"+Integer.parseInt(rawData[k][7])%40; //rti
			for (int j=8; j<12; j++) {
				data[rawData.length-k-1][j-2] = Decoder.hex(Integer.parseInt(rawData[k][j]) & 0xFF);
			}
			data[rawData.length-k-1][10] = "";
			for (int j=13; j<rawData[k].length; j++) {
				if (rawData[k][j] != null)
					data[rawData.length-k-1][10] = data[rawData.length-k-1][10] + Decoder.plainhex(Integer.parseInt(rawData[k][j]) & 0xFF) +" ";
			}
		}
		
		return data;
	}

	private String[][] parseRawBytes() {
		String[][] rawData = new String[1][PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE+2];
		for (int i = 0; i < rawData.length; i ++) {
			rawData[i][0] = Integer.toString(hsPayload.getResets());
			rawData[i][1] = Long.toString(hsPayload.getUptime());

			for (int k =0; k < PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE; k++) {
				rawData[i][k+2] = Decoder.plainhex(hsPayload.fieldValue[k] & 0xff);
			}
		}
		
		return rawData;
	}
	
	protected void parseRadiationFrames() {
		lblHSpayload.setText("Last HERCI EXPERIMENT HIGH SPEED PAYLOAD: " + PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE + " bytes. Reset:" + hsPayload.getResets() + " Uptime:" + hsPayload.getUptime() );
		if (Config.displayRawRadData) {
			String[][] data = parseRawBytes();
			if (data.length > 0) {
				radTableModel.setData(data);
			}
			packetScrollPane.setVisible(false); 
			scrollPane.setVisible(true);
		} else {
			String[][] data = parseMiniPackets();
			if (data.length > 0) {
				radPacketTableModel.setData(data);
			}
			packetScrollPane.setVisible(true);
			scrollPane.setVisible(false);
		}
		
		updateTab(Config.payloadStore.getLatestHerciHeader(foxId));
		MainWindow.frame.repaint();

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
	}
	private void displayFramesDecoded(int u) {
		lblFramesDecoded.setText(DECODED + u);
	}

	@Override
	public void run() {
		running = true;
		done = false;
		while(running) {

			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HERCI thread interrupted");
				e.printStackTrace(Log.getWriter());
			}
			if (foxId != 0)
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					if (hsPayload != null)
						parseRadiationFrames();
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatestHerciHeader(foxId));
				}
				if (Config.payloadStore.getUpdatedHerci(foxId)) {
					hsPayload = Config.payloadStore.getLatestHerci(foxId);
					Config.payloadStore.setUpdatedHerci(foxId, false);

					if (hsPayload != null)
						parseRadiationFrames();
					
					displayFramesDecoded(Config.payloadStore.getNumberOfHerciFrames(foxId));
					MainWindow.setTotalDecodes();
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

			if (hsPayload != null) {
				parseRadiationFrames();
				updateTab(hsPayload);
			}
			
		}
		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawValues = false;
			} else {
				Config.displayRawValues = true;
			}

			if (hsPayload != null) {
				parseRadiationFrames();
				updateTab(hsPayload);
			}
			
		}

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
