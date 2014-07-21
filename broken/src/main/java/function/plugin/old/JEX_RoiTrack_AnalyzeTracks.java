package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataReader.RoiTrackReader;
import Database.DataReader.TrackReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import function.roitracker.RoiTrack;
import function.tracker.Axis;
import function.tracker.HistogramFactory;
import function.tracker.SingleTrackStatistics;
import ij.ImagePlus;
import ij.gui.Roi;
import image.roi.ROIPlus;
import image.roi.Trajectory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logs.Logs;
import miscellaneous.VectorUtility;

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
public class JEX_RoiTrack_AnalyzeTracks extends JEXCrunchable {
	
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
		String result = "Analyze Roi Tracks";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Analyze track characteristics.";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	public String getToolbox()
	{
		String toolbox = "Roi tracking";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(TRACK, "Tracks");
		inputNames[1] = new TypeName(ROI, "Point source");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[14];
		defaultOutputNames[0] = new TypeName(VALUE, "Mean velocity");
		defaultOutputNames[1] = new TypeName(VALUE, "Mean directional velocity");
		defaultOutputNames[2] = new TypeName(VALUE, "Mean angle");
		defaultOutputNames[3] = new TypeName(VALUE, "Mean Displacement");
		defaultOutputNames[4] = new TypeName(VALUE, "Chemotactic index");
		defaultOutputNames[5] = new TypeName(VALUE, "Meandering index");
		defaultOutputNames[6] = new TypeName(VALUE, "Persisence time");
		defaultOutputNames[7] = new TypeName(VALUE, "Percent forward moving cells");
		
		defaultOutputNames[8] = new TypeName(VALUE, "Velocity table");
		defaultOutputNames[9] = new TypeName(VALUE, "Displacement table");
		defaultOutputNames[10] = new TypeName(VALUE, "Angle table");
		
		defaultOutputNames[11] = new TypeName(IMAGE, "Velocity Histogram");
		defaultOutputNames[12] = new TypeName(IMAGE, "Angle Histogram");
		defaultOutputNames[13] = new TypeName(VALUE, "Value table");
		
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
		Parameter p1 = new Parameter("Temporal binning", "For better smoothing you can increase the number", "1");
		Parameter p2 = new Parameter("Maximum velocity", "For outputting normalized data entrer a maximum velocity or -1 to deselect this feature", "-1");
		Parameter p3 = new Parameter("Seconds per frame", "Duration between each frame of the timelapse", "1");
		Parameter p4 = new Parameter("Micron per pixel", "Number of microns per pixel", "1");
		Parameter p5 = new Parameter("Type of chemotaxis", "What is the direction of the chemoattractant stimulus", Parameter.DROPDOWN, new String[] { "left-right" , "right-left" , "top-bottom" , "bottom-top" , "point source" , "random" });
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);  
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
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
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		Logs.log("Collecting inputs", 1, this);
		
		JEXData trackData = inputs.get("Roi Tracks");
		if(!trackData.getTypeName().getType().equals(JEXData.ROI_TRACK)) return false;
		
		JEXData pointData = inputs.get("Point source");
		if(!pointData.getTypeName().getType().equals(JEXData.ROI)) return false;
		
		// Run the function
		Logs.log("Running the function", 1, this);
		
		// Collect the parameters
		int timeBinning  = Integer.parseInt(parameters.getValueOfParameter("Temporal binning"));
		int maxVelocity  = Integer.parseInt(parameters.getValueOfParameter("Maximum velocity"));
		int spf          = Integer.parseInt(parameters.getValueOfParameter("Seconds per frame"));
		int mpp          = Integer.parseInt(parameters.getValueOfParameter("Micron per pixel"));
		String typeChemo = parameters.getValueOfParameter("Type of chemotaxis");
		
		// convert jex datas into a useable format
		List<RoiTrack> tracks = RoiTrackReader.readObjectToRoiTrackList(trackData);
		ROIPlus roi           = RoiReader.readObjectToRoi(pointData);
		Point source          = (roi == null) ? null : roi.getPointList().getCenter();
		Logs.log("Found " + tracks.size(), 1, this);
		
		// Set up the variables
		List<Double> velocities        = new ArrayList<Double>();
		List<Double> chemovelocities   = new ArrayList<Double>();
		List<Double> angles            = new ArrayList<Double>();
		List<Double> displacements     = new ArrayList<Double>();
		List<Double> CIs               = new ArrayList<Double>();
		List<Double> MCIs              = new ArrayList<Double>();
		List<Double> persistenceTimes  = new ArrayList<Double>();
		List<Double> startinPositionXs = new ArrayList<Double>();
		List<Double> startinPositionYs = new ArrayList<Double>();
		double meanVelocity            = 0 ; 		// The mean velocity
		double meanChemoVelocity       = 0 ; 		// The mean chemotactic velocity
		double meanAngle               = 0 ; 		// The mean angle
		double meanDisplacement        = 0 ; 		// The mean displacement
		double meanCI                  = 0 ; 		// The chemotactic index
		double meanMCI                 = 0 ; 		// The meandering index
		double persistenceTime         = 0 ; 		// Persistence time
		double percetForward           = 0 ; 		// Percent cells migrating forward				
		double forwardCells            = 0 ;
		
		defaultOutputNames[0] = new TypeName(VALUE, "Mean velocity");
		defaultOutputNames[1] = new TypeName(VALUE, "Mean directional velocity");
		defaultOutputNames[2] = new TypeName(VALUE, "Mean angle");
		defaultOutputNames[3] = new TypeName(VALUE, "Mean Displacement");
		defaultOutputNames[4] = new TypeName(VALUE, "Chemotactic index");
		defaultOutputNames[5] = new TypeName(VALUE, "Meandering index");
		defaultOutputNames[6] = new TypeName(VALUE, "Persisence time");
		defaultOutputNames[7] = new TypeName(VALUE, "Percent forward moving cells");
		
		defaultOutputNames[8] = new TypeName(VALUE, "Velocity table");
		defaultOutputNames[9] = new TypeName(VALUE, "Displacement table");
		defaultOutputNames[10] = new TypeName(VALUE, "Angle table");
		
		defaultOutputNames[11] = new TypeName(IMAGE, "Velocity Histogram");
		defaultOutputNames[12] = new TypeName(IMAGE, "Angle Histogram");
		defaultOutputNames[13] = new TypeName(VALUE, "Value table");

		// Set up the calculation functions
		for (RoiTrack track : tracks)
		{
			// Get the oringin point and determine the axis of migration
			Point pOrigin = track.getFirst();
			Axis migrationAxis = new Axis(pOrigin, new Point((int) xpos, (int) ypos));

			// Create a new track statistic instance
			SingleTrackStatistics stats = new SingleTrackStatistics();
			stats.setAxis(migrationAxis);
			stats.setBinning(stepSize);
			stats.setMicronsPerPixel(mpp);
			stats.setSecondsPerFrame(spf);
			stats.setNumberOfBinsForSCI(12);
			stats.setMigrationType(SingleTrackStatistics.MIGRATION_POINT_SOURCE);
			stats.setTrack(track);

			// Add values to the list vectors
			velocities.add(stats.meanVelocity);
			chemovelocities.add(stats.meanChemoVelocity);
			angles.add(stats.meanAngle);
			displacements.add(stats.meanDisplacement);
			CIs.add(stats.CI);
			MCIs.add(stats.MCI);
			SCIs.add(stats.SCI);
			xDispRates.add(stats.xDispRate);
			yDispRates.add(stats.yDispRate);
			persistenceTimes.add(stats.persistenceTime);
			startinPositionXs.add(stats.startinPositionX);
			startinPositionYs.add(stats.startinPositionY);

			Point first = track.getFirst();
			Point last  = track.getLast();
			if (first.y - last.y > 0) forwardCells = forwardCells + 1;
		}
		percetForward = forwardCells / trajectories.size();

		// Display stuff
		Logs.log("Finished analyzing trajectories", 1, this);

		// Calculate the mean values
		meanVelocity = VectorUtility.mean(velocities); // The mean velocity
		meanChemoVelocity = VectorUtility.mean(chemovelocities); // The mean chemotactic velocity
		meanAngle = VectorUtility.mean(angles); // The mean angle
		meanDisplacement = VectorUtility.mean(displacements); // The mean
		// displacement
		meanCI = VectorUtility.mean(CIs); // The chemotactic index
		meanMCI = VectorUtility.mean(MCIs); // The meandering index
		meanSCI = VectorUtility.mean(SCIs); // The segmented chemotactic index
		meanxDispRate = VectorUtility.mean(xDispRates); // The x displacement
		// rate
		meanyDispRate = VectorUtility.mean(yDispRates); // The y displacement
		// rate
		persistenceTime = VectorUtility.mean(persistenceTimes); // Persistence
		// time




		
		
		PointSourceHelperFunction graphFunc = new PointSourceHelperFunction(entry, trackData, imageData, outputNames, parameters);
		graphFunc.doit();
		
		// Collect the outputs
		Logs.log("Collecting outputs", 1, this);
		
		// Mean velocity
		JEXData output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + graphFunc.meanVelocity);
		output1.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output1);
		
		JEXData output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + graphFunc.meanxDispRate);
		output2.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output2);
		
		JEXData output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + graphFunc.meanyDispRate);
		output3.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output3);
		
		JEXData output4 = ValueWriter.makeValueObject(outputNames[3].getName(), "" + graphFunc.meanAngle);
		output4.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output4);
		
		JEXData output5 = ValueWriter.makeValueObject(outputNames[4].getName(), "" + graphFunc.meanDisplacement);
		output5.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output5);
		
		JEXData output6 = ValueWriter.makeValueObject(outputNames[5].getName(), "" + graphFunc.meanCI);
		output6.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output6);
		
		JEXData output7 = ValueWriter.makeValueObject(outputNames[6].getName(), "" + graphFunc.meanMCI);
		output7.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output7);
		
		JEXData output8 = ValueWriter.makeValueObject(outputNames[7].getName(), "" + graphFunc.persistenceTime);
		output8.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output8);
		
		JEXData output9 = ValueWriter.makeValueObject(outputNames[8].getName(), "" + graphFunc.percetForward);
		output9.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output9);
		
		// Velocity histogram
		List<Double> velocities = graphFunc.velocities;
		HistogramFactory hfact = new HistogramFactory();
		hfact.title = "Velocity Histogram";
		hfact.numberOfBins = 8;
		hfact.coord = HistogramFactory.CART_HISTOGRAM;
		BufferedImage im = hfact.makeCartesianHistogram(velocities);
		String vhPath = JEXWriter.saveFigure(im, "jpg"); // this function prints
		// to logmanager
		
		JEXData output10 = ImageWriter.makeImageObject(outputNames[9].getName(), vhPath);
		output10.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output10);
		
		// Angle Histogram
		List<Double> angles = graphFunc.angles;
		List<Double> hist = HistogramFactory.makeAbscHistorgram(8, 0, 360);
		List<Double> bins = new ArrayList<Double>();
		for (int i = 0; i < hist.size(); i++)
			bins.add((double) 0);
		for (int i = 0; i < angles.size(); i++)
		{
			// add to the angle histogram
			double angle = SingleTrackStatistics.normalizeAngle(angles.get(i));
			double velocity = velocities.get(i);
			int index = HistogramFactory.findIndexHistogram(angle, hist);
			if(index >= 0 && index < bins.size())
				bins.set(index, velocity + bins.get(index));
		}
		
		HistogramFactory hfact2 = new HistogramFactory();
		hfact.title = "Wangle Histogram";
		hfact.numberOfBins = 8;
		hfact.coord = HistogramFactory.POLAR_HISTOGRAM;
		BufferedImage im2 = hfact2.makeWeighedPolarHistogram(hist, bins);
		String vhPath2 = JEXWriter.saveFigure(im2, "jpg"); // this function
		// prints to
		// logmanager
		
		JEXData output11 = ImageWriter.makeImageObject(outputNames[10].getName(), vhPath2);
		output11.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output11);
		
		// Table sheet
		String[] columns = new String[] { "Start X", "Start Y", "Velocities", "Angles", "Distance travelled", "CIs", "MCIs", "SCIs", "X-Displacement rate", "Y-Displacement rate", "Persistence time" };
		HashMap<String,List<Double>> columnData = new HashMap<String,List<Double>>();
		
		columnData.put("Start X", graphFunc.startinPositionXs);
		columnData.put("Start Y", graphFunc.startinPositionYs);
		columnData.put("Velocities", graphFunc.velocities);
		columnData.put("Angles", graphFunc.angles);
		columnData.put("Distance travelled", graphFunc.displacements);
		columnData.put("CIs", graphFunc.CIs);
		columnData.put("MCIs", graphFunc.MCIs);
		columnData.put("SCIs", graphFunc.SCIs);
		columnData.put("X-Displacement rate", graphFunc.xDispRates);
		columnData.put("Y-Displacement rate", graphFunc.yDispRates);
		columnData.put("Persistence time", graphFunc.persistenceTimes);
		
		JEXData output12 = ValueWriter.makeValueTableFromDoubleList(outputNames[11].getName(), columns, columnData);
		output12.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output12);
		
		JEXData output13 = ValueWriter.makeValueObject(outputNames[12].getName(), "" + graphFunc.meanChemoVelocity);
		output13.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output13);
		
		// Return status
		return true;
	}
	
}

class PointSourceHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// GUI variables
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	
	boolean auto = true;
	int stepSize = 1;
	double spf = 1;
	double mpp = 1;
	double maxVel;
	
	ParameterSet parameters;
	JEXData tracks;
	JEXData timelapse;
	JEXEntry entry;
	private List<Trajectory> trajectories;
	Point source = null;
	double xpos = -1;
	double ypos = -1;
	
	public List<Double> velocities;
	public List<Double> chemovelocities;
	public List<Double> angles;
	public List<Double> displacements;
	public List<Double> CIs;
	public List<Double> MCIs;
	public List<Double> SCIs;
	public List<Double> xDispRates;
	public List<Double> yDispRates;
	public List<Double> persistenceTimes;
	public List<Double> startinPositionXs;
	public List<Double> startinPositionYs;
	public double meanVelocity; 			// The mean velocity
	public double meanChemoVelocity ; 		// The mean chemotactic velocity
	public double meanAngle; 				// The mean angle
	public double meanDisplacement; 		// The mean displacement
	public double meanCI; 					// The chemotactic index
	public double meanMCI; 					// The meandering index
	public double meanSCI; 					// The segmented chemotactic
	public double meanxDispRate; 			// The x displacement rate
	public double meanyDispRate; 			// The y displacement rate
	public double persistenceTime; 			// Persistence time
	public double percetForward; 			// Percent cells migrating forward
	
	PointSourceHelperFunction(JEXEntry entry, JEXData tracks, JEXData timelapse, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.tracks = tracks;
		this.timelapse = timelapse;
		this.parameters = parameters;
		this.entry = entry;
		
		// //// Get params
		getParams();
		
		// Set up the image wrap
		ImagePlus im = ImageReader.readObjectToImagePlus(timelapse);
		imagepanel = new ImagePanel(this, "Set Point Source");
		imagepanel.setImage(im);
		
		wrap = new GraphicalFunctionWrap(this, parameters);
		wrap.addStep(0, "Select gradient source", new String[] { "Automatic", "Seconds per frame", "Micron per pixel", "Temporal binning", "Maximum velocity", "X-Position", "Y-Position" });
		wrap.setInCentralPanel(imagepanel);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public boolean doit()
	{
		boolean b = wrap.start();
		if(!b)
			return false;
		
		return true;
	}
	
	/**
	 * Load the paramters
	 */
	public void getParams()
	{
		String autoStr = parameters.getValueOfParameter("Automatic");
		auto = Boolean.parseBoolean(autoStr);
		
		stepSize = Integer.parseInt(parameters.getValueOfParameter("Temporal binning"));
		maxVel = Double.parseDouble(parameters.getValueOfParameter("Maximum velocity"));
		spf = Double.parseDouble(parameters.getValueOfParameter("Seconds per frame"));
		mpp = Double.parseDouble(parameters.getValueOfParameter("Micron per pixel"));
		xpos = Double.parseDouble(parameters.getValueOfParameter("X-Position"));
		ypos = Double.parseDouble(parameters.getValueOfParameter("Y-Position"));
	}
	
	/**
	 * Display source point
	 */
	public void displaySource()
	{
		Roi roi = null;
		
		// If the position is not defined set to null
		if(xpos == -1 || ypos == -1)
			roi = null;
		
		// else draw a rectangle
		else
		{
			Rectangle rect = new Rectangle((int) (xpos - 3), (int) (ypos - 3), 6, 6);
			roi = new Roi(rect);
		}
		
		// Display the rectangle around the source point
		imagepanel.setRoi(roi);
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
	{
		getParams();
		
		// Display the source point
		displaySource();
	}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return 0;
	}
	
	public void startIT()
	{
		wrap.displayUntilStep();
	}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT()
	{
		// Call the calculation function and set it up
		trajectories = TrackReader.readObjectToTrajectories(tracks);
		Logs.log("Found " + trajectories.size(), 1, this);
		
		// Initiate variables
		velocities = new ArrayList<Double>();
		chemovelocities = new ArrayList<Double>();
		angles = new ArrayList<Double>();
		displacements = new ArrayList<Double>();
		CIs = new ArrayList<Double>();
		MCIs = new ArrayList<Double>();
		SCIs = new ArrayList<Double>();
		xDispRates = new ArrayList<Double>();
		yDispRates = new ArrayList<Double>();
		persistenceTimes = new ArrayList<Double>();
		startinPositionXs = new ArrayList<Double>();
		startinPositionYs = new ArrayList<Double>();
		meanVelocity = 0; // The mean velocity
		meanChemoVelocity = 0; // The mean chemotactic velocity
		meanAngle = 0; // The mean angle
		meanDisplacement = 0; // The mean displacement
		meanCI = 0; // The chemotactic index
		meanMCI = 0; // The meandering index
		meanSCI = 0; // The segmented chemotactic index
		meanxDispRate = 0; // The x displacement rate
		meanyDispRate = 0; // The y displacement rate
		persistenceTime = 0; // Persistence time
		percetForward = 0; // the percent cells migrating forward
		double forwardCells = 0;
		
		// Set up the calculation functions
		for (Trajectory track : trajectories)
		{
			// Get the oringin point and determine the axis of migration
			Point pOrigin = track.getFirst();
			Axis migrationAxis = new Axis(pOrigin, new Point((int) xpos, (int) ypos));
			
			// Create a new track statistic instance
			SingleTrackStatistics stats = new SingleTrackStatistics();
			stats.setAxis(migrationAxis);
			stats.setBinning(stepSize);
			stats.setMicronsPerPixel(mpp);
			stats.setSecondsPerFrame(spf);
			stats.setNumberOfBinsForSCI(12);
			stats.setMigrationType(SingleTrackStatistics.MIGRATION_POINT_SOURCE);
			stats.setTrack(track);
			
			// Add values to the list vectors
			velocities.add(stats.meanVelocity);
			chemovelocities.add(stats.meanChemoVelocity);
			angles.add(stats.meanAngle);
			displacements.add(stats.meanDisplacement);
			CIs.add(stats.CI);
			MCIs.add(stats.MCI);
			SCIs.add(stats.SCI);
			xDispRates.add(stats.xDispRate);
			yDispRates.add(stats.yDispRate);
			persistenceTimes.add(stats.persistenceTime);
			startinPositionXs.add(stats.startinPositionX);
			startinPositionYs.add(stats.startinPositionY);
			
			Point first = track.getFirst();
			Point last  = track.getLast();
			if (first.y - last.y > 0) forwardCells = forwardCells + 1;
		}
		percetForward = forwardCells / trajectories.size();
		
		// Display stuff
		Logs.log("Finished analyzing trajectories", 1, this);
		
		// Calculate the mean values
		meanVelocity = VectorUtility.mean(velocities); // The mean velocity
		meanChemoVelocity = VectorUtility.mean(chemovelocities); // The mean chemotactic velocity
		meanAngle = VectorUtility.mean(angles); // The mean angle
		meanDisplacement = VectorUtility.mean(displacements); // The mean
		// displacement
		meanCI = VectorUtility.mean(CIs); // The chemotactic index
		meanMCI = VectorUtility.mean(MCIs); // The meandering index
		meanSCI = VectorUtility.mean(SCIs); // The segmented chemotactic index
		meanxDispRate = VectorUtility.mean(xDispRates); // The x displacement
		// rate
		meanyDispRate = VectorUtility.mean(yDispRates); // The y displacement
		// rate
		persistenceTime = VectorUtility.mean(persistenceTimes); // Persistence
		// time
	}
	
	public void loopNext()
	{}
	
	public void loopPrevious()
	{}
	
	public void recalculate()
	{}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{
		// Get the x and y positions
		xpos = p.getX();
		ypos = p.getY();
		
		// Diplay the source
		displaySource();
		
		// Update the parameter fields
		wrap.setParameter("X-Position", "" + xpos);
		wrap.setParameter("Y-Position", "" + ypos);
	}
	
	public void mouseMoved(Point p)
	{}
	
}
