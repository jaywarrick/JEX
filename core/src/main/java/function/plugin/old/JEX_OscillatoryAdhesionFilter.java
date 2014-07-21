package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import image.roi.ROIPlus;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimTable;
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
public class JEX_OscillatoryAdhesionFilter extends JEXCrunchable {
	
	public JEX_OscillatoryAdhesionFilter()
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
		String result = "Oscillatory Adhesion Filter";
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
		String result = "Subtract original background (median projection) from each image at each time point and do max projection for each timepoint.";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(IMAGE, "Timelapse Images");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Filtered Timelapse");
		
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
		Parameter p0 = new Parameter("Outlier p-value", "Statistical significance for determining an outlier", "0.01");
		// Parameter p0 = new
		// Parameter("Get Background From Timpoint i","Choose the timepoint to get background information from.","0");//
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Parameter to indicate that you have selected this function",FormLine.DROPDOWN,new
		// String[]{"true"},0);
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
		JEXData imageData = inputs.get("Timelapse Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		TreeMap<DimensionMap,String> imagePaths = ImageReader.readObjectToImagePathTable(imageData);
		DimTable originalDimTable = imageData.getDimTable().copy();
		
		// Collect Parameters
		String dimName = "Z";
		double alpha = Double.parseDouble(parameters.getValueOfParameter("Outlier p-value"));
		// int bgTimePoint =
		// Integer.parseInt(parameters.getValueOfParameter("Get Background From Timpoint i"));
		
		// Get the timelapse dimTable without the Z dim and call it subDimTable
		Dim dimToProject = originalDimTable.getDimWithName(dimName);
		if(dimToProject == null)
			return false;
		DimTable subDimTable = originalDimTable.copy();
		int originalDimIndex = subDimTable.indexOfDimWithName(dimToProject.name());
		subDimTable.remove(originalDimIndex);
		
		// Get the list of dimension maps for the subDimtTable
		List<DimensionMap> maps = subDimTable.getDimensionMaps();
		
		// Initialize variables for looping
		TreeMap<DimensionMap,String> dataMap = new TreeMap<DimensionMap,String>();
		String actualPath;
		FloatProcessor fpBG, fpOrig;
		ShortProcessor shortOrig;
		int total = 0, count = 0, percentage = 0;
		
		// Get the background image from the first timepoint.
		float[] bg = null;
		List<DimensionMap> stackMaps = this.getAllStackMaps(maps.get(0), dimToProject);
		List<DimensionMap> stackMapsCopy = new Vector<DimensionMap>();
		stackMapsCopy.addAll(stackMaps);
		total = (maps.size() + 1) * stackMaps.size();
		cullImages(imagePaths, stackMapsCopy, alpha, total);
		ImagePlus stackToProject = getVirtualStack(imagePaths, stackMapsCopy);
		count = stackMaps.size(); // Update counter here after cullImages and
		// stack
		fpBG = this.evaluate(stackToProject, "min", stackMaps.size());
		bg = (float[]) fpBG.getPixels();
		float[] orig;
		ImagePlus temp;
		ImageStatistics stats;
		List<Double> imMeans = new Vector<Double>(0);
		int outlier;
		Rectangle r = new Rectangle(0, 0, 12, 12);
		ROIPlus roi = new ROIPlus(r);
		Roi imageJRoi = roi.getRoi();
		for (DimensionMap map : maps)
		{
			stackMaps = this.getAllStackMaps(map, dimToProject);
			stackMapsCopy.clear();
			stackMapsCopy.addAll(stackMaps);
			imMeans.clear();
			for (DimensionMap stackMap : stackMaps)
			{
				String path = imagePaths.get(stackMap);
				if(path != null)
				{
					temp = new ImagePlus(path);
					fpOrig = (FloatProcessor) temp.getProcessor().convertToFloat();
					temp.setProcessor(fpOrig);
					temp.setRoi(imageJRoi);
					stats = temp.getStatistics(Measurements.MEAN);
					imMeans.add(stats.mean);
					orig = (float[]) fpOrig.getPixels();
					for (int p = 0; p < bg.length; p++)
					{
						orig[p] = orig[p] - bg[p];
					}
					shortOrig = (ShortProcessor) fpOrig.convertToShort(false);
					actualPath = JEXWriter.saveImage(shortOrig);
					if(actualPath != null)
					{
						dataMap.put(stackMap, actualPath);
					}
					fpOrig = null;
					shortOrig = null;
					orig = null;
					temp.flush();
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / (double) total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			
			// Remove outlier background subtracted images.
			outlier = StatisticsUtility.getOutlier(imMeans, alpha);
			while (outlier != -1)
			{
				dataMap.remove(stackMapsCopy.get(outlier));
				stackMapsCopy.remove(outlier);
				imMeans.remove(outlier);
				outlier = StatisticsUtility.getOutlier(imMeans, alpha);
			}
		}
		bg = null;
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), dataMap);
		output1.setDimTable(originalDimTable);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	/**
	 * Removes the DimensionMaps from stackMaps whos images meet the criteria of an outlier
	 * 
	 * @param imageMap
	 * @param maps
	 * @param alpha
	 */
	private static void cullImages(TreeMap<DimensionMap,String> imagePaths, List<DimensionMap> stackMaps, double alpha, int totalFiles)
	{
		if(imagePaths.size() == 0 || stackMaps.size() == 0)
			return;
		ImagePlus im;
		ImageStatistics stats;
		List<Double> imMeans = new Vector<Double>(0);
		String path;
		Rectangle r = new Rectangle(0, 0, 12, 12);
		ROIPlus roi = new ROIPlus(r);
		Roi imageJRoi = roi.getRoi();
		int outlier, percentage, counter = 0;
		for (DimensionMap map : stackMaps)
		{
			path = imagePaths.get(map);
			if(path != null)
			{
				im = new ImagePlus(path);
				im.setRoi(imageJRoi);
				stats = im.getStatistics(Measurements.MEAN);
				imMeans.add(stats.mean);
				im.flush();
			}
			counter = counter + 1;
			percentage = (int) (100 * ((double) (counter) / (double) totalFiles));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		outlier = StatisticsUtility.getOutlier(imMeans, alpha);
		while (outlier != -1)
		{
			stackMaps.remove(outlier);
			imMeans.remove(outlier);
			outlier = StatisticsUtility.getOutlier(imMeans, alpha);
		}
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
	
	private ImagePlus getVirtualStack(TreeMap<DimensionMap,String> imagePaths, List<DimensionMap> maps)
	{
		if(maps.size() == 0 || imagePaths.size() == 0)
			return null;
		String firstPath = imagePaths.firstEntry().getValue();
		ImagePlus im = new ImagePlus(firstPath);
		File temp = new File(firstPath);
		VirtualStack stack = new VirtualStack(im.getWidth(), im.getHeight(), im.getProcessor().getColorModel(), temp.getParent());
		for (DimensionMap map : maps)
		{
			String path = imagePaths.get(map);
			if(path != null)
			{
				temp = new File(imagePaths.get(map));
				stack.addSlice(temp.getName());
			}
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
	
	private DimensionMap getAStackMap(DimensionMap map, Dim dimToProject, int indexOfValue)
	{
		DimensionMap ret = map.copy();
		ret.put(dimToProject.name(), dimToProject.valueAt(indexOfValue));
		return ret;
	}
}
