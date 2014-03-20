package function.experimentalDataProcessing;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.HashMap;
import java.util.TreeMap;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
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
public class JEX_CoStainCounter extends ExperimentalDataCrunch {
	
	public JEX_CoStainCounter()
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
		String result = "Count CoStained Cells";
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
		String result = "Function to count cells that are costained.";
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
		inputNames[0] = new TypeName(ROI, "First Point ROI");
		inputNames[1] = new TypeName(ROI, "Second Point ROI");
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
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(ROI, "Points Exclusive to Stain A");
		defaultOutputNames[1] = new TypeName(ROI, "CoStained Points");
		defaultOutputNames[2] = new TypeName(ROI, "Points Exclusive to Stain B");
		
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
		Parameter p0 = new Parameter("Cell Radius", "Estimate Cell Radius for Overlap Analysis.", "14");
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
		// Collect the inputs
		JEXData pointData1 = inputs.get("First Point ROI");
		if(pointData1 == null || !pointData1.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Collect the inputs
		JEXData pointData2 = inputs.get("Second Point ROI");
		if(pointData2 == null || !pointData2.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Get the parameters
		String radiusS = parameters.getValueOfParameter("Cell Radius");
		int radius = Integer.parseInt(radiusS);
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> pointROI1 = RoiReader.readObjectToRoiMap(pointData1);
		TreeMap<DimensionMap,ROIPlus> pointROI2 = RoiReader.readObjectToRoiMap(pointData2);
		
		// Create dummy variables
		ROIPlus pointA, pointB;
		Point pointab;
		TreeMap<DimensionMap,ROIPlus> outputROIA = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> outputROIAB = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> outputROIB = new TreeMap<DimensionMap,ROIPlus>();
		
		for (DimensionMap map : pointROI1.keySet())
		{
			pointA = pointROI1.get(map);
			pointB = pointROI2.get(map);
			if(pointB == null)
				continue;
			PointList plA = new PointList();
			PointList plAB = new PointList();
			PointList plB = new PointList();
			
			// iterate through A to get A and AB
			for (Point p : pointA.pointList)
			{
				pointab = pointB.pointList.nearestPointInCircularRange(p, radius);
				if(pointab != null)
					plAB.add(p);
				else
					plA.add(p);
			}
			
			// iterate through B to get B
			for (Point p : pointB.pointList)
			{
				pointab = pointA.pointList.nearestPointInCircularRange(p, radius);
				if(pointab == null)
					plB.add(p);
				else
					continue;
			}
			
			outputROIA.put(map, new ROIPlus(plA, ROIPlus.ROI_POINT));
			outputROIAB.put(map, new ROIPlus(plAB, ROIPlus.ROI_POINT));
			outputROIB.put(map, new ROIPlus(plB, ROIPlus.ROI_POINT));
		}
		
		JEXData output1 = RoiWriter.makeRoiObject(outputNames[0].getName(), outputROIA);
		JEXData output2 = RoiWriter.makeRoiObject(outputNames[1].getName(), outputROIAB);
		JEXData output3 = RoiWriter.makeRoiObject(outputNames[2].getName(), outputROIB);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
}
