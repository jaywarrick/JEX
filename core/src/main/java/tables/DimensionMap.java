package tables;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import miscellaneous.CSVList;
import miscellaneous.Copiable;
import miscellaneous.StringUtility;

/**
 * DimensionMap is a TreeMap that contains variableName, value, e.g. "color", "1"
 *
 * @author Jay Warrick, commented by Jay Warrick and Mengcheng
 *
 */
public class DimensionMap extends TreeMap<String,String> implements Comparable<DimensionMap>, Copiable<DimensionMap> {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * super TreeMap constructor
	 */
	public DimensionMap()
	{
		super(new StringUtility());
	}
	
	/**
	 * Class constructor specifying a CSV string
	 * 
	 * @param csvDimStr input to instance a CSVList
	 */
	public DimensionMap(String csvDimStr)
	{
		super(); // instance a TreeMap<String, String>
		
		CSVList csv = new CSVList(csvDimStr);
		// each csv contains a variable = a certain value
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
	
	/* (non-Javadoc)
	 * @see java.util.AbstractMap#equals(java.lang.Object)
	 */
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
	
	/* (non-Javadoc)
	 * @see java.util.AbstractMap#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractMap#toString()
	 */
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
	
	/**
	 * Get DimensionMap to array
	 * 
	 * @return a string array contains dims
	 */
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
	
	/**
	 * Return true if input is a dim included in DimensionMap
	 * 
	 * @param dim check whether this is a dim
	 * @return true if input is a dim included in DimensionMap
	 */
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
	
	/**
	 * Get DimensionMap to LinkedHashSet
	 * 
	 * @return a LinkedHashSet contains dims
	 */
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
	 * Return a low level copy of the dimensionhashmap from a cvsString
	 * 
	 * @param csvString csvString
	 * @return DimensionMap DimensionMap
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
	 * Returns a string displaying the dimension map with KEYSIZE number of characters for the labels and VALUESIZE number of characters for the values
	 * 
	 * @param keySize keySize
	 * @param valueSize valueSize
	 * @return string Returns a string displaying the dimension map with KEYSIZE number of characters for the labels and VALUESIZE number of characters for the values
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
	 * TreeMap with DimensionMap key and String value.
	 */
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
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
	
	/**
	 * Compare the values of two DimensionMaps 
	 * 
	 * @param that
	 * @return
	 */
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
	
	/**
	 * Compare the keys of two DimensionMaps 
	 * 
	 * @param thisMap
	 * @param thatMap
	 * @return
	 */
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
