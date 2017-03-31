package tables;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import miscellaneous.StringUtility;

public class DimTableBuilder {
	
	TreeMap<String,TreeSet<String>> table;
	StringUtility sorter;
	
	public DimTableBuilder()
	{
		sorter = new StringUtility();
		table = new TreeMap<String,TreeSet<String>>(sorter);		
	}
	
	/**
	 * Order not guaranteed
	 * @param map
	 */
	public void add(DimensionMap map)
	{
		for(Entry<String,String> e : map.entrySet())
		{
			TreeSet<String> temp = this.table.get(e.getKey());
			if(temp == null)
			{
				temp = new TreeSet<String>(sorter);
				this.table.put(e.getKey(), temp);
			}
			temp.add(e.getValue());
		}
	}
	
	public DimTable getDimTable()
	{
		DimTable ret = new DimTable();
		for(Entry<String,TreeSet<String>> e : this.table.entrySet())
		{
			Dim toAdd = new Dim(e.getKey(), e.getValue());
			ret.add(toAdd);
		}
		return ret;
	}

}
