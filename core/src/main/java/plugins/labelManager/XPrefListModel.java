package plugins.labelManager;

import javax.swing.AbstractListModel;

import preferences.XPreferences;

public class XPrefListModel extends AbstractListModel<String> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private XPreferences xpref;
	
	public void setModel(XPreferences model)
	{
		xpref = model;
	}
	
	public String getElementAt(int index)
	{
		return this.xpref.getChildNodeNames().get(index);
	}
	
	public int getSize()
	{
		return xpref.getChildNodeNames().size();
	}
	
	public void fireIntervalAdded(int index)
	{
		this.fireIntervalAdded(this, index, index);
	}
	
	public void fireIntervalRemoved(int index)
	{
		this.fireIntervalRemoved(this, index, index);
	}
	
	public void fireContentsChanged(int index)
	{
		this.fireContentsChanged(this, index, index);
	}
	
}
