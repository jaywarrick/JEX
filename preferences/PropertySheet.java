package preferences;

import java.util.Iterator;

/**
 * PropertySheet.<br>
 * 
 */
public interface PropertySheet {
	
	public final static int VIEW_AS_FLAT_LIST = 0;
	
	public final static int VIEW_AS_CATEGORIES = 1;
	
	public void setProperties(Property[] properties);
	
	public Property[] getProperties();
	
	public void addProperty(Property property);
	
	public void addProperty(int index, Property property);
	
	public void removeProperty(Property property);
	
	public int getPropertyCount();
	
	@SuppressWarnings("rawtypes")
	public Iterator propertyIterator();
	
}
