package plugins.viewer;

import miscellaneous.Copiable;

public class ChangeMap<E extends Copiable<E>> {
	
	private E oldObject;
	private E newObject;
	
	ChangeMap()
	{
		this.oldObject = null;
		this.newObject = null;
	}
	
	public E oldest()
	{
		return this.oldObject;
	}
	
	public E newest()
	{
		return this.newObject;
	}
	
	public void updateObject(E e)
	{
		if(this.oldObject == null)
		{
			this.oldObject = e.copy();
		}
		this.newObject = e.copy();
	}
	
}
