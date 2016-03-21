package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import telemetry.BitArrayLayout;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRtValues;

import java.awt.Dimension;

import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.BevelBorder;

import common.Config;
import common.Log;
import common.Spacecraft;

import java.awt.Color;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

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
public class HealthTab extends ModuleTab implements ItemListener, ActionListener, Runnable {
	
	JPanel centerPanel;
	JPanel bottomPanel;
	
	JLabel lblId;
	JLabel lblIdValue;
	JLabel lblMode;
	JLabel lblModeValue;
	JLabel lblResets;
	JLabel lblResetsValue;
	JLabel lblMaxResets;
	JLabel lblMaxResetsValue;
	JLabel lblMinResets;
	JLabel lblMinResetsValue;
	JLabel lblUptime;
	JLabel lblUptimeValue;
	JLabel lblMaxUptime;
	JLabel lblMaxUptimeValue;
	JLabel lblMinUptime;
	JLabel lblMinUptimeValue;
	JLabel lblFramesDecoded;
	JLabel lblFramesDecodedValue;
	JLabel lblCaptureDate;
	JLabel lblCaptureDateValue;
	
	JCheckBox showRawValues;
	JCheckBox showUTCtime;
	PayloadRtValues realTime; // the RT payload we are currently displaying
	PayloadMaxValues maxPayload; // the max payload we are currently displaying
	PayloadMinValues minPayload; // the min payload we are currently displaying
		
	private static final String ID = "Satellite ";
	private static final String MODE = "  Mode: ";
	private static final String UPTIME = "  Uptime: ";
	private static final String RESETS = "  Resets: ";
	private static final String DECODED = "Telemetry Payloads Decoded: ";
	private static final String CAPTURE_DATE = "Captured: ";
	
	
	public HealthTab(Spacecraft f) {
		fox = f;
		foxId = fox.foxId;
		setLayout(new BorderLayout(0, 0));
		
		JPanel topPanel = new JPanel();
		JPanel topPanel1 = new JPanel();
		JPanel topPanel2 = new JPanel();
		topPanel.setMinimumSize(new Dimension((int)(Config.displayModuleFontSize * 10/11), 50));
		add(topPanel, BorderLayout.NORTH);
		
		topPanel.setLayout(new BorderLayout(0,0));
		topPanel.add(topPanel1, BorderLayout.NORTH);
		topPanel.add(topPanel2, BorderLayout.SOUTH);
		
		topPanel1.setLayout(new BoxLayout(topPanel1, BoxLayout.X_AXIS));
		topPanel2.setLayout(new BoxLayout(topPanel2, BoxLayout.X_AXIS));
		
		lblId = new JLabel(ID);
//		lblId.setMaximumSize(new Dimension(280, 14));
//		lblId.setMinimumSize(new Dimension(280, 14));
		lblId.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblId.setForeground(textLblColor);
		topPanel1.add(lblId);
		lblIdValue = new JLabel();
		//lblIdValue.setMaximumSize(new Dimension(280, 14));
		//lblIdValue.setMinimumSize(new Dimension(280, 14));
		lblIdValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblIdValue.setForeground(textColor);
		topPanel1.add(lblIdValue);

		
		lblMode = new JLabel(MODE);
		lblMode.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblMode.setForeground(textLblColor);
		topPanel1.add(lblMode);
		lblModeValue = new JLabel();
		lblModeValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblModeValue.setForeground(textColor);
		topPanel1.add(lblModeValue);
		
		// force the next labels to the right side of screen
		int fonth = (int)(Config.displayModuleFontSize * 14/11);
		topPanel1.add(new Box.Filler(new Dimension(14,fonth), new Dimension(1600,fonth), new Dimension(1600,fonth)));

		lblFramesDecoded = new JLabel(DECODED);
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, fonth));
		lblFramesDecoded.setBorder(new EmptyBorder(5, 2, 5, 5) );
		lblFramesDecoded.setForeground(textLblColor);
		topPanel1.add(lblFramesDecoded);
		lblFramesDecodedValue = new JLabel();
		lblFramesDecodedValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblFramesDecodedValue.setBorder(new EmptyBorder(5, 2, 5, 5) );
		lblFramesDecodedValue.setForeground(textColor);
		topPanel1.add(lblFramesDecodedValue);
		
		lblResetsValue = addReset(topPanel2, "Last Realtime:");
		lblUptimeValue = addUptime(topPanel2, "");

		lblMaxResetsValue = addReset(topPanel2, "Max:");
		lblMaxUptimeValue = addUptime(topPanel2, "");

		lblMinResetsValue = addReset(topPanel2, "Min:");
		lblMinUptimeValue = addUptime(topPanel2, "");
		
		centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		centerPanel.setBackground(Color.DARK_GRAY);
		
		initDisplayHalves(centerPanel);
		
		bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		
		showRawValues = new JCheckBox("Display Raw Values", Config.displayRawValues);
		showRawValues.setMinimumSize(new Dimension(100, fonth));
		//showRawValues.setMaximumSize(new Dimension(100, 14));
		bottomPanel.add(showRawValues );
		showRawValues.addItemListener(this);

		showUTCtime = new JCheckBox("Display UTC Time", Config.displayUTCtime);
		bottomPanel.add(showUTCtime );
		showUTCtime.addItemListener(this);
		
		// force the next labels to the right side of screen
		bottomPanel.add(new Box.Filler(new Dimension(14,fonth), new Dimension(400,fonth), new Dimension(1600,fonth)));
		
		lblCaptureDate = new JLabel(CAPTURE_DATE);
		lblCaptureDate.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 10/11)));
		lblCaptureDate.setBorder(new EmptyBorder(5, 2, 5, 10) ); // top left bottom right
		lblCaptureDate.setForeground(textLblColor);
		bottomPanel.add(lblCaptureDate );
		
		BitArrayLayout rt = fox.rtLayout;
		BitArrayLayout max = fox.maxLayout;
		BitArrayLayout min = fox.minLayout;

		try {
			analyzeModules(rt, max, min, DisplayModule.DISPLAY_ALL);
		} catch (LayoutLoadException e) {
			Log.errorDialog("FATAL - Load Aborted", e.getMessage());
			e.printStackTrace(Log.getWriter());
			System.exit(1);
		}
	}

	private JLabel addReset(JPanel topPanel2, String type) {
		JLabel lblResets = new JLabel(type + " " + RESETS);
		lblResets.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
//		lblResets.setMinimumSize(new Dimension(200, 14));
//		lblResets.setMaximumSize(new Dimension(200, 14));
		lblResets.setForeground(textLblColor);
		topPanel2.add(lblResets);
		JLabel lblResetsValue = new JLabel();
		lblResetsValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblResetsValue.setForeground(textColor);
		topPanel2.add(lblResetsValue);
		return lblResetsValue;
	}
	private JLabel addUptime(JPanel topPanel2, String type) {
		int fonth = (int)(Config.displayModuleFontSize * 14/11);
		JLabel lblUptime = new JLabel(UPTIME);
		lblUptime.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblUptime.setForeground(textLblColor);
		topPanel2.add(lblUptime);
		JLabel lblUptimeValue = new JLabel();
		lblUptimeValue.setFont(new Font("SansSerif", Font.BOLD, fonth));
		lblUptimeValue.setMinimumSize(new Dimension(1600, fonth));
		lblUptimeValue.setMaximumSize(new Dimension(1600, fonth));
		lblUptimeValue.setForeground(textColor);
		topPanel2.add(lblUptimeValue);
		return lblUptimeValue;
	}
	
	private void displayUptime(JLabel lblUptimeValue, long u) {
		lblUptimeValue.setText("" + u);
	}

	private void displayResets(JLabel lblResetsValue, int u) {
		lblResetsValue.setText("" + u);
	}

	private void displayMode(int u) {
		if (u == 1)
			lblModeValue.setText("SAFE");
		else
			lblModeValue.setText("TRANSPONDER");
	}

	/**
	 * Given the Fox ID, display the actual number of the spacecraft
	 * @param u
	 */
	private void displayId(int u) {
		String id = "??";
		id = fox.toString() + "(" + Spacecraft.models[fox.model] + ")";
		lblIdValue.setText(id);
	}

	private void displayFramesDecoded(int u) {
		lblFramesDecodedValue.setText(Integer.toString(u));
	}
	
	private void displayCaptureDate(String u) {
		//SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
		//SimpleDateFormat df2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
		//df2.setTimeZone(TimeZone.getTimeZone("UTC"));  // messing with timezones here does not work.... not sure why.
		//df2.setTimeZone(TimeZone.getDefault());
		    Date result = null;
		    String reportDate = null;
			try {
				FramePart.fileDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				result = FramePart.fileDateFormat.parse(u);
				if (showUTCtime.isSelected())
					FramePart.reportDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				else 
					FramePart.reportDateFormat.setTimeZone(TimeZone.getDefault());
				reportDate = FramePart.reportDateFormat.format(result);
				
			} catch (ParseException e) {
				reportDate = "unknown";				
			} catch (NumberFormatException e) {
				reportDate = "unknown";				
			} catch (ArrayIndexOutOfBoundsException e) {
				reportDate = "unknown";
			}
			
			lblCaptureDate.setText(CAPTURE_DATE + reportDate);
	}
	
	public void updateTabRT(PayloadRtValues rt) {
		
	//	System.out.println("GOT PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
	
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateRtValues(rt);
		}
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateRtValues(rt);
		}
		displayId(rt.getFoxId());
		displayUptime(lblUptimeValue, rt.getUptime());
		displayResets(lblResetsValue, rt.getResets());
		displayCaptureDate(rt.getCaptureDate());
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
	}

	
	
	public void updateTabMax(PayloadMaxValues max) {
		
	//	System.out.println("GOT MAX PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");
	
		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateMaxValues(max);
		}
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateMaxValues(max);
		}
	
		displayId(max.getFoxId());
		displayUptime(lblMaxUptimeValue, max.getUptime());
		displayResets(lblMaxResetsValue, max.getResets());
		displayCaptureDate(max.getCaptureDate());
		displayMode(max.getRawValue("SafeModeIndication"));
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
	}

	public void updateTabMin(PayloadMinValues min) {
		
	//	System.out.println("GOT MIN PAYLOAD FROM payloadStore: Resets " + rt.getResets() + " Uptime: " + rt.getUptime() + "\n" + rt + "\n");

		for (DisplayModule mod : topModules) {
			if (mod != null)
			mod.updateMinValues(min);
		}
		for (DisplayModule mod : bottomModules) {
			if (mod != null)
			mod.updateMinValues(min);
		}
	
		displayId(min.getFoxId());
		displayUptime(lblMinUptimeValue, min.getUptime());
		displayResets(lblMinResetsValue, min.getResets());
		displayCaptureDate(min.getCaptureDate());
		displayMode(min.getRawValue("SafeModeIndication"));
		displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
	}	
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub	
	}

	
	@Override
	public void run() {
		running = true;
		done = false;
		boolean justStarted = true;
		while(running) {
			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: HealthTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 	
			
			if (Config.displayRawValues != showRawValues.isSelected()) {
				showRawValues.setSelected(Config.displayRawValues);
			}
			if (foxId != 0 && Config.payloadStore.initialized()) {
				if (Config.payloadStore.getUpdatedMax(foxId)) {
					maxPayload = Config.payloadStore.getLatestMax(foxId);
					if (maxPayload != null)
						updateTabMax(maxPayload);
					Config.payloadStore.setUpdatedMax(foxId, false);
					
				}
				if (Config.payloadStore.getUpdatedMin(foxId)) {
					minPayload = Config.payloadStore.getLatestMin(foxId);
					if (minPayload != null)
						updateTabMin(minPayload);
					Config.payloadStore.setUpdatedMin(foxId, false);
					
				}

				// Read the RealTime last so that at startup the Captured Date in the bottom right will be the last real time record
				if (Config.payloadStore.getUpdatedRt(foxId)) {
					realTime = Config.payloadStore.getLatestRt(foxId);
					if (realTime != null) {
						updateTabRT(realTime);
						//System.out.println("UPDATED RT Data: ");
					} else {
						//System.out.println("NO new RT Data: ");

					}
					Config.payloadStore.setUpdatedRt(foxId, false);
					if (justStarted) {
						openGraphs();
						justStarted = false;
					}
				}
				
				
				MainWindow.setTotalDecodes();

			}
			//System.out.println("Health tab running: " + running);
		}
		done = true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if (source == showRawValues) { //updateProperty(e, decoder.flipReceivedBits); }

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayRawValues = false;
			} else {
				Config.displayRawValues = true;
			}
//			Config.save();
			if (realTime != null)
				updateTabRT(realTime);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
		}
		if (source == showUTCtime) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.displayUTCtime = false;
			} else {
				Config.displayUTCtime = true;
			}
//			Config.save();
			if (realTime != null)
				updateTabRT(realTime);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
		}
	}
}
