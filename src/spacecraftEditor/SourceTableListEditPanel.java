package spacecraftEditor;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import common.Config;
import common.Spacecraft;
import spacecraftEditor.listEditors.CsvTableModel;
import spacecraftEditor.listEditors.TableListEditPanel;
import telemetry.LayoutLoadException;
import telemetry.TelemFormat;

public class SourceTableListEditPanel extends TableListEditPanel {
	private static final long serialVersionUID = 1L;
	JPanel leftSourcesPanel, sourceStats;
	
	public SourceTableListEditPanel(Spacecraft sat, String title, CsvTableModel listTableModel,
			CsvTableModel csvTableModel, SpacecraftEditPanel parent) {
		super(sat, title, listTableModel, csvTableModel, parent);
		txtFilename.setEditable(true);
		lFilename.setText("Format");
		
		leftSourcesPanel = new JPanel();
		leftPanel.add(leftSourcesPanel);
		TitledBorder headingSources = SpacecraftEditPanel.title("Format Details");
		leftSourcesPanel.setBorder(headingSources);
		leftSourcesPanel.setLayout(new BorderLayout());

		sourceStats = new JPanel();
		leftPanel.add(sourceStats, BorderLayout.SOUTH);
	}

	@Override
	protected JPanel addCenterPanel() {
		// RIGHT Column

		JPanel center = new JPanel();

		return center;
	}
	
	@Override
	protected void updateRow(int row) {
		txtName.setText(dataLines.get(row)[1]);
		txtFilename.setText(dataLines.get(row)[2]);
		
		// update the source file viewed
	}
	
	@Override
	protected void browseListItem() {
		// do nothing
	}
	
	@Override
	protected void loadTable() {
		dataLines = new ArrayList<String[]>();
		for (int i=0; i< sat.numberOfSources; i++) {
			String[] dataToAdd = new String[listTableModel.getColumnCount()];
			
			dataToAdd[0] = ""+i;
			if (sat.sourceName != null && sat.sourceName[i] != null) 
				dataToAdd[1] = sat.sourceName[i];
			else
				dataToAdd[1] ="NONE";
			if (sat.sourceFormatName != null && sat.sourceFormatName[i] != null) 
				dataToAdd[2] = sat.sourceFormatName[i];
			else
				dataToAdd[2] ="NONE";
		dataLines.add(dataToAdd);
		}
	}

	@Override
	protected void saveTable() throws IOException, LayoutLoadException {
		sat.sourceFormat = new TelemFormat[dataLines.size()];
		sat.sourceName = new String[dataLines.size()];
		sat.numberOfSources = dataLines.size();
		sat.sourceFormatName = new String[dataLines.size()];
		for (int j = 0; j < dataLines.size(); j++) {
			if (dataLines.get(j)[1] == null) {
				sat.sourceName[j] = null;
			} else {
				sat.sourceFormat[j] = Config.satManager.getFormatByName(dataLines.get(j)[2]);
				sat.sourceFormatName[j] = dataLines.get(j)[2];
				sat.sourceName[j] = dataLines.get(j)[1];
			}
			
		}
		
	}

	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		
		if (e.getSource() == table) {
			if (sat.numberOfSources == 0) return;
			int row = table.rowAtPoint(e.getPoint());
			if (row == -1) return;
			int col = table.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				//Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());
				if (sourceStats != null)
					leftSourcesPanel.remove(sourceStats);
				
				String name = (String) table.getValueAt(row, 1);
				if (name != null && ! name.equalsIgnoreCase("NONE")) {
					if (sat.sourceFormat == null || sat.sourceFormat[row] == null) return; // no format here
					System.out.println("Edit:" + sat.sourceFormat[row]);
	//				sourceFormatSelected = row;
					if (e.getClickCount() == 2) {
						String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
						EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.sourceFormatName[row] + ".format");
						editor.setVisible(true);
					}

					

					sourceStats = new JPanel();
					sourceStats.setLayout(new BoxLayout(sourceStats, BoxLayout.Y_AXIS));
					leftSourcesPanel.add(sourceStats, BorderLayout.SOUTH);

					int numRsWords = sat.sourceFormat[row].getInt(TelemFormat.RS_WORDS);
					int headerLength = sat.sourceFormat[row].getInt(TelemFormat.HEADER_LENGTH);
					int frameLength = sat.sourceFormat[row].getFrameLength();
					int dataLength = sat.sourceFormat[row].getInt(TelemFormat.DATA_LENGTH);
					int trailerLength = 32 * numRsWords;

					JLabel labFrameLen = new JLabel("Frame Length: "+ frameLength );

					JLabel labHeaderLen = new JLabel("Header Length: " + headerLength);
					JLabel labDataLen = new JLabel("Data Length: " + dataLength );
					JLabel labRSWords = new JLabel("RS Words: " + numRsWords);
					JLabel labTrailerLen = new JLabel("Trailer Length: " + trailerLength);
					sourceStats.add(labFrameLen);
					sourceStats.add(labHeaderLen);
					sourceStats.add(labDataLen);
					sourceStats.add(labRSWords);
					sourceStats.add(labTrailerLen);

				}
		}
		}
		
	}
	
}
