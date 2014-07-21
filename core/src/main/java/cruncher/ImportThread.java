package cruncher;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Type;

import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;

import jex.statics.JEXStatics;
import miscellaneous.Pair;
import tables.DimensionMap;

public class ImportThread implements Callable<Object> {
	
	String objectName, objectInfo;
	Type objectType; 
	TreeMap<JEXEntry,Vector<Pair<DimensionMap,String>>> dataArray;
	TreeMap<JEXEntry,JEXData> toAdd = new TreeMap<JEXEntry,JEXData>();
	
	public ImportThread(String objectName, Type objectType, String objectInfo, TreeMap<JEXEntry,Vector<Pair<DimensionMap,String>>> dataArray)
	{
		this.objectName = objectName;
		this.objectType = objectType;
		this.objectInfo = objectInfo;
		this.dataArray = dataArray;
	}
	
	public ImportThread call() throws Exception
	{
		JEXStatics.statusBar.setStatusText("Importing new object named: " + objectName);
		toAdd = new TreeMap<JEXEntry,JEXData>();
		double count = 0;
		double total = this.dataArray.keySet().size();
		for (JEXEntry entry : this.dataArray.keySet())
		{
			if(JEXStatics.cruncher.stopGuiTask == true)
			{
				JEXStatics.cruncher.stopGuiTask = false;
				JEXStatics.cruncher.finishImportThread(null);
				return null;
			}
			TreeMap<DimensionMap,String> files2Drop2 = new TreeMap<DimensionMap,String>();
			for (Pair<DimensionMap,String> pair : this.dataArray.get(entry))
			{
				files2Drop2.put(pair.p1, pair.p2);
			}
			
			if(objectType.equals(JEXData.IMAGE))
			{
				JEXData data = ImageWriter.makeImageStackFromPaths(objectName, files2Drop2);
				data.setDataObjectInfo(objectInfo);
				toAdd.put(entry, data);
			}
			else if(objectType.equals(JEXData.FILE))
			{
				JEXData data = FileWriter.makeFileObject(objectName, null, files2Drop2);
				data.setDataObjectInfo(objectInfo);
				toAdd.put(entry, data);
			}
			count = count + 1;
			JEXStatics.statusBar.setProgressPercentage((int) (100 * count / total));
			JEXStatics.statusBar.setStatusText(count + " of " + total + " entries imported.");
		}
		JEXStatics.cruncher.finishImportThread(this);
		return this;
	}
}
