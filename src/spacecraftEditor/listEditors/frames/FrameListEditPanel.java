package spacecraftEditor.listEditors.frames;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;

import common.Config;
import common.Log;
import common.Spacecraft;
import spacecraftEditor.EditorFrame;
import spacecraftEditor.SpacecraftEditPanel;
import spacecraftEditor.SpacecraftEditorWindow;
import telemetry.BitArrayLayout;
import telemetry.LayoutLoadException;
import telemetry.SatPayloadStore;
import telemetry.TelemFormat;
import telemetry.frames.FrameLayout;

public class FrameListEditPanel extends JPanel implements MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;
	public static final String FRAME_TEMPLATE_FILENAME = "FRAME_template.frame";

	SpacecraftEditPanel parent;
	Spacecraft sat;
	
	JTable framesTable;
	JPanel rightPanel;
	
	FramesTableModel frameTableModel;
	
	JButton btnAddFrame, btnRemoveFrame,btnBrowseFrame,btnUpdateFrame;
	JTextField frameFilename,frameName;
	
	public FrameListEditPanel(Spacecraft sat, SpacecraftEditPanel parent) {
		this.sat = sat;
		this.parent = parent;
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setLayout(new BorderLayout(0, 0));
		
		JPanel rightPanel = addLeftPanel();
		add(rightPanel, BorderLayout.WEST);
		
		JPanel centerPanel = addCenterPanel();
		add(centerPanel, BorderLayout.CENTER);
		
	}
	
	private void loadFramesTable() {
		String[][] data = new String[sat.numberOfFrameLayouts][3];
		for (int i=0; i< sat.numberOfFrameLayouts; i++) {
			data[i][0] =""+i;
			if (sat.frameLayout[i] != null) 
				data[i][1] = sat.frameLayout[i].name;
			else
				data[i][1] ="NONE";
			if (sat.frameLayoutFilename[i] != null) 
				data[i][2] = sat.frameLayoutFilename[i];
			else
				data[i][2] ="NONE";
		}
		
		if (sat.numberOfFrameLayouts > 0) 
			frameTableModel.setData(data);
		else {
			String[][] fakeRow = {{"","","",""}};
			frameTableModel.setData(fakeRow);
		}
	}
	
	private JPanel addLeftPanel() {
		// CENTER Column - Things the user can change - e.g. Layout Files, Freq, Tracking etc
		
		JPanel leftPanel = new JPanel();

		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		JPanel leftPanel1 = new JPanel();
		leftPanel.add(leftPanel1);
		leftPanel1.setLayout(new BorderLayout());
		
		TitledBorder headingFrames = SpacecraftEditPanel.title("Frames");
		leftPanel1.setBorder(headingFrames);

		frameTableModel = new FramesTableModel();
		
		//if (sat.numberOfFrameLayouts > 0) {

		framesTable = new JTable(frameTableModel);
		framesTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane (framesTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		framesTable.setFillsViewportHeight(true);
		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		leftPanel1.add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
		TableColumn column = framesTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(20);
		column = framesTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(100);
		column = framesTable.getColumnModel().getColumn(2);
		column.setPreferredWidth(200);

		framesTable.addMouseListener(this);

	//	leftPanel.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		
		loadFramesTable();

		JPanel footerPanel = new JPanel();
		leftPanel.add(footerPanel, BorderLayout.SOUTH);
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS) );
		
		// Row 1
		JPanel footerPanelRow1 = new JPanel();
		footerPanelRow1.setLayout(new BoxLayout(footerPanelRow1, BoxLayout.X_AXIS) );
		footerPanel.add(footerPanelRow1);
		
		JPanel f1 = new JPanel();
		f1.setLayout(new BoxLayout(f1, BoxLayout.Y_AXIS) );
		JLabel lf1 = new JLabel("Name");
		frameName = new JTextField();
		f1.add(lf1);
		f1.add(frameName);
		footerPanelRow1.add(f1);
		
//		JPanel f2 = new JPanel();
//		f2.setLayout(new BoxLayout(f2, BoxLayout.Y_AXIS) );
//		JLabel lf2 = new JLabel("Type");
//		payloadType = new JComboBox<String>(BitArrayLayout.types); 
//		f2.add(lf2);
//		f2.add(payloadType);
//		footerPanelRow1.add(f2);
		
		JPanel f3 = new JPanel();
		f3.setLayout(new BoxLayout(f3, BoxLayout.Y_AXIS) );
		JLabel lf3 = new JLabel("Filename");
		frameFilename = new JTextField();
		f3.add(lf3);
		f3.add(frameFilename);
		frameFilename.setEditable(false);
		frameFilename.addMouseListener(this);
		footerPanelRow1.add(f3);
		
		// Row 2
		JPanel footerPanelRow2 = new JPanel();
		footerPanel.add(footerPanelRow2);
		
		btnAddFrame = new JButton("Add");
		btnAddFrame.addActionListener(this);
		btnBrowseFrame = new JButton("Browse");
		btnBrowseFrame.addActionListener(this);
		btnRemoveFrame = new JButton("Remove");
		btnRemoveFrame.addActionListener(this);
		btnUpdateFrame = new JButton("Update");
		btnUpdateFrame.addActionListener(this);
		
		footerPanelRow2.add(btnAddFrame);
		footerPanelRow2.add(btnUpdateFrame);
		footerPanelRow2.add(btnRemoveFrame);
		//footerPanelRow2.add(btnBrowsePayload);
		btnAddFrame.setEnabled(true);
//		if (sat.numberOfFrameLayouts == 0)
		btnRemoveFrame.setEnabled(true);
		//centerPanel2.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		leftPanel.add(new Box.Filler(new Dimension(500,10), new Dimension(500,400), new Dimension(500,500)));
		return leftPanel;
	}
	
	
	private JPanel addCenterPanel() {
		// RIGHT Column
		rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

//		LayoutTableModel layoutTableModel = new LayoutTableModel();
//		layoutEditorPanel = new PayloadLayoutEditorPanel(layoutTableModel, "Layouts");
//		rightPanel.add(layoutEditorPanel);
		
		TitledBorder heading2 = SpacecraftEditPanel.title("Frame");
		rightPanel.setBorder(heading2);


//		JPanel rightPanel2 = new JPanel();
//		rightPanel.add(rightPanel2);
//		rightPanel2.setLayout(new BoxLayout(rightPanel2, BoxLayout.Y_AXIS));
//
//		TitledBorder heading3 = SpacecraftEditPanel.title("Layout");
//		rightPanel2.setBorder(heading3);
//
//		rightPanel2.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
		return rightPanel;
	}
	
	private void addFrame() {
		String[] newFrameLayoutFilenames = new String[sat.numberOfFrameLayouts+1];
		newFrameLayoutFilenames[sat.numberOfFrameLayouts] = frameFilename.getText();
		
		for (int i=0; i < sat.numberOfFrameLayouts; i++) {
			newFrameLayoutFilenames[i] = sat.frameLayoutFilename[i];
		}
		sat.frameLayoutFilename = newFrameLayoutFilenames;
		
		
		
		try {
			File dest = new File(Config.currentDir+"/spacecraft"+ File.separator + frameFilename.getText());
			if (!dest.isFile()) {
				File source = new File(Config.currentDir+"/spacecraft"+ File.separator + FRAME_TEMPLATE_FILENAME);
				//  IF IT EXISTS WE SHOULD NOT COPY THE TEMPLATE!
				SatPayloadStore.copyFile(source, dest);
			}
			FrameLayout[] newFrameLayouts = new FrameLayout[sat.numberOfFrameLayouts+1];
			/////////////// NEED PTH TO LAYOUT!!
			newFrameLayouts[sat.numberOfFrameLayouts] = new FrameLayout(sat.foxId, "spacecraft"+ File.separator + frameFilename.getText());
			newFrameLayouts[sat.numberOfFrameLayouts].name = frameName.getText();
//			newLayouts[sat.numberOfLayouts].typeStr = (String)payloadType.getSelectedItem();
			for (int i=0; i < sat.numberOfFrameLayouts; i++) {
				newFrameLayouts[i] = sat.frameLayout[i];
			}
			sat.frameLayout = newFrameLayouts;
			
			sat.numberOfFrameLayouts++;
			parent.save();
			loadFramesTable();
			frameName.setText("");
			frameFilename.setText("");
//			payloadType.setSelectedIndex(0);
		} catch (IOException e1) {
			Log.errorDialog("ERROR", "You need to specify a valid frame file\n"+e1);
		} catch (LayoutLoadException e1) {
			Log.errorDialog("ERROR", "Could not parse the frame template\n"+e1);
		}
	}
	
	private void browseFrame() {
		System.out.println("Browse for Frame ...");
		File dir = new File(Config.currentDir+"/spacecraft");
		File file = SpacecraftEditorWindow.pickFile(dir, this, "Specify frame file", "Select", "frame");
		if (file == null) return;
		frameFilename.setText(file.getName());
	}
	


	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == btnAddFrame) {
			System.out.println("Adding Frame ...");
			addFrame();
			
		}
		if (e.getSource() == btnBrowseFrame) {
			browseFrame();
		}
		
		if (e.getSource() == btnUpdateFrame) {
			int row = framesTable.getSelectedRow();
			System.out.println("Updating row " + row);
			if (sat.numberOfFrameLayouts == 0) return;
			
			try {
				sat.frameLayout[row] = new FrameLayout(sat.foxId, frameFilename.getText());
				sat.frameLayoutFilename[row] = frameFilename.getText();
				sat.frameLayout[row].name = frameName.getText();
//				sat.layout[row].typeStr = (String)payloadType.getSelectedItem();
				parent.save();
				loadFramesTable();
				frameName.setText("");
				frameFilename.setText("");
//				payloadType.setSelectedIndex(0);
			} catch (LayoutLoadException e1) {
				Log.errorDialog("ERROR", "Could not load the payload file\n"+e1);
			}

		}

		if (e.getSource() == btnRemoveFrame) {
			int row = framesTable.getSelectedRow();
			System.out.println("Removing row " + row);
			if (sat.numberOfFrameLayouts == 0) return;
			if (row == -1) {
				Log.infoDialog("Select a row", "Select a frame in the table to remove\n");
				return;
			}
			
			int n = Log.optionYNdialog("Remove Frame layout file too?",
					"Remove this frame layout file as well as the frame table row?\n"+frameFilename.getText() + "\n\nThis will be gone forever\n");
			if (n == JOptionPane.NO_OPTION) {
				
			} else {
				File file = new File(Config.currentDir+"/spacecraft" +File.separator + frameFilename.getText());
				System.out.println("Removing: " +file.getAbsolutePath());
				try {
					SatPayloadStore.remove(file.getAbsolutePath());
				} catch (IOException ef) {
					Log.errorDialog("ERROR removing File", "\nCould not remove the frame layout file\n"+ef.getMessage());
				}
			}
			
			if (sat.numberOfFrameLayouts == 1) {
				sat.numberOfFrameLayouts = 0;
				sat.frameLayout = null;
				sat.frameLayoutFilename = null;

			} else {
				int j = 0;
				FrameLayout[] newLayouts = new FrameLayout[sat.numberOfFrameLayouts-1];
				for (int i=0; i < sat.numberOfFrameLayouts; i++) {
					if (i != row)
						newLayouts[j++] = sat.frameLayout[i];
				}
				sat.frameLayout = newLayouts;

				j = 0;
				String[] newLayoutFilenames = new String[sat.numberOfFrameLayouts-1];
				for (int i=0; i < sat.numberOfFrameLayouts; i++) {
					if (i != row)
						newLayoutFilenames[j++] = sat.frameLayoutFilename[i];
				}
				sat.frameLayoutFilename = newLayoutFilenames;
				sat.numberOfFrameLayouts--;
			}
			parent.save();
			loadFramesTable();
			frameName.setText("");
			frameFilename.setText("");
//			payloadType.setSelectedIndex(0);

		}

	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == frameFilename) {
			browseFrame();
		}
		
		// Display the payloads in a frame when the frame definition is clicked
		if (e.getSource() == framesTable) {
			int row = framesTable.rowAtPoint(e.getPoint());
			int col = framesTable.columnAtPoint(e.getPoint());
			if (row >= 0 && col >= 0) {
				Log.println("CLICKED ROW: "+row+ " and COL: " + col + " COUNT: " + e.getClickCount());

				String masterFolder = Config.currentDir + File.separator + Spacecraft.SPACECRAFT_DIR;
				
				frameName.setText(sat.frameLayout[row].name);
				frameFilename.setText(sat.frameLayoutFilename[row]);
				
				if (e.getClickCount() == 2) {

					EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.frameLayoutFilename[row]);
					editor.setVisible(true);
				}
//				String name = (String) framesTable.getValueAt(row, 1);
//				if (name != null && ! name.equalsIgnoreCase("NONE")) {
//					System.out.println("Edit:" + sat.frameLayoutFilename[row]);
//					String masterFolder = Config.currentDir + File.separator + FoxSpacecraft.SPACECRAFT_DIR;
//					EditorFrame editor = new EditorFrame(sat, masterFolder + File.separator + sat.frameLayoutFilename[row]);
//					editor.setVisible(true);
//				}
				
				if (rightPanel != null)
					remove(rightPanel);
				
				rightPanel = new JPanel();
				add(rightPanel);
				rightPanel.setLayout(new BorderLayout());

				TitledBorder heading2 = SpacecraftEditPanel.title("Frame: " + sat.frameLayout[row].name);
				rightPanel.setBorder(heading2);
				
				JPanel stats = new JPanel();
				stats.setLayout(new BoxLayout(stats,BoxLayout.Y_AXIS));
				rightPanel.add(stats, BorderLayout.NORTH);
				
				int calculatedDataLength = 0;
				
				// Populate table rightPanel1
				// read it from disk, just in case..
				FrameLayout frameLayout;
				try {
					frameLayout = new FrameLayout(sat.foxId, Spacecraft.SPACECRAFT_DIR + File.separator + sat.frameLayoutFilename[row]);
					if (frameLayout != null) {
						FrameTableModel frameTableModel = new FrameTableModel();

						JTable frameTable = new JTable(frameTableModel);
						frameTable.setAutoCreateRowSorter(true);
						JScrollPane scrollPane = new JScrollPane (frameTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						scrollPane.setPreferredSize(new Dimension(100,400));
						frameTable.setFillsViewportHeight(true);
						//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

						rightPanel.add(scrollPane, BorderLayout.CENTER);
						TableColumn column = frameTable.getColumnModel().getColumn(0);
						column.setPreferredWidth(20);
						column = frameTable.getColumnModel().getColumn(1);
						column.setPreferredWidth(160);

						frameTable.addMouseListener(this);
						int numOfPayloads = frameLayout.getNumberOfPayloads();
						String[][] data = new String[numOfPayloads][3];
						for (int i=0; i< numOfPayloads; i++) {
							data[i][0] =""+i;
							data[i][1] = frameLayout.getPayloadName(i);
							int len = frameLayout.getPayloadLength(i);
							calculatedDataLength += len;

//							BitArrayLayout layout = sat.getLayoutByName(frameLayout.getPayloadName(i));
//							int realLen = layout.getMaxNumberOfBytes();
							data[i][2] = ""+len ;
//							if (realLen != len) {
//								data[i][2] = data[i][2]  + " ERROR";
//							}

						}
						
						frameTableModel.setData(data);
						
						//rightPanel1.add(new Box.Filler(new Dimension(200,10), new Dimension(100,400), new Dimension(100,500)));
						
						if (sat.sourceFormat == null || sat.sourceFormat[parent.sourceFormatSelected] == null) {
							Log.errorDialog("MISSING", "No Source Format defined.  Can't calculate lengths\n");
						} else {
							int headerLength = sat.sourceFormat[parent.sourceFormatSelected].getInt(TelemFormat.HEADER_LENGTH);
							calculatedDataLength += headerLength;
							int frameLength = sat.sourceFormat[parent.sourceFormatSelected].getFrameLength();
							int dataLength = sat.sourceFormat[parent.sourceFormatSelected].getInt(TelemFormat.DATA_LENGTH);
							int trailerLength = sat.sourceFormat[parent.sourceFormatSelected].getTrailerLength();
							int calculatedFrameLength = calculatedDataLength + trailerLength;
							JLabel labFrameLen = new JLabel("Length of this frame: " + calculatedFrameLength + "   ( Format: " + frameLength + " )");
							if (frameLength < calculatedFrameLength) {
								labFrameLen.setForeground(Config.AMSAT_RED);
							}
							JLabel labHeaderLen = new JLabel("Header Length: " + headerLength);
							JLabel labDataLen = new JLabel("Data Length: " + calculatedDataLength + "   ( Format: " + dataLength + " )");
							if (dataLength < calculatedDataLength) {
								labDataLen.setForeground(Config.AMSAT_RED);
							}
							JLabel labTrailerLen = new JLabel("Trailer Length: " + trailerLength);
							stats.add(labFrameLen);
							stats.add(labHeaderLen);
							stats.add(labDataLen);
							stats.add(labTrailerLen);
						}
						
					}
					rightPanel.revalidate();
					rightPanel.repaint();
				} catch (LayoutLoadException e1) {
					Log.errorDialog("ERROR", "Error in the frame layout\n" + e1);
//				}  catch (Exception e1) {
//					Log.errorDialog("ERROR", "Can't display the frame layout\n" + e1);
				}
				
				
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
