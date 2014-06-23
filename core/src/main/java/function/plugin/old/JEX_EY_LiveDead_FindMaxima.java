package function.plugin.old;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.MaximumFinder;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.HashMap;
import java.util.List;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

/**
 * This is a JEXperiment function template
 * To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder
 * 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types
 * The specific API for these can be found in the main JEXperiment folder.
 * These API provide methods to retrieve data from these objects,
 * create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 *
 */
/**
 * @author edmyoung
 * 
 */
public class JEX_EY_LiveDead_FindMaxima extends JEXCrunchable {
	
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
		String result = "Live/Dead - Find Maxima";
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
		String result = "Using the Find Maxima function to calculate fraction of live cells";
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
		String toolbox = "Custom Cell Analysis";
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
		inputNames[0] = new TypeName(IMAGE, "Live Image");
		inputNames[1] = new TypeName(IMAGE, "Dead Image");
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
		defaultOutputNames = new TypeName[4];
		defaultOutputNames[0] = new TypeName(IMAGE, "Merged Image");
		defaultOutputNames[1] = new TypeName(VALUE, "Live Cell Count");
		defaultOutputNames[2] = new TypeName(VALUE, "Dead Cell Count");
		defaultOutputNames[3] = new TypeName(VALUE, "Live Fraction");
		
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
		Parameter p0 = new Parameter("Tolerance - Live", "Noise Tolerance for Maxima of Live Cells", "50");
		Parameter p1 = new Parameter("Tolerance - Dead", "Noise Tolerance for Maxima of Dead Cells", "300");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
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
		// Collect inputs
		// data1 contains all the LIVE images per entry
		// data2 contains all the DEAD images per entry
		JEXData data1 = inputs.get("Live Image");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		JEXData data2 = inputs.get("Dead Image");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Extract just the images from the imsets, and store as array lists of
		// JEXImage type
		List<String> image1 = ImageReader.readObjectToImagePathList(data1);
		List<String> image2 = ImageReader.readObjectToImagePathList(data2);
		
		// Get the parameters of the function
		double toleranceLive = new Double(parameters.getValueOfParameter("Tolerance - Live"));
		double toleranceDead = new Double(parameters.getValueOfParameter("Tolerance - Dead"));
		
		// for(JEXImage image: images){
		// path1 and path2 are strings to the paths of image1 and image2
		String path1 = image1.get(0);
		String path2 = image2.get(0);
		
		// Pull out the filenames from path1 and path2 by using getName method
		// File imFile1 = new File(path1);
		// String currentName1 = imFile1.getName();
		
		// File imFile2 = new File(path2);
		// String currentName2 = imFile2.getName();
		
		// Define All Necessary Images
		ImagePlus imPlus1 = new ImagePlus(path1);
		ImagePlus imPlus2 = new ImagePlus(path2);
		
		int imWidth = imPlus1.getWidth();
		int imHeight = imPlus1.getHeight();
		
		ShortProcessor bimPlus1 = (ShortProcessor) imPlus1.getProcessor().convertToShort(true);
		ShortProcessor bimPlus2 = (ShortProcessor) imPlus2.getProcessor().convertToShort(true);
		
		// ByteProcessor liveMaxima = new ByteProcessor(imWidth, imHeight);
		// ByteProcessor deadMaxima = new ByteProcessor(imWidth, imHeight);
		
		MaximumFinder maximumFinder1 = new MaximumFinder();
		MaximumFinder maximumFinder2 = new MaximumFinder();
		
		// double tolerance1 = 500.0;
		double threshold1 = ImageProcessor.NO_THRESHOLD;
		// int outputType1 = MaximumFinder.SINGLE_POINTS;
		// double tolerance2 = 500.0;
		double threshold2 = ImageProcessor.NO_THRESHOLD;
		// int outputType2 = MaximumFinder.SINGLE_POINTS;
		
		boolean excludeOnEdges = true;
		boolean isEDM = false;
		
		int outputType3 = MaximumFinder.COUNT;
		int outputType4 = MaximumFinder.COUNT;
		
		ResultsTable rt = ResultsTable.getResultsTable();
		rt.reset();
		
		// liveMaxima = maximumFinder1.findMaxima(bimPlus1, toleranceLive,
		// threshold1, outputType1, excludeOnEdges, isEDM);
		// deadMaxima = maximumFinder2.findMaxima(bimPlus2, toleranceDead,
		// threshold2, outputType2, excludeOnEdges, isEDM);
		
		// Find Maxima for live image using maximumFinder
		maximumFinder1.findMaxima(bimPlus1, toleranceLive, threshold1, outputType3, excludeOnEdges, isEDM);
		double liveCount = rt.getValue("Count", 0);
		System.out.println("livecount = " + liveCount);
		// Find Maxima for dead image using maximumFinder
		maximumFinder2.findMaxima(bimPlus2, toleranceDead, threshold2, outputType4, excludeOnEdges, isEDM);
		double deadCount = rt.getValue("Count", 1);
		System.out.println("deadcount = " + deadCount);
		
		// Quick calculation for live fraction, output to screen
		double liveFraction = liveCount / (liveCount + deadCount);
		System.out.println("live Fraction = " + liveFraction);
		
		// Output Live and Dead images for checking
		// ImagePlus ip_liveMaxima = new ImagePlus("LIVE", liveMaxima);
		// ip_liveMaxima.show();
		// ImagePlus ip_deadMaxima = new ImagePlus("DEAD", deadMaxima);
		// ip_deadMaxima.show();
		
		rt.show("Results - LIVE/DEAD");
		
		// -------------------------
		// Making the color overlay
		// -------------------------
		
		// BimPlus1 is set for green pixels
		byte[] g = (byte[]) bimPlus1.convertToByte(true).getPixels();
		
		// BimPlus2 is set for red pixels
		byte[] r = (byte[]) bimPlus2.convertToByte(true).getPixels();
		
		// Make an empty blue pixel channel
		byte[] b = new byte[imWidth * imHeight];
		
		// Setting the green and red pixels in a new color image
		ColorProcessor cimOutputImage = new ColorProcessor(imWidth, imHeight);
		cimOutputImage.setRGB(r, g, b);
		
		// Save the results
		// String localDir = JEXWriter.getEntryFolder(entry);
		// String newFileName1 = FunctionUtility.getNextName(localDir,
		// currentName1, "L");
		// String finalPath1 = localDir + File.separator + newFileName1;
		// Logs.log("Saving the image to path "+ finalPath1, 1,
		// this);
		// FunctionUtility.imSave(liveMaxima, finalPath1);
		
		// String newFileName2 = FunctionUtility.getNextName(localDir,
		// currentName2, "D");
		// String finalPath2 = localDir + File.separator + newFileName2;
		// Logs.log("Saving the image to path "+ finalPath2, 1,
		// this);
		// FunctionUtility.imSave(deadMaxima, finalPath2);
		
		// FileSaver imFS = new FileSaver(cimOutputImage);
		// Defining the output path for the merged image
		// String finalPath3 = localDir + File.separator + "mergedLiveDead.tif";
		
		// Saving the outputed merged image
		// FunctionUtility.imSave(new ImagePlus("",cimOutputImage), finalPath3);
		// imFS.saveAsTiff(finalPath3);
		
		Logs.log("Saving the live, dead and merged image", 1, this);
		// String finalPath1 = JEXWriter.saveImage(liveMaxima);
		// String finalPath2 = JEXWriter.saveImage(deadMaxima);
		String finalPath3 = JEXWriter.saveImage(cimOutputImage);
		
		// Make the merged image output JEXData and output to JEX
		JEXData resultImage = ImageWriter.makeImageObject(outputNames[0].getName(), finalPath3);
		realOutputs.add(resultImage);
		
		JEXData JEXliveCount = ValueWriter.makeValueObject(outputNames[1].getName(), "" + liveCount);
		realOutputs.add(JEXliveCount);
		JEXData JEXdeadCount = ValueWriter.makeValueObject(outputNames[2].getName(), "" + deadCount);
		realOutputs.add(JEXdeadCount);
		JEXData JEXliveFraction = ValueWriter.makeValueObject(outputNames[3].getName(), "" + liveFraction);
		realOutputs.add(JEXliveFraction);
		
		// Return status
		return true;
	}
}
