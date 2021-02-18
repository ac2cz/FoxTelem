package device.rtl;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import org.usb4java.LibUsbException;

import common.Log;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.rtl.E4KTunerController.E4KGain;
import device.rtl.E4KTunerController.E4KLNAGain;
import device.rtl.E4KTunerController.E4KMixerGain;
import device.rtl.RTL2832TunerController.SampleRate;

@SuppressWarnings("serial")
public class RTLPanelE4K extends DevicePanel implements ItemListener, ActionListener, ChangeListener {
	private static final E4KGain DEFAULT_GAIN = E4KGain.PLUS_290;
	
	int NUM_OF_PARAMS = 15;

    private JSpinner mFrequencyCorrection;
    private JComboBox<E4KGain> mComboMasterGain;
    private JComboBox<E4KMixerGain> mComboMixerGain;
    private JComboBox<E4KLNAGain> mComboLNAGain;
	boolean loading = true;;
	private JLabel lblSampleRate;
	public static final String SAMPLE_RATE = "Sample Rate: ";
	
    
	public RTLPanelE4K() throws IOException, DeviceException {
		TitledBorder title = new TitledBorder(null, "E4000 USB SDR", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
		initializeGui();
	}
	
	public void setEnabled(boolean b) {
		
	}
	public void initializeGui() throws IOException, DeviceException {
		loading = true;
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		JPanel top = new JPanel();;
		JPanel bottom = new JPanel();
		add(top, BorderLayout.NORTH);
		top.setLayout(new FlowLayout());
		add(center, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);
		
		lblSampleRate = new JLabel( SAMPLE_RATE);
		top.add( lblSampleRate );

        //Frequency Correction 
        SpinnerModel model = new SpinnerNumberModel(     0,   //initial value
                                        -1000,   //min
                                         1000,   //max
                                            1 ); //step

        mFrequencyCorrection = new JSpinner( model );
        mFrequencyCorrection.setEnabled( true );

        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)mFrequencyCorrection.getEditor();  
        
        DecimalFormat format = editor.getFormat();  
        format.setMinimumFractionDigits( 0 );  
        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );          

        mFrequencyCorrection.addChangeListener(this);
        
        top.add( new JLabel( " |  Freq Correction (ppm):" ) );
        top.add( mFrequencyCorrection );
        
        /**
         * Gain Controls 
         */
        center.add( new JLabel( "Gain" ));
        
        /* Master Gain Control */
        mComboMasterGain = new JComboBox<E4KGain>( E4KGain.values() );
 //       mComboMasterGain.setEnabled( false );
        mComboMasterGain.addActionListener(this);
        mComboMasterGain.setToolTipText( "<html>Select <b>AUTOMATIC</b> for auto "
        		+ "gain, <b>MANUAL</b> to enable<br> independent control of "
        		+ "<i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>"
        		+ "settings, or one of the individual gain settings for<br>"
        		+ "semi-manual gain control</html>" );
        center.add( new JLabel( "Master:" ) );
        center.add( mComboMasterGain );

        E4KGain gain = (E4KGain)mComboMasterGain.getSelectedItem();

        /* Mixer Gain Control */
        mComboMixerGain = new JComboBox<E4KMixerGain>( E4KMixerGain.values() );
		if( gain != E4KGain.MANUAL ) 
			mComboMixerGain.setEnabled( false );
        mComboMixerGain.addActionListener(this);
        mComboMixerGain.setToolTipText( "<html>Mixer Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        
        center.add( new JLabel( "Mixer:" ) );
        center.add( mComboMixerGain );

        /* LNA Gain Control */
        mComboLNAGain = new JComboBox<E4KLNAGain>( E4KLNAGain.values() );
		if( gain != E4KGain.MANUAL ) 
			mComboLNAGain.setEnabled( false );
        mComboLNAGain.addActionListener(this);
        mComboLNAGain.setToolTipText( "<html>LNA Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        
        center.add( new JLabel( "LNA:" ) );
        center.add( mComboLNAGain );

        loading = false;
	}
	
	@Override
	public void setDevice(TunerController d) throws IOException, DeviceException {
		device = (RTL2832TunerController) d; 
		loading = true;
		getSettings();
		loading = false;
	}
	
	public void getSettings()  throws IOException, DeviceException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  // Allow startup to settle down first
		
		loadParam(mComboMasterGain, "mComboMasterGain");
		loadParam(mComboMixerGain, "mComboMixerGain");
		loadParam(mComboLNAGain, "mComboLNAGain");
		loadParam(mFrequencyCorrection, "mFrequencyCorrection");
		
		setGain();
		setMixerGain();
		setLnaGain();
		setFrequencyCorrection();

		int sampleRate = device.getCurrentSampleRate();
		SampleRate s = SampleRate.getClosest(sampleRate);
		lblSampleRate.setText(SAMPLE_RATE + s);
	}
	
	/**
	 * Save the settings for the RTL
	 */
	private void save() {
		if (!loading) {
			saveParam(mComboMasterGain, "mComboMasterGain");
			saveParam(mComboMixerGain, "mComboMixerGain");
			saveParam(mComboLNAGain, "mComboLNAGain");
			saveParam(mFrequencyCorrection, "mFrequencyCorrection");
		}

	}
	
	
	public void updateFilter() throws IOException, DeviceException {
		//rfFilterValue.setText(fcd.getRfFilter());
	}
	
	
	@Override
	public void run() {
		done = false;
		running = true;
		Thread.currentThread().setName("RTLPanel");

//		while(running) {
//
//			try {
//				Thread.sleep(1000); // approx 1 sec refresh
//			} catch (InterruptedException e) {
//				Log.println("ERROR: FCD thread interrupted");
//				//e.printStackTrace();
//			} 
//
//			this.repaint();
//		}
	}			


	private void setGain() {
		try  {
			E4KGain gain = (E4KGain)mComboMasterGain.getSelectedItem();

			((E4KTunerController) device).setGain( (E4KGain)mComboMasterGain.getSelectedItem(), true );

			if( gain == E4KGain.MANUAL )  {
				mComboMixerGain.setSelectedItem( gain.getMixerGain() ); 
				mComboMixerGain.setEnabled( true );

				mComboLNAGain.setSelectedItem( gain.getLNAGain() );
				mComboLNAGain.setEnabled( true );

//				mComboVGAGain.setSelectedItem( gain.getVGAGain() );
//				mComboVGAGain.setEnabled( true );
			}
			else  {
				mComboMixerGain.setEnabled( false );
				mComboMixerGain.setSelectedItem( gain.getMixerGain() );

				mComboLNAGain.setEnabled( false );
				mComboLNAGain.setSelectedItem( gain.getLNAGain() );

//				mComboVGAGain.setEnabled( false );
//				mComboVGAGain.setSelectedItem( gain.getVGAGain() );
			}
			save();
		}
		catch ( UsbException e ){
			Log.errorDialog( 
					"E4K Tuner Controller - couldn't apply the gain ",
					"setting - " + e.getLocalizedMessage() );  

			Log.println( "E4K Tuner Controller - couldn't apply "
					+ "gain setting - " + e );
		}
	}

	private void setMixerGain() {
		try {
			E4KMixerGain mixerGain = 
					(E4KMixerGain)mComboMixerGain.getSelectedItem();

			if( mixerGain == null ){
				mixerGain = DEFAULT_GAIN.getMixerGain();
			}

			if( mComboMixerGain.isEnabled() ){
				((E4KTunerController) device).setMixerGain( mixerGain, true );
			}
			save();
		}
		catch ( UsbException e ) {
			Log.errorDialog( 
					"E4K Tuner Controller - couldn't apply the mixer ",
					"gain setting - " + e.getLocalizedMessage() );  

			Log.println( "E4K Tuner Controller - couldn't apply mixer "
					+ "gain setting - " + e );
		}
	}

	private void setLnaGain() {
		try {
			E4KLNAGain lnaGain = 
					(E4KLNAGain)mComboLNAGain.getSelectedItem();

			if ( lnaGain == null )
			{
				lnaGain = DEFAULT_GAIN.getLNAGain();
			}

			if( mComboLNAGain.isEnabled() )
			{
				((E4KTunerController) device).setLNAGain( lnaGain, true );
			}

			save();
		}
		catch ( UsbException e ) {
			Log.errorDialog( 
					"E4K Tuner Controller - couldn't apply the LNA ",
					"gain setting - " + e.getLocalizedMessage() );  

			Log.println( "E4K Tuner Controller - couldn't apply LNA "
					+ "gain setting - " + e );
		}
	}

	private void setFrequencyCorrection() {
		
		try {
			int rate = (int) mFrequencyCorrection.getValue();
			((E4KTunerController) device).setSampleRateFrequencyCorrection( rate );
			save();
		}
		catch ( DeviceException eSampleRate ) {
			Log.errorDialog(  
					"E4K Tuner Controller - couldn't set the frequency correction ",
					"rate [" +  mFrequencyCorrection.getValue() + "] " + 
							eSampleRate.getLocalizedMessage() );  

			Log.println( "E4K Tuner Controller - couldn't set the frequency correction "
					+ "rate setting [" +   mFrequencyCorrection.getValue() + "] " + 
					eSampleRate );
		} 
	}

//private void setSampleRate() {
//	SampleRate sampleRate = (SampleRate)mComboSampleRate.getSelectedItem();
//	try {
//		((RTL2832TunerController) device).setSampleRate( sampleRate );
//		save();
//	}
//	catch ( DeviceException | LibUsbException eSampleRate ) {
//		Log.errorDialog(  
//				"E4K Tuner Controller - couldn't apply the sample ",
//				"rate setting [" + sampleRate.getLabel() + "] " + 
//						eSampleRate.getLocalizedMessage() );  
//
//		Log.println( "E4K Tuner Controller - couldn't apply sample "
//				+ "rate setting [" + sampleRate.getLabel() + "] " + 
//				eSampleRate );
//	} 
//}

@Override
public void actionPerformed(ActionEvent e) {

	if (e.getSource() == mComboMasterGain) {
		setGain();
	}
	if (e.getSource() == mComboMixerGain) {
		setMixerGain();
	}
	if (e.getSource() == mComboLNAGain) {
		setLnaGain();
	}


}

@Override
public void itemStateChanged(ItemEvent e) {

}

    /**
     * Read the configuration from the device
     */
//    private void getConfig() {
//    	
//    }
//    
//	private void loadConfig() {
//		
//	}
	
@Override
public void stateChanged(ChangeEvent e) {
	if (e.getSource() == this.mFrequencyCorrection) {
		Log.println("Set PPM to:" + (int) mFrequencyCorrection.getValue());
		setFrequencyCorrection();
	}
} 

	@Override
	public int getSampleRate() {
		try {
			return device.getCurrentSampleRate();
		} catch (DeviceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 192000;
		}
	}

	@Override
	public int getDecimationRate() {
		return 1;
	}


}
