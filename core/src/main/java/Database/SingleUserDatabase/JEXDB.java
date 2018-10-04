package Database.SingleUserDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.Definition.DimensionGroupMap;
import Database.Definition.Experiment;
import Database.Definition.Filter;
import Database.Definition.FilterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import jex.JEXManager;
import jex.statics.JEXStatics;
import logs.Logs;
import signals.SSCenter;

public class JEXDB implements Iterable<JEXEntry> {
	
	// Database types
	public static String LOCAL_DATABASE = "local";
	public static String REMOTE_DATABASE = "remote";
	public static String REMOTE_WITH_IP_DATABASE = "remoteIP";
	
	// Flags and password stuff
	private boolean containsUnsavedData = false; // Flag
	// switches
	// to
	// true
	// when
	// DB
	// is
	// modified
	
	// Low level databasegetExperimentalTable
	private TreeSet<JEXEntry> entries;
	private TreeSet<JEXEntry> filteredEntries;
	private TreeMap<String,Experiment> experimentalTable;
	private int maxID;
	
	// Filters and groups
	private FilterSet filters;
	private List<TypeName> groups;
	private FilterSet statFilters;
	private List<TypeName> statGroups;
	
	// Dictionaries of the databse
	private tnvi TNVI;
	private tnvi fTNVI;
	private tnvi sTNVI;
	
	// ---------------------------------------------
	// Creating and defining
	// ---------------------------------------------
	public JEXDB()
	{
		this.initialize();
	}
	
	/**
	 * Initialize all the fields of the database
	 */
	private void initialize()
	{
		// Make the filter and group sets
		this.filters = new FilterSet();
		this.groups = new ArrayList<TypeName>(0);
		this.statFilters = new FilterSet();
		this.statGroups = new ArrayList<TypeName>(0);
		
		// Make the dictionaries
		this.TNVI = new tnvi();
		this.fTNVI = new tnvi();
		this.sTNVI = new tnvi();
		
		this.entries = new TreeSet<JEXEntry>();
		this.filteredEntries = new TreeSet<JEXEntry>();
	}
	
	public void emitUpdateSignal()
	{
		// Emit signal of experiment tree change
		Logs.log("Send signal of experiment tree change", 1, this);
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.EXPERIMENTTREE_CHANGE, (Object[]) null);
	}
	
	public void emitAvailableObjectsSignal()
	{
		// Emit signal of repository hashmap change
		Logs.log("Send signal of change of database dictionary", 1, this);
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.AVAILABLEOBJ, (Object[]) null);
	}
	
	// ---------------------------------------------
	// Getters and setters
	// ---------------------------------------------
	
	/**
	 * Get the reference to the set of entries in this database
	 */
	public TreeSet<JEXEntry> getEntries()
	{
		return this.entries;
	}
	
	/**
	 * Get the reference to the set of entries in this database
	 */
	public TreeSet<JEXEntry> getFilteredEntries()
	{
		return this.filteredEntries;
	}
	
	public void setMaxID(int maxID)
	{
		this.maxID = maxID;
	}
	
	// ---------------------------------------------
	// Loading, Saving and closing the database
	// ---------------------------------------------
	
	/**
	 * Return true if the database was modifed after last save
	 * 
	 * @return boolean
	 */
	public boolean containsUnsavedData()
	{
		return this.containsUnsavedData;
	}
	
	/**
	 * Set the unsaved flag to false;
	 */
	public boolean databaseSaved()
	{
		this.containsUnsavedData = false;
		// this.emitUpdateSignal();
		return true;
	}
	
	// ---------------------------------------------
	// Settings, options and administration
	// ---------------------------------------------
	
	/**
	 * Return the experimental table in string form
	 */
	public TreeMap<String,Experiment> getExperimentalTable()
	{
		return this.experimentalTable;
	}
	
	// ---------------------------------------------
	// Creation, addition, removing and editing of entries and objects
	// ---------------------------------------------
	/**
	 * Create a new entry in the database
	 */
	public void addEntry(String expName, String arrayName, int row, int col, String date, String author, String info)
	{
		this._addEntry(expName, row, col, date, author, info);
		
		this.containsUnsavedData = true;
		
		// Make the libraries
		this.makeTNVIs();
		this.makeFiltTNVI();
		this.makeExperimentTree();
		// this.emitUpdateSignal();
	}
	
	/**
	 * Create an array of entries and update the dictionarries all at once
	 */
	public JEXEntry[][] addEntries(String expName, int width, int height, String date, String author, String info)
	{
		JEXEntry[][] result = new JEXEntry[width][height];
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				JEXEntry entry = this._addEntry(expName, y, x, date, author, info);
				result[x][y] = entry;
			}
		}
		
		// Make the libraries
		this.containsUnsavedData = true;
		this.makeTNVIs();
		this.makeFiltTNVI();
		this.makeExperimentTree();
		// this.emitUpdateSignal();
		return result;
	}
	
	/**
	 * Create and add an entry to the database
	 * 
	 * @param expName
	 * @param arrayName
	 * @param row
	 * @param col
	 * @param author
	 * @param info
	 * @return
	 */
	private JEXEntry _addEntry(String expName, int row, int col, String date, String author, String info)
	{
		JEXEntry result = new JEXEntry();
		
		result.setParent(this);
		result.setEntryExperiment(expName);
		result.setEntryExperimentInfo(info);
		result.setDate(date);
		result.setEntryID("" + this.maxID);
		// result.setEntryTrayName(arrayName);
		result.setModificationDate(date);
		result.setTrayX(col);
		result.setTrayY(row);
		result.setAuthor(author);
		this.maxID = this.maxID + 1;
		
		// Add entry to the entry list of JEX
		this.entries.add(result);
		
		// Make the valid label
		JEXData vLabel = new JEXLabel(JEXEntry.VALID, "true", "");
		this.addData(result, vLabel, true);
		
		return result;
	}
	
	/**
	 * Remove an entry from the database
	 */
	public boolean removeEntry(JEXEntry entry)
	{
		HashSet<JEXEntry> entries = new HashSet<JEXEntry>();
		return this.removeEntries(entries);
	}
	
	/**
	 * Remove a list of entries from the database
	 */
	public boolean removeEntries(Set<JEXEntry> entries)
	{
		// remove the entries
		boolean success = true;
		for (JEXEntry entry : entries)
		{
			boolean b = this._removeEntry(entry);
			success = success && b;
		}
		
		// Make the libraries
		this.containsUnsavedData = true;
		this.makeTNVIs();
		this.makeFiltTNVI();
		this.makeExperimentTree();
		// this.emitUpdateSignal();
		return success;
	}
	
	/**
	 * Remove the entry ENTRY
	 * 
	 * @param entry
	 */
	private boolean _removeEntry(JEXEntry entry)
	{
		return this.entries.remove(entry);
	}
	
	public void editHeirarchyForEntries(TreeSet<JEXEntry> entries, String experiment, String info, String date)
	{
		for (JEXEntry entry : entries)
		{
			entry.setEntryExperiment(experiment);
			entry.setEntryExperimentInfo(info);
			entry.setDate(date);
		}
		
		// Make the libraries
		this.containsUnsavedData = true;
		this.makeTNVIs();
		this.makeFiltTNVI();
		this.makeExperimentTree();
		// this.emitUpdateSignal();
	}
	
	/**
	 * Create the data object DATA in the entry ENTRY
	 */
	public boolean addData(JEXEntry entry, JEXData data, boolean overwrite)
	{
		boolean result = entry.addData(data, overwrite);
		if(!result)
		{
			return result;
		}
		
		// Load the datamap within the JEXData to mark the data for saving
		data.getDataMap();
		
		// Make the libraries
		this.containsUnsavedData = true;
		this.addEntryForDataToTNVIs(entry, data);
		this.addEntryForDataToExperimentTree(entry, data);
		// this.emitUpdateSignal();
		return true;
	}
	
	/**
	 * Create the dataentries for each jexentry
	 */
	public boolean addDatas(Map<JEXEntry,Set<JEXData>> dataArray, boolean overwrite)
	{
		boolean result = true;
		Logs.log("Adding data to " + dataArray.size() + " entries", 1, this);
		for (JEXEntry entry : dataArray.keySet())
		{
			Set<JEXData> datas = dataArray.get(entry);
			for (JEXData data : datas)
			{
				boolean tempResult = this.addData(entry, data, overwrite);
				if(tempResult)
				{
					this.containsUnsavedData = true;
				}
				result = result && tempResult;
			}
		}
		if(!result)
		{
			Logs.log("Failed to add data to all of " + dataArray.size() + " entries", 1, this);
			return result;
		}
		Logs.log("Data added to " + dataArray.size() + " entries", 1, this);
		
		// this.emitUpdateSignal();
		return true;
	}
	
	/**
	 * Remove the object array
	 */
	public boolean removeObjectArray(Map<JEXEntry,Set<JEXData>> dataArray)
	{
		for (JEXEntry entry : dataArray.keySet())
		{
			Set<JEXData> datas = dataArray.get(entry);
			for (JEXData data : datas)
			{
				// Remove from database
				entry.removeData(data);
				
				// Remove from dictionaries
				this.TNVI.removeEntryForData(entry, data);
				this.fTNVI.removeEntryForData(entry, data);
				// this.sTNVI.removeEntryForData(entry, data); // THINK ABOUT
				// DOING THIS IN THE FUTURE WHEN CHANGING PANELS DOESNT JUST
				// RESET sTNVI ANYWAY
				
				// Remove from Experiment Tree
				this.removeEntryForDataFromExperimentTree(entry, data);
			}
		}
		
		// Make the libraries
		this.containsUnsavedData = true;
		// this.emitUpdateSignal();
		return true;
	}
	
	// ---------------------------------------------
	// Filtering
	// ---------------------------------------------
	/**
	 * Set the filterset of the databse
	 * 
	 * @param filterset
	 */
	public void setFilterSet(FilterSet filterset)
	{
		this.filters = filterset;
		this.makeFiltTNVI();
	}
	
	/**
	 * Add a filter to the filterset... allows rapid rebuilding of the dictionaries rather than resetting the whole filterset Return true is successful
	 * 
	 * @param filter
	 * @return boolean
	 */
	public boolean addFilter(Filter filter)
	{
		if(this.filters == null)
		{
			this.filters = new FilterSet();
		}
		this.filters.add(filter);
		this.makeFiltTNVI();
		return true;
	}
	
	/**
	 * Remove the last filter of the filterset for rapid rebulding of the dictionaries Return true is successful
	 * 
	 * @return boolean
	 */
	public boolean removeLastFilter()
	{
		if(this.filters == null || this.filters.size() == 0)
		{
			return false;
		}
		this.filters.remove(this.filters.size() - 1);
		this.makeFiltTNVI();
		return true;
	}
	
	/**
	 * Return the available filters based on the scope
	 * 
	 * @return
	 */
	public HashMap<TypeName,Set<Filter>> getAvailableFilters()
	{
		return null;
	}
	
	// ---------------------------------------------
	// Grouping
	// ---------------------------------------------
	/**
	 * Return the set of Typenames listed to group the datasets
	 */
	public List<TypeName> getGroupingSet()
	{
		return this.groups;
	}
	
	/**
	 * Set the grouping list
	 */
	public void setGroupingSet(List<TypeName> tns)
	{
		this.groups = tns;
	}
	
	/**
	 * Add a group to the grouping list
	 */
	public void addGrouping(TypeName tn)
	{
		if(this.groups == null)
		{
			this.groups = new ArrayList<TypeName>(0);
		}
		this.groups.add(tn);
	}
	
	/**
	 * Return a grouping based on the first group in the list of the filtered fTNVI dictionary
	 */
	public TreeMap<Filter,Set<JEXEntry>> getGroupedEntries1D()
	{
		TypeName tn = this.groups.get(0);
		return this.getGroupedEntries(tn);
	}
	
	/**
	 * Return the grouped entries depending on the number of groups
	 */
	public TreeMap<Filter,TreeMap<Filter,Set<JEXEntry>>> getGroupedEntries()
	{
		TreeMap<Filter,TreeMap<Filter,Set<JEXEntry>>> result = new TreeMap<Filter,TreeMap<Filter,Set<JEXEntry>>>();
		
		if(this.groups == null || this.groups.size() == 0)
		{
			TreeMap<Filter,Set<JEXEntry>> rows = new TreeMap<Filter,Set<JEXEntry>>();
			Filter colFilter = new Filter(null, "", "");
			for (JEXEntry entry : this.filteredEntries)
			{
				Filter rowFilter = new Filter(JEXData.HIERARCHY, JEXEntry.EID, entry.getEntryID());
				TreeSet<JEXEntry> oneEntry = new TreeSet<JEXEntry>();
				oneEntry.add(entry);
				rows.put(rowFilter, oneEntry);
			}
			result.put(colFilter, rows);
		}
		else if(this.groups.size() == 1)
		{
			TypeName tn1 = this.groups.get(0);
			TreeMap<Filter,Set<JEXEntry>> g1 = this.getGroupedEntries(tn1);
			
			Filter emptyFilter = new Filter(null, "", "");
			result.put(emptyFilter, g1);
		}
		else
		{
			TypeName tn1 = this.groups.get(0);
			TypeName tn2 = this.groups.get(1);
			
			TreeMap<Filter,Set<JEXEntry>> g1 = this.getGroupedEntries(tn1);
			TreeMap<Filter,Set<JEXEntry>> g2 = this.getGroupedEntries(tn2);
			
			for (Filter f : g1.keySet())
			{
				Set<JEXEntry> entriesInF = g1.get(f);
				
				// cross references with entries in g2
				TreeMap<Filter,Set<JEXEntry>> rows = new TreeMap<Filter,Set<JEXEntry>>();
				
				// fill the rows
				for (Filter f2 : g2.keySet())
				{
					Set<JEXEntry> entriesInF2 = g2.get(f2);
					
					// Make the new set of entries common to both
					TreeSet<JEXEntry> commonEntries = new TreeSet<JEXEntry>();
					for (JEXEntry e1 : entriesInF)
					{
						if(entriesInF2.contains(e1))
						{
							commonEntries.add(e1);
						}
					}
					for (JEXEntry e2 : entriesInF2)
					{
						if(entriesInF.contains(e2))
						{
							commonEntries.add(e2);
						}
					}
					
					rows.put(f2, commonEntries);
				}
				
				// put the filter in the result map
				result.put(f, rows);
			}
		}
		
		return result;
	}
	
	/**
	 * Return a grouping of the filtered fTNVI based on the TypeName tn
	 * 
	 * @param tn
	 * @return
	 */
	private TreeMap<Filter,Set<JEXEntry>> getGroupedEntries(TypeName tn)
	{
		if(this.groups == null || this.groups.size() == 0)
		{
			return null;
		}
		
		// Get the first group in the list
		Type type = tn.getType();
		String name = tn.getName();
		
		// Make the treemap to be returned
		TreeMap<Filter,Set<JEXEntry>> result = new TreeMap<Filter,Set<JEXEntry>>();
		
		// Get the filters
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> nvi = this.fTNVI.get(type);
		if(nvi == null)
		{
			return result;
		}
		
		TreeMap<String,Set<JEXEntry>> vi = nvi.get(name);
		if(vi == null)
		{
			return result;
		}
		
		for (String value : vi.keySet())
		{
			Set<JEXEntry> entries = vi.get(value);
			
			// make the filter
			HashSet<String> values = new HashSet<String>(0);
			values.add(value);
			Filter f = new Filter(type, name, values);
			result.put(f, entries);
		}
		
		return result;
	}
	
	// ---------------------------------------------
	// Statistics methods
	// ---------------------------------------------
	/**
	 * Set the statistics filterset
	 */
	public void setStatisticsFilterSet(FilterSet filterset)
	{
		this.statFilters = filterset;
		this.makeStatTNVI();
	}
	
	/**
	 * Set the statistics grouping set
	 */
	public void setStatisticsGrouping(List<TypeName> tns)
	{
		this.statGroups = tns;
	}
	
	/**
	 * Return the statistics TNVI of the database
	 */
	public tnvi getStatisticsTNVI()
	{
		return this.sTNVI;
	}
	
	/**
	 * Return the grouped entries depending on the number of groups
	 */
	public TreeMap<DimensionGroupMap,Set<JEXEntry>> getStatisticsGroupedEntries()
	{
		TreeMap<DimensionGroupMap,Set<JEXEntry>> result = new TreeMap<DimensionGroupMap,Set<JEXEntry>>();
		
		// Make the 0th level supra grouping
		DimensionGroupMap noDim = new DimensionGroupMap();
		
		TreeSet<JEXEntry> allEntries = this.getStatisticsTNVI().entries();
		result.put(noDim, allEntries);
		
		// If there are no grouping rules, put all in one column group
		if(this.statGroups == null || this.statGroups.size() == 0)
		{
			return result;
		}
		else
		{
			for (TypeName tn : this.statGroups)
			{
				result = this.subGroupEntries(result, tn);
			}
		}
		
		return result;
	}
	
	/**
	 * Sub group a grouping based on an additional grouping rule (ie split a current grouping into finner groups)
	 * 
	 * @param supraGrouping
	 * @param subGroup
	 * @return
	 */
	private TreeMap<DimensionGroupMap,Set<JEXEntry>> subGroupEntries(TreeMap<DimensionGroupMap,Set<JEXEntry>> supraGrouping, TypeName subGroup)
	{
		if(subGroup == null)
		{
			return supraGrouping;
		}
		
		// Get the first group in the list
		Type type = subGroup.getType();
		String name = subGroup.getName();
		
		// Prepare the output result
		TreeMap<DimensionGroupMap,Set<JEXEntry>> result = new TreeMap<DimensionGroupMap,Set<JEXEntry>>();
		
		// Go through the supraGroups and sub group them
		for (DimensionGroupMap dim : supraGrouping.keySet())
		{
			Set<JEXEntry> entries = supraGrouping.get(dim);
			
			// Go though the entries of this grouping and find which group they
			// match
			for (JEXEntry entry : entries)
			{
				TreeMap<Type,TreeMap<String,JEXData>> tnv = entry.getDataList();
				
				// Group entries by the different values of the typename from
				// the SUBGROUP
				TreeMap<String,JEXData> typeMap = tnv.get(type);
				
				// If the type does not exist then put into an empty group
				DimensionGroupMap newDim = dim.duplicate();
				JEXData data = (typeMap == null) ? null : typeMap.get(name);
				
				if(data == null)
				{
					newDim.put(new TypeName(type, name), "-");
				}
				else
				{
					newDim.put(new TypeName(type, name), data.getDictionaryValue());
				}
				
				// Check if the new subgroup already exists
				Set<JEXEntry> existingEntries = result.get(newDim);
				// for (DimensionGroupMap dimg: result.keySet()){
				// boolean eq = (dimg.equals(newDim));
				// Logs.log("@#@#@#@# "+ eq, 1, this);
				// }
				if(existingEntries == null)
				{
					existingEntries = new TreeSet<JEXEntry>();
					result.put(newDim, existingEntries);
				}
				existingEntries.add(entry);
			}
		}
		
		return result;
	}
	
	// ---------------------------------------------
	// Querying
	// ---------------------------------------------
	
	/**
	 * Return the TNVI of the database
	 * 
	 * @return TNVI
	 */
	public tnvi getTNVI()
	{
		return this.TNVI;
	}
	
	/**
	 * Return the filtered TNVI of the database
	 * 
	 * @return fTNVI
	 */
	public tnvi getFilteredTNVI()
	{
		return this.fTNVI;
	}
	
	/**
	 * Return the JEXData of typename TN in entry ENTRY
	 */
	public Vector<JEXData> getDatasOfTypeWithNameContainingInEntry(TypeName tn, JEXEntry entry)
	{
		if(tn == null || entry == null)
		{
			return null;
		}
		
		TreeMap<Type,TreeMap<String,JEXData>> tnmap = entry.getDataList();
		if(tnmap == null)
		{
			return null;
		}
		
		TreeMap<String,JEXData> nmap = tnmap.get(tn.getType());
		if(nmap == null)
		{
			return null;
		}
		
		Vector<JEXData> ret = new Vector<>();
		for(String name : nmap.keySet())
		{
			if(name.contains(tn.getName()))
			{
				ret.add(nmap.get(name));
			}
		}
		
		return ret;
	}
	
	/**
	 * Return the JEXData of typename TN in entry ENTRY
	 */
	public Vector<JEXData> getUpdateFlavoredDatasInEntry(JEXEntry entry)
	{
		if(entry == null)
		{
			return null;
		}
		
		TreeMap<Type,TreeMap<String,JEXData>> tnmap = entry.getDataList();
		if(tnmap == null)
		{
			return null;
		}
		
		Vector<JEXData> ret = new Vector<>();
		for(Entry<Type,TreeMap<String,JEXData>> e : tnmap.entrySet())
		{
			if(e.getKey().hasFlavor(JEXData.FLAVOR_UPDATE))
			{
				ret.addAll(e.getValue().values());
			}
		}
		return ret;
	}
	
	/**
	 * Return the JEXData of typename TN in entry ENTRY
	 */
	public JEXData getUpdateFlavoredDataOfTypeNameInEntry(TypeName tn, JEXEntry entry)
	{
		if(tn == null || entry == null)
		{
			return null;
		}
		
		TreeMap<Type,TreeMap<String,JEXData>> tnmap = entry.getDataList();
		if(tnmap == null)
		{
			return null;
		}
		
		boolean found = false;
		TreeMap<String,JEXData> nmap = null;
		for(Entry<Type,TreeMap<String,JEXData>> tnmapE : tnmap.entrySet())
		{
			if(found)
			{
				break;
			}
			if(tnmapE.getKey().matches(tn.getType()) & tnmapE.getKey().hasFlavor(JEXData.FLAVOR_UPDATE))
			{
				for(Entry<String,JEXData> nmapE : tnmapE.getValue().entrySet())
				{
					if(nmapE.getKey().equals(tn.getName()))
					{
						nmap = tnmapE.getValue();
						found = true;
						break;
					}
				}
			}
		}
		if(nmap == null)
		{
			return null;
		}
		
		JEXData data = nmap.get(tn.getName());
		return data;
	}
	
	/**
	 * Return the JEXData of typename TN in entry ENTRY
	 */
	public JEXData getDataOfTypeNameInEntry(TypeName tn, JEXEntry entry)
	{
		if(tn == null || entry == null)
		{
			return null;
		}
		
		TreeMap<Type,TreeMap<String,JEXData>> tnmap = entry.getDataList();
		if(tnmap == null)
		{
			return null;
		}
		
		TreeMap<String,JEXData> nmap = tnmap.get(tn.getType());
		if(nmap == null)
		{
			return null;
		}
		
		JEXData data = nmap.get(tn.getName());
		return data;
	}
	
	/**
	 * Return a TNVI dictionary for an entry list
	 * 
	 * @param entries
	 * @return
	 */
	public tnvi getTNVIforEntryList(Set<JEXEntry> entries)
	{
		tnvi result = new tnvi();
		
		// Fill the tnv from the entry list
		for (JEXEntry entry : entries)
		{
			result.addEntryForAllDataInEntry(entry);
		}
		
		return result;
	}
	
	/**
	 * Set the filtered dictionaries
	 * 
	 * @param TNVI
	 * @param ITNV
	 */
	public void setFilteredDictionaries(tnvi TNVI, TreeSet<JEXEntry> filteredEntries)
	{
		this.fTNVI = TNVI;
		this.filteredEntries = filteredEntries;
	}
	
	/**
	 * Backup the experiment named EXPNAME into the database DB This causes the loading of DB, and the overwritting of all entries and objects with the same experiment name, and finally the saing of DB. This also creates a consolidated archive of the
	 * experiment
	 * 
	 * @param expName
	 * @param db
	 * @return
	 */
	public boolean archiveExperimentIntoDatabase(String expName, JEXDB db)
	{
		boolean result = true;
		
		return result;
	}
	
	// ---------------------------------------------
	// Private methods
	// ---------------------------------------------
	/**
	 * Rebuild all the dictionaries
	 */
	protected void makeTNVIs()
	{
		this.TNVI = new tnvi();
		this.fTNVI = new tnvi();
		this.sTNVI = new tnvi();
		
		// Build the TNVI by looping through each data/value and adding to the
		// TNVI
		int index = 0;
		for (JEXEntry entry : this.entries) // I
		{
			for (Type type : entry.getDataList().keySet()) // T
			{
				TreeMap<String,JEXData> nmap = entry.getDataList().get(type);
				for (String name : nmap.keySet()) // N
				{
					JEXData data = nmap.get(name);
					String value = data.getDictionaryValue(); // V
					
					// Make the TNVI
					TreeMap<String,TreeMap<String,Set<JEXEntry>>> NVI = this.TNVI.get(type);
					if(NVI == null)
					{
						NVI = new TreeMap<String,TreeMap<String,Set<JEXEntry>>>();
						this.TNVI.put(type, NVI);
					}
					TreeMap<String,Set<JEXEntry>> VI = NVI.get(name);
					if(VI == null)
					{
						VI = new TreeMap<String,Set<JEXEntry>>();
						NVI.put(name, VI);
					}
					Set<JEXEntry> ids = VI.get(value);
					if(ids == null)
					{
						ids = new HashSet<JEXEntry>(0);
						VI.put(value, ids);
					}
					ids.add(entry);
				}
			}
			
			// Status bar
			int percentage = (int) (100 * ((double) index / (double) this.size()));
			index++;
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		JEXStatics.statusBar.setProgressPercentage(0);
		
		this.fTNVI = this.TNVI;
		this.sTNVI = this.TNVI;
		this.filteredEntries = this.entries;
	}
	
	protected void makeFiltTNVI()
	{
		this.fTNVI = this.TNVI.getFilteredTNVI(this.filters);
	}
	
	protected void makeStatTNVI()
	{
		if(this.statFilters == null || this.statFilters.size() == 0)
		{
			this.sTNVI = this.TNVI;
			return;
		}
		this.sTNVI = new tnvi();
		
		// IF the filter list is empty do nothing
		if(this.statFilters.size() == 0)
		{
			return;
		}
		this.statFilters.print();
		
		// Loop through the entries and check if they match the filterset
		for (JEXEntry entry : this.entries)
		{
			
			// Does it match the filterset?
			boolean match = true;
			
			// Loop through the filters
			for (Filter filter : this.statFilters)
			{
				// Get the fields of the filter
				Type type = filter.getType();
				String name = filter.getName();
				Set<String> values = filter.getValues();
				
				// Send message
				Logs.log("Filtering TNVI based on filter: type=" + type + " name=" + name + " values=" + values.toString(), 2, this);
				
				// does the entry have an object of type TYPE
				TreeMap<String,JEXData> nv = entry.getDataList().get(type);
				if(nv == null)
				{
					match = false;
					break;
				}
				
				// does the entry have an object of name NAME
				JEXData data = nv.get(name);
				if(data == null)
				{
					match = false;
					break;
				}
				
				// does the value of the object match one of the filtered values
				boolean e = values.contains(data.getDictionaryValue());
				if(!e)
				{
					match = false;
					break;
				}
			}
			
			// Does the entry match the filterset
			if(match)
			{
				// Send message
				Logs.log("Entry with ID=" + entry.getEntryID() + " added to filtered database", 1, this);
				
				// add it to the fTNVI
				this.sTNVI.addEntryForAllDataInEntry(entry);
			}
		}
	}
	
	/**
	 * Recreate the exprimental tree of the database becuase something about the heirarchy has changed (new entries, deleted entries, Experiments or Trays renamed)
	 */
	protected void makeExperimentTree()
	{
		TreeMap<String,Experiment> result = new TreeMap<String,Experiment>();
		
		for (JEXEntry entry : this.entries)
		{
			// Get the experiment if it has already been created, else create
			// one
			Experiment exp = result.get(entry.getEntryExperiment());
			if(exp == null)
			{
				String expName = entry.getEntryExperiment();
				String expInfo = entry.getEntryExperimentInfo();
				String expDate = entry.getDate();
				String expMDate = entry.getModificationDate();
				String expAuthor = entry.getAuthor();
				String expNumber = "1";
				exp = new Experiment(expName, expInfo, expDate, expMDate, expAuthor, expNumber);
				result.put(expName, exp);
			}
			else
			{
				int expNumber = new Integer(exp.expNumber) + 1;
				exp.expNumber = "" + expNumber;
			}
			
			// Add the array name and entry to the experiment
			exp.addEntry(entry);
		}
		
		// Set the experimental table in the manager
		this.experimentalTable = (result);
		
		return;
	}
	
	// ---------------------------------------------
	// Dictionnary updating methods
	// ---------------------------------------------
	
	/**
	 * Update the experiment tree with additional data
	 * 
	 * @param entry
	 * @param data
	 */
	private void addEntryForDataToExperimentTree(JEXEntry entry, JEXData data)
	{
		// Loop through the experimental tree
		TreeMap<String,Experiment> expTree = this.getExperimentalTable();
		for (String expname : expTree.keySet())
		{
			// Get the experiment
			Experiment exp = expTree.get(expname);
			
			// Does the experiment contain the entry ENTRY
			if(!exp.containsEntry(entry))
			{
				continue;
			}
			
			// Update the experiment with the data
			exp.addEntryForData(entry, data);
		}
	}
	
	/**
	 * Update the main dictionaries by adding a data to an entry
	 * 
	 * @param entry
	 * @param data
	 */
	private void addEntryForDataToTNVIs(JEXEntry entry, JEXData data)
	{
		// if data is null pass
		if(data == null)
		{
			return;
		}
		
		// Update TNVI
		this.TNVI.addEntryForData(entry, data);
		this.fTNVI.addEntryForData(entry, data);
		
	}
	
	/**
	 * Update the experiment tree with additional data
	 * 
	 * @param entry
	 * @param data
	 */
	private void removeEntryForDataFromExperimentTree(JEXEntry entry, JEXData data)
	{
		// Loop through the experimental tree
		TreeMap<String,Experiment> expTree = this.getExperimentalTable();
		for (String expname : expTree.keySet())
		{
			// Get the experiment
			Experiment exp = expTree.get(expname);
			
			// Does the experiment contain the entry ENTRY
			if(!exp.containsEntry(entry))
			{
				continue;
			}
			
			// Update the experiment with the data
			exp.removeEntryForData(entry, data);
		}
	}
	
	@Override
	public Iterator<JEXEntry> iterator()
	{
		return this.entries.iterator();
	}
	
	public int size()
	{
		return this.entries.size();
	}
	
}
