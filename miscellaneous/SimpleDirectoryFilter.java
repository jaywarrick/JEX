package miscellaneous;

import java.io.File;
import java.io.FileFilter;

public class SimpleDirectoryFilter implements FileFilter {
	
	public boolean accept(File file)
	{
		return file.isDirectory();
	}
}
