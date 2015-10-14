package common;

import gui.MainWindow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JOptionPane;

public class UpdateManager implements Runnable {

	public UpdateManager() {
		
	}
	
	private void checkVersion() throws IOException {
		URL oracle = new URL("http://amsat.us/FoxTelem/version.txt");
        BufferedReader in = new BufferedReader(
        new InputStreamReader(oracle.openStream()));

        String availableVersion;
        availableVersion = in.readLine(); // read the first line
        Log.println("LATEST VERSION: "+availableVersion);
        int maj = Config.parseVersionMajor(availableVersion);
        int min = Config.parseVersionMinor(availableVersion);
        String point = Config.parseVersionPoint(availableVersion);
        //System.out.println("MAJ: "+maj);
        //System.out.println("MIN: "+min);
        //System.out.println("POINT: "+point);
        
        if (Config.getVersionMajor() < maj) recommendUpgrade(availableVersion);
        if (Config.getVersionMajor() == maj && Config.getVersionMinor() < min) recommendUpgrade(availableVersion);
        
        in.close();
	}
	
	private void recommendUpgrade(String ver) {
		String message = "Version " +ver+ " of FoxTelem is available!  Do you want to go to amsat.us/FoxTelem/ to download it?";
		Object[] options = {"Yes",
		"No"};
		int n = JOptionPane.showOptionDialog(
				MainWindow.frame,
				message,
				"New FoxTelem Version Available",
				JOptionPane.YES_NO_OPTION, 
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[1]);

		if (n == JOptionPane.NO_OPTION) {
			return;
		}
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
