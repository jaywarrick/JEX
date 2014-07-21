package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
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
public class JEX_ImageTools_MeasureROIRegion extends JEXCrunchable {
	
	public JEX_ImageTools_MeasureROIRegion()
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
		String result = "Measure Roi Region";
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
		String result = "Function that allows you to measure characteristics of an image within a roi region (ellipse, polygon, or rectangle only).";
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
		this.defaultOutputNames = new TypeName[6];
		this.defaultOutputNames[0] = new TypeName(VALUE, "Mean");
		this.defaultOutputNames[1] = new TypeName(VALUE, "Area");
		this.defaultOutputNames[2] = new TypeName(VALUE, "Min");
		this.defaultOutputNames[3] = new TypeName(VALUE, "Max");
		this.defaultOutputNames[4] = new TypeName(VALUE, "Std Dev");
		this.defaultOutputNames[5] = new TypeName(VALUE, "Median");
		
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
		Parameter p0 = new Parameter("Dummy Parameter", "Allows user to see when function has been chosen.", Parameter.DROPDOWN, new String[] { "true" }, 0);
		// Parameter p1 = new
		// Parameter("Old Min","Image Intensity Value","0.0");
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
		
		TreeMap<DimensionMap,String> meanMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> areaMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> minMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> maxMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> stdDevMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> medianMap = new TreeMap<DimensionMap,String>();
		ROIPlus roi, baseRoi;
		Roi imageJRoi;
		ImageStatistics stats;
		ImagePlus im;
		DecimalFormat formatD = new DecimalFormat("##0.000");
		int count = 0;
		int percentage = 0;
		List<DimensionMap> maps = roiTable.getDimensionMaps();
		int total = maps.size();
		
		HashMap<String,HashSet<DimensionMap>> roisOrganizedByImage = new HashMap<String,HashSet<DimensionMap>>();
		for (DimensionMap map : maps)
		{
			String path = paths.get(map);
			if(path == null)
			{
				continue;
			}
			HashSet<DimensionMap> curSet = roisOrganizedByImage.get(path);
			if(curSet == null)
			{
				curSet = new HashSet<DimensionMap>();
			}
			curSet.add(map);
			roisOrganizedByImage.put(path, curSet);
		}
		
		// Get a dim name hold info from a roi pattern if necessary
		String patternDimName = this.getNextDimName("ROI", roiTable);
		boolean hasPattern = false, atLeastOneHasPattern = false;
		int maxPatternSize = 1;
		for (String path : roisOrganizedByImage.keySet())
		{
			im = new ImagePlus(path);
			for (DimensionMap map : roisOrganizedByImage.get(path))
			{
				if(this.isCanceled())
				{
					return false;
				}
				baseRoi = rois.get(map);
				if(baseRoi == null)
				{
					// Sometimes an roi doesn't exist for the map and that's ok.
					// Just skip the measurement.
					continue;
				}
				
				// Check to see if there is a pattern defined for this roi that
				// should be taken into consideration
				hasPattern = baseRoi.getPattern().size() > 1;
				if(hasPattern)
				{
					atLeastOneHasPattern = true;
					if(baseRoi.getPattern().size() > maxPatternSize)
					{
						maxPatternSize = baseRoi.getPattern().size();
					}
				}
				
				Iterator<ROIPlus> itr = baseRoi.patternRoiIterator();
				int patternCount = 1;
				while (itr.hasNext())
				{
					roi = itr.next();
					imageJRoi = roi.getRoi();
					im.setRoi(imageJRoi);
					stats = im.getStatistics(Measurements.MEAN + Measurements.AREA + Measurements.MIN_MAX + Measurements.STD_DEV + Measurements.MEDIAN);
					
					DimensionMap newMap = map.copy();
					if(hasPattern)
					{
						newMap.put(patternDimName, "" + patternCount);
					}
					
					meanMap.put(newMap, formatD.format(stats.mean));
					areaMap.put(newMap, formatD.format(stats.area));
					minMap.put(newMap, formatD.format(stats.min));
					maxMap.put(newMap, formatD.format(stats.max));
					stdDevMap.put(newMap, formatD.format(stats.stdDev));
					medianMap.put(newMap, formatD.format(stats.median));
					
					patternCount = patternCount + 1;
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / (double) total));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
			}
			
		}
		
		JEXData output1 = ValueWriter.makeValueTable(this.outputNames[0].getName(), meanMap);
		JEXData output2 = ValueWriter.makeValueTable(this.outputNames[1].getName(), areaMap);
		JEXData output3 = ValueWriter.makeValueTable(this.outputNames[2].getName(), minMap);
		JEXData output4 = ValueWriter.makeValueTable(this.outputNames[3].getName(), maxMap);
		JEXData output5 = ValueWriter.makeValueTable(this.outputNames[4].getName(), stdDevMap);
		JEXData output6 = ValueWriter.makeValueTable(this.outputNames[5].getName(), medianMap);
		
		DimTable dTable = roiTable.copy();
		if(atLeastOneHasPattern)
		{
			Dim d = new Dim(patternDimName, 1, maxPatternSize);
			dTable.add(d);
		}
		output1.setDimTable(dTable);
		output2.setDimTable(dTable);
		output3.setDimTable(dTable);
		output4.setDimTable(dTable);
		output5.setDimTable(dTable);
		output6.setDimTable(dTable);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		this.realOutputs.add(output4);
		this.realOutputs.add(output5);
		this.realOutputs.add(output6);
		
		// Return status
		return true;
	}
	
	private String getNextDimName(String baseName, DimTable roiTable)
	{
		boolean foundUniqueName = false;
		String curName = baseName;
		Dim retDim = roiTable.getDimWithName(baseName);
		if(retDim != null)
		{
			int i = 2;
			while (!foundUniqueName)
			{
				curName = baseName + i;
				if(roiTable.getDimWithName(curName) == null)
				{
					foundUniqueName = true;
				}
				i++;
			}
		}
		return curName;
	}
}
