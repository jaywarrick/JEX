package Database.Definition;

import Database.DBObjects.JEXEntry;

import java.util.TreeMap;
import java.util.TreeSet;

public class EntryLevel extends TreeSet<JEXEntry> implements HierarchyLevel {
	
	private static final long serialVersionUID = 1L;
	
	public EntryLevel(JEXEntry entry)
	{
		this.add(entry);
	}
	
	public String getName()
	{
		String result = "";
		for (JEXEntry entry : this)
		{
			if(entry != null)
			{
				String res = entry.getTrayX() + "." + entry.getTrayY();
				return res;
			}
		}
		return result;
	}
	
	public String getType()
	{
		return JEXEntry.EID;
	}
	
	/**
	 * Return the list of entries in the experiment
	 * 
	 * @return
	 */
	public TreeSet<JEXEntry> getEntries()
	{
		return this;
	}
	
	/**
	 * Returns a representative entry, or NULL if no entry is there
	 */
	public JEXEntry getRepresentativeEntry()
	{
		// Get a representative entry from this cell (usually the first)
		if(this.getEntries().size() > 0)
		{
			return this.first();
		}
		return null;
	}
	
	/**
	 * Is the entry ENTRY part of the group?
	 */
	public boolean containsEntry(JEXEntry entry)
	{
		boolean result = this.contains(entry);
		return result;
	}
	
	/**
	 * Return the list of all trays in this experiment
	 */
	public TreeMap<String,HierarchyLevel> getSublevelList()
	{
		return null;
	}
	
	/**
	 * Return the list of all trays in an array form, i.e. a matrix column
	 */
	public TreeMap<String,TreeMap<String,HierarchyLevel>> getSublevelArray()
	{
		return null;
	}
	
}
