package function.experimentalDataProcessing;

import ij.gui.Roi;
import image.roi.ROIPlus;
import image.roi.Trajectory;
import image.roi.Vect;
import image.roi.XTrajectorySet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;
import utilities.StringUtility;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataReader.TrackReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.ExperimentalDataCrunch;
import function.GraphicalCrunchingEnabling;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.TrajectoryStatistics;

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
public class JEX_Migration_AnalyzeTracksTemporal extends ExperimentalDataCrunch {
	
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
		String result = "7. Temporal track analysis";
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
		String result = "Measure variations of track characteristics through time.";
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
		String toolbox = "Migration";
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
		inputNames[0] = new TypeName(TRACK, "Tracks to Analyze");
		inputNames[1] = new TypeName(ROI, "Optional ROI");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(VALUE, "Temporal value table");
		defaultOutputNames[1] = new TypeName(VALUE, "Rate of incomming");
		
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
		Parameter p1 = new Parameter("Automatic", "Use automated edge determination algorithms", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p2 = new Parameter("Window size", "Size of the sliding window", "1");
		Parameter p3 = new Parameter("Angle offset", "Rotate the angle histograms", "90");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
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
		Logs.log("Collecting inputs", 1, this);
		JEXData data = inputs.get("Tracks to Analyze");
		if(!data.getTypeName().getType().equals(JEXData.TRACK))
			return false;
		
		JEXData roiData = inputs.get("Optional ROI");
		
		// Run the function
		Logs.log("Running the function", 1, this);
		AdvancedAnalyzeTracksHelperFunction graphFunc = new AdvancedAnalyzeTracksHelperFunction(entry, data, roiData, outputNames, parameters);
		graphFunc.doit();
		
		// Collect the outputs
		Logs.log("Collecting outputs", 1, this);
		
		// Mean velocity
		String[] columns = new String[] { "Velocities", "Angles", "Chemotaxis Indexes" };
		HashMap<String,List<Double>> columnData = new HashMap<String,List<Double>>();
		columnData.put("Angles", graphFunc.angles);
		columnData.put("Velocities", graphFunc.velocities);
		columnData.put("Chemotaxis Indexes", graphFunc.cis);
		JEXData output = ValueWriter.makeValueTableFromDoubleList(outputNames[0].getName(), columns, columnData);
		realOutputs.add(output);
		realOutputs.add(graphFunc.rateOutput);
		
		// Return status
		return true;
	}
	
}

class AdvancedAnalyzeTracksHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	boolean auto = true;
	int windowSize;
	int angleOff;
	
	ParameterSet parameters;
	List<Trajectory> trajectories;
	TreeMap<DimensionMap,ROIPlus> rois;
	Roi roi = null;
	TypeName[] outputNames;
	
	public List<Double> velocities;
	public List<Double> cis;
	public List<Double> angles;
	public List<Double> wangles;
	
	// outputs
	public JEXData rateOutput;
	
	AdvancedAnalyzeTracksHelperFunction(JEXEntry entry, JEXData tracks, JEXData roiData, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.parameters = parameters;
		this.velocities = new ArrayList<Double>(0);
		this.cis = new ArrayList<Double>(0);
		this.angles = new ArrayList<Double>(0);
		this.wangles = new ArrayList<Double>(0);
		this.outputNames = outputNames;
		
		// read roi
		this.roi = null;
		if(roiData != null)
		{
			rois = RoiReader.readObjectToRoiMap(roiData);
			ROIPlus roip = RoiReader.readObjectToRoi(roiData);
			this.roi = (roip == null) ? null : roip.getRoi();
		}
		
		// //// Get params
		String autoStr = parameters.getValueOfParameter("Automatic");
		auto = Boolean.parseBoolean(autoStr);
		windowSize = Integer.parseInt(parameters.getValueOfParameter("Window size"));
		angleOff = Integer.parseInt(parameters.getValueOfParameter("Angle offset"));
		
		// Call the calculation function and set it up
		XTrajectorySet set = TrackReader.readObjectToTrajectorySet(tracks);
		trajectories = set.getTrajectories();
		Logs.log("Found " + set.getTrajectories().size(), 1, this);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		// Find size of timelapse
		int size = 0;
		for (Trajectory traj : trajectories)
		{
			int thisSize = traj.length();
			size = (thisSize > size) ? thisSize : size;
		}
		
		// find all the velocities
		for (int i = 0; i < size - windowSize; i++)
		{
			addValue(i);
		}
		
		// analyze the rate of incomming
		// Find the maximum length
		int length = 0;
		for (int i = 0, len = trajectories.size(); i < len; i++)
		{
			Trajectory traj = trajectories.get(i);
			length = Math.max(length, traj.length());
		}
		
		// Loop through the time points
		List<String> colNamesList = new ArrayList<String>(0);
		List<String> rowNamesList = new ArrayList<String>(0);
		HashMap<String,String[]> columnsList = new HashMap<String,String[]>();
		List<Integer> rateTemp = new ArrayList<Integer>(0);
		for (int j = 0; j < length; j++)
		{
			// Get the number of points that have entered the ROI
			int numberOfTracks = 0;
			
			// Loop through the trajectories
			for (int i = 0, len = trajectories.size(); i < len; i++)
			{
				// Get trajectory number i
				Trajectory traj = trajectories.get(i);
				
				// If point wasn't in roi and then was add it to the list
				Point p1 = traj.getPoint(j);
				Point p2 = traj.getPoint(j + 1);
				
				if(roi == null)
					break;
				else if(p1 == null || p2 == null)
					continue;
				else if(!roi.contains(p1.x, p1.y) && roi.contains(p2.x, p2.y))
				{
					numberOfTracks = numberOfTracks + 1;
				}
				else if(roi.contains(p1.x, p1.y) && !roi.contains(p2.x, p2.y))
				{
					numberOfTracks = numberOfTracks - 1;
				}
			}
			rateTemp.add(numberOfTracks);
			
			// Make absiceses
		}
		
		// DO the window averaging
		List<String> rateTempString = new ArrayList<String>(0);
		// for (int j=0; j<rateTemp.size(); j++)
		// {
		// int r = 0;
		// for (int h=0; h<this.windowSize; h++)
		// {
		// int index = j + h;
		// if (index >= rateTemp.size()) continue;
		// r = r + rateTemp.get(index);
		// }
		// rateTempString.add(""+r);
		// }
		int j = 0;
		while (j < rateTemp.size())
		{
			int r = 0;
			for (int h = 0; h < this.windowSize; h++)
			{
				int index = j + h;
				j = j + 1;
				if(index >= rateTemp.size())
					continue;
				r = r + rateTemp.get(index);
			}
			rateTempString.add("" + r);
			rowNamesList.add("Frame " + StringUtility.fillLeft("" + j, 3, "0"));
		}
		
		// Add first colum
		colNamesList.add("Frame");
		columnsList.put("Frame", rowNamesList.toArray(new String[0]));
		
		// Add rate column
		String[] rateCol = rateTempString.toArray(new String[0]);
		colNamesList.add("Rate of incoming");
		columnsList.put("Rate of incoming", rateCol);
		
		String[] columnArrayNames = colNamesList.toArray(new String[0]);
		String[] rowArraynames = rowNamesList.toArray(new String[0]);
		rateOutput = ValueWriter.makeValueTable(outputNames[1].getName(), columnArrayNames, rowArraynames, columnsList);
		rateOutput.put(JEXEntry.INFO, "Created from POCT analysis v2");
		
		return;
	}
	
	public void addValue(int index)
	{
		double velocity = 0;
		double ci = 0;
		double angle = 0;
		Vect vector = new Vect();
		int number = 0;
		
		for (Trajectory traj : trajectories)
		{
			TrajectoryStatistics stats = new TrajectoryStatistics(traj);
			stats.startAnalysis(index, index + windowSize);
			
			int size = stats.vectors.size();
			if(size == 0)
				continue;
			
			velocity = velocity + stats.meanVelocity;
			ci = ci + stats.CI;
			vector.add(stats.meanVector);
			number = number + 1;
		}
		
		velocity = velocity / number;
		ci = ci / number;
		vector.multiply(1 / (double) number);
		angle = vector.angle();
		
		velocities.add(velocity);
		wangles.add(velocity);
		angles.add(angle);
		cis.add(ci);
	}
	
	public void runStep(int index)
	{}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return 0;
	}
	
	public void startIT()
	{}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT()
	{}
	
	public void loopNext()
	{}
	
	public void loopPrevious()
	{}
	
	public void recalculate()
	{}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
