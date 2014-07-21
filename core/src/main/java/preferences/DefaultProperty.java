package preferences;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * DefaultProperty. <br>
 * 
 */
public class DefaultProperty extends AbstractProperty implements Cloneable {
	
	private static final long serialVersionUID = 1L;
	private String name;
	private String displayName;
	private String shortDescription;
	@SuppressWarnings("rawtypes")
	private Class type;
	private boolean editable = true;
	private String category;
	private Property parent;
	private List<Property> subProperties = new ArrayList<Property>();
	
	@Override
	public Object clone()
	{
		DefaultProperty other = null;
		try
		{
			other = (DefaultProperty) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			e.printStackTrace();
		}
		other.name = name;
		other.displayName = displayName;
		other.shortDescription = shortDescription;
		other.type = type;
		other.editable = editable;
		other.category = category;
		other.setValue(getValue());
		return other;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getDisplayName()
	{
		return displayName;
	}
	
	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}
	
	public String getShortDescription()
	{
		return shortDescription;
	}
	
	public void setShortDescription(String shortDescription)
	{
		this.shortDescription = shortDescription;
	}
	
	@SuppressWarnings("rawtypes")
	public Class getType()
	{
		return type;
	}
	
	@SuppressWarnings("rawtypes")
	public void setType(Class type)
	{
		this.type = type;
	}
	
	public boolean isEditable()
	{
		return editable;
	}
	
	public void setEditable(boolean editable)
	{
		this.editable = editable;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public void setCategory(String category)
	{
		this.category = category;
	}
	
	/**
	 * Reads the value of this Property from the given object. It uses reflection and looks for a method starting with "is" or "get" followed by the capitalized Property name.
	 */
	public void readFromObject(Object object)
	{
		try
		{
			Method method = BeanUtils.getReadMethod(object.getClass(), getName());
			if(method != null)
			{
				setValue(method.invoke(object, (Object[]) null));
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		for (Iterator<Property> iter = subProperties.iterator(); iter.hasNext();)
		{
			Property subProperty = iter.next();
			subProperty.readFromObject(object);
		}
	}
	
	/**
	 * Writes the value of the Property to the given object. It uses reflection and looks for a method starting with "set" followed by the capitalized Property name and with one parameter with the same type as the Property.
	 */
	public void writeToObject(Object object)
	{
		try
		{
			Method method = BeanUtils.getWriteMethod(object.getClass(), getName(), getType());
			if(method != null)
			{
				method.invoke(object, new Object[] { getValue() });
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		if(parent != null)
			parent.readFromObject(object);
		for (Iterator<Property> iter = subProperties.iterator(); iter.hasNext();)
		{
			Property subProperty = iter.next();
			subProperty.readFromObject(object);
		}
	}
	
	@Override
	public int hashCode()
	{
		return 28 + ((name != null) ? name.hashCode() : 3) + ((displayName != null) ? displayName.hashCode() : 94) + ((shortDescription != null) ? shortDescription.hashCode() : 394) + ((category != null) ? category.hashCode() : 34) + ((type != null) ? type.hashCode() : 39) + Boolean.valueOf(editable).hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(other == null || getClass() != other.getClass())
		{
			return false;
		}
		
		if(other == this)
		{
			return true;
		}
		
		DefaultProperty dp = (DefaultProperty) other;
		
		return compare(name, dp.name) && compare(displayName, dp.displayName) && compare(shortDescription, dp.shortDescription) && compare(category, dp.category) && compare(type, dp.type) && editable == dp.editable;
	}
	
	private boolean compare(Object o1, Object o2)
	{
		return (o1 != null) ? o1.equals(o2) : o2 == null;
	}
	
	@Override
	public String toString()
	{
		return "name=" + getName() + ", displayName=" + getDisplayName() + ", type=" + getType() + ", category=" + getCategory() + ", editable=" + isEditable() + ", value=" + getValue();
	}
	
	@Override
	public Property getParentProperty()
	{
		return null;
	}
	
	public void setParentProperty(Property parent)
	{
		this.parent = parent;
	}
	
	@Override
	public Property[] getSubProperties()
	{
		return subProperties.toArray(new Property[subProperties.size()]);
	}
	
	public void clearSubProperties()
	{
		for (Iterator<Property> iter = this.subProperties.iterator(); iter.hasNext();)
		{
			Property subProp = iter.next();
			if(subProp instanceof DefaultProperty)
				((DefaultProperty) subProp).setParentProperty(null);
		}
		this.subProperties.clear();
	}
	
	public void addSubProperties(Collection<Property> subProperties)
	{
		this.subProperties.addAll(subProperties);
		for (Iterator<Property> iter = this.subProperties.iterator(); iter.hasNext();)
		{
			Property subProp = iter.next();
			if(subProp instanceof DefaultProperty)
				((DefaultProperty) subProp).setParentProperty(this);
		}
	}
	
	public void addSubProperties(Property[] subProperties)
	{
		this.addSubProperties(Arrays.asList(subProperties));
	}
	
	public void addSubProperty(Property subProperty)
	{
		this.subProperties.add(subProperty);
		if(subProperty instanceof DefaultProperty)
			((DefaultProperty) subProperty).setParentProperty(this);
	}
}