package gui;

import javax.swing.JTable;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import telemetry.BitArrayLayout;
import telemetry.FoxFramePart;
import telemetry.PayloadWODUwExperiment;

@SuppressWarnings("serial")
public class WodUwExperimentTab extends UwExperimentTab {

	
	public static int[] wod_ids = {309920562, 309920256, 309330499};

	public WodUwExperimentTab(FoxSpacecraft sat) {
		super(sat, DisplayModule.DISPLAY_WOD_VULCAN);
	}

	
	@Override
	protected void parseRadiationFrames() {
	String[][] data = null;

	if (Config.displayRawRadData) {
//		if (Config.splitCanPackets) {
			// for each CAN Packet get the layout and all the packets
			String[][][] all = new String[250][][];
			int number = 0;
			int total = 0;
			for (int id : wod_ids) {
				BitArrayLayout lay = Config.satManager.getLayoutByCanId(fox.foxId, id);
				String[][] records = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, lay.name);
				if (records.length > 0) {
					all[number] = records;
					number++;
					total = total + records.length;
				}
			}
			int k = 0;
			data = new String[total][];
			for (int j=0; j<number; j++)
				for (int r=0; r<all[j].length; r++) {
					data[k++] = all[j][r];
				}
//		} else {
//			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.CAN_PKT_LAYOUT);
//		}
		if (data != null && data.length > 0)
			parseRawBytes(data,radTableModel);
	} else {
//		if (Config.splitCanPackets) {
			// for each CAN Packet name, get the layout and see what total we have
			String[][] totals = new String[250][];
			int number = 0;
			for (int id : wod_ids) {
				BitArrayLayout lay = Config.satManager.getLayoutByCanId(fox.foxId, id);
				int total = Config.payloadStore.getNumberOfFrames(fox.foxId, lay.name);
				if (total > 0) {
					String[] row = new String[5];
					row[0] = ""+id;
					row[1] = fox.canFrames.getGroundByCanId(id);
					row[2] = fox.canFrames.getNameByCanId(id);
					row[3] = fox.canFrames.getSenderByCanId(id);
					row[4] = ""+total;
					totals[number] = row;
					number++;
				}
			}
			data = new String[number][];
			for (int j=0; j<number; j++)
				data[j] = totals[j];
//		} else {
//			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.CAN_PKT_LAYOUT);				
//		}
		if (data != null && data.length > 0) {
			parseTelemetry(data);
		}
		//		topHalfPackets.setVisible(false);
		//		bottomHalfPackets.setVisible(false);
		//		topHalf.setVisible(true);
		//		bottomHalf.setVisible(true);

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