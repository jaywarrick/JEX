package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;

import logs.Logs;

/**
 * This is a JEXperiment function template
 * To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder
 * 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types
 * The specific API for these can be found in the main JEXperiment folder.
 * These API provide methods to retrieve data from these objects,
 * create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 *
 */
/**
 * @author bencasavant
 * 
 */
public class JEX_NuclearStainCellCounter extends JEXCrunchable {
	
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
		String result = "Cell Counter for Nuclear Stains";
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
		String result = "Find the cells within a fluorescent image and output a ROI of those cells";
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
		
		inputNames[0] = new TypeName(IMAGE, "Image");
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
		defaultOutputNames[0] = new TypeName(VALUE, "Number of Cells");
		defaultOutputNames[1] = new TypeName(ROI, "Point ROI of Cells");
		
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
		Parameter p0 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
		Parameter p1 = new Parameter("RollingBall", "Rolling ball radius for removing background", "50.0");
		Parameter p2 = new Parameter("Erode-Dilate", "Erode dilate to remove small clusters", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p3 = new Parameter("Threshold", "Threshold for identifying cell locations", "8.0");
		Parameter p4 = new Parameter("Threshold min. value.", "Lowest value for a live cell minimum", "1.0");
		
		Parameter p5 = new Parameter("Min. live radius", "Radius of live cell in pixels (e.g. 3 to 30)", "5");
		Parameter p6 = new Parameter("Max. live radius", "Radius of live cell in pixels (e.g. 3 to 30)", "5");
		
		// Parameter p10 = new
		// Parameter("Outliers","Remove outliers as percentile of found clusters","0.05");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
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
		JEXData data1 = inputs.get("Image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		
		// Run the function
		NuclearCellCounter graphFunc = new NuclearCellCounter(entry, data1, data2, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.cellNumber;
		JEXData output2 = graphFunc.cellROI;
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
}

class NuclearCellCounter implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	int frame = 0;
	
	// Roi interaction
	boolean interactionMode = false;
	Point first = null;
	Point second = null;
	Rectangle rectroi = null;
	
	// Outputs
	public JEXData cellNumber;
	public JEXData cellROI;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int rollingBall = 30;
	int liveThresh = 40;
	double liveMean = 1;
	int deadThresh = 40;
	double deadMean = 1;
	int doubleRadius = 20;
	float singleCellArea = -1;
	boolean erodedilate = true;
	
	double radius1 = 15.0;
	double radius2 = 20.0;
	double radius3 = 1000.0;
	double radius4 = 1000.0;
	boolean createBackground = false;
	boolean lightBackground = false;
	boolean useParaboloid = false;
	boolean doPresmooth = false;
	boolean correctCorners = false;
	
	// Variables used during the function steps
	private ImagePlus im;
	private ImagePlus nuclear;
	private ImagePlus preprocessed;
	
	// Input
	JEXData nuclearCell;
	JEXData imageROI;
	JEXEntry entry;
	TypeName[] outputNames;
	
	PointList nuclearCellPoints;
	float[] clusterAreas;
	
	NuclearCellCounter(JEXEntry entry, JEXData nuclearCell, JEXData imageROI, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.nuclearCell = nuclearCell;
		this.imageROI = imageROI;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Prepare images
		nuclear = ImageReader.readObjectToImagePlus(nuclearCell);
		if(imageROI != null)
		{
			ROIPlus roip = RoiReader.readObjectToRoi(imageROI);
			rectroi = (roip != null && roip.getRoi() != null) ? roip.getRoi().getBounds() : null;
		}
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Locate cells based on nuclear stain");
		
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Preprocess image", new String[] { "RollingBall", "Erode-Dilate", "Tolerance", "Min. value." });
		wrap.addStep(1, "Locate clusters", new String[] { "Min. live radius", "Max. live radius" });
		wrap.addStep(2, "Analyze", new String[] { "Double counting", "Cell Area", "Automatic" });
		
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{
		// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		rollingBall = (int) Double.parseDouble(params.getValueOfParameter("RollingBall"));
		liveThresh = (int) Double.parseDouble(params.getValueOfParameter("Live tolerance"));
		liveMean = Double.parseDouble(params.getValueOfParameter("Live min. value."));
		doubleRadius = (int) Double.parseDouble(params.getValueOfParameter("Double counting"));
		singleCellArea = Float.parseFloat(params.getValueOfParameter("Cell Area"));
		
		radius1 = (int) Double.parseDouble(params.getValueOfParameter("Min. live radius"));
		radius2 = (int) Double.parseDouble(params.getValueOfParameter("Max. live radius"));
		
		// removeOutliers =
		// Float.parseFloat(params.getValueOfParameter("Outliers"));
		erodedilate = Boolean.parseBoolean(params.getValueOfParameter("Erode-Dilate"));
	}
	
	private void displayImage(int index)
	{
		imagepanel.setImage(im);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		// //// Get params
		getParams();
		
		if(auto)
		{
			preProcess();
			locate();
		}
		else
		{
			wrap.start();
		}
		return;
	}
	
	public void runStep(int step)
	{
		atStep = step;
		
		// //// Get params
		getParams();
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		imagepanel.setPointListArray(null, null);
		imagepanel.setRoi(null);
		imagepanel.setTracks(null);
		
		if(atStep == 0)
		{
			preProcess();
			imagepanel.setImage(preprocessed);
			interactionMode = true;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 1)
		{
			locate();
			imagepanel.setPointList(nuclearCellPoints);
			imagepanel.setImage(nuclear);
			interactionMode = true;
			
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
	}
	
	public void runNext()
	{
		atStep = atStep + 1;
		if(atStep > 1)
			atStep = 1;
	}
	
	public void runPrevious()
	{
		atStep = atStep - 1;
		if(atStep < 0)
			atStep = 0;
	}
	
	public int getStep()
	{
		return atStep;
	}
	
	public void loopNext()
	{
		index = 0;
		runStep(atStep);
	}
	
	public void loopPrevious()
	{
		index = 0;
		runStep(atStep);
	}
	
	public void recalculate()
	{}
	
	public void startIT()
	{
		wrap.displayUntilStep();
	}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT()
	{   
		
	}
	
	/**
	 * Pre process and watershed the live image
	 */
	private void preProcess()
	{
		// Get the processor in the right kind
		ShortProcessor shortLive = (ShortProcessor) nuclear.getProcessor().convertToShort(true);
		Logs.log("Image processor reached", 1, this);
		
		// Background subtracter
		BackgroundSubtracter bgs = new BackgroundSubtracter();
		
		// Background subtracter parameters
		double radius1 = this.rollingBall;
		boolean createBackground = false;
		boolean lightBackground = false;
		boolean useParaboloid = false;
		boolean doPresmooth = false;
		boolean correctCorners = false;
		
		// Perform background subtraction for both Hoechst and p65 images
		bgs.rollingBallBackground(shortLive, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		Logs.log("Live image rolling ball performed", 1, this);
		
		// Convert ShortProcessor to ByteProcessor for watershedding
		ByteProcessor byteImPlus1 = (ByteProcessor) shortLive.convertToByte(true);
		ImagePlus tempImage = new ImagePlus("", byteImPlus1);
		ImageStatistics stats = tempImage.getStatistics();
		double mean = stats.mean;
		
		// Find maxima
		Logs.log("Finding particles", 1, this);
		MaximumFinder finder = new MaximumFinder();
		ByteProcessor out = finder.findMaxima(byteImPlus1, liveThresh, liveMean * mean, MaximumFinder.SEGMENTED, true, false);
		
		if(erodedilate)
		{
			out = (ByteProcessor) out.duplicate().convertToByte(true);
			int[] kern2 = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1 };
			out.convolve3x3(kern2);
			out.convolve3x3(kern2);
			int[] kern = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 };
			out.convolve3x3(kern);
			out.convolve3x3(kern);
			out.invertLut();
			out.threshold(100);
		}
		
		// Convert ByteProcessor back to ImagePlus after watershedding is done
		preprocessed = new ImagePlus("Watershed", out.duplicate());
	}
	
	/**
	 * Locate cells in the live image
	 */
	private void locate()
	{
		// Use ImageJ Particle Analyzer on data1
		int options = 0;
		int measure = Measurements.AREA | Measurements.CIRCULARITY | Measurements.INTEGRATED_DENSITY | Measurements.CENTROID | Measurements.ELLIPSE;
		
		// Make the particle analyzer
		ResultsTable rt = new ResultsTable();
		double minSize = radius1;
		// double maxSize = Double.POSITIVE_INFINITY;
		double maxSize = radius2;
		double minCirc = 0.0;
		double maxCirc = 1.0;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
		boolean done = analyzer.analyze(preprocessed);
		Logs.log("Image particle analyzer performed returned " + done, 1, this);
		
		// Acquire the ROIs from the particle analysis and apply to the p65
		// image
		List<Roi> foundRois = analyzer.foundRois;
		Logs.log("Total number of rois is " + foundRois.size(), 1, this);
		ROIPlus cells = new ROIPlus(foundRois.get(0));
		
		nuclearCellPoints = cells.getPointList();
		Polygon testingPolygon = RoiReader.readObjectToRoi(imageROI).getPointList().toPolygon();
		PointList result = new PointList();
		for (Point p : nuclearCellPoints)
		{
			if(testingPolygon.contains(p))
			{
				result.add(p);
			}
		}
		ROIPlus cellROIPlus = new ROIPlus(result, ROIPlus.ROI_POINT);
		cellROI = RoiWriter.makeRoiObject(outputNames[1].getName(), cellROIPlus);
		cellNumber = ValueWriter.makeValueObject(outputNames[0].getName(), "" + cellROIPlus.getPointList().size());
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
