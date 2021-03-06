package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.LabelReader;
import Database.DataReader.ValueReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.JEXCrunchable;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import tables.DimTable;
import tables.DimensionMap;
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
public class JEX_OscillatoryAdhesionCompileDualPopResults extends JEXCrunchable {
	
	public JEX_OscillatoryAdhesionCompileDualPopResults()
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
		String result = "Oscillatory Adhesion Compile Dual Population Results";
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
		String result = "Function that allows you to compile the tau50, sigma, and R^2 results for a bunch of channels into a single spreadsheet.";
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
		String toolbox = "Custom Cell Analysis";
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
	
	@Override
	public boolean isInputValidityCheckingEnabled()
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
		inputNames[0] = new TypeName(VALUE, "Dual Fit Results");
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
		defaultOutputNames[0] = new TypeName(FILE, "Dual Fit Compiled Results");
		
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
		Parameter p0 = new Parameter("Sorting Labels", "Names of labels in these entries by which to sort the data in the compiled results table (comma separated, no extra spaces near commas, case sensitive).", "Valid,Substrate,Cell");
		// Parameter p1 = new
		// Parameter("Old Min","Image Intensity Value","0.0");
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
		// parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
		return parameterArray;
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
		JEXData valueData = inputs.get("Dual Fit Results");
		if(valueData == null || !valueData.getTypeName().getType().matches(JEXData.VALUE))
		{
			return false;
		}
		TreeMap<DimensionMap,Double> fitResults = ValueReader.readObjectToDoubleTable(valueData);
		
		// Collect Parameters
		String sortingLabelsString = parameters.getValueOfParameter("Sorting Labels");
		CSVList sortingLabels = new CSVList(sortingLabelsString);
		
		// Save the data.
		DimensionMap compiledMap = new DimensionMap();
		if(compiledData == null)
		{
			compiledData = new TreeMap<DimensionMap,Double>();
		}
		compiledMap = new DimensionMap();
		compiledMap.put("Experiment", entry.getEntryExperiment());
		compiledMap.put("Array X", "" + entry.getTrayX());
		compiledMap.put("Array Y", "" + entry.getTrayY());
		for (String labelName : sortingLabels)
		{
			JEXData label = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, labelName), entry);
			compiledMap.put(labelName, LabelReader.readLabelValue(label));
		}
		compiledMap.put("Measurement", "Alpha");
		compiledData.put(compiledMap.copy(), fitResults.get(compiledMap));
		compiledMap.put("Measurement", "Tau50_1");
		compiledData.put(compiledMap.copy(), fitResults.get(compiledMap));
		compiledMap.put("Measurement", "Sigma_1");
		compiledData.put(compiledMap.copy(), fitResults.get(compiledMap));
		compiledMap.put("Measurement", "Tau50_2");
		compiledData.put(compiledMap.copy(), fitResults.get(compiledMap));
		compiledMap.put("Measurement", "Sigma_2");
		compiledData.put(compiledMap.copy(), fitResults.get(compiledMap));
		compiledMap.put("Measurement", "R^2");
		compiledData.put(compiledMap.copy(), fitResults.get(compiledMap));
		
		// Don't set the outputs here. Do it when the ticket is finalized.
		
		// Return status
		return true;
	}
	
	public static TreeMap<DimensionMap,Double> compiledData = null;
	
	public void finalizeTicket(Ticket ticket)
	{
		if(compiledData == null)
		{
			return;
		}
		
		// Write the file and make a JEXData
		// Put the final JEXData in all the entries
		TreeMap<JEXEntry,Set<JEXData>> outputList = ticket.getOutputList();
		for (JEXEntry entry : outputList.keySet())
		{
			String path = JEXTableWriter.writeTable(outputNames[0].getName(), new DimTable(compiledData), compiledData, JEXTableWriter.TXT_FILE);
			JEXData data = FileWriter.makeFileObject(outputNames[0].getName(), null, path);
			Set<JEXData> set = outputList.get(entry);
			set.clear();
			set.add(data);
		}
		
		compiledData = null;
	}
}
