package gui;

import common.FoxSpacecraft;
import telemetry.BitArrayLayout;

@SuppressWarnings("serial")
public class WodNamedExperimentTab extends NamedExperimentTab {

	public WodNamedExperimentTab(FoxSpacecraft sat, String displayName, BitArrayLayout displayLayout, BitArrayLayout displayLayout2, int displayType) {
		super(sat, displayName, displayLayout, displayLayout2, displayType);
	}
	
//	@Override
//	public void run() {
//		Thread.currentThread().setName("WodNamedExperimentTab");
//		running = true;
//		done = false;
//		boolean justStarted = true;
//		while(running) {
//
//			try {
//				Thread.sleep(500); // refresh data once a second
//			} catch (InterruptedException e) {
//				Log.println("ERROR: WOD Named Experiment thread interrupted");
//				e.printStackTrace(Log.getWriter());
//			}
//			if (Config.displayRawValues != showRawValues.isSelected()) {
//				showRawValues.setSelected(Config.displayRawValues);
//			}
//			if (foxId != 0 && Config.payloadStore.initialized()) {
//				if (Config.payloadStore.getUpdated(foxId, layout.name)) {
//					Config.payloadStore.setUpdated(foxId, layout.name, false);
//					updateTab(Config.payloadStore.getLatest(foxId, layout2.name), true);
//					parseRadiationFrames();
//					displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, layout.name));
//					MainWindow.setTotalDecodes();
//					if (justStarted) {
//						openGraphs();
//						justStarted = false;
//					}
//				}
//			}
//		}
//		done = true;
//	}
}
