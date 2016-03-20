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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.PayloadStore;
import common.Config;
import common.Log;
import common.Spacecraft;
import decoder.SinkAudio;
import fcd.FcdProDevice;
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
	private int payloadType;
	private int conversionType;
	private JPanel contentPane;
	private GraphPanel panel;
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
	private JComboBox cbAddVariable;
	private ArrayList<String> variables;
	
	public Spacecraft fox;
	public static int DEFAULT_SAMPLES = 180;
	public int SAMPLES = DEFAULT_SAMPLES;
	public static long DEFAULT_START_UPTIME = 0;
	public static int DEFAULT_START_RESET = 0;
	public long START_UPTIME = DEFAULT_START_UPTIME;
	public int START_RESET = DEFAULT_START_RESET;
	public static final int MAX_SAMPLES = 99999;
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
	
	boolean textDisplay = false;
	
	/**
	 * Create the frame.
	 */
	public GraphFrame(String title, String fieldName, String fieldUnits, int conversionType, int plType, Spacecraft sat) {
		fox = sat;
		this.fieldName = new String[1];
		this.fieldName[0] = fieldName;
		this.fieldUnits = fieldUnits;
		this.title = title;
		this.conversionType = conversionType;
		
		BitArrayLayout layout = getLayout(plType);
		
		payloadType = plType;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(this);
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
		JPanel titlePanelLeft = new JPanel();
		JPanel titlePanelcenter = new JPanel();
		titlePanel.add(titlePanelLeft, BorderLayout.EAST);
		titlePanel.add(titlePanelcenter, BorderLayout.CENTER);

		calcTitle();
		
//		JLabel lblTitle = new JLabel(displayTitle);
//		lblTitle.setFont(new Font("SansSerif", Font.BOLD, Config.graphAxisFontSize + 3));
//		titlePanelcenter.add(lblTitle);

		if (conversionType == BitArrayLayout.CONVERT_IHU_DIAGNOSTIC || conversionType == BitArrayLayout.CONVERT_HARD_ERROR || 
				conversionType == BitArrayLayout.CONVERT_SOFT_ERROR ) {   // Should not hard code this - need to update
			//textArea = new DiagnosticTextArea(title, fieldName, this);
			diagnosticTable = new DiagnosticTable(title, fieldName, conversionType, this, fox);
			//JScrollPane scroll = new JScrollPane (diagnosticTable, 
			//		   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			contentPane.add(diagnosticTable, BorderLayout.CENTER);
			textDisplay = true;
		} else {
			panel = new GraphPanel(title, conversionType, payloadType, this, sat);
			contentPane.add(panel, BorderLayout.CENTER);
		}

		
		// Toolbar buttons
		btnLines = new JButton("Lines");
		btnLines.setMargin(new Insets(0,0,0,0));
		btnLines.setToolTipText("Draw lines between data points");
		btnLines.addActionListener(this);
		titlePanelLeft.add(btnLines);
		if (this.textDisplay) btnLines.setVisible(false);

		btnPoints = new JButton("Points");
		btnPoints.setMargin(new Insets(0,0,0,0));
		btnPoints.setToolTipText("Show data points");
		btnPoints.addActionListener(this);
		titlePanelLeft.add(btnPoints);
		if (this.textDisplay) btnPoints.setVisible(false);

		
		btnHorizontalLines = createIconButton("/images/horizontalLines.png","Horizontal","Show Horizontal Lines");
		titlePanelLeft.add(btnHorizontalLines);
		if (this.textDisplay) btnHorizontalLines.setVisible(false);

		btnVerticalLines = createIconButton("/images/verticalLines.png","Verrtical","Show Vertical Lines");
		titlePanelLeft.add(btnVerticalLines);
		if (this.textDisplay) btnVerticalLines.setVisible(false);

		btnMain = new JButton("Hide");
		btnMain.setMargin(new Insets(0,0,0,0));
		btnMain.setToolTipText("Hide the unprocessed telemetry data");
		btnMain.addActionListener(this);
		titlePanelLeft.add(btnMain);
		if (this.textDisplay) btnMain.setVisible(false);

		btnDerivative = createIconButton("/images/derivSmall.png","Deriv","Plot 1st Derivative (1st difference)");
		titlePanelLeft.add(btnDerivative);
		if (this.textDisplay) btnDerivative.setVisible(false);

		btnAvg = new JButton("AVG");
		btnAvg.setMargin(new Insets(0,0,0,0));
		btnAvg.setToolTipText("Running Average / Low Pass Filter");
		btnAvg.addActionListener(this);
		
		titlePanelLeft.add(btnAvg);
		if (this.textDisplay) btnAvg.setVisible(false);

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
		titlePanelLeft.add(btnLatest);
		//if (this.textDisplay) btnLatest.setEnabled(false);
		
		btnCSV = createIconButton("/images/saveSmall.png","CSV","Save this data to a CSV file");
		titlePanelLeft.add(btnCSV);
		if (this.textDisplay) btnCSV.setEnabled(false);
		
		btnCopy = createIconButton("/images/copySmall.png","Copy","Copy graph to clipboard");
		titlePanelLeft.add(btnCopy);
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

		// make a list of the variables that we could plot
		
		variables = new ArrayList<String>();
		for (int v=0; v<layout.fieldName.length; v++) {
			//if (sat.rtLayout.fieldUnits[v].equalsIgnoreCase(fieldUnits))
				variables.add(layout.fieldName[v]);
		}
		Object[] fields = variables.toArray();
		cbAddVariable = new JComboBox(fields);
		footerPanelFarLeft.add(cbAddVariable);
		cbAddVariable.addActionListener(this);
		
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

		lblPlot = new JLabel("Plot");
		txtSamplePeriod = new JTextField();
//		txtSamplePeriod.setPreferredSize(new Dimension(30,14));
		txtSamplePeriod.addActionListener(this);
		txtSamplePeriod.addFocusListener(this);

		
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
		
		chckbxPlotAllUptime = new JCheckBox("Continuous");
		chckbxPlotAllUptime.setToolTipText("");
		footerPanelLeft.add(chckbxPlotAllUptime);
		chckbxPlotAllUptime.setSelected(showContinuous);
		
		chckbxPlotAllUptime.addItemListener(this);
		if (this.textDisplay) chckbxPlotAllUptime.setVisible(false);
		
	}

	private BitArrayLayout getLayout(int plType) {
		BitArrayLayout layout = null;
		if (plType == FramePart.TYPE_REAL_TIME)
			layout = fox.rtLayout;
		else if (plType == SatMeasurementStore.RT_MEASUREMENT_TYPE)
			layout = fox.measurementLayout;
		else if (plType == SatMeasurementStore.PASS_MEASUREMENT_TYPE)
			layout = fox.passMeasurementLayout;
		return layout;
	}
	
	private void calcTitle() {
		//BitArrayLayout layout = getLayout(payloadType);
		displayTitle = title; // + " - " + layout.getShortNameByName(fieldName[0]) + "(" + layout.getUnitsByName(fieldName[0])+ ")";
		if (payloadType != SatMeasurementStore.RT_MEASUREMENT_TYPE &&
				payloadType !=SatMeasurementStore.PASS_MEASUREMENT_TYPE) // measurement
			displayTitle = fox.name + " " + displayTitle;
		if (conversionType == BitArrayLayout.CONVERT_FREQ) {
			int freqOffset = fox.telemetryDownlinkFreqkHz;
			displayTitle = title + " delta from " + freqOffset + " kHz";
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
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "windowHeight", this.getHeight());
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "windowWidth", this.getWidth());
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "windowX", this.getX());
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "windowY", this.getY());
		
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "numberOfSamples", this.SAMPLES);
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "fromReset", this.START_RESET);
		Config.saveGraphLongParam(fox.getIdString(), fieldName[0], "fromUptime", this.START_UPTIME);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "open", open);
		
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "hideMain", hideMain);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "hideLines", hideLines);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "hidePoints", hidePoints);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "plotDerivative", plotDerivative);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "dspAvg", dspAvg);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "showVerticalLines", showVerticalLines);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "showHorizontalLines", showHorizontalLines);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "showUTCtime", showUTCtime);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "hideUptime", hideUptime);
		
		Config.saveGraphIntParam(fox.getIdString(), fieldName[0], "AVG_PERIOD", AVG_PERIOD);
		Config.saveGraphBooleanParam(fox.getIdString(), fieldName[0], "showContinuous", showContinuous);
	}

	public boolean loadProperties() {
		int windowX = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "windowX");
		int windowY = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "windowY");
		int windowWidth = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "windowWidth");
		int windowHeight = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "windowHeight");
		if (windowX == 0 ||windowY == 0 ||windowWidth == 0 ||windowHeight == 0)
			setBounds(100, 100, 740, 400);
		else
			setBounds(windowX, windowY, windowWidth, windowHeight);

		this.SAMPLES = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "numberOfSamples");
		if (SAMPLES == 0) SAMPLES = DEFAULT_SAMPLES;
		if (SAMPLES > MAX_SAMPLES) {
			SAMPLES = MAX_SAMPLES;
		}
			
		this.START_RESET = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "fromReset");
		this.START_UPTIME = Config.loadGraphLongValue(fox.getIdString(), fieldName[0], "fromUptime");
		boolean open = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "open");
		hideMain = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "hideMain");
		hideLines = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "hideLines");
		hidePoints = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "hidePoints");
		plotDerivative = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "plotDerivative");
		dspAvg = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "dspAvg");
		showVerticalLines = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "showVerticalLines");
		showHorizontalLines = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "showHorizontalLines");
		showUTCtime = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "showUTCtime");
		hideUptime = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "hideUptime");
		
		AVG_PERIOD = Config.loadGraphIntValue(fox.getIdString(), fieldName[0], "AVG_PERIOD");
		if (AVG_PERIOD == 0) AVG_PERIOD = DEFAULT_AVG_PERIOD;
		showContinuous = Config.loadGraphBooleanValue(fox.getIdString(), fieldName[0], "showContinuous");
		if (showContinuous) UPTIME_THRESHOLD = CONTINUOUS_UPTIME_THRESHOLD; else UPTIME_THRESHOLD = DEFAULT_UPTIME_THRESHOLD;
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cbAddVariable) {
			// Add or remove a variable to be plotted on the graph
			int position = cbAddVariable.getSelectedIndex();
			if (fieldName[0].equalsIgnoreCase(variables.get(position))) return; // can't toggle the main value
			
			int fields = fieldName.length;
			int fields2 = 0;;
			String[] temp = new String[fields];
			String[] temp2 = null;
			int i = 0;
			boolean toggle = false;
			boolean toggle2 = false;
			BitArrayLayout layout = null;
			if (payloadType == FramePart.TYPE_REAL_TIME)
				layout = fox.rtLayout;
			else if (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE)
				layout = fox.measurementLayout;
			else if (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE)
				layout = fox.passMeasurementLayout;
			
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
			}
			if (toggle && fieldName.length > 1) {
				// we remove this entry
				fieldName = new String[fields-1];
				i=0;
				for (String s: temp)
					if (!s.equalsIgnoreCase(variables.get(position)))
						fieldName[i++] = s;
				
			} else if (toggle2 && fieldName2.length > 1) {
				// we remove this entry
				fieldName2 = new String[fields2-1];
				i=0;
				for (String s: temp2)
					if (!s.equalsIgnoreCase(variables.get(position)))
						fieldName2[i++] = s;
			} else {
				if (fieldName.length < (GraphPanel.MAX_VARIABLES-1)) {
					// Check if this is the same units, otherwise set this as unit2
					String unit = layout.getUnitsByName(variables.get(position));
					if (!unit.equalsIgnoreCase(fieldUnits)) {
						fieldUnits2 = unit;
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
			}
			
			// now rebuild the pick list
			variables = new ArrayList<String>();
			
			for (int v=0; v<layout.fieldName.length; v++) {
				if (layout.fieldUnits[v].equalsIgnoreCase(fieldUnits) || layout.fieldUnits[v].equalsIgnoreCase(fieldUnits2)
						|| fieldUnits2.equalsIgnoreCase(""))
					variables.add(layout.fieldName[v]);
			}
		
			cbAddVariable.removeAllItems();
			for (String s:variables) {
				cbAddVariable.addItem(s);
			}
			
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
			textFromReset.setText(Long.toString(DEFAULT_START_UPTIME));
			textFromUptime.setText(Integer.toString(DEFAULT_START_RESET));
			txtSamplePeriod.setText(Integer.toString(DEFAULT_SAMPLES));
			txtAvgPeriod.setText(Integer.toString(DEFAULT_AVG_PERIOD));
			parseTextFields();
			parseAvgPeriod();
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
		
		graphData = new double[fieldName.length][][];
		
		for (int j=0; j < fieldName.length; j++) {
			if (payloadType == FramePart.TYPE_REAL_TIME)
				graphData[j] = Config.payloadStore.getRtGraphData(fieldName[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME);
			else if (payloadType == FramePart.TYPE_MAX_VALUES)
				graphData[j] = Config.payloadStore.getMaxGraphData(fieldName[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME);
			else if (payloadType == FramePart.TYPE_MIN_VALUES)
				graphData[j] = Config.payloadStore.getMinGraphData(fieldName[j], this.SAMPLES, fox, this.START_RESET, this.START_UPTIME);
			else if (payloadType == FramePart.TYPE_RAD_TELEM_DATA)
				graphData[j] = Config.payloadStore.getRadTelemGraphData(fieldName[j], this.SAMPLES, this.fox, this.START_RESET, this.START_UPTIME);
			else if (payloadType == FramePart.TYPE_HERCI_SCIENCE_HEADER)
				graphData[j] = Config.payloadStore.getHerciScienceHeaderGraphData(fieldName[j], this.SAMPLES, this.fox, this.START_RESET, this.START_UPTIME);
			else if  (payloadType == SatMeasurementStore.RT_MEASUREMENT_TYPE) 
				graphData[j] = Config.payloadStore.getMeasurementGraphData(fieldName[j], this.SAMPLES, this.fox, this.START_RESET, this.START_UPTIME);
			else if  (payloadType == SatMeasurementStore.PASS_MEASUREMENT_TYPE) 
				graphData[j] = Config.payloadStore.getPassMeasurementGraphData(fieldName[j], this.SAMPLES, this.fox, this.START_RESET, this.START_UPTIME);

		}
		if (graphData != null) {
			if(!aFile.exists()){
				aFile.createNewFile();
			} else {

			}
			Writer output = new BufferedWriter(new FileWriter(aFile, false));

			for (int i=0; i< graphData[0].length; i++) {
				String s = graphData[0][PayloadStore.RESETS_COL][i] + ", " +
						graphData[0][PayloadStore.UPTIME_COL][i] ;
				for (int j=0; j < fieldName.length; j++)				
						s = s + ", " + graphData[j][PayloadStore.DATA_COL][i] + "\n";
				
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
