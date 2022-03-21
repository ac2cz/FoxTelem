package spacecraftEditor.listEditors.payload;

import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import common.Spacecraft;
import spacecraftEditor.CsvFileEditPanel;
import spacecraftEditor.CsvFileEditorGrid;

public class PayloadCsvFileEditPanel extends CsvFileEditPanel {

	private static final long serialVersionUID = 1L;
	public static final int CONVERSION_COL = 5;
	public static final int LINE_TYPE_COL = 9;
	public static final int MODULE_LINE_COL = 8;
	
	PayloadLayoutTableModel model;

	public PayloadCsvFileEditPanel(Spacecraft sat, PayloadLayoutTableModel model,
			String titleString, String file) {
		super(sat, model, titleString, file);
		this.model = model;
		
		for (int i=0; i< csvFileEditorGrid.table.getColumnModel().getColumnCount(); i++) {
			TableColumn column = csvFileEditorGrid.table.getColumnModel().getColumn(i);
			column.setCellRenderer(new PayloadTableCellRenderer());
		}
		
		//setConversionComboBox();
		//setLineTypeComboBox();
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
	
	@Override
	protected void updateSpacecraft() {
		
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
				String[] conversions = value.split("\\|");
				for (String conv : conversions) {
					if (!sat.isValidConversion(conv)) {
						cell.setForeground(Color.red);
						return cell;
					}
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
