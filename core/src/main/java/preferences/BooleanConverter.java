package preferences;

/**
 * Converts a boolean to string and vice-versa.
 */
public class BooleanConverter implements Converter {
	
	public void register(ConverterRegistry registry)
	{
		registry.addConverter(String.class, boolean.class, this);
		registry.addConverter(String.class, Boolean.class, this);
		registry.addConverter(Boolean.class, String.class, this);
		registry.addConverter(boolean.class, String.class, this);
	}
	
	@SuppressWarnings("rawtypes")
	public Object convert(Class type, Object value)
	{
		if(String.class.equals(type) && Boolean.class.equals(value.getClass()))
		{
			return String.valueOf(value);
		}
		else if(boolean.class.equals(type) || Boolean.class.equals(type))
		{
			return Boolean.valueOf(String.valueOf(value));
		}
		else
		{
			throw new IllegalArgumentException("Can't convert " + value + " to " + type.getName());
		}
	}
	
}
