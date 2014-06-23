package recycling;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXStatics;
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
public class JEX_SingleCell_SubtractBackgroundIntensitiesFromTable extends JEXCrunchable {
	
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
		String result = "Subtract Background Intensities from Time Files";
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
		String result = "Zeros the background signal in the data.";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(FILE, "Time Files");
		inputNames[1] = new TypeName(FILE, "Background Intensities");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(FILE, "Zeroed Time Files");
		// defaultOutputNames[1] = new
		// TypeName(FILE,"Zeroed Moving Average (logicle)");
		
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
		Parameter p1 = new Parameter("Color Dim Name", "Name of the color dimension in the background intensities table.", "Color");
		// Parameter p2 = new
		// Parameter("xColor Value","Value of the color dimension being plotted on this axis",
		// "1");
		// Parameter p3 = new
		// Parameter("xTransition","Transition point between the linear and log scale",
		// "5");
		// Parameter p4 = new
		// Parameter("xLinLogRatio","Number of linear units to display/calculate per order of magnitude on the log scale.",
		// "10");
		// Parameter p5 = new
		// Parameter("yColor Value","Value of the color dimension being plotted on this axis",
		// "2");
		// Parameter p6 = new
		// Parameter("yTransition","Transition point between the linear and log scale",
		// "5");
		// Parameter p7 = new
		// Parameter("yLinLogRatio","Number of linear units to display/calculate per order of magnitude on the log scale.",
		// "10");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
		// parameterArray.addParameter(p7);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
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
		JEXData data = inputs.get("Time Files");
		if(data != null && !data.getTypeName().getType().equals(JEXData.FILE))
			return false;
		TreeMap<DimensionMap,String> dataFilePaths = FileReader.readObjectToFilePathTable(data);
		
		JEXData bgData = inputs.get("Background Intensities");
		if(bgData != null && !bgData.getTypeName().getType().equals(JEXData.FILE))
			return false;
		String bgFilePath = FileReader.readFileObject(bgData);
		Table<Double> bgTable = JEXTableReader.getNumericTable(bgFilePath);
		
		String colorDimName = parameters.getValueOfParameter("Color Dim Name");
		
		int count = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		DimensionMap temp;
		TreeMap<DimensionMap,String> linearFiles = new TreeMap<DimensionMap,String>();
		for (DimensionMap map : data.getDimTable().getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			String time = map.get("Time");
			Table<Double> dataTable = JEXTableReader.getNumericTable(dataFilePaths.get(map));
			for (Entry<DimensionMap,Double> e : dataTable.data.entrySet())
			{
				temp = e.getKey().copy();
				temp.put("Time", time);
				if(temp.get("Measurement").equals(SingleCellUtility.r) || temp.get("Measurement").equals(SingleCellUtility.g) || temp.get("Measurement").equals(SingleCellUtility.b))
				{
					temp = e.getKey().copy();
					temp.put(colorDimName, temp.get("Measurement"));
					e.setValue(e.getValue() - bgTable.getData(temp));
				}
			}
			String linearPath = JEXTableWriter.writeTable("Zeroed Data", dataTable.data);
			linearFiles.put(map, linearPath);
			
			// Status bar
			count++;
			int percentage = (int) (100 * ((double) count / (double) dataFilePaths.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData linearOutput = FileWriter.makeFileTable(outputNames[0].getName(), linearFiles);
		
		realOutputs.add(linearOutput);
		
		return true;
	}
}
