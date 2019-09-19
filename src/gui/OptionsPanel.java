package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import decoder.HighSpeedBitStream;
import decoder.SlowSpeedBitStream;

import javax.swing.BoxLayout;

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
public class OptionsPanel extends JPanel implements ItemListener {

	//Options check boxes
	
	JCheckBox cbFtpFiles;
	JCheckBox cbFlipBits;
	JCheckBox debugFrames;
	JCheckBox debugFieldValues;
	JCheckBox debugCameraFrames;
	JCheckBox debugBits, debugBytes;
	JCheckBox debugValues;
	JCheckBox debugClock;
	JCheckBox debugGlitches;
	JCheckBox debugSignalFinder;
	JCheckBox debugCalcDopplerContinually;
	JCheckBox filterData;
	JCheckBox useRSfec;
	JCheckBox useRSerasures;
	JCheckBox squelchAudio;
	JCheckBox useAGC;
	JCheckBox recoverClock;
	JCheckBox realTimePlayback;
	JCheckBox useNativeFileChooser;
//	JCheckBox storePayloads;
	JCheckBox logging;
//	JCheckBox highSpeed;
	JLabel hsFrameLength;
	JLabel ssFrameLength;
	JLabel debugWavFile;
	JLabel debugCount;
	
	//Decoder decoder;
	
	OptionsPanel() {
		//decoder = d;
		TitledBorder title = new TitledBorder(null, "Debug Options", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		this.setBorder(title);
		initializeGui();
	}
	
	public void initializeGui() {

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//		cbFtpFiles = addCheckBox("Upload to Server", Config.uploadToServer );
//		cbFlipBits = addCheckBox("Flip Received Bits", Config.flipReceivedBits );
//		cbFlipBits.setEnabled(false);
//		filterData = addCheckBox("Filter Audio", Config.filterData );
	//	filterData.setEnabled(false);
	//	useRSfec = addCheckBox("RS Decoder", Config.useRSfec );
		//useRSfec.setEnabled(false);
	//	useRSerasures = addCheckBox("Use RS Erasures", Config.useRSerasures );
		//useRSerasures.setEnabled(false);
		logging = addCheckBox("Enable Logging", "Write debug information to a log file in the same folder as the decoded payloads", Log.getLogging() );
	//	recoverClock = addCheckBox("Recover Clock", Config.recoverClock );
		//recoverClock.setEnabled(false);
		debugFrames = addCheckBox("Debug Frames", "Print information about the decoded frames into the debug log", Config.debugFrames );
		debugFieldValues = addCheckBox("Debug Fields", "Write all of the decoded fields in the payloads to the debug log", Config.debugFieldValues );
//		debugCameraFrames = addCheckBox("Debug Camera Frames", "Write the entire contents of the camera frame to the debug log", Config.debugCameraFrames );
		debugBits = addCheckBox("Debug Bits", "Write very verbose debug information at the bit level", Config.debugBits );
		debugBytes = addCheckBox("Debug Bytes", "Write the hex bytes in a frame when it is decoded", Config.debugBytes );
		debugValues = addCheckBox("Debug Values", "Display Debug information for bit values on the audio screen", Config.debugValues );
		useRSfec = addCheckBox("Use RS FEC", "Use the RS Decoder", Config.useRSfec );
		debugClock = addCheckBox("Debug Clock", "Write clock changes to the debug log from the clock recovery algorithm", Config.debugClock );
//		storePayloads = addCheckBox("Store Payloads", Config.storePayloads );
//		highSpeed = addCheckBox("Decode 9k6", Config.highSpeed );
	//	useAGC = addCheckBox("Use AGC", Config.useAGC );
		debugGlitches = addCheckBox("Debug missed audio", "Write to debug log when significant audio is being missed from the soundcard", Config.debugAudioGlitches );
		debugSignalFinder = addCheckBox("Debug Find Signal", "Write debug to show the workings of the signal finder and the pass measurements", Config.debugSignalFinder );
		debugCalcDopplerContinually = addCheckBox("Debug (Calc) Doppler Continually", "Calculate doppler continually for debugging.  Calculates first sat in the priority order.", Config.debugCalcDopplerContinually );
		useNativeFileChooser = addCheckBox("Use Native File Chooser", "Use the OS native file chooser", Config.useNativeFileChooser );
		//squelchAudio = addCheckBox("Squelch Decoder", Config.squelchAudio );
		//realTimePlayback = addCheckBox("Slow Down Playback", Config.realTimePlaybackOfFile );
		ssFrameLength = new JLabel("DUV Frame Length: " + (SlowSpeedBitStream.SLOW_SPEED_SYNC_WORD_DISTANCE-10) + " bits");
		hsFrameLength = new JLabel("HS Frame Length: " + (HighSpeedBitStream.HIGH_SPEED_SYNC_WORD_DISTANCE-10) + " bits");
		add(ssFrameLength);
		add(hsFrameLength);
		if (Config.writeDebugWavFile) {
			debugWavFile = new JLabel("Writing to Debug Wav File");
			debugWavFile.setForeground(Color.RED);
			add(debugWavFile);
		}
		if (Config.DEBUG_COUNT != -1) {
			debugCount = new JLabel("Processing only: " + Config.DEBUG_COUNT + " windows");
			debugCount.setForeground(Color.RED);
			add(debugCount);
		}
		// min, pref, max - each is Hor, Vert
		this.add(new Box.Filler(new Dimension(0,0), new Dimension(100,0), new Dimension(1000,0)));

		//this.add(new Box.Filler(new Dimension(10,10), new Dimension(150,400), new Dimension(500,500)));
	}
	
	private JCheckBox addCheckBox(String name, String tip, boolean value) {
		JCheckBox checkBox = new JCheckBox(name);
		checkBox.setEnabled(true);
		checkBox.addItemListener(this);
		checkBox.setToolTipText(tip);
		add(checkBox);
		if (value) checkBox.setSelected(true); else checkBox.setSelected(false);
		return checkBox;
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == cbFtpFiles) { //updateProperty(e, decoder.flipReceivedBits); }
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.uploadToServer = false;
			} else {
				Config.uploadToServer = true;
				
				if (Config.callsign.equalsIgnoreCase(Config.DEFAULT_CALLSIGN) || Config.callsign.length() < 3) {
					String call = JOptionPane.showInputDialog(MainWindow.frame, "Please enter your callsign",
							"CALLSIGN", JOptionPane.QUESTION_MESSAGE);
					Config.callsign = call;
					cbFtpFiles.setSelected(true);
				}
				
			}
			Config.save();
			
		}
		
		if (source == cbFlipBits) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				//Config.flipReceivedBits = false;
			} else {
				//Config.flipReceivedBits = true;
			}
		}

		//if (source == filterData) { updateProperty(e, decoder.filterData); }
		if (source == filterData) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.filterData = false;
			} else {
				Config.filterData = true;
			}
			Config.save();
		}
		if (source == useRSfec) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useRSfec = false;
			} else {
				Config.useRSfec = true;
			}
			Config.save();
		}
		if (source == useRSerasures) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useRSerasures = false;
			} else {
				Config.useRSerasures = true;
			}
			Config.save();
		}
		if (source == logging) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Log.setLogging(false);
			} else {
				// Use InvokeLater to work around a known JAVA bug where a JOptionPane resets the checkbox
				SwingUtilities.invokeLater(new Runnable() {
		            public void run() {
				JOptionPane.showMessageDialog(getTopLevelAncestor(),
						"You have enabled logging, which will write debug information and erros to the log file.\n"
						+ "Over time the log file can grow large, especially if other debugOptions are set.  You\n"
						+ "should have a manual or automatic method to rotate the log files if you intend to leave\n"
						+ "logging enabled permanently.",
						"Logging Selected",
						JOptionPane.WARNING_MESSAGE) ;
		            }
		        });
				Log.setLogging(true);
			}
			Config.save();
		}

		if (source == recoverClock) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.recoverClock = false;
			} else {
				Config.recoverClock = true;
			}
			Config.save();
		}
		if (source == useAGC) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useAGC = false;
			} else {
				Config.useAGC = true;
			}
			Config.save();
		}
		if (source == debugGlitches) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugAudioGlitches = false;
			} else {
				Config.debugAudioGlitches = true;
			}
			Config.save();
		}
		if (source == debugSignalFinder) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugSignalFinder = false;
			} else {
				Config.debugSignalFinder = true;
			}
			Config.save();
		}
		if (source == debugCalcDopplerContinually) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugCalcDopplerContinually = false;
			} else {
				Config.debugCalcDopplerContinually = true;
			}
			Config.save();
		}
		if (source == useNativeFileChooser) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useNativeFileChooser = false;
			} else {
				Config.useNativeFileChooser = true;
			}
			Config.save();
		}
	/*
		if (source == storePayloads) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.storePayloads = false;
			} else {
				Config.storePayloads = true;
			}
			Config.save();
		}
		if (source == highSpeed) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.highSpeed = false;
			} else {
				Config.highSpeed = true;
			}
			Config.save();
		}
		*/
		if (source == debugFrames) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugFrames = false;
			} else {
				Config.debugFrames = true;
			}
			Config.save();
		}
		if (source == debugFieldValues) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugFieldValues = false;
			} else {
				Config.debugFieldValues = true;
			}
			Config.save();
		}
		if (source == debugCameraFrames) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugCameraFrames = false;
			} else {
				Config.debugCameraFrames = true;
			}
			Config.save();
		}
		if (source == debugClock) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugClock = false;
			} else {
				Config.debugClock = true;
			}
			Config.save();
		}
		
		if (source == debugBits) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugBits = false;
			} else {
				Config.debugBits = true;
				debugBits.setSelected(true);
				// Use InvokeLater to work around a known JAVA bug where a JOptionPane resets the checkbox
				SwingUtilities.invokeLater(new Runnable() {
		            public void run() {
				JOptionPane.showMessageDialog(getTopLevelAncestor(),
						"Bit level debugging causes significant quantities of information to be written\n"
						+ "to the disk.  The decoder typically can not keep up, especially in High Speed Mode.\n"
						+ "Use this setting only if it's critial that you see the bit level information to troubleshoot\n"
						+ "an issue.",
						"Bit Level Debugging Selected",
						JOptionPane.WARNING_MESSAGE) ;
		            }
		        });
				
			}
			Config.save();
		}
		if (source == debugBytes) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugBytes = false;
			} else {
				Config.debugBytes = true;
			}
		}
		if (source == debugValues) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.debugValues = false;
			} else {
				Config.debugValues = true;
			}
			Config.save();
		}
		if (source == squelchAudio) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.squelchAudio = false;
			} else {
				Config.squelchAudio = true;
			}
			Config.save();
		}
		if (source == realTimePlayback) { 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.realTimePlaybackOfFile = false;
			} else {
				Config.realTimePlaybackOfFile = true;
			}
			Config.save();
		}
	}


/*
	private void updateProperty(ItemEvent e, boolean value) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			value = false;
		} else {
			value = true;
		}
	}
	*/
}
