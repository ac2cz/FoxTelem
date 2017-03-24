package gui;

import common.Config;
import common.Log;
import common.Spacecraft;

public class WodHealthTab extends HealthTab {

	public WodHealthTab(Spacecraft spacecraft) {
		super(spacecraft);
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
				Log.println("ERROR: WodHealthTab thread interrupted");
				e.printStackTrace(Log.getWriter());
			} 	
			
			if (Config.displayRawValues != showRawValues.isSelected()) {
				showRawValues.setSelected(Config.displayRawValues);
			}
			if (foxId != 0 && Config.payloadStore.initialized()) {
				// Read the RealTime last so that at startup the Captured Date in the bottom right will be the last real time record
				if (Config.payloadStore.getUpdated(foxId, Spacecraft.WOD_LAYOUT)) {
					realTime = Config.payloadStore.getLatest(foxId, Spacecraft.WOD_LAYOUT);
					if (realTime != null) {
						updateTabRT(realTime);
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.WOD_LAYOUT));
						//System.out.println("UPDATED RT Data: ");
					} else {
						//System.out.println("NO new RT Data: ");

					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.WOD_LAYOUT, false);
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
}
