package Database.Definition;

import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import miscellaneous.StringUtility;
import Database.DBObjects.JEXEntry;

public class DatabaseLevel extends TreeMap<String,Experiment> implements HierarchyLevel {
	
	private static final long serialVersionUID = 1L;
	
	public DatabaseLevel(TreeMap<String,Experiment> expTree)
	{
		if(expTree == null)
		{
			return;
		}
		for (String expName : expTree.keySet())
		{
			this.put(expName, expTree.get(expName));
		}
	}
	
	@Override
	public String getName()
	{
		return JEXStatics.jexManager.getDatabaseInfo().getName();
	}
	
	@Override
	public String getType()
	{
		return "DATABASE";
	}
	
	/**
	 * Return the list of entries in the experiment
	 * 
	 * @return
	 */
	@Override
	public TreeSet<JEXEntry> getEntries()
	{
		TreeSet<JEXEntry> result = new TreeSet<JEXEntry>();
		for (String expName : this.keySet())
		{
			Experiment exp = this.get(expName);
			result.addAll(exp.getEntries());
		}
		return result;
	}
	
	/**
	 * Returns a representative entry, or NULL if no entry is there
	 */
	@Override
	public JEXEntry getRepresentativeEntry()
	{
		// Get a representative entry from this cell (usually the first)
		JEXEntry entry = null;
		if(this.getEntries() != null && this.getEntries().size() > 0)
		{
			entry = this.getEntries().iterator().next();
		}
		return entry;
	}
	
	/**
	 * Is the entry ENTRY part of the group?
	 */
	@Override
	public boolean containsEntry(JEXEntry entry)
	{
		boolean result = this.getEntries().contains(entry);
		return result;
	}
	
	/**
	 * Return the list of all trays in this experiment
	 */
	@Override
	public TreeMap<String,HierarchyLevel> getSublevelList()
	{
		TreeMap<String,HierarchyLevel> result = new TreeMap<String,HierarchyLevel>(new StringUtility());
		
		for (String expName : this.keySet())
		{
			result.put(expName, this.get(expName));
		}
		
		return result;
	}
	
	/**
	 * Return the list of all trays in an array form, i.e. a matrix column
	 */
	@Override
	public TreeMap<String,TreeMap<String,HierarchyLevel>> getSublevelArray()
	{
		TreeMap<String,TreeMap<String,HierarchyLevel>> result = new TreeMap<String,TreeMap<String,HierarchyLevel>>(new StringUtility());
		
		TreeMap<String,HierarchyLevel> column = new TreeMap<String,HierarchyLevel>(new StringUtility());
		for (String expName : this.keySet())
		{
			column.put(expName, this.get(expName));
		}
		
		result.put("", column);
		return result;
	}
	
}
