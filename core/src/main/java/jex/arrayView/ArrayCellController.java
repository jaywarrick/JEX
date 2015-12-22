package jex.arrayView;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.HierarchyLevel;
import Database.Definition.Type;
import Database.Definition.TypeName;

import java.awt.Color;
import java.util.Set;
import java.util.TreeMap;

import jex.JEXManager;
import jex.dataView.JEXDataPanelController;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import signals.SSCenter;

public class ArrayCellController {
	
	// Variables defining of the cell
	public HierarchyLevel cell;
	public boolean viewed = false;
	public boolean valid = true;
	public boolean selected = false;
	
	// Other variables
	public String name = "";
	
	// GUI link for quicker refreshing of the display
	public Color labelColor = DisplayStatics.lightBackground;
	public Color borderColor = null;
	public ArrayCellPanel pane;
	public JEXDataPanelController dataView;
	
	public ArrayCellController()
	{
		// Link to the signal from a database list change
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTEDLABEL, this, "labelSelectionChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTEDOBJ, this, "objectSelectionChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.ENTRYVALID, this, "entryValidityChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "selectionChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.ARRAYDATADISPLAY, this, "dataDisplayChange", (Class[]) null);
		
	}
	
	//
	// PUBLIC GETTERS AND SETTERS
	//
	
	/**
	 * Returns the Hierarchy level describing the cell
	 * 
	 * @return
	 */
	public HierarchyLevel cell()
	{
		return cell;
	}
	
	/**
	 * Sets the hierarchy level describing the cell
	 * 
	 * @param cell
	 */
	public void setCell(HierarchyLevel cell)
	{
		this.cell = cell;
	}
	
	//
	// PRIVATE GETTERS AND SETTERS
	//
	
	/**
	 * Returns the title of the cell
	 * 
	 * @return
	 */
	public String name()
	{
		return name;
	}
	
	/**
	 * Sets the title of the cell
	 * 
	 * @param name
	 */
	private void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Returns the entries contained in the cell
	 * 
	 * @return
	 */
	public Set<JEXEntry> entries()
	{
		return cell.getEntries();
	}
	
	//
	// CONTROLLER REFRESHING METHODS
	//
	
	/**
	 * Rebuilds the entry list and the title of the cell
	 */
	public void rebuild()
	{
		// Get the title of the cell from the CELL
		setName(cell.getName());
		
		// Get a representative entry from this cell (usually the first)
		JEXEntry entry = null;
		if(cell.getEntries() != null && cell.getEntries().size() > 0)
			entry = cell.getEntries().iterator().next();
		
		// Is the cell viewed?
		viewed = false;
		if(entry != null)
		{
			JEXEntry viewedEntry = JEXStatics.jexManager.getViewedEntry();
			if(entry.equals(viewedEntry))
				viewed = true;
			else
				viewed = false;
		}
		
		// Is the cell valid?
		TreeMap<Type,TreeMap<String,JEXData>> entrymap = entry.getDataList();
		TreeMap<String,JEXData> labelmap = (entrymap == null) ? new TreeMap<String,JEXData>() : entrymap.get(JEXData.LABEL);
		if(labelmap != null)
		{
			JEXData validityLabelData = labelmap.get(JEXEntry.VALID);
			if(validityLabelData == null || validityLabelData.getDictionaryValue().equals("true"))
				valid = true;
			else
				valid = false;
		}
		
		// Get the label viewed
		TypeName label = JEXStatics.jexManager.getSelectedLabel();
		if(label != null && label.getName() != null && labelmap != null)
		{
			JEXData labelValueData = labelmap.get(label.getName());
			// If there is a label of the same name as the one that is selected
			// then change the background
			if(labelValueData != null)
				labelColor = JEXStatics.labelColorCode.getColorForLabel(label.getName(), labelValueData.getDictionaryValue());
		}
		
		// Get the selection status
		selected = JEXStatics.jexManager.isAllSelected(cell.getEntries());
		if(selected)
		{
			//Logs.log("Setting border color to selected color", 2, this);
			borderColor = DisplayStatics.selectedDividerColor;
			borderColor = DisplayStatics.selectedButtonBorder;
		}
		else
		{
			//Logs.log("Setting border color to unselected color", 2, this);
			borderColor = DisplayStatics.dividerColor;
		}
		
		// Get the selected object
		TypeName object = JEXStatics.jexManager.getSelectedObject();
		
		// If the object is null create a null data view
		if(object == null)
			dataView = null;
		else
		{
			// Get the data that from the entry
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(object, entry);
			
			// If no data is present create a null entry
			if(data == null)
				dataView = null;
			else
			{
				// Else create a data viewer controller
				dataView = new JEXDataPanelController();
				dataView.setData(data);
				dataView.setEntry(entry);
			}
		}
		
		// Rebuild GUI
		rebuildGUI();
	}
	
	/**
	 * Change in the label selection
	 */
	public void labelSelectionChanged()
	{
		// Get a representative entry from this cell (usually the first)
		JEXEntry entry = null;
		if(cell.getEntries() != null && cell.getEntries().size() > 0)
			entry = cell.getEntries().iterator().next();
		
		// Get the label viewed
		TreeMap<Type,TreeMap<String,JEXData>> entrymap = entry.getDataList();
		TreeMap<String,JEXData> labelmap = (entrymap == null) ? new TreeMap<String,JEXData>() : entrymap.get(JEXData.LABEL);
		TypeName label = JEXStatics.jexManager.getSelectedLabel();
		if(label != null && label.getName() != null && labelmap != null)
		{
			JEXData labeldata = labelmap.get(label.getName());
			// If there is a label of the same name as the one that is selected
			// then change the background
			if(labeldata != null)
				labelColor = JEXStatics.labelColorCode.getColorForLabel(label.getName(), labeldata.getDictionaryValue());
			else
				labelColor = DisplayStatics.lightBackground;
		}
		else
			labelColor = DisplayStatics.lightBackground;
		
		// Logs.log("Label Selection changed, new color for cell "+entry.getTrayX()+"."+entry.getTrayY()+" found is "+labelColor,
		// 1, this);
		
		// refresh the gui
		rebuildGUI();
	}
	
	/**
	 * Change in viewed entry
	 */
	public void objectSelectionChanged()
	{
		// Get the selected object
		TypeName object = JEXStatics.jexManager.getSelectedObject();
		
		// If the object is null create a null data view
		if(object == null)
		{
			dataView = null;
			rebuildGUI();
			return;
		}
		
		// Get a representative entry from this cell (usually the first)
		JEXEntry entry = null;
		if(cell.getEntries() != null && cell.getEntries().size() > 0)
			entry = cell.getEntries().first();
		
		// Get the data that from the entry
		JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(object, entry);
		
		// If no data is present create a null entry
		if(data == null)
		{
			dataView = null;
			rebuildGUI();
			return;
		}
		
		// Else create a data viewer controller
		dataView = new JEXDataPanelController();
		dataView.setData(data);
		dataView.setEntry(entry);
		
		// Rebuild GUI
		rebuildGUI();
	}
	
	/**
	 * Change in the entry validity flag
	 */
	public void entryValidityChanged()
	{
		// Get a representative entry from this cell (usually the first)
		JEXEntry entry = null;
		if(cell.getEntries() != null && cell.getEntries().size() > 0)
			entry = cell.getEntries().iterator().next();
		
		// Is the cell valid?
		valid = JEXStatics.jexManager.getEntryValidity(entry);
		
		// refresh the gui
		rebuildGUI();
	}
	
	/**
	 * Change in entry selection list
	 */
	public void selectionChanged()
	{
		selected = JEXStatics.jexManager.isAllSelected(cell.getEntries());
		if(selected)
		{
			Logs.log("Setting border color to selected color", 2, this);
			borderColor = DisplayStatics.selectedDividerColor;
			borderColor = DisplayStatics.selectedButtonBorder;
		}
		else
		{
			Logs.log("Setting border color to unselected color", 2, this);
			borderColor = DisplayStatics.dividerColor;
		}
		
		// refresh the gui
		rebuildGUI();
	}
	
	/**
	 * Change in the flag controlling whether data should be displayed in the array
	 */
	public void dataDisplayChange()
	{
		rebuildGUI();
	}
	
	//
	// GUI METHODS AND CLASSES
	//
	
	/**
	 * Rebuilds the GUI for the panel
	 */
	public void rebuildGUI()
	{
		if(pane == null)
		{
			pane = this.panel();
		}
		pane.rebuild();
	}
	
	/**
	 * Returns the panel to be displayed
	 * 
	 * @return
	 */
	public ArrayCellPanel panel()
	{
		if(pane == null)
		{
			if(cell.getEntries() == null)
				pane = new ArrayCellNULL(this);
			else
				pane = new ArrayCellPanelSINGLE(this);
		}
		else
		{
			pane.rebuild();
		}
		return pane;
	}
	
}
