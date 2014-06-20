package recycling;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.crunchable.JEX_Filters;
import function.singleCellAnalysis.SingleCellUtility;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */
public class JEX_SingleCell_MeasureBackgroundIntensity extends JEXCrunchable {
	
	public JEX_SingleCell_MeasureBackgroundIntensity()
	{}
	
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
		String result = "Measure Mean Background Intensity";
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
		String result = "Use a mean filter with radius r and find the minimum of that filtered image." + "\nThus r should be chosen to fit between the regions that have intensity." + "\nAn option ROI can be supplied in which to determine the mean of the filtered image.";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(ROI, "ROI (optional)");
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(FILE, "Background Intensities");
		
		if(outputNames == null)
		{
			return defaultOutputNames;
		}
		return outputNames;
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p1 = new Parameter("Radius", "Radius of the mean filter", "25");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-ridden by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return false;
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
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		JEXData roiData = inputs.get("ROI (optional)");
		
		// Gather parameters
		double radius = Double.parseDouble(parameters.getValueOfParameter("Radius"));
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> roiMap = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null)
		{
			roiMap = RoiReader.readObjectToRoiMap(roiData);
		}
		
		int count = 0, percentage = 0, total = imageMap.size();
		TreeMap<DimensionMap,TreeMap<DimensionMap,Double>> initialBackgrounds = new TreeMap<DimensionMap,TreeMap<DimensionMap,Double>>();
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			Double bg = getBackgroundIntensity(radius, imageMap.get(map), roiMap.get(map));
			String bgString = map.get("Color");
			if(bgString.equals("0"))
			{
				bgString = SingleCellUtility.b;
			}
			else if(bgString.equals("1"))
			{
				bgString = SingleCellUtility.g;
			}
			else
			// (bgString.equals("2"))
			{
				bgString = SingleCellUtility.r;
			}
			DimensionMap bgMap = new DimensionMap("Color=" + bgString + "," + "Time=" + map.get("Time"));
			TreeMap<DimensionMap,Double> bgs = initialBackgrounds.get(bgMap);
			if(bgs == null)
			{
				bgs = new TreeMap<DimensionMap,Double>();
				initialBackgrounds.put(bgMap, bgs);
			}
			bgs.put(map, bg);
			count = count + 1;
			percentage = (int) (99 * ((double) (count) / ((double) total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		TreeMap<DimensionMap,Double> finalBackgrounds = new TreeMap<DimensionMap,Double>();
		for (Entry<DimensionMap,TreeMap<DimensionMap,Double>> e : initialBackgrounds.entrySet())
		{
			double tot = 0;
			double n = 0;
			for (Double d : e.getValue().values())
			{
				tot = tot + d;
				n = n + 1;
			}
			finalBackgrounds.put(e.getKey(), tot / n);
		}
		
		// // Now get the moving average of the background
		// DimTable imageDimTable = imageData.getDimTable();
		// DimTable timeTable = new DimTable();
		// timeTable.add(imageDimTable.getDimWithName("Time").copy());
		// Dim colorDim = imageDimTable.getDimWithName("Color").copy();
		// DataWindows backgroundWindows = new DataWindows(avgFrames);
		// count = 0;
		// percentage = 0;
		// DimensionMap oldMap, newMap;
		// DecimalFormat formatD = new DecimalFormat("##0.00");
		// String firstTimeValue = null;
		// for(DimensionMap timeMap : timeTable.getIterator())
		// {
		// backgroundWindows.increment();
		// if(firstTimeValue == null)
		// {
		// firstTimeValue = timeMap.get("Time");
		// }
		// for(String color : colorDim.dimValues)
		// {
		// Integer colorI = Integer.parseInt(color);
		// oldMap = timeMap.copy();
		// oldMap.put("Color", color);
		// backgroundWindows.addPoint(colorI, initialBackgrounds.get(oldMap),
		// 0.0);
		//
		// DataWindow track = backgroundWindows.getWindow(colorI);
		// if(track != null && track.isFilled())
		// {
		// newMap = new DimensionMap();
		// Double startFrame = Double.parseDouble(firstTimeValue);
		// newMap.put("Time", formatD.format(startFrame +
		// backgroundWindows.getAvgIndex()));
		// newMap.put("Color", color);
		// finalBackgrounds.put(newMap.copy(), track.avgX());
		// }
		// }
		// count = count + 1;
		// percentage = (int) (99 + 1 * ((double) (count)/ ((double)
		// timeTable.mapCount())));
		// JEXStatics.statusBar.setProgressPercentage(percentage);
		// }
		if(finalBackgrounds.size() == 0)
		{
			return false;
		}
		
		String path = JEXTableWriter.writeTable("Background Intensities", finalBackgrounds);
		JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), path);
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private ImagePlus imageToFilter;
	private FloatProcessor imageProcessorToFilter;
	
	public Double getBackgroundIntensity(double radius, String imagePath, ROIPlus roi)
	{
		imageToFilter = new ImagePlus(imagePath);
		imageProcessorToFilter = (FloatProcessor) imageToFilter.getProcessor().convertToFloat();
		// imageToFilter.show();
		RankFilters rF = new RankFilters();
		rF.setup(JEX_Filters.MEAN, imageToFilter);
		rF.makeKernel(radius);
		rF.run(imageProcessorToFilter);
		if(roi != null)
		{
			imageProcessorToFilter.setRoi(roi.getRoi());
		}
		else
		{
			imageProcessorToFilter.setRoi(new Rectangle(0, 0, imageProcessorToFilter.getWidth(), imageProcessorToFilter.getHeight()));
		}
		ImageStatistics minMax = ImageStatistics.getStatistics(imageProcessorToFilter, ImageStatistics.MIN_MAX, null);
		Double ret = minMax.min;
		minMax = null;
		imageToFilter.flush();
		imageToFilter = null;
		imageProcessorToFilter = null;
		return ret;
	}
}
