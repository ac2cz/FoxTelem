
import gui.InitalSettings;
import gui.MainWindow;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.swing.JFrame;
import javax.swing.UIManager;

import common.Config;
import common.Log;
import decoder.Decoder;

/**
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
 * Version 0.03
 * Pause output when no audio
 * 
 * Version 0.04
 * Better clock recovery by re-processing the current window with the new clock
 * 
 * Version 0.05
 * 400 Hz Low Pass filter and automatic gain control
 *
 * Version 0.06
 * Fixed crash in the bit decoder when the slow speed frame printed out but not populated
 * 
 * Version 0.07
 * Assume SYNC marker in place unless missed several times
 * Flip the received bits
 * 
 * Version 0.08
 * Check for two valid SYNC markers the right distance apart.  Avoids noise and decodes more reliably.
 * Added properties file
 * Decodes what appear to be long frames that are missing the first SYNC word, if a valid header can be found 
 * just after where the SYNC word should be
 * Decodes double length frames that are missing the middle SYNC by inserting a SYNC word
 * 
 * Version 0.09
 * Frames updated to move the 2 bit field to the end.  Frame length defaulted to 970 bits
 * flipReceivedBits defaulted to false - this was likely a recording issue, not an issue with FOX1
 * 
 * Version 0.10
 * Show the user when audio is too quiet and when it is loud enough through the soundcard
 * Implemented KA9Q RSdecoder to correct errors - needs more testing.  Can be turned on/off in properties file
 * 
 * Version 0.11
 * Filter changed to RaisedCosine filter with corner freq of 200Hz.  This seems to give the best shaped bits.  I tested
 * with corner freqs of 100 and 400 as well.  No decode and cosine shaped bits at 100.  Little difference to IIR LPF with 400.
 * Raised Cosine is considerably slower to execute than IIR.  May need to make it a configuration option
 * 
 * Version 0.12
 * Implemented RS Erasures for invalid 10b words.  If a 10b word is not valid (-1) then an erasure is passed to the RS Decoder
 * indicating the position of the bad word.
 * Reject corrupted frames if they fail the RS decode.
 * Added debugFrames to the properties file
 * 
 * Version 0.13
 * Do not attenuate signal.  If signal is above threshold, then pass through unchanged.  Prompt user that level is too loud if needed
 * Added -help
 * Tuned parameters for better decoding of difficult signals
 * 
 * Version 0.14
 * Changed internal storage of bits in the bitStream to boolean values
 * Initial implementation of the high speed decoder
 * 100 Hz Raised Cosine Filter set as Default
 * 
 * Version 0.15
 * Test GUI added for analysis of the signals
 * 
 * Version 0.16
 * Bug fixes to the GUI, including play back of monitored audio
 * 
 * Version 0.17 = revision 2355 in SVN
 * Fixed bug where CPU usage was over 50% even when idle
 * Auto-detect if received bits are flipped
 * Bit detection algorithm using distance from last bit
 * Fixed bugs with selection of audio sources and sound cards
 * 
 * Version 0.18
 * Implemented simple local storage of captured frames and analysis graphs
 * Display screen for captured frames
 * Simplified source screen
 * Defaulted sound card to 44100
 * 
 * Version 0.19
 * Fixed bug when sound card reselected and "line not available"
 * Removed error message for filter SAMPLE_RATE
 * 
 * Version 0.20
 * Fixed bug that prevented VAC from working.  Needed to close the audio line after testing if it was suitable,
 * otherwise it was not available when needed.  Also needed to include the description with the name, because VAC
 * names its devices the same for input and output.
 * Fixed bug where RSSI lookup using extrapolation returned NaN
 * 
 * Version 0.21
 * Store the sample rate in the config file
 * Store the last sound card used as source and sink in the config file
 * Show part of graphs by specifying a start reset / uptime
 * Skip long sections of uptime that have no data when plotting graphs unless user specifies that all uptime should be plotted
 * Changed dates to UTC
 * Fixed minor display bugs for spacecraft parameters
 * 
 * Version 0.22
 * Conversion routines for SPIN
 * Conversion routines for IHUDiagnostic, HardError, SoftError
 * Updated telemetry save file format (unfortunately) to add datestamp and SPIN format
 * Fixed a bug in the TXPACurrent calculation.  Fixed a bug in the solar panel temperature lookup.
 * Battery current multiplied by 1000 to put it in mA
 * Removed options that should not be changed
 * 
 * Version 0.23
 * Fixed bugs that prevented execution on the Mac including file separator and Device list size
 * 
 * Version 0.24
 * Fixed bug in graph display where search params were not updated when tab used to move between fields
 * Fixed a bug where audio data was not reset correctly and we decoded slightly fewer frames in the regression test
 * Added config setting for save to payloadStore - to enable repeat tests of the same Wav file
 * Fixed formatting bugs on source tab and added wav file progress bar
 * Reduced number of allowed RS Erasures to 16
 * 
 * Version 0.25
 * Fixed bug in the capture date where it was stored in 24Hr format and parsed as 12hr format
 * 
 * Version 0.26
 * Shortened radiation frames max length from 60 to 58.  Two empty bytes were printed at the end of the records when saved to file.
 * Added error handling for exceptions that should be evident in the GUI
 * Fixes for High Speed decode of bit files
 * 
 * Version 0.27
 * Fixed bug where radiation bytes written to file as hex
 * Added logging
 * 
 * Version 0.28
 * Fixed bug where error is generated when decode from a wav file is stopped
 * Support for Mono wave files debugged
 * Fixed bug where stereo data copied incorrectly in the filter
 * Fixed bug where DC elimination did not work correctly, but defaulted DC elimination to off
 * Switched to calculation of filter coefficients at run time.  Default set to WindowedSinc 200 Hz filter
 * 
 * Version 0.29
 * Write the radiation data to the file in Hex again
 * Added task number to the decode of hard error
 * Experimental display of the radiation bytes on a separate tab
 * 
 * Version 0.30
 * Fixed divide by zero error in bit detection algorithm
 * 
 * Version 0.31
 * Display the name of the ihuTask when HardError is decoded
 * Decode the STATE and other parts of the radiation packets
 * 
 * Version 0.32
 * Fixed bug in RS decoder initialization
 * Restructured the bitStream to cope with the volume of data from highspeed
 * Fixed RS Decoding for High Speed frames
 * Set the default high speed frame length to 5273 bytes
 * 
 * Version 0.33
 * Fixed a bug where the RsCodeword overran if the frame length was wrong
 * Fixed a bug where the circular bit buffer overflowed
 * Printed out first radiation data in the high speed frame
 * Fixed gain issue with raised cosine filter
 * Displayed filter params
 * Fixed bug where audio could be heard after decoder stopped
 * Disabled filters for high speed
 * Suspended decode of VUC data packets until code is debugged
 * 
 * Version 0.34
 * Stored high speed payloads in the payload storage
 * Decode Radiation packets
 * 
 * Version 0.35
 * Fixed bug where the JfileChooser did not have focus because the JcomboxBox was still open
 * Store duplicate radiation frames with the same reset and uptime
 * Fixed bug where diagnostic, hard and soft error "graph" windows did not update in real time
 * Fixed a bug payload numbers did not update correctly on the Health Tab
 * Only add radiation packets that span across frames if the next packet is also the next in the sequence.  Otherwise reject as corrupt
 * 
 * Version 0.36
 * Make sure COBS decoder throws an exception if the packet overruns and write a message to the log
 * Updates to debugging output for debugBits and debugClock
 * Slight improvement to clock recovery because the default value is set to the last sample of the previous bit and not
 * the first sample of the current bit.  Clock Recovery Zero threshold set tighter for high speed.
 * Fixed bug in gain calculation for Windowed Sinc filter
 * Show total payloads decoded on the source tab
 * Added Eye diagram
 * 
 * Version 0.37
 * Fixed null pointer exception in eye diagram
 * 
 * Version 0.38
 * FTP Files to test location every 10 mins
 * 
 * Version 0.39
 * Fixed label for I2C2 in diagnostic window
 * Added spacecraft panel to selected different params for each model
 * Fixed code that showed warnings during compile
 * Added additional error messages to FTP routine and no-op to prevent timeouts
 * Added File menu option to delete the Payload files
 * Added FM and Spare lookup tables for VBATT/2
 * Display VBATT/2 instead of Battery module telemetry when FM and FS selected
 * 
 * Version 0.40
 * Graph formatting updated.  Graph preferences stored
 * Mac and Linux formatting updated
 * 
 * Version 0.41
 * No longer hard coded the FoxId to 1 in the displayModule
 * Graphs for Status Bits and Antenna have correct axis values
 * Rebuild the audio source lists when the combo box is clicked
 * Added settings screen with font sizes for graphs and display modules
 * 
 * Version 0.42
 * Fixed issue where zero level of graph was sometimes drawn in the wrong place
 * Moved decoder options to settings screen
 * Added log file path to settings screen
 * Allow loading of wav files from the File menu
 * Fixed issue with label on graph just below the axis
 * Display Hard/Soft errors in tables rather than as text
 * Added save to CSV file
 * Added Reset button for graph queries
 * Added copy/paste of graphs to the system clipboard
 * 
 * Version 0.43
 * Bug fixes to 0.42
 * Draft IQ demodulation and libusb support for FCD
 * 
 * Version 0.44
 * Server upload of raw frames
 * Bug fixes for IQ demodulation
 * HID interface for the FCD.
 * Fixes to Mac and Linux fcd interface code
 * 
 * Version 0.45
 * Multi-satellite support
 * Bug fixes
 * Settings screen re-factored and cleaned up
 * 
 * Version 0.46
 * Camera Picture decode, storage on disk as Jpegs and Display
 * 
 * Version 0.47
 * Fixed minor bug when bits purged too frequently in HS frame
 * Better debug output for Camera
 * Refresh images as they are downloading
 * 
 * Version 0.48
 * Fixed bug where decoder crashed on startup if the images had all been manually deleted
 * Track latest image as it is down-loaded (if user wants that)
 * Fixed formatting of error tables
 * Use Boolean values for Safe Mode bits
 * Decode the last 8 bits in the Gyro Diagnostics as other diagnostic items for the camera
 * Updated hard error values for ErrorCode and added Alignment as an additional field
 * Fixed bug where graph axis labels were not drawn across the whole axis
 * Add reset/uptime to camera thumbnail images and fixed data format
 * 
 * Version 0.49
 * Read jpeg header from the spacecraft directory
 * Add picture counter to the thumbnails
 * Bug fix for tick display on graphs
 * 
 * Version 0.50
 * IQ Release - circular buffers and updated FM demodulation
 * High Speed decode possible with IQ decoder
 * Tuning with the arrow keys
 * Filter IF with Tukey filter (vs Blackman) and do not populate DC bins
 * Fixed bugs in FFT overlap add and offset
 * 
 * Version 0.51
 * Insert the JPEG Restart markers and the JPEG Footer when VT Camera images received
 * 
 * Version 0.52
 * Warn the user when debugBits is set
 * Added MPPT conversion
 * Better formatting of graph labels on Uptime axis
 * Added GPL to Help>About and linked the manual file
 * Drafted manual and linked as a PDF file
 * Allow decode from arbitrary IQ source
 * Warn user if soundcard can not read data fast enough
 * Fixed bug where total decodes was corrupted sometimes
 * Remember if audio is being monitored when the program restarts
 * Fixed bug where log file is filled with too many error messages
 * Fixed bug where eye panel display contained noise
 * Capture bit SNR and store as a measurement
 * Ask for log file directory at startup
 * Track drifting frequency in IQ mode
 * Capture signal strength and frequency in IQ mode and store as measurement.  Include in STP headers
 * 
 * Version 0.53
 * Support the Original FCD and other 96000 sample rate IQ devices (not tested because I have the pro plus)
 * Formatting of the input tab and main window
 * Allow Maidenhead locators to be entered and converted to lat/long
 * Added stereo channel to settings
 * Fixed Mac bug where HTML formatting was incorrect
 * FFT panel formatting
 * Rename jpeg header so it is Mac compatible
 * Fixed bug where layout load errors not caught
 * Load the radiation layout from 2 different files for the 2 formats
 * Fixed bug where errorDialogs did not work when logging disabled
 * Fixed issue where eye panel froze when shifting from HS to LS
 * Fixed issue where Font too large on Help About screen on the Mac
 * Added diagnostics for uncaught exceptions in threads
 * Resize the picture in the cameraTab so that it fits
 * Improve Mac fileChooser
 * 
 * Version 0.54
 * Catch uncaught exceptions in the Event Dispatch Thread (EDT)
 * Store RF SNR
 * Capture Az and El from SatPC32 if it is running
 * MPPT current sense should be in mA and not A.
 * Fixed bug where RS Erasures overflowed and decoder crashed
 * Fixed bug where decoder overflowed the audio buffer when idle in high speed mode
 * 
 * Version 0.55
 * Added Fox-1D to the spacecraft directory
 * Fixed bug where filters were not remembered or updated in the GUI
 * 
 * Version 0.56
 * Send to server using UDP
 * Fixed bug where high speed frames were not sent to the server
 * Server upload fixes so that empty data fields are not sent, e.g. Frequency
 * No longer write data to the installation folder.  Save properties in user home directory
 * Display both formats for Radiation data
 * Make sure the Fcd is released when it is no longer selected
 * Fixed bug where the RF SNR was 10dB too high.
 * Fixed bug where "show latest image" sometimes did not work
 * Support much better formatting of big fonts on tabs for people with poor eyes
 * Set the FCD Pro to 96000 when it is selected
 * Use the local platform line.separator in the log file
 * Fixed bug where measured carrier Frequency was not graphed correctly
 * Fixed divide by zero error when wrong rate chosen for IQ decode
 * Fixed bug where frequency was recorded incorrectly if FFT bin was greater than FFT_SAMPLES/2
 * Stopped allocating new memory in the decoder to avoid audio glitches and to reduce memory footprint
 * Fixed bug where measurements were plotted backwards when a reset/uptime was given as the start of the graph
 * Removed conditions where decoder was not stopped before new decoder started, causing buffer read issues
 * Detect the current directory in a way that also works on Linux (hopefully)
 * Fixed bug where SYNC words were added multiple times when a double length frame was detected
 * Fixed bug where decoder tried to process the same bogus frames over and over (when receiving noise)
 * Fixed bug where a single audio byte could be skipped, corrupting the audio stream
 * 
 * Version 0.57
 * Open graph windows that were previously open
 * Fixed bug where Mac could not find the currentDirectory
 * 
 * Version 0.58
 * Fixed bug where reselecting the FCD complained that it could not be opened
 * Stopped the FCD defaulting other sound cards to 192000
 * Fixed bug where canceling out of selecting a wav file caused the program to crash
 * Added event listener to the sample_rate pull down so it is reliably saved to the properties file
 * 
 * Version 0.59
 * Automatically find the signal if it is in the expected frequency range for the satellite we are tracking
 * Store the errors and erasures as a measurement
 * Add warning to the manual about Windows Smart Screen and Java versions on Mac
 * Squelch the audio when SNR of bits is low - note that his has strange behavior with wav file playback, but works well with live data
 * Default the audio monitor to ON at installation
 * Fixed bug where RF SNR was calculated slightly wrong
 * 
 * Version 0.59b
 * Fixed bug where frames sent to the wrong port on the server.
 * Bug where filtered audio was the default
 * Defaulted primary and secondary server for testing
 *
 * Version 0.60
 * Fixed bug where IQ decoder could corrupt a byte in the circular buffer
 *
 * Version 0.61
 * Reduce slightly the processing delay before audio is heard
 * Implemented the MacOs preferences, about and quit menus
 * Minor fixes to text in HelpAbout. Added AA2TX
 * Fixed bug where raw radiation data was listed incorrectly on the radiation tab
 * Don't save properties until exit, to prevent the file being locked
 * Enable the red X on the settings screen
 * Save measurement to file in the background (like telemetry) to avoid any audio glitches
 * Removed Fox Tail Freq from measurements. Will go in PassMeasurements - this will corrupt any saved measurements - sorry!
 * Added ERRORS and ERASURES back to the Spacecraft files so that they are logged
 * Fixed formatting errors for large fonts on graphs
 * Print error message if the manual can not be found
 * Added 12 sample running average filter to the graphs
 * Removed Popup warning when audio missed.  No displayed in footer
 * 
 * Version 0.62
 * Fixed bug where picking an invalid directory caused a crash
 * Fixed bug where running avg for short graphs caused a crash
 * Use the native Mac OSX file chooser on the Mac.  Also use where possible on other platforms.
 * Write TCA at the end of a pass, if possible  (this is not yet uploaded to the server)
 * 
 * Version 1.00
 * HelpAbout now shows the Fox-1 Engineering team (vs 1A)
 * Fixed bug where title of graph could clash with horizontal axis when drawn at the top
 * Changed reset icon on graphs
 * Changed the name of the Radiation Experiments to "Vanderbilt Radiation" from Vulcan 
 * Prevent passMeasurement graphs from displaying because they do not display meaningful data
 * Prevent an incorrect directory from being chosen from the settings or initial settings screens
 * Formatted Livetime correctly on the Vanderbilt Radiation tab
 * Indicated the reset/uptime for the last min/max payloads on the health tab
 * Help about says "Radio Amateur .. " vs just "Amateur"
 * Bug fixes to the find signal algorithm
 * Remember the position of the horizontal divider on the Vanderbilt tabs
 * Display something sensible on the HERCI tab (but this does not include the 1D branch yet)
 * Fixed bug where graphs could not be opened for a second time on MacOs and Linux
 * 
 * Version 1.00a
 * Tweak the position of the graph header and save some space on small graphs
 * Changed Fox-1C to Fox-1Cliff
 * 
 * Version 1.01
 * Send the frames to both primary and secondary server
 * Set the default fcd frequency to 145930 so that Fox-1A, Fox-1Cliff and Fox-1D will be in the passband
 * Allow use to select UDP or TCP for upload to the server and save in settings
 * 
 */

public class FoxTelemMain {

	static Decoder decoder;
	public static String HELP = "AMSAT Fox Telemetry Decoder. Version " + Config.VERSION +"\n\n"
			+ "Usage: FoxTelem [-version] [fileName.wav]\n\n";
		
	public static void main(String[] args) {
		
		FoxTelemMain m = new FoxTelemMain();
		if (Config.missing()) {
			// Then this is the first time we have run FoxTelem on this computer
			Config.setHome();
			m.initialRun();
		}
		
		Log.init();
		
		Config.currentDir = System.getProperty("user.dir"); //m.getCurrentDir(); 
		
		Config.init(); // initialize, load properties and create the payload store.  This runs in a seperate thread to the GUI and the decoder
		Log.println("************************************************************");
		Log.println("AMSAT Fox 1A Telemetry Decoder. " + Config.VERSION + "\nIn: " + Config.currentDir);
		Log.println("************************************************************");
		Log.println("CurrentDir is:" + Config.currentDir);

		if (args.length > 0) {
			if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help")) || (args[0].equalsIgnoreCase("--help"))) {
				System.out.println(HELP);
				System.exit(0);
			}
			if (args[0].equalsIgnoreCase("-version")) {
				System.out.println("AMSAT Fox Telemetry Decoder. Version " + Config.VERSION);
				System.exit(0);
			}

		}

		invokeGUI();
	}

	public void initialRun() {
		
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {

				try { //to set the look and feel to be the same as the platform it is run on
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					// Silently fail if we can't do this and go with default look and feel
					//e.printStackTrace();
				}

				try {
					JFrame window = new JFrame();
					window.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/fox.jpg")));
					//window.setVisible(true);

					// INITIAL RUN
					InitalSettings f = new InitalSettings(window, true);
					f.setVisible(true);

				} catch (Exception e) {
					Log.println("SERIOUS ERROR - Uncaught and thrown from Initial Setup");
					e.printStackTrace();
					e.printStackTrace(Log.getWriter());
				}
	//		}
	//	});		
		
	}
	
	/**
	 * Start the GUI
	 */
	public static void invokeGUI() {
		
		// Need to set the apple menu property in the main thread.  This is ignored on other platforms
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		//System.setProperty("apple.awt.fileDialogForDirectories", "true");  // fix problems with the file chooser


		
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
	    System.setProperty("sun.awt.exception.handler",
	                       ExceptionHandler.class.getName());
		EventQueue.invokeLater(new Runnable() {
			public void run() {

				try { //to set the look and feel to be the same as the platform it is run on
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					// Silently fail if we can't do this and go with default look and feel
					//e.printStackTrace();
				}

				try {
					MainWindow window = new MainWindow();
					window.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/fox.jpg")));
					window.setVisible(true);
				} catch (Exception e) {
					Log.println("SERIOUS ERROR - Uncaught and thrown from GUI");
					e.printStackTrace();
					e.printStackTrace(Log.getWriter());
				}
			}
		});		
	}

	/**
	 * Not pretty, but this works out what directory the jar file is in and returns it
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getCurrentDir() {
		@SuppressWarnings("rawtypes")
		Class cls = this.getClass();
		ProtectionDomain pDomain = cls.getProtectionDomain();
		CodeSource cSource = pDomain.getCodeSource();
		URL loc = cSource.getLocation();
		String dir = loc.getPath();
		File f = new File(dir);
		String s = f.getParent();
		return s;
	}

	public static String[] consumeArg(String [] args) {
		String[] a = new String[args.length-1];
		for (int j=1; j< args.length; j++)
			a[j-1] = args[j];
		return a;
	}

	/**
	 * Inner class to handle excetions in the Event Dispatch Thread (EDT)
	 * @author chris.e.thompson
	 *
	 */
	public static class ExceptionHandler
	implements Thread.UncaughtExceptionHandler {

		public void handle(Throwable thrown) {
			// for EDT exceptions
			handleException(Thread.currentThread().getName(), thrown);
		}

		public void uncaughtException(Thread thread, Throwable thrown) {
			// for other uncaught exceptions
			handleException(thread.getName(), thrown);
		}

		protected void handleException(String tname, Throwable thrown) {
			thrown.printStackTrace();
			Log.errorDialog("SERIOUS GUI ERROR", "Exception on " + tname);
		}
	}

}


