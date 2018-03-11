package device;

import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import common.Config;
import common.Log;

@SuppressWarnings("serial")
public abstract class DevicePanel extends JPanel {

	protected device.TunerController device;
	
	public abstract void setDevice(TunerController fcd) throws IOException, DeviceException;
	
	public abstract void updateFilter() throws IOException, DeviceException;
	
	public abstract void setEnabled(boolean b);
	
	public abstract int getSampleRate();
	public abstract int getDecimationRate();
	
	protected void saveParam(JComboBox box, String key) {
		int g = box.getSelectedIndex();
		Log.println("SAVED " + key + ": " + g);
		Config.saveGraphIntParam("SDR", 0, 0, device.name, key, g);		
	}
	
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

}
