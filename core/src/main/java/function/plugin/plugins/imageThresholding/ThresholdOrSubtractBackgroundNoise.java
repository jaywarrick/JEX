// Define package name as "plugins" as show here
package function.plugin.plugins.imageThresholding;

// Import needed classes here 
import ij.ImagePlus;
import ij.process.FloatProcessor;
import image.roi.ROIPlus;

import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ROIUtility;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;

import org.scijava.plugin.Plugin;

import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
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
		name="Threshold or Subtract Background Noise",
		menuPath="Image Thresholding",
		visible=true,
		description="Threshold or subtract background on an image based on the median (and mad/sigma) pixel intensity of a roi area."
		)
public class ThresholdOrSubtractBackgroundNoise extends JEXPlugin {

	// Define a constructor that takes no arguments.
	public ThresholdOrSubtractBackgroundNoise()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=2, name="Roi (Optional)", type=MarkerConstants.TYPE_ROI, description="Roi indicating region with which to assess background.", optional=true)
	JEXData roiData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="Color Dim Name", description="This textbox is automatically parsed for conversion to whatever primitive type this annotation is associated with (e.g., String, double, int, etc).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String colorDimName;

	@ParameterMarker(uiOrder=2, name="Threshold? (vs. Subtract)", description="Whether to threshold or subtract based on the pixel values in the roi region (or whole image if no roi provided)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true) // Should keep this defaulted to false for backwards compatibility
	boolean threshold;

	@ParameterMarker(uiOrder=2, name="Number of Sigma", description="Number of sigma to determine subtraction value or value at which to threshold image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	double nSigma;

	@ParameterMarker(uiOrder=3, name="Single Threshold per Color?", description="Calculate a single threhsold for each color or a threshold for each image in data set. The combined thresh is calcualted as the median of the individual thresholds.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean threshPerColor;

	/////////// Define Outputs here ///////////

	// See Database.Definition.OutpuMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Thresholded Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputImageData;

	@OutputMarker(uiOrder=3, name="Stats", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputStatsData;

	@OutputMarker(uiOrder=4, name="Thresholds", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputThresholdData;
	
	int bitDepth = 8;

	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Collect the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		TreeMap<String,Object> output = null;
		if(threshPerColor)
		{
			output = this.calcPerColor();
		}
		else
		{
			output = this.calcIndividual();
		}

		if(output == null || !(Boolean) output.get("Success"))
		{
			return false;
		}

		outputImageData = ImageWriter.makeImageStackFromPaths("Image", (TreeMap<DimensionMap,String>) output.get("outputMap"));
		String valuePath2 = JEXTableWriter.writeTable("Stats", (TreeMap<DimensionMap,Double>) output.get("statsMap"));
		outputStatsData = FileWriter.makeFileObject("Stats", null, valuePath2);
		String valuePath3 = JEXTableWriter.writeTable("Thresholds", (TreeMap<DimensionMap,Double>) output.get("outputThreshMap"));
		outputThresholdData = FileWriter.makeFileObject("Thresholds", null, valuePath3);

		// Return status
		return true;
	}

	@SuppressWarnings("unchecked")
	public TreeMap<String,Object> calcPerColor()
	{
		Dim colorDim = imageData.getDimTable().getDimWithName(colorDimName);
		if(colorDim == null)
		{
			return this.calcIndividual();
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		DimTable table = imageData.getDimTable();

		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> statsMap = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> outputThreshMap = new TreeMap<DimensionMap,Double>();
		boolean success = true;
		TreeMap<String,Object> temp = null;
		for (DimTable subTable : table.getSubTableIterator(colorDimName))
		{
			temp = this.calcIndividual();
			Dim colorSubDim = subTable.getDimWithName(colorDimName);

			// Get the median threshold
			TreeMap<DimensionMap,Double> thresholds = (TreeMap<DimensionMap,Double>) temp.get("outputThreshMap");
			Double thresh = StatisticsUtility.median(thresholds.values());

			// Threshold all images for this subtable using the threshold
			for (DimensionMap map : subTable.getMapIterator())
			{
				// Get the image
				ImagePlus im = new ImagePlus(imageMap.get(map));
				FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();

				// Threshold it
				FunctionUtility.imThresh(ip, thresh, false);
				if(this.isCanceled())
				{
					success = false;
					return this.makeTreeMap("Success", false);
				}
				String path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, "false", 8)); // Creating black and white image
				outputMap.put(map.copy(), path);
			}

			// Combine rest of new data with current output data
			statsMap.putAll((TreeMap<DimensionMap,Double>) temp.get("statsMap"));
			outputThreshMap.put(new DimensionMap(colorSubDim.dimName + "=" + colorSubDim.valueAt(0)), thresh);
		}

		return this.makeTreeMap("Success,outputMap,statsMap,outputThreshMap", success, outputMap, statsMap, outputThreshMap);
	}

	public TreeMap<String,Object> calcIndividual()
	{
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> rois = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null && roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			rois = RoiReader.readObjectToRoiMap(roiData);
		}
		DimTable dimsToProcess = imageData.getDimTable();

		int percentage = 0;
		double count = 0, total = dimsToProcess.mapCount();
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> statsMap = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> outputThreshMap = new TreeMap<DimensionMap,Double>();
		boolean success = true;
		for (DimensionMap map : dimsToProcess.getMapIterator())
		{
			if(this.isCanceled())
			{
				return this.makeTreeMap("Success", false);
			}

			// Get the image
			ImagePlus im = new ImagePlus(imageMap.get(map));
			bitDepth = im.getBitDepth();
			FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();

			// Do threshold
			ROIPlus roi = rois.get(map);
			float[] tempPixels = null;
			if(roi != null)
			{
				tempPixels = ROIUtility.getPixelsInRoi(ip, roi);
			}
			if(tempPixels == null)
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
			double med = StatisticsUtility.median(pixels);
			if(this.isCanceled())
			{
				return this.makeTreeMap("Success", false);
			}
			double mad = StatisticsUtility.mad(med, pixels); // Multiplier converts the mad to an approximation of the standard deviation without the effects of outliers
			double threshold = med + nSigma * mad;
			String path = null;
			if(this.threshold)
			{
				FunctionUtility.imThresh(ip, threshold, false);
				if(this.isCanceled())
				{
					success = false;
					return this.makeTreeMap("Success", false);
				}
				path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, "false", 8)); // Creating black and white image
			}
			else
			{
				ip.subtract(threshold);
				if(this.isCanceled())
				{
					success = false;
					return this.makeTreeMap("Success", false);
				}
				path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, ""+false, bitDepth)); // Creating black and white image
			}

			if(path != null)
			{
				outputMap.put(map, path);
			}
			DimensionMap map2 = map.copy();
			map2.put("Measurement", "Median");
			statsMap.put(map2.copy(), med);
			map2.put("Measurement", "MAD");
			statsMap.put(map2.copy(), mad);
			outputThreshMap.put(map, threshold);

			// Update progress
			count = count + 1;
			percentage = (int) (100 * (count / total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		TreeMap<String,Object> ret = this.makeTreeMap("Success,outputMap,statsMap,outputThreshMap", success, outputMap, statsMap, outputThreshMap);
		return ret;
	}

	public TreeMap<String,Object> makeTreeMap(String csvString, Object... items)
	{
		CSVList names = new CSVList(csvString);
		if(names.size() != items.length)
		{
			return null;
		}
		TreeMap<String,Object> ret = new TreeMap<String,Object>();
		for (int i = 0; i < names.size(); i++)
		{
			ret.put(names.get(i), items[i]);
		}
		return ret;
	}
}
