package device.rtl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import device.fcd.FCD1TunerController;
import device.fcd.FCD2TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.rtl.R820TTunerController.R820TGain;
import device.rtl.R820TTunerController.R820TLNAGain;
import device.rtl.R820TTunerController.R820TMixerGain;
import device.rtl.R820TTunerController.R820TVGAGain;
import device.rtl.RTL2832TunerController.SampleRate;

@SuppressWarnings("serial")
public class RTLPanel extends DevicePanel implements ItemListener, ActionListener, ChangeListener {
	private static final R820TGain DEFAULT_GAIN = R820TGain.GAIN_279;
	
	int NUM_OF_PARAMS = 15;

   // private JTextField mConfigurationName;
    //private JButton mTunerInfo;
//    private JComboBox<SampleRate> mComboSampleRate;
    private JSpinner mFrequencyCorrection;
    private JComboBox<R820TGain> mComboMasterGain;
    private JComboBox<R820TMixerGain> mComboMixerGain;
    private JComboBox<R820TLNAGain> mComboLNAGain;
    private JComboBox<R820TVGAGain> mComboVGAGain;	
    private JCheckBox cbBiasTee;
	boolean loading = true;;
	private JLabel lblSampleRate;
	public static final String SAMPLE_RATE = "Sample Rate: ";
	
	// Saved Values
 //   R820TGain gain;
    
	public RTLPanel() throws IOException, DeviceException {
		TitledBorder title = new TitledBorder(null, "R820T USB SDR", TitledBorder.LEADING, TitledBorder.TOP, null, null);
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
		bottom.setLayout(new FlowLayout());
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

        top.add(new Box.Filler(new Dimension(10,10), new Dimension(10,10), new Dimension(10,10)));
		cbBiasTee = new JCheckBox("Bias T");
		top.add(cbBiasTee);
		cbBiasTee.addItemListener(this);
        
        /**
         * Gain Controls 
         */
        center.add( new JLabel( "Gain" ));
        
        /* Master Gain Control */
        mComboMasterGain = new JComboBox<R820TGain>( R820TGain.values() );
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
		if( gain != R820TGain.MANUAL ) 
			mComboMixerGain.setEnabled( false );
        mComboMixerGain.addActionListener(this);
        mComboMixerGain.setToolTipText( "<html>Mixer Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        
        center.add( new JLabel( "Mixer:" ) );
        center.add( mComboMixerGain );

        /* LNA Gain Control */
        mComboLNAGain = new JComboBox<R820TLNAGain>( R820TLNAGain.values() );
		if( gain != R820TGain.MANUAL ) 
			mComboLNAGain.setEnabled( false );
        mComboLNAGain.addActionListener(this);
        mComboLNAGain.setToolTipText( "<html>LNA Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        
        center.add( new JLabel( "LNA:" ) );
        center.add( mComboLNAGain );

        /* VGA Gain Control */
        mComboVGAGain = new JComboBox<R820TVGAGain>( R820TVGAGain.values() );
		if( gain != R820TGain.MANUAL ) 
			mComboVGAGain.setEnabled( false );
        mComboVGAGain.addActionListener(this);
        mComboVGAGain.setToolTipText( "<html>VGA Gain.  Set master gain "
        		+ "to <b>MANUAL</b> to enable adjustment</html>" );
        center.add( new JLabel( "VGA:" ) );
        center.add( mComboVGAGain );
//        loadParam(mComboMasterGain, "mComboMasterGain");
//        loadParam(mComboMixerGain, "mComboMixerGain");
//        loadParam(mComboLNAGain, "mComboLNAGain");
//        loadParam(mComboVGAGain, "mComboVGAGain");
        
//        JLabel lblPpmCorrection = new JLabel("Freq Correction (ppm)");
//        ppmCorrection = new JTextField(0);
//        ppmCorrection.setColumns(4);
//        bottom.add(lblPpmCorrection);
//        bottom.add(ppmCorrection);
      
        
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
		loadParam(mComboVGAGain, "mComboVGAGain");
		loadParam(mFrequencyCorrection, "mFrequencyCorrection");
		loadParam(cbBiasTee, "cbBiasTee");
		
		setGain();
		setMixerGain();
		setLnaGain();
		setVgaGain();
		setFrequencyCorrection();
		setBiasTee(cbBiasTee.isSelected());
		
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
			saveParam(mComboVGAGain, "mComboVGAGain");
			saveParam(mFrequencyCorrection, "mFrequencyCorrection");
			saveParam(cbBiasTee.isSelected(), "cbBiasTee");
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
			save();
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
	}
	
	private void setBiasTee(boolean b) {
		Log.println("RTL Bias Tee: " + b);
		if (b == false) {
			((R820TTunerController) device).setBiasTee(false);
		} else {
			((R820TTunerController) device).setBiasTee(true);
		}
		save();
	}


private void setSampleRate() {
//	SampleRate sampleRate = (SampleRate)mComboSampleRate.getSelectedItem();
//	try {
//		((RTL2832TunerController) device).setSampleRate( sampleRate );
//		save();
//	}
//	catch ( DeviceException | LibUsbException eSampleRate ) {
//		Log.errorDialog(  
//				"R820T Tuner Controller - couldn't apply the sample ",
//				"rate setting [" + sampleRate.getLabel() + "] " + 
//						eSampleRate.getLocalizedMessage() );  
//
//		Log.println( "R820T Tuner Controller - couldn't apply sample "
//				+ "rate setting [" + sampleRate.getLabel() + "] " + 
//				eSampleRate );
//	} 
}

private void setFrequencyCorrection() {
	
	try {
		int rate = (int) mFrequencyCorrection.getValue();
		((RTL2832TunerController) device).setSampleRateFrequencyCorrection( rate );
		save();
	}
	catch ( DeviceException | LibUsbException eSampleRate ) {
		Log.errorDialog(  
				"R820T Tuner Controller - couldn't set the frequency correction ",
				"rate [" +  mFrequencyCorrection.getValue() + "] " + 
						eSampleRate.getLocalizedMessage() );  

		Log.println( "R820T Tuner Controller - couldn't set the frequency correction "
				+ "rate setting [" +   mFrequencyCorrection.getValue() + "] " + 
				eSampleRate );
	} 
}

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
	if (e.getSource() == mComboVGAGain) {
		setVgaGain();
	}

}

@Override
public void itemStateChanged(ItemEvent e) {
	if (e.getSource() == cbBiasTee ) {

		if (e.getStateChange() == ItemEvent.DESELECTED) {
			setBiasTee(false);
		} else {
			setBiasTee(true);
		}
	}
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
