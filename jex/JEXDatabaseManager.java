package jex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXDB;

public class JEXDatabaseManager {
	
	public static String TEMP_FOLDER_PATH = "temp";
	public static String CORE_TEMP_NAME = "JEXData";
	
	public JEXDatabaseManager()
	{   
		
	}
	
	// ---------------------------------------------
	// Data Editing
	// ---------------------------------------------
	
	/**
	 * Add data object to entry
	 * 
	 * @param entry
	 * @param data
	 * @return boolean
	 */
	public synchronized boolean saveDataInEntry(JEXEntry entry, JEXData data, boolean overwrite)
	{
		// Get the current database, return null if no database is open
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		if(db == null)
		{
			return false;
		}
		
		boolean result = db.addData(entry, data, overwrite);
		if(result)
		{
			Logs.log("Data added to database successfully", 1, this);
			JEXStatics.statusBar.setStatusText("Data added successfully");
		}
		else
		{
			Logs.log("Data not added to database", 1, this);
			JEXStatics.statusBar.setStatusText("Data not added");
		}
		
		// -------------------------
		// Update the display
		this.updateObjectsView();
		
		return result;
	}
	
	/**
	 * Add a single data object to a whole list of database entries
	 * 
	 * @param dataArray
	 * @return boolean
	 */
	public synchronized boolean saveDataInEntries(TreeMap<JEXEntry,JEXData> dataArray)
	{
		// Get the current database, return null if no database is open
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		if(db == null)
		{
			return false;
		}
		
		HashMap<JEXEntry,Set<JEXData>> dataArray2 = new HashMap<JEXEntry,Set<JEXData>>();
		for (JEXEntry entry : dataArray.keySet())
		{
			JEXData data = dataArray.get(entry);
			Set<JEXData> datas = new HashSet<JEXData>();
			datas.add(data);
			dataArray2.put(entry, datas);
		}
		boolean result = db.addDatas(dataArray2, true);
		if(result)
		{
			Logs.log("Data added to database successfully", 1, this);
			JEXStatics.statusBar.setStatusText("Data added successfully");
		}
		else
		{
			Logs.log("Data not added to database", 1, this);
			JEXStatics.statusBar.setStatusText("Data not added");
		}
		
		// -------------------------
		// Update the display
		this.updateObjectsView();
		
		return result;
	}
	
	/**
	 * Add a list of dataobjects to a list of database entries
	 * 
	 * @param dataArray
	 * @return boolean
	 */
	public synchronized boolean saveDataListInEntries(TreeMap<JEXEntry,Set<JEXData>> dataArray, boolean overwrite)
	{
		// Get the current database, return null if no database is open
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		if(db == null)
		{
			return false;
		}
		
		boolean result = db.addDatas(dataArray, overwrite);
		if(result)
		{
			Logs.log("Data added to database successfully", 1, this);
			JEXStatics.statusBar.setStatusText("Data added successfully");
		}
		else
		{
			Logs.log("Data not added to database", 1, this);
			JEXStatics.statusBar.setStatusText("Data not added");
		}
		
		// -------------------------
		// Update the display
		this.updateObjectsView();
		
		return result;
	}
	
	public synchronized void addEntries(String expName, int width, int height, String date, String author, String info)
	{
		// Get the current database, return null if no database is open
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		if(db == null)
		{
			Logs.log("Couldn't add the array of entries because there is no DB selected.", this);
			return;
		}
		db.addEntries(expName, width, height, date, author, info);
		
		// -------------------------
		// Update the display
		this.updateDatabaseView();
	}
	
	/**
	 * Add data object to entry
	 * 
	 * @param entry
	 * @param data
	 * @return boolean
	 */
	public synchronized boolean removeDataFromEntry(JEXEntry entry, JEXData data)
	{
		Set<JEXData> datas = new HashSet<JEXData>();
		datas.add(data);
		
		TreeMap<JEXEntry,Set<JEXData>> dataArray = new TreeMap<JEXEntry,Set<JEXData>>();
		dataArray.put(entry, datas);
		
		return this.removeDataListFromEntry(dataArray);
	}
	
	/**
	 * Add data object to entry
	 * 
	 * @param entry
	 * @param data
	 * @return boolean
	 */
	public synchronized boolean removeDataListFromEntry(TreeMap<JEXEntry,Set<JEXData>> dataArray)
	{
		// Get the current database, return null if no database is open
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		if(db == null)
		{
			return false;
		}
		
		boolean result = db.removeObjectArray(dataArray);
		if(result)
		{
			Logs.log("Data removed from database successfully", 1, this);
			JEXStatics.statusBar.setStatusText("Removed data");
		}
		else
		{
			Logs.log("Data not removed from database", 1, this);
			JEXStatics.statusBar.setStatusText("Data not removed");
		}
		
		// -------------------------
		// Update the display
		this.updateObjectsView();
		
		return true;
	}
	
	/**
	 * Remove the entries
	 * 
	 * @param entriesToRemove
	 */
	public synchronized void removeEntries(Set<JEXEntry> entriesToRemove)
	{
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		db.removeEntries(entriesToRemove);
		JEXStatics.statusBar.setStatusText("Removed entries");
		
		// Remove the entries from the selection, so that they don't stay in
		// memory
		JEXStatics.jexManager.removeEntriesFromSelection(entriesToRemove);
		
		// -------------------------
		// Update the display
		this.updateDatabaseView();
	}
	
	/**
	 * Remove the entries
	 * 
	 * @param entriesToRemove
	 */
	public synchronized void editHeirarchyForEntries(TreeSet<JEXEntry> entries, String experiment, String info, String date)
	{
		JEXDB db = JEXStatics.jexManager.getCurrentDatabase();
		db.editHeirarchyForEntries(entries, experiment, info, date);
		JEXStatics.statusBar.setStatusText("Edited entries");
		
		// -------------------------
		// Update the display
		this.updateDatabaseView();
	}
	
	/**
	 * Return a unique object name for objects of type TYPE with a base name BASENAME for all entries amongst ENTRIES
	 * 
	 * First the basename will be tested, if none of the entries contains an object of that name and type it is returned
	 * 
	 * If the baseName exists, a new name will be constructed as follows: "basename #" the method returns the new name with the lowest index
	 * 
	 * @param entries
	 * @param type
	 * @param baseName
	 * @return base name
	 */
	public synchronized String getUniqueObjectName(Set<JEXEntry> entries, Type type, String baseName)
	{
		String result = baseName;
		int index = 1;
		boolean exists = true;
		
		while (exists)
		{
			// set the exists to false and loop through the entries
			exists = false;
			
			// if one of the entries has a data of type name TYPE,RESULT set
			// exists to true
			for (JEXEntry entry : entries)
			{
				TypeName newTN = new TypeName(type, result);
				JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(newTN, entry);
				if(data != null)
				{
					exists = true;
				}
			}
			
			// if exists is true then generate a new name and repeat the process
			if(exists)
			{
				index++;
				result = baseName + " " + index;
			}
		}
		
		return result;
	}
	
	public void updateDatabaseView()
	{
		JEXStatics.jexManager.getCurrentDatabase().emitUpdateSignal();
	}
	
	public void updateObjectsView()
	{
		JEXStatics.jexManager.updateLabelsAndObjects(); // results in an AVAILABLEOBJ Sig
	}
	
}
