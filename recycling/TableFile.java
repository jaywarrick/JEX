package recycling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.CSVReader;
import miscellaneous.CSVWriter;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.Instances;
import weka.core.converters.JEXTableWriter;

public class TableFile {
	
	private String path;
	private DimTable dimTable = null;
	private List<String> header;
	private int firstDataRow = 2;
	
	public TableFile(String path)
	{
		this.path = path;
		this.initialize();
	}
	
	public String getPath()
	{
		return this.path;
	}
	
	private void initialize()
	{
		// Create the dimTable and record the row where data begins (first row
		// in file is row 0).
		this.dimTable = new DimTable();
		ArrayList<String> fields = new ArrayList<String>();
		int fileRowCount = -1;
		try
		{
			final CSVReader csvReader = new CSVReader(new FileReader(this.path));
			while (!csvReader.isEOF())
			{
				fileRowCount++;
				csvReader.readFields(fields);
				System.out.println(fields.toString());
				if(fields.size() > 1) // Reading dimTable
				{
					Dim dim = makeDim(fields);
					if(dim != null)
					{
						this.dimTable.add(dim);
					}
				}
				else
				{
					this.firstDataRow = fileRowCount + 2;
					break;
				}
			}
			this.header = this.dimTable.getDimensionNames();
			this.header.add(JEXTableWriter.VALUE);
			csvReader.close();
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a copy of the dimTable so that the Table doesn't get screwed up by accidentally editing the Table's copy
	 * 
	 * @return
	 */
	public DimTable getDimTable()
	{
		return this.dimTable.copy();
	}
	
	/**
	 * Get a subset of the data within this file to keep memory usage low. The DimensionMap filter should not be null. If you want the whole table then use a new empty dimension map (i.e. new DimensionMap()) or use the TableFile.getTable() method
	 * with no filter argument.
	 * 
	 * @param filter
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Table getData(DimensionMap filter)
	{
		ArrayList<String> fields = new ArrayList<String>();
		TreeMap<DimensionMap,String> table = new TreeMap<DimensionMap,String>();
		int fileRowCount = 0;
		boolean foundMatch = false;
		try
		{
			final CSVReader csvReader = new CSVReader(new FileReader(this.path));
			while (!csvReader.isEOF() && fileRowCount < this.firstDataRow)
			{
				System.out.println(csvReader.readLine());
				fileRowCount++;
			}
			for (DimensionMap map : this.dimTable.getMapIterator(filter))
			{
				// Read until you find this map
				foundMatch = false;
				while (!csvReader.isEOF() && !foundMatch)
				{
					csvReader.readFields(fields);
					// System.out.println(fields.toString());
					DimensionMap rowMap = this.getMap(this.header, fields);
					if(rowMap != null && rowMap.compareTo(map) == 0)
					{
						table.put(map, fields.get(fields.size() - 1));
						foundMatch = true;
					}
					fileRowCount++;
				}
				
			}
			
			csvReader.close();
			
			DimTable ret = new DimTable();
			for (Dim dim : this.dimTable)
			{
				if(filter.get(dim.name()) == null)
				{
					ret.add(dim.copy());
				}
				else
				{
					Dim newDim = new Dim(dim.name(), new String[] { filter.get(dim.name()) });
					ret.add(newDim);
				}
			}
			return new Table(ret, table);
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public Table getData()
	{
		return this.getData(new DimensionMap());
	}
	
	private DimensionMap getMap(List<String> header, List<String> fields)
	{
		if(fields.size() != header.size())
		{
			return null;
		}
		DimensionMap map = new DimensionMap();
		for (int i = 0; i < header.size() - 1; i++)
		{
			map.put(header.get(i), fields.get(i));
		}
		return map;
	}
	
	private static Dim makeDim(ArrayList<String> dimRow)
	{
		if(dimRow.size() < 2)
		{
			return null;
		}
		else
		{
			String dimName = dimRow.get(0);
			List<String> values = dimRow.subList(1, dimRow.size());
			return new Dim(dimName, values);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void saveTable(String filePath, @SuppressWarnings("rawtypes") Table table)
	{
		saveTable(filePath, table.dimTable, table.data);
	}
	
	public static void saveTable(String filePath, DimTable dimTable, Map<DimensionMap,String> data)
	{
		try
		{
			CSVWriter csvWriter = TableFile.startWritingTable(filePath, dimTable);
			double count = 0, total = data.size(), percentage = 0;
			int percent = 0;
			
			if(JEXStatics.statusBar != null)
			{
				JEXStatics.statusBar.setStatusText("Writing CSV data to file.");
				JEXStatics.statusBar.setProgressPercentage(0);
			}
			// Write the data
			for (DimensionMap map : dimTable.getMapIterator())
			{
				for (String dimName : dimTable.getDimensionNames())
				{
					csvWriter.writeField(map.get(dimName));
				}
				csvWriter.writeField(data.get(map));
				csvWriter.newLine();
				
				// Update the status bar
				count = count + 1;
				percentage = 100 * count / total;
				if(JEXStatics.statusBar != null)
				{
					if(percent != (int) percentage) // Only update the status
					// bar when necessary,
					// otherwise if called too
					// frequently, it never
					// updates
					{
						percent = (int) percentage;
						
						JEXStatics.statusBar.setProgressPercentage(percent);
					}
				}
				else
				{
					if(percent != (int) percentage) // Only update the status
					// bar when necessary,
					// otherwise if called too
					// frequently, it never
					// updates
					{
						percent = (int) percentage;
					}
				}
				
			}
			csvWriter.close();
			
			if(JEXStatics.statusBar != null)
			{
				JEXStatics.statusBar.setProgressPercentage(0);
				JEXStatics.statusBar.setStatusText("Finished writing CSV data to file.");
			}
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static CSVWriter startWritingTable(String filePath, DimTable dimTable)
	{
		try
		{
			List<String> dimNames = dimTable.getDimensionNames();
			final CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath));
			// Write the dimTable
			for (String dimName : dimNames)
			{
				csvWriter.writeField(dimName);
				Dim dim = dimTable.getDimWithName(dimName);
				for (String value : dim.values())
				{
					csvWriter.writeField(value);
				}
				csvWriter.newLine();
			}
			
			// Add blank row between dimTable and header row
			csvWriter.newLine();
			
			// Write the header
			for (String dimName : dimNames)
			{
				csvWriter.writeField(dimName);
			}
			csvWriter.writeField(JEXTableWriter.VALUE);
			csvWriter.newLine();
			return csvWriter;
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static PrintWriter startWritingArffFile(String filePath, Instances instances)
	{
		try
		{
			
			PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath)))));
			writer.print(instances.toString());
			writer.print("\n");
			writer.flush();
			return writer;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	// public static int incrementalCounter = 0;
	// public static void writeArffRow(PrintWriter writer, Instances intances,
	// DimensionMap map, String value)
	// {
	// writer.println(inst);
	// incrementalCounter++;
	// //flush every 100 instances
	// if(incrementalCounter > 100){
	// incrementalCounter = 0;
	// writer.flush();
	// }
	// }
	
	public static void writeRow(CSVWriter csvWriter, DimTable dimTable, DimensionMap map, String value)
	{
		try
		{
			// Write the data
			for (String dimName : dimTable.getDimensionNames())
			{
				csvWriter.writeField(map.get(dimName));
			}
			csvWriter.writeField(value);
			csvWriter.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
