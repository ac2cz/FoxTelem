package common;

import gui.MainWindow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JOptionPane;

/*
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
public class UpdateManager implements Runnable {

	public UpdateManager() {
		
	}
	
	private void checkVersion() throws IOException {
		URL oracle = new URL("http://amsat.us/FoxTelem/version.txt");
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
        String point = Config.parseVersionPoint(availableVersion);
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

	
	@Override
	public void run() {
		try {
			checkVersion();
		} catch (IOException e1) {
			Log.println("Can not read the latest version, skipping");
			e1.printStackTrace(Log.getWriter());
		}
	}

}
