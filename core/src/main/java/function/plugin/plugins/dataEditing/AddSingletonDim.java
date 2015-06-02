package function.plugin.plugins.dataEditing;

import java.util.Map.Entry;
import java.util.TreeMap;

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
		name="Add Singleton Dimension",
		menuPath="Data Editing",
		visible=true,
		description="Add a dimension to an object that contains 1 value."
		)
public class AddSingletonDim extends JEXPlugin {

	public AddSingletonDim()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Current Object", type=MarkerConstants.TYPE_ANY, description="Object to add dimension to", optional=false)
	JEXData inputData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=0, name="Dimension", description="Dimension to be added", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimName;
	
	@ParameterMarker(uiOrder=1, name="Value", description="The value of the dimension", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String value;
	
	
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
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(inputData == null)
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,JEXDataSingle> inputDataMap = inputData.getDataMap();
		this.output = new JEXData(inputData.getDataObjectType(), inputData.getDataObjectName());
		for(Entry<DimensionMap,JEXDataSingle> e : inputDataMap.entrySet())
		{
			DimensionMap newMap = e.getKey().copy();
			newMap.put(dimName, value);
			output.addData(newMap, e.getValue());
		}
		
		JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, inputData);
		JEXStatics.jexDBManager.updateObjectsView();
		JEXStatics.jexDBManager.saveDataInEntry(optionalEntry, output, true);
		// Return status
		return true;
	}
}
