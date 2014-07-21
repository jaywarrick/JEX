package Database.SingleUserDatabase;

import Database.DBObjects.JEXEntry;

import java.util.TreeMap;

import logs.Logs;
import signals.SSCenter;

public class itnv extends TreeMap<JEXEntry,TreeMap<String,TreeMap<String,String>>> {
	
	private static final long serialVersionUID = 1L;
	
	public static final String ENTRIES_CHANGED = "ENTRIES_CHANGED";
	public static final String SIG_DICTIONARYCHANGED_NULL = "SIG_DICTIONARYCHANGED_NULL";
	
	public itnv()
	{
		super();
	}
	
	public boolean addEntry(JEXEntry entry, TreeMap<String,TreeMap<String,String>> tnv)
	{
		try
		{
			this.put(entry, tnv);
			
			if(entry != null)
			{
				SSCenter.defaultCenter().emit(this, ENTRIES_CHANGED, (Object[]) null);
			}
		}
		catch (Exception e)
		{
			Logs.log("Adding to itnv failed", 1, this);
			return false;
		}
		
		return true;
	}
	
	public boolean removeEntry(JEXEntry entry)
	{
		try
		{
			this.remove(entry);
			
			if(entry != null)
			{
				SSCenter.defaultCenter().emit(this, ENTRIES_CHANGED, (Object[]) null);
			}
		}
		catch (Exception e)
		{
			Logs.log("Removing from itnv failed", 1, this);
			return false;
		}
		return true;
	}
	
	public boolean removeObjectFromEntry(JEXEntry entry, String objType, String objName)
	{
		TreeMap<String,TreeMap<String,String>> tnv = this.get(entry);
		if(tnv == null)
		{
			Logs.log("Entry doesn't exist in database", 1, this);
			return false;
		}
		
		TreeMap<String,String> nv = tnv.get(objType);
		if(nv == null)
		{
			Logs.log("Type doesn't exist in database", 1, this);
			return false;
		}
		
		String value = nv.get(objName);
		if(value == null)
		{
			Logs.log("Object TypeName doesn't exist in database", 1, this);
			return false;
		}
		
		// Remove the object with name and type
		nv.remove(objName);
		
		// If no other objects of this type exist remove the tnv
		if(nv.size() == 0)
			tnv.remove(objType);
		
		return true;
	}
	
	public boolean addObjectToEntry(JEXEntry entry, String objType, String objName, String objValue)
	{
		TreeMap<String,TreeMap<String,String>> tnv = this.get(entry);
		if(tnv == null)
		{
			Logs.log("Entry doesn't exist in database", 1, this);
			return false;
		}
		
		TreeMap<String,String> nv = tnv.get(objType);
		if(nv == null)
		{
			nv = new TreeMap<String,String>();
			tnv.put(objType, nv);
		}
		
		String value = nv.get(objName);
		if(value != null)
		{
			Logs.log("Object typename already exists in the database", 1, this);
			return false;
		}
		
		// Add the type name value to the dictionary
		nv.put(objName, objValue);
		
		return true;
	}
	
	/**
	 * Add a tnv to the ITNV
	 * 
	 * @param entry
	 * @param tnv
	 */
	public void updateEntry(JEXEntry entry, TreeMap<String,TreeMap<String,String>> tnv)
	{
		// get the TNV
		TreeMap<String,TreeMap<String,String>> entrytnv = this.get(entry);
		if(entrytnv == null)
		{
			this.put(entry, tnv);
			return;
		}
		
		// Loop through the types to add
		for (String type : tnv.keySet())
		{
			// Get the NV
			TreeMap<String,String> entryNV = entrytnv.get(type);
			if(entryNV == null)
			{
				entryNV = new TreeMap<String,String>();
				entrytnv.put(type, entryNV);
			}
			
			// add the v
			TreeMap<String,String> nv = tnv.get(type);
			for (String name : nv.keySet())
			{
				String value = nv.get(name);
				entryNV.put(name, value);
			}
		}
	}
	
	/**
	 * Empty all data from the TNVI
	 */
	public void empty()
	{
		for (JEXEntry e : this.keySet())
		{
			this.remove(e);
		}
		
		SSCenter.defaultCenter().emit(this, ENTRIES_CHANGED, (Object[]) null);
	}
}
