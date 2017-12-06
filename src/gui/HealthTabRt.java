package gui;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;
import telemetry.FoxFramePart;

public class HealthTabRt extends HealthTab {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public HealthTabRt(FoxSpacecraft spacecraft) {
		super(spacecraft, DisplayModule.DISPLAY_ALL);
		
		lblMode = new JLabel(MODE);
		lblMode.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblMode.setForeground(textLblColor);
		topPanel1.add(lblMode);
		lblModeValue = new JLabel();
		lblModeValue.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
		lblModeValue.setForeground(textColor);
		topPanel1.add(lblModeValue);
		
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
		
		lblResetsValue = addReset(topPanel2, "Realtime:");
		lblUptimeValue = addUptime(topPanel2, "");

		lblMaxResetsValue = addReset(topPanel2, "Max:");
		lblMaxUptimeValue = addUptime(topPanel2, "");

		lblMinResetsValue = addReset(topPanel2, "Min:");
		lblMinUptimeValue = addUptime(topPanel2, "");
		
		
		


	}
	
	@Override
	protected void displayRow(int row) {
		long reset_l = (long) table.getValueAt(row, HealthTableModel.RESET_COL);
    	long uptime = (long)table.getValueAt(row, HealthTableModel.UPTIME_COL);
    	//Log.println("RESET: " + reset);
    	//Log.println("UPTIME: " + uptime);
    	int reset = (int)reset_l;
    	realTime = Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.REAL_TIME_LAYOUT);
    	if (realTime != null)
    		updateTabRT(realTime, false);
    	maxPayload = Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.MAX_LAYOUT);
    	if (maxPayload != null)
    		updateTabMax(maxPayload);
    	minPayload = Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.MIN_LAYOUT);
    	if (minPayload != null)
    		updateTabMin(minPayload);
    	table.setRowSelectionInterval(row, row);
	}
	
	@Override
	public void parseFrames() {
		String[][] data = Config.payloadStore.getRtData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
		if (data.length > 0) {
			parseTelemetry(data);
			MainWindow.frame.repaint();
		}		
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
				if (Config.payloadStore.getUpdated(foxId, Spacecraft.MAX_LAYOUT)) {
					maxPayload = Config.payloadStore.getLatestMax(foxId);
					if (maxPayload != null) {
						if (showLatest == GraphFrame.SHOW_LIVE)
							updateTabMax(maxPayload);
						displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.MAX_LAYOUT, false);
				}
				if (Config.payloadStore.getUpdated(foxId, Spacecraft.MIN_LAYOUT)) {
					minPayload = Config.payloadStore.getLatestMin(foxId);
					if (minPayload != null) {
						if (showLatest == GraphFrame.SHOW_LIVE)
							updateTabMin(minPayload);
						displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.MIN_LAYOUT, false);
					
				}

				// Read the RealTime last so that at startup the Captured Date in the bottom right will be the last real time record
				if (Config.payloadStore.getUpdated(foxId, Spacecraft.REAL_TIME_LAYOUT)) {
					realTime = Config.payloadStore.getLatestRt(foxId);
					if (realTime != null) {
						if (showLatest == GraphFrame.SHOW_LIVE)
							updateTabRT(realTime, true);
						//displayMode(0);
						displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
						//System.out.println("UPDATED RT Data: ");
					} else {
						//System.out.println("NO new RT Data: ");

					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.REAL_TIME_LAYOUT, false);
					if (justStarted) {
						openGraphs(FoxFramePart.TYPE_REAL_TIME);
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
