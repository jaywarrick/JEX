package Database.SingleUserDatabase;

import Database.DBObjects.JEXEntry;
import Database.Definition.Bookmark;
import Database.Definition.Experiment;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import jex.JEXLabelColorCode;
import jex.statics.JEXStatics;
import logs.Logs;
import plugins.labelManager.ColorPallet;
import preferences.XPreferences;
import preferences.XPreferences_Utilities;

public class JEXDBInfo {
	
	// statics
	public static String DB_NAME = "Name";
	public static String DB_INFO = "Info";
	public static String DB_DATE = "Creation date";
	public static String DB_MDATE = "Modified date";
	public static String DB_AUTHOR = "Author";
	public static String DB_PWD = "Password";
	public static String DB_PWDREQUIRED = "PasswordProtected";
	public static String DB_VERSION = "Version";
	public static String DB_TYPE = "Type";
	
	public static String DB_EXPTABLE = "Experimental Table";
	public static String DB_LABELCOLORCODE = "Label Color Code";
	public static String DB_LABELCOLORCODE_COLOR = "Color";
	
	// Static Version and filename info
	public static String LOCAL_DBINFO_CURRENTVERSION = "JEX4Database.jex";
	public static HashSet<String> LOCAL_DBINFO_OLDVERSIONS = new HashSet<String>();
	static
	{
		LOCAL_DBINFO_OLDVERSIONS.add("JEX3Database_info.txt");
	}
	
	// File system statics
	public static String ATTACHEDFILES = "Attached Files";
	public static String NOTES = "Note.rtf";
	
	// Core
	private XPreferences xinfo;
	
	// Classy variables
	private String jexPath;
	private boolean isLoaded;
	
	// bookmarks
	private Set<Bookmark> bookmarks;
	
	public JEXDBInfo(String jexPath)
	{
		this.setPath(jexPath);
	}
	
	// ---------------------------------------------
	// Getters and setters
	// ---------------------------------------------
	
	/**
	 * Return the name of the database
	 */
	public String getName()
	{
		String result = xinfo.get(DB_NAME, "No name found");
		return result;
	}
	
	/**
	 * Returns the directory in which the database files and folder structure is saved
	 * 
	 * @return
	 */
	public String getDirectory()
	{
		File jexFile = new File(jexPath);
		File fileDirectory = jexFile.getParentFile();
		String result = fileDirectory.getAbsolutePath();
		return result;
	}
	
	/**
	 * sets the jex file path of this databaseInfo object causes reload using the information at this new location
	 * 
	 * @param jexPath
	 */
	public void setPath(String jexPath)
	{
		this.jexPath = jexPath;
		this.xinfo = new XPreferences(this.jexPath);
	}
	
	/**
	 * Returns true if the database has been loaded
	 * 
	 * @return
	 */
	public boolean isLoaded()
	{
		return this.isLoaded;
	}
	
	/**
	 * Set the loaded flag
	 * 
	 * @param isLoaded
	 */
	public void setIsLoaded(boolean isLoaded)
	{
		this.isLoaded = isLoaded;
	}
	
	public String get(String key)
	{
		String result = xinfo.get(key, "");
		return result;
	}
	
	public void set(String key, String value)
	{
		xinfo.put(key, value);
	}
	
	public String getVersion()
	{
		return this.get(DB_VERSION);
	}
	
	public void setVersion(String version)
	{
		this.set(DB_VERSION, version);
	}
	
	public String getType()
	{
		String type = this.get(DB_TYPE);
		if(type == null || type.equals(""))
			type = JEXDB.LOCAL_DATABASE;
		return type;
	}
	
	public void setType(String type)
	{
		this.set(DB_TYPE, type);
	}
	
	// ---------------------------------------------
	// Experimental table
	// ---------------------------------------------
	
	// public TreeMap<String,Experiment> getExperimentalTable()
	// {
	// TreeMap<String,Experiment> result = new TreeMap<String,Experiment>();
	//
	// // Get the exptable child node
	// XPreferences expPrefs = xinfo.getChildNode(DB_EXPTABLE);
	//
	// // Get the child experiments
	// List<XPreferences> experiments = expPrefs.getChildNodes();
	//
	// // Loop through the experiments
	// for (XPreferences experiment: experiments)
	// {
	// // Get the variables to create an experiment class
	// String expName = experiment.get(JEXEntry.EXPERIMENT);
	// String expInfo = experiment.get(JEXEntry.INFO);
	// String expDate = experiment.get(JEXEntry.DATE);
	// String expMDate = experiment.get(JEXEntry.DATE);
	// String expAuthor = experiment.get(JEXEntry.AUTHOR);
	// String expNumber = experiment.get(Experiment.NUMBER);
	//
	// // Make an experiment class
	// Experiment exp = new
	// Experiment(expName,expInfo,expDate,expMDate,expAuthor,expNumber);
	//
	// // Add it to the table
	// result.put(expName, exp);
	// }
	//
	// return result;
	// }
	
	public void setExperimentalTable(TreeMap<String,Experiment> expTable)
	{
		// Remove the old experiment table
		xinfo.removeNode(DB_EXPTABLE);
		
		// Make a new one
		XPreferences expPrefs = xinfo.getChildNode(DB_EXPTABLE);
		
		// Loop through the expTable
		for (String expName : expTable.keySet())
		{
			// Get the experiment
			Experiment exp = expTable.get(expName);
			
			// Get the variables
			String expInfo = exp.expInfo;
			String expDate = exp.expDate;
			String expMDate = exp.expMDate;
			String expAuthor = exp.expAuthor;
			String expNumber = exp.expNumber;
			
			// Make a new node in the prefs
			XPreferences experiment = expPrefs.getChildNode(expName);
			
			// Fill the node
			experiment.put(JEXEntry.EXPERIMENT, expName);
			experiment.put(JEXEntry.INFO, expInfo);
			experiment.put(JEXEntry.DATE, expDate);
			experiment.put(JEXEntry.DATE, expMDate);
			experiment.put(JEXEntry.AUTHOR, expAuthor);
			experiment.put(Experiment.NUMBER, expNumber);
		}
	}
	
	// ---------------------------------------------
	// Labels and colors
	// ---------------------------------------------
	
	/**
	 * Returns the label color code saved in the info file
	 */
	public void fillLabelColorCode()
	{
		JEXLabelColorCode result = JEXStatics.labelColorCode;
		
		// Get the label color code child node
		XPreferences labelPrefs = this.getLabels();
		
		// Get the child label
		List<XPreferences> labels = labelPrefs.getChildNodes();
		
		// Loop through the label names
		for (XPreferences label : labels)
		{
			// Get the label name
			String labelName = label.getName();
			
			// Loop through the values
			int count = 0;
			for (XPreferences value : label.getChildNodes())
			{
				try
				{
					// Get the label value
					Color color = ColorPallet.getColor(count);
					String labelValue = value.getName();
					String labelColor = value.get(JEXDBInfo.DB_LABELCOLORCODE_COLOR);
					Logs.log(labelValue + " has color of " + labelColor, 0, this);
					if(!ColorPallet.hasColor(labelColor))
					{
						color = ColorPallet.getColor(count);
						value.put(JEXDBInfo.DB_LABELCOLORCODE_COLOR, ColorPallet.colorToString(color));
					}
					else
					{
						color = ColorPallet.stringToColor(labelColor);
					}
					Logs.log(labelValue + " now has matching color " + (ColorPallet.colorToString(color).equals(value.get(JEXDBInfo.DB_LABELCOLORCODE_COLOR))) + " and colorIndex = " + ColorPallet.getColorIndex(color), 0, this);
					result.setColorForLabel(labelName, labelValue, color);
				}
				catch (Exception e)
				{
					Logs.log("Error in reading label " + labelName, 1, null);
					continue;
				}
				count = count + 1;
			}
		}
	}
	
	// ---------------------------------------------
	// Bookmarks
	// ---------------------------------------------
	
	/**
	 * Add a bookmark to this database
	 */
	public void addBookmark(Bookmark bookmark)
	{
		if(bookmarks == null)
			return;
		bookmarks.add(bookmark);
	}
	
	/**
	 * Return the bookmarks of this database
	 */
	public Set<Bookmark> getBookmarks()
	{
		return bookmarks;
	}
	
	/**
	 * Set the bookmark list for the database
	 */
	public void setBookmarks(Set<Bookmark> bookmarks)
	{
		this.bookmarks = bookmarks;
	}
	
	// ---------------------------------------------
	// Peripheral data and attached files
	// ---------------------------------------------
	
	public String getAbsolutePath()
	{
		File f = new File(this.jexPath);
		return f.getAbsolutePath();
	}
	
	public XPreferences getXML()
	{
		return this.xinfo;
	}
	
	public XPreferences getLabels()
	{
		return this.xinfo.getChildNode(DB_LABELCOLORCODE);
	}
	
	/**
	 * Finds the list of database contained in a repository
	 * 
	 * @param rep
	 * @return
	 */
	public static JEXDBInfo[] findDatabasesInRepository(Repository rep)
	{
		List<JEXDBInfo> resultList = new ArrayList<JEXDBInfo>(0);
		
		// Test the path return null if it's not a possible path
		File toLoad = new File(rep.getPath());
		if(!toLoad.exists())
		{
			return new JEXDBInfo[0];
		}
		if(!toLoad.isDirectory())
		{
			return new JEXDBInfo[0];
		}
		
		// Create the result list and loop through the folders
		File[] subFiles = toLoad.listFiles();
		if(subFiles == null)
			return new JEXDBInfo[0];
		
		for (int i = 0; i < subFiles.length; i++)
		{
			File f = subFiles[i];
			
			// If the file is not a folder then continue
			if(!f.isDirectory() || f.getName().startsWith("."))
				continue;
			
			// Loop through the files in that folder
			File[] subsubFiles = f.listFiles();
			
			// Set a flag to see if a repository was found
			boolean repositoryFound = false;
			
			// Loop thorugh the files a first time to find all up-to-date
			// databases
			for (int j = 0; j < subsubFiles.length; j++)
			{
				File f2 = subsubFiles[j];
				
				// If the sub-folder contains a file named Database.xml it's
				// likely a database
				// So load it into the list
				if(f2.getName().equals(JEXDBInfo.LOCAL_DBINFO_CURRENTVERSION))
				{
					// Create a new database wrap
					JEXDBInfo dbInfo = new JEXDBInfo(f2.getAbsolutePath());
					resultList.add(dbInfo);
					
					// move on to the next folder
					repositoryFound = true;
				}
			}
			
			// If none were found, loop through to look for old ones
			if(repositoryFound)
				continue;
			for (int j = 0; j < subsubFiles.length; j++)
			{
				File f2 = subsubFiles[j];
				
				if(JEXDBInfo.LOCAL_DBINFO_OLDVERSIONS.contains(f2.getName()))
				{
					// Load the old info file into a new XPreferences
					String infoPath = rep.getPath() + File.separator + f.getName() + File.separator + "JEX3Database_info.txt";
					XPreferences infoPrefs = XPreferences_Utilities.updateFromVersion3(infoPath);
					
					// Save into the new info file path
					infoPath = rep.getPath() + File.separator + f.getName() + File.separator + JEXDBInfo.LOCAL_DBINFO_CURRENTVERSION;
					infoPrefs.setPath(infoPath);
					infoPrefs.save();
					
					// Create a new database wrap
					JEXDBInfo dbInfo = new JEXDBInfo(infoPath);
					resultList.add(dbInfo);
					
					// move on to the next folder
					break;
				}
			}
		}
		Logs.log("Scanned " + subFiles.length + " files and found " + resultList.size() + " databases", 1, null);
		
		JEXDBInfo[] result = new JEXDBInfo[resultList.size()];
		for (int index = 0; index < resultList.size(); index++)
		{
			result[index] = resultList.get(index);
		}
		return result;
	}
}
