package gui;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;

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
//	ModuleTab experimentTab;
//	ModuleTab ragExperimentTab;
//	HealthTab healthTab;
//	CameraTab cameraTab;
//	HerciHSTab herciTab;
	MyMeasurementsTab measurementsTab;
//	ModuleTab wodExperimentTab;
//	HealthTab wodHealthTab;
	
	// We have one health thread per health tab
//	ArrayList<Thread> tabThreads;
	Thread healthThread;
	// We have one radiation thread and camera thread per Radiation Experiment/Camera tab
	Thread experimentThread;
	Thread ragExperimentThread;
	Thread cameraThread;
	Thread herciThread;
	Thread measurementThread;
	Thread wodHealthThread;
	Thread wodExperimentThread;
	
	public SpacecraftTab(Spacecraft s) {
		sat = s;
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setLayout(new BorderLayout(0, 0));
		add(tabbedPane, BorderLayout.CENTER);
//		tabThreads = new ArrayList<Thread>();
		addHealthTabs();
		addMeasurementsTab(sat);
	}
	
	public void showGraphs() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
		   ModuleTab tab = (ModuleTab) tabbedPane.getTabComponentAt(i);
		   if (tab != null)
			   tab.showGraphs();
		}
	}

	public void refreshXTabs(FoxSpacecraft fox, boolean closeGraphs) {
		closeTabs(fox, closeGraphs);
		createTabs(fox);
	}
	
	public void closeTabs(FoxSpacecraft fox, boolean closeGraphs) {
		sat = fox;
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			ModuleTab tab = (ModuleTab) tabbedPane.getTabComponentAt(i);
			if (tab != null) {
				if (closeGraphs) tab.closeGraphs();
				tab.remove(tab);
			}
		}
	}
	
	public void createTabs(Spacecraft fox) {
		addHealthTabs();		
		addMeasurementsTab(sat);
	}

	public void stop() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			ModuleTab tab = (ModuleTab) tabbedPane.getTabComponentAt(i);
			if (tab != null) {
				stopThreads(tab);
			}
		}
	}
	
	private void addHealthTabs() {
		stop();
		
		HealthTabRt healthTab = new HealthTabRt((FoxSpacecraft)sat);
		healthThread = new Thread(healthTab);
		healthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		healthThread.start();
//		tabThreads.add(healthThread);

		String HEALTH = "Health";
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ HEALTH + "</b></body></html>", healthTab );

		if (sat.getLayoutIdxByName(Spacecraft.WOD_LAYOUT) != Spacecraft.ERROR_IDX) {
			try {
				addWodTab((FoxSpacecraft)sat);
			} catch (Exception e) {
				e.printStackTrace(Log.getWriter());
				Log.errorDialog("Layout Failure", "Failed to setup Whole Orbit Data tab for sat: " + sat.user_display_name 
						+ "\nCheck the Spacecraft.dat file and remove this layout if it is not valid\n" + e);
			}

		}

		for (int exp : ((FoxSpacecraft)sat).experiments) {
			if (exp == FoxSpacecraft.EXP_VANDERBILT_LEP)
				try {
					addExperimentTab((FoxSpacecraft)sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}
			
			if (exp == FoxSpacecraft.RAG_ADAC)
				try {
					addRagExperimentTab((FoxSpacecraft)sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup Ragnaroc Experiment tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove the experiement if it is not valid\n"+e);
				}

			if (exp == FoxSpacecraft.EXP_VT_CAMERA || exp == FoxSpacecraft.EXP_VT_CAMERA_LOW_RES)
				try {
					addCameraTab((FoxSpacecraft)sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup VT Camera tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				}

			if (exp == FoxSpacecraft.EXP_IOWA_HERCI) {
				try {
					addHerciHSTab((FoxSpacecraft)sat);
					addHerciLSTab((FoxSpacecraft)sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup IOWA HERCI tabs for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				}

			}
			if (exp == FoxSpacecraft.EXP_UW)
				try {
					addUwExperimentTab((FoxSpacecraft)sat);
				} catch (Exception e) {
					e.printStackTrace(Log.getWriter());
					Log.errorDialog("Layout Failure", "Failed to setup UW Experiement tab for sat: " + sat.user_display_name 
							+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				}
		}
		if (sat.getLayoutIdxByName(Spacecraft.WOD_RAD_LAYOUT) != Spacecraft.ERROR_IDX) {
			try {
			addWodExpTab((FoxSpacecraft)sat);
			} catch (Exception e) {
				e.printStackTrace(Log.getWriter());
				Log.errorDialog("Layout Failure", "Failed to setup WOD Experiment tab for sat: " + sat.user_display_name 
						+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
			}

		}
		if (sat.getLayoutIdxByName(Spacecraft.WOD_CAN_LAYOUT) != Spacecraft.ERROR_IDX) {
			try {
			addUwWodExperimentTab((FoxSpacecraft)sat);
			} catch (Exception e) {
				e.printStackTrace(Log.getWriter());
				Log.errorDialog("Layout Failure", "Failed to setup UW WOD tab for sat: " + sat.user_display_name 
						+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
			}

		}
		if (sat.getLayoutIdxByName(Spacecraft.WOD_RAG_LAYOUT) != Spacecraft.ERROR_IDX) {
			try {
			addWodRagExpTab((FoxSpacecraft)sat);
			} catch (Exception e) {
				e.printStackTrace(Log.getWriter());
				Log.errorDialog("Layout Failure", "Failed to setup WOD Experiment tab for sat: " + sat.user_display_name 
						+ "\nCheck the Spacecraft.dat file and remove this experiement if it is not valid\n"+e);
				
			}

		}
	}

	private void addWodTab(FoxSpacecraft fox) {
		
		WodHealthTab wodHealthTab = new WodHealthTab((FoxSpacecraft)sat);
		wodHealthThread = new Thread(wodHealthTab);
		wodHealthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodHealthThread.start();
//		tabThreads.add(wodHealthThread);

		String WOD = "WOD";
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ WOD + "</b></body></html>", wodHealthTab );
	}
	
	private void addWodExpTab(FoxSpacecraft fox) {
		WodVulcanTab wodExperimentTab = new WodVulcanTab(fox);
		wodExperimentThread = new Thread((VulcanTab)wodExperimentTab);
		wodExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodExperimentThread.start();
//		tabThreads.add(wodExperimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "VU Rad WOD" + "</b></body></html>", wodExperimentTab );

	}

	private void addExperimentTab(FoxSpacecraft fox) {
		
		VulcanTab experimentTab = new VulcanTab(fox, DisplayModule.DISPLAY_VULCAN);
		experimentThread = new Thread((VulcanTab)experimentTab);
		experimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		experimentThread.start();
//		tabThreads.add(experimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" VU Rad ("+ fox.getIdString() + ")</body></html>", experimentTab );

	}
	
	private void addRagExperimentTab(FoxSpacecraft fox) {

		NamedExperimentTab ragExperimentTab = new NamedExperimentTab(fox, "Ragnaroc Attitude Determination and Control", 
				fox.getLayoutByName(Spacecraft.RAG_LAYOUT),
				fox.getLayoutByName(Spacecraft.RAG_LAYOUT), DisplayModule.DISPLAY_VULCAN);
		ragExperimentThread = new Thread(ragExperimentTab);
		ragExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		ragExperimentThread.start();
//		tabThreads.add(ragExperimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		"ADAC</body></html>", ragExperimentTab);

	}
	
	private void addWodRagExpTab(FoxSpacecraft fox) {
		WodNamedExperimentTab wodExperimentTab = new WodNamedExperimentTab(fox, "Ragnaroc Attitude Determination and Control", 
				fox.getLayoutByName(Spacecraft.WOD_RAG_LAYOUT),
				fox.getLayoutByName(Spacecraft.WOD_RAG_LAYOUT), DisplayModule.DISPLAY_VULCAN);
		wodExperimentThread = new Thread(wodExperimentTab);
		wodExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodExperimentThread.start();
//		tabThreads.add(wodExperimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "ADAC WOD" + "</b></body></html>", wodExperimentTab );

	}
	
	private void addUwExperimentTab(FoxSpacecraft fox) {

		UwExperimentTab experimentTab = new UwExperimentTab(fox, DisplayModule.DISPLAY_UW);
		experimentThread = new Thread((UwExperimentTab)experimentTab);
		experimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		experimentThread.start();
//		tabThreads.add(experimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		"CAN Pkts</body></html>", experimentTab);

	}

	private void addUwWodExperimentTab(FoxSpacecraft fox) {
		WodUwExperimentTab wodExperimentTab = new WodUwExperimentTab(fox);
		wodExperimentThread = new Thread((WodUwExperimentTab)wodExperimentTab);
		wodExperimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodExperimentThread.start();
//		tabThreads.add(wodExperimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "CAN Pkt WOD" + "</b></body></html>", wodExperimentTab );

	}

	private void addHerciLSTab(FoxSpacecraft fox) {

		HerciLSTab experimentTab = new HerciLSTab(fox);
		experimentThread = new Thread((HerciLSTab)experimentTab);
		experimentThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		experimentThread.start();
//		tabThreads.add(experimentThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI HK ("+ fox.getIdString() + ")</body></html>", experimentTab);

	}
	
	private void addHerciHSTab(FoxSpacecraft fox) {
		HerciHSTab herciTab = new HerciHSTab(fox);
		herciThread = new Thread(herciTab);
			
		herciThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		herciThread.start();
//		tabThreads.add(herciThread);

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI ("+ fox.getIdString() + ")</body></html>", herciTab);
	}
	
	private void addCameraTab(FoxSpacecraft fox) {

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
			ModuleTab tab = (ModuleTab) tabbedPane.getTabComponentAt(i);
			if (tab != null) {
				tab.closeGraphs();
			}
		}
//		healthTab.closeGraphs();
//		if (experimentTab != null)
//			experimentTab.closeGraphs();
//		if (ragExperimentTab != null)
//			ragExperimentTab.closeGraphs();
//		if (wodHealthTab != null)
//			wodHealthTab.closeGraphs();
//		if (wodExperimentTab != null)
//			wodExperimentTab.closeGraphs();
//		if (herciTab != null)
//			herciTab.closeGraphs();
//		
//		measurementsTab.closeGraphs();
	}

}
