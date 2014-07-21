package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import function.imageUtility.jBackgroundSubtracter;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;

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
public class JEX_TASSO_QuantifyBlood extends JEXCrunchable {
	
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
		String result = "Quantify Blood volume";
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
		String result = "Quantify blood spatter";
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
		String toolbox = "TASSO plugins";
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
		
		inputNames[0] = new TypeName(IMAGE, "Blood spatter image");
		
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
		defaultOutputNames[0] = new TypeName(VALUE, "Spatter area (pixels)");
		defaultOutputNames[1] = new TypeName(VALUE, "Spatter area (normalized)");
		
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
		Parameter p1 = new Parameter("Background radius", "Radius of the rolling ball for background substract, -1 for no filtering", "100");
		
		Parameter p2 = new Parameter("Min-threshold", "Minimum threshold in case auto-threshold goes too low", "6.0");
		Parameter p3 = new Parameter("Auto-thresholding", "Algorithm for auto-thresholding", Parameter.DROPDOWN, new String[] { "Minimum", "MaxEntropy", "Otsu", "Moments", "Li", "Default", "Mean", "Huang", "Triangle", "MinError(I)", "Percentile" }, 0);
		Parameter p4 = new Parameter("Threshold", "Overide auto-thresold if value is greater than 1", "-1");
		Parameter p5 = new Parameter("uL per pixel", "microliters of blood per pixel", "0.01");
		
		Parameter p6 = new Parameter("Min cluster size", "Minimum cluster size", "100");
		Parameter p7 = new Parameter("Max cluster size", "Maximum cluster size", "100000");
		Parameter p9 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p9);
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
		JEXData data1 = inputs.get("Blood spatter image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		BloosSpatterHelperFunction graphFunc = new BloosSpatterHelperFunction(entry, data1, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.output1;
		JEXData output2 = graphFunc.output2;
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
}

class BloosSpatterHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	ByteProcessor imp;
	ImagePlus im;
	int nb;
	ParticleAnalyzer pA;
	
	// Outputs
	public float[] areas = new float[0];
	public float areaInPixels = 0;
	public double areaInUL = 0;
	public JEXData output1;
	public JEXData output2;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	double radius = 10;
	double minSize = 10;
	double maxSize = 100;
	double threshold = 10;
	double minthreshold = 10;
	double uLPerPixel = 0.01;
	String threshMethod = "Default";
	
	// Input
	JEXData imset;
	JEXEntry entry;
	List<String> images;
	TypeName[] outputNames;
	
	BloosSpatterHelperFunction(JEXEntry entry, JEXData imset, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// Get params
		getParameters();
		
		// Prepare function
		this.images = ImageReader.readObjectToImagePathList(imset);
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Quantify Blood spatter area");
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Subtract background", new String[] { "Automatic", "Background radius" });
		wrap.addStep(1, "Threshold", new String[] { "Min-threshold", "Auto-thresholding", "Threshold" });
		wrap.addStep(2, "Extract spatters", new String[] { "Min cluster size", "Max cluster size", "uL per pixel" });
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	public void getParameters()
	{
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		radius = Double.parseDouble(params.getValueOfParameter("Background radius"));
		minSize = Double.parseDouble(params.getValueOfParameter("Min cluster size"));
		maxSize = Double.parseDouble(params.getValueOfParameter("Max cluster size"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		minthreshold = Double.parseDouble(params.getValueOfParameter("Min-threshold"));
		uLPerPixel = Double.parseDouble(params.getValueOfParameter("uL per pixel"));
		threshMethod = params.getValueOfParameter("Auto-thresholding");
	}
	
	private void displayImage(int index)
	{
		ImagePlus im = new ImagePlus(images.get(index));
		Logs.log("Placing in imagepanel " + images.get(index), 1, this);
		
		imagepanel.setImage(im);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		if(auto)
		{
			this.finishIT();
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
		
		// Get params
		getParameters();
		
		// Get the image
		im = new ImagePlus(images.get(index));
		imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		if(atStep == 0)
		{
			subtractBackground();
			imagepanel.setRoi(null);
		}
		else if(atStep == 1)
		{
			subtractBackground();
			threshold();
		}
		else if(atStep == 2)
		{
			subtractBackground();
			threshold();
			analyzeParticles();
		}
		
		// atStep = atStep+1;
		imagepanel.setImage(new ImagePlus("", imp));
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
		index = index + 1;
		
		if(index >= images.size() - 1)
			index = images.size() - 1;
		if(index < 0)
			index = 0;
		
		runStep(index);
	}
	
	public void loopPrevious()
	{
		index = index - 1;
		
		if(index >= images.size() - 1)
			index = images.size() - 1;
		if(index < 0)
			index = 0;
		
		runStep(index);
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
		// Get params
		getParameters();
		
		// Run the function
		ImagePlus im = ImageReader.readObjectToImagePlus(imset);
		imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		
		// //// Begin Actual Function
		subtractBackground();
		threshold();
		analyzeParticles();
		
		// Collect the output values
		
		areaInPixels = 0;
		areaInUL = 0;
		for (float f : areas)
		{
			areaInPixels = f + areaInPixels;
		}
		areaInUL = uLPerPixel * areaInPixels;
		
		output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + areaInPixels);
		output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + areaInUL);
	}
	
	private void subtractBackground()
	{
		if(radius < 0)
		{
			// Convert to Byte
			ByteProcessor bimp = (ByteProcessor) imp.convertToByte(true);
			
			// Prepare background subtract
			jBackgroundSubtracter bS = new jBackgroundSubtracter();
			jBackgroundSubtracter.radius = -radius;
			jBackgroundSubtracter.lightBackground = false;
			jBackgroundSubtracter.createBackground = false;
			jBackgroundSubtracter.useParaboloid = false;
			jBackgroundSubtracter.doPresmooth = false;
			
			// Run the background subtract
			bS.setup("", im);
			bS.run(bimp);
			
			// Place the byte processor in the image to track
			imp = bimp;
		}
		else
		{
			jBackgroundSubtracter bS = new jBackgroundSubtracter();
			bS.setup("", im);
			jBackgroundSubtracter.radius = radius; // default rolling ball
			// radius
			jBackgroundSubtracter.lightBackground = true;
			jBackgroundSubtracter.createBackground = false;
			jBackgroundSubtracter.useParaboloid = false; // use
			// "Sliding Paraboloid"
			// instead of rolling
			// ball algorithm
			jBackgroundSubtracter.doPresmooth = false;
			bS.run(imp);
			
			// JEXWriter.saveImage(imp);
		}
	}
	
	private void threshold()
	{
		// Invert the image to get a white cell over a dark background
		imp.invert();
		
		// Get the thresholing method
		// If the thresold value is more than 1 then use the manually entered
		// value
		if(threshold < 0)
		{
			Logs.log("Thresholding using " + threshMethod + " algorithm", 1, this);
			int[] histogram = imp.getHistogram();
			function.imageUtility.AutoThresholder thresholder = new function.imageUtility.AutoThresholder();
			threshold = thresholder.getThreshold(threshMethod, histogram);
			threshold = (threshold < minthreshold) ? minthreshold : threshold;
		}
		else
		{
			Logs.log("Thresholding using manually entered value " + threshold, 1, this);
		}
		
		// Do the threshold
		imp.threshold((int) threshold);
		
		// Invert the LUT for display and EDM analysis
		imp.invertLut();
		// JEXWriter.saveImage(imp);
	}
	
	private void analyzeParticles()
	{
		ResultsTable rT = new ResultsTable();
		int options = ParticleAnalyzer.SHOW_OUTLINES;
		int measure = Measurements.AREA | Measurements.CIRCULARITY | Measurements.PERIMETER | Measurements.CENTER_OF_MASS | Measurements.SKEWNESS;
		pA = new ParticleAnalyzer(options, measure, rT, minSize, maxSize);
		pA.setHideOutputImage(true);
		pA.analyze(new ImagePlus("", imp));
		
		if(rT.getColumn(0) == null)
		{
			nb = 0;
			areaInPixels = 0;
			areaInUL = 0;
			areas = new float[0];
			imp = new ByteProcessor(imp.getWidth(), imp.getHeight());
		}
		else
		{
			nb = rT.getColumn(0).length;
			int lastColumn = rT.getLastColumn();
			areaInPixels = 0;
			areaInUL = 0;
			areas = new float[0];
			for (int i = 0; i < lastColumn; i++)
			{
				String cName = rT.getColumnHeading(i);
				if(cName.equals("Area"))
				{
					areas = rT.getColumn(i);
				}
			}
			ImagePlus outIm = pA.getOutputImage();
			imp = (ByteProcessor) outIm.getProcessor().convertToByte(true);
		}
		// JEXWriter.saveImage(imp);
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
