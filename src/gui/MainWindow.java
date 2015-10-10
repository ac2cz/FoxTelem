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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import common.Config;
import common.DesktopApi;
import common.Log;
import common.PassManager;
import common.Spacecraft;

import javax.swing.border.EmptyBorder;
import javax.swing.JCheckBoxMenuItem;

import telemetry.Frame;
import telemetry.FramePart;
import telemetry.HighSpeedFrame;
import telemetry.HighSpeedHeader;
import telemetry.LayoutLoadException;
import telemetry.PayloadCameraData;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRadExpData;
import telemetry.PayloadRtValues;
import telemetry.SlowSpeedFrame;
import telemetry.SlowSpeedHeader;
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

	static // We have one health thread per health tab
	Thread[] healthThread;
	// We have one radiation thread and camera thread per Radiation Experiment/Camera tab
	static Thread[] radiationThread;
	static Thread[] cameraThread;
	// We have one FTP Thread for the whole application
//	Thread ftpThread;
//	FtpLogs ftpLogs;
	
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
	static JMenuItem mntmLoadIQWavFile;
	static JMenuItem mntmStartDecoder;
	static JMenuItem mntmStopDecoder;
	JMenuItem mntmSettings;
	static JMenuItem mntmDelete;
	JMenuItem mntmManual;
	JMenuItem mntmAbout;
	JCheckBoxMenuItem chckbxmntmShowFilterOptions;
	JCheckBoxMenuItem chckbxmntmShowDecoderOptions;
	
	// GUI components
	static JTabbedPane tabbedPane;
	public static SourceTab inputTab;
	static MyMeasurementsTab measurementsTab;
	static Thread measurementThread;
	// We have a radiation tab and a health tab per satellite
	static ModuleTab[] radiationTab;
	static HealthTab[] healthTab;
	static CameraTab[] cameraTab;
	
	JLabel lblVersion;
	static JLabel lblLogFileDir;
	static JLabel lblAudioMissed;
	static JLabel lblTotalDecodes;
	static JLabel lblTotalQueued;
	private static String TOTAL_DECODES = "Decoded: ";
	private static String TOTAL_QUEUED = "Queued: ";
	private static String AUDIO_MISSED = "Audio missed: ";
		
	private static int totalMissed;
	
	/**
	 * Create the application.
	 */
	public MainWindow() {

//		ftpLogs = new FtpLogs();
//		if (ftpThread != null) { ftpLogs.stopProcessing(); }		
//		ftpThread = new Thread(ftpLogs);
//		ftpThread.start();

		//decoder = d;
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

		lblTotalDecodes = new JLabel(TOTAL_DECODES);
		lblTotalDecodes.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblTotalDecodes.setBorder(new EmptyBorder(2, 2, 2, 10) ); // top left bottom right
		lblTotalDecodes.setToolTipText("Total number of payloads decoded from all satellites");
		rightBottom.add(lblTotalDecodes );
		
		lblTotalQueued = new JLabel(TOTAL_QUEUED);
		lblTotalQueued.setFont(new Font("SansSerif", Font.BOLD, 10));
		lblTotalQueued.setBorder(new EmptyBorder(2, 2, 2, 10) ); // top left bottom right
		lblTotalQueued.setToolTipText("The number of frames that need to be sent to the Amsat telemetry server");
		rightBottom.add(lblTotalQueued );
		bottomPanel.add(rightBottom, BorderLayout.EAST);
		
		addHealthTabs();
		
		addMeasurementsTab();
		initMenu();
		
		initialize();
		//pack(); // pack all in as tight as possible

		
	}

	public static void enableSourceSelection(boolean t) {
		mntmLoadWavFile.setEnabled(t);
		mntmImportStp.setEnabled(t);
		//mntmLoadIQWavFile.setEnabled(t);
		mntmDelete.setEnabled(t);
	}
	
	public static void showGraphs() {
		for (HealthTab tab : healthTab) {
			tab.showGraphs();
		}
		for (ModuleTab tab : radiationTab) {
			tab.showGraphs();
		}
		measurementsTab.showGraphs();
		frame.toFront();
	}
	
	public static void refreshTabs(boolean closeGraphs) {
		for (HealthTab tab : healthTab) {
			if (closeGraphs) tab.closeGraphs();
			tabbedPane.remove(tab);
		}
		if (closeGraphs) measurementsTab.closeGraphs();
		tabbedPane.remove(measurementsTab);
		

		for (ModuleTab tab : radiationTab) {
			if (closeGraphs) tab.closeGraphs();
			tabbedPane.remove(tab);
		}
		for (CameraTab tab : cameraTab)
			tabbedPane.remove(tab);

		addHealthTabs();
		addMeasurementsTab();
		Config.payloadStore.setUpdatedAll();

		if (Config.logFileDirectory.equals(""))
			lblLogFileDir.setText("Logs: Current Directory");
		else
			lblLogFileDir.setText("Logs: " + Config.logFileDirectory);
		
		inputTab.audioGraph.updateFont();
		inputTab.eyePanel.updateFont();
	}
	
	private static void addHealthTabs() {
		stopThreads(healthTab);
		stopThreads(radiationTab);
		stopThreads(cameraTab);

		ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
		healthTab = new HealthTab[sats.size()];
		healthThread = new Thread[sats.size()];
		// FIXME - this is inefficient.  We do not need to reserve space for all these tabs.  Not all sats have a camera
		radiationTab = new ModuleTab[sats.size()];
		radiationThread = new Thread[sats.size()];
		cameraTab = new CameraTab[sats.size()];
		cameraThread = new Thread[sats.size()];
		for (int s=0; s<sats.size(); s++) {
			healthTab[s] = new HealthTab(sats.get(s));
			healthThread[s] = new Thread(healthTab[s]);
			healthThread[s].setUncaughtExceptionHandler(Log.uncaughtExHandler);
			healthThread[s].start();
			
			tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
//			tabbedPane.addTab( ""  
			+ sats.get(s).toString() + "</b></body></html>", healthTab[s] );
//			+" Health", healthTab );
			
			for (int exp : sats.get(s).experiments) {
				if (exp == Spacecraft.EXP_VULCAN)
					addExperimentTab(sats.get(s), s);
				if (exp == Spacecraft.EXP_VT_CAMERA)
					addCameraTab(sats.get(s), s);
				if (exp == Spacecraft.EXP_IOWA_HERCI)
					addHerciTab(sats.get(s), s);
			}
			
		}
	}

	private static void addMeasurementsTab() {
		if (measurementsTab != null) {
			measurementsTab.stopProcessing();
			while (!measurementsTab.isDone())
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace(Log.getWriter());
				}
		}
		measurementsTab = new MyMeasurementsTab();
		measurementsTab.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab( "<html><body leftmargin=5 topmargin=8 marginwidth=5 marginheight=5>Measurements</body></html>", measurementsTab );
		measurementThread = new Thread(measurementsTab);
		measurementThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		measurementThread.start();
		
	}
	
	private static void stopThreads(FoxTelemTab[] tabs) {
		if (tabs != null)
			for (FoxTelemTab thread : tabs)
				if (thread != null) { 
					thread.stopProcessing(); 

					while (!thread.isDone())
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							e.printStackTrace(Log.getWriter());
						}

				}

	}
	private static void addExperimentTab(Spacecraft fox, int num) {
		
		radiationTab[num] = new VulcanTab(fox);
		radiationThread[num] = new Thread((VulcanTab)radiationTab[num]);
		radiationThread[num].setUncaughtExceptionHandler(Log.uncaughtExHandler);
		radiationThread[num].start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" VU Rad ("+ fox.getIdString() + ")</body></html>", radiationTab[num] );

	}

	private static void addHerciTab(Spacecraft fox, int num) {
		
		radiationTab[num] = new HerciTab(fox);
		radiationThread[num] = new Thread((HerciTab)radiationTab[num]);
//		radiationTab[num] = new RadiationTab(fox);
//		radiationThread[num] = new Thread((RadiationTab)radiationTab[num]);
			
		radiationThread[num].setUncaughtExceptionHandler(Log.uncaughtExHandler);
		radiationThread[num].start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI ("+ fox.getIdString() + ")</body></html>", radiationTab[num] );

	}
	
	private static void addCameraTab(Spacecraft fox, int num) {

		cameraTab[num] = new CameraTab(fox);
		cameraThread[num] = new Thread(cameraTab[num]);
		cameraThread[num].setUncaughtExceptionHandler(Log.uncaughtExHandler);
		cameraThread[num].start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" Camera ("+ fox.getIdString() + ")</body></html>", cameraTab[num] );
//		tabbedPane.addTab( ""+ fox.toString()+
//				" Experiment", radiationTab );

	}

	public static void setAudioMissed(int missed) {
		double miss = missed / 10.0;
		totalMissed += missed;
		lblAudioMissed.setText(AUDIO_MISSED + GraphPanel.roundToSignificantFigures(miss,2) + "% / " + totalMissed);
		if (missed > 2)
			lblAudioMissed.setForeground(Color.RED);
		else
			lblAudioMissed.setForeground(Color.BLACK);
	}
	
	public static void setTotalDecodes() {
		int total = 0;
		total = Config.payloadStore.getTotalNumberOfFrames();
		lblTotalDecodes.setText(TOTAL_DECODES + total);
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
		JMenuBar menuBar = new JMenuBar();
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

		}
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
		
		JMenu mnOptions = new JMenu("Options");
//		menuBar.add(mnOptions);
		
		chckbxmntmShowFilterOptions = new JCheckBoxMenuItem("Show Filter Options");
		mnOptions.add(chckbxmntmShowFilterOptions);
		chckbxmntmShowFilterOptions.addActionListener(this);
		
		chckbxmntmShowDecoderOptions = new JCheckBoxMenuItem("Show Decoder Options");
		chckbxmntmShowDecoderOptions.addActionListener(this);
		mnOptions.add(chckbxmntmShowDecoderOptions);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		mntmManual = new JMenuItem("Open Manual");
		mnHelp.add(mntmManual);
		mntmManual.addActionListener(this);
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

	public void shutdownWindow() {
		
		if (Config.passManager.getState() == PassManager.FADED) {
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
				    JOptionPane.ERROR_MESSAGE,
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
		this.dispose();
		saveProperties();
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
		if (e.getSource() == mntmImportStp) {
			importStp();
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

				Config.payloadStore.deleteAll();
				Config.rawFrameQueue.delete();
				refreshTabs(true);
			}
		}
		
		
		if (e.getSource() == chckbxmntmShowFilterOptions) {	
				inputTab.showFilters(chckbxmntmShowFilterOptions.getState());
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
		if (e.getSource() == mntmAbout) {
			HelpAbout help = new HelpAbout(this, true);
			help.setVisible(true);
		}
	}

	/**
	 * Get a list of all the files in the STP dir and import them
	 */
	private void importStp() {
		String dir = "stp";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			dir = Config.logFileDirectory + File.separator + dir;
			
		}
		Log.println("IMPORT STP from " + dir);
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		int duvFrames = 0;
		int hsFrames = 0;
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() ) {
					//Log.println("Loading STP data from: " + listOfFiles[i].getName());
					
					try {
						Frame decodedFrame = Frame.loadStp(listOfFiles[i].getName());
						if (decodedFrame != null && !decodedFrame.corrupt)
						if (decodedFrame instanceof SlowSpeedFrame) {
							SlowSpeedFrame ssf = (SlowSpeedFrame)decodedFrame;
							FramePart payload = ssf.getPayload();
							SlowSpeedHeader header = ssf.getHeader();
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
							duvFrames++;
						} else {
							HighSpeedFrame hsf = (HighSpeedFrame)decodedFrame;
							HighSpeedHeader header = hsf.getHeader();
							PayloadRtValues payload = hsf.getRtPayload();
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
							PayloadMaxValues maxPayload = hsf.getMaxPayload();
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), maxPayload);
							PayloadMinValues minPayload = hsf.getMinPayload();
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), minPayload);
							PayloadRadExpData[] radPayloads = hsf.getRadPayloads();
							Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), radPayloads);
							if (Config.satManager.hasCamera(header.getFoxId())) {
								PayloadCameraData cameraData = hsf.getCameraPayload();
								Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), cameraData);
							}
							hsFrames++;
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace(Log.getWriter());
					}
				}
			}
			Log.println("Files Processed: " + listOfFiles.length);
			Log.println("DUV Frames: " + duvFrames);
			Log.println("HS Frames: " + hsFrames);
		}
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

		for (HealthTab tab : healthTab)
			tab.closeGraphs();
		for (ModuleTab tab : radiationTab)
			tab.closeGraphs();
		measurementsTab.closeGraphs();
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
