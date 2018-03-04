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
import ij.plugin.ContrastEnhancer;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import tables.DimTable;
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
		name="FFT Band Pass Filter",
		menuPath="Image Processing",
		visible=true,
		description="Filter objects by size using frequency domain filtering."
		)
public class FFTBandPassFilter extends JEXPlugin {

	public FFTBandPassFilter()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Filter: Max Size", description="The largest features to keep [pixels].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50.0")
	double filterLargeDia;

	@ParameterMarker(uiOrder=2, name="Filter: Min Size", description="The smallest features to keep [pixels].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double filterSmallDia;

	@ParameterMarker(uiOrder=3, name="Suppress Stripes", description="Should stripes be suppressed and, if so, which direction should be suppressed.", ui=MarkerConstants.UI_DROPDOWN, choices={"None","Horizontal","Vertical"}, defaultChoice=0)
	String choiceDia;

	@ParameterMarker(uiOrder=4, name="Tolerane of Direction", description="Tolerance of direction for stripe suppression [%].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	double toleranceDia;

	@ParameterMarker(uiOrder=5, name="Save filter?", description="Should the filter in the frequency domain be saved as an image?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saveFilter;

	@ParameterMarker(uiOrder=6, name="Autoscale result?", description="Should the result be automatically scaled?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean doScalingDia;

	@ParameterMarker(uiOrder=7, name="Saturate autoscaling?", description="If autoscaled, should the result be saturated (1% at tails) to better fill the dynamic range?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saturateDia;

	@ParameterMarker(uiOrder=8, name="Output Bit Depth", description="Depth of the outputted image for all channels.", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;

	//	@ParameterMarker(uiOrder=9, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	//	String filterDimTableString;

	@ParameterMarker(uiOrder=10, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;

	@ParameterMarker(uiOrder=11, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Filtered Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant fft filtered image", enabled=true)
	JEXData output;

	@OutputMarker(uiOrder=2, name="FFT Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The fft filter used to filter the image", enabled=true)
	JEXData fft;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry entry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		//		try
		//		{
		//			TreeMap<DimensionMap,Double> oldMinsMap = StringUtility.getCSVStringAsDoubleTreeMapForDimTable(channelDimName + "=" + oldMins, imageData.getDimTable());

		//			if(oldMinsMap == null)
		//			{
		//				return false;
		//			}

		DimTable filterTable = new DimTable(this.filterDimTableString);

		FFT_Filter fft = new FFT_Filter();
		fft.choiceDia = this.choiceDia;
		fft.displayFilter = this.saveFilter;
		fft.doScalingDia = false;
		fft.filterLargeDia = this.filterLargeDia;
		fft.filterSmallDia = this.filterSmallDia;
		fft.saturateDia = false;
		fft.toleranceDia = this.toleranceDia;

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> fftImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			if(filterTable.testMapAsExclusionFilter(map))
			{
				if(this.keepExcluded)
				{
					Logs.log("Skipping the processing of " + map.toString(), this);
					ImagePlus out = new ImagePlus(imageMap.get(map));
					tempPath = JEXWriter.saveImage(out);
					if(tempPath != null)
					{
						outputImageMap.put(map, tempPath);
					}
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
				else
				{
					Logs.log("Skipping the processing and saving of " + map.toString(), this);
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
				continue;
			}
			// Check if canceled
			if(this.isCanceled())
			{
				return false;
			}

			ImageProcessor out = (new ImagePlus(imageMap.get(map))).getProcessor();
			ImagePlus filter = fft.filter(out);
			if (this.doScalingDia) {
				
				FunctionUtility.imAdjust(out, out.getMin(), out.getMax(), 0.0, Math.pow(2, this.bitDepth)-1, 1);
				ImagePlus imp2 = new ImagePlus("Image-filtered", out);
				new ContrastEnhancer().stretchHistogram(imp2, this.saturateDia?1.0:0.0);
				out = imp2.getProcessor();
			}
			
			out = JEXWriter.convertToBitDepthIfNecessary(out, this.bitDepth);
			tempPath = JEXWriter.saveImage(out);
			if(tempPath != null)
			{
				outputImageMap.put(map, tempPath);
			}
			if(filter != null)
			{
				tempPath = JEXWriter.saveImage(filter);
				fftImageMap.put(map, tempPath);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		if(outputImageMap.size() == 0)
		{
			return false;
		}
		if(fftImageMap.size() != 0)
		{
			this.fft = ImageWriter.makeImageStackFromPaths("temp", fftImageMap);
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

		// Return status
		return true;

		//	}
		//		catch(NumberFormatException e)
		//		{
		//			JEXDialog.messageDialog("Couldn't parse one of the number values provided.");
		//			e.printStackTrace();
		//			return false;
		//		}		
	}

	//	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	//	{
	//		// Get image data
	//		File f = new File(imagePath);
	//		if(!f.exists())
	//		{
	//			return null;
	//		}
	//		ImagePlus im = new ImagePlus(imagePath);
	//		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
	//		
	//		// Adjust the image
	//		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//		
	//		// Save the results
	//		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
	//		String imPath = JEXWriter.saveImage(toSave);
	//		im.flush();
	//		
	//		// return the filepath
	//		return imPath;
	//	}
}
