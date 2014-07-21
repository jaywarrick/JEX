package plugins.valueTable;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import logs.Logs;
import tables.DimensionMap;

public class ValueTable extends JTable {
	
	private static final long serialVersionUID = 1L;
	
	TreeMap<String,Vector<String>> dataTable;
	DefaultTableModel tableModel;
	
	public ValueTable()
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
		Vector<String> keyVect = new Vector<String>(keys);
		
		for (int i = 0; i < keyVect.size(); i++)
		{
			String key = keyVect.get(i);
			tableModel.addColumn(key, dataTable.get(key));
		}
		
		this.setModel(tableModel);
		
		for (int i = 0; i < keyVect.size(); i++)
		{
			this.getColumnModel().getColumn(i).setPreferredWidth(100);
			this.getColumnModel().getColumn(i).setMinWidth(100);
			this.getColumnModel().getColumn(i).setWidth(100);
		}
	}
	
	/**
	 * Return a string CSV version of this jex data
	 */
	private void makeDataTable(JEXData data)
	{
		dataTable = new TreeMap<String,Vector<String>>();
		if(data == null)
		{
			Vector<String> column = new Vector<String>(0);
			column.add("No data to display");
			dataTable.put("", column);
			return;
		}
		
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
			String dim1 = keyIt.next();
			String dim2 = keyIt.next();
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
			Logs.log("Table created", 0, this);
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
