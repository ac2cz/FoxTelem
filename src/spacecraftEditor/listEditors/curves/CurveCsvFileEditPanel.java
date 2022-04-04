package spacecraftEditor.listEditors.curves;

import java.io.FileNotFoundException;
import java.io.IOException;

import common.Log;
import common.Spacecraft;
import spacecraftEditor.listEditors.CsvFileEditPanel;
import spacecraftEditor.listEditors.CsvFileEditorGrid;
import telemetry.LayoutLoadException;

public class CurveCsvFileEditPanel extends CsvFileEditPanel {
	private static final long serialVersionUID = 1L;

	public CurveCsvFileEditPanel(Spacecraft sat, CurvesTableModel model, String titleString, String file) {
		super(sat, model, titleString, file, "csv");
		// TODO Auto-generated constructor stub
	}
	
	protected void updateSpacecraft() {
		sat.useConversionCoeffs = true;
		sat.conversionCurvesFileName = filename;
		try {
			sat.loadConversions();
			sat.save_master_params();
		} catch (FileNotFoundException e) {
			Log.errorDialog("ERROR", "Can not re-load the conversions\n" + e);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Can not process the conversions\n" + e);
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Can not re-load the conversions\n" + e);
		}
	}
}
