package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.Blitter;
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
public class JEX_ImageTools_MeasureFluorescentIntensityWithMask extends JEXCrunchable {
	
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
		String result = "Quantify fluorescence with mask";
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
		String result = "Measures the fluorescent signal from one image using a mask";
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
	 * Return the number of outputs returned by this function
	 * 
	 * @return name of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[3];
		this.defaultOutputNames[0] = new TypeName(VALUE, "Fluorescent intensity");
		this.defaultOutputNames[1] = new TypeName(VALUE, "Fluorescent area");
		this.defaultOutputNames[2] = new TypeName(IMAGE, "Masked image");
		
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
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
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
		JEXData data1 = inputs.get("Image to quantify");
		if(!data1.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		JEXData data2 = inputs.get("Image to quantify");
		if(!data2.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data1);
		TreeMap<DimensionMap,String> masks = ImageReader.readObjectToImagePathTable(data2);
		TreeMap<DimensionMap,String> outputMeanMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> outputAreaMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,ImagePlus> outputImageMap = new TreeMap<DimensionMap,ImagePlus>();
		
		int count = 0;
		int total = images.size();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap dim : images.keySet())
		{
			
			// Get the image
			String path = images.get(dim);
			ImagePlus im = new ImagePlus(path);
			ByteProcessor bimp = (ByteProcessor) im.getProcessor().convertToByte(true); // should
			// be
			// a
			// float
			// processor
			
			// Get the mask
			String maskImage = masks.get(dim);
			ImagePlus imask = new ImagePlus(maskImage);
			ByteProcessor bimpmask = (ByteProcessor) imask.getProcessor().convertToByte(true); // should
			// be
			// a
			// float
			// processor
			
			// Do the masking
			ByteProcessor minImp = (ByteProcessor) bimp.duplicate();
			minImp.copyBits(bimpmask, 0, 0, Blitter.MIN);
			
			// Measure the intensity of the masked image
			ImagePlus minIm = new ImagePlus("", minImp);
			ImageStatistics stats = minIm.getStatistics(ImagePlus.INTEGRATED_DENSITY);
			double rawInt = stats.area * stats.mean;
			
			// Measure the intensity of the mask
			ImageStatistics statMask = imask.getStatistics(ImagePlus.AREA);
			double areaInt = statMask.area;
			
			// Get the result and store in outputMap
			double meanInt = rawInt / areaInt;
			outputMeanMap.put(dim, "" + meanInt);
			outputAreaMap.put(dim, "" + areaInt);
			outputImageMap.put(dim, minIm);
			
			// Update the counter
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Save the intensity data
		JEXData outputData1 = ValueWriter.makeValueTable(this.outputNames[0].getName(), outputMeanMap);
		outputData1.setDataObjectInfo("Mean value of the image under the mask area");
		this.realOutputs.add(outputData1);
		
		// Save the area data
		JEXData outputData2 = ValueWriter.makeValueTable(this.outputNames[1].getName(), outputAreaMap);
		outputData2.setDataObjectInfo("Area of the mask");
		this.realOutputs.add(outputData2);
		
		// Save the area data
		JEXData outputData3 = ImageWriter.makeImageStackFromImagePluses(this.outputNames[2].getName(), outputImageMap);
		outputData3.setDataObjectInfo("Combination of the mask and the image to analyze");
		this.realOutputs.add(outputData3);
		
		// Return status
		return true;
	}
	
}
