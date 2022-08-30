package gui.tabs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.payloads.PayloadMaxValues;
import telemetry.payloads.PayloadMinValues;
import telemetry.payloads.PayloadRtValues;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.graph.GraphFrame;

import java.awt.Color;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

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
public abstract class HealthTab extends ModuleTab implements PropertyChangeListener, MouseListener, ItemListener, ActionListener, Runnable {
	
	public final int DEFAULT_DIVIDER_LOCATION = 5000;
	public static final String HEALTHTAB = "HEALTHTAB";

	private static final String LIVE = "Latest ";
	private static final String DISPLAY = "Historical ";
	protected String TAB_TYPE = "health";
	
	JPanel centerPanel;
	
	JLabel lblId;
	JLabel lblIdValue;
	JLabel lblMode;
	JLabel lblModeValue;
	JLabel lblLive;
	JLabel lblResets;
	JLabel lblResetsValue;
	JLabel lblMaxResets;
	JLabel lblMaxResetsValue;
	JLabel lblMinResets;
	JLabel lblMinResetsValue;
	JLabel lblUptime;
	JLabel lblUptimeValue;
	JLabel lblMaxUptime;
	JLabel lblMaxUptimeValue;
	JLabel lblMinUptime;
	JLabel lblMinUptimeValue;
	JLabel lblFramesDecoded;
	JLabel lblFramesDecodedValue;
	JLabel lblCaptureDate;
	JLabel lblCaptureDateValue;
	

	FramePart realTime; // the RT payload we are currently displaying
	FramePart maxPayload; // the max payload we are currently displaying
	FramePart minPayload; // the min payload we are currently displaying
	
	BitArrayLayout rt;
	BitArrayLayout max;
	BitArrayLayout min;
	
	protected static final String ID = "Satellite ";
	protected static final String MODE = "  Mode: ";
	private static final String UPTIME = "  Uptime: ";
	private static final String RESETS = "  Epoch: ";
	protected static final String DECODED = "Telemetry Payloads Decoded: ";
	protected static final String CAPTURE_DATE = "Captured: ";
	public static final int DISPLAY_CURRENT = 0;
	public static final int DISPLAY_RT = 1;
	public static final int DISPLAY_MAX = 2;
	public static final int DISPLAY_MIN = 3;
	
	protected JPanel topPanel;
	protected JPanel topPanel1;
	protected JPanel topPanel2;
	
	int splitPaneHeight = 0;
	JSplitPane splitPane;
	
	HealthTableModel rtTableModel;
	HealthTableModel minTableModel;
	HealthTableModel maxTableModel;
	JTable rtTable;
	JTable maxTable;
	JTable minTable;
	
	JScrollPane rtScrollPane;
	JScrollPane maxScrollPane;
	JScrollPane minScrollPane;
	
	JRadioButton currentBut;
	JRadioButton rtBut;
	JRadioButton maxBut;
	JRadioButton minBut;
	protected int healthTableToDisplay;
	
	public HealthTab(Spacecraft spacecraft, int displayType) throws LayoutLoadException {
		fox = spacecraft;
		foxId = fox.foxId;
		setLayout(new BorderLayout(0, 0));
		
		// force the next labels to the right side of screen
		
		topPanel = new JPanel();
		topPanel1 = new JPanel();
		topPanel2 = new JPanel();
		topPanel.setMinimumSize(new Dimension((int)(Config.displayModuleFontSize * 10/11), 50));
		add(topPanel, BorderLayout.NORTH);
		
		topPanel.setLayout(new BorderLayout(0,0));
		topPanel.add(topPanel1, BorderLayout.NORTH);
		topPanel.add(topPanel2, BorderLayout.SOUTH);
		
		topPanel1.setLayout(new BoxLayout(topPanel1, BoxLayout.X_AXIS));
		topPanel2.setLayout(new BoxLayout(topPanel2, BoxLayout.X_AXIS));
		
		lblId = new JLabel(ID);
//		lblId.setMaximumSize(new Dimension(280, 14));
//		lblId.setMinimumSize(new Dimension(280, 14));
		lblId.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblId.setForeground(textLblColor);
		topPanel1.add(lblId);
		lblIdValue = new JLabel();
		//lblIdValue.setMaximumSize(new Dimension(280, 14));
		//lblIdValue.setMinimumSize(new Dimension(280, 14));
		lblIdValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblIdValue.setForeground(textColor);
		topPanel1.add(lblIdValue);
		
		lblLive = new JLabel(LIVE);
		lblLive.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblLive.setForeground(Config.AMSAT_RED);
		topPanel2.add(lblLive);
		
		centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		centerPanel.setBackground(Color.DARK_GRAY);
		
		JPanel healthPanel = new JPanel();
		healthPanel.setLayout(new BoxLayout(healthPanel, BoxLayout.X_AXIS));
		
		initDisplayHalves(centerPanel);
				
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				centerPanel, healthPanel);
		splitPane.setOneTouchExpandable(false);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		
		//if (Config.isWindowsOs()) // then the divider is too small to see
		//	splitPane.setDividerSize((int) (splitPane.getDividerSize() * 3));
		
//		SplitPaneUI spui = splitPane.getUI();
//	    if (spui instanceof BasicSplitPaneUI) {
//	      // Setting a mouse listener directly on split pane does not work, because no events are being received.
//	      ((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
//	    	  public void mouseReleased(MouseEvent e) {
//	    		captureSplitPaneHeight();  
//	    	  }
//	      });
//	    }
	    
	    splitPane.addPropertyChangeListener("dividerLocation", this);
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);
		bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		
		addBottomFilter();
	
		lblCaptureDate = new JLabel(CAPTURE_DATE);
		lblCaptureDate.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 10/11)));
		lblCaptureDate.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		lblCaptureDate.setForeground(textLblColor);
		bottomPanel.add(lblCaptureDate );
		
		/////// WE SHOULD PASS THESE LAYOUTS IN EXPLICITY, NOT GRAB ONES WITH HARD CODED NAMES LIKE rttelemetry
		/////// OR we could allow ONLY one RT, MIN, MAX, WOD per spacecraft and use the types to grab this
		rt = null;
		
		if (fox.hasFOXDB_V3) {
			if (displayType == DisplayModule.DISPLAY_WOD)
				rt = fox.getLayoutByType(BitArrayLayout.WOD);
			else
				rt = fox.getLayoutByType(BitArrayLayout.RT);
			max = fox.getLayoutByType(BitArrayLayout.MAX);
			min = fox.getLayoutByType(BitArrayLayout.MIN);
		} else {
			if (displayType == DisplayModule.DISPLAY_WOD)
				rt = fox.getLayoutByName(Spacecraft.WOD_LAYOUT);
			else
				rt = fox.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT);
			max = fox.getLayoutByName(Spacecraft.MAX_LAYOUT);
			min = fox.getLayoutByName(Spacecraft.MIN_LAYOUT);
		}

		
		if (rt == null ) {
			throw new LayoutLoadException("MISSING PAYLOAD DEFINITION: The spacecraft file for satellite " + fox.user_display_name + " is missing the payload definition for "
					+ "" + rt.name + "\n  Remove this satellite or add/fix the payload file");
			//System.exit(1);
		} else 	if (max == null ) {
			throw new LayoutLoadException("MISSING PAYLOAD DEFINITION: The spacecraft file for satellite " + fox.user_display_name + " is missing the payload definition for "
					+ "" + max.name+ "\n  Remove this satellite or add/fix the payload file");
			
		} else if (min == null ) {
			throw new LayoutLoadException("MISSING PAYLOAD DEFINITION: The spacecraft file for satellite " + fox.user_display_name + " is missing the payload definition for "
					+ "" + min.name+ "\n  Remove this satellite or add/fix the payload file");
			
		} else
		
		analyzeModules(rt, max, min, displayType);
	
			
		
		rtTableModel = new HealthTableModel(rt);
		rtTable = new JTable(rtTableModel);
		rtScrollPane = addTable(rtTable, rtTableModel, healthPanel, rt);

		maxTableModel = new HealthTableModel(max);
		maxTable = new JTable(maxTableModel);
		maxScrollPane = addTable(maxTable, maxTableModel, healthPanel, max);

		minTableModel = new HealthTableModel(min);
		minTable = new JTable(minTableModel);
		minScrollPane = addTable(minTable, minTableModel, healthPanel, min);

		displayTable();
		
		String PREV = "prev";
		String NEXT = "next";
		
		InputMap inMap = rtTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMap = rtTable.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = rtTable.getSelectedRow();
				if (row > 0) {
					displayRow(rtTable, NO_ROW_SELECTED, row-1);
					scrollToRow(rtTable, row-1);
				}
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = rtTable.getSelectedRow();
				if (row < rtTable.getRowCount()-1) {
					displayRow(rtTable, NO_ROW_SELECTED, row+1);     
					scrollToRow(rtTable, row+1);
				}
			}
		});
		
		InputMap inMapMax = maxTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMapMax.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMapMax.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMapMax = maxTable.getActionMap();

		actMapMax.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = maxTable.getSelectedRow();
				if (row > 0) {
					displayRow(maxTable, NO_ROW_SELECTED, row-1);
					scrollToRow(maxTable, row-1);
				}
			}
		});
		actMapMax.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = maxTable.getSelectedRow();
				if (row < maxTable.getRowCount()-1) {
					displayRow(maxTable, NO_ROW_SELECTED, row+1);   
					scrollToRow(maxTable, row+1);
				}
			}
		});

		InputMap inMapMin = minTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMapMin.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMapMin.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMapMin = minTable.getActionMap();

		actMapMin.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = minTable.getSelectedRow();
				if (row > 0) {
					displayRow(minTable, NO_ROW_SELECTED, row-1);
					scrollToRow(minTable, row-1);
				}
			}
		});
		actMapMin.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = minTable.getSelectedRow();
				if (row < minTable.getRowCount()-1) {
					displayRow(minTable, NO_ROW_SELECTED, row+1);     
					scrollToRow(minTable, row+1);
				}
			}
		});
		
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		//captureSplitPaneHeight();
		if (healthTableToDisplay == DISPLAY_CURRENT) {
			hideTables(true);
		} 
		showLiveOrHistorical();  
		splitPaneHeight = splitPane.getDividerLocation();
		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, TAB_TYPE+"splitPaneHeight", splitPaneHeight);
		//System.err.println(TAB_TYPE + " set to split:" + splitPaneHeight);
		//Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, HEALTHTAB, TAB_TYPE+"healthTableToDisplay", healthTableToDisplay);
	}
	
	protected void hideTables(boolean hide) {
		//System.err.println("HIDE");
		if (hide) {
			splitPane.setDividerLocation(5000);
		} else {
			splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, TAB_TYPE+"splitPaneHeight");
			if (splitPaneHeight == 0)
				splitPaneHeight = splitPane.getMaximumDividerLocation()/2;
			if (splitPaneHeight >= splitPane.getMaximumDividerLocation())
				splitPaneHeight = splitPane.getMaximumDividerLocation()/2;
			splitPane.setDividerLocation(splitPaneHeight);
		}
//		Field m = null;
//		try {
//			m = BasicSplitPaneUI.class.getDeclaredField("keepHidden");
//			m.setAccessible(true);
//			m.set(splitPane.getUI(), hide);
//		} catch (NoSuchFieldException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		} catch (SecurityException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		} catch (IllegalArgumentException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IllegalAccessException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
	}
	
	private void scrollToRow(JTable table, int row) {
		Rectangle cellRect = table.getCellRect(row, 0, false);
		if (cellRect != null) {
			table.scrollRectToVisible(cellRect);
		}
	}

	protected void displayTable() {
		if (healthTableToDisplay == DISPLAY_CURRENT) {
			rtScrollPane.setVisible(false);
			maxScrollPane.setVisible(false);
			minScrollPane.setVisible(false);
		} else if (healthTableToDisplay == DISPLAY_RT) {
			rtScrollPane.setVisible(true);
			maxScrollPane.setVisible(false);
			minScrollPane.setVisible(false);
		} else if (healthTableToDisplay == DISPLAY_MAX) {
			rtScrollPane.setVisible(false);
			maxScrollPane.setVisible(true);
			minScrollPane.setVisible(false);
		} else {
			rtScrollPane.setVisible(false);
			maxScrollPane.setVisible(false);
			minScrollPane.setVisible(true);
		}

	}
	
	private JScrollPane addTable(JTable table, HealthTableModel healthTableModel, JPanel centerPanel, BitArrayLayout rt) {

		table.setAutoCreateRowSorter(true);
//		listSelectionModel = table.getSelectionModel();
 //       listSelectionModel.addListSelectionListener(new SharedListSelectionHandler());
 //       table.setSelectionModel(listSelectionModel);
		table.addMouseListener(this);
		
		scrollPane = new JScrollPane (table, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//table.setMinimumSize(new Dimension(6200, 6000));

		centerPanel.add(scrollPane);
		return scrollPane;
	}

	protected JLabel addReset(JPanel topPanel2, String type) {
		JLabel lblResets = new JLabel(type + " " + RESETS);
		lblResets.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblResets.setForeground(textLblColor);
		topPanel2.add(lblResets);
		JLabel lblResetsValue = new JLabel();
		lblResetsValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblResetsValue.setForeground(textColor);
		topPanel2.add(lblResetsValue);
		return lblResetsValue;
	}
	protected JLabel addUptime(JPanel topPanel2, String type) {
		return addTopPanelValue(topPanel2, UPTIME);
	}
	
	protected JLabel addTopPanelValue(JPanel topPanel2, String name) {
		int fonth = (int)(Config.displayModuleFontSize * 14/11);
		JLabel lblUptime = new JLabel(name);
		lblUptime.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblUptime.setForeground(textLblColor);
		topPanel2.add(lblUptime);
		JLabel lblUptimeValue = new JLabel();
		lblUptimeValue.setFont(new Font("SansSerif", Font.BOLD, fonth));
		lblUptimeValue.setMinimumSize(new Dimension(1600, fonth));
		lblUptimeValue.setMaximumSize(new Dimension(1600, fonth));
		lblUptimeValue.setForeground(textColor);
		topPanel2.add(lblUptimeValue);
		return lblUptimeValue;
	}
	
	private void displayUptime(JLabel lblUptimeValue, long u) {
		lblUptimeValue.setText("" + u);
	}

	private void displayResets(JLabel lblResetsValue, int u) {
		lblResetsValue.setText("" + u);
	}
		
	protected void displayMode(int newMode) {
		String mode = Spacecraft.modeNames[Spacecraft.SAFE_MODE];
		if (fox.hasModeInHeader) {
			mode = Spacecraft.getModeString(newMode);
		} else {
			FramePart radPayload = Config.payloadStore.getLatestRad(foxId);
			mode = Spacecraft.determineModeString(fox, (PayloadRtValues)realTime, (PayloadMaxValues)maxPayload, (PayloadMinValues)minPayload, radPayload);
		}
		displayMode(mode);
	}
	
	protected void displayMode(String mode) {
		if (lblModeValue != null)
			lblModeValue.setText(mode);
	}
	
	/**
	 * Given the Fox ID, display the actual number of the spacecraft
	 * @param u
	 */
	private void displayId(int u) {
		String id = "??";
		id = fox.toString() + "(" + Spacecraft.models[fox.model] + ")";
		lblIdValue.setText(id);
	}

	int total;
	protected void displayFramesDecoded(int u) {
		total = u;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				lblFramesDecodedValue.setText(""+total);
				lblFramesDecodedValue.invalidate();
				topPanel.validate();
			}
		});
	}
	
	private void displayCaptureDate(String u) {
		//SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
		//SimpleDateFormat df2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
		//df2.setTimeZone(TimeZone.getTimeZone("UTC"));  // messing with timezones here does not work.... not sure why.
		//df2.setTimeZone(TimeZone.getDefault());
		    Date result = null;
		    String reportDate = null;
			try {
				FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				result = FramePart.fileDateFormat.parse(u);
				if (this.showUTCtime)
					FramePart.reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				else 
					FramePart.reportDateFormat.setTimeZone(TimeZone.getDefault());
				reportDate = FramePart.reportDateFormat.format(result);
				
			} catch (ParseException e) {
				reportDate = "unknown";				
			} catch (NumberFormatException e) {
				reportDate = "unknown";				
			} catch (ArrayIndexOutOfBoundsException e) {
				reportDate = "unknown";
			}
			
			lblCaptureDate.setText(CAPTURE_DATE + reportDate);
	}
	
	public void updateTabRT(FramePart realTime2, boolean refreshTable) {
		if (!Config.payloadStore.initialized()) return;
		realTime = realTime2;
	//	System.out.println("GOT PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
	
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateRtValues(realTime2);
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateRtValues(realTime2);
		}
		displayId(realTime2.getFoxId());
		displayUptime(lblUptimeValue, realTime2.getUptime());
		displayResets(lblResetsValue, realTime2.getResets());
		displayCaptureDate(realTime2.getCaptureDate());
		
		if (refreshTable)
			parseFrames();
		showLiveOrHistorical();
		//int newMode = 0;
		displayMode(realTime2.newMode);
//		displayMode(99,99); // we call this just in case we are in DATA mode so that we set the label correctly
		
	}
	
	protected void showLiveOrHistorical() {
		if (healthTableToDisplay == DISPLAY_CURRENT) {
			lblLive.setForeground(Color.BLACK);
			lblLive.setText(LIVE);
		} else {
			lblLive.setForeground(Config.AMSAT_RED);
			lblLive.setText(DISPLAY);
		}
	}

	protected void parseTelemetry(String data[][]) {	

		// Now put the telemetry packets into the table data structure
		long[][] tableData = new long[data.length][data[0].length];
		for (int i=0; i < data.length; i++) { 
			tableData[data.length-i-1][0] = Long.parseLong(data[i][0]);
			tableData[data.length-i-1][1] = Long.parseLong(data[i][1]);
			for (int j=2; j< data[0].length; j++) {
				if ((data[i][j]) != null)
					if (Config.displayRawRadData)
						tableData[data.length-i-1][j] = Long.parseLong(data[i][j]);
					else {
						// Run the conversion
						
						tableData[data.length-i-1][j] = Long.parseLong(data[i][j]);
					}
			}
		}

		if (data.length > 0) {
			// RT MAX MIN SWITCH
			if (healthTableToDisplay == DISPLAY_RT) {
				rtTableModel.setData(tableData);
			} else if (healthTableToDisplay == DISPLAY_MAX) {
				maxTableModel.setData(tableData);
			} else {
				minTableModel.setData(tableData);
			}
		}
	}
	
	public void updateTabMax(FramePart maxPayload2) {
		if (!Config.payloadStore.initialized()) return;
		maxPayload = maxPayload2;
	//	System.out.println("GOT MAX PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
	
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateMaxValues(maxPayload2);
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateMaxValues(maxPayload2);
		}
	
		displayId(maxPayload2.getFoxId());
		displayUptime(lblMaxUptimeValue, maxPayload2.getUptime());
		displayResets(lblMaxResetsValue, maxPayload2.getResets());
		displayCaptureDate(maxPayload2.getCaptureDate());
//		displayMode(maxPayload2.getRawValue(SAFE_MODE_IND), maxPayload2.getRawValue(SCIENCE_MODE_IND));
		displayMode(maxPayload2.newMode);
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
	}

	public void updateTabMin(FramePart minPayload2) {
		if (!Config.payloadStore.initialized()) return;
		minPayload = minPayload2;
	//	System.out.println("GOT MIN PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");

		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateMinValues(minPayload2);
		}
		if (bottomModules != null)
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateMinValues(minPayload2);
		}
	
		displayId(minPayload2.getFoxId());
		displayUptime(lblMinUptimeValue, minPayload2.getUptime());
		displayResets(lblMinResetsValue, minPayload2.getResets());
		displayCaptureDate(minPayload2.getCaptureDate());
		displayMode(minPayload2.newMode);
//		displayMode(minPayload2.getRawValue(SAFE_MODE_IND),  minPayload2.getRawValue(SCIENCE_MODE_IND));
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
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
			if (realTime != null)
				updateTabRT(realTime, false);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
		}
		
		if (source == cbUTC) {
			if (realTime != null)
				updateTabRT(realTime, false);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
			//parseFrames();
		}
		
	}
	
	public static final int NO_ROW_SELECTED = -1;
	
	protected abstract void displayRow(JTable table, int fromRow, int toRow);
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}
	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int fromRow = NO_ROW_SELECTED;
		
		JTable table;
		
		// RT MAX MIN SWITCH
		if (healthTableToDisplay == DISPLAY_RT) {
			table = rtTable;
		} else if (healthTableToDisplay == DISPLAY_MAX) {
			table = maxTable;
		} else {
			table = minTable;
		}
		// row is the one we clicked on
		int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        
        if (e.isShiftDown()) {
        	// from row is the first in the selection.  It equals row if we clicked above the current selected row
			fromRow = table.getSelectedRow();
			int n = table.getSelectedRowCount();
			if (row == fromRow)
				fromRow = fromRow + n-1;
		}
		
        if (row >= 0 && col >= 0) {
        	//Log.println("CLICKED ROW: "+row+ " and COL: " + col);
        	displayRow(table, fromRow, row);
        }
		
	}
}
