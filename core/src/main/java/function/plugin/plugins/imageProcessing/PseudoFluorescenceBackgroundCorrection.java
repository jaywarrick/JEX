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
import function.plugin.plugins.medianFilterHelpers.FastMedian;
// Import needed classes here 
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.StringUtility;
import tables.DimensionMap;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name="Pseudo Fluorescence Background Correction",
		menuPath="Image Processing",
		visible=true,
		description="Subtract the background from fluorescence images in log-space to approximate flat-field correction (assumes background is constant and largescale variations are due to illumination only)."
		)
public class PseudoFluorescenceBackgroundCorrection extends JEXPlugin {

	// Define a constructor that takes no arguments.
	public PseudoFluorescenceBackgroundCorrection()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData inputData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="Rolling ball radius", description="Radius of the rolling ball kernal", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50.0")
	double radius;

	boolean lightBackground = false;

	@ParameterMarker(uiOrder=2, name="Sliding parabaloid?", description="Parabaloid is generally a little faster and has less artifacts than rolling ball.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean paraboloid;

	@ParameterMarker(uiOrder=3, name="Dark Field Estimate", description="How much dark field intensity is there that should be subtracted from the image prior to log scaling.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int df;

	@ParameterMarker(uiOrder=4, name="Pre-smoothing Radius", description="Radius of a mean filter to apply prior to correction (set to 0 or less to skip).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3.0")
	double presmoothRadius;

	@ParameterMarker(uiOrder=5, name="Post Multiplier", description="How much to multiply the result to alter values if desired (set to 1 to skip).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double postMultiplier;

	@ParameterMarker(uiOrder=5, name="Post Addition", description="How much to add to the result to alter values if desired (set to 0 to skip).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double postAddition;

	@ParameterMarker(uiOrder=6, name="Exclusion Filter", description="<DimName>=<Val1>,<Val2>,...<Valn>, Specify the dimension and dimension values to exclude. Leave blank to process all.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String exclusionFilter;

	@ParameterMarker(uiOrder=7, name="Keep Filtered Images?", description="Should the images that were filtered out be kept in the resultant image object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean keepFiltered;

	@ParameterMarker(uiOrder=8, name="Bit Depth for Saving", description="What bit depth should the result be saved as?", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=1)
	int bitDepth;

	boolean presmooth = false;

	/////////// Define Outputs here ///////////

	// See Database.Definition.OutpuMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Psuedo Background Corrected Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
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
		if(inputData == null || !inputData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		// Convert Parameters if needed
		TreeMap<DimensionMap,String> exclusionMap = StringUtility.getCSVStringAsStringTreeMap(exclusionFilter);
		if(exclusionMap == null)
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(inputData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();

		int count = 0;
		int total = imageMap.size();
		int percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			String path = imageMap.get(map);

			// get the image
			ImagePlus im = new ImagePlus(path);
			if(im.getBitDepth() == 32)
			{
				JEXDialog.messageDialog("This function only works with 16 and 8 bit images at the moment. Aborting.", this);
				return false;
			}
			if(exclusionMap.get(map) != null)
			{
				// Then skip, but save unprocessed image if necessary
				if(keepFiltered)
				{
					JEXWriter.convertToBitDepthIfNecessary(im, this.bitDepth);
					String tempPath = JEXWriter.saveImage(im);
					outputMap.put(map, tempPath);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}

			// Convert to a FloatProcessor
			ImageProcessor imp = im.getProcessor();
			imp.subtract(this.df + 1);
			imp.add(1);
			double max = imp.getMax();
			imp = JEXWriter.convertToBitDepthIfNecessary(im.getProcessor(), 32);


			ImageProcessor impTemp = new FloatProcessor(imp.getFloatArray());
			impTemp.log();

			//// Subtract the background from the filtered (Image-DF)
			
			if(presmoothRadius > 0)
			{

				impTemp = FastMedian.process(impTemp, (int) Math.round(radius*2));
				//				RankFilters rF = new RankFilters();
				//				rF.rank(impTemp, presmoothRadius, RankFilters.MEDIAN);
			}

			// Amplify the logged signal to calculate the background (for some reason it matters for low intensity float images), then de-amplify
			impTemp.multiply(65535.0/Math.log(max));
			BackgroundSubtracter bS = new BackgroundSubtracter();
			bS.rollingBallBackground(impTemp, radius, true, lightBackground, paraboloid, presmooth, true);
			impTemp.multiply(Math.log(max)/65535.0);

			// Subtract the background of the logged image from the logged image
			imp.log();
			imp.copyBits(impTemp, 0, 0, FloatBlitter.SUBTRACT);

			// Release memory of impTemp
			impTemp = null;

			// "Un-log" the image
			imp.exp();

			// Subtract 1 given exp(0) is 1.
			imp.add(-1);

			// Perform post-multiplication if desired to bring signal levels up.
			// Without post-multiplication, the result is essentially signal to noise ratio.
			if(postMultiplier != 1.0)
			{
				imp.multiply(postMultiplier);
			}

			// Perform post-addition if desired (e.g., to retain the "gaussian" nature of the noise)
			if(postAddition != 0.0)
			{
				imp.add(postAddition);
			}

			// Convert the image to the appropriate bit depth and save
			ImageProcessor toSave = JEXWriter.convertToBitDepthIfNecessary(imp, this.bitDepth);
			String finalPath = JEXWriter.saveImage(toSave);
			outputMap.put(map.copy(), finalPath);

			// Update display info of progress
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) total)));
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
