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
import common.Log;
import common.Spacecraft;
import spacecraftEditor.listEditors.CsvTableModel;
import spacecraftEditor.listEditors.ListTableModel;
import spacecraftEditor.listEditors.TableListEditPanel;
import telemetry.LayoutLoadException;
import telemetry.Format.TelemFormat;

public class SourceTableListEditPanel extends TableListEditPanel {
	private static final long serialVersionUID = 1L;
	JPanel leftSourcesPanel, sourceStats;
	
	public SourceTableListEditPanel(Spacecraft sat, String title, ListTableModel listTableModel,
			CsvTableModel csvTableModel, SpacecraftEditPanel parent) {
		super(sat, title, listTableModel, csvTableModel, "format", parent);
		txtFilename.setEditable(true);
		lFilename.setText("Format");
		
		leftSourcesPanel = new JPanel();
		leftPanel.add(leftSourcesPanel);
		TitledBorder headingSources = SpacecraftEditPanel.title("Format Details");
		leftSourcesPanel.setBorder(headingSources);
		leftSourcesPanel.setLayout(new BorderLayout());

		sourceStats = new JPanel();
		leftPanel.add(sourceStats, BorderLayout.SOUTH);
		sourceStats.setLayout(new BoxLayout(sourceStats, BoxLayout.Y_AXIS));
		leftSourcesPanel.add(sourceStats, BorderLayout.SOUTH);

		labFrameLen = new JLabel();
		labHeaderLen = new JLabel();
		labDataLen = new JLabel();
		labRSWords = new JLabel();
		labTrailerLen = new JLabel();
		sourceStats.add(labFrameLen);
		sourceStats.add(labHeaderLen);
		sourceStats.add(labDataLen);
		sourceStats.add(labRSWords);
		sourceStats.add(labTrailerLen);
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
		updateSourceStats(row);
	}
	
	@Override
	protected void browseListItem() {
		Log.println("Browse for Source filename ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify file", "Select", "format");
		if (file == null) return;

		String fileName = file.getName();
		try {
			@SuppressWarnings("unused")
			TelemFormat tmpFormat = new TelemFormat(Config.currentDir + File.separator + "spacecraft" + File.separator + fileName);
			txtFilename.setText(fileName);
		} catch (LayoutLoadException e) {
			Log.errorDialog("ERROR", "Can not parse the format from format file: \n" + fileName + "\n" + e);
		}
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
				dataToAdd[2] = sat.sourceFormatName[i]+ ".format";
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
				TelemFormat tmpFormat = new TelemFormat(Config.currentDir + File.separator + "spacecraft" + File.separator + dataLines.get(j)[2]);
				sat.sourceFormat[j] = tmpFormat;
				sat.sourceFormatName[j] = tmpFormat.name;
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
				
				try {
					saveTable();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (LayoutLoadException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} // MAKE SURE IT IS SAVED
				Log.println("CLICKED SOURCE ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());
				
				
				String name = (String) table.getValueAt(row, 1);
				if (name != null && ! name.equalsIgnoreCase("NONE")) {
					if (sat.sourceFormat == null || sat.sourceFormat[row] == null) return; // no format here
					System.out.println("Edit:" + sat.sourceFormat[row]);
	//				sourceFormatSelected = row;
					if (e.getClickCount() == 2) {
						String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
						TextEditorFrame editor = new TextEditorFrame(sat, masterFolder + File.separator + sat.sourceFormatName[row]+".format");
						editor.setVisible(true);
					}

					updateSourceStats(row);
				}
		}
		}
		
	}
	
	JLabel labFrameLen;
	JLabel labHeaderLen;
	JLabel labDataLen;
	JLabel labRSWords;
	JLabel labTrailerLen;
	private void updateSourceStats(int row) {
		if (sat.sourceFormat == null) return;
		if (sat.sourceFormat.length == 0) return;
		int numRsWords = sat.sourceFormat[row].getInt(TelemFormat.RS_WORDS);
		int headerLength = sat.sourceFormat[row].getInt(TelemFormat.HEADER_LENGTH);
		int frameLength = sat.sourceFormat[row].getFrameLength();
		int dataLength = sat.sourceFormat[row].getInt(TelemFormat.DATA_LENGTH);
		int trailerLength = 32 * numRsWords;

		labFrameLen.setText("Frame Length: "+ frameLength );

		labHeaderLen.setText("Header Length: " + headerLength);
		labDataLen.setText("Data Length: " + dataLength );
		labRSWords.setText("RS Words: " + numRsWords);
		labTrailerLen.setText("Trailer Length: " + trailerLength);
		
		this.repaint();
	}
	
}
