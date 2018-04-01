// Define package name as "plugins" as show here
package function.plugin.plugins.imageThresholding;

import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

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
import function.plugin.plugins.featureExtraction.FeatureUtils;
// Import needed classes here 
import ij.ImagePlus;
import ij.process.FloatProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ImageUtility;
import miscellaneous.CSVList;
import miscellaneous.Pair;
import miscellaneous.StatisticsUtility;
import miscellaneous.StringUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

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

	@ParameterMarker(uiOrder=1, name="Channel Dim Name", description="Name of the dimension representing the different channels of the image set.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String colorDimName;

	@ParameterMarker(uiOrder=2, name="Threshold? (vs. Subtract)", description="Whether to threshold or subtract based on the pixel values in the roi region (or whole image if no roi provided)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true) // Should keep this defaulted to false for backwards compatibility
	boolean threshold;

	@ParameterMarker(uiOrder=3, name="Number of Sigma", description="Number of sigma to determine subtraction value or value at which to threshold image. (Comma separated list results in outputs for each value and negative values (-x) result in keeping values below -x sigma)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	String nSigma;

	@ParameterMarker(uiOrder=4, name="Number of Pixels Sample", description="How many pixels should be sampled? Typically after more than 300, the result doesn't change much. Use 0 or less to sample all", ui=MarkerConstants.UI_TEXTFIELD, defaultText="300")
	int numToSample;
	
	@ParameterMarker(uiOrder=4, name="R: Plot Sample Histogram?", description="Requires R. Should the samples be plotted in a histogram and saved for viewing?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean histogram;

	@ParameterMarker(uiOrder=5, name="Refinement Algorithm?", description="Should the background level be refined by the 'Adaptive' algorithm (iteratively weighting the pixels by their proximity to the current median value) or the 'Variance Weighted' algorithm (use a variance filtered version of image to weight pixels to determin median)?", ui=MarkerConstants.UI_DROPDOWN, choices={"None","Adaptive","Variance Weighted","Mode Weighted"}, defaultChoice=0)
	String algorithm;
	
	@ParameterMarker(uiOrder=6, name="Variance Filter Radius", description="If 'Variance Weighted' algorithm is being used, what radius should the variance filter be set to. (typically ~2.0)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2.0")
	double varianceFilterRadius;

	@ParameterMarker(uiOrder=7, name="Single Threshold per Color?", description="Calculate a single threhsold for each color or a threshold for each image in data set. The combined thresh is calcualted as the median of the individual thresholds.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean threshPerColor;

	@ParameterMarker(uiOrder=8, name="Offset", description="Amount to add back to the image before saving. Useful for avoiding clipping of lows, essentially setting the background to a known non-zero offset.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double offset;

	@ParameterMarker(uiOrder=8, name="Evaluate Outside ROI?", description="If an ROI is specified, then indicate whether to use pixels inside or outside the ROI to determine background intensities. (checked uses pixels outside ROI)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean outside;

	@ParameterMarker(uiOrder=10, name="Exclusion Filter", description="<DimName>=<Val1>,<Val2>,...<Valn>, Specify the dimension and dimension values to exclude. Leave blank to process all. Useful for excluding bright-field (e.g., Channel=BF). Technically doesn't have to be the 'Channel' dimension.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String toExclude;

	@ParameterMarker(uiOrder=11, name="Keep Unprocessed Images?", description="Should the images within the object that are exlcluded from analysis by the Dimension Filter be kept in the result?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean keepUnprocessed;

	/////////// Define Outputs here ///////////

	// See Database.Definition.OutpuMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Thresholded Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputImageData;

	@OutputMarker(uiOrder=3, name="Stats", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputStatsData;

	@OutputMarker(uiOrder=4, name="Thresholds", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputThresholdData;
	
	@OutputMarker(uiOrder=4, name="Histograms (R)", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData histograms;

	int bitDepth = 8;
	List<Double> nSigmas = null;

	DimTable filterTable = null;

	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	FeatureUtils utils = new FeatureUtils();

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		getDoubles(); // translates the string nSigma into a list of sigmas as doubles
		if(nSigmas.size() > 1 && threshPerColor)
		{
			JEXDialog.messageDialog("The ability to use multiple sigma values and the one threshold per color option are incompatabile. Choose to do one or the other. Aborting.", this);
			return false;
		}

		// Collect the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		this.filterTable = new DimTable(toExclude);

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
		if(output.get("histMap") != null)
		{
			this.histograms = ImageWriter.makeImageStackFromPaths("Histograms", (TreeMap<DimensionMap,String>) output.get("histMap"));
		}
		
		// Return status
		return true;
	}

	@SuppressWarnings("unchecked")
	public TreeMap<String,Object> calcPerColor()
	{
		TreeMap<String,Object> temp = this.calcIndividual();

		Dim colorDim = imageData.getDimTable().getDimWithName(colorDimName);
		if(colorDim == null)
		{
			return temp;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		DimTable table = imageData.getDimTable();

		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> statsMap = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> outputThreshMap = new TreeMap<DimensionMap,Double>();
		boolean success = true;
		TreeMap<DimensionMap,Double> thresholds = (TreeMap<DimensionMap,Double>) temp.get("outputThreshMap");
		for (DimTable subTable : table.getSubTableIterator(colorDimName))
		{

			Dim colorSubDim = subTable.getDimWithName(colorDimName);

			// Get the median threshold
			TreeMap<DimensionMap,Double> colorThresholds = new TreeMap<DimensionMap,Double>();
			for(DimensionMap map : subTable.getMapIterator())
			{
				Double d = thresholds.get(map);
				if(d != null && d != Double.NaN)
				{
					colorThresholds.put(map, thresholds.get(map));
				}
			}
			Double thresh = StatisticsUtility.median(colorThresholds.values());

			// Threshold all images for this subtable using the threshold
			for (DimensionMap map : subTable.getMapIterator())
			{
				// Get the image
				String imPath = imageMap.get(map);
				if(imPath == null)
				{
					continue;
				}
				ImagePlus im = new ImagePlus(imageMap.get(map));
				FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();

				String path = null;
				if(this.threshold && !filterTable.testMapAsExclusionFilter(map))
				{
					FunctionUtility.imThresh(ip, thresh, false);
					if(this.isCanceled())
					{
						success = false;
						return this.makeTreeMap("Success", false);
					}
					path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, "false", 8)); // Creating black and white image
				}
				else if(!filterTable.testMapAsExclusionFilter(map))
				{
					ip.add(offset);
					ip.subtract(thresh);
					if(this.isCanceled())
					{
						success = false;
						return this.makeTreeMap("Success", false);
					}
					path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, ""+false, this.bitDepth)); // Creating black and white image
				}
				else if(keepUnprocessed)
				{
					path = JEXWriter.saveImage(im);
				}
				if(path != null)
				{
					outputMap.put(map, path);
				}
			}

			// Combine rest of new data with current output data
			statsMap.putAll((TreeMap<DimensionMap,Double>) temp.get("statsMap"));
			outputThreshMap.put(new DimensionMap(colorSubDim.dimName + "=" + colorSubDim.valueAt(0)), thresh);
		}

		return this.makeTreeMap("Success,outputMap,statsMap,outputThreshMap,histMap", success, outputMap, statsMap, outputThreshMap, temp.get("histMap"));
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
		TreeMap<DimensionMap,String> histMap = new TreeMap<DimensionMap,String>();
		boolean success = true;
		for (DimensionMap map : dimsToProcess.getMapIterator())
		{
			for(Double sigma : nSigmas)
			{
				if(this.isCanceled())
				{
					return this.makeTreeMap("Success", false);
				}

				Double med = Double.NaN, mad = Double.NaN, threshold = Double.NaN, n = Double.NaN;

				// Get the image
				String imPath = imageMap.get(map);
				if(imPath == null)
				{
					continue;
				}
				ImagePlus im = new ImagePlus(imageMap.get(map));
				bitDepth = im.getBitDepth();

				if(filterTable.testMapAsExclusionFilter(map))
				{
					if(keepUnprocessed)
					{
						String path = JEXWriter.saveImage(im);
						outputMap.put(map.copy(), path);
					}
				}
				else
				{
					if(this.isCanceled())
					{
						return this.makeTreeMap("Success", false);
					}
					
					FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();

					// Get the appropriate pixels for calculating the median and get the med, mad, and n values
					ROIPlus roi = rois.get(map);
					if(this.algorithm.equals("Variance Weighted") || this.algorithm.equals("Mode Weighted"))
					{
						// THEN 'VARIANCE WEIGHTED' ALGORITHM
						
						// Get the 'weights' version of the image.
						FloatProcessor weights = null;
						if(this.algorithm.equals("Variance Weighted"))
						{
							// This is for 'Variance Weighted'
							Pair<FloatProcessor[], FloatProcessor> temp = ImageUtility.getImageVarianceWeights(ip, this.varianceFilterRadius, false, false, 2.0);
							weights = temp.p1[0];
							//FileUtility.showImg(weights.p1, true);
						}
						else
						{
							// This is for 'Mode Weighted'
							weights = ImageUtility.getImageModeWeights(ip, false, 2.0)[0];
						}
						
						// Sample the same pixels from the test image and the weights image according to the provided roi.
						Pair<Vector<Double>, Vector<Double>> samples = null;
						if(this.numToSample <= 0)
						{
							samples = this.utils.sampleTwoImagesFromShape(ip, weights, roi, !outside);
						}
						else
						{
							samples = this.utils.hexagonallySampleNFromTwoImages(ip, weights, roi, !outside, this.numToSample);
						}
						
						//FileUtility.showImg(weights, true);
						
						med = StatisticsUtility.weightedMedian(samples.p1, samples.p2);
						mad = StatisticsUtility.weightedMad(samples.p1, samples.p2, med);
						n = (double) samples.p1.size();
						
						//double med2 = StatisticsUtility.median(samples.p1);
						//double mad2 = StatisticsUtility.mad(med, samples.p1);
						Pair<double[], int[]> hist2 = ImageUtility.getHistogram(samples.p1, StatisticsUtility.min(samples.p1), StatisticsUtility.max(samples.p1), -1, true);
						if(this.histogram)
						{
							String histPath = ImageUtility.getHistogramPlot(hist2.p1, hist2.p2, false, "tif", 0d, med + sigma*mad);
							histMap.put(map, histPath);
						}
					}
					else
					{
						// ELSE IT IS EITHER 'NONE' OR 'ADAPTIVE'
						
						// Sample the pixels according to the provided roi.
						Vector<Double> samples = null;
						if(this.numToSample <= 0)
						{
							samples = this.utils.sampleFromShape(ip, roi, !outside);
						}
						else
						{
							samples = this.utils.hexagonallySampleN(ip, roi, !outside, this.numToSample);
						}
						
						// Get the median of the sampled pixels
						if(algorithm.equals("Adaptive"))
						{
							med = StatisticsUtility.adaptiveMedian(samples);
						}
						else
						{
							med = StatisticsUtility.median(samples);
						}
						
						// Get the corresponding mad for the median
						mad = StatisticsUtility.mad(med, samples); // Multiplier converts the mad to an approximation of the standard deviation without the effects of outliers
						n = (double) samples.size();
						
						// Debug code.
						Pair<double[], int[]> hist2 = ImageUtility.getHistogram(samples, StatisticsUtility.min(samples), StatisticsUtility.max(samples), -1, true);
						if(this.histogram)
						{
							String histPath = ImageUtility.getHistogramPlot(hist2.p1, hist2.p2, false, "tif", med, med+sigma*mad);
							histMap.put(map, histPath);
						}
					}
					
					// USE THIS OPPURTUNITY
					if(this.isCanceled())
					{
						return this.makeTreeMap("Success", false);
					}
					
					// USE THE INFORMATION TO PERFORM THRESHOLDING / SUBTRACTION
					threshold = med + sigma * mad;
					String path = null;
					if(!threshPerColor)
					{
						if(this.threshold)
						{
							FunctionUtility.imThresh(ip, threshold, false);
							if(this.isCanceled())
							{
								success = false;
								return this.makeTreeMap("Success", false);
							}
							if(sigma < 0)
							{
								path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, false, 1, 8, true)); // Creating black and white image
							}
							else
							{
								path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, false, 1, 8, false)); // Creating black and white image
							}
						}
						else
						{
							ip.add(offset);
							ip.subtract(threshold);
							if(this.isCanceled())
							{
								success = false;
								return this.makeTreeMap("Success", false);
							}
							path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, ""+false, this.bitDepth)); // Creating black and white image
						}
						if(path != null)
						{
							if(nSigmas.size() == 1)
							{
								outputMap.put(map.copy(), path);
							}
							else
							{
								DimensionMap toSave = map.copy();
								toSave.put(colorDimName, map.get(colorDimName) + "_" + sigma);
								outputMap.put(toSave, path);
							}
						}
					}
				}

				if(nSigmas.size() == 1)
				{
					DimensionMap map2 = map.copy();
					map2.put("Measurement", "Median");
					statsMap.put(map2.copy(), med);
					map2.put("Measurement", "MAD");
					statsMap.put(map2.copy(), mad);
					map2.put("Measurement", "n");
					statsMap.put(map2.copy(), n);
					outputThreshMap.put(map, threshold);
				}
				else
				{
					DimensionMap map2 = map.copy();
					map2.put("Measurement", "Median_" + sigma);
					statsMap.put(map2.copy(), med);
					map2.put("Measurement", "MAD_" + sigma);
					statsMap.put(map2.copy(), mad);
					map2.put("Measurement", "n_" + sigma);
					statsMap.put(map2.copy(), n);

					DimensionMap toSave = map.copy();
					toSave.put(colorDimName, map.get(colorDimName) + "_" + sigma);
					outputThreshMap.put(toSave, threshold);
				}

				// Update progress
				count = count + 1;
				percentage = (int) (100 * (count / total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}

		TreeMap<String,Object> ret = this.makeTreeMap("Success,outputMap,statsMap,outputThreshMap,histMap", success, outputMap, statsMap, outputThreshMap, histMap);
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

	private void getDoubles()
	{
		CSVList temp = new CSVList(nSigma);
		Vector<Double> ret = new Vector<>();

		for(String num : temp)
		{
			ret.add(new Double(StringUtility.removeWhiteSpaceOnEnds(num)));
		}
		nSigmas = ret;
	}
}
