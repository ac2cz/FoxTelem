package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.usb.UsbException;

import org.usb4java.LibUsbException;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.PassManager;
import common.Spacecraft;
import decoder.Decoder;
import decoder.Fox200bpsDecoder;
import decoder.Fox9600bpsDecoder;
import decoder.SinkAudio;
import decoder.SourceAudio;
import decoder.SourceIQ;
import decoder.SourceSoundCardAudio;
import decoder.SourceUSB;
import decoder.SourceWav;
import decoder.FoxBPSK.FoxBPSKCostasDecoder;
import decoder.FoxBPSK.FoxBPSKDotProdDecoder;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.TunerManager;
import device.fcd.FCDTunerController;
import telemetry.FramePart;

import javax.swing.JProgressBar;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

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
 * This class implements the Input Tab and logic required to stop / start the decoders, display the diagnostic graphs
 * and listen to the audio.
 *
 */
@SuppressWarnings("serial")
public class SourceTab extends JPanel implements Runnable, ItemListener, ActionListener, PropertyChangeListener, FocusListener, MouseListener {
	Thread audioGraphThread;
	Thread eyePanelThread;
	Thread phasorPanelThread;
	
	Thread fftPanelThread;
	Thread decoder1Thread;
	Thread decoder2Thread;
	AudioGraphPanel audioGraph;
	EyePanel eyePanel;
	PhasorPanel phasorPanel;
	public FFTPanel fftPanel;
	JButton btnMonitorAudio;
	JCheckBox rdbtnMonitorFilteredAudio;
	JCheckBox rdbtnViewFilteredAudio;
	JCheckBox rdbtnSquelchAudio;
	JCheckBox rdbtnFilterOutputAudio;
	JCheckBox rdbtnWriteDebugData;
	//JCheckBox rdbtnApplyBlackmanWindow;
	//JCheckBox rdbtnUseLimiter;
	JCheckBox rdbtnShowIF;
	public JCheckBox rdbtnFindSignal;
	JCheckBox rdbtnShowLog;
//	JCheckBox rdbtnShowFFT;
	JCheckBox rdbtnFcdLnaGain;
	JCheckBox rdbtnFcdMixerGain;
	JTextField peakLevel;
	JTextField avgLevel;
	JTextField bitLevel;
	
	JButton btnFftZoomIn;
	JButton btnFftZoomOut;
	
	JCheckBox rdbtnUseNco;
	JCheckBox rdbtnUseCostas;
	JCheckBox cbRetuneCenterFrequency;
	JComboBox<String> speakerComboBox;
	JButton btnStartButton;
	JComboBox<String> soundCardComboBox;
	JLabel lblFileName;
	JLabel lblFile;
	JComboBox<String> cbSoundCardRate;
	JPanel panelFile;
	DevicePanel panelFcd;
	JPanel SDRpanel;
	JRadioButton highSpeed;
	JRadioButton pskDotProd;
	JRadioButton pskCostas;
	JRadioButton lowSpeed;
	JRadioButton auto;
	JRadioButton WFM;
	JRadioButton FM;
	JRadioButton NFM;
	JRadioButton LSB;
	JRadioButton USB;
	JRadioButton CW;
	JRadioButton iqAudio;
	JRadioButton afAudio;
	JRadioButton showSNR;
	JLabel showLabel;
	JRadioButton showLevel;
	JRadioButton viewHighSpeed;
	JRadioButton viewLowSpeed;
	JButton unpause;
	JButton play;
	JPanel findSignalPanel;
	JPanel autoViewpanel;
	Box.Filler audioOptionsFiller;
	JTextArea log;
	JScrollPane logScrollPane;
	JCheckBox autoStart;
	JPanel sourcePanel, audioOutputPanel;
	
	FilterPanel filterPanel;
	
	TunerController rfDevice;
	TunerManager tunerManager;
	
	static final int RATE_96000_IDX = 2;
	static final int RATE_192000_IDX = 3;
	
	private Task task;
	Thread progressThread;
	
	// Swing File Chooser
	JFileChooser fc = null;
	//AWT file chooser for the Mac
	FileDialog fd = null;
	
	// Variables
	public static final String RETUNE_AND_SWITCH_MODE = "Retune center / Switch Modes";
	public static final String SWITCH_MODE = "Auto Switch Modes";
	public static final String FUNCUBE1 = "FUNcube Dongle V1.0";
	public static final String FUNCUBE2 = "FUNcube Dongle V2.0";
//	public static final String FUNCUBE = "XXXXXXX";  // hack to disable the func cube option
	public Decoder decoder1;
	Decoder decoder2;
	public SourceIQ iqSource1;
	SourceIQ iqSource2;
	//SourceAudio audioSource = null; // this is the source of the audio for the decoder.  We select it in the GUI and pass it to the decoder to use
	SinkAudio sink = null;
	private boolean monitorFiltered;
	
	public static boolean STARTED = false;
	//private JPanel leftPanel_1;
	JLabel lblWhenAboveHorizon;
	JLabel lblFreq;
	JLabel lblkHz;
	
	JPanel opts, satPanel, optionsPanel;  // this list of right hand options panel where we put the sats we track
	JLabel satPosition[];
	
	private JTextField txtFreq;
	MainWindow mainWindow;
	private JProgressBar progressBar;
	
	int splitPaneHeight;
	JSplitPane splitPane;
	
	// Management of the devices and soundcards
	ArrayList<String> usbSources;
	String[] soundcardSources;
	String[] allSources;
	
	public SourceTab(MainWindow mw) {
		mainWindow = mw;
		setLayout(new BorderLayout(3, 3));
		
		JPanel bottomPanel = new JPanel();
		buildBottomPanel(this, BorderLayout.CENTER, bottomPanel);
		JPanel optionsRowPanel = new JPanel();
		buildOptionsRow(this, BorderLayout.SOUTH, optionsRowPanel);
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(3, 3));
		add(topPanel, BorderLayout.NORTH);
		
		JPanel rightPanel = new JPanel();	
		buildRightPanel(topPanel, BorderLayout.EAST, rightPanel);


		//if (Config.useNativeFileChooser) {
			fd = new FileDialog(MainWindow.frame, "Select Wav file",FileDialog.LOAD);
			fd.setFile("*.wav");
		//} else {
			fc = new JFileChooser();
			// initialize the file chooser
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "Wav files", "wav", "wave");
			fc.setFileFilter(filter);
		//}
		sourcePanel = new JPanel();
		buildLeftPanel(topPanel,  BorderLayout.CENTER, sourcePanel);

		
		if (soundCardComboBox.getSelectedIndex() != 0) {
			if (Config.startButtonPressed) {
				// Wait for the database to be ready then press the start button
				while (!Config.payloadStore.initialized()) {
					Log.println("Waiting for Payload store before hitting start...");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				processStartButtonClick();
			}
		}

		showFilters(Config.showFilters); // hide the filters because we have calculated the optimal matched filters
		showSourceOptions(Config.showSourceOptions);
		showAudioOptions(Config.showAudioOptions);
	}
	
	public void showFilters(boolean b) { 
		filterPanel.setVisible(b);
//		audioOptionsFiller.setVisible(!b);
	}
	public void showAudioOptions(boolean b) { 
		optionsPanel.setVisible(b);
		audioOutputPanel.setVisible(b);
	}
	public void showSourceOptions(boolean b) { 
		sourcePanel.setVisible(b);
	}

	public void showSatOptions(boolean b) { 
		satPanel.setVisible(b);
	}

	public void showEye(boolean b) { 
		eyePanel.setVisible(b);
	}
	public void showPhasor(boolean b) { 
		if (decoder1 != null && (decoder1 instanceof FoxBPSKDotProdDecoder || decoder1 instanceof FoxBPSKCostasDecoder))
			phasorPanel.setVisible(b);
	}

	public boolean getShowFilterState() {
		return filterPanel.isVisible();
	}
//	public void showDecoderOptions(boolean b) { optionsPanel.setVisible(b); }
	
	public void enableFilters(boolean b) {
		Component[] components = filterPanel.getComponents();
		for (Component c : components) {
			c.setEnabled(b);
		}
	}

	private void buildOptionsRow(JPanel parent, String layout, JPanel optionsPanel) {
		parent.add(optionsPanel, layout);

		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.X_AXIS));
//		optionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel options1 = new JPanel();
		options1.setLayout(new FlowLayout(FlowLayout.LEFT));
		optionsPanel.add(options1);
		
		showLabel = new JLabel ("Show");
		options1.add(showLabel);
		showLevel = addRadioButton("Peak", options1 );
		showSNR = addRadioButton("SNR", options1 );
		ButtonGroup group = new ButtonGroup();
		group.add(showLevel);
		group.add(showSNR);
		if (Config.showSNR) {
			showSNR.setSelected(true);
		} else {
			showLevel.setSelected(true);
		}


		/*
		rdbtnApplyBlackmanWindow = new JCheckBox("Blackman IF (vs Tukey)");
		optionsPanel.add(rdbtnApplyBlackmanWindow);
		rdbtnApplyBlackmanWindow.addItemListener(this);
		rdbtnApplyBlackmanWindow.setSelected(Config.applyBlackmanWindow);
		rdbtnApplyBlackmanWindow.setVisible(false);
		*/ 
		rdbtnShowIF = new JCheckBox("Show IF");
		optionsPanel.add(rdbtnShowIF);
		rdbtnShowIF.addItemListener(this);
		rdbtnShowIF.setSelected(Config.showIF);
		rdbtnShowIF.setVisible(false);
		 
		

		findSignalPanel = new JPanel();
		findSignalPanel.setLayout(new BoxLayout(findSignalPanel, BoxLayout.X_AXIS));
		
		rdbtnFindSignal = new JCheckBox("Find Signal");
		options1.add(rdbtnFindSignal);
		rdbtnFindSignal.addItemListener(this);
		rdbtnFindSignal.setSelected(Config.findSignal);
		rdbtnFindSignal.setVisible(false); // this is not under user control except for debugging

		optionsPanel.add(findSignalPanel);
				
		JLabel when = new JLabel ("|  Find Signal when peak over ");
		findSignalPanel.add(when);
		peakLevel = new JTextField(Double.toString(Config.SCAN_SIGNAL_THRESHOLD));
		peakLevel.setMinimumSize(new Dimension(30,1));
		peakLevel.addActionListener(this);
		peakLevel.addFocusListener(this);
		findSignalPanel.add(peakLevel);
		JLabel rf = new JLabel ("dB, SNR over ");
		findSignalPanel.add(rf);
		avgLevel = new JTextField(Double.toString(Config.ANALYZE_SNR_THRESHOLD));
		avgLevel.setMinimumSize(new Dimension(30,1));
		avgLevel.addActionListener(this);
		avgLevel.addFocusListener(this);
		findSignalPanel.add(avgLevel);
		JLabel bit = new JLabel ("dB and bit SNR over ");
		findSignalPanel.add(bit);
		bitLevel = new JTextField(Double.toString(Config.BIT_SNR_THRESHOLD));
		bitLevel.setMinimumSize(new Dimension(30,1));
		bitLevel.addActionListener(this);
		bitLevel.addFocusListener(this);
		findSignalPanel.add(bitLevel);
		JLabel bitdb = new JLabel ("dB");
		findSignalPanel.add(bitdb);
	
		findSignalPanel.add(new Box.Filler(new Dimension(10,1), new Dimension(1500,1), new Dimension(1500,1)));
		
		findSignalPanel.setVisible(Config.findSignal);
	
		rdbtnUseNco = new JCheckBox("Use NCO");
		rdbtnUseNco.addItemListener(this);
		rdbtnUseNco.setSelected(Config.useNCO);
		rdbtnUseNco.setVisible(false);
		optionsPanel.add(rdbtnUseNco);
//		rdbtnUseCostas = new JCheckBox("Costas");
//		rdbtnUseCostas.addItemListener(this);
//		rdbtnUseCostas.setSelected(Config.useCostas);
//		rdbtnUseCostas.setVisible(false);
//		optionsPanel.add(rdbtnUseCostas);
		

		btnFftZoomIn = new JButton("+");
		btnFftZoomIn.addActionListener(this);
		btnFftZoomOut = new JButton("-");
		btnFftZoomOut.addActionListener(this);
		//optionsPanel.add(btnFftZoomIn);
		//optionsPanel.add(btnFftZoomOut);
	}
	
	private void buildBottomPanel(JPanel parent, String layout, JPanel bottomPanel) {
		//parent.add(bottomPanel, layout);
		////bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.setLayout(new BorderLayout(3, 3));
		bottomPanel.setPreferredSize(new Dimension(800, 250));
		/*
		JPanel audioOpts = new JPanel();
		bottomPanel.add(audioOpts, BorderLayout.NORTH);

		rdbtnShowFFT = new JCheckBox("Show FFT");
		rdbtnShowFFT.addItemListener(this);
		rdbtnShowFFT.setSelected(true);
		audioOpts.add(rdbtnShowFFT);
		*/
		
		audioGraph = new AudioGraphPanel();
		audioGraph.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		bottomPanel.add(audioGraph, BorderLayout.CENTER);
		audioGraph.setBackground(Color.LIGHT_GRAY);
		//audioGraph.setPreferredSize(new Dimension(800, 250));
		
		if (audioGraphThread != null) { audioGraph.stopProcessing(); }		
		audioGraphThread = new Thread(audioGraph);
		audioGraphThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		audioGraphThread.start();

		JPanel eyePhasorPanel = new JPanel();
		eyePhasorPanel.setLayout(new BorderLayout());
		
		eyePanel = new EyePanel();
		eyePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		bottomPanel.add(eyePhasorPanel, BorderLayout.EAST);
		eyePhasorPanel.add(eyePanel, BorderLayout.WEST);
		eyePanel.setBackground(Color.LIGHT_GRAY);
		eyePanel.setPreferredSize(new Dimension(200, 100));
		eyePanel.setMaximumSize(new Dimension(200, 100));
		eyePanel.setVisible(true);
		
		phasorPanel = new PhasorPanel();
		phasorPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		eyePhasorPanel.add(phasorPanel, BorderLayout.EAST);
		phasorPanel.setBackground(Color.LIGHT_GRAY);
		phasorPanel.setPreferredSize(new Dimension(200, 100));
		phasorPanel.setMaximumSize(new Dimension(200, 100));
		phasorPanel.setVisible(false);
		
		fftPanel = new FFTPanel();
		fftPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		fftPanel.setBackground(Color.LIGHT_GRAY);
		
		//bottomPanel.add(fftPanel, BorderLayout.SOUTH);
		showFFT(false);
		fftPanel.setPreferredSize(new Dimension(100, 150));
		fftPanel.setMaximumSize(new Dimension(100, 150));
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				bottomPanel, fftPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true); // repaint as we resize, otherwise we can not see the moved line against the dark background
		if (Config.splitPaneHeight != 0) 
			splitPane.setDividerLocation(Config.splitPaneHeight);
		else
			splitPane.setDividerLocation(200);
		SplitPaneUI spui = splitPane.getUI();
	    if (spui instanceof BasicSplitPaneUI) {
	      // Setting a mouse listener directly on split pane does not work, because no events are being received.
	      ((BasicSplitPaneUI) spui).getDivider().addMouseListener(new MouseAdapter() {
	          public void mouseReleased(MouseEvent e) {
	        	  if (Config.iq == true) {
	        		  splitPaneHeight = splitPane.getDividerLocation();
	        		  //Log.println("SplitPane: " + splitPaneHeight);
	        		  Config.splitPaneHeight = splitPaneHeight;
	        	  }
	          }
	      });
	    }
;
		
		parent.add(splitPane, layout);
		
	}
	
	/**
	 * Build the list of tracked spacecraft.  Return true if at least one spacecraft is tracked
	 * 
	 * @return
	 */
	public boolean buildTrackedSpacecraftList() {
		boolean oneTracked = false;
		if (satPanel != null)
			opts.remove(satPanel);
		satPanel = new JPanel();
		satPanel.setBorder(new TitledBorder(null, "Spacecraft Tracked", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		opts.add(satPanel, BorderLayout.NORTH);
		satPanel.setLayout(new BoxLayout(satPanel, BoxLayout.Y_AXIS));
		JPanel satRows[] = new JPanel[Config.satManager.spacecraftList.size()];
		JLabel satName[] = new JLabel[Config.satManager.spacecraftList.size()];
		satPosition = new JLabel[Config.satManager.spacecraftList.size()];
		for (int s=0; s < Config.satManager.spacecraftList.size(); s++) {
			Spacecraft sat = Config.satManager.spacecraftList.get(s);
			satRows[s] = new JPanel();
			satRows[s].setLayout(new FlowLayout(FlowLayout.LEFT));
			satPanel.add(satRows[s]);
			satName[s] = new JLabel(sat.user_priority + "/"+sat.user_display_name + "   ");
			satPosition[s] = new JLabel("Not Tracked");
			if (sat.user_track) {
				satPosition[s].setText("Tracked");
				oneTracked = true;
			}
			satPosition[s].setToolTipText("Click to Toggle Tracking on/off");
			satPosition[s].addMouseListener(this);
			satRows[s].add(satName[s]);
			satRows[s].add(satPosition[s]);
		}
		autoStart = new JCheckBox("Auto Start");
		autoStart.setSelected(Config.whenAboveHorizon);
		autoStart.addItemListener(this);
		satPanel.add(autoStart);
		showSatOptions(Config.showSatOptions);
		showEye(Config.showEye);
		if (decoder1 != null && (decoder1 instanceof FoxBPSKDotProdDecoder || decoder1 instanceof FoxBPSKCostasDecoder))
			showPhasor(Config.showPhasor);
		return oneTracked;
	}
	
	private void buildRightPanel(JPanel parent, String layout, JPanel rightPanel) {
		parent.add(rightPanel, layout);
			
		opts = new JPanel();
		rightPanel.add(opts);
		opts.setLayout(new BorderLayout());

		
		//buildTrackedSpacecraftList();

		
		optionsPanel = new JPanel();
		optionsPanel.setBorder(new TitledBorder(null, "Audio Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		opts.add(optionsPanel, BorderLayout.CENTER);
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
		
		filterPanel = new FilterPanel();
		opts.add(filterPanel, BorderLayout.SOUTH);
		
		rdbtnViewFilteredAudio = new JCheckBox("View Filtered Audio");
		optionsPanel.add(rdbtnViewFilteredAudio);
		rdbtnViewFilteredAudio.addItemListener(this);
		rdbtnViewFilteredAudio.setSelected(Config.viewFilteredAudio);
		
		rdbtnMonitorFilteredAudio = new JCheckBox("Monitor Filtered Audio");
		optionsPanel.add(rdbtnMonitorFilteredAudio);
		rdbtnMonitorFilteredAudio.addItemListener(this);
		rdbtnMonitorFilteredAudio.setSelected(Config.monitorFilteredAudio);

		rdbtnSquelchAudio = new JCheckBox("Squelch when no telemetry");
		optionsPanel.add(rdbtnSquelchAudio);
		rdbtnSquelchAudio.addItemListener(this);
		rdbtnSquelchAudio.setSelected(Config.squelchAudio);

		rdbtnFilterOutputAudio = new JCheckBox("LPF Monitored Audio");
		optionsPanel.add(rdbtnFilterOutputAudio);
		rdbtnFilterOutputAudio.addItemListener(this);
		rdbtnFilterOutputAudio.setSelected(Config.filterOutputAudio);
		rdbtnFilterOutputAudio.setVisible(false);
		
//		rdbtnShowFFT = new JCheckBox("Show FFT");
//		rdbtnShowFFT.addItemListener(this);
//		rdbtnShowFFT.setSelected(true);
//		optionsPanel.add(rdbtnShowFFT);
//		rdbtnShowFFT.setVisible(false);
		play = new JButton(">");
		play.addActionListener(this);
		optionsPanel.add(play);
		unpause = new JButton("||");
		unpause.addActionListener(this);
		optionsPanel.add(unpause);
		if (Config.debugValues) {
			play.setVisible(true);
			unpause.setVisible(true);
		}
//		audioOptionsFiller = new Box.Filler(new Dimension(10,10), new Dimension(100,80), new Dimension(100,500));
//		optionsPanel.add(audioOptionsFiller);
		
	//	rdbtnUseLimiter = new JCheckBox("Use FM Limiter");
	//	optionsPanel.add(rdbtnUseLimiter);
	//	rdbtnUseLimiter.addItemListener(this);
	//	rdbtnUseLimiter.setSelected(Config.useLimiter);
	//	rdbtnUseLimiter.setVisible(true);


	//	rdbtnWriteDebugData = new JCheckBox("Debug Values");
	//	optionsPanel.add(rdbtnWriteDebugData);
	//	rdbtnWriteDebugData.addItemListener(this);
	//	rdbtnWriteDebugData.setSelected(Config.debugValues);
	//	rdbtnWriteDebugData.setVisible(true);

//		optionsPanel.setVisible(true);

	}
	
	public void log(String text) {
		if (rdbtnShowLog.isSelected()) {
			log.append(text);
			//log.setLineWrap(true);
			log.setCaretPosition(log.getLineCount());
		}
	}
	
	private String[] getSources() {
		soundcardSources = SourceSoundCardAudio.getAudioSources();
		usbSources = new ArrayList<String>();
//		usbSources.add("AirSpy");
		usbSources.add("RTL SDR");

		String[] allSources = new String[soundcardSources.length + usbSources.size()];
		int j = 0;
		for (String s : soundcardSources) allSources[j++] = s;
		if (usbSources != null)
			for (String s : usbSources) allSources[j++] = s;
		return allSources;
	}

	
	private void buildLeftPanel(JPanel parent, String layout, JPanel leftPanel) {
		parent.add(leftPanel, layout);

		leftPanel.setLayout(new BorderLayout(3, 3));
		
		JPanel panel_2 = new JPanel();		
		leftPanel.add(panel_2, BorderLayout.NORTH);
		panel_2.setMinimumSize(new Dimension(10, 35));
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		
		JLabel lblSource = new JLabel("Source");	
		panel_2.add(lblSource);
		lblSource.setAlignmentX(Component.LEFT_ALIGNMENT);
		lblSource.setMinimumSize(new Dimension(120, 14));
		lblSource.setMaximumSize(new Dimension(120, 14));
		
		JLabel fsk = new JLabel("FSK: ");
		panel_2.add(fsk);
		lowSpeed = addRadioButton("DUV", panel_2 );
		highSpeed = addRadioButton("High Speed", panel_2 );
		auto = addRadioButton("DUV + HS", panel_2 );
		JLabel bar = new JLabel("  |  BPSK: ");
		panel_2.add(bar);
		pskCostas = addRadioButton("Costas", panel_2 );
		pskCostas.setToolTipText("Use a Costas Loop to lock onto the signal and decode the BPSK");
		pskDotProd = addRadioButton("DP", panel_2 );
		pskDotProd.setToolTipText("Use a Dot Product decoder which is less sensitive to phase mismatch but more sensitive to noise");
		ButtonGroup group = new ButtonGroup();
		group.add(lowSpeed);
		group.add(highSpeed);
		group.add(auto);
		group.add(pskCostas);
		group.add(pskDotProd);
		
		setupMode();
		
		JPanel centerPanel = new JPanel();		
		leftPanel.add(centerPanel, BorderLayout.CENTER);	
		centerPanel.setLayout( new BorderLayout(3, 3));
		
		JPanel panel_1 = new JPanel();
		centerPanel.add(panel_1, BorderLayout.NORTH);

		btnStartButton = new JButton("Start");
		panel_1.add(btnStartButton);
		btnStartButton.addActionListener(this);
		btnStartButton.setEnabled(false);
		btnStartButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		tunerManager = new TunerManager();

		allSources = getSources();
		soundCardComboBox = new JComboBox<String>(allSources);
		soundCardComboBox.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				//Log.println("Rebuild Sound card List");
				soundCardComboBox.removeAllItems();

				for (String s : allSources) {
					soundCardComboBox.addItem(s);
				}
				//soundCardComboBox.showPopup();
			}
		});
		soundCardComboBox.addActionListener(this);
		panel_1.add(soundCardComboBox);
		
		String[] scRates = {"44100", "48000", "96000", "192000"}; 
		cbSoundCardRate = new JComboBox<String>(scRates);
		cbSoundCardRate.setVisible(false);
		cbSoundCardRate.addActionListener(this);
		panel_1.add(cbSoundCardRate);
		cbSoundCardRate.setSelectedItem(Integer.toString(Config.scSampleRate));
		
		afAudio = addRadioButton("AF", panel_1 );
		iqAudio = addRadioButton("IQ", panel_1 );
		ButtonGroup group2 = new ButtonGroup();
		group2.add(afAudio);
		group2.add(iqAudio);
		
		JPanel panel_c = new JPanel();
		centerPanel.add(panel_c, BorderLayout.CENTER);
		panel_c.setLayout(new BoxLayout(panel_c, BoxLayout.Y_AXIS));
		
		JPanel panelHorizon = new JPanel();
		panel_c.add(panelHorizon);
		lblWhenAboveHorizon = new JLabel("Decoder will start when spacecraft above horizon ");
		lblWhenAboveHorizon.setForeground(Config.AMSAT_RED);
		lblWhenAboveHorizon.setVisible(false);
		panelHorizon.add(lblWhenAboveHorizon);

		JPanel panelSDRtop = new JPanel();
		panel_c.add(panelSDRtop);
		//panelSDR.setLayout(new BoxLayout(panelSDR, BoxLayout.Y_AXIS));
		panelSDRtop.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		
		JPanel panelFreq = new JPanel();
		panelSDRtop.add(panelFreq, BorderLayout.CENTER);
		panelFreq.setLayout(new BoxLayout(panelFreq, BoxLayout.X_AXIS));

		
		lblFreq = new JLabel("Center Frequency ");
		panelFreq.add(lblFreq);
		lblFreq.setVisible(false);
		
		
		txtFreq = new JTextField();
		txtFreq.addActionListener(this);
		txtFreq.addFocusListener(this);
		panelFreq.add(txtFreq);
		txtFreq.setColumns(10);
		txtFreq.setVisible(false);
		txtFreq.setEnabled(!Config.retuneCenterFrequency);

		lblkHz = new JLabel(" kHz   ");
		panelFreq.add(lblkHz);
		lblkHz.setVisible(false);
		
		cbRetuneCenterFrequency = new JCheckBox(RETUNE_AND_SWITCH_MODE);
		panelFreq.add(cbRetuneCenterFrequency);
		cbRetuneCenterFrequency.addItemListener(this);
		cbRetuneCenterFrequency.setToolTipText("Change the center frequency if the spacecraft is outside the band.  Switch modes if needed.");
		cbRetuneCenterFrequency.setSelected(Config.retuneCenterFrequency);
		//cbRetuneCenterFrequency.setVisible(false);
		
		WFM = addRadioButton("WFM", panelFreq );
		FM = addRadioButton("FM", panelFreq );
		NFM = addRadioButton("NFM", panelFreq );
		LSB = addRadioButton("LSB", panelFreq );
		USB = addRadioButton("USB", panelFreq );
		CW = addRadioButton("CW", panelFreq );
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(WFM);
		modeGroup.add(FM);
		modeGroup.add(NFM);
		modeGroup.add(LSB);
		modeGroup.add(USB);
		modeGroup.add(CW);
		WFM.setVisible(false);
		FM.setVisible(false);
		NFM.setVisible(false);
		LSB.setVisible(false);
		USB.setVisible(false);
		CW.setVisible(false);

		LSB.setEnabled(false);
		USB.setEnabled(false);
		CW.setEnabled(false);
		
		if (Config.iq) {
			iqAudio.doClick();  // we want to trigger the action event so the window is setup correctly at startup
		} else {
			afAudio.doClick();
		}

		SDRpanel = new JPanel();
		//	leftPanel.add(panelFile, BorderLayout.SOUTH);	
		panel_c.add(SDRpanel, BorderLayout.SOUTH);
		SDRpanel.setLayout(new BorderLayout());
		SDRpanel.setVisible(false);
		
	//	fcdPanelThread = new Thread(panelFcd);
	//	fcdPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
	//	fcdPanelThread.start();
		
		panelFile = new JPanel();
	//	leftPanel.add(panelFile, BorderLayout.SOUTH);	
		panel_c.add(panelFile, BorderLayout.CENTER);
		panelFile.setLayout(new BoxLayout(panelFile, BoxLayout.Y_AXIS));
		
		lblFile = new JLabel("File: ");
		lblFileName = new JLabel("none");
//		panel_2.add(lblFileName);
		JPanel fileNamePanel = new JPanel();
		fileNamePanel.setLayout(new BoxLayout(fileNamePanel, BoxLayout.X_AXIS));
		panelFile.add(fileNamePanel);	
		fileNamePanel.add(lblFile);	
		fileNamePanel.add(lblFileName);	
		lblFileName.setForeground(Color.GRAY);
		lblFileName.setHorizontalAlignment(SwingConstants.LEFT);
		
		progressBar = new JProgressBar();
		panelFile.add(progressBar);
		progressBar.setValue(0);
        progressBar.setStringPainted(true);
		
		panelFile.setVisible(false);
		
//		log = new JTextArea();
//		log.setRows(20); 
//		log.setEditable(false);
//		logScrollPane = new JScrollPane (log, 
//				   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//		centerPanel.add(logScrollPane);
//		logScrollPane.setVisible(false);
		
//		spacecraftPanel = new SpacecraftPanel();
//		centerPanel.add(spacecraftPanel, BorderLayout.SOUTH);
		
//		JSeparator separator = new JSeparator();
//		centerPanel.add(separator);
		
		audioOutputPanel = new JPanel();
		leftPanel.add(audioOutputPanel, BorderLayout.SOUTH);
		audioOutputPanel.setLayout(new BorderLayout(3, 3));
		//southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

		JPanel panel_3 = new JPanel();
		audioOutputPanel.add(panel_3, BorderLayout.NORTH);
		
		panel_3.setMinimumSize(new Dimension(10, 35));
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.Y_AXIS));
		
//		rdbtnShowLog = new JCheckBox("Show Log");
//		panel_3.add(rdbtnShowLog);
//		rdbtnShowLog.addItemListener(this);
//		rdbtnShowLog.setSelected(Config.showLog);
		
		
		JLabel lblOutput = new JLabel("Output");	
		panel_3.add(lblOutput);
		lblSource.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		//sink = new SinkAudio();
		
		JPanel panelCombo = new JPanel();
		audioOutputPanel.add(panelCombo, BorderLayout.CENTER);
		speakerComboBox = new JComboBox<String>(SinkAudio.getAudioSinks());

		speakerComboBox.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				//Log.println("Rebuild Sink List");
				speakerComboBox.removeAllItems();
				for (String s:SinkAudio.getAudioSinks()) {
					speakerComboBox.addItem(s);
				}
				//speakerComboBox.showPopup();
			}
		});
		speakerComboBox.addActionListener(this);
		
		btnMonitorAudio = new JButton("Monitor Audio");
		if (Config.monitorAudio) {
			btnMonitorAudio.setText("Silence Speaker");
			speakerComboBox.setEnabled(false);
		}
		panelCombo.add(btnMonitorAudio);
		btnMonitorAudio.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnMonitorAudio.addActionListener(this);
		//btnMonitorAudio.setEnabled(false);
		panelCombo.add(speakerComboBox);
		
		if (Config.soundCard != null && !Config.soundCard.equalsIgnoreCase(Config.NO_SOUND_CARD_SELECTED)) {
			soundCardComboBox.setSelectedItem(Config.soundCard);
		}

		if (Config.audioSink != null && !Config.audioSink.equalsIgnoreCase(Config.NO_SOUND_CARD_SELECTED)) {
			speakerComboBox.setSelectedIndex(SinkAudio.getDeviceIdByName(Config.audioSink));
		}
		autoViewpanel = new JPanel();
		autoViewpanel.add(new Box.Filler(new Dimension(10,1), new Dimension(40,1), new Dimension(1500,1)));
		JLabel view = new JLabel("View ");
		panelCombo.add(autoViewpanel);
		autoViewpanel.add(view);
		viewLowSpeed = addRadioButton("DUV", autoViewpanel );
		viewHighSpeed = addRadioButton("HS Audio", autoViewpanel );
		ButtonGroup group3 = new ButtonGroup();
		group3.add(viewLowSpeed);
		group3.add(viewHighSpeed);
		viewLowSpeed.setSelected(true);
//		if (Config.autoDecodeSpeed)
//			autoViewpanel.setVisible(true);
//		else
		autoViewpanel.setVisible(true);
	}

	public void setupMode() {
		if (Config.mode == SourceIQ.MODE_FSK_AUTO) {
			//Config.autoDecodeSpeed = true;
			auto.setSelected(true);
			enableFilters(true);
		} else
		if (Config.mode == SourceIQ.MODE_FSK_HS) {
			highSpeed.setSelected(true);
			enableFilters(false);
		} else if (Config.mode == SourceIQ.MODE_FSK_DUV){
			lowSpeed.setSelected(true);
			enableFilters(true);
		} else if (Config.mode == SourceIQ.MODE_PSK_NC ){
			pskDotProd.setSelected(true);
			enableFilters(true);
		} else if (Config.mode == SourceIQ.MODE_PSK_COSTAS){
			pskCostas.setSelected(true);
			enableFilters(true);
		}

	}
	
	private JRadioButton addRadioButton(String name, JPanel panel) {
		JRadioButton radioButton = new JRadioButton(name);
		radioButton.setEnabled(true);
		radioButton.addActionListener(this);
		panel.add(radioButton);
		return radioButton;
	}
		

	public boolean fileActions() {
		File file = null;
		File dir = null;
		if (Config.windowCurrentDirectory != "") {
			dir = new File(Config.windowCurrentDirectory);
		}
		soundCardComboBox.hidePopup();

		if(Config.useNativeFileChooser) {
			// use the native file dialog on the mac

			if (dir != null) {
				fd.setDirectory(dir.getAbsolutePath());
			}
			fd.setVisible(true);
			String filename = fd.getFile();
			String dirname = fd.getDirectory();
			if (filename == null)
				Log.println("You cancelled the choice");
			else {
				Log.println("File: " + filename);
				Log.println("DIR: " + dirname);
				file = new File(dirname + filename);
			}
		} else {
			fc.setPreferredSize(new Dimension(Config.windowFcWidth, Config.windowFcHeight));
			if (dir != null)
				fc.setCurrentDirectory(dir);
			// This toggles the details view on
			//		Action details = fc.getActionMap().get("viewTypeDetails");
			//		details.actionPerformed(null);

			int returnVal = fc.showOpenDialog(this);
			Config.windowFcHeight = fc.getHeight();
			Config.windowFcWidth = fc.getWidth();		
			//System.out.println("dialog type: " + fc.getDialogType());
			//System.out.println("select mode: " + fc.getFileSelectionMode());
			//System.out.println("select model: " + fc.getFileSelectionMode());
			//System.out.println("ch file filter: "); for ( FileFilter F: fc.getChoosableFileFilters()) System.out.println(F);
			//System.out.println("select model: " + fc.getFileSystemView());


			//Config.save();
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = fc.getSelectedFile();
			}
		}

		if (file != null) {
			Config.windowCurrentDirectory = file.getParent();	
			Config.save();
			lblFileName.setText(file.getName());
			panelFile.setVisible(true);
			btnStartButton.setEnabled(true);
			cbSoundCardRate.setVisible(false);
			return true;
		}
		Config.save();
		return false;
	}
	
	public void chooseFile() {
		soundCardComboBox.setSelectedIndex(SourceAudio.FILE_SOURCE);
	}

	public void chooseIQFile() {
		soundCardComboBox.setSelectedIndex(SourceAudio.FILE_SOURCE);
		iqAudio.setSelected(true);
		//soundCardComboBox.setSelectedIndex(SourceAudio.IQ_FILE_SOURCE);
	}
	
	void showFFT(boolean b) {
		fftPanel.setVisible(b);
		if (b==true) {
			if (Config.splitPaneHeight != 0) 
				splitPane.setDividerLocation(Config.splitPaneHeight);
			else
				splitPane.setDividerLocation(200);
		}
		mainWindow.chckbxmntmShowFFT.setState(b); // this will get the menu toggle right
	}
	
	private void setIQVisible(boolean b) {
		showFFT(b);
//		rdbtnShowFFT.setVisible(b);
		rdbtnFindSignal.setVisible(false);
		rdbtnFindSignal.setEnabled(false);
		findSignalPanel.setVisible(b);
		showSNR.setVisible(b);
		showLevel.setVisible(b);
		showLabel.setVisible(b);
		//		rdbtnApplyBlackmanWindow.setVisible(b);
		setFreqVisible(b);
		if (this.soundCardComboBox.getSelectedIndex() >= soundcardSources.length) { // USB SOunds card
			cbSoundCardRate.setVisible(!b); //// TODO - This is where we should be setting up the right RATE selection pulldown for use while USB Device stopped
		} 
		if (b)
			cbRetuneCenterFrequency.setText(RETUNE_AND_SWITCH_MODE);
		else
			cbRetuneCenterFrequency.setText(SWITCH_MODE);
	}
	
	private void setFreqVisible(boolean b) {
		lblFreq.setVisible(b);
		lblkHz.setVisible(b);
//		cbRetuneCenterFrequency.setVisible(b);
		txtFreq.setVisible(b);
		
		WFM.setVisible(false);
		FM.setVisible(false);
		NFM.setVisible(false);
		LSB.setVisible(false);
		USB.setVisible(false);
		CW.setVisible(false);
	}
	
	public void setViewDecoder1() {
		try {
			if (decoder2 != null)
				decoder2.stopAudioMonitor();
			if (decoder1 != null)
				if (Config.monitorAudio && sink != null)
				decoder1.setMonitorAudio(sink, Config.monitorAudio, speakerComboBox.getSelectedIndex());
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (decoder1 != null) {
			audioGraph.startProcessing(decoder1);
			eyePanel.startProcessing(decoder1);
			if (decoder1 instanceof FoxBPSKDotProdDecoder || decoder1 instanceof FoxBPSKCostasDecoder) {
				phasorPanel.setVisible(Config.showPhasor);
				phasorPanel.startProcessing(decoder1);
			} else {
				phasorPanel.setVisible(false);
			}
			fftPanel.startProcessing(iqSource1);
		}
		viewLowSpeed.setSelected(true);

	}

	public void setViewDecoder2() {
		if (decoder2 != null) {
			try {
				if (decoder1 != null)
					decoder1.stopAudioMonitor();
				if (decoder2 != null)
					if (Config.monitorAudio && sink != null)
					decoder2.setMonitorAudio(sink, Config.monitorAudio, speakerComboBox.getSelectedIndex());
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (LineUnavailableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			audioGraph.startProcessing(decoder2);
			eyePanel.startProcessing(decoder2);
//			if (decoder2 instanceof FoxBPSKDotProdDecoder || decoder2 instanceof FoxBPSKCostasDecoder)
//				if (Config.showPhasor)
//					phasorPanel.startProcessing(decoder2);
			fftPanel.startProcessing(iqSource2);
			
			viewHighSpeed.setSelected(true);
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == unpause) {
			Config.decoderPaused = false;
			Config.decoderPlay = false;
		}
		if (e.getSource() == play) {
			Config.decoderPlay = true;
		}
	
		if (e.getSource() == highSpeed) { 
				Config.mode = SourceIQ.MODE_FSK_HS;
				//Config.autoDecodeSpeed = false;
				enableFilters(false);
				autoViewpanel.setVisible(false);
				if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_FSK_HS);
				Config.save();
		}
		if (e.getSource() == lowSpeed) { 
			Config.mode = SourceIQ.MODE_FSK_DUV;
			//Config.autoDecodeSpeed = false;
			enableFilters(true);
			autoViewpanel.setVisible(false);
			if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_FSK_DUV);
			Config.save();
		}
		if (e.getSource() == pskCostas) { 
				Config.mode = SourceIQ.MODE_PSK_COSTAS;
			enableFilters(false);
			autoViewpanel.setVisible(false);
			if (iqSource1 != null) {
				iqSource1.setMode(Config.mode);
			}
			Config.save();
		}
		if (e.getSource() == pskDotProd) { 
			Config.mode = SourceIQ.MODE_PSK_NC;
			enableFilters(false);
			autoViewpanel.setVisible(false);
			if (iqSource1 != null) {
				iqSource1.setMode(Config.mode);
			}
			Config.save();
	}
		if (e.getSource() == auto) { 
			Config.mode = SourceIQ.MODE_FSK_AUTO;
			enableFilters(true);
			if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_FSK_DUV);
			if (iqSource2 != null) iqSource2.setMode(SourceIQ.MODE_FSK_HS);
			autoViewpanel.setVisible(true);
			Config.save();
		}
		if (e.getSource() == viewHighSpeed) {
			setViewDecoder2();
		}
		if (e.getSource() == viewLowSpeed) {
			setViewDecoder1();
		}

		if (e.getSource() == iqAudio) { 
			Config.iq = true;
			setIQVisible(true);
			Config.save();
		}
		if (e.getSource() == afAudio) { 
			Config.iq = false;
			setIQVisible(false);
			Config.save();
		}
		if (e.getSource() == showSNR) { 
			Config.showSNR = true;
			Config.save();
		}
		if (e.getSource() == showLevel) { 
			Config.showSNR = false;
			Config.save();
		}

		if (e.getSource() == this.peakLevel) {
			try {
			Config.SCAN_SIGNAL_THRESHOLD = Double.parseDouble(peakLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
			Config.save();
		}

		if (e.getSource() == this.avgLevel) {
			try {
			Config.ANALYZE_SNR_THRESHOLD = Double.parseDouble(avgLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
			Config.save();
		}

		if (e.getSource() == this.bitLevel) {
			try {
			Config.BIT_SNR_THRESHOLD = Double.parseDouble(bitLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
			Config.save();
		}

		// Frequency Text Field
		if (e.getSource() == this.txtFreq) {
			// User has done this so trap the error and report
			try {
				double f = setCenterFreq();
				txtFreq.setText(Double.toString(f));				
			} catch (DeviceException e1) {
				Log.errorDialog("ERROR with txtFreq", e1.getMessage());
				e1.printStackTrace(Log.getWriter());
			} catch (IOException e1) {
				e1.printStackTrace(Log.getWriter());
			} catch (LibUsbException e2) {
				e2.printStackTrace(Log.getWriter());
			}
			Config.save();
		}
		
		// SOUND CARD AND FILE COMBO BOX
		if (e.getSource() == soundCardComboBox) {
			processSoundCardComboBox();
		}

		// SPEAKER OUTPUT COMBO BOX
		if (e.getSource() == speakerComboBox) {
			processSpeakerComboBox();
		}

		// USER CHANGES THE SAMPLE RATE
		if (e.getSource() == cbSoundCardRate) {
			// store the value so it is saved if we exit
			Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());
			Config.save();
		}

		// MONITOR AUDIO BUTTON
		if (e.getSource() == btnMonitorAudio) {
			if (!Config.monitorAudio) {// then we are toggling it on, get the sink ready
				if (decoder1 != null)
				setupAudioSink(decoder1);
			} else {// we are toggling it off
				if (sink != null) {
					sink.flush();
					sink.closeOutput();
					sink = null;
				}
			}
			try {
				if (viewLowSpeed.isSelected())
					if (decoder1 != null) { // then we want to toggle the live audio	
						Config.monitorAudio = decoder1.toggleAudioMonitor(sink, monitorFiltered, speakerComboBox.getSelectedIndex());
					} else {
						Config.monitorAudio = !Config.monitorAudio; // otherwise just note that we want to change it for the next decoder run
					}
				else
					if (decoder2 != null) { // then we want to toggle the live audio	
						Config.monitorAudio = decoder2.toggleAudioMonitor(sink, monitorFiltered, speakerComboBox.getSelectedIndex());
					} else {
						Config.monitorAudio = !Config.monitorAudio; // otherwise just note that we want to change it for the next decoder run
					}
			} catch (IllegalArgumentException e1) {
				JOptionPane.showMessageDialog(this,
						"Is there a valid sound card attached?\n"+e1.toString(),
						"CAN'T MONITOR THE AUDIO",
						JOptionPane.ERROR_MESSAGE) ;
				//e1.printStackTrace();	
			} catch (LineUnavailableException e1) {
				JOptionPane.showMessageDialog(this,
						"Is there a valid sound card attached?\n"+e1.toString(),
						"CAN'T MONITOR THE AUDIO",
						JOptionPane.ERROR_MESSAGE) ;
			}

			if (Config.monitorAudio) { 
				btnMonitorAudio.setText("Silence Speaker");
				speakerComboBox.setEnabled(false);
			} else {
				btnMonitorAudio.setText("Monitor Audio");
				speakerComboBox.setEnabled(true);
			}
			Config.save();
		}

		// START BUTTON
		if (e.getSource() == btnStartButton) {
			processStartButtonClick();
		}
		if (e.getSource() == btnFftZoomIn) {
			this.fftPanel.zoomIn();
		}
		if (e.getSource() == btnFftZoomOut) {
			this.fftPanel.zoomOut();
		}
		
	}

	
	private void setupAudioSink(Decoder decoder12) {
		int position = speakerComboBox.getSelectedIndex();
		if (sink != null) {
			sink.flush();
			sink.closeOutput();
			sink = null;
		}
		
		try {
			sink = new SinkAudio(decoder12.getAudioFormat());
			if (sink != null)
				sink.setDevice(position);
		if (position != -1) {
			Config.audioSink = SinkAudio.getDeviceName(position);
			Config.save();
		}

		} catch (LineUnavailableException e1) {
			JOptionPane.showMessageDialog(this,
					e1.toString(),
					"LINE UNAVAILABLE ERROR",
				    JOptionPane.ERROR_MESSAGE) ;
			//e1.printStackTrace();
			
		} catch (IllegalArgumentException e1) {
			JOptionPane.showMessageDialog(this,
					e1.toString(),
					"ARGUMENT ERROR",
				    JOptionPane.ERROR_MESSAGE) ;
			//e1.printStackTrace();	
		}

	}

	private SourceWav setWavFile(boolean IQ) {
		SourceWav audioSource = null;
		//if (audioSource != null)
		//	audioSource.stop();
		try {
			audioSource = new SourceWav(Config.windowCurrentDirectory + File.separator + lblFileName.getText(), IQ);
		} catch (UnsupportedAudioFileException e) {
			Log.errorDialog("ERROR With Audio File", e.toString());
			e.printStackTrace(Log.getWriter());
			stopButton();
		} catch (IOException e) {
			Log.errorDialog("ERROR With File", e.toString());
			e.printStackTrace(Log.getWriter());
			stopButton();
		}
		if (task != null) { task.resetProgress(); }
		return audioSource;
	}
	
	/**
	 * Process the action from the sound card combo box when an item is selected.  The user has not pressed start, but they have selected
	 * the audio source that we are going to use.  We do nothing at this point other than changes the visibility of GUI components
	 */
	private void processSoundCardComboBox() {
		String source = (String)soundCardComboBox.getSelectedItem();
		int position = soundCardComboBox.getSelectedIndex();
		if (source == null) {
			// Do nothing
		} else
		if (position <= 0) {

		} else if (position == SourceAudio.FILE_SOURCE) {
			if (fileActions()) {
//				releaseFcd();
			}
			auto.setEnabled(false);
			if (auto.isSelected()) { 
				lowSpeed.setSelected(true);
				Config.mode = SourceIQ.MODE_FSK_DUV; // so it is saved for next time
				autoViewpanel.setVisible(false);
			}
		} else if (position >= this.soundcardSources.length) { // then this is a USB device IQ
			btnStartButton.setEnabled(true);
			cbSoundCardRate.setVisible(false);
			panelFile.setVisible(false);
			auto.setEnabled(false);

			setIQVisible(true);
			if (auto.isSelected()) { 
				autoViewpanel.setVisible(false);
				lowSpeed.setSelected(true);
				highSpeed.setSelected(false);
				pskCostas.setSelected(false);
				pskDotProd.setSelected(false);
			}
		} else { // its not a file so its a sound card or FCD that was picked
			int fcdSelected = fcdSelected();
			auto.setEnabled(true);
			if (fcdSelected > 0) {

			} else {
				Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());	
			}
			

			btnStartButton.setEnabled(true);
			cbSoundCardRate.setVisible(true);
			panelFile.setVisible(false);
			if (Config.iq) {
				setIQVisible(true);
			} else {
				setIQVisible(false);
			}
		}
		Config.soundCard = soundCardComboBox.getItemAt(soundCardComboBox.getSelectedIndex());

	}

	private void processSpeakerComboBox() {
		int position = speakerComboBox.getSelectedIndex();
		
		if (position != -1) {
			Config.audioSink = SinkAudio.getDeviceName(position);
		}
	}
	
	@SuppressWarnings("unused")
	private void releaseFcd() {
		if (rfDevice != null) { // release the FCD device
			try {
				rfDevice.cleanup();
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());
			} catch (DeviceException e) {
				e.printStackTrace(Log.getWriter());
			}
			rfDevice = null;
		}
	}
	
	private int fcdSelected() {
		
		String source = (String)soundCardComboBox.getSelectedItem();
		Pattern pattern2 = Pattern.compile(FUNCUBE2);
		Matcher matcher2 = pattern2.matcher(source);
		if (matcher2.find()) {
			return 2;
		}

		Pattern pattern1 = Pattern.compile(FUNCUBE1);
		Matcher matcher1 = pattern1.matcher(source);
		if (matcher1.find()) {
			return 1;
		}
		return 0;
	}

	private void connectFCD(short vendorId, short deviceId) throws UsbException, DeviceException {
		if (rfDevice == null) { // this is a hack, you need to exit FoxTelem to switch devices if you have two plugged in.  Otherwise it just opens the previous one. FIXME
			try {
				rfDevice = tunerManager.findDevice(vendorId, deviceId);
			} catch (Exception e1) {
				// FIXME - This can not be right..
				// Sometimes we fail the first time but a retry succeeds.  If this fails we throw the exception
				rfDevice = tunerManager.findDevice(vendorId, deviceId);
			}
		} 
	}

	
	private int usingFcd() throws IOException, DeviceException, UsbException {
		int fcdSelected = fcdSelected();
		if (fcdSelected >0) {
			short vendorId = 0;
			short deviceId = 0;
			if (rfDevice == null) {
				// FCDPP
				if (fcdSelected == 2) {
					vendorId = (short)0x04D8;
					deviceId = (short)0xFB31;
					cbSoundCardRate.setSelectedIndex(RATE_192000_IDX);
				} else { // FCDP
					vendorId = (short)0x04D8;
					deviceId = (short)0xFB56;	
					cbSoundCardRate.setSelectedIndex(RATE_96000_IDX);
				}
				
				connectFCD(vendorId, deviceId);
				
				if (rfDevice == null) {
					this.lblkHz.setText(" kHz   " + " |   FCD ERR - SET FREQ MANUALLY");
					Log.println("ERROR setting FCD device on panel and reading its settings, but carrying on...");
					return 0; // FIXME this is an issue because we found the description but not the HID device
				}
			}
			if (panelFcd == null)
				try {
					panelFcd = rfDevice.getDevicePanel();
					} catch (IOException e) {
						e.printStackTrace(Log.getWriter());
					} catch (DeviceException e) {
						e.printStackTrace(Log.getWriter());
					}
				try {
					panelFcd.setDevice(rfDevice);
				} catch (DeviceException e) {
					this.lblkHz.setText(" kHz   " + " |   FCD DEVICE NOT CONNECTED");
					Log.println("ERROR setting FCD device on panel and reading its settings, but carrying on...");
				}
				SDRpanel.add(panelFcd, BorderLayout.CENTER);

				SDRpanel.setVisible(true);
				if (rfDevice.isConnected()) {
					panelFcd.setEnabled(true);
				} else {
					panelFcd.setEnabled(false);
				}
		
			Config.iq = true;
			iqAudio.setSelected(true);
			setIQVisible(true);
		} else {
			SDRpanel.setVisible(false);
			panelFcd = null;
		}

		return fcdSelected;
	}

	private void setFcdSampleRate() {
		Config.scSampleRate = rfDevice.SAMPLE_RATE;
		if (rfDevice.SAMPLE_RATE == 96000)
			cbSoundCardRate.setSelectedIndex(RATE_96000_IDX);
		else
			cbSoundCardRate.setSelectedIndex(RATE_192000_IDX);
		Config.save();
	}
	
	private SourceSoundCardAudio setupSoundCard(boolean highSpeed, int sampleRate) {
		int position = soundCardComboBox.getSelectedIndex();
		int circularBufferSize = sampleRate * 4;
		if (highSpeed || Config.mode == SourceIQ.MODE_FSK_AUTO) {
			circularBufferSize = sampleRate * 4;
		} else {
		}		SourceSoundCardAudio audioSource = null;
		boolean storeStereo = false;
		if (Config.iq) storeStereo = true;
		try {
			if (Config.mode == SourceIQ.MODE_FSK_AUTO)
				audioSource = new SourceSoundCardAudio(circularBufferSize, sampleRate, position, 2, storeStereo); // split the audio source
			else
				audioSource = new SourceSoundCardAudio(circularBufferSize, sampleRate, position, 0, storeStereo);
		} catch (LineUnavailableException e1) {
			JOptionPane.showMessageDialog(this,
					e1.toString(),
					"LINE UNAVAILABLE ERROR",
				    JOptionPane.ERROR_MESSAGE) ;
			e1.printStackTrace(Log.getWriter());
			stopButton();
		} catch (IllegalArgumentException e1) {
			JOptionPane.showMessageDialog(this,
					e1.toString(),
					"ARGUMENT ERROR",
					JOptionPane.ERROR_MESSAGE) ;
			e1.printStackTrace(Log.getWriter());
			stopButton();
		}
		return audioSource;
	}
	
	
	/**
	 * Create a new Decoder with the setup params
	 * Start a new thread with this decoder and run it.
	 * @param highSpeed
	 */
	private void setupDecoder(boolean highSpeed, SourceAudio audioSource, SourceAudio audioSource2) {
		
		if (Config.mode == SourceIQ.MODE_FSK_AUTO) {
			if (Config.iq) {
				decoder1 = new Fox200bpsDecoder(audioSource, 0);
				decoder2 = new Fox9600bpsDecoder(audioSource2, 0);
			} else {
				decoder1 = new Fox200bpsDecoder(audioSource, 0);
				decoder2 = new Fox9600bpsDecoder(audioSource2, 1);
			}
		} else if (this.pskCostas.isSelected()) {
			if (Config.iq) {
				iqSource1.setMode(SourceIQ.MODE_PSK_COSTAS);
				decoder1 = new FoxBPSKCostasDecoder(audioSource, 0, FoxBPSKCostasDecoder.AUDIO_MODE);
			} else
				decoder1 = new FoxBPSKCostasDecoder(audioSource, 0, FoxBPSKCostasDecoder.PSK_MODE);
		} else if (this.pskDotProd.isSelected()) {
			decoder1 = new FoxBPSKDotProdDecoder(audioSource, 0, FoxBPSKCostasDecoder.AUDIO_MODE);
		} else if (highSpeed) {
			decoder1 = new Fox9600bpsDecoder(audioSource, 0);
		} else {
			decoder1 = new Fox200bpsDecoder(audioSource, 0);
		}
		if (Config.monitorAudio)
			setupAudioSink(decoder1);
	}
	

	/**
	 * The user has clicked the start button.  We already know the audio Source.
	 */
	public void processStartButtonClick() {
		//String source = (String)soundCardComboBox.getSelectedItem();
		int position = soundCardComboBox.getSelectedIndex();
		lblkHz.setText(" kHz   ");
		if (STARTED) {
			// we stop everything
			stopButton();
		} else {
			viewLowSpeed.setSelected(true);
			STARTED = true;
			btnStartButton.setText("Stop");
			stopDecoder(); // make sure everything is stopped
			Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());
			
			if (task != null) {
				Log.println("Stopping file progress task-");
				task.end();
			}

			if (position == 0) {
				// we don't have a selection
			} else {
				if (position == SourceAudio.FILE_SOURCE) { // || position == SourceAudio.IQ_FILE_SOURCE) {
					//Config.useNCO = false;
					SourceWav wav = setWavFile(Config.iq);
					if (wav != null) {
						if (task != null) {
							task.end();
						}
						task = new Task(wav);
						progressThread = new Thread(task);
						progressThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
						if (Config.iq) { //position == SourceAudio.IQ_FILE_SOURCE) {
							if (iqSource1 != null) iqSource1.stop();
							iqSource1 = new SourceIQ((int)wav.getAudioFormat().getSampleRate()*4,0,highSpeed.isSelected());
							iqSource1.setAudioSource(wav,0); // wave file does not work with auto speed
							setupDecoder(highSpeed.isSelected(), iqSource1, iqSource1);
							try {
								double f = setCenterFreq();
								txtFreq.setText(Double.toString(f));
							} catch (DeviceException e) {
								Log.println("ERROR setting the Center Frequency: " + e.getMessage());
								e.printStackTrace(Log.getWriter());
							} catch (IOException e) {
								Log.println("ERROR setting the Center Frequency: " + e.getMessage());
								e.printStackTrace(Log.getWriter());
							}
							Config.passManager.setDecoder1(decoder1, iqSource1, this);
							//if (Config.autoDecodeSpeed)
							//	Config.passManager.setDecoder2(decoder2, iqSource2, this);
						} else {
							setupDecoder(highSpeed.isSelected(), wav, wav);
						}
						Config.soundCard = "";  // We don't want to restart with File selected
						progressThread.start(); // need to start after the audio source wav is created
					} else {
						stopButton();
					}
				} else if (position >= soundcardSources.length) {
					// USB Sound card - this is not pretty and needs to be fixed
					// Ids should be looked up from TunerClass, but the implementation is a mess.  FIXME
					SourceAudio audioSource;
					if (autoViewpanel != null)
						autoViewpanel.setVisible(false);
//					Config.useNCO = true; // always use NCO for a USB device
					short vendorId = 0;
					short deviceId = 0;
//					if (position-soundcardSources.length == 0) { // airspy
//						vendorId = (short)0x1D50;
//						deviceId = (short)0x60A1;
//					} else 
					if (position-soundcardSources.length == 0) { // rtlsdr // this is probablly not all the devices!
						vendorId = (short)0x0BDA;
						deviceId = (short)0x2838;
					} 
					if (rfDevice == null) { // this is a hack, you need to exit FoxTelem to switch devices if you have two plugged in.  Otherwise it just opens the previous one. FIXME
						try {
							rfDevice = tunerManager.findDevice(vendorId, deviceId);
						} catch (UsbException e1) {
							Log.errorDialog("ERROR", "USB Issue trying to open device:\n" + e1.getMessage());
							e1.printStackTrace();
							rfDevice = null;
						} catch (DeviceException e) {
							Log.errorDialog("ERROR", "Device could not be opened:\n" + e.getMessage());
							e.printStackTrace();
							rfDevice = null;
						}
					} 
					if (rfDevice == null) {
						Log.errorDialog("Missing USB device", "Insert the device or choose anther source");
						stopButton();
					} else {
						Log.println("USB Source Selected: " + rfDevice.name);
						panelFcd = null; // get rid of any existing panel
						try {
							panelFcd = rfDevice.getDevicePanel();
						} catch (IOException e) {
							Log.errorDialog("USB Panel Error", e.getMessage());
							e.printStackTrace(Log.getWriter());
							stopButton();
						} catch (DeviceException e) {
							Log.errorDialog("USB Device Error", e.getMessage());
							e.printStackTrace(Log.getWriter());
							stopButton();
						}

						try {
							panelFcd.setDevice(rfDevice);
						} catch (IOException e) {
							Log.errorDialog("USB Panel Error", e.getMessage());
							e.printStackTrace(Log.getWriter());
							stopButton();
						} catch (DeviceException e) {
							Log.errorDialog("USB Device Error", e.getMessage());
							e.printStackTrace(Log.getWriter());
							stopButton();
						}
						SDRpanel.add(panelFcd, BorderLayout.CENTER);
						SDRpanel.setVisible(true);
						panelFcd.setEnabled(false); // this is just the rate change params for the Airspy panel

						Config.iq = true;
						iqAudio.setSelected(true);
						setIQVisible(true);
						int rate = panelFcd.getSampleRate();
						
						int channels = 0;
						if (Config.mode == SourceIQ.MODE_FSK_AUTO)
							channels = 2;
						if (position-soundcardSources.length == 2) { // FCDPP
							setFcdSampleRate();
							Config.soundCard = soundCardComboBox.getItemAt(soundCardComboBox.getSelectedIndex());
							audioSource = setupSoundCard(highSpeed.isSelected(), Config.scSampleRate);
							
						} else {
							audioSource = new SourceUSB("USB Source", rate, rate*2, channels); 
							rfDevice.setUsbSource((SourceUSB)audioSource);
						}
						boolean decoder1HS = highSpeed.isSelected();
						if (Config.mode == SourceIQ.MODE_FSK_AUTO) {
							iqSource2 = new SourceIQ(rate*2, 0,true);
							iqSource2.setAudioSource(audioSource,1); 
							decoder1HS = false;
						}
						iqSource1 = new SourceIQ(rate*2, 0,decoder1HS); 
						iqSource1.setAudioSource(audioSource,0);
						setupDecoder(highSpeed.isSelected(), iqSource1, iqSource1);
						
						Config.passManager.setDecoder1(decoder1, iqSource1, this);
						if (Config.mode == SourceIQ.MODE_FSK_AUTO)
							Config.passManager.setDecoder2(decoder2, iqSource2, this);

						Config.soundCard = soundCardComboBox.getItemAt(soundCardComboBox.getSelectedIndex());
					}
				} 
				else { // soundcard - fcd or normal
					SourceAudio audioSource;
					int fcdSelected = 0;
//					Config.useNCO = false;
					try {
						fcdSelected = usingFcd();
						if (fcdSelected > 0) {
							setFcdSampleRate();
						} else {
							Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());	
						}

						Config.soundCard = soundCardComboBox.getItemAt(soundCardComboBox.getSelectedIndex());
						audioSource = setupSoundCard(highSpeed.isSelected(), Config.scSampleRate);
						if (audioSource != null)
							if (fcdSelected > 0 || Config.iq) {
								Log.println("IQ Source Selected");
								boolean decoder1HS = highSpeed.isSelected();
								if (Config.mode == SourceIQ.MODE_FSK_AUTO) {
									iqSource2 = new SourceIQ(Config.scSampleRate * 4, 0,true);
									iqSource2.setAudioSource(audioSource,1); 
									decoder1HS = false;
								}
								iqSource1 = new SourceIQ(Config.scSampleRate * 4, 0,decoder1HS); 
								iqSource1.setAudioSource(audioSource,0); 
								
								setupDecoder(highSpeed.isSelected(), iqSource1, iqSource2);
								Config.passManager.setDecoder1(decoder1, iqSource1, this);
								if (Config.mode == SourceIQ.MODE_FSK_AUTO)
									Config.passManager.setDecoder2(decoder2, iqSource2, this);
								txtFreq.setText(Double.toString(Config.fcdFrequency)); // trigger the change to the text field and set the center freq
								setCenterFreq();
							} else {
								setupDecoder(highSpeed.isSelected(), audioSource, audioSource);
							}	
					} catch (IOException e) {
						// This is a more serious error because we could not read audio data.  Need to halt decoder.
						Log.errorDialog("FCD Start Error", e.getMessage());
						e.printStackTrace(Log.getWriter());
						stopButton();
					} catch (DeviceException e) {
						Log.errorDialog("ERROR", "FCD Startup Error writing commands: " + e.getMessage());
						e.printStackTrace(Log.getWriter());
						stopButton();
					} catch (UsbException e) {
						Log.errorDialog("ERROR", "FCD Startup error with USB: " + e.getMessage());
						e.printStackTrace(Log.getWriter());
						stopButton();
					}
				}
				
				if (decoder1 != null) {
					decoder1Thread = new Thread(decoder1);
					decoder1Thread.setUncaughtExceptionHandler(Log.uncaughtExHandler);				

					if (decoder2 != null) {
						decoder2Thread = new Thread(decoder2);
						decoder2Thread.setUncaughtExceptionHandler(Log.uncaughtExHandler);				
					}
					if (Config.monitorAudio && sink != null)
						try {
							decoder1.setMonitorAudio(sink, Config.monitorAudio, speakerComboBox.getSelectedIndex());
						} catch (IllegalArgumentException e) {
							Log.errorDialog("ERROR", "Can't monitor the audio " + e.getMessage());
							e.printStackTrace(Log.getWriter());
						} catch (LineUnavailableException e) {
							Log.errorDialog("ERROR", "Can't monitor the audio " + e.getMessage());
							e.printStackTrace(Log.getWriter());
						}

				}

				if (decoder1Thread != null) {
					setMode();
					try {
						decoder1Thread.start();
						//if (audioGraphThread != null) audioGraph.stopProcessing();
						audioGraph.startProcessing(decoder1);
						if (eyePanelThread == null) {	
							eyePanelThread = new Thread(eyePanel);
							eyePanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
							eyePanelThread.start();
						}
						eyePanel.startProcessing(decoder1);
						if (decoder1 instanceof FoxBPSKDotProdDecoder || decoder1 instanceof FoxBPSKCostasDecoder) {
							if (phasorPanelThread == null) {	
								phasorPanelThread = new Thread(phasorPanel);
								phasorPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
								phasorPanelThread.start();
							}
							phasorPanel.startProcessing(decoder1);
							phasorPanel.setVisible(Config.showPhasor);
						} else {
							phasorPanel.setVisible(false);
						}
						enableSourceSelectionComponents(false);
						
						if (iqSource1 != null) {
							if (fftPanelThread == null) { 		
								fftPanelThread = new Thread(fftPanel);
								fftPanelThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
								fftPanelThread.start();
							}
							fftPanel.startProcessing(iqSource1);
						}	
					} catch (IllegalThreadStateException e2) {
						JOptionPane.showMessageDialog(this,
								e2.toString(),
								"THREAD LAUNCH ERROR",
								JOptionPane.ERROR_MESSAGE) ;
					}
					if (decoder2Thread != null) {
						try {
							decoder2Thread.start();
					
						} catch (IllegalThreadStateException e2) {
							JOptionPane.showMessageDialog(this,
									e2.toString(),
									"THREAD2 LAUNCH ERROR",
									JOptionPane.ERROR_MESSAGE) ;
						}
					}
					try {
						Thread.sleep(100); // wait to prevent race condition as decode starts
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (rfDevice != null) {
						txtFreq.setText(Double.toString(Config.fcdFrequency)); // trigger the change to the text field and set the center freq
						try {
							double f = setCenterFreq();
							txtFreq.setText(Double.toString(f));
						} catch (DeviceException e) {
							Log.println("ERROR setting the Center Frequency: " + e.getMessage());
							e.printStackTrace(Log.getWriter());
						} catch (IOException e) {
							Log.println("ERROR setting the Center Frequency: " + e.getMessage());
							e.printStackTrace(Log.getWriter());
						}

					}
					
				}
			}
		}
		Config.startButtonPressed = SourceTab.STARTED;
		Config.save();
	}
	
	public double setCenterFreqKhz(double freq) {
		double f = 0;
		txtFreq.setText(Double.toString(freq)); // trigger the change to the text field and set the center freq
		try {
			f = setCenterFreq();
			txtFreq.setText(Double.toString(f));
		} catch (DeviceException e) {
			Log.println("ERROR setting the Center Frequency: " + e.getMessage());
			e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("ERROR setting the Center Frequency: " + e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
		return f;
	}

	private double setCenterFreq() throws DeviceException, IOException {
		//String text = txtFreq.getText();
		double freq = Config.fcdFrequency; // we fall back to this and return it if we cant parse the value
		try {
			txtFreq.selectAll();
			freq = (double)(Math.round(Double.parseDouble(txtFreq.getText())*1000)/1000.0);
			if (iqSource1 != null)
				(iqSource1).setCenterFreqkHz(freq);
			if (iqSource2 != null)
				(iqSource2).setCenterFreqkHz(freq);
			Config.fcdFrequency = freq;
			if (rfDevice != null && panelFcd != null) {
				if (freq < rfDevice.getMinFreq() || freq > rfDevice.getMaxFreq()) {
					Log.errorDialog("DEVICE ERROR", "Frequency must be between " + rfDevice.getMinFreq() + " and " + rfDevice.getMaxFreq());
				} else {
					rfDevice.setFrequency((long) (freq*1000));
					panelFcd.updateFilter();
				}
			} else {

			}

		} catch (NumberFormatException n) {
			// not much to say here, just catch the error
		}
		Config.save();
		return freq;
	}

	private void stopDecoder() {
//		releaseFcd();
		if (decoder1 != null) {
			decoder1.stopProcessing(); // This blocks and waits for the audiosource to be done
			decoder1 = null;
			if (iqSource1 != null)
				iqSource1.stop();
			iqSource1 = null;
			decoder1Thread = null;
			Config.passManager.setDecoder1(decoder1, iqSource1, this);			
		}
		if (decoder2 != null) {
			decoder2.stopProcessing(); // This blocks and waits for the audiosource to be done
			decoder2 = null;
			if (iqSource2 != null)
				iqSource2.stop();
			iqSource2 = null;
			decoder2Thread = null;
			Config.passManager.setDecoder2(decoder2, iqSource2, this);			
		}
		
		if (rfDevice != null && !(rfDevice instanceof FCDTunerController)) {
			try {
				rfDevice.cleanup();  // Must call this to stop the buffer copy routines.  If exiting the USB device causes issues then don't exit in the called routine
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DeviceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//	rfDevice = null;
			
		}
		
		if (this.soundCardComboBox.getSelectedIndex() >= soundcardSources.length) {
			SDRpanel.setVisible(true);	
			if (panelFcd != null)
				panelFcd.setEnabled(true);
		} else {
			SDRpanel.setVisible(false);
		}
		

	}
	
	private void stopButton() {
		if (Config.passManager.getState() == PassManager.FADED || Config.passManager.getState() == PassManager.DECODE) {
			Object[] options = {"Yes",
	        "No"};
			int n = JOptionPane.showOptionDialog(
					MainWindow.frame,
					"The pass manager is still processing a satellite pass. If the satellite has\n"
					+ "faded it waits 2 minutes in case contact is re-established, even when it is at the\n"
					+ "horizon.  If you stop the decoder now the LOS will not be logged and TCA will not be calculated.\n"
					+ "Do you want to stop?",
					"Stop decoding while pass in progress?",
				    JOptionPane.YES_NO_OPTION, 
				    JOptionPane.ERROR_MESSAGE,
				    null,
				    options,
				    options[1]);
						
			if (n == JOptionPane.NO_OPTION) {
				// don't exit
			} else {
				stop();

			}
		} else {
			stop();
		}
	}
	
	private void stop() {
		stopDecoder();
		STARTED = false;
		btnStartButton.setText("Start");
		enableSourceSelectionComponents(true);
	}
	
	private void setMode() {
		if (iqSource1 != null) {
			if (pskCostas.isSelected()) {
				iqSource1.setMode(SourceIQ.MODE_PSK_COSTAS);
				//Config.mode = SourceIQ.MODE_PSK_COSTAS; // so it is saved for next time
				autoViewpanel.setVisible(false);
			}
			if (pskDotProd.isSelected()) {
				iqSource1.setMode(SourceIQ.MODE_PSK_NC);
				//Config.mode = SourceIQ.MODE_PSK_NC; // so it is saved for next time
				autoViewpanel.setVisible(false);
			}
			if (lowSpeed.isSelected()) {
				iqSource1.setMode(SourceIQ.MODE_FSK_DUV);
				Config.mode = SourceIQ.MODE_FSK_DUV; // so it is saved for next time
				autoViewpanel.setVisible(false);
			}
			if (highSpeed.isSelected()) {
				iqSource1.setMode(SourceIQ.MODE_FSK_HS);
				Config.mode = SourceIQ.MODE_FSK_HS; // so it is saved for next time
				autoViewpanel.setVisible(false);
			}
			if (auto.isSelected()) {
				iqSource1.setMode(SourceIQ.MODE_FSK_DUV);
				Config.mode = SourceIQ.MODE_FSK_AUTO; // so it is saved for next time
				autoViewpanel.setVisible(true);
			}
		}
		if (iqSource2 != null) {
			if (auto.isSelected()) iqSource2.setMode(SourceIQ.MODE_FSK_HS);			
		}
		Config.save();
	}
	
	private void enableSourceSelectionComponents(boolean t) {
		soundCardComboBox.setEnabled(t);
		cbSoundCardRate.setEnabled(t);
		highSpeed.setEnabled(t);
		lowSpeed.setEnabled(t);
		pskCostas.setEnabled(t);
		pskDotProd.setEnabled(t);
		int position = soundCardComboBox.getSelectedIndex(); 
		if (position == SourceAudio.FILE_SOURCE || position >= this.soundcardSources.length)
			auto.setEnabled(false);
		else
			auto.setEnabled(t);
		iqAudio.setEnabled(t);
		afAudio.setEnabled(t);
		MainWindow.enableSourceSelection(t);
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		//public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == rdbtnViewFilteredAudio) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
	            audioGraph.showUnFilteredAudio();
	            Config.viewFilteredAudio=false;
	            //Config.save();
	        } else {
	        	audioGraph.showFilteredAudio();
	        	Config.viewFilteredAudio=true;
	        	//Config.save();
	        }
			Config.save();
		}
		if (e.getSource() == rdbtnMonitorFilteredAudio) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				monitorFiltered=false;
	            Config.monitorFilteredAudio=false;
	            //Config.save();
	        } else {
	        	Config.monitorFilteredAudio=true;
	        	monitorFiltered=true;
	        	//Config.save();
	        }
			Config.save();
		}
		if (e.getSource() == rdbtnSquelchAudio) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
	            Config.squelchAudio=false;
	            //Config.save();
	        } else {
	        	Config.squelchAudio=true;
	        	//Config.save();
	        }
			Config.save();
		}
		if (e.getSource() == rdbtnFilterOutputAudio) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
	            Config.filterOutputAudio=false;
	            //Config.save();
	        } else {
	        	Config.filterOutputAudio=true;
	        	//Config.save();
	        }
			Config.save();
		}
		if (e.getSource() == rdbtnWriteDebugData) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.debugValues=false;
	            //Config.save();
	        } else {
	        	Config.debugValues=true;
	        }
			Config.save();
		}
		
		if (e.getSource() == cbRetuneCenterFrequency) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
	            Config.retuneCenterFrequency=false;
	        } else {
	        	Config.retuneCenterFrequency=true;
	        }
			txtFreq.setEnabled(!Config.retuneCenterFrequency);
			Config.save();
		}
		
//		if (e.getSource() == rdbtnShowFFT) {
//			if (fftPanel != null)
//			if (e.getStateChange() == ItemEvent.DESELECTED) {
//				setFFTVisible(false);
//	        } else {
//	        	setFFTVisible(true);
//	        	
//	        }
		Config.save();
//		}
		if (e.getSource() == autoStart) {
			
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.whenAboveHorizon = false;
	        } else {
	        	Config.whenAboveHorizon = true;	        	
	        }
			Config.save();
		}
		
		if (e.getSource() == rdbtnUseNco) {
			if (fftPanel != null)
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useNCO = false;
				Log.println("NCO = false");
	        } else {
	        	Config.useNCO = true;
				Log.println("NCO = true");
	        	
	        }
			Config.save();
		}
		if (e.getSource() == rdbtnUseCostas) {
			if (fftPanel != null)
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	        } else {
	        	
	        }
			Config.save();
		}
		/*
		if (e.getSource() == rdbtnUseLimiter) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.useLimiter=false;
	            //Config.save();
	        } else {
	        	Config.useLimiter=true;
	        	
	        	//Config.save();
	        }
	        Config.save();
		}
		*/
		if (e.getSource() == rdbtnShowIF) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.showIF=false;
	            //Config.save();
	        } else {
	        	Config.showIF=true;
	        	//Decoder.SAMPLE_WINDOW_LENGTH = 1000;  //// cause a crash for testing
//	        	String s = null;
//	        	int i = s.length();  // crash the GUI EDT for testing 
	        	//Config.save();
	        }
			Config.save();
		}
		
		
		if (e.getSource() == rdbtnFindSignal) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.findSignal=false;
	            //Config.save();
	        } else {
	        	Config.findSignal=true;
	        	
	        	//Config.save();
	        }
			findSignalPanel.setVisible(Config.findSignal);
			Config.save();
		}
		
		if (e.getSource() == rdbtnShowLog) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				logScrollPane.setVisible(false);
				log.setVisible(false);
				Log.setGUILog(null);
				
//	            Config.applyBlackmanWindow=false;
	            //Config.save();
	        } else {
	        	logScrollPane.setVisible(true);
	        	log.setVisible(true);
	        	MainWindow.frame.repaint(); // need to kick this to get it to redraw straight away.  Not sure why
	    		Log.setGUILog(this);

	        	//	        	Config.applyBlackmanWindow=true;
	        	
	        	//Config.save();
	        }
			Config.save();
		}
		
	}

	/**
	 * Try to close any open OS resources
	 */
	public void shutdown() {
		Config.startButtonPressed = SourceTab.STARTED;
		if (rfDevice != null)
			try {
				rfDevice.cleanup();
			} catch (IOException e) {
				e.printStackTrace(Log.getWriter());
			} catch (DeviceException e) {
				e.printStackTrace(Log.getWriter());
			}

		if (decoder1 != null)
			decoder1.stopProcessing(); 
		if (decoder2 != null)
			decoder2.stopProcessing(); 
	}
	
	/**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
            
        } 
    }
        
    /**
     * A task sub class that we use to track the progress of a wav file as it is being played.  This thread updates the
     * progress bar
     * @author chris.e.thompson
     *
     */
	class Task implements Runnable { 
		int progress;
		SourceWav wavSource;
		
		public Task(SourceWav wav) {
			wavSource = wav;
			progress = 0;
		}
		
		public void setProgress(int p) { progress = p; }
		
		public void resetProgress() {
			progress = 0;
			setProgress(progress); 
		}
	
		/*
         * Main task. Executed in background thread.
         */
        @Override
        public void run() { 
    		Thread.currentThread().setName("ProgressBar");
            //Initialize progress property.
            setProgress(0);
            while (progress < 100 && wavSource != null) {
                //Sleep for a bit
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {}
                //Get progress from decoder
    //            System.out.println("task..");
                //if (audioSource instanceof SourceWav)
                if (wavSource != null)
                	progress = wavSource.getPercentProgress();
                else
                	progress = -1;
                if (progress != -1)
                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                		progressBar.setValue(Math.min(progress,100));
                	}
                });
            }
            setProgress(100);
            done();
            Log.println("WORKER IS DONE");
        }
 
        
        
        /*
         * Executed in event dispatching thread
         */
        public void done() {
        	
            Toolkit.getDefaultToolkit().beep();
            stopButton();    
            //btnStartButton.doClick(); // toggle start to stop
            //System.out.println("WORKER IS DONE");
        }
        
        public void end() {
        	
            setProgress(100);
            //System.out.println("WORKER IS DONE");
        }
    }

	@Override
	public void focusGained(FocusEvent arg0) {
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource() == this.peakLevel) {
			try {
			Config.SCAN_SIGNAL_THRESHOLD = Double.parseDouble(peakLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
			Config.save();
		}

		if (e.getSource() == this.avgLevel) {
			try {
			Config.ANALYZE_SNR_THRESHOLD = Double.parseDouble(avgLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
			Config.save();
		}

		if (e.getSource() == this.bitLevel) {
			try {
			Config.BIT_SNR_THRESHOLD = Double.parseDouble(bitLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
			Config.save();
		}
		if (e.getSource() == txtFreq) {
			try {
				double f = setCenterFreq();
				txtFreq.setText(Double.toString(f));
			} catch (DeviceException e1) {
				Log.errorDialog("ERROR loosing focus", e1.getMessage());
				e1.printStackTrace(Log.getWriter());
			} catch (IOException e1) {
				e1.printStackTrace(Log.getWriter());
			} catch (LibUsbException e2) {
				e2.printStackTrace(Log.getWriter());
			}
			
		}
		
	}

	private boolean aboveHorizon = false;
	
	public void startDecoding() {
		aboveHorizon = true;
	}
	
	public void stopDecoding() {
		aboveHorizon = false;
	}
	
	/**
	 * Method that checks to see if the decoder should be started or stopped.  The start/stop methods can be called from the pass manager.
	 * Note that this new thread is no longer part of the Event Dispatch thread.  Any changes to the GUI must be passed back to that thread.
	 * 
	 */
	@Override
	public void run() {
		Thread.currentThread().setName("SourceTab:Tracking");

		// Runs until we exit
		while(true) {

			// Sleep first to avoid race conditions at start up
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					boolean atLeastOneTracked = false;
					try {
						if (Config.satManager.updated) {
							atLeastOneTracked = buildTrackedSpacecraftList();	
							Config.satManager.updated = false;
						}


						for (int s=0; s < Config.satManager.spacecraftList.size(); s++) {
							Spacecraft sat = Config.satManager.spacecraftList.get(s);
							if (sat.user_track)
								atLeastOneTracked = true;
							if ((Config.foxTelemCalcsPosition || (Config.whenAboveHorizon && aboveHorizon)) && sat.user_track && sat.aboveHorizon())
								satPosition[s].setForeground(Config.AMSAT_RED);
							else
								satPosition[s].setForeground(Config.AMSAT_BLUE);

							if (Config.foxTelemCalcsPosition && sat.user_track) {
								if (sat.satPos != null) {
									double az = FramePart.radToDeg(sat.satPos.getAzimuth());
									double el = FramePart.radToDeg(sat.satPos.getElevation());
									String position = "" + String.format("%2.1f", az) 
									+ " | " + String.format("%2.1f", el);
									if (Config.foxTelemCalcsDoppler) {
										double freq = sat.satPos.getDopplerFrequency(sat.user_telemetryDownlinkFreqkHz);
										String sign="";
										if (freq > 0) sign = "+";
										position = position + " | " + sign+String.format("%2.3f", freq) + "kHz";
									}
									satPosition[s].setText(position);
								} else {
									String msg = "Tracked / ";
									if (Config.whenAboveHorizon) {
										msg = "Not " + msg;
										satPosition[s].setForeground(Color.GRAY);
									}
									if (sat.satPosErrorCode == FramePart.NO_T0)
										msg = msg + "No T0";
									else if (sat.satPosErrorCode == FramePart.NO_TLE)
										msg = msg + "No TLE";
									else
										msg = msg + "No Position";
									satPosition[s].setText(msg);
								}
							} else if (Config.useDDEforAzEl && sat.user_track) {
								satPosition[s].setText("Tracked via SATPC32");
							} else {
								if (sat.user_track)
									satPosition[s].setText("Tracked");
								else {
									satPosition[s].setText("Not Tracked");
									satPosition[s].setForeground(Color.GRAY);
								}
							}

						}
					} catch (ArrayIndexOutOfBoundsException e) {
						// We changed the size of the spacecraft array.  Do nothing.  This will fix itself
					}
					if (atLeastOneTracked && (Config.foxTelemCalcsPosition || Config.useDDEforAzEl)) {
						autoStart.setEnabled(true);
						if (Config.foxTelemCalcsPosition && !Config.findSignal)
							cbRetuneCenterFrequency.setEnabled(true);
						else {
							cbRetuneCenterFrequency.setEnabled(false);
							cbRetuneCenterFrequency.setSelected(false);
						}							
					} else {
						autoStart.setEnabled(false);
						cbRetuneCenterFrequency.setEnabled(false);
						cbRetuneCenterFrequency.setSelected(false);
					}
					autoStart.setSelected(Config.whenAboveHorizon);
					if (soundCardComboBox.getSelectedIndex() == 0) {
						btnStartButton.setEnabled(false);
					} else if (Config.whenAboveHorizon && soundCardComboBox.getSelectedIndex() != 0) {
						rdbtnFindSignal.setEnabled(false);
						btnStartButton.setEnabled(false);
						lblWhenAboveHorizon.setVisible(true);
						if (aboveHorizon && !STARTED) {
							processStartButtonClick();
						}
						if (!aboveHorizon && STARTED) {
							processStartButtonClick();
						}
					} else {
						btnStartButton.setEnabled(true);
						lblWhenAboveHorizon.setVisible(false);
					}
					
					if (Config.iq && (atLeastOneTracked && !Config.foxTelemCalcsDoppler)) {
						//rdbtnFindSignal.setEnabled(true);
						rdbtnFindSignal.setSelected(true);
						Config.findSignal = true;
						if (Config.iq) {
							findSignalPanel.setVisible(true);
						}
					} else {
						rdbtnFindSignal.setEnabled(false);
						rdbtnFindSignal.setSelected(false);
						Config.findSignal = false;
						findSignalPanel.setVisible(false);
					}
					
					
					// These are just debug values
					if (Config.debugValues && !unpause.isVisible()) {
						unpause.setVisible(true);
						play.setVisible(true);
					}
					if (!Config.debugValues && unpause.isVisible()) {
						unpause.setVisible(false);
						play.setVisible(false);
					}
				}
			});
		}

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		for (int s=0; s < Config.satManager.spacecraftList.size(); s++) {
			if (e.getSource() == satPosition[s]) {
				Config.satManager.spacecraftList.get(s).user_track = !Config.satManager.spacecraftList.get(s).user_track;
				((FoxSpacecraft)Config.satManager.spacecraftList.get(s)).save();
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
