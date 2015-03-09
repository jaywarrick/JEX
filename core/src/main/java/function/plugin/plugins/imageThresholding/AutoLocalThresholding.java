package function.plugin.plugins.imageThresholding;

import ij.ImagePlus;
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


@Plugin(
		type = JEXPlugin.class,
		name="Auto Local Threshold",
		menuPath="Image Thresholding > Auto Local Threshold",
		visible=true,
		description="Function that allows you to stitch an image ARRAY into a single image using two image alignment objects."
		)
public class AutoLocalThresholding extends JEXPlugin{



	public AutoLocalThresholding()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be thresholded.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Method", description="select algorithm to be applied", ui=MarkerConstants.UI_DROPDOWN, choices={"Try All", "Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack", "Otsu", "Phansalkar", "Sauvola"}, defaultChoice=0)
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
	
	@ParameterMarker(uiOrder=5, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	static
	int bitDepth;

	//@ParameterMarker(uiOrder=5, name="Stack", description="can be used to process all the slices of a stack", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	//boolean stackProcessing;

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
		//imageData.getDataMap(); // dead code?
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
			tempPath = saveAdjustedImage(imageMap.get(map), method, radius, par1, par2, doIWhite);
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

	public static String saveAdjustedImage(String imagePath, String myMethod, int radius,  double par1, double par2, boolean doIwhite )
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		

		// Adjust the image
		Auto_Local_Threshold alt = new Auto_Local_Threshold();
		alt.exec(im, method, radius, par1, par2, doIWhite);

		
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
		
		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		im.flush();

		// return temp filePath
		return imPath;
	}

}
