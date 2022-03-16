package spacecraftEditor;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import common.Spacecraft;
import spacecraftEditor.listEditors.curves.CurvesTableModel;
import spacecraftEditor.listEditors.expressions.ExpressionsCsvFileEditPanel;
import spacecraftEditor.listEditors.expressions.ExpressionsCsvFileEditorGrid;
import spacecraftEditor.listEditors.expressions.ExpressionsTableModel;
import spacecraftEditor.listEditors.curves.CurveCsvFileEditPanel;
import spacecraftEditor.listEditors.curves.CurveCsvFileEditorGrid;
import spacecraftEditor.listEditors.frames.FrameListEditPanel;
import spacecraftEditor.listEditors.payload.PayloadListEditPanel;

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
	public static final String CURVES_TEMPLATE_FILENAME = "CURVES_template.csv";
	public static final String MATH_EXPRESSIONS_TEMPLATE_FILENAME = "MATH_EXPRESSIONS_template.csv";

	Spacecraft sat;
	JTabbedPane tabbedPane;
	SpacecraftEditPanel spacecraftEditPanel;
	
	String[] satLists = {"Payloads","Frames","Lookup tables", "String Lookup tables", "Conversion Coeff", "Math Expressions" };
	
	public SpacecraftEditTab(Spacecraft s) {
		sat = s;
		setLayout(new BorderLayout(0, 0));
		spacecraftEditPanel = new SpacecraftEditPanel(sat);
		add(spacecraftEditPanel, BorderLayout.WEST);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabbedPane, BorderLayout.CENTER);
		
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
		CurveCsvFileEditorGrid curveCsvFileEditorPanel = new CurveCsvFileEditorGrid(model);
		CsvFileEditPanel csvFileEdit = new CurveCsvFileEditPanel(sat, model,curveCsvFileEditorPanel, "Curves",f);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
			+ "Conversion Curves" + "</b></body></html>", csvFileEdit );
		
		// EXPRESSIONS
		ExpressionsTableModel expressionsModel = new ExpressionsTableModel();
		String expFile = sat.conversionExpressionsFileName;
		if (expFile == null) expFile = MATH_EXPRESSIONS_TEMPLATE_FILENAME;
		ExpressionsCsvFileEditorGrid expressionsCsvFileEditorPanel = new ExpressionsCsvFileEditorGrid(expressionsModel);
		ExpressionsCsvFileEditPanel expressionsCsvFileEdit = new ExpressionsCsvFileEditPanel(sat, expressionsModel,expressionsCsvFileEditorPanel, "Expressions",expFile);
		tabbedPane.addTab( "<html><body leftmargin=1 topmargin=1 marginwidth=1 marginheight=1><b>" 
				+ "Math Expressions" + "</b></body></html>", expressionsCsvFileEdit );

	}
}
