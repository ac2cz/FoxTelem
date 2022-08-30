package gui.tabs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import gui.graph.GraphFrame;
import predict.PositionCalcException;
import telemetry.FramePart;
import telemetry.LayoutLoadException;
import telemetry.payloads.PayloadWOD;
import uk.me.g4dpz.satellite.SatPos;

public class WodHealthTab extends HealthTab {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JLabel lblSatLatitudeValue;
	JLabel lblSatLongitudeValue;
	
	public WodHealthTab(Spacecraft spacecraft) throws LayoutLoadException {
		super(spacecraft, DisplayModule.DISPLAY_WOD);
		TAB_TYPE = "wod";
		healthTableToDisplay = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, TAB_TYPE+"healthTableToDisplay");
		splitPaneHeight = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, TAB_TYPE+"splitPaneHeight");
		if (healthTableToDisplay == DISPLAY_CURRENT) {
			hideTables(true);
		} else {
			if (splitPaneHeight != 0) 
				splitPane.setDividerLocation(splitPaneHeight);
			else
				splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);
		}
		//captureSplitPaneHeight(); // this will also lock us to the bottom at startup if needed.
		showLiveOrHistorical();  
		
		topPanel1.add(new Box.Filler(new Dimension(14,fonth), new Dimension(1600,fonth), new Dimension(1600,fonth)));

		lblFramesDecoded = new JLabel("WOD Payloads Decoded:");
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
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

	@Override
	protected void addBottomFilter() {
		// Bottom panel
		currentBut = new JRadioButton("Current");
		bottomPanel.add(currentBut);
		currentBut.addActionListener(this);
		rtBut = new JRadioButton("History");
		bottomPanel.add(rtBut);
		rtBut.addActionListener(this);

		ButtonGroup group = new ButtonGroup();
		group.add(currentBut);
		group.add(rtBut);
		healthTableToDisplay = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "wod"+"healthTableToDisplay");
		if (healthTableToDisplay == DISPLAY_CURRENT) {
			currentBut.setSelected(true);
		} else if (healthTableToDisplay == DISPLAY_RT) {
			rtBut.setSelected(true);
		}
		super.addBottomFilter();
	}
	
	/**
	 * Display the LAT LON of the selected record at the top of the WOD tab.
	 * The position calculation returns an error value if the LAT LON can not be calculated.
	 */
	private void displayLatLong() {
		PayloadWOD wod = (PayloadWOD)realTime;
		SatPos pos = null;
		try {
			pos = fox.getSatellitePosition(wod.getResets(), wod.getUptime());
			if (pos != null) {
				wod.setSatPosition(pos);
				lblSatLatitudeValue.setText(" " + wod.getSatLatitudeStr());
				lblSatLongitudeValue.setText(" " + wod.getSatLongitudeStr());
			} else {
				lblSatLatitudeValue.setText(" ERR");
				lblSatLongitudeValue.setText(" ERR");
			}
		} catch (PositionCalcException e) {
			if (e.errorCode == FramePart.NO_TLE) {
				lblSatLatitudeValue.setText(" NO TLE");
				lblSatLongitudeValue.setText(" NO TLE");
			} else if (e.errorCode == FramePart.NO_T0) {
				lblSatLatitudeValue.setText(" T0 NOT SET");
				lblSatLongitudeValue.setText(" T0 NOT SET");
			}
		}
		
		
	}

	protected void displayRow(JTable rtTable, int fromRow, int row) {
		long reset_l = (long)rtTable.getValueAt(row, HealthTableModel.RESET_COL);
    	long uptime = (long)rtTable.getValueAt(row, HealthTableModel.UPTIME_COL);
    	int reset = (int)reset_l;
    	//Log.println("RESET: " + reset);
    	//Log.println("UPTIME: " + uptime);
    	realTime = Config.payloadStore.getFramePart(foxId, reset, uptime, rt.name, false);
    	if (realTime != null)
    		updateTabRT(realTime, false);
    	if (fromRow == NO_ROW_SELECTED)
    		fromRow = row;
    	if (fromRow <= row)
    		rtTable.setRowSelectionInterval(fromRow, row);
    	else
    		rtTable.setRowSelectionInterval(row, fromRow);
       	displayLatLong();
	}
	
	@Override
	public void parseFrames() {
		//String[][] data = Config.payloadStore.getWODData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, reverse);
		String[][] data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, rt.name);

		if (data != null && data.length > 0) {
			parseTelemetry(data);
			displayTable();
			MainWindow.frame.repaint();
		}		
	}

	@Override
	public void run() {
		Thread.currentThread().setName("WodHealthTab");
		running = true;
		done = false;
		boolean justStarted = true;
		int currentFrames = 0;
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
				int frames = Config.payloadStore.getNumberOfFrames(foxId, rt.name);
				if (frames != currentFrames) {
					currentFrames = frames;
					realTime = Config.payloadStore.getLatest(foxId, rt.name);
					if (realTime != null) {
						if (healthTableToDisplay == DISPLAY_CURRENT) {
							updateTabRT(realTime, true);
							displayLatLong();
						} else {
							parseFrames();
						}
						displayFramesDecoded(frames);
						//System.out.println("UPDATED RT Data: ");
					} else {
						//System.out.println("NO new RT Data: ");

					}
					Config.payloadStore.setUpdated(foxId, rt.name, false);
					MainWindow.setTotalDecodes();
					if (justStarted) {
						openGraphs();
						justStarted = false;
					}
					MainWindow.frame.repaint();
				}
			}
			//System.out.println("Health tab running: " + running);
		}
		done = true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (e.getSource() == rtBut) {
			healthTableToDisplay = DISPLAY_RT;
			hideTables(false);
      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "wod"+"healthTableToDisplay", healthTableToDisplay);
      		//Log.println("RT Picked");
      		parseFrames();
		}

		if (e.getSource() == currentBut) {
			healthTableToDisplay = DISPLAY_CURRENT;
			hideTables(true);
      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "wod"+"healthTableToDisplay", healthTableToDisplay);
     		//Log.println("MIN Picked");
      		
      		realTime = Config.payloadStore.getLatest(foxId, Spacecraft.WOD_LAYOUT);
      		
      		if (realTime != null)
      			updateTabRT(realTime, true);
     		parseFrames();
		}
		showLiveOrHistorical();
	}	

}
