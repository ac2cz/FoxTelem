package gui;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import telemetry.FoxFramePart;

public class WodVulcanTab extends VulcanTab {

	public WodVulcanTab(FoxSpacecraft sat) {
		super(sat, DisplayModule.DISPLAY_WOD_VULCAN);
	}

	
	@Override
	protected void parseRadiationFrames() {
		
		if (Config.displayRawRadData) {
			String[][] data = Config.payloadStore.getRadData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
			if (data != null && data.length > 0)
				radTableModel.setData(parseRawBytes(data));
		} else {
			if (displayTelem) {
				String[][] data = Config.payloadStore.getWodRadTelemData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
				if (data != null && data.length > 0)
					parseTelemetry(data);
					topHalfPackets.setVisible(false);
					bottomHalfPackets.setVisible(false);
					topHalf.setVisible(true);
//					bottomHalf.setVisible(true);
			
			}
			else {
				String[][] data = Config.payloadStore.getRadData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
				if (data.length > 0)
					parsePackets(data);
					topHalfPackets.setVisible(true);
					bottomHalfPackets.setVisible(true);
					topHalf.setVisible(false);
					bottomHalf.setVisible(false);
			
			}
		}
		if (showRawBytes.isSelected()) {
			packetScrollPane.setVisible(false); 
			scrollPane.setVisible(true);
		} else { 
			packetScrollPane.setVisible(true);
			scrollPane.setVisible(false);
		}
		
		MainWindow.frame.repaint();
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
					if (Config.payloadStore.getUpdated(foxId, Spacecraft.WOD_RAD_LAYOUT)) {
						//radPayload = Config.payloadStore.getLatestRad(foxId);
						Config.payloadStore.setUpdated(foxId, Spacecraft.WOD_RAD_LAYOUT, false);

						parseRadiationFrames();
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.WOD_RAD_LAYOUT));
						MainWindow.setTotalDecodes();
						if (justStarted) {
							openGraphs(FoxFramePart.TYPE_WOD_RAD);
							justStarted = false;
						}
					}
			}
		}
		done = true;
	}
}
