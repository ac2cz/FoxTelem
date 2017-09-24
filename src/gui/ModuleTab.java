package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.PayloadWOD;

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
 * This is a tab on the main window that contains Display Modules, as defined in a Layout file.
 */
@SuppressWarnings("serial")
public abstract class ModuleTab extends FoxTelemTab implements ActionListener {

	public int SAMPLES = 100;
	public int MAX_SAMPLES = 9999;
	public int MIN_SAMPLES = 1;
	public long START_UPTIME = 0;
	public int START_RESET = 0;
	
	JLabel lblFromReset;
	JTextField textFromReset;
	JLabel lblFromUptime;
	JTextField textFromUptime;
	JTextField displayNumber2;
	
	public static final int DEFAULT_START_RESET = 0;
	public static final int DEFAULT_START_UPTIME = 0;
	
	Spacecraft fox;
	int foxId = 0;

	DisplayModule[] topModules;
	DisplayModule[] bottomModules;

	JPanel topHalf;
	JPanel bottomHalf;
	JScrollPane scrollPane;
	JPanel bottomPanel;

	protected void addBottomFilter() {
		bottomPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(500,10), new Dimension(1500,10)));
		JLabel displayNumber1 = new JLabel("Displaying last");
		displayNumber2 = new JTextField();
		JLabel displayNumber3 = new JLabel("payloads decoded");
		displayNumber1.setFont(new Font("SansSerif", Font.PLAIN, 10));
		displayNumber3.setFont(new Font("SansSerif", Font.PLAIN, 10));
		displayNumber1.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber3.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		displayNumber2.setMinimumSize(new Dimension(50, 14));
		displayNumber2.setMaximumSize(new Dimension(50, 14));
		displayNumber2.setText(Integer.toString(SAMPLES));
		displayNumber2.addActionListener(this);
		bottomPanel.add(displayNumber1);
		bottomPanel.add(displayNumber2);
		bottomPanel.add(displayNumber3);
		
		lblFromReset = new JLabel("   from Reset  ");
		lblFromReset.setFont(new Font("SansSerif", Font.PLAIN, 10));
		bottomPanel.add(lblFromReset);
		
		textFromReset = new JTextField();
		textFromReset.setMinimumSize(new Dimension(40, 14));
		textFromReset.setMaximumSize(new Dimension(40, 14));
		bottomPanel.add(textFromReset);
		textFromReset.setText(Integer.toString(START_RESET));

		textFromReset.setColumns(8);
		textFromReset.addActionListener(this);
		
		lblFromUptime = new JLabel("   from Uptime  ");
		lblFromUptime.setFont(new Font("SansSerif", Font.PLAIN, 10));
		bottomPanel.add(lblFromUptime);
		
		textFromUptime = new JTextField();
		textFromUptime.setMinimumSize(new Dimension(70, 14));
		textFromUptime.setMaximumSize(new Dimension(70, 14));
		bottomPanel.add(textFromUptime);

		textFromUptime.setText(Long.toString(START_UPTIME));
		textFromUptime.setColumns(8);
//		textFromUptime.setPreferredSize(new Dimension(50,14));
		textFromUptime.addActionListener(this);

		
	}
	protected void initDisplayHalves(JPanel centerPanel) {
		topHalf = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars1.png");
		topHalf.setBackground(Color.DARK_GRAY);
		//topHalf.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		centerPanel.add(topHalf);
		//JScrollPane scrollPane = new JScrollPane(table);
		//scrollPane = new JScrollPane (topHalf, 
		//		   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		//centerPanel.add(scrollPane);
		
//		if (bottomModules != null) {
//			bottomHalf = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars5.png");
//			//bottomHalf.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
//			bottomHalf.setBackground(Color.DARK_GRAY);
//			centerPanel.add(bottomHalf);
//		}
	}
	
	/**
	 * Analyze the layouts that have been loaded and determine the list of modules and lines that should be used in 
	 * the display
	 * @throws LayoutLoadException 
	 */
	protected void analyzeModules(BitArrayLayout rt, BitArrayLayout max, BitArrayLayout min, int moduleType) throws LayoutLoadException {
		String[] topModuleNames = new String[20];
		int[] topModuleLines = new int[20];
		String[] bottomModuleNames = new String[10];
		int[] bottomModuleLines = new int[10];
		int numOfTopModules = 1;
		int numOfBottomModules = 0;
		// First get a quick list of all the modules names and sort them into top/bottom
		for (int i=0; i<rt.NUMBER_OF_FIELDS; i++) {
			if (!rt.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (true || rt.moduleNum[i] > 0 && rt.moduleNum[i] < 10) {
					if (!containedIn(topModuleNames, rt.module[i])) {
						topModuleNames[rt.moduleNum[i]] = rt.module[i];
						numOfTopModules++;
					}
					topModuleLines[rt.moduleNum[i]]++;
				} else if (rt.moduleNum[i] >= 10 && rt.moduleNum[i] < 20) {
					if (!containedIn(bottomModuleNames,rt.module[i])) {
						bottomModuleNames[rt.moduleNum[i]-9] = rt.module[i];
						numOfBottomModules++;
					}
					bottomModuleLines[rt.moduleNum[i]-9]++;		
				}
			}
		}
		if (moduleType == DisplayModule.DISPLAY_WOD) {
			; //System.out.println("STOP");
		} else {
		if (max != null)
		for (int i=0; i<max.NUMBER_OF_FIELDS; i++) {
			if (!max.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (true || max.moduleNum[i] > 0 && max.moduleNum[i] < 10) {
					if (!containedIn(topModuleNames, max.module[i])) {
						topModuleNames[max.moduleNum[i]] = max.module[i];
						numOfTopModules++;
					}
					topModuleLines[max.moduleNum[i]]++;
				} else if (max.moduleNum[i] >= 10 && max.moduleNum[i] < 20) {
					if (!containedIn(bottomModuleNames,max.module[i])) {
						bottomModuleNames[max.moduleNum[i]-9] = max.module[i];
						numOfBottomModules++;
					}
					bottomModuleLines[max.moduleNum[i]-9]++;		
				}
			}
		}
		if (min != null)
		for (int i=0; i<min.NUMBER_OF_FIELDS; i++) {
			if (!min.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (true || min.moduleNum[i] > 0 && min.moduleNum[i] < 10) {
					if (!containedIn(topModuleNames, min.module[i])) {
						topModuleNames[min.moduleNum[i]] = min.module[i];
						numOfTopModules++;
					}
					topModuleLines[min.moduleNum[i]]++;
				} else if (min.moduleNum[i] >= 10 && min.moduleNum[i] < 20) {
					if (!containedIn(bottomModuleNames,min.module[i])) {
						bottomModuleNames[min.moduleNum[i]-9] = min.module[i];
						numOfBottomModules++;
					}
					bottomModuleLines[min.moduleNum[i]-9]++;		
				}
			}

		}
		}
		topModules = new DisplayModule[numOfTopModules];
		if (numOfBottomModules > 0)
		bottomModules = new DisplayModule[numOfBottomModules];
		
		// Process the top Modules - which run from 1 to 9
		for (int i=1; i < numOfTopModules; i++) {
			topModules[i] = new DisplayModule(fox, topModuleNames[i], topModuleLines[i]+1, moduleType);
			addModuleLines(topModules[i], topModuleNames[i], topModuleLines[i], rt);
			if (moduleType != DisplayModule.DISPLAY_WOD) {
				if (max != null) addModuleLines(topModules[i], topModuleNames[i], topModuleLines[i], max);
				if (min != null) addModuleLines(topModules[i], topModuleNames[i], topModuleLines[i], min);
			}
			topHalf.add(topModules[i]);
		}

		// Process the bottom Modules - which run from 10 to 19
		for (int i=1; i < numOfBottomModules; i++) {
			bottomModules[i] = new DisplayModule(fox, bottomModuleNames[i], bottomModuleLines[i]+1, moduleType);
			addModuleLines(bottomModules[i], bottomModuleNames[i], bottomModuleLines[i], rt);
			if (moduleType != DisplayModule.DISPLAY_WOD) {
				if (max != null) addModuleLines(bottomModules[i], bottomModuleNames[i], bottomModuleLines[i], max);
				if (min != null) addModuleLines(bottomModules[i], bottomModuleNames[i], bottomModuleLines[i], min);
			}
			bottomHalf.add(bottomModules[i]);
		}
		
	}

	private void addModuleLines(DisplayModule displayModule, String topModuleName, int topModuleLine, BitArrayLayout rt) throws LayoutLoadException {
		for (int j=0; j<rt.NUMBER_OF_FIELDS; j++) {
			if (rt.module[j].equals(topModuleName)) {
				//Log.println("Adding:" + rt.shortName[j]);
				if (rt.moduleLinePosition[j] > topModuleLine) throw new LayoutLoadException("Found error in Layout File: "+ rt.fileName +
				".\nModule: " + topModuleName +
						" has " + topModuleLine + " lines, so we can not add " + rt.shortName[j] + " on line " + rt.moduleLinePosition[j]);
				try {
					if (rt.name.equals(Spacecraft.WOD_LAYOUT)) rt.moduleDisplayType[j] = DisplayModule.DISPLAY_WOD;
					displayModule.addName(rt.moduleLinePosition[j], rt.shortName[j] + formatUnits(rt.fieldUnits[j]), rt.fieldName[j], rt.description[j], rt.moduleDisplayType[j]);					
				} catch (NullPointerException e) {
					throw new LayoutLoadException("Found NULL item error in Layout File: "+ rt.fileName +
							".\nModule: " + topModuleName +
									" has " + topModuleLine + " lines, but error adding " + rt.shortName[j] + " on line " + rt.moduleLinePosition[j]);
				}
				}
		}

	}
	
	private String formatUnits(String unit) {
		if (unit.equals("-") || unit.equalsIgnoreCase(BitArrayLayout.NONE)) return "";
		unit = " ("+unit+")";
		return unit;
				
	}

	private boolean containedIn(String[] array, String item) {
		for(String s : array) {
			if (s!=null)
				if (s.equals(item)) return true;
		}
		return false;
	}

	public void showGraphs() {
		if (topModules != null)
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.showGraphs();
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.showGraphs();
		}
	
	}

	
	public void closeGraphs() {
		if (topModules != null)
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.closeGraphs();
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.closeGraphs();
		}
	
	}
	
	public void openGraphs(int payloadType) {
		if (topModules != null)
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.openGraphs(payloadType);
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.openGraphs(payloadType);
		}
	
	}
	
	private void parseTextFields() {
		String text = displayNumber2.getText();
		try {
			SAMPLES = Integer.parseInt(text);
			if (SAMPLES > MAX_SAMPLES) {
				SAMPLES = MAX_SAMPLES;
				text = Integer.toString(MAX_SAMPLES);
			}
			if (SAMPLES < MIN_SAMPLES) {
				SAMPLES = MIN_SAMPLES;
				text = Integer.toString(MIN_SAMPLES);
			}
		} catch (NumberFormatException ex) {
			
		}
		displayNumber2.setText(text);
		text = textFromReset.getText();
		try {
			START_RESET = Integer.parseInt(text);
			if (START_RESET < 0) START_RESET = 0;
			
		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				START_RESET = DEFAULT_START_RESET;
				
			}
		}
		textFromReset.setText(text);
		
		text = textFromUptime.getText();
		try {
			START_UPTIME = Integer.parseInt(text);
			if (START_UPTIME < 0) START_UPTIME = 0;
			
		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				START_UPTIME = DEFAULT_START_UPTIME;
				
			}
		}
		textFromUptime.setText(text);

		repaint();
	}
	
	public abstract void parseFrames();
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.displayNumber2 ||
				e.getSource() == this.textFromReset ||
				e.getSource() == this.textFromUptime) {
			try {
				parseTextFields();
				//System.out.println(SAMPLES);
				
				//lblActual.setText("("+text+")");
				//txtPeriod.setText("");
			} catch (NumberFormatException ex) {
				
			}
			
			parseFrames();
		}

	}
}
