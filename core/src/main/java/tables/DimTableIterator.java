package tables;

import java.util.Iterator;
import java.util.Vector;

public class DimTableIterator implements Iterator<DimTable> {
	
	DimTable table;
	String dimName;
	Iterator<String> itr;

	/**
	 * Class constructor 
	 * create a String type iterator of dimValues (Vector<String>)
	 * 
	 * @param table DimTable 
	 * @param dimName DimName
	 */
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
