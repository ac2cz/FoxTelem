package fcd;

import java.io.IOException;

import common.Log;
import device.DeviceException;
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

	
	// TUNER_LNA_GAIN_ENUM
	//int[] lnaGainConfigValue = {0,1,4,5,6,7,8,9,10,11,12,13,14};
	
	public static String[] lnaGain = {"-5dB","-2.5dB","0dB","2.5dB","5dB","7.5dB","10dB","12.5dB","15dB","17.5dB","20dB","25dB","30dB"};

	// TUNER_MIXER_GAIN_ENUM
	public static String[] mixerGain = {"4dB", "12dB"};
	int TMGE_P4_0DB=0,
	 TMGE_P12_0DB=1;
	
	// TUNER_LNA_ENHANCE_ENUM
	int TLEE_OFF=0,
			  TLEE_0=1,
			  TLEE_1=3,
			  TLEE_2=5,
			  TLEE_3=7;
	
	// TUNER_BAND_ENUM
	int TBE_VHF2=0,
	  TBE_VHF3=1,
	  TBE_UHF=2, 
	  TBE_LBAND=3;
	
	String[] band = {"VHF II", "VHF III", "UHF", "LBAND"};
	
	String[] vhfIIFilterName = {"268MHz", "299MHz"};
	String[] vhfIIIFilterName = {"509MHz", "656MHz"};
	String[] uhfFilterName = {"360MHz", "380MHz","405MHz", "425MHz", "450MHz", "475MHz", "505MHz", "540MHz", "575MHz", "615MHz", 
			"670MHz", "720MHz", "760MHz", "840MHz", "890MHz", "970MHz"};
	String[] lbandFilterName = {"1300MHz", "1320MHz", "1360MHz","1410MHz","1445MHz","1460MHz","1490MHz","1530MHz","1560MHz",
			"1590MHz","1640MHz","1660MHz","1680MHz","1700MHz","1720MHz","1750MHz"};
	
	// TUNER_RF_FILTER_ENUM
	int
	// Band 0, VHF II
	  TRFE_LPF268MHZ=0,
	  TRFE_LPF299MHZ=8,
	  // Band 1, VHF III
	  TRFE_LPF509MHZ=0,
	  TRFE_LPF656MHZ=8,
	  // Band 2, UHF
	  TRFE_BPF360MHZ=0,
	  TRFE_BPF380MHZ=1,
	  TRFE_BPF405MHZ=2,
	  TRFE_BPF425MHZ=3,
	  TRFE_BPF450MHZ=4,
	  TRFE_BPF475MHZ=5,
	  TRFE_BPF505MHZ=6,
	  TRFE_BPF540MHZ=7,
	  TRFE_BPF575MHZ=8,
	  TRFE_BPF615MHZ=9,
	  TRFE_BPF670MHZ=10,
	  TRFE_BPF720MHZ=11,
	  TRFE_BPF760MHZ=12,
	  TRFE_BPF840MHZ=13,
	  TRFE_BPF890MHZ=14,
	  TRFE_BPF970MHZ=15,
	  // Band 2, L band
	  TRFE_BPF1300MHZ=0,
	  TRFE_BPF1320MHZ=1,
	  TRFE_BPF1360MHZ=2,
	  TRFE_BPF1410MHZ=3,
	  TRFE_BPF1445MHZ=4,
	  TRFE_BPF1460MHZ=5,
	  TRFE_BPF1490MHZ=6,
	  TRFE_BPF1530MHZ=7,
	  TRFE_BPF1560MHZ=8,
	  TRFE_BPF1590MHZ=9,
	  TRFE_BPF1640MHZ=10,
	  TRFE_BPF1660MHZ=11,
	  TRFE_BPF1680MHZ=12,
	  TRFE_BPF1700MHZ=13,
	  TRFE_BPF1720MHZ=14,
	  TRFE_BPF1750MHZ=15;
	  
	
	
	// TUNER_BIAS_CURRENT_ENUM
	int  TBCE_LBAND=0,
			  TBCE_1=1,
			  TBCE_2=2,
			  TBCE_VUBAND=3;
	
	// TUNER_MIXER_FILTER_ENUM
	int  TMFE_27_0MHZ=0,
			  TMFE_4_6MHZ=8,
			  TMFE_4_2MHZ=9,
			  TMFE_3_8MHZ=10,
			  TMFE_3_4MHZ=11,
			  TMFE_3_0MHZ=12,
			  TMFE_2_7MHZ=13,
			  TMFE_2_3MHZ=14,
			  TMFE_1_9MHZ=15;
	
	// TUNER_IF_GAIN1_ENUM
	int  TIG1E_N3_0DB=0,
			  TIG1E_P6_0DB=1;
	
	// TUNER_IF_GAIN_MODE_ENUM
	int  TIGME_LINEARITY=0,
			  TIGME_SENSITIVITY=1;
	
	// TUNER_IF_RC_FILTER_ENUM
	int TIRFE_21_4MHZ=0,
			  TIRFE_21_0MHZ=1,
			  TIRFE_17_6MHZ=2,
			  TIRFE_14_7MHZ=3,
			  TIRFE_12_4MHZ=4,
			  TIRFE_10_6MHZ=5,
			  TIRFE_9_0MHZ=6,
			  TIRFE_7_7MHZ=7,
			  TIRFE_6_4MHZ=8,
			  TIRFE_5_3MHZ=9,
			  TIRFE_4_4MHZ=10,
			  TIRFE_3_4MHZ=11,
			  TIRFE_2_6MHZ=12,
			  TIRFE_1_8MHZ=13,
			  TIRFE_1_2MHZ=14,
			  TIRFE_1_0MHZ=15;
	
	// TUNER_IF_GAIN2_ENUM
	int TIG2E_P0_0DB=0,
			  TIG2E_P3_0DB=1,
			  TIG2E_P6_0DB=2,
			  TIG2E_P9_0DB=3;
	
	// TUNER_IF_GAIN3_ENUM
	int TIG3E_P0_0DB=0,
			  TIG3E_P3_0DB=1,
			  TIG3E_P6_0DB=2,
			  TIG3E_P9_0DB=3;
	
	// TUNER_IF_GAIN4_ENUM
	int  TIG4E_P0_0DB=0,
			  TIG4E_P1_0DB=1,
			  TIG4E_P2_0DB=2;
	
	// TUNER_IF_FILTER_ENUM
	int  TIFE_5_50MHZ=0,
			  TIFE_5_30MHZ=1,
			  TIFE_5_00MHZ=2,
			  TIFE_4_80MHZ=3,
			  TIFE_4_60MHZ=4,
			  TIFE_4_40MHZ=5,
			  TIFE_4_30MHZ=6,
			  TIFE_4_10MHZ=7,
			  TIFE_3_90MHZ=8,
			  TIFE_3_80MHZ=9,
			  TIFE_3_70MHZ=10,
			  TIFE_3_60MHZ=11,
			  TIFE_3_40MHZ=12,
			  TIFE_3_30MHZ=13,
			  TIFE_3_20MHZ=14,
			  TIFE_3_10MHZ=15,
			  TIFE_3_00MHZ=16,
			  TIFE_2_95MHZ=17,
			  TIFE_2_90MHZ=18,
			  TIFE_2_80MHZ=19,
			  TIFE_2_75MHZ=20,
			  TIFE_2_70MHZ=21,
			  TIFE_2_60MHZ=22,
			  TIFE_2_55MHZ=23,
			  TIFE_2_50MHZ=24,
			  TIFE_2_45MHZ=25,
			  TIFE_2_40MHZ=26,
			  TIFE_2_30MHZ=27,
			  TIFE_2_28MHZ=28,
			  TIFE_2_24MHZ=29,
			  TIFE_2_20MHZ=30,
			  TIFE_2_15MHZ=31;
	
	// TUNER_IF_GAIN5_ENUM
	int TIG5E_P3_0DB=0,
			  TIG5E_P6_0DB=1,
			  TIG5E_P9_0DB=2,
			  TIG5E_P12_0DB=3,
			  TIG5E_P15_0DB=4;
	
	// TUNER_IF_GAIN6_ENUM
	int   TIG6E_P3_0DB=0,
			  TIG6E_P6_0DB=1,
			  TIG6E_P9_0DB=2,
			  TIG6E_P12_0DB=3,
			  TIG6E_P15_0DB=4;
	
	
	public FcdProDevice(HidDeviceInfo fcdInfo) throws IOException, DeviceException {
		super(fcdInfo);
		SAMPLE_RATE = 96000;
		MIN_FREQ = 150000;
		MAX_FREQ = 2050000000;
		}

	public int setLnaGain(int val) throws DeviceException {

		try {
			int FCD_CMD_LEN = 2;
			byte[] report = new byte[FCD_CMD_LEN];

			report[0] = (byte)APP_SET_LNA_GAIN;
			if (val > 1)
				report[1] = (byte)(2+val); // we are missing 2 values in the array per the API.  Dont know why, but we offset
			else
				report[1] = (byte)val;

			sendFcdCommand(report, FCD_CMD_LEN);
			if (report[0] == APP_SET_LNA_GAIN)
				return 0;
			else
				throw new DeviceException("Set LNA Command not executed: ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return -1;
		}
	}
	public int getLnaGain() throws IOException, DeviceException {

		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_LNA_GAIN;
		sendFcdCommand(report,FCD_CMD_LEN);

		if (report[0] == APP_GET_LNA_GAIN) {
			Log.println("LNA GAIN: " + report[2]);
			if (report[2]> 3) 
				report[2] = (byte) (report[2] - 2);
			return report[2];
		} else
			throw new DeviceException("Get LNA Command not executed: ");
	}
	
public int setRFFilter(int filter) throws DeviceException {
    	
    	try {
    		int FCD_CMD_LEN = 2;
    		byte[] report = new byte[FCD_CMD_LEN];

    		report[0] = (byte)APP_SET_RF_FILTER;
    		report[1] = (byte)filter;

    		sendFcdCommand(report, FCD_CMD_LEN);
    		if (report[0] == APP_SET_RF_FILTER)
    			return 0;
    		else
    			throw new DeviceException("Set RF Filter Command not executed: ");
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }
    
    public String getRfFilter() throws IOException, DeviceException {
		int band = getBandInt();
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_RF_FILTER;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == APP_GET_RF_FILTER) {
			Log.println("RF FILTER: " + report[2]);
			if (band == 0) {
			if (report[2] > -1 && report[2] < vhfIIFilterName.length)
				return vhfIIFilterName[report[2]];
			} else if (band == 1) {
				if (report[2] > -1 && report[2] < vhfIIIFilterName.length)
					return vhfIIIFilterName[report[2]];
			} else if (band == 2) {
				if (report[2] > -1 && report[2] < uhfFilterName.length)
					return uhfFilterName[report[2]];
			} else if (band == 3) {
				if (report[2] > -1 && report[2] < lbandFilterName.length)
					return lbandFilterName[report[2]];
			} else
				return "";
		} else
			throw new DeviceException("Get RF Filter Command not executed: ");
		return "";
    }

    public String getBand() throws IOException, DeviceException {
    	int ret = getBandInt();
    	if (ret > -1 && ret < band.length)
    	return band[ret];
    	else
    		return "";
    }
    public int getBandInt() throws IOException, DeviceException {
    	int FCD_CMD_LEN = 3;
    	byte[] report = new byte[FCD_CMD_LEN];
    	report[1] = 0;
    	report[0] = (byte)APP_GET_BAND         ;
    	sendFcdCommand(report,FCD_CMD_LEN);

    	if (report[0] == APP_GET_BAND) {
    		Log.println("BAND: " + report[2]);
    		if (report[2] > -1 && report[2] < band.length)
    			return report[2];
    	} else
    		throw new DeviceException("Get RF Filter Command not executed: ");
    	return 99;
    }

  //  public int setFcdFreq(long freq) throws FcdException {
   // 	super.setFcdFreq(freq);
  //  	Log.println("Freq causes RF Filter to be set"); 
   // 	return setRfFilter(freq);
   // }
    
    /**
	 * Set the RF Filter and Band based on the frequency requested
	 * @param freq
	 * @throws FcdException 
	 
	public int setRfFilter(long freq) throws FcdException {
		if (freq >= 4000000 && freq <= 268000000) {
			Log.println("RF Filter set to 268M");
			return setRFFilter(TRFE_LPF268MHZ);
		} else if (freq > 268000000 && freq <= 299000000) {
			Log.println("RF Filter set to 299M");
			return setRFFilter(TRFE_LPF299MHZ); 
		} 
		return 0;
	}
	*/
}
