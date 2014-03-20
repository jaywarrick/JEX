package jex.utilities;

import java.util.HashMap;
import java.util.Iterator;

public class XKey {
	
	private HashMap<String,Object> keys;
	
	public XKey()
	{
		this.keys = new HashMap<String,Object>();
	}
	
	public XKey(String[] properties, Object[] values) throws IllegalArgumentException
	{
		this.keys = new HashMap<String,Object>();
		if(properties == null || values == null)
		{
			throw (new IllegalArgumentException("null arguments not allowed"));
		}
		if(properties.length != values.length)
		{
			throw (new IllegalArgumentException("argument lengths must be equal"));
		}
		for (int i = 0; i < values.length; i++)
		{
			if(values[i] == null || properties[i] == null || properties[i].equals(""))
			{
				throw (new IllegalArgumentException("null/empty objects in arrays not allowed"));
			}
			this.putKey(properties[i], values[i]);
		}
	}
	
	public void putKey(String key, Object o)
	{
		this.keys.put(key, o);
	}
	
	public void removeKey(String key)
	{
		this.keys.remove(key);
	}
	
	public Object getKey(String key)
	{
		return this.keys.get(key);
	}
	
	@Override
	public boolean equals(Object xkey)
	{
		if(xkey instanceof XKey)
		{
			if(this.size() == ((XKey) xkey).size())
			{
				Iterator<String> itr = ((XKey) xkey).iterator();
				String tempKey;
				while (itr.hasNext())
				{
					tempKey = itr.next();
					if(!this.getKey(tempKey).equals(((XKey) xkey).getKey(tempKey)))
					{
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public int size()
	{
		return this.keys.size();
	}
	
	public Iterator<String> iterator()
	{
		return this.keys.keySet().iterator();
	}
	
	@Override
	public int hashCode()
	{
		Iterator<String> itr = this.iterator();
		int ret = 0;
		while (itr.hasNext())
		{
			ret = ret + this.getKey(itr.next()).hashCode();
		}
		return ret;
	}
	
	@Override
	public String toString()
	{
		return this.keys.toString();
	}
	
}
