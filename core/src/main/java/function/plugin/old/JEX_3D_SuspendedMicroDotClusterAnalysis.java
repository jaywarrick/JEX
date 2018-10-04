package function.plugin.old;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;

/**
 * This is a JEXperiment function template
 * To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder
 * 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types
 * The specific API for these can be found in the main JEXperiment folder.
 * These API provide methods to retrieve data from these objects,
 * create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 *
 */
/**
 * @author edmyoung
 * 
 */
public class JEX_3D_SuspendedMicroDotClusterAnalysis extends JEXCrunchable {
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	public String getName()
	{
		String result = "Cluster Microdot Analysis.";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Find clusters in a microdot and analyse the features";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
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
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
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
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[1];
		
		inputNames[0] = new TypeName(IMAGE, "Fluorescent Image");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[5];
		defaultOutputNames[0] = new TypeName(VALUE, "Total Cluster Number");
		defaultOutputNames[1] = new TypeName(VALUE, "Total Cluster Area");
		defaultOutputNames[2] = new TypeName(VALUE, "Mean Cluster Area");
		defaultOutputNames[3] = new TypeName(VALUE, "More Info");
		defaultOutputNames[4] = new TypeName(IMAGE, "Cluster Mask");
		
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
	public ParameterSet requiredParameters()
	{
		Parameter p1 = new Parameter("Radius", "Average diameter of a cell in pixels", "15");
		// Parameter p2 = new
		// Parameter("Method","Method of automatically choosing the threshold.",FormLine.DROPDOWN,
		// new String[]{"Huang", "Intermodes", "IsoData", "Li", "MaxEntropy",
		// "Mean", "Minerror", "Minimum", "Moments", "Otsu", "Percentile",
		// "Renyientropy", "Shanbhag", "Triangle", "Yen"},13);
		Parameter p3 = new Parameter("Threshold", "Minimum threshold level.", "100");
		// Parameter p4 = new
		// Parameter("Watershed","Watershed the image for cell identification?.",FormLine.DROPDOWN,
		// new String[]{"true", "false"},1);
		Parameter p5 = new Parameter("Erosions", "Number of erosions", "1");
		Parameter p6 = new Parameter("Dilations", "Number of dilations", "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
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
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData data1 = inputs.get("Fluorescent Image");
		if(!data1.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		
		// Run the function
		FluorescentClusterFinder func = new FluorescentClusterFinder(entry, data1, outputNames, parameters);
		JEXData output1 = func.output1;
		JEXData output2 = func.output2;
		JEXData output3 = func.output3;
		JEXData output4 = func.output4;
		JEXData output5 = func.output5;
		
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

class FluorescentClusterFinder {
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	public JEXData output3;
	public JEXData output4;
	public JEXData output5;
	
	// Parameters
	ParameterSet params;
	double radius = 10;
	String threshMethod = "Default";
	int minThresh = -1;
	boolean watershed = true;
	int erosions = 1;
	int dilations = 1;
	
	// Input
	JEXData fluo;
	JEXEntry entry;
	TypeName[] outputNames;
	
	// Data
	ImagePlus fluoIm;
	ImagePlus fluoMask;
	ParticleAnalyzer pA;
	List<Roi> fluoRois;
	List<Point> fluoCenters;
	float[] clusterAreas;
	
	FluorescentClusterFinder(JEXEntry entry, JEXData fluo, TypeName[] outputNames, ParameterSet parameters)
	{
		
		this.fluo = fluo;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// Get params
		getParameters();
		
		// Prepare function
		fluoIm = ImageReader.readObjectToImagePlus(fluo);
		
		// Prepare the graphics
		doit();
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParameters()
	{
		radius = Double.parseDouble(params.getValueOfParameter("Radius"));
		threshMethod = params.getValueOfParameter("Method");
		watershed = Boolean.parseBoolean(params.getValueOfParameter("Watershed"));
		erosions = Integer.parseInt(params.getValueOfParameter("Erosions"));
		dilations = Integer.parseInt(params.getValueOfParameter("Dilations"));
		minThresh = Integer.parseInt(params.getValueOfParameter("Threshold"));
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		// Process the blue image
		Logs.log("Processing the Image", 1, this);
		
		// Get the imageprocessor
		ImageProcessor fluoImp = fluoIm.getProcessor();
		fluoImp = fluoImp.convertToByte(true);
		
		// Subtract background
		subtractBackground(fluoImp);
		
		// Threshold
		threshold(fluoImp, -1);
		
		// Dilate
		for (int i = 0; i < dilations; i++)
		{
			fluoImp.dilate();
		}
		
		// Erode
		for (int i = 0; i < erosions; i++)
		{
			fluoImp.erode();
		}
		
		// Watershed
		if(watershed)
		{
			EDM edm = new EDM();
			edm.toWatershed(fluoImp);
		}
		
		// Particle analyzer
		fluoImp.invertLut();
		fluoRois = new ArrayList<Roi>(0);
		fluoCenters = new ArrayList<Point>(0);
		fluoMask = analyzeParticles(fluoImp, fluoRois, fluoCenters);
		
		this.finishIT();
	}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT()
	{
		double totalClusterNb = fluoRois.size();
		double totalClusterSize = 0;
		for (Roi r : fluoRois)
		{
			fluoIm.setRoi(r);
			int mOptions = ParticleAnalyzer.AREA;
			ImageStatistics stats = fluoIm.getStatistics(mOptions);
			
			double area = stats.area;
			totalClusterSize = totalClusterSize + area;
		}
		double meanClusterSize = totalClusterSize / totalClusterNb;
		
		output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + totalClusterNb);
		output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + totalClusterSize);
		output3 = ValueWriter.makeValueObject(outputNames[2].getName(), "" + meanClusterSize);
		output4 = ValueWriter.makeValueObject(outputNames[3].getName(), "" + meanClusterSize);
		output5 = ImageWriter.makeImageObject(outputNames[4].getName(), fluoMask);
	}
	
	/**
	 * Perform a subtract background
	 * 
	 * @param imp
	 */
	private void subtractBackground(ImageProcessor imp)
	{
		BackgroundSubtracter bS = new BackgroundSubtracter();
		bS.rollingBallBackground(imp, 3 * radius, false, false, false, false, true);
	}
	
	/**
	 * Perform a threshold
	 * 
	 * @param imp
	 */
	private void threshold(ImageProcessor imp, int thresh)
	{
		// Get the thresholing method
		int[] histogram = imp.getHistogram();
		function.imageUtility.AutoThresholder thresholder = new function.imageUtility.AutoThresholder();
		
		// Get the autothreshold value
		int threshold = thresholder.getThreshold(threshMethod, histogram);
		if(thresh > -1)
		{
			threshold = (threshold > thresh) ? threshold : thresh;
		}
		
		// Do the thresholding
		if(thresh == -1)
		{
			imp.threshold((int) threshold);
			Logs.log("Using autothreshold = " + threshold, 1, this);
		}
		else
		{
			imp.threshold((int) threshold);
			Logs.log("Using set threshold = " + thresh + " instead of auto = " + threshold, 1, this);
		}
	}
	
	/**
	 * Analyze the particles and return a mask image
	 * 
	 * @param im
	 * @return
	 */
	private ImagePlus analyzeParticles(ImageProcessor imp, List<Roi> rois, List<Point> centers)
	{
		ResultsTable rT = new ResultsTable();
		int options = ParticleAnalyzer.SHOW_MASKS;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.PERIMETER | ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.SKEWNESS;
		int minSize = (int) (radius * radius / 4);
		int maxSize = (int) (radius * radius * 64);
		pA = new ParticleAnalyzer(options, measure, rT, (double) minSize, (double) maxSize, 0.0, 1.0);
		pA.setHideOutputImage(true);
		pA.analyze(new ImagePlus("", imp));
		
		// Get the outputted image
		ImagePlus outIm = pA.getOutputImage();
		if(outIm == null)
		{
			Logs.log("Analyzer output image is null... creating an empty black one", 1, this);
			outIm = new ImagePlus("", imp.createProcessor(imp.getWidth(), imp.getHeight()));
		}
		
		// Get the list of rois
		for (Roi r : pA.foundRois)
		{
			rois.add(r);
		}
		
		// Get the centers of each Roi
		float[] xPos = new float[0];
		float[] yPos = new float[0];
		if(rT.getColumn(0) == null)
		{
			xPos = new float[0];
			yPos = new float[0];
		}
		else
		{
			int lastColumn = rT.getLastColumn();
			xPos = new float[0];
			yPos = new float[0];
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
			}
		}
		for (int i = 0; i < xPos.length; i++)
		{
			Point p = new Point((int) xPos[i], (int) yPos[i]);
			centers.add(p);
		}
		
		return outIm;
	}
	
}
