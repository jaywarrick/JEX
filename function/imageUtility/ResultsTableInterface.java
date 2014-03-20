package function.imageUtility;

import ij.measure.ResultsTable;

import java.io.IOException;

import miscellaneous.CSVList;
import miscellaneous.LSVList;
import miscellaneous.TSVList;

public class ResultsTableInterface {
	
	private ResultsTable rt;
	
	public ResultsTableInterface(ResultsTable rt)
	{
		this.rt = rt;
	}
	
	public ResultsTable resultsTable()
	{
		return this.rt;
	}
	
	public TSVList headings()
	{
		TSVList col_headings = new TSVList(rt.getColumnHeadings());
		return col_headings;
	}
	
	public LSVList LSVTSVTable(boolean withHeader)
	{
		LSVList table = new LSVList();
		if(withHeader)
		{
			table.add(this.headings().toString());
		}
		for (int r = 0; r < this.rows(); r++)
		{
			table.add(rt.getRowAsString(r));
		}
		return table;
	}
	
	public LSVList LSVCSVTable(boolean withHeader)
	{
		LSVList table = new LSVList();
		CSVList newHeadings = new CSVList();
		for (String header : this.headings())
		{
			header.replace(',', ';');
			newHeadings.add(header);
		}
		
		if(withHeader)
		{
			table.add(this.headings().toString());
		}
		for (int r = 0; r < this.rows(); r++)
		{
			table.add(this.getRowCSV(r).toString());
		}
		return table;
	}
	
	public int cols()
	{
		return this.headings().size();
	}
	
	public int rows()
	{
		return rt.getCounter() - 1;
	}
	
	public TSVList getRow(int row)
	{
		return new TSVList(rt.getRowAsString(row));
	}
	
	public CSVList getRowCSV(int row)
	{
		CSVList ret = new CSVList();
		for (int c = 0; c < this.cols(); c++)
		{
			ret.add("" + rt.getValueAsDouble(c, row));
		}
		return ret;
	}
	
	public LSVList getCol(int col, boolean withHeader)
	{
		double[] values = rt.getColumnAsDoubles(col);
		LSVList ret = new LSVList();
		if(col < 0 || col > this.cols())
			return ret;
		if(withHeader)
		{
			ret.add(rt.getColumnHeading(col));
		}
		for (double val : values)
		{
			ret.add("" + val);
		}
		return ret;
	}
	
	public LSVList getCol(String col, boolean withHeader)
	{
		return this.getCol(rt.getColumnIndex(col), withHeader);
	}
	
	public void saveCSV(String fullPathWithoutExtension)
	{
		try
		{
			rt.saveAs(fullPathWithoutExtension + ".csv");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
