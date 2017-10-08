package function.plugin.old;

import java.util.HashMap;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
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
import tables.DimensionMap;

/**
 * Subtract background of image and correct for uneven illumination using
 * calibration images.
 * 
 * @author erwinberthier
 * 
 */
public class JEX_SingleCell_BackGroundCorrectCalibrated extends JEXCrunchable {

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------

	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName()
	{
		String result = "Background Correct (Calibrated)";
		return result;
	}

	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		String result = "Subtract background of image and correct for uneven illumination using calibration images";
		return result;
	}

	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		String toolbox = "Single Cell Analysis";
		return toolbox;
	}

	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList()
	{
		return true;
	}

	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return true;
	}

	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------

	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[4];
		inputNames[0] = new TypeName(IMAGE, "DF Image");
		inputNames[1] = new TypeName(IMAGE, "IF Image");
		inputNames[2] = new TypeName(IMAGE, "Images");
		inputNames[3] = new TypeName(ROI, "Optional Crop ROI");
		return inputNames;
	}

	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Background Corrected");

		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
	}

	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters()
	{
		Parameter pa0 = getNumThreadsParameter(10, 6);
		Parameter pa1 = new Parameter("IF-DF Radius", "Radius of mean for smoothing illumination correction image", "5");
		Parameter pa2 = new Parameter("Image-DF Radius", "Radius of median filter for smoothing dark field corrected experimental image (if no DF provided, it WILL smooth the image but not subtract any DF image from the image)", "3");
		Parameter pa3 = new Parameter("Est. BG sigma", "Estimated noise in the background signal (i.e., mu +/- sigma). Set to 0 or less to avoid this step.", "100");
		// Parameter pa3 = new
		// Parameter("Color Dim Name","Name of the color dimension in this image set","Color");
		// Parameter pa4 = new
		// Parameter("Exposure Times of Color Dims","Exposure times used for each color dim in order separated by commas","100,2000,1000");
		// Parameter pa5 = new
		// Parameter("Perform Final Background Substract?","Should a background subtraction be performed after the background correction?",Parameter.DROPDOWN,new
		// String[]{"true","false"},1);
		Parameter p0 = new Parameter("BG Sub. Presmooth Radius", "Radius of mean filter to apply temporarily for background subtraction (only affects rolling ball subtraction, not applied directly to original image)", "5");
		Parameter p1 = new Parameter("BG Sub. Radius", "Kernal radius in pixels (e.g. 3.8)", "150");
		// Parameter p2 = new
		// Parameter("BG Sub. Inverted","Generally false but true if imaging absorbance",Parameter.DROPDOWN,new
		// String[] {"true","false"},1);
		Parameter p4 = new Parameter("BG Sub. Paraboloid", "A sliding paraboloid is recommended", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		// Parameter p5 = new
		// Parameter("BG Sub. Presmoothing","Do a presmoothing",Parameter.DROPDOWN,new
		// String[] {"true","false"},1);
		Parameter p6 = new Parameter("Output Bit Depth", "Depth of the outputted image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 1);
		// Parameter p7 = new
		// Parameter("Noise Filter Radius","Radius of the mean filter used to determine the mean of the remainder of the background noise after the rolling ball background subtraction.","20");
		Parameter p8 = new Parameter("Nominal Value to Add Back", "Nominal value to add back to the image so that we don't clip values below zero", "100");
		Parameter p9 = new Parameter("'<Name>=<Value>' to Exclude (optional)", "Optionally specify a particular name value pair to exclude from background correction. Useful for excluding bright-field (e.g., Channel=BF). Technically doesn't have to be the 'Channel' dimension.", "");

		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(pa0);
		parameterArray.addParameter(pa1);
		parameterArray.addParameter(pa2);
		parameterArray.addParameter(pa3);
		// parameterArray.addParameter(pa3);
		// parameterArray.addParameter(pa4);
		// parameterArray.addParameter(pa5);
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		// parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		return parameterArray;
	}

	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------

	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return true;
	}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData data = inputs.get("Images");
		if(data == null || !data.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		JEXData darkData = inputs.get("DF Image");
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

		JEXData illumData = inputs.get("IF Image");
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

		JEXData roiData = inputs.get("Optional Crop ROI");
		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null && roiData.getDataObjectType().equals(JEXData.ROI))
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
		}

		// //// Get params
		// double darkExp =
		// Double.parseDouble(parameters.getValueOfParameter("Dark Field Exposure Time"));
		// double illumExp =
		// Double.parseDouble(parameters.getValueOfParameter("Illumination Field Exposure Time"));
		// CSVList exposureStrings = new
		// CSVList(parameters.getValueOfParameter("Exposure Times of Color Dims"));
		// Double[] exposures = new Double[exposureStrings.size()];
		// int i = 0;
		// for(String exp : exposureStrings)
		// {
		// exposures[i] = Double.parseDouble(exp);
		// i++;
		// }
		// String colorDimName =
		// parameters.getValueOfParameter("Color Dim Name");
		// Dim colorDim = data.getDimTable().getDimWithName(colorDimName);
		// boolean performFinalBGSubtract =
		// Boolean.parseBoolean(parameters.getValueOfParameter("Perform Final Background Substract?"));
		double IFDFRadius = Double.parseDouble(this.parameters.getValueOfParameter("IF-DF Radius"));
		double imageDFRadius = Double.parseDouble(this.parameters.getValueOfParameter("Image-DF Radius"));
		double sigma = Double.parseDouble(this.parameters.getValueOfParameter("Est. BG sigma"));
		double bgRadius = Double.parseDouble(this.parameters.getValueOfParameter("BG Sub. Radius"));
		boolean bgInverse = false; // Boolean.parseBoolean(parameters.getValueOfParameter("Inverted"));
		boolean bgParaboloid = Boolean.parseBoolean(this.parameters.getValueOfParameter("Paraboloid"));
		boolean bgPresmooth = false; // Boolean.parseBoolean(parameters.getValueOfParameter("Presmoothing"));
		double bgPresmoothRadius = Double.parseDouble(this.parameters.getValueOfParameter("BG Sub. Presmooth Radius"));
		int outputDepth = Integer.parseInt(this.parameters.getValueOfParameter("Output Bit Depth"));
		String toExclude = this.parameters.getValueOfParameter("'<Name>=<Value>' to Exclude (optional)");
		DimensionMap thingToExclude = null;
		if(toExclude != null && !toExclude.equals(""))
		{
			thingToExclude = new DimensionMap(toExclude);
		}

		// double noiseRadius =
		// Double.parseDouble(parameters.getValueOfParameter("Noise Filter Radius"));
		double nominal = Double.parseDouble(this.parameters.getValueOfParameter("Nominal Value to Add Back"));
		// double bgDetectionRadius =
		// Double.parseDouble(parameters.getValueOfParameter("BG Detection Radius"));
		// boolean normalize =
		// Boolean.parseBoolean(parameters.getValueOfParameter("Normalize"));

		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);
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
		int total = images.size();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap dim : images.keySet())
		{
			FloatProcessor croppedIllumImp = illumImp;
			FloatProcessor croppedDarkImp = darkImp;
			if(this.isCanceled())
			{
				return false;
			}
			String path = images.get(dim);


			Logs.log("Calibrating image for " + dim.toString(), this);

			// /// Get the image
			ImagePlus im = new ImagePlus(path);
			FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor

			// //// crop if desired
			ROIPlus cropRoi = roiMap.get(dim);
			if(cropRoi != null)
			{
				imp.setRoi(cropRoi.getRoi());
				imp = (FloatProcessor) imp.crop();
				
				// For efficiency, we don't need to do this if we are going to skip the rest of the steps anyway
				if(thingToExclude == null || dim.compareTo(thingToExclude) != 0)
				{
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
			}
			
			if(thingToExclude != null && dim.compareTo(thingToExclude) == 0)
			{
				// Then it is a match for the thing to exclude
				// Just save a copy of the original (cropped) image.
				ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", outputDepth);
				String temp = JEXWriter.saveImage(toSave);
				outputMap.put(dim.copy(), temp);
				Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
				count++;
				continue;
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
					bS.rollingBallBackground(impTemp, bgRadius, true, bgInverse, bgParaboloid, bgPresmooth, true);

					// subtract the calculated background from the image
					blit.copyBits(impTemp, 0, 0, FloatBlitter.SUBTRACT);

					// Release memory of impTemp
					impTemp = null;
				}
				else
				{
					BackgroundSubtracter bS = new BackgroundSubtracter();			
					bS.rollingBallBackground(imp, bgRadius, false, bgInverse, bgParaboloid, bgPresmooth, true);
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

			outputMap.put(dim.copy(), finalPath1);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;

			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		// Set the outputs
		JEXData output = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outputMap);
		output.setDataObjectInfo("Background subtracted using background subtraction function");
		this.realOutputs.add(output);

		// Return status
		return true;
	}

}
