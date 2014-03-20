package plugins.selector;

import image.roi.PointList;

import java.awt.Point;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.DBObjects.JEXEntry;
import Database.Definition.Experiment;

public class QuickSelector {
	
	private JPanel panel;
	// private QuickSelectorHeaders headers;
	private QuickSelectorList list;
	private QuickSelectorArray array;
	private TreeMap<String,Experiment> experiments;
	public Experiment curExpt;
	// public Tray curTray;
	public List<String> listNames;
	
	public QuickSelector()
	{
		this.panel = new JPanel(new MigLayout("left,flowy,ins 5", "[fill,grow]", "[fill, 100]5[fill,grow]"));
		this.panel.setBackground(DisplayStatics.lightBackground);
		// this.panel.setBackground(Color.RED);
		// this.headers = new QuickSelectorHeaders();
		// this.headers.panel().setBackground(Color.BLUE);
		this.list = new QuickSelectorList();
		// this.list.panel().setBackground(Color.YELLOW);
		this.array = new QuickSelectorArray();
		this.experiments = JEXStatics.jexManager.getExperimentTree();
		this.curExpt = null;
		// this.curTray = null;
		this.listNames = new Vector<String>();
		this.connect();
		this.initialize();
		this.rebuildDataSetList();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void connect()
	{
		SSCenter.defaultCenter().connect(this.list, QuickSelectorList.SIG_ListSelectionChanged_NULL, this, "chooseExptSelection", (Class[]) null);
		// SSCenter.defaultCenter().connect(this.list, QuickSelectorList.SIG_ListSelectionChanged_NULL, this, "rebuildEntryArray", (Class[]) null);
		SSCenter.defaultCenter().connect(this.array, QuickSelectorArray.SIG_SelectionChanged_NULL, this, "chooseArraySelection", (Class[]) null);
		
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "rebuildEntryArray", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.EXPERIMENTTREE_CHANGE, this, "rebuildDataSetList", (Class[]) null);
	}
	
	private void initialize()
	{
		this.panel.add(this.list.panel(), "grow");
		this.panel.add(this.array.panel(), "grow");
	}
	
	public void chooseExptSelection()
	{
		// Use gui information to change selection settings in JEX
		
		// Grab the current selection
		String selectedExptName = this.list.getSelection();
		
		// Send command to the jexmanager to select the experiment
		JEXStatics.jexManager.setViewedExperiment(selectedExptName);
	}
	
	public void chooseArraySelection()
	{
		// Use the gui information to changes selection information in JEX
		if(this.curExpt != null)
		{
			PointList pl = this.array.getSelected();
			TreeSet<JEXEntry> selectedEntries = new TreeSet<JEXEntry>();
			for (Point p : pl)
			{
				selectedEntries.add(this.curExpt.get(p.x).get(p.y));
			}
			TreeSet<JEXEntry> currentSelection = JEXStatics.jexManager.getSelectedEntries();
			for (JEXEntry e : currentSelection)
			{
				String tempExpName = e.getEntryExperiment();
				if(!tempExpName.equals(this.curExpt.expName))
				{
					selectedEntries.add(e);
				}
			}
			JEXStatics.jexManager.setSelectedEntries(selectedEntries);
		}
	}
	
	public void rebuildDataSetList()
	{
		/*
		 * There are currently two scenarios.
		 * 
		 * 1) Initialization: We want to choose the first experiment in the potential list of experiments, if possible.
		 * 
		 * 2) Experimental Tree change : i.e. we add a data set, in which case we want to keep the current selection
		 */
		
		// Initialize local variables
		this.experiments = JEXStatics.jexManager.getExperimentTree();
		String viewedExp = JEXStatics.jexManager.getViewedExperiment();
		if(viewedExp == null)
		{
			this.curExpt = null;
		}
		else
		{
			this.curExpt = this.experiments.get(viewedExp);
		}
		
		// Build the list
		this.listNames.clear();
		if(this.experiments != null)
		{
			for (String exptName : this.experiments.keySet())
			{
				this.listNames.add(exptName);
			}
		}
		
		// Set the list
		this.list.setList(this.listNames);
		if(this.curExpt == null)
		{
			if(this.listNames.size() > 0)
			{
				this.list.setSelection(this.listNames.get(0));
				this.chooseExptSelection(); // Actually have to induce expt selection change in all of JEX (results in rebuilding array selector)
			}
		}
		else
		{
			// Then don't change the current selection
			// We probably don't have to do anything but we are just making sure the gui matches the internal information
			this.list.setSelection(this.curExpt.getName());
			this.rebuildEntryArray(); // Just make sure the array matches the expeirment we have selected in the list
			this.panel.revalidate();
			this.panel.repaint();
		}
		
	}
	
	public void rebuildEntryArray()
	{
		// Draw the array selector for the experiments in this.curExp
		if(this.curExpt == null)
		{
			this.array.setRowsAndCols(0, 0);
		}
		else
		{
			int width = 0, height = 0;
			for (Integer x : this.curExpt.keySet())
			{
				if((x + 1) > width)
				{
					width = x + 1;
				}
				for (Integer y : this.curExpt.get(x).keySet())
				{
					if((y + 1) > height)
					{
						height = y + 1;
					}
				}
			}
			this.array.setRowsAndCols(height, width);
			this.array.deselectAll();
			for (JEXEntry e : this.curExpt.entries)
			{
				if(JEXStatics.jexManager.isSelected(e))
				{
					this.array._select(new Point(e.getTrayX(), e.getTrayY()));
				}
			}
		}
		this.array.panel().repaint();
	}
	
	public void navigationChanged()
	{
		// 1) get the current experiment and record it
		this.experiments = JEXStatics.jexManager.getExperimentTree();
		String viewedExp = JEXStatics.jexManager.getViewedExperiment();
		if(viewedExp == null)
		{
			this.curExpt = null;
		}
		else
		{
			this.curExpt = this.experiments.get(viewedExp);
		}
		
		// Silently change the list selection to the current experiment
		this.list.setSelection(viewedExp);
		
		// Build the array selector to match the current experiment
		this.rebuildEntryArray();
	}
	
}
