package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
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
import function.ExperimentalDataCrunch;

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
public class JEX_OverlayImageSets extends ExperimentalDataCrunch {
	
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
		String result = "Overlay Image Objects";
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
		String result = "Overlay two image sets and assign them a color channel";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE, "Channel 1");
		inputNames[1] = new TypeName(IMAGE, "Channel 2");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Overlay");
		
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
		Parameter p1 = new Parameter("Channel 1 color", "Color of channel 1", Parameter.DROPDOWN, new String[] { "R", "G", "B" }, 0);
		Parameter p2 = new Parameter("Channel 2 color", "Color of channel 2", Parameter.DROPDOWN, new String[] { "R", "G", "B" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
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
		JEXData data1 = inputs.get("Channel 1");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Channel 2");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// //// Get params
		String s1 = parameters.getValueOfParameter("Channel 1 color");
		String s2 = parameters.getValueOfParameter("Channel 2 color");
		int color1 = s1.equals("R") ? 0 : (s1.equals("G") ? 1 : 2);
		int color2 = s2.equals("R") ? 0 : (s2.equals("G") ? 1 : 2);
		
		// Run the function
		TreeMap<DimensionMap,String> images1 = ImageReader.readObjectToImagePathTable(data1);
		TreeMap<DimensionMap,String> images2 = ImageReader.readObjectToImagePathTable(data2);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0;
		int total = images1.size();
		
		for (DimensionMap dim : images1.keySet())
		{
			// get the path
			String path1 = images1.get(dim);
			String path2 = images2.get(dim);
			
			// get the image
			ImagePlus im1 = new ImagePlus(path1);
			ImagePlus im2 = new ImagePlus(path2);
			
			// get the image processor
			ij.process.ByteProcessor imp1 = (ij.process.ByteProcessor) im1.getProcessor().convertToByte(true); // should
			// be
			// a
			// float
			// processor
			ij.process.ByteProcessor imp2 = (ij.process.ByteProcessor) im2.getProcessor().convertToByte(true); // should
			// be
			// a
			// float
			// processor
			
			// //// Begin Actual Function
			int w = im1.getWidth(), h = im1.getHeight();
			byte[] r = null, g = null, b = null;
			ColorProcessor cp = new ColorProcessor(w, h);
			if(color1 == 0)
				r = (byte[]) imp1.getPixels();
			if(color2 == 0)
				r = (byte[]) imp2.getPixels();
			if(r == null)
				r = new byte[w * h];
			if(color1 == 1)
				g = (byte[]) imp1.getPixels();
			if(color2 == 1)
				g = (byte[]) imp2.getPixels();
			if(g == null)
				g = new byte[w * h];
			if(color1 == 2)
				b = (byte[]) imp1.getPixels();
			if(color2 == 2)
				b = (byte[]) imp2.getPixels();
			if(b == null)
				b = new byte[w * h];
			cp.setRGB(r, g, b);
			// //// End Actual Function
			
			// //// Save the results
			String finalPath = JEXWriter.saveImage(cp);
			outputMap.put(dim.copy(), finalPath);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images1.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		JEXData output = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputMap);
		output.setDataObjectInfo("Overaly performed using Image Overlay Function");
		realOutputs.add(output);
		
		// Return status
		return true;
	}
}
