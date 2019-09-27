/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package device.fcd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;

import common.Log;
import device.DeviceException;
import device.DevicePanel;

public class FCD1TunerController extends FCDTunerController
{
	public static final int MINIMUM_TUNABLE_FREQUENCY = 64000;
	public static final int MAXIMUM_TUNABLE_FREQUENCY = 1700000;
	public static final int SAMPLE_RATE = 96000;
	
//	private double mDCCorrectionInPhase = 0.0;
//	private double mDCCorrectionQuadrature = 0.0;
//	private double mPhaseCorrection = 0.0;
//	private double mGainCorrection = 0.0;
//	private LNAGain mLNAGain;
//	private LNAEnhance mLNAEnhance;
//	private MixerGain mMixerGain;
	
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

	public FCD1TunerController( Device device, DeviceDescriptor descriptor ) throws DeviceException 
	{
		super( "FCDP", device, descriptor, SAMPLE_RATE,
				   MINIMUM_TUNABLE_FREQUENCY, MAXIMUM_TUNABLE_FREQUENCY );
	}
	
	public void init() throws DeviceException
	{
		super.init();
		
		//mFrequencyController.setSampleRate( SAMPLE_RATE );
		
		try
		{
			setFCDMode( Mode.APPLICATION );
			//getPhaseAndGainCorrection();
			//getDCIQCorrection();
			//getLNAGainSetting();
			//getLNAEnhanceSetting();
			//getMixerGainSetting();
			
			send( FCDCommand.APP_SET_MIXER_GAIN, 1l );
		}
		catch( Exception e )
		{
			e.printStackTrace();
			
			throw new DeviceException( "FCDTunerController error " +
					"during construction: " + e.getMessage() );
		}
	}
	
	public int getCurrentSampleRate()
	{
		return SAMPLE_RATE;
	}
	
	public void setLnaGain(int val) throws DeviceException {
		try {
        	send( FCDCommand.APP_SET_LNA_GAIN, val );
        }
        catch ( Exception e ) {
        	throw new DeviceException( "error while setting LNA Gain: " + e.getMessage() );
        }
	}
	
	public int getLnaGain() {
		ByteBuffer buffer = send( FCDCommand.APP_GET_LNA_GAIN );
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		int gain = buffer.getInt( 2 );
		return gain;
	}
	
	public void setRFFilter(int filter) throws DeviceException {
		try {
			send( FCDCommand.APP_SET_RF_FILTER, filter );
		}
		catch ( Exception e ) {
			throw new DeviceException( "error while setting RF Filter: " + e.getMessage() );
		}	
	}

    public String getRfFilter() throws IOException, DeviceException {	 	
		int band = getBandInt();
		
		ByteBuffer buffer = send( FCDCommand.APP_GET_RF_FILTER );
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		int filter = buffer.getInt( 2 );

		Log.println("RF FILTER: " + filter);
		if (band == 0) {
			if (filter > -1 && filter < vhfIIFilterName.length)
				return vhfIIFilterName[filter];
		} else if (band == 1) {
			if (filter > -1 && filter < vhfIIIFilterName.length)
				return vhfIIIFilterName[filter];
		} else if (band == 2) {
			if (filter > -1 && filter < uhfFilterName.length)
				return uhfFilterName[filter];
		} else if (band == 3) {
			if (filter > -1 && filter < lbandFilterName.length)
				return lbandFilterName[filter];
		}
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
    	ByteBuffer buffer = send( FCDCommand.APP_GET_BAND );
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		int bandNum = buffer.getInt( 2 );
    		Log.println("BAND: " + bandNum);
    		if (bandNum > -1 && bandNum < band.length)
    			return bandNum;
    	return 99;
    }
	
	public enum Block 
	{ 
		CELLULAR_BAND_BLOCKED( "Blocked" ),
		NO_BAND_BLOCK( "Unblocked" ),
		UNKNOWN( "Unknown" );
		
		private String mLabel;
		
		private Block( String label )
		{
		    mLabel = label;
		}
		
		public String getLabel()
		{
		    return mLabel;
		}

		public static Block getBlock( String block )
		{
			Block retVal = UNKNOWN;

			if( block.equalsIgnoreCase( "No blk" ) )
			{
				retVal = NO_BAND_BLOCK;
			}
			else if( block.equalsIgnoreCase( "Cell blk" ) )
			{
				retVal = CELLULAR_BAND_BLOCKED;
			}
			
			return retVal;
		}
	}


	@Override
	public DevicePanel getDevicePanel() throws IOException, DeviceException {
		return new FcdProPanel();
	}
}
