package preferences;

import java.awt.Color;
import java.beans.PropertyDescriptor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.table.TableCellRenderer;

/**
 * Mapping between Properties, Property Types and Renderers.
 */
public class PropertyRendererRegistry implements PropertyRendererFactory {
	
	@SuppressWarnings("rawtypes")
	private Map typeToRenderer;
	@SuppressWarnings("rawtypes")
	private Map propertyToRenderer;
	
	@SuppressWarnings("rawtypes")
	public PropertyRendererRegistry()
	{
		typeToRenderer = new HashMap();
		propertyToRenderer = new HashMap();
		registerDefaults();
	}
	
	public TableCellRenderer createTableCellRenderer(Property property)
	{
		return getRenderer(property);
	}
	
	@SuppressWarnings("rawtypes")
	public TableCellRenderer createTableCellRenderer(Class type)
	{
		return getRenderer(type);
	}
	
	/**
	 * Gets a renderer for the given property. The lookup is as follow:
	 * <ul>
	 * <li>if a renderer was registered with {@link ExtendedPropertyDescriptor#setPropertyTableRendererClass(Class)} - BeanInfo, it is returned, else</li>
	 * <li>if a renderer was registered with {@link #registerRenderer(Property, TableCellRenderer)}, it is returned, else</li>
	 * <li>if a renderer class was registered with {@link #registerRenderer(Property, Class)}, it is returned, else
	 * <li>
	 * <li>look for renderer for the property type using {@link #getRenderer(Class)}.</li>
	 * </ul>
	 * 
	 * @param property
	 * @return a renderer suitable for the Property.
	 */
	@SuppressWarnings("rawtypes")
	public synchronized TableCellRenderer getRenderer(Property property)
	{
		
		// editors bound to the property descriptor have the highest priority
		TableCellRenderer renderer = null;
		if(property instanceof PropertyDescriptorAdapter)
		{
			PropertyDescriptor descriptor = ((PropertyDescriptorAdapter) property).getDescriptor();
			if(descriptor instanceof ExtendedPropertyDescriptor)
			{
				if(((ExtendedPropertyDescriptor) descriptor).getPropertyTableRendererClass() != null)
				{
					try
					{
						return (TableCellRenderer) (((ExtendedPropertyDescriptor) descriptor).getPropertyTableRendererClass()).newInstance();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		}
		Object value = propertyToRenderer.get(property);
		if(value instanceof TableCellRenderer)
		{
			renderer = (TableCellRenderer) value;
		}
		else if(value instanceof Class)
		{
			try
			{
				renderer = (TableCellRenderer) ((Class) value).newInstance();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			renderer = getRenderer(property.getType());
		}
		return renderer;
	}
	
	/**
	 * Gets a renderer for the given property type. The lookup is as follow:
	 * <ul>
	 * <li>if a renderer was registered with {@link #registerRenderer(Class, TableCellRenderer)}, it is returned, else</li>
	 * <li>if a renderer class was registered with {@link #registerRenderer(Class, Class)}, it is returned, else
	 * <li>
	 * <li>it returns null.</li>
	 * </ul>
	 * 
	 * @param type
	 * @return a renderer editor suitable for the Property type or null if none found
	 */
	@SuppressWarnings("rawtypes")
	public synchronized TableCellRenderer getRenderer(Class type)
	{
		TableCellRenderer renderer = null;
		Object value = typeToRenderer.get(type);
		if(value instanceof TableCellRenderer)
		{
			renderer = (TableCellRenderer) value;
		}
		else if(value instanceof Class)
		{
			try
			{
				renderer = (TableCellRenderer) ((Class) value).newInstance();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return renderer;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void registerRenderer(Class type, Class rendererClass)
	{
		typeToRenderer.put(type, rendererClass);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void registerRenderer(Class type, TableCellRenderer renderer)
	{
		typeToRenderer.put(type, renderer);
	}
	
	@SuppressWarnings("rawtypes")
	public synchronized void unregisterRenderer(Class type)
	{
		typeToRenderer.remove(type);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void registerRenderer(Property property, Class rendererClass)
	{
		propertyToRenderer.put(property, rendererClass);
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void registerRenderer(Property property, TableCellRenderer renderer)
	{
		propertyToRenderer.put(property, renderer);
	}
	
	public synchronized void unregisterRenderer(Property property)
	{
		propertyToRenderer.remove(property);
	}
	
	/**
	 * Adds default renderers. This method is called by the constructor but may be called later to reset any customizations made through the <code>registerRenderer</code> methods. <b>Note: if overriden, <code>super.registerDefaults()</code> must be
	 * called before plugging custom defaults. </b>
	 */
	public void registerDefaults()
	{
		typeToRenderer.clear();
		propertyToRenderer.clear();
		
		// use the default renderer for Object and all primitives
		DefaultCellRenderer renderer = new DefaultCellRenderer();
		renderer.setShowOddAndEvenRows(false);
		
		ColorCellRenderer colorRenderer = new ColorCellRenderer();
		colorRenderer.setShowOddAndEvenRows(false);
		
		BooleanCellRenderer booleanRenderer = new BooleanCellRenderer();
		
		DateRenderer dateRenderer = new DateRenderer();
		dateRenderer.setShowOddAndEvenRows(false);
		
		registerRenderer(Object.class, renderer);
		registerRenderer(Color.class, colorRenderer);
		registerRenderer(boolean.class, booleanRenderer);
		registerRenderer(Boolean.class, booleanRenderer);
		registerRenderer(byte.class, renderer);
		registerRenderer(Byte.class, renderer);
		registerRenderer(char.class, renderer);
		registerRenderer(Character.class, renderer);
		registerRenderer(double.class, renderer);
		registerRenderer(Double.class, renderer);
		registerRenderer(float.class, renderer);
		registerRenderer(Float.class, renderer);
		registerRenderer(int.class, renderer);
		registerRenderer(Integer.class, renderer);
		registerRenderer(long.class, renderer);
		registerRenderer(Long.class, renderer);
		registerRenderer(short.class, renderer);
		registerRenderer(Short.class, renderer);
		registerRenderer(Date.class, dateRenderer);
	}
	
}
