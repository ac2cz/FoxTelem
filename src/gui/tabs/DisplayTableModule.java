package gui.tabs;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import common.Config;
import common.Spacecraft;
import gui.graph.GraphFrame;
import telemetry.legacyPayloads.RadiationPacket;
import telemetry.payloads.PayloadMaxValues;
import telemetry.payloads.PayloadMinValues;
import telemetry.payloads.PayloadRtValues;

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
public class DisplayTableModule extends JPanel implements ActionListener, MouseListener {
	
	Spacecraft fox;
	int size = 0;
	String[] fieldName = null;  // string used to lookup new values
//	JLabel[] rtValue = null;   // the RT value displated
//	JLabel[] maxValue = null;
//	JLabel[] minValue = null;
//	JLabel[] label = null;     // the label for the row
//	JPanel[] row = null;      // a panel that stores the whole row
	int[] display;           // an array that stores how each row is displayed RT, MAX. MIN, ALL
	String title = null;
	GraphFrame[] graph = null;
	//GridBagConstraints layoutConstraints;
	String noValue = "0000";
	
	DisplayTableModel displayTableModel;
	JTable table;
	String[][] data;
	
	int id = 1; // THIS NEEDS TO BE PASSED IN in the future
	
	public static final int DISPLAY_RT_ONLY = 0;
	public static final int DISPLAY_MAX_ONLY = 1;
	public static final int DISPLAY_MIN_ONLY = 2;
	public static final int DISPLAY_ALL = 3;
	public static final int DISPLAY_VULCAN = 4;
	public static final int DISPLAY_LEP = 5;
	public static final int DISPLAY_LEP_EXPOSURE = 6;
	public static final int DISPLAY_VULCAN_EXP = 7;
	
	public static Color vulcanFontColor = new Color(153,0,0);
	
	int moduleType = DISPLAY_ALL; // default this to a module that displays normal RT MAX MIN telem

	/**
	 * Create a new module and set the title.  Initialize the name and value arrays.
	 * Create the GUI
	 * 
	 * @param title
	 * @param size
	 */
	public DisplayTableModule(Spacecraft sat, String title, int size, int modType) {
		fox = sat;
		this.size = size;
		this.title = title;
		TitledBorder border = new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		
		moduleType = modType;
		
		if (moduleType >= DISPLAY_VULCAN) {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, 10));
			border.setTitleColor(vulcanFontColor);
		} else {
			border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));	
			border.setTitleColor(Color.BLUE);
//			maxValue = new JLabel[size];
//			minValue = new JLabel[size];

		}
		this.setBorder(border);
		
		displayTableModel = new DisplayTableModel();
		table = new JTable(displayTableModel);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		table.setOpaque(false);
		
		add(new JScrollPane(table));
		
		//add(table);

		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(130);
		column.setMinWidth(60);
		data = new String[size][4];
		
		for (int i=1; i<4; i++) {
			column = table.getColumnModel().getColumn(i);
			//column.sizeWidthToFit();
			column.setPreferredWidth(25);
			column.setMinWidth(25);
		}

		
		fieldName = new String[size]; 
//		rtValue = new JLabel[size];
//		label = new JLabel[size];
//		row = new JPanel[size];
		graph = new GraphFrame[size];
		display = new int[size];
		initGui();
		
	}

	/**
	 * Add a row to the module with "name" as the text to display on the screen.  This is linked to "fieldName" which
	 * is the string used to lookup new values in the telemetry record
	 * @param i - the row 
	 * @param name
	 * @param fieldName
	 * @param display - value that determines how the row is written
	 */
	public void addName(int i, String name, String fieldName, int display) {
		data[i][0] = name;
		this.fieldName[i] = fieldName;
		this.display[i] = display;
		
	}
	
	
	
	public void addNameOLD(int i, String name, String fieldName, int display) {
		
	/*
		this.label[i].setText(name);
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
		if (display < DISPLAY_VULCAN) {
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
		*/
	}


	public void updateRtValues(PayloadRtValues rt) {
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				if (Config.displayRawValues)
					data[i][1] = Integer.toString(rt.getRawValue(fieldName[i]));
				else
					data[i][1] = rt.getStringValue(fieldName[i], fox);
				if (graph[i] != null) graph[i].updateGraphData("DisplayTableModule.updateRtValues");
			}
		}
		displayTableModel.setData(data);
	}
	
	public void updateMaxValues(PayloadMaxValues rt) {
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				if (Config.displayRawValues)
					data[i][3] = Integer.toString(rt.getRawValue(fieldName[i]));
				else
					data[i][3] = rt.getStringValue(fieldName[i], fox);
				//if (graph[i] != null) graph[i].updateGraphData();
			}
		}
		displayTableModel.setData(data);

	}
	
	public void updateMinValues(PayloadMinValues rt) {
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				if (Config.displayRawValues)
					data[i][2]= Integer.toString(rt.getRawValue(fieldName[i]));
				else
					data[i][2] = rt.getStringValue(fieldName[i], fox);
				//if (graph[i] != null) graph[i].updateGraphData();
			}
		}
		displayTableModel.setData(data);

	}

	public void updateVulcanValues(RadiationPacket rad) {
		for (int i=0; i < size; i++) {
			if(fieldName[i] != null) {
				if (Config.displayRawValues)
					data[i][1] = Integer.toString(rad.getRawValue(fieldName[i]));
				else
					data[i][1] = rad.getStringValue(fieldName[i]);
				//if (graph[i] != null) graph[i].updateGraphData();
			}
		}
		displayTableModel.setData(data);

	}

	/**
	 *  Create the title row for the table and populate the labels for each row
	 */
	private void initGui() {
		data[0][0] = " ";
		data[0][1] = "RT";
		data[0][2] = "MIN";
		data[0][3] = "MAX";
		displayTableModel.setData(data);

	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
/*
		for (int i=1; i< size; i++) {
			if (e.getSource() == row[i]) {
				if (rtValue[i].getText().equalsIgnoreCase(noValue)) {
					// dont open graph
				} else
				try {
					if (graph[i] == null) {
						graph[i] = new GraphFrame(title + " - " + label[i].getText(), fieldName[i], id);
						graph[i].setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/fox.jpg")));
					}
					graph[i].updateGraphData();
					graph[i].setVisible(true);
				} catch (Exception ex) {
					Log.println("MOUSE CLICKED EXCEPTION");
					ex.printStackTrace(Log.getWriter());
				}
			}
		}
		*/
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		/*
		for (int i=1; i< size; i++) {
			if (e.getSource() == row[i]) {
				row[i].setBackground(Color.GREEN);
			}
		}
		*/
	}

	@Override
	public void mouseExited(MouseEvent e) {
		/*
		for (int i=0; i< size; i++) {
			if (e.getSource() == row[i]) {
				row[i].setBackground(Color.lightGray);
			}
		}
		*/
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void saveGraphs() {
		for (int i=0; i < graph.length; i++) {
			if (graph[i] != null)
				graph[i].saveProperties(true);
		}
	}
}
