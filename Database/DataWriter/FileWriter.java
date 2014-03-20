package Database.DataWriter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import miscellaneous.FileUtility;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.Definition.Type;
import Database.SingleUserDatabase.JEXWriter;

public class FileWriter {
	
	public static JEXDataSingle saveFileDataSingle(String filePath)
	{
		// Get the file
		if(filePath == null || filePath == "")
		{
			return null;
		}
		File file = new File(filePath);
		
		// if the file doesn't exist return null
		if(!file.exists())
		{
			return null;
		}
		
		// Get a new file name in the temp folder
		String extension = FileUtility.getFileNameExtension(filePath);
		String relativePath = JEXWriter.getUniqueRelativeTempPath(extension);
		File tempFile = new File(JEXWriter.getDatabaseFolder() + File.separator + relativePath);
		
		try
		{
			JEXWriter.copy(file, tempFile); // Doesn't actually copy if trying
			// to copy to the same directory
			// to be more efficient
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		// Make a new JEXDataSingle
		JEXDataSingle ds = new JEXDataSingle();
		ds.put(JEXDataSingle.RELATIVEPATH, relativePath);
		
		// Return the datasingle
		return ds;
	}
	
	/**
	 * Make a file data object with a file movie inside
	 * 
	 * @param objectName
	 * @param filePath
	 * @return data
	 */
	public static JEXData makeFileObject(String objectName, String filePath)
	{
		return makeFileObject(JEXData.FILE, objectName, filePath);
	}
	
	/**
	 * Make a file data object with a file movie inside
	 * 
	 * @param objectName
	 * @param filePath
	 * @return data
	 */
	public static JEXData makeFileObject(Type objectType, String objectName, String filePath)
	{
		JEXData data = new JEXData(objectType, objectName);
		
		// Make a data single
		JEXDataSingle ds = saveFileDataSingle(filePath);
		data.addData(new DimensionMap(), ds);
		
		if(data.datamap.size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Make a file data object with a file movie inside
	 * 
	 * @param objectName
	 * @param filePath
	 * @return data
	 */
	public static JEXData makeFileTable(Type objectType, String objectName, Map<DimensionMap,String> pathMap)
	{
		JEXData data = new JEXData(objectType, objectName);
		
		for (DimensionMap map : pathMap.keySet())
		{
			String path = pathMap.get(map);
			
			// Make a data single
			JEXDataSingle ds = saveFileDataSingle(path);
			DimensionMap newmap = map.copy();
			data.addData(newmap, ds);
		}
		
		if(data.datamap.size() == 0)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Return an image stack object from filepaths and a dimensionname (ie T, Time, Z, etc...)
	 * 
	 * @param objectName
	 * @param imageMap
	 * @return
	 */
	public static JEXData makeFileTable(String objectName, Map<DimensionMap,String> imageMap)
	{
		JEXData data = new JEXData(JEXData.FILE, objectName);
		
		for (DimensionMap map : imageMap.keySet())
		{
			String path = imageMap.get(map);
			
			// Make a data single
			JEXDataSingle ds = saveFileDataSingle(path);
			DimensionMap newmap = map.copy();
			data.addData(newmap, ds);
		}
		
		if(data.datamap.size() == 0)
		{
			return null;
		}
		return data;
	}
}
