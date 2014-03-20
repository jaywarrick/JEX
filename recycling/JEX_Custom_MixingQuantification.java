package recycling;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Pair;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
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
public class JEX_Custom_MixingQuantification extends ExperimentalDataCrunch {
	
	public JEX_Custom_MixingQuantification()
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
		String result = "Center ROIs";
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
		String result = "Function that allows you to measure characteristics of an image within a roi region (ellipse, polygon, or rectangle only) and output to ARFF file format.";
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
		String toolbox = "Custom image analysis";
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
		inputNames[0] = new TypeName(ROI, "Region ROI");
		inputNames[1] = new TypeName(IMAGE, "Image");
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
		this.defaultOutputNames[0] = new TypeName(JEXData.FILE, "Region Measures");
		
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
		Parameter p0 = new Parameter("Measurement", "Type of measurement to perform", Parameter.DROPDOWN, new String[] { "All", "Mean", "Min,Max", "Median", "Std Dev", "Mean Dev", "x,y", "Area" }, 0);
		// Parameter p1 = new Parameter("Old Min","Image Intensity Value","0.0");
		// Parameter p2 = new Parameter("Old Max","Image Intensity Value","4095.0");
		// Parameter p3 = new Parameter("New Min","Image Intensity Value","0.0");
		// Parameter p4 = new Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new String[] {"8","16","32"},1);
		
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
		JEXData roiData = inputs.get("Region ROI");
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Collect the inputs
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Gather the parameters
		// String measure = this.parameters.getValueOfParameter("Measurement");
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		DimTable roiTable = roiData.getDimTable();
		DimTable unionTable = DimTable.union(roiTable, imageData.getDimTable());
		if(!unionTable.equals(roiTable))
		{
			Logs.log("Image DimTable needs to be the same or be a subset of the ROI DimTable for this function.", 0, this);
			JEXStatics.statusBar.setStatusText("Function failed for entry " + entry.getEntryID() + ". The Image DimTable needs to be the same or be a subset of the ROI DimTable for this function.");
			return false;
		}
		
		TreeMap<DimensionMap,Double> resultsTreeMap = new TreeMap<DimensionMap,Double>();
		ImagePlus im;
		int count = 0;
		int percentage = 0;
		int total = rois.size();
		
		for (DimensionMap map : paths.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			ROIPlus roiToMeasure = rois.get(map);
			if(roiToMeasure == null || roiToMeasure.pointList.size() < 2)
			{
				continue; // This speeds things up for sparse rois
			}
			String path = paths.get(map);
			if(path == null)
			{
				continue; // This speeds things up for sparse imagesets
			}
			im = new ImagePlus(path);
			
			FloatProcessor imp = (FloatProcessor) (im.getProcessor().convertToFloat());
			Pair<ImageStatistics,ImageStatistics> result = this.getBestRoi(imp, roiToMeasure, 30);
			DimensionMap newNewMap = map.copy();
			newNewMap.put("Measurement", "meandev");
			resultsTreeMap.put(newNewMap.copy(), result.p2.mean);
			newNewMap.put("Measurement", "mean");
			resultsTreeMap.put(newNewMap.copy(), result.p1.mean);
			newNewMap.put("Measurement", "median");
			resultsTreeMap.put(newNewMap.copy(), result.p1.median);
			newNewMap.put("Measurement", "dy");
			resultsTreeMap.put(newNewMap.copy(), (double) result.p2.ystart);
			newNewMap.put("Measurement", "mean_median");
			resultsTreeMap.put(newNewMap.copy(), result.p1.stdDev);
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			
		}
		
		String path = JEXTableWriter.writeTable("RegionMeasures", resultsTreeMap);
		
		JEXData output = FileWriter.makeFileObject(this.outputNames[0].getType(), this.outputNames[0].getName(), path);
		
		// Set the outputs
		this.realOutputs.add(output);
		
		// Return status
		return true;
	}
	
	private Pair<ImageStatistics,ImageStatistics> getBestRoi(FloatProcessor imp, ROIPlus roi, int dy)
	{
		TreeMap<Double,Pair<ImageStatistics,ImageStatistics>> results = new TreeMap<Double,Pair<ImageStatistics,ImageStatistics>>();
		Pair<ImageStatistics,ImageStatistics> temp = null;
		for (int i = 0; i <= dy; i++)
		{
			temp = this.getRoiData(imp, roi, i);
			double delta = Math.abs(temp.p1.mean - temp.p1.median);
			results.put(delta, temp);
			temp = this.getRoiData(imp, roi, -1 * i);
			delta = Math.abs(temp.p1.mean - temp.p1.median);
			results.put(delta, temp);
		}
		return results.firstEntry().getValue();
	}
	
	private Pair<ImageStatistics,ImageStatistics> getRoiData(FloatProcessor imp, ROIPlus roi2, int dy)
	{
		ROIPlus roi = roi2.copy();
		roi.pointList.translate(0, dy);
		Roi imageJRoi = roi.getRoi();
		imp.setRoi(imageJRoi);
		ImageStatistics stats = ImageStatistics.getStatistics(imp, Measurements.MEAN + Measurements.MEDIAN, null);
		stats.stdDev = (stats.mean - stats.median);
		FloatProcessor temp = new FloatProcessor(roi.pointList.getBounds().width, roi.pointList.getBounds().height);
		temp.copyBits(imp, -1 * roi.pointList.getBounds().x, -1 * roi.pointList.getBounds().y, Blitter.COPY);
		temp.add(-1 * stats.mean);
		temp.abs();
		// temp.resetMinAndMax();
		// String path = JEXWriter.saveImage(temp);
		// try
		// {
		// FileUtility.openFileDefaultApplication(path);
		// }
		// catch (Exception e)
		// {
		// e.printStackTrace();
		// }
		ImageStatistics tempStats = ImageStatistics.getStatistics(temp, Measurements.MEAN, null);
		tempStats.ystart = dy;
		
		imp = null;
		
		return new Pair<ImageStatistics,ImageStatistics>(stats, tempStats);
	}
	
}
