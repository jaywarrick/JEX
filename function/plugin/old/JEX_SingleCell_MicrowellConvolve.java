package function.plugin.old;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import tables.DimTable;
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
import function.singleCellAnalysis.MicrowellFinder;

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
public class JEX_SingleCell_MicrowellConvolve extends JEXCrunchable {
	
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
		String result = "Microwell Convovler";
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
		String result = "Convolution with a specialized kernal for finding microwells in an image.";
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
		String toolbox = "Single Cell Analysis";
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
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[4];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Kernel Image");
		this.defaultOutputNames[1] = new TypeName(IMAGE, "Radius Filtered Image");
		this.defaultOutputNames[2] = new TypeName(IMAGE, "Edge Filtered Image");
		this.defaultOutputNames[3] = new TypeName(IMAGE, "Convolution Image");
		
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
		Parameter pa0 = new Parameter("Test Run?", "Just run this for the first image in each well?", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter pa1 = new Parameter("Color Dim Name", "Name of the 'Color' dimension.", "Color");
		Parameter pa2 = new Parameter("Color Dim Value", "Which 'Color' to analyze", "2");
		Parameter pa4 = new Parameter("Time Dim Name", "Name of the 'Time' dimension.", "Time");
		Parameter pa5 = new Parameter("Time Value to Convolve", "Value of the 'Time' dimension that should be chosen for finding microwells", "0");
		Parameter pb1 = new Parameter("Mean Pre-filter Radius", "Set to 0 or negative to avoid mean prefilter", "5");
		Parameter pb2 = new Parameter("Edge Pre-filter?", "Apply edge filter prior to convolution?", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter pa6 = new Parameter("Kernel Outer Radius", "The half width/height of the whole kernel (white).", "24");
		Parameter pa7 = new Parameter("Kernel Inner Radius", "The half width/height of the black box in the center of the kernel.", "11");
		Parameter pa8 = new Parameter("Invert Kernel?", "Whether to invert the colors of the kernel.", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p1 = new Parameter("Normalize", "Normalize the image", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p2 = new Parameter("Output Bit Depth", "Depth of the outputted image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 2);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(pa0);
		parameterArray.addParameter(pa1);
		parameterArray.addParameter(pa2);
		parameterArray.addParameter(pa4);
		parameterArray.addParameter(pa5);
		parameterArray.addParameter(pb1);
		parameterArray.addParameter(pb2);
		parameterArray.addParameter(pa6);
		parameterArray.addParameter(pa7);
		parameterArray.addParameter(pa8);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be overridden by returning false If over-ridden ANY batch function using this function will not be able perform error
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
		// Get the base images
		JEXData data1 = inputs.get("Image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// //// Get params
		Boolean isTest = Boolean.parseBoolean(this.parameters.getValueOfParameter("Test Run?"));
		String colorDimName = this.parameters.getValueOfParameter("Color Dim Name");
		String colorVal = this.parameters.getValueOfParameter("Color Dim Value");
		String timeDimName = this.parameters.getValueOfParameter("Time Dim Name");
		String timeVal = this.parameters.getValueOfParameter("Time Value to Convolve");
		double radius = Double.parseDouble(this.parameters.getValueOfParameter("Mean Pre-filter Radius"));
		boolean edgeFilter = Boolean.parseBoolean(this.parameters.getValueOfParameter("Edge Pre-filter?"));
		int outerRad = Integer.parseInt(this.parameters.getValueOfParameter("Kernel Outer Radius"));
		int innerRad = Integer.parseInt(this.parameters.getValueOfParameter("Kernel Inner Radius"));
		boolean inverted = Boolean.parseBoolean(this.parameters.getValueOfParameter("Invert Kernel?"));
		boolean norm = Boolean.parseBoolean(this.parameters.getValueOfParameter("Normalize"));
		int depth = Integer.parseInt(this.parameters.getValueOfParameter("Output Bit Depth"));
		
		// Get the sub-DimTable of only the BF images at one timepoint for convolving.
		DimTable imageTable = data1.getDimTable();
		DimTable subTable = null;
		if(imageTable.getDimWithName(timeDimName) == null)
		{
			subTable = imageTable.getSubTable(new DimensionMap(colorDimName + "=" + colorVal));
		}
		else
		{
			subTable = imageTable.getSubTable(new DimensionMap(colorDimName + "=" + colorVal + "," + timeDimName + "=" + timeVal));
		}
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data1);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		String kernelImage = null;
		String radiusFilteredImage = null;
		String edgeFilteredImage = null;
		String convolutionImage = null;
		
		int count = 0;
		int total = subTable.mapCount();
		FloatProcessor kernel = MicrowellFinder.makeKernel(outerRad, innerRad, inverted);
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap map : subTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			// Get the image
			String path = images.get(map);
			FloatProcessor imp = (FloatProcessor) (new ImagePlus(path)).getProcessor().convertToFloat();
			
			// Convolve the image
			Vector<FloatProcessor> results = MicrowellFinder.filterAndConvolve(imp, radius, edgeFilter, kernel, isTest);
			
			// Save the image
			ImagePlus im = FunctionUtility.makeImageToSave(imp, norm, 1, depth);
			String finalPath = JEXWriter.saveImage(im);
			DimensionMap outMap = map.copy();
			outMap.remove(colorDimName);
			outMap.remove(timeDimName);
			outputMap.put(outMap, finalPath);
			
			if(isTest)
			{
				Vector<String> paths = new Vector<String>();
				for (FloatProcessor fp : results)
				{
					String temp = JEXWriter.saveImage(fp);
					if(temp != null)
					{
						paths.add(temp);
						// (new ImagePlus(temp, fp)).show();
					}
					else
					{
						// (new ImagePlus("", fp)).show();
						paths.add("");
					}
				}
				convolutionImage = finalPath;
				kernelImage = paths.get(0);
				radiusFilteredImage = paths.get(1);
				edgeFilteredImage = paths.get(2);
				Logs.log("Finished Convolution Test", this);
				JEXStatics.statusBar.setProgressPercentage(100);
				break;
			}
			
			// Update the status
			count++;
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			int percentage = (int) (100 * ((double) count / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		if(isTest)
		{
			JEXData output0 = ImageWriter.makeImageObject(this.outputNames[0].getName(), kernelImage);
			JEXData output1 = ImageWriter.makeImageObject(this.outputNames[1].getName(), radiusFilteredImage);
			JEXData output2 = ImageWriter.makeImageObject(this.outputNames[2].getName(), edgeFilteredImage);
			JEXData output3 = ImageWriter.makeImageObject(this.outputNames[3].getName(), convolutionImage);
			
			this.realOutputs.add(output0);
			this.realOutputs.add(output1);
			this.realOutputs.add(output2);
			this.realOutputs.add(output3);
		}
		else
		{
			JEXData output3 = ImageWriter.makeImageStackFromPaths(this.outputNames[3].getName(), outputMap);
			this.realOutputs.add(output3);
		}
		
		// Return status
		return true;
	}
}
