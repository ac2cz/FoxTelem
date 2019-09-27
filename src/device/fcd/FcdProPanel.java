package device.fcd;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.Config;
import common.Log;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;

@SuppressWarnings("serial")
public class FcdProPanel extends DevicePanel implements ItemListener, ActionListener, Runnable {
	int NUM_OF_PARAMS = 15;
	@SuppressWarnings("rawtypes")
	JComboBox cbMixerGain;
	@SuppressWarnings("rawtypes")
	JComboBox cbLnaGain;
	JTextField rfFilterValue;
	JTextField bandValue;
	JTextField ifFilterValue;
	
	public FcdProPanel() throws IOException, DeviceException {
		TitledBorder title = new TitledBorder(null, "Funcube Dongle Pro", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
		initializeGui();
	}
	
	public void setEnabled(boolean b) {
		cbMixerGain.setEnabled(b);
		cbLnaGain.setEnabled(b);		
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void initializeGui() throws IOException, DeviceException {
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		add(center, BorderLayout.NORTH);
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		
		// LNA gain 10 default, -5 to +30 
		JLabel lblLna = new JLabel("LNA Gain");
		center.add(lblLna);
		cbLnaGain = new JComboBox(FCD1TunerController.lnaGain);
		center.add(cbLnaGain);
		cbLnaGain.addItemListener(this);
		
		// Mixer Gain 4dB or 12dB
		JLabel lblMix = new JLabel("    Mixer Gain");
		center.add(lblMix);
		cbMixerGain = new JComboBox(FCD1TunerController.mixerGain);
		
		center.add(cbMixerGain);
		cbMixerGain.addItemListener(this);
		
		// IF Gain 1 - -3dB or +6dB
		
		// Band
		JLabel lblband = new JLabel("    Band");
		center.add(lblband);
		bandValue = new JTextField();
		bandValue.setMinimumSize(new Dimension(40,10));
		bandValue.setEnabled(false);
		center.add(bandValue);
		
		// RF Filter - not needed 268Mhz vs 298Mhz
		JLabel rfFilter = new JLabel("    RF Filter");
		center.add(rfFilter);
		rfFilterValue = new JTextField();
		rfFilterValue.setMinimumSize(new Dimension(70,10));
		rfFilterValue.setEnabled(false);
		center.add(rfFilterValue);


	}
	
	@Override
	public void setDevice(TunerController fcd) throws IOException, DeviceException {
		setFcd((FCD1TunerController)fcd);
		
	}
	public void setFcd(FCD1TunerController f) throws IOException, DeviceException { 
		device = f; 
		getSettings();
	}
	
	public void updateFilter() throws IOException, DeviceException {
		rfFilterValue.setText(((FCD1TunerController) device).getRfFilter());
		bandValue.setText(((FCD1TunerController) device).getBand());
	}
	
	public void getSettings()  throws IOException, DeviceException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  // Allow startup to settle down first
		
		loadParam(cbLnaGain, "cbLnaGain");
		setLnaGain(cbLnaGain.getSelectedIndex());
		loadParam(cbMixerGain, "cbMixerGain");
		setMixerGain(cbMixerGain.getSelectedIndex());
		rfFilterValue.setText(((FCD1TunerController) device).getRfFilter());
		bandValue.setText(((FCD1TunerController) device).getBand());
//		ifFilterValue.setText(fcd.getIfFilter());
	}
	
	
	@Override
	public void run() {
		done = false;
		running = true;
		Thread.currentThread().setName("FCDProPanel");

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
	
	private void setLnaGain(int position) {
		try {
			((FCD1TunerController) device).setLnaGain(position);
		} catch (DeviceException e1) {
			Log.println("Error setting LNA Gain on FCD");
			e1.printStackTrace(Log.getWriter());
		}
		if (Config.saveFcdParams)
			saveParam(cbLnaGain, "cbLnaGain");
	}

	private void setMixerGain(int position) {
		try {
			if (position == 1)
				((FCD1TunerController) device).setMixerGain(true);
			else 
				((FCD1TunerController) device).setMixerGain(false);
		} catch (DeviceException e1) {
			Log.println("Error setting LNA Gain on FCD");
			e1.printStackTrace(Log.getWriter());
		}
		if (Config.saveFcdParams)
			saveParam(cbMixerGain, "cbMixerGain");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == cbMixerGain) {
			setMixerGain(cbMixerGain.getSelectedIndex());			
		}
		if (e.getSource() == cbLnaGain) {
			setLnaGain(cbLnaGain.getSelectedIndex());
		}
	}

	@Override
	public int getSampleRate() {
		return FCD1TunerController.SAMPLE_RATE;
	}

	@Override
	public int getDecimationRate() {
		return 1;
	}

	

}