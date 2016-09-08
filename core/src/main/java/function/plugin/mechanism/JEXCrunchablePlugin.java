package function.plugin.mechanism;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.scijava.InstantiableException;
import org.scijava.util.ClassUtils;
import org.scijava.util.ConversionUtils;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.JEXCrunchable;
import function.plugin.IJ2.IJ2PluginUtility;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Canceler;


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
	
	// run before starting
	public void prepareEntry()
	{
		// Create an empty JEXData with the right type and name in case you want that information within the function during run.
		for(String outputName : this.info.outputs.keySet())
		{
			Field outputField = this.info.oFields.get(outputName);
			JEXData emptyJEXData = new JEXData(this.info.outputs.get(outputName));
			outputField.setAccessible(true);
			ClassUtils.setValue(outputField, this.plugin, emptyJEXData);
		}
	}
	
	// run after ending
	public void finalizeEntry()
	{
		JEXStatics.statusBar.setProgressPercentage(0);
	}
	
	public void prepareTicket()
	{   
		this.plugin.prepareTicket();
	}
	
	public void finalizeTicket(Ticket ticket)
	{   
		this.plugin.finalizeTicket(ticket);
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
		for(String inputKey : this.info.iOrder.keySet())
		{
			String inputName = this.info.iOrder.get(inputKey);
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
			ret.addParameter(this.info.parameters.get(paramName).duplicate());
		}
		return ret;
	}
	
	@Override
	public TypeName[] getOutputs()
	{
		Vector<TypeName> ret = new Vector<TypeName>();
		for(String outputKey : this.info.oOrder.keySet())
		{
			String outputName = this.info.oOrder.get(outputKey);
			if(JEXData.class.isAssignableFrom(this.info.oFields.get(outputName).getType()))
			{
				ret.add(this.info.outputs.get(outputName));
			}
			else
			{
				Logs.log("Found output but not a single JEXData. Could be a collection or map of JEXDatas...", this);
			}
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
	
	public void setPluginParameters()
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
			Field outputField = this.info.oFields.get(outputName);
			int i = this.getIndexOfOutput(outputName);
			Object output = ClassUtils.getValue(outputField, this.plugin);
			if(output instanceof JEXData)
			{
				JEXData dataOutput = (JEXData) output;
				dataOutput.setDataObjectName(this.outputNames[i].getName());
				ret.add(dataOutput);
			}
			if(output instanceof Collection && ((Collection<?>) output).size() > 0)
			{
				int j = 1;
				for(Object o : ((Collection<?>) output))
				{
					if(o instanceof JEXData)
					{
						JEXData dataOutput = (JEXData) o;
						if(this.outputNames.length >= i && (dataOutput.getDataObjectName() == null || dataOutput.getDataObjectName().equals("")))
						{
							dataOutput.setDataObjectName(this.outputNames[i].getName() + " - " + j);
						}
						ret.add(dataOutput);
					}
					j = j + 1;
				}
			}
			if(output instanceof Map && ((Map<?,?>) output).size() > 0)
			{
				for(Entry<?,?> o : ((Map<?,?>) output).entrySet())
				{
					if(o.getValue() instanceof JEXData)
					{
						JEXData dataOutput = (JEXData) o.getValue();
						if(this.outputNames.length >= i && (dataOutput.getDataObjectName() == null || dataOutput.getDataObjectName().equals("")))
						{
							dataOutput.setDataObjectName(this.outputNames[i].getName() + " - " + o.getKey());
						}
						ret.add(dataOutput);
					}
				}
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
		for(String outputName : this.info.oOrder.values())
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
