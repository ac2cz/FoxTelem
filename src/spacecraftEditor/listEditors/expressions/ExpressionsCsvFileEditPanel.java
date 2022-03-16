package spacecraftEditor.listEditors.expressions;

import java.io.FileNotFoundException;
import java.io.IOException;

import common.Log;
import common.Spacecraft;
import spacecraftEditor.CsvFileEditPanel;
import spacecraftEditor.CsvFileEditorGrid;
import spacecraftEditor.CsvTableModel;
import telemetry.LayoutLoadException;

public class ExpressionsCsvFileEditPanel extends CsvFileEditPanel {
	private static final long serialVersionUID = 1L;

	public ExpressionsCsvFileEditPanel(Spacecraft sat, CsvTableModel model, CsvFileEditorGrid csvFileEditorPanel,
			String titleString, String file) {
		super(sat, model, csvFileEditorPanel, titleString, file);
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
