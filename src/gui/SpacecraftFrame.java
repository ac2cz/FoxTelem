package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import javax.swing.JLabel;

import common.Log;
import common.Spacecraft;
import common.Config;

/**
* 
* FOX 1 Telemetry Decoder
* @author chris.e.thompson g0kla/ac2cz
*
* Copyright (C) 2015 amsat.org
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
* The SpacecraftFrame is a seperate window that opens and allows the spacecraft paramaters to
* be viewed and edited.
*
*/
@SuppressWarnings("serial")
public class SpacecraftFrame extends JDialog implements ItemListener, ActionListener, FocusListener, WindowListener {

	private SpacecraftPanel contentPanel;
	
	JButton btnCancel;
	JButton btnSave;
	
	Spacecraft sat;
	
	/**
	 * Create the dialog.
	 */
	public SpacecraftFrame(Spacecraft sat, JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Spacecraft paramaters");
		this.sat = sat;
		addWindowListener(this);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		getContentPane().setLayout(new BorderLayout());
		contentPanel = new SpacecraftPanel(sat);
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		addButtons();
		
	}
	
	
	private void addButtons() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		btnSave = new JButton("Save");
		btnSave.setActionCommand("Save");
		buttonPane.add(btnSave);
		btnSave.addActionListener(this);
		getRootPane().setDefaultButton(btnSave);


		btnCancel = new JButton("Cancel");
		btnCancel.setActionCommand("Cancel");
		buttonPane.add(btnCancel);
		btnCancel.addActionListener(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		boolean refreshTabs = false;
		boolean rebuildMenu = false;
		
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == btnSave) {
			boolean dispose = contentPanel.save();
				if (dispose) {
					sat.save();
					Config.initSatelliteManager();
					if (rebuildMenu) {
						Config.mainWindow.initSatMenu();
						MainWindow.frame.repaint();
					}
					this.dispose();
					if (refreshTabs)
						MainWindow.refreshTabs(false);
				}
		}

	}
	
	public void saveProperties() {
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow", "windowHeight", this.getHeight());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow", "windowWidth", this.getWidth());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow", "windowX", this.getX());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftWindow",  "windowY", this.getY());
	}
	
	public void loadProperties() {
		int windowX = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowX");
		int windowY = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowY");
		int windowWidth = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowWidth");
		int windowHeight = Config.loadGraphIntValue("Global", 0, 0, "spacecraftWindow", "windowHeight");
		if (windowX == 0 || windowY == 0 ||windowWidth == 0 ||windowHeight == 0) {
			setBounds(100, 100, 600, 700);
		} else {
			setBounds(windowX, windowY, windowWidth, windowHeight);
		}
	}


	@Override
	public void itemStateChanged(ItemEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
