package function.plugin.plugins.dataEditing;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.StringUtility;
import tables.DimensionMap;

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
		name="Rename Dimension and Values",
		menuPath="Data Editing",
		visible=true,
		description="Rename a dimension and its values."
		)
public class RenameDimensionAndValues extends JEXPlugin {

	public RenameDimensionAndValues() 
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Current Object", type=MarkerConstants.TYPE_ANY, description="Object to remove dimension from", optional=false)
	JEXData inputData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Old Dimension Name", description="Dimension to be changed", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String oldDimName;

	@ParameterMarker(uiOrder=1, name="New Dimension Name", description="New dimension name", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String newDimName;
	
	@ParameterMarker(uiOrder=2, name="Old Dimension Values", description="Comma separated list (leading and trailing spaces are removed) of values for this dimension to be changed.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String oldDimValues;

	@ParameterMarker(uiOrder=3, name="New Dimension Values", description="Comma separated list (leading and trailing spaces are removed) of values new values to replace old (respectively).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String newDimValues;	



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
		TreeMap<String,String> oldToNewMap = new TreeMap<>(new StringUtility());
		CSVList oldNameList = new CSVList(oldDimValues);
		CSVList newNameList = new CSVList(newDimValues);
		
		if(oldNameList.size() != newNameList.size())
		{
			JEXDialog.messageDialog("List of old dim values must the same length as the list of new dim values! Aborting.", this);
			return false;
		}
		
		if(inputData.getDimTable().getDimWithName(StringUtility.removeWhiteSpaceOnEnds(oldDimName)) == null)
		{
			JEXDialog.messageDialog("This object doesn't have a dim with the name '" + oldDimName + "'. Please check and re-run.", this);
			return false;
		}
		
		for(int i = 0; i < oldNameList.size(); i++)
		{
			oldToNewMap.put(StringUtility.removeWhiteSpaceOnEnds(oldNameList.get(i)), StringUtility.removeWhiteSpaceOnEnds(newNameList.get(i)));
		}

		for(Entry<DimensionMap,JEXDataSingle> e : inputDataMap.entrySet()) // for each little piece of the TreeMap (Entry)
		{
			DimensionMap newMap = e.getKey().copy(); // copy the key corresponding to the entry into a DimensionMap
			String tempValue = newMap.get(StringUtility.removeWhiteSpaceOnEnds(oldDimName)); // get the value corresponding to the old dim name and save in tempValue
			String newValue = oldToNewMap.get(StringUtility.removeWhiteSpaceOnEnds(tempValue));
			if(newValue != null)
			{
				tempValue = StringUtility.removeWhiteSpaceOnEnds(newValue);
			}
			newMap.remove(StringUtility.removeWhiteSpaceOnEnds(oldDimName)); // remove the old dim name from the new DimensionMap
			newMap.put(StringUtility.removeWhiteSpaceOnEnds(newDimName), tempValue); // add the value back but with the new dim name
			output.addData(newMap, e.getValue()); // add the new map to the output JEXData
		}
		
		// Could try to create a new DimTable based on the old one but forgoing that for now.

		// update the database manually (since we are working at such a low level)
		JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, inputData);
		JEXStatics.jexDBManager.updateObjectsView();
		JEXStatics.jexDBManager.saveDataInEntry(optionalEntry, output, true);

		// Return status
		return true;
	}


}
