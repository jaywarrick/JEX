package tables;

import java.util.Iterator;
import java.util.Vector;

/**
 * 
 * Iterator through dimTable return DimensionMap
 * 
 * @author Jay Warrick, commented by Mengcheng
 *
 */
public class DimTableMapIterator implements Iterator<DimensionMap> {
	
	private DimTable dimTable;
	private Vector<Integer> index;
	private DimensionMap filter;
	private boolean firstIteration;
	private int skipN;
	
	/**
	 * Iterator through dimTable return DimensionMap
	 * 
	 * @param dimTable DimTable
	 * @param filter DimensionMap
	 * @param skipN Number of iterations that user want to skip
	 */
	public DimTableMapIterator(DimTable dimTable, DimensionMap filter, int skipN)
	{
		this.firstIteration = true;
		this.filter = filter;
		this.index = new Vector<Integer>();
		this.dimTable = dimTable;
		this.skipN = skipN;
		for (Dim dim : dimTable)
		{
			// check if DimName in the DimTable is in given DimensionMap
			if(filter.get(dim.name()) != null)
			{
				// if yes, record the index of filtered value
				index.add(dim.index(filter.get(dim.name())));
			}
			else
			{
				index.add(0);
			}
		}
		// System.out.println("Created DimTableIterator");
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext()
	{
		return (this.getNextIndex() != null);
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
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
	
	/**
	 * If we have a index as following (bracket means filtered)
	 *  Dim0 Dim1 Dim2 Dim3 Dim4
	 * | 0  |[1] |[0] | 2  | 3  |
	 * 
	 * assume max Dimvalue index of Dim0 = 5, Dim3 = 2, Dim4 = 3
	 * 
	 * Then the returned index as following
	 *  Dim0 Dim1 Dim2 Dim3 Dim4
	 * | 1  |[1] |[0] | 0  | 0  |
	 * Dim3 Dim4 get wrap around, Dim1 Dim2 get fixed since filtered, Dim0 incremented by one
	 * 
	 * @return updated index
	 */
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
		for (int skip = 0; skip <= skipN; skip++) // skip next skipN items
		{
			// iterate through whole dimTable from bottom to top
			for (int i = dimTable.size() - 1; i > -1; i--)
			{
				Dim dim = this.dimTable.get(i); // get Dim
				Integer n = ret.get(i); // get current index of DimValue
				
				// if current Dim is included in the filter 
				if(filter.get(dim.name()) != null)
				{
					if(i == 0) // skip the last Dim in the dimTable 
					{
						return null;
					}
					else // skip current Dim
					{
						continue;
					}
				}
				
				// if current Dim is not included in the filter 
				else if(n < dim.size() - 1) // increment Index by one if not exceed the boundary
				{
					ret.setElementAt((n + 1), i);
					break; // like a counting clk, each function call the index is incremented by 1
				}
				else if(i > 0) // n >= dim.size() - 1, wrap around
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
