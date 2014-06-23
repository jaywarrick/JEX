package jex.arrayView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;

import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.Definition.Experiment;
import Database.Definition.HierarchyLevel;

public class ArrayViewController {
	
	// Statics signals
	public static String CLOSE_ARRAY = "CLOSE_ARRAY";
	
	// Model variables
	private ArrayCellController[][] arrayCellControllers;
	private Experiment tray;
	private int width;
	private int height;
	
	// Gui links
	private ArrayPanel pane;
	
	public ArrayViewController()
	{
		// Link to the signal from a database list change
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.VIEWEDENTRY, this, "entryViewedChanged", (Class[]) null);
	}
	
	public void setArray(Experiment tray)
	{
		this.tray = tray;
		rebuildControllerArray();
		refreshGUI();
	}
	
	/**
	 * Build a 2D array of simple array cell controllers
	 */
	private void rebuildControllerArray()
	{
		// Get the list of hierarchies to display
		TreeMap<String,TreeMap<String,HierarchyLevel>> hArray = tray.getSublevelArray();
		width = hArray.size();
		height = 1;
		for (TreeMap<String,HierarchyLevel> row : hArray.values())
		{
			height = Math.max(height, row.size());
		}
		
		// Make a 2D array of panel controllers
		arrayCellControllers = new ArrayCellController[height][width];
		Logs.log("Creating a 2D array of controllers", 1, this);
		
		// Loop through the rows of the array
		for (String xString : hArray.keySet())
		{
			// Get the index of the row
			int x = new Integer(xString);
			
			// Get the tow
			TreeMap<String,HierarchyLevel> col = hArray.get(xString);
			
			// Loop through the columns
			for (String yString : col.keySet())
			{
				// Get the index of the column
				int y = new Integer(yString);
				
				// Get the cell at row x and column y
				HierarchyLevel cell = col.get(yString);
				
				// Make the array cell controller
				ArrayCellController cellController = new ArrayCellController();
				
				// Set its required variables and rebuild it
				cellController.setCell(cell);
				cellController.rebuild();
				
				// Place it in the array
				arrayCellControllers[y][x] = cellController;
			}
		}
	}
	
	/**
	 * Refresh the gui
	 */
	public void refreshGUI()
	{
		if(pane == null)
		{
			pane = this.panel();
		}
		pane.rebuild();
	}
	
	//
	// SINGALS
	//
	
	/**
	 * Call the methods required to rebuild the array form the new viewed data
	 */
	public void arrayChanged()
	{
		Logs.log("Updating the arrayed view", 0, this);
		HierarchyLevel viewedLevel = JEXStatics.jexManager.getViewedHierarchyLevel();
		if(viewedLevel instanceof Experiment)
		{
			setArray((Experiment) viewedLevel);
		}
		rebuildControllerArray();
		refreshGUI();
	}
	
	/**
	 * 
	 */
	public void entryViewedChanged()
	{
		Logs.log("Updating the Entry viewed", 0, this);
		rebuildControllerArray();
		refreshGUI();
	}
	
	//
	// GET THE PANEL
	//
	
	/**
	 * Returns the panel to be displayed
	 */
	public ArrayPanel panel()
	{
		if(pane == null)
		{
			pane = new ArrayPanel();
		}
		else
		{
			pane.rebuild();
		}
		return pane;
	}
	
	/**
	 * Returns the height of the array
	 * 
	 * @return
	 */
	public int height()
	{
		return height;
	}
	
	/**
	 * Returns the width of the array
	 * 
	 * @return
	 */
	public int width()
	{
		return width;
	}
	
	class ArrayPanel extends JPanel implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
		ArrayPanel()
		{
			rebuild();
		}
		
		public void rebuild()
		{
			// Remove old layout
			this.removeAll();
			
			// Create the layout and set the gui parameters of this panel
			int percentWidth = (int) (100.0 / (double) width);
			String columnStr = "5";
			for (int i = 0; i < width; i++)
			{
				columnStr = columnStr + "[]5";
			}
			
			int percentHeight = (int) (100.0 / (double) height);
			String rowStr = "5";
			for (int i = 0; i < width; i++)
			{
				rowStr = rowStr + "[]5";
			}
			
			// Set the mig layout
			this.setBackground(DisplayStatics.background);
			this.setLayout(new MigLayout("ins 0", columnStr, rowStr));
			
			// add the array panels to this panel
			for (int i = 0; i < width; i++)
			{
				for (int j = 0; j < height; j++)
				{
					// Get the cell panel
					JPanel pane = arrayCellControllers[j][i].panel();
					this.add(pane, "Cell " + i + " " + j + " 0,width " + percentWidth + "%,height " + percentHeight + "%");
				}
			}
			
			// repain and revalidate
			this.invalidate();
			this.validate();
			this.repaint();
		}
		
		public void actionPerformed(ActionEvent e)
		{}
	}
}
