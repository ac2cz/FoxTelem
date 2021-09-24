package spacecraftEditor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import telemetry.LayoutLoadException;

public class SpacecraftEditorWindow extends JFrame implements WindowListener, ActionListener {
	
	public static final String VERSION_NUM = "0.4";
	public static final String VERSION = VERSION_NUM + " - 17 Aug 2021";
	
	// Swing File Chooser
	JFileChooser fc = null;
	//AWT file chooser for the Mac
	FileDialog fd = null;
	JMenuBar menuBar;
	//Menu Buttons
	JMenuItem mntmExit;
	JMenuItem mntmNewSpacecraftFile;
	JMenuItem mntmAddSpacecraftFile;
	
	JTabbedPane tabbedPane;
	ArrayList<Spacecraft> sats;
	SpacecraftEditPanel[] spacecraftTab;

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
		loadProperties();
		//setBounds(Config.windowX, Config.windowY, Config.windowWidth, Config.windowHeight);
		addWindowListener(this);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/pacsat.jpg")));
		this.setTitle("AMSAT Spacecraft Editor");
		
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

		mntmNewSpacecraftFile = new JMenuItem("New Spacecraft");
		mnFile.add(mntmNewSpacecraftFile);
		mntmNewSpacecraftFile.addActionListener(this);

		mntmAddSpacecraftFile = new JMenuItem("Add Spacecraft");
		mnFile.add(mntmAddSpacecraftFile);
		mntmAddSpacecraftFile.addActionListener(this);

		mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		mntmExit.addActionListener(this);

	}

	private void layoutMainPanel(JPanel panel) {
		panel.setLayout(new BorderLayout());
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(tabbedPane, BorderLayout.CENTER);
		addSpacecraftTabs(tabbedPane);
		JPanel footer = new JPanel();
		JLabel lblVersion = new JLabel("Version: " + VERSION );
		footer.add(lblVersion);
		JLabel lblHomeDir = new JLabel( "  |  Current: " + Config.currentDir +"  |  Home: " + Config.homeDirectory + "  |  Log files: " + Config.logFileDirectory);
		footer.add(lblHomeDir);
		panel.add(footer, BorderLayout.SOUTH);
	}
	
	public void addSpacecraftTabs(JTabbedPane tabbedPane) {
		sats = Config.satManager.getSpacecraftList();
		spacecraftTab = new SpacecraftEditPanel[sats.size()];
		for (int s=0; s<sats.size(); s++) {
			spacecraftTab[s] = new SpacecraftEditPanel(sats.get(s));

				tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
						//			tabbedPane.addTab( ""  
						+ sats.get(s).propertiesFile.getName() + "</b></body></html>", spacecraftTab[s] );
				//			+" Health", healthTab );
		}
	}
	
	/**
	 * Create a new MASTER file and generate the defaults.  Load into the workspace.
	 * 
	 */
	private void newSpacecraft() {
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
			fd.setTitle("Specify new MASTER spacecraft file to create");

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

			fc.setDialogTitle("Specify new MASTER spacecraft file to create");
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"Spacecraft files", "MASTER");
			fc.setFileFilter(filter);
			fc.setApproveButtonText("Create");

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
			if (file.exists()) {
				Object[] options = {"Yes",
		        "No"};
				int n = JOptionPane.showOptionDialog(
						MainWindow.frame,
						"This will delete the existing MASTER spacecraft file: " + file.getName() + "\n"
						+ "Select yes to replace this with a new blank file\n"
						+ "Select no to quit",
						"Replace existing file?",
					    JOptionPane.YES_NO_OPTION, 
					    JOptionPane.ERROR_MESSAGE,
					    null,
					    options,
					    options[1]);
							
				if (n == JOptionPane.NO_OPTION) {
					return;
				}
			}
			Log.println("Creating MASTER File: " + file.getAbsolutePath() );
			
		}
	}

	/**
	 * Allow the user to choose a new spacecraft file to load
	 */
	private void addSpacecraft() {
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
						Spacecraft satellite = new Spacecraft(Config.satManager, file, targetFile);
						//JLabel title = new JLabel(satellite.getIdString());
						
						satellite.save();
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
		if (e.getSource() == mntmNewSpacecraftFile) {
			newSpacecraft();
		}
		if (e.getSource() == mntmAddSpacecraftFile) {
			addSpacecraft();
		}

	}
	
	public void saveProperties() {
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorWindow", "windowHeight", this.getHeight());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorWindow", "windowWidth", this.getWidth());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorWindow", "windowX", this.getX());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorWindow",  "windowY", this.getY());
		Config.save();
	}
	
	public void loadProperties() {
		int windowX = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorWindow", "windowX");
		int windowY = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorWindow", "windowY");
		int windowWidth = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorWindow", "windowWidth");
		int windowHeight = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorWindow", "windowHeight");
		if (windowX == 0 || windowY == 0 ||windowWidth == 0 ||windowHeight == 0) {
			setBounds(100, 100, 1000, 600);
		} else {
			setBounds(windowX, windowY, windowWidth, windowHeight);
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowClosed(WindowEvent arg0) {
		// This is once dispose has run
		Log.close();
		
		System.exit(0);
		Log.println("Window Closed");
	}
	@Override
	public void windowClosing(WindowEvent arg0) {
		// close has been requested from the X or otherwise
		Log.println("Closing Window");
		saveProperties();
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
