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
 * @author jay warrick
 * 
 */
@Plugin(
		type = JEXPlugin.class,
		name="Merge Objects (virtual)",
		menuPath="Data Editing",
		visible=true,
		description="Merges objects. If data exists at a certain dimensional location in multiple objects, objects provided that are eariler in the list of inputs will take priority."
		)
public class MergeObjectsVirtual extends JEXPlugin {

	public MergeObjectsVirtual()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder = 1, name = "Object 1", type = MarkerConstants.TYPE_ANY, description = "Any object", optional = false)
	JEXData data1;

	@InputMarker(uiOrder = 2, name = "Object 2", type = MarkerConstants.TYPE_ANY, description = "Any object.", optional = false)
	JEXData data2;
	
	@InputMarker(uiOrder = 3, name = "Object 3", type = MarkerConstants.TYPE_ANY, description = "Any object.", optional = false)
	JEXData data3;
	
	@InputMarker(uiOrder = 4, name = "Object 4", type = MarkerConstants.TYPE_ANY, description = "Any object.", optional = false)
	JEXData data4;
	
	@InputMarker(uiOrder = 5, name = "Object 5", type = MarkerConstants.TYPE_ANY, description = "Any object.", optional = false)
	JEXData data5;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=0, name="New Object Name", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Merged Object")
	String newName;

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
		Vector<JEXData> datalist = new Vector<>();
		Type t = data1.getTypeName().getType();
		if(data1 != null)
		{
			datalist.add(data1);
		}
		if(data2 != null)
		{
			datalist.add(data2);
		}
		if(data3 != null)
		{
			datalist.add(data3);
		}
		if(data4 != null)
		{
			datalist.add(data4);
		}
		if(data5 != null)
		{
			datalist.add(data5);
		}
		
		for(JEXData d : datalist)
		{
			if(!d.getTypeName().getType().matches(t))
			{
				JEXDialog.messageDialog("All data must be of the same type to merge. Aborting.", this);
				return false;
			}
		}
		
		DimTable retDT = new DimTable();
		for(JEXData d : datalist)
		{
			retDT = DimTable.union(retDT, d.getDimTable());
		}
		for(JEXData d : datalist)
		{
			DimTable tempDT = d.getDimTable();
			for(Dim dim : retDT)
			{
				if(tempDT.getDimWithName(dim.name())==null)
				{
					JEXDialog.messageDialog("The dimension " + dim.name() + " does not exist in all objects. For example that dim doesn't exist in " + d.getTypeName().toString() + ". Aborting.", this);
					return false;
				}
			}
		}
		
		JEXData ret = new JEXData(data1.getTypeName().getType(), newName);
		TreeMap<DimensionMap,JEXDataSingle> retMap = ret.getDataMap();

		int tot = retDT.mapCount();
		int count = 0;
		int percentage = 0;

		for(DimensionMap map : retDT.getMapIterator())
		{
			JEXDataSingle toAdd = null;
			for(JEXData d : datalist)
			{
				if(d.getData(map) != null)
				{
					toAdd = d.getData(map).copy();
					break; // This way, the first object with something at this dimension map takes precedent
				}
			}
			if(toAdd == null)
			{
				continue;
			}
			
			if(this.isCanceled())
			{
				return false;
			}
			
			if(data1.getTypeName().getType().matches(JEXData.FILE) || data1.getTypeName().getType().matches(JEXData.IMAGE) || data1.getTypeName().getType().matches(JEXData.MOVIE) || data1.getTypeName().getType().matches(JEXData.SOUND))
			{
				toAdd.put(JEXDataSingle.RELATIVEPATH,  toAdd.get(JEXDataSingle.RELATIVEPATH));
				//				// Copy any additional file data if necessary, otherwise it is stored directly in the JEXDataSingle
				//				File src = new File(JEXWriter.getDatabaseFolder() + File.separator + toAdd.get(JEXDataSingle.RELATIVEPATH));
				//				String extension = FileUtility.getFileNameExtension(toAdd.get(JEXDataSingle.RELATIVEPATH));
				//				String relativePath = JEXWriter.getUniqueRelativeTempPath(extension);
				//				File dst = new File(JEXWriter.getDatabaseFolder() + File.separator + relativePath);
				//				try {
				//					if(!FileUtility.moveFileOrFolder(src, dst, true))
				//					{
				//						Logs.log("Failed to move a file for the object! Aborting", this);
				//						return false;
				//					}
				//				} catch (IOException e) {
				//					Logs.log("Failed to move a file for the object! Aborting", this);
				//					return false;
				//				}
				//				toAdd.put(JEXDataSingle.RELATIVEPATH, relativePath);
			}

			retMap.put(map.copy(), toAdd);
			// Status bar
			count = count + 1;
			percentage = (int) (100 * ((double) count / (double) tot));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		ret.setDimTable(new DimTable(retMap));
		output.add(ret);
		
		for(JEXData d : datalist)
		{
			JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, d);
		}

		// Return status
		return true;
	}
}
