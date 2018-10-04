package function.plugin.plugins.dataEditing;

import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import tables.DimTable;
import tables.DimensionMap;

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
		name="Split Object (virtual)",
		menuPath="Data Editing",
		visible=true,
		description="Split the object with respect to a given dimension."
		)
public class SplitObjectVirtual extends JEXPlugin {

	public SplitObjectVirtual()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder = 1, name = "Object", type = MarkerConstants.TYPE_ANY, description = "Any object", optional = false)
	JEXData inputData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=0, name="New Object Name", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Split")
	String newName;
	
	@ParameterMarker(uiOrder=1, name="Dimension to Split", description="Name of the dimension to split.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimToSplit;
	
	@ParameterMarker(uiOrder=2, name="Keep Singleton Dimensions?", description="The resulting objects will have one value for the split dimension. Should this dimension be kept (checked) or discarded (uncheck).", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean keepSingleton;

	@ParameterMarker(uiOrder=3, name="Keep or Exclude Data", description="Should the specified filter be used to keep or exclude data from the object?", ui=MarkerConstants.UI_DROPDOWN, choices={"Keep","Exclude"}, defaultChoice=1)
	String operation;

	@ParameterMarker(uiOrder=4, name="Filter DimTable", description="Include or Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String filterString;

	/////////// Define Outputs here ///////////

	// Can't define the output because we don't know the type it will be. Specifying a type of "ANY" won't work when we try and drag and drop it.

	@OutputMarker(uiOrder=1, name="Output Object", type=MarkerConstants.TYPE_ANY, flavor="", description="The resultant merged object.", enabled=true)
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
		Type t = inputData.getTypeName().getType();

		DimTable filter = new DimTable(this.filterString);

		boolean keeping = this.operation.equals("Keep");
		
		if(this.dimToSplit == "" || inputData.getDimTable().getDimWithName(this.dimToSplit) == null)
		{
			JEXDialog.messageDialog("The dim to split was either blank or was not found in the object. Aborting.", this);
			return false;
		}

		int tot = inputData.getDimTable().mapCount();
		int count = 0;
		int percentage = 0;
		boolean success = false;
		if(t.matches(JEXData.FILE) || t.matches(JEXData.IMAGE) || t.matches(JEXData.MOVIE) || t.matches(JEXData.SOUND) || t.matches(JEXData.ROI))
		{
			DimTable dt = inputData.getDimTable();
			for(DimTable subDT : dt.getSubTableIterator(this.dimToSplit))
			{
				JEXData ret = new JEXData(inputData.getTypeName().getType(), newName + " " + this.dimToSplit + " " + subDT.getDimWithName(this.dimToSplit).valueAt(0));
				TreeMap<DimensionMap,JEXDataSingle> retMap = ret.getDataMap(); // get a reference to the datamap
				
				for(DimensionMap map : subDT.getMapIterator())
				{
					success = false;
					JEXDataSingle toAdd = null;
					if(inputData.getData(map) != null)
					{
						toAdd = inputData.getData(map).copy();
					}
					if(toAdd == null)
					{
						continue;
					}

					if(!keeping)
					{
						// Then we are trying to exclude data using the filter
						if(!filter.testMapAsExclusionFilter(map) && !t.matches(JEXData.ROI))
						{
							// Then the data doesn't fit the exclusion filter and should be added to the new object
							success = this.copyData(toAdd);
							if(!success)
							{
								return false;
							}
						}
					}
					else
					{
						// Then we are trying to keep the data using the filter
						if(filter.testOverdefinedMap(map) && !t.matches(JEXData.ROI))
						{
							// The data fits the 'keep' filter and should be added to the new object
							success = this.copyData(toAdd);
							if(!success)
							{
								return false;
							}
						}
					}

					if(success || t.matches(JEXData.ROI))
					{
						if(keepSingleton)
						{
							retMap.put(map.copy(), toAdd);
						}
						else
						{
							DimensionMap temp = map.copy();
							temp.remove(this.dimToSplit);
							retMap.put(temp, toAdd);
						}
					}
					
					// Status bar
					count = count + 1;
					percentage = (int) (100 * ((double) count / (double) tot));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
				
				ret.setDimTable(new DimTable(retMap));
				output.add(ret);
			}
		}
		else
		{
			JEXDialog.messageDialog("The data provided isn't currently one of the supported data types that can be used with this fuction (FILE, IMAGE, MOVIE, SOUND, ROI).", this);
			return false;
		}

		JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, this.inputData);
		// Return status
		return true;
	}

	public boolean copyData(JEXDataSingle toAdd)
	{
		toAdd.put(JEXDataSingle.RELATIVEPATH, toAdd.get(JEXDataSingle.RELATIVEPATH));
		return true;
	}
}
