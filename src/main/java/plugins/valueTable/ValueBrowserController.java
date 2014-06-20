package plugins.valueTable;

import java.util.TreeSet;
import java.util.Vector;

import jex.JEXManager;
import jex.statics.JEXStatics;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import signals.SSCenter;
import tables.DimTable;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.TypeName;

public class ValueBrowserController implements PlugInController {
	
	// Entries and data
	private Vector<JEXEntry> entries;
	private TypeName typeName = null;
	private DataBrowser dataBrowser;
	
	// GUI
	private ValueBrowser dialog;
	private ValueTable valueTable;
	
	// Variables
	private boolean liveScroll = true;
	private boolean scrolling = false;
	
	public ValueBrowserController(Vector<JEXEntry> entries, TypeName objectTN)
	{
		this.entries = entries;
		this.typeName = objectTN;
		
		initialize();
	}
	
	public ValueBrowserController(TreeSet<JEXEntry> entries, TypeName objectTN)
	{
		Vector<JEXEntry> ent = new Vector<JEXEntry>();
		for (JEXEntry j : entries)
		{
			ent.add(j);
		}
		this.entries = ent;
		this.typeName = objectTN;
		
		initialize();
	}
	
	public ValueBrowserController()
	{
		initialize();
	}
	
	private void initialize()
	{
		this.dataBrowser = new DataBrowser();
		this.dataBrowser.setScrollBars(1, null);
		
		// Make appropriate connections
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.AVAILABLEOBJ, this, "viewedEntryChanged", (Class[]) null);
		
		// Make the dialog
		dialog = new ValueBrowser(this);
		
		// Make appropriate connections
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_EntryChanged_NULL, this, "entryChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_EntryScrollFinished_NULL, this, "entryScrollFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
		
		// display stuff
		viewedEntryChanged();
	}
	
	public void setDBSelection(TreeSet<JEXEntry> entries, TypeName tn)
	{
		this.entries = new Vector<JEXEntry>(entries);
		this.typeName = tn;
		
		viewedEntryChanged();
		// dialog.setDBSelection();
	}
	
	public ValueTable getTable()
	{
		return this.valueTable;
	}
	
	public Vector<JEXEntry> getEntries()
	{
		return entries;
	}
	
	public TypeName getTypeName()
	{
		return typeName;
	}
	
	public DataBrowser getDataBrowser()
	{
		return this.dataBrowser;
	}
	
	public JEXEntry currentEntry()
	{
		if(this.entries == null || this.dataBrowser == null)
			return null;
		
		int entryIndex = this.dataBrowser.currentEntry();
		return this.entries.get(entryIndex);
	}
	
	public DimTable getValueDimTable(TypeName tn, JEXEntry e)
	{
		DimTable ret = null;
		JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, e);
		if(data != null)
			ret = data.getDimTable();
		else
			ret = null;
		return ret;
	}
	
	// //////////////////////////////////////
	// //////////Reactions //////////////
	// //////////////////////////////////////
	
	public void viewedEntryChanged()
	{
		// Make the value table
		valueTable = new ValueTable();
		
		// get the entry
		JEXEntry entry = this.currentEntry();
		if(entry == null)
			valueTable.setDataToView(null);
		
		// get the type name of the value to display
		if(this.typeName == null || !this.typeName.getType().equals(JEXData.VALUE))
			valueTable.setDataToView(null);
		
		// Get the jexdata
		JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.typeName, entry);
		if(data == null)
			valueTable.setDataToView(null);
		
		// Make the value table
		valueTable.setDataToView(data);
		valueTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		valueTable.setCellSelectionEnabled(true);
		
		this.dialog.setDBSelection();
	}
	
	public void entryChanged()
	{
		JEXEntry entry = this.currentEntry();
		DimTable valueDimTable = this.getValueDimTable(this.getTypeName(), entry);
		this.dataBrowser.storeDimState();
		this.dataBrowser.setImageDimTable(valueDimTable);
		this.dataBrowser.recallDimState(); // set what we can
		this.dataBrowser.repaint();
		this.valueChanged();
	}
	
	public void entryScrollFinished()
	{
		this.scrolling = false;
		this.entryChanged();
	}
	
	public void valueDimChanged()
	{
		if(scrolling && !liveScroll)
		{
			return;
		}
		this.valueChanged();
	}
	
	public void imageScrollFinished()
	{
		this.scrolling = false;
		this.valueDimChanged();
	}
	
	public void scrollStarted()
	{
		this.scrolling = true;
	}
	
	public void valueChanged() // Called each time the entry, DimensionMap;
	{
		// Load the appropriate image according to the DimensionMap and entry
		JEXData valueData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.getTypeName(), this.currentEntry());
		
		if(valueData == null)
		{
			this.valueTable.setDataToView(null);
			return;
		}
		
		JEXDataSingle ds = valueData.getData(this.dataBrowser.currentImageDimMap());
		if(ds != null)
		{
			this.valueTable.setDataToView(valueData);
		}
		else
		{
			this.valueTable.setDataToView(null);
			return;
		}
		
	}
	
	// //////////////////////////////////////
	// //////////PlugIn Stuff //////////////
	// //////////////////////////////////////
	
	/**
	 * Called upon closing of the window
	 */
	public void finalizePlugIn()
	{   
		
	}
	
	public PlugIn plugIn()
	{
		return this.dialog;
	}
}
