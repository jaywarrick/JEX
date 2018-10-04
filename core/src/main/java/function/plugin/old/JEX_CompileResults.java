package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.LabelReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.JEXCrunchable;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
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
public class JEX_CompileResults extends JEXCrunchable {
	
	public JEX_CompileResults()
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
		String result = "Compile Results Tables";
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
		String result = "Function that groups tables of the same name from different entries into a single table with the option to include label information.";
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
		inputNames[0] = new TypeName(FILE, "Results Files");
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
		defaultOutputNames[0] = new TypeName(FILE, "Compiled Results File");
		
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
		JEXData data = inputs.get("Results Files");
		if(data == null || !data.getTypeName().getType().matches(JEXData.FILE))
		{
			return false;
		}
		
		TreeMap<DimensionMap,String> tables = FileReader.readObjectToFilePathTable(data);
		TreeMap<DimensionMap,Double> tableDatas = new TreeMap<DimensionMap,Double>();
		double count = 0;
		double total = tables.size();
		double percentage = 0;
		for (DimensionMap map : tables.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			Table<Double> tableData = JEXTableReader.getNumericTable(tables.get(map));
			for (Entry<DimensionMap,Double> e : tableData.data.entrySet())
			{
				DimensionMap map2 = e.getKey().copy();
				map2.putAll(map);
				tableDatas.put(map2, e.getValue());
			}
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}
		
		// Collect Parameters
		String sortingLabelsString = parameters.getValueOfParameter("Sorting Labels");
		CSVList sortingLabels = new CSVList();
		if(sortingLabelsString != null && !sortingLabelsString.equals(""))
		{
			sortingLabels = new CSVList(sortingLabelsString);
		}
		
		// Save the data.
		DimensionMap compiledMap = new DimensionMap();
		if(compiledData == null)
		{
			compiledData = new TreeMap<DimensionMap,Double>();
		}
		compiledMap = new DimensionMap();
		compiledMap.put("Experiment", entry.getEntryExperiment());
		// compiledMap.put("Array Name", entry.getEntryTrayName());
		compiledMap.put("Array X", "" + entry.getTrayX());
		compiledMap.put("Array Y", "" + entry.getTrayY());
		for (String labelName : sortingLabels)
		{
			JEXData label = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, labelName), entry);
			compiledMap.put(labelName, LabelReader.readLabelValue(label));
		}
		
		for (Entry<DimensionMap,Double> e : tableDatas.entrySet())
		{
			DimensionMap map = e.getKey().copy();
			map.putAll(compiledMap);
			compiledData.put(map, e.getValue());
		}
		
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
			JEXData data = FileWriter.makeFileObject(outputNames[0].getName(),null, path);
			Set<JEXData> set = outputList.get(entry);
			set.clear();
			set.add(data);
		}
		
		compiledData = null;
	}
}
