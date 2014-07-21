package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import tables.DimTable;
import tables.DimensionMap;

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
public class JEX_ImageTools_MeasureROILineSegment extends JEXCrunchable {
	
	public JEX_ImageTools_MeasureROILineSegment()
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
		String result = "Measure Roi Line";
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
		String result = "Function that allows you to measure characteristics of a 1 segment long line.";
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
		String toolbox = "Image tools";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(ROI, "Line ROI");
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
		defaultOutputNames = new TypeName[4];
		defaultOutputNames[0] = new TypeName(VALUE, "Width");
		defaultOutputNames[1] = new TypeName(VALUE, "Height");
		defaultOutputNames[2] = new TypeName(VALUE, "Length");
		defaultOutputNames[3] = new TypeName(VALUE, "Angle");
		
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
		Parameter p0 = new Parameter("Angle Range", "Choose the way in which the angle is measured by indicating the limits of the measurement.", Parameter.DROPDOWN, new String[] { "-pi/2 to pi/2", "-pi to pi" }, 0);
		Parameter p1 = new Parameter("First Measurement Only", "Choose to return a table of values for multi-dimensional image object or just a single value representing the first measurement", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
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
		parameterArray.addParameter(p1);
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
		JEXData roiData = inputs.get("Line ROI");
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Collect Parameters
		String angleMeasurement = parameters.getValueOfParameter("Angle Range");
		boolean useAtan2 = angleMeasurement.equals("-pi to pi");
		boolean firstOnly = Boolean.parseBoolean(parameters.getValueOfParameter("First Measurement Only"));
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		
		TreeMap<DimensionMap,String> widthMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> heightMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> lengthMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> angleMap = new TreeMap<DimensionMap,String>();
		ROIPlus roi;
		double width, height, length, angle;
		DecimalFormat formatD = new DecimalFormat("##0.000");
		DimTable roiDimTable = roiData.getDimTable();
		List<DimensionMap> maps = roiDimTable.getDimensionMaps();
		int total = maps.size(), count = 0;
		for (DimensionMap map : maps)
		{
			roi = rois.get(map);
			if(roi != null && roi.getPointList() != null && roi.getPointList().size() != 0)
			{
				if(roi.type != ROIPlus.ROI_LINE)
					return false;
				PointList pl = roi.getPointList();
				Point p1 = pl.get(0);
				Point p2 = pl.get(1);
				width = Math.abs(p2.x - p1.x);
				height = Math.abs(p2.y - p1.y);
				length = Math.sqrt(width * width + height * height);
				if(useAtan2)
				{
					angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
				}
				else
				{
					angle = Math.atan(height / width);
				}
				if(firstOnly)
				{
					JEXData output1 = ValueWriter.makeValueObject(outputNames[0].getName(), formatD.format(width));
					JEXData output2 = ValueWriter.makeValueObject(outputNames[1].getName(), formatD.format(height));
					JEXData output3 = ValueWriter.makeValueObject(outputNames[2].getName(), formatD.format(length));
					JEXData output4 = ValueWriter.makeValueObject(outputNames[3].getName(), formatD.format(angle));
					
					// Set the outputs
					realOutputs.add(output1);
					realOutputs.add(output2);
					realOutputs.add(output3);
					realOutputs.add(output4);
					
					JEXStatics.statusBar.setProgressPercentage(100);
					
					return true;
				}
				else
				{
					widthMap.put(map, formatD.format(width));
					heightMap.put(map, formatD.format(height));
					lengthMap.put(map, formatD.format(length));
					angleMap.put(map, formatD.format(angle));
					
					JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) count / total));
				}
			}
		}
		
		JEXData output1 = ValueWriter.makeValueTable(outputNames[0].getName(), widthMap);
		JEXData output2 = ValueWriter.makeValueTable(outputNames[1].getName(), heightMap);
		JEXData output3 = ValueWriter.makeValueTable(outputNames[2].getName(), lengthMap);
		JEXData output4 = ValueWriter.makeValueTable(outputNames[3].getName(), angleMap);
		
		output1.setDimTable(roiDimTable);
		output2.setDimTable(roiDimTable);
		output3.setDimTable(roiDimTable);
		output4.setDimTable(roiDimTable);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		realOutputs.add(output4);
		
		// Return status
		return true;
	}
}
