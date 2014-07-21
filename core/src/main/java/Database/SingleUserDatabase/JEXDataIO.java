package Database.SingleUserDatabase;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXWorkflow;
import Database.DataReader.FileReader;
import Database.SingleUserDatabase.xml.ObjectFactory;
import Database.SingleUserDatabase.xml.XData;
import Database.SingleUserDatabase.xml.XDataSingle;
import cruncher.JEXFunction;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.XMLUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;

public class JEXDataIO {
	
	public static final String DETACHED_FILEEXTENSION = "jxd";
	public static final String WORKFLOW_FILEEXTENSION = "jxw";
	
	public static String createJXD(JEXData data, String extension, boolean transaction) throws Exception
	{
		TreeMap<DimensionMap,JEXDataSingle> dataMap = data.getDataMap();
		TreeMap<DimensionMap,String> dataTable = new TreeMap<DimensionMap,String>();
		Dim metaDim = new Dim(JEXTableWriter.METADATA);
		boolean firstSingle = true;
		for (DimensionMap map : dataMap.keySet())
		{
			DimensionMap newMap;
			TreeMap<String,String> singleDataMap = dataMap.get(map).getDataMap();
			for (String singleKey : singleDataMap.keySet())
			{
				newMap = map.copy();
				newMap.put(JEXTableWriter.METADATA, singleKey);
				if(firstSingle)
				{
					metaDim.dimValues.add(singleKey);
				}
				dataTable.put(newMap, singleDataMap.get(singleKey));
			}
			firstSingle = false;
		}
		DimTable dimTable = data.getDimTable().copy();
		dimTable.add(metaDim);
		Table<String> tableToSave = new Table<String>(dimTable, dataTable);
		String sourcePath = null;
		if(transaction)
		{
			sourcePath = JEXTableWriter.writeJXD(data.getTypeName().toString(), tableToSave, extension);
		}
		else
		{
			sourcePath = JEXTableWriter.writeTable(data.getTypeName().toString(), tableToSave, extension);
		}
		
		return sourcePath;
	}
	
	/**
	 * Returns the heirarchically and TypeName defined file name to save data in entry ENTRY
	 * 
	 * @param type
	 * @param name
	 * @param entry
	 * @param f
	 * @return
	 */
	public static String createReferencedFileName(JEXDataSingle ds)
	{
		// Get the directory to save in
		JEXEntry entry = ds.getParent().getParent();
		
		// Get the row and column of the entry
		String row = "" + entry.getTrayX();
		String column = "" + entry.getTrayY();
		
		// Make the dimension string
		String dimStr = "";
		DimensionMap dim = ds.getDimensionMap();
		if(dim != null)
		{
			int i = 0;
			for (String dimName : dim.keySet())
			{
				if(i == 0)
				{
					dimStr = dimName + dim.get(dimName);
				}
				else
				{
					dimStr = dimStr + "_" + dimName + dim.get(dimName);
				}
				i = i + 1;
			}
		}
		
		// Get the extension
		String extension = FileUtility.getFileNameExtension(FileReader.readToPath(ds));
		
		// Construct a file name
		String fileName;
		if(dimStr.equals(""))
		{
			fileName = "x" + row + "_y" + column + "." + extension;
		}
		else
		{
			fileName = "x" + row + "_y" + column + "_" + FileUtility.removeWhiteSpaceOnEnds(dimStr) + "." + extension;
		}
		
		return fileName;
	}
	
	/**
	 * Returns the heirarchically and TypeName defined file name to save data in entry ENTRY
	 * 
	 * @param type
	 * @param name
	 * @param entry
	 * @param f
	 * @return
	 */
	public static String createJXDFileName(JEXData data)
	{
		// Get the directory to save in
		JEXEntry entry = data.getParent();
		
		// Get the row and column of the entry
		String row = "" + entry.getTrayX();
		String column = "" + entry.getTrayY();
		
		// Get the extension
		String extension = DETACHED_FILEEXTENSION;
		
		// Construct a file name
		String fileName = "x" + row + "_y" + column + "." + extension;
		
		return fileName;
	}
	
	/**
	 * Just load the DimTable for this JEXData.
	 * 
	 * @param data
	 */
	public static void loadDimTable(JEXData data)
	{
		if(data.hasDetachedFile())
		{
			// Then we could be dealing with either and old xml object or a new
			// arff/csv object
			if(FileUtility.getFileNameExtension(data.getDetachedRelativePath()).equals("xml"))
			{
				// OLD XML VERSION OF JEXDATA FOUND
				JEXDataIO.loadDetachedXMLJEXDataDimTable(data);
			}
			else
			// THEN CSV/ARFF
			{
				DimTable dimTable = JEXTableReader.getDimTable(JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath());
				dimTable.removeDimWithName(JEXTableWriter.METADATA);
				data.setDimTable(dimTable);
			}
		}
		else
		{
			// Then we are dealing with an old xml object and all the info is
			// already loaded
			// DON'T DO ANYTHING
		}
	}
	
	/**
	 * Load the DimTable and data map for this JEXData.
	 * 
	 * @param data
	 */
	public static void loadDimTableAndDataMap(JEXData data)
	{
		if(data.hasDetachedFile())
		{
			// Then we could be dealing with either and old xml object or a new
			// arff/jxd object
			// We also need to load all the information
			if(FileUtility.getFileNameExtension(data.getDetachedRelativePath()).equals("xml"))
			{
				// OLD XML VERSION OF JEXDATA FOUND
				JEXDataIO.loadDetachedXMLJEXData(data);
			}
			else
			// THEN ARFF/JXD
			{
				JEXDataIO.loadJXD(data, JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath());
			}
		}
		else
		{
			// Then we are either dealing with an old old xml object and all the
			// data was already loaded by JEXDBIO
		}
	}
	
	/**
	 * Build a JEXDataSingle from the information in the table. RETURNING NULL IF ANY METADATA IS MISSING
	 * 
	 * @param data
	 * @param table
	 * @param map
	 * @param metaDim
	 * @return
	 */
	private static JEXDataSingle makeJEXDataSingle(Table<String> table, DimensionMap map, Dim metaDim)
	{
		JEXDataSingle ret = new JEXDataSingle();
		DimensionMap temp = map.copy();
		for (String metaKey : metaDim.dimValues)
		{
			temp.put(JEXTableWriter.METADATA, metaKey);
			// Logs.log("adding value to single", 0,
			// JEXDataIO.class.getSimpleName());
			String metaValue = table.getData(temp);
			if(metaValue != null)
			{
				// Logs.log("adding value to single", 0,
				// JEXDataIO.class.getSimpleName());
				ret.put(metaKey, metaValue);
			}
		}
		if(ret.getDataMap().size() == 0)
		{
			return null;
		}
		return ret;
	}
	
	/**
	 * Load the detached XML version of the JEXData
	 */
	private static void loadDetachedXMLJEXData(JEXData data)
	{
		// Parse the xml object file
		Logs.log("Parsing the object xml string " + data.getDetachedRelativePath(), 1, JEXDataIO.class.getSimpleName());
		String loadPath = JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath();
		
		// Get the Xdata object
		XData temp = (XData) XMLUtility.XMLload(loadPath, new ObjectFactory());
		List<XDataSingle> singles = temp.getSingleDataElements();
		
		// Make the dimTable again
		String dataDims = temp.getAtt(JEXData.DIMS);
		if(dataDims != null)
		{
			DimTable table = new DimTable(dataDims);
			data.setDimTable(table);
		}
		
		// Fill the datamap
		data.datamap = new TreeMap<DimensionMap,JEXDataSingle>();
		for (XDataSingle ds : singles)
		{
			JEXDataSingle dataSingle = JEXDBIO.XDataSingleToDatabaseObject(ds, data);
			if(dataSingle == null)
			{
				continue;
			}
			DimensionMap map = dataSingle.getDimensionMap();
			data.addData(map, dataSingle);
		}
		
		if(data.getTypeName().getType().equals(JEXData.FUNCTION_OLD))
		{
			JEXFunction func = null;
			try
			{
				func = JEXFunction.fromOldJEXData(data);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Logs.log("Can't load old function: " + data.toString() + ". Skipping.", JEXDataIO.class);
				return;
			}
			
			JEXWorkflow workflow = new JEXWorkflow(data.getDataObjectName());
			workflow.add(func);
			JEXData newData = workflow.toJEXData();
			
			data.type = newData.type;
			data.info.clear();
			data.info.putAll(newData.info);
			data.setAuthor(newData.getAuthor());
			data.setDataObjectDate(newData.getDataObjectDate());
			data.setDataObjectModifDate(newData.getDataObjectModifDate());
			data.setDataID(newData.getDataID());
			
			data.setDimTable(newData.getDimTable());
			data.datamap.clear();
			data.datamap.putAll(newData.datamap);
		}
	}
	
	/**
	 * Load the detached XML version of the JEXData
	 */
	private static void loadDetachedXMLJEXDataDimTable(JEXData data)
	{
		// Parse the xml object file
		Logs.log("Parsing the object xml string " + data.getDetachedRelativePath(), 1, data);
		String loadPath = JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath();
		
		// Get the Xdata object
		XData temp = (XData) XMLUtility.XMLload(loadPath, new ObjectFactory());
		
		// Make the dimTable again
		String dataDims = temp.getAtt(JEXData.DIMS);
		if(dataDims != null)
		{
			DimTable table = new DimTable(dataDims);
			data.setDimTable(table);
		}
	}
	
	public static void loadJXD(JEXData data, String fullpath)
	{
		if(!(new File(fullpath)).exists())
		{
			/* Create an empty dataTable for the time being to allow the database
			 * to open. Then the object can be deleted once the viewer sees that the
			 * object doesn't actually contain any data.
			 */
			data.setDimTable(new DimTable());
			data.datamap = new TreeMap<DimensionMap,JEXDataSingle>();
			return;
		}
		Table<String> table = JEXTableReader.getStringTable(fullpath);
		DimTable temp = table.dimTable.copy();
		Dim metaDim = temp.removeDimWithName(JEXTableWriter.METADATA);
		data.setDimTable(temp);
		data.datamap = new TreeMap<DimensionMap,JEXDataSingle>();
		if(temp.size() == 0)
		{
			DimensionMap map = new DimensionMap();
			JEXDataSingle ds = JEXDataIO.makeJEXDataSingle(table, map, metaDim);
			if(ds != null)
			{
				data.addData(map, ds);
			}
		}
		else
		{
			for (Entry<DimensionMap,String> entry : table.data.entrySet())
			{
				JEXDataSingle ds = JEXDataIO.makeJEXDataSingle(table, entry.getKey(), metaDim);
				if(ds != null)
				{
					// Logs.log(ds.toString(), 0,
					// JEXDataIO.class.getSimpleName());
					DimensionMap newMap = entry.getKey().copy();
					newMap.remove(JEXTableWriter.METADATA);
					data.addData(newMap, ds);
				}
			}
		}
		return;
	}
}
