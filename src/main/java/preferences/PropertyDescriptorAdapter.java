package preferences;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * PropertyDescriptorAdapter.<br>
 * 
 */
class PropertyDescriptorAdapter extends AbstractProperty {
	
	private static final long serialVersionUID = 1L;
	private PropertyDescriptor descriptor;
	
	public PropertyDescriptorAdapter()
	{
		super();
	}
	
	public PropertyDescriptorAdapter(PropertyDescriptor descriptor)
	{
		this();
		setDescriptor(descriptor);
	}
	
	public void setDescriptor(PropertyDescriptor descriptor)
	{
		this.descriptor = descriptor;
	}
	
	public PropertyDescriptor getDescriptor()
	{
		return descriptor;
	}
	
	public String getName()
	{
		return descriptor.getName();
	}
	
	public String getDisplayName()
	{
		return descriptor.getDisplayName();
	}
	
	public String getShortDescription()
	{
		return descriptor.getShortDescription();
	}
	
	@SuppressWarnings("rawtypes")
	public Class getType()
	{
		return descriptor.getPropertyType();
	}
	
	@Override
	public Object clone()
	{
		PropertyDescriptorAdapter clone = new PropertyDescriptorAdapter(descriptor);
		clone.setValue(getValue());
		return clone;
	}
	
	public void readFromObject(Object object)
	{
		try
		{
			Method method = descriptor.getReadMethod();
			if(method != null)
			{
				setValue(method.invoke(object, (Object[]) null));
			}
		}
		catch (Exception e)
		{
			String message = "Got exception when reading property " + getName();
			if(object == null)
			{
				message += ", object was 'null'";
			}
			else
			{
				message += ", object was " + String.valueOf(object);
			}
			throw new RuntimeException(message, e);
		}
	}
	
	public void writeToObject(Object object)
	{
		try
		{
			Method method = descriptor.getWriteMethod();
			if(method != null)
			{
				method.invoke(object, new Object[] { getValue() });
			}
		}
		catch (Exception e)
		{
			String message = "Got exception when writing property " + getName();
			if(object == null)
			{
				message += ", object was 'null'";
			}
			else
			{
				message += ", object was " + String.valueOf(object);
			}
			throw new RuntimeException(message, e);
		}
	}
	
	public boolean isEditable()
	{
		return descriptor.getWriteMethod() != null;
	}
	
	public String getCategory()
	{
		if(descriptor instanceof ExtendedPropertyDescriptor)
		{
			return ((ExtendedPropertyDescriptor) descriptor).getCategory();
		}
		else
		{
			return null;
		}
	}
	
}
