package gui;

import java.io.IOException;

import javax.swing.JPanel;

import fcd.FcdDevice;
import fcd.FcdException;

@SuppressWarnings("serial")
public abstract class FcdPanel extends JPanel {

	public abstract void setFcd(FcdDevice fcd) throws IOException, FcdException;
	
	public abstract void updateFilter() throws IOException, FcdException;
	
	public abstract void setEnabled(boolean b);

}
