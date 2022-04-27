package spacecraftEditor.listEditors.payload;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import common.Spacecraft;
import spacecraftEditor.listEditors.CsvFileEditPanel;
import spacecraftEditor.listEditors.CsvFileEditorGrid;
import telemetry.LayoutLoadException;

public class PayloadCsvFileEditPanel extends CsvFileEditPanel {

	private static final long serialVersionUID = 1L;
	public static final int FIELD_COL = 2;
	public static final int BITS_COL = 3;
	public static final int CONVERSION_COL = 5;
	public static final int LINE_TYPE_COL = 9;
	public static final int MODULE_LINE_COL = 8;
	
	PayloadLayoutTableModel model;
	JLabel lblNumberOfBits;
	
	public PayloadCsvFileEditPanel(Spacecraft sat, PayloadLayoutTableModel model,
			String titleString, String file) {
		super(sat, model, titleString, file, "csv");
		this.model = model;
		
		for (int i=0; i< csvFileEditorGrid.table.getColumnModel().getColumnCount(); i++) {
			TableColumn column = csvFileEditorGrid.table.getColumnModel().getColumn(i);
			column.setCellRenderer(new PayloadTableCellRenderer());
		}
		
		model.addTableModelListener(new TableModelListener() {

		      public void tableChanged(TableModelEvent e) {
		         //System.out.println("Updated Row: " + e.getFirstRow() +" Col: "+ e.getColumn());
		         int rows = model.getRowCount();
		         model.setValueAt(rows-1, 0, 0);
		      }
		    });
		//setConversionComboBox();
		//setLineTypeComboBox();
		JPanel bitsPanel = new JPanel();
		footerFilePanel.add(bitsPanel, BorderLayout.WEST);
		bitsPanel.add(new JLabel("Total Bits:"));
		lblNumberOfBits = new JLabel("0");
		bitsPanel.add(lblNumberOfBits);
		
	}
	
	protected void load() throws FileNotFoundException, LayoutLoadException {
		super.load();
		updateNumberOfBits();
	}

	public void setConversionComboBox() {
		String[] conv = sat.getConversionsArray();
		JComboBox<String> comboBoxConversion = new JComboBox<String>(conv);
		TableColumn moduleColumn = csvFileEditorGrid.table.getColumnModel().getColumn(CONVERSION_COL);
		moduleColumn.setCellEditor(new DefaultCellEditor(comboBoxConversion));
	}
	
//	public void setLineTypeComboBox() {
//		String[] lineTypes = sat.dd();
//		JComboBox<String> comboBoxLineTypes = new JComboBox<String>(lineTypes);
//		TableColumn moduleColumn = csvFileEditorGrid.table.getColumnModel().getColumn(LINE_TYPE_COL);
//		moduleColumn.setCellEditor(new DefaultCellEditor(comboBoxLineTypes));
//	}
	
	private void updateNumberOfBits() {
		int num=0;
		for (int i=0; i < this.model.getRowCount(); i++) {
			String value = (String) model.getValueAt(i, BITS_COL);
			try {
				int b = Integer.parseInt(value);
				num += b;
			} catch (NumberFormatException e) { ;}
			
		}
		double n = num/8.0;
		DecimalFormat fmt = new DecimalFormat ("0.##");
		lblNumberOfBits.setText(""+num + "   Bytes: " + fmt.format(n));

	}
	
	@Override
	protected void updateSpacecraft() {
		updateNumberOfBits();
		//setConversionComboBox();
		// We don't need to save the spacecraft as data is only changed in the layout file
	}

	/**
	 * Color the rows in the directory so that we know when we have data
	 * @author chris.e.thompson g0kla
	 *
	 */
	public class PayloadTableCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		// This is a overridden function which gets executed for each action to the dir table
		public Component getTableCellRendererComponent (JTable table, 
				Object obj, boolean isSelected, boolean hasFocus, int row, int column) {

			Component cell = super.getTableCellRendererComponent(
					table, obj, isSelected, hasFocus, row, column);
			
			if (row >0 && column == CONVERSION_COL) {
				String value = (String) model.getValueAt(row, column);
				if (value != null) {
					String[] conversions = value.split("\\|");
					for (String conv : conversions) {
						if (!sat.isValidConversion(conv)) {
							cell.setForeground(Color.red);
							return cell;
						}
					}
				}
				cell.setForeground(Color.black);	
			}
			
			if (row >0 && column == FIELD_COL) {
				// It is an error if there is a duplicate 
				boolean noDupes = true;
				String value = (String) model.getValueAt(row, column);
				if (value != null)
					for(int i=0; i < model.getRowCount(); i++) {
						if (i != row && value.equalsIgnoreCase((String) model.getValueAt(i, column))) {
							cell.setForeground(Color.red);
							return cell;
						}
					}
				// It is an error if it is called a reserved word
				if (value != null)
					
					if (value.equalsIgnoreCase("uptime") 
							|| value.equalsIgnoreCase("captureDate") 
							|| value.equalsIgnoreCase("id") 
							|| value.equalsIgnoreCase("resets")
							|| value.equalsIgnoreCase("type") 
						) {
						cell.setForeground(Color.red);
						return cell;
					}
					
				cell.setForeground(Color.black);	
			}
			if (row >0 && column == MODULE_LINE_COL) {
				// It is an error if this line number is a duplicate or if there is a gap between this and the previous
				
				
			}
			return cell;
		}
	} 
	
}
