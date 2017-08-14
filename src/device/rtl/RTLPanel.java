package device.rtl;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.usb.UsbException;

import org.usb4java.LibUsbException;

import common.Log;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;
import device.airspy.AirspyDevice.Gain;
import device.airspy.AirspyDevice.GainMode;

@SuppressWarnings("serial")
public class RTLPanel extends DevicePanel implements ItemListener, ActionListener, Runnable, ChangeListener {
	int NUM_OF_PARAMS = 15;
	boolean running = true;
	boolean done = false;
	RTL2832TunerController device;
	JCheckBox cbMixerGain;
	JCheckBox cbLnaGain;
	JTextField rfFilterValue;
	JTextField ifFilterValue;
	JSpinner ifSpinner;
	
	
	
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
		
		
	}
	
	@Override
	public void setDevice(TunerController d) throws IOException, DeviceException {
		device = (RTL2832TunerController) d; 
		initializeGui();
		

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
     * Read the configuration from the device
     */
    private void getConfig() {
    	
    }
    
	private void loadConfig() {
		
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		
		
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
