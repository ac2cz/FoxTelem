package common;

import gui.MainWindow;
import telemetry.SatPayloadStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
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
 * This class reads configuration from the amsat server to determine if an upgrade is
 * available or required.
 * 
 * It also reads configuration values from the server that overwrite local values if they are different.
 * 
 */

public class UpdateManager implements Runnable {

	private final static long CHECK_PERIOD = 1*60*60*1000; // check every hour for changes
	private final static long SERVER_UPDATE_PERIOD = 4*60*60*1000; // check every 4 hours for server changes
	private final static long T0_UPDATE_PERIOD = 1 * 60*60* 1000; // check every hour to see if T0 changed
	public final static long KEP_UPDATE_PERIOD = 7*24*60*60*1000; // check every 7 days for TLE changes
	private boolean server = false;
	
	public UpdateManager(boolean server) {
		this.server = server;
	}
	
	private void updateServerParams() throws IOException {
		if (Config.serverParamsUrl != null) {
			BufferedReader in = null;
			Properties serverProperties = new Properties();
			try {
				Log.println("Reading server params from: "+ Config.serverParamsUrl);
				URL server = new URL(Config.serverParamsUrl);
				in = new BufferedReader(
						new InputStreamReader(server.openStream()));

				//String availableVersion;
				//availableVersion = in.readLine(); // read the first line

				serverProperties.load(in);
			} finally {
				if (in != null) in.close();
			}
			
			try {
				Log.println("Setting server params to: ");
				String primary = serverProperties.getProperty("primaryServer");
				if (primary != null) {
					Config.primaryServer = primary;
					Log.println("Primary set to: " + Config.primaryServer);
				}
				String secondary = serverProperties.getProperty("secondaryServer");
				if (secondary != null) {
					Config.secondaryServer = secondary;
					Log.println("Secondary set to: " + Config.secondaryServer);
				}
				String website = serverProperties.getProperty("webSiteUrl");
				if (website != null) {
					Config.webSiteUrl = website;
					Log.println("Website UTL set to: " + Config.webSiteUrl);
				}
				Boolean both = Boolean.parseBoolean(serverProperties.getProperty("sendToBothServers"));
				if (both != null) {
					Config.sendToBothServers = both;
					Log.println("Sent to both set to: "+Config.sendToBothServers);
				}
				String port = serverProperties.getProperty("serverPort");
				if (port != null) {
					Config.serverPort = Integer.parseInt(port);
					Log.println("Port set to: "+Config.serverPort);
				}
				String protocol = serverProperties.getProperty("serverProtocol");
				if (protocol != null) {
					Config.serverProtocol = Integer.parseInt(protocol);
					Log.println("Protocol set to: "+Config.serverProtocol);
				}
			} catch (NumberFormatException nf) {
				Log.println("Could not load the server paramaters: " + nf.getMessage());
			} catch (NullPointerException nf) {
				Log.println("Could not load the server paramaters: " + nf.getMessage());
			}
		}
	}
	
	public void deleteT0(Spacecraft sat) {
		String file = sat.series + sat.foxId + Config.t0UrlFile;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + file;			
		}
		Log.println("Deleting: " + file);
		try {
			SatPayloadStore.remove(file);
			sat.loadTimeZeroSeries(null);
		} catch (IOException e) {
			Log.println("Could not delete T0 file: " + file);
			e.printStackTrace(Log.getWriter());
		}
	}
		
	public void updateT0(Spacecraft sat) {
		String urlString = Config.webSiteUrl+ Config.t0UrlPath + sat.series + sat.foxId + Config.t0UrlFile;
		String file = sat.series + sat.foxId + Config.t0UrlFile;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + file;			
		}
		Log.println("Trying to download: " + urlString);
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
			File f1 = new File(file + ".tmp");
			File f2 = new File(file);
			if (sat.loadTimeZeroSeries(file + ".tmp")) {
				// this is a good file so we can now use it as the default
				SatPayloadStore.remove(file);
				
				SatPayloadStore.copyFile(f1, f2);
				SatPayloadStore.remove(file + ".tmp");
				return;
			} else {
				SatPayloadStore.remove(file + ".tmp");
				if (f2.exists()) 
					sat.loadTimeZeroSeries(file); // load the existing
				else
					sat.loadTimeZeroSeries(null); // load the default
			}
		} catch (MalformedURLException e) {
			Log.println("Invalid location for T0 file: " + file);
			//e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("Could not write T0 file: " + file);
			//e.printStackTrace(Log.getWriter());
		} catch (IndexOutOfBoundsException e) {
			Log.println("T0 file is corrupt - likely missing reset in sequence or duplicate reset: " + file);
			//e.printStackTrace(Log.getWriter());
		} finally {
			try {
				if (fos != null) fos.close();
				if (rbc != null) rbc.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private void checkVersion() throws IOException {
		URL oracle = new URL(Config.newVersionUrl);
        BufferedReader in = new BufferedReader(
        new InputStreamReader(oracle.openStream()));

        String availableVersion;
        availableVersion = in.readLine(); // read the first line
        String line;
        String notes = "";
        try {
        	while ((line = in.readLine()) != null) {
        		notes = notes + line + "\n";
        	}
        	Log.println("LATEST VERSION: "+availableVersion);
        	try {
        		int maj = Config.parseVersionMajor(availableVersion);
        		int min = Config.parseVersionMinor(availableVersion);
        		//String point = Config.parseVersionPoint(availableVersion);
        		//System.out.println("MAJ: "+maj);
        		System.out.println("AVAIL MIN: "+min);
        		//System.out.println("POINT: "+point);

        		System.out.println("CURRENT MIN: "+Config.getVersionMinor() );
        		
        		if (Config.getVersionMajor() < maj) requireUpgrade(availableVersion, notes);
        		if (Config.getVersionMajor() == maj && Config.getVersionMinor() < min) recommendUpgrade(availableVersion, notes);
        	} catch (NumberFormatException e) {
        		e.printStackTrace(Log.getWriter());
        		Log.println("Error parsing the latest version information.  Abandoning the check");
        	} 
        } finally {
        	if (in != null) in.close();
        }
	}

	private void recommendUpgrade(String ver, String notes) {
		String message = "Version " +ver+ " of FoxTelem is available!  Do you want to go to amsat.us/FoxTelem/ to download it?\n"
				+ "Release information:\n" + notes;
		Object[] options = {"Yes",
		"No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message,
				"New FoxTelem Version Available",
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.INFORMATION_MESSAGE,
				null,
				options,
				options[1]);

		if (n == JOptionPane.YES_OPTION) {
			gotoSite();			
		}
	}
	
	private void requireUpgrade(String ver, String notes) {
		String message = "You must upgrade to FoxTelem Version " +ver + "  Do you want to go to amsat.us/FoxTelem/ to download it?\n"
				+ "Release information:\n" + notes;
		Object[] options = {"Yes",
		"No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message,
				"REQUIRED UPGRADE",
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[1]);

		if (n == JOptionPane.NO_OPTION) {
			System.exit(0);
		}
		gotoSite();
		System.exit(0);
	}
	
	private void gotoSite() {
		String url = "http://amsat.us/FoxTelem/windows";
		if (Config.isMacOs())
			url = "http://amsat.us/FoxTelem/mac";
		if (Config.isLinuxOs())
			url = "http://amsat.us/FoxTelem/linux";
		try {
			DesktopApi.browse(new URI(url));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void updateKeps() {
		Config.satManager.fetchTLEFile();
	}
	
	boolean worldHasNotEnded = true;
	
	@Override
	public void run() {
	
		// Check the server paramaters first so that the config is quickly updated
		if (!server)
			try {
				updateServerParams();
			} catch (IOException e1) {
				Log.println("Can not read the server paramaters, skipping");
				e1.printStackTrace(Log.getWriter());
			}
		
		// Update Keps is called at startup by the SatelliteManager.  This just calls it periodically if FoxTelem left running.
		if (!server)
		if (Config.downloadT0FromServer) {
			ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
			for (int i=0; i<sats.size(); i++) {
				//if (sats.get(i).isFox1())
					updateT0(sats.get(i));
			}
		}

		if (!server)
			try {
				checkVersion();
			} catch (IOException e1) {
				Log.println("Can not read the latest version, skipping");
				e1.printStackTrace(Log.getWriter());
			}

		long elapsed = 0;
		while (worldHasNotEnded) {
			try {
				Thread.sleep(CHECK_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
			elapsed = elapsed + CHECK_PERIOD;
			
			if (!server)
				if (elapsed % SERVER_UPDATE_PERIOD == 0) {
					try {
						updateServerParams();
					} catch (IOException e1) {
						Log.println("Can not read the server paramaters, skipping");
						e1.printStackTrace(Log.getWriter());
					}
				}
			
			if (!server)
				if (elapsed % T0_UPDATE_PERIOD == 0) {
					if (Config.downloadT0FromServer) {
						ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
						for (int i=0; i<sats.size(); i++) {
							//if (sats.get(i).isFox1()) {
							Spacecraft fox = sats.get(i);
								if (fox.hasFixedReset) {
									updateT0(sats.get(i));
								}
							//}
						}
					}	
				}
			
			if (elapsed % KEP_UPDATE_PERIOD == 0) {
				if (Config.foxTelemCalcsPosition)
					updateKeps();
				elapsed = 0;
			}
				
		}
	}

}
