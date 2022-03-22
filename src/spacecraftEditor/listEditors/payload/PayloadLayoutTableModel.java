package spacecraftEditor.listEditors.payload;

import spacecraftEditor.listEditors.CsvTableModel;

public class PayloadLayoutTableModel extends CsvTableModel {

	public PayloadLayoutTableModel() {
		super();
	}
	
	protected void initColumns() {
		columnNames = new String[12];
		columnNames[0] = "Num";
		columnNames[1] = "Type";
		columnNames[2] = "Field";
		columnNames[3] = "Bits";
		columnNames[4] = "Unit";
		columnNames[5] = "Conversion";
		columnNames[6] = "Module";
		columnNames[7] = "Mod Num";
		columnNames[8] = "Mod Line";
		columnNames[9] = "Line Type";
		columnNames[10] = "Short Name";
		columnNames[11] = "Description";

		columnClass = new Class[] {
		        Integer.class, 
		        String.class, 
		        String.class, 
		        Integer.class, //Bits
		        String.class,  // Unit
		        String.class, // Conversion
		        String.class,  // Module
		        Integer.class, //module num
		        Integer.class,  // module line
		        Integer.class, // line type
		        String.class, // short name
		        String.class
		    };

		columnWidths = new int[columnNames.length];
		columnWidths[0] = 30;
		columnWidths[1] = 30;
		columnWidths[2] = 100;
		columnWidths[3] = 30;
		columnWidths[4] = 30;
		columnWidths[5] = 150;
		columnWidths[6] = 50;
		columnWidths[7] = 30;
		columnWidths[8] = 30;
		columnWidths[9] = 30;
		columnWidths[10] = 50;
		columnWidths[11] = 100;
	}
	
	public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.

		//if (row > 0)
			return true;
		//return false;
	}
}
