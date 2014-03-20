package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
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
public class JEX_MakeCalibrationImageFromObject extends ExperimentalDataCrunch {
	
	public static ImagePlus calibrationImage = null;
	
	public JEX_MakeCalibrationImageFromObject()
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
		String result = "Make Calibration Image (Object)";
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
		String result = "Treat an image object as a stack and calculate the mean stack intensity.";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(IMAGE, "Source Images");
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
		Parameter p1 = new Parameter("Stack Projection Method", "Calculation method for projecting the stack to a single image (pseudo median = The median of subgroups will be averaged)", Parameter.DROPDOWN, new String[] { "Mean", "Pseudo Median" }, 1);
		Parameter p2 = new Parameter("Pseudo Median Subroup Size", "The number of images in each subgroup. Used for pseudo median option. Each median operation only produces integer value increments. Mean produces decimal increments", "10");
		Parameter p3 = new Parameter("Final Smoothing Method", "Smoothing function to apply at the end", Parameter.DROPDOWN, new String[] { "none", JEX_StackProjection.METHOD_MEAN, JEX_StackProjection.METHOD_MEDIAN }, 2);
		Parameter p4 = new Parameter("Smoothing Filter Radius", "Radius of the smoothing filter", "2");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
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
		// Collect the inputs
		JEXData imageData = inputs.get("Source Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			// Return status
			return true;
		}
		
		// Gather parameters
		String method = parameters.getValueOfParameter("Stack Projection Method");
		double radius = Double.parseDouble(parameters.getValueOfParameter("Smoothing Filter Radius"));
		int groupSize = Integer.parseInt(parameters.getValueOfParameter("Pseudo Median Subroup Size"));
		String smooth = parameters.getValueOfParameter("Final Smoothing Method");
		
		if(calibrationImage == null)
		{
			String[] filePaths = ImageReader.readObjectToImagePathStack(imageData);
			
			FloatProcessor imp = null;
			if(method.equals("Mean"))
			{
				imp = getMeanProjection(filePaths);
			}
			else
			{
				imp = getPseudoMedianProjection(filePaths, groupSize);
			}
			
			if(!smooth.equals("none"))
			{
				ImagePlus temp = new ImagePlus("temp", imp);
				RankFilters rF = new RankFilters();
				rF.setup(method, temp);
				rF.makeKernel(radius);
				rF.run(imp);
				temp.flush();
				temp = null;
			}
			
			// //// End Actual Function
			calibrationImage = new ImagePlus("temp", imp);
		}
		
		// JEXData temp = ImageWriter.makeImageObject(outputNames[0].getName(),
		// "Placeholder");
		// realOutputs.add(temp);
		
		// Return status
		return true;
	}
	
	public FloatProcessor getPseudoMedianProjection(String[] fileList, int groupSize)
	{
		int i = 0, k = 0;
		FloatProcessor ret = null, imp = null;
		FloatBlitter blit = null;
		while (i < fileList.length)
		{
			File[] files = new File[groupSize];
			for (int j = 0; j < groupSize && i < fileList.length; j++)
			{
				files[j] = new File(fileList[i]);
				i++;
			}
			// Get the median of the group
			ImagePlus stack = ImageReader.readFileListToVirtualStack(files);
			stack.setProcessor((FloatProcessor) stack.getProcessor().convertToFloat());
			imp = JEX_StackProjection.evaluate(stack, JEX_StackProjection.METHOD_MEDIAN, groupSize);
			
			// Add it to the total for taking the mean of the groups
			if(k == 0)
			{
				ret = imp;
				blit = new FloatBlitter(ret);
			}
			else
			{
				blit.copyBits(imp, 0, 0, Blitter.ADD);
			}
			JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / fileList.length));
			k++;
		}
		// Divide the total by the number of groups to get the final mean of the
		// groups
		ret.multiply((double) 1 / k);
		return ret;
	}
	
	public FloatProcessor getMeanProjection(String[] fileList)
	{
		int i = 0;
		FloatProcessor imp1 = null, imp2 = null;
		FloatBlitter blit = null;
		for (String f : fileList)
		{
			if(i == 0)
			{
				imp1 = (FloatProcessor) (new ImagePlus(f)).getProcessor().convertToFloat();
				blit = new FloatBlitter(imp1);
			}
			else
			{
				imp2 = (FloatProcessor) (new ImagePlus(f)).getProcessor().convertToFloat();
				blit.copyBits(imp2, 0, 0, Blitter.ADD);
			}
			JEXStatics.statusBar.setProgressPercentage((int) (100 * (double) i / fileList.length));
			i++;
		}
		imp1.multiply((double) 1 / fileList.length);
		return imp1;
	}
	
	public void finalizeTicket(Ticket ticket)
	{
		if(calibrationImage != null)
		{
			// Copy the calibration image to all the locations
			TreeMap<JEXEntry,Set<JEXData>> outputs = ticket.getOutputList();
			for (Entry<JEXEntry,Set<JEXData>> e : outputs.entrySet())
			{
				String finalPath = JEXWriter.saveImage(calibrationImage);
				JEXData temp = ImageWriter.makeImageObject(outputNames[0].getName(), finalPath);
				Set<JEXData> data = e.getValue();
				data.add(temp);
			}
			calibrationImage.flush();
			calibrationImage = null;
		}
	}
}
