package jex.arrayView;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

public class JEXValueTable extends JTable {
	
	private static final long serialVersionUID = 1L;
	
	TreeMap<String,Vector<String>> dataTable;
	DefaultTableModel tableModel;
	
	public JEXValueTable()
	{
		initialize();
	}
	
	private void initialize()
	{   
		
	}
	
	public void setDataToView(JEXData data)
	{
		makeDataTable(data);
		displayTable();
	}
	
	private void displayTable()
	{
		tableModel = new DefaultTableModel();
		Set<String> keys = dataTable.keySet();
		for (String key : keys)
		{
			tableModel.addColumn(key, dataTable.get(key));
		}
		this.setModel(tableModel);
	}
	
	/**
	 * Return a string CSV version of this jex data
	 */
	public void makeDataTable(JEXData data)
	{
		dataTable = new TreeMap<String,Vector<String>>();
		if(data == null)
			return;
		
		TreeMap<DimensionMap,JEXDataSingle> datamap = data.getDataMap();
		DimensionMap firstMap = datamap.firstKey();
		if(firstMap == null)
		{   
			
		}
		else if(firstMap.size() == 0)
		{
			Logs.log("Creating 0 dimension table view", 0, this);
			JEXDataSingle ds = datamap.get(firstMap);
			String value = ds.get(JEXDataSingle.VALUE);
			Vector<String> column = new Vector<String>(0);
			column.add(value);
			dataTable.put("", column);
		}
		else if(firstMap.size() == 1)
		{
			Logs.log("Creating 1 dimension table view", 0, this);
			Vector<String> column = new Vector<String>(0);
			for (DimensionMap map : datamap.keySet())
			{
				JEXDataSingle ds = datamap.get(map);
				String value = ds.get(JEXDataSingle.VALUE);
				column.add(value);
			}
			String dimName = firstMap.keySet().iterator().next();
			dataTable.put(dimName, column);
		}
		else if(firstMap.size() == 2)
		{
			Logs.log("Creating 2 dimension table view", 0, this);
			
			TreeSet<String> dimValues1 = new TreeSet<String>();
			TreeSet<String> dimValues2 = new TreeSet<String>();
			Iterator<String> keyIt = firstMap.keySet().iterator();
			String dim2 = keyIt.next();
			String dim1 = keyIt.next();
			for (DimensionMap map : datamap.keySet())
			{
				dimValues1.add(map.get(dim1));
				dimValues2.add(map.get(dim2));
			}
			
			for (String value1 : dimValues1)
			{
				Vector<String> column = new Vector<String>(0);
				for (String value2 : dimValues2)
				{
					DimensionMap map = new DimensionMap();
					map.put(dim1, "" + value1);
					map.put(dim2, "" + value2);
					JEXDataSingle ds = datamap.get(map);
					String value = (ds.get(JEXDataSingle.VALUE) == null) ? "-" : ds.get(JEXDataSingle.VALUE);
					column.add(value);
				}
				dataTable.put(dim1 + "-" + value1, column);
			}
		}
		else
		{
			Logs.log("Creating >2 dimension table view", 0, this);
			Vector<String> column1 = new Vector<String>(0);
			Vector<String> column2 = new Vector<String>(0);
			for (DimensionMap map : datamap.keySet())
			{
				JEXDataSingle ds = datamap.get(map);
				column1.add(map.toString());
				column2.add(ds.get(JEXDataSingle.VALUE));
			}
			dataTable.put("Label", column1);
			dataTable.put("Value", column2);
		}
	}
	
}
