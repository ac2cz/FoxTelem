package gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import common.Config;
import common.Log;
import fcd.FcdDevice;
import fcd.FcdException;

public class FcdPanel extends JPanel implements ActionListener, Runnable {
	JLabel title;
	JLabel[] param;
	int NUM_OF_PARAMS = 15;
	boolean running = true;
	boolean done = false;
	FcdDevice fcd;
	JButton refresh;
	
	FcdPanel() {
		TitledBorder title = new TitledBorder(null, "Funcube Dongle", TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		this.setBorder(title);
		initializeGui();
	}
	
	public void initializeGui() {
		setLayout(new BorderLayout(3,3));
		JPanel center = new JPanel();
		add(center, BorderLayout.CENTER);
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		JPanel left = new JPanel();
		JPanel right = new JPanel();
		center.add(left);
		center.add(right);
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		param = new JLabel[NUM_OF_PARAMS];
		for (int i=0; i < NUM_OF_PARAMS/2; i++) {
			param[i] = new JLabel();
			left.add(param[i]);
		}
		for (int i=NUM_OF_PARAMS/2; i < NUM_OF_PARAMS; i++) {
			param[i] = new JLabel();
			right.add(param[i]);
		}
		
		JPanel south = new JPanel();
		refresh = new JButton("Refresh");
		add(south, BorderLayout.SOUTH);
		south.add(refresh);
		refresh.addActionListener(this);
	}
	
	public void setFcd(FcdDevice f) throws IOException, FcdException { 
		fcd = f; 
		getSettings();
	}
	
	public void getSettings() throws IOException, FcdException {
		updateParam(0, "RF FILTER", fcd.getRfFilter());
		updateParam(1, "MIXER GAIN", fcd.getMixerGain());
		updateParam(2, "LNA  GAIN", fcd.getParam(FcdDevice.APP_GET_LNA_GAIN));
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
	
	private void updateParam(int i, String name, int cmd) {
		param[i].setText(name + ": " + cmd + "     ");
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
		if (e.getSource() == this.refresh) {
			try {
				getSettings();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (FcdException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

}
