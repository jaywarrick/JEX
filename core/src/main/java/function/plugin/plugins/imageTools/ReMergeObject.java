package function.plugin.plugins.imageTools;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import logs.Logs;
import miscellaneous.FileUtility;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author jay warrick
 * 
 */
@Plugin(
		type = JEXPlugin.class,
		name="ReMerge Objects",
		menuPath="Data Editing",
		visible=true,
		description="ReMerges Objects that were split with the 'Split Objects' function."
		)
public class ReMergeObject extends JEXPlugin {

	public ReMergeObject()
	{}

	/////////// Define Inputs here ///////////

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=0, name="Object Base Name", description="Base name of the object to remerge (i.e., the common portion of the name)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Color")
	String baseName;

	@ParameterMarker(uiOrder=1, name="Object Type", description="Type of object to look for.", ui=MarkerConstants.UI_DROPDOWN, choices={MarkerConstants.TYPE_ANY, MarkerConstants.TYPE_FILE, MarkerConstants.TYPE_IMAGE, MarkerConstants.TYPE_LABEL, MarkerConstants.TYPE_MOVIE, MarkerConstants.TYPE_ROI, MarkerConstants.TYPE_SOUND, MarkerConstants.TYPE_VALUE, MarkerConstants.TYPE_WORKFLOW}, defaultChoice=2)
	String type;
	
	@ParameterMarker(uiOrder=0, name="New Object Name", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Merged Object")
	String newName;

	/////////// Define Outputs here ///////////
	
	// Can't define the output because we don't know the type it will be. Specifying a type of "ANY" won't work when we try and drag and drop it.

	@OutputMarker(uiOrder=1, name="Split Image", type=MarkerConstants.TYPE_ANY, flavor="", description="The resultant split image stack", enabled=true)
	Vector<JEXData> output = new Vector<JEXData>();
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		TreeMap<Type, TreeMap<String,JEXData>> allData = optionalEntry.getDataList();
		
		TreeMap<DimensionMap,JEXData> objectsToMerge = new TreeMap<DimensionMap,JEXData>();
		
		// Collect objects matching the specified basename
		for(Entry<Type, TreeMap<String,JEXData>> e1 : allData.entrySet())
		{
			if(e1.getKey().matches(new Type(type)))
			{
				boolean foundMatch = false;
				for(Entry<String,JEXData> e2 : e1.getValue().entrySet())
				{
					if(e2.getKey().startsWith(baseName) && e2.getKey().length() > baseName.length())
					{
						foundMatch = true;
						DimensionMap map = getDimensionMapFromName(e2.getKey(), baseName);
						objectsToMerge.put(map, e2.getValue());
					}
				}
				if(foundMatch)
				{
					break;
				}
			}
		}
		
		JEXData ret = new JEXData(new Type(type), newName);
		
		for(Entry<DimensionMap,JEXData> e1 : objectsToMerge.entrySet())
		{
			TreeMap<DimensionMap,JEXDataSingle> temp = e1.getValue().getDataMap();
			for(Entry<DimensionMap,JEXDataSingle> e2 : temp.entrySet())
			{
				JEXDataSingle toAdd = e2.getValue().copy();
				if(e1.getValue().getTypeName().getType().matches(JEXData.FILE) || e1.getValue().getTypeName().getType().matches(JEXData.IMAGE) || e1.getValue().getTypeName().getType().matches(JEXData.MOVIE) || e1.getValue().getTypeName().getType().matches(JEXData.SOUND))
				{
					// Copy any file data if necessary
					File src = new File(JEXWriter.getDatabaseFolder() + File.separator + toAdd.get(JEXDataSingle.RELATIVEPATH));
					String extension = FileUtility.getFileNameExtension(toAdd.get(JEXDataSingle.RELATIVEPATH));
					String relativePath = JEXWriter.getUniqueRelativeTempPath(extension);
					File dst = new File(JEXWriter.getDatabaseFolder() + File.separator + relativePath);
					try {
						if(!JEXWriter.copy(src, dst))
						{
							Logs.log("Failed to copy a file for the object! Aborting", this);
							return false;
						}
					} catch (IOException e) {
						Logs.log("Failed to copy a file for the object! Aborting", this);
						return false;
					}
					toAdd.put(JEXDataSingle.RELATIVEPATH, relativePath);
				}
				DimensionMap newMap = e1.getKey().copy();
				newMap.putAll(e2.getKey());
				ret.addData(newMap, toAdd);
			}
		}
		
		
		output.add(ret);
		
		// Return status
		return true;
	}

	private DimensionMap getDimensionMapFromName(String name, String baseName)
	{
		String rest = name.substring(baseName.length(), name.length()).trim();
		String[] parts = rest.split("\\s+");
		String firstPart = parts[0];
		String value = "0";
		if(parts.length >= 1)
		{
			value = rest.substring(firstPart.length(), rest.length()).trim();
		}
		else
		{
			Logs.log("Warning: Couldn't parse the object name appropriately", this);
		}
		
		DimensionMap ret = new DimensionMap();
		ret.put(firstPart, value);
		return ret;
	}
}
