package preferences;

import java.text.NumberFormat;

/**
 * Convert to and from numbers. <br>
 * 
 * The following convertions are supported:
 * 
 * <table>
 * <tr>
 * <th>From</th>
 * <th>To</th>
 * <th>Reverse</th>
 * </tr>
 * <tr>
 * <td>Number (and subclasses)</td>
 * <td>Number (and subclasses)</td>
 * <td>Yes</td>
 * </tr>
 * </table>
 */
public class NumberConverters implements Converter {
	
	private static NumberFormat defaultFormat;
	
	private NumberFormat format;
	
	public NumberConverters()
	{
		this(getDefaultFormat());
	}
	
	public NumberConverters(NumberFormat format)
	{
		this.format = format;
	}
	
	public static NumberFormat getDefaultFormat()
	{
		synchronized (NumberConverters.class)
		{
			if(defaultFormat == null)
			{
				defaultFormat = NumberFormat.getNumberInstance();
				defaultFormat.setMinimumIntegerDigits(1);
				defaultFormat.setMaximumIntegerDigits(64);
				defaultFormat.setMinimumFractionDigits(0);
				defaultFormat.setMaximumFractionDigits(64);
			}
		}
		return defaultFormat;
	}
	
	public void register(ConverterRegistry registry)
	{
		registry.addConverter(Number.class, Double.class, this);
		registry.addConverter(Number.class, Float.class, this);
		registry.addConverter(Number.class, Integer.class, this);
		registry.addConverter(Number.class, Long.class, this);
		registry.addConverter(Number.class, Short.class, this);
		
		registry.addConverter(Double.class, Double.class, this);
		registry.addConverter(Double.class, Float.class, this);
		registry.addConverter(Double.class, Integer.class, this);
		registry.addConverter(Double.class, Long.class, this);
		registry.addConverter(Double.class, Short.class, this);
		registry.addConverter(Double.class, String.class, this);
		
		registry.addConverter(Float.class, Double.class, this);
		registry.addConverter(Float.class, Float.class, this);
		registry.addConverter(Float.class, Integer.class, this);
		registry.addConverter(Float.class, Long.class, this);
		registry.addConverter(Float.class, Short.class, this);
		registry.addConverter(Float.class, String.class, this);
		
		registry.addConverter(Integer.class, Double.class, this);
		registry.addConverter(Integer.class, Float.class, this);
		registry.addConverter(Integer.class, Integer.class, this);
		registry.addConverter(Integer.class, Long.class, this);
		registry.addConverter(Integer.class, Short.class, this);
		registry.addConverter(Integer.class, String.class, this);
		
		registry.addConverter(Long.class, Double.class, this);
		registry.addConverter(Long.class, Float.class, this);
		registry.addConverter(Long.class, Integer.class, this);
		registry.addConverter(Long.class, Long.class, this);
		registry.addConverter(Long.class, Short.class, this);
		registry.addConverter(Long.class, String.class, this);
		
		registry.addConverter(Short.class, Double.class, this);
		registry.addConverter(Short.class, Float.class, this);
		registry.addConverter(Short.class, Integer.class, this);
		registry.addConverter(Short.class, Long.class, this);
		registry.addConverter(Short.class, Short.class, this);
		registry.addConverter(Short.class, String.class, this);
		
		registry.addConverter(String.class, Double.class, this);
		registry.addConverter(String.class, Float.class, this);
		registry.addConverter(String.class, Integer.class, this);
		registry.addConverter(String.class, Long.class, this);
		registry.addConverter(String.class, Short.class, this);
	}
	
	@SuppressWarnings("rawtypes")
	public Object convert(Class targetType, Object value)
	{
		// are we dealing with a number to number conversion?
		if((value instanceof Number) && Number.class.isAssignableFrom(targetType))
		{
			if(Double.class.equals(targetType))
			{
				return new Double(((Number) value).doubleValue());
			}
			else if(Float.class.equals(targetType))
			{
				return new Float(((Number) value).floatValue());
			}
			else if(Integer.class.equals(targetType))
			{
				return new Integer(((Number) value).intValue());
			}
			else if(Long.class.equals(targetType))
			{
				return new Long(((Number) value).longValue());
			}
			else if(Short.class.equals(targetType))
			{
				return new Short(((Number) value).shortValue());
			}
			else
			{
				throw new IllegalArgumentException("this code must not be reached");
			}
		}
		else if((value instanceof Number) && String.class.equals(targetType))
		{
			if((value instanceof Double) || (value instanceof Float))
			{
				return format.format(((Number) value).doubleValue());
			}
			else
			{
				return format.format(((Number) value).longValue());
			}
		}
		else if((value instanceof String) && Number.class.isAssignableFrom(targetType))
		{
			if(Double.class.equals(targetType))
			{
				return new Double((String) value);
			}
			else if(Float.class.equals(targetType))
			{
				return new Float((String) value);
			}
			else if(Integer.class.equals(targetType))
			{
				return new Integer((String) value);
			}
			else if(Long.class.equals(targetType))
			{
				return new Long((String) value);
			}
			else if(Short.class.equals(targetType))
			{
				return new Short((String) value);
			}
			else
			{
				throw new IllegalArgumentException("this code must not be reached");
			}
		}
		throw new IllegalArgumentException("no conversion supported");
	}
	
}