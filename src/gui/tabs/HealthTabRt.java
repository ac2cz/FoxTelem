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
import telemetry.FramePart;

public class HealthTabRt extends HealthTab {

	private static final long serialVersionUID = 1L;

	public HealthTabRt(Spacecraft spacecraft) {
		super(spacecraft, DisplayModule.DISPLAY_ALL);
		TAB_TYPE = "health";
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
		lblFramesDecoded.setFont(new Font("SansSerif", Font.BOLD, (int)(Config.displayModuleFontSize * 14/11)));
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
	protected void addBottomFilter() {
		currentBut = new JRadioButton("Current");
		bottomPanel.add(currentBut);
		currentBut.addActionListener(this);
		rtBut = new JRadioButton("RT");
		bottomPanel.add(rtBut);
		rtBut.addActionListener(this);
		maxBut = new JRadioButton("MAX");
		bottomPanel.add(maxBut);
		maxBut.addActionListener(this);
		minBut = new JRadioButton("MIN");
		bottomPanel.add(minBut);
		minBut.addActionListener(this);
		
		ButtonGroup group = new ButtonGroup();
		group.add(currentBut);
		group.add(rtBut);
		group.add(maxBut);
		group.add(minBut);
		healthTableToDisplay = Config.loadGraphIntValue(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "health"+"healthTableToDisplay");
		if (healthTableToDisplay == DISPLAY_CURRENT) {
			currentBut.setSelected(true);
		} else if (healthTableToDisplay == DISPLAY_RT) {
			rtBut.setSelected(true);
		} else if (healthTableToDisplay == DISPLAY_MAX) {
			maxBut.setSelected(true);
		} else {
			minBut.setSelected(true);
		}

		super.addBottomFilter();
	}
	
	@Override
	protected void displayRow(JTable rtTable, int fromRow, int row) {
		long reset_l = (long) rtTable.getValueAt(row, HealthTableModel.RESET_COL);
    	long uptime = (long)rtTable.getValueAt(row, HealthTableModel.UPTIME_COL);
    	//Log.println("RESET: " + reset);
    	//Log.println("UPTIME: " + uptime);
    	int reset = (int)reset_l;
    	maxPayload = Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.MAX_LAYOUT, true);
    	if (maxPayload != null)
    		updateTabMax(maxPayload);
    	minPayload = Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.MIN_LAYOUT, true);
    	if (minPayload != null)
    		updateTabMin(minPayload);
    	realTime = Config.payloadStore.getFramePart(foxId, reset, uptime, Spacecraft.REAL_TIME_LAYOUT, false);
    	if (realTime != null)
    		updateTabRT(realTime, false);
    	if (fromRow == NO_ROW_SELECTED)
    		fromRow = row;
    	if (fromRow <= row)
    		rtTable.setRowSelectionInterval(fromRow, row);
    	else
    		rtTable.setRowSelectionInterval(row, fromRow);
	}
	
	@Override
	public void parseFrames() {
		String[][] data = null;

		if (healthTableToDisplay == DISPLAY_CURRENT) {
		}
		if (healthTableToDisplay == DISPLAY_RT)
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.REAL_TIME_LAYOUT);
		if (healthTableToDisplay == DISPLAY_MAX)
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.MAX_LAYOUT);
		if (healthTableToDisplay == DISPLAY_MIN)
			data = Config.payloadStore.getTableData(SAMPLES, fox.foxId, START_RESET, START_UPTIME, true, reverse, Spacecraft.MIN_LAYOUT);
		
		displayTable();
		if (data != null && data.length > 0) {
			parseTelemetry(data);
			MainWindow.frame.repaint();
		}

	}


	@Override
	public void run() {
		int currentRtFrames = 0;
		int currentMaxFrames = 0;
		int currentMinFrames = 0;
		Thread.currentThread().setName("HealthTabRt");
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
				int frames = Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.MAX_LAYOUT);
				if (frames != currentMaxFrames) {
					currentMaxFrames = frames;
					maxPayload = Config.payloadStore.getLatestMax(foxId);
					if (maxPayload != null) {
						if (healthTableToDisplay == DISPLAY_CURRENT) 
							updateTabMax(maxPayload);
					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.MAX_LAYOUT, false);
					displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
					MainWindow.setTotalDecodes();
				}
				frames = Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.MIN_LAYOUT);
				if (frames != currentMinFrames) {
					currentMinFrames = frames;
					minPayload = Config.payloadStore.getLatestMin(foxId);
					if (minPayload != null) {
						if (healthTableToDisplay == DISPLAY_CURRENT) 
							updateTabMin(minPayload);
						
					}
					displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
					Config.payloadStore.setUpdated(foxId, Spacecraft.MIN_LAYOUT, false);
					MainWindow.setTotalDecodes();
				}

				// Read the RealTime last so that at startup the Captured Date in the bottom right will be the last real time record
				frames = Config.payloadStore.getNumberOfFrames(foxId, Spacecraft.REAL_TIME_LAYOUT);
				if (frames != currentRtFrames) {
					currentRtFrames = frames;
					realTime = Config.payloadStore.getLatestRt(foxId);
					if (realTime != null) {
						if (healthTableToDisplay == DISPLAY_CURRENT)
							updateTabRT(realTime, true);
						else
							parseFrames();
						//displayMode(0);
						displayFramesDecoded(Config.payloadStore.getNumberOfTelemFrames(foxId));
						//System.out.println("UPDATED RT Data: ");
					} else {
						//System.out.println("NO new RT Data: ");

					}
					Config.payloadStore.setUpdated(foxId, Spacecraft.REAL_TIME_LAYOUT, false);
					MainWindow.setTotalDecodes();
					if (justStarted) {
						openGraphs();
						justStarted = false;
					}
					MainWindow.frame.repaint();
				}
				//if (showLatest == GraphFrame.SHOW_LIVE)
//				System.err.println("Split at/max:" + + splitPane.getDividerLocation() + "/"+ splitPane.getMaximumDividerLocation());
				if (healthTableToDisplay == DISPLAY_CURRENT) {
					if (fox.hasModeInHeader) { 
						displayMode(fox.determineModeFromHeader());
					}
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
      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "health"+"healthTableToDisplay", healthTableToDisplay);
      		//Log.println("RT Picked");
      		parseFrames();
		}
		if (e.getSource() == maxBut) {
			healthTableToDisplay = DISPLAY_MAX;
			hideTables(false);
      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "health"+"healthTableToDisplay", healthTableToDisplay);
     		//Log.println("MAX Picked");
     		parseFrames();
			
		}
		if (e.getSource() == minBut) {
			healthTableToDisplay = DISPLAY_MIN;
			hideTables(false);
      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "health"+"healthTableToDisplay", healthTableToDisplay);
     		//Log.println("MIN Picked");
     		parseFrames();
		}
		if (e.getSource() == currentBut) {
			healthTableToDisplay = DISPLAY_CURRENT;
			hideTables(true);
      		Config.saveGraphIntParam(fox.getIdString(), GraphFrame.SAVED_PLOT, FramePart.TYPE_REAL_TIME, HEALTHTAB, "health"+"healthTableToDisplay", healthTableToDisplay);
     		//Log.println("MIN Picked");
      		
      		realTime = Config.payloadStore.getLatestRt(foxId);
      		minPayload = Config.payloadStore.getLatestMin(foxId);
      		maxPayload = Config.payloadStore.getLatestMax(foxId);
      		
      		if (realTime != null)
				updateTabRT(realTime, false);
			if (maxPayload != null)
				updateTabMax(maxPayload);
			if (minPayload != null)
				updateTabMin(minPayload);
     		parseFrames();
		}
		showLiveOrHistorical();
	}	
}
