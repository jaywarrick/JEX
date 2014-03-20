package preferences;

import java.beans.BeanInfo;

/**
 * BeanInfoResolver.<br>
 * 
 */
public interface BeanInfoResolver {
	
	public BeanInfo getBeanInfo(Object object);
	
	@SuppressWarnings("rawtypes")
	public BeanInfo getBeanInfo(Class clazz);
	
}
