package fcd;

import java.io.IOException;

import purejavahidapi.HidDeviceInfo;
import common.Log;
import device.DeviceException;

public class FcdProPlusDevice extends FcdDevice {

	

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

	public FcdProPlusDevice(HidDeviceInfo fcdInfo) throws IOException, DeviceException {
		super(fcdInfo);
		SAMPLE_RATE = 192000;
		MIN_FREQ = 150;
		MAX_FREQ = 2050000;
	}

 //   public int setFcdFreq(long freq) throws FcdException {
 //   	super.setFcdFreq(freq);
//		Log.println("Freq causes RF Filter to be set"); 
//		return setRfFilter(freq);
 //   }
	/**
	 * Set the RF Filter based on the frequency requested
	 * @param freq
	 * @throws DeviceException 
	 
	public int setRfFilter(long freq) throws FcdException {
		if (freq >= 4000000 && freq <= 8000000) {
			Log.println("RF Filter set to 8-16M");
			return setRFFilter(TRFE_8_16);
		} else if (freq > 8000000 && freq <= 16000000) {
			Log.println("RF Filter set to 8-16M");
			return setRFFilter(TRFE_8_16); 
		} else if (freq > 16000000 && freq <= 32000000) {
			Log.println("RF Filter set to 16-32M");
			return setRFFilter(TRFE_16_32);
		} else if (freq > 32000000 && freq <= 75000000) {
			Log.println("RF Filter set to 32-75M");
			return setRFFilter(TRFE_32_75); 
		} else if (freq > 75000000 && freq <= 125000000) {
			Log.println("RF Filter set to 75-125M");
			return setRFFilter(TRFE_75_125); 
		} else if (freq >= 144000000 && freq <= 148000000) {
			Log.println("RF Filter set to 2m");
			return setRFFilter(TRFE_145); // Default to 2m
		} else if (freq >= 430000000 && freq <= 440000000) {
			Log.println("RF Filter set to 70cm");
			return setRFFilter(TRFE_435); // Default to 70cm
		} else if (freq > 125000000 && freq <= 250000000) {
			Log.println("RF Filter set to 125-250M");
			return setRFFilter(TRFE_125_250); 
		} else if (freq >= 410000000 && freq <= 875000000) {
			Log.println("RF Filter set to 410-875M");
			return setRFFilter(TRFE_410_875);
		} else if (freq > 875000000 && freq <= 2000000000) {
			Log.println("RF Filter set to 875-2000M");
			return setRFFilter(TRFE_875_2000); 
		}
		return 0;
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
    */
    public String getRfFilter() throws IOException, DeviceException {
		
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
			throw new DeviceException("Get RF Filter Command not executed: ");
		return "";
	}

    public String getIfFilter() throws IOException, DeviceException {
		
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
			throw new DeviceException("Get IF Filter Command not executed: ");
		return "";
	}

    
    public int setLnaGain(boolean on) throws DeviceException {
    	
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
    			throw new DeviceException("Set LNA Command not executed: ");
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }

    public boolean getLnaGain() throws IOException, DeviceException {
		
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
			throw new DeviceException("Get LNA Command not executed: ");
		return false;
	}
    
    public int setIFGain(int val) throws DeviceException {
    	
    	try {
    		int FCD_CMD_LEN = 2;
    		byte[] report = new byte[FCD_CMD_LEN];

    		report[0] = (byte)APP_SET_IF_GAIN1;
    		if (val > 0 && val < 60)
    			report[1] = (byte)val;
    		else
    			report[1] = (byte)0x00;

    		sendFcdCommand(report, FCD_CMD_LEN);
    		if (report[0] == APP_SET_IF_GAIN1)
    			return 0;
    		else
    			throw new DeviceException("Set LNA Command not executed: ");
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();

    		return -1;
    	}
    }

    public int getIFGain() throws IOException, DeviceException {
		
		int FCD_CMD_LEN = 3;
		byte[] report = new byte[FCD_CMD_LEN];
		report[1] = 0;
		report[0] = (byte)APP_GET_IF_GAIN1;
		sendFcdCommand(report,FCD_CMD_LEN);
		
		if (report[0] == APP_GET_IF_GAIN1) {
			Log.println("IF GAIN: " + report[2]);
			if (report[2] > 0 && report[2] < 60)
				return report[2];
		} else
			throw new DeviceException("Get LNA Command not executed: ");
		return 0;
	}
    

}
