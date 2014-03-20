package plugins.selector;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import miscellaneous.StringUtility;
import signals.SSCenter;

public class QuickSelectorList implements ListSelectionListener {
	
	public static final String SIG_ListSelectionChanged_NULL = "SIG_ListSelectionChanged_NULL";
	
	private JPanel panel;
	private JList<String> listDisplay;
	
	private boolean settingSelectedValue = false;
	private boolean settingList = false;
	
	public QuickSelectorList()
	{
		this.panel = new JPanel(new BorderLayout());
		this.listDisplay = new JList<String>();
		JScrollPane scroll = new JScrollPane(this.listDisplay);
		this.panel.add(scroll, BorderLayout.CENTER);
		this.listDisplay.addListSelectionListener(this);
		this.panel.repaint();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void setList(List<String> names)
	{
		// Don't use this method to cause signals that should originate from JEXManager (i.e. this method should be silent)
		StringUtility.sortStringList(names);
		this.settingList = true;
		this.listDisplay.setListData(names.toArray(new String[names.size()]));
		this.settingList = false;
	}
	
	public String getSelection()
	{
		Object selectedItem = this.listDisplay.getSelectedValue();
		if(selectedItem == null)
		{
			return null;
		}
		return this.listDisplay.getSelectedValue().toString();
	}
	
	public void setSelection(String value)
	{
		// Don't use this method to cause signals that should originate from JEXManager (i.e. this method should be silent)
		this.settingSelectedValue = true;
		this.listDisplay.setSelectedValue(value, true);
		this.settingSelectedValue = false;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		// Only emit signals when changes are made by user in gui (i.e. silence any listener events that originate from programmatic changes of the list view)
		if(e.getSource() == this.listDisplay && e.getValueIsAdjusting() == false && !this.settingSelectedValue && !this.settingList)
		{
			SSCenter.defaultCenter().emit(this, SIG_ListSelectionChanged_NULL, (Object[]) null);
		}
	}
}
