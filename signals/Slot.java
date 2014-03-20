package signals;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import logs.Logs;

/**
 * Value combining an object with a selector. The object is weakly referenced, and null values are not allowed.
 */
public class Slot extends WeakReference<Object> {
	
	private Method method;
	private int hashCode;
	
	public Slot(Object slotProvider, String slotMethod, Class<?>... slotArgTypes)
	{
		super(slotProvider);
		hashCode = slotProvider.hashCode();
		try
		{
			method = slotProvider.getClass().getMethod(slotMethod, slotArgTypes);
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			Logs.log("Couldn't find method \"" + slotMethod + "\" in " + slotProvider.getClass().getSimpleName() + " to create the desired Slot object", 0, this);
			e.printStackTrace();
		}
	}
	
	/**
	 * This constructor is used when the slot is a static method
	 * 
	 * @param slotProvider
	 * @param slotMethod
	 * @param slotArgTypes
	 */
	public Slot(Class<?> slotProvider, String slotMethod, Class<?>... slotArgTypes)
	{
		super(slotProvider);
		hashCode = slotProvider.hashCode();
		try
		{
			method = slotProvider.getMethod(slotMethod, slotArgTypes);
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			Logs.log("Couldn't find method to create the desired Slot object", 0, this);
			e.printStackTrace();
		}
	}
	
	public Method method()
	{
		return method;
	}
	
	public Object slotProvider()
	{
		return this.get();
	}
	
	public void invoke(Object... args)
	{
		try
		{
			this.method().invoke(this.slotProvider(), args);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			Logs.log("Couldn't find desired method in " + this.get().getClass().getSimpleName(), 0, this);
		}
	}
	
	@Override
	public int hashCode()
	{
		return hashCode;
	}
	
	@Override
	public boolean equals(Object anObject)
	{
		if(this == anObject)
			return true;
		// assumes only used with other compound values
		Slot testSlot = (Slot) anObject;
		
		// If they are the same, they probably the same Method object so test
		// that first
		if(this.method() == testSlot.method() || (this.method() != null && this.method().equals(testSlot.method())))
		{
			Object slotProvider = this.slotProvider();
			if(slotProvider != null)
			{
				// compares by reference
				if(slotProvider == testSlot.slotProvider())
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "[Slot:" + this.slotProvider() + ":" + this.method().getName() + "]";
	}
}
