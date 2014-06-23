package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.Pair;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import tables.DimensionMap;
import Database.DBObjects.JEXEntry;
import Database.Definition.Experiment;

public class DistributorArray implements ActionListener {
	
	public FileDropCellController[][] cellControllers;
	public int rows;
	public int cols;
	public JPanel pane = new JPanel();
	public Experiment currentTray;
	public HashMap<Point,Boolean> selectionArray;
	
	JEXDistributionPanelController parentController;
	JButton importButton;
	JButton cancelButton;
	
	public Point pressed, released;
	
	public DistributorArray(JEXDistributionPanelController parentController)
	{
		this.parentController = parentController;
		
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "selectionChanged", (Class[]) null);
		
		// files = new HashMap<Point,TreeMap<DimensionMap,String>>();
		this.rows = 0;
		this.cols = 0;
		
		importButton = new JButton("Import");
		importButton.addActionListener(this);
		
		cancelButton = new JButton("Clear");
		cancelButton.addActionListener(this);
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	// GETTERS AND SETTERS
	
	public JPanel panel()
	{
		return pane;
	}
	
	public void setRowsAndCols(int rows, int cols)
	{
		this.rows = cols;
		this.cols = rows;
		this.rebuild();
	}
	
	public void setBlank()
	{
		this.setRowsAndCols(0, 0);
		this.rebuild();
	}
	
	public int getNumberColumns()
	{
		return cols;
	}
	
	public int getNumberRows()
	{
		return rows;
	}
	
	public HashMap<Point,Boolean> getSelectionArray()
	{
		return selectionArray;
	}
	
	public void setTray(Experiment currentTray)
	{
		this.currentTray = currentTray;
		if(cellControllers == null)
			rebuild();
		refreshGUI();
	}
	
	public void setFileArray(HashMap<Point,Vector<Pair<DimensionMap,String>>> files)
	{
		// this.files = files;
		if(cellControllers == null)
			rebuild();
		refreshGUI();
	}
	
	public HashMap<Point,Boolean> getSelectedIndeces()
	{
		return selectionArray;
	}
	
	public void setSelectionForCellAtIndexXY(int x, int y, boolean selected)
	{
		// Get the tray
		if(this.currentTray == null)
			return;
		
		// Get the column
		TreeMap<Integer,JEXEntry> columns = this.currentTray.get(x);
		if(columns == null)
			return;
		
		// Get the entry
		JEXEntry entry = columns.get(y);
		
		// Set the selection
		if(selected)
			JEXStatics.jexManager.addEntryToSelection(entry);
		else
			JEXStatics.jexManager.removeEntryFromSelection(entry);
	}
	
	public void addFile(int row, int column, File f)
	{
		if(this.parentController.files == null)
		{
			this.parentController.files = new HashMap<Point,Vector<Pair<DimensionMap,String>>>();
		}
		
		// Get the current file list
		Vector<Pair<DimensionMap,String>> fileList = this.parentController.files.get(new Point(row, column));
		if(fileList == null)
		{
			fileList = new Vector<Pair<DimensionMap,String>>();
			this.parentController.files.put(new Point(row, column), fileList);
		}
		
		// make a new dimension map
		DimensionMap map = new DimensionMap();
		map.put(this.parentController.fileController.getManualDimensionName(), "" + fileList.size());
		
		// Put the file in the list
		fileList.add(new Pair<DimensionMap,String>(map, f.getAbsolutePath()));
		
		// Refresh the gui
		refreshGUI();
	}
	
	// GUI BUILDING
	
	public void rebuild()
	{
		cellControllers = new FileDropCellController[rows][cols];
		
		// Loop through the rows of the array
		for (int i = 0; i < rows; i++)
		{
			// Loop through the columns
			for (int j = 0; j < cols; j++)
			{
				// Make the array cell controller
				FileDropCellController cellController = new FileDropCellController(this);
				cellController.setRowAndColumn(i, j);
				
				// Place it in the array
				cellControllers[i][j] = cellController;
			}
		}
		
		refreshGUI();
	}
	
	public void refreshGUI()
	{
		// Create variables if required
		if(selectionArray == null)
			selectionArray = new HashMap<Point,Boolean>();
		
		// Remove old layout
		pane.removeAll();
		
		// If an array is not selected
		if(rows == 0 || cols == 0)
		{
			JLabel label = new JLabel("No array selected... Use the left panel to browse until an array is selected");
			label.setForeground(Color.white);
			pane.setBackground(DisplayStatics.background);
			pane.setLayout(new MigLayout("flowy, ins 5, center, center", "[fill,grow]", "20[fill,grow]10"));
			pane.add(label, "width 100%, grow");
			
			// repain and revalidate
			pane.invalidate();
			pane.validate();
			pane.repaint();
			
			return;
		}
		
		// Create the layout and set the gui parameters of this panel
		int percentWidth = (int) (100.0 / (double) rows);
		String columnStr = "5";
		for (int i = 0; i < rows; i++)
		{
			columnStr = columnStr + "[]5";
		}
		
		int percentHeight = (int) (100.0 / (double) cols);
		String rowStr = "5";
		for (int i = 0; i < cols; i++)
		{
			rowStr = rowStr + "[]5";
		}
		
		// Set the mig layout
		JPanel arrayPanel = new JPanel();
		arrayPanel.setBackground(DisplayStatics.background);
		arrayPanel.setLayout(new MigLayout("ins 0", columnStr, rowStr));
		
		// add the array panels to this panel
		if(this.parentController.files == null)
			this.parentController.files = new HashMap<Point,Vector<Pair<DimensionMap,String>>>();
		for (int i = 0; i < rows; i++)
		{
			for (int j = 0; j < cols; j++)
			{
				// set the selection status
				Boolean selected = selectionArray.get(new Point(i, j));
				if(selected != null && selected)
					this.cellControllers[i][j].setSelected(true);
				else
					this.cellControllers[i][j].setSelected(false);
				
				// Set the files to display
				Vector<Pair<DimensionMap,String>> theFiles = this.parentController.files.get(new Point(i, j));
				this.cellControllers[i][j].setFiles(theFiles);
				
				// Get the cell panel
				JPanel cellpane = this.cellControllers[i][j].panel();
				
				// add to the panel
				arrayPanel.add(cellpane, "Cell " + i + " " + j + " 0,width " + percentWidth + "%,height " + percentHeight + "%");
			}
		}
		
		// Redo the new panel
		pane.setBackground(DisplayStatics.background);
		pane.setLayout(new MigLayout("ins 5, center, center", "[fill,grow]", "10[fill,grow]0[30]10"));
		pane.add(arrayPanel, "span 2, width 100%, grow, wrap");
		pane.add(importButton, "growx");
		pane.add(cancelButton, "growx");
		
		// repain and revalidate
		pane.invalidate();
		pane.validate();
		pane.repaint();
	}
	
	private void cancelDistribution()
	{
		// Clear changes
		this.parentController.clear();
	}
	
	// ////////////////////////////////////
	// /////// Signaling Methods //////////
	// ////////////////////////////////////
	
	public void selectionChanged()
	{
		if(currentTray == null)
			selectionArray = new HashMap<Point,Boolean>();
		else
		{
			selectionArray = new HashMap<Point,Boolean>();
			for (Integer x : this.currentTray.keySet())
			{
				TreeMap<Integer,JEXEntry> columns = this.currentTray.get(x);
				for (Integer y : this.currentTray.get(x).keySet())
				{
					JEXEntry entry = columns.get(y);
					if(JEXStatics.jexManager.isSelected(entry))
					{
						selectionArray.put(new Point(x, y), true);
					}
				}
			}
		}
		
		refreshGUI();
	}
	
	public void navigationChanged()
	{
		if(parentController.curTray != null)
		{
			int width = 0, height = 0;
			for (Integer x : parentController.curTray.keySet())
			{
				if((x + 1) > width)
				{
					width = x + 1;
				}
				for (Integer y : parentController.curTray.get(x).keySet())
				{
					if((y + 1) > height)
					{
						height = y + 1;
					}
				}
			}
			if(!(width == 0 || height == 0))
			{
				this.setRowsAndCols(height, width);
				this.setTray(parentController.curTray);
				this.selectionChanged();
				return;
			}
		}
		
		// Else set labeler blank
		this.setBlank();
	}
	
	// /////////////////////////////////////////////////////////
	// ////////////// handle MouseEvents ///////////////////////
	// /////////////////////////////////////////////////////////
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == importButton)
		{
			this.parentController.createObjects();
			this.cancelDistribution();
		}
		if(e.getSource() == cancelButton)
		{
			cancelDistribution();
		}
	}
}
