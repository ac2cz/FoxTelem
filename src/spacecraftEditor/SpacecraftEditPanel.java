package spacecraftEditor;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import common.Log;
import common.Spacecraft;
import gui.SourceTab;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;
import telemetry.TelemFormat;
import telemetry.frames.FrameLayout;
import common.Config;
import common.Spacecraft;

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
* The SpacecraftFrame is a seperate window that opens and allows the spacecraft paramaters to
* be viewed and edited.
*
*/
@SuppressWarnings("serial")
public class SpacecraftEditPanel extends JPanel implements ActionListener, ItemListener, FocusListener, MouseListener {

	public static final String PAYLOAD_TEMPLATE_FILENAME = "PAYLOAD_template.csv";
	
	JTextField id, displayName;
	JTextField name;
	JComboBox<String> priority, layoutType;
	JTextField telemetryDownlinkFreqkHz;
	JTextField minFreqBoundkHz;
	JTextField maxFreqBoundkHz;
	JTextField rssiLookUpTableFileName;
	JTextField ihuTempLookUpTableFileName;
	JTextField ihuVBattLookUpTableFileName;
	JTextField BATTERY_CURRENT_ZERO;
	JTextField mpptResistanceError;
	JTextField mpptSensorOffThreshold;
	JTextField memsRestValueX, memsRestValueY, memsRestValueZ;
	JTextField[] T0;
	JTextField localServer;
	JTextField localServerPort;
	JTextField payloadFilename,payloadName;
	
	JCheckBox[] sendLayoutToServer;
	JTextArea taDesc;
	
	JCheckBox useIHUVBatt;
	JCheckBox track,hasFOXDB_V3;
	JComboBox<String> cbMode;
	JComboBox<String> payloadType;
	JButton btnCancel;
	JButton btnSave, btnFrameAdd, btnFrameRemove, btnAddPayload, btnRemovePayload,btnBrowsePayload,btnUpdatePayload, btnGeneratePayload;
	
	PayloadsTableModel layoutsTableModel;
	
	Spacecraft sat;
	
	JTable sourcesTable,framesTable,payloadsTable;
	JPanel leftSourcesPanel, sourceStats, leftPanel, rightPanel, rightPanel1;
	
	int sourceFormatSelected = 0; // the source format that the user has clicked
	
	int headerSize = 12;
	
	/**
	 * Create the dialog.
	 */
	public SpacecraftEditPanel(Spacecraft sat) {
		
		this.sat = sat;
		
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setLayout(new BorderLayout(0, 0));
		JPanel titlePanel = addTitlePanel();
		add(titlePanel, BorderLayout.NORTH);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(0,3));
		add(mainPanel, BorderLayout.CENTER);
		
		JPanel leftPanel = addLeftPanel();
		mainPanel.add(leftPanel);
		JPanel centerPanel = addCenterPanel();
		mainPanel.add(centerPanel);
		JPanel rightPanel = addRightPanel();
		mainPanel.add(rightPanel);
		
		JPanel footer = bottomPanel();
		add(footer, BorderLayout.SOUTH);
		
	}
	
	private JPanel addTitlePanel() {
		
		// TITLE PANEL
		JPanel mainTitlePanel = new JPanel();
		mainTitlePanel.setLayout(new BorderLayout());
		
		JPanel titlePanel = new JPanel();
		mainTitlePanel.add(titlePanel, BorderLayout.WEST);
		
		titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
		
		JPanel titlePanel0 = new JPanel();
		titlePanel.add(titlePanel0, BorderLayout.NORTH);
		titlePanel0.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JPanel titlePanel1 = new JPanel();
		titlePanel.add(titlePanel1, BorderLayout.NORTH);
		titlePanel1.setLayout(new BoxLayout(titlePanel1, BoxLayout.Y_AXIS));

		JPanel titlePanel2 = new JPanel();
		titlePanel.add(titlePanel2, BorderLayout.NORTH);
		titlePanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		TitledBorder heading0 = title("Identification");
		titlePanel.setBorder(heading0);

		id = addSettingsRow(titlePanel0, 6, "Sat ID", 
				"This number is sent in the header of all telemetry frames", ""+sat.foxId);
		titlePanel0.add(id);
		JLabel lblPriority = new JLabel("    Priority");
		titlePanel0.add(lblPriority);
		String tip = "The highest priority spacecraft is tracked if more than one is above the horizon";
		lblPriority.setToolTipText(tip);
		String[] nums = new String[20];
		for (int i=0; i < nums.length; i++)
			nums[i] = ""+i;
		priority = new JComboBox<String>(nums); 
		priority.setSelectedIndex(sat.user_priority);
		titlePanel0.add(priority);
		
		name = addSettingsRow(titlePanel1, 8, "Name (for Keps)", 
				"The name must be the same as the name in your TLE/Keps file if you want to calculate positions or sync with SatPC32", ""+sat.user_keps_name);
		displayName = addSettingsRow(titlePanel1, 15, "Display Name", 
				"This name is use used as a label on Graphs and Tabs", ""+sat.user_display_name);
		
		hasFOXDB_V3 = addCheckBoxRow("Use V3 Telem Database (recommended)", "This is true for all new spacecraft", sat.hasFOXDB_V3, titlePanel1 );
		
		JPanel descPanel = new JPanel();
		descPanel.setLayout(new BorderLayout());
		TitledBorder heading9 = title("Description");
		descPanel.setBorder(heading9);

		taDesc = new JTextArea(2, 45);
		taDesc.setText(sat.description);
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);
		taDesc.setEditable(true);
		taDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
		descPanel.add(taDesc, BorderLayout.CENTER);
		mainTitlePanel.add(descPanel, BorderLayout.CENTER);
		
		return mainTitlePanel;
	}
	
	private JPanel addLeftPanel() {

		// LEFT Column - Fixed Params that can not be changed
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		
		leftSourcesPanel = new JPanel();
		leftPanel.add(leftSourcesPanel);
		TitledBorder headingSources = title("Sources");
		leftSourcesPanel.setBorder(headingSources);
		leftSourcesPanel.setLayout(new BorderLayout());
		
		sourceStats = new JPanel();
		leftSourcesPanel.add(sourceStats, BorderLayout.SOUTH);
		
		SourcesTableModel sourcesTableModel = new SourcesTableModel();
		
		sourcesTable = new JTable(sourcesTableModel);
		sourcesTable.setAutoCreateRowSorter(true);
		JScrollPane sourcesScrollPane = new JScrollPane (sourcesTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sourcesScrollPane.setPreferredSize(new Dimension(100,400));
		sourcesTable.setFillsViewportHeight(true);
	//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		leftSourcesPanel.add(sourcesScrollPane, BorderLayout.CENTER);
		TableColumn column = null;
		column = sourcesTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(100);
		column = sourcesTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(100);
		sourcesTable.addMouseListener(this);
		
		String[][] data = new String[sat.numberOfSources][2];
		for (int i=0; i< sat.numberOfSources; i++) {
			data[i][0] =sat.sourceName[i];
			if (sat.sourceFormatName != null) 
				data[i][1] = sat.sourceFormatName[i];
			else
				data[i][1] ="NONE";
		}
		sourcesTableModel.setData(data);
	
		//leftSourcesPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));
		
		
		
		JPanel leftFixedPanel = new JPanel();
		JPanel leftFixedPanelf = new JPanel();
		leftFixedPanelf.setLayout(new BorderLayout());
		leftFixedPanel.setLayout(new BoxLayout(leftFixedPanel, BoxLayout.Y_AXIS));
		leftPanel.add(leftFixedPanelf);
		leftFixedPanelf.add(leftFixedPanel, BorderLayout.NORTH);
		
		TitledBorder heading = title("Fixed Paramaters");
		leftFixedPanel.setBorder(heading);
		
		JLabel lModel = new JLabel("Model: " + Spacecraft.modelNames[sat.model]);
		leftFixedPanel.add(lModel);
		JLabel lIhusn = new JLabel("IHU S/N: " + sat.IHU_SN);
		leftFixedPanel.add(lIhusn);
		JLabel icr = new JLabel("ICR: " + sat.hasImprovedCommandReceiver);
		leftFixedPanel.add(icr);
		
		JLabel lExp[] = new JLabel[4];
		for (int i=0; i<4; i++) {
			lExp[i] = new JLabel("Experiment "+(i+1)+": " + Spacecraft.expNames[sat.experiments[i]]);
			leftFixedPanel.add(lExp[i]);
		}

//		leftPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		JPanel leftPane1 = new JPanel();
		leftPanel.add(leftPane1);
		leftPane1.setLayout(new BoxLayout(leftPane1, BoxLayout.Y_AXIS));
		
		TitledBorder heading4 = title("Frequency and Tracking");
		leftPane1.setBorder(heading4);

		cbMode = this.addComboBoxRow(leftPane1, "Mode", "", SourceTab.formats);
		setSelection(cbMode, SourceTab.formats, SourceTab.formats[sat.user_format]);
				
		telemetryDownlinkFreqkHz = addSettingsRow(leftPane1, 15, "Downlink Freq (kHz)", 
				"The nominal downlink frequency of the spacecraft", ""+sat.user_telemetryDownlinkFreqkHz);
		minFreqBoundkHz = addSettingsRow(leftPane1, 15, "Lower Freq Bound (kHz)", 
				"The lower frequency boundry when we are searching for the spacecraft signal", ""+sat.user_minFreqBoundkHz);
		maxFreqBoundkHz = addSettingsRow(leftPane1, 15, "Upper Freq Bound (kHz)", 
				"The upper frequency boundry when we are searching for the spacecraft signal", ""+sat.user_maxFreqBoundkHz);
		track = addCheckBoxRow("Track this spacecraft", "When Doppler tracking or Find Signal is enabled include this satellite", sat.user_track, leftPane1 );
//		leftPane1.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		JPanel leftPane2 = new JPanel();
		leftPanel.add(leftPane2);
		leftPane2.setLayout(new BoxLayout(leftPane2, BoxLayout.Y_AXIS));
		
		if (!sat.hasFOXDB_V3) {
			TitledBorder heading5 = title("Calibration");
			leftPane2.setBorder(heading5);

			BATTERY_CURRENT_ZERO = addSettingsRow(leftPane2, 25, "Battery Current Zero", 
					"The calibration paramater for zero battery current", ""+sat.user_BATTERY_CURRENT_ZERO);

			rssiLookUpTableFileName = addSettingsRow(leftPane2, 25, "RSSI Lookup Table", 
					"The file containing the lookup table for Received Signal Strength", ""+sat.getLookupTableFileNameByName(Spacecraft.RSSI_LOOKUP));
			ihuTempLookUpTableFileName = addSettingsRow(leftPane2, 25, "IHU Temp Lookup Table", 
					"The file containing the lookup table for the IHU Temperature", ""+sat.getLookupTableFileNameByName(Spacecraft.IHU_TEMP_LOOKUP));
			ihuVBattLookUpTableFileName = addSettingsRow(leftPane2, 25, "VBatt Lookup Table", 
					"The file containing the lookup table for the Battery Voltage", ""+sat.getLookupTableFileNameByName(Spacecraft.IHU_VBATT_LOOKUP));

			rssiLookUpTableFileName.setEnabled(false);
			ihuTempLookUpTableFileName.setEnabled(false);
			ihuVBattLookUpTableFileName .setEnabled(false);

			useIHUVBatt = addCheckBoxRow("Use Bus Voltage as VBatt", "Read the Bus Voltage from the IHU rather than the Battery "
					+ "Voltage from the battery card using I2C", sat.useIHUVBatt, leftPane2 );
			if (sat.hasMpptSettings) {
				mpptResistanceError = addSettingsRow(leftPane2, 25, "MPPT Resistance Error", 
						"The extra resistance in the RTD temperature measurement circuit", ""+sat.user_mpptResistanceError);
				mpptSensorOffThreshold = addSettingsRow(leftPane2, 25, "MPPT Sensor Off Threshold", 
						"The ADC value when the temperature sensor is considered off", ""+sat.user_mpptSensorOffThreshold);
			}
			if (sat.hasMemsRestValues) {
				memsRestValueX = addSettingsRow(leftPane2, 25, "MEMS Rest Value X", 
						"The rest value for the MEMS X rotation sensor", ""+sat.user_memsRestValueX);
				memsRestValueY = addSettingsRow(leftPane2, 25, "MEMS Rest Value Y", 
						"The rest value for the MEMS Y rotation sensor", ""+sat.user_memsRestValueY);
				memsRestValueZ = addSettingsRow(leftPane2, 25, "MEMS Rest Value Z", 
						"The rest value for the MEMS Z rotation sensor", ""+sat.user_memsRestValueZ);
			}
		}
//	leftPane2.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		
		return leftPanel;
		
	}
	
	private void loadPayloadTable() {
		String[][] data = new String[sat.numberOfLayouts][4];
		for (int i=0; i< sat.numberOfLayouts; i++) {
			data[i][0] =""+i;
			if (sat.layout[i].name != null) 
				data[i][1] = sat.layout[i].name;
			else
				data[i][1] ="NONE";

			if (i < sat.layoutFilename.length && sat.layoutFilename[i] != null) // we don't store filenames for can layouts, so skip those
				data[i][2] = sat.layoutFilename[i];
			else
				data[i][2] ="-";
			data[i][3] = ""+sat.layout[i].getMaxNumberOfBytes();

		}
		if (sat.numberOfLayouts > 0) 
			layoutsTableModel.setData(data);
		else {
			String[][] fakeRow = {{"","","",""}};
			layoutsTableModel.setData(fakeRow);
		}
	}
	
	private JPanel addCenterPanel() {
		// CENTER Column - Things the user can change - e.g. Layout Files, Freq, Tracking etc
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

		JPanel centerPanel1 = new JPanel();
		centerPanel.add(centerPanel1);
		centerPanel1.setLayout(new BorderLayout());
		
		TitledBorder headingFrames = title("Frames");
		centerPanel1.setBorder(headingFrames);

		if (sat.numberOfFrameLayouts > 0) {
			FramesTableModel frameTableModel = new FramesTableModel();

			framesTable = new JTable(frameTableModel);
			framesTable.setAutoCreateRowSorter(true);
			JScrollPane scrollPane = new JScrollPane (framesTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setPreferredSize(new Dimension(100,400));
			framesTable.setFillsViewportHeight(true);
			//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

			centerPanel1.add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
			TableColumn column = framesTable.getColumnModel().getColumn(0);
			column.setPreferredWidth(20);
			column = framesTable.getColumnModel().getColumn(1);
			column.setPreferredWidth(200);

			framesTable.addMouseListener(this);

			String[][] data = new String[sat.numberOfFrameLayouts][2];
			for (int i=0; i< sat.numberOfFrameLayouts; i++) {
				data[i][0] =""+i;
				if (sat.frameLayoutFilename[i] != null) 
					data[i][1] = sat.frameLayoutFilename[i];
				else
					data[i][1] ="NONE";
			}
			frameTableModel.setData(data);
			
			
			
		}
		JPanel frameButtons = new JPanel();
		centerPanel1.add(frameButtons, BorderLayout.SOUTH);
		btnFrameAdd = new JButton("Add");
		frameButtons.add(btnFrameAdd);
		btnFrameRemove = new JButton("Remove");
		frameButtons.add(btnFrameRemove);

		//centerPanel1.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		
		JPanel centerPanel2 = new JPanel();
		centerPanel.add(centerPanel2);
		centerPanel2.setLayout(new BorderLayout());

		TitledBorder headingLayout = title("Payloads");
		centerPanel2.setBorder(headingLayout);

		
		layoutsTableModel = new PayloadsTableModel();

		payloadsTable = new JTable(layoutsTableModel);
		payloadsTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (payloadsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		payloadsTable.setFillsViewportHeight(true);
		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		centerPanel2.add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
		TableColumn column = payloadsTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(20);
		column = payloadsTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(100);
		column = payloadsTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(200);
		column = payloadsTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(40);

		payloadsTable.addMouseListener(this);

		loadPayloadTable();

		JPanel footerPanel = new JPanel();
		centerPanel2.add(footerPanel, BorderLayout.SOUTH);
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS) );
		
		// Row 1
		JPanel footerPanelRow1 = new JPanel();
		footerPanelRow1.setLayout(new BoxLayout(footerPanelRow1, BoxLayout.X_AXIS) );
		footerPanel.add(footerPanelRow1);
		
		JPanel f1 = new JPanel();
		f1.setLayout(new BoxLayout(f1, BoxLayout.Y_AXIS) );
		JLabel lf1 = new JLabel("Name");
		payloadName = new JTextField();
		f1.add(lf1);
		f1.add(payloadName);
		footerPanelRow1.add(f1);
		
		JPanel f2 = new JPanel();
		f2.setLayout(new BoxLayout(f2, BoxLayout.Y_AXIS) );
		JLabel lf2 = new JLabel("Type");
		payloadType = new JComboBox<String>(BitArrayLayout.types); 
		f2.add(lf2);
		f2.add(payloadType);
		footerPanelRow1.add(f2);
		
		JPanel f3 = new JPanel();
		f3.setLayout(new BoxLayout(f3, BoxLayout.Y_AXIS) );
		JLabel lf3 = new JLabel("Filename");
		payloadFilename = new JTextField();
		f3.add(lf3);
		f3.add(payloadFilename);
		payloadFilename.setEditable(false);
		payloadFilename.addMouseListener(this);
		footerPanelRow1.add(f3);
		
		// Row 2
		JPanel footerPanelRow2 = new JPanel();
		footerPanel.add(footerPanelRow2);
		
		btnAddPayload = new JButton("Add");
		btnAddPayload.addActionListener(this);
		btnBrowsePayload = new JButton("Browse");
		btnBrowsePayload.addActionListener(this);
		btnRemovePayload = new JButton("Remove");
		btnRemovePayload.addActionListener(this);
		btnUpdatePayload = new JButton("Update");
		btnUpdatePayload.addActionListener(this);
		
		btnGeneratePayload = new JButton("Generate C");
		btnGeneratePayload.addActionListener(this);
		
		footerPanelRow2.add(btnAddPayload);
		footerPanelRow2.add(btnUpdatePayload);
		footerPanelRow2.add(btnRemovePayload);
		//footerPanelRow2.add(btnBrowsePayload);
		footerPanelRow2.add(btnGeneratePayload);
		btnAddPayload.setEnabled(true);
		if (sat.numberOfLayouts == 0)
			btnRemovePayload.setEnabled(false);
		//centerPanel2.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		return centerPanel;
	}
	
	
	private JPanel addRightPanel() {
		// RIGHT Column
		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

//		JPanel rightPanel1 = new JPanel();
//		rightPanel.add(rightPanel1);
//		rightPanel1.setLayout(new BoxLayout(rightPanel1, BoxLayout.Y_AXIS));
//
//		TitledBorder heading2 = title("Frame");
//		rightPanel1.setBorder(heading2);
//
//
//
//
//
//		
//
//		JPanel rightPanel2 = new JPanel();
//		rightPanel.add(rightPanel2);
//		rightPanel2.setLayout(new BoxLayout(rightPanel2, BoxLayout.Y_AXIS));
//
//		TitledBorder heading3 = title("Layout");
//		rightPanel2.setBorder(heading3);
//
//
//
//		rightPanel2.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		return rightPanel;
	}
	
	private JPanel bottomPanel() {

		// Bottom panel for description
		JPanel footerPanel = new JPanel();
		
		btnSave = new JButton("Save");
		btnSave.addActionListener(this);
		//btnCancel = new JButton("Reload");
		//btnCancel.addActionListener(this);
		
		footerPanel.add(btnSave);
		//footerPanel.add(btnCancel);
		btnSave.setEnabled(true);
		//btnCancel.setEnabled(true);
		
		return footerPanel;
	}
	
	private void setSelection(JComboBox<String> comboBox, String[] values, String value ) {
		int i=0;
		for (String rate : values) {
			if (rate.equalsIgnoreCase(value))
					break;
			i++;
		}
		if (i >= values.length)
			i = 0;
		comboBox.setSelectedIndex(i);
		
	}
	
	private JComboBox<String> addComboBoxRow(JPanel parent, String name, String tip, String[] values) {
		JPanel row = new JPanel();
		row.setLayout(new GridLayout(1,2,5,5));

		//row.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel lbl = new JLabel(name);
		JComboBox<String> checkBox = new JComboBox<String>(values);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		lbl.setToolTipText(tip);
		row.add(lbl);
		row.add(checkBox);
		parent.add(row);
		parent.add(new Box.Filler(new Dimension(10,5), new Dimension(10,5), new Dimension(10,5)));
		return checkBox;
	}
	
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	private JCheckBox addCheckBoxRow(String name, String tip, boolean value, JPanel parent) {
		JPanel box = new JPanel();
		box.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JCheckBox checkBox = new JCheckBox(name);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		box.add(checkBox);
		parent.add(box);
		if (value) checkBox.setSelected(true); else checkBox.setSelected(false);
		return checkBox;
	}


	private JTextField addSettingsRow(JPanel column, int length, String name, String tip, String value) {
		JPanel panel = new JPanel();
		column.add(panel);
		panel.setLayout(new GridLayout(1,2,5,5));
		JLabel lblDisplayModuleFont = new JLabel(name);
		lblDisplayModuleFont.setToolTipText(tip);
		panel.add(lblDisplayModuleFont);
		JTextField textField = new JTextField(value);
		textField.setToolTipText(tip);
		panel.add(textField);
		textField.setColumns(length);
		textField.addActionListener(this);
		textField.addFocusListener(this);

		column.add(new Box.Filler(new Dimension(10,5), new Dimension(10,5), new Dimension(10,5)));

		return textField;
	
	}
	
	/**
	 * Returns true if successful and can be disposed
	 * @return
	 */
	public boolean save() {
		boolean refreshTabs = false;
		boolean rebuildMenu = false;
		boolean dispose = true;
		double downlinkFreq = 0;
		double minFreq = 0;
		double maxFreq = 0;
		try {
			// MASTER File params
			sat.foxId = Integer.parseInt(id.getText());
			sat.hasFOXDB_V3 = hasFOXDB_V3.isSelected();
			sat.description = taDesc.getText();
			
			
			
			
			
			// USER File params
			try {
				downlinkFreq = (double)(Math.round(Double.parseDouble(telemetryDownlinkFreqkHz.getText())*1000)/1000.0);
				minFreq = (double)(Math.round(Double.parseDouble(minFreqBoundkHz.getText())*1000)/1000.0);
				maxFreq = (double)(Math.round(Double.parseDouble(maxFreqBoundkHz.getText())*1000)/1000.0);
			} catch (NumberFormatException ex) {
				throw new NumberFormatException("The Frequency fields must contain a valid frequency in kHz");
			}
			if (minFreq < maxFreq) {
				sat.user_telemetryDownlinkFreqkHz = downlinkFreq;
				sat.user_minFreqBoundkHz = minFreq;
				sat.user_maxFreqBoundkHz = maxFreq;
			} else {
				Log.errorDialog("ERROR", "Lower Frequency Bound must be less than Upper Frequency Bound");
				dispose = false;
			}
			int m = cbMode.getSelectedIndex();
			sat.user_format = m;
			//String md = (String) cbMode.getSelectedItem();

			//			if (!sat.getLookupTableFileNameByName(Spacecraft.RSSI_LOOKUP).equalsIgnoreCase(rssiLookUpTableFileName.getText())) {
			//				sat.rssiLookUpTableFileName = rssiLookUpTableFileName.getText();
			//				refreshTabs = true;
			//			}
			//			if (!sat.ihuTempLookUpTableFileName.equalsIgnoreCase(ihuTempLookUpTableFileName.getText())) {
			//				sat.ihuTempLookUpTableFileName = ihuTempLookUpTableFileName.getText();
			//				refreshTabs = true;
			//			}
			//			if (!sat.ihuVBattLookUpTableFileName.equalsIgnoreCase(ihuVBattLookUpTableFileName.getText())) {
			//				sat.ihuVBattLookUpTableFileName = ihuVBattLookUpTableFileName.getText();
			//				refreshTabs = true;
			//			}

			if (!sat.hasFOXDB_V3) {
				if (sat.user_BATTERY_CURRENT_ZERO != Double.parseDouble(BATTERY_CURRENT_ZERO.getText())) {
					sat.user_BATTERY_CURRENT_ZERO = Double.parseDouble(BATTERY_CURRENT_ZERO.getText());
					refreshTabs=true;
				}

				if (sat.hasMpptSettings) {
					if (sat.user_mpptResistanceError != Double.parseDouble(mpptResistanceError.getText())) {
						sat.user_mpptResistanceError = Double.parseDouble(mpptResistanceError.getText());
						refreshTabs=true;
					}

					if (sat.user_mpptSensorOffThreshold != Integer.parseInt(mpptSensorOffThreshold.getText())) {
						sat.user_mpptSensorOffThreshold = Integer.parseInt(mpptSensorOffThreshold.getText());
						refreshTabs=true;
					}
				}
				if (sat.hasMemsRestValues) {
					if (sat.user_memsRestValueX != Integer.parseInt(memsRestValueX.getText())) {
						sat.user_memsRestValueX = Integer.parseInt(memsRestValueX.getText());
						refreshTabs=true;
					}
					if (sat.user_memsRestValueY != Integer.parseInt(memsRestValueY.getText())) {
						sat.user_memsRestValueY = Integer.parseInt(memsRestValueY.getText());
						refreshTabs=true;
					}
					if (sat.user_memsRestValueZ != Integer.parseInt(memsRestValueZ.getText())) {
						sat.user_memsRestValueZ = Integer.parseInt(memsRestValueZ.getText());
						refreshTabs=true;
					}
				}
				if (sat.useIHUVBatt != useIHUVBatt.isSelected()) {
					sat.useIHUVBatt = useIHUVBatt.isSelected();
					refreshTabs = true;
				}
			}
			if (!sat.user_keps_name.equalsIgnoreCase(name.getText())) {
				sat.user_keps_name = name.getText();
			}
			if (!sat.user_display_name.equalsIgnoreCase(displayName.getText())) {
				sat.user_display_name = displayName.getText();
				rebuildMenu = true;
				refreshTabs = true;
			}
			int pri = 99;
			try {
				pri = priority.getSelectedIndex();
			} catch (NumberFormatException e2) {
				
			}
			if (sat.user_priority != pri) {
				rebuildMenu = true;
				sat.user_priority = pri;
				//refreshTabs = true; // refresh the menu list and sat list but not the tabs
			}
			if (localServer != null) {
				sat.user_localServer = localServer.getText();
				if (localServerPort.getText().equalsIgnoreCase(""))
					sat.user_localServerPort = 0;
				else
					sat.user_localServerPort = Integer.parseInt(localServerPort.getText());
			}
			sat.user_track = track.isSelected();
			
			
			sat.save_master_params();
			sat.save(); // store the user params with these default values
		} catch (NumberFormatException Ex) {
			Log.errorDialog("Invalid Paramaters", Ex.getMessage());
		}
		return dispose;
	}
	
	private void addPayload() {
		String[] newLayoutFilenames = new String[sat.numberOfLayouts+1];
		newLayoutFilenames[sat.numberOfLayouts] = payloadFilename.getText();
		
		for (int i=0; i < sat.numberOfLayouts; i++) {
			newLayoutFilenames[i] = sat.layoutFilename[i];
		}
		sat.layoutFilename = newLayoutFilenames;
		
		File source = new File(Config.currentDir+"/spacecraft"+ File.separator + PAYLOAD_TEMPLATE_FILENAME);
		File dest = new File(Config.currentDir+"/spacecraft"+ File.separator + payloadFilename.getText());
		try {
			SatPayloadStore.copyFile(source, dest);
			
			BitArrayLayout[] newLayouts = new BitArrayLayout[sat.numberOfLayouts+1];
			newLayouts[sat.numberOfLayouts] = new BitArrayLayout(payloadFilename.getText());
			newLayouts[sat.numberOfLayouts].name = payloadName.getText();
			newLayouts[sat.numberOfLayouts].typeStr = (String)payloadType.getSelectedItem();
			for (int i=0; i < sat.numberOfLayouts; i++) {
				newLayouts[i] = sat.layout[i];
			}
			sat.layout = newLayouts;
			
			sat.numberOfLayouts++;
			save();
			loadPayloadTable();
			payloadName.setText("");
			payloadFilename.setText("");
			payloadType.setSelectedIndex(0);
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "You need to specify a valid payload file\n"+e1);
		} catch (LayoutLoadException e1) {
			Log.errorDialog("ERROR", "Could not parse the payload template\n"+e1);
		}
	}
	
	private void browsePayload() {
		System.out.println("Browse for Payload ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify payload file", "Select", "csv");
		if (file == null) return;
		payloadFilename.setText(file.getName());
	}
	
	private void generatePayload() {
		System.out.println("Generate Payload ...");
		File layout = new File(Config.currentDir+"/spacecraft"+ File.separator + payloadFilename.getText());
		
		if (!layout.isFile()) {
			Log.errorDialog("ERROR", "Select a row with a valid payload file\n");
			return;
		}
		
		String PYTHON = "C:\\bin\\Python\\Python36-32\\python.exe";
		String SCRIPT = Config.currentDir + File.separator + "gen_header.py";
		String COMMAND = PYTHON + " " + SCRIPT + " " + layout;
		System.out.println(" running: " + COMMAND);
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(COMMAND);
			BufferedReader stdInput = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new 
					InputStreamReader(p.getErrorStream()));

			// read the output from the command
			System.out.println("Here is the standard output of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}

			// read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error running python generate script\n");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == btnSave) {
			System.out.println("Saving ...");
			save();
			
		}
		if (e.getSource() == btnAddPayload) {
			System.out.println("Adding Payload ...");
			addPayload();
			
		}
		if (e.getSource() == btnBrowsePayload) {
			browsePayload();
		}
		
		if (e.getSource() == btnUpdatePayload) {
			int row = payloadsTable.getSelectedRow();
			System.out.println("Updating row " + row);
			if (sat.numberOfLayouts == 0) return;
			
			try {
				sat.layout[row] = new BitArrayLayout(payloadFilename.getText());
				sat.layoutFilename[row] = payloadFilename.getText();
				sat.layout[row].name = payloadName.getText();
				sat.layout[row].typeStr = (String)payloadType.getSelectedItem();
				save();
				loadPayloadTable();
				payloadName.setText("");
				payloadFilename.setText("");
				payloadType.setSelectedIndex(0);
			} catch (FileNotFoundException e1) {
				Log.errorDialog("ERROR", "Could not initilize the payload file\n"+e1);
			} catch (LayoutLoadException e1) {
				Log.errorDialog("ERROR", "Could not load the payload file\n"+e1);
			}

		}

		if (e.getSource() == btnRemovePayload) {
			int row = payloadsTable.getSelectedRow();
			System.out.println("Removing row " + row);
			if (sat.numberOfLayouts == 0) return;
			
			int n = Log.optionYNdialog("Remove payload file too?",
					"Remove this payload file as well as the payload table row?\n"+payloadFilename.getText() + "\n\nThis will be gone forever\n");
			if (n == JOptionPane.NO_OPTION) {
				
			} else {
				File file = new File(Config.currentDir+"/spacecraft" +File.separator + payloadFilename.getText());
				try {
					SatPayloadStore.remove(file.getAbsolutePath());
				} catch (IOException ef) {
					Log.errorDialog("ERROR removing File", "\nCould not remove the payload file\n"+ef.getMessage());
				}
			}
			
			if (sat.numberOfLayouts == 1) {
				sat.numberOfLayouts = 0;
				sat.layout = null;
				sat.layoutFilename = null;

			} else {
				int j = 0;
				BitArrayLayout[] newLayouts = new BitArrayLayout[sat.numberOfLayouts-1];
				for (int i=0; i < sat.numberOfLayouts; i++) {
					if (i != row)
						newLayouts[j++] = sat.layout[i];
				}
				sat.layout = newLayouts;

				j = 0;
				String[] newLayoutFilenames = new String[sat.numberOfLayouts-1];
				for (int i=0; i < sat.numberOfLayouts; i++) {
					if (i != row)
						newLayoutFilenames[j++] = sat.layoutFilename[i];
				}
				sat.layoutFilename = newLayoutFilenames;
				sat.numberOfLayouts--;
			}
			save();
			loadPayloadTable();
			payloadName.setText("");
			payloadFilename.setText("");
			payloadType.setSelectedIndex(0);

			
		}
		if (e.getSource() == btnGeneratePayload) {
			generatePayload();
		}

	}
	
	


	@Override
	public void itemStateChanged(ItemEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == payloadFilename) {
			browsePayload();
		}
		
		// Display text for source when it is clicked
		if (e.getSource() == sourcesTable) {
			int row = sourcesTable.rowAtPoint(e.getPoint());
			int col = sourcesTable.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				//Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());

				String name = (String) sourcesTable.getValueAt(row, 1);
				if (name != null && ! name.equalsIgnoreCase("NONE")) {
					System.out.println("Edit:" + sat.sourceFormat[row]);
					sourceFormatSelected = row;
					if (e.getClickCount() == 2) {
						String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
						EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.sourceFormatName[row] + ".format");
						editor.setVisible(true);
					}
					
					if (sourceStats != null)
						leftSourcesPanel.remove(sourceStats);
					
					sourceStats = new JPanel();
					sourceStats.setLayout(new BoxLayout(sourceStats, BoxLayout.Y_AXIS));
					leftSourcesPanel.add(sourceStats, BorderLayout.SOUTH);
					
					int numRsWords = sat.sourceFormat[sourceFormatSelected].getInt(TelemFormat.RS_WORDS);
					int headerLength = sat.sourceFormat[sourceFormatSelected].getInt(TelemFormat.HEADER_LENGTH);
					int frameLength = sat.sourceFormat[sourceFormatSelected].getFrameLength();
					int dataLength = sat.sourceFormat[sourceFormatSelected].getInt(TelemFormat.DATA_LENGTH);
					int trailerLength = 32 * numRsWords;
					
					JLabel labFrameLen = new JLabel("Frame Length: "+ frameLength );
					
					JLabel labHeaderLen = new JLabel("Header Length: " + headerLength);
					JLabel labDataLen = new JLabel("Data Length: " + dataLength );
					JLabel labRSWords = new JLabel("RS Words: " + numRsWords);
					JLabel labTrailerLen = new JLabel("Trailer Length: " + trailerLength);
					sourceStats.add(labFrameLen);
					sourceStats.add(labHeaderLen);
					sourceStats.add(labDataLen);
					sourceStats.add(labRSWords);
					sourceStats.add(labTrailerLen);
				
				}
				leftSourcesPanel.revalidate();
				leftSourcesPanel.repaint();
			}
		}
		
		// Display the payloads in a frame when the frame definition is clicked
		if (e.getSource() == framesTable) {
			int row = framesTable.rowAtPoint(e.getPoint());
			int col = framesTable.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());

				if (e.getClickCount() == 2) {
					String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
					EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.frameLayoutFilename[row]);
					editor.setVisible(true);
				}
//				String name = (String) framesTable.getValueAt(row, 1);
//				if (name != null && ! name.equalsIgnoreCase("NONE")) {
//					System.out.println("Edit:" + sat.frameLayoutFilename[row]);
//					String masterFolder = Config.currentDir + File.separator + FoxSpacecraft.SPACECRAFT_DIR;
//					EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.frameLayoutFilename[row]);
//					editor.setVisible(true);
//				}
				
				if (rightPanel1 != null)
					rightPanel.remove(rightPanel1);
				
				rightPanel1 = new JPanel();
				rightPanel.add(rightPanel1);
				rightPanel1.setLayout(new BorderLayout());

				TitledBorder heading2 = title("Frame: " + sat.frameLayout[row].name);
				rightPanel1.setBorder(heading2);
				
				JPanel stats = new JPanel();
				stats.setLayout(new BoxLayout(stats,BoxLayout.Y_AXIS));
				rightPanel1.add(stats, BorderLayout.NORTH);
				
				int calculatedDataLength = 0;
				
				// Populate table rightPanel1
				// read it from disk, just in case..
				FrameLayout frameLayout;
				try {
					frameLayout = new FrameLayout(sat.foxId, Spacecraft.SPACECRAFT_DIR + File.separator + sat.frameLayoutFilename[row]);
					if (frameLayout != null) {
						FrameTableModel frameTableModel = new FrameTableModel();

						JTable frameTable = new JTable(frameTableModel);
						frameTable.setAutoCreateRowSorter(true);
						JScrollPane scrollPane = new JScrollPane (frameTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						scrollPane.setPreferredSize(new Dimension(100,400));
						frameTable.setFillsViewportHeight(true);
						//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

						rightPanel1.add(scrollPane, BorderLayout.CENTER);
						TableColumn column = frameTable.getColumnModel().getColumn(0);
						column.setPreferredWidth(20);
						column = frameTable.getColumnModel().getColumn(1);
						column.setPreferredWidth(160);

						frameTable.addMouseListener(this);
						int numOfPayloads = frameLayout.getNumberOfPayloads();
						String[][] data = new String[numOfPayloads][3];
						for (int i=0; i< numOfPayloads; i++) {
							data[i][0] =""+i;
							data[i][1] = frameLayout.getPayloadName(i);
							int len = frameLayout.getPayloadLength(i);
							calculatedDataLength += len;

//							BitArrayLayout layout = sat.getLayoutByName(frameLayout.getPayloadName(i));
//							int realLen = layout.getMaxNumberOfBytes();
							data[i][2] = ""+len ;
//							if (realLen != len) {
//								data[i][2] = data[i][2]  + " ERROR";
//							}

						}
						
						frameTableModel.setData(data);
						
						//rightPanel1.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
						
						if (sat.sourceFormat == null || sat.sourceFormat[sourceFormatSelected] == null) {
							Log.errorDialog("MISSING", "No Source Format defined.  Can't calculate lengths\n");
						} else {
							int headerLength = sat.sourceFormat[sourceFormatSelected].getInt(TelemFormat.HEADER_LENGTH);
							calculatedDataLength += headerLength;
							int frameLength = sat.sourceFormat[sourceFormatSelected].getFrameLength();
							int dataLength = sat.sourceFormat[sourceFormatSelected].getInt(TelemFormat.DATA_LENGTH);
							int trailerLength = sat.sourceFormat[sourceFormatSelected].getTrailerLength();
							int calculatedFrameLength = calculatedDataLength + trailerLength;
							JLabel labFrameLen = new JLabel("Length of this frame: " + calculatedFrameLength + "   ( Format: " + frameLength + " )");
							if (frameLength < calculatedFrameLength) {
								labFrameLen.setForeground(Config.AMSAT_RED);
							}
							JLabel labHeaderLen = new JLabel("Header Length: " + headerLength);
							JLabel labDataLen = new JLabel("Data Length: " + calculatedDataLength + "   ( Format: " + dataLength + " )");
							if (dataLength < calculatedDataLength) {
								labDataLen.setForeground(Config.AMSAT_RED);
							}
							JLabel labTrailerLen = new JLabel("Trailer Length: " + trailerLength);
							stats.add(labFrameLen);
							stats.add(labHeaderLen);
							stats.add(labDataLen);
							stats.add(labTrailerLen);
						}
						
					}
					rightPanel.revalidate();
					rightPanel.repaint();
				} catch (LayoutLoadException e1) {
					Log.errorDialog("ERROR", "Error in the frame layout\n" + e1);
//				}  catch (Exception e1) {
//					Log.errorDialog("ERROR", "Can't display the frame layout\n" + e1);
				}
				
				
			}
		}
		if (e.getSource() == payloadsTable) {
			if (sat.numberOfLayouts == 0) return;
			int row = payloadsTable.rowAtPoint(e.getPoint());
			int col = payloadsTable.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());

				payloadName.setText(sat.layout[row].name);
				payloadFilename.setText(sat.layoutFilename[row]);
				payloadType.setSelectedItem((String)sat.layout[row].typeStr);
				
				if (e.getClickCount() == 2) {
					String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
					//EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.layoutFilename[row]);
					File file = new File(masterFolder + File.separator + sat.layoutFilename[row]);
					try {
						Desktop.getDesktop().open(file);
					} catch (IOException e1) {
						Log.errorDialog("ERROR", "Could not open payload file\n"+e1);
					}
					
					//editor.setVisible(true);
				}
			}
		}

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
