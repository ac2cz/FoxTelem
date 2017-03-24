package gui;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;

public class WodVulcanTab extends VulcanTab {

	public WodVulcanTab(FoxSpacecraft sat) {
		super(sat);
		// TODO Auto-generated constructor stub
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
			if (Config.payloadStore.initialized()) {
				if (Config.displayRawRadData != showRawBytes.isSelected()) {
					showRawBytes.setSelected(Config.displayRawRadData);
					parseRadiationFrames();
				}
				if (Config.displayRawValues != showRawValues.isSelected()) {
					showRawValues.setSelected(Config.displayRawValues);
					updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.WOD_RAD_LAYOUT));
				}

				if (foxId != 0)
					if (Config.payloadStore.getUpdated(foxId, Spacecraft.WOD_RAD_LAYOUT)) {
						//radPayload = Config.payloadStore.getLatestRad(foxId);
						Config.payloadStore.setUpdated(foxId, Spacecraft.WOD_RAD_LAYOUT, false);

						parseRadiationFrames();
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.WOD_RAD_LAYOUT));
						MainWindow.setTotalDecodes();
						if (justStarted) {
							openGraphs();
							justStarted = false;
						}
					}
			}
		}
		done = true;
	}
}
