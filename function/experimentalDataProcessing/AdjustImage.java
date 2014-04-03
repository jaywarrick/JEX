package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import Database.SingleUserDatabase.xml.XElement;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@PluginMarker(
		type = JEXBatchablePlugin.class,
		name="Adjust Image Test",
		toolbox="Sandbox",
		visibleInToolbox=true,
		description="Test of new plugin interface for JEX using the SciJavaPlugin mechanism.",
		allowMultiThreading=true
		)
public class AdjustImage implements JEXBatchablePlugin {

	public AdjustImage()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(defaultName="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(name="Old Min", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double oldMin;
	
	@ParameterMarker(name="Old Max", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4095.0")
	double oldMax;
	
	@ParameterMarker(name="New Min", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double newMin;
	
	@ParameterMarker(name="New Max", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="65535.0")
	double newMax;
	
	@ParameterMarker(name="New Min", description="0.1-5.0, value of 1 results in no change", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double gamma;
	
	@ParameterMarker(name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(defaultName="Adjusted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", defaultSaving=true)
	JEXData output;
	

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		imageData.getDataMap();
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			tempPath = saveAdjustedImage(imageMap.get(map), oldMin, oldMax, newMin, newMax, gamma, bitDepth);
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
		
		this.output = ImageWriter.makeImageStackFromPaths("", outputImageMap);
		
		// Return status
		return true;
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
		
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
		
		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		im.flush();
		
		// return temp filePath
		return imPath;
	}
}
