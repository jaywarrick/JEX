package function.plugin.plugins.dataEditing;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

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
import tables.Dim;
import tables.DimTable;
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
		name="Sort Decimal Dimension Values",
		menuPath="Data Editing",
		visible=true,
		description="Sort the values of dimensions interpreting them as numbers instead of strings/characters."
		)
public class SortDecimalDimensionValues extends JEXPlugin {

	public SortDecimalDimensionValues() 
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Current Object", type=MarkerConstants.TYPE_ANY, description="Object to edit", optional=false)
	JEXData inputData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Dimensions to Sort", description="Dimension to be changed", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimNames;

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
		CSVList dimNameList = new CSVList(dimNames);
		
		// Copy over the JEXDataSingles
		TreeMap<DimensionMap, JEXDataSingle> retMap = this.output.getDataMap();
		for(Entry<DimensionMap,JEXDataSingle> e : inputDataMap.entrySet())
		{
			retMap.put(e.getKey(), e.getValue().copy());
		}
		
		DimTable newDT = getDecimalOrderedDimTable(inputData.getDimTable(), dimNameList);
		
		this.output.setDimTable(newDT);
		
		// Remove the old data object.
		JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, this.inputData);
		//		JEXStatics.jexDBManager.updateObjectsView(); // This is called within the next call.
		JEXStatics.jexDBManager.saveDataInEntry(optionalEntry, output, true);

		// Return status
		return true;
	}
	
	public static DimTable getDecimalOrderedDimTable(DimTable dt, String[] dimNames)
	{
		Vector<String> temp = new Vector<>();
		for(String s : dimNames)
		{
			temp.add(s);
		}
		return getDecimalOrderedDimTable(dt, temp);
	}
	
	public static DimTable getDecimalOrderedDimTable(DimTable dt, Collection<String> dimNames)
	{
		// Remove whitespace from ends of the dimension names provided.
		Vector<String> editedList = new Vector<>();
		for(String name : dimNames)
		{
			editedList.add(StringUtility.removeWhiteSpaceOnEnds(name));
		}
		
		// Notify if any of them might be mispelled and inadvertantly skipped.
		for(String name : editedList)
		{
			if(dt.getDimWithName(name) == null)
			{
				JEXDialog.messageDialog("The specified dimension name '" + name + "' was not found in the object. Skipping that name.", SortDecimalDimensionValues.class);
			}
		}
		
		// Go through dim table and create a new one in the same order with decimal sorted versions of the specified dimensions.
		DimTable ret = new DimTable();
		for(String dimName : dt.getDimensionNames())
		{
			if(editedList.contains(dimName))
			{
				ret.add(getDecimalOrderedDim(dt.getDimWithName(dimName)));
			}
			else
			{
				ret.add(dt.getDimWithName(dimName).copy());
			}
		}
		
		return ret;
	}
	
	public static Dim getDecimalOrderedDim(Dim d)
	{
		TreeMap<Double,String> doubleOrder = new TreeMap<>();
		for(String v : d.dimValues)
		{
			doubleOrder.put(Double.parseDouble(v), v);
		}
		Dim finalDim = new Dim(d.dimName, doubleOrder.values());
		return(finalDim);
	}


}
