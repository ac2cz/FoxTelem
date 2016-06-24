package device.airspy;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import org.usb4java.LibUsbException;

import common.Log;
import device.Device;
import device.DeviceException;
import device.DevicePanel;

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
	AirspyTunerConfiguration config = new AirspyTunerConfiguration();
	
	public AirspyPanel() throws IOException, DeviceException {
		TitledBorder title = new TitledBorder(null, "Airspy", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
	}
	
	public void setEnabled(boolean b) {

		cbMixerGain.setEnabled(b);
		cbLnaGain.setEnabled(b);
	}
	public void initializeGui() throws IOException, DeviceException {
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		JPanel top = new JPanel();
		add(top, BorderLayout.NORTH);
		top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
		cbMixerGain = new JCheckBox("Mixer Gain");
		top.add(cbMixerGain);
		cbMixerGain.addItemListener(this);
		cbLnaGain = new JCheckBox("LNA Gain    ");
		top.add(cbLnaGain);
		cbLnaGain.addItemListener(this);
		
		/**
		 * Sample Rate
		 */
		top.add( new JLabel( "Sample Rate:" ) );
		
		List<AirspySampleRate> rates = device.getSampleRates();
		
		mSampleRateCombo = new JComboBox<AirspySampleRate>( 
			new DefaultComboBoxModel<AirspySampleRate>( rates.toArray( 
					new AirspySampleRate[ rates.size() ] ) ) );
		mSampleRateCombo.setEnabled( false );
		mSampleRateCombo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				AirspySampleRate rate = (AirspySampleRate)mSampleRateCombo.getSelectedItem();

				try
				{
					device.setSampleRate( rate );
					//save();
				} 
				catch ( LibUsbException | UsbException | DeviceException e1 )
				{
					//JOptionPane.showMessageDialog( AirspyTunerEditor.this, 
					//	"Couldn't set sample rate to " + rate.getLabel() );
					
					Log.errorDialog( "Error setting airspy sample rate", e1.getMessage() );
				} 
			}
		} );
		
		top.add( mSampleRateCombo );
		mSampleRateCombo.setEnabled(true);
		/*
		JLabel lblIfGain = new JLabel("IF Gain ");
		top.add(lblIfGain);
		String[] vals = new String[60];
		for (int i=0; i < 60; i++)
			vals[i] = ""+i;
		SpinnerListModel ifGainModel = new SpinnerListModel(vals);
		ifSpinner = new JSpinner(ifGainModel);
		JComponent editor = ifSpinner.getEditor();
	    JFormattedTextField ftf = ((JSpinner.DefaultEditor)editor).getTextField();
		ftf.setColumns(3);
		ifSpinner.addChangeListener(this);
		top.add(ifSpinner);
		
		*/
		add(center, BorderLayout.CENTER);
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		
		JLabel rfFilter = new JLabel("    RF Filter");
//		top.add(rfFilter);
		rfFilterValue = new JTextField();
		rfFilterValue.setColumns(35);
		rfFilterValue.setEnabled(false);
//		top.add(rfFilterValue);
		
		JLabel ifFilter = new JLabel("    IF Filter");
//		top.add(ifFilter);
		ifFilterValue = new JTextField();
		ifFilterValue.setColumns(30);
		ifFilterValue.setEnabled(false);
//		top.add(ifFilterValue);
		top.add(new Box.Filler(new Dimension(10,10), new Dimension(500,10), new Dimension(1000,10)));
	}
	
	@Override
	public void setDevice(Device fcd) throws IOException, DeviceException {
		setAirpy((AirspyDevice)fcd);
		initializeGui();
	}
	public void setAirpy(AirspyDevice f) throws IOException, DeviceException { 
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
		//cbMixerGain.setSelected(fcd.getMixerGain());
	//	cbLnaGain.setSelected(device.getLnaGain());	
		//rfFilterValue.setText(fcd.getRfFilter());
		//ifFilterValue.setText(fcd.getIfFilter());
//		int ifG = fcd.getIFGain();
//		ifSpinner.setValue(""+ifG);
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

	

}
