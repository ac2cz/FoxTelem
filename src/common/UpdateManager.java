package common;

import gui.MainWindow;

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

	private final long SERVER_UPDATE_PERIOD = 4*60*60*1000; //1*24*60*60*1000; // check every 4 hours for server changes
	
	public UpdateManager() {
		
	}
	
	private void updateServerParams() throws IOException {
		if (Config.serverParamsUrl != null) {
			Log.println("Reading server params from: "+ Config.serverParamsUrl);
			URL server = new URL(Config.serverParamsUrl);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(server.openStream()));

			//String availableVersion;
			//availableVersion = in.readLine(); // read the first line
			Properties serverProperties = new Properties();
			serverProperties.load(in);
			in.close();
			
			try {
				Log.println("Setting server params to: ");
				Config.primaryServer = serverProperties.getProperty("primaryServer");
				Log.println(Config.primaryServer);
				Config.secondaryServer = serverProperties.getProperty("secondaryServer");
				Log.println(Config.secondaryServer);
				Config.sendToBothServers = Boolean.parseBoolean(serverProperties.getProperty("sendToBothServers"));
				Log.println(""+Config.sendToBothServers);
			} catch (NumberFormatException nf) {
				Log.println("Could not load the server paramaters: " + nf.getMessage());
			} catch (NullPointerException nf) {
				Log.println("Could not load the server paramaters: " + nf.getMessage());
			}

		}
		
		
	}
	
	public void updateT0(Spacecraft sat) {
		String urlString = Config.t0UrlPath + "FOX" + sat.foxId + Config.t0UrlFile;
		String file = "FOX" + sat.foxId + Config.t0UrlFile;
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + "FOX" + sat.foxId + Config.t0UrlFile;			
		}
		URL website;
		try {
			website = new URL(urlString);
		
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos;
			fos = new FileOutputStream(file);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			sat.loadTimeZeroSeries();
		} catch (MalformedURLException e) {
			Log.println("Invalid location for T0 file: " + file);
			//e.printStackTrace(Log.getWriter());
		} catch (IOException e) {
			Log.println("Could not write T0 file: " + file);
			//e.printStackTrace(Log.getWriter());
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
        while ((line = in.readLine()) != null) {
        	notes = notes + line + "\n";
        }
        Log.println("LATEST VERSION: "+availableVersion);
        try {
        int maj = Config.parseVersionMajor(availableVersion);
        int min = Config.parseVersionMinor(availableVersion);
        //String point = Config.parseVersionPoint(availableVersion);
        //System.out.println("MAJ: "+maj);
        //System.out.println("MIN: "+min);
        //System.out.println("POINT: "+point);
        
        if (Config.getVersionMajor() < maj) requireUpgrade(availableVersion, notes);
        if (Config.getVersionMajor() == maj && Config.getVersionMinor() < min) recommendUpgrade(availableVersion, notes);
        } catch (NumberFormatException e) {
        	e.printStackTrace(Log.getWriter());
        	Log.println("Error parsing the latest version information.  Abandoning the check");
        }
        in.close();
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

	boolean worldHasNotEnded = true;
	
	@Override
	public void run() {
	
		// Check the server paramaters first so that the config is quickly updated
		try {
			updateServerParams();
		} catch (IOException e1) {
			Log.println("Can not read the server paramaters, skipping");
			e1.printStackTrace(Log.getWriter());
		}

		if (Config.downloadT0FromServer) {
			ArrayList<Spacecraft> sats = Config.satManager.getSpacecraftList();
			for (int i=0; i<sats.size(); i++) {
				updateT0(sats.get(i));
			}
		}

		try {
			checkVersion();
		} catch (IOException e1) {
			Log.println("Can not read the latest version, skipping");
			e1.printStackTrace(Log.getWriter());
		}
	
		while (worldHasNotEnded) {
			try {
				Thread.sleep(SERVER_UPDATE_PERIOD);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(Log.getWriter());
			}
			try {
				updateServerParams();
			} catch (IOException e1) {
				Log.println("Can not read the server paramaters, skipping");
				e1.printStackTrace(Log.getWriter());
			}
		}
	}

}
