package function.plugin.plugins.imageProcessing;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
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

@Plugin(
		type = JEXPlugin.class,
		name="Scale Image Intensity",
		menuPath="Image Processing",
		visible=true,
		description="Scale the image by a scale factor."
		)
public class ScaleImage extends JEXPlugin {
	
	public ScaleImage()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Scale", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double scale;
	
	@ParameterMarker(uiOrder=2, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;
	
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
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		int total = images.size();
		for (DimensionMap dim : images.keySet())
		{
			
			if(this.isCanceled())
			{
				return false;
			}
			
			String path = images.get(dim);
			// File f = new File(path);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			ij.process.ImageProcessor imProc = im.getProcessor();
			if(imProc == null)
				continue;
			FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
			
			int newWidth = (int) (imp.getWidth() * scale);
			imp.setInterpolationMethod(ImageProcessor.BILINEAR);
			imp = (FloatProcessor) imp.resize(newWidth);
			
			ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
			String finalPath = JEXWriter.saveImage(toSave);
			
			outputMap.put(dim.copy(), finalPath);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		output = ImageWriter.makeImageStackFromPaths("temp", outputMap);
		output.setDataObjectInfo("Stack binned using binning function");
		
		// Return status
		return true;
	}
}
