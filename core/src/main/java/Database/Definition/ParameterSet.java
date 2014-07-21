package Database.Definition;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

public class ParameterSet {
	
	private TreeMap<String,Parameter> paramsByName;
	private Vector<Parameter> paramsByOrderAdded;
	
	public ParameterSet()
	{
		paramsByName = new TreeMap<String,Parameter>();
		paramsByOrderAdded = new Vector<Parameter>();
	}
	
	public ParameterSet(List<Parameter> params)
	{
		this.paramsByName = new TreeMap<String,Parameter>();
		for (Parameter p : params)
		{
			this.paramsByName.put(p.title, p);
		}
	}
	
	public ParameterSet duplicate()
	{
		ParameterSet result = new ParameterSet();
		for (String key : paramsByName.keySet())
		{
			result.addParameter(paramsByName.get(key).duplicate());
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
		this.paramsByName.put(param.title, param);
		this.paramsByOrderAdded.add(param);
	}
	
	/**
	 * Returns the parameter with name NAME
	 * 
	 * @param name
	 * @return Parameter named NAME
	 */
	public Parameter getParameter(String name)
	{
		return paramsByName.get(name);
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
		return this.paramsByOrderAdded;
	}
}
