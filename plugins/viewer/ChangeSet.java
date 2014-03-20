package plugins.viewer;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import logs.Logs;
import miscellaneous.Copiable;
import signals.SSCenter;
import Database.DBObjects.JEXEntry;

public class ChangeSet<E extends Copiable<E>> {
	
	public static final String SIG_Updated_NULL = "SIG_Updated_NULL";
	
	public static final int OLD = 0, NEW = 1;
	private HashMap<JEXEntry,HashMap<String,ChangeMap<E>>> map;
	
	public ChangeSet()
	{
		this.map = new HashMap<JEXEntry,HashMap<String,ChangeMap<E>>>();
	}
	
	public Set<JEXEntry> keySet()
	{
		return this.map.keySet();
	}
	
	public void updateObject(JEXEntry e, String name, E object)
	{
		HashMap<String,ChangeMap<E>> temp = this.map.get(e);
		if(temp == null)
		{
			temp = new HashMap<String,ChangeMap<E>>();
		}
		ChangeMap<E> changeMap = temp.get(name);
		if(changeMap == null)
		{
			changeMap = new ChangeMap<E>();
		}
		changeMap.updateObject(object);
		temp.put(name, changeMap);
		this.map.put(e, temp);
		SSCenter.defaultCenter().emit(this, SIG_Updated_NULL, (Object[]) null);
	}
	
	public boolean containsName(String name)
	{
		HashMap<String,ChangeMap<E>> temp;
		for (JEXEntry e : this.map.keySet())
		{
			temp = this.map.get(e);
			if(temp.containsKey(name))
				return true;
		}
		return false;
	}
	
	// @SuppressWarnings("unchecked")
	// /**
	// * Old objects cannot be overwritten. This allows one to always attempt
	// * to put an old object in the change map but olny keep the first
	// */
	// private boolean putOld(JEXEntry e, String newName, E oldObject)
	// {
	// HashMap<String,ChangeMap<E>> temp = this.map.get(e);
	// if(temp == null)
	// {
	// temp = new HashMap<String,ChangeMap<E>>();
	// }
	// ChangeMap<E> changeMap = temp.get(newName);
	// if(changeMap == null)
	// {
	// changeMap = new ChangeMap<E>();
	// changeMap.oldest() = oldObject.copy();
	// }
	// else
	// {
	// //Logs.log("Can't add old object! An old object already exists for this entry and newName",
	// 0, this);
	// return false;
	// }
	// temp.put(newName, changeMap);
	// this.map.put(e, temp);
	// return true;
	// }
	//
	// @SuppressWarnings("unchecked")
	// private boolean putNew(JEXEntry e, String newName, E newObject)
	// {
	// HashMap<String,ChangeMap<E>> temp = this.map.get(e);
	// if(temp == null)
	// {
	// temp = new HashMap<String,ChangeMap<E>>();
	// }
	// ChangeMap<E> changeMap = temp.get(newName);
	// if(changeMap == null)
	// {
	// changeMap = new ChangeMap<E>();
	// }
	// changeMap.updateTo(newObject.copy());
	// return true;
	// }
	
	public boolean updateObjectWithChangedName(JEXEntry e, E object, String oldName, String newName)
	{
		HashMap<String,ChangeMap<E>> temp = this.map.get(e);
		if(temp == null)
		{
			Logs.log("Can't change name! No change map found for this entry", 0, this);
			return false;
		}
		ChangeMap<E> changeMap = temp.get(oldName);
		if(changeMap == null)
		{
			Logs.log("Can't change name! No change map found for this entry and oldName", 0, this);
			return false;
		}
		else
		{
			temp.remove(oldName);
			temp.put(newName, changeMap);
			changeMap.updateObject(object);
			SSCenter.defaultCenter().emit(this, SIG_Updated_NULL, (Object[]) null);
			return true;
		}
	}
	
	public ChangeMap<E> getChangeMap(JEXEntry e, String newName)
	{
		if(e == null || newName == null)
			return null;
		HashMap<String,ChangeMap<E>> temp = this.map.get(e);
		if(temp == null)
			return null;
		return temp.get(newName);
	}
	
	public void removeChangeMap(JEXEntry e, String newName)
	{
		if(e == null || newName == null)
			return;
		HashMap<String,ChangeMap<E>> temp = this.map.get(e);
		if(temp == null)
			return;
		temp.remove(newName);
	}
	
	public List<ChangeMap<E>> getChangeMaps(JEXEntry e)
	{
		List<ChangeMap<E>> ret = new Vector<ChangeMap<E>>();
		HashMap<String,ChangeMap<E>> temp = this.map.get(e);
		if(temp == null)
			return ret;
		for (String s : temp.keySet())
		{
			ret.add(temp.get(s));
		}
		return ret;
	}
	
	public void clear()
	{
		this.map.clear();
		SSCenter.defaultCenter().emit(this, SIG_Updated_NULL, (Object[]) null);
	}
	
	public int size()
	{
		int ret = 0;
		for (JEXEntry e : this.map.keySet())
		{
			ret = ret + this.map.get(e).size();
		}
		return ret;
	}
}
