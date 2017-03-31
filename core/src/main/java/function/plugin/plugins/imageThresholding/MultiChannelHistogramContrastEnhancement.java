package function.plugin.plugins.imageThresholding;

import java.util.TreeMap;
import java.util.Vector;

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
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.Pair;
import miscellaneous.StatisticsUtility;
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
		name="Histogram Contrast Enhancement",
		menuPath="Image Thresholding",
		visible=true,
		description="Adjust the intensities of each channel to be between a min and max intensity defined as a percentile of the pixels below those thresholds (e.g., between min=1% and max=99%)."
		)
public class MultiChannelHistogramContrastEnhancement extends JEXPlugin {

	public MultiChannelHistogramContrastEnhancement()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Min Percentile", description="Lower threshold or new 'Min Intensity (i.e., 0)' of the image defined as a percentile of the current intensities in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double minPercentile;

	@ParameterMarker(uiOrder=2, name="Max Percentile", description="Upper threshold or new 'Max Intensity (e.g., 255 for 8-bit)' of the image defined as a percentile of the current intensities in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100.0")
	double maxPercentile;
	
	@ParameterMarker(uiOrder=3, name="(n) Number of Images to estimate", description="The percentiles thresholds/intensities of n images will be calculated. The minimum low threshold and maximum high threshold are used on the rest of the images.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	int n;

	@ParameterMarker(uiOrder=4, name="Channel Dim Name (optional)", description="If a channel dimension name is provided, then a single threshold per channel used for all images in each channel based upon the first n images encountered in that channel.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String channelDimName;

	@ParameterMarker(uiOrder=5, name="Plot First (n) Histograms?", description="Should the first histogram and thresholds be plotted? (REQUIRES R / RServe to be up an running)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean plotFirst;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Contrasted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry entry)
	{
		// Validate the input data
		if(!JEXPlugin.isInputValid(imageData, JEXData.IMAGE))
		{
			return false;
		}

		// Check parameters
		if(minPercentile >= maxPercentile)
		{
			JEXDialog.messageDialog("The 'Min Percentile' must be less than the 'Max Percentile'", this);
			return false;
		}
		if(minPercentile < 0)
		{
			JEXDialog.messageDialog("The 'Min Percentile' must be >= 0", this);
			return false;
		}
		if(maxPercentile > 100)
		{
			JEXDialog.messageDialog("The 'Max Percentile' must be <= 100", this);
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;

		if(!this.channelDimName.equals(""))
		{
			if(this.imageData.getDimTable().getDimWithName(this.channelDimName) == null)
			{
				JEXDialog.messageDialog("The channel dimension named: " + this.channelDimName + " does not exist in this image object. Aborting.", this);
				return false;
			}
			else
			{
				for(DimTable t : this.imageData.getDimTable().getSubTableIterator(this.channelDimName))
				{
					Pair<Double, Double> thresholds = null;
					Vector<Double> mins = new Vector<>();
					Vector<Double> maxs = new Vector<>();
					int i = 0;
					for(DimensionMap map : t.getMapIterator())
					{
						// Check if canceled
						if(this.isCanceled())
						{
							return false;
						}
						
						// Get the image
						ImagePlus im = new ImagePlus(imageMap.get(map));
						FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
						
						// Get the thresholds
						thresholds = this.getThresholds(im, imp, this.plotFirst, map);
						mins.add(thresholds.p1);
						maxs.add(thresholds.p2);
						
						// Reclaim memory
						im.flush();
						imp = null;
						
						// increment loop counter.
						i = i + 1;
						
						// break if possible
						if(i >= this.n)
						{
							break;
						}
					}
					
					Double minThresh = StatisticsUtility.min(mins);
					Double maxThresh = StatisticsUtility.max(maxs);
					
					// Apply the thresholds throughout.
					for(DimensionMap map : t.getMapIterator())
					{
						// Check if canceled
						if(this.isCanceled())
						{
							return false;
						}
						
						// Get the image
						ImagePlus im = new ImagePlus(imageMap.get(map));
						FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
												
						// Save the contrasted image
						tempPath = this.getAdjustedImage(im, imp, new Pair<Double,Double>(minThresh, maxThresh));
						if(tempPath != null)
						{
							outputImageMap.put(map, tempPath);
						}
						
						// Reclaim memory
						im.flush();
						imp = null;
						
						// Update the progress bar
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
						JEXStatics.statusBar.setProgressPercentage(percentage);
					}
				}
			}
		}
		else
		{
			Pair<Double,Double> thresholds = null;
			Vector<Double> mins = new Vector<>();
			Vector<Double> maxs = new Vector<>();
			int i = 0;
			for (DimensionMap map : imageMap.keySet())
			{
				// Check if canceled
				if(this.isCanceled())
				{
					return false;
				}
				
				// Get the image
				ImagePlus im = new ImagePlus(imageMap.get(map));
				FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();

				// Get the thresholds
				thresholds = this.getThresholds(im, imp, this.plotFirst, map);
				if(thresholds == null)
				{
					Logs.log("Couldn't determine a threshold for image: " + map.toString() + " - " + imageMap.get(map) + ". Skipping.", this);
					i = i + 1;
					continue;
				}
				mins.add(thresholds.p1);
				maxs.add(thresholds.p2);
				
				// Reclaim memory
				im.flush();
				imp = null;
				
				// Increment counter
				i = i + 1;
				
				// Break if possible
				if(i >= this.n)
				{
					break;
				}
			}
			
			Double minThresh = StatisticsUtility.min(mins);
			Double maxThresh = StatisticsUtility.max(maxs);
			
			// Apply thresholds throughout
			for (DimensionMap map : imageMap.keySet())
			{
				// Check if canceled
				if(this.isCanceled())
				{
					return false;
				}
				
				// Get the image
				ImagePlus im = new ImagePlus(imageMap.get(map));
				FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
				
				// Save the contrasted image
				tempPath = this.getAdjustedImage(im, imp, new Pair<Double,Double>(minThresh, maxThresh));
				if(tempPath != null)
				{
					outputImageMap.put(map, tempPath);
				}
				
				// Reclaim memory
				im.flush();
				imp = null;
				
				// Update the progress bar
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}

		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

		// Return status
		return true;	
	}

	public Pair<Double, Double> getThresholds(ImagePlus im, FloatProcessor imp, boolean plot, DimensionMap map)
	{
		Pair<Double,Double> thresholds = null;
		// Get the thresholds
		if(im.getBitDepth() < 32)
		{
			thresholds = ImageUtility.getHistogramQuantileThresholds(imp, 0, (int) Math.pow(2, im.getBitDepth()), (int) Math.pow(2, im.getBitDepth()), minPercentile, maxPercentile, plot, map.toString());
		}
		else
		{
			ImageStatistics stats = im.getStatistics(ImageStatistics.MIN_MAX);
			thresholds = ImageUtility.getHistogramQuantileThresholds(imp, stats.min, stats.max, (int) Math.pow(2, 16), minPercentile, maxPercentile, plot, map.toString());
		}

		if(thresholds == null)
		{
			JEXDialog.messageDialog("Couldn't determine the threshold intensities associated with the given percentiles. Aborting.", this);
			return null;
		}
		
		return thresholds;
	}

	public String getAdjustedImage(ImagePlus im, FloatProcessor imp, Pair<Double,Double> thresholds)
	{
		// Call helper method
		String tempPath = null;
		if(im.getBitDepth() < 32)
		{
			tempPath = this.saveAdjustedImage(imp, thresholds.p1, thresholds.p2, 0, (int) Math.pow(2, im.getBitDepth())-1, 1.0, im.getBitDepth());
		}
		else
		{
			tempPath = this.saveAdjustedImage(imp, thresholds.p1, thresholds.p2, 0, 1, 1.0, im.getBitDepth());
		}

		return tempPath;
	}

	public String saveAdjustedImage(FloatProcessor imp, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);

		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		toSave.flush();

		// return the filepath
		return imPath;
	}
}
