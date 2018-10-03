package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
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
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import jex.arrayView.ImageDisplayController;
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
public class JEX_PhagocytosisAnalysis2 extends JEXCrunchable {
	
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
		String result = "Phagocytosis analysis light";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Determine phagoctytosis characteristics such as particles per phagocyte, area of phagocyte, etc..";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
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
		TypeName[] inputNames = new TypeName[4];
		
		inputNames[0] = new TypeName(IMAGE, "Particle image");
		inputNames[1] = new TypeName(IMAGE, "Cell image");
		inputNames[2] = new TypeName(IMAGE, "BF image");
		inputNames[3] = new TypeName(ROI, "Optional ROI");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[7];
		defaultOutputNames[0] = new TypeName(IMAGE, "Analysed image");
		defaultOutputNames[1] = new TypeName(VALUE, "Particles per cell");
		defaultOutputNames[2] = new TypeName(VALUE, "Total cell number");
		defaultOutputNames[3] = new TypeName(VALUE, "Percent non-empty cells");
		defaultOutputNames[4] = new TypeName(VALUE, "Mean particle area");
		defaultOutputNames[5] = new TypeName(VALUE, "Mean spread index");
		defaultOutputNames[6] = new TypeName(VALUE, "More info");
		
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
		Parameter p0 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
		Parameter p1 = new Parameter("RollingBall 1", "Rolling ball radius for removing background in cell image", "100.0");
		Parameter p2 = new Parameter("Cell threshold", "Choose the auto-thresholding method or type a threshold value", "Triangle");
		Parameter p3 = new Parameter("Minimum c-thresh", "Type a threshold value for the minimum cell threshold... Useful when using auto-threshold methods that mess up controls", "-1");
		Parameter p4 = new Parameter("Watershed", "Do a watershedding to split cells close together", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p5 = new Parameter("Dilate", "How many dilatation ", "0");
		Parameter p6 = new Parameter("Erode", "Rolling ball radius for removing background in cell image", "1");
		Parameter p7 = new Parameter("Contrast enhance", "Enhance the constrast if level too low", "1");
		
		Parameter p8 = new Parameter("Min. cell radius", "Minimum radius of cell in pixels (e.g. 3 to 30)", "3000");
		Parameter p9 = new Parameter("Max. cell radius", "Maximum radius of cell in pixels (e.g. 3 to 30)", "100000");
		
		Parameter p10 = new Parameter("RollingBall 2", "Rolling ball radius for removing background in particle image", "20.0");
		Parameter p11 = new Parameter("Particle threshold", "Choose the auto-thresholding method or type a threshold value", "Minimum");
		Parameter p12 = new Parameter("Minimum p-thresh", "Type a threshold value for the minimum particle threshold... Useful when using auto-threshold methods that mess up controls", "-1");
		
		Parameter p13 = new Parameter("Min. particle radius", "Minimum radius of particle in pixels (e.g. 3 to 30)", "50");
		Parameter p14 = new Parameter("Max. particle radius", "Maximum radius of particle in pixels (e.g. 3 to 30)", "1000");
		
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
		parameterArray.addParameter(p14);
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
		JEXData data1 = inputs.get("Particle image");
		if(!data1.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Cell image");
		if(!data2.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		
		JEXData data3 = inputs.get("BF image");
		if(!data3.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		
		JEXData data4 = inputs.get("Optional ROI");
		
		// Run the function
		PhagocytosisAnalyzer2 graphFunc = new PhagocytosisAnalyzer2(entry, data1, data2, data3, data4, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.outputImage;
		JEXData output2 = graphFunc.particlesPerCell;
		JEXData output3 = graphFunc.cellNumber;
		JEXData output4 = graphFunc.percentCellsWithParticle;
		JEXData output5 = graphFunc.moreInfo;
		
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

class PhagocytosisAnalyzer2 implements GraphicalCrunchingEnabling, ImagePanelInteractor, ImageDisplayController {
	
	// Utilities
	ImagePanel imageDisplay;
	// ImageDisplay imageDisplay;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	int frame = 0;
	
	// Roi interaction
	boolean interactionMode = false;
	
	// Outputs
	public JEXData outputImage;
	public JEXData particlesPerCell;
	public JEXData cellNumber;
	public JEXData percentCellsWithParticle;
	public JEXData meanCellArea;
	public JEXData cellSpreadIndex;
	public JEXData moreInfo;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	boolean watershed = true;
	int rollingBall1 = 30;
	int rollingBall2 = 30;
	int minRadius1 = 10;
	int maxRadius1 = 1000;
	int minRadius2 = 10;
	int maxRadius2 = 1000;
	int nbDilate = 0;
	int nbErode = 0;
	int minCThresh = -1;
	int minPThresh = -1;
	int contrastPlus = 1;
	String cthreshMethod = "Mean";
	String pthreshMethod = "Mean";
	
	boolean createBackground = false;
	boolean lightBackground = false;
	boolean useParaboloid = false;
	boolean doPresmooth = false;
	boolean correctCorners = false;
	
	// Input
	JEXData particleImageData;
	JEXData cellImageData;
	JEXData bfImageData;
	JEXData roiData;
	JEXEntry entry;
	TypeName[] outputNames;
	
	// Variables used during the function steps
	private String bfPath;
	private String finalPath;
	
	private String cellPath;
	private String cellMaskPath;
	private String cellPathMerge;
	private String cellPathMergeWithRois;
	
	private String particlePath;
	private String particleMaskPath;
	private String particlePathMerge;
	private String particlePathMergeWithRois;
	
	private Rectangle rectangle;
	private List<Roi> cellRois;
	private List<Roi> particleRois;
	private ResultsTable cellTable;
	private ResultsTable particleTable;
	
	PhagocytosisAnalyzer2(JEXEntry entry, JEXData particleImageData, JEXData cellImageData, JEXData bfImageData, JEXData roiData, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.particleImageData = particleImageData;
		this.cellImageData = cellImageData;
		this.bfImageData = bfImageData;
		this.roiData = roiData;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Prepare images
		bfPath = ImageReader.readObjectToImagePath(bfImageData);
		cellPath = ImageReader.readObjectToImagePath(cellImageData);
		particlePath = ImageReader.readObjectToImagePath(particleImageData);
		
		// Get the roi
		if(roiData != null)
		{
			ROIPlus roip = RoiReader.readObjectToRoi(roiData);
			rectangle = (roip != null && roip.getRoi() != null) ? roip.getRoi().getBounds() : null;
		}
		
		// Prepare the graphics
		// imageDisplay = new ImageDisplay(this,"Locate particles and cells");
		// imageDisplay.setImage(new ImagePlus(cellPath));
		imageDisplay = new ImagePanel(this, "Locate particles and cells");
		imageDisplay.setImage(new ImagePlus(cellPath));
		
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "CELL - Process image", new String[] { "RollingBall 1", "Cell threshold", "Minimum c-thresh", "Watershed", "Dilate", "Erode", "Contrast enhance" });
		wrap.addStep(1, "CELL - Find cells", new String[] { "Min. cell radius", "Max. cell radius" });
		wrap.addStep(2, "PARTICLE - Process image", new String[] { "RollingBall 2", "Particle threshold", "Minimum p-thresh" });
		wrap.addStep(3, "PARTICLE - Find particles", new String[] { "Min. particle radius", "Max. particle radius" });
		wrap.addStep(4, "ANALYZE", new String[] { "Automatic" });
		
		String title = "Analyzing entry " + entry.getEntryExperiment() + " - " + entry.getTrayX() + "." + entry.getTrayY();
		wrap.setTitle(title);
		wrap.setInCentralPanel(imageDisplay);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{
		// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		
		rollingBall1 = (int) Double.parseDouble(params.getValueOfParameter("RollingBall 1"));
		cthreshMethod = params.getValueOfParameter("Cell threshold");
		minCThresh = (int) Double.parseDouble(params.getValueOfParameter("Minimum c-thresh"));
		watershed = Boolean.parseBoolean(params.getValueOfParameter("Watershed"));
		nbDilate = (int) Double.parseDouble(params.getValueOfParameter("Dilate"));
		nbErode = (int) Double.parseDouble(params.getValueOfParameter("Erode"));
		minRadius1 = (int) Double.parseDouble(params.getValueOfParameter("Min. cell radius"));
		maxRadius1 = (int) Double.parseDouble(params.getValueOfParameter("Max. cell radius"));
		contrastPlus = (int) Double.parseDouble(params.getValueOfParameter("Contrast enhance"));
		
		rollingBall2 = (int) Double.parseDouble(params.getValueOfParameter("RollingBall 2"));
		pthreshMethod = params.getValueOfParameter("Particle threshold");
		minPThresh = (int) Double.parseDouble(params.getValueOfParameter("Minimum p-thresh"));
		minRadius2 = (int) Double.parseDouble(params.getValueOfParameter("Min. particle radius"));
		maxRadius2 = (int) Double.parseDouble(params.getValueOfParameter("Max. particle radius"));
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
		
		// auto = true;
		if(auto)
		{
			Logs.log("Processing Live Image", 1, this);
			processCellImage();
			Logs.log("Analyzing Live Image", 1, this);
			analyzeCellImage();
			Logs.log("Processing Dead Image", 1, this);
			processParticleImage();
			Logs.log("Analyzing Dead Image", 1, this);
			analyzeParticleImage();
			Logs.log("Analyzing Results", 1, this);
			analyze();
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
		
		if(atStep == 0)
		{
			processCellImage();
			imageDisplay.setImage(new ImagePlus(cellPathMerge));
			// imageDisplay.setImage(new ImagePlus(cellMaskPath));
		}
		if(atStep == 1)
		{
			analyzeCellImage();
			imageDisplay.setImage(new ImagePlus(cellPathMergeWithRois));
		}
		else if(atStep == 2)
		{
			processParticleImage();
			imageDisplay.setImage(new ImagePlus(particlePathMerge));
		}
		else if(atStep == 3)
		{
			analyzeParticleImage();
			imageDisplay.setImage(new ImagePlus(particlePathMergeWithRois));
		}
		else if(atStep == 4)
		{
			analyze();
			imageDisplay.setImage(new ImagePlus(finalPath));
		}
		
	}
	
	public void runNext()
	{
		atStep = atStep + 1;
		if(atStep > 4)
			atStep = 4;
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
	
	private void processCellImage()
	{
		// Get the processor of the cell image
		ImagePlus image = new ImagePlus(cellPath);
		ImageProcessor cellImp = (ShortProcessor) image.getProcessor().convertToShort(true);
		
		// Do the background subtract
		BackgroundSubtracter bgs = new BackgroundSubtracter();
		bgs.rollingBallBackground(cellImp, rollingBall1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		
		// Get the histogram of the image
		int[] histogram = cellImp.getHistogram();
		
		// Histogram method
		String method = cthreshMethod;
		int thresh = 0;
		
		try
		{
			Double threshValue = new Double(method);
			thresh = threshValue.intValue();
		}
		catch (java.lang.NumberFormatException e)
		{
			// Get the threshold level
			function.imageUtility.AutoThresholder thresholder = new function.imageUtility.AutoThresholder();
			thresh = thresholder.getThreshold(method, histogram);
		}
		
		// If a minimum threshold is required then test it
		thresh = (minCThresh < 0) ? thresh : (thresh < minCThresh) ? minCThresh : thresh;
		
		// Make the threshold
		cellImp.setThreshold((double) 0, (double) thresh, ImageProcessor.BLACK_AND_WHITE_LUT);
		cellImp.threshold(thresh);
		cellImp.invertLut();
		
		// Dilate and erode to remove the small clumps
		cellImp = (ByteProcessor) cellImp.convertToByte(true);
		for (int i = 0; i < this.nbErode; i++)
		{
			((ByteProcessor) cellImp).erode();
		}
		for (int i = 0; i < this.nbDilate; i++)
		{
			((ByteProcessor) cellImp).dilate();
		}
		
		// Watershed the image
		if(watershed)
		{
			EDM edm = new EDM();
			edm.toWatershed(cellImp);
		}
		
		// Save the cell mask image
		cellMaskPath = JEXWriter.saveImage(new ImagePlus("Cell mask", cellImp));
		
		// Make new imageplus
		int imWidth = cellImp.getWidth();
		int imHeight = cellImp.getHeight();
		cellImp.invertLut();
		
		// Find the normalization scale
		// double norm = cellImp.maxValue();
		
		// Make Overlay Image
		// ColorProcessor color1 = (ColorProcessor)
		// cellImp.convertToByte(true).convertToRGB();
		ImageProcessor cimp = (new ImagePlus(cellPath)).getProcessor();
		cimp.multiply((double) contrastPlus);
		cimp = cimp.convertToByte(true);
		
		// // TEMP TEMP
		// ImagePlus cellMaskMerge = new ImagePlus("Cell merge",cimp);
		// cellPathMerge = JEXWriter.saveImage(cellMaskMerge);
		// Logs.log("Saved image at path "+cellPathMerge, 1,
		// this);
		
		ColorProcessor color1 = (ColorProcessor) cimp.convertToRGB();
		ColorProcessor color2 = new ColorProcessor(imWidth, imHeight);
		byte[] color2RPixels = (byte[]) cellImp.convertToByte(true).getPixels();
		byte[] color2GPixels = new byte[imWidth * imHeight];
		byte[] color2BPixels = new byte[imWidth * imHeight];
		color2.setRGB(color2RPixels, color2GPixels, color2BPixels);
		color1.copyBits(color2, 0, 0, Blitter.MAX);
		ImagePlus cellMaskMerge = new ImagePlus("Cell merge", color1);
		
		// Save the cell mask image
		cellPathMerge = JEXWriter.saveImage(cellMaskMerge);
	}
	
	private void analyzeCellImage()
	{
		// Analyze the particles
		int options = ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.INCLUDE_HOLES;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.INTEGRATED_DENSITY | ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE | ParticleAnalyzer.PERIMETER;
		cellTable = new ResultsTable();
		ParticleAnalyzer analyzer1 = new ParticleAnalyzer(options, measure, cellTable, minRadius1, maxRadius1, 0, 1);
		analyzer1.analyze(new ImagePlus(cellMaskPath));
		
		// Find the rois
		List<Roi> cellRoisFound = analyzer1.foundRois;
		cellRois = new ArrayList<Roi>(0);
		
		// Keep only the rois within the optional roi
		for (Roi r : cellRoisFound)
		{
			Rectangle rect = r.getBounds();
			int xpos = (int) (rect.getX() + rect.getWidth() / 2);
			int ypos = (int) (rect.getY() + rect.getHeight() / 2);
			if(this.isPointInRectangle(xpos, ypos, rectangle))
			{
				cellRois.add(r);
			}
		}
		
		// Make an image showing the rois that were kept
		BufferedImage cimp = ((ColorProcessor) (new ImagePlus(cellPathMerge)).getProcessor()).getBufferedImage();
		Graphics g = cimp.getGraphics();
		int index = 0;
		g.setColor(Color.yellow);
		for (Roi roi : cellRois)
		{
			Polygon poly = roi.getPolygon();
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;
			Point first = null;
			Point second = null;
			for (int i = 0; i < xpos.length; i++)
			{
				int x = xpos[i];
				int y = ypos[i];
				first = second;
				second = new Point(x, y);
				
				// Draw outline
				g.fillRect(x - 1, y - 1, 2, 2);
				g.fillRect(x, y, 1, 1);
				if(first != null && second != null)
				{
					g.drawLine((int) first.getX(), (int) first.getY(), (int) second.getX(), (int) second.getY());
					g.drawLine((int) first.getX() - 1, (int) first.getY() - 1, (int) second.getX() - 1, (int) second.getY() - 1);
				}
			}
			
			// Draw central number
			java.awt.Rectangle r = roi.getBounds();
			int cx = (int) (r.getX() + r.getWidth() / 2);
			int cy = (int) (r.getY() + r.getHeight() / 2);
			g.drawString("" + index, cx, cy);
		}
		g.dispose();
		
		// Make image
		ImagePlus cellMaskMergeWithRois = new ImagePlus("", cimp);
		cellPathMergeWithRois = JEXWriter.saveImage(cellMaskMergeWithRois);
	}
	
	private void processParticleImage()
	{
		// Get the processor of the cell image
		ImagePlus image = new ImagePlus(particlePath);
		ShortProcessor particleImp = (ShortProcessor) image.getProcessor().convertToShort(true);
		
		// Do the background subtract
		BackgroundSubtracter bgs = new BackgroundSubtracter();
		bgs.rollingBallBackground(particleImp, rollingBall2, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		
		// Get the histogram of the image
		int[] histogram = particleImp.getHistogram();
		
		// Find the thresholding level, using an automated algorithm if
		// necessary
		String method = pthreshMethod;
		int thresh = 0;
		
		try
		{
			Double threshValue = new Double(method);
			thresh = threshValue.intValue();
		}
		catch (java.lang.NumberFormatException e)
		{
			// Get the threshold level
			function.imageUtility.AutoThresholder thresholder = new function.imageUtility.AutoThresholder();
			thresh = thresholder.getThreshold(method, histogram);
		}
		
		// If a minimum threshold is required then test it
		thresh = (minPThresh < 0) ? thresh : (thresh < minPThresh) ? minPThresh : thresh;
		
		// Make the threshold
		particleImp.setThreshold((double) 0, (double) thresh, ImageProcessor.BLACK_AND_WHITE_LUT);
		particleImp.threshold(thresh);
		particleImp.invertLut();
		
		// Dilate and erode to remove the small clumps
		ByteProcessor particleImpByte = (ByteProcessor) particleImp.convertToByte(true);
		particleImpByte.erode();
		particleImpByte.dilate();
		particleImpByte.dilate();
		// ByteProcessor cellImpByte = (ByteProcessor)
		// cellImp.convertToByte(true);
		// for (int i=0; i<this.nbErode; i++)
		// {
		// cellImpByte.erode();
		// }
		// for (int i=0; i<this.nbDilate; i++)
		// {
		// cellImpByte.dilate();
		// }
		
		// Watershed the image
		EDM edm = new EDM();
		edm.toWatershed(particleImpByte);
		
		// Save the cell mask image
		particleMaskPath = JEXWriter.saveImage(new ImagePlus("Particle mask", particleImpByte));
		
		// Make new imageplus
		int imWidth = particleImpByte.getWidth();
		int imHeight = particleImpByte.getHeight();
		
		// Make a displayable image for viewer feedback
		ByteProcessor particleByte = (ByteProcessor) particleImpByte.convertToByte(true);
		particleByte.invertLut();
		
		// Make Overlay Image
		ColorProcessor color1 = (ColorProcessor) particleByte.convertToRGB();
		ColorProcessor color2 = new ColorProcessor(imWidth, imHeight);
		byte[] color2RPixels = (byte[]) particleImpByte.convertToByte(true).getPixels();
		byte[] color2GPixels = new byte[imWidth * imHeight];
		byte[] color2BPixels = new byte[imWidth * imHeight];
		color2.setRGB(color2RPixels, color2GPixels, color2BPixels);
		color1.copyBits(color2, 0, 0, Blitter.MAX);
		ImagePlus particleMaskMerge = new ImagePlus("Particle merge", color1);
		
		// Save the cell mask image
		particlePathMerge = JEXWriter.saveImage(particleMaskMerge);
	}
	
	private void analyzeParticleImage()
	{
		// Analyze the particles
		int options = ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.INCLUDE_HOLES;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.INTEGRATED_DENSITY | ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE | ParticleAnalyzer.PERIMETER;
		particleTable = new ResultsTable();
		ParticleAnalyzer analyzer2 = new ParticleAnalyzer(options, measure, particleTable, minRadius2, maxRadius2, 0, 1);
		analyzer2.analyze(new ImagePlus(particleMaskPath));
		
		// Find the rois
		List<Roi> particleRoisFound = analyzer2.foundRois;
		particleRois = new ArrayList<Roi>(0);
		
		// Keep only the rois within the optional roi
		for (Roi r : particleRoisFound)
		{
			Rectangle rect = r.getBounds();
			int xpos = (int) (rect.getX() + rect.getWidth() / 2);
			int ypos = (int) (rect.getY() + rect.getHeight() / 2);
			if(this.isPointInRectangle(xpos, ypos, rectangle))
			{
				particleRois.add(r);
			}
		}
		
		// Make an image showing the rois that were kept
		BufferedImage cimp = ((ColorProcessor) (new ImagePlus(particlePathMerge)).getProcessor()).getBufferedImage();
		Graphics g = cimp.getGraphics();
		int index = 0;
		g.setColor(Color.yellow);
		for (Roi roi : particleRois)
		{
			Polygon poly = roi.getPolygon();
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;
			Point first = null;
			Point second = null;
			for (int i = 0; i < xpos.length; i++)
			{
				int x = xpos[i];
				int y = ypos[i];
				first = second;
				second = new Point(x, y);
				
				// Draw outline
				g.fillRect(x - 1, y - 1, 2, 2);
				g.fillRect(x, y, 1, 1);
				if(first != null && second != null)
				{
					g.drawLine((int) first.getX(), (int) first.getY(), (int) second.getX(), (int) second.getY());
					g.drawLine((int) first.getX() - 1, (int) first.getY() - 1, (int) second.getX() - 1, (int) second.getY() - 1);
				}
			}
			
			// Draw central number
			java.awt.Rectangle r = roi.getBounds();
			int cx = (int) (r.getX() + r.getWidth() / 2);
			int cy = (int) (r.getY() + r.getHeight() / 2);
			g.drawString("" + index, cx, cy);
		}
		g.dispose();
		
		// Make image
		ImagePlus particleMaskMergeWithRois = new ImagePlus("", cimp);
		particlePathMergeWithRois = JEXWriter.saveImage(particleMaskMergeWithRois);
	}
	
	private void analyze()
	{
		int numberOfCells = 0;
		int percentOfCellsWithparticles = 0;
		int numberOfParticlesPerCell = 0;
		double meanCellSpreadIndex = 0;
		double averageCellArea = 0;
		List<Point> locationCells = new ArrayList<Point>();
		List<Point> locationParticle = new ArrayList<Point>();
		TreeMap<Integer,List<Point>> particleMap = new TreeMap<Integer,List<Point>>();
		TreeMap<Integer,List<Roi>> roiMap = new TreeMap<Integer,List<Roi>>();
		List<Roi> outsideParticles = new ArrayList<Roi>();
		
		// Analyze the particle result table
		int lastColumn = particleTable.getLastColumn();
		float[] partXPos = new float[0];
		float[] partYPos = new float[0];
		for (int i = 0; i < lastColumn; i++)
		{
			String cName = particleTable.getColumnHeading(i);
			if(cName.equals("X"))
			{
				partXPos = particleTable.getColumn(i);
			}
			if(cName.equals("Y"))
			{
				partYPos = particleTable.getColumn(i);
			}
		}
		
		// Loop through all the points
		for (int i = 0; i < partXPos.length; i++)
		{
			// Make the point
			float x = partXPos[i];
			float y = partYPos[i];
			Point p = new Point((int) x, (int) y);
			
			// If the point is in a cell ROI keep it
			int cindex = roiContainingPoint(cellRois, p);
			if(cindex == -1)
			{
				int pindex = roiContainingPoint(particleRois, p);
				if(pindex == -1)
					continue;
				Roi particleRoi = particleRois.get(pindex);
				outsideParticles.add(particleRoi);
				continue;
			}
			
			// Get the particle roi associated to it
			int pindex = roiContainingPoint(particleRois, p);
			if(pindex == -1)
				continue;
			Roi particleRoi = particleRois.get(pindex);
			
			// Add it to the roiMap
			List<Roi> roisInCell = roiMap.get(cindex);
			if(roisInCell == null)
			{
				roisInCell = new ArrayList<Roi>();
				roiMap.put(cindex, roisInCell);
			}
			roisInCell.add(particleRoi);
			
			// Get the roi in the particleMap, create one if none already exist
			List<Point> pointsInCell = particleMap.get(cindex);
			if(pointsInCell == null)
			{
				pointsInCell = new ArrayList<Point>();
				particleMap.put(cindex, pointsInCell);
			}
			pointsInCell.add(p);
			
			// Add to particle list
			locationParticle.add(p);
		}
		percentOfCellsWithparticles = (cellRois.size() == 0) ? 0 : (100 * particleMap.size()) / cellRois.size();
		
		// Get the average number of particles per cell
		for (int index = 0; index < cellRois.size(); index++)
		{
			List<Roi> prois = roiMap.get(index);
			int nbpart = (prois == null) ? 0 : prois.size();
			numberOfParticlesPerCell = numberOfParticlesPerCell + nbpart;
		}
		numberOfParticlesPerCell = (int) ((double) numberOfParticlesPerCell / (double) cellRois.size());
		numberOfCells = cellRois.size();
		
		// Analyze the cell result table
		lastColumn = cellTable.getLastColumn();
		float[] cellXPos = new float[0];
		float[] cellYPos = new float[0];
		float[] cellAreas = new float[0];
		float[] cellPerim = new float[0];
		for (int i = 0; i < lastColumn; i++)
		{
			String cName = cellTable.getColumnHeading(i);
			if(cName.equals("X"))
			{
				cellXPos = cellTable.getColumn(i);
			}
			if(cName.equals("Y"))
			{
				cellYPos = cellTable.getColumn(i);
			}
			if(cName.equals("Area"))
			{
				cellAreas = cellTable.getColumn(i);
			}
			if(cName.equals("Perim."))
			{
				cellAreas = cellTable.getColumn(i);
			}
		}
		
		for (int i = 0; i < cellXPos.length; i++)
		{
			float x = cellXPos[i];
			float y = cellYPos[i];
			Point p = new Point((int) x, (int) y);
			locationCells.add(p);
		}
		for (int i = 0; i < cellAreas.length; i++)
		{
			if(i >= cellAreas.length || i >= cellPerim.length)
				continue;
			float area = cellAreas[i];
			float perim = cellPerim[i];
			double spreadIndex = (double) perim * perim / (4 * Math.PI * area);
			
			averageCellArea = averageCellArea + (double) (area / cellAreas.length);
			meanCellSpreadIndex = meanCellSpreadIndex + spreadIndex / cellAreas.length;
		}
		
		// Make the image
		ImagePlus finalMerge = plotImage(bfPath, roiMap, outsideParticles);
		this.finalPath = JEXWriter.saveImage(finalMerge);
		
		// Make the JEXDatas
		outputImage = ImageWriter.makeImageObject(outputNames[0].getName(), finalMerge);
		outputImage.setDataObjectInfo("Merged image of defined cells and particles found");
		
		particlesPerCell = ValueWriter.makeValueObject(outputNames[1].getName(), "" + numberOfParticlesPerCell);
		particlesPerCell.setDataObjectInfo("Mean number of particles per cell");
		
		cellNumber = ValueWriter.makeValueObject(outputNames[2].getName(), "" + numberOfCells);
		cellNumber.setDataObjectInfo("Total number of cells in the image");
		
		percentCellsWithParticle = ValueWriter.makeValueObject(outputNames[3].getName(), "" + percentOfCellsWithparticles);
		percentCellsWithParticle.setDataObjectInfo("Percent of the cells that contain a particle");
		
		meanCellArea = ValueWriter.makeValueObject(outputNames[4].getName(), "" + averageCellArea);
		meanCellArea.setDataObjectInfo("Mean cell area in pixels");
		
		cellSpreadIndex = ValueWriter.makeValueObject(outputNames[5].getName(), "" + meanCellSpreadIndex);
		cellSpreadIndex.setDataObjectInfo("Mean ratio of perimeter over the area of a cell");
		
		moreInfo = ValueWriter.makeValueObject(outputNames[6].getName(), "More info");
		moreInfo.setDataObjectInfo("More info on localization of the cells and particles");
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private ImagePlus plotImage(String brightFieldPath, TreeMap<Integer,List<Roi>> roiMap, List<Roi> outsideRois)
	{
		// Load BF image
		ImagePlus brightFieldImage = new ImagePlus(brightFieldPath);
		ImageProcessor bfProc = brightFieldImage.getProcessor();
		bfProc.multiply((double) contrastPlus);
		ColorProcessor color = (ColorProcessor) bfProc.convertToRGB();
		// ColorProcessor color = (ColorProcessor)
		// brightFieldImage.getProcessor().convertToRGB();
		BufferedImage cimp = color.getBufferedImage();
		Graphics g = cimp.getGraphics();
		
		// Random color generator
		Random random = new Random();
		
		// Draw the cell rois
		for (int index = 0, len = cellRois.size(); index < len; index++)
		{
			Roi cellRoi = cellRois.get(index);
			
			// Generate a random color
			Color c = Color.getHSBColor(random.nextFloat(), 0.85F, 1.0F);
			g.setColor(c);
			
			// Draw the cell roi
			Polygon poly = cellRoi.getPolygon();
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;
			Point first = null;
			Point second = null;
			for (int i = 0; i < xpos.length; i++)
			{
				int x = xpos[i];
				int y = ypos[i];
				first = second;
				second = new Point(x, y);
				
				// Draw outline
				g.fillRect(x - 1, y - 1, 2, 2);
				g.fillRect(x, y, 1, 1);
				if(first != null && second != null)
				{
					g.drawLine((int) first.getX(), (int) first.getY(), (int) second.getX(), (int) second.getY());
					g.drawLine((int) first.getX() - 1, (int) first.getY() - 1, (int) second.getX() - 1, (int) second.getY() - 1);
				}
			}
			
			// Draw the particles within
			List<Roi> insideRois = roiMap.get(index);
			if(insideRois == null)
				continue;
			for (Roi insideRoi : insideRois)
			{
				java.awt.Rectangle bounds = insideRoi.getBounds();
				for (int xx = 0; xx < bounds.getWidth(); xx++)
				{
					for (int yy = 0; yy < bounds.getHeight(); yy++)
					{
						int drawX = xx + (int) bounds.getX();
						int drawY = yy + (int) bounds.getY();
						
						if(insideRoi.contains(drawX, drawY))
						{
							g.fillRect(drawX, drawY, 1, 1);
						}
					}
				}
			}
		}
		
		// Generate a random color
		g.setColor(Color.red);
		for (Roi outsideRoi : outsideRois)
		{
			// Draw the cell roi
			Polygon ppoly = outsideRoi.getPolygon();
			int[] pxpos = ppoly.xpoints;
			int[] pypos = ppoly.ypoints;
			Point pfirst = null;
			Point psecond = null;
			for (int i = 0; i < pxpos.length; i++)
			{
				int x = pxpos[i];
				int y = pypos[i];
				pfirst = psecond;
				psecond = new Point(x, y);
				
				// Draw outline
				g.fillRect(x - 1, y - 1, 2, 2);
				g.fillRect(x, y, 1, 1);
				if(pfirst != null && psecond != null)
				{
					g.drawLine((int) pfirst.getX(), (int) pfirst.getY(), (int) psecond.getX(), (int) psecond.getY());
					g.drawLine((int) pfirst.getX() - 1, (int) pfirst.getY() - 1, (int) psecond.getX() - 1, (int) psecond.getY() - 1);
				}
			}
		}
		
		ImagePlus result = new ImagePlus("brightField merge", cimp);
		return result;
	}
	
	/**
	 * Return the roi from the list of rois ROIS that contains the point p, NULL is none do
	 * 
	 * @param rois
	 * @param p
	 * @return
	 */
	private Integer roiContainingPoint(List<Roi> rois, Point p)
	{
		for (int index = 0; index < rois.size(); index++)
		{
			Roi roi = rois.get(index);
			if(roi.contains((int) p.getX(), (int) p.getY()))
				return index;
		}
		return -1;
	}
	
	private boolean isPointInRectangle(int x, int y, Rectangle rect)
	{
		if(rect == null)
			return true;
		if(x > rect.getX() && x < rect.getX() + rect.getWidth() && y > rect.getY() && y < rect.getY() + rect.getHeight())
			return true;
		return false;
	}
	
	public void rightClickedPoint(Point p)
	{}
	
	public void extendedRectangle(Rectangle r)
	{}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
