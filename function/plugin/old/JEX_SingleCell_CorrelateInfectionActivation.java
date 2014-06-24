package function.plugin.old;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.DoubleWindow;
import jex.utilities.FunctionUtility;
import rtools.R;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

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
public class JEX_SingleCell_CorrelateInfectionActivation extends JEXCrunchable {
	
	public JEX_SingleCell_CorrelateInfectionActivation()
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
		String result = "Correlate Infection vs Activation";
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
		defaultOutputNames[0] = new TypeName(FILE, "Infection vs Activation Times Plot");
		
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
		Parameter p0 = new Parameter("Window", "Number of time steps to look ahead to determine if the cell gets infected", "3");
		// Parameter p1 = new
		// Parameter("Drop Off Window","Threshold over which is considered an infected cell","10");
		// Parameter p2 = new
		// Parameter("Drop Off Fraction","Proportion of Green signal seen in the Red channel","0.85");
		// Parameter p3 = new
		// Parameter("Activation Threshold","Threshold over which is considered an activated cell","10.0");
		// Parameter p4 = new
		// Parameter("Red to Green Crossover Parameter","Proportion of Red signal seen in the Green channel","0.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",Parameter.DROPDOWN,new
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
		JEXData jData = inputs.get("Time Files");
		jData.getDataMap();
		if(jData == null || !jData.getTypeName().getType().equals(JEXData.FILE))
			return false;
		TreeMap<DimensionMap,String> tables = FileReader.readObjectToFilePathTable(jData);
		
		// Gather parameters
		int window = Integer.parseInt(parameters.getValueOfParameter("Window"));
		// int dropOffWindow =
		// Integer.parseInt(parameters.getValueOfParameter("Drop Off Window"));
		// double dropOffFraction =
		// Double.parseDouble(parameters.getValueOfParameter("Drop Off Fraction"));
		
		double count = 0;
		double total = tables.size();
		double percentage = 0;
		TreeMap<DimensionMap,Double> tableDatas = new TreeMap<DimensionMap,Double>();
		DimTable unionTable = jData.getDimTable().copy();
		DimTable temp = null;
		Dim mDim = new Dim("Measurement", new String[] { "R", "G" });
		unionTable.add(mDim);
		for (DimensionMap map : tables.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			Table<Double> tableData = JEXTableReader.getNumericTable(tables.get(map));
			temp = tableData.dimTable.copy();
			temp.removeDimWithName("Measurement");
			unionTable = DimTable.union(unionTable, temp);
			for (Entry<DimensionMap,Double> e : tableData.data.entrySet())
			{
				if(e.getKey().get("Measurement").equals("R") || e.getKey().get("Measurement").equals("G"))
				{
					DimensionMap map2 = e.getKey().copy();
					map2.putAll(map);
					tableDatas.put(map2, e.getValue());
				}
			}
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}
		
		Dim trackDim = unionTable.getDimWithName("ID");
		Table<Double> allData = new Table<Double>(unionTable, tableDatas);
		
		count = 0;
		String path = R.startPlot("pdf", 4, 4, 300, 10, null, null);
		R.eval("plot(c(),c(),xlim=c(0,350), ylim=c(0,350), xlab='Activation Half Max Time', ylab='Infection Half Max Time')");
		for (String track : trackDim.dimValues)
		{
			if(this.isCanceled())
			{
				return false;
			}
			TreeMap<String,Object> rMetrics = DoubleWindow.getWindowedData(allData, new DimensionMap("ID=" + track + ",Measurement=R"), window, "Time");
			if(rMetrics != null)
			{
				TreeMap<String,Object> gMetrics = DoubleWindow.getWindowedData(allData, new DimensionMap("ID=" + track + ",Measurement=G"), window, "Time");
				if(gMetrics != null)
				{
					// @SuppressWarnings("unchecked")
					// TreeMap<DimensionMap,Double> rVals =
					// (TreeMap<DimensionMap,Double>)rMetrics.get("avg");
					// @SuppressWarnings("unchecked")
					// TreeMap<DimensionMap,Double> gVals =
					// (TreeMap<DimensionMap,Double>)gMetrics.get("avg");
					// R.makeVector("rAvgs", rVals.values());
					// R.makeVector("gAvgs", gVals.values());
					// @SuppressWarnings("unchecked")
					// Vector<Double> time =
					// (Vector<Double>)rMetrics.get("time");
					// R.makeVector("time", time);
					
					R.eval("points(" + gMetrics.get("halfAvgRangeTime") + "," + rMetrics.get("halfAvgRangeTime") + ", pch=20, cex=0.3, col=rgb(0,0,0,0.2))");
					R.eval("print(c(" + gMetrics.get("halfAvgRangeTime") + "," + rMetrics.get("halfAvgRangeTime") + "))");
				}
			}
			
			JEXStatics.statusBar.setProgressPercentage((int) (100 * count / trackDim.size()));
			count = count + 1;
		}
		R.endPlot();
		
		// String tempPath =
		// JEXTableWriter2.writeTable(outputNames[0].getName(), infectionTimes);
		
		JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), null, path);
		
		// Set the outputs
		realOutputs.add(output1);
		
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
}
