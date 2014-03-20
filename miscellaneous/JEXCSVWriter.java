package miscellaneous;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JEXCSVWriter {
	
	public String path;
	public CSVWriter writer;
	
	public JEXCSVWriter(String path)
	{
		try
		{
			this.path = path;
			this.writer = new CSVWriter(new FileWriter(new File(this.path)));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void write(List<String> rowOfInfo)
	{
		try
		{
			this.writer.writeFields(rowOfInfo);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void write(String[] rowOfInfo)
	{
		try
		{
			this.writer.writeFields(rowOfInfo);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public String getPath()
	{
		return this.path;
	}
	
	public void close()
	{
		try
		{
			if(this.writer != null)
			{
				this.writer.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
