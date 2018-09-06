package function.plugin.plugins.dataEditing;

import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.Definition.TypeName;
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
		name="Merge Objects by Name (virtual)",
		menuPath="Data Editing",
		visible=true,
		description="Merges objects. If data exists at a certain dimensional location in multiple objects, objects provided that are later in the list of inputs will take priority."
		)
public class MergeObjectsByNameVirtual extends JEXPlugin {

	public MergeObjectsByNameVirtual()
	{}

	/////////// Define Inputs here ///////////


	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="Object Type", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_DROPDOWN, choices={"Image","Roi"}, defaultChoice=1)
	String objectType;
	
	@ParameterMarker(uiOrder=2, name="Search For Names Containing", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String searchName;
	
	@ParameterMarker(uiOrder=3, name="New Object Name", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Auto-merged Object")
	String newName;
	
	@ParameterMarker(uiOrder=4, name="New Dim Name", description="Name to give the new Merged Object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Roi")
	String newDimName;

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
		Type t = new Type(this.objectType);
		
		Vector<JEXData> datalist = JEXStatics.jexManager.getDatasOfTypeWithNameContainingInEntry(new TypeName(new Type(this.objectType), this.searchName), optionalEntry);
		if(datalist.size() < 2)
		{
			JEXDialog.messageDialog("Less than 2 objects found with the given search criteria in this entry. Aborting merge operation for this entry.", this);
			return false;
		}
		
		DimTable retDT = new DimTable();
		for(JEXData d : datalist)
		{
			retDT = DimTable.union(retDT, d.getDimTable());
		}
		retDT.add(new Dim(this.newDimName, datalist.size()));
		for(JEXData d : datalist)
		{
			DimTable tempDT = d.getDimTable();
			for(Dim dim : retDT)
			{
				if(dim.name() != this.newDimName && tempDT.getDimWithName(dim.name())==null)
				{
					JEXDialog.messageDialog("The dimension " + dim.name() + " does not exist in all objects. For example that dim doesn't exist in " + d.getTypeName().toString() + ". Aborting.", this);
					return false;
				}
			}
		}
		
		JEXData ret = new JEXData(t, newName);
		TreeMap<DimensionMap,JEXDataSingle> retMap = ret.getDataMap();

		int tot = retDT.mapCount();
		int count = 0;
		int percentage = 0;

		for(DimensionMap map : retDT.getMapIterator())
		{
			JEXDataSingle toAdd = null;
			int dimCounter = 0;
			for(JEXData d : datalist)
			{
				if(d.getData(map) != null)
				{
					toAdd = d.getData(map).copy();
					
					if(this.isCanceled())
					{
						return false;
					}
					
					if(t.matches(JEXData.FILE) || t.matches(JEXData.IMAGE) || t.matches(JEXData.MOVIE) || t.matches(JEXData.SOUND))
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

					DimensionMap toPut = map.copyAndSet(this.newDimName + "=" + dimCounter);
					retMap.put(toPut, toAdd);
					// Status bar
					count = count + 1;
					percentage = (int) (100 * ((double) count / (double) tot));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
				dimCounter = dimCounter + 1;
			}
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
