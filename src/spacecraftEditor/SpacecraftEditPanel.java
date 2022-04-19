package spacecraftEditor;

import java.awt.BorderLayout;
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
import java.io.File;
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
import common.Log;
import common.Spacecraft;
import gui.SourceTab;
import spacecraftEditor.listEditors.CsvTableModel;
import telemetry.Format.TelemFormat;
import common.Config;

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
 * The SpacecraftFrame is a tab that allows the spacecraft paramaters to
 * be viewed and edited.
 * 
 * The tab is structure as follows:
 * 
 * NORTH: Reserved
 * WEST: The spacecraft paramaters are shown and edited here
 * CENTER: A tabbed pane to edit the spacecraft lists e.g. frames, payloads, lookup tables
 * EAST: Reserved
 * SOUTH: Buttons Row
 *
 */
@SuppressWarnings("serial")
public class SpacecraftEditPanel extends JPanel implements ActionListener, ItemListener, FocusListener, MouseListener {

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
	JTextField localServerPort, IHU_SN, model;

	JCheckBox[] sendLayoutToServer;
	JCheckBox hasImprovedCommandReceiver, hasImprovedCommandReceiverII, hasModeInHeader, hasFrameCrc;
	JTextArea taDesc;

	JCheckBox useIHUVBatt;
	JCheckBox track,hasFOXDB_V3,useConversionCoeffs;
	JComboBox<String> cbMode, cbModel;
	JComboBox<String>[] cbExperiments;

	JButton btnCancel,btnSave;
	
	Spacecraft sat;

	JPanel leftSourcesPanel,leftPanel, rightPanel;

	public int sourceFormatSelected = 0; // the source format that the user has clicked

	int headerSize = 12;

	/**
	 * Create the dialog.
	 */
	public SpacecraftEditPanel(Spacecraft sat) {

		this.sat = sat;

		setBorder(new EmptyBorder(5, 5, 5, 5));

		setLayout(new BorderLayout(0, 0));

		leftPanel = addLeftPanel();
		add(leftPanel, BorderLayout.WEST);

		rightPanel = addRightPanel();
		add(rightPanel, BorderLayout.CENTER);


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
		hasFOXDB_V3.setEnabled(false);
		useConversionCoeffs = addCheckBoxRow("Use conversion coefficients (recommended)", "This is true for all new spacecraft", sat.useConversionCoeffs, titlePanel1 );
		useConversionCoeffs.setEnabled(false);
		JPanel descPanel = new JPanel();
		descPanel.setLayout(new BorderLayout());
		TitledBorder heading9 = title("Description");
		descPanel.setBorder(heading9);

		taDesc = new JTextArea(6, 45);
		taDesc.setText(sat.description);
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);
		taDesc.setEditable(true);
		taDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
		descPanel.add(taDesc, BorderLayout.CENTER);
		titlePanel1.add(descPanel);

		return mainTitlePanel;
	}

	private JPanel addLeftPanel() {

		// LEFT Column - Fixed Params that can not be changed

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		JPanel titlePanel = addTitlePanel();
		leftPanel.add(titlePanel);

		leftSourcesPanel = new JPanel();
		leftPanel.add(leftSourcesPanel);
		TitledBorder headingSources = title("Sources");
		leftSourcesPanel.setBorder(headingSources);
		leftSourcesPanel.setLayout(new BorderLayout());

		SourcesTableModel sourcesListTableModel = new SourcesTableModel();
		CsvTableModel sourceTableModel = new SourcesTableModel();
		SourceTableListEditPanel sourceTableListEditPanel = new SourceTableListEditPanel(sat, "Sources", sourcesListTableModel, sourceTableModel, this);
		leftSourcesPanel.add(sourceTableListEditPanel, BorderLayout.CENTER);
		
//		sourcesTable = new JTable(sourcesTableModel);
//		sourcesTable.setAutoCreateRowSorter(true);
//		JScrollPane sourcesScrollPane = new JScrollPane (sourcesTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		sourcesScrollPane.setPreferredSize(new Dimension(100,400));
//		sourcesTable.setFillsViewportHeight(true);
//		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//
//		leftSourcesPanel.add(sourcesScrollPane, BorderLayout.CENTER);
//		TableColumn column = null;
//		column = sourcesTable.getColumnModel().getColumn(0);
//		column.setPreferredWidth(100);
//		column = sourcesTable.getColumnModel().getColumn(1);
//		column.setPreferredWidth(100);
//		sourcesTable.addMouseListener(this);

//		String[][] data = new String[sat.numberOfSources][2];
//		for (int i=0; i< sat.numberOfSources; i++) {
//			data[i][0] =sat.sourceName[i];
//			if (sat.sourceFormatName != null) 
//				data[i][1] = sat.sourceFormatName[i];
//			else
//				data[i][1] ="NONE";
//		}
//		sourcesTableModel.setData(data);

		//leftSourcesPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		leftPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,100), new Dimension(100,500)));

		return leftPanel;

	}

	private JPanel addRightPanel() {
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		JPanel leftFixedPanel = new JPanel();
		JPanel leftFixedPanelf = new JPanel();
		leftFixedPanelf.setLayout(new BorderLayout());
		leftFixedPanel.setLayout(new BoxLayout(leftFixedPanel, BoxLayout.Y_AXIS));
		rightPanel.add(leftFixedPanelf);
		leftFixedPanelf.add(leftFixedPanel, BorderLayout.NORTH);

		TitledBorder heading = title("Fixed Paramaters");
		leftFixedPanel.setBorder(heading);

		//JLabel lModel = new JLabel("Model: " + Spacecraft.modelNames[sat.model]);
		//leftFixedPanel.add(lModel);
		cbModel = this.addComboBoxRow(leftFixedPanel, "Model", "", Spacecraft.modelNames);
		setSelection(cbModel, Spacecraft.modelNames, Spacecraft.modelNames[sat.model]);
		IHU_SN = addSettingsRow(leftFixedPanel, 15, "Computer (IHU) S/N)", 
				"The serial number of the onboard computer", ""+sat.IHU_SN);
		//JLabel lIhusn = new JLabel("IHU S/N: " + sat.IHU_SN);
		//leftFixedPanel.add(lIhusn);
		hasModeInHeader = addCheckBoxRow("Mode in header", "Every recevied frame will include the mode in the header", sat.hasModeInHeader, leftFixedPanel );
		hasFrameCrc = addCheckBoxRow("Frame CRC", "The last two bytes of the (BPSK) frame contain a CRC checksum", sat.hasFrameCrc, leftFixedPanel );
		hasImprovedCommandReceiver = addCheckBoxRow("Improved Command Receiver", "Set to true if this has the Improved Command Receiver", sat.hasImprovedCommandReceiver, leftFixedPanel );
		hasImprovedCommandReceiverII = addCheckBoxRow("Improved Command Receiver II", "Set to true if this has the Improved Command Receiver", sat.hasImprovedCommandReceiverII, leftFixedPanel );

		//JLabel icr = new JLabel("ICR: " + sat.hasImprovedCommandReceiver);
		//leftFixedPanel.add(icr);

		JLabel lExp[] = new JLabel[4];
		cbExperiments = new JComboBox[4];
		for (int i=0; i<4; i++) {
			//lExp[i] = new JLabel("Experiment "+(i+1)+": " + Spacecraft.expNames[sat.experiments[i]]);
			//leftFixedPanel.add(lExp[i]);
			cbExperiments[i] = this.addComboBoxRow(leftFixedPanel, "Experiment "+(i+1), "", Spacecraft.expNames);
			setSelection(cbExperiments[i], Spacecraft.expNames, Spacecraft.expNames[sat.experiments[i]]);
			
		}

		leftPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		JPanel leftPane1 = new JPanel();
		rightPanel.add(leftPane1);
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
		leftPane1.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		JPanel leftPane2 = new JPanel();
		rightPanel.add(leftPane2);
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

			//rssiLookUpTableFileName.setEnabled(false);
			//ihuTempLookUpTableFileName.setEnabled(false);
			//ihuVBattLookUpTableFileName .setEnabled(false);

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


	public static TitledBorder title(String s) {
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
			sat.useConversionCoeffs = useConversionCoeffs.isSelected();
			sat.description = taDesc.getText();

			sat.model = cbModel.getSelectedIndex();
			try {
				sat.IHU_SN = Integer.parseInt(IHU_SN.getText());
			} catch (NumberFormatException ex) {
				throw new NumberFormatException("The Serial number must be an integer value");
			}
			
			sat.hasModeInHeader = hasModeInHeader.isSelected();
			if (hasImprovedCommandReceiver.isSelected() && hasImprovedCommandReceiverII.isSelected()) {
				throw new NumberFormatException("Can not select ICR II if ICR is already selected");
			}
			sat.hasImprovedCommandReceiver = hasImprovedCommandReceiver.isSelected();
			sat.hasImprovedCommandReceiverII = hasImprovedCommandReceiverII.isSelected();
			sat.hasFrameCrc = hasFrameCrc.isSelected();
			
			for (int i=0; i<4; i++) {
				sat.experiments[i] = cbExperiments[i].getSelectedIndex();
			}
			
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
			int pri = 9;
			try {
				pri = priority.getSelectedIndex();
			} catch (NumberFormatException e2) {

			}
			sat.user_priority = pri;
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


	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == btnSave) {
			System.out.println("Saving ...");
			save();

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
	public void mousePressed(MouseEvent e) {
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

}
