package fcd;

import java.io.IOException;

import purejavahidapi.HidDeviceInfo;

public class FcdProDevice extends FcdDevice {

	public static final byte APP_GET_LNA_ENHANCE = (byte)0x97;
	public static final byte APP_GET_BAND = (byte)0x98;
	public static final byte APP_GET_MIXER_FILTER = (byte)0x9C;
	public static final byte APP_GET_GAIN1 = (byte)0x9D;
	public static final byte APP_GET_GAIN_MODE = (byte)0x9E;
	public static final byte APP_GET_RC_FILTER = (byte)0x9F;
	public static final byte APP_GET_GAIN2 = (byte)0xA0;
	public static final byte APP_GET_GAIN3 = (byte)0xA1;
	public static final byte APP_GET_GAIN4 = (byte)0xA3;
	public static final byte APP_GET_GAIN5 = (byte)0xA4;
	public static final byte APP_GET_GAIN6 = (byte)0xA5;

	
	public FcdProDevice(HidDeviceInfo fcdInfo) throws IOException, FcdException {
		super(fcdInfo);
		SAMPLE_RATE = 96000;
		MIN_FREQ = 150000;
		MAX_FREQ = 2050000000;
		}

}
