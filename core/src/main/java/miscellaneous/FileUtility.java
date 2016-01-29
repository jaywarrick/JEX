package miscellaneous;

import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import Database.SingleUserDatabase.JEXWriter;
import jex.statics.JEXStatics;
import logs.Logs;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

public class FileUtility implements Comparator<File> {
	
	public static void openFileDefaultApplication(String name) throws Exception
	{
		Desktop.getDesktop().open(new File(name));
		System.out.println("Executed ! ");
	}
	
	public static <T extends RealType<T>> void showImg(Img<T> img, boolean defaultApp)
	{
		if(defaultApp)
		{
			String path = JEXWriter.saveImage(img);
			try {
				FileUtility.openFileDefaultApplication(path);
			} catch (Exception e) {
				Logs.log("Couldn't save, open, and show image.", FileUtility.class);
				e.printStackTrace();
			}
		}
		else
		{
			ImageJFunctions.show(img);
		}
	}
	
	public static void openFileDefaultApplication(File path) throws Exception
	{
		openFileDefaultApplication(path.getPath());
	}
	
	// public static int runMultipleStringCommands(String[] separateCommands)
	// throws Exception
	// {
	//
	// if(OsVersion.IS_OSX)
	// {
	// StringBuilder finalCommand = new StringBuilder();
	// int count = 0;
	// for (String command : separateCommands)
	// {
	// if(count == 0)
	// {
	// finalCommand.append(command);
	// }
	// else
	// {
	// finalCommand.append(" ; ").append(command);
	// }
	// count++;
	// }
	// String str = finalCommand.toString();
	// Logs.log("Execute: " + "/bin/bash -c " + str, 0, "FileUtility");
	// Process p = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c",
	// str });
	// int result = p.waitFor();
	// System.out.println("Executed ! ");
	// return result;
	// }
	// else if(OsVersion.IS_WINDOWS)
	// {
	// return -1;
	// }
	// return -1;
	// }
	
	/**
	 * Deletes all files and subdirectories under dir. Returns true if all deletions were successful. If a deletion fails, the method stops attempting to delete and returns false.
	 * 
	 * @param dir
	 * @throws IOException
	 */
	public static void emptyDir(File dir) throws IOException
	{
		FileUtils.cleanDirectory(dir);
	}
	
	/**
	 * Deletes all files and subdirectories under dir. Returns true if all deletions were successful. If a deletion fails, the method stops attempting to delete and returns false.
	 * 
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	public static void deleteDir(File dir) throws IOException
	{
		if(dir.isDirectory())
		{
			Logs.log("Dangerous to delete directories...", FileUtility.class);
			
			// Double check with user.
			int n = JOptionPane.showConfirmDialog(
			    JEXStatics.main,
			    "Are you sure you want to delete the entire directory: " + dir.getPath(),
			    "Continue?",
			    JOptionPane.YES_NO_OPTION);
			if(n == 0)
			{
				FileUtils.deleteDirectory(dir);
			}
		}
		else
		{
			Logs.log("Supplied file is not a directory, aborting attempt to delete: " + dir.getPath(), FileUtility.class);
		}
	}
	
	/**
	 * Moves the file or folder from src to dst and creates directories as needed.
	 * 
	 * @param src
	 * @param dst
	 * @param overwrite
	 * @return
	 * @throws IOException
	 */
	public static boolean moveFileOrFolder(File src, File dst, boolean overwrite) throws IOException
	{
		// Then move the file and overwrite if necessary
		Logs.log("Moving file/folder from " + src.getPath() + " to " + dst.getPath(), 0, "FileUtility");
		
		if(!overwrite)
		{
			if(dst.exists())
			{
				Logs.log("File exists at destination. Move cancelled: from " + src.getPath() + " to " + dst.getPath(), 0, "FileUtility");
				return false;
			}
		}
		else
		{
			if(dst.exists())
			{
				try
				{
					if(dst.isDirectory())
					{
						deleteDir(dst);						
					}
					else
					{
						FileUtils.forceDelete(dst); // make room for the new
						// file or else moveFile
						// will error
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
					Logs.log("Needed to delete a file but couldn't. Please close " + dst.getPath() + " if is open in another program. It interferes with file permissions for JEX to manage the database properly.", 0, FileUtility.class);
					return false;
				}
			}
		}
		
		// The destination location has been cleared. Now things can be moved.
		if(src.isDirectory())
		{
			try
			{
				File dstParent = dst.getParentFile();
				if(!dstParent.exists())
				{
					dstParent.mkdirs();
				}
				FileUtils.moveDirectory(src, dst);
				return true;
			}
			catch (IOException e)
			{
				Logs.log("Attempted to move the directory " + src.getPath() + " to " + dst.getPath() + " but couldn't. Not sure why... check stack trace. ", 0, FileUtility.class);
				throw e;
			}
		}
		else
		{
			try
			{
				File dstParent = dst.getParentFile();
				if(!dstParent.exists())
				{
					dstParent.mkdirs();
				}
				FileUtils.moveFile(src, dst);
				return true;
			}
			catch (IOException e)
			{
				Logs.log("Attempted to move the file " + src.getPath() + " to " + dst.getPath() + " but couldn't. Not sure why... check stack trace. ", 0, FileUtility.class);
				throw e;
			}
		}
	}
	
	public static String getFileNameWithoutExtension(String pathOrFilename)
	{
		String fileName = getFileNameWithExtension(pathOrFilename);
		if(fileName == null)
		{
			return null;
		}
		
		int index = fileName.lastIndexOf('.');
		if(index == -1 || index > fileName.length())
		{
			return pathOrFilename;
		}
		String result = fileName.substring(0, index);
		
		return result;
	}
	
	public static String makeOSCompatible(String path)
	{
		//SystemUtils.IS_OS_WINDOWS
		String os = System.getProperty("os.name");
		
		if (path == null) return path;
		else if(os.startsWith("Windows"))
		{
			if (path.contains("/"))
			{
				String newPath = path.replace("/", "\\");
				return newPath;
			}
			else return path;
		}
		else if (os.startsWith("Mac"))
		{
			if (path.contains("\\"))
			{
				String newPath = path.replace("\\", "/");
				return newPath;
			}
			else return path;
		}
		else return path;
	}
	
	public static String removeWhiteSpaceOnEnds(String s)
	{
		return StringUtility.removeWhiteSpaceOnEnds(s);
	}
	
	public static String getFileNameSuffixDigits(String pathOrFilename)
	{
		String filename = FileUtility.getFileNameWithoutExtension(pathOrFilename);
		String suffix = "";
		for (int j = filename.length() - 1; j >= 0; j--)
		{
			if(Character.isDigit(filename.charAt(j)))
			{
				suffix = filename.substring(j, filename.length());
			}
			else
			{
				break;
			}
		}
		return suffix;
	}
	
	public static String getFileNameWithExtension(String pathOrFilename)
	{
		if(pathOrFilename == null)
		{
			return null;
		}
		
		File temp = new File(pathOrFilename);
		temp = temp.getAbsoluteFile();
		String fileName = temp.getName();
		return fileName;
	}
	
	public static String getFileNameExtension(String pathOrFilename)
	{
		if(pathOrFilename == null)
		{
			return null;
		}
		
		int index = pathOrFilename.lastIndexOf('.');
		if(index == -1 || index > pathOrFilename.length())
		{
			return null;
		}
		String result = pathOrFilename.substring(index + 1);
		
		return result;
	}
	
	public static String[] splitFilePathOnSeparator(String path)
	{
		return path.split(Pattern.quote(File.separator));
	}
	
	public static String getFileParent(String path)
	{
		if(path == null)
		{
			return null;
		}
		
		File temp = new File(path);
		temp = temp.getAbsoluteFile();
		return temp.getParent();
	}
	
	public static boolean isFileInDirectory(File f, File directory)
	{
		File folder = f.getParentFile();
		if(folder.equals(directory))
		{
			return true;
		}
		return false;
	}
	
	public static List<File> getSortedFileList(File[] files)
	{
		List<File> fileList = new Vector<File>();
		if(files == null || files.length == 0)
		{
			return fileList;
		}
		for (File f : files)
		{
			fileList.add(f);
		}
		sortFileList(fileList);
		return fileList;
	}
	
	public static void sortFileList(List<File> files)
	{
		Collections.sort(files, new FileUtility());
	}
	
	/**
	 * Path comparator method for sorting fileLists
	 */
	@Override
	public int compare(File thisFile, File thatFile)
	{
		try
		{
			int ret = StringUtility.compareString(thisFile.getCanonicalPath(), thatFile.getCanonicalPath());
			return ret;
		}
		catch (IOException e)
		{
			Logs.log("Couldn't resolve one of the following path strings to a canonical path: " + thisFile.getPath() + " and " + thatFile.getPath(), 0, this);
			e.printStackTrace();
			return -1;
		}
	}
	
	public static String openPath(String path, boolean directories, Component comp)
	{
		JFileChooser chooser = new JFileChooser();
		if(directories)
		{
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		}
		
		// Set the folder
		File filepath = new File(path);
		if(filepath.isDirectory())
		{
			chooser.setCurrentDirectory(filepath);
		}
		else
		{
			File filefolder = filepath.getParentFile();
			chooser.setCurrentDirectory(filefolder);
		}
		
		String savepath = "";
		File saveFile = null;
		int returnVal = chooser.showOpenDialog(comp);
		
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			saveFile = chooser.getSelectedFile();
			savepath = saveFile.getAbsolutePath();
		}
		else
		{
			Logs.log("Not possible to choose that file...", 1, null);
		}
		
		if(saveFile == null)
		{
			return null;
		}
		
		File folder = saveFile.getParentFile();
		if(!folder.exists())
		{
			folder.mkdirs();
		}
		
		return savepath;
	}
	
	public static String savePath(String path, boolean directories, Component comp)
	{
		JFileChooser chooser = new JFileChooser();
		if(directories)
		{
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		}
		
		// Set the folder
		File filepath = new File(path);
		if(filepath.isDirectory())
		{
			chooser.setCurrentDirectory(filepath);
		}
		else
		{
			File filefolder = filepath.getParentFile();
			chooser.setCurrentDirectory(filefolder);
		}
		
		String savepath = "";
		File saveFile = null;
		int returnVal = chooser.showSaveDialog(comp);
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			saveFile = chooser.getSelectedFile();
			savepath = saveFile.getAbsolutePath();
		}
		else
		{
			Logs.log("Not possible to save in that file...", 1, null);
		}
		
		if(saveFile == null)
		{
			return null;
		}
		
		File folder = saveFile.getParentFile();
		if(!folder.exists())
		{
			folder.mkdirs();
		}
		
		return savepath;
	}
	
	public static String getNextName(String path, String currentName, String desiredPrefix)
	{
		int count = 0;
		File test = new File(path + File.separator + desiredPrefix + count + "_" + currentName);
		while (test.exists())
		{
			count++;
			test = new File(path + File.separator + desiredPrefix + count + "_" + currentName);
		}
		return (desiredPrefix + count + "_" + currentName);
	}
	
	/**
	 * Gets the absolute paths of all files within a File array and adds them 
	 * to a LSVList of absolute paths. Recursively searches through all 
	 * sub-directories of a directory.
	 * 
	 * @param files a File list
	 * @param list a LSVList of absolute paths
	 * @return a LSVList of absolute paths
	 */
	public static SVList getAllAbsoluteFilePaths(File[] files, SVList list) {
		for (File file: files) {
			if (file.isDirectory()) {
				getAllAbsoluteFilePaths(file.listFiles(), list);
			}
			else if (!file.getAbsolutePath().contains(".jxd")){
				list.append(file.getAbsolutePath());
			}
		}
		
		
		return list;
	}
}
