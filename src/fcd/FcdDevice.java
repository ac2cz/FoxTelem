package fcd;

import java.io.IOException;
import java.util.List;

import common.Config;
import common.Log;
import device.Device;
import device.DeviceException;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;

public abstract class FcdDevice extends Device {
	byte[] lastReport;
	//static boolean commandMUX = false;

	HidDevice dev = null;
	// FCD Pro+ defaults
	// Commands that are common to pro and pro plus
	public static final byte APP_SET_FREQUENCY_HZ = 101;
	public static final byte APP_GET_FREQUENCY_HZ = 102;

	public static final byte APP_SET_LNA_GAIN = 110;
	public static final byte APP_GET_LNA_GAIN = (byte)0x96; // 150

	public static final byte APP_SET_MIXER_GAIN = 114;
	public static final byte APP_GET_MIXER_GAIN = (byte)0x9A; // 154

	public static final byte APP_SET_IF_GAIN1 = 115;
	public static final byte APP_GET_IF_GAIN1 = (byte)0x9D; // 157

	public static final byte APP_SET_RF_FILTER = 113;
	public static final byte APP_GET_RF_FILTER = (byte)0x99; // 153

	public static final byte APP_GET_IF_FILTER = (byte)0xA2; //162

	HidDeviceInfo fcdInfo;

	public FcdDevice(HidDeviceInfo fcdInfo) throws IOException, DeviceException {
		super();
		this.fcdInfo = fcdInfo;
		init();
	}
	public boolean isConnected() { if (dev != null) return true; return false; }

	public static Device makeDevice() throws IOException, DeviceException {
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

	private void init() throws IOException, DeviceException {
		Log.println("INIT HID USB");

		if (fcdInfo == null)
			Log.errorDialog("ERROR", "RF device not found");
		else {
		}
	}
	protected void open() throws DeviceException {
		try {
			dev = PureJavaHidApi.openDevice(fcdInfo.getPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//if (dev == null) throw new FcdException("Cant Open the FCD.  Is Fox Telem already running?");    	
	}

	public void cleanup() throws IOException, DeviceException {
		if (dev != null) {
			if (!Config.isLinuxOs()) {
				dev.close();  // This causes a hang on Linux!
				Log.println("Closed RF device");
			}
		}
		dev = null;
	}
	public void getFcdVersion() throws IOException, DeviceException {

		int FCD_CMD_LEN = 1;
		int FCD_CMD_BL_QUERY = 01;
		byte[] report = new byte[FCD_CMD_LEN];
		//report[1] = 0;
		report[0] = (byte)FCD_CMD_BL_QUERY;
		sendFcdCommand(report,FCD_CMD_LEN);
	}

	public int setFrequency(long freq) throws DeviceException {

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


	public int getParam(byte cmd) throws IOException, DeviceException {

		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)cmd;
		sendFcdCommand(report,FCD_CMD_LEN);

		if (report[0] == cmd) {
			Log.println("PARAM:"+cmd+" " + report[2]);
			return report[2];
		} else 
			throw new DeviceException("Command not executed: " + cmd);

	}


	protected void sendFcdCommand(byte[] command, int len) throws IOException, DeviceException {

		//HidDevice dev = null;
		lastReport = null;
		if (dev == null) open();
		if (dev != null) {
			dev.setInputReportListener(new InputReportListener() {
				@Override
				public void onInputReport(HidDevice source, byte Id, byte[] data, int len) {
					lastReport = data;
				}
			});
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


	}
	public int setMixerGain(boolean on) throws DeviceException {

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
				throw new DeviceException("Set Mixer Gain Command not executed: ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return -1;
		}
	}

	public boolean getMixerGain() throws IOException, DeviceException {

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
			throw new DeviceException("Get Mixer Gain Command not executed: ");
		return false;
	}


}
