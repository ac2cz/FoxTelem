package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import FuncubeDecoder.FUNcubeSpacecraft;
import gui.MainWindow;
import predict.FoxTLE;
import predict.SortedTleList;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;
import uk.me.g4dpz.satellite.TLE;

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
	
	public static final String AMSAT_NASA_ALL = "http://www.amsat.org/amsat/ftp/keps/current/nasabare.txt";
	
	public ArrayList<Spacecraft> spacecraftList = new ArrayList<Spacecraft>();
	
	public SatelliteManager()  {
		init();
		fetchTLEFile();
	}
	
	public void init() {
		File masterFolder = new File(Config.currentDir + File.separator + FoxSpacecraft.SPACECRAFT_DIR);
		File folder = getFolder(masterFolder);
		//File folder = new File("spacecraft");
		loadSats(folder);
	}
	
	/**
	 * We check to see if we already have .dat files in the local directory.  If we have none, then we ask if each should be installed from the MASTER
	 * installation folder.  If we already have some then the timestamps on those files are checked and the user is warned if there are newer files
	 * in the spacecraft folder.  This would typically only be the case the first time FoxTelem is run after a new installation.
	 * 
	 * @param masterFolder
	 * @param folder
	 */
	private void copyDatFiles(File masterFolder, File folder) {
		File[] listOfFiles = masterFolder.listFiles();
		// First check to see if we have any local .dat files
		boolean haveDatFiles = false;
		File[] targetFiles = folder.listFiles();
		if (targetFiles != null) {
			for (int i = 0; i < targetFiles.length; i++) {
				if (targetFiles[i].isFile() && targetFiles[i].getName().endsWith(".dat")) {
					haveDatFiles = true;
				}
			}
		}
		
		
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".MASTER")) {
					Log.println("Checking spacecraft file: " + listOfFiles[i].getName());
					String targetName = listOfFiles[i].getName().replace(".MASTER", ".dat");
					File targetFile = new File(folder + File.separator + targetName);
					if(!targetFile.exists()){
						// Missing this file
						if (!haveDatFiles) {
							// And we are doing a new install, so as the user what to do
							Object[] options = {"Yes",
							"No"};
							int n = Log.optionYNdialog("Hit yes to install or No to skip?",
									"Do you want to install the spacecraft file for: " + targetFile);

							if (n == JOptionPane.YES_OPTION) {
								Log.println("Copying spacecraft file: " + listOfFiles[i].getName() + " to " + targetFile.getName());
								try {
									SatPayloadStore.copyFile(listOfFiles[i], targetFile);
								} catch (IOException e) {
									Log.errorDialog("ERROR", "Can't copy spacecraft file: " + listOfFiles[i].getName() + " to " + targetFile.getName() +"\n"+ e.getMessage());
									e.printStackTrace();
								}
							}
						}
					} else {
						// It exists, but maybe it is not the latest.  Check the timestamp and warn the user if we have a later one
						if (targetFile.lastModified() < listOfFiles[i].lastModified()) {
							Date targetDate = new Date(targetFile.lastModified());
							Date masterDate = new Date(listOfFiles[i].lastModified());
							Object[] options = {"Yes",
					        "No"};
							int n = Log.optionYNdialog("Overwrite Existing spacecraft config file",
									"There is a newer spacecraft file available in the installation directory. Do you want to replace your local file?\n"
									+ "The local file contains any changes you have made to the spacecraft, such as Freqency Bounds.\n"
									+ "Existing File ("+targetDate+"): " + targetFile.getPath() +"\nwill be replaced with\n"
									+ "Master Copy ("+masterDate+"): " + listOfFiles[i].getPath());
										
							if (n == JOptionPane.NO_OPTION) {
								Log.println("Leaving existing spacecraft file: " + targetFile.getName());
							} else {
								Log.println("Copying spacecraft file: " + listOfFiles[i].getName() + " to " + targetFile.getName());
								try {
									SatPayloadStore.copyFile(listOfFiles[i], targetFile);
								} catch (IOException e) {
									Log.errorDialog("ERROR", "Can't copy spacecraft file: " + listOfFiles[i].getName() + " to " + targetFile.getName() +"\n"+ e.getMessage());
									e.printStackTrace();
								}							
							}
						} else
							Log.println("Leaving existing spacecraft file: " + targetFile.getName());
					}
				}
			}
		}

	}
	
	private File getFolder(File masterFolder) {
		File folder = new File(Config.getLogFileDirectory() + FoxSpacecraft.SPACECRAFT_DIR);
		
		if(!folder.isDirectory()){
			folder.mkdir();
			Log.infoDialog("SPACECRAFT FILES INSTALLATION", "The configuration files for the spacecraft will be copied to: \n" + folder.getAbsolutePath() + "\n"
					+ "You will be prompted to install each file.  If you are running multiple copies of FoxTelem, then only install the "
					+ "file(s) you need.\n\n You can also delete or add '.dat' files later for this logfiles directory.\n"
					+ "A master copy of the spacecraft configuration files are still stored in: \n" + masterFolder.getAbsolutePath() + "\n");
		}
		if(!folder.isDirectory()){
			Log.errorDialog("ERROR", "ERROR can't create the directory: " + folder.getAbsolutePath() +  
					"\nFoxTelem needs to save the spacecraft settings in your logfile directroy.  It is either not accessible or not writable\n");
		}
		// Now copy in any missing files
		copyDatFiles(masterFolder, folder);
		
		Log.println("Set Logfile Spacecraft directory to: " + folder);
				
		return folder;
	}
	
	private void loadSats(File folder) {
		File[] listOfFiles = folder.listFiles();
		Pattern pattern = Pattern.compile("AO-73");
		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".dat")) {
					Log.println("Loading spacecraft from: " + listOfFiles[i].getName());
					Spacecraft satellite = null;
					try {
						//FIXME - HACK FOR FCUBE
						Matcher matcher = pattern.matcher(listOfFiles[i].getName());
						if (matcher.find())
							satellite = new FUNcubeSpacecraft(listOfFiles[i]);
						else
							satellite = new FoxSpacecraft(listOfFiles[i]);
					} catch (FileNotFoundException e) {
						Log.errorDialog("ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
						e.printStackTrace(Log.getWriter());
						satellite = null;
					} catch (LayoutLoadException e) {
						Log.errorDialog("ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
						e.printStackTrace(Log.getWriter());
						satellite = null;
					} catch (IOException e) {
						Log.errorDialog("IO ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
						e.printStackTrace(Log.getWriter());
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
					Config.currentDir + File.separator + FoxSpacecraft.SPACECRAFT_DIR +
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
	public BitArrayLayout getLayoutByName(int sat, String name) {
		if (!validFoxId(sat)) return null;
		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
		if (sc != null) return sc.getLayoutByName(name);
		return null;
	}
	
	public BitArrayLayout getMeasurementLayout(int sat) {
		if (!validFoxId(sat)) return null;
		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
		if (sc != null) return sc.measurementLayout;
		return null;
	}

	public BitArrayLayout getPassMeasurementLayout(int sat) {
		if (!validFoxId(sat)) return null;
		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
		if (sc != null) return sc.passMeasurementLayout;
		return null;
	}
	
	public ArrayList<Spacecraft> getSpacecraftList() { 
		return spacecraftList; 
	} 

	public int getNumberOfSpacecraft() { return spacecraftList.size(); }

	public boolean hasCamera(int sat) {
		if (!validFoxId(sat)) return false;
		FoxSpacecraft s = (FoxSpacecraft)getSpacecraft(sat);
		return s.hasCamera();
	}

	public boolean hasHerci(int sat) {
		if (!validFoxId(sat)) return false;
		FoxSpacecraft s = (FoxSpacecraft)getSpacecraft(sat);
		return s.hasHerci();
	}

	public boolean haveSpacecraft(String name) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).name.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	public Spacecraft getSpacecraftByName(String name) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).name.equalsIgnoreCase(name))
				return spacecraftList.get(i);
		}
		return null;
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
	
	/*
	 * We Fetch a TLE file from amsat.org.  We then see if it contains TLEs for the Spacecraft we are interested in. If it does we
	 * check if there is a later TLE than the one we have.  If it is, then we append it to the TLE store for the given sat.
	 * We then load the TLEs for each Sat and store the, in the spacecraft class.  This can then be used to find the position of the spacecraft at 
	 * any time since launch
	 */

	public void fetchTLEFile() {
		Log.println("Checking for new Keps");
		String urlString = AMSAT_NASA_ALL;
		String file = FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasabare.txt";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasabare.txt";		
		}
		File f1 = new File(file + ".tmp");
		File f2 = new File(file);
		Date lm = new Date(f2.lastModified());
		Date now = new Date();
		if (now.getTime() - lm.getTime() <= UpdateManager.KEP_UPDATE_PERIOD) { // then dont try to update it
			Log.println(".. keps are current");
		} else {
			Log.println(".. downloading keps");
			URL website;
			FileOutputStream fos = null;
			ReadableByteChannel rbc = null;
			try {
				website = new URL(urlString);
				rbc = Channels.newChannel(website.openStream());
				fos = new FileOutputStream(file + ".tmp");
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				rbc.close();
				// Now lets see if we got a good file.  If we did not, it will throw an exception
				parseTleFile(file + ".tmp");
				// this is a good file so we can now use it as the default
				SatPayloadStore.remove(file);

				SatPayloadStore.copyFile(f1, f2);
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SatPayloadStore.remove(file + ".tmp");
				return;
			} catch (MalformedURLException e) {
				Log.println("Invalid location for Keps file: " + file);
				try { SatPayloadStore.remove(file + ".tmp"); } catch (IOException e1) {e1.printStackTrace();}
			} catch (IOException e) {
				Log.println("Could not write Keps file: " + file);
				try { SatPayloadStore.remove(file + ".tmp"); } catch (IOException e1) {e1.printStackTrace();}
			} catch (IndexOutOfBoundsException e) {
				Log.println("Keps file is corrupt: " + file);
				try { SatPayloadStore.remove(file + ".tmp"); } catch (IOException e1) {e1.printStackTrace();}
			} finally {
				try {
					if (fos != null) fos.close();
					if (rbc != null) rbc.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/*
	private void loadTLE() {
		String file = FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasa.all";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasa.all";		
		}
		try {
			List<TLE> TLEs = parseTleFile(file);
			for (TLE tle : TLEs) {
				String name = tle.getName();
				Spacecraft spacecraft = this.getSpacecraftByName(name);
				if (spacecraft != null) {
					Log.println("Stored TLE for: " + name);
					spacecraft.addTLE(tle);
				}
			}
		} catch (IOException e) {
			Log.errorDialog("TLE Load ERROR", "CANT PARSE the TLE file - " + file + "/n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
	}
	*/
	
	/**
	 * Parse the nasabare.txt file and make a list
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private SortedTleList parseTleFile(String filename) throws IOException {
		File f = new File(filename);
		InputStream is = new FileInputStream(f);
		try {
			SortedTleList tles = FoxTLE.importFoxSat(is);
			is.close();
			for (FoxTLE ftle : tles) {
				String name = ftle.getName();
				Spacecraft spacecraft = this.getSpacecraftByName(name);
				if (spacecraft != null) {
					Log.println("Stored TLE for: " + name);
					spacecraft.addTLE(ftle);
				}
			}
			return tles;
		} finally {
			is.close();
		}
	}
	
}
