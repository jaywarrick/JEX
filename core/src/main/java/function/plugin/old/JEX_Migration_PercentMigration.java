package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.TrackReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import function.tracker.CompareTrajectories;
import ij.ImagePlus;
import image.roi.Trajectory;
import image.roi.XTrajectorySet;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
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
public class JEX_Migration_PercentMigration extends JEXCrunchable {
	
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
		String result = "Percent migrating";
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
		String result = "Quantifies the number of cells that are moving relative to those that are not.";
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
		inputNames[0] = new TypeName(TRACK, "Tracks to Process");
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
		defaultOutputNames[0] = new TypeName(VALUE, "Percent migrating");
		
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
		Parameter p1 = new Parameter("Automatic", "Perform without displaying GUI interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p2 = new Parameter("N Longest", "Keep only the N longest tracks (-1 keeps all)", "-1");
		Parameter p3 = new Parameter("N Fastest", "Keep only the N fastest tracks (-1 keeps all)", "-1");
		
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
		Logs.log("Collecting inputs", 1, this);
		JEXData data = inputs.get("Tracks to Process");
		if(!data.getTypeName().getType().equals(JEXData.TRACK))
			return false;
		
		// Run the function
		Logs.log("Running the function", 1, this);
		
		TracksPercentMigratingHelperFunction graphFunc = new TracksPercentMigratingHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		
		// Collect the outputs
		Logs.log("Collecting outputs", 1, this);
		double percentMoving = graphFunc.percentMoving();
		
		// Create string and saveString
		JEXData output = ValueWriter.makeValueObject(outputNames[0].getName(), "" + percentMoving);
		output.setDataObjectInfo("Value calculated with the function PERCENT MIGRATION");
		realOutputs.add(output);
		
		// Return status
		return true;
	}
	
}

class TracksPercentMigratingHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// GUI variables
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	
	// Parameters
	boolean auto = true;
	int longest = -1;
	int fastest = -1;
	
	// Variables
	ParameterSet parameters;
	List<Trajectory> allTrajectories;
	List<Trajectory> rankedTrajectories;
	List<Trajectory> rejectedTrajectories;
	List<Trajectory> movingTrajectories;
	Rectangle r;
	
	TracksPercentMigratingHelperFunction(JEXEntry entry, JEXData tracks, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.parameters = parameters;
		
		// //// Get params
		String autoStr = parameters.getValueOfParameter("Automatic");
		auto = Boolean.parseBoolean(autoStr);
		longest = Integer.parseInt(parameters.getValueOfParameter("N Longest"));
		fastest = Integer.parseInt(parameters.getValueOfParameter("N Fastest"));
		
		// Call the calculation function and set it up
		XTrajectorySet set = TrackReader.readObjectToTrajectorySet(tracks);
		allTrajectories = set.getTrajectories();
		Logs.log("Found " + set.getTrajectories().size(), 1, this);
		
		// Prepare the graphics
		r = makeBoundingImage();
		int width = (int) r.getWidth();
		int height = (int) r.getHeight();
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(width, height, type);
		ImagePlus im = new ImagePlus("", bimage);
		imagepanel = new ImagePanel(this, "Determine non migrating cells");
		imagepanel.setImage(im);
		
		imagepanel.resetRois();
		List<Trajectory> temp = new ArrayList<Trajectory>(0);
		for (Trajectory t : allTrajectories)
		{
			Point transP = new Point(r.x, r.y);
			Trajectory newT = t.translate(transP);
			temp.add(newT);
			imagepanel.addRoiWithColor(newT, Color.yellow);
		}
		allTrajectories = temp;
		
		wrap = new GraphicalFunctionWrap(this, parameters);
		wrap.addStep(0, "Select threshold", new String[] { "Automatic", "N Longest", "N Fastest" });
		wrap.setInCentralPanel(imagepanel);
	}
	
	private Rectangle makeBoundingImage()
	{
		int minx = 0;
		int maxx = 0;
		int miny = 0;
		int maxy = 0;
		for (Trajectory traj : allTrajectories)
		{
			List<Point> points = traj.getPoints();
			for (Point p : points)
			{
				if(p.x < minx)
					minx = p.x;
				if(p.x > maxx)
					maxx = p.x;
				if(p.y < miny)
					miny = p.y;
				if(p.y > maxy)
					maxy = p.y;
			}
		}
		Rectangle result = new Rectangle(minx, miny, maxx - minx, maxy - miny);
		return result;
	}
	
	public double percentMoving()
	{
		if(movingTrajectories == null || allTrajectories == null)
			return Double.NaN;
		double result = 100.0 * ((double) movingTrajectories.size()) / ((double) allTrajectories.size());
		return result;
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public boolean doit()
	{
		boolean b = wrap.start();
		if(!b)
			return false;
		
		return true;
	}
	
	public void runStep(int index)
	{
		String autoStr = parameters.getValueOfParameter("Automatic");
		auto = Boolean.parseBoolean(autoStr);
		longest = Integer.parseInt(parameters.getValueOfParameter("N Longest"));
		fastest = Integer.parseInt(parameters.getValueOfParameter("N Fastest"));
		
		rankedTrajectories = new ArrayList<Trajectory>(0);
		rejectedTrajectories = new ArrayList<Trajectory>(0);
		for (Trajectory traj : allTrajectories)
		{
			rankedTrajectories.add(traj);
		}
		
		// Sort by longest
		if(longest > 0)
		{
			CompareTrajectories longComp = new CompareTrajectories(CompareTrajectories.LENGTH);
			Collections.sort(rankedTrajectories, longComp);
			List<Trajectory> temp = new ArrayList<Trajectory>(0);
			for (int i = 0; i < longest; i++)
			{
				temp.add(rankedTrajectories.get(i));
			}
			for (int i = longest; i < rankedTrajectories.size(); i++)
			{
				rejectedTrajectories.add(rankedTrajectories.get(i));
			}
			rankedTrajectories = temp;
		}
		
		// Sort by fastest
		if(fastest > 0)
		{
			CompareTrajectories longComp = new CompareTrajectories(CompareTrajectories.VELOCITY);
			Collections.sort(rankedTrajectories, longComp);
			List<Trajectory> temp = new ArrayList<Trajectory>(0);
			for (int i = 0; i < fastest; i++)
			{
				temp.add(rankedTrajectories.get(i));
			}
			for (int i = fastest; i < rankedTrajectories.size(); i++)
			{
				rejectedTrajectories.add(rankedTrajectories.get(i));
			}
			rankedTrajectories = temp;
		}
		
		// Plot the trajectories
		imagepanel.resetRois();
		for (Trajectory t : rankedTrajectories)
		{
			imagepanel.addRoiWithColor(t, Color.yellow);
		}
		for (Trajectory t : rejectedTrajectories)
		{
			imagepanel.addRoiWithColor(t, Color.red);
		}
	}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return 0;
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
		movingTrajectories = new ArrayList<Trajectory>(0);
		for (Trajectory t : rankedTrajectories)
		{
			Point transP = new Point(-r.x, -r.y);
			t.translate(transP);
			movingTrajectories.add(t);
		}
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
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
