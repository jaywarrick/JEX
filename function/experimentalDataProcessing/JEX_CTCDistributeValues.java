package function.experimentalDataProcessing;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

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
public class JEX_CTCDistributeValues extends ExperimentalDataCrunch {
	
	// public static TreeMap<DimensionMap,String> valueMap = null;
	public static final int ENTRY = 0, SEEDED = 1, MRNA = 2, EPI = 3, BLOOD = 4, TUBE = 5, EQUIV = 6;
	
	public JEX_CTCDistributeValues()
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
		String result = "CTC Distribute Blood Volumes";
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
		String toolbox = "CTC Tools";
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
		defaultOutputNames = new TypeName[7];
		defaultOutputNames[0] = new TypeName(VALUE, "uL seeded");
		defaultOutputNames[1] = new TypeName(VALUE, "uL for mRNA");
		defaultOutputNames[2] = new TypeName(VALUE, "uL for EPISpot");
		defaultOutputNames[3] = new TypeName(VALUE, "mL whole blood to start");
		defaultOutputNames[4] = new TypeName(VALUE, "mL equivalent seeded");
		
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
		Parameter p0 = new Parameter("uL seeded", "Separate values with a comma", "");
		Parameter p1 = new Parameter("uL for mRNA", "Separate values with a comma", "");
		Parameter p2 = new Parameter("uL for EPISpot", "Separate values with a comma", "");
		Parameter p3 = new Parameter("mL whole blood to start", "Separate values with a comma", "");
		Parameter p4 = new Parameter("Rows", "Number of rows.", "2");
		Parameter p5 = new Parameter("Cols", "Number of columns.", "1");
		Parameter p6 = new Parameter("Order", "Fill row then go to next column (Row) or fill column and then go to next row (Column)", Parameter.DROPDOWN, new String[] { "Row", "Column" });
		Parameter p7 = new Parameter("Came from which tube?", "Separate values with a comma", "1,2");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
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
		// uL seeded
		String seededString = parameters.getValueOfParameter("uL seeded");
		CSVList seededList = new CSVList(seededString);
		JEXData output1 = this.parseParam(entry, seededList, outputNames[0].getName());
		
		// uL for mRNA
		String mrnaString = parameters.getValueOfParameter("uL for mRNA");
		CSVList mrnaList = new CSVList(mrnaString);
		JEXData output2 = this.parseParam(entry, mrnaList, outputNames[1].getName());
		
		// uL for EPISpot
		String epispotString = parameters.getValueOfParameter("uL for EPISpot");
		CSVList epispotList = new CSVList(epispotString);
		JEXData output3 = this.parseParam(entry, epispotList, outputNames[2].getName());
		
		// mL whole blood to start
		String bloodString = parameters.getValueOfParameter("mL whole blood to start");
		CSVList bloodList = new CSVList(bloodString);
		JEXData output4 = this.parseParam(entry, bloodList, outputNames[3].getName());
		
		// mL equivalent seeded
		// If there are multiple seeded channels from one sample, collecting the
		// volume from all additions
		String tube = parameters.getValueOfParameter("Came from which tube?");
		CSVList tubeList = new CSVList(tube);
		
		// This is the meat
		List<CSVList> devices = this.transversitizeit(seededList, mrnaList, epispotList, bloodList, tubeList);
		List<List<CSVList>> oncoQuicks = this.groupify(devices);
		for (List<CSVList> oncoQuick : oncoQuicks)
		{
			this.calcmLsPerMicroLiter(oncoQuick);
		}
		CSVList seedequivList = new CSVList();
		for (int i = 0; i < devices.size(); i++)
		{
			for (CSVList device : devices)
			{
				if(device.get(ENTRY).equals("" + i))
				{
					seedequivList.add(device.get(EQUIV));
				}
			}
		}
		
		// devices holds all
		JEXData output5 = this.parseParam(entry, seedequivList, outputNames[4].getName());
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		realOutputs.add(output4);
		realOutputs.add(output5);
		
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
	
	public JEXData parseParam(JEXEntry entry, CSVList param, String outputName)
	{
		if(param == null || param.size() == 0)
			return null;
		boolean rowOrder4 = parameters.getValueOfParameter("Order").equals("Row");
		int rows4 = Integer.parseInt(parameters.getValueOfParameter("Rows"));
		int cols4 = Integer.parseInt(parameters.getValueOfParameter("Cols"));
		
		TreeMap<DimensionMap,String> valueMap = getValueMap(param, rows4, cols4, rowOrder4);
		
		DimensionMap valueToGet4 = new DimensionMap();
		valueToGet4.put("R", "" + entry.getTrayY());
		valueToGet4.put("C", "" + entry.getTrayX());
		
		return ValueWriter.makeValueObject(outputName, valueMap.get(valueToGet4));
	}
	
	public List<CSVList> transversitizeit(CSVList seededList, CSVList mrnaList, CSVList epispotList, CSVList bloodList, CSVList tubeList)
	{
		List<CSVList> oncoQuick = new Vector<CSVList>();
		CSVList channel = new CSVList();
		for (int i = 0; i < seededList.size(); i++)
		{
			channel = new CSVList();
			channel.add("" + i);
			channel.add(seededList.get(i));
			channel.add(mrnaList.get(i));
			channel.add(epispotList.get(i));
			channel.add(bloodList.get(i));
			channel.add(tubeList.get(i));
			oncoQuick.add(i, channel);
		}
		return oncoQuick;
	}
	
	public List<List<CSVList>> groupify(List<CSVList> oncoQuick)
	{
		List<List<CSVList>> ret = new Vector<List<CSVList>>();
		List<CSVList> group;
		int i = 0;
		while (i < oncoQuick.size() + 1)
		{
			group = new Vector<CSVList>();
			for (int j = 0; j < oncoQuick.size(); j++)
			{
				if(Integer.parseInt(oncoQuick.get(j).get(TUBE)) == i)
				{
					group.add(oncoQuick.get(j));
				}
			}
			if(group.size() > 0)
				ret.add(group);
			i++;
		}
		return ret;
	}
	
	public void calcmLsPerMicroLiter(List<CSVList> group)
	{
		double totalSeeded = 0;
		double seeded = 0, mRNA = 0, epiSpot = 0, blood = 0;
		for (CSVList tube : group)
		{
			seeded = Double.parseDouble(tube.get(SEEDED));
			totalSeeded = totalSeeded + seeded;
		}
		for (CSVList tube : group)
		{
			mRNA = Double.parseDouble(tube.get(MRNA));
			epiSpot = Double.parseDouble(tube.get(EPI));
			blood = Double.parseDouble(tube.get(BLOOD));
			seeded = Double.parseDouble(tube.get(SEEDED));
			double ratio = blood / (mRNA + epiSpot + totalSeeded);
			double seedEquiv;
			seedEquiv = seeded * ratio;
			tube.add("" + seedEquiv);
		}
	}
	
	@Override
	public void finalizeTicket(Ticket t)
	{
		// valueMap = null;
	}
}
