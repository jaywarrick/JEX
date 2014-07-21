package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.DoubleWindow;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.Canceler;
import miscellaneous.Pair;
import rtools.R;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;

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
public class JEX_SingleCell_PlotTrajectories extends JEXCrunchable {
	
	public JEX_SingleCell_PlotTrajectories()
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
		String result = "Plot Trajectories";
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
		String result = "Calculate the when infections occur in each cell relative to activation.";
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Trajectories");
		defaultOutputNames[1] = new TypeName(IMAGE, "Correlation Plot");
		
		if(outputNames == null)
		{
			return defaultOutputNames;
		}
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p0a = new Parameter("Measurements to Plot","(i.e. 'R' to plot 'R' and 'R,G' to plot 'R' and 'G')","R,G");
		Parameter p0b = new Parameter("Colors for Each Measurement","Specify (in lower case) the name of the color that you would like to plot each measurement as.", "red,green");
		Parameter p0c = new Parameter("Time Dim Name","Name of the time dimension (typically 'T' or 'Time').","Time");
		Parameter p0 = new Parameter("Window", "Number of time steps to look ahead to determine if the cell gets infected", "3");
		Parameter p1 = new Parameter("Track Length Min", "Minimum number of frames for a track to be considered", "150");
		Parameter p2 = new Parameter("Red Threshold", "The maximum that the first red datapoint can be to be considered", "4");
		// Parameter p3 = new
		// Parameter("Activation Threshold","Threshold over which is considered an activated cell","10.0");
		// Parameter p4 = new
		// Parameter("Red to Green Crossover Parameter","Proportion of Red signal seen in the Green channel","0.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",Parameter.DROPDOWN,new
		// String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0a);
		parameterArray.addParameter(p0b);
		parameterArray.addParameter(p0c);
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
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
		JEXData data = inputs.get("Data File(s)");
		data.getDataMap();
		if(data == null || !data.getTypeName().getType().equals(JEXData.FILE))
			return false;
		String timeDimName = parameters.getValueOfParameter("Time Dim Name");
		Table<String> dataFiles = new Table<String>(data.getDimTable(), FileReader.readObjectToFilePathTable(data));
		if(data.dimTable.getDimWithName(timeDimName) != null)
		{
			dataFiles = this.joinTimeDimAndReturnNewFileTable(dataFiles, timeDimName, this);
		}
		
		// Gather parameters
		CSVList measurements = new CSVList(parameters.getValueOfParameter("Measurements to Plot"));
		CSVList colors = new CSVList(parameters.getValueOfParameter("Colors for Each Measurement"));
		TreeMap<String,String> colorMap = new TreeMap<String,String>();
		int i = 0;
		for(String measurement : measurements)
		{
			colorMap.put(measurement, colors.get(i));
			i = i + 1;
		}
		int window = Integer.parseInt(parameters.getValueOfParameter("Window"));
		int trackLengthMin = Integer.parseInt(parameters.getValueOfParameter("Track Length Min"));
		double redThresh = Double.parseDouble(parameters.getValueOfParameter("Red Threshold"));
		
		Vector<Pair<Double,Double>> halfMaxTimes = new Vector<Pair<Double,Double>>();
		R.eval("x <- 1"); // Start R if it isn't already
		TreeMap<DimensionMap,String> trajPlots = new TreeMap<DimensionMap,String>();
		DecimalFormat formatter = new DecimalFormat("0.00");
		double minDeltaT = Double.MAX_VALUE;
		double maxDeltaT = Double.MIN_VALUE;
		double minMax = Double.MAX_VALUE;
		double maxMax = Double.MIN_VALUE;
		double deltaT = 0;
		double maxInt = 0;
		for (DimensionMap map : dataFiles.dimTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			/// New Stuff
			// read this data file
			Table<Double> dataNums = JEXTableReader.getNumericTable(dataFiles.getData(map));
			DimTable timelessMeasurementLessDimTable = dataNums.dimTable.copy();
			timelessMeasurementLessDimTable.removeDimWithName(timeDimName);
			timelessMeasurementLessDimTable.removeDimWithName("Measurement");
			double count = 0;
			// for each unique set of time data, plot the trajectories for the specified measurements
			for(DimensionMap subMap : timelessMeasurementLessDimTable.getMapIterator())
			{
				JEXStatics.statusBar.setProgressPercentage(0);
				if(this.isCanceled())
				{
					return false;
				}
				// gather the trajectories of the specified measurements
				DimensionMap filter = subMap.copy();
				TreeMap<String,TreeMap<String,Object>> metrics = new TreeMap<String,TreeMap<String,Object>>();
				for(String m : measurements)
				{
					filter.put("Measurement", m);
					TreeMap<String,Object> metric = DoubleWindow.getWindowedData(dataNums, filter, window, timeDimName);
					if(metric != null)
					{
						metrics.put(m, metric);
					}
				}
				
				// plot the trajectories
				int mTextCounter = 1;
				int side = 1;
				int alignment = 0;
				double yMin = Double.MAX_VALUE;
				double yMax = Double.MIN_VALUE;
				Vector<Double> time = (Vector<Double>) metrics.firstEntry().getValue().get("time");
				if(time.size() < trackLengthMin)
				{
					continue;
				}
				for(Entry<String,TreeMap<String,Object>> metric : metrics.entrySet())
				{
					if((Double) metric.getValue().get("maxAvg") > yMax)
					{
						yMax = (Double) metric.getValue().get("maxAvg");
					}
					if((Double) metric.getValue().get("minAvg") < yMin)
					{
						yMin = (Double) metric.getValue().get("minAvg");
					}
				}
				String path = R.startPlot("tif", 5, 4, 300, 10, null, null);
				SingleCellUtility.initializeTrajectoryPlot("Time [frame]", "Value", 0, time.lastElement(), yMin, 1.1*yMax, false, false);
				for(Entry<String,TreeMap<String,Object>> metric : metrics.entrySet())
				{
					if(mTextCounter == 2)
					{
						alignment = 1;
					}
					if(mTextCounter == 3)
					{
						side = 3;
						alignment = 0;
					}
					if(mTextCounter == 4)
					{
						alignment = 1;
					}
					time = (Vector<Double>) metric.getValue().get("time");
					R.makeVector("time", time);
					Collection<Double> y = ((TreeMap<DimensionMap,Double>) metric.getValue().get("avg")).values();
					R.makeVector("y", y);
					R.eval("lines(time, y, col=" + R.sQuote(colorMap.get(metric.getKey())) + ")");
					R.eval("mtext(" + R.sQuote("  MinAvg: " + formatter.format(metric.getValue().get("minAvg")) + ", TimeHalfMax: " + formatter.format(metric.getValue().get("halfAvgRangeTime")) + ", MaxAvg: " + formatter.format(metric.getValue().get("maxAvg"))) + ", side=" + side + ", adj=" + alignment + ", cex=" + SingleCellUtility.annotationsize + ", outer=TRUE, col=" + R.sQuote(colorMap.get(metric.getKey())) + ");");
					
					mTextCounter = mTextCounter + 1;
				}
				
				// Get correlation data
				if(measurements.size() >= 2 && (Double) metrics.get(measurements.get(0)).get("maxAvg") > redThresh)
				{
					deltaT = (Double) metrics.get(measurements.get(1)).get("halfAvgRangeTime") - (Double) metrics.get(measurements.get(0)).get("halfAvgRangeTime");
					if(deltaT > maxDeltaT)
					{
						maxDeltaT = deltaT;
					}
					if(deltaT < minDeltaT)
					{
						minDeltaT = deltaT;
					}
					maxInt = (Double) metrics.get(measurements.get(1)).get("maxAvg");
					if(maxInt > maxMax)
					{
						maxMax = maxInt;
					}
					if(maxInt < minMax)
					{
						minMax = maxInt;
					}
					Pair<Double,Double> correlation = new Pair<Double,Double>(deltaT, maxInt);
					halfMaxTimes.add(correlation);
				}
				
				R.endPlot();
				trajPlots.put(subMap, path);
				
				JEXStatics.statusBar.setProgressPercentage((int) (100 * count / timelessMeasurementLessDimTable.mapCount()));
				count = count + 1;
			}
		}
		
		String path = R.startPlot("tif", 5, 4, 300, 10, null, null);
		SingleCellUtility.initializeTrajectoryPlot("Half Max Time Difference [frames, " + measurements.get(1) + " - " + measurements.get(0) + "]", measurements.get(1) + " Max [au]", minDeltaT, maxDeltaT, minMax, maxMax, false, false);
		for (Pair<Double,Double> pair : halfMaxTimes)
		{
			R.eval("points(" + pair.p1 + ", " + pair.p2 + ", pch=21, col=rgb(0,0,0,0), bg=rgb(0,0,0,0.2), cex=0.5)");
		}
		R.endPlot();
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), trajPlots);
		JEXData output2 = ImageWriter.makeImageObject(outputNames[1].getName(), path);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
			return null;
		ImagePlus im = new ImagePlus(imagePath);
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should
		// be
		// a
		// float
		// processor
		
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
		
		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		im.flush();
		
		// return temp filePath
		return imPath;
	}
	
	private Table<String> joinTimeDimAndReturnNewFileTable(Table<String> fileTable, String timeDimName, Canceler canceler)
	{
		Logs.log("Have to join the time dimension... this could take a minute.", this);
		JEXStatics.statusBar.setStatusText("Have to reorganize tables by time... could take a minute.");
		DimTable temp = fileTable.dimTable.copy();
		TreeMap<DimensionMap,String> pathTable = new TreeMap<DimensionMap,String>();
		if(temp.size() > 1)
		{
			temp.removeDimWithName(timeDimName);
			Dim timeDim = temp.getDimWithName(timeDimName);
			for(DimensionMap map : temp.getMapIterator())
			{
				TreeMap<DimensionMap,String> tempPathTable = new TreeMap<DimensionMap,String>();
				DimensionMap tempMap = map.copy();
				for(String val : timeDim.dimValues)
				{
					tempMap.put(timeDimName, val);
					tempPathTable.put(tempMap, fileTable.getData(tempMap));
				}
				
				String path = Table.joinTables(new Table<String>(new DimTable(tempPathTable), tempPathTable), canceler);
				pathTable.put(map, path);
			}
			Table<String> ret = new Table<String>(temp, pathTable);
			return ret;
		}
		else
		{
			String path = Table.joinTables(fileTable, this);
			pathTable.put(new DimensionMap(), path);
			Table<String> ret = new Table<String>(new DimTable(), pathTable);
			return ret;
		}
	}
}
