package weka.core.converters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import logs.Logs;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;

import org.apache.commons.io.FileUtils;

import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;
//import org.xadisk.additional.XAFileOutputStreamWrapper;
//import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;

public class JEXTableWriter {
	
	public final static String VALUE = "Value";
	public final static String METADATA = "Metadata";
	public final static String ARFF_FILE = "arff", CSV_FILE = "csv", TXT_FILE = "txt";
	
	public PrintWriter writer;
	public String filePath, tableName;
	public DimTable dimTable;
	public Instances instances;
	public String valueHeaderName = VALUE;
	public static int precisionAfterDecPoint = 6;
	
	private int rowCounter = 0;
	
	public static <E> String writeTable(String tableName, Table<E> table)
	{
		return JEXTableWriter.writeTable(tableName, table, ARFF_FILE);
	}
	
	public static <E> String writeTable(String tableName, Table<E> table, String fileExtension)
	{
		JEXTableWriter writer = new JEXTableWriter(tableName, fileExtension);
		writer.writeTable(table);
		return writer.getPath();
	}
	
	public static <E> String writeJXD(String tableName, Table<E> table, String fileExtension) throws Exception
	{
		JEXTableWriter writer = new JEXTableWriter(tableName, fileExtension);
		writer.writeJXD(table);
		return writer.getPath();
	}
	
	public static <E> String writeTable(String tableName, TreeMap<DimensionMap,E> data)
	{
		return JEXTableWriter.writeTable(tableName, data, ARFF_FILE);
	}
	
	public static <E> String writeTable(String tableName, TreeMap<DimensionMap,E> data, String extension)
	{
		DimTable dimTable = new DimTable(data);
		return JEXTableWriter.writeTable(tableName, dimTable, data, extension);
	}
	
	public static <E> String writeTable(String tableName, DimTable dimTable, TreeMap<DimensionMap,E> data)
	{
		return JEXTableWriter.writeTable(tableName, dimTable, data, ARFF_FILE);
	}
	
	public static <E> String writeTable(String tableName, DimTable dimTable, TreeMap<DimensionMap,E> data, String fileExtension)
	{
		return JEXTableWriter.writeTable(tableName, new Table<E>(dimTable, data), fileExtension);
	}
	
	public JEXTableWriter(String tableName)
	{
		this(tableName, ARFF_FILE);
	}
	
	public JEXTableWriter(String tableName, String fileExtension)
	{
		this.tableName = tableName;
		this.filePath = DirectoryManager.getUniqueAbsoluteTempPath(fileExtension);
	}
	
	public <E> void writeTable(Table<E> table)
	{
		if(table == null || table.data == null)
		{
			return;
		}
		else if(table.data.size() == 0 || table.data.firstEntry().getValue() == null)
		{
			// Then just assume it is a numeric table.
			this.writeNumericTableHeader(table.dimTable);
			this.writeData(table.data);
		}
		else if(table.data.firstEntry().getValue() instanceof String)
		{
			this.writeStringTableHeader(table.dimTable);
			this.writeData(table.data);
		}
		else
		{
			this.writeNumericTableHeader(table.dimTable);
			this.writeData(table.data);
		}
		this.close();
	}
	
	public <E> void writeJXD(Table<E> table) throws Exception
	{
		// Always a string table
		this.writeJXDHeader(table.dimTable);
		this.writeData(table.data);
		this.close();
	}
	
	/**
	 * Every possible dimension map must have a row even if the value is blank (i.e. "")
	 * 
	 * @param filter
	 * @param data
	 */
	public <E> void writeTable(DimTable dimTable, TreeMap<DimensionMap,E> data)
	{
		this.writeTable(new Table<E>(dimTable, data));
	}
	
	public void setAlternateFileOutputPath(String newPath)
	{
		this.filePath = newPath;
	}
	
	public void setAlternateValueHeaderName(String valueHeaderName)
	{
		this.valueHeaderName = valueHeaderName;
	}
	
	/**
	 * This also flushes the buffer used to write the file so if this isn't called at the end there will likely be missing data. Be sure to call this when done writing.
	 */
	public void close()
	{
		if(this.writer != null)
		{
			this.writer.flush();
			this.writer.close();
		}
	}
	
	public String getPath()
	{
		return this.filePath;
	}
	
	public void writeStringTableHeader(DimTable dimTable)
	{
		this.writeHeader(dimTable, false);
	}
	
	public void writeNumericTableHeader(DimTable dimTable)
	{
		this.writeHeader(dimTable, true);
	}
	
	protected void writeJXDHeader(DimTable dimTable) throws Exception
	{
		try
		{
			this.dimTable = dimTable;
			
			ArrayList<Attribute> atts = this.dimTable.getArffAttributeList();
			atts.add(new Attribute(this.valueHeaderName, (List<String>) null));
			
			// Have to have a separate header method for JXD files because we
			// need to at the "Value" atrribute to the file.
			this.instances = new Instances(this.tableName, atts, this.dimTable.mapCount());
			
			// Initialize PrintWriter and write header
			File tableFile = new File(this.getPath());
			this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tableFile))));
			this.writer.print(this.instances.toString());
			this.writer.flush();
		}
		catch (Exception e)
		{
			if(this.writer != null)
			{
				this.writer.close();
			}
			throw e;
		}
	}
	
	protected void writeHeader(DimTable dimTable, boolean isNumeric)
	{
		try
		{
			this.dimTable = dimTable;
			// this.filePath = JEXWriter.getUniqueTempPath(fileExtension);
			
			ArrayList<Attribute> atts = this.dimTable.getArffAttributeList();
			if(isNumeric)
			{
				atts.add(new Attribute(this.valueHeaderName));
			}
			else
			{
				atts.add(new Attribute(this.valueHeaderName, (List<String>) null));
			}
			this.instances = new Instances(this.tableName, atts, this.dimTable.mapCount());
			
			// Initialize PrintWriter and write header
			this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(this.filePath)))));
			this.writer.print(this.instances.toString());
			this.writer.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if(this.writer != null)
			{
				this.writer.flush();
				this.writer.close();
			}
		}
	}
	
	public <E> void writeData(DimensionMap map, E value)
	{
		this.writer.println(this.makeRow(map, value));
		this.rowCounter++;
		if(this.rowCounter > 100)
		{
			this.writer.flush();
			this.rowCounter = 0;
		}
	}
	
	protected <E> void writeData(TreeMap<DimensionMap,E> data)
	{
		int count = 0, percentage = 0, total = data.size(), newPercentage = 0;
		Logs.setStatusText("Writing ARFF: 0%");
		for (Entry<DimensionMap,E> e : data.entrySet())
		{
			count = count + 1;
			newPercentage = (int) (100 * ((double) count) / (total));
			E value = e.getValue();
			this.writeData(e.getKey(), value);
			
			if(newPercentage != percentage)
			{
				percentage = newPercentage;
				Logs.setStatusText("Writing ARFF: " + percentage + "%");
			}
		}
		Logs.setStatusText("Writing ARFF Done.");
	}
	
	private <E> String makeRow(DimensionMap map, E value)
	{
		StringBuffer text = new StringBuffer();
		int i = 0;
		for (Dim dim : this.dimTable)
		{
			if(i > 0)
			{
				text.append(",");
			}
			String dimValue = map.get(dim.name());
			if(dimValue == null)
			{
				dimValue = dim.dimValues.get(0);
				map.put(dim.name(), dimValue);
				// JEXStatics.logManager.log("found null value", 0, this);
			}
			text.append(Utils.quote(dimValue));
			i++;
		}
		if(i > 0)
		{
			text.append(",");
		}
		if(value == null)
		{
			text.append("?");
		}
		else if(value instanceof String)
		{
			text.append(Utils.quote(value.toString()));
		}
		else
		{
			Double temp = ((Number) value).doubleValue();
			if(temp.equals(Double.NaN))
			{
				text.append("?");
			}
			else
			{
				text.append(doubleToString(((Number) value).doubleValue(), precisionAfterDecPoint));
			}
		}
		return text.toString();
	}
	
	public static String doubleToString(double value, int precisionAfterDecPoint)
	{
		return Utils.doubleToString(value, precisionAfterDecPoint);
	}
	
	public static String defrag(String path)
	{
		String extension = FileUtility.getFileNameExtension(path);
		String newPath = DirectoryManager.getUniqueAbsoluteTempPath(extension);
		try
		{
			FileUtils.copyFile(new File(path), new File(newPath));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		return newPath;
	}
}
