package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.RadiationTelemetry;

import java.awt.Dimension;

import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.border.BevelBorder;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import java.awt.Color;
import java.text.ParseException;
import java.util.ArrayList;
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
public abstract class HealthTab extends ModuleTab implements ItemListener, ActionListener, Runnable {
	
	public final int DEFAULT_DIVIDER_LOCATION = 500;
	public static final String HEALTHTAB = "HEALTHTAB";
	public static final String SAFE_MODE_IND = "SafeModeIndication";
	public static final String SCIENCE_MODE_IND = "ScienceModeActive";
	
	JPanel centerPanel;
	
	JLabel lblId;
	JLabel lblIdValue;
	JLabel lblMode;
	JLabel lblModeValue;
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
	
	JCheckBox showRawValues;
	JCheckBox showUTCtime;
	FramePart realTime; // the RT payload we are currently displaying
	FramePart maxPayload; // the max payload we are currently displaying
	FramePart minPayload; // the min payload we are currently displaying
		
	protected static final String ID = "Satellite ";
	protected static final String MODE = "  Mode: ";
	private static final String UPTIME = "  Uptime: ";
	private static final String RESETS = "  Resets: ";
	protected static final String DECODED = "Telemetry Payloads Decoded: ";
	protected static final String CAPTURE_DATE = "Captured: ";
	
	protected int fonth = 0;
	
	protected JPanel topPanel;
	protected JPanel topPanel1;
	protected JPanel topPanel2;
	
	int splitPaneHeight = 0;
	JSplitPane splitPane;
	
	HealthTableModel healthTableModel;
	JTable table;
	
	public HealthTab(Spacecraft spacecraft, int displayType) {
		fox = spacecraft;
		foxId = fox.foxId;
		setLayout(new BorderLayout(0, 0));
		
		// force the next labels to the right side of screen
		fonth = (int)(Config.displayModuleFontSize * 14/11);
		
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
		
		centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		centerPanel.setBackground(Color.DARK_GRAY);
		
		JPanel healthPanel = new JPanel();
		healthPanel.setLayout(new BoxLayout(healthPanel, BoxLayout.X_AXIS));
		
		initDisplayHalves(centerPanel);
		
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, HEALTHTAB, "splitPaneHeight");

		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				centerPanel, healthPanel);
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
	      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FoxFramePart.TYPE_REAL_TIME, HEALTHTAB, "splitPaneHeight", splitPaneHeight);
	          }
	      });
	    }
		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		healthPanel.setMinimumSize(minimumSize);
		centerPanel.setMinimumSize(minimumSize);
		add(splitPane, BorderLayout.CENTER);
		bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		
		showRawValues = new JCheckBox("Display Raw Values", Config.displayRawValues);
		showRawValues.setMinimumSize(new Dimension(100, fonth));
		//showRawValues.setMaximumSize(new Dimension(100, 14));
		bottomPanel.add(showRawValues );
		showRawValues.addItemListener(this);

		showUTCtime = new JCheckBox("Display UTC Time", Config.displayUTCtime);
		bottomPanel.add(showUTCtime );
		showUTCtime.addItemListener(this);

		addBottomFilter();

		// force the next labels to the right side of screen
		bottomPanel.add(new Box.Filler(new Dimension(14,fonth), new Dimension(400,fonth), new Dimension(1600,fonth)));
		
		lblCaptureDate = new JLabel(CAPTURE_DATE);
		lblCaptureDate.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 10/11)));
		lblCaptureDate.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		lblCaptureDate.setForeground(textLblColor);
		bottomPanel.add(lblCaptureDate );
		
		BitArrayLayout rt = null;
		if (displayType == DisplayModule.DISPLAY_WOD)
			rt = fox.getLayoutByName(Spacecraft.WOD_LAYOUT);
		else
			rt = fox.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT);
		BitArrayLayout max = fox.getLayoutByName(Spacecraft.MAX_LAYOUT);
		BitArrayLayout min = fox.getLayoutByName(Spacecraft.MIN_LAYOUT);

		if (rt == null ) {
			Log.errorDialog("MISSING LAYOUTS", "The spacecraft file for satellite " + fox.name + " is missing the layout definition for "
					+ "" + Spacecraft.REAL_TIME_LAYOUT + "\n  Remove this satellite or fix the layout file");
			System.exit(1);
		} else 	if (max == null ) {
			Log.errorDialog("MISSING LAYOUTS", "The spacecraft file for satellite " + fox.name + " is missing the layout definition for "
					+ "" + Spacecraft.MAX_LAYOUT+ "\n  Remove this satellite or fix the layout file");
			System.exit(1);
		} else if (min == null ) {
			Log.errorDialog("MISSING LAYOUTS", "The spacecraft file for satellite " + fox.name + " is missing the layout definition for "
					+ "" + Spacecraft.MIN_LAYOUT+ "\n  Remove this satellite or fix the layout file");
			System.exit(1);
		} else
		try {
			analyzeModules(rt, max, min, displayType);
		} catch (LayoutLoadException e) {
			Log.errorDialog("FATAL - Health Tab Load Aborted", e.getMessage());
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		}
		
		addTable(healthPanel, rt);
	}
	
	private void addTable(JPanel centerPanel, BitArrayLayout rt) {
		healthTableModel = new HealthTableModel(rt);
		
		table = new JTable(healthTableModel);
		table.setAutoCreateRowSorter(true);
		
		scrollPane = new JScrollPane (table, 
				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//table.setMinimumSize(new Dimension(6200, 6000));
		centerPanel.add(scrollPane);
		
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

	protected void displayMode(int safeMode, int scienceMode) {
		
		if (scienceMode == 1)
			lblModeValue.setText("SCIENCE");
		else if (safeMode == 1)
			lblModeValue.setText("SAFE");
		else {
			// If the last received telemetry was from a High Speed Frame, then we are in DATA mode, otherwise TRANSPONDER
			// We know the last frame was High Speed if the Uptime for RT, MAX, MIN are the same
			if (realTime != null && minPayload != null && maxPayload != null) {
				if (realTime.uptime == minPayload.uptime && minPayload.uptime == maxPayload.uptime)				
					lblModeValue.setText("DATA");
				else
					lblModeValue.setText("TRANSPONDER");
			} else
				lblModeValue.setText("TRANSPONDER");
		}
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

	protected void displayFramesDecoded(int u) {
		lblFramesDecodedValue.setText(Integer.toString(u));
	}
	
	private void displayCaptureDate(String u) {
		//SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
		//SimpleDateFormat df2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
		//df2.setTimeZone(TimeZone.getTimeZone("UTC"));  // messing with timezones here does not work.... not sure why.
		//df2.setTimeZone(TimeZone.getDefault());
		    Date result = null;
		    String reportDate = null;
			try {
				FoxFramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				result = FoxFramePart.fileDateFormat.parse(u);
				if (showUTCtime.isSelected())
					FoxFramePart.reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				else 
					FoxFramePart.reportDateFormat.setTimeZone(TimeZone.getDefault());
				reportDate = FoxFramePart.reportDateFormat.format(result);
				
			} catch (ParseException e) {
				reportDate = "unknown";				
			} catch (NumberFormatException e) {
				reportDate = "unknown";				
			} catch (ArrayIndexOutOfBoundsException e) {
				reportDate = "unknown";
			}
			
			lblCaptureDate.setText(CAPTURE_DATE + reportDate);
	}
	
	public void updateTabRT(FramePart realTime2) {
		
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
		
		
		parseFrames();
	}
	

	protected void parseTelemetry(String data[][]) {	
//		ArrayList<RadiationTelemetry> packets = new ArrayList<RadiationTelemetry>(20);
		
		// try to decode any telemetry packets
/*		for (int i=0; i<data.length; i++) {
			RadiationTelemetry radTelem = null;
			radTelem = new RadiationTelemetry(Integer.valueOf(data[i][0]), Long.valueOf(data[i][1]), this.fox.getLayoutByName(Spacecraft.RAD2_LAYOUT));
			radTelem.rawBits = null; // otherwise we will overwrite the data we side load in
			for (int k=2; k<this.fox.getLayoutByName(Spacecraft.RAD2_LAYOUT).NUMBER_OF_FIELDS+2; k++) {  // Add 2 to skip past reset uptime
				try {
					int val = Integer.valueOf(data[i][k]);
					radTelem.fieldValue[k-2] = val;
				} catch (NumberFormatException e) {

				}
			}
			if (radTelem != null) {
				packets.add(radTelem);
			}
			
		}
*/	
		// Now put the telemetry packets into the table data structure
		String[][] packetData = new String[data.length][data[0].length];
		for (int i=0; i < data.length; i++) { 
			packetData[data.length-i-1][0] = ""+data[i][0];
			packetData[data.length-i-1][1] = ""+data[i][1];
			for (int j=2; j< data[0].length; j++) {
				if (Config.displayRawRadData)
					packetData[data.length-i-1][j] = ""+data[i][j];
				else
					packetData[data.length-i-1][j] = ""+data[i][j];
			}
		}

		if (data.length > 0) {
			healthTableModel.setData(packetData);
		}
		//(Config.payloadStore.getLatestRadTelem(foxId));
		//updateTab(packets.get(packets.size()-1));
	}
	
	public void updateTabMax(FramePart maxPayload2) {
		
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
		displayMode(maxPayload2.getRawValue(SAFE_MODE_IND), maxPayload2.getRawValue(SCIENCE_MODE_IND));
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
	}

	public void updateTabMin(FramePart minPayload2) {
		
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
		displayMode(minPayload2.getRawValue(SAFE_MODE_IND),  minPayload2.getRawValue(SCIENCE_MODE_IND));
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
	}	
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawValues = false;
			} else {
				Config.displayRawValues = true;
			}
//			Config.save();
			if (realTime != null)
				updateTabRT(realTime);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
		}
		if (source == showUTCtime) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayUTCtime = false;
			} else {
				Config.displayUTCtime = true;
			}
//			Config.save();
			if (realTime != null)
				updateTabRT(realTime);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
		}
	}
}
