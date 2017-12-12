package function.plugin.plugins.dataEditing;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
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
		name="Slice Object",
		menuPath="Data Editing",
		visible=true,
		description="Take a slice of an object producing a new object with a subset of the data."
		)
public class SliceObject extends JEXPlugin {

	public SliceObject()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder = 1, name = "Object", type = MarkerConstants.TYPE_ANY, description = "Any object", optional = false)
	JEXData inputData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="New Object Name", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Merged Object")
	String newName;

	@ParameterMarker(uiOrder=2, name="Keep or Exclude Data", description="Should the specified filter be used to keep or exclude data from the object?", ui=MarkerConstants.UI_DROPDOWN, choices={"Keep","Exclude"}, defaultChoice=0)
	String operation;

	@ParameterMarker(uiOrder=3, name="Filter DimTable", description="Include or Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
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

		JEXData ret = new JEXData(inputData.getTypeName().getType(), newName);
		TreeMap<DimensionMap,JEXDataSingle> retMap = ret.getDataMap();

		int tot = inputData.getDimTable().mapCount();
		int count = 0;
		int percentage = 0;
		boolean success = false;
		if(t.matches(JEXData.FILE) || t.matches(JEXData.IMAGE) || t.matches(JEXData.MOVIE) || t.matches(JEXData.SOUND))
		{
			for(DimensionMap map : inputData.getDimTable().getMapIterator())
			{
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
					if(!filter.testMapAsExclusionFilter(map))
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
					if(filter.testOverdefinedMap(map))
					{
						// The data fits the 'keep' filter and should be added to the new object
						success = this.copyData(toAdd);
						if(!success)
						{
							return false;
						}
					}
				}

				retMap.put(map.copy(), toAdd);
				// Status bar
				count = count + 1;
				percentage = (int) (100 * ((double) count / (double) tot));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		else
		{
			JEXDialog.messageDialog("The data provided isn't currently one of the supported data types that can be used with this fuction (FILE, IMAGE, MOVIE, SOUND).", this);
			return false;
		}

		ret.setDimTable(new DimTable(retMap));
		output.add(ret);

		// Return status
		return true;
	}

	public boolean copyData(JEXDataSingle toAdd)
	{

		// Copy any additional file data if necessary, otherwise it is stored directly in the JEXDataSingle
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
		return true;
	}
}
