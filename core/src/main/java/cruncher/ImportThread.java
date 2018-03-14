package cruncher;

import java.util.TreeMap;
import java.util.concurrent.Callable;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Type;
import jex.statics.JEXStatics;
import tables.DimensionMap;

public class ImportThread implements Callable<Object> {
	
	String objectName, objectInfo;
	Type objectType; 
	TreeMap<JEXEntry,TreeMap<DimensionMap,String>> dataArray;
	TreeMap<JEXEntry,JEXData> toAdd = new TreeMap<JEXEntry,JEXData>();
	Double overlap = 0.0d;
	int rows = 1, cols = 1;
	
	public ImportThread(String objectName, Type objectType, String objectInfo, TreeMap<JEXEntry,TreeMap<DimensionMap,String>> dataArray)
	{
		this.objectName = objectName;
		this.objectType = objectType;
		this.objectInfo = objectInfo;
		this.dataArray = dataArray;
	}
	
	public ImportThread(JEXEntry entry, String objectName, Type objectType, String objectInfo, TreeMap<DimensionMap,String> dataArray)
	{
		this.objectName = objectName;
		this.objectType = objectType;
		this.objectInfo = objectInfo;
		this.dataArray = new TreeMap<JEXEntry,TreeMap<DimensionMap,String>>();
		this.dataArray.put(entry, dataArray);
	}
	
	public void setTileParameters(double overlap, int rows, int cols)
	{
		this.overlap = new Double(overlap);
		this.rows = rows;
		this.cols = cols;
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
			TreeMap<DimensionMap,String> files2Drop2 = this.dataArray.get(entry);
			
			if(objectType.equals(JEXData.IMAGE))
			{
				JEXData data = null;
				if(this.overlap <= 0.0d)
				{
					data = ImageWriter.makeImageStackFromPaths(objectName, files2Drop2);
				}
				else
				{
					data = ImageWriter.makeImageTilesFromPaths(objectName, files2Drop2, this.overlap, this.rows, this.cols);
				}
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
