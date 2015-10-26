package fcd;

import java.io.IOException;
import java.util.List;

import common.Config;
import common.Log;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

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
 * This class wraps the HID Device and holds the commands that we pass to the Funcube Dongle.
 * 
 */
public class FcdDevice  {
	byte[] lastReport;
	static boolean commandMUX = false;
	
	HidDevice dev = null;
	// FCD Pro+ defaults
	public int MIN_FREQ = 150000;
	public int MAX_FREQ = 2050000000;
	public int SAMPLE_RATE = 192000;
	
	// Commands that are common to pro and pro plus
	public static final byte APP_SET_FREQUENCY_HZ = 101;
	public static final byte APP_GET_FREQUENCY_HZ = 102;

	public static final byte APP_SET_LNA_GAIN = 110;
	public static final byte APP_GET_LNA_GAIN = (byte)0x96; // 150

	HidDeviceInfo fcdInfo;

	public FcdDevice(HidDeviceInfo fcdInfo) throws IOException, FcdException {
		this.fcdInfo = fcdInfo;
		init();
	}

	public static FcdDevice makeDevice() throws IOException, FcdException {
		try {
			List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();

			for (HidDeviceInfo info : devList) {
				Log.println(String.format("VID = 0x%04X PID = 0x%04X Manufacturer = %s Product = %s Path = %s\n", //
						info.getVendorId(), //
						info.getProductId(), //
						info.getManufacturerString(), //
						info.getProductString(), //
						info.getPath()));

				if (info.getVendorId() == (short) 0x04d8 && info.getProductId() == (short) 0xfb31) {
					Log.println("Found the funcube Pro Plus HID device");
					return new FcdProPlusDevice(info);
				}
				if (info.getVendorId() == (short) 0x04d8 && info.getProductId() == (short) 0xfb56) {
					Log.println("Found the funcube Pro (original) HID device");
					return new FcdProDevice(info);
				}
			}
		} finally {

		}
		return null;
	}
	
	private void init() throws IOException, FcdException {
		Log.println("INIT HID USB");

		if (fcdInfo == null)
			Log.errorDialog("ERROR", "FCD device not found");
		else {
			Log.println("Set Freq to: " + Config.fcdFrequency*1000);
			setFcdFreq(Config.fcdFrequency*1000);
		}
}


	
	public void getFcdVersion() throws IOException, FcdException {
	
		int FCD_CMD_LEN = 1;
		int FCD_CMD_BL_QUERY = 01;
		byte[] report = new byte[FCD_CMD_LEN];
		//report[1] = 0;
		report[0] = (byte)FCD_CMD_BL_QUERY;
		sendFcdCommand(report,FCD_CMD_LEN);
	}
	
    public int setFcdFreq(long freq) throws FcdException {
    	
    	try {
    		int FCD_CMD_LEN = 5;
    		byte[] report = new byte[FCD_CMD_LEN];

    		report[0] = (byte)APP_SET_FREQUENCY_HZ;;
    		report[1] = (byte)freq;
    		report[2] = (byte)(freq>>8); 
    		report[3] = (byte)(freq>>16); 
    		report[4] = (byte)(freq>>24); 

    		sendFcdCommand(report, FCD_CMD_LEN);
    		return 0;
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }
    

    public int getParam(byte cmd) throws IOException, FcdException {
		
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)cmd;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == cmd) {
			Log.println("PARAM:"+cmd+" " + report[2]);
			return report[2];
		} else 
			throw new FcdException("Command not executed: " + cmd);
		
	}
    

    protected void sendFcdCommand(byte[] command, int len) throws IOException, FcdException {

    	//HidDevice dev = null;
    	lastReport = null;
    	if (dev == null) open();
    		dev.setInputReportListener(new InputReportListener() {
    			@Override
    			public void onInputReport(HidDevice source, byte Id, byte[] data, int len) {
    				lastReport = data;
    			}
    		});
    		@SuppressWarnings("unused")
			int result = dev.setOutputReport((byte)0, command, len);
    		Log.println("COMMAND: " + (int)command[0] + " Output Report: " + result);
    		
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		result = dev.getFeatureReport(command, len);
    		if (lastReport != null) {
    			for (int i = 0; i < len; i++)
    				Log.print(String.format("%02X ", lastReport[i]));
    			Log.println("");
    			String s = new String();
    			for (int i=0; i< len; i++) {
    				s = s + (char)lastReport[i];
    				command[i] = lastReport[i];
    			}
    			//Log.println(s);
    		}
    }
    private void open() throws FcdException {
		try {
			dev = PureJavaHidApi.openDevice(fcdInfo.getPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (dev == null) throw new FcdException("Cant Open the FCD.  Is Fox Telem already running?");    	
    }

    public void cleanup() throws IOException, FcdException {
    	if (dev != null) {
    			dev.close();
    			Log.println("Closed FCD device");
    	}
    	dev = null;
    }
}
