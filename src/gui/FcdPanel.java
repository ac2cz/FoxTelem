package gui;

import java.io.IOException;

import javax.swing.JPanel;

import fcd.Device;
import fcd.DeviceException;

@SuppressWarnings("serial")
public abstract class FcdPanel extends JPanel {

	public abstract void setFcd(Device fcd) throws IOException, DeviceException;
	
	public abstract void updateFilter() throws IOException, DeviceException;
	
	public abstract void setEnabled(boolean b);

}
