package gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.herci.HerciHSTab;
import gui.herci.HerciLSTab;
import gui.legacyTabs.CameraTab;
import gui.legacyTabs.VulcanTab;
import gui.legacyTabs.WodVulcanTab;
import gui.tabs.CanExperimentTab;
import gui.tabs.DisplayModule;
import gui.tabs.FoxTelemTab;
import gui.tabs.HealthTabRt;
import gui.tabs.ModuleTab;
import gui.tabs.MyMeasurementsTab;
import gui.tabs.NamedExperimentTab;
import gui.tabs.WodHealthTab;
import gui.tabs.WodNamedExperimentTab;
import gui.uw.UwExperimentTab;
import gui.uw.WodUwExperimentTab;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;

/**
* 
* FOX 1 Telemetry Decoder
* @author chris.e.thompson g0kla/ac2cz
*
* Copyright (C) 2016 amsat.org
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
* The SpacecraftTab is a sub tab of the MainWindow.  It groups all of the tabs for a given spacecraft
* including health, experiments and measurements.
*
*/
@SuppressWarnings("serial")
public class SpacecraftTab extends JPanel {

	Spacecraft sat;
	JTabbedPane tabbedPane;
	MyMeasurementsTab measurementsTab;
	
	// We have one health thread per health tab
//	ArrayList<Thread> tabThreads;
	Thread healthThread;
	// We have one radiation thread and camera thread per Radiation Experiment/Camera tab
	Thread experimentThread;
	//Thread ragExperimentThread;
	Thread cameraThread;
	Thread herciThread;
	Thread measurementThread;
	Thread wodHealthThread;
	//Thread wodExperimentThread;
	
	public SpacecraftTab(Spacecraft s) {
		sat = s;
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setLayout(new BorderLayout(0, 0));
		add(tabbedPane, BorderLayout.CENTER);
//		tabThreads = new ArrayList<Thread>();
		createTabs(sat);
	}
	
	public void showGraphs() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component comp = tabbedPane.getComponentAt(i);
			if (comp instanceof ModuleTab) {
				ModuleTab tab = (ModuleTab) comp;
				if (tab != null)
					tab.showGraphs();
			}
			if (comp instanceof MyMeasurementsTab) {
				MyMeasurementsTab tab = (MyMeasurementsTab) comp;
				if (tab != null) {
					tab.showGraphs();
				}
			}
		}
	}

	public void refreshTabs(Spacecraft fox, boolean closeGraphs) {
		closeTabs(fox, closeGraphs);
		createTabs(fox);
	}
	
	public void closeTabs(Spacecraft fox, boolean closeGraphs) {
		sat = fox;
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component comp = tabbedPane.getComponentAt(i);
			if (comp instanceof ModuleTab) {
				ModuleTab tab = (ModuleTab) comp;
				if (tab != null) {
					if (closeGraphs) tab.closeGraphs();
					tab.remove(tab);
				}
			}
			if (comp instanceof MyMeasurementsTab) {
				MyMeasurementsTab tab = (MyMeasurementsTab) comp;
				if (tab != null) {
					if (closeGraphs) tab.closeGraphs();
					tab.remove(tab);
				}
			}
		}
	}
	
	public void createTabs(Spacecraft fox) {
		if (fox.hasFOXDB_V3)
			addTabs();
		else
			addLegacyTabs();		
		addMeasurementsTab(sat);
	}

	public void stop() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component comp = tabbedPane.getComponentAt(i);
			if (comp instanceof ModuleTab) {
				ModuleTab tab = (ModuleTab) comp;
				if (tab != null) {
					stopThreads(tab);
				}
			}
		}
	}
	private void addTabs() {
		stop();

		for (BitArrayLayout lay : sat.layout) {
			if (lay.isSecondaryPayload()) continue; // not the secondary format that is displayed at the top of the tab and in the table when we uncheck "Show Raw Bytes"
			if (lay.isRealTime()) {
				// Add health tab
				HealthTabRt healthTab = null;
				try {
					healthTab = new HealthTabRt(sat);
				} catch (LayoutLoadException e) {
					Log.errorDialog("ERROR loading health tab", ""+e);
					e.printStackTrace(Log.getWriter());
					System.exit(1);;
				}
				Thread healthThread = new Thread(healthTab);
				healthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				healthThread.start();
				String HEALTH = "Health";
				tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
						+ HEALTH + "</b></body></html>", healthTab );
			}
			if (lay.isWOD()) {
				WodHealthTab wodHealthTab = null;
				try {
					wodHealthTab = new WodHealthTab(sat);
				} catch (LayoutLoadException e) {
					Log.errorDialog("ERROR loading WOD tab", ""+e);
					e.printStackTrace(Log.getWriter());
					System.exit(1);;
				}
				Thread wodHealthThread = new Thread(wodHealthTab);
				wodHealthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
				wodHealthThread.start();
//				tabThreads.add(wodHealthThread);

				String WOD = "WOD";
				tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
						+ WOD + "</b></body></html>", wodHealthTab );
			}
			if (lay.isExperiment()) { 
				try {
					addNamedExperimentTab(sat, lay);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}
			}
			if (lay.isWODExperiment()) {
				try {
					addWodNamedExpTab(sat, lay);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup WOD Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}
			}
			if (lay.isCanExperiment()) { 
				try {
					addCanExperimentTab(sat, lay);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup CAN Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}
			}
			if (lay.isCanWodExperiment()) { 
				try {
					addCanWodExperimentTab(sat, lay);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup CAN WOD Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}
			}
		}

	}

	private void addLegacyTabs() {
		stop();
		
		HealthTabRt healthTab = null;
		try {
			healthTab = new HealthTabRt(sat);
		} catch (LayoutLoadException e1) {
			Log.errorDialog("ERROR loading health tab", ""+e1);
			e1.printStackTrace(Log.getWriter());
			System.exit(1);;
		}
		healthThread = new Thread(healthTab);
		healthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		healthThread.start();
//		tabThreads.add(healthThread);

		String HEALTH = "Health";
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ HEALTH + "</b></body></html>", healthTab );

		if (sat.getLayoutIdxByName(Spacecraft.WOD_LAYOUT) != Spacecraft.ERROR_IDX) {
			try {
				addWodTab(sat);
			} catch (Exception e) {
				e.printStackTrace(Log.getWriter());
				Log.errorDialog("Layout Failure", "Failed to setup Whole Orbit Data tab for sat: " + sat.user_display_name 
						+ "\nCheck the Spacecraft.dat file and remove this layout if it is not valid\n" + e);
			}

		}

		for (int exp : (sat).experiments) {
			if (exp == Spacecraft.EXP_VANDERBILT_LEP) {
				try {
					addExperimentTab(sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}
				if (sat.getLayoutIdxByName(Spacecraft.WOD_RAD_LAYOUT) != Spacecraft.ERROR_IDX) {
					try {
					addWodExpTab(sat);
					} catch (Exception e) {
						e.printStackTrace(Log.getWriter());
						Log.errorDialog("Layout Failure", "Failed to setup WOD Experiment tab for sat: " + sat.user_display_name 
								+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
					}
				}
			}

			if (exp == Spacecraft.EXP_VT_CAMERA || exp == Spacecraft.EXP_VT_CAMERA_LOW_RES)
				try {
					addCameraTab(sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup VT Camera tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				}

			if (exp == Spacecraft.EXP_IOWA_HERCI) {
				try {
					addHerciHSTab(sat);
					addHerciLSTab(sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup IOWA HERCI tabs for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				}
			}
			if (exp == Spacecraft.EXP_UW)
				try {
					addUwExperimentTab(sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup UW Experiement tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				}
		}
		
		if (sat.getLayoutIdxByName(Spacecraft.WOD_CAN_LAYOUT) != Spacecraft.ERROR_IDX) {
			try {
			addUwWodExperimentTab(sat);
			} catch (Exception e) {
				e.printStackTrace(Log.getWriter());
				Log.errorDialog("Layout Failure", "Failed to setup UW WOD tab for sat: " + sat.user_display_name 
						+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
			}
		}
		
	}

	private void addWodTab(Spacecraft fox) {
		
		WodHealthTab wodHealthTab = null;
		try {
			wodHealthTab = new WodHealthTab(sat);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR loading WOD tab", ""+e);
			e.printStackTrace(Log.getWriter());
			System.exit(1);;
		}
		wodHealthThread = new Thread(wodHealthTab);
		wodHealthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodHealthThread.start();
//		tabThreads.add(wodHealthThread);

		String WOD = "WOD";
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ WOD + "</b></body></html>", wodHealthTab );
	}
	
	private void addWodExpTab(Spacecraft fox) {
		WodVulcanTab wodExperimentTab = new WodVulcanTab(fox);
		Thread wodExperimentThread = new Thread((VulcanTab)wodExperimentTab);
		wodExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodExperimentThread.start();
//		tabThreads.add(wodExperimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" 
				+ "VU Rad WOD" + "</body></html>", wodExperimentTab );

	}

	private void addExperimentTab(Spacecraft fox) {
		
		VulcanTab experimentTab = new VulcanTab(fox, DisplayModule.DISPLAY_EXPERIMENT);
		experimentThread = new Thread((VulcanTab)experimentTab);
		experimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		experimentThread.start();
//		tabThreads.add(experimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" VU Rad ("+ fox.getIdString() + ")</body></html>", experimentTab );

	}
	
	private void addNamedExperimentTab(Spacecraft fox, BitArrayLayout layout) {
		BitArrayLayout secondaryLayout = fox.getSecondaryLayoutFromPrimaryName(layout.name);
		String title = "Experiment: " + layout.name;
		if (layout.title != null && !layout.title.equalsIgnoreCase(""))
			title = layout.title;
		NamedExperimentTab ragExperimentTab = new NamedExperimentTab(fox, title, 
				layout,
				secondaryLayout, DisplayModule.DISPLAY_EXPERIMENT);
		Thread ragExperimentThread = new Thread(ragExperimentTab);
		ragExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		ragExperimentThread.start();
//		tabThreads.add(ragExperimentThread);

		String shortTitle = layout.name;
		if (layout.shortTitle != null && !layout.shortTitle.equalsIgnoreCase(""))
			shortTitle = layout.shortTitle;
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
				shortTitle + "</body></html>", ragExperimentTab);

	}
	
	private void addWodNamedExpTab(Spacecraft fox, BitArrayLayout layout) {
		BitArrayLayout secondaryLayout = fox.getSecondaryLayoutFromPrimaryName(layout.name);
		String title = "Experiment: " + layout.name;
		if (layout.title != null && !layout.title.equalsIgnoreCase(""))
			title = layout.title;
		WodNamedExperimentTab wodExperimentTab = new WodNamedExperimentTab(fox, title, 
				layout,
				secondaryLayout, DisplayModule.DISPLAY_WOD_EXPERIMENT);
		Thread wodExperimentThread = new Thread(wodExperimentTab);
		wodExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodExperimentThread.start();
//		tabThreads.add(wodExperimentThread);

		String shortTitle = layout.name;
		if (layout.shortTitle != null && !layout.shortTitle.equalsIgnoreCase(""))
			shortTitle = layout.shortTitle;
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" 
				+ shortTitle + "</body></html>", wodExperimentTab );

	}
	
	private void addCanExperimentTab(Spacecraft fox, BitArrayLayout layout) {
		String title = "Experiment: " + layout.name;
		if (layout.title != null && !layout.title.equalsIgnoreCase(""))
			title = layout.title;
		BitArrayLayout canPktLayout =  Config.satManager.getLayoutByName(fox.foxId, Spacecraft.CAN_PKT_LAYOUT);
		CanExperimentTab canExperimentTab = new CanExperimentTab(fox, title, 
				layout, canPktLayout, DisplayModule.DISPLAY_EXPERIMENT);
		Thread ragExperimentThread = new Thread(canExperimentTab);
		ragExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		ragExperimentThread.start();

		String shortTitle = layout.name;
		if (layout.shortTitle != null && !layout.shortTitle.equalsIgnoreCase(""))
			shortTitle = layout.shortTitle;
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
				shortTitle + "</body></html>", canExperimentTab);

	}
	
	private void addCanWodExperimentTab(Spacecraft fox, BitArrayLayout layout) {
		String title = "WOD Experiment: " + layout.name;
		if (layout.title != null && !layout.title.equalsIgnoreCase(""))
			title = layout.title;
		BitArrayLayout canPktLayout =  Config.satManager.getLayoutByName(fox.foxId, Spacecraft.WOD_CAN_PKT_LAYOUT);
		CanExperimentTab canExperimentTab = new CanExperimentTab(fox, title, 
				layout, canPktLayout, DisplayModule.DISPLAY_EXPERIMENT);
		Thread ragExperimentThread = new Thread(canExperimentTab);
		ragExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		ragExperimentThread.start();

		String shortTitle = layout.name;
		if (layout.shortTitle != null && !layout.shortTitle.equalsIgnoreCase(""))
			shortTitle = layout.shortTitle;
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
				shortTitle + "</body></html>", canExperimentTab);

	}
	
	private void addUwExperimentTab(Spacecraft fox) {

		UwExperimentTab experimentTab = new UwExperimentTab(fox, DisplayModule.DISPLAY_UW);
		experimentThread = new Thread((UwExperimentTab)experimentTab);
		experimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		experimentThread.start();
//		tabThreads.add(experimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		"CAN Pkts</body></html>", experimentTab);

	}

	private void addUwWodExperimentTab(Spacecraft fox) {
		WodUwExperimentTab wodExperimentTab = new WodUwExperimentTab(fox);
		Thread wodExperimentThread = new Thread((WodUwExperimentTab)wodExperimentTab);
		wodExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodExperimentThread.start();
//		tabThreads.add(wodExperimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" 
				+ "CAN Pkt WOD" + "</body></html>", wodExperimentTab );

	}

	private void addHerciLSTab(Spacecraft fox) {

		HerciLSTab experimentTab = new HerciLSTab(fox);
		experimentThread = new Thread((HerciLSTab)experimentTab);
		experimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		experimentThread.start();
//		tabThreads.add(experimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI HK ("+ fox.getIdString() + ")</body></html>", experimentTab);

	}
	
	private void addHerciHSTab(Spacecraft fox) {
		HerciHSTab herciTab = new HerciHSTab(fox);
		herciThread = new Thread(herciTab);
			
		herciThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		herciThread.start();
//		tabThreads.add(herciThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI ("+ fox.getIdString() + ")</body></html>", herciTab);
	}
	
	private void addCameraTab(Spacecraft fox) {

		CameraTab cameraTab = new CameraTab(fox);
		cameraThread = new Thread(cameraTab);
		cameraThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		cameraThread.start();
//		tabThreads.add(cameraThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" Camera ("+ fox.getIdString() + ")</body></html>", cameraTab);
	}
	
	private void addMeasurementsTab(Spacecraft fox) {
		if (measurementsTab != null) {
			measurementsTab.stopProcessing();
			while (!measurementsTab.isDone())
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace(Log.getWriter());
				}
		}
		measurementsTab = new MyMeasurementsTab(fox);
		measurementsTab.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab( "<html><body leftmargin=5 topmargin=8 marginwidth=5 marginheight=5>Measurements</body></html>", measurementsTab );
		Thread measurementThread = new Thread(measurementsTab);
		measurementThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		measurementThread.start();
//		tabThreads.add(measurementThread);
		
	}
	

	private void stopThreads(FoxTelemTab tab) {
		if (tab != null) {
			tab.stopProcessing(); 

			while (!tab.isDone())
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace(Log.getWriter());
				}
		}
	}

	public void closeGraphs() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			Component comp = tabbedPane.getComponentAt(i);
			if (comp instanceof ModuleTab) {
				ModuleTab tab = (ModuleTab) comp;
				if (tab != null) {
					tab.closeGraphs();
				}
			}
			if (comp instanceof MyMeasurementsTab) {
				MyMeasurementsTab tab = (MyMeasurementsTab) comp;
				if (tab != null) {
					tab.closeGraphs();
				}
			}
		}
	}

}
