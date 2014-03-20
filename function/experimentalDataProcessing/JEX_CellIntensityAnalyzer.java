package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.LSVList;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;

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
public class JEX_CellIntensityAnalyzer extends ExperimentalDataCrunch {
	
	public JEX_CellIntensityAnalyzer()
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
		String result = "Cell Intensity Analyzer";
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
		String result = "Function to count fluorescent cell intensities.";
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
		String toolbox = "Custom Cell Analysis";
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
		return false;
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
		inputNames[0] = new TypeName(ROI, "Point ROI");
		inputNames[1] = new TypeName(IMAGE, "Images");
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
		defaultOutputNames[0] = new TypeName(VALUE, "List of Point Intensities");
		
		if(outputNames == null)
			return defaultOutputNames;
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
		Parameter p0 = new Parameter("Cell Radius", "Estimate Cell Radius for Intensity Analysis.", "14");
		// Parameter p1 = new
		// Parameter("Old Min","Image Intensity Value","0.0");
		// Parameter p2 = new
		// Parameter("Old Max","Image Intensity Value","4095.0");
		// Parameter p3 = new
		// Parameter("New Min","Image Intensity Value","0.0");
		// Parameter p4 = new
		// Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new
		// Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new
		// String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
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
		try
		{
			// Do file stuff
			
			// Collect the inputs
			JEXData pointData1 = inputs.get("Point ROI");
			if(pointData1 == null || !pointData1.getTypeName().getType().equals(JEXData.ROI))
				return false;
			JEXData image = inputs.get("Images");
			if(image == null || !image.getTypeName().getType().equals(JEXData.IMAGE))
				return false;
			
			// Get the parameters
			String radiusS = parameters.getValueOfParameter("Cell Radius");
			int radius = Integer.parseInt(radiusS);
			
			// Run the function
			TreeMap<DimensionMap,ROIPlus> pointROI1 = RoiReader.readObjectToRoiMap(pointData1);
			TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(image);
			
			// Create dummy variables
			ROIPlus point, pointy;
			OvalRoi oval;
			double count = 0;
			LSVList result = new LSVList();
			int intensity;
			int x, y;
			CSVList intensities = new CSVList();
			
			// This creates a line at the top of the CSV list showing a numbered
			// list of the points
			int maxCount = 0, curCount = 0;
			for (DimensionMap map : pointROI1.keySet())
			{
				curCount = pointROI1.get(map).getPointList().size();
				if(curCount > maxCount)
					maxCount = curCount;
			}
			for (int i = 1; i <= maxCount; i++)
			{
				intensities.add("" + i);
			}
			result.add(intensities.toString());
			
			// This creates a line underneath that CSV list showing the x
			// coordinates of each point
			for (DimensionMap map : pointROI1.keySet())
			{
				pointy = pointROI1.get(map);
				if(pointy == null)
					continue;
				intensities.clear();
				for (Point p : pointy.pointList)
				{
					x = p.x;
					intensities.add("" + x);
				}
			}
			result.add(intensities.toString());
			
			// This creates a line underneath that CSV list showing the y
			// coordinates of each point
			for (DimensionMap map : pointROI1.keySet())
			{
				pointy = pointROI1.get(map);
				if(pointy == null)
					continue;
				intensities.clear();
				for (Point p : pointy.pointList)
				{
					y = p.y;
					intensities.add("" + y);
				}
			}
			result.add(intensities.toString());
			
			// This is the actual intensity measuring function
			for (DimensionMap map : pointROI1.keySet())
			{
				point = pointROI1.get(map); // This gets the points
				// corresponding to the JEX entry
				if(point == null)
					continue;
				intensities.clear();
				for (Point p : point.pointList)
				{
					// make each point into an oval
					x = p.x - radius;
					y = p.y - radius;
					oval = new OvalRoi(x, y, radius * 2, radius * 2);
					
					// create intensity list based on points
					String imPath = images.get(map);
					ImagePlus im = new ImagePlus(imPath);
					im.setRoi(oval);
					ImageStatistics imStat = im.getStatistics(Measurements.MIN_MAX);
					intensity = (int) imStat.max;
					intensities.add("" + intensity);
				}
				
				// Status bar
				int percentage = (int) (100 * (count / images.size()));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				count = count + 1;
				
				// This will be printed in the file on a new line.
				result.add(intensities.toString());
				
			}
			
			String finalPath = JEXWriter.saveText(result.toString(), "csv");
			JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), null, finalPath);
			// Set the outputs
			realOutputs.add(output1);
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			return false;
		}
		
		// Return status
		return true;
	}
}
