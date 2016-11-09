package device.airspy;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.Box;
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

import common.Log;
import decoder.SourceIQ;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.airspy.AirspyDevice.Gain;
import device.airspy.AirspyDevice.GainMode;

@SuppressWarnings("serial")
public class AirspyPanel extends DevicePanel implements ItemListener, ActionListener, Runnable, ChangeListener {
	int NUM_OF_PARAMS = 15;
	boolean running = true;
	boolean done = false;
	AirspyDevice device;
	JCheckBox cbMixerGain;
	JCheckBox cbLnaGain;
	JTextField rfFilterValue;
	JTextField ifFilterValue;
	JSpinner ifSpinner;
	
	private JComboBox<AirspySampleRate> mSampleRateCombo;
	private JComboBox<Integer> cbDecimation;
	AirspyTunerConfiguration config = new AirspyTunerConfiguration();
	
	private JTextField mConfigurationName;
    private JButton mTunerInfo;

    private JSpinner mFrequencyCorrection;

    private JComboBox<GainMode> mGainModeCombo;

    private JSlider mMasterGain;
    private JLabel mMasterGainValueLabel;

    private JSlider mIFGain;
    private JLabel mIFGainValueLabel;

    private JSlider mLNAGain;
    private JLabel mLNAGainValueLabel;

    private JSlider mMixerGain;
    private JLabel mMixerGainValueLabel;

    private JCheckBox mLNAAGC;
    private JCheckBox mMixerAGC;

    //private AirspyDevice device;
    private boolean mLoading;
	
	public AirspyPanel() throws IOException, DeviceException {
		TitledBorder title = new TitledBorder(null, "Airspy", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
	}
	
	public void setEnabled(boolean b) {
		if (mSampleRateCombo != null)
			mSampleRateCombo.setEnabled(b);
		if (cbDecimation != null)
			cbDecimation.setEnabled(b);
	}
	public void initializeGui() throws IOException, DeviceException {
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		JPanel top = new JPanel();
		add(top, BorderLayout.NORTH);
		top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
		/*
		cbMixerGain = new JCheckBox("Mixer Gain");
		top.add(cbMixerGain);
		cbMixerGain.addItemListener(this);
		cbLnaGain = new JCheckBox("LNA Gain    ");
		top.add(cbLnaGain);
		cbLnaGain.addItemListener(this);
		*/
		/**
		 * Sample Rate
		 */
		top.add( new JLabel( "Sample Rate:  " ) );
		List<AirspySampleRate> rates = device.getSampleRates();
		mSampleRateCombo = new JComboBox<AirspySampleRate>( 
			new DefaultComboBoxModel<AirspySampleRate>( rates.toArray( 
					new AirspySampleRate[ rates.size() ] ) ) );

		mSampleRateCombo.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				AirspySampleRate rate = (AirspySampleRate)mSampleRateCombo.getSelectedItem();

				try	{
					device.setSampleRate( rate );
					
					//save();
				} 
				catch ( LibUsbException | UsbException | DeviceException e1 ) {
					//JOptionPane.showMessageDialog( AirspyTunerEditor.this, 
					//	"Couldn't set sample rate to " + rate.getLabel() );
					
					Log.errorDialog( "Error setting airspy sample rate", e1.getMessage() );
				} 
			}
		} );
		
		top.add( mSampleRateCombo );
		mSampleRateCombo.setEnabled(false);

		top.add( new JLabel( "  Decimation:  " ) );
		Integer[] decRates = {1,2,4,8,16,32};
		cbDecimation = new JComboBox<Integer>( 
			new DefaultComboBoxModel<Integer>( decRates));
			cbDecimation.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				
			}
		} );
		
		top.add( cbDecimation );
		cbDecimation.setEnabled(false);

		 /**
         * Frequency Correction
         
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

        mFrequencyCorrection.addChangeListener( new ChangeListener() 
        {
			@Override
            public void stateChanged( ChangeEvent e )
            {
				final double value = ((SpinnerNumberModel)mFrequencyCorrection
						.getModel()).getNumber().doubleValue();
				
                try
				{
                	device.setFrequencyCorrection( value );
				} 
                catch ( SourceException e1 )
				{
					mLog.error( "Error setting frequency correction value", e1 );
				}
                
                save();
            }
        } );
        
        add( new JLabel( "PPM:" ) );
        add( mFrequencyCorrection );
		*/
//		top.add( new JSeparator() );


		/**
		 * Gain Mode
		 */
		top.add( new JLabel( "  Gain Mode:  " ) );
		mGainModeCombo = new JComboBox<GainMode>( GainMode.values() );
		mGainModeCombo.setEnabled( true );
		mGainModeCombo.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
				updateGainComponents( mode );			
			}
		} );
		
		top.add( mGainModeCombo );

		JPanel centerLeft = new JPanel();
		JPanel centerRight = new JPanel();
		add(center, BorderLayout.CENTER);
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		center.add(centerLeft);
		centerLeft.setLayout(new BoxLayout(centerLeft, BoxLayout.Y_AXIS));
		center.add(centerRight);
		centerRight.setLayout(new BoxLayout(centerRight, BoxLayout.Y_AXIS));

		/**
		 * Gain
		 */
		JPanel line1 = new JPanel();
		centerLeft.add(line1);
		line1.add( new JLabel( "Master:" ) );
		
		mMasterGain = new JSlider( JSlider.HORIZONTAL, 
				AirspyDevice.GAIN_MIN, 
				AirspyDevice.GAIN_MAX,
				AirspyDevice.GAIN_MIN );
		mMasterGain.setMajorTickSpacing( 1 );
		mMasterGain.setPaintTicks( true );
		
		mMasterGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event ) {
				GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
				int value = mMasterGain.getValue();
				Gain gain = Gain.getGain( mode, value );

				try {
					device.setGain( gain );
					mMasterGainValueLabel.setText( String.valueOf( gain.getValue() ) );
				} 
				catch ( Exception e ) {
					Log.errorDialog( "Couldn't set airspy gain to:" + gain.name(), e.getMessage() );
					JOptionPane.showMessageDialog( mMasterGain, "Couldn't set gain value to " + 
							gain.getValue() );
				}
				updateGainComponents( mode );
			}
		} );

		line1.add( mMasterGain );
		
		mMasterGainValueLabel = new JLabel( "0" );
		line1.add( mMasterGainValueLabel );
		
		JPanel line2 = new JPanel();
		centerLeft.add(line2);
		line2.add( new JLabel( "IF:" ) );
		
		mIFGain = new JSlider( JSlider.HORIZONTAL, 
								AirspyDevice.IF_GAIN_MIN, 
								AirspyDevice.IF_GAIN_MAX,
								AirspyDevice.IF_GAIN_MIN );
		
		mIFGain.setMajorTickSpacing( 1 );
		mIFGain.setPaintTicks( true );
		
		mIFGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event ) {
				int gain = mIFGain.getValue();

				try {
					device.setIFGain( gain );
					//save();
					mIFGainValueLabel.setText( String.valueOf( gain ) );
				} 
				catch ( Exception e ) {
					Log.errorDialog( "Couldn't set airspy IF gain to:" + gain, e.getMessage() );
					
					JOptionPane.showMessageDialog( mIFGain, "Couldn't set IF gain value to " + gain );
				}
			}
		} );

		line2.add( mIFGain);
		
		mIFGainValueLabel = new JLabel( "0" );
		line2.add( mIFGainValueLabel );
		
		/**
		 *  Mixer Gain 
		 */
		JPanel line3 = new JPanel();
		centerLeft.add(line3);
		line3.add( new JLabel( "Mixer:" ) );
		
		mMixerGain = new JSlider( JSlider.HORIZONTAL, 
								AirspyDevice.MIXER_GAIN_MIN, 
								AirspyDevice.MIXER_GAIN_MAX,
								AirspyDevice.MIXER_GAIN_MIN );
		
		mMixerGain.setMajorTickSpacing( 1 );
		mMixerGain.setPaintTicks( true );
		
		mMixerGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				int gain = mMixerGain.getValue();

				try
				{
					device.setMixerGain( gain );
					//save();
					mMixerGainValueLabel.setText( String.valueOf( gain ) );
				} 
				catch ( Exception e )
				{
					Log.errorDialog( "Couldn't set airspy Mixer gain to:" + gain, e.getMessage() );
					JOptionPane.showMessageDialog( mIFGain, "Couldn't set Mixer gain value to " + gain );
				}
			}
		} );

		line3.add( mMixerGain);

		mMixerGainValueLabel = new JLabel( "0" );
		line3.add( mMixerGainValueLabel );
		
		JPanel line4 = new JPanel();
		centerLeft.add(line4);
		line4.add( new JLabel( "LNA:" ) );
		
		mLNAGain = new JSlider( JSlider.HORIZONTAL, 
								AirspyDevice.LNA_GAIN_MIN, 
								AirspyDevice.LNA_GAIN_MAX,
								AirspyDevice.LNA_GAIN_MIN );
		
		mLNAGain.setMajorTickSpacing( 1 );
		mLNAGain.setPaintTicks( true );
		
		mLNAGain.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				int gain = mLNAGain.getValue();

				try
				{
					device.setLNAGain( gain );
					//save();
					mLNAGainValueLabel.setText( String.valueOf( gain ) );
				} 
				catch ( Exception e )
				{
					Log.errorDialog( "Couldn't set airspy LNA gain to:" + gain, e.getMessage() );
					JOptionPane.showMessageDialog( mIFGain, "Couldn't set LNA gain value to " + gain );
				}
			}
		} );

		line4.add( mLNAGain );

		mLNAGainValueLabel = new JLabel( "0" );
		line4.add( mLNAGainValueLabel );
		
		mMixerAGC = new JCheckBox( "Mixer AGC" );
		mMixerAGC.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					device.setMixerAGC( mMixerAGC.isSelected() );
					mMixerGain.setEnabled( !mMixerAGC.isSelected() );
					
					//save();
				} 
				catch ( Exception e1 )
				{
					Log.errorDialog( "ERROR", "Error setting Mixer AGC Enabled" );
				}
			}
		} );
		
		centerRight.add( mMixerAGC);

		mLNAAGC = new JCheckBox( "LNA AGC" );
		mLNAAGC.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					device.setLNAAGC( mLNAAGC.isSelected() );
					mLNAGain.setEnabled( !mLNAAGC.isSelected() );

					
				} 
				catch ( Exception e1 )
				{
					Log.errorDialog( "ERROR", "Error setting LNA AGC Enabled" );
				}
				
			}
		} );
		
		centerRight.add( mLNAAGC );
	}
	
	@Override
	public void setDevice(TunerController fcd) throws IOException, DeviceException {
		setAirpy((AirspyDevice)fcd);
		initializeGui();
		GainMode mode = (GainMode)mGainModeCombo.getSelectedItem();
		updateGainComponents( mode );

	}
	private void setAirpy(AirspyDevice f) throws IOException, DeviceException { 
		device = f; 
		getSettings();
	}
	
	public void updateFilter() throws IOException, DeviceException {
		//rfFilterValue.setText(fcd.getRfFilter());
	}
	
	public void getSettings()  throws IOException, DeviceException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  // Allow startup to settle down first
		//mSampleRateCombo.setSelectedItem("");

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

	@Override
	public void actionPerformed(ActionEvent e) {
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == cbLnaGain) {
			loadConfig();
		}
		if (e.getSource() == cbMixerGain) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				;//fcd.setMixerGain(false);
			} else {
				;//fcd.setMixerGain(true);
			}
			//cbMixerGain.setSelected(fcd.getMixerGain());
		}
		
		
	}

	/**
     * Updates the enabled state of each of the gain controls according to the
     * specified gain mode.  The master gain control is enabled for linearity
     * and sensitivity and the individual gain controls are disabled, and 
     * vice-versa for custom mode.
     */
    private void updateGainComponents( GainMode mode )
    {
		switch( mode )
		{
			case LINEARITY:
					mMasterGain.setEnabled( true );
					mMasterGainValueLabel.setEnabled( true );
				    mIFGain.setEnabled( false );
					mIFGainValueLabel.setEnabled( false );
					mLNAAGC.setEnabled( false );
				    mLNAGainValueLabel.setEnabled( false );
				    mLNAGain.setEnabled( false );
				    mMixerGainValueLabel.setEnabled( false );
				    mMixerAGC.setEnabled( false );
				    mMixerGain.setEnabled( false );
				break;
			case SENSITIVITY:
					mMasterGain.setEnabled( true );
					mMasterGainValueLabel.setEnabled( true );
				    mIFGain.setEnabled( false );
					mIFGainValueLabel.setEnabled( false );
					mLNAAGC.setEnabled( false );
				    mLNAGainValueLabel.setEnabled( false );
				    mLNAGain.setEnabled( false );
				    mMixerGainValueLabel.setEnabled( false );
				    mMixerAGC.setEnabled( false );
				    mMixerGain.setEnabled( false );
				break;
			case CUSTOM:
					mMasterGain.setEnabled( false );
					mMasterGainValueLabel.setEnabled( false );
				    mIFGain.setEnabled( true );
					mIFGainValueLabel.setEnabled( true );
					mLNAAGC.setEnabled( true );
				    mLNAGainValueLabel.setEnabled( true );
				    mLNAGain.setEnabled( true );
				    mMixerGainValueLabel.setEnabled( true );
				    mMixerAGC.setEnabled( true );
				    mMixerGain.setEnabled( true );
			default:
				break;
		}
		AirspyTunerConfiguration airspy = config;
		int value = mMasterGain.getValue();
		Gain gain = Gain.getGain( mode, value );

		if( mode == GainMode.CUSTOM ) {
			mIFGain.setValue( airspy.getIFGain() );
			mMixerGain.setValue( airspy.getMixerGain() );
			mMixerAGC.setSelected( airspy.isMixerAGC() );
			mLNAGain.setValue( airspy.getLNAGain() );
			mLNAAGC.setSelected( airspy.isLNAAGC() );
		}
		else {
			mMixerAGC.setSelected( false );
			mLNAAGC.setSelected( false );
			mMasterGain.setValue( gain.getValue() );
			mIFGain.setValue( gain.getIF() );
			mMixerGain.setValue( gain.getMixer() );
			mLNAGain.setValue( gain.getLNA() );
		}
    }
    
    /**
     * Read the configuration from the device
     */
    private void getConfig() {
    	
    }
    
	private void loadConfig() {
		try {
			Log.println("Loading config");
			device.apply(config);
		} catch (LibUsbException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (DeviceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UsbException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		
		if (e.getSource() == ifSpinner) {
			int u = Integer.parseInt((String) ifSpinner.getValue());
	        try {
	        	Log.println("Setting IF Gain to: " + u);
				device.setIFGain(u);
			} catch (DeviceException e1) {
				Log.println("Error setting IF Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			}
		}
		
	}

	@Override
	public int getSampleRate() {
		return device.getAirspySampleRate().getRate();
	}

	@Override
	public int getDecimationRate() {
		return (int)cbDecimation.getSelectedItem();
	}


}
