package spacecraftEditor;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import common.Config;
import common.Log;
import common.Spacecraft;

public class EditorFrame extends JFrame implements ActionListener, WindowListener {
	private static final long serialVersionUID = 1L;
	Spacecraft spacecraft;
	boolean editable;
	
	private JTextArea ta;
	private JMenuBar menuBar;
	private JMenu fileM,editM;
	private JScrollPane scpane;
	private JMenuItem cancelI,cutI,copyI,pasteI,selectI,saveI, loadI,statusI;
	private String pad;
	private JToolBar toolBar;
	private String filename;
	private boolean buildingGui = true;
	
	JPanel textPane;
	JPanel centerpane;
	
	public static final String EDIT_WINDOW_X = "edit_window_x";
	public static final String EDIT_WINDOW_Y = "edit_window_y";
	public static final String EDIT_WINDOW_WIDTH = "edit_window_width";
	public static final String EDIT_WINDOW_HEIGHT = "edit_window_height";

	/**
	 * Call to reply to a message
	 * @param toCallsign
	 * @param fromCallsign
	 * @param title
	 * @param keywords
	 * @param origText
	 */
	public EditorFrame(Spacecraft spacecraft, String filename) {
		super("Message Editor");
		this.spacecraft = spacecraft;
		this.filename = filename;
		editable = true;
		
		makeFrame(editable);
		addTextArea();
		
		if (filename != null) {
			this.setTitle(filename);
			try {
				byte[] encoded = Files.readAllBytes(Paths.get(filename));
				String text = new String(encoded, StandardCharsets.US_ASCII);
				ta.setText(text);
			} catch (IOException e) {
				Log.errorDialog("ERROR", "Could not open the file: " + filename);
			}
		}
		
	}

	private void makeFrame(boolean edit) {

		addWindowListener(this);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/pacsat.jpg")));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		loadProperties();
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());

		pad = " ";

		menuBar = new JMenuBar(); //menubar
//		menuBar.setFont(MainWindow.sysFont);
		fileM = new JMenu("File"); //file menu
//		fileM.setFont(MainWindow.sysFont);
		editM = new JMenu("Edit"); //edit menu
//		editM.setFont(MainWindow.sysFont);
		//		viewM = new JMenu("View"); //edit menu

		cancelI = new JMenuItem("Exit");
//		cancelI.setFont(MainWindow.sysFont);
		cutI = new JMenuItem("Cut");
//		cutI.setFont(MainWindow.sysFont);
		copyI = new JMenuItem("Copy");
//		copyI.setFont(MainWindow.sysFont);
		pasteI = new JMenuItem("Paste");
//		pasteI.setFont(MainWindow.sysFont);
		selectI = new JMenuItem("Select All"); //menuitems
//		selectI.setFont(MainWindow.sysFont);
		saveI = new JMenuItem("Save"); //menuitems
//		exportI.setFont(MainWindow.sysFont);
//		saveAndExitI = new JMenuItem("Send"); //menuitems
//		saveAndExitI.setFont(MainWindow.sysFont);

		statusI = new JMenuItem("Status"); //menuitems
//		statusI.setFont(MainWindow.sysFont);
		toolBar = new JToolBar();

		setJMenuBar(menuBar);
		menuBar.add(fileM);
		menuBar.add(editM);
		//		menuBar.add(viewM);

		fileM.add(saveI);
		fileM.add(cancelI);

		editM.add(cutI);
		editM.add(copyI);
		editM.add(pasteI);        
		editM.add(selectI);

		//		viewM.add(statusI);

		cutI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		copyI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		pasteI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		selectI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));

		// on ESC key close frame
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel"); //$NON-NLS-1$
		getRootPane().getActionMap().put("Cancel", new AbstractAction(){ //$NON-NLS-1$
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});

		centerpane = new JPanel();
		pane.add(centerpane,BorderLayout.CENTER);
		centerpane.setLayout(new BorderLayout());
		
		textPane = new JPanel();
		textPane.setLayout(new BorderLayout());
		
		centerpane.add(textPane, BorderLayout.CENTER);
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		pane.add(topPanel,BorderLayout.NORTH);

		// Button bar
		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(buttonBar, BorderLayout.NORTH);

//		butReply = new JButton("Save");
//		butReply.setMargin(new Insets(0,0,0,0));
//		butReply.addActionListener(this);
//		butReply.setToolTipText("Save File");
//		butReply.setFont(MainWindow.sysFont);
//		if (editable) butReply.setEnabled(false);
//		buttonBar.add(butReply);

		pane.add(toolBar,BorderLayout.SOUTH);

		saveI.addActionListener(this);
		cancelI.addActionListener(this);
		cutI.addActionListener(this);
		copyI.addActionListener(this);
		pasteI.addActionListener(this);
		selectI.addActionListener(this);
		statusI.addActionListener(this);

		
		setVisible(true);
	}
	
	private void addTextArea() {
		ta = new JTextArea(); //textarea
		scpane = new JScrollPane(ta); //scrollpane  and add textarea to scrollpane
		ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setEditable(editable);
		ta.setVisible(true);
//		ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Config.getInt(Config.FONT_SIZE)));  // default to same as the system size
//		editPane.add(scpane, TEXT_CARD);
		textPane.add(scpane, BorderLayout.CENTER);
	}

	public void saveProperties() {
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorFrame", "windowHeight", this.getHeight());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorFrame", "windowWidth", this.getWidth());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorFrame", "windowX", this.getX());
		Config.saveGraphIntParam("Global", 0, 0, "spacecraftEditorFrame",  "windowY", this.getY());
	}
	
	public void loadProperties() {
		int windowX = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorFrame", "windowX");
		int windowY = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorFrame", "windowY");
		int windowWidth = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorFrame", "windowWidth");
		int windowHeight = Config.loadGraphIntValue("Global", 0, 0, "spacecraftEditorFrame", "windowHeight");
		if (windowX == 0 || windowY == 0 ||windowWidth == 0 ||windowHeight == 0) {
			setBounds(100, 100, 600, 700);
		} else {
			setBounds(windowX, windowY, windowWidth, windowHeight);
		}
	}
	

	@Override
	public void windowClosed(WindowEvent arg0) {
		saveProperties();
	}
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == saveI) {
			try {
				saveFile(filename);
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not save the file");
			}			
		}
		// save and exit
		if (e.getSource() == cancelI )
			dispose();
		else if (e.getSource() == cutI) {
			pad = ta.getSelectedText();
			ta.replaceRange("", ta.getSelectionStart(), ta.getSelectionEnd());
		}
		else if (e.getSource() == copyI)
			pad = ta.getSelectedText();
		else if (e.getSource() == pasteI)
			ta.insert(pad, ta.getCaretPosition());
		else if (e.getSource() == selectI)
			ta.selectAll();
		else if (e.getSource() == statusI) {
		}
		
	}
	
	private void saveFile(String filename) throws FileNotFoundException {
		try (PrintWriter out = new PrintWriter(filename)) {
		    out.print(ta.getText());
		}
	}

}
