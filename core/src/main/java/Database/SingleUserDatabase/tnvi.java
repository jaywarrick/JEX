package Database.SingleUserDatabase;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Filter;
import Database.Definition.FilterSet;
import Database.Definition.Type;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logs.Logs;
import signals.SSCenter;

public class tnvi extends TreeMap<Type,TreeMap<String,TreeMap<String,Set<JEXEntry>>>> {
	
	private static final long serialVersionUID = 1L;
	public static final String SIG_ObjectsChanged_NULL = "SIG_ObjectsChanged_NULL";
	private String hashString;
	
	public tnvi()
	{
		super();
	}
	
	public tnvi(String hashString)
	{
		super();
		
		this.hashString = hashString;
	}
	
	/**
	 * Returns a list of all the entries contained in the TNVI
	 * 
	 * @return
	 */
	public TreeSet<JEXEntry> entries()
	{
		TreeSet<JEXEntry> result = new TreeSet<JEXEntry>();
		
		// loop through the tnvi to collect the entries
		for (TreeMap<String,TreeMap<String,Set<JEXEntry>>> nvi : this.values())
		{
			for (TreeMap<String,Set<JEXEntry>> vi : nvi.values())
			{
				for (Set<JEXEntry> i : vi.values())
				{
					result.addAll(i);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Add an entry to this TNVI dictionary. For each data contained in this entry, add the entry to the entry list keyed by each JEXData in the entry Send a change signal after completion.
	 * 
	 * @param entry
	 * @param tnv
	 * @return
	 */
	public boolean addEntryForAllDataInEntry(JEXEntry entry)
	{
		boolean result = _addEntryForAllDataInEntry(entry);
		
		// if (entry.getDataList() != null && entry.getDataList().size()>0)
		// {
		// sendChangeSignal();
		// }
		sendChangeSignal();
		
		return result;
	}
	
	/**
	 * Add an entry to this TNVI dictionary. For each data contained in this entry, add the entry to the entry list keyed by each JEXData in the entry Perform the task WITHOUT sending a change signal after completion.
	 * 
	 * @param entry
	 * @return
	 */
	public boolean _addEntryForAllDataInEntry(JEXEntry entry)
	{
		boolean result = true;
		
		// Loop through the object types contained in the entry ENTRY
		for (Type type : entry.getDataList().keySet())
		{
			// Get the nvi
			TreeMap<String,JEXData> addNV = entry.getDataList().get(type);
			
			// Loop through the names contained in the entry ENTRY for type TYPE
			for (String name : addNV.keySet())
			{
				// Get the value
				// String value = addNV.get(name).getDictionaryValue();
				
				// Add the object type name and value to the TNVI
				boolean done = addEntryForData(entry, addNV.get(name));
				result = result && done;
			}
		}
		
		return result;
	}
	
	/**
	 * Remove the entry from the TNVI dictionary. Remove the entry from the entry list keyed by each data in the entry.
	 * 
	 * @param entry
	 * @return
	 */
	public boolean removeEntryForAllDataInEntry(JEXEntry entry)
	{
		boolean result = true;
		
		// Loop through the object types contained in the entry ENTRY
		for (Type type : entry.getDataList().keySet())
		{
			// Get the nvi
			TreeMap<String,JEXData> removeNV = entry.getDataList().get(type);
			
			// Loop through the names contained in the entry ENTRY for type TYPE
			for (String name : removeNV.keySet())
			{
				// Get the value
				JEXData data = removeNV.get(name);
				
				// Add the object type name and value to the TNVI
				boolean done = _removeEntryForData(entry, data);
				result = result && done;
			}
		}
		
		// if (entry.getDataList() != null && entry.getDataList().size()>0)
		// {
		// sendChangeSignal();
		// }
		sendChangeSignal();
		
		return result;
	}
	
	/**
	 * Remove the entry list associate with this data
	 * 
	 * @param entry
	 * @param objType
	 * @param objName
	 * @param objValue
	 * @return
	 */
	public boolean removeEntryForData(JEXEntry entry, JEXData data)
	{
		boolean result = this._removeEntryForData(entry, data);
		
		// if (entry.getDataList() != null && entry.getDataList().size()>0)
		// {
		// sendChangeSignal();
		// }
		sendChangeSignal();
		
		return result;
	}
	
	/**
	 * Remove the entry list associate with this data
	 * 
	 * @param entry
	 * @param objType
	 * @param objName
	 * @param objValue
	 * @return
	 */
	public boolean _removeEntryForData(JEXEntry entry, JEXData data)
	{
		// If the data to remove is null return true
		if(data == null)
		{
			return true;
		}
		
		// Get the nvi of this dictionary
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> thisNVI = this.get(data.getTypeName().getType());
		
		// If null return
		if(thisNVI == null)
			return true;
		
		// Get the values in this dictionary
		TreeMap<String,Set<JEXEntry>> thisVI = thisNVI.get(data.getTypeName().getName());
		
		// If null return
		if(thisVI == null)
			return true;
		
		// Get the entries for the new value
		Set<JEXEntry> entries = thisVI.get(data.getDictionaryValue());
		
		// If null return
		if(entries == null)
			return true;
		
		// remove the entry ENTRY from the list
		entries.remove(entry);
		
		// Clean up the dictionary so as to not leave any loose ends
		// First clean up the entries list if null
		if(entries.size() == 0)
		{
			thisVI.remove(data.getDictionaryValue());
		}
		
		// Second check the name VI dict
		if(thisVI.size() == 0)
		{
			thisNVI.remove(data.getTypeName().getName());
		}
		
		// third check the NVI dict
		if(thisNVI.size() == 0)
		{
			this.remove(data.getTypeName().getType());
		}
		
		return true;
	}
	
	/**
	 * Add to the entry ENTRY the object of type OBJTYPE, name OBJNAME, and value OBJVALUE
	 * 
	 * @param entry
	 * @param objType
	 * @param objName
	 * @param objValue
	 * @return
	 */
	public boolean addEntryForData(JEXEntry entry, JEXData data)
	{
		// Get the nvi of this dictionary
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> thisNVI = this.get(data.getTypeName().getType());
		
		// If null create a new one
		if(thisNVI == null)
		{
			thisNVI = new TreeMap<String,TreeMap<String,Set<JEXEntry>>>();
			this.put(data.getTypeName().getType(), thisNVI);
		}
		
		// Get the values in this dictionary
		TreeMap<String,Set<JEXEntry>> thisVI = thisNVI.get(data.getTypeName().getName());
		
		// If null create one
		if(thisVI == null)
		{
			thisVI = new TreeMap<String,Set<JEXEntry>>();
			thisNVI.put(data.getTypeName().getName(), thisVI);
		}
		
		// Get the entries for the new value
		Set<JEXEntry> entries = thisVI.get(data.getDictionaryValue());
		
		// If null create a new list of entries
		if(entries == null)
		{
			entries = new TreeSet<JEXEntry>();
			thisVI.put(data.getDictionaryValue(), entries);
		}
		
		// Add the entry ENTRY to the list of entries
		entries.add(entry);
		
		return true;
	}
	
	public tnvi getFilteredTNVI(FilterSet filters)
	{
		tnvi fTNVI = new tnvi();
		if(filters == null || filters.size() == 0)
			return this;
		
		for (Filter filter : filters)
		{
			// Get the fields of the filter
			Type type = filter.getType();
			String name = filter.getName();
			Set<String> values = filter.getValues();
			
			// Get the NVI of both the filtered and stat TNVIs
			TreeMap<String,TreeMap<String,Set<JEXEntry>>> nvi = this.get(type);
			TreeMap<String,TreeMap<String,Set<JEXEntry>>> fnvi = fTNVI.get(type);
			
			// if nvi is null continue to the next filter
			if(nvi == null)
				continue;
			
			// if fnvi is null create an entry
			if(fnvi == null)
			{
				fnvi = new TreeMap<String,TreeMap<String,Set<JEXEntry>>>();
				fTNVI.put(type, fnvi);
			}
			
			// Get the vi of both the filtered and stat NVI
			TreeMap<String,Set<JEXEntry>> vi = nvi.get(name);
			TreeMap<String,Set<JEXEntry>> fvi = fnvi.get(name);
			
			// if the vi is null continue to the next filter
			if(vi == null)
				continue;
			
			// If the fvi is null create an entry in the dic
			if(fvi == null)
			{
				fvi = new TreeMap<String,Set<JEXEntry>>();
				fnvi.put(name, fvi);
			}
			
			// Loop through the values to be filtered
			for (String value : values)
			{
				// get the i
				Set<JEXEntry> i = vi.get(value);
				
				// If no entries then continue
				if(i == null)
					continue;
				
				// get the fi
				Set<JEXEntry> fi = fvi.get(value);
				
				// if null create a new entry
				if(fi == null)
				{
					fi = new TreeSet<JEXEntry>();
					fvi.put(value, fi);
				}
				
				// add the entries to the fi
				fi.addAll(i);
			}
		}
		return fTNVI;
	}
	
	/**
	 * Empty all data from the TNVI
	 */
	public void empty(boolean emitSignal)
	{
		this.clear();
		
		if(emitSignal)
		{
			sendChangeSignal();
		}
	}
	
	public void sendChangeSignal()
	{
		Logs.log("TNVI has changed sending signal", 2, this);
		SSCenter.defaultCenter().emit(this, SIG_ObjectsChanged_NULL, (Object[]) null);
	}
	
	@Override
	public int hashCode()
	{
		if(hashString == null)
			return super.hashCode();
		return hashString.hashCode();
	}
}
