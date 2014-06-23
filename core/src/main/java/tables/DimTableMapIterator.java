package tables;

import java.util.Iterator;
import java.util.Vector;

public class DimTableMapIterator implements Iterator<DimensionMap> {
	
	private DimTable dimTable;
	private Vector<Integer> index;
	private DimensionMap filter;
	private boolean firstIteration;
	private int skipN;
	
	public DimTableMapIterator(DimTable dimTable, DimensionMap filter, int skipN)
	{
		this.firstIteration = true;
		this.filter = filter;
		this.index = new Vector<Integer>();
		this.dimTable = dimTable;
		this.skipN = skipN;
		for (Dim dim : dimTable)
		{
			if(filter.get(dim.name()) != null)
			{
				index.add(dim.index(filter.get(dim.name())));
			}
			else
			{
				index.add(0);
			}
		}
		// System.out.println("Created DimTableIterator");
	}
	
	public boolean hasNext()
	{
		return (this.getNextIndex() != null);
	}
	
	public DimensionMap next()
	{
		this.index = this.getNextIndex();
		if(this.index == null)
		{
			return null;
		}
		DimensionMap ret = new DimensionMap();
		int count = 0;
		for (Integer i : this.index)
		{
			String dimName = this.dimTable.get(count).name();
			String dimValue = this.dimTable.get(count).valueAt(i);
			ret.put(dimName, dimValue);
			count++;
		}
		if(this.firstIteration)
		{
			this.firstIteration = false;
		}
		return ret;
	}
	
	public int currentRow()
	{
		int ret = 0;
		for (int i = 0; i < this.dimTable.size(); i++)
		{
			int temp = 1;
			for (int j = i + 1; j < this.dimTable.size(); j++)
			{
				temp = temp * this.dimTable.get(j).size();
			}
			ret = ret + temp * this.index.get(i);
		}
		return ret;
	}
	
	private Vector<Integer> getNextIndex()
	{
		Vector<Integer> ret = new Vector<Integer>();
		for (Integer i : this.index)
		{
			int temp = i; // To make sure we aren't copying the pointer and have simultaneous editing of a single Integer in two vectors.
			ret.add(temp);
		}
		if(this.firstIteration) // first set of indicies to return is the index array as it was initialized.
		{
			return ret;
		}
		if(!this.firstIteration && ret.size() == 0)
		{
			return null;
		}
		for (int skip = 0; skip <= skipN; skip++)
		{
			for (int i = dimTable.size() - 1; i > -1; i--)
			{
				Dim dim = this.dimTable.get(i);
				Integer n = ret.get(i);
				if(filter.get(dim.name()) != null)
				{
					if(i == 0)
					{
						return null;
					}
					else
					{
						continue;
					}
				}
				else if(n < dim.size() - 1)
				{
					ret.setElementAt((n + 1), i);
					break;
				}
				else if(i > 0)
				{
					ret.setElementAt(0, i);
					continue;
				}
				else
				{
					return null;
				}
			}
			// JEXStatics.logManager.log(skip + " " + skipN + " " + ret.toString(), 0, this);
		}
		return ret;
	}
	
	public void remove()
	{
		// TODO Auto-generated method stub
		
	}
	
}
