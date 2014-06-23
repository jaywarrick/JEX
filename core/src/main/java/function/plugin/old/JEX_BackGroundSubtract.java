package function.plugin.old;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.imageUtility.jBackgroundSubtracter;

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
public class JEX_BackGroundSubtract extends JEXCrunchable {
	
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
		String result = "Background Substract";
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
		String result = "Subtract background of image.";
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
		String toolbox = "Image processing";
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
		inputNames[0] = new TypeName(IMAGE, "Image to process");
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
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Background subtracted");
		
		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
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
		Parameter p1 = new Parameter("Radius", "Kernal radius in pixels (e.g. 3.8)", "50.0");
		Parameter p2 = new Parameter("Inverted", "Generally false but true if imaging absorbance", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p3 = new Parameter("Extract background", "Make background image", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p4 = new Parameter("Paraboloid", "Use a sliding paraboloid", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p5 = new Parameter("Presmoothing", "Do a presmoothing", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p6 = new Parameter("Output Bit Depth", "Depth of the outputted image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 1);
		Parameter p7 = new Parameter("Normalize", "Normalize image on save", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		
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
		JEXData data = inputs.get("Image to process");
		if(!data.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// //// Get params
		double radius = Double.parseDouble(this.parameters.getValueOfParameter("Radius"));
		boolean inverse = Boolean.parseBoolean(this.parameters.getValueOfParameter("Inverted"));
		boolean extractBackground = Boolean.parseBoolean(this.parameters.getValueOfParameter("Extract background"));
		boolean paraboloid = Boolean.parseBoolean(this.parameters.getValueOfParameter("Paraboloid"));
		boolean presmooth = Boolean.parseBoolean(this.parameters.getValueOfParameter("Presmoothing"));
		int outputDepth = Integer.parseInt(this.parameters.getValueOfParameter("Output Bit Depth"));
		// boolean normalize =
		// Boolean.parseBoolean(parameters.getValueOfParameter("Normalize"));
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		int total = images.size();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap dim : images.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			String path = images.get(dim);
			// File f = new File(path);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should
			// be
			// a
			// float
			// processor
			
			// //// Begin Actual Function
			jBackgroundSubtracter bS = new jBackgroundSubtracter();
			bS.setup("", im);
			jBackgroundSubtracter.radius = radius; // default rolling ball
			// radius
			jBackgroundSubtracter.lightBackground = inverse;
			jBackgroundSubtracter.createBackground = extractBackground;
			jBackgroundSubtracter.useParaboloid = paraboloid; // use
			// "Sliding Paraboloid"
			// instead of
			// rolling ball
			// algorithm
			jBackgroundSubtracter.doPresmooth = presmooth;
			// bS.JEX_setup();
			bS.run(imp);
			bS.setup("final", im);
			// //// End Actual Function
			
			// //// Save the results
			// String localDir = JEXWriter.getEntryFolder(entry);
			// if(localDir == null)
			// Logs.log("Null local directory returned!!!!!",
			// 0, this);
			// String newFileName = FunctionUtility.getNextName(localDir,
			// f.getName(), "BG");
			// String finalPath = localDir + File.separator + newFileName;
			// FunctionUtility.imSave(imp, "false", outputDepth, finalPath);
			ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", outputDepth);
			String finalPath = JEXWriter.saveImage(toSave);
			
			outputMap.put(dim.copy(), finalPath);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		JEXData output = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outputMap);
		output.setDataObjectInfo("Background subtracted using background subtraction function");
		this.realOutputs.add(output);
		
		// Return status
		return true;
	}
	
}
