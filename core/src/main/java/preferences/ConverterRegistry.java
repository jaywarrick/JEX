package preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * ConverterRegistry. <br>
 * 
 */
public class ConverterRegistry implements Converter {
	
	private static ConverterRegistry sharedInstance = new ConverterRegistry();
	
	@SuppressWarnings("rawtypes")
	private Map fromMap;
	
	@SuppressWarnings("rawtypes")
	public ConverterRegistry()
	{
		fromMap = new HashMap();
		
		new BooleanConverter().register(this);
		new AWTConverters().register(this);
		new NumberConverters().register(this);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addConverter(Class from, Class to, Converter converter)
	{
		Map toMap = (Map) fromMap.get(from);
		if(toMap == null)
		{
			toMap = new HashMap();
			fromMap.put(from, toMap);
		}
		toMap.put(to, converter);
	}
	
	@SuppressWarnings("rawtypes")
	public Converter getConverter(Class from, Class to)
	{
		Map toMap = (Map) fromMap.get(from);
		if(toMap != null)
		{
			return (Converter) toMap.get(to);
		}
		else
		{
			return null;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public Object convert(Class targetType, Object value)
	{
		if(value == null)
		{
			return null;
		}
		
		Converter converter = getConverter(value.getClass(), targetType);
		if(converter == null)
		{
			throw new IllegalArgumentException("No converter from " + value.getClass() + " to " + targetType.getName());
		}
		else
		{
			return converter.convert(targetType, value);
		}
	}
	
	public static ConverterRegistry instance()
	{
		return sharedInstance;
	}
	
}
