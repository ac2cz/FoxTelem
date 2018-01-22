package gui;

import javax.swing.JFrame;

import decoder.SourceSoundCardAudio;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import common.Config;
import common.DesktopApi;
import common.Log;
import common.PassManager;
import common.Spacecraft;
import common.FoxSpacecraft;
import common.UpdateManager;

import javax.swing.border.EmptyBorder;
import javax.swing.JCheckBoxMenuItem;

import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import macos.MacAboutHandler;
import macos.MacPreferencesHandler;
import macos.MacQuitHandler;

import com.apple.eawt.Application;

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
 * The Window and main JFrame of the application.  The GUI starts here.
 */
@SuppressWarnings("serial")
public class MainWindow extends JFrame implements ActionListener, ItemListener, WindowListener, WindowStateListener {
	
	static Application macApplication;
	
	static UpdateManager updateManager;
	Thread updateManagerThread;
	
	// This is the JFrame for the main window.  We make it static so that we can reach it from other parts of the program.
	// We only need one for the whole application
	public static JFrame frame;
		
	// Variables
//	Decoder decoder;
	SourceSoundCardAudio audioSource;
	
	//Menu Buttons
	JMenuItem mntmExit;
	static JMenuItem mntmLoadWavFile;
	static JMenuItem mntmImportStp;
	static JMenuItem mntmGetServerData;
	static JMenuItem mntmLoadIQWavFile;
	static JMenuItem mntmStartDecoder;
	static JMenuItem mntmStopDecoder;
	static JMenuItem[] mntmSat;
	static ArrayList<Spacecraft> sats;
	JMenu mnSats;
	JMenuBar menuBar;
	JMenuItem mntmSettings;
	static JMenuItem mntmDelete;
	JMenuItem mntmManual;
	JMenuItem mntmLeaderboard;
	JMenuItem mntmSoftware;
	JMenuItem mntmAbout;
	JCheckBoxMenuItem chckbxmntmShowFilterOptions;
	JCheckBoxMenuItem chckbxmntmShowDecoderOptions;
	
	// GUI components
	static JTabbedPane tabbedPane;
	public static SourceTab inputTab;
	Thread inputTabThread;
	// We have a radiation tab and a health tab per satellite
	static SpacecraftTab[] spacecraftTab;
	
	JLabel lblVersion;
	static JLabel lblLogFileDir;
	static JLabel lblAudioMissed;
	static JLabel lblTotalFrames;
	static JLabel lblTotalDecodes;
	static JLabel lblTotalQueued;
	private static String TOTAL_RECEIVED_FRAMES = "Frames: ";
	private static String TOTAL_DECODES = "Payloads: ";
	private static String TOTAL_QUEUED = "Queued: ";
	private static String AUDIO_MISSED = "Audio missed: ";
		
	private static int totalMissed;
	ProgressPanel importProgress;
	
	/**
	 * Create the application.
	 */
	public MainWindow() {

		frame = this; // a handle for error dialogues
		frame.addWindowFocusListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		        //frame.requestFocusInWindow();
		        //System.err.println("FOCUS!");
		    	// THIS DOES NOT QUITE WORK. We get an oscillation.  And the event fires every window interaction		    	
		        //showGraphs();
		    }
		});
	
		if (Config.isMacOs()) {
			macApplication = com.apple.eawt.Application.getApplication();
			macApplication.setAboutHandler(new MacAboutHandler());
			macApplication.setPreferencesHandler(new MacPreferencesHandler());
			macApplication.setQuitHandler(new MacQuitHandler(this));
		}
		
		initMenu();
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		getContentPane().add(tabbedPane);

		inputTab = new SourceTab(this);
		//JPanel inputTab = new JPanel();
		inputTab.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab( "<html><body leftmargin=15 topmargin=8 marginwidth=15 marginheight=5>Input</body></html>", inputTab );

		JPanel bottomPanel = new JPanel();
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BorderLayout ());
		JPanel rightBottom = new JPanel();
		rightBottom.setLayout(new BoxLayout(rightBottom, BoxLayout.X_AXIS));
		
		lblVersion = new JLabel("Version " + Config.VERSION);
		lblVersion.setFont(new Font("SansSerif", Font.BOLD, 10));
	//	lblVersion.setMinimumSize(new Dimension(1600, 14)); // forces the next label to the right side of the screen
	//	lblVersion.setMaximumSize(new Dimension(1600, 14));
		lblVersion.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right

		bottomPanel.add(lblVersion, BorderLayout.WEST);
		
		if (Config.logFileDirectory.equals(""))
			lblLogFileDir = new JLabel("Logs: Current Directory");
		else
			lblLogFileDir = new JLabel("Logs: " + Config.logFileDirectory);
		lblLogFileDir.setFont(new Font("SansSerif", Font.BOLD, 10));
		//lblLogFileDir.setMinimumSize(new Dimension(1600, 14)); // forces the next label to the right side of the screen
		//lblLogFileDir.setMaximumSize(new Dimension(1600, 14));
		lblLogFileDir.setBorder(new EmptyBorder(2, 10, 2, 10) ); // top left bottom right
		bottomPanel.add(lblLogFileDir, BorderLayout.CENTER );

		lblAudioMissed = new JLabel(AUDIO_MISSED);
		lblAudioMissed.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblAudioMissed.setBorder(new EmptyBorder(2, 2, 2, 10) ); // top left bottom right
		lblAudioMissed.setToolTipText("The number of audio buffers missed");
		rightBottom.add(lblAudioMissed );

		lblTotalFrames = new JLabel(TOTAL_RECEIVED_FRAMES);
		lblTotalFrames.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblTotalFrames.setBorder(new EmptyBorder(2, 2, 2, 10) ); // top left bottom right
		lblTotalFrames.setToolTipText("Total number of frames received since FoxTelem restart (including duplicates)");
		rightBottom.add(lblTotalFrames );
		
		lblTotalDecodes = new JLabel(TOTAL_DECODES);
		lblTotalDecodes.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblTotalDecodes.setBorder(new EmptyBorder(2, 2, 2, 10) ); // top left bottom right
		lblTotalDecodes.setToolTipText("Total number of unique payloads decoded from all satellites");
		rightBottom.add(lblTotalDecodes );
		
		lblTotalQueued = new JLabel(TOTAL_QUEUED);
		lblTotalQueued.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblTotalQueued.setBorder(new EmptyBorder(2, 2, 2, 10) ); // top left bottom right
		lblTotalQueued.setToolTipText("The number of frames that need to be sent to the Amsat telemetry server");
		rightBottom.add(lblTotalQueued );
		bottomPanel.add(rightBottom, BorderLayout.EAST);
		
		addHealthTabs();
		
		initialize();
		//pack(); // pack all in as tight as possible

		// Once the main window is up we check the version info
		updateManager = new UpdateManager();
		updateManagerThread = new Thread(updateManager);
		updateManagerThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		updateManagerThread.start();
		
		inputTabThread = new Thread(inputTab);
		inputTabThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		inputTabThread.start();
		
		// We are fully up, remove the database loading message
		Config.fileProgress.updateProgress(100);
	}

	public static void enableSourceSelection(boolean t) {
		mntmLoadWavFile.setEnabled(t);
		mntmImportStp.setEnabled(t);
		//mntmLoadIQWavFile.setEnabled(t);
		mntmDelete.setEnabled(t);
		mntmGetServerData.setEnabled(t);
	}
	
	public static void showGraphs() {
		for (SpacecraftTab tab : spacecraftTab) {
			tab.showGraphs();
		}
		frame.toFront();
	}
	
	public static void refreshTabs(boolean closeGraphs) {
		ProgressPanel fileProgress = new ProgressPanel(Config.mainWindow, "Refreshing tabs, please wait ...", false);
		fileProgress.setVisible(true);

		// Close tabs according to the old list
		removeTabs();
		Config.payloadStore.setUpdatedAll(); // mark everything as new so that we display the results on the new tabs
		addHealthTabs(); // add them back again, could be a new list of sats this time
		
		if (Config.logFileDirectory.equals(""))
			lblLogFileDir.setText("Logs: Current Directory");
		else
			lblLogFileDir.setText("Logs: " + Config.logFileDirectory);
		
		inputTab.audioGraph.updateFont();
		inputTab.eyePanel.updateFont();
		setTotalDecodes();
		
		fileProgress.updateProgress(100);
	}
	
	public static void removeTabs() {
		if (spacecraftTab != null) {
			for (int s=0; s<spacecraftTab.length; s++) {
				spacecraftTab[s].stop(); // stop any running threads
				tabbedPane.remove(spacecraftTab[s]);
				spacecraftTab[s] = null;
			}
		}
		spacecraftTab = null;
	}
	public static void addHealthTabs() {
		sats = Config.satManager.getSpacecraftList();
		spacecraftTab = new SpacecraftTab[sats.size()];
		for (int s=0; s<sats.size(); s++) {
			spacecraftTab[s] = new SpacecraftTab(sats.get(s));

				tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
						//			tabbedPane.addTab( ""  
						+ sats.get(s).toString() + "</b></body></html>", spacecraftTab[s] );
				//			+" Health", healthTab );
		}
	}

	public static void setAudioMissed(int missed) {
		if (lblAudioMissed != null) { // just in case we are delayed starting up
			double miss = missed / 10.0;
			totalMissed += missed;
			lblAudioMissed.setText(AUDIO_MISSED + GraphPanel.roundToSignificantFigures(miss,2) + "% / " + totalMissed);
			if (missed > 2)
				lblAudioMissed.setForeground(Color.RED);
			else
				lblAudioMissed.setForeground(Color.BLACK);
		}
	}

	public static void setTotalDecodes() {
		if (lblTotalDecodes != null) { // make sure we have initialized before we try to update from another thread
			int total = 0;
			total = Config.payloadStore.getTotalNumberOfFrames();
			lblTotalDecodes.setText(TOTAL_DECODES + total);
		}
		if (lblTotalFrames != null) { 
			lblTotalFrames.setText(TOTAL_RECEIVED_FRAMES + Config.totalFrames);
		}
	}

	public static void setTotalQueued(int total) {
		if (lblTotalQueued !=null) // make sure we have initialized before we try to update from another thread
			lblTotalQueued.setText(TOTAL_QUEUED + total);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setBounds(Config.windowX, Config.windowY, Config.windowWidth, Config.windowHeight);
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle("Fox 1 Telemetry Analysis Tool ");
		addWindowListener(this);
		addWindowStateListener(this);

	}
	
	private void initMenu() {
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		mntmDelete = new JMenuItem("Delete Payload Files");
		mnFile.add(mntmDelete);
		mntmDelete.addActionListener(this);

		mnFile.addSeparator();
		
		mntmLoadWavFile = new JMenuItem("Load Wav File");
		mnFile.add(mntmLoadWavFile);
		mntmLoadWavFile.addActionListener(this);

		String dir = "stp";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator + dir;
			
		}
	
		mntmImportStp = new JMenuItem("Import STP");
		mnFile.add(mntmImportStp);
		
		File aFile = new File(dir);
		if(aFile.isDirectory()){
			mntmImportStp.setVisible(true);
			mntmImportStp.addActionListener(this);
		} else {
			mntmImportStp.setVisible(false);
			//mntmGetServerData.setVisible(false);

		}

		mntmGetServerData = new JMenuItem("Fetch Server Data");
		mnFile.add(mntmGetServerData);
		mntmGetServerData.addActionListener(this);

//		mntmLoadIQWavFile = new JMenuItem("Load IQ Wav File");
//		mnFile.add(mntmLoadIQWavFile);
//		mntmLoadIQWavFile.addActionListener(this);

		mnFile.addSeparator();
		
		if (!Config.isMacOs()) {
			mntmSettings = new JMenuItem("Settings");
			mnFile.add(mntmSettings);
			mntmSettings.addActionListener(this);

			mnFile.addSeparator();
		}
		mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		mntmExit.addActionListener(this);

		
		JMenu mnDecoder = new JMenu("Decoder");
		menuBar.add(mnDecoder);
		
		mntmStartDecoder = new JMenuItem("Start/Stop");
		mnDecoder.add(mntmStartDecoder);
		mntmStartDecoder.addActionListener(this);

//		mntmStopDecoder = new JMenuItem("Stop");
//		mnDecoder.add(mntmStopDecoder);
//		mntmStopDecoder.addActionListener(this);
		
		initSatMenu();
		
		JMenu mnOptions = new JMenu("Options");
		//menuBar.add(mnOptions);
		
		chckbxmntmShowFilterOptions = new JCheckBoxMenuItem("Show Filter Options");
		mnDecoder.add(chckbxmntmShowFilterOptions);
		chckbxmntmShowFilterOptions.setState(Config.showFilters);
		chckbxmntmShowFilterOptions.addActionListener(this);
		
		chckbxmntmShowDecoderOptions = new JCheckBoxMenuItem("Show Audio Options");
		chckbxmntmShowDecoderOptions.addActionListener(this);
		mnOptions.add(chckbxmntmShowDecoderOptions);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		mntmManual = new JMenuItem("Open Manual");
		mnHelp.add(mntmManual);
		mntmManual.addActionListener(this);
		mntmLeaderboard = new JMenuItem("View Fox Server Leaderboard");
		mnHelp.add(mntmLeaderboard);
		mntmLeaderboard.addActionListener(this);
		//mntmSoftware = new JMenuItem("Latest Software");
		//mnHelp.add(mntmSoftware);
		//mntmSoftware.addActionListener(this);
		if (!Config.isMacOs()) {
			mntmAbout = new JMenuItem("About FoxTelem");
			mnHelp.add(mntmAbout);
			mntmAbout.addActionListener(this);
		}
	}

	/**
	 * Store a reference to the decoder so that we can shut it down if the user closes the window
	 * @param d
	 
	public void setDecoder(Decoder d) {
		decoder = d;
	}
	*/

	public void setAudioSource(SourceSoundCardAudio a) {
		audioSource = a;
	}

	void initSatMenu() {
		if (sats !=null && mnSats != null) {
			for (int i=0; i<sats.size(); i++) {
				if (mntmSat[i] != null)
				mnSats.remove(mntmSat[i]);
				mntmSat[i] = null;
			}
			menuBar.remove(mnSats);
			
		}
		mnSats = new JMenu("Spacecraft");
		menuBar.add(mnSats);

		sats = Config.satManager.getSpacecraftList();

		mntmSat = new JMenuItem[sats.size()];
		for (int i=0; i<sats.size(); i++) {
			mntmSat[i] = new JMenuItem(sats.get(i).name);
			mnSats.add(mntmSat[i]);
			mntmSat[i].addActionListener(this);
		}
	}
	
	public void shutdownWindow() {
		
		if (Config.passManager.getState() == PassManager.FADED ||
				Config.passManager.getState() == PassManager.DECODE) {
			Object[] options = {"Yes",
	        "No"};
			int n = JOptionPane.showOptionDialog(
					MainWindow.frame,
					"The pass manager is still processing a satellite pass. If the satellite has\n"
					+ "faded it waits 2 minutes in case contact is re-established, even when it is at the\n"
					+ "horizon.  If you exit now the LOS will not be logged and TCA will not be calculated.\n"
					+ "Do you want to exit?",
					"Exit while pass in progress?",
				    JOptionPane.YES_NO_OPTION, 
				    JOptionPane.QUESTION_MESSAGE,
				    null,
				    options,
				    options[1]);
						
			if (n == JOptionPane.NO_OPTION) {
				// don't exit
			} else {
				shutdown();

			}
		} else {
			shutdown();
		}
		
			
	}
	
	private void shutdown() {
		inputTab.shutdown();
		Log.println("Window Closed");
		Log.close();
		saveProperties();
		this.dispose();
		System.exit(0);
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) {

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		shutdownWindow();
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		shutdownWindow();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		//System.out.println("Window Deactivated");
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		// MENU ACTIONS
		if (e.getSource() == mntmExit) {
			
			this.windowClosed(null);
		}
		if (e.getSource() == mntmLoadWavFile) {
			inputTab.chooseFile();
		}
		if (e.getSource() == mntmGetServerData) {
			replaceServerData();
		}
		if (e.getSource() == mntmLoadIQWavFile) {
			inputTab.chooseIQFile();
		}
		if (e.getSource() == mntmStartDecoder) {
			inputTab.btnStartButton.doClick();
		}
		if (e.getSource() == mntmStopDecoder) {
			inputTab.btnStartButton.doClick();
		}

		if (e.getSource() == mntmSettings) {
			SettingsFrame f = new SettingsFrame(this, true);
			f.setVisible(true);
		}
		if (e.getSource() == mntmDelete) {
			Object[] options = {"Yes",
	        "No"};
			int n = JOptionPane.showOptionDialog(
					MainWindow.frame,
					"Are you really sure you want to delete the existing log files?\n"
					+ "All will be removed and any open graphs will be closed.  This will\n"
					+ "also empty the queue of any raw frames pending upload to the server,\n"
					+ "delete any stored measurement and delete any camera images.",
					"Delete Payload Files?",
				    JOptionPane.YES_NO_OPTION, 
				    JOptionPane.ERROR_MESSAGE,
				    null,
				    options,
				    options[1]);
						
			if (n == JOptionPane.YES_OPTION) {
				ProgressPanel fileProgress = new ProgressPanel(Config.mainWindow, "Clearing saved data, please wait ...", false);
				fileProgress.setVisible(true);

				Config.payloadStore.deleteAll();
				Config.rawFrameQueue.delete();
				Config.totalFrames = 0;
				refreshTabs(true);
				fileProgress.updateProgress(100);
			}
		}
		
		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
		
		for (int i=0; i<sats.size(); i++) {
			if (e.getSource() == mntmSat[i]) {
				if (sats.get(i).isFox1()) {
					SpacecraftFrame f = new SpacecraftFrame((FoxSpacecraft) sats.get(i), this, true);
					f.setVisible(true);
				}
			}
		}
		if (e.getSource() == chckbxmntmShowFilterOptions) {	
			Config.showFilters = chckbxmntmShowFilterOptions.getState();
				inputTab.showFilters(Config.showFilters);
		}
		
		if (e.getSource() == chckbxmntmShowDecoderOptions) {	
			//inputTab.showDecoderOptions(chckbxmntmShowDecoderOptions.getState());
	}
		
		
		if (e.getSource() == mntmManual) {
			try {
				DesktopApi.browse(new URI(HelpAbout.MANUAL));
			} catch (URISyntaxException ex) {
				//It looks like there's a problem
				ex.printStackTrace();
			}

		}
		if (e.getSource() == mntmLeaderboard) {
			try {
				DesktopApi.browse(new URI(HelpAbout.LEADERBOARD));
			} catch (URISyntaxException ex) {
				//It looks like there's a problem
				ex.printStackTrace();
			}

		}
		if (e.getSource() == mntmSoftware) {
			try {
				DesktopApi.browse(new URI(HelpAbout.SOFTWARE));
			} catch (URISyntaxException ex) {
				//It looks like there's a problem
				ex.printStackTrace();
			}

		}
		if (e.getSource() == mntmAbout) {
			HelpAbout help = new HelpAbout(this, true);
			help.setVisible(true);
		}
	}

	private void downloadServerData(String dir) {
		String file = "FOXDB.tar.gz";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + "FOXDB.tar.gz";
		}
		// We have the dir, so pull down the file
		ProgressPanel fileProgress = new ProgressPanel(this, "Downloading " + dir + " data, please wait ...", false);
		fileProgress.setVisible(true);

		String urlString = Config.webSiteUrl + "/" + dir + "/FOXDB.tar.gz";
		try {
			URL website = new URL(urlString);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos;
			fos = new FileOutputStream(file);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
		} catch (FileNotFoundException e) {
			// The file was not found on the server.  This is probablly because there was no data for this spacecraft
			
			Log.println("ERROR reading/writing the server data to: " + file + "\n" +
					e.getMessage());
			e.printStackTrace(Log.getWriter());
			fileProgress.updateProgress(100);
			return;
		} catch (MalformedURLException e) {
			Log.errorDialog("ERROR", "ERROR can't access the server data at: " + urlString );
			e.printStackTrace(Log.getWriter());
			fileProgress.updateProgress(100);
			return;
		} catch (IOException e) {
			Log.errorDialog("ERROR", "ERROR reading/writing the server data from server: " + file  + "\n+"
					+ e.getMessage() );
			e.printStackTrace(Log.getWriter());
			fileProgress.updateProgress(100);
			return;
		}

		fileProgress.updateProgress(100);
		File archive = new File(file);

		if (archive.length() == 0) {
			// No data in file, so we skip
		} else {
			ProgressPanel decompressProgress = new ProgressPanel(this, "decompressing " + dir + " data ...", false);
			decompressProgress.setVisible(true);

			// Now decompress it and expand
			File destination = new File(Config.logFileDirectory);

			Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
			try {
				archiver.extract(archive, destination);
			} catch (IOException e) {
				Log.errorDialog("ERROR", "ERROR could not uncompress the server data\n+"
						+ e.getMessage() );
				e.printStackTrace(Log.getWriter());
				decompressProgress.updateProgress(100);
				return;
			} catch (IllegalArgumentException e) {
				Log.errorDialog("ERROR", "ERROR could not uncompress the server data\n+"
						+ e.getMessage() );
				e.printStackTrace(Log.getWriter());
				decompressProgress.updateProgress(100);
				return;
			}

			decompressProgress.updateProgress(100);
		}

	}
	
	private String getFoxServerDir(int id) {
		if (id == 1) return "ao85";
		if (id == 2) return "radfxsat";
		if (id == 3) return "fox1c";
		if (id == 4) return "fox1d";
		if (id == 5) return "fox1e";
		return null;
	}
	
	private void replaceServerData() {

		if (Config.logFileDirectory.equalsIgnoreCase("")) {
			Log.errorDialog("CAN'T EXTRACT SERVER DATA INTO CURRENT DIRECTORY", "You can not replace the log files in the current directory.  "
					+ "Pick another directory from the settings menu\n");
			return;
					
		}
		String message = "Do you want to download server data to REPLACE your existing data?\n"
				+ "THIS WILL OVERWRITE YOUR EXISTING LOG FILES. Switch to a new directory if you have live data received from FOX\n"
				+ "To import into into a different set of log files select NO, then choose a new log file directory from the settings menu";
		Object[] options = {"Yes",
		"No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message,
				"Do you want to continue?",
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[1]);

		if (n == JOptionPane.NO_OPTION) {
			return;
		}

		// Get the server data for each spacecraft we have
		sats = Config.satManager.getSpacecraftList();
		for (Spacecraft sat : sats) {
			// We can not rely on the name of the spacecraft being the same as the directory name on the server
			// because the user can change it.  So we have a hard coded routine to look it up
			String dir = getFoxServerDir(sat.foxId);
			if (dir == null) {
				// no server data for this satellite.  Skip
			} else {
				downloadServerData(dir);
			}
		}

		ProgressPanel refreshProgress = new ProgressPanel(this, "refreshing tabs ...", false);
		refreshProgress.setVisible(true);
		
		Config.save(); // make sure any changed settings saved
		Config.initPayloadStore();
		Config.initSequence();
		Config.initServerQueue();
		refreshTabs(true);
		
		refreshProgress.updateProgress(100);
		
		// We are fully updated, remove the database loading message
		Config.fileProgress.updateProgress(100);
		
	}
	
	/**
	 * Save properties that are not captured realtime.  This is mainly generic properties such as the size of the
	 * window that are not tied to a control that we have added.
	 */
	public void saveProperties() {
		Config.windowHeight = this.getHeight();
		Config.windowWidth = this.getWidth();
		Config.windowX = this.getX();
		Config.windowY = this.getY();

		for (SpacecraftTab tab : spacecraftTab)
			tab.closeGraphs();

		Config.save();
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		
	}	

	@Override
	public void windowStateChanged(WindowEvent e) {
		if (e.getNewState() == WindowEvent.WINDOW_LOST_FOCUS) {
			//e.getWindow().setVisible(false);
			Log.println("LOST FOCUS");
		}
		if (e.getNewState() == WindowEvent.WINDOW_ACTIVATED) {
			//e.getWindow().setVisible(false);
			Log.println("ACTIVATED");
		}
		if (e.getNewState() == WindowEvent.WINDOW_GAINED_FOCUS) {
			//e.getWindow().setVisible(false);
			Log.println("GOT FOCUS");
		}
	}

}
