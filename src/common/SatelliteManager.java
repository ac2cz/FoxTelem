package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;

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
 *
 * This class holds a list of the satellites and loads their details from a file.  The
 * spacecraft class then loads the telemetry layouts and any lookup tables
 * 
 * 
 */
public class SatelliteManager {
	
	ArrayList<Spacecraft> spacecraftList = new ArrayList<Spacecraft>();
	
	public SatelliteManager()  {
		File folder = new File(Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR);
		//File folder = new File("spacecraft");
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles != null) {
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".dat")) {
				Log.println("Loading spacecraft from: " + listOfFiles[i].getName());
				Spacecraft satellite = null;
				try {
					satellite = new Spacecraft(listOfFiles[i].getName());
				} catch (FileNotFoundException e) {
					Log.errorDialog("ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
					e.printStackTrace(Log.getWriter());
					satellite = null;
				} catch (LayoutLoadException e) {
					Log.errorDialog("ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
					e.printStackTrace(Log.getWriter());
					satellite = null;
				}
				if (satellite != null)
					if (getSpacecraft(satellite.foxId) != null)
						Log.errorDialog("WARNING", "Can not load two satellites with the same Fox ID.  Skipping file\n"
								+ listOfFiles[i].getName());
					else
						spacecraftList.add(satellite);
			} 
		}
		}
		if (spacecraftList.size() == 0) {
			Log.errorDialog("FATAL!", "No satellites could be loaded.  Check the spacecraft directory:\n " + 
					Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR +
					"\n and confirm it contains the "
					+ "satellite data files, their telemetry layouts and lookup tables. Program will exit");
			System.exit(1);
		}
	}
	
	/**
	 * Return the Bit Array layout for this satellite Id
	 * @param sat
	 * @return
	 */
	public BitArrayLayout getRtLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.rtLayout;
		return null;
	}

	public BitArrayLayout getMaxLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.maxLayout;
		return null;
	}

	public BitArrayLayout getMinLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.minLayout;
		return null;
	}

	public BitArrayLayout getRadLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.radLayout;
		return null;
	}

	public BitArrayLayout getRadTelemLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.rad2Layout;
		return null;
	}

	
	public BitArrayLayout getMeasurementLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.measurementLayout;
		return null;
	}

	public BitArrayLayout getPassMeasurementLayout(int sat) {
		Spacecraft sc = getSpacecraft(sat);
		if (sc != null) return sc.passMeasurementLayout;
		return null;
	}
	
	public ArrayList<Spacecraft> getSpacecraftList() { return spacecraftList; } 

	public int getNumberOfSpacecraft() { return spacecraftList.size(); }

	public boolean hasCamera(int sat) {
		Spacecraft s = getSpacecraft(sat);
		return s.hasCamera();
	}
	
	public Spacecraft getSpacecraft(int sat) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).foxId == sat)
				return spacecraftList.get(i);
		}
		return null;
	}
	
	public boolean validFoxId(int id) {
		if (id > 0 && id < 6) return true;
		return false;
	}
	
}
