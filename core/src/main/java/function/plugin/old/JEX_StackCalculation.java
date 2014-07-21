package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

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
public class JEX_StackCalculation extends JEXCrunchable {
	
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
		String result = "Stack operator";
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
		String result = "Perform operations on stacks";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE, "Stack 1");
		inputNames[1] = new TypeName(IMAGE, "Optional Stack 2");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Output stack");
		
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
		Parameter p1 = new Parameter("Type", "Type of calculation to perform", Parameter.DROPDOWN, new String[] { "Average", "Add", "Subtract" }, 0);
		Parameter p6 = new Parameter("Output Bit Depth", "Depth of the outputted image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p1);
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
		JEXData data1 = inputs.get("Stack 1");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Optional Stack 2");
		
		// Run the function
		StackCalculationHelper graphFunc = new StackCalculationHelper(entry, data1, data2, outputNames, parameters);
		JEXData output1 = graphFunc.output1;
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
}

class StackCalculationHelper {
	
	// Utilities
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	int index = 0;
	int atStep = 0;
	int frame = 0;
	
	// Outputs
	public JEXData output1;
	
	// Parameters
	ParameterSet params;
	
	// Variables used during the function steps
	private ImagePlus im;
	private List<String> jimages;
	
	// Input
	JEXData imset;
	JEXData imset2;
	JEXEntry entry;
	TypeName[] outputNames;
	
	StackCalculationHelper(JEXEntry entry, JEXData imset, JEXData imset2, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.imset = imset;
		this.imset2 = imset2;
		this.params = parameters;
		this.entry = entry;
		this.outputNames = outputNames;
		
		// Prepare function
		jimages = ImageReader.readObjectToImagePathList(imset);
		
		// What is the calculation to be performed
		String calculation = params.getValueOfParameter("Type");
		if(calculation == null || calculation.equals(""))
			return;
		else if(calculation.equals("Average"))
			average();
		else if(calculation.equals("Add"))
			add();
		else if(calculation.equals("Subtract"))
			subtract();
	}
	
	/**
	 * calculate an average statck image
	 */
	private void average()
	{
		// Run the function
		int len = jimages.size();
		int i = 0;
		
		// prepare the image
		ImagePlus averageImage = null;
		JEXStatics.statusBar.setProgressPercentage(0);
		
		for (String impath : jimages)
		{
			
			// Get the image
			ImagePlus im = new ImagePlus(impath);
			ImageProcessor imp = im.getProcessor();
			FloatProcessor fmp = (FloatProcessor) imp.convertToFloat();
			
			// if this is the first image prepare the output
			if(averageImage == null)
			{
				ImageProcessor avimp = fmp.createProcessor(im.getWidth(), im.getHeight());
				averageImage = new ImagePlus("", avimp);
			}
			
			// rescale the image
			fmp.multiply((double) 1 / jimages.size());
			
			// add the image to the mean
			ImageProcessor avimp = averageImage.getProcessor();
			avimp.copyBits(fmp, 0, 0, Blitter.ADD);
			
			// update status bar
			Logs.log("Added image " + i + " of " + len + ".", 1, this);
			
			// Status bar
			int percentage = (int) (100 * ((double) i / (double) len));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			i++;
		}
		
		// String localDir = JEXWriter.getEntryFolder(entry);
		// String newFileName = FunctionUtility.getNextName(localDir,
		// "AverageStack", "Av");
		// String path = localDir + File.separator + newFileName;
		// FunctionUtility.imSave(averageImage, path);
		
		String path = JEXWriter.saveImage(averageImage);
		Logs.log("Saved outputted image.", 1, this);
		
		output1 = ImageWriter.makeImageObject(outputNames[0].getName(), path);
	}
	
	/**
	 * add two stacks
	 */
	private void add()
	{
		// get the tree map of the first imageset
		TreeMap<DimensionMap,String> map1 = new TreeMap<DimensionMap,String>();
		if(imset != null)
			map1 = ImageReader.readObjectToImagePathTable(imset);
		
		// get the tree map of the second imageset
		TreeMap<DimensionMap,String> map2 = new TreeMap<DimensionMap,String>();
		if(imset2 != null)
			map2 = ImageReader.readObjectToImagePathTable(imset2);
		
		// loop
		int len = map1.size();
		int i = 0;
		
		TreeMap<DimensionMap,String> output = new TreeMap<DimensionMap,String>();
		for (DimensionMap dim : map1.keySet())
		{
			
			// Get image 1
			String path1 = map1.get(dim);
			ImagePlus im1 = new ImagePlus(path1);
			if(im1 == null || im1.getProcessor() == null)
				continue;
			ImageProcessor imp1 = im1.getProcessor();
			FloatProcessor fmp1 = (FloatProcessor) imp1.convertToFloat();
			fmp1.multiply(0.5);
			
			// Get image 2
			String path2 = map2.get(dim);
			ImagePlus im2 = new ImagePlus(path2);
			if(im2 == null || im2.getProcessor() == null)
				continue;
			ImageProcessor imp2 = im2.getProcessor();
			FloatProcessor fmp2 = (FloatProcessor) imp2.convertToFloat();
			fmp2.multiply(0.5);
			
			// Make new image
			ImageProcessor avimp = fmp1.createProcessor(im.getWidth(), im.getHeight());
			avimp.copyBits(fmp1, 0, 0, Blitter.ADD);
			avimp.copyBits(fmp2, 0, 0, Blitter.ADD);
			
			// Save image
			// File file1 = new File(path1);
			// String localDir = JEXWriter.getEntryFolder(entry);
			// String baseName =
			// file1.getName().substring(0,file1.getName().length()-4);
			// String newFileName = FunctionUtility.getNextName(localDir,
			// baseName, "Av");
			// String path = localDir + File.separator + newFileName;
			// FunctionUtility.imSave(new ImagePlus("",avimp), path);
			
			String path = JEXWriter.saveImage(avimp);
			
			// Put in jexdata for output
			output.put(dim.copy(), path);
			
			// Update progress bar
			Logs.log("Added image " + i + " of " + len + ".", 1, this);
			
			// Status bar
			int percentage = (int) (100 * ((double) i / (double) len));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			i++;
		}
		
		output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), output);
	}
	
	/**
	 * subtract two stacks
	 */
	private void subtract()
	{
		// get the tree map of the first imageset
		TreeMap<DimensionMap,String> map1 = new TreeMap<DimensionMap,String>();
		if(imset != null)
			map1 = ImageReader.readObjectToImagePathTable(imset);
		
		// get the tree map of the second imageset
		TreeMap<DimensionMap,String> map2 = new TreeMap<DimensionMap,String>();
		if(imset2 != null)
			map2 = ImageReader.readObjectToImagePathTable(imset2);
		
		// loop
		int len = map1.size();
		int i = 0;
		
		TreeMap<DimensionMap,String> output = new TreeMap<DimensionMap,String>();
		for (DimensionMap dim : map1.keySet())
		{
			
			// Get image 1
			String path1 = map1.get(dim);
			ImagePlus im1 = new ImagePlus(path1);
			if(im1 == null || im1.getProcessor() == null)
				continue;
			ImageProcessor imp1 = im1.getProcessor();
			FloatProcessor fmp1 = (FloatProcessor) imp1.convertToFloat();
			fmp1.multiply(0.5);
			
			// Get image 2
			String path2 = map2.get(dim);
			ImagePlus im2 = new ImagePlus(path2);
			if(im2 == null || im2.getProcessor() == null)
				continue;
			ImageProcessor imp2 = im2.getProcessor();
			FloatProcessor fmp2 = (FloatProcessor) imp2.convertToFloat();
			fmp2.multiply(0.5);
			
			// Make new image
			ImageProcessor avimp = fmp1.createProcessor(im.getWidth(), im.getHeight());
			avimp.copyBits(fmp1, 0, 0, Blitter.ADD);
			avimp.copyBits(fmp2, 0, 0, Blitter.SUBTRACT);
			
			// Save image
			// File file1 = new File(path1);
			// String localDir = JEXWriter.getEntryFolder(entry);
			// String baseName =
			// file1.getName().substring(0,file1.getName().length()-4);
			// String newFileName = FunctionUtility.getNextName(localDir,
			// baseName, "Av");
			// String path = localDir + File.separator + newFileName;
			// FunctionUtility.imSave(new ImagePlus("",avimp), path);
			
			String path = JEXWriter.saveImage(avimp);
			
			// Put in jexdata for output
			output.put(dim.copy(), path);
			
			// Update progress bar
			Logs.log("Added image " + i + " of " + len + ".", 1, this);
			
			// Status bar
			int percentage = (int) (100 * ((double) i / (double) len));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			i++;
		}
		
		output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), output);
	}
	
}
