package cruncher;

import java.util.TreeMap;
import java.util.concurrent.Callable;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Type;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Canceler;
import tables.DimTable;
import tables.DimensionMap;

public class ImportThread implements Callable<Object>, Canceler {
	
	String objectName, objectInfo;
	Type objectType; 
	DimTable filter;
	TreeMap<JEXEntry,TreeMap<DimensionMap,String>> dataArray;
	TreeMap<JEXEntry,JEXData> toAdd = new TreeMap<JEXEntry,JEXData>();
	Double overlap = 0.0d;
	int rows = 1, cols = 1;
	Canceler c = JEXStatics.cruncher;
	
	public ImportThread(String objectName, Type objectType, String objectInfo, TreeMap<JEXEntry,TreeMap<DimensionMap,String>> dataArray, DimTable exclusionFilterTable)
	{
		this.objectName = objectName;
		this.objectType = objectType;
		this.objectInfo = objectInfo;
		this.dataArray = dataArray;
		this.filter = exclusionFilterTable;
		if(this.filter == null)
		{
			this.filter = new DimTable();
		}
	}
	
	public ImportThread(JEXEntry entry, String objectName, Type objectType, String objectInfo, TreeMap<DimensionMap,String> dataArray, DimTable exclusionFilterTable)
	{
		this.objectName = objectName;
		this.objectType = objectType;
		this.objectInfo = objectInfo;
		this.dataArray = new TreeMap<JEXEntry,TreeMap<DimensionMap,String>>();
		this.dataArray.put(entry, dataArray);
		this.filter = exclusionFilterTable;
		if(this.filter == null)
		{
			this.filter = new DimTable();
		}
	}
	
	public void setTileParameters(double overlap, int rows, int cols)
	{
		this.overlap = new Double(overlap);
		this.rows = rows;
		this.cols = cols;
	}
	
	public ImportThread call() throws Exception
	{
		JEXStatics.statusBar.setStatusText("Importing new object named: " + objectName + ". Use Ctrl(CMD) + G to cancel the import task.");
		toAdd = new TreeMap<JEXEntry,JEXData>();
		double count = 0;
		double total = this.dataArray.keySet().size();
		for (JEXEntry entry : this.dataArray.keySet())
		{
			if(this.isCanceled())
			{
				JEXStatics.cruncher.stopGuiTask = false;
				Logs.log("Aborted the current import task.", this);
				JEXStatics.statusBar.setStatusText("Aborted the current import task.");
				JEXStatics.cruncher.finishImportThread(this);
				return null;
			}
			TreeMap<DimensionMap,String> files2Drop2 = this.dataArray.get(entry);
			
			TreeMap<DimensionMap,String> files2Drop3 = new TreeMap<>();
			
			// filter the files
			for(DimensionMap map : files2Drop2.keySet())
			{
				if(!this.filter.testMapAsExclusionFilter(map))
				{
					files2Drop3.put(map, files2Drop2.get(map));
				}
			}
			files2Drop2 = files2Drop3;
			
			Logs.log("Importing items into Entry " + entry.getEntryID() + " at X:Y " + entry.getTrayX() + ":" + entry.getTrayY() , this);
			if(objectType.equals(JEXData.IMAGE))
			{
				JEXData data = null;
				if(this.overlap <= 0.0d)
				{
					data = ImageWriter.makeImageStackFromPaths(objectName, files2Drop2, this);
				}
				else
				{
					data = ImageWriter.makeImageTilesFromPaths(objectName, files2Drop2, this.overlap, this.rows, this.cols, this);
				}
				if(data != null)
				{
					data.setDataObjectInfo(objectInfo);
					toAdd.put(entry, data);
				}
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
		if(this.isCanceled())
		{
			JEXStatics.cruncher.stopGuiTask = false;
			Logs.log("Aborted the current import task.", this);
			JEXStatics.statusBar.setStatusText("Aborted the current import task.");
		}
		JEXStatics.cruncher.finishImportThread(this);
		return this;
	}

	@Override
	public boolean isCanceled() {
		return JEXStatics.cruncher.stopGuiTask;
	}
}
