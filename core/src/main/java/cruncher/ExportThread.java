package cruncher;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import jex.statics.JEXStatics;
import miscellaneous.FileUtility;
import tables.DimensionMap;
import Database.DBObjects.JEXEntry;
import Database.SingleUserDatabase.JEXWriter;


public class ExportThread implements Callable<Object> {
	
	TreeMap<JEXEntry,TreeMap<DimensionMap,String>> dataArray;
	String directory;
	
	public ExportThread(String directory, TreeMap<JEXEntry,TreeMap<DimensionMap,String>> dataArray)
	{
		this.directory = directory;
		this.dataArray = dataArray;
	}
	
	public ExportThread(String directory, JEXEntry entry, TreeMap<DimensionMap,String> dataArray)
	{
		this.directory = directory;
		this.dataArray = new TreeMap<JEXEntry,TreeMap<DimensionMap,String>>();
		this.dataArray.put(entry, dataArray);
	}
	
	public ExportThread call() throws Exception
	{
		if(directory == null)
		{
			return null;
		}
		File dir = new File(directory);
		if(!dir.exists())
		{
			return null;
		}
		int count = 0;
		int tot = 0;
		for(JEXEntry e : this.dataArray.keySet())
		{
			tot = tot + this.dataArray.get(e).size();
		}		
		for (JEXEntry e : this.dataArray.keySet())
		{
			File eDir = new File(directory + File.separator + e.getEntryExperiment());
			eDir.mkdir();
			TreeMap<DimensionMap,String> files = this.dataArray.get(e);
			for (DimensionMap map : files.keySet())
			{
				if(JEXStatics.cruncher.stopGuiTask == true)
				{
					JEXStatics.cruncher.stopGuiTask = false;
					JEXStatics.cruncher.finishImportThread(null);
					return null;
				}
				String source = files.get(map);
				if(source == null)
				{
					continue;
				}
				String mapString = map.toString().replaceAll("\\,", "_");
				mapString = mapString.replace("=", "");
				String fileName = e.getTrayX() + "_" + e.getTrayY() + "_" + FileUtility.getFileNameWithExtension(source);
				try
				{
					JEXWriter.copy(new File(source), new File(eDir.getCanonicalPath() + File.separator + fileName));
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
					return null;
				}
				count = count + 1;
				JEXStatics.statusBar.setProgressPercentage((int) (100 * count / tot));
				JEXStatics.statusBar.setStatusText(count + " of " + tot + " files imported.");
			}
		}
		JEXStatics.cruncher.finishExportThread(this);
		return this;
	}
}
