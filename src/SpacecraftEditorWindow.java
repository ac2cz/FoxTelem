import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import gui.MainWindow;
import telemetry.LayoutLoadException;

public class SpacecraftEditorWindow extends JFrame implements WindowListener, ActionListener {
	// Swing File Chooser
	JFileChooser fc = null;
	//AWT file chooser for the Mac
	FileDialog fd = null;
	JMenuBar menuBar;
	//Menu Buttons
	JMenuItem mntmExit;
	static JMenuItem mntmLoadSpacecraftFile;
	
	JPanel spacecraftPanel;

	private static final long serialVersionUID = 1L;
	public SpacecraftEditorWindow() {
		initialize();
		JPanel mainPanel = new JPanel();
		initMenu();
		getContentPane().add(mainPanel);
		layoutMainPanel(mainPanel);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setBounds(100, 100, 1000, 600);
		//setBounds(Config.windowX, Config.windowY, Config.windowWidth, Config.windowHeight);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle("AMSAT Spacecraft Config Editor");
		addWindowListener(this);
		//addWindowStateListener(this);

		fd = new FileDialog(MainWindow.frame, "Select Spacecraft file",FileDialog.LOAD);
		//	fd.setFile("*.MASTER");
		//} else {
		fc = new JFileChooser();
	}

	private void initMenu() {
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		mntmLoadSpacecraftFile = new JMenuItem("Load Spacecraft");
		mnFile.add(mntmLoadSpacecraftFile);
		mntmLoadSpacecraftFile.addActionListener(this);

		mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		mntmExit.addActionListener(this);

	}

	private void layoutMainPanel(JPanel panel) {
		panel.setLayout(new BorderLayout());
		spacecraftPanel = new JPanel();
		panel.add(spacecraftPanel, BorderLayout.WEST);
	}

	/**
	 * Allow the user to choose a new spacecraft file to load
	 */
	private void openSpacecraft() {
		File file = null;
		File destinationDir = null;
		File dir = null;

		dir = new File(Config.currentDir+"/spacecraft");
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			destinationDir = new File(Config.logFileDirectory+"/spacecraft");
		} else {
			destinationDir = dir;
		}

		if(Config.useNativeFileChooser && !Config.isLinuxOs()) { // not on Linux because the Native File Chooser does not filter files 
			// use the native file dialog on the mac

			fd.setFile("*.MASTER");
			fd.setTitle("Select spacecraft MASTER file to open");

			if (dir != null) {
				fd.setDirectory(dir.getAbsolutePath());
			}
			fd.setVisible(true);
			String filename = fd.getFile();
			String dirname = fd.getDirectory();
			if (filename == null) {
				Log.println("You cancelled the choice");
				file = null;
			} else {
				Log.println("File: " + filename);
				Log.println("DIR: " + dirname);
				file = new File(dirname + filename);
			}	
		} else {
			fc.setPreferredSize(new Dimension(Config.windowFcWidth, Config.windowFcHeight));

			fc.setDialogTitle("Select spacecraft MASTER file to open");
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"Spacecraft files", "MASTER");
			fc.setFileFilter(filter);
			fc.setApproveButtonText("Add");

			if (dir != null)
				fc.setCurrentDirectory(dir);
			// This toggles the details view on
			//		Action details = fc.getActionMap().get("viewTypeDetails");
			//		details.actionPerformed(null);

			int returnVal = fc.showOpenDialog(this);
			Config.windowFcHeight = fc.getHeight();
			Config.windowFcWidth = fc.getWidth();		

			//Config.save();
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
				Log.println("File: " + file.getName());
				Log.println("DIR: " + file.getPath());
			} else
				file = null;
		}
		Config.save();
		if (file !=null) {
			boolean refresh = false;

			String targetName = file.getName().replace(".MASTER", ".dat");
			File targetFile = new File(destinationDir.getPath()+ File.separator + targetName);

			Log.println("Opening " + file.getAbsolutePath() );

			if (file.exists()) {
				try {
					//SatPayloadStore.copyFile(file, targetFile);
					try {
						FoxSpacecraft satellite = new FoxSpacecraft(Config.satManager, file, targetFile);
						JLabel title = new JLabel(satellite.getIdString());
						spacecraftPanel.add(title);
						//satellite.save();
					} catch (LayoutLoadException e) {
						Log.errorDialog("Layout Issue", "Could not fully parse the spacecraft file.  It may not be installed\n"+e.getMessage());
						// But carry on.  Hopefully the new MASTER file will fix it!
						e.printStackTrace(Log.getWriter()); // but log if user has that enabled
					}
					refresh = true;
				} catch (IOException e) {
					Log.errorDialog("ERROR Copy File", "Could not copy the spacecraft file\n"+e.getMessage());
					e.printStackTrace(Log.getWriter());
				}
			} else {

			}

		}
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		// MENU ACTIONS
		if (e.getSource() == mntmExit) {
			this.windowClosing(null);
		}
		if (e.getSource() == mntmLoadSpacecraftFile) {
			openSpacecraft();
		}

	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowClosed(WindowEvent arg0) {
		// This is once dispose has run
	}
	@Override
	public void windowClosing(WindowEvent arg0) {
		// close has been requested from the X or otherwise
		this.dispose();

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
