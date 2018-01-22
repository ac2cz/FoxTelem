package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.Log;

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
public class InitalSettings extends JDialog implements ActionListener, WindowListener {

	private JPanel contentPane;
//	private JPanel buttons;
	private JPanel directories;
	
	//JRadioButton typical;
	//JRadioButton custom;

	private JTextField txtLogFileDirectory;
	JLabel title;
	JLabel lab;
	JLabel lab2;
	JLabel lab3;
	JLabel lab4;
	
	JButton btnContinue;
	JButton btnCancel;
	JButton btnBrowse;
	
	public InitalSettings(JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Welcome too the Amsat Fox Telemetry Analysis Tool");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//setBounds(100, 100, 650, 200);
		//this.setResizable(false);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
	
		JPanel top = new JPanel();
		contentPane.add(top, BorderLayout.NORTH);
		JPanel center = new JPanel();
		contentPane.add(center, BorderLayout.CENTER);
		JPanel bottom = new JPanel();
		contentPane.add(bottom, BorderLayout.SOUTH);
		
		title = new JLabel();
		lab = new JLabel();
		lab2 = new JLabel();
		lab3 = new JLabel();
		lab4 = new JLabel();
		title.setFont(new Font("SansSerif", Font.BOLD, 14));
		
		title.setText("AMSAT Fox Satellite Decoder");
		lab.setText("It looks like this is the first time you have run the FoxTelem program. You must choose a directory to store the the decoded data.");
		lab2.setText("Also note that configuration settings will be saved in:  " + Config.homeDirectory);
		lab3.setText("If you want to run multiple copies of FoxTelem, using different settings, then hit cancel and rerun passing the ");
		lab4.setText("logFile directory name as a paramater.  Otherwise choose a logFile directory below.  See the manual for details.");
		
		JPanel titlePanel = new JPanel();
		top.setLayout(new BorderLayout(0, 0));
		top.add(titlePanel,BorderLayout.NORTH);
		titlePanel.add(title);
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		top.add(textPanel, BorderLayout.CENTER);
		textPanel.add(lab);
		textPanel.add(lab2);
		textPanel.add(lab3);
		textPanel.add(lab4);
		directories = FilesPanel();
		directories.setVisible(true);
		center.add(directories);
		
		bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		btnContinue = new JButton("Continue");
		btnContinue.addActionListener(this);
		bottom.add(btnContinue);
		
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		bottom.add(btnCancel);

		pack();
		setLocationRelativeTo(null);
	}
	
	private JPanel FilesPanel() {
		JPanel northpanel = new JPanel();
		//JPanel northcolumnpanel = addColumn(northpanel);
		//txtLogFileDirectory = addSettingsRow(northpanel, 20, "Log files directory", Config.logFileDirectory);
		//JPanel panel = new JPanel();
		//northpanel.add(panel);
		northpanel.setLayout(new BorderLayout());
		
		JLabel lblDisplayModuleFont = new JLabel("Log files directory");
		lblDisplayModuleFont.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel.add(lblDisplayModuleFont, BorderLayout.WEST);
		txtLogFileDirectory = new JTextField(Config.logFileDirectory);
		northpanel.add(txtLogFileDirectory, BorderLayout.CENTER);
		txtLogFileDirectory.setColumns(30);
		
		txtLogFileDirectory.addActionListener(this);

		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		northpanel.add(btnBrowse, BorderLayout.EAST);
		//TitledBorder title = new TitledBorder(null, "Files and Directories", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		//northpanel.setBorder(title);
		
		return northpanel;

	}
		
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			System.exit(1);
		}
		if (e.getSource() == btnContinue) {
				// We are done
				saveAndExit();
		}

		if (e.getSource() == btnBrowse) {
			File dir = null;
			if (!Config.logFileDirectory.equalsIgnoreCase("")) {
				dir = new File(Config.logFileDirectory);
			}
			if(Config.useNativeFileChooser && !Config.isWindowsOs()) { // not on windows because native dir chooser does not work
				// use the native file dialog on the mac
				System.setProperty("apple.awt.fileDialogForDirectories", "true");
				FileDialog fd =
						new FileDialog(this, "Choose Directory for Log Files",FileDialog.LOAD);
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
					txtLogFileDirectory.setText(selectedFile.getAbsolutePath());
				}
			} else {
				JFileChooser fc = new JFileChooser();
				fc.setApproveButtonText("Choose");
				if (dir != null) {
					fc.setCurrentDirectory(dir);	
				}		
				fc.setDialogTitle("Choose Directory for Log Files");
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setPreferredSize(new Dimension(Config.windowFcWidth, Config.windowFcHeight));
				int returnVal = fc.showOpenDialog(this);
				Config.windowFcHeight = fc.getHeight();
				Config.windowFcWidth = fc.getWidth();	

				if (returnVal == JFileChooser.APPROVE_OPTION) { 
					txtLogFileDirectory.setText(fc.getSelectedFile().getAbsolutePath());
				} else {
					System.out.println("No Selection ");
				}
			}


		}

	}

	void saveAndExit() {
		Config.FIRST_RUN = false;
		Config.firstRun106 = false; // dont need to tell a brand new user about changes from version to version
		if (!Config.logFileDirectory.equalsIgnoreCase(txtLogFileDirectory.getText())) {
			// user changed the logfile directory
			File file = new File(txtLogFileDirectory.getText());
			if (!file.isDirectory() || file == null || !file.exists()){
			    Log.errorDialog("Invalid directory", "Can not find the specified directory: " + txtLogFileDirectory.getText());
			} else {
				Config.logFileDirectory = txtLogFileDirectory.getText();
				this.dispose();
			}
		} else {
			this.dispose();
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		//System.exit(1);
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	

}
