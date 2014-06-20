package function.plugin.old;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.Trajectory;
import image.roi.Vect;
import image.roi.VectSet;
import image.roi.XTrajectorySet;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
import tables.DimensionMap;
import utilities.StringUtility;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.MovieWriter;
import Database.DataWriter.TrackWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.FindMaxima;
import function.tracker.SimpleConvolve;
import function.tracker.TrackExtend;
import function.tracker.TrackStatistics;
import function.tracker.TracksMovieMaker;

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
public class JEX_WholeBloodNeutrophilMigration extends JEXCrunchable {
	
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
		String result = "Analyze migration v1";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Analyze the migration of cells in bright field, using a neutrophil matching approach";
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
		TypeName[] inputNames = new TypeName[3];
		
		inputNames[0] = new TypeName(IMAGE, "Timelapse");
		inputNames[1] = new TypeName(ROI, "Optional ROI");
		inputNames[2] = new TypeName(IMAGE, "Optional Cell");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return name of outputs
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[8];
		defaultOutputNames[0] = new TypeName(TRACK, "Tracks");
		defaultOutputNames[1] = new TypeName(VALUE, "Velocity");
		defaultOutputNames[2] = new TypeName(VALUE, "X displacement");
		defaultOutputNames[3] = new TypeName(VALUE, "Y displacement");
		defaultOutputNames[4] = new TypeName(VALUE, "Chemotaxis index");
		defaultOutputNames[5] = new TypeName(VALUE, "Track analysis");
		defaultOutputNames[6] = new TypeName(VALUE, "Temporal analysis");
		defaultOutputNames[7] = new TypeName(MOVIE, "Track Movie");
		
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
		Parameter p1 = new Parameter("Binning", "Binning factor for quicker analysis", "2.0");
		
		Parameter p2 = new Parameter("Preset cell", "Use a preset cell instead of custom ROI", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p3 = new Parameter("Cell Path", "Path to the cell image to use for the convolution", "");
		
		Parameter p4 = new Parameter("Threshold", "Threshold for identifying neutrophil locations", "50.0");
		Parameter p5 = new Parameter("Cell Radius", "Cell radius in pixels (e.g. 3 to 30)", "20");
		Parameter p6 = new Parameter("Scan size", "Scan x images prior and after the image to locate non-moving cells", "2");
		
		Parameter p7 = new Parameter("Filter tracks", "Keep tracks of mean velocity superior to", "1.3");
		Parameter p8 = new Parameter("Minimum length", "Minimum length for keeping a track in number of points", "40");
		
		Parameter p10 = new Parameter("Timepoints", "Save values in the value table every X frames", "20");
		
		Parameter p11 = new Parameter("Seconds per frame", "Duration between each frame of the timelapse", "30");
		Parameter p12 = new Parameter("Pixels per micron", "Number of pixels per micron", "1.32");
		
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
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
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
		JEXData data1 = inputs.get("Timelapse");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		if(data2 != null && (!data2.getTypeName().getType().equals(JEXData.ROI)))
			return false;
		
		JEXData data3 = inputs.get("Optional Cell");
		if(data3 != null && (!data3.getTypeName().getType().equals(JEXData.IMAGE)))
			return false;
		
		// Run the function
		NeutrophilTrackerHelperFunction2 graphFunc = new NeutrophilTrackerHelperFunction2(data1, data2, data3, entry, outputNames, parameters);
		graphFunc.doit();
		
		// Set the outputs
		realOutputs.add(graphFunc.output1);
		realOutputs.add(graphFunc.output2);
		realOutputs.add(graphFunc.output3);
		realOutputs.add(graphFunc.output4);
		realOutputs.add(graphFunc.output5);
		realOutputs.add(graphFunc.output6);
		realOutputs.add(graphFunc.output7);
		realOutputs.add(graphFunc.output8);
		
		// Return status
		return true;
	}
	
}

class NeutrophilTrackerHelperFunction2 implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	public JEXData output1;
	public JEXData output2;
	public JEXData output3;
	public JEXData output4;
	public JEXData output5;
	public JEXData output6;
	public JEXData output7;
	public JEXData output8;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	boolean useNeut = false;
	String neutPath = null;
	int scanDim = 0;
	int bin = 2;
	double threshold = 10;
	int minLength = 10;
	int radius = 15;
	double minVel = 10;
	int interval = 20;
	double spf = 1;
	double ppm = 1;
	
	// Variables used during the function steps
	private ByteProcessor imp;
	private ImagePlus im;
	static ImagePlus cellImage = null;
	private Roi imageRoi = null;
	private Roi roi = null;
	
	// Input
	JEXData imset;
	JEXData roiData;
	JEXData cell;
	JEXEntry entry;
	Rectangle rectangle;
	TypeName[] outputNames;
	List<String> jimages;
	TreeMap<Integer,PointList> pLists;
	Trajectory[] trajs;
	Trajectory[] finaltrajs;
	XTrajectorySet trajSet;
	
	NeutrophilTrackerHelperFunction2(JEXData imset, JEXData roiData, JEXData cell, JEXEntry entry, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.imset = imset;
		this.roiData = roiData;
		this.cell = cell;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		this.pLists = new TreeMap<Integer,PointList>();
		
		// //// Get params
		getParams();
		
		// Prepare function
		TreeMap<DimensionMap,String> jimagesMap = ImageReader.readObjectToImagePathTable(imset);
		jimages = new ArrayList<String>();
		// for (DimensionMap map: imset.getDataMap().keySet()){
		// JEXDataSingle ds = imset.getDataMap().get(map);
		// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
		// ds.get(JEXDataSingle.FILENAME);
		// jimages.add(path);
		// }
		jimages.addAll(jimagesMap.values());
		
		if(roiData != null)
		{
			ROIPlus roip = RoiReader.readObjectToRoi(roiData);
			rectangle = (roip != null && roip.getRoi() != null) ? roip.getRoi().getBounds() : null;
		}
		
		if(cell != null)
		{
			// Get the cell image
			ImagePlus cimp = ImageReader.readObjectToImagePlus(cell);
			ImageProcessor cimpProc = cimp.getProcessor();
			
			// Bin the cell image
			int newWidth = (int) ((double) cimp.getWidth() / bin);
			cimpProc = cimpProc.resize(newWidth);
			
			// Save the new image
			cellImage = new ImagePlus("cell", cimpProc);
			// cellImage = ImageReader.readObjectToImagePlus(cell);
		}
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Analyze neutrophil migration in POCT channels");
		imagepanel.setRoi(roi);
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Reduce image size", new String[] { "Binning" });
		wrap.addStep(1, "Extract neutrophil image", new String[] { "Preset cell", "Cell Path" });
		wrap.addStep(2, "Set channel ROI", new String[0]);
		wrap.addStep(3, "Convolve", new String[] { "Scan size" });
		wrap.addStep(4, "Find neutrophils", new String[] { "Threshold", "Cell Radius" });
		wrap.addStep(5, "Apply to stack", new String[0]);
		wrap.addStep(6, "Track", new String[0]);
		wrap.addStep(7, "Select tracks", new String[] { "Filter tracks", "Minimum length" });
		wrap.addStep(8, "Analysis", new String[] { "Timepoints", "Seconds per frame", "Pixels per micron", "Automatic" });
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		minLength = Math.max(1, Integer.parseInt(params.getValueOfParameter("Minimum length")));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		minVel = Double.parseDouble(params.getValueOfParameter("Filter tracks"));
		interval = (int) Double.parseDouble(params.getValueOfParameter("Timepoints"));
		spf = Double.parseDouble(params.getValueOfParameter("Seconds per frame"));
		ppm = Double.parseDouble(params.getValueOfParameter("Pixels per micron"));
		
		useNeut = Boolean.parseBoolean(params.getValueOfParameter("Preset cell"));
		neutPath = params.getValueOfParameter("Cell Path");
		scanDim = (int) Double.parseDouble(params.getValueOfParameter("Scan size"));
		
	}
	
	private void displayImage(int index)
	{
		ImagePlus im = new ImagePlus(jimages.get(index));
		imagepanel.setImage(im);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		if(auto && rectangle != null)
		{
			// //// Get params
			getParams();
			
			makeROI();
			findMaxInStack();
			track();
			selectTracks();
			analyze();
			makeMovie();
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
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			binImage();
			interactionMode = true;
			if(auto || cellImage != null)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 1)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			binImage();
			extractImage();
			interactionMode = true;
			
			if(rectangle != null)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 2)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			binImage();
			makeROI();
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 3)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(imageRoi);
			
			binImage();
			convolve();
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 4)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			
			binImage();
			convolve();
			findMax();
			interactionMode = false;
			
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(imageRoi);
			imagepanel.setPointList(pLists.get(index));
			imagepanel.setCellRadius(radius);
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 5)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			
			findMaxInStack();
			interactionMode = false;
			
			Color[] colors = new Color[pLists.size()];
			PointList[] pls = new PointList[pLists.size()];
			for (int i = 0; i < pls.length; i++)
			{
				int shade = 255 * i / (pls.length);
				pls[i] = pLists.get(i);
				colors[i] = new Color(shade, shade, 255);
			}
			imagepanel.setPointList(null);
			imagepanel.setPointListArray(pls, colors);
			imagepanel.setCellRadius((int) radius);
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 6)
		{
			track();
			
			imagepanel.setPointList(null);
			imagepanel.setTracks(trajs);
			imagepanel.setPointListArray(null, null);
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 7)
		{
			selectTracks();
			
			imagepanel.setTracks(finaltrajs);
			imagepanel.setPointListArray(null, null);
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 8)
		{
			analyze();
			
			imagepanel.setTracks(finaltrajs);
			imagepanel.setPointListArray(null, null);
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		
		imagepanel.setImage(new ImagePlus("", imp));
	}
	
	public void runNext()
	{
		atStep = atStep + 1;
		if(atStep > 8)
			atStep = 8;
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
		index = index + 1;
		
		if(index >= jimages.size() - 1)
			index = jimages.size() - 1;
		if(index < 0)
			index = 0;
		
		runStep(atStep);
	}
	
	public void loopPrevious()
	{
		index = index - 1;
		
		if(index >= jimages.size() - 1)
			index = jimages.size() - 1;
		if(index < 0)
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
		// //// Get params
		getParams();
		
		if(!auto && finaltrajs == null)
		{
			findMaxInStack();
			track();
			selectTracks();
			analyze();
		}
		makeMovie();
	}
	
	/**
	 * Bin the image for faster processing
	 */
	private void binImage()
	{
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
	}
	
	private ImageProcessor binImage(ImageProcessor imp)
	{
		int newWidth = (int) ((double) imp.getWidth() / bin);
		ImageProcessor result = (ByteProcessor) imp.resize(newWidth);
		return result;
	}
	
	/**
	 * Get the neutrophil to recognize
	 */
	private void extractImage()
	{
		if(auto)
			return;
		if(cell != null)
		{
			// Get the cell image
			ImagePlus cimp = ImageReader.readObjectToImagePlus(cell);
			ImageProcessor cimpProc = cimp.getProcessor();
			cimpProc = (ByteProcessor) cimpProc.convertToByte(true);
			
			// Bin the cell image
			int newWidth = (int) ((double) cimp.getWidth() / bin);
			cimpProc = cimpProc.resize(newWidth);
			
			// Save the new image
			cellImage = new ImagePlus("cell", cimpProc);
		}
		else if(useNeut)
		{
			cellImage = new ImagePlus(neutPath);
			if(cellImage == null)
			{
				FloatProcessor fp = new FloatProcessor(radius, radius);
				cellImage = new ImagePlus("", fp);
			}
			else
			{
				cellImage = new ImagePlus("", (ByteProcessor) cellImage.getProcessor().convertToByte(true));
			}
		}
		else
		{
			ImagePlus image = new ImagePlus("", imp);
			image.setRoi(roi);
			ImageProcessor cellImp = image.getProcessor().crop();
			cellImage = new ImagePlus("", cellImp);
			roi = null;
		}
	}
	
	private void makeROI()
	{
		if(roiData != null)
		{
			// Get the roiplus object
			ROIPlus roip = RoiReader.readObjectToRoi(roiData);
			
			// Get the rectangle
			Rectangle r = (roip != null && roip.getRoi() != null) ? roip.getRoi().getBounds() : null;
			
			// Bin the rectangle as needed
			// int x1 = (int) r.getX()/bin;
			// int y1 = (int) r.getY()/bin;
			// int w = (int) r.getWidth()/bin;
			// int h = (int) r.getHeight()/bin;
			int x1 = (int) r.getX() / bin;
			int y1 = (int) r.getY() / bin;
			int w = (int) r.getWidth() / bin;
			int h = (int) r.getHeight() / bin;
			
			// Set the rois
			r = new Rectangle(x1, y1, w, h);
			roi = new Roi(r);
			imageRoi = new Roi(r);
		}
		else if(rectangle != null)
		{
			// Bin the rectangle as needed
			int x1 = (int) rectangle.getX() / bin;
			int y1 = (int) rectangle.getY() / bin;
			int w = (int) rectangle.getWidth() / bin;
			int h = (int) rectangle.getHeight() / bin;
			
			// Set the rois
			Rectangle r = new Rectangle(x1, y1, w, h);
			roi = new Roi(r);
			imageRoi = new Roi(r);
			// roi = new Roi(rectangle);
			// imageRoi = new Roi(rectangle);
		}
		else if(roi == null)
		{
			imageRoi = new Roi(0, 0, im.getWidth(), im.getHeight());
		}
		else
		{
			imageRoi = roi;
		}
	}
	
	private void convolve()
	{
		// make image to convolve on, if averaging is selected,
		// then subtract from the current image an mean image
		// of the images before and after to keep only objects that are moving
		if(scanDim > 0)
		{
			List<ImagePlus> imagesToaverage = new ArrayList<ImagePlus>(0);
			
			// get images after image of interest
			for (int i = 1; i <= scanDim; i++)
			{
				if(index + i < jimages.size())
				{
					ImagePlus imPlus = new ImagePlus(jimages.get(index + 1));
					imagesToaverage.add(imPlus);
				}
			}
			
			// If not enough images were found then grab some before the image
			// of interest
			if(imagesToaverage.size() < scanDim)
			{   
				
			}
			
			// Make a new image
			ImageProcessor avimp = imp.createProcessor(im.getWidth(), im.getHeight());
			
			// Average the images of the list
			for (ImagePlus image : imagesToaverage)
			{
				ImageProcessor imagep = image.getProcessor();
				imagep = imagep.convertToByte(true);
				binImage(imagep);
				imagep.multiply((double) 1 / imagesToaverage.size());
				avimp.copyBits(imagep, 0, 0, Blitter.ADD);
				Logs.log("Added image to average", 1, this);
			}
			
			// Subtract from current image
			imp.copyBits(avimp, 0, 0, Blitter.SUBTRACT);
		}
		
		// Perform the convolution
		SimpleConvolve convolver = new SimpleConvolve();
		ImagePlus result = convolver.start(new ImagePlus("", imp), cellImage, null);
		imp = (ByteProcessor) result.getProcessor().convertToByte(true);
		
		// mask the ROI
		java.awt.Rectangle rect = (roi == null) ? null : imageRoi.getBounds();
		imp = (ByteProcessor) maskImage(imp, rect);
	}
	
	private ImageProcessor maskImage(ImageProcessor proc, Rectangle rect)
	{
		// Copy and crop the original processor
		ImageProcessor copy = proc.duplicate();
		if(rect != null)
		{
			// copy.setRoi(new Roi(rect));
			copy.setRoi((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
			copy = copy.crop();
		}
		
		// Create new image with black background
		ImageProcessor result = new ByteProcessor(proc.getWidth(), proc.getHeight());
		
		// Copy the old image at the right location
		int x = 0;
		int y = 0;
		if(rect != null)
		{
			x = (int) rect.getX();
			y = (int) rect.getY();
		}
		result.copyBits(copy, x, y, Blitter.COPY);
		
		return result;
	}
	
	private void findMax()
	{
		FindMaxima finder = new FindMaxima();
		finder.threshold = (int) threshold;
		finder.cellNB = -1;
		finder.fixedCellNb = false;
		finder.cellRadius = radius;
		java.awt.Rectangle rect = (roi == null) ? null : imageRoi.getBounds();
		
		// // Make image with black rim
		// ImageProcessor imp2 = maskImage(imp,rect);
		
		PointList result = finder.findMaximum(new ImagePlus("", imp), rect);
		pLists.put(new Integer(index), result);
	}
	
	private void findMaxInStack()
	{
		for (int j = 0, len = jimages.size(); j < len; j++)
		{
			index = j;
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			
			binImage();
			convolve();
			findMax();
			
			// Status bar
			int percentage = (int) (100 * ((double) j / (double) jimages.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
	}
	
	private void track()
	{
		TrackExtend extender = new TrackExtend();
		extender.maxDisplacement = (int) radius;
		extender.maxDissapear = 3;
		extender.spf = 1;
		extender.mpp = 1;
		extender.setExtensionMode(TrackExtend.EXTEND_TO_CLOSEST);
		
		Set<Integer> keys = pLists.keySet();
		for (Integer key : keys)
		{
			PointList current = pLists.get(key);
			Logs.log("Extended points for frame " + key + " of " + keys.size() + ".", 1, this);
			extender.extendWithPoints(current, key);
		}
		
		List<Trajectory> trajectories = extender.getTrajectories();
		trajs = trajectories.toArray(new Trajectory[0]);
	}
	
	private void selectTracks()
	{
		List<Trajectory> selected = new ArrayList<Trajectory>();
		
		for (int i = 0, len = trajs.length; i < len; i++)
		{
			Trajectory t = trajs[i];
			TrackStatistics stats = new TrackStatistics();
			stats.startAnalysis(t);
			if(t.nbPoints() > minLength && stats.meanVelocity > minVel)
			{
				selected.add(t);
			}
		}
		
		finaltrajs = selected.toArray(new Trajectory[0]);
		
		output1 = TrackWriter.makeTracksObject(outputNames[0].getName(), selected);
	}
	
	private void analyze()
	{
		TrackStatistics stats = new TrackStatistics(finaltrajs);
		// TrackStatistics stats = new TrackStatistics(trajSet);
		stats.deltaFrame = 1;
		stats.micronPerPixel = 1;
		stats.nbCells = 1000000;
		stats.angleOffset = 90;
		stats.secondPerFrame = 1;
		
		stats.startAnalysis();
		double meanVelocity = stats.meanVelocity * 60 * bin / (ppm * spf);
		double chemoIndex = stats.CI;
		double xdisp = stats.xDispRate * 60 * bin / (ppm * spf);
		double ydisp = stats.yDispRate * 60 * bin / (ppm * spf);
		VectSet vectors = stats.vectors;
		
		Vector<Double> wanglesA = new Vector<Double>(0);
		Vector<Double> wanglesV = new Vector<Double>(0);
		for (int i = 0, len = vectors.size(); i < len; i++)
		{
			Vect v = vectors.get(i);
			if(v.norm() == 0)
			{
				wanglesA.add(0.0);
				wanglesV.add(0.0);
				continue;
			}
			Double a = v.angle() + 90;
			Double w = (double) v.norm() * 60 * bin / (ppm * spf);
			a = normalizeAngle(a);
			wanglesA.add(a);
			wanglesV.add(w);
		}
		
		// Mean velocity
		output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + meanVelocity);
		output2.put(JEXEntry.INFO, "Created from POCT analysis v2");
		
		// XDisplacement
		output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + xdisp);
		output3.put(JEXEntry.INFO, "Created from POCT analysis v2");
		
		// YDisplacement
		output4 = ValueWriter.makeValueObject(outputNames[3].getName(), "" + ydisp);
		output4.put(JEXEntry.INFO, "Created from POCT analysis v2");
		
		// Chemotactic index
		output5 = ValueWriter.makeValueObject(outputNames[4].getName(), "" + chemoIndex);
		output5.put(JEXEntry.INFO, "Created from POCT analysis v2");
		
		// Get value vectors
		List<Double> startinPositionX = stats.startinPositionX;
		List<Double> startinPositionY = stats.startinPositionY;
		List<Double> totalDistancePerCell = stats.totalDist;
		List<Double> effectiveDistancePerCell = stats.distTrav;
		List<Double> ciPerCell = stats.CIs;
		
		// Table sheet
		String[] columnNames = new String[] { "Start X", "Start Y", "Velocities", "Total Distance", "Effective Distance", "Chemotaxis Index", "Angles" };
		String[] rowNames = new String[startinPositionX.size()];
		for (int index = 0; index < startinPositionX.size(); index++)
			rowNames[index] = "Cell " + index;
		
		HashMap<String,String[]> columns = new HashMap<String,String[]>();
		String[] col1 = new String[startinPositionX.size()];
		String[] col2 = new String[startinPositionY.size()];
		String[] col3 = new String[wanglesV.size()];
		String[] col4 = new String[totalDistancePerCell.size()];
		String[] col5 = new String[effectiveDistancePerCell.size()];
		String[] col6 = new String[ciPerCell.size()];
		String[] col7 = new String[wanglesA.size()];
		for (int i = 0, len = startinPositionX.size(); i < len; i++)
			col1[i] = "" + startinPositionX.get(i);
		for (int i = 0, len = startinPositionY.size(); i < len; i++)
			col2[i] = "" + startinPositionY.get(i);
		for (int i = 0, len = wanglesV.size(); i < len; i++)
			col3[i] = "" + wanglesV.get(i);
		for (int i = 0, len = totalDistancePerCell.size(); i < len; i++)
			col4[i] = "" + totalDistancePerCell.get(i);
		for (int i = 0, len = effectiveDistancePerCell.size(); i < len; i++)
			col5[i] = "" + effectiveDistancePerCell.get(i);
		for (int i = 0, len = ciPerCell.size(); i < len; i++)
			col6[i] = "" + ciPerCell.get(i);
		for (int i = 0, len = wanglesA.size(); i < len; i++)
			col7[i] = "" + wanglesA.get(i);
		columns.put("Start X", col1);
		columns.put("Start Y", col2);
		columns.put("Velocities", col3);
		columns.put("Total Distance", col4);
		columns.put("Effective Distance", col5);
		columns.put("Chemotaxis Index", col6);
		columns.put("Angles", col7);
		
		output6 = ValueWriter.makeValueTable(outputNames[5].getName(), columnNames, rowNames, columns);
		output6.put(JEXEntry.INFO, "Created from POCT analysis v2");
		
		// Table sheet
		List<String> colNamesList = new ArrayList<String>(0);
		List<String> rowNamesList = new ArrayList<String>(0);
		HashMap<String,String[]> columnsList = new HashMap<String,String[]>();
		
		// Find the maximum length
		int length = 0;
		for (int i = 0, len = finaltrajs.length; i < len; i++)
		{
			Trajectory traj = finaltrajs[i];
			length = Math.max(length, traj.length());
		}
		
		// Loop through the time points
		List<String> rateTemp = new ArrayList<String>(0);
		int oldNumberOfTracks = 0;
		for (int j = 0; j < length; j++)
		{
			// Get the number of trajectories currently active between times j
			// and j+interval
			int numberOfTracks = 0;
			
			// Loop through the trajectories
			for (int i = 0, len = finaltrajs.length; i < len; i++)
			{
				// Get trajectory number i
				Trajectory traj = finaltrajs[i];
				
				// Loop from j to j+interval
				for (int z = 0; z < interval; z++)
				{
					Point p = traj.getPoint(z);
					if(p != null)
						numberOfTracks = numberOfTracks + 1;
				}
			}
			int nb = numberOfTracks - oldNumberOfTracks;
			oldNumberOfTracks = numberOfTracks;
			rateTemp.add("" + nb);
			
			// Make absiceses
			rowNamesList.add("Frame " + StringUtility.fillLeft("" + j, 3, "0"));
		}
		
		// Add first colum
		colNamesList.add("Frame");
		columnsList.put("Frame", rowNamesList.toArray(new String[0]));
		
		// Add rate column
		String[] rateCol = rateTemp.toArray(new String[0]);
		colNamesList.add("Rate of incoming");
		columnsList.put("Rate of incoming", rateCol);
		
		// Loop through the trajectories
		// for (int i=0, len=finaltrajs.length; i<len; i++)
		// {
		// // Get trajectory number i
		// Trajectory traj = finaltrajs[i];
		// TrajectoryStatistics trajStat = new TrajectoryStatistics(traj);
		//
		// // Get the values of interest with a sliding window
		// int j = 0;
		// List<String> colValuesTemp = new ArrayList<String>(0);
		// while (j<traj.length()){
		//
		// // Calculate velocity
		// trajStat.startAnalysis(j, j+interval);
		//
		// // Add value to list
		// double velocity = trajStat.meanVelocity * 60 * bin / (ppm * spf);
		// colValuesTemp.add(""+velocity);
		// j = j + interval;
		// }
		//
		// // Make the data lists
		// String[] colValues = colValuesTemp.toArray(new String[0]);
		// columnsList.put("Trajectory "+i, colValues);
		// colNamesList.add("Trajectory "+i);
		// }
		
		String[] columnArrayNames = colNamesList.toArray(new String[0]);
		String[] rowArraynames = rowNamesList.toArray(new String[0]);
		output7 = ValueWriter.makeValueTable(outputNames[6].getName(), columnArrayNames, rowArraynames, columnsList);
		// output7 = ValueWriter.makeValueTable(outputNames[6],
		// columnArrayNames, columnsList);
		output7.put(JEXEntry.INFO, "Created from POCT analysis v2");
	}
	
	private void makeMovie()
	{
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		Logs.log("Saving the movie to path " + path, 1, this);
		
		TracksMovieMaker movieMaker = new TracksMovieMaker(path, jimages, finaltrajs);
		movieMaker.setImageBinning(bin);
		// movieMaker.setTrackBinning(bin);
		movieMaker.makeMovie();
		
		// Make the JEXTrack object
		output8 = MovieWriter.makeMovieObject(outputNames[7].getName(), path);
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{
		if(interactionMode)
		{
			first = p;
			second = null;
			imagepanel.setRoi(null);
			roi = null;
		}
	}
	
	public void mouseMoved(Point p)
	{
		if(interactionMode)
		{
			second = p;
			
			int roiW = Math.abs(second.x - first.x);
			int roiH = Math.abs(second.y - first.y);
			int roiX = Math.min(second.x, first.x);
			int roiY = Math.min(second.y, first.y);
			roi = new Roi(roiX, roiY, roiW, roiH);
			imagepanel.setRoi(roi);
		}
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
}
