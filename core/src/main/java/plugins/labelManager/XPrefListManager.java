package plugins.labelManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import preferences.XPreferences;
import signals.SSCenter;

public class XPrefListManager implements ActionListener, ListSelectionListener {
	
	public static final String SIG_ValueSelected_NULL = "SIG_ValueSelected_NULL";
	public static final String SIG_NoValueSelected_NULL = "SIG_NoValueSelected_NULL";
	public static final String SIG_AddClicked_NULL = "SIG_AddClicked_NULL";
	public static final String SIG_RemoveClicked_NULL = "SIG_RemoveClicked_NULL";
	public static final String SIG_RenameClicked_NULL = "SIG_RenameClicked_NULL";
	
	private JPanel panel;
	private JList<String> list;
	private JButton addButton, removeButton, renameButton;
	private JTextField itemField;
	private XPreferences model;
	private XPrefListModel listModel;
	
	public XPrefListManager(String title)
	{
		this.listModel = null;
		this.model = null;
		
		// int width = 140;
		this.panel = new JPanel(new MigLayout("flowy, ins 0", "[fill,grow]", "[]0[fill,grow]0[]0[]"));
		this.panel.setBackground(DisplayStatics.lightBackground);
		
		// Create the list and put it in a scroll pane
		list = new JList<String>();
		list.setSelectedIndex(0);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(this);
		JScrollPane listScrollPane = new JScrollPane(list);
		
		itemField = new JTextField();
		itemField.addActionListener(this);
		itemField.setFont(FontUtility.defaultFont);
		
		addButton = new JButton("Add");
		addButton.addActionListener(this);
		addButton.setEnabled(true);
		addButton.setFont(FontUtility.defaultFonts);
		
		removeButton = new JButton("Remove");
		removeButton.addActionListener(this);
		removeButton.setEnabled(false);
		removeButton.setFont(FontUtility.defaultFonts);
		
		renameButton = new JButton("Rename");
		renameButton.addActionListener(this);
		renameButton.setEnabled(false);
		renameButton.setFont(FontUtility.defaultFonts);
		
		// Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(DisplayStatics.lightBackground);
		buttonPane.setLayout(new MigLayout("flowx, ins 0", "[fill,grow]0[fill,grow]0[fill,grow]", "[]"));
		buttonPane.add(addButton, "growx, wmin 50");
		buttonPane.add(removeButton, "growx, wmin 50");
		buttonPane.add(renameButton, "growx, wmin 50");
		
		JLabel theTitle = new JLabel(title);
		theTitle.setFont(FontUtility.boldFont);
		this.panel.add(theTitle, "growx, gap 3");
		this.panel.add(listScrollPane, "growx, wmin 150, gap 3 3 0 0");
		this.panel.add(this.itemField, "growx");
		this.panel.add(buttonPane, "growx");
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void setModel(XPreferences model)
	{
		this.model = model;
		this.listModel = new XPrefListModel();
		this.listModel.setModel(model);
		this.list.setModel(this.listModel);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(this.listModel == null)
			return;
		if(e.getSource() == itemField && !this.itemField.getText().equals(""))
		{
			SSCenter.defaultCenter().emit(this, SIG_AddClicked_NULL, (Object[]) null);
		}
		else if(e.getSource() == addButton && !this.itemField.getText().equals("") && this.getIndexOfItem(itemField.getText()) == -1)
		{
			SSCenter.defaultCenter().emit(this, SIG_AddClicked_NULL, (Object[]) null);
		}
		else if(e.getSource() == removeButton)
		{
			int selectedIndex = this.list.getSelectedIndex();
			if(selectedIndex == -1)
				return;
			SSCenter.defaultCenter().emit(this, SIG_RemoveClicked_NULL, (Object[]) null);
		}
		else if(e.getSource() == renameButton && !this.itemField.getText().equals(""))
		{
			SSCenter.defaultCenter().emit(this, SIG_RenameClicked_NULL, (Object[]) null);
		}
	}
	
	public String getCurrentSelection()
	{
		if(this.list.getSelectedIndex() == -1)
			return null;
		return this.listModel.getElementAt(this.list.getSelectedIndex());
	}
	
	public int getCurrentSelectionIndex()
	{
		return this.list.getSelectedIndex();
	}
	
	public int getIndexOfItem(String name)
	{
		return this.model.getChildNodeNames().indexOf(name);
	}
	
	public String getTextField()
	{
		return this.itemField.getText();
	}
	
	public int size()
	{
		return this.model.getChildNodeNames().size();
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		if(e.getValueIsAdjusting() == false)
		{
			if(this.list.getSelectedIndex() == -1)
			{
				this.removeButton.setEnabled(false);
				this.renameButton.setEnabled(false);
				SSCenter.defaultCenter().emit(this, SIG_NoValueSelected_NULL, (Object[]) null);
			}
			else
			{
				if(addButton.isEnabled())
				{
					this.renameButton.setEnabled(true);
				}
				this.removeButton.setEnabled(true);
				SSCenter.defaultCenter().emit(this, SIG_ValueSelected_NULL, (Object[]) null);
			}
		}
	}
	
	public void setSelectedIndex(int index, int defaultIndex)
	{
		if(index == -1)
		{
			this.list.setSelectedIndex(defaultIndex);
			this.list.ensureIndexIsVisible(defaultIndex);
		}
		else
		{
			this.list.setSelectedIndex(index);
			this.list.ensureIndexIsVisible(index);
		}
	}
	
	public XPrefListModel getListModel()
	{
		return this.listModel;
	}
	
	public XPreferences getModel()
	{
		return this.model;
	}
}
