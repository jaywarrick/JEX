package Database.DataWriter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import logs.Logs;
import miscellaneous.FileUtility;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.Definition.Type;
import Database.SingleUserDatabase.JEXWriter;

public class FileWriter {
	
	/**
	 * Notes: on getPath, getAbsolutePath, and getCanonicalPath
	 * 
	 * Our preference is to use getCanonicalPath (see below)
	 * 
	 * 
	 * Consider these filenames:
	 * C:\temp\file.txt - This is a path, an absolute path, and a canonical path.
	 * .\file.txt - This is a path. It's neither an absolute path nor a canonical path.
	 * C:\temp\myapp\bin\..\\..\file.txt - This is a path and an absolute path. It's not a canonical path.
	 * A canonical path is always an absolute path.
	 * Converting from a path to a canonical path makes it absolute (usually tack on the current working directory
	 * so e.g. ./file.txt becomes c:/temp/file.txt). The canonical path of a file just "purifies" the path,
	 * removing and resolving stuff like ..\ and resolving symlinks (on unixes).
	 */

	/**
	 * Make a JEXData file object from a single object
	 * 
	 * @param objectName
	 * @param ObjectFlavor
	 * @param file (either a string path or a file object)
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
	public static JEXData makeFileObject(String objectName, String objectFlavor, Map<DimensionMap,?> fileMap)
	{
		// Make the data that will be returned
		JEXData data = new JEXData(new Type(JEXData.FILE, objectFlavor), objectName);

		// loop through the file objects
		for (DimensionMap map : fileMap.keySet())
		{
			// Get the file object
			Object fileObject = fileMap.get(map);

			// If the file is a string, make a JEXDataSingle with the string embedded
			JEXDataSingle ds = saveFileDataSingle((String) fileObject);

			// test the JEXDataSingle
			if (ds == null)
				continue;

			// Add the datasingle to the data
			data.addData(map.copy(), ds);
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
	 * @throws IOException 
	 */
	public static JEXDataSingle saveFileDataSingle(Object fileObject)
	{
		// test the fileObject
		if (fileObject == null) return null;

		// Create a variable for the path and the file
		File file = null;

		// Test the file object send and make a JEXDataSingle based on that
		if (fileObject instanceof String)
		{
			// If the fileObject is a String
			file = new File((String) fileObject);
		}
		else if (fileObject instanceof File)
		{
			file = ((File) fileObject);
		}
		else
		{
			Logs.log("Couldn't convert object to a filePath", Logs.ERROR, FileWriter.class);
			return null;
		}

		// Test if the file exists else return null
		if(!file.exists())
		{
			return null;
		}

		// Get a new file name in the temp folder
		String extension = FileUtility.getFileNameExtension(file.getName());
		String relativePath = JEXWriter.getUniqueRelativeTempPath(extension);
		File tempFile = new File(JEXWriter.getDatabaseFolder() + File.separator + relativePath);
		
		// Copy the file to the database temp folder
		try
		{
			JEXWriter.copy(file, tempFile); // Doesn't actually copy if trying to copy to the same directory to be more efficient
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}

		// Make a new JEXDataSingle using the relative path from the database folder to the file.
		JEXDataSingle ds = new JEXDataSingle();
		ds.put(JEXDataSingle.RELATIVEPATH, relativePath);

		// Return the datasingle
		return ds;
	}

}
