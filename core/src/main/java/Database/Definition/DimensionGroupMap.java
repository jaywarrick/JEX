package Database.Definition;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class DimensionGroupMap extends HashMap<TypeName,String> implements Comparable<DimensionGroupMap> {
	
	private static final long serialVersionUID = 1L;
	
	public DimensionGroupMap()
	{
		super();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof DimensionGroupMap))
			return false;
		DimensionGroupMap d = (DimensionGroupMap) o;
		int compare = this.compareTo(d);
		if(compare == 0)
			return true;
		return false;
		// if (o instanceof DimensionGroupMap){
		// DimensionGroupMap other = (DimensionGroupMap)o;
		// return super.equals(other);
		// }
		// return false;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	
	public TypeName[] getDimensionArray()
	{
		TypeName[] result = new TypeName[this.size()];
		Set<TypeName> keys = this.keySet();
		int index = 0;
		for (TypeName key : keys)
		{
			result[index] = key;
			index++;
		}
		return result;
	}
	
	public boolean isDimension(TypeName dim)
	{
		Set<TypeName> keys = this.keySet();
		for (TypeName key : keys)
		{
			if(dim.equals(key))
				return true;
		}
		return false;
	}
	
	public LinkedHashSet<TypeName> getDimensionSet()
	{
		LinkedHashSet<TypeName> result = new LinkedHashSet<TypeName>();
		Set<TypeName> keys = this.keySet();
		for (TypeName key : keys)
		{
			result.add(key);
		}
		return result;
	}
	
	/**
	 * Return a low level copy of the dimensionhashmap
	 * 
	 * @return dimensionhashmap
	 */
	public DimensionGroupMap duplicate()
	{
		DimensionGroupMap result = new DimensionGroupMap();
		
		Set<TypeName> keys = this.keySet();
		for (TypeName key : keys)
		{
			TypeName keyCopy = key.duplicate();
			result.put(keyCopy, this.get(key));
		}
		
		return result;
	}
	
	/**
	 * Return a multiline string of this dimension group map
	 * 
	 * @return
	 */
	public String toMultiLineString()
	{
		String result = "";
		int index = 0;
		for (TypeName tn : this.keySet())
		{
			String value = this.get(tn);
			// if (index==this.size()-1) result = result + tn.getName() + "-" + tn.getType() + " = " + value;
			// else result = result + tn.getName() + "-" + tn.getType() + " = " + value + "\n";
			if(index == this.size() - 1)
				result = result + tn.getName() + " = " + value;
			else
				result = result + tn.getName() + " = " + value + "\n";
			index++;
		}
		return result;
	}
	
	/**
	 * Return a multiline string of this dimension group map
	 * 
	 * @return
	 */
	@Override
	public String toString()
	{
		String result = "";
		int index = 0;
		
		for (TypeName tn : this.keySet())
		{
			String value = this.get(tn);
			if(index == this.size() - 1)
				result = result + tn.getName() + "-" + tn.getType() + " = " + value;
			else
				result = result + tn.getName() + "-" + tn.getType() + " = " + value + "; ";
			index++;
		}
		
		return result;
	}
	
	/**
	 * Return a comparaison with another DimensionMap
	 */
	public int compareTo(DimensionGroupMap o)
	{
		DimensionGroupMap d = o;
		
		for (TypeName tn : this.keySet())
		{
			String value1 = this.get(tn);
			String value2 = d.get(tn);
			if(value1 == null)
				return -1;
			if(value2 == null)
				return 1;
			int compare = value1.compareTo(value2);
			if(compare != 0)
				return compare;
		}
		// Set<TypeName> keys = this.keySet();
		// for (TypeName key: keys){
		// String value1 = this.get(key);
		// if (value1 == null) return -1;
		// String value2 = d.get(key);
		// if (value2 == null) return 1;
		//
		// int compare = value1.compareTo(value2);
		// if (compare > 0) return 1;
		// if (compare < 0) return -1;
		// }
		
		return 0;
	}
}
