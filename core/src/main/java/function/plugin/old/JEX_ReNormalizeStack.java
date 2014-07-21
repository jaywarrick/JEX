package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageStatistics;

import java.util.HashMap;
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
public class JEX_ReNormalizeStack extends JEXCrunchable {
	
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
		String result = "Normalize stack";
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
		String result = "Normalize a stack to the same mean intensity";
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
		inputNames[0] = new TypeName(IMAGE, "Stack");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Normalized stack");
		
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
		Parameter p6 = new Parameter("Output Bit Depth", "Depth of the outputted image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
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
		JEXData data = inputs.get("Stack");
		if(!data.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// //// Get params
		// int depth =
		// Integer.parseInt(parameters.getValueOfParameter("Output Bit Depth"));
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		if(images.size() == 0)
			return true;
		int count = 0;
		int total = images.size();
		double mean = 1;
		for (DimensionMap dim : images.keySet())
		{
			String path = images.get(dim);
			// File f = new File(path);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			ByteProcessor imp = (ByteProcessor) im.getProcessor().convertToByte(true);
			ImageStatistics stats = (new ImagePlus("", imp)).getStatistics();
			
			if(count == 0)
			{
				mean = stats.mean;
			}
			else
			{
				imp.multiply(mean / stats.mean);
			}
			
			// //// Save the results
			// String localDir = JEXWriter.getEntryFolder(entry);
			// String newFileName = FunctionUtility.getNextName(localDir,
			// f.getName(), "Norm");
			// String finalPath = localDir + File.separator + newFileName;
			// FunctionUtility.imSave(imp, finalPath);
			String finalPath = JEXWriter.saveImage(imp);
			
			outputMap.put(dim.copy(), finalPath);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputMap);
		output.setDataObjectInfo("Stack normalized using stack normalization function");
		realOutputs.add(output);
		
		// Return status
		return true;
	}
	
}
