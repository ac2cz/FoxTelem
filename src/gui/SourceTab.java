package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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

import FuncubeDecoder.FUNcubeDecoder;
import common.Config;
import common.Log;
import common.PassManager;
import decoder.FoxDecoder;
import decoder.Decoder;
import decoder.Fox200bpsDecoder;
import decoder.Fox9600bpsDecoder;
import decoder.SinkAudio;
import decoder.SourceAudio;
import decoder.SourceIQ;
import decoder.SourceSoundCardAudio;
import decoder.SourceUSB;
import decoder.SourceWav;
import decoder.FoxBPSK.FoxBPSKDecoder;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.TunerManager;
import device.airspy.AirspyDevice;
import device.airspy.AirspyPanel;
import fcd.FcdDevice;
import fcd.FcdProPanel;
import fcd.FcdProPlusDevice;
import fcd.FcdProPlusPanel;

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
public class SourceTab extends JPanel implements ItemListener, ActionListener, PropertyChangeListener, FocusListener {
	Thread audioGraphThread;
	Thread eyePanelThread;
	//Thread fcdPanelThread;
	
	Thread fftPanelThread;
	Thread decoder1Thread;
	Thread decoder2Thread;
	AudioGraphPanel audioGraph;
	EyePanel eyePanel;
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
	JCheckBox rdbtnTrackSignal;
	JCheckBox rdbtnFindSignal;
	JCheckBox rdbtnWhenAboveHorizon;
	JCheckBox rdbtnShowLog;
	JCheckBox rdbtnShowFFT;
	JCheckBox rdbtnFcdLnaGain;
	JCheckBox rdbtnFcdMixerGain;
	JTextField peakLevel;
	JTextField avgLevel;
	JTextField bitLevel;
	
	//JCheckBox rdbtnUseNco;
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
	JRadioButton psk;
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
	JRadioButton showLevel;
	JRadioButton viewHighSpeed;
	JRadioButton viewLowSpeed;
	JPanel findSignalPanel;
	JPanel autoViewpanel;
	Box.Filler audioOptionsFiller;
	JTextArea log;
	JScrollPane logScrollPane;
	
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
	public static final String FUNCUBE = "FUNcube";
//	public static final String FUNCUBE = "XXXXXXX";  // hack to disable the func cube option
	Decoder decoder1;
	Decoder decoder2;
	SourceIQ iqSource1;
	SourceIQ iqSource2;
	//SourceAudio audioSource = null; // this is the source of the audio for the decoder.  We select it in the GUI and pass it to the decoder to use
	SinkAudio sink = null;
	private boolean monitorFiltered;
	
	public static boolean STARTED = false;
	//private JPanel leftPanel_1;
	JLabel lblFreq;
	JLabel lblkHz;
	private JTextField txtFreq;
	MainWindow mainWindow;
	private JProgressBar progressBar;
	
	int splitPaneHeight;
	JSplitPane splitPane;
	
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

		JPanel leftPanel_1 = new JPanel();
		buildLeftPanel(topPanel,  BorderLayout.CENTER, leftPanel_1);

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
		if (Config.startButtonPressed) processStartButtonClick();
	}
	
	public void showFilters(boolean b) { 
		filterPanel.setVisible(b);
		audioOptionsFiller.setVisible(!b);}
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
		

		showSNR = addRadioButton("Show Avg SNR", options1 );
		showLevel = addRadioButton("Peak SNR", options1 );
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
		 
		rdbtnTrackSignal = new JCheckBox("Track Doppler");
		options1.add(rdbtnTrackSignal);
		rdbtnTrackSignal.addItemListener(this);
		rdbtnTrackSignal.setSelected(Config.trackSignal);
		rdbtnTrackSignal.setVisible(true);

		findSignalPanel = new JPanel();
		findSignalPanel.setLayout(new BoxLayout(findSignalPanel, BoxLayout.X_AXIS));
		
		rdbtnFindSignal = new JCheckBox("Find Signal");
		options1.add(rdbtnFindSignal);
		rdbtnFindSignal.addItemListener(this);
		rdbtnFindSignal.setSelected(Config.findSignal);
		rdbtnFindSignal.setVisible(true);

		optionsPanel.add(findSignalPanel);
		
		rdbtnWhenAboveHorizon = new JCheckBox("when above horizon  ");
		rdbtnWhenAboveHorizon.setToolTipText("Find Signal is executed when the Satellite is above the horizon according to SatPC32, which must be running");
		findSignalPanel.add(rdbtnWhenAboveHorizon);
		rdbtnWhenAboveHorizon.addItemListener(this);
		rdbtnWhenAboveHorizon.setSelected(Config.useDDEforFindSignal);
		
		
		JLabel when = new JLabel ("when peak over ");
		findSignalPanel.add(when);
		peakLevel = new JTextField(Double.toString(Config.SCAN_SIGNAL_THRESHOLD));
		peakLevel.setMinimumSize(new Dimension(30,1));
		peakLevel.addActionListener(this);
		peakLevel.addFocusListener(this);
		findSignalPanel.add(peakLevel);
		JLabel rf = new JLabel ("dB, avg over ");
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
	
		/*
		rdbtnUseNco = new JCheckBox("Use NCO carrier");
		rdbtnUseNco.addItemListener(this);
		rdbtnUseNco.setSelected(SourceIQ.useNCO);
		optionsPanel.add(rdbtnUseNco);
		*/

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

		eyePanel = new EyePanel();
		eyePanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		bottomPanel.add(eyePanel, BorderLayout.EAST);
		eyePanel.setBackground(Color.LIGHT_GRAY);
		eyePanel.setPreferredSize(new Dimension(200, 100));
		eyePanel.setMaximumSize(new Dimension(200, 100));
		
		fftPanel = new FFTPanel();
		fftPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		fftPanel.setBackground(Color.LIGHT_GRAY);
		
		//bottomPanel.add(fftPanel, BorderLayout.SOUTH);
		setFFTVisible(false);
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
	
	private void buildRightPanel(JPanel parent, String layout, JPanel rightPanel) {
		parent.add(rightPanel, layout);
			
		JPanel opts = new JPanel();
		rightPanel.add(opts);
		opts.setLayout(new BorderLayout());
		
		JPanel optionsPanel = new JPanel();
		optionsPanel.setBorder(new TitledBorder(null, "Audio Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		opts.add(optionsPanel, BorderLayout.CENTER);
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
		
		filterPanel = new FilterPanel();
		opts.add(filterPanel, BorderLayout.NORTH);
		
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
		
		rdbtnShowFFT = new JCheckBox("Show FFT");
		rdbtnShowFFT.addItemListener(this);
		rdbtnShowFFT.setSelected(true);
		optionsPanel.add(rdbtnShowFFT);
		rdbtnShowFFT.setVisible(false);
		audioOptionsFiller = new Box.Filler(new Dimension(10,10), new Dimension(100,80), new Dimension(100,500));
		optionsPanel.add(audioOptionsFiller);
		
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
		showFilters(Config.showFilters); // hide the filters because we have calculated the optimal matched filters

	}
	
	public void log(String text) {
		if (rdbtnShowLog.isSelected()) {
			log.append(text);
			//log.setLineWrap(true);
			log.setCaretPosition(log.getLineCount());
		}
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
		lblSource.setMinimumSize(new Dimension(180, 14));
		lblSource.setMaximumSize(new Dimension(180, 14));
		
		lowSpeed = addRadioButton("DUV", panel_2 );
		highSpeed = addRadioButton("High Speed", panel_2 );
		psk = addRadioButton("PSK", panel_2 );
//		highSpeed = addRadioButton("High Speed", panel_2 );
		auto = addRadioButton("Auto", panel_2 );
		ButtonGroup group = new ButtonGroup();
		group.add(lowSpeed);
		group.add(highSpeed);
		group.add(psk);
		group.add(auto);
		
		if (Config.autoDecodeSpeed) {
			auto.setSelected(true);
			enableFilters(true);
		} else
		if (Config.highSpeed) {
			highSpeed.setSelected(true);
			enableFilters(false);
		} else {
			lowSpeed.setSelected(true);
			enableFilters(true);
		}
		
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
		
		String[] sources = SourceSoundCardAudio.getAudioSources();
		ArrayList<String> usbSources = null;
		try {
			usbSources = tunerManager.makeDeviceList();
		} catch (UsbException e) {
			Log.println("ERROR GETTING USB SOURCES");
			e.printStackTrace();
		}
		String[] allSources = new String[sources.length + usbSources.size()];
		int j = 0;
		for (String s : sources) allSources[j++] = s;
		if (usbSources != null)
			for (String s : usbSources) allSources[j++] = s;
		soundCardComboBox = new JComboBox<String>(allSources);
		soundCardComboBox.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				//Log.println("Rebuild Sound card List");
				soundCardComboBox.removeAllItems();
				for (String s: SourceSoundCardAudio.getAudioSources()) {
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
		centerPanel.add(panel_c);
		panel_c.setLayout(new BoxLayout(panel_c, BoxLayout.Y_AXIS));
		

		JPanel panelSDR = new JPanel();
		panel_c.add(panelSDR);
		//panelSDR.setLayout(new BoxLayout(panelSDR, BoxLayout.Y_AXIS));
		panelSDR.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JPanel panelFreq = new JPanel();
		panelSDR.add(panelFreq, BorderLayout.CENTER);
		panelFreq.setLayout(new BoxLayout(panelFreq, BoxLayout.X_AXIS));
		
		lblFreq = new JLabel("Center Frequency ");
		panelFreq.add(lblFreq);
		lblFreq.setVisible(false);
		
		
		txtFreq = new JTextField();
		txtFreq.addActionListener(this);
		panelFreq.add(txtFreq);
		txtFreq.setColumns(10);
		txtFreq.setVisible(false);

		lblkHz = new JLabel(" kHz   ");
		panelFreq.add(lblkHz);
		lblkHz.setVisible(false);
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
		panel_c.add(SDRpanel, BorderLayout.CENTER);
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
		
		JPanel southPanel = new JPanel();
		leftPanel.add(southPanel, BorderLayout.SOUTH);
		southPanel.setLayout(new BorderLayout(3, 3));
		//southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

		JPanel panel_3 = new JPanel();
		southPanel.add(panel_3, BorderLayout.NORTH);
		
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
		southPanel.add(panelCombo, BorderLayout.CENTER);
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
			soundCardComboBox.setSelectedIndex(SourceSoundCardAudio.getDeviceIdByName(Config.soundCard));
		}

		if (Config.audioSink != null && !Config.audioSink.equalsIgnoreCase(Config.NO_SOUND_CARD_SELECTED)) {
			speakerComboBox.setSelectedIndex(SinkAudio.getDeviceIdByName(Config.audioSink));
		}
		autoViewpanel = new JPanel();
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
			autoViewpanel.setVisible(false);
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
			lblFileName.setText(file.getName());
			panelFile.setVisible(true);
			btnStartButton.setEnabled(true);
			cbSoundCardRate.setVisible(false);
			return true;
		}
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
	
	private void setFFTVisible(boolean b) {
		fftPanel.setVisible(b);
		if (b==true) {
			if (Config.splitPaneHeight != 0) 
				splitPane.setDividerLocation(Config.splitPaneHeight);
			else
				splitPane.setDividerLocation(200);
		}
	}
	
	private void setIQVisible(boolean b) {
		setFFTVisible(b);
		rdbtnShowFFT.setVisible(b);
		rdbtnShowIF.setVisible(b);
		rdbtnTrackSignal.setVisible(b);
		rdbtnFindSignal.setVisible(b);
		if (Config.isWindowsOs())
			rdbtnWhenAboveHorizon.setVisible(b);
		else
			rdbtnWhenAboveHorizon.setVisible(false);
		findSignalPanel.setVisible(b&&Config.findSignal);
		showSNR.setVisible(b);
		showLevel.setVisible(b);
		//		rdbtnApplyBlackmanWindow.setVisible(b);
		setFreqVisible(b);
		if (this.soundCardComboBox.getSelectedIndex() == SourceAudio.AIRSPY_SOURCE) {
				cbSoundCardRate.setVisible(!b);
			}
	}
	
	private void setFreqVisible(boolean b) {
		lblFreq.setVisible(b);
		lblkHz.setVisible(b);
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
				decoder1.setMonitorAudio(sink, Config.monitorAudio, speakerComboBox.getSelectedIndex());
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		audioGraph.startProcessing(decoder1);
		eyePanel.startProcessing(decoder1);
		fftPanel.startProcessing(iqSource1);
		
		viewLowSpeed.setSelected(true);

	}

	public void setViewDecoder2() {
		if (decoder2 != null) {
			try {
				if (decoder1 != null)
					decoder1.stopAudioMonitor();
				if (decoder2 != null)
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
			fftPanel.startProcessing(iqSource2);
			
			viewHighSpeed.setSelected(true);
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
	
		if (e.getSource() == highSpeed) { 
				Config.highSpeed = true;
				Config.autoDecodeSpeed = false;
				enableFilters(false);
				autoViewpanel.setVisible(false);
				if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_FM);
				//Config.save();
		}
		if (e.getSource() == lowSpeed) { 
			Config.highSpeed = false;
			Config.autoDecodeSpeed = false;
			enableFilters(true);
			autoViewpanel.setVisible(false);
			if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_NFM);
			//Config.save();
		}
		if (e.getSource() == psk) { 
			Config.highSpeed = false;
			Config.autoDecodeSpeed = false;
			enableFilters(false);
			autoViewpanel.setVisible(false);
			if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_PSK);
			//Config.save();
		}
		if (e.getSource() == auto) { 
			Config.autoDecodeSpeed = true;
			enableFilters(true);
			if (iqSource1 != null) iqSource1.setMode(SourceIQ.MODE_NFM);
			if (iqSource2 != null) iqSource2.setMode(SourceIQ.MODE_FM);
	//		autoViewpanel.setVisible(true);
			//Config.save();
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
		}
		if (e.getSource() == afAudio) { 
			Config.iq = false;
			setIQVisible(false);
		}
		if (e.getSource() == showSNR) { 
			Config.showSNR = true;
			
		}
		if (e.getSource() == showLevel) { 
			Config.showSNR = false;
			
		}

		if (e.getSource() == this.peakLevel) {
			try {
			Config.SCAN_SIGNAL_THRESHOLD = Double.parseDouble(peakLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
		}

		if (e.getSource() == this.avgLevel) {
			try {
			Config.ANALYZE_SNR_THRESHOLD = Double.parseDouble(avgLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
		}

		if (e.getSource() == this.bitLevel) {
			try {
			Config.BIT_SNR_THRESHOLD = Double.parseDouble(bitLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
		}

		// Frequency Text Field
		if (e.getSource() == this.txtFreq) {
			setCenterFreq();
		}
		
		// SOUND CARD AND FILE COMBO BOX
		if (e.getSource() == soundCardComboBox) {
			processSoundCardComboBox();
		}

		// SPEAKER OUTPUT COMBO BOX
		if (e.getSource() == speakerComboBox) {
			//String source = (String)speakerComboBox.getSelectedItem();
		}
		
		// USER CHANGES THE SAMPLE RATE
		if (e.getSource() == cbSoundCardRate) {
			// store the value so it is saved if we exit
			Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());
		}
		
		// MONITOR AUDIO BUTTON
		if (e.getSource() == btnMonitorAudio) {
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
						e1.toString(),
						"ARGUMENT ERROR",
					    JOptionPane.ERROR_MESSAGE) ;
				//e1.printStackTrace();	
			} catch (LineUnavailableException e1) {
				JOptionPane.showMessageDialog(this,
						e1.toString(),
						"LINE UNAVAILABLE ERROR",
					    JOptionPane.ERROR_MESSAGE) ;
			}
			if (Config.monitorAudio) { 
				btnMonitorAudio.setText("Silence Speaker");
				speakerComboBox.setEnabled(false);
			} else {
				btnMonitorAudio.setText("Monitor Audio");
				speakerComboBox.setEnabled(true);
			}
		}

		// START BUTTON
		if (e.getSource() == btnStartButton) {
			processStartButtonClick();
		}

		
	}

	
	private void setupAudioSink(Decoder decoder12) {
		int position = speakerComboBox.getSelectedIndex();
		
		try {
			sink = new SinkAudio(decoder12.getAudioFormat());
			sink.setDevice(position);
		if (position != -1) {
			Config.audioSink = SinkAudio.getDeviceName(position);
			//Config.save();
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

	private SourceWav setWavFile() {
		SourceWav audioSource = null;
		//if (audioSource != null)
		//	audioSource.stop();
		try {
			audioSource = new SourceWav(Config.windowCurrentDirectory + File.separator + lblFileName.getText());
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
				highSpeed.setSelected(false);
				psk.setSelected(false);
				Config.autoDecodeSpeed = false;
			}
		} else if (position == SourceAudio.AIRSPY_SOURCE) {
			btnStartButton.setEnabled(true);
			cbSoundCardRate.setVisible(true);
			panelFile.setVisible(false);
			auto.setEnabled(true);
			setIQVisible(true);
		} else { // its not a file so its a sound card or FCD that was picked
			boolean fcdSelected = fcdSelected();
			auto.setEnabled(true);
			if (fcdSelected) {

			} else {
				Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());	
			}
			
			Config.soundCard = SourceSoundCardAudio.getDeviceName(position); // store this so it gets saved
			btnStartButton.setEnabled(true);
			cbSoundCardRate.setVisible(true);
			panelFile.setVisible(false);
			if (Config.iq) {
				setIQVisible(true);
			} else {
				setIQVisible(false);
			}
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
	
	private boolean fcdSelected() {
		
		String source = (String)soundCardComboBox.getSelectedItem();
		Pattern pattern = Pattern.compile(FUNCUBE);
		Matcher matcher = pattern.matcher(source);
		if (matcher.find()) {
			return true;
		}
		return false;
	}
	
	private boolean usingFcd() throws IOException, DeviceException {
		boolean fcdSelected = fcdSelected();
		if (fcdSelected) {
			
				if (rfDevice == null) {
					rfDevice = FcdDevice.makeDevice();	
					if (rfDevice == null) return false; // FIXME this is an issue because we found the description but not the HID device
					try {
						if (rfDevice instanceof FcdProPlusDevice)
							panelFcd = new FcdProPlusPanel();
						else
							panelFcd = new FcdProPanel();
						panelFcd.setDevice(rfDevice);
					} catch (IOException e) {
						e.printStackTrace(Log.getWriter());
					} catch (DeviceException e) {
						e.printStackTrace(Log.getWriter());
					}
					SDRpanel.add(panelFcd, BorderLayout.CENTER);
				}
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

	}
	
	private SourceSoundCardAudio setupSoundCard(boolean highSpeed, int sampleRate) {
		int position = soundCardComboBox.getSelectedIndex();
		int circularBufferSize = sampleRate * 4;
		if (highSpeed || Config.autoDecodeSpeed) {
			circularBufferSize = sampleRate * 4;
		} else {
		}		SourceSoundCardAudio audioSource = null;
		boolean storeStereo = false;
		if (Config.iq) storeStereo = true;
		try {
			if (Config.autoDecodeSpeed)
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
		
		if (Config.autoDecodeSpeed) {
			if (Config.iq) {
				decoder1 = new Fox200bpsDecoder(audioSource, 0);
				decoder2 = new Fox9600bpsDecoder(audioSource2, 0);
			} else {
				decoder1 = new Fox200bpsDecoder(audioSource, 0);
				decoder2 = new Fox9600bpsDecoder(audioSource2, 1);
			}
		} else if (this.psk.isSelected()) 
			decoder1 = new FoxBPSKDecoder(audioSource, 0); // test PSK decoder
		else if (highSpeed) {
			decoder1 = new Fox9600bpsDecoder(audioSource, 0);
		} else {
			decoder1 = new Fox200bpsDecoder(audioSource, 0);
		}
		setupAudioSink(decoder1);
	}
	

	
	/**
	 * The user has clicked the start button.  We already know the audio Source.
	 */
	private void processStartButtonClick() {
		//String source = (String)soundCardComboBox.getSelectedItem();
		int position = soundCardComboBox.getSelectedIndex();
				
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
					SourceWav wav = setWavFile();
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
							setCenterFreq();
							Config.passManager.setDecoder1(decoder1, iqSource1, this);
							//if (Config.autoDecodeSpeed)
							//	Config.passManager.setDecoder2(decoder2, iqSource2, this);
						} else {
							setupDecoder(highSpeed.isSelected(), wav, wav);
						}
						progressThread.start(); // need to start after the audio source wav is created
					} else {
						stopButton();
					}
				} else if (position == SourceAudio.AIRSPY_SOURCE) {
					SourceAudio audioSource;
					if (rfDevice == null) {
						Log.println("Airspy Source Selected");
						try {
							rfDevice = AirspyDevice.makeDevice();
						} catch (UsbException e1) {
							Log.errorDialog("AIRSPY Start Error", e1.getMessage());
							e1.printStackTrace(Log.getWriter());
							stopButton();
						}
					}
					try {
						panelFcd = new AirspyPanel();

					} catch (IOException e) {
						Log.errorDialog("AIRSPY Panel Error", e.getMessage());
						e.printStackTrace(Log.getWriter());
						stopButton();
					} catch (DeviceException e) {
						Log.errorDialog("AIRSPY Device Error", e.getMessage());
						e.printStackTrace(Log.getWriter());
						stopButton();
					}

					if (rfDevice == null) {
						Log.errorDialog("Missing AIRSPY device", "Insert the device or choose anther source");
						stopButton();
					} else {
						try {
							panelFcd.setDevice(rfDevice);
						} catch (IOException e) {
							Log.errorDialog("AIRSPY Panel Error", e.getMessage());
							e.printStackTrace(Log.getWriter());
							stopButton();
						} catch (DeviceException e) {
							Log.errorDialog("AIRSPY Device Error", e.getMessage());
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
						//int decmimation = panelFcd.getDecimationRate();
						int channels = 0;
						if (Config.autoDecodeSpeed)
							channels = 2;
						audioSource = new SourceUSB("Airspy USB Source", rate, rate*2, channels); 
						((AirspyDevice)rfDevice).setUsbSource((SourceUSB)audioSource);
						boolean decoder1HS = highSpeed.isSelected();
						if (Config.autoDecodeSpeed) {
							iqSource2 = new SourceIQ(rate*2, 0,true);
							iqSource2.setAudioSource(audioSource,1); 
							decoder1HS = false;
						}
						iqSource1 = new SourceIQ(rate*2, 0,decoder1HS); 
						iqSource1.setAudioSource(audioSource,0);
						setCenterFreq();
						setupDecoder(highSpeed.isSelected(), iqSource1, iqSource1);
						setupAudioSink(decoder1);
						Config.passManager.setDecoder1(decoder1, iqSource1, this);
						if (Config.autoDecodeSpeed)
							Config.passManager.setDecoder2(decoder2, iqSource2, this);
						Config.soundCard = SourceSoundCardAudio.getDeviceName(position); // store the name
					}

				} else { // soundcard - fcd or normal
					SourceAudio audioSource;
					boolean fcdSelected = false;
					try {
						fcdSelected = usingFcd();
						if (fcdSelected) {
							setFcdSampleRate();
						} else {
							Config.scSampleRate = Integer.parseInt((String) cbSoundCardRate.getSelectedItem());	
						}

						Config.soundCard = SourceSoundCardAudio.getDeviceName(position); // store this so it gets saved

						audioSource = setupSoundCard(highSpeed.isSelected(), Config.scSampleRate);
						if (audioSource != null)
							if (fcdSelected || Config.iq) {
								Log.println("IQ Source Selected");
								boolean decoder1HS = highSpeed.isSelected();
								if (Config.autoDecodeSpeed) {
									iqSource2 = new SourceIQ(Config.scSampleRate * 4, 0,true);
									iqSource2.setAudioSource(audioSource,1); 
									decoder1HS = false;
								}
								iqSource1 = new SourceIQ(Config.scSampleRate * 4, 0,decoder1HS); 
								iqSource1.setAudioSource(audioSource,0); 
								
								setupDecoder(highSpeed.isSelected(), iqSource1, iqSource2);
								Config.passManager.setDecoder1(decoder1, iqSource1, this);
								if (Config.autoDecodeSpeed)
									Config.passManager.setDecoder2(decoder2, iqSource2, this);
								txtFreq.setText(Long.toString(Config.fcdFrequency)); // trigger the change to the text field and set the center freq
								setCenterFreq();
							} else {
								setupDecoder(highSpeed.isSelected(), audioSource, audioSource);
							}	
					} catch (IOException e) {
						Log.errorDialog("FCD Start Error", e.getMessage());
						e.printStackTrace(Log.getWriter());
						stopButton();
					} catch (DeviceException e) {
						Log.errorDialog("FCD Start Error", e.getMessage());
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (rfDevice != null) {
						txtFreq.setText(Long.toString(Config.fcdFrequency)); // trigger the change to the text field and set the center freq
						setCenterFreq();
					}
					setMode();
				}
			}
		}

	}

	private void setCenterFreq() {
		//String text = txtFreq.getText();
		try {
			txtFreq.selectAll();
			int freq = Integer.parseInt(txtFreq.getText());
			if (iqSource1 != null)
				(iqSource1).setCenterFreqkHz(freq);
			if (iqSource2 != null)
				(iqSource2).setCenterFreqkHz(freq);
			Config.fcdFrequency = freq;
			if (rfDevice != null && panelFcd != null) {
				if (freq < rfDevice.getMinFreq() || freq > rfDevice.getMaxFreq()) {
					Log.errorDialog("DEVICE ERROR", "Frequency must be between " + rfDevice.getMinFreq() + " and " + rfDevice.getMaxFreq());
				} else {
					try {
						rfDevice.setFrequency(freq*1000);
						panelFcd.updateFilter();
					} catch (DeviceException e1) {
						Log.errorDialog("ERROR", e1.getMessage());
						e1.printStackTrace(Log.getWriter());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace(Log.getWriter());
					}
				}
			} else {

			}

		} catch (NumberFormatException n) {
			// not much to say here, just catch the error
		}

	}

	private void stopDecoder() {
//		releaseFcd();
		if (decoder1 != null) {
			decoder1.stopProcessing(); // This blocks and waits for the audiosource to be done
			decoder1 = null;
			iqSource1 = null;
			decoder1Thread = null;
			Config.passManager.setDecoder1(decoder1, iqSource1, this);			
		}
		if (decoder2 != null) {
			decoder2.stopProcessing(); // This blocks and waits for the audiosource to be done
			decoder2 = null;
			iqSource2 = null;
			decoder2Thread = null;
			Config.passManager.setDecoder2(decoder2, iqSource2, this);			
		}
		if (rfDevice != null)
			if (rfDevice instanceof AirspyDevice) {
				((AirspyDevice)rfDevice).stop();
				//rfDevice = null;
			}
		if (this.soundCardComboBox.getSelectedIndex() == SourceAudio.AIRSPY_SOURCE) {
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
			if (psk.isSelected()) iqSource1.setMode(SourceIQ.MODE_PSK);
			if (lowSpeed.isSelected()) iqSource1.setMode(SourceIQ.MODE_NFM);
			if (highSpeed.isSelected()) iqSource1.setMode(SourceIQ.MODE_FM);
			if (auto.isSelected()) iqSource1.setMode(SourceIQ.MODE_NFM);
		}
		if (iqSource2 != null) {
			if (auto.isSelected()) iqSource2.setMode(SourceIQ.MODE_FM);			
		}
	}
	
	private void enableSourceSelectionComponents(boolean t) {
		soundCardComboBox.setEnabled(t);
		cbSoundCardRate.setEnabled(t);
		highSpeed.setEnabled(t);
		lowSpeed.setEnabled(t);
		psk.setEnabled(t);
		int position = soundCardComboBox.getSelectedIndex(); 
		if (position == SourceAudio.FILE_SOURCE)
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
		}
		if (e.getSource() == rdbtnSquelchAudio) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
	            Config.squelchAudio=false;
	            //Config.save();
	        } else {
	        	Config.squelchAudio=true;
	        	//Config.save();
	        }
		}
		if (e.getSource() == rdbtnFilterOutputAudio) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
	            Config.filterOutputAudio=false;
	            //Config.save();
	        } else {
	        	Config.filterOutputAudio=true;
	        	//Config.save();
	        }
		}
		if (e.getSource() == rdbtnWriteDebugData) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.debugValues=false;
	            //Config.save();
	        } else {
	        	Config.debugValues=true;
	        	
	        	//Config.save();
	        }
		}
		/*
		if (e.getSource() == rdbtnApplyBlackmanWindow) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.applyBlackmanWindow=false;
	            //Config.save();
	        } else {
	        	Config.applyBlackmanWindow=true;
	        	
	        	//Config.save();
	        }
		}
		*/
		if (e.getSource() == rdbtnShowFFT) {
			if (fftPanel != null)
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				setFFTVisible(false);
	        } else {
	        	setFFTVisible(true);
	        	
	        }
		}
		/*
		if (e.getSource() == rdbtnUseNco) {
			if (fftPanel != null)
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				if (iqSource != null)
					SourceIQ.useNCO = false;
	        } else {
	        	if (iqSource != null)
	        		SourceIQ.useNCO = true;
	        	
	        }
		}
		if (e.getSource() == rdbtnUseLimiter) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.useLimiter=false;
	            //Config.save();
	        } else {
	        	Config.useLimiter=true;
	        	
	        	//Config.save();
	        }
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
		}
		
		if (e.getSource() == rdbtnTrackSignal) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.trackSignal=false;
	            //Config.save();
	        } else {
	        	Config.trackSignal=true;
	        	
	        	//Config.save();
	        }
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

		}
		
		if (e.getSource() == rdbtnWhenAboveHorizon) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				
	            Config.useDDEforFindSignal=false;
	            //Config.save();
	        } else {
	        	Config.useDDEforFindSignal=true;
	        	
	        	//Config.save();
	        }

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource() == this.peakLevel) {
			try {
			Config.SCAN_SIGNAL_THRESHOLD = Double.parseDouble(peakLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
		}

		if (e.getSource() == this.avgLevel) {
			try {
			Config.ANALYZE_SNR_THRESHOLD = Double.parseDouble(avgLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
		}

		if (e.getSource() == this.bitLevel) {
			try {
			Config.BIT_SNR_THRESHOLD = Double.parseDouble(bitLevel.getText());
			} catch (NumberFormatException n) {
				Log.errorDialog("Invalid Value", n.getMessage());
			}
		}

		
	}


}
