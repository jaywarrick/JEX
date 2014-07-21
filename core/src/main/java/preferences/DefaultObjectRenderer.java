package preferences;

import java.io.File;

/**
 * DefaultObjectRenderer. <br>
 * 
 */
public class DefaultObjectRenderer implements ObjectRenderer {
	
	private boolean idVisible = false;
	
	public void setIdVisible(boolean b)
	{
		idVisible = b;
	}
	
	public String getText(Object object)
	{
		if(object == null)
		{
			return null;
		}
		
		// lookup the shared ConverterRegistry
		try
		{
			return (String) ConverterRegistry.instance().convert(String.class, object);
		}
		catch (IllegalArgumentException e)
		{}
		
		if(object instanceof Boolean)
		{
			return Boolean.TRUE.equals(object) ? ResourceManager.common().getString("true") : ResourceManager.common().getString("false");
		}
		
		if(object instanceof File)
		{
			return ((File) object).getAbsolutePath();
		}
		
		StringBuffer buffer = new StringBuffer();
		if(idVisible && object instanceof HasId)
		{
			buffer.append(((HasId) object).getId());
		}
		if(object instanceof TitledObject)
		{
			buffer.append(((TitledObject) object).getTitle());
		}
		if(!(object instanceof HasId || object instanceof TitledObject))
		{
			buffer.append(String.valueOf(object));
		}
		return buffer.toString();
	}
	
}
