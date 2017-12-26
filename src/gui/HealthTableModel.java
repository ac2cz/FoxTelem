package gui;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import common.Log;
import telemetry.BitArrayLayout;

public class HealthTableModel  extends AbstractTableModel {
	String[] columnNames = null;
    private long[][] data = null;
	public static final int RESET_COL = 0;
	public static final int UPTIME_COL = 1;

    HealthTableModel(BitArrayLayout lay) {
		columnNames = new String[lay.fieldName.length+2];
		columnNames[RESET_COL] = "RESET";
		columnNames[UPTIME_COL] = "UPTIME";
		for (int k=0; k<columnNames.length-2; k++) 
			columnNames[k+2] = lay.fieldName[k];
	}
	
    public void setData(long[][] d) { 
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
        return Integer.class; // make sure we sort by int vs string    
        //return getValueAt(0, c).getClass();
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
     
    public void setValueAt(String value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }
 
    */
}
