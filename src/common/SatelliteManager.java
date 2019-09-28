package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import FuncubeDecoder.FUNcubeSpacecraft;
import gui.MainWindow;
import gui.ProgressPanel;
import predict.FoxTLE;
import predict.PositionCalcException;
import predict.SortedTleList;
import telemetry.BitArrayLayout;
import telemetry.FrameLayout;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;

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
public class SatelliteManager implements Runnable {
	
	public static final String AMSAT_NASA_ALL = "http://www.amsat.org/amsat/ftp/keps/current/nasabare.txt";
	public boolean updated = true; // true when we have first been created or the sats have been updated and layout needs to change
	
	public ArrayList<Spacecraft> spacecraftList = new ArrayList<Spacecraft>();
	
	public SatelliteManager()  {
		init();
		if (Config.foxTelemCalcsPosition)
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
							int n = Log.optionYNdialog("Hit yes to install or No to skip?",
									"Install: " + targetName + "\n\nDo you want to install the spacecraft file:\n" + targetFile);

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
							int n = Log.optionYNdialog("Overwrite Existing spacecraft config file",
									"There is a newer spacecraft file available in the installation directory. You should replace your local file.\n"
									+ "Local changes you have made to the spacecraft, such as Freqency Bounds, will be preserved.\n"
									+ "Existing File ("+targetDate+"): " + targetFile.getPath() +"\nwill be replaced with\n"
									+ "Master Copy ("+masterDate+"): " + listOfFiles[i].getPath());
										
							if (n == JOptionPane.NO_OPTION) {
								Log.println("Leaving existing spacecraft file: " + targetFile.getName());
							} else {
								Log.println("Copying spacecraft file: " + listOfFiles[i].getName() + " to " + targetFile.getName());
								try {
									// Temporarily try to load this to init the user paramaters if they have not already been copied over
									try {
										@SuppressWarnings("unused")
										FoxSpacecraft satellite = new FoxSpacecraft(targetFile);
									} catch (LayoutLoadException e) {
										// But ingnore any errors.  Hopefully the new MASTER file will fix it!
										e.printStackTrace(Log.getWriter()); // but log if user has that enabled
									}
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
			Log.infoDialog("SPACECRAFT FILES INSTALLATION", "The configuration files for the spacecraft will be copied to: \n" 
					+ folder.getAbsolutePath() + "\n\n"
					+ "You will be prompted to install each file.  If you are running multiple copies of FoxTelem, \n"
					+ "then only install the file(s) you need.\n\n "
					+ "You can also delete or add spacecraft later from the spacecraft menu\n\n"
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
//						int frameLayouts = satellite.numberOfFrameLayouts;
//						if (frameLayouts > 0) {
//							Log.println("Frame Layouts: " + frameLayouts);
//							for (int k=0; k < frameLayouts; k++) {
//								Log.print(" : " + satellite.frameLayout[k].name);
//								Log.print(" : " + satellite.frameLayout[k].getInt(FrameLayout.NUMBER_OF_PAYLOADS) + " payloads");
//								Log.println("");
//							}
//						}
						
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
		Collections.sort((List<Spacecraft>)spacecraftList);
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
	
	public BitArrayLayout getLayoutByCanId(int sat, int id) {
		if (!validFoxId(sat)) return null;
		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
		if (sc != null) return sc.getLayoutByCanId(id);
		return null;
	}

	public FrameLayout getFrameLayout(int sat, int type) {
		if (!validFoxId(sat)) return null;
		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
		if (sc != null) return sc.frameLayout[type];
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

	public boolean haveSpacecraftDisplayName(String name) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).user_display_name.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	public Spacecraft getSpacecraftByKepsName(String name) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).user_keps_name.equalsIgnoreCase(name))
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
	
	/**
	 * We consider a valid Fox-Id to be 1 - 9
	 * @param id
	 * @return
	 */
	public boolean validFoxId(int id) {
		if (id > 0 && id <= Spacecraft.MAX_FOXID) return true;
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
		String filetmp = file + ".tmp";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasabare.txt";
			filetmp = file + ".tmp";
		}
		File f1 = new File(filetmp);
		File f2 = new File(file);
		Date lm = new Date(f2.lastModified());
		//Date now = new Date();

		String msg = "Downloading new keps ...                 ";
		ProgressPanel initProgress = null;
		if (Log.showGuiDialogs) {
			initProgress = new ProgressPanel(MainWindow.frame, msg, false);
			initProgress.setVisible(true);
		}
		Log.println("Downloading new keps ..");
		URL website;
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			website = new URL(urlString);
			HttpURLConnection httpCon = (HttpURLConnection) website.openConnection();
			long date = httpCon.getLastModified();
			httpCon.disconnect();
			Date kepsDate = new Date(date);
			if (kepsDate.getTime() <= lm.getTime()) { // then dont try to update it
				Log.println(".. keps are current");
				filetmp = file;
			} else {
				Log.println(" ... open RBC ..");
				rbc = Channels.newChannel(website.openStream());
				Log.println(" ... open output file .." + filetmp);
				fos = new FileOutputStream(filetmp);
				Log.println(" ... getting file ..");
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				Log.println(" ... closing outpt stream ..");
				fos.close();
				Log.println(" ... closing input stream ..");
				rbc.close();
			}
			// Always process the file because it is quick and the user may have changed the name of a spacecraft
			// The code throws away duplicate keps with the same epoch
			// Now lets see if we have a good file.  If we did not, it will throw an exception
			Log.println(" ... parsing file ..");
			parseTleFile(filetmp);
			// this is a good file so we can now use it as the default
			Log.println(" ... remove and rename ..");
			if (!file.equalsIgnoreCase(filetmp)) {
				// We downloaded a new file so rename tmp as the new file
				SatPayloadStore.remove(file);
				SatPayloadStore.copyFile(f1, f2);
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!file.equalsIgnoreCase(filetmp))
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
			if (Log.showGuiDialogs) 
				initProgress.updateProgress(100);
			try {
				if (fos != null) fos.close();
				if (rbc != null) rbc.close();
			} catch (IOException e) {
				// ignore
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
				Spacecraft spacecraft = this.getSpacecraftByKepsName(name);
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

	boolean running = true;
	boolean done = false;
	
	public void stop() {
		running = false;
		while (!done) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		// Runs until we exit
		while(running) {
			// Sleep first to avoid race conditions at start up
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (Config.foxTelemCalcsPosition) {
				// Calculate the sat positions, which caches them in each sat
				for (int s=0; s < spacecraftList.size(); s++) {
					Spacecraft sat = spacecraftList.get(s);
					if (sat.user_track) {
						if (Config.GROUND_STATION != null)
							if (Config.GROUND_STATION.getLatitude() == 0 && Config.GROUND_STATION.getLongitude() == 0) {
								// We have a dummy Ground station which is fine for sat position calc but not for Az, El calc.
								sat.user_track = false;
								sat.save();
								Log.errorDialog("MISSING GROUND STATION", "FoxTelem is configured to calculate the spacecraft position, but your ground station\n"
										+ "is not defined.  Go to the settings tab and setup the ground station position or turn of calculation of the spacecraft position.\n"
										+ "Tracking will be disabled for " + sat.user_display_name + ".");
								sat.satPos = null;
							} else {
								try {
									sat.calcualteCurrentPosition();
								} catch (PositionCalcException e) {
									// We wont get NO T0 as we are using the current time, but we may have missing keps
							/*		if (running) { // otherwise we reset the sats and another copy of this thread will deal with the issue
										sat.track = false;
										sat.save();
										String scd = Config.getLogFileDirectory() + "spacecraft\\";
										Log.errorDialog("MISSING TLE", "FoxTelem is configured to calculate the spacecraft position, but no TLE was found for "
												+ sat.name +".\nMake sure the name of the spacecraft matches the name of the satellite in the nasabare.tle\n "
												+ "file from amsat.  This file is automatically downloaded from: \nhttp://www.amsat.org/amsat/ftp/keps/current/nasabare.txt\n"
												+ "TLE for this spacecraft is copied from nasabare.txt into the file:\n"+scd+"FOX"+ sat.foxId + ".tle.  It may be missing or corrupt.\n"
												+ "Tracking will be disabled for this spacecraft. \n\n "
												+ "You can still use 'Find Signal' for this spacecraft if you turn off position calculation on the settings panel and \n"
												+ "uncheck 'Fox Telem calculates position'.  Then re-enable tracking.");
												
										sat.satPos = null;
									}
									*/
									sat.satPos = null;
								}
							}
					}
				}
			}

		}
		done = true;
	}

}
