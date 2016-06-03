package fcd;

import java.io.IOException;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public abstract class DevicePanel extends JPanel {

	public abstract void setDevice(Device fcd) throws IOException, DeviceException;
	
	public abstract void updateFilter() throws IOException, DeviceException;
	
	public abstract void setEnabled(boolean b);

}
