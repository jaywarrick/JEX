package Database.DataWriter;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tables.DimensionMap;

public class ValueWriter {
	
	/**
	 * Make an value data object with a single value inside
	 * 
	 * @param objectName
	 * @param value
	 * @return data
	 */
	public static JEXData makeValueObject(String objectName, String value)
	{
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		JEXDataSingle ds = new JEXDataSingle(value);
		data.addData(new DimensionMap(), ds);
		return data;
	}
	
	/**
	 * Make an value object containing an value list, each value is at an index defined by an index from the list INDEXES, a value from VALUES and a dimension name
	 * 
	 * @param objectName
	 * @param values
	 * @param indexes
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeValueStack(String objectName, String[] values, String[] indexes, String dimensionName)
	{
		if(values.length == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		
		for (int index = 0; index < indexes.length; index++)
		{
			String indexStr = indexes[index];
			String value = values[index];
			JEXDataSingle ds = new JEXDataSingle(value);
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		return data;
	}
	
	/**
	 * Return an value list object from values and a dimension name (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param values
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeValueStack(String objectName, String[] values, String dimensionName)
	{
		if(values.length == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		
		for (int index = 0; index < values.length; index++)
		{
			String indexStr = "" + index;
			String value = values[index];
			JEXDataSingle ds = new JEXDataSingle(value);
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		return data;
	}
	
	/**
	 * Return an value list object from values and a dimension name (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param values
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeValueStack(String objectName, float[] values, String dimensionName)
	{
		if(values.length == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		
		for (int index = 0; index < values.length; index++)
		{
			String indexStr = "" + index;
			String value = "" + values[index];
			JEXDataSingle ds = new JEXDataSingle(value);
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		return data;
	}
	
	/**
	 * Return an value list object from values and a dimension name (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param values
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeValueStack(String objectName, double[] values, String dimensionName)
	{
		if(values.length == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		
		for (int index = 0; index < values.length; index++)
		{
			String indexStr = "" + index;
			String value = "" + values[index];
			JEXDataSingle ds = new JEXDataSingle(value);
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		return data;
	}
	
	/**
	 * Return an value list object from values and a dimension name (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param values
	 * @param dimensionName
	 * @return data
	 */
	public static JEXData makeValueStack(String objectName, int[] values, String dimensionName)
	{
		if(values.length == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		
		for (int index = 0; index < values.length; index++)
		{
			String indexStr = "" + index;
			String value = "" + values[index];
			JEXDataSingle ds = new JEXDataSingle(value);
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indexStr);
			data.addData(map, ds);
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTable(String objectName, String[] columnNames, String[] rowNames, HashMap<String,String[]> columns)
	{
		boolean foundData = false;
		for(Entry<String,String[]> e : columns.entrySet())
		{
			if(e.getValue().length > 0)
			{
				foundData = true;
				break;
			}
		}
		if(!foundData)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (int index = 0; index < columnNames.length; index++)
		{
			String columnName = columnNames[index];
			String[] column = columns.get(columnName);
			if(column == null)
				continue;
			for (int index2 = 0; index2 < column.length; index2++)
			{
				String columnValue = column[index2];
				String rowName = rowNames[index2];
				JEXDataSingle ds = new JEXDataSingle(columnValue);
				DimensionMap map = new DimensionMap();
				map.put("Row", rowName);
				map.put("Column", columnName);
				data.addData(map, ds);
			}
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTable(String objectName, String[] columnNames, HashMap<String,String[]> columns)
	{
		boolean foundData = false;
		for(Entry<String,String[]> e : columns.entrySet())
		{
			if(e.getValue().length > 0)
			{
				foundData = true;
				break;
			}
		}
		if(!foundData)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (int index = 0; index < columnNames.length; index++)
		{
			String columnName = columnNames[index];
			String[] column = columns.get(columnName);
			if(column == null)
				continue;
			for (int index2 = 0; index2 < column.length; index2++)
			{
				String columnValue = column[index2];
				String rowName = "" + index2;
				JEXDataSingle ds = new JEXDataSingle(columnValue);
				DimensionMap map = new DimensionMap();
				map.put("Row", rowName);
				map.put("Column", columnName);
				data.addData(map, ds);
			}
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTableFromFloat(String objectName, String[] columnNames, HashMap<String,float[]> columns)
	{
		boolean foundData = false;
		for(Entry<String,float[]> e : columns.entrySet())
		{
			if(e.getValue().length > 0)
			{
				foundData = true;
				break;
			}
		}
		if(!foundData)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (int index = 0; index < columnNames.length; index++)
		{
			String columnName = columnNames[index];
			float[] column = columns.get(columnName);
			if(column == null)
				continue;
			for (int index2 = 0; index2 < column.length; index2++)
			{
				String columnValue = "" + column[index2];
				String rowName = "" + index2;
				JEXDataSingle ds = new JEXDataSingle(columnValue);
				DimensionMap map = new DimensionMap();
				map.put("Row", rowName);
				map.put("Column", columnName);
				data.addData(map, ds);
			}
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTableFromFloatList(String objectName, String[] columnNames, HashMap<String,List<Float>> columns)
	{
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (int index = 0; index < columnNames.length; index++)
		{
			String columnName = columnNames[index];
			List<Float> column = columns.get(columnName);
			if(column == null)
				continue;
			for (int index2 = 0; index2 < column.size(); index2++)
			{
				String columnValue = "" + column.get(index2);
				String rowName = "" + index2;
				JEXDataSingle ds = new JEXDataSingle(columnValue);
				DimensionMap map = new DimensionMap();
				map.put("Row", rowName);
				map.put("Column", columnName);
				data.addData(map, ds);
			}
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTableFromDouble(String objectName, String[] columnNames, HashMap<String,double[]> columns)
	{
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (int index = 0; index < columnNames.length; index++)
		{
			String columnName = columnNames[index];
			double[] column = columns.get(columnName);
			if(column == null)
				continue;
			for (int index2 = 0; index2 < column.length; index2++)
			{
				String columnValue = "" + column[index2];
				String rowName = "" + index2;
				JEXDataSingle ds = new JEXDataSingle(columnValue);
				DimensionMap map = new DimensionMap();
				map.put("Row", rowName);
				map.put("Column", columnName);
				data.addData(map, ds);
			}
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTableFromDoubleList(String objectName, String[] columnNames, HashMap<String,List<Double>> columns)
	{
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (int index = 0; index < columnNames.length; index++)
		{
			String columnName = columnNames[index];
			List<Double> column = columns.get(columnName);
			if(column == null)
				continue;
			for (int index2 = 0; index2 < column.size(); index2++)
			{
				String columnValue = "" + column.get(index2);
				String rowName = "" + index2;
				JEXDataSingle ds = new JEXDataSingle(columnValue);
				DimensionMap map = new DimensionMap();
				map.put("Row", rowName);
				map.put("Column", columnName);
				data.addData(map, ds);
			}
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTable(String objectName, Map<DimensionMap,String> valueMap)
	{
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (DimensionMap dim : valueMap.keySet())
		{
			String value = valueMap.get(dim);
			JEXDataSingle ds = new JEXDataSingle(value);
			DimensionMap map = dim.copy();
			data.addData(map, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Make a valud table data object with row and column labels, and a list of values for each column
	 * 
	 * @param objectName
	 * @param columnNames
	 * @param rowNames
	 * @param columns
	 * @return
	 */
	public static JEXData makeValueTableFromDouble(String objectName, Map<DimensionMap,Double> valueMap)
	{
		JEXData data = new JEXData(JEXData.VALUE, objectName);
		for (DimensionMap dim : valueMap.keySet())
		{
			Double value = valueMap.get(dim);
			JEXDataSingle ds = new JEXDataSingle("" + value);
			DimensionMap map = dim.copy();
			data.addData(map, ds);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
	// /**
	// * Make a valud table data object with row and column labels, and a list
	// of values for each column
	// * @param objectName
	// * @param columnNames
	// * @param rowNames
	// * @param columns
	// * @return
	// */
	// public static JEXData makeValueTable(String objectName,
	// TreeMap<DimensionMap, String> valueMap){
	// JEXData data = new DefaultJEXData(JEXData.VALUE,objectName);
	// for (DimensionMap dim: valueMap.keySet()){
	// String value = valueMap.get(dim);
	// JEXDataSingle ds = new DefaultJEXDataSingle(value);
	// DimensionMap map = dim.duplicate();
	// data.addData(map, ds);
	// }
	// return data;
	// }
	
}
