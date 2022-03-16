package spacecraftEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import spacecraftEditor.listEditors.curves.CurvesTableModel;

public abstract class CsvFileEditorGrid extends JPanel implements MouseListener, TableModelListener  {

	private static final long serialVersionUID = 1L;
	CsvTableModel tableModel;
	public JTable table;
	
	public CsvFileEditorGrid(CsvTableModel model) {
		setLayout(new BorderLayout());
		tableModel = model;
		
		table = new JTable(model);
		table.setAutoCreateRowSorter(false);  // sorting does not help here because it does not change the save order
		JScrollPane scrollPane = new JScrollPane (table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(100,400));
		table.setFillsViewportHeight(true);
		//	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		add(scrollPane, BorderLayout.CENTER);//, BorderLayout.WEST);
		for (int i=0; i < tableModel.columnWidths.length; i++) {
		TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(tableModel.columnWidths[i]);
		}
		
		table.addMouseListener(this);
		table.getModel().addTableModelListener(this);
	}
	
	public void setData(String titleString, String[][] d) {
		TitledBorder title = new TitledBorder(null, titleString, TitledBorder.LEADING, TitledBorder.TOP, null, null);
		title.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
		setBorder(title);
		tableModel.setData(d);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
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

	@Override
	public void tableChanged(TableModelEvent e) {
		// If we need to do something when a cell is edited, then it happens here
		// e.g. validate that the lines of a module are correct
		
//		int row = e.getFirstRow();
//		int column = e.getColumn();
//		if (column == -1) return;
//		TableModel model = (TableModel)e.getSource();
//		String columnName = model.getColumnName(column);
//		Object data = model.getValueAt(row, column);
//		model.setValueAt(e, row, column);

	}
}
