package Database.SingleUserDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import miscellaneous.CSVReader;

public class JEXReader {
	
	public static ArrayList<ArrayList<String>> readCSVFileToDataMap(File f)
	{
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		try
		{
			CSVReader reader = new CSVReader(new FileReader(f));
			while (!reader.isEOF())
			{
				ArrayList<String> fields = new ArrayList<String>();
				reader.readFields(fields);
				ret.add(fields);
			}
			reader.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	
}
