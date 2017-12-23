package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.FoxSpacecraft;
import common.FoxTime;
import common.Log;
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
public abstract class ModuleTab extends FoxTelemTab implements FocusListener, ActionListener, ItemListener {

	public static final int SHOW_LIVE = 0;
	public static final int SHOW_RANGE = 2;
	public static final int SHOW_NEXT = 1;
	public int showLatest = GraphFrame.SHOW_LIVE;
	public static final String LIVE_TEXT = "Live";
	public static final String RANGE_TEXT = "Range";
	public static final String NEXT_TEXT = "Next";
	public static String NOW = "now";
	public static String YESTERDAY = "yesterday";
	public static String LAUNCH = "launch";
	public String dateFormatsToolTip = "formats:YYYYMMDD HHMMSS, YYYY/MM/DD HH:MM:SS, "
			+ "dd MMM yy HH:mm:ss, now, yesterday, launch";
	public boolean reverse = true;
	public int MIN_SAMPLES = 1;
	public static int DEFAULT_SAMPLES = 180;
	public int SAMPLES = DEFAULT_SAMPLES;
	public static long DEFAULT_START_UPTIME = 0;
	public static int DEFAULT_START_RESET = 0;
	public long START_UPTIME = DEFAULT_START_UPTIME;
	public int START_RESET = DEFAULT_START_RESET;
	public int END_RESET = DEFAULT_START_RESET;
	public long END_UPTIME = DEFAULT_START_UPTIME;
	public static String DEFAULT_START_UTC = NOW;
	public static String DEFAULT_END_UTC = NOW;
	public String START_UTC = DEFAULT_START_UTC;
	public String END_UTC = DEFAULT_END_UTC;	
	public static final int MAX_SAMPLES = 9999;
	
	protected int fonth = 0; // font height for footer. MUST set in the class that inherits from this
	
	private JLabel lblFromUptime;
	private JTextField textFromUptime;
//	private JLabel lblPlot;
	JLabel lblSamplePeriod; // The number of samples to grab for each graph
	protected JTextField txtSamplePeriod;
	private JLabel lblAvg;
	JLabel lblAvgPeriod; 
	private JTextField txtAvgPeriod;
	private JLabel lblFromReset;
	private JTextField textFromReset;
	
	private JLabel lblToReset;
	private JLabel lblToUptime;
	private JTextField textToReset;
	private JTextField textToUptime;

	JLabel lblFromUTC;
	JLabel lblToUTC;
	
	public static final String FROM_RESET = "From Reset";
	public static final String BEFORE_RESET = " before Reset";
	public static final String FROM_UTC = "From UTC";
	public static final String BEFORE_UTC = " before UTC";
	
	private JTextField textFromUtc;
	private JTextField textToUtc;
	
	protected JCheckBox cbUTC;

	JPanel footerPanel2uptime = new JPanel();
	JPanel footerPanel2utc = new JPanel();
	public boolean showUTCtime = false;
	
//	JTextField displayNumber2;
	
	FoxSpacecraft fox;
	int foxId = 0;

	DisplayModule[] topModules;
	DisplayModule[] bottomModules;

	JPanel topHalf;
	JPanel bottomHalf;
	JScrollPane scrollPane;
	JPanel bottomPanel;
	JButton btnLatest;
	JCheckBox showRawValues;
	
	protected void addBottomFilter() {
		JPanel footerPanel = new JPanel();
		bottomPanel.add(footerPanel);
		footerPanel.setLayout(new BorderLayout(0,0));
		
		JPanel footerPanelRight = new JPanel();
		JPanel footerPanelLeft = new JPanel();
		JPanel footerPanel1 = new JPanel();
		JPanel footerPanel2 = new JPanel();
		JPanel footerPanel3 = new JPanel();	
		
		footerPanel.add(footerPanelRight, BorderLayout.EAST);
		footerPanelRight.setLayout(new BorderLayout(0,0));
		footerPanelRight.add(footerPanel1, BorderLayout.WEST);
		footerPanelRight.add(footerPanel2, BorderLayout.CENTER);
		footerPanel2.setLayout(new BorderLayout(0,0));
		footerPanel2.add(footerPanel2uptime, BorderLayout.EAST);
		footerPanel2.add(footerPanel2utc, BorderLayout.WEST);
		footerPanelRight.add(footerPanel3, BorderLayout.EAST);

		footerPanel.add(footerPanelLeft, BorderLayout.WEST);
		footerPanelLeft.setLayout(new BoxLayout(footerPanelLeft, BoxLayout.X_AXIS));

		showRawValues = new JCheckBox("Display Raw Values", Config.displayRawValues);
		showRawValues.setMinimumSize(new Dimension(100, fonth));
		//showRawValues.setMaximumSize(new Dimension(100, 14));
		footerPanelLeft.add(showRawValues );
		showRawValues.addItemListener(this);
		
		cbUTC = new JCheckBox("Display UTC Time", Config.displayUTCtime);
		footerPanelLeft.add(cbUTC );
		cbUTC.addItemListener(this);
		
		footerPanelLeft.add(new Box.Filler(new Dimension(14,fonth), new Dimension(400,fonth), new Dimension(1600,fonth)));		
		
		lblFromReset = new JLabel(FROM_RESET);
		footerPanel2uptime.add(lblFromReset);
		
		textFromReset = new JTextField();
		footerPanel2uptime.add(textFromReset);
		textFromReset.setText(Integer.toString(START_RESET));

		textFromReset.setColumns(8);
		textFromReset.addActionListener(this);
		textFromReset.addFocusListener(this);
		
		lblFromUptime = new JLabel(" and Uptime");
		footerPanel2uptime.add(lblFromUptime);
		
		textFromUptime = new JTextField();
		footerPanel2uptime.add(textFromUptime);
		textFromUptime.setText(Long.toString(START_UPTIME));
		textFromUptime.setColumns(8);
		textFromUptime.addActionListener(this);
		textFromUptime.addFocusListener(this);

		lblToReset = new JLabel("  to Reset");
		footerPanel2uptime.add(lblToReset);
		
		textToReset = new JTextField();
		footerPanel2uptime.add(textToReset);

		textToReset.setText(Integer.toString(END_RESET));

		textToReset.setColumns(8);
		textToReset.addActionListener(this);
		textToReset.addFocusListener(this);
		
		lblToUptime = new JLabel(" and Uptime");
		footerPanel2uptime.add(lblToUptime);
		
		textToUptime = new JTextField();
		footerPanel2uptime.add(textToUptime);

		textToUptime.setText(Long.toString(END_UPTIME));
		textToUptime.setColumns(8);
		textToUptime.addActionListener(this);
		textToUptime.addFocusListener(this);
		
		// Now footerPanel2 for Utc
		lblFromUTC = new JLabel(FROM_UTC);
		lblFromUTC.setToolTipText(dateFormatsToolTip);

		footerPanel2utc.add(lblFromUTC);
		
		textFromUtc = new JTextField();
		footerPanel2utc.add(textFromUtc);
		textFromUtc.setText(START_UTC);
		textFromUtc.setToolTipText(dateFormatsToolTip);
		textFromUtc.setColumns(16);
		textFromUtc.addActionListener(this);
		textFromUtc.addFocusListener(this);
		
		lblToUTC = new JLabel(" to UTC");
		lblToUTC.setToolTipText(dateFormatsToolTip);

		footerPanel2utc.add(lblToUTC);
		
		textToUtc = new JTextField();
		footerPanel2utc.add(textToUtc);
		textToUtc.setText(END_UTC);
		textToUtc.setColumns(16);
		textToUtc.setToolTipText(dateFormatsToolTip);
		textToUtc.addActionListener(this);
		textToUtc.addFocusListener(this);
				
		btnLatest = new JButton(LIVE_TEXT);
		btnLatest.setForeground(Config.AMSAT_RED);
		btnLatest.setMargin(new Insets(0,0,0,0));
		btnLatest.setToolTipText("Toggle between showing the live samples, the next samples from a date/uptime or a range of samples");
		btnLatest.addActionListener(this);		
		footerPanel3.add(btnLatest);

		txtSamplePeriod = new JTextField();
		txtSamplePeriod.addActionListener(this);
		txtSamplePeriod.addFocusListener(this);
		txtSamplePeriod.setToolTipText("The number of data samples to plot.  The latest samples are returned unless a from reset/uptime is specified");

		footerPanel3.add(txtSamplePeriod);
		lblSamplePeriod = new JLabel("samples");
		footerPanel3.add(lblSamplePeriod);
		txtSamplePeriod.setText(Integer.toString(SAMPLES));
		txtSamplePeriod.setColumns(6);
		
		showRangeSearch(GraphFrame.SHOW_LIVE);
		showUptimeQuery(!showUTCtime);
		
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
				
					if (!containedIn(topModuleNames, rt.module[i])) {
						topModuleNames[rt.moduleNum[i]] = rt.module[i];
						numOfTopModules++;
					}
					topModuleLines[rt.moduleNum[i]]++;
				
			}
		}
		if (moduleType == DisplayModule.DISPLAY_WOD) {
			; //System.out.println("STOP");
		} else {
		if (max != null)
		for (int i=0; i<max.NUMBER_OF_FIELDS; i++) {
			if (!max.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				
					if (!containedIn(topModuleNames, max.module[i])) {
						topModuleNames[max.moduleNum[i]] = max.module[i];
						numOfTopModules++;
					}
					topModuleLines[max.moduleNum[i]]++;
				
			}
		}
		if (min != null)
		for (int i=0; i<min.NUMBER_OF_FIELDS; i++) {
			if (!min.module[i].equalsIgnoreCase(BitArrayLayout.NONE)) {
				
					if (!containedIn(topModuleNames, min.module[i])) {
						topModuleNames[min.moduleNum[i]] = min.module[i];
						numOfTopModules++;
					}
					topModuleLines[min.moduleNum[i]]++;
				
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
	
	private void showRangeSearch(int showLive) {
		boolean show = false;
		if (showLive == SHOW_RANGE) {
			btnLatest.setText(RANGE_TEXT);
			lblFromUTC.setText(FROM_UTC);
			lblFromReset.setText(FROM_RESET);
			show = true;
			btnLatest.setForeground(Color.BLACK);
			lblFromReset.setVisible(show);
			textFromReset.setVisible(show);
			lblFromUptime.setVisible(show);
			textFromUptime.setVisible(show);
			textFromUtc.setVisible(show);
			
			lblFromUTC.setVisible(show);
			lblToUTC.setVisible(show);
			reverse=false;
		} 
		if (showLive == SHOW_LIVE) {
			lblFromUTC.setText(BEFORE_UTC);
			lblFromReset.setText(BEFORE_RESET);
			btnLatest.setText(LIVE_TEXT);
			btnLatest.setForeground(Config.AMSAT_RED);
			lblFromReset.setVisible(show);
			textFromReset.setVisible(show);
			lblFromUptime.setVisible(show);
			textFromUptime.setVisible(show);
			textFromUtc.setVisible(show);
			lblFromUTC.setVisible(show);
			reverse=true;
		}
		if (showLive == SHOW_NEXT) {
			btnLatest.setText(NEXT_TEXT);
			lblFromUTC.setText(FROM_UTC);
			lblFromReset.setText(FROM_RESET);
			btnLatest.setForeground(Color.BLACK);
			lblFromReset.setVisible(!show);
			textFromReset.setVisible(!show);
			lblFromUptime.setVisible(!show);
			textFromUptime.setVisible(!show);
			textFromUtc.setVisible(!show);
			lblFromUTC.setVisible(!show);
			reverse=false;
		}
		
		lblToReset.setVisible(show);
		textToReset.setVisible(show);
		lblToUptime.setVisible(show);
		textToUptime.setVisible(show);
		txtSamplePeriod.setEnabled(!show);
		lblToUTC.setVisible(show);
		textToUtc.setVisible(show);

	}
	
	protected void showUptimeQuery(boolean up) {
		if (up) {
			footerPanel2uptime.setVisible(true);
			footerPanel2utc.setVisible(false);
		} else {
			footerPanel2uptime.setVisible(false);
			footerPanel2utc.setVisible(true);
		}
		
	}
	
	public static final DateFormat dateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
	public static final DateFormat dateFormat2 = new SimpleDateFormat(
			"yyyyMMdd HHmmss", Locale.ENGLISH);
	public static final DateFormat dateFormat3 = new SimpleDateFormat(
			"dd MMM yy HH:mm:ss", Locale.ENGLISH);
	
	private Date parseDate(String strDate) {
		Date date = null;
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			date = dateFormat.parse(strDate);
		} catch (ParseException e) {
			try {
				date = dateFormat2.parse(strDate);
			} catch (ParseException e2) {
				try {
					date = dateFormat3.parse(strDate);
				} catch (ParseException e3) {
					// We don't do anything in this case, the date will be null
					Log.errorDialog("Invalid Date", "Try a date in one of the following formats: \nYYYYMMDD HHMMSS\nYYYY/MM/DD HH:MM:SS\n"
							+ "dd MMM yy HH:mm:ss\nnow\nyesterday\nlaunch");

					date = null;
				}
			}
		}

		return date;
	}
	private FoxTime parseUTCField(JTextField field, String strDate) {
		if (strDate.equalsIgnoreCase(NOW)) {
			Date currentDate = new Date();
			FoxTime foxTime = fox.getUptimeForUtcDate(currentDate);
			dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
			String time = dateFormat2.format(currentDate);
			field.setText(time);
			return foxTime;
		} 
		if (strDate.equalsIgnoreCase(YESTERDAY)) {
			final Calendar cal = Calendar.getInstance();
		    cal.add(Calendar.DATE, -1);
		    Date currentDate = new Date(cal.getTimeInMillis());
		    FoxTime foxTime = fox.getUptimeForUtcDate(currentDate);
		    dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
		    String time = dateFormat2.format(currentDate);
		    field.setText(time);
		    return foxTime;
		} 
		if (strDate.equalsIgnoreCase(LAUNCH)) {
			Date date = fox.getUtcForReset(0, 0);
			if (date != null) {
				dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
				String time = dateFormat2.format(date);
				field.setText(time);
			}
			return new FoxTime(0,0);
		} 
		Date dateFrom = parseDate(strDate);
		if (dateFrom != null) {
			FoxTime foxTime = fox.getUptimeForUtcDate(dateFrom);
			return foxTime;
		}
		return null;
	}

	protected void parseUTCFields() {
		String strDate = textFromUtc.getText();
		FoxTime foxTime = parseUTCField(textFromUtc, strDate);
		if (foxTime != null) {
			START_RESET = foxTime.getReset();
			START_UPTIME = foxTime.getUptime();
		//	Log.println("From Reset: " + foxTime.getReset() + " Uptime: " + foxTime.getUptime());
		}
		strDate = textToUtc.getText();
		FoxTime foxTime2 = parseUTCField(textToUtc, strDate);
		if (foxTime2 != null) {
			END_RESET = foxTime2.getReset();
			END_UPTIME = foxTime2.getUptime();
		//	Log.println("To Reset" + foxTime2.getReset() + " Uptime: " + foxTime2.getUptime());
		}
		
	}
	
	protected void parseTextFields() {
		String text = textFromReset.getText();
		try {
			START_RESET = Integer.parseInt(text);
			if (START_RESET < 0) START_RESET = 0;

		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				START_RESET = DEFAULT_START_RESET;

			}
		}
		text = textFromUptime.getText();
		try {
			START_UPTIME = Integer.parseInt(text);
			if (START_UPTIME < 0) START_UPTIME = 0;

		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				START_UPTIME = DEFAULT_START_UPTIME;

			}
		}
		
		text = textToReset.getText();
		try {
			END_RESET = Integer.parseInt(text);
			if (END_RESET < 0) END_RESET = 0;

		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				END_RESET = DEFAULT_START_RESET;

			}
		}
		text = textToUptime.getText();
		try {
			END_UPTIME = Integer.parseInt(text);
			if (END_UPTIME < 0) END_UPTIME = 0;

		} catch (NumberFormatException ex) {
			if (text.equals("")) {
				END_UPTIME = DEFAULT_START_UPTIME;
			}
		}

		// Now back populate into the UTC fields in case the user switches
		
	}
	
	private void convertToUtc() {
		parseTextFields();
		Date date = fox.getUtcForReset(START_RESET, START_UPTIME);
		if (date != null) {
			dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
			String time = dateFormat2.format(date);
			textFromUtc.setText(time);
			START_UTC = time;
			textFromUtc.setText(time);
		}
		Date date2 = fox.getUtcForReset(END_RESET, END_UPTIME);
		if (date2 != null) {
			dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
			String time2 = dateFormat2.format(date2);
			textToUtc.setText(time2);
			END_UTC = time2;
			textToUtc.setText(time2);
		}
		if (showLatest == SHOW_RANGE) {
			SAMPLES = Config.payloadStore.getNumberOfPayloadsBetweenTimestamps(fox.foxId, START_RESET, START_UPTIME, END_RESET, END_UPTIME, FoxSpacecraft.REAL_TIME_LAYOUT);
			
		}
	}
	
	private void convertToUptime() {
		parseUTCFields();
		textFromReset.setText(Integer.toString(START_RESET));
		textFromUptime.setText(Long.toString(START_UPTIME));
		textToReset.setText(Integer.toString(END_RESET));
		textToUptime.setText(Long.toString(END_UPTIME));
		if (showLatest == SHOW_RANGE) {
			SAMPLES = Config.payloadStore.getNumberOfPayloadsBetweenTimestamps(fox.foxId, START_RESET, START_UPTIME, END_RESET, END_UPTIME, FoxSpacecraft.REAL_TIME_LAYOUT);
			
		}
	}

	private void parseFields() {
		String text = null;
		if (showUTCtime) {
			convertToUptime();
		} else {
			convertToUtc();
		}
		text = txtSamplePeriod.getText();
		try {
			SAMPLES = Integer.parseInt(text);
			if (SAMPLES > MAX_SAMPLES) {
				SAMPLES = MAX_SAMPLES;
				text = Integer.toString(MAX_SAMPLES);
			}
			//System.out.println(SAMPLES);

			//lblActual.setText("("+text+")");
			//txtSamplePeriod.setText("");
		} catch (NumberFormatException ex) {

		}
		parseFrames();
	}


	public abstract void parseFrames();
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.txtSamplePeriod) {
			parseFields();
			
		} else if (e.getSource() == this.textFromReset) {
			parseFields();
			
		} else if (e.getSource() == this.textFromUptime) {
			parseFields();
		} else if (e.getSource() == this.textToReset) {
			parseFields();
			
		} else if (e.getSource() == this.textToUptime) {
			parseFields();

		} else if (e.getSource() == this.textFromUtc) {
			parseFields();

		} else if (e.getSource() == this.textToUtc) {
			parseFields();
		} else if (e.getSource() == this.btnLatest) {
			showLatest++;
			if (showLatest > SHOW_RANGE)
				showLatest = SHOW_LIVE;
			showRangeSearch(showLatest);
			parseFrames();
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == cbUTC) {

			showUTCtime = !showUTCtime;
			if (showUTCtime) {
				parseTextFields();
				//textToUtc.setText();
				
				if (SAMPLES > MAX_SAMPLES) {
					SAMPLES = MAX_SAMPLES;
				}
				txtSamplePeriod.setText(Integer.toString(SAMPLES));
				
			} else {
				parseUTCFields();
				if (SAMPLES > MAX_SAMPLES) {
					SAMPLES = MAX_SAMPLES;
				}
				txtSamplePeriod.setText(Integer.toString(SAMPLES));
			}
			showUptimeQuery(!showUTCtime);
		}
		
	}
	
	@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource() == this.txtSamplePeriod) {
		//	parseTextFields();
			
		} else if (e.getSource() == this.textFromReset) {
		//	parseTextFields();
			
		} else if (e.getSource() == this.textFromUptime) {
		//	parseTextFields();
			
		} else if (e.getSource() == this.txtAvgPeriod) {
		//		parseAvgPeriod();
		}
		
	}

}
