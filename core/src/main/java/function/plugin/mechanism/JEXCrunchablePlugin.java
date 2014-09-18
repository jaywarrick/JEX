package function.plugin.mechanism;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.plugin.IJ2.IJ2PluginUtility;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import logs.Logs;
import miscellaneous.Canceler;

import org.scijava.InstantiableException;
import org.scijava.util.ClassUtils;
import org.scijava.util.ConversionUtils;


public class JEXCrunchablePlugin extends JEXCrunchable {
	
	public JEXPluginInfo info;
	public JEXPlugin plugin;
	
	public JEXCrunchablePlugin(JEXPluginInfo info)
	{
		this.info = info;
		try
		{
			this.plugin = info.createJEXPlugin();
		}
		catch (InstantiableException e)
		{
			Logs.log("Couldn't instantiate the JEXPlugin.", Logs.ERROR, this);
			e.printStackTrace();
		}
	}
	
	public int getMaxThreads()
	{
		int max = this.plugin.getMaxThreads();
		if(max < 1)
		{
			return 1;
		}
		return max;
	}
	
	@Override
	public boolean showInList()
	{
		return this.info.getInfo().isVisible();
	}
	
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return this.info.getInfo().getName();
	}
	
	@Override
	public String getInfo()
	{
		return this.info.getInfo().getDescription();
	}
	
	@Override
	public String getToolbox()
	{
		return IJ2PluginUtility.getToolboxString("JEX", this.info.getInfo().getMenuPath());
	}
	
	@Override
	public TypeName[] getInputNames()
	{
		Vector<TypeName> ret = new Vector<TypeName>();
		for(String inputName : this.info.inputs.keySet())
		{
			ret.add(this.info.inputs.get(inputName));
		}
		return ret.toArray(new TypeName[ret.size()]);
	}
	
	@Override
	public ParameterSet requiredParameters()
	{
		ParameterSet ret = new ParameterSet();
		for(String paramKey : this.info.pOrder.keySet())
		{
			String paramName = this.info.pOrder.get(paramKey);
			ret.addParameter(this.info.parameters.get(paramName));
		}
		return ret;
	}
	
	@Override
	public TypeName[] getOutputs()
	{
		Vector<TypeName> ret = new Vector<TypeName>();
		for(String outputName : this.info.outputs.keySet())
		{
			ret.add(this.info.outputs.get(outputName));
		}
		return ret.toArray(new TypeName[ret.size()]);
	}
	
	@Override
	public boolean allowMultithreading()
	{
		return this.getMaxThreads() > 1;
	}
	
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		this.setPluginInputs(inputs);
		this.setPluginParameters();
		
		boolean success = this.plugin.run(entry);
		
		if(!success)
		{
			return success;
		}
		
		HashSet<JEXData> outputs = this.getPluginOutputs();
		
		this.realOutputs.addAll(outputs);
		
		return true;
	}
	
	private void setPluginInputs(HashMap<String,JEXData> jexDatas)
	{
		for(String inputName : jexDatas.keySet())
		{
			JEXData input = jexDatas.get(inputName);
			ClassUtils.setValue(this.info.iFields.get(inputName), this.plugin, input);
		}
	}
	
	private void setPluginParameters()
	{
		for(Database.Definition.Parameter parameter : this.parameters.getParameters())
		{
			Object o = convertToValue(parameter.getValue(), this.info.pField.get(parameter.getTitle()));
			ClassUtils.setValue(this.info.pField.get(parameter.getTitle()), this.plugin, o);
		}
	}
	
	private HashSet<JEXData> getPluginOutputs()
	{
		HashSet<JEXData> ret = new HashSet<JEXData>();
		for(String outputName : this.info.outputs.keySet())
		{
			Field outputField = this.info.oField.get(outputName);
			int i = this.getIndexOfOutput(outputName);
			Object output = ClassUtils.getValue(outputField, this.plugin);
			if(output instanceof JEXData)
			{
				JEXData dataOutput = (JEXData) output;
				dataOutput.setDataObjectName(this.outputNames[i].getName());
				ret.add(dataOutput);
			}
		}
		return ret;
	}
	
	@SuppressWarnings("deprecation")
	private Object convertToValue(String o, Field f)
	{
		if(ConversionUtils.canConvert(o, f.getType()))
		{
			try
			{
				Object ret = ConversionUtils.convert(o, f.getType());
				return ret;
			}
			catch(Exception e)
			{
				Logs.log("Couldn't convert parameter. Value = " + o + ", Type to convert to = " + f.getType().getSimpleName(), Logs.ERROR, this);
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	public int getIndexOfOutput(String pluginOutputName)
	{
		int i = 0;
		for(String outputName : this.info.outputs.keySet())
		{
			if(outputName.equals(pluginOutputName))
			{
				return i;
			}
			i++;
		}
		return -1;
	}
	
	@Override
	public void setCanceler(Canceler canceler)
	{
		this.plugin.setCanceler(canceler);
	}
	
	@Override
	public Canceler getCanceler()
	{
		return this.plugin.canceler;
	}
	
	@Override
	public boolean isCanceled()
	{
		return this.plugin.isCanceled();
	}
	
}
