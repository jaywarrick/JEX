package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
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
 * @author erwinberthier
 * 
 */
public class JEX_ImageTools_MakeROIArrayManual extends JEXCrunchable {
	
	public JEX_ImageTools_MakeROIArrayManual()
	{}
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	public String getName()
	{
		String result = "Make ROI Pattern (manual)";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Make an array of rectangles (polygons to allow for angles), ellipses, lines (one segment), or points using the points of a Point ROI, Polyline ROI, or Polygon ROI to define locations in the pattern";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
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
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
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
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(ROI, "Points");
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(ROI, "Array Roi");
		
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
	public ParameterSet requiredParameters()
	{
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p3 = new Parameter("Type", "Type of roi", Parameter.DROPDOWN, new String[] { "Rectangle", "Ellipse", "Line", "Point" }, 0);
		Parameter p4 = new Parameter("ROI Width", "Width of ROI in pixels (ignored for Point ROI)", "10");
		Parameter p5 = new Parameter("ROI Height", "Height of ROI in pixels (ignored for Point ROI)", "10");
		Parameter p6 = new Parameter("ROI Origin", "What part of the roi should be placed at the indicated points (i.e. does the array of points indicate where the upper-left corner should be placed?", Parameter.DROPDOWN, new String[] { "Center", "Upper-Left", "Upper-Right", "Lower-Right", "Lower-Left" }, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
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
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		
		JEXStatics.statusBar.setProgressPercentage(0);
		
		// Collect the inputs
		JEXData roiData = inputs.get("Points");
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Gather parameters
		String type = parameters.getValueOfParameter("Type");
		int roiType = ROIPlus.ROI_RECT;
		if(type.equals("Ellipse"))
		{
			roiType = ROIPlus.ROI_ELLIPSE;
		}
		else if(type.equals("Line"))
		{
			roiType = ROIPlus.ROI_LINE;
		}
		else if(type.equals("Point"))
		{
			roiType = ROIPlus.ROI_POINT;
		}
		int roiWidth = Integer.parseInt(parameters.getValueOfParameter("ROI Width"));
		int roiHeight = Integer.parseInt(parameters.getValueOfParameter("ROI Height"));
		String roiOrigin = parameters.getValueOfParameter("ROI Origin");
		
		// Create the roi for which a location and pattern will be set
		ROIPlus roip = getRoi(roiOrigin, roiType, roiWidth, roiHeight);
		if(roip == null || roip.getPointList().size() == 0)
		{
			Logs.log("Couldn't get the points to make the roi!", 9, this);
			JEXStatics.statusBar.setStatusText("Couldn't get the points to make the roi!");
			return false;
		}
		
		// Run the function
		DimTable roiTable = roiData.getDimTable();
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		int count = 0, percentage = 0, total = roiTable.getDimensionMaps().size();
		
		// Make the rois and put them in the arrayRoiMap
		TreeMap<DimensionMap,ROIPlus> arrayRoiMap = new TreeMap<DimensionMap,ROIPlus>();
		for (DimensionMap roiM : roiMap.keySet())
		{
			
			// Get the array Locations
			ROIPlus patternRoi = roiMap.get(roiM);
			if(patternRoi == null)
			{
				Logs.log("Couldn't read the roi that defines the array!", 9, this);
				JEXStatics.statusBar.setStatusText("Couldn't read the roi that defines the array!");
				return false;
			}
			PointList pattern = patternRoi.getPointList();
			
			ROIPlus roip2 = roip.copy();
			roip2.getPointList().translate(pattern.get(0).x, pattern.get(0).y);
			roip2.setPattern(pattern);
			arrayRoiMap.put(roiM, roip2);
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output1 = RoiWriter.makeRoiObject(outputNames[0].getName(), arrayRoiMap);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private ROIPlus getRoi(String roiOrigin, int roiType, int roiWidth, int roiHeight)
	{
		PointList pl = new PointList();
		Point p1 = new Point(0, 0);
		Point p2 = new Point(roiWidth, roiHeight);
		
		ROIPlus ret = null;
		
		if(roiType == ROIPlus.ROI_POINT)
		{
			pl.add(p1);
			ret = new ROIPlus(pl, roiType);
			return ret;
		}
		else
		{
			pl.add(p1);
			pl.add(p2);
		}
		
		double width = (double) roiWidth;
		double height = (double) roiHeight;
		
		if(roiOrigin.equals("Center"))
		{
			pl.translate(-width / 2, -height / 2);
		}
		else if(roiOrigin.equals("Upper-Right"))
		{
			pl.translate(-width, 0);
		}
		else if(roiOrigin.equals("Lower-Left"))
		{
			pl.translate(0, -height);
		}
		else if(roiOrigin.equals("Lower-Right"))
		{
			pl.translate(-width, -height);
		}
		
		ret = new ROIPlus(pl, roiType);
		return ret;
	}
}
