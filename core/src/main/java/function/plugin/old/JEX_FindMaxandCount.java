package function.plugin.old;

import ij.ImagePlus;
import ij.gui.Roi;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.FindMaxima;

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
public class JEX_FindMaxandCount extends JEXCrunchable {
	
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
		String result = "FIND AND COUNT MAXIMA";
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
		String result = "Determine the location of fluorescent/bright cells through their pixel value.";
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
		String toolbox = "Image processing";
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
		inputNames[0] = new TypeName(IMAGE, "Source Image");
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
		defaultOutputNames[0] = new TypeName(ROI, "Maxima");
		defaultOutputNames[1] = new TypeName(VALUE, "Point Count");
		
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
		Parameter p1 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p2 = new Parameter("Cell Radius", "Cell radius in pixels (e.g. 3 to 30)", "15");
		Parameter p3 = new Parameter("Min Cell Radius", "Minimum Cell Size (e.g. 0 to 6)", "0");
		Parameter p4 = new Parameter("Mode", "Which type of calculation to perform?", Parameter.DROPDOWN, new String[] { "Threshold", "Fixed number" }, 1);
		Parameter p5 = new Parameter("Threshold", "Cell radius in pixels (e.g. 3 to 30)", "15");
		Parameter p6 = new Parameter("Number of Cells", "Cell radius in pixels (e.g. 3 to 30)", "15");
		Parameter p7 = new Parameter("Loop order", "Reverse looping order?", Parameter.DROPDOWN, new String[] { "Normal", "Reverse" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
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
		JEXData data1 = inputs.get("Source Image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		
		// Run the function
		FindMaxHelperFunction2 graphFunc = new FindMaxHelperFunction2(entry, data1, data2, outputNames, parameters);
		graphFunc.doit();
		
		// Set the outputs
		JEXData output1 = graphFunc.output;
		JEXData output2 = graphFunc.counts;
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
}

class FindMaxHelperFunction2 implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	FindMaxima finder;
	List<DimensionMap> dimensions;
	TreeMap<DimensionMap,ROIPlus> rois;
	TreeMap<DimensionMap,PointList> pLists;
	TreeMap<DimensionMap,String> images;
	TreeMap<DimensionMap,String> outputCounts = new TreeMap<DimensionMap,String>();
	int index = 0;
	int atStep = 0;
	
	boolean auto = true;
	boolean ord = true;
	int roiX = -1;
	int roiY = -1;
	int roiW = -1;
	int roiH = -1;
	Roi roi = null;
	
	ParameterSet params;
	JEXData imset;
	JEXData jroi;
	JEXData output;
	JEXData counts;
	TypeName[] outputNames;
	JEXEntry entry;
	int count;
	
	FindMaxHelperFunction2(JEXEntry entry, JEXData imset, JEXData jroi, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.entry = entry;
		this.outputNames = outputNames;
		this.params = parameters;
		this.jroi = jroi;
		
		// make variables
		images = ImageReader.readObjectToImagePathTable(imset);
		this.roi = null;
		if(jroi != null)
		{
			rois = RoiReader.readObjectToRoiMap(jroi);
			ROIPlus roip = RoiReader.readObjectToRoi(jroi);
			this.roi = (roip == null) ? null : roip.getRoi();
		}
		
		dimensions = new ArrayList<DimensionMap>(0);
		for (DimensionMap map : images.keySet())
		{
			dimensions.add(map);
		}
		
		// //// Get params
		// boolean auto =
		// Boolean.parseBoolean(parameters.getValueOfParameter("Automatic"));
		int radius = Integer.parseInt(parameters.getValueOfParameter("Cell Radius"));
		int minradius = Integer.parseInt(parameters.getValueOfParameter("Min Cell Radius"));
		int threshold = Integer.parseInt(parameters.getValueOfParameter("Threshold"));
		int nbCells = Integer.parseInt(parameters.getValueOfParameter("Number of Cells"));
		String mode = parameters.getValueOfParameter("Mode");
		String order = parameters.getValueOfParameter("Loop order");
		this.ord = (order.equals("Reverse")) ? false : true;
		this.finder = new FindMaxima();
		
		this.finder.threshold = threshold;
		this.finder.cellNB = nbCells;
		this.finder.fixedCellNb = (mode.equals("Threshold")) ? false : true;
		this.finder.cellRadius = radius;
		this.finder.mincellRadius = minradius;
		if(!this.ord)
			index = this.images.size() - 1;
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Find Max");
		imagepanel.setRoi(roi);
		
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Select roi", new String[] { "Automatic", "Cell Radius", "Min Cell Radius", "Threshold", "Number of Cells", "Mode" });
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	private void displayImage(int index)
	{
		DimensionMap map = dimensions.get(index);
		String imPath = images.get(map);
		ImagePlus im = new ImagePlus(imPath);
		Logs.log("Placing in imagepanel " + imPath, 1, this);
		
		imagepanel.setImage(im);
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
		// Get the new parameters
		int radius = Integer.parseInt(params.getValueOfParameter("Cell Radius"));
		int minradius = Integer.parseInt(params.getValueOfParameter("Min Cell Radius"));
		int threshold = Integer.parseInt(params.getValueOfParameter("Threshold"));
		int nbCells = Integer.parseInt(params.getValueOfParameter("Number of Cells"));
		String mode = params.getValueOfParameter("Mode");
		finder.threshold = threshold;
		finder.cellNB = nbCells;
		finder.fixedCellNb = (mode.equals("Threshold")) ? false : true;
		finder.cellRadius = radius;
		finder.mincellRadius = minradius;
		
		// prepare the images for calculation
		DimensionMap map = dimensions.get(index);
		String imPath = images.get(map);
		ImagePlus im = new ImagePlus(imPath);
		java.awt.Rectangle rect = (roi == null) ? null : roi.getBounds();
		PointList result = finder.findMaximum(im, rect);
		
		if(pLists == null)
			pLists = new TreeMap<DimensionMap,PointList>();
		pLists.put(map.copy(), result);
		
		imagepanel.setImage(im);
		imagepanel.setPointList(result);
	}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return atStep;
	}
	
	public void loopNext()
	{
		if(!this.ord)
			index = index - 1;
		else
			index = index + 1;
		
		if(index >= images.size() - 1)
			index = images.size() - 1;
		if(index < 0)
			index = 0;
		
		runStep(index);
	}
	
	public void loopPrevious()
	{
		if(!this.ord)
			index = index + 1;
		else
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
		// if some steps were missed try to run them
		for (int index = 0; index < dimensions.size(); index++)
		{
			DimensionMap map = dimensions.get(index);
			
			PointList pList = pLists.get(map);
			if(pList == null || pList.size() == 0)
			{
				Logs.log("Runing missed step " + map, 1, this);
				runStep(index);
			}
			
			// Status bar
			int percentage = (int) (100 * ((double) index / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			// index ++;
		}
		
		HashMap<DimensionMap,ROIPlus> outputList = new HashMap<DimensionMap,ROIPlus>();
		for (int index = 0; index < dimensions.size(); index++)
		{
			DimensionMap map = dimensions.get(index);
			
			PointList pList = pLists.get(map);
			if(pList == null || pList.size() == 0)
			{
				outputCounts.put(map, "0");
				continue;
			}
			
			ROIPlus thisROI = new ROIPlus(pList, ROIPlus.ROI_POINT);
			count = thisROI.getPointList().size();
			outputList.put(map.copy(), thisROI);
			outputCounts.put(map, "" + count);
			Logs.log("Finished processing " + atStep + " of " + images.size() + ".", 1, this);
		}
		
		output = RoiWriter.makeRoiObject(outputNames[0].getName(), outputList);
		counts = ValueWriter.makeValueTable(outputNames[1].getName(), outputCounts);
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
