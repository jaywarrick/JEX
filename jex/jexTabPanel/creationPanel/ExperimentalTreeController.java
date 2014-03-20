package jex.jexTabPanel.creationPanel;

import java.awt.Color;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.Definition.Experiment;
import Database.Definition.HierarchyLevel;

public class ExperimentalTreeController {
	
	public static String DATA_EDIT = "DATA_EDIT";
	public static String DATA_OPEN = "DATA_OPEN";
	public static int TREE_MODE_CREATION = 1;
	public static int TREE_MODE_VIEW = 2;
	public static Color defaultBackground = DisplayStatics.lightBackground;
	public static Color selectedBackground = DisplayStatics.selectedLightBBackground;
	public static String[] SORT_OPTIONS = new String[] { "Dataset name", "Creation date", "Dataset Info" };
	public static String sortBy = SORT_OPTIONS[0];
	
	// Model
	public TreeMap<String,Experiment> experiments;
	public TreeSet<ExperimentalTreeExperimentController> cellControllers;
	private HierarchyLevel currentlyViewed = null;
	private int treeMode = TREE_MODE_VIEW;
	
	// GUI
	private ExperimentalTree panel;
	
	public ExperimentalTreeController()
	{
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.EXPERIMENTTREE_CHANGE, this, "rebuildModel", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
		
		rebuildModel();
	}
	
	public void rebuildModel()
	{
		Logs.log("Rebuilding Experimental tree model", 1, this);
		
		// Get the new model
		experiments = JEXStatics.jexManager.getExperimentTree();
		experiments = (experiments == null) ? new TreeMap<String,Experiment>() : experiments;
		
		// Rebuild the controllers
		cellControllers = new TreeSet<ExperimentalTreeExperimentController>();
		for (Experiment exp : experiments.values())
		{
			// Create a new experiment controller
			ExperimentalTreeExperimentController cellController = new ExperimentalTreeExperimentController(this);
			cellController.setExperiment(exp);
			
			// Link it to a signaling center
			SSCenter.defaultCenter().connect(cellController, ExperimentalTreeController.DATA_EDIT, this, "editData", new Class[] { HierarchyLevel.class });
			SSCenter.defaultCenter().connect(cellController, ExperimentalTreeController.DATA_OPEN, this, "openData", new Class[] { HierarchyLevel.class });
			
			// Add it to the list of controllers
			cellControllers.add(cellController);
		}
		
		// Set the expansion of experiments
		navigationChanged();
	}
	
	public HierarchyLevel getCurrentlyViewed()
	{
		return this.currentlyViewed;
	}
	
	public int treeMode()
	{
		return this.treeMode;
	}
	
	public void setTreeMode(int treeMode)
	{
		this.treeMode = treeMode;
		rebuildModel();
	}
	
	// /
	// / ACTION
	// /
	
	public void openExperiment(Experiment exp)
	{
		String expName = (exp == null) ? "NULL" : exp.getName();
		Logs.log("Emitting signal for opening an experiment - " + expName, 1, this);
		if(exp == null)
		{
			JEXStatics.jexManager.setViewedExperiment(null);
		}
		else
		{
			JEXStatics.jexManager.setViewedExperiment(exp.getName());
		}
	}
	
	// /
	// / SIGNALS
	// /
	
	public void navigationChanged()
	{
		Logs.log("Recieved navigation changed signal", 1, this);
		
		// Get the viewed experiment
		String expViewed = JEXStatics.jexManager.getViewedExperiment();
		String arrayViewed = JEXStatics.jexManager.getArrayViewed();
		
		// If an array is viewed don't care about this
		if(expViewed != null && arrayViewed != null)
			return;
		
		panel().rebuild();
	}
	
	public void editData(HierarchyLevel currentlyViewed)
	{
		SSCenter.defaultCenter().emit(this, DATA_EDIT, new Object[] { currentlyViewed });
	}
	
	public void openData(HierarchyLevel hLevel)
	{
		if(hLevel instanceof Experiment)
		{
			Logs.log("Displaying array named - " + hLevel.getName(), 1, this);
			JEXStatics.jexManager.setViewedExperiment(hLevel.getName());
			JEXStatics.main.displayViewPane();
		}
	}
	
	// /
	// / GETTERS AND SETTERS
	// /
	
	public ExperimentalTree panel()
	{
		if(panel == null)
		{
			panel = new ExperimentalTree();
		}
		return panel;
	}
	
	public void setSortOption(String sortOption)
	{
		sortBy = sortOption;
	}
	
	class ExperimentalTree extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		ExperimentalTree()
		{
			rebuild();
		}
		
		public void rebuild()
		{
			Logs.log("Rebuilding Experimental tree panel", 1, this);
			
			// remove all
			this.removeAll();
			
			// Set the mig layout
			this.setLayout(new MigLayout("flowy, ins 0", "10[fill,grow]10", ""));
			this.setBackground(DisplayStatics.background);
			
			// Add all the components
			for (ExperimentalTreeExperimentController cellController : cellControllers)
			{
				this.add(cellController.panel(), "gapy 5");
			}
			
			// refresh display
			this.revalidate();
			this.repaint();
		}
		
	}
	
}
