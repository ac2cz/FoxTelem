package spacecraftEditor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import common.Config;
import common.Log;
import common.Spacecraft;
import gui.MainWindow;
import gui.ProgressPanel;
import telemetry.LayoutLoadException;

public abstract class CsvFileEditPanel extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;
	public CsvFileEditorGrid csvFileEditorGrid;
	protected CsvTableModel tableModel;
	JPanel centerPanel;
	JPanel footerPanel;
	ArrayList<String[]> dataLines; // An array list for each line. A line is an array of strings
	protected String filename;
	protected Spacecraft sat;

	JButton btnLoad,btnAddAbove, btnAddBelow, btnRemove, btnSave, btnUp, btnDown;
	JTextField csvFilename;
	
	public CsvFileEditPanel(Spacecraft sat, CsvTableModel model, String titleString, String file) {
		this.sat = sat;
		tableModel = model;
		filename = file;
		
		setLayout(new BorderLayout());
		
		setTitle(titleString);
		csvFileEditorGrid = new CsvFileEditorGrid(model, this);
		add(csvFileEditorGrid);
		
		footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
		add(footerPanel, BorderLayout.SOUTH);
		JPanel footerPanel1 = new JPanel();
		JPanel footerPanel2 = new JPanel();
		footerPanel.add(footerPanel1);
		footerPanel.add(footerPanel2);
		
		JLabel lf3 = new JLabel("Filename");
		csvFilename = new JTextField(filename);
		footerPanel1.add(lf3);
		footerPanel1.add(csvFilename);
		csvFilename.setEditable(false);
		csvFilename.setColumns(20);
		csvFilename.addMouseListener(this);
		
		btnLoad = new JButton("Load");
		btnUp = new JButton("Up");
		btnDown = new JButton("Down");
		btnAddAbove = new JButton("Add Above");
		btnAddBelow = new JButton("Add Below");
		btnRemove = new JButton("Remove");
		btnSave = new JButton("Save");
		footerPanel2.add(btnLoad);
		footerPanel2.add(btnUp);
		footerPanel2.add(btnDown);
		footerPanel2.add(btnAddAbove);
		footerPanel2.add(btnAddBelow);
		footerPanel2.add(btnRemove);
		footerPanel2.add(btnSave);
		btnLoad.addActionListener(this);
		btnUp.addActionListener(this);
		btnDown.addActionListener(this);
		btnAddAbove.addActionListener(this);
		btnAddBelow.addActionListener(this);
		btnRemove.addActionListener(this);
		btnSave.addActionListener(this);
		
		setFile(file);
	}
	
	protected abstract void updateSpacecraft(); // call back that allows the implementor to save settings back to the spacecraft file
	
	private void setTitle(String titleString) {
		TitledBorder title = new TitledBorder(null, titleString, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		setBorder(title);
	}
	
	public void setFile(String file) {
		if (file !=null) {
			ProgressPanel initProgress = null;
			initProgress = new ProgressPanel(MainWindow.frame, "Loading file " +filename+", please wait ...", false);
			initProgress.setVisible(true);
			this.filename = file;
			this.csvFilename.setText(file);
			setTitle(" : " + file);
			try {
				load();
			} catch (FileNotFoundException e) {
				Log.errorDialog("ERROR", "Could not load CSV file:\n" + file + "\n" + e);
			} catch (LayoutLoadException e) {
				Log.errorDialog("ERROR", "Could not parse CSV file\n" + file + "\n" + e);
			}
			initProgress.updateProgress(100);
		}
	}

	private void setData() {
		String[][] data = new String[dataLines.size()][tableModel.getColumnCount()];  // row column
		for (int j = 0; j < dataLines.size(); j++) {
			data[j] = dataLines.get(j);
		}

		tableModel.setData(data);
	}
	
	private void load() throws FileNotFoundException, LayoutLoadException {
		String line;
		String fileName = "spacecraft" +File.separator + filename;
	//	File aFile = new File(fileName);
		
		Log.println("Loading CSV File: "+ fileName);
		@SuppressWarnings("resource")
		BufferedReader dis = new BufferedReader(new FileReader(fileName));

		dataLines = new ArrayList<String[]>(); // get rid of any existing data
		try {
			while ((line = dis.readLine()) != null) {
				if (line != null) {
					String[] dataToAdd = new String[tableModel.getColumnCount()];
					String[] lineOfdata = line.split(",");
					//					if (lineOfdata.length <  tableModel.getColumnCount()) // if it is greater then we assume commas in the comments
					//						throw new LayoutLoadException("Could not load file.  Column count is: "+lineOfdata.length+" expected " + tableModel.getColumnCount() + "\n" + filename);
					for (int d=0; d < lineOfdata.length; d++) {
						if (d < tableModel.getColumnCount())
							dataToAdd[d] = lineOfdata[d];
						else
							dataToAdd[tableModel.getColumnCount()-1] += " " + lineOfdata[d]; // append any extra items to the description
					}
					dataLines.add(dataToAdd);
				}
			}
			setData();
		} catch (NumberFormatException e) {
			Log.errorDialog("ERROR", "Error processing the CSV file\n" + e);
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Can not load the CSV file\n" + e);
		}
		finally {
			try {
				dis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void save() throws IOException {
		String fileName = "spacecraft" +File.separator + filename;
		BufferedWriter dis = new BufferedWriter(new FileWriter(fileName,false)); // overwrite the existing file
		try {
			for (int j = 0; j < dataLines.size(); j++) {
				String line = "";
				for (int i=0; i < dataLines.get(j).length-1; i++) {
					if (dataLines.get(j)[i] == null)
						line = line + "0,";
					else
						line = line + dataLines.get(j)[i] + ",";
				}
				if (dataLines.get(j)[dataLines.get(j).length-1] == null)
					line = line + " \n";
				else
					line = line + dataLines.get(j)[dataLines.get(j).length-1] + "\n";
				dis.write(line);
			} 
		} finally {
			dis.close();
		}
		updateSpacecraft();
	}
	
	private void addRows(boolean above, int row, int num) {
		if (row == -1)
			return;
		if (above) {
			for (int i=0; i < num; i++) {
				String[] empty_row = new String[tableModel.getColumnCount()];
				dataLines.add(row, empty_row);
			}
		} else {
			for (int i=0; i < num; i++) {
				String[] empty_row = new String[tableModel.getColumnCount()];
				dataLines.add(row+num, empty_row);
			}
		}
		setData();
		// now select the same row in case we want to do the same again
		csvFileEditorGrid.table.setRowSelectionInterval(row, row);
	}

	private void browseFile() {
		Log.println("Browse for File ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify file", "Select", "csv");
		if (file == null) return;
		csvFilename.setText(file.getName());
		filename = file.getName();
	}
	
	private void removeRow(int row) {
		Log.println("Removing row " + row);
		if (dataLines.size() == 0) return;
		dataLines.remove(row);
		setData();
		if (dataLines.size() == 0) return; 
		if (row >= dataLines.size())
			csvFileEditorGrid.table.setRowSelectionInterval(row-1, row-1);
		else
			csvFileEditorGrid.table.setRowSelectionInterval(row, row);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnAddAbove) {
			Log.println("Adding Rows Above ...");
			if (dataLines.size() == 0) return;
			int row = csvFileEditorGrid.table.getSelectedRow();
			addRows(true, row, csvFileEditorGrid.table.getSelectedRowCount());
		}
		if (e.getSource() == btnAddBelow) {
			Log.println("Adding Rows Below ...");
			if (dataLines.size() == 0) return;
			int row = csvFileEditorGrid.table.getSelectedRow();
			addRows(false, row, csvFileEditorGrid.table.getSelectedRowCount());
		}
		
		if (e.getSource() == btnSave) {
			try {
				save();
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not save table to :" +filename + "\n" + e1);
			}
		}
		if (e.getSource() == btnLoad) {
			try {
				load();
			} catch (IOException e1) {
				Log.errorDialog("ERROR", "Could not load table from :" +filename + "\n" + e1);
			} catch (LayoutLoadException e1) {
				Log.errorDialog("ERROR", "Could not parse table from :" +filename + "\n" + e1);
			}
		}
		if (e.getSource() == btnRemove) {
			int i=0;
			for (int row : csvFileEditorGrid.table.getSelectedRows())
				removeRow(row-i++);
		}
		
		if (e.getSource() == btnUp) {
			int row = csvFileEditorGrid.table.getSelectedRow();
			Log.println("Moving row up:" + row);
			if (dataLines.size() == 0) return;
			if (row < 1) return;
			String[] line = dataLines.get(row);
			dataLines.remove(row);
			dataLines.add(row-1, line);
			setData();
			csvFileEditorGrid.table.setRowSelectionInterval(row-1, row-1);
		}
		if (e.getSource() == btnDown) {
			int row = csvFileEditorGrid.table.getSelectedRow();
			Log.println("Moving row down:" + row);
			if (dataLines.size() == 0) return;
			if (row >= dataLines.size()-1) return;
			String[] line = dataLines.get(row);
			dataLines.remove(row);
			dataLines.add(row+1, line);
			setData();
			csvFileEditorGrid.table.setRowSelectionInterval(row+1, row+1);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == csvFilename) {
			browseFile();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
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
