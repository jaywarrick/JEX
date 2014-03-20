package Database.Definition;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import miscellaneous.StringUtility;
import Database.DBObjects.JEXData;

public class Filter implements Comparable<Filter> {
	
	private Type type;
	private String name;
	private Set<String> values;
	
	// ---------------------------------------------
	// Creators
	// ---------------------------------------------
	
	public Filter(Type type, String name, Set<String> values)
	{
		this.type = type;
		this.name = name;
		this.values = values;
	}
	
	public Filter(Type type, String name, String value)
	{
		this.type = type;
		this.name = name;
		this.values = new TreeSet<String>();
		this.values.add(value);
	}
	
	// ---------------------------------------------
	// Getter / Setters
	// ---------------------------------------------
	/**
	 * Set the type field
	 * 
	 * @param type
	 */
	public void setType(Type type)
	{
		this.type = type;
	}
	
	/**
	 * Set the name field
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Set the value field
	 * 
	 * @param value
	 */
	public void setValues(Set<String> values)
	{
		this.values = values;
	}
	
	/**
	 * Add a value to the value set
	 * 
	 * @param value
	 */
	public void addValue(String value)
	{
		if(this.values == null)
		{
			this.values = new TreeSet<String>();
		}
		this.values.add(value);
	}
	
	/**
	 * Remove a value from the value set
	 * 
	 * @param value
	 */
	public void removeValue(String value)
	{
		if(this.values == null)
		{
			return;
		}
		this.values.remove(value);
	}
	
	/**
	 * Return the type field
	 * 
	 * @return type
	 */
	public Type getType()
	{
		return this.type;
	}
	
	/**
	 * Return the name field
	 * 
	 * @return name
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Return the value field
	 * 
	 * @return value
	 */
	public Set<String> getValues()
	{
		return this.values;
	}
	
	/**
	 * Return the first value of the values set
	 * 
	 * @return string
	 */
	public String getValue()
	{
		if(this.values == null || this.values.size() == 0)
		{
			return null;
		}
		String value = this.getValues().iterator().next();
		return value;
	}
	
	// ---------------------------------------------
	// Methods
	// ---------------------------------------------
	/**
	 * Return true if and only if the type, name and value fields of both filters match
	 * 
	 * @return true or false
	 */
	public boolean equals(Filter filter)
	{
		// if the type and name do not match return false
		if(!(this.getType() != null && this.getType().equals(filter.getType())))
		{
			return false;
		}
		if(!(this.getName() != null && this.getName().equals(filter.getName())))
		{
			return false;
		}
		
		// if the value set filtered on do not match then return false
		// if both sets are null return true
		// if either set is null or the sets are not the same size return false
		Set<String> otherValues = filter.getValues();
		if(this.values == null && otherValues == null)
		{
			return true;
		}
		else if(this.values == null || otherValues == null)
		{
			return false;
		}
		else if(this.values.size() != otherValues.size())
		{
			return false;
		}
		
		// if all values in this value set match a value in the other
		// and since the two sets are the same size, then they match
		for (String str : this.values)
		{
			if(!otherValues.contains(str))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns a low level copy of the filter
	 * 
	 * @return duplicate filter
	 */
	public Filter duplicate()
	{
		// make a low level copy of the value set
		HashSet<String> valueSet = new HashSet<String>();
		for (String str : this.values)
		{
			valueSet.add(str);
		}
		
		// create a duplicate of the filter
		Filter result = new Filter(this.getType(), this.getName(), valueSet);
		return result;
	}
	
	/**
	 * Return true if and only if the object matches the filter
	 * 
	 * @return
	 */
	public boolean matches(JEXData object)
	{
		// TODO
		return false;
	}
	
	/**
	 * Compare to other filter
	 */
	@Override
	public int compareTo(Filter o)
	{
		Filter f = o;
		int typeCompare = f.getType().compareTo(this.getType());
		if(typeCompare == 0)
		{
			
			int nameCompare = f.getName().compareTo(this.getName());
			if(nameCompare == 0)
			{
				
				int valueCompare = StringUtility.compareString(this.getValue(), f.getValue());
				return valueCompare;
				// int valueCompare = f.getValue().compareTo(this.getValue());
				// return valueCompare;
				
			}
			else
			{
				return nameCompare;
			}
			
		}
		else
		{
			return typeCompare;
		}
	}
	
}
