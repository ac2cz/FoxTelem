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
	// FCD Pro+
	public static final int MIN_FREQ = 150000;
	public static final int MAX_FREQ = 2050000000;
	public int SAMPLE_RATE = 192000;
	
	// Commands that we need
	public static final byte APP_SET_RF_FILTER = 113;
	public static final byte APP_SET_MIXER_GAIN = 114;
	public static final byte APP_SET_LNA_GAIN = 110;
	public static final byte APP_SET_FREQUENCY_HZ = 101;
	
	public static final byte APP_GET_LNA_GAIN = (byte)0x96;
	public static final byte APP_GET_LNA_ENHANCE = (byte)0x97;
	public static final byte APP_GET_BAND = (byte)0x98;
	public static final byte APP_GET_RF_FILTER = (byte)0x99;
	public static final byte APP_GET_MIXER_GAIN = (byte)0x9A;
	public static final byte APP_GET_MIXER_FILTER = (byte)0x9C;
	public static final byte APP_GET_GAIN1 = (byte)0x9D;
	public static final byte APP_GET_GAIN_MODE = (byte)0x9E;
	public static final byte APP_GET_RC_FILTER = (byte)0x9F;
	public static final byte APP_GET_GAIN2 = (byte)0xA0;
	public static final byte APP_GET_GAIN3 = (byte)0xA1;
	public static final byte APP_GET_IF_FILTER = (byte)0xA2;
	public static final byte APP_GET_GAIN4 = (byte)0xA3;
	public static final byte APP_GET_GAIN5 = (byte)0xA4;
	public static final byte APP_GET_GAIN6 = (byte)0xA5;
	
	//RF Filter Numbers
	int TRFE_0_4 = 0,
			TRFE_4_8 = 1,
			TRFE_8_16 = 2,
			TRFE_16_32 = 3,
			TRFE_32_75 = 4,
			TRFE_75_125 = 5,
			TRFE_125_250 = 6,
			TRFE_145 = 7,
			TRFE_410_875 = 8,
			TRFE_435 = 9,
			TRFE_875_2000 = 10;
	
	String[] rfFilterName = {"0-4MHz", "4-8MHz", "8-18MHz", "16-32MHz", "32-75MHz", "75-125MHz",
			"125-250MHz", "144-148MHz", "410-875MHz", "430-440MHz", "875-2GHz"
	};
	
	//IF Filter numbers
	int TIFE_200KHZ=0,
			TIFE_300KHZ=1,
			TIFE_600KHZ=2,
			TIFE_1536KHZ=3,
			TIFE_5MHZ=4,
			TIFE_6MHZ=5,
			TIFE_7MHZ=6,
			TIFE_8MHZ=7;

	String[] ifFilterName = {"200kHz", "300kHz", "600kHz", "1536kHz", "5MHz", "6MHz",
			"7MHz", "8MHz"
	};

        
	public final static byte FCD_INTERFACE = (byte)0x2;
	public final static byte FCD_ENDPOINT_IN = (byte)0x82;
	public final static byte FCD_ENDPOINT_OUT = (byte)0x2;
	
	HidDeviceInfo fcdInfo;
	
	public FcdDevice() throws IOException, FcdException {
		init();
	}
	
	private void init() throws IOException, FcdException {
    	Log.println("INIT HID USB");
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
					SAMPLE_RATE = 192000;
					fcdInfo = info;
					break;
				}
				if (info.getVendorId() == (short) 0x04d8 && info.getProductId() == (short) 0xfb56) {
					Log.println("Found the funcube Pro (original) HID device");
					SAMPLE_RATE = 96000;
					fcdInfo = info;
					break;
				}
			}
			if (fcdInfo == null)
				Log.errorDialog("ERROR", "FCD device not found");
			else {
				Log.println("Get Version");
				getFcdVersion();
				//Log.println("Set Mixer Gain");
				//setMixerGain(true);
				//Log.println("Set LNA Gain");
				//setLnaGain(true);
				Log.println("Set Freq to: " + Config.fcdFrequency*1000);
				setFcdFreq(Config.fcdFrequency*1000);
			}
		} finally {
			
		}
    }

	/**
	 * Set the RF Filter based on the frequency requested
	 * @param freq
	 * @throws FcdException 
	 */
	public void setRfFilter(long freq) throws FcdException {
		if (freq >= 4000000 && freq <= 8000000) {
			setRFFilter(TRFE_8_16); 
			Log.println("RF Filter set to 8-16M");
		} else if (freq > 8000000 && freq <= 16000000) {
			setRFFilter(TRFE_8_16); 
			Log.println("RF Filter set to 8-16M");
		} else if (freq > 16000000 && freq <= 32000000) {
			setRFFilter(TRFE_16_32); 
			Log.println("RF Filter set to 16-32M");
		} else if (freq > 32000000 && freq <= 75000000) {
			setRFFilter(TRFE_32_75); 
			Log.println("RF Filter set to 32-75M");
		} else if (freq > 75000000 && freq <= 125000000) {
			setRFFilter(TRFE_75_125); 
			Log.println("RF Filter set to 75-125M");
		} else if (freq >= 144000000 && freq <= 148000000) {
			setRFFilter(TRFE_145); // Default to 2m
			Log.println("RF Filter set to 2m");
		} else if (freq >= 430000000 && freq <= 440000000) {
			setRFFilter(TRFE_435); // Default to 70cm
			Log.println("RF Filter set to 70cm");
		} else if (freq > 125000000 && freq <= 250000000) {
			setRFFilter(TRFE_125_250); 
			Log.println("RF Filter set to 125-250M");
		} else if (freq >= 410000000 && freq <= 875000000) {
			setRFFilter(TRFE_410_875);
			Log.println("RF Filter set to 410-875M");
		} else if (freq > 875000000 && freq <= 2000000000) {
			setRFFilter(TRFE_875_2000); 
			Log.println("RF Filter set to 875-2000M");
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
    		try {
				Thread.sleep(100); // give it time to process last command
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
    		setRfFilter(freq);
    		return 0;
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }
    
    public int setRFFilter(int filter) throws FcdException {
    	
    	try {
    		int FCD_CMD_LEN = 2;
    		byte[] report = new byte[FCD_CMD_LEN];

    		report[0] = (byte)APP_SET_RF_FILTER;
    		report[1] = (byte)filter;

    		sendFcdCommand(report, FCD_CMD_LEN);
    		if (report[0] == APP_SET_RF_FILTER)
    			return 0;
    		else
    			throw new FcdException("Set RF Filter Command not executed: ");
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
    
    public String getRfFilter() throws IOException, FcdException {
		
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_RF_FILTER;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == APP_GET_RF_FILTER) {
			Log.println("RF FILTER: " + report[2]);
			if (report[2] > -1 && report[2] < rfFilterName.length)
			return rfFilterName[report[2]];
		} else
			throw new FcdException("Get RF Filter Command not executed: ");
		return "";
	}

    public String getIfFilter() throws IOException, FcdException {
		
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_IF_FILTER;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == APP_GET_IF_FILTER) {
			Log.println("IF FILTER: " + report[2]);
			if (report[2] > -1 && report[2] < ifFilterName.length)
			return ifFilterName[report[2]];
		} else
			throw new FcdException("Get IF Filter Command not executed: ");
		return "";
	}

    public int setMixerGain(boolean on) throws FcdException {
    	
    	try {
    		int FCD_CMD_LEN = 2;
    		byte[] report = new byte[FCD_CMD_LEN];

    		report[0] = (byte)APP_SET_MIXER_GAIN;
    		if (on)
    			report[1] = (byte)0x01;
    		else
    			report[1] = (byte)0x00;

    		sendFcdCommand(report, FCD_CMD_LEN);
    		if (report[0] == APP_SET_MIXER_GAIN)
    			return 0;
    		else
    			throw new FcdException("Set Mixer Gain Command not executed: ");
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }

    public boolean getMixerGain() throws IOException, FcdException {
		
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_MIXER_GAIN;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == APP_GET_MIXER_GAIN) {
			Log.println("MIXER GAIN: " + report[2]);
			if (report[2] == 1)
				return true;
		} else
			throw new FcdException("Get Mixer Gain Command not executed: ");
		return false;
	}
    public int setLnaGain(boolean on) throws FcdException {
    	
    	try {
    		int FCD_CMD_LEN = 2;
    		byte[] report = new byte[FCD_CMD_LEN];

    		report[0] = (byte)APP_SET_LNA_GAIN;
    		if (on)
    			report[1] = (byte)0x01;
    		else
    			report[1] = (byte)0x00;

    		sendFcdCommand(report, FCD_CMD_LEN);
    		if (report[0] == APP_SET_LNA_GAIN)
    			return 0;
    		else
    			throw new FcdException("Set LNA Command not executed: ");
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }

    public boolean getLnaGain() throws IOException, FcdException {
		
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_LNA_GAIN;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == APP_GET_LNA_GAIN) {
			Log.println("LNA GAIN: " + report[2]);
			if (report[2] == 1)
				return true;
		} else
			throw new FcdException("Get LNA Command not executed: ");
		return false;
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
    private void sendFcdCommand(byte[] command, int len) throws IOException, FcdException {
    	while (commandMUX) {
    		
    	}
    	commandMUX = true;
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
				Thread.sleep(10);
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
    		commandMUX = false;
    }
    
    public void cleanup() throws IOException, FcdException {
    	if (dev != null) {
    			dev.close();
    			Log.println("Closed FCD device");
    	}
    	dev = null;
    }
}
