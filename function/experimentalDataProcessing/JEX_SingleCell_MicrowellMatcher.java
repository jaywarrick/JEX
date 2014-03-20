package function.experimentalDataProcessing;

import image.roi.HashedPointList;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
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
public class JEX_SingleCell_MicrowellMatcher extends ExperimentalDataCrunch {
	
	public JEX_SingleCell_MicrowellMatcher()
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
		String result = "Microwell Matcher";
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
		String result = "Function that allows you to measure intensity with a defineable ellipse or rectangle at locations defined by a point roi.";
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
		TypeName[] inputNames = new TypeName[3];
		inputNames[0] = new TypeName(ROI, "Before ROI");
		inputNames[1] = new TypeName(ROI, "Before ROI (Adjusted)");
		inputNames[2] = new TypeName(ROI, "After ROI");
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
		this.defaultOutputNames = new TypeName[2];
		this.defaultOutputNames[0] = new TypeName(ROI, "Matched Microwells A");
		this.defaultOutputNames[1] = new TypeName(ROI, "Matched Microwells B");
		
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
		Parameter p3 = new Parameter("Grid Spacing", "Grid Spacing", "60");
		Parameter p4 = new Parameter("Tolerance [%]", "Tolerance in terms of percent spacing for finding a match", "40");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		
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
		JEXData beforeData = inputs.get("Before ROI");
		if(beforeData == null || !beforeData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Collect the inputs
		JEXData beforeDataAdjusted = inputs.get("Before ROI (Adjusted)");
		if(beforeDataAdjusted == null || !beforeDataAdjusted.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Collect the inputs
		JEXData afterData = inputs.get("After ROI");
		if(afterData == null || !afterData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Gather parameters
		int spacing = Integer.parseInt(this.parameters.getValueOfParameter("Grid Spacing"));
		double tol = Double.parseDouble(this.parameters.getValueOfParameter("Tolerance [%]"));
		tol = tol / 100.0 * (spacing);
		
		// Get the input data
		TreeMap<DimensionMap,ROIPlus> rois1 = RoiReader.readObjectToRoiMap(beforeData);
		TreeMap<DimensionMap,ROIPlus> rois1b = RoiReader.readObjectToRoiMap(beforeDataAdjusted);
		TreeMap<DimensionMap,ROIPlus> rois2 = RoiReader.readObjectToRoiMap(afterData);
		
		// Initialize loop variables
		
		double total = beforeData.getDimTable().mapCount();
		double count = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		
		TreeMap<DimensionMap,ROIPlus> matchRoisA = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> matchRoisB = new TreeMap<DimensionMap,ROIPlus>();
		for (DimensionMap map : beforeData.getDimTable().getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			PointList pts1 = rois1.get(map).getPointList();
			PointList pts1b = rois1b.get(map).getPointList();
			HashedPointList pts1bHash = new HashedPointList(pts1b);
			PointList pts2 = rois2.get(map).getPointList();
			
			PointList matchesB = new PointList();
			for (IdPoint p : pts2)
			{
				IdPoint match = pts1bHash.getNearestInRange(p, tol, true);
				if(match != null)
				{
					matchesB.add(new IdPoint(p.x, p.y, match.id)); // use position of p but the id of match
				}
			}
			
			PointList matchesA = new PointList();
			for (IdPoint match : matchesB)
			{
				for (IdPoint p1 : pts1)
				{
					if(p1.id == match.id)
					{
						matchesA.add(p1.copy()); // use position of p but the id of match
						break;
					}
				}
			}
			
			ROIPlus matchRoiA = new ROIPlus(matchesA, ROIPlus.ROI_POINT);
			ROIPlus matchRoiB = new ROIPlus(matchesB, ROIPlus.ROI_POINT);
			matchRoisA.put(map, matchRoiA);
			matchRoisB.put(map, matchRoiB);
			
			count = count + 1;
			int percentage = (int) (100 * ((count) / total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output1 = RoiWriter.makeRoiObject(this.outputNames[0].getName(), matchRoisA);
		JEXData output2 = RoiWriter.makeRoiObject(this.outputNames[1].getName(), matchRoisB);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		
		// Return status
		return true;
		
	}
	
}
