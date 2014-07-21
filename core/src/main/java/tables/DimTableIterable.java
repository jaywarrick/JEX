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
		return new DimTableIterator(this.table, this.dimName);
	}

}
