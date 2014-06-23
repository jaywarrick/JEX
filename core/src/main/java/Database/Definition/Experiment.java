package Database.Definition;

import java.awt.Dimension;
import java.util.TreeMap;
import java.util.TreeSet;

import miscellaneous.StringUtility;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.SingleUserDatabase.tnvi;

public class Experiment extends TreeMap<Integer,TreeMap<Integer,JEXEntry>> implements Comparable<Experiment>, HierarchyLevel {
	
	private static final long serialVersionUID = 1L;
	
	public static String NUMBER = "Size";
	
	public String expName;
	public String expInfo;
	public String expDate;
	public String expMDate;
	public String expAuthor;
	public String expNumber;
	
	public TreeSet<JEXEntry> entries;
	public tnvi TNVI;
	
	public Experiment(String expName, String expInfo, String expDate, String expMDate, String expAuthor, String expNumber)
	{
		super();
		this.expName = expName;
		this.expInfo = expInfo;
		this.expDate = expDate;
		this.expMDate = expMDate;
		this.expAuthor = expAuthor;
		this.expNumber = expNumber;
		this.TNVI = new tnvi();
	}
	
	/**
	 * Add an entry to the list of entries in the experiment
	 * 
	 * @param entry
	 */
	public void addEntry(JEXEntry entry)
	{
		// Add the entry to the list
		if(this.entries == null)
		{
			this.entries = new TreeSet<JEXEntry>();
		}
		this.entries.add(entry);
		
		// Add to the dictionaries
		this.TNVI.addEntryForAllDataInEntry(entry);
		
		// Place it in an array
		Integer x = entry.getTrayX();
		Integer y = entry.getTrayY();
		TreeMap<Integer,JEXEntry> row = this.get(x);
		if(row == null)
		{
			row = new TreeMap<Integer,JEXEntry>();
			this.put(x, row);
		}
		row.put(y, entry);
	}
	
	/**
	 * Update the Tray TNVI
	 * 
	 * @param entry
	 * @param data
	 */
	public void addEntryForData(JEXEntry entry, JEXData data)
	{
		this.TNVI.addEntryForData(entry, data);
	}
	
	/**
	 * Update the Tray TNVI
	 * 
	 * @param entry
	 * @param data
	 */
	public void removeEntryForData(JEXEntry entry, JEXData data)
	{
		this.TNVI.removeEntryForData(entry, data);
	}
	
	public tnvi tnvi()
	{
		return this.TNVI;
	}
	
	/**
	 * Return the list of entries in the experiment
	 * 
	 * @return
	 */
	@Override
	public TreeSet<JEXEntry> getEntries()
	{
		return this.entries;
	}
	
	/**
	 * Returns a representative entry, or NULL if no entry is there
	 */
	@Override
	public JEXEntry getRepresentativeEntry()
	{
		// Get a representative entry from this cell (usually the first)
		JEXEntry entry = null;
		if(this.entries != null && this.entries.size() > 0)
		{
			entry = this.entries.iterator().next();
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
	
	// ----------------------------------------------------
	// --------- HIERARCHYLEVEL METHODS ------------------------
	// ----------------------------------------------------
	
	@Override
	public String getName()
	{
		return this.expName;
	}
	
	@Override
	public String getType()
	{
		return JEXEntry.TRAY;
	}
	
	/**
	 * Return the list of all ??? in this tray (NOT USED)
	 */
	@Override
	public TreeMap<String,HierarchyLevel> getSublevelList()
	{
		TreeMap<String,HierarchyLevel> result = new TreeMap<String,HierarchyLevel>(new StringUtility());
		
		for (JEXEntry entry : this.entries)
		{
			result.put(entry.getEntryID(), new EntryLevel(entry));
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
		
		for (Integer i : this.keySet())
		{
			TreeMap<Integer,JEXEntry> columns = this.get(i);
			TreeMap<String,HierarchyLevel> column = new TreeMap<String,HierarchyLevel>(new StringUtility());
			for (Integer j : columns.keySet())
			{
				JEXEntry entry = columns.get(j);
				column.put("" + j, new EntryLevel(entry));
			}
			
			result.put("" + i, column);
		}
		
		return result;
	}
	
	public Dimension getArrayDimension()
	{
		int width = 0, height = 0;
		for (Integer x : this.keySet())
		{
			if((x + 1) > width)
			{
				width = x + 1;
			}
			for (Integer y : this.get(x).keySet())
			{
				if((y + 1) > height)
				{
					height = y + 1;
				}
			}
		}
		
		Dimension result = new Dimension(width, height);
		return result;
	}
	
	@Override
	public int hashCode()
	{
		return this.expName.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Experiment))
		{
			return false;
		}
		Experiment e = (Experiment) o;
		return this.expName.equals(e.expName);
	}
	
	@Override
	public int compareTo(Experiment o)
	{
		return this.expName.compareTo(o.expName);
	}
	
}
