package function.experimentalDataProcessing;

import ij.ImagePlus;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.Trajectory;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.TrackWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.ExperimentalDataCrunch;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.TrackExtend;

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
public class JEX_Migration_TrackCells extends ExperimentalDataCrunch {
	
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
		String result = "5. Track";
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
		String result = "Link cell positions in a timelapse image together to form tracks.";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(ROI, "Cell positions");
		inputNames[1] = new TypeName(IMAGE, "Optional timelapse");
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
		defaultOutputNames[0] = new TypeName(TRACK, "Tracks");
		
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
		
		Parameter p2 = new Parameter("Max Displacement", "Maximum diplacement of a cell in pixels", "35");
		Parameter p3 = new Parameter("Max Dissapearance", "Maximum number of frames a cell can dissapear", "3");
		Parameter p4 = new Parameter("Seconds Per Frame", "Seconds between each frame", "15");
		Parameter p5 = new Parameter("Micron Per Pixel", "Image Scale in Micrometers per pixel", "0.3");
		Parameter p6 = new Parameter("Extension algorithm", "Use a different extention algorithm", Parameter.DROPDOWN, new String[] { "Minimize displacements", "Closest neighbour" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
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
		// Collect the inputs
		JEXData data1 = inputs.get("Cell positions");
		if(!data1.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		JEXData data2 = inputs.get("Optional timelapse");
		
		// Run the function
		TrackHelperFunction graphFunc = new TrackHelperFunction(entry, data1, data2, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.output;
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
}

class TrackHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	TrackExtend extender;
	boolean auto = true;
	
	int index = 0;
	List<ROIPlus> rois;
	List<String> oimages;
	
	ParameterSet params;
	JEXData roiset;
	JEXData oimset;
	JEXData output;
	JEXEntry entry;
	TypeName[] outputNames;
	
	TrackHelperFunction(JEXEntry entry, JEXData roiset, JEXData oimset, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.roiset = roiset;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		this.rois = RoiReader.readObjectToRoiList(roiset);
		this.oimages = ImageReader.readObjectToImagePathList(oimset);
		this.extender = new TrackExtend();
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Track");
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Extend tracks", new String[] { "Automatic", "Max Displacement", "Max Dissapearance", "Seconds Per Frame", "Micron Per Pixel", "Extension algorithm" });
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	private void displayImage(int index)
	{
		if(oimset == null)
		{   
			
		}
		else
		{
			ImagePlus im = new ImagePlus(oimages.get(index));
			imagepanel.setImage(im);
		}
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		wrap.start();
		return;
	}
	
	public void runStep(int index)
	{
		// Get the new parameters
		boolean auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		int maxDisplacement = Integer.parseInt(params.getValueOfParameter("Max Displacement"));
		int maxDissapear = Integer.parseInt(params.getValueOfParameter("Max Dissapearance"));
		double spf = Double.parseDouble(params.getValueOfParameter("Seconds Per Frame"));
		double mpp = Double.parseDouble(params.getValueOfParameter("Micron Per Pixel"));
		String extensionModeStr = params.getValueOfParameter("Extension algorithm");
		int extensionMode = (extensionModeStr.equals("Minimize displacements")) ? TrackExtend.EXTEND_TO_MINIMAL : TrackExtend.EXTEND_TO_CLOSEST;
		
		extender.maxDisplacement = maxDisplacement;
		extender.maxDissapear = maxDissapear;
		extender.spf = spf;
		extender.mpp = mpp;
		extender.setExtensionMode(extensionMode);
		
		// image plus
		ImagePlus im = new ImagePlus(oimages.get(index));
		imagepanel.setImage(im);
		
		// Display the points still remaining for calculation
		int len = rois.size() - index;
		PointList[] remainingPointLists = new PointList[len];
		Color[] colors = new Color[len];
		for (int i = index; i < rois.size(); i++)
		{
			remainingPointLists[i - index] = rois.get(i).getPointList();
			int shade = 255 * i / (rois.size());
			colors[i - index] = new Color(shade, shade, 255);
		}
		imagepanel.setPointListArray(remainingPointLists, colors);
		
		// Calculate the extension
		PointList current = rois.get(index).getPointList();
		imagepanel.setCellRadius(maxDisplacement);
		imagepanel.setPointList(current);
		extender.extendWithPoints(current, index);
		
		// Plot the trajectories
		List<Trajectory> trajectories = extender.getTrajectories();
		Trajectory[] trajs = trajectories.toArray(new Trajectory[0]);
		imagepanel.setTracks(trajs);
		
		if(auto && index < rois.size() - 1)
			loopNext();
	}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return 0;
	}
	
	public void loopNext()
	{
		index = index + 1;
		if(index >= rois.size() - 1)
			index = rois.size() - 1;
		runStep(index);
	}
	
	public void loopPrevious()
	{
		index = index - 1;
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
		Logs.log("Retrieving and saving the tracks", 1, this);
		
		// Get tracks
		List<Trajectory> trajectories = extender.getTrajectories();
		List<Trajectory> selected = new ArrayList<Trajectory>();
		
		// Make xml element
		for (Trajectory traj : trajectories)
		{
			if(traj.nbPoints() > 1)
				selected.add(traj);
		}
		
		// Create string and save
		output = TrackWriter.makeTracksObject(outputNames[0].getName(), selected);
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
