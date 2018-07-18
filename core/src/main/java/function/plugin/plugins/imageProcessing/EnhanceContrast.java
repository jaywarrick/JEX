package function.plugin.plugins.imageProcessing;

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
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
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
		name="Enhance Contrast",
		menuPath="Image Processing",
		visible=true,
		description="Adjust the intensities of each channel to be between a min and max intensity defined as a percentile of the pixels below those thresholds (e.g., between min=1% and max=99%)."
		)
public class EnhanceContrast extends JEXPlugin {

	public EnhanceContrast()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Min Percentile", description="Lower threshold (e.g., 0.5%)' of the image defined as a percentile of the current intensities in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double minPercentile;

	@ParameterMarker(uiOrder=2, name="Max Percentile", description="Upper threshold (e.g., 99%)' of the image defined as a percentile of the current intensities in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="99.0")
	double maxPercentile;
	
	@ParameterMarker(uiOrder=3, name="Gamma (post)", description="Gamma adjustment applied post contrast adjustment.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double gamma;

	@ParameterMarker(uiOrder=4, name="Method", description="Should thresholds be determined for each individual image or by inidivual channel or one threshold for all?", ui=MarkerConstants.UI_DROPDOWN, choices={"per Image","per Channel","Uniform"}, defaultChoice=0)
	String method;

	@ParameterMarker(uiOrder=5, name="Channel Dim Name", description="If creating a single threshold per channel, what is the name of the channel dimension?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;

	@ParameterMarker(uiOrder=6, name="(n) Number of Images to Sample", description="For large datasets, one can just analyze the first 'n' images to limit calculation time and just use threshold limits based on some of the images. A value < 1 results in use of all images. The per Image method ignores this setting.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	int n;
	
	@ParameterMarker(uiOrder=7, name="Pixel Sample Size", description="For large images, a subset of the pixels can be used to estimate percentiles to speed calculation. Sample size < 1 results in sampling of the entire image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="-1")
	int sampleSize;
	
	@ParameterMarker(uiOrder=8, name="Output Bit Depth", description="What bit depth should the image be saved as, scaling the image to fill the bit depth range?", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=0)
	int outputBitDepth;
	
	@ParameterMarker(uiOrder=9, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;
	
	@ParameterMarker(uiOrder=10, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	//	@ParameterMarker(uiOrder=5, name="Plot First (n) Histograms?", description="Should the first histogram and thresholds be plotted? (REQUIRES R / RServe to be up an running)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	//	boolean plotFirst;

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
		//		if(minPercentile >= maxPercentile)
		//		{
		//			JEXDialog.messageDialog("The 'Min Percentile' must be less than the 'Max Percentile'", this);
		//			return false;
		//		}
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

		DimTable filterTable = new DimTable(this.filterDimTableString);
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		
		if(this.method.equals("per Channel"))
		{
			if(this.channelDimName.equals("") || this.imageData.getDimTable().getDimWithName(this.channelDimName) == null)
			{
				JEXDialog.messageDialog("The channel dimension named: '" + this.channelDimName + "' does not exist in this image object. Aborting.", this);
				return false;
			}
			else
			{
				// Then do per Channel
				for(DimTable t : this.imageData.getDimTable().getSubTableIterator(this.channelDimName))
				{
					Pair<Double, Double> thresholds = null;
					Vector<Double> mins = new Vector<>();
					Vector<Double> maxs = new Vector<>();
					int i = 0;
					for(DimensionMap map : t.getMapIterator())
					{
						if(filterTable.testMapAsExclusionFilter(map))
						{
							continue;
						}
						
						// Check if canceled
						if(this.isCanceled())
						{
							return false;
						}

						// Get the image
						String path = imageMap.get(map);
						if(path == null)
						{
							i = i + 1;
							continue;
						}
						ImageProcessor imp = (new ImagePlus(path)).getProcessor();
						// Get the thresholds
						thresholds = ImageUtility.percentile(imp, this.minPercentile, this.maxPercentile, this.sampleSize);
						
						if(thresholds == null)
						{
							Logs.log("Couldn't determine a threshold for image: " + map.toString() + " - " + imageMap.get(map) + ". Skipping.", this);
							i = i + 1;
							continue;
						}
						mins.add(thresholds.p1);
						maxs.add(thresholds.p2);

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
							}
							else
							{
								Logs.log("Skipping the processing and saving of " + map.toString(), this);
							}
							// Update the progress bar
							count = count + 1;
							percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
							JEXStatics.statusBar.setProgressPercentage(percentage);
							continue;
						}
						
						// Check if canceled
						if(this.isCanceled())
						{
							return false;
						}

						// Get the image
						ImagePlus im = new ImagePlus(imageMap.get(map));
						ImageProcessor imp = im.getProcessor();

						// Save the contrasted image
						tempPath = this.getAdjustedImage(imp, new Pair<Double,Double>(minThresh, maxThresh), this.gamma);
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
		else if(this.method.equals("per Image"))
		{
			// Then apply a single min/max threshold for all the images.
			Pair<Double,Double> thresholds = null;
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

				// Get the image
				String path = imageMap.get(map);
				if(path == null)
				{
					continue;
				}

				// Get the image
				ImageProcessor imp = (new ImagePlus(path)).getProcessor();
				
				// Get the thresholds
				thresholds = ImageUtility.percentile(imp, this.minPercentile, this.maxPercentile, this.sampleSize);
				if(thresholds == null)
				{
					Logs.log("Couldn't determine a threshold for image: " + map.toString() + " - " + imageMap.get(map) + ". Skipping.", this);
					// Update the progress bar
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
					JEXStatics.statusBar.setProgressPercentage(percentage);
					continue;
				}
				
				// Save the contrasted image
				tempPath = this.getAdjustedImage(imp, thresholds, this.gamma);//im.getBitDepth());
				if(tempPath != null)
				{
					outputImageMap.put(map, tempPath);
				}

				// Reclaim memory
				imp = null;

				// Update the progress bar
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				
			}
		}
		else
		{
			// Then apply a single min/max threshold for all the images.
			Pair<Double,Double> thresholds = null;
			Vector<Double> mins = new Vector<>();
			Vector<Double> maxs = new Vector<>();
			int i = 0;
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
					}
					else
					{
						Logs.log("Skipping the processing and saving of " + map.toString(), this);
					}
					continue;
				}
				
				// Check if canceled
				if(this.isCanceled())
				{
					return false;
				}

				// Get the image
				String path = imageMap.get(map);
				if(path == null)
				{
					continue;
				}

				// Get the thresholds
				thresholds = ImageUtility.percentile((new ImagePlus(path)).getProcessor(), this.minPercentile, this.maxPercentile, this.sampleSize);
				if(thresholds == null)
				{
					Logs.log("Couldn't determine a threshold for image: " + map.toString() + " - " + imageMap.get(map) + ". Skipping.", this);
					continue;
				}
				mins.add(thresholds.p1);
				maxs.add(thresholds.p2);

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
				String path = imageMap.get(map);
				if(path == null)
				{
					count = count + 1;
					continue;
				}
				ImageProcessor imp = (new ImagePlus(imageMap.get(map))).getProcessor();

				// Save the contrasted image
				tempPath = this.getAdjustedImage(imp, new Pair<Double,Double>(minThresh, maxThresh), this.gamma);
				if(tempPath != null)
				{
					outputImageMap.put(map, tempPath);
				}

				// Reclaim memory
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

	public String getAdjustedImage(ImageProcessor imp, Pair<Double,Double> thresholds, double gamma)
	{
		// Call helper method
		String tempPath = null;
		if(this.outputBitDepth < 32)
		{
			tempPath = saveAdjustedImage(imp, thresholds.p1, thresholds.p2, 0.0, Math.pow(2, this.outputBitDepth)-1, gamma, this.outputBitDepth);
		}
		else
		{
			tempPath = saveAdjustedImage(imp, thresholds.p1, thresholds.p2, 0.0, 1.0, gamma, this.outputBitDepth);
		}

		return tempPath;
	}

	//	public String saveAdjustedImage(FloatProcessor imp, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	//	{
	//		// Adjust the image
	//		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//
	//		// Save the results
	//		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
	//		String imPath = JEXWriter.saveImage(toSave);
	//		toSave.flush();
	//
	//		// return the filepath
	//		return imPath;
	//	}
}
