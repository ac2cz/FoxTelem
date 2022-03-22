package spacecraftEditor.listEditors.stringLookupTables;

import java.io.IOException;
import java.util.ArrayList;

import common.Spacecraft;
import spacecraftEditor.SpacecraftEditPanel;
import spacecraftEditor.listEditors.CsvTableModel;
import spacecraftEditor.listEditors.TableListEditPanel;
import telemetry.LayoutLoadException;
import telemetry.conversion.ConversionLookUpTable;
import telemetry.conversion.ConversionStringLookUpTable;

public class StringLookupTableListEditPanel extends TableListEditPanel {
	private static final long serialVersionUID = 1L;

	public StringLookupTableListEditPanel(Spacecraft sat, String title, CsvTableModel listTableModel,
			CsvTableModel csvTableModel, SpacecraftEditPanel parent) {
		super(sat, title, listTableModel, csvTableModel, parent);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void loadTable() {
		dataLines = new ArrayList<String[]>();
		for (int i=0; i< sat.numberOfStringLookupTables; i++) {
			String[] dataToAdd = new String[listTableModel.getColumnCount()];
			
			dataToAdd[0] =""+i;
			if (sat.stringLookupTable[i] != null) 
				dataToAdd[1] = sat.stringLookupTable[i].getName();
			else
				dataToAdd[1] ="NONE";
			if (sat.stringLookupTableFilename[i] != null) 
				dataToAdd[2] = sat.stringLookupTableFilename[i];
			else
				dataToAdd[2] ="NONE";
			
			dataLines.add(dataToAdd);
		}
		
	}

	@Override
	protected void saveTable() throws IOException, LayoutLoadException {
		sat.stringLookupTable = new ConversionStringLookUpTable[dataLines.size()];
		sat.numberOfStringLookupTables = dataLines.size();
		sat.stringLookupTableFilename = new String[dataLines.size()];
		for (int j = 0; j < dataLines.size(); j++) {
			if (dataLines.get(j)[1] == null) {
				sat.stringLookupTable[j] = null;
			} else {
				sat.stringLookupTableFilename[j] = dataLines.get(j)[2];
				sat.stringLookupTable[j] = new ConversionStringLookUpTable(dataLines.get(j)[1],sat.stringLookupTableFilename[j]);
			}
			
		}
		
	}

}
