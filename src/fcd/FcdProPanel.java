package fcd;

import java.awt.BorderLayout;
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

import common.Log;
import device.Device;
import device.DeviceException;
import device.DevicePanel;

@SuppressWarnings("serial")
public class FcdProPanel extends DevicePanel implements ItemListener, ActionListener, Runnable {
	int NUM_OF_PARAMS = 15;
	boolean running = true;
	boolean done = false;
	FcdProDevice fcd;
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
		bandValue.setEnabled(b);
		
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
		cbLnaGain = new JComboBox(FcdProDevice.lnaGain);
		center.add(cbLnaGain);
		cbLnaGain.addItemListener(this);
		
		// Mixer Gain 4dB or 12dB
		JLabel lblMix = new JLabel("    Mixer Gain");
		center.add(lblMix);
		cbMixerGain = new JComboBox(FcdProDevice.mixerGain);
		
		center.add(cbMixerGain);
		cbMixerGain.addItemListener(this);
		
		// IF Gain 1 - -3dB or +6dB
		
		// Band
		JLabel lblband = new JLabel("    Band");
		center.add(lblband);
		bandValue = new JTextField();
		bandValue.setEnabled(false);
		center.add(bandValue);
		
		// RF Filter - not needed 268Mhz vs 298Mhz
		JLabel rfFilter = new JLabel("    RF Filter");
		center.add(rfFilter);
		rfFilterValue = new JTextField();
		rfFilterValue.setEnabled(false);
		center.add(rfFilterValue);


	}
	
	@Override
	public void setDevice(Device fcd) throws IOException, DeviceException {
		setFcd((FcdProDevice)fcd);
		
	}
	public void setFcd(FcdProDevice f) throws IOException, DeviceException { 
		fcd = f; 
		getSettings();
	}
	
	public void updateFilter() throws IOException, DeviceException {
		rfFilterValue.setText(fcd.getRfFilter());
		bandValue.setText(fcd.getBand());
	}
	
	public void getSettings()  throws IOException, DeviceException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  // Allow startup to settle down first
		boolean mixer = fcd.getMixerGain();
		if (mixer)
			cbMixerGain.setSelectedIndex(1);
		else 
			cbMixerGain.setSelectedIndex(0);
		cbLnaGain.setSelectedIndex(fcd.getLnaGain());	
		rfFilterValue.setText(fcd.getRfFilter());
		bandValue.setText(fcd.getBand());
//		ifFilterValue.setText(fcd.getIfFilter());
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


			if (fcd != null) {
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
		if (e.getSource() == cbMixerGain) {
			try {
				int position = cbMixerGain.getSelectedIndex();
				if (position == 1)
					fcd.setMixerGain(true);
				else 
					fcd.setMixerGain(false);
			} catch (DeviceException e1) {
				Log.println("Error setting LNA Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			} 
		}
		if (e.getSource() == cbLnaGain) {
			try {
				int position = cbLnaGain.getSelectedIndex();
				fcd.setLnaGain(position);
			} catch (DeviceException e1) {
				Log.println("Error setting LNA Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			} 
		}
		
	}

	@Override
	public int getSampleRate() {
		return fcd.SAMPLE_RATE;
	}

	@Override
	public int getDecimationRate() {
		return 1;
	}

	

}
