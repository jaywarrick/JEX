package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
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
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
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
 * @author edmyoung
 * 
 */
public class JEX_CountFluorescentCells extends JEXCrunchable {
	
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
		String result = "Count cells";
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
		String result = "Fluorescent cell counter";
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
	 * Return the number of inputs required by this function In case of a function that takes optional inputs set this number to the maximum number of inputs possible
	 * 
	 * @return number of inputs
	 */
	public int getNumberOfInputs()
	{
		// Inputs are Hoechst nuclear stain (the mask) and p65 stain (the
		// primary)
		return 2;
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
		defaultOutputNames[0] = new TypeName(VALUE, "Number Cells");
		defaultOutputNames[1] = new TypeName(VALUE, "More Info");
		
		if(outputNames == null)
			return defaultOutputNames;
		return outputNames;
	}
	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[getNumberOfInputs()];
		
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(ROI, "Optional ROI");
		
		return inputNames;
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
		
		Parameter p1 = new Parameter("Binning", "Binning factor for quicker analysis", "1.0");
		Parameter p3 = new Parameter("RollingBall", "Rolling ball radius for removing background", "50.0");
		Parameter p6 = new Parameter("Tolerance", "Threshold for identifying live cell locations", "8.0");
		
		Parameter p2 = new Parameter("Threshold", "Threshold for identifying neutrophil locations", "40.0");
		Parameter p4 = new Parameter("Radius", "Radius of live cell in pixels (e.g. 3 to 30)", "0");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		// Collect inputs
		// data1 contains all the Hoechst images per entry
		// data2 contains all the p65 images per entry
		JEXData data1 = inputs.get("Image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		Rectangle roi = null;
		if(data2 != null)
		{
			ROIPlus roip = RoiReader.readObjectToRoi(data2);
			roi = (roip == null) ? null : roip.pointList.getBounds();
		}
		// Run the function
		LiveCellFinder graphFunc = new LiveCellFinder(entry, data1, roi, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.day1_liveNumber;
		JEXData output3 = graphFunc.moreInfo;
		realOutputs.add(output1);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
}

class LiveCellFinder implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	
	// Outputs
	public JEXData day1_liveNumber;
	public JEXData moreInfo;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int bin = 2;
	int rollingBall = 30;
	int liveThresh = 40;
	int doubleRadius = 20;
	
	double radius1 = 15.0;
	double radius2 = 20.0;
	boolean createBackground = false;
	boolean lightBackground = false;
	boolean useParaboloid = false;
	boolean doPresmooth = false;
	boolean correctCorners = false;
	
	// Variables used during the function steps
	private ByteProcessor imp;
	private ImagePlus im;
	private ImagePlus live;
	private ImagePlus livePreprocessed;
	
	// Input
	JEXData day1_liveImset;
	JEXEntry entry;
	TypeName[] outputNames;
	Rectangle rectangle;
	
	PointList day1_livePoints;
	
	LiveCellFinder(JEXEntry entry, JEXData day1_liveImset, Rectangle rectangle, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.day1_liveImset = day1_liveImset;
		this.rectangle = rectangle;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Prepare images
		live = ImageReader.readObjectToImagePlus(day1_liveImset);
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Locate live dead cells and determine proliferation");
		
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "LIVE - Preprocess image", new String[] { "Binning", "RollingBall", "Tolerance" });
		wrap.addStep(1, "LIVE - locate cells", new String[] { "Radius" });
		wrap.addStep(2, "Analyze", new String[0]);
		
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
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		rollingBall = (int) Double.parseDouble(params.getValueOfParameter("RollingBall"));
		liveThresh = (int) Double.parseDouble(params.getValueOfParameter("Tolerance"));
		
		radius1 = (int) Double.parseDouble(params.getValueOfParameter("Radius"));
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
			binImage();
			
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
			preProcessLive();
			imagepanel.setImage(livePreprocessed);
			interactionMode = true;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 1)
		{
			locateLive();
			
			// ImageProcessor imp = live.getProcessor();
			// ImageProcessor imp2 = imp.duplicate();
			// imp2 = imp2.convertToByte(true);
			// ImagePlus im2 = new ImagePlus("",imp2);
			
			imagepanel.setPointList(day1_livePoints);
			imagepanel.setImage(live);
			// imagepanel.setImage(im2);
			interactionMode = true;
			
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 2)
		{
			analyze();
			
			interactionMode = false;
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
		if(atStep > 2)
			atStep = 2;
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
	 * Bin the image for faster processing
	 */
	private void binImage()
	{
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
	}
	
	/**
	 * Bin the image
	 */
	public ImagePlus binImage(ImagePlus im)
	{
		ImageProcessor imp = im.getProcessor().convertToByte(true);
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = imp.resize(newWidth);
		ImagePlus result = new ImagePlus("", imp);
		return result;
	}
	
	/**
	 * Pre process and watershed the live image
	 */
	private void preProcessLive()
	{
		// Get the processor in the right kind
		ShortProcessor shortLive = (ShortProcessor) live.getProcessor().convertToShort(true);
		Logs.log("Live cell processor reached", 1, this);
		
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
		Logs.log("Live image Finding particles", 1, this);
		MaximumFinder finder = new MaximumFinder();
		ByteProcessor out = finder.findMaxima(byteImPlus1, liveThresh, 4 * mean, MaximumFinder.SEGMENTED, true, false);
		out.invertLut();
		
		// Convert ByteProcessor back to ImagePlus after watershedding is done
		livePreprocessed = new ImagePlus("Watershed", out.duplicate());
		imp = (ByteProcessor) livePreprocessed.getProcessor().duplicate().convertToByte(true);
	}
	
	/**
	 * Locate cells in the live image
	 */
	private void locateLive()
	{
		// Use ImageJ Particle Analyzer on data1
		// int options = ParticleAnalyzer.SHOW_MASKS |
		// ParticleAnalyzer.ADD_TO_MANAGER;
		int options = 0;
		int measure = Measurements.AREA | Measurements.CIRCULARITY | Measurements.INTEGRATED_DENSITY | Measurements.CENTROID | Measurements.ELLIPSE;
		
		// Make the particle analyzer
		ResultsTable rt = new ResultsTable();
		double minSize = radius1;
		double maxSize = Double.POSITIVE_INFINITY;
		double minCirc = 0.0;
		double maxCirc = 1.0;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
		analyzer.analyze(livePreprocessed);
		Logs.log("Live image particle analyzer performed", 1, this);
		
		// Acquire the ROIs from the particle analysis and apply to the p65
		// image
		List<Roi> foundRois = analyzer.foundRois;
		Logs.log("Total number of rois is " + foundRois.size(), 1, this);
		
		// Get the results out
		float[] xPos = new float[0];
		float[] yPos = new float[0];
		// float[] intDen = new float[0] ;
		// float[] areas = new float[0] ;
		if(rt.getLastColumn() == 0 || rt.getColumn(0) == null)
		{   
			
		}
		else
		{
			// int nb = rt.getColumn(0).length;
			int lastColumn = rt.getLastColumn();
			
			for (int i = 0; i < lastColumn; i++)
			{
				String cName = rt.getColumnHeading(i);
				if(cName.equals("X"))
				{
					xPos = rt.getColumn(i);
				}
				if(cName.equals("Y"))
				{
					yPos = rt.getColumn(i);
				}
				// if (cName.equals("Area")){
				// areas = rt.getColumn(i);
				// }
				// if (cName.equals("Mean")){
				// intDen = rt.getColumn(i);
				// }
			}
		}
		
		day1_livePoints = new PointList();
		for (int i = 0; i < xPos.length; i++)
		{
			Point p = new Point((int) xPos[i], (int) yPos[i]);
			day1_livePoints.add(p);
		}
	}
	
	/**
	 * Analyze proliferation and fill the variables to output
	 */
	private void analyze()
	{
		int liveNumber = 0;
		
		liveNumber = day1_livePoints.size();
		
		day1_liveNumber = ValueWriter.makeValueObject(outputNames[0].getName(), "" + liveNumber);
		day1_liveNumber.setDataObjectInfo("Count performed using Fluorescent Cell Counter Function");
		
		// Table sheet
		String[] columns = new String[] { "Start X", "Start Y" };
		String[] xpos = new String[day1_livePoints.size()];
		String[] ypos = new String[day1_livePoints.size()];
		HashMap<String,String[]> columnData = new HashMap<String,String[]>();
		for (int i = 0; i < day1_livePoints.size(); i++)
		{
			Point p = day1_livePoints.get(i);
			xpos[i] = "" + p.x;
			ypos[i] = "" + p.y;
		}
		columnData.put("Start X", xpos);
		columnData.put("Start Y", ypos);
		moreInfo = ValueWriter.makeValueTable(outputNames[1].getName(), columns, columnData);
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
