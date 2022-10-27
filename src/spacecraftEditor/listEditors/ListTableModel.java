package spacecraftEditor.listEditors;

public abstract class ListTableModel extends CsvTableModel {
	private static final long serialVersionUID = 1L;

	public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.

		return false;
	}

}
