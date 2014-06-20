package function.plugin.old;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
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
import function.imageUtility.AutoThresholder;

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
public class JEX_AutoThreshold extends JEXCrunchable {
	
	public JEX_AutoThreshold()
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
		String result = "Auto-Threshold";
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
		String result = "Automatically threhsold image and return a binary image.";
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
		return true;
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
		inputNames[0] = new TypeName(IMAGE, "Image");
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
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Auto-Thresholded Image");
		
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p1 = new Parameter("Method", "Method of automatically choosing the threshold.", Parameter.DROPDOWN, new String[] { "HUANG", "INTERMODES", "ISODATA", "LI", "MAXENTROPY", "MEAN", "MINERROR", "MINIMUM", "MOMENTS", "OTSU", "PERCENTILE", "RENYIENTROPY", "SHANBHAG", "TRIANGLE", "YEN" });
		Parameter p2 = new Parameter("Threshold Multiplier", "Scale the threshold returned by the autothresholder before applying the threshold.", "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
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
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Gather parameters
		double multiplier = Double.parseDouble(this.parameters.getValueOfParameter("Threshold Multiplier"));
		String method = this.parameters.getValueOfParameter("Method");
		int methodInt = AutoThresholder.OTSU;
		if(method.equals("HUANG"))
		{
			methodInt = AutoThresholder.HUANG;
		}
		else if(method.equals("INTERMODES"))
		{
			methodInt = AutoThresholder.INTERMODES;
		}
		else if(method.equals("ISODATA"))
		{
			methodInt = AutoThresholder.ISODATA;
		}
		else if(method.equals("LI"))
		{
			methodInt = AutoThresholder.LI;
		}
		else if(method.equals("MAXENTROPY"))
		{
			methodInt = AutoThresholder.MAXENTROPY;
		}
		else if(method.equals("MEAN"))
		{
			methodInt = AutoThresholder.MEAN;
		}
		else if(method.equals("MINERROR"))
		{
			methodInt = AutoThresholder.MINERROR;
		}
		else if(method.equals("MINIMUM"))
		{
			methodInt = AutoThresholder.MINIMUM;
		}
		else if(method.equals("MOMENTS"))
		{
			methodInt = AutoThresholder.MOMENTS;
		}
		else if(method.equals("OTSU"))
		{
			methodInt = AutoThresholder.OTSU;
		}
		else if(method.equals("PERCENTILE"))
		{
			methodInt = AutoThresholder.PERCENTILE;
		}
		else if(method.equals("RENYIENTROPY"))
		{
			methodInt = AutoThresholder.RENYIENTROPY;
		}
		else if(method.equals("SHANBHAG"))
		{
			methodInt = AutoThresholder.SHANBHAG;
		}
		else if(method.equals("TRIANGLE"))
		{
			methodInt = AutoThresholder.TRIANGLE;
		}
		else if(method.equals("YEN"))
		{
			methodInt = AutoThresholder.YEN;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count = 0, percentage = 0;
		AutoThresholder at = new AutoThresholder();
		for (DimensionMap map : imageMap.keySet())
		{
			// Get the image
			ImagePlus im = new ImagePlus(imageMap.get(map));
			ImageProcessor ip = im.getProcessor();
			
			// Do threshold
			FloatProcessor temp = (FloatProcessor) ip.convertToFloat();
			FunctionUtility.imAdjust(temp, ip.getMin(), ip.getMax(), 0d, 255d, 1d);
			ByteProcessor bp = (ByteProcessor) temp.convertToByte(false);
			int[] hist = bp.getHistogram();
			double threshold = at.getThreshold(methodInt, hist);
			threshold = threshold * multiplier;
			if(threshold > 255)
			{
				threshold = 255;
			}
			else if(threshold < 0)
			{
				threshold = 0;
			}
			bp.threshold((int) threshold);
			String path = JEXWriter.saveImage(bp);
			outputMap.put(map, path);
			
			// Update progress
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputMap.size() == 0)
		{
			return false;
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outputMap);
		
		// Set the outputs
		this.realOutputs.add(output1);
		
		// Return status
		return true;
	}
}
