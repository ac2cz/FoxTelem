package gui;

import javax.swing.table.AbstractTableModel;

public class SoftError84488TableModel extends AbstractTableModel {
	@SuppressWarnings("serial")
	String[] columnNames = null;
	private String[][] data = null;

	SoftError84488TableModel() {
		columnNames = new String[7];
		columnNames[0] = "Resets";
		columnNames[1] = "Uptime";
		columnNames[2] = "DAC Overflows";
		columnNames[3] = "I2C Retries";	
		columnNames[4] = "I2C Retries";	
		columnNames[5] = "SPI Retreies";
		columnNames[6] = "MRAM CRCs";

	}

	public void setData(String[][] d) { 
		data = d;
		fireTableDataChanged();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		if (data != null)
			return data.length;
		else 
			return 0;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * Don't need to implement this method unless your table's
	 * editable.
	 */
	public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.

		return false;

	}

	/*
	 * Don't need to implement this method unless your table's
	 * data can change.
	 */
	public void setValueAt(String value, int row, int col) {
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}


}
