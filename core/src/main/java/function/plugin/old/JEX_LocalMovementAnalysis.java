package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import image.roi.Vect;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
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
public class JEX_LocalMovementAnalysis extends JEXCrunchable {
	
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
		String result = "Local movement";
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
		String result = "Analyze the local movement in an image";
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
		String toolbox = "Stack processing";
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
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(IMAGE, "Displacement field");
		defaultOutputNames[1] = new TypeName(VALUE, "Displacement matrix");
		
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
		Parameter p2 = new Parameter("Grid size", "Size of the subdivisions", "40.0");
		Parameter p3 = new Parameter("Search radius", "Search radius", "20.0");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
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
		JEXData data = inputs.get("Timelapse");
		if(!data.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		DeformationCalculationHelperFunction graphFunc = new DeformationCalculationHelperFunction(entry, data, outputNames, parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.output1;
		JEXData output2 = graphFunc.output2;
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
	
}

class DeformationCalculationHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
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
	
	// Parameters
	ParameterSet params;
	boolean auto = false;
	int bin = 2;
	int grid = 10;
	int radius = 10;
	
	// Variables used during the function steps
	private ByteProcessor imp;
	private ImagePlus im;
	
	// Input
	JEXData imset;
	JEXEntry entry;
	TypeName[] outputNames;
	List<String> jimages;
	List<DimensionMap> dimensions;
	List<Integer> xPos;
	List<Integer> yPos;
	HashMap<Integer,HashMap<Integer,HashMap<Integer,Vect>>> displacements;
	String csvArray;
	
	DeformationCalculationHelperFunction(JEXEntry entry, JEXData imset, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// //// Get params
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		grid = (int) Double.parseDouble(params.getValueOfParameter("Grid size"));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Search radius"));
		
		// Prepare function
		TreeMap<DimensionMap,String> map = ImageReader.readObjectToImagePathTable(imset);
		jimages = new ArrayList<String>(0);
		dimensions = new ArrayList<DimensionMap>(0);
		for (DimensionMap dim : map.keySet())
		{
			jimages.add(map.get(dim));
			dimensions.add(dim.copy());
		}
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this, "Analyze image deformation");
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Make grid", new String[] { "Automatic", "Binning", "Grid size" });
		wrap.addStep(1, "Find deformation", new String[] { "Search radius" });
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
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		grid = (int) Double.parseDouble(params.getValueOfParameter("Grid size"));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Search radius"));
		
		// /// Run step index
		Logs.log("Running step " + atStep, 1, this);
		if(atStep == 0)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			binImage();
			drawGrid();
		}
		else if(atStep == 1)
		{
			im = new ImagePlus(jimages.get(index));
			imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			imagepanel.setPointListArray(null, null);
			imagepanel.setRoi(null);
			
			displacements = new HashMap<Integer,HashMap<Integer,HashMap<Integer,Vect>>>();
			// binImage();
			// makeGrid();
			findDeformation();
		}
		
		imagepanel.setImage(new ImagePlus("", imp));
	}
	
	public void runNext()
	{
		atStep = atStep + 1;
		if(atStep > 1)
			atStep = 1;
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
		auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		grid = (int) Double.parseDouble(params.getValueOfParameter("Grid size"));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Search radius"));
		
		findDeformationForStack();
	}
	
	private void binImage()
	{
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
	}
	
	private void makeGrid()
	{
		imp.setColor(Color.yellow);
		grid = (grid < 1) ? 1 : grid;
		
		// Draw vertical lines
		int w = 0;
		xPos = new ArrayList<Integer>(0);
		while (w < imp.getWidth())
		{
			// imp.drawLine(w, 0, w, imp.getHeight());
			if(w + grid < imp.getWidth())
				xPos.add(w);
			w = w + grid;
		}
		
		// Draw horizontal lines
		int h = 0;
		yPos = new ArrayList<Integer>(0);
		while (h < imp.getHeight())
		{
			// yPos.add(h);
			// imp.drawLine(0, h, imp.getWidth(), h);
			if(h + grid < imp.getHeight())
				yPos.add(h);
			h = h + grid;
		}
		
		Logs.log("X and Y positions found", 1, this);
	}
	
	private void drawGrid()
	{
		imp.setColor(Color.yellow);
		grid = (grid < 1) ? 1 : grid;
		
		// Draw vertical lines
		int w = 0;
		xPos = new ArrayList<Integer>(0);
		while (w < imp.getWidth())
		{
			imp.drawLine(w, 0, w, imp.getHeight());
			if(w + grid < imp.getWidth())
				xPos.add(w);
			w = w + grid;
		}
		
		// Draw horizontal lines
		int h = 0;
		yPos = new ArrayList<Integer>(0);
		while (h < imp.getHeight())
		{
			// yPos.add(h);
			imp.drawLine(0, h, imp.getWidth(), h);
			if(h + grid < imp.getHeight())
				yPos.add(h);
			h = h + grid;
		}
		
		Logs.log("X and Y positions found", 1, this);
	}
	
	private void findDeformation()
	{
		// Get the current image
		im = new ImagePlus(jimages.get(index));
		;
		imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
		makeGrid();
		
		// Get the next image
		// int nextIndex = index + 1;
		// if (index >= jimages.size()-1) nextIndex = index;
		ImagePlus nextIm = new ImagePlus(jimages.get(index));
		ByteProcessor nextImp = (ByteProcessor) nextIm.getProcessor().convertToByte(true);
		int nextWidth = (int) ((double) nextImp.getWidth() / bin);
		nextImp = (ByteProcessor) nextImp.resize(nextWidth);
		float[][] imageMatrix = nextImp.getFloatArray();
		
		// Find the deformation for each location
		String csvArrayX = ",";
		String csvArrayY = ",";
		for (int x : xPos)
			csvArrayX = csvArrayX + x + ",";
		for (int x : xPos)
			csvArrayY = csvArrayY + x + ",";
		csvArrayX = csvArrayX + "\n";
		csvArrayY = csvArrayY + "\n";
		for (int y : yPos)
		{
			csvArrayX = csvArrayX + y + ",";
			csvArrayY = csvArrayY + y + ",";
			for (int x : xPos)
			{
				// Logs.log("Finding deformation at position "+x+" - "+y,
				// 1, this);
				
				// Get the sub image
				ByteProcessor gridImage = (ByteProcessor) imp.duplicate();
				gridImage.setRoi(new Rectangle(x, y, grid, grid));
				gridImage = (ByteProcessor) gridImage.crop();
				float[][] gridMatrix = gridImage.getFloatArray();
				
				// Convolve around the serach area
				float[][] scoreMatrix = findScore(imageMatrix, gridMatrix, x, y);
				// printArray(scoreMatrix);
				Vect maxScore = findMax(scoreMatrix);
				// Logs.log("   Displacement minimum for vector "+maxScore.dX+" - "+maxScore.dY,
				// 1, this);
				
				// Find the vector
				HashMap<Integer,HashMap<Integer,Vect>> vectXYpos = displacements.get(index);
				if(vectXYpos == null)
				{
					vectXYpos = new HashMap<Integer,HashMap<Integer,Vect>>();
					displacements.put(index, vectXYpos);
				}
				
				HashMap<Integer,Vect> vectYpos = vectXYpos.get(x + grid / 2);
				if(vectYpos == null)
				{
					vectYpos = new HashMap<Integer,Vect>();
					vectXYpos.put(x + grid / 2, vectYpos);
				}
				
				vectYpos.put(y, maxScore);
				
				// Draw vector
				imp.setColor(Color.yellow);
				imp.fillOval(x + grid / 2 - 1, y + grid / 2 - 1, 3, 3);
				imp.drawLine(x + grid / 2, y + grid / 2, x + grid / 2 + (int) maxScore.dX, y + grid / 2 + (int) maxScore.dY);
				
				// Make the JEXValue
				csvArrayX = csvArrayX + maxScore.dX + ",";
				csvArrayY = csvArrayY + maxScore.dY + ",";
			}
			csvArrayX = csvArrayX + "\n";
			csvArrayY = csvArrayY + "\n";
		}
		
		csvArray = csvArrayX + "\n" + "\n" + csvArrayY;
	}
	
	private void findDeformationForStack()
	{
		displacements = new HashMap<Integer,HashMap<Integer,HashMap<Integer,Vect>>>();
		TreeMap<DimensionMap,String> outputMap1 = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> outputMap2 = new TreeMap<DimensionMap,String>();
		
		JEXStatics.statusBar.setProgressPercentage(0);
		for (int i = 0, len = jimages.size(); i < len; i++)
		{
			index = i;
			findDeformation();
			
			// save the movement file
			// String impath = jimages.get(index);
			DimensionMap map = dimensions.get(index);
			// File f = new File(impath);
			// String localDir = JEXWriter.getEntryFolder(entry);
			// String newFileName = FunctionUtility.getNextName(localDir,
			// f.getName(), "Disp");
			// String path = localDir + File.separator + newFileName;
			// FunctionUtility.imSave(imp, path);
			
			String path = JEXWriter.saveImage(imp);
			outputMap1.put(map, path);
			
			// save the displacement array
			// String newFileName2 = FunctionUtility.getNextName(localDir,
			// "Matrix.csv", "Disp");
			// String path2 = localDir + File.separator + newFileName2;
			// Logs.log("Printing csv array to file",1,this);
			// Utility.printToFile(csvArray, path2);
			
			Logs.log("Printing csv array to file", 1, this);
			String path2 = JEXWriter.saveText(csvArray, "csv");
			outputMap2.put(map, path2);
			
			// Status bar
			int percentage = (int) (100 * ((double) i / (double) jimages.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputMap1);
		output1.setDataObjectInfo("Local displacement Image stack");
		
		output2 = FileWriter.makeFileObject(outputNames[1].getName(), null, outputMap2);
		output2.setDataObjectInfo("Local displacement Value stack");
	}
	
	private float[][] findScore(float[][] image, float[][] convolveImage, int x, int y)
	{
		float[][] result = new float[2 * radius + 1][2 * radius + 1];
		float mean = 0;
		for (int i = 0, leni = convolveImage.length; i < leni; i++)
		{
			for (int j = 0, lenj = convolveImage[0].length; j < lenj; j++)
			{
				mean = mean + convolveImage[i][j];
			}
		}
		mean = mean / (convolveImage.length * convolveImage.length);
		
		for (int i = 0; i < 2 * radius + 1; i++)
		{
			for (int j = 0; j < 2 * radius + 1; j++)
			{
				int posX = x - radius + i;
				int posY = y - radius + j;
				result[i][j] = scoreConvolution(image, convolveImage, posX, posY, mean);
			}
		}
		
		return result;
	}
	
	public float scoreConvolution(float[][] image, float[][] convolveImage, int posX, int posY, float mean)
	{
		float average = 0;
		for (int i = 0, leni = convolveImage.length; i < leni; i++)
		{
			for (int j = 0, lenj = convolveImage[0].length; j < lenj; j++)
			{
				int x = posX + i;
				int y = posY + j;
				
				if(x < 0)
					continue;
				if(x >= image.length)
					continue;
				if(y < 0)
					continue;
				if(y >= image[0].length)
					continue;
				
				average = average + image[x][y];
			}
		}
		average = average / (convolveImage.length * convolveImage.length);
		
		// convolve the images while correcting for average intesity differences
		float score = 0;
		for (int i = 0, leni = convolveImage.length; i < leni; i++)
		{
			for (int j = 0, lenj = convolveImage[0].length; j < lenj; j++)
			{
				int x = posX + i;
				int y = posY + j;
				
				if(x < 0)
					continue;
				if(x >= image.length)
					continue;
				if(y < 0)
					continue;
				if(y >= image[0].length)
					continue;
				
				// Logs.log("ooo Getting score at position "+x+" - "+y,
				// 1, this);
				score = score + (image[x][y] - average) * (convolveImage[i][j] - mean);
			}
		}
		
		score = (score < 0) ? 0 : score;
		return score;
	}
	
	private Vect findMax(float[][] scoreMatrix)
	{
		Vect result = null;
		float currentMax = -1;
		
		for (int i = 0, leni = scoreMatrix.length; i < leni; i++)
		{
			for (int j = 0, lenj = scoreMatrix[0].length; j < lenj; j++)
			{
				Vect v = new Vect(i - radius, j - radius);
				if(scoreMatrix[i][j] > currentMax)
				{
					currentMax = scoreMatrix[i][j];
					result = v;
				}
				if(scoreMatrix[i][j] == currentMax && v.norm() < result.norm())
				{
					currentMax = scoreMatrix[i][j];
					result = v;
				}
			}
		}
		
		return result;
	}
	
	public void printArray(float[][] array)
	{
		System.out.println("***** Array print *****");
		for (int k = 0, lenk = array.length; (k < lenk); k++)
		{
			String line = " - ";
			for (int j = 0, lenj = array[0].length; (j < lenj); j++)
			{
				line = line + " " + array[k][j];
			}
			System.out.println(line);
		}
	}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{}
	
	public void mouseMoved(Point p)
	{}
	
}
