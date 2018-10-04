package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.DataWindow;
import jex.utilities.DataWindows;
import miscellaneous.Pair;
import rtools.R;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;

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
public class JEX_MakeFACSTimeCoursePlots extends JEXCrunchable {
	
	public JEX_MakeFACSTimeCoursePlots()
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
		String result = "Make FACS Timecourse Plots";
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
		String result = "Make a plot of the expression timecourse for each color for a specified percent of the total number of cells";
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
		String toolbox = "Table Tools";
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
		inputNames[0] = new TypeName(FILE, "ARFF Files");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Timecourse Plots");
		
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
		// Octave file is the following
		// [outPath] = makeFACSPlot(inPath, outPath, colorName, colorx, xmin,
		// xmax, xbins, colory, ymin, ymax, ybins)
		
		// Parameter p0 = new
		// Parameter("Working Directory","Desired R working directory","/Users/warrick/Desktop/R Scripts/CellTracking");
		// Parameter p1 = new
		// Parameter("Octave Binary File","Location of the Octave binary file","/Applications/Octave.app/Contents/Resources/bin/octave");
		Parameter p2 = new Parameter("'Time' Dim Name", "Name of 'Time' dim", "Time");
		Parameter p4 = new Parameter("'Color' Dim Name", "Name of 'Color' dim", "Color");
		Parameter p5 = new Parameter("Skip N Cells", "The percent of the total number of cells for which to plot timecourses", "0");
		Parameter p6 = new Parameter("Variable Shading?", "Whether to allow the shade of the lines to vary to enhance visibility of individual tracks", Parameter.DROPDOWN, new String[] { "True", "False" }, 0);
		Parameter p6b = new Parameter("Normalized?", "Whether to normalize all the values to be between 0 and 1 (min and max)", Parameter.DROPDOWN, new String[] { "True", "False" }, 1);
		Parameter p7 = new Parameter("X Axis Label", "Label for the x axis", "X");
		Parameter p9 = new Parameter("Y Axis Label", "Label for the y axis", "Y");
		Parameter p10 = new Parameter("Time Start", "Start time in [hpi]", "0");
		Parameter p10b = new Parameter("Time Interval", "Time interval in [hpi] between frames", "0.25");
		Parameter p11 = new Parameter("Number of Frames", "Size of the rolling window average in number of frames", "1");
		// Parameter p13 = new Parameter("Measurement Name",
		// "Name of the measurement attribute","Measurement");
		// Parameter p14 = new Parameter("Measurement Value",
		// "The measurement to plot (Measure Points Roi - 1:Mean, 2:Area, 3:Min, 4:Max, 5:StdDev, 6:Median)",
		// "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p6b);
		parameterArray.addParameter(p7);
		// parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p10b);
		parameterArray.addParameter(p11);
		// parameterArray.addParameter(p12);
		// parameterArray.addParameter(p13);
		// parameterArray.addParameter(p14);
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
	
	public LinkedList<Pair<String,DimTable>> dimTables = new LinkedList<Pair<String,DimTable>>();
	public TreeMap<DimensionMap,Double> data = new TreeMap<DimensionMap,Double>();
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData fileData = inputs.get("ARFF Files");
		if(fileData == null || !fileData.getTypeName().getType().matches(JEXData.FILE))
		{
			return false;
		}
		
		// Gather parameters
		String timeDimName = parameters.getValueOfParameter("'Time' Dim Name");
		String colorDimName = parameters.getValueOfParameter("'Color' Dim Name");
		String xLabel = parameters.getValueOfParameter("X Axis Label");
		String yLabel = parameters.getValueOfParameter("Y Axis Label");
		Integer skipN = Integer.parseInt(parameters.getValueOfParameter("Skip N Cells"));
		Boolean shading = Boolean.parseBoolean(parameters.getValueOfParameter("Variable Shading?"));
		Boolean normalized = Boolean.parseBoolean(parameters.getValueOfParameter("Normalized?"));
		Double timeStart = Double.parseDouble(parameters.getValueOfParameter("Time Start"));
		Double timeInt = Double.parseDouble(parameters.getValueOfParameter("Time Interval"));
		Integer avgFrames = Integer.parseInt(parameters.getValueOfParameter("Number of Frames"));
		
		// Run the function
		TreeMap<DimensionMap,String> outputFileTreeMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> fileTreeMap = FileReader.readObjectToFilePathTable(fileData);
		DimTable fileTable = fileData.getDimTable();
		int count = 0, percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		
		// Get track min and max values;
		TreeMap<String,Double> overallMin = new TreeMap<String,Double>();
		TreeMap<String,Double> overallMax = new TreeMap<String,Double>();
		Table<Double> firstTrack = JEXTableReader.getNumericTable(fileTreeMap.firstEntry().getValue());
		Dim colorDim = firstTrack.dimTable.getDimWithName(colorDimName);
		int colors = colorDim.size();
		Dim timeDim = firstTrack.dimTable.getDimWithName(timeDimName);
		int times = timeDim.size();
		TreeMap<DimensionMap,Double> trackMaxTreeMap = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> trackMinTreeMap = new TreeMap<DimensionMap,Double>();
		for (DimensionMap fileMap : fileTable.getMapIterator(new DimensionMap(), skipN))
		{
			Table<Double> track = JEXTableReader.getNumericTable(fileTreeMap.get(fileMap));
			for (String color : colorDim.dimValues)
			{
				TreeMap<DimensionMap,Double> colorSubset = track.getFilteredData(new DimensionMap("Measurement=1," + colorDimName + "=" + color));
				Pair<Vector<Double>,Vector<Double>> colorSubsetMovingAvg = DataWindow.getMovingAverage(timeDimName, avgFrames, colorSubset);
				Pair<Double,Double> minMax = getMinMax(colorSubsetMovingAvg.p2);
				DimensionMap newMap = fileMap.copy();
				newMap.put(colorDimName, color);
				trackMinTreeMap.put(newMap, minMax.p1);
				trackMaxTreeMap.put(newMap, minMax.p2);
				Double theMin = overallMin.get(color);
				if(theMin == null)
				{
					overallMin.put(color, minMax.p1);
				}
				else if(minMax.p1 < theMin)
				{
					overallMin.put(color, minMax.p1);
				}
				Double theMax = overallMax.get(color);
				if(theMax == null)
				{
					overallMax.put(color, minMax.p2);
				}
				else if(minMax.p2 > theMax)
				{
					overallMax.put(color, minMax.p2);
				}
				count = count + 1 + skipN;
				percentage = (int) (100 * ((double) (count) / ((double) fileTable.mapCount() * colors * 2)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		
		// For each color create a plot
		for (String color : colorDim.dimValues)
		{
			// Start the plot
			String path = R.startPlot("tif", 4, 3, 600, 10, "Arial", "lzw");
			R.eval("par(lwd = 1)");
			R.eval("par(mar = c(3,3,1,1))"); // beyond plot frame [b,l,t,r]
			R.eval("par(mgp = c(2,1,0))"); // placement of axis labels [1],
			// numbers [2] and symbols[3]
			R.eval("par(oma = c(0,0,0,0))"); // cutoff beyond other measures
			R.eval("par(cex = 1)"); // axis label size
			R.eval("par(col = 'black')"); // axis label size
			Double tempXMin, tempYMin, tempXMax, tempYMax;
			tempXMin = 0.0;
			tempXMax = 1.05 * ((Double.parseDouble(timeDim.max())) * timeInt + timeStart);
			tempYMin = 0.95 * overallMin.get(color);
			tempYMax = 1.05 * overallMax.get(color);
			
			R.eval("plot(c(), c(), log='y', axes='F', frame='F', xaxs='i', yaxs='i', xlim=c(" + tempXMin + "," + tempXMax + "), ylim=c(" + tempYMin + "," + tempYMax + "), xlab=" + R.sQuote(xLabel) + ", ylab=" + R.sQuote(yLabel) + ")");
			
			// Plot the timecourse lines
			for (DimensionMap fileMap : fileTable.getMapIterator(new DimensionMap(), skipN))
			{
				Table<Double> track = JEXTableReader.getNumericTable(fileTreeMap.get(fileMap), new DimensionMap("Measurement=1," + colorDimName + "=" + color));
				Pair<Vector<Double>,Vector<Double>> trackMovingAvg = DataWindow.getMovingAverage(timeDimName, avgFrames, track.data);
				R.makeVector("t", trackMovingAvg.p1);
				R.eval("t <- (t*" + timeInt + ") + " + timeStart);
				R.makeVector("y", trackMovingAvg.p2);
				DimensionMap newMap = fileMap.copy();
				newMap.put(colorDimName, color);
				String col = "col = 'black'";
				if(shading || normalized)
				{
					Double trackMax = trackMaxTreeMap.get(newMap);
					Double trackMin = trackMinTreeMap.get(newMap);
					Double shade = 1 - ((double) trackMovingAvg.p1.size()) / ((double) times);
					if(shade > 1)
					{
						shade = 1.0;
					}
					if(shade < 0)
					{
						shade = 0.0;
					}
					if(shading)
					{
						col = "col = gray(" + shade + ")";
					}
					if(normalized)
					{
						R.eval("y <- (y-" + trackMin + ")/(" + (trackMax - trackMin) + ")");
					}
				}
				count = count + 1 + skipN;
				percentage = (int) (100 * ((double) (count) / ((double) fileTable.mapCount() * colors * 2)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				R.eval("lines(t, y, " + col + ", lwd=1)");
			}
			
			// Finish the plot with axes
			R.eval("par(col = 'black')");
			R.eval("axis(1)");
			R.eval("axis(2)");
			R.eval("box()");
			R.endPlot();
			
			// Output the data and update the progress bar
			outputFileTreeMap.put(new DimensionMap(colorDimName + "=" + color), path);
		}
		
		if(outputFileTreeMap.size() == 0)
		{
			return false;
		}
		
		// Save the data
		JEXData output = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputFileTreeMap);
		realOutputs.add(output);
		return true;
	}
	
	public Pair<Double,Double> getMinMax(TreeMap<DimensionMap,Double> colorSubset)
	{
		Double max = -1 * Double.MAX_VALUE;
		Double min = Double.MAX_VALUE;
		for (Entry<DimensionMap,Double> e : colorSubset.entrySet())
		{
			if(e.getValue() > max)
			{
				max = e.getValue();
			}
			if(e.getValue() < min)
			{
				min = e.getValue();
			}
		}
		return new Pair<Double,Double>(min, max);
	}
	
	public Pair<Double,Double> getMinMax(Vector<Double> data)
	{
		Double max = -1 * Double.MAX_VALUE;
		Double min = Double.MAX_VALUE;
		for (Double d : data)
		{
			if(d > max)
			{
				max = d.doubleValue();
			}
			if(d < min)
			{
				min = d.doubleValue();
			}
		}
		return new Pair<Double,Double>(min, max);
	}
	
	public Pair<DimensionMap,String> writeAvgTimePoint(DataWindows tracks, Dim trackDim, Dim timeDim, String timeFormat)
	{
		// "##0.00" time format
		TreeMap<DimensionMap,Double> avgData = new TreeMap<DimensionMap,Double>();
		DimensionMap dataPointMap = new DimensionMap();
		DecimalFormat formatD = new DecimalFormat(timeFormat);
		int count = 0, percentage = 0, total = trackDim.size();
		JEXStatics.statusBar.setStatusText("Calculating: 0%");
		for (String trackNumString : trackDim.dimValues)
		{
			Integer trackNum = Integer.parseInt(trackNumString);
			DataWindow track = tracks.getWindow(trackNum);
			if(track != null && track.isFilled())
			{
				dataPointMap.put(trackDim.name(), trackNumString);
				dataPointMap.put("Measurement", "x");
				avgData.put(dataPointMap.copy(), track.avgX());
				dataPointMap.put("Measurement", "y");
				avgData.put(dataPointMap.copy(), track.avgY());
			}
			count = count + 1;
			percentage = (int) (100 * ((double) count) / ((double) total));
			JEXStatics.statusBar.setStatusText("Calculating: " + percentage + "%");
		}
		if(avgData.size() == 0)
		{
			return null;
		}
		
		// create the data table for writing
		DimTable newDimTable = new DimTable();
		newDimTable.add(trackDim.copy());
		Dim measurementDim = new Dim("Measurement", new String[] { "x", "y" });
		newDimTable.add(measurementDim);
		Table<Double> table = new Table<Double>(newDimTable, avgData);
		
		// write the data and store the path and time stamp
		String tablePath = JEXTableWriter.writeTable("AvgData", table);
		DimensionMap retDim = new DimensionMap();
		Double startFrame = Double.parseDouble(timeDim.dimValues.get(0));
		retDim.put(timeDim.name(), formatD.format(startFrame + tracks.getAvgIndex()));
		return new Pair<DimensionMap,String>(retDim, tablePath);
	}
}