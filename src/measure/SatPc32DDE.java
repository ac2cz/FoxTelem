package measure;

import com.pretty_tools.dde.DDEException;
import com.pretty_tools.dde.DDEMLException;
import com.pretty_tools.dde.client.DDEClientConversation;

import common.Config;
import common.Log;

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
 * This is stored in Config as a single instance of the class for the whole program.  We only connect once.
 * If the class is instantiated and the connection is not null then we have connected.  The satellite
 * should be NONE if nothing is up or a valid value.
 *
 */
public class SatPc32DDE {

	public static final int CONNECT_FAIL_ERROR_CODE = 0x400a;

	// This stores the results from the last request
	public DDEClientConversation conversation = null;

	String satellite;
	double azimuth;
	double elevation;
	long downlinkFrequency;
	
	public SatPc32DDE() {
		connectConversation();
	}
	
	public String getSatellite() {
		connectConversation();
		return satellite;
	}

	public double getAzimuth() {
		connectConversation();
		return azimuth;
	}

	public double getElevation() {
		connectConversation();
		return elevation;
	}

	public long getDownlinkFrequency() {
		connectConversation();
		return downlinkFrequency;
	}

	public synchronized void connectConversation() {
		if (conversation == null) {
			conversation = new DDEClientConversation();
			conversation.setTimeout(1000); // 1000ms timeout
			try {
				conversation.connect("SatPC32", "SatPcDdeConv");
			} catch (DDEException e) {
				conversation = null;
				; // no error message if we can't connect.  SatPC32 is closed
			}
		}
	}
	
	public synchronized void disconnect() {
		if (conversation != null) {
			// Sending "close()" command
			//try { conversation.execute("[close()]"); } catch (DDEException e) {/* do nothing */	}
			try { conversation.disconnect(); } catch (DDEException e) { Log.println("DDEException while disconnecting: " + e.getMessage());/* do nothing */	}
		}
		conversation = null;
	}

	/**
	 * Request the data from the DDE Server.  Satellite is set to null if we fail, NONE if the sat is not
	 * returned but the connection is good, otherwise to a valid value.
	 * 
	 * The return value indicates if the request was successful.  You need to lool at the data to see if the sat is up
	 * @return
	 */
	public synchronized boolean request() {
		String ddeString = null;

		connectConversation();
		
		if (conversation == null) {
			satellite = null;
			return false; 
		}
		try {
			// Requesting DDE String
			ddeString = conversation.request("SatPcDdeItem");
			  Log.println("SatPC32: " + ddeString);
			if (ddeString.length() > 0 && !ddeString.startsWith("**")) {
				String parts[] = ddeString.split(" ");
				satellite = parts[0].substring(2, parts[0].length());
				String az = parts[1].substring(2, parts[1].length());
				az = az.replaceAll(",","."); // in case we have European formatting
				azimuth = Double.parseDouble(az);
				String el = parts[2].substring(2, parts[2].length());
				el = el.replaceAll(",",".");
				elevation = Double.parseDouble(el);
				downlinkFrequency = Long.parseLong(parts[5].substring(2, parts[5].length()));
				if (Config.debugDDE)
					System.out.println("DDE Sat: " + satellite + " Az: " + azimuth + " El: " + elevation + " Freq: " + downlinkFrequency);

				return true;
			} else {
				satellite = "NONE"; // put something in here so that the SourceTab says "Tracked"
				return true; // request was still successful
			}
		}

		catch (DDEMLException e) {
			if (e.getErrorCode() == CONNECT_FAIL_ERROR_CODE)
				;// we can ignore failed connection error.  SatPC32 is not open
			else
				Log.println("DDEMLException: 0x" + Integer.toHexString(e.getErrorCode())
				+ " " + e.getMessage());
			satellite = null;
			disconnect();
			return false;
		}		
		catch (DDEException e) {
			Log.println("DDEException in request: " + e.getMessage());
			satellite = null;
			disconnect();
			return false;
		}
		catch (UnsatisfiedLinkError e) {
			Log.errorDialog("MISSING DDE DLLs", "FoxTelem could not find the JavaDDE.dll or JavaDDEx64.dll files.  They need to be in the same\n"
					+ "folder as the jar file and should have been part of the installation.\n"
					+ "The DDE connection to SatPC32 has been disabled.");
			Config.useDDEforAzEl = false;
			Config.useDDEforFreq = false;
			satellite = null;
			disconnect();
			return false;
		}
		catch (NoClassDefFoundError e) {
			Log.errorDialog("MISSING DDE DLLs", "FoxTelem could not find the JavaDDE.dll or JavaDDEx64.dll files.  They need to be in the same\n"
					+ "folder as the jar file and should have been part of the installation.\n"
					+ "The DDE connection to SatPC32 has been disabled.");
			Config.useDDEforAzEl = false;
			Config.useDDEforFreq = false;
			satellite = null;
			disconnect();
			return false;
		}
		catch (NumberFormatException e) {
			if (ddeString != null) 
				Log.println("Cannot parse the DDE message: " + ddeString + "\nNumber format error: " + e.getMessage());
			else
				Log.println("Cannot parse the DDE message.  \nNumber format error: " + e.getMessage());
			satellite = null;
			disconnect();
			return false;
		} finally {
			
		}
	}
}
