package Database.Definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class ParameterSet {
	
	LinkedHashMap<String,Parameter> params;
	
	public ParameterSet()
	{
		params = new LinkedHashMap<String,Parameter>();
	}
	
	public ParameterSet(List<Parameter> params)
	{
		this.params = new LinkedHashMap<String,Parameter>();
		for (Parameter p : params)
		{
			this.params.put(p.title, p);
		}
	}
	
	public ParameterSet duplicate()
	{
		ParameterSet result = new ParameterSet();
		for (String key : params.keySet())
		{
			result.addParameter(params.get(key).duplicate());
		}
		return result;
	}
	
	/**
	 * Add a parameter to the set
	 * 
	 * @param param
	 */
	public void addParameter(Parameter param)
	{
		this.params.put(param.title, param);
	}
	
	/**
	 * Returns the parameter with name NAME
	 * 
	 * @param name
	 * @return Parameter named NAME
	 */
	public Parameter getParameter(String name)
	{
		return params.get(name);
	}
	
	/**
	 * Get the value of parameter with name NAME
	 * 
	 * @param name
	 * @return value of parameter with name NAME
	 */
	public String getValueOfParameter(String name)
	{
		Parameter p = this.getParameter(name);
		if(p == null)
			return null;
		return p.getValue();
	}
	
	/**
	 * Set the value of parameter with name NAME
	 * 
	 * @param name
	 * @param value
	 */
	public void setValueOfParameter(String name, String value)
	{
		Parameter p = this.getParameter(name);
		if(p != null)
		{
			p.setValue(value);
		}
	}
	
	/**
	 * Return a collection of all the parameters in teh set
	 * 
	 * @return
	 */
	public Collection<Parameter> getParameters()
	{
		Collection<Parameter> result = params.values();
		return result;
	}
}
