package function.experimentalDataProcessing;

import image.roi.Vect;
import image.roi.XTrajectorySet;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.TrackReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;
import function.GraphicalCrunchingEnabling;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.HistogramFactory;
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
public class JEX_Migration_AnalyzeTracks extends ExperimentalDataCrunch {
	
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
		String result = "7. Analyze Tracks";
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
		String result = "Analyze features of tracks such as mean velocity and directionality.";
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
		defaultOutputNames = new TypeName[7];
		defaultOutputNames[0] = new TypeName(VALUE, "Mean velocity");
		defaultOutputNames[1] = new TypeName(VALUE, "Mean angle");
		defaultOutputNames[2] = new TypeName(VALUE, "Chemotactic index");
		defaultOutputNames[3] = new TypeName(IMAGE, "Velocity Histogram");
		defaultOutputNames[4] = new TypeName(IMAGE, "Angle Histogram");
		defaultOutputNames[5] = new TypeName(VALUE, "Value table");
		defaultOutputNames[6] = new TypeName(VALUE, "Persisence time");
		
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
		Parameter p2 = new Parameter("Single vectors?", "Perform calculations on every single displacement vector instead of full tracks", Parameter.DROPDOWN, new String[] { "Yes", "No" }, 1);
		Parameter p3 = new Parameter("Number of vectors", "If single vector calculation is selected, then choose how many vectors to keep", "150");
		Parameter p4 = new Parameter("Frames per vector", "For better smoothing you can increase the number", "1");
		Parameter p5 = new Parameter("Maximum velocity", "For outputting normalized data entrer a maximum velocity or -1 to deselect this feature", "-1");
		Parameter p6 = new Parameter("Angle offset", "Rotate the angle histograms", "90");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
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
		JEXData data = inputs.get("Tracks to Analyze");
		if(!data.getTypeName().getType().equals(JEXData.TRACK))
			return false;
		
		// Run the function
		Logs.log("Running the function", 1, this);
		AnalyzeTracksHelperFunction graphFunc = new AnalyzeTracksHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		
		// Collect the outputs
		Logs.log("Collecting outputs", 1, this);
		
		// Mean velocity
		JEXData output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + graphFunc.meanVelocity);
		output1.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output1);
		
		JEXData output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + graphFunc.meanAngle);
		output2.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output2);
		
		JEXData output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + graphFunc.chemoIndex);
		output3.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output3);
		
		// Velocity histogram
		List<Double> velocities = graphFunc.velocities;
		HistogramFactory hfact = new HistogramFactory();
		hfact.title = "Velocity Histogram";
		hfact.numberOfBins = 8;
		hfact.coord = HistogramFactory.CART_HISTOGRAM;
		hfact.maxX = Double.parseDouble(parameters.getValueOfParameter("Maximum velocity"));
		BufferedImage im = hfact.makeCartesianHistogram(velocities);
		String vhPath = JEXWriter.saveFigure(im, "jpg"); // this function prints
		// to logmanager
		
		JEXData output4 = ImageWriter.makeImageObject(outputNames[3].getName(), vhPath);
		output4.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output4);
		
		// Angle Histogram
		List<Double> wangleA = graphFunc.wanglesA;
		List<Double> wangleV = graphFunc.wanglesV;
		List<Double> distTrav = graphFunc.distTrav;
		List<Double> pTimes = graphFunc.persistenceTimes;
		List<Double> CIs = graphFunc.CIs;
		HistogramFactory hfact2 = new HistogramFactory();
		hfact.title = "Wangle Histogram";
		hfact.numberOfBins = 8;
		hfact.coord = HistogramFactory.POLAR_HISTOGRAM;
		BufferedImage im2 = hfact2.makeWeighedPolarHistogram(wangleA, wangleV);
		String vhPath2 = JEXWriter.saveFigure(im2, "jpg"); // this function
		// prints to
		// logmanager
		
		JEXData output5 = ImageWriter.makeImageObject(outputNames[4].getName(), vhPath2);
		output5.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output5);
		
		// Table sheet
		String[] columns = new String[] { "Angles", "Velocities", "Distance travelled", "CIs", "Persistence time" };
		HashMap<String,List<Double>> columnData = new HashMap<String,List<Double>>();
		columnData.put("Angles", wangleA);
		columnData.put("Velocities", wangleV);
		columnData.put("Distance travelled", distTrav);
		columnData.put("Persistence time", pTimes);
		columnData.put("CIs", CIs);
		JEXData output6 = ValueWriter.makeValueTableFromDoubleList(outputNames[5].getName(), columns, columnData);
		output6.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output6);
		
		// Persistence time
		JEXData output7 = ValueWriter.makeValueObject(outputNames[6].getName(), "" + graphFunc.persistenceTime);
		output7.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output7);
		
		// Return status
		return true;
	}
	
}

class AnalyzeTracksHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	boolean auto = true;
	int mode;
	int nbVect;
	int stepSize;
	double maxVel;
	int angleOff;
	
	ParameterSet parameters;
	JEXData tracks;
	JEXEntry entry;
	TrackStatistics stats;
	XTrajectorySet set;
	
	double meanVelocity;
	double meanAngle;
	double chemoIndex;
	double persistenceTime;
	List<Double> velocities;
	List<Double> angles;
	List<Vect> vectors;
	List<Double> wanglesA;
	List<Double> wanglesV;
	List<Double> distTrav;
	List<Double> CIs;
	List<Double> persistenceTimes;
	
	AnalyzeTracksHelperFunction(JEXEntry entry, JEXData tracks, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.tracks = tracks;
		this.parameters = parameters;
		this.entry = entry;
		
		// //// Get params
		String autoStr = parameters.getValueOfParameter("Automatic");
		String modeStr = parameters.getValueOfParameter("Single vectors?");
		auto = Boolean.parseBoolean(autoStr);
		mode = (modeStr.equals("yes")) ? TrackStatistics.SINGLEDISPLACEMENT : TrackStatistics.FULLTRACKS;
		nbVect = Integer.parseInt(parameters.getValueOfParameter("Number of vectors"));
		stepSize = Integer.parseInt(parameters.getValueOfParameter("Frames per vector"));
		maxVel = Double.parseDouble(parameters.getValueOfParameter("Maximum velocity"));
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
		meanVelocity = stats.meanVelocity;
		meanAngle = stats.meanAngle;
		chemoIndex = stats.CI;
		CIs = stats.CIs;
		velocities = stats.velocities;
		angles = stats.angles;
		vectors = stats.vectors;
		persistenceTime = stats.persistenceTime;
		persistenceTimes = stats.persistenceTimes;
		
		wanglesA = new Vector<Double>(0);
		wanglesV = new Vector<Double>(0);
		distTrav = new Vector<Double>(0);
		for (int i = 0, len = vectors.size(); i < len; i++)
		{
			Vect v = vectors.get(i);
			if(v.norm() == 0)
				continue;
			Double a = v.angle() + angleOff;
			Double w = v.norm() / len;
			a = normalizeAngle(a);
			wanglesA.add(a);
			wanglesV.add(w);
		}
		distTrav = stats.distTrav;
		
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
