package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ROIUtility;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
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
public class JEX_ThresholdBGNoise extends ExperimentalDataCrunch {
	
	public JEX_ThresholdBGNoise()
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
		String result = "Threshold Background Noise";
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
		String result = "Threshold based on the background noise and a set multiple of sigma above the noise.";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(ROI, "Roi (Optional)");
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
		this.defaultOutputNames = new TypeName[3];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Thresholded Image");
		this.defaultOutputNames[1] = new TypeName(FILE, "Stats");
		this.defaultOutputNames[2] = new TypeName(FILE, "Thresholds");
		
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
		Parameter p1 = new Parameter("Color Dim Name", "Name of the color dimension if any. If not found, it is assumed there is no color dimension.", "Color");
		Parameter p2 = new Parameter("Number of Sigma", "How many multiples of of sigma above background should the threshold be set?", "5");
		Parameter p3 = new Parameter("Single Threshold per Color?", "Calculate a single threhsold for each color or a threshold for each image in data set. The combined thresh is calcualted as the median of the individual thresholds.", Parameter.CHECKBOX, false);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		JEXData roiData = inputs.get("Roi (Optional)");
		TreeMap<DimensionMap,ROIPlus> rois = new TreeMap<DimensionMap,ROIPlus>();
		if(roiData != null && roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			rois = RoiReader.readObjectToRoiMap(roiData);
		}
		
		// Gather parameters
		String colorDimName = this.parameters.getValueOfParameter("Color Dim Name");
		double nSigma = Double.parseDouble(this.parameters.getValueOfParameter("Number of Sigma"));
		boolean threshPerColor = Boolean.parseBoolean(this.parameters.getValueOfParameter("Single Threshold per Color?"));
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		
		TreeMap<String,Object> output = null;
		if(threshPerColor)
		{
			output = this.calcPerColor(colorDimName, imageMap, imageData.getDimTable(), rois, nSigma);
		}
		else
		{
			output = this.calcIndividual(true, imageMap, imageData.getDimTable(), rois, nSigma);
		}
		
		if(output == null || !(Boolean) output.get("Success"))
		{
			return false;
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), (TreeMap<DimensionMap,String>) output.get("outputMap"));
		String valuePath2 = JEXTableWriter.writeTable(this.outputNames[1].getName(), (TreeMap<DimensionMap,Double>) output.get("statsMap"));
		JEXData output2 = FileWriter.makeFileObject(this.outputNames[1].getName(), valuePath2);
		String valuePath3 = JEXTableWriter.writeTable(this.outputNames[2].getName(), (TreeMap<DimensionMap,Double>) output.get("outputThreshMap"));
		JEXData output3 = FileWriter.makeFileObject(this.outputNames[2].getName(), valuePath3);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		
		// Return status
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public TreeMap<String,Object> calcPerColor(String colorDimName, TreeMap<DimensionMap,String> imageMap, DimTable table, TreeMap<DimensionMap,ROIPlus> rois, double nSigma)
	{
		Dim colorDim = table.getDimWithName(colorDimName);
		if(colorDim == null)
		{
			return this.calcIndividual(true, imageMap, table, rois, nSigma);
		}
		
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> statsMap = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> outputThreshMap = new TreeMap<DimensionMap,Double>();
		boolean success = true;
		TreeMap<String,Object> temp = null;
		for (DimTable subTable : table.getSubTableIterator(colorDimName))
		{
			temp = this.calcIndividual(false, imageMap, subTable, rois, nSigma);
			Dim colorSubDim = subTable.getDimWithName(colorDimName);
			
			// Get the median threshold
			TreeMap<DimensionMap,Double> thresholds = (TreeMap<DimensionMap,Double>) temp.get("outputThreshMap");
			Double thresh = StatisticsUtility.median(thresholds.values());
			
			// Threshold all images for this subtable using the threshold
			for (DimensionMap map : subTable.getMapIterator())
			{
				// Get the image
				ImagePlus im = new ImagePlus(imageMap.get(map));
				FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();
				
				// Threshold it
				FunctionUtility.imThresh(ip, thresh, false);
				if(this.isCanceled())
				{
					success = false;
					return this.makeTreeMap("Success", false);
				}
				String path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, "false", 8)); // Creating black and white image
				outputMap.put(map.copy(), path);
			}
			
			// Combine rest of new data with current output data
			statsMap.putAll((TreeMap<DimensionMap,Double>) temp.get("statsMap"));
			outputThreshMap.put(new DimensionMap(colorSubDim.dimName + "=" + colorSubDim.valueAt(0)), thresh);
		}
		
		return this.makeTreeMap("Success,outputMap,statsMap,outputThreshMap", success, outputMap, statsMap, outputThreshMap);
	}
	
	public TreeMap<String,Object> calcIndividual(boolean threshImages, TreeMap<DimensionMap,String> imageMap, DimTable dimsToProcess, TreeMap<DimensionMap,ROIPlus> rois, double nSigma)
	{
		int percentage = 0;
		double count = 0, total = dimsToProcess.mapCount();
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> statsMap = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> outputThreshMap = new TreeMap<DimensionMap,Double>();
		boolean success = true;
		for (DimensionMap map : dimsToProcess.getMapIterator())
		{
			if(this.isCanceled())
			{
				return this.makeTreeMap("Success", false);
			}
			
			// Get the image
			ImagePlus im = new ImagePlus(imageMap.get(map));
			FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();
			
			// Do threshold
			ROIPlus roi = rois.get(map);
			float[] tempPixels = null;
			if(roi != null)
			{
				tempPixels = ROIUtility.getPixelsInRoi(ip, roi);
			}
			if(tempPixels == null)
			{
				tempPixels = (float[]) ip.getPixels();
			}
			double[] pixels = new double[tempPixels.length];
			int i = 0;
			for (float f : tempPixels)
			{
				pixels[i] = f;
				i++;
			}
			tempPixels = null;
			double med = StatisticsUtility.median(pixels);
			if(this.isCanceled())
			{
				return this.makeTreeMap("Success", false);
			}
			double mad = StatisticsUtility.mad(med, pixels); // Multiplier converts the mad to an approximation of the standard deviation without the effects of outliers
			double threshold = med + nSigma * mad;
			String path = null;
			if(threshImages)
			{
				FunctionUtility.imThresh(ip, threshold, false);
				if(this.isCanceled())
				{
					success = false;
					return this.makeTreeMap("Success", false);
				}
				path = JEXWriter.saveImage(FunctionUtility.makeImageToSave(ip, "false", 8)); // Creating black and white image
			}
			
			if(path != null)
			{
				outputMap.put(map, path);
			}
			DimensionMap map2 = map.copy();
			map2.put("Measurement", "Median");
			statsMap.put(map2.copy(), med);
			map2.put("Measurement", "MAD");
			statsMap.put(map2.copy(), mad);
			outputThreshMap.put(map, threshold);
			
			// Update progress
			count = count + 1;
			percentage = (int) (100 * (count / total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		TreeMap<String,Object> ret = this.makeTreeMap("Success,outputMap,statsMap,outputThreshMap", success, outputMap, statsMap, outputThreshMap);
		return ret;
	}
	
	public TreeMap<String,Object> makeTreeMap(String csvString, Object... items)
	{
		CSVList names = new CSVList(csvString);
		if(names.size() != items.length)
		{
			return null;
		}
		TreeMap<String,Object> ret = new TreeMap<String,Object>();
		for (int i = 0; i < names.size(); i++)
		{
			ret.put(names.get(i), items[i]);
		}
		return ret;
	}
}
