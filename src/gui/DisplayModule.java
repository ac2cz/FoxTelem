package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import measure.SatMeasurementStore;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FramePart;
import telemetry.PayloadWOD;
import telemetry.RadiationPacket;

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
 *
 * A module that displays telemetry results on the screen as a table.  The table has up to four columns name, RT, MIN, MAX
 * 
 * The module is a JPanel with BoxLayout in the Y AXIS
 * It consists of a set of JPanels as rows, each with BoxLayout in the X AXIS
 * 
 * Rows are set to height ROW_HEIGHT
 * 
 * The display type indicates if it is a single type, such at RT, all three types or a vulcan module.
 * 
 * 
 * The NAME column has width NAME_WIDTH unless it is the Vulcan data then it is VULCAN_NAME_WIDTH
 * 
 *
 */
@SuppressWarnings("serial")
public class DisplayModule extends JPanel implements ActionListener, MouseListener {

	int size = 0;
	FoxSpacecraft fox;
	int foxId;
	double scale; // amount to scale the modules by given the Font size
	String[] fieldName = null;  // string used to lookup new values
	JLabel[] rtValue = null;   // the RT value displated
	JLabel[] maxValue = null;
	JComponent[] minValue = null;
	JLabel[] label = null;     // the label for the row
	JPanel[] row = null;      // a panel that stores the whole row
	int[] display;           // an array that stores how each row is displayed RT, MAX. MIN, ALL
	String title = null;
	GraphFrame[][] graph = null; // Array of graphs by plotType
	//GridBagConstraints layoutConstraints;
	String noValue = "0000";
	FramePart rtPayload;
	FramePart maxPayload;
	FramePart minPayload;
	
	private static final int DEFAULT_FONT_SIZE = 11;
//	int id = 0; // The Fox Id
	
	// These are the windows defaults
	private int VAL_WIDTH = 40;
	private int SINGLE_VAL_WIDTH = 120;
	private int VULCAN_WIDTH = 70;
	private int VULCAN_NAME_WIDTH = 140;
	private int MEASUREMENT_WIDTH = 80;
	private int MEASUREMENT_NAME_WIDTH = 140;
	private int HERCI_MICRO_PKT_NAME_WIDTH = 80;
	private int HERCI_MICRO_PKT_VALUE_WIDTH = 310;

	private int NAME_WIDTH = 110;
	private int ROW_HEIGHT = 16;

	// These are the mac defaults
	private static final int MAC_VAL_WIDTH = 44;
	private static final int MAC_SINGLE_VAL_WIDTH = 132;
	private static final int MAC_VULCAN_WIDTH = 55;
	private static final int MAC_VULCAN_NAME_WIDTH = 154;
	private static final int MAC_NAME_WIDTH = 121;
	private static final int MAC_ROW_HEIGHT = 16;
	private static final int MAC_HERCI_MICRO_PKT_NAME_WIDTH = 88;
	private static final int MAC_HERCI_MICRO_PKT_VALUE_WIDTH = 341;
	private static final int MAC_MEASUREMENT_WIDTH = 88;
	private static final int MAC_MEASUREMENT_NAME_WIDTH = 154;

	// These are the linux defaults
	private static final int LIN_VAL_WIDTH = 48;
	private static final int LIN_SINGLE_VAL_WIDTH = 144;
	private static final int LIN_VULCAN_WIDTH = 60;
	private static final int LIN_VULCAN_NAME_WIDTH = 168;
	private static final int LIN_NAME_WIDTH = 132;
	private static final int LIN_ROW_HEIGHT = 16;
	private static final int LIN_HERCI_MICRO_PKT_NAME_WIDTH = 92;
	private static final int LIN_HERCI_MICRO_PKT_VALUE_WIDTH = 382;
	private static final int LIN_MEASUREMENT_WIDTH = 96;
	private static final int LIN_MEASUREMENT_NAME_WIDTH = 168;
	
	public static final int DISPLAY_RT_ONLY = 0;
	public static final int DISPLAY_MAX_ONLY = 1;
	public static final int DISPLAY_MIN_ONLY = 2;
	public static final int DISPLAY_ALL = 3;
	public static final int DISPLAY_ALL_SWAP_MINMAX = 4;
	public static final int DISPLAY_VULCAN = 5;
	public static final int DISPLAY_LEP = 6;
	public static final int DISPLAY_LEP_EXPOSURE = 7;
	public static final int DISPLAY_WOD_VULCAN = 8;
	public static final int DISPLAY_MEASURES = 9;
	public static final int DISPLAY_PASS_MEASURES = 10;
	public static final int DISPLAY_UW = 11;
	public static final int DISPLAY_WOD_UW = 12;
	public static final int DISPLAY_MIN_AND_MAX_ONLY = 15;
	public static final int DISPLAY_WOD = 16;
	public static final int DISPLAY_HERCI = 20;
	public static final int DISPLAY_HERCI_HK = 21;
	public static final int DISPLAY_HERCI_MICRO_PKT = 22;
	
	public static Color vulcanFontColor = new Color(153,0,0);
	public static Color herciFontColor = new Color(240,154,21);
	public static Color wodFontColor = new Color(70,146,32);
	
	int moduleType = DISPLAY_ALL; // default this to a module that displays normal RT MAX MIN telem
	BitArrayLayout telemLayout = null;
	
	/**
	 * Create a new module and set the title.  Initialize the name and value arrays.
	 * Create the GUI
	 * 
	 * @param title
	 * @param size
	 */
	public DisplayModule(FoxSpacecraft fox2, String title, int size, int modType) {
		fox = fox2;
		foxId = fox.foxId;
		this.size = size;
		this.title = title;
		TitledBorder border = new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		scale = (double)(Config.displayModuleFontSize)/(double)(DEFAULT_FONT_SIZE);	
		//Log.println("SCALE: " + scale + " font:" +Config.displayModuleFontSize + " def: " + DEFAULT_FONT_SIZE);
		setDefaultSizes();
		moduleType = modType;
				
		if (moduleType == DISPLAY_HERCI || moduleType == DISPLAY_HERCI_HK || moduleType == DISPLAY_HERCI_MICRO_PKT) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/11)));
			border.setTitleColor(herciFontColor);
		} else if (moduleType == DISPLAY_MEASURES) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/10)));
			border.setTitleColor(vulcanFontColor);
			minValue = new JButton[size];
		} else if (moduleType >= DISPLAY_WOD) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/10)));
			border.setTitleColor(wodFontColor);
			minValue = new JButton[size];
		} else if (moduleType >= DISPLAY_VULCAN) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/10)));
			border.setTitleColor(vulcanFontColor);
		} else {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/11)));	
			border.setTitleColor(Color.BLUE);
			maxValue = new JLabel[size];
			minValue = new JLabel[size];

		}
		this.setBorder(border);
		
		fieldName = new String[size]; 
		rtValue = new JLabel[size];
		label = new JLabel[size];
		row = new JPanel[size];
		graph = new GraphFrame[GraphFrame.MAX_PLOT_TYPES][size];
		display = new int[size];
		initGui();
		
	}
	
	public void setLayout(BitArrayLayout lay) {telemLayout = lay; }
	public BitArrayLayout getTelemLayout() { return telemLayout; }
	
	public void addName(int i, String name, String fieldName, int display) {
		String desc = null;
		this.addName(i, name, fieldName, desc, display);
	}
	/**
	 * Add a row to the module with "name" as the text to display on the screen.  This is linked to "fieldName" which
	 * is the string used to lookup new values in the telemetry record
	 * @param i - the row 
	 * @param name
	 * @param fieldName
	 * @param display - value that determines how the row is written
	 */
	public void addName(int i, String name, String fieldName, String desc, int display) {
		this.label[i].setText(name);
		if (desc != null) row[i].setToolTipText(desc + " | click for graph | right-click for EarthPlot");
		this.label[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		this.fieldName[i] = fieldName;
		this.display[i] = display;
		
		int w = SINGLE_VAL_WIDTH; // default e.g. for passmeasures
		if (display == DISPLAY_RT_ONLY)  {
			w = SINGLE_VAL_WIDTH;			
		} else 	if (display == DISPLAY_MAX_ONLY || display == DISPLAY_MIN_ONLY || display == DISPLAY_MIN_AND_MAX_ONLY) {
			w = 0;
		} else if (display == DISPLAY_MEASURES  ) {
			w= MEASUREMENT_WIDTH;
		} else if (display == DISPLAY_ALL || display == DISPLAY_ALL_SWAP_MINMAX ) {
			w= VAL_WIDTH;
		} else if ( display >= DISPLAY_VULCAN && display < DISPLAY_HERCI_MICRO_PKT ) {
			w = VULCAN_WIDTH;
		} else if ( display == DISPLAY_HERCI || display == DISPLAY_HERCI_HK || display == DISPLAY_HERCI_MICRO_PKT) {
			w = HERCI_MICRO_PKT_NAME_WIDTH;
			// Fix the name size as this is tied to module type and not line type
			// FIXME - ideally this would flow through and not be "fixed" here
			label[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			label[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			label[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			w = HERCI_MICRO_PKT_VALUE_WIDTH;
		}

		rtValue[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
		rtValue[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
		rtValue[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height

		if (display < DISPLAY_VULCAN && minValue != null && maxValue != null) {

			w = 0;
			if (display == DISPLAY_MIN_ONLY || display == DISPLAY_MIN_AND_MAX_ONLY) {
				w = SINGLE_VAL_WIDTH;
			} else 	if (display == DISPLAY_MAX_ONLY) {
				w = 0;
			} else if (display == DISPLAY_ALL || display == DISPLAY_ALL_SWAP_MINMAX) {
				w= VAL_WIDTH;
			}
			minValue[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			minValue[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			minValue[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			w = 0;
			if (display == DISPLAY_MAX_ONLY || display == DISPLAY_MIN_AND_MAX_ONLY) {
				w = SINGLE_VAL_WIDTH;
			} else 	if (display == DISPLAY_MIN_ONLY) {
				w = 0;
			} else if (display == DISPLAY_ALL || display == DISPLAY_ALL_SWAP_MINMAX) {
				w= VAL_WIDTH;
			}
			maxValue[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			maxValue[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			maxValue[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
		}

	}


	public void updateRtValues(FramePart rt) {
		rtPayload = rt;
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				rtValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (rt.hasFieldName(fieldName[i])) { 
					if (Config.displayRawValues)
						rtValue[i].setText(Integer.toString(rt.getRawValue(fieldName[i])));
					else
						rtValue[i].setText(rt.getStringValue(fieldName[i],fox));
					for (int p=0; p < GraphFrame.MAX_PLOT_TYPES; p++)
						if (graph[p][i] != null) graph[p][i].updateGraphData("DisplayModule.updateRtValues");
				}
			}
		}
	}
	
	public void updateMaxValues(FramePart maxPayload2) {
		maxPayload = maxPayload2;
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				maxValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (maxPayload2.hasFieldName(fieldName[i])) { 
					if (Config.displayRawValues) {
						if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
							rtValue[i].setText(Integer.toString(maxPayload2.getRawValue(fieldName[i])));
						else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
							((JLabel)minValue[i]).setText(Integer.toString(maxPayload2.getRawValue(fieldName[i])));
						else
							maxValue[i].setText(Integer.toString(maxPayload2.getRawValue(fieldName[i])));
					} else if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
						rtValue[i].setText(maxPayload2.getStringValue(fieldName[i],fox));
					else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
						((JLabel)minValue[i]).setText(maxPayload2.getStringValue(fieldName[i],fox));
					else
						maxValue[i].setText(maxPayload2.getStringValue(fieldName[i],fox));
					for (int p=0; p < GraphFrame.MAX_PLOT_TYPES; p++)
					if (display[i] == DISPLAY_RT_ONLY && graph[p][i] != null) graph[p][i].updateGraphData("DisplayModule.updateMaxValues");
				}
			}
		}
	}
	
	public void updateMinValues(FramePart minPayload2) {
		minPayload = minPayload2;
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				minValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (minPayload2.hasFieldName(fieldName[i])) {
					if (Config.displayRawValues) {
						if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
							rtValue[i].setText(Integer.toString(minPayload2.getRawValue(fieldName[i])));
						else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
							maxValue[i].setText(Integer.toString(minPayload2.getRawValue(fieldName[i])));
						else
							((JLabel)minValue[i]).setText(Integer.toString(minPayload2.getRawValue(fieldName[i])));
					} else if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
						rtValue[i].setText(minPayload2.getStringValue(fieldName[i],fox));
					else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
						maxValue[i].setText(minPayload2.getStringValue(fieldName[i],fox));
					else
						((JLabel)minValue[i]).setText(minPayload2.getStringValue(fieldName[i],fox));
					for (int p=0; p < GraphFrame.MAX_PLOT_TYPES; p++)
						if (display[i] == DISPLAY_RT_ONLY && graph[p][i] != null) graph[p][i].updateGraphData("DisplayModule.updateMinValues");
				}
			}
		}
	}

	public void updateVulcanValues(RadiationPacket rad) {
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				rtValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (Config.displayRawValues)
					rtValue[i].setText(Integer.toString(rad.getRawValue(fieldName[i])));
				else
					rtValue[i].setText(rad.getStringValue(fieldName[i]));
				//if (graph[i] != null) graph[i].updateGraphData();
			}
		}
	}

	public void updateSingleValue(int line, String value) {
		rtValue[line].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		rtValue[line].setText(value);
		for (int p=0; p < GraphFrame.MAX_PLOT_TYPES; p++)
		if (graph[p][line] != null) graph[p][line].updateGraphData("DisplayModule.updateSingleValue");
	}
	
	private void initGui() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		for (int i=0; i < size; i++) {
			JPanel r = new JPanel();
			r.setLayout(new BorderLayout());
			row[i] = new JPanel();
			row[i].setLayout(new BoxLayout(row[i], BoxLayout.X_AXIS));
			r.add(row[i], BorderLayout.CENTER);
			add(r);
			row[i].addMouseListener(this);
			row[i].setBackground(Color.lightGray);
			
		}
		
		// Setup the width for the NAME column
		int w = NAME_WIDTH;
		if ( moduleType == DISPLAY_MEASURES || moduleType == DISPLAY_WOD) {
			w = MEASUREMENT_NAME_WIDTH;
		} else if ( moduleType >= DISPLAY_VULCAN && moduleType < DISPLAY_HERCI_MICRO_PKT ) {
			w = VULCAN_NAME_WIDTH;
		} else if (moduleType == DISPLAY_HERCI || moduleType == DISPLAY_HERCI_HK || moduleType == DISPLAY_HERCI_MICRO_PKT) {
			w = HERCI_MICRO_PKT_NAME_WIDTH;
		}

		for (int i=0; i < size; i++) {
			label[i] = new JLabel();
			label[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			label[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			label[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			row[i].add(label[i]);
		}

		// Setup the width for the Value Column
		w = VAL_WIDTH;
		if ( moduleType == DISPLAY_MEASURES || moduleType == DISPLAY_WOD) {
			w = MEASUREMENT_WIDTH;
		} else if ( moduleType >= DISPLAY_VULCAN && moduleType < DISPLAY_HERCI_MICRO_PKT ) {
			w = VULCAN_WIDTH;
		} else if ( moduleType == DISPLAY_HERCI || moduleType == DISPLAY_HERCI_HK || moduleType == DISPLAY_HERCI_MICRO_PKT) {
			w = HERCI_MICRO_PKT_VALUE_WIDTH;
		}

		// NOW ADD THE TITLE
		JLabel rtTitle;
		if (moduleType < DISPLAY_VULCAN) {
			rtTitle = new JLabel("  RT");
			rtTitle.setToolTipText("The most current realtime value of the telemetry is in this column");
		} else { 
			rtTitle = new JLabel("Last");
			rtTitle.setToolTipText("The most current value of the telemetry is in this column");
			
		}
		rtTitle.setFont(new Font("SansSerif", Font.BOLD, Config.displayModuleFontSize));	
		rtTitle.setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
		rtTitle.setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
		rtTitle.setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
		row[0].add(rtTitle);
		
		// AND THE VALUES
		for (int i=1; i < size; i++) {

			rtValue[i] = new JLabel(noValue);
			rtValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));			
			row[i].add(rtValue[i]);
		}
		
		if (moduleType == DISPLAY_MEASURES) {
			// We want to add a button for a sky plot.  This goes in the min column
			for (int i=1; i < size; i++) {
				minValue[i] = createIconButton("/images/skyPlot.png","Sky","Plot sky chart");
				minValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				row[i].add(minValue[i]);
			}
		}
		
		if (moduleType == DISPLAY_WOD) {
			// We want to add a button for a sky plot.  This goes in the min column
			for (int i=1; i < size; i++) {
				minValue[i] = new JButton();
				((JButton) minValue[i]).setMargin(new Insets(3,3,3,3));

				((JButton)minValue[i]).addActionListener(this);
				((JButton)minValue[i]).setBackground(wodFontColor);
				row[i].add(minValue[i]);
			}
		}
		
		if (moduleType < DISPLAY_VULCAN) {
			JLabel minTitle = new JLabel("MIN");
			minTitle.setToolTipText("The minimum realtime value since the min telemetry was reset is in this column");
			minTitle.setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			minTitle.setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			minTitle.setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			minTitle.setFont(new Font("SansSerif", Font.BOLD, Config.displayModuleFontSize));	
			row[0].add(minTitle);
			
			for (int i=1; i < size; i++) {
				minValue[i] = new JLabel(noValue);
				minValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				row[i].add(minValue[i]);
			}

			JLabel maxTitle = new JLabel("MAX");
			maxTitle.setToolTipText("The maximum realtime value since the max telemetry was reset is in this column");
			maxTitle.setFont(new Font("SansSerif", Font.BOLD, Config.displayModuleFontSize));	
			maxTitle.setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			maxTitle.setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			maxTitle.setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			row[0].add(maxTitle);
			
			for (int i=1; i < size; i++) {
				maxValue[i] = new JLabel(noValue);
				maxValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				row[i].add(maxValue[i]);
			}
		}
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		for (int i=1; i< size; i++) {
			if (rtValue[i].getText().equalsIgnoreCase(noValue))
				;// dont open graph
			else {
				if (moduleType == DISPLAY_MEASURES && e.getSource() == minValue[i])
					displayGraph(i, GraphFrame.SKY_PLOT);				
				else if (moduleType == DISPLAY_WOD && e.getSource() == minValue[i])
						displayGraph(i, GraphFrame.EARTH_PLOT);				
			}
		}
	}

	public void displayGraph(int i, int plotType) {
		try {
			if (graph[plotType][i] == null) {
				int conversion = BitArrayLayout.CONVERT_NONE;
				String units = "";
				
				if ((moduleType == DisplayModule.DISPLAY_ALL  || moduleType == DisplayModule.DISPLAY_WOD || moduleType == DisplayModule.DISPLAY_ALL_SWAP_MINMAX ) && rtPayload!=null && rtPayload.hasFieldName(fieldName[i])) {
					conversion = rtPayload.getConversionByName(fieldName[i]);
					units = rtPayload.getUnitsByName(fieldName[i]);
					if (rtPayload instanceof PayloadWOD)
						graph[plotType][i] = new GraphFrame("WOD: " + title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_WOD, rtPayload.getLayout(), fox, plotType);
					else
						graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_REAL_TIME, rtPayload.getLayout(), fox, plotType);
				}
				else if (moduleType == DISPLAY_PASS_MEASURES) {
					conversion = fox.passMeasurementLayout.getIntConversionByName(fieldName[i]);
					units = fox.passMeasurementLayout.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  SatMeasurementStore.PASS_MEASUREMENT_TYPE, null, fox, plotType);
				} 
				else if (moduleType == DISPLAY_MEASURES) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					conversion = fox.measurementLayout.getIntConversionByName(fieldName[i]);
					units = fox.measurementLayout.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  SatMeasurementStore.RT_MEASUREMENT_TYPE, null, fox, plotType);
				}
				else if (moduleType == DISPLAY_VULCAN  || moduleType == DisplayModule.DISPLAY_WOD_VULCAN) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					BitArrayLayout lay = fox.getLayoutByName(Spacecraft.RAD2_LAYOUT);
					conversion = lay.getIntConversionByName(fieldName[i]);
					units = lay.getUnitsByName(fieldName[i]);
					if (moduleType == DisplayModule.DISPLAY_WOD_VULCAN) {
					//	Log.errorDialog("NOT YET IMPLEMENTED", "Need to define TELEM layout for WOD RAD and pass to the graph");
						graph[plotType][i] = new GraphFrame("WOD: " + title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_WOD_RAD_TELEM_DATA, null, fox, plotType);
					} else
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_RAD_TELEM_DATA, null, fox, plotType);
				}
				else if (moduleType == DISPLAY_UW) {
					//BitArrayLayout lay = fox.getLayoutByName(Spacecraft.CAN_LAYOUT);
					conversion = telemLayout.getIntConversionByName(fieldName[i]);
					units = telemLayout.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_UW_EXPERIMENT, telemLayout, fox, plotType);
				}
				else if (moduleType == DISPLAY_WOD_UW) {
					BitArrayLayout lay = fox.getLayoutByName(Spacecraft.WOD_CAN_LAYOUT);
					conversion = lay.getIntConversionByName(fieldName[i]);
					units = lay.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_UW_WOD_EXPERIMENT, telemLayout, fox, plotType);
				}
				else if (moduleType == DISPLAY_HERCI) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					BitArrayLayout lay = fox.getLayoutByName(Spacecraft.HERCI_HS_HEADER_LAYOUT);
					conversion = lay.getIntConversionByName(fieldName[i]);
					units = lay.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_HERCI_SCIENCE_HEADER, null, fox, plotType);
				}
				else if (moduleType == DISPLAY_HERCI_HK) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					BitArrayLayout lay = fox.getLayoutByName(Spacecraft.RAD2_LAYOUT);
					conversion = lay.getIntConversionByName(fieldName[i]);
					units = lay.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_RAD_TELEM_DATA, null, fox, plotType);
				}
				else if (minPayload!=null && minPayload.hasFieldName(fieldName[i])) {
					conversion = minPayload.getConversionByName(fieldName[i]);
					units = minPayload.getUnitsByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_MIN_VALUES, null, fox, plotType);
				}
				else if (maxPayload!=null && maxPayload.hasFieldName(fieldName[i])) {
					conversion = maxPayload.getConversionByName(fieldName[i]);
					conversion = maxPayload.getConversionByName(fieldName[i]);
					graph[plotType][i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], units, conversion,  FoxFramePart.TYPE_MAX_VALUES, null, fox, plotType);
				} else return;
				
				graph[plotType][i].setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/fox.jpg")));
			}
//			graph[plotType][i].updateGraphData("DisplayModule.displayGraph");  // This is also called from new, probably don't need to call it here?
			graph[plotType][i].setVisible(true);
		} catch (Exception ex) {
			Log.println("MOUSE CLICKED EXCEPTION");
			ex.printStackTrace();
			ex.printStackTrace(Log.getWriter());
		}

	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
		for (int i=1; i< size; i++) {
			if (e.getSource() == row[i]) {
				if (rtValue[i].getText().equalsIgnoreCase(noValue)) {
					// dont open graph
				} else
					if (e.isControlDown() || e.getButton() == MouseEvent.BUTTON3)
						displayGraph(i, GraphFrame.EARTH_PLOT);
					else
						displayGraph(i, GraphFrame.GRAPH_PLOT);

			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		for (int i=1; i< size; i++) {
			if (e.getSource() == row[i]) {
				row[i].setBackground(Color.BLUE);
				if (label[i] != null)
					label[i].setForeground(Color.WHITE);
				if (rtValue[i] != null)
					rtValue[i].setForeground(Color.WHITE);
				if (maxValue != null && maxValue[i] != null)
					maxValue[i].setForeground(Color.WHITE);
				if (minValue != null && minValue[i] != null)
					minValue[i].setForeground(Color.WHITE);
			}
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		for (int i=0; i< size; i++) {
			if (e.getSource() == row[i]) {
				row[i].setBackground(Color.lightGray);
				if (label[i] != null)
					label[i].setForeground(Color.BLACK);
				if (rtValue[i] != null)
					rtValue[i].setForeground(Color.BLACK);
				if (maxValue != null && maxValue[i] != null)
					maxValue[i].setForeground(Color.BLACK);
				if (minValue != null && minValue[i] != null)
					minValue[i].setForeground(Color.BLACK);
				this.repaint();
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void closeGraphs() {
		for (int p=0; p<GraphFrame.MAX_PLOT_TYPES; p++)
			for (int i=0; i < graph[p].length; i++) {
				if (graph[p][i] != null) {
					if (graph[p][i].isVisible())
						graph[p][i].saveProperties(true);
					graph[p][i].dispose();
				}
			}
	}

	public void showGraphs() {
		for (int p=0; p<GraphFrame.MAX_PLOT_TYPES; p++)
			for (int i=0; i < graph[p].length; i++) {
				if (graph[p][i] != null) {
					if (graph[p][i].isVisible())
						graph[p][i].toFront();
				}
			}
	}

	public void openGraphs(int payloadType) {
		for (int i=0; i < fieldName.length; i++) {
			openPlot(i, GraphFrame.GRAPH_PLOT, payloadType);
			openPlot(i, GraphFrame.SKY_PLOT, payloadType);
			openPlot(i, GraphFrame.EARTH_PLOT, payloadType);
		}
	}
	
	private void openPlot(int i, int plotType, int payloadType) {
		boolean open = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[i], "open");
		if (open) {
			displayGraph(i, plotType);
		}
	}
	
	private void setDefaultSizes() {
		if (Config.isMacOs()) {
			VAL_WIDTH = MAC_VAL_WIDTH;
			SINGLE_VAL_WIDTH = MAC_SINGLE_VAL_WIDTH;
			VULCAN_WIDTH = MAC_VULCAN_WIDTH;
			VULCAN_NAME_WIDTH = MAC_VULCAN_NAME_WIDTH;
			NAME_WIDTH = MAC_NAME_WIDTH;
			ROW_HEIGHT = MAC_ROW_HEIGHT;
			HERCI_MICRO_PKT_NAME_WIDTH = MAC_HERCI_MICRO_PKT_NAME_WIDTH;
			HERCI_MICRO_PKT_VALUE_WIDTH = MAC_HERCI_MICRO_PKT_VALUE_WIDTH;
			MEASUREMENT_WIDTH = MAC_MEASUREMENT_WIDTH;
			MEASUREMENT_NAME_WIDTH = MAC_MEASUREMENT_NAME_WIDTH;
		}
		if (Config.isLinuxOs()) {
			VAL_WIDTH = LIN_VAL_WIDTH;
			SINGLE_VAL_WIDTH = LIN_SINGLE_VAL_WIDTH;
			VULCAN_WIDTH = LIN_VULCAN_WIDTH;
			VULCAN_NAME_WIDTH = LIN_VULCAN_NAME_WIDTH;
			NAME_WIDTH = LIN_NAME_WIDTH;
			ROW_HEIGHT = LIN_ROW_HEIGHT;
			HERCI_MICRO_PKT_NAME_WIDTH = LIN_HERCI_MICRO_PKT_NAME_WIDTH;
			HERCI_MICRO_PKT_VALUE_WIDTH = LIN_HERCI_MICRO_PKT_VALUE_WIDTH;
			MEASUREMENT_WIDTH = LIN_MEASUREMENT_WIDTH;
			MEASUREMENT_NAME_WIDTH = LIN_MEASUREMENT_NAME_WIDTH;
		}

//		Log.println("Using scale: " + scale);
		VAL_WIDTH = (int)(VAL_WIDTH * scale);
		SINGLE_VAL_WIDTH = (int)(SINGLE_VAL_WIDTH * scale);
		VULCAN_WIDTH = (int)(VULCAN_WIDTH * scale);
		VULCAN_NAME_WIDTH = (int)(VULCAN_NAME_WIDTH * scale);
		NAME_WIDTH = (int)(NAME_WIDTH * scale);
		MEASUREMENT_WIDTH = (int)(MEASUREMENT_WIDTH * scale);
		MEASUREMENT_NAME_WIDTH = (int)(MEASUREMENT_NAME_WIDTH * scale);
		ROW_HEIGHT = (int)(ROW_HEIGHT * scale);
		HERCI_MICRO_PKT_NAME_WIDTH = (int)(HERCI_MICRO_PKT_NAME_WIDTH * scale);
		HERCI_MICRO_PKT_VALUE_WIDTH = (int)(HERCI_MICRO_PKT_VALUE_WIDTH * scale);
	}
	
	public JButton createIconButton(String icon, String name, String toolTip) {
		JButton btn;
		BufferedImage wPic = null;
		try {
			wPic = ImageIO.read(this.getClass().getResource(icon));
		} catch (IOException e) {
			e.printStackTrace(Log.getWriter());
		}
		if (wPic != null) {
			btn = new JButton(new ImageIcon(wPic));
			btn.setMargin(new Insets(0,0,0,0));
		} else {
			btn = new JButton(name);	
		}
		btn.setToolTipText(toolTip);
		
		btn.addActionListener(this);
		return btn;
	}
}
