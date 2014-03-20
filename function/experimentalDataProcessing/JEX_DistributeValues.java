package function.experimentalDataProcessing;

import java.util.HashMap;
import java.util.TreeMap;

import miscellaneous.CSVList;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import cruncher.Ticket;
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
public class JEX_DistributeValues extends ExperimentalDataCrunch {
	
	public static TreeMap<DimensionMap,String> valueMap = null;
	
	public JEX_DistributeValues()
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
		String result = "Distribute Values To Entries";
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
		String result = "Function that takers a comma separate list of values and places them in each entry given a specified order.";
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
		String toolbox = "Data Importing";
		return toolbox;
	}
	
	/**
	 * This method defines if the function appears in the list in JEX It should be set to true except if you have good reason for it
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
		TypeName[] inputNames = new TypeName[0];
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
		defaultOutputNames[0] = new TypeName(VALUE, "Value Name");
		
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
		Parameter p0 = new Parameter("Values", "Separate values with a comma", "");
		Parameter p1 = new Parameter("Rows", "Number of rows.", "2");
		Parameter p2 = new Parameter("Cols", "Number of columns.", "1");
		Parameter p3 = new Parameter("Order", "Fill row then go to next column (Row) or fill column and then go to next row (Column)", Parameter.DROPDOWN, new String[] { "Row", "Column" });
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
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		// Run the function
		// collect the parameters, define as "inputs"
		CSVList valueList = new CSVList(parameters.getValueOfParameter("Values"));
		if(valueList == null || valueList.size() == 0)
			return false;
		boolean rowOrder = parameters.getValueOfParameter("Order").equals("Row");
		int rows = Integer.parseInt(parameters.getValueOfParameter("Rows"));
		int cols = Integer.parseInt(parameters.getValueOfParameter("Cols"));
		
		if(valueMap == null)
			valueMap = getValueMap(valueList, rows, cols, rowOrder);
		
		DimensionMap valueToGet = new DimensionMap();
		valueToGet.put("R", "" + entry.getTrayY());
		valueToGet.put("C", "" + entry.getTrayX());
		
		JEXData output1 = ValueWriter.makeValueObject(outputNames[0].getName(), valueMap.get(valueToGet));
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	public static TreeMap<DimensionMap,String> getValueMap(CSVList valueList, int rows, int cols, boolean rowOrder)
	{
		TreeMap<DimensionMap,String> valueMap = new TreeMap<DimensionMap,String>();
		DimensionMap map;
		int count = 0;
		if(rowOrder)
		{
			for (int i = 0; i < cols; i++)
			{
				for (int j = 0; j < rows; j++)
				{
					map = new DimensionMap();
					map.put("R", "" + j);
					map.put("C", "" + i);
					valueMap.put(map, valueList.get(count));
					count = count + 1;
				}
			}
		}
		else
		{
			for (int j = 0; j < rows; j++)
			{
				for (int i = 0; i < cols; i++)
				{
					map = new DimensionMap();
					map.put("R", "" + j);
					map.put("C", "" + i);
					valueMap.put(map, valueList.get(count));
					count = count + 1;
				}
			}
		}
		return valueMap;
	}
	
	@Override
	public void finalizeTicket(Ticket t)
	{
		valueMap = null;
	}
}
