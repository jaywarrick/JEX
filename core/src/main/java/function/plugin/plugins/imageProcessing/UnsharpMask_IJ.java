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
import ij.plugin.filter.GaussianBlur;
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
		name="DoG Filter",
		menuPath="Image Processing",
		visible=true,
		description="A DoG filter based on IJ's unsharp mask filter, adding a prefiltering step and weighting if desired. (Gaussian blur 1 - weight*Gaussian blur 2)/(1-weight)"
		)
public class UnsharpMask_IJ extends JEXPlugin {

	public UnsharpMask_IJ()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Sigma 1", description="Sigma of the first Gaussian filter kernel. Rule of thumb: radius = 1.5*sigma given +/- 1.5 sigma is 86% of AUC.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double sigma1;

	@ParameterMarker(uiOrder=2, name="Sigma 2", description="Sigma of the second Gaussian filter kernel. Rule of thumb: radius = 1.5*sigma given +/- 1.5 sigma is 86% of AUC.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double sigma2;

	@ParameterMarker(uiOrder=3, name="Mask Weight", description="For Unsharp masking, set weight 0.1-0.9. For pure DoG filtering with no weighting, set weight=1 (i.e., simply Gaussian 1 - Gaussian 2).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double weight;

	@ParameterMarker(uiOrder=4, name="Output Bit Depth", description="Bit depth of the saved image.", ui=MarkerConstants.UI_DROPDOWN, choices={"8", "16", "32"}, defaultChoice=1)
	String depth;

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
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		//		UnsharpMask um = new UnsharpMask();
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			// Call helper method
			ImagePlus im = new ImagePlus(imageMap.get(map));
			FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();
			//			ip.setSnapshotPixels(ip.getPixelsCopy());
			this.DoG(ip, sigma1, sigma2, (float) weight);
			im = FunctionUtility.makeImageToSave(ip, "false", Integer.parseInt(this.depth));
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

	/**
	 * This is a DoG filter, separately filtering the image with each Gaussian sigma.
	 * Then, the result of sigma2 is subtracted from sigma1.
	 * 
	 * The concept of weighting is brought over from the unsharp mask, allowing different
	 * levels of edge enhancement.
	 *  
	 * If sigma1 is null or <= 0, then the prefiltering step is skipped and it behaves
	 * as a simple unsharp mask.
	 * 
	 * @param ip is the FloatProcessor to filter
	 * @param sigma1 is a pre-filter to reduce noise before enhancing edges.
	 * @param sigma2 is the actual radius for enhancing edges and is typically larger than radius2
	 */
	public void DoG(FloatProcessor fp, Double sigma1, Double sigma2, float weight)
	{
		GaussianBlur gb = new GaussianBlur();
		FloatProcessor fp2 = (FloatProcessor) fp.duplicate();
		if(sigma1 != null && sigma1 > 0)
		{
			gb.blurGaussian(fp, sigma1, sigma1, 0.00002);
		}
		gb.blurGaussian(fp2, sigma2, sigma2, 0.00002);
		float[] pixels1 = (float[])fp.getPixels();
		float[] pixels2 = (float[])fp2.getPixels();
		int width = fp.getWidth();
		int height = fp.getHeight();
		if(weight==1d)
		{
			for (int y = 0; y < height; y++)
				for (int x = 0, p=width*y+x; x < width; x++, p++)
					pixels1[p] = (pixels1[p] - pixels2[p]);
		}
		else
		{
			for (int y = 0; y < height; y++)
				for (int x = 0, p=width*y+x; x < width; x++, p++)
					pixels1[p] = (pixels1[p] - weight*pixels2[p])/(1f - weight);
		}
	}
}
