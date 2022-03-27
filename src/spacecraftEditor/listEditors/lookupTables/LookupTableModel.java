package spacecraftEditor.listEditors.lookupTables;

import spacecraftEditor.listEditors.CsvTableModel;

public class LookupTableModel extends CsvTableModel{
	private static final long serialVersionUID = 1L;

	public LookupTableModel() {
		super();
	}
	
	@Override
	protected void initColumns() {
		columnNames = new String[2];
		columnNames[0] = "Raw";
		columnNames[1] = "Value";

		columnClass = new Class[] {
				Integer.class, //Name
				String.class
		};

		columnWidths = new int[columnNames.length];
		columnWidths[0] = 50;
		columnWidths[1] = 50;
		
		filterColumns = new int[] {0,1};
	}

}
