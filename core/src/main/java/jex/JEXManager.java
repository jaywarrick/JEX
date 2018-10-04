package jex;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.Definition.Bookmark;
import Database.Definition.DatabaseLevel;
import Database.Definition.DimensionGroupMap;
import Database.Definition.Experiment;
import Database.Definition.Filter;
import Database.Definition.FilterSet;
import Database.Definition.HierarchyLevel;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXDB;
import Database.SingleUserDatabase.JEXDBIO;
import Database.SingleUserDatabase.JEXDBInfo;
import Database.SingleUserDatabase.JEXWriter;
import Database.SingleUserDatabase.Repository;
import Database.SingleUserDatabase.tnvi;
import jex.infoPanels.InfoPanelController;
import jex.jexTabPanel.jexLabelPanel.LabelsPanel;
import jex.objectAndEntryPanels.JEXDataPanel;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.statics.PrefsUtility;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import preferences.XPreferences;
import signals.SSCenter;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

public class JEXManager {
	
	public static String ALL_DATABASE = "all";
	public static String FILTERED_DATABASE = "filtered";
	public static String ARRAY_0D = "0D";
	public static String ARRAY_1D = "1D";
	public static String ARRAY_2D = "2D";
	public static String ARRAY_LINK = "LINK";
	
	// JEX gui level
	public static String VIEWINGPANEL = "viewingpanel";
	
	// Session level
	public static String INFOPANELS_EXP = "InfoPanels";
	public static String INFOPANELS_ARR = "InfoPanels";
	public static String USERNAME = "username";
	public static String DATABASES = "databases";
	public static String CLICKDATABASE = "clickeddatabase";
	
	// database level
	public static String BOOKMARKS = "bookmarks";
	public static String OPENDATABASE = "opendatabase";
	public static String HIERARCHY = "hierarchy";
	public static String SELECTEDENTRY = "selectedentry";
	public static String DATASETS = "datasets";
	
	// viewing options
	public static String ARRAYVIEWMODE = "arrayviewmode";
	public static String ARRAYDATADISPLAY = "ARRAYDATADISPLAY";
	
	// browsing the database
	public static String EXPERIMENTTREE_CHANGE = "EXPERIMENTREE_CHANGE";
	public static String NAVIGATION = "navigation";
	
	// selected entries
	public static String SELECTION = "selection";
	
	// Object and label lists
	public static String SCOPE = "scope";
	public static String SELECTEDOBJ = "selectedobject";
	public static String SELECTEDLABEL = "selectedlabel";
	public static String AVAILABLEOBJ = "availableobjects";
	public static String AVAILABLELAB = "availablelabels";
	
	// change in filtering
	public static String FILTERSET = "filterset";
	
	// Viewed entry options
	public static String VIEWEDENTRY = "viewedEntry";
	public static String ENTRYVALID = "entryValid";
	
	// Viewed data
	public static String DATAVIEWED = "dataviewed";
	public static String DATADIMTABLE = "datadimtable";
	
	// Statistics viewing
	public static String STATSFILTERS = "statistics filters";
	public static String STATSGROUPS = "statistics groups";
	public static String STATSVALUEOBJ = "statistics data";
	public static String STATSRESULTS = "statistics result";
	
	// User name info
	private String userName = null;
	
	// Databases available for each repository
	private TreeMap<Repository,JEXDBInfo[]> databases = null;
	
	// Current database
	private JEXDB currentDatabase = null;
	private JEXDBInfo currentDBInfo = null;
	
	// Viewing panel, e.g. the experimental browser, or the statistics panel
	private String viewingPanel = null;
	
	// Experimental viewing mode, e.g. 1D or 2D viewing
	private String arrayViewingMode = ARRAY_LINK;
	
	// Viewed hierarchy level
	private String viewedExperiment = null;
	private String viewedArray = null;
	
	// Scope and visibility of the elements
	private HashMap<String,String> scopeSet;
	
	// Object selected for viewing
	private TypeName selectedObject = null;
	private tnvi objects;
	
	// Label selected for viewing
	private TypeName selectedLabel = null;
	private TreeMap<String,TreeMap<String,Set<JEXEntry>>> labels;
	
	// Filterset applied
	private FilterSet filterSet = null;
	private List<TypeName> groups = null;
	
	// Selection
	private TreeSet<JEXEntry> selectedEntries;
	
	// Info panels
	private TreeMap<String,InfoPanelController> infoPanelControllersExp;
	private TreeMap<String,InfoPanelController> infoPanelControllersArr;
	
	// Array View
	private JEXEntry viewedEntry;
	private boolean displayDataInArray;
	
	// Data view
	private DimensionMap viewedData;
	private DimTable dimTable;
	
	// Statistics variables
	private List<TypeName> statsGroups = null;
	private FilterSet statsFilterSet = null;
	private TypeName statsValueObject = null;
	
	public JEXManager()
	{
		// reset variables
		this.reset();
	}
	
	/**
	 * Reset the manager variables
	 */
	private void reset()
	{
		this.userName = null;
		this.databases = null;
		this.currentDatabase = null;
		this.viewingPanel = null;
		this.arrayViewingMode = ARRAY_1D;
		this.viewedExperiment = null;
		this.viewedArray = null;
		this.selectedObject = null;
		this.selectedLabel = null;
		this.selectedEntries = null;
		this.filterSet = null;
		this.groups = null;
		this.infoPanelControllersExp = null;
		this.infoPanelControllersArr = null;
		this.displayDataInArray = true;
		
		// Make the scope set
		this.scopeSet = new HashMap<String,String>();
	}
	
	// ---------------------------------------------
	// Session managing
	// ---------------------------------------------
	/**
	 * Set the panel to be viewed
	 */
	public void setViewingPanel(String viewingPanel)
	{
		this.viewingPanel = viewingPanel;
		
		// Send a signal of change of array viewing mode
		Logs.log("Send signal of viewed panel change", 1, this);
		SSCenter.defaultCenter().emit(this, VIEWINGPANEL, (Object[]) null);
	}
	
	/**
	 * Get the panel currently viewed
	 * 
	 * @return
	 */
	public String getViewingPanel()
	{
		return this.viewingPanel;
	}
	
	// ---------------------------------------------
	// Session managing
	// ---------------------------------------------
	
	/**
	 * Return true if user has logged on
	 * 
	 * @return boolean
	 */
	public boolean isLoggedOn()
	{
		if(this.userName == null)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Create a new user file
	 * 
	 * @param userFile
	 * @return
	 */
	public boolean createUser(File userFile)
	{
		boolean result = true;
		
		// Saving the file
		File folder = userFile.getParentFile();
		if(!folder.exists())
		{
			folder.mkdirs();
		}
		
		// Creating the file content
		XPreferences prefs = new XPreferences();
		
		// Create the user preferences node
		XPreferences userPrefernces = prefs.getChildNode("User Preferences");
		userPrefernces.put("Consolidate Database", "false");
		
		// Make octave preferences
		XPreferences octavePrefernces = userPrefernces.getChildNode("Octave");
		octavePrefernces.put("OctavePath", "/Applications/Octave.app/Contents/Resources/bin/octave");
		octavePrefernces.putBoolean("AutoDrawNow", true);
		octavePrefernces.putInt("History length", 100);
		
		// Create the repository node
		@SuppressWarnings("unused")
		XPreferences repositoryNode = prefs.getChildNode("Repositories");
		
		// Save
		prefs.saveToPath(userFile.getAbsolutePath());
		
		return result;
	}
	
	/**
	 * Set the username
	 * 
	 * @param userName
	 */
	public boolean logOn(File userFile)
	{
		this.reset();
		
		// Change username and send signal
		Logs.log("Triggering new database creation", 1, this);
		this.userName = FileUtility.getFileNameWithoutExtension(userFile.getName());
		SSCenter.defaultCenter().emit(this, USERNAME, (Object[]) null);
		
		// Get the root preferences
		PrefsUtility.loadPrefs(userFile.getAbsolutePath());
		
		// Get the list of repositories
		List<Repository> repositoryList = PrefsUtility.getRepositoryList();
		
		// Get the databases in the repositories
		for (int index = 0; index < repositoryList.size(); index++)
		{
			Repository rep = repositoryList.get(index);
			
			// Find databases inside
			JEXDBInfo[] dbs = JEXDBInfo.findDatabasesInRepository(rep);
			
			// Send to log
			Logs.log("Added repository " + rep.getPath() + " with " + dbs.length + " databases", 1, this);
			
			// Add it to the hash map
			// initialize the variable if necessary
			if(this.databases == null)
			{
				this.databases = new TreeMap<Repository,JEXDBInfo[]>();
			}
			this.databases.put(rep, dbs);
		}
		
		// Emit signal of repository hashmap change
		Logs.log("Send signal of change of database list", 1, this);
		SSCenter.defaultCenter().emit(this, DATABASES, (Object[]) null);
		
		// Add the user file to the recently opened list
		java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(this.getClass());
		String fileCSV = prefs.get("Recent user files", "");
		CSVList csv = new CSVList(fileCSV);
		if(!csv.contains(userFile.getAbsolutePath()))
		{
			csv.add(0, userFile.getAbsolutePath());
			if(csv.size() > 5)
			{
				for (int i = 0, len = csv.size() - 5; i < len; i++)
				{
					csv.remove(5);
				}
			}
			fileCSV = csv.toString();
			prefs.put("Recent user files", fileCSV);
		}
		
		return true;
	}
	
	/**
	 * Set the username
	 * 
	 * @param userName
	 */
	public boolean logOff()
	{
		this.userName = null;
		this.reset();
		
		return true;
	}
	
	/**
	 * Get the user login name
	 * 
	 * @return
	 */
	public String getUserName()
	{
		return this.userName;
	}
	
	/**
	 * Order the creation of a new repository to look for databases
	 * 
	 * @param type
	 * @param name
	 * @param usr
	 * @param pswd
	 */
	public boolean createRepository(String path, String usr, String pswd)
	{
		// initialize the variable if necessary
		if(this.databases == null)
		{
			this.databases = new TreeMap<Repository,JEXDBInfo[]>();
		}
		
		// Make the new repository
		Repository rep = new Repository(path, usr, pswd);
		
		// Find databases inside
		JEXDBInfo[] dbs = JEXDBInfo.findDatabasesInRepository(rep);
		
		// Add it to the hash map
		boolean done = PrefsUtility.addRepository(rep);
		if(!done)
		{
			Logs.log("Repository saving failed", 1, this);
			return false;
		}
		this.databases.put(rep, dbs);
		
		// Send a signal of change of repository list
		Logs.log("Send signal of change of database list", 1, this);
		SSCenter.defaultCenter().emit(this, DATABASES, (Object[]) null);
		
		return true;
	}
	
	/**
	 * Remove a repository from the list
	 * 
	 * @param rep
	 * @return boolean
	 */
	public boolean removeRepository(Repository rep)
	{
		Object[] options = { "Yes", "No" };
		int n = JOptionPane.showOptionDialog(JEXStatics.main, "Are you sure you want to delete repository from list", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if(n == 1)
		{
			return false;
		}
		
		Logs.log("Repoistory will be removed", 0, this);
		boolean done = PrefsUtility.removeRepository(rep);
		
		if(!done)
		{
			Logs.log("Repository removal failed", 1, this);
			return false;
		}
		this.databases.remove(rep);
		
		// Send a signal of change of repository list
		Logs.log("Send signal of change of database list", 1, this);
		SSCenter.defaultCenter().emit(this, DATABASES, (Object[]) null);
		
		return true;
	}
	
	/**
	 * Edit a repository from the list
	 * 
	 * @param rep
	 * @param type
	 * @param path
	 * @param usr
	 * @param pswd
	 * @return boolean
	 */
	public boolean editRepository(Repository rep, String path, String usr, String pswd)
	{
		// initialize the variable if necessary
		if(this.databases == null)
		{
			this.databases = new TreeMap<Repository,JEXDBInfo[]>();
		}
		
		// Make the new repository
		rep.setPassword(pswd);
		rep.setPath(path);
		rep.setUserName(usr);
		
		// Find databases inside
		JEXDBInfo[] dbs = JEXDBInfo.findDatabasesInRepository(rep);
		
		// Add it to the hash map
		boolean done = PrefsUtility.updateRepository(rep);
		if(!done)
		{
			Logs.log("Repository saving failed", 1, this);
			return false;
		}
		this.databases.put(rep, dbs);
		
		// Send a signal of change of repository list
		Logs.log("Send signal of change of database list", 1, this);
		SSCenter.defaultCenter().emit(this, DATABASES, (Object[]) null);
		
		return true;
	}
	
	/**
	 * Return the list of user files that have been opened recently
	 * 
	 * @return
	 */
	public String[] getRecentelyOpenedUserFiles()
	{
		java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(this.getClass());
		String fileCSV = prefs.get("Recent user files", "");
		CSVList csv = new CSVList(fileCSV);
		String[] result = csv.toStringArray();
		return result;
	}
	
	// ---------------------------------------------
	// Database managing
	// ---------------------------------------------
	
	/**
	 * Create a new Database
	 */
	public boolean createNewDatabase(String type, Repository rep, String name, String info, String password)
	{
		Logs.log("Creating Database", 1, this);
		
		// Create a new database in the right repository
		JEXDBInfo db = JEXWriter.createDBInfo(rep, name, info, password);
		
		// Add it to the database map
		if(db == null)
		{
			return false;
		}
		JEXDBInfo[] dbs = this.databases.get(rep);
		if(dbs == null)
		{
			dbs = new JEXDBInfo[1];
			dbs[0] = db;
			this.databases.put(rep, dbs);
		}
		else
		{
			JEXDBInfo[] newDBs = new JEXDBInfo[dbs.length + 1];
			for (int i = 0; i < dbs.length; i++)
			{
				newDBs[i] = dbs[i];
			}
			newDBs[dbs.length] = db;
			this.databases.put(rep, newDBs);
		}
		
		// Send a signal of change of repository list
		Logs.log("Send signal of change of database list", 1, this);
		SSCenter.defaultCenter().emit(this, DATABASES, (Object[]) null);
		
		return true;
	}
	
	/**
	 * Return a hashmap of available databases for all defined repositories
	 */
	public TreeMap<Repository,JEXDBInfo[]> getAvailableDatabases()
	{
		return this.databases;
	}
	
	/**
	 * Set the list of avaialable databases... this causes a signal to be emited on the DATABSES channel
	 * 
	 * @param databases
	 */
	public void setAvailableDatabases(TreeMap<Repository,JEXDBInfo[]> databases)
	{
		this.databases = databases;
		
		// Emit signal of database list change
		Logs.log("Send signal of change of database list", 1, this);
		SSCenter.defaultCenter().emit(this, DATABASES, (Object[]) null);
	}
	
	/**
	 * Returns the currently browsed database
	 * 
	 * @return
	 */
	public JEXDB getCurrentDatabase()
	{
		return this.currentDatabase;
	}
	
	/**
	 * Return true if the current database contains unsaved modifications
	 * 
	 * @return
	 */
	public boolean isCurrentDatabaseModified()
	{
		if(this.getCurrentDatabase() == null)
		{
			return false;
		}
		return this.getCurrentDatabase().containsUnsavedData();
	}
	
	/**
	 * Save the current database
	 */
	public void saveCurrentDatabase()
	{
		
		if(this.getCurrentDatabase() == null || this.getDatabaseInfo() == null)
		{
			JEXStatics.statusBar.setStatusText("No database loaded");
			return;
		}
		
		// Save the database
		boolean success = JEXDBIO.saveDB(this.getCurrentDatabase());
		
		// Save the db info
		JEXWriter.saveBDInfo(this.getDatabaseInfo());
		
		// Set the flag
		if(success)
		{
			this.getCurrentDatabase().databaseSaved();
			JEXStatics.statusBar.setStatusText("Database saved");
		}
		else
		{
			JEXStatics.statusBar.setStatusText("Error during saving of database. Unsaved changes exist.");
		}
	}
	
	/**
	 * Returns the database info of the current database
	 * 
	 * @return
	 */
	public JEXDBInfo getDatabaseInfo()
	{
		return this.currentDBInfo;
	}
	
	/**
	 * Set the database info and open/load/refresh the database associated to it
	 * 
	 * @param currentDBInfo
	 */
	public void setDatabaseInfo(JEXDBInfo currentDBInfo)
	{
		// Reset the Manager variables
		this.reset();
		
		// Set the database info
		this.currentDBInfo = currentDBInfo;
		DirectoryManager.setHostDirectory(currentDBInfo.getDirectory());
		
		// Change the status bar
		JEXStatics.statusBar.setStatusText("Opening Database");
		
		// Load the database
		JEXDB temp = JEXDBIO.load(JEXWriter.getDatabaseFolder() + File.separator + JEXDBIO.LOCAL_DATABASE_FILENAME);
		if(temp == null)
		{
			JEXStatics.statusBar.setStatusText("Database could not be opened");
			return;
		}
		this.currentDatabase = temp;
		
		this.currentDatabase.setFilterSet(null);
		this.currentDatabase.setGroupingSet(null);
		
		// Load the label color code
		currentDBInfo.fillLabelColorCode(); // sets colors in JEXLabelColorCode
		JEXStatics.labelManager.setLabels(currentDBInfo.getLabels());
		
		// switch viewed panel
		// JEXStatics.main.displayViewPane();
		
		// set the selected entries
		this.setSelectedEntries(null);
		
		// Set the selected labels and objects to null
		this.setSelectedLabel(null);
		this.setSelectedObject(null);
		
		// Set the label list
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> thelabels = this.getTNVI().get(JEXData.LABEL);
		this.setLabels(thelabels);
		
		// Set the objects list
		tnvi theobjects = JEXStatics.jexManager.getTNVI();
		this.setObjects(theobjects);
		
		// Emit signal of viewing mode
		Logs.log("Send signal of experiment viewing mode change", 1, this);
		this.setArrayViewingMode(ARRAY_LINK);
		
		// Emit signal of experiment tree change
		Logs.log("Send signal of experiment tree change", 1, this);
		SSCenter.defaultCenter().emit(this, EXPERIMENTTREE_CHANGE, (Object[]) null);
		
		// Send a signal of change of browsed database
		Logs.log("Send signal of change of database", 1, this);
		SSCenter.defaultCenter().emit(this, OPENDATABASE, (Object[]) null);
		
		// // Set the database / repository info panel
		// DatabaseInfoPanel infoPanel = new DatabaseInfoPanel(currentDBInfo);
		// setInfoPanel("Database", infoPanel);
		
		// Send a signal of change of browsed database
		Logs.log("Send signal of change of database content", 1, this);
		SSCenter.defaultCenter().emit(this, DATASETS, (Object[]) null);
		
		// Change the status bar
		JEXStatics.statusBar.setStatusText("Opened Database");
	}
	
	// ---------------------------------------------
	// Database editing
	// ---------------------------------------------
	
	/**
	 * Check for conflicts of names and create an array of entries in the current database using methods of JEXDB
	 * 
	 * @param expName
	 * @param arrayName
	 * @param info
	 * @param w
	 * @param h
	 * @return false if on already exists and creation was aborted
	 */
	public boolean createEntryArray(String expName, String date, String info, int w, int h)
	{
		String author = this.userName;
		
		// Check if entires already exist in the same experiment and tray
		TreeMap<String,Experiment> experiments = this.getCurrentDatabase().getExperimentalTable();
		Experiment arraysOfSameExperimentName = experiments.get(expName);
		
		if(arraysOfSameExperimentName != null)
		{			
			JEXDialog.messageDialog("Warning: A dataset by that name already exists. No additional dataset created.");
			return false;
		}
		
		JEXStatics.jexDBManager.addEntries(expName, w, h, date, author, info);
		JEXStatics.statusBar.setStatusText("Created db entries");
		return true;
	}
	
	// ---------------------------------------------
	// Database selection
	// ---------------------------------------------
	
	/**
	 * Return the selected entries
	 * 
	 * @return selectedEntries
	 */
	public TreeSet<JEXEntry> getSelectedEntries()
	{
		return this.selectedEntries;
	}
	
	/**
	 * Add entry to selection list
	 * 
	 * @param entry
	 */
	public void addEntryToSelection(JEXEntry entry)
	{
		if(this.selectedEntries == null)
		{
			this.selectedEntries = new TreeSet<JEXEntry>();
		}
		this.selectedEntries.add(entry);
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selection", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTION, (Object[]) null);
	}
	
	/**
	 * Add a list of entries to the selection
	 * 
	 * @param entries
	 */
	public void addEntriesToSelection(Set<JEXEntry> entries)
	{
		if(this.selectedEntries == null)
		{
			this.selectedEntries = new TreeSet<JEXEntry>();
		}
		for (JEXEntry entry : entries)
		{
			this.selectedEntries.add(entry);
		}
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selection", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTION, (Object[]) null);
	}
	
	/**
	 * Set the selection list of entries
	 * 
	 * @param entries
	 */
	public void setSelectedEntries(TreeSet<JEXEntry> entries)
	{
		if(entries == null)
		{
			this.selectedEntries = new TreeSet<JEXEntry>();
		}
		else
		{
			this.selectedEntries = entries;
		}
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selection", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTION, (Object[]) null);
	}
	
	/**
	 * Remove the entry ENTRY from selection list
	 * 
	 * @param entry
	 */
	public void removeEntryFromSelection(JEXEntry entry)
	{
		if(this.selectedEntries == null)
		{
			this.selectedEntries = new TreeSet<JEXEntry>();
		}
		this.selectedEntries.remove(entry);
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selection", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTION, (Object[]) null);
	}
	
	/**
	 * Remove the entries ENTRIES from selection list
	 * 
	 * @param entries
	 */
	public void removeEntriesFromSelection(Set<JEXEntry> entries)
	{
		if(this.selectedEntries == null)
		{
			this.selectedEntries = new TreeSet<JEXEntry>();
		}
		for (JEXEntry entry : entries)
		{
			this.selectedEntries.remove(entry);
		}
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selection", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTION, (Object[]) null);
	}
	
	/**
	 * Return true if one of the entries is contained in the set of selected entries
	 * 
	 * @param entries
	 * @return
	 */
	public boolean isOneSelected(Set<JEXEntry> entries)
	{
		if(entries == null || entries.size() == 0)
		{
			return false;
		}
		
		boolean result = false;
		for (JEXEntry entry : entries)
		{
			result = result || this.isSelected(entry);
		}
		return result;
	}
	
	/**
	 * Return true if all of the entries are contained in the set of selected entries
	 * 
	 * @param entries
	 * @return
	 */
	public boolean isAllSelected(Set<JEXEntry> entries)
	{
		if(entries == null || entries.size() == 0)
		{
			return false;
		}
		
		boolean result = true;
		for (JEXEntry entry : entries)
		{
			result = result && this.isSelected(entry);
		}
		return result;
	}
	
	/**
	 * Return true if the entry ENTRY is contained in the set of selected entries
	 * 
	 * @param entry
	 * @return
	 */
	public boolean isSelected(JEXEntry entry)
	{
		if(this.selectedEntries == null)
		{
			return false;
		}
		return this.selectedEntries.contains(entry);
	}
	
	// ---------------------------------------------
	// Database viewing
	// ---------------------------------------------
	
	/**
	 * Open a bookmarked link
	 */
	public void openLink(Bookmark bookmark)
	{
		Logs.log("Going to link", 1, this);
		
	}
	
	/**
	 * Set the array viewing mode
	 * 
	 * @param arrayViewingMode
	 */
	public void setArrayViewingMode(String arrayViewingMode)
	{
		this.arrayViewingMode = arrayViewingMode;
		
		String exp = this.getViewedExperiment();
		String arr = this.getArrayViewed();
		if(exp == null && arr == null)
		{
			if(arrayViewingMode.equals(ARRAY_2D))
			{
				return;
			}
			else if(arrayViewingMode.equals(ARRAY_1D))
			{
				this.setGroupingSet(null);
			}
			else if(arrayViewingMode.equals(ARRAY_0D))
			{
				this.setGroupingSet(null);
				
				// if no entry is viewed selected the first one
				JEXEntry entry = this.getViewedEntry();
				if(entry == null)
				{
					JEXEntry firstEntry = this.getCurrentDatabase().getEntries().first();
					this.setViewedEntry(firstEntry);
				}
			}
			else if(arrayViewingMode.equals(ARRAY_LINK))
			{
				List<TypeName> ltn = new ArrayList<TypeName>();
				ltn.add(new TypeName(JEXData.HIERARCHY, JEXEntry.EXPERIMENT));
				this.setGroupingSet(ltn);
			}
		}
		else if(exp != null && arr == null)
		{
			if(arrayViewingMode.equals(ARRAY_2D))
			{
				return;
			}
			else if(arrayViewingMode.equals(ARRAY_1D))
			{
				this.setGroupingSet(null);
			}
			else if(arrayViewingMode.equals(ARRAY_0D))
			{
				this.setGroupingSet(null);
				
				// if no entry is viewed selecte the first one
				JEXEntry entry = this.getViewedEntry();
				if(entry == null)
				{
					JEXEntry firstEntry = this.getCurrentDatabase().getFilteredEntries().first();
					this.setViewedEntry(firstEntry);
				}
			}
			else if(arrayViewingMode.equals(ARRAY_LINK))
			{
				List<TypeName> ltn = new ArrayList<TypeName>();
				ltn.add(new TypeName(JEXData.HIERARCHY, JEXEntry.TRAY));
				this.setGroupingSet(ltn);
			}
		}
		else if(exp != null && arr != null)
		{
			if(arrayViewingMode.equals(ARRAY_2D))
			{
				List<TypeName> ltn = new ArrayList<TypeName>();
				ltn.add(new TypeName(JEXData.HIERARCHY, JEXEntry.X));
				ltn.add(new TypeName(JEXData.HIERARCHY, JEXEntry.Y));
				this.setGroupingSet(ltn);
			}
			else if(arrayViewingMode.equals(ARRAY_1D))
			{
				this.setGroupingSet(null);
			}
			else if(arrayViewingMode.equals(ARRAY_0D))
			{
				this.setGroupingSet(null);
				
				// if no entry is viewed selected the first one
				JEXEntry entry = this.getViewedEntry();
				if(entry == null)
				{
					JEXEntry firstEntry = this.getCurrentDatabase().getEntries().first();
					this.setViewedEntry(firstEntry);
				}
				this.updateDimensionTable();
			}
			else if(arrayViewingMode.equals(ARRAY_LINK))
			{
				return;
			}
		}
		
		// Send a signal of change of array viewing mode
		Logs.log("Send signal of arrayed view mode change", 1, this);
		SSCenter.defaultCenter().emit(this, ARRAYVIEWMODE, (Object[]) null);
	}
	
	/**
	 * Return the array viewing mode
	 * 
	 * @return string
	 */
	public String getArrayViewingMode()
	{
		return this.arrayViewingMode;
	}
	
	/**
	 * Return the object to be displayed
	 * 
	 * @return object
	 */
	public TreeMap<Filter,TreeMap<Filter,Set<JEXEntry>>> getViewedArray()
	{
		if(this.getCurrentDatabase() == null)
		{
			return null;
		}
		return this.getCurrentDatabase().getGroupedEntries();
	}
	
	/**
	 * Return the TNVI of the database
	 * 
	 * @return TNVI
	 */
	public tnvi getTNVI()
	{
		if(this.getCurrentDatabase() == null)
		{
			return null;
		}
		return this.getCurrentDatabase().getTNVI();
	}
	
	/**
	 * Return the filtered TNVI of the database
	 * 
	 * @return fTNVI
	 */
	public tnvi getFilteredTNVI()
	{
		tnvi result = this.getCurrentDatabase().getFilteredTNVI();
		if(result == null)
		{
			return new tnvi();
		}
		return result;
		// return this.getCurrentDatabase().getFilteredTNVI();
	}
	
	/**
	 * Return a TNVI dictionary for an entry list
	 * 
	 * @param entries
	 * @return
	 */
	public tnvi getTNVIforEntryList(Set<JEXEntry> entries)
	{
		return this.getCurrentDatabase().getTNVIforEntryList(entries);
	}
	
	/**
	 * Return the labels to be displayed
	 * 
	 * @return
	 */
	public TreeMap<String,TreeMap<String,Set<JEXEntry>>> getLabels()
	{
		// TreeMap<String,TreeMap<String,Set<JEXEntry>>> result =
		// this.getTNVI().get(JEXData.LABEL);
		// return result;
		//
		return this.labels;
	}
	
	/**
	 * Set the label map of available labels to be displayed
	 * 
	 * @param labels
	 */
	public void setLabels(TreeMap<String,TreeMap<String,Set<JEXEntry>>> labels)
	{
		this.labels = labels;
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of labels available", 1, this);
		SSCenter.defaultCenter().emit(this, AVAILABLELAB, (Object[]) null);
	}
	
	/**
	 * Return the labels to be displayed
	 * 
	 * @return object map
	 */
	public tnvi getObjects()
	{
		return this.objects;
	}
	
	/**
	 * Set the object map of available objects to be displayed
	 * 
	 * @param objects
	 */
	public void setObjects(tnvi objects)
	{
		this.objects = objects;
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of object available", 1, this);
		SSCenter.defaultCenter().emit(this, AVAILABLEOBJ, (Object[]) null);
	}
	
	public void updateLabelsAndObjects()
	{
		if(this.viewedExperiment == null)
		{
			this.getCurrentDatabase().setFilteredDictionaries(this.getTNVI(), this.getCurrentDatabase().getEntries());
		}
		else
		{
			Experiment exp = this.getExperimentTree().get(this.viewedExperiment);
			if(exp == null)
			{
				this.getCurrentDatabase().setFilteredDictionaries(this.getTNVI(), this.getCurrentDatabase().getEntries());
			}
			this.getCurrentDatabase().setFilteredDictionaries(exp.tnvi(), exp.getEntries());
		}
		
		// Change the available objects and labels if scope is not the whole
		// database
		String labScope = this.getScope(LabelsPanel.class.toString());
		if(!labScope.equals(ALL_DATABASE))
		{
			this.setLabels(this.getFilteredTNVI().get(JEXData.LABEL));
		}
		else
		{
			this.setLabels(this.getTNVI().get(JEXData.LABEL));
		}
		String objScope = this.getScope(JEXDataPanel.class.toString());
		if(!objScope.equals(ALL_DATABASE))
		{
			this.setObjects(this.getFilteredTNVI());
		}
		else
		{
			this.setObjects(this.getTNVI());
		}
	}
	
	/**
	 * Return the data object in entry ENTRY of typename TN
	 * 
	 * @param tn
	 * @param entry
	 * @return
	 */
	public JEXData getUpdateFlavoredDataOfTypeNameInEntry(TypeName tn, JEXEntry entry)
	{
		JEXData result = this.getCurrentDatabase().getUpdateFlavoredDataOfTypeNameInEntry(tn, entry);
		return result;
	}
	
	/**
	 * Return the data object in entry ENTRY of typename TN
	 * 
	 * @param tn
	 * @param entry
	 * @return
	 */
	public JEXData getDataOfTypeNameInEntry(TypeName tn, JEXEntry entry)
	{
		JEXData result = this.getCurrentDatabase().getDataOfTypeNameInEntry(tn, entry);
		return result;
	}
	
	/**
	 * Return the data object in entry ENTRY of typename TN
	 * 
	 * @param tn
	 * @param entry
	 * @return
	 */
	public Vector<JEXData> getDatasOfTypeWithNameContainingInEntry(TypeName tn, JEXEntry entry)
	{
		Vector<JEXData> result = this.getCurrentDatabase().getDatasOfTypeWithNameContainingInEntry(tn, entry);
		return result;
	}
	
	public Vector<JEXData> getUpdateFlavoredDatasInEntry(JEXEntry entry)
	{
		Vector<JEXData> result = this.getCurrentDatabase().getUpdateFlavoredDatasInEntry(entry);
		return result;
	}
	
	/**
	 * Return a data object of typename TN in the first encountered entry with such and object
	 * 
	 * @param tn
	 * @param entry
	 * @return
	 */
	public JEXData getExampleOfDataWithTypeNameInEntries(TypeName tn, Collection<JEXEntry> entries)
	{
		JEXData ret = null;
		for (JEXEntry entry : entries)
		{
			ret = this.getCurrentDatabase().getDataOfTypeNameInEntry(tn, entry);
			if(ret != null)
			{
				return ret;
			}
		}
		return ret;
	}
	
	/**
	 * Appends " i" to the name of the object where i is indexed to find the next available name. The value of i starts at 1 and increases. The name will not exist in any of the supplied entries.
	 * 
	 * @param tn
	 * @param l
	 * @return
	 */
	public TypeName getNextAvailableTypeNameInEntries(TypeName tn, List<JEXEntry> entries)
	{
		int count = 1;
		TypeName newTN = null;
		if(tn == null || tn.getType() == null || tn.getName() == null || entries.size() == 0)
		{
			return null;
		}
		newTN = new TypeName(tn.getType(), tn.getName() + " " + count);
		while (this.entryListContains(entries, newTN))
		{
			count = count + 1;
			newTN = new TypeName(tn.getType(), tn.getName() + " " + count);
		}
		return newTN;
	}
	
	private boolean entryListContains(List<JEXEntry> entries, TypeName tn)
	{
		for (int i = 0; i < entries.size(); i++)
		{
			if(this.getDataOfTypeNameInEntry(tn, entries.get(i)) != null)
			{
				return true;
			}
		}
		return false;
	}
	
	// ---------------------------------------------
	// Selected Object and Labels
	// ---------------------------------------------
	
	/**
	 * Set the selected object
	 * 
	 * @param object
	 */
	public void setSelectedObject(TypeName object)
	{
		this.selectedObject = object;
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selected object", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTEDOBJ, (Object[]) null);
		
		// // Set the database / repository info panel
		// if (object == null) setInfoPanel("SelectedObject", null);
		// else {
		// SelectedObjectInfoPanel infoPanel = new
		// SelectedObjectInfoPanel(object);
		// setInfoPanel("SelectedObject", infoPanel);
		// }
		
		// Update viewed data if necessary
		this.updateDimensionTable();
		
		// Set this object for statistics too
		this.setSelectedStatisticsObject(object);
	}
	
	/**
	 * Return the typename of the selectedobject
	 * 
	 * @return typename
	 */
	public TypeName getSelectedObject()
	{
		return this.selectedObject;
	}
	
	/**
	 * Set the selected label
	 * 
	 * @param object
	 */
	public void setSelectedLabel(TypeName object)
	{
		this.selectedLabel = object;
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selected label", 1, this);
		SSCenter.defaultCenter().emit(this, SELECTEDLABEL, (Object[]) null);
		
		// // Set the database / repository info panel
		// if (object == null) setInfoPanel("SelectedLabel", null);
		// else {
		// SelectedLabelInfoPanel infoPanel = new
		// SelectedLabelInfoPanel(object);
		// setInfoPanel("SelectedLabel", infoPanel);
		// }
	}
	
	/**
	 * Return the typename of the selectedLabel
	 * 
	 * @return typename
	 */
	public TypeName getSelectedLabel()
	{
		return this.selectedLabel;
	}
	
	/**
	 * Return a map of all labels and label values available for the entry set ENTRIES
	 * 
	 * @param entries
	 * @return
	 */
	public TreeMap<String,TreeSet<String>> getAvailableLabels(Set<JEXEntry> entries)
	{
		// Create the list of labels
		TreeMap<String,TreeSet<String>> result = new TreeMap<String,TreeSet<String>>();
		
		// Loop through the entries and look for entries continaing a label
		// named SELECTEDNAME
		for (JEXEntry entry : entries)
		{
			// get the tnv
			TreeMap<Type,TreeMap<String,JEXData>> tnv = entry.getDataList();
			if(tnv == null)
			{
				continue;
			}
			
			// Get the NV
			TreeMap<String,JEXData> nv = tnv.get(JEXData.LABEL);
			if(nv == null)
			{
				continue;
			}
			
			// Loop through the label names and values
			for (String labelName : nv.keySet())
			{
				// Get the value for labelname
				JEXData valueData = nv.get(labelName);
				
				// Get the current list for label name LABELNAME
				TreeSet<String> values = result.get(labelName);
				
				// if it's null add a new list
				if(values == null)
				{
					values = new TreeSet<String>();
					result.put(labelName, values);
				}
				
				// Add the value to the set
				if(!values.contains(valueData.getDictionaryValue()))
				{
					values.add(valueData.getDictionaryValue());
				}
			}
		}
		
		// return the list
		return result;
	}
	
	/**
	 * Return a map of all labels and label values available for the entry set ENTRIES
	 * 
	 * @param entries
	 * @return
	 */
	public TreeMap<String,String> getAvailableLabels(JEXEntry entry)
	{
		// Create the list of labels
		TreeMap<String,String> result = new TreeMap<String,String>();
		
		// get the tnv
		TreeMap<Type,TreeMap<String,JEXData>> tnv = entry.getDataList();
		if(tnv == null)
		{
			return result;
		}
		
		// Get the NV
		TreeMap<String,JEXData> nv = tnv.get(JEXData.LABEL);
		if(nv == null)
		{
			return result;
		}
		
		// Loop through the label names and values
		for (String labelName : nv.keySet())
		{
			// Get the value for labelname
			JEXData valueData = nv.get(labelName);
			
			// Add it to result list
			result.put(labelName, valueData.getDictionaryValue());
		}
		
		// return the list
		return result;
	}
	
	// ---------------------------------------------
	// Database entry Grouping
	// ---------------------------------------------
	
	/**
	 * Return the set of Typenames listed to group the datasets
	 */
	public List<TypeName> getGroupingSet()
	{
		return this.groups;
	}
	
	/**
	 * Set the grouping list
	 */
	public void setGroupingSet(List<TypeName> tns)
	{
		this.groups = tns;
		this.getCurrentDatabase().setGroupingSet(tns);
		
		// Send a signal of change of browsed database
		Logs.log("Send signal of change of database content", 1, this);
		SSCenter.defaultCenter().emit(this, DATASETS, (Object[]) null);
	}
	
	/**
	 * Add a group to the grouping list
	 */
	public void addGrouping(TypeName tn)
	{
		if(this.groups == null)
		{
			this.groups = new ArrayList<TypeName>(0);
		}
		this.groups.add(tn);
		this.getCurrentDatabase().setGroupingSet(this.groups);
		
		// Send a signal of change of browsed database
		Logs.log("Send signal of change of database content", 1, this);
		SSCenter.defaultCenter().emit(this, DATASETS, (Object[]) null);
	}
	
	// ---------------------------------------------
	// Database filtering
	// ---------------------------------------------
	
	/**
	 * Set the filterset of the databse
	 * 
	 * @param filterset
	 */
	public void setFilterSet(FilterSet filterset)
	{
		this.filterSet = filterset;
		
		this.getCurrentDatabase().setFilterSet(this.filterSet);
		// Change the available objects and labels if scope is not the whole
		// database
		String labScope = this.getScope(LabelsPanel.class.toString());
		if(!labScope.equals(ALL_DATABASE))
		{
			this.setLabels(this.getFilteredTNVI().get(JEXData.LABEL));
		}
		else
		{
			this.setLabels(this.getTNVI().get(JEXData.LABEL));
		}
		String objScope = this.getScope(JEXDataPanel.class.toString());
		if(!objScope.equals(ALL_DATABASE))
		{
			this.setObjects(this.getFilteredTNVI());
		}
		else
		{
			this.setObjects(this.getTNVI());
		}
		
		this.getCurrentDatabase().emitAvailableObjectsSignal();
	}
	
	/**
	 * Add a filter to the filterset... allows rapid rebuilding of the dictionaries rather than resetting the whole filterset Return true is successful
	 * 
	 * @param filter
	 * @return boolean
	 */
	public boolean addFilter(Filter filter)
	{
		if(this.filterSet == null)
		{
			this.filterSet = new FilterSet();
		}
		this.filterSet.add(filter);
		this.getCurrentDatabase().addFilter(filter);
		
		// Emit signal of repository hashmap change
		Logs.log("Send signal of change of database dictionary", 1, this);
		SSCenter.defaultCenter().emit(this, AVAILABLEOBJ, (Object[]) null);
		
		return true;
	}
	
	/**
	 * Set the scope (visibility) of the element of type TYPE to the value SCOPE
	 * 
	 * @param type
	 *            element type
	 * @param scope
	 *            scope
	 */
	public void setScope(String type, String scope)
	{
		String oldScope = this.scopeSet.get(type);
		
		// Test if the scope really has changed
		if(oldScope == null || !oldScope.equals(scope))
		{
			// Set the new scope
			this.scopeSet.put(type, scope);
			
			// Emit signal of repository hashmap change
			Logs.log("Send signal of change of scope", 1, this);
			SSCenter.defaultCenter().emit(this, SCOPE, (Object[]) null);
			
			// Rebuild what needs to be rebuild
			if(type.equals(LabelsPanel.class.toString()))
			{
				TreeMap<String,TreeMap<String,Set<JEXEntry>>> thelabels = null;
				if(scope.equals(ALL_DATABASE))
				{
					thelabels = this.getTNVI().get(JEXData.LABEL);
				}
				else
				{
					thelabels = this.getFilteredTNVI().get(JEXData.LABEL);
				}
				this.setLabels(thelabels);
			}
			else if(type.equals(JEXDataPanel.class.toString()))
			{
				tnvi theobjects = null;
				if(scope.equals(ALL_DATABASE))
				{
					theobjects = this.getTNVI();
				}
				else
				{
					theobjects = this.getFilteredTNVI();
				}
				this.setObjects(theobjects);
			}
		}
		return;
	}
	
	/**
	 * Returns the scope of the element of type TYPE
	 * 
	 * @return string
	 */
	public String getScope(String type)
	{
		String scope = this.scopeSet.get(type);
		if(scope == null)
		{
			return FILTERED_DATABASE;
		}
		return scope;
	}
	
	// ---------------------------------------------
	// Experimental tree
	// ---------------------------------------------
	
	/**
	 * Set the experiment filter
	 * 
	 * @param experimentName
	 */
	public void setViewedExperiment(String viewedExperiment)
	{
		this.viewedExperiment = viewedExperiment;
		
		this.updateLabelsAndObjects();
		
		// Emit signal of viewed experiment change
		Logs.log("Send signal of viewed experiment change", 1, this);
		SSCenter.defaultCenter().emit(this, NAVIGATION, (Object[]) null);
	}
	
	/**
	 * Return the current experiment being viewed
	 * 
	 * @return experimentName
	 */
	public String getViewedExperiment()
	{
		return this.viewedExperiment;
	}
	
	/**
	 * Return the current arrayed viewed
	 * 
	 * @return arrayName
	 */
	public String getArrayViewed()
	{
		return this.viewedArray;
	}
	
	/**
	 * return the experiment tree
	 * 
	 * @return experimentTree
	 */
	public TreeMap<String,Experiment> getExperimentTree()
	{
		if(this.getCurrentDatabase() == null)
		{
			return null;
		}
		TreeMap<String,Experiment> experiments = this.getCurrentDatabase().getExperimentalTable();
		return experiments;
	}
	
	/**
	 * Return the current viewed hierarchy level
	 * 
	 * @return
	 */
	public HierarchyLevel getViewedHierarchyLevel()
	{
		// Get the viewed experiment
		String expViewed = this.getViewedExperiment();
		
		// if the experiment viewed is null return the whole database
		if(expViewed == null)
		{
			return new DatabaseLevel(this.getExperimentTree());
		}
		else
		{
			// Get the experiment object
			TreeMap<String,Experiment> expTree = this.getExperimentTree();
			Experiment exp = expTree.get(expViewed);
			
			// If it is null return the whole database
			if(exp == null)
			{
				return new DatabaseLevel(this.getExperimentTree());
			}
			else
			{
				return exp;
			}
		}
	}
	
	// ---------------------------------------------
	// Bookmarks managing
	// ---------------------------------------------
	
	/**
	 * Make a bookmark saving the current display / browsing settings
	 */
	public Bookmark makeBookMark(String bkName, String bkInfo)
	{
		Bookmark result = new Bookmark();
		
		return result;
	}
	
	// ---------------------------------------------
	// InfoPanels
	// ---------------------------------------------
	
	// Get the list of infopanel controllers
	public TreeMap<String,InfoPanelController> getInfoPanelControllersExp()
	{
		if(this.infoPanelControllersExp == null)
		{
			this.infoPanelControllersExp = new TreeMap<String,InfoPanelController>();
		}
		return this.infoPanelControllersExp;
	}
	
	// Set an infopanel controller with a key KEY
	public void setInfoPanelControllerExp(String key, InfoPanelController controller)
	{
		if(this.infoPanelControllersExp == null)
		{
			this.infoPanelControllersExp = new TreeMap<String,InfoPanelController>();
		}
		this.infoPanelControllersExp.put(key, controller);
		SSCenter.defaultCenter().emit(this, INFOPANELS_EXP, (Object[]) null);
	}
	
	// Get the list of infopanel controllers
	public TreeMap<String,InfoPanelController> getInfoPanelControllersArr()
	{
		if(this.infoPanelControllersArr == null)
		{
			this.infoPanelControllersArr = new TreeMap<String,InfoPanelController>();
		}
		return this.infoPanelControllersArr;
	}
	
	// Set an infopanel controller with a key KEY
	public void setInfoPanelControllerArr(String key, InfoPanelController controller)
	{
		if(this.infoPanelControllersArr == null)
		{
			this.infoPanelControllersArr = new TreeMap<String,InfoPanelController>();
		}
		this.infoPanelControllersArr.put(key, controller);
		SSCenter.defaultCenter().emit(this, INFOPANELS_ARR, (Object[]) null);
	}
	
	// ---------------------------------------------
	// Array view template
	// ---------------------------------------------
	
	/**
	 * Set flag to true to display data in the array
	 */
	public void setViewDataInArray(boolean displayDataInArray)
	{
		this.displayDataInArray = displayDataInArray;
		SSCenter.defaultCenter().emit(this, ARRAYDATADISPLAY, (Object[]) null);
	}
	
	/**
	 * Returns the flag controlling whether data should be viewed in the arrays
	 * 
	 * @return
	 */
	public boolean viewDataInArray()
	{
		return this.displayDataInArray;
	}
	
	/**
	 * Set the entry that is currently viewed This influences the viewer and the cell of the array that is currently highlighted
	 * 
	 * @param entry
	 */
	public void setViewedEntry(JEXEntry entry)
	{
		if(this.viewedEntry == entry)
		{
			return;
		}
		this.viewedEntry = entry;
		
		// Update viewed data information
		this.updateDimensionTable();
		
		// Emit signal of experiment tree change
		Logs.log("Send signal of viewed entry change", 1, this);
		SSCenter.defaultCenter().emit(this, VIEWEDENTRY, (Object[]) null);
		
		// // Set the database / repository info panel
		// if (this.getSelectedObject() == null) setInfoPanel("SelectedObject",
		// null);
		// else {
		// SelectedObjectInfoPanel infoPanel = new
		// SelectedObjectInfoPanel(this.getSelectedObject());
		// setInfoPanel("SelectedObject", infoPanel);
		// }
	}
	
	/**
	 * Return the entry that is currently viewed
	 * 
	 * @return entry
	 */
	public JEXEntry getViewedEntry()
	{
		return this.viewedEntry;
	}
	
	/**
	 * Set the valid label of entry ENTRY to VALID
	 * 
	 * @param entry
	 * @param valid
	 */
	public void setEntryValid(JEXEntry entry, boolean valid)
	{
		JEXLabel label = new JEXLabel(JEXEntry.VALID, "" + valid, "");
		// this.addDataToEntry(entry, label, true);
		JEXStatics.jexDBManager.saveDataInEntry(entry, label, true);
		
		// Emit signal of experiment tree change
		Logs.log("Send signal of entry validity change", 1, this);
		SSCenter.defaultCenter().emit(this, ENTRYVALID, (Object[]) null);
	}
	
	/**
	 * Return the validity label of entry ENTRY
	 * 
	 * @param entry
	 * @return
	 */
	public boolean getEntryValidity(JEXEntry entry)
	{
		JEXData data = this.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, JEXEntry.VALID), entry);
		if(!data.getTypeName().getType().matches(JEXData.LABEL))
		{
			return true;
		}
		
		boolean result = Boolean.parseBoolean(data.getFirstSingle().get(JEXDataSingle.VALUE));
		return result;
	}
	
	/**
	 * Set the viewed data map
	 * 
	 * @param map
	 */
	public void setViewedDatamap(DimensionMap map)
	{
		if(this.viewedData == map)
		{
			return;
		}
		this.viewedData = map;
		
		// Emit signal of experiment tree change
		Logs.log("Send signal of viewed datamap change", 1, this);
		SSCenter.defaultCenter().emit(this, DATAVIEWED, (Object[]) null);
	}
	
	/**
	 * Return the viewed data map
	 * 
	 * @return
	 */
	public DimensionMap getViewedDatamap()
	{
		return this.viewedData;
	}
	
	/**
	 * Set the dimension table of the data selected
	 * 
	 * @param dimTable
	 */
	public void setDataDimensionTable(DimTable dimTable)
	{
		if(this.dimTable == dimTable)
		{
			return;
		}
		this.dimTable = dimTable;
		
		// Emit signal of experiment tree change
		Logs.log("Send signal of data dimension table change", 1, this);
		SSCenter.defaultCenter().emit(this, DATADIMTABLE, (Object[]) null);
	}
	
	/**
	 * Return the dimension table of the data viewed
	 * 
	 * @return
	 */
	public DimTable getDataDimensionTable()
	{
		return this.dimTable;
	}
	
	// ---------------------------------------------
	// Statistics filtering
	// ---------------------------------------------
	
	/**
	 * Return the statistics TNVI of the database
	 * 
	 * @return fTNVI
	 */
	public TreeMap<Type,TreeMap<String,TreeMap<String,Set<JEXEntry>>>> getStatisticsTNVI()
	{
		// return this.getCurrentDatabase().getStatisticsTNVI();
		return this.getCurrentDatabase().getTNVI();
	}
	
	// /**
	// * Return the statistics ITN of the database
	// * @return fITN
	// */
	// public TreeMap<JEXEntry,TreeMap<String,TreeMap<String,String>>>
	// getStatisticsITNV(){
	// return this.getCurrentDatabase().getStatisticsITNV();
	// }
	
	/**
	 * Set the grouping list for the statistics panel
	 */
	public void setStatisticsGrouping(List<TypeName> statGroups)
	{
		this.statsGroups = statGroups;
		this.getCurrentDatabase().setStatisticsGrouping(statGroups);
		
		// Send a signal of change of browsed database
		Logs.log("Send signal of change of statistics grouping", 1, this);
		SSCenter.defaultCenter().emit(this, STATSGROUPS, (Object[]) null);
		SSCenter.defaultCenter().emit(this, STATSRESULTS, (Object[]) null);
	}
	
	/**
	 * Return the grouping list for the statistics
	 * 
	 * @return
	 */
	public List<TypeName> getStatisticsGrouping()
	{
		if(this.statsGroups == null)
		{
			this.statsGroups = new ArrayList<TypeName>(0);
		}
		return this.statsGroups;
	}
	
	/**
	 * Set the filterset of the databse
	 * 
	 * @param filterset
	 */
	public void setStatisticsFilterSet(FilterSet statsFilterSet)
	{
		this.statsFilterSet = statsFilterSet;
		this.getCurrentDatabase().setStatisticsFilterSet(statsFilterSet);
		
		// Emit signal of repository hashmap change
		Logs.log("Send signal of change of statistics filters", 1, this);
		SSCenter.defaultCenter().emit(this, STATSFILTERS, (Object[]) null);
		SSCenter.defaultCenter().emit(this, STATSRESULTS, (Object[]) null);
	}
	
	/**
	 * Return the statistics filter set
	 * 
	 * @return
	 */
	public FilterSet getStatisticsFilterSet()
	{
		if(this.statsFilterSet == null)
		{
			this.statsFilterSet = new FilterSet();
		}
		return this.statsFilterSet;
	}
	
	/**
	 * Return the grouping of entries for the statistics
	 * 
	 * @return
	 */
	public TreeMap<DimensionGroupMap,Set<JEXEntry>> getStatisticsGroupedEntries()
	{
		return this.getCurrentDatabase().getStatisticsGroupedEntries();
	}
	
	/**
	 * Set the selected object for statistics
	 * 
	 * @param object
	 */
	public void setSelectedStatisticsObject(TypeName object)
	{
		this.statsValueObject = object;
		
		// Send a signal of change of selected object
		Logs.log("Send signal of change of selected statistics object", 1, this);
		SSCenter.defaultCenter().emit(this, STATSVALUEOBJ, (Object[]) null);
	}
	
	/**
	 * Return the typename of the selectedobject for statistics
	 * 
	 * @return typename
	 */
	public TypeName getSelectedStatisticsObject()
	{
		return this.statsValueObject;
	}
	
	// ---------------------------------------------
	// Archiving and consolidating
	// ---------------------------------------------
	
	// /**
	// * Consolidate the entries in the entry set ENTRIES
	// */
	// public boolean consolidateEntries(Set<JEXEntry> entries)
	// {
	// if (entries == null) return false;
	// JEXLocalDatabase db = this.getCurrentDatabase();
	// if (db == null) return false;
	// db.consolidateEntries(entries);
	//
	// // Emit signal of experiment tree change
	// Logs.log("Send signal of experiment tree change", 1,
	// this);
	// SSCenter.defaultCenter().emit(this, EXPERIMENTREE_UPDATE,
	// (Object[])null);
	//
	// return true;
	// }
	
	/**
	 * Archive the experiment name EXPNAME into the database DB
	 * 
	 * @param expName
	 * @return
	 */
	public boolean archiveExperimentIntoDatabase(String expName, JEXDB db)
	{
		
		return false;
	}
	
	// ---------------------------------------------
	// Private functions....
	// ---------------------------------------------
	
	/**
	 * Update the current dimension table if necessary
	 */
	private void updateDimensionTable()
	{
		JEXEntry entry = this.getViewedEntry();
		TypeName object = this.getSelectedObject();
		if(entry == null || object == null)
		{
			return;
		}
		
		JEXData data = this.getDataOfTypeNameInEntry(object, entry);
		if(data == null)
		{
			return;
		}
		DimTable table = data.getDimTable();
		if(table == null)
		{
			table = new DimTable(data.getDataMap());
		}
		
		// make the default dim
		DimensionMap dim = new DimensionMap();
		List<String> dimNames = table.getDimensionNames();
		for (String dimName : dimNames)
		{
			Dim diemension = table.getDimWithName(dimName);
			String[] dimValues = diemension.valueArray();
			if(dimValues == null || dimValues.length == 0)
			{
				continue;
			}
			dim.put(dimName, dimValues[0]);
		}
		
		Logs.log("Setting the dimTable", 1, this);
		this.setDataDimensionTable(table);
		this.setViewedDatamap(dim);
	}
	
}
