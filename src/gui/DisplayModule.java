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
import common.Log;
import common.Spacecraft;
import measure.SatMeasurementStore;
import telemetry.BitArray;
import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
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
	Spacecraft fox;
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
	GraphFrame[] graph = null;
	//GridBagConstraints layoutConstraints;
	String noValue = "0000";
	BitArray rtPayload;
	PayloadMaxValues maxPayload;
	PayloadMinValues minPayload;
	
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
	public static final int DISPLAY_VULCAN_EXP = 8;
	public static final int DISPLAY_MEASURES = 9;
	public static final int DISPLAY_PASS_MEASURES = 10;
	public static final int DISPLAY_MIN_AND_MAX_ONLY = 15;
	public static final int DISPLAY_HERCI = 20;
	public static final int DISPLAY_HERCI_HK = 21;
	public static final int DISPLAY_HERCI_MICRO_PKT = 22;
	
	public static Color vulcanFontColor = new Color(153,0,0);
	public static Color herciFontColor = new Color(240,154,21);
	//public static Color herciFontColor = new Color(244,193,4);
	
	int moduleType = DISPLAY_ALL; // default this to a module that displays normal RT MAX MIN telem

	/**
	 * Create a new module and set the title.  Initialize the name and value arrays.
	 * Create the GUI
	 * 
	 * @param title
	 * @param size
	 */
	public DisplayModule(Spacecraft sat, String title, int size, int modType) {
		fox = sat;
		foxId = fox.foxId;
		this.size = size;
		this.title = title;
		TitledBorder border = new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		scale = (double)(Config.displayModuleFontSize)/(double)(DEFAULT_FONT_SIZE);	
		//Log.println("SCALE: " + scale + " font:" +Config.displayModuleFontSize + " def: " + DEFAULT_FONT_SIZE);
		setDefaultSizes();
		moduleType = modType;
		
		if (moduleType >= DISPLAY_HERCI) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/11)));
			border.setTitleColor(herciFontColor);
		} else if (moduleType == DISPLAY_MEASURES) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 12/10)));
			border.setTitleColor(vulcanFontColor);
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
		graph = new GraphFrame[size];
		display = new int[size];
		initGui();
		
	}
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
		if (desc != null) row[i].setToolTipText(desc);
		this.label[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
		this.fieldName[i] = fieldName;
		this.display[i] = display;
		
		int w = SINGLE_VAL_WIDTH; // default e.g. for passmeasures
		if (display == DISPLAY_RT_ONLY)  {
			w = SINGLE_VAL_WIDTH;			
		} else 	if (display == DISPLAY_MAX_ONLY || display == DISPLAY_MIN_ONLY || display == DISPLAY_MIN_AND_MAX_ONLY) {
			w = 0;
		} else if (display == DISPLAY_MEASURES ) {
			w= MEASUREMENT_WIDTH;
		} else if (display == DISPLAY_ALL || display == DISPLAY_ALL_SWAP_MINMAX ) {
			w= VAL_WIDTH;
		} else if ( display >= DISPLAY_VULCAN && display < DISPLAY_HERCI_MICRO_PKT ) {
			w = VULCAN_WIDTH;
		} else if ( display >= DISPLAY_HERCI_MICRO_PKT) {
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


	public void updateRtValues(BitArray rt) {
		rtPayload = rt;
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				rtValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (rt.hasFieldName(fieldName[i])) { 
					if (Config.displayRawValues)
						rtValue[i].setText(Integer.toString(rt.getRawValue(fieldName[i])));
					else
						rtValue[i].setText(rt.getStringValue(fieldName[i],fox));
					if (graph[i] != null) graph[i].updateGraphData("DisplayModule.updateRtValues");
				}
			}
		}
	}
	
	public void updateMaxValues(PayloadMaxValues max) {
		maxPayload = max;
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				maxValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (max.hasFieldName(fieldName[i])) { 
					if (Config.displayRawValues) {
						if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
							rtValue[i].setText(Integer.toString(max.getRawValue(fieldName[i])));
						else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
							((JLabel)minValue[i]).setText(Integer.toString(max.getRawValue(fieldName[i])));
						else
							maxValue[i].setText(Integer.toString(max.getRawValue(fieldName[i])));
					} else if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
						rtValue[i].setText(max.getStringValue(fieldName[i],fox));
					else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
						((JLabel)minValue[i]).setText(max.getStringValue(fieldName[i],fox));
					else
						maxValue[i].setText(max.getStringValue(fieldName[i],fox));
					if (display[i] == DISPLAY_RT_ONLY && graph[i] != null) graph[i].updateGraphData("DisplayModule.updateMaxValues");
				}
			}
		}
	}
	
	public void updateMinValues(PayloadMinValues min) {
		minPayload = min;
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				minValue[i].setFont(new Font("SansSerif", Font.PLAIN, Config.displayModuleFontSize));
				if (min.hasFieldName(fieldName[i])) {
					if (Config.displayRawValues) {
						if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
							rtValue[i].setText(Integer.toString(min.getRawValue(fieldName[i])));
						else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
							maxValue[i].setText(Integer.toString(min.getRawValue(fieldName[i])));
						else
							((JLabel)minValue[i]).setText(Integer.toString(min.getRawValue(fieldName[i])));
					} else if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
						rtValue[i].setText(min.getStringValue(fieldName[i],fox));
					else if (display[i] == DISPLAY_ALL_SWAP_MINMAX) // we put the max in the min column
						maxValue[i].setText(min.getStringValue(fieldName[i],fox));
					else
						((JLabel)minValue[i]).setText(min.getStringValue(fieldName[i],fox));
					if (display[i] == DISPLAY_RT_ONLY && graph[i] != null) graph[i].updateGraphData("DisplayModule.updateMinValues");
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
		if (graph[line] != null) graph[line].updateGraphData("DisplayModule.updateSingleValue");
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
		if ( moduleType == DISPLAY_MEASURES) {
			w = MEASUREMENT_NAME_WIDTH;
		} else if ( moduleType >= DISPLAY_VULCAN && moduleType < DISPLAY_HERCI_MICRO_PKT ) {
			w = VULCAN_NAME_WIDTH;
		} else if ( moduleType >= DISPLAY_HERCI_MICRO_PKT) {
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
		if ( moduleType == DISPLAY_MEASURES) {
			w = MEASUREMENT_WIDTH;
		} else if ( moduleType >= DISPLAY_VULCAN && moduleType < DISPLAY_HERCI_MICRO_PKT ) {
			w = VULCAN_WIDTH;
		} else if ( moduleType >= DISPLAY_HERCI_MICRO_PKT) {
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
			if (moduleType == DISPLAY_MEASURES && e.getSource() == minValue[i]) {
				if (rtValue[i].getText().equalsIgnoreCase(noValue))
					;// dont open graph
				else
					displayGraph(i, true);				
			} 
		}
	}

	public void displayGraph(int i, Boolean showSkyChart) {
		try {
			if (graph[i] == null || graph[i].skyPlot != showSkyChart) {
				int conversion = BitArrayLayout.CONVERT_NONE;
				String units = "";
				
				if ((moduleType == this.DISPLAY_ALL || moduleType == this.DISPLAY_ALL_SWAP_MINMAX ) && rtPayload!=null && rtPayload.hasFieldName(fieldName[i])) {
					conversion = rtPayload.getConversionByName(fieldName[i]);
					units = rtPayload.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  FramePart.TYPE_REAL_TIME, fox, showSkyChart);
				}
				else if (moduleType == DISPLAY_PASS_MEASURES) {
					conversion = fox.passMeasurementLayout.getConversionByName(fieldName[i]);
					units = fox.passMeasurementLayout.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  SatMeasurementStore.PASS_MEASUREMENT_TYPE, fox, showSkyChart);
				} 
				else if (moduleType == DISPLAY_MEASURES) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					conversion = fox.measurementLayout.getConversionByName(fieldName[i]);
					units = fox.measurementLayout.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  SatMeasurementStore.RT_MEASUREMENT_TYPE, fox, showSkyChart);
				}
				else if (moduleType == DISPLAY_VULCAN) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					conversion = fox.rad2Layout.getConversionByName(fieldName[i]);
					units = fox.rad2Layout.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  FramePart.TYPE_RAD_TELEM_DATA, fox, showSkyChart);
				}
				else if (moduleType == DISPLAY_HERCI) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					conversion = fox.herciHS2Layout.getConversionByName(fieldName[i]);
					units = fox.herciHS2Layout.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  FramePart.TYPE_HERCI_SCIENCE_HEADER, fox, showSkyChart);
				}
				else if (moduleType == DISPLAY_HERCI_HK) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					conversion = fox.rad2Layout.getConversionByName(fieldName[i]);
					units = fox.rad2Layout.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  FramePart.TYPE_RAD_TELEM_DATA, fox, showSkyChart);
				}
				else if (minPayload!=null && minPayload.hasFieldName(fieldName[i])) {
					conversion = minPayload.getConversionByName(fieldName[i]);
					units = minPayload.getUnitsByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  FramePart.TYPE_MIN_VALUES, fox, showSkyChart);
				}
				else if (maxPayload!=null && maxPayload.hasFieldName(fieldName[i])) {
					conversion = maxPayload.getConversionByName(fieldName[i]);
					conversion = maxPayload.getConversionByName(fieldName[i]);
					graph[i] = new GraphFrame(title + "-" + label[i].getText(), fieldName[i], units, conversion,  FramePart.TYPE_MAX_VALUES, fox, showSkyChart);
				} else return;
				
				graph[i].setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/fox.jpg")));
			}
			graph[i].updateGraphData("DisplayModule.displayGraph");
			graph[i].setVisible(true);
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
					displayGraph(i, false);
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
		for (int i=0; i < graph.length; i++) {
			if (graph[i] != null) {
				if (graph[i].isVisible())
					graph[i].saveProperties(true);
				graph[i].dispose();
			}
		}
	}

	public void showGraphs() {
		for (int i=0; i < graph.length; i++) {
			if (graph[i] != null) {
				if (graph[i].isVisible())
					graph[i].toFront();
			}
		}
	}

	public void openGraphs() {
		for (int i=0; i < fieldName.length; i++) {
			boolean open = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[i], "open");
			if (open) {
				displayGraph(i, null);
			}
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
