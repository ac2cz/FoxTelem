package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

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
public abstract class RadiationTab extends ModuleTab  {

	JPanel topPanel;
	JPanel centerPanel;
	PayloadRadExpData radPayload;

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
	
	/*
	protected void addBottomFilter() {
		JLabel displayNumber1 = new JLabel("Displaying last");
		displayNumber2 = new JTextField();
		JLabel displayNumber3 = new JLabel("payloads decoded");
		displayNumber1.setFont(new Font("SansSerif", Font.BOLD, 10));
		displayNumber3.setFont(new Font("SansSerif", Font.BOLD, 10));
		displayNumber1.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber3.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber2.setMinimumSize(new Dimension(50, 14));
		displayNumber2.setMaximumSize(new Dimension(50, 14));
		displayNumber2.setText(Integer.toString(SAMPLES));
		displayNumber2.addActionListener(this);
		bottomPanel.add(displayNumber1);
		bottomPanel.add(displayNumber2);
		bottomPanel.add(displayNumber3);
		
	}
	*/
	
	
	protected abstract void parseRadiationFrames();
	
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
	
	

	

}
