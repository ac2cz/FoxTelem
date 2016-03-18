package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import measure.PassMeasurement;
import measure.RtMeasurement;
import common.Config;
import common.Log;
import common.Spacecraft;

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
public class MyMeasurementsTab extends FoxTelemTab implements Runnable,
		ItemListener {
	DisplayModule[] satellite;
	DisplayModule[] passes;
	ArrayList<Spacecraft> sats;
	RtMeasurement rtMeasurement;
	PassMeasurement passMeasurement;
	JCheckBox cbUseDDEAzEl;
	JCheckBox cbUseDDEFreq;

	MyMeasurementsTab() {
		setLayout(new BorderLayout(0, 0));

		JPanel topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(10, 50));
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		JLabel lblId = new JLabel("Measurements captured at Ground Station "
				+ Config.callsign);
		lblId.setFont(new Font("SansSerif", Font.BOLD, 14));
		lblId.setForeground(textLblColor);
		topPanel.add(lblId);

		JPanel centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);

		sats = Config.satManager.getSpacecraftList();
		satellite = new DisplayModule[sats.size()];
		passes = new DisplayModule[sats.size()];
		for (int s = 0; s < sats.size(); s++) {

			Spacecraft fox = sats.get(s);
			satellite[s] = new DisplayModule(fox, fox.name, 9,
					DisplayModule.DISPLAY_MEASURES);
			centerPanel.add(satellite[s]);
			satellite[s].addName(1, "Bit Sig to Noise (-)",
					RtMeasurement.BIT_SNR, DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(2, "RF Sig to Noise (db)",
					RtMeasurement.RF_SNR, DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(3, "RF Power (dBm)", RtMeasurement.RF_POWER,
					DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(4, "Carrier Frequency (Hz)",
					RtMeasurement.CARRIER_FREQ, DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(5, "Azimuth (deg)", RtMeasurement.AZ,
					DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(6, "Elevation (deg)", RtMeasurement.EL,
					DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(7, "RS Erros", RtMeasurement.ERRORS,
					DisplayModule.DISPLAY_RT_ONLY);
			satellite[s].addName(8, "RS Erasures", RtMeasurement.ERASURES,
					DisplayModule.DISPLAY_RT_ONLY);
		}
		
		for (int s = 0; s < sats.size(); s++) {
			Spacecraft fox = sats.get(s);
			passes[s] = new DisplayModule(fox, fox.name + " passes", 9,
					DisplayModule.DISPLAY_PASS_MEASURES);
			centerPanel.add(passes[s]);
			passes[s].addName(1, "AOS",	PassMeasurement.AOS, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(2, "TCA",	PassMeasurement.TCA, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(3, "TCA Freq (Hz)",PassMeasurement.TCA_FREQ, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(4, "LOS", PassMeasurement.LOS, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(5, "Start Azimuth", PassMeasurement.START_AZIMUTH, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(6, "End Azimuth", PassMeasurement.END_AZIMUTH, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(7, "Max Elevation", PassMeasurement.MAX_ELEVATION, DisplayModule.DISPLAY_RT_ONLY);
			passes[s].addName(8, "Payloads Decoded", PassMeasurement.TOTAL_PAYLOADS, DisplayModule.DISPLAY_RT_ONLY);
		}
		
		JPanel bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

		cbUseDDEFreq = new JCheckBox("Read Freq from SatPC32",
				Config.useDDEforFreq);
		cbUseDDEFreq.setMinimumSize(new Dimension(100, 14));
		bottomPanel.add(cbUseDDEFreq);
		cbUseDDEFreq.addItemListener(this);
		cbUseDDEFreq.setEnabled(false);
		if (Config.isWindowsOs())
			cbUseDDEFreq.setEnabled(true);

		cbUseDDEAzEl = new JCheckBox("Read Az/El from SatPC32",
				Config.useDDEforAzEl);
		cbUseDDEAzEl.setMinimumSize(new Dimension(100, 14));
		bottomPanel.add(cbUseDDEAzEl);
		cbUseDDEAzEl.addItemListener(this);
		cbUseDDEAzEl.setEnabled(false);
		if (Config.isWindowsOs())
			cbUseDDEAzEl.setEnabled(true);

	}

	public void showGraphs() {
		for (DisplayModule mod : satellite) {
			if (mod != null)
				mod.showGraphs();
		}
	}

	public void openGraphs() {
		for (DisplayModule mod : satellite) {
			if (mod != null)
				mod.openGraphs();
		}
	}

	public void closeGraphs() {
		for (DisplayModule mod : satellite) {
			if (mod != null)
				mod.closeGraphs();
		}
	}

	@Override
	public void run() {
		running = true;
		done = false;
		boolean justStarted = true;

		while (running) {
			try {
				Thread.sleep(500); // refresh data once a second
			} catch (InterruptedException e) {
				Log.println("ERROR: Measurement thread interrupted");
				e.printStackTrace(Log.getWriter());
			}
			for (int s = 0; s < sats.size(); s++) {
				if (Config.payloadStore.getUpdatedMeasurement(sats.get(s).foxId)) {

					rtMeasurement = Config.payloadStore
							.getLatestMeasurement(sats.get(s).foxId);
					if (rtMeasurement != null) {
						Config.payloadStore.setUpdatedMeasurement(sats.get(s).foxId, false);

						double snr = GraphPanel.roundToSignificantFigures(
								rtMeasurement
										.getRawValue(RtMeasurement.BIT_SNR), 3);
						satellite[s].updateSingleValue(1, Double.toString(snr));
						double rfsnr = GraphPanel
								.roundToSignificantFigures(rtMeasurement
										.getRawValue(RtMeasurement.RF_SNR), 3);
						satellite[s].updateSingleValue(2,
								Double.toString(rfsnr));
						double power = GraphPanel
								.roundToSignificantFigures(rtMeasurement
										.getRawValue(RtMeasurement.RF_POWER), 3);
						satellite[s].updateSingleValue(3,
								Double.toString(power));
						long freq = (long) rtMeasurement
								.getRawValue(RtMeasurement.CARRIER_FREQ);
						satellite[s].updateSingleValue(4, Long.toString(freq));
						int az = (int) rtMeasurement
								.getRawValue(RtMeasurement.AZ);
						satellite[s].updateSingleValue(5, Integer.toString(az));
						int el = (int) rtMeasurement
								.getRawValue(RtMeasurement.EL);
						satellite[s].updateSingleValue(6, Integer.toString(el));
						int err = (int) rtMeasurement
								.getRawValue(RtMeasurement.ERRORS);
						satellite[s]
								.updateSingleValue(7, Integer.toString(err));
						int erase = (int) rtMeasurement
								.getRawValue(RtMeasurement.ERASURES);
						satellite[s].updateSingleValue(8,
								Integer.toString(erase));
					}
				}
				if (Config.payloadStore.getUpdatedPassMeasurement(sats.get(s).foxId)) {
					passMeasurement = Config.payloadStore.getLatestPassMeasurement(sats.get(s).foxId);
					if (passMeasurement != null) {
						Config.payloadStore.setUpdatedPassMeasurement(sats.get(s).foxId, false);

						passes[s].updateSingleValue(1, passMeasurement.getStringValue(PassMeasurement.AOS));
						passes[s].updateSingleValue(2, passMeasurement.getStringValue(PassMeasurement.TCA));
						passes[s].updateSingleValue(3, passMeasurement.getStringValue(PassMeasurement.TCA_FREQ));
						passes[s].updateSingleValue(4, passMeasurement.getStringValue(PassMeasurement.LOS));
						passes[s].updateSingleValue(5, passMeasurement.getStringValue(PassMeasurement.START_AZIMUTH));
						passes[s].updateSingleValue(6, passMeasurement.getStringValue(PassMeasurement.END_AZIMUTH));
						passes[s].updateSingleValue(7, passMeasurement.getStringValue(PassMeasurement.MAX_ELEVATION));
						passes[s].updateSingleValue(8, passMeasurement.getStringValue(PassMeasurement.TOTAL_PAYLOADS));
					}
				}
			}
			if (justStarted) {
				openGraphs();
				justStarted = false;
			}

		}
		done = true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();

		if (source == cbUseDDEFreq) {

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useDDEforFreq = false;
			} else {
				Config.useDDEforFreq = true;
			}
		}
		if (source == cbUseDDEAzEl) {

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				Config.useDDEforAzEl = false;
			} else {
				Config.useDDEforAzEl = true;
			}
		}
	}
}
