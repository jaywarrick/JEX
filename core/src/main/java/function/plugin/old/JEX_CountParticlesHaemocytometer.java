package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import function.tracker.SimpleConvolve;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
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
public class JEX_CountParticlesHaemocytometer extends JEXCrunchable {
	
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
		String result = "Count particles in Haemocytometer";
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
		String result = "Count the number of particles in a region of the haemocytometer";
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
		String toolbox = "Fungal Culture Analysis";
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
		
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(ROI, "Counting ROI");
		
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
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(VALUE, "Number of particles");
		defaultOutputNames[1] = new TypeName(VALUE, "Suspension density");
		defaultOutputNames[2] = new TypeName(IMAGE, "Counted Image");
		
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
		
		Parameter p1 = new Parameter("Cell Radius", "Cell radius in pixels (e.g. 3 to 30)", "15");
		// Parameter p2 = new
		// Parameter("Threshold","Threshold for identifying particle locations","50.0");
		Parameter p2 = new Parameter("Auto-thresholding", "Algorithm for auto-thresholding", Parameter.DROPDOWN, new String[] { "Minimum", "MaxEntropy", "Otsu", "Moments", "Li", "Default", "Mean", "Huang", "Triangle", "MinError(I)", "Percentile" }, 8);
		
		Parameter p3 = new Parameter("Dillution factor", "Dillution factor of the spore suspension", "1000");
		Parameter p4 = new Parameter("Haemo-coefficient", "Coefficient of the haemocytometer, e.g. 10000", "10000");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
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
			return false;
		
		JEXData data2 = inputs.get("Counting ROI");
		if(data2 != null && (!data2.getTypeName().getType().equals(JEXData.ROI)))
			return false;
		
		// Run the function
		HaemocytometerCountingHelperFunction graphFunc = new HaemocytometerCountingHelperFunction(data1, data2, entry, outputNames, parameters);
		graphFunc.doit();
		
		// Set the outputs
		realOutputs.add(graphFunc.output1);
		realOutputs.add(graphFunc.output2);
		realOutputs.add(graphFunc.output3);
		
		// Return status
		return true;
	}
	
}

class HaemocytometerCountingHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	int frame = 0;
	
	// Roi interaction
	boolean interactionMode = false;
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	public JEXData output3;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int radius = 15;
	int dilFactor = 1000;
	int haemCoef = 10000;
	String threshMethod = "Default";
	
	// Variables used during the function steps
	static ImagePlus cellImage = null;
	private DimensionMap currentDim;
	private ROIPlus currentRoi;
	private ImagePlus im;
	private PointList currentPList;
	
	// Input
	JEXData imset;
	JEXData roiData;
	JEXEntry entry;
	Rectangle rectangle;
	TypeName[] outputNames;
	TreeMap<DimensionMap,String> imageMap;
	TreeMap<DimensionMap,ROIPlus> roiMap;
	
	// Class variables
	TreeMap<DimensionMap,PointList> pLists;
	TreeMap<DimensionMap,ImagePlus> convolvedImages;
	TreeMap<DimensionMap,ImagePlus> maskImages;
	
	HaemocytometerCountingHelperFunction(JEXData imset, JEXData roiData, JEXEntry entry, TypeName[] outputNames, ParameterSet parameters)
	{
		
		// Pass the variables
		this.imset = imset;
		this.roiData = roiData;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Read the inputs
		imageMap = ImageReader.readObjectToImagePathTable(imset);
		roiMap = RoiReader.readObjectToRoiMap(roiData);
		
		// Prepare the variables
		currentDim = imageMap.firstKey();
		currentRoi = roiMap.get(currentDim);
		im = new ImagePlus(imageMap.get(currentDim));
		currentPList = new PointList();
		
		// Reset the image maps
		pLists = new TreeMap<DimensionMap,PointList>();
		convolvedImages = new TreeMap<DimensionMap,ImagePlus>();
		maskImages = new TreeMap<DimensionMap,ImagePlus>();
		
		// Prepare the graphics
		interactionMode = true;
		imagepanel = new ImagePanel(this, "Count particles in field of view");
		displayImage(im, currentRoi, currentPList);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Select particle", new String[] { "Cell Radius" });
		wrap.addStep(1, "Find particles", new String[] { "Auto-thresholding" });
		wrap.addStep(2, "Calculate density", new String[] { "Dillution factor", "Haemo-coefficient", "Automatic" });
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		threshMethod = params.getValueOfParameter("Auto-thresholding");
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		dilFactor = (int) Double.parseDouble(params.getValueOfParameter("Dillution factor"));
		haemCoef = (int) Double.parseDouble(params.getValueOfParameter("Haemo-coefficient"));
	}
	
	/**
	 * Display the image, rectangle roi and list of cells
	 * 
	 * @param image
	 * @param roip
	 * @param pList
	 */
	private void displayImage(ImagePlus image, ROIPlus roip, PointList pList)
	{
		imagepanel.setImage(image);
		imagepanel.setPointList(pList);
		// imagepanel.setPointListArray(null,null);
		imagepanel.setRoi(roip.getRoi());
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public void doit()
	{
		if(auto && cellImage != null)
		{
			getParams();
			convolve();
			filterImages();
			findParticles();
			analyze();
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
		getParams();
		
		// If the cell hasn't been defined stay in step number 0
		if(cellImage == null)
		{
			atStep = 0;
		}
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		if(atStep == 0)
		{
			// Perform the convolution
			this.convolve();
			
			// Create the images and rois to be displayed
			currentRoi = roiMap.get(currentDim);
			im = convolvedImages.get(currentDim);
			
			// Set the interactionmode to true
			interactionMode = true;
			
			// Display the image
			this.displayImage(im, currentRoi, null);
		}
		else if(atStep == 1)
		{
			// Perform the convolution
			this.convolve();
			this.filterImages();
			
			// Display the image and rois
			currentRoi = roiMap.get(currentDim);
			im = convolvedImages.get(currentDim);
			
			// Set the interactionmode to true
			interactionMode = false;
			
			// Make the overlay image
			ImagePlus image1 = new ImagePlus(imageMap.get(currentDim));
			ImagePlus image2 = convolvedImages.get(currentDim);
			
			int imWidth = image2.getWidth();
			int imHeight = image2.getHeight();
			
			ColorProcessor color1 = (ColorProcessor) image1.getProcessor().duplicate().convertToRGB();
			ColorProcessor color2 = new ColorProcessor(imWidth, imHeight);
			byte[] color2RPixels = (byte[]) image2.getProcessor().convertToByte(true).getPixels();
			byte[] color2GPixels = new byte[imWidth * imHeight];
			byte[] color2BPixels = new byte[imWidth * imHeight];
			color2.setRGB(color2RPixels, color2GPixels, color2BPixels);
			color1.copyBits(color2, 0, 0, Blitter.MAX);
			ImagePlus maskImOverlay = new ImagePlus("Cell merge", color1);
			
			// Display the image
			this.displayImage(maskImOverlay, currentRoi, null);
		}
		else if(atStep == 2)
		{
			// Find the particles
			this.findParticles();
			
			// Display the image and rois
			currentRoi = roiMap.get(currentDim);
			im = new ImagePlus(imageMap.get(currentDim));
			currentPList = pLists.get(currentDim);
			
			// Set the interactionmode to true
			interactionMode = false;
			
			// Display the image
			this.displayImage(im, currentRoi, currentPList);
		}
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
		// Go to the previous image
		DimensionMap lowerKey = imageMap.lowerKey(currentDim);
		if(lowerKey != null)
			currentDim = lowerKey;
		currentRoi = roiMap.get(currentDim);
		im = new ImagePlus(imageMap.get(currentDim));
		this.displayImage(im, currentRoi, currentPList);
		
		runStep(atStep);
	}
	
	public void loopPrevious()
	{
		// Go to the previous image
		DimensionMap higherKey = imageMap.higherKey(currentDim);
		if(higherKey != null)
			currentDim = higherKey;
		currentRoi = roiMap.get(currentDim);
		im = new ImagePlus(imageMap.get(currentDim));
		this.displayImage(im, currentRoi, currentPList);
		
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
		// getParams();
		// convolve();
		// filterImages();
		// findParticles();
		analyze();
	}
	
	/**
	 * Get the neutrophil to recognize
	 */
	private void extractImage(Point p)
	{
		// Get parameters
		wrap.validateParameters();
		this.getParams();
		
		// Make a rectangle around the selected particle
		Rectangle rect = new Rectangle((int) p.getX() - radius, (int) p.getY() - radius, 2 * radius + 1, 2 * radius + 1);
		Roi rroi = new Roi(rect);
		
		// Extract the image
		ImagePlus image = new ImagePlus(imageMap.get(currentDim));
		image.setRoi(rroi);
		ImageProcessor cellImp = image.getProcessor().crop();
		cellImage = new ImagePlus("", cellImp);
		
		// Display the extracted image
		imagepanel.setRoi(rroi);
		imagepanel.repaint();
	}
	
	/**
	 * Convolve the images
	 */
	private void convolve()
	{
		// Loop through the images
		for (DimensionMap dim : imageMap.keySet())
		{
			// Get the image
			String imagePath = imageMap.get(dim);
			ImagePlus image = new ImagePlus(imagePath);
			if(image == null || image.getProcessor() == null)
				continue;
			
			// Perform the convolution
			SimpleConvolve convolver = new SimpleConvolve();
			ImagePlus result = convolver.start(image, cellImage, null);
			result = new ImagePlus("", result.getProcessor().convertToByte(true));
			
			// Put it in the convolved map
			convolvedImages.put(dim, result);
		}
	}
	
	/**
	 * Filter the images
	 */
	private void filterImages()
	{
		// Loop through the images
		for (DimensionMap dim : imageMap.keySet())
		{
			// Get the image and the roi
			ImagePlus image = convolvedImages.get(dim);
			ImageProcessor imp = image.getProcessor();
			
			// Get the histogram of the image
			int[] histogram = imp.getHistogram();
			
			// Histogram method
			// String method = "MaxEntropy";
			
			// Get the threshold level
			function.imageUtility.AutoThresholder thresholder = new function.imageUtility.AutoThresholder();
			int thresh = thresholder.getThreshold(threshMethod, histogram);
			
			// Make the threshold
			imp.setThreshold(0, thresh, ImageProcessor.BLACK_AND_WHITE_LUT);
			imp.threshold(thresh);
			imp.invertLut();
			ImagePlus result = new ImagePlus("", imp);
			
			// Put it in the convolved map
			convolvedImages.put(dim, result);
		}
	}
	
	/**
	 * Find the particles in each image of the stack
	 */
	private void findParticles()
	{
		// Loop through the images
		for (DimensionMap dim : imageMap.keySet())
		{
			// Get the image
			ImagePlus image = convolvedImages.get(dim);
			ImageProcessor imp = image.getProcessor();
			JEXWriter.saveImage(image);
			
			// Get the roi
			ROIPlus roip = roiMap.get(dim);
			if(image == null || imp == null || roip == null)
				continue;
			
			// Use ImageJ Particle Analyzer on data1
			int options = 0;
			int measure = Measurements.AREA | Measurements.CIRCULARITY | Measurements.INTEGRATED_DENSITY | Measurements.CENTROID | Measurements.ELLIPSE;
			
			// Make the particle analyzer
			ResultsTable rt = new ResultsTable();
			double minSize = 3.14 * radius * radius / 4;
			double maxSize = 3.14 * radius * radius * 4;
			minSize = 1;
			maxSize = 20000;
			double minCirc = 0.0;
			double maxCirc = 1.0;
			ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
			analyzer.analyze(image);
			
			// Get the results out
			int lastColumn = rt.getLastColumn();
			float[] xPos = new float[0];
			float[] yPos = new float[0];
			
			for (int i = 0; i < lastColumn; i++)
			{
				String cName = rt.getColumnHeading(i);
				if(cName.equals("X"))
				{
					xPos = rt.getColumn(i);
				}
				if(cName.equals("Y"))
				{
					yPos = rt.getColumn(i);
				}
			}
			
			// Make the point list
			PointList result = new PointList();
			Rectangle rect = roip.getRoi().getBounds();
			for (int i = 0; i < xPos.length; i++)
			{
				// Get the next point
				int px = (int) xPos[i];
				int py = (int) yPos[i];
				Point p = new Point(px, py);
				
				// If it is in the ROI add it to the list
				if(rect.contains(px, py))
					result.add(p);
			}
			
			// Put the point list in the result list
			pLists.put(dim, result);
		}
	}
	
	/**
	 * Analyze the results
	 */
	private void analyze()
	{
		// Prepare variables
		int NBPARTICLES = 0;
		double DENSITY = 0;
		int NBIMAGES = 0;
		TreeMap<DimensionMap,ImagePlus> analyzedImages = new TreeMap<DimensionMap,ImagePlus>();
		
		// Loop through the images
		for (DimensionMap dim : imageMap.keySet())
		{
			// Get the image
			ImagePlus image = new ImagePlus(imageMap.get(dim));
			
			// Get the point list
			PointList pList = pLists.get(dim);
			
			// If there is a error skip
			if(image == null || pList == null)
				continue;
			
			// Calculate the number
			NBPARTICLES = NBPARTICLES + pList.size();
			NBIMAGES++;
			
			// Make the analyzed image
			BufferedImage bim = plotImage(image, pList);
			ImagePlus analyzed = new ImagePlus("", bim);
			analyzedImages.put(dim, analyzed);
		}
		
		// Calculate the output values
		DENSITY = ((double) NBPARTICLES / (double) 1000000) * dilFactor * haemCoef / NBIMAGES;
		
		// Make the output datas
		output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + NBPARTICLES);
		output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + DENSITY);
		output3 = ImageWriter.makeImageStackFromImagePluses(outputNames[2].getName(), analyzedImages);
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage plotImage(ImagePlus implus, PointList points)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth()));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = points.size(); k < len; k++)
		{
			// Get point
			Point p = points.get(k);
			
			// Draw point
			g.setColor(Color.red);
			g.drawRect(p.x - radius, p.y - radius, radius * 2 + 1, radius * 2 + 1);
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
			Logs.log("Extract cell image", 1, this);
			extractImage(p);
		}
	}
	
	public void mouseMoved(Point p)
	{}
}
