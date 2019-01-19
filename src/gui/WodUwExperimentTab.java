package gui;

import javax.swing.JTable;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import telemetry.FoxFramePart;
import telemetry.PayloadWODUwExperiment;

@SuppressWarnings("serial")
public class WodUwExperimentTab extends UwExperimentTab {

	public WodUwExperimentTab(FoxSpacecraft sat) {
		super(sat, DisplayModule.DISPLAY_WOD_VULCAN);
	}

	
	@Override
	protected void parseRadiationFrames() {
		
		if (Config.displayRawRadData) {
			String[][] data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.WOD_CAN_PKT_LAYOUT);
			if (data != null && data.length > 0)
				parseRawBytes(data, radTableModel);
		} else {
			if (displayTelem) {
				String[][] data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.WOD_CAN_PKT_LAYOUT);
//				String[][] data = Config.payloadStore.getWodRadTelemData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
				if (data != null && data.length > 0)
					parseTelemetry(data);
//					topHalfPackets.setVisible(false);
//					bottomHalfPackets.setVisible(false);
					topHalf.setVisible(true);
					bottomHalf.setVisible(true);
			
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
	
	protected void displayRow(JTable table, int row) {
		long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
    	long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
    	//Log.println("RESET: " + reset);
    	//Log.println("UPTIME: " + uptime);
    	int reset = (int)reset_l;
    	updateTab((PayloadWODUwExperiment) Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.WOD_RAD_LAYOUT, false), false);
    	
    	table.setRowSelectionInterval(row, row);
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("UwWODTab");
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
						Config.payloadStore.setUpdated(foxId, Spacecraft.WOD_RAD_LAYOUT, false);
						
						parseRadiationFrames();
						updateTab(Config.payloadStore.getLatest(foxId, Spacecraft.WOD_RAD_LAYOUT), true);
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.WOD_RAD_LAYOUT),
								Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.WOD_CAN_PKT_LAYOUT));
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
