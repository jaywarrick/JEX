package preferences;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * IconPool.<br>
 * 
 */
public class IconPool {
	
	private static IconPool iconPool = new IconPool();
	
	@SuppressWarnings("rawtypes")
	private Map pool;
	
	@SuppressWarnings("rawtypes")
	public IconPool()
	{
		pool = new HashMap();
	}
	
	public static IconPool shared()
	{
		return iconPool;
	}
	
	/**
	 * Gets the icon denoted by url. If url is relative, it is relative to the caller.
	 * 
	 * @param url
	 * @return an icon
	 */
	@SuppressWarnings("rawtypes")
	public Icon get(String url)
	{
		StackTraceElement[] stacks = new Exception().getStackTrace();
		try
		{
			Class callerClazz = Class.forName(stacks[1].getClassName());
			return get(callerClazz.getResource(url));
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized Icon get(URL url)
	{
		if(url == null)
		{
			return null;
		}
		
		Icon icon = (Icon) pool.get(url.toString());
		if(icon == null)
		{
			icon = new ImageIcon(url);
			pool.put(url.toString(), icon);
		}
		return icon;
	}
	
	public synchronized void clear()
	{
		pool.clear();
	}
	
}
