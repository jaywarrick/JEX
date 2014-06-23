package tables;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import miscellaneous.CSVList;
import miscellaneous.Copiable;
import miscellaneous.StringUtility;

public class DimensionMap extends TreeMap<String,String> implements Comparable<DimensionMap>, Copiable<DimensionMap> {
	
	private static final long serialVersionUID = 1L;
	
	public DimensionMap()
	{
		super(new StringUtility());
	}
	
	public DimensionMap(String csvDimStr)
	{
		super();
		
		CSVList csv = new CSVList(csvDimStr);
		for (String s : csv)
		{
			String[] dimNameValue = s.split("=");
			if(dimNameValue.length < 2)
			{
				continue;
			}
			
			this.put(dimNameValue[0], dimNameValue[1]);
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof DimensionMap)
		{
			DimensionMap other = (DimensionMap) o;
			return super.equals(other);
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	
	@Override
	public String toString()
	{
		CSVList l = new CSVList();
		for (Entry<String,String> e : this.entrySet())
		{
			l.add(e.getKey() + "=" + e.getValue());
		}
		return l.toString();
	}
	
	public String[] getDimensionArray()
	{
		String[] result = new String[this.size()];
		Set<String> keys = this.keySet();
		int index = 0;
		for (String key : keys)
		{
			result[index] = key;
			index++;
		}
		return result;
	}
	
	public boolean isDimension(String dim)
	{
		Set<String> keys = this.keySet();
		for (String key : keys)
		{
			if(dim.equals(key))
			{
				return true;
			}
		}
		return false;
	}
	
	public LinkedHashSet<String> getDimensionSet()
	{
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		Set<String> keys = this.keySet();
		for (String key : keys)
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
	@Override
	public DimensionMap copy()
	{
		DimensionMap result = new DimensionMap();
		
		Set<String> keys = this.keySet();
		for (String key : keys)
		{
			result.put(key, this.get(key));
		}
		
		return result;
	}
	
	/**
	 * Return a low level copy of the dimensionhashmap
	 * 
	 * @return dimensionhashmap
	 */
	public DimensionMap copyAndSet(String csvString)
	{
		DimensionMap temp = new DimensionMap(csvString);
		DimensionMap result = new DimensionMap();
		Set<String> keys = this.keySet();
		for (String key : keys)
		{
			result.put(key, this.get(key));
		}
		result.putAll(temp);
		return result;
	}
	
	/**
	 * Returns a string displaying the dimensionshap map with KEYSIZE number of characters for the labels and VALUESIZE number of characters for the values
	 * 
	 * @param keySize
	 * @param valueSize
	 * @return string
	 */
	public String getLine(int keySize, int valueSize)
	{
		String result = "";
		for (String key : this.keySet())
		{
			String keyStr = key;
			String calStr = this.get(key);
			result = result + StringUtility.fillLeft(keyStr, keySize, " ") + StringUtility.fillLeft(calStr, valueSize, " ");
		}
		return result;
	}
	
	/**
	 * Return a comparaison with another DimensionMap. This DimensionMap can be overdefined compared to d and still return 0. Thus, A = B doesn't necessarily mean B = A but having this behavior allows us to access items in maps sorted by
	 * DimensionMaps with overdefined DimensionMaps. In other words, if we have an array of rois on a single image, we can use the DimensionMap of the rois to access the appropriate image in the imageSet by simply using the get method for a
	 * TreeMap<DimensionMap,String>.
	 */
	@Override
	public int compareTo(DimensionMap d)
	{
		
		// If both DimensionMaps are empty, return 0
		if(this.size() == 0 && d.size() == 0)
		{
			return 0;
		}
		
		// If this DimensionMap is underdefined compared to d, return -1
		if(this.size() < d.size())
		{
			return -1;
		}
		
		// This has more keys that d so check to see if all the keys in d are
		// contained in this. If not, compare the keys (not values) of each
		// sorted alhpanumerically
		for (String key : d.keySet())
		{
			if(this.get(key) == null)
			{
				return this.compareKeys(this, d);
			}
		}
		
		// Otherwise all the keys in d are contained in this and we can compare
		// each of the values for the matching keys in a predefined order (i.e.
		// alhpanumeric sort of the d keys)
		return this.compareValues(d);
		
	}
	
	private int compareValues(DimensionMap that)
	{
		// Otherwise all the keys in d are contained in this and we can compare
		// each of the values for the matching keys in a predefined order (i.e.
		// alhpanumeric sort of the d keys)
		for (Entry<String,String> e : that.entrySet())
		{
			int comparison = StringUtility.compareString(this.get(e.getKey()), e.getValue());
			if(comparison > 0)
			{
				return 1;
			}
			if(comparison < 0)
			{
				return -1;
			}
		}
		
		// all the keys of d are contained in this map and the values for those
		// matching keys also matched
		return 0;
	}
	
	private int compareKeys(DimensionMap thisMap, DimensionMap thatMap)
	{
		StringBuffer thisString = new StringBuffer(), thatString = new StringBuffer();
		
		// When compiling the strings of the keys, add a character between the
		// key names for more robust comparison (i.e. so keys "A,BC" don't
		// result in the same string ABC as "AB,C" to end up looking equal)
		for (String key : thisMap.keySet())
		{
			thisString.append("_").append(key);
		}
		for (String key : thatMap.keySet())
		{
			thatString.append("_").append(key);
		}
		return StringUtility.compareString(thisString.toString(), thatString.toString());
	}
}
