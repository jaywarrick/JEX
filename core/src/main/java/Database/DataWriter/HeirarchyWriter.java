package Database.DataWriter;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import jex.statics.JEXStatics;
import tables.DimensionMap;

public class HeirarchyWriter {
	
	public static JEXData makeHeirarchy(String name, String value)
	{
		String info = "";
		if(name.equals(JEXEntry.EXPERIMENT))
		{
			info = "experiment";
		}
		else if(name.equals(JEXEntry.TRAY))
		{
			info = "tray";
		}
		else if(name.equals(JEXEntry.X))
		{
			info = "X";
		}
		else if(name.equals(JEXEntry.Y))
		{
			info = "Y";
		}
		JEXData result = new JEXData(JEXData.HIERARCHY, name, info);
		JEXDataSingle ds = new JEXDataSingle(value);
		result.addData(new DimensionMap(), ds);
		result.put(JEXData.AUTHOR, JEXStatics.jexManager.getUserName());
		return result;
	}
	
}
