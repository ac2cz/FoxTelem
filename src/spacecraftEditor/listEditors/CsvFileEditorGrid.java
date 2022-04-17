package spacecraftEditor.listEditors;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import common.Log;

public class CsvFileEditorGrid extends JPanel implements MouseListener, TableModelListener  {

	private static final long serialVersionUID = 1L;
	CsvTableModel tableModel;
	public JTable table;
	TableRowSorter<CsvTableModel> sorter;
	CsvFileEditPanel parent;
	JTextField filterFields[];
	JTextField filterConversion;
	
	public CsvFileEditorGrid(CsvTableModel model, CsvFileEditPanel parent) {
		setLayout(new BorderLayout());
		tableModel = model;
		this.parent = parent;
		
		table = new JTable(model);
		table.setAutoCreateRowSorter(false);  // sorting does not help here because it does not change the save order
		JScrollPane scrollPane = new JScrollPane (table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		table.setFillsViewportHeight(true);
		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
		for (int i=0; i < tableModel.columnWidths.length; i++) {
		TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(tableModel.columnWidths[i]);
		}
		
		table.addMouseListener(this);
		table.getModel().addTableModelListener(this);
		
		JPanel filterPanel = new JPanel();
		//filterPanel.setLayout(new BorderLayout());
		add(filterPanel, BorderLayout.SOUTH);
		
		filterPanel.add(new JLabel("Filter: "));
		if (tableModel.filterColumns != null) {
			filterFields = new JTextField[tableModel.filterColumns.length];
			for (int i=0; i< tableModel.filterColumns.length; i++) {
				filterFields[i] = addFilterField(filterPanel, tableModel.columnNames[tableModel.filterColumns[i]], tableModel.filterColumns[i], 6);
			}

		
		sorter = new TableRowSorter<CsvTableModel>(tableModel);
		table.setRowSorter(sorter);
		
		}
		
		String PASTE = "paste";
		
		InputMap inMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inMap.put(KeyStroke.getKeyStroke("control V"), PASTE);
		ActionMap actMap = table.getActionMap();

		actMap.put(PASTE, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("PREV");
				int row = table.getSelectedRow();

				if (row > 0) {

					table.setRowSelectionInterval(row, row);
					//System.err.println("PASTE");
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Clipboard clipboard = toolkit.getSystemClipboard();
					String result;
					try {
						result = (String) clipboard.getData(DataFlavor.stringFlavor);
						System.out.println("String from Clipboard:" + result);
						// now put it in the cells
						String[] pasteRows = result.split("\n");
						if (pasteRows.length == 0) {
							pasteRows = new String[1];
							pasteRows[0] = result;
						}
						if (pasteRows.length + row > tableModel.getRowCount()) {
							Log.errorDialog("ERROR", "Can't fit "+pasteRows.length+" rows into the table. Perhaps add more empty rows first.");
							return;
						}
						for(int r=0; r < pasteRows.length; r++) {
							String[] cellValues = pasteRows[r].split("\t");
							if (cellValues.length % tableModel.getColumnCount() != 0) {
								Log.infoDialog("Sorry!", "Can only paste whole rows at the moment");
								return;
							}
							
							for (int i=0; i< tableModel.getColumnCount(); i++) {
								tableModel.setValueAt(cellValues[i], row, i);
							}
							row++; // incase we have more rows
						}
					} catch (UnsupportedFlavorException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
			}
		});
		
//		table.getModel().addTableModelListener(new TableModelListener() {
//
//		      public void tableChanged(TableModelEvent e) {
//		         System.out.println("Updated Row: " + e.getFirstRow() +" Col: "+ e.getColumn());
//		         updated = true;
//		      }
//		    });
	}
	
	private JTextField addFilterField(JPanel filterPanel, String name, int col, int width) {
		JTextField filterField;
		JLabel lblFilter = new JLabel(name + ":");
		filterPanel.add(lblFilter, BorderLayout.WEST); 
		filterField = new JTextField();
		filterPanel.add(filterField, BorderLayout.CENTER);
		filterField.setColumns(width);
		
		// Filter if the text changes	
		filterField.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        newFilter(filterField,col);
                    }
                    public void insertUpdate(DocumentEvent e) {
                        newFilter(filterField,col);
                    }
                    public void removeUpdate(DocumentEvent e) {
                        newFilter(filterField,col);
                    }
                });
		return filterField;
	}
	
	private void newFilter(JTextField field, int col) {
	    RowFilter<CsvTableModel, Object> rf = null;
	    //If current expression doesn't parse, don't update.
	    try {
	        rf = RowFilter.regexFilter(field.getText(),col);
	    } catch (java.util.regex.PatternSyntaxException e) {
	        return;
	    }
	    sorter.setRowFilter(rf);
	}
	
	public void setData(String titleString, String[][] d) {
		TitledBorder title = new TitledBorder(null, titleString, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		setBorder(title);
		tableModel.setData(d);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
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

	@Override
	public void tableChanged(TableModelEvent e) {
		if (e.getColumn() == -1) {
			//System.err.println("NO UPDATE Row: " + e.getFirstRow() +" Col: "+ e.getColumn());
			return;
		}
		if (e.getFirstRow() == -1) {
			//System.err.println("NO UPDATE Row: " + e.getFirstRow() +" Col: "+ e.getColumn());
			return;
		}
		//System.err.println("Updated Row: " + e.getFirstRow() +" Col: "+ e.getColumn());
		try {
			parent.save();
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "Could not save the CSV file\n" + e1);
		}

	}
}
