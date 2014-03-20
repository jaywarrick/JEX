package guiObject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class ListManager {
	
	public static final String SIG_SelectionChanged_NULL = "SIG_SelectionChanged_NULL";
	
	// Gui variables
	
	private JPanel panel = new JPanel();
	private JPanel contentPane = new JPanel();
	private JScrollPane scroll = new JScrollPane(contentPane);
	
	// variables
	private List<ListManagerItem> panelList;
	private String selectedItem;
	
	public ListManager()
	{
		
		panelList = new ArrayList<ListManagerItem>(0);
		this.selectedItem = null;
		initialize();
		this.setItems(new Vector<String>());
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void initialize()
	{
		
		contentPane.setBackground(DisplayStatics.lightBackground);
		contentPane.setLayout(new MigLayout("flowy,ins 3 0 3 3, gap 0", "[fill,grow]", "[]"));
		
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new BorderLayout());
		this.panel.add(scroll, BorderLayout.CENTER);
	}
	
	public void setItems(List<String> items)
	{
		
		contentPane.removeAll();
		
		if(items == null || items.size() == 0)
		{
			JPanel temp = new JPanel();
			temp.setLayout(new BorderLayout());
			JLabel label = new JLabel("No items");
			label.setFont(FontUtility.italicFonts);
			temp.add(label);
			temp.setPreferredSize(new Dimension(20, 20));
			contentPane.add(temp);
		}
		else
		{
			panelList = new ArrayList<ListManagerItem>(0);
			
			for (String itemName : items)
			{
				ListManagerItem newItemPane = new ListManagerItem(itemName, this);
				// newItemPane.setPreferredSize(new Dimension(100,25));
				// newItemPane.setMaximumSize(new
				// Dimension(Integer.MAX_VALUE,25));
				panelList.add(newItemPane);
				contentPane.add(newItemPane, "growx");
				// contentPane.add(Box.createRigidArea(new Dimension(1,1)));
			}
			// contentPane.add(Box.createVerticalGlue());
		}
		
		contentPane.revalidate();
		contentPane.repaint();
	}
	
	/**
	 * Return the currently selected item
	 * 
	 * @return
	 */
	public String getSelectedItem()
	{
		return this.selectedItem;
	}
	
	/**
	 * Public version of method sends no signal
	 * 
	 * @param selectedItem
	 */
	public void setSelectedItem(String selectedItem)
	{
		boolean foundMatch = false;
		for (ListManagerItem itemPane : panelList)
		{
			String itemName = itemPane.getItemName();
			if(selectedItem != null && itemName.equals(selectedItem))
			{
				this.selectedItem = selectedItem;
				foundMatch = true;
				itemPane.setViewed(true);
			}
			else
			{
				itemPane.setViewed(false);
			}
		}
		if(!foundMatch)
			this.selectedItem = null;
	}
	
	/**
	 * Iternal version of method sends a signal
	 * 
	 * @param selectedItem
	 */
	protected void _setSelectedItem(String selectedItem)
	{
		this.setSelectedItem(selectedItem);
		SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
	}
	
	public void setSelectedIndex(int index)
	{
		if(index < 0)
			index = 0;
		if(index >= panelList.size())
			index = panelList.size() - 1;
		for (int i = 0; i < panelList.size(); i++)
		{
			ListManagerItem item = panelList.get(i);
			if(index == i)
			{
				this.selectedItem = item.getItemName();
				item.setViewed(true);
			}
			else
			{
				item.setViewed(false);
			}
		}
	}
	
	protected void _setSelectedIndex(int index)
	{
		setSelectedIndex(index);
		SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
	}
	
}
