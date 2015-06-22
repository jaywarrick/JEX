package function.plugin.plugins.dataEditing;

import java.util.TreeMap;
import java.util.Map.Entry;

import jex.statics.JEXStatics;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Change Singleton Dimension",
		menuPath="Data Editing",
		visible=true,
		description="Rename a dimension in an object that contains one value."
		)
public class ChangeSingletonDim extends JEXPlugin {

	public ChangeSingletonDim() 
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Current Object", type=MarkerConstants.TYPE_ANY, description="Object to remove dimension from", optional=false)
	JEXData inputData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Old Dimension", description="Dimension to be changed (only if it is a singleton)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String oldDimName;

	@ParameterMarker(uiOrder=0, name="New Dimension", description="New dimension name", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String newDimName;	


	/////////// Define Outputs ///////////

	// Don't allow the gui to show the output because when the output is of type "ANY" then, when it is dragged to the input of another function
	// then it doesn't match the type of the object in the database (e.g., Image).
	//@OutputMarker(uiOrder=0, name="New Object", type=MarkerConstants.TYPE_ANY, flavor="", description="The resultant image with added dimension", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		// Validate the input data
		if(inputData == null)
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,JEXDataSingle> inputDataMap = inputData.getDataMap(); // get the TreeMap of DimensionMaps to JEXDataSingles
		this.output = new JEXData(inputData.getDataObjectType(), inputData.getDataObjectName()); // create a new JEXData that the edited TreeMap will go into

		for(Entry<DimensionMap,JEXDataSingle> e : inputDataMap.entrySet()) // for each little piece of the TreeMap (Entry)
		{
			DimensionMap newMap = e.getKey().copy(); // copy the key corresponding to the entry into a DimensionMap
			String tempValue = newMap.get(oldDimName); // get the value corresponding to the old dim name and save in tempValue
			newMap.remove(oldDimName); // remove the old dim name from the new DimensionMap
			newMap.put(newDimName, tempValue); // add the value back but with the new dim name
			output.addData(newMap, e.getValue()); // add the new map to the output JEXData
		}

		// update the database manually (since we are working at such a low level)
		JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, inputData);
		JEXStatics.jexDBManager.updateObjectsView();
		JEXStatics.jexDBManager.saveDataInEntry(optionalEntry, output, true);

		// Return status
		return true;
	}


}
