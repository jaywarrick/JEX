package image.roi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import signals.Observable;
import signals.Observer;
import signals.SSCenter;

public class PointMapSet<E extends Observable> implements Observer {
	
	/**
	 * This class uses getters and setters to file and access each item in the dictionary. Just set up which accessors to use and 'set' is automatically applied to be able to change the item if the setter is ever called.
	 */
	
	// ASK ERWIN WHETHER THE DEFAULT HASH IS ADEQUATE FOR THE HASHSETS
	private TreeMap<Integer,HashSet<E>> dictionary;
	private String accessorName;
	
	public PointMapSet(String accessorName)
	{
		this.accessorName = accessorName;
		this.dictionary = new TreeMap<Integer,HashSet<E>>();
	}
	
	public void put(E obj)
	{
		this.silentPut(obj);
		
		this.observe(obj);
	}
	
	private void silentPut(E obj)
	{
		if(obj == null)
		{
			return;
			// Print error message here.
		}
		
		Integer xkey = this.keyForObject(obj);
		if(xkey == null)
		{
			return;
		}
		HashSet<E> currentSet = this.getSet(xkey);
		
		// Create new set if necessary
		if(currentSet == null)
		{
			currentSet = new HashSet<E>();
			this.dictionary.put(xkey, currentSet);
		}
		
		currentSet.add(obj);
	}
	
	// ASK ERWIN WHETHER THE DEFAULT HASH IS ADEQUATE FOR THE HASHSETS
	public void remove(E obj)
	{
		this.silentRemove(obj);
		
		this.unobserve(obj);
	}
	
	private void silentRemove(E obj)
	{
		Integer xkey = this.keyForObject(obj);
		if(xkey == null)
		{
			return;
		}
		HashSet<E> currentSet = this.getSet(xkey);
		if(currentSet == null)
		{
			return;
		}
		currentSet.remove(obj);
		
		// Remove set if necessary
		if(currentSet.size() == 0)
		{
			this.dictionary.remove(xkey);
		}
	}
	
	public void removeAll()
	{
		Iterator<Integer> key = this.iterator();
		while (key.hasNext())
		{
			HashSet<E> set = this.getSet(key.next());
			for (E item : set)
			{
				this.unobserve(item);
			}
		}
		this.dictionary.clear();
	}
	
	public HashSet<E> getSet(Integer xkey)
	{
		return this.dictionary.get(xkey);
	}
	
	public Iterator<Integer> iterator()
	{
		return this.dictionary.keySet().iterator();
	}
	
	public String accessorName()
	{
		return this.accessorName;
	}
	
	public int size()
	{
		return this.dictionary.size();
	}
	
	private void observe(E obj)
	{
		SSCenter.observe(this, obj);
	}
	
	private void unobserve(E obj)
	{
		SSCenter.unobserve(this, obj);
	}
	
	private Integer keyForObject(E o)
	{
		Method m;
		
		try
		{
			m = o.getClass().getMethod(this.accessorName, (Class[]) null);
			if(m != null)
			{
				return (Integer) m.invoke(o, (Object[]) null);
			}
			else
			{
				return null;
			}
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (NoSuchMethodException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void changed(Observable obj, String accessorName)
	{
		this.silentPut((E) obj);
		// JEXStatics.Manager.log("Changed: " + obj.toString(), 0, this);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void changeAborted(Observable obj, String accessorName)
	{
		this.silentPut((E) obj);
		// Logs.log("Change Aborted: " + obj.toString(), 0,
		// this);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void changing(Observable obj, String accessorName)
	{
		this.silentRemove((E) obj);
		// Logs.log("Changing: " + obj.toString(), 0, this);
	}
	
	@Override
	public String toString()
	{
		// String ret = "";
		// Iterator<XKey> itr = this.iterator();
		// while(itr.hasNext())
		// {
		// XKey tempKey = itr.next();
		// ret = ret + "\n";
		// ret = ret +
		// }
		// Logs.log("size: "+this.size(), 0, this);
		return this.dictionary.toString();
	}
	
	public Integer firstKey()
	{
		return this.dictionary.firstKey();
	}
	
	public Integer lastKey()
	{
		return this.dictionary.lastKey();
	}
	
}
