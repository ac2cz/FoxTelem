package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.ArrayList;

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
import telemetry.FoxFramePart;
import telemetry.PayloadStore;
import common.Config;
import common.Log;
import common.Spacecraft;
import common.FoxSpacecraft;
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
public class GraphFrame extends JFrame implements WindowListener, ActionListener, ItemListener, FocusListener {

	public String[] fieldName;
	public String[] fieldName2;
	String fieldUnits = "";
	String fieldUnits2 = "";
	String displayTitle; // the actual title of the graph - calculated
	String title; // the title of the module, e.g. Computer - passed in
	BitArrayLayout layout;
	private int payloadType;
	int conversionType;
	int conversionType2;
	private JPanel contentPane;
	private GraphCanvas panel;
	private JPanel titlePanel;
	private JPanel footerPanel;
	
	private JButton btnLatest;
	private JButton btnVerticalLines;
	private JButton btnHorizontalLines;
	private JButton btnCSV;
	private JButton btnCopy;
	private JButton btnDerivative;
	private JButton btnMain;
	private JButton btnAvg;
	private JButton btnLines;
	private JButton btnPoints;
	private JCheckBox cbUTC;
	private JCheckBox cbUptime;
	@SuppressWarnings("rawtypes")
	private JComboBox cbAddVariable;
	private ArrayList<String> variables;
	
	public Spacecraft fox;
	public static int DEFAULT_SAMPLES = 180;
	public int SAMPLES = DEFAULT_SAMPLES;
	public static long DEFAULT_START_UPTIME = 0;
	public static int DEFAULT_START_RESET = 0;
	public long START_UPTIME = DEFAULT_START_UPTIME;
	public int START_RESET = DEFAULT_START_RESET;
	public static final int MAX_SAMPLES = 999999;
	public static final int MAX_AVG_SAMPLES = 999;
	public static int DEFAULT_AVG_PERIOD = 12;
	public int AVG_PERIOD = DEFAULT_AVG_PERIOD;
	//private JLabel lblActual;
	public static final int DEFAULT_UPTIME_THRESHOLD = 60*60*1;// plot continuous uptime unless more than 1 hour gap
	public static final int CONTINUOUS_UPTIME_THRESHOLD = -1;
	public double UPTIME_THRESHOLD =DEFAULT_UPTIME_THRESHOLD; 
	private JCheckBox chckbxPlotAllUptime;
	private JLabel lblFromUptime;
	private JTextField textFromUptime;
	private JLabel lblPlot;
	JLabel lblSamplePeriod; // The number of samples to grab for each graph
	private JTextField txtSamplePeriod;
	private JLabel lblAvg;
	JLabel lblAvgPeriod; 
	private JTextField txtAvgPeriod;
	private JLabel lblFromReset;
	private JTextField textFromReset;
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
	public boolean hidePoints = true;
	public boolean hideLines = true;
	public boolean showContinuous = false;
	
	public boolean add = false;
	public final static int GRAPH_PLOT = 0;
	public final static int SKY_PLOT = 1;
	public final static int EARTH_PLOT = 2;
	public final static int SAVED_PLOT = 99;
	public final static int MAX_PLOT_TYPES = 3;
	
	public int plotType = SAVED_PLOT;
	
	boolean textDisplay = false;
	
	/**
	 * Create the frame.
	 */
	@SuppressWarnings("rawtypes")
	public GraphFrame(String title, String fieldName, String fieldUnits, int conversionType, int plType, Spacecraft fox2, int plot) {
		fox = fox2;
		this.fieldName = new String[1];
		this.fieldName[0] = fieldName;
		this.fieldUnits = fieldUnits;
		this.title = title;
		this.conversionType = conversionType;
		
		layout = getLayout(plType);
		
		payloadType = plType;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(this);
		
		
		if (plot != SAVED_PLOT) // take the value, otherwise we use what was loaded from the save
			plotType = plot;
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
			diagnosticTable = new DiagnosticTable(title, fieldName, conversionType, this, (FoxSpacecraft)fox);
			contentPane.add(diagnosticTable, BorderLayout.CENTER);
			textDisplay = true;
		} else if (plotType == SKY_PLOT){
			initSkyPlotFields();
			panel = new DensityPlotPanel(title, conversionType, payloadType, this, (FoxSpacecraft)fox2);
			contentPane.add(panel, BorderLayout.CENTER);
		} else if (plotType == EARTH_PLOT){
			panel = new EarthPlotPanel(title, conversionType, payloadType, this, (FoxSpacecraft)fox2);
			contentPane.add(panel, BorderLayout.CENTER);
		} else {
			panel = new GraphPanel(title, conversionType, payloadType, this, fox2);
			contentPane.add(panel, BorderLayout.CENTER);
		}

		if (!(textDisplay || plotType == SKY_PLOT)) {
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
		btnLines = new JButton("Lines");
		btnLines.setMargin(new Insets(0,0,0,0));
		btnLines.setToolTipText("Draw lines between data points");
		btnLines.addActionListener(this);
		titlePanelRight.add(btnLines);
		if (this.textDisplay || plotType == SKY_PLOT) btnLines.setVisible(false);

		btnPoints = new JButton("Points");
		btnPoints.setMargin(new Insets(0,0,0,0));
		btnPoints.setToolTipText("Show data points");
		btnPoints.addActionListener(this);
		titlePanelRight.add(btnPoints);
		if (this.textDisplay || plotType == SKY_PLOT) btnPoints.setVisible(false);

		
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
		if (this.textDisplay || plotType == SKY_PLOT) btnMain.setVisible(false);

		btnDerivative = createIconButton("/images/derivSmall.png","Deriv","Plot 1st Derivative (1st difference)");
		titlePanelRight.add(btnDerivative);
		if (this.textDisplay || plotType == SKY_PLOT) btnDerivative.setVisible(false);

		btnAvg = new JButton("AVG");
		btnAvg.setMargin(new Insets(0,0,0,0));
		btnAvg.setToolTipText("Running Average / Low Pass Filter");
		btnAvg.addActionListener(this);
		
		titlePanelRight.add(btnAvg);
		if (this.textDisplay || plotType == SKY_PLOT) btnAvg.setVisible(false);

		if (conversionType == BitArrayLayout.CONVERT_STATUS_BIT || conversionType == BitArrayLayout.CONVERT_ANTENNA || 
				conversionType == BitArrayLayout.CONVERT_BOOLEAN ) {
			btnDerivative.setVisible(false);
			btnAvg.setVisible(false);
		}

		setRedOutline(btnMain, hideMain);
		setRedOutline(btnLines, hideLines);
		setRedOutline(btnPoints, hidePoints);
		setRedOutline(btnDerivative, plotDerivative);
		setRedOutline(btnAvg, dspAvg);
		setRedOutline(btnHorizontalLines, showHorizontalLines);
		setRedOutline(btnVerticalLines, showVerticalLines);

		btnLatest = createIconButton("/images/refreshSmall.png","Reset","Reset to default range and show latest data");
		titlePanelRight.add(btnLatest);
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
		JPanel footerPanelLeft = new JPanel();
		JPanel footerPanelFarLeft = new JPanel();
		JPanel footerPanelRight = new JPanel();
		footerPanel.add(footerPanelLeft, BorderLayout.EAST);
		footerPanel.add(footerPanelRight, BorderLayout.CENTER);
		footerPanel.add(footerPanelFarLeft, BorderLayout.WEST);

		if (!(plotType == SKY_PLOT)) {
			cbUptime = new JCheckBox("Show Uptime");
			cbUptime.setSelected(!hideUptime);
			cbUptime.addItemListener(this);
			footerPanelLeft.add(cbUptime);

			cbUTC = new JCheckBox("UTC Time   |");
			cbUTC.setSelected(showUTCtime);
			cbUTC.addItemListener(this);
			footerPanelLeft.add(cbUTC);

			lblAvg = new JLabel("Avg");
			txtAvgPeriod = new JTextField();
			//		txtSamplePeriod.setPreferredSize(new Dimension(30,14));
			txtAvgPeriod.addActionListener(this);
			txtAvgPeriod.addFocusListener(this);
			lblAvgPeriod = new JLabel("samples  ");

			setAvgVisible(dspAvg);

			footerPanelLeft.add(lblAvg);
			footerPanelLeft.add(txtAvgPeriod);
			footerPanelLeft.add(lblAvgPeriod);
			txtAvgPeriod.setText(Integer.toString(AVG_PERIOD));
			txtAvgPeriod.setColumns(3);
		}
		lblPlot = new JLabel("Plot");
		txtSamplePeriod = new JTextField();
//		txtSamplePeriod.setPreferredSize(new Dimension(30,14));
		txtSamplePeriod.addActionListener(this);
		txtSamplePeriod.addFocusListener(this);
		txtSamplePeriod.setToolTipText("The number of data samples to plot.  The latest samples are returned unless a from reset/uptime is specified");

		
		footerPanelLeft.add(lblPlot);
		footerPanelLeft.add(txtSamplePeriod);
		lblSamplePeriod = new JLabel("samples");
		footerPanelLeft.add(lblSamplePeriod);
		txtSamplePeriod.setText(Integer.toString(SAMPLES));
		txtSamplePeriod.setColumns(6);
		//lblActual = new JLabel("(180)");
		//footerPanel.add(lblActual);
		
		lblFromReset = new JLabel("        from Reset");
		footerPanelLeft.add(lblFromReset);
		
		textFromReset = new JTextField();
		footerPanelLeft.add(textFromReset);
//		if (START_RESET == 0)
//			textFromReset.setText("Last");
//		else
			textFromReset.setText(Integer.toString(START_RESET));

		textFromReset.setColumns(8);
//		textFromReset.setPreferredSize(new Dimension(50,14));
		textFromReset.addActionListener(this);
		textFromReset.addFocusListener(this);
		
		lblFromUptime = new JLabel(" from Uptime");
		footerPanelLeft.add(lblFromUptime);
		
		textFromUptime = new JTextField();
		footerPanelLeft.add(textFromUptime);
//		if (START_UPTIME == 0)
//			textFromUptime.setText("Last");
//		else
			textFromUptime.setText(Long.toString(START_UPTIME));
		textFromUptime.setColumns(8);
//		textFromUptime.setPreferredSize(new Dimension(50,14));
		textFromUptime.addActionListener(this);
		textFromUptime.addFocusListener(this);
		
		if (!(plotType == SKY_PLOT || textDisplay)) {
			chckbxPlotAllUptime = new JCheckBox("Continuous");
			chckbxPlotAllUptime.setToolTipText("");
			footerPanelLeft.add(chckbxPlotAllUptime);
			chckbxPlotAllUptime.setSelected(showContinuous);

			chckbxPlotAllUptime.addItemListener(this);
			chckbxPlotAllUptime.setToolTipText("Show all uptime values, even if there is no data to plot");
			
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
		if (conversionType == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC || conversionType == BitArrayLayout.CONVERT_HARD_ERROR || 
				conversionType == BitArrayLayout.CONVERT_SOFT_ERROR || conversionType == BitArrayLayout.CONVERT_ICR_DIAGNOSTIC)
			return true;
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initVarlist() {
		variables = new ArrayList<String>();
		for (int v=0; v<layout.fieldName.length; v++) {
			if (!layout.module[v].equalsIgnoreCase(BitArrayLayout.NONE))
				variables.add(layout.fieldName[v]);
		}
		Object[] fields = variables.toArray();
		cbAddVariable.removeAllItems();
		cbAddVariable.setModel(new DefaultComboBoxModel(fields));
	}
	
	// FIXME - if we pass in the layout, then we would not need this lookup.  This logic SHOULD NOT BE HERE!
	// Need to pass layout into DisplayModule and then to here
	private BitArrayLayout getLayout(int plType) {
		BitArrayLayout layout = null;
		if (plType == FoxFramePart.TYPE_REAL_TIME)
			layout = fox.getLayoutByName(Spacecraft.REAL_TIME_LAYOUT);
		else if (plType == FoxFramePart.TYPE_MAX_VALUES)
			layout = fox.getLayoutByName(Spacecraft.MAX_LAYOUT);
		else if (plType == FoxFramePart.TYPE_MIN_VALUES)
			layout = fox.getLayoutByName(Spacecraft.MIN_LAYOUT);
		else if (plType == FoxFramePart.TYPE_RAD_TELEM_DATA)
			layout = fox.getLayoutByName(Spacecraft.RAD2_LAYOUT);
		else if (plType == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER)
			layout = fox.getLayoutByName(Spacecraft.HERCI_HS_HEADER_LAYOUT);
		else if (plType == SatMeasurementStore.RT_MEASUREMENT_TYPE)
			layout = fox.measurementLayout;
		else if (plType == SatMeasurementStore.PASS_MEASUREMENT_TYPE)
			layout = fox.passMeasurementLayout;
		else if (plType == FoxFramePart.TYPE_WOD)
			layout = fox.getLayoutByName(Spacecraft.WOD_LAYOUT);
		else if (plType == FoxFramePart.TYPE_WOD_RAD)
			layout = fox.getLayoutByName(Spacecraft.WOD_RAD_LAYOUT);
		return layout;
	}
	
	private void calcTitle() {
		//BitArrayLayout layout = getLayout(payloadType);
		if (!(plotType == SKY_PLOT) && (fieldName.length > 1 || fieldName2 != null))
			displayTitle = fox.name;
		else {
			displayTitle = title; // + " - " + layout.getShortNameByName(fieldName[0]) + "(" + layout.getUnitsByName(fieldName[0])+ ")";
			if (payloadType != SatMeasurementStore.RT_MEASUREMENT_TYPE &&
					payloadType !=SatMeasurementStore.PASS_MEASUREMENT_TYPE) // measurement
				displayTitle = fox.name + " " + displayTitle;
			if (conversionType == BitArrayLayout.CONVERT_FREQ) {
				int freqOffset = fox.telemetryDownlinkFreqkHz;
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
			panel.updateGraphData("GraphFrame.updateGraphData");
	}

	/**
	 * Save properties that are not captured realtime.  This is mainly generic properties such as the size of the
	 * window that are not tied to a control that we have added.
	 */
	public void saveProperties(boolean open) {
		//Log.println("Saving graph properties: " + fieldName);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowHeight", this.getHeight());
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowWidth", this.getWidth());
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowX", this.getX());
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "windowY", this.getY());
		
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "numberOfSamples", this.SAMPLES);
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fromReset", this.START_RESET);
		Config.saveGraphLongParam(fox.getIdString(), plotType, payloadType, fieldName[0], "fromUptime", this.START_UPTIME);
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
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "plotType", plotType);
		
		Config.saveGraphIntParam(fox.getIdString(), plotType, payloadType, fieldName[0], "AVG_PERIOD", AVG_PERIOD);
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
		int windowX = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowX");
		int windowY = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowY");
		int windowWidth = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowWidth");
		int windowHeight = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "windowHeight");
		if (windowX == 0 ||windowY == 0 ||windowWidth == 0 ||windowHeight == 0)
			setBounds(100, 100, 740, 400);
		else
			setBounds(windowX, windowY, windowWidth, windowHeight);

		this.SAMPLES = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "numberOfSamples");
		if (SAMPLES == 0) SAMPLES = DEFAULT_SAMPLES;
		if (SAMPLES > MAX_SAMPLES) {
			SAMPLES = MAX_SAMPLES;
		}
			
		this.START_RESET = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fromReset");
		this.START_UPTIME = Config.loadGraphLongValue(fox.getIdString(), plotType, payloadType, fieldName[0], "fromUptime");
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
		//plotType = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "plotType");
		
		AVG_PERIOD = Config.loadGraphIntValue(fox.getIdString(), plotType, payloadType, fieldName[0], "AVG_PERIOD");
		if (AVG_PERIOD == 0) AVG_PERIOD = DEFAULT_AVG_PERIOD;
		showContinuous = Config.loadGraphBooleanValue(fox.getIdString(), plotType, payloadType, fieldName[0], "showContinuous");
		if (showContinuous) UPTIME_THRESHOLD = CONTINUOUS_UPTIME_THRESHOLD; else UPTIME_THRESHOLD = DEFAULT_UPTIME_THRESHOLD;
		
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
			panel.updateGraphData("GraphFrame.parseAvgPeriod");
	}
	
	private void parseTextFields() {
		String text = txtSamplePeriod.getText();
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
		text = textFromReset.getText();
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
		if (textDisplay)
			diagnosticTable.updateData();
		else
			panel.updateGraphData("GraphFrame.parseTextFields");
	}
	
	private void setRedOutline(JButton but, boolean red) {
		if (red) {	
			but.setBackground(Color.RED);
		} else
			but.setBackground(Color.GRAY);
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
			BitArrayLayout layout = getLayout(payloadType);
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
					conversionType2 = layout.getConversionByName(variables.get(position));
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
			variables = new ArrayList<String>();
			
			for (int v=0; v<layout.fieldName.length; v++) {
				if (!layout.module[v].equalsIgnoreCase(BitArrayLayout.NONE))
				if (layout.fieldUnits[v].equalsIgnoreCase(fieldUnits) || layout.fieldUnits[v].equalsIgnoreCase(fieldUnits2)
						|| fieldUnits2.equalsIgnoreCase(""))
					variables.add(layout.fieldName[v]);
			}
		
			cbAddVariable.removeAllItems();
			for (String s:variables) {
				cbAddVariable.addItem(s);
			}
			
			if (fieldName.length > 1 || fieldName2 != null)
				;
			else {
				add = false;
				cbAddVariable.setVisible(add);
			}
			calcTitle();
			panel.updateGraphData("GraphFrame:stateChange:addVariable");
		}
		else if (e.getSource() == this.txtSamplePeriod) {
			parseTextFields();
			
		} else if (e.getSource() == this.textFromReset) {
			parseTextFields();
			
		} else if (e.getSource() == this.textFromUptime) {
			parseTextFields();
			
		} else if (e.getSource() == this.txtAvgPeriod) {
				parseAvgPeriod();
		} else if (e.getSource() == btnLatest) { // This is now called reset on the graph and also resets the averaging
			if (!textDisplay) {
				if (plotType == SKY_PLOT)
					initSkyPlotFields();
				else {
					fieldUnits2 = "";
					fieldName2 = null;
					String name = fieldName[0];
					fieldName = new String[1];
					fieldName[0] = name;
				}
				if (!(plotType == SKY_PLOT)) {
					initVarlist();
					add = false;
					cbAddVariable.setVisible(add);
				}
				calcTitle();
			}
			textFromReset.setText(Long.toString(DEFAULT_START_UPTIME));
			textFromUptime.setText(Integer.toString(DEFAULT_START_RESET));
			txtSamplePeriod.setText(Integer.toString(DEFAULT_SAMPLES));
			
			parseTextFields();
			if (!(plotType == SKY_PLOT)) {
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
		}  else if (e.getSource() == btnCopy) {
			copyToClipboard();
			Log.println("Graph copied to clipboard");
		}  else if (e.getSource() == btnHorizontalLines) {
			showHorizontalLines = !showHorizontalLines;
			//Log.println("Plot Derivative " + plotDerivative);
			if (showHorizontalLines) {	
				btnHorizontalLines.setBackground(Color.RED);
			} else
				btnHorizontalLines.setBackground(Color.GRAY);
			panel.updateGraphData("GraphFrame.actionPerformed:horizontal");
		} else if (e.getSource() == btnVerticalLines) {
			showVerticalLines = !showVerticalLines;
			//Log.println("Plot Derivative " + plotDerivative);
			if (showVerticalLines) {	
				btnVerticalLines.setBackground(Color.RED);
			} else
				btnVerticalLines.setBackground(Color.GRAY);
			panel.updateGraphData("GraphFrame.actionPerformed:vertical");
		} else if (e.getSource() == btnDerivative) {
			plotDerivative = !plotDerivative;
			//Log.println("Plot Derivative " + plotDerivative);
			if (plotDerivative) {	
				btnDerivative.setBackground(Color.RED);
			} else
				btnDerivative.setBackground(Color.GRAY);
			panel.updateGraphData("GraphFrame.actionPerformed:derivative");
		}  else if (e.getSource() == btnAvg) {
			dspAvg = !dspAvg;
			if (dspAvg) {	
				btnAvg.setBackground(Color.RED);
			} else
				btnAvg.setBackground(Color.GRAY);
			setAvgVisible(dspAvg);
			//Log.println("Calc Average " + dspAvg);
			panel.updateGraphData("GraphFrame.actionPerformed:avg");
		} else if (e.getSource() == btnMain) {
			hideMain = !hideMain;
			if (!hideMain) {	
				btnMain.setBackground(Color.RED);
			} else
				btnMain.setBackground(Color.GRAY);

			panel.updateGraphData("GraphFrame.actionPerformed:main");
		}  else if (e.getSource() == btnLines) {
			hideLines = !hideLines;
			if (hideLines) {	
				btnLines.setBackground(Color.RED);
			} else
				btnLines.setBackground(Color.GRAY);

			panel.updateGraphData("GraphFrame.actionPerformed:hideLines");
		} else if (e.getSource() == btnPoints) {
			hidePoints = !hidePoints;
			if (hidePoints) {	
				btnPoints.setBackground(Color.RED);
			} else
				btnPoints.setBackground(Color.GRAY);

			panel.updateGraphData("GraphFrame.actionPerformed:points");
		} 

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == chckbxPlotAllUptime) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				showContinuous = false;
				UPTIME_THRESHOLD = DEFAULT_UPTIME_THRESHOLD;
			} else {
				showContinuous = true;
				UPTIME_THRESHOLD = CONTINUOUS_UPTIME_THRESHOLD;
			}
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData("GraphFrame:stateChange:plotAllUptime");
		}
		
		if (e.getSource() == cbUTC) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				showUTCtime = false;
			} else {
				showUTCtime = true;
			}
			if (textDisplay)
				diagnosticTable.updateData();
			else
				panel.updateGraphData("GraphFrame:stateChange:UTC");
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
				panel.updateGraphData("GraphFrame:stateChange:Uptime");
		}
		
	}

	private void saveToCSV(File aFile) throws IOException {
		double[][][] graphData = null;
		double[][][] graphData2 = null;
		
		graphData = new double[fieldName.length][][];

		for (int j=0; j < fieldName.length; j++) {
			if (payloadType == FoxFramePart.TYPE_REAL_TIME)
				graphData[j] = Config.payloadStore.getRtGraphData(fieldName[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME, false);
			else if (payloadType == FoxFramePart.TYPE_MAX_VALUES)
				graphData[j] = Config.payloadStore.getMaxGraphData(fieldName[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME, false);
			else if (payloadType == FoxFramePart.TYPE_MIN_VALUES)
				graphData[j] = Config.payloadStore.getMinGraphData(fieldName[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME, false);
			else if (payloadType == FoxFramePart.TYPE_RAD_TELEM_DATA)
				graphData[j] = Config.payloadStore.getRadTelemGraphData(fieldName[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME, false);
			else if (payloadType == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER)
				graphData[j] = Config.payloadStore.getHerciScienceHeaderGraphData(fieldName[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME, false);
			else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
				graphData[j] = Config.payloadStore.getMeasurementGraphData(fieldName[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME);
			else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
				graphData[j] = Config.payloadStore.getPassMeasurementGraphData(fieldName[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME);
			else if  (payloadType == FoxFramePart.TYPE_WOD) 
				graphData[j] = Config.payloadStore.getGraphData(fieldName[j], SAMPLES, fox, START_RESET, START_UPTIME, Spacecraft.WOD_LAYOUT, true);
			else if  (payloadType == FoxFramePart.TYPE_WOD_RAD) 
				graphData[j] = Config.payloadStore.getGraphData(fieldName[j], SAMPLES, fox, START_RESET, START_UPTIME, Spacecraft.WOD_RAD_LAYOUT, false);
			
		}
		if (fieldName2 != null) {
			graphData2 = new double[fieldName.length][][];
			for (int j=0; j < fieldName2.length; j++) {
				if (payloadType == FoxFramePart.TYPE_REAL_TIME)
					graphData2[j] = Config.payloadStore.getRtGraphData(fieldName2[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME, false);
				else if (payloadType == FoxFramePart.TYPE_MAX_VALUES)
					graphData2[j] = Config.payloadStore.getMaxGraphData(fieldName2[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME, false);
				else if (payloadType == FoxFramePart.TYPE_MIN_VALUES)
					graphData2[j] = Config.payloadStore.getMinGraphData(fieldName2[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME, false);
				else if (payloadType == FoxFramePart.TYPE_RAD_TELEM_DATA)
					graphData2[j] = Config.payloadStore.getRadTelemGraphData(fieldName2[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME, false);
				else if (payloadType == FoxFramePart.TYPE_HERCI_SCIENCE_HEADER)
					graphData2[j] = Config.payloadStore.getHerciScienceHeaderGraphData(fieldName2[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME, false);
				else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
					graphData2[j] = Config.payloadStore.getMeasurementGraphData(fieldName2[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME);
				else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
					graphData2[j] = Config.payloadStore.getPassMeasurementGraphData(fieldName2[j], this.SAMPLES, (FoxSpacecraft)this.fox, this.START_RESET, this.START_UPTIME);
				else if (payloadType == FoxFramePart.TYPE_WOD)
					graphData2[j] = Config.payloadStore.getGraphData(fieldName2[j], SAMPLES, fox, START_RESET, START_UPTIME, Spacecraft.WOD_LAYOUT, true);
				else if (payloadType == FoxFramePart.TYPE_WOD_RAD)
					graphData2[j] = Config.payloadStore.getGraphData(fieldName2[j], SAMPLES, fox, START_RESET, START_UPTIME, Spacecraft.WOD_RAD_LAYOUT, false);		
	
			}
		}
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
				h= "resets, uptime";
			for (int j=0; j < fieldName.length; j++)				
				h = h + ", " + fieldName[j] ;
			if (fieldName2 != null)
			for (int j=0; j < fieldName2.length; j++)				
				h = h + ", " + fieldName2[j] ;
			output.write(h + "\n");
			
			for (int i=0; i< graphData[0][0].length; i++) {
				String s;
				if (this.showUTCtime && fox.isFox1()) {
					FoxSpacecraft fox2 = (FoxSpacecraft)fox;
					if (fox2.hasTimeZero((int)graphData[0][PayloadStore.RESETS_COL][i]))
						s = fox2.getUtcDateForReset((int)graphData[0][PayloadStore.RESETS_COL][i], (long)graphData[0][PayloadStore.UPTIME_COL][i]) 
						+ " " + fox2.getUtcTimeForReset((int)graphData[0][PayloadStore.RESETS_COL][i], (long)graphData[0][PayloadStore.UPTIME_COL][i]);
					else
						s = "??"; 
				} else
					s = graphData[0][PayloadStore.RESETS_COL][i] + ", " +  // can always read reset and uptime from field 0
						graphData[0][PayloadStore.UPTIME_COL][i] ;
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
		
		BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
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

}
