package preferences;

import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * Property. <br>
 * Component of a PropertySheet, based on the java.beans.PropertyDescriptor for easy wrapping of beans in PropertySheet.
 */
public interface Property extends Serializable, Cloneable {
	
	public String getName();
	
	public String getDisplayName();
	
	public String getShortDescription();
	
	@SuppressWarnings("rawtypes")
	public Class getType();
	
	public Object getValue();
	
	public void setValue(Object value);
	
	public boolean isEditable();
	
	public String getCategory();
	
	public void readFromObject(Object object);
	
	public void writeToObject(Object object);
	
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	public void removePropertyChangeListener(PropertyChangeListener listener);
	
	public Object clone() throws CloneNotSupportedException;
	
	public Property getParentProperty();
	
	public Property[] getSubProperties();
}
