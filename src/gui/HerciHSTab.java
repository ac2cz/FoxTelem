package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
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
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.FramePart;
import telemetry.HerciHighspeedHeader;
import telemetry.LayoutLoadException;
import telemetry.PayloadHERCIhighSpeed;
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
public class HerciHSTab extends ExperimentTab implements Runnable, ItemListener, MouseListener {

	public static final String HERCITAB = "HERCITAB";
	public final int DEFAULT_DIVIDER_LOCATION = 226;
	PayloadHERCIhighSpeed hsPayload;
	HerciHighspeedHeader hsHeader;
	
	JLabel lblFramesDecoded;
	JLabel lblHSpayload;
	int displayRows = PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE/32+1;
	JLabel[] lblBytes = new JLabel[displayRows];
	
	HerciHSTableModel radTableModel;
	HerciHsPacketTableModel radPacketTableModel;
	
	//JCheckBox showRawBytes;
	private static final String DECODED = "HS Payloads Decoded: ";

	
	public HerciHSTab(FoxSpacecraft sat) {
		super();
		fox = sat;
		foxId = fox.foxId;

		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HERCITAB, "splitPaneHeight");
		
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

		BitArrayLayout rad = fox.getLayoutByName(Spacecraft.HERCI_HS_HEADER_LAYOUT);
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
	      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HERCITAB, "splitPaneHeight", splitPaneHeight);
	          }
	      });
	    }
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);
		
//		showRawValues = new JCheckBox("Display Raw Values", Config.displayRawValues);
//		bottomPanel.add(showRawValues );
//		showRawValues.addItemListener(this);
		showRawBytes = new JCheckBox("Show Raw Bytes", Config.displayRawRadData);
		bottomPanel.add(showRawBytes );
		showRawBytes.addItemListener(this);
	//	showRawBytes.setMinimumSize(new Dimension(1600, 14));
	//	showRawBytes.setMaximumSize(new Dimension(1600, 14));

		addBottomFilter();

		radTableModel = new HerciHSTableModel();
		radPacketTableModel = new HerciHsPacketTableModel();
		addTables(radTableModel,radPacketTableModel);

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
		for (int i=0; i<PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE; i++) {
			column = table.getColumnModel().getColumn(i+3);
			if (i <100)
				column.setPreferredWidth(25);
			else
				column.setPreferredWidth(30);
		}

		column = table2.getColumnModel().getColumn(0);
		column.setPreferredWidth(45);
		
		column = table2.getColumnModel().getColumn(1);
		column.setPreferredWidth(55);
		column = table2.getColumnModel().getColumn(2);
		column.setPreferredWidth(55);
		column = table2.getColumnModel().getColumn(3);
		column.setPreferredWidth(90);

		column = table2.getColumnModel().getColumn(4);
		column.setPreferredWidth(35);
		column = table2.getColumnModel().getColumn(5);
		column.setPreferredWidth(35);

		column = table2.getColumnModel().getColumn(6);
		column.setPreferredWidth(55);

		column = table2.getColumnModel().getColumn(7);
		column.setPreferredWidth(35);
		
		column = table2.getColumnModel().getColumn(8);
		column.setPreferredWidth(45);
		column = table2.getColumnModel().getColumn(9);
		column.setPreferredWidth(45);
		column = table2.getColumnModel().getColumn(10);
		column.setPreferredWidth(45);

		column = table2.getColumnModel().getColumn(11);
		column.setPreferredWidth(600);

		if (showRawBytes.isSelected()) {
			scrollPane2.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			scrollPane2.setVisible(true);
			scrollPane.setVisible(false);
		}
				
	}

	private void parseMiniPackets() {
		String[][] rawData = Config.payloadStore.getHerciPacketData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse);
		String[][] data = new String[rawData.length][9];
		long[][] keydata = new long[rawData.length][3];
		
		//for (int k =0; k < rawData.length; k++) {
		for (int k =rawData.length-1; k >= 0; k--) {
			keydata[rawData.length-k-1][0] = Long.parseLong(rawData[k][0]);
			keydata[rawData.length-k-1][1] = Long.parseLong(rawData[k][1]);
			keydata[rawData.length-k-1][2] = Long.parseLong(rawData[k][2]);
			
			data[rawData.length-k-1][0] = rawData[k][3] + ":" + rawData[k][5];  //acquire time
			data[rawData.length-k-1][1] = rawData[k][6];
			data[rawData.length-k-1][2] = rawData[k][7];
			data[rawData.length-k-1][3] = ""+Integer.parseInt(rawData[k][8])/40+":"+Integer.parseInt(rawData[k][8])%40; //rti
			for (int j=9; j<13; j++) {
				data[rawData.length-k-1][j-5] = FoxDecoder.hex(Integer.parseInt(rawData[k][j]) & 0xFF);
			}
			data[rawData.length-k-1][8] = "";
			for (int j=14; j<rawData[k].length; j++) {
				if (rawData[k][j] != null)
					data[rawData.length-k-1][8] = data[rawData.length-k-1][8] + FoxDecoder.plainhex(Integer.parseInt(rawData[k][j]) & 0xFF) +" ";
			}
		}
		
		if (data.length > 0) {
			radPacketTableModel.setData(keydata, data);
		}
	}

	private void parseRawBytes() {
		if (hsPayload == null) return;
		String[][] rawData = Config.payloadStore.getHerciHsData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
		//String[][] rawData = new String[1][PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE];
		String[][] data = new String[rawData.length][PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE];
		long[][] keydata = new long[rawData.length][3];
		int j = 0;
		for (int i = rawData.length-1; i >=0 ; i--) {
			int reset = (int)(Integer.parseInt(rawData[i][0]));
			long uptime = (long)(Long.parseLong(rawData[i][1]));	
			int type = (int)(Integer.parseInt(rawData[i][2]));
			/* this needs to be able to store the results as as string, not long
			if (showUTCtime) {
				keydata[i][1] = fox.getUtcTimeForReset(reset, uptime);
				keydata[i][0] = fox.getUtcDateForReset(reset, uptime);	
			} else {
			*/
				keydata[j][0] = (long)(reset);
				keydata[j][1] = uptime;
				keydata[j][2] = type;
		//	}
			for (int k =0; k < PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE; k++) {
				data[j][k] = FoxDecoder.plainhex(Integer.parseInt(rawData[i][k+3]) & 0xff);
			}
			j++;
		}
		
		if (rawData.length > 0) {
			radTableModel.setData(keydata,data);
		}
	}
	
	protected void parseRadiationFrames() {
		if (Config.displayRawRadData) {
			parseRawBytes();
			
			scrollPane2.setVisible(false); 
			scrollPane.setVisible(true);
		} else {
			parseMiniPackets();
			
			scrollPane2.setVisible(true);
			scrollPane.setVisible(false);
		}
		
		//updateTab(Config.payloadStore.getLatestHerciHeader(foxId), true);
		MainWindow.frame.repaint();

	}
	
	
	public void updateTab(FramePart rad,boolean refreshTable) {
		if (!Config.payloadStore.initialized()) return;
		if (rad == null) return;
		hsHeader = (HerciHighspeedHeader) rad; // Cache this in case show raw is toggled
		lblHSpayload.setText("HERCI EXPERIMENT PAYLOAD: " + PayloadHERCIhighSpeed.MAX_PAYLOAD_SIZE + " bytes. Reset:" + rad.getResets() + " Uptime:" + rad.getUptime() );
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
	private void displayFramesDecoded(int u) {
		lblFramesDecoded.setText(DECODED + u);
	}

	@Override
	public void run() {
		Thread.currentThread().setName("HerciHSTab");
		running = true;
		done = false;
		int currentFrames = 0;
		boolean justStarted = true;
		while(running) {

			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HERCI thread interrupted");
				e.printStackTrace(Log.getWriter());
			}
			if (foxId != 0 && Config.payloadStore.initialized()) {
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					if (hsPayload != null) {
						parseRadiationFrames();
						updateTab(Config.payloadStore.getLatestHerciHeader(foxId), true);
					}
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatestHerciHeader(foxId), true);
				}
				int frames = Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.HERCI_HS_LAYOUT);
				if (frames != currentFrames) {
					currentFrames = frames;
					hsPayload = Config.payloadStore.getLatestHerci(foxId);
					Config.payloadStore.setUpdated(foxId, Spacecraft.HERCI_HS_LAYOUT, false);

					if (hsPayload != null) {
						parseRadiationFrames();
						updateTab(Config.payloadStore.getLatestHerciHeader(foxId), true);
					}
					
					displayFramesDecoded(frames);
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
			if (hsHeader != null) {
				updateTab(hsHeader, false);
			}
			
		}

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
	}

	
	@Override
	public void parseFrames() {
		parseRadiationFrames();
	}
	
	protected void displayRow(JTable table, int fromRow, int row) {
		long type_l = 0;
		long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
    	long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
    	
    	type_l = (long)table.getValueAt(row, HerciHSTableModel.TYPE_COL);
    	
    	Log.println("RESET: " + reset_l);
    	Log.println("UPTIME: " + uptime);
    	Log.println("TYPE: " + type_l);
    	int reset = (int)reset_l;
    	int type = (int)type_l;
    	int headerType = 0;
    	if (table.getModel() instanceof HerciHSTableModel)
    		headerType = type + 200;
    	else {
    		type = (int)(type/1000);
    		headerType = type + 200; // FIXME - these hard coded numbers should be tied back to the base types 600 / 800
    		
    	}
    	Log.println("Header Type: " + headerType);
    	this.hsPayload = (PayloadHERCIhighSpeed) Config.payloadStore.getFramePart(foxId, reset, uptime, type, Spacecraft.HERCI_HS_LAYOUT, false);
    	updateTab((HerciHighspeedHeader) Config.payloadStore.getFramePart(foxId, reset, uptime, headerType, Spacecraft.HERCI_HS_HEADER_LAYOUT, false), false);
    	
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
