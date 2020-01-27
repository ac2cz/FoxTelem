package gui;

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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
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
import common.Config;
import common.FoxSpacecraft;

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
public class SpacecraftFrame extends JDialog implements ItemListener, ActionListener, FocusListener, WindowListener {

	private final JPanel contentPanel = new JPanel();
	JTextField displayName;
	JTextField name;
	JComboBox<String> priority;
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
	JCheckBox[] sendLayoutToServer;
	
	JCheckBox useIHUVBatt;
	JCheckBox track;
	JComboBox<String> cbMode;
	
	JButton btnCancel;
	JButton btnSave;
	JButton btnGetT0;
	T0SeriesTableModel t0TableModel;
	
	FoxSpacecraft sat;

	int headerSize = 12;
	
	/**
	 * Create the dialog.
	 */
	public SpacecraftFrame(FoxSpacecraft sat, JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Spacecraft paramaters");
		this.sat = sat;
		addWindowListener(this);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		contentPanel.setLayout(new BorderLayout(0, 0));
		addFields();
		
		addButtons();
		
	}
	
	private void addFields() {
		JPanel titlePanel = new JPanel();
		contentPanel.add(titlePanel, BorderLayout.NORTH);
		titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
		
		JPanel titlePanel1 = new JPanel();
		titlePanel.add(titlePanel1, BorderLayout.NORTH);
		titlePanel1.setLayout(new FlowLayout(FlowLayout.LEFT));

		JPanel titlePanel2 = new JPanel();
		titlePanel.add(titlePanel2, BorderLayout.NORTH);
		titlePanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		TitledBorder heading0 = title("Identification");
		titlePanel.setBorder(heading0);

		//JLabel lName = new JLabel("Name: " + sat.name);
		name = addSettingsRow(titlePanel1, 8, "Name (for Keps)", 
				"The name must be the same as the name in your TLE/Keps file if you want to calculate positions or sync with SatPC32", ""+sat.user_keps_name);
		titlePanel1.add(name);
		displayName = addSettingsRow(titlePanel2, 15, "Display Name", 
				"This name is use used as a label on Graphs and Tabs", ""+sat.user_display_name);
		titlePanel2.add(displayName);
		JLabel lId = new JLabel("     ID: " + sat.foxId);
		titlePanel1.add(lId);

		JLabel lblPriority = new JLabel("    Priority");
		titlePanel1.add(lblPriority);
		String tip = "The highest priority spacecraft is tracked if more than one is above the horizon";
		lblPriority.setToolTipText(tip);
		String[] nums = new String[20];
		for (int i=0; i < nums.length; i++)
			nums[i] = ""+i;
		priority = new JComboBox<String>(nums); 
		priority.setSelectedIndex(sat.user_priority);
		titlePanel1.add(priority);

		// Left Column - Fixed Params that can not be changed
		JPanel leftPanel = new JPanel();
		contentPanel.add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		
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
			lExp[i] = new JLabel("Experiment "+(i+1)+": " + FoxSpacecraft.expNames[sat.experiments[i]]);
			leftFixedPanel.add(lExp[i]);
		}


		JPanel t0Panel = new JPanel();
		leftPanel.add(t0Panel);
		
		TitledBorder headingT0 = title("Time Zero");
		t0Panel.setBorder(headingT0);
		t0Panel.setLayout(new BoxLayout(t0Panel, BoxLayout.Y_AXIS));
		//t0Panel.setLayout(new BorderLayout());
		t0TableModel = new T0SeriesTableModel();
		
		JTable table = new JTable(t0TableModel);
		table.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		table.setFillsViewportHeight(true);
	//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		t0Panel.add(scrollPane);//, BorderLayout.WEST);
		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(20);
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(60);

		updateTimeSeries();
		
		btnGetT0 = new JButton("Update T0 from Server");
		btnGetT0.addActionListener(this);
		t0Panel.add(btnGetT0);//, BorderLayout.WEST);
		
		JPanel localServerPanel = new JPanel();
		leftPanel.add(localServerPanel);
		
		if (sat.user_localServer != null) {
			TitledBorder localServerPanelHeader = title("COSMOS TCP Interface");
			localServerPanel.setBorder(localServerPanelHeader);
			localServerPanel.setLayout(new BoxLayout(localServerPanel, BoxLayout.Y_AXIS));

			localServer = addSettingsRow(localServerPanel, 15, "Server", 
					"The IP address or domain name of the local server", "" + sat.user_localServer);
			localServerPort = addSettingsRow(localServerPanel, 15, "Port", 
					"The port of the local Server", ""+sat.user_localServerPort);
			
			/*
			sendLayoutToServer = new JCheckBox[sat.numberOfLayouts];
			for (int i=0; i<sat.numberOfLayouts; i++) {
				sendLayoutToServer[i] = new JCheckBox("Send "+ sat.layout[i].name);
				localServerPanel.add(sendLayoutToServer[i]);
				if (sat.sendLayoutLocally[i]) sendLayoutToServer[i].setSelected(true); else sendLayoutToServer[i].setSelected(false);
			}
			*/
		}

		leftPanel.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		// Right Column - Things the user can change - e.g. Layout Files, Freq, Tracking etc
		JPanel rightPanel = new JPanel();
		contentPanel.add(rightPanel, BorderLayout.CENTER);
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		JPanel rightPanel1 = new JPanel();
		rightPanel.add(rightPanel1);
		rightPanel1.setLayout(new BoxLayout(rightPanel1, BoxLayout.Y_AXIS));
		
		TitledBorder heading2 = title("Frequency and Tracking");
		rightPanel1.setBorder(heading2);

		cbMode = this.addComboBoxRow(rightPanel1, "Mode", tip, SourceTab.formats);
		setSelection(cbMode, SourceTab.formats, SourceTab.formats[sat.user_format]);
				
		telemetryDownlinkFreqkHz = addSettingsRow(rightPanel1, 15, "Downlink Freq (kHz)", 
				"The nominal downlink frequency of the spacecraft", ""+sat.user_telemetryDownlinkFreqkHz);
		minFreqBoundkHz = addSettingsRow(rightPanel1, 15, "Lower Freq Bound (kHz)", 
				"The lower frequency boundry when we are searching for the spacecraft signal", ""+sat.user_minFreqBoundkHz);
		maxFreqBoundkHz = addSettingsRow(rightPanel1, 15, "Upper Freq Bound (kHz)", 
				"The upper frequency boundry when we are searching for the spacecraft signal", ""+sat.user_maxFreqBoundkHz);
		track = addCheckBoxRow("Track when Find Signal Enabled", "When Find Signal is enabled include this satellite in the search", sat.user_track, rightPanel1 );
		rightPanel1.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		JPanel rightPanel2 = new JPanel();
		rightPanel.add(rightPanel2);
		rightPanel2.setLayout(new BoxLayout(rightPanel2, BoxLayout.Y_AXIS));
		
		TitledBorder heading3 = title("Calibration");
		rightPanel2.setBorder(heading3);

		BATTERY_CURRENT_ZERO = addSettingsRow(rightPanel2, 25, "Battery Current Zero", 
				"The calibration paramater for zero battery current", ""+sat.user_BATTERY_CURRENT_ZERO);

		rssiLookUpTableFileName = addSettingsRow(rightPanel2, 25, "RSSI Lookup Table", 
				"The file containing the lookup table for Received Signal Strength", ""+sat.getLookupTableFileNameByName(Spacecraft.RSSI_LOOKUP));
		ihuTempLookUpTableFileName = addSettingsRow(rightPanel2, 25, "IHU Temp Lookup Table", 
				"The file containing the lookup table for the IHU Temperature", ""+sat.getLookupTableFileNameByName(Spacecraft.IHU_TEMP_LOOKUP));
		ihuVBattLookUpTableFileName = addSettingsRow(rightPanel2, 25, "VBatt Lookup Table", 
				"The file containing the lookup table for the Battery Voltage", ""+sat.getLookupTableFileNameByName(Spacecraft.IHU_VBATT_LOOKUP));
	
		rssiLookUpTableFileName.setEnabled(false);
		ihuTempLookUpTableFileName.setEnabled(false);
		ihuVBattLookUpTableFileName .setEnabled(false);
		
		useIHUVBatt = addCheckBoxRow("Use Bus Voltage as VBatt", "Read the Bus Voltage from the IHU rather than the Battery "
				+ "Voltage from the battery card using I2C", sat.useIHUVBatt, rightPanel2 );
		if (sat.hasMpptSettings) {
			mpptResistanceError = addSettingsRow(rightPanel2, 25, "MPPT Resistance Error", 
					"The extra resistance in the RTD temperature measurement circuit", ""+sat.user_mpptResistanceError);
			mpptSensorOffThreshold = addSettingsRow(rightPanel2, 25, "MPPT Sensor Off Threshold", 
					"The ADC value when the temperature sensor is considered off", ""+sat.user_mpptSensorOffThreshold);
		}
		if (sat.hasMemsRestValues) {
			memsRestValueX = addSettingsRow(rightPanel2, 25, "MEMS Rest Value X", 
					"The rest value for the MEMS X rotation sensor", ""+sat.user_memsRestValueX);
			memsRestValueY = addSettingsRow(rightPanel2, 25, "MEMS Rest Value Y", 
					"The rest value for the MEMS Y rotation sensor", ""+sat.user_memsRestValueY);
			memsRestValueZ = addSettingsRow(rightPanel2, 25, "MEMS Rest Value Z", 
					"The rest value for the MEMS Z rotation sensor", ""+sat.user_memsRestValueZ);
		}
		rightPanel2.add(new Box.Filler(new Dimension(10,10), new Dimension(100,400), new Dimension(100,500)));

		
		// Bottom panel for description
		JPanel footerPanel = new JPanel();
		TitledBorder heading9 = title("Description");
		footerPanel.setBorder(heading9);

		JTextArea taDesc = new JTextArea(2, 45);
		taDesc.setText(sat.description);
		taDesc.setLineWrap(true);
		taDesc.setWrapStyleWord(true);
		taDesc.setEditable(false);
		taDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
		footerPanel.add(taDesc);
		contentPanel.add(footerPanel, BorderLayout.SOUTH);
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
	private void updateTimeSeries() {
		String[][] data = null;
		if (sat.hasTimeZero()) {
			data = sat.getT0TableData();
		}			
		if (data == null) {
			data = new String[1][2];
			data[0][0] = "0";
			data[0][1] = "Time origin missing";
		}
		t0TableModel.setData(data);
		
		
	}
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	private void addButtons() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		btnSave = new JButton("Save");
		btnSave.setActionCommand("Save");
		buttonPane.add(btnSave);
		btnSave.addActionListener(this);
		getRootPane().setDefaultButton(btnSave);


		btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);
		btnCancel.addActionListener(this);

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

	@Override
	public void actionPerformed(ActionEvent e) {
		boolean refreshTabs = false;
		boolean rebuildMenu = false;
		if (e.getSource() == btnGetT0) {
			MainWindow.updateManager.updateT0(sat);
			updateTimeSeries();
		}
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == btnSave) {
			boolean dispose = true;
			double downlinkFreq = 0;
			double minFreq = 0;
			double maxFreq = 0;
			try {
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
//				if (!sat.ihuTempLookUpTableFileName.equalsIgnoreCase(ihuTempLookUpTableFileName.getText())) {
//					sat.ihuTempLookUpTableFileName = ihuTempLookUpTableFileName.getText();
//					refreshTabs = true;
//				}
	//			if (!sat.ihuVBattLookUpTableFileName.equalsIgnoreCase(ihuVBattLookUpTableFileName.getText())) {
	//				sat.ihuVBattLookUpTableFileName = ihuVBattLookUpTableFileName.getText();
	//				refreshTabs = true;
	//			}
				
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

				if (dispose) {
					sat.save();
					Config.initSatelliteManager();
					if (rebuildMenu) {
						Config.mainWindow.initSatMenu();
						MainWindow.frame.repaint();
					}
					this.dispose();
					if (refreshTabs)
						MainWindow.refreshTabs(false);
				}
			} catch (NumberFormatException Ex) {
				Log.errorDialog("Invalid Paramaters", Ex.getMessage());
			}
		}

	}
	
	public void saveProperties() {
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow", "windowHeight", this.getHeight());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow", "windowWidth", this.getWidth());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow", "windowX", this.getX());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow",  "windowY", this.getY());
	}
	
	public void loadProperties() {
		int windowX = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowX");
		int windowY = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowY");
		int windowWidth = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowWidth");
		int windowHeight = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowHeight");
		if (windowX == 0 || windowY == 0 ||windowWidth == 0 ||windowHeight == 0) {
			setBounds(100, 100, 600, 700);
		} else {
			setBounds(windowX, windowY, windowWidth, windowHeight);
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
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
