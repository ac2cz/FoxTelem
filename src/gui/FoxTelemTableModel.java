package gui;
import javax.swing.table.AbstractTableModel;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
@SuppressWarnings("serial")
abstract class FoxTelemTableModel extends AbstractTableModel {
	String[] columnNames = null;
	private long[][] keyData = null;
    private String[][] data = null;
    int keys = 2;

	
    public void setData(long[][] kd, String[][] d) { 
    	keyData = kd;
    	keys = keyData[0].length;
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
    	if (col < keys)
    		if (keyData != null && keyData.length > 0)
    			return keyData[row][col];
    		else 
    			return null;
    	else 		
    		if (data != null && data.length > 0)
    			if (keyData != null)
    				return data[row][col-keys];
    			else
    				return data[row][col];
    		else 
    			return null;
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