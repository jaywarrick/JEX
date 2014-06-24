package function.plugin.old;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import rtools.R;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.singleCellAnalysis.SingleCellUtility;

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
public class JEX_SingleCell_GatedFACSPlots extends JEXCrunchable {
	
	public JEX_SingleCell_GatedFACSPlots()
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
		String result = "Make Gated FACS Plots";
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
		String result = "Make a FACS density plot showing multiple times in grayscale from a list of ARFF files using the R function \"makeJEXFACSPlot3.R\".";
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
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(IMAGE, "Gated FACS Plots");
		defaultOutputNames[1] = new TypeName(FILE, "Gated Population Stats");
		
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
		Parameter p0 = new Parameter("Time Dim Name", "Name of the Time Dimension.", "Time");
		
		Parameter p1a = new Parameter("X Measurement Name", "Name of the measurement to plot on the x axis.", "G");
		Parameter p1 = new Parameter("X Label", "Label of the X axis", "Antiviral Defense Activity [au] (green)");
		Parameter p2 = new Parameter("Xmin,Xmax", "The minimum and maximum of the x axis.", "-5,3000");
		Parameter p3 = new Parameter("X Ticks", "Values at which to place tick marks", "-5,0,5,10,100,1000,10000");
		Parameter p4 = new Parameter("X linear transition pt and ratio", "For plotting on logicle scale, we transition to linear scale at the specified value greater than 0.\nThe ratio is the number of linear units that will be displayed per order of magnitude in the log area of the plot.", "5,10");
		Parameter p5 = new Parameter("Vert. line threshold and crossover", "The position of the x threshold and the ratiometric amount of crossover signal from the y channel. A blank value leaves out threshold data and lines for this axis.", "");
		
		Parameter p6a = new Parameter("Y Measurement Name", "Name of the measurement to plot on the y axis.", "R");
		Parameter p6 = new Parameter("Y Label", "Label of the Y axis", "Viral Activity [au] (red)");
		Parameter p7 = new Parameter("Ymin,Ymax", "The minimum and maximum of the y axis.", "-5,10000");
		Parameter p8 = new Parameter("Y Ticks", "Values at which to place tick marks", "-5,0,5,10,100,1000,10000");
		Parameter p9 = new Parameter("Y linear transition pt and ratio", "For plotting on logicle scale, we transition to linear scale at the specified value greater than 0.\nThe ratio is the number of linear units that will be displayed per order of magnitude in the log area of the plot.", "5,10");
		Parameter p10 = new Parameter("Hor. line threshold and crossover", "The position of the y threshold and the ratiometric amount of crossover signal from the x channel. A blank value leaves out threshold data and lines for this axis.", "");
		
		Parameter p11 = new Parameter("Width [in]", "Width of plot in INCHES", "4");
		Parameter p12 = new Parameter("Height [in]", "Height of plot in INCHES", "3");
		Parameter p13 = new Parameter("Resolution [ppi]", "Resolution of the plot in PPI", "300");
		Parameter p14 = new Parameter("Time Stamp Start", "Initial time to start time stamp", "0");
		Parameter p15 = new Parameter("Time Stamp Interval", "Time interval between frames", "0.3333");
		Parameter p16 = new Parameter("Time Stamp Units", "Labe for the units of time", "[hpi]");
		Parameter p17 = new Parameter("Number of Frames Per Plot", "Number of frames per plot", "4");
		Parameter p18 = new Parameter("Skip Size", "Number of timepoints to skip between plots.", "0");
		Parameter p19 = new Parameter("Pt Background Color Measurement, Min, Max", "Comma separated list of the measurement name to plot as the background for each point scaled by the min and max value provided", "RN,0.01,10");
		Parameter p20 = new Parameter("Plot Parity Line?","True or false, whether to plot the parity line (1:1 ratio)",Parameter.DROPDOWN, new String[]{"true","false"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1a);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6a);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		parameterArray.addParameter(p13);
		parameterArray.addParameter(p14);
		parameterArray.addParameter(p15);
		parameterArray.addParameter(p16);
		parameterArray.addParameter(p17);
		parameterArray.addParameter(p18);
		parameterArray.addParameter(p19);
		parameterArray.addParameter(p20);
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
		
		
		// Get Parameters
		String timeDimName = parameters.getValueOfParameter("Time Dim Name");
		Table<String> timeFiles = new Table<String>(data.getDimTable(), FileReader.readObjectToFilePathTable(data));
		if(data.dimTable.getDimWithName(timeDimName) == null)
		{
			Logs.log("Have to reorganize tables by time... could take a minute.", this);
			JEXStatics.statusBar.setStatusText("Have to reorganize tables by time... could take a minute.");
			timeFiles = Table.joinAndSplitTables(timeFiles, timeDimName, this);
		}
		
		// Get the X Axis parameters
		String xMeasurement = parameters.getValueOfParameter("X Measurement Name");
		xLabel = parameters.getValueOfParameter("X Label");
		CSVList xminmax = new CSVList(parameters.getValueOfParameter("Xmin,Xmax"));
		xmin = Double.parseDouble(xminmax.get(0));
		xmax = Double.parseDouble(xminmax.get(1));
		xTicks = parameters.getValueOfParameter("X Ticks");
		CSVList xLinTransRatio = new CSVList(parameters.getValueOfParameter("X linear transition pt and ratio"));
		xTransition = Double.parseDouble(xLinTransRatio.get(0));
		xLinLogRatio = Double.parseDouble(xLinTransRatio.get(1));
		CSVList xThreshAndCross = new CSVList(parameters.getValueOfParameter("Vert. line threshold and crossover"));
		xThresh = null;
		xCross = null;
		if(xThreshAndCross != null && xThreshAndCross.size() != 0 && !xThreshAndCross.get(0).equals(""))
		{
			xThresh = Double.parseDouble(xThreshAndCross.get(0));
			xCross = Double.parseDouble(xThreshAndCross.get(1));
		}
		
		// Get the Y Axis parameters
		String yMeasurement = parameters.getValueOfParameter("Y Measurement Name");
		yLabel = parameters.getValueOfParameter("Y Label");
		CSVList yminmax = new CSVList(parameters.getValueOfParameter("Ymin,Ymax"));
		ymin = Double.parseDouble(yminmax.get(0));
		ymax = Double.parseDouble(yminmax.get(1));
		yTicks = parameters.getValueOfParameter("Y Ticks");
		CSVList yLinTransRatio = new CSVList(parameters.getValueOfParameter("Y linear transition pt and ratio"));
		yTransition = Double.parseDouble(yLinTransRatio.get(0));
		yLinLogRatio = Double.parseDouble(yLinTransRatio.get(1));
		CSVList yThreshAndCross = new CSVList(parameters.getValueOfParameter("Hor. line threshold and crossover"));
		yThresh = null;
		yCross = null;
		if(yThreshAndCross != null && yThreshAndCross.size() != 0 && !yThreshAndCross.get(0).equals(""))
		{
			yThresh = Double.parseDouble(yThreshAndCross.get(0));
			yCross = Double.parseDouble(yThreshAndCross.get(1));
		}
		
		// Gather the plot output parameters
		Double width = Double.parseDouble(parameters.getValueOfParameter("Width [in]"));
		Double height = Double.parseDouble(parameters.getValueOfParameter("Height [in]"));
		Double res = Double.parseDouble(parameters.getValueOfParameter("Resolution [ppi]"));
		Double tStart = Double.parseDouble(parameters.getValueOfParameter("Time Stamp Start"));
		Double tInt = Double.parseDouble(parameters.getValueOfParameter("Time Stamp Interval"));
		String tUnits = parameters.getValueOfParameter("Time Stamp Units");
		Integer window = Integer.parseInt(parameters.getValueOfParameter("Number of Frames Per Plot"));
		if(window == null || window == 0)
		{
			window = 1;
		}
		Integer skipSize = Integer.parseInt(parameters.getValueOfParameter("Skip Size"));
		String bgColorString = parameters.getValueOfParameter("Pt Background Color Measurement, Min, Max");
		if(skipSize == null || skipSize == 0)
		{
			skipSize = 1;
		}
		boolean plotParity = Boolean.parseBoolean(parameters.getValueOfParameter("Plot Parity Line?"));
		
		// Run the function
		TreeMap<DimensionMap,String> linearMap = timeFiles.data;
		DimTable fileTable = timeFiles.dimTable;
		Dim timeDim = fileTable.getDimWithName(timeDimName);
		TreeMap<DimensionMap,String> outputImageData = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> outputMeans = new TreeMap<DimensionMap,Double>();
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
			TreeMap<String,Double> stats = plotTimePoint(xMeasurement, yMeasurement, linearMap, maps, i, window, timeStamp, xTransition, xLinLogRatio, yTransition, yLinLogRatio, bgColorString, plotParity);
			
			// End the plotting
			R.endPlot();
			
			// Put the file that was created into the outputImageData map
			int endIndex = i + (window - 1);
			DimensionMap map2 = maps.get(endIndex).copy();
			outputImageData.put(map2.copy(), outPath);
			for (String stat : stats.keySet())
			{
				map2.put("Measurement", stat);
				outputMeans.put(map2.copy(), stats.get(stat));
			}
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			// return false;
		}
		if(outputImageData.size() == 0)
		{
			return false;
		}
		
		String meansPath = JEXTableWriter.writeTable("FACS Means", outputMeans);
		
		// Save the output images in the database
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputImageData);
		JEXData output2 = FileWriter.makeFileObject(outputNames[1].getName(), null, meansPath);
		realOutputs.add(output1);
		realOutputs.add(output2);
		
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
	
	private TreeMap<String,Double> plotTimePoint(String xMeasurement, String yMeasurement, TreeMap<DimensionMap,String> linearMap, List<DimensionMap> maps, int start, int window, String timeStamp, double xTransition, double xLinLogRatio, double yTransition, double yLinLogRatio, String bgColorString, boolean plotParity)
	{
		double cexPointSize = 0.6;
		double[] grays = getGrayValues(window);
		String bgColorMeasurementName = null;
		double bgMin = 0;
		double bgMax = 0;
		if(bgColorString != null && !bgColorString.equals(""))
		{
			CSVList bgStrings = new CSVList(bgColorString);
			bgColorMeasurementName = bgStrings.get(0);
			bgMin = Double.parseDouble(bgStrings.get(1));
			bgMax = Double.parseDouble(bgStrings.get(2));
		}
		int end = start + window;
		SingleCellUtility.initializeFACSPlot(xLabel, yLabel, SingleCellUtility.calculateLogicleScaleValue(xmin, xTransition, xLinLogRatio), SingleCellUtility.calculateLogicleScaleValue(xmax, xTransition, xLinLogRatio), SingleCellUtility.calculateLogicleScaleValue(ymin, xTransition, xLinLogRatio), SingleCellUtility.calculateLogicleScaleValue(ymax, xTransition, xLinLogRatio));
		
		int count = 0;
		for (int i = start; i < end; i++)
		{
			if(i == end - 1)
			{
				// plot the data in black (filled or not filled depending on the
				// size of the window
				if(window == 1 && bgColorMeasurementName == null)
				{
					SingleCellUtility.plotXYData(true, linearMap.get(maps.get(i)), xMeasurement, yMeasurement, cexPointSize, xTransition, xLinLogRatio, yTransition, yLinLogRatio, null, 0, 0, null, null); // no
					// background
					// color
					// (transparent)
				}
				else if(window != 1 && bgColorMeasurementName == null)
				{
					SingleCellUtility.plotXYData(true, linearMap.get(maps.get(i)), xMeasurement, yMeasurement, cexPointSize, xTransition, xLinLogRatio, yTransition, yLinLogRatio, null, 0, 0, "black", null); // black
					// background
					// color
				}
				else
				// (bgColorMeasurementName != null)
				{
					SingleCellUtility.plotXYData(true, linearMap.get(maps.get(i)), xMeasurement, yMeasurement, cexPointSize, xTransition, xLinLogRatio, yTransition, yLinLogRatio, bgColorMeasurementName, bgMin, bgMax, "red", null); // black
					// background
					// color
				}
			}
			else
			// always plot previous timpoints in grayscale
			{
				SingleCellUtility.plotXYData(true, linearMap.get(maps.get(i)), xMeasurement, yMeasurement, cexPointSize, xTransition, xLinLogRatio, yTransition, yLinLogRatio, null, 0, 0, null, grays[count]);
			}
			count = count + 1;
		}
		
		// calculate and draw the threshold percentages and gating lines
		
		TreeMap<String,Double> stats = null;
		if(xThresh != null && yThresh != null)
		{
			stats = SingleCellUtility.calculateStats_DoubleThreshold(xThresh, xCross, yThresh, yCross);
			SingleCellUtility.plotDoubleThresholdText(stats, xmin, xmax, ymin, ymax, xTransition, xLinLogRatio, yTransition, yLinLogRatio);
			SingleCellUtility.plotSingleThresholdLine(true, xThresh, xCross, "blue", xTransition, xLinLogRatio, yTransition, yLinLogRatio);
			SingleCellUtility.plotSingleThresholdLine(false, yThresh, yCross, "blue", xTransition, xLinLogRatio, yTransition, yLinLogRatio);
		}
		else if(xThresh != null)
		{
			stats = SingleCellUtility.calculateStats_SingleThreshold(true, xThresh, xCross);
			SingleCellUtility.plotSingleThresholdText(stats, true, xmin, xmax, ymin, ymax, xTransition, xLinLogRatio, yTransition, yLinLogRatio);
			SingleCellUtility.plotSingleThresholdLine(true, xThresh, xCross, "blue", xTransition, xLinLogRatio, yTransition, yLinLogRatio);
		}
		else if(yThresh != null)
		{
			stats = SingleCellUtility.calculateStats_SingleThreshold(false, yThresh, yCross);
			SingleCellUtility.plotSingleThresholdText(stats, false, xmin, xmax, ymin, ymax, xTransition, xLinLogRatio, yTransition, yLinLogRatio);
			SingleCellUtility.plotSingleThresholdLine(false, yThresh, yCross, "blue", xTransition, xLinLogRatio, yTransition, yLinLogRatio);
		}
		else
		{
			stats = SingleCellUtility.calculateStats_NoThreshold();
		}
		
		// calculate and draw the mean information
		SingleCellUtility.addMeanInfo(stats.get("meanX"), stats.get("meanY"), xTransition, xLinLogRatio, yTransition, yLinLogRatio);
		if(plotParity)
		{
			SingleCellUtility.plotLine(false, true, stats.get("meanY")/stats.get("meanX"), 0, -100, 1000000, 100, "red", 0.5, xTransition, xLinLogRatio, yTransition, yLinLogRatio);
		}
		SingleCellUtility.drawLogicleAxis(true, xTransition, xLinLogRatio, xTicks);
		SingleCellUtility.drawLogicleAxis(false, yTransition, yLinLogRatio, yTicks);
		R.eval("box()"); // Finish of axis with box command
		SingleCellUtility.plotTimeStamp(timeStamp);
		return stats;
	}
	
	public double[] getGrayValues(int n)
	{
		double interval = 1.0 / (n); // purposely not 1/(n-1) here because we
		// don't want to include black.
		double[] grays = new double[n];
		for (int i = 0; i < n; i++)
		{
			if(i == 0)
			{
				grays[i] = 1.0;
			}
			else
			{
				grays[i] = 1 - i * interval;
			}
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
