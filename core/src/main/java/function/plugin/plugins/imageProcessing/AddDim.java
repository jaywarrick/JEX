package function.plugin.plugins.imageProcessing;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

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
		name="Add Dimension",
		menuPath="Image Processing",
		visible=true,
		description="Add a dimension to an image object."
		)
public class AddDim extends JEXPlugin {

	public AddDim()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to add dimension to", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=0, name="Dimension", description="Dimension to be added", ui=MarkerConstants.UI_DROPDOWN, defaultText="Color")
	DimensionMap dimension;
	
	@ParameterMarker(uiOrder=1, name="Value", description="The value of the dimension", ui=MarkerConstants.UI_TEXTFIELD, choices = {"Color","Time"}, defaultChoice=0)
	String value;
	
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=0, name="Image+Dimension", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant image with added dimension", enabled=true)
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
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		
		imageMap.put(dimension, value);
		
		// return the same object that was input
		this.output = imageData;
		
		// Return status
		return true;
	}
}
