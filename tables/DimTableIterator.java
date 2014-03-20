package tables;

import java.util.Iterator;
import java.util.Vector;

public class DimTableIterator implements Iterator<DimTable> {
	
	DimTable table;
	String dimName;
	Iterator<String> itr;

	public DimTableIterator(DimTable table, String dimName)
	{
		this.table = table;
		this.dimName = dimName;
		Dim dimToIterate = this.table.getDimWithName(dimName);
		if(dimToIterate == null)
		{
			itr = (new Vector<String>()).iterator();
		}
		else
		{
			this.itr = table.getDimWithName(dimName).values().iterator();
		}
	}
	
	@Override
	public boolean hasNext()
	{
		return itr.hasNext();
	}

	@Override
	public DimTable next()
	{
		return table.getSubTable(new DimensionMap(dimName + "=" + itr.next()));
	}

	/**
	 * Not implemented.
	 */
	@Override
	public void remove()
	{
		return;
	}


}
