package miscellaneous;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;

public class JEXCSVReader {

	CSVReader reader;
	boolean hasHeaderRow;
	ArrayList<String> header;

	public JEXCSVReader(String path, boolean hasHeaderRow)
	{
		try
		{
			this.header = null;
			this.reader = new CSVReader(new FileReader(new File(path)));
			this.hasHeaderRow = hasHeaderRow;
			if(this.hasHeaderRow)
			{
				this.header = this.readRowToArrayList();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isEOF()
	{
		return this.reader.isEOF();
	}
	
	public ArrayList<String> getHeaderAsList()
	{
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(this.header);
		return ret;
	}

	public ArrayList<String> readRowToArrayList()
	{
		if(this.isEOF())
		{
			return null;
		}
		
		try {
			ArrayList<String> fields = new ArrayList<String>();
			reader.readFields(fields);
			return fields;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Pair<DimensionMap,String> readRowToDimensionMapString()
	{
		if(!hasHeaderRow)
		{
			Logs.log("Unable to read to DimensionMap without header in file.", this);
			return null;
		}
		
		ArrayList<String> temp = this.readRowToArrayList();
		if(temp == null || temp.size()!=header.size())
		{
			return null;
		}
		
		DimensionMap map = new DimensionMap();
		for(int i = 0; i < this.header.size() - 1; i++)
		{
			map.put(header.get(i), temp.get(i));
		}
		
		Pair<DimensionMap, String> ret = new Pair<DimensionMap,String>(map, temp.get(this.header.size()-1));
		return ret;
	}
	
	public static TreeMap<DimensionMap,String> getCSVTable(String csvPath, boolean hasHeaderRow)
	{
		JEXCSVReader reader = new JEXCSVReader(csvPath, true);

		reader = new JEXCSVReader(csvPath, true);
		TreeMap<DimensionMap,String> ret = new TreeMap<>();
		while(!reader.isEOF())
		{
			miscellaneous.Pair<DimensionMap,String> result = reader.readRowToDimensionMapString();
			if(result != null)
			{
				ret.put(result.p1, result.p2);
			}
		}
		
		reader.close();
		return ret;
	}
	
	public void close()
	{
		try
		{
			this.reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}

