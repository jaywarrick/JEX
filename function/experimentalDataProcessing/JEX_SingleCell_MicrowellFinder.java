package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.ExperimentalDataCrunch;
import function.singleCellAnalysis.MicrowellFinder;

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
public class JEX_SingleCell_MicrowellFinder extends ExperimentalDataCrunch {
	
	public JEX_SingleCell_MicrowellFinder()
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
		String result = "Microwell Finder";
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
		String result = "Find microwells from the microwell convolution image.";
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
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(ROI, "Microwells");
		
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
		// (ImageProcessor ip, double tolerance, double threshold, int outputType, boolean excludeOnEdges, boolean isEDM, Roi roiArg, boolean lightBackground)
		// Parameter p0 = new Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new String[] {"true"},0);
		Parameter p1 = new Parameter("Conv. Threshold", "Minimum hieght of a maximum.", "0");
		Parameter p2 = new Parameter("Conv. Tolerance", "Local intensity increase threshold.", "1.5");
		Parameter p3 = new Parameter("Grid Spacing", "Approximate pixels between microwell centers.", "60");
		Parameter p4 = new Parameter("Grid Finding Tolerance [%]", "Tolerance or estimated % error in microwell spacing (e.g. due to tilted image)", "10");
		Parameter p5 = new Parameter("Remove Wells On Grid Borders?", "Should the wells on the border of the array (i.e. those without a neighbor to the N,E,S&W) be removed?", Parameter.CHECKBOX, true);
		Parameter p6 = new Parameter("Interpolate Missing Wells?", "Whether or not to assume that a well exists between two wells (i.e. there is a row or column gap)", Parameter.CHECKBOX, true);
		Parameter p7 = new Parameter("Grid Smoothing Tolerance [%]", "Smooth grid locations that don't fit the overall pattern to within X % of the grid spacing\n(negative values or values that result in 0 pixels tolerance result in skipping this step)", "4.0");
		Parameter p8 = new Parameter("Minimum Number of Wells", "The minimum number of wells to find grouped together to consider the lattice of wells as the correct lattice", "40");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
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
		try
		{
			// Collect the inputs
			boolean roiProvided = false;
			JEXData imageData = inputs.get("Image");
			if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			{
				return false;
			}
			JEXData roiData = inputs.get("ROI (optional)");
			if(roiData != null && roiData.getTypeName().getType().equals(JEXData.ROI))
			{
				roiProvided = true;
			}
			
			// Gather parameters
			double convTol = Double.parseDouble(this.parameters.getValueOfParameter("Conv. Tolerance"));
			double convThresh = Double.parseDouble(this.parameters.getValueOfParameter("Conv. Threshold"));
			int gridSpacing = Integer.parseInt(this.parameters.getValueOfParameter("Grid Spacing"));
			double gridTol = Double.parseDouble(this.parameters.getValueOfParameter("Grid Finding Tolerance [%]"));
			boolean excludeGridBorder = Boolean.parseBoolean(this.parameters.getValueOfParameter("Remove Wells On Grid Borders?"));
			boolean interpolate = Boolean.parseBoolean(this.parameters.getValueOfParameter("Interpolate Missing Wells?"));
			double smoothingTolerance = Double.parseDouble(this.parameters.getValueOfParameter("Grid Smoothing Tolerance [%]"));
			int minNumberOfWells = Integer.parseInt(this.parameters.getValueOfParameter("Minimum Number of Wells"));
			
			TreeMap<DimensionMap,ROIPlus> roiMap;
			// Run the function
			if(roiProvided)
			{
				roiMap = RoiReader.readObjectToRoiMap(roiData);
			}
			else
			{
				roiMap = new TreeMap<DimensionMap,ROIPlus>();
			}
			TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,ROIPlus> outputRoiMap = new TreeMap<DimensionMap,ROIPlus>();
			int count = 0, percentage = 0;
			ROIPlus roip;
			int counter = 0;
			for (DimensionMap map : imageMap.keySet())
			{
				if(this.isCanceled())
				{
					return false;
				}
				ImageProcessor imp = (new ImagePlus(imageMap.get(map))).getProcessor();
				roip = roiMap.get(map);
				
				Logs.log("Finding microwells for " + map, 0, JEX_SingleCell_MicrowellFinder.class.getSimpleName());
				PointList points = MicrowellFinder.findMicrowellCentersInConvolvedImage(imp, roip, convThresh, convTol, gridSpacing, gridTol, excludeGridBorder, minNumberOfWells, interpolate, smoothingTolerance);
				
				ROIPlus newRoip = new ROIPlus(points, ROIPlus.ROI_POINT);
				outputRoiMap.put(map, newRoip);
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				counter = counter + 1;
				imp = null;
			}
			if(outputRoiMap.size() == 0)
			{
				return false;
			}
			
			JEXData output1 = RoiWriter.makeRoiObject(this.outputNames[0].getName(), outputRoiMap);
			
			// Set the outputs
			this.realOutputs.add(output1);
			
			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	
	// private String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	// {
	// // Get image data
	// File f = new File(imagePath);
	// if(!f.exists()) return null;
	// ImagePlus im = new ImagePlus(imagePath);
	// FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
	//
	// // Adjust the image
	// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//
	// // Save the results
	// ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
	// String imPath = JEXWriter.saveImage(toSave);
	// im.flush();
	//
	// // return temp filePath
	// return imPath;
	// }
}