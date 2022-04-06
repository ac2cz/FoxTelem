package spacecraftEditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;

import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import common.Config;
import common.Location;
import common.Log;
import common.Spacecraft;
import common.TlmServer;
import gui.MainWindow;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

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
public class EditorSettingsFrame extends JDialog implements ActionListener, ItemListener, FocusListener, WindowListener {

	JPanel contentPane;
	
	JTextField txtLogFileDirectory, txtMasterFileDirectory;
	
	JButton btnSave;
	JButton btnCancel;
	JButton btnBrowse, btnBrowseMaster;
	JCheckBox cbLogging;
	JTextField txtPython,txtPayloadHeaderGenScript;
	
	/**
	 * Create the Dialog
	 */
	public EditorSettingsFrame(JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Settings");
		addWindowListener(this);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		//this.setResizable(false);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		// South panel for the buttons
		JPanel southpanel = new JPanel();
		contentPane.add(southpanel, BorderLayout.SOUTH);
		southpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		btnSave = new JButton("Save");
		btnSave.addActionListener(this);
		southpanel.add(btnSave);
		getRootPane().setDefaultButton(btnSave);
		
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		southpanel.add(btnCancel);
		
		// North panel for settings that span across the window
		JPanel northpanel = new JPanel();
		contentPane.add(northpanel, BorderLayout.NORTH);
		northpanel.setLayout(new BorderLayout());

		JPanel northpanel1 = new JPanel();
		JPanel northpanel2 = new JPanel();
		JPanel northpanelA = new JPanel();
		JPanel northpanelB = new JPanel();
		northpanel.add(northpanel1, BorderLayout.NORTH);
		northpanel1.setLayout(new BorderLayout());
		northpanel1.add(northpanelA, BorderLayout.NORTH);
		northpanel1.add(northpanelB, BorderLayout.SOUTH);
		northpanel.add(northpanel2, BorderLayout.SOUTH);
		
		northpanel2.setLayout(new BorderLayout());
		northpanelA.setLayout(new BorderLayout());
		northpanelB.setLayout(new BorderLayout());
		
		JLabel lblHomeDir = new JLabel("Home directory     ");
		lblHomeDir.setToolTipText("This is the directory that contains the settings file");
		lblHomeDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanelA.add(lblHomeDir, BorderLayout.WEST);

		JLabel lblHomeDir2 = new JLabel(Config.homeDirectory);
		northpanelA.add(lblHomeDir2, BorderLayout.CENTER);

		JLabel lblServerUrl = new JLabel("MASTER files directory  ");
		lblServerUrl.setToolTipText("This contains the spacecraft folder with the MASTER files that will be edited");
		lblServerUrl.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanelB.add(lblServerUrl, BorderLayout.WEST);
		
		txtMasterFileDirectory = new JTextField(Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR);
		northpanelB.add(txtMasterFileDirectory, BorderLayout.CENTER);
		txtMasterFileDirectory.setColumns(30);
		
		txtMasterFileDirectory.addActionListener(this);
		
		btnBrowseMaster = new JButton("Browse");
		btnBrowseMaster.addActionListener(this);
		northpanelB.add(btnBrowseMaster, BorderLayout.EAST);

		
		JLabel lblLogFilesDir = new JLabel("Log files directory");
		lblLogFilesDir.setToolTipText("This sets the directory that the downloaded telemetry data is stored in");
		lblLogFilesDir.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel2.add(lblLogFilesDir, BorderLayout.WEST);
		
		txtLogFileDirectory = new JTextField(Config.logFileDirectory);
		northpanel2.add(txtLogFileDirectory, BorderLayout.CENTER);
		txtLogFileDirectory.setColumns(30);
		
		txtLogFileDirectory.addActionListener(this);

		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		northpanel2.add(btnBrowse, BorderLayout.EAST);
		
		if (Config.logDirFromPassedParam) {
			txtLogFileDirectory.setEnabled(false);
			btnBrowse.setVisible(false);
			JLabel lblPassedParam = new JLabel("  (Fixed at Startup)");
			northpanel2.add(lblPassedParam, BorderLayout.EAST);
		}
//		TitledBorder eastTitle1 = new TitledBorder(null, "<html><body> <h3>Files and Directories</h3></body></html>", TitledBorder.LEADING, TitledBorder.TOP, null, null); 
		TitledBorder eastTitle1 = title("Files and Directories");
		northpanel.setBorder(eastTitle1);
		
		// Center panel for settings
		JPanel centerpanel = new JPanel();
		contentPane.add(centerpanel, BorderLayout.CENTER);
		TitledBorder centerTitle1 = title("Options");
		centerpanel.setBorder(centerTitle1);
		centerpanel.setLayout(new BoxLayout(centerpanel, BoxLayout.Y_AXIS));
		cbLogging = addCheckBoxRow("Enable Logging", "Log debug information to a file", Config.logging, centerpanel);
		txtPython = addSettingsRow(centerpanel, 20, "Path to Python","python executable name with folder if it is not in the path", Config.python);
		txtPayloadHeaderGenScript = addSettingsRow(centerpanel, 20, "C header Script","Name of the python script to generate the C headers", Config.payloadHeaderGenScript);

		centerpanel.add(new Box.Filler(new Dimension(0,1000), new Dimension(100,1000), new Dimension(1000,1000)));
	}

	public void saveProperties() {
		Config.saveGraphIntParam("Global", 0, 0, "settingsWindow", "windowHeight", this.getHeight());
		Config.saveGraphIntParam("Global", 0, 0, "settingsWindow", "windowWidth", this.getWidth());
		Config.saveGraphIntParam("Global", 0, 0, "settingsWindow", "windowX", this.getX());
		Config.saveGraphIntParam("Global", 0, 0, "settingsWindow",  "windowY", this.getY());
	}
	
	public void loadProperties() {
		int windowX = Config.loadGraphIntValue("Global", 0, 0, "settingsWindow", "windowX");
		int windowY = Config.loadGraphIntValue("Global", 0, 0, "settingsWindow", "windowY");
		int windowWidth = Config.loadGraphIntValue("Global", 0, 0, "settingsWindow", "windowWidth");
		int windowHeight = Config.loadGraphIntValue("Global", 0, 0, "settingsWindow", "windowHeight");
		if (windowX == 0 ||windowY == 0 ||windowWidth == 0 ||windowHeight == 0) {
			setBounds(100, 100, 400, 700);
		} else {
			setBounds(windowX, windowY, windowWidth, windowHeight);
		}
	}
	
	private TitledBorder title(String s) {
		TitledBorder title = new TitledBorder(null, s, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		return title;
	}
	
	private void setServerPanelEnabled(boolean en) {
		//serverPanel.setEnabled(en);
		//txtCallsign.setEnabled(en);
	//	txtPrimaryServer.setEnabled(en);
	//	txtSecondaryServer.setEnabled(en);
	//	cbUseUDP.setEnabled(en);
		//txtLatitude.setEnabled(en);
		//txtLongitude.setEnabled(en);
	}

	private JPanel addColumn(JPanel parent, int rows) {
		JPanel columnpanel = new JPanel();
		parent.add(columnpanel);
		//columnpanel.setLayout(new GridLayout(rows,1,10,10));
		columnpanel.setLayout(new BoxLayout(columnpanel, BoxLayout.Y_AXIS));

		return columnpanel;
	}


	private JCheckBox addCheckBoxRow(String name, String tip, boolean value, JPanel parent) {
		JCheckBox checkBox = new JCheckBox(name);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		parent.add(checkBox);
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
		panel.add(textField);
		textField.setColumns(length);
		textField.addActionListener(this);
		textField.addFocusListener(this);

//		column.add(new Box.Filler(new Dimension(10,5), new Dimension(10,5), new Dimension(10,5)));

		return textField;
	
	}


		

	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			this.dispose();
		}

		if (e.getSource() == btnSave) {
			boolean dispose = true;
			boolean refreshTabs = false;
			boolean refreshGraphs = false;

			String testString = Config.currentDir + File.separator+"spacecraft";
			if (!testString.equalsIgnoreCase(txtMasterFileDirectory.getText())) {
				File file = new File(txtMasterFileDirectory.getText());
				if ((!file.isDirectory() || file == null || !file.exists())){
					Log.errorDialog("Invalid directory", "Can not find the specified directory: " + txtMasterFileDirectory.getText());
					dispose = false;
					refreshGraphs=false;
				} else if (!txtMasterFileDirectory.getText().matches(".*"+File.separator+"spacecraft$")) {
					Log.errorDialog("Invalid directory", "Master file directory must be called 'spacecraft' :\n" + txtMasterFileDirectory.getText());
					dispose = false;
					refreshGraphs=false;
				} else {
					Object[] options = {"Yes",
					"No"};
					int n = JOptionPane.showOptionDialog(
							MainWindow.frame,
							"Do you want to switch MASTER spacecraft directories? You must restart the program to reload the spacecraft.  Unsaved changes will be lost.",
							"Do you want to continue?",
							JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);

					if (n == JOptionPane.YES_OPTION) {
						String txt = txtMasterFileDirectory.getText();
						String newDir = txt.replaceAll(File.separator+"spacecraft$", "");
						Config.editorCurrentDir = newDir;
						Config.currentDir = newDir;
						Log.println("Setting MASTER directory to: " + Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR);
						SpacecraftEditorWindow.displayDirs();
						Config.initSatelliteManager();
					
					}
				}
			}

			if (!Config.logFileDirectory.equalsIgnoreCase(txtLogFileDirectory.getText())) {
				boolean currentDir = false;
				if (txtLogFileDirectory.getText().equalsIgnoreCase(""))
					currentDir = true;

				File file = new File(txtLogFileDirectory.getText());
				if (!currentDir && (!file.isDirectory() || file == null || !file.exists())){
					Log.errorDialog("Invalid directory", "Can not find the specified directory: " + txtLogFileDirectory.getText());
					dispose = false;
					refreshGraphs=false;
				} else {

					Object[] options = {"Yes",
					"No"};
					int n = JOptionPane.showOptionDialog(
							MainWindow.frame,
							"Do you want to switch log file directories? You must restart the program to reload the spacecraft.  Unsaved changes will be lost.",
							"Do you want to continue?",
							JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);

					if (n == JOptionPane.YES_OPTION) {
						Config.logFileDirectory = txtLogFileDirectory.getText();
						Log.println("Setting log file directory to: " + Config.logFileDirectory);

						Config.initSatelliteManager();

						refreshTabs = true;
						refreshGraphs = true;

					}
				}		
			} 

			Config.logging = cbLogging.isSelected();
			Config.python = txtPython.getText();
			if (!txtPayloadHeaderGenScript.getText().equalsIgnoreCase("")) {
				File script = new File(System.getProperty("user.dir") + File.separator + txtPayloadHeaderGenScript.getText());
				if (script.exists()) {
					Config.payloadHeaderGenScript = txtPayloadHeaderGenScript.getText();
				} else {
					Log.errorDialog("ERROR", "Make sure the header generation script is in this folder:\n" + System.getProperty("user.dir") );
				}
			} else
				Config.payloadHeaderGenScript = txtPayloadHeaderGenScript.getText();

			if (dispose) {
				Config.save();
				this.dispose();
			}
			if (refreshTabs) {

				//			SpacecraftEditorWindow.removeSpacecraftTabs();
				//			SpacecraftEditorWindow.addSpacecraftTabs();
				// We are fully up, remove the database loading message
				//Config.fileProgress.updateProgress(100);
			}
		}

		if (e.getSource() == btnBrowse) {
			File initialdir = null;
			if (!Config.logFileDirectory.equalsIgnoreCase("")) {
				initialdir = new File(Config.logFileDirectory);
			}
			String dir = pickDir(initialdir);
			txtLogFileDirectory.setText(dir);			
		}
		if (e.getSource() == btnBrowseMaster) {
			File initialdir = null;
			if (!Config.currentDir.equalsIgnoreCase("")) {
				initialdir = new File(Config.currentDir);
			}
			String dir = pickDir(initialdir);
			txtMasterFileDirectory.setText(dir);			
		}

	}

	private String pickDir(File dir) {

		if(Config.isMacOs()) { // not on windows/Linux because native dir chooser does not work
			// use the native file DIR dialog on the mac
			System.setProperty("apple.awt.fileDialogForDirectories", "true");
			FileDialog fd =
					new FileDialog(this, "Choose Directory",FileDialog.LOAD);
			if (dir != null) {
				fd.setDirectory(dir.getAbsolutePath());
			}
			fd.setVisible(true);
			System.setProperty("apple.awt.fileDialogForDirectories", "false");

			String filename = fd.getFile();
			String dirname = fd.getDirectory();
			if (filename == null)
				Log.println("You cancelled the choice");
			else {
				Log.println("File: " + filename);
				Log.println("DIR: " + dirname);
				File selectedFile = new File(dirname + filename);
				return selectedFile.getAbsolutePath();
			}
		} else {
			JFileChooser fc = new JFileChooser();
			fc.setApproveButtonText("Choose");
			if (dir != null) {
				fc.setCurrentDirectory(dir);	
			}		
			fc.setDialogTitle("Choose Directory");
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setPreferredSize(new Dimension(Config.windowFcWidth, Config.windowFcHeight));
			int returnVal = fc.showOpenDialog(this);
			Config.windowFcHeight = fc.getHeight();
			Config.windowFcWidth = fc.getWidth();	

			if (returnVal == JFileChooser.APPROVE_OPTION) { 
				return fc.getSelectedFile().getAbsolutePath();
			} else {
				System.out.println("No Selection ");
			}
		}
		return "";

	}

	private int parseIntTextField(JTextField text) {
		int value = 0;

		try {
			value = Integer.parseInt(text.getText());
			if (value > 30) {
				value = 30;
				text.setText(Integer.toString(30));
			}
		} catch (NumberFormatException ex) {
			
		}
		return value;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		
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
