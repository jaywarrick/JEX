package preferences;

import java.beans.BeanInfo;

/**
 * DefaultBeanInfoResolver. <br>
 * 
 */
public class DefaultBeanInfoResolver implements BeanInfoResolver {
	
	public DefaultBeanInfoResolver()
	{
		super();
	}
	
	public BeanInfo getBeanInfo(Object object)
	{
		if(object == null)
		{
			return null;
		}
		
		return getBeanInfo(object.getClass());
	}
	
	@SuppressWarnings("rawtypes")
	public BeanInfo getBeanInfo(Class clazz)
	{
		if(clazz == null)
		{
			return null;
		}
		
		String classname = clazz.getName();
		
		// look for .impl.basic., remove it and call getBeanInfo(class)
		int index = classname.indexOf(".impl.basic");
		if(index != -1 && classname.endsWith("Basic"))
		{
			classname = classname.substring(0, index) + classname.substring(index + ".impl.basic".length(), classname.lastIndexOf("Basic"));
			try
			{
				return getBeanInfo(Class.forName(classname));
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			try
			{
				BeanInfo beanInfo = (BeanInfo) Class.forName(classname + "BeanInfo").newInstance();
				return beanInfo;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}
	}
	
}
