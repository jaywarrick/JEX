package function.plugin.plugins.dataEditing;

import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import logs.Logs;

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

	@ParameterMarker(uiOrder=1, name="Merge Result with Existing?", description="Should the result be merged with any possible existing object of the same name? This is useful in Auto-Updating mode.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean mergeWithExisting;

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

		Logs.log("yo", this);
		JEXData update = JEXWriter.performVirtualMerge(datalist, optionalEntry, this.newName, this);
		this.output.add(update);
		
		for(JEXData d : datalist)
		{
			if(!d.getDataObjectName().equals(this.newName))
			{
				JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, d);
			}
		}
		
		// Perform the merge.
		if(mergeWithExisting)
		{			
			TypeName existingTN = new TypeName(new Type(datalist.get(0).getTypeName().getType().getType(), null), this.newName);
			JEXData existing = JEXStatics.jexManager.getDataOfTypeNameInEntry(existingTN, optionalEntry);
			if(existing != null)
			{
				JEXData updateCopy = JEXWriter.copyData(update);
				updateCopy.setDataObjectFlavor(null);
				datalist.clear();
				datalist.add(updateCopy);
				datalist.add(existing);
				JEXData ret = JEXWriter.performVirtualMerge(datalist, optionalEntry, this.newName, this);
				ret.setDataObjectType(new Type(ret.getDataObjectType().getType(), null));
				output.add(ret);
			}
		}
		
		// Return status
		return true;
	}
}
