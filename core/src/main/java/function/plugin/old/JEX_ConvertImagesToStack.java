package function.plugin.old;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
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
public class JEX_ConvertImagesToStack extends JEXCrunchable {
	
	public JEX_ConvertImagesToStack()
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
		String result = "Convert Images to Stack";
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
		String result = "Function that takes a list of images in a JEX database and converts them into an image stack.";
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
		String toolbox = "Stack processing";
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Timepoint Stack");
		
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
		Parameter p0 = new Parameter("Image Names", "Separate names with a comma", "");
		Parameter p1 = new Parameter("New Dimension Name", "Creates a new dimension name", "T");
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
		// Run the function
		// collect the parameters, define as "inputs"
		CSVList nameList = new CSVList(parameters.getValueOfParameter("Image Names"));
		if(nameList == null || nameList.size() < 2)
			return false;
		int n = nameList.size();
		String dimName = parameters.getValueOfParameter("New Dimension Name");
		
		// gather list of inputs corresponding to JexData
		TreeMap<DimensionMap,String> imageMap = new TreeMap<DimensionMap,String>();
		Vector<Boolean> hasImage = new Vector<Boolean>();
		TypeName tn;
		JEXData temp;
		int count = 0;
		DimTable dimT1 = null, dimT2 = null;
		DimensionMap newMap;
		for (String s : nameList)
		{
			tn = new TypeName(JEXData.IMAGE, s);
			temp = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, entry);
			if(temp != null)
			{
				if(dimT1 == null)
					dimT1 = temp.getDimTable();
				dimT2 = temp.getDimTable();
				if(!dimT1.matches(dimT2))
					return false;
				TreeMap<DimensionMap,String> oldImageMap = ImageReader.readObjectToImagePathTable(temp);
				for (DimensionMap map : oldImageMap.keySet())
				{
					newMap = map.copy();
					newMap.put(dimName, "" + count);
					imageMap.put(newMap, ImageReader.readObjectToImagePath(temp, map));
				}
				hasImage.add(true);
			}
			else
			{
				hasImage.add(false);
				
			}
			count = count + 1;
		}
		Vector<String> newDimValues = new Vector<String>();
		for (int i = 0; i < n; i++)
		{
			if(hasImage.get(i))
			{
				newDimValues.add("" + i);
			}
		}
		if(newDimValues.size() == 0)
			return false;
		Dim dim = new Dim(dimName, newDimValues);
		dimT1.add(0, dim);
		
		// Now we have a dim table with a set of images, put them into a stack
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), imageMap);
		output1.setDimTable(dimT1);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
}
