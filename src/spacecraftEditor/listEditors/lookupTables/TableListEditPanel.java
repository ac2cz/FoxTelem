package spacecraftEditor.listEditors.lookupTables;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import common.Config;
import common.Log;
import common.Spacecraft;
import spacecraftEditor.CsvFileEditorGrid;
import spacecraftEditor.CsvTableModel;
import spacecraftEditor.SpacecraftEditPanel;
import spacecraftEditor.SpacecraftEditorWindow;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;
import telemetry.conversion.ConversionLookUpTable;

public abstract class TableListEditPanel extends JPanel implements MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;
	public String TEMPLATE_FILENAME = "LOOKUP_template.csv";

	SpacecraftEditPanel parent;
	protected Spacecraft sat;
	String title;
	
	JTable table;
	JPanel rightPanel;
	
	protected CsvTableModel listTableModel;
	CsvTableModel csvTableModel;
	LookupCsvFileEditPanel lookupCsvFileEditPanel;
	
	/* Internally we store the data as an array of String arrays */
	protected ArrayList<String[]> dataLines;
	
	JButton btnAdd, btnRemove,btnBrowse,btnUpdate;
	JTextField txtFilename,txtName;
	
	
	public TableListEditPanel(Spacecraft sat, String title, CsvTableModel listTableModel, CsvTableModel csvTableModel, SpacecraftEditPanel parent) {
		this.sat = sat;
		this.title = title;
		this.listTableModel = listTableModel;
		this.csvTableModel = csvTableModel;
		this.parent = parent;
		
		dataLines = new ArrayList<String[]>();
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setLayout(new BorderLayout(0, 0));
		
		JPanel rightPanel = addLeftPanel();
		add(rightPanel, BorderLayout.WEST);
		
		JPanel centerPanel = addCenterPanel();
		add(centerPanel, BorderLayout.CENTER);
		
	}
	
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
		JLabel lf3 = new JLabel("Filename");
		txtFilename = new JTextField();
		f3.add(lf3);
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
	
	private JPanel addCenterPanel() {
		// RIGHT Column

		CsvFileEditorGrid csvFileEditorGrid = new CsvFileEditorGrid(csvTableModel);
		lookupCsvFileEditPanel = new LookupCsvFileEditPanel(sat, csvTableModel, csvFileEditorGrid, "Lookup Tables",null);

		return lookupCsvFileEditPanel;
	}
	
	/**
	 * Add the list item to the internal structure, then save and reload
	 */
	private void addListItem() {
		String[] dataToAdd = new String[listTableModel.getColumnCount()];

		try {
			dataToAdd[0] =""+dataLines.size();
			if (txtName.getText() != "") 
				dataToAdd[1] = txtName.getText();
			else
				dataToAdd[1] ="NONE";
			if (this.txtFilename.getText() != "") 
				dataToAdd[2] = txtFilename.getText();
			else
				dataToAdd[2] ="NONE";

			dataLines.add(dataToAdd);

			File dest = new File(Config.currentDir+"/spacecraft"+ File.separator + txtFilename.getText());
			if (!dest.isFile()) {
				File source = new File(Config.currentDir+ File.separator +"spacecraft"+File.separator+"templates"+ File.separator + TEMPLATE_FILENAME);
				SatPayloadStore.copyFile(source, dest);
			}
			
			save(); // put this into the spacecraft layout
			load(); // use that to repopulate the tabe
			txtName.setText("");
			txtFilename.setText("");
			table.setRowSelectionInterval(dataLines.size()-1, dataLines.size()-1);
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Error with file\n"+e1);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Error loading table\n"+e);
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
				"Remove the file as well as the table row?\n"+txtFilename.getText() + "\n\nThis will be gone forever\n");
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
			save();
			load();
			if (dataLines.size() > 0) 
				if (row > 0)
					table.setRowSelectionInterval(row-1,row-1);
				else
					table.setRowSelectionInterval(0,0);
			txtName.setText("");
			txtFilename.setText("");
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Error with file\n"+e);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Error loading table\n"+e);
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
		if (txtName.getText() != "") 
			dataToAdd[1] = txtName.getText();
		else
			dataToAdd[1] ="NONE";
		if (this.txtFilename.getText() != "") 
			dataToAdd[2] = txtFilename.getText();
		else
			dataToAdd[2] ="NONE";
		dataLines.set(row, dataToAdd);
		try {
			save();
			load();
			table.setRowSelectionInterval(row,row);
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Error with file\n"+e);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Error loading table\n"+e);
		}
	}

	private void browseListItem() {
		Log.println("Browse for list item filename ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify file", "Select", "csv");
		if (file == null) return;
		txtFilename.setText(file.getName());
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
				
				txtName.setText(dataLines.get(row)[1]);
				txtFilename.setText(dataLines.get(row)[2]);
				
				lookupCsvFileEditPanel.setFile(dataLines.get(row)[2]);
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
