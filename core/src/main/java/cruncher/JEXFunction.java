package cruncher;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.CrunchFactory;
import function.JEXCrunchable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import logs.Logs;
import tables.DimensionMap;

public class JEXFunction {
	
	// statics
	public static int NOTRUN = 0;
	public static int CANNOTRUNYET = -2;
	public static int CANNOTRUN = -1;
	public static int CANRUN = 1;
	
	// JEXData keys (Use underscores to attempt to avoid naming conflicts) ie.
	// Type might also be the name
	// of a parameter within the Parameters and overwrite DIMKEY. This happened
	// so I'm fixing it now
	public static String OLD_DIMKEY = "__Type__";
	public static String OLD_FUNCTIONNAME = "__FunctionName__";
	public static String INPUTS = "__Inputs__";
	public static String OUTPUTS = "__Outputs__";
	public static String PARAMETERS = "__Parameters__";
	public static String SAVINGSELECTIONS = "__SavingSelections__";
	
	public static String FUNCTION_NAME = "Function Name";
	public static String INFOTYPE = "Info Type";
	public static String INFOKEY = "Key";
	
	ParameterSet parameters;
	TreeMap<String,TypeName> inputs;
	TreeMap<Integer,TypeName> outputs;
	TreeMap<Integer,Boolean> savingSelections;
	// TypeName[] outputTNs ;
	JEXCrunchable cruncher;
	
	public JEXFunction(String functionName)
	{
		this.cruncher = CrunchFactory.getExperimentalDataCrunch(functionName);
		
		inputs = new TreeMap<String,TypeName>();
		TypeName[] inputs = cruncher.getInputNames();
		for (TypeName intput : inputs)
		{
			this.inputs.put(intput.getName(), null);
		}
		outputs = new TreeMap<Integer,TypeName>();
		TypeName[] outputs = cruncher.getOutputs();
		for (int i = 0; i < outputs.length; i++)
		{
			this.outputs.put(i, outputs[i]);
		}
		this.savingSelections = new TreeMap<Integer,Boolean>();
		for (int i = 0; i < outputs.length; i++)
		{
			this.savingSelections.put(i, new Boolean(true));
		}
		parameters = this.cruncher.requiredParameters();
		
	}
	
	public static JEXFunction fromOldJEXData(JEXData data)
	{
		// set the function name
		DimensionMap map = new DimensionMap();
		map.put(OLD_DIMKEY, OLD_FUNCTIONNAME);
		JEXDataSingle funcDS = data.getData(map);
		
		JEXFunction ret = new JEXFunction(funcDS.getValue());
		
		// Set the inputs
		ret.inputs = new TreeMap<String,TypeName>();
		DimensionMap mapInput = new DimensionMap();
		mapInput.put(OLD_DIMKEY, INPUTS);
		JEXDataSingle inputDS = data.getData(mapInput);
		Set<String> keys = inputDS.getKeys();
		for (String key : keys)
		{
			ret.inputs.put(key, new TypeName(inputDS.get(key)));
		}
		
		// set the outputs
		ret.outputs = new TreeMap<Integer,TypeName>();
		DimensionMap mapOutput = new DimensionMap();
		mapOutput.put(OLD_DIMKEY, OUTPUTS);
		JEXDataSingle outputDS = data.getData(mapOutput);
		Set<String> outkeys = outputDS.getKeys();
		for (String key : outkeys)
		{
			try
			{
				Integer intKey = Integer.parseInt(key);
				String outName = outputDS.get(key);
				
				// The type of each output was never saved before so we can't
				// actually/properly load
				// a JEXFunction properly from xml.
				// We do what we can here, setting the type to a random type so
				// that at least we
				// can see what parameters we ran the function with in the past.
				ret.outputs.put(intKey, new TypeName(JEXData.FILE, outName));
			}
			catch (NumberFormatException e)
			{
				continue;
			}
		}
		
		// set the parameters
		DimensionMap mapParams = new DimensionMap();
		mapParams.put(OLD_DIMKEY, PARAMETERS);
		JEXDataSingle paramsDS = data.getData(mapParams);
		for (String key : paramsDS.getKeys())
		{
			ret.parameters.setValueOfParameter(key, paramsDS.get(key));
		}
		
		return ret;
	}
	
	public JEXFunction(String functionName, Vector<JEXDataSingle> singles)
	{
		this(functionName);
		
		for (JEXDataSingle single : singles)
		{
			DimensionMap map = single.getDimensionMap();
			if(map.get(INFOTYPE).equals(INPUTS))
			{
				this.inputs.put(map.get(INFOKEY), new TypeName(single.getValue()));
			}
			else if(map.get(INFOTYPE).equals(OUTPUTS))
			{
				this.outputs.put(new Integer(map.get(INFOKEY)), new TypeName(single.getValue()));
			}
			else if(map.get(INFOTYPE).equals(PARAMETERS))
			{
				this.parameters.setValueOfParameter(map.get(INFOKEY), single.getValue());
			}
			else
			{ // Saving selections
				this.savingSelections.put(new Integer(map.get(INFOKEY)), Boolean.parseBoolean(single.getValue()));
			}
		}
	}
	
	public Vector<JEXDataSingle> getSingles()
	{
		Vector<JEXDataSingle> ret = new Vector<JEXDataSingle>();
		
		for (String key : inputs.keySet())
		{
			JEXDataSingle single = new JEXDataSingle();
			DimensionMap map = new DimensionMap();
			map.put(FUNCTION_NAME, this.getFunctionName());
			map.put(INFOTYPE, INPUTS);
			map.put(INFOKEY, key);
			TypeName inputTN = inputs.get(key);
			if(inputTN == null)
				continue;
			single.setValue(inputTN.toCSVString());
			single.setDimensionMap(map);
			ret.add(single);
		}
		
		for (Integer index : outputs.keySet())
		{
			JEXDataSingle single = new JEXDataSingle();
			DimensionMap map = new DimensionMap();
			map.put(FUNCTION_NAME, this.getFunctionName());
			map.put(INFOTYPE, OUTPUTS);
			map.put(INFOKEY, "" + index);
			TypeName outputTN = outputs.get(index);
			if(outputTN == null)
				continue;
			single.setValue(outputTN.toCSVString());
			single.setDimensionMap(map);
			ret.add(single);
		}
		
		for (Parameter param : parameters.getParameters())
		{
			if(param.type == Parameter.PASSWORD)
			{
				param.setValue("xxxxxxx");
			}
			JEXDataSingle single = new JEXDataSingle();
			DimensionMap map = new DimensionMap();
			map.put(FUNCTION_NAME, this.getFunctionName());
			map.put(INFOTYPE, PARAMETERS);
			map.put(INFOKEY, "" + param.getTitle());
			single.setValue(param.getValue());
			single.setDimensionMap(map);
			ret.add(single);
		}
		
		for (Integer index : savingSelections.keySet())
		{
			JEXDataSingle single = new JEXDataSingle();
			DimensionMap map = new DimensionMap();
			map.put(FUNCTION_NAME, this.getFunctionName());
			map.put(INFOTYPE, SAVINGSELECTIONS);
			map.put(INFOKEY, "" + index);
			Boolean selection = savingSelections.get(index);
			if(selection == null)
				continue;
			single.setValue(selection.toString());
			single.setDimensionMap(map);
			ret.add(single);
		}
		
		return ret;
	}
	
	public String getFunctionName()
	{
		return this.getCrunch().getName();
	}
	
	public HashSet<JEXData> run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		JEXCrunchable crunch = this.getCrunch();
		
		crunch.prepareEntry();
		boolean result = false;
		try
		{
			result = crunch.run(entry, inputs);
			crunch.finalizeEntry();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		Logs.log("Function " + this.getFunctionName() + " returned " + result, 1, this);
		HashSet<JEXData> dataOutput = crunch.getRealOutputs();
		
		// Save only data which has been selected to be saved.
		// This will cause issues for people that uncheck things that are needed to run subsequent functions.
		HashSet<JEXData> savedOutput = new HashSet<JEXData>();
		for(JEXData d : dataOutput)
		{
			for(Integer i : this.outputs.keySet())
			{
				if(this.outputs.get(i).equals(d.getTypeName()))
				{
					if(this.savingSelections.get(i))
					{
						savedOutput.add(d);
					}
				}
			}
		}
		
		return savedOutput;
	}
	
	// PARAMETERS
	
	public ParameterSet getParameters()
	{
		return parameters;
	}
	
	public void setParameters(ParameterSet params)
	{
		this.parameters = params;
		this.getCrunch().setParameters(params);
	}
	
	// INPUTS
	
	public void setInput(String inputName, TypeName inputTN)
	{
		inputs.put(inputName, inputTN);
		
	}
	
	public TreeMap<String,TypeName> getInputs()
	{
		return inputs;
	}
	
	// // OUTPUTS
	// public void checkOutputsLoaded()
	// {
	// if (outputs == null){
	// ExperimentalDataCrunch cr = this.getCrunch();
	// TypeName[] outputTNs = cr.getOutputs();
	// for(int i = 0; i < outputTNs.length; i++)
	// {
	// outputs.put(i, outputTNs[i]);
	// }
	// }
	// }
	
	public void setExpectedOutputName(int index, String outputName)
	{
		// this.checkOutputsLoaded();
		if(index >= outputs.size())
			return;
		TypeName tn = outputs.get(index);
		tn.setName(outputName);
		this.outputs.put(index, tn);
	}
	
	public void setSavingSelections(TreeMap<Integer,Boolean> selections)
	{
		this.savingSelections.putAll(duplicateSavingSelections(selections));
	}
	
	public TreeMap<Integer,Boolean> getSavingSelections()
	{
		return this.savingSelections;
	}
	
	public void setExpectedOutputs(TreeMap<Integer,TypeName> outputs)
	{
		// this.checkOutputsLoaded();
		this.outputs.putAll(duplicateOutputMap(outputs));
		JEXCrunchable cr = this.getCrunch();
		cr.setOutputs(this.getExpectedOutputs());
	}
	
	public TreeMap<Integer,TypeName> getExpectedOutputs()
	{
		// this.checkOutputsLoaded();
		return duplicateOutputMap(this.outputs);
	}
	
	public TypeName getExpectedOutputTN(int index)
	{
		// this.checkOutputsLoaded();
		if(index >= this.outputs.size())
			return null;
		return this.outputs.get(index).duplicate();
	}
	
	// ADMINISTRATIVE VARIABLES
	
	public JEXCrunchable getCrunch()
	{
		return cruncher;
	}
	
	// public void initializeCrunch(){
	// cruncher =
	// CrunchFactory.getExperimentalDataCrunch(this.getFunctionName());
	// cruncher.setOutputs(getExpectedOutputs());
	// cruncher.setParameters(parameters);
	// cruncher.setInputs(inputs);
	// }
	
	/**
	 * Return a clone of itself
	 * 
	 * @return jexfunction clone
	 */
	public JEXFunction duplicate()
	{
		JEXFunction result = new JEXFunction(this.getFunctionName());
		result.setParameters(parameters.duplicate());
		result.inputs = duplicateInputMap(inputs);
		result.outputs = duplicateOutputMap(outputs);
		result.savingSelections = duplicateSavingSelections(savingSelections);
		result.getCrunch().setOutputs(result.getExpectedOutputs());
		result.getCrunch().setCanceler(this.cruncher.canceler);
		return result;
	}
	
	// MISC
	
	/**
	 * Return a string
	 */
	@Override
	public String toString()
	{
		String result = this.getFunctionName();
		return result;
	}
	
	private static TreeMap<String,TypeName> duplicateInputMap(TreeMap<String,TypeName> inputMap)
	{
		TreeMap<String,TypeName> result = new TreeMap<String,TypeName>();
		for (String key : inputMap.keySet())
		{
			TypeName newTN = inputMap.get(key);
			if(newTN == null)
				result.put(key, null);
			else
				result.put(key, newTN.duplicate());
		}
		return result;
	}
	
	private static TreeMap<Integer,TypeName> duplicateOutputMap(TreeMap<Integer,TypeName> outputMap)
	{
		TreeMap<Integer,TypeName> result = new TreeMap<Integer,TypeName>();
		for (Integer key : outputMap.keySet())
		{
			TypeName newTN = outputMap.get(key).duplicate();
			Integer keyCopy = new Integer(key);
			result.put(keyCopy, newTN);
		}
		return result;
	}
	
	private static TreeMap<Integer,Boolean> duplicateSavingSelections(TreeMap<Integer,Boolean> selections)
	{
		TreeMap<Integer,Boolean> result = new TreeMap<Integer,Boolean>();
		for (Integer key : selections.keySet())
		{
			Boolean selection = selections.get(key).booleanValue();
			Integer keyCopy = new Integer(key);
			result.put(keyCopy, selection);
		}
		return result;
	}
	
}
