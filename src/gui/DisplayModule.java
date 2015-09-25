package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import common.Config;
import common.Log;
import common.Spacecraft;
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
	JLabel[] minValue = null;
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
	private int NAME_WIDTH = 110;
	private int ROW_HEIGHT = 16;

	// These are the mac defaults
	private static final int MAC_VAL_WIDTH = 44;
	private static final int MAC_SINGLE_VAL_WIDTH = 132;
	private static final int MAC_VULCAN_WIDTH = 55;
	private static final int MAC_VULCAN_NAME_WIDTH = 154;
	private static final int MAC_NAME_WIDTH = 121;
	private static final int MAC_ROW_HEIGHT = 16;

	// These are the linux defaults
	private static final int LIN_VAL_WIDTH = 48;
	private static final int LIN_SINGLE_VAL_WIDTH = 144;
	private static final int LIN_VULCAN_WIDTH = 60;
	private static final int LIN_VULCAN_NAME_WIDTH = 168;
	private static final int LIN_NAME_WIDTH = 132;
	private static final int LIN_ROW_HEIGHT = 16;
	
	public static final int DISPLAY_RT_ONLY = 0;
	public static final int DISPLAY_MAX_ONLY = 1;
	public static final int DISPLAY_MIN_ONLY = 2;
	public static final int DISPLAY_ALL = 3;
	public static final int DISPLAY_VULCAN = 5;
	public static final int DISPLAY_LEP = 6;
	public static final int DISPLAY_LEP_EXPOSURE = 7;
	public static final int DISPLAY_VULCAN_EXP = 8;
	public static final int DISPLAY_MEASURES = 9;
	public static final int DISPLAY_PASS_MEASURES = 10;
	
	public static Color vulcanFontColor = new Color(153,0,0);
	
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
		
		if (moduleType >= DISPLAY_VULCAN) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, 10));
			border.setTitleColor(vulcanFontColor);
		} else {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));	
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
		
		int w = 0;
		if (display == DISPLAY_RT_ONLY) {
			w = SINGLE_VAL_WIDTH;
		} else if (display == DISPLAY_ALL ) {
			w= VAL_WIDTH;
		} else if ( display >= DISPLAY_VULCAN) {
			w = VULCAN_WIDTH;
		}
		rtValue[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
		rtValue[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
		rtValue[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
		w = 0;
		if (display == DISPLAY_MIN_ONLY) {
			w = SINGLE_VAL_WIDTH;
		} else if (display == DISPLAY_ALL) {
			w= VAL_WIDTH;
		}
		if (display < DISPLAY_VULCAN && minValue != null && maxValue != null) {
			minValue[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			minValue[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			minValue[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			w = 0;
			if (display == DISPLAY_MAX_ONLY) {
				w = SINGLE_VAL_WIDTH;
			} else if (display == DISPLAY_ALL) {
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
					if (graph[i] != null) graph[i].updateGraphData();
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
						else
							maxValue[i].setText(Integer.toString(max.getRawValue(fieldName[i])));
					} else if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
						rtValue[i].setText(max.getStringValue(fieldName[i],fox));
					else
						maxValue[i].setText(max.getStringValue(fieldName[i],fox));
					if (graph[i] != null) graph[i].updateGraphData();
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
						else
							minValue[i].setText(Integer.toString(min.getRawValue(fieldName[i])));
					} else if (display[i] == DISPLAY_RT_ONLY) // we put this in the RT column
						rtValue[i].setText(min.getStringValue(fieldName[i],fox));
					else
						minValue[i].setText(min.getStringValue(fieldName[i],fox));
					if (graph[i] != null) graph[i].updateGraphData();
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
		if (graph[line] != null) graph[line].updateGraphData();
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
		
		int w = NAME_WIDTH;
		if (moduleType >= DISPLAY_VULCAN)
			w = VULCAN_NAME_WIDTH;
	
		for (int i=0; i < size; i++) {
			label[i] = new JLabel();
			label[i].setMinimumSize(new Dimension(w, ROW_HEIGHT)); // width height
			label[i].setMaximumSize(new Dimension(w, ROW_HEIGHT)); // width height
			label[i].setPreferredSize(new Dimension(w, ROW_HEIGHT)); // width height
			row[i].add(label[i]);
		}

		w = VAL_WIDTH;
		if (moduleType >= DISPLAY_VULCAN)
			w = VULCAN_WIDTH;

		// NOW ADD THE TITLE
		JLabel rtTitle;
		if (moduleType < DISPLAY_VULCAN)
			rtTitle = new JLabel("  RT");
		else 
			rtTitle = new JLabel("Last");
		
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
			
		if (moduleType < DISPLAY_VULCAN) {
			JLabel minTitle = new JLabel("MIN");
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
		// TODO Auto-generated method stub
		
	}

	public void displayGraph(int i) {
		try {
			if (graph[i] == null) {
				int conversion = BitArrayLayout.CONVERT_NONE;
				if (rtPayload!=null && rtPayload.hasFieldName(fieldName[i])) {
					conversion = rtPayload.getConversionByName(fieldName[i]);
					graph[i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], conversion,  FramePart.TYPE_REAL_TIME, fox);
				}
				else if (moduleType == DISPLAY_PASS_MEASURES) {
				
				} 
				else if (moduleType == DISPLAY_MEASURES) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					conversion = fox.measurementLayout.getConversionByName(fieldName[i]);
					graph[i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], conversion,  0, fox);
				}
				else if (moduleType == DISPLAY_VULCAN) {
					//  && Double.parseDouble(rtValue[i].getText()) != 0.0
					graph[i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], BitArrayLayout.CONVERT_NONE,  0, fox);
				}
				else if (minPayload!=null && minPayload.hasFieldName(fieldName[i])) {
					conversion = minPayload.getConversionByName(fieldName[i]);
					graph[i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], conversion,  FramePart.TYPE_MIN_VALUES, fox);
				}
				else if (maxPayload!=null && maxPayload.hasFieldName(fieldName[i])) {
					conversion = maxPayload.getConversionByName(fieldName[i]);
					graph[i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], conversion,  FramePart.TYPE_MAX_VALUES, fox);
				}
				
				graph[i].setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/fox.jpg")));
			}
			graph[i].updateGraphData();
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
				if (moduleType == DISPLAY_PASS_MEASURES || moduleType == DISPLAY_VULCAN || rtValue[i].getText().equalsIgnoreCase(noValue)) {
					// dont open graph
				} else
					displayGraph(i);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		for (int i=1; i< size; i++) {
			if (e.getSource() == row[i]) {
				row[i].setBackground(Color.BLUE);
			}
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		for (int i=0; i< size; i++) {
			if (e.getSource() == row[i]) {
				row[i].setBackground(Color.lightGray);
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
				displayGraph(i);
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
		}
		if (Config.isLinuxOs()) {
			VAL_WIDTH = LIN_VAL_WIDTH;
			SINGLE_VAL_WIDTH = LIN_SINGLE_VAL_WIDTH;
			VULCAN_WIDTH = LIN_VULCAN_WIDTH;
			VULCAN_NAME_WIDTH = LIN_VULCAN_NAME_WIDTH;
			NAME_WIDTH = LIN_NAME_WIDTH;
			ROW_HEIGHT = LIN_ROW_HEIGHT;
		}

//		Log.println("Using scale: " + scale);
		VAL_WIDTH = (int)(VAL_WIDTH * scale);
		SINGLE_VAL_WIDTH = (int)(SINGLE_VAL_WIDTH * scale);
		VULCAN_WIDTH = (int)(VULCAN_WIDTH * scale);
		VULCAN_NAME_WIDTH = (int)(VULCAN_NAME_WIDTH * scale);
		NAME_WIDTH = (int)(NAME_WIDTH * scale);
		ROW_HEIGHT = (int)(ROW_HEIGHT * scale);

	}
}
