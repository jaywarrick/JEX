package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import image.roi.PointList;
import image.roi.Trajectory;
import image.roi.Vect;
import image.roi.VectSet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.ImageUtility;
import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.MovieWriter;
import Database.DataWriter.TrackWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.FindMaxima;
import function.tracker.SimpleConvolve;
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
public class JEX_WholeBloodNeutrophilMigration2 extends ExperimentalDataCrunch {
	
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
		String result = "Analyze migration v2";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Analyze the migration of cells in bright field, using an adaptive neutrophil mask";
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
		TypeName[] inputNames = new TypeName[2];
		
		inputNames[0] = new TypeName(IMAGE, "Timelapse");
		inputNames[1] = new TypeName(ROI, "Optional ROI");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return name of outputs
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[7];
		defaultOutputNames[0] = new TypeName(TRACK, "Tracks");
		defaultOutputNames[1] = new TypeName(VALUE, "Velocity");
		defaultOutputNames[2] = new TypeName(VALUE, "X displacement");
		defaultOutputNames[3] = new TypeName(VALUE, "Y displacement");
		defaultOutputNames[4] = new TypeName(VALUE, "Chemotaxis index");
		defaultOutputNames[5] = new TypeName(VALUE, "Track analysis");
		defaultOutputNames[6] = new TypeName(MOVIE, "Track Movie");
		
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
		
		Parameter p2 = new Parameter("Threshold", "Threshold for identifying neutrophil locations", "50.0");
		Parameter p3 = new Parameter("Immobility frames", "Number of frames to average on for background removal", "20.0");
		Parameter p4 = new Parameter("Particle size", "Particle convoluation radius", "3.0");
		Parameter p12 = new Parameter("Exclusion radius", "Cell exclusion radius for maximum finder", "10.0");
		
		Parameter p5 = new Parameter("Cell Radius", "Cell radius in pixels (e.g. 3 to 30)", "10");
		Parameter p6 = new Parameter("Cell Movement", "Maximum movement of a cell per frame (e.g. 3 to 30)", "10");
		Parameter p13 = new Parameter("Rolling ball", "Radius of rolling ball (-1 means no rolling ball)", "-1");
		Parameter p14 = new Parameter("Starting times", "Interval in number of frames to search for new cells and start tracking them", "-1");
		
		Parameter p7 = new Parameter("Filter tracks", "Keep tracks of mean velocity superior to", "1.3");
		Parameter p8 = new Parameter("Minimum length", "Minimum length for keeping a track in number of points", "40");
		
		Parameter p9 = new Parameter("Timepoints", "Save values in the value table every X frames", "20");
		
		Parameter p10 = new Parameter("Seconds per frame", "Duration between each frame of the timelapse", "30");
		Parameter p11 = new Parameter("Pixels per micron", "Number of pixels per micron", "1.32");
		
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
		JEXData data1 = inputs.get("Timelapse");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		if(data2 != null && (!data2.getTypeName().getType().equals(JEXData.ROI)))
			return false;
		
		// Run the function
		NeutrophilTrackerHelperFunction3 graphFunc = new NeutrophilTrackerHelperFunction3(data1, data2, outputNames, parameters);
		graphFunc.doit();
		
		// Set the outputs
		realOutputs.add(graphFunc.output1);
		realOutputs.add(graphFunc.output2);
		realOutputs.add(graphFunc.output3);
		realOutputs.add(graphFunc.output4);
		realOutputs.add(graphFunc.output5);
		realOutputs.add(graphFunc.output6);
		realOutputs.add(graphFunc.output7);
		
		// Return status
		return true;
	}
	
}

class NeutrophilTrackerHelperFunction3 implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int bin = 2;
	int radius = 15;
	int movement = 15;
	int rollBall = 15;
	int startTimes = -1;
	int immobile = 10;
	int psize = 7;
	int exclusionRad = 10;
	double threshold = 10;
	int minLength = 10;
	double minVel = 10;
	int interval = 20;
	double spf = 1;
	double ppm = 1;
	
	// Non controlable parameters
	double radiusForRemovingTrack = 1.5;
	int radiusForFindingCells = 3;
	
	// Variables used during the function steps
	private ByteProcessor imp;
	private ImagePlus im;
	private ImagePlus imageWithoutImmobile;
	private ImagePlus convolvedImage;
	static ImagePlus cellImage = null;
	
	// Input
	private PointList seedParticles;
	private TreeMap<Integer,Point> trail;
	
	private TypeName[] outputNames;
	private List<String> jimages;
	private List<ImagePlus> impmages;
	
	private Trajectory[] trajs;
	private Trajectory[] selected;
	
	NeutrophilTrackerHelperFunction3(JEXData imset, JEXData roiData, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		TreeMap<DimensionMap,String> jimagesMap = ImageReader.readObjectToImagePathTable(imset);
		jimages = new ArrayList<String>();
		// for (DimensionMap map: imset.getDataMap().keySet()){
		// JEXDataSingle ds = imset.getDataMap().get(map);
		// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
		// ds.get(JEXDataSingle.FILENAME);
		// jimages.add(path);
		// }
		jimages.addAll(jimagesMap.values());
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Analyze neutrophil migration in POCT channels");
		
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Reduce image size", new String[] { "Binning" });
		wrap.addStep(1, "Remove immobile particles", new String[] { "Immobility frames" });
		wrap.addStep(2, "Convolve", new String[] { "Particle size" });
		wrap.addStep(3, "Find particles", new String[] { "Exclusion radius", "Threshold" });
		wrap.addStep(4, "Track Neutrophil", new String[] { "Cell Movement", "Rolling ball", "Starting times" });
		wrap.addStep(5, "Select tracks", new String[] { "Filter tracks", "Minimum length" });
		wrap.addStep(6, "Analysis", new String[] { "Timepoints", "Seconds per frame", "Pixels per micron", "Automatic" });
		
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
		
		immobile = (int) Double.parseDouble(params.getValueOfParameter("Immobility frames"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		psize = (int) Double.parseDouble(params.getValueOfParameter("Particle size"));
		exclusionRad = (int) Double.parseDouble(params.getValueOfParameter("Exclusion radius"));
		
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		movement = (int) Double.parseDouble(params.getValueOfParameter("Cell Movement"));
		rollBall = (int) Double.parseDouble(params.getValueOfParameter("Rolling ball"));
		startTimes = (int) Double.parseDouble(params.getValueOfParameter("Starting times"));
		
		minLength = Math.max(1, Integer.parseInt(params.getValueOfParameter("Minimum length")));
		minVel = Double.parseDouble(params.getValueOfParameter("Filter tracks"));
		
		interval = (int) Double.parseDouble(params.getValueOfParameter("Timepoints"));
		spf = Double.parseDouble(params.getValueOfParameter("Seconds per frame"));
		ppm = Double.parseDouble(params.getValueOfParameter("Pixels per micron"));
	}
	
	private void displayImage(int index)
	{
		// ImagePlus im = jimages.get(index).getImagePlus();
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
		// //// Get params
		getParams();
		
		if(auto)
		{
			// im = jimages.get(index).getImagePlus();
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			binImage();
			removeImobile();
			convolveImage();
			locateSeedParticles();
			trackNeutrophilsFromStartingPointList();
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
		// im = jimages.get(index).getImagePlus();
		im = new ImagePlus(jimages.get(index));
		imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		imagepanel.setPointListArray(null, null);
		imagepanel.setRoi(null);
		imagepanel.setTracks(null);
		
		if(atStep == 0)
		{
			binImage();
			interactionMode = true;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 1)
		{
			binImage();
			removeImobile();
			interactionMode = true;
			
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 2)
		{
			binImage();
			convolveImage();
			interactionMode = true;
			
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 3)
		{
			binImage();
			locateSeedParticles();
			interactionMode = true;
			
			imagepanel.setCellRadius(radius);
			imagepanel.setPointList(seedParticles);
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 4)
		{
			binImage();
			trackNeutrophilsFromStartingPointList();
			
			imagepanel.setTracks(trajs);
			
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 5)
		{
			binImage();
			selectTracks();
			
			imagepanel.setTracks(trajs);
			
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 6)
		{
			binImage();
			analyze();
			
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
		if(atStep > 6)
			atStep = 6;
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
		makeMovie();
		makeFirstFrame();
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
	private ImagePlus binImage(ImagePlus im)
	{
		ImageProcessor imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
		ImagePlus result = new ImagePlus("", imp);
		return result;
	}
	
	/**
	 * Remove immobile particles
	 */
	private void removeImobile()
	{
		// // make image to convolve on, if averaging is selected,
		// // then subtract from the current image an mean image
		// // of the images before and after to keep only objects that are
		// moving
		// if (immobile > 0){
		// List<ImagePlus> imagesToaverage = new ArrayList<ImagePlus>(0);
		//
		// // get images after image of interest
		// for (int i=1; i<=immobile; i++){
		// if (index+i < jimages.size())
		// imagesToaverage.add(jimages.get(index+i).getImagePlus());
		// }
		//
		// // If not enough images were found then grab some before the image of
		// interest
		// if (imagesToaverage.size() < immobile){
		//
		// }
		//
		// // Make a new image
		// ImageProcessor avimp = imp.createProcessor(im.getWidth(),
		// im.getHeight());
		//
		// // Average the images of the list
		// for (ImagePlus image: imagesToaverage){
		// image = binImage(image);
		// ImageProcessor imagep = image.getProcessor();
		// imagep.multiply((double)1/imagesToaverage.size());
		// avimp.copyBits(imagep, 0, 0, Blitter.ADD);
		// Logs.log("Added image to average",1,this);
		// }
		//
		// // Subtract from current image
		// imp.copyBits(avimp, 0, 0, Blitter.SUBTRACT);
		// }
		//
		// imageWithoutImmobile = new ImagePlus("",imp.duplicate());
		imageWithoutImmobile = removeImobile(new ImagePlus("", imp), index);
	}
	
	/**
	 * Remove the imobile parts of the timelapse image
	 * 
	 * @param image
	 * @param frame
	 * @return imageplus
	 */
	private ImagePlus removeImobile(ImagePlus image, int frame)
	{
		ImageProcessor imp = image.getProcessor();
		
		// make image to convolve on, if averaging is selected,
		// then subtract from the current image an mean image
		// of the images before and after to keep only objects that are moving
		if(immobile > 0)
		{
			List<ImagePlus> imagesToaverage = new ArrayList<ImagePlus>(0);
			
			// get images after image of interest
			for (int i = frame; i <= frame + immobile; i++)
			{
				// if (i < jimages.size())
				// imagesToaverage.add(jimages.get(i).getImagePlus());
				if(i < jimages.size())
					imagesToaverage.add(new ImagePlus(jimages.get(i)));
			}
			
			// If not enough images were found then grab some before the image
			// of interest
			if(imagesToaverage.size() < immobile)
			{   
				
			}
			
			// Make a new image
			ImageProcessor avimp = imp.createProcessor(image.getWidth(), image.getHeight());
			
			// Average the images of the list
			for (ImagePlus otherimage : imagesToaverage)
			{
				otherimage = binImage(otherimage);
				ImageProcessor imagep = otherimage.getProcessor();
				imagep.multiply((double) 1 / imagesToaverage.size());
				avimp.copyBits(imagep, 0, 0, Blitter.ADD);
				Logs.log("Added image to average", 1, this);
			}
			
			// Subtract from current image
			imp.copyBits(avimp, 0, 0, Blitter.SUBTRACT);
		}
		
		ImagePlus imageWithoutImmobile = new ImagePlus("", imp.duplicate());
		return imageWithoutImmobile;
	}
	
	/**
	 * Return an image of width and hight W and H of a disk of diameter PARTICLESIZE
	 * 
	 * @param w
	 * @param h
	 * @param particleSize
	 * @return
	 */
	private ImagePlus makeCellOfRadius(int w, int h, int particleSize)
	{
		// Create a buffered image using the B&W model
		int type = BufferedImage.TYPE_BYTE_BINARY;
		BufferedImage bimage = new BufferedImage(w, h, type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint a circle in the center
		g.setColor(Color.white);
		g.fillOval(w / 2 - particleSize, h / 2 - particleSize, 2 * particleSize, 2 * particleSize);
		g.dispose();
		
		// Make an imageplus
		ImagePlus result = new ImagePlus("", bimage);
		ImageProcessor imp = (ByteProcessor) result.getProcessor().convertToByte(true);
		result = new ImagePlus("", imp);
		
		return result;
	}
	
	/**
	 * Convolve image
	 */
	private void convolveImage()
	{
		// Logs.log("Convolving image",1,this);
		//
		// // make cell image
		// cellImage = makeCellOfRadius(4*psize,4*psize,psize);
		//
		// // Make bufferedimage source
		// BufferedImage bim =
		// ImageUtility.imagePlus2BufferedImage(imageWithoutImmobile);
		//
		// // make the kernel
		// FloatProcessor convFloat = (FloatProcessor)
		// cellImage.getProcessor().convertToFloat();
		// float[][] convMat = convFloat.getFloatArray();
		// Kernel kernel = SimpleConvolve.makeConvolutionKernel(convMat,
		// cellImage, imageWithoutImmobile);
		//
		// // Make the convolution operator
		// ConvolveOp convolver = new ConvolveOp(kernel);
		//
		// // Make the destination image
		// ColorModel model = im.getProcessor().getColorModel();
		// BufferedImage out = convolver.createCompatibleDestImage(bim, model);
		//
		// // Do the convolution
		// out = convolver.filter(bim, out);
		//
		// // Transform the buffered image into imageplus and return
		// convolvedImage = new ImagePlus("",out);
		// imp = (ByteProcessor)
		// convolvedImage.getProcessor().convertToByte(true);
		
		convolvedImage = convolveImage(imageWithoutImmobile);
		imp = (ByteProcessor) convolvedImage.getProcessor();
	}
	
	/**
	 * Return a convolved image
	 * 
	 * @param imageToConvolve
	 * @return
	 */
	private ImagePlus convolveImage(ImagePlus imageToConvolve)
	{
		Logs.log("Convolving image", 1, this);
		
		// make cell image
		cellImage = makeCellOfRadius(4 * psize, 4 * psize, psize);
		
		// Make bufferedimage source
		BufferedImage bim = ImageUtility.imagePlus2BufferedImage(imageToConvolve);
		
		// make the kernel
		FloatProcessor convFloat = (FloatProcessor) cellImage.getProcessor().convertToFloat();
		float[][] convMat = convFloat.getFloatArray();
		Kernel kernel = SimpleConvolve.makeConvolutionKernel(convMat, cellImage, imageToConvolve);
		
		// Make the convolution operator
		ConvolveOp convolver = new ConvolveOp(kernel);
		
		// Make the destination image
		ColorModel model = imageToConvolve.getProcessor().getColorModel();
		BufferedImage out = convolver.createCompatibleDestImage(bim, model);
		
		// Do the convolution
		out = convolver.filter(bim, out);
		
		// Transform the buffered image into imageplus and return
		ImagePlus convolvedImage = new ImagePlus("", out);
		ImageProcessor imp = (ByteProcessor) convolvedImage.getProcessor().convertToByte(true);
		ImagePlus result = new ImagePlus("", imp.duplicate());
		
		return result;
	}
	
	/**
	 * Locate local maxima
	 */
	private void locateSeedParticles()
	{
		// ImagePlus im = jimages.get(index).getImagePlus();
		ImagePlus im = new ImagePlus(jimages.get(index));
		seedParticles = locateSeedParticles(im, index, new PointList());
		Logs.log("Finding particles", 1, this);
		
		// Logs.log("Finding particles",1,this);
		// FindMaxima finder = new FindMaxima();
		// finder.threshold = (int)threshold;
		// finder.cellNB = -1;
		// finder.fixedCellNb = false;
		// finder.cellRadius = exclusionRad;
		// PointList particles = finder.findMaximum(convolvedImage, new
		// Rectangle(0,0,convolvedImage.getWidth(),convolvedImage.getHeight()));
		//
		// // Remove seed particles that are outside of the inner ROI
		// // or that are close to an existing located particle
		// seedParticles = new PointList();
		// for (Point p: particles){
		// // If p is outside of the inner ROI then remove it as a starting
		// point for a track
		// int innerRadius = (int) (radiusForFindingCells * radius);
		// boolean pointOutside = (p.getX() < innerRadius);
		// pointOutside = pointOutside || (p.getY() < innerRadius);
		// pointOutside = pointOutside || (p.getX() > convolvedImage.getWidth()
		// - innerRadius);
		// pointOutside = pointOutside || (p.getY() >
		// convolvedImage.getHeight()- innerRadius);
		// if (pointOutside) continue;
		//
		// // If p is close to an existing point in that frame then remove it
		//
		//
		// // Add p as a starting point
		// seedParticles.add(p);
		// }
	}
	
	/**
	 * Locate local maxima in an image
	 */
	private PointList locateSeedParticles(ImagePlus image, int index, PointList existingPoints)
	{
		// Bin the image
		image = this.binImage(image);
		
		// remove imobile features
		image = this.removeImobile(image, index);
		
		// Convolve the image
		image = this.convolveImage(image);
		
		Logs.log("Finding particles", 1, this);
		PointList result = new PointList();
		
		FindMaxima finder = new FindMaxima();
		finder.threshold = (int) threshold;
		finder.cellNB = -1;
		finder.fixedCellNb = false;
		finder.cellRadius = exclusionRad;
		PointList particles = finder.findMaximum(image, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
		
		// Remove seed particles that are outside of the inner ROI
		// or that are close to an existing located particle
		for (Point p : particles)
		{
			// If p is outside of the inner ROI then remove it as a starting
			// point for a track
			int innerRadius = (int) (radiusForFindingCells * radius);
			boolean pointOutside = (p.getX() < innerRadius);
			pointOutside = pointOutside || (p.getY() < innerRadius);
			pointOutside = pointOutside || (p.getX() > image.getWidth() - innerRadius);
			pointOutside = pointOutside || (p.getY() > image.getHeight() - innerRadius);
			if(pointOutside)
				continue;
			
			// If p is close to an existing point in that frame then remove it
			if(isPointCloseToPointInList(radius, p, existingPoints))
				continue;
			
			// Add p as a starting point
			result.add(p);
		}
		
		return result;
	}
	
	/**
	 * Return the distance between 2 points
	 * 
	 * @param p1
	 * @param p2
	 * @return distance
	 */
	private double distanceBetweenTwoPoints(Point p1, Point p2)
	{
		double result = Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
		return result;
	}
	
	/**
	 * Return true if point P is close by a radius of RADIUS to a point in pointlist LIST
	 * 
	 * @param radius
	 * @param p
	 * @param list
	 * @return boolean
	 */
	private boolean isPointCloseToPointInList(int radius, Point p, PointList list)
	{
		for (Point pp : list)
		{
			if(distanceBetweenTwoPoints(p, pp) < radius)
				return true;
		}
		return false;
	}
	
	/**
	 * Make all the tracks from each starting point
	 */
	private void trackNeutrophilsFromStartingPointList()
	{
		// Make the list of images
		impmages = new ArrayList<ImagePlus>(0);
		for (String imPath : jimages)
		{
			// Get the imageplus
			ImagePlus image1 = new ImagePlus(imPath);// jimage.getImagePlus();
			image1 = this.binImage(image1);
			
			// Remove the background if required
			if(rollBall > 0)
			{
				// Background subtracter
				BackgroundSubtracter bgs = new BackgroundSubtracter();
				ShortProcessor shortImage = (ShortProcessor) image1.getProcessor().convertToShort(true);
				
				// Background subtracter parameters
				double radius1 = rollBall;
				boolean createBackground = false;
				boolean lightBackground = false;
				boolean useParaboloid = false;
				boolean doPresmooth = false;
				boolean correctCorners = false;
				
				// Perform background subtraction for both Hoechst and p65
				// images
				bgs.rollingBallBackground(shortImage, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
				Logs.log("Rolling ball performed", 1, this);
				
				// Convert ShortProcessor to ByteProcessor for watershedding
				ByteProcessor byteImPlus1 = (ByteProcessor) shortImage.convertToByte(true);
				image1 = new ImagePlus("", byteImPlus1);
			}
			
			// Add to image stack list
			impmages.add(image1);
		}
		
		// for each starting image
		List<Trajectory> tracks = new ArrayList<Trajectory>();
		int len = (startTimes == -1) ? 1 : (impmages.size() - 1) / startTimes;
		int tick = (int) (100 * ((double) 1 / (double) (len - 1)));
		for (int index = 0; index < len; index++)
		{
			// Get the image at index INDEX
			int imageIndex = (startTimes == -1) ? 0 : index * startTimes;
			// ImagePlus image = jimages.get(imageIndex).getImagePlus();
			ImagePlus image = new ImagePlus(jimages.get(imageIndex));
			
			// Get the existing points at index INDEX
			PointList existingPoints = new PointList();
			for (Trajectory t : tracks)
			{
				Point p = t.getPoint(imageIndex);
				if(p != null)
					existingPoints.add(p);
			}
			
			// Get the seed particles for index INDEX
			PointList seeds = locateSeedParticles(image, imageIndex, existingPoints);
			
			// Get the tracks that start at the seeding points
			int seedDone = 1;
			for (Point p : seeds)
			{
				Trajectory t = trackNeutrophilFromStartingPoint(p, imageIndex);
				tracks.add(t);
				Logs.log("Added track to list.", 1, this);
				
				int percentage = (int) (100 * ((double) index / (double) (len - 1))) + tick * (int) ((double) seedDone / (double) seeds.size());
				JEXStatics.statusBar.setProgressPercentage(percentage);
				seedDone++;
			}
			
			// Update status bar
			Logs.log("Found cell in image " + index + " of " + len + ".", 1, this);
			int percentage = (int) (100 * ((double) (index + 1) / (double) (len - 1)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// make the trajectory array
		trajs = tracks.toArray(new Trajectory[0]);
	}
	
	/**
	 * Track the current neutrophil
	 */
	private Trajectory trackNeutrophilFromStartingPoint(Point startingPoint, int frame)
	{
		// Create the future trajectory
		Trajectory result = new Trajectory();
		trail = new TreeMap<Integer,Point>();
		trail.put(frame, startingPoint);
		List<BufferedImage> images = new ArrayList<BufferedImage>(0);
		List<BufferedImage> cellimages = new ArrayList<BufferedImage>(0);
		
		// Setup the gliding variables
		ImagePlus image1 = impmages.get(frame);
		ImagePlus currentCell = extractImageAroundPoint(image1, startingPoint, radius);
		Point maxPoint = startingPoint;
		
		for (int i = frame + 1; i < jimages.size(); i++)
		{
			// Get the next image
			ImagePlus image = impmages.get(i);
			
			// make the movie frame
			BufferedImage bim = makeMovieFrame(image, maxPoint, trail, i, radius, movement, bin);
			images.add(bim);
			
			// Add the cell image to the list
			ImageProcessor cimp = currentCell.getProcessor();
			int type = BufferedImage.TYPE_INT_RGB;
			BufferedImage bimage = new BufferedImage(cimp.getWidth(), cimp.getHeight(), type);
			Graphics g = bimage.createGraphics();
			g.drawImage(cimp.getBufferedImage(), 0, 0, null);
			g.dispose();
			cellimages.add(bimage);
			
			// Convolve the cell in the search area of the next image
			Vect maxDisp = convolveNeutrophilAroundPointInImage(image, currentCell, maxPoint, movement);
			
			// Find the maximum displacement
			maxPoint = new Point(maxPoint.x + (int) maxDisp.dX, maxPoint.y + (int) maxDisp.dY);
			
			// Add to trail
			trail.put(i, maxPoint);
			
			// Extract next cell image to convolve
			currentCell = extractImageAroundPoint(image, maxPoint, radius);
			
			// If the track left the outter ROI then stop the track
			int outterRadius = (int) (radiusForRemovingTrack * radius);
			boolean pointOutside = (maxPoint.getX() < outterRadius);
			pointOutside = pointOutside || (maxPoint.getY() < outterRadius);
			pointOutside = pointOutside || (maxPoint.getX() > image.getWidth() - outterRadius);
			pointOutside = pointOutside || (maxPoint.getY() > image.getHeight() - outterRadius);
			if(pointOutside)
				break;
		}
		
		// create currentList
		for (Integer key : trail.keySet())
		{
			result.add(trail.get(key), key);
		}
		
		// make the movie
		// makeTempMovie(images);
		// makeTempMovie(cellimages);
		
		return result;
	}
	
	/**
	 * Return the convolution score matrix of image CELLIMAGE in image IMAGE, around point P in a radius AREARADIUS
	 * 
	 * @param image
	 * @param cellImage
	 * @param p
	 * @param areaRadius
	 * @return matrix
	 */
	private Vect convolveNeutrophilAroundPointInImage(ImagePlus image, ImagePlus cellImage, Point p, int areaRadius)
	{
		// Setup the variables
		int maxi = 0;
		int maxj = 0;
		float currentmax = 0;
		ImageProcessor iimp = image.getProcessor();
		ImageProcessor cimp = cellImage.getProcessor();
		
		// Convolve around the area of reasearch
		for (int i = -areaRadius; i <= areaRadius; i++)
		{
			for (int j = -areaRadius; j <= areaRadius; j++)
			{
				
				// Get the score of the convolution
				Point pp = new Point(p.x + i - cellImage.getWidth() / 2, p.y + j - cellImage.getHeight() / 2);
				float score = scoreConvolution(iimp, cimp, pp);
				
				// Check if it is larger than the previous max
				// if it is, update the new max
				if(score > currentmax)
				{
					maxi = i;
					maxj = j;
					currentmax = score;
				}
			}
		}
		
		// return the matrix
		return new Vect(maxi, maxj);
	}
	
	/**
	 * Return the score of the convolution of image CELLIMAGE in image IMAGE at location P
	 * 
	 * @param image
	 * @param cellImage
	 * @param p
	 * @return
	 */
	private float scoreConvolution(ImageProcessor image, ImageProcessor cellImage, Point p)
	{
		// setup result
		float result = 0;
		
		// dimensions
		int w = cellImage.getWidth();
		int h = cellImage.getHeight();
		int maxW = image.getWidth();
		int maxH = image.getHeight();
		
		// normalization area
		float area = cellImage.getWidth() * cellImage.getHeight();
		
		// Do the convolution
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				int currentX = p.x + i;
				int currentY = p.y + j;
				
				// Find the multiplication of the pixels...
				// if outside of image return 0
				float f = 0;
				if(currentX > 0 && currentX < maxW && currentY > 0 && currentY < maxH)
					f = (float) image.getPixel(currentX, currentY) * cellImage.getPixel(i, j);
				else
					f = 0;
				
				// add pixel multiplcation to score
				result = result + f / area;
			}
		}
		
		return result;
	}
	
	/**
	 * Extract an image or radius CELLRADIUS around point P in image IMAGE
	 * 
	 * @param image
	 * @param p
	 * @param cellRadius
	 * @return
	 */
	private ImagePlus extractImageAroundPoint(ImagePlus image, Point p, int cellRadius)
	{
		ImageProcessor resultIMP = image.getProcessor().duplicate();
		
		// set the roi in the big image
		resultIMP.setRoi(new Rectangle(p.x - cellRadius, p.y - cellRadius, 2 * cellRadius + 1, 2 * cellRadius + 1));
		
		// crop the big image
		resultIMP = resultIMP.crop();
		
		return new ImagePlus("", resultIMP);
	}
	
	/**
	 * Make the frame of a movie
	 * 
	 * @param image
	 * @param p
	 * @param trail
	 * @param frameNB
	 * @param cellRadius
	 * @param areaRadius
	 * @param binning
	 * @return
	 */
	private BufferedImage makeMovieFrame(ImagePlus image, Point p, TreeMap<Integer,Point> trail, int frameNB, int cellRadius, int areaRadius, int binning)
	{
		ImageProcessor imp = image.getProcessor();
		// imp = imp.resize((int)(image.getWidth()/binning));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// Paint the current point
		g.setColor(Color.yellow);
		g.drawOval(p.x - 1, p.y - 1, 3, 3);
		
		// Paint the cell radius (ie the cell that is extraced for convolution)
		g.setColor(Color.red);
		g.drawRect(p.x - cellRadius, p.y - cellRadius, 2 * cellRadius + 1, 2 * cellRadius + 1);
		
		// Paint the search radius
		g.setColor(Color.green);
		g.drawRect(p.x - areaRadius - cellRadius, p.y - areaRadius - cellRadius, 2 * areaRadius + 2 * cellRadius + 1, 2 * areaRadius + 2 * cellRadius + 1);
		
		// Paint the track history
		Point lastP = null;
		for (Integer key : trail.keySet())
		{
			// Get the point
			Point newP = trail.get(key);
			
			// draw the point
			int shade = 255 * (key) / (frameNB);
			Color c = new Color(shade, shade, 255);
			g.setColor(c);
			g.drawRect(newP.x, newP.y, 2, 2);
			
			// draw the line
			if(lastP != null)
			{
				g.drawLine(lastP.x, lastP.y, newP.x, newP.y);
			}
			
			// set last point
			lastP = newP;
		}
		
		g.dispose();
		
		return bimage;
	}
	
	/**
	 * Select only certain tracks
	 */
	private void selectTracks()
	{
		List<Trajectory> selectedList = new ArrayList<Trajectory>();
		
		for (int i = 0, len = trajs.length; i < len; i++)
		{
			Trajectory t = trajs[i];
			TrackStatistics stats = new TrackStatistics();
			stats.startAnalysis(t);
			if(t.nbPoints() > minLength && stats.meanVelocity > minVel)
			{
				selectedList.add(t);
			}
		}
		selected = selectedList.toArray(new Trajectory[0]);
		
		output1 = TrackWriter.makeTracksObject(outputNames[0].getName(), selectedList);
	}
	
	/**
	 * Analyze the migration
	 */
	private void analyze()
	{
		TrackStatistics stats = new TrackStatistics(selected);
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
		output2.put(JEXEntry.INFO, "Created from POCT analysis v3");
		
		// XDisplacement
		output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + xdisp);
		output3.put(JEXEntry.INFO, "Created from POCT analysis v3");
		
		// YDisplacement
		output4 = ValueWriter.makeValueObject(outputNames[3].getName(), "" + ydisp);
		output4.put(JEXEntry.INFO, "Created from POCT analysis v3");
		
		// Chemotactic index
		output5 = ValueWriter.makeValueObject(outputNames[4].getName(), "" + chemoIndex);
		output5.put(JEXEntry.INFO, "Created from POCT analysis v3");
		
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
		output6.put(JEXEntry.INFO, "Created from POCT analysis v3");
		
		// // Table sheet
		// int length = (selected == null)? 0 : selected.length;
		// List<String> colNamesList = new ArrayList<String>(0);
		// String[] rowNamesList = new String[length];
		// for (int index=0; index<length; index++) rowNames[index] =
		// "Cell "+index;
		// HashMap<String,String[]> columnsList = new
		// HashMap<String,String[]>();
		//
		// for (int i=0, len=length; i<len; i++){
		// Trajectory traj = selected[i];
		// TrajectoryStatistics trajStat = new TrajectoryStatistics(traj);
		//
		// int j = 0;
		// String colName = "Trajectory "+i;
		// String[] colValues = null;
		// List<String> colValuesTemp = new ArrayList<String>(0);
		// while (j<traj.length()){
		// trajStat.startAnalysis(j, j+interval);
		// double velocity = trajStat.meanVelocity * 60 * bin / (ppm * spf);
		//
		// // String timeLabel = StringUtility.fillLeft(""+j, 4, "0");
		// // colName = "Time "+timeLabel;
		//
		// colValuesTemp.add(""+velocity);
		// j = j + interval;
		// }
		//
		// colValues = colValuesTemp.toArray(new String[0]);
		// columnsList.put(colName, colValues);
		// colNamesList.add(colName);
		// }
		// String[] columnArrayNames = colNamesList.toArray(new String[0]);
		//
		// // output7 = DataFactory.makeValueTable(outputNames[6],
		// columnArrayNames, rowNamesList, columnsList);
		// output7 = ValueWriter.makeValueTable(outputNames[6],
		// columnArrayNames, columnsList);
		// output7.put(JEXEntry.INFO, "Created from POCT analysis v3");
		
	}
	
	/**
	 * Make the movie of the tracking
	 */
	private void makeMovie()
	{
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		Logs.log("Saving the movie to path " + path, 1, this);
		
		TracksMovieMaker movieMaker = new TracksMovieMaker(path, jimages, selected);
		movieMaker.setImageBinning(bin);
		movieMaker.makeMovie();
		
		// Make the JEXTrack object
		output7 = MovieWriter.makeMovieObject(outputNames[6].getName(), path);
	}
	
	private void makeFirstFrame()
	{
		// ------------------------------
		// Get the first image in the stack
		ImagePlus implus = new ImagePlus(jimages.get(0));// jimages.get(0).getImagePlus();
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((int) (implus.getWidth() / this.bin));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		int length = (selected == null) ? 0 : selected.length;
		for (int k = 0; k < length; k++)
		{
			
			Trajectory trajK = selected[k];
			Point firstPoint = trajK.getPoint(0);
			
			if(firstPoint != null)
			{
				g.setColor(Color.YELLOW);
				g.fillOval(firstPoint.x, firstPoint.y, 3, 3);
				
				g.setColor(Color.red);
				g.drawRect(firstPoint.x - radius, firstPoint.y - radius, radius * 2 + 1, radius * 2 + 1);
				
				g.setColor(Color.GREEN);
				g.drawRect(firstPoint.x - radius - movement, firstPoint.y - radius - movement, movement * 2 + radius * 2 + 1, movement * 2 + radius * 2 + 1);
			}
		}
		
		g.dispose();
		
		// save the image
		JEXWriter.saveFigure(bimage, "jpg");
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{
		if(interactionMode)
		{
			radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
			second = p;
			// int roiX = Math.max(second.x - radius, 0);
			// int roiY = Math.max(second.y - radius, 0);
			// int roiW = 2*radius+1;
			// int roiH = 2*radius+1;
			// roi = new Roi(roiX,roiY,roiW,roiH);
			// imagepanel.setRoi(roi);
		}
	}
	
	public void mouseMoved(Point p)
	{
		if(interactionMode)
		{
			radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
			second = p;
			// int roiX = Math.max(second.x - radius, 0);
			// int roiY = Math.max(second.y - radius, 0);
			// int roiW = 2*radius+1;
			// int roiH = 2*radius+1;
			// roi = new Roi(roiX,roiY,roiW,roiH);
			// imagepanel.setRoi(roi);
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
