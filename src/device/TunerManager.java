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
package device;

import java.util.ArrayList;
import javax.usb.UsbException;

import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

import common.Log;
import device.airspy.AirspyDevice;
import device.fcd.FCD1TunerController;
import device.fcd.FCD2TunerController;
import device.rtl.E4KTunerController;
import device.rtl.R820TTunerController;
import device.rtl.RTL2832TunerController;
import device.rtl.RTL2832TunerController.SampleRate;

public class TunerManager {

	ArrayList<String> deviceNames;
	ArrayList<TunerController> tunerControllerList;
	
	//private MixerManager mMixerManager;
	//private TunerModel mTunerModel;
	private ThreadPoolManager mThreadPoolManager;
	
    public TunerManager( )
	{
    	//mMixerManager = mixerManager;
    	//mTunerModel = tunerModel;
		mThreadPoolManager = new ThreadPoolManager();
	}
    
    /**
     * Performs cleanup of USB related issues
     */
    public void dispose()
    {
    	LibUsb.exit( null );
    }

    /**
     * Loads all USB tuners and USB/Mixer tuner devices
     * This locks each USB device that it finds.  It's nice that we build the list of the actual devices, but not good that they are
     * all locked by FoxTelem
     * @throws UsbException 
     * @throws DeviceException 
     */
    /*
    public ArrayList<String> makeDeviceList() throws UsbException, DeviceException	{
    	deviceNames = new ArrayList<String>();
    	tunerControllerList = new ArrayList<TunerController>();
    	DeviceList deviceList = new DeviceList();
    	int result = LibUsb.init( null );
    	if( result != LibUsb.SUCCESS ){
    		Log.println( "unable to initialize libusb [" + 
    				LibUsb.errorName( result ) + "]" );
    	} else {
    		Log.println( "LibUSB API Version: " + LibUsb.getApiVersion() );
    		Log.println( "LibUSB Version: " + LibUsb.getVersion() );

    		result = LibUsb.getDeviceList( null, deviceList );

    		if( result < 0 ) {
    			Log.println( "unable to get device list from libusb [" + result + " / " + 
    					LibUsb.errorName( result ) + "]" );
    		} else {
    			Log.println( "discovered [" + result + "] attached USB devices" );
    		}
    	}

    	for( Device device: deviceList ) {
    		DeviceDescriptor descriptor = new DeviceDescriptor();
    		result = LibUsb.getDeviceDescriptor( device, descriptor );
    		if( result != LibUsb.SUCCESS ) {
    			Log.println( "unable to read device descriptor [" + 
    					LibUsb.errorName( result ) + "]" );
    		} else {
    			TunerController dev = initTuner( device, descriptor );
    			if (dev !=null) {
    				deviceNames.add((java.lang.String) dev.name);
    				tunerControllerList.add(dev);
    				StringBuilder sb = new StringBuilder();

    				sb.append( "usb device [" );
    				sb.append( descriptor.idVendor() );
//    				sb.append( String.format( "%04X", descriptor.idVendor() ) );
    				sb.append( ":" );
 //   				sb.append( String.format( "%04X", descriptor.idProduct() ) );
    				sb.append( descriptor.idProduct() );

    				Log.println( sb.toString() );
    			}
    		}
    	}

    	LibUsb.freeDeviceList( deviceList, true );
    	return deviceNames;
    }
	*/
    
    public TunerController findDevice(short vendor, short product, SampleRate sampleRate) throws UsbException, DeviceException	{
    	DeviceList deviceList = new DeviceList();
    	int result = LibUsb.init( null );
    	if( result != LibUsb.SUCCESS ){
    		Log.println( "unable to initialize libusb [" + 
    				LibUsb.errorName( result ) + "]" );
    	} else {
    		Log.println( "LibUSB API Version: " + LibUsb.getApiVersion() );
    		Log.println( "LibUSB Version: " + LibUsb.getVersion() );
    		result = LibUsb.getDeviceList( null, deviceList );
    		if( result < 0 ) {
    			Log.println( "unable to get device list from libusb [" + result + " / " + 
    					LibUsb.errorName( result ) + "]" );
    		} else {
    			Log.println( "discovered [" + result + "] attached USB devices" );
    		}
    	}

    	for( Device device: deviceList ) {
    		DeviceDescriptor descriptor = new DeviceDescriptor();
    		result = LibUsb.getDeviceDescriptor( device, descriptor );
    		if( result != LibUsb.SUCCESS ) {
    			Log.println( "unable to read device descriptor [" + 
    					LibUsb.errorName( result ) + "]" );
    		} else {
    			StringBuilder sb = new StringBuilder();
    			sb.append( "usb device [" );
    			sb.append( String.format( "%04X", descriptor.idVendor() ) );
    			sb.append( ":" );
    			sb.append( String.format( "%04X", descriptor.idProduct() ) + "]");

    			Log.println( sb.toString() );
    			Log.println(device.toString());
    			if (descriptor.idVendor() == vendor && descriptor.idProduct() == product) {
    				Log.println("FOUND DEVICE!");
    				TunerController dev = initTuner( device, descriptor, sampleRate );
    				if (dev !=null) {
    	    			LibUsb.freeDeviceList( deviceList, true );
    					return dev;
    				}
    			}
    		}
    	}

    	LibUsb.freeDeviceList( deviceList, true );
    	return null;
    }

    public TunerController DEPRECIATED_getTunerControllerById(int id) {
    	return tunerControllerList.get(id);
    }
    
    private device.TunerController initTuner( Device device, 
    		DeviceDescriptor descriptor, SampleRate sampleRate ) throws UsbException, DeviceException
    {
    	if( device != null && descriptor != null )
    	{
			TunerClass tunerClass = TunerClass.valueOf( descriptor.idVendor(), 
					descriptor.idProduct() );
			
			switch( tunerClass )
			{
				case AIRSPY:
					return initAirspyTuner( device, descriptor );
				case ETTUS_USRP_B100:
					//return initEttusB100Tuner( device, descriptor );
				case FUNCUBE_DONGLE_PRO:
					return initFuncubeProTuner( device, descriptor );
				case FUNCUBE_DONGLE_PRO_PLUS:
					return initFuncubeProPlusTuner( device, descriptor );
				case HACKRF_ONE:
				case RAD1O:
					//return initHackRFTuner( device, descriptor );
				case COMPRO_VIDEOMATE_U620F:
				case COMPRO_VIDEOMATE_U650F:
				case COMPRO_VIDEOMATE_U680F:
				case GENERIC_2832:
				case GENERIC_2838:
				case DEXATEK_5217_DVBT:
				case DEXATEK_DIGIVOX_MINI_II_REV3:
				case DEXATEK_LOGILINK_VG002A:
				case GIGABYTE_GTU7300:
				case GTEK_T803:
				case LIFEVIEW_LV5T_DELUXE:
				case MYGICA_TD312:
				case PEAK_102569AGPK:
				case PROLECTRIX_DV107669:
				case SVEON_STV20:
				case TERRATEC_CINERGY_T_REV1:
				case TERRATEC_CINERGY_T_REV3:
				case TERRATEC_NOXON_REV1_B3:
				case TERRATEC_NOXON_REV1_B4:
				case TERRATEC_NOXON_REV1_B7:
				case TERRATEC_NOXON_REV1_C6:
				case TERRATEC_NOXON_REV2:
				case TERRATEC_T_STICK_PLUS:
				case TWINTECH_UT40:
				case ZAAPA_ZTMINDVBZP:
					return initRTL2832Tuner( tunerClass, device, descriptor, sampleRate );
				case UNKNOWN:
				default:
					break;
			}
		}
		
		return null;
	}

	private device.TunerController initAirspyTuner( Device device, 
											 DeviceDescriptor descriptor ) throws UsbException
	{
		try
		{
			ThreadPoolManager mThreadPoolManager = new ThreadPoolManager();
			AirspyDevice airspyController = new AirspyDevice( device, mThreadPoolManager );

			airspyController.init();

			//AirspyTuner tuner = new AirspyTuner( airspyController );

			return airspyController;
		}
		catch( DeviceException se ) {
			Log.println("Couldn't construct Airspy controller/tuner.  It is probablly in use");

			return null;
		}
	}


	/*
	private TunerInitStatus initEttusB100Tuner( Device device, 
												DeviceDescriptor descriptor )
	{
		return new TunerInitStatus( null, "Ettus B100 tuner not currently "
				+ "supported" );
	}
*/	
	private FCD1TunerController initFuncubeProTuner( Device device,  DeviceDescriptor descriptor ) {
		try {
			FCD1TunerController controller = 
					new FCD1TunerController( device, descriptor );
			controller.init();
			return controller;
		}
		catch( DeviceException se ) {
			Log.println( "error constructing tuner: " + se );
			return null;
		}
	}

	private FCD2TunerController initFuncubeProPlusTuner( Device device, DeviceDescriptor descriptor ) throws DeviceException {
			FCD2TunerController controller = 
					new FCD2TunerController( device, descriptor );
			controller.init();
			return controller;

	}
/*
	private TunerInitStatus initHackRFTuner( Device device, 
											 DeviceDescriptor descriptor )
	{
		try
	    {
			HackRFTunerController hackRFController = 
						new HackRFTunerController( device, descriptor, 
								mThreadPoolManager );
			
			hackRFController.init();
			
			HackRFTuner tuner = new HackRFTuner( hackRFController );
			
			return new TunerInitStatus( tuner, "LOADED" );
	    }
		catch( DeviceException se )
		{
			Log.println( "couldn't construct HackRF controller/tuner", se );
			
			return new TunerInitStatus( null, 
					"error constructing HackRF tuner controller" );
		}
	}
*/
	
	
	private device.TunerController initRTL2832Tuner( TunerClass tunerClass,
											  Device device, 
											  DeviceDescriptor deviceDescriptor, SampleRate sampleRate )
	{
		String reason = "NOT LOADED";

		TunerType tunerType = tunerClass.getTunerType();
			
		if( tunerType == TunerType.RTL2832_VARIOUS )
		{
			try
			{
				tunerType = RTL2832TunerController.identifyTunerType( device );
			}
			catch( DeviceException e )
			{
				Log.println( "couldn't determine RTL2832 tuner type: " + e );
				tunerType = TunerType.UNKNOWN;
			}
		}
			
		switch( tunerType )
		{
			case ELONICS_E4000:
				try
				{
					E4KTunerController controller = 
						new E4KTunerController( device, deviceDescriptor, 
								mThreadPoolManager );
					
					controller.init(sampleRate);
					
	//				RTL2832Tuner rtlTuner = 
	//					new RTL2832Tuner( tunerClass, controller );
					
					//return new TunerInitStatus( rtlTuner, "LOADED" );
					return controller;
				}
				catch( DeviceException se )
				{
					//return new TunerInitStatus( null, "Error constructing E4K tuner "
					//	+ "controller - " + se.getLocalizedMessage() );
					Log.println( "error constructing tuner: " + se );
					return null;
				}
			case RAFAELMICRO_R820T:
				try
				{
					R820TTunerController controller = 
						new R820TTunerController( device, deviceDescriptor, 
								mThreadPoolManager );
					
					controller.init(sampleRate);
					
	//				RTL2832Tuner rtlTuner = 
	//					new RTL2832Tuner( tunerClass, controller );
					
					//return new TunerInitStatus( rtlTuner, "LOADED" );
					return controller;
				}
				catch( DeviceException se )
				{
					Log.println( "error constructing tuner: " + se );
					
					//return new TunerInitStatus( null, "Error constructing R820T "
					//	+ "tuner controller - " + se.getLocalizedMessage() );
					return null;
				}
			case FITIPOWER_FC0012:
			case FITIPOWER_FC0013:
			case RAFAELMICRO_R828D:
			case UNKNOWN:
			default:
				reason = "SDRTRunk doesn't currently support RTL2832 "
					+ "Dongle with [" + tunerType.toString() + 
					"] tuner for tuner class[" + tunerClass.toString() + "]";
				break;
		}
		
//		return new TunerInitStatus( null, reason );
		Log.println( "error constructing tuner: " + reason );

		return null;
		
	}

	
    /**
     * Gets the first tuner mixer dataline that corresponds to the tuner class.
     * 
     * Note: this method is not currently able to align multiple tuner mixer
     * data lines of the same tuner type.  If you have multiple Funcube Dongle
     * tuners of the same TYPE, there is no guarantee that you will get the 
     * correct mixer.
     * 
     * @param tunerClass
     * @return
     */
	/*
    private MixerTunerDataLine getMixerTunerDataLine( TunerType tunerClass )
    {
    	Collection<MixerTunerDataLine> datalines = 
    			mMixerManager.getMixerTunerDataLines();

    	for( MixerTunerDataLine mixerTDL: datalines  )
		{
    		if( mixerTDL.getMixerTunerType().getTunerClass() == tunerClass )
    		{
    			return mixerTDL;
    		}
		}
    	
    	return null;
    }
    */
	
	/*
	 * This allows us to return the device and the status.  e.g. Loaded, failed etc.
	 * Overkill.. Probablly only need the device.
	 */
    public class TunerInitStatus
    {
    	private device.TunerController mTuner;
    	private String mInfo;
    	
    	public TunerInitStatus( device.TunerController tuner, String info )
    	{
    		mTuner = tuner;
    		mInfo = info;
    	}
    	
    	public device.TunerController getTuner()
    	{
    		return mTuner;
    	}
    	
    	public String getInfo()
    	{
    		return mInfo;
    	}
    	
    	public boolean isLoaded()
    	{
    		return mTuner != null;
    	}
    }
}
