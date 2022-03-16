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

public class PayloadCsvFileEditPanel extends CsvFileEditPanel {

	private static final long serialVersionUID = 1L;
	public static final int CONVERSION_COL = 5;
	
	PayloadCsvFileEditorGrid payloadFileEditorGrid;
	PayloadLayoutTableModel model;

	public PayloadCsvFileEditPanel(Spacecraft sat, PayloadLayoutTableModel model, PayloadCsvFileEditorGrid payloadFileEditorGrid,
			String titleString, String file) {
		super(sat, model, payloadFileEditorGrid, titleString, file);
		this.model = model;
		
		for (int i=0; i< csvFileEditorGrid.table.getColumnModel().getColumnCount(); i++) {
			TableColumn column = csvFileEditorGrid.table.getColumnModel().getColumn(i);
			column.setCellRenderer(new PayloadTableCellRenderer());
		}
		
		//setConversionComboBox();
		checkValues();
	}
	
	public void checkValues() {
		// Check the conversions


	}

	public void setConversionComboBox() {
		String[] conv = sat.getConversionsArray();
		JComboBox<String> comboBoxConversion = new JComboBox<String>(conv);
		TableColumn moduleColumn = csvFileEditorGrid.table.getColumnModel().getColumn(CONVERSION_COL);
		moduleColumn.setCellEditor(new DefaultCellEditor(comboBoxConversion));
	}
	
	@Override
	protected void updateSpacecraft() {
		checkValues();
		
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
			return cell;
		}
	} 
	
}
