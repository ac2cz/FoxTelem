package spacecraftEditor;

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
public class InitalEditorSettings extends JDialog implements ActionListener, WindowListener {

	private JPanel contentPane;
//	private JPanel buttons;
	private JPanel directories;
	
	//JRadioButton typical;
	//JRadioButton custom;

	private JTextField txtLogFileDirectory, txtMasterFileDirectory;
	JLabel title;
	JLabel lab;
	JLabel lab2;
	JLabel lab3;
	JLabel lab4, lab5, lab6, lab7, lab8;
	
	JButton btnContinue;
	JButton btnCancel;
	JButton btnBrowse, btnBrowseMaster;
	
	public InitalEditorSettings(JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Welcome to the Amsat Spacecraft MASTER file editor");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//setAlwaysOnTop(true);
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
		
		// TOP
		top.setLayout(new BorderLayout(0, 0));
		title = new JLabel();
				
		title.setFont(new Font("SansSerif", Font.BOLD, 14));
		
		title.setText("AMSAT Editor");

		JPanel titlePanel = new JPanel();
		
		top.add(titlePanel,BorderLayout.NORTH);
		titlePanel.add(title);
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		top.add(textPanel, BorderLayout.CENTER);
		addLabel("It looks like this is the first time you have run the editor. You must choose the directories where the files and data are stored", textPanel);
		addLabel(" ", textPanel);
		
		addLabel("The MASTER folder holds the spacecraft directory.  This is where the spacecraft MASTER files are stored. Don't choose the ",textPanel);
		addLabel("actual 'spacecraft' folder, choose the folder above it.  This is usually where FoxTelem is installed, but you can edit spacecraft ", textPanel);
		addLabel("files in another directory if you wish.  ", textPanel);
		addLabel(" ", textPanel);
		addLabel("You must also choose a working directory to store runtime files and keep track of the loaded spacecraft. FoxTelem ", textPanel);
		addLabel("calls this the 'log files directory'.  Usually it is different to the directory where the MASTER spacecraft files are stored.", textPanel);
		addLabel(" ", textPanel);
		addLabel("Choose the directories below.  See the manual for details.", textPanel);
		addLabel(" ", textPanel);
		// CENTER
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));


		JPanel masterFiles = MasterFilesPanel();
		center.add(masterFiles);
		
		
		directories = FilesPanel();
		center.add(directories);
		
		addLabel("Configuration settings will be saved in:  " + Config.homeDirectory,center);
		
		// BOTTOM
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
	
	private void addLabel(String text, JPanel panel) {
		JLabel lbl = new JLabel(text);
		panel.add(lbl);
	}
	
	private JPanel MasterFilesPanel() {
		JPanel northpanel = new JPanel();
		northpanel.setLayout(new BorderLayout());
		
		JLabel lblDisplayModuleFont = new JLabel("MASTER files directory");
		lblDisplayModuleFont.setBorder(new EmptyBorder(5, 2, 5, 5) );
		northpanel.add(lblDisplayModuleFont, BorderLayout.WEST);
		txtMasterFileDirectory = new JTextField(System.getProperty("user.dir"));
		northpanel.add(txtMasterFileDirectory, BorderLayout.CENTER);
		txtMasterFileDirectory.setColumns(30);
		
		txtMasterFileDirectory.addActionListener(this);

		btnBrowseMaster = new JButton("Browse");
		btnBrowseMaster.addActionListener(this);
		northpanel.add(btnBrowseMaster, BorderLayout.EAST);
		
		return northpanel;
	}
	
	private JPanel FilesPanel() {
		JPanel northpanel = new JPanel();
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
			String dir = pickDir();
			txtLogFileDirectory.setText(dir);
		}
		if (e.getSource() == btnBrowseMaster) {
			String dir = pickDir();
			txtMasterFileDirectory.setText(dir);
		}

	}
	
	private String pickDir() {
		File dir = null;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = new File(Config.logFileDirectory);
		}
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
		} 
		
		if (!Config.editorCurrentDir.equalsIgnoreCase(txtMasterFileDirectory.getText())) {
			// user changed the MASTER directory
			File file = new File(txtMasterFileDirectory.getText());
			if (!file.isDirectory() || file == null || !file.exists()){
			    Log.errorDialog("Invalid directory", "Can not find the specified directory: " + txtMasterFileDirectory.getText());
			} else {
				Config.editorCurrentDir = txtMasterFileDirectory.getText();
				this.dispose();
			}
		} 
		this.dispose();
		
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
