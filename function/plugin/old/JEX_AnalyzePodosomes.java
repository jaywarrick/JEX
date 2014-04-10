package function.plugin.old;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageStatistics;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.Trajectory;
import image.roi.XTrajectorySet;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.DataWriter.TrackWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.imageUtility.jBackgroundSubtracter;
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
public class JEX_AnalyzePodosomes extends JEXCrunchable {
	
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
		String result = "Analyze Podosomes";
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
		String result = "Analyze the assembly and dissassembly of podosomes in a fluorescent timelapse image stack";
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
		String toolbox = "Custom Cell Analysis";
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
		
		inputNames[0] = new TypeName(IMAGE, "Timelapse");
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
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(VALUE, "Podosome intensities");
		
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
		Parameter p1 = new Parameter("Background radius", "Radius of the background subtract", "50.0");
		Parameter p2 = new Parameter("Threshold", "Threshold for identifying podosomes", "40.0");
		Parameter p3 = new Parameter("Cluster radius", "Minimum distance between two podosomes", "30.0");
		Parameter p4 = new Parameter("Min cluster size", "Minimum cluster size", "5.0");
		Parameter p5 = new Parameter("Max cluster size", "Maximum cluster size", "200.0");
		Parameter p6 = new Parameter("Recalculate", "Recalculate", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p7 = new Parameter("Minimum length", "Minimum length of a track to keep", "4.0");
		
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
		JEXData data1 = inputs.get("Timelapse");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		
		// Run the function
		FindPodosomeHelperFunction graphFunc = new FindPodosomeHelperFunction(entry, data1, data2, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.output1;
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
}

class FindPodosomeHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	int frame = 0;
	ByteProcessor imp;
	ByteProcessor orimp;
	ImagePlus orim;
	ImagePlus im;
	int nb;
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	public JEXData output3;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	boolean recalc = false;
	double threshold = 10;
	double bckradius = 10;
	double clusterradius = 25;
	double minSize = 10;
	double maxSize = 100;
	
	// Optional ROI
	float[] xPos;
	float[] yPos;
	float[] intDen;
	float[] areas;
	HashMap<Integer,HashMap<Point,Double>> pi;
	PointList[] pointListArray;
	
	// Input
	JEXData imset;
	JEXData roiset;
	JEXEntry entry;
	List<String> jimages;
	List<PointList> rois;
	Trajectory[] trajs;
	Trajectory[] finaltrajs;
	ROIPlus roi;
	TypeName[] outputNames;
	
	FindPodosomeHelperFunction(JEXEntry entry, JEXData imset, JEXData roiset, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.params = parameters;
		this.roiset = roiset;
		this.entry = entry;
		this.outputNames = outputNames;
		pi = new HashMap<Integer,HashMap<Point,Double>>();
		
		// find the data
		jimages = ImageReader.readObjectToImagePathList(imset);
		roi = RoiReader.readObjectToRoi(roiset);
		
		// //// Get params
		auto = false;
		bckradius = Double.parseDouble(parameters.getValueOfParameter("Background radius"));
		threshold = Double.parseDouble(parameters.getValueOfParameter("Threshold"));
		clusterradius = Double.parseDouble(parameters.getValueOfParameter("Cluster radius"));
		minSize = Double.parseDouble(parameters.getValueOfParameter("Min cluster size"));
		maxSize = Double.parseDouble(parameters.getValueOfParameter("Max cluster size"));
		recalc = Boolean.parseBoolean(parameters.getValueOfParameter("Recalculate"));
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Analyze podosomes");
		imagepanel.setRoi(roi.getRoi());
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Subtract background", new String[] { "Background radius" });
		wrap.addStep(1, "Threshold", new String[] { "Threshold" });
		wrap.addStep(2, "Extract cells", new String[] { "Min cluster size", "Max cluster size" });
		wrap.addStep(3, "Find all", new String[] { "Cluster radius", "Recalculate" });
		wrap.addStep(4, "Track", new String[0]);
		wrap.addStep(5, "Select Tracks", new String[] { "Minimum length" });
		wrap.addStep(6, "Calculate Intensities", new String[0]);
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
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
		
		// //// Get params
		bckradius = Double.parseDouble(params.getValueOfParameter("Background radius"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		clusterradius = Double.parseDouble(params.getValueOfParameter("Cluster radius"));
		minSize = Double.parseDouble(params.getValueOfParameter("Min cluster size"));
		maxSize = Double.parseDouble(params.getValueOfParameter("Max cluster size"));
		recalc = Boolean.parseBoolean(params.getValueOfParameter("Recalculate"));
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		if(atStep == 0)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			
			subtractBackground();
		}
		else if(atStep == 1)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			
			subtractBackground();
			threshold();
		}
		else if(atStep == 2)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			
			subtractBackground();
			threshold();
			analyzeParticles();
		}
		else if(atStep == 3)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			
			if(recalc || pointListArray == null)
			{
				makePointList();
			}
			
			Color[] colors = new Color[pointListArray.length];
			for (int i = 0; i < pointListArray.length; i++)
			{
				int shade = 255 * i / (pointListArray.length);
				colors[i] = new Color(shade, shade, 255);
			}
			imagepanel.setPointListArray(pointListArray, colors);
			imagepanel.setCellRadius((int) clusterradius);
		}
		else if(atStep == 4)
		{
			if(pointListArray == null)
			{
				makePointList();
			}
			
			makeTracks();
			
			imagepanel.setTracks(trajs);
			imagepanel.setPointListArray(null, null);
		}
		else if(atStep == 5)
		{
			if(trajs == null)
			{
				makePointList();
				makeTracks();
			}
			
			selectTracks();
			
			imagepanel.setTracks(finaltrajs);
			imagepanel.setPointListArray(null, null);
		}
		else if(atStep == 6)
		{
			if(finaltrajs == null)
			{
				makePointList();
				makeTracks();
				selectTracks();
			}
			calculateIntensities();
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
		// //// Get params
		bckradius = Double.parseDouble(params.getValueOfParameter("Background radius"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		clusterradius = Double.parseDouble(params.getValueOfParameter("Cluster radius"));
		minSize = Double.parseDouble(params.getValueOfParameter("Min cluster size"));
		maxSize = Double.parseDouble(params.getValueOfParameter("Max cluster size"));
		
		makePointList();
		makeTracks();
		selectTracks();
		calculateIntensities();
	}
	
	private void subtractBackground()
	{
		orimp = (ByteProcessor) imp.duplicate();
		orim = new ImagePlus("", orimp);
		jBackgroundSubtracter bS = new jBackgroundSubtracter();
		bS.setup("", im);
		jBackgroundSubtracter.radius = bckradius; // default rolling ball radius
		jBackgroundSubtracter.lightBackground = false;
		jBackgroundSubtracter.createBackground = false;
		jBackgroundSubtracter.useParaboloid = false; // use "Sliding Paraboloid"
		// instead of rolling ball
		// algorithm
		jBackgroundSubtracter.doPresmooth = false;
		bS.run(imp);
		
		if(roi != null)
		{
			java.awt.Rectangle rect = roi.getRoi().getBounds();
			imp.setRoi(rect);
			imp = (ij.process.ByteProcessor) imp.crop();
		}
		
		// FunctionUtility.imSave(imp, "./quantifyMigration1.tif");
	}
	
	private void threshold()
	{
		// imp.invert();
		imp.threshold((int) threshold);
		// IJ.run(new ImagePlus("",bimp), "Watershed",null);
		imp.invertLut();
		
		// Watershed
		EDM edm = new EDM();
		edm.toWatershed(imp);
		
		// FunctionUtility.imSave(imp,"./quantifyMigration2.tif");
	}
	
	private void analyzeParticles()
	{
		ResultsTable rT = new ResultsTable();
		int options = ParticleAnalyzer.SHOW_OUTLINES | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
		int measure = Measurements.AREA | Measurements.INTEGRATED_DENSITY | Measurements.CENTER_OF_MASS | Measurements.MEAN;
		
		ParticleAnalyzer pA = new ParticleAnalyzer(options, measure, rT, minSize, maxSize);
		pA.setHideOutputImage(true);
		pA.analyze(new ImagePlus("", imp));
		List<Roi> foundRois = pA.foundRois;
		
		nb = rT.getColumn(0).length;
		int lastColumn = rT.getLastColumn();
		xPos = new float[0];
		yPos = new float[0];
		intDen = new float[0];
		areas = new float[0];
		
		for (int i = 0; i < lastColumn; i++)
		{
			String cName = rT.getColumnHeading(i);
			if(cName.equals("XM"))
			{
				xPos = rT.getColumn(i);
			}
			if(cName.equals("YM"))
			{
				yPos = rT.getColumn(i);
			}
			if(cName.equals("Area"))
			{
				areas = rT.getColumn(i);
			}
			if(cName.equals("Mean"))
			{
				intDen = rT.getColumn(i);
			}
		}
		
		ImagePlus outIm = pA.getOutputImage();
		imp = (ByteProcessor) outIm.getProcessor().convertToByte(true);
		
		double[] values = new double[foundRois.size()];
		HashMap<Point,Double> pd = new HashMap<Point,Double>();
		for (int i = 0, len = foundRois.size(); i < len; i++)
		{
			// Get the intensity
			Roi roi = foundRois.get(i);
			orim.setRoi(roi);
			ImageStatistics stats = orim.getStatistics(Measurements.INTEGRATED_DENSITY | Measurements.AREA | Measurements.MEAN);
			values[i] = (stats.area == 0) ? 0 : (double) stats.area * (double) stats.mean;
			
			// get the point
			Point p = new Point((int) xPos[i], (int) yPos[i]);
			
			// Put the into the hashmap
			pd.put(p, values[i]);
		}
		
		if(pi == null)
		{
			pi = new HashMap<Integer,HashMap<Point,Double>>();
		}
		pi.put(frame, pd);
	}
	
	private void makePointList()
	{
		// Run the function
		int count = 0;
		int total = jimages.size();
		pointListArray = new PointList[total];
		pi = new HashMap<Integer,HashMap<Point,Double>>();
		
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imset);
		TreeMap<DimensionMap,ROIPlus> outputMap = new TreeMap<DimensionMap,ROIPlus>();
		
		for (DimensionMap dim : imageMap.keySet())
		{
			// get the data
			String path = imageMap.get(dim);
			
			// get the image
			im = new ImagePlus(path);
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			frame = count;
			
			// //// Begin Actual Function
			subtractBackground();
			threshold();
			analyzeParticles();
			
			PointList pList = new PointList();
			String pointStr = "";
			for (int i = 0, len = xPos.length; i < len; i++)
			{
				int x = (int) xPos[i];
				int y = (int) yPos[i];
				Point p = new Point(x, y);
				pList.add(p);
				pointStr = pointStr + " - " + x + "," + y;
			}
			pointListArray[count] = pList;
			
			// //// Prepare outputs
			ROIPlus outRoi = new ROIPlus(pList, ROIPlus.ROI_POINT);
			outputMap.put(dim.copy(), outRoi);
			
			// Go to next image
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
		}
		
		output1 = RoiWriter.makeRoiObject(outputNames[0].getName(), outputMap);
	}
	
	private void makeTracks()
	{
		TrackExtend extender = new TrackExtend();
		extender.maxDisplacement = (int) clusterradius;
		extender.maxDissapear = 3;
		extender.spf = 1;
		extender.mpp = 1;
		extender.setExtensionMode(TrackExtend.EXTEND_TO_CLOSEST);
		
		for (int i = 0, len = pointListArray.length; i < len; i++)
		{
			PointList current = pointListArray[i];
			
			Logs.log("Extended points for frame " + i + " of " + len + ".", 1, this);
			extender.extendWithPoints(current, i);
		}
		
		List<Trajectory> trajectories = extender.getTrajectories();
		trajs = trajectories.toArray(new Trajectory[0]);
	}
	
	private void selectTracks()
	{
		int minLen = (int) Float.parseFloat(params.getValueOfParameter("Minimum length"));
		List<Trajectory> selected = new ArrayList<Trajectory>();
		
		for (int i = 0, len = trajs.length; i < len; i++)
		{
			Trajectory t = trajs[i];
			int starts = t.initial();
			int ends = t.length();
			int size = t.nbPoints();
			
			if(starts > 0 && ends < jimages.size() - 1 && size > minLen)
			{
				selected.add(t);
			}
		}
		
		finaltrajs = selected.toArray(new Trajectory[0]);
		
		// Make xml element
		XTrajectorySet trajSet = new XTrajectorySet();
		for (Trajectory traj : finaltrajs)
		{
			if(traj.nbPoints() > 1)
				trajSet.addTrajectory(traj);
		}
		
		output2 = TrackWriter.makeTracksObject(outputNames[1].getName(), selected);
	}
	
	private void calculateIntensities()
	{
		TreeMap<DimensionMap,String> valueMap = new TreeMap<DimensionMap,String>();
		for (int i = 0, len = finaltrajs.length; i < len; i++)
		{
			Trajectory t = finaltrajs[i];
			
			for (int j = 0, lenj = jimages.size(); j < lenj; j++)
			{
				Point p = t.getPoint(j);
				double value = 0;
				
				if(p != null)
				{
					HashMap<Point,Double> pd = pi.get(j);
					if(pd != null)
					{
						Double v = pd.get(p);
						if(v != null)
						{
							value = v;
						}
					}
				}
				
				DimensionMap map = new DimensionMap();
				map.put("Podosome", "" + i);
				map.put("Frame", "" + j);
				valueMap.put(map, "" + value);
			}
		}
		
		output3 = ValueWriter.makeValueTable(outputNames[2].getName(), valueMap);
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
