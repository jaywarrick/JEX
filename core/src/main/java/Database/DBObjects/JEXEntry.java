package Database.DBObjects;

import Database.DataWriter.HeirarchyWriter;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXDB;

import java.util.Date;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.DateUtility;
import miscellaneous.StringUtility;

public class JEXEntry implements Comparable<JEXEntry> {
	
	public static String EXPERIMENT = "Experiment";
	public static String TRAY = "Tray";
	public static String DATE = "Date";
	public static String MODIFDATE = "Modification Date";
	public static String INFO = "Info";
	public static String X = "X";
	public static String Y = "Y";
	public static String EID = "ID";
	public static String VALID = "Valid";
	public static String AUTHOR = "Author";
	public static String[] descriptors = new String[] { EXPERIMENT, DATE, MODIFDATE, TRAY, INFO, VALID };
	public static String[] filterables = new String[] { EXPERIMENT, DATE, MODIFDATE, TRAY, X, Y };
	
	TreeMap<String,String> info;
	JEXDB parent;
	TreeMap<Type,TreeMap<String,JEXData>> dataList;
	public String loadTimeExperimentName = null, loadTimeTrayName = null;
	
	public JEXEntry()
	{
		info = new TreeMap<String,String>();
	}
	
	// ----------------------------------------------------
	// --------- GETTERS AND SETTERS ----------------------
	// ----------------------------------------------------
	
	/**
	 * Return the value of key KEY in the info map
	 * 
	 * @param key
	 * @return Value of Key KEY
	 */
	public String get(String key)
	{
		return info.get(key);
	}
	
	/**
	 * Set the value VALUE at key KEY
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, String value)
	{
		this.info.put(key, value);
	}
	
	/**
	 * Set the column location in the tray
	 * 
	 * @param x
	 */
	public void setTrayX(int x)
	{
		info.put(X, "" + x);
		JEXData data = HeirarchyWriter.makeHeirarchy(JEXEntry.X, "" + this.getTrayX());
		this.addData(data, true);
	}
	
	/**
	 * Return the column location in the tray
	 * 
	 * @return Column number
	 */
	public int getTrayX()
	{
		String xstr = info.get(X);
		if(xstr == null)
			return -1;
		Integer x = Integer.parseInt(xstr);
		return x;
	}
	
	/**
	 * Set the row location in the tray
	 * 
	 * @param y
	 */
	public void setTrayY(int y)
	{
		info.put(Y, "" + y);
		JEXData data = HeirarchyWriter.makeHeirarchy(JEXEntry.Y, "" + this.getTrayY());
		this.addData(data, true);
	}
	
	/**
	 * Return the row location in the tray
	 * 
	 * @return Row number
	 */
	public int getTrayY()
	{
		String ystr = info.get(Y);
		if(ystr == null)
			return -1;
		Integer y = Integer.parseInt(ystr);
		return y;
	}
	
	/**
	 * Set the experiment name the entry belongs to
	 * 
	 * @param experiment
	 */
	public void setEntryExperiment(String experiment)
	{
		info.put(EXPERIMENT, experiment);
		JEXData data = HeirarchyWriter.makeHeirarchy(JEXEntry.EXPERIMENT, this.getEntryExperiment());
		this.addData(data, true);
	}
	
	/**
	 * Return the experiment this entry belongs to
	 * 
	 * @return Experiment name
	 */
	public String getEntryExperiment()
	{
		String result = (info.get(EXPERIMENT) == null) ? "No Experiment" : info.get(EXPERIMENT);
		return result;
	}
	
	/**
	 * Set the information on the experiment the entry belongs to
	 * 
	 * @param experiment
	 */
	public void setEntryExperimentInfo(String experimentInfo)
	{
		info.put(INFO, experimentInfo);
	}
	
	/**
	 * Return the information on the experiment this entry belongs to
	 * 
	 * @return Experiment name
	 */
	public String getEntryExperimentInfo()
	{
		String result = (info.get(INFO) == null) ? "No Info" : info.get(INFO);
		return result;
	}
	
	/**
	 * Return the tray this experiment belongs to
	 * 
	 * @return Tray Name
	 */
	public String getEntryTrayName()
	{
		// Will return null when no tray has been set to mark that this is a new
		// type of JEXEntry
		String result = info.get(TRAY);
		return result;
	}
	
	/**
	 * Set the tray this entry belongs to
	 * 
	 * @param author
	 */
	public void setAuthor(String author)
	{
		info.put(AUTHOR, author);
	}
	
	/**
	 * Return the tray this experiment belongs to
	 * 
	 * @return Author
	 */
	public String getAuthor()
	{
		String result = (info.get(AUTHOR) == null) ? "No author" : info.get(AUTHOR);
		return result;
	}
	
	/**
	 * Set the date of the entry
	 * 
	 * @param ID
	 */
	public void setDate(String date)
	{
		info.put(DATE, date);
	}
	
	/**
	 * Return the date this entry
	 * 
	 * @return String ID
	 */
	public String getDate()
	{
		String date = info.get(DATE);
		if(date == null || date.equals("NA") || date.equals(""))
			return DateUtility.getDate();
		return info.get(DATE);
	}
	
	/**
	 * Set the date of the entry
	 * 
	 * @param ID
	 */
	public void setModificationDate(String date)
	{
		info.put(MODIFDATE, date);
	}
	
	/**
	 * Return the date this entry
	 * 
	 * @return String ID
	 */
	public String getModificationDate()
	{
		String date = info.get(MODIFDATE);
		if(date == null || date.equals("NA") || date.equals(""))
			setModificationDate(DateUtility.getDate());
		return info.get(MODIFDATE);
	}
	
	/**
	 * Set the ID of this experiment, WARNING not to be used in normal circumstances
	 * 
	 * @param ID
	 */
	public void setEntryID(String ID)
	{
		info.put(EID, ID);
	}
	
	/**
	 * Return the unique ID of this entry
	 * 
	 * @return String ID
	 */
	public String getEntryID()
	{
		return info.get(EID);
	}
	
	/**
	 * Set the parent database of the entry
	 * 
	 * @param parent
	 */
	public void setParent(JEXDB parent)
	{
		this.parent = parent;
	}
	
	/**
	 * Return the database parent of the database
	 * 
	 * @return
	 */
	public JEXDB getParent()
	{
		return this.parent;
	}
	
	/**
	 * Recalculate the modiciation date
	 * 
	 * @return
	 */
	public String findModificationDate()
	{
		String dateStr = null;
		
		TreeMap<Type,TreeMap<String,JEXData>> dataList = getDataList();
		if(dataList == null || dataList.size() == 0)
		{
			if(this.getDate() != null)
				return this.getDate();
			else
				return DateUtility.getDate();
		}
		
		for (Type type : dataList.keySet())
		{
			TreeMap<String,JEXData> dl = dataList.get(type);
			for (String name : dl.keySet())
			{
				JEXData data = dl.get(name);
				String dateStr2 = data.getDataObjectDate();
				
				if(dateStr == null)
				{
					dateStr = dateStr2;
					continue;
				}
				
				Date date = DateUtility.makeDate(dateStr);
				Date date2 = DateUtility.makeDate(dateStr2);
				if(dateStr2 != null && DateUtility.compareDates(date, date2) > 0)
				{
					dateStr = dateStr2;
				}
			}
		}
		
		return dateStr;
	}
	
	// ----------------------------------------------------
	// --------- ADD REMOVE DATA TO THE INTERNAL LIST -----
	// ----------------------------------------------------
	
	/**
	 * Add a data element to this entry... If flag boolean is true an existing data of the same name and type will be overwritten
	 * 
	 * @param data
	 * @param overwrite
	 * @return success flag
	 */
	public boolean addData(JEXData data, boolean overwrite)
	{
		
		if(data == null)
		{
			return false;
		}
		
		try
		{
			data.setParent(this); // Important because data doesn't have parent
			// yet.
			
			// Get the object keys TYPE and NAME
			Type   type = data.getDataObjectType();
			String name = data.getDataObjectName();
			
			// Get the dictionaries corresponding and create them if null
			TreeMap<String,JEXData> nmap = this.getDataList().get(type);
			if(nmap == null)
			{
				nmap = new TreeMap<String,JEXData>();
				this.getDataList().put(type, nmap);
			}
			JEXData existingData = nmap.get(name);
			
			// If a data object already exists in that spot, overwrite if the
			// flag allows, return if not
			if(existingData != null && !overwrite)
				return false;
			
			// // If a data with the same type and name exists and you want to
			// overwirte it, delete it before
			// if (existingData != null && overwrite)
			// {
			// JEXStatics.jexDBManager.removeDataFromEntry(this, existingData);
			// }
			
			// Get the dictionaries corresponding and create them if null
			nmap = this.getDataList().get(type);
			if(nmap == null)
			{
				nmap = new TreeMap<String,JEXData>();
				this.getDataList().put(type, nmap);
			}
			
			// Add the new data
			nmap.put(name, data);
			return true;
			
		}
		catch (Exception e)
		{
			String value = "";
			if(data != null && data.getDataObjectName() != null)
				value = data.getDataObjectName();
			Logs.log("Error adding data to the database " + value, 0, this);
			JEXStatics.statusBar.setStatusText("Error creating data");
			return false;
		}
	}
	
	/**
	 * Remove data with TypeName TYPENAME
	 * 
	 * @param typeName
	 */
	public boolean removeData(JEXData data)
	{
		if(data == null)
			return true;
		
		boolean success = true;
		
		// Get the entry and the type name to remove
		Type   type = data.getDataObjectType();
		String name = data.getDataObjectName();
		
		// get the no dict and remove the object
		TreeMap<String,JEXData> nameMap = this.getDataList().get(type);
		if(nameMap == null)
			return true;
		nameMap.remove(name);
		
		// If the name map is empty remove the type map
		if(nameMap.size() == 0)
		{
			this.getDataList().remove(type);
		}
		
		return success;
	}
	
	/**
	 * Return the data contained in this entry of TypeName TYPENAME if non exists returns null
	 * 
	 * @param typeName
	 * @return JEXData of TypeName typeName
	 */
	public JEXData getData(TypeName typeName)
	{
		TreeMap<String,JEXData> dl = getDataList().get(typeName.getType());
		if(dl == null)
			return null;
		
		JEXData data = dl.get(typeName.getName());
		return data;
	}
	
	/**
	 * Returns the list of jexdata objects in this entry
	 * 
	 * @return JEXData array
	 */
	public TreeMap<Type,TreeMap<String,JEXData>> getDataList()
	{
		if(dataList == null)
			dataList = new TreeMap<Type,TreeMap<String,JEXData>>();
		return this.dataList;
	}
	
	/**
	 * Set the data list of this entry
	 * 
	 * @param dataList
	 */
	public void setDataList(TreeMap<Type,TreeMap<String,JEXData>> dataList)
	{
		this.dataList = dataList;
	}
	
	// ----------------------------------------------------
	// --------- OUTPUTTING AND EXPORT --------------------
	// ----------------------------------------------------
	
	/**
	 * Return a string description of the entry
	 * 
	 * @return String value of the entry
	 */
	@Override
	public String toString()
	{
		String result = "Entry: ID = " + getEntryID() + " on date " + getDate();
		result = result + " located in " + getEntryExperiment();
		result = result + " - " + getTrayX() + "." + getTrayY();
		
		result = "ID-" + getEntryID();
		return result;
	}
	
	/**
	 * Return a string description of the entry
	 * 
	 * @return String value of the entry
	 */
	public String toSimpleString()
	{
		String result = getEntryExperiment() + " - " + getTrayX() + "." + getTrayY();
		return result;
	}
	
	// ----------------------------------------------------
	// --------- HASHMAP FUNCTIONS ------------------------
	// ----------------------------------------------------
	
	public int compareTo(JEXEntry o)
	{
		JEXEntry f = o;
		int idCompare = StringUtility.compareString(this.getEntryID(), f.getEntryID());
		return idCompare;
		// return this.getEntryID().compareTo(f.getEntryID());
	}
	
	@Override
	public int hashCode()
	{
		return this.getEntryID().hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof JEXEntry))
			return false;
		JEXEntry entry = (JEXEntry) o;
		return this.getEntryID().equals(entry.getEntryID());
	}
	
}
