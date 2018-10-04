package function.plugin.plugins.imageThresholding;

import java.io.File;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.SingleUserDatabase.JEXWriter;
import fiji.threshold.Auto_Local_Threshold;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.CanvasResizer;
import ij.plugin.MontageMaker;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ROIUtility;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimensionMap;


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
		menuPath="Image Thresholding",
		visible=true,
		description="Function that allows you to use various local thresholding methods. " +
				"By 'local' here is meant that the threshold is computed for each pixel according " +
				"to the image characteristics within a window of radius r around it. 8-bit images only"
		)
public class AutoLocalThresholding extends JEXPlugin{



	public AutoLocalThresholding()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be thresholded.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Method", description="select algorithm to be applied", ui=MarkerConstants.UI_DROPDOWN, choices={"Try all", "Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack", "Otsu", "Phansalkar", "Sauvola"}, defaultChoice=0)
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

	@ParameterMarker(uiOrder=6, name="Low Percentile (> 0 <= 100)", description="The low percentile intensity limit used for preadjusting the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double loP;

	@ParameterMarker(uiOrder=7, name="Hi Percentile (> Low Percentile <= 100)", description="The high percentile intensity limit used for preadjusting the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="99")
	double hiP;

	Parameter p3 = new Parameter("'<Name>=<Value>' to Exclude (optional)", "Optionally specify a particular name value pair to exclude from thresholding. Useful for excluding bright-field (e.g., Channel=BF). Technically doesn't have to be the 'Channel' dimension.", "");

	@ParameterMarker(uiOrder=8, name="Exclusion Filter (optional)", description="'<Name>=<Val1>,<Val2>,...,<Valn>', Optionally specify a particular name value pair to exclude from thresholding. Useful for excluding bright-field (e.g., Channel=BF). Technically doesn't have to be the 'Channel' dimension. Leave blank to skip.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String toExclude;
	
	@ParameterMarker(uiOrder=9, name="Keep Unproceessed Images?", description="Should images excluded from processing be kept in the output image object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean keepUnprocessed;

	//	@ParameterMarker(uiOrder=5, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	//	static
	//	int bitDepth;


	//////////Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Thresholded Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant thresholded image", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		// validate image
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Gather parameters
		Dim filterDim = null;
		if(toExclude != null && !toExclude.equals(""))
		{
			String[] filterArray = toExclude.split("=");
			filterDim = new Dim(filterArray[0], new CSVList(filterArray[1]));
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
			boolean skip = false;
			if(filterDim != null && filterDim.containsValue(map.get(filterDim.dimName)))
			{
				skip = true;
			}
			tempPath = this.saveAdjustedImage(imageMap.get(map), skip, keepUnprocessed);

			if(tempPath != null)
			{
				outputImageMap.put(map, tempPath);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);

			if (method.equals("Try all")) //single image try all
				break;
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

		// Return status
		return true;
	}

	public String saveAdjustedImage(String imagePath, boolean skip, boolean keepUnprocessed)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}

		if(skip && !keepUnprocessed)
		{
			return null;
		}
		
		ImagePlus im = new ImagePlus(imagePath);
		if(skip)
		{
			return(JEXWriter.saveImage(im));
		}
		
		FloatProcessor imp = im.getProcessor().convertToFloatProcessor();
		ByteProcessor impByte = null;
		if(preAdjust)
		{
			// Adjust the image
			double[] pixels = getPixelsAsDoubleArray(imp, null);
			double[] limits = StatisticsUtility.percentile(pixels, new double[]{ loP, hiP });
			FunctionUtility.imAdjust(imp, limits[0], limits[1], 0d, 255d, 1d);
			//			ImagePlus imTemp = new ImagePlus("duh", temp);
			//			imTemp.show();
			impByte = (ByteProcessor) imp.convertToByte(false);
		}
		else
		{
			FunctionUtility.imAdjust(imp, imp.getMin(), imp.getMax(), 0d, 255d, 1d);
			impByte = (ByteProcessor) imp.convertToByte(true);
		}

		im.flush();
		imp = null;
		im = new ImagePlus("temp", impByte);

		// Execute auto local threshold algorithm to the image
		Auto_Local_Threshold alt = new Auto_Local_Threshold();

		String imPath = "";

		//***************** "Try all" codes are cited from ImageJ function ********************//
		// AutoLocalThreshold segmentation 
		// Following the guidelines at http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines
		// ImageJ plugin by G. Landini at bham. ac. uk
		// 1.0  15/Apr/2009
		// 1.1  01/Jun/2009
		// 1.2  25/May/2010
		// 1.3  1/Nov/2011 added constant offset to Niblack's method (request)
		// 1.4  2/Nov/2011 Niblack's new constant should be subtracted to match mean,mode and midgrey methods. Midgrey method had the wrong constant sign.
		// 1.5  18/Nov/2013 added 3 new local thresholding methdos: Constrast, Otsu and Phansalkar
		// *********************************************************************************//
		if (method.equals("Try all")){//single image try all
			ImageProcessor ip = im.getProcessor();
			int xe = ip.getWidth();
			int ye = ip.getHeight();
			String [] methods={"Try all", "Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack","Otsu", "Phansalkar", "Sauvola"};
			int ml = methods.length;
			ImagePlus imp2, imp3;
			ImageStack tstack=null, stackNew;


			tstack= new ImageStack(xe,ye);
			for (int k=1; k<ml;k++)
				tstack.addSlice(methods[k], ip.duplicate());
			imp2 = new ImagePlus("Auto Threshold", tstack);
			imp2.updateAndDraw();

			for (int k=1; k<ml;k++){
				imp2.setSlice(k);
				//IJ.log("analyzing slice with "+methods[k]);
				alt.exec(imp2, methods[k], radius, par1, par2, doIWhite );
			}
			//imp2.setSlice(1);
			CanvasResizer cr= new CanvasResizer();
			stackNew = cr.expandStack(tstack, (xe+2), (ye+18), 1, 1);
			imp3 = new ImagePlus("Auto Threshold", stackNew);
			MontageMaker mm= new MontageMaker();
			ImagePlus ret = mm.makeMontage2( imp3, 3, 3, 1.0, 1, (ml-1), 1, 0, true);
			imPath = JEXWriter.saveImage(ret.getProcessor());
			im.flush();
		}
		else{ // apply the required method to all images
			alt.exec(im, method, radius, par1, par2, doIWhite);
			// Save the results
			imPath = JEXWriter.saveImage(im.getProcessor());
			im.flush();
		}


		// return temp filePath
		return imPath;
	}

	public double[] getPixelsAsDoubleArray(FloatProcessor ip, ROIPlus optionalRoi)
	{
		float[] tempPixels = null;
		if(optionalRoi != null)
		{
			tempPixels = ROIUtility.getPixelsInRoi(ip, optionalRoi);
		}
		else
		{
			tempPixels = (float[]) ip.getPixels();
		}
		double[] pixels = new double[tempPixels.length];
		int i = 0;
		for (float f : tempPixels)
		{
			pixels[i] = f;
			i++;
		}
		tempPixels = null;
		return pixels;
	}
}
