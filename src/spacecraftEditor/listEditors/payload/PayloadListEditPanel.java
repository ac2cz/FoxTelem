package spacecraftEditor.listEditors.payload;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.tabs.CanExperimentTab;
import gui.tabs.DisplayModule;
import gui.tabs.FoxTelemTab;
import gui.tabs.HealthTabRt;
import gui.tabs.NamedExperimentTab;
import gui.tabs.WodHealthTab;
import gui.tabs.WodNamedExperimentTab;
import spacecraftEditor.SpacecraftEditPanel;
import spacecraftEditor.SpacecraftEditorWindow;
import spacecraftEditor.listEditors.CsvFileEditorGrid;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;

public class PayloadListEditPanel extends JPanel implements MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;
	public static final String PAYLOAD_TEMPLATE_FILENAME = "templates"+File.separator+"PAYLOAD_template.csv";

	SpacecraftEditPanel parent;
	Spacecraft sat;
	JTabbedPane tabbedPane;
	JTable payloadsTable;
	JPanel rightPanel, rightPanel1;
	PayloadCsvFileEditPanel payloadCsvFileEditPanel;
	
	PayloadListTableModel layoutsTableModel;
	
	JComboBox<String> payloadType;
	JButton btnAddPayload, btnRemovePayload,btnBrowsePayload,btnUpdatePayload, btnGeneratePayload;
	JTextField payloadFilename,payloadName;
	JTextArea codeTextArea;
	FoxTelemTab modulesTab;
	
	public PayloadListEditPanel(Spacecraft sat, SpacecraftEditPanel parent) {
		this.sat = sat;
		this.parent = parent;
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setLayout(new BorderLayout(0, 0));
		
		JPanel rightPanel = addLeftPanel();
		add(rightPanel, BorderLayout.WEST);
		
		tabbedPane = addCenterPanel();
		add(tabbedPane, BorderLayout.CENTER);
		
	}
	
	private void loadPayloadsListTable() {
		String[][] data = new String[sat.numberOfLayouts][4];
		for (int i=0; i< sat.numberOfLayouts; i++) {
			data[i][0] =""+i;
			if (sat.layout[i].name != null) 
				data[i][1] = sat.layout[i].name;
			else
				data[i][1] ="NONE";

			if (i < sat.layoutFilename.length && sat.layoutFilename[i] != null) // we don't store filenames for can layouts, so skip those
				data[i][2] = sat.layoutFilename[i];
			else
				data[i][2] ="-";
			data[i][3] = ""+sat.layout[i].getMaxNumberOfBytes();

		}
		if (sat.numberOfLayouts > 0) 
			layoutsTableModel.setData(data);
		else {
			String[][] fakeRow = {{"","","",""}};
			layoutsTableModel.setData(fakeRow);
		}
	}
	
	private void scrollToRow(JTable table, int row) {
		Rectangle cellRect = table.getCellRect(row, 0, false);
		if (cellRect != null) {
			table.scrollRectToVisible(cellRect);
		}
	}
	
	private JPanel addLeftPanel() {
		
		JPanel centerPanel = new JPanel();

		//centerPanel1.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		
		JPanel centerPanel2 = new JPanel();
		centerPanel.add(centerPanel2);
		centerPanel2.setLayout(new BorderLayout());

		TitledBorder headingLayout = SpacecraftEditPanel.title("Payloads");
		centerPanel2.setBorder(headingLayout);
		
		layoutsTableModel = new PayloadListTableModel();

		payloadsTable = new JTable(layoutsTableModel);
		payloadsTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (payloadsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		payloadsTable.setFillsViewportHeight(true);
		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		centerPanel2.add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
		TableColumn column = payloadsTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(20);
		column = payloadsTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(100);
		column = payloadsTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(200);
		column = payloadsTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(40);

		payloadsTable.addMouseListener(this);
		
		String PREV = "prev";
		String NEXT = "next";
		
		InputMap inMap = payloadsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMap = payloadsTable.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = payloadsTable.getSelectedRow();
				if (row > 0) {
					updateTabs(row-1);
					payloadsTable.setRowSelectionInterval(row-1, row-1);
					scrollToRow(payloadsTable, row-1);
				}
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = payloadsTable.getSelectedRow();
				if (row < payloadsTable.getRowCount()-1) {
					updateTabs(row+1);     
					payloadsTable.setRowSelectionInterval(row+1, row+1);
					scrollToRow(payloadsTable, row+1);
				}
			}
		});

		loadPayloadsListTable();

		JPanel footerPanel = new JPanel();
		centerPanel2.add(footerPanel, BorderLayout.SOUTH);
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS) );
		
		// Row 1
		JPanel footerPanelRow1 = new JPanel();
		footerPanelRow1.setLayout(new BoxLayout(footerPanelRow1, BoxLayout.X_AXIS) );
		footerPanel.add(footerPanelRow1);
		
		JPanel f1 = new JPanel();
		f1.setLayout(new BoxLayout(f1, BoxLayout.Y_AXIS) );
		JLabel lf1 = new JLabel("Name");
		payloadName = new JTextField();
		f1.add(lf1);
		f1.add(payloadName);
		footerPanelRow1.add(f1);
		
		JPanel f2 = new JPanel();
		f2.setLayout(new BoxLayout(f2, BoxLayout.Y_AXIS) );
		JLabel lf2 = new JLabel("Type");
		payloadType = new JComboBox<String>(BitArrayLayout.types); 
		f2.add(lf2);
		f2.add(payloadType);
		footerPanelRow1.add(f2);
		
		JPanel f3 = new JPanel();
		f3.setLayout(new BoxLayout(f3, BoxLayout.Y_AXIS) );
		JLabel lf3 = new JLabel("Filename");
		payloadFilename = new JTextField();
		f3.add(lf3);
		f3.add(payloadFilename);
		payloadFilename.setEditable(false);
		payloadFilename.addMouseListener(this);
		footerPanelRow1.add(f3);
		
		// Row 2
		JPanel footerPanelRow2 = new JPanel();
		footerPanel.add(footerPanelRow2);
		
		btnAddPayload = new JButton("Add");
		btnAddPayload.addActionListener(this);
		btnBrowsePayload = new JButton("Browse");
		btnBrowsePayload.addActionListener(this);
		btnRemovePayload = new JButton("Remove");
		btnRemovePayload.addActionListener(this);
		btnUpdatePayload = new JButton("Update");
		btnUpdatePayload.addActionListener(this);
		
		btnGeneratePayload = new JButton("Generate C");
		btnGeneratePayload.addActionListener(this);
		
		footerPanelRow2.add(btnAddPayload);
		footerPanelRow2.add(btnUpdatePayload);
		footerPanelRow2.add(btnRemovePayload);
		//footerPanelRow2.add(btnBrowsePayload);
		footerPanelRow2.add(btnGeneratePayload);
		btnAddPayload.setEnabled(true);
		if (sat.numberOfLayouts == 0)
			btnRemovePayload.setEnabled(false);
		footerPanel.add(new Box.Filler(new Dimension(400,10), new Dimension(400,400), new Dimension(400,500)));

		//centerPanel2.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		return centerPanel;
	}
	
	private JTabbedPane addCenterPanel() {
		// RIGHT Side we put the center panel
		tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabbedPane, BorderLayout.CENTER);
		
		PayloadLayoutTableModel payloadTableModel = new PayloadLayoutTableModel();
		
		payloadCsvFileEditPanel = new PayloadCsvFileEditPanel(sat, payloadTableModel, "Payload",null);
			
		codeTextArea = new JTextArea();
		JScrollPane scpane = new JScrollPane(codeTextArea); //scrollpane  and add textarea to scrollpane
		codeTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		codeTextArea.setLineWrap(true);
		codeTextArea.setWrapStyleWord(true);
		codeTextArea.setEditable(true);
		codeTextArea.setVisible(true);
		
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Payload" + "</b></body></html>", payloadCsvFileEditPanel );
		
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Code" + "</b></body></html>", scpane );
		
		tab.setLayout(new BorderLayout());
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Modules" + "</b></body></html>", tab );
		updateTabs(0);
		
		return tabbedPane;
	}
	
	JPanel tab = new JPanel();
	private void updateTabs(int row) {
		if (sat.layoutFilename.length == 0) return;
		
//		if (payloadCsvFileEditPanel.csvFileEditorGrid != null) {
//			int n = Log.optionYNdialog("Unsaved file",
//					"Do you want to save this payload file before switching to another?\n"+payloadFilename.getText() + "\nOtherwise your changes will be lost\n");
//			if (n == JOptionPane.NO_OPTION) {
//				
//			} else {
//				return;
//			}	
//		}
		
		payloadName.setText(sat.layout[row].name);
		payloadFilename.setText(sat.layoutFilename[row]);
		payloadType.setSelectedItem((String)sat.layout[row].typeStr);
		
		if (modulesTab != null)
			tab.remove(modulesTab);
		
		payloadCsvFileEditPanel.setFile(sat.layoutFilename[row]);
		BitArrayLayout lay = sat.layout[row];
		BitArrayLayout secondaryLayout = sat.getSecondaryLayoutFromPrimaryName(lay.name);
		String title = "Experiment: " + lay.name;
		if (lay.title != null)
			title = lay.title;
		if (lay.isRealTime() || lay.isMAX() || lay.isMIN())
			try {
				modulesTab = new HealthTabRt(sat);
			} catch (LayoutLoadException e) {
				Log.errorDialog("ERROR loading health tab", ""+e);
			}
		if (lay.isWOD())
			try {
				modulesTab = new WodHealthTab(sat);
			} catch (LayoutLoadException e) {
				Log.errorDialog("ERROR loading WOD tab", ""+e);
			}
		if (lay.isExperiment()) {
			
			modulesTab = new NamedExperimentTab(sat, title, 
					lay,
					secondaryLayout, DisplayModule.DISPLAY_EXPERIMENT);
		}
		if (lay.isWODExperiment()) {
			modulesTab = new WodNamedExperimentTab(sat, title, 
					lay,
					secondaryLayout, DisplayModule.DISPLAY_WOD_EXPERIMENT);
		}
		if (lay.isCanExperiment()) {
			BitArrayLayout canPktLayout =  Config.satManager.getLayoutByName(sat.foxId, Spacecraft.CAN_PKT_LAYOUT);
			modulesTab = new CanExperimentTab(sat, title, 
					lay, canPktLayout, DisplayModule.DISPLAY_EXPERIMENT);
		}
		if (lay.isCanWodExperiment()) {
			BitArrayLayout canPktLayout =  Config.satManager.getLayoutByName(sat.foxId, Spacecraft.WOD_CAN_PKT_LAYOUT);
			modulesTab = new CanExperimentTab(sat, title, 
					lay, canPktLayout, DisplayModule.DISPLAY_EXPERIMENT);
		}
		if (modulesTab != null) {
			tab.add(modulesTab, BorderLayout.CENTER);
			tabbedPane.repaint();
		}
		
	}
	
	private void addPayload() {
		String[] newLayoutFilenames = new String[sat.numberOfLayouts+1];
		newLayoutFilenames[sat.numberOfLayouts] = payloadFilename.getText();
		
		for (int i=0; i < sat.numberOfLayouts; i++) {
			newLayoutFilenames[i] = sat.layoutFilename[i];
		}
		sat.layoutFilename = newLayoutFilenames;
		
		
		try {
			File dest = new File(Config.currentDir+"/spacecraft"+ File.separator + payloadFilename.getText());
			if (!dest.isFile()) {
				File source = new File(Config.currentDir+ File.separator +"spacecraft"+File.separator+"templates"+ File.separator + PAYLOAD_TEMPLATE_FILENAME);
				SatPayloadStore.copyFile(source, dest);
			}
			
			BitArrayLayout[] newLayouts = new BitArrayLayout[sat.numberOfLayouts+1];
			newLayouts[sat.numberOfLayouts] = new BitArrayLayout(payloadFilename.getText());
			newLayouts[sat.numberOfLayouts].name = payloadName.getText();
			newLayouts[sat.numberOfLayouts].typeStr = (String)payloadType.getSelectedItem();
			for (int i=0; i < sat.numberOfLayouts; i++) {
				newLayouts[i] = sat.layout[i];
			}
			sat.layout = newLayouts;
			
			sat.numberOfLayouts++;
			parent.save();
			loadPayloadsListTable();
			payloadName.setText("");
			payloadFilename.setText("");
			payloadType.setSelectedIndex(0);
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "You need to specify a valid payload file\n"+e1);
		} catch (LayoutLoadException e1) {
			Log.errorDialog("ERROR", "Could not parse the payload template\n"+e1);
		}
	}
	
	private void browsePayload() {
		Log.println("Browse for Payload ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify payload file", "Select", "csv");
		if (file == null) return;
		payloadFilename.setText(file.getName());
	}
	
	private void generatePayload() {
		Log.println("Generate Payload ...");
		File layout = new File(Config.currentDir+"/spacecraft"+ File.separator + payloadFilename.getText());
		
		if (!layout.isFile()) {
			Log.errorDialog("ERROR", "Select a row with a valid payload file\n");
			return;
		}
		
		String PYTHON = "python.exe";
		String SCRIPT = Config.currentDir + File.separator + "gen_header.py";
		String COMMAND = PYTHON + " " + SCRIPT + " " + payloadName.getText() + " " + layout;
		Log.println(" running: " + COMMAND);
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(COMMAND);
			BufferedReader stdInput = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new 
					InputStreamReader(p.getErrorStream()));

			// read the output from the command
			codeTextArea.setText(null); // clear the text area
			while ((s = stdInput.readLine()) != null) {
				codeTextArea.append(s + "\n");
			}

			// read any errors from the attempted command
			Log.println("Here is the standard error of the code generation (if any):\n");
			while ((s = stdError.readLine()) != null) {
				codeTextArea.append(s + "\n");
				Log.println(s);
			}
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error running python generate script\n");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == btnAddPayload) {
			Log.println("Adding Payload ...");
			addPayload();
			
		}
		if (e.getSource() == btnBrowsePayload) {
			browsePayload();
		}
		
		if (e.getSource() == btnUpdatePayload) {
			int row = payloadsTable.getSelectedRow();
			Log.println("Updating row " + row);
			if (sat.numberOfLayouts == 0) return;
			
			try {
				sat.layout[row] = new BitArrayLayout(payloadFilename.getText());
				sat.layoutFilename[row] = payloadFilename.getText();
				sat.layout[row].name = payloadName.getText();
				sat.layout[row].typeStr = (String)payloadType.getSelectedItem();
				parent.save();
				loadPayloadsListTable();
				payloadName.setText("");
				payloadFilename.setText("");
				payloadType.setSelectedIndex(0);
			} catch (FileNotFoundException e1) {
				Log.errorDialog("ERROR", "Could not initilize the payload file\n"+e1);
			} catch (LayoutLoadException e1) {
				Log.errorDialog("ERROR", "Could not load the payload file\n"+e1);
			}

		}

		if (e.getSource() == btnRemovePayload) {
			int row = payloadsTable.getSelectedRow();
			Log.println("Removing row " + row);
			if (sat.numberOfLayouts == 0) return;
			
			int n = Log.optionYNdialog("Remove payload file too?",
					"Remove this payload file as well as the payload table row?\n"+payloadFilename.getText() + "\n\nThis will be gone forever\n");
			if (n == JOptionPane.NO_OPTION) {
				
			} else {
				File file = new File(Config.currentDir+"/spacecraft" +File.separator + payloadFilename.getText());
				try {
					SatPayloadStore.remove(file.getAbsolutePath());
				} catch (IOException ef) {
					Log.errorDialog("ERROR removing File", "\nCould not remove the payload file\n"+ef.getMessage());
				}
			}
			
			if (sat.numberOfLayouts == 1) {
				sat.numberOfLayouts = 0;
				sat.layout = null;
				sat.layoutFilename = null;

			} else {
				int j = 0;
				BitArrayLayout[] newLayouts = new BitArrayLayout[sat.numberOfLayouts-1];
				for (int i=0; i < sat.numberOfLayouts; i++) {
					if (i != row)
						newLayouts[j++] = sat.layout[i];
				}
				sat.layout = newLayouts;

				j = 0;
				String[] newLayoutFilenames = new String[sat.numberOfLayouts-1];
				for (int i=0; i < sat.numberOfLayouts; i++) {
					if (i != row)
						newLayoutFilenames[j++] = sat.layoutFilename[i];
				}
				sat.layoutFilename = newLayoutFilenames;
				sat.numberOfLayouts--;
			}
			parent.save();
			loadPayloadsListTable();
			payloadName.setText("");
			payloadFilename.setText("");
			payloadType.setSelectedIndex(0);

			
		}
		if (e.getSource() == btnGeneratePayload) {
			generatePayload();
		}

	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == payloadFilename) {
			browsePayload();
		}
		if (e.getSource() == payloadsTable) {
			if (sat.numberOfLayouts == 0) return;
			int row = payloadsTable.rowAtPoint(e.getPoint());
			int col = payloadsTable.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				Log.println("PRESSED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());
				String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
				
				
				
//				try {
//					Log.println("Loading: " + masterFolder + File.separator + sat.layoutFilename[row]);
//					BitArrayLayout layout = new BitArrayLayout(sat.layoutFilename[row]);
//					String[][] data = layout.getJTableData();
//					layoutEditorPanel.setData(sat.layout[row].name + " : " + sat.layoutFilename[row], data);
//				} catch (FileNotFoundException e2) {
//					Log.errorDialog("ERROR", "Could not open layout file: \n"+masterFolder + File.separator + sat.layoutFilename[row] + "\n" + e2);
//				} catch (LayoutLoadException e2) {
//					Log.errorDialog("ERROR", "Could not load layout file: \n"+masterFolder + File.separator + sat.layoutFilename[row] + "\n" + e2);
//				}
				
				updateTabs(row);
				
				if (e.getClickCount() == 2) {
					
					//EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.layoutFilename[row]);
					File file = new File(masterFolder + File.separator + sat.layoutFilename[row]);
					try {
						Desktop.getDesktop().open(file);
					} catch (IOException e1) {
						Log.errorDialog("ERROR", "Could not open payload file\n"+e1);
					}
					
					//editor.setVisible(true);
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
