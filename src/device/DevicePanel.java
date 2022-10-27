package device;

import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.usb.UsbException;

import common.Config;
import common.Log;

@SuppressWarnings("serial")
public abstract class DevicePanel extends JPanel implements Runnable {

	protected device.TunerController device;
	protected boolean running = true;
	protected boolean done = false;
	
	public abstract void setDevice(TunerController fcd) throws IOException, DeviceException, UsbException;
	
	public abstract void updateFilter() throws IOException, DeviceException;
	
	public abstract void setEnabled(boolean b);
	
	public abstract int getSampleRate();
	public abstract int getDecimationRate();
	
	public void stopProcessing() {
		running = false;
	}
	
	@SuppressWarnings("rawtypes")
	protected void saveParam(JComboBox box, String key) {
		int g = box.getSelectedIndex();
		Log.println("SAVED " + key + ": " + g);
		Config.saveGraphIntParam("SDR", 0, 0, device.name, key, g);		
	}
	
	@SuppressWarnings("rawtypes")
	protected void loadParam(JComboBox box, String key) {
        int g = Config.loadGraphIntValue("SDR", 0, 0, device.name, key);
		Log.println("Loaded " + key + ": " + g);
		box.setSelectedIndex(g);
	}

	protected void saveParam(boolean b, String key) {
		Log.println("SAVED " + key + ": " + b);
		Config.saveGraphBooleanParam("SDR", 0, 0, device.name, key, b);		
	}
	
	protected void loadParam(JCheckBox b, String key) {
        boolean g = Config.loadGraphBooleanValue("SDR", 0, 0, device.name, key);
		Log.println("Loaded " + key + ": " + g);
		b.setSelected(g);
	}
	
	protected void saveParam(JSpinner spinner, String key) {
		int g = (Integer) spinner.getValue();
		Log.println("SAVED " + key + ": " + g);
		Config.saveGraphIntParam("SDR", 0, 0, device.name, key, g);		
	}
	
	protected void loadParam(JSpinner spinner, String key) {
        int g = Config.loadGraphIntValue("SDR", 0, 0, device.name, key);
		Log.println("Loaded " + key + ": " + g);
		spinner.setValue(g);
	}

}
