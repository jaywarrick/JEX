package function.plugin.old;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
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
public class JEX_Calculator_BasicArithmetic extends JEXCrunchable {
	
	public JEX_Calculator_BasicArithmetic()
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
		String result = "Basic Arithmetic";
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
		String result = "Arithmetic operation based on values.";
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
		String toolbox = "Calculator";
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
		inputNames[0] = new TypeName(VALUE, "Value 1");
		inputNames[1] = new TypeName(VALUE, "Value 2");
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
		defaultOutputNames[0] = new TypeName(VALUE, "Result");
		
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
		// (ImageProcessor ip, double tolerance, double threshold, int
		// outputType, boolean excludeOnEdges, boolean isEDM, Roi roiArg,
		// boolean lightBackground)
		Parameter p0 = new Parameter("Operation", "Select the arithmetic operation to apply.", Parameter.DROPDOWN, new String[] { "Add", "Subtract", "Divide", "Multiply" }, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
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
		JEXData value1Data = inputs.get("Value 1");
		JEXData value2Data = inputs.get("Value 2");
		
		// Check the inputs
		if(value1Data == null || !value1Data.getTypeName().getType().equals(JEXData.VALUE))
			return false;
		if(value2Data == null || !value2Data.getTypeName().getType().equals(JEXData.VALUE))
			return false;
		
		// Gather the paramters
		String operation = parameters.getValueOfParameter("Operation");
		
		// Run the function
		TreeMap<DimensionMap,Double> value1Map = ValueReader.readObjectToDoubleTable(value1Data);
		TreeMap<DimensionMap,Double> value2Map = ValueReader.readObjectToDoubleTable(value2Data);
		TreeMap<DimensionMap,Double> resultMap = new TreeMap<DimensionMap,Double>();
		
		// Loop
		int count = 0, percentage = 0;
		for (DimensionMap map : value1Map.keySet())
		{
			// Get the values
			Double value1 = value1Map.get(map);
			Double value2 = value2Map.get(map);
			
			// Test if the values exist
			if(value1 == null || value2 == null)
			{
				continue;
			}
			
			// Calculate the new value
			if(operation.equals("Add"))
			{
				double valueResult = value1 + value2;
				
				// Add the output value
				resultMap.put(map, valueResult);
			}
			else if(operation.equals("Subtract"))
			{
				double valueResult = value1 - value2;
				
				// Add the output value
				resultMap.put(map, valueResult);
			}
			else if(operation.equals("Divide"))
			{
				double valueResult = value1 / value2;
				
				// Add the output value
				resultMap.put(map, valueResult);
			}
			else if(operation.equals("Multiply"))
			{
				double valueResult = value1 * value2;
				
				// Add the output value
				resultMap.put(map, valueResult);
			}
			else
			{   
				
			}
			
			// Increment the counter
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) resultMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		//
		JEXData output1 = ValueWriter.makeValueTableFromDouble(outputNames[0].getName(), resultMap);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
}