package miscellaneous;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;

import logs.Logs;

import org.apache.commons.io.FileUtils;

import jex.statics.JEXDialog;

public class DirectoryManager {
	
	public static String TEMP_FOLDER_NAME = "temp";
	public static String CORE_TEMP_NAME = "JEXData";
	public static int NUM_SEQ_LENGTH = 10;
	private volatile static int fileCounter = 0;
	public static File hostDirectory = null;
	
	public synchronized static void setHostDirectory(String path)
	{
		File folder = new File(path);
		if(!folder.exists() || !folder.isDirectory())
		{
			hostDirectory = null;
			Logs.log("Cannot set host directory to: " + path + " as it either doesn't exist or is not a directory", Logs.ERROR, DirectoryManager.class);
		}
		hostDirectory = folder;
	}
	
	public synchronized static File getHostDirectory()
	{
		if(hostDirectory == null || !hostDirectory.exists())
		{
			if(hostDirectory != null)
			{
				// Try and wait a few seconds and see if the connection reestablishes itself.
				try
				{
					TimeUnit.SECONDS.sleep(10);
					JEXDialog.messageDialog("Couldn't find the host directory... waiting 5 seconds.", DirectoryManager.class);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			// Then test one last time...
			if(hostDirectory == null || !hostDirectory.exists())
			{
				Logs.log("Need a non-null or existing host directory... asking user.", 0, DirectoryManager.class);
				hostDirectory = getDirectoryFromUser(null);
			}
		}
		return hostDirectory;
	}
	
	/**
	 * Return a unique name in the temporary folder
	 * 
	 * @return
	 * @throws IOException 
	 */
	public synchronized static String getUniqueRelativeTempPath(String extension) throws IOException
	{
		if(hostDirectory == null)
		{
			return null;
		}
		// Create the file path
		String tempName = getAvailableTempFileName(CORE_TEMP_NAME, NUM_SEQ_LENGTH, extension);
		String relativePath = DirectoryManager.TEMP_FOLDER_NAME + File.separator + tempName;
		DirectoryManager.getTempFolderPath(); // Makes sure that the temp folder exists.
		return relativePath;
	}
	
	/**
	 * Return a unique name in the temporary folder
	 * 
	 * @return
	 * @throws IOException 
	 */
	public synchronized static String getUniqueAbsoluteTempPath(String extension) throws IOException
	{
		if(hostDirectory == null)
		{
			return null;
		}
		// Create the file path
		String tempName = getAvailableTempFileName(CORE_TEMP_NAME, NUM_SEQ_LENGTH, extension);
		String relativePath = getTempFolderPath() + File.separator + tempName;
		return relativePath;
	}
	
	/**
	 * Get the path of the temporary folder to save data that is not yet attached to the database
	 * 
	 * @return
	 * @throws IOException 
	 */
	public synchronized static String getTempFolderPath() throws IOException
	{
		// Create it if the folder doesn't exist
		File f = new File(getHostDirectory() + File.separator + TEMP_FOLDER_NAME);
		if(!f.exists())
		{
			f.mkdirs();
		}
		return f.getCanonicalPath();
	}
	
	/**
	 * Get the next free file name based on the core name, a suffix and a selected extension.
	 * If extension is null, it will return a unique directory name, incrementing the unique
	 * id counter as well.
	 * 
	 * @param path
	 * @param coreName
	 * @param suffix
	 * @param extension
	 * @return
	 */
	private synchronized static String getAvailableTempFileName(String coreName, int suffixNumberLength, String extension)
	{
		String fileName;
		if(extension == null)
		{
			fileName = coreName + StringUtility.fillLeft("" + fileCounter, suffixNumberLength, "0");
		}
		else
		{
			fileName = coreName + StringUtility.fillLeft("" + fileCounter, suffixNumberLength, "0") + "." + extension;
		}
		fileCounter = fileCounter + 1;
		return fileName;
	}
	
	public synchronized static File getDirectoryFromUser(Component parent)
	{
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fc.showOpenDialog(parent);
		File directory = null;
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			directory = fc.getSelectedFile();
			if(directory == null)
			{
				return null;
			}
		}
		return directory;
	}
	
	public synchronized static void deleteContentsOfTempFolder() throws IOException
	{
		File toDelete = new File(getTempFolderPath());
		try
		{
			FileUtils.deleteDirectory(toDelete);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		toDelete.mkdirs();
		
	}
	
}
