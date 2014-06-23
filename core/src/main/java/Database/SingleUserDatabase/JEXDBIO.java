package Database.SingleUserDatabase;

import image.roi.ROIPlus;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import jex.statics.JEXDBFileManager;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import miscellaneous.StopWatch;
import miscellaneous.XMLUtility;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

import preferences.XMLPreferences_XElement;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataReader.MovieReader;
import Database.DataReader.TrackReader;
import Database.Definition.Type;
import Database.SingleUserDatabase.xml.ObjectFactory;
import Database.SingleUserDatabase.xml.XData;
import Database.SingleUserDatabase.xml.XDataSingle;
import Database.SingleUserDatabase.xml.XElement;
import Database.SingleUserDatabase.xml.XEntry;
import Database.SingleUserDatabase.xml.XEntrySet;

public class JEXDBIO {
	
	// Administrative statics
	public static String LOCAL_DATABASE_FILENAME = "JEX4Database.xml";
	
	// statics
	public static String VERSION = "2012-06-05";
	
	// ---------------------------------------------
	// Load / save
	// ---------------------------------------------
	
	/**
	 * Load the file as a Database
	 */
	public static JEXDB load(String xmlPath)
	{
		
		Logs.log("Loading the local database " + xmlPath, 0, null);
		
		// Load the xml into this db object
		JEXDB db = JEXDBIO.XEntrySetToDatabaseObject(xmlPath);
		
		if(db == null)
		{
			Logs.log("!!! Database loading failed !!!", 0, null);
		}
		return db;
	}
	
	/**
	 * Save the database in folder FOLDER Set consolidate flag to true to copy all raw data with it
	 * 
	 * @param folder
	 * @param consolidate
	 * @return
	 * @throws IOException
	 */
	public synchronized static boolean saveDB(JEXDB db)
	{
		
		// Get the saving folder
		File folder = new File(JEXWriter.getDatabaseFolder());
		Vector<Update> updates = null;
		
		// //////////////////////////////////////////////////////////////////////////////////////
		// Get the XML version of the database and any file updates that need to
		// be performed //
		// Then perform the file updates //
		// //////////////////////////////////////////////////////////////////////////////////////
		boolean savingSucceeded = false;
		
		// Clean FileManager Folder to start fresh.
		File fileManagerFolder = new File(JEXWriter.getDatabaseFolder() + File.separator + JEXDBFileManager.managerFolder);
		cleanDirectory(fileManagerFolder, new TreeSet<File>(), false);
		
		// Temporarily move changed files to the temp folder to avoid moving
		// files in
		// the wrong order and deleting something that we should be keeping.
		try
		{
			JEXStatics.statusBar.setStatusText("Creating the file manager for saving.");
			Logs.log("Creating the file manager for saving.", 0, JEXDBIO.class.getSimpleName());
			JEXStatics.fileManager = new JEXDBFileManager();
			JEXStatics.fileManager.initialize();
			JEXStatics.fileManager.startNewSession();
			JEXStatics.fileManager.session().setTransactionTimeout(12000);
			
			JEXStatics.statusBar.setStatusText("Gathering the updates to the database file structure.");
			Logs.log("Gathering the updates to the database file structure.", 0, JEXDBIO.class.getSimpleName());
			Pair<XEntrySet,Vector<Update>> dbUpdates = JEXDBIO.DatabaseObjectToXEntrySet(db);
			XEntrySet xml = dbUpdates.p1;
			updates = dbUpdates.p2;
			
			JEXStatics.statusBar.setStatusText("Archiving the current database xml file.");
			Logs.log("Archiving the current database xml file.", 0, JEXDBIO.class.getSimpleName());
			// Archive the old database
			String dbFileName = FileUtility.getNextName(folder.getPath(), LOCAL_DATABASE_FILENAME, "Archive");
			File dbFile = new File(folder.getPath() + File.separator + LOCAL_DATABASE_FILENAME);
			File archiveDBFile = new File(folder.getPath() + File.separator + dbFileName);
			if(JEXStatics.fileManager.session().fileExists(archiveDBFile))
			{
				JEXStatics.fileManager.session().deleteFile(archiveDBFile); // Make
				// room
				// to
				// copy
				// file
				// if
				// necessary,
				// but
				// should
				// never
				// be
				// needed.
			}
			if(dbFile.exists())
			{
				JEXStatics.fileManager.session().copyFile(dbFile, archiveDBFile);
			}
			
			JEXStatics.statusBar.setStatusText("Updating: copying changes to temp folder.");
			Logs.log("Updating: copying changes to temp folder.", 0, JEXDBIO.class.getSimpleName());
			// Perform all the updates gathered during compiling of the XML
			// database structure.
			for (Update update : updates)
			{
				update.startUpdate();
			}
			JEXStatics.statusBar.setStatusText("Updating: moving changes to database folders.");
			Logs.log("Updating: moving changes to database folders.", 0, JEXDBIO.class.getSimpleName());
			for (Update update : updates)
			{
				update.finishUpdate();
			}
			
			JEXStatics.statusBar.setStatusText("Updates successful. Writing new database xml.");
			Logs.log("Updates successful. Writing new database xml.", 0, JEXDBIO.class.getSimpleName());
			// ///////////////////////////////////////
			// Save the real XML version of the DB //
			// ///////////////////////////////////////
			String XMLDBString = XMLUtility.toHardXML(xml);
			String xmlPath = JEXWriter.getDatabaseFolder() + File.separator + LOCAL_DATABASE_FILENAME;
			JEXStatics.fileManager.saveText(xmlPath, XMLDBString, true);
			Logs.log("======================================================", 0, JEXDBIO.class.getSimpleName());
			Logs.log("Saved hard XML at location " + xmlPath, 0, JEXDBIO.class.getSimpleName());
			
			// Save the pretty version of the XML
			String XMLDBPrettyString = XMLUtility.toXML(xml);
			String prettypath = xmlPath.substring(0, xmlPath.length() - 4) + "_pretty.xml";
			JEXStatics.fileManager.saveText(prettypath, XMLDBPrettyString, true);
			Logs.log("Saved pretty XML at location " + prettypath, 0, JEXDBIO.class.getSimpleName());
			
			// Commit fileManager session
			Logs.log("Committing changes to database.", 0, JEXDBIO.class.getSimpleName());
			JEXStatics.fileManager.session().commit();
			Logs.log("Changes committed to database.", 0, JEXDBIO.class.getSimpleName());
			Logs.log("======================================================", 0, JEXDBIO.class.getSimpleName());
			savingSucceeded = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			try
			{
				JEXStatics.statusBar.setStatusText("Error occurred during save. Rolling back changes to database files made during change.");
				Logs.log("Error occurred during save. Rolling back changes to database files made during change.", 0, JEXDBIO.class.getSimpleName());
				JEXStatics.fileManager.session().rollback();
			}
			catch (NoTransactionAssociatedException e1)
			{
				e1.printStackTrace();
			}
		}
		finally
		{
			if(JEXStatics.fileManager != null)
			{
				JEXStatics.fileManager.shutdown();
			}
		}
		
		if(savingSucceeded && updates != null)
		{
			JEXStatics.statusBar.setStatusText("Finalizing update.");
			Logs.log("Finalizing update.", 0, JEXDBIO.class.getSimpleName());
			for (Update update : updates)
			{
				update.finalizeUpdate();
			}
			
			// Remake the tnvis because the dictionary values have been updated
			db.makeTNVIs();
			db.makeFiltTNVI();
			db.makeStatTNVI();
			db.makeExperimentTree();
		}
		
		if(savingSucceeded)
		{
			JEXStatics.statusBar.setStatusText("Database Saved");
			Logs.log("Updates Finalized. \nDatabase Saved.  " + DateFormat.getDateTimeInstance().format(new Date()), 0, JEXDBIO.class.getSimpleName());
		}
		return savingSucceeded;
		
	}
	
	public synchronized static void cleanDB(JEXDB db)
	{
		boolean saveSucceeded = saveDB(db); // Always save first so that all
		// files are where they are supposed
		// to be before we start looking for
		// stranded items.
		
		if(!saveSucceeded)
		{
			JEXStatics.statusBar.setStatusText("Saving must succeed before database can be cleaned of orphaned files.");
			Logs.log("Saving must succeed before database can be cleaned of orphaned files.", 0, JEXDBIO.class.getSimpleName());
			return;
		}
		
		JEXStatics.statusBar.setStatusText("Cleaning Database. Gathering list of files to delete...");
		Logs.log("Cleaning Database. Gathering list of files to delete...", 0, JEXDBIO.class.getSimpleName());
		
		TreeSet<File> keeperList = new TreeSet<File>();
		
		String databaseFolder = JEXWriter.getDatabaseFolder();
		// keeperList.add(new File(databaseFolder + File.separator +
		// JEXWriter.getTempFolderName()));
		// keeperList.add(new File(databaseFolder + File.separator +
		// JEXDBFileManager.managerFolder));
		File[] fileList = (new File(databaseFolder)).listFiles();
		for (File f : fileList)
		{
			if(!f.isDirectory())
			{
				keeperList.add(f);
			}
		}
		
		for (JEXEntry entry : db)
		{
			// String path = JEXWriter.getEntryFolder(entry, false, false);
			// File temp = new File(path);
			// keeperList.add(temp);
			// path = JEXWriter.getArrayFolder(entry, false, false);
			// temp = new File(path);
			// keeperList.add(temp);
			// path = JEXWriter.getExperimentFolder(entry, false, false);
			// temp = new File(path);
			// keeperList.add(temp);
			for (Entry<Type,TreeMap<String,JEXData>> e3 : entry.getDataList().entrySet())
			{
				for (Entry<String,JEXData> e4 : e3.getValue().entrySet())
				{
					JEXData data = e4.getValue();
					// String dataFolderPath = JEXWriter.getDataFolder(data,
					// false);
					// File dataFolderFile = new File(dataFolderPath);
					// keeperList.add(dataFolderFile);
					cleanDataFolder(data, keeperList);
				}
			}
		}
		cleanDirectory(new File(databaseFolder), keeperList, true);
	}
	
	private static void cleanDataFolder(JEXData data, TreeSet<File> keeperList)
	{
		File jxdFile = new File(JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath());
		keeperList.add(jxdFile);
		Type type = data.getDataObjectType();
		
		TreeMap<DimensionMap,String> paths = null;
		String path = null;
		// If there are references files, then add them to the keeperList
		if(type.matches(JEXData.IMAGE))
		{
			paths = ImageReader.readObjectToImagePathTable(data);
		}
		if(type.matches(JEXData.MOVIE))
		{
			path = MovieReader.readMovieObject(data);
		}
		if(type.matches(JEXData.FILE))
		{
			paths = FileReader.readObjectToFilePathTable(data);
		}
		if(type.matches(JEXData.TRACK))
		{
			path = TrackReader.readTrackObject(data);
		}
		
		if(path != null)
		{
			keeperList.add(new File(path));
		}
		if(paths != null)
		{
			for (Entry<DimensionMap,String> e : paths.entrySet())
			{
				keeperList.add(new File(e.getValue()));
			}
		}
	}
	
	/**
	 * Deletes all files and subdirectories under dir. Returns true if all deletions were successful. If a deletion fails, the method stops attempting to delete and returns false.
	 * 
	 * @param dir
	 * @return
	 * @throws InterruptedException
	 * @throws FileNotExistsException
	 * @throws InsufficientPermissionOnFileException
	 * @throws NoTransactionAssociatedException
	 * @throws LockingFailedException
	 * @throws FileUnderUseException
	 * @throws DirectoryNotEmptyException
	 */
	private static void cleanDirectory(File f, TreeSet<File> keeperList, boolean isDatabaseFolder)
	{
		if(f.exists() && f.isDirectory() && !keeperList.contains(f))
		{
			File[] children = f.listFiles();
			for (File f2 : children)
			{
				cleanDirectory(f2, keeperList, false);
			}
		}
		
		// The file is then either a file or could be an empty directory and an
		// attempt to delete it can now be made.
		// As long as it isn't the database folder
		if(!isDatabaseFolder)
		{
			if(!keeperList.contains(f))
			{
				boolean success = f.delete();
				if(success)
				{
					Logs.log("Deleted: " + f.getPath(), 0, JEXDBIO.class.getSimpleName());
				}
			}
		}
	}
	
	// ----------------------------
	// XEntrySet to JEXLocalDatabaseCore
	// ----------------------------
	
	public static JEXDB XEntrySetToDatabaseObject(String xmlPath)
	{
		JEXDB ret = new JEXDB();
		
		// Generate the datamap
		TreeMap<JEXEntry,TreeMap<String,TreeMap<String,JEXData>>> datamap = new TreeMap<JEXEntry,TreeMap<String,TreeMap<String,JEXData>>>();
		
		// Make the database path
		File xmlFile = new File(xmlPath);
		if(!xmlFile.exists())
		{
			ret.setMaxID(0);
			ret.makeTNVIs();
			ret.makeFiltTNVI();
			ret.makeStatTNVI();
			ret.makeExperimentTree();
			return ret;
		}
		
		// Parse the xml
		Logs.log("Parsing the xml string " + xmlPath, 0, JEXDBIO.class.getSimpleName());
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		SAXBuilder sb = new SAXBuilder();
		sb.setFactory(new ObjectFactory());
		Document resturnDoc = null;
		try
		{
			resturnDoc = sb.build(new File(xmlPath));
		}
		catch (JDOMException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		stopwatch.stop();
		Logs.log("Factory TIME: " + stopwatch.toString(), 0, JEXDBIO.class.getSimpleName());
		Logs.log("======================================================", 0, JEXDBIO.class.getSimpleName());
		XEntrySet xmlElement = (XEntrySet) resturnDoc.getRootElement();
		
		stopwatch = new StopWatch();
		stopwatch.start();
		
		// set the entry set of this class
		List<XElement> children = xmlElement.getXElements();
		int maxID = 0;
		boolean resetIDs = false;
		
		for (XElement elem : children)
		{
			XEntry xentry = (XEntry) elem;
			JEXEntry entry = JEXDBIO.XEntryToDatabaseObject(xentry);
			entry.setParent(ret);
			
			// Check that the entry is good
			String eid = entry.getEntryID();
			if(eid == null || eid.equals("") || resetIDs)
			{
				
				// If this is the first entry that doesn't have an ID then
				// renumerate
				// all of the previous entries
				if(!resetIDs)
				{
					int index = 0;
					for (JEXEntry e : datamap.keySet())
					{
						e.setEntryID("" + index);
						index++;
					}
					maxID = index;
				}
				
				// Switch the resetID flag to true and override the id setting
				resetIDs = true;
				entry.setEntryID("" + maxID);
				maxID++;
			}
			
			// Set the max id as the maximum current ID value
			int thisID = Integer.parseInt(entry.getEntryID());
			maxID = Math.max(maxID, thisID) + 1;
			
			// add the data list
			ret.getEntries().add(entry);
			
			// Status bar
			int percentage = (int) (100 * ((double) maxID / (double) children.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		ret.setMaxID(maxID);
		ret.makeTNVIs();
		ret.makeFiltTNVI();
		ret.makeStatTNVI();
		ret.makeExperimentTree();
		return ret;
	}
	
	// ----------------------------
	// XEntry to JEXEntry
	// ----------------------------
	
	/**
	 * Make a JEXEntry out of a XEntry
	 */
	public static JEXEntry XEntryToDatabaseObject(XEntry xe)
	{
		JEXEntry result = new JEXEntry();
		
		String ID = xe.getAtt(JEXEntry.EID);
		ID = (ID == null) ? "" : ID;
		String expName = null;
		String expInfo = null;
		String trayName = null;
		String trayX = null;
		String trayY = null;
		String date = xe.getAtt(JEXEntry.DATE);
		String modif = xe.getAtt(JEXEntry.MODIFDATE);
		String author = xe.getAtt(JEXEntry.AUTHOR);
		
		// Get the child elements
		List<XElement> children = xe.getXElements();
		
		// Loop through to make the JEXEntry Hierarchy
		for (XElement elem : children)
		{
			XData xdata = (XData) elem;
			Type xDataType = new Type(xdata.getTypeField());
			// if it's a hierarchy treat it differently
			if(xDataType.matches(JEXData.HIERARCHY))
			{
				String level = xdata.getAtt(JEXData.NAME);
				XDataSingle ds = xdata.getFirstDataElement();
				
				if(level.equals(JEXEntry.EXPERIMENT))
				{
					expName = ds.getAtt("Value");
					expInfo = xdata.getAtt("Info");
				}
				if(level.equals(JEXEntry.TRAY))
				{
					trayName = ds.getAtt("Value");
				}
				if(level.equals("Row"))
				{
					trayX = ds.getAtt("Value");
				}
				if(level.equals("Column"))
				{
					trayY = ds.getAtt("Value");
				}
			}
		}
		
		// set the meta data on the entry
		if(trayName == null)
		{
			result.setEntryExperiment(expName);
		}
		else
		{
			result.setEntryExperiment(expName + " - " + trayName);
		}
		result.loadTimeExperimentName = expName;
		result.setEntryExperimentInfo(expInfo);
		// result.setEntryTrayName(trayName); // No longer using Trays
		result.loadTimeTrayName = trayName; // Use this to keep track of old
		// entries that were associated with
		// a tray
		result.setTrayX(Integer.parseInt(trayX));
		result.setTrayY(Integer.parseInt(trayY));
		result.setDate(date);
		result.setModificationDate(modif);
		result.setEntryID(ID);
		result.setAuthor(author);
		
		// Loop though to add the common data
		for (XElement elem : children)
		{
			XData xdata = (XData) elem;
			
			// if it's a normal object
			if(!xdata.getTypeField().equals(JEXData.HIERARCHY))
			{
				JEXData data = JEXDBIO.XDataToDatabaseObject(xdata, result);
				if(data == null)
				{
					continue;
				}
				result.addData(data, true);
			}
		}
		
		// Update modification date if necessary
		if(modif == null)
		{
			modif = result.findModificationDate();
		}
		
		// Check that the valid label exists
		TreeMap<String,JEXData> labels = result.getDataList().get(JEXData.LABEL);
		JEXData vLabel = null;
		if(labels == null)
		{
			vLabel = new JEXLabel(JEXEntry.VALID, "true", "");
			result.addData(vLabel, true);
		}
		else
		{
			vLabel = labels.get(JEXEntry.VALID);
			if(vLabel == null)
			{
				vLabel = new JEXLabel(JEXEntry.VALID, "true", "");
				result.addData(vLabel, true);
			}
		}
		
		return result;
	}
	
	// ----------------------------
	// XData to JEXData
	// ----------------------------
	
	/**
	 * Make a JEXData out of a XData
	 * 
	 * @param xData
	 * @param entry
	 * @return
	 */
	public static JEXData XDataToDatabaseObject(XData xData, JEXEntry entry)
	{
		JEXData result = null;
		
		String typeS = xData.getAtt(JEXData.TYPE);
		Type type = null;
		if(typeS != null)
		{
			type = new Type(typeS);
		}
		if(type == null)
		{
			return null;
		}
		String dataName = xData.getAtt(JEXData.NAME);
		String dataInfo = xData.getAtt(JEXData.INFO);
		String dataDate = xData.getAtt(JEXData.DATE);
		String dataModif = xData.getAtt(JEXData.MDATE);
		String author = xData.getAtt(JEXData.AUTHOR);
		String dataDims = xData.getAtt(JEXData.DIMS);
		String objPath = xData.getAtt(JEXData.DETACHED_RELATIVEPATH);
		String dictKey = xData.getAtt(JEXData.DICTKEY);
		dataName = (dataName == null) ? "No" : dataName;
		dataInfo = (dataInfo == null) ? "No" : dataInfo;
		
		result = new JEXData(type, dataName, dataInfo);
		result.setParent(entry);
		
		result.setDataObjectDate(dataDate);
		result.setDataObjectModifDate(dataModif);
		result.setDataObjectInfo(dataInfo);
		result.setDataObjectName(dataName);
		result.setAuthor(author);
		
		if(dictKey != null && !dictKey.equals(""))
		{
			// Else the next request for the dictionaryvalue witll load the
			// object and set it
			result.setDictionaryValue(dictKey);
		}
		if(objPath == null || objPath.equals(""))
		{
			// IF WE HAVE AN OLD XML TYP JEXDATA THAT DOESN'T HAVE A DETACHED
			// PATH
			// LOAD THE XDATASINGLES
			result.setDetachedRelativePath(null);
			result.setDimTable(new DimTable(dataDims));
			
			List<XDataSingle> datas = xData.getSingleDataElements();
			for (XDataSingle ds : datas)
			{
				JEXDataSingle dataSingle = JEXDBIO.XDataSingleToDatabaseObject(ds, result);
				if(dataSingle == null)
				{
					continue;
				}
				DimensionMap map = dataSingle.getDimensionMap();
				result.addData(map, dataSingle);
			}
			
			JEXDataSingle ds = result.getFirstSingle();
			if(ds == null)
			{
				Logs.log("Error loading JEXData - " + result.getTypeName().toString(), 0, JEXDBIO.class.getSimpleName());
				return null;
			}
			
			// / Set the dict key for building the tnvi
			result.setDictionaryValue(ds.toString());
			
			// try to overwrite with dictKey (won't work if null)
			result.setDictionaryValue(dictKey);
		}
		else
		{
			if(objPath.endsWith("xml")) // XML VERSIONS OF JEXDATA WERE SAVED IN
			// THE ENTRY FOLDER NOT THE JEXDATA
			// FOLDER
			{
				// Correct the objPath to be a relative database path rather
				// than a filename or full path
				result.setDetachedRelativePath(JEXWriter.getOldEntryFolder(entry, true) + File.separator + FileUtility.getFileNameWithExtension(objPath));
				result.setDimTable(new DimTable(dataDims)); // DIM TABLE WAS
				// SAVED IN DATABASE
				// XML
			}
			else
			// then the objPath should already be relative path and can be used
			// directly
			{
				String objPathOSCompatible = FileUtility.makeOSCompatible(objPath);
				result.setDetachedRelativePath(objPathOSCompatible);
				// DIM TABLE IS CONTAINED IN CSV/ARFF SO WE DONT WANT TO LOAD IT
				// YET
			}
		}
		
		return result;
	}
	
	// ----------------------------
	// XDataSingle to JEXDataSingle
	// ----------------------------
	
	/**
	 * Make a JEXDataSingle out of a XDataSingle
	 * 
	 * @param parent
	 * @param dimArray
	 * @return
	 */
	public static JEXDataSingle XDataSingleToDatabaseObject(XDataSingle xds, JEXData parent)
	{
		// ////////////////
		// / This is only called for old XML objects that were either stored in
		// the database
		// / xml or in dettached xml files
		// / For these singles, the referenced files were always save in the
		// ENTRY FOLDER
		// ////////////////
		
		JEXDataSingle result = new JEXDataSingle();
		
		// get the type field
		String typeS = xds.getAtt(JEXData.TYPE);
		Type type = null;
		if(typeS != null)
		{
			type = new Type(typeS);
		}
		if(type == null)
		{
			return null;
		}
		
		
		// Make the dimensionmap
		DimensionMap dimMap = new DimensionMap();
		for (String dimStr : parent.getDimTable().getDimensionNames())
		{
			String dimValueStr = xds.getAtt(dimStr);
			if(dimValueStr == null)
			{
				continue;
			}
			dimMap.put(dimStr, dimValueStr);
		}
		
		if(type.equals(JEXData.IMAGE) || type.equals(JEXData.FILE) || type.equals(JEXData.MOVIE) || type.equals(JEXData.TRACK) || type.equals(JEXData.SOUND))
		{
			String test = xds.getAtt("Path");
			if(test != null) // then it is from a very old database. Just making
			// an attempt here. Not sure if it will work.
			{
				result.put(JEXDataSingle.RELATIVEPATH, JEXWriter.getOldEntryFolder(parent.getParent(), true) + File.separator + (new File(test)).getName());
			}
			else
			// then it is an old xml dettached file instead of a new .jxd file
			// and any referenced files will be located in the entry folder
			{
				// DOING THINGS THIS WAY CONVERTS STUFF PREVIOUSLY SAVED AS A
				// FILENAME OR FULL PATH TO A DATABASE RELATIVE PATH
				// BUT ALSO WORKS WHEN IT IS ALREADY A RELATIVE PATH
				String fileName = FileUtility.getFileNameWithExtension(xds.getAtt(JEXDataSingle.RELATIVEPATH));
				result.put(JEXDataSingle.RELATIVEPATH, JEXWriter.getOldEntryFolder(parent.getParent(), true) + File.separator + fileName);
			}
		}
		else if(type.equals(JEXData.FUNCTION_OLD))
		{
			// Make the special dimensionmap
			dimMap = new DimensionMap();
			
			List<String> attNames = xds.getAttNames();
			
			for (String attName : attNames)
			{
				if(attName.equals(cruncher.JEXFunction.OLD_DIMKEY))
				{
					String attValue = xds.getAtt(attName);
					dimMap.put(cruncher.JEXFunction.OLD_DIMKEY, attValue);
				}
				else
				{
					String attValue = xds.getAtt(attName);
					result.put(attName, attValue);
				}
			}
		}
		else if(type.equals(JEXData.ROI))
		{
			String roitype = xds.getAtt(JEXDataSingle.ROITYPE);
			String points = xds.getAtt(JEXDataSingle.POINTLIST);
			String pattern = xds.getAtt(JEXDataSingle.PATTERN);
			
			if(roitype.equals("JRectangle"))
			{
				roitype = "" + ROIPlus.ROI_RECT;
			}
			else if(roitype.equals("JPolygon"))
			{
				roitype = "" + ROIPlus.ROI_POINT;
			}
			else if(roitype.equals("JPolygonLine"))
			{
				roitype = "" + ROIPlus.ROI_POLYGON;
			}
			
			result.put(JEXDataSingle.ROITYPE, roitype);
			result.put(JEXDataSingle.POINTLIST, points);
			result.put(JEXDataSingle.PATTERN, pattern);
		}
		else if(type.equals(JEXData.VALUE))
		{
			String value = xds.getAtt(JEXDataSingle.VALUE);
			String unit = xds.getAtt(JEXDataSingle.UNIT);
			result.put(JEXDataSingle.VALUE, value);
			result.put(JEXDataSingle.UNIT, unit);
		}
		else if(type.equals(JEXData.LABEL))
		{
			String value = xds.getAtt(JEXDataSingle.VALUE);
			String unit = xds.getAtt(JEXDataSingle.UNIT);
			result.put(JEXDataSingle.VALUE, value);
			result.put(JEXDataSingle.UNIT, unit);
		}
		else
		{
			result = null;
		}
		
		if(result != null)
		{
			result.setDimensionMap(dimMap);
		}
		return result;
	}
	
	// ----------------------------
	// JEXEntrySet to XEntrySet
	// ----------------------------
	
	/**
	 * Return an XEntrySet from a JEXDatabase
	 */
	public static Pair<XEntrySet,Vector<Update>> DatabaseObjectToXEntrySet(JEXDB db)
	{
		XEntrySet result = new XEntrySet();
		Vector<Update> updates = new Vector<Update>();
		for (JEXEntry entry : db)
		{
			try
			{
				Pair<XEntry,Vector<Update>> entryUpdates = JEXDBIO.DatabaseObjectToXEntry(entry);
				XEntry xentry = entryUpdates.p1;
				updates.addAll(entryUpdates.p2);
				result.addEntry(xentry);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Logs.log("Error in saving entry number " + entry.getEntryID() + ": " + entry.getEntryExperiment() + " - x" + entry.getTrayX() + " - y" + entry.getTrayY(), 0, JEXDBIO.class.getSimpleName());
				JEXStatics.statusBar.setStatusText("Error in saving entry number " + entry.getEntryID());
			}
		}
		
		return new Pair<XEntrySet,Vector<Update>>(result, updates);
	}
	
	// ----------------------------
	// JEXEntry to XEntry
	// ----------------------------
	
	/**
	 * Make an Xentry out of the classy entry
	 * 
	 * @param folder
	 * @param localPath
	 * @param consolidate
	 * @return
	 */
	public static Pair<XEntry,Vector<Update>> DatabaseObjectToXEntry(JEXEntry entry)
	{
		XEntry result = new XEntry();
		Vector<Update> updates = new Vector<Update>();
		
		boolean expOrTrayNameChanged = didExperimentOrTrayNameChange(entry);
		if(expOrTrayNameChanged)
		{
			updates.add(new JEXEntryUpdate(entry));
		}
		
		// Make the meta bud
		result.addMeta(JEXEntry.EID, entry.getEntryID());
		result.addMeta(JEXEntry.DATE, entry.getDate());
		result.addMeta(JEXEntry.MODIFDATE, entry.getModificationDate());
		result.addMeta(JEXEntry.AUTHOR, entry.getAuthor());
		
		// Add the hierarchy elements to the data object
		XData expGroup = new XData(JEXData.HIERARCHY);
		expGroup.addMeta(JEXData.NAME, "Experiment");
		expGroup.addMeta(JEXData.TYPE, JEXData.HIERARCHY.toString());
		expGroup.addMeta(JEXData.INFO, entry.getEntryExperimentInfo());
		expGroup.addMeta(JEXData.DATE, entry.getDate());
		expGroup.addMeta(JEXData.DIMS, "");
		XDataSingle expSingle = new XDataSingle(JEXData.VALUE);
		expSingle.addMeta(JEXDataSingle.VALUE, entry.getEntryExperiment());
		expSingle.addMeta(JEXDataSingle.UNIT, "");
		expGroup.addDataSingle(expSingle);
		result.addData(expGroup);
		
		// XData trayGroup = new XData(JEXData.HIERARCHY);
		// trayGroup.addMeta(JEXData.NAME, "Tray");
		// trayGroup.addMeta(JEXData.TYPE, JEXData.HIERARCHY);
		// trayGroup.addMeta(JEXData.INFO, "");
		// trayGroup.addMeta(JEXData.DATE, entry.getDate());
		// trayGroup.addMeta(JEXData.DIMS,"");
		// XDataSingle traySingle = new XDataSingle(JEXData.VALUE);
		// traySingle.addMeta(JEXDataSingle.VALUE, entry.getEntryTrayName());
		// traySingle.addMeta(JEXDataSingle.UNIT, "");
		// trayGroup.addDataSingle(traySingle);
		// result.addData(trayGroup);
		
		XData rowGroup = new XData(JEXData.HIERARCHY);
		rowGroup.addMeta(JEXData.NAME, "Row");
		rowGroup.addMeta(JEXData.TYPE, JEXData.HIERARCHY.toString());
		rowGroup.addMeta(JEXData.INFO, "");
		rowGroup.addMeta(JEXData.DATE, entry.getDate());
		rowGroup.addMeta(JEXData.DIMS, "");
		XDataSingle rowSingle = new XDataSingle(JEXData.VALUE);
		rowSingle.addMeta(JEXDataSingle.VALUE, "" + entry.getTrayX());
		rowSingle.addMeta(JEXDataSingle.UNIT, "");
		rowGroup.addDataSingle(rowSingle);
		result.addData(rowGroup);
		
		XData colGroup = new XData(JEXData.HIERARCHY);
		colGroup.addMeta(JEXData.NAME, "Column");
		colGroup.addMeta(JEXData.TYPE, JEXData.HIERARCHY.toString());
		colGroup.addMeta(JEXData.INFO, "");
		colGroup.addMeta(JEXData.DATE, entry.getDate());
		colGroup.addMeta(JEXData.DIMS, "");
		XDataSingle colSingle = new XDataSingle(JEXData.VALUE);
		colSingle.addMeta(JEXDataSingle.VALUE, "" + entry.getTrayY());
		colSingle.addMeta(JEXDataSingle.UNIT, "");
		colGroup.addDataSingle(colSingle);
		result.addData(colGroup);
		
		// Make the data objects
		TreeMap<Type,TreeMap<String,JEXData>> datas = entry.getDataList();
		for (Type type : datas.keySet())
		{
			if(type.matches(JEXData.HIERARCHY))
			{
				continue;
			}
			
			TreeMap<String,JEXData> dl = datas.get(type);
			for (String name : dl.keySet())
			{
				JEXData data = dl.get(name);
				if(data.getParent() == null)
				{
					data.setParent(entry);
				}
				
				if(expOrTrayNameChanged)
				{
					// LOAD THE DATA TO MARK IT FOR UPDATING TO NEW EXPERIMENT
					// OR TRAY OR DATA FOLDERS IF IT ISNT ALREADY
					// THIS OCCURS IN JEXMLFactory.DatabaseObjectToXData
					if(!data.isLoaded())
					{
						data.getDataMap();
					}
				}
				if(!data.getDataObjectType().equals(JEXData.HIERARCHY))
				{
					Pair<XData,Vector<Update>> dataUpdates = JEXDBIO.DatabaseObjectToXData(data);
					XData xdata = dataUpdates.p1;
					updates.addAll(dataUpdates.p2);
					result.addData(xdata);
				}
			}
		}
		
		return new Pair<XEntry,Vector<Update>>(result, updates);
	}
	
	private static boolean didExperimentOrTrayNameChange(JEXEntry entry)
	{
		if(entry.loadTimeExperimentName == null || !entry.getEntryExperiment().equals(entry.loadTimeExperimentName))
		{
			return true;
		}
		if(entry.loadTimeTrayName != null) // This was an old database that had
		// trays that needs to be updated
		{
			return true;
		}
		return false;
	}
	
	// ----------------------------
	// JEXData to XData
	// ----------------------------
	
	/**
	 * Return an XML version of the dataobject
	 */
	public static Pair<XData,Vector<Update>> DatabaseObjectToXData(JEXData data)
	{
		
		XData xData = new XData(data.getTypeName().getType());
		Vector<Update> updates = new Vector<Update>();
		
		// Make the meta bud
		xData.addMetaWithCategory(JEXData.NAME, "Adm", data.getDataObjectName());
		xData.addMetaWithCategory(JEXData.TYPE, "Adm", data.getDataObjectType().toString());
		xData.addMetaWithCategory(JEXData.INFO, "Adm", data.getDataObjectInfo());
		xData.addMetaWithCategory(JEXData.DATE, "Adm", data.getDataObjectDate());
		xData.addMetaWithCategory(JEXData.MDATE, "Adm", data.getDataObjectModifDate());
		xData.addMetaWithCategory(JEXData.AUTHOR, "Adm", data.getAuthor());
		xData.addMetaWithCategory(JEXData.DICTKEY, "Adm", data.getDictionaryValue());
		
		// Make the collection bud with the data objects
		// If the object is not loaded, no need to load it, keep it unchanged
		if(!data.isLoaded())
		{
			xData.addMetaWithCategory(JEXData.DETACHED_RELATIVEPATH, "Adm", data.getDetachedRelativePath());
		}
		else
		{
			JEXDataUpdate update = new JEXDataUpdate(data);
			updates.add(update);
			// Fill the new xml with where the data will be saved if all goes to
			// plan with the file transactions (Don't worry, if something goes
			// wrong, everything goes back to the way it was).
			xData.addMetaWithCategory(JEXData.DETACHED_RELATIVEPATH, "Adm", JEXWriter.getDataFolder(data, true) + File.separator + JEXDataIO.createJXDFileName(data));
		}
		
		return new Pair<XData,Vector<Update>>(xData, updates);
	}
	
	public static XMLPreferences_XElement getViewingXMLElement(JEXData data)
	{
		
		XMLPreferences_XElement result = new XMLPreferences_XElement();
		
		// Make the meta bud
		result.setAtt(JEXData.NAME, data.getDataObjectName(), JEXData.ADMIN);
		result.setAtt(JEXData.TYPE, data.getDataObjectType().toString(), JEXData.ADMIN);
		result.setAtt(JEXData.INFO, data.getDataObjectInfo(), JEXData.ADMIN);
		result.setAtt(JEXData.DATE, data.getDataObjectDate(), JEXData.ADMIN);
		result.setAtt(JEXData.MDATE, data.getDataObjectModifDate(), JEXData.ADMIN);
		result.setAtt(JEXData.AUTHOR, data.getAuthor(), JEXData.ADMIN);
		
		TreeMap<DimensionMap,JEXDataSingle> datas = data.getDataMap();
		for (DimensionMap map : datas.keySet())
		{
			JEXDataSingle ds = datas.get(map);
			ds.setDimensionMap(map);
			XMLPreferences_XElement datasingle = JEXDBIO.getViewingXMLElement(ds, data.getDataObjectType().toString());
			result.addXElement(datasingle);
		}
		
		return result;
	}
	
	public static XMLPreferences_XElement getViewingXMLElement(JEXDataSingle ds, String type)
	{
		XMLPreferences_XElement result = new XMLPreferences_XElement();
		
		// Make the meta bud
		Set<String> keys = ds.getDimensionMap().keySet();
		for (String key : keys)
		{
			String index = ds.getDimensionMap().get(key);
			result.setAtt(key, index);
		}
		
		for (String key : ds.getDataMap().keySet())
		{
			String value = ds.getDataMap().get(key);
			
			if(value == null)
			{
				// try to solve the problem
				if(key.equals(JEXDataSingle.UNIT))
				{
					result.setAtt(key, "");
				}
				else
				{
					Logs.log("Problem with saving data object for key = " + key.equals(JEXDataSingle.UNIT), 1, null);
				}
				continue;
			}
			
			result.setAtt(key, value);
		}
		
		return result;
	}
	
}
