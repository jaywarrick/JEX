package preferences;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

/**
 * AbstractProperty. <br>
 * 
 */
public abstract class AbstractProperty implements Property {
	
	private static final long serialVersionUID = 1L;
	
	private Object value;
	
	// PropertyChangeListeners are not serialized.
	private transient PropertyChangeSupport listeners = new PropertyChangeSupport(this);
	
	public Object getValue()
	{
		return value;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}
	
	public void setValue(Object value)
	{
		Object oldValue = this.value;
		this.value = value;
		if(value != oldValue && (value == null || !value.equals(oldValue)))
			firePropertyChange(oldValue, getValue());
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		listeners.addPropertyChangeListener(listener);
		Property[] subProperties = getSubProperties();
		if(subProperties != null)
			for (int i = 0; i < subProperties.length; ++i)
				subProperties[i].addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		listeners.removePropertyChangeListener(listener);
		Property[] subProperties = getSubProperties();
		if(subProperties != null)
			for (int i = 0; i < subProperties.length; ++i)
				subProperties[i].removePropertyChangeListener(listener);
	}
	
	protected void firePropertyChange(Object oldValue, Object newValue)
	{
		listeners.firePropertyChange("value", oldValue, newValue);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		listeners = new PropertyChangeSupport(this);
	}
	
	public Property getParentProperty()
	{
		return null;
	}
	
	public Property[] getSubProperties()
	{
		return null;
	}
}
