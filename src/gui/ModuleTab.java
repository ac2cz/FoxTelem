package gui;

import java.awt.Color;

import javax.swing.JPanel;

import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;

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
public class ModuleTab extends FoxTelemTab {

	Spacecraft fox;
	int foxId = 0;

	DisplayModule[] topModules;
	DisplayModule[] bottomModules;

	JPanel topHalf;
	JPanel bottomHalf;

	protected void initDisplayHalves(JPanel centerPanel) {
		topHalf = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars1.png");
		topHalf.setBackground(Color.DARK_GRAY);
		//topHalf.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		bottomHalf = new JPanel(); //new ImagePanel("C:/Users/chris.e.thompson/Desktop/workspace/SALVAGE/data/stars5.png");
		//bottomHalf.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		bottomHalf.setBackground(Color.DARK_GRAY);
		centerPanel.add(topHalf);
		centerPanel.add(bottomHalf);
	}
	
	/**
	 * Analyze the layouts that have been loaded and determine the list of modules and lines that should be used in 
	 * the display
	 * @throws LayoutLoadException 
	 */
	protected void analyzeModules(BitArrayLayout rt, BitArrayLayout max, BitArrayLayout min, int moduleType) throws LayoutLoadException {
		String[] topModuleNames = new String[10];
		int[] topModuleLines = new int[10];
		String[] bottomModuleNames = new String[10];
		int[] bottomModuleLines = new int[10];
		int numOfTopModules = 1;
		int numOfBottomModules = 1;
		// First get a quick list of all the modules names and sort them into top/bottom
		for (int i=0; i<rt.NUMBER_OF_FIELDS; i++) {
			if (!rt.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (rt.moduleNum[i] > 0 && rt.moduleNum[i] < 10) {
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
		if (max != null)
		for (int i=0; i<max.NUMBER_OF_FIELDS; i++) {
			if (!max.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				if (max.moduleNum[i] > 0 && max.moduleNum[i] < 10) {
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
				if (min.moduleNum[i] > 0 && min.moduleNum[i] < 10) {
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

		topModules = new DisplayModule[numOfTopModules];
		bottomModules = new DisplayModule[numOfBottomModules];
		
		// Process the top Modules - which run from 1 to 9
		for (int i=1; i < numOfTopModules; i++) {
			topModules[i] = new DisplayModule(fox, topModuleNames[i], topModuleLines[i]+1, moduleType);
			addModuleLines(topModules[i], topModuleNames[i], topModuleLines[i], rt);
			if (max != null) addModuleLines(topModules[i], topModuleNames[i], topModuleLines[i], max);
			if (min != null) addModuleLines(topModules[i], topModuleNames[i], topModuleLines[i], min);
			/*
			for (int j=0; j<max.NUMBER_OF_FIELDS; j++) {
				if (max.module[j].equals(topModuleNames[i])) {
					topModules[i].addName(max.moduleLinePosition[j], max.shortName[j] + formatUnits(max.fieldUnits[j]), max.fieldName[j], max.description[j], max.moduleDisplayType[j]);					
				}
			}
			for (int j=0; j<min.NUMBER_OF_FIELDS; j++) {
				if (min.module[j].equals(topModuleNames[i])) {
					topModules[i].addName(min.moduleLinePosition[j], min.shortName[j] + formatUnits(min.fieldUnits[j]), min.fieldName[j], min.description[j], min.moduleDisplayType[j]);					
				}

			}
			*/
			topHalf.add(topModules[i]);
		}

		// Process the bottom Modules - which run from 10 to 19
		for (int i=1; i < numOfBottomModules; i++) {
			bottomModules[i] = new DisplayModule(fox, bottomModuleNames[i], bottomModuleLines[i]+1, moduleType);
			addModuleLines(bottomModules[i], bottomModuleNames[i], bottomModuleLines[i], rt);
			if (max != null) addModuleLines(bottomModules[i], bottomModuleNames[i], bottomModuleLines[i], max);
			if (min != null) addModuleLines(bottomModules[i], bottomModuleNames[i], bottomModuleLines[i], min);
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
				displayModule.addName(rt.moduleLinePosition[j], rt.shortName[j] + formatUnits(rt.fieldUnits[j]), rt.fieldName[j], rt.description[j], rt.moduleDisplayType[j]);					
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
	
	public void openGraphs() {
		if (topModules != null)
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.openGraphs();
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.openGraphs();
		}
	
	}
}
