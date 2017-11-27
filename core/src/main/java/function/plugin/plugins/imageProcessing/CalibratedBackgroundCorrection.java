package function.plugin.plugins.imageProcessing;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.RankFilters;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ImageUtility;
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
		name="Calibrated Background Correction",
		menuPath="Image Processing",
		visible=true,
		description="Subtract background of image and correct for uneven illumination using calibration images."
		)
public class CalibratedBackgroundCorrection extends JEXPlugin {

	public CalibratedBackgroundCorrection()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="DF Image", type=MarkerConstants.TYPE_IMAGE, description="Darkfield image.", optional=false)
	JEXData darkData;

	@InputMarker(uiOrder=2, name="IF Image", type=MarkerConstants.TYPE_IMAGE, description="Illumination-field image.", optional=false)
	JEXData illumData;

	@InputMarker(uiOrder=3, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Images to be adjusted.", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=4, name="Optional Crop ROI", type=MarkerConstants.TYPE_IMAGE, description="Optional roi to use for cropping during processing.", optional=true)
	JEXData roiData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="IF-DF Radius", description="Radius of mean for smoothing illumination correction image", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	double IFDFRadius;

	@ParameterMarker(uiOrder=2, name="Image-DF Radius", description="Radius of median filter for smoothing dark field corrected experimental image (if no DF provided, it WILL smooth the image but not subtract any DF image from the image)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3.0")
	double imageDFRadius;

	@ParameterMarker(uiOrder=3, name="Est. BG sigma", description="Estimated noise in the background signal (i.e., mu +/- sigma). Set to 0 or less to avoid this step.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100.0")
	double sigma;

	@ParameterMarker(uiOrder=4, name="BG Sub. Presmooth Radius", description="Radius of mean filter to apply temporarily for background subtraction (only affects rolling ball subtraction, not applied directly to original image)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	double bgPresmoothRadius;
	
	@ParameterMarker(uiOrder=5, name="BG Sub. Radius", description="Kernal radius in pixels (e.g. 3.8)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="150.0")
	double bgRadius;

	//@ParameterMarker(uiOrder=1, name="BF Sub.", description="", ui=MarkerConstants.UI_TEXTFIELD, defaultBoolean=true)
	boolean bgInverse = false;

	@ParameterMarker(uiOrder=6, name="BG Sub. Parabaloid?", description="Should the sliding paraboid algorithm be used instead of rolling ball? (typically yes)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean bgParaboloid = true;

	@ParameterMarker(uiOrder=7, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=1)
	int outputDepth;

	@ParameterMarker(uiOrder=8, name="Nominal Value to Add Back", description="Nominal value to add back to the image so that we don't clip values below zero", ui=MarkerConstants.UI_TEXTFIELD, defaultText="500.0")
	double nominal;

	@ParameterMarker(uiOrder=9, name="Exclusion Filter DimTable (optional)", description="Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String filterDimTableString;

	@ParameterMarker(uiOrder=10, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Images (BG)", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant background corrected images", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Collect the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		FloatProcessor darkImp = null;
		if(darkData != null && darkData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			ImagePlus darkIm = ImageReader.readObjectToImagePlus(darkData);
			if(darkIm == null)
			{
				return false;
			}
			darkImp = (FloatProcessor) darkIm.getProcessor().convertToFloat();
		}

		FloatProcessor illumImp = null;
		if(illumData != null && illumData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			ImagePlus illumIm = ImageReader.readObjectToImagePlus(illumData);
			if(illumIm == null)
			{
				return false;
			}
			illumImp = (FloatProcessor) illumIm.getProcessor().convertToFloat();
		}

		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null && roiData.getDataObjectType().equals(JEXData.ROI))
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
		}
		
		DimTable filterTable = new DimTable(this.filterDimTableString);

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();

		// //// Create a reference to a Blitter for float operations
		FloatBlitter blit = null;

		if(illumImp != null)
		{
			// //// Prepare the illumination Field Image

			// //// Subtract dark field from illumination field
			blit = new FloatBlitter(illumImp);
			if(darkImp != null)
			{
				blit.copyBits(darkImp, 0, 0, FloatBlitter.SUBTRACT);
			}

			if(IFDFRadius > 0)
			{
				// //// Smooth the result
				ImagePlus IC = new ImagePlus("IC", illumImp);
				RankFilters rF = new RankFilters();
				rF.rank(illumImp, IFDFRadius, RankFilters.MEAN);
				IC.flush();
				IC = null;
			}

			// Calculate the mean of the illumination field correction for back
			// multiplication
			FloatStatistics illumStats = new FloatStatistics(illumImp, FloatStatistics.MEAN, null);
			double illumMean = illumStats.mean;
			illumStats = null;
			illumImp.multiply(1 / illumMean); // Normalized IllumImp so we don't
			// have to multiply back up all
			// other images
		}

		int count = 0;
		int percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			FloatProcessor croppedIllumImp = illumImp;
			FloatProcessor croppedDarkImp = darkImp;
			if(this.isCanceled())
			{
				return false;
			}
			String path = imageMap.get(map);

			// If the image is to be excluded, then skip (saving original if necessary)
			if(filterTable.testDimensionMapUsingDimTableAsFilter(map))
			{
				// Then the image fits the exclusion filter
				if(keepExcluded)
				{
					Logs.log("Skipping the processing of " + map.toString(), this);
					ImagePlus out = new ImagePlus(imageMap.get(map));
					tempPath = JEXWriter.saveImage(out); // Don't convert the bitDepth of this output since it is "not be in processed".
					if(tempPath != null)
					{
						outputMap.put(map, tempPath);
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
			// Else, proceed to process the image

			Logs.log("Calibrating image for " + map.toString(), this);

			// /// Get the image
			ImagePlus im = new ImagePlus(path);
			FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor

			// //// crop if desired
			ROIPlus cropRoi = roiMap.get(map);
			if(cropRoi != null)
			{
				imp.setRoi(cropRoi.getRoi());
				imp = (FloatProcessor) imp.crop();

				if(croppedIllumImp != null)
				{
					croppedIllumImp.setRoi(cropRoi.getRoi());
					croppedIllumImp = (FloatProcessor) croppedIllumImp.crop();
				}
				if(croppedDarkImp != null)
				{
					croppedDarkImp.setRoi(cropRoi.getRoi());
					croppedDarkImp = (FloatProcessor) croppedDarkImp.crop();
				}
			}

			// //// First calculate (Image-DF)
			blit = new FloatBlitter(imp);
			if(darkImp != null)
			{
				blit.copyBits(darkImp, 0, 0, FloatBlitter.SUBTRACT);
			}

			if(imageDFRadius > 0)
			{
				// //// Smooth (Image-DF)
				RankFilters rF = new RankFilters();
				rF.rank(imp, imageDFRadius, RankFilters.MEDIAN);
			}

			// //// Subtract the background from the filtered (Image-DF)
			if(bgRadius > 0)
			{
				if(bgPresmoothRadius > 0)
				{
					FloatProcessor impTemp = new FloatProcessor(imp.getFloatArray());
					RankFilters rF = new RankFilters();
					rF.rank(impTemp, bgPresmoothRadius, RankFilters.MEAN);

					BackgroundSubtracter bS = new BackgroundSubtracter();			
					bS.rollingBallBackground(impTemp, bgRadius, true, bgInverse, bgParaboloid, false, true);

					// subtract the calculated background from the image
					blit.copyBits(impTemp, 0, 0, FloatBlitter.SUBTRACT);

					// Release memory of impTemp
					impTemp = null;
				}
				else
				{
					BackgroundSubtracter bS = new BackgroundSubtracter();			
					bS.rollingBallBackground(imp, bgRadius, false, bgInverse, bgParaboloid, false, true);
				}
			}


			// //// Subtract off remaining background because subtraction method
			// //subtracts off the MINIMUM of the background, we want the mean of
			// //the background to be zero
			if(sigma > 0)
			{
				double remainderMean = ImageUtility.getHistogramPeakBin(imp, -sigma, sigma, -1, false);
				// try
				// {
				// FileUtility.openFileDefaultApplication(JEXWriter.saveImage(imp));
				// }
				// catch (Exception e)
				// {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

				// Subtract off the remainder of the background before division with
				// the illumination field
				imp.add(-1 * remainderMean);
			}

			if(croppedIllumImp != null)
			{
				// //// Then divide by IllumImp
				blit.copyBits(croppedIllumImp, 0, 0, FloatBlitter.DIVIDE);
			}

			// //// Add back a nominal amount to avoid clipping negative values
			// upon conversion to 16-bit
			imp.add(nominal);

			// //// reset the display min and max
			imp.resetMinAndMax();

			ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", outputDepth);
			String finalPath1 = JEXWriter.saveImage(toSave);

			outputMap.put(map.copy(), finalPath1);
			Logs.log("Finished processing " + count + " of " + imageMap.size() + ".", 1, this);
			count++;

			// Status bar
			percentage = (int) (100 * ((double) count / (double) imageMap.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		// Set the outputs
		this.output = ImageWriter.makeImageStackFromPaths("temp", outputMap);
		output.setDataObjectInfo("Background subtracted using background subtraction function");

		// Return status
		return true;
	}

}
