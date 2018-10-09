package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import tables.DimensionMap;

public class ValueReader {
	
	/**
	 * Get the value stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readValueObject(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.VALUE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String value = ds.get(JEXDataSingle.VALUE);
		return value;
	}
	
	/**
	 * Read the JEXData value object into a stack
	 * 
	 * @param data
	 * @return
	 */
	public static String[] readValueStack(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.VALUE))
			return null;
		List<String> resultList = new ArrayList<String>(0);
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String value = ds.get(JEXDataSingle.VALUE);
			resultList.add(value);
		}
		String[] result = resultList.toArray(new String[0]);
		return result;
	}
	
	/**
	 * Read all the values in the value object into a hashable table
	 * 
	 * @param data
	 * @return
	 */
	public static TreeMap<DimensionMap,String> readValueTable(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.VALUE))
			return null;
		TreeMap<DimensionMap,String> result = new TreeMap<DimensionMap,String>();
		for (DimensionMap map : data.getDataMap().keySet())
		{
			JEXDataSingle ds = data.getData(map);
			String value = ds.get(JEXDataSingle.VALUE);
			result.put(map, value);
		}
		return result;
	}
	
	/**
	 * Get the value stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static Double readObjectToDouble(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.VALUE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String value = ds.get(JEXDataSingle.VALUE);
		Double result = new Double(value);
		return result;
	}
	
	/**
	 * Read the JEXData value object into a stack
	 * 
	 * @param data
	 * @return
	 */
	public static Double[] readObjectToDoubleStack(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.VALUE))
			return null;
		List<String> resultList = new ArrayList<String>(0);
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String value = ds.get(JEXDataSingle.VALUE);
			resultList.add(value);
		}
		String[] result = resultList.toArray(new String[0]);
		Double[] imresult = new Double[result.length];
		for (int i = 0; i < result.length; i++)
		{
			String path = result[i];
			imresult[i] = new Double(path);
		}
		return imresult;
	}
	
	/**
	 * Read all the values in the value object into a hashable table
	 * 
	 * @param data
	 * @return
	 */
	public static TreeMap<DimensionMap,Double> readObjectToDoubleTable(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.VALUE))
			return null;
		TreeMap<DimensionMap,Double> result = new TreeMap<DimensionMap,Double>();
		for (DimensionMap map : data.getDataMap().keySet())
		{
			JEXDataSingle ds = data.getData(map);
			String value = ds.get(JEXDataSingle.VALUE);
			Double d = new Double(value);
			result.put(map, d);
		}
		return result;
	}
	
}
