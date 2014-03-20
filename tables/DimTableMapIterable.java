package tables;

public class DimTableMapIterable implements Iterable<DimensionMap> {
	
	private DimTable dimTable;
	private DimensionMap filter;
	private int skipN;
	
	public DimTableMapIterable(DimTable dimTable)
	{
		this(dimTable, new DimensionMap(), 0);
	}
	
	public DimTableMapIterable(DimTable dimTable, DimensionMap filter)
	{
		this(dimTable, filter, 0);
	}
	
	public DimTableMapIterable(DimTable dimTable, DimensionMap filter, int skipN)
	{
		this.dimTable = dimTable;
		if(filter == null)
		{
			this.filter = new DimensionMap();
		}
		else
		{
			this.filter = filter;
		}
		this.skipN = skipN;
	}
	
	public DimTableMapIterator iterator()
	{
		return new DimTableMapIterator(this.dimTable, this.filter, this.skipN);
	}
}
