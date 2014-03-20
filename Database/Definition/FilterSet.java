package Database.Definition;

import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import logs.Logs;

public class FilterSet extends Vector<Filter> {
	
	/**
	 * Don't know what this is...
	 */
	private static final long serialVersionUID = 1L;
	
	// ---------------------------------------------
	// Creators
	// ---------------------------------------------
	/**
	 * Create an empty filterset
	 */
	public FilterSet()
	{
		super(0);
	}
	
	/**
	 * Create an empty filterset
	 */
	public FilterSet(Filter f)
	{
		super(0);
		this.add(f);
	}
	
	/**
	 * Create an empty filterset
	 */
	public FilterSet(Filter[] fset)
	{
		super(0);
		for (Filter f : fset)
		{
			this.add(f);
		}
	}
	
	/**
	 * Create an empty filterset
	 */
	public FilterSet(Set<Filter> fset)
	{
		super(0);
		for (Filter f : fset)
		{
			this.add(f);
		}
	}
	
	// ---------------------------------------------
	// Getter / Setters
	// ---------------------------------------------
	/**
	 * Return the number of filters in the set
	 * 
	 * @return integer
	 */
	public int getNumberFilters()
	{
		return this.size();
	}
	
	/**
	 * Return filter at position index
	 * 
	 * @param index
	 * @return Filter
	 */
	public Filter getFilter(int index)
	{
		if(index < 0 || index > this.size() - 1)
			return null;
		Filter result = this.get(index);
		return result;
	}
	
	public FilterSet duplicate()
	{
		FilterSet result = new FilterSet();
		for (int i = 0; i < this.size(); i++)
		{
			Filter f = this.get(i);
			String fName = f.getName();
			Type fType = f.getType();
			Set<String> vcopy = new TreeSet<String>();
			for (String value : f.getValues())
			{
				vcopy.add(value);
			}
			Filter fcopy = new Filter(fType, fName, vcopy);
			result.add(fcopy);
		}
		return result;
	}
	
	// ---------------------------------------------
	// Add / Remove
	// ---------------------------------------------
	/**
	 * Add a filter and all it's values to the filterset
	 */
	public void addFilter(Filter filter)
	{
		if(filter == null)
			return;
		Type type = filter.getType();
		String name = filter.getName();
		
		if(this.size() == 0)
		{
			this.add(filter);
			return;
		}
		
		boolean hasAdded = false;
		for (Filter f : this)
		{
			// If the filter typename already exists in the filterset, then only
			// add
			// the values ot the value set
			if(f.getType().equals(type) && f.getName().equals(name))
			{
				for (String value : filter.getValues())
				{
					f.addValue(value);
				}
				hasAdded = true;
				break;
			}
			// Else add the whole filter to the filterset
			else
			{}
		}
		if(!hasAdded)
			this.add(filter);
	}
	
	/**
	 * Remove a filter and all it's values to the filterset
	 */
	public void removeFilter(Filter filter)
	{
		if(filter == null)
			return;
		Type type = filter.getType();
		String name = filter.getName();
		
		// Make list of filters to clean up if empty
		java.util.List<Filter> sweep = new Vector<Filter>(0);
		
		for (Filter f : this)
		{
			// If the filter typename already exists in the filterset, then only
			// remove
			// the values ot the value set
			if(f.getType().equals(type) && f.getName().equals(name))
			{
				for (String value : filter.getValues())
				{
					f.removeValue(value);
					if(f.getValues().size() == 0)
						sweep.add(f);
				}
			}
			// Else do nothing
			else
			{}
		}
		
		for (Filter f : sweep)
		{
			// if (f.getValues().size() == 0) this.remove(f);
			this.remove(f);
		}
	}
	
	// ---------------------------------------------
	// View
	// ---------------------------------------------
	/**
	 * Print the filter set
	 */
	public void print()
	{
		Logs.log("-----------------", 1, this);
		Logs.log("Filter Set output", 1, this);
		
		for (Filter f : this)
		{
			// Get the fields of the filter
			Type type = f.getType();
			String name = f.getName();
			Set<String> values = f.getValues();
			Logs.log("   Filter 1: TYPE=" + type + " NAME=" + name + " VALUES=" + values.toString(), 1, this);
		}
		
		Logs.log("-----------------", 1, this);
	}
}
