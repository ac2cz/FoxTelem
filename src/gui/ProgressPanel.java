package gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import common.Config;

public class ProgressPanel extends JDialog implements ActionListener {

	String title;
	
	public ProgressPanel(JFrame owner, String message, boolean modal) {
		super(owner, modal);
		title = message;
		setTitle(message);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		int x = 100;
		int y = 100;
		if (MainWindow.frame != null) {
			x = MainWindow.frame.getX() + MainWindow.frame.getWidth()/2 - (message.length()*9)/2;
			y = MainWindow.frame.getY() + MainWindow.frame.getHeight()/2;
		} else {
			Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
			x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
			y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
		}
		setBounds(100, 100, message.length()*9, 10);
	
		    this.setLocation(x, y);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void updateProgress(int p) {
		if (p == 100) this.dispose();
		setTitle(title + " (" + p + "%)");
	}

}
