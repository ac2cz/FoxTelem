package spacecraftEditor.listEditors.lookupTables;

import common.Spacecraft;
import spacecraftEditor.CsvFileEditPanel;
import spacecraftEditor.CsvFileEditorGrid;
import spacecraftEditor.CsvTableModel;

public class LookupCsvFileEditPanel extends CsvFileEditPanel {
	public LookupCsvFileEditPanel(Spacecraft sat, CsvTableModel model,
			String titleString, String file) {
		super(sat, model, titleString, file);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected void updateSpacecraft() {
		sat.save_master_params();
	}

}
