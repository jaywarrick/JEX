package function.plugin.old;

import image.roi.XTrajectorySet;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.TrackReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.GraphicalCrunchingEnabling;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.TrackStatistics;

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
public class JEX_Migration_SimpleAnalyzeTrack extends JEXCrunchable {
	
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
		String result = "7. Analyze distance of track";
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
		String result = "Analyze migration distances for tracked cells.";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(TRACK, "Tracks to Analyze");
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
		defaultOutputNames = new TypeName[6];
		defaultOutputNames[0] = new TypeName(VALUE, "Total distance");
		defaultOutputNames[1] = new TypeName(VALUE, "Effective distance");
		defaultOutputNames[2] = new TypeName(VALUE, "Chemotactic index");
		defaultOutputNames[3] = new TypeName(VALUE, "Values table");
		
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
		Parameter p4 = new Parameter("Frames per vector", "For better smoothing you can increase the number", "1");
		Parameter p6 = new Parameter("Angle offset", "Rotate the angle histograms", "90");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p6);
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
		
		// Run the function
		Logs.log("Running the function", 1, this);
		AnalyzeDistanceTrackHelperFunction graphFunc = new AnalyzeDistanceTrackHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		
		// Collect the outputs
		Logs.log("Collecting outputs", 1, this);
		
		// Mean velocity
		JEXData output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + graphFunc.meanTotalDistance);
		output1.setDataObjectInfo("Value determined using Simple Track Analyzer function");
		realOutputs.add(output1);
		
		// Mean angle
		JEXData output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + graphFunc.meanEffectiveDistance);
		output2.setDataObjectInfo("Value determined using Simple Track Analyzer function");
		realOutputs.add(output2);
		
		// Chemotactic index
		JEXData output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + graphFunc.chemoIndex);
		output3.setDataObjectInfo("Value determined using Simple Track Analyzer function");
		realOutputs.add(output3);
		
		// Table sheet
		String[] columns = new String[] { "Total distance", "Effective Distance", "Chemotaxis Index" };
		HashMap<String,String[]> columnData = new HashMap<String,String[]>();
		
		List<Double> totalDistancePerCell = graphFunc.totalDistancePerCell;
		String[] totalDistancePerCellArray = new String[totalDistancePerCell.size()];
		for (int i = 0, len = totalDistancePerCell.size(); i < len; i++)
		{
			Double a = totalDistancePerCell.get(i);
			totalDistancePerCellArray[i] = "" + a;
		}
		List<Double> effectiveDistancePerCell = graphFunc.effectiveDistancePerCell;
		String[] effectiveDistancePerCellArray = new String[effectiveDistancePerCell.size()];
		for (int i = 0, len = effectiveDistancePerCell.size(); i < len; i++)
		{
			Double v = effectiveDistancePerCell.get(i);
			effectiveDistancePerCellArray[i] = "" + v;
		}
		List<Double> ciPerCell = graphFunc.ciPerCell;
		String[] ciPerCellArray = new String[ciPerCell.size()];
		for (int i = 0, len = ciPerCell.size(); i < len; i++)
		{
			Double v = ciPerCell.get(i);
			ciPerCellArray[i] = "" + v;
		}
		JEXData output4 = ValueWriter.makeValueTable(outputNames[3].getName(), columns, columnData);
		
		realOutputs.add(output4);
		
		// Return status
		return true;
	}
	
}

class AnalyzeDistanceTrackHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	boolean auto = true;
	int mode;
	int nbVect;
	int stepSize;
	double maxVel;
	int angleOff;
	
	ParameterSet parameters;
	JEXData tracks;
	// JEXTrack track ;
	JEXEntry entry;
	TrackStatistics stats;
	XTrajectorySet set;
	
	double meanTotalDistance;
	double meanEffectiveDistance;
	double chemoIndex;
	List<Double> totalDistancePerCell;
	List<Double> effectiveDistancePerCell;
	List<Double> ciPerCell;
	
	AnalyzeDistanceTrackHelperFunction(JEXEntry entry, JEXData tracks, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.tracks = tracks;
		this.parameters = parameters;
		this.entry = entry;
		
		// //// Get params
		String autoStr = parameters.getValueOfParameter("Automatic");
		auto = Boolean.parseBoolean(autoStr);
		mode = TrackStatistics.FULLTRACKS;
		stepSize = Integer.parseInt(parameters.getValueOfParameter("Frames per vector"));
		angleOff = Integer.parseInt(parameters.getValueOfParameter("Angle offset"));
		
		// Call the calculation function and set it up
		set = TrackReader.readObjectToTrajectorySet(tracks);
		Logs.log("Found " + set.getTrajectories().size(), 1, this);
		
		// Set up the calculation functions
		stats = new TrackStatistics(set);
		stats.deltaFrame = stepSize;
		stats.nbCells = nbVect;
		stats.angleOffset = angleOff;
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public boolean doit()
	{
		boolean b = true;
		
		stats.startAnalysis();
		meanTotalDistance = stats.meanTotalDisplacement;
		meanEffectiveDistance = stats.meanDisplacement;
		chemoIndex = stats.CI;
		
		totalDistancePerCell = stats.totalDist;
		effectiveDistancePerCell = stats.distTrav;
		ciPerCell = stats.CIs;
		
		return b;
	}
	
	/**
	 * Normalize a degrees angle between 0 (included) and 360 (excluded)
	 * 
	 * @param a
	 * @return
	 */
	public static double normalizeAngle(double a)
	{
		double result = a;
		while (result < 0 || result >= 360)
		{
			if(result < 0)
				result = result + 360;
			if(result >= 360)
				result = result - 360;
		}
		return result;
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
