package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.VirtualStack;
import ij.measure.Measurements;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.awt.Rectangle;
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.LSVList;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.LabelReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
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
public class JEX_OscillatoryAdhesionAnalysis extends ExperimentalDataCrunch {
	
	public JEX_OscillatoryAdhesionAnalysis()
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
		String result = "Oscillatory Adhesion Analysis";
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
		String result = "Use the filtered images to obtain a baseline measurement for each frame to average and output as a value along with the value of the thresholded max stack projection.";
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
		inputNames[0] = new TypeName(IMAGE, "Filtered Timelapse Images");
		inputNames[1] = new TypeName(ROI, "ROI");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Analyzed Timelapse Images");
		defaultOutputNames[1] = new TypeName(IMAGE, "Thresholded Timelapse Images");
		defaultOutputNames[2] = new TypeName(FILE, "Octave Summary File");
		
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
		Parameter p0 = new Parameter("Threshold", "Threshold to make binary image of streaks for calculations.", "1000.0");
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
	@SuppressWarnings("unchecked")
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData imageData = inputs.get("Filtered Timelapse Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		DimTable originalDimTable = imageData.getDimTable().copy();
		TreeMap<DimensionMap,String> imagePaths = ImageReader.readObjectToImagePathTable(imageData);
		
		// Collect the inputs
		JEXData roiData = inputs.get("ROI");
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		
		// Collect Parameters
		String dimName = "Z";
		double threshold = Double.parseDouble(parameters.getValueOfParameter("Threshold"));
		
		// Collect fStart and fEnd Labels if present and return false if not;
		double fStart = 0;
		JEXData fStartData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, "fStart"), entry);
		if(fStartData != null)
		{
			fStart = Double.parseDouble(LabelReader.readLabelValue(fStartData));
		}
		else
		{
			return false;
		}
		double fEnd = 0;
		JEXData fEndData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, "fEnd"), entry);
		if(fEndData != null)
		{
			fEnd = Double.parseDouble(LabelReader.readLabelValue(fEndData));
		}
		else
		{
			return false;
		}
		
		// Run the function
		Dim dimToProject = originalDimTable.getDimWithName(dimName);
		if(dimToProject == null)
			return false;
		DimTable subDimTable = originalDimTable.copy();
		int originalDimIndex = subDimTable.indexOfDimWithName(dimToProject.name());
		subDimTable.remove(originalDimIndex);
		
		List<DimensionMap> maps = subDimTable.getDimensionMaps();
		TreeMap<DimensionMap,String> imageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> threshMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> baselineMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> streakMap = new TreeMap<DimensionMap,String>();
		DecimalFormat formatD = new DecimalFormat("##0.000");
		int bitDepth = 32;
		FloatProcessor fpProjection;
		int total = 0, index = 0, count = 0;
		ImagePlus statsIm = null;
		ImageStatistics stats;
		double streak, baseline, area = 0;
		ImagePlus stackToProject = null;
		String projectedImagePath = null;
		int numSlices = 0;
		for (DimensionMap map : maps)
		{
			// Project and threshold the stack for this map
			List<DimensionMap> stackMaps = this.getAllStackMaps(map, dimToProject);
			total = maps.size() * stackMaps.size();
			count = index * stackMaps.size();
			if(stackToProject != null)
				stackToProject.flush();
			
			// Put information into stackToProject and get projection
			stackToProject = getVirtualStack(imagePaths, stackMaps);
			numSlices = stackToProject.getStackSize();
			bitDepth = stackToProject.getBitDepth();
			fpProjection = evaluate(stackToProject, "max", numSlices);
			projectedImagePath = saveImage(fpProjection, bitDepth);
			FunctionUtility.imThresh(fpProjection, threshold, false);
			
			// get the average baseline for the stack
			// Results are... Object[] = {average intensity in roi for this
			// stack, thresholded projected Image}
			Object[] averagingResults = getAverage(entry, imageData, stackMaps, threshold, roiData, count, total);
			baseline = (Double) averagingResults[0];
			
			threshMap.putAll((TreeMap<DimensionMap,String>) averagingResults[1]);
			
			// get the streak measurement
			if(statsIm != null)
				statsIm.flush();
			statsIm = new ImagePlus("Stats Image", fpProjection);
			statsIm.setRoi(roiMap.get(stackMaps.get(0)).getRoi());
			stats = statsIm.getStatistics(Measurements.MEAN + Measurements.AREA);
			streak = stats.mean;
			area = stats.area;
			
			// store results
			imageMap.put(map, projectedImagePath);
			baselineMap.put(map, formatD.format(baseline));
			streakMap.put(map, formatD.format(streak));
			
			index = index + 1;
			if(stackToProject != null)
				stackToProject.flush();
			if(statsIm != null)
				statsIm.flush();
		}
		
		String filePath = this.writeMFile(entry, subDimTable, baselineMap, streakMap, area, fStart, fEnd);
		if(filePath == null)
			return false;
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), imageMap);
		JEXData output2 = ImageWriter.makeImageStackFromPaths(outputNames[1].getName(), threshMap);
		JEXData output3 = FileWriter.makeFileObject(outputNames[2].getName(), null, filePath);
		output1.setDimTable(subDimTable);
		output2.setDimTable(originalDimTable);
		output3.setDimTable(subDimTable);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
	
	/**
	 * Takes each frame of each stack applies the threshold and gets the intensity Returns result in a 2 object Object[] = {average intensity in roi for each stack, thresholded images}
	 * 
	 * @param imageData
	 * @param stackMaps
	 * @param thresh
	 * @param regionToAnalyze
	 * @return
	 */
	private static Object[] getAverage(JEXEntry entry, JEXData imageData, List<DimensionMap> stackMaps, double thresh, JEXData regionToAnalyze, int statusIndex, int statusTotal)
	{
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(regionToAnalyze);
		TreeMap<DimensionMap,String> imageMap = new TreeMap<DimensionMap,String>();
		ImagePlus imAvg = null;
		FloatProcessor impAvg;
		ImageStatistics stats;
		double count = 0, avgCount = 0;
		double runningTotal = 0;
		String threshImPath = null;
		ROIPlus errorDetectionRoi = null;
		for (DimensionMap map : stackMaps)
		{
			if(imAvg != null)
				imAvg.flush();
			String path = paths.get(map);
			if(path != null)
			{
				imAvg = new ImagePlus(paths.get(map));
				if(errorDetectionRoi == null)
				{
					Rectangle r = new Rectangle(imAvg.getWidth(), 100);
					errorDetectionRoi = new ROIPlus(r);
				}
				// Get the errorDetection measurement
				impAvg = (FloatProcessor) imAvg.getProcessor().convertToFloat();
				imAvg.setRoi(rois.get(map).getRoi());
				stats = imAvg.getStatistics(Measurements.MEAN);
				
				// Get the baseline measurement and thresholded image
				// Only take measurement if error measurement is below threshold
				FunctionUtility.imThresh(impAvg, thresh, false);
				imAvg.flush();
				imAvg.setProcessor(impAvg);
				imAvg.setRoi(rois.get(map).getRoi());
				stats = imAvg.getStatistics(Measurements.MEAN);
				runningTotal = runningTotal + stats.mean;
				threshImPath = saveImage(impAvg, 8);
				imageMap.put(map, threshImPath);
				avgCount = avgCount + 1;
			}
			else
			{
				Logs.log("Couldn't find image for: " + map, 0, "JEX_OscillatoryAdhesionAnalysis");
			}
			count = count + 1;
			int percentage = (int) (100 * ((statusIndex + count) / statusTotal));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(imAvg != null)
			imAvg.flush();
		return new Object[] { new Double(runningTotal / avgCount), imageMap };
	}
	
	private String writeMFile(JEXEntry entry, DimTable subDimTable, TreeMap<DimensionMap,String> baselineMap, TreeMap<DimensionMap,String> streakMap, double area, double fStart, double fEnd)
	{
		CSVList baseline = new CSVList();
		CSVList streak = new CSVList();
		CSVList time = new CSVList();
		List<DimensionMap> maps = subDimTable.getDimensionMaps();
		for (DimensionMap map : maps)
		{
			streak.add(streakMap.get(map));
			baseline.add(baselineMap.get(map));
			time.add(map.get("T"));
		}
		
		String fStartString = "fStart = " + fStart + ";";
		String fEndString = "fEnd = " + fEnd + ";";
		String streakString = "streak = [" + streak.toSVString() + "];";
		String baselineString = "baseline = [" + baseline.toSVString() + "];";
		String timeString = "time = [" + time.toSVString() + "];";
		String areaString = "area = " + area + ";";
		String calculateString = "signal = streak./baseline;";
		String calibrationString = "cal = 100;";
		String plotString = "plotLogData(strea, baseline, time, area, '-r', [], fStart, fEnd, duration, cal);";
		
		LSVList out = new LSVList();
		out.add(fStartString);
		out.add(fEndString);
		out.add(streakString);
		out.add(baselineString);
		out.add(timeString);
		out.add(areaString);
		out.add(calculateString);
		out.add(calibrationString);
		out.add(plotString);
		
		String fullPath = JEXWriter.saveText(out.toString(), "m");
		return fullPath;
	}
	
	/**
	 * Saves the image in the database temp folder
	 * 
	 * @param imp
	 * @param bitDepth
	 * @return
	 */
	private static String saveImage(FloatProcessor imp, int bitDepth)
	{
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String finalPath = JEXWriter.saveImage(toSave);
		Logs.log("Writing image to: " + finalPath, 1, "JEX_OscillatoryAdhesionAnalysis");
		return finalPath;
	}
	
	private static FloatProcessor evaluate(ImagePlus virtualStack, String method, int stackSize)
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
	
	private static ImagePlus getVirtualStack(TreeMap<DimensionMap,String> imagePaths, List<DimensionMap> maps)
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
