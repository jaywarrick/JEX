package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.VirtualStack;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import tables.Dim;
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
public class JEX_TimeLapseAdaptiveBackgroundSubtraction extends ExperimentalDataCrunch {
	
	public JEX_TimeLapseAdaptiveBackgroundSubtraction()
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
		String result = "Timelapse Adaptive Background Subtraction";
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
		String result = "A stack projection (max, median, or min) is used to subtract the background depending upon the type of imaging being performed.";
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
		inputNames[0] = new TypeName(IMAGE, "Timelapse Image(s)");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Treated Timelapse");
		
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
		Parameter p0 = new Parameter("Dimension", "Name of dimension to perform the operation (case and whitespace sensitive).", "Z");
		Parameter p1 = new Parameter("Math Operation", "Type of math operation to perform.", Parameter.DROPDOWN, new String[] { "mean", "max", "min", "sum", "std. dev.", "median" }, 5);
		Parameter p2 = new Parameter("Sliding Window Projection", "Perform the projection for the whole stack or for N number of images at a time, shifting by 1 each time.", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p3 = new Parameter("N", "Number of images in sliding window (ignored if not sliding window and increased if necessary to make an odd number).", "5");
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
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		JEXData imageData = inputs.get("Timelapse Image(s)");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		DimTable originalDimTable = imageData.getDimTable().copy();
		
		// Collect Parameters
		String dimName = parameters.getValueOfParameter("Dimension");
		String mathOperation = parameters.getValueOfParameter("Math Operation");
		boolean slidingWindow = Boolean.parseBoolean(parameters.getValueOfParameter("Sliding Window Projection"));
		int slidingWindowSize = Integer.parseInt(parameters.getValueOfParameter("N"));
		
		// Run the function
		Dim dimToProject = originalDimTable.getDimWithName(dimName);
		if(dimToProject == null)
			return false;
		DimTable subDimTable = originalDimTable.copy();
		int originalDimIndex = subDimTable.indexOfDimWithName(dimToProject.name());
		subDimTable.remove(originalDimIndex);
		
		List<DimensionMap> maps = subDimTable.getDimensionMaps();
		TreeMap<DimensionMap,String> dataMap = new TreeMap<DimensionMap,String>();
		String baseName;
		String actualPath;
		int count = 0;
		int bitDepth = 32;
		FloatProcessor fpBG, fpOrig;
		for (DimensionMap map : maps)
		{
			if(slidingWindow)
			{
				for (int i = 0; i <= dimToProject.size() - slidingWindowSize; i++)
				{
					List<DimensionMap> stackMaps = this.getSomeStackMaps(map, dimToProject, slidingWindowSize, i);
					ImagePlus stackToProject = this.getVirtualStack(entry, imageData, stackMaps);
					bitDepth = stackToProject.getBitDepth();
					fpBG = this.evaluate(stackToProject, mathOperation, stackMaps.size());
					fpOrig = (FloatProcessor) (new ImagePlus(ImageReader.readObjectToImagePath(imageData, stackMaps.get(0)))).getProcessor().convertToFloat();
					float[] orig = (float[]) fpOrig.getPixels();
					float[] bg = (float[]) fpBG.getPixels();
					for (int p = 0; p < bg.length; p++)
					{
						orig[p] = orig[p] - bg[p];
					}
					DimensionMap saveMap = this.getAStackMap(map, dimToProject, i);
					baseName = ImageReader.readObjectToImageName(imageData, saveMap);
					actualPath = this.saveProjectedImage(entry, baseName, fpOrig, bitDepth);
					if(actualPath != null)
					{
						dataMap.put(saveMap, actualPath);
					}
				}
			}
			else
			{
				List<DimensionMap> stackMaps = this.getAllStackMaps(map, dimToProject);
				ImagePlus stackToProject = this.getVirtualStack(entry, imageData, stackMaps);
				bitDepth = stackToProject.getBitDepth();
				fpBG = this.evaluate(stackToProject, mathOperation, stackMaps.size());
				float[] bg = (float[]) fpBG.getPixels();
				float[] orig;
				for (DimensionMap stackMap : stackMaps)
				{
					fpOrig = (FloatProcessor) (new ImagePlus(ImageReader.readObjectToImagePath(imageData, stackMap))).getProcessor().convertToFloat();
					orig = null;
					orig = (float[]) fpOrig.getPixels();
					for (int p = 0; p < bg.length; p++)
					{
						orig[p] = orig[p] - bg[p];
					}
					baseName = ImageReader.readObjectToImageName(imageData, stackMap);
					actualPath = this.saveProjectedImage(entry, baseName, fpOrig, bitDepth);
					if(actualPath != null)
					{
						dataMap.put(stackMap, actualPath);
					}
				}
			}
			// Status bar
			count = count + 1;
			int percentage = (int) (100 * ((double) count / (double) maps.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), dataMap);
		Dim newDim = null;
		if(slidingWindow)
		{
			newDim = new Dim(dimToProject.name(), dimToProject.valuesUpThrough(dimToProject.size() - slidingWindowSize));
			subDimTable.add(originalDimIndex, newDim);
			output1.setDimTable(subDimTable);
		}
		else
		{
			output1.setDimTable(originalDimTable);
		}
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private String saveProjectedImage(JEXEntry entry, String baseName, FloatProcessor imp, int bitDepth)
	{
		// //// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String finalPath = JEXWriter.saveImage(toSave);
		
		return finalPath;
	}
	
	private FloatProcessor evaluate(ImagePlus virtualStack, String method, int stackSize)
	{
		// :,mean,max,min,sum,std. dev.,median"///
		int methodInt = 5;
		if(method.equals("mean"))
			methodInt = 0;
		else if(method.equals("max"))
			methodInt = 1;
		else if(method.equals("min"))
			methodInt = 2;
		else if(method.equals("sum"))
			methodInt = 3;
		else if(method.equals("std. dev."))
			methodInt = 4;
		else if(method.equals("median"))
			methodInt = 5;
		
		ZProjector p = new ZProjector(virtualStack);
		p.setStartSlice(1);
		p.setStopSlice(stackSize);
		p.setMethod(methodInt);
		p.doProjection();
		return (FloatProcessor) p.getProjection().getProcessor().convertToFloat();
	}
	
	private ImagePlus getVirtualStack(JEXEntry entry, JEXData imageData, List<DimensionMap> maps)
	{
		if(maps.size() == 0)
			return null;
		ImagePlus im = ImageReader.readObjectToImagePlus(imageData);
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		File temp = new File(paths.get(maps.get(0)));
		VirtualStack stack = new VirtualStack(im.getWidth(), im.getHeight(), im.getProcessor().getColorModel(), temp.getParent());
		for (DimensionMap map : maps)
		{
			temp = new File(paths.get(map));
			stack.addSlice(temp.getName());
		}
		ImagePlus ret = new ImagePlus(temp.getPath() + "... ", stack);
		return ret;
	}
	
	private List<DimensionMap> getAllStackMaps(DimensionMap map, Dim dimToProject)
	{
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int i = 0; i < dimToProject.size(); i++)
		{
			ret.add(this.getAStackMap(map, dimToProject, i));
		}
		return ret;
	}
	
	private List<DimensionMap> getSomeStackMaps(DimensionMap map, Dim dimToProject, int slidingWindowSize, int i)
	{
		Dim subDim = this.getSubDim(dimToProject, slidingWindowSize, i);
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int j = 0; j < subDim.size(); j++)
		{
			ret.add(this.getAStackMap(map, subDim, j));
		}
		return ret;
	}
	
	private DimensionMap getAStackMap(DimensionMap map, Dim dimToProject, int indexOfValue)
	{
		DimensionMap ret = map.copy();
		ret.put(dimToProject.name(), dimToProject.valueAt(indexOfValue));
		return ret;
	}
	
	private Dim getSubDim(Dim dimToProject, int slidingWindowSize, int i)
	{
		Dim right = new Dim(dimToProject.name(), dimToProject.valuesStartingAt(i));
		Dim left = new Dim(dimToProject.name(), dimToProject.valuesUpThrough(i + slidingWindowSize - 1));
		return Dim.intersect(left, right);
	}
}

// class AdjustImageHelperFunction implements GraphicalCrunchingEnabling,
// ImagePanelInteractor{
// ImagePanel imagepanel ;
// GraphicalFunctionWrap wrap ;
// DimensionMap[] dimensions ;
// String[] images ;
// ImagePlus im ;
// FloatProcessor imp ;
// int index = 0 ;
// int atStep = 0 ;
//
// boolean auto = false;
// double oldMin = 0;
// double oldMax = 1000;
// double newMin = 0;
// double newMax = 100;
// double gamma = 0.5;
// int depth = 16;
//
// ParameterSet params;
// JEXData imset;
// JEXEntry entry;
// String[] outputNames;
// public JEXData output;
//
// AdjustImageHelperFunction(JEXData imset, JEXEntry entry, String[]
// outputNames, ParameterSet parameters){
// // Pass the variables
// this.imset = imset;
// this.params = parameters;
// this.entry = entry;
// this.outputNames = outputNames;
//
// ////// Get params
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int length = map.size();
// images = new String[length];
// dimensions = new DimensionMap[length];
// int i = 0;
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// dimensions[i] = dim;
// images[i] = path;
// i ++;
// }
//
// // Prepare the graphics
// imagepanel = new ImagePanel(this,"Adjust image");
// displayImage(index);
// wrap = new GraphicalFunctionWrap(this,params);
// wrap.addStep(0, "Select roi", new String[]
// {"Automatic","Old Min","Old Max","New Min","New Max","Output Bit Depth"});
// wrap.setInCentralPanel(imagepanel);
// wrap.setDisplayLoopPanel(true);
// }
//
// private void displayImage(int index){
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// }
//
// /**
// * Run the function and open the graphical interface
// * @return the ROI data
// */
// public JEXData doit(){
// if (!auto){
// wrap.start();
// }
// else {
// finishIT();
// }
//
// return output;
// }
//
// public void runStep(int index) {
// // Get the new parameters
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// // prepare the images for calculation
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// adjustImage();
//
// imagepanel.setImage(new ImagePlus("",imp));
// }
// public void runNext(){
// atStep = atStep+1;
// if (atStep > 0) atStep = 0;
// }
// public void runPrevious(){
// atStep = atStep-1;
// if (atStep < 0) atStep = 0;
// }
// public int getStep(){ return atStep;}
//
// public void loopNext(){
// index = index + 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void loopPrevious(){
// index = index - 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void recalculate(){}
//
// public void startIT() {
// wrap.displayUntilStep();
// }
// /**
// * Apply the roi to all other images
// */
// public void finishIT() {
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// output = new DefaultJEXData(JEXData.IMAGE,outputNames[0],"Adjusted image");
//
// // Run the function
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int count = 0;
// int total = map.size();
// JEXStatics.statusBar.setProgressPercentage(0);
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String imagePath = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// File imageFile = new File(imagePath);
// String imageName = imageFile.getName();
//
// // get the image
// im = new ImagePlus(imagePath);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// ////// Begin Actual Function
// adjustImage();
// ////// End Actual Function
//
// ////// Save the results
// String localDir = JEXStatics.jexManager.getLocalFolder(entry);
// String newFileName = FunctionUtility.getNextName(localDir, imageName, "A");
// String newImagePath = localDir + File.separator + newFileName;
// FunctionUtility.imSave(imp, "false", depth, newImagePath);
//
// JEXDataSingle outputds = new DefaultJEXDataSingle();
// outputds.put(JEXDataSingle.FOLDERNAME, localDir);
// outputds.put(JEXDataSingle.FILENAME, newFileName);
// output.addData(dim,outputds);
// Logs.log("Finished processing " + count + " of " + total +
// ".",1,this);
// count++;
//
// // Status bar
// int percentage = (int) (100 * ((double) count/ (double)map.size()));
// JEXStatics.statusBar.setProgressPercentage(percentage);
// }
//
//
// }
//
// private void adjustImage(){
// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
// }
//
//
// public void clickedPoint(Point p) {}
// public void pressedPoint(Point p) {}
// public void mouseMoved(Point p){}
//
// }

