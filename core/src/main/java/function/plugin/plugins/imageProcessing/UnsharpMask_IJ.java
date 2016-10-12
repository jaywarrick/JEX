package function.plugin.plugins.imageProcessing;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

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
import ij.ImagePlus;
import ij.plugin.filter.UnsharpMask;
import ij.process.FloatProcessor;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
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
		name="Unsharp Mask",
		menuPath="Image Processing",
		visible=true,
		description="ImageJ's unsharp mask filter"
		)
public class UnsharpMask_IJ extends JEXPlugin {

	public UnsharpMask_IJ()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Radius (sigma)", description="Radius of the filter kernel", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double radius;
	
	@ParameterMarker(uiOrder=2, name="Mask Weight (0.1-0.9)", description="Weight given to the mask during sharpening.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.6")
	double weight;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Adjusted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
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
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		UnsharpMask um = new UnsharpMask();
		for (DimensionMap map : imageMap.keySet())
		{
			// Call helper method
			ImagePlus im = new ImagePlus(imageMap.get(map));
			int bitDepth = im.getBitDepth();
			FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();
			ip.setSnapshotPixels(ip.getPixelsCopy());
			um.sharpenFloat(ip, radius, (float) weight);
			im = FunctionUtility.makeImageToSave(ip, "false", bitDepth);
			tempPath = JEXWriter.saveImage(im);
			if(tempPath != null)
			{
				outputImageMap.put(map, tempPath);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}
		
		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);
		
		// Return status
		return true;
	}
}
