package Database.DataReader;

import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import tables.DimensionMap;

public class MovieReader {
	
	/**
	 * Get the movie path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readMovieObject(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.MOVIE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = FileReader.readToPath(ds);
		
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		
		return result;
	}
	
	public static TreeMap<DimensionMap,String> readMovieObjects(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.MOVIE))
			return null;
		TreeMap<DimensionMap,String> result = new TreeMap<DimensionMap,String>();
		JEXDataSingle ds = data.getFirstSingle();
		String dataFolder = FileReader.readToFile(ds).getParent(); // DO THIS
		// ONE TIME OUTSIDE LOOP OTHERWISE YOU WILL CHECK IF THIS DIRECTORY EXISTS FOR EACH DATASINGLE IN THE JEXDATA! (MAJORLY SLOW)
		for (DimensionMap map : data.getDataMap().keySet())
		{
			ds = data.getData(map);
			String path = FileReader.readToPath(dataFolder, ds);
			result.put(map, path);
		}
		return result;
	}
}
