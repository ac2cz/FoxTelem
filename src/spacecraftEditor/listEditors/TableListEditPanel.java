package spacecraftEditor.listEditors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import common.Config;
import common.Log;
import common.Spacecraft;
import spacecraftEditor.SpacecraftEditPanel;
import spacecraftEditor.SpacecraftEditorWindow;
import spacecraftEditor.listEditors.lookupTables.LookupCsvFileEditPanel;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;

public abstract class TableListEditPanel extends JPanel implements MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;
	public String TEMPLATE_FILENAME = "LOOKUP_template.csv";

	protected String FILE_EXT = "csv";
	
	protected SpacecraftEditPanel parent;
	protected Spacecraft sat;
	String title;
	
	protected JTable table;
	protected JPanel leftPanel;
	protected JLabel lFilename;
	
	protected ListTableModel listTableModel;
	CsvTableModel csvTableModel;
	LookupCsvFileEditPanel lookupCsvFileEditPanel;
	
	/* Internally we store the data as an array of String arrays.  This is not impacted by the way the data is stored in the
	 * MASTER file */
	protected ArrayList<String[]> dataLines;
	
	JButton btnAdd, btnRemove,btnBrowse,btnUpdate;
	protected JTextField txtFilename;
	protected JTextField txtName;
	
	
	public TableListEditPanel(Spacecraft sat, String title, ListTableModel listTableModel, 
			CsvTableModel csvTableModel, String file_ext, SpacecraftEditPanel parent) {
		this.sat = sat;
		this.title = title;
		this.listTableModel = listTableModel;
		this.csvTableModel = csvTableModel;
		this.parent = parent;
		this.FILE_EXT = file_ext;
		
		dataLines = new ArrayList<String[]>();
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setLayout(new BorderLayout(0, 0));
		
		leftPanel = addLeftPanel();
		add(leftPanel, BorderLayout.WEST);
		
		JPanel centerPanel = addCenterPanel();
		add(centerPanel, BorderLayout.CENTER);
		
		if (table.getRowCount() > 0) {
			table.setRowSelectionInterval(0,0);
			if (lookupCsvFileEditPanel != null && dataLines != null && dataLines.size() > 0)
				lookupCsvFileEditPanel.setFile(dataLines.get(0)[2]);
		}
		
	}
	
	/**
	 * Implemented these methods to copy the dataLines to/from the actual storage structure
	 * - loadTable() copies data from the MASTER file to dataLines
	 * - saveTable() copise data from dataLines to the MASTER file fields.
	 */
	abstract protected void loadTable();
	abstract protected void saveTable() throws IOException, LayoutLoadException;
	
	/**
	 * Populate the JTable data array from the internal format
	 */
	private void setData() {
		if (dataLines.size() == 0) {
			String[] dataToAdd = new String[listTableModel.getColumnCount()];
			String[][] fakeRow = {dataToAdd};
			listTableModel.setData(fakeRow);
		} else {
			String[][] data = new String[dataLines.size()][listTableModel.getColumnCount()];  // row column
			for (int j = 0; j < dataLines.size(); j++) {
				data[j] = dataLines.get(j);
			}
			listTableModel.setData(data);
		}
	}
	
	private void load() {
		loadTable();
		setData();
	}
	private void save() throws IOException, LayoutLoadException {
		saveTable();
		parent.save();
	}
	
	private void scrollToRow(JTable table, int row) {
		Rectangle cellRect = table.getCellRect(row, 0, false);
		if (cellRect != null) {
			table.scrollRectToVisible(cellRect);
		}
	}

	private JPanel addLeftPanel() {
		// CENTER Column - Things the user can change - e.g. Layout Files, Freq, Tracking etc
		
		JPanel leftPanel = new JPanel();

		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		JPanel leftPanel1 = new JPanel();
		leftPanel.add(leftPanel1);
		leftPanel1.setLayout(new BorderLayout());
		
		TitledBorder headingFrames = SpacecraftEditPanel.title(title);
		leftPanel1.setBorder(headingFrames);
		

		table = new JTable(listTableModel);
		table.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		table.setFillsViewportHeight(true);
		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		leftPanel1.add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
		for (int i=0; i < listTableModel.columnWidths.length; i++) {
		TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(listTableModel.columnWidths[i]);
		}

		String PREV = "prev";
		String NEXT = "next";
		
		InputMap inMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("UP"), PREV);
		inMap.put(KeyStroke.getKeyStroke("DOWN"), NEXT);
		ActionMap actMap = table.getActionMap();

		actMap.put(PREV, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = table.getSelectedRow();
				if (row > 0) {
					updateRow(row-1);
					table.setRowSelectionInterval(row-1, row-1);
					scrollToRow(table, row-1);
				}
			}
		});
		actMap.put(NEXT, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				//    System.out.println("NEXT");
				int row = table.getSelectedRow();
				if (row < table.getRowCount()-1) {
					updateRow(row+1);
					table.setRowSelectionInterval(row+1, row+1);
					scrollToRow(table, row+1);
				}
			}
		});
		
		table.addMouseListener(this);

	//	leftPanel.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		
		load();

		JPanel footerPanel = new JPanel();
		leftPanel.add(footerPanel, BorderLayout.SOUTH);
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS) );
		
		// Row 1
		JPanel footerPanelRow1 = new JPanel();
		footerPanelRow1.setLayout(new BoxLayout(footerPanelRow1, BoxLayout.X_AXIS) );
		footerPanel.add(footerPanelRow1);
		
		JPanel f1 = new JPanel();
		f1.setLayout(new BoxLayout(f1, BoxLayout.Y_AXIS) );
		JLabel lf1 = new JLabel("Name");
		txtName = new JTextField();
		f1.add(lf1);
		f1.add(txtName);
		footerPanelRow1.add(f1);
		
//		JPanel f2 = new JPanel();
//		f2.setLayout(new BoxLayout(f2, BoxLayout.Y_AXIS) );
//		JLabel lf2 = new JLabel("Type");
//		payloadType = new JComboBox<String>(BitArrayLayout.types); 
//		f2.add(lf2);
//		f2.add(payloadType);
//		footerPanelRow1.add(f2);
		
		JPanel f3 = new JPanel();
		f3.setLayout(new BoxLayout(f3, BoxLayout.Y_AXIS) );
		lFilename = new JLabel("Filename");
		txtFilename = new JTextField();
		f3.add(lFilename);
		f3.add(txtFilename);
		txtFilename.setEditable(false);
		txtFilename.addMouseListener(this);
		footerPanelRow1.add(f3);
		
		// Row 2
		JPanel footerPanelRow2 = new JPanel();
		footerPanel.add(footerPanelRow2);
		
		btnAdd = new JButton("Add");
		btnAdd.addActionListener(this);
		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		btnRemove = new JButton("Remove");
		btnRemove.addActionListener(this);
		btnUpdate = new JButton("Update");
		btnUpdate.addActionListener(this);
		
		footerPanelRow2.add(btnAdd);
		footerPanelRow2.add(btnUpdate);
		footerPanelRow2.add(btnRemove);
		//footerPanelRow2.add(btnBrowsePayload);
		btnAdd.setEnabled(true);

		btnRemove.setEnabled(true);

		leftPanel.add(new Box.Filler(new Dimension(500,10), new Dimension(500,400), new Dimension(500,500)));
		return leftPanel;
	}
	
	protected JPanel addCenterPanel() {
		// RIGHT Column

		lookupCsvFileEditPanel = new LookupCsvFileEditPanel(sat, csvTableModel, "Lookup Tables",null);

		return lookupCsvFileEditPanel;
	}
	
	/**
	 * Add the list item to the internal structure, then save and reload
	 */
	private void addListItem() {
		String[] dataToAdd = new String[listTableModel.getColumnCount()];
		boolean fileSet = false;
		try {
			File dest = new File(Config.currentDir+"/spacecraft"+ File.separator + txtFilename.getText());
			if (!dest.isFile()) {
				File source = new File(System.getProperty("user.dir") + File.separator + Spacecraft.SPACECRAFT_DIR 
						+ File.separator + "templates" + File.separator + TEMPLATE_FILENAME);
				SatPayloadStore.copyFile(source, dest);
			}
			
			dataToAdd[0] =""+dataLines.size();
			if (!txtName.getText().equalsIgnoreCase("")) 
				dataToAdd[1] = txtName.getText();
			else
				dataToAdd[1] ="NONE";
			if (!this.txtFilename.getText().equalsIgnoreCase("")) {
				dataToAdd[2] = txtFilename.getText();
				if (lookupCsvFileEditPanel != null)
					fileSet = lookupCsvFileEditPanel.setFile( txtFilename.getText());
			} else
				dataToAdd[2] ="NONE";

			if (lookupCsvFileEditPanel == null || fileSet) {
				dataLines.add(dataToAdd);
				try {
					save(); // put this into the spacecraft layout
				} catch (IOException e1) {
					Log.errorDialog("ERROR", "Error saving details to file\n"+e1);
				} catch (LayoutLoadException e) {
					Log.errorDialog("ERROR", "Error saving table\n"+e);
				}
				load(); // use that to repopulate the table

//				txtName.setText("");
//				txtFilename.setText("");
				table.setRowSelectionInterval(dataLines.size()-1, dataLines.size()-1);
				updateRow(dataLines.size()-1);
			}
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error with file\n"+e1);
		}
		
	}
	
	/*
	 * Remove an item from the internal structure, then save and reload
	 */
	private void removeListItem(int row) {
		if (row == -1) {
			Log.infoDialog("No row selected", "Select a row to remove");
			return;
		}
		if (dataLines.size() == 0) return;
		int n = Log.optionYNdialog("Remove file on disk too?",
				"Remove the file as well as the table row?\n"+txtFilename.getText() + "\n\n"
						+ "If this file is used by other rows or spacecraft then click No.  Otherwise this file will be gone forever.\n");
		if (n == JOptionPane.NO_OPTION) {
			
		} else {
			File file = new File(Config.currentDir+"/spacecraft" +File.separator + txtFilename.getText());
			try {
				SatPayloadStore.remove(file.getAbsolutePath());
			} catch (IOException ef) {
				Log.errorDialog("ERROR removing File", "\nCould not remove the file\n"+ef.getMessage());
			}
		}
		dataLines.remove(row);
		
		try {
			save(); // put this into the spacecraft layout
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error saving details to file\n"+e1);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Error saving table\n"+e);
		}
		load();
		if (dataLines.size() > 0) 
			try {
			if (row > 0) {
				table.setRowSelectionInterval(row-1,row-1);
				txtName.setText(dataLines.get(row-1)[1]);
				txtFilename.setText(dataLines.get(row-1)[2]);
				updateRow(row-1);
			} else {
				table.setRowSelectionInterval(row,row);
				txtName.setText(dataLines.get(row)[1]);
				txtFilename.setText(dataLines.get(row)[2]);
				updateRow(row);
			}
			} catch (java.lang.IllegalArgumentException e) {
				// likely we removed a row and tried to select one that does not exist
				// Ignore
			}
		
	}
	
	/*
	 * Update the internal structure then save and reload into table
	 */
	private void updateListItem(int row) {
		if (row == -1) {
			Log.infoDialog("No row selected", "Select a row to update");
			return;
		}
		if (dataLines.size() == 0) return;
		String[] dataToAdd = new String[listTableModel.getColumnCount()];

		dataToAdd[0] =""+row;
		if (!txtName.getText().equalsIgnoreCase("")) 
			dataToAdd[1] = txtName.getText();
		else
			dataToAdd[1] ="NONE";
		if (! this.txtFilename.getText().equalsIgnoreCase("")) 
			dataToAdd[2] = txtFilename.getText();
		else {
			dataToAdd[2] ="NONE";
			Log.infoDialog("ERROR", "Select a valid file");
			return;
		}
		dataLines.set(row, dataToAdd);
		try {
			save(); // put this into the spacecraft layout
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error saving details to file\n"+e1);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Error saving table\n"+e);
		}
		load();
		table.setRowSelectionInterval(row,row);
		if (lookupCsvFileEditPanel != null)
			lookupCsvFileEditPanel.setFile(dataLines.get(row)[2]);
		updateRow(row);
	}

	protected void browseListItem() {
		Log.println("Browse for list item filename ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify file", "Select", FILE_EXT);
		if (file == null) return;
		txtFilename.setText(file.getName());
	}
	
	protected void updateRow(int row) {
		txtName.setText(dataLines.get(row)[1]);
		txtFilename.setText(dataLines.get(row)[2]);
		
		lookupCsvFileEditPanel.setFile(dataLines.get(row)[2]);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnAdd) {
			Log.println("Adding List item ...");
			addListItem();
		}
		if (e.getSource() == btnUpdate) {
			int row = table.getSelectedRow();
			Log.println("Updating row " + row);
			updateListItem(row);
		}
		if (e.getSource() == btnRemove) {
			int row = table.getSelectedRow();
			Log.println("Removing row " + row);
			removeListItem(row);
		}
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// Mouse Pressed is more reliable and has the same effect
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == txtFilename) {
			browseListItem();
		}
		
		if (e.getSource() == table) {
			if (dataLines.size() == 0) return;
			int row = table.rowAtPoint(e.getPoint());
			int col = table.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				Log.println("PRESSED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());
				//String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
				
				updateRow(row);
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
