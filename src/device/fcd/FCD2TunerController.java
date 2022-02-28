/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import common.Log;
import device.DeviceException;
import device.DevicePanel;
import device.rtl.RTL2832TunerController.SampleRate;

public class FCD2TunerController extends FCDTunerController
{

	public static final int MINIMUM_TUNABLE_FREQUENCY = 150;
	public static final int MAXIMUM_TUNABLE_FREQUENCY = 2050000;
	public static final int SAMPLE_RATE = 192000;
	
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
		
	public FCD2TunerController( Device device, DeviceDescriptor descriptor ) throws DeviceException 
	{
		super( "FCDPP", device, descriptor, SAMPLE_RATE,
			   MINIMUM_TUNABLE_FREQUENCY, MAXIMUM_TUNABLE_FREQUENCY );
	}

	public void init() throws DeviceException {
		super.init();

		int retried = 0;

		while (retried < 5) {
			try {
				setFCDMode( Mode.APPLICATION );
				return;
			} catch (LibUsbException e) {
				if (e.getErrorCode() == LibUsb.ERROR_PIPE
						|| e.getErrorCode() == LibUsb.ERROR_TIMEOUT
						|| e.getErrorCode() == LibUsb.ERROR_INTERRUPTED
						|| e.getErrorCode() == LibUsb.ERROR_BUSY
						|| e.getErrorCode() == LibUsb.ERROR_IO) {
					// this is a temporary error
					retried++;
					Log.println("FCD Error setting mode to APPLICATION, retrying...");
					try { Thread.sleep(200); } catch (InterruptedException e1) { }
				} else {
					// user intervention is required
					throw new DeviceException( "Error setting Mode to APPLICATION: " + e.getMessage() );
				}
			} catch( Exception e ) {
				throw new DeviceException( "Error setting Mode to APPLICATION: " + e.getMessage() );
			}
		}
	}
	
	public int getCurrentSampleRate()
	{
		return SAMPLE_RATE;
	}

	/*
	@Override
    public TunerClass getTunerClass()
    {
	    return TunerClass.FUNCUBE_DONGLE_PRO_PLUS;
    }

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.FUNCUBE_DONGLE_PRO_PLUS;
    }
	*/
	public void setLnaGain( boolean enabled ) throws DeviceException {
		try {
        	send( FCDCommand.APP_SET_LNA_GAIN, enabled ? 1 : 0 );
        }
        catch ( Exception e ) {
        	Log.println( "error while setting LNA Gain: " + e.getMessage() );
        	throw e;
        }
	}
	
	public boolean getLnaGain() throws DeviceException {
		try {
        	ByteBuffer buffer = send( FCDCommand.APP_GET_LNA_GAIN );
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			int ret = buffer.get(2);
			if (ret > 0 )
				return true;
        }
        catch ( Exception e ) {
        	Log.println( "error while getting LNA Gain: " + e.getMessage() );
        	throw e;
        }
		return false;
	}


	public void setBiasTee( boolean enabled ) throws DeviceException {
		try  {
        	send( FCDCommand.APP_SET_BIAS_TEE, enabled ? 1 : 0 );
        }
        catch ( Exception e ) {
        	Log.println( "error while setting Bias Tee: " + e.getMessage() );
        	throw e;
        }
	}
	
	public boolean getBiasTee() throws DeviceException {
		try {
        	ByteBuffer buffer = send( FCDCommand.APP_GET_BIAS_TEE );
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			int ret = buffer.get(2);
			if (ret > 0 )
				return true;
        }
        catch ( Exception e ) {
        	Log.println( "error while getting Bias Tee: " + e.getMessage() );
        	throw e;
        }
		return false;
	}
	
	public String getRfFilter() throws DeviceException {
		try {
        	ByteBuffer buffer = send( FCDCommand.APP_GET_RF_FILTER );
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			int ret = buffer.get(2);
			if (ret > -1 && ret < rfFilterName.length)
				return rfFilterName[ret];
        }
        catch ( Exception e ) {
        	Log.println( "error while getting RF Filter: " + e.getMessage() );
        	throw e;
        }
		return "";
	}

	public String getIfFilter() throws DeviceException {
		try {
        	ByteBuffer buffer = send( FCDCommand.APP_GET_IF_FILTER );
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			int ret = buffer.get(2);
			if (ret > -1 && ret < ifFilterName.length)
				return ifFilterName[ret];
        }
        catch ( Exception e ) {
        	Log.println( "error while getting IF Filter: " + e.getMessage() );
        	throw e;
        }
		return "";
	}
	
	public int getDCCorrection() {
		//int dcCorrection = -999;
		
		try {
			ByteBuffer buffer = send( FCDCommand.APP_GET_DC_CORRECTION );
			
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			
			return buffer.getInt( 2 );
        }
        catch ( Exception e ) {
        	Log.println( "error getting dc correction value "+ e.getMessage() );
        	throw e;
        }
	}
	
	public void setDCCorrection( int value ) {
		try {
			send( FCDCommand.APP_SET_DC_CORRECTION, value );
        }
        catch ( Exception e ) {
        	Log.println( "error setting dc correction to [" + value + "] "+ e.getMessage() );
        	throw e;
        }
	}
	
	public int getIQCorrection() {
		//int iqCorrection = -999;
		
		try {
			ByteBuffer buffer = send( FCDCommand.APP_GET_IQ_CORRECTION );
			
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			
			return buffer.getInt( 2 );
        }
        catch ( Exception e )  {
        	Log.println( "error reading IQ correction value " + e.getMessage() );
        	throw e;
        }
	}
	
	public void setIQCorrection( int value ) {
		try {
	        send( FCDCommand.APP_SET_IQ_CORRECTION, value );
        }
        catch ( Exception e ) {
        	Log.println( "error setting IQ correction to [" + value + "] " + e.getMessage() );
        	throw e;
        }
	}
	
	public enum Block { 
		CELLULAR_BAND_BLOCKED( "Blocked" ),
		NO_BAND_BLOCK( "Unblocked" ),
		UNKNOWN( "Unknown" );
		
		private String mLabel;
		
		private Block( String label ) {
		    mLabel = label;
		}
		
		public String getLabel() {
		    return mLabel;
		}

		public static Block getBlock( String block ) {
			Block retVal = UNKNOWN;
			if( block.equalsIgnoreCase( "No blk" ) ) {
				retVal = NO_BAND_BLOCK;
			}
			else if( block.equalsIgnoreCase( "Cell blk" ) ) {
				retVal = CELLULAR_BAND_BLOCKED;
			}
			return retVal;
		}
	}

	@Override
	public DevicePanel getDevicePanel() throws IOException, DeviceException {
		return new FcdProPlusPanel();
	}

	@Override
	public void setSampleRate(SampleRate sampleRate) {
		// TODO Auto-generated method stub
		
	}
}
