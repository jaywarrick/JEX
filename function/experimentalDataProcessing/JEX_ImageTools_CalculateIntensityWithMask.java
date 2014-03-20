package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.ImageStatistics;

import java.util.HashMap;
import java.util.TreeMap;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
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
public class JEX_ImageTools_CalculateIntensityWithMask extends ExperimentalDataCrunch {
	
	public JEX_ImageTools_CalculateIntensityWithMask()
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
		String result = "Calculate intensity";
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
		String result = "Calculate fluorescent intensity of an image using a mask";
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
		String toolbox = "Image tools";
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
		inputNames[0] = new TypeName(IMAGE, "Image to quantify");
		inputNames[1] = new TypeName(IMAGE, "Mask");
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
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(VALUE, "Mean intensity per pixel");
		defaultOutputNames[1] = new TypeName(VALUE, "Area of mask");
		defaultOutputNames[2] = new TypeName(VALUE, "Integrated Intensity");
		
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
		// Parameter p0 = new
		// Parameter("Cell Radius","Estimate Cell Radius for Intensity Analysis.","14");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
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
		JEXData data1 = inputs.get("Image to quantify");
		JEXData data2 = inputs.get("Mask");
		if(data1 == null || !data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data1);
		TreeMap<DimensionMap,String> masks = ImageReader.readObjectToImagePathTable(data2);
		
		// Prepare the outputs
		TreeMap<DimensionMap,String> means = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> areas = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> intdens = new TreeMap<DimensionMap,String>();
		
		// Do the calculation
		for (DimensionMap dim : images.keySet())
		{
			// prepare the variables
			double mean = 0;
			double area = 0;
			double intden = 0;
			
			// Open the image to quantify
			String imPath = images.get(dim);
			ImagePlus imp = new ImagePlus(imPath);
			
			// Open the mask
			String maskPath = masks.get(dim);
			ImagePlus imm = new ImagePlus(maskPath);
			
			// calculate integrated intensity in the image to quantify
			ImageStatistics imStat = imp.getStatistics();
			intden = imStat.mean * imStat.area;
			
			// Calculate the area of the mask
			ImageStatistics maskStat = imm.getStatistics();
			area = maskStat.mean * maskStat.area / maskStat.max;
			
			// Calculate the mean intensity in the image to quanfity
			mean = intden / area;
			
			// save the variables
			means.put(dim, "" + mean);
			areas.put(dim, "" + area);
			intdens.put(dim, "" + intden);
		}
		
		// Make the outputs
		JEXData output1 = ValueWriter.makeValueTable(outputNames[0].getName(), means);
		JEXData output2 = ValueWriter.makeValueTable(outputNames[1].getName(), areas);
		JEXData output3 = ValueWriter.makeValueTable(outputNames[2].getName(), intdens);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
}
