package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
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
public class JEX_ImageTools_CropImageUsingROI extends JEXCrunchable {
	
	public JEX_ImageTools_CropImageUsingROI()
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
		String result = "Crop an Image";
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
		String result = "Function that allows you to crop a set of images based on a Regoin ROI within each image.";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(ROI, "Region ROI");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Cropped Image");
		
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
		Parameter p0 = new Parameter("Are You Sure?", "Dummy Parameter.", Parameter.DROPDOWN, new String[] { "yes" }, 0);
		// Parameter p1 = new
		// Parameter("Interpolation Method","Reassigns Pixel Intensities Based on This Method",FormLine.DROPDOWN,new
		// String[] {"Bilinear","Bicubic","Nearest Neighbor"},0);
		// Parameter p2 = new
		// Parameter("Old Max","Image Intensity Value","4095.0");
		// Parameter p3 = new
		// Parameter("New Min","Image Intensity Value","0.0");
		// Parameter p4 = new
		// Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new
		// Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new
		// String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
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
		JEXData imageFiles = inputs.get("Image");
		if(imageFiles == null || !imageFiles.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Collect the inputs
		JEXData regionData = inputs.get("Region ROI");
		if(regionData == null || !regionData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Run the function
		TreeMap<DimensionMap,String> imageList = ImageReader.readObjectToImagePathTable(imageFiles);
		TreeMap<DimensionMap,ROIPlus> regionROI = RoiReader.readObjectToRoiMap(regionData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		ROIPlus region;
		ImagePlus image;
		FloatProcessor imageP;
		int count = 0;
		String actualPath = "";
		int total = imageList.size();
		
		for (DimensionMap map : imageList.keySet())
		{
			image = new ImagePlus(imageList.get(map));
			imageP = (FloatProcessor) image.getProcessor().convertToFloat();
			int bitDepth = image.getBitDepth();
			region = regionROI.get(map);
			if(region != null)
			{
				boolean isLine = region.isLine();
				if(isLine)
					return false;
				imageP.setRoi(region.getRoi());
				imageP = (FloatProcessor) imageP.crop().convertToFloat();
			}
			actualPath = this.saveProjectedImage(imageP, bitDepth);
			outputMap.put(map, actualPath);
			count = count + 1;
			JEXStatics.statusBar.setProgressPercentage(count * 100 / total);
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputMap);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private String saveProjectedImage(FloatProcessor imp, int bitDepth)
	{
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		return imPath;
	}
}
