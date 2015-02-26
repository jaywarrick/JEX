package tables;

import java.util.Iterator;

public class DimTableIterable implements Iterable<DimTable>{
	
	DimTable table;
	String dimName;

	public DimTableIterable(DimTable table, String dimName)
	{
		this.table = table;
		this.dimName = dimName;
	}
	
	@Override
	public Iterator<DimTable> iterator()
	{
		// an iterator of DimValues given this DimName in this DimTable
		return new DimTableIterator(this.table, this.dimName);
	}

}
