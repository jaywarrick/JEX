package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

public class MovieReader {
	
	/**
	 * Get the movie path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readMovieObject(JEXData data)
	{
		if(!data.getDataObjectType().equals(JEXData.MOVIE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = FileReader.readToPath(ds);
		
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		
		return result;
	}
	
}
