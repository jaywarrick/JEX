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

	/**
	 * Make a JEXData file object from a single object
	 * 
	 * @param objectName
	 * @param ObjectFlavor
	 * @param file
	 * @return data
	 */
	public static JEXData makeFileObject(String objectName, String objectFlavor, Object fileObject)
	{
		// Make the data that will be returned
		JEXData data = new JEXData(new Type(JEXData.FILE, objectFlavor), objectName);

		// If the file is a string, make a JEXDataSingle with the string embedded
		JEXDataSingle ds = saveFileDataSingle(fileObject);

		// test the JEXDataSingle
		if (ds == null) return null;

		// Add the datasingle to the data
		data.addData(new DimensionMap(), ds);

		// Test to see that the data was well inputed in the new JEXData, else return null
		if(data.datamap.size() == 0)
		{
			return null;
		}

		// Return the JEXData
		return data;
	}

	/**
	 * Make a JEXData file object from a single object
	 * 
	 * @param objectName
	 * @param ObjectFlavor
	 * @param file
	 * @return data
	 */
	public static JEXData makeFileObject(String objectName, String objectFlavor, Map<DimensionMap,Object> fileMap)
	{
		// Make the data that will be returned
		JEXData data = new JEXData(new Type(JEXData.FILE, objectFlavor), objectName);

		// loop through the file objects
		for (DimensionMap map : fileMap.keySet())
		{
			// Get the file object
			Object fileObject = fileMap.get(map);

			// If the file is a string, make a JEXDataSingle with the string embedded
			JEXDataSingle ds = saveFileDataSingle(fileObject);

			// test the JEXDataSingle
			if (ds == null) continue;

			// Add the datasingle to the data
			data.addData(new DimensionMap(), ds);
		}

		// Test to see that the data was well inputed in the new JEXData, else return null
		if(data.datamap.size() == 0)
		{
			return null;
		}

		// Return the JEXData
		return data;
	}

	/**
	 * Make a JEXDataSingle from a filepath
	 * 
	 * @param filePath
	 * @return
	 */
	public static JEXDataSingle saveFileDataSingle(Object fileObject)
	{
		// test the fileObject
		if (fileObject == null) return null;

		// Create a variable for the path and the file
		File          file     = null;
		String        filePath = null;

		// Test the file object send and make a JEXDataSingle based on that
		if (fileObject.getClass().equals(String.class))
		{
			// If the fileObject is a String
			filePath = (String) fileObject;
			file     = new File(filePath);
		}
		else if (fileObject.getClass().equals(File.class))
		{
			// If the fileObject is a file
			file     = (File) fileObject;
			filePath = file.getAbsolutePath();
		}
		else
		{
			return null;
		}

		// Test if the file exists else return null
		if(filePath == "" || !file.exists())
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

}
