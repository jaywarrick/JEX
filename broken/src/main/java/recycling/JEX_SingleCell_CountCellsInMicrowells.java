package recycling;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import image.roi.HashedPointList;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.ROIPlus.PatternRoiIterator;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import logs.Logs;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

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
public class JEX_SingleCell_CountCellsInMicrowells extends JEXCrunchable {
	
	public JEX_SingleCell_CountCellsInMicrowells()
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
		String result = "Count Cells in Microwells";
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
		String result = "Function that allows you to count the number of points in a point roi that fall into the array roi that defines microwell boundaries and outputs a table of that information.";
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
		inputNames[0] = new TypeName(ROI, "Maxima");
		inputNames[1] = new TypeName(ROI, "Microwells");
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
		defaultOutputNames[0] = new TypeName(FILE, "Microwell Cell Counts");
		defaultOutputNames[1] = new TypeName(FILE, "Microwell Cell Count Avgs");
		defaultOutputNames[2] = new TypeName(ROI, "Filtered Maxima");
		
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
		// Parameter p3 = new Parameter("Type","Type of roi",Parameter.DROPDOWN,
		// new String[]{"Rectangle","Ellipse","Line","Point"},0);
		// Parameter p4 = new
		// Parameter("ROI Width","Width of ROI in pixels (ignored for Point ROI)","10");
		// Parameter p5 = new
		// Parameter("ROI Height","Height of ROI in pixels (ignored for Point ROI)","10");
		// Parameter p6 = new
		// Parameter("ROI Origin","What part of the roi should be placed at the indicated points (i.e. does the array of points indicate where the upper-left corner should be placed?",Parameter.DROPDOWN,new
		// String[]{"Center","Upper-Left","Upper-Right","Lower-Right","Lower-Left"},0);
		Parameter p7 = new Parameter("'Time' Dim Name", "Name of the 'Time' Dimension in the image", "Time");
		Parameter p8 = new Parameter("Name for 'Microwell' Dim", "Name of the Dimension to create for the microwell index", "Microwell");
		Parameter p9 = new Parameter("'Color' Dim Name", "Name of the 'Color' Dimension in the roi", "Color");
		Parameter p10 = new Parameter("Nuclear Stain Color Dim Value", "Value of the Color Dim for Nuclear Stain Images", "1");
		// Parameter p11 = new
		// Parameter("Green Color Value","Value of the Color Dim for Green","1");
		// Parameter p12 = new
		// Parameter("Red Color Value","Value of the Color Dim for Red","2");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		// parameterArray.addParameter(p11);
		// parameterArray.addParameter(p12);
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
	
	public String timeDimName, colorDimName, microwellDimName, nuclearColor;
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData maximaData = inputs.get("Maxima");
		if(maximaData == null || !maximaData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Collect the inputs
		JEXData microwellData = inputs.get("Microwells");
		if(microwellData == null || !microwellData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Gather parameters
		microwellDimName = parameters.getValueOfParameter("Name for 'Microwell' Dim");
		timeDimName = parameters.getValueOfParameter("'Time' Dim Name"); // Typically
		// "Time"
		// or
		// "T"
		colorDimName = parameters.getValueOfParameter("'Color' Dim Name");
		nuclearColor = parameters.getValueOfParameter("Nuclear Stain Color Dim Value");
		
		// Get the input data
		TreeMap<DimensionMap,ROIPlus> maxima = RoiReader.readObjectToRoiMap(maximaData);
		TreeMap<DimensionMap,ROIPlus> microwells = RoiReader.readObjectToRoiMap(microwellData);
		DimTable filteredTable = microwellData.getDimTable().getSubTable(new DimensionMap(colorDimName + "=" + nuclearColor));
		
		// Initialize loop variables
		ROIPlus microwellROI = null;
		int count = 0;
		int total = filteredTable.mapCount();
		int percentage = 0;
		
		TreeSet<String> times = new TreeSet<String>();
		TreeMap<DimensionMap,Double> results = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,ROIPlus> filteredROIs = new TreeMap<DimensionMap,ROIPlus>();
		DimensionMap dataPtMap;
		for (DimensionMap microwellMap : filteredTable.getMapIterator())
		{
			microwellROI = microwells.get(microwellMap);
			ROIPlus maximaRoi = maxima.get(microwellMap);
			if(maximaRoi == null)
			{
				Logs.log("Help me.", 0, this);
			}
			PointList maximaPts = maximaRoi.getPointList();
			HashedPointList sortedMaxima = new HashedPointList(maximaPts);
			String time = microwellMap.get(timeDimName);
			times.add(time);
			
			PointList filteredPoints = new PointList();
			PatternRoiIterator microwellROIItr = microwellROI.new PatternRoiIterator(microwellROI);
			while (microwellROIItr.hasNext())
			{
				ROIPlus region = microwellROIItr.next();
				PointList pointsInMicrowell = sortedMaxima.getPointsInRect(region.getPointList().getBounds());
				dataPtMap = microwellMap.copy();
				dataPtMap.remove(colorDimName);
				// dataPtMap.put(timeDimName, time);
				dataPtMap.put(microwellDimName, "" + microwellROIItr.currentPatternPoint().id);
				dataPtMap.put("Measurement", "Cell Count");
				if(pointsInMicrowell == null)
				{
					results.put(dataPtMap, 0.0);
				}
				else
				{
					results.put(dataPtMap, (double) pointsInMicrowell.size());
					filteredPoints.addAll(pointsInMicrowell);
				}
			}
			
			ROIPlus filteredROI = new ROIPlus(filteredPoints, ROIPlus.ROI_POINT);
			filteredROIs.put(microwellMap, filteredROI);
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		TreeMap<DimensionMap,Double> avgs = new TreeMap<DimensionMap,Double>();
		DimensionMap avgMap;
		Double avg;
		double tempNum;
		for (DimensionMap map : results.keySet())
		{
			avgMap = map.copy();
			avgMap.remove(timeDimName);
			avg = avgs.get(avgMap);
			tempNum = results.get(map);
			if(avg == null)
			{
				avg = 0.0;
			}
			avg = avg + tempNum;
			avgs.put(avgMap, avg);
		}
		for (Entry<DimensionMap,Double> e : avgs.entrySet())
		{
			e.setValue(e.getValue() / times.size()); // divide by the number of
			// timepoints to get the
			// average
		}
		
		String resultsPath = JEXTableWriter.writeTable("Cell Counts", results);
		String avgsPath = JEXTableWriter.writeTable("Cell Count Averages", avgs);
		JEXData output1 = FileWriter.makeFileObject(outputNames[0].getType(), outputNames[0].getName(), resultsPath);
		JEXData output2 = FileWriter.makeFileObject(outputNames[1].getType(), outputNames[1].getName(), avgsPath);
		JEXData output3 = RoiWriter.makeRoiObject(outputNames[2].getName(), filteredROIs);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
}
