package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.singleCellAnalysis.SingleCellUtility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import rtools.R;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;

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
public class JEX_SingleCell_IndividualHistograms extends JEXCrunchable {
	
	public JEX_SingleCell_IndividualHistograms()
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
		String result = "Make Individual Histograms";
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
		String result = "Make a histogram plot showing multiple times in grayscale from a list of ARFF files.";
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
		inputNames[0] = new TypeName(FILE, "Data File(s)");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Histograms");
		
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
		Parameter p1a = new Parameter("Time Dim Name","Name of the time Dim (typically 'Time' or 'T').","Time");
		Parameter p1b = new Parameter("Measurement Name", "Name of the measurement to create histograms for", "G");
		Parameter p2b = new Parameter("Number of Bins", "Number of bins for the histogram.", "20");
		Parameter p3b = new Parameter("X Ticks", "Places to put and label tick marks.", "1,10,100,1000,10000");
		Parameter p1 = new Parameter("Min Bin", "Minimum bin center", "0");
		Parameter p2 = new Parameter("Max Bin", "Maximum bin center", "4000");
		Parameter p3 = new Parameter("Threshold", "Threshold (before offset, optional)", "5");
		Parameter p4 = new Parameter("Max Normalized Y", "Maximum value of the y-axis ()", "4000");
		Parameter p5 = new Parameter("Offset", "Amount to offset data to avoid zeros.", "10");
		
		Parameter p11 = new Parameter("Width [in]", "Width of plot in INCHES", "4");
		Parameter p12 = new Parameter("Height [in]", "Height of plot in INCHES", "3");
		Parameter p13 = new Parameter("Resolution [ppi]", "Resolution of the plot in PPI", "300");
		Parameter p14 = new Parameter("Time Stamp Start", "Initial time to start time stamp", "0");
		Parameter p15 = new Parameter("Time Stamp Interval", "Time interval between frames", "0.3333");
		Parameter p16 = new Parameter("Time Stamp Units", "Labe for the units of time", "[hpi]");
		Parameter p17 = new Parameter("Number of Frames Per Plot", "Number of frames per plot", "4");
		Parameter p18 = new Parameter("Skip Size", "Number of timepoints to skip between plots.", "0");
		// Parameter p19 = new
		// Parameter("Pt Background Color Measurement, Min, Max","Comma separated list of the measurement name to plot as the background for each point scaled by the min and max value provided","RN,0,10");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1a);
		parameterArray.addParameter(p1b);
		parameterArray.addParameter(p2b);
		parameterArray.addParameter(p3b);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6a);
		// parameterArray.addParameter(p6);
		// parameterArray.addParameter(p7);
		// parameterArray.addParameter(p8);
		// parameterArray.addParameter(p9);
		// parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		parameterArray.addParameter(p13);
		parameterArray.addParameter(p14);
		parameterArray.addParameter(p15);
		parameterArray.addParameter(p16);
		parameterArray.addParameter(p17);
		parameterArray.addParameter(p18);
		// parameterArray.addParameter(p19);
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
	
	public Double xmin, xmax, xThresh, xCross, xTransition, xLinLogRatio;
	public Double ymin, ymax, yThresh, yCross, yTransition, yLinLogRatio;
	public String xLabel, xTicks, yLabel, yTicks;
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData data = inputs.get("Data File(s)");
		if(data == null || !data.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		
		String timeDimName = parameters.getValueOfParameter("Time Dim Name");
		Table<String> timeFiles = new Table<String>(data.getDimTable(), FileReader.readObjectToFilePathTable(data));
		if(data.dimTable.getDimWithName(timeDimName) == null)
		{
			Logs.log("Have to reorganize tables by time... could take a minute.", this);
			JEXStatics.statusBar.setStatusText("Have to reorganize tables by time... could take a minute.");
			timeFiles =Table.joinAndSplitTables(timeFiles, timeDimName, this);
		}
		
		// Get the X Axis parameters
		String measurement = parameters.getValueOfParameter("Measurement Name");
		// xLabel = parameters.getValueOfParameter("X Label");
		Integer bins = Integer.parseInt(parameters.getValueOfParameter("Number of Bins"));
		String ticks = parameters.getValueOfParameter("X Ticks");
		xmin = Double.parseDouble(parameters.getValueOfParameter("Min Bin"));
		xmax = Double.parseDouble(parameters.getValueOfParameter("Max Bin"));
		// Double thresh =
		// Double.parseDouble(parameters.getValueOfParameter("Threshold"));
		Double offset = Double.parseDouble(parameters.getValueOfParameter("Offset"));
		// xTransition = Double.parseDouble(xLinTransRatio.get(0));
		// xLinLogRatio = Double.parseDouble(xLinTransRatio.get(1));
		// CSVList xThreshAndCross = new
		// CSVList(parameters.getValueOfParameter("Vert. line threshold and crossover"));
		// xThresh = null;
		// xCross = null;
		// if(xThreshAndCross != null && xThreshAndCross.size() != 0 &&
		// !xThreshAndCross.get(0).equals(""))
		// {
		// xThresh = Double.parseDouble(xThreshAndCross.get(0));
		// xCross = Double.parseDouble(xThreshAndCross.get(1));
		// }
		//
		// Get the Y Axis parameters
		ymax = Double.parseDouble(parameters.getValueOfParameter("Max Normalized Y"));
		
		// Gather the plot output parameters
		Double width = Double.parseDouble(parameters.getValueOfParameter("Width [in]"));
		Double height = Double.parseDouble(parameters.getValueOfParameter("Height [in]"));
		Double res = Double.parseDouble(parameters.getValueOfParameter("Resolution [ppi]"));
		Double tStart = Double.parseDouble(parameters.getValueOfParameter("Time Stamp Start"));
		Double tInt = Double.parseDouble(parameters.getValueOfParameter("Time Stamp Interval"));
		String tUnits = parameters.getValueOfParameter("Time Stamp Units");
		Integer window = Integer.parseInt(parameters.getValueOfParameter("Number of Frames Per Plot"));
		if(window < 1)
		{
			window = 1;
		}
		Integer skipSize = Integer.parseInt(parameters.getValueOfParameter("Skip Size"));
		if(skipSize == null || skipSize < 1)
		{
			skipSize = 1;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> linearMap = timeFiles.data;
		DimTable fileTable = timeFiles.dimTable;
		Dim timeDim = fileTable.get(0);
		TreeMap<DimensionMap,String> outputImageData = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		
		List<DimensionMap> maps = fileTable.getDimensionMaps();
		double total = 0;
		for (int i = 0; i < maps.size() - (window - 1); i = i + skipSize)
		{
			total = total + 1;
		}
		for (int i = 0; i < maps.size() - (window - 1); i = i + skipSize)
		{
			if(this.isCanceled())
			{
				return false;
			}
			// Create the timeStamp
			String timeStamp = getTimeStamp(timeDim, i, window, tStart, tInt, tUnits);
			
			// Start output to file
			String outPath = R.startPlot("tif", width, height, res, 10, "Arial", "lzw");
			
			// Perform the plotting of the sublist of files
			plotTimePoint(measurement, linearMap, maps, i, window, timeStamp, bins, offset, ticks);// ,
			// xTransition,
			// xLinLogRatio,
			// yTransition,
			// yLinLogRatio);
			
			// End the plotting
			R.endPlot();
			
			// Put the file that was created into the outputImageData map
			int endIndex = i + (window - 1);
			outputImageData.put(maps.get(endIndex), outPath);
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			// return false;
		}
		if(outputImageData.size() == 0)
		{
			return false;
		}
		
		// Save the output images in the database
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputImageData);
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private String getTimeStamp(Dim timeDim, int i, int window, double tStart, double tInt, String tUnits)
	{
		DecimalFormat formatD = new DecimalFormat("##0.0");
		// Create the timestamp string
		int startIndex = i;
		int endIndex = i + (window - 1);
		double startFrame = Double.parseDouble(timeDim.dimValues.get(startIndex));
		double endFrame = Double.parseDouble(timeDim.dimValues.get(endIndex));
		double startTime = tStart + ((startFrame) * tInt);
		double endTime = tStart + ((endFrame) * tInt);
		String timeStamp = "";
		if(startIndex != endIndex)
		{
			timeStamp = formatD.format(startTime) + "-" + formatD.format(endTime) + " " + tUnits + "  ";
		}
		else
		{
			timeStamp = formatD.format(startTime) + " " + tUnits + "  ";
		}
		return timeStamp;
	}
	
	// private String getRCommand_makeFileSublist(TreeMap<DimensionMap,String>
	// fileMap, List<DimensionMap> maps, int start, int window)
	// {
	// String command = "fileList <- c(";
	// CSVList files = new CSVList();
	// for(int i = start; i < start+window; i++)
	// {
	// files.add(R.quotedPath(fileMap.get(maps.get(i))));
	// }
	// command = command + files.toString() + ")";
	// return command;
	// }
	
	private void plotTimePoint(String Measurement, TreeMap<DimensionMap,String> linearMap, List<DimensionMap> maps, int start, int window, String timeStamp, int nBins, double offset, String ticks)
	{
		double[] grays = getGrayValues(window);
		int end = start + window;
		SingleCellUtility.initializeLogHistogram("Intensity", "Density", nBins, xmin + offset, xmax + offset, ymax);
		int count = 0;
		for (int i = start; i < end; i++)
		{
			if(i == end - 1)
			{
				SingleCellUtility.plotLogHistogram(linearMap.get(maps.get(i)), Measurement, 0, 1, offset);
			}
			else
			{
				SingleCellUtility.plotLogHistogram(linearMap.get(maps.get(i)), Measurement, grays[count], 1, offset);
			}
			count = count + 1;
		}
		
		// calculate and draw the threshold percentages and gating lines
		
		// if(xThresh != null && yThresh != null)
		// {
		// TreeMap<String,Double> percentages =
		// SingleCellUtility.calculateDoubleThresholdPercentages(xThresh,
		// xCross, yThresh, yCross);
		// SingleCellUtility.plotDoubleThresholdText(percentages, xmin, xmax,
		// ymin, ymax, xTransition, xLinLogRatio, yTransition, yLinLogRatio);
		// SingleCellUtility.plotSingleThresholdLine(true, xThresh, xCross,
		// "blue");
		// SingleCellUtility.plotSingleThresholdLine(false, yThresh, yCross,
		// "blue");
		// }
		// else if(xThresh != null)
		// {
		// TreeMap<String,Double> percentages =
		// SingleCellUtility.calculateSingleThresholdPercentages(true, xThresh,
		// xCross);
		// SingleCellUtility.plotSingleThresholdText(percentages, true, xmin,
		// xmax, ymin, ymax, xTransition, xLinLogRatio, yTransition,
		// yLinLogRatio);
		// SingleCellUtility.plotSingleThresholdLine(true, xThresh, xCross,
		// "blue");
		// }
		// else if(yThresh != null)
		// {
		// TreeMap<String,Double> percentages =
		// SingleCellUtility.calculateSingleThresholdPercentages(false, yThresh,
		// yCross);
		// SingleCellUtility.plotSingleThresholdText(percentages, true, xmin,
		// xmax, ymin, ymax, xTransition, xLinLogRatio, yTransition,
		// yLinLogRatio);
		// SingleCellUtility.plotSingleThresholdLine(false, yThresh, yCross,
		// "blue");
		// }
		//
		// // calculate and draw the mean information
		// SingleCellUtility.calculateAndAddMeanInfo(xTransition, xLinLogRatio,
		// yTransition, yLinLogRatio);
		// SingleCellUtility.drawLogicleAxis(true, xTransition, xLinLogRatio,
		// xTicks);
		// SingleCellUtility.drawLogicleAxis(false, yTransition, yLinLogRatio,
		// yTicks);
		// R.eval("box()"); // Finish of axis with box command
		SingleCellUtility.drawLogScaleAxis(true, ticks);
		SingleCellUtility.plotTimeStamp(timeStamp);
	}
	
	public double[] getGrayValues(int n)
	{
		double interval = 1.0 / (n + 1); // purposely not 1/(n-1) here because
		// we don't want to include black. then
		// we add one to make the lightest
		// color not 0
		double[] grays = new double[n];
		for (int i = 0; i < n; i++)
		{
			grays[i] = 1 - (i + 1) * interval;
		}
		return grays;
	}
	
	// private String getRCommand_makePlot(String timeStamp)
	// {
	// //
	// makeJEXFACSPlot(inPaths,colorName,xColI,yColI,xTicks,yTicks,xLabel,yLabel,timeStamp)
	// String xTicks = parameters.getValueOfParameter("X Ticks");
	// String xLabel = parameters.getValueOfParameter("X Label");
	// String yTicks = parameters.getValueOfParameter("Y Ticks");
	// String yLabel = parameters.getValueOfParameter("Y Label");
	// // String measurementName =
	// parameters.getValueOfParameter("Measurement Name");
	// // double measurementIndex =
	// Double.parseDouble(parameters.getValueOfParameter("Measurement Value"));
	//
	// CSVList args = new CSVList();
	// args.add("fileList");
	// args.add(R.sQuote(xTicks));
	// args.add(R.sQuote(xLabel));
	// args.add(R.sQuote(yTicks));
	// args.add(R.sQuote(yLabel));
	// args.add(R.sQuote(timeStamp));
	// // args.add("\"" + measurementName + "\"");
	// // args.add("" + (int)measurementIndex);
	//
	// String ret = "makeJEXFACSPlot3(" + args.toString() + ")";
	//
	// return ret;
	// }
	
}
