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
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ByteProcessor;
import ij.process.ImageStatistics;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;

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
public class JEX_ImageTools_MeasureIntensity extends JEXCrunchable {
	
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
		String result = "Measure intensity";
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
		String result = "Select a region of an image and measure the mean intensity";
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
		String toolbox = "Image tools";
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
		
		inputNames[0] = new TypeName(IMAGE, "Image");
		
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
		this.defaultOutputNames = new TypeName[2];
		this.defaultOutputNames[0] = new TypeName(VALUE, "Treated Intensity");
		this.defaultOutputNames[1] = new TypeName(VALUE, "Untreated Intensity");
		
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
		Parameter p0 = new Parameter("Radius", "Radius of the rolling ball", "50");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		
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
		{
			return false;
		}
		
		// Run the function
		RegionMeasureHelper graphFunc = new RegionMeasureHelper(data1, entry, this.outputNames, this.parameters);
		graphFunc.doit();
		
		// Set the outputs
		this.realOutputs.add(graphFunc.output1);
		this.realOutputs.add(graphFunc.output2);
		
		// Return status
		return true;
	}
}

class RegionMeasureHelper implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	
	// Parameters
	ParameterSet params;
	int radius = 50;
	
	// Variables used during the function steps
	private ByteProcessor imp;
	private ImagePlus im;
	private Roi region1 = null;
	private Roi region2 = null;
	
	// Input
	JEXData imset;
	JEXEntry entry;
	Rectangle rectangle1;
	Rectangle rectangle2;
	TypeName[] outputNames;
	List<String> jimages;
	
	RegionMeasureHelper(JEXData imset, JEXEntry entry, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.imset = imset;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		this.getParams();
		
		// Prepare function
		TreeMap<DimensionMap,String> jimagesMap = ImageReader.readObjectToImagePathTable(imset);
		this.jimages.addAll(jimagesMap.values());
		// jimages = new ArrayList<String>();
		// for (DimensionMap map: imset.getDataMap().keySet()){
		// JEXDataSingle ds = imset.getDataMap().get(map);
		// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
		// ds.get(JEXDataSingle.FILENAME);
		// jimages.add(path);
		// }
		
		// Prepare the graphics
		this.imagepanel = new ImagePanel(this, "Measure ROIs");
		this.imagepanel.setRoi(null);
		this.displayImage(this.index);
		this.wrap = new GraphicalFunctionWrap(this, this.params);
		this.wrap.addStep(0, "Clean Image", new String[] { "Radius" });
		this.wrap.addStep(1, "Select Region 1", new String[0]);
		this.wrap.addStep(2, "Select Region 2", new String[0]);
		this.wrap.addStep(3, "Measure", new String[0]);
		this.wrap.setInCentralPanel(this.imagepanel);
		this.wrap.setDisplayLoopPanel(true);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{// //// Get params
		this.radius = (int) Double.parseDouble(this.params.getValueOfParameter("Radius"));
	}
	
	private void displayImage(int index)
	{
		ImagePlus im = new ImagePlus(this.jimages.get(index));
		this.imagepanel.setImage(im);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		this.wrap.start();
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
		if(this.atStep == 0)
		{
			this.im = new ImagePlus(this.jimages.get(this.index));
			this.imp = (ByteProcessor) this.im.getProcessor().convertToByte(true);
			this.imagepanel.setPointListArray(null, null);
			this.imagepanel.setRoi(null);
			
			this.cleanImage();
			this.interactionMode = true;
		}
		else if(this.atStep == 1)
		{
			this.im = new ImagePlus(this.jimages.get(this.index));
			this.imp = (ByteProcessor) this.im.getProcessor().convertToByte(true);
			this.imagepanel.setPointListArray(null, null);
			this.imagepanel.setRoi(null);
			
			this.cleanImage();
			this.makeRectangle1();
			this.interactionMode = true;
		}
		else if(this.atStep == 2)
		{
			this.im = new ImagePlus(this.jimages.get(this.index));
			this.imp = (ByteProcessor) this.im.getProcessor().convertToByte(true);
			this.imagepanel.setPointListArray(null, null);
			this.imagepanel.setRoi(this.region1);
			
			this.cleanImage();
			this.makeRectangle2();
			this.interactionMode = true;
		}
		else if(this.atStep == 3)
		{
			this.im = new ImagePlus(this.jimages.get(this.index));
			this.imp = (ByteProcessor) this.im.getProcessor().convertToByte(true);
			this.imagepanel.setPointListArray(null, null);
			this.imagepanel.setRoi(this.region2);
			
			this.cleanImage();
			this.measure();
		}
		
		this.imagepanel.setImage(new ImagePlus("", this.imp));
	}
	
	@Override
	public void runNext()
	{
		this.atStep = this.atStep + 1;
		if(this.atStep > 3)
		{
			this.atStep = 3;
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
		this.index = this.index + 1;
		
		if(this.index >= this.jimages.size() - 1)
		{
			this.index = this.jimages.size() - 1;
		}
		if(this.index < 0)
		{
			this.index = 0;
		}
		
		this.runStep(this.atStep);
	}
	
	@Override
	public void loopPrevious()
	{
		this.index = this.index - 1;
		
		if(this.index >= this.jimages.size() - 1)
		{
			this.index = this.jimages.size() - 1;
		}
		if(this.index < 0)
		{
			this.index = 0;
		}
		
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
	{}
	
	private void cleanImage()
	{
		// Background subtracter
		BackgroundSubtracter bgs = new BackgroundSubtracter();
		
		// Background subtracter parameters
		double radius1 = this.radius;
		boolean createBackground = true;
		boolean lightBackground = false;
		boolean useParaboloid = false;
		boolean doPresmooth = false;
		boolean correctCorners = false;
		
		// Perform background subtraction for both Hoechst and p65 images
		bgs.rollingBallBackground(this.imp, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		Logs.log("Clean image performed", 1, this);
	}
	
	private void makeRectangle1()
	{   
		
	}
	
	private void makeRectangle2()
	{   
		
	}
	
	private void measure()
	{
		// Measure in rectangle 1
		this.imp.setRoi(this.region1);
		ImageStatistics stats = this.imp.getStatistics();
		double mean1 = stats.mean;
		
		// Measure in rectangle 2
		this.imp.setRoi(this.region2);
		ImageStatistics stats2 = this.imp.getStatistics();
		double mean2 = stats2.mean;
		
		Logs.log("Values found are " + mean1 + " and " + mean2, 1, this);
		
		// Create the first output
		this.output1 = ValueWriter.makeValueObject(this.outputNames[0].getName(), "" + mean1);
		this.output2 = ValueWriter.makeValueObject(this.outputNames[1].getName(), "" + mean2);
	}
	
	@Override
	public void clickedPoint(Point p)
	{}
	
	@Override
	public void pressedPoint(Point p)
	{
		if(this.interactionMode)
		{
			this.first = p;
			this.second = null;
			this.imagepanel.setRoi(null);
			if(this.atStep == 1)
			{
				this.rectangle1 = null;
			}
			else if(this.atStep == 2)
			{
				this.rectangle2 = null;
			}
		}
	}
	
	@Override
	public void mouseMoved(Point p)
	{
		if(this.interactionMode)
		{
			this.second = p;
			
			if(this.atStep == 1)
			{
				int roiW = Math.abs(this.second.x - this.first.x);
				int roiH = Math.abs(this.second.y - this.first.y);
				int roiX = Math.min(this.second.x, this.first.x);
				int roiY = Math.min(this.second.y, this.first.y);
				this.rectangle1 = new Rectangle(roiX, roiY, roiW, roiH);
				this.region1 = new Roi(roiX, roiY, roiW, roiH);
				this.imagepanel.setRoi(this.region1);
			}
			else if(this.atStep == 2)
			{
				int roiW = Math.abs(this.second.x - this.first.x);
				int roiH = Math.abs(this.second.y - this.first.y);
				int roiX = Math.min(this.second.x, this.first.x);
				int roiY = Math.min(this.second.y, this.first.y);
				this.rectangle2 = new Rectangle(roiX, roiY, roiW, roiH);
				this.region2 = new Roi(roiX, roiY, roiW, roiH);
				this.imagepanel.setRoi(this.region2);
			}
		}
	}
	
}
