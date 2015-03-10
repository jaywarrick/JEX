package function.plugin.plugins.imageThresholding;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import fiji.threshold.Auto_Local_Threshold;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;


/**
 * Function that allows you to use various local thresholding methods. 
 * By 'local' here is meant that the threshold is computed for each pixel 
 * according to the image characteristics within a window of radius r around it. 
 * 8-bit images only
 * 
 * @author Thoms Huibregtse, Mengcheng Qi
 *
 */

@Plugin(
		type = JEXPlugin.class,
		name="Auto Local Threshold",
		menuPath="Image Thresholding > Auto Local Threshold",
		visible=true,
		description="Function that allows you to use various local thresholding methods. " +
				"By 'local' here is meant that the threshold is computed for each pixel according " +
				"to the image characteristics within a window of radius r around it. 8-bit images only"
		)
public class AutoLocalThresholding extends JEXPlugin{



	public AutoLocalThresholding()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be thresholded.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Method", description="select algorithm to be applied", ui=MarkerConstants.UI_DROPDOWN, choices={"Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack", "Otsu", "Phansalkar", "Sauvola"}, defaultChoice=0)
	static
	String method;


	@ParameterMarker(uiOrder=1, name="radius", description="sets the radius of the local domain over which the threshold will be computed", ui=MarkerConstants.UI_TEXTFIELD, defaultText="15")
	int radius;

	@ParameterMarker(uiOrder=2, name="White Obj on Black Backgr", description="sets to white the pixels with values above the threshold value (otherwise, it sets to white the values less or equal to the threshold)", ui=MarkerConstants.UI_DROPDOWN, choices={"true","false"})
	static
	boolean doIWhite;

	@ParameterMarker(uiOrder=3, name="Parameter 1", description="See imagej.net/Auto_Local_Threshold for details", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	double par1;

	@ParameterMarker(uiOrder=4, name="Parameter 2", description="See imagej.net/Auto_Local_Threshold for details", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	double par2;

	/////////// Define Conversion Parameters for images that are not 8 bit ///////////

	@ParameterMarker(uiOrder=5, name="Pre-adjust?", description="Whether to adjust intensities before converting to 8-bit scale using options below (check the box) or just autoscale (don't check the box)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean preAdjust;
	
	@ParameterMarker(uiOrder=6, name="Min", description="Intensity to make 0 in 8-bit image", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double oldMin;

	@ParameterMarker(uiOrder=7, name="Max", description="Intensity to make 255 in 8-bit image", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4095.0")
	double oldMax;

	double newMin = 0;

	double newMax = 255;

	//	@ParameterMarker(uiOrder=5, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	//	static
	//	int bitDepth;


	//////////Define Outputs ///////////

	@OutputMarker(name="Thresholded Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant thresholded image", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		// validate image
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;

		// iterate through the DimTable to get each DimensionMap
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}

			// call the real local threshold function and save the result
			tempPath = this.saveAdjustedImage(imageMap.get(map), method, radius, par1, par2, doIWhite);
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

	public String saveAdjustedImage(String imagePath, String myMethod, int radius,  double par1, double par2, boolean doIwhite )
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}


		ImagePlus im = new ImagePlus(imagePath);
		
		FloatProcessor imp = im.getProcessor().convertToFloatProcessor();
		ByteProcessor impByte = null;
		if(preAdjust)
		{
			// Adjust the image
			FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, 1);
			impByte = (ByteProcessor) imp.convertToByte(false);
		}
		else
		{
			impByte = (ByteProcessor) imp.convertToByte(true);
		}
		
		im.flush();
		imp = null;
		im = new ImagePlus("temp", impByte);

		// Execute auto local threshold algorithm to the image
		Auto_Local_Threshold alt = new Auto_Local_Threshold();
		alt.exec(im, method, radius, par1, par2, doIWhite);
		
		// Save the results
		String imPath = JEXWriter.saveImage(im.getProcessor());
		im.flush();

		// return temp filePath
		return imPath;
	}

}
