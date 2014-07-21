package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;

import org.rosuda.REngine.REXP;

import rtools.R;
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
public class JEX_MakeFACSRatePlots extends JEXCrunchable {
	
	public JEX_MakeFACSRatePlots()
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
		String result = "Make FACS Rate Plots";
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
		String result = "Make a plot of the rates of changes of XY data using \"makeJEXFACSPlot4.R\".";
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Rate Plots");
		
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
		
		Parameter p0 = new Parameter("Working Directory", "Desired R working directory", "/Volumes/shared/JEX Databases/Adam/CellTracking");
		Parameter p4 = new Parameter("X Ticks", "Values at which to place tick marks", "-500,-250,250,500");
		Parameter p5 = new Parameter("X Label", "Label of the X axis", "G");
		Parameter p7 = new Parameter("Y Ticks", "Values at which to place tick marks", "-500,-250,250,500");
		Parameter p8 = new Parameter("Y Label", "Label of the Y axis", "R");
		Parameter p9 = new Parameter("Width [in]", "Width of plot in INCHES", "4");
		Parameter p10 = new Parameter("Height [in]", "Height of plot in INCHES", "4");
		Parameter p10b = new Parameter("Resolution [ppi]", "Resolution of the plot in PPI", "600");
		Parameter p11 = new Parameter("Time Stamp Start", "Initial time to start time stamp", "0");
		Parameter p12 = new Parameter("Time Stamp Interval", "Time interval between frames", "0.3333");
		Parameter p13 = new Parameter("Time Stamp Units", "Label for the units of time", "[hpi]");
		// Parameter p13 = new Parameter("Measurement Name",
		// "Name of the measurement attribute","Measurement");
		// Parameter p14 = new Parameter("Measurement Value",
		// "The measurement to plot (Measure Points Roi - 1:Mean, 2:Area, 3:Min, 4:Max, 5:StdDev, 6:Median)",
		// "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p10b);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		parameterArray.addParameter(p13);
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
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData fileData = inputs.get("ARFF Files");
		
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))
			return false;
		
		// Gather parameters
		String workingDirectory = parameters.getValueOfParameter("Working Directory");
		Double width = Double.parseDouble(parameters.getValueOfParameter("Width [in]"));
		Double height = Double.parseDouble(parameters.getValueOfParameter("Height [in]"));
		Double res = Double.parseDouble(parameters.getValueOfParameter("Resolution [ppi]"));
		
		Double tStart = Double.parseDouble(parameters.getValueOfParameter("Time Stamp Start"));
		Double tInt = Double.parseDouble(parameters.getValueOfParameter("Time Stamp Interval"));
		String tUnits = parameters.getValueOfParameter("Time Stamp Units");
		
		// Start R Engine
		@SuppressWarnings("unused")
		REXP result;
		result = R.setwd(workingDirectory);
		result = R.source("makeJEXFACSTrajPlot2.R");
		
		// Run the function
		TreeMap<DimensionMap,String> fileMap = FileReader.readObjectToFilePathTable(fileData);
		DimTable fileTable = fileData.getDimTable();
		Dim timeDim = fileTable.get(0);
		TreeMap<DimensionMap,String> outputImageData = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		DecimalFormat formatD = new DecimalFormat("##0.0");
		List<DimensionMap> maps = fileTable.getDimensionMaps();
		for (int i = 0; i < maps.size(); i++)
		{
			String inPath = fileMap.get(maps.get(i));
			File inFile = new File(inPath);
			if(inFile.exists())
			{
				// Create the timestamp string
				double startFrame = Double.parseDouble(timeDim.dimValues.get(i));
				double startTime = tStart + ((startFrame) * tInt);
				String timeStamp = "";
				timeStamp = formatD.format(startTime) + " " + tUnits + "  ";
				
				// Start output to file
				String outPath = R.startPlot("tif", width, height, res, 10, "Arial", "lzw");
				
				// Perform the plotting
				String command = this.getRCommand_makePlot(inPath, outPath, timeStamp);
				result = R.eval(command);
				
				// End the plotting
				result = R.endPlot();
				
				// Put the file that was created into the outputImageData map
				outputImageData.put(maps.get(i), outPath);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) fileTable.mapCount())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
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
	
	private String getRCommand_makePlot(String inPath, String outPath, String timeStamp)
	{
		// makeJEXFACSPlot(inPath,xTicks,xLabel,yTicks,yLabel,timeStamp)
		String xTicks = parameters.getValueOfParameter("X Ticks");
		String xLabel = parameters.getValueOfParameter("X Label");
		String yTicks = parameters.getValueOfParameter("Y Ticks");
		String yLabel = parameters.getValueOfParameter("Y Label");
		// String measurementName =
		// parameters.getValueOfParameter("Measurement Name");
		// double measurementIndex =
		// Double.parseDouble(parameters.getValueOfParameter("Measurement Value"));
		
		CSVList args = new CSVList();
		args.add(R.quotedPath(inPath));
		args.add(R.sQuote(xTicks));
		args.add(R.sQuote(xLabel));
		args.add(R.sQuote(yTicks));
		args.add(R.sQuote(yLabel));
		args.add(R.sQuote(timeStamp));
		// args.add("\"" + measurementName + "\"");
		// args.add("" + (int)measurementIndex);
		
		String ret = "makeJEXFACSTrajPlot2(" + args.toString() + ")";
		
		return ret;
	}
	
}
