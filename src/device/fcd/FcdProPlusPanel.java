package device.fcd;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import common.Config;
import common.Log;
import device.TunerController;
import device.DeviceException;
import device.DevicePanel;

@SuppressWarnings("serial")
public class FcdProPlusPanel extends DevicePanel implements ItemListener, ActionListener, Runnable, ChangeListener {
	int NUM_OF_PARAMS = 15;
	JCheckBox cbMixerGain;
	JCheckBox cbLnaGain;
	JCheckBox cbBiasTee;
	JTextField rfFilterValue;
	JTextField ifFilterValue;
	JSpinner ifSpinner;
	boolean checkingSettings = false;
	
	public FcdProPlusPanel() throws IOException {
		TitledBorder title = new TitledBorder(null, "Funcube Dongle Pro Plus", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
		try {
			initializeGui();
		} catch (DeviceException e) {
			// Don't stop the GUI thread, just report the error
			Log.println("ERROR setting the inital settings for the FCD");
			e.printStackTrace(Log.getWriter());
		}
	}
	
	public void setEnabled(boolean b) {

		cbMixerGain.setEnabled(b);
		cbLnaGain.setEnabled(b);
		cbBiasTee.setEnabled(b);
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
		cbLnaGain = new JCheckBox("LNA Gain");
		top.add(cbLnaGain);
		cbLnaGain.addItemListener(this);
		top.add(new Box.Filler(new Dimension(10,10), new Dimension(10,10), new Dimension(10,10)));
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
		top.add(rfFilter);
		rfFilterValue = new JTextField();
		rfFilterValue.setColumns(35);
		rfFilterValue.setEnabled(false);
		rfFilterValue.setMinimumSize(new Dimension(70,10));
		top.add(rfFilterValue);
		
		JLabel ifFilter = new JLabel("    IF Filter");
		top.add(ifFilter);
		ifFilterValue = new JTextField();
		ifFilterValue.setColumns(30);
		ifFilterValue.setEnabled(false);
		ifFilterValue.setMinimumSize(new Dimension(40,10));
		top.add(ifFilterValue);
		
		top.add(new Box.Filler(new Dimension(10,10), new Dimension(10,10), new Dimension(10,10)));
		cbBiasTee = new JCheckBox("Bias T");
		top.add(cbBiasTee);
		cbBiasTee.addItemListener(this);
		
		top.add(new Box.Filler(new Dimension(10,10), new Dimension(500,10), new Dimension(1000,10)));
	}
	
	@Override
	public void setDevice(TunerController fcd) throws IOException, DeviceException {
		setFcd((FCD2TunerController)fcd);
		
	}
	public void setFcd(FCD2TunerController f) throws IOException, DeviceException { 
		device = f; 
		getSettings();
	}
	
	public void updateFilter() throws IOException, DeviceException {
		rfFilterValue.setText(((FCD2TunerController) device).getRfFilter());
		ifFilterValue.setText(((FCD2TunerController) device).getIfFilter());	
	}
	
	private void checkSettings() throws DeviceException {
		// THIS DOES NOT WORK...
		checkingSettings = true;
		boolean lnaGain = ((FCD2TunerController) device).getLnaGain();
		if (lnaGain != cbLnaGain.isSelected()) {
			cbLnaGain.setSelected(lnaGain);
			//setLnaGain(lnaGain);
		}
		boolean mixerGain = ((FCD2TunerController) device).getMixerGain();
		if (mixerGain != cbMixerGain.isSelected()) {
			setMixerGain(mixerGain);
		}
		//rfFilterValue.setText(((FCD2TunerController) device).getRfFilter());
		//ifFilterValue.setText(((FCD2TunerController) device).getIfFilter());		
		checkingSettings = false;
	}
	
	public void getSettings()  throws IOException, DeviceException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  // Allow startup to settle down first
		
		if (Config.saveFcdParams) {
		loadParam(cbLnaGain, "cbLnaGain");
		setLnaGain(cbLnaGain.isSelected());
		loadParam(cbMixerGain, "cbMixerGain");
		setMixerGain(cbMixerGain.isSelected());
		loadParam(cbBiasTee, "cbBiasTee");
		setBiasTee(cbBiasTee.isSelected());
		} else {
			cbLnaGain.setSelected(((FCD2TunerController) device).getLnaGain());
			cbMixerGain.setSelected(((FCD2TunerController) device).getMixerGain());
			cbBiasTee.setSelected(((FCD2TunerController) device).getBiasTee());
		}
		rfFilterValue.setText(((FCD2TunerController) device).getRfFilter());
		ifFilterValue.setText(((FCD2TunerController) device).getIfFilter());
//		int ifG = fcd.getIFGain();
//		ifSpinner.setValue(""+ifG);
	}
	
	
	@Override
	public void run() {
		done = false;
		running = true;
		Thread.currentThread().setName("FcdProPlusPanel");
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
				} catch (DeviceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
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

	private void setMixerGain(boolean b) {
		try {
			if (b == false) {
				((FCD2TunerController) device).setMixerGain(false);
			} else {
				((FCD2TunerController) device).setMixerGain(true);
			}
			//cbMixerGain.setSelected(fcd.getMixerGain());
		} catch (DeviceException e1) {
			Log.println("Error setting Mixer Gain on FCD");
			e1.printStackTrace(Log.getWriter());
		}
		if (Config.saveFcdParams) {
			saveParam(cbMixerGain.isSelected(), "cbMixerGain");
			Config.save();
		}
	}
	
	private void setLnaGain(boolean b) {
		try {
			if (b == false) {
				((FCD2TunerController) device).setLnaGain(false);
			} else {
				((FCD2TunerController) device).setLnaGain(true);
			}
		} catch (DeviceException e1) {
			Log.println("Error setting LNA Gain on FCD");
			e1.printStackTrace(Log.getWriter());
		}
		if (Config.saveFcdParams) {
			saveParam(cbLnaGain.isSelected(), "cbLnaGain");
			Config.save();
		}
	}
	
	private void setBiasTee(boolean b) {
		try {
			if (b == false) {
				((FCD2TunerController) device).setBiasTee(false);
			} else {
				((FCD2TunerController) device).setBiasTee(true);
			}
		} catch (DeviceException e1) {
			Log.println("Error setting Bias Tee on FCD");
			e1.printStackTrace(Log.getWriter());
		}
		if (Config.saveFcdParams) {
			saveParam(cbBiasTee.isSelected(), "cbBiasTee");
			Config.save();
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (checkingSettings) return;
		if (e.getSource() == cbMixerGain) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				setMixerGain(false);
			} else {
				setMixerGain(true);
			}		
		}
		if (e.getSource() == cbLnaGain) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				setLnaGain(false);
			} else {
				setLnaGain(true);
			}
		}
		if (e.getSource() == cbBiasTee ) {

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				setBiasTee(false);
			} else {
				setBiasTee(true);
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		/*
		if (e.getSource() == ifSpinner) {
			int u = Integer.parseInt((String) ifSpinner.getValue());
	        try {
	        	Log.println("Setting IF Gain to: " + u);
	        	// IMPLEMENT			fcd.setIFGain(u);
			} catch (DeviceException e1) {
				Log.println("Error setting IF Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			}
		}
		*/
		
	}

	@Override
	public int getSampleRate() {
		return FCD2TunerController.SAMPLE_RATE;
	}

	@Override
	public int getDecimationRate() {
		return 1;
	}

}
