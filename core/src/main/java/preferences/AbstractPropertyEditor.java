package preferences;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;

/**
 * AbstractPropertyEditor. <br>
 * 
 */
public class AbstractPropertyEditor<E extends Component> implements PropertyEditor {
	
	protected E editor;
	private PropertyChangeSupport listeners = new PropertyChangeSupport(this);
	
	public boolean isPaintable()
	{
		return false;
	}
	
	public boolean supportsCustomEditor()
	{
		return false;
	}
	
	public Component getCustomEditor()
	{
		return editor;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		listeners.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		listeners.removePropertyChangeListener(listener);
	}
	
	protected void firePropertyChange(Object oldValue, Object newValue)
	{
		listeners.firePropertyChange("value", oldValue, newValue);
	}
	
	public Object getValue()
	{
		return null;
	}
	
	public void setValue(Object value)
	{}
	
	public String getAsText()
	{
		return null;
	}
	
	public String getJavaInitializationString()
	{
		return null;
	}
	
	public String[] getTags()
	{
		return null;
	}
	
	public void setAsText(String text) throws IllegalArgumentException
	{}
	
	public void paintValue(Graphics gfx, Rectangle box)
	{}
	
}
