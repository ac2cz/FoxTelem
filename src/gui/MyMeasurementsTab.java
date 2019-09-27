package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import measure.PassMeasurement;
import measure.RtMeasurement;
import measure.SatMeasurementStore;
import common.Config;
import common.Log;
import common.Spacecraft;
import common.FoxSpacecraft;

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
	DisplayModule satellite;
	DisplayModule passes;
	Spacecraft sat;
	RtMeasurement rtMeasurement;
	PassMeasurement passMeasurement;

	MyMeasurementsTab(Spacecraft s) {
		sat = s;
		setLayout(new BorderLayout(0, 0));

		JPanel topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(10, 50));
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		JLabel lblId = new JLabel("Measurements captured at Ground Station "
				+ Config.callsign);
		lblId.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblId.setForeground(textLblColor);
		topPanel.add(lblId);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new WrapLayout(FlowLayout.LEADING, 25, 25));
		JScrollPane scrollPane = new JScrollPane (centerPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);

		if (sat.isFox1()) {
			FoxSpacecraft fox = (FoxSpacecraft) sat;
			satellite = new DisplayModule(fox, fox.user_display_name, 9,
					DisplayModule.DISPLAY_MEASURES);
			centerPanel.add(satellite);
			satellite.addName(1, "Bit Sig to Noise (-)",
					RtMeasurement.BIT_SNR, DisplayModule.DISPLAY_MEASURES);
			satellite.addName(2, "RF Sig to Noise (db)",
					RtMeasurement.RF_SNR, DisplayModule.DISPLAY_MEASURES);
			satellite.addName(3, "RF Power (dBm)", RtMeasurement.RF_POWER,
					DisplayModule.DISPLAY_MEASURES);
			satellite.addName(4, "Carrier Frequency (Hz)",
					RtMeasurement.CARRIER_FREQ, DisplayModule.DISPLAY_MEASURES);
			satellite.addName(5, "Azimuth (deg)", RtMeasurement.AZ,
					DisplayModule.DISPLAY_MEASURES);
			satellite.addName(6, "Elevation (deg)", RtMeasurement.EL,
					DisplayModule.DISPLAY_MEASURES);
			satellite.addName(7, "RS Errors", RtMeasurement.ERRORS,
					DisplayModule.DISPLAY_MEASURES);
			satellite.addName(8, "RS Erasures", RtMeasurement.ERASURES,
					DisplayModule.DISPLAY_MEASURES);
		}

		if (sat.isFox1()) {
			FoxSpacecraft fox = (FoxSpacecraft) sat;
			passes = new DisplayModule(fox, fox.user_display_name + " passes", 9,
					DisplayModule.DISPLAY_PASS_MEASURES);
			centerPanel.add(passes);
			passes.addName(1, "AOS",	PassMeasurement.AOS, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(2, "TCA",	PassMeasurement.TCA, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(3, "TCA Freq (Hz)",PassMeasurement.TCA_FREQ, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(4, "LOS", PassMeasurement.LOS, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(5, "Start Azimuth", PassMeasurement.START_AZIMUTH, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(6, "End Azimuth", PassMeasurement.END_AZIMUTH, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(7, "Max Elevation", PassMeasurement.MAX_ELEVATION, DisplayModule.DISPLAY_RT_ONLY);
			passes.addName(8, "Payloads Decoded", PassMeasurement.TOTAL_PAYLOADS, DisplayModule.DISPLAY_RT_ONLY);
		}
		JPanel bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

	}

	public void showGraphs() {

		if (satellite != null)
			satellite.showGraphs();

		if (passes != null)
			passes.showGraphs();

	}

	public void openGraphs() {
		if (satellite != null)
			satellite.openGraphs(SatMeasurementStore.RT_MEASUREMENT_TYPE);
		if (passes != null)
			passes.openGraphs(SatMeasurementStore.PASS_MEASUREMENT_TYPE);
	}

	public void closeGraphs() {
		if (satellite != null)
			satellite.closeGraphs();
		if (passes != null)
			passes.closeGraphs();
	}

	@Override
	public void run() {
		Thread.currentThread().setName("MyMeasurementsTab");
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
			if (Config.payloadStore.getUpdatedMeasurement(sat.foxId)) {

				rtMeasurement = Config.payloadStore
						.getLatestMeasurement(sat.foxId);
				if (rtMeasurement != null) {
					Config.payloadStore.setUpdatedMeasurement(sat.foxId, false);

					double snr = GraphPanel.roundToSignificantFigures(
							rtMeasurement
							.getRawValue(RtMeasurement.BIT_SNR), 3);
					satellite.updateSingleValue(1, Double.toString(snr));
					double rfsnr = GraphPanel
							.roundToSignificantFigures(rtMeasurement
									.getRawValue(RtMeasurement.RF_SNR), 3);
					satellite.updateSingleValue(2,
							Double.toString(rfsnr));
					double power = GraphPanel
							.roundToSignificantFigures(rtMeasurement
									.getRawValue(RtMeasurement.RF_POWER), 3);
					satellite.updateSingleValue(3,
							Double.toString(power));
					long freq = (long) rtMeasurement
							.getRawValue(RtMeasurement.CARRIER_FREQ);
					satellite.updateSingleValue(4, Long.toString(freq));
					int az = (int) rtMeasurement
							.getRawValue(RtMeasurement.AZ);
					satellite.updateSingleValue(5, Integer.toString(az));
					int el = (int) rtMeasurement
							.getRawValue(RtMeasurement.EL);
					satellite.updateSingleValue(6, Integer.toString(el));
					int err = (int) rtMeasurement
							.getRawValue(RtMeasurement.ERRORS);
					satellite
					.updateSingleValue(7, Integer.toString(err));
					int erase = (int) rtMeasurement
							.getRawValue(RtMeasurement.ERASURES);
					satellite.updateSingleValue(8,
							Integer.toString(erase));
				}
			}
			if (Config.payloadStore.getUpdatedPassMeasurement(sat.foxId)) {
				passMeasurement = Config.payloadStore.getLatestPassMeasurement(sat.foxId);
				if (passMeasurement != null) {
					Config.payloadStore.setUpdatedPassMeasurement(sat.foxId, false);
					//Log.println("Updated Pass Params Table");
					passes.updateSingleValue(1, passMeasurement.getStringValue(PassMeasurement.AOS));
					passes.updateSingleValue(2, passMeasurement.getStringValue(PassMeasurement.TCA));
					passes.updateSingleValue(3, passMeasurement.getStringValue(PassMeasurement.TCA_FREQ));
					passes.updateSingleValue(4, passMeasurement.getStringValue(PassMeasurement.LOS));
					passes.updateSingleValue(5, passMeasurement.getStringValue(PassMeasurement.START_AZIMUTH));
					passes.updateSingleValue(6, passMeasurement.getStringValue(PassMeasurement.END_AZIMUTH));
					passes.updateSingleValue(7, passMeasurement.getStringValue(PassMeasurement.MAX_ELEVATION));
					passes.updateSingleValue(8, passMeasurement.getStringValue(PassMeasurement.TOTAL_PAYLOADS));
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
		
	}
}
