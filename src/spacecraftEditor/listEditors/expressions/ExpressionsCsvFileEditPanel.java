package spacecraftEditor.listEditors.expressions;

import java.io.FileNotFoundException;
import java.io.IOException;

import common.Log;
import common.Spacecraft;
import spacecraftEditor.listEditors.CsvFileEditPanel;
import spacecraftEditor.listEditors.CsvFileEditorGrid;
import spacecraftEditor.listEditors.CsvTableModel;
import telemetry.LayoutLoadException;

public class ExpressionsCsvFileEditPanel extends CsvFileEditPanel {
	private static final long serialVersionUID = 1L;

	public ExpressionsCsvFileEditPanel(Spacecraft sat, CsvTableModel model,
			String titleString, String file) {
		super(sat, model, titleString, file, "csv");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void updateSpacecraft() {
		sat.useConversionCoeffs = true;
		sat.conversionExpressionsFileName = filename;
		try {
			sat.loadConversions();
			sat.save_master_params();
		} catch (FileNotFoundException e) {
			Log.errorDialog("ERROR", "Can not re-load the expressions\n" + e);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Can not process the expressions\n" + e);
		} catch (IOException e) {
			Log.errorDialog("ERROR", "Can not re-load the expressions\n" + e);
		}
		
	}

}
