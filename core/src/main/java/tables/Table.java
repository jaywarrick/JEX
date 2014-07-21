package tables;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import miscellaneous.Canceler;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;

public class Table<E> implements Iterable<DimensionMap> {
	
	public DimTable dimTable;
	public TreeMap<DimensionMap,E> data;
	
	public Table(DimTable dimTable, TreeMap<DimensionMap,E> table)
	{
		this.dimTable = dimTable;
		this.data = table;
	}
	
	@Override
	public Iterator<DimensionMap> iterator()
	{
		return this.dimTable.getMapIterator().iterator();
	}
	
	public DimTableMapIterable getIterator(DimensionMap filter)
	{
		return this.dimTable.getMapIterator(filter);
	}
	
	public E getData(DimensionMap key)
	{
		return this.data.get(key);
	}
	
	public TreeMap<DimensionMap,E> getFilteredData(String filter)
	{
		return this.getFilteredData(new DimensionMap(filter));
	}
	
	public TreeMap<DimensionMap,E> getFilteredData(DimensionMap filter)
	{
		TreeMap<DimensionMap,E> ret = new TreeMap<DimensionMap,E>();
		for (DimensionMap map : this.dimTable.getMapIterator(filter))
		{
			E val = this.data.get(map);
			if(val != null)
			{
				ret.put(map, val);
			}
		}
		return ret;
	}
	
	public static <B> TreeMap<DimensionMap,B> getFilteredData(TreeMap<DimensionMap,B> data, DimensionMap filter)
	{
		return JEXTableReader.filter(data, filter);
	}
	
	public static <B> TreeMap<DimensionMap,B> getFilteredData(TreeMap<DimensionMap,B> data, String filter)
	{
		return JEXTableReader.filter(data, new DimensionMap(filter));
	}
	
	public static String joinTables(Table<String> fileTable, Canceler canceler)
	{
		DimTable union = fileTable.dimTable;
		for(Entry<DimensionMap,String> e : fileTable.data.entrySet())
		{
			DimTable lower = JEXTableReader.getDimTable(e.getValue());
			union = DimTable.union(union,lower);
		}
		JEXTableWriter writer = new JEXTableWriter("UnionTable");
		writer.writeNumericTableHeader(union);
		
		for(Entry<DimensionMap,String> e : fileTable.data.entrySet())
		{
			if(canceler.isCanceled())
			{
				return null;
			}
			Table<Double> data = JEXTableReader.getNumericTable(e.getValue());
			for(Entry<DimensionMap,Double> e2 : data.data.entrySet())
			{
				writer.writeData(e2.getKey(), e2.getValue());
			}
		}
		String path = writer.getPath();
		writer.close();
		
		return path;
	}
	
	public static Table<String> joinAndSplitTables(Table<String> fileTable, String timeDimName, Canceler canceler)
	{
		String path = Table.joinTables(fileTable, canceler);
		
		Table<String> timeFiles = JEXTableReader.splitTable(path, timeDimName, "TimePoint", JEXTableWriter.ARFF_FILE, canceler);
		return timeFiles;
	}
	
}
