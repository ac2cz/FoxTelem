package gui.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.PayloadStore;
import telemetry.conversion.Conversion;
import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import common.FoxTime;
import measure.SatMeasurementStore;

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
public class GraphFrame extends JFrame implements WindowListener, ActionListener, ItemListener, FocusListener, ComponentListener {

	public String[] fieldName;
	public String[] fieldName2;
	String fieldUnits = "";
	String fieldUnits2 = "";
	String displayTitle; // the actual title of the graph - calculated
	String title; // the title of the module, e.g. Computer - passed in
	BitArrayLayout layout;
	int payloadType;  // This should now be the payload number and is used to uniquely save the graph config.  Except for Measurements, where it is still used to identify them
	int conversionType;
	int conversionType2;
	Conversion conversion;
	Conversion conversion2;
	private JPanel contentPane;
	private GraphCanvas panel;
	private JPanel titlePanel;
	private JPanel footerPanel;
	
	private JButton btnDefault;
	private JButton btnVerticalLines;
	private JButton btnHorizontalLines;
	private JButton btnCSV;
	private JButton btnCopy;
	private JButton btnDerivative;
	private JButton btnMain;
	private JButton btnAvg;
	private JButton btnLines;
	private JButton btnPoints;
	private JButton btnLatest;
	private JButton btnMapType;
	private JButton cbUTC;
	private JButton cbUptime;
	private JCheckBox cbRoundLabels;
	private JCheckBox cbShowSun;
	@SuppressWarnings("rawtypes")
	private JComboBox cbAddVariable;
	private ArrayList<String> variables;
	
	public Spacecraft fox;
	public static final String LIVE_TEXT = "Last";
	public static final String RANGE_TEXT = "Range";
	public static final String NEXT_TEXT = "Next";
	public static String NOW = "now";
	public static String YESTERDAY = "yesterday";
	public static String LAUNCH = "launch";
	public String dateFormatsToolTip = "formats:YYYYMMDD HHMMSS, YYYY/MM/DD HH:MM:SS, "
			+ "dd MMM yy HH:mm:ss, now, yesterday, launch";
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
	public static final int MAX_SAMPLES = 999999;
	public static final int MAX_AVG_SAMPLES = 999;
	public static int DEFAULT_AVG_PERIOD = 12;
	public int AVG_PERIOD = DEFAULT_AVG_PERIOD;
	public Date fromUtcDate;
	public Date toUtcDate;
	//private JLabel lblActual;
	public static final int DEFAULT_UPTIME_THRESHOLD = 60*60*1;// plot continuous uptime unless more than 1 hour gap
	public static final int CONTINUOUS_UPTIME_THRESHOLD = -1;
	public double UPTIME_THRESHOLD =DEFAULT_UPTIME_THRESHOLD; 
	private JButton chckbxPlotAllUptime;
	private JLabel lblFromUptime;
	private JTextField textFromUptime;
//	private JLabel lblPlot;
	JLabel lblSamplePeriod; // The number of samples to grab for each graph
	private JTextField txtSamplePeriod;
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
	
	public static final String FROM_RESET = "From Epoch";
	public static final String BEFORE_RESET = " before Epoch";
	public static final String FROM_UTC = "From UTC";
	public static final String BEFORE_UTC = " before UTC";
	
	private JTextField textFromUtc;
	private JTextField textToUtc;

	JPanel footerPanel2uptime = new JPanel();
	JPanel footerPanel2utc = new JPanel();
	
	//private DiagnosticTextArea textArea;
	private DiagnosticTable diagnosticTable;
	JButton btnAdd;
	
	public boolean plotDerivative;
	public boolean dspAvg;
	public boolean showVerticalLines;
	public boolean showHorizontalLines;
	public boolean hideMain = true;
	public boolean showUTCtime = false;
	public boolean hideUptime = true;
	public boolean roundLabels = true;
	public boolean showSun = false;
	public boolean hidePoints = true;
	public boolean hideLines = true;
	public boolean showContinuous = false;
	public int showLatest = 0;
	public static final int SHOW_LIVE = 0;
	public static final int SHOW_RANGE = 2;
	public static final int SHOW_NEXT = 1;
	
	public boolean add = false;
	public final static int GRAPH_PLOT = 0;
	public final static int SKY_PLOT = 1;
	public final static int EARTH_PLOT = 2;
	public final static int SAVED_PLOT = 99;
	public final static int MAX_PLOT_TYPES = 3;
	public int plotType = SAVED_PLOT;
	

	public final static int NO_MAP_EQUIRECTANGULAR = 1;
//	public final static int NO_MAP_MERCATOR = 1;
	public final static int LINE_MAP_EQUIRECTANGULAR = 2;
	public final static int COLOR_MAP_EQUIRECTANGULAR = 3;
//	public final static int COLOR_MAP_MERCATOR = 4;
	public int mapType = LINE_MAP_EQUIRECTANGULAR;
	
	
	boolean textDisplay = false;
	
	public GraphFrame(String title, String fieldName, BitArrayLayout lay, Spacecraft fox2, int plot) {
		this.title = title;
		fox = fox2;
		this.fieldName = new String[1];
		this.fieldName[0] = fieldName;
		this.layout = lay;
		String conversionName = lay.getConversionNameByName(fieldName);
		this.conversion = fox.getConversionByName(conversionName);
		this.conversionType = Conversion.getLegacyConversionFromString(conversionName);
		this.fieldUnits = lay.getUnitsByName(fieldName);
		payloadType = lay.number; // set this just so that things get saved correctly
		if (plot != SAVED_PLOT) // take the value, otherwise we use what was loaded from the save
			plotType = plot;
		init();
		
	}	
	/**
	 * Create the frame.
	 */
	@SuppressWarnings("rawtypes")
	public GraphFrame(String title, String fieldName, String fieldUnits, String conversionName, int plType, BitArrayLayout lay, Spacecraft fox2, int plot) {
		fox = fox2;
		this.fieldName = new String[1];
		this.fieldName[0] = fieldName;
		this.fieldUnits = fieldUnits;
		this.title = title;
		this.conversion = fox.getConversionByName(conversionName); ///////////////// Need to make sure we get the legacy conversion if needed. Set up BOTH
		this.conversionType = Conversion.getLegacyConversionFromString(conversionName);
		
		if (lay == null)
			this.layout = getLayout(plType);
		else
			this.layout = lay;
		
		payloadType = plType;
		if (plot != SAVED_PLOT) // take the value, otherwise we use what was loaded from the save
			plotType = plot;
		init();
	}
	
	private void init() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		addWindowListener(this);
		addComponentListener(this);
		
		
		loadProperties();
//		Image img = Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/fox.jpg"));
//		setIconImage(img);	
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		titlePanel = new JPanel();
		contentPane.add(titlePanel, BorderLayout.NORTH);
		titlePanel.setLayout(new BorderLayout(0,0));
		JPanel titlePanelRight = new JPanel();
		JPanel titlePanelcenter = new JPanel();
		JPanel titlePanelLeft = new JPanel();
		titlePanel.add(titlePanelRight, BorderLayout.EAST);
		titlePanel.add(titlePanelcenter, BorderLayout.CENTER);
		titlePanel.add(titlePanelLeft, BorderLayout.WEST);
		calcTitle();
		
//		JLabel lblTitle = new JLabel(displayTitle);
//		lblTitle.setFont(new Font("SansSerif", Font.BOLD, Config.graphAxisFontSize + 3));
//		titlePanelcenter.add(lblTitle);

		if (textDisplay(conversionType) ) {   
			diagnosticTable = new DiagnosticTable(title, fieldName[0], conversionType, this, fox);
			contentPane.add(diagnosticTable, BorderLayout.CENTER);
			textDisplay = true;
		} else if (plotType == SKY_PLOT){
			initSkyPlotFields();
			panel = new DensityPlotPanel(title, this, fox);
			contentPane.add(panel, BorderLayout.CENTER);
		} else if (plotType == EARTH_PLOT){
			panel = new EarthPlotPanel(title, this, fox);
			contentPane.add(panel, BorderLayout.CENTER);
		} else {
			panel = new GraphPanel(title, this, fox);
			contentPane.add(panel, BorderLayout.CENTER);
		}

		if (!(textDisplay || plotType == SKY_PLOT || plotType == EARTH_PLOT)) {
			btnAdd = new JButton("+ ");
			titlePanelLeft.add(btnAdd);
			btnAdd.addActionListener(this);
			btnAdd.setToolTipText("Add another trace to this graph");
			cbAddVariable = new JComboBox();

			// make a list of the variables that we could plot
			// this is every variable in the Layout that is shown in a module. Only broken telem channels are not shown
			initVarlist();
			titlePanelLeft.add(cbAddVariable);
			cbAddVariable.addActionListener(this);
			cbAddVariable.setVisible(add);
		}

		
		// Toolbar buttons
		cbUTC = new JButton("UTC");
		cbUTC.setMargin(new Insets(0,0,0,0));
		cbUTC.addActionListener(this);
		cbUTC.setToolTipText("Toggle between UTC time and spacecraft epoch/uptime");
		titlePanelRight.add(cbUTC);
		
		if (!(plotType == SKY_PLOT || plotType == EARTH_PLOT)) {
			cbUptime = new JButton("Uptime");
			cbUptime.setMargin(new Insets(0,0,0,0));
			cbUptime.setToolTipText("Toggle display of uptime on the horizontal axis");
			//cbUptime.setSelected(!hideUptime);
			cbUptime.addActionListener(this);
			titlePanelRight.add(cbUptime);
		}
		if (!(plotType == SKY_PLOT || textDisplay || plotType == EARTH_PLOT)) {
			chckbxPlotAllUptime = new JButton("Continuous");
			chckbxPlotAllUptime.setMargin(new Insets(0,0,0,0));
			titlePanelRight.add(chckbxPlotAllUptime);
			//chckbxPlotAllUptime.setSelected(showContinuous);
			chckbxPlotAllUptime.addActionListener(this);
			chckbxPlotAllUptime.setToolTipText("Show all uptime values, even if there is no data to plot");
			
		}
		
		btnLines = new JButton("Lines");
		btnLines.setMargin(new Insets(0,0,0,0));
		btnLines.setToolTipText("Draw lines between data points");
		btnLines.addActionListener(this);
		titlePanelRight.add(btnLines);
		if (this.textDisplay || plotType == SKY_PLOT || plotType == EARTH_PLOT) btnLines.setVisible(false);

		btnPoints = new JButton("Points");
		btnPoints.setMargin(new Insets(0,0,0,0));
		btnPoints.setToolTipText("Show data points");
		btnPoints.addActionListener(this);
		titlePanelRight.add(btnPoints);
		if (this.textDisplay || plotType == SKY_PLOT || plotType == EARTH_PLOT) btnPoints.setVisible(false);


		btnMapType = createIconButton("/images/mapButton.jpg","Map Type","Toggle background map type and projection");
		titlePanelRight.add(btnMapType);
		if (plotType != EARTH_PLOT) btnMapType.setVisible(false);

		btnHorizontalLines = createIconButton("/images/horizontalLines.png","Horizontal","Show Horizontal Lines");
		titlePanelRight.add(btnHorizontalLines);
		if (this.textDisplay || plotType == SKY_PLOT) btnHorizontalLines.setVisible(false);

		btnVerticalLines = createIconButton("/images/verticalLines.png","Verrtical","Show Vertical Lines");
		titlePanelRight.add(btnVerticalLines);
		if (this.textDisplay || plotType == SKY_PLOT) btnVerticalLines.setVisible(false);

		btnMain = new JButton("Hide");
		btnMain.setMargin(new Insets(0,0,0,0));
		btnMain.setToolTipText("Hide the first trace (useful if the derivative or average has been plotted)");
		btnMain.addActionListener(this);
		titlePanelRight.add(btnMain);
		if (this.textDisplay || plotType == SKY_PLOT || plotType == EARTH_PLOT) btnMain.setVisible(false);

		btnDerivative = createIconButton("/images/derivSmall.png","Deriv","Plot 1st Derivative (1st difference)");
		titlePanelRight.add(btnDerivative);
		if (this.textDisplay || plotType == SKY_PLOT || plotType == EARTH_PLOT) btnDerivative.setVisible(false);

		btnAvg = new JButton("AVG");
		btnAvg.setMargin(new Insets(0,0,0,0));
		btnAvg.setToolTipText("Running Average / Low Pass Filter");
		btnAvg.addActionListener(this);
		
		titlePanelRight.add(btnAvg);
		if (this.textDisplay || plotType == SKY_PLOT || plotType == EARTH_PLOT) btnAvg.setVisible(false);

		if (conversionType == BitArrayLayout.CONVERT_STATUS_BIT || conversionType == BitArrayLayout.CONVERT_ANTENNA
				|| conversionType == BitArrayLayout.CONVERT_STATUS_ENABLED || conversionType == BitArrayLayout.CONVERT_BOOLEAN ) {
			btnDerivative.setVisible(false);
			btnAvg.setVisible(false);
		}

		setRedOutline(btnMain, hideMain);
		setRedOutline(btnLines, !hideLines);
		setRedOutline(btnPoints, !hidePoints);
		setRedOutline(btnDerivative, plotDerivative);
		setRedOutline(btnAvg, dspAvg);
		setRedOutline(btnHorizontalLines, showHorizontalLines);
		setRedOutline(btnVerticalLines, showVerticalLines);
		setRedOutline(cbUTC, showUTCtime);
		if (chckbxPlotAllUptime != null)
			setRedOutline(chckbxPlotAllUptime,showContinuous);
		if (cbUptime != null)
			setRedOutline(cbUptime,!hideUptime);


		btnDefault = createIconButton("/images/refreshSmall.png","Reset","Reset to default range and show latest data");
		titlePanelRight.add(btnDefault);
		//if (this.textDisplay) btnLatest.setEnabled(false);
		
		btnCSV = createIconButton("/images/saveSmall.png","CSV","Save this data to a CSV file");
		titlePanelRight.add(btnCSV);
		if (this.textDisplay) btnCSV.setEnabled(false);
		
		btnCopy = createIconButton("/images/copySmall.png","Copy","Copy graph to clipboard");
		titlePanelRight.add(btnCopy);
		if (this.textDisplay) btnCopy.setEnabled(false);
		
		footerPanel = new JPanel();
		contentPane.add(footerPanel, BorderLayout.SOUTH);
		footerPanel.setLayout(new BorderLayout(0,0));
		JPanel footerPanelRight = new JPanel();
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

		if (!(plotType == SKY_PLOT || plotType == EARTH_PLOT)) {
			cbRoundLabels = new JCheckBox("Round Labels");
			cbRoundLabels.setSelected(roundLabels);
			cbRoundLabels.addItemListener(this);
			footerPanel1.add(cbRoundLabels);
			cbShowSun = new JCheckBox("Show Sun");
			if (Config.foxTelemCalcsPosition) {
				cbShowSun.setSelected(showSun);
				cbShowSun.setEnabled(true);
			} else
				cbShowSun.setEnabled(false);
			cbShowSun.addItemListener(this);
			footerPanel1.add(cbShowSun);
		}
		if (!(plotType == SKY_PLOT || plotType == EARTH_PLOT)) {

			lblAvg = new JLabel("Avg");
			txtAvgPeriod = new JTextField();
			txtAvgPeriod.addActionListener(this);
			txtAvgPeriod.addFocusListener(this);
			lblAvgPeriod = new JLabel("samples  ");

			setAvgVisible(dspAvg);

			footerPanel1.add(lblAvg);
			footerPanel1.add(txtAvgPeriod);
			footerPanel1.add(lblAvgPeriod);
			txtAvgPeriod.setText(Integer.toString(AVG_PERIOD));
			txtAvgPeriod.setColumns(3);
		}
		
		// Now footerPanel2 for Uptime
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

		lblToReset = new JLabel("  to Epoch");
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
//		btnLatest.setForeground(Config.AMSAT_RED);
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

		showRangeSearch(showLatest);
		showUptimeQuery(!showUTCtime);

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
		} 
		if (showLive == SHOW_LIVE) {
			lblFromUTC.setText(BEFORE_UTC);
			lblFromReset.setText(BEFORE_RESET);
			btnLatest.setText(LIVE_TEXT);
//			btnLatest.setForeground(Config.AMSAT_RED);
			lblFromReset.setVisible(show);
			textFromReset.setVisible(show);
			lblFromUptime.setVisible(show);
			textFromUptime.setVisible(show);
			textFromUtc.setVisible(show);
			lblFromUTC.setVisible(show);
	
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
			
		}
		
		
		lblToReset.setVisible(show);
		textToReset.setVisible(show);
		lblToUptime.setVisible(show);
		textToUptime.setVisible(show);
		txtSamplePeriod.setEnabled(!show);
		lblToUTC.setVisible(show);
		textToUtc.setVisible(show);

	}
	
	private void showUptimeQuery(boolean up) {
		if (up) {
			footerPanel2uptime.setVisible(true);
			footerPanel2utc.setVisible(false);
		} else {
			footerPanel2uptime.setVisible(false);
			footerPanel2utc.setVisible(true);
		}
		
	}
	
	private void initSkyPlotFields() {
		String s = this.fieldName[0];
		this.fieldName2 = new String[2];
		this.fieldName2[0] = "EL";
		this.fieldName2[1] = "AZ";
		this.fieldName[0] = s;
	}

	
	private void initEarthPlotFields() {
		String s = this.fieldName[0];
		this.fieldName2 = new String[2];
		this.fieldName2[0] = "LAT";
		this.fieldName2[1] = "LONG";
		this.fieldName[0] = s;
	}

	
	private boolean textDisplay(int conversionType) {
		if (conversionType == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC || 
				conversionType == BitArrayLayout.CONVERT_HARD_ERROR || 
				conversionType == BitArrayLayout.CONVERT_SOFT_ERROR || 
				conversionType == BitArrayLayout.CONVERT_SOFT_ERROR_84488 ||
				conversionType == BitArrayLayout.CONVERT_ICR_DIAGNOSTIC ||
				conversionType == BitArrayLayout.CONVERT_COM1_ISIS_ANT_STATUS)
			return true;
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initVarlist() {
		variables = new ArrayList<String>();
		ArrayList<String> labels = new ArrayList<String>();
		for (int v=0; v<layout.fieldName.length; v++) {
			if (!layout.module[v].equalsIgnoreCase(BitArrayLayout.NONE)) {
				labels.add(layout.module[v] + "-" + layout.shortName[v]);
				variables.add(layout.fieldName[v]);
			}
		}
		Object[] fields = labels.toArray();
		cbAddVariable.removeAllItems();
		cbAddVariable.setModel(new DefaultComboBoxModel(fields));
	}
	
	// FIXME - if we pass in the layout, then we would not need this lookup.  This logic SHOULD NOT BE HERE!
	// We now pass layout into DisplayModule. This is only called in legacy situations
	private BitArrayLayout getLayout(int plType) {
		BitArrayLayout layout = null;
		if (plType == FramePart.TYPE_REAL_TIME)
			layout = fox.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT);
		else if (plType == FramePart.TYPE_MAX_VALUES)
			layout = fox.getLayoutByName(Spacecraft.MAX_LAYOUT);
		else if (plType == FramePart.TYPE_MIN_VALUES)
			layout = fox.getLayoutByName(Spacecraft.MIN_LAYOUT);
		else if (plType == FramePart.TYPE_RAD_EXP_DATA)
			layout = fox.getLayoutByName(Spacecraft.RAD_LAYOUT);
		else if (plType == FramePart.TYPE_RAD_TELEM_DATA)
			layout = fox.getLayoutByName(Spacecraft.RAD2_LAYOUT);
		else if (plType == FramePart.TYPE_HERCI_SCIENCE_HEADER)
			layout = fox.getLayoutByName(Spacecraft.HERCI_HS_HEADER_LAYOUT);
		else if (plType == SatMeasurementStore.RT_MEASUREMENT_TYPE)
			layout = fox.measurementLayout;
		else if (plType == SatMeasurementStore.PASS_MEASUREMENT_TYPE)
			layout = fox.passMeasurementLayout;
		else if (plType == FramePart.TYPE_WOD)
			layout = fox.getLayoutByName(Spacecraft.WOD_LAYOUT);
		else if (plType == FramePart.TYPE_WOD_EXP)
			layout = fox.getLayoutByName(Spacecraft.WOD_RAD_LAYOUT);
		else if (plType == FramePart.TYPE_WOD_EXP_TELEM_DATA)
			layout = fox.getLayoutByName(Spacecraft.WOD_RAD2_LAYOUT);
		else if (plType == FramePart.TYPE_UW_EXPERIMENT)
			layout = fox.getLayoutByName(Spacecraft.CAN_LAYOUT);
		else if (plType == FramePart.TYPE_UW_WOD_EXPERIMENT)
			layout = fox.getLayoutByName(Spacecraft.WOD_CAN_LAYOUT);
		return layout;
	}
	
	private void calcTitle() {
		//BitArrayLayout layout = getLayout(payloadType);
		if (!(plotType == SKY_PLOT) && (fieldName.length > 1 || fieldName2 != null))
			displayTitle = fox.user_display_name;
		else {
			displayTitle = title; // + " - " + layout.getShortNameByName(fieldName[0]) + "(" + layout.getUnitsByName(fieldName[0])+ ")";
		//	if (payloadType != SatMeasurementStore.RT_MEASUREMENT_TYPE &&
		//			payloadType !=SatMeasurementStore.PASS_MEASUREMENT_TYPE) // measurement
				displayTitle = fox.user_display_name + " " + displayTitle;
			if (conversionType == BitArrayLayout.CONVERT_FREQ) {
				int freqOffset = (int) fox.user_telemetryDownlinkFreqkHz;
				displayTitle = title + " delta from " + freqOffset + " kHz";
			}
		}
	}
	private void setAvgVisible(boolean f) {
		lblAvg.setVisible(f);
		txtAvgPeriod.setVisible(f);
		lblAvgPeriod.setVisible(f);
		
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
	
	public void updateGraphData(String by) {
		//System.err.println("Graph Update by: " + by);
		if (this.textDisplay) {
			diagnosticTable.updateData();
			//textArea.updateData();			
		} else
			panel.updateGraphData(layout, "GraphFrame.updateGraphData");
	}

	/**
	 * Save properties that are not captured realtime.  This is mainly generic properties such as the size of the
	 * window that are not tied to a control that we have added.
	 */
	public void saveProperties(boolean open) {
		//Log.println("Saving graph properties: " + fieldName);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "showLatest", showLatest);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowHeight", this.getHeight());
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowWidth", this.getWidth());
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowX", this.getX());
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowY", this.getY());
		
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "numberOfSamples", this.SAMPLES);
		
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fromReset", this.START_RESET);
		Config.saveGraphLongParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fromUptime", this.START_UPTIME);
		Config.saveGraphLongParam(fox.getIdString(), plotType, payloadType, fieldName[0], "toUptime", this.END_UPTIME);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "toReset", this.END_RESET);
		
		Config.saveGraphParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fromUtc", this.START_UTC);
		Config.saveGraphParam(fox.getIdString(), plotType, payloadType, fieldName[0], "toUtc", this.END_UTC);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "open", open);
		
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "hideMain", hideMain);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "hideLines", hideLines);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "hidePoints", hidePoints);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "plotDerivative", plotDerivative);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "dspAvg", dspAvg);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "showVerticalLines", showVerticalLines);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "showHorizontalLines", showHorizontalLines);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "showUTCtime", showUTCtime);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "hideUptime", hideUptime);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "roundLabels", roundLabels);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "showSun", showSun);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "plotType", plotType);
		
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "AVG_PERIOD", AVG_PERIOD);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "mapType", mapType);
		Config.saveGraphBooleanParam(fox.getIdString(), plotType, payloadType, fieldName[0], "showContinuous", showContinuous);
		
		String fields1 = "";
		for (String s : fieldName)
			fields1 += s + ";";
		Config.saveGraphParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fieldName", fields1);
		String fields2 = "";
		if (fieldName2 != null) {
			for (String s : fieldName2)
				fields2 += s + ";";
			Config.saveGraphParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fieldName2", fields2);
		} else {
			Config.saveGraphParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fieldName2", fields2); // make sure it is saved as blank
		}
	}

	public boolean loadProperties() {
		showLatest = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showLatest");
		int windowX = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowX");
		int windowY = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowY");
		int windowWidth = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowWidth");
		int windowHeight = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowHeight");
		if (windowX == 0 ||windowY == 0 ||windowWidth == 0 ||windowHeight == 0) {
			if (plotType == GraphFrame.EARTH_PLOT)
				setBounds(100, 100, 1000, 500);
			else
				setBounds(100, 100, 740, 400);
		} else {
			if (plotType == GraphFrame.EARTH_PLOT)
				setBounds(windowX, windowY, windowWidth, (int)windowWidth/2);
			else
				setBounds(windowX, windowY, windowWidth, windowHeight);

		}

		this.SAMPLES = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "numberOfSamples");
		if (SAMPLES == 0) SAMPLES = DEFAULT_SAMPLES;
		if (SAMPLES > MAX_SAMPLES) {
			SAMPLES = MAX_SAMPLES;
		}
			
		this.START_RESET = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fromReset");
		this.START_UPTIME = Config.loadGraphLongValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fromUptime");
		this.END_RESET = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "toReset");
		this.END_UPTIME = Config.loadGraphLongValue(fox.getIdString(), plotType, payloadType, fieldName[0], "toUptime");
		
		this.START_UTC = Config.loadGraphValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fromUtc");
		if (START_UTC == null) START_UTC = DEFAULT_START_UTC;
		this.END_UTC = Config.loadGraphValue(fox.getIdString(), plotType, payloadType, fieldName[0], "toUtc");
		if (END_UTC == null) END_UTC = DEFAULT_END_UTC;
		
		boolean open = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "open");
		hideMain = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "hideMain");
		hideLines = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "hideLines");
		hidePoints = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "hidePoints");
		plotDerivative = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "plotDerivative");
		dspAvg = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "dspAvg");
		showVerticalLines = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showVerticalLines");
		showHorizontalLines = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showHorizontalLines");
		showUTCtime = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showUTCtime");
		hideUptime = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "hideUptime");
		roundLabels = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "roundLabels");
		showSun = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showSun");
		//plotType = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "plotType");
		
		AVG_PERIOD = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "AVG_PERIOD");
		if (AVG_PERIOD == 0) AVG_PERIOD = DEFAULT_AVG_PERIOD;
		showContinuous = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showContinuous");
		if (showContinuous) UPTIME_THRESHOLD = CONTINUOUS_UPTIME_THRESHOLD; else UPTIME_THRESHOLD = DEFAULT_UPTIME_THRESHOLD;
		mapType = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "mapType");
		if (mapType == 0) // 0 is not a valid value
			mapType = LINE_MAP_EQUIRECTANGULAR;
		
		String fields1 = Config.loadGraphValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fieldName");
		if (fields1 != null)
			fieldName = fields1.split(";");
		String fields2 = Config.loadGraphValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fieldName2");
		if (fields2 != null && !fields2.equalsIgnoreCase("")) {
			fieldName2 = fields2.split(";");
			fieldUnits2 = layout.getUnitsByName(fieldName2[0]);
		}
		return open;
	}

	
	@Override
	public void windowActivated(WindowEvent e) {
		
		
	}


	@Override
	public void windowClosed(WindowEvent e) {
		saveProperties(false);
	}


	@Override
	public void windowClosing(WindowEvent e) {
		//saveProperties(false);
	}


	@Override
	public void windowDeactivated(WindowEvent e) {
		
		
	}


	@Override
	public void windowDeiconified(WindowEvent e) {
		
		
	}


	@Override
	public void windowIconified(WindowEvent e) {
		
		
	}


	@Override
	public void windowOpened(WindowEvent e) {
		
		
	}

	private void parseAvgPeriod() {
		String text = txtAvgPeriod.getText();
		try {
			AVG_PERIOD = Integer.parseInt(text);
			if (AVG_PERIOD > MAX_AVG_SAMPLES) {
				AVG_PERIOD = MAX_AVG_SAMPLES;
				text = Integer.toString(MAX_AVG_SAMPLES);
				txtAvgPeriod.setText(text);
			}
		} catch (NumberFormatException ex) {
			
		}
		if (textDisplay)
			diagnosticTable.updateData();
		else
			panel.updateGraphData(layout, "GraphFrame.parseAvgPeriod");
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

	private void parseUTCFields() {
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
	
	private void parseTextFields() {
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
		if (textDisplay)
			diagnosticTable.updateData();
		else
			panel.updateGraphData(layout, "GraphFrame:parseTextFields");

	}
	
	private void toggleSunCheckBox() {
		if (!textDisplay)
			if (!(plotType == SKY_PLOT || plotType == EARTH_PLOT))
				if (cbShowSun !=null)
				if (Config.foxTelemCalcsPosition) {
					cbShowSun.setSelected(showSun);
					cbShowSun.setEnabled(true);
				} else {
					cbShowSun.setSelected(false);
					cbShowSun.setEnabled(false);
				}
	}

	private void setRedOutline(JButton but, boolean red) {
		if (red) {	
			//but.setBackground(Color.RED);
			but.setForeground(Color.BLACK);
			but.setBackground(Color.GRAY);
		} else
			but.setForeground(Color.GRAY);
			but.setBackground(Color.WHITE);
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
			SAMPLES = Config.payloadStore.getNumberOfPayloadsBetweenTimestamps(fox.foxId, START_RESET, START_UPTIME, END_RESET, END_UPTIME, layout.name);
			txtSamplePeriod.setText(Integer.toString(SAMPLES));
		}
	}

	private void convertToUptime() {
		parseUTCFields();
		textFromReset.setText(Integer.toString(START_RESET));
		textFromUptime.setText(Long.toString(START_UPTIME));
		textToReset.setText(Integer.toString(END_RESET));
		textToUptime.setText(Long.toString(END_UPTIME));
		if (showLatest == SHOW_RANGE) {
			SAMPLES = Config.payloadStore.getNumberOfPayloadsBetweenTimestamps(fox.foxId, START_RESET, START_UPTIME, END_RESET, END_UPTIME, layout.name);
			txtSamplePeriod.setText(Integer.toString(SAMPLES));

		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnAdd) {
			add = !add;
			cbAddVariable.setVisible(add);
		} else
			if (e.getSource() == cbAddVariable) {
			// Add or remove a variable to be plotted on the graph
			int position = cbAddVariable.getSelectedIndex();
			if (position == -1) return;
			if (fieldName[0].equalsIgnoreCase(variables.get(position))) return; // can't toggle the main value
			
			int fields = fieldName.length;
			int fields2 = 0;;
			String[] temp = new String[fields];
			String[] temp2 = null;
			int i = 0;
			boolean toggle = false;
			boolean toggle2 = false;
//			BitArrayLayout layout = getLayout(payloadType);
			int totalVariables = fieldName.length;
			
			for (String s: fieldName) {
				temp[i++] = s;
				if (s.equalsIgnoreCase(variables.get(position))) toggle=true;
			}
			int j = 0;
			if (fieldName2 != null && fieldName2.length > 0) {
				fields2 = fieldName2.length;
				temp2 = new String[fields2];
				for (String s: fieldName2) {
					temp2[j++] = s;
					if (s.equalsIgnoreCase(variables.get(position))) toggle2=true;
				}
				totalVariables += fieldName2.length;
			}
			if (toggle && fieldName.length > 1) {
				// we remove this entry
				fieldName = new String[fields-1];
				i=0;
				for (String s: temp)
					if (!s.equalsIgnoreCase(variables.get(position)))
						fieldName[i++] = s;
				
			} else if (toggle2 && fieldName2.length > 0) {
				// we remove this entry
				if (fieldName2.length == 1) {
					fieldName2 = null;
					fieldUnits2 = "";
					fields2 = 0;
				} else {
					fieldName2 = new String[fields2-1];
					i=0;
					for (String s: temp2)
						if (!s.equalsIgnoreCase(variables.get(position)))
							fieldName2[i++] = s;
				}
			} else {
				if (totalVariables == GraphPanel.MAX_VARIABLES) return; // can't add more variables than the graphPanel can plot
				// Check if this is the same units, otherwise set this as unit2
				String unit = layout.getUnitsByName(variables.get(position));
				if (!unit.equalsIgnoreCase(fieldUnits)) {
					fieldUnits2 = unit;
					conversionType2 = layout.getIntConversionByName(variables.get(position));
					conversion2 = fox.getConversionByName(variables.get(position));
					// we add it to the second list as the units are different
					fieldName2 = new String[fields2+1];
					i=0;
					if (temp2 != null) // then add what we have so far
						for (String s: temp2)
							fieldName2[i++] = s;
					fieldName2[i] = variables.get(position);
				} else {
					// we add it to the normal list
					fieldName = new String[fields+1];
					i=0;
					for (String s: temp)
						fieldName[i++] = s;
					fieldName[i] = variables.get(position);
				}


			}
			
			// now rebuild the pick list
			ArrayList<String> labels = new ArrayList<String>();
			variables = new ArrayList<String>();
			
			for (int v=0; v<layout.fieldName.length; v++) {
				if (!layout.module[v].equalsIgnoreCase(BitArrayLayout.NONE))
				if (layout.fieldUnits[v].equalsIgnoreCase(fieldUnits) || layout.fieldUnits[v].equalsIgnoreCase(fieldUnits2)
						|| fieldUnits2.equalsIgnoreCase("")) {
					variables.add(layout.fieldName[v]);
					labels.add(layout.module[v] + "-" + layout.shortName[v]);
				}
			}
		
			cbAddVariable.removeAllItems();
			for (String s:labels) {
				cbAddVariable.addItem(s);
			}
			
			if (fieldName.length > 1 || fieldName2 != null)
				;
			else {
				add = false;
				cbAddVariable.setVisible(add);
			}
			calcTitle();
			panel.updateGraphData(layout, "GraphFrame:stateChange:addVariable");
		}
		else if (e.getSource() == this.txtSamplePeriod) {
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
			
		} else if (e.getSource() == this.txtAvgPeriod) {
				parseAvgPeriod();
		} else if (e.getSource() == this.btnLatest) {
			showLatest++;
			if (showLatest > SHOW_RANGE)
				showLatest = SHOW_LIVE;
			showRangeSearch(showLatest);
			parseFields();
			/*
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData("GraphFrame.btnLatest");
			*/
			
		} else if (e.getSource() == btnDefault) { // This is now called reset on the graph and also resets the averaging
			if (!textDisplay) {
				if (plotType == SKY_PLOT)
					initSkyPlotFields();
				else if (plotType == EARTH_PLOT)
					initEarthPlotFields();
				else {
					fieldUnits2 = "";
					fieldName2 = null;
					String name = fieldName[0];
					fieldName = new String[1];
					fieldName[0] = name;
				}
				if (!(plotType == SKY_PLOT || plotType == EARTH_PLOT)) {
					initVarlist();
					add = false;
					cbAddVariable.setVisible(add);
				}
				calcTitle();
			}
			showLatest = SHOW_LIVE;
			showRangeSearch(showLatest);
			//textFromReset.setText(Integer.toString(DEFAULT_START_RESET));
			//textFromUptime.setText(Long.toString(DEFAULT_START_UPTIME));
			//textToReset.setText(Integer.toString(DEFAULT_START_RESET));
			//textToUptime.setText(Long.toString(DEFAULT_START_UPTIME));
			textFromUtc.setText(DEFAULT_START_UTC);
			textToUtc.setText(DEFAULT_END_UTC);
			txtSamplePeriod.setText(Integer.toString(DEFAULT_SAMPLES));
			parseFields();
			if (!(plotType == SKY_PLOT || plotType == EARTH_PLOT)) {
				txtAvgPeriod.setText(Integer.toString(DEFAULT_AVG_PERIOD));
				parseAvgPeriod();
			}
			
		} else if (e.getSource() == btnCSV) {
			File file = null;
			File dir = null;
			if (!Config.csvCurrentDirectory.equalsIgnoreCase("")) {
				dir = new File(Config.csvCurrentDirectory);
			}
			if(Config.useNativeFileChooser) {
				// use the native file dialog on the mac
				FileDialog fd =
						new FileDialog(this, "Choose or enter CSV file to save graph results",FileDialog.SAVE);
				if (dir != null) {
					fd.setDirectory(dir.getAbsolutePath());
				}
	//			FilenameFilter filter = new FilenameFilter("CSV Files", "csv", "txt");
	//			fd.setFilenameFilter(filter);
				fd.setVisible(true);
				String filename = fd.getFile();
				String dirname = fd.getDirectory();
				if (filename == null)
					Log.println("You cancelled the choice");
				else {
					Log.println("File: " + filename);
					Log.println("DIR: " + dirname);
					file = new File(dirname + filename);
				}
			} else {

				JFileChooser fc = new JFileChooser();
				fc.setApproveButtonText("Save");
				if (dir != null) {
					fc.setCurrentDirectory(dir);	
				}				
				FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv", "txt");
				fc.setFileFilter(filter);
				fc.setDialogTitle("Choose or enter CSV file to save graph results");
				fc.setPreferredSize(new Dimension(Config.windowFcWidth, Config.windowFcHeight));
				int returnVal = fc.showOpenDialog(this);
				Config.windowFcHeight = fc.getHeight();
				Config.windowFcWidth = fc.getWidth();		


				if (returnVal == JFileChooser.APPROVE_OPTION) { 
					file = fc.getSelectedFile();
				}
			}
			if (file != null) {
				Config.csvCurrentDirectory = file.getParent();
				try {
					saveToCSV(file);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(MainWindow.frame,
							e1.toString(),
							"ERROR WRITING FILE",
							JOptionPane.ERROR_MESSAGE) ;

					e1.printStackTrace(Log.getWriter());
				}
			} else {
				System.out.println("No Selection ");
			}
			Config.save();
		}  else if (e.getSource() == btnCopy) {
			copyToClipboard();
			Log.println("Graph copied to clipboard");
		}  else if (e.getSource() == btnHorizontalLines) {
			showHorizontalLines = !showHorizontalLines;
			//Log.println("Plot Derivative " + plotDerivative);
			setRedOutline(btnHorizontalLines,showHorizontalLines);
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:horizontal");
		} else if (e.getSource() == btnVerticalLines) {
			showVerticalLines = !showVerticalLines;
			//Log.println("Plot Derivative " + plotDerivative);
			setRedOutline(btnVerticalLines,showVerticalLines);
/*			if (showVerticalLines) {	
				btnVerticalLines.setBackground(Color.RED);
			} else
				btnVerticalLines.setBackground(Color.GRAY);
				*/
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:vertical");
		} else if (e.getSource() == btnDerivative) {
			plotDerivative = !plotDerivative;
			//Log.println("Plot Derivative " + plotDerivative);
			setRedOutline(btnDerivative,plotDerivative);
			
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:derivative");
		}  else if (e.getSource() == btnAvg) {
			dspAvg = !dspAvg;
			setRedOutline(btnAvg,dspAvg);
			setAvgVisible(dspAvg);
			//Log.println("Calc Average " + dspAvg);
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:avg");
		} else if (e.getSource() == btnMain) {
			hideMain = !hideMain;
			setRedOutline(btnMain,hideMain);

			panel.updateGraphData(layout, "GraphFrame.actionPerformed:main");
		}  else if (e.getSource() == btnLines) {
			hideLines = !hideLines;
			setRedOutline(btnLines,!hideLines);
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:hideLines");
		} else if (e.getSource() == btnPoints) {
			hidePoints = !hidePoints;
			setRedOutline(btnPoints,!hidePoints);
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:points");
		}  else if (e.getSource() == btnMapType) {
			mapType = mapType + 1;
			if (mapType > COLOR_MAP_EQUIRECTANGULAR)
				mapType = NO_MAP_EQUIRECTANGULAR;
			panel.updateGraphData(layout, "GraphFrame.actionPerformed:points");
		} 
		if (e.getSource() == cbUptime) {
				hideUptime = !hideUptime;
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData(layout, "GraphFrame:stateChange:Uptime");
			setRedOutline(cbUptime, !hideUptime);
		}
		if (e.getSource() == chckbxPlotAllUptime) {
			showContinuous = !showContinuous;
			if (showContinuous) {
				UPTIME_THRESHOLD = CONTINUOUS_UPTIME_THRESHOLD;
			} else {
				UPTIME_THRESHOLD = DEFAULT_UPTIME_THRESHOLD;				
			}
			setRedOutline(chckbxPlotAllUptime,showContinuous);
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData(layout, "GraphFrame:stateChange:plotAllUptime");
		}
		
		if (e.getSource() == cbUTC) {
			showUTCtime = !showUTCtime;
			if (showUTCtime) {
				parseTextFields();
				//textToUtc.setText();
				txtSamplePeriod.setText(Integer.toString(SAMPLES));
				
			} else {
				parseUTCFields();
				txtSamplePeriod.setText(Integer.toString(SAMPLES));
			}
			showUptimeQuery(!showUTCtime);
			setRedOutline(cbUTC,showUTCtime);

			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData(layout, "GraphFrame:stateChange:UTC");
		}
		
		toggleSunCheckBox();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == cbRoundLabels) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				roundLabels = false;
			} else {
				roundLabels = true;
			}
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData(layout, "GraphFrame:stateChange:altLabels");
		}
		if (e.getSource() == cbShowSun) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				showSun = false;
			} else {
				showSun = true;
			}
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData(layout, "GraphFrame:stateChange:altLabels");
		}
		if (e.getSource() == cbUptime) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				hideUptime = true;
			} else {
				hideUptime = false;
			}
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData(layout, "GraphFrame:stateChange:Uptime");
		}		
		toggleSunCheckBox();
	}

	private void saveToCSV(File aFile) throws IOException {
		double[][][] graphData = panel.graphData;
		double[][][] graphData2 = panel.graphData2;


		if (graphData != null) {
			if(!aFile.exists()){
				aFile.createNewFile();
			} else {

			}
			Writer output = new BufferedWriter(new FileWriter(aFile, false));

			// Write the data to the file.  Reset, Uptime, Value.
			// If there are multiple variabes then we write the rest of the values on the same line into subsequent columns
			// First write a header row
			String h;
			if (this.showUTCtime)
				h="UTC";
			else
				h= "epoch, uptime";
			if (plotType == EARTH_PLOT)
				h= h+",lat, lon";
			for (int j=0; j < fieldName.length; j++)				
				h = h + ", " + fieldName[j] ;
			if (fieldName2 != null)
			for (int j=0; j < fieldName2.length; j++)				
				h = h + ", " + fieldName2[j] ;
			output.write(h + "\n");
			
			for (int i=0; i< graphData[0][0].length; i++) {
				String s;
				if (this.showUTCtime /* && fox.isFox1() */) {
					Spacecraft fox2 = fox;
					if (fox2.hasTimeZero((int)graphData[0][PayloadStore.RESETS_COL][i]))
						s = fox2.getUtcDateForReset((int)graphData[0][PayloadStore.RESETS_COL][i], (long)graphData[0][PayloadStore.UPTIME_COL][i]) 
						+ " " + fox2.getUtcTimeForReset((int)graphData[0][PayloadStore.RESETS_COL][i], (long)graphData[0][PayloadStore.UPTIME_COL][i]);
					else
						s = "??"; 
				} else
					s = graphData[0][PayloadStore.RESETS_COL][i] + ", " +  // can always read reset and uptime from field 0
						graphData[0][PayloadStore.UPTIME_COL][i] ;
				if (plotType == EARTH_PLOT)
					s = s + "," + graphData[0][PayloadStore.LAT_COL][i] + ", " + 
							graphData[0][PayloadStore.LON_COL][i] ;
				for (int j=0; j < fieldName.length; j++)				
						s = s + ", " + graphData[j][PayloadStore.DATA_COL][i] ;
				if (graphData2 != null)
				for (int j=0; j < fieldName2.length; j++)				
					s = s + ", " + graphData2[j][PayloadStore.DATA_COL][i] ;
				s=s+ "\n";
				output.write(s); 
			}
			
			output.flush();
			output.close();

		}
		
	}
	
	/*
	 * Copy the graph panel to the clipboard
	 */
	public void copyToClipboard() {
		
		BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB); // Omit transparency as clipboard copy does not support it
		Graphics imgGraphics = img.getGraphics(); // grab the graphics "pen" for the image
		panel.printAll(imgGraphics); // write the data to the image
		
		write(img);	
	}
	
	 /**
     *  Image to system clipboard
     *  @param  image - the image to be added to the system clipboard
     */
    public static void write(Image image)
    {
        if (image == null)
            throw new IllegalArgumentException ("Image can't be null");

        ImageTransferable transferable = new ImageTransferable( image );
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
    }

    static class ImageTransferable implements Transferable
    {
        private Image image;

        public ImageTransferable (Image image)
        {
            this.image = image;
        }

        public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException
        {
            if (isDataFlavorSupported(flavor))
            {
                return image;
            }
            else
            {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public boolean isDataFlavorSupported (DataFlavor flavor)
        {
            return flavor == DataFlavor.imageFlavor;
        }

        public DataFlavor[] getTransferDataFlavors ()
        {
            return new DataFlavor[] { DataFlavor.imageFlavor };
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

	
	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void componentResized(ComponentEvent arg0) {
		int MAX_WIDTH = 1486;
		if (plotType == EARTH_PLOT){
			int W = 4;  
			int H = 2;  
			int width = 0;
			Rectangle b = arg0.getComponent().getBounds();
			width = b.width;
			if (width > MAX_WIDTH)
				width = MAX_WIDTH;
			arg0.getComponent().setBounds(b.x, b.y, width, width*H/W);
		//	Log.println("WH:" + b.width + " " + b.height);
		}
		toggleSunCheckBox();
	}

	@Override
	public void componentShown(ComponentEvent arg0) {

		
	}

}
