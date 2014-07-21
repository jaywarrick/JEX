package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.RoiWriter;
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
import image.roi.ROIPlus;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
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
public class JEX_Migration_MakeROI extends JEXCrunchable {
	
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
		String result = "3. Make ROI";
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
		String result = "Define an ROI containing features of interest for further analysis.";
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
		String toolbox = "Migration";
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
		inputNames[0] = new TypeName(IMAGE, "Source Image");
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
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(ROI, "ROI");
		
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
		Parameter p1 = new Parameter("Automatic", "Use automated edge determination algorithms", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p2 = new Parameter("Width", "Width of the roi to create (if -1, ROI is custom)", "-1");
		Parameter p3 = new Parameter("Height", "Height of the roi to create (if -1, ROI is custom)", "-1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		JEXData data = inputs.get("Source Image");
		if(!data.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		MakeRoiHelperFunction graphFunc = new MakeRoiHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		
		// Set the outputs
		JEXData output1 = graphFunc.output;
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
}

class MakeRoiHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	List<String> images;
	List<DimensionMap> dimensions;
	int atStep = 0;
	boolean auto = true;
	Point first = null;
	Point second = null;
	int roiX = -1;
	int roiY = -1;
	int roiW = -1;
	int roiH = -1;
	Roi roi = null;
	int width = -1;
	int height = -1;
	
	ParameterSet params;
	TypeName[] outputNames;
	JEXData imset;
	JEXData output;
	
	MakeRoiHelperFunction(JEXEntry entry, JEXData imset, TypeName[] outputNames, ParameterSet params)
	{
		// Pass the variables
		this.imset = imset;
		this.params = params;
		this.outputNames = outputNames;
		
		// //// Get params
		String autoStr = params.getValueOfParameter("Automatic");
		String widthStr = params.getValueOfParameter("Width");
		String heightStr = params.getValueOfParameter("Height");
		auto = Boolean.parseBoolean(autoStr);
		width = Integer.parseInt(widthStr);
		height = Integer.parseInt(heightStr);
		
		// Prepare the graphics
		images = new ArrayList<String>(0);
		dimensions = new ArrayList<DimensionMap>(0);
		TreeMap<DimensionMap,String> ims = ImageReader.readObjectToImagePathTable(imset);
		for (DimensionMap map : ims.keySet())
		{
			dimensions.add(map);
			images.add(ims.get(map));
		}
		Logs.log("Loading with list of " + images.size() + " images", 1, this);
		
		ImagePlus im = new ImagePlus(images.get(0));
		Logs.log("Placing in imagepanel " + images.get(0), 1, this);
		imagepanel = new ImagePanel(this, "Select ROI");
		imagepanel.setImage(im);
		
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Select roi", new String[] { "Automatic", "Width", "Height" });
		wrap.setInCentralPanel(imagepanel);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		wrap.start();
	}
	
	public void runStep(int index)
	{
		String autoStr = params.getValueOfParameter("Automatic");
		String widthStr = params.getValueOfParameter("Width");
		String heightStr = params.getValueOfParameter("Height");
		auto = Boolean.parseBoolean(autoStr);
		width = Integer.parseInt(widthStr);
		height = Integer.parseInt(heightStr);
	}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return atStep;
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
		TreeMap<DimensionMap,ROIPlus> outputMap = new TreeMap<DimensionMap,ROIPlus>();
		for (int i = 0; i < images.size(); i++)
		{
			DimensionMap dim = dimensions.get(i);
			Rectangle rect = new Rectangle(roiX, roiY, roiW, roiH);
			ROIPlus roip = new ROIPlus(rect);
			outputMap.put(dim.copy(), roip);
			
			Logs.log("Finished processing " + atStep + " of " + images.size() + ".", 1, this);
			atStep++;
		}
		output = RoiWriter.makeRoiObject(outputNames[0].getName(), outputMap);
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
		if(width != -1 && height != -1)
		{
			first = p;
			roiX = first.x;
			roiY = first.y;
			roiW = width;
			roiH = height;
			roi = new Roi(roiX, roiY, roiW, roiH);
			imagepanel.setRoi(roi);
		}
		else
		{
			first = p;
			second = null;
			imagepanel.setRoi(null);
			roi = null;
		}
	}
	
	public void mouseMoved(Point p)
	{
		if(width != -1 && height != -1)
		{
			second = p;
			roiX = second.x;
			roiY = second.y;
			roiW = width;
			roiH = height;
			roi = new Roi(roiX, roiY, roiW, roiH);
			imagepanel.setRoi(roi);
		}
		else
		{
			second = p;
			
			roiW = Math.abs(second.x - first.x);
			roiH = Math.abs(second.y - first.y);
			roiX = Math.min(second.x, first.x);
			roiY = Math.min(second.y, first.y);
			roi = new Roi(roiX, roiY, roiW, roiH);
			imagepanel.setRoi(roi);
		}
	}
	
}
