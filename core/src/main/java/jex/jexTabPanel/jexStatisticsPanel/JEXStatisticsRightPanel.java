package jex.jexTabPanel.jexStatisticsPanel;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Filter;
import Database.Definition.FilterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.tnvi;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class JEXStatisticsRightPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// GUI variables
	private TreeMap<String,FilterPanel> panelList;
	
	private JPanel titlePane;
	private JPanel centerPane = new JPanel();
	private JScrollPane centralScrollPane = new JScrollPane(centerPane);
	private JPanel suffixPane;
	private JButton reset;
	
	/**
	 * Create the label panel list
	 * 
	 * @param parent
	 */
	public JEXStatisticsRightPanel()
	{
		// Connect to statistics filter change
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.DATASETS, this, "filterListChange", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.STATSFILTERS, this, "filtersChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.STATSGROUPS, this, "groupingChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
		
		panelList = new TreeMap<String,FilterPanel>();
		
		// FilterSet fset = new FilterSet();
		// Filter f = new Filter(JEXData.LABEL,"Valid","true");
		// fset.add(f);
		JEXStatics.jexManager.setStatisticsFilterSet(null);
		JEXStatics.jexManager.setStatisticsGrouping(null);
		
		init();
		navigationChanged();
		filterListChange();
		JEXStatics.jexManager.setStatisticsFilterSet(null);
		JEXStatics.jexManager.setStatisticsGrouping(null);
		
	}
	
	// /
	// / GUI AND INITIALIZATION
	// /
	
	private void init()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(DisplayStatics.background);
		
		JLabel title = new JLabel("LABELS FOR FILTERING");
		title.setFont(FontUtility.boldFont);
		titlePane = new JPanel();
		titlePane.setLayout(new BoxLayout(titlePane, BoxLayout.LINE_AXIS));
		titlePane.setBackground(DisplayStatics.menuBackground);
		titlePane.setPreferredSize(new Dimension(100, 15));
		titlePane.add(Box.createHorizontalGlue());
		titlePane.add(title);
		titlePane.add(Box.createHorizontalGlue());
		
		reset = new JButton("reset");
		reset.setFont(FontUtility.defaultFont);
		reset.addActionListener(this);
		
		suffixPane = new JPanel();
		suffixPane.setBackground(DisplayStatics.lightBackground);
		suffixPane.setLayout(new BoxLayout(suffixPane, BoxLayout.LINE_AXIS));
		suffixPane.setPreferredSize(new Dimension(15, 15));
		suffixPane.add(Box.createHorizontalGlue());
		suffixPane.add(reset);
		
		centerPane.setBackground(DisplayStatics.lightBackground);
		centerPane.setLayout(new MigLayout("flowy,ins 3 3 3 3, gap 0", "[fill,grow]", "[]"));
		centralScrollPane.setBorder(BorderFactory.createEmptyBorder());
		centralScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		centralScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		// Place the objects
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new MigLayout("center,flowy,ins 2", "[center,grow]", "[]1[0:0,fill,grow]1[]"));
		this.add(titlePane, "growx");
		this.add(centralScrollPane, "grow");
		this.add(suffixPane, "growx");
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	/**
	 * Rebuild GUI
	 */
	private void rebuildGUI()
	{
		centerPane.removeAll();
		
		Logs.log("Rebuilding the Filter Panel", 1, this);
		tnvi filters = JEXStatics.jexManager.getFilteredTNVI();
		panelList = new TreeMap<String,FilterPanel>();
		
		// make a filter line for each hierarchy levels
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> hierarchies = filters.get(JEXData.HIERARCHY);
		if(hierarchies == null)
			hierarchies = new TreeMap<String,TreeMap<String,Set<JEXEntry>>>();
		for (String name : hierarchies.keySet())
		{
			TreeMap<String,Set<JEXEntry>> valueSet = hierarchies.get(name);
			if(valueSet == null)
				continue;
			List<String> valueList = new ArrayList<String>(0);
			for (String value : valueSet.keySet())
				valueList.add(value);
			String[] values = valueList.toArray(new String[0]);
			
			FilterPanel expPanel = new FilterPanel(JEXData.HIERARCHY, name, values, this);
			panelList.put(name, expPanel);
		}
		
		// make a filter line for each label
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> labels = filters.get(JEXData.LABEL);
		if(labels == null)
			labels = new TreeMap<String,TreeMap<String,Set<JEXEntry>>>();
		for (String name : labels.keySet())
		{
			TreeMap<String,Set<JEXEntry>> valueSet = labels.get(name);
			if(valueSet == null)
				continue;
			List<String> valueList = new ArrayList<String>(0);
			for (String value : valueSet.keySet())
				valueList.add(value);
			String[] values = valueList.toArray(new String[0]);
			
			FilterPanel expPanel = new FilterPanel(JEXData.LABEL, name, values, this);
			panelList.put(name, expPanel);
		}
		
		// Display them
		for (FilterPanel pane : panelList.values())
		{
			centerPane.add(pane, "growx");
		}
		
		this.revalidate();
		this.repaint();
	}
	
	// /
	// / SIGNALS
	// /
	
	/**
	 * Call to rebuild the filter list panel
	 */
	public void filterListChange()
	{
		this.rebuildGUI();
	}
	
	/**
	 * Call to set the position of the filter buttons
	 */
	public void filtersChanged()
	{
		Logs.log("Updating filter panels", 1, this);
		// set all filter buttons to false
		for (FilterPanel fpane : panelList.values())
		{
			fpane.setAllValuesToFalse();
		}
		
		FilterSet fs = JEXStatics.jexManager.getStatisticsFilterSet();
		if(fs == null)
			return;
		for (Filter f : fs)
		{
			String expName = f.getName();
			Set<String> values = f.getValues();
			FilterPanel panel = panelList.get(expName);
			if(panel == null)
				continue;
			for (String value : values)
			{
				panel.setFilteredValue(value, true);
			}
		}
	}
	
	/**
	 * Call to set the position of the grouping buttons
	 */
	public void groupingChanged()
	{
		Logs.log("Updating grouping panels", 1, this);
		List<TypeName> groups = JEXStatics.jexManager.getStatisticsGrouping();
		Set<String> labelNames = panelList.keySet();
		
		for (String labelName : labelNames)
		{
			FilterPanel panel = panelList.get(labelName);
			if(panel == null)
				continue;
			
			if(contains(groups, labelName))
				panel.setIsGrouped(true);
			else
				panel.setIsGrouped(false);
		}
	}
	
	/**
	 * Navigation change changing the number of entries on which to perform statistics
	 */
	public void navigationChanged()
	{
		this.rebuildGUI();
	}
	
	// /
	// / METHODS FOR ADDING AND REMOVING GROUPS AND FILTERS
	// /
	
	/**
	 * Return true if a typename of name NAME is contained in the list
	 * 
	 * @param tns
	 * @param name
	 * @return
	 */
	private boolean contains(List<TypeName> tns, String name)
	{
		for (TypeName tn : tns)
		{
			if(tn.getName().equals(name))
				return true;
		}
		return false;
	}
	
	public void addedFilter(Type type, String name, String value)
	{
		Logs.log("Add filter name " + name + " and value " + value, 1, this);
		FilterSet fs = JEXStatics.jexManager.getStatisticsFilterSet();
		FilterSet fs2 = fs.duplicate();
		Filter f = new Filter(type, name, value);
		fs2.addFilter(f);
		JEXStatics.jexManager.setStatisticsFilterSet(fs2);
	}
	
	public void removedFilter(Type type, String name, String value)
	{
		Logs.log("Remove filter name " + name + " and value " + value, 1, this);
		FilterSet fs = JEXStatics.jexManager.getStatisticsFilterSet();
		Filter f = new Filter(type, name, value);
		fs.removeFilter(f);
		JEXStatics.jexManager.setStatisticsFilterSet(fs);
	}
	
	public void addedGroup(Type type, String name)
	{
		Logs.log("Add group name " + name, 1, this);
		List<TypeName> tns = JEXStatics.jexManager.getStatisticsGrouping();
		if(contains(tns, name))
			return;
		tns.add(new TypeName(type, name));
		JEXStatics.jexManager.setStatisticsGrouping(tns);
	}
	
	public void removedGroup(Type type, String name)
	{
		Logs.log("Remove group name " + name, 1, this);
		TypeName theTN = new TypeName(type, name);
		List<TypeName> tns = JEXStatics.jexManager.getStatisticsGrouping();
		
		TypeName tn = null;
		for (TypeName atn : tns)
		{
			if(atn.equals(theTN))
				tn = atn;
		}
		if(tn != null)
		{
			tns.remove(tn);
			JEXStatics.jexManager.setStatisticsGrouping(tns);
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == reset)
		{
			JEXStatics.jexManager.setStatisticsFilterSet(null);
			JEXStatics.jexManager.setStatisticsGrouping(null);
		}
	}
}
