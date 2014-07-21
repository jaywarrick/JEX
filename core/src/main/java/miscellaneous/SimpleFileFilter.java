package miscellaneous;

import java.io.File;
import java.io.FileFilter;

public class SimpleFileFilter implements FileFilter {
	
	private String[] okExtensions;
	
	public SimpleFileFilter(String[] okExtensions_lowerCase)
	{
		this.okExtensions = okExtensions_lowerCase;
	}
	
	public boolean accept(File file)
	{
		for (String extension : okExtensions)
		{
			if(file.getName().toLowerCase().endsWith(extension))
			{
				return true;
			}
		}
		return false;
	}
}
