package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import common.Config;
import telemetry.PayloadRadExpData;
import telemetry.SatPayloadStore;

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
public abstract class RadiationTab extends ModuleTab implements MouseListener {

	JPanel topPanel;
	JPanel centerPanel;
	PayloadRadExpData radPayload;
	JTable table;
	JTable packetTable;
	JScrollPane packetScrollPane;
	JScrollPane scrollPane;
	JCheckBox showRawBytes;
	
	int splitPaneHeight = 0;
	JSplitPane splitPane;
	
	RadiationTab() {
		setLayout(new BorderLayout(0, 0));
		
		topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(34, 250));
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		
		bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
	}
	
	protected void addTables(AbstractTableModel radTableModel, AbstractTableModel radPacketTableModel ) {
		
		
		table = new JTable(radTableModel);
		table.setAutoCreateRowSorter(true);
		table.addMouseListener(this);
		
		
		packetTable = new JTable(radPacketTableModel);
		packetTable.setAutoCreateRowSorter(true);
		
		//JScrollPane scrollPane = new JScrollPane(table);
		scrollPane = new JScrollPane (table, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		String PREV = "prev";
		String NEXT = "next";
		InputMap inMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMap = table.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = table.getSelectedRow();
				if (row > 0)
					displayRow(table,row-1);
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = table.getSelectedRow();
				if (row < table.getRowCount()-1)
					displayRow(table,row+1);        
			}
		});
		//table.setMinimumSize(new Dimension(6200, 6000));
		centerPanel.add(scrollPane);

		packetScrollPane = new JScrollPane (packetTable, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		packetTable.setFillsViewportHeight(true);
		packetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//table.setMinimumSize(new Dimension(6200, 6000));
		centerPanel.add(packetScrollPane);

		packetTable.addMouseListener(this);
		
		InputMap packetinMap = packetTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		packetinMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		packetinMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap packetactMap = packetTable.getActionMap();

		packetactMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = packetTable.getSelectedRow();
				if (row > 0)
					displayRow(packetTable, row-1);
			}
		});
		packetactMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = packetTable.getSelectedRow();
				if (row < packetTable.getRowCount()-1)
					displayRow(packetTable, row+1);        
			}
		});
		
	}
	
	protected abstract void displayRow(JTable packetTable, int row); // When we click on a row in the table we call this function to update the top part of the display
	protected abstract void parseRadiationFrames(); // When we get new data we call this function to display it
	
	protected void parseRawBytes(String data[][], RadiationTableModel radTableModel) {
		long[][] keyRawData = new long[data.length][2];
		String[][] rawData = new String[data.length][SatPayloadStore.MAX_RAD_DATA_LENGTH-2];
		for (int i=0; i<data.length; i++)
			for (int k=0; k<SatPayloadStore.MAX_RAD_DATA_LENGTH; k++)
				try {
					if (k<=1)
						keyRawData[i][k] = Long.parseLong(data[data.length-i-1][k]);
					else
						rawData[i][k-2] = Integer.toHexString(Integer.valueOf(data[data.length-i-1][k]));
				} catch (NumberFormatException e) {

				}
		radTableModel.setData(keyRawData, rawData);
		

	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		super.itemStateChanged(e);
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
	}
	public void mouseClicked(MouseEvent e) {

		if (showRawBytes.isSelected()) {
			int row = table.rowAtPoint(e.getPoint());
			int col = table.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				//Log.println("CLICKED ROW: "+row+ " and COL: " + col);
				displayRow(table, row);
			}
		} else {
			int row = packetTable.rowAtPoint(e.getPoint());
			int col = packetTable.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				//Log.println("CLICKED ROW: "+row+ " and COL: " + col);
				displayRow(packetTable, row);
			}
		}
	}

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
