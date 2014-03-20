package function.experimentalDataProcessing;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import jex.utilities.NDataWindow;
import jex.utilities.NDataWindows;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.ExperimentalDataCrunch;
import function.singleCellAnalysis.ContextCalculator_v1;

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
public class JEX_SingleCell_TransformRawData_v1 extends ExperimentalDataCrunch {
	
	public JEX_SingleCell_TransformRawData_v1()
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
		String result = "Transform Raw Data v1";
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
		String result = "Transform raw R,G,B,x,y,t data into R,G,B,RN,GN,BN,x,y,t data.";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(FILE, "Time Files");
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
		defaultOutputNames[0] = new TypeName(FILE, "Transformed Time Files");
		
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
		// Parameter p2 = new
		// Parameter("Name of Color Dim","Name of the 'Color' dim.","Color");
		Parameter p7 = new Parameter("Radius of Neighborhood", "Size of the neighborhood for calculating local context information [pixels]", "350");
		// Parameter p8 = new
		// Parameter("Number of Frames, Rates","Size of the rolling window in number of frames for calculating rates from running averages","3");
		// Parameter p9 = new
		// Parameter("Minutes Between Frames","Number of minutes between each frame","5");
		// Parameter p9 = new
		// Parameter("Plot Height","Height of plot in pixels","1200");
		// Parameter p4 = new
		// Parameter("X Ticks","Values at which to place tick marks","1,10,100,1000");
		// Parameter p5 = new
		// Parameter("X Label","Label of the X axis","Antiviral Defense Activity [au]");
		// Parameter p6 = new
		// Parameter("Y Color","Color to be plotted on Y axis","2");
		// Parameter p7 = new
		// Parameter("Y Ticks","Values at which to place tick marks","1,10,100,1000,10000");
		// Parameter p8 = new
		// Parameter("Y Label","Label of the Y axis","Viral Activity [au]");
		// Parameter p9 = new Parameter("Width","Pixel width of plot","1500");
		// Parameter p10 = new
		// Parameter("Height","Pixel height of plot","1200");
		// Parameter p13 = new Parameter("Measurement Name",
		// "Name of the measurement attribute","Measurement");
		// Parameter p14 = new Parameter("Measurement Value",
		// "The measurement to plot (Measure Points Roi - 1:Mean, 2:Area, 3:Min, 4:Max, 5:StdDev, 6:Median)",
		// "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		// parameterArray.addParameter(p8);
		// parameterArray.addParameter(p9);
		// parameterArray.addParameter(p10);
		// parameterArray.addParameter(p11);
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
		JEXData fileData = inputs.get("Time Files");
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		DimTable fileTable = fileData.getDimTable();
		TreeMap<DimensionMap,String> fileMap = FileReader.readObjectToFilePathTable(fileData);
		
		// Gather parameters
		// String colorDimName =
		// parameters.getValueOfParameter("Name of Color Dim");
		Double radius = Double.parseDouble(parameters.getValueOfParameter("Radius of Neighborhood"));
		
		// Run the function
		
		int count = 0, percentage = 0, total = fileTable.mapCount();
		JEXStatics.statusBar.setProgressPercentage(0);
		TreeMap<DimensionMap,Double> transformedData;
		TreeMap<DimensionMap,Double> temp = null;
		Dim measurementDim = null;
		TreeMap<DimensionMap,String> newFileMap = new TreeMap<DimensionMap,String>();
		for (DimensionMap timeMap : fileTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			transformedData = new TreeMap<DimensionMap,Double>();
			Table<Double> timePt = JEXTableReader.getNumericTable(fileMap.get(timeMap));
			DimTable timePtTable = timePt.dimTable;
			Dim trackDim = timePtTable.getDimWithName("Track");
			ContextCalculator_v1 calc = new ContextCalculator_v1(timePt.data, "Track", null, radius);
			for (String cell : trackDim.dimValues)
			{
				temp = calc.getContextForCell(cell);
				if(temp != null && temp.size() != 0)
				{
					if(measurementDim == null)
					{
						Vector<String> measurements = new Vector<String>();
						for (DimensionMap map : temp.keySet())
						{
							measurements.add(map.get("Measurement"));
						}
						measurementDim = new Dim("Measurement", measurements);
					}
					transformedData.putAll(temp);
				}
			}
			// DimTable toSave = new DimTable();
			// toSave.add(trackDim.copy());
			// toSave.add(measurementDim.copy());
			String path = JEXTableWriter.writeTable("TransformedData", transformedData);
			newFileMap.put(timeMap, path);
			
			count = count + 1;
			percentage = (int) (100 * ((double) count) / ((double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Save the output images in the database
		JEXData output1 = FileWriter.makeFileTable(outputNames[0].getName(), newFileMap);
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	public Pair<DimensionMap,String> writeAvgTimePoint(NDataWindows cells, Dim trackDim, Dim timeDim, String timeFormat)
	{
		// "##0.00" time format
		TreeMap<DimensionMap,Double> avgData = new TreeMap<DimensionMap,Double>();
		DimensionMap dataPointMap = new DimensionMap();
		DecimalFormat formatD = new DecimalFormat(timeFormat);
		int count = 0, percentage = 0, total = trackDim.size();
		JEXStatics.statusBar.setStatusText("Calculating: 0%");
		TreeMap<String,Double> avg = null;
		for (String cellString : trackDim.dimValues)
		{
			Integer cell = Integer.parseInt(cellString);
			NDataWindow measurements = cells.getWindow(cell);
			if(measurements != null && measurements.isFilled(null))
			{
				dataPointMap.put(trackDim.name(), cellString);
				avg = measurements.avg();
				for (Entry<String,Double> e : avg.entrySet())
				{
					dataPointMap.put("Measurement", e.getKey());
					avgData.put(dataPointMap.copy(), e.getValue());
				}
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
		Dim measurementDim = new Dim("Measurement", avg.keySet()); // "Measurement",new
		// String[]{"x","y"});
		newDimTable.add(measurementDim);
		Table<Double> table = new Table<Double>(newDimTable, avgData);
		
		// write the data and store the path and time stamp
		String tablePath = JEXTableWriter.writeTable("AvgData", table);
		DimensionMap retDim = new DimensionMap();
		Double startFrame = Double.parseDouble(timeDim.dimValues.get(0));
		retDim.put(timeDim.name(), formatD.format(startFrame + cells.getAvgIndex()));
		return new Pair<DimensionMap,String>(retDim, tablePath);
	}
}