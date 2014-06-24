package function.plugin.old;

import image.roi.ROIPlus;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import miscellaneous.JEXCSVWriter;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

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
public class JEX_DeathStar_PropagationLength extends JEXCrunchable {
	
	public JEX_DeathStar_PropagationLength()
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
		String result = "Death Star Propagation Lengths";
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
		String result = "Function that uses roi information to quantify virus propagation into the death stars.";
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
		inputNames[1] = new TypeName(ROI, "Rectangle ROI");
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
		this.defaultOutputNames[0] = new TypeName(FILE, "Measurements");
		
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
		Parameter p0 = new Parameter("Dummy Parameter", "Lets user know that the function has been selected.", Parameter.DROPDOWN, new String[] { "true" }, 0);
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
		JEXData pointData = inputs.get("Point ROI");
		if(pointData == null || !pointData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Collect the inputs
		JEXData rectangleData = inputs.get("Rectangle ROI");
		if(rectangleData == null || !rectangleData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> pointROI = RoiReader.readObjectToRoiMap(pointData);
		TreeMap<DimensionMap,ROIPlus> rectangleROI = RoiReader.readObjectToRoiMap(rectangleData);
		JEXCSVWriter writer = new JEXCSVWriter(JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("csv"));
		Vector<String> rowOfInfo = new Vector<String>();
		DimTable rTable = rectangleData.getDimTable();
		for (Dim d : rTable)
		{
			rowOfInfo.add(d.name());
		}
		rowOfInfo.add("Quadrant");
		rowOfInfo.add("X0");
		rowOfInfo.add("Y0");
		rowOfInfo.add("X");
		rowOfInfo.add("Y");
		writer.write(rowOfInfo);
		
		ROIPlus points, rectangle;
		for (DimensionMap map : rectangleROI.keySet())
		{
			rectangle = rectangleROI.get(map);
			points = pointROI.get(map);
			for (Point p : points.getPointList())
			{
				rowOfInfo.clear();
				String quad = quadrant(p, rectangle);
				for (String key : map.keySet())
				{
					rowOfInfo.add(map.get(key));
				}
				rowOfInfo.add(quad);
				Point origin = origin(p, rectangle);
				rowOfInfo.add("" + origin.x);
				rowOfInfo.add("" + origin.y);
				rowOfInfo.add("" + p.x);
				rowOfInfo.add("" + p.y);
				writer.write(rowOfInfo);
			}
		}
		
		writer.close();
		
		JEXData output1;
		output1 = FileWriter.makeFileObject(this.outputNames[0].getName(), null, writer.getPath());
		
		// Set the outputs
		this.realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	public static Double distance(Point p, ROIPlus rect)
	{
		Rectangle r = rect.getPointList().getBounds();
		
		boolean left = false;
		if(p.x < r.x + r.width / 2)
		{
			left = true;
		}
		
		int x0 = r.x;
		if(!left)
		{
			x0 = r.x + r.width;
		}
		
		return new Double(Math.abs(p.x - x0));
	}
	
	public static Point origin(Point p, ROIPlus rect)
	{
		Rectangle r = rect.getPointList().getBounds();
		String quad = quadrant(p, rect);
		if(quad.equals("TL"))
		{
			return new Point(r.x, r.y);
		}
		else if(quad.equals("TR"))
		{
			return new Point(r.x + r.width, r.y);
		}
		else if(quad.equals("BR"))
		{
			return new Point(r.x + r.width, r.y + r.height);
		}
		else
		// "BL"
		{
			return new Point(r.x, r.y + r.height);
		}
	}
	
	public static String quadrant(Point p, ROIPlus rect)
	{
		Rectangle r = rect.getPointList().getBounds();
		boolean left = false;
		boolean top = false;
		if(p.x < r.x + r.width / 2)
		{
			left = true;
		}
		if(p.y < r.y + r.height / 2)
		{
			top = true;
		}
		if(left && top)
		{
			return "TL";
		}
		if(!left && top)
		{
			return "TR";
		}
		if(!left && !top)
		{
			return "BR";
		}
		return "BL";
	}
}
