package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.JEXCrunchable;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.LSVList;
import miscellaneous.SSVList;
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
public class JEX_MakeOctaveMatrix extends JEXCrunchable {
	
	private static LSVList text = null;
	
	public JEX_MakeOctaveMatrix()
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
		String result = "Make an Octave Matrix";
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
		String result = "Function that allows you to make an numeric n-dim matrix from an numeric n-dim Value object.";
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
		String toolbox = "Octave Tools";
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
		inputNames[0] = new TypeName(VALUE, "Value");
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
		defaultOutputNames[0] = new TypeName(FILE, "m-File");
		
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
		Parameter p0 = new Parameter("Variable Name", "Name of the matrix variable that the data will be save in.", "var1");
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
		// Collect the inputs
		JEXData valueData = inputs.get("Value");
		if(valueData == null || !valueData.getTypeName().getType().equals(JEXData.VALUE))
		{
			return false;
		}
		
		// Collect the parameters
		String var1 = parameters.getValueOfParameter("Variable Name");
		
		// Get the DimTable
		DimTable valueTable = valueData.getDimTable();
		TreeMap<DimensionMap,Double> valueMap = ValueReader.readObjectToDoubleTable(valueData);
		
		// DimTable flippedValueTable = new DimTable();
		// for(int i = valueTable.size()-1; i > -1; i--)
		// {
		// flippedValueTable.add(valueTable.get(i));
		// }
		
		// Create the text of the file
		if(text == null)
		{
			text = new LSVList();
			text.add("% [Experiment = " + entry.getEntryExperiment() + "]:[Value = " + valueData.getTypeName().getName() + "]");
			text.add(""); // blank line to look prettier
			String indexOrder = "% " + var1 + "(ArrayRow,ArrayCol).data(";
			for (int i = 0; i < valueTable.size(); i++)
			{
				if(i != 0)
					indexOrder = indexOrder + ",";
				indexOrder = indexOrder + valueTable.get(i).name();
			}
			indexOrder = indexOrder + ")     [valid for first entry only]";
			text.add(indexOrder);
			text.add("");
			// String initializationText = var1 + " = zeros(";
			// CSVList dimSizeList = new CSVList();
			// for(Dim d : flippedValueTable)
			// {
			// dimSizeList.add(""+d.size());
			// }
			// initializationText = initializationText + dimSizeList.toString()
			// + ");";
			// text.add(initializationText);
			// text.add(""); // To look prettier again.
		}
		
		int numDims = valueTable.size();
		
		if(numDims >= 2)
		{
			DimTable arrayTable = new DimTable();
			arrayTable.add(valueTable.get(valueTable.size() - 2));
			arrayTable.add(valueTable.get(valueTable.size() - 1));
			DimTable remainderTable = valueTable.copy();
			remainderTable.remove(arrayTable.get(0));
			remainderTable.remove(arrayTable.get(1));
			
			List<DimensionMap> remainderMaps = remainderTable.getDimensionMaps();
			int total = remainderMaps.size();
			int count = 0;
			int percentage = 0;
			if(numDims == 2) // then remainderDims will be empty
			{
				String toAdd = var1 + "(" + (entry.getTrayY() + 1) + "," + (entry.getTrayX() + 1) + ").data(:,:) = ";
				toAdd = toAdd + get2DMatrixForDims(null, arrayTable, valueMap) + ";";
				text.add(toAdd);
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / (double) total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			else
			{
				for (DimensionMap remainderMap : remainderMaps)
				{
					String toAdd = var1 + "(" + (entry.getTrayY() + 1) + "," + (entry.getTrayX() + 1) + ").data(" + getRemainderIndicies(remainderMap, remainderTable) + ",:,:) = ";
					toAdd = toAdd + get2DMatrixForDims(remainderMap, arrayTable, valueMap) + ";";
					text.add(toAdd);
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / (double) total));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
			}
		}
		else if(numDims == 1)
		{
			DimTable arrayTable = valueTable.copy();
			
			int total = 1;
			int count = 0;
			int percentage = 0;
			
			String toAdd = var1 + "(" + (entry.getTrayY() + 1) + "," + (entry.getTrayX() + 1) + ").data(:) = ";
			toAdd = toAdd + get2DMatrixForDims(null, arrayTable, valueMap) + ";";
			text.add(toAdd);
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		else
		// numDims == 0
		{
			int total = 1;
			int count = 0;
			int percentage = 0;
			
			String toAdd = var1 + "(" + (entry.getTrayY() + 1) + "," + (entry.getTrayX() + 1) + ").data = ";
			toAdd = toAdd + get2DMatrixForDims(null, null, valueMap) + ";";
			text.add(toAdd);
			
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData placeholderData = ValueWriter.makeValueObject("Place Holder", "0");
		
		// Set the outputs
		realOutputs.add(placeholderData);
		
		// Return status
		return true;
	}
	
	private String get2DMatrixForDims(DimensionMap remainderMap, DimTable arrayTable, TreeMap<DimensionMap,Double> valueMap)
	{
		Dim row, col;
		if(arrayTable == null || arrayTable.size() == 0)
		{
			row = new Dim("Row", 1, 1);
			col = new Dim("Col", 1, 1);
		}
		else if(arrayTable.size() == 1)
		{
			row = arrayTable.get(0);
			col = new Dim("Col", 1, 1);
		}
		else
		{
			row = arrayTable.get(0);
			col = arrayTable.get(1);
		}
		
		SSVList matrixData = new SSVList();
		for (String r : row.values())
		{
			CSVList rowData = new CSVList();
			for (String c : col.values())
			{
				DimensionMap map = new DimensionMap();
				if(remainderMap != null)
				{
					DimensionMap remMapCopy = remainderMap.copy();
					for (String key : remMapCopy.keySet())
					{
						map.put(key, remMapCopy.get(key));
					}
				}
				map.put(row.name(), r);
				map.put(col.name(), c);
				Double val = valueMap.get(map);
				rowData.add(val.toString());
			}
			matrixData.add(rowData.toString());
		}
		return "[" + matrixData.toString() + "]";
	}
	
	private String getRemainderIndicies(DimensionMap remainderMap, DimTable remainderTable)
	{
		CSVList indexList = new CSVList();
		for (Dim d : remainderTable)
		{
			String dimVal = remainderMap.get(d.name());
			indexList.add("" + (d.index(dimVal) + 1));
		}
		return indexList.toString();
	}
	
	@Override
	public void finalizeTicket(Ticket t)
	{
		if(text == null)
		{
			return;
		}
		
		// Write the file and make a JEXData
		// Put the final JEXData in all the entries
		TreeMap<JEXEntry,Set<JEXData>> outputList = t.getOutputList();
		for (JEXEntry entry : outputList.keySet())
		{
			String path = JEXWriter.saveText(text.toString(), "m");
			JEXData data = FileWriter.makeFileObject(outputNames[0].getType(), outputNames[0].getName(), path);
			Set<JEXData> set = outputList.get(entry);
			set.clear();
			set.add(data);
		}
		
		text = null;
	}
}
