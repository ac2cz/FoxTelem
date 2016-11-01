package gui;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import common.Config;
import common.FoxSpacecraft;
import common.Log;
import common.Spacecraft;

/**
* 
* FOX 1 Telemetry Decoder
* @author chris.e.thompson g0kla/ac2cz
*
* Copyright (C) 2015 amsat.org
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
*/
@SuppressWarnings("serial")
public class SpacecraftTab extends JPanel {

	Spacecraft sat;
	JTabbedPane tabbedPane;
	ModuleTab radiationTab;
	HealthTab healthTab;
	CameraTab cameraTab;
	HerciHSTab herciTab;

	// We have one health thread per health tab
	Thread healthThread;
	// We have one radiation thread and camera thread per Radiation Experiment/Camera tab
	Thread radiationThread;
	Thread cameraThread;
	Thread herciThread;

	public SpacecraftTab(Spacecraft s) {
		sat = s;
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setLayout(new BorderLayout(0, 0));
		add(tabbedPane, BorderLayout.CENTER);
		addHealthTabs();
	}
	
	public void showGraphs() {
		healthTab.showGraphs();
		radiationTab.showGraphs();
		herciTab.showGraphs();
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

		addHealthTabs();
	}

	private void addHealthTabs() {
		stopThreads(healthTab);
		stopThreads(radiationTab);
		stopThreads(cameraTab);
		stopThreads(herciTab);

		healthTab = new HealthTab(sat);
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
	}

	private void addExperimentTab(FoxSpacecraft fox) {
		
		radiationTab = new VulcanTab(fox);
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
		if (herciTab != null)
			herciTab.closeGraphs();
	}

}
