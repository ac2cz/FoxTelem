package telemServer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import common.Log;

public class ServerConfig {
	public static Properties properties; // Java properties file for user defined values
	public static final String propertiesFileName = "FoxTelemServer.properties";
	public static ArrayList<String> trustedGroundStations;
	public static final String TRUSTED_GROUNDSTATION_FILENAME = "trusted_ground_stations.txt";
	
	public static boolean slowSpeedRsDecode=true;
	public static boolean highSpeedRsDecode=true;

	// Version 0.32b
	public static int socketReadTimeout=1000;

	// Version 0.34
	public static int newResetCheckThreshold=30;
	public static int newResetCheckUptimeMax=15000;
	public static int groundStationClockThreshold=60;
	public static boolean debugResetCheck=true;
	
	public static void init() {
		properties = new Properties();
		try {
			loadTrustedGroundStations();
		} catch (IOException e) {
			Log.alert("FATAL: Cannot load the trusted stations list: " + e);
		}
		load();
	}
	
	public static boolean isTrustedGroundStation(String groundStation) {
		for (String station : trustedGroundStations)
			if (station.equalsIgnoreCase( groundStation))
				return true;
		return false;
	}
	
	private static void loadTrustedGroundStations() throws IOException {
		trustedGroundStations = new ArrayList<String>();
		BufferedReader bufReader = new BufferedReader(new FileReader(TRUSTED_GROUNDSTATION_FILENAME));

		String line = bufReader.readLine();
		while (line != null) {
			trustedGroundStations.add(line);
			line = bufReader.readLine();
		}

		bufReader.close();
	}

	public static void save() {
		properties.setProperty("slowSpeedRsDecode", Boolean.toString(slowSpeedRsDecode));
		properties.setProperty("highSpeedRsDecode", Boolean.toString(highSpeedRsDecode));
		properties.setProperty("socketReadTimeout", Integer.toString(socketReadTimeout));
		properties.setProperty("newResetCheckThreshold", Integer.toString(newResetCheckThreshold));
		properties.setProperty("newResetCheckUptimeMax", Integer.toString(newResetCheckUptimeMax));
		properties.setProperty("groundStationClockThreshold", Integer.toString(groundStationClockThreshold));
		properties.setProperty("debugResetCheck", Boolean.toString(debugResetCheck));
		store();
	}

	private static void store() {
		try {
			FileOutputStream fos = new FileOutputStream(propertiesFileName);
			properties.store(fos, "Fox 1 Telemetry Decoder Properties");
			fos.close();
		} catch (FileNotFoundException e1) {
			Log.errorDialog("ERROR", "Could not write properties file. Check permissions on directory or on the file\n" +
					propertiesFileName);
			e1.printStackTrace(Log.getWriter());
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error writing properties file");
			e1.printStackTrace(Log.getWriter());
		}

	}

	private static String getProperty(String key) {
		String value = properties.getProperty(key);
		if (value == null) throw new NullPointerException();
		return value;
	}

	public static void load() {
		// try to load the properties from a file
		try {
			FileInputStream fis = new FileInputStream(propertiesFileName);
			properties.load(fis);
			fis.close();
		} catch (IOException e) {
			Log.println("Writing Default properties file");
			save();
		}
		try {
			slowSpeedRsDecode = Boolean.parseBoolean(getProperty("slowSpeedRsDecode"));
			highSpeedRsDecode = Boolean.parseBoolean(getProperty("highSpeedRsDecode"));
			socketReadTimeout = Integer.parseInt(getProperty("socketReadTimeout"));
			newResetCheckThreshold = Integer.parseInt(getProperty("newResetCheckThreshold"));
			newResetCheckUptimeMax = Integer.parseInt(getProperty("newResetCheckUptimeMax"));
			groundStationClockThreshold = Integer.parseInt(getProperty("groundStationClockThreshold"));
			debugResetCheck = Boolean.parseBoolean(getProperty("debugResetCheck"));

		} catch (NumberFormatException nf) {
			Log.println("FATAL: Could not load properties: " + nf.getMessage());
			System.exit(1);
		}
		catch (NullPointerException nf) {
			Log.println("FATAL: Could not load properties: " +nf.getMessage());
			System.exit(1);
		}
		Log.println("LOADED: " + propertiesFileName);
	}
}
