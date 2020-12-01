package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

import common.Config;
import common.Log;
import common.Spacecraft;

public class ServerDownloadDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	JButton btnSave, btnCancel,btnClear, btnSelectAll;
	JList<String> list;
	
	public ServerDownloadDialog(JFrame owner, boolean modal) {
		super(owner, modal);
		setTitle("Download Server Data");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		int y = getOwner().getY()+getOwner().getHeight()/2-300;
		if (y < 0) y = 0;
		int x = getOwner().getX()+getOwner().getWidth()/2-150;
		if (x < 0) x = 0;
		setBounds(x,y,300,400);
		
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		// South panel for the buttons
		JPanel southpanel = new JPanel();
		contentPane.add(southpanel, BorderLayout.SOUTH);
		southpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		btnSave = new JButton("Download");
		btnSave.addActionListener(this);
		southpanel.add(btnSave);
		getRootPane().setDefaultButton(btnSave);

		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		southpanel.add(btnCancel);

		// North panel for settings that span across the window
		JPanel northpanel = new JPanel();
		contentPane.add(northpanel, BorderLayout.NORTH);
		northpanel.setLayout(new BoxLayout(northpanel, BoxLayout.X_AXIS));

		btnSelectAll = new JButton("Select all");
		northpanel.add(btnSelectAll);
		btnClear = new JButton("Clear");
		northpanel.add(btnClear);
		btnSelectAll.addActionListener(this);
		btnClear.addActionListener(this);	
		
		JPanel centerpanel = new JPanel();
		contentPane.add(centerpanel, BorderLayout.CENTER);
		
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		List<Spacecraft> names = Config.satManager.spacecraftList;
		for (Spacecraft sat : names)
			listModel.addElement(sat.user_display_name);

		list = new JList<String>(listModel);
		list.setSelectionModel(new DefaultListSelectionModel() 
		{
			private static final long serialVersionUID = 1L;

			@Override
		    public void setSelectionInterval(int index0, int index1) 
		    {
		        if(list.isSelectedIndex(index0)) 
		        {
		            list.removeSelectionInterval(index0, index1);
		        }
		        else 
		        {
		            list.addSelectionInterval(index0, index1);
		        }
		    }
		});
		//list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		list.setLayoutOrientation(JList.VERTICAL); //.HORIZONTAL_WRAP);
		list.setVisibleRowCount(-1);
		ListModel<String> model = list.getModel();
		list.setSelectionInterval(0, ((DefaultListModel<String>) model).getSize()-1);
		
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(100, 200));
		//listScroller.setAlignmentX(LEFT_ALIGNMENT);
		
		centerpanel.add(listScroller);
	}
	
	public void setSelectedValues(JList<String> list, List<String> values) {
	    list.clearSelection();
	    for (String value : values) {
	        //list.setSelectedValue(value, false);
	        int index = getIndex(list.getModel(), value);
	        if (index >=0) {
	            list.addSelectionInterval(index, index);
	        }
	    }
	}
	
	public int getIndex(ListModel<String> model, Object value) {
	    if (value == null) return -1;
	    if (model instanceof DefaultListModel) {
	        return ((DefaultListModel<String>) model).indexOf(value);
	    }
	    for (int i = 0; i < model.getSize(); i++) {
	        if (value.equals(model.getElementAt(i))) return i;
	    }
	    return -1;
	}
	
	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnCancel) {
			this.dispose();
		}
		if (e.getSource() == btnSelectAll) {
			ListModel<String> model = list.getModel();
			list.setSelectionInterval(0, ((DefaultListModel<String>) model).getSize()-1);
		}
		if (e.getSource() == btnClear) {
			list.clearSelection();
		}
		if (e.getSource() == btnSave) {
			boolean dispose = true;
			List<String> satNames = list.getSelectedValuesList();
			if (satNames.isEmpty()) {
				Log.errorDialog("Error", "You must pick at least one spacecraft to download");
				return;
			}
			ArrayList<Spacecraft> sats = new ArrayList<Spacecraft>();
			for (String name : satNames) {
				sats.add(Config.satManager.getSpacecraftByDisplayName(name));
			}
			((MainWindow)getOwner()).downloadServerData(sats);
			
			if (dispose) {
				this.dispose();
			}
		}
		
	}
}
