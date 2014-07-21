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
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.HashMap;

import logs.Logs;

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
public class JEX_MeasureFluorescentIntensity extends JEXCrunchable {
	
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
		String result = "Measure fluorescent signal";
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
		String result = "Measures the fluorescent signal from bacteria of fungus";
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
		String toolbox = "Fungal Culture Analysis";
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
	 * @return name of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[4];
		defaultOutputNames[0] = new TypeName(VALUE, "Fluorescent intensity");
		defaultOutputNames[1] = new TypeName(VALUE, "Fluorescent area");
		defaultOutputNames[2] = new TypeName(VALUE, "Fluorescent stdev");
		defaultOutputNames[3] = new TypeName(IMAGE, "Mask image");
		
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
		Parameter p1 = new Parameter("Lower threshold", "Set a value for excluding pixels with an intensity lower than this", "50.0");
		Parameter p2 = new Parameter("Higher threshold", "Set a value for excluding pixels with an intensity higher than this", "4000.0");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
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
		FluorescentCounterHelperFunction graphFunc = new FluorescentCounterHelperFunction(data1, data2, entry, outputNames, parameters);
		graphFunc.doit();
		
		// Set the outputs
		realOutputs.add(graphFunc.meanIntensity);
		realOutputs.add(graphFunc.surfaceCoverage);
		realOutputs.add(graphFunc.stdev);
		realOutputs.add(graphFunc.maskImage);
		
		// Return status
		return true;
	}
	
}

class FluorescentCounterHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	int frame = 0;
	
	// Roi interaction
	boolean interactionMode = false;
	
	// Outputs
	public JEXData meanIntensity;
	public JEXData surfaceCoverage;
	public JEXData maskImage;
	public JEXData stdev;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int lThresh = 1;
	int hThresh = 4000;
	
	// Variables used during the function steps
	private ROIPlus imRoi;
	private ImagePlus cellIm;
	private ImagePlus maskIm;
	private ImagePlus maskImOverlay;
	double meanDens = 0;
	double surfCov = 0;
	double spread = 0;
	
	// Input
	JEXData imset;
	JEXData roidata;
	JEXEntry entry;
	TypeName[] outputNames;
	
	FluorescentCounterHelperFunction(JEXData imset, JEXData roidata, JEXEntry entry, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.imset = imset;
		this.roidata = roidata;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Prepare images
		cellIm = ImageReader.readObjectToImagePlus(imset);
		imRoi = RoiReader.readObjectToRoi(roidata);
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Make mask");
		imagepanel.setImage(cellIm);
		
		// displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Make mask", new String[] { "Lower threshold", "Higher threshold" });
		wrap.addStep(1, "Analyze", new String[] { "Automatic" });
		
		String title = "Analyzing entry " + entry.getEntryExperiment() + " - " + entry.getTrayX() + "." + entry.getTrayY();
		wrap.setTitle(title);
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
		lThresh = (int) Double.parseDouble(params.getValueOfParameter("Lower threshold"));
		hThresh = (int) Double.parseDouble(params.getValueOfParameter("Higher threshold"));
	}
	
	@SuppressWarnings("unused")
	private void displayImage(int index)
	{
		imagepanel.setImage(cellIm);
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
			makeMask();
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
		imagepanel.setPointListArray(null, null);
		imagepanel.setRoi(null);
		imagepanel.setTracks(null);
		
		if(atStep == 0)
		{
			makeMask();
			imagepanel.setImage(maskImOverlay);
		}
		else if(atStep == 1)
		{
			analyze();
			imagepanel.setImage(maskIm);
		}
		
		String title = "Analyzing entry " + entry.getEntryExperiment() + " - " + entry.getTrayX() + "." + entry.getTrayY();
		wrap.setTitle(title);
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
	private void makeMask()
	{
		// Make a mask for the lower threshold
		ImageProcessor lim = cellIm.getProcessor().duplicate().convertToByte(true);
		
		// Crop the image
		if(imRoi != null && imRoi.getRoi() != null)
		{
			lim.setRoi(imRoi.getRoi().getBounds());
			lim.crop();
		}
		
		// Threshold the image
		lim.threshold(lThresh);
		
		// Make a mask for the higher threshold
		ImageProcessor him = cellIm.getProcessor().duplicate().convertToByte(true);
		
		// Crop the image
		if(imRoi != null && imRoi.getRoi() != null)
		{
			him.setRoi(imRoi.getRoi().getBounds());
			him.crop();
		}
		
		// Threshold the image
		him.threshold(hThresh);
		
		// Get image dimensions
		int imWidth = him.getWidth();
		int imHeight = him.getHeight();
		
		// Make a mask intersecting the two masks
		ImageProcessor maskProc = lim.duplicate().convertToByte(true);
		maskProc.copyBits(him, 0, 0, Blitter.XOR);
		maskIm = new ImagePlus("", maskProc);
		
		// Make the overlay image
		ColorProcessor color1 = (ColorProcessor) cellIm.getProcessor().duplicate().convertToRGB();
		ColorProcessor color2 = new ColorProcessor(imWidth, imHeight);
		byte[] color2RPixels = (byte[]) maskProc.convertToByte(true).getPixels();
		byte[] color2GPixels = new byte[imWidth * imHeight];
		byte[] color2BPixels = new byte[imWidth * imHeight];
		color2.setRGB(color2RPixels, color2GPixels, color2BPixels);
		color1.copyBits(color2, 0, 0, Blitter.MAX);
		maskImOverlay = new ImagePlus("Cell merge", color1);
	}
	
	/**
	 * Analyze proliferation and fill the variables to output
	 */
	private void analyze()
	{
		// get the area
		ImageStatistics stats1 = maskIm.getStatistics();
		surfCov = stats1.mean * stats1.area / stats1.histMax;
		
		// Mask the image
		ImageProcessor maskedImage = cellIm.getProcessor().duplicate().convertToByte(true);
		maskedImage.copyBits(maskIm.getProcessor(), 0, 0, Blitter.MIN);
		maskIm = new ImagePlus("", maskedImage);
		
		// Get the statistics
		ImageStatistics stats = maskedImage.getStatistics();
		meanDens = stats.mean;
		spread = stats.stdDev;
		
		meanIntensity = ValueWriter.makeValueObject(outputNames[0].getName(), "" + meanDens);
		meanIntensity.setDataObjectInfo("Mean density of the fluorescent signal");
		
		surfaceCoverage = ValueWriter.makeValueObject(outputNames[1].getName(), "" + surfCov);
		surfaceCoverage.setDataObjectInfo("Surface coverage of the fluorescent signal");
		
		stdev = ValueWriter.makeValueObject(outputNames[2].getName(), "" + spread);
		stdev.setDataObjectInfo("Standard dev of the fluorescent signal");
		
		maskImage = ImageWriter.makeImageObject(outputNames[3].getName(), maskIm);
		maskImage.setDataObjectInfo("Refined image of Fibrinogen fibers");
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
