import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SpacecraftEditorWindow extends JFrame implements WindowListener {

	private static final long serialVersionUID = 1L;
	public SpacecraftEditorWindow() {
		initialize();
		JPanel mainPanel = new JPanel();
		getContentPane().add(mainPanel);
		layoutMainPanel(mainPanel);
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setBounds(100, 100, 1000, 600);
//		setBounds(Config.windowX, Config.windowY, Config.windowWidth, Config.windowHeight);
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle("AMSAT Spacecraft Config Editor");
		addWindowListener(this);
		//addWindowStateListener(this);

	}
	
	private void layoutMainPanel(JPanel panel) {
		panel.setLayout(new BorderLayout());
		
	}
	
	
	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowClosed(WindowEvent arg0) {
		// This is once dispose has run
		
	}
	@Override
	public void windowClosing(WindowEvent arg0) {
		// close has been requested from the X or otherwise
		this.dispose();
		
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
