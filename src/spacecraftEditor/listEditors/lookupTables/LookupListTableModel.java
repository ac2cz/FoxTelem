package spacecraftEditor.listEditors.lookupTables;

import spacecraftEditor.CsvTableModel;

public class LookupListTableModel extends CsvTableModel{
	private static final long serialVersionUID = 1L;

	public LookupListTableModel() {
		super();
	}
	
	@Override
	protected void initColumns() {
		columnNames = new String[3];
		columnNames[0] = "Num";
		columnNames[1] = "Name";
		columnNames[2] = "FileName";

		columnClass = new Class[] {
				Integer.class, //Num
				String.class, //Name
				String.class
		};

		columnWidths = new int[columnNames.length];
		columnWidths[0] = 20;
		columnWidths[1] = 100;
		columnWidths[2] = 200;		
	}

}
