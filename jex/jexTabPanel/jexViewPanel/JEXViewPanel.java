package jex.jexTabPanel.jexViewPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.arrayView.ArrayViewController;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.Definition.Experiment;
import Database.Definition.HierarchyLevel;

public class JEXViewPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private ArrayViewController arrayPaneController;
	
	JEXViewPanel()
	{
		initialize();
		
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(arrayPaneController);
		SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		// make a controller
		Logs.log("Initializing new controller", 1, this);
		
		navigationChanged();
	}
	
	public void navigationChanged()
	{
		Logs.log("Navigation changed, displaying update", 1, this);
		
		// Get the viewed experiment
		String expViewed = JEXStatics.jexManager.getViewedExperiment();
		if(expViewed == null)
		{
			openArray(null);
			return;
		}
		// String arrayViewed = JEXStatics.jexManager.getArrayViewed();
		TreeMap<String,Experiment> tree = JEXStatics.jexManager.getExperimentTree();
		if(tree == null)
		{
			openArray(null);
			return;
		}
		Experiment exp = tree.get(expViewed);
		openArray(exp);
	}
	
	public void openArray(HierarchyLevel tray)
	{
		if(tray == null)
		{
			displayArray(null);
		}
		if(tray instanceof Experiment)
		{
			displayArray((Experiment) tray);
		}
	}
	
	private void displayArray(Experiment tray)
	{
		Logs.log("Displaying experimental array", 1, this);
		
		// Remove all
		this.removeAll();
		
		// Set graphics
		this.setLayout(new BorderLayout());
		this.setBackground(DisplayStatics.background);
		
		if(tray != null)
		{
			// Make the array controller
			arrayPaneController = new ArrayViewController();
			arrayPaneController.setArray(tray);
			
			// Place the components in this panel
			this.add(arrayPaneController.panel(), BorderLayout.CENTER);
		}
		else
		{
			JLabel label = new JLabel("No array selected... Use the left panel to browse until an array is selected");
			label.setForeground(Color.white);
			JPanel pane = new JPanel();
			pane.setBackground(DisplayStatics.background);
			pane.setLayout(new MigLayout("flowy, ins 5, center, center", "[fill,grow]", "20[fill,grow]10"));
			pane.add(label, "width 100%, grow");
			this.add(pane, BorderLayout.CENTER);
		}
		
		// REvalidate
		this.revalidate();
		this.repaint();
	}
	
}