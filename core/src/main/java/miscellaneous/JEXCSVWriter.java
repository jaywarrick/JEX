package miscellaneous;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

public class JEXCSVWriter {
	
	public String path;
	public CSVWriter writer;
	
	public JEXCSVWriter()
	{
		try
		{
			this.path = DirectoryManager.getUniqueAbsoluteTempPath("csv");
			this.writer = new CSVWriter(new FileWriter(new File(this.path)));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public JEXCSVWriter(String path)
	{
		try
		{
			this.path = path;
			this.writer = new CSVWriter(new FileWriter(new File(this.path)));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void write(DimensionMap map, String value)
	{
		String[] row = new String[map.size() + 1];
		int i = 0;
		for(Entry<String,String> e : map.entrySet())
		{
			row[i] = e.getValue();
			i = i + 1;
		}
		row[map.size()] = value;
		
		this.write(row);
	}
	
	public static String write(TreeMap<DimensionMap,String> tableData)
	{
		JEXCSVWriter writer = new JEXCSVWriter();
		writer.writeHeader(tableData.firstKey());
		for(Entry<DimensionMap,String> e : tableData.entrySet())
		{
			writer.write(e.getKey(), e.getValue());
		}
		String ret = writer.getPath();
		writer.close();
		return ret;
	}
	
	/**
	 * Will write header of with values equal to keys of the map. It will
	 * also add a "VALUE" column based on JEXTableWriter.VALUE.
	 * @param map
	 */
	public void writeHeader(DimensionMap map)
	{
		String[] row = new String[map.size() + 1];
		
		int i = 0;
		for(Entry<String,String> e : map.entrySet())
		{
			row[i] = e.getKey();
			i = i + 1;
		}
		row[map.size()] = JEXTableWriter.VALUE;
		this.write(row);
	}
	
	public void writeHeader(DimTable dimTable)
	{
		// If we do it this way then the order of the dims will match the order in the DimensionMaps
		// when a row is written.
		this.writeHeader(dimTable.getMapIterator().iterator().next());
	}
	
	public void write(List<String> rowOfInfo)
	{
		try
		{
			this.writer.writeFields(rowOfInfo);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void write(String[] rowOfInfo)
	{
		try
		{
			this.writer.writeFields(rowOfInfo);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public String getPath()
	{
		return this.path;
	}
	
	public void close()
	{
		try
		{
			if(this.writer != null)
			{
				this.writer.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
