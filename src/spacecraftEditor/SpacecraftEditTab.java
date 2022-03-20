package spacecraftEditor;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import common.Spacecraft;
import spacecraftEditor.listEditors.curves.CurvesTableModel;
import spacecraftEditor.listEditors.expressions.ExpressionsCsvFileEditPanel;
import spacecraftEditor.listEditors.expressions.ExpressionsTableModel;
import spacecraftEditor.listEditors.curves.CurveCsvFileEditPanel;
import spacecraftEditor.listEditors.frames.FrameListEditPanel;
import spacecraftEditor.listEditors.lookupTables.LookupListTableModel;
import spacecraftEditor.listEditors.lookupTables.LookupTableListEditPanel;
import spacecraftEditor.listEditors.lookupTables.LookupTableModel;
import spacecraftEditor.listEditors.payload.PayloadListEditPanel;
import spacecraftEditor.listEditors.stringLookupTables.StringLookupTableListEditPanel;

/**
 * This holds an entire spacecraft that is being edited.  It is organized as follows:
 * 
 * LEFT: SpacecraftEditPanel
 * CENTER: TabbedPane containing SpacecraftListEditPanels
 * 
 * @author chris
 *
 */
public class SpacecraftEditTab extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String CURVES_TEMPLATE_FILENAME = "templates"+File.separator+"CURVES_template.csv";
	public static final String MATH_EXPRESSIONS_TEMPLATE_FILENAME = "templates"+File.separator+"MATH_EXPRESSIONS_template.csv";

	Spacecraft sat;
	JTabbedPane tabbedPane;
	SpacecraftEditPanel spacecraftEditPanel;
	
	String[] satLists = {"Payloads","Frames","Lookup tables", "String Lookup tables", "Conversion Coeff", "Math Expressions" };
	
	public SpacecraftEditTab(Spacecraft s) {
		sat = s;
		setLayout(new BorderLayout(0, 0));
//		spacecraftEditPanel = new SpacecraftEditPanel(sat);
//		add(spacecraftEditPanel, BorderLayout.WEST);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabbedPane, BorderLayout.CENTER);
	
		// Params
		spacecraftEditPanel = new SpacecraftEditPanel(sat);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Paramaters" + "</b></body></html>", spacecraftEditPanel );
		
		// PAYLOADS 
		PayloadListEditPanel payloadListEditPanel = new PayloadListEditPanel(sat,spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ "Payloads" + "</b></body></html>", payloadListEditPanel );
		
		// FRAMES
		FrameListEditPanel frameListEditPanel = new FrameListEditPanel(sat,spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ satLists[1] + "</b></body></html>", frameListEditPanel );
		
		// CURVES
		CurvesTableModel model = new CurvesTableModel();
		String f = sat.conversionCurvesFileName;
		if (f == null) f = CURVES_TEMPLATE_FILENAME;
		CsvFileEditorGrid curveCsvFileEditorPanel = new CsvFileEditorGrid(model);
		CsvFileEditPanel csvFileEdit = new CurveCsvFileEditPanel(sat, model,curveCsvFileEditorPanel, "Curves",f);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ "Conversion Curves" + "</b></body></html>", csvFileEdit );
		
		// EXPRESSIONS
		ExpressionsTableModel expressionsModel = new ExpressionsTableModel();
		String expFile = sat.conversionExpressionsFileName;
		if (expFile == null) expFile = MATH_EXPRESSIONS_TEMPLATE_FILENAME;
		CsvFileEditorGrid expressionsCsvFileEditorPanel = new CsvFileEditorGrid(expressionsModel);
		ExpressionsCsvFileEditPanel expressionsCsvFileEdit = new ExpressionsCsvFileEditPanel(sat, expressionsModel,expressionsCsvFileEditorPanel, "Expressions",expFile);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Math Expressions" + "</b></body></html>", expressionsCsvFileEdit );
		
		// LOOKUP TABLES 
		LookupListTableModel lookupListTableModel = new LookupListTableModel();
		LookupTableModel lookupTableModel = new LookupTableModel();
		LookupTableListEditPanel lookupTableListEditPanel = new LookupTableListEditPanel(sat,"Lookup Tables", lookupListTableModel, lookupTableModel, spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Lookup Tables" + "</b></body></html>", lookupTableListEditPanel );
		
		// STRING LOOKUP TABLES 
		LookupListTableModel stringLookupListTableModel = new LookupListTableModel();
		LookupTableModel stringLookupTableModel = new LookupTableModel();
		StringLookupTableListEditPanel stringLookupTableListEditPanel = new StringLookupTableListEditPanel(sat,"String Lookup Tables", stringLookupListTableModel, stringLookupTableModel, spacecraftEditPanel);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "String Lookup Tables" + "</b></body></html>", stringLookupTableListEditPanel );

	}
}
