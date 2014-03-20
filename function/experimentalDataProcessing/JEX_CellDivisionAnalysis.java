package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.Trajectory;
import image.roi.Vect;
import image.roi.VectSet;
import image.roi.XTrajectorySet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
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
import ch.randelshofer.media.quicktime.QuickTimeOutputStream;
import function.ExperimentalDataCrunch;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.tracker.FindMaxima;
import function.tracker.SimpleConvolve;
import function.tracker.TrackExtend;
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
public class JEX_CellDivisionAnalysis extends ExperimentalDataCrunch {
	
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
		String result = "Cell division analysis";
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
		String result = "Track and analyze the division of cells in bright field microscopy";
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
		TypeName[] inputNames = new TypeName[1];
		
		inputNames[0] = new TypeName(IMAGE, "Timelapse");
		
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
		defaultOutputNames = new TypeName[5];
		defaultOutputNames[0] = new TypeName(TRACK, "Tracks");
		defaultOutputNames[1] = new TypeName(VALUE, "Duration");
		defaultOutputNames[2] = new TypeName(VALUE, "Percentage dividing");
		defaultOutputNames[3] = new TypeName(VALUE, "More information");
		defaultOutputNames[4] = new TypeName(MOVIE, "Track Movie");
		
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
		Parameter p1 = new Parameter("Binning", "Binning factor for quicker analysis", "2.0");
		Parameter p2 = new Parameter("Threshold", "Threshold for identifying neutrophil locations", "40.0");
		Parameter p3 = new Parameter("Filter tracks", "Keep tracks of mean velocity superior to", "1.0");
		Parameter p4 = new Parameter("Cell Radius", "Cell radius in pixels (e.g. 3 to 30)", "15");
		Parameter p5 = new Parameter("Minimum length", "Minimum length for keeping a track in number of points", "10");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
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
		JEXData data = inputs.get("Timelapse");
		if(!data.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		CellDivisionHelperFunction graphFunc = new CellDivisionHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.output1;
		JEXData output2 = graphFunc.output2;
		JEXData output3 = graphFunc.output3;
		JEXData output4 = graphFunc.output4;
		JEXData output5 = graphFunc.output5;
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		realOutputs.add(output4);
		realOutputs.add(output5);
		
		// Return status
		return true;
	}
}

class CellDivisionHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int bin = 2;
	double threshold = 10;
	int minLength = 10;
	int radius = 15;
	double minVel = 10;
	
	// Variables used during the function steps
	private ByteProcessor imp;
	private ImagePlus im;
	static ImagePlus cellImage = null;
	private Roi imageRoi = null;
	private Roi roi = null;
	
	// Input
	JEXData imset;
	JEXEntry entry;
	TypeName[] outputNames;
	List<String> jimages;
	TreeMap<Integer,PointList> pLists;
	Trajectory[] trajs;
	Trajectory[] finaltrajs;
	XTrajectorySet trajSet;
	
	CellDivisionHelperFunction(JEXEntry entry, JEXData imset, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		this.pLists = new TreeMap<Integer,PointList>();
		
		// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		minLength = Math.max(1, Integer.parseInt(params.getValueOfParameter("Minimum length")));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		minVel = Double.parseDouble(params.getValueOfParameter("Filter tracks"));
		
		// Prepare function
		jimages = ImageReader.readObjectToImagePathList(imset);
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Analyze neutrophil migration in POCT channels");
		imagepanel.setRoi(roi);
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Reduce image size", new String[] { "Binning" });
		wrap.addStep(1, "Extract neutrophil image", new String[0]);
		wrap.addStep(2, "Set channel ROI", new String[0]);
		wrap.addStep(3, "Convolve", new String[0]);
		wrap.addStep(4, "Find neutrophils", new String[] { "Threshold", "Cell Radius" });
		wrap.addStep(5, "Apply to stack", new String[0]);
		wrap.addStep(6, "Track", new String[0]);
		wrap.addStep(7, "Select tracks", new String[] { "Filter tracks", "Minimum length" });
		wrap.addStep(8, "Analysis", new String[] { "Automatic" });
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
		wrap.start();
		return;
	}
	
	public void runStep(int step)
	{
		atStep = step;
		
		// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		minLength = Math.max(1, Integer.parseInt(params.getValueOfParameter("Minimum length")));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		minVel = Double.parseDouble(params.getValueOfParameter("Filter tracks"));
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		if(atStep == 0)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
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
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			binImage();
			extractImage();
			interactionMode = true;
		}
		else if(atStep == 2)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			binImage();
			makeROI();
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 3)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(imageRoi);
			
			binImage();
			convolve();
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 4)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			
			binImage();
			convolve();
			findMax();
			interactionMode = false;
			
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(imageRoi);
			imagepanel.setPointList(pLists.get(index));
			imagepanel.setCellRadius(radius);
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 5)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			
			findMaxInStack();
			interactionMode = false;
			
			Color[] colors = new Color[pLists.size()];
			PointList[] pls = new PointList[pLists.size()];
			for (int i = 0; i < pls.length; i++)
			{
				int shade = 255 * i / (pls.length);
				pls[i] = pLists.get(i);
				colors[i] = new Color(shade, shade, 255);
			}
			imagepanel.setPointList(null);
			imagepanel.setPointListArray(pls, colors);
			imagepanel.setCellRadius(radius);
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 6)
		{
			track();
			
			imagepanel.setPointList(null);
			imagepanel.setTracks(trajs);
			imagepanel.setPointListArray(null, null);
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 7)
		{
			selectTracks();
			
			imagepanel.setTracks(finaltrajs);
			imagepanel.setPointListArray(null, null);
			interactionMode = false;
			if(auto)
			{
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if(atStep == 8)
		{
			analyze();
			
			imagepanel.setTracks(finaltrajs);
			imagepanel.setPointListArray(null, null);
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
		if(atStep > 8)
			atStep = 8;
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
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		minVel = Double.parseDouble(params.getValueOfParameter("Filter tracks"));
		
		if(!auto)
		{
			findMaxInStack();
			track();
			selectTracks();
			analyze();
		}
		makeMovie();
	}
	
	private void binImage()
	{
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
	}
	
	private void extractImage()
	{
		if(auto)
			return;
		ImagePlus image = new ImagePlus("", imp);
		image.setRoi(roi);
		ImageProcessor cellImp = image.getProcessor().crop();
		cellImage = new ImagePlus("", cellImp);
		roi = null;
	}
	
	private void makeROI()
	{
		if(roi == null)
		{
			imageRoi = new Roi(0, 0, im.getWidth(), im.getHeight());
		}
		else
		{
			imageRoi = roi;
		}
	}
	
	private void convolve()
	{
		SimpleConvolve convolver = new SimpleConvolve();
		ImagePlus result = convolver.start(new ImagePlus("", imp), cellImage, null);
		imp = (ByteProcessor) result.getProcessor().convertToByte(true);
	}
	
	private void findMax()
	{
		FindMaxima finder = new FindMaxima();
		finder.threshold = (int) threshold;
		finder.cellNB = -1;
		finder.fixedCellNb = false;
		finder.cellRadius = radius;
		java.awt.Rectangle rect = (roi == null) ? null : imageRoi.getBounds();
		PointList result = finder.findMaximum(new ImagePlus("", imp), rect);
		pLists.put(new Integer(index), result);
	}
	
	private void findMaxInStack()
	{
		for (int j = 0, len = jimages.size(); j < len; j++)
		{
			index = j;
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			
			binImage();
			convolve();
			findMax();
			
			// Status bar
			int percentage = (int) (100 * ((double) j / (double) jimages.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
	}
	
	private void track()
	{
		TrackExtend extender = new TrackExtend();
		extender.maxDisplacement = radius;
		extender.maxDissapear = 3;
		extender.spf = 1;
		extender.mpp = 1;
		extender.setExtensionMode(TrackExtend.EXTEND_TO_CLOSEST);
		
		Set<Integer> keys = pLists.keySet();
		for (Integer key : keys)
		{
			PointList current = pLists.get(key);
			Logs.log("Extended points for frame " + key + " of " + keys.size() + ".", 1, this);
			extender.extendWithPoints(current, key);
		}
		
		List<Trajectory> trajectories = extender.getTrajectories();
		trajs = trajectories.toArray(new Trajectory[0]);
	}
	
	private void selectTracks()
	{
		List<Trajectory> selected = new ArrayList<Trajectory>();
		
		for (int i = 0, len = trajs.length; i < len; i++)
		{
			Trajectory t = trajs[i];
			TrackStatistics stats = new TrackStatistics();
			stats.startAnalysis(t);
			if(t.nbPoints() > minLength && stats.meanVelocity > minVel)
			{
				selected.add(t);
			}
		}
		
		// Make the JEXTrack object
		output1 = TrackWriter.makeTracksObject(outputNames[0].getName(), selected);
	}
	
	private void analyze()
	{
		TrackStatistics stats = new TrackStatistics(trajSet);
		stats.deltaFrame = 1;
		stats.micronPerPixel = 1;
		stats.nbCells = 1000000;
		stats.angleOffset = 90;
		stats.secondPerFrame = 30;
		
		stats.startAnalysis();
		double meanVelocity = stats.meanVelocity;
		double chemoIndex = stats.CI;
		VectSet vectors = stats.allvectors;
		
		Vector<Double> wanglesA = new Vector<Double>(0);
		Vector<Double> wanglesV = new Vector<Double>(0);
		for (int i = 0, len = vectors.size(); i < len; i++)
		{
			Vect v = vectors.get(i);
			if(v.norm() == 0)
				continue;
			Double a = v.angle() + 90;
			Double w = (double) v.norm();
			a = normalizeAngle(a);
			wanglesA.add(a);
			wanglesV.add(w);
		}
		
		// Mean velocity
		output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + meanVelocity);
		output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + chemoIndex);
		
		// Table sheet
		String[] columns = new String[] { "Angles", "Velocities" };
		HashMap<String,List<Double>> columnData = new HashMap<String,List<Double>>();
		columnData.put("Angles", wanglesA);
		columnData.put("Velocities", wanglesV);
		output4 = ValueWriter.makeValueTableFromDoubleList(outputNames[3].getName(), columns, columnData);
	}
	
	private void makeMovie()
	{
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		Logs.log("Saving the movie to path " + path, 1, this);
		
		// ------------------------------
		// save the movie of the tracking
		File outMovieFile = new File(path);
		QuickTimeOutputStream newStream = null;
		try
		{
			QuickTimeOutputStream.VideoFormat format = QuickTimeOutputStream.VideoFormat.values()[0];
			newStream = new QuickTimeOutputStream(outMovieFile, format);
		}
		catch (IOException e)
		{
			Logs.log("Not possible to create movie ... ", 1, this);
		}
		
		List<Trajectory> trajectories = trajSet.getTrajectories();
		int binning = 2;
		float compression = (float) 0.7;
		
		// add each image one by one
		for (int k = 0, len = jimages.size(); (k < len); k++)
		{
			ImagePlus imk = new ImagePlus(jimages.get(k));
			BufferedImage bimage = trackImage(k, binning, imk, trajectories);
			
			if(k == 0)
			{
				newStream.setVideoCompressionQuality(compression);
				newStream.setTimeScale(8);
			}
			
			try
			{
				newStream.writeFrame(bimage, 1);
				Logs.log("Writing frame " + k, 1, this);
			}
			catch (IOException e)
			{
				Logs.log("Not possible to write frame " + k, 1, this);
			}
			
			// Status bar
			int percentage = (int) (100 * ((double) k / (double) jimages.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		try
		{
			newStream.finish();
			newStream.close();
		}
		catch (IOException e)
		{
			Logs.log("Not possible to finalize movie ", 1, this);
		}
		Logs.log("Tracks movie saved in " + outMovieFile.getPath(), 1, this);
		
		// Make the JEXTrack object
		output5 = MovieWriter.makeMovieObject(outputNames[4].getName(), path);
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage trackImage(int i, int binning, ImagePlus implus, List<Trajectory> trajectories)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth() / binning));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = trajectories.size(); k < len; k++)
		{
			
			Trajectory trajK = trajectories.get(k);
			List<Point> trajAllPoints = trajK.getPointsAfter(i);
			
			for (int kk = 0, length = trajAllPoints.size(); kk < length; kk++)
			{
				Point newP = trajAllPoints.get(kk);
				int shade = 255 * (kk) / (length);
				Color c = new Color(shade, shade, 255);
				g.setColor(c);
				g.drawRect(newP.x - 2, newP.y - 2, 2 * 2, 2 * 2);
			}
			
			g.setColor(Color.YELLOW);
			int frame1 = trajK.initial();
			Point f2 = trajK.getPoint(frame1);
			Point s2 = f2;
			
			while (s2 != null)
			{
				g.fillOval(f2.x, f2.y, 3, 3);
				g.drawLine(f2.x, f2.y, s2.x, s2.y);
				f2 = s2;
				frame1 = trajK.next(frame1);
				s2 = trajK.getPoint(frame1);
				if(frame1 > i)
					s2 = null;
			}
			
		}
		
		g.dispose();
		
		return bimage;
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{
		if(interactionMode)
		{
			first = p;
			second = null;
			imagepanel.setRoi(null);
			roi = null;
		}
	}
	
	public void mouseMoved(Point p)
	{
		if(interactionMode)
		{
			second = p;
			
			int roiW = Math.abs(second.x - first.x);
			int roiH = Math.abs(second.y - first.y);
			int roiX = Math.min(second.x, first.x);
			int roiY = Math.min(second.y, first.y);
			roi = new Roi(roiX, roiY, roiW, roiH);
			imagepanel.setRoi(roi);
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
