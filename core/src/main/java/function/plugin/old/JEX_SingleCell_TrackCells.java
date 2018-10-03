package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.singleCellAnalysis.DataPointTracker;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
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
public class JEX_SingleCell_TrackCells extends JEXCrunchable {
	
	public JEX_SingleCell_TrackCells()
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
		String result = "Track Cell Location and Expression";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Use a nearest neighbor approach in 6-D for creating tracks from point rois in each frame of a stack (i.e. along one dimension)";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
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
		inputNames[0] = new TypeName(FILE, "Time Files");
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[5];
		defaultOutputNames[0] = new TypeName(FILE, "Tracked Time Files");
		defaultOutputNames[1] = new TypeName(IMAGE, "Old D Histograms");
		defaultOutputNames[2] = new TypeName(IMAGE, "Old FC Histograms");
		defaultOutputNames[3] = new TypeName(IMAGE, "New D Histograms");
		defaultOutputNames[4] = new TypeName(IMAGE, "New FC Histograms");
		
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
		Parameter p1 = new Parameter("W", "Relative weight the algorithm places on x-y positions relative to R-G-B expressions (0 to 1). Expressions get a weight of (1-W).", "0.5");
		Parameter p2 = new Parameter("sigma R", "Estimated standard deviation of the background in the RED channel", "1.5");
		Parameter p3 = new Parameter("sigma G", "Estimated standard deviation of the background in the GREEN channel", "1.5");
		Parameter p4 = new Parameter("sigma B", "Estimated standard deviation of the background in the BLUE channel", "1.5");
		Parameter p5 = new Parameter("n", "An integer that represents how many multiples of sigma to add to the normalization signal before normalizing. Helps avoid over estimating fold change due to noise.", "3");
		Parameter p6 = new Parameter("Dmax", "Maximum pixel radius of a cell to be considered a match", "350");
		// Parameter p4 = new Parameter("Time Dim",
		// "Name of the dimension along which points will be linked (typicall time)",
		// "Time");
		Parameter p8 = new Parameter("Track/Point/ID Dim Name", "Name of the dim that currently identifies each point.", "Track");
		Parameter p9 = new Parameter("Time Dim Name", "Name of the time dimension.", "Time");
		Parameter p10 = new Parameter("Create Histograms", "should histograms be created for the distance metrics at each time point?", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		// parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
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
		JEXData fileData = inputs.get("Time Files");
		if(fileData == null || !fileData.getTypeName().getType().matches(JEXData.FILE))
			return false;
		
		// Gather parameters
		double W = Double.parseDouble(parameters.getValueOfParameter("W"));
		double sigR = Double.parseDouble(parameters.getValueOfParameter("sigma R"));
		double sigG = Double.parseDouble(parameters.getValueOfParameter("sigma G"));
		double sigB = Double.parseDouble(parameters.getValueOfParameter("sigma B"));
		int n = Integer.parseInt(parameters.getValueOfParameter("n"));
		double Dmax = Integer.parseInt(parameters.getValueOfParameter("Dmax"));
		// double FCmax =
		// Double.parseDouble(parameters.getValueOfParameter("FCmax"));
		String pointDimName = parameters.getValueOfParameter("Track/Point/ID Dim Name"); // Used
		// to
		// get
		// the
		// point
		// from
		// the
		// file,
		// New
		// Tracks
		// always
		// given
		// dim
		// name
		// of
		// "Track"
		String timeDimName = parameters.getValueOfParameter("Time Dim Name");
		boolean createHistograms = Boolean.parseBoolean(parameters.getValueOfParameter("Create Histograms"));
		
		// Run the function
		DimTable fileDimTable = fileData.getDimTable();
		
		// Track first time using only x-y position and look at distribution of
		// distance and fold-change
		// to discover the appropriate scaling factors for weighting.
		TreeMap<DimensionMap,Double> oldMedianDs = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> oldMedianFCs = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> newMedianDs = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> newMedianFCs = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,String> oldHistDs = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> oldHistFCs = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> newHistDs = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> newHistFCs = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> fileMap = FileReader.readObjectToFilePathTable(fileData);
		TreeMap<DimensionMap,String> outputFileMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		DataPointTracker tracker1 = new DataPointTracker();
		tracker1.setParams(1.0, sigR, sigG, sigB, percentage, Dmax);
		DataPointTracker tracker2 = new DataPointTracker();
		for (DimensionMap map : fileDimTable.getMapIterator())
		{
			if(this.isCanceled() == true)
			{
				return false;
			}
			
			// Perform tracking based only on position
			Table<Double> timePoint = JEXTableReader.getNumericTable(fileMap.get(map));
			tracker1.addTimePoint(timePoint, map.get(timeDimName), pointDimName, createHistograms);
			oldMedianDs.put(map, tracker1.medianD);
			oldMedianFCs.put(map, tracker1.medianFC);
			oldHistDs.put(map, tracker1.histD);
			oldHistFCs.put(map, tracker1.histFC);
			
			// Refine weighting based on ratio of magnitudes of values
			double adjustedW = W * (tracker1.medianFC / tracker1.medianD);
			
			// Redo tracking with refined weighting
			tracker2.setParams(adjustedW, sigR, sigG, sigB, n, Dmax);
			tracker2.addTimePoint(timePoint, map.get(timeDimName), pointDimName, createHistograms);
			newMedianDs.put(map, tracker2.medianD);
			newMedianFCs.put(map, tracker2.medianFC);
			newHistDs.put(map, tracker2.histD);
			newHistFCs.put(map, tracker2.histFC);
			TreeMap<DimensionMap,Double> trackedTimePoint = tracker2.getTrackedTimePoint();
			String path = JEXTableWriter.writeTable("TrackedTimePoint", trackedTimePoint);
			outputFileMap.put(map, path);
			
			// Update status indicator
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) fileDimTable.mapCount())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), null, outputFileMap);
		JEXData output2 = ImageWriter.makeImageStackFromPaths(outputNames[1].getName(), oldHistDs);
		JEXData output3 = ImageWriter.makeImageStackFromPaths(outputNames[2].getName(), oldHistFCs);
		JEXData output4 = ImageWriter.makeImageStackFromPaths(outputNames[3].getName(), newHistDs);
		JEXData output5 = ImageWriter.makeImageStackFromPaths(outputNames[4].getName(), newHistFCs);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		realOutputs.add(output4);
		realOutputs.add(output5);
		
		// Return status
		return true;
	}
	
}
