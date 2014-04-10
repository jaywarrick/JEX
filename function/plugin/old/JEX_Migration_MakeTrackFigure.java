package function.plugin.old;

import image.roi.Trajectory;
import image.roi.Vect;
import image.roi.XTrajectorySet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.TrackReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.GraphicalCrunchingEnabling;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.HistogramFactory;
import function.tracker.TrackStatistics;

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
public class JEX_Migration_MakeTrackFigure extends JEXCrunchable {
	
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
		String result = "8. Make track figure";
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
		String result = "Make histograms of track statisitcs.";
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
		inputNames[0] = new TypeName(TRACK, "Tracks to Analyze");
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
		defaultOutputNames = new TypeName[4];
		defaultOutputNames[0] = new TypeName(IMAGE, "Velocity Histogram");
		defaultOutputNames[1] = new TypeName(IMAGE, "Angle Histogram");
		defaultOutputNames[2] = new TypeName(IMAGE, "Wangle Histogram");
		defaultOutputNames[3] = new TypeName(IMAGE, "Tracks Image");
		
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
		Parameter p4 = new Parameter("Maximum velocity", "For outputting normalized data entrer a maximum velocity or -1 to deselect this feature", "-1");
		Parameter p5 = new Parameter("Max Displacement", "To output optimized track figures (-1 to deselect this feature)", "-1");
		Parameter p6 = new Parameter("Angle offset", "Rotate the angle histograms", "90");
		Parameter p7 = new Parameter("Color code", "Colors tracks moving forward in yellow and backwards in red", "yes");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
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
	JEXData data ;
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		data = inputs.get("Tracks to Analyze");
		if(!data.getTypeName().getType().equals(JEXData.TRACK))
			return false;
		
		// Run the function
		Logs.log("Running the function", 1, this);
		TracksFiguresHelperFunction graphFunc = new TracksFiguresHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		
		// Collect the outputs
		Logs.log("Collecting outputs", 1, this);
		
		// Velocity histogram
		List<Double> velocities = graphFunc.velocities;
		HistogramFactory hfact = new HistogramFactory();
		hfact.title = "Velocity Histogram";
		hfact.numberOfBins = 8;
		hfact.coord = HistogramFactory.CART_HISTOGRAM;
		BufferedImage im = hfact.makeCartesianHistogram(velocities);
		String vhPath = JEXWriter.saveFigure(im, "jpg"); // Logs saving activity
		JEXData output4 = ImageWriter.makeImageObject(outputNames[0].getName(), vhPath);
		output4.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output4);
		
		// Angle Histogram
		List<Double> angleA = graphFunc.wanglesA;
		HistogramFactory hfact3 = new HistogramFactory();
		hfact3.title = "Wangle Histogram";
		hfact3.numberOfBins = 8;
		hfact3.coord = HistogramFactory.POLAR_HISTOGRAM;
		BufferedImage im3 = hfact3.makePolarHistogram(angleA);
		String vhPath3 = JEXWriter.saveFigure(im3, "jpg"); // Logs saving
		JEXData output6 = ImageWriter.makeImageObject(outputNames[1].getName(), vhPath3);
		output6.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output6);
		
		// Wangle Histogram
		List<Double> wangleA = graphFunc.wanglesA;
		List<Double> wangleV = graphFunc.wanglesV;
		HistogramFactory hfact2 = new HistogramFactory();
		hfact2.title = "Wangle Histogram";
		hfact2.numberOfBins = 8;
		hfact2.coord = HistogramFactory.POLAR_HISTOGRAM;
		BufferedImage im2 = hfact2.makeWeighedPolarHistogram(wangleA, wangleV);
		String vhPath2 = JEXWriter.saveFigure(im2, "jpg"); // Logs saving
		JEXData output5 = ImageWriter.makeImageObject(outputNames[2].getName(), vhPath2);
		output5.setDataObjectInfo("Value determined using the track analysis function");
		realOutputs.add(output5);
		
		// Tracks image
		BufferedImage im4 = makeTracksImage();
		String vhPath4 = JEXWriter.saveFigure(im4, "jpg"); 
		JEXData output7 = ImageWriter.makeImageObject(outputNames[3].getName(), vhPath4);
		output7.setDataObjectInfo("Tracks plotted from origin");
		realOutputs.add(output7);
		
		// Return status
		return true;
	}
	
	private BufferedImage makeTracksImage()
	{
		
		// Set size of final image
		int width = 600;
		int height = 600;
		int type = BufferedImage.TYPE_INT_RGB;
		
		// Create image
		BufferedImage bimage = new BufferedImage(width, height, type);
		Graphics g = bimage.createGraphics();
		
		// Draw the white background
		g.setColor(Color.white);
		g.fillRect(0, 0, width, height);
		
		// Draw the axes
		g.setColor(Color.black);
		g.drawLine(width/2, 0, width/2, height);
		g.drawLine(width/2-1, 0, width/2-1, height);
		g.drawLine(width/2+1, 0, width/2+1, height);
		g.drawLine(0, height/2, width, height/2);
		g.drawLine(0, height/2-1, width, height/2-1);
		g.drawLine(0, height/2+1, width, height/2+1);
		
		// Get the scaling parameters
		double maxDisplacement = Double.parseDouble(parameters.getValueOfParameter("Max Displacement"));
		double scale = (maxDisplacement==-1)? 1 : Math.min(height/2, width/2) / maxDisplacement;
		
		// Color coding parameters
		String colorcode = parameters.getValueOfParameter("Color code");
		boolean color = (colorcode.equals("yes")) ? true : false;
		
		// Load the track
		XTrajectorySet set = TrackReader.readObjectToTrajectorySet(data);
		List<Trajectory> trajectories = set.getTrajectories();
		
		Random random = new Random();
		for (int i = 0, len = trajectories.size(); i < len; i++)
		{
			// Choose a random color for the track
			Color nextColor = Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F);
			g.setColor(nextColor);
			
			// Load the track
			Trajectory traj = trajectories.get(i);
			if (traj == null) continue;
			
			// Initialize the variables
			List<Point> points = traj.getPoints();
			
			// If the track is too short (0 or 1) discard it
			if (points.size() <2) continue;
			
			// Initalize the point counter
			Point p1 = points.get(0);
			Point p2 = null;
			
			// Remember origin
			int x0 = (int) scale * p1.x;
			int y0 = (int) scale * p1.y;
			
			// Color code tracks?
			if (color)
			{
				Point first = points.get(0);
				Point last  = points.get(points.size()-1);
				if (first.y - last.y > 0) nextColor = Color.red;
				else if (first.y - last.y <= 0) nextColor = Color.yellow;
				g.setColor(nextColor);
			}
			
			// Plot the first point
			int x1 = width/2;
			int y1 = height/2;
			g.fillOval(x1,y1, 3, 3);
			
			for (int j = 1, lenj = points.size(); j < lenj; j++)
			{
				// Get the next point
				p2 = traj.getPoint(j);
				if (p1 == null || p2 == null) continue;
				
				// Calculate displacement
				int dispX = (int) scale * p2.x - x0; 
				int dispY = (int) scale * p2.y - y0; 
				
				// Get the x and y variables of the point
				int x2 = x1 + dispX;
				int y2 = y1 + dispY;
				
				// Reset origin
				x0 = x0 + dispX;
				y0 = y0 + dispY;
				
				// Draw the line and next dot
				g.drawLine(x1,y1,x2,y2);
				g.fillOval(x2,y2, 3, 3);
				
				// Re-initialize the variables
				p1 = p2;
				x1 = x2;
				y1 = y2;
			}
		}
		
		g.dispose();
		return bimage;
	}
}

class TracksFiguresHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	//boolean auto = true;
	//int mode;
	//int nbVect;
	//int stepSize;
	double maxVel;
	int angleOff;
	
	ParameterSet parameters;
	JEXData tracks;
	JEXEntry entry;
	TrackStatistics stats;
	XTrajectorySet set;
	
	double meanVelocity;
	double meanAngle;
	double chemoIndex;
	List<Double> velocities;
	List<Double> angles;
	List<Vect> vectors;
	List<Double> wanglesA;
	List<Double> wanglesV;
	List<Double> distTrav;
	
	TracksFiguresHelperFunction(JEXEntry entry, JEXData tracks, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.tracks = tracks;
		this.parameters = parameters;
		this.entry = entry;
		
		// //// Get params
		maxVel = Double.parseDouble(parameters.getValueOfParameter("Maximum velocity"));
		angleOff = Integer.parseInt(parameters.getValueOfParameter("Angle offset"));
		
		// Call the calculation function and set it up
		set = TrackReader.readObjectToTrajectorySet(tracks);
		Logs.log("Found " + set.getTrajectories().size(), 1, this);
		
		// Set up the calculation functions
		stats = new TrackStatistics(set);
		stats.angleOffset = angleOff;
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public boolean doit()
	{
		boolean b = true;
		
		stats.startAnalysis();
		meanVelocity = stats.meanVelocity;
		meanAngle = stats.meanAngle;
		chemoIndex = stats.CI;
		velocities = stats.velocities;
		angles = stats.angles;
		vectors = stats.vectors;
		
		wanglesA = new Vector<Double>(0);
		wanglesV = new Vector<Double>(0);
		distTrav = new Vector<Double>(0);
		for (int i = 0, len = vectors.size(); i < len; i++)
		{
			Vect v = vectors.get(i);
			if(v.norm() == 0)
				continue;
			Double a = v.angle() + angleOff;
			Double w = v.norm() / len;
			a = normalizeAngle(a);
			wanglesA.add(a);
			wanglesV.add(w);
		}
		distTrav = stats.distTrav;
		
		return b;
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
	
	public void runStep(int index)
	{}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return 0;
	}
	
	public void startIT()
	{}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT()
	{}
	
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
