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
import telemetry.PayloadWOD;

public class WodHealthTab extends HealthTab {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JLabel lblSatLatitudeValue;
	JLabel lblSatLongitudeValue;
	
	public WodHealthTab(FoxSpacecraft spacecraft) {
		super(spacecraft, DisplayModule.DISPLAY_WOD);
		
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
		
		lblResetsValue = addReset(topPanel2, "Last WOD:");
		lblUptimeValue = addUptime(topPanel2, "");
		
	//	topPanel2.add(new Box.Filler(new Dimension(14,fonth), new Dimension(400,fonth), new Dimension(1600,fonth)));
		
		lblSatLatitudeValue = addTopPanelValue(topPanel2, "Footprint   Latitude:");
		lblSatLongitudeValue = addTopPanelValue(topPanel2, "Longitude:");
	}
	
	private void displayLatLong() {
		PayloadWOD wod = (PayloadWOD)realTime;
		lblSatLatitudeValue.setText(" " + wod.getSatLatitudeStr());
		lblSatLongitudeValue.setText(" " + wod.getSatLongitudeStr());
	}

	@Override
	public void parseFrames() {
		String[][] data = Config.payloadStore.getWODData(SAMPLES, fox.foxId, START_RESET, START_UPTIME);
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
						displayLatLong();
						displayFramesDecoded(Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.WOD_LAYOUT));
						//System.out.println("UPDATED RT Data: ");
					} else {
						//System.out.println("NO new RT Data: ");

					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.WOD_LAYOUT, false);
					if (justStarted) {
						openGraphs(FoxFramePart.TYPE_WOD);
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
