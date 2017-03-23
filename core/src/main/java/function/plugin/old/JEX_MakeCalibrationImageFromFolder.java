package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.HashMap;

import jex.statics.JEXStatics;

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
public class JEX_MakeCalibrationImageFromFolder extends JEXCrunchable {
	
	public static ImagePlus calibrationImage = null;
	
	public JEX_MakeCalibrationImageFromFolder()
	{}
	
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
		String result = "Make Calibration Image (Folder)";
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
		String result = "Treat a folder of images as a stack and calculate the mean or median stack intensity.";
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
		String toolbox = "Calibration Tools";
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
		TypeName[] inputNames = new TypeName[0];
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(IMAGE, "Calibration Image");
		
		if(outputNames == null)
		{
			return defaultOutputNames;
		}
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
		Parameter p0 = new Parameter("Folder", "Folder containing the images from which to make the calibration image", Parameter.FILECHOOSER, "");
		Parameter p1 = new Parameter("Stack Projection Method", "Calculation method for projecting the stack to a single image", Parameter.DROPDOWN, new String[] { JEX_StackProjection.METHOD_MEAN, JEX_StackProjection.METHOD_MEDIAN, JEX_StackProjection.METHOD_MIN, JEX_StackProjection.METHOD_MAX, JEX_StackProjection.METHOD_STDEV, JEX_StackProjection.METHOD_SUM }, 1);
		Parameter p2 = new Parameter("Final Smoothing Method", "Smoothing function to apply at the end", Parameter.DROPDOWN, new String[] { "none", JEX_StackProjection.METHOD_MEAN, JEX_StackProjection.METHOD_MEDIAN }, 2);
		Parameter p3 = new Parameter("Smoothing Filter Radius", "Radius of the smoothing filter", "2");
		
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
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-ridden by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return false;
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
		// Gather parameters
		String folderPath = parameters.getValueOfParameter("Folder");
		String method = parameters.getValueOfParameter("Stack Projection Method");
		double radius = Double.parseDouble(parameters.getValueOfParameter("Smoothing Filter Radius"));
		String smooth = parameters.getValueOfParameter("Final Smoothing Method");
		
		// Run the function
		File folder = new File(folderPath);
		if(!folder.isDirectory())
		{
			return false;
		}
		
		if(calibrationImage == null)
		{
			File[] files = folder.listFiles();
			
			FloatProcessor imp = getMeanProjection(files);
			
			if(!smooth.equals("none"))
			{
				RankFilters rF = new RankFilters();
				rF.rank(imp, radius, JEX_StackProjection.getMethodInt(method));
			}
			
			// //// End Actual Function
			calibrationImage = new ImagePlus("temp", imp);
		}
		
		// //// Save the results
		String finalPath = JEXWriter.saveImage(calibrationImage);
		
		JEXData output1 = ImageWriter.makeImageObject(outputNames[0].getName(), finalPath);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	public FloatProcessor getMeanProjection(File[] fileList)
	{
		int i = 0;
		FloatProcessor imp1 = null, imp2 = null;
		FloatBlitter blit = null;
		for (File f : fileList)
		{
			if(i == 0)
			{
				imp1 = (FloatProcessor) (new ImagePlus(f.getPath())).getProcessor().convertToFloat();
				blit = new FloatBlitter(imp1);
			}
			else
			{
				imp2 = (FloatProcessor) (new ImagePlus(f.getPath())).getProcessor().convertToFloat();
				blit.copyBits(imp2, 0, 0, Blitter.ADD);
			}
			JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / fileList.length));
			i++;
		}
		imp1.multiply((double) 1.0 / fileList.length);
		return imp1;
	}
	
	public void finalizeTicket(Ticket ticket)
	{
		if(calibrationImage != null)
		{
			calibrationImage.flush();
			calibrationImage = null;
		}
	}
}
