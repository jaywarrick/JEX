package tables;

import java.util.Iterator;
import java.util.Vector;

public class DimTableIterator implements Iterator<DimTable> {
	
	DimTable table;
	String dimName;
	boolean dimIsBlank = false;
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
			Vector<String> temp = new Vector<>();
			temp.add("temp");
			itr = temp.iterator();
			dimIsBlank = true;
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
		if(dimIsBlank)
		{
			itr.next();
			return table;
		}
		else
		{
			return table.getSubTable(new DimensionMap(dimName + "=" + itr.next()));
		}
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
