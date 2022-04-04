package spacecraftEditor.listEditors.lookupTables;

import java.io.IOException;
import java.util.ArrayList;

import common.Spacecraft;
import spacecraftEditor.SpacecraftEditPanel;
import spacecraftEditor.listEditors.CsvTableModel;
import spacecraftEditor.listEditors.ListTableModel;
import spacecraftEditor.listEditors.TableListEditPanel;
import telemetry.LayoutLoadException;
import telemetry.conversion.ConversionLookUpTable;

public class LookupTableListEditPanel extends TableListEditPanel{
	private static final long serialVersionUID = 1L;

	public LookupTableListEditPanel(Spacecraft sat, String title, ListTableModel listTableModel,
			CsvTableModel csvTableModel, SpacecraftEditPanel parent) {
		super(sat, title, listTableModel, csvTableModel, "tab", parent);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Load the lookup tables into the internal format
	 */
	protected void loadTable() {
		dataLines = new ArrayList<String[]>();
		for (int i=0; i< sat.numberOfLookupTables; i++) {
			String[] dataToAdd = new String[listTableModel.getColumnCount()];
			
			dataToAdd[0] =""+i;
			if (sat.lookupTable[i] != null) 
				dataToAdd[1] = sat.lookupTable[i].getName();
			else
				dataToAdd[1] ="NONE";
			if (sat.lookupTableFilename[i] != null) 
				dataToAdd[2] = sat.lookupTableFilename[i];
			else
				dataToAdd[2] ="NONE";
			
			dataLines.add(dataToAdd);
		}
	}
	
	/**
	 * Save the internal format back to the spacecraft arrays
	 * @throws IOException
	 * @throws LayoutLoadException 
	 */
	protected void saveTable() throws IOException, LayoutLoadException {
		sat.lookupTable = new ConversionLookUpTable[dataLines.size()];
		sat.numberOfLookupTables = dataLines.size();
		sat.lookupTableFilename = new String[dataLines.size()];
		for (int j = 0; j < dataLines.size(); j++) {
			if (dataLines.get(j)[1] == null) {
				sat.lookupTable[j] = null;
			} else {
				sat.lookupTableFilename[j] = dataLines.get(j)[2];
				sat.lookupTable[j] = new ConversionLookUpTable(dataLines.get(j)[1],sat.lookupTableFilename[j], sat);
			}
			
		}
	}
}
