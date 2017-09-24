package gui;

import java.awt.BorderLayout;
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
	ModuleTab radiationTab;
	HealthTab healthTab;
	CameraTab cameraTab;
	HerciHSTab herciTab;
	MyMeasurementsTab measurementsTab;
	ModuleTab wodRadiationTab;
	HealthTab wodHealthTab;
	
	// We have one health thread per health tab
	Thread healthThread;
	// We have one radiation thread and camera thread per Radiation Experiment/Camera tab
	Thread radiationThread;
	Thread cameraThread;
	Thread herciThread;
	Thread measurementThread;
	Thread wodHealthThread;
	Thread wodRadiationThread;
	
	public SpacecraftTab(Spacecraft s) {
		sat = s;
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setLayout(new BorderLayout(0, 0));
		add(tabbedPane, BorderLayout.CENTER);
		addHealthTabs();
		addMeasurementsTab(sat);
	}
	
	public void showGraphs() {
		healthTab.showGraphs();
		radiationTab.showGraphs();
		herciTab.showGraphs();
		measurementsTab.showGraphs();
		wodHealthTab.showGraphs();
		wodRadiationTab.showGraphs();
	}

	public void refreshTabs(boolean closeGraphs) {
		
		if (closeGraphs) healthTab.closeGraphs();
		tabbedPane.remove(healthTab);

		if (closeGraphs) radiationTab.closeGraphs();
		tabbedPane.remove(radiationTab);

		if (herciTab != null)
			if (closeGraphs) herciTab.closeGraphs();
		tabbedPane.remove(herciTab);

		if (cameraTab != null)
			tabbedPane.remove(cameraTab);
		
		if (wodHealthTab != null)
		if (closeGraphs) wodHealthTab.closeGraphs();
		tabbedPane.remove(wodHealthTab);

		if (wodRadiationTab != null)
		if (closeGraphs) wodRadiationTab.closeGraphs();
		tabbedPane.remove(wodRadiationTab);
		addHealthTabs();
		
		if(closeGraphs)
			measurementsTab.closeGraphs();
		tabbedPane.remove(measurementsTab);
		addMeasurementsTab(sat);
	}

	private void addHealthTabs() {
		stopThreads(healthTab);
		stopThreads(radiationTab);
		stopThreads(cameraTab);
		stopThreads(herciTab);
		stopThreads(wodHealthTab);
		stopThreads(wodRadiationTab);
		
		healthTab = new HealthTabRt(sat);
		healthThread = new Thread(healthTab);
		healthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		healthThread.start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Health" + "</b></body></html>", healthTab );

		if (sat.isFox1()) {
			for (int exp : ((FoxSpacecraft)sat).experiments) {
				if (exp == FoxSpacecraft.EXP_VULCAN)
					addExperimentTab((FoxSpacecraft)sat);
				if (exp == FoxSpacecraft.EXP_VT_CAMERA || exp == FoxSpacecraft.EXP_VT_CAMERA_LOW_RES)
					addCameraTab((FoxSpacecraft)sat);
				if (exp == FoxSpacecraft.EXP_IOWA_HERCI) {
					addHerciHSTab((FoxSpacecraft)sat);
					addHerciLSTab((FoxSpacecraft)sat);
				}
			}
		}
		if (sat.foxId == Spacecraft.FOX1E) {
			addWodTabs((FoxSpacecraft)sat);
		}
	}

	private void addWodTabs(FoxSpacecraft fox) {
		
		wodHealthTab = new WodHealthTab(sat);
		wodHealthThread = new Thread(wodHealthTab);
		wodHealthThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodHealthThread.start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "WOD" + "</b></body></html>", wodHealthTab );
		
		wodRadiationTab = new WodVulcanTab(fox);
		wodRadiationThread = new Thread((VulcanTab)wodRadiationTab);
		wodRadiationThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		wodRadiationThread.start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "VU Rad WOD" + "</b></body></html>", wodRadiationTab );

	}

	private void addExperimentTab(FoxSpacecraft fox) {
		
		radiationTab = new VulcanTab(fox, DisplayModule.DISPLAY_VULCAN);
		radiationThread = new Thread((VulcanTab)radiationTab);
		radiationThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		radiationThread.start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" VU Rad ("+ fox.getIdString() + ")</body></html>", radiationTab );

	}

	private void addHerciLSTab(FoxSpacecraft fox) {

		radiationTab = new HerciLSTab(fox);
		radiationThread = new Thread((HerciLSTab)radiationTab);
		radiationThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		radiationThread.start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI HK ("+ fox.getIdString() + ")</body></html>", radiationTab);

	}
	
	private void addHerciHSTab(FoxSpacecraft fox) {
		herciTab = new HerciHSTab(fox);
		herciThread = new Thread(herciTab);
			
		herciThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		herciThread.start();

		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1>" + 
		" HERCI ("+ fox.getIdString() + ")</body></html>", herciTab);
	}
	
	private void addCameraTab(FoxSpacecraft fox) {

		cameraTab = new CameraTab(fox);
		cameraThread = new Thread(cameraTab);
		cameraThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		cameraThread.start();

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
		measurementThread = new Thread(measurementsTab);
		measurementThread.setUncaughtExceptionHandler(Log.uncaughtExHandler);
		measurementThread.start();
		
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
		healthTab.closeGraphs();
		if (radiationTab != null)
			radiationTab.closeGraphs();
		if (wodHealthTab != null)
			wodHealthTab.closeGraphs();
		if (wodRadiationTab != null)
			wodRadiationTab.closeGraphs();
		if (herciTab != null)
			herciTab.closeGraphs();
		
		measurementsTab.closeGraphs();
	}

}
