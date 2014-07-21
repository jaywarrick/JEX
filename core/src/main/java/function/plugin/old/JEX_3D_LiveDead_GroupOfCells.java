package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataReader.ValueReader;
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
import function.imageUtility.Thresholder;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.ArrayUtility;

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
public class JEX_3D_LiveDead_GroupOfCells extends JEXCrunchable {
	
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
		String result = "3D live-dead cell location for clusters";
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
		String result = "Find locations of clusters of live and dead cells in a live/dead stain and find number";
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
		TypeName[] inputNames = new TypeName[4];
		
		inputNames[0] = new TypeName(IMAGE, "Live Image");
		inputNames[1] = new TypeName(IMAGE, "Dead Image");
		inputNames[2] = new TypeName(ROI, "Optional ROI");
		inputNames[3] = new TypeName(VALUE, "Optional Normalization value");
		
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
		this.defaultOutputNames = new TypeName[7];
		this.defaultOutputNames[0] = new TypeName(VALUE, "Number Live");
		this.defaultOutputNames[1] = new TypeName(VALUE, "Number Dead");
		this.defaultOutputNames[2] = new TypeName(VALUE, "Total cell number");
		this.defaultOutputNames[3] = new TypeName(VALUE, "Mean cluster size");
		this.defaultOutputNames[4] = new TypeName(VALUE, "More Info");
		this.defaultOutputNames[5] = new TypeName(IMAGE, "Live Cells Image");
		this.defaultOutputNames[6] = new TypeName(IMAGE, "Dead Cells Image");
		
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
		Parameter p0 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
		Parameter p1 = new Parameter("RollingBall", "Rolling ball radius for removing background", "50.0");
		Parameter p2 = new Parameter("Use thresh. method", "Use a thresholding method to find the cells... better for out-of-focus clusters", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p3 = new Parameter("Erode-Dilate", "Erode dilate to remove small clusters", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p4 = new Parameter("Live tolerance", "Threshold for identifying live cell locations", "8.0");
		Parameter p5 = new Parameter("Live min. value.", "Lowest value for a live cell minimum", "1.0");
		Parameter p6 = new Parameter("Min. live radius", "Radius of live cell in pixels (e.g. 3 to 30)", "5");
		Parameter p7 = new Parameter("Max. live radius", "Radius of live cell in pixels (e.g. 3 to 30)", "1000");
		
		Parameter p8 = new Parameter("Dead tolerance", "Threshold for identifying dead cell locations", "15.0");
		Parameter p9 = new Parameter("Dead min. value.", "Lowest value for a dead cell minimum", "1.0");
		Parameter p10 = new Parameter("Min. dead radius", "Radius of dead cell in pixels (e.g. 3 to 30)", "5");
		Parameter p11 = new Parameter("Max. dead radius", "Radius of dead cell in pixels (e.g. 3 to 30)", "200");
		
		Parameter p12 = new Parameter("Double counting", "Distance of particles to prevent double counting", "10");
		Parameter p13 = new Parameter("Cell Area", "Number of pixels covered by one cell. Set to -1 for disabling area normalization", "-1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		parameterArray.addParameter(p13);
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
		JEXData data1 = inputs.get("Live Image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		JEXData data2 = inputs.get("Dead Image");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		JEXData data3 = inputs.get("Optional Normalization Image");
		JEXData data4 = inputs.get("Optional ROI");
		
		// Run the function
		LiveDeadCellFinder2 graphFunc = new LiveDeadCellFinder2(entry, data1, data2, data3, data4, this.outputNames, this.parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.day1_liveNumber;
		JEXData output2 = graphFunc.day1_deadNumber;
		JEXData output3 = graphFunc.totalNumber;
		JEXData output4 = graphFunc.day1_clusterSize;
		JEXData output5 = graphFunc.moreInfo;
		JEXData output6 = graphFunc.liveImage;
		JEXData output7 = graphFunc.deadImage;
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		this.realOutputs.add(output4);
		this.realOutputs.add(output5);
		this.realOutputs.add(output6);
		this.realOutputs.add(output7);
		
		// Return status
		return true;
	}
}

class LiveDeadCellFinder2 implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	public JEXData day1_liveNumber;
	public JEXData day1_deadNumber;
	public JEXData totalNumber;
	public JEXData day1_clusterSize;
	public JEXData moreInfo;
	public JEXData liveImage;
	public JEXData deadImage;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	boolean useThreshold = false;
	int rollingBall = 30;
	int liveThresh = 40;
	double liveMean = 1;
	int deadThresh = 40;
	int doubleRadius = 20;
	double deadMean = 1;
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
	private ImagePlus live;
	private ImagePlus dead;
	private ImagePlus livePreprocessed;
	private ImagePlus deadPreprocessed;
	private Double norm;
	
	// Input
	JEXData day1_liveImset;
	JEXData day1_deadImset;
	JEXData day1_normalization;
	JEXData rectangle;
	JEXEntry entry;
	TypeName[] outputNames;
	
	PointList day1_livePoints;
	PointList day1_deadPoints;
	float[] clusterAreas;
	
	LiveDeadCellFinder2(JEXEntry entry, JEXData day1_liveImset, JEXData day1_deadImset, JEXData day1_normalization, JEXData rectangle, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.day1_liveImset = day1_liveImset;
		this.day1_deadImset = day1_deadImset;
		this.day1_normalization = day1_normalization;
		this.rectangle = rectangle;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		this.getParams();
		
		// Prepare images
		this.live = ImageReader.readObjectToImagePlus(day1_liveImset);
		this.dead = ImageReader.readObjectToImagePlus(day1_deadImset);
		if(day1_normalization != null)
		{
			this.norm = ValueReader.readObjectToDouble(day1_normalization);
		}
		if(rectangle != null)
		{
			ROIPlus roip = RoiReader.readObjectToRoi(rectangle);
			this.rectroi = (roip != null && roip.getRoi() != null) ? roip.getRoi().getBounds() : null;
		}
		
		// Prepare the graphics
		this.imagepanel = new ImagePanel(this, "Locate live dead cells and determine proliferation");
		this.imagepanel.setImage(this.live);
		
		// displayImage(index);
		this.wrap = new GraphicalFunctionWrap(this, this.params);
		this.wrap.addStep(0, "LIVE - Preprocess image", new String[] { "RollingBall", "Use thresh. method", "Erode-Dilate", "Live tolerance", "Live min. value." });
		this.wrap.addStep(1, "LIVE - locate clusters", new String[] { "Min. live radius", "Max. live radius" });
		this.wrap.addStep(2, "DEAD - Preprocess image", new String[] { "Dead tolerance", "Dead min. value." });
		this.wrap.addStep(3, "DEAD - locate cells", new String[] { "Min. dead radius", "Max. dead radius" });
		this.wrap.addStep(4, "Analyze", new String[] { "Double counting", "Cell Area", "Automatic" });
		
		String title = "Analyzing entry " + entry.getEntryExperiment() + " - " + entry.getTrayX() + "." + entry.getTrayY();
		this.wrap.setTitle(title);
		this.wrap.setInCentralPanel(this.imagepanel);
		this.wrap.setDisplayLoopPanel(true);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{
		// //// Get params
		this.auto = Boolean.parseBoolean(this.params.getValueOfParameter("Automatic"));
		this.useThreshold = Boolean.parseBoolean(this.params.getValueOfParameter("Use thresh. method"));
		this.rollingBall = (int) Double.parseDouble(this.params.getValueOfParameter("RollingBall"));
		this.liveThresh = (int) Double.parseDouble(this.params.getValueOfParameter("Live tolerance"));
		this.liveMean = Double.parseDouble(this.params.getValueOfParameter("Live min. value."));
		this.deadThresh = (int) Double.parseDouble(this.params.getValueOfParameter("Dead tolerance"));
		this.deadMean = Double.parseDouble(this.params.getValueOfParameter("Dead min. value."));
		this.doubleRadius = (int) Double.parseDouble(this.params.getValueOfParameter("Double counting"));
		this.singleCellArea = Float.parseFloat(this.params.getValueOfParameter("Cell Area"));
		
		this.radius1 = (int) Double.parseDouble(this.params.getValueOfParameter("Min. live radius"));
		this.radius2 = (int) Double.parseDouble(this.params.getValueOfParameter("Min. dead radius"));
		this.radius3 = (int) Double.parseDouble(this.params.getValueOfParameter("Max. live radius"));
		this.radius4 = (int) Double.parseDouble(this.params.getValueOfParameter("Max. dead radius"));
		
		// removeOutliers =
		// Float.parseFloat(params.getValueOfParameter("Outliers"));
		this.erodedilate = Boolean.parseBoolean(this.params.getValueOfParameter("Erode-Dilate"));
	}
	
	@SuppressWarnings("unused")
	private void displayImage(int index)
	{
		this.imagepanel.setImage(this.im);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		// //// Get params
		this.getParams();
		
		if(this.auto)
		{
			this.preProcessLive();
			this.locateLive();
			this.preProcessDead();
			this.locateDead();
			this.analyze();
		}
		else
		{
			this.wrap.start();
		}
		return;
	}
	
	@Override
	public void runStep(int step)
	{
		this.atStep = step;
		
		// //// Get params
		this.getParams();
		
		// /// Run step index
		Logs.log("Running step " + this.atStep, 1, this);
		this.imagepanel.setPointListArray(null, null);
		this.imagepanel.setRoi(null);
		this.imagepanel.setTracks(null);
		
		if(this.atStep == 0)
		{
			this.preProcessLive();
			this.imagepanel.setImage(this.livePreprocessed);
			this.interactionMode = true;
			if(this.auto)
			{
				this.atStep = this.atStep + 1;
				this.runStep(this.atStep);
			}
		}
		else if(this.atStep == 1)
		{
			this.locateLive();
			this.imagepanel.setPointList(this.day1_livePoints);
			this.imagepanel.setImage(this.live);
			this.interactionMode = true;
			
			if(this.auto)
			{
				this.atStep = this.atStep + 1;
				this.runStep(this.atStep);
			}
		}
		else if(this.atStep == 2)
		{
			this.preProcessDead();
			this.imagepanel.setImage(this.deadPreprocessed);
			this.interactionMode = true;
			
			if(this.auto)
			{
				this.atStep = this.atStep + 1;
				this.runStep(this.atStep);
			}
		}
		else if(this.atStep == 3)
		{
			this.locateDead();
			this.imagepanel.setPointList(this.day1_deadPoints);
			this.imagepanel.setImage(this.dead);
			
			this.interactionMode = false;
			if(this.auto)
			{
				this.atStep = this.atStep + 1;
				this.runStep(this.atStep);
			}
		}
		else if(this.atStep == 4)
		{
			this.analyze();
			
			this.interactionMode = false;
			if(this.auto)
			{
				this.atStep = this.atStep + 1;
				this.runStep(this.atStep);
			}
		}
		
		String title = "Analyzing entry " + this.entry.getEntryExperiment() + " - " + this.entry.getTrayX() + "." + this.entry.getTrayY();
		this.wrap.setTitle(title);
	}
	
	@Override
	public void runNext()
	{
		this.atStep = this.atStep + 1;
		if(this.atStep > 4)
		{
			this.atStep = 4;
		}
	}
	
	@Override
	public void runPrevious()
	{
		this.atStep = this.atStep - 1;
		if(this.atStep < 0)
		{
			this.atStep = 0;
		}
	}
	
	@Override
	public int getStep()
	{
		return this.atStep;
	}
	
	@Override
	public void loopNext()
	{
		this.index = 0;
		this.runStep(this.atStep);
	}
	
	@Override
	public void loopPrevious()
	{
		this.index = 0;
		this.runStep(this.atStep);
	}
	
	@Override
	public void recalculate()
	{}
	
	@Override
	public void startIT()
	{
		this.wrap.displayUntilStep();
	}
	
	/**
	 * Apply the roi to all other images
	 */
	@Override
	public void finishIT()
	{   
		
	}
	
	/**
	 * Pre process and watershed the live image
	 */
	private void preProcessLive()
	{
		// if the threshold method is used call the threshold method else use
		// the find max method
		if(this.useThreshold)
		{
			this.processUsingThreshold();
		}
		else
		{
			this.processUsingFindMax();
		}
	}
	
	private void processUsingFindMax()
	{
		// Get the processor in the right kind
		ShortProcessor shortLive = (ShortProcessor) this.live.getProcessor().convertToShort(true);
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
		ByteProcessor out = finder.findMaxima(byteImPlus1, this.liveThresh, this.liveMean * mean, MaximumFinder.SEGMENTED, true, false);
		
		if(this.erodedilate)
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
		this.livePreprocessed = new ImagePlus("Watershed", out.duplicate());
	}
	
	private void processUsingThreshold()
	{
		// Normalize the image
		ImagePlus im2run = new ImagePlus("", this.live.getProcessor().duplicate());
		FloatProcessor imp = (FloatProcessor) im2run.getProcessor().convertToFloat();
		FunctionUtility.imAdjust(imp, imp.getMin(), imp.getMax(), 0, 4095, 1);
		
		// Get the processor in the right kind
		ShortProcessor shortLive = (ShortProcessor) imp.convertToShort(true);
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
		
		// Convert ShortProcessor to ByteProcessor
		ByteProcessor byteImPlus1 = (ByteProcessor) shortLive.convertToByte(true);
		
		// Do the threshold
		Thresholder thresholder = new Thresholder(new ImagePlus("", byteImPlus1));
		thresholder.run(null);
		ImagePlus result = thresholder.im2run;
		ImageProcessor out = result.getProcessor();
		
		// Erode dilate to remove the small objects
		if(this.erodedilate)
		{
			out.erode();
			out.erode();
			out.dilate();
			out.dilate();
		}
		
		this.livePreprocessed = new ImagePlus("Thresholded", out.duplicate());
	}
	
	/**
	 * Locate cells in the live image
	 */
	private void locateLive()
	{
		// Use ImageJ Particle Analyzer on data1
		int options = 0;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.INTEGRATED_DENSITY | ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE;
		
		// Make the particle analyzer
		ResultsTable rt = new ResultsTable();
		double minSize = this.radius1;
		double maxSize = this.radius3;
		double minCirc = 0.0;
		double maxCirc = 1.0;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
		boolean done = analyzer.analyze(this.livePreprocessed);
		Logs.log("Live image particle analyzer performed returned " + done, 1, this);
		
		// Acquire the ROIs from the particle analysis and apply to the p65
		// image
		List<Roi> foundRois = analyzer.foundRois;
		Logs.log("Total number of rois is " + foundRois.size(), 1, this);
		
		// Get the results out
		int lastColumn = rt.getLastColumn();
		float[] xPos = new float[0];
		float[] yPos = new float[0];
		float[] areas = new float[0];
		
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
			if(cName.equals("Area"))
			{
				areas = rt.getColumn(i);
			}
		}
		
		this.day1_livePoints = new PointList();
		this.clusterAreas = new float[xPos.length];
		for (int i = 0; i < xPos.length; i++)
		{
			int px = (int) xPos[i];
			int py = (int) yPos[i];
			Point p = new Point(px, py);
			
			if(this.rectroi != null)
			{
				boolean isInX = (px > this.rectroi.getX() && px < this.rectroi.getX() + this.rectroi.getWidth());
				boolean isInY = (py > this.rectroi.getY() && py < this.rectroi.getY() + this.rectroi.getHeight());
				if(!isInX || !isInY)
				{
					continue;
				}
			}
			
			this.day1_livePoints.add(p);
			this.clusterAreas[i] = areas[i];
		}
		
		// Make the live image output
		BufferedImage bim = this.plotImage(this.live, this.day1_livePoints);
		ImagePlus lim = new ImagePlus("", bim);
		String limPath = JEXWriter.saveImage(lim);
		this.liveImage = ImageWriter.makeImageObject(this.outputNames[5].getName(), limPath);
	}
	
	/**
	 * Preprocess the dead image
	 */
	private void preProcessDead()
	{
		// Get the processor in the right kind
		ShortProcessor shortDead = (ShortProcessor) this.dead.getProcessor().convertToShort(true);
		Logs.log("Dead cell processor reached", 1, this);
		
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
		bgs.rollingBallBackground(shortDead, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		Logs.log("Dead image rolling ball performed", 1, this);
		
		// Convert ShortProcessor to ByteProcessor for watershedding
		ByteProcessor byteImPlus1 = (ByteProcessor) shortDead.convertToByte(true);
		ImagePlus tempImage = new ImagePlus("", byteImPlus1);
		ImageStatistics stats = tempImage.getStatistics();
		double mean = stats.mean;
		
		// Find maxima
		Logs.log("Dead image Finding particles", 1, this);
		MaximumFinder finder = new MaximumFinder();
		ByteProcessor out = finder.findMaxima(byteImPlus1, this.deadThresh, this.deadMean * mean, MaximumFinder.SEGMENTED, true, false);
		out.invertLut();
		
		// Convert ByteProcessor back to ImagePlus after watershedding is done
		this.deadPreprocessed = new ImagePlus("Watershed", out.duplicate());
	}
	
	private void locateDead()
	{// Use ImageJ Particle Analyzer on data1
		int options = 0;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.INTEGRATED_DENSITY | ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE;
		
		// Make the particle analyzer
		ResultsTable rt = new ResultsTable();
		double minSize = this.radius2;
		double maxSize = this.radius4;
		double minCirc = 0.0;
		double maxCirc = 1.0;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
		analyzer.analyze(this.deadPreprocessed);
		Logs.log("Live image particle analyzer performed", 1, this);
		
		// Acquire the ROIs from the particle analysis and apply to the p65
		// image
		List<Roi> foundRois = analyzer.foundRois;
		Logs.log("Total number of rois is " + foundRois.size(), 1, this);
		
		// Get the results out
		int lastColumn = rt.getLastColumn();
		float[] xPos = new float[0];
		float[] yPos = new float[0];
		
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
		}
		
		this.day1_deadPoints = new PointList();
		for (int i = 0; i < xPos.length; i++)
		{
			int px = (int) xPos[i];
			int py = (int) yPos[i];
			Point p = new Point(px, py);
			
			if(this.rectroi != null)
			{
				boolean isInX = (px > this.rectroi.getX() && px < this.rectroi.getX() + this.rectroi.getWidth());
				boolean isInY = (py > this.rectroi.getY() && py < this.rectroi.getY() + this.rectroi.getHeight());
				if(!isInX || !isInY)
				{
					continue;
				}
			}
			
			this.day1_deadPoints.add(p);
		}
		
		// Make the live image output
		BufferedImage bim = this.plotImage(this.dead, this.day1_deadPoints);
		ImagePlus dim = new ImagePlus("", bim);
		String dimPath = JEXWriter.saveImage(dim);
		this.liveImage = ImageWriter.makeImageObject(this.outputNames[6].getName(), dimPath);
	}
	
	private double distanceBetweenTwoPoints(Point p1, Point p2)
	{
		double result = Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
		return result;
	}
	
	private boolean isPointCloseToPointInList(int radius, Point p, PointList list)
	{
		for (Point pp : list)
		{
			if(this.distanceBetweenTwoPoints(p, pp) < radius)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Analyze proliferation and fill the variables to output
	 */
	private void analyze()
	{
		int liveNumber = 0;
		int deadNumber = 0;
		int totalCellNumber = 0;
		float sizeNumber = 0;
		
		if(this.norm != null || this.singleCellArea > 0)
		{
			float totalArea = 0;
			for (float f : this.clusterAreas)
			{
				totalArea = totalArea + f;
			}
			
			if(this.norm != null)
			{
				liveNumber = (int) (totalArea / this.norm);
			}
			else
			{
				liveNumber = (int) (totalArea / this.singleCellArea);
			}
			deadNumber = this.day1_deadPoints.size();
			totalCellNumber = liveNumber + deadNumber;
		}
		else
		{
			PointList validatedLive = new PointList();
			for (Point p : this.day1_livePoints)
			{
				if(this.isPointCloseToPointInList(this.doubleRadius, p, this.day1_deadPoints))
				{
					continue;
				}
				validatedLive.add(p);
			}
			liveNumber = validatedLive.size();
			deadNumber = this.day1_deadPoints.size();
			totalCellNumber = liveNumber + deadNumber;
		}
		
		sizeNumber = ArrayUtility.mean(this.clusterAreas);
		sizeNumber = ArrayUtility.mean(this.clusterAreas, (float) 0.05);
		
		this.day1_liveNumber = ValueWriter.makeValueObject(this.outputNames[0].getName(), "" + liveNumber);
		this.day1_liveNumber.setDataObjectInfo("Live cell number found with function: 3D live dead cell location for clusters");
		
		this.day1_deadNumber = ValueWriter.makeValueObject(this.outputNames[1].getName(), "" + deadNumber);
		this.day1_deadNumber.setDataObjectInfo("Dead cell number found with function: 3D live dead cell location for clusters");
		
		this.totalNumber = ValueWriter.makeValueObject(this.outputNames[2].getName(), "" + totalCellNumber);
		this.totalNumber.setDataObjectInfo("Total cell number found with function: 3D live dead cell location for clusters");
		
		this.day1_clusterSize = ValueWriter.makeValueObject(this.outputNames[3].getName(), "" + sizeNumber);
		this.day1_clusterSize.setDataObjectInfo("Mean cluster size found with function: 3D live dead cell location for clusters");
		
		String[] columnNames = new String[] { "Start X", "Start Y", "Area" };
		HashMap<String,String[]> columns = new HashMap<String,String[]>();
		String[] stX = new String[this.day1_livePoints.size()];
		String[] stY = new String[this.day1_livePoints.size()];
		String[] are = new String[this.clusterAreas.length];
		for (int i = 0; i < this.day1_livePoints.size(); i++)
		{
			Point p = this.day1_livePoints.get(i);
			stX[i] = "" + p.x;
			stY[i] = "" + p.y;
		}
		for (int i = 0; i < this.clusterAreas.length; i++)
		{
			are[i] = "" + this.clusterAreas[i];
		}
		columns.put("Start X", stX);
		columns.put("Start Y", stY);
		columns.put("Area", are);
		this.moreInfo = ValueWriter.makeValueTable(this.outputNames[4].getName(), columnNames, columns);
		this.moreInfo.setDataObjectInfo("Additional info from function: 3D live dead cell location for clusters");
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage plotImage(ImagePlus implus, PointList points)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth()));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = points.size(); k < len; k++)
		{
			// Get point
			Point p = points.get(k);
			
			// Draw point
			g.setColor(Color.red);
			g.drawRect(p.x - (int) this.radius1, p.y - (int) this.radius1, (int) this.radius1 * 2 + 1, (int) this.radius1 * 2 + 1);
			g.drawRect(p.x - (int) this.radius1 + 1, p.y - (int) this.radius1 - 3, (int) this.radius1 * 2 - 1, (int) this.radius1 * 2 - 1);
		}
		
		g.dispose();
		
		return bimage;
	}
	
	@Override
	public void clickedPoint(Point p)
	{}
	
	@Override
	public void pressedPoint(Point p)
	{}
	
	@Override
	public void mouseMoved(Point p)
	{}
	
}
