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

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;

import device.DeviceException;
import device.DevicePanel;

public class FCD1TunerController extends FCDTunerController
{
	public static final int MINIMUM_TUNABLE_FREQUENCY = 64000000;
	public static final int MAXIMUM_TUNABLE_FREQUENCY = 1700000000;
	public static final int SAMPLE_RATE = 96000;
	
	private double mDCCorrectionInPhase = 0.0;
	private double mDCCorrectionQuadrature = 0.0;
	private double mPhaseCorrection = 0.0;
	private double mGainCorrection = 0.0;
	private LNAGain mLNAGain;
	private LNAEnhance mLNAEnhance;
	private MixerGain mMixerGain;
	
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
		super( device, descriptor, SAMPLE_RATE,
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
	

	/**
	 * Gets the current LNA Gain value from the controller and stores it
	 */
	private void getLNAGainSetting() throws UsbClaimException, UsbException
	{
		ByteBuffer buffer = send( FCDCommand.APP_GET_LNA_GAIN );
		
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		
		int gain = buffer.getInt( 2 );

		mLNAGain = LNAGain.valueOf( gain );
	}
	
	/**
	 * @return - current lna gain setting
	 */
	public LNAGain getLNAGain()
	{
		return mLNAGain;
	}

	/**
	 * Sets lna gain for the controller
	 * @param gain
	 * @throws UsbException
	 * @throws UsbClaimException
	 */
	public void setLNAGain( LNAGain gain ) 
						throws UsbException, UsbClaimException
	{
		send( FCDCommand.APP_SET_LNA_GAIN, gain.getSetting() );
	}
	
	/**
	 * @return - current phase correction setting
	 */
	public double getPhaseCorrection()
	{
		return mPhaseCorrection;
	}

	/**
	 * Sets the phase correction
	 * @param value - new phase correction
	 * @throws IllegalArgumentException for value outside limits (0.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setPhaseCorrection( double value ) 
			throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "Phase correction value [" + 
					value + "] outside limits ( -1.0 <> 1.0 )" );
		}
		
		mPhaseCorrection = value;
		setPhaseAndGainCorrection();
	}

	/**
	 * @return current gain correction setting
	 */
	public double getGainCorrection()
	{
		return mGainCorrection;
	}

	/**
	 * 
	 * @param value - gain correction value
	 * @throws IllegalArgumentException for value outside limits (0.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setGainCorrection( double value ) 
					throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "Gain correction value [" + 
					value + "] outside limits ( -1.0 <> 1.0 )" );
		}
		
		mGainCorrection = value;
		setPhaseAndGainCorrection();
	}
	
	/**
	 * Sends the phase and gain correction values to FCD.
	 * 
	 * The interface expects an argument with the SIGNED phase value in the first
	 * 16 bits and the UNSIGNED gain value in the second 16 bits, both in
	 * big-endian format.  Since we're sending the argument in java little endian
	 * format, we place the phase in the high-order bits and the gain in the 
	 * low-order bits, so that the send(command) function will reorder the bytes
	 * into big endian format, and place the arguments correctly.
	 * 
	 * Gain correction values range -1.0 to 0.0 to 1.0 and applied to the dongle
	 * as values 0 to 65534
	 * 
	 * Phase correction values range -1.0 to 0.0 to 1.0 and applied to the
	 * dongle as signed values -32768 to 32767
	 */
	private void setPhaseAndGainCorrection() throws UsbClaimException,
													UsbException
	{
		//UNSIGNED short gain value - masked into a long to avoid sign-extension
		long gain = (long)( ( 1.0 + mGainCorrection ) * Short.MAX_VALUE );
		
		//Left shift gain to place value in upper 16 bits
		long longGain = Long.rotateLeft( ( 0xFFFF & gain ), 16 );

		//SIGNED short phase value
		short phase = (short)( mPhaseCorrection * Short.MAX_VALUE );

		//Merge the results
		long correction = longGain | phase;

		send( FCDCommand.APP_SET_IQ_CORRECTION, correction );
	}

	/**
	 * Retrieves the stored phase and gain correction values from the FCD.
	 * 
	 * Note: testing shows that when the dongle is unplugged, any stored phase
	 * and gain correction values are reset.  Reading the dongle after a fresh
	 * plugin shows the values reset.
	 * 
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	private void getPhaseAndGainCorrection() throws UsbClaimException,
													UsbException
	{
		ByteBuffer buffer = send( FCDCommand.APP_GET_IQ_CORRECTION );
		
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		
		int correction = buffer.getInt( 2 );
		
		/**
		 * Gain: mask the upper 16 phase bits and right shift to get the 
		 * unsigned short value and then divide by 32768 to get the double 
		 * value.  We place the unsigned short value in an int to avoid sign
		 * issues.
		 */
		int gain = (int)( Long.rotateRight( correction & 0xFFFF0000, 16 ) );

		mGainCorrection = ( (double)gain / (double)Short.MAX_VALUE ) - 1.0;

		/**
		 * Phase: mask the lower 16 bits and divide by 32768 to get the double
		 * value
		 */
		short phase = (short)( correction & 0x0000FFFF );
		
		mPhaseCorrection = ( (double)phase / (double)Short.MAX_VALUE );
	}
	
	/**
	 * @return current DC correction for the I (inphase) component
	 */
	public double getDCCorrectionInPhase()
	{
		return mDCCorrectionInPhase;
	}

	/**
	 * Sets DC correction for the I (inphase) component and sends the new value
	 * to the FCD controller
	 * @param value - new DC correction value for the inphase component
	 * @throws IllegalArgumentException for values outside limits (-1.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setDCCorrectionInPhase( double value ) 
			throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "DC inphase correction value "
					+ "[" + value + "] outside limits ( 0.0 <> 1.0 )" );
		}
		
		mDCCorrectionInPhase = value;
		setDCIQCorrection();
	}

	/**
	 * @return current DC correction for the Q (quadrature) component
	 */
	public double getDCCorrectionQuadrature()
	{
		return mDCCorrectionQuadrature;
	}

	/**
	 * Sets the DC correction for the Q (quadrature) component and send the
	 * new value to the FCD controller
	 * @param value - new DC correction value for the quadrature component
	 * @throws IllegalArgumentException for values outside limits (-1.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setDCCorrectionQuadrature( double value ) 
			throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "DC quadrature correction "
					+ "value [" + value + "] outside limits ( 0.0 <> 1.0 )" );
		}
		
		mDCCorrectionQuadrature = value;
		setDCIQCorrection();
	}

	/**
	 * Sends the I/Q DC offset correction values to FCD.
	 * 
	 * The interface expects an argument with the signed inphase value in the 
	 * first 16 bits and the signed quadrature value in the second 16 bits, 
	 * both in big-endian format.  
	 * 
	 * Since we're sending the argument in java little endian format, we place 
	 * the quadrature in the high-order bits and the inphase in the low-order 
	 * bits, so that the send(command) function will reorder the bytes into big 
	 * endian format, and place the arguments correctly.
	 * 
	 * Both Inphase and Quadrature correction values range -1.0 to 0.0 to 1.0 
	 * and are applied to the dongle as signed short values -32768 to 0 to 32767
	 */
	private void setDCIQCorrection() throws UsbClaimException, UsbException
	{
		//I & Q DC offset correction values are signed short values
		short inphase = (short)( mDCCorrectionInPhase * Short.MAX_VALUE );
		short quadrature = (short)( mDCCorrectionQuadrature * Short.MAX_VALUE );

		//Mask the shorts into longs to preserve the sign bit and prepare
		//for merging to a single 32-bit value
		long maskedInphase = inphase & 0x0000FFFF;
		long maskedQuadrature = quadrature & 0x0000FFFF;
		
		//Left shift quadrature to place value in upper 16 bits
		long shiftedQuadrature = Long.rotateLeft( maskedQuadrature, 16 );
		
		//Merge the results
		long correction = shiftedQuadrature | maskedInphase;
		
//		Log.info( "FCD1: setting iq correction," +
//				" inphase:" + inphase +
//				" masked inphase:" + maskedInphase + 
//				" quad:" + quadrature + 
//				" masked quad:" + maskedQuadrature + 
//				" shifted quadrature:" + shiftedQuadrature + 
//				" correction:" + correction );

		send( FCDCommand.APP_SET_DC_CORRECTION, correction );
	}
	
	private void getDCIQCorrection() throws UsbClaimException,
													UsbException
	{
		ByteBuffer buffer = send( FCDCommand.APP_GET_DC_CORRECTION );
		
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		
		int correction = buffer.getInt( 2 );

		/**
		 * Quadrature: mask the upper 16 bits and divide by 32768 to get the
		 * stored quadrature value.  Cast the 16-bit value to a short to get 
		 * the signed short value
		 */
		long shiftedQuadrature = correction & 0xFFFF0000;
		long quadrature = Long.rotateRight( shiftedQuadrature, 16 );

		mDCCorrectionQuadrature = ( (short)quadrature / (double)Short.MAX_VALUE );

		/**
		 * InPhase: mask the lower 16 bits to get the short 
		 * value and then divide by 32768 to get the stored inphase value.
		 * Cast the 16-bit value to a short to get the signed short value
		 */
		long inphase = correction & 0x0000FFFF;
		mDCCorrectionInPhase = ( (short)inphase / (double)Short.MAX_VALUE );
		
//		Log.info( "FCD1:" +
//				  " correction:" + correction + 
//				  " Q:" + quadrature + 
//				  " I:" + inphase + 
//				  " QCorr:" + mDCCorrectionQuadrature + 
//				  " ICorr:" + mDCCorrectionInPhase );
	}
	
	private void getLNAEnhanceSetting() throws UsbClaimException, UsbException
	{
		ByteBuffer buffer = send( FCDCommand.APP_GET_LNA_ENHANCE );
		
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		
		int enhance = buffer.getInt( 2 );

		mLNAEnhance = LNAEnhance.valueOf( enhance );
	}
	
	public LNAEnhance getLNAEnhance()
	{
		return mLNAEnhance;
	}
	
	public void setLNAEnhance( LNAEnhance enhance ) 
				throws UsbClaimException, UsbException
	{
		send( FCDCommand.APP_SET_LNA_ENHANCE, enhance.getSetting() );
	}
	
	private void getMixerGainSetting() throws UsbClaimException, UsbException
	{
		ByteBuffer buffer = send( FCDCommand.APP_GET_MIXER_GAIN );
		
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		
		int gain = buffer.getInt( 2 );

		mMixerGain = MixerGain.valueOf( gain );
	}
	
	public MixerGain getMixerGain()
	{
		return mMixerGain;
	}
	
	public void setMixerGain( MixerGain gain ) throws UsbClaimException, UsbException
	{
		send( FCDCommand.APP_SET_MIXER_GAIN, gain.getSetting() );
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

	/**
	 * LNA Gain values suppported by the FCD Pro 1.0
	 */
    public enum LNAGain 
    {
    	LNA_GAIN_MINUS_5_0( 0, "-5.0 dB" ),
    	LNA_GAIN_MINUS_2_5( 1, "-2.5 dB" ),
    	LNA_GAIN_PLUS_0_0( 4, "0.0 dB" ),
    	LNA_GAIN_PLUS_2_5( 5, "2.5 dB" ),
    	LNA_GAIN_PLUS_5_0( 6, "5.0 dB" ),
    	LNA_GAIN_PLUS_7_5( 7, "7.5 dB" ),
    	LNA_GAIN_PLUS_10_0( 8, "10.0 dB" ),
    	LNA_GAIN_PLUS_12_5( 9, "12.5 dB" ),
    	LNA_GAIN_PLUS_15_0( 10, "15.0 dB" ),
    	LNA_GAIN_PLUS_17_5( 11, "17.5 dB" ),
    	LNA_GAIN_PLUS_20_0( 12, "20.0 dB" ),
    	LNA_GAIN_PLUS_25_0( 13, "25.0 dB" ),
    	LNA_GAIN_PLUS_30_0( 14, "30.0 dB" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private LNAGain( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public static LNAGain valueOf( int setting )
    	{
    		switch( setting )
    		{
    			case 0:
    				return LNA_GAIN_MINUS_5_0;
    			case 1:
    				return LNA_GAIN_MINUS_2_5;
    			case 4:
    				return LNA_GAIN_PLUS_0_0;
    			case 5:
    				return LNA_GAIN_PLUS_2_5;
    			case 6:
    				return LNA_GAIN_PLUS_5_0;
    			case 7:
    				return LNA_GAIN_PLUS_7_5;
    			case 8:
    				return LNA_GAIN_PLUS_10_0;
    			case 9:
    				return LNA_GAIN_PLUS_12_5;
    			case 10:
    				return LNA_GAIN_PLUS_15_0;
    			case 11:
    				return LNA_GAIN_PLUS_17_5;
    			case 12:
    				return LNA_GAIN_PLUS_20_0;
    			case 13:
    				return LNA_GAIN_PLUS_25_0;
    			case 14:
    				return LNA_GAIN_PLUS_30_0;
				default:
					throw new IllegalArgumentException( "FCD 1.0 Tuner "
							+ "Controller - unrecognized LNA gain setting "
							+ "value [" + setting + "]" );
    		}
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }

	/**
	 * LNA Enhance values suppported by the FCD Pro 1.0
	 */
    public enum LNAEnhance 
    {
    	LNA_ENHANCE_OFF( 0, "Off" ),
    	LNA_ENHANCE_0( 1, "0" ),
    	LNA_ENHANCE_1( 3, "1" ),
    	LNA_ENHANCE_2( 5, "2" ),
    	LNA_ENHANCE_3( 7, "3" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private LNAEnhance( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public static LNAEnhance valueOf( int setting )
    	{
    		switch( setting )
    		{
    			case 0:
    				return LNA_ENHANCE_OFF;
    			case 1:
    				return LNA_ENHANCE_0;
    			case 3:
    				return LNA_ENHANCE_1;
    			case 5:
    				return LNA_ENHANCE_2;
    			case 7:
    				return LNA_ENHANCE_3;
				default:
					throw new IllegalArgumentException( "FCD 1.0 Tuner "
							+ "Controller - unrecognized LNA enhance setting "
							+ "value [" + setting + "]" );
    		}
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }

	/**
	 * Mixer Gain values suppported by the FCD Pro 1.0
	 */
    public enum MixerGain 
    {
    	MIXER_GAIN_PLUS_4_0( 0, "4.0 dB" ),
    	MIXER_GAIN_PLUS_12_0( 1, "12.0 dB" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private MixerGain( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public static MixerGain valueOf( int setting )
    	{
    		switch( setting )
    		{
    			case 0:
    				return MIXER_GAIN_PLUS_4_0;
    			case 1:
    				return MIXER_GAIN_PLUS_12_0;
				default:
					throw new IllegalArgumentException( "FCD 1.0 Tuner "
							+ "Controller - unrecognized mixer gain setting "
							+ "value [" + setting + "]" );
    		}
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }

	@Override
	public DevicePanel getDevicePanel() throws IOException, DeviceException {
		return new FcdProPanel();
	}
}
