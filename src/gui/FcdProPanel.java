package gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.Config;
import common.Log;
import fcd.FcdDevice;
import fcd.FcdException;
import fcd.FcdProDevice;

@SuppressWarnings("serial")
public class FcdProPanel extends FcdPanel implements ItemListener, ActionListener, Runnable {
	JLabel title;
	int NUM_OF_PARAMS = 15;
	boolean running = true;
	boolean done = false;
	FcdProDevice fcd;
	JCheckBox cbMixerGain;
	JCheckBox cbLnaGain;
	JTextField rfFilterValue;
	JTextField ifFilterValue;
	
	FcdProPanel() throws IOException, FcdException {
		TitledBorder title = new TitledBorder(null, "Funcube Dongle Pro", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		//title.setTitleFont(new Font("SansSerif", Font.PLAIN, 12));
		this.setBorder(title);
		initializeGui();
	}
	
	public void initializeGui() throws IOException, FcdException {
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		add(center, BorderLayout.NORTH);
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		cbMixerGain = new JCheckBox("Mixer Gain");
		center.add(cbMixerGain);
		cbMixerGain.addItemListener(this);
		cbLnaGain = new JCheckBox("LNA Gain    ");
		center.add(cbLnaGain);
		cbLnaGain.addItemListener(this);
		
		JLabel rfFilter = new JLabel("    RF Filter");
		center.add(rfFilter);
		rfFilterValue = new JTextField();
		rfFilterValue.setEnabled(false);
		center.add(rfFilterValue);
		
		JLabel ifFilter = new JLabel("    IF Filter");
		center.add(ifFilter);
		ifFilterValue = new JTextField();
		ifFilterValue.setEnabled(false);
		center.add(ifFilterValue);
		
	}
	
	@Override
	public void setFcd(FcdDevice fcd) throws IOException, FcdException {
		setFcd((FcdProDevice)fcd);
		
	}
	public void setFcd(FcdProDevice f) throws IOException, FcdException { 
		fcd = f; 
		getSettings();
	}
	
	public void updateFilter() throws IOException, FcdException {
		//rfFilterValue.setText(fcd.getRfFilter());
	}
	
	public void getSettings()  throws IOException, FcdException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  // Allow startup to settle down first
//		cbMixerGain.setSelected(fcd.getMixerGain());
//		cbLnaGain.setSelected(fcd.getLnaGain());	
//		rfFilterValue.setText(fcd.getRfFilter());
//		ifFilterValue.setText(fcd.getIfFilter());
	}
	
	/*
	public void getSettingsOLD() throws IOException, FcdException {
		
		updateParam(3, "LNA  ENHANCE", fcd.getParam(FcdDevice.APP_GET_LNA_ENHANCE));
		updateParam(4, "BAND", fcd.getParam(FcdDevice.APP_GET_BAND));
		updateParam(5, "MIXER FILTER", fcd.getParam(FcdDevice.APP_GET_MIXER_FILTER));
		updateParam(6, "GAIN MODE", fcd.getParam(FcdDevice.APP_GET_GAIN_MODE));
		updateParam(7, "RC FILTER", fcd.getParam(FcdDevice.APP_GET_RC_FILTER));
		updateParam(8, "MIXER GAIN1", fcd.getParam(FcdDevice.APP_GET_GAIN1));
		updateParam(9, "MIXER GAIN2", fcd.getParam(FcdDevice.APP_GET_GAIN2));
		updateParam(10, "MIXER GAIN3", fcd.getParam(FcdDevice.APP_GET_GAIN3));
		updateParam(11, "MIXER GAIN4", fcd.getParam(FcdDevice.APP_GET_GAIN4));
		updateParam(12, "MIXER GAIN5", fcd.getParam(FcdDevice.APP_GET_GAIN5));
		updateParam(13, "MIXER GAIN6", fcd.getParam(FcdDevice.APP_GET_GAIN6));
		updateParam(14, "IF FILTER", fcd.getParam(FcdDevice.APP_GET_IF_FILTER));
	}
	*/
	private void updateParam(int i, String name, int cmd) {
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
				} catch (FcdException e) {
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
		/*
		if (e.getSource() == cbMixerGain) {
			try {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
//					fcd.setMixerGain(false);
				} else {
//					fcd.setMixerGain(true);
				}
//				cbMixerGain.setSelected(fcd.getMixerGain());
			} catch (FcdException e1) {
				Log.println("Error setting Mixer Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			} catch (IOException e1) {
				Log.println("Error reading Mixer Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			}
		}
		if (e.getSource() == cbLnaGain) {
			try {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
//					fcd.setLnaGain(false);
				} else {
//					fcd.setLnaGain(true);
				}
//				cbLnaGain.setSelected(fcd.getLnaGain());
			} catch (FcdException e1) {
				Log.println("Error setting LNA Gain on FCD");
				e1.printStackTrace(Log.getWriter());
			} catch (IOException e1) {
				Log.println("Error reading LNA Gain on FCD");
				e1.printStackTrace(Log.getWriter());
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		*/
	}

	

}
