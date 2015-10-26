package function.plugin.old;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;
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
public class JEX_Migration_QuantifyMigration extends JEXCrunchable {
	
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
		String result = "Quantify migration";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	public String getInfo()
	{
		String result = "Quantify features of cells migrating into a channel defined by a ROI. Default is the whole image";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
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
		TypeName[] inputNames = new TypeName[2];
		
		inputNames[0] = new TypeName(IMAGE, "Image to process");
		inputNames[1] = new TypeName(ROI, "Optional ROI");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[6];
		defaultOutputNames[0] = new TypeName(VALUE, "Number of Cells");
		defaultOutputNames[1] = new TypeName(VALUE, "Number of Cells per Area");
		defaultOutputNames[2] = new TypeName(VALUE, "Mean Distance traveled");
		defaultOutputNames[3] = new TypeName(VALUE, "Max Distance traveled");
		defaultOutputNames[4] = new TypeName(IMAGE, "Outline image");
		defaultOutputNames[5] = new TypeName(VALUE, "More information on each cells");
		
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
		Parameter p1 = new Parameter("Background radius", "Radius of the rolling ball for background substract, -1 for no filtering", "8.0");
		
		Parameter p2 = new Parameter("Min-threshold", "Minimum threshold in case auto-threshold goes too low", "6.0");
		Parameter p3 = new Parameter("Auto-thresholding", "Algorithm for auto-thresholding", Parameter.DROPDOWN, new String[] { "Minimum", "MaxEntropy", "Otsu", "Moments", "Li", "Default", "Mean", "Huang", "Triangle", "MinError(I)", "Percentile" }, 8);
		Parameter p4 = new Parameter("Threshold", "Overide auto-thresold if value is greater than 1", "-1");
		Parameter p5 = new Parameter("Watershed", "Do a watershedding to split cells close together", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
		Parameter p6 = new Parameter("Min cluster size", "Minimum cluster size", "25.0");
		Parameter p7 = new Parameter("Max cluster size", "Maximum cluster size", "200.0");
		Parameter p8 = new Parameter("Horizontal", "Migration channel is horizontal?", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p9 = new Parameter("Automatic", "Enable visual interface", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
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
		JEXData data1 = inputs.get("Image to process");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional ROI");
		
		// Run the function
		FindNeutrophilsHelperFunction graphFunc = new FindNeutrophilsHelperFunction(entry, data1, data2, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.output1;
		JEXData output2 = graphFunc.output2;
		JEXData output3 = graphFunc.output3;
		JEXData output4 = graphFunc.output4;
		JEXData output5 = graphFunc.output5;
		JEXData output6 = graphFunc.output6;
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		realOutputs.add(output4);
		realOutputs.add(output5);
		realOutputs.add(output6);
		
		// Return status
		return true;
	}
}

class FindNeutrophilsHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	ByteProcessor imp;
	ImagePlus im;
	int nb;
	float mean = 0;
	float max = 0;
	float[] xPos = new float[0];
	float[] yPos = new float[0];
	float[] perimeter = new float[0];
	float[] skew = new float[0];
	ParticleAnalyzer pA;
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	public JEXData output3;
	public JEXData output4;
	public JEXData output5;
	public JEXData output6;
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	boolean horizontal = true;
	double radius = 10;
	double minSize = 10;
	double maxSize = 100;
	double threshold = 10;
	double minthreshold = 10;
	String threshMethod = "Default";
	boolean watershed = false;
	
	// Optional ROI
	Roi roi = null;
	
	// Input
	JEXData imset;
	JEXData jroi;
	JEXEntry entry;
	List<String> images;
	TypeName[] outputNames;
	
	FindNeutrophilsHelperFunction(JEXEntry entry, JEXData imset, JEXData jroi, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.params = parameters;
		this.jroi = jroi;
		this.entry = entry;
		this.outputNames = outputNames;
		if(jroi != null)
		{
			ROIPlus roip = RoiReader.readObjectToRoi(jroi);
			this.roi = (roip == null) ? null : roip.getRoi();
		}
		
		// Get params
		getParameters();
		
		// Prepare function
		this.images = ImageReader.readObjectToImagePathList(imset);
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Quantify migration");
		imagepanel.setRoi(roi);
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Subtract background", new String[] { "Automatic", "Background radius" });
		wrap.addStep(1, "Threshold", new String[] { "Min-threshold", "Auto-thresholding", "Threshold", "Watershed" });
		wrap.addStep(2, "Extract cells", new String[] { "Min cluster size", "Max cluster size" });
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	public void getParameters()
	{
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		horizontal = Boolean.parseBoolean(params.getValueOfParameter("Horizontal"));
		radius = Double.parseDouble(params.getValueOfParameter("Background radius"));
		minSize = Double.parseDouble(params.getValueOfParameter("Min cluster size"));
		maxSize = Double.parseDouble(params.getValueOfParameter("Max cluster size"));
		threshold = Double.parseDouble(params.getValueOfParameter("Threshold"));
		minthreshold = Double.parseDouble(params.getValueOfParameter("Min-threshold"));
		threshMethod = params.getValueOfParameter("Auto-thresholding");
		watershed = Boolean.parseBoolean(params.getValueOfParameter("Watershed"));
	}
	
	private void displayImage(int index)
	{
		ImagePlus im = new ImagePlus(images.get(index));
		Logs.log("Placing in imagepanel " + images.get(index), 1, this);
		
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
		
		// Get params
		getParameters();
		
		// Get the image
		im = new ImagePlus(images.get(index));
		imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		if(atStep == 0)
		{
			subtractBackground();
			imagepanel.setRoi(null);
		}
		else if(atStep == 1)
		{
			subtractBackground();
			threshold();
		}
		else if(atStep == 2)
		{
			subtractBackground();
			threshold();
			analyzeParticles();
		}
		
		// atStep = atStep+1;
		imagepanel.setImage(new ImagePlus("", imp));
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
		index = index + 1;
		
		if(index >= images.size() - 1)
			index = images.size() - 1;
		if(index < 0)
			index = 0;
		
		runStep(index);
	}
	
	public void loopPrevious()
	{
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
		// Get params
		getParameters();
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imset);
		TreeMap<DimensionMap,String> nbOutput = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> densOutput = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> meanOutput = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> maxOutput = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> imOutput = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		int total = images.size();
		double area = 0;
		for (DimensionMap dim : images.keySet())
		{
			String path = images.get(dim);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			
			// //// Begin Actual Function
			subtractBackground();
			threshold();
			analyzeParticles();
			
			// //// Prepare outputs
			nbOutput.put(dim.copy(), "" + nb);
			meanOutput.put(dim.copy(), "" + mean);
			maxOutput.put(dim.copy(), "" + max);
			
			// Get the outline image
			ImagePlus outlineImage = plotImage(path, roi, pA.foundRois);
			String finalPath = JEXWriter.saveImage(outlineImage);
			imOutput.put(dim.copy(), finalPath);
			
			// Make location table
			String[] columns = new String[] { "XPosition", "YPosition", "Perimeter", "Skew" };
			HashMap<String,float[]> columnData = new HashMap<String,float[]>();
			columnData.put("XPosition", xPos);
			columnData.put("YPosition", yPos);
			columnData.put("Perimeter", perimeter);
			columnData.put("Skew", skew);
			output6 = ValueWriter.makeValueTableFromFloat(outputNames[5].getName(), columns, columnData);
			
			// Measure the ROI area
			if(roi != null)
			{
				java.awt.Rectangle rect = roi.getBounds();
				double width = rect.getWidth();
				double height = rect.getHeight();
				area = width * height;
			}
			else
			{
				double width = im.getWidth();
				double height = im.getHeight();
				area = width * height;
			}
			double density = 10000.0 * (double) nb / area;
			densOutput.put(dim.copy(), "" + density);
			
			// Go to next image
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
		}
		
		output1 = ValueWriter.makeValueTable(outputNames[0].getName(), nbOutput);
		output2 = ValueWriter.makeValueTable(outputNames[1].getName(), densOutput);
		output3 = ValueWriter.makeValueTable(outputNames[2].getName(), meanOutput);
		output4 = ValueWriter.makeValueTable(outputNames[3].getName(), maxOutput);
		output5 = ImageWriter.makeImageStackFromPaths(outputNames[4].getName(), imOutput);
	}
	
	private void subtractBackground()
	{
		if(radius < 0)
		{
			// Convert to Byte
			ByteProcessor bimp = (ByteProcessor) imp.convertToByte(true);
			
			BackgroundSubtracter bS = new BackgroundSubtracter();			
			bS.rollingBallBackground(bimp, -radius, false, false, false, false, true);
			
			// If the roi is set then crop the outputted image
			if(roi != null)
			{
				java.awt.Rectangle rect = roi.getBounds();
				bimp.setRoi(rect);
				bimp = (ij.process.ByteProcessor) bimp.crop();
			}
			
			// Appears this saved image was never used (other than for potentially debug)
			// FunctionUtility.imSave(imp, "./quantifyMigration1.tif");
			// JEXWriter.saveImage(bimp);;
			
			// Place the byte processor in the image to track
			imp = bimp;
		}
		else
		{
			BackgroundSubtracter bS = new BackgroundSubtracter();	
			// set lightBackground to true in this case.
			bS.rollingBallBackground(im.getProcessor(), -radius, false, true, false, false, true);
			
			if(roi != null)
			{
				java.awt.Rectangle rect = roi.getBounds();
				imp.setRoi(rect);
				imp = (ij.process.ByteProcessor) imp.crop();
			}
			
			// Appears this saved image was never used (other than for potentially debug)
			// FunctionUtility.imSave(imp, "./quantifyMigration1.tif");
			// JEXWriter.saveImage(imp);
		}
	}
	
	private void threshold()
	{
		// Invert the image to get a white cell over a dark background
		imp.invert();
		
		// Get the thresholing method
		// If the thresold value is more than 1 then use the manually entered
		// value
		if(threshold < 0)
		{
			Logs.log("Thresholding using " + threshMethod + " algorithm", 1, this);
			int[] histogram = imp.getHistogram();
			function.imageUtility.AutoThresholder thresholder = new function.imageUtility.AutoThresholder();
			threshold = thresholder.getThreshold(threshMethod, histogram);
			threshold = (threshold < minthreshold) ? minthreshold : threshold;
		}
		else
		{
			Logs.log("Thresholding using manually entered value " + threshold, 1, this);
		}
		
		// Do the threshold
		imp.threshold((int) threshold);
		
		// Invert the LUT for display and EDM analysis
		imp.invertLut();
		
		// Watershed
		// Boolean watershed =
		// Boolean.parseBoolean(params.getValueOfParameter("Watershed"));
		if(watershed)
		{
			EDM edm = new EDM();
			edm.toWatershed(imp);
		}
		
		JEXWriter.saveImage(imp);
	}
	
	private void analyzeParticles()
	{
		ResultsTable rT = new ResultsTable();
		int options = ParticleAnalyzer.SHOW_OUTLINES;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY | ParticleAnalyzer.PERIMETER | ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.SKEWNESS;
		pA = new ParticleAnalyzer(options, measure, rT, minSize, maxSize);
		pA.setHideOutputImage(true);
		pA.analyze(new ImagePlus("", imp));
		
		if(rT.getColumn(0) == null)
		{
			nb = 0;
			mean = 0;
			max = 0;
			xPos = new float[0];
			yPos = new float[0];
			perimeter = new float[0];
			skew = new float[0];
			imp = new ByteProcessor(imp.getWidth(), imp.getHeight());
		}
		else
		{
			nb = rT.getColumn(0).length;
			int lastColumn = rT.getLastColumn();
			mean = 0;
			max = 0;
			xPos = new float[0];
			yPos = new float[0];
			perimeter = new float[0];
			skew = new float[0];
			for (int i = 0; i < lastColumn; i++)
			{
				String cName = rT.getColumnHeading(i);
				if(cName.equals("XM"))
				{
					xPos = rT.getColumn(i);
					if(horizontal)
					{
						for (float f : xPos)
						{
							max = Math.max(max, imp.getWidth() - (int) f);
							mean = mean + f / xPos.length;
						}
					}
				}
				if(cName.equals("YM"))
				{
					yPos = rT.getColumn(i);
					if(!horizontal)
					{
						for (float f : yPos)
						{
							max = Math.max(max, imp.getHeight() - (int) f);
							mean = mean + f / yPos.length;
						}
					}
				}
				if(cName.equals("Perim."))
				{
					perimeter = rT.getColumn(i);
				}
				if(cName.equals("Skew"))
				{
					skew = rT.getColumn(i);
				}
			}
			ImagePlus outIm = pA.getOutputImage();
			imp = (ByteProcessor) outIm.getProcessor().convertToByte(true);
		}
		
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private ImagePlus plotImage(String brightFieldPath, Roi imageRoi, List<Roi> rois)
	{
		ImagePlus brightFieldImage = new ImagePlus(brightFieldPath);
		ColorProcessor color = (ColorProcessor) brightFieldImage.getProcessor().convertToRGB();
		BufferedImage cimp = color.getBufferedImage();
		Graphics g = cimp.getGraphics();
		
		// Random color generator
		Random random = new Random();
		
		// Find offset
		int offX = 0;
		int offY = 0;
		if(imageRoi != null)
		{
			java.awt.Rectangle rect = imageRoi.getBounds();
			offX = (int) rect.getX();
			offY = (int) rect.getY();
		}
		
		// Draw the cell rois
		for (Roi roi : rois)
		{
			// Generate a random color
			Color c = Color.getHSBColor(random.nextFloat(), 0.85F, 1.0F);
			g.setColor(c);
			
			// Draw the cell roi
			Polygon poly = roi.getPolygon();
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;
			Point first = null;
			Point second = null;
			for (int i = 0; i < xpos.length; i++)
			{
				int x = xpos[i] + offX;
				int y = ypos[i] + offY;
				first = second;
				second = new Point(x, y);
				
				// Draw outline
				// g.fillRect(x-1, y-1, 2, 2);
				g.fillRect(x, y, 1, 1);
				if(first != null && second != null)
				{
					g.drawLine((int) first.getX(), (int) first.getY(), (int) second.getX(), (int) second.getY());
					// g.drawLine((int)first.getX()-1, (int)first.getY()-1,
					// (int)second.getX()-1, (int)second.getY()-1);
				}
			}
		}
		
		ImagePlus result = new ImagePlus("brightField merge", cimp);
		return result;
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
