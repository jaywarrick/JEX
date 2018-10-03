package function.plugin.plugins.imageProcessing;

// Import needed classes here 
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ImageProcessor;

import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name="ImageJ Background Subtraction",
		menuPath="Image Processing",
		visible=true,
		description="Subtract the background from an image using the ImageJ background subtraction tool."
		)
public class ImageJBackgroundSubtraction extends JEXPlugin {

	// Define a constructor that takes no arguments.
	public ImageJBackgroundSubtraction()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData inputData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="Rolling ball radius", description="Radius of the rolling ball kernal", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50.0")
	double radius;
	
	@ParameterMarker(uiOrder=2, name="Light background?", description="Generally false for fluroescent images and true for bright-field etc.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean lightBackground;

	@ParameterMarker(uiOrder=3, name="Create background (don't subtract)?", description="Output an 'image' of the background instead of subtracting from the original and outputing the result?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean createBackground;

	@ParameterMarker(uiOrder=4, name="Sliding parabaloid?", description="Parabaloid is generally a little faster and has less artifacts than rolling ball.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean paraboloid;

	@ParameterMarker(uiOrder=5, name="Do presmoothing?", description="A simple checkbox for entering true/false variables.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean presmooth;

	/////////// Define Outputs here ///////////

	// See Database.Definition.OutpuMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Background Subtracted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputData;

	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(inputData == null || !inputData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(inputData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();

		int count = 0;
		int total = images.size();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap dim : images.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			String path = images.get(dim);

			// get the image
			ImagePlus im = new ImagePlus(path);
			ImageProcessor imp = im.getProcessor();

			// //// Begin Actual Function
			BackgroundSubtracter bS = new BackgroundSubtracter();

			// apply the function to imp
			bS.rollingBallBackground(imp, radius, createBackground, lightBackground, paraboloid, presmooth, true);

			// //// End Actual Function
			String finalPath = JEXWriter.saveImage(imp);

			outputMap.put(dim.copy(), finalPath);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;

			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		if(outputMap.size() == 0)
		{
			return false;
		}

		// Set the outputs
		this.outputData = ImageWriter.makeImageStackFromPaths("temp Name", outputMap);
		this.outputData.setDataObjectInfo("Background subtracted using background subtraction function");

		// Return status
		return true;
	}
}
