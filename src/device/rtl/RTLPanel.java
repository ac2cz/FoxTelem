package device.rtl;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import org.usb4java.LibUsbException;

import common.Config;
import common.Log;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.SourceException;
import device.airspy.AirspyDevice;
import device.airspy.AirspyDevice.Gain;
import device.airspy.AirspyDevice.GainMode;
import device.rtl.R820TTunerController.R820TGain;
import device.rtl.R820TTunerController.R820TLNAGain;
import device.rtl.R820TTunerController.R820TMixerGain;
import device.rtl.R820TTunerController.R820TVGAGain;
import device.rtl.RTL2832TunerController.SampleRate;

@SuppressWarnings("serial")
public class RTLPanel extends DevicePanel implements ItemListener, ActionListener, Runnable, ChangeListener {
	private static final R820TGain DEFAULT_GAIN = R820TGain.GAIN_279;
	
	int NUM_OF_PARAMS = 15;
	boolean running = true;
	boolean done = false;

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JComboBox<SampleRate> mComboSampleRate;
    private JSpinner mFrequencyCorrection;
    private JComboBox<R820TGain> mComboMasterGain;
    private JComboBox<R820TMixerGain> mComboMixerGain;
    private JComboBox<R820TLNAGain> mComboLNAGain;
    private JComboBox<R820TVGAGain> mComboVGAGain;	
	
	// Saved Values
    R820TGain gain;
    
	public RTLPanel() throws IOException, DeviceException {
		TitledBorder title = new TitledBorder(null, "RTL USB SDR", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
	}
	
	public void setEnabled(boolean b) {
		
	}
	public void initializeGui() throws IOException, DeviceException {
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		JPanel top = new JPanel();;
		JPanel bottom = new JPanel();
		add(top, BorderLayout.NORTH);
		top.setLayout(new FlowLayout());
		add(center, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);
		
		int sampleRate = device.getCurrentSampleRate();
		mComboSampleRate = new JComboBox<>( SampleRate.values() );
		loadParam(mComboSampleRate, "mComboSampleRate");
		mComboSampleRate.addActionListener(this);	
		SampleRate s = SampleRate.getClosest(sampleRate);
		if (s != null)
			mComboSampleRate.setSelectedItem(s);
		
		//top.add(mComboSampleRate);
		top.add( new JLabel( "Sample Rate:" ) );
		top.add( mComboSampleRate );
		// We are fixed at the sample rate that was used to start the decoder.  No way to dynamically change
		mComboSampleRate.setEnabled(false); // fixed at 240k for now.  Other rates do not work

        /*Frequency Correction 
        SpinnerModel model =
                new SpinnerNumberModel(     0.0,   //initial value
                                        -1000.0,   //min
                                         1000.0,   //max
                                            0.1 ); //step

        mFrequencyCorrection = new JSpinner( model );
        mFrequencyCorrection.setEnabled( false );

        JSpinner.NumberEditor editor = 
        		(JSpinner.NumberEditor)mFrequencyCorrection.getEditor();  
        
        DecimalFormat format = editor.getFormat();  
        format.setMinimumFractionDigits( 1 );  
        editor.getTextField().setHorizontalAlignment( SwingConstants.CENTER );          

        mFrequencyCorrection.addChangeListener(this);
        
        add( new JLabel( "PPM:" ) );
        add( mFrequencyCorrection );
        */
        //add( new JSeparator( JSeparator.HORIZONTAL ) );
        
        /**
         * Gain Controls 
         */
        center.add( new JLabel( "Gain" ));
        
        /* Master Gain Control */
        mComboMasterGain = new JComboBox<R820TGain>( R820TGain.values() );
        loadParam(mComboMasterGain, "mComboMasterGain");
 //       mComboMasterGain.setEnabled( false );
        mComboMasterGain.addActionListener(this);
        mComboMasterGain.setToolTipText( "<html>Select <b>AUTOMATIC</b> for auto "
        		+ "gain, <b>MANUAL</b> to enable<br> independent control of "
        		+ "<i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>"
        		+ "settings, or one of the individual gain settings for<br>"
        		+ "semi-manual gain control</html>" );
        center.add( new JLabel( "Master:" ) );
        center.add( mComboMasterGain );

        R820TGain gain = (R820TGain)mComboMasterGain.getSelectedItem();

        /* Mixer Gain Control */
        mComboMixerGain = new JComboBox<R820TMixerGain>( R820TMixerGain.values() );
        loadParam(mComboMixerGain, "mComboMixerGain");
		if( gain != R820TGain.MANUAL ) 
			mComboMixerGain.setEnabled( false );
        mComboMixerGain.addActionListener(this);
        mComboMixerGain.setToolTipText( "<html>Mixer Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        
        center.add( new JLabel( "Mixer:" ) );
        center.add( mComboMixerGain );

        /* LNA Gain Control */
        mComboLNAGain = new JComboBox<R820TLNAGain>( R820TLNAGain.values() );
        loadParam(mComboLNAGain, "mComboLNAGain");
		if( gain != R820TGain.MANUAL ) 
			mComboLNAGain.setEnabled( false );
        mComboLNAGain.addActionListener(this);
        mComboLNAGain.setToolTipText( "<html>LNA Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        
        center.add( new JLabel( "LNA:" ) );
        center.add( mComboLNAGain );

        /* VGA Gain Control */
        mComboVGAGain = new JComboBox<R820TVGAGain>( R820TVGAGain.values() );
        loadParam(mComboVGAGain, "mComboVGAGain");
		if( gain != R820TGain.MANUAL ) 
			mComboVGAGain.setEnabled( false );
        mComboVGAGain.addActionListener(this);
        mComboVGAGain.setToolTipText( "<html>VGA Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        center.add( new JLabel( "VGA:" ) );
        center.add( mComboVGAGain );
	}
	
	@Override
	public void setDevice(TunerController d) throws IOException, DeviceException {
		device = (RTL2832TunerController) d; 
		initializeGui();
		getSettings();

	}
	
	/**
	 * Save the settings for the RTL
	 */
	private void save() {
		if (gain != null) {
			
		}
		
	}
	
	
	public void updateFilter() throws IOException, DeviceException {
		//rfFilterValue.setText(fcd.getRfFilter());
	}
	
	
	
	public void getSettings()  throws IOException, DeviceException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}  // Allow startup to settle down first

		
		//gain = R820TGain(g);

	}
	
	
	@Override
	public void run() {
		done = false;
		running = true;

		while(running) {

			try {
				Thread.sleep(1000); // approx 1 sec refresh
			} catch (InterruptedException e) {
				Log.println("ERROR: FCD thread interrupted");
				//e.printStackTrace();
			} 


			if (device != null) {
				try {
					getSettings();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (DeviceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				this.repaint();
			}
		}			
	}
	
	private void setGain() {
		try  {
			R820TGain gain = (R820TGain)mComboMasterGain.getSelectedItem();

			((R820TTunerController) device).setGain( (R820TGain)mComboMasterGain.getSelectedItem(), true );

			if( gain == R820TGain.MANUAL )  {
				mComboMixerGain.setSelectedItem( gain.getMixerGain() ); 
				mComboMixerGain.setEnabled( true );

				mComboLNAGain.setSelectedItem( gain.getLNAGain() );
				mComboLNAGain.setEnabled( true );

				mComboVGAGain.setSelectedItem( gain.getVGAGain() );
				mComboVGAGain.setEnabled( true );
			}
			else  {
				mComboMixerGain.setEnabled( false );
				mComboMixerGain.setSelectedItem( gain.getMixerGain() );

				mComboLNAGain.setEnabled( false );
				mComboLNAGain.setSelectedItem( gain.getLNAGain() );

				mComboVGAGain.setEnabled( false );
				mComboVGAGain.setSelectedItem( gain.getVGAGain() );
			}
			saveParam(mComboMasterGain, "mComboMasterGain");
			saveParam(mComboMixerGain, "mComboMixerGain");
			saveParam(mComboLNAGain, "mComboLNAGain");
			saveParam(mComboVGAGain, "mComboVGAGain");
		}
		catch ( UsbException e ){
			Log.errorDialog( 
					"R820T Tuner Controller - couldn't apply the gain ",
					"setting - " + e.getLocalizedMessage() );  

			Log.println( "R820T Tuner Controller - couldn't apply "
					+ "gain setting - " + e );
		}
	}

	private void setMixerGain() {
		try {
			R820TMixerGain mixerGain = 
					(R820TMixerGain)mComboMixerGain.getSelectedItem();

			if( mixerGain == null ){
				mixerGain = DEFAULT_GAIN.getMixerGain();
			}

			if( mComboMixerGain.isEnabled() ){
				((R820TTunerController) device).setMixerGain( mixerGain, true );
			}

			save();
		}
		catch ( UsbException e ) {
			Log.errorDialog( 
					"R820T Tuner Controller - couldn't apply the mixer ",
					"gain setting - " + e.getLocalizedMessage() );  

			Log.println( "R820T Tuner Controller - couldn't apply mixer "
					+ "gain setting - " + e );
		}
		saveParam(mComboMixerGain, "mComboMixerGain");
	}

	private void setLnaGain() {
		try {
			R820TLNAGain lnaGain = 
					(R820TLNAGain)mComboLNAGain.getSelectedItem();

			if ( lnaGain == null )
			{
				lnaGain = DEFAULT_GAIN.getLNAGain();
			}

			if( mComboLNAGain.isEnabled() )
			{
				((R820TTunerController) device).setLNAGain( lnaGain, true );
			}

			save();
		}
		catch ( UsbException e ) {
			Log.errorDialog( 
					"R820T Tuner Controller - couldn't apply the LNA ",
					"gain setting - " + e.getLocalizedMessage() );  

			Log.println( "R820T Tuner Controller - couldn't apply LNA "
					+ "gain setting - " + e );
		}
		saveParam(mComboLNAGain, "mComboLNAGain");
	}

	private void setVgaGain() {
		try {
			R820TVGAGain vgaGain = 
					(R820TVGAGain)mComboVGAGain.getSelectedItem();

			if( vgaGain == null )
			{
				vgaGain = DEFAULT_GAIN.getVGAGain();
			}

			if( mComboVGAGain.isEnabled() )
			{
				((R820TTunerController) device).setVGAGain( vgaGain, true );
			}

			save();
		}
		catch ( UsbException e ) {
			Log.errorDialog(
					"R820T Tuner Controller - couldn't apply the VGA ",
					"gain setting - " + e.getLocalizedMessage() );  

			Log.println( "R820T Tuner Controller - couldn't apply VGA "
					+ "gain setting" + e );
		}
		saveParam(mComboVGAGain, "mComboVGAGain");
	}


private void setSampleRate() {
	SampleRate sampleRate = (SampleRate)mComboSampleRate.getSelectedItem();
	try {
		((RTL2832TunerController) device).setSampleRate( sampleRate );
		save();
	}
	catch ( DeviceException | LibUsbException eSampleRate ) {
		Log.errorDialog(  
				"R820T Tuner Controller - couldn't apply the sample ",
				"rate setting [" + sampleRate.getLabel() + "] " + 
						eSampleRate.getLocalizedMessage() );  

		Log.println( "R820T Tuner Controller - couldn't apply sample "
				+ "rate setting [" + sampleRate.getLabel() + "] " + 
				eSampleRate );
	} 
	saveParam(mComboSampleRate, "mComboSampleRate");
}

@Override
public void actionPerformed(ActionEvent e) {
	if (e.getSource() == mComboSampleRate) {
		setSampleRate();
	}
	if (e.getSource() == mComboMasterGain) {
		setGain();
	}
	if (e.getSource() == mComboMixerGain) {
		setMixerGain();
	}
	if (e.getSource() == mComboLNAGain) {
		setLnaGain();
	}
	if (e.getSource() == mComboVGAGain) {
		setVgaGain();
	}

}

@Override
public void itemStateChanged(ItemEvent e) {



}

	
    /**
     * Read the configuration from the device
     */
    private void getConfig() {
    	
    }
    
	private void loadConfig() {
		
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == this.mFrequencyCorrection) {
/*
			final double value = ((SpinnerNumberModel)mFrequencyCorrection
					.getModel()).getNumber().doubleValue();

			try
			{
				device.setFrequencyCorrection( value );
			} 
			catch ( SourceException e1 )
			{
				Log.println( "Error setting frequency correction value: " + e1 );
			}

			save();
		*/
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
